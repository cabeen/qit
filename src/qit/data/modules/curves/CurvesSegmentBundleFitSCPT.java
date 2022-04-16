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

import qit.base.Global;
import qit.base.JsonDataset;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Curves;
import qit.data.datasets.Deformation;
import qit.data.datasets.Vects;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.math.structs.CovarianceType;
import qit.math.structs.Gaussian;

@ModuleUnlisted
@ModuleDescription("Fit a Gaussian model for bundle segmentation using a Gaussian one class classifier based on the SCPT")
@ModuleCitation("(in preparation)")
@ModuleAuthor("Ryan Cabeen")
public class CurvesSegmentBundleFitSCPT implements Module
{
    @ModuleInput
    @ModuleDescription("the input")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("the landmarks")
    public Vects landmarks = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a deformation")
    public Deformation deform = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the shrinkage prior bundle size (in mm)")
    public Double prior = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the gaussian type (full, diagonal, spherical)")
    public String type = "diagonal";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the prior mixing weight (use zero for no prior)")
    public Double mix = 0.5;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the additive prior size (in mm).")
    public Double add = null;

    @ModuleOutput
    @ModuleDescription("the output model")
    public JsonDataset output = null;

    @Override
    public CurvesSegmentBundleFitSCPT run()
    {
        Global.assume(prior > 0, "prior must be non-zero");
        Global.assume(mix >= 0 && mix <= 1, "mixing must be unit valued");

        Logging.info("transforming points");
        CurvesClosestPointTransform transform = new CurvesClosestPointTransform();
        transform.input = this.input;
        transform.landmarks = this.landmarks;
        transform.deform = this.deform;
        Vects samples = transform.run().output;

        Logging.info("fitting model");
        VectsGaussianFitter fitter = new VectsGaussianFitter().withInput(samples);
        fitter.withType(CovarianceType.valueOf(this.type));
        fitter.withPrior(this.prior);
        fitter.withMix(this.mix);
        fitter.withAdd(this.add);
        Gaussian gauss = fitter.getOutput();

        this.output = gauss;
        
        return this;
    }
}
