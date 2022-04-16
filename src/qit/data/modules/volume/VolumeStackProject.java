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

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Triple;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.utils.volume.VolumeStackUtils;

import java.io.IOException;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@ModuleDescription("Compute the maximum or minimum intensity projection of an image stack")
@ModuleAuthor("Ryan Cabeen")
public class VolumeStackProject implements Module
{
    private enum StatisticMode
    {
        Max, Min, Mean, Sum
    }

    private enum ProjectMode
    {
        FullZ, SlabZ, SlabFracZ, FullX, FullY
    }

    @ModuleParameter
    @ModuleDescription("input filename pattern (e.g. %04d will be replaced with 0000, 0001, etc. or e.g. %d will be replaced with 0, 1, etc.)")
    public String input = "input/slice%04d.png";

    @ModuleParameter
    @ModuleDescription("use a minimum intensity projection (default is maximum intensity projection)")
    public boolean min = false;

    @ModuleParameter
    @ModuleDescription("project slabs of a given stack fraction (only relevant to SlabFracZ)")
    public ProjectMode mode = ProjectMode.FullZ;

    @ModuleParameter
    @ModuleDescription("the statistic to use for projection")
    public StatisticMode statistic = StatisticMode.Max;

    @ModuleParameter
    @ModuleDescription("project slabs of a given thickness (only relevant to SlabZ)")
    public Integer slabCount = 100;

    @ModuleParameter
    @ModuleDescription("use a given step amount (only relevant to the SlabZ)")
    public Integer slabStep = 100;

    @ModuleParameter
    @ModuleDescription("project slabs of a given stack fraction (only relevant to SlabFracZ)")
    public Double slabFraction = 0.1;

    @ModuleParameter
    @ModuleDescription("start at the given index projecting pixels in x")
    public int xstart = 0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("end at the given index projecting pixels in x")
    public Integer xend = null;

    @ModuleParameter
    @ModuleDescription("start at the given index projecting pixels in y")
    public int ystart = 0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("end at the given index projecting pixels in y")
    public Integer yend = null;

    @ModuleParameter
    @ModuleDescription("start at the given index when loading slices")
    public int dstart = 0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("end at the given index when loading slices")
    public Integer dend = null;

    @ModuleParameter
    @ModuleDescription("step the given amount when loading slices")
    public int dstep = 1;

    @ModuleParameter
    @ModuleDescription("the output stack (use %d if you specify a slab)")
    public String output = "output.png";

    @Override
    public VolumeStackProject run() throws IOException
    {
        List<Integer> sidx = VolumeStackUtils.detect(this.input, this.dstart, this.dend, this.dstep);

        Global.assume(sidx.size() > 0, "no sidx were found!");

        Volume ref = Volume.read(String.format(this.input, sidx.get(0)));
        Sampling sref = ref.getSampling();
        Global.assume(sref.numK() == 1, "expected 2D input");

        final int ni = ref.getSampling().numI();
        final int nj = ref.getSampling().numJ();
        final int nk = sidx.size();

        final int istart = this.xstart;
        final int iend = this.xend != null ? this.xend : ni;

        final int jstart = this.ystart;
        final int jend = this.yend != null ? this.yend : nj;

        Logging.info("detected number of slices to process: " + nk);

        Function<Integer, Volume> readSlice = (k) ->
        {
            String fn = String.format(this.input, k);
            try
            {
                if (!PathUtils.exists(fn))
                {
                    Logging.info("...slice not found: " + fn);
                    return null;
                }

                Logging.info("...processing slice: " + fn);
                return Volume.read(fn);
            }
            catch (IOException e)
            {
                Logging.info("... skipping slice: " + fn);
                return null;
            }
        };

        Function<Triple<Integer, Double, Double>, Double> update = triple ->
        {
            double num = triple.a;
            double stat = triple.b;
            double value = triple.c;

            switch (this.statistic)
            {
                case Max:
                    return Math.max(stat, value);
                case Min:
                    return Math.min(stat, value);
                case Mean:
                    return stat + (value / num);
                case Sum:
                    return stat + value;
                default:
                    return Math.max(stat, value);
            }
        };

        Function<List<Integer>, Volume> projectX = (list) ->
        {
            Volume out = VolumeSource.create(nj, nk, 1, ref.getDim());

            for (int k = 0; k < nk; k++)
            {
                Volume slice = readSlice.apply(list.get(k));
                if (slice != null)
                {
                    for (int j = jstart; j < jend; j++)
                    {
                        out.set(j, k, 0, slice.get(istart, j, 0));

                        for (int i = istart + 1; i < iend; i++)
                        {
                            for (int d = 0; d < ref.getDim(); d++)
                            {
                                double stat = out.get(j, k, 0, d);
                                double val = slice.get(i, j, 0, d);
                                out.set(j, k, 0, d, update.apply(Triple.of(list.size(), stat, val)));
                            }
                        }
                    }
                }
            }

            return out;
        };

        Function<List<Integer>, Volume> projectY = (list) ->
        {
            Volume out = VolumeSource.create(ni, nk, 1, ref.getDim());

            for (int k = 0; k < nk; k++)
            {
                Volume slice = readSlice.apply(list.get(k));
                if (slice != null)
                {
                    for (int i = istart; i < iend; i++)
                    {
                        out.set(i, k, 0, slice.get(i, jstart, 0));

                        for (int j = jstart + 1; j < jend; j++)
                        {
                            for (int d = 0; d < ref.getDim(); d++)
                            {
                                double stat = out.get(i, k, 0, d);
                                double val = slice.get(i, j, 0, d);

                                out.set(j, k, 0, d, update.apply(Triple.of(list.size(), stat, val)));
                            }
                        }
                    }
                }
            }

            return out;
        };

        Function<List<Integer>, Volume> projectZ = (list) ->
        {
            Volume out = ref.copy();

            for (int k : list)
            {
                Volume slice = readSlice.apply(k);
                if (slice != null)
                {
                    for (int i = istart; i < iend; i++)
                    {
                        for (int j = jstart; j < jend; j++)
                        {
                            for (int d = 0; d < ref.getDim(); d++)
                            {
                                double stat = out.get(i, j, 0, d);
                                double val = slice.get(i, j, 0, d);
                                out.set(j, k, 0, d, update.apply(Triple.of(list.size(), stat, val)));
                            }

                        }
                    }
                }
            }

            return out;
        };

        int mystep = this.slabStep;
        int mycount = this.slabCount;

        switch (this.mode)

        {
            case FullZ:
            {
                Logging.info("using whole stack Z projection");
                Volume proj = projectZ.apply(sidx);

                Logging.info("writing slice: " + this.output);
                proj.write(this.output);
            }
            break;
            case SlabFracZ:
                mystep = (int) Math.round(this.slabFraction * sidx.size());
                mycount = mystep;
                Logging.info("using fractional slab: " + mystep);
                // pass through to SlabZ
            case SlabZ:
            {
                Logging.info("using slab projection");
                for (int k = 0; k < nk; k += mystep)
                {
                    Volume proj = projectZ.apply(sidx.subList(k, Math.min(nk, k + mycount)));

                    String fn = String.format(this.output, k);
                    Logging.info("writing slice: " + fn);
                    proj.write(fn);
                }
            }
            break;
            case FullX:
            {
                Logging.info("using whole stack X projection");
                Volume proj = projectX.apply(sidx);

                Logging.info("writing slice: " + this.output);
                proj.write(this.output);
            }
            break;
            case FullY:
            {
                Logging.info("using whole stack Y projection");
                Volume proj = projectY.apply(sidx);

                Logging.info("writing slice: " + this.output);
                proj.write(this.output);
            }
            break;
        }

        Logging.info("finished projection");

        return this;
    }
}
