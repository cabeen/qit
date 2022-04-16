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

package qit.data.utils.mri.fitting;

import imaging.DW_Scheme;
import inverters.DiffusionInversion;
import inverters.LinearDT_Inversion;
import misc.DT;
import misc.LoggedException;
import optimizers.MarquardtChiSqFitter;
import optimizers.MarquardtMinimiser;
import optimizers.MarquardtMinimiserException;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.VectUtils;
import qit.data.utils.mri.CaminoUtils;
import qit.data.models.Fibers;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

public class FitFibersCaminoNLLS extends VectFunction
{
    // non-linear fitting of fibers.  this only seems to work for single fibers though, probably due to local minima

    private Gradients gradients;
    private VectFunction output;
    private int comps = 2;
    private int maxtry = 10;
    private int maxrun = 5;
    private int maxiter = 500;

    public FitFibersCaminoNLLS withGradients(Gradients g)
    {
        this.gradients = g;
        this.output = null;

        return this;
    }

    public FitFibersCaminoNLLS withComps(int n)
    {
        this.comps = n;
        this.output = null;
        return this;
    }

    public int getComps()
    {
        return this.comps;
    }

    public FitFibersCaminoNLLS run()
    {
        MarquardtMinimiser.setMAXITER(this.maxiter);

        this.output = new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                DW_Scheme scheme = CaminoUtils.scheme(FitFibersCaminoNLLS.this.gradients);
                double[] voxel = input.toArray();

                double minerr = Double.MAX_VALUE;
                Vect minout = null;

                for (int i = 0; i < FitFibersCaminoNLLS.this.maxrun; i++)
                {
                    double[] soln = null;
                    int code = 1;
                    int numtry = 1;
                    while (numtry++ <= FitFibersCaminoNLLS.this.maxtry && code != 0)
                    {
                        // Logging.info("running try " + numtry);
                        soln = fit(scheme, FitFibersCaminoNLLS.this.comps, voxel);
                        code = (int) soln[0];
                    }

                    if (soln == null)
                    {
                        throw new RuntimeException("bug when checking nlls solution");
                    }

                    double lns0 = soln[1];
                    double s0 = Math.exp(lns0);
                    double d = soln[2];
                    double f0 = soln[3];
                    Vect fracs = new Vect(FitFibersCaminoNLLS.this.comps);
                    Vects lines = new Vects();
                    for (int j = 0; j < FitFibersCaminoNLLS.this.comps; j++)
                    {
                        int idx = 4 + 4 * j;
                        double f = soln[idx + 0];
                        double x = soln[idx + 1];
                        double y = soln[idx + 2];
                        double z = soln[idx + 3];
                        Vect line = VectSource.create3D(x, y, z);

                        fracs.set(j, f);
                        lines.add(line);
                    }

                    if (!MathUtils.zero(code))
                    {
                        // Logging.info("warning: failed to fit ball and sticks");
                        output.set(new Fibers(FitFibersCaminoNLLS.this.comps).getEncoding());
                    }
                    else
                    {
                        for (Vect line : lines)
                        {
                            if (!MathUtils.unit(line.norm()))
                            {
                                throw new RuntimeException("invalid stick orientation");
                            }
                        }

                        if (!MathUtils.unit(f0 + fracs.sum()))
                        {
                            throw new RuntimeException("invalid volume fractions");
                        }

                        Fibers fibers = new Fibers(FitFibersCaminoNLLS.this.comps);
                        fibers.setBaseline(s0);
                        fibers.setDiffusivity(d);

                        int[] perm = VectUtils.permutation(fracs);
                        for (int j = 0; j < FitFibersCaminoNLLS.this.comps; j++)
                        {
                            int idx = perm[perm.length - j - 1];
                            fibers.setFrac(j, fracs.get(idx));
                            fibers.setLine(j, lines.get(idx));
                        }

                        double rmse = Fibers.rmse(input, FitFibersCaminoNLLS.this.gradients, fibers);

                        if (rmse < minerr)
                        {
                            minerr = rmse;
                            minout = fibers.getEncoding();
                        }
                    }
                }

                if (minout != null)
                {
                    output.set(minout);
                }
            }
        }.init(this.gradients.size(), new Fibers(this.comps).getEncodingSize());

        return this;
    }

    public VectFunction getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }

    private static double[] fit(final DW_Scheme scheme, final int comps, final double[] voxel)
    {
        return new BallMultiStickInversion(scheme, comps).invert(voxel);
    }

    private static class BallMultiStickInversion extends DiffusionInversion
    {
        private BallMultiStickFitter fitter;
        private LinearDT_Inversion inv;
        private int comps;

        public BallMultiStickInversion(DW_Scheme ip, int comps)
        {
            this.comps = comps;
            this.inv = new LinearDT_Inversion(ip);
            this.ip = ip;

            try
            {
                this.fitter = new BallMultiStickFitter(ip, this.comps);
            }
            catch (Exception e)
            {
                throw new LoggedException(e);
            }
        }

        /* return: [status, ln(S_0), d, f0, f1, x1, y1, z1, ..., fN, xN, yN, zN] */
        public double[] invert(double[] data)
        {
            double exitCode = 0.0;

            // quick check for bad data
            for (int i = 0; i < data.length; i++)
            {
                if (!(data[i] > 0.0))
                {
                    exitCode = 6.0;
                }
            }

            try
            {
                this.fitter.update(data);
                this.fitter.minimise();
            }
            catch (Exception e)
            {
                // Code 2 indicates single tensor fit being used
                exitCode = 2.0;

                double[] res = new double[this.itemsPerVoxel()];
                double[] comps = this.inv.invert(data);
                DT dt = new DT(comps[2], comps[3], comps[4], comps[5], comps[6], comps[7]);
                double lns0 = comps[1];
                double[][] seig = dt.sortedEigenSystem();
                double fa = dt.fa();
                double md = dt.trace() / 3.0;
                double ux = seig[1][0];
                double uy = seig[2][0];
                double uz = seig[3][0];

                double f1 = fa > 0.1 && fa < 0.9 ? fa : 0.8;
                double f0 = 1 - f1;
                double d = md;

                res[0] = exitCode;
                res[1] = lns0;
                res[2] = d;
                res[3] = f0;
                res[4] = f1;
                res[5] = ux;
                res[6] = uy;
                res[7] = uz;

                for (int i = 8; i < res.length; i++)
                {
                    res[i] = 0;
                }

                return res;
            }

            // [0.0, ln(S_0), d, f0, f1, theta1, phi1, ..., fN, thetaN, phiN]
            double[] params = this.fitter.getParameters();

            double lnS0 = params[1];
            double d = params[2];
            double f0 = params[3];

            double[] res = new double[this.itemsPerVoxel()];
            res[0] = exitCode;
            res[1] = lnS0;
            res[2] = d;
            res[3] = f0;

            for (int i = 0; i < this.comps; i++)
            {
                int pidx = 4 + 3 * i;
                double frac = params[pidx + 0];
                double theta = params[pidx + 1];
                double phi = params[pidx + 2];

                double cosT = Math.cos(theta);
                double sinT = Math.sin(theta);
                double cosP = Math.cos(phi);
                double sinP = Math.sin(phi);

                double x = sinT * cosP;
                double y = sinT * sinP;
                double z = cosT;

                int ridx = 4 + 4 * i;
                res[ridx + 0] = frac;
                res[ridx + 1] = x;
                res[ridx + 2] = y;
                res[ridx + 3] = z;
            }

            return res;
        }

        public int itemsPerVoxel()
        {
            return 4 + 4 * this.comps;
        }
    }

    public static class BallMultiStickFitter extends MarquardtChiSqFitter
    {
        private LinearDT_Inversion inv;
        protected double[] bvals;
        protected int params;
        protected int comps;

        public BallMultiStickFitter(DW_Scheme ip, int comps) throws MarquardtMinimiserException
        {
            this.comps = comps;
            this.params = 3 + 3 * comps; // note: the parameter array has one
                                         // more element

            int numMeas = ip.numMeasurements();

            double[][] g = new double[numMeas][];
            double[] b = new double[numMeas];

            for (int i = 0; i < numMeas; i++)
            {
                g[i] = ip.getG_Dir(i);
                b[i] = ip.getB_Value(i);
            }

            this.inv = new LinearDT_Inversion(ip);
            this.bvals = new double[b.length + 1];

            for (int i = 0; i < b.length; i++)
            {
                this.bvals[i + 1] = b[i];
            }

            double[] voxel = new double[b.length];
            super.initData(g, voxel, this.params);
        }

        public void update(double[] voxel) throws MarquardtMinimiserException
        {
            if (voxel.length != this.y.length - 1)
            {
                throw new MarquardtMinimiserException("New data contains the wrong number of values.");
            }
            for (int i = 0; i < this.ndata; i++)
            {
                this.y[i + 1] = voxel[i];
            }

            this.initASigs();
        }

        public void setInitParams(double[] aInit) throws MarquardtMinimiserException
        {
            // @TODO: update this method to multi-fibers
            throw new RuntimeException("initialization not implemented (how does this work?)");
        }

        public double[] getParameters()
        {
            double code = 0;
            double lnS0 = Math.log(this.a[1] * this.a[1]);
            double d = this.a[2] * this.a[2];
            double ef0hat = Math.exp(this.a[3]);

            double[] out = new double[this.params + 1];
            out[0] = code;
            out[1] = lnS0;
            out[2] = d;

            Vect efhats = new Vect(this.comps);
            for (int i = 0; i < this.comps; i++)
            {
                efhats.set(i, Math.exp(this.a[4 + 3 * i]));
            }

            double fsum = efhats.sum() + ef0hat;
            double f0 = ef0hat / fsum;
            Vect fracs = efhats.times(1.0 / fsum);

            out[3] = f0;

            for (int j = 0; j < this.comps; j++)
            {
                int idx = 4 + 3 * j;

                double f = fracs.get(j);
                double theta = this.a[idx + 1];
                double phi = this.a[idx + 2];

                out[idx + 0] = f;
                out[idx + 1] = theta;
                out[idx + 2] = phi;
            }

            return out;
        }

        protected void initASigs()
        {
            double[] data = new double[this.ndata];

            for (int i = 0; i < this.ndata; i++)
            {
                data[i] = this.y[i + 1];
            }

            double[] params = this.inv.invert(data);

            DT dt = new DT(params[2], params[3], params[4], params[5], params[6], params[7]);

            double s0 = Math.exp(params[1]);
            double fa = dt.fa();
            double md = dt.trace() / 3.0;
            double[][] seig = dt.sortedEigenSystem();
            double pdx = seig[1][0];
            double pdy = seig[2][0];
            double pdz = seig[3][0];
            double frac = fa < 0.1 || fa > 0.9 ? 0.8 / this.comps : fa / this.comps;

            double s0hat = Math.sqrt(s0);
            double dhat = Math.sqrt(md);
            double f0hat = Math.log(1 - frac * this.comps);

            this.a[1] = s0hat;
            this.a[2] = dhat;
            this.a[3] = f0hat;

            for (int i = 0; i < this.comps; i++)
            {
                int idx = 4 + 3 * i;

                double logf = Math.log(frac);
                double theta;
                double phi;
                if (i == 0)
                {
                    theta = Math.acos(pdz);
                    phi = Math.atan2(pdy, pdx);
                }
                else
                {
                    Vect u = VectSource.randomUnit();
                    double ux = u.get(0);
                    double uy = u.get(1);
                    double uz = u.get(2);
                    theta = Math.acos(uz);
                    phi = Math.atan2(uy, ux);
                }

                this.a[idx + 0] = logf;
                this.a[idx + 1] = theta;
                this.a[idx + 2] = phi;
            }
        }

        protected double yfit(double[] atry, int i)
        {
            double s0hat = atry[1];
            double dhat = atry[2];
            double ef0hat = Math.exp(atry[3]);
            Vect efhats = new Vect(this.comps);
            Vects lines = new Vects();

            for (int j = 0; j < this.comps; j++)
            {
                int idx = 4 + 3 * j;
                double efhat = Math.exp(atry[idx + 0]);
                double theta = atry[idx + 1];
                double phi = atry[idx + 2];

                double sinT = Math.sin(theta);
                double cosT = Math.cos(theta);
                double sinP = Math.sin(phi);
                double cosP = Math.cos(phi);

                double x = sinT * cosP;
                double y = sinT * sinP;
                double z = cosT;
                Vect line = VectSource.create3D(x, y, z);

                efhats.set(j, efhat);
                lines.add(line);
            }

            double efsum = efhats.sum() + ef0hat;
            double f0 = ef0hat / efsum;
            Vect fracs = efhats.times(1.0 / efsum);

            double s0 = s0hat * s0hat;
            double d = dhat * dhat;

            double b = this.bvals[i];
            double gx = this.x[i][0];
            double gy = this.x[i][1];
            double gz = this.x[i][2];
            Vect g = VectSource.create3D(gx, gy, gz);

            double s = 0;

            // add ball compartment
            s += f0 * Math.exp(-b * d);

            for (int j = 0; j < this.comps; j++)
            {
                double f = fracs.get(j);
                Vect u = lines.get(j);
                double gu = g.dot(u);
                double gu2 = gu * gu;

                // add stick
                s += f * Math.exp(-b * d * gu2);
            }

            // multiple by baseline signal
            s *= s0;

            return s;
        }

        protected double[] dydas_analytic(double[] atry, int i)
        {
            double s0hat = atry[1];
            double dhat = atry[2];
            double ef0hat = Math.exp(atry[3]);

            Vect efhats = new Vect(this.comps);
            Vect thetas = new Vect(this.comps);
            Vect phis = new Vect(this.comps);
            Vects lines = new Vects();

            for (int j = 0; j < this.comps; j++)
            {
                int idx = 4 + 3 * j;
                double efhat = Math.exp(atry[idx + 0]);
                double theta = atry[idx + 1];
                double phi = atry[idx + 2];

                double sinT = Math.sin(theta);
                double cosT = Math.cos(theta);
                double sinP = Math.sin(phi);
                double cosP = Math.cos(phi);

                double x = sinT * cosP;
                double y = sinT * sinP;
                double z = cosT;
                Vect line = VectSource.create3D(x, y, z);

                thetas.set(j, theta);
                phis.set(j, phi);
                efhats.set(j, efhat);
                lines.add(line);
            }

            double efsum = efhats.sum() + ef0hat;
            double f0 = ef0hat / efsum;
            Vect fracs = efhats.times(1.0 / efsum);

            double s0 = s0hat * s0hat;
            double d = dhat * dhat;

            double b = this.bvals[i];
            double gx = this.x[i][0];
            double gy = this.x[i][1];
            double gz = this.x[i][2];
            Vect g = VectSource.create3D(gx, gy, gz);

            double[][] dfdfhat = new double[this.comps + 1][this.comps + 1];
            double efsum2 = efsum * efsum;
            for (int top = 0; top <= this.comps; top++)
            {
                double eftop = top == 0 ? ef0hat : efhats.get(top - 1);
                for (int bottom = 0; bottom <= this.comps; bottom++)
                {
                    double efbottom = bottom == 0 ? ef0hat : efhats.get(bottom - 1);

                    double v = 0;
                    if (top == bottom)
                    {
                        v += efsum * efbottom;
                    }
                    v -= eftop * efbottom;
                    v /= efsum2;

                    dfdfhat[top][bottom] = v;
                }
            }

            double ball = Math.exp(-b * d);
            Vect gus = new Vect(this.comps);
            Vect gu2s = new Vect(this.comps);
            Vect sticks = new Vect(this.comps);
            for (int j = 0; j < this.comps; j++)
            {
                Vect u = lines.get(j);
                double gu = g.dot(u);
                double gu2 = gu * gu;
                double stick = Math.exp(-b * d * gu2);

                gus.set(j, gu);
                gu2s.set(j, gu2);
                sticks.set(j, stick);
            }

            double[] dyda = new double[this.params + 1];

            {
                double dshat = -2 * s0hat;

                double dyds = 0;
                dyds += f0 * ball;
                for (int j = 0; j < this.comps; j++)
                {
                    dyds += fracs.get(j) * sticks.get(j);
                }
                dyds *= dshat;
                dyda[1] = dyds;
            }

            {
                double ddhat = 2 * dhat;

                double dydd = 0;
                dydd += f0 * ball * -b;
                for (int j = 0; j < this.comps; j++)
                {
                    dydd += fracs.get(j) * sticks.get(j) * (-b * gu2s.get(j));
                }
                dydd *= s0;
                dydd *= ddhat;

                dyda[2] = dydd;
            }

            for (int k = 0; k <= this.comps; k++)
            {
                double dsdf = 0;
                dsdf += dfdfhat[0][k] * ball;
                for (int j = 0; j < this.comps; j++)
                {
                    dsdf += dfdfhat[j + 1][k] * sticks.get(j);
                }
                dsdf *= s0;

                int idx = k == 0 ? 3 : 4 + 3 * (k - 1);
                dyda[idx] = dsdf;
            }

            for (int j = 0; j < this.comps; j++)
            {
                double theta = thetas.get(j);
                double phi = phis.get(j);

                double sinT = Math.sin(theta);
                double cosT = Math.cos(theta);
                double sinP = Math.sin(phi);
                double cosP = Math.cos(phi);

                double gu = gus.get(j);
                double frac = fracs.get(j);
                double stick = sticks.get(j);

                {
                    double gdudt = gx * cosT * cosP + gy * cosT * sinP - gz * sinT;
                    double dsdt = s0 * frac * stick * (-2 * d * b * gu) * gdudt;
                    dyda[4 + 3 * j + 1] = dsdt;
                }
                {
                    double gdudp = -gx * sinT * sinP + gy * sinT * cosP;
                    double dsdp = s0 * frac * stick * (-2 * d * b * gu) * gdudp;
                    dyda[4 + 3 * j + 2] = dsdp;
                }
            }

            return dyda;
        }

        protected double[] dydas_approximate(double[] atry, int i)
        {
            double delta = 1e-6;

            double[] dyda = new double[this.params + 1];
            for (int j = 0; j < this.params; j++)
            {
                double[] next = MathUtils.copy(atry);
                next[j + 1] += delta;
                double fnext = this.yfit(next, i);

                double[] prev = MathUtils.copy(atry);
                prev[j + 1] -= delta;
                double fprev = this.yfit(prev, i);

                double fprime = (fnext - fprev) / (2.0 * delta);
                dyda[j + 1] = fprime;
            }

            return dyda;
        }

        protected double[] dydas(double[] atry, int i)
        {
            return this.dydas_analytic(atry, i);
        }
    }
}
