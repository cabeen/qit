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

package qit.data.utils.mri.estimation;

import com.google.common.collect.Lists;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.enums.KernelInterpolationType;
import qit.data.utils.volume.VolumeInterpTrilinear;
import qit.base.Model;
import qit.data.utils.mri.structs.ModelEstimator;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;

public class VolumeKernelModelEstimator<M extends Model<M>> extends VectFunction
{
    public Volume volume = null;
    public Mask mask = null;
    public ModelEstimator estimator = null;
    public M proto = null;
    public KernelInterpolationType interp = KernelInterpolationType.Trilinear;
    public Integer support = 1;
    public Double hpos = 1d;
    public Double hval = null;
    public Double hsig = null;

    public VolumeKernelModelEstimator(M p)
    {
        this.init(3, p.getEncodingSize());
        this.proto = p;
    }

    public void apply(Vect input, Vect output)
    {
        Vect value = estimate(input);

        if (value == null)
        {
            output.setAll(0);
        }
        else
        {
            output.set(value);
        }
    }

    public Vect estimate(Vect coord)
    {
        return this.estimate(coord, null);
    }

    public Vect estimate(Vect coord, Vect ref)
    {
        switch(this.interp)
        {
            case Gaussian:
                return this.estimateGaussian(coord, ref);
            case Trilinear:
                return this.estimateTrilinear(coord);
            case Nearest:
                return this.estimateNearest(coord);
            default:
                throw new RuntimeException("invalid interpolation: " + this.interp);
        }
    }

    public Vect estimateNearest(Vect coord)
    {
        Sampling sampling = this.volume.getSampling();
        Sample sample = sampling.nearest(coord);

        if (this.volume.valid(sample, this.mask))
        {
            return this.volume.get(sample);
        }
        else
        {
            return new Vect(this.getDimOut());
        }
    }

    public Vect estimateTrilinear(Vect coord)
    {
        List<Double> weights = Lists.newArrayList();
        List<Vect> models = Lists.newArrayList();
        Sampling sampling = this.volume.getSampling();

        Vect voxel = sampling.voxel(coord);

        double cx = voxel.getX();
        int sx = (int) Math.floor(cx);
        double dx = cx - sx;

        double cy = voxel.getY();
        int sy = (int) Math.floor(cy);
        double dy = cy - sy;

        double cz = voxel.getZ();
        int sz = (int) Math.floor(cz);
        double dz = cz - sz;

        int length = 2;
        double[] wx = new double[length];
        double[] wy = new double[length];
        double[] wz = new double[length];

        if (!sampling.contains(sx, sy, sz))
        {
            return new Vect(this.getDimOut());
        }

        for (int i = 0; i < length; i++)
        {
            wx[i] = VolumeInterpTrilinear.triangle(i - dx);
            wy[i] = VolumeInterpTrilinear.triangle(i - dy);
            wz[i] = VolumeInterpTrilinear.triangle(i - dz);
        }

        for (int k = 0; k < length; k++)
        {
            double wk = wz[k];

            for (int j = 0; j < length; j++)
            {
                double wj = wy[j];
                double wjk = wj * wk;

                for (int i = 0; i < length; i++)
                {
                    double weight = wx[i] * wjk;
                    int xidx = sx + i;
                    int yidx = sy + j;
                    int zidx = sz + k;
                    Sample sample = new Sample(xidx, yidx, zidx);

                    if (this.volume.valid(sample, this.mask))
                    {
                        weights.add(weight);
                        models.add(this.volume.get(sample));
                    }
                }
            }
        }

        MathUtils.timesEquals(weights, 1.0 / MathUtils.sum(weights));

        return this.estimator.run(weights, models);
    }

    public Vect estimateGaussian(Vect coord, Vect ref)
    {
        List<Double> weights = Lists.newArrayList();
        List<Vect> models = Lists.newArrayList();
        Sampling sampling = this.volume.getSampling();
        Sample nearest = sampling.nearest(coord);

        for (int dk = -this.support; dk <= this.support; dk++)
        {
            for (int dj = -this.support; dj <= this.support; dj++)
            {
                for (int di = -this.support; di <= this.support; di++)
                {
                    int ni = nearest.getI() + di;
                    int nj = nearest.getJ() + dj;
                    int nk = nearest.getK() + dk;
                    Sample ns = new Sample(ni, nj, nk);
                    Vect nv = sampling.world(ns);

                    if (this.volume.valid(ns, this.mask))
                    {
                        double weight = 1.0;
                        {
                            double d2 = nv.dist2(coord);
                            double h2 = this.hpos * this.hpos;
                            double kern = Math.exp(-d2 / h2);
                            weight *= kern;
                        }

                        if (this.hval != null && ref != null && !MathUtils.zero(this.hval))
                        {
                            Vect nref = this.volume.get(ns);
                            M left = this.proto.proto().setEncoding(ref);
                            M right = this.proto.proto().setEncoding(nref);

                            double d2 = left.dist(right);
                            double h2 = this.hval * this.hval;
                            double kern = Math.exp(-d2 / h2);
                            weight *= kern;
                        }

                        if (this.hsig != null && ref != null && !MathUtils.zero(this.hsig))
                        {
                            Vect nref = this.volume.get(ns);
                            M left = this.proto.proto().setEncoding(ref);
                            M right = this.proto.proto().setEncoding(nref);

                            double db = left.baseline() - right.baseline();
                            double d2 = db * db;
                            double h2 = this.hsig * this.hsig;
                            double kern = Math.exp(-d2 / h2);
                            weight *= kern;
                        }

                        weights.add(weight);
                        models.add(this.volume.get(ns));
                    }
                }
            }
        }

        double sumw = MathUtils.sum(weights);

        if (weights.size() == 0 || MathUtils.zero(sumw))
        {
            return new Vect(this.getDimOut());
        }
        else
        {
            MathUtils.timesEquals(weights, 1.0 / sumw);

            return this.estimator.run(weights, models);
        }
    }
}