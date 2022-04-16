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

package qit.base;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import qit.base.structs.DataType;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.formats.curves.DtkCurvesCoder;
import qit.data.formats.curves.VtkCurvesCoder;
import qit.data.formats.mesh.VtkMeshCoder;
import qit.data.formats.vects.TxtVectsCoder;
import qit.data.formats.volume.NiftiHeader;
import qit.data.formats.volume.NiftiVolumeCoder;
import qit.data.formats.volume.NrrdVolumeCoder;
import qit.data.datasets.Record;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Global
{
    public final static String NUMERIC_REGEX = "((-|\\+)?[0-9]+(\\.[0-9]+)?)+";
    public final static ImmutableSet<Class<?>> NUMERIC_TYPES;

    // voxel neighborhoods for looking up neighborhood
    public final static List<Integers> NEIGHBORS_6 = Lists.newArrayList();
    public final static List<Integers> NEIGHBORS_27 = Lists.newArrayList();

    public final static double DELTA = 1e-6;

    public static Random RANDOM = new Random();

    private static boolean EXPERT = false;

    // do not reuse nifti header information (forgets bitrate and voxel order, but coordinates are preserved)
    private static boolean FRESH = false;

    // force NONE as nifti intent code
    private static boolean NOINTENT = false;

    // a system default datatype (only use double or float)
    private static DataType DATATYPE = DataType.FLOAT;

    // this indicates whether prototyped and copied volumes should use the system default
    private static boolean PRESERVE = false;

    // this indicates whether output should be dumped when an error occurs
    private static boolean DUMP = false;

    // this specifies the pipeline hostname
    public static String PIPELINE_HOST = "cranium.loni.usc.edu";

    // this specifies the pipeline qit path
    public static String PIPELINE_PATH = "/ifshome/rcabeen/bin/qit";

    // this specifies that obj will be treated as minc instead of lightwave
    public static boolean MINC = false;

    static
    {
        Set<Class<?>> numerics = Sets.newHashSet();
        numerics.add(Integer.class);
        numerics.add(Long.class);
        numerics.add(Float.class);
        numerics.add(Double.class);
        numerics.add(int.class);
        numerics.add(long.class);
        numerics.add(float.class);
        numerics.add(double.class);
        NUMERIC_TYPES = ImmutableSet.copyOf(numerics);

        for (int i = -1; i <= 1; i++)
        {
            for (int j = -1; j <= 1; j++)
            {
                for (int k = -1; k <= 1; k++)
                {
                    int hash = i * i + j * j + k * k;

                    if (hash == 0)
                    {
                        continue;
                    }

                    if (hash == 1)
                    {
                        NEIGHBORS_6.add(new Integers(i, j, k));
                    }

                    NEIGHBORS_27.add(new Integers(i, j, k));
                }
            }
        }
    }

    public static void seed(long seed)
    {
        RANDOM = new Random(seed);
    }

    public static void setReference(Volume example)
    {
        DtkCurvesCoder.setReference(example.getSampling());
    }

    public static String getLicense()
    {
        String path = Global.getRoot();
        path = PathUtils.join(path, "doc");
        path = PathUtils.join(path, "licenses");
        path = PathUtils.join(path, "qit.txt");

        try
        {
            return FileUtils.readFileToString(new File(path));
        }
        catch (IOException e)
        {
            Logging.info("warning: failed to load license: " + path);
            return "error, no license found!";
        }
    }

    public static String getBuildInfo()
    {
        String path = Global.getRoot();
        path = PathUtils.join(path, "doc");
        path = PathUtils.join(path, "build.properties");

        try
        {
            return FileUtils.readFileToString(new File(path));
        }
        catch (IOException e)
        {
            Logging.info("warning: failed to load build info: " + path);
            return "error, no build information found!";
        }
    }

    public static String getCitation()
    {
        String path = Global.getRoot();
        path = PathUtils.join(path, "doc");
        path = PathUtils.join(path, "cite.txt");

        try
        {
            return FileUtils.readFileToString(new File(path));
        }
        catch (IOException e)
        {
            Logging.info("warning: failed to load citation: " + path);
            return "error, no citation found!";
        }
    }

    public static boolean getExpert()
    {
        return Global.EXPERT;
    }

    public static void setExpert(boolean v)
    {
        Global.EXPERT = v;
    }

    public static boolean getDump()
    {
        return Global.DUMP;
    }

    public static void setDump(boolean v)
    {
        Global.DUMP = v;
    }

    public static boolean getPreserve()
    {
        return Global.PRESERVE;
    }

    public static void setPreserve(boolean v)
    {
        Global.PRESERVE = v;
    }

    public static DataType getDataType()
    {
        return Global.DATATYPE;
    }

    public static boolean getNoIntent()
    {
        return Global.NOINTENT;
    }

    public static boolean getFresh()
    {
        return Global.FRESH;
    }

    public static void setFresh(boolean v)
    {
        Global.FRESH = v;
    }

    public static void setDataType(DataType dtype)
    {
        Global.DATATYPE = dtype;
    }

    public static void setDataType(String name)
    {
       Global.DATATYPE = DataType.valueOf(name);
    }

    public static DataType getDataType(DataType dt)
    {
        return Global.PRESERVE ? dt : Global.DATATYPE;
    }

    public static void setDiffusion(double bval, Vects bvecs)
    {
        NrrdVolumeCoder.bval = bval;
        NrrdVolumeCoder.bvecs = bvecs;
    }

    public static String getVersion()
    {
        String root = getRoot();
        String sep = File.separator;
        File version = new File(root + sep + "doc" + sep + "build.properties");
        try
        {
            Map<String,String> entries = Maps.newHashMap();
            for (String line : FileUtils.readLines(version))
            {
               if (line.contains("="))
               {
                   String[] tokens = line.split("=");
                   if (tokens.length == 2)
                   {
                       entries.put(tokens[0], tokens[1]);
                   }
               }
            }

            StringBuilder out = new StringBuilder();

            if (entries.containsKey("commit"))
            {
                out.append("commit: ");
                out.append(entries.get("commit"));
            }

            if (entries.containsKey("timestamp"))
            {
                if (out.length() > 0)
                {
                    out.append(", ");
                }

                out.append("timestamp: ");
                out.append(entries.get("timestamp"));
            }

            return out.toString();
        }
        catch (IOException e)
        {
            return "not found!";
        }
    }

    public static String getRoot()
    {
        String jar = Global.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        jar = jar.replace("%20", " ");

        // This allows debugging in Eclipse
        if (jar.endsWith("jar"))
        {
            return new File(jar).getParentFile().getParentFile().getParent();
        }
        else
        {
            return new File(jar).getParent();
        }
    }

    public static String getScriptsDir()
    {
        String root = Global.getRoot();
        String sep = File.separator;
        String scripts = root + sep + "lib" + sep + "modules" + sep + "qit";
        return scripts;
    }

    public static void nonnull(Object value, String name)
    {
        assume(value != null, String.format("%s should not be null", name));
    }

    public static void assume(boolean value, String message)
    {
        // Use this to check assumptions on input parameters.
        // This differs from an assertion because we throw an exception
        // and include a hopefully informative message to print.

        assert value;
        if (!value)
        {
            Logging.error(message);
        }
    }

    public static List<String> parse(List<String> args)
    {
        List<String> argv = Lists.newArrayList(args);

        if (argv.remove("--verbose") || argv.remove("-verbose"))
        {
            Logging.console();
        }

        if (argv.remove("--debug") || argv.remove("-debug"))
        {
            Logging.debug();
        }

        if (argv.remove("--memory") || argv.remove("-memory"))
        {
            Logging.memory();
        }

        if (argv.remove("--preserve"))
        {
            Global.PRESERVE = true;
        }

        if (argv.remove("--nointent"))
        {
            Global.NOINTENT = true;
        }

        if (argv.remove("--dump"))
        {
            Global.DUMP = true;
        }

        if (argv.contains("--dtype"))
        {
            int idx = argv.indexOf("--dtype");
            argv.remove(idx);
            String dn = argv.remove(idx);
            Logging.info("setting data type to " + dn);
            Global.setDataType(dn.toUpperCase());
        }

        if (argv.contains("--double"))
        {
            int idx = argv.indexOf("--double");
            argv.remove(idx);
            Logging.info("setting data type to double");
            Global.setDataType(DATATYPE.DOUBLE.toString());
        }

        if (argv.remove("--expert"))
        {
            Logging.info("setting expert mode");
            Global.setExpert(true);
        }

        if (argv.remove("--ascii"))
        {
            Logging.info("setting vtk data type to ascii");
            VtkCurvesCoder.ASCII = true;
            VtkMeshCoder.ASCII = true;
        }

        if (argv.remove("--minc"))
        {
            Logging.info("setting using minc obj");
            MINC = true;
        }

        if (argv.remove("--trkimage"))
        {
            Logging.info("setting trk image coordinate mode");
            DtkCurvesCoder.IMAGE = true;
        }

        if (argv.contains(TxtVectsCoder.FORMAT_FLAG))
        {
            int idx = argv.indexOf(TxtVectsCoder.FORMAT_FLAG);
            argv.remove(idx);
            TxtVectsCoder.FORMAT = argv.remove(idx);
            Logging.info("setting vects string format to " + TxtVectsCoder.FORMAT);
        }

        if (argv.remove("--fresh"))
        {
            Logging.info("setting fresh");
            FRESH = true;
        }

        if (argv.remove("--noscale"))
        {
            Logging.info("disabling nifti volume scaling");
            NiftiVolumeCoder.SCALE = false;
        }

        if (argv.contains("--cwd"))
        {
            int idx = argv.indexOf("--cwd");
            argv.remove(idx);
            String dn = argv.remove(idx);
            Logging.info("setting current working directory to " + dn);
            File d = new File(dn).getAbsoluteFile();
            if (d.exists() || d.mkdirs())
            {
                System.setProperty("user.dir", d.getAbsolutePath());
            }
        }

        if (argv.contains("--rseed"))
        {
            int idx = argv.indexOf("--rseed");
            argv.remove(idx);
            int rseed = Integer.valueOf(argv.remove(idx));
            Global.seed(rseed);
        }

        if (argv.size() == 1 && (argv.remove("--version") || argv.remove("-version")))
        {
            System.out.println("QIT " + Global.getVersion());
            System.exit(1);
        }

        return argv;
    }
}
