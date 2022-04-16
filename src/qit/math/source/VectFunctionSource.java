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

package qit.math.source;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliUtils;
import qit.base.structs.Pair;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class VectFunctionSource
{
    public static final int JAC_DFXDX = 0;
    public static final int JAC_DFXDY = 1;
    public static final int JAC_DFXDZ = 2;
    public static final int JAC_DFYDX = 3;
    public static final int JAC_DFYDY = 4;
    public static final int JAC_DFYDZ = 5;
    public static final int JAC_DFZDX = 6;
    public static final int JAC_DFZDY = 7;
    public static final int JAC_DFZDZ = 8;
    public static final int JAC_DIM = 9;

    public static final int DIFF_F = 0;
    public static final int DIFF_X = 1;
    public static final int DIFF_Y = 2;
    public static final int DIFF_Z = 3;
    public static final int DIFF_XX = 4;
    public static final int DIFF_XY = 5;
    public static final int DIFF_XZ = 6;
    public static final int DIFF_YY = 7;
    public static final int DIFF_YZ = 8;
    public static final int DIFF_ZZ = 9;
    public static final int DIFF_DIM = 10;

    public static int FEATURE_NORM_X = 0;
    public static int FEATURE_NORM_Y = 1;
    public static int FEATURE_NORM_Z = 2;
    public static int FEATURE_MEAN = 3;
    public static int FEATURE_GAUSS = 4;
    public static int FEATURE_KMIN = 5;
    public static int FEATURE_KMAX = 6;
    public static int FEATURE_EMIN_X = 7;
    public static int FEATURE_EMIN_Y = 8;
    public static int FEATURE_EMIN_Z = 9;
    public static int FEATURE_EMAX_X = 10;
    public static int FEATURE_EMAX_Y = 11;
    public static int FEATURE_EMAX_Z = 12;
    public static int FEATURE_SI = 13;
    public static int FEATURE_CN = 14;
    public static int FEATURE_CI = 15;
    public static int FEATURE_DIM = 16;

    public static VectFunction isosphere(Vect p, double r)
    {
        final Vect fp = p.copy();
        final double fr = r;

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Vect pm = fp.minus(input);

                double diff_f = pm.norm2() - fr * fr;
                double diff_x = 2 * pm.get(0);
                double diff_y = 2 * pm.get(1);
                double diff_z = 2 * pm.get(2);
                double diff_xx = 2;
                double diff_yy = 2;
                double diff_zz = 2;
                double diff_xy = 0;
                double diff_xz = 0;
                double diff_yz = 0;

                output.set(DIFF_F, diff_f);
                output.set(DIFF_X, diff_x);
                output.set(DIFF_Y, diff_y);
                output.set(DIFF_Z, diff_z);
                output.set(DIFF_XX, diff_xx);
                output.set(DIFF_XY, diff_xy);
                output.set(DIFF_XZ, diff_xz);
                output.set(DIFF_YY, diff_yy);
                output.set(DIFF_YZ, diff_yz);
                output.set(DIFF_ZZ, diff_zz);
            }
        }.init(3, DIFF_DIM);
    }

    public static VectFunction polynomial(final Vects ts, final Vects vs, final int order)
    {
        return polynomial(ts, null, vs, order, null);
    }

    public static VectFunction polynomial(final Vects ts, final Vects ws, final Vects vs, final int order)
    {
        return polynomial(ts, ws, vs, order, null);
    }

    public static VectFunction polynomial(final Vects t, final Vects vs, final int order, final Double lambda)
    {
        return polynomial(t, null, vs, order, lambda);
    }

    public static VectFunction polynomial(final Vects ts, final Vects ws, final Vects vs, final int order, final Double lambda)
    {
        // this fits a polynomial of a given order to make a function f: t -> V
        // lambda is the (optional) negative log tikohonov regularization
        // w are the (optional) weights

        Global.assume(ts.size() == vs.size(), "input sizes must match");
        Global.assume(ts.getDim() == 1, "input must be one dimensional");
        Global.assume(ws == null || ws.getDim() == 1, "input must be one dimensional");

        final int dim = vs.getDim();
        final int num = ts.size();
        final int coeff = order + 1;

        Matrix A = new Matrix(dim * num, dim * coeff);
        Vect b = VectSource.createND(dim * num);

        for (int i = 0; i < num; i++)
        {
            Vect p = vs.get(i);
            Vect tp = MathUtils.poly(ts.get(i).get(0), order);
            int idx = dim * i;

            for (int j = 0; j < order + 1; j++)
            {
                for (int k = 0; k < dim; k++)
                {
                    A.set(idx + k, k * coeff + j, tp.get(j));
                }
            }

            for (int k = 0; k < dim; k++)
            {
                b.set(idx + k, p.get(k));
            }
        }

        Matrix At = A.transpose();

        if (ws != null)
        {
            Global.assume(ws.size() == vs.size(), "input sizes must match");

            Vect wv = VectSource.createND(dim * num);
            for (int i = 0; i < num; i++)
            {
                int idx = dim * i;
                for (int k = 0; k < dim; k++)
                {
                    wv.set(idx + k, ws.get(i).get(0));
                }
            }

            Matrix W = MatrixSource.diag(wv);
            At = At.times(W);
        }

        Matrix AtA = At.times(A);

        Matrix S = AtA;

        if (lambda != null)
        {
            double lamb = Math.exp(-lambda);
            S = AtA.plus(lamb * lamb, MatrixSource.identity(dim * coeff));
        }

        Vect soln = S.inv().times(At.times(b));

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Vect encoded = MathUtils.poly(input.get(0), order);
                for (int k = 0; k < dim; k++)
                {
                    output.set(k, encoded.dot(soln.sub(coeff * k, coeff * (k + 1))));
                }
            }
        }.init(1, dim);
    }

    public static VectFunction kernel(final double s, final Vects ts, final Vects vs, final int order, double thresh)
    {
        final int num = ts.size();
        final int dim = vs.getDim();
        final double s2 = s * s;

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double tn = input.get(0);

                Vects nts = new Vects();
                Vects nws = new Vects();
                Vects nvs = new Vects();

                for (int i = 0; i < num; i++)
                {
                    double t = ts.get(i).get(0);
                    double d = tn - t;
                    double w = Math.exp(-d * d / s2);

                    if (w > thresh)
                    {
                        nws.add(VectSource.create1D(w));
                        nts.add(VectSource.create1D(t));
                        nvs.add(vs.get(i));
                    }
                }

                output.set(polynomial(nts, nws, nvs, order).apply(input));
            }
        }.init(1, dim);
    }

    public static VectFunction lowess(Vects ts, Vects vs, int num, int order)
    {
        if (ts.size() < 4)
        {
            return linear(ts, vs);
        }

        // assume ts and vs are sorted
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                int nearest = ts.nearest(input);

                Vects pos = new Vects();

                int nearestIndex = 0;
                Vect nearestPos = null;
                List<Integer> idx = Lists.newArrayList();

                for (int j = Math.max(0, nearest - num); j < Math.min(ts.size(), nearest + num); j++)
                {
                    Vect mypos = vs.get(j);
                    if (j == nearest)
                    {
                        nearestIndex = pos.size();
                        nearestPos = mypos;
                    }

                    pos.add(mypos);
                    idx.add(j);
                }

                Global.assume(nearestPos != null, "failed to find nearest neighbor");

                if (pos.size() < 4)
                {
                    output.set(VectFunctionSource.linear(ts, vs).apply(input));
                }

                Vect cumlen = new Vect(pos.size());
                for (int j = 0; j < pos.size(); j++)
                {
                    cumlen.set(j, ts.get(idx.get(j)).minus(ts.get(idx.get(0))));
                }

                Vect dists = new Vect(pos.size());
                for (int j = 0; j < pos.size(); j++)
                {
                    dists.set(j, nearestPos.dist(pos.get(j)));
                }

                Vect weights = dists.times(1.0 / dists.max()).abs().pow(3).times(-1).plus(1).pow(3);
                Vect gamma = cumlen.times(1.0 / cumlen.get(cumlen.size() - 1));

                try
                {
                    VectFunction function = VectFunctionSource.polynomial(gamma.vects(), weights.vects(), pos, order);
                    Vect npos = function.apply(gamma.getv(nearestIndex));

                    if (npos.finite())
                    {
                        output.set(npos);
                    }
                    else
                    {
                        output.set(pos.get(nearest));
                    }
                }
                catch (RuntimeException e)
                {
                    output.set(pos.get(nearest));
                }

            }
        }.init(1, vs.getDim());
    }

    public static VectFunction linear(Vects ts, Vects vs)
    {
        if (ts.size() == 0 || MathUtils.zero(vs.first().dist(vs.last())))
        {
            return new VectFunction()
            {
                public void apply(Vect input, Vect output)
                {
                    output.setAll(0);
                }
            }.init(1, vs.getDim());
        }
        else if (ts.size() == 1 || MathUtils.zero(vs.first().dist(vs.last())))
        {
            return new VectFunction()
            {
                public void apply(Vect input, Vect output)
                {
                    output.set(vs.get(0));
                }
            }.init(1, vs.getDim());
        }
        else
        {
            // assume ts and vs are sorted
            return new VectFunction()
            {
                public void apply(Vect input, Vect output)
                {
                    double t = input.first();
                    int nearest = ts.nearest(input);
                    boolean before = (t < ts.get(nearest).first() || nearest == ts.size() - 1) && nearest > 0;

                    int aidx = before ? nearest - 1 : nearest;
                    int bidx = before ? nearest : nearest + 1;

                    double at = ts.get(aidx).first();
                    double bt = ts.get(bidx).first();

                    Vect av = vs.get(aidx);
                    Vect bv = vs.get(bidx);

                    double delta = MathUtils.eq(at, bt) ? 1.0 : bt - at;
                    double mix = (t - at) / delta;
                    Vect nv = av.times(mix).plus(1.0 - mix, bv);

                    output.set(nv);
                }
            }.init(1, vs.getDim());
        }
    }

    public static VectFunction reorient(final VectFunction xfm, final double dx, final double dy, final double dz)
    {
        final VectFunction jac = jac(xfm, dx, dy, dz);
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                // treat input/output as px, py, pz, dx, dy, dz

                Vect pos = input.sub(0, 3);
                Vect dir = input.sub(3, 6);

                pos = xfm.apply(pos);
                dir = MatrixSource.createCols(jac.apply(pos), 3, 3).times(dir).normalize();

                output.set(0, pos);
                output.set(3, dir);
            }
        }.init(6, 6);
    }

    public static VectFunction jac(final VectFunction f, final double dx, final double dy, final double dz)
    {
        final double fx = 1.0 / (2.0 * dx);
        final double fy = 1.0 / (2.0 * dy);
        final double fz = 1.0 / (2.0 * dz);
        final Vect[][][] sampled = new Vect[3][3][3];

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double x = input.get(0);
                double y = input.get(1);
                double z = input.get(2);

                for (int i = 0; i < 3; i++)
                {
                    for (int j = 0; j < 3; j++)
                    {
                        for (int k = 0; k < 3; k++)
                        {
                            double sx = x + (i - 1) * dx;
                            double sy = y + (j - 1) * dy;
                            double sz = z + (k - 1) * dz;
                            sampled[i][j][k] = f.apply(VectSource.create3D(sx, sy, sz));
                        }
                    }
                }

                double dfxdx = fx * (sampled[2][1][1].get(0) - sampled[0][1][1].get(0));
                double dfxdy = fy * (sampled[1][2][1].get(0) - sampled[1][0][1].get(0));
                double dfxdz = fz * (sampled[1][1][2].get(0) - sampled[1][1][0].get(0));
                double dfydx = fx * (sampled[2][1][1].get(1) - sampled[0][1][1].get(1));
                double dfydy = fy * (sampled[1][2][1].get(1) - sampled[1][0][1].get(1));
                double dfydz = fz * (sampled[1][1][2].get(1) - sampled[1][1][0].get(1));
                double dfzdx = fx * (sampled[2][1][1].get(2) - sampled[0][1][1].get(2));
                double dfzdy = fy * (sampled[1][2][1].get(2) - sampled[1][0][1].get(2));
                double dfzdz = fz * (sampled[1][1][2].get(2) - sampled[1][1][0].get(2));

                output.set(JAC_DFXDX, dfxdx);
                output.set(JAC_DFXDY, dfxdy);
                output.set(JAC_DFXDZ, dfxdz);
                output.set(JAC_DFYDX, dfydx);
                output.set(JAC_DFYDY, dfydy);
                output.set(JAC_DFYDZ, dfydz);
                output.set(JAC_DFZDX, dfzdx);
                output.set(JAC_DFZDY, dfzdy);
                output.set(JAC_DFZDZ, dfzdz);
            }
        }.init(3, JAC_DIM);
    }

    public static VectFunction diff(final VectFunction f, final double dx, final double dy, final double dz)
    {
        final double fx = 1.0 / (2.0 * dx);
        final double fy = 1.0 / (2.0 * dy);
        final double fz = 1.0 / (2.0 * dz);
        final double fxx = 1.0 / (dx * dx);
        final double fyy = 1.0 / (dy * dy);
        final double fzz = 1.0 / (dz * dz);
        final double fxy = 1.0 / (4.0 * dx * dy);
        final double fxz = 1.0 / (4.0 * dx * dz);
        final double fyz = 1.0 / (4.0 * dy * dz);
        final double[][][] sampled = new double[3][3][3];

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double x = input.get(0);
                double y = input.get(1);
                double z = input.get(2);

                for (int i = 0; i < 3; i++)
                {
                    for (int j = 0; j < 3; j++)
                    {
                        for (int k = 0; k < 3; k++)
                        {
                            double sx = x + (i - 1) * dx;
                            double sy = y + (j - 1) * dy;
                            double sz = z + (k - 1) * dz;
                            sampled[i][j][k] = f.apply(VectSource.create3D(sx, sy, sz)).get(0);
                        }
                    }
                }

                double diff_f = sampled[1][1][1];
                double diff_x = fx * (sampled[0][1][1] - sampled[2][1][1]);
                double diff_y = fy * (sampled[1][0][1] - sampled[1][2][1]);
                double diff_z = fz * (sampled[1][1][0] - sampled[1][1][2]);
                double diff_xx = fxx * (sampled[0][1][1] - 2.0 * sampled[1][1][1] + sampled[2][1][1]);
                double diff_yy = fyy * (sampled[1][0][1] - 2.0 * sampled[1][1][1] + sampled[1][2][1]);
                double diff_zz = fzz * (sampled[1][1][0] - 2.0 * sampled[1][1][1] + sampled[1][1][2]);
                double diff_xy = fxy * (sampled[0][0][1] - sampled[2][0][1] - sampled[0][2][1] + sampled[2][2][1]);
                double diff_xz = fxz * (sampled[0][1][0] - sampled[0][1][2] - sampled[2][1][0] + sampled[2][1][2]);
                double diff_yz = fyz * (sampled[1][0][0] - sampled[1][2][0] - sampled[1][0][2] + sampled[1][2][2]);

                output.set(DIFF_F, diff_f);
                output.set(DIFF_X, diff_x);
                output.set(DIFF_Y, diff_y);
                output.set(DIFF_Z, diff_z);
                output.set(DIFF_XX, diff_xx);
                output.set(DIFF_XY, diff_xy);
                output.set(DIFF_XZ, diff_xz);
                output.set(DIFF_YY, diff_yy);
                output.set(DIFF_YZ, diff_yz);
                output.set(DIFF_ZZ, diff_zz);
            }
        }.init(3, DIFF_DIM);
    }

    public static VectFunction features()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double px = input.get(VectFunctionSource.DIFF_X);
                double py = input.get(VectFunctionSource.DIFF_Y);
                double pz = input.get(VectFunctionSource.DIFF_Z);
                double pxx = input.get(VectFunctionSource.DIFF_XX);
                double pxy = input.get(VectFunctionSource.DIFF_XY);
                double pxz = input.get(VectFunctionSource.DIFF_XZ);
                double pyy = input.get(VectFunctionSource.DIFF_YY);
                double pyz = input.get(VectFunctionSource.DIFF_YZ);
                double pzz = input.get(VectFunctionSource.DIFF_ZZ);

                double px2 = px * px;
                double py2 = py * py;
                double pz2 = pz * pz;

                double mean_num = 0;
                mean_num += pxx * (py2 + pz2);
                mean_num += pyy * (px2 + pz2);
                mean_num += pzz * (px2 + py2);
                mean_num -= 2 * px * py * pxy;
                mean_num -= 2 * py * pz * pyz;
                mean_num -= 2 * px * pz * pxz;

                double s2 = px2 + py2 + pz2;
                double d3 = Math.pow(s2, 1.5);
                double mean_denom = 2 * d3;

                double mean = MathUtils.zero(mean_denom) ? 0 : -mean_num / mean_denom;

                double gauss_denom = s2 * s2;

                double gauss_numa = 0;
                gauss_numa += px2 * (pyy * pzz - pyz * pyz);
                gauss_numa += py2 * (pxx * pzz - pxz * pxz);
                gauss_numa += pz2 * (pxx * pyy - pxy * pxy);

                double gauss_numb = 0;
                gauss_numb += px * py * (pxz * pyz - pxy * pzz);
                gauss_numb += py * pz * (pxy * pxz - pyz * pxx);
                gauss_numb += px * pz * (pxy * pyz - pxz * pyy);
                gauss_numb *= 2.0;

                double gauss = MathUtils.zero(gauss_denom) ? 0 : (gauss_numa - gauss_numb) / gauss_denom;

                double dis = mean * mean - gauss;

                double kt = dis < 0 ? 0 : Math.sqrt(dis);
                double kmax = mean + kt;
                double kmin = mean - kt;

                double n1 = 0;
                n1 += py * px * pyy * pz - py2 * px * pyz - pz2 * pz * pxy + pz2 * pxz * py;
                n1 += pz2 * pyz * px - pz * pzz * px * py - py2 * pxy * pz + py2 * py * pxz;
                n1 *= -1.0;

                double n2 = 0;
                n2 += py * px * pxy * pz + py2 * pxz * px - px2 * pyz * py;
                n2 += 2 * pz2 * pxz * px - pz * pzz * px2 - pz2 * pxx * pz;
                n2 *= -1.0;

                double d = pz * d3;
                double eminx = -pz * n1;
                double eminy = -pz * n2 + pz * kmin * d;
                double eminz = -px * n1 + py * (n2 - kmin * d);
                double emaxx = -pz * n1;
                double emaxy = -pz * n2 + pz * kmax * d;
                double emaxz = -px * n1 + py * (n2 - kmax * d);

                Vect n = VectSource.create3D(px, py, pz).normalize();
                double norm_x = n.get(0);
                double norm_y = n.get(1);
                double norm_z = n.get(2);

                double cn = Math.sqrt((kmax * kmax + kmin * kmin) / 2.0);
                double si = 2.0 / Math.PI * Math.atan2(kmin + kmax, kmin - kmax);
                double ci = MathUtils.sign(si) * cn;

                output.set(FEATURE_NORM_X, norm_x);
                output.set(FEATURE_NORM_Y, norm_y);
                output.set(FEATURE_NORM_Z, norm_z);
                output.set(FEATURE_GAUSS, gauss);
                output.set(FEATURE_MEAN, mean);
                output.set(FEATURE_KMIN, kmin);
                output.set(FEATURE_KMAX, kmax);
                output.set(FEATURE_EMIN_X, eminx);
                output.set(FEATURE_EMIN_Y, eminy);
                output.set(FEATURE_EMIN_Z, eminz);
                output.set(FEATURE_EMAX_X, emaxx);
                output.set(FEATURE_EMAX_Y, emaxy);
                output.set(FEATURE_EMAX_Z, emaxz);
                output.set(FEATURE_SI, si);
                output.set(FEATURE_CN, cn);
                output.set(FEATURE_CI, ci);
            }
        }.init(VectFunctionSource.DIFF_DIM, FEATURE_DIM);
    }

    public static VectFunction linearInterp(Table table, String from, String to)
    {
        List<Pair<Double,Double>> pairs = Lists.newArrayList();

        for (int key : table.getKeys())
        {
            Record record = table.getRecord(key);

            try
            {
                double fromval = Double.valueOf(record.get(from));
                double toval = Double.valueOf(record.get(to));

                pairs.add(Pair.of(fromval, toval));
            }
            catch (RuntimeException e)
            {
                Logging.info("... skipping invalid record");
            }
        }

        return linearInterp(pairs);
    }

    public static VectFunction linearInterp(List<Pair<Double, Double>> pairs)
    {
        Global.assume(pairs.size() > 0, "invalid pairs");

        int n = pairs.size();
        double[] xs = new double[n];
        Vect[] ys = new Vect[n];

        for (int i = 0; i < n; i++)
        {
            Pair<Double, Double> pair = pairs.get(i);
            xs[i] = pair.a;
            ys[i] = VectSource.create1D(pair.b);
        }

        return linearInterp(xs, ys);
    }

    public static VectFunction linearInterp(double[] vxs, Vect[] vys)
    {
        Global.assume(vxs.length > 0 && vys.length > 0, "invalid pairs");
        Global.assume(vxs.length == vys.length, "invalid pairs");

        int[] perm = MathUtils.permutation(vxs);

        final List<Double> xs = Lists.newArrayList();
        final List<Vect> ys = Lists.newArrayList();

        for (int element : perm)
        {
            double x = vxs[element];
            Vect y = vys[element].copy();

            // skip duplicates
            if (xs.size() == 0 || !MathUtils.eq(x, xs.get(xs.size() - 1)))
            {
                xs.add(x);
                ys.add(y);
            }
        }
        final int n = xs.size();

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double x = input.get(0);

                if (x <= xs.get(0))
                {
                    output.set(ys.get(0));
                    return;
                }

                if (x >= xs.get(n - 1))
                {
                    output.set(ys.get(n - 1));
                    return;
                }

                for (int i = 1; i < n; i++)
                {
                    if (x <= xs.get(i))
                    {
                        double xp = xs.get(i - 1);
                        double xn = xs.get(i);
                        Vect yp = ys.get(i - 1);
                        Vect yn = ys.get(i);

                        double alpha = (x - xp) / (xn - xp);

                        for (int j = 0; j < output.size(); j++)
                        {
                            double ny = yp.get(j) + alpha * (yn.get(j) - yp.get(j));
                            output.set(j, ny);
                        }

                        return;
                    }
                }

                throw new RuntimeException("an unexpected error occurred during interpolation for x = " + x);
            }
        }.init(1, vys[0].size());
    }

    public static VectFunction pad(final int from, final int to)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < to; i++)
                {
                    if (i < from)
                    {
                        output.set(i, input.get(i));
                    }
                    else
                    {
                        output.set(i, 0);
                    }
                }
            }
        }.init(from, to);
    }

    public static VectFunction ramp(final double x0, final double y0, final double x1, final double y1)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                if (!MathUtils.eq(x0, x1))
                {
                    double x = input.get(0);

                    if (x < x0)
                    {
                        output.set(0, y0);
                        return;
                    }

                    if (x > x1)
                    {
                        output.set(0, y1);
                        return;
                    }

                    double alpha = (x - x0) / (x1 - x0);
                    double y = y0 + alpha * (y1 - y0);

                    output.set(0, y);
                }
                else
                {
                    output.set(0, y1);
                }
            }
        }.init(1, 1);
    }

    public static VectFunction xfm(String fn) throws IOException
    {
        if (fn.endsWith("nii.gz") || fn.endsWith("vtk"))
        {
            VectFunction xfm = VolumeUtils.interp(InterpolationType.Trilinear, Volume.read(fn));
            xfm = deform(xfm);
            return xfm;
        }
        else
        {
            return Affine.read(fn);
        }
    }

    public static VectFunction square(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, input.get(i) * input.get(i));
                }
            }
        }.init(dim, dim);
    }

    public static VectFunction sqrt(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.sqrt(input.get(i)));
                }
            }
        }.init(dim, dim);
    }

    public static VectFunction abs(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.abs(input.get(i)));
                }
            }
        }.init(dim, dim);
    }

    public static VectFunction exp(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.exp(input.get(i)));
                }
            }
        }.init(dim, dim);
    }

    public static VectFunction log(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.log(input.get(i)));
                }
            }
        }.init(dim, dim);
    }

    public static VectFunction recip(final boolean zero)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double value = input.get(0);
                if (zero && value == 0)
                {
                    output.set(0, 0);
                }
                else
                {
                    output.set(0, 1.0 / value);
                }
            }
        }.init(1, 1);
    }

    public static VectFunction invprob(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, 1 - input.get(i));
                }
            }
        }.init(dim, dim);
    }

    public static VectFunction round(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.round(input.get(i)));
                }
            }
        }.init(dim, dim);
    }

    public static VectFunction pow(final double pow, int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < input.size(); i++)
                {
                    output.set(i, Math.pow(input.get(i), pow));
                }
            }
        }.init(dim, dim);
    }

    public static VectFunction add(final double scalar, final Vect value)
    {
        final Vect fvalue = value.copy();
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(input);
                for (int i = 0; i < fvalue.size(); i++)
                {
                    output.set(i, output.get(i) + scalar * value.get(i));
                }
            }
        }.init(value.size(), value.size());
    }

    public static VectFunction add(final double value)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(input);
                output.set(0, input.get(0) + value);
            }
        }.init(1, 1);
    }

    public static VectFunction scale(final double sx, final double sy, final double sz)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(0, sx * input.get(0));
                output.set(1, sy * input.get(1));
                output.set(2, sz * input.get(2));
            }
        }.init(3, 3);
    }

    public static VectFunction scale(final double scalar, final int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(input.times(scalar));
            }
        }.init(dim, dim);
    }

    public static VectFunction scale(final Vect scale)
    {
        final int dim = scale.size();
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(scale.times(input));
            }
        }.init(dim, dim);
    }

    public static VectFunction scale(final double scalar)
    {
        return scale(scalar, 1);
    }

    public static VectFunction constant(Vect out)
    {
        return constant(1, out);
    }

    public static VectFunction square()
    {
        return square(1);
    }

    public static VectFunction sqrt()
    {
        return sqrt(1);
    }

    public static VectFunction abs()
    {
        return abs(1);
    }

    public static VectFunction exp()
    {
        return exp(1);
    }

    public static VectFunction log()
    {
        return log(1);
    }

    public static VectFunction round()
    {
        return round(1);
    }

    public static VectFunction invprob()
    {
        return invprob(1);
    }

    public static VectFunction constant(int dim, Vect out)
    {
        final Vect fout = out.copy();
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(fout);
            }
        }.init(dim, out.size());
    }

    public static VectFunction linmap(final double x0, final double x1, final double y0, final double y1,
                                      final boolean clamp)
    {
        final double dx = x1 - x0;
        final double dy = y1 - y0;

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double value = input.get(0);

                if (MathUtils.zero(dx))
                {
                    output.set(0, y0);
                }
                else if (clamp && value < x0)
                {
                    output.set(0, y0);
                }
                else if (clamp && value > x1)
                {
                    output.set(0, y1);
                }
                else
                {
                    output.set(0, (value - x0) * dy / dx + y0);
                }
            }
        }.init(1, 1);
    }

    public static VectFunction thresh(double value, int idx, int dim)
    {
        final int fidx = idx;
        final double fvalue = value;
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double v = input.get(fidx);
                if (v >= fvalue)
                {
                    output.set(0, 1.0);
                }
                else
                {
                    output.set(0, 0.0);
                }
            }
        }.init(dim, 1);
    }

    public static VectFunction clamp(final double value, final int idx, int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double v = input.get(idx);
                if (v >= value)
                {
                    output.set(0, value);
                }
                else
                {
                    output.set(0, v);
                }
            }
        }.init(dim, 1);
    }

    public static VectFunction thresh(double value)
    {
        return thresh(value, 0, 1);
    }

    public static VectFunction clamp(double value)
    {
        return clamp(value, 0, 1);
    }

    public static VectFunction normalize(final int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(input.normalize());
            }
        }.init(dim, dim);
    }

    public static VectFunction norm(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(0, input.norm());
            }
        }.init(dim, 1);
    }

    public static VectFunction norm2(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(0, input.norm2());
            }
        }.init(dim, 1);
    }

    public static VectFunction normalize()
    {
        return normalize(3);
    }

    public static VectFunction norm()
    {
        return norm(3);
    }

    public static VectFunction norm2()
    {
        return norm2(3);
    }

    public static VectFunction mean(int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double m = 0;
                int n = input.size();

                for (int i = 0; i < n; i++)
                {
                    m += input.get(i);
                }

                m /= n;

                output.set(0, m);
            }
        }.init(dim, 1);
    }

    public static VectFunction mean()
    {
        return mean(3);
    }

    public static VectFunction index(List<Integer> idx, final int dim)
    {
        int[] nidx = new int[idx.size()];
        for (int i = 0; i < idx.size(); i++)
        {
            nidx[i] = idx.get(i);
        }

        return index(nidx, dim);
    }

    public static VectFunction index(int[] idx, final int dim)
    {
        final int num = idx.length;
        final int[] fidx = new int[num];
        for (int i = 0; i < num; i++)
        {
            fidx[i] = idx[i];
        }

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < num; i++)
                {
                    output.set(i, input.get(fidx[i]));
                }
            }
        }.init(dim, num);
    }

    public static VectFunction map(final boolean keep, final Map<Integer, Integer> map)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                int value = (int) input.get(0);
                if (map.containsKey(value))
                {
                    output.set(0, map.get(value));
                }
                else if (keep)
                {
                    output.set(0, input.get(0));
                }
                else
                {
                    output.set(0, 0);
                }
            }
        }.init(1, 1);
    }

    public static VectFunction index(final int idx, final int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(0, input.get(idx));
            }
        }.init(dim, 1);
    }

    public static VectFunction repeat(final int indim, final int outdim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < outdim; i++)
                {
                    output.set(i, input.get(i % indim));
                }
            }
        }.init(indim, outdim);
    }

    public static VectFunction identity(final int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(input);
            }
        }.init(dim, dim);
    }

    public static VectFunction swap(final int a, final int b, final int dim)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(input);

                double va = input.get(a);
                double vb = input.get(b);

                output.set(b, va);
                output.set(a, vb);
            }
        }.init(dim, dim);
    }

    public static VectFunction equals(Vect value)
    {
        final Vect fvalue = value.copy();
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(0, input.equals(fvalue) ? 1.0 : 0.0);
            }
        }.init(value.size(), 1);
    }

    public static VectFunction equals(double value)
    {
        return equals(value, 0, 1);
    }

    public static VectFunction equals(final double value, final int idx, int dim)
    {

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(0, MathUtils.eq(value, input.get(idx)) ? 1.0 : 0.0);
            }
        }.init(dim, 1);
    }

    public static VectFunction gaussian(final double mu, final double sigma)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double delta = mu - input.get(0);
                double arg = -delta * delta / (sigma * sigma);
                double val = Math.exp(arg);
                output.set(0, val);
            }
        }.init(1, 1);
    }

    public static VectFunction gaussianND(final int dim, final double sigma)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < input.size(); i++)
                {
                    double value = input.get(i);
                    double noise = Global.RANDOM.nextGaussian() * sigma;
                    double noisy = value + noise;
                    output.set(i, noisy);
                }
            }
        }.init(dim, dim);
    }

    public static VectFunction rgbToGray()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double r = input.get(0);
                double g = input.get(1);
                double b = input.get(2);
                double v = 0.2989 * r + 0.5870 * g + 0.1140 * b;
                output.set(0, v);
            }
        }.init(3, 1);
    }

    public static VectFunction rgbaToGray()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double r = input.get(0);
                double g = input.get(1);
                double b = input.get(2);
                double v = 0.2989 * r + 0.5870 * g + 0.1140 * b;
                output.set(0, v);
            }
        }.init(4, 1);
    }

    public static VectFunction rgbToHsv()
    {
        final float[] hsv = new float[3];
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                int r = (int) input.get(0);
                int g = (int) input.get(1);
                int b = (int) input.get(2);

                Color.RGBtoHSB(r, g, b, hsv);

                output.set(0, hsv[0]);
                output.set(1, hsv[1]);
                output.set(2, hsv[2]);
            }
        }.init(3, 3);
    }

    public static VectFunction deform(final VectFunction deform)
    {
        final int dim = deform.getDimIn();
        final Vect buff = VectSource.createND(dim);
        Global.assume(dim == deform.getDimOut(), "invalid deformation");

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(input.plus(deform.apply(input)));
            }
        }.init(dim, dim);
    }

    public static VectFunction sphereCoords()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                // assume (r, theta, phi) = f(x, y, z)
                double x = input.get(0);
                double y = input.get(1);
                double z = input.get(2);

                double r = Math.sqrt(x * x + y * y + z * z);
                double theta = MathUtils.zero(r) ? 0 : Math.acos(z / r);
                double phi = Math.atan2(y, x);

                output.set(0, r);
                output.set(1, theta);
                output.set(2, phi);
            }
        }.init(3, 3);
    }

    public static VectFunction sphereTexture()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                // assume (r, theta, phi) = f(x, y, z)
                double x = input.get(0);
                double y = input.get(1);
                double z = input.get(2);

                double r = Math.sqrt(x * x + y * y + z * z);
                double theta = MathUtils.zero(r) ? 0 : Math.acos(z / r);
                double phi = Math.atan2(y, x);

                double u = theta / Math.PI;
                double v = 0.5 * ((phi / Math.PI) + 1);

                output.set(0, u);
                output.set(1, v);
            }
        }.init(3, 2);
    }

    public static VectFunction sphereToCart()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                // assume (x, y, z) = f(r, theta, phi)
                double r = input.get(0);
                double theta = input.get(1);
                double phi = input.get(2);

                double st = Math.sin(theta);
                double sp = Math.sin(phi);
                double ct = Math.cos(theta);
                double cp = Math.cos(phi);

                double x = r * st * cp;
                double y = r * st * sp;
                double z = r * ct;

                output.set(0, x);
                output.set(1, y);
                output.set(2, z);
            }
        }.init(3, 3);
    }

    public static VectFunction angleToCart()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                // assume (x, y, z) = f(theta, phi)
                double theta = input.get(0);
                double phi = input.get(1);

                double st = Math.sin(theta);
                double sp = Math.sin(phi);
                double ct = Math.cos(theta);
                double cp = Math.cos(phi);

                double x = st * cp;
                double y = st * sp;
                double z = ct;

                output.set(0, x);
                output.set(1, y);
                output.set(2, z);
            }
        }.init(2, 3);
    }

    public static VectFunction warp(final VectFunction displacement)
    {
        if (displacement.getDimIn() != displacement.getDimOut())
        {
            throw new RuntimeException("invalid displacement function");
        }

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                displacement.apply(input, output);
                output.plusEquals(input);
            }
        }.init(displacement.getDimIn(), displacement.getDimOut());
    }

    public static VectFunction fan(final int dim, final VectFunction inner)
    {
        if (inner.getDimIn() != 1 || inner.getDimOut() != 1)
        {
            throw new RuntimeException("invalid function");
        }

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < dim; i++)
                {
                    Vect soutput = inner.apply(input);
                    output.set(i, soutput.get(0));
                }
            }
        }.init(1, dim);
    }

    public static VectFunction sphere()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double theta = input.get(0);
                double phi = input.get(1);

                double cos_theta = Math.cos(theta);
                double sin_theta = Math.sin(theta);
                double cos_phi = Math.cos(phi);
                double sin_phi = Math.sin(phi);

                double x = cos_theta * sin_phi;
                double y = sin_theta * sin_phi;
                double z = cos_phi;

                output.set(0, x);
                output.set(1, y);
                output.set(2, z);
            }
        }.init(3, 3);
    }

    public static VectFunction rgba()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < 4; i++)
                {
                    double v = Math.abs(input.get(i));
                    v = Math.min(1.0, Math.max(0.0, v));
                    output.set(i, v);
                }
            }
        }.init(4, 4);
    }

    public static VectFunction rgba255()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < 4; i++)
                {
                    double v = Math.abs(input.get(i) / 255.0);
                    v = Math.min(1.0, Math.max(0.0, v));
                    output.set(i, v);
                }
            }
        }.init(4, 4);
    }

    public static VectFunction rgb255()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < 3; i++)
                {
                    double v = Math.abs(input.get(i) / 255.0);
                    v = Math.min(1.0, Math.max(0.0, v));
                    output.set(i, v);
                }
                output.set(3, 1.0);
            }
        }.init(3, 4);
    }

    public static VectFunction rgb()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < 3; i++)
                {
                    double v = Math.abs(input.get(i));
                    v = Math.min(1.0, Math.max(0.0, v));
                    output.set(i, v);
                }
                output.set(3, 1.0);
            }
        }.init(3, 4);
    }

    public static VectFunction rgbsq()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double mag = input.norm();
                Vect vec = input.times(mag);

                for (int i = 0; i < 3; i++)
                {
                    double v = Math.abs(vec.get(i));
                    v = Math.min(1.0, Math.max(0.0, v));
                    output.set(i, v);
                }

                output.set(3, 1.0);
            }
        }.init(3, 4);
    }

    public static VectFunction rgbsqrt()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double mag = input.norm();
                double nmag = MathUtils.zero(mag) ? 1.0 : 1.0 / mag;
                Vect vec = input.times(Math.sqrt(mag) * nmag);

                for (int i = 0; i < 3; i++)
                {
                    double v = Math.abs(vec.get(i));
                    v = Math.min(1.0, Math.max(0.0, v));
                    output.set(i, v);
                }

                output.set(3, 1.0);
            }
        }.init(3, 4);
    }

    public static VectFunction rgbdouble()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double mag = input.norm();
                double nmag = MathUtils.zero(mag) ? 1.0 : 1.0 / mag;
                Vect vec = input.times(2.0);

                for (int i = 0; i < 3; i++)
                {
                    double v = Math.abs(vec.get(i));
                    v = Math.min(1.0, Math.max(0.0, v));
                    output.set(i, v);
                }

                output.set(3, 1.0);
            }
        }.init(3, 4);
    }

    public static VectFunction rgbquad()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double mag = input.norm();
                double nmag = MathUtils.zero(mag) ? 1.0 : 1.0 / mag;
                Vect vec = input.times(4.0);

                for (int i = 0; i < 3; i++)
                {
                    double v = Math.abs(vec.get(i));
                    v = Math.min(1.0, Math.max(0.0, v));
                    output.set(i, v);
                }

                output.set(3, 1.0);
            }
        }.init(3, 4);
    }

    public static VectFunction rgbnorm()
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double mag = input.norm();
                Vect vec = MathUtils.zero(mag) ? input : input.normalize();

                for (int i = 0; i < 3; i++)
                {
                    double v = Math.abs(vec.get(i));
                    v = Math.min(1.0, Math.max(0.0, v));
                    output.set(i, v);
                }

                output.set(3, 1.0);
            }
        }.init(3, 4);
    }

    public static VectFunction wash(final double wash)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < 3; i++)
                {
                    double v = input.get(i);
                    double w = Math.min(1, Math.abs(v) + wash);
                    output.set(i, w);
                }
                output.set(3, input.get(3));
            }
        }.init(4, 4);
    }

    public static VectFunction thinPlateSpline(final int dim)
    {
        Global.assume(dim >= 1 && dim <= 3, "invalid dimension: " + dim);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double radius = input.norm();

                if (dim == 1)
                {
                    output.set(0, Math.abs(radius * radius * radius));
                }
                else if (dim == 2)
                {
                    output.set(0, -radius * radius * Math.log(radius * radius));
                }
                else if (dim == 3)
                {
                    output.set(0, Math.abs(radius));
                }
                else
                {
                    Logging.error("invalid dimension: " + dim);
                }
            }
        }.init(dim, 1);
    }

    public static VectFunction select(Vect values, String select)
    {
        int size = values.size();
        boolean[] subset = new boolean[size];

        for (int i = 0; i < size; i++)
        {
            subset[i] = true;
        }

        if (values != null)
        {
            List<Double> selection = Lists.newArrayList();

            for (String token : select.split(","))
            {
                selection.add(Double.valueOf(token));
            }

            for (int i = 0; i < size; i++)
            {
                double value = values.get(i);
                boolean found = false;
                for (Double s : selection)
                {
                    if (MathUtils.eq(value, s))
                    {
                        found = true;
                        break;
                    }
                }

                subset[i] = found;
            }
        }

        return subset(subset);
    }

    public static VectFunction subset(int size, String which, String exclude)
    {
        boolean[] subset = new boolean[size];

        for (int i = 0; i < size; i++)
        {
            subset[i] = true;
        }

        if (which != null)
        {
            List<Integer> whichidx = CliUtils.parseWhich(which, size);

            for (int i = 0; i < size; i++)
            {
                if (subset[i] && !whichidx.contains(i))
                {
                    subset[i] = false;
                }
            }
        }

        if (exclude != null)
        {
            for (int i : CliUtils.parseWhich(exclude))
            {
                subset[i] = false;
            }
        }

        return subset(subset);
    }

    public static VectFunction subset(boolean[] subset)
    {
        int indim = subset.length;
        int outdim = 0;

        for (boolean s : subset)
        {
            outdim += s ? 1 : 0;
        }

        Global.assume(outdim > 0, "invalid subjset");

        final int foutdim = outdim;
        final int[] foutidx = new int[outdim];
        {
            int idx = 0;
            for (int i = 0; i < indim; i++)
            {
                if (subset[i])
                {
                    foutidx[idx] = i;
                    idx += 1;
                }
            }
        }

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                for (int i = 0; i < foutdim; i++)
                {
                    output.set(i, input.get(foutidx[i]));
                }
            }
        }.init(indim, outdim);
    }
}
