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

package qit.data.modules.volume;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Sample;
import qit.data.datasets.Solids;
import qit.data.datasets.Table;
import qit.data.datasets.Volume;
import qit.data.utils.VolumeUtils;
import qit.data.utils.volume.VolumeOnlineStats;
import qit.data.utils.volume.VolumeStats;
import qit.data.utils.volume.VolumeVoxelStats;

import java.io.IOException;

@ModuleDescription("Compute pairwise statistics from volumetric data")
@ModuleAuthor("Ryan Cabeen")
public class VolumeStatsPaired implements Module
{
    @ModuleInput
    @ModuleDescription("a table where each row stores a pair of matching subjects")
    public Table matching;

    @ModuleParameter
    @ModuleDescription("input volume filename pattern (should contain %s for the subject identifier)")
    public String pattern;

    @ModuleParameter
    @ModuleDescription("the left group in the pair")
    public String left = "left";

    @ModuleParameter
    @ModuleDescription("the right group in the pair")
    public String right = "right";

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output left mean volume")
    public Volume outputLeftMean;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output left standard deviation volume")
    public Volume outputLeftStd;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output right mean volume")
    public Volume outputRightMean;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output right standard deviation volume")
    public Volume outputRightStd;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output difference in means")
    public Volume outputDiff;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output Cohen's-d effect size")
    public Volume outputCohenD;

    @Override
    public VolumeStatsPaired run()
    {
        VolumeOnlineStats leftStats = new VolumeOnlineStats();
        VolumeOnlineStats rightStats = new VolumeOnlineStats();
        VolumeOnlineStats diffStats = new VolumeOnlineStats();

        Logging.infosub("processing %d pairs", this.matching.getNumRecords());
        for (Integer key : this.matching.keys())
        {
            try
            {
                String leftsub = this.matching.get(key, this.left);
                String rightsub = this.matching.get(key, this.right);

                Logging.infosub("... processing pair: (%s, %s)", leftsub, rightsub);

                String leftfn = String.format(this.pattern, leftsub);
                String rightfn = String.format(this.pattern, rightsub);

                Volume leftvol = Volume.read(leftfn);
                Volume rightvol = Volume.read(rightfn);

                leftStats.update(leftvol, true);
                rightStats.update(rightvol, true);
            }
            catch (IOException e)
            {
                Logging.info("...... warning: failed to process pair");
            }
            catch (RuntimeException e)
            {
                Logging.info("...... warning: failed to process pair");
            }
        }

        Volume diff = leftStats.mean.proto();
        Volume cohend = leftStats.mean.proto();

        for (Sample sample : cohend.getSampling())
        {
            for (int i = 0; i < cohend.getDim(); i++)
            {
                double leftmean = leftStats.mean.get(sample, i);
                double leftstd = leftStats.std.get(sample, i);
                double rightmean = rightStats.mean.get(sample, i);
                double rightstd = rightStats.std.get(sample, i);

                double meandiff = leftmean - rightmean;
                double pooledstd = Math.sqrt(0.5 * (leftstd * leftstd + rightstd * rightstd));

                diff.set(sample, i, meandiff);
                cohend.set(sample, i, meandiff / pooledstd);
            }
        }

        this.outputLeftMean = leftStats.mean;
        this.outputLeftStd = leftStats.std;
        this.outputRightMean = rightStats.mean;
        this.outputRightStd = rightStats.std;
        this.outputDiff = diff;
        this.outputCohenD = cohend;

        return this;
    }
}
