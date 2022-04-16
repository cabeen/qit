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

package qit.data.modules.volume;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.vects.stats.VectStats;
import qit.math.structs.Quaternion;

@ModuleDescription("Compute a projection of an image volume along one axis, for example a minimum intensity projection")
@ModuleAuthor("Ryan Cabeen")
public class VolumeProjection implements Module
{
    public enum VolumeProjectionType
    {
        Min, Max, Mean, Sum, Var, CV, Median, IQR
    }

    public enum VolumeProjectionAxis
    {
        i, j, k
    }

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the axis for the projection")
    public VolumeProjectionAxis axis = VolumeProjectionAxis.k;

    @ModuleParameter
    @ModuleDescription("the type of statistic for the projection")
    public VolumeProjectionType type = VolumeProjectionType.Mean;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use thin projection, whereby groups of the given number of slices are aggregated")
    public Integer thin = null;

    @ModuleParameter
    @ModuleDescription("when collapsing the volume to a single slice, use this image index for the position")
    public int index = 0;

    @ModuleOutput
    @ModuleDescription("output image slice")
    public Volume output;

    @Override
    public VolumeProjection run()
    {
        this.output = this.thin != null ? this.thin() : this.full();

        return this;
    }

    public Volume thin()
    {
        Sampling insamp = this.input.getSampling();
        int nd = this.input.getDim();
        Volume out = this.thinProto();
        Sampling outsamp = out.getSampling();

        switch (this.axis)
        {
            case i:
            {
                for (int d = 0; d < nd; d++)
                {
                    for (Sample s : outsamp)
                    {
                        Vect values = VectSource.createND(this.thin);

                        for (int t = 0; t < this.thin; t++)
                        {
                            int ti = this.thin * s.getI()  + t - this.thin / 2;
                            Sample ts = new Sample(ti, s.getJ(), s.getK());
                            if (insamp.contains(ts))
                            {
                                values.set(t, this.input.get(ts, d));
                            }
                        }

                        out.set(s, d, this.stats(values));
                    }
                }
                break;
            }

            case j:
            {
                for (int d = 0; d < nd; d++)
                {
                    for (Sample s : out.getSampling())
                    {
                        Vect values = VectSource.createND(this.thin);

                        for (int t = 0; t < this.thin; t++)
                        {
                            int tj = this.thin * s.getJ()  + t - this.thin / 2;
                            Sample ts = new Sample(s.getI(), tj, s.getK());
                            if (insamp.contains(ts))
                            {
                                values.set(t, this.input.get(ts, d));
                            }
                        }

                        out.set(s, d, this.stats(values));
                    }
                }
                break;
            }

            case k:
            {
                for (int d = 0; d < nd; d++)
                {
                    for (Sample s : out.getSampling())
                    {
                        Vect values = VectSource.createND(this.thin);

                        for (int t = 0; t < this.thin; t++)
                        {
                            int tk = this.thin * s.getK()  + t - this.thin / 2;
                            Sample ts = new Sample(s.getI(), s.getJ(), tk);
                            if (insamp.contains(ts))
                            {
                                values.set(t, this.input.get(ts, d));
                            }
                        }

                        out.set(s, d, this.stats(values));
                    }
                }
                break;
            }
        }

        return out;
    }

    private Volume thinProto()
    {
        Sampling sampling = this.input.getSampling();

        double di = sampling.deltaI();
        double dj = sampling.deltaJ();
        double dk = sampling.deltaK();

        int ni = sampling.numI();
        int nj = sampling.numJ();
        int nk = sampling.numK();

        switch (this.axis)
        {
            case i:
            {
                ni /= this.thin;
                di *= this.thin;
                break;
            }
            case j:
            {
                nj /= this.thin;
                dj *= this.thin;
                break;
            }
            case k:
            {
                nk /= this.thin;
                dk *= this.thin;
                break;
            }
            default:
                throw new RuntimeException("bug in volume prototyping for projection!");
        }


        Vect s = sampling.start();
        Vect d = VectSource.create3D(di, dj, dk);
        Quaternion r = sampling.quat();
        Integers n = new Integers(ni, nj, nk);

        Sampling nsampling = new Sampling(s, d, r, n);

        return VolumeSource.create(nsampling, this.input.getDim());
    }

    public Volume full()
    {
        Sampling sampling = this.input.getSampling();

        int ni = sampling.numI();
        int nj = sampling.numJ();
        int nk = sampling.numK();
        int nd = this.input.getDim();

        Volume out = this.fullProto();

        switch (this.axis)
        {
            case i:
            {
                for (int d = 0; d < nd; d++)
                {
                    for (int j = 0; j < nj; j++)
                    {
                        for (int k = 0; k < nk; k++)
                        {
                            Vect values = VectSource.createND(ni);

                            for (int i = 0; i < ni; i++)
                            {
                                values.set(i, this.input.get(i, j, k, d));
                            }

                            out.set(0, j, k, d, this.stats(values));
                        }
                    }
                }
                break;
            }

            case j:
            {
                for (int d = 0; d < nd; d++)
                {
                    for (int i = 0; i < ni; i++)
                    {
                        for (int k = 0; k < nk; k++)
                        {
                            Vect values = VectSource.createND(nj);

                            for (int j = 0; j < nj; j++)
                            {
                                values.set(j, this.input.get(i, j, k, d));
                            }

                            out.set(i, 0, k, d, this.stats(values));
                        }
                    }
                }
                break;
            }

            case k:
            {
                for (int d = 0; d < nd; d++)
                {
                    for (int i = 0; i < ni; i++)
                    {
                        for (int j = 0; j < nj; j++)
                        {
                            Vect values = VectSource.createND(nk);

                            for (int k = 0; k < nk; k++)
                            {
                                values.set(k, this.input.get(i, j, k, d));
                            }

                            out.set(i, j, 0, d, this.stats(values));
                        }
                    }
                }
                break;
            }
        }

        return out;
    }

    private Volume fullProto()
    {
        switch (this.axis)
        {
            case i:
            {
                Sampling sampling = this.input.getSampling();
                Sample start = new Sample(this.index, 0, 0);
                Sample end = new Sample(this.index + 1, sampling.numJ(), sampling.numK());
                Sampling nsampling = this.input.getSampling().range(start, end);

                return VolumeSource.create(nsampling, this.input.getDim());
            }
            case j:
            {
                Sampling sampling = this.input.getSampling();
                Sample start = new Sample(0, this.index, 0);
                Sample end = new Sample(sampling.numI(), this.index + 1, sampling.numK());
                Sampling nsampling = this.input.getSampling().range(start, end);

                return VolumeSource.create(nsampling, this.input.getDim());
            }
            case k:
            {
                Sampling sampling = this.input.getSampling();
                Sample start = new Sample(0, 0, this.index);
                Sample end = new Sample(sampling.numI(), sampling.numJ(), this.index + 1);
                Sampling nsampling = this.input.getSampling().range(start, end);

                return VolumeSource.create(nsampling, this.input.getDim());
            }
            default:
                throw new RuntimeException("bug in volume prototyping for projection!");
        }
    }

    private double stats(Vect values)
    {
        VectStats stats = new VectStats().withInput(values).run();

        switch (this.type)
        {
            case Min:
                return stats.min;
            case Max:
                return stats.max;
            case Mean:
                return stats.mean;
            case Sum:
                return stats.sum;
            case CV:
                return stats.cv;
            case Var:
                return stats.var;
            case Median:
                return stats.median;
            case IQR:
                return stats.iqr;
            default:
                throw new RuntimeException("invalid statistic: " + this.type);
        }
    }

}
