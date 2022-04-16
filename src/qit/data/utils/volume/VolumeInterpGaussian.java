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

import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

public class VolumeInterpGaussian extends VectFunction
{
    public static final String NAME = "Gaussian";

    private Volume volume;
    private double sigma = 1.5;
    private int support = 3;

    public VolumeInterpGaussian(Volume vol)
    {
        super(3, vol.getDim());
        this.volume = vol;
    }

    public VolumeInterpGaussian withSigma(double v)
    {
        this.sigma = v;
        return this;
    }

    public VolumeInterpGaussian withSupport(int n)
    {
        this.support = n;
        return this;
    }

    public void apply(Vect input, Vect output)
    {
        Sampling sampling = this.volume.getSampling();

        if (!sampling.contains(input))
        {
            output.setAll(0.0);
            return;
        }

        Vect voxel = sampling.voxel(input);

        double vx = voxel.get(0);
        double vy = voxel.get(1);
        double vz = voxel.get(2);
        int sx = (int) Math.floor(vx);
        int sy = (int) Math.floor(vy);
        int sz = (int) Math.floor(vz);
        double dx = vx - sx;
        double dy = vy - sy;
        double dz = vz - sz;

        int length = 2 * (this.support + 1);
        double[] wx = new double[length];
        double[] wy = new double[length];
        double[] wz = new double[length];

        for (int i = 0; i < length; i++)
        {
            wx[i] = kernel(this.sigma, (i - this.support - dx) * sampling.deltaI());
            wy[i] = kernel(this.sigma, (i - this.support - dy) * sampling.deltaJ());
            wz[i] = kernel(this.sigma, (i - this.support - dz) * sampling.deltaK());
        }

        for (int didx = 0; didx < this.volume.getDim(); didx++)
        {
            double sumv = 0;
            double sumw = 0;

            for (int k = 0; k < length; k++)
            {
                double wk = wz[k];

                for (int j = 0; j < length; j++)
                {
                    double wj = wy[j];
                    double wjk = wj * wk;

                    for (int i = 0; i < length; i++)
                    {
                        double w = wx[i] * wjk;
                        int xidx = sx + i - this.support;
                        int yidx = sy + j - this.support;
                        int zidx = sz + k - this.support;

                        if (sampling.contains(xidx, yidx, zidx))
                        {
                            sumv += w * this.volume.get(xidx, yidx, zidx, didx);
                            sumw += w;
                        }
                    }
                }
            }

            output.set(didx, MathUtils.zero(sumw) ? 0.0 : sumv / sumw);
        }
    }

    public static double kernel(double h, double x)
    {
        double x2 = x * x;
        double h2 = h * h;
        return Math.exp(-x2 / h2);
    }

}