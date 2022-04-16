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
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

public class Quaternion extends JsonDataset
{
    // model a implicitly, since |q| = 1
    private final double b;
    private final double c;
    private final double d;

    public Quaternion(double b, double c, double d)
    {
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public Quaternion(Vect axis, double angle)
    {
        Vect norm = axis.normalize();
        this.b = norm.get(0) * Math.sin(angle / 2);
        this.c = norm.get(1) * Math.sin(angle / 2);
        this.d = norm.get(2) * Math.sin(angle / 2);
    }

    public Quaternion(Matrix mat)
    {
        Global.assume(mat.rows() == mat.cols() && mat.rows() == 3, "invalid rotation matrix");

        double R11 = mat.get(0, 0);
        double R22 = mat.get(1, 1);
        double R33 = mat.get(2, 2);
        double R32 = mat.get(2, 1);
        double R23 = mat.get(1, 2);
        double R21 = mat.get(1, 0);
        double R12 = mat.get(0, 1);
        double R31 = mat.get(2, 0);
        double R13 = mat.get(0, 2);

        double qw = Math.sqrt(R11+R22+R33+1)/2.0;
        double qx = R32 - R23;
        double qy = R13 - R31;
        double qz = R21 - R12;

        // Compute set of vector elements for general case
        double qx1 = R31 + R13;
        double qy1 = R32 + R23;
        double qz1 = R33 - R11 - R22 + 1;
        boolean add = (qz >= 0);

        //  Handle the first degenerate case
        boolean case3 = R22 >= R33;
        if (R22 >= R33)
        {
            qx1 = R21 + R12;
            qy1 = R22 - R11 - R33 + 1;
            qz1 = R32 + R23;
            add = (qy >= 0);
        }

        // Handle the second degenerate case
        if ((R11 >= R22) & (R11 >= R33))
        {
            qx1 = R11 - R22 - R33 + 1;
            qy1 = R21 + R12;
            qz1 = R31 + R13;
            add = (qx >= 0);
        }

        // Subtract general from initial
        double qx2 = qx - qx1;
        double qy2 = qy - qy1;
        double qz2 = qz - qz1;

        // Add in the case that adding is necessary
        if (add)
        {
            qx2 = qx + qx1;
            qy2 = qy + qy1;
            qz2 = qz + qz1;
        }

        // Compute the norm of the vector components
        double qnorm = Math.sqrt(qx2 * qx2 + qy2 * qy2 + qz2 * qz2);

        if (MathUtils.zero(qnorm))
        {
            this.b = 0;
            this.c = 0;
            this.d = 0;
        }
        else
        {
            // Create a scaling factor for the vector components
            double qscale = (qw <= 1) ? Math.sqrt(1.0 - qw * qw) / qnorm : 0;

            // Construct the full quaternion
            this.b = qscale * qx2;
            this.c = qscale * qy2;
            this.d = qscale * qz2;
        }
    }

    public String toString()
    {
        return VectSource.create(b, c, d).toString();
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Quaternion))
        {
            return false;
        }
        else if (obj == this)
        {
            return true;
        }
        else
        {
            Quaternion left = this;
            Quaternion right = (Quaternion) obj;

            boolean pass = true;
            pass &= MathUtils.eq(left.b, right.b);
            pass &= MathUtils.eq(left.c, right.c);
            pass &= MathUtils.eq(left.d, right.d);

            return pass;
        }
    }

    public int hashCode()
    {
        int h = 0;
        h += (int) b;
        h += (int) c;
        h += (int) d;

        return h;
    }


    public double getA()
    {
        return Math.sqrt(1.0 - (this.b * this.b + this.c * this.c + this.d * this.d));
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

    public Quaternion inverse()
    {
        return new Quaternion(-this.b, -this.c, -this.d);
    }

    public Matrix matrix(boolean homo)
    {
        double a = this.getA();
        double b = this.b;
        double c = this.c;
        double d = this.d;

        int dim = homo ? 4 : 3;
        Matrix mat = new Matrix(dim, dim);

        if (homo)
        {
            mat.set(3, 3, 1);
        }

        mat.set(0, 0, a * a + b * b - c * c - d * d);
        mat.set(0, 1, 2 * b * c - 2 * a * d);
        mat.set(0, 2, 2 * b * d + 2 * a * c);

        mat.set(1, 0, 2 * b * c + 2 * a * d);
        mat.set(1, 1, a * a + c * c - b * b - d * d);
        mat.set(1, 2, 2 * c * d - 2 * a * b);

        mat.set(2, 0, 2 * b * d - 2 * a * c);
        mat.set(2, 1, 2 * c * d + 2 * a * b);
        mat.set(2, 2, a * a + d * d - c * c - b * b);

        return mat;
    }

    public Matrix matrix4()
    {
        return matrix(true);
    }

    public Matrix matrix3()
    {
        return matrix(false);
    }

    public Matrix matrix()
    {
        return matrix(false);
    }

    public Quaternion copy()
    {
        return new Quaternion(this.b, this.c, this.d);
    }
}
