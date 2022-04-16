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


package qit.data.formats.mri;

import qit.base.Global;
import qit.base.Logging;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.models.Fibers;
import qit.data.models.Noddi;
import qit.data.models.Tensor;
import qit.data.utils.volume.VolumeFunction;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

import java.io.File;
import java.io.IOException;

public class VolumeFibersCoder
{
    public static boolean matches(String fn)
    {
        return Fibers.matches(PathUtils.basename(fn));
    }

    public static Volume read(String path) throws IOException
    {
        if (PathUtils.isDir(path))
        {
            // check if the input is a tensor volume
            if (new File(path, "dti_V1.nii.gz").exists())
            {
                Logging.info("reading fibers data from dti");
                return readTensor(path);
            }
            else if (new File(path + "_V1.nii.gz").exists())
            {
                Logging.info("reading fibers data from dti");
                return readTensor(new File(path).getAbsoluteFile().getParent());
            }
            else if (new File(path, "noddi_ICVF.nii.gz").exists())
            {
                Logging.info("reading fibers data from noddi");
                return readNoddi(path);
            }
            else if (new File(path + "_ICVF.nii.gz").exists())
            {
                Logging.info("reading fibers data from noddi");
                return readNoddi(new File(path).getAbsoluteFile().getParent());
            }
            else if (new File(path, "dir1.nii.gz").exists())
            {
                Logging.info("reading fibers data from fom");
                return readFom(path);
            }
            else
            {
                Logging.info("reading fibers data from bedpost");
                return readXfibres(path);
            }
        }
        else if (path.contains("peaks"))
        {
            Logging.info("reading fibers data from peaks");
            return readPeaks(path);
        }
        else if (PathUtils.exists(path))
        {
            Logging.info("reading fibers data from axes");
            return readAxes(path);
        }
        else
        {
            throw new RuntimeException("file not found: " + path);
        }
    }

    public static void write(Volume volume, String path) throws IOException
    {
        if (path.endsWith("fom"))
        {
            Logging.info("writing fibers to fom (this may lose information)");
            writeFom(volume, path);
        }
        else if (path.contains("peaks"))
        {
            // this is lossy!
            Logging.info("writing fibers to peaks (this may lose information)");
            writePeaks(volume, path);
        }
        else
        {
            Logging.info("writing fibers to bedpost");
            writeXfibers(volume, path);
        }
    }

    public static Volume readAxes(String fn) throws IOException
    {
        Logging.progress("reading from axes");
        Volume axes = Volume.readRaw(fn);
        Volume out = VolumeSource.create(axes.getSampling().copy(), new Fibers(1).getEncodingSize());

        for (Sample sample : out.getSampling())
        {
            Vect pd = axes.get(sample);

            Fibers fibers = new Fibers(out.get(sample));
            fibers.setBaseline(0);
            fibers.setDiffusivity(0);
            fibers.setFrac(0, 1);
            fibers.setLine(0, pd);

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }

    public static Volume readTensor(String dirname) throws IOException
    {
        Volume dti = VolumeTensorCoder.read(dirname);
        Volume out = VolumeSource.create(dti.getSampling().copy(), new Fibers(1).getEncodingSize());

        for (Sample sample : out.getSampling())
        {
            Tensor tensor = new Tensor(dti.get(sample));
            double s0 = tensor.feature(Tensor.FEATURES_S0).get(0);
            double fa = tensor.feature(Tensor.FEATURES_FA).get(0);
            double md = tensor.feature(Tensor.FEATURES_MD).get(0);
            Vect pd = tensor.getVec(0);

            Fibers fibers = new Fibers(out.get(sample));
            fibers.setBaseline(s0);
            fibers.setDiffusivity(md);
            fibers.setFrac(0, fa);
            fibers.setLine(0, pd);

            out.set(sample, fibers.getEncoding());
        }

        dti = null;
        System.gc();
        System.gc();

        return out;
    }


    public static Volume readNoddi(String dirname) throws IOException
    {
        Volume noddi = VolumeNoddiCoder.read(dirname);
        Volume out = VolumeSource.create(noddi.getSampling().copy(), new Fibers(1).getEncodingSize());

        for (Sample sample : out.getSampling())
        {
            Noddi model = new Noddi(noddi.get(sample));
            double icvf = model.feature(Noddi.FICVF).get(0);
            double od = model.feature(Noddi.ODI).get(0);
            Vect dir = model.getDir();

            Fibers fibers = new Fibers(out.get(sample));
            fibers.setBaseline(icvf);
            fibers.setDiffusivity(od);
            fibers.setFrac(0, icvf);
            fibers.setLine(0, dir);

            out.set(sample, fibers.getEncoding());
        }

        noddi = null;
        System.gc();
        System.gc();

        return out;
    }

    public static Volume readXfibres(String dirname) throws IOException
    {
        int comps = 1;
        while (new File(dirname, String.format("mean_f%dsamples.nii.gz", comps + 1)).exists())
        {
            comps++;
        }

        Global.assume(comps != 0, "no fibers found");

        String baseFn = new File(dirname, "mean_S0samples.nii.gz").getAbsolutePath();
        Volume base = PathUtils.exists(baseFn) ? Volume.readRaw(baseFn) : null;

        String diffFn = new File(dirname, "mean_dsamples.nii.gz").getAbsolutePath();
        Volume diff = PathUtils.exists(diffFn) ? Volume.readRaw(diffFn) : null;

        Volume[] dirs = new Volume[comps];
        Volume[] fracs = new Volume[comps];
        Volume[] stats = new Volume[comps];
        Mask[] labels = new Mask[comps];

        for (int idx = 0; idx < comps; idx++)
        {
            String dirFn = new File(dirname, String.format("dyads%d.nii.gz", idx + 1)).getAbsolutePath();
            String fracFn = new File(dirname, String.format("mean_f%dsamples.nii.gz", idx + 1)).getAbsolutePath();
            String statFn = new File(dirname, String.format("stat%d.nii.gz", idx + 1)).getAbsolutePath();
            String labelFn = new File(dirname, String.format("label%d.nii.gz", idx + 1)).getAbsolutePath();

            fracs[idx] = PathUtils.exists(fracFn) ? Volume.readRaw(fracFn) : null;
            stats[idx] = PathUtils.exists(statFn) ? Volume.readRaw(statFn) : null;
            labels[idx] = PathUtils.exists(labelFn) ? Mask.read(labelFn) : null;

            // sometimes bedpost does not save the dyads file, so we may have to look for the mean phi and theta
            String phFn = new File(dirname, String.format("mean_ph%dsamples.nii.gz", idx + 1)).getAbsolutePath();
            String thFn = new File(dirname, String.format("mean_th%dsamples.nii.gz", idx + 1)).getAbsolutePath();

            if (PathUtils.exists(dirFn))
            {
                dirs[idx] = Volume.readRaw(dirFn);
            }
            else if (PathUtils.exists(phFn) && PathUtils.exists(thFn))
            {
                Volume ph = Volume.readRaw(phFn);
                Volume th = Volume.readRaw(thFn);

                Volume dir = ph.proto(3);
                for (Sample sample : dir.getSampling())
                {
                    double theta = th.get(sample, 0);
                    double phi = ph.get(sample, 0);

                    double st = Math.sin(theta);
                    double sp = Math.sin(phi);
                    double ct = Math.cos(theta);
                    double cp = Math.cos(phi);

                    double x = st * cp;
                    double y = st * sp;
                    double z = ct;

                    dir.set(sample, VectSource.create3D(x, y, z));
                }

                dirs[idx] = dir;
            }
        }

        Volume ref = base != null ? base : diff != null ? diff : dirs[0];
        Sampling sampling = ref.getSampling();
        Volume out = VolumeSource.create(sampling, new Fibers(comps).getEncodingSize());
        for (Sample sample : out.getSampling())
        {
            Fibers fibers = new Fibers(comps);

            fibers.setBaseline(base == null ? 1.0 : base.get(sample, 0));
            fibers.setDiffusivity(diff == null ? 1.0 : diff.get(sample, 0));

            for (int i = 0; i < comps; i++)
            {
                Volume dir = dirs[i];
                Volume frac = fracs[i];
                Volume stat = stats[i];
                Mask label = labels[i];

                fibers.setLine(i, dir == null ? VectSource.create3D() : dir.get(sample));
                fibers.setFrac(i, frac == null ? 0 : frac.get(sample, 0));
                fibers.setStat(i, stat == null ? 0 : stat.get(sample, 0));
                fibers.setLabel(i, label == null ? 0 : label.get(sample));
            }

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }

    public static void writeXfibers(Volume volume, String dirname) throws IOException
    {
        File dir = new File(dirname).getAbsoluteFile();
        if (!dir.exists())
        {
            dir.mkdir();
        }


        int nf = Fibers.count(volume.getDim());

        for (String id : new String[]{"dyads%s.nii.gz", "mean_f%ssamples.nii.gz"})
        {
            for (int idx = 0; idx < nf; idx++)
            {
                String fn = new File(dirname, String.format(id, idx + 1)).getAbsolutePath();
                int dim = id.startsWith("dyads") ? 3 : 1;
                Volume elem = VolumeSource.create(volume.getSampling(), dim);
                boolean dyad = id.startsWith("dyads");

                for (Sample sample : elem.getSampling())
                {
                    Fibers fibers = new Fibers(volume.get(sample));
                    if (dyad)
                    {
                        elem.set(sample, fibers.getLine(idx));
                    }
                    else
                    {
                        elem.set(sample, 0, fibers.getFrac(idx));
                    }
                }

                elem.write(fn);
            }
        }

        Volume s0 = volume.proto(1);
        Volume d = volume.proto(1);
        Volume fsum = volume.proto(1);
        Volume fiso = volume.proto(1);

        for (Sample sample : volume.getSampling())
        {
            Fibers fibers = new Fibers(volume.get(sample));
            s0.set(sample, 0, fibers.getBaseline());
            d.set(sample, 0, fibers.getDiffusivity());
            fiso.set(sample, 0, fibers.getFracIso());
            fsum.set(sample, 0, 1.0 - fibers.getFracIso());
        }

        s0.write(new File(dirname, "mean_S0samples.nii.gz").getAbsolutePath());
        d.write(new File(dirname, "mean_dsamples.nii.gz").getAbsolutePath());
        fsum.write(new File(dirname, "fsum.nii.gz").getAbsolutePath());
        fiso.write(new File(dirname, "fiso.nii.gz").getAbsolutePath());
    }

    public static Volume readPeaks(String fn) throws IOException
    {
        Volume peaks = Volume.readRaw(fn);

        Global.assume(peaks.getDim() % 3 == 0, "peak volume dimension must be a multiple of three");

        int num = peaks.getDim() / 3;

        Sampling sampling = peaks.getSampling();
        Volume out = VolumeSource.create(sampling, new Fibers(num).getEncodingSize());

        for (Sample sample : sampling)
        {
            Vect value = peaks.get(sample);
            Fibers fibers = new Fibers(num);

            for (int i = 0; i < num; i++)
            {
                Vect peak = value.sub(3 * i, 3 * (i + 1));

                double frac = peak.norm();
                Vect line = peak.divSafe(frac);

                fibers.setFrac(i, frac);
                fibers.setLine(i, line);
            }

            // add this for visualization purposes
            double fsum = fibers.getFracSum();
            fibers.setBaseline(fsum);

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }

    public static Volume readFom(String dirname) throws IOException
    {
        int comps = 1;
        while (new File(dirname, String.format("dir%d.nii.gz", comps + 1)).exists())
        {
            comps++;
        }

        Global.assume(comps != 0, "no fibers found");

        String baseFn = new File(dirname, "baseline.nii.gz").getAbsolutePath();
        Volume base = PathUtils.exists(baseFn) ? Volume.readRaw(baseFn) : null;

        String diffFn = new File(dirname, "diff.nii.gz").getAbsolutePath();
        Volume diff = PathUtils.exists(diffFn) ? Volume.readRaw(diffFn) : null;

        Volume[] dirs = new Volume[comps];
        Volume[] fracs = new Volume[comps];

        for (int idx = 0; idx < comps; idx++)
        {
            String dirFn = new File(dirname, String.format("dir%s.nii.gz", idx + 1)).getAbsolutePath();
            String fracFn = new File(dirname, String.format("frac%s.nii.gz", idx + 1)).getAbsolutePath();

            dirs[idx] = Volume.readRaw(dirFn);
            fracs[idx] = PathUtils.exists(fracFn) ? Volume.readRaw(fracFn) : null;
        }

        Sampling sampling = dirs[0].getSampling();
        Volume out = VolumeSource.create(sampling, new Fibers(comps).getEncodingSize());
        for (Sample sample : out.getSampling())
        {
            Fibers fibers = new Fibers(comps);

            fibers.setBaseline(base == null ? 1.0 : base.get(sample, 0));
            fibers.setDiffusivity(diff == null ? 1.0 : diff.get(sample, 0));

            for (int i = 0; i < comps; i++)
            {
                Volume dir = dirs[i];
                Volume frac = fracs[i];

                fibers.setLine(i, dir.get(sample));
                fibers.setFrac(i, frac == null ? 0 : frac.get(sample, 0));
            }

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }

    public static void writeFom(Volume volume, String dirname) throws IOException
    {
        File dir = new File(dirname).getAbsoluteFile();
        if (!dir.exists())
        {
            dir.mkdir();
        }

        int nf = Fibers.count(volume.getDim());

        for (String id : new String[]{"dir%s.nii.gz", "frac%s.nii.gz"})
        {
            for (int idx = 0; idx < nf; idx++)
            {
                String fn = new File(dirname, String.format(id, idx + 1)).getAbsolutePath();
                int dim = id.startsWith("dir") ? 3 : 1;
                Volume elem = VolumeSource.create(volume.getSampling(), dim);
                boolean dyad = id.startsWith("dir");

                for (Sample sample : elem.getSampling())
                {
                    Fibers fibers = new Fibers(volume.get(sample));
                    if (dyad)
                    {
                        elem.set(sample, fibers.getLine(idx));
                    }
                    else
                    {
                        elem.set(sample, 0, fibers.getFrac(idx));
                    }
                }

                elem.write(fn);
            }
        }
    }

    public static void writePeaks(Volume volume, String fn) throws IOException
    {
        if (!Fibers.valid(volume.getDim()))
        {
            volume.writeRaw(fn);
        }
        else
        {
            int num = Fibers.count(volume.getDim());
            Volume out = volume.proto(3 * num);

            for (Sample sample : volume.getSampling())
            {
                Fibers model = new Fibers(volume.get(sample));
                Vect peaks = out.dproto();

                for (int i = 0; i < num; i++)
                {
                    double frac = model.getFrac(i);
                    Vect line = model.getLine(i);
                    Vect peak = line.times(frac);

                    peaks.set(3 * i, peak);
                }

                out.set(sample, peaks);
            }

            out.writeRaw(fn);
        }
    }
}
