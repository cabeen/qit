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

package qitview.main;

import qit.base.Global;
import qit.base.Logging;
import qitview.panels.Viewables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.reflections.Reflections;
import qit.base.Dataset;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qit.base.utils.PathUtils;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.widgets.FileLoader;
import qitview.widgets.SwingUtils;

import javax.swing.*;
import java.time.Clock;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Viewer
{
    public static boolean FULLPATH = false;

    private static boolean BUILDING = false;
    private static boolean READY = false;
    private static State INSTANCE = null;

    public static boolean ready()
    {
        return READY;
    }

    public static void build()
    {
        Global.assume(INSTANCE == null && !BUILDING, "instance already build!");

        Viewer.BUILDING = true;
        Logging.info("started qitview");
        Logging.info("build " + Global.getVersion());
        Logging.info("local time: " + Calendar.getInstance().getTime());
        Logging.info("utc time: " + Clock.systemUTC().instant());
        Logging.info("os arch: " + System.getProperty("os.arch", "generic").toLowerCase(Locale.ENGLISH));
        Logging.info("os name: " + System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH));
        Logging.info("os version: " + System.getProperty("os.version", "generic").toLowerCase(Locale.ENGLISH));
        Logging.info("java version: " + System.getProperty("java.version"));

        Logging.info("initializing");

        try
        {
            Viewer.INSTANCE = new State();
            Viewer.BUILDING = false;
            Viewer.READY = true;
        }
        catch (Exception ex)
        {
            if (Logging.DEBUG)
            {
                ex.printStackTrace();
            }

            Logging.info(ex.getMessage());

            if (Viewer.INSTANCE != null)
            {
                Viewer.INSTANCE.gui.close();
            }

            Viewer.BUILDING = false;
            Viewer.READY = false;
        }
    }

    public static void check()
    {
        Global.assume(!BUILDING, "bug, viewer currently being built");
    }

    public static State getInstance()
    {
        Viewer.check();
        return Viewer.INSTANCE;
    }

    public Viewer()
    {
        Global.assume(INSTANCE == null, "bug encountered, viewer already exists!");
    }

    public static void main(String[] args)
    {
        Reflections.log = null;

        List<String> argv = Global.parse(Lists.newArrayList(args));

        if (argv.remove("--fullpath"))
        {
            FULLPATH = true;
        }

        boolean colorshare = false;
        if (argv.remove("--colorshare"))
        {
            colorshare = true;
        }

        final Map<String, List<String>> inputs = Maps.newLinkedHashMap();
        final List<String> untyped = Lists.newArrayList();

        while (argv.size() > 0)
        {
            String arg = argv.remove(0);
            if (arg.startsWith("--"))
            {
                String id = arg.substring(2).toLowerCase();
                List<String> fns = Lists.newArrayList();
                while (argv.size() > 0 && !argv.get(0).startsWith("--"))
                {
                    fns.add(argv.remove(0));
                }

                if (fns.size() != 0)
                {
                    inputs.put(id, fns);
                }
            }
            else
            {
                untyped.add(arg);

                while (argv.size() > 0 && !argv.get(0).startsWith("--"))
                {
                    untyped.add(argv.remove(0));
                }
            }
        }

        // Start the show
        SwingUtils.invokeAndWait(() ->
        {
            Viewer.build();

            if (untyped.size() > 0)
            {
                new FileLoader(untyped);
            }
        });

        // add files to the queue
        boolean found = false;
        for (String name : inputs.keySet())
        {
            if (name.equals("scene"))
            {
                Viewer.getInstance().data.openScene(inputs.get(name));
                found = true;
            }
            else if (name.equals("global"))
            {
                Viewer.getInstance().data.openGlobalSettings(inputs.get(name).get(0));
                found = true;
            }

            if (name.equals("cd"))
            {
                String dn = inputs.get(name).get(0);
                Logging.info("changing working directory to " + dn);
                System.setProperty( "user.dir", dn);
            }
            else if (name.equals("colormap"))
            {
                for (String input : inputs.get(name))
                {
                    Viewer.getInstance().colormaps.importColormap(input);
                }
                found = true;
            }
            else
            {
                if (ViewableType.hasName(name))
                {
                    // always use fullpath naming for command line data
                    ViewableType type = ViewableType.getFromName(name);
                    Viewer.getInstance().control.readQueue(type, inputs.get(name), true);
                    found = true;
                }
            }

            if (!found)
            {
                Logging.info(String.format("warning: data type '%s' not found", name));
            }
        }

        {
            String settingsDir = PathUtils.join(System.getProperty("user.home"), ".qit");
            String globals = PathUtils.join(settingsDir, Constants.GLOBAL_SETTINGS);
            if (PathUtils.exists(globals))
            {
                Viewer.getInstance().data.openGlobalSettings(globals);
            }
        }

        Settings settings = Viewer.getInstance().settings;
        settings.colorshare = colorshare;

        Runnable qloader = () ->
        {
            if (Viewer.getInstance().qload.isEmpty())
            {
                return;
            }

            List<Triple<ViewableType, List<String>, Boolean>> load = Lists.newArrayList();

            while (!Viewer.getInstance().qload.isEmpty())
            {
                load.add(Viewer.getInstance().qload.poll());
            }

            int total = 0;
            for (Triple<ViewableType, List<String>, Boolean> d : load)
            {
                total += d.b.size();
            }
            String text = String.format("Reading %d files", total);

            Viewer.getInstance().gui.addProcess(text, new Thread(() ->
            {
                try
                {
                    Controller control = Viewer.getInstance().control;
                    for (Triple<ViewableType, List<String>, Boolean> pair : load)
                    {
                        final ViewableType type = pair.a;
                        List<String> fns = pair.b;
                        boolean fullpath = pair.c;

                        Map<String, String> names = control.names(fns, fullpath);
                        for (final String fn : names.keySet())
                        {
                            control.setStatusMessage("started loading: " + fn);
                            final String name = names.get(fn);

                            control.setStatusMessage("started reading file: " + fn);
                            final Dataset data = ViewableType.read(type, fn);
                            control.setStatusMessage("finished reading file: " + fn);

                            SwingUtilities.invokeLater(() ->
                            {
                                Viewable viewable = ViewableType.create(type);
                                viewable.setName(name);
                                viewable.setFilename(fn);
                                viewable.setData(data);

                                Viewer.getInstance().qviewables.offer(viewable);
                            });

                            Viewer.getInstance().control.setStatusMessage("finished loading: " + fn);
                        }
                    }
                }
                catch (Exception e)
                {
                    SwingUtils.safeMessage("warning: failed to load data.  check the console for more information");
                    e.printStackTrace();
                }
            }));
        };

        Runnable qrunner = () ->
        {
            if (Viewer.getInstance().qrun.isEmpty())
            {
               return;
            }

            Pair<String, Runnable> run = Viewer.getInstance().qrun.poll();

            Logging.info("offering process: " + run.a);
            Viewer.getInstance().gui.addProcess(run.a, run.b);
        };

        while (Viewer.getInstance().running)
        {
            try
            {
                Thread.sleep(100);

                if (Viewer.getInstance().started)
                {
                    qloader.run();
                    qrunner.run();
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}