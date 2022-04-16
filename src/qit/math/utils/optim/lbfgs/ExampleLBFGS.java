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

/* copyright 2002 by Robert Dodier
 *
 * This program is free software; you can redistribute it and/or modify
 * it under either option (1) or (2) below.
 * (1) The GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * (2) The Apache license, version 2.
 */

package qit.math.utils.optim.lbfgs;

public class ExampleLBFGS
{
    public static void main(String args[])
    {
        int ndim = 2000;

        double[] x = new double[ndim];
        double[] g = new double[ndim];
        double[] diag = new double[ndim];

        int[] iprint = { 0, 0 };
        int[] iflag = { 0 };

        boolean diagco = false;
        int n = 100;
        int m = 7;
        int icall = 0;
        float eps = 1.0e-5f;
        float xtol = 1.0e-16f;

        for (int j = 0; j < n; j += 2)
        {
            x[j] = -1.2e0;
            x[j + 1] = 1.e0;
        }

        do
        {
            double f = 0;
            for (int j = 0; j < n; j += 2)
            {
                double t1 = 1.e0 - x[j];
                double t2 = 1.e1 * (x[j + 1] - x[j] * x[j]);
                g[j + 1] = 2.e1 * t2;
                g[j] = -2.e0 * (x[j] * g[j + 1] + t1);
                f = f + t1 * t1 + t2 * t2;
            }

            try
            {
                LBFGS.lbfgs(n, m, x, f, g, diagco, diag, iprint, eps, xtol, iflag);
            }
            catch (LBFGS.ExceptionWithIflag e)
            {
                System.err.println("Sdrive: lbfgs failed.\n" + e);
                return;
            }

            icall += 1;
        }
        while (iflag[0] != 0 && icall <= 200);
    }
}
