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

package qit.data.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.math3.stat.inference.TTest;
import qit.base.Global;
import qit.base.Logging;
import qit.base.ModelType;
import qit.base.structs.DataType;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.data.datasets.Affine;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.modules.volume.VolumeDownsample;
import qit.data.modules.volume.VolumeFilterMedian;
import qit.data.modules.volume.VolumeNormalize;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.enums.ReorientationType;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.data.utils.volume.*;
import qit.math.structs.Gaussian;
import qit.math.structs.Histogram;
import qit.math.structs.VectFunction;
import qit.math.source.VectFunctionSource;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * utilties for processing volumes
 */
public class VolumeUtils
{
    public static Volume mag(Volume volume)
    {
        Volume out = volume.proto();

        for (Sample sample : volume.getSampling())
        {
            out.set(sample, volume.get(sample).norm());
        }

        return out;
    }

    public static Volume ratio(Volume a, Volume b)
    {
        Volume out = a.proto();

        for (Sample sample : a.getSampling())
        {
            for (int i = 0; i < a.getDim(); i++)
            {
                double av = a.get(sample, i);
                double bv = b.get(sample, i);

                out.set(sample, i, av / bv);
            }
        }

        return out;
    }

    public static void lincombEquals(double alpha, Volume a, double beta, Volume b)
    {
        for (Sample sample : a.getSampling())
        {
            for (int i = 0; i < a.getDim(); i++)
            {
                double av = a.get(sample, i);
                double bv = b.get(sample, Math.min(b.getDim() - 1, i));

                a.set(sample, i, alpha * av + beta * bv);
            }
        }
    }

    public static Volume lincomb(double alpha, Volume a, double beta, Volume b)
    {
        Volume out = a.proto();

        for (Sample sample : a.getSampling())
        {
            for (int i = 0; i < a.getDim(); i++)
            {
                double av = a.get(sample, i);
                double bv = b.get(sample, Math.min(b.getDim() - 1, i));

                out.set(sample, i, alpha * av + beta * bv);
            }
        }

        return out;
    }

    public static Map<Integer, VectsOnlineStats> stats(Volume volume, Mask mask)
    {
        Global.assume(volume.getSampling().equals(mask.getSampling()), "invalid samplings");

        Map<Integer, VectsOnlineStats> stats = Maps.newHashMap();

        for (Sample sample : volume.getSampling())
        {
            if (!volume.valid(sample, mask))
            {
                continue;
            }

            int label = mask.get(sample);
            Vect value = volume.get(sample);

            // update statistics
            if (!stats.containsKey(label))
            {
                stats.put(label, new VectsOnlineStats(volume.getDim()));
            }
            stats.get(label).update(value);
        }

        return stats;
    }

    public static void set(Volume volume, Sample sample, Volume block)
    {
        Sampling sampling = volume.getSampling();
        Sampling bsampling = block.getSampling();
        for (int i = 0; i < bsampling.numI(); i++)
        {
            for (int j = 0; j < bsampling.numJ(); j++)
            {
                for (int k = 0; k < bsampling.numK(); k++)
                {
                    Sample bsample = new Sample(sample.getI() + i, sample.getJ() + j, sample.getK() + k);
                    if (sampling.contains(bsample))
                    {
                        volume.set(bsample, block.get(i, j, k));
                    }
                }
            }
        }
    }

    public static Map<Sample, Volume> blocks(Volume volume, Integer bx, Integer by, Integer bz)
    {
        Sampling sampling = volume.getSampling();
        if (bx == null || bx == 0)
        {
            bx = sampling.numI();
        }
        if (by == null || by == 0)
        {
            by = sampling.numJ();
        }
        if (bz == null || bz == 0)
        {
            bz = sampling.numK();
        }

        int dim = volume.getDim();

        double sx = sampling.startI();
        double sy = sampling.startJ();
        double sz = sampling.startK();

        double dx = sampling.deltaI();
        double dy = sampling.deltaJ();
        double dz = sampling.deltaK();

        int nx = sampling.numI();
        int ny = sampling.numJ();
        int nz = sampling.numK();

        int nbx = nx / bx;
        int nby = nx / bx;
        int nbz = nx / bx;

        Map<Sample, Volume> output = Maps.newConcurrentMap();
        for (int i = 0; i < nbx; i++)
        {
            for (int j = 0; j < nby; j++)
            {
                for (int k = 0; k < nbz; k++)
                {
                    int si = bx * i;
                    int sj = by * j;
                    int sk = bz * k;

                    Vect start = VectSource.create3D(sx, sy, sz);
                    Vect delta = VectSource.create3D(dx, dy, dz);
                    Integers num = new Integers(nx, ny, nz);

                    Sampling bsampling = new Sampling(start, delta, num);

                    Volume block = VolumeSource.create(bsampling, dim);
                    for (int ii = 0; ii < bx; ii++)
                    {
                        for (int jj = 0; jj < by; jj++)
                        {
                            for (int kk = 0; kk < bz; kk++)
                            {
                                block.set(ii, jj, kk, volume.get(si, sj, sk));
                            }
                        }
                    }
                    output.put(new Sample(si, sj, sk), block);
                }
            }
        }
        return output;
    }

    public static Volume mean(Volume volume)
    {
        Volume out = volume.proto(1);

        for (Sample sample : volume.getSampling())
        {
            out.set(sample, 0, volume.get(sample).mean());
        }

        return out;
    }

    public static Volume max(Sampling sampling, List<Sample> samples, Matrix resp)
    {
        if (samples.size() != resp.cols())
        {
            throw new RuntimeException("invalid responsibility matrix");
        }

        Volume out = VolumeSource.create(sampling);
        out.setAll(VectSource.create1D());
        for (int i = 0; i < samples.size(); i++)
        {
            double max = resp.getColumn(i).max();
            out.set(samples.get(i), 0, max);
        }

        return out;
    }

    public static Volume pack(Sampling sampling, List<Sample> samples, Vect values)
    {
        Volume out = VolumeSource.create(sampling);
        out.setAll(VectSource.create1D());
        for (int i = 0; i < samples.size(); i++)
        {
            out.set(samples.get(i), 0, values.get(i));
        }

        return out;
    }

    public static Volume cost(Sampling sampling, List<Sample> samples, Matrix resp)
    {
        if (samples.size() != resp.cols())
        {
            throw new RuntimeException("invalid responsibility matrix");
        }

        int dim = resp.rows();
        Volume out = VolumeSource.create(sampling, dim);
        out.setAll(VectSource.createND(dim));
        for (int i = 0; i < samples.size(); i++)
        {
            for (int j = 0; j < dim; j++)
            {
                double r = resp.get(j, i);
                double c = -Math.log(r);
                out.set(samples.get(i), 0, c);
            }
        }

        return out;
    }

    public static double nrss(Volume a, Volume b)
    {
        if (!a.getSampling().equals(b.getSampling()))
        {
            throw new RuntimeException("samplings must match");
        }

        double rss = 0;

        for (Sample s : a.getSampling())
        {
            double va = a.get(s, 0);
            double vb = b.get(s, 0);

            double dab = va - vb;
            double dab2 = dab * dab;
            double denom = (va + vb) / 2.0;

            if (MathUtils.nonzero(denom))
            {
                rss += Math.sqrt(dab2) / denom;
            }
        }

        return rss;
    }

    public static double rss(Volume a, Volume b)
    {
        if (!a.getSampling().equals(b.getSampling()))
        {
            throw new RuntimeException("samplings must match");
        }

        double rss = 0;

        for (Sample s : a.getSampling())
        {
            double va = a.get(s, 0);
            double vb = b.get(s, 0);

            double dab = va - vb;

            rss += dab * dab;
        }

        return rss;
    }

    public static void copy(Volume volume, int fidx, Volume to, int tidx)
    {
        if (!volume.getSampling().equals(to.getSampling()))
        {
            throw new RuntimeException("samplings do not match");
        }

        if (fidx < 0 || tidx < 0 || fidx >= volume.getDim() || tidx >= to.getDim())
        {
            throw new RuntimeException("invalid dims");
        }

        for (int i = 0; i < volume.getSampling().size(); i++)
        {
            to.set(i, tidx, volume.get(i, fidx));
        }
    }

    public static Volume mask(Volume volume, Mask mask)
    {
        if (mask == null)
        {
            return volume.copy();
        }

        return new VolumeFunction(VectFunctionSource.identity(volume.getDim())).withInput(volume).withMask(mask).run();
    }

    public static double diceWeighted(final Volume a, final Volume b)
    {
        // see :Cousineau, et al. (2017). A test-retest study on Parkinson's PPMI dataset yields statistically
        // significant white matter fascicles. NeuroImage: Clinical, 16, 222-233.

        Global.assume(a.getSampling().equals(b.getSampling()), "samplings must match");

        Volume na = new VolumeNormalize()
        {{
            this.input = a;
            this.type = VolumeNormalizeType.UnitMax;
        }}.run().output;

        Volume nb = new VolumeNormalize()
        {{
            this.input = b;
            this.type = VolumeNormalizeType.UnitMax;
        }}.run().output;

        double num = 0;
        double denom = 0;

        for (Sample s : a.getSampling())
        {
            double va = na.get(s, 0);
            double vb = nb.get(s, 0);
            double vab = va + vb;

            if (MathUtils.nonzero(va) && MathUtils.nonzero(vb))
            {
                num += vab;
            }

            denom += vab;
        }

        double wdice = MathUtils.zero(denom) ? 0 : num / denom;

        return wdice;
    }

    public static Volume projMaxX(Volume volume, Mask mask)
    {
        if (volume.getDim() != 1)
        {
            throw new RuntimeException("invalid channel");
        }

        Sampling sampling = volume.getSampling();
        Sample start = new Sample(0, 0, 0);
        Sample end = new Sample(1, sampling.numJ(), sampling.numK());
        Sampling nsampling = sampling.range(start, end);
        Volume nvolume = new Volume(nsampling, volume.getType(), 1);
        nvolume.setAll(VectSource.create1D(Double.MIN_VALUE));

        for (Sample sample : sampling)
        {
            if (volume.valid(sample, mask))
            {
                Sample nsample = new Sample(0, sample.getJ(), sample.getK());
                double nvalue = nvolume.get(nsample, 0);
                double value = volume.get(sample, 0);
                nvolume.set(nsample, 0, Math.max(nvalue, value));
            }
        }

        return nvolume;
    }

    public static Volume projMaxY(Volume volume, Mask mask)
    {
        if (volume.getDim() != 1)
        {
            throw new RuntimeException("invalid channel");
        }

        Sampling sampling = volume.getSampling();
        Sample start = new Sample(0, 0, 0);
        Sample end = new Sample(sampling.numI(), 1, sampling.numK());
        Sampling nsampling = sampling.range(start, end);
        Volume nvolume = new Volume(nsampling, volume.getType(), 1);
        nvolume.setAll(VectSource.create1D(Double.MIN_VALUE));

        for (Sample sample : sampling)
        {
            if (volume.valid(sample, mask))
            {
                Sample nsample = new Sample(sample.getI(), 0, sample.getK());
                double nvalue = nvolume.get(nsample, 0);
                double value = volume.get(sample, 0);
                nvolume.set(nsample, 0, Math.max(nvalue, value));
            }
        }

        return nvolume;
    }

    public static Volume projMaxZ(Volume volume, Mask mask)
    {
        if (volume.getDim() != 1)
        {
            throw new RuntimeException("invalid channel");
        }

        Sampling sampling = volume.getSampling();
        Sample start = new Sample(0, 0, 0);
        Sample end = new Sample(sampling.numI(), sampling.numJ(), 1);
        Sampling nsampling = sampling.range(start, end);
        Volume nvolume = new Volume(nsampling, volume.getType(), 1);
        nvolume.setAll(VectSource.create1D(Double.MIN_VALUE));

        for (Sample sample : sampling)
        {
            if (volume.valid(sample, mask))
            {
                Sample nsample = new Sample(sample.getI(), sample.getJ(), 0);
                double nvalue = nvolume.get(nsample, 0);
                double value = volume.get(sample, 0);
                nvolume.set(nsample, 0, Math.max(nvalue, value));
            }
        }

        return nvolume;
    }

    public static Volume projMeanX(Volume volume, Mask mask)
    {
        Sampling sampling = volume.getSampling();
        Sample start = new Sample(0, 0, 0);
        Sample end = new Sample(1, sampling.numJ(), sampling.numK());
        Sampling nsampling = sampling.range(start, end);
        Volume nvolume = new Volume(nsampling, volume.getType(), volume.getDim());

        double frac = 1.0 / sampling.numI();
        for (Sample sample : sampling)
        {
            if (volume.valid(sample, mask))
            {
                Sample nsample = new Sample(0, sample.getJ(), sample.getK());
                Vect nvalue = nvolume.get(nsample);
                nvalue.plusEquals(frac, volume.get(sample));
                nvolume.set(nsample, nvalue);
            }
        }

        return nvolume;
    }

    public static Volume projMeanY(Volume volume, Mask mask)
    {
        Sampling sampling = volume.getSampling();
        Sample start = new Sample(0, 0, 0);
        Sample end = new Sample(sampling.numI(), 1, sampling.numK());
        Sampling nsampling = sampling.range(start, end);
        Volume nvolume = new Volume(nsampling, volume.getType(), volume.getDim());

        double frac = 1.0 / sampling.numJ();
        for (Sample sample : sampling)
        {
            Sample nsample = new Sample(sample.getI(), 0, sample.getK());
            Vect nvalue = nvolume.get(nsample);
            nvalue.plusEquals(frac, volume.get(sample));
            nvolume.set(nsample, nvalue);
        }

        return nvolume;
    }

    public static Volume projMeanZ(Volume volume, Mask mask)
    {
        Sampling sampling = volume.getSampling();
        Sample start = new Sample(0, 0, 0);
        Sample end = new Sample(sampling.numI(), sampling.numJ(), 1);
        Sampling nsampling = sampling.range(start, end);
        Volume nvolume = new Volume(nsampling, volume.getType(), volume.getDim());

        double frac = 1.0 / sampling.numK();
        for (Sample sample : sampling)
        {
            Sample nsample = new Sample(sample.getI(), sample.getJ(), 0);
            Vect nvalue = nvolume.get(nsample);
            nvalue.plusEquals(frac, volume.get(sample));
            nvolume.set(nsample, nvalue);
        }

        return nvolume;
    }

    public static Volume stackX(Volume[] vols)
    {
        Sampling sampling = vols[0].getSampling();
        if (sampling.numI() != 1)
        {
            throw new RuntimeException("only 2D volumes can be stacked");
        }

        for (Volume vol : vols)
        {
            if (!vol.getSampling().equals(sampling))
            {
                throw new RuntimeException("stacked volumes must have the same sampling");
            }
            else if (vol.getDim() != vols[0].getDim())
            {
                throw new RuntimeException("stacked volumes must have the same data channel");
            }
            else if (vol.getType() != vols[0].getType())
            {
                throw new RuntimeException("stacked volumes must have the same data type");
            }
        }
        int nx = vols.length;
        int ny = sampling.numJ();
        int nz = sampling.numK();

        double dx = 1.0;
        double dy = sampling.deltaJ();
        double dz = sampling.deltaK();

        double sx = sampling.startI();
        double sy = sampling.startJ();
        double sz = sampling.startK();

        DataType type = vols[0].getType();
        int dim = vols[0].getDim();

        Vect start = VectSource.create3D(sx, sy, sz);
        Vect delta = VectSource.create3D(dx, dy, dz);
        Integers num = new Integers(nx, ny, nz);

        Sampling nsampling = new Sampling(start, delta, num);

        Volume vol = new Volume(nsampling, type, dim);

        for (Sample nsample : nsampling)
        {
            vol.set(nsample, vols[nsample.getI()].get(0, nsample.getJ(), nsample.getK()));
        }

        return vol;
    }

    public static Volume stackY(Volume[] vols)
    {
        Sampling sampling = vols[0].getSampling();
        if (sampling.numJ() != 1)
        {
            throw new RuntimeException("only 2D volumes can be stacked");
        }

        for (Volume vol : vols)
        {
            if (!vol.getSampling().equals(sampling))
            {
                throw new RuntimeException("stacked volumes must have the same sampling");
            }
            else if (vol.getDim() != vols[0].getDim())
            {
                throw new RuntimeException("stacked volumes must have the same data channel");
            }
            else if (vol.getType() != vols[0].getType())
            {
                throw new RuntimeException("stacked volumes must have the same data type");
            }
        }

        int nx = sampling.numI();
        int ny = vols.length;
        int nz = sampling.numK();

        double dx = sampling.deltaI();
        double dy = 1.0;
        double dz = sampling.deltaK();

        double sx = sampling.startI();
        double sy = sampling.startJ();
        double sz = sampling.startK();

        DataType type = vols[0].getType();
        int dim = vols[0].getDim();

        Vect start = VectSource.create3D(sx, sy, sz);
        Vect delta = VectSource.create3D(dx, dy, dz);
        Integers num = new Integers(nx, ny, nz);

        Sampling nsampling = new Sampling(start, delta, num);

        Volume vol = new Volume(nsampling, type, dim);

        for (Sample nsample : nsampling)
        {
            vol.set(nsample, vols[nsample.getJ()].get(nsample.getI(), 0, nsample.getK()));
        }

        return vol;
    }

    public static Volume stackZ(Volume[] vols)
    {
        Sampling sampling = vols[0].getSampling();
        if (sampling.numK() != 1)
        {
            throw new RuntimeException("only 2D volumes can be stacked");
        }

        for (Volume vol : vols)
        {
            if (!vol.getSampling().equals(sampling))
            {
                throw new RuntimeException("stacked volumes must have the same sampling");
            }
            else if (vol.getDim() != vols[0].getDim())
            {
                throw new RuntimeException("stacked volumes must have the same data channel");
            }
            else if (vol.getType() != vols[0].getType())
            {
                throw new RuntimeException("stacked volumes must have the same data type");
            }
        }

        int nx = sampling.numI();
        int ny = sampling.numJ();
        int nz = vols.length;

        double dx = sampling.deltaI();
        double dy = sampling.deltaJ();
        double dz = 1.0;

        double sx = sampling.startI();
        double sy = sampling.startJ();
        double sz = sampling.startK();

        DataType type = vols[0].getType();
        int dim = vols[0].getDim();

        Vect start = VectSource.create3D(sx, sy, sz);
        Vect delta = VectSource.create3D(dx, dy, dz);
        Integers num = new Integers(nx, ny, nz);

        Sampling nsampling = new Sampling(start, delta, num);

        Volume vol = new Volume(nsampling, type, dim);

        for (Sample nsample : nsampling)
        {
            vol.set(nsample, vols[nsample.getK()].get(nsample.getI(), nsample.getJ(), 0));
        }

        return vol;
    }

    public static Volume stack(Volume[] vols)
    {
        if (vols[0].getSampling().numI() == 1)
        {
            return stackX(vols);
        }
        else if (vols[0].getSampling().numJ() == 1)
        {
            return stackY(vols);
        }
        else if (vols[0].getSampling().numK() == 1)
        {
            return stackZ(vols);
        }
        else
        {
            throw new RuntimeException("cannot stack full volumes");
        }
    }

    public static Volume cat(Volume[] vols)
    {
        if (vols.length == 0)
        {
            throw new RuntimeException("no volumes found");
        }

        if (vols.length == 1)
        {
            return vols[0].copy();
        }

        DataType type = null;
        Sampling samp = null;
        int dim = 0;
        for (Volume v : vols)
        {
            if (samp == null)
            {
                samp = v.getSampling();
                type = v.getType();
            }
            else if (!samp.equals(v.getSampling()))
            {
                throw new RuntimeException("samplings do not match");
            }

            dim += v.getDim();
        }

        Volume out = new Volume(samp, type, dim);
        for (Sample s : samp)
        {
            int idx = 0;
            for (Volume v : vols)
            {
                for (int i = 0; i < v.getDim(); i++)
                {
                    out.set(s, idx++, v.get(s, i));
                }
            }
        }

        return out;
    }

    public static void flipX(Volume volume)
    {
        int nx = volume.getSampling().numI();
        int ny = volume.getSampling().numJ();
        int nz = volume.getSampling().numK();

        int n = (int) Math.ceil(nx / 2.0);
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < ny; j++)
            {
                for (int k = 0; k < nz; k++)
                {
                    int i2 = nx - 1 - i;
                    Vect va = volume.get(i, j, k);
                    Vect vb = volume.get(i2, j, k);

                    volume.set(i, j, k, vb);
                    volume.set(i2, j, k, va);
                }
            }
        }
    }

    public static void flipY(Volume volume)
    {
        int nx = volume.getSampling().numI();
        int ny = volume.getSampling().numJ();
        int nz = volume.getSampling().numK();

        int n = (int) Math.ceil(ny / 2.0);
        for (int i = 0; i < nx; i++)
        {
            for (int j = 0; j < n; j++)
            {
                for (int k = 0; k < nz; k++)
                {
                    int j2 = ny - 1 - j;
                    Vect va = volume.get(i, j, k);
                    Vect vb = volume.get(i, j2, k);

                    volume.set(i, j, k, vb);
                    volume.set(i, j2, k, va);
                }
            }
        }
    }

    public static void flipZ(Volume volume)
    {
        int nx = volume.getSampling().numI();
        int ny = volume.getSampling().numJ();
        int nz = volume.getSampling().numK();

        int n = (int) Math.ceil(nz / 2.0);
        for (int i = 0; i < nx; i++)
        {
            for (int j = 0; j < ny; j++)
            {
                for (int k = 0; k < n; k++)
                {
                    int k2 = nz - 1 - k;
                    Vect va = volume.get(i, j, k);
                    Vect vb = volume.get(i, j, k2);

                    volume.set(i, j, k, vb);
                    volume.set(i, j, k2, va);
                }
            }
        }
    }

    public static Volume sliceX(Volume volume, int idx)
    {
        Sampling samp = volume.getSampling();
        return range(volume, new Sample(idx, 0, 0), new Sample(idx + 1, samp.numJ(), samp.numK()));
    }

    public static Volume sliceY(Volume volume, int idx)
    {
        Sampling samp = volume.getSampling();
        return range(volume, new Sample(0, idx, 0), new Sample(samp.numI(), idx + 1, samp.numK()));
    }

    public static Volume sliceZ(Volume volume, int idx)
    {
        Sampling samp = volume.getSampling();
        return range(volume, new Sample(0, 0, idx), new Sample(samp.numI(), samp.numJ(), idx + 1));
    }

    public static Volume[] splitX(Volume volume)
    {
        int n = volume.getSampling().numI();
        Volume[] out = new Volume[n];
        for (int i = 0; i < n; i++)
        {
            out[i] = sliceX(volume, i);
        }

        return out;
    }

    public static Volume[] splitY(Volume volume)
    {
        int n = volume.getSampling().numJ();
        Volume[] out = new Volume[n];
        for (int i = 0; i < n; i++)
        {
            out[i] = sliceY(volume, i);
        }

        return out;
    }

    public static Volume[] splitZ(Volume volume)
    {
        int n = volume.getSampling().numK();
        Volume[] out = new Volume[n];
        for (int i = 0; i < n; i++)
        {
            out[i] = sliceZ(volume, i);
        }

        return out;
    }

    public static Volume range(Volume volume, String spec)
    {
        Sampling psampling = volume.getSampling();
        Sampling sampling = psampling.range(spec);
        Volume out = new Volume(sampling, volume.getType(), volume.getDim());
        for (Sample sample : sampling)
        {
            Sample psample = psampling.nearest(sampling.world(sample));
            out.set(sample, volume.get(psample));
        }

        return out;
    }

    public static Volume crop(Volume volume, Sampling sampling)
    {
        Sampling psampling = volume.getSampling();
        Volume out = new Volume(sampling, volume.getType(), volume.getDim());
        for (Sample sample : sampling)
        {
            Sample psample = psampling.nearest(sampling.world(sample));
            out.set(sample, volume.get(psample));
        }

        return out;
    }

    public static Volume range(Volume volume, int iStart, int jStart, int kStart, int iEnd, int jEnd, int kEnd)
    {
        return range(volume, new Sample(iStart, jStart, kStart), new Sample(iEnd, jEnd, kEnd));
    }

    public static Volume range(Volume volume, Sample start, Sample end)
    {
        Sampling sampling = volume.getSampling().range(start, end);
        Volume out = new Volume(sampling, volume.getType(), volume.getDim());
        for (Sample sample : sampling)
        {
            int i = sample.getI() + start.getI();
            int j = sample.getJ() + start.getJ();
            int k = sample.getK() + start.getK();

            out.set(sample, volume.get(new Sample(i, j, k)));
        }

        return out;
    }

    public static void set(Volume volume, String spec, Vect value)
    {
        Sampling sampling = volume.getSampling();
        Sampling rsampling = sampling.range(spec);

        for (Sample sample : rsampling)
        {
            Sample rsample = sampling.nearest(rsampling.world(sample));
            volume.set(rsample, value);
        }
    }

    public static Volume subvolumes(Volume volume, int[] idx)
    {
        List<Integer> lidx = Lists.newArrayList();
        for (int i : idx)
        {
            lidx.add(i);
        }
        return subvolumes(volume, lidx);
    }

    public static Volume subvolumes(Volume volume, List<Integer> idx)
    {
        for (Integer i : idx)
        {
            Global.assume(i >= 0 && i < volume.getDim(), "invalid indices");
        }

        Volume out = volume.proto(idx.size());
        for (int i = 0; i < idx.size(); i++)
        {
            copy(volume, idx.get(i), out, i);
        }

        return out;
    }

    public static Volume subvolume(Volume volume, int idx)
    {
        Volume out = volume.proto(1);
        copy(volume, idx, out, 0);
        return out;
    }

    public static void maskInvert(Volume volume, Mask mask)
    {
        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, mask))
            {
                double value = volume.get(sample, 0);
                double nvalue = MathUtils.zero(value) ? 1.0 : 0.0;
                volume.set(sample, 0, nvalue);
            }
        }
    }

    public static Volume zscore(Volume mean, Volume std, Volume input)
    {
        Volume out = input.proto();

        for (Sample sample : input.getSampling())
        {
            double mv = mean.get(sample, 0);
            double sv = std.get(sample, 0);
            double v = input.get(sample, 0);
            double t = (v - mv) / sv;
            out.set(sample, 0, t);
        }

        return out;
    }

    public static Record measure(Volume volume)
    {
        double dx = volume.getSampling().deltaI();
        double dy = volume.getSampling().deltaJ();
        double dz = volume.getSampling().deltaK();
        int nx = volume.getSampling().numI();
        int ny = volume.getSampling().numJ();
        int nz = volume.getSampling().numK();

        Record out = new Record();
        out.with("dim", String.valueOf(volume.getDim()));
        out.with("startx", String.valueOf(volume.getSampling().startI()));
        out.with("starty", String.valueOf(volume.getSampling().startJ()));
        out.with("startz", String.valueOf(volume.getSampling().startK()));
        out.with("deltax", String.valueOf(dx));
        out.with("deltay", String.valueOf(dy));
        out.with("deltaz", String.valueOf(dz));
        out.with("numx", String.valueOf(nx));
        out.with("numy", String.valueOf(ny));
        out.with("numz", String.valueOf(nz));
        out.with("num", String.valueOf(nx * ny * nz));
        out.with("vol", String.valueOf(dx * dy * dz * nx * ny * nz));
        return out;
    }

    public static Record attr(Volume volume, Mask mask)
    {
        Record out = new Record();

        out.with("startx", String.valueOf(volume.getSampling().startI()));
        out.with("starty", String.valueOf(volume.getSampling().startJ()));
        out.with("startz", String.valueOf(volume.getSampling().startK()));
        out.with("deltax", String.valueOf(volume.getSampling().deltaI()));
        out.with("deltay", String.valueOf(volume.getSampling().deltaJ()));
        out.with("deltaz", String.valueOf(volume.getSampling().deltaK()));
        out.with("numx", String.valueOf(volume.getSampling().numI()));
        out.with("numy", String.valueOf(volume.getSampling().numJ()));
        out.with("numz", String.valueOf(volume.getSampling().numK()));
        out.with("dim", String.valueOf(volume.getDim()));

        return out;
    }

    public static double wmean(Volume volume, Volume weights)
    {
        if (volume.getDim() != 1 && weights.getDim() != 1)
        {
            throw new RuntimeException("invalid image data dimensions");
        }

        double out = 0;
        double sumw = 0;
        for (Sample sample : volume.getSampling())
        {
            double w = weights.get(sample).get(0);
            double v = volume.get(sample).get(0);
            out += w * v;
            sumw += w;
        }

        if (MathUtils.nonzero(sumw))
        {
            out /= sumw;
        }
        return out;
    }

    public static Vects values(Volume volume)
    {
        Vects out = new Vects(volume.getSampling().size());
        for (Sample s : volume.getSampling())
        {
            out.add(volume.get(s));
        }
        return out;
    }

    public static Vects values(Volume volume, Mask mask)
    {
        if (!mask.getSampling().equals(volume.getSampling()))
        {
            throw new RuntimeException("invalid sampling");
        }

        Vects out = new Vects(volume.getSampling().size());
        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, mask))
            {
                out.add(volume.get(sample));
            }
        }
        return out;
    }

    public static Volume toSpherePolar(Volume[] vols, int idx)
    {
        // assume idx identifies the desired polar coordinate
        Volume out = vols[0].proto(vols.length);
        VectFunction xfm = VectFunctionSource.sphereCoords();

        for (int i = 0; i < vols.length; i++)
        {
            for (Sample sample : out.getSampling())
            {
                out.set(sample, i, xfm.apply(vols[i].get(sample)).get(idx));
            }
        }

        return out;
    }

    public static Volume toLines(Volume thetas, Volume phis, int idx)
    {
        // assume idx identifies which vector to extract
        Sampling sampling = thetas.getSampling();
        Vect sphere = VectSource.create3D();
        VectFunction xfm = VectFunctionSource.sphereToCart();

        Volume out = thetas.proto(3);
        for (Sample sample : sampling)
        {
            sphere.set(0, 1);
            sphere.set(1, thetas.get(sample, idx));
            sphere.set(2, phis.get(sample, idx));
            out.set(sample, xfm.apply(sphere));
        }

        return out;
    }

    public static void reorient(Volume volume, Matrix[] map)
    {
        if (volume.getDim() != 3)
        {
            throw new RuntimeException("expected a vector-valued volume");
        }

        Sampling sampling = volume.getSampling();
        for (int idx = 0; idx < sampling.size(); idx++)
        {
            Vect input = volume.get(idx);

            // preserve the magnitude
            double mag = input.norm();

            if (map[idx] != null)
            {
                Matrix xfm = map[idx].inv();
                Vect oriented = xfm.times(input).normalize().times(mag);
                volume.set(idx, oriented);
            }
            else
            {
                volume.set(idx, input);
            }
        }
    }

    public static Volume render(Volume volume, Curve curve, Vect value)
    {
        for (Vect vect : curve)
        {
            Sample samp = volume.getSampling().nearest(vect);
            if (volume.getSampling().contains(samp))
            {
                volume.set(samp, value);
            }
        }

        return volume;
    }

    public static Volume render(Volume volume, Curves curves, Vect value)
    {
        for (Curve curve : curves)
        {
            render(volume, curve, value);
        }
        return volume;
    }

    public static Volume render(Volume volume, Mesh mesh, Vect value)
    {
        for (Vect vect : mesh.vattr.get(Mesh.COORD))
        {
            Sample samp = volume.getSampling().nearest(vect);
            if (volume.getSampling().contains(samp))
            {
                volume.set(samp, value);
            }
        }

        return volume;
    }

    public static Volume det(Sampling sampling, Matrix[] jacs)
    {
        Volume out = new Volume(sampling, DataType.DOUBLE, 1);
        for (int idx = 0; idx < sampling.size(); idx++)
        {
            if (jacs[idx] != null)
            {
                out.set(idx, 0, jacs[idx].det());
            }
            else
            {
                out.set(idx, 0, 0);
            }
        }

        return out;
    }

    public static Matrix[] jacobian(Volume xfm, Mask mask)
    {
        Global.assume(xfm.getDim() == 3, "expected 3D vector volume");

        Sampling sampling = xfm.getSampling();
        Matrix[] out = new Matrix[sampling.size()];
        int[] sidx = new int[3];
        double[] factors = new double[3];
        for (int i = 0; i < 3; i++)
        {
            factors[i] = 1.0 / (2.0 * sampling.delta(i));
        }

        for (int idx = 0; idx < sampling.size(); idx++)
        {
            if (sampling.boundary(idx))
            {
                continue;
            }

            if (!xfm.valid(idx, mask))
            {
                continue;
            }

            Sample sample = sampling.sample(idx);
            Matrix J = new Matrix(3, 3);

            for (int i = 0; i < 3; i++)
            {
                for (int j = 0; j < 3; j++)
                {
                    sample.get(sidx);

                    sidx[j] = sample.get(j) - 1;
                    double backward = xfm.get(sidx, i);
                    sidx[j] = sample.get(j) + 1;
                    double forward = xfm.get(sidx, i);

                    double diff = (forward - backward) * factors[j];

                    J.set(i, j, diff);
                }
            }

            out[idx] = J;
        }

        return out;
    }

    public static Matrix[] finitestrain(Volume xfm, Mask mask)
    {
        Global.assume(xfm.getDim() == 3, "expected 3D vector volume");

        Sampling sampling = xfm.getSampling();
        Matrix[] out = new Matrix[sampling.size()];
        int[] sidx = new int[3];
        double[] factors = new double[3];
        for (int i = 0; i < 3; i++)
        {
            factors[i] = 1.0 / (2.0 * sampling.delta(i));
        }

        for (int idx = 0; idx < sampling.size(); idx++)
        {
            if (sampling.boundary(idx))
            {
                continue;
            }

            if (!xfm.valid(idx, mask))
            {
                continue;
            }

            Sample sample = sampling.sample(idx);
            Matrix J = new Matrix(3, 3);

            for (int i = 0; i < 3; i++)
            {
                for (int j = 0; j < 3; j++)
                {
                    sample.get(sidx);

                    sidx[j] = sample.get(j) - 1;
                    double backward = xfm.get(sidx, i);
                    sidx[j] = sample.get(j) + 1;
                    double forward = xfm.get(sidx, i);

                    double diff = (forward - backward) * factors[j];

                    J.set(i, j, diff);
                }
            }

            Matrix JT = J.transpose();
            Matrix JJT = J.times(JT);
            Matrix JJTsqrt = MatrixUtils.sqrt(JJT);
            Matrix JJTsqrtinv = JJTsqrt.inv();
            Matrix rot = JJTsqrtinv.times(J);

            out[idx] = rot;
        }

        return out;
    }

    public static Matrix[] reorient(Volume xfm, Mask mask, ReorientationType method)
    {
        switch (method)
        {
            case FiniteStrain:
                return finitestrain(xfm, mask);
            case Jacobian:
                return jacobian(xfm, mask);
            default:
                throw new RuntimeException("unknown reorientation method: " + method);
        }
    }

    public static Volume color(Sampling sampling, Curves curves, boolean normalize)
    {
        Volume count = VolumeSource.create(sampling);
        Volume out = VolumeSource.create(sampling, 3);
        int max = 0;

        for (Curve curve : curves)
        {
            for (Pair<Sample, Vect> pair : sampling.traverseLine(curve.getAll(Curves.COORD)))
            {
                Sample sample = pair.a;
                Vect line = pair.b.abs();

                if (sampling.contains(sample))
                {
                    int ncount = (int) count.get(sample, 0) + 1;
                    count.set(sample, 0, ncount);
                    out.set(sample, out.get(sample).plus(line));

                    max = Math.max(ncount, max);
                }
            }
        }

        for (Sample sample : sampling)
        {
            int c = (int) count.get(sample, 0);
            if (c > 0)
            {
                Vect line = out.get(sample).normalize();
                if (normalize)
                {
                    line.timesEquals((double) c / (double) max);
                }
                out.set(sample, line);
            }
        }

        return out;
    }

    public static Volume density(Sampling sampling, Curves curves)
    {
        return density(sampling, curves, false);
    }

    public static Volume density(Sampling sampling, Curves curves, boolean normalize)
    {
        Volume volume = VolumeSource.create(sampling);
        int max = 0;

        for (Curve curve : curves)
        {
            for (Sample sample : sampling.traverse(curve.getAll(Curves.COORD)))
            {
                if (sampling.contains(sample))
                {
                    volume.set(sample, 0, volume.get(sample, 0) + 1);

                    int ncount = (int) volume.get(sample, 0) + 1;
                    volume.set(sample, 0, ncount);

                    max = Math.max(ncount, max);
                }
            }
        }

        if (normalize && max > 0)
        {
            for (Sample sample : sampling)
            {
                int c = (int) volume.get(sample, 0);
                if (c > 0)
                {
                    volume.set(sample, volume.get(sample).div(max));
                }
            }
        }

        return volume;
    }

    public static Volume ttest(List<Volume> a, List<Volume> b, Mask mask, String type)
    {
        if (type.equals("paired") && a.size() != b.size())
        {
            throw new RuntimeException("expected equal sized groups");
        }

        List<Volume> all = Lists.newArrayList();
        all.addAll(a);
        all.addAll(b);
        for (Volume v : all)
        {
            if (v.getDim() != 1)
            {
                throw new RuntimeException("expected scalar valued volumes");
            }
        }

        int na = a.size();
        int nb = b.size();
        double[] sample1 = new double[na];
        double[] sample2 = new double[nb];
        Volume out = a.get(0).proto();

        for (Sample sample : out.getSampling())
        {
            if (out.valid(sample, mask))
            {
                out.set(sample, 0, 1.0);
            }
            else
            {
                for (int i = 0; i < na; i++)
                {
                    sample1[i] = a.get(i).get(sample, 0);
                }
                for (int i = 0; i < nb; i++)
                {
                    sample2[i] = b.get(i).get(sample, 0);
                }

                Double pval = null;
                if (type.equals("homoscedastic"))
                {
                    pval = new TTest().homoscedasticTTest(sample1, sample2);
                }
                else if (type.equals("paired"))
                {
                    pval = new TTest().pairedTTest(sample1, sample2);
                }
                else if (type.equals("heteroscedastic"))
                {
                    pval = new TTest().tTest(sample1, sample2);
                }
                else
                {
                    throw new RuntimeException("invalid type");
                }

                out.set(sample, 0, pval);
            }
        }

        return out;
    }

    public static VectFunction interp(String type, Volume volume)
    {
        return interp(InterpolationType.valueOf(type), volume);
    }

    public static VectFunction interp(InterpolationType type, Volume volume)
    {
        switch (type)
        {
            case Nearest:
                return new VolumeInterpNearest(volume);
            case Trilinear:
                return new VolumeInterpTrilinear(volume);
            case Tricubic:
                return new VolumeInterpTricubic(volume);
            case Gaussian:
                return new VolumeInterpGaussian(volume);
            case GaussianLocalLinear:
                return new VolumeInterpGaussianLocalLinear(volume);
            case GaussianLocalQuadratic:
                return new VolumeInterpGaussianLocalQuadratic(volume);
            default:
                throw new RuntimeException("unknown interpolation type: " + type);
        }
    }

    public static Volume vects2fibers(Volume vects)
    {
        Volume out = VolumeSource.create(vects.getSampling(), new Fibers(1).getEncodingSize());
        out.setModel(ModelType.Fibers);

        for (Sample sample : vects.getSampling())
        {
            Vect vect = vects.get(sample);

            double frac = vect.norm();
            Vect line = vect.normalize();

            Fibers fibers = new Fibers(1);
            fibers.setBaseline(frac);
            fibers.setFrac(0, frac);
            fibers.setLine(0, line);

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }

    public static VectFunction xfm(Affine affine, Affine invaffine, Deformation deform, boolean reverse)
    {
        if (affine != null && deform != null)
        {
            if (reverse)
            {
                return affine.compose(deform);
            }
            else
            {
                return deform.compose(affine);
            }
        }
        else if (invaffine != null && deform != null)
        {
            if (reverse)
            {
                return invaffine.inv().compose(deform);
            }
            else
            {
                return deform.compose(invaffine.inv());
            }
        }
        else if (affine != null)
        {
            return affine;
        }
        else if (invaffine != null)
        {
            return invaffine.inv();
        }
        else if (deform != null)
        {
            return deform;
        }
        else
        {
            return VectFunctionSource.identity(3);
        }
    }

    public static VectOnlineStats featureStats(Volume data, Mask mask, Function<Vect, Double> feature)
    {
        VectOnlineStats stats = new VectOnlineStats();

        for (Sample sample : data.getSampling())
        {
            if (data.valid(sample, mask))
            {
                stats.update(feature.apply(data.get(sample)));
            }
        }

        return stats;
    }

    public static Volume apply(Volume input, VectFunction function)
    {
        Volume output = null;
        for (Sample sample : input.getSampling())
        {
            Vect out = function.apply(input.get(sample));
            if (output == null)
            {
                output = input.proto(out.size());
            }
            output.set(sample, out);
        }

        return output;
    }

    public static void apply(Volume input, Mask mask, VectFunction function, Volume output)
    {
        for (Sample sample : input.getSampling())
        {
            if (input.valid(sample, mask))
            {
                output.set(sample, function.apply(input.get(sample)));
            }
        }
    }

    public static void applyEquals(Volume volume, Mask mask, VectFunction function)
    {
        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, mask))
            {
                volume.set(sample, function.apply(volume.get(sample)));
            }
        }
    }

    public static Volume apply(Volume input, Mask mask, VectFunction function)
    {
        Volume output = null;
        for (Sample sample : input.getSampling())
        {
            if (input.valid(sample, mask))
            {
                Vect out = function.apply(input.get(sample));
                if (output == null)
                {
                    output = input.proto(out.size());
                }
                output.set(sample, out);
            }
        }

        return output;
    }

    public static void apply(Volume input, Function<Vect, Vect> function, Volume output)
    {
        for (Sample sample : input.getSampling())
        {
            output.set(sample, function.apply(input.get(sample)));
        }
    }

    public static void apply(Volume input, Mask mask, Function<Vect, Vect> function, Volume output)
    {
        for (Sample sample : input.getSampling())
        {
            if (input.valid(sample, mask))
            {
                output.set(sample, function.apply(input.get(sample)));
            }
        }
    }

    public static Volume apply(Volume input, Function<Vect, Vect> function)
    {
        Volume output = null;
        for (Sample sample : input.getSampling())
        {
            Vect out = function.apply(input.get(sample));
            if (output == null)
            {
                output = input.proto(out.size());
            }
            output.set(sample, out);
        }

        return output;
    }

    public static Volume apply(Volume input, Mask mask, Function<Vect, Vect> function)
    {
        Volume output = null;
        for (Sample sample : input.getSampling())
        {
            if (input.valid(sample, mask))
            {
                Vect out = function.apply(input.get(sample));
                if (output == null)
                {
                    output = input.proto(out.size());
                }
                output.set(sample, out);
            }
        }

        return output;
    }

    public static void applyEquals(Volume volume, VectFunction function)
    {
        for (Sample sample : volume.getSampling())
        {
            volume.set(sample, function.apply(volume.get(sample)));
        }
    }

    public static void applyEquals(Volume Volume, Mask mask, Function<Vect, Vect> function)
    {
        for (Sample sample : Volume.getSampling())
        {
            if (Volume.valid(sample, mask))
            {
                Volume.set(sample, function.apply(Volume.get(sample)));
            }
        }
    }

    public static void applyEquals(Volume Volume, Function<Vect, Vect> function)
    {
        for (Sample sample : Volume.getSampling())
        {
            Volume.set(sample, function.apply(Volume.get(sample)));
        }
    }

    public static Gaussian gaussian(Volume volume)
    {
        Sampling sampling = volume.getSampling();

        Logging.info("computing sum");
        double sum = 0;
        for (Sample sample : volume.getSampling())
        {
            sum += volume.get(sample, 0);
        }

        Logging.info("computing mean");
        Vect mean = VectSource.createND(3);
        for (Sample sample : sampling)
        {
            Vect mypos = sampling.world(sample);
            double myweight = volume.get(sample, 0) / sum;

            mean.plusEquals(myweight, mypos);
        }

        Logging.info("computing covariance");
        Matrix cov = new Matrix(3, 3);
        for (Sample sample : sampling)
        {
            Vect mypos = sampling.world(sample);
            double myweight = volume.get(sample, 0) / sum;

            Vect mydiff = mypos.minus(mean);
            cov.plusEquals(myweight, MatrixSource.dyadic(mydiff));
        }

        return new Gaussian(mean, cov);
    }

    public static Volume autodownsample(Volume volume, int min, int max)
    {
        Volume out = volume;
        Function<Volume, Boolean> down = (v) ->
        {
            int ni = v.getSampling().numI();
            int nj = v.getSampling().numJ();
            int nk = v.getSampling().numK();

            return MathUtils.max(ni, nj, nk) >= max && MathUtils.min(ni, nj, nk) > min;
        };

        while (down.apply(out))
        {
            out = VolumeDownsample.apply(out, 2.0);
        }

        return out;
    }

    public static Volume autodownsample(Volume volume)
    {
        return autodownsample(volume, 31, 65);
    }

    public static double minval(Volume data)
    {
        double min = data.get(0, 0);
        for (Sample sample : data.getSampling())
        {
            min = Math.min(min, data.get(sample, 0));
        }

        return min;
    }

    public static double maxval(Volume data)
    {
        double max = data.get(0, 0);
        for (Sample sample : data.getSampling())
        {
            max = Math.max(max, data.get(sample, 0));
        }

        return max;
    }
}
