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


package qit.data.formats.vects;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;
import qit.base.utils.PathUtils;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class RawVectsCoder
{
    public static LittleEndianDataOutputStream dos(String fn) throws IOException
    {
        OutputStream os = fn.equals("-") ? System.out : new FileOutputStream(fn);

        if (fn.endsWith(".gz"))
        {
            os = new GZIPOutputStream(os);
        }

        return new LittleEndianDataOutputStream(os);
    }

    public static LittleEndianDataInputStream dis(String fn) throws IOException
    {
        InputStream os = new FileInputStream(fn);

        if (fn.endsWith(".gz"))
        {
            os = new GZIPInputStream(os);
        }

        return new LittleEndianDataInputStream(os);
    }

    public static Vects read(String fn) throws IOException
    {
        String lower = fn.toLowerCase();
        if (lower.contains("f32") || lower.contains("float32") || lower.contains("single"))
        {
            return readFloat32(fn);
        }
        else if (lower.contains("f64") || lower.contains("float64") || lower.contains("double"))
        {
            return readFloat64(fn);
        }
        else if (lower.contains("int") || lower.contains("integer") || lower.contains("label"))
        {
            return readInt(fn);
        }
        else
        {
            return readFloat64(fn);
        }
    }

    public static void write(Vects vects, String fn) throws IOException
    {
        String lower = PathUtils.basename(fn).toLowerCase();
        if (lower.contains("f32.") || lower.contains("float32.") || lower.contains("float."))
        {
            writeFloat32(vects, fn);
        }
        else if (lower.contains("f64.") || lower.contains("float64.") || lower.contains("double."))
        {
            writeFloat64(vects, fn);
        }
        else if (lower.contains("int32.") || lower.contains("integer."))
        {
            writeInt(vects, fn);
        }
        else
        {
            writeFloat64(vects, fn);
        }
    }

    public static Vects readInt(String fn) throws IOException
    {
        LittleEndianDataInputStream dis = dis(fn);

        Vects out = new Vects();
        try
        {
            while (true)
            {
                double val = dis.readInt();
                out.add(VectSource.create1D(val));
            }
        }
        catch (Exception e)
        {
            dis.close();
        }

        return out;
    }

    public static Vects readFloat32(String fn) throws IOException
    {
        LittleEndianDataInputStream dis = dis(fn);

        Vects out = new Vects();
        try
        {
            while (true)
            {
                double val = dis.readFloat();
                out.add(VectSource.create1D(val));
            }
        }
        catch (Exception e)
        {
            dis.close();
        }

        return out;
    }

    public static Vects readFloat64(String fn) throws IOException
    {
        LittleEndianDataInputStream dis = dis(fn);

        Vects out = new Vects();
        try
        {
            while (true)
            {
                double val = dis.readDouble();
                out.add(VectSource.create1D(val));
            }
        }
        catch (Exception e)
        {
            dis.close();
        }

        return out;
    }

    public static void writeFloat32(Vects vects, String fn) throws IOException
    {
        LittleEndianDataOutputStream dos = dos(fn);

        for (int i = 0; i < vects.size(); i++)
        {
            for (int j = 0; j < vects.getDim(); j++)
            {
                try
                {
                    dos.writeFloat((float) vects.get(i).get(j));
                }
                catch (Exception e)
                {
                    dos.close();
                    return;
                }
            }
        }

        dos.close();
    }

    public static void writeFloat64(Vects vects, String fn) throws IOException
    {
        LittleEndianDataOutputStream dos = dos(fn);

        for (int i = 0; i < vects.size(); i++)
        {
            for (int j = 0; j < vects.getDim(); j++)
            {
                try
                {
                    dos.writeDouble(vects.get(i).get(j));
                }
                catch (Exception e)
                {
                    dos.close();
                    return;
                }
            }
        }

        dos.close();
    }

    public static void writeInt(Vects vects, String fn) throws IOException
    {
        LittleEndianDataOutputStream dos = dos(fn);

        for (int i = 0; i < vects.size(); i++)
        {
            for (int j = 0; j < vects.getDim(); j++)
            {
                try
                {
                    dos.writeInt((int) vects.get(i).get(j));
                }
                catch (Exception e)
                {
                    dos.close();
                    return;
                }
            }
        }

        dos.close();
    }
}
