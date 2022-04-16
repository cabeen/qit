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

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Record;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.utils.TableUtils;
import qit.math.utils.MathUtils;

import java.util.Map;

@ModuleDescription("Filter a table, a few options are available")
@ModuleAuthor("Ryan Cabeen")
public class TableFilter implements Module
{
    @ModuleInput
    @ModuleDescription("input table")
    public Table input;

    @ModuleInput
    @ModuleDescription("add a field for the mean value based on a table of volumes for each field (this is an unusual need)")
    public Table volumes;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the name of the field for saving the mean value")
    public String meanField = "mean";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the name of the field for merging tables")
    public String mergeField = "subject";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a token for missing values")
    public String missing = "NA";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableFilter run()
    {
        Table table = this.input;

        if (this.volumes != null)
        {
            Schema schema = this.input.getSchema();
            schema.add(this.meanField, this.missing);
            table = new Table(schema);

            Map<String, Record> lookup = TableUtils.createLookup(this.volumes, this.mergeField);

            for (Integer key : this.input.keys())
            {
                Record values = this.input.getRecord(key).copy();
                String mergeValue = values.get(this.mergeField);
                if (!lookup.containsKey(mergeValue))
                {
                    Logging.infosub("warning, skipping %s as no match was found", mergeValue);
                    continue;
                }

                Record vols = lookup.get(mergeValue);

                double sum = 0;
                double dot = 0;

                for (String field : values)
                {
                    if (!field.equals(this.mergeField) && vols.containsKey(field))
                    {
                        try
                        {
                            double value = Double.parseDouble(values.get(field));
                            double vol = Double.parseDouble(vols.get(field));

                            sum += vol;
                            dot += value * vol;
                        }
                        catch (RuntimeException e)
                        {
                            // skip failed pairings
                        }
                    }
                }

                String mean = MathUtils.zero(sum) ? this.missing : String.valueOf(dot / sum);

                values.with(this.meanField, mean);

                table.addRecord(values);
            }
        }

        this.output = table;

        return this;
    }
}
