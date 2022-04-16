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

package qit.data.modules.mri.dwi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.utils.PathUtils;
import qit.data.datasets.*;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.data.utils.volume.VolumeFunction;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@ModuleDescription("normalize a mri weighted image volume by the baseline signal")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiNormalize implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the input gradients")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a grouping of scans with similar TE/TR (otherwise all scans are assumed to be part of one group)")
    public String grouping;

    @ModuleParameter
    @ModuleDescription("normalize the signal to the unit interval (divide by average baseline by voxel)")
    public boolean unit = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("normalize the signal to have a given mean value across the entire volume")
    public Double mean = null;

    @ModuleOutput
    @ModuleDescription("the output baseline volume")
    public Volume output;

    public VolumeDwiNormalize run()
    {
        final int num = this.gradients.size();
        final List<Integer> grouping = Lists.newArrayList();
        for (int i = 0; i < num; i++)
        {
            grouping.add(1);
        }

        if (this.grouping != null)
        {
            if (PathUtils.exists(this.grouping))
            {
                try
                {
                    Vects vects = Vects.read(this.grouping);
                    if (vects.getDim() != 1)
                    {
                        vects = vects.transpose();
                    }

                    Global.assume(vects.getDim() == 1, "invalid grouping: " + this.grouping);
                    Global.assume(vects.size() == num, "invalid grouping: " + this.grouping);

                    for (int i = 0; i < num; i++)
                    {
                        grouping.set(i, (int) Math.round(vects.get(i).get(0)));
                    }
                }
                catch (Exception e)
                {
                    Logging.error("invalid grouping: " + this.grouping);
                }
            }
            else
            {
                String[] tokens = this.grouping.split(",");
                Global.assume(tokens.length == num, "invalid grouping");
                for (int i = 0; i < num; i++)
                {
                    grouping.set(i, Integer.valueOf(tokens[i]));
                }
            }
        }

        final List<Integer> baselineIdx = gradients.getBaselineIdx();

        final VectOnlineStats vstats = new VectOnlineStats();
        if (this.mean != null)
        {
            for (Sample sample : this.input.getSampling())
            {
                if (this.input.valid(sample, this.mask))
                {
                    for (int idx : baselineIdx)
                    {
                        vstats.update(this.input.get(sample, idx));
                    }
                }
            }

            Logging.info("detected mean baseline: " + vstats.mean);
        }

        Supplier<VectFunction> factory = () -> new VectFunction()
        {
            public void apply(Vect inv, Vect outv)
            {
                VectOnlineStats total = new VectOnlineStats();
                Map<Integer, VectOnlineStats> stats = Maps.newHashMap();

                for (int idx : baselineIdx)
                {
                    int group = grouping.get(idx);
                    double value = inv.get(idx);

                    if (!stats.containsKey(group))
                    {
                        stats.put(group, new VectOnlineStats());
                    }

                    stats.get(group).update(value);
                    total.update(value);
                }

                for (int i = 0; i < num; i++)
                {
                    int group = grouping.get(i);
                    double value = inv.get(i);

                    Global.assume(stats.containsKey(group), "invalid grouping, no baseline found for group: " + group);
                    double base = stats.get(group).mean;
                    base = MathUtils.zero(base) ? 1.0 : base;

                    double norm = VolumeDwiNormalize.this.unit ? value / base : total.mean * value / base;

                    if (VolumeDwiNormalize.this.mean != null)
                    {
                        norm = VolumeDwiNormalize.this.mean * norm / vstats.mean;
                    }

                    outv.set(i, norm);
                }
            }
        }.init(this.gradients.size(), this.gradients.size());

        this.output = new VolumeFunction(factory).withInput(this.input).withMask(this.mask).run();

        return this;
    }
}
