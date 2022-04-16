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

package qit.data.utils.vects.stats;

import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.data.utils.mri.CaminoUtils;
import qit.math.structs.Watson;
import qit.math.utils.MathUtils;

public class VectsWatsonFitter
{
    private Vect weights;
    private Vects input;
    private Double fixed;
    private Double reg;
    private Watson output;

    public VectsWatsonFitter withWeights(Vect w)
    {
        this.weights = w;
        this.output = null;

        return this;
    }

    public VectsWatsonFitter withInput(Vects p)
    {
        this.input = p;
        this.output = null;

        return this;
    }

    public VectsWatsonFitter withReg(Double p)
    {
        this.reg = p;
        this.output = null;

        return this;
    }

    public VectsWatsonFitter withFixed(Double v)
    {
        this.fixed = v;
        this.output = null;

        return this;
    }

    public VectsWatsonFitter run()
    {
        int num = this.input.size();
        double sumw = this.weights == null ? num : this.weights.sum();
        double norm = 1.0 / sumw;

        Matrix sum = new Matrix(3, 3);
        for (int i = 0; i < num; i++)
        {
            double w = this.weights == null ? 1.0 : this.weights.get(i);
            Vect line = this.input.get(i);
            Matrix outer = MatrixSource.outer(line, line);
            sum.plusEquals(w, outer);
        }
        sum.timesEquals(norm);
        EigenDecomp eig = MatrixUtils.eig(sum);
        Vect mu = eig.vectors.get(0);
        Double kappa = null;

        if (sum.normF() < Global.DELTA || !MathUtils.unit(mu.norm()))
        {
            Logging.info("warning: mu estimation was degenerate");
            mu = VectSource.randomUnit();
            kappa = 0d;
        }
        else if (this.fixed != null)
        {
            kappa = this.fixed;
        }
        else
        {
            double lamb = eig.values.get(0);

            if (MathUtils.zero(lamb))
            {
                Logging.info("warning: zero isotropic distribution found");
            }

            kappa = CaminoUtils.kappaWatson(lamb);

            // check if lambda could be estimated
            if (kappa == null || kappa < 0)
            {
                // if not approximate it
                double sumwdot = 0;
                for (int i = 0; i < num; i++)
                {
                    double w = this.weights == null ? 1.0 : this.weights.get(i);
                    double dot = this.input.get(i).dot(mu);
                    sumwdot += w * dot * dot;
                }

                kappa = sumw / (sumw - sumwdot);
                if (Double.isInfinite(kappa) || Double.isNaN(kappa))
                {
                    kappa = 100.0;
                }
                if (kappa < 0)
                {
                    kappa = 1.0;
                }
            }
        }

        if (kappa == null)
        {
            kappa = 0d;
        }

        if (this.reg != null && kappa > 0)
        {
            // this will roughly take reg percent off the concentration, e.g. 0.1 will be 10%
            kappa = Math.exp(Math.log(kappa) - this.reg);
        }

        this.output = new Watson(mu, kappa);

        return this;
    }

    public Watson getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}