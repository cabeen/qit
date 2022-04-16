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

import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.volume.VolumeInterpTrilinear;
import qit.data.utils.volume.VolumeSample;
import qit.math.utils.MathUtils;

@ModuleDescription("Zoom a volume.  Note: be careful using this for downsampling, as it does not apply an anti-aliasing prefilter.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeZoom implements Module
{
    @ModuleInput
    @ModuleDescription("the input volume")
    public Volume input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an isotropic scaling factor")
    public Double factor = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a scaling factor in i")
    public Double fi = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a scaling factor in j")
    public Double fj = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a scaling factor in k")
    public Double fk = null;

    @ModuleParameter
    @ModuleDescription("an interpolation type")
    public InterpolationType interp = InterpolationType.Trilinear;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 3;

    @ModuleOutput
    @ModuleDescription("the output volume")
    public Volume output;

    @Override
    public VolumeZoom run()
    {
        if (this.factor != null)
        {
            this.output = zoom(this.factor, this.factor, this.factor,this.input, this.interp, this.threads);
        }
        else
        {
            this.output = zoom(this.fi, this.fj, this.fk, this.input, this.interp, this.threads);
        }

        return this;
    }

    private static Volume zoom(double fi, double fj, double fk, Volume volume, InterpolationType interp, int threads)
    {
        Global.assume(fi >= 0 && !MathUtils.zero(fi),"invalid zoom factor i:" + fi);
        Global.assume(fj >= 0 && !MathUtils.zero(fj),"invalid zoom factor j:" + fj);
        Global.assume(fk >= 0 && !MathUtils.zero(fk),"invalid zoom factor k:" + fk);

        Sampling nsampling = volume.getSampling().zoom(fi, fj, fk);
        VolumeSample sampler = new VolumeSample();
        sampler.threads = threads;
        sampler.sampling = nsampling;
        sampler.function = VolumeUtils.interp(interp, volume);
        Volume nvolume = sampler.run().output;

        return nvolume;
    }
}
