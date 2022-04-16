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
import qit.base.utils.JsonUtils;
import qit.data.source.TableSource;
import qit.data.utils.TableUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** an entry in a table */
public class Record implements Dataset, Iterable<String>
{
    public static final List<String> EXTS = Lists.newArrayList(new String[]{"txt", "csv"});

    private Map<String, String> data = Maps.newLinkedHashMap();

    public Record()
    {
        super();
    }

    public Record with(Record map)
    {
        for (String name : map.keySet())
        {
            this.with(name, map.get(name));
        }

        return this;
    }

    public Record with(String key, String value)
    {
        Global.assume(key != null, "null keys are not allowed");

        this.data.put(key, value);

        return this;
    }

    public Record with(String key, Object value)
    {
        this.with(key, String.valueOf(value));

        return this;
    }

    public Record select(Set<String> keys)
    {
        Record out = new Record();
        for (String key : this.keySet())
        {
            if (keys.contains(key))
            {
                out.with(key, this.get(key));
            }
        }

        return out;
    }

    public Record select(List<String> keys)
    {
        Record out = new Record();
        for (String key : this.keySet())
        {
            if (keys.contains(key))
            {
                out.with(key, this.get(key));
            }
        }

        return out;
    }

    public Record copy()
    {
        Record out = new Record();
        for (String key : this.keySet())
        {
            out.with(key, this.get(key));
        }

        return out;
    }

    public List<String> keys()
    {
        return Lists.newArrayList(this.data.keySet());
    }

    public Set<String> keySet()
    {
        return this.data.keySet();
    }

    public String get(String key)
    {
        return this.data.get(key);
    }

    public String remove(String key)
    {
        return this.data.remove(key);
    }

    public boolean containsKey(String key)
    {
        return this.data.containsKey(key);
    }

    public int size()
    {
        return this.data.size();
    }

    public void clear()
    {
        this.data.clear();
    }

    public int hashCode()
    {
        int out = 0;
        for (String key : this)
        {
            out += key.hashCode() * this.get(key).hashCode();
        }

        return out;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Record))
        {
            return false;
        }
        else if (obj == this)
        {
            return true;
        }
        else
        {
            Record a = this;
            Record b = (Record) obj;

            if (a.size() != b.size())
            {
                return false;
            }

            for (String key : a)
            {
                if (!b.containsKey(key))
                {
                    return false;
                }

                if (!a.get(key).equals(b.get(key)))
                {
                    return false;
                }
            }

            return true;
        }
    }

    public String toStringFlat()
    {
        String out = "";
        for (String key : this.keys())
        {
            if (out.length() > 0)
            {
               out += " ";
            }

            out += String.format("%s=%s", key, this.get(key));
        }

        return out;
    }

    public String toString()
    {
        return JsonUtils.encode(this);
    }

    public static Record read(String fn) throws IOException
    {
        return TableUtils.createLookup(Table.read(fn), "name", "value");
    }

    public void write(String fn) throws IOException
    {
        if (fn.endsWith("csv"))
        {
            TableSource.createNarrow(this).write(fn);
        }
        else
        {
            FileUtils.write(new File(fn), JsonUtils.encode(this), false);
        }
    }

    public List<String> getExtensions()
    {
        return EXTS;
    }

    @Override
    public Iterator<String> iterator()
    {
        return this.data.keySet().iterator();
    }

    public Map<String,String> map()
    {
        return this.data;
    }
}
