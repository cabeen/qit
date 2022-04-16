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

import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.math.structs.VectFunction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class VolumeFunction
{
    public Supplier<VectFunction> factory;
    public Volume input;
    public Mask mask;
    public Integer threads = 1;
    public boolean slice = true;
    public boolean messages = true;
    public boolean chatty = false;

    public VolumeFunction(Supplier<VectFunction> v)
    {
        this.factory = v;
    }

    public VolumeFunction(VectFunction v)
    {
        // careful with functions that have settings that cannot be shared by multiple threads!
        this.factory = () -> v;
    }

    public VolumeFunction withInput(Volume input)
    {
        this.input = input;
        return this;
    }

    public VolumeFunction withMask(Mask mask)
    {
        this.mask = mask;
        return this;
    }

    public VolumeFunction withMessages(boolean v)
    {
        this.messages = v;
        return this;
    }

    public VolumeFunction withSlice(boolean v)
    {
        this.slice = v;
        return this;
    }

    public VolumeFunction withThreads(int v)
    {
        this.threads = v;
        return this;
    }

    public Volume run()
    {
        Global.assume(this.input != null, "input is required");
        Global.assume(this.factory != null, "a function factory is required");

        final Sampling sampling = this.input.getSampling();
        VectFunction function = this.factory.get();
        Global.assume(this.input.getDim() == function.getDimIn(), "channel mismatch");
        final Volume out = this.input.proto(function.getDimOut());

        if (this.messages)
        {
            Logging.info("started applying function to volume");
        }

        if (this.threads <= 1)
        {
            for (int k = 0; k < sampling.numK(); k++)
            {
                if (this.messages)
                {
                    Logging.info(String.format("... processing slice %d/%d", k + 1, sampling.numK()));
                }

                for (Sample sample : sampling.iterateK(k))
                {
                    if (this.input.valid(sample, this.mask))
                    {
                        if (this.chatty)
                        {
                            Logging.info(String.format("...... processing voxel (%d, %d, %d)", sample.getI(), sample.getJ(), sample.getK()));
                        }

                        out.set(sample, function.apply(this.input.get(sample)));
                    }
                }
            }
        }
        else if (this.slice)
        {
            if (this.messages)
            {
                Logging.info("using threads: " + this.threads);
            }

            ExecutorService exec = Executors.newFixedThreadPool(this.threads);

            for (int k = 0; k < sampling.numK(); k++)
            {
                final int fk = k;
                exec.execute(() ->
                {
                    // functions can have data with side effects,
                    // so we require a new function to be created for each thread
                    VectFunction functionThread = VolumeFunction.this.factory.get();

                    if (VolumeFunction.this.messages)
                    {
                        Logging.info(String.format("... processing slice %d/%d", fk + 1, sampling.numK()));
                    }

                    for (Sample sample : sampling.iterateK(fk))
                    {
                        if (VolumeFunction.this.input.valid(sample, VolumeFunction.this.mask))
                        {
                            if (VolumeFunction.this.chatty)
                            {
                                Logging.info(String.format("...... processing voxel (%d, %d, %d)", sample.getI(), sample.getJ(), sample.getK()));
                            }

                            out.set(sample, functionThread.apply(VolumeFunction.this.input.get(sample)));
                        }
                    }
                });
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
        else
        {
            if (this.messages)
            {
                Logging.info("using threads: " + this.threads);
            }

            ExecutorService exec = Executors.newFixedThreadPool(this.threads);

            for (int k = 0; k < sampling.numK(); k++)
            {
                final int fk = k;

                for (int j = 0; j < sampling.numJ(); j++)
                {
                    final int fj = j;

                    exec.execute(() -> {
                        // functions can have data with side effects,
                        // so we require a new function to be created for each thread
                        VectFunction functionThread = VolumeFunction.this.factory.get();

                        if (VolumeFunction.this.messages)
                        {
                            Logging.info(String.format("... processing slice %d/%d column %d/%d", fk + 1, sampling.numK(), fj + 1, sampling.numJ()));
                        }

                        for (int i = 0; i < sampling.numI(); i++)
                        {
                            if (VolumeFunction.this.input.valid(i, fj, fk, VolumeFunction.this.mask))
                            {
                                out.set(i, fj, fk, functionThread.apply(VolumeFunction.this.input.get(i, fj, fk)));
                            }
                        }
                    });
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

        if (this.messages)
        {
            Logging.info("finished applying function to volume");
        }

        return out;
    }
}
