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

package qit.data.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.text.StrSubstitutor;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Triple;
import qit.base.utils.StringUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.TableSource;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectStats;
import qit.math.utils.MathUtils;
import qit.math.utils.expression.StringExpression;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * utilties for processing tables
 */
public class TableUtils
{
    public static Map<Record, List<Record>> group(Table table, String groupby)
    {
        List<String> grouping = Lists.newArrayList();

        if (groupby != null)
        {
            for (String g : StringUtils.split(groupby, ','))
            {
                grouping.add(g);
            }
        }

        return group(table, grouping);
    }

    public static Vect vect(Table table, String name)
    {
        return VectSource.create(doubles(table, name));
    }

    public static Vect vect(List<Record> records, String name)
    {
        return VectSource.create(doubles(records, name));
    }

    public static List<Double> doubles(Table table, String name)
    {
        List<Double> values = Lists.newArrayList();

        for (Integer key : table.keys())
        {
            String token = table.get(key, name);
            try
            {
                double val = Double.valueOf(token);
                values.add(val);
            }
            catch (Exception e)
            {
                Logging.info("warning, failed to parse: " + token);
            }
        }

        return values;
    }

    public static List<Double> doubles(List<Record> records, String name)
    {
        List<Double> values = Lists.newArrayList();

        for (Record record : records)
        {
            String token = record.get(name);
            try
            {
                double val = Double.valueOf(token);
                values.add(val);
            }
            catch (Exception e)
            {
                Logging.info("warning, failed to parse: " + token);
            }
        }

        return values;
    }

    public static Map<Record, List<Record>> group(Table table, List<String> groupby)
    {
        if (groupby.size() == 0)
        {
            Map<Record, List<Record>> grouped = Maps.newHashMap();
            grouped.put(new Record(), table.getRecords());
            return grouped;
        }

        Map<Record, List<Record>> grouped = Maps.newHashMap();

        for (Integer key : table.keys())
        {
            Record record = new Record();
            for (String field : groupby)
            {
                record.with(field, table.get(key, field));
            }

            if (!grouped.containsKey(record))
            {
                grouped.put(record, Lists.newArrayList());
            }

            grouped.get(record).add(table.getRecord(key));
        }

        return grouped;
    }

    public static Map<String, Record> createLookup(Table table, String field)
    {
        Global.assume(table.hasField(field), "expected an from field: " + field);

        Map<String, Record> lut = Maps.newHashMap();
        for (Integer key : table.getKeys())
        {
            Record row = table.getRecord(key);
            String value = row.get(field);

            if (lut.containsKey(value))
            {
                Logging.info("warning: found repeated entry for " + value);
            }

            lut.put(value, row);
        }

        return lut;
    }

    public static Map<String, String> createStringLookup(Table table, String from, String to)
    {
        Global.assume(table.hasField(from), "expected an from field: " + from);
        Global.assume(table.hasField(to), "expected a value field: " + to);

        Map<String, String> lut = Maps.newHashMap();
        for (Integer key : table.getKeys())
        {
            Record row = table.getRecord(key);

            String fromElem = row.get(from);
            String toElem = row.get(to);

            lut.put(fromElem, toElem);
        }

        return lut;
    }

    public static Map<String, Double> createStringDoubleLookup(Table table, String from, String to)
    {
        Global.assume(table.hasField(from), "expected an from field: " + from);
        Global.assume(table.hasField(to), "expected a value field: " + to);

        Map<String, Double> lut = Maps.newHashMap();
        for (Integer key : table.getKeys())
        {
            Record row = table.getRecord(key);

            String fromElem = row.get(from);
            String tooElem = row.get(to);

            try
            {
                Double toValue = Double.valueOf(String.valueOf(tooElem));
                lut.put(fromElem, toValue);
            }
            catch (Exception e)
            {
                // skip entry with non-numeric entries, e..g NA
            }
        }

        return lut;
    }

    public static Map<Integer, Double> createIntegerDoubleLookup(Table table, String from, String to)
    {
        Global.assume(table.hasField(from), "expected a from field: " + from);
        Global.assume(table.hasField(to), "expected a value field: " + to);

        Map<Integer, Double> lut = Maps.newHashMap();
        for (Integer key : table.getKeys())
        {
            Record row = table.getRecord(key);

            String fromElem = row.get(from);
            String tooElem = row.get(to);

            Integer fromValue = Integer.valueOf(fromElem);
            if (lut.containsKey(fromValue))
            {
                Logging.info("warning: duplicate key found: " + fromValue);
            }

            try
            {
                Double toValue = Double.valueOf(tooElem);

                lut.put(fromValue, toValue);
            }
            catch (Exception e)
            {
                // skip entry with non-numeric entries, e..g NA
            }

        }

        return lut;
    }

    public static Map<Integer, String> createIntegerStringLookup(Table table, String from, String to)
    {
        Global.assume(table.hasField(from), "expected an from field: " + from);
        Global.assume(table.hasField(to), "expected a value field: " + to);

        Map<Integer, String> lut = Maps.newHashMap();
        for (Integer key : table.getKeys())
        {
            Record row = table.getRecord(key);

            String fromElem = row.get(from);
            String toElem = row.get(to);

            lut.put(Integer.valueOf(fromElem), toElem);
        }

        return lut;
    }

    public static Map<String, Integer> createStringIntegerLookup(Table table, String from, String to)
    {
        Global.assume(table.hasField(from), "expected an from field: " + from);
        Global.assume(table.hasField(to), "expected a value field: " + to);

        Map<String, Integer> lut = Maps.newHashMap();
        for (Integer key : table.getKeys())
        {
            Record row = table.getRecord(key);

            String fromElem = row.get(from);
            String toElem = row.get(to);

            lut.put(fromElem, Integer.valueOf(toElem));
        }

        return lut;
    }

    public static Record createLookup(Table table)
    {
        Global.assume(table.getNumFields() >= 2, "table does not encode a lookup");

        return createLookup(table, 0, 1);
    }

    public static Record createLookup(Table table, String from, String to)
    {
        Global.assume(table.getNumFields() >= 2, "table does not encode a lookup");

        Integer fromIdx = table.getSchema().getIndex(from);
        Integer toIdx = table.getSchema().getIndex(to);

        Global.assume(fromIdx != null && toIdx != null, "table does not encode a lookup");

        return createLookup(table, fromIdx, toIdx);
    }

    public static Record createLookup(Table table, int fromIdx, int toIdx)
    {
        Global.assume(table.getNumFields() >= 2, "table does not encode a lookup");

        Record out = new Record();
        for (Object[] row : table)
        {
            String from = row[fromIdx].toString();
            String to = row[toIdx].toString();
            out.with(from, to);
        }

        return out;
    }

    public static Map<Integer, Integer> createIntegerLookup(Table table)
    {
        Global.assume(table.getNumFields() >= 2, "table does not encode a lookup");

        Integer fromIdx = table.getSchema().getIndex(Table.FROM);
        Integer toIdx = table.getSchema().getIndex(Table.TO);

        Global.assume(fromIdx != null && toIdx != null, "table does not encode a lookup");

        Map<Integer, Integer> out = new HashMap<Integer, Integer>();
        for (Object[] row : table)
        {
            int from = Integer.valueOf(row[fromIdx].toString());
            int to = Integer.valueOf(row[toIdx].toString());
            out.put(from, to);
        }

        return out;
    }

    public static Table joinInner(List<Table> tables, List<String> names, String field, String delimiter)
    {
        Global.assume(tables.size() != 0, "no tables were found");
        Global.assume(tables.size() == names.size(), "table names do not match");
        Global.assume(tables.get(0).hasField(field), "field '" + field + "' not found");

        int num = tables.size();
        Set<Object> values = Sets.newLinkedHashSet();
        Schema schema = new Schema();
        schema.add(field);
        for (int i = 0; i < num; i++)
        {
            String n = names.get(i);
            Table t = tables.get(i);

            Global.assume(t.hasField(field), "field '" + field + " 'not found in table '" + n + "'");

            for (String f : t.getFields())
            {
                if (!f.equals(field))
                {
                    schema.add(n + delimiter + f);
                }
            }

            for (Integer key : t.getKeys())
            {
                values.add(t.get(key, field));
            }
        }

        Table join = new Table(schema);
        int idx = 0;
        for (Object value : values)
        {
            Record row = new Record();
            row.with(field, String.valueOf(value));

            for (int i = 0; i < num; i++)
            {
                String n = names.get(i);
                Table t = tables.get(i);
                List<String[]> rs = t.getRecordArrayWith(field, String.valueOf(value));
                if (rs.size() == 0)
                {
                    for (String f : t.getFields())
                    {
                        row.with(n + delimiter + f, null);
                    }
                }
                else if (rs.size() == 1)
                {
                    Object[] r = rs.get(0);
                    for (int j = 0; j < t.getNumFields(); j++)
                    {
                        String f = t.getField(j);
                        Object v = r[j];
                        row.with(n + delimiter + f, String.valueOf(v));
                    }
                }
                else
                {
                    Logging.info("warning: skipping additional rows in table '" + n + "'");
                }
            }

            join.addRecord(idx++, row);
        }

        return join;
    }

    public static Table unionInner(List<Table> tables)
    {
        Global.assume(tables.size() != 0, "no tables were found");

        Set<String> common = null;
        for (Table table : tables)
        {
            if (common == null)
            {
                common = Sets.newHashSet(table.getFields());
            }
            else
            {
                common.retainAll(table.getFields());
            }
        }

        Global.assume(common.size() != 0, "no common fields were found");

        Schema schema = new Schema();
        Table proto = tables.get(0);
        for (String field : proto.getFields())
        {
            if (common.contains(field))
            {
                schema.add(field);
            }
        }

        Table union = new Table(schema);
        List<String> fields = union.getFields();
        int key = 0;

        for (Table table : tables)
        {
            for (Integer k : table.keys())
            {
                union.addRecord(key);
                for (String field : fields)
                {
                    Object value = table.get(k, field);
                    union.set(key, field, String.valueOf(value));
                }
                key++;
            }
        }
        return union;
    }

    public static Table unionOuter(List<Table> tables)
    {
        Schema schema = new Schema();
        for (Table table : tables)
        {
            for (String field : table.getFields())
            {
                if (!schema.hasField(field))
                {
                    schema.add(field);
                }
            }
        }

        Table union = new Table(schema);

        int key = 0;
        for (Table table : tables)
        {
            String[] fields = table.getFieldColumns();
            for (String[] row : table)
            {
                for (int i = 0; i < row.length; i++)
                {
                    union.set(key, fields[i], row[i]);
                }
                key++;
            }
        }

        return union;
    }

    public static Table constant(Table table, String query)
    {
        Table out = new Table();

        for (String pair : StringUtils.split(query, ','))
        {
            String[] split = StringUtils.split(pair, '=');

            Global.assume(split.length == 2, "constant must be formatted like field=value");

            String field = split[0];
            String value = split[1];

            out.withFields(table.getFields());
            out.withField(field);

            for (Integer key : table.keys())
            {
                Record record = table.getRecord(key).copy();
                record.with(field, value);
                out.addRecord(record);
            }
        }

        return out;
    }

    public static Table sort(Table table, String query)
    {
        List<Record> records = table.getRecords();

        final List<Triple<Boolean, Boolean, String>> conditions = Lists.newArrayList();
        for (String field : StringUtils.split(query, ','))
        {
            boolean numeric = false;
            if (field.contains("#"))
            {
                numeric = true;
                field = field.replace("#", "");
            }

            boolean reverse = false;
            if (field.contains("^"))
            {
                reverse = true;
                field = field.replace("^", "");
            }

            conditions.add(Triple.of(numeric, reverse, field));
        }

        Comparator<Record> comparator = (a, b) ->
        {
            int result = 0;
            for (Triple<Boolean, Boolean, String> condition : conditions)
            {
                boolean numeric = condition.a;
                boolean reverse = condition.b;
                String field = condition.c;

                String av = a.get(field);
                String bv = b.get(field);

                if (result == 0)
                {
                    if (numeric)
                    {
                        result = Double.compare(MathUtils.parse(av, Double.NaN), MathUtils.parse(bv, Double.NaN));
                    }
                    else
                    {
                        result = av.compareTo(bv);
                    }

                    if (reverse)
                    {
                        result *= -1;
                    }
                }

                if (result != 0)
                {
                    break;
                }
            }

            return result;
        };

        Collections.sort(records, comparator);

        return TableSource.create(records);
    }

    public static Table unique(Table table, String query)
    {
        List<Record> records = TableUtils.sort(table, query).getRecords();

        List<Record> all = Lists.newArrayList(records);
        records.clear();

        List<String> ufields = Lists.newArrayList();
        for (String field : StringUtils.split(query, ','))
        {
            ufields.add(field);
        }

        String last = null;
        for (Record record : all)
        {
            StringBuilder builder = new StringBuilder();
            for (String ufield : ufields)
            {
                if (record.containsKey(ufield))
                {
                    builder.append(record.get(ufield));
                }
            }

            String hash = builder.toString();

            if (last == null || !last.equals(hash))
            {
                records.add(record);
            }

            last = hash;
        }

        return TableSource.create(records);
    }

    public static Table remove(Table table, String query)
    {
        List<String> fields = table.getFields();

        for (String pattern : StringUtils.split(query, ','))
        {
            for (String field : table.getFields())
            {
                if (fields.contains(field) && field.matches(pattern))
                {
                    fields.remove(field);
                }
            }
        }

        return TableSource.create(table.getRecords(), fields);
    }

    public static Table dempty(Table table)
    {
        List<String> fields = table.getFields();
        fields.remove("");
        return TableSource.create(table.getRecords(), fields);
    }

    public static Table retain(Table table, String query)
    {
        List<String> fields = Lists.newArrayList();
        for (String pattern : query.split(","))
        {
            for (String field : table.getFields())
            {
               if (!fields.contains(field) && field.matches(pattern))
               {
                   fields.add(field);
               }
            }
        }

        return TableSource.create(table.getRecords(), fields);
    }

    public static Table rename(Table table, String query)
    {
        Map<String,String> map = Maps.newHashMap();
        for (String token : query.split(","))
        {
            String[] tokens = token.split("=");

            if (tokens.length == 2)
            {
                map.put(tokens[1], tokens[0]);
            }
        }

        List<Record> records = Lists.newArrayList();

        for (Integer key : table.getKeys())
        {
            Record record = table.getRecord(key);

            Record renamed = new Record();
            for (String field : record.keys())
            {
                if (map.containsKey(field))
                {
                    renamed.with(map.get(field), record.get(field));
                }
                else
                {
                    renamed.with(field, record.get(field));
                }
            }

            records.add(renamed);
        }

        return TableSource.create(records);
    }

    public static Table where(Table table, String query)
    {
        if (query == null || query == "")
        {
            query = StringExpression.TRUE;
        }

        List<String> fields = Lists.newArrayList();
        for (String field : table.getFields())
        {
            if (query.contains(field))
            {
                fields.add(field);
            }
        }

        String myexp = query;
        Map<String,String> clean = Maps.newHashMap();

        for (String field : fields)
        {
            String nfield = field;
            if (field.contains("."))
            {
                nfield = field.replace('.', '_');
            }

            if (field.contains("-"))
            {
                nfield = field.replace('-', '_');
            }

            clean.put(field, nfield);
            myexp = myexp.replaceAll(field, nfield);
        }

        StringExpression whereExpression = new StringExpression(myexp);
        List<Record> records = Lists.newArrayList();

        for (Integer key : table.getKeys())
        {
            Record record = table.getRecord(key);

            for (String field: fields)
            {
                whereExpression.with(clean.get(field), record.get(field));
            }

            if (StringExpression.test(whereExpression.eval()))
            {
                records.add(record);
            }
        }

        return TableSource.create(records);
    }

    public static Table cat(Table table, String cat)
    {
        Map<String,String> map = Maps.newHashMap();

        for (String token : StringUtils.split(cat, ','))
        {
            String[] tokens = StringUtils.split(token, '=');

            if (tokens.length == 2)
            {
                String name = tokens[0];
                String pattern = tokens[1].replace('%', '$');

                map.put(name, pattern);
            }
        }

        List<Record> records = Lists.newArrayList();

        for (Integer key : table.keys())
        {
            Record record = table.getRecord(key).copy();

            for (String name : map.keySet())
            {
                record.with(name, new StrSubstitutor(record.map()).replace(map.get(name)));
            }

            records.add(record);
        }

        return TableSource.create(records);
    }
}
