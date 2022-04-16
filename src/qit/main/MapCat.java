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

package qit.main;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.text.StrSubstitutor;
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.utils.ComboUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.utils.TableUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapCat implements CliMain
{
    public static void main(String[] args)
    {
        new MapCat().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "concatenate map files into a table";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);

            cli.withOption(new CliOption().asParameter().withName("pattern").withArg("<Pattern>").withDoc("specify an input pattern (substitution like ${name})"));
            cli.withOption(new CliOption().asParameter().withName("vars").withArg("name=value(s)").withDoc("specify a list of identifiers").withNoMax());
            cli.withOption(new CliOption().asParameter().asOptional().withName("tuples").withDoc("expand multiple vars to tuples (default: cartesian product)"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("rows").withDoc("expand each input file to a row"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("input-name").withArg("<String>").withDoc("specify the input map name field").withDefault("name"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("input-value").withArg("<String>").withDoc("specify the input map value field").withDefault("value"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("output-name").withArg("<String>").withDoc("specify the output name field").withDefault("name"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("output-value").withArg("<String>").withDoc("specify the output value field").withDefault("value"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("na").withArg("<String>").withDoc("specify value for missing entries").withDefault("NA"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("skip").withDoc("skip missing files"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("include").withArg("name(s)").withDoc("specify which names to include").withNoMax());
            cli.withOption(new CliOption().asParameter().asOptional().withName("exclude").withArg("name(s)").withDoc("specify which names to exclude").withNoMax());
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<File>").withDoc("specify the output"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            String pattern = entries.keyed.get("pattern").get(0);
            String output = entries.keyed.get("output").get(0);
            Map<String, List<String>> vars = CliUtils.variables(entries.keyed.get("vars"));
            List<String> include = null;
            List<String> exclude = null;

            boolean product = !entries.keyed.containsKey("tuples");
            boolean narrow = !entries.keyed.containsKey("rows");
            String nameInputField = entries.keyed.get("input-name").get(0);
            String valueInputField = entries.keyed.get("input-value").get(0);
            String nameOutputField = entries.keyed.get("output-name").get(0);
            String valueOutputField = entries.keyed.get("output-value").get(0);
            String naValue = entries.keyed.get("na").get(0);
            boolean skip = entries.keyed.containsKey("skip");

            pattern = pattern.replace("%", "$");

            if (entries.pos.size() != 0)
            {
                Logging.error("invalid positional arguments");
            }

            if (entries.keyed.containsKey("include"))
            {
                include = CliUtils.names(entries.keyed.get("include"));
            }

            if (entries.keyed.containsKey("exclude"))
            {
                exclude = CliUtils.names(entries.keyed.get("exclude"));
            }

            List<Map<String, String>> recmaps = Lists.newArrayList();
            for (Map<String, String> group : ComboUtils.groups(vars, product))
            {
                for (String gkey : group.keySet())
                {
                    Global.assume(!gkey.equals(nameOutputField), "field collision on " + gkey);
                    Global.assume(!gkey.equals(valueOutputField), "field collision on " + gkey);
                }

                String fn = new StrSubstitutor(group).replace(pattern);

                if (!PathUtils.exists(fn))
                {
                    if (skip)
                    {
                        Logging.info("ignoring: " + fn);
                    }
                    else
                    {
                        Logging.error("file not found: " + fn);
                    }
                    continue;
                }

                try
                {
                    Logging.info("reading: " + fn);
                    Record map = TableUtils.createLookup(Table.read(fn), nameInputField, valueInputField);

                    if (narrow)
                    {
                        // add a row for each key-value pair
                        for (String key : map.keySet())
                        {
                            if (include != null && !include.contains(key))
                            {
                                continue;
                            }

                            if (exclude != null && exclude.contains(key))
                            {
                                continue;
                            }

                            Map<String, String> rowmap = Maps.newLinkedHashMap();
                            for (String gkey : group.keySet())
                            {
                                rowmap.put(gkey, group.get(gkey));
                            }
                            rowmap.put(nameOutputField, key);
                            rowmap.put(valueOutputField, map.get(key));

                            recmaps.add(rowmap);
                        }
                    }
                    else
                    {
                        // add a row for all key-value pairs
                        Map<String, String> rowmap = Maps.newLinkedHashMap();
                        for (String key : group.keySet())
                        {
                            rowmap.put(key, group.get(key));
                        }
                        for (String key : map.keySet())
                        {
                            if (include != null && !include.contains(key))
                            {
                                continue;
                            }

                            if (exclude != null && exclude.contains(key))
                            {
                                continue;
                            }

                            rowmap.put(key, map.get(key));
                        }
                        recmaps.add(rowmap);
                    }
                }
                catch (RuntimeException e)
                {
                    if (skip)
                    {
                        Logging.info("skipping due to error: " + fn);
                        continue;
                    }
                    else
                    {
                        Logging.error(e.getMessage());
                    }
                }
            }

            Set<String> fields = Sets.newLinkedHashSet();
            for (Map<String, String> rowmap : recmaps)
            {
                fields.addAll(rowmap.keySet());
            }

            Logging.info("creating table");
            Table table = new Table().withFields(fields);

            for (Map<String, String> rowmap : recmaps)
            {
                Record record = new Record();
                for (String field : fields)
                {
                    if (rowmap.containsKey(field))
                    {
                        record.with(field, rowmap.get(field));
                    }
                    else
                    {
                        record.with(field, naValue);
                    }
                }
                table.addRecord(record);
            }

            if (table.getNumRecords() > 0)
            {
                Logging.info("writing: " + output);
                table.write(output);
            }
            else
            {
                Logging.info("no table written");
            }

            Logging.info("finished");
        }

        catch (Exception e)

        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}
