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
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.source.TableSource;
import qit.data.utils.vects.stats.VectOnlineStats;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Compute summary statistics of a field of a table")
@ModuleAuthor("Ryan Cabeen")
public class TableStats implements Module
{
    @ModuleInput
    @ModuleDescription("input table")
    public Table input;

    @ModuleParameter
    @ModuleDescription("the field to summarize")
    public String value = "value";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("fields to group records by (comma separated)")
    public String group = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a pattern for saving statistics (contains %s)")
    public String pattern = "%s";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("which statistics to include (mean, min, max, sum, var, std, num, cv)")
    public String which = "mean,std";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableStats run() throws IOException
    {
        return run(CliUtils.names(this.group, Lists.newArrayList()));
    }

    public TableStats run(List<String> grouping)
    {
        Map<Record, VectOnlineStats> stats = Maps.newHashMap();

        for (Integer key : input.keys())
        {
            String token = input.get(key, this.value);
            try
            {
                double val = Double.valueOf(token);

                Record record = new Record();
                for (String field : grouping)
                {
                    record.with(field, input.get(key, field));
                }

                if (!stats.containsKey(record))
                {
                    stats.put(record, new VectOnlineStats());
                }

                stats.get(record).update(val);
            }
            catch (Exception e)
            {
                Logging.info("warning, failed to parse: " + token);
            }
        }

        Set<String> valid = Sets.newHashSet(new String[]{"min", "max", "mean", "var", "std", "stde", "sum", "num", "cv"});

        List<String> names = Lists.newArrayList();
        for (String name : this.which.split(","))
        {
            if (valid.contains(name))
            {
                names.add(name);
            }
        }

        List<Record> records = Lists.newArrayList();
        for (Record record : stats.keySet())
        {
            VectOnlineStats stat = stats.get(record);

            for (String name : names)
            {
                switch (name)
                {
                    case "min":
                        record.with(String.format(this.pattern, name), stat.min);
                        break;
                    case "max":
                        record.with(String.format(this.pattern, name), stat.max);
                        break;
                    case "mean":
                        record.with(String.format(this.pattern, name), stat.mean);
                        break;
                    case "sum":
                        record.with(String.format(this.pattern, name), stat.sum);
                        break;
                    case "var":
                        record.with(String.format(this.pattern, name), stat.var);
                        break;
                    case "std":
                        record.with(String.format(this.pattern, name), stat.std);
                        break;
                    case "stde":
                        record.with(String.format(this.pattern, name), stat.stde);
                        break;
                    case "num":
                        record.with(String.format(this.pattern, name), stat.num);
                        break;
                    case "cv":
                        record.with(String.format(this.pattern, name), stat.cv);
                        break;
                }
            }

            records.add(record);
        }

        this.output = TableSource.create(records);

        return this;
    }
}
