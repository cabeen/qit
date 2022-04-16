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

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("enhance the intensity contrast of a given volume.  The output will have a range of zero to one.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeEnhanceContrast implements Module
{
    public enum VolumeEnhanceContrastType
    {
        Histogram, Ramp, RampGauss, Mean, None
    }

    public enum VolumeEnhanceContrastSqueeze
    {
        Square, Root, Sine, SquineLow, SquineHigh, None
    }

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the type of contrast enhancement")
    public VolumeEnhanceContrastType type = VolumeEnhanceContrastType.Histogram;

    @ModuleParameter
    @ModuleDescription("the type of squeezing")
    public VolumeEnhanceContrastSqueeze squeeze = VolumeEnhanceContrastSqueeze.None;

    @ModuleParameter
    @ModuleDescription("the number of histogram bins")
    public int bins = 1024;

    @ModuleParameter
    @ModuleDescription("the Gaussian bandwidth")
    public double gauss = 3.0;

    @ModuleParameter
    @ModuleDescription("the intensity scaling")
    public double scale = 1.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("smooth the histogram by adding the given proportion of the total (reduces effect of outliers)")
    public Double smooth = null;

    @ModuleParameter
    @ModuleDescription("remove background voxels")
    public boolean nobg = false;

    @ModuleParameter
    @ModuleDescription("invert the intensities on a unit interval")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("specify a threshold for background removal")
    public double thresh = 1e-6;

    @ModuleOutput
    @ModuleDescription("output enhanced image")
    public Volume output;

    public VolumeEnhanceContrast run()
    {
        switch (this.type)
        {
            case Histogram:
                this.output = histogram();
                break;
            case Mean:
                this.output = mean();
                break;
            case Ramp:
                this.output = ramp();
                break;
            case RampGauss:
                this.output = rampgauss();
                break;
            case None:
                this.output = this.input.copy();
                break;
        }

        for (Sample sample : this.output.getSampling())
        {
            if (this.valid(sample, 0))
            {
                Vect out = this.output.get(sample);
                out.timesEquals(this.scale);

                if (this.invert)
                {
                    out.timesEquals(-1.0);
                    out.plusEquals(1.0);
                }

                this.output.set(sample, out);
            }
        }

        return this;
    }

    private boolean valid(Sample sample, int d)
    {
        if (!this.input.valid(sample, this.mask))
        {
            return false;
        }

        if (this.nobg && this.input.get(sample, d) < this.thresh)
        {
            return false;
        }

        return true;
    }

    public Volume mean()
    {
        Volume out = this.input.proto();

        VectOnlineStats stats = new VectOnlineStats();

        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.valid(sample, d))
                {
                    double value = this.input.get(sample, d);
                    stats.update(value);
                }
            }
        }

        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.valid(sample, d))
                {
                    double value = this.input.get(sample, d);
                    double nvalue = (value - stats.min) / (stats.mean - stats.min);

                    out.set(sample, d, nvalue);
                }
            }
        }

        return out;
    }

    public Volume rampgauss()
    {
        Volume out = this.input.proto();

        VectOnlineStats stats = new VectOnlineStats();

        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.valid(sample, d))
                {
                    double value = this.input.get(sample, d);
                    stats.update(value);
                }
            }
        }

        Logging.info("min: " + stats.min);
        Logging.info("max: " + stats.max);
        Logging.info("mean: " + stats.mean);
        Logging.info("std: " + stats.std);

        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.valid(sample, d))
                {
                    double value = this.input.get(sample, d);
                    double nvalue = 0.5 + (value - stats.mean) / (this.gauss * stats.std * 2.0);
                    nvalue = this.squeezeit(nvalue);

                    out.set(sample, d, nvalue);
                }
            }
        }

        return out;
    }

    public Volume ramp()
    {
        Volume out = this.input.proto();

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.valid(sample, d))
                {
                    double value = this.input.get(sample, d);
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
        }

        double delta = MathUtils.eq(min, max) ? 1.0 : max - min;
        Logging.info("min: " + min);
        Logging.info("max: " + max);
        Logging.info("delta: " + delta);

        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.valid(sample, d))
                {
                    double value = this.input.get(sample, d);
                    double nvalue = (value - min) / delta;
                    nvalue = this.squeezeit(nvalue);

                    out.set(sample, d, nvalue);
                }
            }
        }

        return out;
    }

    private Volume histogram()
    {
        Volume out = this.input.proto();

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;

        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.valid(sample, d))
                {
                    double value = this.input.get(sample, d);
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
        }

        double delta = MathUtils.eq(min, max) ? 1.0 : max - min;

        int[] counts = new int[this.bins];
        double[] breaks = new double[this.bins];

        for (int b = 0; b < this.bins; b++)
        {
            breaks[b] = min + (b + 1) * delta / (double) this.bins;
        }

        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.valid(sample, d))
                {
                    double value = this.input.get(sample, d);

                    for (int b = 0; b < this.bins; b++)
                    {
                        if (value <= breaks[b])
                        {
                            counts[b] += 1;
                            break;
                        }
                    }
                }
            }
        }

        if (this.smooth != null)
        {
            int total = MathUtils.sum(counts);
            int change = (int) (this.smooth * total);

            for (int b = 0; b < this.bins; b++)
            {
                counts[b] = Math.max(0, counts[b] + change);
            }
        }

        double[] cumpdf = MathUtils.cumpdf(counts);

        List<Pair<Double, Double>> pairs = Lists.newArrayList();

        pairs.add(Pair.of(min, 0.0d));
        for (int b = 0; b < this.bins; b++)
        {
            pairs.add(Pair.of(breaks[b], cumpdf[b]));
        }

        VectFunction map = VectFunctionSource.linearInterp(pairs);

        for (int d = 0;  d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.valid(sample, d))
                {
                    double value = this.input.get(sample, d);
                    double nvalue = map.apply(VectSource.create1D(value)).get(0);
                    nvalue = this.squeezeit(nvalue);

                    out.set(sample, d, nvalue);
                }
            }
        }

        return out;
    }

    public double squeezeit(double value)
    {
        value = Math.min(1.0, Math.max(0.0, value));
        switch (this.squeeze)
        {
            case Square:
                return value * value;
            case Root:
                return Math.sqrt(value);
            case Sine:
                return MathUtils.unitsine(value);
            case SquineLow:
                return MathUtils.square(MathUtils.unitsine(value));
            case SquineHigh:
                return 1.0 - MathUtils.square(MathUtils.unitsine(value + 1));
            default:
                return value;
        }
    }

    public static Volume apply(Volume volume)
    {
        return new VolumeEnhanceContrast()
        {{
            this.input = volume;
            this.nobg = true;
            this.type = VolumeEnhanceContrastType.Histogram;
        }}.run().output;
    }
}
