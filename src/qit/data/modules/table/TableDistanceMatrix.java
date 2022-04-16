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

package qit.data.modules.table;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Triple;
import qit.data.datasets.Record;
import qit.data.datasets.Table;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Create a distance matrix from pairs of distances")
@ModuleAuthor("Ryan Cabeen")
public class TableDistanceMatrix implements Module
{
    @ModuleInput
    @ModuleDescription("input table (should have field for the left and right identifiers)")
    public Table input;

    @ModuleParameter
    @ModuleDescription("the identifier for left values")
    public String left = "left";

    @ModuleParameter
    @ModuleDescription("the identifier for right values")
    public String right = "right";

    @ModuleParameter
    @ModuleDescription("the identifier for right values")
    public String value = "value";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableDistanceMatrix run()
    {
        Global.assume(this.input.hasField(this.left), "left fieid was not found: " + this.left);
        Global.assume(this.input.hasField(this.right), "right fieid was not found: " + this.right);
        Global.assume(this.input.hasField(this.value), "value fieid was not found: " + this.value);

        Set<String> fieldset = Sets.newLinkedHashSet();

        for (int key : this.input.keys())
        {
            fieldset.add(this.input.getRecord(key).get(this.left));
        }

        for (int key : this.input.keys())
        {
            fieldset.add(this.input.getRecord(key).get(this.right));
        }

        List<String> fields = Lists.newArrayList(fieldset);
        Collections.sort(fields);

        Map<String,Record> map = Maps.newHashMap();

        for (String a : fields)
        {
            Record record = new Record();
            record.with("name", a);
            for (String b : fields)
            {
                record.with(b, "0");
            }
            map.put(a, record);
        }

        for (int key : this.input.keys())
        {
            Record record = this.input.getRecord(key);
            String left = record.get(this.left);
            String right = record.get(this.right);
            String value = record.get(this.value);

            map.get(left).with(right, value);
            map.get(right).with(left, value);
        }

        Table out = new Table();
        out.addField("name");

        for (String field : fields)
        {
            out.addField(field);
        }

        for (String field : fields)
        {
            out.addRecord(map.get(field));
        }

        this.output = out;

        return this;
    }
}
