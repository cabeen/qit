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

package qit.data.utils.curves;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import qit.base.Global;
import qit.base.JsonDataset;
import qit.base.Logging;
import qit.data.datasets.Curves;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.modules.curves.CurvesClosestPointTransform;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.math.structs.CovarianceType;
import qit.math.structs.Gaussian;

import java.io.IOException;

/** a simple spatial model with a chi-square test for goodness of fit */
public class CurvesBundleSCPT extends JsonDataset
{
    private CurvesClosestPointTransform transform;
    private Vect mean = null;
    private Matrix invcov = null;
    private double thresh = 2.0;

    public CurvesBundleSCPT()
    {
    }

    public CurvesBundleSCPT withTransform(CurvesClosestPointTransform d)
    {
        this.transform = d;
        return this;
    }

    public CurvesClosestPointTransform getTransform()
    {
        return this.transform;
    }

    public CurvesBundleSCPT withMean(Vect d)
    {
        this.mean = d;

        return this;
    }

    public Vect getMean()
    {
        return this.mean;
    }

    public Matrix getInvCov()
    {
        return this.invcov;
    }

    public CurvesBundleSCPT withInvCov(Matrix v)
    {
        this.invcov = v;

        return this;
    }

    public CurvesBundleSCPT withThresh(double d)
    {
        this.thresh = d;
        return this;
    }

    public double getThresh()
    {
        return this.thresh;
    }

    public Curves apply(Curves cs)
    {
        Logging.info("starting bundle selection");
        int ppercent = 0;
        int num = cs.size();
        Curves out = new Curves();
        out.add(Curves.PVAL, VectSource.create1D());
        out.add(Curves.DIST, VectSource.create1D());
        out.add(Curves.MAHAL, VectSource.create1D());

        transform.input = cs;
        Vects xs = this.transform.run().output;

        for (int i = 0; i < cs.size(); i++)
        {
            int percent = (int) Math.ceil(100.0 * (i + 1) / num);
            if (percent >= ppercent + 2)
            {
                Logging.info("... processed " + percent + " percent");
                ppercent = percent;
            }

            Vect x = xs.get(i);
            Vect delta = x.minus(this.mean);
            double mahal2 = this.invcov.times(delta).dot(delta);

            // mahal2 is distributed as chi-squared
            double pcum = new ChiSquaredDistribution(this.mean.size() - 1).cumulativeProbability(mahal2);
            double pval = 1 - pcum;
            double mahal = Math.sqrt(mahal2);
            double dist = Math.sqrt(mahal2 / this.mean.size());

            if (dist < this.thresh)
            {
                out.add(cs.get(i));
                CurvesUtils.attrSet(out.get(out.size() - 1), VectSource.create1D(dist), Curves.DIST);
                CurvesUtils.attrSet(out.get(out.size() - 1), VectSource.create1D(pval), Curves.PVAL);
                CurvesUtils.attrSet(out.get(out.size() - 1), VectSource.create1D(mahal), Curves.MAHAL);
            }
        }
        Logging.info("finished bundle selection");
        return out;
    }

    public static CurvesBundleSCPT fit(CurvesClosestPointTransform transform, Curves curves)
    {
        return fit(transform, curves, 1.0, 0);
    }

    public static CurvesBundleSCPT fit(CurvesClosestPointTransform transform, Curves curves, double mix, double prior)
    {
        Global.assume(prior > 0, "prior must be non-zero");
        Global.assume(mix >= 0 && mix <= 1, "mixing must be unit valued");

        transform.input = curves;
        Vects samples = transform.run().output;
        Gaussian gaussian = new VectsGaussianFitter().withInput(samples).withType(CovarianceType.full).getOutput();
        Vect mean = gaussian.getMean();
        Matrix var = gaussian.getCov();
        Matrix pmat = MatrixSource.diag(VectSource.createND(mean.size(), prior));
        Matrix shrink = var.times(1 - mix).plus(mix, pmat);
        Matrix invcov = shrink.inv();

        return new CurvesBundleSCPT().withMean(mean).withInvCov(invcov).withTransform(transform);
    }

    public static CurvesBundleSCPT read(String fn) throws IOException
    {
        return JsonDataset.read(CurvesBundleSCPT.class, fn);
    }

    public CurvesBundleSCPT copy()
    {
        return new CurvesBundleSCPT().withMean(this.mean).withInvCov(this.invcov).withThresh(this.thresh);
    }

}