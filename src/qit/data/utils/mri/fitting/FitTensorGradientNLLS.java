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

import qit.base.Logging;
import qit.base.structs.Pair;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.models.Tensor;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

public class FitTensorGradientNLLS
{
    public static final String NAME = "nlls";

    // use a cholesky decomposition to enforce positive definiteness
    private static final int CHOLXX_IDX = 0;
    private static final int CHOLYY_IDX = 1;
    private static final int CHOLZZ_IDX = 2;
    private static final int CHOLXY_IDX = 3;
    private static final int CHOLYZ_IDX = 4;
    private static final int CHOLXZ_IDX = 5;
    private static final int DIM = 6;

    public Gradients gradients;
    public double gamma = 1e-3;
    public double thresh = 1e-9;
    public int maxiter = 10000;
    public VectFunction output;

    public FitTensorGradientNLLS run()
    {
        FitTensorLLS source = new FitTensorLLS();
        source.gradients = this.gradients;
        final VectFunction lls = source.get();

        this.output = new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double base = ModelUtils.baselineStats(FitTensorGradientNLLS.this.gradients, input).mean;
                base = MathUtils.zero(base) ? 1.0 : base;
                Vect meas = input.times(1.0 / base);
                Vect param = param(new Tensor(lls.apply(input)));

                if (param.infinite())
                {
                    param.setAll(0);
                    param.set(0, 0.001);
                    param.set(1, 0.0001);
                    param.set(2, 0.00001);
                }

                Pair<Double,Vect> current = update(FitTensorGradientNLLS.this.gradients, param, meas);
                double starting = current.a;

                if (!Double.isNaN(starting) && !MathUtils.zero(starting))
                {
                    int iter = 0;
                    int code = 0;

                    while (true)
                    {
                        Vect nparam = param.plus(-1.0 * FitTensorGradientNLLS.this.gamma, current.b);
                        constrain(nparam);

                        Pair<Double, Vect> update = update(FitTensorGradientNLLS.this.gradients, nparam, meas);
                        double change = current.a - update.a;

                        if (change < FitTensorGradientNLLS.this.thresh)
                        {
                            code = 1;
                            break;
                        }
                        if (change < 0)
                        {
                            code = 2;
                            break;
                        }
                        if (iter > FitTensorGradientNLLS.this.maxiter)
                        {
                            code = 3;
                            break;
                        }

                        param = nparam;
                        iter += 1;
                        current = update;
                    }

                    if (iter > 0)
                    {
                        double total = (starting - current.a) / starting;
                        Logging.info(String.format("updated tensor after %d iters, code %d, and percentage decrease %g", iter, code, total));
                    }
                }

                output.set(model(base, param).getEncoding());
            }
        }.init(this.gradients.size(), new Tensor().getEncodingSize());

        return this;
    }

    private static Vect param(Tensor tensor)
    {
        Vect out = VectSource.createND(DIM);

        Matrix matrix = tensor.getMatrix();
        Matrix chol = MatrixUtils.cholesky(matrix);

        double cholXX = chol.get(0, 0);
        double cholYY = chol.get(1, 1);
        double cholZZ = chol.get(2, 2);
        double cholXY = chol.get(1, 0);
        double cholYZ = chol.get(2, 1);
        double cholXZ = chol.get(2, 0);

        out.set(CHOLXX_IDX, cholXX);
        out.set(CHOLYY_IDX, cholYY);
        out.set(CHOLZZ_IDX, cholZZ);
        out.set(CHOLXY_IDX, cholXY);
        out.set(CHOLYZ_IDX, cholYZ);
        out.set(CHOLXZ_IDX, cholXZ);

        return out;
    }

    private static Tensor model(double base, Vect param)
    {
        Tensor out = new Tensor();

        Matrix L = new Matrix(3,3);
        L.set(0, 0, param.get(CHOLXX_IDX));
        L.set(1, 1, param.get(CHOLYY_IDX));
        L.set(2, 2, param.get(CHOLZZ_IDX));
        L.set(1, 0, param.get(CHOLXY_IDX));
        L.set(2, 0, param.get(CHOLXZ_IDX));
        L.set(2, 1, param.get(CHOLYZ_IDX));
        Matrix D = L.times(L.transpose());

        out.set(base, D, 0);

        return out;
    }

    private static void constrain(Vect param)
    {
        // do nothing for now
    }

    private static Pair<Double,Vect> update(Gradients gradients, Vect param, Vect signal)
    {
        // return the predicted signal and gradient

        double cost = 0;
        Vect grad = VectSource.createND(DIM);
        double norm = 1.0 / 2.0 / (double) gradients.size();

        for (int i = 0; i < gradients.size(); i++)
        {
            double gsig = signal.get(i);
            Pair<Double,Vect> gupdate = update(gradients.getBval(i), gradients.getBvec(i), param);
            double dsig = gupdate.a - gsig;

            cost += norm * dsig * dsig;
            grad.plusEquals(norm * dsig, gupdate.b);
        }

        return Pair.of(cost, grad);
    }

    private static Pair<Double,Vect> update(double bval, Vect bvec, Vect param)
    {
        // return the predicted signal and gradient

        double b = bval;
        double x = bvec.get(0);
        double y = bvec.get(1);
        double z = bvec.get(2);

        double cholXX = param.get(CHOLXX_IDX);
        double cholYY = param.get(CHOLYY_IDX);
        double cholZZ = param.get(CHOLZZ_IDX);
        double cholXY = param.get(CHOLXY_IDX);
        double cholYZ = param.get(CHOLYZ_IDX);
        double cholXZ = param.get(CHOLXZ_IDX);

        double expArg = 0;
        expArg += -b * x * x * (cholXX * cholXX);
        expArg += -b * y * y * (cholYY * cholYY + cholXY * cholXY);
        expArg += -b * z * z * (cholXZ * cholXZ + cholYZ * cholYZ + cholZZ * cholZZ);
        expArg += -b * 2.0 * x * y * (cholXX * cholXY);
        expArg += -b * 2.0 * x * z * (cholXX * cholXZ);
        expArg += -b * 2.0 * y * z * (cholXY * cholXZ + cholYY * cholYZ);

        double signal = Math.exp(expArg);

        double dCholXX = -b * 2.0 * (x * x * cholXX + x * y * cholXY + x * z * cholXZ);
        double dCholYY = -b * 2.0 * (y * y * cholYY + z * y * cholYZ);
        double dCholZZ = -b * 2.0 * (z * z * cholZZ);
        double dCholXY = -b * 2.0 * (y * y * cholXY + x * y * cholXX + y * z * cholXZ);
        double dCholYZ = -b * 2.0 * (z * z * cholYZ + x * z * cholXX + y * z * cholXY);
        double dCholXZ = -b * 2.0 * (z * z * cholXZ + x * z * cholXX + y * z * cholYY);

        Vect gradExpArg = VectSource.createND(DIM);
        gradExpArg.set(CHOLXX_IDX, dCholXX);
        gradExpArg.set(CHOLYY_IDX, dCholYY);
        gradExpArg.set(CHOLZZ_IDX, dCholZZ);
        gradExpArg.set(CHOLXY_IDX, dCholXY);
        gradExpArg.set(CHOLYZ_IDX, dCholYZ);
        gradExpArg.set(CHOLXZ_IDX, dCholXZ);

        Vect gradient = gradExpArg.times(signal);

        return Pair.of(signal, gradient);
    }

    public VectFunction getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
