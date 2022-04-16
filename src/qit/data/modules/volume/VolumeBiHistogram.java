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
import qit.base.annot.*;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.volume.VolumeFilter;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Compute a histogram of a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeBiHistogram implements Module
{
    @ModuleInput
    @ModuleDescription("the volume for the x-dimension")
    public Volume x;

    @ModuleInput
    @ModuleDescription("the volume for the y-dimension")
    public Volume y;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the number of bins in x")
    public int xbins = 100;

    @ModuleParameter
    @ModuleDescription("the number of bins in y")
    public int ybins = 100;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply smoothing by the given amount")
    public Double smooth = null;

    @ModuleParameter
    @ModuleDescription("exclude counts outside the histogram range")
    public boolean exclude = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum in x")
    public Double xmin = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum in y")
    public Double ymin = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maximum in x")
    public Double xmax = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maximum in y")
    public Double ymax = null;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output breaks")
    public Volume breaks = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output mapping")
    public Mask mapping = null;

    public VolumeBiHistogram run()
    {
        Double myxmin = this.xmin;
        Double myxmax = this.xmax;

        if (myxmin == null || myxmax == null)
        {
            VolumeVoxelStats xstats = new VolumeVoxelStats().withInput(this.x).withMask(this.mask).run();
            myxmin = myxmin == null ? xstats.min : myxmin;
            myxmax = myxmax == null ? xstats.max : myxmax;
        }

        Double myymin = this.ymin;
        Double myymax = this.ymax;

        if (myymin == null || myymax == null)
        {
            VolumeVoxelStats ystats = new VolumeVoxelStats().withInput(this.y).withMask(this.mask).run();
            myymin = myymin == null ? ystats.min : myymin;
            myymax = myymax == null ? ystats.max : myymax;
        }

        double xspacing = (myxmax - myxmin) / (double) this.xbins;
        double yspacing = (myymax - myymin) / (double) this.ybins;

        Mask mymapping = MaskSource.create(this.x.getSampling());

        Sampling sampling = SamplingSource.create2D(0, 0, 1, 1, this.xbins, this.ybins);
        Volume myhist = VolumeSource.create(sampling);
        Volume mybreaks = VolumeSource.create(sampling, 2);

        for (Sample sample : this.x.getSampling())
        {
            if (this.x.valid(sample, this.mask))
            {
                double xval = this.x.get(sample, 0);
                double yval = this.y.get(sample, 0);

                int sx = MathUtils.round((xval - myxmin) / xspacing);
                int sy = MathUtils.round((yval - myymin) / yspacing);

                boolean inside = sx >= 0 && sx < this.xbins && sy >= 0 && sy < this.ybins;

                if (!this.exclude || inside)
                {
                    sx = sx < 0 ? 0 : sx >= this.xbins ? this.xbins - 1 : sx;
                    sy = sy < 0 ? 0 : sy >= this.ybins ? this.ybins - 1 : sy;

                    myhist.set(sx, sy, 0, 0, myhist.get(sx, sy, 0, 0) + 1);
                    mymapping.set(sample, sampling.index(sx, sy, 0));
                }
            }
        }

        for (Sample sample : sampling)
        {
            double xpos = myxmin + xspacing * sample.getI();
            double ypos = myymin + yspacing * sample.getJ();

            mybreaks.set(sample, VectSource.create2D(xpos, ypos));
        }

        if (this.smooth != null)
        {
            int vnum = (int) Math.ceil(this.smooth);
            List<Volume> filters = Lists.newArrayList();
            filters.add(VolumeSource.gauss(sampling, vnum, 1, 1, this.smooth * xspacing));
            filters.add(VolumeSource.gauss(sampling, 1, vnum, 1, this.smooth * yspacing));

            for (Volume filter : filters)
            {
                VolumeFilter filterer = new VolumeFilter();
                filterer.withFilter(filter);
                filterer.withNormalize(true);
                filterer.withBoundary(true);

                myhist = filterer.withInput(myhist).withMask(this.mask).run().getOutput();
            }
        }

        this.output = myhist;
        this.breaks = mybreaks;
        this.mapping = mymapping;

        return this;
    }
}
