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
// import no.uib.cipr.matrix.sparse.CompRowMatrix;
// import qit.data.datasets.Matrix;
//
// /* Derived from the ls_l1 Matlab package.
//  * This port is made available under GPL (as opposed to LGPL (for the rest of the package).
//  *
//  * Adapted by Ryan Cabeen <cabeen@gmail.com>
//  * 2014.03.20
//  * Downloaded from: http://stanford.edu/~boyd/l1_ls/
//  *
//  * Adapted by Bennett Landman <landman@jhu.edu> and Hanlin Wan <hwan1@jhu.edu>
//  * 2009.07.09
//  *
//  % AUTHOR    Kwangmoo Koh <deneb1@stanford.edu>
//  % UPDATE    Apr 8 2007
//  %
//  % COPYRIGHT 2008 Kwangmoo Koh, Seung-Jean Kim, and Stephen Boyd
//  %
//  % l1-Regularized Least Squares Problem Solver
//  %
//  %   l1_ls solves problems of the following form:
//  %
//  %       minimize ||A*x-y||^2 + lambda*sum|x_i|,
//  %
//  %   where A and y are problem data and x is variable (described below).
//
//  % AUTHOR    Kwangmoo Koh <deneb1@stanford.edu>
//  % UPDATE    Apr 10 2008
//  %
//  % COPYRIGHT 2008 Kwangmoo Koh, Seung-Jean Kim, and Stephen Boyd
//  %
//  % l1-Regularized Least Squares Problem Solver
//  %
//  %   l1_ls solves problems of the following form:
//  %
//  %       minimize   ||A*x-y||^2 + lambda*sum(x_i),
//  %       subject to x_i >= 0, i=1,...,n
//  %
//  %   where A and y are problem data and x is variable (described below).
//  %   x       : n vector; classifier
//  */
//
// public abstract class L1LSBase
// {
//
//     // % IPM PARAMETERS
//     protected double MU = 2; // updating parameter of t
//     protected double MAX_NT_ITER = 100; // maxima IPM (Newton) iteration
//
//     // % LINE SEARCH PARAMETERS
//     protected double ALPHA = 0.01; // minima fraction of decrease in the
//                                    // objective
//     protected double BETA = 0.5; // stepsize decrease factor
//     protected double MAX_LS_ITER = 100; // maxima backtracking line search
//                                         // iteration
//
//     protected Matrix A;
//     protected Matrix At;
//     protected Matrix AtA2;
//     protected Matrix x; // the result
//     protected int m; // % m : number of examples (rows) of A
//     protected int n; // % n : number of features (column)s of A
//     protected DenseMatrix cgA;
//     protected CompRowMatrix pre;
//
//     protected boolean statusConverged = false;
//     protected boolean verbose = false;
//
//     public L1LSBase(double[][] A)
//     {
//         this(new Matrix(A));
//     }
//
//     public double findLambdaMax(double[] y)
//     {
//         double[][] y1 = new double[1][];
//         y1[0] = y;
//         Matrix m = this.At.times(new Matrix(y1).transpose());
//         return m.normInf() * 2;
//     }
//
//     public L1LSBase(Matrix A)
//     {
//         this.A = A;
//         this.At = this.A.transpose();
//         this.AtA2 = this.At.times(this.A).times(2);
//         this.n = this.A.cols();
//         this.m = this.A.rows();
//         this.statusConverged = false;
//         this.verbose = false;
//     }
//
//     public boolean solve(double[] y, double lambda)
//     {
//         double[][] y1 = new double[1][];
//         y1[0] = y;
//         return this.solve(new Matrix(y1).transpose(), lambda);
//     }
//
//     public boolean solve(double[] y, double lambda, double reltol)
//     {
//         return this.solve(y, lambda, reltol, -1);
//     }
//
//     public boolean solve(double[] y, double lambda, double reltol, double maxError)
//     {
//         return this.solve(y, lambda, reltol, maxError, false);
//     }
//
//     public boolean solve(double[] y, double lambda, double reltol, double maxError, boolean allowRestart)
//     {
//         double[][] y1 = new double[1][];
//         y1[0] = y;
//         return this.solve(new Matrix(y1).transpose(), lambda, reltol, maxError, allowRestart);
//     }
//
//     public boolean solve(Matrix y, double lambda)
//     {
//         return this.solve(y, lambda, 1e-3, 10, false);
//     }
//
//     public abstract boolean solve(Matrix y, double lambda, double reltol, double maxError, boolean allowRestart);
//
//     public boolean isConvereged()
//     {
//         return this.statusConverged;
//     }
//
//     public void setVerbose(boolean verbose)
//     {
//         this.verbose = verbose;
//     }
//
//     public Matrix getMatrixResult()
//     {
//         if (this.x == null)
//         {
//             return null;
//         }
//         return this.x.copy();
//     }
//
//     public double[] getResult()
//     {
//         double[] res = new double[this.x.rows()];
//         for (int i = 0; i < this.x.rows(); i++)
//         {
//             res[i] = this.x.get(i, 0);
//         }
//         return res;
//     }
// }
