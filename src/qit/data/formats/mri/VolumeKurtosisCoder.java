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
import qit.base.utils.PathUtils;
import qit.data.datasets.Sample;
import qit.data.datasets.Volume;
import qit.data.models.Kurtosis;
import qit.data.modules.mri.model.VolumeModelFeature;

import java.io.File;
import java.io.IOException;

public class VolumeKurtosisCoder
{
    public static boolean matches(String path)
    {
        return Kurtosis.matches(PathUtils.basename(path)) || PathUtils.exists(path + "_params.nii.gz");
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
        Volume dt = readSafe(String.format("%s_%s.nii.gz", basename, "dt"));
        Volume b0 = readSafeNull(String.format("%s_%s.nii.gz", basename, "b0"));
        Volume fa = readSafeNull(String.format("%s_%s.nii.gz", basename, "fa"));
        Volume md = readSafeNull(String.format("%s_%s.nii.gz", basename, "md"));
        Volume ad = readSafeNull(String.format("%s_%s.nii.gz", basename, "ad"));
        Volume rd = readSafeNull(String.format("%s_%s.nii.gz", basename, "rd"));
        Volume fe = readSafeNull(String.format("%s_%s.nii.gz", basename, "fe"));
        Volume mk = readSafeNull(String.format("%s_%s.nii.gz", basename, "mk"));
        Volume ak = readSafeNull(String.format("%s_%s.nii.gz", basename, "ak"));
        Volume rk = readSafeNull(String.format("%s_%s.nii.gz", basename, "rk"));
        Volume awf = readSafeNull(String.format("%s_%s.nii.gz", basename, "awf"));
        Volume eas = readSafeNull(String.format("%s_%s.nii.gz", basename, "eas"));
        Volume ias = readSafeNull(String.format("%s_%s.nii.gz", basename, "ias"));

        Kurtosis proto = new Kurtosis();
        Volume out = dt.proto(proto.getEncodingSize());

        for (Sample sample : out.getSampling())
        {
            Kurtosis model = new Kurtosis();
            model.dt = dt.get(sample);
            model.b0 = b0.get(sample, 0);
            model.fa = fa == null ? 0 : fa.get(sample, 0);
            model.md = md == null ? 0 : md.get(sample, 0);
            model.ad = ad == null ? 0 : ad.get(sample, 0);
            model.rd = rd == null ? 0 : rd.get(sample, 0);
            model.fe = fe == null ? 0 : fe.get(sample, 0);
            model.mk = mk == null ? 0 : mk.get(sample, 0);
            model.ak = ak == null ? 0 : ak.get(sample, 0);
            model.rk = rk == null ? 0 : rk.get(sample, 0);
            model.awf = awf == null ? 0 : awf.get(sample, 0);
            model.eas = eas == null ? 0 : eas.get(sample, 0);
            model.ias = ias == null ? 0 : ias.get(sample, 0);

            out.set(sample, model.getEncoding());
        }

        System.gc();
        System.gc();

        return out;
    }

    public static Volume readDirectory(String path) throws IOException
    {
        File file = new File(path).getAbsoluteFile();
        String basename = null;
        if (file.isDirectory())
        {
            // read a directory
            basename = new File(path, Kurtosis.NAME).getAbsolutePath();
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

    public static Volume readSafeNull(String path)
    {
        if (PathUtils.exists(path))
        {
            try
            {
                return readSafe(path);
            }
            catch (IOException e)
            {
                return null;
            }
        }
        else
        {
            return null;
        }
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
            throw new IOException("could not find nifti volume: " + path);
        }
    }

    public static void write(Volume volume, String path) throws IOException
    {
        if (path.endsWith(Kurtosis.NAME))
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
        String basename = new File(dirname, Kurtosis.NAME).getAbsolutePath();
        writeBasename(volume, basename);
    }

    public static void writeBasename(Volume volume, String basename) throws IOException
    {
        String[] features = Kurtosis.FEATURES;
        for (String name : features)
        {
            VolumeModelFeature feature = new VolumeModelFeature();
            feature.input = volume;
            feature.model = Kurtosis.NAME;
            feature.feature = name;
            Volume feat = feature.run().output;

            Logging.info("writing feature: " + name);
            String fn = String.format("%s_%s.nii.gz", basename, name.toLowerCase());
            feat.write(fn);
        }
    }
}
