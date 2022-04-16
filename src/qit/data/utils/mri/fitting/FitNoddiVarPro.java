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

import qit.base.Global;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.models.Noddi;
import qit.data.models.Tensor;
import qit.data.source.MatrixSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

import java.util.function.Supplier;


/**
 * Golub, Gene, and Victor Pereyra. "Separable nonlinear least squares: the variable projection method and its applications." Inverse problems 19.2 (2003)
 *
 * Farooq, Hamza, et al. "Microstructure Imaging of Crossing (MIX) White Matter Fibers from diffusion MRI." Scientific reports 6 (2016).
 */
public class FitNoddiVarPro implements Supplier<VectFunction>
{
    public static final String NAME = "VarPro";

    public static final double THRESH_LOW = 0.1;
    public static final double THRESH_HIGH = 0.25;

    public static final CostType DEFAULT_COST = CostType.SE;

    public Gradients gradients;
    public CostType cost = DEFAULT_COST;
    public boolean separate = true;
    public boolean linear = true;
    public boolean nonlinear = true;
    public int maxiter = 10;
    public double thresh = 1e-3;
    public double maxden = 0.99;

    public VectFunction get()
    {
        FitTensorLLS lls = new FitTensorLLS();
        lls.gradients = this.gradients;

        final VectFunction tensorFitter = lls.get();

        final VectFunction syntherHigh = Noddi.synth(this.gradients, 0);
        final VectFunction syntherMed = Noddi.synth(this.gradients, 1);
        final VectFunction syntherLow = Noddi.synth(this.gradients, 2);

        final Noddi.Synther syntherBasisHigh = new Noddi.Synther(this.gradients, 0);
        final Noddi.Synther syntherBasisMed = new Noddi.Synther(this.gradients, 1);
        final Noddi.Synther syntherBasisLow = new Noddi.Synther(this.gradients, 2);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                final Vect finput = input;
                final Tensor tensor = new Tensor(tensorFitter.apply(input));
                final double tensorBaseline = ModelUtils.baselineStats(gradients, input).mean;
                final Vect tensorDir = tensor.getVec(0);

                final Noddi best = new Noddi();
                best.setBaseline(tensorBaseline);
                best.setDir(tensorDir);
                best.setODI(0.5);
                best.setFISO(0.5);
                best.setFICVF(0.5);

                Double prevCost = null;

                for (int iter = 0; iter < FitNoddiVarPro.this.maxiter; iter++)
                {
                    // do nonlinear solve for ODI
                    if (FitNoddiVarPro.this.separate)
                    {
                        double[] x = {best.getODI()};

                        int maxiter = 1000;
                        int NDIM = 1;
                        int mdim = 2;
                        int iprint = 0;
                        double rhobeg = 0.5;
                        double rhoend = 1e-8;
                        Calcfc func = new Calcfc()
                        {
                            @Override
                            public double Compute(int n, int m, double[] x, double[] con)
                            {
                                Global.assume(n == NDIM, "invalid value dimension: " + n);
                                Global.assume(m == mdim, "invalid constraint dimension: " + m);

                                double odi = x[0];

                                con[0] = odi;
                                con[1] = 1.0 - odi;

                                try
                                {
                                    Noddi model = best.copy();
                                    model.setODI(odi);
                                    Noddi.Synther synther = odi < THRESH_LOW ? syntherBasisLow : odi < THRESH_HIGH ? syntherBasisMed : syntherBasisHigh;
                                    Matrix basis = synther.basis(model);
                                    Matrix binv = basis.inv();
                                    Matrix bbinv = basis.times(binv);
                                    Matrix ident = MatrixSource.identity(basis.rows());
                                    Matrix imbbinv = ident.minus(bbinv);
                                    Matrix imbinvs = imbbinv.times(MatrixSource.createCol(finput));
                                    double cost = imbinvs.norm2();
                                    return cost;
                                }
                                catch (RuntimeException e)
                                {
                                   return Double.MAX_VALUE;
                                }
                            }
                        };
                        Cobyla.FindMinimum(func, NDIM, mdim, x, rhobeg, rhoend, iprint, maxiter);

                        best.setODI(x[0]);
                    }

                    // do linear solve for fractions and baseline
                    if (FitNoddiVarPro.this.linear)
                    {
                        double odi = best.getODI();
                        Noddi.Synther synther = odi < THRESH_LOW ? syntherBasisLow : odi < THRESH_HIGH ? syntherBasisMed : syntherBasisHigh;
                        Matrix basis = synther.basis(best);

                        // Matrix homsys = basis.catRows(MatrixSource.createCol(input.times(-1)));
                        // SvdDecomp svd = MatrixUtils.svd(homsys);
                        // Vect vals = svd.S.diag();
                        // int idx = VectUtils.permutation(vals)[0];
                        // Vect fracs= svd.V.getColumn(idx);

                        // int dim = basis.cols();
                        // Matrix BT = basis.transpose();
                        // Matrix BTB = BT.times(basis);
                        // Matrix BTBL = BTB.plus(MatrixSource.identity(dim).times(0.000));
                        // Matrix solution = BTBL.times(BT.times(MatrixSource.createCol(input)));
                        // Vect fracs= solution.getColumn(0);

                        try
                        {
                            Matrix solution = MatrixUtils.solve(basis, MatrixSource.createCol(input));
                            Vect fracs = solution.getColumn(0);

                            for (int i = 0; i < fracs.size(); i++)
                            {
                                if (fracs.get(i) < 0)
                                {
                                    fracs.set(i, 0);
                                }
                            }

                            double sum = fracs.sum();

                            if (MathUtils.zero(sum))
                            {
                                throw new RuntimeException();
                            }

                            fracs.timesEquals(1.0 / sum);

                            double fiso = fracs.get(2);
                            double fisoi = 1.0 - fiso;
                            double ficvf = MathUtils.nonzero(fisoi) ? fracs.get(0) / fisoi : 0;
                            ficvf = Math.min(FitNoddiVarPro.this.maxden, ficvf);

                            best.setFICVF(ficvf);
                            best.setFISO(fiso);
                        }
                        catch (RuntimeException e)
                        {
                            best.setFICVF(Global.RANDOM.nextDouble());
                            best.setFISO(Global.RANDOM.nextDouble());
                            best.setODI(Global.RANDOM.nextDouble());
                        }
                    }

                    if (FitNoddiVarPro.this.nonlinear)
                    {
                        double[] x = {best.getFICVF(), best.getFISO(), best.getODI()};

                        int maxiter = 10000;
                        int ndim = 3;
                        int mdim = 7;
                        int iprint = 0;
                        double rhobeg = 0.5;
                        double rhoend = 1e-8;
                        Calcfc func = new Calcfc()
                        {
                            @Override
                            public double Compute(int n, int m, double[] x, double[] con)
                            {
                                Global.assume(n == ndim, "invalid value dimension: " + n);
                                Global.assume(m == mdim, "invalid constraint dimension: " + m);

                                double ficvf = x[0];
                                double fiso = x[1];
                                double odi = x[2];
                                VectFunction synther = odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;

                                Noddi model = best.copy();
                                model.setFICVF(ficvf);
                                model.setFISO(fiso);
                                model.setODI(odi);

                                Vect pred = synther.apply(model.getEncoding());

                                double cost = ModelUtils.cost(FitNoddiVarPro.this.cost, gradients, finput, pred);
                                con[0] = odi;
                                con[1] = 1.0 - odi;
                                con[2] = ficvf;
                                con[3] = FitNoddiVarPro.this.maxden - ficvf;
                                con[4] = fiso;
                                con[5] = 1.0 - fiso;
                                con[6] = 1.0 - fiso - ficvf;

                                return cost;
                            }
                        };
                        Cobyla.FindMinimum(func, ndim, mdim, x, rhobeg, rhoend, iprint, maxiter);

                        best.setFICVF(x[0]);
                        best.setFISO(x[1]);
                        best.setODI(x[2]);
                    }

                    // if we are interating, check for convergence
                    if (FitNoddiVarPro.this.maxiter > 1)
                    {
                        double odi = best.getODI();
                        VectFunction synther = odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;
                        Vect pred = synther.apply(best.getEncoding());
                        double cost = ModelUtils.cost(FitNoddiVarPro.this.cost, gradients, finput, pred);

                        if (prevCost != null)
                        {
                            double change = (prevCost - cost) / prevCost;

                            if (change < FitNoddiVarPro.this.thresh)
                            {
                                break;
                            }
                        }

                        prevCost = cost;
                    }
                }

                output.set(best.getEncoding());
            }
        }.init(this.gradients.size(), new Noddi().getEncodingSize());
    }
}
