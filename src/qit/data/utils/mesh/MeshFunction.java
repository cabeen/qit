/*******************************************************************************
  *
  * Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
  * All rights reserved.
  *
  * The Software remains the property of Ryan Cabeen ("the Author").
  *
  * The Software is distributed "AS IS" under this Licence solely for
  * non-commercial use in the hope that it will be useful, but in order
  * that the Author as a charitable foundation protects its assets for
  * the benefit of its educational and research purposes, the Author
  * makes clear that no condition is made or to be implied, nor is any
  * warranty given or to be implied, as to the accuracy of the Software,
  * or that it will be suitable for any particular purpose or for use
  * under any specific conditions. Furthermore, the Author disclaims
  * all responsibility for the use which is made of the Software. It
  * further disclaims any liability for the outcomes arising from using
  * the Software.
  *
  * The Licensee agrees to indemnify the Author and hold the
  * Author harmless from and against any and all claims, damages and
  * liabilities asserted by third parties (including claims for
  * negligence) which arise directly or indirectly from the use of the
  * Software or the sale of any products based on the Software.
  *
  * No part of the Software may be reproduced, modified, transmitted or
  * transferred in any form or by any means, electronic or mechanical,
  * without the express permission of the Author. The permission of
  * the Author is not required if the said reproduction, modification,
  * transmission or transference is done without financial return, the
  * conditions of this Licence are imposed upon the receiver of the
  * product, and all original and amended source code is included in any
  * transmitted product. You may be held legally responsible for any
  * copyright infringement that is caused or encouraged by your failure to
  * abide by these terms and conditions.
  *
  * You are not permitted under this Licence to use this Software
  * commercially. Use for which any financial return is received shall be
  * defined as commercial use, and includes (1) integration of all or part
  * of the source code or the Software into a product for sale or license
  * by or on behalf of Licensee to third parties or (2) use of the
  * Software or any derivative of it for research with the final aim of
  * developing software products for sale or license to a third party or
  * (3) use of the Software or any derivative of it for research with the
  * final aim of developing non-software products for sale or license to a
  * third party, or (4) use of the Software to provide any service to an
  * external organisation for which payment is received.
  *
  ******************************************************************************/

package qit.data.utils.mesh;

import qit.data.datasets.AttrMap;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;

public class MeshFunction
{
    private Mesh mesh;
    private VectFunction func;
    private String source = Mesh.COORD;
    private String dest = Mesh.COORD;

    public MeshFunction(VectFunction function)
    {
        this.func = function;
    }

    public MeshFunction withMesh(Mesh mesh)
    {
        this.mesh = mesh;

        return this;
    }

    public MeshFunction withSource(String attr)
    {
        this.source = attr;

        return this;
    }

    public MeshFunction withDest(String attr)
    {
        this.dest = attr;

        return this;
    }

    public Mesh run()
    {
        AttrMap<Vertex> attr = this.mesh.vattr;

        boolean constant = this.source == null || !this.mesh.vattr.has(this.source);
        Vect zero = VectSource.createND(this.func.getDimIn());

        Vect buffer = this.func.protoOut();
        for (Vertex vert : attr)
        {
            Vect value = constant ? zero : attr.get(vert, this.source);
            this.func.apply(value, buffer);
            attr.set(vert, this.dest, buffer);
        }

        if (this.dest.equals(Mesh.COORD))
        {
            MeshUtils.computeNormals(this.mesh);
        }

        return this.mesh;
    }
}
