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
import qit.data.source.MaskSource;

public class VolumeMRFBP
{
    public final static int ITER = 100;
    public final static int NUM_DIR = 6;
    public final static int[] DIRX = { 1, -1, 0, 0, 0, 0 };
    public final static int[] DIRY = { 0, 0, 1, -1, 0, 0 };
    public final static int[] DIRZ = { 0, 0, 0, 0, 1, -1 };
    public final static int[] REVERSE = { 1, 0, 3, 2, 5, 4 };

    private int values;
    private double dataWeight = 1.0;
    private double smoothWeight = 1.0;

    private Volume data;
    private Mask mask;

    private Volume[] dirs;
    private Volume[] pdirs;

    public VolumeMRFBP(Volume dataCost)
    {
        // the input is a vector valued volume, where the i-th value at
        // each voxel is the cost of assigning the i-th label to that voxel

        this.values = dataCost.getDim();
        this.data = dataCost;

        this.dirs = new Volume[NUM_DIR];
        this.pdirs = new Volume[NUM_DIR];

        for (int i = 0; i < NUM_DIR; i++)
        {
            this.dirs[i] = dataCost.proto(this.values);
            this.pdirs[i] = dataCost.proto(this.values);
        }
    }
    
    public VolumeMRFBP withMask(Mask m)
    {
        this.mask = m;
        return this;
    }
    
    public VolumeMRFBP withDataWeight(double v)
    {
        this.dataWeight = v;
        return this;
    }
    
    public VolumeMRFBP withSmoothWeight(double v)
    {
        this.smoothWeight = v;
        return this;
    }

    public VolumeMRFBP run(int iter)
    {
        for (int t = 0; t < iter; t++)
        {
            for (Sample p : this.data.getSampling())
            {
                if (this.data.valid(p, this.mask))
                {
                    for (int d = 0; d < NUM_DIR; d++)
                    {
                        for  (int qv = 0; qv < this.values; qv++)
                        {
                            double sum = 0;
                            double min = Double.MAX_VALUE;
                            for (int pv = 0; pv < this.values; pv++)
                            {
                                double data = this.data.get(p, pv);
                                double smooth = pv == qv ? 0 : 1;

                                double psum = this.dataWeight * data;
                                psum += this.smoothWeight * smooth;

                                for (int s = 0; s < NUM_DIR; s++)
                                {
                                    Sample q = new Sample(p.getI() - DIRX[s], p.getJ() - DIRY[s], p.getK() - DIRZ[s]);
                                    if (REVERSE[s] != d && this.data.valid(q, this.mask))
                                    {
                                        psum += this.pdirs[s].get(q, pv);
                                    }
                                }

                                min = Math.min(min, psum);
                                sum += psum;
                            }

                            min /= sum;

                            this.dirs[d].set(p, qv, min);
                        }
                    }
                }
            }

            Volume[] tmp = this.pdirs;
            this.pdirs = this.dirs;
            this.dirs = tmp;
        }
        
        return this;
    }
    
    public VolumeMRFBP run()
    {
        for (int i = 0; i < ITER; i++)
        {
            Logging.progress("started belief propagation iteration " + (i + 1) + " of " + ITER);
            this.run();
        }
        
        return this;
    }

    public Mask getOutput()
    {
        Mask out = MaskSource.create(this.data.getSampling());

        for (Sample p : this.data.getSampling())
        {
            if (this.data.valid(p, this.mask))
            {
                int best = -1;
                Double min = Double.MAX_VALUE;
                for (int v = 0; v < this.values; v++)
                {
                    double val = this.dataWeight * this.data.get(p, v);

                    for (int d = 0; d < NUM_DIR; d++)
                    {
                        Sample q = new Sample(p.getI() - DIRX[d], p.getJ() - DIRY[d], p.getK() - DIRZ[d]);
                        if (REVERSE[d] != d && this.data.valid(q, this.mask))
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

                out.set(p, best);
            }
        }

        return out;
    }
}