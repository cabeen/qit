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

package qit.data.modules.mesh;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.utils.MeshUtils;
import qit.math.structs.Face;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

@ModuleDescription("Extract the boundaries of mesh regions")
@ModuleAuthor("Ryan Cabeen")
public class MeshAttrBoundary implements Module
{
    @ModuleInput
    @ModuleDescription("the input Mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("a comma-separated list of coordinate attributes to lift")
    public String coord = Mesh.COORD;

    @ModuleParameter
    @ModuleDescription("the label attribute name")
    public String label = Mesh.COORD;

    @ModuleParameter
    @ModuleDescription("use a given threshold for picking the boundary")
    public double threshold = 0.5;

    @ModuleParameter
    @ModuleDescription("apply the given amount of lift off the mesh")
    public double lift = 0;

    @ModuleOutput
    @ModuleDescription("the output Mesh")
    public Mesh output;

    @Override
    public MeshAttrBoundary run()
    {
        Mesh out = new Mesh();

        for (Face face : this.input.graph.faces())
        {
            boolean a = this.input.vattr.get(face.getA(), this.label).get(0) >= this.threshold;
            boolean b = this.input.vattr.get(face.getB(), this.label).get(0) >= this.threshold;
            boolean c = this.input.vattr.get(face.getC(), this.label).get(0) >= this.threshold;

            if (a != b || b != c | a != c)
            {
                out.graph.add(face);
            }
        }

        for (Vertex vertex : out.graph.verts())
        {
            out.vattr.set(vertex, Mesh.COORD, this.input.vattr.get(vertex, this.coord));
            out.vattr.set(vertex, this.label, this.input.vattr.get(vertex, this.label));
        }


        if (MathUtils.nonzero(this.lift))
        {
            Logging.info("lifting attribute: " + this.coord);
            MeshUtils.computeNormals(this.input, this.coord);

            for (Vertex vertex : out.graph.verts())
            {
                Vect cv = this.input.vattr.get(vertex, this.coord);
                Vect nv = this.input.vattr.get(vertex, Mesh.NORMAL);
                Vect lifted = cv.plus(this.lift, nv);
                out.vattr.set(vertex, Mesh.COORD, lifted);
            }
        }

        this.output = out;

        return this;
    }
}
