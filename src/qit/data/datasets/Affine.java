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

package qit.data.datasets;

import qit.base.Dataset;
import qit.base.Global;
import qit.base.utils.JsonUtils;
import qit.data.formats.matrix.AffMatrixCoder;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;
import qit.math.structs.Quaternion;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/** an affine transformation */
public class Affine extends VectFunction implements Dataset
{
    public static final List<String> EXTS = Matrix.EXTS;

    static
    {
        EXTS.add("xfm");
        EXTS.add("aff");
    }

    private Matrix matrix;
    transient protected Matrix buff;

    public Affine(Affine xfm)
    {
        this.matrix = xfm.matrix.copy();
        this.buff = xfm.buff.copy();
    }

    public Affine(Matrix xfm)
    {
        super(xfm.cols() - 1, xfm.cols() - 1);

        int cols = xfm.cols();
        int rows = xfm.rows();

        Global.assume(cols == rows || cols == rows - 1, "invalid affine matrix");

        this.buff = new Matrix(cols, 1);
        this.matrix = new Matrix(cols, cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                this.matrix.set(i, j, xfm.get(i, j));
            }
        }

        if (rows == cols - 1)
        {
            this.matrix.set(rows, rows, 1);
        }
    }

    public Affine(Matrix R, Vect T)
    {
        this(R.rows());

        int dim = R.rows();
        this.matrix.set(0, dim - 1, 0, dim - 1, R);
        this.matrix.setColumn(dim, T);
    }

    public Affine(Quaternion R, Vect T)
    {
        this(T.size());

        int dim = T.size();
        this.matrix.set(0, dim - 1, 0, dim - 1, R.matrix());
        this.matrix.setColumn(dim, T);
    }

    public Affine(int dim)
    {
        this(MatrixSource.identity(dim + 1));
    }

    public static Affine linear(Matrix mat)
    {
        Affine out = new Affine(mat.rows());

        int dim = mat.rows();
        out.matrix.set(0, dim - 1, 0, dim - 1, mat);

        return out;
    }

    public static Affine id(int dim)
    {
        return new Affine(MatrixSource.identity(dim + 1));
    }

    public Affine copy()
    {
        return new Affine(this.matrix.copy());
    }

    public Affine jac()
    {
        // return the jacobian affine
        return new Affine(this.mat3(), VectSource.createND(this.getDimIn()));
    }

    public Matrix mat3()
    {
        // sub here includes the last index
        return this.matrix.sub(0, 3, 0, 3);
    }

    public Matrix linear()
    {
        // sub here includes the last index
        return this.matrix.sub(0, this.getDimIn() - 1, 0, this.getDimOut() - 1);
    }

    public Vect trans()
    {
        // sub here excludes the last index
        return this.matrix.getColumn(this.getDimIn()).sub(0, this.getDimOut());
    }

    public Matrix mat4()
    {
        return this.matrix.copy();
    }

    public Affine compose(Affine xfm)
    {
        return new Affine(this.matrix.times(xfm.matrix));
    }

    public void apply(Vect input, Vect output)
    {
        super.apply(input, output);

        for (int i = 0; i < this.getDimIn(); i++)
        {
            this.buff.set(i, 0, input.get(i));
        }
        this.buff.set(this.getDimIn(), 0, 1);

        Matrix out = this.matrix.times(this.buff);
        double h = out.get(this.getDimOut(), 0);

        if (h == 0)
        {
            h = 1;
        }

        for (int i = 0; i < this.getDimOut(); i++)
        {
            output.set(i, out.get(i, 0) / h);
        }
    }

    public Affine inv()
    {
        return new Affine(this.mat4().inv());
    }

    public Affine times(double s)
    {
        int dim = this.getDimIn() + 1;
        Matrix mat = MatrixSource.identity(dim);
        for (int i = 0; i < dim - 1; i++)
        {
            mat.set(i, i, s);
        }

        return this.compose(new Affine(mat));
    }

    public Affine times(double sx, double sy, double sz)
    {
        Matrix mat = MatrixSource.identity(4);
        mat.set(0, 0, sx);
        mat.set(1, 1, sy);
        mat.set(2, 2, sz);

        return this.compose(new Affine(mat));
    }

    public Affine times(Vect s)
    {
        int dim = s.size();
        Matrix mat = MatrixSource.identity(dim + 1);
        for (int i = 0; i < dim; i++)
        {
            mat.set(i, i, s.get(i));
        }

        return this.compose(new Affine(mat));
    }

    public Affine plus(double v)
    {
        return this.add(v, 1);
    }

    public Affine add(double v, int dim)
    {
        Matrix mat = MatrixSource.identity(dim + 1);
        for (int i = 0; i < dim; i++)
        {
            mat.set(i, dim, v);
        }

        return this.compose(new Affine(mat));
    }

    public Affine plus(double s, Vect v)
    {
        return this.plus(v.times(s));
    }

    public Affine plus(Vect v)
    {
        int dim = v.size();
        Matrix mat = MatrixSource.identity(dim + 1);
        for (int i = 0; i < dim; i++)
        {
            mat.set(i, dim, v.get(i));
        }

        return this.compose(new Affine(mat));
    }

    public Affine rotate(double angle)
    {
        Matrix R = MatrixSource.rotation(angle);

        Matrix mat = MatrixSource.identity(3);
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                mat.set(i, j, R.get(i, j));
            }
        }

        return this.compose(new Affine(mat));
    }

    public Affine rotate(Vect axis, double angle)
    {
        Matrix R = MatrixSource.rotation(axis, angle);

        Matrix mat = MatrixSource.identity(4);
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                mat.set(i, j, R.get(i, j));
            }
        }

        return this.compose(new Affine(mat));
    }

    public Affine rotate(double[][] R)
    {
        Matrix mat = MatrixSource.identity(4);
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                mat.set(i, j, R[i][j]);
            }
        }

        return this.compose(new Affine(mat));
    }

    public Affine rotate(Matrix R)
    {
        Matrix mat = MatrixSource.identity(4);
        mat.set(0, 2, 0, 2, R);

        return this.compose(new Affine(mat));
    }

    public Affine times(double[][] matrix)
    {
        return this.compose(new Affine(new Matrix(matrix)));
    }

    public Affine times(Matrix matrix)
    {
        return this.compose(new Affine(matrix));
    }

    public String toString()
    {
        return JsonUtils.encode(this);
    }

    public static Affine read(String fn) throws IOException
    {
        return new Affine(Matrix.read(fn));
    }

    public List<String> getExtensions()
    {
        return EXTS;
    }

    public void write(String fn) throws IOException
    {
        if (fn.endsWith("aff"))
        {
            AffMatrixCoder.write(this, new FileOutputStream(fn));
        }
        else
        {
            this.matrix.write(fn);
        }
    }
}