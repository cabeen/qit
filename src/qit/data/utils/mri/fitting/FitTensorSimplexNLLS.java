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

import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.models.Tensor;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

import java.util.function.Supplier;

public class FitTensorSimplexNLLS implements Supplier<VectFunction>
{
    public static final String NAME = "nlls";

    public static final CostType DEFAULT_COST = CostType.SE;

    public static final int MAXITER = 1000;
    public static final int NDIM = 6;
    public static final int MDIM = 0;
    public static final int IPRINT = 0;
    public static final double RHOBEG = 0.1;
    public static final double RHOEND = 1e-6;

    public Gradients gradients;
    public CostType cost = DEFAULT_COST;
    public boolean baseline = false;

    public VectFunction get()
    {
        FitTensorLLS lls = new FitTensorLLS();
        lls.weighted = true;
        lls.baseline = this.baseline;
        lls.gradients = this.gradients;
        final VectFunction initter = lls.get();
        final VectFunction synther = Tensor.synth(this.gradients);

        return new VectFunction()
        {
            public void apply(final Vect input, Vect output)
            {
                Vect initParam = initter.apply(input);
                double errorLLS = synther.apply(initParam).minus(input).norm2();

                Tensor modelLLS = new Tensor(initParam);
                Vect paramLLS = param(modelLLS);

                final double baseline = modelLLS.getBaseline();

                double[] x = new double[NDIM];
                paramLLS.get(x);

                Calcfc func = new Calcfc()
                {
                    @Override
                    public double Compute(int n, int m, double[] x, double[] con)
                    {
                        Global.assume(n == NDIM, "invalid value dimension: " + n);
                        Global.assume(m == MDIM, "invalid constraint dimension: " + m);

                        Tensor model = model(baseline, new Vect(x));
                        Vect pred = synther.apply(model.getEncoding());

                        double cost = ModelUtils.cost(FitTensorSimplexNLLS.this.cost, gradients, input, pred);
                        return cost;
                    }
                };
                Cobyla.FindMinimum(func, NDIM, MDIM, x, RHOBEG, RHOEND, IPRINT, MAXITER);

                Tensor modelNLLS = model(modelLLS.getBaseline(), new Vect(x));
                double errorNLLS = synther.apply(modelNLLS.getEncoding()).minus(input).norm2();

                if (errorNLLS > errorLLS)
                {
                    Logging.info("warning: error went up, reverting to linear fitting");
                    output.set(modelLLS.getEncoding());
                }
                else
                {
                    output.set(modelNLLS.getEncoding());
                }
            }
        }.init(this.gradients.size(), new Tensor().getEncodingSize());
    }

    private static Vect param(Tensor tensor)
    {
        Vect out = VectSource.createND(6);

        Matrix matrix = tensor.getMatrix();
        Matrix chol = MatrixUtils.cholesky(matrix);

        out.set(0, chol.get(0, 0));
        out.set(1, chol.get(1, 1));
        out.set(2, chol.get(2, 2));
        out.set(3, chol.get(1, 0));
        out.set(4, chol.get(2, 1));
        out.set(5, chol.get(2, 0));

        return out;
    }

    private static Tensor model(double base, Vect param)
    {
        Tensor out = new Tensor();

        Matrix L = new Matrix(3,3);
        L.set(0, 0, param.get(0));
        L.set(1, 1, param.get(1));
        L.set(2, 2, param.get(2));
        L.set(1, 0, param.get(3));
        L.set(2, 1, param.get(4));
        L.set(2, 0, param.get(5));
        Matrix D = L.times(L.transpose());

        out.set(base, D, 0);

        return out;
    }
}
