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
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.ModelType;
import qit.base.structs.DataType;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.formats.mri.VolumeBiTensorCoder;
import qit.data.formats.mri.VolumeMcsmtCoder;
import qit.data.formats.volume.*;
import qit.data.models.Spharm;
import qit.data.source.VectSource;
import qit.data.formats.mri.VolumeFibersCoder;
import qit.data.formats.mri.VolumeKurtosisCoder;
import qit.data.formats.mri.VolumeNoddiCoder;
import qit.data.formats.mri.VolumeTensorCoder;

import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferDouble;
import java.awt.image.DataBufferFloat;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class Volume implements Dataset
{
    final static List<String> EXTS = Lists.newArrayList(new String[]{"nii.gz", "nii", "vtk.gz", "vtk", "nrrd", "stack", "dti", "xfib", "noddi", "dki", "png", "txt"});

    private Sampling sampling;
    private DataBuffer data;
    private ModelType model = ModelType.Vect;

    protected Volume()
    {
    }

    public Volume(Volume volume)
    {
        this.sampling = volume.sampling;
        this.data = volume.data;
    }

    public Volume(Sampling sampling, DataType type, int dim)
    {
        Global.assume(dim >= 0, "invalid channel");

        this.sampling = sampling;

        int size = sampling.size();
        switch (type)
        {
            case BYTE:
                this.data = new DataBufferByte(size, dim);
                break;
            case DOUBLE:
                this.data = new DataBufferDouble(size, dim);
                break;
            case FLOAT:
                this.data = new DataBufferFloat(size, dim);
                break;
            case INT:
                this.data = new DataBufferInt(size, dim);
                break;
            case SHORT:
                this.data = new DataBufferShort(size, dim);
                break;
            case USHORT:
                this.data = new DataBufferUShort(size, dim);
                break;
            default:
                Logging.error("unsupported data type: " + type.toString());
        }
    }

    public ModelType getModel()
    {
        return this.model;
    }

    public Volume setModel(ModelType m)
    {
        this.model = m;

        return this;
    }

    public Volume copy()
    {
        Volume nvol = this.proto(this.getSampling(), this.getDim());
        nvol.set(this);
        return nvol;
    }

    public Volume copy(Sampling sampling)
    {
        Volume out = this.proto(sampling);

        Sampling fsampling = this.getSampling();
        for (Sample sample : sampling)
        {
            out.set(sample, this.get(fsampling.nearest(sampling.world(sample))));
        }

        out.model = this.model;

        return out;
    }

    public Volume copy(Sampling sampling, int dim)
    {
        Volume out = this.proto(sampling);

        Sampling fsampling = this.getSampling();
        for (Sample sample : sampling)
        {
            out.set(sample, dim, this.get(fsampling.nearest(sampling.world(sample)), dim));
        }

        if (dim == this.getDim())
        {
            out.model = this.model;
        }

        return out;
    }

    public Volume proto()
    {
        return this.proto(this.getSampling(), this.getDim());
    }

    public Volume proto(Sampling sampling)
    {
        return this.proto(sampling, this.getDim());
    }

    public Volume proto(int dim)
    {
        Global.assume(dim > 0, "invalid channel");

        return this.proto(this.getSampling(), dim);
    }

    public Volume proto(Sampling sampling, int dim)
    {
        Global.assume(dim > 0, "invalid channel");

        // the code below decides which datatype to use when prototyping
        // there's a global variable that indicates whether the input
        // data type should be preserved or whether it should be promoted
        // to a system default, e.g. double.  yes, i know global variables
        // are awful, but this avoids having to add a "use this datatype"
        // option to every module in the whole package

        Volume out = new Volume(sampling, Global.getDataType(this.getType()), dim);

        if (dim == this.getDim())
        {
            out.model = this.model;
        }

        return out;
    }

    public Volume getVolume(int dim)
    {
        Volume out = this.proto(1);
        for (Sample sample : this.sampling)
        {
            out.set(sample, 0, this.get(sample, dim));
        }

        return out;
    }

    public Sampling getSampling()
    {
        return this.sampling;
    }

    public void setSampling(Sampling v)
    {
        Global.assume(v.size() == this.sampling.size(), "invalid sampling");

        this.sampling = v;
    }

    public int getDim()
    {
        return this.data.getNumBanks();
    }

    public DataType getType()
    {
        switch (this.data.getDataType())
        {
            case DataBuffer.TYPE_BYTE:
                return DataType.BYTE;
            case DataBuffer.TYPE_DOUBLE:
                return DataType.DOUBLE;
            case DataBuffer.TYPE_FLOAT:
                return DataType.FLOAT;
            case DataBuffer.TYPE_INT:
                return DataType.INT;
            case DataBuffer.TYPE_SHORT:
                return DataType.SHORT;
            case DataBuffer.TYPE_USHORT:
                return DataType.USHORT;
            default:
                throw new RuntimeException("unrecognized image type");
        }
    }

    public void set(Sample sample, Vect input)
    {
        int idx = this.sampling.index(sample);
        this.set(idx, input);
    }

    public void set(int idx, Vect input)
    {
        for (int i = 0; i < this.data.getNumBanks(); i++)
        {
            this.data.setElemDouble(i, idx, input.get(i));
        }
    }

    public void set(int idx, int dim, double v)
    {
        this.data.setElemDouble(dim, idx, v);
    }

    public void set(int i, int j, int k, double v)
    {
        this.set(i, j, k, 0, v);
    }

    public void set(int idx, double v)
    {
        this.set(idx, 0, v);
    }

    public void set(Sample sample, double v)
    {
        this.set(sample, 0, v);
    }

    public void set(Sample sample, int dim, double v)
    {
        int idx = this.sampling.index(sample);
        this.data.setElemDouble(dim, idx, v);
    }

    public void get(Sample sample, Vect output)
    {
        int idx = this.sampling.index(sample);
        for (int i = 0; i < this.data.getNumBanks(); i++)
        {
            output.set(i, this.data.getElemDouble(i, idx));
        }
    }

    public void get(int idx, Vect output)
    {
        for (int i = 0; i < this.data.getNumBanks(); i++)
        {
            output.set(i, this.data.getElemDouble(i, idx));
        }
    }

    public Vect get(int idx)
    {
        Vect output = new Vect(this.getDim());
        for (int i = 0; i < this.data.getNumBanks(); i++)
        {
            output.set(i, this.data.getElemDouble(i, idx));
        }
        return output;
    }

    public double get(int idx, int dim)
    {
        return this.data.getElemDouble(dim, idx);
    }

    public Vect get(Sample sample)
    {
        Vect output = new Vect(this.getDim());
        int idx = this.sampling.index(sample);
        for (int i = 0; i < this.data.getNumBanks(); i++)
        {
            output.set(i, this.data.getElemDouble(i, idx));
        }
        return output;
    }

    public double get(Sample sample, int dim)
    {
        int idx = this.sampling.index(sample);
        if (idx < 0)
        {
            Logging.error("test");
        }
        return this.data.getElemDouble(dim, idx);
    }

    public Vect get(int i, int j, int k)
    {
        return this.get(new Sample(i, j, k));
    }

    public double get(int i, int j, int k, int dim)
    {
        return this.get(new Sample(i, j, k), dim);
    }

    public void get(int i, int j, int k, Vect output)
    {
        this.get(new Sample(i, j, k), output);
    }

    public void set(int i, int j, int k, int dim, double v)
    {
        this.set(new Sample(i, j, k), dim, v);
    }

    public void set(int i, int j, int k, Vect v)
    {
        this.set(new Sample(i, j, k), v);
    }

    public double get(int[] idx, int dim)
    {
        return this.get(idx[0], idx[1], idx[2], dim);
    }

    public Vect get(int[] idx)
    {
        return this.get(idx[0], idx[1], idx[2]);
    }

    public void set(int[] idx, int dim, double v)
    {
        this.set(idx[0], idx[1], idx[2], dim, v);
    }

    public void get(int[] idx, Vect v)
    {
        this.get(idx[0], idx[1], idx[2], v);
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

    public Vect dproto()
    {
        return new Vect(this.getDim());
    }

    public void set(Volume v)
    {
        Global.assume(this.getSampling().equals(v.getSampling()), "samplings do no match");
        Global.assume(this.getDim() == v.getDim(), "dimensions do not match");

        copy(v.data, this.data);
        this.model = v.model;
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

    public void setDelta(double dx, double dy, double dz)
    {
        double sx = this.sampling.startI();
        double sy = this.sampling.startJ();
        double sz = this.sampling.startK();
        int nx = this.sampling.numI();
        int ny = this.sampling.numJ();
        int nz = this.sampling.numK();

        Vect start = VectSource.create3D(sx, sy, sz);
        Vect delta = VectSource.create3D(dx, dy, dz);
        Integers num = new Integers(nx, ny, nz);

        this.sampling = new Sampling(start, delta, num);
    }

    private static void copy(DataBuffer from, DataBuffer to)
    {
        for (int i = 0; i < from.getNumBanks(); i++)
        {
            for (int j = 0; j < from.getSize(); j++)
            {
                to.setElemDouble(i, j, from.getElemDouble(i, j));
            }
        }
    }

    public void set(Mask mask, Vect v)
    {
        for (Sample s : this.sampling)
        {
            if (this.valid(s, mask))
            {
                this.set(s, v);
            }
        }
    }

    public void setAll(Mask mask, Vect v)
    {
        for (Sample s : this.getSampling())
        {
            if (this.valid(s, mask))
            {
                this.set(s, v);
            }
        }
    }

    public void setAll(Vect v)
    {
        for (Sample s : this.getSampling())
        {
            this.set(s, v);
        }
    }

    public void setVolume(int idx, Volume sub)
    {
        for (Sample s : this.getSampling())
        {
            this.set(s, idx, sub.get(s, 0));
        }
    }

    public Vect vect()
    {
        return VectSource.create(this.data);
    }

    public void write(String fn) throws IOException
    {
        PathUtils.mkpar(fn);

        if (VolumeBiTensorCoder.matches(fn))
        {
            VolumeBiTensorCoder.write(this, fn);
        }
        else if (VolumeTensorCoder.matches(fn))
        {
            VolumeTensorCoder.write(this, fn);
        }
        else if (VolumeFibersCoder.matches(fn))
        {
            VolumeFibersCoder.write(this, fn);
        }
        else if (VolumeNoddiCoder.matches(fn))
        {
            VolumeNoddiCoder.write(this, fn);
        }
        else if (VolumeKurtosisCoder.matches(fn))
        {
            VolumeKurtosisCoder.write(this, fn);
        }
        else if (VolumeMcsmtCoder.matches(fn))
        {
            VolumeMcsmtCoder.write(this, fn);
        }
        else
        {
            this.writeRaw(fn);
        }
    }

    public void writeRaw(String fn) throws IOException
    {
        if (VolumeVtkCoder.matches(fn))
        {
            VolumeVtkCoder.write(this, new FileOutputStream(fn));
        }
        else if (NrrdVolumeCoder.matches(fn))
        {
            NrrdVolumeCoder.write(this, new FileOutputStream(fn));
        }
        else if (VolumeTextCoder.matches(fn))
        {
            VolumeTextCoder.write(this, fn);
        }
        else if (VolumeStackCoder.matches(fn))
        {
            VolumeStackCoder.write(this, fn);
        }
        else if (NiftiVolumeCoder.matches(fn))
        {
            NiftiVolumeCoder.write(this, fn);
        }
        else if (TiffVolumeCoder.matches(fn))
        {
            TiffVolumeCoder.write(this, fn);
        }
        else if (BufferedImageVolumeCoder.matches(fn))
        {
            BufferedImageVolumeCoder.write(this, fn);
        }
        else
        {
            // use nifti if the filename extension was not recognized
            NiftiVolumeCoder.write(this, fn);
        }
    }

    @Override
    public List<String> getExtensions()
    {
        return EXTS;
    }

    public static Volume read(String fn) throws IOException
    {
        Global.assume(PathUtils.exists(fn), "file not found: " + fn);
        String bn = PathUtils.basename(fn);

        if (VolumeTensorCoder.matches(bn))
        {
            Volume out = VolumeTensorCoder.read(fn);
            out.setModel(ModelType.Tensor);
            return out;
        }
        else if (VolumeBiTensorCoder.matches(bn))
        {
            Volume out = VolumeBiTensorCoder.read(fn);
            out.setModel(ModelType.BiTensor);
            return out;
        }
        else if (VolumeFibersCoder.matches(bn))
        {
            Volume out = VolumeFibersCoder.read(fn);
            out.setModel(ModelType.Fibers);
            return out;
        }
        else if (VolumeNoddiCoder.matches(bn))
        {
            Volume out = VolumeNoddiCoder.read(fn);
            out.setModel(ModelType.Noddi);
            return out;
        }
        else if (Spharm.matches(bn))
        {
            Volume out = NiftiVolumeCoder.read(fn);
            out.setModel(ModelType.Spharm);
            return out;
        }
        else if (VolumeKurtosisCoder.matches(bn))
        {
            Volume out = VolumeKurtosisCoder.read(fn);
            out.setModel(ModelType.Kurtosis);
            return out;
        }
        else if (VolumeMcsmtCoder.matches(bn))
        {
            Volume out = VolumeMcsmtCoder.read(fn);
            out.setModel(ModelType.Mcsmt);
            return out;
        }
        else
        {
            return readRaw(fn);
        }
    }

    public static Volume readRaw(String fn) throws IOException
    {
        Global.assume(PathUtils.exists(fn), "file not found: " + fn);
        String bn = PathUtils.basename(fn);

        if (VolumeVtkCoder.matches(bn))
        {
            return VolumeVtkCoder.read(fn);
        }
        else if (VolumeTextCoder.matches(bn))
        {
            return VolumeTextCoder.read(fn);
        }
        else if (MghVolumeCoder.matches(bn))
        {
            return MghVolumeCoder.read(fn);
        }
        else if (VolumeStackCoder.matches(bn))
        {
            return VolumeStackCoder.read(fn);
        }
        else if (NiftiVolumeCoder.matches(bn))
        {
            return NiftiVolumeCoder.read(fn);
        }
        else if (TiffVolumeCoder.matches(bn))
        {
            return TiffVolumeCoder.read(fn);
        }
        else if (BufferedImageVolumeCoder.matches(bn))
        {
            return BufferedImageVolumeCoder.read(fn);
        }
        else
        {
            return NiftiVolumeCoder.read(fn); // use nifti if the filename extension was not recognized
        }
    }
}