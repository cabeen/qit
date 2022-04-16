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
import qit.data.datasets.Affine;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.enums.ReorientationType;
import qit.data.utils.volume.VolumeSample;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

@ModuleDescription("Transform a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTransform implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("input reference volume")
    public Volume reference;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("use a mask (defined in the reference space)")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("use an input mask (defined in the input space)")
    public Mask inputMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an affine xfm")
    public Affine affine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an inverse affine xfm")
    public Affine invaffine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply a deformation xfm")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("image interpolation method")
    public InterpolationType interp = InterpolationType.Trilinear;

    @ModuleParameter
    @ModuleDescription("reverse the order, i.e. compose the affine(deform(x)), whereas the default is deform(affine(x))")
    public boolean reverse = false;

    @ModuleParameter
    @ModuleDescription("reorient vector image data")
    public boolean reorient = false;

    @ModuleParameter
    @ModuleDescription("specify a reorient method (fs or jac)")
    public ReorientationType reoriention = ReorientationType.Jacobian;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    @Override
    public VolumeTransform run()
    {
        Sampling sampling = this.reference.getSampling();
        Volume myinput = this.input;

        if (this.inputMask != null)
        {
            myinput = VolumeUtils.mask(myinput, this.inputMask);
        }

        VectFunction xfm = VolumeUtils.xfm(this.affine, this.invaffine, this.deform, this.reverse);
        VectFunction func = xfm.compose(VolumeUtils.interp(this.interp, myinput));
        VolumeSample sampler = new VolumeSample();
        sampler.threads = this.threads;
        sampler.withSampling(sampling);
        sampler.withFunction(func);

        if (this.mask != null)
        {
            sampler.withMask(this.mask);
        }

        Volume out = sampler.getOutput();

        if (this.reorient)
        {
            Volume sampleXfm = new VolumeSample().withSampling(sampling).withFunction(xfm).getOutput();

            Matrix[] rf = null;
            if (ReorientationType.FiniteStrain.equals(this.reoriention))
            {
                rf = VolumeUtils.finitestrain(sampleXfm, this.mask);
            }
            else
            {
                rf = VolumeUtils.jacobian(sampleXfm, this.mask);
            }

            VolumeUtils.reorient(out, rf);
        }

        out.setModel(this.input.getModel());

        this.output = out;
        return this;
    }
}
