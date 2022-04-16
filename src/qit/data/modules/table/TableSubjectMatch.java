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
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.TableUtils;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.math.structs.Gaussian;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@ModuleDescription("Match subjects into pairs.  You should provide a field that stores a binary variable on which to split.  The smaller group will be paired with subjects from the bigger group, and optionally, the pairing can be optimized to maximize similarity between paired individuals based on a number of scalar and discrete variables")
@ModuleAuthor("Ryan Cabeen")
public class TableSubjectMatch implements Module
{
    @ModuleInput
    @ModuleDescription("input table")
    public Table input;

    @ModuleParameter
    @ModuleDescription("the field specifying the subject identifier")
    public String subject = "subject";

    @ModuleParameter
    @ModuleDescription("the field for splitting the group (should be binary)")
    public String split = "subject";

    @ModuleParameter
    @ModuleDescription("a token for specifying missing data")
    public String na = "NA";

    @ModuleParameter
    @ModuleDescription("the field name for distance in the output")
    public String distance = "distance";

    @ModuleParameter
    @ModuleDescription("scalar fields to include when computing subject distances (comma separated list)")
    public String scalar = "";

    @ModuleParameter
    @ModuleDescription("discrete fields to include when computing subject distances (comma separated list)")
    public String discrete = "";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableSubjectMatch run()
    {
        Function<Pair<Record,Record>, Double> dister = this.dister();

        Map<String, Record> map = TableUtils.createLookup(this.input, this.subject);

        Map<String,Set<String>> groups = Maps.newHashMap();
        for (String subject : map.keySet())
        {
            Record record = map.get(subject);
            String group = record.get(this.split);

            if (group == null || group.equals(this.na))
            {
                continue;
            }

            if (!groups.containsKey(group))
            {
                groups.put(group, Sets.newLinkedHashSet());
            }

            groups.get(group).add(subject);
        }

        Global.assume(groups.size() == 2, "expected two values in split field: " + this.split);

        List<String> gvals = Lists.newArrayList(groups.keySet());
        int asize = groups.get(gvals.get(0)).size();
        int bsize = groups.get(gvals.get(1)).size();

        String lowgroup = asize < bsize ? gvals.get(0) : gvals.get(1);
        String highgroup = asize < bsize ? gvals.get(1) : gvals.get(0);

        Table out = new Table();
        out.addField(lowgroup);
        out.addField(highgroup);
        out.addField(this.distance);

        Set<String> candidates = groups.get(highgroup);
        for (String lowsub : groups.get(lowgroup))
        {
            Record lowrec = map.get(lowsub);
            double mindist = Double.MAX_VALUE;
            String minsub = null;

            for (String highsub : candidates)
            {
                Record highrec = map.get(highsub);
                Double dist = dister.apply(Pair.of(lowrec, highrec));

                if (dist != null && dist < mindist)
                {
                    mindist = dist;
                    minsub = highsub;
                }
            }

            if (minsub != null)
            {
                Double dist = dister.apply(Pair.of(lowrec, map.get(minsub)));

                candidates.remove(minsub);

                Record outrec = new Record();
                outrec.with(lowgroup, lowsub);
                outrec.with(highgroup, minsub);
                outrec.with(this.distance, mindist);

                out.addRecord(outrec);
            }
        }

        this.output = out;

        return this;
    }

    public Function<Pair<Record,Record>, Double> dister()
    {
        final List<String> discrete = Lists.newArrayList();
        final Map<String, Double> scalar = Maps.newHashMap();

        for (String token : this.discrete.split(","))
        {
            if (this.input.hasField(token))
            {
                discrete.add(token);
            }
        }

        for (String token : this.scalar.split(","))
        {
            if (this.input.hasField(token))
            {
                double std = VectStats.stats(TableUtils.vect(this.input, token)).std;
                scalar.put(token, std);
            }
        }

        return (pair) ->
        {
            double sum = 0;
            int count = 0;

            for (String field : discrete)
            {
                String aval = pair.a.get(field);
                String bval = pair.b.get(field);

                sum += aval.equals(bval) ? 0 : 1;
                count += 1;
            }

            for (String field : scalar.keySet())
            {
                try
                {
                    double aval = Double.valueOf(pair.a.get(field));
                    double bval = Double.valueOf(pair.b.get(field));
                    double std = scalar.get(field);

                    if (MathUtils.nonzero(std))
                    {
                        sum += Math.abs(aval - bval) / std;
                        count += 1;
                    }
                }
                catch (RuntimeException e)
                {
                    // skip bad cases
                }
            }

            if (count == 0)
            {
                return null;
            }
            else
            {
                return sum / (double) count;
            }
        };
    }
}
