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
import qit.base.Model;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;

import java.util.List;

public class BiTensor extends Model<BiTensor>
{
    public final static String NAME = "bdti";
    public final static String COLOR = "color";

    public static double FREE_DIFF = 3.0e-3;

    // how parameters are stored as vectors
    public final static int BDT_S0 = 0;
    public final static int BDT_DOT = 1;
    public final static int BDT_FRAC = 2;
    public final static int BDT_TXX = 3;
    public final static int BDT_TXY = 4;
    public final static int BDT_TYY = 5;
    public final static int BDT_TXZ = 6;
    public final static int BDT_TYZ = 7;
    public final static int BDT_TZZ = 8;
    public final static int BDT_FXX = 9;
    public final static int BDT_FXY = 10;
    public final static int BDT_FYY = 11;
    public final static int BDT_FXZ = 12;
    public final static int BDT_FYZ = 13;
    public final static int BDT_FZZ = 14;
    public final static int BDT_DIM = 15;

    public final static String FEATURES_S0 = "S0";
    public final static String FEATURES_DOT = "dot";
    public final static String FEATURES_FRAC = "frac";
    public final static String FEATURES_TFA = "tFA";
    public final static String FEATURES_TMD = "tMD";
    public final static String FEATURES_TRD = "tRD";
    public final static String FEATURES_TAD = "tAD";
    public final static String FEATURES_TCP = "tCP";
    public final static String FEATURES_TCL = "tCL";
    public final static String FEATURES_TCS = "tCS";
    public final static String FEATURES_TPD = "tPD";
    public final static String FEATURES_FFA = "fFA";
    public final static String FEATURES_FMD = "fMD";
    public final static String FEATURES_FRD = "fRD";
    public final static String FEATURES_FAD = "fAD";
    public final static String FEATURES_FCP = "fCP";
    public final static String FEATURES_FCL = "fCL";
    public final static String FEATURES_FCS = "fCS";
    public final static String FEATURES_FPD = "fPD";
    public final static String[] FEATURES = {
            FEATURES_S0,
            FEATURES_DOT,
            FEATURES_FRAC,
            FEATURES_TFA,
            FEATURES_TMD,
            FEATURES_TRD,
            FEATURES_TAD,
            FEATURES_TCP,
            FEATURES_TCL,
            FEATURES_TCS,
            FEATURES_TPD,
            FEATURES_FFA,
            FEATURES_FMD,
            FEATURES_FRD,
            FEATURES_FAD,
            FEATURES_FCP,
            FEATURES_FCL,
            FEATURES_FCS,
            FEATURES_FPD};

    public Double s0;
    public Double dot;
    public Double frac;

    public Vects tvecs;
    public Vect tvals;

    public Vects fvecs;
    public Vect fvals;

    public static boolean matches(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lname = name.toLowerCase();
        return (lname.contains(".bti") || lname.contains(".bdti") || lname.contains(".bitensor"));
    }

    public BiTensor()
    {
        this.s0 = 0.0;
        this.dot = 0.0;
        this.frac = 0.0;

        this.tvecs = new Vects();
        this.tvecs.add(VectSource.create3D());
        this.tvecs.add(VectSource.create3D());
        this.tvecs.add(VectSource.create3D());

        this.tvals = VectSource.create3D();

        this.fvecs = new Vects();
        this.fvecs.add(VectSource.create3D());
        this.fvecs.add(VectSource.create3D());
        this.fvecs.add(VectSource.create3D());

        this.fvals = VectSource.create3D();
    }

    public BiTensor(Vect encoding)
    {
        this();
        this.setEncoding(encoding);
    }

    public BiTensor(double s0, double frac, Matrix tissue, Matrix fluid)
    {
        this();
        this.set(s0, frac, tissue, fluid);
    }

    public double baseline()
    {
        return this.s0;
    }

    public Vect getFluidVec(int i)
    {
        return this.fvecs.get(i).copy();
    }

    public double getFluidVal(int i)
    {
        return this.fvals.get(i);
    }

    public Vect getFluidVals()
    {
        return this.fvals.copy();
    }

    public void setFluidVals(Vect v)
    {
        this.fvals.set(v);
    }

    public Vects getFluidVecs()
    {
        return this.fvecs.copy();
    }

    public Vect getTissueVec(int i)
    {
        return this.tvecs.get(i).copy();
    }

    public double getTissueVal(int i)
    {
        return this.tvals.get(i);
    }

    public Vect getTissueVals()
    {
        return this.tvals.copy();
    }

    public void setTissueVals(Vect v)
    {
        this.tvals.set(v);
    }

    public Vects getTissueVecs()
    {
        return this.tvecs.copy();
    }

    public double getFraction()
    {
        return this.frac;
    }

    public double getDot()
    {
        return this.dot;
    }

    public BiTensor setDot(double e)
    {
        this.dot = e;
        return this;
    }

    public BiTensor setFraction(double e)
    {
        this.frac = e;
        return this;
    }

    public BiTensor setBaseline(double v)
    {
        this.s0 = v;
        return this;
    }

    public double getBaseline()
    {
        return this.s0;
    }

    public BiTensor setTissueVec(int i, Vect v)
    {
        this.tvecs.get(i).set(v);
        return this;
    }

    public BiTensor setTissueVal(int i, double v)
    {
        this.tvals.set(i, v);
        return this;
    }

    public BiTensor setFluidVec(int i, Vect v)
    {
        this.fvecs.get(i).set(v);
        return this;
    }

    public BiTensor setFluidVal(int i, double v)
    {
        this.fvals.set(i, v);
        return this;
    }

    public void setFluidMatrix(Matrix fluid)
    {
        try
        {
            EigenDecomp eig = MatrixUtils.eig(fluid);
            this.fvals.set(eig.values);
            for (int i = 0; i < 3; i++)
            {
                this.fvecs.get(i).set(eig.vectors.get(i));
            }
        }
        catch (RuntimeException e)
        {
            Logging.info("farning: eig failed");
            this.fvals.setAll(0);
            this.fvecs.get(0).set(VectSource.randomUnit());
            this.fvecs.get(1).set(this.fvecs.get(0).perp());
            this.fvecs.get(2).set(this.fvecs.get(0).cross(this.fvecs.get(1)));
        }
    }

    public void setTissueMatrix(Matrix tissue)
    {
        try
        {
            EigenDecomp eig = MatrixUtils.eig(tissue);
            this.tvals.set(eig.values);
            for (int i = 0; i < 3; i++)
            {
                this.tvecs.get(i).set(eig.vectors.get(i));
            }
        }
        catch (RuntimeException e)
        {
            Logging.info("farning: eig failed");
            this.tvals.setAll(0);
            this.tvecs.get(0).set(VectSource.randomUnit());
            this.tvecs.get(1).set(this.tvecs.get(0).perp());
            this.tvecs.get(2).set(this.tvecs.get(0).cross(this.tvecs.get(1)));
        }
    }

    public void set(double s0, double frac, Matrix tissue, Matrix fluid)
    {
        this.s0 = s0;
        this.frac = frac;
        this.setTissueMatrix(tissue);
        this.setFluidMatrix(fluid);
    }

    public BiTensor set(BiTensor t)
    {
        this.s0 = t.s0;
        this.dot = t.dot;
        this.frac = t.frac;
        this.tvals.set(t.tvals);
        for (int i = 0; i < 3; i++)
        {
            this.tvecs.get(i).set(t.tvecs.get(i));
        }
        this.fvals.set(t.fvals);
        for (int i = 0; i < 3; i++)
        {
            this.fvecs.get(i).set(t.fvecs.get(i));
        }

        return this;
    }

    public BiTensor copy()
    {
        BiTensor out = new BiTensor();
        out.set(this);
        return out;
    }

    public BiTensor proto()
    {
        return new BiTensor();
    }

    public int getDegreesOfFreedom()
    {
        return BDT_DIM;
    }

    public int getEncodingSize()
    {
        return BDT_DIM;
    }

    public Matrix getTissueMatrix()
    {
        return MatrixSource.eig(this.tvals, this.tvecs);
    }

    public Matrix getFluidMatrix()
    {
        return MatrixSource.eig(this.fvals, this.fvecs);
    }

    public BiTensor setEncoding(Vect encoding)
    {
        double s0 = encoding.get(BDT_S0);
        double dot = encoding.get(BDT_DOT);
        double frac = encoding.get(BDT_FRAC);

        Matrix tissue = new Matrix(3, 3);
        tissue.set(0, 0, encoding.get(BDT_TXX));
        tissue.set(1, 1, encoding.get(BDT_TYY));
        tissue.set(2, 2, encoding.get(BDT_TZZ));
        tissue.set(0, 1, encoding.get(BDT_TXY));
        tissue.set(1, 0, encoding.get(BDT_TXY));
        tissue.set(0, 2, encoding.get(BDT_TXZ));
        tissue.set(2, 0, encoding.get(BDT_TXZ));
        tissue.set(1, 2, encoding.get(BDT_TYZ));
        tissue.set(2, 1, encoding.get(BDT_TYZ));

        Matrix fluid = new Matrix(3, 3);
        fluid.set(0, 0, encoding.get(BDT_FXX));
        fluid.set(1, 1, encoding.get(BDT_FYY));
        fluid.set(2, 2, encoding.get(BDT_FZZ));
        fluid.set(0, 1, encoding.get(BDT_FXY));
        fluid.set(1, 0, encoding.get(BDT_FXY));
        fluid.set(0, 2, encoding.get(BDT_FXZ));
        fluid.set(2, 0, encoding.get(BDT_FXZ));
        fluid.set(1, 2, encoding.get(BDT_FYZ));
        fluid.set(2, 1, encoding.get(BDT_FYZ));

        this.setBaseline(s0);
        this.setDot(dot);
        this.setFraction(frac);
        this.setTissueMatrix(tissue);
        this.setFluidMatrix(fluid);

        return this;
    }

    public void getEncoding(Vect encoding)
    {
        encoding.set(BDT_S0, this.s0);
        encoding.set(BDT_DOT, this.dot);
        encoding.set(BDT_FRAC, this.frac);

        Matrix tissue = getTissueMatrix();
        encoding.set(BDT_TXX, tissue.get(0, 0));
        encoding.set(BDT_TYY, tissue.get(1, 1));
        encoding.set(BDT_TZZ, tissue.get(2, 2));
        encoding.set(BDT_TXY, tissue.get(0, 1));
        encoding.set(BDT_TXZ, tissue.get(0, 2));
        encoding.set(BDT_TYZ, tissue.get(1, 2));

        Matrix fluid = getFluidMatrix();
        encoding.set(BDT_FXX, fluid.get(0, 0));
        encoding.set(BDT_FYY, fluid.get(1, 1));
        encoding.set(BDT_FZZ, fluid.get(2, 2));
        encoding.set(BDT_FXY, fluid.get(0, 1));
        encoding.set(BDT_FXZ, fluid.get(0, 2));
        encoding.set(BDT_FYZ, fluid.get(1, 2));
    }

    public double dist(BiTensor t)
    {
        Matrix ta = MatrixSource.eig(this.tvals, this.tvecs);
        Matrix tb = MatrixSource.eig(t.tvals, t.tvecs);
        double tc = ta.minus(tb).norm2();

        Matrix wa = MatrixSource.eig(this.fvals, this.fvecs);

        Matrix wb = MatrixSource.eig(t.fvals, t.fvecs);
        double wc = wa.minus(wb).norm2();

        double fc = Math.log(this.frac / t.frac);

        double left = tc * (1.0 - this.frac) + this.frac * wc;
        double right = tc * (1.0 - t.frac) + t.frac * wc;

        return fc + left + right;
    }

    public Vect feature(String name)
    {
        if (FEATURES_FFA.equals(name))
        {
            return new Tensor(this.s0, this.getFluidMatrix()).feature(Tensor.FEATURES_FA);
        }
        else if (FEATURES_FMD.equals(name))
        {
            return new Tensor(this.s0, this.getFluidMatrix()).feature(Tensor.FEATURES_MD);
        }
        else if (FEATURES_FRD.equals(name))
        {
            return new Tensor(this.s0, this.getFluidMatrix()).feature(Tensor.FEATURES_RD);
        }
        else if (FEATURES_FAD.equals(name))
        {
            return new Tensor(this.s0, this.getFluidMatrix()).feature(Tensor.FEATURES_AD);
        }
        else if (FEATURES_FCP.equals(name))
        {
            return new Tensor(this.s0, this.getFluidMatrix()).feature(Tensor.FEATURES_CP);
        }
        else if (FEATURES_FCL.equals(name))
        {
            return new Tensor(this.s0, this.getFluidMatrix()).feature(Tensor.FEATURES_CL);
        }
        else if (FEATURES_FCS.equals(name))
        {
            return new Tensor(this.s0, this.getFluidMatrix()).feature(Tensor.FEATURES_CS);
        }
        else if (FEATURES_FPD.equals(name))
        {
            return new Tensor(this.s0, this.getFluidMatrix()).feature(Tensor.FEATURES_PD);
        }
        if (FEATURES_TFA.equals(name))
        {
            return new Tensor(this.s0, this.getTissueMatrix()).feature(Tensor.FEATURES_FA);
        }
        else if (FEATURES_TMD.equals(name))
        {
            return new Tensor(this.s0, this.getTissueMatrix()).feature(Tensor.FEATURES_MD);
        }
        else if (FEATURES_TRD.equals(name))
        {
            return new Tensor(this.s0, this.getTissueMatrix()).feature(Tensor.FEATURES_RD);
        }
        else if (FEATURES_TAD.equals(name))
        {
            return new Tensor(this.s0, this.getTissueMatrix()).feature(Tensor.FEATURES_AD);
        }
        else if (FEATURES_TCP.equals(name))
        {
            return new Tensor(this.s0, this.getTissueMatrix()).feature(Tensor.FEATURES_CP);
        }
        else if (FEATURES_TCL.equals(name))
        {
            return new Tensor(this.s0, this.getTissueMatrix()).feature(Tensor.FEATURES_CL);
        }
        else if (FEATURES_TCS.equals(name))
        {
            return new Tensor(this.s0, this.getTissueMatrix()).feature(Tensor.FEATURES_CS);
        }
        else if (FEATURES_TPD.equals(name))
        {
            return new Tensor(this.s0, this.getTissueMatrix()).feature(Tensor.FEATURES_PD);
        }
        else if (FEATURES_S0.equals(name))
        {
            return VectSource.create1D(this.s0);
        }
        else if (FEATURES_FRAC.equals(name))
        {
            return VectSource.create1D(this.frac);
        }
        else if (FEATURES_DOT.equals(name))
        {
            return VectSource.create1D(this.dot);
        }
        else
        {
            throw new RuntimeException("invalid index: " + name);
        }
    }

    public List<String> features()
    {
        return Lists.newArrayList(FEATURES);
    }

    public BiTensor getThis()
    {
        return this;
    }

    public static VectFunction synth(final Gradients gradients)
    {
        final Matrix tissue = new Matrix(gradients.size(), BDT_DIM);
        final Matrix fluid = new Matrix(gradients.size(), BDT_DIM);

        for (int i = 0; i < gradients.size(); i++)
        {
            double b = gradients.getBval(i);
            Vect g = gradients.getBvec(i);

            double gx = g.get(0);
            double gy = g.get(1);
            double gz = g.get(2);

            tissue.set(i, BDT_TXX, -b * gx * gx);
            tissue.set(i, BDT_TYY, -b * gy * gy);
            tissue.set(i, BDT_TZZ, -b * gz * gz);
            tissue.set(i, BDT_TXY, -b * 2 * gx * gy);
            tissue.set(i, BDT_TYZ, -b * 2 * gy * gz);
            tissue.set(i, BDT_TXZ, -b * 2 * gx * gz);

            fluid.set(i, BDT_FXX, -b * gx * gx);
            fluid.set(i, BDT_FYY, -b * gy * gy);
            fluid.set(i, BDT_FZZ, -b * gz * gz);
            fluid.set(i, BDT_FXY, -b * 2 * gx * gy);
            fluid.set(i, BDT_FYZ, -b * 2 * gy * gz);
            fluid.set(i, BDT_FXZ, -b * 2 * gx * gz);
        }

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double s0 = input.get(BiTensor.BDT_S0);
                double dot = input.get(BiTensor.BDT_DOT);
                double frac = input.get(BiTensor.BDT_FRAC);

                Vect fdot = fluid.times(input);
                Vect tdot = tissue.times(input);

                for (int i = 0; i < gradients.size(); i++)
                {
                    double fsig = Math.exp(fdot.get(i));
                    double tsig = Math.exp(tdot.get(i));
                    double nsig = (1.0 - frac - dot) * tsig + frac * fsig + dot;

                    output.set(i, s0 * nsig);
                }
            }
        }.init(BiTensor.BDT_DIM, gradients.size());
    }

    public static double rmse(Vect signal, Gradients gradients, Vect param)
    {
        double s0 = param.get(BiTensor.BDT_S0);
        double mse = 0;

        for (int i = 0; i < gradients.size(); i++)
        {
            double b = gradients.getBval(i);
            Vect g = gradients.getBvec(i);

            double gx = g.get(0);
            double gy = g.get(1);
            double gz = g.get(2);

            Vect tissue = VectSource.createND(BiTensor.BDT_DIM);
            tissue.set(BiTensor.BDT_TXX, -b * gx * gx);
            tissue.set(BiTensor.BDT_TYY, -b * gy * gy);
            tissue.set(BiTensor.BDT_TZZ, -b * gz * gz);
            tissue.set(BiTensor.BDT_TXY, -b * 2 * gx * gy);
            tissue.set(BiTensor.BDT_TYZ, -b * 2 * gy * gz);
            tissue.set(BiTensor.BDT_TXZ, -b * 2 * gx * gz);

            Vect fluid = VectSource.createND(BiTensor.BDT_DIM);
            fluid.set(BiTensor.BDT_FXX, -b * gx * gx);
            fluid.set(BiTensor.BDT_FYY, -b * gy * gy);
            fluid.set(BiTensor.BDT_FZZ, -b * gz * gz);
            fluid.set(BiTensor.BDT_FXY, -b * 2 * gx * gy);
            fluid.set(BiTensor.BDT_FYZ, -b * 2 * gy * gz);
            fluid.set(BiTensor.BDT_FXZ, -b * 2 * gx * gz);

            double frac = param.get(BiTensor.BDT_FRAC);

            double wsig = Math.exp(param.dot(fluid));
            double tsig = Math.exp(param.dot(tissue));
            double nsig = (1.0 - frac) * tsig + frac * wsig;
            double si = s0 * nsig;

            double ds = si - signal.get(i);
            mse += ds * ds / gradients.size();
        }

        double rmse = Math.sqrt(mse);

        return rmse;
    }
}