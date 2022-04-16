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
import qit.data.datasets.*;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.data.utils.volume.VolumeFilter;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Compute a histogram of a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeEstimateLookup implements Module
{
    @ModuleInput
    @ModuleDescription("the volume for the x-dimension")
    public Volume from;

    @ModuleInput
    @ModuleDescription("the volume for the y-dimension")
    public Volume to;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the number of bins in x")
    public int bins = 100;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum in x")
    public Double xmin = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maximum in x")
    public Double xmax = null;

    @ModuleParameter
    @ModuleDescription("the name field name for the input value")
    public String inputField = "input";

    @ModuleParameter
    @ModuleDescription("the name field name for the output value")
    public String outputField = "output";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output breaks")
    public Volume breaks = null;

    public VolumeEstimateLookup run()
    {
        Double myxmin = this.xmin;
        Double myxmax = this.xmax;

        if (myxmin == null || myxmax == null)
        {
            VolumeVoxelStats xstats = new VolumeVoxelStats().withInput(this.from).withMask(this.mask).run();
            myxmin = myxmin == null ? xstats.min : myxmin;
            myxmax = myxmax == null ? xstats.max : myxmax;
        }

        double xspacing = (myxmax - myxmin) / (double) this.bins;

        List<VectOnlineStats> stats = Lists.newArrayList();
        for (int i = 0; i < this.bins; i++)
        {
            stats.add(new VectOnlineStats());
        }

        for (Sample sample : this.from.getSampling())
        {
            if (this.from.valid(sample, this.mask))
            {
                double xval = this.from.get(sample, 0);
                double yval = this.to.get(sample, 0);

                int idx = MathUtils.round((xval - myxmin) / xspacing);
                idx = idx < 0 ? 0 : idx > this.bins - 1 ? this.bins - 1 : idx;

                stats.get(idx).update(yval);
            }
        }

        Table out = new Table();
        out.withField(this.inputField);
        out.withField(this.outputField);
        out.withField(this.outputField + "_mean");
        out.withField(this.outputField + "_std");
        out.withField(this.outputField + "_min");
        out.withField(this.outputField + "_max");
        out.withField(this.outputField + "_num");

        for (int i = 0; i < this.bins; i++)
        {
            VectOnlineStats mystats = stats.get(i);

            Record record = new Record();

            record.with(this.inputField, String.valueOf(myxmin + i * xspacing));
            record.with(this.outputField, String.valueOf(mystats.mean));
            record.with(this.outputField + "_mean", String.valueOf(mystats.mean));
            record.with(this.outputField + "_std", String.valueOf(mystats.std));
            record.with(this.outputField + "_min", String.valueOf(mystats.min));
            record.with(this.outputField + "_max", String.valueOf(mystats.max));
            record.with(this.outputField + "_num", String.valueOf(mystats.num));

            out.addRecord(record);
        }

        this.output = out;

        return this;
    }
}
