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

package qit.data.modules.mask;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sampling;
import qit.data.utils.MaskUtils;

import java.util.Map;

@ModuleDescription("Merge small regions using an adjacency graph")
@ModuleAuthor("Ryan Cabeen")
public class MaskRegionMerge implements Module
{
    @ModuleInput
    @ModuleDescription("input region input")
    public Mask input;

    @ModuleParameter
    @ModuleDescription("the input threshold")
    public double threshold = 10;

    @ModuleParameter
    @ModuleDescription("use a full 27-voxel neighborhood (default is 6-voxel)")
    public boolean full = false;

    @ModuleOutput
    @ModuleDescription("output input")
    public Mask output;

    @Override
    public MaskRegionMerge run()
    {
        Logging.info("started to merge regions");

        Sampling sampling = this.input.getSampling();
        double voxvol = sampling.voxvol();

        Logging.info("computing region statistics");
        Multimap<Integer, Integer> rag = MaskUtils.rag(this.input, this.full);
        Map<Integer, Integer> counts = MaskUtils.counts(this.input);
        Map<Integer, Integer> merge = Maps.newHashMap();

        for (Integer label : counts.keySet())
        {
            double volume = voxvol * counts.get(label);
            if (volume <= this.threshold)
            {
                Integer maxCount = null;
                Integer maxLabel = null;
                for (Integer nlabel : rag.get(label))
                {
                    int count = counts.get(nlabel);
                    if (maxCount == null || count > maxCount)
                    {
                        maxCount = count;
                        maxLabel = nlabel;
                    }
                }

                int nlabel = maxLabel == null ? 0 : maxLabel;
                merge.put(label, nlabel);
            }
            else
            {
                merge.put(label, label);
            }
        }

        Mask out = MaskUtils.map(input, merge);

        this.output = out;
        return this;
    }
}

