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
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.MeshUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;

@ModuleDescription("Sample a volume at input vertices")
@ModuleAuthor("Ryan Cabeen")
public class MeshSample implements Module
{
    enum MeshSampleStatistic
    {Mean, Min, Max}

    enum MeshSampleDirection
    {Both, Inside, Outside}

    @ModuleInput
    @ModuleDescription("input input")
    private Mesh input;

    @ModuleInput
    @ModuleDescription("input volume")
    private Volume volume;

    @ModuleParameter
    @ModuleDescription("interpolation method")
    private InterpolationType interp = InterpolationType.Trilinear;

    @ModuleParameter
    @ModuleDescription("coord attribute name (used for sampling volume)")
    private String coord = "coord";

    @ModuleParameter
    @ModuleDescription("output attribute name (where the sampled image data will be stored)")
    private String attr = "sampled";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("compute a statistical summary of image intensities sampled along points within a certain window of each vertex (units: mm), sampled along the surface normal direction")
    private Double window = null;

    @ModuleParameter
    @ModuleDescription("compute the statistical summary in a given direction (default is both inside and outside the mesh)")
    private MeshSampleDirection sample = MeshSampleDirection.Both;

    @ModuleParameter
    @ModuleDescription("use the given statistic for summarizing the vertex window")
    private MeshSampleStatistic stat = MeshSampleStatistic.Mean;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    private boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    private Mesh output;

    @Override
    public Module run()
    {
        VectFunction interp = VolumeUtils.interp(this.interp, this.volume);
        Mesh mesh = this.inplace ? this.input : this.input.copy();
        MeshUtils.sample(mesh, interp, this.coord, this.attr);

        double delta = 0.5 * this.volume.getSampling().deltaMin();
        if (this.window != null)
        {
            Logging.info("using window statistics with step size: " + delta);
            if (mesh.vattr.has(Mesh.NORMAL))
            {
                Logging.info("computing normals");
                MeshUtils.computeNormals(mesh);
            }
        }

        mesh.vattr.add(attr, interp.protoOut());
        for (Vertex vert : mesh.vattr)
        {
            Vect pos = mesh.vattr.get(vert, this.coord);

            if (this.window == null)
            {
                mesh.vattr.set(vert, this.attr, interp.apply(pos));
            }
            else
            {
                Vect normal = mesh.vattr.get(vert, Mesh.NORMAL).normalize();
                VectsOnlineStats stats = new VectsOnlineStats(interp.getDimOut());

                // update the mesh vertex
                stats.update(interp.apply(pos));

                // update the window
                for (int i = 1; i < Math.ceil(this.window / delta); i++)
                {
                    if (this.sample == MeshSampleDirection.Both || this.sample == MeshSampleDirection.Outside)
                    {
                        stats.update(interp.apply(pos.plus(normal.times(i * delta))));
                    }
                    else if (this.sample == MeshSampleDirection.Both || this.sample == MeshSampleDirection.Inside)
                    {
                        stats.update(interp.apply(pos.plus(normal.times(-1 * i * delta))));
                    }
                }

                switch (this.stat)
                {
                    case Mean:
                        mesh.vattr.set(vert, this.attr, stats.mean);
                        break;
                    case Min:
                        mesh.vattr.set(vert, this.attr, stats.min);
                        break;
                    case Max:
                        mesh.vattr.set(vert, this.attr, stats.max);
                        break;
                }

            }
        }

        this.output = mesh;

        return this;
    }
}
