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
import com.google.common.collect.Queues;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qit.data.datasets.Affine;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.vects.VectsRegisterLinear;
import qit.data.source.VolumeSource;

import java.util.List;
import java.util.Queue;

@ModuleDescription("Flip the voxel values of volume along coordinate axes")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFloodFill implements Module
{
    @ModuleInput
    @ModuleDescription("input volume of values for filling")
    private Volume input;

    @ModuleInput
    @ModuleDescription("a mask indicating the source, i.e. which voxels of the input are used for filling")
    private Mask source;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask used for restricting the voxel flooded (must be convex, but this is not checked)")
    private Mask dest;

    @ModuleParameter
    @ModuleDescription("maximum number of fixing passes")
    private int fix = 0;

    @ModuleOutput
    @ModuleDescription("output volume of flood values")
    private Volume outputFlood;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output distance field")
    private Volume outputDist;

    public VolumeFloodFill run()
    {
        Sampling sampling = this.input.getSampling();

        Volume dist = this.input.proto(1);
        Volume flood = this.input.proto();

        Logging.info("started flood fill");

        double max = sampling.world(sampling.start()).dist(sampling.world(sampling.last()));

        for (Sample s : sampling)
        {
            dist.set(s, 0, max);
        }

        Logging.info("... initializing");

        Mask visited = new Mask(sampling);
        Mask closest = new Mask(sampling);

        List<Sample> batchNext = Lists.newArrayList();
        for (Sample s : sampling)
        {
            if (this.input.valid(s, this.source))
            {
                visited.set(s, 2);
                closest.set(s, sampling.index(s));
                dist.set(s, 0);
                flood.set(s, this.input.get(s));

                for (Integers n : Global.NEIGHBORS_27)
                {
                    Sample ns = s.offset(n);
                    if (this.input.valid(ns, this.dest) && this.source.background(ns))
                    {
                        batchNext.add(ns);
                        visited.set(ns, 1);
                    }
                }
            }
        }

        while (batchNext.size() > 0)
        {
            Logging.info("... running flood fill batch size " + batchNext.size());

            Queue<Sample> batchCurrent = Queues.newLinkedBlockingQueue();
            for (Sample s : batchNext)
            {
                batchCurrent.offer(s);
            }
            batchNext.clear();

            while (batchCurrent.size() > 0)
            {
                Sample s = batchCurrent.poll();

                Vect p = sampling.world(s);
                double d = dist.get(s, 0);
                Integer c = null;
                Vect f = flood.get(s);

                for (Integers n : Global.NEIGHBORS_27)
                {
                    Sample ns = s.offset(n);
                    if (this.input.valid(ns, this.dest))
                    {
                        int v = visited.get(ns);
                        if (v == 2)
                        {
                            int idx = closest.get(ns);
                            double nd = sampling.world(idx).dist(p);

                            if (nd <= d)
                            {
                                d = nd;
                                c = idx;
                                f = this.input.get(idx);
                            }
                        }
                        else if (v == 0)
                        {
                            batchNext.add(ns);
                            visited.set(ns, 1);
                        }
                    }
                }

                //Global.assume(c != null, "invalid flood fill pass");

                if (c != null)
                {
                    visited.set(s, 2);
                    closest.set(s, c);
                    dist.set(s, 0, d);
                    flood.set(s, f);
                }
                else
                {
                    batchNext.add(s);
                }
            }
        }

        Logging.info("... running passes to fix");

        for (int i = 0; i < this.fix; i++)
        {
            int count = 0;
            visited.setAll(0);

            for (Sample s : sampling)
            {
                int si = sampling.index(s);

                if (si % 2 == 0 ^ i % 2 == 0)
                {
                    continue;
                }

                Vect p = sampling.world(s);
                double d = dist.get(s, 0);
                Integer c = closest.get(s);
                Vect f = flood.get(s);

                boolean changed = false;

                for (Integers n : Global.NEIGHBORS_27)
                {
                    Sample ns = s.offset(n);
                    if (this.input.valid(ns, this.dest) && visited.background(ns))
                    {
                        int idx = closest.get(ns);
                        double nd = sampling.world(idx).dist(p);

                        if (nd < d)
                        {
                            d = nd;
                            c = idx;
                            f = this.input.get(idx);

                            changed = true;
                        }
                    }
                }

                if (changed)
                {
                    visited.set(s, 1);
                    closest.set(s, c);
                    dist.set(s, 0, d);
                    flood.set(s, f);

                    count += 1;
                }
            }

            Logging.info("...... fixed voxels: " + count);
        }

        Logging.info("finished flood fill");

        this.outputFlood = flood;
        this.outputDist = dist;

        return this;
    }
}
