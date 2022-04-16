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
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.models.Tensor;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

public class FitTensorLLS implements Supplier<VectFunction>
{
    public static final String LLS = "lls";
    public static final String WLLS = "wlls";

    public Gradients gradients;
    public Double clamp = null;
    public boolean baseline = false;
    public boolean weighted = false;

    public VectFunction get()
    {
        int dim = this.gradients.size();
        if (this.baseline)
        {
            // assume the last channel is the MSE
            final Matrix A = new Matrix(dim, 6);
            for (int idx = 0; idx < dim; idx++)
            {
                double b = this.gradients.getBval(idx);
                Vect g = this.gradients.getBvec(idx).normalize();

                double gx = g.get(0);
                double gy = g.get(1);
                double gz = g.get(2);

                A.set(idx, 0, -b * gx * gx);
                A.set(idx, 1, -b * gy * gy);
                A.set(idx, 2, -b * gz * gz);
                A.set(idx, 3, -b * 2 * gx * gy);
                A.set(idx, 4, -b * 2 * gy * gz);
                A.set(idx, 5, -b * 2 * gx * gz);
            }
            final Matrix Ainv = A.inv();

            return new VectFunction()
            {
                public void apply(Vect input, Vect output)
                {
                    double s0 = ModelUtils.baselineStats(FitTensorLLS.this.gradients, input).mean;
                    Vect norm = input.times(1.0 / Math.max(s0, Global.DELTA));

                    Matrix B = new Matrix(dim, 1);

                    for (int idx = 0; idx < dim; idx++)
                    {
                        double s = norm.get(idx);
                        if (s > 0)
                        {
                            B.set(idx, 0, Math.log(s));
                        }
                        else
                        {
                            B.set(idx, 0, 0);
                        }
                    }

                    Matrix X = Ainv.times(B);

                    if (FitTensorLLS.this.weighted)
                    {
                        Matrix W = MatrixSource.diag(A.times(X.getColumn(0)).exp());
                        X = (W.times(A)).inv().times(W.times(B));
                    }

                    Vect param = VectSource.createND(Tensor.DT_DIM);

                    param.set(Tensor.DT_XX, X.get(0, 0));
                    param.set(Tensor.DT_YY, X.get(1, 0));
                    param.set(Tensor.DT_ZZ, X.get(2, 0));
                    param.set(Tensor.DT_XY, X.get(3, 0));
                    param.set(Tensor.DT_YZ, X.get(4, 0));
                    param.set(Tensor.DT_XZ, X.get(5, 0));
                    param.set(Tensor.DT_S0, s0);

                    if (FitTensorLLS.this.clamp != null)
                    {
                        param.set(new Tensor(param).clamp(FitTensorLLS.this.clamp).getEncoding());
                    }

                    output.set(param);
                }
            }.init(this.gradients.size(), new Tensor().getEncodingSize());
        }
        else
        {
            // assume the last channel is the MSE
            final Matrix A = new Matrix(dim, 7);
            for (int idx = 0; idx < dim; idx++)
            {
                double b = this.gradients.getBval(idx);
                Vect g = this.gradients.getBvec(idx).normalize();

                double gx = g.get(0);
                double gy = g.get(1);
                double gz = g.get(2);

                A.set(idx, 0, -b * gx * gx);
                A.set(idx, 1, -b * gy * gy);
                A.set(idx, 2, -b * gz * gz);
                A.set(idx, 3, -b * 2 * gx * gy);
                A.set(idx, 4, -b * 2 * gy * gz);
                A.set(idx, 5, -b * 2 * gx * gz);
                A.set(idx, 6, -1);
            }
            final Matrix Ainv = A.inv();

            return new VectFunction()
            {
                public void apply(Vect input, Vect output)
                {
                    Matrix B = new Matrix(dim, 1);

                    for (int idx = 0; idx < dim; idx++)
                    {
                        double s = input.get(idx);
                        if (s > 0)
                        {
                            B.set(idx, 0, Math.log(s));
                        }
                        else
                        {
                            B.set(idx, 0, 0);
                        }
                    }

                    Matrix X = Ainv.times(B);

                    if (FitTensorLLS.this.weighted)
                    {
                        Matrix W = MatrixSource.diag(A.times(X.getColumn(0)).exp());
                        X = (W.times(A)).inv().times(W.times(B));
                    }

                    double nls0 = X.get(6, 0);
                    double s0 = nls0 > -23 ? Math.exp(-nls0) : input.max();

                    Vect param = VectSource.createND(Tensor.DT_DIM);

                    param.set(Tensor.DT_XX, X.get(0, 0));
                    param.set(Tensor.DT_YY, X.get(1, 0));
                    param.set(Tensor.DT_ZZ, X.get(2, 0));
                    param.set(Tensor.DT_XY, X.get(3, 0));
                    param.set(Tensor.DT_YZ, X.get(4, 0));
                    param.set(Tensor.DT_XZ, X.get(5, 0));
                    param.set(Tensor.DT_S0, s0);

                    if (FitTensorLLS.this.clamp != null)
                    {
                        param.set(new Tensor(param).clamp(FitTensorLLS.this.clamp).getEncoding());
                    }

                    output.set(param);
                }
            }.init(this.gradients.size(), new Tensor().getEncodingSize());
        }
    }
}
