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

package qit.base.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import qit.base.Global;
import qit.base.Logging;

/**
 * utilities for manipulating file paths
 */
public class PathUtils
{
    public static void write(List<String> vals, String fn) throws FileNotFoundException
    {
        PrintWriter pw = new PrintWriter(fn);
        for (String val : vals)
        {
            pw.println(val);
        }
        pw.close();
    }

    public static String noext(String path)
    {
        if (path.endsWith(".gz"))
        {
            path = path.split(".gz")[0];
        }

        if (path.contains("."))
        {
            String ext = FilenameUtils.getExtension(path);
            return path.split("." + ext)[0];
        }
        else
        {
            return path;
        }
    }

    public static String basename(String path)
    {
        return new File(path).getName();
    }

    public static String dirname(String path)
    {
        return new File(path).getParent();
    }

    public static boolean isFile(String fn)
    {
        File f = new File(fn);
        return f.exists() && f.isFile();
    }

    public static boolean isDir(String fn)
    {
        File f = new File(fn);
        return f.exists() && f.isDirectory();
    }

    public static boolean isWritable(String fn)
    {
        File f = new File(fn);
        return f.canWrite();
    }

    public static boolean exists(String fn)
    {
        if (fn == null)
        {
            return false;
        }
        else
        {
            return new File(fn).exists();
        }
    }

    public static String join(String dn, String bn)
    {
        return new File(dn, bn).getAbsolutePath();
    }

    public static String absolute(String fn)
    {
        return new File(fn).getAbsolutePath();
    }

    public static void mkdirs(String dn) throws IOException
    {
        Global.assume(!isFile(dn), "cannot make dir: " + dn);

        if (!exists(dn))
        {
            new File(dn).mkdirs();
        }
    }

    public static void mkpar(String fn) throws IOException
    {
        String parent = PathUtils.dirname(PathUtils.absolute(fn));
        if (!PathUtils.exists(parent))
        {
            PathUtils.mkdirs(parent);
        }
    }

    public static void delete(String path) throws IOException
    {
        FileUtils.forceDelete(new File(path));
    }

    public static String backup(String path)
    {
        if (isFile(path))
        {
            return backupFile(path);
        }
        else if (isDir(path))
        {
            return backupDir(path);
        }
        else
        {
            return null;
        }
    }

    public static String backupFile(String fn)
    {
        Global.assume(!isDir(fn), "cannot backup file: " + fn);

        if (isFile(fn))
        {
            String bfn = null;

            int maxiter = 1000;
            int iter = 0;
            while (bfn == null || exists(bfn))
            {
                bfn = fn + ".bck." + String.valueOf((int) (System.currentTimeMillis() / 1000L));
                iter += 1;

                if (iter > maxiter)
                {
                    Logging.error("failed to backup file: " + fn);
                }
            }

            try
            {
                FileUtils.moveFile(new File(fn), new File(bfn));
                Logging.info("backed up file: " + bfn);

                return bfn;
            }
            catch (IOException e)
            {
                Logging.error("failed to backup file: " + fn);
            }
        }

        return null;
    }

    public static String backupDir(String dn)
    {
        Global.assume(!isFile(dn), "cannot backup directory: " + dn);

        if (isDir(dn))
        {
            String bdn = null;

            int maxiter = 1000;
            int iter = 0;
            while (bdn == null || exists(bdn))
            {
                bdn = dn + ".bck." + String.valueOf((int) (System.currentTimeMillis() / 1000L));
                iter += 1;

                if (iter > maxiter)
                {
                    Logging.error("failed to backup directory: " + dn);
                }
            }

            try
            {
                FileUtils.moveDirectory(new File(dn), new File(bdn));
                Logging.info("backed up dir: " + bdn);

                return bdn;
            }
            catch (IOException e)
            {
                Logging.error("failed to backup directory: " + dn);
            }
        }

        return null;
    }

    public static String tmpFilename(String fn) throws IOException
    {
        int maxiter = 1000;
        int iter = 0;
        String bfn = null;
        while (bfn == null || exists(bfn))
        {
            bfn = fn + ".tmp." + String.valueOf((int) (System.currentTimeMillis() / 1000L));
            iter += 1;

            if (iter > maxiter)
            {
                Logging.error("failed to create temporary filename: " + fn);
            }
        }

        return bfn;
    }

    public static String tmpDir(String dn) throws IOException
    {
        int maxiter = 1000;
        int iter = 0;
        String bdn = null;
        while (bdn == null || exists(bdn))
        {
            bdn = dn + ".tmp." + String.valueOf((int) (System.currentTimeMillis() / 1000L));
            iter += 1;

            if (iter > maxiter)
            {
                Logging.error("failed to create temporary directory: " + dn);
            }
        }

        PathUtils.mkdirs(bdn);

        return bdn;
    }

    public static String move(String from, String to, boolean backup) throws IOException
    {
        String bfn = null;
        if (exists(to))
        {
            if (backup)
            {
                bfn = PathUtils.backup(to);
            }
            else
            {
                PathUtils.delete(to);
            }
        }

        if (PathUtils.isFile(from))
        {
            FileUtils.moveFile(new File(from), new File(to));
        }
        else
        {
            FileUtils.moveDirectory(new File(from), new File(to));
        }

        return bfn;
    }
}
