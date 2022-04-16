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

package qit.data.datasets;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.cli.CliUtils;
import qit.base.utils.JsonUtils;
import qit.data.formats.table.AnnotTableCoder;
import qit.data.formats.table.CsvTableCoder;
import qit.data.formats.table.TxtTableCoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** a table that keeps track of order of inserts */
public class Table implements Iterable<String[]>, Dataset
{
    public final static String NAME = "name";
    public final static String INDEX = "index";
    public final static String RED = "red";
    public final static String GREEN = "green";
    public final static String BLUE = "blue";
    public final static String ALPHA = "alpha";
    public final static String FROM = "from";
    public final static String TO = "to";

    private Schema schema;
    private Map<Integer, String[]> records = Maps.newLinkedHashMap();
    private Integer maxkey = 0;

    // use a linked map to maintain order

    public Table()
    {
        this(new Schema());
    }

    public Table(Schema schema)
    {
        this.schema = schema.copy();
    }

    public Table(Iterable<String> fields)
    {
        this.schema = new Schema(fields);
    }

    public Table copy()
    {
        Table out = new Table(this.schema);
        for (Integer key : this.records.keySet())
        {
            String[] row = this.records.get(key);
            String[] crow = new String[row.length];
            System.arraycopy(row, 0, crow, 0, row.length);
            out.addRecord(key, crow);
        }

        return out;
    }

    public Table proto()
    {
        return new Table(this.schema.copy());
    }

    public boolean hasRow(Integer key)
    {
        return this.records.containsKey(key);
    }

    public Table withField(String field, String def)
    {
        this.schema.add(field, def);
        this.expand();

        return this;
    }

    public Table withFields(List<String> fields)
    {
        for (String field : fields)
        {
            if (!this.schema.hasField(field))
            {
                this.schema.add(field);
            }
        }
        this.expand();

        return this;
    }

    public Table withField(String field)
    {
        if (!this.schema.hasField(field))
        {
            this.schema.add(field);
            this.expand();
        }

        return this;
    }

    public Table addField(String field)
    {
        return this.withField(field);
    }

    public Table addFields(Iterable<String> fields)
    {
        for (String field : fields)
        {
            this.withField(field);
        }

        return this;
    }

    public Table withFields(Collection<String> fields)
    {
        for (String field : fields)
        {
            this.schema.add(field);
        }
        this.expand();

        return this;
    }

    private void expand()
    {
        for (Integer key : this.records.keySet())
        {
            String[] prev = this.records.get(key);
            String[] next = new String[this.schema.size()];
            System.arraycopy(prev, 0, next, 0, prev.length);
            for (int i = prev.length; i < this.schema.size(); i++)
            {
                next[i] = this.schema.getDefault(i);
            }
            this.addRecord(key, next);
        }
    }

    public Schema getSchema()
    {
        Schema out = this.schema.copy();
        out.lock();
        return out;
    }

    public int getNumRecords()
    {
        return this.records.size();
    }

    public int getNumFields()
    {
        return this.schema.size();
    }

    public int getIndex(String field)
    {
        return this.schema.getIndex(field);
    }

    public String getFieldName(int i)
    {
        return this.schema.getField(i);
    }

    public List<String> getFields()
    {
        return this.schema.getFields();
    }

    public String[] getFieldColumns()
    {
        String[] out = new String[this.getNumFields()];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = this.getField(i);
        }

        return out;
    }

    public int getFieldIndex(String field)
    {
        return this.schema.getIndex(field);
    }

    public String getField(int column)
    {
        return this.schema.getField(column);
    }

    public String getFieldDefault(String field)
    {
        return this.schema.getDefault(field);
    }

    public boolean hasField(String name)
    {
        return this.schema.hasField(name);
    }

    public boolean hasDefault(String name)
    {
        return this.schema.hasField(name);
    }

    public List<Integer> getKeys()
    {
        return Lists.newArrayList(this.records.keySet());
    }

    public String[] getRecordArray(Integer key)
    {
        return this.records.get(key);
    }

    public List<Record> getRecords()
    {
        List<Record> out = Lists.newArrayList();

        for (Integer key : this.keys())
        {
            out.add(this.getRecord(key));
        }

        return out;
    }

    public List<Record> select(List<String> fields)
    {
        List<Record> out = Lists.newArrayList();

        for (Integer key : this.keys())
        {
            out.add(this.getRecord(key).select(fields));
        }

        return out;
    }

    public List<Record> where(Record query)
    {
        List<Record> out = Lists.newArrayList();

        outer: for (Integer key : this.keys())
        {
            Record record = this.getRecord(key);

            for (String qkey : query.keys())
            {
                if (!record.containsKey(qkey) || !query.get(qkey).equals(record.get(qkey)))
                {
                    continue outer;
                }
            }

            out.add(record.copy());
        }

        return out;

    }

    public Record getRecord(Integer key)
    {
        Record out = new Record();

        for (String field : this.getFields())
        {
            out.with(field, this.get(key, field));
        }

        return out;
    }

    public List<String[]> getRecordArrayWith(String field, String value)
    {
        List<String[]> out = Lists.newArrayList();
        if (value == null)
        {
            return out;
        }

        Global.assume(this.hasField(field), "field " + field + " not found");

        int idx = this.getIndex(field);
        for (String[] row : this)
        {
            if (value.equals(row[idx]))
            {
                out.add(row);
            }
        }

        return out;
    }

    public Map<Integer, String> getFieldMap(String field)
    {
        Map<Integer, String> column = Maps.newHashMap();
        for (Integer key : this.keys())
        {
            column.put(key, this.get(key, field));
        }
        return column;
    }

    public List<String> getFieldValues(String field)
    {
        List<String> column = Lists.newArrayList();
        for (Integer key : this.keys())
        {
            column.add(this.get(key, field));
        }
        return column;
    }

    public void remove(Integer key)
    {
        this.records.remove(key);
    }

    public String[] getDefaultRow()
    {
        String[] out = new String[this.schema.size()];
        for (int i = 0; i < this.schema.size(); i++)
        {
            out[i] = this.schema.getDefault(i);
        }

        return out;
    }

    public String get(Integer key, int idx)
    {
        return this.records.get(key)[idx];
    }

    public String get(Integer key, String field)
    {
        return this.records.get(key)[this.schema.getIndex(field)];
    }

    public Iterator<String[]> iterator()
    {
        return this.records.values().iterator();
    }

    public Set<Integer> keys()
    {
        return this.records.keySet();
    }

    public void set(Integer key, String field, String value)
    {
        if (!this.hasField(field))
        {
            this.withField(field);
        }

        if (!this.records.containsKey(key))
        {
            this.addRecord(key, new String[this.schema.size()]);
        }

        int idx = this.schema.getIndex(field);
        String[] row = this.records.get(key);
        row[idx] = value;
    }

    public void set(Integer key, Record row)
    {
        if (!this.records.containsKey(key))
        {
            this.addRecord(key, new String[this.schema.size()]);
        }

        String[] nrow = this.records.get(key);

        for (String field : row.keySet())
        {
            if (this.hasField(field))
            {
                nrow[this.schema.getIndex(field)] = row.get(field);
            }
        }
    }

    public void set(String field, Map<Integer, String> values)
    {
        if (!this.hasField(field))
        {
            this.withField(field);
        }

        for (Integer key : values.keySet())
        {
            this.set(key, field, values.get(key));
        }
    }

    public void addFieldValues(String field, Map<Integer, String> column)
    {
        this.withField(field);
        for (Integer key : column.keySet())
        {
            this.set(key, field, column.get(key));
        }
    }

    public Table addRecord(Integer key, Record record)
    {
        String[] row = new String[this.schema.size()];
        for (String name : this.schema)
        {
            if (record.containsKey(name))
            {
                row[this.schema.getIndex(name)] = record.get(name);
            }
            else
            {
                row[this.schema.getIndex(name)] = this.schema.getDefault(name);
            }
        }
        this.records.put(key, row);
        this.maxkey = Math.max(key, this.maxkey);

        return this;
    }

    public Table addRecord(Integer key)
    {
        this.addRecord(key, this.getDefaultRow());

        return this;
    }

    public Table addRecordSafe(Integer key, String[] row)
    {
        String[] nrow = new String[this.getNumFields()];
        for (int i = 0; i < Math.min(row.length, this.getNumFields()); i++)
        {
            nrow[i] = row[i];
        }

        this.records.put(key, nrow);
        this.maxkey = Math.max(key, this.maxkey);

        return this;
    }

    public Table addRecord(Integer key, String[] row)
    {
        int n = row.length;
        Global.assume(n == this.schema.size(), String.format("row length (%d) does not match schema (%d)", n, this.schema.size()));

        String[] nrow = new String[n];
        System.arraycopy(row, 0, nrow, 0, n);

        this.records.put(key, nrow);
        this.maxkey = Math.max(key, this.maxkey);

        return this;
    }

    public Table addRecord(Record record)
    {
        return this.addRecord(this.maxkey + 1, record);
    }

    public Table addRecords(Iterable<Record> records)
    {
        for (Record record : records)
        {
            this.addRecord(record);
        }

        return this;
    }

    public Table addRecord(String[] record)
    {
        return this.addRecord(this.maxkey + 1, record);
    }

    public static Table read(String fn) throws IOException
    {
        CliUtils.validate(fn);
        InputStream is = new FileInputStream(fn);

        if (fn.endsWith("annot"))
        {
            return AnnotTableCoder.read(is);
        }
        else if (fn.endsWith("txt"))
        {
            return TxtTableCoder.read(is);
        }
        else
        {
            return CsvTableCoder.read(is);
        }
    }

    public void write(String fn) throws IOException
    {
        if (fn.endsWith("json"))
        {
            FileUtils.write(new File(fn), JsonUtils.encode(this), false);
        }
        else
        {
            CsvTableCoder.write(this, new FileOutputStream(fn));
        }
    }

    public List<String> getExtensions()
    {
        List<String> out = Lists.newArrayList();
        out.add("csv");
        out.add("txt");
        out.add("json");
        return out;
    }

    public String getDefaultExtension()
    {
        return "csv";
    }
}