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
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Deformation;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.curves.CurvesFunctionApply;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.math.source.DistanceSource;
import qit.math.structs.Box;
import qit.math.structs.CovarianceType;
import qit.math.structs.Distance;
import qit.math.structs.Gaussian;

@ModuleDescription("Filter outliers using a mvGaussian probabilistic model with a Chi2 distribution on the Mahalanobis distance")
@ModuleAuthor("Ryan Cabeen")
public class CurvesOutlierGaussian implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleInput
    @ModuleDescription("input reference curve")
    public Curves reference;

    @ModuleParameter
    @ModuleDescription("the number of points used for outlier rejection")
    public Integer outlierCount = 10;

    @ModuleParameter
    @ModuleDescription("the probability threshold for outlier rejection")
    public Double outlierThresh = 0.95;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    public CurvesOutlierGaussian run()
    {
        if (this.input.size() == 0 || this.reference.size() == 0)
        {
            this.output = new Curves();
            return this;
        }

        Curves curves = new CurvesOrient()
        {{
            this.input = CurvesOutlierGaussian.this.input;
        }}.run().output;

        Logging.info("resampling curves");
        Vect gamma = VectSource.linspace(0, 1, this.outlierCount);
        Curves resampled = curves.copy();
        resampled.resample(gamma);

        Logging.info("flattening curves");
        Vects flatten = new Vects();
        for (
                Curve curve : resampled)

        {
            flatten.add(curve.getAll().flatten());
        }

        Logging.info("fitting gaussian");
        VectsGaussianFitter fitter = new VectsGaussianFitter().withInput(flatten);
        fitter.withType(CovarianceType.diagonal);
        Gaussian gauss = fitter.getOutput();

        Logging.info("testing for outliers");
        boolean[] inlier = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            Vect x = flatten.get(i);
            double mahal2 = gauss.mahal2(x);

            // mahal2 is distributed as chi-squared
            double pcum = new ChiSquaredDistribution(gauss.getDim() - 1).cumulativeProbability(mahal2);

            inlier[i] = pcum <= this.outlierThresh;
        }

        Logging.info("keeping inliers");
        curves.keep(inlier);

        this.output = curves;

        return this;
    }
}
