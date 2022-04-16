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
import org.apache.commons.lang3.text.StrSubstitutor;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.utils.TableUtils;

import java.util.Map;
import java.util.Set;

@ModuleDescription("Split mask regions at the midline to produce left and right regions")
@ModuleAuthor("Ryan Cabeen")
public class MaskSplitMidline implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input lookup")
    public Table lookup;

    @ModuleParameter
    @ModuleDescription("a pattern for renaming entries")
    public String pattern = "%{hemi}_%{name}";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify a midline index (otherwise the middle is used)")
    public Integer midline = null;

    @ModuleParameter
    @ModuleDescription("specify an offset value (0 or 1)")
    public Integer start = 1;

    @ModuleParameter
    @ModuleDescription("specify an identifier for the left hemisphere")
    public String left = "left";

    @ModuleParameter
    @ModuleDescription("specify an identifier for the right hemisphere")
    public String right = "right";

    @ModuleParameter
    @ModuleDescription("the name field in the lookup table")
    public String name = "name";

    @ModuleParameter
    @ModuleDescription("the index field in the lookup table")
    public String index = "index";

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask outputMask;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output lookup")
    public Table outputLookup;

    @Override
    public MaskSplitMidline run()
    {
        Mask maskOutput = this.mask.proto();
        Map<String, Integer> lookupInput = TableUtils.createStringIntegerLookup(this.lookup, this.name, this.index);
        Map<String, Integer> lookupOutput = Maps.newLinkedHashMap();

        int max = 0;
        for (String key : lookupInput.keySet())
        {
            max = Math.max(max, lookupInput.get(key));
        }

        int add = 1;
        for (int n : new int[]{10, 100, 1000, 10000, 100000, 1000000})
        {
            if (max < n)
            {
                add = n;
                break;
            }
        }

        for (String name : lookupInput.keySet())
        {
            int index = lookupInput.get(name);

            Map<String, String> replace = Maps.newLinkedHashMap();
            replace.put("name", name);

            replace.put("hemi", this.left);
            String nameLeft = new StrSubstitutor(replace).replace(this.pattern.replace('%', '$'));

            replace.put("hemi", this.right);
            String nameRight = new StrSubstitutor(replace).replace(this.pattern.replace('%', '$'));

            lookupOutput.put(nameLeft, index + this.start * add);
            lookupOutput.put(nameRight, index + (this.start + 1) * add);
        }

        int mymidline = this.midline != null ? this.midline : (this.mask.getSampling().numI()) / 2;

        for (Sample sample : this.mask.getSampling())
        {
            int index = this.mask.get(sample);

            if (index > 0)
            {
                int offset = sample.getI() < mymidline ? 0 : 1;
                maskOutput.set(sample, index + (this.start + offset) * add);
            }
        }

        Table outLookup = new Table();
        outLookup.withField(this.name);
        outLookup.withField(this.index);
        for (String n : lookupOutput.keySet())
        {
            Record row = new Record();
            row.with(this.name, n);
            row.with(this.index, lookupOutput.get(n));
            outLookup.addRecord(row);
        }

        this.outputMask = maskOutput;
        this.outputLookup = outLookup;

        return this;
    }
}

