/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.modules.volume;

import com.google.common.collect.Lists;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Table;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;
import qit.data.utils.volume.VolumeFilter;
import qit.data.utils.volume.VolumeStats;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.structs.Histogram;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Compute a histogram of a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeHistogram implements Module
{
    public enum Bound { Extrema, Quartile, HalfStd, OneStd, TwoStd, ThreeStd, User }

    @ModuleInput
    @ModuleDescription("the input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the number of bins")
    public int bins = 256;

    @ModuleParameter
    @ModuleDescription("the method for computing the histogram lower bound")
    public Bound lower = Bound.Extrema;

    @ModuleParameter
    @ModuleDescription("the method for computing the histogram upper bound")
    public Bound upper = Bound.Extrema;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify a specific lower bound for user defined mode")
    public Double mylower = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify a specific upper bound for user defined mode")
    public Double myupper = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply a lower threshold before computing the histogram")
    public Double lowerThresh = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply an upper threshold before computing the histogram")
    public Double upperThresh = null;

    @ModuleParameter
    @ModuleDescription("normalize the counts by the total count")
    public boolean normalize = false;

    @ModuleParameter
    @ModuleDescription("compute the cdf")
    public boolean cdf = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply Gaussian smoothing (see hdata)")
    public Double smooth = null;

    @ModuleParameter
    @ModuleDescription("interpret the smoothing parameter relative to the data intensities (default is bins)")
    public boolean hdata = false;

    @ModuleParameter
    @ModuleDescription("print a the histogram to standard output")
    public boolean print = false;

    @ModuleParameter
    @ModuleDescription("the value column name")
    public String value = "value";

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("an output table")
    public Table output;

    public VolumeHistogram run()
    {
        Histogram histogram = this.get();

        if (this.print)
        {
            System.out.println();
            System.out.println("Lower break: " + histogram.getBoundAbove(0));
            System.out.println("Mode: " + histogram.mode());
            System.out.println("Upper break: " + histogram.getBoundBelow(histogram.size() - 1));
            System.out.println("Entropy: " + histogram.entropy());
            System.out.println();

            double maxval = 50.0 / histogram.maxValue();

            for (int i = 0; i < histogram.getBins(); i++)
            {
                double bound = histogram.getBoundBelow(i);
                String output = "*".repeat(MathUtils.round(histogram.getData(i) * maxval));
                System.out.println(String.format("%g: %s", bound, output));
            }
        }

        Table out = histogram.table();

        if (!"value".equals(this.value))
        {
            out = TableUtils.rename(out, String.format("%s=value", this.value));
            out = TableUtils.remove(out, "value");
        }

        this.output = out;

        return this;
    }

    public Histogram get()
    {
        Mask myMask = this.mask;

        if (this.lowerThresh != null)
        {
            myMask = MaskUtils.and(VolumeThreshold.apply(this.input, this.lowerThresh), myMask);
        }

        if (this.upperThresh != null)
        {
            myMask = MaskUtils.and(MaskUtils.invert(VolumeThreshold.apply(this.input, this.upperThresh)), myMask);
        }

        VolumeVoxelStats stats = new VolumeVoxelStats().withInput(this.input).withMask(myMask).run();

        Double min = Double.MAX_VALUE;
        switch (this.lower)
        {
            case User:
                min = this.mylower;
                break;
            case Extrema:
                min = stats.min;
                break;
            case Quartile:
                min = stats.qlow;
                break;
            case HalfStd:
                min = stats.mean - 0.5 * stats.std;
                break;
            case OneStd:
                min = stats.mean - 1.0 * stats.std;
                break;
            case TwoStd:
                min = stats.mean - 2.0 * stats.std;
                break;
            case ThreeStd:
                min = stats.mean - 3.0 * stats.std;
                break;
        }

        Double max = Double.MAX_VALUE;
        switch (this.upper)
        {
            case User:
                max = this.myupper;
                break;
            case Extrema:
                max = stats.max;
                break;
            case Quartile:
                max = stats.qhigh;
                break;
            case HalfStd:
                max = stats.mean + 0.5 * stats.std;
                break;
            case OneStd:
                max = stats.mean + 1.0 * stats.std;
                break;
            case TwoStd:
                max = stats.mean + 2.0 * stats.std;
                break;
            case ThreeStd:
                max = stats.mean + 3.0 * stats.std;
                break;
        }

        Histogram histogram = Histogram.create(this.bins, min, max);

        Volume out = this.input.proto();
        for (int d = 0; d < this.input.getDim(); d++)
        {
            for (Sample sample : out.getSampling())
            {
                if (this.input.valid(sample, myMask))
                {
                    histogram.update(this.input.get(sample, d));
                }
            }
        }

        if (this.smooth != null)
        {
            if (this.hdata)
            {
                histogram.smoothData(this.smooth);
            }
            else
            {
                histogram.smoothBins(this.smooth);
            }
        }

        if (this.cdf)
        {
            histogram.cdf();
        }

        return histogram;
    }
}
