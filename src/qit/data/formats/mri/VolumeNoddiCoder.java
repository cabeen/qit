/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.formats.mri;

import qit.base.Logging;
import qit.base.ModelType;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sample;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.models.Noddi;
import qit.data.modules.mri.model.VolumeModelFeature;

import java.io.File;
import java.io.IOException;

public class VolumeNoddiCoder
{
    public static boolean matches(String path)
    {
        return Noddi.matches(PathUtils.basename(path)) || PathUtils.exists(path + "_odi.nii.gz") || PathUtils.exists(path + "_odi.nii");
    }

    public static Volume read(String path) throws IOException
    {
        if (PathUtils.isDir(path))
        {
            return readDirectory(path);
        }
        else if (!PathUtils.exists(path))
        {
            return readBasename(path);
        }
        else
        {
            return Volume.read(path);
        }
    }

    public static Volume readBasename(String basename) throws IOException
    {
        // read a noddi volume organized by a basename (typically subject identifier)

        Volume base = readSafe(String.format("%s_%s", basename, Noddi.BASELINE));
        Volume ficvf = readSafe(String.format("%s_%s", basename, Noddi.FICVF));
        Volume fiso = readSafe(String.format("%s_%s", basename, Noddi.FISO));
        Volume odi = readSafe(String.format("%s_%s", basename, Noddi.ODI));
        Volume dirx = readSafe(String.format("%s_%s", basename, Noddi.DIRX));
        Volume diry = readSafe(String.format("%s_%s", basename, Noddi.DIRY));
        Volume dirz = readSafe(String.format("%s_%s", basename, Noddi.DIRZ));
        Volume irfrac = readSafe(String.format("%s_%s", basename, Noddi.IRFRAC));

        Volume out = ficvf.proto(Noddi.DIM);

        for (Sample sample : out.getSampling())
        {
            Noddi noddi = new Noddi();
            if (ficvf != null)
            {
                noddi.setFICVF(ficvf.get(sample, 0));
            }

            if (fiso != null)
            {
                noddi.setFISO(fiso.get(sample, 0));
            }

            if (odi != null)
            {
                noddi.setODI(odi.get(sample, 0));
            }

            if (dirx != null && diry != null && dirz != null)
            {
                double dx = dirx.get(sample, 0);
                double dy = diry.get(sample, 0);
                double dz = dirz.get(sample, 0);

                noddi.setDir(VectSource.create(dx, dy, dz));
            }

            if (irfrac != null)
            {
                noddi.setIRFRAC(irfrac.get(sample, 0));
            }

            if (base != null)
            {
                noddi.setBaseline(base.get(sample, 0));
            }

            out.set(sample, noddi.getEncoding());
        }

        System.gc();
        System.gc();

        return out;
    }

    public static Volume readDirectory(String path) throws IOException
    {
        // read a noddi volume organized within a single directory

        File file = new File(path).getAbsoluteFile();
        String basename = null;
        if (file.isDirectory())
        {
            // read a directory
            basename = new File(path, "noddi").getAbsolutePath();
        }
        else
        {
            // read from a basename
            String dirname = file.getParent();
            String name = file.getName();
            String base = name.split("[_...nii]?")[0];
            basename = new File(dirname, base).getAbsolutePath();
        }

        return readBasename(basename);
    }

    public static boolean existsSafe(String path)
    {
        // check if any combination of volume names exists

        return PathUtils.exists(path) || PathUtils.exists(path + ".nii") || PathUtils.exists(path + ".nii.gz");
    }

    public static Volume readSafe(String path) throws IOException
    {
        // handle files with our without gz

        if (path.endsWith("nii") || path.endsWith("nii.gz"))
        {
            return Volume.read(path);
        }
        else if (PathUtils.exists(path + ".nii"))
        {
            return Volume.read(path + ".nii");
        }
        else if (PathUtils.exists(path + ".nii.gz"))
        {
            return Volume.read(path + ".nii.gz");
        }
        else
        {
            return null;
        }
    }

    public static void write(Volume volume, String path) throws IOException
    {
        if (path.endsWith("noddi"))
        {
            writeDirectory(volume, path);
        }
        else if (path.endsWith("nii.gz") || path.endsWith("nii"))
        {
            volume.write(path);
        }
        else
        {
            writeBasename(volume, path);
        }
    }

    public static void writeDirectory(Volume volume, String dirname) throws IOException
    {
        File file = new File(dirname).getAbsoluteFile();
        if (!file.exists())
        {
            file.mkdir();
        }
        String basename = new File(dirname, "noddi").getAbsolutePath();
        writeBasename(volume, basename);
    }

    public static void writeBasename(Volume volume, String basename) throws IOException
    {
        String[] features = {Noddi.DIRX, Noddi.DIRY, Noddi.DIRZ, Noddi.ODI, Noddi.FICVF, Noddi.FISO, Noddi.IRFRAC, Noddi.BASELINE};
        for (String name : features)
        {
            Logging.info("writing feature: " + name);
            VolumeModelFeature feature = new VolumeModelFeature();
            feature.input = volume;
            feature.model = ModelType.Noddi.name();
            feature.feature = name;
            Volume feat = feature.run().output;
            String fn = String.format("%s_%s.nii.gz", basename, name);
            feat.write(fn);
        }
    }
}
