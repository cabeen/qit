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

package qit.data.modules.mask;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.modules.volume.VolumeGradient;
import qit.data.utils.MaskUtils;

import java.util.function.Function;

@ModuleDescription("Compute a force field in the ambient space around the regions")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class MaskForceField implements Module
{
    public enum MaskForceFieldType
    {
        Gaussian, Box, Triangle, Epanechnikov, Gravity
    }

    @ModuleInput
    @ModuleDescription("the input mask")
    public Mask input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input solids")
    public Solids solids;

    @ModuleParameter
    @ModuleDescription("the kernel of force field to compute")
    public MaskForceFieldType kernel = MaskForceFieldType.Gaussian;

    @ModuleParameter
    @ModuleDescription("use the given field scaleCamera")
    public double scale = 1.0;

    @ModuleParameter
    @ModuleDescription("use the given field gain")
    public double gain = 1.0;

    @ModuleParameter
    @ModuleDescription("use the given field orientation")
    public boolean repel = false;

    @ModuleOutput
    @ModuleDescription("the output")
    public Volume output;

    public MaskForceField run()
    {
        final Mask regions = new MaskSet()
        {{
            this.input = MaskForceField.this.input;
            this.solids = MaskForceField.this.solids;
        }}.run().output;

        Function<Double, Double> function = function();
        Volume out = this.input.protoVolume(3);

        for (Integer lab : MaskUtils.listNonzero(regions))
        {
            final Mask region = new MaskExtract()
            {{
                this.input = regions;
                this.label = String.valueOf(lab);
            }}.run().output;

            final Volume dist = new MaskDistanceTransform()
            {{
                this.input = region;
                this.signed = true;
            }}.run().output;


            final Volume grad = new VolumeGradient()
            {{
                this.input = dist;
            }}.run().output;

            for (Sample sample : grad.getSampling())
            {
                double mval = function.apply(dist.get(sample, 0) / this.scale);
                Vect dval = grad.get(sample).normalize().times(mval);

                out.set(sample, out.get(sample).plus(dval));
            }
        }

        this.output = out;

        return this;
    }

    public double loglik(Vect input, Vect force)
    {
        double mag = force.norm();
        Vect dir = force.divSafe(mag);
        double cost = input.dot(dir);

        if (this.repel)
        {
            cost = 1 - cost;
        }

        double loglik = this.gain * mag * cost;

        return loglik;
    }

    private Function<Double, Double> function()
    {
        switch (this.kernel)
        {
            case Gaussian:
                return x -> Math.exp(-0.5 * x * x);
            case Box:
                return x -> x < 1.0 ? 1.0 : 0.0;
            case Triangle:
                return x -> x < 1.0 ? 1.0 - this.scale : 0.0;
            case Epanechnikov:
                return x -> x <= 1.0 ? 0.75 * (1.0 - x * x) : 0;
            case Gravity:
                return x -> 1.0 / (x + 1);
            default:
                throw new RuntimeException("invalid kernel kernel: " + this.kernel);
        }
    }
}
