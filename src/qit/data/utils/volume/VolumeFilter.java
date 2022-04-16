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

package qit.data.utils.volume;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.math.utils.MathUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VolumeFilter
{
    public Volume input = null;
    public Volume filter = null;
    public Integer channel = null;
    public Mask mask = null;
    public boolean pass = false;
    public int threads = 1;
    public boolean boundary = false;
    public boolean normalize = false;
    public Volume output = null;

    public VolumeFilter()
    {
    }

    public VolumeFilter withFilter(Volume filter)
    {
        this.filter = filter;
        this.output = null;

        return this;
    }

    public VolumeFilter withInput(Volume input)
    {
        this.input = input;
        this.output = null;

        return this;
    }

    public VolumeFilter withMask(Mask mask)
    {
        this.mask = mask;
        this.output = null;

        return this;
    }

    public VolumeFilter withPass(boolean v)
    {
        this.pass = v;
        this.output = null;

        return this;
    }

    public VolumeFilter withChannel(int c)
    {
        this.channel = c;
        this.output = null;

        return this;
    }

    public VolumeFilter withThreads(int c)
    {
        this.threads = c;
        this.output = null;

        return this;
    }

    public VolumeFilter withBoundary(boolean v)
    {
        this.boundary = v;
        this.output = null;

        return this;
    }

    public VolumeFilter withNormalize(boolean v)
    {
        this.normalize = v;
        this.output = null;

        return this;
    }

    public VolumeFilter run()
    {
        Global.assume(this.mask == null || this.mask.getSampling().num().equals(this.input.getSampling().num()), "invalid mask, must match dimensions of the image");

        if (this.threads < 2)
        {
            int dim = this.input.getDim();
            Sampling sampling = this.input.getSampling();
            Sampling fsampling = this.filter.getSampling();
            Volume out = this.input.copy();

            int cx = (fsampling.numI() - 1) / 2;
            int cy = (fsampling.numJ() - 1) / 2;
            int cz = (fsampling.numK() - 1) / 2;

            for (int d = 0; d < dim; d++)
            {
                for (Sample sample : sampling)
                {
                    if (!out.valid(sample, this.mask))
                    {
                        if (this.pass)
                        {
                            out.set(sample, d, this.input.get(sample, d));
                        }

                        continue;
                    }

                    if (this.channel != null && d != this.channel)
                    {
                        out.set(sample, d, this.input.get(sample, d));
                        continue;
                    }

                    boolean all = true;

                    double pv = 0;
                    double sf = 0;
                    for (Sample fsample : fsampling)
                    {
                        int ni = sample.getI() + fsample.getI() - cx;
                        int nj = sample.getJ() + fsample.getJ() - cy;
                        int nk = sample.getK() + fsample.getK() - cz;
                        Sample nsample = new Sample(ni, nj, nk);

                        if (sampling.contains(nsample))
                        {
                            double v = this.input.get(nsample, d);
                            double f = this.filter.get(fsample, 0);
                            sf += f;
                            pv += v * f;
                        }
                        else
                        {
                            all = false;
                        }
                    }

                    if (this.normalize && !MathUtils.zero(sf))
                    {
                        pv /= sf;
                    }

                    if (!all && !this.boundary)
                    {
                        pv = 0;
                    }

                    out.set(sample, d, pv);
                }
            }

            this.output = out;
        }
        else
        {
            ExecutorService exec = Executors.newFixedThreadPool(this.threads);

            int dim = this.input.getDim();
            Sampling sampling = this.input.getSampling();
            Sampling fsampling = this.filter.getSampling();
            Volume out = this.input.copy();

            int cx = (fsampling.numI() - 1) / 2;
            int cy = (fsampling.numJ() - 1) / 2;
            int cz = (fsampling.numK() - 1) / 2;

            final Volume finput = input;
            final Mask fmask = this.mask;
            final Integer fchannel = this.channel;
            final boolean fpass = this.pass;
            final boolean fnormalize = this.normalize;
            final Volume ffilter = this.filter;

            for (int d = 0; d < dim; d++)
            {
                final int fd = d;
                for (int k = 0; k < sampling.numK(); k++)
                {
                    final int fk = k;

                    for (int j = 0; j < sampling.numJ(); j++)
                    {
                        final int fj = j;

                        Runnable runnable = () ->
                        {
                            for (int i = 0; i < sampling.numI(); i++)
                            {
                                Sample sample = new Sample(i, fj, fk);

                                if (!out.valid(sample, fmask))
                                {
                                    if (fpass)
                                    {
                                        out.set(sample, fd, finput.get(sample, fd));
                                    }

                                    continue;
                                }

                                if (fchannel != null && fd != fchannel)
                                {
                                    out.set(sample, fd, finput.get(sample, fd));
                                    continue;
                                }

                                double pv = 0;
                                double sf = 0;
                                for (Sample fsample : fsampling)
                                {
                                    int ni = sample.getI() + fsample.getI() - cx;
                                    int nj = sample.getJ() + fsample.getJ() - cy;
                                    int nk = sample.getK() + fsample.getK() - cz;
                                    Sample nsample = new Sample(ni, nj, nk);

                                    if (sampling.contains(nsample))
                                    {
                                        double v = finput.get(nsample, fd);
                                        double f = ffilter.get(fsample, 0);
                                        sf += f;
                                        pv += v * f;
                                    }
                                }

                                if (fnormalize && !MathUtils.zero(sf))
                                {
                                    pv /= sf;
                                }

                                out.set(sample, fd, pv);
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

            this.output = out;
        }

        return this;
    }

    public Volume getOutput()
    {
        return this.output;
    }
}
