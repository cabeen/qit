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


package qit.base.cli;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import qit.base.Global;
import qit.base.Logging;
import qit.base.utils.ComboUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;

/**
 * a collection of utilities for processing command line arguments
 */
public class CliUtils
{
    public static List<Integer> parseWhichWithLookup(String which, Table lookup)
    {
        return parseWhichWithLookup(which, lookup, "name", "index");
    }

    public static List<Integer> parseWhichWithLookup(String which, Table lookup, String name, String index)
    {
        List<Integer> out = Lists.newArrayList();
        if (lookup == null)
        {
            for (Integer s : parseWhich(which))
            {
                out.add(s);
            }
        }
        else
        {
            Map<String, Integer> lut = TableUtils.createStringIntegerLookup(lookup, name, index);

            for (String token : which.split("[,\\s]+"))
            {
                if (lut.containsKey(token))
                {
                    out.add(lut.get(token));
                }
            }
        }

        return out;
    }

    public static List<Integer> parseIndex(int num, String which, String exclude)
    {
        List<Integer> out = Lists.newArrayList();
        if (which != null)
        {
            for (Integer idx : CliUtils.parseWhich(which))
            {
                out.add(idx);
            }
        }
        else if (exclude != null)
        {
            Set<Integer> skip = Sets.newHashSet();
            for (Integer idx : CliUtils.parseWhich(exclude))
            {
                skip.add(idx);
            }

            for (int i = 0; i < num; i++)
            {
                if (!skip.contains(i))
                {
                    out.add(i);
                }
            }
        }
        else
        {
            for (int i = 0; i < num; i++)
            {
                out.add(i);
            }
        }

        return out;
    }


    public static List<Integer> parseWhich(String which, int size)
    {
        List<Integer> initial = parseWhich(which);
        List<Integer> out = Lists.newArrayList();

        for (Integer i : initial)
        {
            if (i < 0)
            {
                out.add(size + i);
            }
            else if (i >= size)
            {
                out.add(i % (size - 1));
            }
            else
            {
                out.add(i);
            }
        }

        return out;
    }

    public static List<Integer> parseWhich(String which)
    {
        List<Integer> out = Lists.newArrayList();

        if (which != null && which.length() > 0)
        {
            List<String> tokens = Lists.newArrayList();

            if (PathUtils.exists(which))
            {
                String file = null;
                try
                {
                    file = FileUtils.readFileToString(new File(which));
                }
                catch (IOException e)
                {
                    throw new RuntimeException("invalid which argument: " + which);
                }

                if (file.contains(","))
                {
                    for (String token : file.split("[,\\s]+"))
                    {
                        tokens.add(token);
                    }
                }
                else
                {
                    for (String token : StringUtils.split(file))
                    {
                        tokens.add(token);
                    }
                }
            }
            else
            {
                for (String token : which.split("[,\\s]+"))
                {
                    tokens.add(token);
                }
            }

            for (String token : tokens)
            {
                if (token.contains(":"))
                {
                    String[] subtokens = token.split(":");
                    int start = Integer.valueOf(subtokens[0]);
                    int end = Integer.valueOf(subtokens[1]);

                    for (int idx = start; idx <= end; idx++)
                    {
                        out.add(idx);
                    }
                }
                else if (token.length() > 0)
                {
                    try
                    {
                        out.add(Integer.valueOf(token));
                    }
                    catch (RuntimeException e)
                    {
                        // skip values that cannot be parsed
                    }
                }
            }
        }

        return out;
    }

    public static List<Integer> parseIndexList(String value, int max)
    {
        // null value means all index values

        List<Integer> out = Lists.newArrayList();

        for (int d = 0; d < max; d++)
        {
            out.add(d);
        }

        if (value != null)
        {
            if (value.startsWith("-"))
            {
                for (String c : value.split("-")[1].split(","))
                {
                    out.remove(Integer.valueOf(c));
                }
            }
            else
            {
                out.clear();
                for (String c : value.split(","))
                {
                    out.add(Integer.valueOf(c));
                }
            }
        }

        return out;
    }

    public static void validate(String fn)
    {
        Global.assume(fn != null, "undefined filename");
        Global.assume(new File(fn).exists(), "file not found: " + fn);
    }

    public static Map<Integer, String> lut(String rois_fn, String lut_fn) throws IOException
    {
        List<Integer> idx = MaskUtils.listNonzero(Mask.read(rois_fn));

        Map<Integer, String> out = Maps.newHashMap();

        // try to detect a related lookup file
        if (lut_fn == null)
        {
            String clean_fn = rois_fn.endsWith("gz") ? FilenameUtils.removeExtension(rois_fn) : rois_fn;
            String test_fn = FilenameUtils.removeExtension(clean_fn) + ".csv";

            if (new File(test_fn).exists())
            {
                lut_fn = test_fn;
            }
        }

        if (lut_fn == null)
        {
            for (Integer i : idx)
            {
                out.put(i, "region" + i);
            }
        }
        else if (lut_fn.contains(","))
        {
            String[] names = StringUtils.split(lut_fn, ",");
            if (names.length != idx.size())
            {
                throw new RuntimeException("lookup size mismatch");
            }

            for (int i = 0; i < idx.size(); i++)
            {
                out.put(idx.get(i), names[i]);
            }
        }
        else if (lut_fn.endsWith("txt"))
        {
            String file = FileUtils.readFileToString(new File(lut_fn));
            String[] names = StringUtils.split(file);

            if (names.length != idx.size())
            {
                throw new RuntimeException("lookup size mismatch");
            }

            for (int i = 0; i < idx.size(); i++)
            {
                out.put(idx.get(i), names[i]);
            }
        }
        else if (lut_fn.endsWith("csv"))
        {
            Table table = Table.read(lut_fn);
            Record lut = TableUtils.createLookup(table, "index", "name");

            for (String key : lut.keySet())
            {
                Integer kidx = Integer.valueOf(key);
                out.put(kidx, lut.get(key));
            }
        }
        else
        {
            throw new RuntimeException("invalid lookup: " + lut_fn);
        }

        return out;
    }

    public static List<String> names(List<String> args) throws IOException
    {
        List<String> names = Lists.newArrayList();

        int numIdArgs = args.size();
        if (numIdArgs == 0)
        {
            Logging.error("invalid identifiers");
        }
        else if (args.size() == 1)
        {
            String arg = args.get(0);
            names.addAll(CliUtils.names(arg, names));
        }
        else
        {
            names.addAll(args);
        }


        return names;
    }

    public static List<String> namesSafe(String arg)
    {
        try
        {
            return names(arg, Lists.newArrayList());
        }
        catch (IOException e)
        {
            Logging.info("warning failed to parse names from: " + arg);
            return Lists.newArrayList();
        }
    }

    public static List<String> names(String arg) throws IOException
    {
        return names(arg, Lists.newArrayList());
    }

    public static List<String> names(String arg, List<String> defaults) throws IOException
    {
        if (arg == null)
        {
            return defaults;
        }
        else if (arg.contains(","))
        {
            return Lists.newArrayList(StringUtils.split(arg, ","));
        }
        else if (arg.endsWith(".csv"))
        {
            Table table = Table.read(arg);
            String field = "name";
            if (!table.hasField(field))
            {
                field = table.getFields().get(0);
            }

            Map<Integer, String> column = table.getFieldMap(field);
            String[] out = new String[column.size()];
            int idx = 0;
            for (Integer key : column.keySet())
            {
                out[idx++] = column.get(key).toString();
            }

            return Lists.newArrayList(out);

        }
        else if (new File(arg).exists())
        {
            String file = FileUtils.readFileToString(new File(arg));
            return Lists.newArrayList(StringUtils.split(file));
        }
        else if (arg.contains(":"))
        {
            String[] tokens = StringUtils.split(arg, ":");

            if (arg.contains("."))
            {
                // floating point
                double start = Double.parseDouble(tokens[0]);
                double delta = tokens.length == 3 ? Double.parseDouble(tokens[1]) : 1.0;
                double end = Double.parseDouble(tokens.length == 3 ? tokens[2] : tokens[1]);

                int num = (int) Math.floor((end - start) / delta);
                String[] out = new String[num];
                for (int i = 0; i < num; i++)
                {
                    out[i] = String.valueOf(start + i * delta);
                }
                return Lists.newArrayList(out);
            }
            else
            {
                // integer
                int start = Integer.parseInt(tokens[0]);
                int delta = tokens.length == 3 ? Integer.parseInt(tokens[1]) : 1;
                int end = Integer.parseInt(tokens.length == 3 ? tokens[2] : tokens[1]);

                int num = (int) Math.floor((end - start) / delta);
                String[] out = new String[num];
                for (int i = 0; i < num; i++)
                {
                    out[i] = String.valueOf(start + i * delta);
                }
                return Lists.newArrayList(out);
            }
        }
        else
        {
            List out = Lists.newArrayList();
            out.add(arg);
            return out;
        }
    }

    public static List<String> ids(List<String> fns)
    {
        List<String> out = Lists.newArrayList();
        for (String fn : fns)
        {
            String basename = new File(fn).getName();
            String[] tokens = StringUtils.split(basename, ".");
            StringBuilder builder = new StringBuilder();
            builder.append(tokens[0]);
            for (int j = 1; j < tokens.length - 1; j++)
            {
                builder.append(".");
                builder.append(tokens[j]);
            }
            out.add(builder.toString());
        }

        return out;
    }

    public static String args(String input, String def)
    {
        for (int i = 0, j = 0, n = 0; i < input.length(); i++)
        {
            char c = input.charAt(i);
            if (c == '{')
            {
                if (++n == 1)
                {
                    j = i;
                }
            }
            else if (c == '}' && --n == 0)
            {
                return input.substring(j + 1, i);
            }
        }

        return def;
    }

    public static List<List<String>> batches(List<String> args, String enable, String var, String table, String product) throws IOException
    {
        List<List<String>> batches = Lists.newArrayList();
        if (args.remove(enable))
        {
            Logging.info("batch mode detected");

            for (int i = 0; i < args.size(); i++)
            {
                args.set(i, args.get(i).replace('%', '$'));
            }

            Global.assume(args.contains(var) ^ args.contains(table), "invalid batch flags.  var and table are mutually exclusive arguments.");

            Map<String, List<String>> vars = Maps.newHashMap();
            if (args.contains(var))
            {
                while (args.contains(var))
                {
                    String opt = eatKeyValuePair(args, var);
                    String[] tokens = StringUtils.split(opt, "=");
                    Global.assume(tokens.length == 2, "invalid batch variable option: " + opt);

                    String name = tokens[0];
                    String value = tokens[1];
                    List<String> parsed = CliUtils.names(value, Lists.newArrayList(value));

                    vars.put(name, parsed);
                }
            }
            else if (args.contains(table))
            {
                String opt = eatKeyValuePair(args, table);
                Table tabled = Table.read(opt);
                for (String field : tabled.getFields())
                {
                    vars.put(field, Lists.<String>newArrayList());
                }

                for (Integer key : tabled.getKeys())
                {
                    Record record = tabled.getRecord(key);
                    for (String field : record.keySet())
                    {
                        vars.get(field).add(record.get(field));
                    }
                }
            }

            for (String name : vars.keySet())
            {
                Logging.info(String.format("loaded batch variable %s with %d values", name, vars.get(name).size()));
            }

            Global.assume(vars.size() != 0, "no batch variables found");

            Logging.info(String.format("loaded a total of %d batch variables", vars.size()));

            boolean prod = args.remove(product);
            for (Map<String, String> group : (prod ? ComboUtils.product(vars) : ComboUtils.tuples(vars)))
            {
                List<String> batch = Lists.newArrayList();
                for (String arg : args)
                {
                    batch.add(CliUtils.expand(arg, group));
                }
                batches.add(batch);
            }
        }
        else
        {
            batches.add(args);
        }

        Logging.info(String.format("found %d batches", batches.size()));

        return batches;
    }

    public static String eatKeyValuePair(List<String> args, String flag)
    {
        int idx = args.indexOf(flag);

        Global.assume(idx != args.size() - 1, "invalid batch variable option");

        args.remove(idx);
        String value = args.remove(idx);

        return value;
    }

    public static String expand(String pattern, Map<String, String> env)
    {
        return new StrSubstitutor(env).replace(pattern);
    }

    public static Map<String, List<String>> variables(List<String> args) throws IOException
    {
        Map<String, List<String>> vars = Maps.newLinkedHashMap();
        for (String arg : args)
        {
            String[] tokens = StringUtils.split(arg, "=");
            Global.assume(tokens.length == 2, "invalid batch variable option: " + arg);

            String name = tokens[0];
            String value = tokens[1];
            List<String> parsed = CliUtils.names(value, Lists.newArrayList(value));

            vars.put(name, parsed);
        }

        return vars;
    }
}