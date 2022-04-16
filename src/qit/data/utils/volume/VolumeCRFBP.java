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

import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.vects.stats.VectOnlineStats;

public class VolumeCRFBP
{
    private static int NUM_DIR = 6;
    private static int[] DIRX = { 1, -1, 0, 0, 0, 0 };
    private static int[] DIRY = { 0, 0, 1, -1, 0, 0 };
    private static int[] DIRZ = { 0, 0, 0, 0, 1, -1 };
    private static int[] REVERSE = { 1, 0, 3, 2, 5, 4 };

    private int values;
    private double dataWeight;
    private double smoothWeight;
    private double contrastWeight;
    private double contrastGain;
    private Sampling sampling;
    private Volume data;
    private Volume image;
    private Volume[] dirs;
    private Volume[] pdirs;
    private Mask mask;

    public VolumeCRFBP(Double dataWeight, Double smoothWeight, Double contrastWeight, Volume image, Volume dataCost, Mask mask)
    {
        this.values = dataCost.getDim();

        this.dataWeight = dataWeight == null ? 1.0 : dataWeight;
        this.smoothWeight = smoothWeight == null ? 1.0 : smoothWeight;

        this.data = dataCost;
        this.sampling = dataCost.getSampling();
        this.mask = mask;
        this.dirs = new Volume[NUM_DIR];
        this.pdirs = new Volume[NUM_DIR];

        for (int i = 0; i < NUM_DIR; i++)
        {
            this.dirs[i] = dataCost.proto(this.values);
            this.pdirs[i] = dataCost.proto(this.values);
        }

        this.image = image;
        this.contrastWeight = contrastWeight == null ? 1.0 : contrastWeight;
        this.contrastGain = this.computeContrastGain();
    }

    private double computeContrastGain()
    {
        VectOnlineStats stats = new VectOnlineStats();
        Sampling sampling = this.image.getSampling();
        for (Sample s : sampling)
        {
            Vect v = this.image.get(s);
            Sample sa = new Sample(s.getI() + 1, s.getJ(), s.getK());
            Sample sb = new Sample(s.getI(), s.getJ() + 1, s.getK());
            Sample sc = new Sample(s.getI(), s.getJ(), s.getK() + 1);

            if (this.valid(sa))
            {
                stats.update(v.dist2(this.image.get(sa)));
            }

            if (this.valid(sb))
            {
                stats.update(v.dist2(this.image.get(sb)));
            }

            if (this.valid(sc))
            {
                stats.update(v.dist2(this.image.get(sc)));
            }
        }

        return stats.mean == 0 ? 1 : 1.0 / (2.0 * stats.mean);
    }

    private boolean valid(Sample sample)
    {
        return this.sampling.contains(sample) && (this.mask == null || this.mask.foreground(sample));
    }

    public void run(int iter)
    {
        for (int t = 0; t < iter; t++)
        {
            for (Sample ps : this.sampling)
            {
                if (!this.valid(ps))
                {
                    continue;
                }

                Vect pi = this.image.get(ps);
                for (int d = 0; d < NUM_DIR; d++)
                {
                    Sample qs = new Sample(ps.getI() + DIRX[d], ps.getJ() + DIRY[d], ps.getK() + DIRZ[d]);
                    double contrast = this.valid(qs) ? Math.exp(-1.0 * this.contrastGain * pi.dist2(this.image.get(qs))) : 0.0;

                    for (int qv = 0; qv < this.values; qv++)
                    {
                        double sum = 0;
                        double min = Double.MAX_VALUE;
                        for (int pv = 0; pv < this.values; pv++)
                        {
                            double data = this.data.get(ps, pv);
                            double smooth = pv == qv ? 0 : 1;

                            double psum = this.dataWeight * data;
                            psum += this.smoothWeight * smooth;
                            psum += this.contrastWeight * smooth * contrast;

                            for (int s = 0; s < NUM_DIR; s++)
                            {
                                Sample q = new Sample(ps.getI() - DIRX[s], ps.getJ() - DIRY[s], ps.getK() - DIRZ[s]);
                                if (REVERSE[s] != d && this.valid(q))
                                {
                                    psum += this.pdirs[s].get(q, pv);
                                }
                            }

                            min = Math.min(min, psum);
                            sum += psum;
                        }

                        min /= sum;

                        this.dirs[d].set(ps, qv, min);
                    }
                }
            }

            Volume[] tmp = this.pdirs;
            this.pdirs = this.dirs;
            this.dirs = tmp;
        }
    }

    public Volume output()
    {
        Volume out = this.data.proto(1);

        for (Sample p : this.sampling)
        {
            if (!this.valid(p))
            {
                continue;
            }

            int best = -1;
            Double min = Double.MAX_VALUE;
            for (int v = 0; v < this.values; v++)
            {
                double val = this.dataWeight * this.data.get(p, v);
                for (int d = 0; d < NUM_DIR; d++)
                {
                    Sample q = new Sample(p.getI() - DIRX[d], p.getJ() - DIRY[d], p.getK() - DIRZ[d]);
                    if (REVERSE[d] != d && this.valid(q))
                    {
                        val += this.dirs[d].get(q, v);
                    }
                }

                if (best == -1 || val < min)
                {
                    min = val;
                    best = v;
                }
            }

            out.set(p, 0, best);
        }

        return out;
    }
}