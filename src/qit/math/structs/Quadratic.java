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

package qit.math.structs;

import qit.base.Global;
import qit.base.JsonDataset;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;

public class Quadratic extends JsonDataset
{
    // Model the quadratic: a * x ^2 + b * x * y + c * y ^2 + d * x + e * y = z
    private double a;
    private double b;
    private double c;
    private double d;
    private double e;

    public Quadratic(double a, double b, double c, double d, double e)
    {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
    }

    public double getA()
    {
        return this.a;
    }

    public double getB()
    {
        return this.b;
    }

    public double getC()
    {
        return this.c;
    }

    public double getD()
    {
        return this.d;
    }

    public double getE()
    {
        return this.e;
    }

    public static Quadratic fit(Vects vs)
    {
        Global.assume(vs.size() >= 3, "Cannot fit simple quadratic to fewer than three points");
        Global.assume(vs.getDim() == 3, "Vects must be three dimensional");

        // Find the simple quadratic the least-squares best-fit
        // Find Q = argmin<sub>Q</sub>|| A * Q - B||
        // where Q = (a, b, c, d, e), a * x ^ 2 + b * x * y + c * y ^ 2 + d * x
        // + e * y = 0,
        // A is the matrix of quadratic coordinates (x * x, x * y, y * y, x, y),
        // and B is the matrix of z coordinates.

        int N = vs.size();
        Matrix A = new Matrix(N, 5);
        Matrix B = new Matrix(N, 1);

        int i = 0;
        for (Vect v : vs)
        {
            A.set(i, 0, v.get(0) * v.get(0));
            A.set(i, 1, v.get(1) * v.get(0));
            A.set(i, 2, v.get(1) * v.get(1));
            A.set(i, 3, v.get(0));
            A.set(i, 4, v.get(1));
            B.set(i, 0, v.get(2));
            i++;
        }

        Matrix Q = A.inv().times(B);
        double a = Q.get(0, 0);
        double b = Q.get(1, 0);
        double c = Q.get(2, 0);
        double d = Q.get(3, 0);
        double e = Q.get(4, 0);

        return new Quadratic(a, b, c, d, e);
    }

    public Quadratic copy()
    {
        return new Quadratic(this.a, this.b, this.c, this.d, this.e);
    }
}
