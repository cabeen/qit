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
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.vects.stats.VectsOnlineAxialStats;
import qit.math.structs.VectFunction;
import qit.math.source.VectFunctionSource;

public class VolumeOnlineAxialStats
{
    // assume we have 3D axes
    private static final int DIM = 3;

    private Sampling sampling;
    private VectsOnlineAxialStats[] stats;
;
    public Mask mask;

    private Volume mean;
    private Volume coherence;
    private Volume lambda;

    private VolumeOnlineAxialStats clear()
    {
        this.mean = null;
        this.coherence = null;
        this.lambda = null;

        return this;
    }

    private  VolumeOnlineAxialStats init(Sampling proto)
    {
        if (this.sampling == null)
        {
            this.sampling = proto;
            this.stats = new VectsOnlineAxialStats[this.sampling.size()];

            for (Sample sample : this.sampling)
            {
                if (this.sampling.contains(sample) && (this.mask == null || this.mask.foreground(sample)))
                {
                    this.stats[this.sampling.index(sample)] = new VectsOnlineAxialStats();
                }
            }
        }

        return this;
    }

    public VolumeOnlineAxialStats withMask(Mask m)
    {
        this.mask = m;
        return this.clear();
    }

    public VolumeOnlineAxialStats update(Volume thetas, Volume phis)
    {
        Global.assume(thetas.getDim() == phis.getDim(), "invalid theta/phi combination");
        Global.assume(thetas.getSampling().equals(phis.getSampling()), "invalid theta/phi combination");

        init(thetas.getSampling());

        VectFunction xfm = VectFunctionSource.sphereToCart();

        for (Sample sample : this.sampling)
        {
            if (thetas.valid(sample, this.mask))
            {
                VectsOnlineAxialStats stats = this.stats[this.sampling.index(sample)];

                for(int i = 0; i < thetas.getDim(); i++)
                {
                    double theta = thetas.get(sample, i);
                    double phi = phis.get(sample, i);

                    Vect sphere = VectSource.create3D(1, theta, phi);
                    Vect axis = xfm.apply(sphere);

                    stats.update(axis);
                }
            }
        }

        return this;
    }

    public VolumeOnlineAxialStats update(Volume vals)
    {
        Global.assume(vals.getDim() == 3, "invalid axial volume");

        init(vals.getSampling());

        for (Sample sample : this.sampling)
        {
            if (vals.valid(sample, this.mask))
            {
                this.stats[this.sampling.index(sample)].update(vals.get(sample));
            }
        }

        return this;
    }

    public VolumeOnlineAxialStats compile()
    {
        if (this.mean == null)
        {
            Global.assume(this.sampling != null, "no updates were made!");

            Volume meanOut = VolumeSource.create(this.sampling, DIM);
            Volume coherenceOut = VolumeSource.create(this.sampling, DIM);
            Volume lambdaOut = VolumeSource.create(this.sampling, DIM);

            for (Sample sample : this.sampling)
            {
                if (meanOut.valid(sample, this.mask))
                {
                    VectsOnlineAxialStats stat = this.stats[this.sampling.index(sample)];

                    meanOut.set(sample, stat.mean);
                    coherenceOut.set(sample, 0, stat.coherence);
                    lambdaOut.set(sample, 0, stat.lambda);
                }
            }

            this.mean = meanOut;
            this.coherence = coherenceOut;
            this.lambda = lambdaOut;
        }

        return this;
    }

    public Volume getOutputMean()
    {
        return this.compile().mean;
    }

    public Volume getOutputCoherence()
    {
        return this.compile().coherence;
    }

    public Volume getOutputLambda()
    {
        return this.compile().lambda;
    }
}