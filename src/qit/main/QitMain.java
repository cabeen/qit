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
import org.apache.commons.lang3.StringUtils;
import org.python.util.PythonInterpreter;
import org.reflections.Reflections;
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Interpreter;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleUnlisted;
import qit.base.cli.CliModule;
import qit.base.cli.CliUtils;
import qit.base.utils.ModuleUtils;
import qit.base.utils.PathUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class QitMain
{
    private static List<String> listJython()
    {
        String root = Global.getRoot();
        String sep = File.separator;
        String scripts = root + sep + "lib" + sep + "modules" + sep + "qit";

        File[] files = new File(scripts).listFiles(new FilenameFilter()
        {
            public boolean accept(File dir, String filename)
            {
                return filename.endsWith(".py") && !filename.equals("common.py");
            }
        });

        List<String> out = Lists.newArrayList();

        if (files != null)
        {
            for (File file : files)
            {
                String name = StringUtils.split(PathUtils.basename(file.getAbsolutePath()), ".")[0];
                out.add(name);
            }
        }

        return out;
    }

    private static Map<String, CommandLineMainSource> listCommand()
    {
        try
        {
            // disable info messages
            Reflections.log = null;
            Reflections reflections = new Reflections("qit");

            Map<String, CommandLineMainSource> out = Maps.newLinkedHashMap();
            for (final Class<? extends CliMain> c : reflections.getSubTypesOf(CliMain.class))
            {
                if (!c.equals(CliModule.class) && c.getAnnotation(ModuleUnlisted.class) == null)
                {
                    String name = c.getName();
                    String[] tokens = StringUtils.split(name, ".");
                    String base = tokens[tokens.length - 1];

                    out.put(base, new CommandLineMainSource()
                    {
                        public CliMain create()
                        {
                            try
                            {
                                return c.newInstance();
                            }
                            catch (Exception e)
                            {
                                Logging.error("failed to run command");
                                return null;
                            }
                        }
                    });
                }
            }
            for (final Class<? extends Module> c : reflections.getSubTypesOf(Module.class))
            {
                if (c != null && c.getAnnotation(ModuleUnlisted.class) == null)
                {
                    String name = c.getName();
                    String[] tokens = StringUtils.split(name, ".");
                    String base = tokens[tokens.length - 1];

                    if (name.contains("$"))
                    {
                        continue;
                    }

                    out.put(base, new CommandLineMainSource()
                    {
                        public CliMain create()
                        {
                            try
                            {
                                return new CliModule(c.newInstance());
                            }
                            catch (Exception e)
                            {
                                Logging.error("failed to build command");
                                return null;
                            }
                        }
                    });
                }
            }
            return out;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("failed to load commands: " + e.getMessage());
            return null;
        }
    }

    private static void runCommand(List<String> args)
    {
        String name = "unknown";
        try
        {
            // disable info messages
            Reflections.log = null;
            Reflections reflections = new Reflections("qit");

            // look for climain classes to run
            for (final Class<? extends CliMain> c : reflections.getSubTypesOf(CliMain.class))
            {
                if (!c.equals(CliModule.class) && c.getName().endsWith(args.get(0)))
                {
                    name = args.remove(0);
                    c.newInstance().run(args);
                    return;
                }
            }

            String savefn = null;

            if (args.contains("--save"))
            {
                int idx = args.indexOf("--save");
                Global.assume(idx + 1 < args.size(), "invalid parameters file");
                args.remove(idx);
                savefn = args.remove(idx);
            }

            Module module = null;

            if (args.contains("--load"))
            {
                int idx = args.indexOf("--load");
                Global.assume(idx + 1 < args.size(), "invalid parameters file");
                args.remove(idx);
                String fn = args.remove(idx);

                Logging.info("loading parameters file: " + fn);
                module = ModuleUtils.read(fn);

                String mname = module.getClass().getSimpleName();
                if (args.contains(mname))
                {
                    args.remove(mname);
                    name = mname;
                }
            }
            else
            {
                name = args.get(0);

                // look for modules to run
                for (final Class<? extends Module> c : reflections.getSubTypesOf(Module.class))
                {
                    if (c != null && c.getSimpleName().equals(name))
                    {
                        args.remove(0);
                        module = c.newInstance();
                        break;
                    }
                }

                if (module == null)
                {
                    System.out.println();
                    System.out.println(String.format("  No module named %s was found!", name));

                    List<String> sort = ModuleUtils.sort(name);
                    if (sort != null && sort.size() > 0)
                    {
                        System.out.println();
                        System.out.println("  Maybe you are looking for one of these?");
                        System.out.println();
                        for (int i = 0; i < Math.min(10, sort.size()); i++)
                        {
                            System.out.println(String.format("    %s", sort.get(i)));
                        }
                    }

                    System.out.println();

                    return;
                }
            }

            if (module != null)
            {
                if (args.remove("--norun"))
                {
                    new CliModule(module).norun(args);
                }
                else
                {
                    new CliModule(module).run(args);
                }

                if (savefn != null)
                {
                    Logging.info("saving parameters file: " + savefn);
                    ModuleUtils.write(module, savefn);
                }

                return;
            }

            Logging.error("command not found: " + args.get(0));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error(String.format("failed to run %s: %s", name, e.getMessage()));
        }
    }

    private static void runJython(List<String> args, PythonInterpreter interp)
    {
        String sep = File.separator;
        String scripts = Global.getScriptsDir();
        interp.exec("sys.path.append(\"" + scripts + "\")");

        interp.set("args", args);
        interp.exec("sys.argv = list(args)");

        String cmd = args.get(0);
        if (cmd.endsWith(".py") && new File(cmd).exists())
        {
            Logging.info("loading user script");
            interp.exec("sys.path.append(\"" + new File(cmd).getParent() + "\")");
            interp.execfile(cmd);
        }
        else if (new File(scripts + sep + cmd + ".py").exists())
        {
            Logging.info("loading built-in script " + cmd);
            interp.execfile(scripts + sep + cmd + ".py");
        }
        else
        {
            throw new RuntimeException("invalid jython script: " + cmd);
        }
    }

    // a layer to protect commands across multiple batches
    private static abstract class CommandLineMainSource
    {
        public abstract CliMain create();
    }

    public static void main(String[] args)
    {
        try
        {
            Locale.setDefault(new Locale("en", "US"));

            List<String> argv = Global.parse(Lists.newArrayList(args));

            if (argv.remove("--validate"))
            {
                ModuleUtils.validate();
                return;
            }

            if (argv.remove("--expert"))
            {
                Logging.info("using expert mode");
                Global.setExpert(true);
            }

            if (argv.contains("--pipeline-host"))
            {
                int idx = argv.indexOf("--pipeline-host");
                Global.assume(idx + 1 < argv.size(), "expected arguments specifying hostname");

                argv.remove(idx);
                String name = argv.remove(idx);

                Global.PIPELINE_HOST = name;

                return;
            }

            if (argv.contains("--pipeline-path"))
            {
                int idx = argv.indexOf("--pipeline-path");
                Global.assume(idx + 1 < argv.size(), "expected arguments specifying path");

                argv.remove(idx);
                String name = argv.remove(idx);

                Global.PIPELINE_PATH = name;

                return;
            }

            if (argv.contains("--pipeline"))
            {
                int idx = argv.indexOf("--pipeline");
                Global.assume(idx + 2 < argv.size(), "expected arguments specifying module name and output filename");

                argv.remove(idx);
                String name = argv.remove(idx);
                String fn = argv.remove(idx);

                ModuleUtils.writePipeline(ModuleUtils.instance(name), fn);

                return;
            }

            if (argv.contains("--pipelines"))
            {
                int idx = argv.indexOf("--pipelines");
                Global.assume(idx + 1 < argv.size(), "expected arguments specifying module name and output filename");

                argv.remove(idx);
                String dn = argv.remove(idx);

                ModuleUtils.writePipelines(dn);

                return;
            }

            if (argv.remove("--interactive"))
            {
                Interpreter.interactive();
                return;
            }

            List<String> scripts = listJython();
            Map<String, CommandLineMainSource> commands = listCommand();
            List<String> names = Lists.newArrayList();
            for (String name : scripts)
            {
                names.add(name);
            }
            for (String name : commands.keySet())
            {
                names.add(name);
            }
            Collections.sort(names);

            if (argv.size() == 0 || argv.remove("--list"))
            {
                System.out.println("");
                System.out.println("Usage: qit [jvm_args] [batch_args] <command> [cmd_args]");
                System.out.println("");
                System.out.println("  Run a qit command.  The first argument may be either the name of a built-in");
                System.out.println("  command or the path to a user-defined Jython script.  Each command has its ");
                System.out.println("  own usage page which should be consulted for help.");
                System.out.println("");
                System.out.println("Built-in commands:");
                System.out.println("");
                for (String name : names)
                {
                    System.out.println("  " + name);
                }
                System.out.println("");

                return;
            }

            boolean cont = argv.remove("--continue");

            PythonInterpreter interp = null;
            List<List<String>> batches = CliUtils.batches(argv, "--batch", "--batch-var", "--batch-table", "--batch-product");
            for (int i = 0; i < batches.size(); i++)
            {
                List<String> batch = batches.get(i);

                Logging.info(String.format("starting batch: %d", (i + 1)));
                Logging.info(String.format("batch args: %s", StringUtils.join(batch, " ")));

                try
                {
                    String cmd = batch.get(0);
                    if (scripts.contains(cmd) || PathUtils.exists(cmd))
                    {
                        if (interp == null)
                        {
                            interp = Interpreter.build();
                        }
                        runJython(batch, interp);
                    }
                    else
                    {
                        runCommand(batch);
                    }
                }
                catch (RuntimeException e)
                {
                    if (cont)
                    {
                        Logging.info("skipping batch due to error: " + e.getMessage());
                    }
                    else
                    {
                        throw e;
                    }
                }

            }
        }
        catch (Exception e)
        {
            if (Logging.DEBUG)
            {
                e.printStackTrace();
            }
            Logging.error(e.getMessage());
        }
    }
}
