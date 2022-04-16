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

/*
 * [LEDataOutputStream.java]
 *
 * Summary: Little-endian version of DataOutputStream.
 *
 * Copyright: (c) 1998-2011 Roedy Green, Canadian Mind Products, http://mindprod.com
 *
 * Licence: This software may be copied and used freely for any purpose but military.
 *          http://mindprod.com/contact/nonmil.html
 *
 * Requires: JDK 1.1+
 *
 * Created with: JetBrains IntelliJ IDEA IDE http://www.jetbrains.com/idea/
 *
 * Version History:
 *  1.0 1998-01-06
 *  1.1 1998-01-07 officially implements DataInput
 *  1.2 1998-01-09 add LERandomAccessFile
 *  1.3 1998-08-28
 *  1.4 1998-11-10 add new address and phone.
 *  1.5 1999-10-08 use com.mindprod.ledatastream package name.
 *  1.6 2005-06-13 made readLine deprecated
 *  1.7 2007-01-01
 *  1.8 2007-05-24 add pad, icon, pass Intellij inspector
 */
package qit.base.structs;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Little-endian version of DataOutputStream.
 * <p/>
 * Very similar to DataOutputStream except it writes little-endian instead of
 * big-endian binary data. We can't extend DataOutputStream directly since it
 * has only final methods. This forces us implement LEDataOutputStream with a
 * DataOutputStream object, and use wrapper methods.
 * 
 * @author Roedy Green, Canadian Mind Products
 * @version 1.8 2007-05-24
 * @since 1998-01-06
 */
public final class LEDataOutputStream implements DataOutput
{
    // ------------------------------ CONSTANTS ------------------------------

    /**
     * undisplayed copyright notice.
     * 
     * @noinspection UnusedDeclaration
     */
    @SuppressWarnings("unused")
    private static final String EMBEDDED_COPYRIGHT = "Copyright: (c) 1999-2011 Roedy Green, Canadian Mind Products, http://mindprod.com";

    // ------------------------------ FIELDS ------------------------------

    /**
     * to build at big-Endian write methods of DataOutPutStream.
     * 
     * @noinspection WeakerAccess
     */
    protected final DataOutputStream dis;

    /**
     * work array for composing output.
     * 
     * @noinspection WeakerAccess
     */
    protected final byte[] work;

    // -------------------------- PUBLIC INSTANCE METHODS
    // --------------------------

    /**
     * constructor.
     * 
     * @param out
     *            the outputstream we write little endian binary data onto.
     */
    public LEDataOutputStream(OutputStream out)
    {
        this.dis = new DataOutputStream(out);
        this.work = new byte[8];// work array for composing output
    }

    /**
     * Close stream.
     * 
     * @throws IOException
     *             if close fails.
     */
    public final void close() throws IOException
    {
        this.dis.close();
    }

    /**
     * Flush stream without closing.
     * 
     * @throws IOException
     *             if flush fails.
     */
    public void flush() throws IOException
    {
        this.dis.flush();
    }

    /**
     * Get size of stream.
     * 
     * @return bytes written so far in the stream. Note this is an int, not a
     *         long as you would exect. This because the underlying
     *         DataInputStream has a design flaw.
     */
    public final int size()
    {
        return this.dis.size();
    }

    /**
     * This method writes only one byte, even though it says int (non-Javadoc)
     * 
     * @param ib
     *            the byte to write.
     * 
     * @throws IOException
     *             if write fails.
     * @see java.io.DataOutput#write(int)
     */
    public final synchronized void write(int ib) throws IOException
    {
        this.dis.write(ib);
    }

    /**
     * Write out an array of bytes.
     * 
     * @throws IOException
     *             if write fails.
     * @see java.io.DataOutput#write(byte[])
     */
    public final void write(byte ba[]) throws IOException
    {
        this.dis.write(ba, 0, ba.length);
    }

    /**
     * Writes out part of an array of bytes.
     * 
     * @throws IOException
     *             if write fails.
     * @see java.io.DataOutput#write(byte[], int, int)
     */
    public final synchronized void write(byte ba[], int off, int len) throws IOException
    {
        this.dis.write(ba, off, len);
    }

    /**
     * Write a booleans as one byte.
     * 
     * @param v
     *            boolean to write.
     * 
     * @throws IOException
     *             if write fails.
     * @see java.io.DataOutput#writeBoolean(boolean)
     */
    /* Only writes one byte */
    public final void writeBoolean(boolean v) throws IOException
    {
        this.dis.writeBoolean(v);
    }

    /**
     * write a byte.
     * 
     * @param v
     *            the byte to write.
     * 
     * @throws IOException
     *             if write fails.
     * @see java.io.DataOutput#writeByte(int)
     */
    public final void writeByte(int v) throws IOException
    {
        this.dis.writeByte(v);
    }

    /**
     * Write a string.
     * 
     * @param s
     *            the string to write.
     * 
     * @throws IOException
     *             if write fails.
     * @see java.io.DataOutput#writeBytes(java.lang.String)
     */
    public final void writeBytes(String s) throws IOException
    {
        this.dis.writeBytes(s);
    }

    /**
     * Write a char. Like DataOutputStream.writeChar. Note the parm is an int
     * even though this as a writeChar
     * 
     * @param v
     *            the char to write
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeChar(int v) throws IOException
    {
        // same code as writeShort
        this.work[0] = (byte) v;
        this.work[1] = (byte) (v >> 8);
        this.dis.write(this.work, 0, 2);
    }

    /**
     * Write a string, not a char[]. Like DataOutputStream.writeChars, flip
     * endianness of each char.
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeChars(String s) throws IOException
    {
        int len = s.length();
        for (int i = 0; i < len; i++)
        {
            this.writeChar(s.charAt(i));
        }
    }// end writeChars

    /**
     * Write a double.
     * 
     * @param v
     *            the double to write. Like DataOutputStream.writeDouble.
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeDouble(double v) throws IOException
    {
        this.writeLong(Double.doubleToLongBits(v));
    }

    /**
     * Write a float. Like DataOutputStream.writeFloat.
     * 
     * @param v
     *            the float to write.
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeFloat(float v) throws IOException
    {
        this.writeInt(Float.floatToIntBits(v));
    }

    /**
     * Write an int, 32-bits. Like DataOutputStream.writeInt.
     * 
     * @param v
     *            the int to write
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeInt(int v) throws IOException
    {
        this.work[0] = (byte) v;
        this.work[1] = (byte) (v >> 8);
        this.work[2] = (byte) (v >> 16);
        this.work[3] = (byte) (v >> 24);
        this.dis.write(this.work, 0, 4);
    }

    /**
     * Write a long, 64-bits. like DataOutputStream.writeLong.
     * 
     * @param v
     *            the long to write
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeLong(long v) throws IOException
    {
        this.work[0] = (byte) v;
        this.work[1] = (byte) (v >> 8);
        this.work[2] = (byte) (v >> 16);
        this.work[3] = (byte) (v >> 24);
        this.work[4] = (byte) (v >> 32);
        this.work[5] = (byte) (v >> 40);
        this.work[6] = (byte) (v >> 48);
        this.work[7] = (byte) (v >> 56);
        this.dis.write(this.work, 0, 8);
    }

    /**
     * Write short, 16-bits. Like DataOutputStream.writeShort. also acts as a
     * writeUnsignedShort
     * 
     * @param v
     *            the short you want written in little endian binary format
     * 
     * @throws IOException
     *             if write fails.
     */
    public final void writeShort(int v) throws IOException
    {
        this.work[0] = (byte) v;
        this.work[1] = (byte) (v >> 8);
        this.dis.write(this.work, 0, 2);
    }

    /**
     * Write a string as a UTF counted string.
     * 
     * @param s
     *            the string to write.
     * 
     * @throws IOException
     *             if write fails.
     * @see java.io.DataOutput#writeUTF(java.lang.String)
     */
    public final void writeUTF(String s) throws IOException
    {
        this.dis.writeUTF(s);
    }
}// end LEDataOutputStream
