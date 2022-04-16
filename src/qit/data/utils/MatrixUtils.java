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

import Jama.EigenvalueDecomposition;
import Jama.SingularValueDecomposition;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.structs.Integers;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** utilties for processing matrices */
public class MatrixUtils
{
    public static double dunn(Matrix dists, int[] labels)
    {
        Global.assume(dists.isSquare(), "matrix must be square");
        Global.assume(dists.rows() == labels.length, "matrix and labels must match");

        Set<Integer> keys = MathUtils.counts(labels).keySet();
        Map<Integer, Integer> idx = Maps.newHashMap();
        for (Integer key : keys)
        {
            idx.put(key, idx.size());
        }

        int num = keys.size();
        VectOnlineStats[][] stats = new VectOnlineStats[num][num];
        for (int i = 0; i < num; i++)
        {
            for (int j = i; j < num; j++)
            {
                stats[i][j] = new VectOnlineStats();
            }
        }

        for (int i = 0; i < labels.length; i++)
        {
            for (int j = 0; j < labels.length; j++)
            {
                int li = labels[i];
                int lj = labels[j];
                double dist = dists.get(i, j);

                int mli = idx.get(li);
                int mlj = idx.get(lj);

                stats[Math.min(mli, mlj)][Math.max(mli, mlj)].update(dist);
            }
        }

        double denom = 0;
        for (int i = 0; i < num; i++)
        {
            denom = Math.max(denom, stats[i][i].max);
        }

        double score = Double.MAX_VALUE;
        for (int i = 0; i < num; i++)
        {
            for (int j = i + 1; j < num; j++)
            {
                score = Math.min(score, stats[i][j].min / denom);
            }
        }

        return score;
    }

    public static Vect silhouette(Matrix dists, int[] labels)
    {
        Global.assume(dists.isSquare(), "matrix must be square");
        Global.assume(dists.rows() == labels.length, "matrix and labels must match");

        Vect out = VectSource.createND(labels.length);
        for (int i = 0; i < labels.length; i++)
        {
            int label = labels[i];

            Map<Integer, VectOnlineStats> stats = Maps.newConcurrentMap();

            for (int j = 0; j < labels.length; j++)
            {
                int other = labels[j];
                double dist = dists.get(i, j);

                if (!stats.containsKey(other))
                {
                    stats.put(other, new VectOnlineStats());
                }

                stats.get(other).update(dist);
            }

            double a = stats.get(label).mean;
            double b = Double.MAX_VALUE;
            double minDist = Double.MAX_VALUE;

            for (int other : stats.keySet())
            {
                VectOnlineStats stat = stats.get(other);
                if (stat.min < minDist && other != label)
                {
                    minDist = stat.mean;
                    b = stat.mean;
                }
            }

            double score = (b - a) / Math.max(a, b);
            out.set(i, score);
        }

        return out;
    }

    public static Matrix distToSim(Matrix dist)
    {
        Global.assume(dist.rows() == dist.cols(), "matrix must be square");

        // compute a scaling factor from variance of distances
        int dim = dist.rows();
        VectOnlineStats stats = new VectOnlineStats();
        for (int i = 0; i < dim; i++)
        {
            for (int j = 0; j < dim; j++)
            {
                stats.update(dist.get(i, j));
            }
        }

        double var = stats.var;
        double gamma = MathUtils.nonzero(var) ? 1.0 / (2.0 * var) : 1.0;

        return distToSim(dist, gamma);
    }

    public static Matrix distToSim(Matrix dist, double gamma)
    {
        Global.assume(dist.rows() == dist.cols(), "matrix must be square");

        // convert distances to similarity with a gaussian triangle
        int dim = dist.rows();
        Matrix out = dist.proto();

        for (int i = 0; i < dim; i++)
        {
            for (int j = 0; j < dim; j++)
            {
                double d = dist.get(i, j);
                double s = Math.exp(-gamma * d * d);
                out.set(i, j, s);
            }
        }

        return out;
    }

    public static Matrix plus(Matrix x1, double s)
    {
        Matrix y = new Matrix(x1.rows(), x1.cols());
        for (int i = 0; i < x1.rows(); i++)
        {
            for (int j = 0; j < x1.cols(); j++)
            {
                y.set(i, j, x1.get(i, j) + s);
            }
        }
        return y;
    }

    public static Matrix eleminv(Matrix m)
    {
        Matrix y = new Matrix(m.rows(), m.cols());
        for (int i = 0; i < m.rows(); i++)
        {
            for (int j = 0; j < m.cols(); j++)
            {
                y.set(i, j, 1 / m.get(i, j));
            }
        }
        return y;
    }

    public static Matrix elemtimes(Matrix a, Matrix b)
    {
        Matrix y = new Matrix(a.rows(), a.cols());
        for (int i = 0; i < a.rows(); i++)
        {
            for (int j = 0; j < a.cols(); j++)
            {
                y.set(i, j, a.get(i, j) * b.get(i, j));
            }
        }
        return y;
    }

    public static double colSumNorm(Matrix m)
    {
        double norm = Double.NEGATIVE_INFINITY;
        for (int j = 0; j < m.cols(); j++)
        {
            double colNorm = 0;
            for (int i = 0; i < m.rows(); i++)
            {
                colNorm += Math.abs(m.get(i, j));
            }
            if (colNorm > norm)
            {
                norm = colNorm;
            }
        }
        return norm;
    }

    public static Matrix catrows(Matrix a, Matrix b)
    {
        int r = a.rows();
        Matrix y = new Matrix(a.rows() + b.rows(), a.cols());
        for (int i = 0; i < a.rows(); i++)
        {
            for (int j = 0; j < a.cols(); j++)
            {
                y.set(i, j, a.get(i, j));
                y.set(r + i, j, b.get(i, j));
            }
        }
        return y;
    }

    public static Matrix catcols(Matrix a, Matrix b)
    {
        int c = a.cols();
        Matrix y = new Matrix(a.rows(), a.cols() + b.cols());
        for (int i = 0; i < a.rows(); i++)
        {
            for (int j = 0; j < a.cols(); j++)
            {
                y.set(i, j, a.get(i, j));
                y.set(i, c + j, b.get(i, j));
            }
        }
        return y;
    }

    public static Matrix log(Matrix m)
    {
        Matrix out = m.proto();
        for (int i = 0; i < m.rows(); i++)
        {
            for (int j = 0; j < m.cols(); j++)
            {
                double v = m.get(i, j);
                out.set(i, j, Math.log(v));
            }
        }

        return out;
    }

    public static double min(Matrix m)
    {
        double min = m.get(0, 0);
        for (int i = 0; i < m.rows(); i++)
        {
            for (int j = 0; j < m.cols(); j++)
            {
                double v = m.get(i, j);
                if (v < min)
                {
                    min = v;
                }
            }
        }

        return min;
    }

    public static int[] maxidxrows(Matrix m)
    {
        int[] out = new int[m.rows()];
        for (int i = 0; i < m.rows(); i++)
        {
            Double maxval = null;
            Integer maxidx = null;
            for (int j = 0; j < m.cols(); j++)
            {
                double val = m.get(i, j);
                if (maxval == null || val > maxval)
                {
                    maxval = val;
                    maxidx = j;
                }
            }
            out[i] = maxidx;
        }

        return out;
    }

    public static int[] maxidxcols(Matrix m)
    {
        int[] out = new int[m.cols()];
        for (int j = 0; j < m.cols(); j++)
        {
            Double maxval = null;
            Integer maxidx = null;
            for (int i = 0; i < m.rows(); i++)
            {
                double val = m.get(i, j);
                if (maxval == null || val > maxval)
                {
                    maxval = val;
                    maxidx = i;
                }
            }
            out[j] = maxidx;
        }

        return out;
    }

    public static double max(Matrix m)
    {
        double max = m.get(0, 0);
        for (int i = 0; i < m.rows(); i++)
        {
            for (int j = 0; j < m.cols(); j++)
            {
                double v = m.get(i, j);
                if (v > max)
                {
                    max = v;
                }
            }
        }

        return max;
    }

    public static Vect maxRows(Matrix m)
    {
        Vect out = VectSource.createND(m.rows());
        for (int i = 0; i < m.rows(); i++)
        {
            double v = m.getRow(i).max();
            out.set(i, v);
        }

        return out;
    }

    public static Vect maxCols(Matrix m)
    {
        Vect out = VectSource.createND(m.cols());
        for (int j = 0; j < m.cols(); j++)
        {
            double v = m.getColumn(j).max();
            out.set(j, v);
        }

        return out;
    }

    public static Vect minRows(Matrix m)
    {
        Vect out = VectSource.createND(m.rows());
        for (int i = 0; i < m.rows(); i++)
        {
            double v = m.getRow(i).min();
            out.set(i, v);
        }

        return out;
    }

    public static Vect minCols(Matrix m)
    {
        Vect out = VectSource.createND(m.cols());
        for (int j = 0; j < m.cols(); j++)
        {
            double v = m.getColumn(j).min();
            out.set(j, v);
        }

        return out;
    }

    public static double dot(Matrix a, Matrix b)
    {
        double dot = 0;
        for (int i = 0; i < a.rows(); i++)
        {
            for (int j = 0; j < a.cols(); j++)
            {
                dot += a.get(i, j) * b.get(i, j);
            }
        }

        return dot;
    }

    public static double sum(Matrix m)
    {
        double sum = 0;
        for (int i = 0; i < m.rows(); i++)
        {
            for (int j = 0; j < m.cols(); j++)
            {
                sum += m.get(i, j);
            }
        }

        return sum;
    }

    public static void symmetrizeEquals(Matrix mat)
    {
        if (mat.rows() != mat.cols())
        {
            throw new RuntimeException("matrix must be square");
        }

        for (int i = 0; i < mat.rows(); i++)
        {
            for (int j = i + 1; j < mat.cols(); j++)
            {
                double left = mat.get(i, j);
                double right = mat.get(j, i);
                double mean = 0.5 * (left + right);
                mat.set(i, j, mean);
                mat.set(j, i, mean);
            }
        }
    }

    public static Matrix sqrt(Matrix mat)
    {
        if (mat.rows() != mat.cols())
        {
            throw new RuntimeException("matrix must be square");
        }

        int dim = mat.rows();
        EigenvalueDecomposition eig = new Jama.Matrix(mat.toArray()).eig();

        Matrix D = new Matrix(eig.getD());
        Matrix V = new Matrix(eig.getV());

        for (int i = 0; i < dim; i++)
        {
            double v = D.get(i, i);
            if (v > 0)
            {
                D.set(i, i, Math.sqrt(v));
            }
        }

        return V.times(D).times(V.transpose());
    }

    public static Matrix orthogonalize(Matrix mat)
    {
        // http://www.cs.yale.edu/homes/el327/datamining2012aFiles/06_singular_value_decomposition.pdf
        Global.assume(mat.rows() == mat.cols(), "matrix must be square");
        SvdDecomp svd = svd(mat);
        return svd.U.times(svd.V.transpose());
    }

    public static Matrix symmeterizeMean(Matrix mat)
    {
        Matrix out = mat.copy();
        for (int i = 0; i < mat.rows(); i++)
        {
            for (int j = i + 1; j < mat.cols(); j++)
            {
                double a = mat.get(i, j);
                double b = mat.get(j, i);
                double mean = (a + b) / 2.0;
                out.set(i, j, mean);
                out.set(j, i, mean);
            }
        }

        return out;
    }

    public static Matrix symmeterizeMax(Matrix mat)
    {
        Matrix out = mat.copy();
        for (int i = 0; i < mat.rows(); i++)
        {
            for (int j = i + 1; j < mat.cols(); j++)
            {
                double a = mat.get(i, j);
                double b = mat.get(j, i);
                double max = Math.max(a, b);
                out.set(i, j, max);
                out.set(j, i, max);
            }
        }

        return out;
    }

    public static Matrix symmeterizeMin(Matrix mat)
    {
        Matrix out = mat.copy();
        for (int i = 0; i < mat.rows(); i++)
        {
            for (int j = i + 1; j < mat.cols(); j++)
            {
                double a = mat.get(i, j);
                double b = mat.get(j, i);
                double min = Math.min(a, b);
                out.set(i, j, min);
                out.set(j, i, min);
            }
        }

        return out;
    }

    public static Matrix square(Matrix m)
    {
        Matrix out = m.proto();
        for (int i = 0; i < m.rows(); i++)
        {
            for (int j = 0; j < m.cols(); j++)
            {
                double v = m.get(i, j);
                out.set(i, j, v * v);
            }
        }

        return out;
    }

    public static Matrix exp(Matrix m)
    {
        Matrix out = m.proto();
        for (int i = 0; i < m.rows(); i++)
        {
            for (int j = 0; j < m.cols(); j++)
            {
                out.set(i, j, Math.exp(m.get(i, j)));
            }
        }

        return out;
    }

    public static Matrix threshold(Matrix m, double thresh)
    {
        Matrix out = m.proto();
        for (int i = 0; i < m.rows(); i++)
        {
            for (int j = 0; j < m.cols(); j++)
            {
                double v = m.get(i, j) > thresh ? 1.0 : 0.0;
                out.set(i, j, v);
            }
        }

        return out;
    }

    public static VectOnlineStats stats(Matrix mat)
    {
        VectOnlineStats stats = new VectOnlineStats();
        for (int i = 0; i < mat.rows(); i++)
        {
            for (int j = 0; j < mat.cols(); j++)
            {
                stats.update(mat.get(i, j));
            }
        }

        return stats;
    }

    public static VectOnlineStats upperTriStats(Matrix mat)
    {
        if (!mat.isSquare())
        {
            throw new RuntimeException("matrix is not square");
        }

        VectOnlineStats stats = new VectOnlineStats();
        for (int i = 0; i < mat.rows(); i++)
        {
            for (int j = i + 1; j < mat.cols(); j++)
            {
                stats.update(mat.get(i, j));
            }
        }

        return stats;
    }

    public static VectOnlineStats lowerTriStats(Matrix mat)
    {
        if (!mat.isSquare())
        {
            throw new RuntimeException("matrix is not square");
        }

        VectOnlineStats stats = new VectOnlineStats();
        for (int i = 0; i < mat.rows(); i++)
        {
            for (int j = 0; j < i; j++)
            {
                stats.update(mat.get(i, j));
            }
        }

        return stats;
    }

    public static Vect colsum(Matrix mat)
    {
        Vect out = VectSource.createND(mat.cols());
        for (int i = 0; i < mat.cols(); i++)
        {
            out.set(i, mat.getColumn(i).sum());
        }
        return out;
    }

    public static Vect rowsum(Matrix mat)
    {
        Vect out = VectSource.createND(mat.rows());
        for (int i = 0; i < mat.rows(); i++)
        {
            Vect row = mat.getRow(i);
            double sum = row.sum();
            out.set(i, sum);
        }
        return out;
    }

    public static VectsOnlineStats rowStats(Matrix mat)
    {
        VectsOnlineStats stats = new VectsOnlineStats(mat.cols());
        for (int i = 0; i < mat.rows(); i++)
        {
            stats.update(mat.getRow(i));
        }

        return stats;
    }

    public static VectsOnlineStats columnStats(Matrix mat)
    {
        VectsOnlineStats stats = new VectsOnlineStats(mat.rows());
        for (int i = 0; i < mat.cols(); i++)
        {
            stats.update(mat.getColumn(i));
        }

        return stats;
    }

    public static Vect solve(Matrix A, Vect B)
    {
        Jama.Matrix jA = new Jama.Matrix(A.toArray());
        Jama.Matrix jB = new Jama.Matrix(MatrixSource.createCol(B).toArray());
        return new Matrix(jA.solve(jB)).getColumn(0);
    }

    public static Matrix solve(Matrix A, Matrix B)
    {
        return new Matrix(new Jama.Matrix(A.toArray()).solve(new Jama.Matrix(B.toArray())));
    }

    public static Vect solve(Matrix A, Vect B, double lambda)
    {
        Matrix Areg = A.plus(A.proto().setAllDiag(lambda));
        return MatrixUtils.solve(Areg, B);
    }

    public static Matrix solve(Matrix A, Matrix B, double lambda)
    {
        Matrix Areg = A.plus(A.proto().setAllDiag(lambda));
        return MatrixUtils.solve(Areg, B);
    }

    public static AxisAngle axisangle(Matrix mat)
    {
        Vect axis = mat.prineig();
        Vect ortho = axis.perp();
        Vect rortho = mat.times(ortho);
        double angle = ortho.angleDeg(rortho);

        return new AxisAngle(axis, angle);
    }

    public static EigenDecomp eig(Matrix mat)
    {
        EigenvalueDecomposition eig = new Jama.Matrix(mat.toArray()).eig();

        int n = mat.rows();
        EigenDecomp out = new EigenDecomp(n);

        Matrix D = new Matrix(eig.getD());
        Matrix V = new Matrix(eig.getV());

        Vect values = D.diag();
        int[] perm = VectUtils.permutation(values);

        for (int i = 0; i < n; i++)
        {
            int idx = perm[n - 1 - i];
            out.values.set(i, values.get(idx));
            out.vectors.add(V.getColumn(idx));
        }

        return out;
    }

    public static SvdDecomp svd(Matrix mat)
    {
        SvdDecomp out = new SvdDecomp();
        SingularValueDecomposition eig = new Jama.Matrix(mat.toArray()).svd();
        out.U = new Matrix(eig.getU());
        out.V = new Matrix(eig.getV());
        out.S = new Matrix(eig.getS());

        return out;
    }

    public static class AxisAngle
    {
        public AxisAngle(Vect axisv, double anglev)
        {
            this.axis = axisv;
            this.angle = anglev;
        }

        public Vect axis;
        public double angle;
    }

    public static class EigenDecomp
    {
        public EigenDecomp(int n)
        {
            this.values = new Vect(n);
            this.vectors = new Vects(n);
        }

        public Vect values;
        public Vects vectors;
    }

    public static class SvdDecomp
    {
        public Matrix U;
        public Matrix S;
        public Matrix V;
    }

    public static Matrix abs(Matrix m)
    {
        Matrix out = m.copy();
        for (int i = 0; i < out.rows(); i++)
        {
            for (int j = 0; j < out.cols(); j++)
            {
                out.set(i, j, Math.abs(out.get(i, j)));
            }
        }

        return out;
    }

    public static Matrix sq(Matrix m)
    {
        Matrix out = m.copy();
        for (int i = 0; i < out.rows(); i++)
        {
            for (int j = 0; j < out.cols(); j++)
            {
                double v = out.get(i, j);
                out.set(i, j, v * v);
            }
        }

        return out;
    }

    public static Matrix recip(Matrix m)
    {
        Matrix out = m.copy();
        for (int i = 0; i < out.rows(); i++)
        {
            for (int j = 0; j < out.cols(); j++)
            {
                double v = out.get(i, j);
                out.set(i, j, 1.0 / v);
            }
        }

        return out;
    }

    public static Matrix errorAbsolute(Matrix value, Matrix approx)
    {
        if (!value.compatible(approx))
        {
            throw new RuntimeException("dimensions do not match");
        }

        Matrix out = value.proto();
        for (int i = 0; i < value.rows(); i++)
        {
            for (int j = 0; j < value.cols(); j++)
            {
                double v = value.get(i, j);
                double a = approx.get(i, j);
                double error = Math.abs(v - a);
                out.set(i, j, error);
            }
        }

        return out;
    }

    public static Matrix errorRelative(Matrix value, Matrix approx)
    {
        if (!value.compatible(approx))
        {
            throw new RuntimeException("dimensions do not match");
        }

        Matrix out = value.proto();
        for (int i = 0; i < value.rows(); i++)
        {
            for (int j = 0; j < value.cols(); j++)
            {
                double v = value.get(i, j);
                double a = approx.get(i, j);
                double numer = Math.abs(v - a);
                double denom = v == 0 ? 1e-6 : Math.abs(v);
                double error = numer / denom;
                out.set(i, j, error);
            }
        }

        return out;
    }

    public static Matrix cholesky(Matrix mat)
    {
        return new Matrix(new Jama.Matrix(mat.toArray()).chol().getL());
    }

    public static List<Integers> knn(Matrix dists, int k)
    {
        Global.assume(k > 0, "k must be positive");
        Global.assume(dists.rows() == dists.cols(), "matrix must be positive");
        List<Integers> out = Lists.newArrayList();

        int n = dists.rows();

        // sort distances to each point
        for (int i = 0; i < n; i++)
        {
            Vect dis = dists.getRow(i);
            int[] perm = VectUtils.permutation(dis);
            int[] nn = new int[k];

            int kidx = 0;
            int pidx = 0;
            while (kidx < k)
            {
                // skip the identity entry
                if (perm[pidx] == i)
                {
                    pidx += 1;
                }
                else
                {
                    nn[kidx] = perm[pidx];
                    pidx += 1;
                    kidx += 1;
                }
            }

            out.add(new Integers(nn));
        }

        return out;
    }

    public Matrix finiteStrain(Matrix J)
    {
        Matrix JT = J.transpose();
        Matrix JJT = J.times(JT);
        Matrix JJTsqrt = MatrixUtils.sqrt(JJT);
        Matrix JJTsqrtinv = JJTsqrt.inv();
        Matrix rot = JJTsqrtinv.times(J);

        return rot;
    }
}