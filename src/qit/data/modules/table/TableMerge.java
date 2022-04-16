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

import com.google.common.collect.Maps;

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.Record;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.utils.TableUtils;

import java.util.Map;

@ModuleDescription("Merge two tables based the value of a shared field")
@ModuleAuthor("Ryan Cabeen")
public class TableMerge implements Module
{
    @ModuleInput
    @ModuleDescription("input left table")
    public Table left;

    @ModuleInput
    @ModuleDescription("input right table")
    public Table right;

    @ModuleParameter
    @ModuleDescription("the common field to merge on")
    public String field = "name";

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("use the given left field (instead of the shared merge field)")
    public String leftField = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("use the given right field (instead of the shared merge field)")
    public String rightField = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("add a prefix to the fields from the left table")
    public String leftPrefix = "";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("add a prefix to the fields from the right table")
    public String rightPrefix = "";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("add a postfix to the fields from the left table")
    public String leftPostfix = "";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("add a postfix to the fields from the right table")
    public String rightPostfix = "";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableMerge run()
    {
        Table leftTable = this.left;
        Table rightTable = this.right;

        if (this.leftField != null)
        {
            leftTable = TableUtils.rename(leftTable, String.format("%s=%s", this.field, this.leftField));
        }

        if (this.rightField != null)
        {
            rightTable = TableUtils.rename(rightTable, String.format("%s=%s", this.field, this.rightField));
        }

        Logging.info("merge field: " + this.field);

        Logging.info("left fields:");
        for (String field : leftTable.getFields())
        {
            Logging.info("  " + field);
        }

        Logging.info("right fields:");
        for (String field : rightTable.getFields())
        {
            Logging.info("  " + field);
        }

        Global.assume(leftTable.hasField(this.field), "left table must have field: " + this.field);
        Global.assume(rightTable.hasField(this.field), "right table must have field: " + this.field);

        Schema schema = new Schema();
        schema.add(this.field);

        Map<String, String> leftLookup = Maps.newHashMap();
        Map<String, String> rightLookup = Maps.newHashMap();

        for (String f : leftTable.getFields())
        {
            if (!schema.hasField(f))
            {
                String nf = this.leftPrefix + f + this.leftPostfix;
                leftLookup.put(f, nf);
                schema.add(nf);
            }
        }

        for (String f : rightTable.getFields())
        {
            if (!schema.hasField(f))
            {
                String nf = this.rightPrefix + f + this.rightPostfix;
                rightLookup.put(f, nf);
                schema.add(nf);
            }
        }

        Table merge = new Table(schema);

        // please try to avoid re-implementing a full database below...
        for (Integer leftKey : leftTable.getKeys())
        {
            Record leftRec = leftTable.getRecord(leftKey);
            String match = leftRec.get(this.field);

            for (Integer rightKey : rightTable.getKeys())
            {
                Record rightRec = rightTable.getRecord(rightKey);

                if (rightRec.get(this.field).equals(match))
                {
                    Record rec = new Record();
                    rec.with(this.field, match);

                    for (String f : leftRec.keys())
                    {
                        if (!f.equals(this.field) && leftLookup.containsKey(f))
                        {
                            rec.with(leftLookup.get(f), leftRec.get(f));
                        }
                    }

                    for (String f : rightRec.keys())
                    {
                        if (!f.equals(this.field) && rightLookup.containsKey(f))
                        {
                            rec.with(rightLookup.get(f), rightRec.get(f));
                        }
                    }

                    merge.addRecord(rec);
                }
            }
        }

        this.output = merge;

        return this;
    }
}
