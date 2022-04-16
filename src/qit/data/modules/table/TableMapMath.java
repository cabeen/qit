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

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.math.utils.expression.ScalarExpression;

import java.math.BigDecimal;

@ModuleDescription("Evaluate an expression with a map (a table listing name/value pairs)")
@ModuleAuthor("Ryan Cabeen")
public class TableMapMath implements Module
{
    @ModuleInput
    @ModuleDescription("the input table map")
    public Table input;

    @ModuleParameter
    @ModuleDescription("the expression to evaluate")
    public String expression = "x > 0.5";

    @ModuleParameter
    @ModuleDescription("the field name for the name")
    public String name = "name";

    @ModuleParameter
    @ModuleDescription("the field name for the value")
    public String value = "value";

    @ModuleParameter
    @ModuleDescription("the name of the result")
    public String result = "result";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    public TableMapMath run()
    {
        ScalarExpression e = new ScalarExpression(this.expression);

        Global.assume(this.input.hasField(this.name), "name field not found: " + this.name);
        Global.assume(this.input.hasField(this.value), "value field not found: " + this.value);

        for (int key : this.input.getKeys())
        {
            Record row = this.input.getRecord(key);

            String name = row.get(this.name);
            String value = row.get(this.value);

            try
            {
                e.with(name, new BigDecimal(Double.valueOf(value)));

            }
            catch (RuntimeException re)
            {
                Logging.info("warning: failed to parse value: " + value);
            }
        }

        double r = e.eval().doubleValue();

        Table out = this.input.copy();

        Record row = new Record();
        row.with(this.name, this.result);
        row.with(this.value, String.valueOf(r));
        out.addRecord(row);

        this.output = out;

        return this;
    }
}
