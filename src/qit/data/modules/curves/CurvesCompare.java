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

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Table;
import qit.data.datasets.Volume;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.SamplingSource;
import qit.data.source.TableSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.structs.Box;

@ModuleDescription("Compare a pair of curves objects quantitatively")
@ModuleAuthor("Ryan Cabeen")
public class CurvesCompare implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves left;

    @ModuleInput
    @ModuleDescription("the other curves")
    public Curves right;

    @ModuleParameter
    @ModuleDescription("the volume resolution")
    public double delta = 1;

    @ModuleParameter
    @ModuleDescription("the density threshold")
    public double thresh = 0.5;

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public CurvesCompare run()
    {
        Box box = this.left.bounds().union(this.right.bounds()).scale(1.25);
        Sampling sampling = SamplingSource.create(box, delta);

        final Volume densityLeft = VolumeUtils.density(sampling, this.left);
        final Volume densityRight = VolumeUtils.density(sampling, this.right);
        final double fthresh = this.thresh;

        Mask maskLeft = new VolumeThreshold() {{
            this.input = densityLeft;
            this.threshold = fthresh;
        }}.run().output;

        Mask maskRight = new VolumeThreshold() {{
            this.input = densityRight;
            this.threshold = fthresh;
        }}.run().output;

        double volumeLeft = MaskUtils.volume(maskLeft);
        double volumeRight= MaskUtils.volume(maskRight);
        double volumeDiffAbs = Math.abs(volumeLeft - volumeRight);
        double volumeDiffLeft = volumeDiffAbs / volumeLeft;
        double volumeDiffRight = volumeDiffAbs / volumeRight;
        double volumeDiffSym = 2.0 * volumeDiffAbs / (volumeLeft + volumeRight);

        double dice = MaskUtils.dice(maskLeft, maskRight);

        VectOnlineStats diffStatsLeft = new VectOnlineStats();
        VectOnlineStats diffStatsRight = new VectOnlineStats();
        VectOnlineStats diffStatsSym = new VectOnlineStats();
        VectOnlineStats diffStatsAbs = new VectOnlineStats();

        for (Sample sample : sampling)
        {
            if (maskLeft.foreground(sample) || maskRight.foreground(sample))
            {
                double denLeft = densityLeft.get(sample, 0);
                double denRight = densityRight.get(sample, 0);
                double denDiff = Math.abs(denLeft - denRight);

                if (denLeft > 0)
                {
                    diffStatsLeft.update(denDiff / denLeft);
                }

                if (denRight > 0)
                {
                    diffStatsRight.update(denDiff / denRight);
                }

                if (denLeft > 0 || denRight > 0)
                {
                    diffStatsSym.update(2.0 * denDiff / (denLeft + denRight));
                }

                diffStatsAbs.update(denDiff);
            }
        }

        Record out = new Record();
        out.with("dice", dice);
        out.with("density_left", diffStatsLeft.mean);
        out.with("density_right", diffStatsRight.mean);
        out.with("density_sym", diffStatsSym.mean);
        out.with("density_abs", diffStatsAbs.mean);
        out.with("volume_left", volumeDiffLeft);
        out.with("volume_right", volumeDiffRight);
        out.with("volume_sym", volumeDiffSym);
        out.with("volume_abs", volumeDiffAbs);

        this.output = TableSource.createNarrow(out);
        return this;
    }
}
