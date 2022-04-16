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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Record;
import qit.data.datasets.Table;

import java.util.Map;
import java.util.Set;

@ModuleDescription("Widen a table to expand a single field to many")
@ModuleAuthor("Ryan Cabeen")
public class TableWiden implements Module
{
    @ModuleInput
    @ModuleDescription("input table")
    public Table input;

    @ModuleParameter
    @ModuleDescription("the field name to expand")
    public String name = "name";

    @ModuleParameter
    @ModuleDescription("the field value to expand")
    public String value = "value";

    @ModuleParameter
    @ModuleDescription("pattern for joining names")
    public String pattern = "%s_%s";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the value used for missing entries")
    public String na = "NA";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("include fields")
    public String include = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("exclude fields")
    public String exclude = null;

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableWiden run()
    {
        Set<String> fields = Sets.newLinkedHashSet();
        for (Integer key : this.input.keys())
        {
            fields.add(this.input.get(key, this.name));
        }

        Set<String> values = Sets.newLinkedHashSet();
        for (String v : this.value.split(","))
        {
            values.add(v);
        }

        Set<String> include = null;
        if (this.include != null)
        {
            include = Sets.newLinkedHashSet();
            for (String v : this.include.split(","))
            {
                include.add(v);
            }
        }

        Set<String> exclude = Sets.newLinkedHashSet();
        if (this.exclude != null)
        {
            for (String v : this.exclude.split(","))
            {
                exclude.add(v);
            }
        }

        Table out = new Table();
        for (String f : this.input.getFields())
        {
            if (f.equals(this.name) || values.contains(f))
            {
                continue;
            }

            if (exclude.contains(f) || (include != null && !include.contains(f)))
            {
                continue;
            }

            out.withField(f);
        }

        for (String f : fields)
        {
            if (values.size() == 1)
            {
                out.withField(f);
            }
            else
            {
                for (String v : values)
                {
                    out.withField(String.format(this.pattern, f, v));
                }
            }
        }

        Map<Record,Integer> keys = Maps.newLinkedHashMap();
        for (Integer key : this.input.keys())
        {
            for (String val : values)
            {
                String rname = this.input.get(key, this.name);
                String rvalue = this.input.get(key, val);

                Record group = new Record();
                for (String f : this.input.getFields())
                {
                    if (f.equals(this.name) || values.contains(f))
                    {
                        continue;
                    }

                    if (exclude.contains(f) || (include != null && !include.contains(f)))
                    {
                        continue;
                    }

                    group.with(f, this.input.get(key, f));
                }

                if (!keys.containsKey(group))
                {
                    int rkey = keys.size();
                    out.addRecord(rkey, group);
                    keys.put(group, rkey);
                }

                int rkey = keys.get(group);
                String rfield = values.size() == 1 ? rname : String.format(this.pattern, rname, val);

                if (rvalue == null || rvalue.strip().toLowerCase().equals("null"))
                {
                    rvalue = this.na;
                }

                out.set(rkey, rfield, rvalue);
            }
        }

        for (Integer key : out.keys())
        {
            for (String field : out.getFields())
            {
                String value = out.get(key, field);
                if (value == null || value.strip().toLowerCase().equals("null"))
                {
                    out.set(key, field, this.na);
                }
            }
        }

        this.output = out;

        return this;
    }
}
