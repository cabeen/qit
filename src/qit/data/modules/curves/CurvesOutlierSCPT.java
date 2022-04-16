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
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleExpert;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.math.structs.CovarianceType;
import qit.math.structs.Gaussian;

@ModuleDescription("Detect outliers among curves with a sparse closest point transform.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesOutlierSCPT implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleExpert
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("preprocess by resampling the curves")
    public Double preden = null;

    @ModuleParameter
    @ModuleExpert
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("preprocess by simplifying the curves")
    public Double preeps = 1.0;

    @ModuleParameter
    @ModuleExpert
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the number of curves for landmarking")
    public Integer lmsub = 5000;

    @ModuleParameter
    @ModuleExpert
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the simplifification threshold for landmarking")
    public Double lmeps = 1d;

    @ModuleParameter
    @ModuleExpert
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the number of clusters for landmarking")
    public Integer lmnum = 2;

    @ModuleParameter
    @ModuleExpert
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the landmarking threshold")
    public Double lmthresh = 15d;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the prior bundle size (mm)")
    public Double prior = 0.001;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the prior mixing weight (use zero for no prior)")
    public Double mix = 0.0;

    @ModuleParameter
    @ModuleDescription("covariance matrix type (full, diagonal, spherical)")
    public String type = "diagonal";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the threshold probability")
    public Double thresh = 0.99;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output inlier curves")
    public Curves inlier;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output outlier curves")
    public Curves outlier;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the computed landmarks")
    public Vects landmarks = null;

    @Override
    public CurvesOutlierSCPT run()
    {
        if (this.input.size() == 0)
        {
            Logging.info("no curves found");
            return this;
        }

        Logging.info("extracting landmarks");
        CurvesLandmarks landmarker = new CurvesLandmarks();
        landmarker.input = this.input;

        if (this.lmsub != null)
        {
            landmarker.subsamp = this.lmsub;
        }

        if (this.lmeps != null)
        {
            landmarker.eps = this.lmeps;
        }

        if (this.lmthresh != null)
        {
            landmarker.radius = this.lmthresh;
        }

        if (this.lmnum != null)
        {
            landmarker.num = this.lmnum;
        }

        Vects lm = landmarker.getOutput();

        Logging.info("transforming curves");
        CurvesClosestPointTransform transform = new CurvesClosestPointTransform();
        transform.input = this.input;
        transform.landmarks = lm;
        Vects samples = transform.run().output;

        Logging.info("fitting gaussian");
        VectsGaussianFitter fitter = new VectsGaussianFitter().withInput(samples);
        fitter.withType(CovarianceType.valueOf(this.type));
        fitter.withPrior(this.prior);
        fitter.withMix(this.mix);
        Gaussian gauss = fitter.getOutput();

        Logging.info("testing for outliers");
        Curves cinlier = new Curves();
        Curves coutlier = new Curves();
        for (int i = 0; i < this.input.size(); i++)
        {
            Vect x = samples.get(i);
            double mahal2 = gauss.mahal2(x);

            // mahal2 is distributed as chi-squared
            double pcum = new ChiSquaredDistribution(gauss.getDim() - 1).cumulativeProbability(mahal2);

            if (pcum > this.thresh)
            {
                coutlier.add(this.input.get(i));
            }
            else
            {
                cinlier.add(this.input.get(i));
            }
        }

        this.landmarks = lm;
        this.outlier = coutlier;
        this.inlier = cinlier;

        return this;
    }
}
