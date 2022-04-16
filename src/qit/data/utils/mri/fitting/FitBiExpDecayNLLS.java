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
import qit.data.models.BiExpDecay;
import qit.data.models.ExpDecay;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

public class FitBiExpDecayNLLS extends VectFunction
{
    public static final String NLLS = "nlls";

    public static final int MAXITER = 5000;
    public static final int NDIM = 4;
    public static final int MDIM = 7;
    public static final int IPRINT = 0;
    public static final double RHOBEG = 0.2;
    public static final double RHOEND = 1e-10;

    public static VectFunction get(final Vect varying)
    {
        final VectFunction initter = new FitExpDecayLLS().withVarying(varying).withWeighted(true).get();
        final VectFunction synther = BiExpDecay.synth(varying);

        return new VectFunction()
        {
            public void apply(final Vect input, Vect output)
            {
                Vect initParam = initter.apply(input);
                ExpDecay monofit = new ExpDecay(initParam);

                // alpha and beta typically have different magnitudes, so let's normalize them during cobyla
                final double initAlpha = monofit.getAlpha();
                final double initBeta = monofit.getBeta();
                final double scaleAlpha = MathUtils.zero(initAlpha) ? 1.0 : initAlpha;
                final double scaleBeta = MathUtils.zero(initBeta) ? 1.0 : initBeta;
                final double scaleFrac = 0.2;

                final double mindecay = 0; // 0.1 * initBeta;

                double[] x = new double[NDIM];
                x[0] = monofit.getAlpha() / scaleAlpha; // alpha
                x[1] = 0.25 / scaleFrac; // frac
                x[2] = (0.5 * monofit.getBeta()) / scaleBeta; // beta
                x[3] = (1.25 * monofit.getBeta()) / scaleBeta; // gamma
                // the other is determined by (1.0 - frac)

                Calcfc func = new Calcfc()
                {
                    @Override
                    public double Compute(int n, int m, double[] x, double[] con)
                    {
                        Global.assume(n == NDIM, "invalid value dimension: " + n);
                        Global.assume(m == MDIM, "invalid constraint dimension: " + m);

                        double alpha = x[0] * scaleAlpha;
                        double frac = x[1] * scaleFrac;
                        double beta = x[2] * scaleBeta;
                        double gamma = x[3] * scaleBeta;

                        BiExpDecay model = new BiExpDecay();
                        model.setAlpha(alpha);
                        model.setFrac(frac);
                        model.setBeta(beta);
                        model.setGamma(gamma);
                        Vect pred = synther.apply(model.getEncoding());

                        double cost = pred.minus(input).norm();
                        // @todo: add a prior of fractions?

                        con[0] = alpha; // alpha > 0
                        con[1] = frac; // frac > 0
                        con[2] = 1.0 - frac; // frac < 1
                        con[3] = beta; // beta > 0
                        con[4] = gamma; // gamma > 0
                        con[5] = gamma - beta; // beta < gamma
                        con[6] = beta - mindecay; // mindecay < beta
                        // @todo: add a margin between beta and gamma?

                        return cost;
                    }
                };
                Cobyla.FindMinimum(func, NDIM, MDIM, x, RHOBEG, RHOEND, IPRINT, MAXITER);

                BiExpDecay modelNLLS = new BiExpDecay();
                modelNLLS.setAlpha(x[0] * scaleAlpha);
                modelNLLS.setFrac(x[1] * scaleFrac);
                modelNLLS.setBeta(x[2] * scaleBeta);
                modelNLLS.setGamma(x[3] * scaleBeta);

                // double errorNLLS = synther.apply(modelNLLS.getEncoding()).minus(input).norm2();
                output.set(modelNLLS.getEncoding());
            }
        }.init(varying.size(), new BiExpDecay().getEncodingSize());
    }
}