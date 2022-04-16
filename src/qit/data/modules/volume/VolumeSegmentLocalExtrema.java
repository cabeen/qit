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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.data.datasets.*;
import qit.data.modules.mask.MaskSort;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.math.structs.DisjointSet;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Segment a volume based on local extrema")
@ModuleAuthor("Ryan Cabeen")
public class VolumeSegmentLocalExtrema implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("find minima instead of maxima")
    public boolean minima = false;

    @ModuleParameter
    @ModuleDescription("use a full 27-voxel neighborhood (default is 6-voxel)")
    public boolean full = false;

    @ModuleOutput
    @ModuleDescription("output region labels")
    public Mask output = null;

    public VolumeSegmentLocalExtrema run()
    {
        Sampling sampling = this.input.getSampling();
        List<Integers> neighborhood = this.full ? Global.NEIGHBORS_27 : Global.NEIGHBORS_6;
        Mask labels = new Mask(sampling);
        int currentLabel = 1;

        Map<Integer,Integer> mapping = Maps.newHashMap();
        Map<Integer, Pair<Sample, Double>> peaks = Maps.newHashMap();

        for (Sample sample : sampling)
        {
            if (!this.input.valid(sample, this.mask) || labels.foreground(sample))
            {
                continue;
            }

            Sample previous = sample;
            Sample next = sample;

            while (true)
            {
                double value = this.input.get(previous, 0);

                for (Integers offset : neighborhood)
                {
                    Sample neighbor = new Sample(previous, offset);
                    if (!previous.equals(neighbor) && sampling.contains(neighbor) && this.input.valid(neighbor, this.mask))
                    {
                        double nvalue = this.input.get(neighbor, 0);

                        if ((this.minima && nvalue < value) || nvalue > value)
                        {
                            value = nvalue;
                            next = neighbor;
                        }
                    }
                }

                labels.set(previous, currentLabel);

                if (next.equals(previous))
                {
                    peaks.put(currentLabel, Pair.of(previous, value));
                    break;
                }

                if (labels.foreground(next))
                {
                    mapping.put(currentLabel, labels.get(next));
                    peaks.put(currentLabel, peaks.get(labels.get(next)));
                    break;
                }

                previous = next;
            }

            currentLabel += 1;
        }


        for (Sample sample : sampling)
        {
            int initial = labels.get(sample);
            int label = initial;

            while (mapping.containsKey(label))
            {
                label = mapping.get(label);
            }

            labels.set(sample, label);

            // path compression
            // mapping.put(initial, label);
        }

        Map<Integer,Integer> counts = MaskUtils.counts(labels);
        Map<Integer, Integer> lookup = MathUtils.remap(counts);


        for (Sample sample : labels.getSampling())
        {
            if (labels.foreground(sample))
            {
                labels.set(sample, lookup.get(labels.get(sample)));
            }
        }

        this.output = labels;

        return this;
    }
}