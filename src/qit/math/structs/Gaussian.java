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

package qit.math.structs;

import qit.base.Global;
import qit.base.JsonDataset;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.math.utils.MathUtils;
import smile.stat.distribution.MultivariateGaussianDistribution;

import java.io.IOException;

public class Gaussian extends JsonDataset
{
    private MultivariateGaussianDistribution model;

    private Gaussian()
    {
    }

    public Gaussian(Vect mean, Matrix cov)
    {
        this.model = new MultivariateGaussianDistribution(mean.toArray(), cov.toArray());
        Global.assume(!Double.isNaN(this.model.entropy()), "invalid mixture component");
    }
    
    public Vect getMean()
    {
        return new Vect(this.model.mean());
    }

    public Matrix getCov()
    {
        return new Matrix(this.model.cov());
    }

    public int getDim()
    {
        return this.model.mean().length;
    }

    public Vect sample()
    {
        return new Vect(this.model.rand());
    }

    public double cdf(Vect point)
    {
        return this.model.cdf(point.toArray());
    }
    
    public double density(Vect input)
    {
        return this.model.p(input.toArray());
    }

    public double kl(Gaussian right)
    {
        Gaussian left = this;

        Matrix leftCov = left.getCov();
        Matrix rightCov = right.getCov();

        int d = this.getDim();
        double detLeft = leftCov.det();
        double detRight = rightCov.det();
        double logdet = Math.log(detRight / detLeft);
        double trace = rightCov.inv().times(leftCov).diag().sum();
        Vect del = right.getMean().minus(left.getMean());
        double sdel = del.dot(left.getCov().inv().times(del));

        double out = 0.5 * (logdet - d + trace + sdel);

        return out;
    }

    public double klsym(Gaussian model)
    {
        double left = this.kl(model);
        double right = model.kl(this);

        return 0.5 * (left + right);
    }

    public double mahal2(Vect input)
    {
        return this.mahal2(input, this.getMean());
    }

    public double mahal2(Vect a, Vect b)
    {
        Vect d = a.minus(b);
        return d.dot(this.getCov().inv().times(d));
    }

    public int dof()
    {
        return this.model.npara();
    }

    public double nll(Vect input)
    {
        return -1.0 * Math.log(this.density(input));
    }

    public double nll(Vects points)
    {
        double nll = 0;
        for (Vect v : points)
        {
            nll += this.nll(v);
        }

        return nll;
    }

    public double bic(Vects points)
    {
        return this.dof() * Math.log(points.size()) + 2 * this.nll(points);
    }
    
    public static Gaussian read(String fn) throws IOException
    {
        return JsonDataset.read(Gaussian.class, fn);
    }

    public Gaussian copy()
    {
        return new Gaussian(this.getMean(), this.getCov());
    }
}