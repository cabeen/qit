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

package qit.math.utils.optim.mcmc;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.structs.Pair;
import qit.data.datasets.Vect;

import java.util.List;

public abstract class SamplerMCMC
{
    private int sample = 25;
    private int update = 24;
    private int burnin = 1000;
    private int jumps = 1250;

    private double cost;
    private Vect params;
    private Vect sigmas;

    private int[] batch_accept;
    private int[] batch_reject;

    private int[] total_accept;
    private int[] total_reject;

    private int iter = 0;

    private List<Pair<Double, Vect>> samples = null;

    public SamplerMCMC()
    {
    }

    public SamplerMCMC clear()
    {
        this.samples = null;
        return this;
    }

    public SamplerMCMC withParams(Vect v)
    {
        this.params = v.copy();
        this.cost = cost(this.params);
        this.sigmas = params.times(0.1);

        this.batch_accept = new int[this.params.size()];
        this.batch_reject = new int[this.params.size()];
        this.total_accept = new int[this.params.size()];
        this.total_reject = new int[this.params.size()];

        return this.clear();
    }

    public SamplerMCMC withSigmas(Vect v)
    {
        this.sigmas = v;
        return this.clear();
    }

    public SamplerMCMC withSample(int v)
    {
        this.sample = v;
        return clear();
    }

    public SamplerMCMC withUpdate(int v)
    {
        this.update = v;
        return clear();
    }

    public SamplerMCMC withBurnin(int v)
    {
        this.burnin = v;
        return clear();
    }

    public SamplerMCMC withJumps(int v)
    {
        this.jumps = v;
        return clear();
    }

    public SamplerMCMC run()
    {
        Global.assume(this.params != null, "initial params must be specified");

        this.samples = Lists.newArrayList();

        for (int i = 0; i < this.burnin; i++)
        {
            this.jump();

            if (i % this.update == 0)
            {
                this.update();
            }
        }

        for (int i = 0; i < this.jumps; i++)
        {
            this.jump();

            if (i % this.sample == 0)
            {
                this.sample();
            }

            if (i % this.update == 0)
            {
                this.update();
            }
        }

        return this;
    }

    public List<Pair<Double,Vect>> getOutputSamples()
    {
        if (this.samples == null)
        {
            this.run();
        }

        return this.samples;
    }

    public Pair<Double,Vect> getOutputBest()
    {
        if (this.samples == null)
        {
            this.run();
        }

        Pair<Double,Vect> best = this.samples.get(0);
        for (Pair<Double,Vect> sample : this.samples)
        {
            if (sample.a < best.a)
            {
                best = sample;
            }
        }

        return best;
    }

    private void jump()
    {
        for (int which = 0; which < this.params.size(); which++)
        {
            Vect proposalParams = propose(this.params, this.sigmas, which);
            double proposalCost = cost(proposalParams);

            if (Math.exp(this.cost - proposalCost) > Global.RANDOM.nextDouble())
            {
                this.params = proposalParams;
                this.cost = proposalCost;

                this.batch_accept[which] += 1;
                this.total_accept[which] += 1;
            }
            else
            {
                this.batch_reject[which] += 1;
                this.total_reject[which] += 1;
            }
        }

        this.iter += 1;
    }

    private void update()
    {
        for (int i = 0; i < this.sigmas.size(); i++)
        {
            double acc = this.batch_accept[i];
            double rej = this.batch_reject[i];

            double fac = Math.sqrt((acc + 1) / (rej + 1));
            this.sigmas.timesEquals(i, fac);

            this.batch_accept[i] = 0;
            this.batch_reject[i] = 0;
        }
    }

    private void sample()
    {
        this.samples.add(Pair.of(this.cost, this.params.copy()));
    }

    abstract public Vect propose(Vect params, Vect sigmas, int which);

    abstract public double cost(Vect param);
}
