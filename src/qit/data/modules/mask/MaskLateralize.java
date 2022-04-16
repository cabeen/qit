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

import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.utils.MaskUtils;

import java.util.List;
import java.util.Map;

@ModuleDescription("Compute the logical AND of two masks")
@ModuleAuthor("Ryan Cabeen")
public class MaskLateralize implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleDescription("a mask marking off the left hemisphere (the remainder is assumed to be the right)")
    public Mask left;

    @ModuleParameter
    @ModuleDescription("the left prefix")
    public String leftPrefix = "Left_";

    @ModuleParameter
    @ModuleDescription("the left postfix")
    public String leftPostfix = "";

    @ModuleParameter
    @ModuleDescription("the right prefix")
    public String rightPrefix = "Right_";

    @ModuleParameter
    @ModuleDescription("the right postfix")
    public String rightPostfix = "";

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskLateralize run()
    {
        Global.assume(this.input.getSampling().equals(this.left.getSampling()), "samplings must match");

        Mask out = this.input.proto();

        Table lookup = new Table();
        lookup.withField("name");
        lookup.withField("index");

        List<Integer> labels = MaskUtils.listNonzero(this.input);
        Map<Integer,Integer> leftMap = Maps.newHashMap();
        Map<Integer,Integer> rightMap = Maps.newHashMap();

        for (int i = 0; i < labels.size(); i++)
        {
            int label = labels.get(i);
            String name = this.input.getName(label);

            int leftLabel = i + 1;
            String leftName = this.leftPrefix + name + this.leftPostfix;

            int rightLabel = labels.size() + i + 1;
            String rightName = this.rightPrefix + name + this.rightPostfix;

            leftMap.put(label, leftLabel);
            rightMap.put(label, rightLabel);

            lookup.addRecord(new Record().with("name", leftName).with("index", leftLabel));
            lookup.addRecord(new Record().with("name", rightName).with("index", rightLabel));
        }

        leftMap.put(0, 0);
        rightMap.put(0, 0);

        out.addLookup(lookup);

        for (Sample sample : this.input.getSampling())
        {
            int label = this.input.get(sample);
            out.set(sample, this.left.foreground(sample) ? leftMap.get(label) : rightMap.get(label));
        }

        this.output = out;

        return this;
    }
}

