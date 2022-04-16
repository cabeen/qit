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

package qit.data.modules.curves;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;

import java.util.List;
import java.util.Optional;

@ModuleDescription("Extract the connecting segments between the given regions")
@ModuleAuthor("Ryan Cabeen")
public class CurvesConnectRegions implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleInput
    @ModuleDescription("input regions (should have two distinct labels)")
    public Mask regions;

    @ModuleParameter
    @ModuleDescription("extract only the shortest connection")
    public boolean shortest = false;

    @ModuleParameter
    @ModuleDescription("extract only simple connections")
    public boolean simple = false;

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @Override
    public CurvesConnectRegions run()
    {
        this.output = this.apply(this.input, this.regions, this.shortest, this.simple);

        return this;
    }

    public static Curves apply(Curves input, Mask regions)
    {
        return apply(input, regions, false, false);
    }

    public static Curves apply(Curves input, Mask regions, boolean shortest, boolean simple)
    {
        Sampling sampling = regions.getSampling();
        Curves out = new Curves();

        for (Curves.Curve curve : input)
        {
            int[] labels = new int[curve.size()];
            for (int i = 0; i < labels.length; i++)
            {
                Sample sample = sampling.nearest(curve.get(i));

                if (sampling.contains(sample))
                {
                    labels[i] = regions.get(sample);
                }
            }

            int start = 0;
            int end = curve.size() - 1;

            while (start < curve.size() && labels[start] == 0)
            {
                start += 1;
            }

            while (end >= 0 && labels[end] == 0)
            {
                end -= 1;
            }

            int count = end - start;

            if (count < 2 || labels[end] == labels[start])
            {
                Logging.info("excluding curve");
                continue;
            }

            if (shortest)
            {
                List<Pair<Integer,Integer>> segs = Lists.newArrayList();

                int aidx = start; // the start of the first region
                int bidx = -1; // the start of the second region

                for (int i = start + 1; i < end; i++)
                {
                    int prev = labels[i - 1];
                    int curr = labels[i];

                    if (prev != curr)
                    {
                        // a label change occurred
                        if (prev == labels[aidx])
                        {
                            // we are leaving the first region, nothing to do
                        }

                        if (curr > 0 && curr != labels[aidx] && bidx == -1)
                        {
                            // we are entering the second region
                            bidx = i;
                        }

                        if (bidx != -1 && prev == labels[bidx])
                        {
                            // we are leaving the second region
                            segs.add(Pair.of(aidx, i));
                            aidx = bidx;
                            bidx = 0;
                        }
                    }
                }

                if (bidx >= 0)
                {
                    segs.add(Pair.of(aidx, end));
                }

                Optional<Pair<Integer,Integer>> minseg = Optional.empty();

                for (Pair<Integer,Integer> seg : segs)
                {
                    int len = seg.b - seg.a;

                    if (labels[seg.b] != labels[seg.a])
                    {
                        if (minseg.isEmpty() || len < minseg.get().b - minseg.get().a)
                        {
                            minseg = Optional.of(seg);
                        }
                    }
                }

                if (minseg.isPresent())
                {
                    int pcount = count;

                    start = minseg.get().a;
                    end = minseg.get().b;
                    count = end - start;

                    Logging.info("shortened curve from %d to %d", pcount, count);
                }
            }

            if (simple)
            {
                int steps = 0;

                for (int i = start + 1; i < end; i++)
                {
                    int prev = labels[i - 1];
                    int curr = labels[i];

                    if (prev != curr)
                    {
                        steps += 1;
                    }
                }

                if (steps > 2)
                {
                    Logging.info("excluding complex connection");
                    continue;
                }
            }

            Curves.Curve ncurve = out.add(count);
            for (int i = 0; i < count; i++)
            {
                int idx = start + i;
                ncurve.set(i, curve.get(idx));

                for (String attr : input.names())
                {
                    ncurve.set(attr, i, curve.get(attr, idx));
                }
            }
        }

        return out;
    }
}
