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

public class VolumeInterpCubicKernel extends VectFunction
{
    public static final String NAME = "CubicKernel";

    private Volume vol;

    public VolumeInterpCubicKernel(Volume vol)
    {
        super(3, vol.getDim());
        this.vol = vol;
    }

    public void apply(Vect input, Vect output)
    {
        Sampling samp = this.vol.getSampling();
        
        int width = 1;
        int length = 2 * (width + 1);
        
        double[] wx = new double[length];
        double[] wy = new double[length];
        double[] wz = new double[length];
        
        Vect voxel = samp.voxel(input);
        
        double cx = voxel.getX();
        int sx = (int) Math.floor(cx);
        double dx = cx - sx;

        double cy = voxel.getY();
        int sy = (int) Math.floor(cy);
        double dy = cy - sy;

        double cz = voxel.getZ();
        int sz = (int) Math.floor(cz);
        double dz = cz - sz;

        if (sx <= -width || sx >= samp.numI() + width ||
            sy <= -width || sy >= samp.numJ() + width ||
            sz <= -width || sz >= samp.numK() + width)
        {
            output.setAll(0.0);
            return;
        }

        for (int i = 0; i < length; i++)
        {
            wx[i] = kernel(i - width - dx);
            wy[i] = kernel(i - width - dy);
            wz[i] = kernel(i - width - dz);
        }
        
        for (int didx = 0; didx < this.vol.getDim(); didx++)
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
                        int xidx = sx + i - width;
                        int yidx = sy + j - width;
                        int zidx = sz + k - width;

                        if (samp.contains(xidx, yidx, zidx))
                        {
                            double v = this.vol.get(xidx, yidx, zidx, didx);
                            sumv += v * w;
                            sumw += w;
                        }
                    }
                }
            }

            output.set(didx, sumw == 0 ? 0.0 : sumv / sumw);
        }
    }

    public static double kernel(double x)
    {
        double r = x < 0 ? -x : x;
        if (r <= 1)
        {
            return 1. / 6. * (4 + (-6 + 3 * r) * r * r);
        }
        else if (r <= 2)
        {
            r -= 1;
            return 1. / 6. * (1 + (-3 + (3 - r) * r) * r);
        }
        else
        {
            return 0.0;
        }
    }
}