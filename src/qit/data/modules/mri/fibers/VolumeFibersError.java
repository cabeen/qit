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

package qit.data.modules.mri.fibers;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;

@ModuleDescription("Compute error metrics between a ground truth and test fibers volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersError implements Module
{
    @ModuleInput
    @ModuleDescription("the truth fibers volume")
    public Volume truth;

    @ModuleInput
    @ModuleDescription("the test fibers volume")
    public Volume test;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("a threshold for compartment existence")
    public double thresh = 0.05;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output measuring number of missing compartment error")
    public Volume missing;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output measuring number of extra compartment error")
    public Volume extra;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output measuring the hausdorff angular error")
    public Volume linehaus;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output measuring the total angular error")
    public Volume linetotal;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output measuring the hausdorff fraction error")
    public Volume frachaus;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output measuring the total fraction error")
    public Volume fractotal;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output measuring the isotropic fraction error")
    public Volume fraciso;

    @Override
    public Module run()
    {
        Volume missingOut = truth.proto(1);
        Volume extraOut = truth.proto(1);
        Volume lineHausOut = truth.proto(1);
        Volume lineTotalOut = truth.proto(1);
        Volume fracHausOut = truth.proto(1);
        Volume fracTotalOut = truth.proto(1);
        Volume fracIsoOut = truth.proto(1);

        for (Sample sample : truth.getSampling())
        {
            if (truth.valid(sample, mask))
            {
                Fibers a = new Fibers(truth.get(sample));
                Fibers b = new Fibers(test.get(sample));

                double errorMissing = a.errorMissing(b, this.thresh);
                double errorExtra = a.errorExtra(b, this.thresh);
                double errorLineHaus = a.errorLineHaus(b, this.thresh);
                double errorLineTotal = a.errorLineTotal(b, this.thresh);
                double errorFracHaus = a.errorFracHaus(b, this.thresh);
                double errorFracTotal = a.errorFracTotal(b, this.thresh);
                double errorFracIso = a.errorFracIso(b);

                missingOut.set(sample, errorMissing);
                extraOut.set(sample, errorExtra);
                lineHausOut.set(sample, errorLineHaus);
                lineTotalOut.set(sample, errorLineTotal);
                fracHausOut.set(sample, errorFracHaus);
                fracTotalOut.set(sample, errorFracTotal);
                fracIsoOut.set(sample, errorFracIso);
            }
        }

        this.missing = missingOut;
        this.extra = extraOut;
        this.linehaus = lineHausOut;
        this.linetotal = lineTotalOut;
        this.frachaus = fracHausOut;
        this.fractotal = fracTotalOut;
        this.fraciso = fracIsoOut;

        return this;
    }
}
