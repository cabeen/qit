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


package qit.data.formats.volume;

import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.DataType;
import qit.base.structs.Integers;
import qit.base.structs.LEDataInputStream;
import qit.base.structs.LEDataOutputStream;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.math.structs.Quaternion;
import qit.math.utils.MathUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * a coder for the nifti volume file format
 */
public class NiftiVolumeCoder
{
    public static boolean SCALE = true;
    public static Map<Triple<Sampling, Integer, Boolean>, Pair<NiftiHeader, Function<Sample, Sample>>> CACHE = Maps.newHashMap();

    public static boolean matches(String fn)
    {
        return fn.endsWith("nii.gz") || fn.endsWith("nii");
    }

    public static Volume read(String fn) throws IOException
    {
        return read(fn, false);
    }

    public static void copyHeader(String inputFn, String refFn, String outputFn) throws IOException
    {
        Logging.info("reading reference");
        NiftiHeader refHeader = NiftiHeader.read(refFn);
        Pair<Volume, Function<Sample, Sample>> refInit = init(refHeader);
        readData(refHeader, refInit.a, refInit.b);

        Logging.info("reading input");
        NiftiHeader inputHeader = NiftiHeader.read(inputFn);
        Pair<Volume, Function<Sample, Sample>> inputInit = init(inputHeader);
        readData(inputHeader, inputInit.a, inputInit.b);

        Logging.info("writing output");
        NiftiHeader header = refHeader;
        Function<Sample, Sample> permutation = refInit.b;

        int dim = inputInit.a.getDim();
        if (dim != refInit.a.getDim())
        {
            header.dim[0] = (short) (dim > 1 ? 4 : 3);
            header.dim[4] = (short) (dim > 1 ? dim : 0);
        }
        header.filename = outputFn;
        header.descrip = new StringBuffer("Created: " + new Date().toString());
        writeData(header, inputInit.a, permutation);
    }

    public static Volume read(String fn, boolean mask) throws IOException
    {
        NiftiHeader hdr = NiftiHeader.read(fn);
        Pair<Volume, Function<Sample, Sample>> init = init(hdr);
        Volume volume = init.a;
        Function<Sample, Sample> permutation = init.b;

        readData(hdr, volume, permutation);
        CACHE.put(Triple.of(volume.getSampling(), volume.getDim(), mask), Pair.of(hdr, permutation));

        return volume;
    }

    public static void write(Volume volume, String fn) throws IOException
    {
        write(volume, fn, false);
    }

    public static void write(Volume volume, String fn, boolean mask) throws IOException
    {
        Pair<NiftiHeader, Function<Sample, Sample>> pair = getHeader(volume, mask);

        NiftiHeader header = pair.a;
        Function<Sample, Sample> permutation = pair.b;

        int dim = volume.getDim();
        header.dim[0] = (short) (dim > 1 ? 4 : 3);
        header.dim[4] = (short) (dim > 1 ? dim : 0);
        for (int i = 5; i < header.dim.length; i++)
        {
            header.dim[i] = 1;
        }

        if (dim == 1 || Global.getNoIntent())
        {
            header.intent_code = NiftiHeader.NIFTI_INTENT_NONE;
        }
        else
        {
            header.intent_code = NiftiHeader.NIFTI_INTENT_VECTOR;
        }

        header.scl_inter = 0;
        header.scl_slope = 0;

        header.setDatatype(NiftiHeader.mapType(volume.getType()));

        header.filename = fn;
        header.descrip = new StringBuffer("Created: " + new Date().toString());

        writeData(header, volume, permutation);
    }

    public static Matrix permutation(NiftiHeader header)
    {
        // this method returns a volume for storing the imaging data and a function that maps
        // the nifti coordinates (in some arbitrary voxel order) to the RAS IJK order used by QIT

        int nx = header.dim[1];
        int ny = header.dim[2];
        int nz = header.dim[3];

        if (header.dim[0] == 2)
        {
            nz = 1;
        }

        if (header.qform_code == 0 && header.sform_code > 0)
        {
            Logging.info("warning: no qform was found.  sform may include affine components that are not used!");
        }

        String orig = header.orientation();
        int[] permuteAxes = {-1, -1, -1};
        for (int i = 0; i < 3; i++)
        {
            switch (orig.charAt(i))
            {
                case 'L':
                case 'R':
                    permuteAxes[i] = 0;
                    break;
                case 'P':
                case 'A':
                    permuteAxes[i] = 1;
                    break;
                case 'I':
                case 'S':
                    permuteAxes[i] = 2;
                    break;
                default:
                    throw new RuntimeException("invalid orientation string");
            }
        }

        for (int i = 0; i < 3; i++)
        {
            Global.assume(permuteAxes[i] != -1, "invalid orientation string");
        }

        String targ = "LPI";
        String comp = "RAS";

        int[] oldNum = {nx, ny, nz};

        int[][] xfm = new int[4][4];
        xfm[3][3] = 1;

        for (int fidx = 0; fidx < 3; fidx++)
        {
            char v = orig.charAt(fidx);
            int tidx = permuteAxes[fidx];

            if (v == targ.charAt(tidx))
            {
                xfm[tidx][fidx] = 1;
            }
            else if (v == comp.charAt(tidx))
            {
                xfm[tidx][fidx] = -1;
                xfm[tidx][3] = oldNum[fidx] - 1;
            }
        }

        return new Matrix(MathUtils.toDouble(xfm));
    }

    private static Pair<Volume, Function<Sample, Sample>> init(NiftiHeader header)
    {
        // this method returns a volume for storing the imaging data and a function that maps
        // the nifti coordinates (in some arbitrary voxel order) to the RAS IJK order used by QIT

        int nx = header.dim[1];
        int ny = header.dim[2];
        int nz = header.dim[3];
        int dim = header.dim[4];

        if (header.dim[0] == 5)
        {
            dim = header.dim[5];
        }

        if (header.dim[0] == 2)
        {
            nz = 1;
        }

        if (dim == 0)
        {
            dim = 1;
        }

        boolean rgb24 = header.datatype == NiftiHeader.NIFTI_TYPE_RGB24;
        if (rgb24)
        {
            dim = 3;
        }

        DataType dtype = NiftiHeader.mapType(header.datatype);

        if (!(MathUtils.unit(header.scl_slope) || MathUtils.zero(header.scl_slope)) || !MathUtils.zero(header.scl_inter))
        {
            Logging.info("promoting datatype to double due to detected intensity scaling");
            dtype = DataType.DOUBLE;
        }

        if (dtype.equals(DataType.DOUBLE) && Global.getDataType().equals(DataType.FLOAT))
        {
            Logging.info("warning: demoting double volume to float due to dtype flag");
            dtype = DataType.FLOAT;
        }

        if (header.qform_code == 0 && header.sform_code > 0)
        {
            Logging.info("warning: no qform was found.  sform may include affine components that are not used!");
        }

        String orig = header.orientation();
        int[] permuteAxes = {-1, -1, -1};
        for (int i = 0; i < 3; i++)
        {
            switch (orig.charAt(i))
            {
                case 'L':
                case 'R':
                    permuteAxes[i] = 0;
                    break;
                case 'P':
                case 'A':
                    permuteAxes[i] = 1;
                    break;
                case 'I':
                case 'S':
                    permuteAxes[i] = 2;
                    break;
                default:
                    throw new RuntimeException("invalid orientation string");
            }
        }

        for (int i = 0; i < 3; i++)
        {
            Global.assume(permuteAxes[i] != -1, "invalid orientation string");
        }

        String targ = "LPI";
        String comp = "RAS";

        Matrix qmat = new Matrix(header.mat44());

        double[] oldDelta = {header.pixdim[1], header.pixdim[2], header.pixdim[3]};
        int[] oldNum = {nx, ny, nz};

        int[][] xfm = new int[4][4];
        xfm[3][3] = 1;

        for (int fidx = 0; fidx < 3; fidx++)
        {
            char v = orig.charAt(fidx);
            int tidx = permuteAxes[fidx];

            if (v == targ.charAt(tidx))
            {
                xfm[tidx][fidx] = 1;
            }
            else if (v == comp.charAt(tidx))
            {
                xfm[tidx][fidx] = -1;
                xfm[tidx][3] = oldNum[fidx] - 1;
            }
        }

        int[][] invxfm = new int[4][4];
        invxfm[3][3] = 1;

        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                invxfm[i][j] = xfm[j][i];
            }
        }
        for (int i = 0; i < 3; i++)
        {
            int t = 0;
            for (int j = 0; j < 3; j++)
            {
                t -= invxfm[i][j] * xfm[j][3];
            }
            invxfm[i][3] = t;
        }

        Matrix mat44 = qmat.times(new Matrix(MathUtils.toDouble(invxfm)));
        Matrix mat33 = MatrixUtils.orthogonalize(mat44.sub(0, 2, 0, 2));
        Quaternion rotate = new Quaternion(mat33);

        int[] newNum = new int[3];
        double[] newDelta = new double[3];
        double[] newStart = new double[3];

        for (int i = 0; i < 3; i++)
        {
            newNum[permuteAxes[i]] = oldNum[i];
            newDelta[permuteAxes[i]] = oldDelta[i];
            newStart[i] = mat44.get(i, 3);
        }

        Vect start = VectSource.create3D(newStart[0], newStart[1], newStart[2]);
        Vect delta = VectSource.create3D(newDelta[0], newDelta[1], newDelta[2]);
        Integers num = new Integers(newNum);
        Sampling newSampling = new Sampling(start, delta, rotate, num);

        Volume volume = new Volume(newSampling, dtype, dim);
        Function<Sample, Sample> permutation = s ->
        {
            int[] oldVoxel = new int[3];
            int[] newVoxel = new int[3];

            oldVoxel[0] = s.getI();
            oldVoxel[1] = s.getJ();
            oldVoxel[2] = s.getK();

            for (int tidx = 0; tidx < 3; tidx++)
            {
                newVoxel[tidx] = 0;
                for (int fidx = 0; fidx < 3; fidx++)
                {
                    newVoxel[tidx] += xfm[tidx][fidx] * oldVoxel[fidx];
                }
                newVoxel[tidx] += xfm[tidx][3];
            }

            return new Sample(newVoxel);
        };

        return Pair.of(volume, permutation);
    }

    public static Volume read(NiftiHeader hdr) throws IOException
    {
        Pair<Volume, Function<Sample, Sample>> init = NiftiVolumeCoder.init(hdr);
        return NiftiVolumeCoder.readData(hdr, init.a, init.b);
    }

    private static Volume readData(NiftiHeader hdr, Volume volume, Function<Sample, Sample> permutation) throws IOException
    {
        FileInputStream fis = new FileInputStream(hdr.filename);
        InputStream is = fis;

        if (hdr.filename.endsWith(".gz"))
        {
            is = new GZIPInputStream(is);
        }

        is = new BufferedInputStream(is);

        is.skip((long) hdr.vox_offset);

        int nx = hdr.dim[1];
        int ny = hdr.dim[2];
        int nz = hdr.dim[3];
        int dim = hdr.dim[4];

        if (hdr.dim[0] == 5)
        {
            dim = hdr.dim[5];
        }

        if (hdr.dim[0] == 2)
        {
            nz = 1;
        }

        if (dim == 0)
        {
            dim = 1;
        }

        boolean rgb24 = hdr.datatype == NiftiHeader.NIFTI_TYPE_RGB24;
        if (rgb24)
        {
            dim = 3;
        }

        DataInput di = hdr.little_endian ? new LEDataInputStream(is) : new DataInputStream(is);

        if (rgb24)
        {
            for (int k = 0; k < nz; k++)
            {
                for (int j = 0; j < ny; j++)
                {
                    for (int i = 0; i < nx; i++)
                    {
                        double r = ((double) di.readUnsignedByte()) / 255d;
                        double g = ((double) di.readUnsignedByte()) / 255d;
                        double b = ((double) di.readUnsignedByte()) / 255d;

                        Sample sample = permutation.apply(new Sample(i, j, k));
                        volume.set(sample, 0, r);
                        volume.set(sample, 1, g);
                        volume.set(sample, 2, b);
                    }
                }
            }
        }
        else
        {
            for (int d = 0; d < dim; d++)
            {
                for (int k = 0; k < nz; k++)
                {
                    for (int j = 0; j < ny; j++)
                    {
                        for (int i = 0; i < nx; i++)
                        {
                            double v;
                            switch (hdr.datatype)
                            {
                                case NiftiHeader.NIFTI_TYPE_INT8:
                                case NiftiHeader.NIFTI_TYPE_UINT8:
                                    v = di.readByte();

                                    if (hdr.datatype == NiftiHeader.NIFTI_TYPE_UINT8 && v < 0)
                                    {
                                        v = v + 256d;
                                    }
                                    break;
                                case NiftiHeader.NIFTI_TYPE_INT16:
                                case NiftiHeader.NIFTI_TYPE_UINT16:
                                    v = di.readShort();

                                    if (hdr.datatype == NiftiHeader.NIFTI_TYPE_UINT16 && v < 0)
                                    {
                                        v = Math.abs(v) + (1 << 15);
                                    }
                                    break;
                                case NiftiHeader.NIFTI_TYPE_INT32:
                                case NiftiHeader.NIFTI_TYPE_UINT32:
                                    v = di.readInt();

                                    if (hdr.datatype == NiftiHeader.NIFTI_TYPE_UINT32 && v < 0)
                                    {
                                        v = Math.abs(v) + (1 << 31);
                                    }
                                    break;
                                case NiftiHeader.NIFTI_TYPE_INT64:
                                case NiftiHeader.NIFTI_TYPE_UINT64:
                                    v = di.readLong();
                                    if (hdr.datatype == NiftiHeader.NIFTI_TYPE_UINT64 && v < 0)
                                    {
                                        v = Math.abs(v) + (1 << 63);
                                    }
                                    break;
                                case NiftiHeader.NIFTI_TYPE_FLOAT32:
                                    v = di.readFloat();
                                    break;
                                case NiftiHeader.NIFTI_TYPE_FLOAT64:
                                    v = di.readDouble();
                                    break;
                                case NiftiHeader.NIFTI_TYPE_RGB24:
                                    // this is odd because I found an RGB24 image that was actually three one channels
                                    v = di.readByte();

                                    if (hdr.datatype == NiftiHeader.NIFTI_TYPE_UINT8 && v < 0)
                                    {
                                        v = v + 256d;
                                    }
                                    break;
                                case NiftiHeader.DT_NONE:
                                case NiftiHeader.DT_BINARY:
                                case NiftiHeader.NIFTI_TYPE_COMPLEX64:
                                case NiftiHeader.NIFTI_TYPE_FLOAT128:
                                case NiftiHeader.NIFTI_TYPE_COMPLEX128:
                                case NiftiHeader.NIFTI_TYPE_COMPLEX256:
                                case NiftiHeader.DT_ALL:
                                default:
                                    throw new IOException("Sorry, cannot yet read nifti-1 datatype " + NiftiHeader.decodeDatatype(hdr.datatype));
                            }

                            // I've found some datasets where scl_slope maps the intensity outside the valid range for
                            // that datatype.  For example, a int16 could be scaled beyond 32,767.  When we preserve the
                            // datatype in the volume this leads to invalid values.  Earlier in this file, there's a step
                            // to promote the datatype if there is scaling present

                            if (SCALE && hdr.scl_slope != 0)
                            {
                                v = v * hdr.scl_slope + hdr.scl_inter;
                            }

                            if (Double.isNaN(v))
                            {
                                v = 0;
                            }

                            if (Double.isInfinite(v))
                            {
                                if (v == Double.NEGATIVE_INFINITY)
                                {
                                    v = Double.MIN_VALUE;
                                }
                                else
                                {
                                    v = Double.MAX_VALUE;
                                }
                            }

                            Sample sample = permutation.apply(new Sample(i, j, k));
                            volume.set(sample, d, v);
                        }
                    }
                }
            }
        }

        fis.close();

        return volume;
    }

    public static Pair<NiftiHeader, Function<Sample, Sample>> getHeader(Volume volume, boolean mask)
    {
        Sampling sampling = volume.getSampling();
        Quaternion rotate = sampling.quat();

        // test for strict equality
        for (Triple<Sampling, Integer, Boolean> c : CACHE.keySet())
        {
            if (!Global.getFresh() && c.a == sampling && c.b == volume.getDim() && c.c == mask)
            {
                Logging.info("using previous nifti header");
                return CACHE.get(c);
            }
        }

        // assume LPI<->RAS
        NiftiHeader hdr = new NiftiHeader();

        hdr.dim[1] = (short) sampling.numI();
        hdr.dim[2] = (short) sampling.numJ();
        hdr.dim[3] = (short) sampling.numK();

        hdr.sform_code = 1;
        hdr.qform_code = 1;
        hdr.qfac = 1;
        hdr.pixdim[0] = 1;
        hdr.pixdim[1] = (float) sampling.deltaI();
        hdr.pixdim[2] = (float) sampling.deltaJ();
        hdr.pixdim[3] = (float) sampling.deltaK();

        hdr.qoffset[0] = (float) sampling.startI();
        hdr.qoffset[1] = (float) sampling.startJ();
        hdr.qoffset[2] = (float) sampling.startK();
        hdr.quatern[0] = (float) rotate.getB();
        hdr.quatern[1] = (float) rotate.getC();
        hdr.quatern[2] = (float) rotate.getD();

        hdr.update_sform();

        // return the identity since no permutation is needed
        Function<Sample, Sample> permutation = s -> s;

        return Pair.of(hdr, permutation);
    }

    public static void writeData(NiftiHeader hdr, Volume volume, Function<Sample, Sample> permutation) throws IOException
    {
        int dim = hdr.dim[4] > 0 ? hdr.dim[4] : 1;
        int nz = hdr.dim[3] > 0 ? hdr.dim[3] : 1;
        int ny = hdr.dim[2];
        int nx = hdr.dim[1];

        boolean le = hdr.little_endian;
        boolean gz = hdr.filename.endsWith(".gz");

        FileOutputStream fos = new FileOutputStream(hdr.filename);
        OutputStream os = gz ? new BufferedOutputStream(new GZIPOutputStream(fos)) : new BufferedOutputStream(fos);
        DataOutput dout = le ? new LEDataOutputStream(os) : new DataOutputStream(os);

        byte[] hbytes = hdr.encodeHeader();
        dout.write(hbytes);

        int nextra = (int) hdr.vox_offset - hbytes.length;
        byte[] extra = new byte[nextra];
        dout.write(extra);

        // don't iterate over samples, because order matters
        for (int d = 0; d < dim; d++)
        {
            for (int k = 0; k < nz; k++)
            {
                for (int j = 0; j < ny; j++)
                {
                    for (int i = 0; i < nx; i++)
                    {
                        double v = volume.get(permutation.apply(new Sample(i, j, k)), d);

                        switch (hdr.datatype)
                        {
                            case NiftiHeader.NIFTI_TYPE_INT8:
                            case NiftiHeader.NIFTI_TYPE_UINT8:
                                if (hdr.scl_slope == 0)
                                {
                                    dout.writeByte((int) v);
                                }
                                else
                                {
                                    dout.writeByte((int) ((v - hdr.scl_inter) / hdr.scl_slope));
                                }
                                break;
                            case NiftiHeader.NIFTI_TYPE_INT16:
                            case NiftiHeader.NIFTI_TYPE_UINT16:
                                if (hdr.scl_slope == 0)
                                {
                                    dout.writeShort((short) v);
                                }
                                else
                                {
                                    dout.writeShort((short) ((v - hdr.scl_inter) / hdr.scl_slope));
                                }
                                break;
                            case NiftiHeader.NIFTI_TYPE_INT32:
                            case NiftiHeader.NIFTI_TYPE_UINT32:
                                if (hdr.scl_slope == 0)
                                {
                                    dout.writeInt((int) v);
                                }
                                else
                                {
                                    dout.writeInt((int) ((v - hdr.scl_inter) / hdr.scl_slope));
                                }
                                break;
                            case NiftiHeader.NIFTI_TYPE_INT64:
                            case NiftiHeader.NIFTI_TYPE_UINT64:
                                if (hdr.scl_slope == 0)
                                {
                                    dout.writeLong((long) Math.rint(v));
                                }
                                else
                                {
                                    dout.writeLong((long) Math.rint((v - hdr.scl_inter) / hdr.scl_slope));
                                }
                                break;
                            case NiftiHeader.NIFTI_TYPE_FLOAT32:
                                if (hdr.scl_slope == 0)
                                {
                                    dout.writeFloat((float) v);
                                }
                                else
                                {
                                    dout.writeFloat((float) ((v - hdr.scl_inter) / hdr.scl_slope));
                                }
                                break;
                            case NiftiHeader.NIFTI_TYPE_FLOAT64:
                                if (hdr.scl_slope == 0)
                                {
                                    dout.writeDouble(v);
                                }
                                else
                                {
                                    dout.writeDouble((v - hdr.scl_inter) / hdr.scl_slope);
                                }
                                break;
                            case NiftiHeader.DT_NONE:
                            case NiftiHeader.DT_BINARY:
                            case NiftiHeader.NIFTI_TYPE_COMPLEX64:
                            case NiftiHeader.NIFTI_TYPE_FLOAT128:
                            case NiftiHeader.NIFTI_TYPE_RGB24:
                            case NiftiHeader.NIFTI_TYPE_COMPLEX128:
                            case NiftiHeader.NIFTI_TYPE_COMPLEX256:
                            case NiftiHeader.DT_ALL:
                            default:
                                throw new IOException("Sorry, cannot yet write nifti-1 datatype " + NiftiHeader.decodeDatatype(hdr.datatype));

                        }
                    }
                }
            }
        }

        if (le)
        {
            ((LEDataOutputStream) dout).close();
        }
        else
        {
            ((DataOutputStream) dout).close();
        }

        return;
    }

}
