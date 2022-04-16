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

package qit.data.modules.mri.fibers;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.models.Tensor;
import qit.data.modules.volume.VolumeTransform;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.function.Function;

@ModuleDescription("Project fibers onto a reference volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersPick implements Module
{
    enum VolumeFibersProjectType { Hard, Soft, Pick }

    @ModuleInput
    @ModuleDescription("the input fibers to pick")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the reference vectors")
    public Volume reference;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask of the input image")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a deformation from the input to the reference")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("a threshold for picking")
    public double thresh = 0.05;

    @ModuleParameter
    @ModuleDescription("the number of threads")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("the output fibers")
    public Volume output;

    @Override
    public VolumeFibersPick run()
    {
        Sampling sampling = this.input.getSampling();

        final int pcomps = Fibers.count(this.input.getDim());
        Volume refvol = this.reference;

        if (this.deform != null)
        {
            Logging.info("deforming");
            refvol = new VolumeTransform()
            {{
                this.input = VolumeFibersPick.this.reference;
                this.reference = VolumeFibersPick.this.input;
                this.deform = VolumeFibersPick.this.deform;
                this.mask = VolumeFibersPick.this.mask;
                this.threads = VolumeFibersPick.this.threads;
                this.reorient = true;
            }}.run().output;
        }

        Function<Vect, Fibers> inputGet = v -> new Fibers(v);

        if (this.input.getModel().equals(ModelType.Tensor))
        {
            inputGet = v ->
            {
                Tensor tensor = new Tensor(v);
                Fibers out = new Fibers(1);
                out.setBaseline(tensor.getBaseline());
                out.setFrac(0, tensor.feature(Tensor.FEATURES_FA).get(0));
                out.setLine(0, tensor.getVec(0));

                return out;
            };
        }

        Volume out = this.input.copy();
        out.setModel(ModelType.Fibers);

        for (Sample sample : sampling)
        {
            if (this.input.valid(sample, this.mask))
            {
                Fibers fibers = inputGet.apply(this.input.get(sample));
                Vect ref = refvol.get(sample);

                if (ref.norm() >= this.thresh && fibers.getFracMax() >= this.thresh)
                {
                    Vect dists = VectSource.createND(fibers.size());
                    for (int i = 0; i < fibers.size(); i++)
                    {
                        dists.set(i, Math.abs(ref.dot(fibers.getLine(i).times(fibers.getFrac(i)))));
                    }

                    int maxidx = dists.maxidx();

                    if (dists.get(maxidx) > this.thresh)
                    {
                        Fibers fout = new Fibers(pcomps);
                        fout.setBaseline(fibers.base);
                        fout.setDiffusivity(fibers.diff);
                        fout.setFrac(0, fibers.getFrac(maxidx));
                        fout.setLine(0, fibers.getLine(maxidx));
                        out.set(sample, fout.getEncoding());
                    }
                }
            }
        }

        this.output = out;

        return this;
    }
}
