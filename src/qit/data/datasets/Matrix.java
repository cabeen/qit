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

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.utils.JsonUtils;
import qit.data.formats.matrix.AffMatrixCoder;
import qit.data.formats.matrix.CsvMatrixCoder;
import qit.data.formats.matrix.MatMatrixCoder;
import qit.data.formats.matrix.TxtMatrixCoder;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.math.utils.MathUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Matrix implements Dataset
{
    public static final List<String> EXTS = Lists.newArrayList(new String[]{JsonUtils.EXT, "csv", "txt"});
    private double[][] matrix;

    @SuppressWarnings("unused")
    private Matrix()
    {
    }

    public Matrix(int rows, int cols)
    {
        this.matrix = new double[rows][cols];
    }

    public Matrix(int rows, int cols, double value)
    {
        this(rows, cols);
        this.setAll(value);
    }

    public Matrix(double[][] array)
    {
        this(new Jama.Matrix(array));
    }

    public Matrix(Jama.Matrix mat)
    {
        this.matrix = mat.getArray();
    }

    public boolean isSquare()
    {
        return this.rows() == this.cols();
    }

    public boolean compatible(Matrix mat)
    {
        return mat.rows() == this.rows() && mat.cols() == this.cols();
    }

    public Matrix hom()
    {
        // convert a linear matrix into a homogeneous one
        Global.assume(this.isSquare(), "matrix must be square");

        int num = this.rows();
        Matrix out = new Matrix(num + 1, num + 1);
        out.set(0, num - 1, 0, num - 1, this);
        out.set(num, num, 1.0);

        return out;
    }

    public void set(Matrix mat)
    {
        Global.assume(this.compatible(mat), "dimensions do not match");

        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                this.matrix[i][j] = mat.matrix[i][j];
            }
        }
    }

    public Matrix setAll(double v)
    {
        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                this.matrix[i][j] = v;
            }
        }

        return this;
    }

    public Matrix setAllDiag(double v)
    {
        for (int i = 0; i < Math.min(this.rows(), this.cols()); i++)
        {
            this.matrix[i][i] = v;
        }

        return this;
    }

    public void set(int i, int j, double v)
    {
        this.matrix[i][j] = v;
    }

    public void set(int i0, int i1, int j0, int j1, Matrix m)
    {
        for (int i = i0; i <= i1; i++)
        {
            for (int j = j0; j <= j1; j++)
            {
                this.matrix[i][j] = m.matrix[i - i0][j - j0];
            }
        }
    }

    public Matrix sub(int i0, int i1, int j0, int j1)
    {
        Matrix out = new Matrix(i1 - i0 + 1, j1 - j0 + 1);
        for (int i = i0; i <= i1; i++)
        {
            for (int j = j0; j <= j1; j++)
            {
                out.matrix[i - i0][j - j0] = this.matrix[i][j];
            }
        }

        return out;
    }

    public double get(int i, int j)
    {
        return this.matrix[i][j];
    }

    public int rows()
    {
        return this.matrix.length;
    }

    public int cols()
    {
        return this.matrix[0].length;
    }

    public Matrix copy()
    {
        return new Matrix(MathUtils.copy(this.matrix));
    }

    public Matrix proto()
    {
        return new Matrix(this.rows(), this.cols());
    }

    public double mean()
    {
        double out = 0;

        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < this.matrix[i].length; j++)
            {
                out += this.matrix[i][j];
            }
        }

        return out / (this.cols() * this.rows());
    }

    public Vect meanRow()
    {
        double[] out = new double[this.cols()];

        for (int j = 0; j < this.cols(); j++)
        {
            out[j] = this.getColumn(j).mean();
        }

        return new Vect(out);
    }

    public Vect meanColumn()
    {
        double[] out = new double[this.rows()];

        for (int i = 0; i < this.rows(); i++)
        {
            out[i] = this.getRow(i).mean();
        }

        return new Vect(out);
    }

    public Vect stdRow()
    {
        double[] out = new double[this.cols()];

        for (int j = 0; j < this.cols(); j++)
        {
            out[j] = this.getColumn(j).std();
        }

        return new Vect(out);
    }

    public Vect stdColumn()
    {
        double[] out = new double[this.rows()];

        for (int i = 0; i < this.rows(); i++)
        {
            out[i] = this.getRow(i).std();
        }

        return new Vect(out);
    }

    public double[][] toArray()
    {
        return this.matrix;
    }

    public Matrix inv()
    {
        return new Matrix(new Jama.Matrix(this.matrix).inverse().getArray());
    }

    public double trace()
    {
        return new Jama.Matrix(this.matrix).trace();
    }

    public Matrix transpose()
    {
        return new Matrix(new Jama.Matrix(this.matrix).transpose().getArray());
    }

    public Matrix times(double s)
    {
        Matrix out = this.copy();
        out.timesEquals(s);
        return out;
    }

    public void timesEquals(double s)
    {
        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < this.matrix[i].length; j++)
            {
                this.matrix[i][j] *= s;
            }
        }
    }

    public Matrix times(Matrix m)
    {
        return new Matrix(new Jama.Matrix(this.matrix).times(new Jama.Matrix(m.matrix)));
    }

    public Matrix timesElem(Matrix m)
    {
        Global.assume(m.rows() == this.rows(), "rows do not match");
        Global.assume(m.cols() == this.cols(), "cols do not match");

        Matrix out = this.proto();
        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                out.set(i, j, this.get(i, j) * m.get(i, j));
            }
        }

        return out;
    }

    public Matrix timesElemEquals(Matrix m)
    {
        Global.assume(m.rows() == this.rows(), "rows do not match");
        Global.assume(m.cols() == this.cols(), "cols do not match");

        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                this.set(i, j, this.get(i, j) * m.get(i, j));
            }
        }

        return this;
    }

    public Matrix plus(Matrix m)
    {
        Matrix out = this.copy();
        out.plusEquals(m);
        return out;
    }

    public void plusEquals(Matrix m)
    {
        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < this.matrix[i].length; j++)
            {
                this.matrix[i][j] += m.matrix[i][j];
            }
        }
    }

    public Matrix plus(double s, Matrix m)
    {
        Global.assume(this.compatible(m), "dimensions do not match");

        Matrix out = this.proto();
        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                double va = this.get(i, j);
                double vb = m.get(i, j);

                out.set(i, j, va + s * vb);
            }
        }

        return out;
    }

    public void plusEquals(double s, Matrix m)
    {
        Global.assume(this.compatible(m), "dimensions do not match");

        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                double va = this.get(i, j);
                double vb = m.get(i, j);

                this.set(i, j, va + s * vb);
            }
        }
    }

    public void plusEquals(int i, int j, double v)
    {
        this.set(i, j, this.get(i, j) + v);
    }

    public Matrix plusRows(Vect v)
    {
        Matrix out = this.copy();
        out.plusRowsEquals(v);
        return out;
    }

    public void plusRowsEquals(Vect v)
    {
        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < this.matrix[i].length; j++)
            {
                this.matrix[i][j] += v.get(j);
            }
        }
    }

    public Matrix plusCols(Vect v)
    {
        Matrix out = this.copy();
        out.plusColsEquals(v);
        return out;
    }

    public void plusColsEquals(Vect v)
    {
        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < this.matrix[i].length; j++)
            {
                this.matrix[i][j] += v.get(i);
            }
        }
    }

    public Matrix plusElem(Matrix m)
    {
        Global.assume(m.rows() == this.rows(), "rows do not match");
        Global.assume(m.cols() == this.cols(), "cols do not match");

        Matrix out = this.proto();
        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                out.set(i, j, this.get(i, j) + m.get(i, j));
            }
        }

        return out;
    }

    public Matrix plusElemEquals(Matrix m)
    {
        Global.assume(m.rows() == this.rows(), "rows do not match");
        Global.assume(m.cols() == this.cols(), "cols do not match");

        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                this.set(i, j, this.get(i, j) + m.get(i, j));
            }
        }

        return this;
    }

    public Matrix minus(Matrix m)
    {
        Matrix out = this.copy();
        out.minusEquals(m);
        return out;
    }

    public void minusEquals(Matrix m)
    {
        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < this.matrix[i].length; j++)
            {
                this.matrix[i][j] -= m.matrix[i][j];
            }
        }
    }

    public Matrix minusRows(Vect v)
    {
        Matrix out = this.copy();
        out.minusRowsEquals(v);
        return out;
    }

    public void minusRowsEquals(Vect v)
    {
        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < this.matrix[i].length; j++)
            {
                this.matrix[i][j] -= v.get(j);
            }
        }
    }

    public Matrix minusCols(Vect v)
    {
        Matrix out = this.copy();
        out.minusColsEquals(v);
        return out;
    }

    public void minusColsEquals(Vect v)
    {
        for (int i = 0; i < this.matrix.length; i++)
        {
            for (int j = 0; j < this.matrix[i].length; j++)
            {
                this.matrix[i][j] -= v.get(i);
            }
        }
    }

    public Matrix minusElem(Matrix m)
    {
        Global.assume(m.rows() == this.rows(), "rows do not match");
        Global.assume(m.cols() == this.cols(), "cols do not match");

        Matrix out = this.proto();
        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                out.set(i, j, this.get(i, j) - m.get(i, j));
            }
        }

        return out;
    }

    public Matrix minusElemEquals(Matrix m)
    {
        Global.assume(m.rows() == this.rows(), "rows do not match");
        Global.assume(m.cols() == this.cols(), "cols do not match");

        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                this.set(i, j, this.get(i, j) - m.get(i, j));
            }
        }

        return this;
    }

    public Vect times(Vect vect)
    {
        int m = this.rows();
        int n = this.cols();

        Global.assume(n == vect.size(), String.format("matrix (%d) and vector (%d) don't match", n, vect.size()));

        Vect out = new Vect(m);
        for (int i = 0; i < m; i++)
        {
            double v = 0;
            for (int j = 0; j < n; j++)
            {
                v += this.get(i, j) * vect.get(j);
            }
            out.set(i, v);
        }

        return out;
    }

    public Matrix expEquals()
    {
        int m = this.rows();
        int n = this.cols();

        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                this.set(i, j, Math.exp(this.get(i, j)));
            }
        }

        return this;
    }

    public Matrix exp()
    {
        return this.copy().expEquals();
    }

    public Matrix logEquals()
    {
        int m = this.rows();
        int n = this.cols();

        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                this.set(i, j, Math.log(this.get(i, j)));
            }
        }

        return this;
    }

    public Matrix log()
    {
        return this.copy().logEquals();
    }

    public Matrix logSafeEquals()
    {
        int m = this.rows();
        int n = this.cols();

        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                double inv = this.get(i, j);
                double outv = inv <= 0 ? Double.NEGATIVE_INFINITY : Math.log(inv);
                this.set(i, j, outv);
            }
        }

        return this;
    }

    public Matrix permuteRows(int[] map)
    {
        Matrix out = new Matrix(this.rows(), map.length);

        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < map.length; j++)
            {
                out.set(i, j, this.get(i, map[j]));
            }
        }

        return out;
    }

    public Matrix permuteCols(int[] map)
    {
        Matrix out = new Matrix(map.length, this.cols());

        for (int i = 0; i < map.length; i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                out.set(i, j, this.get(map[i], j));
            }
        }

        return out;
    }

    public Matrix catRows(Matrix m)
    {
        Global.assume(m.rows() == this.rows(), String.format("left (%d) and right (%d) row mismatch", this.rows(), m.rows()));
        int ncols = this.cols() + m.cols();
        Matrix out = new Matrix(this.rows(), ncols);

        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                out.set(i, j, this.get(i, j));
            }

            for (int j = 0; j < m.cols(); j++)
            {
                out.set(i, this.cols() + j, m.get(i, j));
            }
        }

        return out;
    }


    public Matrix catCols(Matrix m)
    {
        Global.assume(m.cols() == this.cols(), String.format("left (%d) and right (%d) col mismatch", this.cols(), m.cols()));
        int nrows = this.rows() + m.rows();
        Matrix out = new Matrix(nrows, this.cols());

        for (int j = 0; j < this.cols(); j++)
        {
            for (int i = 0; i < this.rows(); i++)
            {
                out.set(i, j, this.get(i, j));
            }

            for (int i = 0; i < m.rows(); i++)
            {
                out.set(this.rows() + i, j, m.get(i, j));
            }
        }

        return out;
    }

    public Matrix subrows(List<Integer> which)
    {
        Matrix out = new Matrix(which.size(), this.cols());

        for (int i = 0; i < which.size(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                out.set(i, j, this.get(which.get(i), j));
            }
        }

        return out;
    }

    public Matrix subcols(List<Integer> which)
    {
        Matrix out = new Matrix(this.rows(), which.size());

        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < which.size(); j++)
            {
                out.set(i, j, this.get(i, which.get(j)));
            }
        }

        return out;
    }

    public Vect prineig()
    {
        return MatrixUtils.eig(this).vectors.get(0);
    }

    public double normf()
    {
        return new Jama.Matrix(this.matrix).normF();
    }

    public double det()
    {
        return new Jama.Matrix(this.matrix).det();
    }

    public double sumsq()
    {
        double out = 0;

        int m = this.rows();
        int n = this.cols();

        for (int i = 0; i < m; i++)
        {
            for (int j = 0; j < n; j++)
            {
                double v = this.get(i, j);
                out += v * v;
            }
        }

        return out;
    }

    public Vect diag()
    {
        int dim = Math.min(this.cols(), this.rows());
        Vect out = new Vect(dim);
        for (int i = 0; i < dim; i++)
        {
            out.set(i, this.get(i, i));
        }

        return out;
    }

    public int rank()
    {
        return new Jama.Matrix(this.matrix).rank();
    }

    public double norm1()
    {
        return new Jama.Matrix(this.matrix).norm1();
    }

    public double norm2()
    {
        return new Jama.Matrix(this.matrix).norm2();
    }

    public double normF()
    {
        return new Jama.Matrix(this.matrix).normF();
    }

    public double normInf()
    {
        return new Jama.Matrix(this.matrix).normInf();
    }

    public boolean infinite()
    {
        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                if (Double.isInfinite(this.get(i, j)))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean nan()
    {
        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                if (Double.isNaN(this.get(i, j)))
                {
                    return true;
                }
            }
        }

        return false;
    }

    public double inner(Matrix m)
    {
        double out = 0;
        for (int i = 0; i < this.rows(); i++)
        {
            for (int j = 0; j < this.cols(); j++)
            {
                out += this.get(i, j) * m.get(i, j);
            }
        }
        return out;
    }

    public Vect getColumn(int idx)
    {
        int dim = this.rows();
        Vect out = new Vect(dim);

        for (int i = 0; i < dim; i++)
        {
            out.set(i, this.get(i, idx));
        }

        return out;
    }

    public Vect getRow(int idx)
    {
        int dim = this.cols();
        Vect out = new Vect(dim);

        for (int i = 0; i < dim; i++)
        {
            out.set(i, this.get(idx, i));
        }

        return out;
    }

    public void setRow(int idx, Vect v)
    {
        for (int i = 0; i < v.size(); i++)
        {
            this.set(idx, i, v.get(i));
        }
    }

    public void setColumn(int idx, Vect v)
    {
        for (int i = 0; i < v.size(); i++)
        {
            this.set(i, idx, v.get(i));
        }
    }

    public Vect packColumn()
    {
        return new Vect(new Jama.Matrix(this.matrix).getColumnPackedCopy());
    }

    public Vect packRow()
    {
        return new Vect(new Jama.Matrix(this.matrix).getRowPackedCopy());
    }

    public Vect flatten()
    {
        int rows = this.rows();
        int cols = this.cols();

        Vect out = VectSource.createND(rows * cols);
        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                out.set(i * cols + j, this.get(i, j));
            }
        }

        return out;
    }

    public Matrix set(Vect vect)
    {
        int rows = this.rows();
        int cols = this.cols();

        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                this.set(i, j, vect.get(i * cols + j));
            }
        }

        return this;
    }

    public String toString()
    {
        return JsonUtils.encode(this);
    }

    public static Matrix read(String fn) throws IOException
    {
        if (fn.endsWith("txt.gz") || fn.endsWith("txt") || fn.endsWith("xfm"))
        {
            return TxtMatrixCoder.read(fn);
        }
        else if (fn.endsWith("csv"))
        {
            return CsvMatrixCoder.read(fn);
        }
        else if (fn.endsWith("aff"))
        {
            return AffMatrixCoder.read(fn);
        }
        else if (fn.endsWith("mat"))
        {
            return MatMatrixCoder.read(fn);
        }
        else if (fn.endsWith(JsonUtils.EXT))
        {
            return JsonUtils.decode(Matrix.class, Files.toString(new File(fn), Charsets.UTF_8));
        }
        else
        {
            // use text if the filename extension was not recognized
            return TxtMatrixCoder.read(fn);
        }
    }

    public void write(String fn) throws IOException
    {
        if (fn.endsWith("txt.gz") || fn.endsWith("txt") || fn.endsWith("xfm"))
        {
            TxtMatrixCoder.write(this, fn);
            return;
        }
        else if (fn.endsWith("csv"))
        {
            CsvMatrixCoder.write(this, fn);
            return;
        }
        else if (fn.endsWith(JsonUtils.EXT))
        {
            FileUtils.write(new File(fn), JsonUtils.encode(this), false);
            return;
        }
        else
        {
            // use text if the filename extension was not recognized
            TxtMatrixCoder.write(this, fn);
        }

        Logging.error("invalid filename: " + fn);
    }

    public List<String> getExtensions()
    {
        return EXTS;
    }
}
