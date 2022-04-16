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

package qit.data.modules.curves;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import qit.base.JsonDataset;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.*;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.math.structs.Gaussian;
import qit.math.utils.MathUtils;

import java.io.IOException;

@ModuleUnlisted
@ModuleDescription("Segment a bundle using a Gaussian one class classifier based on the SCPT")
@ModuleCitation("(in preparation)")
@ModuleAuthor("Ryan Cabeen")
public class CurvesSegmentBundleApplySCPT implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleInput
    @ModuleDescription("the landmarks")
    public Vects landmarks = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a deformation from the curves to landmark space")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("the model to apply")
    public String model;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a probability threshold")
    public Double pval = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a mahalanobis threshold")
    public Double mahal = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a normalized mahalanobis threshold")
    public Double dist = null;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    public CurvesSegmentBundleApplySCPT run() throws IOException
    {
        MyGaussian mygauss = JsonDataset.read(MyGaussian.class, this.model);
        Gaussian gaussian = new Gaussian(mygauss.mean, mygauss.cov);

        Logging.info("starting bundle selection");
        int ppercent = 0;
        int num = this.input.size();
        int dim = gaussian.getDim();
        Curves out = new Curves();
        out.add(Curves.PVAL, VectSource.create1D());
        out.add(Curves.DIST, VectSource.create1D());
        out.add(Curves.MAHAL, VectSource.create1D());

        Logging.info("transforming curves");
        CurvesClosestPointTransform transform = new CurvesClosestPointTransform();
        transform.input = this.input;
        transform.landmarks = this.landmarks;
        transform.deform = this.deform;
        Vects xs = transform.run().output;

        Logging.info("computing statistics");
        for (int i = 0; i < this.input.size(); i++)
        {
            Vect x = xs.get(i);
            double mahal2 = gaussian.mahal2(x);

            // mahal2 is distributed as chi-squared
            double pcum = new ChiSquaredDistribution(dim - 1).cumulativeProbability(mahal2);
            double pval = 1 - pcum;
            double mahal = Math.sqrt(mahal2);
            double dist = Math.sqrt(mahal2 / dim);

            boolean pass = true;

            if (this.pval != null)
            {
                pass &= pval > this.pval;
            }

            if (this.mahal != null)
            {
                pass &= mahal < this.mahal;
            }

            if (this.dist != null)
            {
                pass &= dist < this.dist;
            }

            if (pass)
            {
                out.add(this.input.get(i));
                CurvesUtils.attrSet(out.get(out.size() - 1), VectSource.create1D(dist), Curves.DIST);
                CurvesUtils.attrSet(out.get(out.size() - 1), VectSource.create1D(pval), Curves.PVAL);
                CurvesUtils.attrSet(out.get(out.size() - 1), VectSource.create1D(mahal), Curves.MAHAL);
            }
        }

        this.output = out;

        Logging.info("finished bundle selection");

        return this;
    }


    static class MyGaussian extends JsonDataset
    {
        int dim;
        Vect mean;
        Matrix cov;

        private MyGaussian()
        {
        }

        public MyGaussian copy()
        {
            MyGaussian out = new MyGaussian();
            out.dim = this.dim;
            out.mean = this.mean.copy();
            out.cov = this.cov.copy();

            return out;
        }
    }

}
