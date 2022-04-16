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
import qit.base.Logging;
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
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ModuleDescription("Filter a volume using a median filter")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFilterMedian implements Module
{
    private static final String I_SLICE = "i";
    private static final String J_SLICE = "j";
    private static final String K_SLICE = "k";

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleParameter
    @ModuleDescription("the window size in voxels")
    public int window = 1;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the volume channel (default applies to all)")
    public Integer channel;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("restrict the filtering to a specific slice (i, j, or k)")
    public String slice = null;

    @ModuleParameter
    @ModuleDescription("the number of times to filter")
    public Integer num = 1;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output = null;

    public VolumeFilterMedian run()
    {
        if (this.num == 0)
        {
            this.output = this.input.copy();
        }
        else
        {
            Volume volume = this.input;

            for (int i = 0; i < this.num; i++)
            {
                volume = single(volume);
            }

            this.output = volume;
        }

        return this;
    }

    public Volume single(Volume volume)
    {
        final Volume out = volume.proto();
        final int dim = volume.getDim();
        final Sampling sampling = volume.getSampling();
        final int n = 2 * this.window + 1;
        final Sampling fsampling = fsampling(n, this.slice);

        final int cx = (fsampling.numI() - 1) / 2;
        final int cy = (fsampling.numJ() - 1) / 2;
        final int cz = (fsampling.numK() - 1) / 2;

        if (this.threads == null || this.threads < 2)
        {
            for (int d = 0; d < dim; d++)
            {
                for (Sample sample : sampling)
                {
                    if (!out.valid(sample, this.mask))
                    {
                        continue;
                    }

                    if (this.channel != null && d != this.channel)
                    {
                        out.set(sample, d, volume.get(sample, d));
                        continue;
                    }

                    List<Double> vals = Lists.newArrayList();
                    for (Sample fsample : fsampling)
                    {
                        int ni = sample.getI() + fsample.getI() - cx;
                        int nj = sample.getJ() + fsample.getJ() - cy;
                        int nk = sample.getK() + fsample.getK() - cz;
                        Sample nsample = new Sample(ni, nj, nk);

                        if (sampling.contains(nsample))
                        {
                            double val = volume.get(nsample, d);
                            vals.add(val);
                        }
                    }

                    Collections.sort(vals);

                    double pv = vals.get(vals.size() / 2);

                    out.set(sample, d, pv);
                }

                this.output = out;
            }
        }
        else
        {
            ExecutorService exec = Executors.newFixedThreadPool(this.threads);

            final Volume finput = volume;
            final Mask fmask = this.mask;
            final Integer fchannel = this.channel;

            for (int d = 0; d < dim; d++)
            {
                final int fd = d;
                for (int k = 0; k < sampling.numK(); k++)
                {
                    final int fk = k;

                    for (int j = 0; j < sampling.numJ(); j++)
                    {
                        final int fj = j;

                        Runnable runnable = new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                for (int i = 0; i < sampling.numI(); i++)
                                {
                                    Sample sample = new Sample(i, fj, fk);

                                    if (!out.valid(sample, fmask))
                                    {
                                        continue;
                                    }

                                    if (fchannel != null && fd != fchannel)
                                    {
                                        out.set(sample, fd, finput.get(sample, fd));
                                        continue;
                                    }

                                    List<Double> vals = Lists.newArrayList();
                                    for (Sample fsample : fsampling)
                                    {
                                        int ni = sample.getI() + fsample.getI() - cx;
                                        int nj = sample.getJ() + fsample.getJ() - cy;
                                        int nk = sample.getK() + fsample.getK() - cz;
                                        Sample nsample = new Sample(ni, nj, nk);

                                        if (sampling.contains(nsample))
                                        {
                                            double val = finput.get(nsample, fd);
                                            vals.add(val);
                                        }
                                    }

                                    Collections.sort(vals);

                                    double pv = vals.get(vals.size() / 2);

                                    out.set(sample, fd, pv);
                                }
                            }
                        };

                        exec.execute(runnable);
                    }
                }
            }

            exec.shutdown();
            try
            {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
            catch (InterruptedException e)
            {
                Logging.error("failed to execute in concurrent mode");
            }
        }

        return out;
    }

    private static Sampling fsampling(int n, String slice)
    {
        if (slice != null && I_SLICE.equals(slice))
        {
            return SamplingSource.create(1, n, n);
        }

        if (slice != null && J_SLICE.equals(slice))
        {
            return SamplingSource.create(n, 1, n);
        }

        if (slice != null && K_SLICE.equals(slice))
        {
            return SamplingSource.create(n, n, 1);
        }

        return SamplingSource.create(n, n, n);
    }

    public static Volume apply(Volume volume)
    {
        VolumeFilterMedian median = new VolumeFilterMedian();
        median.input = volume;
        return median.run().output;
    }

    public static Volume apply(Volume volume, Mask mask)
    {
        VolumeFilterMedian median = new VolumeFilterMedian();
        median.input = volume;
        median.mask = mask;
        return median.run().output;
    }

    public static Volume apply(Volume volume, int num)
    {
        Volume out = volume;
        for (int i = 0; i < num; i++)
        {
            out = apply(out);
        }
        return out;
    }

    public static Volume apply(Volume volume, Mask mask, int num)
    {
        Volume out = volume;
        for (int i = 0; i < num; i++)
        {
            out = apply(out, mask);
        }
        return out;
    }
}