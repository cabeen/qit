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

package qit.base.structs;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ArrayInputStream implements DataInput
{
    private boolean little;
    private InputStream is;
    private DataInputStream dis;
    private byte[] buffer;

    public ArrayInputStream(InputStream in, boolean little)
    {
        this.is = in;
        this.dis = new DataInputStream(in);
        this.little = little;
        this.buffer = new byte[8];
    }

    public ArrayInputStream(InputStream in)
    {
        this(in, false);
    }

    /** Interface methods */

    public final void close() throws IOException
    {
        this.dis.close();
    }

    public final int read(byte ba[], int off, int len) throws IOException
    {
        return this.is.read(ba, off, len);
    }

    public final boolean readBoolean() throws IOException
    {
        return this.dis.readBoolean();
    }

    public final byte readByte() throws IOException
    {
        return this.dis.readByte();
    }

    public final char readChar() throws IOException
    {
        this.dis.readFully(this.buffer, 0, 2);
        if (this.little)
        {
            return (char) ((this.buffer[1] & 0xff) << 8 | this.buffer[0] & 0xff);
        }
        else
        {
            return (char) ((this.buffer[0] & 0xff) << 8 | this.buffer[1] & 0xff);
        }
    }

    public final double readDouble() throws IOException
    {
        return Double.longBitsToDouble(this.readLong());
    }

    public final float readFloat() throws IOException
    {
        return Float.intBitsToFloat(this.readInt());
    }

    public final void readFully(byte ba[]) throws IOException
    {
        this.dis.readFully(ba, 0, ba.length);
    }

    public final void readFully(byte ba[], int off, int len) throws IOException
    {
        this.dis.readFully(ba, off, len);
    }

    public final int readInt() throws IOException
    {
        this.dis.readFully(this.buffer, 0, 4);
        if (this.little)
        {
            return this.buffer[3] << 24 | (this.buffer[2] & 0xff) << 16 | (this.buffer[1] & 0xff) << 8 | this.buffer[0] & 0xff;
        }
        else
        {
            return this.buffer[0] << 24 | (this.buffer[1] & 0xff) << 16 | (this.buffer[2] & 0xff) << 8 | this.buffer[3] & 0xff;
        }

    }

    @SuppressWarnings("deprecation")
    public final String readLine() throws IOException
    {
        return this.dis.readLine();
    }

    public final long readLong() throws IOException
    {
        this.dis.readFully(this.buffer, 0, 8);
        /* long cast needed or shift done modulo 32 */
        if (this.little)
        {
            return (long) this.buffer[7] << 56 | (long) (this.buffer[6] & 0xff) << 48 | (long) (this.buffer[5] & 0xff) << 40
                    | (long) (this.buffer[4] & 0xff) << 32 | (long) (this.buffer[3] & 0xff) << 24 | (long) (this.buffer[2] & 0xff) << 16
                    | (long) (this.buffer[1] & 0xff) << 8 | this.buffer[0] & 0xff;
        }
        else
        {
            return (long) this.buffer[0] << 56 | (long) (this.buffer[1] & 0xff) << 48 | (long) (this.buffer[2] & 0xff) << 40
                    | (long) (this.buffer[3] & 0xff) << 32 | (long) (this.buffer[4] & 0xff) << 24 | (long) (this.buffer[5] & 0xff) << 16
                    | (long) (this.buffer[6] & 0xff) << 8 | this.buffer[7] & 0xff;
        }
    }

    public final short readShort() throws IOException
    {
        this.dis.readFully(this.buffer, 0, 2);
        if (this.little)
        {
            return (short) ((this.buffer[1] & 0xff) << 8 | this.buffer[0] & 0xff);
        }
        else
        {
            return (short) ((this.buffer[0] & 0xff) << 8 | this.buffer[1] & 0xff);
        }
    }

    public final String readUTF() throws IOException
    {
        return this.dis.readUTF();
    }

    public final int readUnsignedByte() throws IOException
    {
        return this.dis.readUnsignedByte();
    }

    public final int readUnsignedShort() throws IOException
    {
        this.dis.readFully(this.buffer, 0, 2);
        if (this.little)
        {
            return (this.buffer[1] & 0xff) << 8 | this.buffer[0] & 0xff;
        }
        else
        {
            return (this.buffer[0] & 0xff) << 8 | this.buffer[1] & 0xff;
        }
    }

    public final int skipBytes(int n) throws IOException
    {
        return this.dis.skipBytes(n);
    }

    /** Custom methods */

    public long readUnsignedInt() throws IOException
    {
        int v = this.dis.readInt();
        if (v < 0)
        {
            return Math.abs(v) + (long) (1 << 63);
        }
        else
        {
            return v;
        }
    }

    public void readArrayBoolean(float[] out, int offset, int count) throws IOException
    {
        for (int i = 0; i < count; i++)
        {
            out[offset + i] = this.readBoolean() ? 1.0f : 0.0f;
        }
    }
    
    public void readArrayByte(float[] out, int offset, int count) throws IOException
    {
        for (int i = 0; i < count; i++)
        {
            out[offset + i] = this.readByte();
        }
    }
    
    public void readArrayUnsignedByte(float[] out, int offset, int count) throws IOException
    {
        for (int i = 0; i < count; i++)
        {
            out[offset + i] = this.readUnsignedByte();
        }
    }
    
    public void readArrayInt(float[] out, int offset, int count) throws IOException
    {
        for (int i = 0; i < count; i++)
        {
            out[offset + i] = this.readInt();
        }
    }
    
    public void readArrayLong(float[] out, int offset, int count) throws IOException
    {
        for (int i = 0; i < count; i++)
        {
            out[offset + i] = this.readLong();
        }
    }
    
    public void readArrayFloat(float[] out, int offset, int count) throws IOException
    {
        for (int i = 0; i < count; i++)
        {
            out[offset + i] = this.readFloat();
        }
    }
    
    public void readArrayDouble(float[] out, int offset, int count) throws IOException
    {
        for (int i = 0; i < count; i++)
        {
            out[offset + i] = (float) this.readDouble();
        }
    }

    public void readArrayText(float[] buffer, int offset, int count) throws IOException
    {
        String line = null;
        int idx = 0;
        while (idx < count)
        {
            if ((line = this.readLine()) == null)
            {
                throw new RuntimeException("Unexpected end of file");
            }

            for (String token : line.split("\\s+"))
            {
                buffer[offset + idx++] = Float.parseFloat(token);
            }
        }
    }
}
