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

import qit.base.Logging;
import qit.base.structs.DataType;
import qit.base.structs.LEDataInputStream;
import qit.base.structs.LEDataOutputStream;
import qit.data.datasets.Record;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

/** a representation for the header information in a nifti header */
public class NiftiHeader
{
    /* derived from http://niftilib.sourceforge.net/ */
    public static final int ANZ_HDR_SIZE = 348;
    public static final int EXT_KEY_SIZE = 8;
    public static final String NII_MAGIC_STRING = "n+1";

    public static final short NIFTI_INTENT_NONE = 0;
    public static final short NIFTI_INTENT_CORREL = 2;
    public static final short NIFTI_INTENT_TTEST = 3;
    public static final short NIFTI_INTENT_FTEST = 4;
    public static final short NIFTI_INTENT_ZSCORE = 5;
    public static final short NIFTI_INTENT_CHISQ = 6;
    public static final short NIFTI_INTENT_BETA = 7;
    public static final short NIFTI_INTENT_BINOM = 8;
    public static final short NIFTI_INTENT_GAMMA = 9;
    public static final short NIFTI_INTENT_POISSON = 10;
    public static final short NIFTI_INTENT_NORMAL = 11;
    public static final short NIFTI_INTENT_FTEST_NONC = 12;
    public static final short NIFTI_INTENT_CHISQ_NONC = 13;
    public static final short NIFTI_INTENT_LOGISTIC = 14;
    public static final short NIFTI_INTENT_LAPLACE = 15;
    public static final short NIFTI_INTENT_UNIFORM = 16;
    public static final short NIFTI_INTENT_TTEST_NONC = 17;
    public static final short NIFTI_INTENT_WEIBULL = 18;
    public static final short NIFTI_INTENT_CHI = 19;
    public static final short NIFTI_INTENT_INVGAUSS = 20;
    public static final short NIFTI_INTENT_EXTVAL = 21;
    public static final short NIFTI_INTENT_PVAL = 22;
    public static final short NIFTI_INTENT_ESTIMATE = 1001;
    public static final short NIFTI_INTENT_LABEL = 1002;
    public static final short NIFTI_INTENT_NEURONAME = 1003;
    public static final short NIFTI_INTENT_GENMATRIX = 1004;
    public static final short NIFTI_INTENT_SYMMATRIX = 1005;
    public static final short NIFTI_INTENT_DISPVECT = 1006;
    public static final short NIFTI_INTENT_VECTOR = 1007;
    public static final short NIFTI_INTENT_POINTSET = 1008;
    public static final short NIFTI_INTENT_TRIANGLE = 1009;
    public static final short NIFTI_INTENT_QUATERNION = 1010;

    public static final short DT_NONE = 0;
    public static final short DT_BINARY = 1;
    public static final short NIFTI_TYPE_UINT8 = 2;
    public static final short NIFTI_TYPE_INT16 = 4;
    public static final short NIFTI_TYPE_INT32 = 8;
    public static final short NIFTI_TYPE_FLOAT32 = 16;
    public static final short NIFTI_TYPE_COMPLEX64 = 32;
    public static final short NIFTI_TYPE_FLOAT64 = 64;
    public static final short NIFTI_TYPE_RGB24 = 128;
    public static final short DT_ALL = 255;
    public static final short NIFTI_TYPE_INT8 = 256;
    public static final short NIFTI_TYPE_UINT16 = 512;
    public static final short NIFTI_TYPE_UINT32 = 768;
    public static final short NIFTI_TYPE_INT64 = 1024;
    public static final short NIFTI_TYPE_UINT64 = 1280;
    public static final short NIFTI_TYPE_FLOAT128 = 1536;
    public static final short NIFTI_TYPE_COMPLEX128 = 1792;
    public static final short NIFTI_TYPE_COMPLEX256 = 2048;

    public static final short NIFTI_UNITS_UNKNOWN = 0;
    public static final short NIFTI_UNITS_METER = 1;
    public static final short NIFTI_UNITS_MM = 2;
    public static final short NIFTI_UNITS_MICRON = 3;
    public static final short NIFTI_UNITS_SEC = 8;
    public static final short NIFTI_UNITS_MSEC = 16;
    public static final short NIFTI_UNITS_USEC = 24;
    public static final short NIFTI_UNITS_HZ = 32;
    public static final short NIFTI_UNITS_PPM = 40;

    public static final short NIFTI_SLICE_SEQ_INC = 1;
    public static final short NIFTI_SLICE_SEQ_DEC = 2;
    public static final short NIFTI_SLICE_ALT_INC = 3;
    public static final short NIFTI_SLICE_ALT_DEC = 4;

    public static final short NIFTI_XFORM_UNKNOWN = 0;
    public static final short NIFTI_XFORM_SCANNER_ANAT = 1;
    public static final short NIFTI_XFORM_ALIGNED_ANAT = 2;
    public static final short NIFTI_XFORM_TALAIRACH = 3;
    public static final short NIFTI_XFORM_MNI_152 = 4;

    public static final int NIFTI_L2R = 1;
    public static final int NIFTI_R2L = 2;
    public static final int NIFTI_P2A = 3;
    public static final int NIFTI_A2P = 4;
    public static final int NIFTI_I2S = 5;
    public static final int NIFTI_S2I = 6;

    public static final String[] NIFTI_ORIENT = {"L", "R", "P", "A", "I", "S"};

    public String filename;
    public boolean little_endian;
    public short freq_dim, phase_dim, slice_dim;
    public short xyz_unit_code, t_unit_code;
    public short qfac;
    public Vector<int[]> extensions_list;
    public Vector<byte[]> extension_blobs;

    public int sizeof_hdr;
    public StringBuffer data_type_string;
    public StringBuffer db_name;
    public int extents;
    public short session_error;
    public StringBuffer regular;
    public StringBuffer dim_info;
    public short dim[];
    public float intent[];
    public short intent_code;
    public short datatype;
    public short bitpix;
    public short slice_start;
    public float pixdim[];
    public float vox_offset;
    public float scl_slope;
    public float scl_inter;
    public short slice_end;
    public byte slice_code;
    public byte xyzt_units;
    public float cal_max;
    public float cal_min;
    public float slice_duration;
    public float toffset;
    public int glmax;
    public int glmin;
    public StringBuffer descrip;
    public StringBuffer aux_file;
    public short qform_code;
    public short sform_code;
    public float quatern[];
    public float qoffset[];
    public float srow_x[];
    public float srow_y[];
    public float srow_z[];
    public StringBuffer intent_name;
    public StringBuffer magic;
    public byte extension[];

    public NiftiHeader()
    {
        this.setDefaults();
        return;
    }

    public void setDatatype(short code)
    {
        this.datatype = code;
        this.bitpix = (short) (bytesPerVoxel(code) * 8);
        return;
    }

    public String decodeIntent(short icode)
    {
        switch (icode)
        {
            case NiftiHeader.NIFTI_INTENT_NONE:
                return "NIFTI_INTENT_NONE";
            case NiftiHeader.NIFTI_INTENT_CORREL:
                return "NIFTI_INTENT_CORREL";
            case NiftiHeader.NIFTI_INTENT_TTEST:
                return "NIFTI_INTENT_TTEST";
            case NiftiHeader.NIFTI_INTENT_FTEST:
                return "NIFTI_INTENT_FTEST";
            case NiftiHeader.NIFTI_INTENT_ZSCORE:
                return "NIFTI_INTENT_ZSCORE";
            case NiftiHeader.NIFTI_INTENT_CHISQ:
                return "NIFTI_INTENT_CHISQ";
            case NiftiHeader.NIFTI_INTENT_BETA:
                return "NIFTI_INTENT_BETA";
            case NiftiHeader.NIFTI_INTENT_BINOM:
                return "NIFTI_INTENT_BINOM";
            case NiftiHeader.NIFTI_INTENT_GAMMA:
                return "NIFTI_INTENT_GAMMA";
            case NiftiHeader.NIFTI_INTENT_POISSON:
                return "NIFTI_INTENT_POISSON";
            case NiftiHeader.NIFTI_INTENT_NORMAL:
                return "NIFTI_INTENT_NORMAL";
            case NiftiHeader.NIFTI_INTENT_FTEST_NONC:
                return "NIFTI_INTENT_FTEST_NONC";
            case NiftiHeader.NIFTI_INTENT_CHISQ_NONC:
                return "NIFTI_INTENT_CHISQ_NONC";
            case NiftiHeader.NIFTI_INTENT_LOGISTIC:
                return "NIFTI_INTENT_LOGISTIC";
            case NiftiHeader.NIFTI_INTENT_LAPLACE:
                return "NIFTI_INTENT_LAPLACE";
            case NiftiHeader.NIFTI_INTENT_UNIFORM:
                return "NIFTI_INTENT_UNIFORM";
            case NiftiHeader.NIFTI_INTENT_TTEST_NONC:
                return "NIFTI_INTENT_TTEST_NONC";
            case NiftiHeader.NIFTI_INTENT_WEIBULL:
                return "NIFTI_INTENT_WEIBULL";
            case NiftiHeader.NIFTI_INTENT_CHI:
                return "NIFTI_INTENT_CHI";
            case NiftiHeader.NIFTI_INTENT_INVGAUSS:
                return "NIFTI_INTENT_INVGAUSS";
            case NiftiHeader.NIFTI_INTENT_EXTVAL:
                return "NIFTI_INTENT_EXTVAL";
            case NiftiHeader.NIFTI_INTENT_PVAL:
                return "NIFTI_INTENT_PVAL";
            case NiftiHeader.NIFTI_INTENT_ESTIMATE:
                return "NIFTI_INTENT_ESTIMATE";
            case NiftiHeader.NIFTI_INTENT_LABEL:
                return "NIFTI_INTENT_LABEL";
            case NiftiHeader.NIFTI_INTENT_NEURONAME:
                return "NIFTI_INTENT_NEURONAME";
            case NiftiHeader.NIFTI_INTENT_GENMATRIX:
                return "NIFTI_INTENT_GENMATRIX";
            case NiftiHeader.NIFTI_INTENT_SYMMATRIX:
                return "NIFTI_INTENT_SYMMATRIX";
            case NiftiHeader.NIFTI_INTENT_DISPVECT:
                return "NIFTI_INTENT_DISPVECT";
            case NiftiHeader.NIFTI_INTENT_VECTOR:
                return "NIFTI_INTENT_VECTOR";
            case NiftiHeader.NIFTI_INTENT_POINTSET:
                return "NIFTI_INTENT_POINTSET";
            case NiftiHeader.NIFTI_INTENT_TRIANGLE:
                return "NIFTI_INTENT_TRIANGLE";
            case NiftiHeader.NIFTI_INTENT_QUATERNION:
                return "NIFTI_INTENT_QUATERNION";
            default:
                return "INVALID_NIFTI_INTENT_CODE";
        }
    }

    public static String decodeDatatype(short dcode)
    {
        switch (dcode)
        {
            case DT_NONE:
                return "DT_NONE";
            case DT_BINARY:
                return "DT_BINARY";
            case NiftiHeader.NIFTI_TYPE_UINT8:
                return "NIFTI_TYPE_UINT8";
            case NiftiHeader.NIFTI_TYPE_INT16:
                return "NIFTI_TYPE_INT16";
            case NiftiHeader.NIFTI_TYPE_INT32:
                return "NIFTI_TYPE_INT32";
            case NiftiHeader.NIFTI_TYPE_FLOAT32:
                return "NIFTI_TYPE_FLOAT32";
            case NiftiHeader.NIFTI_TYPE_COMPLEX64:
                return "NIFTI_TYPE_COMPLEX64";
            case NiftiHeader.NIFTI_TYPE_FLOAT64:
                return "NIFTI_TYPE_FLOAT64";
            case NiftiHeader.NIFTI_TYPE_RGB24:
                return "NIFTI_TYPE_RGB24";
            case DT_ALL:
                return "DT_ALL";
            case NiftiHeader.NIFTI_TYPE_INT8:
                return "NIFTI_TYPE_INT8";
            case NiftiHeader.NIFTI_TYPE_UINT16:
                return "NIFTI_TYPE_UINT16";
            case NiftiHeader.NIFTI_TYPE_UINT32:
                return "NIFTI_TYPE_UINT32";
            case NiftiHeader.NIFTI_TYPE_INT64:
                return "NIFTI_TYPE_INT64";
            case NiftiHeader.NIFTI_TYPE_UINT64:
                return "NIFTI_TYPE_UINT64";
            case NiftiHeader.NIFTI_TYPE_FLOAT128:
                return "NIFTI_TYPE_FLOAT128";
            case NiftiHeader.NIFTI_TYPE_COMPLEX128:
                return "NIFTI_TYPE_COMPLEX128";
            case NiftiHeader.NIFTI_TYPE_COMPLEX256:
                return "NIFTI_TYPE_COMPLEX256";
            default:
                return "INVALID_NIFTI_DATATYPE_CODE";
        }
    }

    public static short bytesPerVoxel(short dcode)
    {
        switch (dcode)
        {
            case DT_NONE:
                return 0;
            case DT_BINARY:
                return -1;
            case NiftiHeader.NIFTI_TYPE_UINT8:
                return 1;
            case NiftiHeader.NIFTI_TYPE_INT16:
                return 2;
            case NiftiHeader.NIFTI_TYPE_INT32:
                return 4;
            case NiftiHeader.NIFTI_TYPE_FLOAT32:
                return 4;
            case NiftiHeader.NIFTI_TYPE_COMPLEX64:
                return 8;
            case NiftiHeader.NIFTI_TYPE_FLOAT64:
                return 8;
            case NiftiHeader.NIFTI_TYPE_RGB24:
                return 3;
            case DT_ALL:
                return 0;
            case NiftiHeader.NIFTI_TYPE_INT8:
                return 1;
            case NiftiHeader.NIFTI_TYPE_UINT16:
                return 2;
            case NiftiHeader.NIFTI_TYPE_UINT32:
                return 4;
            case NiftiHeader.NIFTI_TYPE_INT64:
                return 8;
            case NiftiHeader.NIFTI_TYPE_UINT64:
                return 8;
            case NiftiHeader.NIFTI_TYPE_FLOAT128:
                return 16;
            case NiftiHeader.NIFTI_TYPE_COMPLEX128:
                return 16;
            case NiftiHeader.NIFTI_TYPE_COMPLEX256:
                return 32;
            default:
                return 0;
        }
    }

    public String decodeSliceOrder(short code)
    {
        switch (code)
        {
            case NiftiHeader.NIFTI_SLICE_SEQ_INC:
                return "NIFTI_SLICE_SEQ_INC";
            case NiftiHeader.NIFTI_SLICE_SEQ_DEC:
                return "NIFTI_SLICE_SEQ_DEC";
            case NiftiHeader.NIFTI_SLICE_ALT_INC:
                return "NIFTI_SLICE_ALT_INC";
            case NiftiHeader.NIFTI_SLICE_ALT_DEC:
                return "NIFTI_SLICE_ALT_DEC";
            default:
                return "INVALID_NIFTI_SLICE_SEQ_CODE";
        }
    }

    public String decodeXform(short code)
    {
        switch (code)
        {
            case NiftiHeader.NIFTI_XFORM_UNKNOWN:
                return "NIFTI_XFORM_UNKNOWN";
            case NiftiHeader.NIFTI_XFORM_SCANNER_ANAT:
                return "NIFTI_XFORM_SCANNER_ANAT";
            case NiftiHeader.NIFTI_XFORM_ALIGNED_ANAT:
                return "NIFTI_XFORM_ALIGNED_ANAT";
            case NiftiHeader.NIFTI_XFORM_TALAIRACH:
                return "NIFTI_XFORM_TALAIRACH";
            case NiftiHeader.NIFTI_XFORM_MNI_152:
                return "NIFTI_XFORM_MNI_152";
            default:
                return "INVALID_NIFTI_XFORM_CODE";
        }
    }

    public String decodeUnits(short code)
    {
        switch (code)
        {
            case NiftiHeader.NIFTI_UNITS_UNKNOWN:
                return "NIFTI_UNITS_UNKNOWN";
            case NiftiHeader.NIFTI_UNITS_METER:
                return "NIFTI_UNITS_METER";
            case NiftiHeader.NIFTI_UNITS_MM:
                return "NIFTI_UNITS_MM";
            case NiftiHeader.NIFTI_UNITS_MICRON:
                return "NIFTI_UNITS_MICRON";
            case NiftiHeader.NIFTI_UNITS_SEC:
                return "NIFTI_UNITS_SEC";
            case NiftiHeader.NIFTI_UNITS_MSEC:
                return "NIFTI_UNITS_MSEC";
            case NiftiHeader.NIFTI_UNITS_USEC:
                return "NIFTI_UNITS_USEC";
            case NiftiHeader.NIFTI_UNITS_HZ:
                return "NIFTI_UNITS_HZ";
            case NiftiHeader.NIFTI_UNITS_PPM:
                return "NIFTI_UNITS_PPM";
            default:
                return "INVALID_NIFTI_UNITS_CODE";
        }
    }

    private void setDefaults()
    {
        this.little_endian = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
        this.sizeof_hdr = ANZ_HDR_SIZE;
        this.data_type_string = new StringBuffer();
        for (int i = 0; i < 10; i++)
        {
            this.data_type_string.append("\0");
        }
        this.db_name = new StringBuffer();
        for (int i = 0; i < 18; i++)
        {
            this.db_name.append("\0");
        }
        this.extents = 0;
        this.session_error = 0;
        this.regular = new StringBuffer("\0");
        this.dim_info = new StringBuffer("\0");
        this.freq_dim = 0;
        this.phase_dim = 0;
        this.slice_dim = 0;
        this.dim = new short[8];
        for (int i = 0; i < 8; i++)
        {
            this.dim[i] = 0;
        }
        this.dim[1] = 0;
        this.dim[2] = 0;
        this.dim[3] = 0;
        this.dim[4] = 0;
        this.intent = new float[3];
        for (int i = 0; i < 3; i++)
        {
            this.intent[i] = (float) 0.0;
        }
        this.intent_code = NiftiHeader.NIFTI_INTENT_NONE;
        this.datatype = DT_NONE;
        this.bitpix = 0;
        this.slice_start = 0;
        this.pixdim = new float[8];
        this.pixdim[0] = 1;
        this.qfac = 1;
        for (int i = 1; i < 8; i++)
        {
            this.pixdim[i] = (float) 0.0;
        }

        this.vox_offset = 352;

        this.scl_slope = (float) 0.0;
        this.scl_inter = (float) 0.0;
        this.slice_end = 0;
        this.slice_code = (byte) 0;
        this.xyzt_units = (byte) 0;
        this.xyz_unit_code = NiftiHeader.NIFTI_UNITS_UNKNOWN;
        this.t_unit_code = NiftiHeader.NIFTI_UNITS_UNKNOWN;

        this.cal_max = (float) 0.0;
        this.cal_min = (float) 0.0;
        this.slice_duration = (float) 0.0;
        this.toffset = (float) 0.0;
        this.glmax = 0;
        this.glmin = 0;

        this.descrip = new StringBuffer();

        for (int i = 0; i < 80; i++)
        {
            this.descrip.append("\0");
        }
        this.aux_file = new StringBuffer();
        for (int i = 0; i < 24; i++)
        {
            this.aux_file.append("\0");
        }

        this.qform_code = NiftiHeader.NIFTI_XFORM_UNKNOWN;
        this.sform_code = NiftiHeader.NIFTI_XFORM_UNKNOWN;

        this.quatern = new float[3];
        this.qoffset = new float[3];
        for (int i = 0; i < 3; i++)
        {
            this.quatern[i] = (float) 0.0;
            this.qoffset[i] = (float) 0.0;
        }

        this.srow_x = new float[4];
        this.srow_y = new float[4];
        this.srow_z = new float[4];
        for (int i = 0; i < 4; i++)
        {
            this.srow_x[i] = (float) 0.0;
            this.srow_y[i] = (float) 0.0;
            this.srow_z[i] = (float) 0.0;
        }

        this.intent_name = new StringBuffer();
        for (int i = 0; i < 16; i++)
        {
            this.intent_name.append("\0");
        }

        this.magic = new StringBuffer(NII_MAGIC_STRING);

        this.extension = new byte[4];
        for (int i = 0; i < 4; i++)
        {
            this.extension[i] = (byte) 0;
        }

        this.extensions_list = new Vector<>(5);
        this.extension_blobs = new Vector<>(5);

        return;
    }

    public Record info()
    {
        Record info = new Record();

        info.with("size", String.valueOf(this.sizeof_hdr));
        info.with("data_offset", String.valueOf(this.vox_offset));
        info.with("datatype_code", String.valueOf(this.datatype));
        info.with("datatype_name", decodeDatatype(this.datatype));
        info.with("bit_per_vox", String.valueOf(this.bitpix));
        info.with("scaling_offset", String.valueOf(this.scl_inter));
        info.with("scaling_slope", String.valueOf(this.scl_slope));

        for (int i = 0; i <= this.dim[0]; i++)
        {
            info.with("dim" + i, String.valueOf(this.dim[i]));
        }

        for (int i = 0; i <= this.dim[0]; i++)
        {
            info.with("space" + i, String.valueOf(this.pixdim[i]));
        }

        info.with("xyz_units_code", String.valueOf(this.xyz_unit_code));
        info.with("xyz_units_name", this.decodeUnits(this.xyz_unit_code));
        info.with("t_units_code", String.valueOf(this.t_unit_code));
        info.with("t_units_name", this.decodeUnits(this.t_unit_code));
        info.with("t_offset", String.valueOf(this.toffset));

        for (int i = 0; i < 3; i++)
        {
            info.with("intent" + i, String.valueOf(this.intent[i]));
        }

        info.with("intent_code", String.valueOf(this.intent_code));
        info.with("intent_name", this.decodeIntent(this.intent_code));
        info.with("cal_max", String.valueOf(this.cal_max));
        info.with("cal_min", String.valueOf(this.cal_min));

        info.with("slice_code", String.valueOf(this.slice_code));
        info.with("slice_name", this.decodeSliceOrder(this.slice_code));
        info.with("slice_freq", String.valueOf(this.freq_dim));
        info.with("slice_phase", String.valueOf(this.phase_dim));
        info.with("slice_index", String.valueOf(this.slice_dim));
        info.with("slice_start", String.valueOf(this.slice_start));
        info.with("slice_end", String.valueOf(this.slice_end));
        info.with("slice_dur", String.valueOf(this.slice_duration));
        info.with("qfac", String.valueOf(this.qfac));
        info.with("qform_code", String.valueOf(this.qform_code));
        info.with("qform_name", this.decodeXform(this.qform_code));
        info.with("quat_b", String.valueOf(this.quatern[0]));
        info.with("quat_c", String.valueOf(this.quatern[1]));
        info.with("quat_d", String.valueOf(this.quatern[2]));
        info.with("quat_x", String.valueOf(this.qoffset[0]));
        info.with("quat_y", String.valueOf(this.qoffset[1]));
        info.with("quat_z", String.valueOf(this.qoffset[2]));

        info.with("sform_code", String.valueOf(this.sform_code));
        info.with("sform_name", this.decodeXform(this.sform_code));
        for (int i = 0; i < 4; i++)
        {
            info.with("sform0" + i, String.valueOf(this.srow_x[i]));
        }
        for (int i = 0; i < 4; i++)
        {
            info.with("sform1" + i, String.valueOf(this.srow_y[i]));
        }
        for (int i = 0; i < 4; i++)
        {
            info.with("sform2" + i, String.valueOf(this.srow_z[i]));
        }

        info.with("computed orientation", this.orientation());

        return info;
    }

    private static byte[] setStringSize(StringBuffer s, int n)
    {
        byte b[];
        int slen;

        slen = s.length();

        if (slen >= n)
        {
            return s.toString().substring(0, n).getBytes();
        }

        b = new byte[n];
        for (int i = 0; i < slen; i++)
        {
            b[i] = (byte) s.charAt(i);
        }
        for (int i = slen; i < n; i++)
        {
            b[i] = 0;
        }

        return b;
    }

    public static DataType mapType(int type)
    {
        switch (type)
        {
            case NiftiHeader.NIFTI_TYPE_INT8:
            case NiftiHeader.NIFTI_TYPE_INT16:
                return DataType.SHORT;
            case NiftiHeader.NIFTI_TYPE_UINT16:
            case NiftiHeader.NIFTI_TYPE_INT32:
                return DataType.INT;
            case NiftiHeader.NIFTI_TYPE_FLOAT64:
                return DataType.DOUBLE;
            default:
                return DataType.FLOAT;
        }
    }

    public static short mapType(DataType type)
    {
        switch (type)
        {
            case SHORT:
                return NiftiHeader.NIFTI_TYPE_INT16;
            case INT:
            case USHORT:
                return NiftiHeader.NIFTI_TYPE_INT32;
            case FLOAT:
                return NiftiHeader.NIFTI_TYPE_FLOAT32;
            case DOUBLE:
                return NiftiHeader.NIFTI_TYPE_FLOAT64;
            case BYTE:
                return NiftiHeader.NIFTI_TYPE_UINT8;
            default:
                return -1;
        }
    }

    private static boolean littleEndian(String fn) throws IOException
    {
        InputStream is = new FileInputStream(fn);
        if (fn.endsWith(".gz"))
        {
            is = new GZIPInputStream(is);
        }
        DataInputStream di = new DataInputStream(is);

        di.skipBytes(40);
        short s = di.readShort();

        di.close();
        return s < 1 || s > 7;
    }

    public static NiftiHeader read(String fn) throws IOException
    {
        boolean le = littleEndian(fn);

        InputStream is = new FileInputStream(fn);
        if (fn.endsWith(".gz"))
        {
            is = new GZIPInputStream(is);
        }

        DataInput di = le ? new LEDataInputStream(is) : new DataInputStream(is);

        NiftiHeader ds = new NiftiHeader();

        ds.filename = fn;
        ds.little_endian = le;
        ds.sizeof_hdr = di.readInt();

        byte[] bb = new byte[10];
        di.readFully(bb, 0, 10);
        ds.data_type_string = new StringBuffer(new String(bb));

        bb = new byte[18];
        di.readFully(bb, 0, 18);
        ds.db_name = new StringBuffer(new String(bb));
        ds.extents = di.readInt();
        ds.session_error = di.readShort();
        ds.regular = new StringBuffer();
        ds.regular.append((char) di.readUnsignedByte());
        ds.dim_info = new StringBuffer();
        ds.dim_info.append((char) di.readUnsignedByte());

        int fps_dim = ds.dim_info.charAt(0);
        ds.freq_dim = (short) (fps_dim & 3);
        ds.phase_dim = (short) (fps_dim >>> 2 & 3);
        ds.slice_dim = (short) (fps_dim >>> 4 & 3);

        for (int i = 0; i < 8; i++)
        {
            ds.dim[i] = di.readShort();
        }
        if (ds.dim[0] > 0)
        {
            ds.dim[1] = ds.dim[1];
        }
        if (ds.dim[0] > 1)
        {
            ds.dim[2] = ds.dim[2];
        }
        if (ds.dim[0] > 2)
        {
            ds.dim[3] = ds.dim[3];
        }
        if (ds.dim[0] > 3)
        {
            ds.dim[4] = ds.dim[4];
        }

        for (int i = 0; i < 3; i++)
        {
            ds.intent[i] = di.readFloat();
        }

        ds.intent_code = di.readShort();
        ds.datatype = di.readShort();
        ds.bitpix = di.readShort();
        ds.slice_start = di.readShort();

        for (int i = 0; i < 8; i++)
        {
            ds.pixdim[i] = di.readFloat();
        }

        ds.qfac = (short) Math.floor(ds.pixdim[0]);
        ds.vox_offset = di.readFloat();
        ds.scl_slope = di.readFloat();
        ds.scl_inter = di.readFloat();
        ds.slice_end = di.readShort();
        ds.slice_code = (byte) di.readUnsignedByte();

        ds.xyzt_units = (byte) di.readUnsignedByte();

        int unit_codes = ds.xyzt_units;
        ds.xyz_unit_code = (short) (unit_codes & 007);
        ds.t_unit_code = (short) (unit_codes & 070);

        ds.cal_max = di.readFloat();
        ds.cal_min = di.readFloat();
        ds.slice_duration = di.readFloat();
        ds.toffset = di.readFloat();
        ds.glmax = di.readInt();
        ds.glmin = di.readInt();

        bb = new byte[80];
        di.readFully(bb, 0, 80);
        ds.descrip = new StringBuffer(new String(bb));

        bb = new byte[24];
        di.readFully(bb, 0, 24);
        ds.aux_file = new StringBuffer(new String(bb));

        ds.qform_code = di.readShort();
        ds.sform_code = di.readShort();

        for (int i = 0; i < 3; i++)
        {
            ds.quatern[i] = di.readFloat();
        }
        for (int i = 0; i < 3; i++)
        {
            ds.qoffset[i] = di.readFloat();
        }
        for (int i = 0; i < 4; i++)
        {
            ds.srow_x[i] = di.readFloat();
        }
        for (int i = 0; i < 4; i++)
        {
            ds.srow_y[i] = di.readFloat();
        }
        for (int i = 0; i < 4; i++)
        {
            ds.srow_z[i] = di.readFloat();
        }

        bb = new byte[16];
        di.readFully(bb, 0, 16);
        ds.intent_name = new StringBuffer(new String(bb));

        bb = new byte[4];
        di.readFully(bb, 0, 4);
        ds.magic = new StringBuffer(new String(bb));

        di.readFully(ds.extension, 0, 4);

        if (ds.extension[0] != (byte) 0)
        {
            int start_addr = NiftiHeader.ANZ_HDR_SIZE + 4;

            while (start_addr < (int) ds.vox_offset)
            {
                int[] size_code = new int[2];
                size_code[0] = di.readInt();
                size_code[1] = di.readInt();

                int nb = size_code[0] - NiftiHeader.EXT_KEY_SIZE;
                byte[] eblob = new byte[nb];
                di.readFully(eblob, 0, nb);
                ds.extension_blobs.add(eblob);
                ds.extensions_list.add(size_code);
                start_addr += size_code[0];

                if (start_addr > (int) ds.vox_offset)
                {
                    throw new IOException("Error: Data  for extension " + ds.extensions_list.size() + " appears to overrun start of image data.");
                }
            }
        }

        if (le)
        {
            ((LEDataInputStream) di).close();
        }
        else
        {
            ((DataInputStream) di).close();
        }

        return ds;
    }

    public byte[] encodeHeader() throws IOException
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutput dout = this.little_endian ? new LEDataOutputStream(os) : new DataOutputStream(os);

        dout.writeInt(this.sizeof_hdr);

        if (this.data_type_string.length() >= 10)
        {
            dout.writeBytes(this.data_type_string.substring(0, 10));
        }
        else
        {
            dout.writeBytes(this.data_type_string.toString());
            for (int i = 0; i < 10 - this.data_type_string.length(); i++)
            {
                dout.writeByte(0);
            }
        }

        if (this.db_name.length() >= 18)
        {
            dout.writeBytes(this.db_name.substring(0, 18));
        }
        else
        {
            dout.writeBytes(this.db_name.toString());
            for (int i = 0; i < 18 - this.db_name.length(); i++)
            {
                dout.writeByte(0);
            }
        }

        dout.writeInt(this.extents);
        dout.writeShort(this.session_error);
        dout.writeByte(this.regular.charAt(0));

        int spf_dims = 0;
        spf_dims = (spf_dims & this.slice_dim & 3) << 2;
        spf_dims = (spf_dims & this.phase_dim & 3) << 2;
        spf_dims = spf_dims & this.freq_dim & 3;
        byte b = (byte) spf_dims;
        dout.writeByte(b);

        for (int i = 0; i < 8; i++)
        {
            dout.writeShort(this.dim[i]);
        }

        for (int i = 0; i < 3; i++)
        {
            dout.writeFloat(this.intent[i]);
        }

        dout.writeShort(this.intent_code);
        dout.writeShort(this.datatype);
        dout.writeShort(this.bitpix);
        dout.writeShort(this.slice_start);

        for (int i = 0; i < 8; i++)
        {
            dout.writeFloat(this.pixdim[i]);
        }

        dout.writeFloat(this.vox_offset);
        dout.writeFloat(this.scl_slope);
        dout.writeFloat(this.scl_inter);
        dout.writeShort(this.slice_end);
        dout.writeByte(this.slice_code);

        int units = (byte) (this.xyz_unit_code & 007 | this.t_unit_code & 070);
        dout.writeByte(units);
        dout.writeFloat(this.cal_max);
        dout.writeFloat(this.cal_min);
        dout.writeFloat(this.slice_duration);
        dout.writeFloat(this.toffset);
        dout.writeInt(this.glmax);
        dout.writeInt(this.glmin);
        dout.write(NiftiHeader.setStringSize(this.descrip, 80), 0, 80);
        dout.write(NiftiHeader.setStringSize(this.aux_file, 24), 0, 24);
        dout.writeShort(this.qform_code);
        dout.writeShort(this.sform_code);

        for (int i = 0; i < 3; i++)
        {
            dout.writeFloat(this.quatern[i]);
        }
        for (int i = 0; i < 3; i++)
        {
            dout.writeFloat(this.qoffset[i]);
        }
        for (int i = 0; i < 4; i++)
        {
            dout.writeFloat(this.srow_x[i]);
        }
        for (int i = 0; i < 4; i++)
        {
            dout.writeFloat(this.srow_y[i]);
        }
        for (int i = 0; i < 4; i++)
        {
            dout.writeFloat(this.srow_z[i]);
        }

        dout.write(NiftiHeader.setStringSize(this.intent_name, 16), 0, 16);
        dout.write(NiftiHeader.setStringSize(this.magic, 4), 0, 4);

        if (this.extension[0] != 0)
        {
            for (int i = 0; i < 4; i++)
            {
                dout.writeByte(this.extension[i]);
            }

            for (int i = 0; i < this.extensions_list.size(); i++)
            {
                int[] size_code = this.extensions_list.get(i);
                dout.writeInt(size_code[0]);
                dout.writeInt(size_code[1]);

                byte[] eblob = this.extension_blobs.get(i);
                dout.write(eblob);
            }
        }

        if (this.little_endian)
        {
            ((LEDataOutputStream) dout).close();
        }
        else
        {
            ((DataOutputStream) dout).close();
        }

        return os.toByteArray();
    }

    public String orientation()
    {
        double[][] mat44 = this.mat44();
        double val, detQ, detP;
        double[][] P = new double[3][3];
        double[][] Q = new double[3][3];
        double[][] M = new double[3][3];

        int i = 0;
        int j = 0;
        int k = 0;
        int p, q, r, ibest, jbest, kbest, pbest, qbest, rbest;
        double vbest;

        double xi = mat44[0][0];
        double xj = mat44[0][1];
        double xk = mat44[0][2];
        double yi = mat44[1][0];
        double yj = mat44[1][1];
        double yk = mat44[1][2];
        double zi = mat44[2][0];
        double zj = mat44[2][1];
        double zk = mat44[2][2];

        // normalize column vectors to build unit vectors along each ijk-axis

        // normalize i axis
        val = Math.sqrt(xi * xi + yi * yi + zi * zi);
        if (val == 0.0)
        {
            throw new RuntimeException("invalid nifti transform");
        }
        xi /= val;
        yi /= val;
        zi /= val;

        // normalize j axis
        val = Math.sqrt(xj * xj + yj * yj + zj * zj);
        if (val == 0.0)
        {
            throw new RuntimeException("invalid nifti transform");
        }
        xj /= val;
        yj /= val;
        zj /= val;

        // orthogonalize j axis to i axis, if needed
        val = xi * xj + yi * yj + zi * zj; // dot product between i and j
        if (Math.abs(val) > 1.e-4)
        {
            xj -= val * xi;
            yj -= val * yi;
            zj -= val * zi;
            val = Math.sqrt(xj * xj + yj * yj + zj * zj); // renormalize
            if (val == 0.0)
            {
                // j was parallel to i?
                throw new RuntimeException("invalid nifti transform");
            }
            xj /= val;
            yj /= val;
            zj /= val;
        }

        // normalize k axis; if it is zero, make it the cross product i x j
        val = Math.sqrt(xk * xk + yk * yk + zk * zk);
        if (val == 0.0)
        {
            xk = yi * zj - zi * yj;
            yk = zi * xj - zj * xi;
            zk = xi * yj - yi * xj;
        }
        else
        {
            xk /= val;
            yk /= val;
            zk /= val;
        }

        // orthogonalize k to i
        val = xi * xk + yi * yk + zi * zk; /* dot product between i and k */
        if (Math.abs(val) > 1.e-4)
        {
            xk -= val * xi;
            yk -= val * yi;
            zk -= val * zi;
            val = Math.sqrt(xk * xk + yk * yk + zk * zk);
            if (val == 0.0)
            {
                throw new RuntimeException("invalid nifti transform"); /* bad */
            }
            xk /= val;
            yk /= val;
            zk /= val;
        }

        // orthogonalize k to j
        val = xj * xk + yj * yk + zj * zk; // dot product between j and k
        if (Math.abs(val) > 1.e-4)
        {
            xk -= val * xj;
            yk -= val * yj;
            zk -= val * zj;
            val = Math.sqrt(xk * xk + yk * yk + zk * zk);
            if (val == 0.0)
            {
                throw new RuntimeException("invalid nifti transform"); /* bad */
            }
            xk /= val;
            yk /= val;
            zk /= val;
        }

        Q[0][0] = xi;
        Q[0][1] = xj;
        Q[0][2] = xk;
        Q[1][0] = yi;
        Q[1][1] = yj;
        Q[1][2] = yk;
        Q[2][0] = zi;
        Q[2][1] = zj;
        Q[2][2] = zk;

        // now Q is the rotation matrix from the (i,j,k) to (x,y,z) axes
        detQ = det33(Q);
        if (detQ == 0.0)
        {
            throw new RuntimeException("invalid nifti transform");
        }

        /*
         * Build and test all possible +1/-1 coordinate permutation matrices P;
         * then find the P such that the rotation matrix M=PQ is closest to the
         * identity, in the sense of M having the smallest total rotation angle.
         */

        /*
         * Despite the formidable looking 6 nested loops, there are only
         * 3*3*3*2*2*2 = 216 passes, which will run very quickly.
         */

        vbest = -666.0;
        ibest = pbest = qbest = rbest = 1;
        jbest = 2;
        kbest = 3;
        for (i = 1; i <= 3; i++)
        { /* i = column number to use for row #1 */
            for (j = 1; j <= 3; j++)
            { /* j = column number to use for row #2 */
                if (i == j)
                {
                    continue;
                }
                for (k = 1; k <= 3; k++)
                {
                    /* k = column number to use for row #3 */
                    if (i == k || j == k)
                    {
                        continue;
                    }
                    P[0][0] = P[0][1] = P[0][2] = P[1][0] = P[1][1] = P[1][2] = P[2][0] = P[2][1] = P[2][2] = 0.0;
                    for (p = -1; p <= 1; p += 2)
                    { /* p,q,r are -1 or +1 */
                        for (q = -1; q <= 1; q += 2)
                        { /* and go into rows #1,2,3 */
                            for (r = -1; r <= 1; r += 2)
                            {
                                P[0][i - 1] = p;
                                P[1][j - 1] = q;
                                P[2][k - 1] = r;
                                detP = det33(P); // permutation
                                // sign
                                if (detP * detQ <= 0.0)
                                {
                                    continue; /* doesn't match sign of Q */
                                }
                                M = mult(P, Q);

                                /*
                                 * angle of M rotation =
                                 * 2.0*acos(0.5*sqrt(1.0+trace(M)))
                                 */
                                /*
                                 * we want largest trace(M) == smallest angle ==
                                 * M nearest to I
                                 */

                                val = M[0][0] + M[1][1] + M[2][2]; /* trace */
                                if (val > vbest)
                                {
                                    vbest = val;
                                    ibest = i;
                                    jbest = j;
                                    kbest = k;
                                    pbest = p;
                                    qbest = q;
                                    rbest = r;
                                }
                            }
                        }
                    }
                }
            }
        }

        /*
         * At this point ibest is 1 or 2 or 3; pbest is -1 or +1; etc.
         * 
         * The matrix P that corresponds is the best permutation approximation
         * to Q-inverse; that is, P (approximately) takes (x,y,z) coordinates to
         * the (i,j,k) axes.
         * 
         * For example, the first row of P (which contains pbest in column
         * ibest) determines the way the i axis points relative to the
         * anatomical (x,y,z) axes. If ibest is 2, then the i axis is along the
         * y axis, which is direction P2A (if pbest > 0) or A2P (if pbest < 0).
         * 
         * So, using ibest and pbest, we can assign the output code for the i
         * axis. Mutatis mutandis for the j and k axes, of course.
         */

        switch (ibest * pbest)
        {
            case 1:
                i = NIFTI_L2R;
                break;
            case -1:
                i = NIFTI_R2L;
                break;
            case 2:
                i = NIFTI_P2A;
                break;
            case -2:
                i = NIFTI_A2P;
                break;
            case 3:
                i = NIFTI_I2S;
                break;
            case -3:
                i = NIFTI_S2I;
                break;
        }

        switch (jbest * qbest)
        {
            case 1:
                j = NIFTI_L2R;
                break;
            case -1:
                j = NIFTI_R2L;
                break;
            case 2:
                j = NIFTI_P2A;
                break;
            case -2:
                j = NIFTI_A2P;
                break;
            case 3:
                j = NIFTI_I2S;
                break;
            case -3:
                j = NIFTI_S2I;
                break;
        }

        switch (kbest * rbest)
        {
            case 1:
                k = NIFTI_L2R;
                break;
            case -1:
                k = NIFTI_R2L;
                break;
            case 2:
                k = NIFTI_P2A;
                break;
            case -2:
                k = NIFTI_A2P;
                break;
            case 3:
                k = NIFTI_I2S;
                break;
            case -3:
                k = NIFTI_S2I;
                break;
        }

        return NIFTI_ORIENT[i - 1] + NIFTI_ORIENT[j - 1] + NIFTI_ORIENT[k - 1];
    }

    public static double det33(double[][] R)
    {
        // determinant of a 3x3 matrix
        double r11 = R[0][0];
        double r12 = R[0][1];
        double r13 = R[0][2];
        double r21 = R[1][0];
        double r22 = R[1][1];
        double r23 = R[1][2];
        double r31 = R[2][0];
        double r32 = R[2][1];
        double r33 = R[2][2];

        return r11 * r22 * r33 - r11 * r32 * r23 - r21 * r12 * r33 + r21 * r32 * r13 + r31 * r12 * r23 - r31 * r22 * r13;
    }

    public double[][] mat44()
    {
        if (this.sform_code > 0)
        {
            return this.sform_to_mat44();
        }
        else if (this.qform_code > 0)
        {
            return this.qform_to_mat44();
        }
        else
        {
            double[][] out = new double[4][4];
            out[0][0] = this.pixdim[1];
            out[1][1] = this.pixdim[2];
            out[2][2] = this.pixdim[3];
            out[3][3] = 1.0;

            return out;
        }
    }

    public double[][] sform_to_mat44()
    {
        double[][] out = new double[4][4];

        for (int i = 0; i < 4; i++)
        {
            out[0][i] = this.srow_x[i];
            out[1][i] = this.srow_y[i];
            out[2][i] = this.srow_z[i];
            out[3][i] = 0;
        }
        out[3][3] = 1.0;

        return out;
    }

    public double[][] qform_to_mat44()
    {
        double qb = this.quatern[0];
        double qc = this.quatern[1];
        double qd = this.quatern[2];
        double qx = this.qoffset[0];
        double qy = this.qoffset[1];
        double qz = this.qoffset[2];
        double dx = this.pixdim[1];
        double dy = this.pixdim[2];
        double dz = this.pixdim[3];

        double qfac = this.qfac;

        double[][] R = new double[4][4];

        /* last row is always [ 0 0 0 1 ] */
        R[3][0] = R[3][1] = R[3][2] = 0.0;
        R[3][3] = 1.0;

        /* compute a parameter from b,c,d */

        double d = qd;
        double c = qc;
        double b = qb;
        double a = 1.0 - (b * b + c * c + d * d);
        if (a < 1.e-7)
        { /* special case */
            a = 1.0 / Math.sqrt(b * b + c * c + d * d);
            b *= a;
            c *= a;
            d *= a; /* normalize (b,c,d) vector */
            a = 0.0; /* a = 0 ==> 180 degree rotation */
        }
        else
        {
            a = Math.sqrt(a); /* angle = 2*arccos(a) */
        }

        /* load rotation matrix, including scaling factors for voxel sizes */

        double xd = dx > 0.0 ? dx : 1.0; /* make sure are positive */
        double yd = dy > 0.0 ? dy : 1.0;
        double zd = dz > 0.0 ? dz : 1.0;

        if (qfac < 0.0)
        {
            zd = -zd; /* left handedness? */
        }

        R[0][0] = (a * a + b * b - c * c - d * d) * xd;
        R[0][1] = 2.0 * (b * c - a * d) * yd;
        R[0][2] = 2.0 * (b * d + a * c) * zd;
        R[1][0] = 2.0 * (b * c + a * d) * xd;
        R[1][1] = (a * a + c * c - b * b - d * d) * yd;
        R[1][2] = 2.0 * (c * d - a * b) * zd;
        R[2][0] = 2.0 * (b * d - a * c) * xd;
        R[2][1] = 2.0 * (c * d + a * b) * yd;
        R[2][2] = (a * a + d * d - c * c - b * b) * zd;

        /* load offsets */
        R[0][3] = qx;
        R[1][3] = qy;
        R[2][3] = qz;

        return R;
    }

    public void update_sform()
    {
        double[][] mat44 = this.qform_to_mat44();

        for (int i = 0; i < 4; i++)
        {
            this.srow_x[i] = (float) mat44[0][i];
            this.srow_y[i] = (float) mat44[1][i];
            this.srow_z[i] = (float) mat44[2][i];
        }

        this.sform_code = 1;
    }

    private static double[][] mult(double[][] A, double[][] B)
    {
        // multiply 3x3 matrices
        double[][] C = new double[3][3];
        int i, j;
        for (i = 0; i < 3; i++)
        {
            for (j = 0; j < 3; j++)
            {
                C[i][j] = A[i][0] * B[0][j] + A[i][1] * B[1][j] + A[i][2] * B[2][j];
            }
        }
        return C;
    }
}