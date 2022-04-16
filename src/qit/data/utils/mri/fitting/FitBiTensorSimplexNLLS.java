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

package qit.data.utils.mri.fitting;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.models.BiTensor;
import qit.data.models.Tensor;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

import java.util.List;

public class FitBiTensorSimplexNLLS
{
    public enum BiTensorFitType
    {
        DTI,
        DTIFWE,
        Isotropic,
        FixedIsotropic,
        BothIsotropic,
        AlignedZeppelin,
        Zeppelin,
        Anisotropic,
    }

    public static final CostType DEFAULT_COST = CostType.NRMSE;
    public static final double DEFAULT_ANGLE_SCALE = 0.01;
    public static final double DEFAULT_FRAC_SCALE = 1.0;
    public static final double DEFAULT_TISSUE_SCALE = 250.0;
    public static final double DEFAULT_FLUID_SCALE = 50.0;
    public static final double DEFAULT_MAX_DIFF = 1.0;
    public static final double DEFAULT_MIN_DIFF = 0.0;
    public static final double DEFAULT_RHOBEG = 0.005;
    public static final double DEFAULT_RHOEND = 1e-7;
    public static final int DEFAULT_MAXITERS = 10000;
    public static final int DEFAULT_IPRINT = 0;

    public BiTensorFitType method = BiTensorFitType.Isotropic;
    public Gradients gradients;
    public CostType cost = DEFAULT_COST;
    public boolean weighted = false;
    public double fluidDiffMin = DEFAULT_MIN_DIFF;
    public double fluidDiffMax = DEFAULT_MAX_DIFF;
    public double scaleFrac = DEFAULT_FRAC_SCALE;
    public double scaleAngle = DEFAULT_ANGLE_SCALE;
    public double scaleTissue = DEFAULT_TISSUE_SCALE;
    public double scaleFluid = DEFAULT_FLUID_SCALE;
    public double rhobeg = DEFAULT_RHOBEG;
    public double rhoend = DEFAULT_RHOEND;
    public int maxiters = DEFAULT_MAXITERS;
    public int iprint = DEFAULT_IPRINT;
    public boolean verbose = false;

    public boolean manualInit = false;
    public double initFrac = 0.1;
    public double initFluidDiff = 0.003;
    public double initTissueDiff = 0.001;

    public VectFunction create()
    {
        switch (this.method)
        {
            case DTI:
                return createDTI();
            case DTIFWE:
                return createDTIFWE();
            default:
                return createBiTensor();
        }
    }

    public VectFunction createDTI()
    {
        FitTensorLLS lls = new FitTensorLLS();
        lls.weighted = this.weighted;
        lls.gradients = this.gradients;
        final VectFunction initter = lls.get();

        return new VectFunction()
        {
            public void apply(final Vect input, Vect output)
            {
                Tensor initTensor = new Tensor(initter.apply(input).clean());

                final double baseline = initTensor.getBaseline();
                double frac = 0;
                Matrix tD = initTensor.getMatrix();
                Matrix fD = MatrixSource.identity(3).times(0.0);

                BiTensor bitensor = new BiTensor(baseline, frac, tD, fD);
                bitensor.setFluidVec(0, bitensor.getTissueVec(0));
                bitensor.setFluidVec(1, bitensor.getTissueVec(1));
                bitensor.setFluidVec(2, bitensor.getTissueVec(2));

                output.set(bitensor.getEncoding());
            }
        }.init(this.gradients.size(), new BiTensor().getEncodingSize());
    }

    public VectFunction createDTIFWE()
    {
        FitTensorFreeWaterLLS lls = new FitTensorFreeWaterLLS();
        lls.weighted = this.weighted;
        lls.gradients = this.gradients;
        final VectFunction initter = lls.get();

        return new VectFunction()
        {
            public void apply(final Vect input, Vect output)
            {
                Tensor initTensor = new Tensor(initter.apply(input).clean());

                final double baseline = initTensor.getBaseline();
                double frac = initTensor.getFreeWater();
                Matrix tD = initTensor.getMatrix();
                Matrix fD = MatrixSource.identity(3).times(BiTensor.FREE_DIFF);

                BiTensor bitensor = new BiTensor(baseline, frac, tD, fD);
                bitensor.setFluidVec(0, bitensor.getTissueVec(0));
                bitensor.setFluidVec(1, bitensor.getTissueVec(1));
                bitensor.setFluidVec(2, bitensor.getTissueVec(2));

                output.set(bitensor.getEncoding());
            }
        }.init(this.gradients.size(), new BiTensor().getEncodingSize());
    }

    public VectFunction createBiTensor()
    {
        FitTensorFreeWaterLLS lls = new FitTensorFreeWaterLLS();
        lls.weighted = this.weighted;
        lls.gradients = this.gradients;
        final VectFunction initter = lls.get();
        final VectFunction synther = BiTensor.synth(this.gradients);
        final Parameterizer parameterizer = method();

        return new VectFunction()
        {
            public void apply(final Vect input, Vect output)
            {
                if (FitBiTensorSimplexNLLS.this.verbose)
                {
                    Logging.info("computing initial guess");
                }

                Tensor initTensor = new Tensor(initter.apply(input).clean());

                List<BiTensor> initModels = Lists.newArrayList();
                BiTensor initFWE = inits(initTensor);
                BiTensor initIsos = optimize(initFWE, synther, new BothIsotropic(), input);
                initIsos.setFluidVec(0, initFWE.getTissueVec(0));
                initIsos.setFluidVec(1, initFWE.getTissueVec(1));
                initIsos.setFluidVec(2, initFWE.getTissueVec(2));
                initIsos.setTissueVec(0, initFWE.getTissueVec(0));
                initIsos.setTissueVec(1, initFWE.getTissueVec(1));
                initIsos.setTissueVec(2, initFWE.getTissueVec(2));

                initModels.add(initFWE);
                initModels.add(initIsos);

                if (FitBiTensorSimplexNLLS.this.verbose)
                {
                    Logging.info("starting optimization with " + FitBiTensorSimplexNLLS.this.method.name());
                }

                BiTensor bestModel = null;
                Double bestError = null;
                boolean bestWentUp = false;

                for (BiTensor initModel : initModels)
                {
                    if (initModel.getFraction() < 0.1)
                    {
                        initModel.setFraction(0.1);
                    }

                    if (initModel.getFraction() > 0.90)
                    {
                        initModel.setFraction(0.90);
                    }

                    double initError = ModelUtils.cost(FitBiTensorSimplexNLLS.this.cost, gradients, input, synther.apply(initModel.getEncoding()));

                    if (FitBiTensorSimplexNLLS.this.verbose)
                    {
                        Logging.info("init error: " + initError);
                    }

                    BiTensor fitModel = optimize(initModel, synther, parameterizer, input);
                    double fitError = ModelUtils.cost(FitBiTensorSimplexNLLS.this.cost, gradients, input, synther.apply(fitModel.getEncoding()));
                    boolean wentUp = false;

                    if (FitBiTensorSimplexNLLS.this.verbose)
                    {
                        Logging.info("fit error: " + fitError);
                    }

                    if (initError < fitError)
                    {
                        fitModel = initModel;
                        fitError = initError;
                        wentUp = true;
                    }

                    if (bestError == null || fitError < bestError)
                    {
                        bestModel = fitModel;
                        bestError = fitError;
                        bestWentUp = wentUp;
                    }
                }

                if (bestWentUp)
                {
                    Logging.info("warning: error in best case went up from initial model.  check optimization params");
                }

                if (FitBiTensorSimplexNLLS.this.verbose)
                {
                    Logging.info("final error: " + bestError);
                }

                output.set(bestModel.getEncoding());
            }
        }.init(this.gradients.size(), new BiTensor().getEncodingSize());
    }

    private BiTensor inits(Tensor initTensor)
    {
        if (this.manualInit)
        {
            final double baseline = initTensor.getBaseline();
            double frac = this.initFrac;
            Matrix tD = MatrixSource.identity(3).times(this.initTissueDiff);
            Matrix fD = MatrixSource.identity(3).times(this.initFluidDiff);

            BiTensor initIso = new BiTensor(baseline, frac, tD, fD);
            initIso.setTissueVec(0, initIso.getTissueVec(0));
            initIso.setTissueVec(1, initIso.getTissueVec(1));
            initIso.setTissueVec(2, initIso.getTissueVec(2));
            initIso.setTissueVal(0, this.initTissueDiff);
            initIso.setTissueVal(1, this.initTissueDiff);
            initIso.setTissueVal(2, this.initTissueDiff);
            initIso.setFluidVec(0, initIso.getTissueVec(0));
            initIso.setFluidVec(1, initIso.getTissueVec(1));
            initIso.setFluidVec(2, initIso.getTissueVec(2));
            initIso.setFluidVal(0, this.initFluidDiff);
            initIso.setFluidVal(1, this.initFluidDiff);
            initIso.setFluidVal(2, this.initFluidDiff);
            return initIso;
        }
        else
        {
            final double baseline = initTensor.getBaseline();
            double frac = initTensor.getFreeWater();
            Matrix tD = initTensor.getMatrix();
            Matrix fD = MatrixSource.identity(3).times(BiTensor.FREE_DIFF);

            BiTensor initIso = new BiTensor(baseline, frac, tD, fD);
            initIso.setFluidVec(0, initIso.getTissueVec(0));
            initIso.setFluidVec(1, initIso.getTissueVec(1));
            initIso.setFluidVec(2, initIso.getTissueVec(2));
            initIso.setFluidVal(0, BiTensor.FREE_DIFF);
            initIso.setFluidVal(1, BiTensor.FREE_DIFF);
            initIso.setFluidVal(2, BiTensor.FREE_DIFF);

            if (!this.method.name().contains("Isotropic"))
            {
                initIso.setFluidVal(0, 1.25 * Tensor.FREE_DIFF);
                initIso.setFluidVal(1, 0.75 * Tensor.FREE_DIFF);
                initIso.setFluidVal(2, 0.75 * Tensor.FREE_DIFF);
            }

            return initIso;
        }
    }

    private BiTensor optimize(BiTensor initModel, VectFunction synther, Parameterizer parameterizer, Vect sig)
    {
        Vect initParam = parameterizer.param(initModel).clean();

        double[] x = initParam.toArray();
        int np = parameterizer.numparam();
        int nc = parameterizer.numcon();

        Calcfc func = new Calcfc()
        {
            @Override
            public double Compute(int n, int m, double[] x, double[] con)
            {
                BiTensor model = parameterizer.model(initModel, new Vect(x));
                Vect pred = synther.apply(model.getEncoding());
                double cost = ModelUtils.cost(FitBiTensorSimplexNLLS.this.cost, gradients, sig, pred);

                parameterizer.constraints(x, con);

                return cost;
            }
        };
        Cobyla.FindMinimum(func, np, nc, x, this.rhobeg, this.rhoend, this.iprint, this.maxiters);

        BiTensor fitModel = parameterizer.model(initModel, new Vect(x));

        return fitModel;
    }

    private Parameterizer method()
    {
        switch (this.method)
        {
            case Isotropic:
                return new Isotropic();
            case BothIsotropic:
                return new BothIsotropic();
            case Zeppelin:
                return new Zeppelin();
            case AlignedZeppelin:
                return new AlignedZeppelin();
            case Anisotropic:
                return new Anisotropic();
            default:
                throw new RuntimeException("unimplemented method: " + this.method);
        }
    }

    private static abstract class Parameterizer
    {
        abstract int numparam();

        abstract Vect param(BiTensor tensor);

        abstract BiTensor model(BiTensor init, Vect x);

        int numcon()
        {
            return 0;
        }

        void constraints(double[] x, double[] con)
        {
        }
    }

    private class Anisotropic extends Parameterizer
    {
        int numparam()
        {
            return 13;
        }

        Vect param(BiTensor tensor)
        {
            Vect out = VectSource.createND(this.numparam());

            double frac = tensor.getFraction();

            Matrix tissue = MatrixUtils.cholesky(tensor.getTissueMatrix());
            Matrix fluid = MatrixUtils.cholesky(tensor.getFluidMatrix());

            out.set(0,  frac * FitBiTensorSimplexNLLS.this.scaleFrac);
            out.set(1,  tissue.get(0, 0) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(2,  tissue.get(1, 1) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(3,  tissue.get(2, 2) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(4,  tissue.get(1, 0) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(5,  tissue.get(2, 0) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(6,  tissue.get(2, 1) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(7,  fluid.get(0, 0) * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(8,  fluid.get(1, 1) * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(9,  fluid.get(2, 2) * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(10, fluid.get(1, 0) * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(11, fluid.get(2, 0) * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(12, fluid.get(2, 1) * FitBiTensorSimplexNLLS.this.scaleFluid);

            return out;
        }

        BiTensor model(BiTensor init, Vect param)
        {
            BiTensor out = init.copy();

            double frac = param.get(0) / FitBiTensorSimplexNLLS.this.scaleFrac;

            Matrix tC = new Matrix(3, 3);
            tC.set(0, 0, param.get(1) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(1, 1, param.get(2) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(2, 2, param.get(3) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(1, 0, param.get(4) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(2, 0, param.get(5) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(2, 1, param.get(6) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(1, 0, tC.get(0, 1));
            tC.set(2, 0, tC.get(0, 2));
            tC.set(2, 1, tC.get(1, 2));
            Matrix tD = tC.times(tC.transpose());

            Matrix fC = new Matrix(3, 3);
            fC.set(0, 0, param.get(7) / FitBiTensorSimplexNLLS.this.scaleFluid);
            fC.set(1, 1, param.get(8) / FitBiTensorSimplexNLLS.this.scaleFluid);
            fC.set(2, 2, param.get(9) / FitBiTensorSimplexNLLS.this.scaleFluid);
            fC.set(1, 0, param.get(10) / FitBiTensorSimplexNLLS.this.scaleFluid);
            fC.set(2, 0, param.get(11) / FitBiTensorSimplexNLLS.this.scaleFluid);
            fC.set(2, 1, param.get(12) / FitBiTensorSimplexNLLS.this.scaleFluid);
            fC.set(1, 0, fC.get(0, 1));
            fC.set(2, 0, fC.get(0, 2));
            fC.set(2, 1, fC.get(1, 2));
            Matrix fD = fC.times(fC.transpose());

            out.setFraction(frac);
            out.setTissueMatrix(tD);
            out.setFluidMatrix(fD);

            return out;
        }

        int numcon()
        {
            return 2;
        }

        void constraints(double[] x, double[] con)
        {
            con[0] = x[0];
            con[1] =  FitBiTensorSimplexNLLS.this.scaleFrac - x[0];
        }
    }

    private class Zeppelin extends Parameterizer
    {
        int numparam()
        {
            return 11;
        }

        Vect param(BiTensor tensor)
        {
            Vect out = VectSource.createND(this.numparam());

            double frac = tensor.getFraction();

            Matrix tissue = MatrixUtils.cholesky(tensor.getTissueMatrix());

            Matrix fluid = tensor.getFluidMatrix();
            MatrixUtils.EigenDecomp weig = MatrixUtils.eig(fluid);

            double wad = weig.values.get(0);
            double wrd = 0.5 * (weig.values.get(1) + weig.values.get(2));

            Vect wdir = weig.vectors.get(0);
            double wtheta = Math.acos(wdir.getZ());
            double wphi = Math.atan2(wdir.getY(), wdir.getX());

            out.set(0, frac * FitBiTensorSimplexNLLS.this.scaleFrac);
            out.set(1, wad * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(2, wrd * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(3, wtheta * FitBiTensorSimplexNLLS.this.scaleAngle);
            out.set(4, wphi * FitBiTensorSimplexNLLS.this.scaleAngle);
            out.set(5, tissue.get(0, 0) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(6, tissue.get(1, 1) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(7, tissue.get(2, 2) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(8, tissue.get(0, 1) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(9, tissue.get(0, 1) * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(10, tissue.get(1, 2) * FitBiTensorSimplexNLLS.this.scaleTissue);

            return out;
        }

        BiTensor model(BiTensor init, Vect param)
        {
            double frac = param.get(0) / FitBiTensorSimplexNLLS.this.scaleFrac;
            double wad = param.get(1) / FitBiTensorSimplexNLLS.this.scaleFluid;
            double wrd = param.get(2) / FitBiTensorSimplexNLLS.this.scaleFluid;
            double wtheta = param.get(3) / FitBiTensorSimplexNLLS.this.scaleAngle;
            double wphi = param.get(4) / FitBiTensorSimplexNLLS.this.scaleAngle;

            double wx = Math.sin(wtheta) * Math.cos(wphi);
            double wy = Math.sin(wtheta) * Math.sin(wphi);
            double wz = Math.cos(wtheta);

            Vect w1 = VectSource.create3D(wx, wy, wz);
            Vect w2 = w1.perp();
            Vect w3 = w1.cross(w2);

            Matrix tC = new Matrix(3, 3);
            tC.set(0, 0, param.get(5) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(1, 1, param.get(6) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(2, 2, param.get(7) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(0, 1, param.get(8) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(0, 2, param.get(9) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(1, 2, param.get(10) / FitBiTensorSimplexNLLS.this.scaleTissue);
            tC.set(1, 0, tC.get(0, 1));
            tC.set(2, 0, tC.get(0, 2));
            tC.set(2, 1, tC.get(1, 2));
            Matrix tD = tC.times(tC.transpose());

            BiTensor out = init.copy();
            out.setFraction(frac);
            out.setTissueMatrix(tD);
            out.setFluidVal(0, wad);
            out.setFluidVal(1, wrd);
            out.setFluidVal(2, wrd);
            out.setFluidVec(0, w1);
            out.setFluidVec(1, w2);
            out.setFluidVec(2, w3);

            return out;
        }

        int numcon()
        {
            return 7;
        }

        void constraints(double[] x, double[] con)
        {
            double scale = FitBiTensorSimplexNLLS.this.scaleFluid;

            con[0] = x[0];
            con[1] =  FitBiTensorSimplexNLLS.this.scaleFrac - x[0];
            con[2] = x[1] - FitBiTensorSimplexNLLS.this.fluidDiffMin * scale;
            con[3] = FitBiTensorSimplexNLLS.this.fluidDiffMax * scale - x[1];
            con[4] = x[2] - FitBiTensorSimplexNLLS.this.fluidDiffMin * scale;
            con[5] = FitBiTensorSimplexNLLS.this.fluidDiffMax * scale - x[2];
            con[6] = x[1] - x[2];
        }
    }

    private class AlignedZeppelin extends Parameterizer
    {
        int numparam()
        {
            return 6;
        }

        Vect param(BiTensor tensor)
        {
            Vect out = VectSource.createND(this.numparam());

            double frac = tensor.getFraction();

            Matrix tissue = tensor.getTissueMatrix();
            MatrixUtils.EigenDecomp teig = MatrixUtils.eig(tissue);

            double tv1 = teig.values.get(0);
            double tv2 = teig.values.get(1);
            double tv3 = teig.values.get(2);

            Matrix fluid = tensor.getFluidMatrix();
            MatrixUtils.EigenDecomp feig = MatrixUtils.eig(fluid);

            double wad = feig.values.get(0);
            double wrd = 0.5 * (feig.values.get(1) + feig.values.get(2));

            out.set(0, frac * FitBiTensorSimplexNLLS.this.scaleFrac);
            out.set(1, wad * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(2, wrd * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(3, tv1 * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(4, tv2 * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(5, tv3 * FitBiTensorSimplexNLLS.this.scaleTissue);

            return out;
        }

        BiTensor model(BiTensor init, Vect param)
        {
            double frac = param.get(0) / FitBiTensorSimplexNLLS.this.scaleFrac;

            double fad = param.get(1) / FitBiTensorSimplexNLLS.this.scaleFluid;
            double frd = param.get(2) / FitBiTensorSimplexNLLS.this.scaleFluid;

            double tv1 = param.get(3) / FitBiTensorSimplexNLLS.this.scaleTissue;
            double tv2 = param.get(4) / FitBiTensorSimplexNLLS.this.scaleTissue;
            double tv3 = param.get(5) / FitBiTensorSimplexNLLS.this.scaleTissue;

            BiTensor out = init.copy();
            out.setFraction(frac);
            out.setFluidVal(0, fad);
            out.setFluidVal(1, frd);
            out.setFluidVal(2, frd);
            out.setFluidVec(0, init.getTissueVec(0));
            out.setFluidVec(1, init.getTissueVec(1));
            out.setFluidVec(2, init.getTissueVec(2));
            out.setTissueVal(0, tv1);
            out.setTissueVal(1, tv2);
            out.setTissueVal(2, tv3);
            out.setTissueVec(0, init.getTissueVec(0));
            out.setTissueVec(1, init.getTissueVec(1));
            out.setTissueVec(2, init.getTissueVec(2));

            return out;
        }

        int numcon()
        {
            return 13;
        }

        void constraints(double[] x, double[] con)
        {
            double scale = FitBiTensorSimplexNLLS.this.scaleFluid;

            con[0] = x[0];
            con[1] = FitBiTensorSimplexNLLS.this.scaleFrac - x[0];
            con[2] = x[1] - FitBiTensorSimplexNLLS.this.fluidDiffMin * scale;
            con[3] = FitBiTensorSimplexNLLS.this.fluidDiffMax * scale - x[1];
            con[4] = x[2] - FitBiTensorSimplexNLLS.this.fluidDiffMin * scale;
            con[5] = FitBiTensorSimplexNLLS.this.fluidDiffMax * scale - x[2];
            con[6] = x[1] - x[2];
            con[7] = x[3] - x[4];
            con[8] = x[4] - x[5];
            con[9] = x[3] - x[5];
            con[10] = x[3];
            con[11] = x[4];
            con[12] = x[5];
        }
    }

    private class Isotropic extends Parameterizer
    {
        int numparam()
        {
            return 5;
        }

        Vect param(BiTensor tensor)
        {
            Vect out = VectSource.createND(this.numparam());

            double frac = tensor.getFraction();
            double fluid = MatrixUtils.eig(tensor.getFluidMatrix()).values.mean();

            Matrix tissue = tensor.getTissueMatrix();
            MatrixUtils.EigenDecomp teig = MatrixUtils.eig(tissue);

            double tv1 = teig.values.get(0);
            double tv2 = teig.values.get(1);
            double tv3 = teig.values.get(2);

            out.set(0, frac * FitBiTensorSimplexNLLS.this.scaleFrac);
            out.set(1, fluid * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(2, tv1 * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(3, tv2 * FitBiTensorSimplexNLLS.this.scaleTissue);
            out.set(4, tv3 * FitBiTensorSimplexNLLS.this.scaleTissue);
            return out;
        }

        BiTensor model(BiTensor init, Vect param)
        {
            BiTensor out = init.copy();

            double frac = param.get(0) / FitBiTensorSimplexNLLS.this.scaleFrac;
            double fluid = param.get(1) / FitBiTensorSimplexNLLS.this.scaleFluid;

            double tv1 = param.get(2) / FitBiTensorSimplexNLLS.this.scaleTissue;
            double tv2 = param.get(3) / FitBiTensorSimplexNLLS.this.scaleTissue;
            double tv3 = param.get(4) / FitBiTensorSimplexNLLS.this.scaleTissue;

            out.setFraction(frac);
            out.setFluidVal(0, fluid);
            out.setFluidVal(1, fluid);
            out.setFluidVal(2, fluid);
            out.setTissueVal(0, tv1);
            out.setTissueVal(1, tv2);
            out.setTissueVal(2, tv3);

            return out;
        }

        int numcon()
        {
            return 10;
        }

        void constraints(double[] x, double[] con)
        {
            double scale = FitBiTensorSimplexNLLS.this.scaleFluid;

            con[0] = x[0];
            con[1] =  FitBiTensorSimplexNLLS.this.scaleFrac - x[0];
            con[2] = x[1] - FitBiTensorSimplexNLLS.this.fluidDiffMin * scale;
            con[3] = FitBiTensorSimplexNLLS.this.fluidDiffMax * scale - x[1];
            con[4] = x[2] - x[3];
            con[5] = x[3] - x[4];
            con[6] = x[2] - x[4];
            con[7] = x[2];
            con[8] = x[3];
            con[9] = x[4];
        }
    }

    private class BothIsotropic extends Parameterizer
    {
        int numparam()
        {
            return 3;
        }

        Vect param(BiTensor tensor)
        {
            Vect out = VectSource.createND(this.numparam());

            double frac = tensor.getFraction();
            double fluid = MatrixUtils.eig(tensor.getFluidMatrix()).values.mean();
            double tissue = MatrixUtils.eig(tensor.getTissueMatrix()).values.mean();

            out.set(0, frac * FitBiTensorSimplexNLLS.this.scaleFrac);
            out.set(1, fluid * FitBiTensorSimplexNLLS.this.scaleFluid);
            out.set(2, tissue * FitBiTensorSimplexNLLS.this.scaleTissue);

            return out;
        }

        BiTensor model(BiTensor init, Vect param)
        {
            BiTensor out = init.copy();

            double frac = param.get(0) / FitBiTensorSimplexNLLS.this.scaleFrac;
            double fluid = param.get(1) / FitBiTensorSimplexNLLS.this.scaleFluid;
            double tissue = param.get(2) / FitBiTensorSimplexNLLS.this.scaleTissue;

            out.setFraction(frac);
            out.setFluidVal(0, fluid);
            out.setFluidVal(1, fluid);
            out.setFluidVal(2, fluid);
            out.setTissueVal(0, tissue);
            out.setTissueVal(1, tissue);
            out.setTissueVal(2, tissue);

            return out;
        }

        int numcon()
        {
            return 6;
        }

        void constraints(double[] x, double[] con)
        {
            double scale = FitBiTensorSimplexNLLS.this.scaleFluid;

            con[0] = x[0];
            con[1] =  FitBiTensorSimplexNLLS.this.scaleFrac - x[0];
            con[2] = x[1] - scale * FitBiTensorSimplexNLLS.this.fluidDiffMin;
            con[3] = scale * FitBiTensorSimplexNLLS.this.fluidDiffMax - x[1];
            con[4] = x[1];
            con[5] = x[2];
        }
    }
}