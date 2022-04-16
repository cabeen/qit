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

package qit.data.utils.mri.fitting;

import com.google.common.collect.Lists;
import org.python.core.util.ConcurrentHashSet;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineAxialStats;
import qit.data.models.Fibers;
import qit.data.utils.mri.structs.Gradients;
import qit.data.models.Tensor;
import qit.math.structs.VectFunction;
import qit.math.utils.optim.mcmc.SamplerMCMC;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FitFibersMCMC extends VectFunction
{
    // this is similar to FSL xfibres

    private static final double MAX_DOUBLE = 1e10;
    private static final double MAX_PROPOSE = 1000;

    private Gradients gradients;
    private VectFunction output;

    private int comps = 2;
    private int sample = 1;
    private int update = 40;
    private int burnin = 250;
    private int jumps = 5000;
    private int restarts = 5;
    private int threads = 2;

    public FitFibersMCMC withGradients(Gradients g)
    {
        this.gradients = g;
        this.output = null;

        return this;
    }

    public FitFibersMCMC withComps(int n)
    {
        this.comps = n;
        this.output = null;
        return this;
    }

    public FitFibersMCMC withSample(int n)
    {
        this.sample = n;
        this.output = null;
        return this;
    }

    public FitFibersMCMC withUpdate(int n)
    {
        this.update = n;
        this.output = null;
        return this;
    }

    public FitFibersMCMC withBurnin(int n)
    {
        this.burnin = n;
        this.output = null;
        return this;
    }

    public FitFibersMCMC withJumps(int n)
    {
        this.jumps = n;
        this.output = null;
        return this;
    }

    public FitFibersMCMC withRestarts(int n)
    {
        this.restarts = n;
        this.output = null;
        return this;
    }

    public FitFibersMCMC withThreads(int n)
    {
        this.threads = n;
        this.output = null;
        return this;
    }

    public int getComps()
    {
        return this.comps;
    }

    public FitFibersMCMC run()
    {
        this.output = new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(fit(input).getEncoding());
            }
        }.init(this.gradients.size(), new Fibers(this.comps).getEncodingSize());

        return this;
    }

    public Pair<Double,Fibers> fitBatch(Vect input)
    {
            int nc = FitFibersMCMC.this.comps;
            Vect params = VectSource.createND(2 + 3 * nc);
            {
                FitTensorLLS lls = new FitTensorLLS();
                lls.gradients = this.gradients;
                Tensor tensor = new Tensor(lls.get().apply(input));
                double s0 = tensor.getBaseline();
                double md = tensor.feature(Tensor.FEATURES_MD).get(0);
                double fa = tensor.feature(Tensor.FEATURES_FA).get(0);

                double v1 = tensor.getVal(0);
                double v2 = tensor.getVal(1);
                double v3 = tensor.getVal(2);

                Vect d1 = tensor.getVec(0);
                Vect d2 = tensor.getVec(1);
                Vect d3 = tensor.getVec(2);

                params.set(0, s0);
                params.set(1, md);

                for (int i = 0; i < nc; i++)
                {
                    Vect line = VectSource.randomUnit();

                    params.set(2 + 3 * i + 0, 0.1);
                    params.set(2 + 3 * i + 1, theta(line));
                    params.set(2 + 3 * i + 2, phi(line));
                }
            }

            Vect sigmas = params.proto();
            sigmas.set(0, 0.1 * params.get(0));
            sigmas.set(1, 0.1 * params.get(1));

            for (int i = 0; i < nc; i++)
            {
                sigmas.set(2 + i * 3 + 0, 0.2);
                sigmas.set(2 + i * 3 + 1, 0.2);
                sigmas.set(2 + i * 3 + 2, 0.2);
            }

            FibersSamplerMCMC sampler = new FibersSamplerMCMC(input, FitFibersMCMC.this.gradients);
            sampler.withParams(params);
            sampler.withSigmas(sigmas);
            sampler.withSample(FitFibersMCMC.this.sample);
            sampler.withUpdate(FitFibersMCMC.this.update);
            sampler.withBurnin(FitFibersMCMC.this.burnin);
            sampler.withJumps(FitFibersMCMC.this.jumps);
            List<Pair<Double, Vect>> samples = sampler.getOutputSamples();

            if (samples.size() == 0)
            {
                Logging.info("... warning: no samples taken");

                Fibers fibers = fibers(params);
                double cost = FitFibersMCMC.cost(input, gradients, fibers);
                return Pair.of(cost, fibers);
            }
            else
            {
                Fibers fibers = fibers(samples);
                double cost = FitFibersMCMC.cost(input, gradients, fibers);
                return Pair.of(cost, fibers);

//                for (Pair<Double,Vect> sample : samples)
//                {
//                    if (sample.a < mean.a)
//                    {
//                        mean = Pair.of(sample.a, fibers(sample.b));
//                    }
//                }
            }
    }

    public Fibers fit(Vect input)
    {
        if (this.threads == 1)
        {
            Logging.info("fitting model using single thread");
            Pair<Double,Fibers> best = null;
            for (int k = 0; k < this.restarts; k++)
            {
                Pair<Double, Fibers> mean = fitBatch(input);
                if (best == null || best.a < mean.a)
                {
                    best = mean;
                }
            }

            return best.b;
        }
        else
        {
            int pool = this.threads;
            if (pool < 1)
            {
                pool = Runtime.getRuntime().availableProcessors();
                Logging.info("detected processors: " + pool);
            }

            Logging.info("fitting model using multi-threading: " + pool);
            ExecutorService exec = Executors.newFixedThreadPool(pool);

            final Vect finput = input;
            final ConcurrentHashSet<Pair<Double,Fibers>> results = new ConcurrentHashSet<>();
            for (int k = 0; k < this.restarts; k++)
            {
                Runnable runnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        results.add(fitBatch(finput));
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

            Pair<Double,Fibers> best = null;
            for (Pair<Double,Fibers> result : results)
            {
                if (best == null || result.a < best.a)
                {
                    best = result;
                }
            }

            return best.b;
        }
    }

    private static double theta(Vect xyz)
    {
        return Math.acos(xyz.get(2));
    }

    private static double phi(Vect xyz)
    {
        return Math.atan2(xyz.get(1), xyz.get(0));
    }

    private static Vect cart(double th, double ph)
    {
        double x = Math.sin(th) * Math.cos(ph);
        double y = Math.sin(th) * Math.sin(ph);
        double z = Math.cos(th);
        return VectSource.create3D(x, y, z);
    }

    private static Fibers fibers(List<Pair<Double,Vect>> samples)
    {
        Fibers fibers = fibers(samples.get(0).b).proto();

        VectOnlineStats baseStats = new VectOnlineStats();
        VectOnlineStats diffStats = new VectOnlineStats();
        List<VectOnlineStats> fracStats = Lists.newArrayList();
        List<VectsOnlineAxialStats> lineStats = Lists.newArrayList();
        for (int i = 0; i < fibers.size(); i++)
        {
            fracStats.add(new VectOnlineStats());
            lineStats.add(new VectsOnlineAxialStats());
        }

        for (Pair<Double,Vect> sample : samples)
        {
            Fibers model = fibers(sample.b);

            baseStats.update(model.getBaseline());
            diffStats.update(model.getDiffusivity());

            for (int i = 0; i < fibers.size(); i++)
            {
                fracStats.get(i).update(model.getFrac(i));
                lineStats.get(i).update(model.getLine(i));
            }
        }

        fibers.setBaseline(baseStats.mean);
        fibers.setDiffusivity(diffStats.mean);

        for (int i = 0; i < fibers.size(); i++)
        {
            fibers.setFrac(i, fracStats.get(i).mean);
            fibers.setLine(i, lineStats.get(i).mean);
        }

        return fibers;
    }

    private static Fibers fibers(Vect param)
    {
        int nc = (param.size() - 2) / 3;
        Fibers fibers = new Fibers(nc);

        fibers.setBaseline(param.get(0));
        fibers.setDiffusivity(param.get(1));

        for (int i = 0; i < nc; i++)
        {
            double fr = param.get(2 + 3 * i + 0);
            double th = param.get(2 + 3 * i + 1);
            double ph = param.get(2 + 3 * i + 2);

            fibers.setFrac(i, fr);
            fibers.setLine(i, cart(th, ph));
        }

        return fibers;
    }

    public static double cost(Vect signal, Gradients gradients, Fibers fibers)
    {
        double s0 = fibers.getBaseline();
        double d = fibers.getDiffusivity();
        double sumsquares = 0;

        for (int i = 0; i < gradients.size(); i++)
        {
            double b = gradients.getBval(i);
            Vect g = gradients.getBvec(i).normalize();
            double ndb = -d * b;

            double si = 0;
            double sumf = 0;

            for (int j = 0; j < fibers.size(); j++)
            {
                double f = fibers.getFrac(j);
                Vect u = fibers.getLine(j);
                double gdotu = g.dot(u);
                double arg = ndb * gdotu * gdotu;
                si += f * s0 * Math.exp(arg);
                sumf += f;
            }

            si += (1 - sumf) * s0 * Math.exp(ndb);

            double ds = si - signal.get(i);
            sumsquares += ds * ds;
        }

//        double cost = (gradients.size() / 2.0) * Math.log(sumsquares / 2.0) * sumsquares / 2.0;
        double cost = sumsquares / 2.0;

        return cost;
    }

    public VectFunction getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }

    private static class FibersSamplerMCMC extends SamplerMCMC
    {
        private Vect signal;
        private Gradients gradients;

        private FibersSamplerMCMC(Vect signal, Gradients gradients)
        {
            super();
            this.signal = signal;
            this.gradients = gradients;
        }

        public double cost(Vect params)
        {
            return FitFibersMCMC.cost(this.signal, this.gradients, fibers(params));
        }

        public Vect propose(Vect params, Vect sigmas, int which)
        {
            // use early rejection to avoid invalid parameters
            propose:
            for (int i = 0; i < MAX_PROPOSE; i++)
            {
                Vect proposal = params.plus(which, sigmas.get(which) * Global.RANDOM.nextGaussian());

                if (proposal.get(0) <= 0)
                {
                    continue;
                }

                if (proposal.get(1) <= 0)
                {
                    continue;
                }

                double fsum = 0;
                for (int j = 2; j < proposal.size(); j++)
                {
                    if ((j - 2) % 3 == 0)
                    {
                        double frac = proposal.get(j);
                        if (frac < 0)
                        {
                            continue propose;
                        }

                        fsum += frac;
                    }
                }
                if (fsum > 1)
                {
                    continue propose;
                }

                return proposal;
            }

            Logging.error("bug in mcmc code");

            return null;
        }
    }
}