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

package qit.data.source;

import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Indexed;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.math.structs.BinaryVectFunction;
import qit.math.structs.Distance;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

/** utilties for creating matrices */
public class MatrixSource
{
    public static <E> Matrix distSym(Distance<E> dist, Indexed<E> data)
    {
        int num = data.size();
        Matrix out = new Matrix(num, num);

        int total = num * num / 2;
        int ppercent = 0;
        int step = 2;
        
        Logging.info("... starting computing distance matrix with " + num + " entries");
        int count = 0;
        for (int i = 0; i < num; i++)
        {
            for (int j = i + 1; j < num; j++)
            {
                int percent = (int) Math.round(100.0 * count / (double) total);
                if (percent != ppercent && percent % step == 0)
                {
                    Logging.info("...... " + percent +" percent complete");
                    ppercent = percent;
                }
                count += 1;
                
                E left = data.get(i);
                E right = data.get(j);
                double d = dist.dist(left, right);
                out.set(i, j, d);   
                out.set(j, i, d);                
            }
        }

        return out;
    }
    
    public static <E> Matrix distAllPairs(Distance<E> dist, Indexed<E> data)
    {
        int num = data.size();
        Matrix out = new Matrix(num, num);

        int total = num * num;
        int ppercent = 0;
        int step = 2;
        
        Logging.info("... starting computing distance matrix with " + num + " entries");
        for (int i = 0; i < num; i++)
        {
            for (int j = 0; j < num; j++)
            {
                int percent = (int) Math.round(100.0 * ((i * num) + j) / (double) total);
                if (percent != ppercent && percent % step == 0)
                {
                    Logging.info("...... " + percent +" percent complete");
                    ppercent = percent;
                }
                
                E left = data.get(i);
                E right = data.get(j);
                double d = dist.dist(left, right);
                out.set(i, j, d);
            }
        }

        return out;
    }

    public static Matrix eig(Vect vals, Vects vecs)
    {
        Matrix u = MatrixSource.createCols(vecs);
        Matrix v = MatrixSource.diag(vals);

        return u.times(v).times(u.transpose());
    }

    public static Matrix tensor(Vect v, double v1, double v2, double v3)
    {
        Matrix dyadic = outer(v, v);
        EigenDecomp eig = MatrixUtils.eig(dyadic);
        Matrix V = columns(eig.vectors);
        Matrix D = new Matrix(3, 3);
        D.set(0, 0, v1);
        D.set(1, 1, v2);
        D.set(2, 2, v3);

        Matrix VDVT = V.times(D).times(V.transpose());

        return VDVT;
    }

    public static Matrix rotation(double angle)
    {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);
        Matrix mat = new Matrix(2, 2);
        mat.set(0, 0, cos);
        mat.set(1, 1, cos);
        mat.set(0, 1, -sin);
        mat.set(1, 0, sin);

        return mat;
    }

    public static Matrix rotation(Vect axis, double angle)
    {
        double mag = axis.norm();

        if (mag == 0)
        {
            throw new IllegalArgumentException("Error, rotation axis has magnitude zero");
        }

        // The the unit magnitude coordinates
        double x = axis.get(0) / mag;
        double y = axis.get(1) / mag;
        double z = axis.get(2) / mag;

        // Create the curl operator
        Matrix cp = new Matrix(3, 3);
        cp.set(0, 1, -z);
        cp.set(1, 0, z);
        cp.set(0, 2, y);
        cp.set(2, 0, -y);
        cp.set(1, 2, -x);
        cp.set(2, 1, x);

        // Compute the sin and cos of the angle
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        // Compute the rotation matrix using the Rodrigues Rotation Formula
        Matrix term2, term3;
        term2 = cp.times(sin);
        term3 = cp.times(cp).times(1.0 - cos);
        MatrixSource.identity(3).plus(term2).plus(term3);
        Matrix R = MatrixSource.identity(3).plus(term2).plus(term3);

        return R;
    }

    // See: Computing Euler angles from a rotation matrix by Gregory G. Slabaugh

    public static Vect angles(Matrix rot)
    {
        Global.assume(MathUtils.unit(rot.det()), "expected positive orthogonal matrix");

        // should be inverse of rotation function
        if (Math.abs(rot.get(2, 0)) != 1)
        {
            double theta = -Math.asin(rot.get(2, 0));

            double c = Math.cos(theta);
            double psi = Math.atan2(rot.get(2, 1) / c, rot.get(2, 2) / c);
            double phi = Math.atan2(rot.get(1, 0) / c, rot.get(0, 0) / c);

            return VectSource.create3D(psi, theta, phi);
        }
        else
        {
            double s = -MathUtils.sign(rot.get(2, 0));
            double psi = Math.atan2(s * rot.get(0, 1), s * rot.get(0, 2));
            double theta = s * Math.PI / 2;
            double phi = 0;

            return VectSource.create3D(psi, theta, phi);
        }
    }

    public static Matrix rotation(Vect angles)
    {
        // euler angles to a (ortogonal) rotation matrix
        double psi = angles.get(0);
        double theta = angles.get(1);
        double phi = angles.get(2);

        double cospsi = Math.cos(psi);
        double sinpsi = Math.sin(psi);
        double costheta = Math.cos(theta);
        double sintheta = Math.sin(theta);
        double cosphi = Math.cos(phi);
        double sinphi = Math.sin(phi);

        Matrix rot = new Matrix(3,3);
        rot.set(0, 0, costheta * cosphi);
        rot.set(0, 1, sinpsi * sintheta * cosphi - cospsi * sinphi);
        rot.set(0, 2, cospsi * sintheta * cosphi + sinpsi * sinphi);
        rot.set(1, 0, costheta * sinphi);
        rot.set(1, 1, sinpsi * sintheta * sinphi + cospsi * cosphi);
        rot.set(1, 2, cospsi * sintheta * sinphi - sinpsi * cosphi);
        rot.set(2, 0, -sintheta);
        rot.set(2, 1, sinpsi * costheta);
        rot.set(2, 2, cospsi * costheta);

        // this should be invertable with the angles function

        return rot;
    }

    public static Matrix rotation2D(double angle)
    {
        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        Matrix out = new Matrix(2, 2);
        out.set(0, 0, cos);
        out.set(1, 1, cos);
        out.set(0, 1, -sin);
        out.set(1, 0, sin);

        return out;
    }

    public static Matrix dyadic(Vect v)
    {
        return outer(v, v);
    }

    public static Matrix outer(Vect a, Vect b)
    {
        int na = a.size();
        int nb = b.size();
        Matrix matrix = new Matrix(na, nb);
        for (int i = 0; i < na; i++)
        {
            for (int j = 0; j < nb; j++)
            {
                double av = a.get(i);
                double bv = b.get(j);
                matrix.set(i, j, av * bv);
            }
        }

        return matrix;
    }

    public static Matrix rows(Vects vects)
    {
        int cols = vects.getDim();
        int rows = vects.size();

        Matrix out = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                out.set(i, j, vects.get(i).get(j));
            }
        }

        return out;
    }

    public static Matrix columns(Vects vects)
    {
        int rows = vects.getDim();
        int cols = vects.size();

        Matrix out = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                out.set(i, j, vects.get(j).get(i));
            }
        }

        return out;
    }

    public static Matrix skew(double x, double y, double z)
    {
        Matrix out = diag(1, 1, 1);
        out.set(2, 1, x);
        out.set(1, 2, -x);
        out.set(2, 0, y);
        out.set(0, 2, -y);
        out.set(1, 0, z);
        out.set(0, 1, -z);

        return out;
    }

    public static Matrix diag(double x, double y, double z)
    {
        Matrix out = new Matrix(3, 3);
        out.set(0, 0, x);
        out.set(1, 1, y);
        out.set(2, 2, z);

        return out;
    }

    public static Matrix diag(Vect vect)
    {
        int dim = vect.size();
        Matrix out = new Matrix(dim, dim);
        for (int i = 0; i < dim; i++)
        {
            out.set(i, i, vect.get(i));
        }
        return out;
    }

    public static Matrix col(Vect vect)
    {
        int dim = vect.size();
        Matrix out = new Matrix(dim, 1);
        for (int i = 0; i < dim; i++)
        {
            out.set(i, 0, vect.get(i));
        }
        return out;
    }
    public static Matrix row(Vect vect)
    {
        int dim = vect.size();
        Matrix out = new Matrix(1, dim);
        for (int j = 0; j < dim; j++)
        {
            out.set(0, j, vect.get(j));
        }
        return out;
    }

    public static Matrix constant(int m, int n, double v)
    {
        Matrix out = new Matrix(m, n);
        out.setAll(v);

        return out;
    }

    public static Matrix createCols(Vect vect, int m, int n)
    {
        Matrix out = new Matrix(m, n);
        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                out.set(i, j, vect.get(i * n + j));
            }
        }

        return out;
    }

    public static Matrix createRows(Vect vect, int m, int n)
    {
        Matrix out = new Matrix(m, n);
        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                out.set(i, j, vect.get(j * m + i));
            }
        }

        return out;
    }

    public static Matrix createRows(Vect vect)
    {
        int n = (int) Math.sqrt(vect.size());
        Global.assume(n * n == vect.size(), "vector does not encode a square matrix");

        return createRows(vect, n, n);
    }

    public static Matrix createCols(Vect vect)
    {
        int n = (int) Math.sqrt(vect.size());
        Global.assume(n * n == vect.size(), "vector does not encode a square matrix");

        return createCols(vect, n, n);
    }

    public static Matrix createCol(Vect vect)
    {
        Matrix out = new Matrix(vect.size(), 1);
        for (int i = 0; i < vect.size(); i++)
        {
            out.set(i, 0, vect.get(i));
        }

        return out;
    }

    public static Matrix createRow(Vect vect)
    {
        Matrix out = new Matrix(1, vect.size());
        for (int i = 0; i < vect.size(); i++)
        {
            out.set(0, i, vect.get(i));
        }

        return out;
    }

    public static Matrix createRows(Vects vects)
    {
        int n = vects.getDim();
        int m = vects.size();

        Matrix mat = new Matrix(m, n);
        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                mat.set(i, j, vects.get(i).get(j));
            }
        }

        return mat;
    }

    public static Matrix createCols(Vects vects)
    {
        int n = vects.size();
        int m = vects.getDim();

        Matrix mat = new Matrix(m, n);
        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                mat.set(i, j, vects.get(j).get(i));
            }
        }

        return mat;
    }

    public static Matrix identity(int dim)
    {
        Matrix mat = new Matrix(dim, dim);
        for (int i = 0; i < dim; i++)
        {
            mat.set(i, i, 1);
        }

        return mat;
    }

    public static Matrix gaussianKernel(VectFunction kernel, BinaryVectFunction dist, Vects vects)
    {
        if (kernel.getDimOut() != 1)
        {
            Logging.info("invalid triangle function");
        }

        int n = vects.size();
        Matrix out = new Matrix(n, n);
        for (int i = 0; i < n; i++)
        {
            for (int j = i + 1; j < n; j++)
            {
                Vect v = dist.apply(vects.get(i), vects.get(j));
                double k = kernel.apply(v).get(0);
                out.set(i, j, k);
                out.set(j, i, k);
            }
        }

        return out;
    }

    public static Matrix rotateAxis(Vect axis)
    {
        // rotate axis onto [0,0,1]
        Vect perp = axis.perp();
        Vect cross = axis.cross(perp);

        Matrix R = new Matrix(3,3);
        R.setRow(0, cross);
        R.setRow(1, perp);
        R.setRow(2, axis);

        return R;
    }

    public static Matrix zeppelin(Vect axis, double a, double b)
    {
        Vect perp = axis.perp();
        Vect cross = axis.cross(perp);

        Matrix R = new Matrix(3,3);
        R.setColumn(0, axis);
        R.setColumn(1, perp);
        R.setColumn(2, cross);

        Matrix V = MatrixSource.diag(VectSource.create3D(a, b, b));

        Matrix Z = R.times(V).times(R.transpose());

        return Z;
    }

    public static Matrix ident()
    {
        return diag(1, 1, 1);
    }

    public static Matrix diag3(double v)
    {
        return diag(v, v, v);
    }

    public static Matrix flipX()
    {
        return diag(-1, 1, 1);
    }

    public static Matrix flipY()
    {
        return diag(1, -1, 1);
    }

    public static Matrix flipZ()
    {
        return diag(1, 1, -1);
    }

    public static Matrix swapXY()
    {
        Matrix out = new Matrix(3, 3);

        out.set(0, 1, 1);
        out.set(1, 0, 1);
        out.set(2, 2, 1);

        return out;
    }

    public static Matrix swapXZ()
    {
        Matrix out = new Matrix(3, 3);

        out.set(0, 2, 1);
        out.set(1, 1, 1);
        out.set(2, 0, 1);

        return out;
    }

    public static Matrix swapYZ()
    {
        Matrix out = new Matrix(3, 3);

        out.set(0, 0, 1);
        out.set(1, 2, 1);
        out.set(2, 1, 1);

        return out;
    }

    public static Matrix trans4(Vect vec)
    {
        Matrix out = identity(4);
        out.set(0, 3, vec.getX());
        out.set(1, 3, vec.getY());
        out.set(2, 3, vec.getZ());

        return out;
    }

    public static Matrix trans4(double x, double y, double z)
    {
        Matrix out = identity(4);
        out.set(0, 3, x);
        out.set(1, 3, y);
        out.set(2, 3, z);

        return out;
    }

    public static Matrix scale4(Vect vec)
    {
        Matrix out = identity(4);
        out.set(0, 0, vec.getX());
        out.set(1, 1, vec.getY());
        out.set(2, 2, vec.getZ());

        return out;
    }

    public static Matrix scale4(double val)
    {
        Matrix out = identity(4);
        out.set(0, 0, val);
        out.set(1, 1, val);
        out.set(2, 2, val);

        return out;
    }

    public static Matrix scale4(double x, double y, double z)
    {
        Matrix out = identity(4);
        out.set(0, 0, x);
        out.set(1, 1, y);
        out.set(2, 2, z);

        return out;
    }

    public static Matrix skew4(double x, double y, double z)
    {
        Matrix out = identity(4);
        out.set(2, 1, x);
        out.set(1, 2, -x);
        out.set(2, 0, y);
        out.set(0, 2, -y);
        out.set(1, 0, z);
        out.set(0, 1, -z);

        return out;
    }

    public static Matrix skew4(Vect vec)
    {
        return skew4(vec.getX(), vec.getY(), vec.getZ());
    }
}
