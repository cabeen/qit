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

import qit.base.Global;
import qit.base.structs.Pair;
import qit.data.datasets.Affine;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.modules.vects.VectsPCA;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.BinaryVectFunction;
import qit.math.structs.Box;
import qit.math.structs.Containable;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.Collections;
import java.util.List;

/**
 * utilties for processing vectors
 */
public class VectsUtils
{
    public static int nearest(Vects vects, Vect point)
    {
        Global.assume(vects != null, "no points found");

        double minDist2 = vects.get(0).dist2(point);
        int minIdx = 0;
        for (int i = 1; i < vects.size(); i++)
        {
            double dist2 = vects.get(i).dist2(point);
            if (dist2 < minDist2)
            {
                minIdx = i;
                minDist2 = dist2;
            }
        }

        return minIdx;
    }

    public static Vects subsample(Vects vects, Integer limit)
    {
        Vects out = vects.copy();

        if (limit != null && limit < out.size())
        {
            Collections.shuffle(out);
            out = out.subList(0, limit);
        }

        return out;
    }

    public static Vects vects(Curves curves)
    {
        Vects out = new Vects();

        for (Curve curve : curves)
        {
            for (Vect vect : curve)
            {
                out.add(vect);
            }
        }

        return out;
    }

    public static Vects vects(Mesh mesh)
    {
        return vects(mesh, Mesh.COORD);
    }

    public static Vects vects(Mesh mesh, String attr)
    {
        Vects out = new Vects();

        for (Vertex vert : mesh.graph.verts())
        {
            out.add(mesh.vattr.get(vert, attr));
        }

        return out;
    }

    public static Vects vects(Mesh mesh, String attr, String query, int value)
    {
        Vects out = new Vects();

        for (Vertex vert : mesh.graph.verts())
        {
            int qvalue = (int) mesh.vattr.get(vert, query).get(0);
            if (qvalue == value)
            {
                out.add(mesh.vattr.get(vert, attr));
            }
        }

        return out;
    }

    public static Vects vects(Mesh mesh, String attr, String query)
    {
        Vects out = new Vects();

        for (Vertex vert : mesh.graph.verts())
        {
            double qvalue = mesh.vattr.get(vert, query).get(0);
            if (MathUtils.nonzero(qvalue))
            {
                out.add(mesh.vattr.get(vert, attr));
            }
        }

        return out;
    }

    public static Vects crop(Vects input, Containable select)
    {
        Vects output = new Vects();

        for (Vect vect : input)
        {
            if (!select.contains(vect))
            {
                output.add(vect);
            }
        }

        return output;
    }

    public static Vects select(Vects input, Containable select)
    {
        Vects output = new Vects();

        for (Vect vect : input)
        {
            if (select.contains(vect))
            {
                output.add(vect);
            }
        }

        return output;
    }

    public static Vects tfHist(Vects vects)
    {
        Vects out = new Vects();
        for (Vect vect : vects)
        {
            double max = vect.max();
            Vect nvect = vect.copy();
            if (max > 0)
            {
                nvect.timesEquals(1.0 / max);
            }
            out.add(nvect);
        }

        return out;
    }

    public static Vects boolHist(Vects vects)
    {
        Vects out = new Vects();
        for (Vect vect : vects)
        {
            Vect nvect = vect.copy();
            for (int i = 0; i < nvect.size(); i++)
            {
                if (!MathUtils.zero(nvect.get(i)))
                {
                    nvect.set(i, 1);
                }
                else
                {
                    nvect.set(i, 0);
                }
            }
            out.add(nvect);
        }

        return out;
    }

    public static Vects logHist(Vects vects)
    {
        Vects out = new Vects();
        for (Vect vect : vects)
        {
            Vect nvect = vect.copy();
            for (int i = 0; i < nvect.size(); i++)
            {
                if (nvect.get(i) > 0)
                {
                    nvect.set(i, 1 + Math.log(nvect.get(i)));
                }
            }
            out.add(nvect);
        }

        return out;
    }

    public static Vects tfidfHist(Vects vects)
    {
        Vects out = new Vects();

        Vect idf = new Vect(vects.getDim());
        for (int i = 0; i < idf.size(); i++)
        {
            int count = 0;
            for (Vect vect : vects)
            {
                if (!MathUtils.zero(vect.get(i)))
                {
                    count += 1;
                }
            }

            double idfv = Math.log(vects.size() / (1.0 + count));
            idf.set(i, idfv);
        }

        for (Vect vect : vects)
        {
            double max = vect.max();
            Vect nvect = vect.proto();
            for (int i = 0; i < vect.size(); i++)
            {
                if (vect.get(i) > 0)
                {
                    nvect.set(i, vect.get(i) * idf.get(i) / max);
                }
                else
                {
                    nvect.set(i, 0);
                }
            }
            out.add(nvect);
        }

        return out;
    }

    public static Vects select(Vects v, int[] labels, int label)
    {
        Vects out = new Vects();
        for (int i = 0; i < labels.length; i++)
        {
            if (labels[i] == label)
            {
                out.add(v.get(i));
            }
        }
        return out;
    }

    public static Vects sub(Vects vects, int[] which)
    {
        Vects out = new Vects();
        for (int i : which)
        {
            out.add(vects.get(i));
        }
        return out;
    }

    public static Vects sub(Vects vects, List<Integer> which)
    {
        Vects out = new Vects();
        for (Integer i : which)
        {
            out.add(vects.get(i));
        }
        return out;
    }

    public static Vects index(Vects vects, int[] idx)
    {
        Vects out = new Vects(vects.size());
        int dim = idx.length;

        for (Vect v : vects)
        {
            Vect nv = new Vect(dim);
            for (int i = 0; i < idx.length; i++)
            {
                nv.set(i, v.get(idx[i]));
            }
            out.add(nv);
        }

        return out;
    }

    public static Vects index(Vects vects, int idx)
    {
        Vects out = new Vects(vects.size());

        for (Vect v : vects)
        {
            out.add(VectSource.create1D(v.get(idx)));
        }

        return out;
    }

    public static Vects transpose(Vects vects)
    {
        int m = vects.size();
        int n = vects.get(0).size();

        Vects out = new Vects(n);
        for (int i = 0; i < n; i++)
        {
            Vect v = new Vect(m);
            for (int j = 0; j < m; j++)
            {
                v.set(j, vects.get(j).get(i));
            }

            out.add(v);
        }

        return out;
    }

    public static Vects apply(Vects vects, VectFunction xfm)
    {
        Vects out = new Vects();
        for (Vect vect : vects)
        {
            out.add(xfm.apply(vect));
        }
        return out;
    }

    public static Vects apply(Vects vects, BinaryVectFunction func, Vects right)
    {
        if (right.size() != vects.size())
        {
            throw new RuntimeException("sizes do not match");
        }

        Vects out = new Vects();
        for (int i = 0; i < right.size(); i++)
        {
            out.add(func.apply(vects.get(i), right.get(i)));
        }
        ;
        return out;
    }

    public static void packFrontTo(Vects vects, int size, Vect value)
    {
        while (vects.size() < size)
        {
            vects.add(0, new Vect(vects.get(0).size()));
        }
    }

    public static Vect pack(Vects vects)
    {
        int num = vects.size();
        int dim = vects.getDim();

        Vect value = VectSource.createND(num * dim);
        for (int i = 0; i < num; i++)
        {
            value.set(i * dim, vects.get(i));
        }

        return value;
    }

    public static VectFunction alignment(Vects vects)
    {
        int dim = vects.getDim();
        if (dim != 3)
        {
            throw new RuntimeException("invalid dimensions");
        }

        VectsPCA pca = new VectsPCA();
        pca.input = vects;
        pca.run();
        Matrix R = MatrixSource.identity(4);
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                R.set(i, j, pca.comps.get(j).get(i));
            }
        }

        return new Affine(R);
    }

    public static Box bounds(Vects vects)
    {
        return Box.create(vects);
    }

    public static double[] dists(Vects from, Vect to)
    {
        double[] out = new double[from.size()];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = from.get(i).dist(to);
        }
        return out;
    }

    public static double[][] dists(Vects vects)
    {
        int num = vects.size();
        double[][] dists = new double[num][num];
        for (int i = 0; i < num; i++)
        {
            for (int j = i + 1; j < num; j++)
            {
                double dist = vects.get(i).dist(vects.get(j));
                dists[i][j] = dist;
                dists[j][i] = dist;
            }
        }

        return dists;
    }

    public static VectOnlineStats distStats(Vects vects)
    {
        VectOnlineStats stats = new VectOnlineStats();

        int num = vects.size();
        for (int i = 0; i < num; i++)
        {
            for (int j = i + 1; j < num; j++)
            {
                stats.update(vects.get(i).dist(vects.get(j)));
            }
        }

        return stats;
    }

    public static VectOnlineStats normStats(Vects vects)
    {
        VectOnlineStats stat = new VectOnlineStats();
        for (Vect vect : vects)
        {
            stat.update(vect.norm());
        }

        return stat;
    }

    public static double scale(Vects vects)
    {
        double sum = 0;
        for (Vect v : vects)
        {
            sum += v.norm2();
        }

        double s = Math.sqrt(sum);
        return s;
    }

    public static Vect mean(Vects vects)
    {
        Vect sum = null;
        int count = 0;
        for (Vect vect : vects)
        {
            if (sum == null)
            {
                sum = vect.copy();
            }
            else
            {
                sum.plusEquals(vect);
            }

            count += 1;
        }

        return sum.times(1.0 / count);
    }

    public static Vect mean(Vects vects, double[] weights)
    {
        return VectsUtils.mean(weights, vects);
    }

    public static Matrix cov(Vects vects, double[] weights)
    {
        return cov(vects, mean(vects), weights);
    }

    public static Matrix cov(Vects vects, Vect mean, double[] weights)
    {
        int num = vects.size();
        int dim = vects.getDim();
        Matrix cov = new Matrix(dim, dim);

        for (int i = 0; i < num; i++)
        {
            double w = weights[i];
            Vect dv = vects.get(i).minus(mean);
            for (int j = 0; j < dim; j++)
            {
                for (int k = j; k < dim; k++)
                {
                    double p = w * dv.get(j) * dv.get(k);
                    cov.set(j, k, cov.get(j, k) + p);
                    cov.set(k, j, cov.get(k, j) + p);
                }
            }
        }

        return cov;
    }

    public static Matrix cov(Vects vects)
    {
        return cov(vects, mean(vects));
    }

    public static Matrix cov(Vects vects, Vect mean)
    {
        int num = vects.size();
        int dim = vects.getDim();
        Matrix cov = new Matrix(dim, dim);
        double w = 1.0 / num;
        for (int i = 0; i < num; i++)
        {
            Vect d = vects.get(i).minus(mean);
            Matrix dtd = MatrixSource.dyadic(d);
            cov.plusEquals(w, dtd);
        }

        return cov;
    }

    public static Matrix matrix(Vects vects)
    {
        int num = vects.size();
        int dim = vects.getDim();

        Matrix out = new Matrix(num, dim);
        for (int i = 0; i < num; i++)
        {
            for (int j = 0; j < dim; j++)
            {
                out.set(i, j, vects.get(i).get(j));
            }
        }

        return out;
    }

    public static Vect mean(Iterable<Vect> vects)
    {
        Vect sum = null;
        int count = 0;
        for (Vect vect : vects)
        {
            if (sum == null)
            {
                sum = vect.copy();
            }
            else
            {
                sum.plusEquals(vect);
            }

            count += 1;
        }

        return sum.times(1.0 / count);
    }

    public static double scale(Iterable<Vect> vects)
    {
        double sum = 0;

        for (Vect v : vects)
        {
            sum += v.norm2();
        }

        double s = Math.sqrt(sum);

        return s;
    }

    public static double frobnorm(Iterable<Vect> vects)
    {
        double out = 0;

        for (Vect vect : vects)
        {
            for (int j = 0; j < vect.size(); j++)
            {
                double v = vect.get(j);
                out += v * v;
            }
        }

        return Math.sqrt(out);

    }

    public static int[] label(Vects values, Vects clusters, Double thresh)
    {
        int[] out = new int[values.size()];
        double[] dists = new double[clusters.size()];

        for (int i = 0; i < values.size(); i++)
        {
            Vect value = values.get(i);
            for (int j = 0; j < clusters.size(); j++)
            {
                dists[j] = value.dist(clusters.get(j));
            }

            int minidx = MathUtils.minidx(dists);
            double minval = dists[minidx];

            if (thresh == null || minval > thresh)
            {
                out[i] = minidx;
            }
            else
            {
                out[i] = -1;
            }
        }

        return out;
    }


    public static Vects seed(Mask mask, Integer count)
    {
        return seed(mask, count, null);
    }

    public static Vects seed(Mask mask, Integer count, Integer limit)
    {
        Vects out = new Vects();
        Sampling sampling = mask.getSampling();

        for (Sample sample : sampling)
        {
            if (mask.foreground(sample))
            {
                if (count != null)
                {
                    // jittered seeds
                    for (int c = 0; c < count; c++)
                    {
                        out.add(sampling.random(sample));
                    }
                }
                else
                {
                    // deterministic seeds
                    out.add(sampling.world(sample));
                }
            }
        }

        if (limit != null && limit != 0 && limit < out.size())
        {
            Collections.shuffle(out);
            out = out.subList(0, limit);
        }

        return out;
    }

    public static double norm(Vects vects, Vects vs)
    {
        if (vects.size() != vs.size())
        {
            throw new RuntimeException("Dimensions do not match");
        }

        double out = 0;
        for (int i = 0; i < vects.size(); i++)
        {
            out += vects.get(i).dot(vs.get(i));
        }

        return out;
    }

    public static Vects copy(Vects vects, int[] labels, int label)
    {
        if (labels.length != vects.size())
        {
            throw new RuntimeException("invalid labels");
        }

        Vects out = new Vects();
        for (int i = 0; i < labels.length; i++)
        {
            if (labels[i] == label)
            {
                out.add(vects.get(i).copy());
            }
        }

        return out;
    }

    public static Vect mean(double[] weights, Iterable<Vect> vects)
    {
        double sw = 0;
        Vect mean = null;
        int idx = 0;
        for (Vect v : vects)
        {
            if (mean == null)
            {
                mean = v.proto();
            }

            double w = weights[idx++];
            mean.plusEquals(w, v);
            sw += w;
        }

        if (MathUtils.zero(sw))
        {
            throw new RuntimeException("zero weights");
        }

        mean.timesEquals(1.0 / sw);

        return mean;
    }

    public static double scale(double[] weights, Iterable<Vect> vects)
    {
        double sw = 0;
        double sum = 0;

        int idx = 0;
        for (Vect v : vects)
        {
            double w = weights[idx++];
            sum += w * v.norm2();
            sw += w;
        }

        if (MathUtils.zero(sw))
        {
            throw new RuntimeException("zero weights");
        }

        sum /= sw;
        sum *= idx;

        double s = Math.sqrt(sum);

        return s;
    }

    public static Vect project(Vect sample, Vects comps)
    {
        int num = comps.size();

        Vect vect = new Vect(num);
        for (int j = 0; j < num; j++)
        {
            vect.set(j, sample.dot(comps.get(j)));
        }

        return vect;
    }

    public static Vects project(Vects vects, Vects comps)
    {
        Vects out = new Vects(vects.size());
        for (Vect sample : vects)
        {
            out.add(project(sample, comps));
        }

        return out;
    }

    public static Vects means(List<Vects> vects)
    {
        int m = vects.size();
        int n = vects.get(0).size();
        int d = vects.get(0).get(0).size();

        Vects out = new Vects(vects.get(0).size());
        for (int i = 0; i < n; i++)
        {
            VectsOnlineStats vs = new VectsOnlineStats(d);
            for (int j = 0; j < m; j++)
            {
                vs.update(vects.get(j).get(i));
            }
            out.add(vs.mean);
        }

        return out;
    }

    public static Affine affine(Vects a, Vects b)
    {
        Global.assume(a.size() == b.size(), "sizes must match");
        Global.assume(a.getDim() == b.getDim(), "dims must match");

        int dim = a.getDim();
        int num = a.size();
        int sizeJ = dim * (dim + 1);
        int sizeI = dim * num;

        Matrix lhs = new Matrix(sizeI, sizeJ);
        Matrix rhs = new Matrix(sizeI, 1);

        for (int i = 0; i < num; i++)
        {
            Vect input = a.get(i);
            Vect output = b.get(i);

            for (int j = 0; j < dim; j++)
            {
                for (int k = 0; k < dim; k++)
                {
                    lhs.set(i * dim + j, j * dim + k, input.get(k));
                }
                lhs.set(i * dim + j, dim * dim + j, 1);
                rhs.set(i * dim + j, 0, output.get(j));
            }

            i++;
        }

        Matrix solution = MatrixUtils.solve(lhs, rhs);

        Matrix xfm = new Matrix(dim + 1, dim + 1);
        xfm.set(0, 0, solution.get(0, 0));
        xfm.set(0, 1, solution.get(1, 0));
        xfm.set(0, 2, solution.get(2, 0));
        xfm.set(1, 0, solution.get(3, 0));
        xfm.set(1, 1, solution.get(4, 0));
        xfm.set(1, 2, solution.get(5, 0));
        xfm.set(2, 0, solution.get(6, 0));
        xfm.set(2, 1, solution.get(7, 0));
        xfm.set(2, 2, solution.get(8, 0));
        xfm.set(0, 3, solution.get(9, 0));
        xfm.set(1, 3, solution.get(10, 0));
        xfm.set(2, 3, solution.get(11, 0));
        xfm.set(3, 3, 1);

        return new Affine(xfm);
    }

    public static Affine rotation(double[] weights, List<Pair<Vect, Vect>> ps)
    {
        int n = weights.length;
        Matrix ref = new Matrix(n, 3);
        Matrix tar = new Matrix(n, 3);

        int idx = 0;
        for (Pair<Vect, Vect> p : ps)
        {
            double w = weights[idx];
            Vect refv = p.a;
            Vect tarv = p.b;

            for (int j = 0; j < 3; j++)
            {
                ref.set(idx, j, w * refv.get(j));
                tar.set(idx, j, tarv.get(j));
            }

            idx++;
        }

        MatrixUtils.SvdDecomp svd = MatrixUtils.svd(tar.transpose().times(ref));
        Matrix R = svd.U.times(svd.V.transpose());

        Matrix mat = MatrixSource.identity(4);
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                mat.set(i, j, R.get(i, j));
            }
        }

        return new Affine(mat);
    }

    public static Affine rotation(List<Pair<Vect, Vect>> ps)
    {
        int n = ps.size();
        Matrix ref = new Matrix(n, 3);
        Matrix tar = new Matrix(n, 3);

        int i = 0;
        for (Pair<Vect, Vect> p : ps)
        {
            Vect refv = p.a;
            Vect tarv = p.b;

            for (int j = 0; j < 3; j++)
            {
                ref.set(i, j, refv.get(j));
                tar.set(i, j, tarv.get(j));
            }

            i++;
        }

        MatrixUtils.SvdDecomp svd = MatrixUtils.svd(tar.transpose().times(ref));
        Matrix R = svd.U.times(svd.V.transpose());

        Matrix mat = MatrixSource.identity(4);
        for (i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                mat.set(i, j, R.get(i, j));
            }
        }

        return new Affine(mat);
    }

    public static int[] permutation(Vects values)
    {
        return permutation(values, 0);
    }

    public static int[] permutation(Vects values, int idx)
    {
        double[] array = new double[values.size()];
        for (int i = 0; i < array.length; i++)
        {
            array[i] = values.get(i).get(idx);
        }

        return MathUtils.permutation(array);
    }

    public static Matrix dist2(Vects vects)
    {
        int n = vects.size();

        // compute a distance matrix (yes this is worst case, I know)
        Matrix dists = new Matrix(n, n);
        for (int i = 0; i < n; i++)
        {
            Vect vi = vects.get(i);

            for (int j = i + 1; j < n; j++)
            {
                Vect vj = vects.get(j);

                double dij = vi.dist2(vj);

                dists.set(i, j, dij);
                dists.set(j, i, dij);
            }
        }

        return dists;
    }

    public static Matrix dist(Vects vects)
    {
        int n = vects.size();

        // compute a distance matrix (yes this is worst case, I know)
        Matrix dists = new Matrix(n, n);
        for (int i = 0; i < n; i++)
        {
            Vect vi = vects.get(i);

            for (int j = i + 1; j < n; j++)
            {
                Vect vj = vects.get(j);

                double dij = vi.dist(vj);

                dists.set(i, j, dij);
                dists.set(j, i, dij);
            }
        }

        return dists;
    }

    public static Matrix expDist(Vects vects, double s2)
    {
        int n = vects.size();

        // compute a distance matrix (yes this is worst case, I know)
        Matrix out = new Matrix(n, n);
        for (int i = 0; i < n; i++)
        {
            Vect vi = vects.get(i);

            for (int j = i + 1; j < n; j++)
            {
                Vect vj = vects.get(j);

                double dij = vi.dist2(vj);
                double eij = Math.exp(-dij * dij / s2);

                out.set(i, j, eij);
                out.set(j, i, eij);
            }
        }

        return out;
    }

    public static Vects normalizeDist(Vects vects)
    {
        Vect mean = vects.mean();
        Vects out = vects.copy();

        double sum2 = 0;
        for (int k = 0; k < vects.size(); k++)
        {
            out.get(k).minusEquals(mean);
            sum2 += out.get(k).norm2();
        }

        double norm2 = Math.sqrt(sum2);

        for (int k = 0; k < vects.size(); k++)
        {
            out.get(k).divSafeEquals(norm2);
        }

        return out;
    }

    public static Vects normalize(Vects vects)
    {
        Vects out = new Vects();
        for (Vect v : vects)
        {
            out.add(v.normalize());
        }
        return out;
    }

    public static Matrix normdist(Vects vects, double sig2)
    {
        Vects normal = VectsUtils.normalizeDist(vects);
        Matrix dists = VectsUtils.expDist(normal, sig2);

        return dists;
    }
}