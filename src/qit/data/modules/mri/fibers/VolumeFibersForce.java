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

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.base.structs.Integers;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.modules.mask.MaskForceField;
import qit.data.source.VectSource;
import qit.math.source.VectFunctionSource;
import qit.math.structs.GaussianWatsonMixture;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ModuleUnlisted
@ModuleDescription("Segment compartments of a fibers volume using a force field")
@ModuleCitation("(in preparation)")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersForce implements Module
{
    @ModuleInput
    @ModuleDescription("the input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply the operation inside a mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("attract fibers with a solids object")
    public Solids attractSolids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("attract fibers with a mask object")
    public Mask attractMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("repel fibers with a solids object")
    public Solids repelSolids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("repel fibers with a mask object")
    public Mask repelMask;

    @ModuleParameter
    @ModuleDescription("the force scaleCamera")
    public double scale = 10.0;

    @ModuleParameter
    @ModuleDescription("the softmax gain")
    public double gain = 1.0;

    @ModuleParameter
    @ModuleDescription("the kernel for track force")
    public MaskForceField.MaskForceFieldType kernel = MaskForceField.MaskForceFieldType.Gaussian;

    @ModuleParameter
    @ModuleDescription("select maximum")
    public boolean max = false;

    @ModuleOutput
    @ModuleDescription("the output fibers volume")
    public Volume output;

    public VolumeFibersForce run()
    {
        Mask amask = this.attractMask != null ? this.attractMask : new Mask(this.input.getSampling());
        Mask rmask = this.repelMask != null ? this.repelMask : new Mask(this.input.getSampling());

        MaskForceField attract = new MaskForceField()
        {{
            this.input = amask;
            this.solids = VolumeFibersForce.this.attractSolids;
            this.scale = VolumeFibersForce.this.scale;
            this.kernel = VolumeFibersForce.this.kernel;
        }}.run();

        MaskForceField repel = new MaskForceField()
        {{
            this.input = rmask;
            this.solids = VolumeFibersForce.this.repelSolids;
            this.scale = VolumeFibersForce.this.scale;
            this.kernel = VolumeFibersForce.this.kernel;
        }}.run();

        Volume out = this.input.proto();
        for (Sample sample : this.input.getSampling())
        {
            if (this.input.valid(sample, this.mask))
            {
                Vect aval = attract.output.get(sample);
                Vect rval = repel.output.get(sample);

                Fibers model = new Fibers(this.input.get(sample));

                Vect probs = VectSource.createND(model.size());
                for (int i = 0; i < model.size(); i++)
                {
                    double frac = model.getFrac(i);
                    Vect line = model.getLine(i);

                    double alog = attract.loglik(line, aval);
                    double rlog = repel.loglik(line, rval);
                    double prob = Math.exp(alog + rlog);

                    probs.set(i, frac * prob);
                }

                double sum = probs.sum();

                if (sum > 0)
                {
                    probs.divEquals(sum);
                }
                else
                {
                    probs.setAll(1.0 / (double) probs.size());
                }

                if (this.max)
                {
                    int maxidx = probs.maxidx();
                    for (int i = 0; i < model.size(); i++)
                    {
                        if (i != maxidx)
                        {
                            model.setFrac(i, 0);
                        }
                    }
                }

                for (int i = 0; i < model.size(); i++)
                {
                    model.setStat(i, probs.get(i));
                }

                out.set(sample, model.getEncoding());
            }
        }

        this.output = out;

        return this;
    }
}
