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
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;
import qit.base.structs.Pair;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Synthesize data based on a reference dataset")
@ModuleAuthor("Ryan Cabeen")
public class TableSynth implements Module
{
    @ModuleInput
    @ModuleDescription("input reference table")
    public Table reference;

    @ModuleInput
    @ModuleDescription("input sample table")
    public Table sample;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an optional list of variables (comma-separated) of variables for grouping")
    public String group = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the scalar-valued fields (floating point)")
    public String scalar = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the discrete-valued fields (integers)")
    public String discrete = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the categorical-valued fields (factors)")
    public String factor = null;

    @ModuleParameter
    @ModuleDescription("synthesize missing data")
    public boolean missing = false;

    @ModuleParameter
    @ModuleDescription("specify a missing value token")
    public String na = "NA";

    @ModuleOutput
    @ModuleDescription("output synthesized table")
    public Table output;

    @Override
    public TableSynth run() throws IOException
    {
        List<String> grouping = CliUtils.names(this.group, Lists.newArrayList());
        List<String> fields = this.reference.getFields();

        List<String> scalars = CliUtils.names(this.scalar, Lists.newArrayList());
        List<String> discretes = CliUtils.names(this.discrete, Lists.newArrayList());
        List<String> factors = CliUtils.names(this.factor, Lists.newArrayList());

        grouping.retainAll(fields);
        scalars.retainAll(fields);
        discretes.retainAll(fields);
        factors.retainAll(fields);

        Map<Pair<String,Record>, MyFactorMap> factorMaps = Maps.newHashMap();
        Map<Pair<String,Record>, MyDiscreteMap> discreteMaps = Maps.newHashMap();
        Map<Pair<String,Record>, MyScalarMap> scalarMaps = Maps.newHashMap();

        for (Record grec : Sets.newHashSet(this.sample.select(grouping)))
        {
            for (String myfactor : factors)
            {
                factorMaps.put(Pair.of(myfactor, grec), new MyFactorMap(myfactor, grec));
            }

            for (String mydiscrete : discretes)
            {
                discreteMaps.put(Pair.of(mydiscrete, grec), new MyDiscreteMap(mydiscrete, grec));
            }

            for (String myscalar : scalars)
            {
                scalarMaps.put(Pair.of(myscalar, grec), new MyScalarMap(myscalar, grec));
            }
        }

        Table out = new Table();
        out.withFields(this.sample.getFields());
        out.withFields(factors);
        out.withFields(discretes);
        out.withFields(scalars);

        for (Integer key : this.sample.keys())
        {
            Record record = this.sample.getRecord(key).copy();
            Record grec = record.select(grouping);

            for (String myfactor : factors)
            {
                record.with(myfactor, factorMaps.get(Pair.of(myfactor, grec)).sample());
            }

            for (String mydiscrete : discretes)
            {
                record.with(mydiscrete, discreteMaps.get(Pair.of(mydiscrete, grec)).sample());
            }

            for (String myscalar : scalars)
            {
                record.with(myscalar, scalarMaps.get(Pair.of(myscalar, grec)).sample());
            }

            out.addRecord(record);
        }

        this.output = out;

        return this;
    }

    private List<String> unique(String field)
    {
        List<String> values = Lists.newArrayList();
        values.addAll(Sets.newHashSet(TableSynth.this.reference.getFieldValues(field)));
        values.remove(null);
        values.remove("");
        values.remove(" ");
        values.remove("na");
        values.remove("Na");
        values.remove("NA");
        values.remove("nan");
        values.remove("NaN");
        values.remove("Nan");
        values.remove("null");
        values.remove("Null");
        values.remove("NULL");
        values.remove(TableSynth.this.na);

        return values;
    }

    private boolean present(String value)
    {
        return value != null && !value.equals("") && !value.toLowerCase().equals("nan") && !value.toLowerCase().equals("na") && !value.toLowerCase().equals("null") && !value.equals(this.na);
    }

    private class MyFactorMap
    {
        int missing = 0;
        int total = 0;

        List<String> values = null;
        double[] cumpdf = null;

        public MyFactorMap(String field, Record group)
        {
            Logging.info(String.format("Mapping factor %s and %s", field, group.toStringFlat()));

            this.values = unique(field);
            int[] counts = new int[this.values.size()];

            for (Record record : TableSynth.this.reference.where(group))
            {
                String value = record.get(field);
                this.total += 1;

                if (this.values.contains(value))
                {
                    int idx = this.values.indexOf(value);
                    counts[idx] += 1;
                }
                else
                {
                    this.missing += 1;
                }
            }

            this.cumpdf = MathUtils.cumpdf(counts);

            Logging.info("  missing: " + this.missing);
            Logging.info("  total: " + this.total);
            for (int i = 0; i < counts.length; i++)
            {
                Logging.info(String.format("  %s: %d", this.values.get(i), counts[i]));
            }
        }

        public String sample()
        {
            double flip = Global.RANDOM.nextDouble();

            if (TableSynth.this.missing)
            {
                if (flip < (this.missing / (double) this.total))
                {
                    return TableSynth.this.na;
                }
            }

            for (int i = 0; i < this.cumpdf.length; i++)
            {
                if (flip < this.cumpdf[i])
                {
                    return this.values.get(i);
                }
            }

            if (this.values.size() > 0)
            {
                return this.values.get(this.values.size() - 1);
            }
            else
            {
                return TableSynth.this.na;
            }
        }
    }

    private class MyScalarMap
    {
        int missing = 0;
        int total = 0;

        VectOnlineStats stats = new VectOnlineStats();

        public MyScalarMap(String field, Record group)
        {
            Logging.info(String.format("Mapping scalar %s and %s", field, group.toStringFlat()));

            for (Record record : TableSynth.this.reference.where(group))
            {
                String value = record.get(field);
                this.total += 1;

                if (present(value))
                {
                    try
                    {
                        double scalar = Double.parseDouble(value);
                        this.stats.update(scalar);
                    }
                    catch (RuntimeException e)
                    {
                        Logging.info("warning: failed to parse: " + value);
                        this.missing += 1;
                    }
                }
                else
                {
                    this.missing += 1;
                }
            }

            Logging.info("  missing: " + this.missing);
            Logging.info("  total: " + this.total);
            Logging.info("  mean: " + this.stats.mean);
            Logging.info("  std: " + this.stats.std);
        }

        public String sample()
        {
            if (TableSynth.this.missing)
            {
                double flip = Global.RANDOM.nextDouble();
                if (flip < (this.missing / (double) this.total))
                {
                    return TableSynth.this.na;
                }
            }

            double gauss = this.stats.mean + Global.RANDOM.nextGaussian() * this.stats.std;
            return String.valueOf(gauss);
        }
    }

    private class MyDiscreteMap extends MyScalarMap
    {
        public MyDiscreteMap(String field, Record group)
        {
            super(field, group);
        }

        public String sample()
        {
            return String.valueOf(MathUtils.round(Double.valueOf(super.sample())));
        }
    }
}
