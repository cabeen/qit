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

package qit.data.modules.mri.tensor;

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
import qit.data.models.Tensor;
import qit.data.modules.mask.MaskOpen;
import qit.data.modules.mri.model.VolumeModelFeature;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.modules.volume.VolumeThresholdOtsu;
import qit.data.utils.MaskUtils;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.utils.MathUtils;

@ModuleDescription("Use a free-water eliminated tensor volume to segment gray and white matter")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTensorSegmentTissue implements Module
{
    @ModuleInput
    @ModuleDescription("the input tensor volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("a maximum free water")
    public Double fw = 0.5;

    @ModuleParameter
    @ModuleDescription("a minimum mean diffusivity")
    public Double md = 0.0002;

    @ModuleParameter
    @ModuleDescription("a number of iterations for mask opening")
    public Integer num = 2;

    @ModuleOutput
    @ModuleDescription("the gray matter segmentation mask")
    public Mask labels;

    @ModuleOutput
    @ModuleDescription("the gray matter density map")
    public Volume gray;

    @ModuleOutput
    @ModuleDescription("the white matter density map")
    public Volume white;

    public VolumeTensorSegmentTissue run()
    {
        Volume md = VolumeModelFeature.apply(this.input, Tensor.FEATURES_MD);
        Volume fw = VolumeModelFeature.apply(this.input, Tensor.FEATURES_FW);

        Mask mdm = VolumeThreshold.apply(md, this.md);
        Mask fwm = VolumeThreshold.applyInverse(fw, this.fw);
        Mask fg = MaskUtils.and(mdm, fwm);
        fg = MaskUtils.and(fg, this.mask);
        fg = MaskOpen.apply(fg, this.num, true);

        double thresh = VolumeThresholdOtsu.otsu(md, fg);
        Mask gm = VolumeThreshold.apply(md, fg, thresh);
        Mask wm = MaskUtils.and(MaskUtils.invert(gm), fg);

        VolumeVoxelStats gmstats = new VolumeVoxelStats().withInput(md).withMask(gm).run();
        VolumeVoxelStats wmstats = new VolumeVoxelStats().withInput(md).withMask(wm).run();

        Mask mylabels = fg.proto();
        mylabels.setAll(wm, 1);
        mylabels.setAll(gm, 2);

        Volume mygray = md.proto(1);
        Volume mywhite = md.proto(1);
        for (Sample sample : this.input.getSampling())
        {
            if (this.input.valid(sample, fg))
            {
                double x = md.get(sample, 0);
                double gmd = gmstats.num * MathUtils.gaussian(x, gmstats.mean, gmstats.std);
                double wmd = wmstats.num * MathUtils.gaussian(x, wmstats.mean, wmstats.std);
                mygray.set(sample, 0, gmd / (gmd + wmd));
                mywhite.set(sample, 0, wmd / (gmd + wmd));
            }
        }

        this.labels = mylabels;
        this.gray = mygray;
        this.white = mywhite;

        return this;
    }
}
