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
import qit.math.structs.CovarianceType;
import qit.math.structs.Gaussian;
import qit.math.utils.MathUtils;

public class VectsGaussianFitter
{
    private Vect weights;
    private Vects input;
    private Double fixed;
    private CovarianceType type = CovarianceType.full;
    private Double prior = null;
    private Double mix = 0.5;
    private Double add = null;
    private Gaussian output;

    public VectsGaussianFitter withWeights(Vect w)
    {
        this.weights = w;
        this.output = null;

        return this;
    }

    public VectsGaussianFitter withInput(Vects p)
    {
        this.input = p;
        this.output = null;

        return this;
    }

    public VectsGaussianFitter withType(CovarianceType t)
    {
        this.type = t;
        this.output = null;

        return this;
    }

    public VectsGaussianFitter withPrior(Double v)
    {
        this.prior = v;
        this.output = null;

        return this;
    }

    public VectsGaussianFitter withMix(Double v)
    {
        this.mix = v;
        this.output = null;

        return this;
    }

    public VectsGaussianFitter withAdd(Double v)
    {
        this.add = v;
        this.output = null;

        return this;
    }

    public VectsGaussianFitter withFixed(Double v)
    {
        this.fixed = v;
        this.output = null;

        return this;
    }

    public VectsGaussianFitter run()
    {
        if (this.weights == null)
        {
            this.weights = VectSource.createND(this.input.size(), 1);
        }

        Global.assume(this.weights.size() == this.input.size(), "invalid weight array");

        int num = this.input.size();
        int dim = this.input.getDim();
        double wsum = this.weights.sum();
        double norm = 1.0 / wsum;

        Global.assume(MathUtils.nonzero(wsum) && wsum >= 0, "invalid weight sum: " + wsum);

        Vect mean = VectSource.createND(dim);
        for (int nidx = 0; nidx < num; nidx++)
        {
            double w = this.weights.get(nidx);
            Vect v = this.input.get(nidx);
            mean.plusEquals(w, v);
        }
        mean.timesEquals(norm);

        Matrix cov = null;
        if (this.type == CovarianceType.spherical && this.fixed != null)
        {
            cov = MatrixSource.identity(dim).times(this.fixed);
        }
        else if (this.type == CovarianceType.spherical && this.fixed == null)
        {
            double sigma2 = 0;
            for (int i = 0; i < num; i++)
            {
                double w = this.weights.get(i);
                double d2 = this.input.get(i).minus(mean).norm2();
                sigma2 += w * d2;
            }
            sigma2 *= norm;
            sigma2 /= dim;
            cov = MatrixSource.identity(dim).times(sigma2);
        }
        else if (this.type == CovarianceType.diagonal)
        {
            Vect sigma2 = VectSource.createND(dim);
            for (int i = 0; i < num; i++)
            {
                double w = this.weights.get(i);
                Vect p = this.input.get(i);
                for (int j = 0; j < dim; j++)
                {
                    double d = p.get(j) - mean.get(j);
                    double d2 = d * d;
                    sigma2.set(j, sigma2.get(j) + w * d2);
                }
            }
            sigma2.timesEquals(norm);
            cov = MatrixSource.diag(sigma2);
        }
        else if (this.type == CovarianceType.full)
        {
            cov = new Matrix(dim, dim);
            for (int i = 0; i < num; i++)
            {
                double w = this.weights.get(i);
                Vect d = this.input.get(i).minus(mean);
                Matrix dtd = MatrixSource.dyadic(d);
                cov.plusEquals(w, dtd);
            }
            cov.timesEquals(norm);
        }
        else
        {
            Logging.error("invalid covariance estimator: " + this.type);
        }

        if (this.prior != null)
        {
            Matrix pmat = MatrixSource.diag(VectSource.createND(mean.size(), prior));
            cov = cov.times(1 - mix).plus(mix, pmat);
        }

        if (this.add != null)
        {
            cov = cov.plus(MatrixSource.identity(mean.size()).times(this.add));
        }

        this.output = new Gaussian(mean, cov);

        return this;
    }

    public Gaussian getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
