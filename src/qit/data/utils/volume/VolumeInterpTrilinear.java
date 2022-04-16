/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.utils.volume;

import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.math.structs.VectFunction;

public class VolumeInterpTrilinear extends VectFunction
{
    public static final String NAME = "Trilinear";

    private Volume volume;

    public VolumeInterpTrilinear(Volume vol)
    {
        super(3, vol.getDim());
        this.volume = vol;
    }

    public void apply(Vect input, Vect output)
    {
        Sampling samp = this.volume.getSampling();

        int length = 2;
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

        if (sx < 0 || sx >= samp.numI() ||
            sy < 0 || sy >= samp.numJ() ||
            sz < 0 || sz >= samp.numK())

        {
            output.setAll(0.0);
            return;
        }

        for (int idx = 0; idx < length; idx++)
        {
            wx[idx] = triangle(idx - dx);
            wy[idx] = triangle(idx - dy);
            wz[idx] = triangle(idx - dz);
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
                        int xidx = sx + i;
                        int yidx = sy + j;
                        int zidx = sz + k;

                        if (samp.contains(xidx, yidx, zidx))
                        {
                            double v = this.volume.get(xidx, yidx, zidx, didx);
                            sumv += v * w;
                            sumw += w;
                        }
                    }
                }
            }

            output.set(didx, sumw == 0 ? 0.0 : sumv / sumw);
        }
    }

    public static double triangle(double x)
    {
        double r = x < 0 ? -x : x;
        if (x < 1.0)
        {
            return 1 - r;
        }
        else
        {
            return 0.0;
        }
    }
}
