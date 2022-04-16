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

package qit.data.modules.mask;

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
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;
import qit.math.structs.VectFunction;
import qit.math.source.VectFunctionSource;

@ModuleDescription("Transform a mask")
@ModuleAuthor("Ryan Cabeen")
public class MaskTransform implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Mask input;

    @ModuleInput
    @ModuleDescription("input reference volume")
    public Volume reference;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask (defined in the reference space)")
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
    @ModuleDescription("reverse the order, i.e. compose the affine(deform(x)), whereas the default is deform(affine(x))")
    public boolean reverse = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a label for filling background voxels")
    public Integer background;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Mask output;

    @Override
    public MaskTransform run()
    {
        Sampling sampling = this.reference.getSampling();
        Mask myinput = this.input;

        if (this.inputMask != null)
        {
            myinput = MaskUtils.and(myinput, this.inputMask);
        }

        VectFunction xfm = VolumeUtils.xfm(this.affine, this.invaffine, this.deform, this.reverse);

        Mask out = new Mask(sampling);
        Sampling samplingTar = myinput.getSampling();
        Sampling samplingRef = out.getSampling();

        for (Sample sampleRef : samplingRef)
        {
            if (out.valid(sampleRef, this.mask))
            {
                Vect sampleWorld = samplingRef.world(sampleRef);
                Vect sampleXfm = xfm.apply(sampleWorld);

                if (samplingTar.contains(sampleXfm))
                {
                    Sample sampleNearest = samplingTar.nearest(sampleXfm);
                    out.set(sampleRef, myinput.get(sampleNearest));
                }
                else if (this.background != null)
                {
                    out.set(sampleRef, this.background);
                }
            }
            else if (this.background != null)
            {
                out.set(sampleRef, this.background);
            }
        }

        this.output = out;
        return this;
    }
}
