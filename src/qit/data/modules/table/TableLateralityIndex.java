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

import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Record;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.utils.TableUtils;
import qit.math.utils.MathUtils;

import java.util.Map;
import java.util.Set;

@ModuleDescription("Compute the lateralization index of a given map.  This assumes you have a two column table (name,value) that contains left and right measurements (see module parameters to specify the left/right identifiers)")
@ModuleAuthor("Ryan Cabeen")
public class TableLateralityIndex implements Module
{
    @ModuleInput
    @ModuleDescription("input table (should use the naming convention like the module parameters)")
    public Table input;

    @ModuleParameter
    @ModuleDescription("the field holding the name of the measurement")
    public String name = "name";

    @ModuleParameter
    @ModuleDescription("the field holding the value of the measurement")
    public String value = "value";

    @ModuleParameter
    @ModuleDescription("the identifier for left values")
    public String left = "lh_";

    @ModuleParameter
    @ModuleDescription("the identifier for right values")
    public String right = "rh_";

    @ModuleParameter
    @ModuleDescription("the identifier for the lateralization index")
    public String lat = "lat_";

    @ModuleParameter
    @ModuleDescription("the identifier for the absolute lateralization index")
    public String abslat = "abslat_";

    @ModuleParameter
    @ModuleDescription("the identifier for the left-right average")
    public String mean = "mean_";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableLateralityIndex run()
    {
        Global.assume(this.input.hasField(this.name), "the table must have field: " + this.name);
        Global.assume(this.input.hasField(this.value), "the table must have field: " + this.value);

        Schema schema = new Schema();
        schema.add(this.name);
        schema.add(this.value);

        Table output = new Table(schema);

        Map<String,Double> map = TableUtils.createStringDoubleLookup(this.input, this.name, this.value);

        for (String leftKey : map.keySet())
        {
            if (leftKey.contains(this.left))
            {
                String rightKey = leftKey.replace(this.left, this.right);
                String latKey = leftKey.replace(this.left, this.lat);
                String abslatKey = leftKey.replace(this.left, this.abslat);
                String meanKey = leftKey.replace(this.left, this.mean);

                if (map.containsKey(rightKey))
                {
                    double leftValue = map.get(leftKey);
                    double rightValue = map.get(rightKey);

                    double meanValue = 0.5 * (leftValue + rightValue);
                    double latValue = MathUtils.zero(meanValue) ? 0.0 : (leftValue - rightValue) / meanValue;

                    output.addRecord(new Record().with(this.name, meanKey).with(this.value, meanValue));
                    output.addRecord(new Record().with(this.name, latKey).with(this.value, latValue));
                    output.addRecord(new Record().with(this.name, abslatKey).with(this.value, Math.abs(latValue)));
                }
                else
                {
                    Logging.info("warning, key expected but not found: " + rightKey);
                }
            }

        }

        this.output = output;

        return this;
    }
}
