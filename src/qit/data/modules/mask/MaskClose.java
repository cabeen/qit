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

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.source.MaskSource;
import qit.data.utils.MaskUtils;

@ModuleDescription("Close a mask morphologically.  This dilates and then erodes the mask")
@ModuleAuthor("Ryan Cabeen")
public class MaskClose implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleParameter
    @ModuleDescription("the number of times to erode and dilate the mask")
    public int num = 1;

    @ModuleParameter
    @ModuleDescription("select the largest component as an intermediate step between erosion and dilation")
    public boolean largest = false;

    @ModuleParameter
    @ModuleDescription("apply a mode filter as an intermediate step between erosion and dilation")
    public boolean mode = false;

    @ModuleParameter
    @ModuleDescription("specify an element: cross, cube, or sphere. you can also specify an optional size, e.g. cross(3)")
    public String element = MaskSource.DEFAULT_ELEMENT;

    @ModuleParameter
    @ModuleDescription("treat voxels outside mask as background")
    public boolean outside = false;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskClose run()
    {
        Mask mask = this.input;
        
        MaskDilate dilater = new MaskDilate();
        dilater.input = mask;
        dilater.element = this.element;
        dilater.num = this.num;
        mask = dilater.run().output;

        if (this.largest)
        {
            mask = MaskUtils.largest(mask);
        }

        if (this.mode)
        {
            mask = MaskFilterMode.apply(mask);
        }

        MaskErode eroder = new MaskErode();
        eroder.input = mask;
        eroder.element = this.element;
        eroder.outside = this.outside;
        eroder.num = this.num;
        mask = eroder.run().output;

        this.output = mask;
        return this;
    }

    public static Mask apply(Mask mask)
    {
        return new MaskClose()
        {{
            this.input = mask;
            this.num = 1;
        }}.run().output;
    }

    public static Mask apply(Mask mask, int num)
    {
        return new MaskClose()
        {{
            this.input = mask;
            this.num = num;
        }}.run().output;
    }
}
