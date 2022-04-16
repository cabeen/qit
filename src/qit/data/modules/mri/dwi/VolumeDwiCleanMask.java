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


package qit.data.modules.mri.dwi;

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskErode;
import qit.data.modules.mask.MaskFilterMode;
import qit.data.modules.mask.MaskShell;
import qit.data.modules.mri.model.VolumeModelError;
import qit.data.modules.mri.tensor.VolumeTensorFit;
import qit.data.modules.volume.VolumeMeasure;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.VectSource;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.volume.VolumeFunction;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import smile.math.kernel.MercerKernel;
import smile.regression.GaussianProcessRegression;

import java.util.List;
import java.util.function.Supplier;

@ModuleDescription("Clean a brain mask to remove background voxels")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiCleanMask implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the input gradients (must match input DWI)")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("cleaning depth from the boundary")
    public int depth = 2;

    @ModuleParameter
    @ModuleDescription("the threshold (multiplier for inter-quartile range)")
    public Double threshold = 1.5;

    @ModuleParameter
    @ModuleDescription("skip mode filtering")
    public boolean nomode = false;

    @ModuleParameter
    @ModuleDescription("the number of threads to use")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("the output mask")
    public Mask output;

    public VolumeDwiCleanMask run()
    {
        VolumeTensorFit fitter = new VolumeTensorFit();
        fitter.input = this.input;
        fitter.gradients = this.gradients;
        fitter.mask = this.mask;
        fitter.method = VolumeTensorFit.TensorFitType.LLS;
        Volume tensors = fitter.run().output;

        VolumeModelError modelerr = new VolumeModelError();
        modelerr.input = tensors;
        modelerr.dwi = this.input;
        modelerr.gradients = this.gradients;
        modelerr.mask = this.mask;
        modelerr.type = VolumeModelError.ModelErrorType.NMAD;
        Volume error = modelerr.run().output;

        Mask shell = MaskShell.apply(this.mask, this.depth);
        Mask erode = MaskErode.apply(this.mask, this.depth);
        Mask sample = MaskShell.apply(erode, 3 * this.depth);

        VolumeVoxelStats stats = new VolumeVoxelStats().withInput(error).withMask(sample).run();
        double mythresh = stats.qhigh + this.threshold * stats.iqr;

        Logging.info("detected error median: " + stats.median);
        Logging.info("detected error qhigh: " + stats.qhigh);
        Logging.info("detected error iqr: " + stats.iqr);
        Logging.info("detected error thresh: " + mythresh);

        Mask background = VolumeThreshold.apply(error, shell, mythresh);
        Mask out = this.mask.copy();
        out.setAll(background, 0);

        if (!this.nomode)
        {
            out = MaskFilterMode.apply(out);
        }

        this.output = out;

        return this;
    }
}