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
import qit.data.utils.TableUtils;
import qit.data.utils.vects.stats.VectStats;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@ModuleDescription("Compute reproducibility statistics from repeated measures")
@ModuleAuthor("Ryan Cabeen")
public class TableReliability implements Module
{
    @ModuleInput
    @ModuleDescription("input table")
    public Table input;

    @ModuleParameter
    @ModuleDescription("the field to summarize")
    public String value = "value";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("fields for identifying repeated measures (comma separated)")
    public String id = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("fields to group records by (comma separated)")
    public String group = null;

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableReliability run() throws IOException
    {
        List<String> mygroups = CliUtils.names(this.group, Lists.newArrayList());
        List<String> myidgroups = CliUtils.names(this.group, Lists.newArrayList());
        myidgroups.addAll(CliUtils.names(this.id, Lists.newArrayList()));

        TableStats mystats = new TableStats();
        mystats.input = this.input;
        mystats.value = this.value;
        mystats.which = "mean,var,cv,num";
        Table stats = mystats.run(myidgroups).output;
        Map<Record, List<Record>> grouped = TableUtils.group(stats, mygroups);

        Table out = new Table();
        for (String g : mygroups)
        {
            out.withField(g);
        }

        out.withField("icc");
        out.withField("cv_mean");
        out.withField("cv_std");
        out.withField("cv_median");
        out.withField("cv_mad");
        out.withField("cv_max");
        out.withField("num_mean");
        out.withField("rmse");
        out.withField("within");
        out.withField("between");

        for (Record group : grouped.keySet())
        {
            List<Record> recs = grouped.get(group);
            VectStats means = VectStats.stats(TableUtils.vect(recs, "mean").removeNaN());
            VectStats vars = VectStats.stats(TableUtils.vect(recs, "var").removeNaN());
            VectStats cvs = VectStats.stats(TableUtils.vect(recs, "cv").removeNaN());
            VectStats nums= VectStats.stats(TableUtils.vect(recs, "num").removeNaN());

            double within = vars.mean;
            double between = means.var;

            double rmse = Math.sqrt(vars.mean);
            double icc = (between - within) / (between + within);

            Record record = group.copy();

            record.with("rmse", rmse);
            record.with("icc", icc);
            record.with("cv_mean", cvs.mean);
            record.with("cv_std", cvs.std);
            record.with("cv_median", cvs.median);
            record.with("cv_mad", cvs.mad);
            record.with("cv_max", cvs.max);
            record.with("num_mean", nums.mean);
            record.with("within", within);
            record.with("between", between);

            out.addRecord(record);
        }

        this.output = out;

        return this;
    }
}