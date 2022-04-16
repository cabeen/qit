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
import com.google.common.collect.Sets;
import java.nio.CharBuffer;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import qit.base.Global;
import qit.base.Logging;

/**
 * a class for easily making command line interfaces
 */
public class CliSpecification
{
    public static final String HELP = "help";
    public static final String MARKDOWN = "markdown";
    public static final String VERBOSE = "verbose";
    public static final String DEBUG = "debug";

    public static final String INDENT = "  ";
    public static final int INDENTWIDTH = INDENT.length();
    public static final int WIDTHMAX = 79;

    private String name = "program";
    private String doc = null;
    private String author = null;
    private String citation = null;
    private CliOption positional = new CliOption();
    private List<CliOption> options = Lists.newArrayList();

    public CliSpecification withName(String v)
    {
        this.name = v;
        return this;
    }

    public CliSpecification withDoc(String v)
    {
        this.doc = v;
        return this;
    }

    public CliSpecification withAuthor(String v)
    {
        this.author = v;
        return this;
    }

    public CliSpecification withCitation(String v)
    {
        this.citation = v;
        return this;
    }

    public CliSpecification withPositional(CliOption v)
    {
        this.positional = v;
        return this;
    }

    public CliSpecification withOption(CliOption v)
    {
        this.options.add(v);
        return this;
    }

    public void printMarkdown()
    {
        StringBuilder out = new StringBuilder();
        {
            out.append(String.format("## %s \n\n", this.name));
        }

        if (this.doc != null)
        {
            append("### Description", this.doc, out);
        }

        if (this.positional.getMin() > 0)
        {
            out.append("### Positional Arguments: \n\n  ");
            append(this.positional, out);
            out.append("\n");
        }


        BiConsumer<List<CliOption>, String> appendCliOptions = (input, header) ->
        {
            if (input.size() > 0)
            {
                out.append("### " + header + ":\n\n");
                for (CliOption entry : input)
                {
                    out.append("```lang-none\n\n");
                    for (String line : format(entry, WIDTHMAX - INDENTWIDTH))
                    {
                        out.append(INDENT);
                        out.append(line);
                    }
                    out.append("\n```\n\n");
                }
            }
        };

        appendCliOptions.accept(this.getInput(false), "Required Input Arguments");
        appendCliOptions.accept(this.getInput(true), "Optional Input Arguments");
        appendCliOptions.accept(this.getParameter(false), "Required Parameter Arguments");
        appendCliOptions.accept(this.getParameter(true), "Optional Parameter Arguments");
        appendCliOptions.accept(this.getParameterAdvanced(), "Advanced Parameter Arguments");
        appendCliOptions.accept(this.getOutput(false), "Required Output Arguments");
        appendCliOptions.accept(this.getOutput(true), "Optional Output Arguments");

        if (this.author != null)
        {
            append("### Author", this.author, out);
        }

        if (this.citation != null)
        {
            append("### Citation", this.citation, out);
        }

        System.out.print(out.toString());
        System.exit(1);
    }

    public void printUsage()
    {
        StringBuilder out = new StringBuilder();
        {
            out.append("Name: \n\n  ");
            out.append(this.name);
            out.append("\n\n");
        }

        if (this.doc != null)
        {
            append("Description", this.doc, out);
        }

        if (this.positional.getMin() > 0)
        {
            out.append("Positional Arguments: \n\n  ");
            append(this.positional, out);
            out.append("\n");
        }

        append(this.getInput(false), "Required Input Arguments", out);
        append(this.getInput(true), "Optional Input Arguments", out);
        append(this.getParameter(false), "Required Parameter Arguments", out);
        append(this.getParameter(true), "Optional Parameter Arguments", out);
        append(this.getParameterAdvanced(), "Advanced Parameter Arguments", out);
        append(this.getOutput(false), "Required Output Arguments", out);
        append(this.getOutput(true), "Optional Output Arguments", out);

        if (this.author != null)
        {
            append("Author", this.author, out);
        }

        if (this.citation != null)
        {
            append("Citation", this.citation, out);
        }

        System.out.print(out.toString());
        System.exit(1);
    }

    public CliValues parse(String[] args)
    {
        return this.parse(Lists.newArrayList(args));
    }

    public CliValues parse(List<String> args)
    {
        CliValues entries = new CliValues();

        int idx = 0;
        while (idx < args.size())
        {
            if (!args.get(idx).contains("--"))
            {
                entries.pos.add(args.get(idx));
                idx += 1;
            }
            else
            {
                String[] tokens = args.get(idx).split("--");
                if (tokens.length != 2)
                {
                    Logging.error("invalid arguments");
                }

                String base = tokens[1];

                if (base.contains("="))
                {
                    String[] stokens = base.split("=");

                    String key = stokens[0];
                    List<String> vals = Lists.newArrayList();

                    for (String sarg : stokens[1].split(","))
                    {
                        vals.add(sarg);
                    }
                    idx += 1;

                    entries.keyed.put(key, vals);
                }
                else
                {
                    String key = base;
                    List<String> vals = Lists.newArrayList();

                    idx += 1;
                    while (idx < args.size() && !args.get(idx).startsWith("--"))
                    {
                        vals.add(args.get(idx));
                        idx += 1;
                    }

                    entries.keyed.put(key, vals);
                }
            }
        }

        if (entries.keyed.containsKey(HELP))
        {
            this.printUsage();
        }

        if (entries.keyed.containsKey(MARKDOWN))
        {
            this.printMarkdown();
        }

        if (entries.keyed.containsKey(VERBOSE))
        {
            Logging.console();
        }

        if (entries.keyed.containsKey(DEBUG))
        {
            Logging.debug();
        }

        Set<String> valid = Sets.newHashSet();
        {
            List<CliOption> all = Lists.newArrayList();
            all.addAll(this.getInput(false));
            all.addAll(this.getParameter(false));
            all.addAll(this.getOutput(false));
            for (CliOption entry : all)
            {
                String name = entry.getName();
                valid.add(name);
                if (!entries.keyed.containsKey(name))
                {
                    if (entry.hasDefault())
                    {
                        entries.keyed.put(name, Lists.newArrayList(entry.getDefault().split(" ")));
                    }
                    else
                    {
                        System.out.println("missing required entry: " + name);
                        System.out.println();
                        this.printUsage();
                    }
                }

                int size = entries.keyed.get(name).size();
                Global.assume(size >= entry.getMin() && size <= entry.getMax(), "invalid number of arguments: " + name);
            }
        }

        {
            List<CliOption> all = Lists.newArrayList();
            all.addAll(this.getInput(true));
            all.addAll(this.getParameter(true));
            all.addAll(this.getOutput(true));
            for (CliOption entry : all)
            {
                String name = entry.getName();
                valid.add(name);
                if (!entries.keyed.containsKey(name))
                {
                    if (entry.hasDefault())
                    {
                        List<String> def = Lists.newArrayList(entry.getDefault().split(" "));
                        entries.keyed.put(name, def);
                        int size = def.size();
                        Global.assume(size >= entry.getMin() && size <= entry.getMax(), "invalid number of arguments: " + name);
                    }
                }
            }
        }

        for (String key : entries.keyed.keySet())
        {
            Global.assume(valid.contains(key), "undefined parameter: " + key);
        }

        {
            int minPos = this.positional.getMin();
            int maxPos = this.positional.getMax();
            int numPos = entries.pos.size();
            if (numPos < minPos || numPos > maxPos)
            {
                System.out.println("invalid number of positional entries: " + numPos);
                System.out.println();
                this.printUsage();
            }
            else
            {
                Logging.info(String.format("found %d positional parameters", numPos));
            }
        }

        return entries;
    }

    private static List<String> format(String doc, int maxwidth)
    {
        List<String> lines = Lists.newArrayList();

        StringBuilder line = new StringBuilder();
        for (String token : doc.split(" "))
        {
            int len = token.length();
            if (line.length() + len > maxwidth)
            {
                lines.add(line.toString());
                line = new StringBuilder();
            }

            line.append(token);
            line.append(" ");
        }

        lines.add(line.toString());

        return lines;
    }

    private static String spaces(int spaces)
    {
        return CharBuffer.allocate(spaces).toString().replace('\0', ' ');
    }

    private static List<String> format(CliOption entry, int maxwidth)
    {
        List<String> lines = Lists.newArrayList();
        {
            StringBuilder line = new StringBuilder();
            line.append("--");
            line.append(entry.getName());
            line.append(" ");
            int indent = line.length();

            if (entry.getArgs().size() == 0)
            {
                lines.add(line.toString());
            }
            else
            {
                for (String arg : entry.getArgs())
                {
                    if (line.length() < indent)
                    {
                        line.append(spaces(indent));
                    }
                    line.append(arg);
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
            }

            lines.add("");
        }
        {
            String desc = entry.getDoc();
            if (entry.hasDefault())
            {
                desc += String.format(" (Default: %s)", entry.getDefault());
            }

            int indent = 2;
            for (String dline : format(desc, maxwidth - indent))
            {
                StringBuilder line = new StringBuilder();
                line.append(spaces(indent));
                line.append(dline);
                lines.add(line.toString());
            }

            lines.add("");
        }

        return lines;
    }

    public static void append(String header, String entry, StringBuilder out)
    {
        out.append(header + ":\n\n");
        for (String line : format(entry, WIDTHMAX - INDENTWIDTH))
        {
            out.append(INDENT);
            out.append(line);
            out.append("\n");
        }
        out.append("\n");
    }

    public static void append(CliOption entry, StringBuilder out)
    {
        for (String line : format(entry, WIDTHMAX - INDENTWIDTH))
        {
            out.append(INDENT);
            out.append(line);
            out.append("\n");
        }
    }

    public static void append(List<CliOption> input, String header, StringBuilder out)
    {
        if (input.size() > 0)
        {
            out.append(header + ":\n\n");
            for (CliOption entry : input)
            {
                append(entry, out);
            }
        }
    }

    public List<CliOption> getInput(boolean optional)
    {
        List<CliOption> out = Lists.newArrayList();
        for (CliOption entry : this.options)
        {
            if (entry.isInput() && !(optional ^ entry.isOptional()))
            {
                out.add(entry);
            }
        }
        return out;
    }

    public List<CliOption> getParameter(boolean optional)
    {
        List<CliOption> out = Lists.newArrayList();
        for (CliOption entry : this.options)
        {
            if (entry.isParameter() && !(optional ^ entry.isOptional()))
            {
                out.add(entry);
            }
        }
        return out;
    }

    public List<CliOption> getParameterAdvanced()
    {
        List<CliOption> out = Lists.newArrayList();
        for (CliOption entry : getParameter(true))
        {
            if (entry.isAdvanced())
            {
                out.add(entry);
            }
        }
        return out;
    }

    public List<CliOption> getOutput(boolean optional)
    {
        List<CliOption> out = Lists.newArrayList();
        for (CliOption entry : this.options)
        {
            if (entry.isOutput() && !(optional ^ entry.isOptional()))
            {
                out.add(entry);
            }
        }
        return out;
    }
}