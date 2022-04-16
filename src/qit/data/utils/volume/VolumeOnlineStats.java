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
import qit.data.datasets.Volume;

public class VolumeOnlineStats
{
    public Mask mask = null;

    // model statistics that can be updated in an online fashion
    // this doesn't include median and quartiles unfortunately...
    public boolean init = false;
    public Volume min = null;
    public Volume max = null;
    public Volume mean = null;
    public Volume var = null;
    public Volume std = null;
    public Volume sum = null;
    public Volume num = null;
    public Volume cv = null;
    private Volume m2 = null; // An intermediate for computing the variance

    public VolumeOnlineStats()
    {

    }

    public VolumeOnlineStats withMask(Mask vol)
    {
        this.mask = vol;

        return this;
    }

    public void update(Volume vol)
    {
        this.update(vol, false);
    }

    public void update(Volume vol, boolean skipnan)
    {
        if (!this.init)
        {
            this.min = vol.copy();
            this.max = vol.copy();
            this.sum = vol.copy();

            this.num = vol.proto();
            this.mean = vol.proto();
            this.std = vol.proto();
            this.cv = vol.proto();
            this.m2 = vol.proto();
            this.var = vol.proto();

            this.init = true;
        }

        for (Sample sample : vol.getSampling())
        {
            if (vol.valid(sample, this.mask))
            {
                for (int i = 0; i < vol.getDim(); i++)
                {
                    double v = vol.get(sample, i);

                    if (Double.isNaN(v))
                    {
                        if (skipnan)
                        {
                            Logging.info("nan encountered");
                        }
                        else
                        {
                            throw new RuntimeException("nan encountered");
                        }
                    }

                    double numv = this.num.get(sample, i);
                    double meanv = this.mean.get(sample, i);
                    double m2v = this.m2.get(sample, i);
                    double minv = this.min.get(sample, i);
                    double maxv = this.max.get(sample, i);
                    double sumv = this.sum.get(sample, i);

                    numv += 1;
                    sumv += v;
                    minv = Math.min(minv, v);
                    maxv = Math.min(maxv, v);

                    // Use an online algorithm for mean and variance
                    double nmeanv = meanv + (v - meanv) / numv;
                    m2v += (v - nmeanv) * (v - meanv);
                    meanv = nmeanv;
                    double varv = m2v / numv;
                    double stdv = Math.sqrt(varv);
                    double cvv = stdv / meanv;

                    this.num.set(sample, i, numv);
                    this.min.set(sample, i, minv);
                    this.max.set(sample, i, maxv);
                    this.mean.set(sample, i, meanv);
                    this.std.set(sample, i, stdv);
                    this.cv.set(sample, i, cvv);
                    this.var.set(sample, i, varv);
                    this.sum.set(sample, i, sumv);
                }
            }
        }

    }
}
