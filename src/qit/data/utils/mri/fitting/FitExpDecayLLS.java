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

import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.models.ExpDecay;
import qit.data.source.MatrixSource;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

public class FitExpDecayLLS implements Supplier<VectFunction>
{
    public static final String LLS = "lls";
    public static final String WLLS = "wlls";

    private static final int PARAM_LOGALPHA = 0;
    private static final int PARAM_BETA = 1;

    public Vect varying;
    public boolean weighted;

    public double minAlpha = 0;
    public double minBeta= 0;

    public FitExpDecayLLS withVarying(Vect v)
    {
        this.varying = v;
        return this;
    }

    public FitExpDecayLLS withWeighted(boolean v)
    {
        this.weighted = v;
        return this;
    }

    public FitExpDecayLLS withMins(double ma, double mb)
    {
        this.minAlpha = ma;
        this.minBeta = mb;
        return this;
    }

    @Override
    public VectFunction get()
    {
        final Vect x = varying;
        final int num = x.size();

        // assume the last channel is the MSE
        // use a log-linear model
        // log(y) = log(alpha) - beta * x
        final Matrix A = new Matrix(num, 2);

        for (int i = 0; i < num; i++)
        {
            double xv = x.get(i);
            A.set(i, PARAM_LOGALPHA, 1);
            A.set(i, PARAM_BETA, -xv);
        }

        final Matrix Ainv = A.inv();

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Vect y = input;
                Matrix B = new Matrix(num, 1);

                for (int i = 0; i < num; i++)
                {
                    double yv = input.get(i);
                    double logy = yv <= 0 ? Math.log(1e-3) : Math.log(yv);
                    B.set(i, 0, logy);
                }

                Matrix X = Ainv.times(B);

                if (FitExpDecayLLS.this.weighted)
                {
                    Matrix W = MatrixSource.diag(A.times(X.getColumn(0)).exp());
                    X = (W.times(A)).inv().times(W.times(B));
                }

                double logAlpha = X.get(PARAM_LOGALPHA, 0);
                double alpha = Math.exp(logAlpha);
                double beta = X.get(PARAM_BETA, 0);

                // this was unconstrained, so enforce positivity constraints after the fact
                alpha = Math.max(alpha, FitExpDecayLLS.this.minAlpha);
                beta = Math.max(beta, FitExpDecayLLS.this.minBeta);

                ExpDecay model = new ExpDecay();
                model.setAlpha(alpha);
                model.setBeta(beta);

                output.set(model.getEncoding());
            }
        }.init(x.size(), new ExpDecay().getEncodingSize());
    }
}