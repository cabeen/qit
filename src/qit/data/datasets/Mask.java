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

package qit.data.datasets;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.formats.volume.*;
import qit.data.source.MaskSource;
import qit.data.source.TableSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.TableUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * a volumetric mask that can take a discrete value at each voxel
 */
public class Mask implements Dataset
{
    private Sampling sampling;
    private int[] labels;

    private Map<Integer, String> lookup = Maps.newHashMap();

    protected Mask()
    {
    }

    public Mask(Sampling sampling)
    {
        this.sampling = sampling;
        this.labels = new int[sampling.size()];
    }

    public Mask copy()
    {
        Mask nvol = new Mask(this.getSampling());
        nvol.set(this);
        return nvol;
    }

    public Mask copy(Sampling sampling)
    {
        Mask out = this.proto(sampling);

        Sampling fsampling = this.getSampling();
        for (Sample sample : sampling)
        {
            Sample fsample = fsampling.nearest(sampling.world(sample));
            if (fsampling.contains(fsample))
            {
                out.set(sample, this.get(fsample));
            }
        }

        return out;
    }

    public Mask proto()
    {
        return new Mask(this.getSampling());
    }

    public Mask proto(Sampling sampling)
    {
        return new Mask(sampling);
    }

    public Sampling getSampling()
    {
        return this.sampling;
    }

    public void set(Sample sample, int label)
    {
        int idx = this.sampling.index(sample);
        this.labels[idx] = label;
    }

    public void set(int idx, int label)
    {
        this.labels[idx] = label;
    }

    public boolean foreground(Sample sample)
    {
        return this.get(sample) != 0;
    }

    public boolean foreground(int idx)
    {
        return get(idx) != 0;
    }

    public boolean foreground(int i, int j, int k)
    {
        return get(i, j, k) != 0;
    }

    public boolean background(Sample sample)
    {
        return get(sample) == 0;
    }

    public boolean background(int idx)
    {
        return get(idx) == 0;
    }

    public boolean background(int i, int j, int k)
    {
        return get(i, j, k) == 0;
    }

    public int get(Sample sample)
    {
        int idx = this.sampling.index(sample);
        return this.labels[idx];
    }

    public int get(int idx)
    {
        return this.labels[idx];
    }

    public int get(int i, int j, int k)
    {
        return this.get(new Sample(i, j, k));
    }

    public void set(int i, int j, int k, int label)
    {
        this.set(new Sample(i, j, k), label);
    }

    public int get(int[] idx)
    {
        return this.get(idx[0], idx[1], idx[2]);
    }

    public void set(int[] idx, int label)
    {
        this.set(idx[0], idx[1], idx[2], label);
    }

    public Vect vect()
    {
        return VectSource.create(this.labels);
    }

    public void set(Mask vol)
    {
        Global.assume(this.getSampling().equals(vol.getSampling()), "samplings do no match");
        System.arraycopy(vol.labels, 0, this.labels, 0, this.labels.length);
    }

    public void setAll(Mask mask, int label)
    {
        if (mask == null)
        {
            return;
        }

        Sampling maskSampling = mask.getSampling();
        Sampling thisSampling = this.getSampling();

        if (maskSampling.equals(thisSampling))
        {
            Logging.info("setting mask with identical sampling");

            for (Sample sample : maskSampling)
            {
                if (mask.foreground(sample))
                {
                    this.set(sample, label);
                }
            }
        }
        else
        {
            Logging.info("setting mask with different sampling");

            for (Sample thisSample : thisSampling)
            {
                Vect world = thisSampling.world(thisSample);
                Sample maskSample = maskSampling.nearest(world);
                if (maskSampling.contains(maskSample) && mask.foreground(maskSample))
                {
                    this.set(thisSample, label);
                }
            }
        }
    }

    public void setAll(int label)
    {
        for (int i = 0; i < this.labels.length; i++)
        {
            this.labels[i] = label;
        }
    }

    public boolean valid(Sample sample, Mask mask)
    {
        return this.sampling.contains(sample) && (mask == null || mask.foreground(sample));
    }

    public boolean valid(int idx, Mask mask)
    {
        return this.sampling.contains(idx) && (mask == null || mask.foreground(idx));
    }

    public boolean valid(int i, int j, int k, Mask mask)
    {
        return this.sampling.contains(i, j, k) && (mask == null || mask.foreground(i, j, k));
    }

    public boolean valid(Sample sample)
    {
        return this.sampling.contains(sample);
    }

    public boolean valid(int i, int j, int k)
    {
        return this.sampling.contains(i, j, k);
    }

    public Volume protoVolume()
    {
        return VolumeSource.create(sampling, 1);
    }

    public Volume protoVolume(int dim)
    {
        return VolumeSource.create(sampling, dim);
    }

    public Volume copyVolume()
    {
        Volume out = VolumeSource.create(sampling, 1);
        for (int i = 0; i < this.labels.length; i++)
        {
            out.set(i, 0, this.labels[i]);
        }
        return out;
    }

    public void setOrigin(double x, double y, double z)
    {
        double dx = this.sampling.deltaI();
        double dy = this.sampling.deltaJ();
        double dz = this.sampling.deltaK();
        int nx = this.sampling.numI();
        int ny = this.sampling.numJ();
        int nz = this.sampling.numK();

        Vect start = VectSource.create3D(x, y, z);
        Vect delta = VectSource.create3D(dx, dy, dz);
        Integers num = new Integers(nx, ny, nz);

        this.sampling = new Sampling(start, delta, num);
    }

    public Set<Integer> getDefinedLabels()
    {
        return Sets.newHashSet(this.lookup.keySet());
    }

    public boolean hasLabel(int label)
    {
        return this.lookup.containsKey(label);
    }

    public void setName(int label, String name)
    {
        this.lookup.put(label, name);
    }

    public boolean hasName(int label)
    {
        return this.lookup.keySet().contains(label);
    }

    public String getName(int label)
    {
        if (this.lookup.keySet().contains(label))
        {
            return this.lookup.get(label);
        }
        else
        {
            return "region" + label;
        }
    }

    public static String mapFilenameToLookup(String fn)
    {
        List<String> exts = Lists.newArrayList();
        exts.add("vtk.gz");
        exts.add("vtk");
        exts.add("nii.gz");
        exts.add("nii");

        for (String ext : exts)
        {
            if (fn.endsWith(ext))
            {
                return fn.replace(ext, "csv");
            }
        }

        return fn + ".csv";
    }

    public void addLookup(String fn)
    {
        try
        {
            this.addLookup(Table.read(fn));
        }
        catch (RuntimeException e)
        {
            Logging.info("warning: could not read table");
            e.printStackTrace();
        }
        catch (IOException e)
        {
            Logging.info("warning: could not read table");
            e.printStackTrace();
        }
    }

    public void addLookup(Table table)
    {
        try
        {
            if (table.hasField("name") && table.hasField("index"))
            {
                Map<String, String> lut = TableUtils.createStringLookup(table, "index", "name");

                for (String key : lut.keySet())
                {
                    this.setName(Integer.valueOf(key), lut.get(key));
                }
            }
        }
        catch (RuntimeException e)
        {
            Logging.info("warning: could not add names to mask from table");
            e.printStackTrace();
        }
    }

    public void write(String fn) throws IOException
    {
        Volume volume = this.copyVolume();
        if (NiftiVolumeCoder.matches(fn))
        {
            NiftiVolumeCoder.write(volume, fn, true);
        }
        else
        {
            volume.write(fn);
        }

        if (this.lookup.size() > 0)
        {
            Table table = new Table();
            table.addField("index");
            table.addField("name");

            for (int label : this.lookup.keySet())
            {
                Record record = new Record();
                record.with("index", String.valueOf(label));
                record.with("name", this.lookup.get(label));
                table.addRecord(record);
            }

            table.write(mapFilenameToLookup(fn));
        }
    }

    public static Mask read(String fn) throws IOException
    {
        Volume volume = null;

        if (VolumeVtkCoder.matches(fn))
        {
            volume = VolumeVtkCoder.read(fn);
        }
        else if (VolumeTextCoder.matches(fn))
        {
            volume = VolumeTextCoder.read(fn);
        }
        else if (VolumeStackCoder.matches(fn))
        {
            volume = VolumeStackCoder.read(fn);
        }
        else if (NiftiVolumeCoder.matches(fn))
        {
            volume = NiftiVolumeCoder.read(fn, true);
        }
        else if (BufferedImageVolumeCoder.matches(fn))
        {
            volume = BufferedImageVolumeCoder.read(fn);
        }
        else
        {
            volume = NiftiVolumeCoder.read(fn, true);
        }

        Mask out = MaskSource.discretize(volume);

        String lookupFn = mapFilenameToLookup(fn);
        if (PathUtils.exists(lookupFn))
        {
            out.addLookup(lookupFn);
        }

        return out;
    }

    public List<String> getExtensions()
    {
        return Lists.newArrayList(Volume.EXTS);
    }
}
