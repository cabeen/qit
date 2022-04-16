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
import qit.base.structs.Integers;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/** a coder for the visualization toolkit volume file format */
@SuppressWarnings("serial")
public class VolumeVtkCoder
{
    private static final String VTK_FLAG = "@Volume";
    private static final String IDENT = "# vtk DataFile";
    private static final String VERSION = "Version 2.0";
    private static final String ASCII_ENCODING = "ASCII";
    private static final String BINARY_ENCODING = "BINARY";
    private static final String DATASET = "DATASET STRUCTURED_POINTS";
    private static final String DIMENSIONS = "DIMENSIONS";
    private static final String ORIGIN = "ORIGIN";
    private static final String SPACING = "SPACING";
    private static final String FIELD_DATA = "FIELD FieldData";
    private static final String POINT_DATA = "POINT_DATA";

    private static HashMap<DataType, String> TO_VTK = new HashMap<DataType, String>()
    {
        {
            this.put(DataType.DOUBLE, "double");
            this.put(DataType.FLOAT, "float");
            this.put(DataType.INT, "int");
            this.put(DataType.DOUBLE, "double");
            this.put(DataType.BYTE, "short");
            this.put(DataType.USHORT, "int");
            this.put(DataType.SHORT, "short");
        }
    };

    @SuppressWarnings("unused")
    private static HashMap<String, DataType> FROM_VTK = new HashMap<String, DataType>()
    {
        {
            this.put("double", DataType.DOUBLE);
            this.put("float", DataType.DOUBLE);
            this.put("bit", DataType.BYTE);
            this.put("int", DataType.INT);
            this.put("char", DataType.SHORT);
            this.put("short", DataType.SHORT);
            this.put("unsigned_short", DataType.USHORT);
            this.put("unsigned_int", DataType.INT);
            this.put("long", DataType.INT);
            this.put("unsigned_char", DataType.BYTE);
        }
    };

    public static boolean matches(String fn)
    {
        return fn.endsWith("vtk") || fn.endsWith("vtk.gz");
    }

    public static boolean detect(String fn)
    {
        try
        {
            InputStream is = new FileInputStream(fn);

            if (fn.endsWith(".gz"))
            {
                is = new GZIPInputStream(is);
            }

            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            String line = null;

            // Check first line for identifier
            if (!(line = dis.readLine()).startsWith(IDENT))
            {
                return false;
            }

            // Skip the comment
            String comment = dis.readLine();

            return comment.contains(VTK_FLAG);
        }
        catch (IOException e)
        {
        }

        return false;
    }

    @SuppressWarnings({ "unused", "deprecation" })
    public static Volume read(String fn) throws IOException
    {
        InputStream is = new FileInputStream(fn);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        String line = dis.readLine();

        // Check first line for identifier
        if (line == null || !line.startsWith(IDENT))
        {
            Logging.error("failed to find VTK identifier");
        }

        // Skip the comment
        line = dis.readLine();

        // Check the encoding
        line = dis.readLine();
        if (!line.startsWith(BINARY_ENCODING) && !line.startsWith(ASCII_ENCODING))
        {
            Logging.error("failed to find VTK dataset");
        }
        boolean binary = line.startsWith(BINARY_ENCODING);

        // Read the dimensions
        while ((line = dis.readLine()) != null && !line.startsWith(DIMENSIONS))
        {
        }
        if (line == null)
        {
            Logging.error("failed to find VTK dimensions");
        }

        String[] dimTokens = line.split(" ");
        int dimSamp = dimTokens.length - 1;
        int[] dims = new int[dimSamp];
        for (int i = 0; i < dimSamp; i++)
        {
            dims[i] = Integer.parseInt(dimTokens[i + 1]);
        }

        // Read the origin
        while ((line = dis.readLine()) != null && !line.startsWith(ORIGIN))
        {
        }
        if (line == null)
        {
            Logging.error("failed to find VTK origin");
        }

        String[] originTokens = line.split(" ");
        double[] origin = new double[dimSamp];
        for (int i = 0; i < dimSamp; i++)
        {
            origin[i] = Double.parseDouble(originTokens[i + 1]);
        }

        // Read the spacing
        while ((line = dis.readLine()) != null && !line.startsWith(SPACING))
        {
        }
        if (line == null)
        {
            Logging.error("failed to find VTK spacing");
        }

        String[] spacingTokens = line.split(" ");
        double[] spacing = new double[dimSamp];
        for (int i = 0; i < dimSamp; i++)
        {
            spacing[i] = Double.parseDouble(spacingTokens[i + 1]);
        }

        Vect start = VectSource.create3D(origin[0], origin[1], origin[2]);
        Vect delta = VectSource.create3D(spacing[0], spacing[1], spacing[2]);
        Integers num = new Integers(dims[0], dims[1], dims[2]);
        
        Sampling sampling = new Sampling(start, delta, num);

        while ((line = dis.readLine()) != null && !line.startsWith(POINT_DATA))
        {
        }
        if (line == null)
        {
            Logging.error("failed to find VTK point data");
        }

        String[] pointTokens = line.split(" ");
        int numPoints = Integer.parseInt(pointTokens[pointTokens.length - 1]);

        while ((line = dis.readLine()) != null && !line.startsWith(FIELD_DATA))
        {
        }
        if (line == null)
        {
            Logging.error("failed to find VTK point data");
        }

        String[] fieldTokens = line.split(" ");
        int numFields = Integer.parseInt(fieldTokens[fieldTokens.length - 1]);

        Volume data = null;
        while ((line = dis.readLine()) != null)
        {
            String[] headTokens = line.split(" ");
            if (headTokens.length >= 4)
            {
                int tupleSize = Integer.parseInt(headTokens[headTokens.length - 2]);
                if (tupleSize != numPoints)
                {
                    Logging.error("failed to find matching number of points");
                }

                int fieldSize = Integer.parseInt(headTokens[headTokens.length - 3]);
                data = new Volume(sampling, DataType.FLOAT, fieldSize);
                break;
            }
        }

        if (data == null)
        {
            Logging.error("invalid file");
        }
        
        Vect buff = data.dproto();

        if (binary)
        {
            byte[] b = new byte[numPoints * data.getDim() * 4];
            dis.readFully(b);
            dis = new DataInputStream(new ByteArrayInputStream(b));
            for (Sample sample : sampling)
            {
                for (int i = 0; i < data.getDim(); i++)
                {
                    buff.set(i, dis.readFloat());
                }

                data.set(sample, buff);
            }
        }
        else
        {
            for (Sample sample : sampling)
            {
                String[] valueTokens = dis.readLine().split(" ");
                for (int i = 0; i < data.getDim(); i++)
                {
                    buff.set(i, Double.parseDouble(valueTokens[i]));
                }

                data.set(sample, buff);
            }
        }
        dis.close();

        return data;
    }

    public static void write(Volume data, OutputStream os) throws IOException
    {
        Sampling samp = data.getSampling();

        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(os));

        out.writeBytes(String.format("%s %s\n", IDENT, VERSION));
        out.writeBytes(VTK_FLAG);
        out.writeBytes(String.format(" Generated on %s\n", new Date().toString()));
        out.writeBytes(BINARY_ENCODING);
        out.writeByte('\n');
        out.writeBytes(DATASET);
        out.writeByte('\n');

        out.writeBytes(DIMENSIONS);
        out.writeBytes(String.format(" %d", samp.numI()));
        out.writeBytes(String.format(" %d", samp.numJ()));
        out.writeBytes(String.format(" %d", samp.numK()));
        out.writeByte('\n');

        out.writeBytes(ORIGIN);
        out.writeBytes(String.format(" %f", samp.startI()));
        out.writeBytes(String.format(" %f", samp.startJ()));
        out.writeBytes(String.format(" %f", samp.startK()));
        out.writeByte('\n');

        out.writeBytes(SPACING);
        out.writeBytes(String.format(" %f", samp.deltaI()));
        out.writeBytes(String.format(" %f", samp.deltaJ()));
        out.writeBytes(String.format(" %f", samp.deltaK()));
        out.writeByte('\n');

        out.writeBytes(String.format("%s %d\n", POINT_DATA, samp.size()));
        out.writeBytes(String.format("%s 1\n", FIELD_DATA));

        DataType dtype = data.getType();
        String type = TO_VTK.get(dtype);

        out.writeBytes(String.format("data %d %d %s\n", data.getDim(), samp.size(), type));
        for (int i = 0; i < samp.size(); i++)
        {
            for (int j = 0; j < data.getDim(); j++)
            {
                double v = data.get(i, j);
                switch (dtype)
                {
                case SHORT:
                case BYTE:
                    out.writeShort((int) v);
                    break;
                case USHORT:
                case INT:
                    out.writeInt((int) v);
                    break;
                case FLOAT:
                    out.writeFloat((float) v);
                    break;
                case DOUBLE:
                    out.writeDouble(v);
                    break;
                }
            }
        }
        out.close();
    }
}
