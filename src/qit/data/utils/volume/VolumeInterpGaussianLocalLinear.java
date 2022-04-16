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

import com.google.common.collect.Lists;
import qit.base.structs.Triple;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.MatrixSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;

public class VolumeInterpGaussianLocalLinear extends VectFunction
{
    public static final String NAME = "GaussianLocalLinear";

    private Volume volume;
    private double sigma = 1.0;
    private int support = 3;

    public VolumeInterpGaussianLocalLinear(Volume vol)
    {
        super(3, vol.getDim());
        this.volume = vol;
    }

    public VolumeInterpGaussianLocalLinear withSigma(double v)
    {
        this.sigma = v;
        return this;
    }

    public VolumeInterpGaussianLocalLinear withSupport(int n)
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
            double sumw = 0;
            List<Triple<Double,Vect,Double>> values = Lists.newArrayList();
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
                            sumw += w;
                            Vect p = sampling.world(xidx, yidx, zidx);
                            double v = this.volume.get(xidx, yidx, zidx, didx);

                            values.add(Triple.of(w, p, v));
                        }
                    }
                }
            }

            int dim = 4;
            int num = values.size();
            Vect ws = new Vect(num);
            Matrix xs = new Matrix(num, dim);
            Vect ys = new Vect(num);

            for (int i = 0; i < values.size(); i++)
            {
                Triple<Double,Vect,Double> value = values.get(i);
                double w = MathUtils.zero(sumw) ? 1.0 : value.a / sumw;
                double x = value.b.get(0);
                double y = value.b.get(1);
                double z = value.b.get(2);
                double v = value.c;

                ws.set(i, w);
                xs.set(i, 0, 1);
                xs.set(i, 1, x);
                xs.set(i, 2, y);
                xs.set(i, 3, z);
                ys.set(i, v);
            }

            Matrix xst = xs.transpose();
            Matrix xstws = xst.times(MatrixSource.diag(ws));
            Vect beta = xstws.times(xs).inv().times(xstws.times(ys));

            double px = input.get(0);
            double py = input.get(1);
            double pz = input.get(2);
            Vect p = new Vect(dim);
            p.set(0, 1);
            p.set(1, px);
            p.set(2, py);
            p.set(3, pz);
            double pv = beta.dot(p);

            output.set(didx, pv);
        }
    }

    public static double kernel(double h, double x)
    {
        double x2 = x * x;
        double h2 = h * h;
        return Math.exp(-x2 / h2);
    }

}