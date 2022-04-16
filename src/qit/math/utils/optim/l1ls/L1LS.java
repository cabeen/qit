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
// import no.uib.cipr.matrix.sparse.BiCGstab;
// import no.uib.cipr.matrix.sparse.CompRowMatrix;
// import no.uib.cipr.matrix.sparse.ILU;
// import no.uib.cipr.matrix.sparse.IterativeSolver;
// import no.uib.cipr.matrix.sparse.IterativeSolverNotConvergedException;
// import no.uib.cipr.matrix.sparse.Preconditioner;
// import qit.data.datasets.Matrix;
// import qit.data.utils.MatrixUtils;
//
// /*
//  * Downloaded from: http://stanford.edu/~boyd/l1_ls/
//  *
//  * Adapted by Ryan Cabeen 2014.03.20
//  */
// public class L1LS extends L1LSBase
// {
//     public L1LS(double[][] A)
//     {
//         this(new Matrix(A));
//     }
//
//     public L1LS(Matrix A)
//     {
//         super(A);
//         Matrix bigA = new Matrix(2 * this.n, 2 * this.n);
//         bigA.set(0, this.n - 1, 0, this.n - 1, this.AtA2);
//         this.cgA = new DenseMatrix(bigA.toArray());
//         int[][] nz = new int[2 * this.n][2];
//         for (int i = 0; i < 2 * this.n; i++)
//         {
//             nz[i][0] = i;
//             nz[i][1] = (i + this.n) % (2 * this.n);
//         }
//         this.pre = new CompRowMatrix(2 * this.n, 2 * this.n, nz);
//     }
//
//     public boolean solve(Matrix y, double lambda, double reltol, double maxError, boolean allowRestart)
//     {
//         this.x = new Matrix(this.n, 1);
//         Matrix u = new Matrix(this.n, 1, 1);
//         Matrix xu = MatrixUtils.catrows(this.x, u);
//         Matrix f = MatrixUtils.catrows(this.x.minus(u), this.x.times(-1).minus(u)); // f
//                                                                                   // =
//                                                                                   // [x-u;-x-u];
//         double t0 = Math.min(Math.max(1, 1 / lambda), this.n / 1e-3);
//         double t = t0;
//
//         if (this.verbose)
//         {
//             System.out.println(this.getClass().getCanonicalName() + "\t" + "Solving a problem of size (m=" + this.m + ", n=" + this.n + "), with lambda="
//                     + lambda);
//             System.out.println(this.getClass().getCanonicalName() + "\t" + "-----------------------------------------------------------------------------");
//             System.out.println(this.getClass().getCanonicalName() + "\t" + "iter\tgap\tprimobj\tdualobj\tstep\tlen\tpcg\titers");
//         }
//         double pobj = Double.POSITIVE_INFINITY;
//         double dobj = Double.NEGATIVE_INFINITY;
//         double s = Double.POSITIVE_INFINITY;
//         double pitr = 0;
//         int ntiter = 0;
//         int lsiter = 0;
//         Matrix dxu = new Matrix(2 * this.n, 1);
//         Matrix dx = this.x.copy();
//         Matrix du = this.x.copy();
//
//         for (ntiter = 0; ntiter <= this.MAX_NT_ITER; ntiter++)
//         {
//             // %------------------------------------------------------------
//             // % CALCULATE DUALITY GAP
//             // %------------------------------------------------------------
//
//             Matrix z = this.A.times(this.x).minus(y); // z = A*x-y;
//             Matrix nu = z.times(2); // nu = 2*z;
//
//             double maxAnu = this.At.times(nu).normInf();
//             if (maxAnu > lambda)
//             {
//                 nu = nu.times(lambda / maxAnu);
//             }
//
//             pobj = MatrixUtils.dot(z, z) + lambda * MatrixUtils.colSumNorm(this.x);
//             dobj = Math.max(MatrixUtils.dot(nu, nu) * -.25 - MatrixUtils.dot(nu, y), dobj); // dobj
//                                                                                         // =
//                                                                                         // max(-0.25*nu'*nu-nu'*y,dobj);
//             double gap = pobj - dobj;
//             if (this.verbose)
//             {
//                 System.out.println(this.getClass().getCanonicalName() + "\t" + ntiter + "\t" + gap + "\t" + pobj + "\t" + dobj + "\t" + s + "\t" + pitr);
//             }
//
//             // %------------------------------------------------------------
//             // % STOPPING CRITERION
//             // %------------------------------------------------------------
//
//             if (gap / Math.abs(dobj) < reltol)
//             {
//                 if (this.verbose)
//                 {
//                     System.out.println(this.getClass().getCanonicalName() + "\t" + "Converged.");
//                 }
//                 this.statusConverged = true;
//                 return this.statusConverged;
//             }
//
//             // %------------------------------------------------------------
//             // % UPDATE t
//             // %------------------------------------------------------------
//             if (s >= 0.5)
//             {
//                 t = Math.max(Math.min(2 * this.n * this.MU / gap, this.MU * t), t); // t
//                                                                                     // =
//                                                                                     // max(min(n*MU/gap,
//                                                                                     // MU*t),
//                                                                                     // t);
//             }
//
//             // %------------------------------------------------------------
//             // % CALCULATE NEWTON STEP
//             // %------------------------------------------------------------
//
//             Matrix q1 = MatrixUtils.eleminv(u.plus(this.x));
//             Matrix q2 = MatrixUtils.eleminv(u.minus(this.x));
//             Matrix d1 = MatrixUtils.elemtimes(q1, q1).plus(MatrixUtils.elemtimes(q2, q2)).times(1. / t);
//             Matrix d2 = MatrixUtils.elemtimes(q1, q1).minus(MatrixUtils.elemtimes(q2, q2)).times(1. / t);
//
//             // % calculate gradient
//             // gradphi = [At*(z*2)-(q1-q2)/t; lambda*ones(n,1)-(q1+q2)/t];
//             Matrix gradphi = MatrixUtils.catrows(this.At.times(z.times(2)).minus(q1.minus(q2).times(1. / t)), MatrixUtils.plus(q1.plus(q2).times(-1. / t), lambda));
//
//             // [2*A'*A+diag(d1) diag(d2); diag(d2) diag(d1)]
//             for (int i = 0; i < this.n; i++)
//             {
//                 this.cgA.set(i, i, this.AtA2.get(i, i) + d1.get(i, 0));
//                 this.cgA.set(i, i + this.n, d2.get(i, 0));
//                 this.cgA.set(i + this.n, i, d2.get(i, 0));
//                 this.cgA.set(i + this.n, i + this.n, d1.get(i, 0));
//
//                 this.pre.set(i, i, d1.get(i, 0));
//                 this.pre.set(i, i + this.n, d2.get(i, 0));
//                 this.pre.set(i + this.n, i, d2.get(i, 0));
//                 this.pre.set(i + this.n, i + this.n, d1.get(i, 0));
//             }
//
//             DenseVector cgB = new DenseVector(gradphi.times(-1).packColumn().toArray());
//             DenseVector cgX = new DenseVector(xu.packColumn().toArray());
//
//             IterativeSolver solver = new BiCGstab(cgB);
//             Preconditioner M = new ILU(this.pre);
//             M.setMatrix(this.pre);
//             solver.setPreconditioner(M);
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
//             // Writes the data back into dx and du
//             for (int i = 0; i < this.n; i++)
//             {
//                 dx.set(i, 0, cgX.get(i));
//                 du.set(i, 0, cgX.get(i + this.n));
//             }
//
//             // %------------------------------------------------------------
//             // % BACKTRACKING LINE SEARCH
//             // %------------------------------------------------------------
//
//             double phi = MatrixUtils.dot(z, z) + lambda * MatrixUtils.sum(u) - MatrixUtils.sum(MatrixUtils.log(f.times(-1))) / t;
//             s = 1.0;
//             double gdx = MatrixUtils.dot(gradphi, dxu);
//             Matrix newx = null;
//             Matrix newf = null;
//             Matrix newu = null;
//             for (lsiter = 1; lsiter <= this.MAX_LS_ITER; lsiter++)
//             {
//                 newx = this.x.plus(dx.times(s));// newx = x+s*dx;
//                 newu = u.plus(du.times(s));// newu = u+s*du;
//                 newf = MatrixUtils.catrows(newx.minus(newu), newx.times(-1).minus(newu)); // f
//                                                                                         // =
//                                                                                         // [x-u;-x-u];
//                 if (MatrixUtils.max(newf) < 0)
//                 {
//                     Matrix newz = this.A.times(newx).minus(y);
//                     double newphi = MatrixUtils.dot(newz, newz) + lambda * MatrixUtils.sum(newu) - MatrixUtils.sum(MatrixUtils.log(newf.times(-1))) / t;
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
//                 break; // end % exit by BLS
//             }
//
//             this.x = newx;
//             f = newf;
//             u = newu;
//         }
//
//         this.statusConverged = false;
//         if (this.verbose)
//         {
//             if (lsiter > this.MAX_LS_ITER)
//             {
//                 System.out.println(this.getClass().getCanonicalName() + "\t" + "MAX_LS_ITER exceeded in BLS");
//             }
//             else if (ntiter > this.MAX_NT_ITER)
//             {
//                 System.out.println(this.getClass().getCanonicalName() + "\t" + "MAX_NT_ITER exceeded.");
//
//             }
//             else
//             {
//                 System.out.println(this.getClass().getCanonicalName() + "\t" + "Unknown failure result.");
//             }
//         }
//         return false;
//     }
// }
