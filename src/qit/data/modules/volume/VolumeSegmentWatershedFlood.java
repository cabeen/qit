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
import qit.base.Global;
import qit.base.JsonDataset;
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
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskComponents;
import qit.data.source.MaskSource;
import qit.data.utils.MaskUtils;

import java.util.List;
import java.util.PriorityQueue;

@ModuleDescription("Segment a volume with Meyer's watershed flooding algorithm")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
@ModuleCitation("Beucher, S., & Meyer, F. (1992). The morphological approach to segmentation: the watershed transformation. OPTICAL ENGINEERING-NEW YORK-MARCEL DEKKER INCORPORATED-, 34, 433-433.")
public class VolumeSegmentWatershedFlood implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the volume channel")
    public Integer channel = 0;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("optional seed labels")
    public Mask seed;

    @ModuleParameter
    @ModuleDescription("preprocess with a median filter")
    public boolean median = false;

    @ModuleParameter
    @ModuleDescription("apply flood to the gradient")
    public boolean gradient = false;

    @ModuleParameter
    @ModuleDescription("seed from minima")
    public boolean minima = false;

    @ModuleParameter
    @ModuleDescription("seed from maxima")
    public boolean maxima = false;

    @ModuleParameter
    @ModuleDescription("use a full 27-voxel neighborhood (default is 6-voxel)")
    public boolean full = false;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output region labels")
    public Mask labels = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output boundary mask (watershed lines)")
    public Mask boundary = null;

    public VolumeSegmentWatershedFlood run()
    {
        Logging.info("started watershed flood segmentation");

        Volume volume = this.input;
        Sampling sampling = this.input.getSampling();
        int maxLabel = 0;

        if (this.median)
        {
            VolumeFilterMedian filter = new VolumeFilterMedian();
            filter.input = volume;
            volume = filter.run().output;
        }

        List<Integers> neighborhood = this.full ? Global.NEIGHBORS_27 : Global.NEIGHBORS_6;

        PriorityQueue<FloodElement> queue = new PriorityQueue<>();
        Mask outLabels = MaskSource.create(sampling);
        Mask outBoundary = MaskSource.create(sampling);

        if (this.seed != null)
        {
            outLabels.set(this.seed);

            for (Integer label : MaskUtils.listNonzero(outLabels))
            {
                maxLabel = Math.max(maxLabel, label);
            }
        }

        if (this.minima || this.maxima)
        {
            Logging.info("... initializing extrema");
            VolumeExtrema extremer = new VolumeExtrema();
            extremer.input = volume;
            extremer.mask = this.mask;
            extremer.channel = this.channel;
            extremer.minima = this.minima;
            extremer.maxima = this.maxima;
            Mask extrema = extremer.run().output;

            MaskComponents comper = new MaskComponents();
            comper.input = extrema;
            comper.full = this.full;
            Mask comps = comper.run().output;

            // make sure these are distinct from any seed labels
            for (Sample s : sampling)
            {
                if (comps.foreground(s) && outLabels.background(s))
                {
                    outLabels.set(s, comps.get(s) + maxLabel);
                }
            }

            for (Integer label : MaskUtils.listNonzero(outLabels))
            {
                maxLabel = Math.max(maxLabel, label);
            }
        }

        if (this.gradient)
        {
            Logging.info("... computing gradient");
            VolumeGradientMagnitude grader = new VolumeGradientMagnitude();
            grader.input = volume;
            grader.mask = this.mask;
            grader.channel = this.channel;
            grader.type = VolumeGradient.TYPE_CENTRAL;
            volume = grader.run().output;
        }

        Logging.info("... initializing");
        Global.assume(outLabels.getSampling().equals(sampling), "initial sampling does not match");

        Mask marked = outLabels.proto();
        for (Sample s : sampling)
        {
            if (outLabels.valid(s, this.mask) && outLabels.foreground(s))
            {
                for (Integers n : neighborhood)
                {
                    Sample ns = new Sample(s, n);
                    if (!outLabels.valid(s, this.mask) || !sampling.contains(ns) || outLabels.foreground(ns) || ns.equals(s) || marked.foreground(ns))
                    {
                        continue;
                    }

                    queue.offer(new FloodElement(s, ns, volume.get(ns, 0)));
                    marked.set(ns, 1);
                }
            }
        }

        Logging.info("... flooding");
        while (!queue.isEmpty())
        {
            FloodElement elem = queue.poll();
            int label = outLabels.get(elem.source);

            if (outLabels.foreground(elem.current))
            {
                continue;
            }

            boolean include = true;
            List<Sample> nexts = Lists.newArrayList();
            for (Integers n : neighborhood)
            {
                Sample ns = new Sample(elem.current, n);
                if (!sampling.contains(ns))
                {
                    continue;
                }

                if (outLabels.foreground(ns) && outLabels.get(ns) != label)
                {
                    include = false;
                    break;
                }
                else if (outBoundary.background(ns) && outLabels.background(ns))
                {
                    nexts.add(ns);
                }
            }

            if (include)
            {
                outLabels.set(elem.current, label);
                for (Sample next : nexts)
                {
                    queue.add(new FloodElement(elem.source, next, volume.get(next, 0)));
                }
            }
            else
            {
                outBoundary.set(elem.current, 1);
            }
        }

        Logging.info("finished watershed flood segmentation");
        this.labels = outLabels;
        this.boundary = outBoundary;

        return this;
    }

    private static class FloodElement extends JsonDataset implements Comparable<FloodElement>
    {
        private Sample source;
        private Sample current;
        private double gradient;

        public FloodElement(Sample source, Sample current, double gradient)
        {
            Global.assume(!source.equals(current), "invalid flood samples");
            this.source = source;
            this.current = current;
            this.gradient = gradient;
        }

        public boolean equals(Object o)
        {
            return this.current.equals(o);
        }

        public int hashCode()
        {
            return this.current.hashCode();
        }

        @Override
        public int compareTo(FloodElement e)
        {
            if (this.gradient < e.gradient)
            {
                return -1;
            }
            if (this.gradient > e.gradient)
            {
                return 1;
            }
            return 0;
        }

        @Override
        public FloodElement copy()
        {
            return new FloodElement(this.source, this.current, this.gradient);
        }
    }
}
