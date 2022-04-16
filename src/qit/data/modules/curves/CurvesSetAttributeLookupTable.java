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

package qit.data.modules.curves;

import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Table;
import qit.data.modules.table.TableMerge;
import qit.data.source.VectSource;
import qit.data.utils.TableUtils;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.Map;

@ModuleDescription("Set vertex attributes of curves based on a table.  The curves should have a discrete-valued attribute that is used to match vertices to entries in the table.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesSetAttributeLookupTable implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves curves;

    @ModuleInput
    @ModuleDescription("input table")
    public Table table;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a lookup table to relate names to indices")
    public Table lookup = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a table field name to merge on")
    public String mergeTable = "name";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a lookup field name to merge on")
    public String mergeLookup = "along_name";

    @ModuleParameter
    @ModuleDescription("the lookup table index field name")
    public String index = "along_index";

    @ModuleParameter
    @ModuleDescription("a table field to get")
    public String value = "value";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the curves index field name (defaults to lookup table index field name)")
    public String cindex = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a curves field to set (defaults to table field name)")
    public String cvalue = null;

    @ModuleParameter
    @ModuleDescription("a background value")
    public double background = 0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an missing value")
    public Double missing = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("exclude data with no entry in the table")
    public boolean exclude = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @Override
    public CurvesSetAttributeLookupTable run() throws IOException
    {
        Curves curves = this.inplace ? this.curves : this.curves.copy();

        Table mtable = this.table;

        if (this.lookup != null)
        {
            TableMerge module = new TableMerge();
            module.left = mtable;
            module.right = this.lookup;
            module.leftField = this.mergeTable;
            module.rightField = this.mergeLookup;
            mtable = module.run().output;
            mtable.write("/tmp/test.csv");
        }

        String curvesIndexField = this.cindex == null ? this.index : this.cindex;
        Global.assume(curves.has(curvesIndexField), "expected curves to have index field: " + curvesIndexField);

        String[] values = this.value.split(",");
        String[] cvalues = this.cvalue == null ? values : this.cvalue.split(",");

        Global.assume(values.length == cvalues.length, "value names have difference sizes");

        for (int i = 0; i < values.length; i++)
        {
            Map<Integer, Double> lut = TableUtils.createIntegerDoubleLookup(mtable, this.index, values[i]);

            if (!lut.containsKey(0))
            {
                lut.put(0, this.background);
            }

            if (curves.has(cvalues[i]))
            {
                curves.add(cvalues[i], VectSource.create1D());
            }

            boolean[] filter = new boolean[curves.size()];
            for (int c = 0; c < curves.size(); c++)
            {
                Curves.Curve curve = curves.get(c);

                for (int j = 0; j < curve.size(); j++)
                {
                    int label = MathUtils.round(curve.get(curvesIndexField, j).get(0));

                    if (lut.containsKey(label))
                    {
                        curve.set(cvalues[i], j, VectSource.create1D(lut.get(label)));
                        filter[c] = true;
                    }
                    else
                    {
                        filter[c] = false;
                        if (this.missing != null)
                        {
                            curve.set(cvalues[i], j, VectSource.create1D(this.missing));
                        }
                    }
                }
            }

            if (this.exclude)
            {
                curves.keep(filter);
            }
        }

        this.output = curves;

        return this;
    }
}
