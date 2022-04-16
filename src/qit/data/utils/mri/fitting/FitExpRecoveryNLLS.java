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
import qit.data.datasets.Vect;
import qit.data.models.ExpRecovery;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

import java.util.function.Supplier;

public class FitExpRecoveryNLLS implements Supplier<VectFunction>
{
    public static final String NLLS = "nlls";

    public static final int MAXITER = 1000;
    public static final int NDIM = 2;
    public static final int MDIM = 2;
    public static final int IPRINT = 0;
    public static final double RHOBEG = 1.0; // wlls is quite good, so we don't have to search far from that solution
    public static final double RHOEND = 1e-7; // if this is any smaller, the results won't be better than wlls

    public Vect varying;

    public FitExpRecoveryNLLS withVarying(Vect v)
    {
        this.varying = v;
        return this;
    }

    public VectFunction get()
    {
        final VectFunction synther = ExpRecovery.synth(FitExpRecoveryNLLS.this.varying);

        return new VectFunction()
        {
            public void apply(final Vect input, Vect output)
            {
                final double initAlpha = input.max();
                final double initBeta = 1 / (varying.max() - varying.min());
                final double scaleAlpha = MathUtils.zero(initAlpha) ? 1.0 : input.mean();
                final double scaleBeta = MathUtils.zero(initBeta) ? 1.0 : 1.0 / varying.mean();

                double[] x = new double[NDIM];
                x[0] = initAlpha / scaleAlpha;
                x[1] = initBeta / scaleBeta;

                Calcfc func = new Calcfc()
                {
                    @Override
                    public double Compute(int n, int m, double[] x, double[] con)
                    {
                        Global.assume(n == NDIM, "invalid value dimension: " + n);
                        Global.assume(m == MDIM, "invalid constraint dimension: " + m);

                        double alpha = x[0] * scaleAlpha;
                        double beta = x[1] * scaleBeta;

                        ExpRecovery model = new ExpRecovery();
                        model.setAlpha(alpha);
                        model.setBeta(beta);
                        Vect pred = synther.apply(model.getEncoding());

                        double cost = pred.minus(input).norm2();

                        con[0] = alpha;
                        con[1] = beta;

                        return cost;
                    }
                };
                Cobyla.FindMinimum(func, NDIM, MDIM, x, RHOBEG, RHOEND, IPRINT, MAXITER);

                ExpRecovery model = new ExpRecovery();
                model.setAlpha(x[0] * scaleAlpha);
                model.setBeta(x[1] * scaleBeta);

                output.set(model.getEncoding());
            }
        }.init(varying.size(), new ExpRecovery().getEncodingSize());
    }
}