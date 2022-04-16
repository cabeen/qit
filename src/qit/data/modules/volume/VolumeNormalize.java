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


package qit.data.modules.volume;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

@ModuleDescription("Normalize the values of a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeNormalize implements Module
{
    public enum VolumeNormalizeType
    {
        Unit, UnitMax, UnitSum, UnitMean, Mean, UnitMaxFraction, UnitMeanFraction, Gaussian
    }

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("type of normalization")
    public VolumeNormalizeType type = VolumeNormalizeType.UnitMax;

    @ModuleParameter
    @ModuleDescription("a fraction for normalization (only applies to some types of normalization)")
    public double fraction = 0.5;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    public VolumeNormalize run()
    {
        final VectOnlineStats stats = new VectOnlineStats();

        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : this.input.getSampling())
            {
                if (this.input.valid(sample, this.mask))
                {
                    stats.update(this.input.get(sample, d));
                }
            }
        }

        VectFunction fun = new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                switch (type)
                {
                    case Unit:
                    {
                        double offset = stats.min;
                        double scale = MathUtils.eq(stats.min, stats.max) ? 0.0 : 1.0 / (stats.max - stats.min);
                        output.set(input.minus(offset).times(scale));
                        return;
                    }
                    case UnitMax:
                    {
                        double offset = 0.0;
                        double scale = MathUtils.zero(stats.max) ? 0.0 : 1.0 / stats.max;
                        output.set(input.minus(offset).times(scale));
                        return;
                    }
                    case UnitSum:
                    {
                        double offset = 0.0;
                        double scale = MathUtils.zero(stats.sum) ? 0.0 : 1.0 / stats.sum;
                        output.set(input.minus(offset).times(scale));
                        return;
                    }
                    case UnitMean:
                    {
                        double scale = MathUtils.zero(stats.mean) ? 0.0 : 1.0 / stats.mean;

                        for (int i = 0; i < input.size(); i++)
                        {
                            double cv = Math.min(1, Math.max(0, scale * input.get(i)));
                            output.set(i, cv);
                        }
                        return;
                    }
                    case Mean:
                    {
                        double scale = MathUtils.zero(stats.mean) ? 0.0 : 1.0 / stats.mean;

                        for (int i = 0; i < input.size(); i++)
                        {
                            double cv = scale * input.get(i);
                            output.set(i, cv);
                        }
                        return;
                    }
                    case UnitMaxFraction:
                    {
                        double scale = MathUtils.zero(stats.mean) ? 0.0 : 1.0 / (VolumeNormalize.this.fraction * stats.max);

                        for (int i = 0; i < input.size(); i++)
                        {
                            double cv = Math.min(1, Math.max(0, scale * input.get(i)));
                            output.set(i, cv);
                        }
                        return;
                    }
                    case UnitMeanFraction:
                    {
                        double scale = MathUtils.zero(stats.mean) ? 0.0 : 1.0 / (VolumeNormalize.this.fraction * stats.mean);

                        for (int i = 0; i < input.size(); i++)
                        {
                            double cv = Math.min(1, Math.max(0, scale * input.get(i)));
                            output.set(i, cv);
                        }
                        return;
                    }
                    case Gaussian:
                    {
                        double offset = stats.mean;
                        double scale = MathUtils.zero(stats.std) ? 0.0 : 1.0 / stats.std;
                        output.set(input.minus(offset).times(scale));
                        return;
                    }
                }
            }

        }.init(this.input.getDim(), this.input.getDim());

        this.output = new VolumeFunction(fun).withInput(this.input).withMask(this.mask).withMessages(false).run();

        return this;
    }

}
