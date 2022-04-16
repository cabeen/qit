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
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Solids;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.math.source.SelectorSource;
import qit.math.structs.Containable;

@ModuleDescription("Crop a volume down to a smaller volume (only one criteria per run)")
@ModuleAuthor("Ryan Cabeen")
public class VolumeCrop implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("some soids")
    public Solids solids;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a range specification, e.g. start:end,start:end,start:end")
    public String range = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("crop out the background with a given threshold")
    public Double thresh = null;

    @ModuleParameter
    @ModuleDescription("invert the selection")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("ensure the volume has even dimensions")
    public boolean even = false;

    @ModuleParameter
    @ModuleDescription("a padding size in voxels")
    public int pad = 0;

    @ModuleOutput
    @ModuleDescription("the output volume")
    public Volume output = null;

    public VolumeCrop run()
    {
        if (this.range != null)
        {
            Logging.info("cropping with range: " + this.range);
            Sampling sampling = this.input.getSampling().range(this.range);

            this.output = this.input.copy(sampling);

            return this;
        }
        else if (this.even)
        {
            Sampling sampling = this.input.getSampling();
            String ri = sampling.numI() % 2 == 1 ? "0:-1" : ":";
            String rj = sampling.numJ() % 2 == 1 ? "0:-1" : ":";
            String rk = sampling.numK() % 2 == 1 ? "0:-1" : ":";
            String myrange = String.format("%s,%s,%s", ri, rj, rk);

            Logging.info("cropping with range: " + myrange);
            Sampling mysampling = this.input.getSampling().range(myrange);

            this.output = this.input.copy(mysampling);

            return this;
        }
        else
        {
            Containable selector = null;

            if (this.mask != null)
            {
                Logging.info("cropping with mask");
                selector = SelectorSource.mask(this.mask);
            }
            else if (this.solids != null)
            {
                Logging.info("cropping with solids");
                selector = this.solids;
            }
            else if (this.thresh != null)
            {
                Logging.info("cropping with thresh");
                VolumeThreshold module = new VolumeThreshold();
                module.input = this.input;
                module.threshold = this.thresh;
                Mask fg = module.run().output;

                selector = SelectorSource.mask(fg);
            }

            if (selector == null)
            {
                this.output = this.input.copy();
            }
            else
            {
                if (this.invert)
                {
                    selector = SelectorSource.invert(selector);
                }

                Sampling full = this.input.getSampling();
                Sampling sub = full.crop(selector).grow(this.pad);
                Volume cropped = VolumeSource.create(sub, this.input.getDim());
                cropped.setModel(this.input.getModel());

                for (Sample csample : sub)
                {
                    Sample fsample = full.nearest(sub.world(csample));
                    if (full.contains(fsample))
                    {
                        cropped.set(csample, this.input.get(fsample));
                    }
                }

                this.output = cropped;
            }

            return this;
        }
    }

    public static Volume apply(Volume myinput, Solids mysolids)
    {
        VolumeCrop module = new VolumeCrop();
        module.input = myinput;
        module.solids = mysolids;
        return module.run().output;
    }
}