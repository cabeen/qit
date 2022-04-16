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
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.utils.TableUtils;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;

@ModuleDescription("Compute the lateralization index of a given map.  This assumes you have a two column table (name,value) that contains left and right measurements (see module parameters to specify the left/right identifiers)")
@ModuleAuthor("Ryan Cabeen")
public class TableLaterality implements Module
{
    @ModuleInput
    @ModuleDescription("input table (should use the naming convention like the module parameters)")
    public Table input;

    @ModuleParameter
    @ModuleDescription("the identifier for left values")
    public String left = "lh_";

    @ModuleParameter
    @ModuleDescription("the identifier for right values")
    public String right = "rh_";

    @ModuleParameter
    @ModuleDescription("the identifier for the lateralization index")
    public String indlat = "indlat_";

    @ModuleParameter
    @ModuleDescription("the identifier for the absolute lateralization index")
    public String abslat = "abslat_";

    @ModuleParameter
    @ModuleDescription("the identifier for the unilateral averaging")
    public String unilat = "unilat_";

    @ModuleParameter
    @ModuleDescription("the identifier for the left right minimum")
    public String minlat = "minlat_";

    @ModuleParameter
    @ModuleDescription("the identifier for the left right maximum")
    public String maxlat = "maxlat_";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableLaterality run()
    {
        List<Triple<String, String, String>> mapping = Lists.newArrayList();
        List<String> keep = Lists.newArrayList();

        Table output = null;

        for (String field : this.input.getFields())
        {
            if (field.contains(this.left))
            {
                String leftKey = field;
                String rightKey = leftKey.replace(this.left, this.right);

                if (this.input.hasField(rightKey))
                {
                    String newKey = leftKey.replace(this.left, "");
                    mapping.add(Triple.of(leftKey, rightKey, newKey));

                    Logging.infosub("... using pair: (%s, %s)", leftKey, rightKey);
                }
            }
            else if (!field.contains(this.right))
            {
                Logging.infosub("... keeping %s", field);
                keep.add(field);
            }
        }

        Logging.infosub("processing %d records", this.input.getNumRecords());

        for (Integer key : this.input.keys())
        {
            Record myinput = this.input.getRecord(key);
            Record myoutput = new Record();

            for (String field : keep)
            {
                myoutput.with(field, myinput.get(field));
            }

            for (Triple<String, String, String> map : mapping)
            {
                String leftKey = map.a;
                String rightKey = map.b;
                String newKey = map.c;

                try
                {
                    double leftValue = Double.valueOf(myinput.get(leftKey));
                    double rightValue = Double.valueOf(myinput.get(rightKey));

                    double indlatValue = MathUtils.eq(leftValue, rightValue) ? 0.0 : 2.0 * (leftValue - rightValue) / (leftValue + rightValue);
                    double abslatValue = Math.abs(indlatValue);
                    double uniValue = 0.5 * (leftValue + rightValue);
                    double minValue = Math.min(leftValue, rightValue);
                    double maxValue = Math.max(leftValue, rightValue);

                    myoutput.with(this.left   + newKey, leftValue);
                    myoutput.with(this.right  + newKey, rightValue);
                    myoutput.with(this.indlat + newKey, indlatValue);
                    myoutput.with(this.abslat + newKey, abslatValue);
                    myoutput.with(this.unilat + newKey, uniValue);
                    myoutput.with(this.minlat + newKey, minValue);
                    myoutput.with(this.maxlat + newKey, maxValue);

                }
                catch (RuntimeException e)
                {
                    // skip non-numeric values
                }
            }

            if (output == null)
            {
                output = new Table(myoutput.keys());
            }

            output.addRecord(myoutput);
        }

        Logging.infosub("finished with %d records and %d fields", output.getNumRecords(), output.getNumFields());

        this.output = output;

        return this;
    }
}
