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

import qit.base.Logging;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.math.structs.VectFunction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VolumeSample
{
    public VectFunction function;
    public Sampling sampling;
    public Mask mask;
    public Integer threads = 1;
    public boolean verbose = true;
    public Volume output;

    public VolumeSample withFunction(VectFunction f)
    {
        this.function = f;
        this.output = null;

        return this;
    }

    public VolumeSample withSampling(Sampling s)
    {
        this.sampling = s;
        this.output = null;

        return this;
    }

    public VolumeSample withMask(Mask m)
    {
        this.mask = m;
        this.output = null;

        return this;
    }

    public VolumeSample withThreads(int v)
    {
        this.threads = v;
        this.output = null;

        return this;
    }

    public VolumeSample withVerbose(boolean v)
    {
        this.verbose = v;
        this.output = null;

        return this;
    }

    public VolumeSample run()
    {
        final Volume out = VolumeSource.create(this.sampling, this.function.getDimOut());

        if (this.threads == null || this.threads < 2)
        {
            if (this.verbose)
            {
                Logging.info("starting function sampling");
            }

            Integer prev = null;
            for (Sample s : this.sampling)
            {
                int percent = (100 * this.sampling.index(s)) / this.sampling.size();
                if (prev == null || percent > prev)
                {
                    Logging.info(String.format("... %d percent complete", percent));
                    prev = percent;
                }

                if (out.valid(s, this.mask))
                {
                    out.set(s, this.function.apply(this.sampling.world(s)));
                }
            }

            if (this.verbose)
            {
                Logging.info("finished function sampling");
            }
        }
        else
        {
            if (this.verbose)
            {
                Logging.info("starting parallel function sampling");
                Logging.info("thread count: " + this.threads);
            }

            ExecutorService exec = Executors.newFixedThreadPool(this.threads);

            final int nk = this.sampling.numK();
            for (int tk = 0; tk < nk; tk++)
            {
                final int k = tk;

                Runnable runnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        if (VolumeSample.this.verbose)
                        {
                            Logging.info(String.format("... started processing slice (%s/%s)", k + 1, nk));
                        }

                        for (int j = 0; j < sampling.numJ(); j++)
                        {
                            for (int i = 0; i < sampling.numI(); i++)
                            {
                                Sample sample = new Sample(i, j, k);

                                if (out.valid(sample, VolumeSample.this.mask))
                                {
                                    Vect world = VolumeSample.this.sampling.world(sample);
                                    Vect value = VolumeSample.this.function.apply(world);
                                    out.set(sample, value);
                                }
                            }
                        }

                        if (VolumeSample.this.verbose)
                        {
                            Logging.info(String.format("... finished processing slice (%s/%s)", k + 1, nk));
                        }
                    }
                };

                exec.execute(runnable);
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

            if (this.verbose)
            {
                Logging.info("finished parallel function sampling");
            }
        }

        this.output = out;

        return this;
    }

    public Volume getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
