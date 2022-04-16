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
import qit.base.Global;
import qit.base.utils.JsonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** the fields of a table */
public class Schema implements Iterable<String>
{
    private List<String> fields = Lists.newArrayList();
    private List<String> defaults = Lists.newArrayList();
    private Map<String, Integer> index = Maps.newLinkedHashMap();
    private transient boolean locked = false;

    public Schema()
    {
        this(0);
    }

    public Schema(Iterable<String> fields)
    {
        super();
        for (String field : fields)
        {
            this.add(field);
        }
    }

    public Schema(int num)
    {
        Global.assume(num >= 0, "invalid number of fields");
    }

    public boolean locked()
    {
        return this.locked;
    }

    public void lock()
    {
        this.locked = true;
    }

    public Schema copy()
    {
        Schema out = new Schema(this.size());
        out.fields.addAll(this.fields);
        out.defaults.addAll(this.defaults);
        out.index.putAll(this.index);

        return out;
    }

    public List<String> getFields()
    {
        return new ArrayList<>(this.fields);
    }

    public void add(String field)
    {
        this.add(field, null);
    }

    public void add(String field, String def)
    {
        Global.assume(!this.hasField(field), "field already exists: " + field);

        this.fields.add(field);
        this.defaults.add(def);
        this.index.put(field, this.fields.size() - 1);
    }

    public String getField(int idx)
    {
        return this.fields.get(idx);
    }

    public String getDefault(String field)
    {
        return this.defaults.get(this.index.get(field));
    }

    public String getDefault(int idx)
    {
        return this.defaults.get(idx);
    }

    public int getIndex(String field)
    {
        Global.assume(this.hasField(field), "field not found: " + field);

        return this.index.get(field);
    }

    public boolean hasField(String field)
    {
        return this.index.containsKey(field);
    }

    public int size()
    {
        return this.fields.size();
    }

    public Object clone()
    {
        return this.copy();
    }

    public Iterator<String> iterator()
    {
        return this.fields.iterator();
    }

    public String toString()
    {
        return JsonUtils.encode(this);
    }
}
