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

package qit.data.models;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.data.utils.mri.structs.Gradients;
import qit.base.Model;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;

public class Tensor extends Model<Tensor>
{
    public final static String NAME = "dti";
    public final static String COLOR = "color";

    public static double FREE_DIFF = 3.0e-3;

    // how parameters are stored as vectors
    public final static int DT_S0 = 0;
    public final static int DT_XX = 1;
    public final static int DT_XY = 2;
    public final static int DT_YY = 3;
    public final static int DT_XZ = 4;
    public final static int DT_YZ = 5;
    public final static int DT_ZZ = 6;
    public final static int DT_FW = 7;
    public final static int DT_DIM = 8;

    public final static String FEATURES_S0 = "S0";
    public final static String FEATURES_FA = "FA";
    public final static String FEATURES_MD = "MD";
    public final static String FEATURES_RD = "RD";
    public final static String FEATURES_AD = "AD";
    public final static String FEATURES_CP = "CP";
    public final static String FEATURES_CL = "CL";
    public final static String FEATURES_CS = "CS";
    public final static String FEATURES_PD = "PD";
    public final static String FEATURES_FW = "FW";
    public final static String FEATURES_RGB = "RGB";
    public final static String[] FEATURES = { FEATURES_S0, FEATURES_FA, FEATURES_MD, FEATURES_RD, FEATURES_AD, FEATURES_CP, FEATURES_CL, FEATURES_CS, FEATURES_PD, FEATURES_FW, FEATURES_RGB };

    public final static int DT_NV = 3;

    // Thanks to Gary Zhang for reporting all of these tensor representations
    public final static int[] DTITK_MAP = { DT_XX, DT_XY, DT_YY, DT_XZ, DT_YZ, DT_ZZ };
    public final static int[] CAMINO_MAP = { DT_XX, DT_YY, DT_ZZ, DT_XY, DT_XZ, DT_YZ };
    public final static int[] FSL_MAP = { DT_XX, DT_XY, DT_XZ, DT_YY, DT_YZ, DT_ZZ };
    public final static int[] DTISTUDIO_MAP = { DT_XX, DT_YY, DT_ZZ, DT_XY, DT_XZ, DT_YZ };

    public final static int TENSOR_RES = 2;

    public Double s0;
    public Vects vecs;
    public Vect vals;
    public Double fw;

    public static boolean matches(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lname = name.toLowerCase();
        return (lname.contains(".fwdti") || lname.contains(".dti") || lname.contains(".tensor"));
    }

    public Tensor()
    {
        this.s0 = 0.0;
        this.fw = 0.0;

        this.vecs = new Vects();
        this.vecs.add(VectSource.create3D());
        this.vecs.add(VectSource.create3D());
        this.vecs.add(VectSource.create3D());

        this.vals = VectSource.create3D();
    }

    public Tensor(Vect encoding)
    {
        this();
        this.setEncoding(encoding);
    }

    public Tensor(double s0, Matrix matrix)
    {
        this();
        this.set(s0, matrix, 0.0);
    }

    public Tensor(double s0, Matrix matrix, double fw)
    {
        this();
        this.set(s0, matrix, fw);
    }

    public void orient()
    {
        Matrix rot = new Matrix(3, 3);
        rot.setRow(0, this.getVec(0));
        rot.setRow(1, this.getVec(1));
        rot.setRow(2, this.getVec(2));

        if (rot.det() < 0)
        {
            this.setVec(2, this.getVec(2).times(-1));
        }
    }

    public double baseline()
    {
        return this.s0;
    }

    public Vect getVec(int i)
    {
        return this.vecs.get(i).copy();
    }

    public double getVal(int i)
    {
        return this.vals.get(i);
    }

    public Vect getVals()
    {
        return this.vals.copy();
    }

    public Tensor setVals(Vect v)
    {
        this.vals.set(v);

        return this;
    }

    public Vects getVecs()
    {
        return this.vecs.copy();
    }

    public double getBaseline()
    {
        return this.s0;
    }

    public double getFreeWater()
    {
        return this.fw;
    }

    public Tensor setVec(int i, Vect v)
    {
        this.vecs.get(i).set(v);

        return this;
    }

    public Tensor setVal(int i, double v)
    {
        this.vals.set(i, v);

        return this;
    }

    public Tensor setZero()
    {
        this.s0 = 0.0;
        this.vals.setAll(0);

        return this;
    }

    public Tensor setBaseline(double v)
    {
        this.s0 = v;

        return this;
    }

    public Tensor setFreeWater(double e)
    {
        this.fw = e;

        return this;
    }

    public Tensor setMatrix(Matrix matrix)
    {
        try {
            EigenDecomp eig = MatrixUtils.eig(matrix);
            this.vals.set(eig.values);
            for (int i = 0; i < DT_NV; i++)
            {
                this.vecs.get(i).set(eig.vectors.get(i));
            }
        } catch (RuntimeException e)
        {
            Logging.info("warning: eig failed");
            this.vals.setAll(0);
            this.vecs.get(0).set(VectSource.randomUnit());
            this.vecs.get(1).set(this.vecs.get(0).perp());
            this.vecs.get(2).set(this.vecs.get(0).cross(this.vecs.get(1)));
        }

        return this;
    }

    public Tensor scale(double scale)
    {
        this.vals.timesEquals(scale);

        return this;
    }

    public Tensor clamp(double clamp)
    {
        for (int i = 0; i < 3; i++)
        {
            double val = this.vals.get(i);
            if (val < clamp)
            {
                this.vals.set(i, clamp);
            }
        }

        return this;
    }

    public Tensor set(double s0, Matrix matrix, double fw)
    {
        this.s0 = s0;
        this.fw = fw;

        this.setMatrix(matrix);

        return this;
    }

    public Tensor set(Tensor t)
    {
        this.s0 = t.s0;
        this.fw = t.fw;
        this.vals.set(t.vals);
        for (int i = 0; i < DT_NV; i++)
        {
            this.vecs.get(i).set(t.vecs.get(i));
        }

        return this;
    }

    public Tensor copy()
    {
        Tensor out = new Tensor();
        out.set(this);
        return out;
    }

    public Tensor proto()
    {
        return new Tensor();
    }

    public int getDegreesOfFreedom()
    {
        return 6;
    }

    public int getEncodingSize()
    {
        return DT_DIM;
    }

    public Matrix getMatrix()
    {
        return MatrixSource.eig(this.vals, this.vecs);
    }

    public Tensor setEncoding(Vect encoding)
    {
        double s0 = encoding.get(DT_S0);
        Matrix matrix = new Matrix(3, 3);
        matrix.set(0, 0, encoding.get(DT_XX));
        matrix.set(1, 1, encoding.get(DT_YY));
        matrix.set(2, 2, encoding.get(DT_ZZ));
        matrix.set(0, 1, encoding.get(DT_XY));
        matrix.set(1, 0, encoding.get(DT_XY));
        matrix.set(0, 2, encoding.get(DT_XZ));
        matrix.set(2, 0, encoding.get(DT_XZ));
        matrix.set(1, 2, encoding.get(DT_YZ));
        matrix.set(2, 1, encoding.get(DT_YZ));
        double fw = encoding.get(DT_FW);

        this.set(s0, matrix, fw);

        return this;
    }

    public void getEncoding(Vect encoding)
    {
        encoding.set(DT_S0, this.s0);
        Matrix matrix = getMatrix();
        encoding.set(DT_XX, matrix.get(0, 0));
        encoding.set(DT_YY, matrix.get(1, 1));
        encoding.set(DT_ZZ, matrix.get(2, 2));
        encoding.set(DT_XY, matrix.get(0, 1));
        encoding.set(DT_XZ, matrix.get(0, 2));
        encoding.set(DT_YZ, matrix.get(1, 2));
        encoding.set(DT_FW, this.fw);
    }

    public Tensor log()
    {
        Tensor output = new Tensor();
        for (int i = 0; i < DT_NV; i++)
        {
            Vect vec = this.vecs.get(i);
            double val = this.vals.get(i);
            double logval = val <= 0 ? Double.MIN_VALUE : Math.log(val);

            output.vecs.get(i).set(vec);
            output.vals.set(i, logval);
        }
        return output;
    }

    public double dist(Tensor t)
    {
        Matrix ma = MatrixSource.eig(this.vals, this.vecs);
        Matrix mb = MatrixSource.eig(t.vals, t.vecs);

        Matrix md = ma.minus(mb);
        return md.norm2();
    }

    public Vect feature(String name)
    {
        double v1 = this.vals.get(0);
        double v2 = this.vals.get(1);
        double v3 = this.vals.get(2);

        if (FEATURES_FA.equals(name))
        {
            return VectSource.create1D(fa(v1, v2, v3));
        }
        else if (FEATURES_MD.equals(name))
        {
            return VectSource.create1D((v1 + v2 + v3) / 3.0);
        }
        else if (FEATURES_RD.equals(name))
        {
            return VectSource.create1D((v2 + v3) / 2.0);
        }
        else if (FEATURES_AD.equals(name))
        {
            return VectSource.create1D(v1);
        }
        else if (FEATURES_CP.equals(name))
        {
            double den = v1 + v2 + v3;
            double c = MathUtils.zero(den) ? 0.0 : 2 * (v2 - v3) / den;
            return VectSource.create1D(c);
        }
        else if (FEATURES_CL.equals(name))
        {
            double den = v1 + v2 + v3;
            double c = MathUtils.zero(den) ? 0.0 : 2 * (v1 - v2) / den;
            return VectSource.create1D(c);
        }
        else if (FEATURES_CS.equals(name))
        {
            double den = v1 + v2 + v3;
            double c = MathUtils.zero(den) ? 0.0 : 3 * v3 / den;
            return VectSource.create1D(c);
        }
        else if (FEATURES_PD.equals(name))
        {
            return this.getVec(0);
        }
        else if (FEATURES_S0.equals(name))
        {
            return VectSource.create1D(this.s0);
        }
        else if (FEATURES_FW.equals(name))
        {
            return VectSource.create1D(this.fw);
        }
        else if (FEATURES_RGB.equals(name))
        {
            return this.getVec(0).abs().times(this.feature(FEATURES_FA).get(0));
        }
        else
        {
            throw new RuntimeException("invalid index: " + name);
        }
    }

    public static double fa(double v1, double v2, double v3)
    {
        double mdv = (v1 + v2 + v3) / 3.0;
        double dv1 = v1 - mdv;
        double dv2 = v2 - mdv;
        double dv3 = v3 - mdv;

        double num = dv1 * dv1 + dv2 * dv2 + dv3 * dv3;
        double den = v1 * v1 + v2 * v2 + v3 * v3;

        double fa = den == 0 ? 0.0 : Math.sqrt(1.5 * num / den);

        return fa;
    }

    public static double rmse(Vect signal, Gradients gradients, Vect param)
    {
        double s0 = param.get(Tensor.DT_S0);
        double mse = 0;

        for (int i = 0; i < gradients.size(); i++)
        {
            double b = gradients.getBval(i);
            Vect g = gradients.getBvec(i);

            double gx = g.get(0);
            double gy = g.get(1);
            double gz = g.get(2);

            Vect bv = VectSource.createND(Tensor.DT_DIM);
            bv.set(Tensor.DT_XX, -b * gx * gx);
            bv.set(Tensor.DT_YY, -b * gy * gy);
            bv.set(Tensor.DT_ZZ, -b * gz * gz);
            bv.set(Tensor.DT_XY, -b * 2 * gx * gy);
            bv.set(Tensor.DT_YZ, -b * 2 * gy * gz);
            bv.set(Tensor.DT_XZ, -b * 2 * gx * gz);

            double bval = gradients.getBval(i);

            double expiso = Math.exp(-bval * FREE_DIFF);
            double frac = param.get(Tensor.DT_FW);

            double expten = Math.exp(param.dot(bv));
            double asig = frac * expiso + (1.0 - frac) * expten;

            double si = s0 * asig;

            double ds = si - signal.get(i);
            mse += ds * ds / gradients.size();
        }

        double rmse = Math.sqrt(mse);

        return rmse;
    }

    public List<String> features()
    {
        return Lists.newArrayList(FEATURES);
    }

    public Tensor getThis()
    {
        return this;
    }

    public static VectFunction synth(final Gradients gradients)
    {
        final Matrix bv = new Matrix(gradients.size(), Tensor.DT_DIM);
        for (int i = 0; i < gradients.size(); i++)
        {
            double b = gradients.getBval(i);
            Vect g = gradients.getBvec(i);

            double gx = g.get(0);
            double gy = g.get(1);
            double gz = g.get(2);

            bv.set(i, Tensor.DT_XX, -b * gx * gx);
            bv.set(i, Tensor.DT_YY, -b * gy * gy);
            bv.set(i, Tensor.DT_ZZ, -b * gz * gz);
            bv.set(i, Tensor.DT_XY, -b * 2 * gx * gy);
            bv.set(i, Tensor.DT_YZ, -b * 2 * gy * gz);
            bv.set(i, Tensor.DT_XZ, -b * 2 * gx * gz);
        }

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double s0 = input.get(Tensor.DT_S0);
                Vect dv = bv.times(input);

                for (int i = 0; i < gradients.size(); i++)
                {
                    double expten = Math.exp(dv.get(i));

                    double bval = gradients.getBval(i);
                    double expiso = Math.exp(-bval * FREE_DIFF);
                    double frac = input.get(Tensor.DT_FW);

                    double aseg = frac * expiso + (1.0 - frac) * expten;

                    output.set(i, s0 * aseg);
                }
            }
        }.init(Tensor.DT_DIM, gradients.size());
    }


    public Vect odf(double alpha, Vects samples)
    {
        Vect out = VectSource.createND(samples.size());
        for (int i = 0; i < samples.size(); i++)
        {
            Vect sample = samples.get(i);

            Matrix U = this.getMatrix().inv().times(this.feature(Tensor.FEATURES_MD).get(0));
            Vect Un = U.times(sample);
            double nUn = sample.dot(Un);
            double odf = MathUtils.zero(nUn) ? 0.0 : 1.0 / nUn;
            odf = MathUtils.eq(alpha, 1.0) ? odf : Math.pow(odf, 0.5 * (alpha + 1));

            out.set(i, odf);
        }

        return out;
    }
}