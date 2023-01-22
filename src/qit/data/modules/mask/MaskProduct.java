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
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.utils.MaskUtils;

import java.util.Collections;

@ModuleDescription("Compute the product of two multi-label masks.  Unlike MaskIntersection, this will make sure the product is unique and the names are updated.")
@ModuleAuthor("Ryan Cabeen")
public class MaskProduct implements Module
{
    @ModuleInput
    @ModuleDescription("input left mask")
    public Mask left;

    @ModuleInput
    @ModuleDescription("input right mask")
    public Mask right;

    @ModuleParameter
    @ModuleDescription("the string for combining region names")
    public String sep = "_";

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskProduct run()
    {
        Global.assume(this.left.getSampling().equals(this.right.getSampling()), "samplings must match");

        int maxLeft = Collections.max(MaskUtils.listNonzero(this.left));
        int maxRight = Collections.max(MaskUtils.listNonzero(this.right));

        Mask out = this.left.proto();
        for (int i = 1; i <= maxLeft; i++)
        {
            for (int j = 1; j <= maxRight; j++)
            {
                if (i > 0 && j > 0)
                {
                    int newLabel = (i - 1) * maxRight + j;
                    String newName = this.left.getName(i) + this.sep + this.right.getName(j);
                    out.setName(newLabel, newName);
                }
            }
        }

        for (Sample sample : this.left.getSampling())
        {
            int leftLabel = this.left.get(sample);
            int rightLabel = this.right.get(sample);

            int newLabel = (leftLabel - 1) * maxRight + rightLabel;

            if (leftLabel != 0 && rightLabel != 0)
            {
                out.set(sample, newLabel);
            }
        }

        this.output = out;

        return this;
    }
}

