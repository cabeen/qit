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

import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.math.structs.Face;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

@ModuleDescription("Grow a mesh to fill a volume")
@ModuleAuthor("Ryan Cabeen")
public class MeshGrow implements Module
{
    @ModuleInput
    @ModuleDescription("the input mesh")
    public Mesh input;

    @ModuleInput
    @ModuleDescription("the reference volume")
    public Volume reference;

    @ModuleParameter
    @ModuleDescription("the source vertex attribute (default is the vertex position)")
    public String source = "coord";

    @ModuleParameter
    @ModuleDescription("the destination vertex attribute (the result will be saved here)")
    public String dest = "shell";

    @ModuleParameter
    @ModuleDescription("the distance to the best match (the result will be saved here)")
    public String dist = "dist";

    @ModuleParameter
    @ModuleDescription("the threshold")
    public double threshold = 0.5;

    @ModuleParameter
    @ModuleDescription("the maximums distance to search")
    public double distance = 20;

    @ModuleParameter
    @ModuleDescription("the maximums distance to search")
    public double step = 0.25;

    @ModuleParameter
    @ModuleDescription("detect and increase past the threshold (the default is to detect the drop)")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("search inside the mesh (the default is to search outside)")
    public boolean outside = false;

    @ModuleParameter
    @ModuleDescription("image interpolation method")
    public InterpolationType interp = InterpolationType.Trilinear;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    public MeshGrow run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();
        VectFunction sample = VolumeUtils.interp(this.interp, this.reference);

        int steps = MathUtils.round(this.distance / this.step);

        mesh.vattr.add(this.dest, VectSource.create3D());
        mesh.vattr.add(this.dist, VectSource.create1D());

        for (Vertex vi : mesh.graph.verts())
        {
            // Compute the initial normal
            Vect n = VectSource.create3D();
            for (Face f : mesh.graph.faceRing(vi))
            {
                Vect pa = mesh.vattr.get(f.getA(), this.source);
                Vect pb = mesh.vattr.get(f.getB(), this.source);
                Vect pc = mesh.vattr.get(f.getC(), this.source);

                Vect ba = pb.minus(pa);
                Vect ca = pc.minus(pa);
                Vect fn = ba.cross(ca).normalize();
                n.plusEquals(fn);
            }
            n.normalizeEquals();

            Vect start = mesh.vattr.get(vi, this.source);

            boolean found = false;
            for (int i = 0; i < steps; i++)
            {
                double dist = i * this.step;
                Vect pos = start.plus(dist, n);
                double value = sample.apply(pos).get(0);

                if ((this.invert && value < this.threshold) || (!this.invert && value > this.threshold))
                {
                    mesh.vattr.set(vi, this.dest, pos);
                    mesh.vattr.set(vi, this.dist, VectSource.create1D(dist));
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                mesh.vattr.set(vi, this.dest, start);
                mesh.vattr.set(vi, this.dist, VectSource.create1D(0));
            }
        }

        this.output = mesh;

        return this;
    }
}
