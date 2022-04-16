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

package qit.math.utils.optim.l1ls;

// This requires this package: https://github.com/fommil/matrix-toolkits-java
// but it is quite large, so let's not include it in QIT until we need it
//
// import no.uib.cipr.matrix.DenseMatrix;
// import no.uib.cipr.matrix.DenseVector;
// import no.uib.cipr.matrix.sparse.CG;
// import no.uib.cipr.matrix.sparse.CompRowMatrix;
// import no.uib.cipr.matrix.sparse.DefaultIterationMonitor;
// import no.uib.cipr.matrix.sparse.DiagonalPreconditioner;
// import no.uib.cipr.matrix.sparse.IterativeSolver;
// import no.uib.cipr.matrix.sparse.IterativeSolverNotConvergedException;
// import no.uib.cipr.matrix.sparse.Preconditioner;
// import qit.base.Logging;
// import qit.data.datasets.Matrix;
// import qit.data.utils.MatrixUtils;
// import qit.math.utils.MathUtils;
//
// /**
//  * Positively constrained least squares solver
//  *
//  * @author Hanlin Wan <hanlinwan@gmail.com>
//  *
//  *         Adapted by Ryan Cabeen 2014.03.20
//  */
// public class L1NNLS extends L1LSBase
// {
//     public L1NNLS(double[][] A)
//     {
//         this(new Matrix(A));
//     }
//
//     public double findLambdaMax(double[] y)
//     {
//         double[][] y1 = new double[1][];
//         y1[0] = y;
//         Matrix m = this.At.times(new Matrix(y1).transpose());
//         double mx = Double.NEGATIVE_INFINITY;
//         for (int i = 0; i < m.rows(); i++)
//         {
//             if (mx < m.get(i, 0))
//             {
//                 mx = m.get(i, 0);
//             }
//         }
//         return 2 * mx;
//     }
//
//     public L1NNLS(Matrix A)
//     {
//         super(A);
//         this.cgA = new DenseMatrix(this.AtA2.toArray());
//         int[][] nz = new int[this.n][1];
//         for (int i = 0; i < this.n; i++)
//         {
//             nz[i][0] = i;
//         }
//         this.pre = new CompRowMatrix(this.n, this.n, nz);
//     }
//
//     public boolean solve(Matrix y, double lambda, double reltol, double maxError, boolean allowRestart)
//     {
//         this.x = new Matrix(this.n, 1, 0.01);
//         if (allowRestart)
//         {
//             this.MAX_LS_ITER = 20;
//         }
//         double t0 = Math.min(Math.max(1, 1 / lambda), this.n / 1e-3);
//         double t = t0;
//
//         if (this.verbose)
//         {
//             Logging.info(this.getClass().getCanonicalName() + "\t" + "Solving a problem of size (m=" + this.m + ", n=" + this.n + "), with lambda=" + lambda);
//             Logging.info(this.getClass().getCanonicalName() + "\t" + "-----------------------------------------------------------------------------");
//             Logging.info(this.getClass().getCanonicalName() + "\t" + "iter\tgap\tprimobj\tdualobj\tstep\tlen\tpcg\titers");
//         }
//         double pobj = Double.POSITIVE_INFINITY;
//         double dobj = Double.NEGATIVE_INFINITY;
//         double s = Double.POSITIVE_INFINITY;
//         double pitr = 0;
//         int ntiter = 0;
//         int lsiter = 0;
//         int errorCount = 0;
//         double prevError = -1;
//         double minAnu, gap, relError, phi, newphi;
//
//         Matrix dx = new Matrix(this.n, 1);
//         Matrix z = this.A.times(this.x).minus(y); // z = A*x-y;
//         Matrix nu, newx = null, newz = null;
//         DenseVector cgX = new DenseVector(this.n);
//         DenseVector cgB = new DenseVector(this.n);
//         IterativeSolver solver = new CG(cgX);
//         if (allowRestart)
//         {
//             solver.setIterationMonitor(new DefaultIterationMonitor(1000, 0.1, 1e-5, 1e3));
//         }
//         Preconditioner M = new DiagonalPreconditioner(this.n);
//         solver.setPreconditioner(M);
//
//         for (ntiter = 0; ntiter <= this.MAX_NT_ITER; ntiter++)
//         {
//
//             nu = z.times(2); // nu = 2*z;
//             Matrix Atz2 = this.At.times(nu);
//             minAnu = MatrixUtils.min(Atz2);
//             if (minAnu < -lambda)
//             {
//                 nu = nu.times(lambda / -minAnu);
//             }
//
//             pobj = MatrixUtils.dot(z, z) + lambda * MatrixUtils.sum(this.x); // pobj
//                                                                          // =
//                                                                          // z'*z+lambda*sum(x,1);
//             dobj = Math.max(MatrixUtils.dot(nu, nu) * -.25 - MatrixUtils.dot(nu, y), dobj); // dobj
//                                                                                         // =
//                                                                                         // max(-0.25*nu'*nu-nu'*y,dobj);
//             gap = pobj - dobj;
//             if (this.verbose)
//             {
//                 Logging.info(this.getClass().getCanonicalName() + "\t" + ntiter + "\t" + gap + "\t" + pobj + "\t" + dobj + "\t" + s + "\t" + pitr);
//             }
//
//             // %------------------------------------------------------------
//             // % STOPPING CRITERION
//             // %------------------------------------------------------------
//             relError = gap / Math.abs(dobj);
//             if (relError < reltol)
//             {
//                 if (this.verbose)
//                 {
//                     Logging.info(this.getClass().getCanonicalName() + "\t" + "Converged.");
//                 }
//                 this.statusConverged = true;
//                 return this.statusConverged;
//             }
//             else if (maxError > 0 && relError > maxError)
//             {
//                 if (this.verbose)
//                 {
//                     Logging.info(this.getClass().getCanonicalName() + "\t" + "Error too high.");
//                 }
//                 if (MathUtils.eq(prevError, -1))
//                 {
//                     errorCount++;
//                     prevError = relError;
//                 }
//                 else
//                 {
//                     if (relError > prevError)
//                     {
//                         errorCount++;
//                         prevError = relError;
//                     }
//                     else
//                     {
//                         errorCount = 0;
//                         prevError = -1;
//                     }
//                 }
//                 if (errorCount == 3)
//                 {
//                     if (this.verbose)
//                     {
//                         System.out.print("FAILED1\t");
//                     }
//                     this.statusConverged = false;
//                     return this.statusConverged;
//                 }
//             }
//             else
//             {
//                 errorCount = 0;
//                 prevError = -1;
//             }
//
//             // %------------------------------------------------------------
//             // % UPDATE t
//             // %------------------------------------------------------------
//             if (s >= 0.5)
//             {
//                 t = Math.max(Math.min(this.n * this.MU / gap, this.MU * t), t); // t
//                                                                                 // =
//                 // max(min(n*MU/gap,
//                 // MU*t), t);
//             }
//
//             // %------------------------------------------------------------
//             // % CALCULATE NEWTON STEP
//             // %------------------------------------------------------------
//             for (int i = 0; i < this.n; i++)
//             {
//                 this.pre.set(i, i, 1 / (this.x.get(i, 0) * this.x.get(i, 0) * t)); // d1
//                                                                                    // =
//                 // (1/t)./(x.^2);
//                 this.cgA.set(i, i, this.AtA2.get(i, i) + this.pre.get(i, i)); // 2*A'*A+diag(d1)
//                 cgB.set(i, -Atz2.get(i, 0) - lambda + 1 / (t * this.x.get(i, 0))); // -gradphi
//                 // =
//                 // -[At*(z*2)+lambda-(1/t)./x];
//             }
//             M.setMatrix(this.pre);
//
//             try
//             {
//                 solver.solve(this.cgA, cgB, cgX);
//             }
//             catch (IterativeSolverNotConvergedException e)
//             {
//                 System.err.println(this.getClass().getCanonicalName() + "Iterative solver failed to converge");
//             }
//
//             // Writes the data back into dx
//             for (int i = 0; i < this.n; i++)
//             {
//                 dx.set(i, 0, cgX.get(i));
//             }
//
//             // %------------------------------------------------------------
//             // % BACKTRACKING LINE SEARCH
//             // %------------------------------------------------------------
//             phi = MatrixUtils.dot(z, z) + lambda * MatrixUtils.sum(this.x) - MatrixUtils.sum(MatrixUtils.log(this.x)) / t;
//             s = 1.0;
//             double gdx = 0;
//             for (int i = 0; i < this.n; i++)
//             {
//                 gdx += this.x.get(i, 0) * cgB.get(i) * -1;
//             }
//             for (lsiter = 1; lsiter <= this.MAX_LS_ITER; lsiter++)
//             {
//                 newx = this.x.plus(dx.times(s)); // newx = x+s*dx;
//                 if (MatrixUtils.min(newx) > 0)
//                 {
//                     newz = this.A.times(newx).minus(y);
//                     newphi = MatrixUtils.dot(newz, newz) + lambda * MatrixUtils.sum(newx) - MatrixUtils.sum(MatrixUtils.log(newx)) / t;
//                     if (newphi - phi <= this.ALPHA * s * gdx)
//                     {
//                         break;
//                     }
//                 }
//                 s = this.BETA * s;
//             }
//
//             if (lsiter > this.MAX_LS_ITER)
//             {
//                 if (allowRestart && ntiter < 5)
//                 { // restart
//                     if (this.verbose)
//                     {
//                         System.out.print("RESTART\t");
//                     }
//                     solver.setIterationMonitor(new DefaultIterationMonitor());
//                     this.x = new Matrix(this.n, 1, 0.01);
//                     z = this.A.times(this.x).minus(y);
//                     s = Double.POSITIVE_INFINITY;
//                     this.MAX_LS_ITER = 100;
//                 }
//                 else
//                 {
//                     break;
//                 }
//             }
//             else
//             {
//                 this.x = newx;
//                 z = newz;
//                 if (allowRestart && ntiter > 5)
//                 {
//                     this.MAX_LS_ITER = 100;
//                 }
//             }
//         }
//
//         this.statusConverged = false;
//         if (this.verbose)
//         {
//             if (lsiter > this.MAX_LS_ITER)
//             {
//                 Logging.info(this.getClass().getCanonicalName() + "\t" + "MAX_LS_ITER exceeded in BLS");
//             }
//             else if (ntiter > this.MAX_NT_ITER)
//             {
//                 Logging.info(this.getClass().getCanonicalName() + "\t" + "MAX_NT_ITER exceeded.");
//
//             }
//             else
//             {
//                 Logging.info(this.getClass().getCanonicalName() + "\t" + "Unknown failure result.");
//             }
//         }
//         if (this.verbose)
//         {
//             System.out.print("FAILED2\t");
//         }
//         return true;
//     }
// }
