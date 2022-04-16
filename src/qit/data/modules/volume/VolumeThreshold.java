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

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;

@ModuleDescription("Threshold a volume to make a mask")
@ModuleAuthor("Ryan Cabeen")
public class VolumeThreshold implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;
    
    @ModuleInput
    @ModuleOptional
    @ModuleDescription("mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("threshold value")
    public Double threshold = 0.5;

    @ModuleParameter
    @ModuleDescription("use the magnitude for multi-channel volumes")
    public boolean magnitude = false;

    @ModuleParameter
    @ModuleDescription("invert the threshold")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("normalize the intensities to unit mean before thresholding")
    public boolean prenorm = false;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    public VolumeThreshold run()
    {
        Volume volume = this.input;

        if (this.prenorm)
        {
            VolumeNormalize normer = new VolumeNormalize();
            normer.input = volume;
            normer.mask = this.mask;
            normer.type = VolumeNormalize.VolumeNormalizeType.Mean;
            volume = normer.run().output;
        }

        Mask out = apply(volume, this.mask, this.threshold, this.magnitude);

        if (this.invert)
        {
            out = MaskUtils.invert(out, this.mask);

        }

        this.output = out;

        return this;
    }

    public static Mask apply(Volume input, double threshold)
    {
        return apply(input, null, threshold);
    }

    public static Mask applyInverse(Volume input, Mask mask, double threshold)
    {
        return MaskUtils.and(MaskUtils.invert(apply(input, null, threshold)), mask);
    }

    public static Mask applyInverse(Volume input, double threshold)
    {
        return MaskUtils.invert(apply(input, null, threshold));
    }

    public static Mask apply(Volume input, Mask mask, double threshold)
    {
        Mask out = new Mask(input.getSampling());

        for (Sample sample : input.getSampling())
        {
            if (input.valid(sample, mask))
            {
                double value = input.get(sample, 0);
                if (value >= threshold)
                {
                    out.set(sample, 1);
                }
            }
        }

        return out;
    }

    public static Mask apply(Volume input, Mask mask, double threshold, boolean mag)
    {
        Mask out = new Mask(input.getSampling());

        for (Sample sample : input.getSampling())
        {
            if (input.valid(sample, mask))
            {
                Vect vect = input.get(sample);
                double value = mag && input.getDim() > 1 ? vect.norm() : vect.get(0);
                if (value >= threshold)
                {
                    out.set(sample, 1);
                }
            }
        }

        return out;
    }

    public static Volume applyToVolume(Volume input, Double threshold)
    {
        if (threshold == null)
        {
            return input;
        }

        Mask threshmask = apply(input, threshold);
        return VolumeUtils.mask(input, threshmask);
    }

    public static Volume applyToVolume(Volume input, Mask mask, Double threshold)
    {
        if (threshold == null)
        {
            return input;
        }

        Mask threshmask = apply(input, mask, threshold);
        return VolumeUtils.mask(input, threshmask);
    }
}
