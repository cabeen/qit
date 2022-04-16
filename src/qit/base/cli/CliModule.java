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

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Clock;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import qit.base.CliMain;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleExpert;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.utils.ModuleUtils;
import qit.base.utils.PathUtils;

/**
 * a class for accessing a module on the command line
 */
public class CliModule implements CliMain
{
    private Module module;

    public CliModule(Module v)
    {
        this.module = v;
        ModuleUtils.validate(this.module);
    }

    public void norun(List<String> args)
    {
        CliValues values = cli(this.module).parse(args);

        Logging.info("started");

        Logging.info("reading input");
        read(this.module, values);

        Logging.info("finished");
    }

    @Override
    public void run(List<String> args)
    {
        try
        {
            CliValues values = cli(this.module).parse(args);

            Logging.info("started");

            Logging.info("version: " + Global.getVersion());
            Logging.info("local time: " + Calendar.getInstance().getTime());
            Logging.info("utc time: " + Clock.systemUTC().instant());
            Logging.info("os arch: " + System.getProperty("os.arch", "generic").toLowerCase(Locale.ENGLISH));
            Logging.info("os name: " + System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH));
            Logging.info("os version: " + System.getProperty("os.version", "generic").toLowerCase(Locale.ENGLISH));
            Logging.info("java version: " + System.getProperty("java.version"));

            Logging.info("reading input");
            read(this.module, values);

            Logging.info("started operation");
            this.module.run();

            Logging.info("writing output");
            write(this.module, values);

            Logging.info("finished");
        }
        catch (IOException e)
        {
            Logging.error("error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void read(Module module, CliValues values)
    {
        Global.assume(values.pos.size() == 0, "expected no positional arguments");

        try
        {
            for (Field field : ModuleUtils.fields(module))
            {
                if (field.getAnnotation(ModuleOutput.class) != null)
                {
                    continue;
                }

                boolean optional = field.getAnnotation(ModuleOptional.class) != null;
                boolean binary = field.getType().equals(boolean.class) || field.getType().equals(Boolean.class);

                String fieldName = field.getName();
                Class<?> fieldType = field.getType();

                field.setAccessible(true);

                if (values.keyed.containsKey(fieldName))
                {
                    if (binary)
                    {
                        boolean paramValue = true;
                        Logging.info("using " + fieldName + ": " + paramValue);
                        field.set(module, paramValue);
                    }
                    else
                    {
                        List<String> args = values.keyed.get(fieldName);

                        if (args.size() != 1)
                        {
                            Logging.error("invalid param: " + fieldName);
                        }

                        String valueRaw = args.get(0);
                        Logging.info("using " + fieldName + ": " + valueRaw);
                        field.set(module, read(fieldType, valueRaw));
                    }
                }

                if (field.get(module) == null && !optional)
                {
                    Logging.error("unspecified module field: " + fieldName);
                }
            }
        }
        catch (IllegalArgumentException | IllegalAccessException e)
        {
            Logging.error("failed to population module fields (report this as a bug): " + e.getMessage());
        }
    }

    private static void write(Module module, CliValues values)
    {
        try
        {
            boolean failed = false;

            for (Field field : ModuleUtils.fields(module))
            {
                if (field.getAnnotation(ModuleOutput.class) == null)
                {
                    continue;
                }

                boolean optional = field.getAnnotation(ModuleOptional.class) != null;

                field.setAccessible(true);
                if (field.get(module) == null && optional)
                {
                    continue;
                }

                String fieldName = field.getName();

                if (!values.keyed.containsKey(fieldName) && !optional)
                {
                    throw new RuntimeException("missing parameter: " + fieldName);
                }

                if (field.get(module) == null && !optional)
                {
                    throw new RuntimeException("missing output: " + fieldName);
                }

                if (!values.keyed.containsKey(fieldName))
                {
                    if (!optional)
                    {
                        throw new RuntimeException("missing value: " + fieldName);
                    }
                    continue;
                }

                List<String> args = values.keyed.get(fieldName);

                if (args.size() != 1)
                {
                    throw new RuntimeException("invalid output: " + fieldName);
                }

                String valueRaw = args.get(0);
                Logging.info("using " + fieldName + ": " + valueRaw);
                try
                {
                    Object data = field.get(module);
                    Method method = data.getClass().getMethod("write", String.class);

                    String path = valueRaw;
                    String parent = PathUtils.dirname(PathUtils.absolute(path));

                    if (!PathUtils.exists(parent))
                    {
                        PathUtils.mkdirs(parent);
                    }

                    if (PathUtils.isWritable(parent))
                    {
                        method.invoke(data, valueRaw);
                    }
                    else if (Global.getDump())
                    {
                        Logging.info(String.format("warning: could not parameter '%s' to '%s'", fieldName, valueRaw));

                        String npath = PathUtils.join(System.getProperty("user.home"), ".qit");
                        npath = PathUtils.join(npath, "backup_" + Math.abs(Global.RANDOM.nextInt()));
                        npath = PathUtils.join(npath, PathUtils.basename(path));

                        Logging.info(String.format("warning: saving '%s' to '%s'", fieldName, npath));
                        PathUtils.mkdirs(PathUtils.dirname(npath));
                        method.invoke(data, npath);
                        failed = true;
                    }
                    else
                    {
                        throw new RuntimeException("output could not be written due to permissions issue");
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    throw new RuntimeException("failed to write output parameter: " + fieldName);
                }
            }

            if (failed)
            {
                Logging.error("module failed.  check log for more information");
            }
        }
        catch (IllegalArgumentException | IllegalAccessException e)
        {
            Logging.error("failed to find operation: " + e.getMessage());
        }

    }

    private static Object read(Class<?> type, String value)
    {
        try
        {
            if (type.equals(String.class))
            {
                return value;
            }
            else if (type.equals(Byte.class) || type.equals(byte.class))
            {
                return Byte.valueOf(value);
            }
            else if (type.equals(Short.class) || type.equals(short.class))
            {
                return Short.valueOf(value);
            }
            else if (type.equals(Integer.class) || type.equals(int.class))
            {
                return Integer.valueOf(value);
            }
            else if (type.equals(Long.class) || type.equals(long.class))
            {
                return Long.valueOf(value);
            }
            else if (type.equals(Float.class) || type.equals(float.class))
            {
                return Float.valueOf(value);
            }
            else if (type.equals(Double.class) || type.equals(double.class))
            {
                return Double.valueOf(value);
            }
            else if (type.isEnum())
            {
                return Enum.valueOf((Class<Enum>) type, value);
            }
        }
        catch (RuntimeException e)
        {
            Logging.error(String.format("failed to parse %s: %s", type.getSimpleName(), value));
        }

        try
        {
            Method method = type.getMethod(Dataset.READ, String.class);
            return method.invoke(null, value);
        }
        catch (Exception e)
        {
            if (!PathUtils.exists(value))
            {
                Logging.error(String.format("file not found: %s", value));
            }
            else
            {
                e.printStackTrace();
                Logging.error(String.format("failed to parse %s: %s", type.getSimpleName(), value));
            }
        }

        return null;
    }

    private static String getDescription(AnnotatedElement field)
    {
        String doc = "none";
        {
            ModuleDescription annot = field.getAnnotation(ModuleDescription.class);
            if (annot != null)
            {
                doc = annot.value();
            }
        }

        return doc;
    }

    private static CliSpecification cli(Module module)
    {
        Class<? extends Module> type = module.getClass();

        CliSpecification cli = new CliSpecification();
        cli.withName(type.getSimpleName());
        cli.withDoc(getDescription(type));

        ModuleAuthor author = type.getAnnotation(ModuleAuthor.class);
        if (author != null)
        {
            cli.withAuthor(author.value());
        }

        ModuleCitation citation = type.getAnnotation(ModuleCitation.class);
        if (citation != null)
        {
            cli.withCitation(citation.value());
        }

        for (Field field : ModuleUtils.fields(module))
        {
            if (!Global.getExpert() && field.getAnnotation(ModuleExpert.class) != null)
            {
                continue;
            }

            boolean optional = field.getAnnotation(ModuleOptional.class) != null;
            boolean advanced = field.getAnnotation(ModuleAdvanced.class) != null;
            boolean input = field.getAnnotation(ModuleInput.class) != null;
            boolean output = field.getAnnotation(ModuleOutput.class) != null;
            boolean binary = field.getType().equals(boolean.class) || field.getType().equals(Boolean.class);

            String doc = getDescription(field);
            String fieldName = field.getName();
            Object fieldValue = ModuleUtils.value(module, field);

            if (fieldName.equals("inplace"))
            {
                continue;
            }

            Class<?> fieldType = field.getType();
            if (fieldType.isEnum())
            {
                Object[] values = fieldType.getEnumConstants();

                StringBuffer buffer = new StringBuffer();
                buffer.append(" (Options: ");
                for (int i = 0; i < values.length; i++)
                {
                    buffer.append(values[i].toString());
                    if (i < values.length - 1)
                    {
                        buffer.append(", ");
                    }
                }
                buffer.append(")");

                doc += buffer.toString();
            }


            CliOption clp = new CliOption();
            clp.withName(fieldName);
            clp.withDoc(doc);

            if (!binary)
            {
                if (fieldValue != null)
                {
                    clp.withDefault(fieldValue.toString());
                }
                clp.withArg(String.format("<%s>", field.getType().getSimpleName()));
            }

            if (input)
            {
                clp.asInput();
            }
            else if (output)
            {
                clp.asOutput();
            }
            else
            {
                clp.asParameter();
            }

            if (optional || fieldValue != null)
            {
                clp.asOptional();
            }

            if (advanced)
            {
                clp.asAdvanced();
            }

            cli.withOption(clp);
        }

        return cli;
    }
}
