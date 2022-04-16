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
 * [LEDataInputStream.java]
 *
 * Summary: Little-Endian version of DataInputStream.
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
 *  1.8 2007-05-24
 */
package qit.base.structs;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Little-Endian version of DataInputStream.
 * <p/>
 * Very similar to DataInputStream except it reads little-endian instead of
 * big-endian binary data. We can't extend DataInputStream directly since it has
 * only final methods, though DataInputStream itself is not final. This forces
 * us implement LEDataInputStream with a DataInputStream object, and use wrapper
 * methods.
 * 
 * @author Roedy Green, Canadian Mind Products
 * @version 1.8 2007-05-24
 * @since 1998
 */
public final class LEDataInputStream implements DataInput
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
     * to build at the big-Endian methods of a basic DataInputStream
     * 
     * @noinspection WeakerAccess
     */
    protected final DataInputStream dis;

    /**
     * to build at the a basic readBytes method.
     * 
     * @noinspection WeakerAccess
     */
    protected final InputStream is;

    /**
     * work array for buffering input.
     * 
     * @noinspection WeakerAccess
     */
    protected final byte[] work;

    // -------------------------- PUBLIC STATIC METHODS
    // --------------------------

    /**
     * Note. This is a STATIC method!
     * 
     * @param in
     *            stream to read UTF chars from (endian irrelevant)
     * 
     * @return string from stream
     * @throws IOException
     *             if read fails.
     */
    public static String readUTF(DataInput in) throws IOException
    {
        return DataInputStream.readUTF(in);
    }

    // -------------------------- PUBLIC INSTANCE METHODS
    // --------------------------

    /**
     * constructor.
     * 
     * @param in
     *            binary inputstream of little-endian data.
     */
    public LEDataInputStream(InputStream in)
    {
        this.is = in;
        this.dis = new DataInputStream(in);
        this.work = new byte[8];
    }

    /**
     * close.
     * 
     * @throws IOException
     *             if close fails.
     */
    public final void close() throws IOException
    {
        this.dis.close();
    }

    /**
     * Read bytes. Watch out, read may return fewer bytes than requested.
     * 
     * @param ba
     *            where the bytes go.
     * @param off
     *            offset in buffer, not offset in file.
     * @param len
     *            count of bytes to read.
     * 
     * @return how many bytes read.
     * @throws IOException
     *             if read fails.
     */
    public final int read(byte ba[], int off, int len) throws IOException
    {
        // For efficiency, we avoid one layer of wrapper
        return this.is.read(ba, off, len);
    }

    /**
     * read only a one-byte boolean.
     * 
     * @return true or false.
     * @throws IOException
     *             if read fails.
     * @see java.io.DataInput#readBoolean()
     */
    public final boolean readBoolean() throws IOException
    {
        return this.dis.readBoolean();
    }

    /**
     * read byte.
     * 
     * @return the byte read.
     * @throws IOException
     *             if read fails.
     * @see java.io.DataInput#readByte()
     */
    public final byte readByte() throws IOException
    {
        return this.dis.readByte();
    }

    /**
     * Read on char. like DataInputStream.readChar except little endian.
     * 
     * @return little endian 16-bit unicode char from the stream.
     * @throws IOException
     *             if read fails.
     */
    public final char readChar() throws IOException
    {
        this.dis.readFully(this.work, 0, 2);
        return (char) ((this.work[1] & 0xff) << 8 | this.work[0] & 0xff);
    }

    /**
     * Read a double. like DataInputStream.readDouble except little endian.
     * 
     * @return little endian IEEE double from the datastream.
     * @throws IOException
     */
    public final double readDouble() throws IOException
    {
        return Double.longBitsToDouble(this.readLong());
    }

    /**
     * Read one float. Like DataInputStream.readFloat except little endian.
     * 
     * @return little endian IEEE float from the datastream.
     * @throws IOException
     *             if read fails.
     */
    public final float readFloat() throws IOException
    {
        return Float.intBitsToFloat(this.readInt());
    }

    /**
     * Read bytes until the array is filled.
     * 
     * @see java.io.DataInput#readFully(byte[])
     */
    public final void readFully(byte ba[]) throws IOException
    {
        this.dis.readFully(ba, 0, ba.length);
    }

    /**
     * Read bytes until the count is satisfied.
     * 
     * @throws IOException
     *             if read fails.
     * @see java.io.DataInput#readFully(byte[], int, int)
     */
    public final void readFully(byte ba[], int off, int len) throws IOException
    {
        this.dis.readFully(ba, off, len);
    }

    /**
     * Read an int, 32-bits. Like DataInputStream.readInt except little endian.
     * 
     * @return little-endian binary int from the datastream
     * @throws IOException
     *             if read fails.
     */
    public final int readInt() throws IOException
    {
        this.dis.readFully(this.work, 0, 4);
        return this.work[3] << 24 | (this.work[2] & 0xff) << 16 | (this.work[1] & 0xff) << 8 | this.work[0] & 0xff;
    }

    /**
     * Read a line.
     * 
     * @return a rough approximation of the 8-bit stream as a 16-bit unicode
     *         string
     * @throws IOException
     * @noinspection deprecation
     * @deprecated This method does not properly convert bytes to characters.
     *             Use a Reader instead with a little-endian encoding.
     */
    public final String readLine() throws IOException
    {
        return this.dis.readLine();
    }

    /**
     * read a long, 64-bits. Like DataInputStream.readLong except little endian.
     * 
     * @return little-endian binary long from the datastream.
     * @throws IOException
     */
    public final long readLong() throws IOException
    {
        this.dis.readFully(this.work, 0, 8);
        return (long) this.work[7] << 56 |
        /* long cast needed or shift done modulo 32 */
        (long) (this.work[6] & 0xff) << 48 | (long) (this.work[5] & 0xff) << 40 | (long) (this.work[4] & 0xff) << 32 | (long) (this.work[3] & 0xff) << 24
                | (long) (this.work[2] & 0xff) << 16 | (long) (this.work[1] & 0xff) << 8 | this.work[0] & 0xff;
    }

    /**
     * Read short, 16-bits. Like DataInputStream.readShort except little endian.
     * 
     * @return little endian binary short from stream.
     * @throws IOException
     *             if read fails.
     */
    public final short readShort() throws IOException
    {
        this.dis.readFully(this.work, 0, 2);
        return (short) ((this.work[1] & 0xff) << 8 | this.work[0] & 0xff);
    }

    /**
     * Read UTF counted string.
     * 
     * @return String read.
     */
    public final String readUTF() throws IOException
    {
        return this.dis.readUTF();
    }

    /**
     * Read an unsigned byte. Note: returns an int, even though says Byte
     * (non-Javadoc)
     * 
     * @throws IOException
     *             if read fails.
     * @see java.io.DataInput#readUnsignedByte()
     */
    public final int readUnsignedByte() throws IOException
    {
        return this.dis.readUnsignedByte();
    }

    /**
     * Read an unsigned short, 16 bits. Like DataInputStream.readUnsignedShort
     * except little endian. Note, returns int even though it reads a short.
     * 
     * @return little-endian int from the stream.
     * @throws IOException
     *             if read fails.
     */
    public final int readUnsignedShort() throws IOException
    {
        this.dis.readFully(this.work, 0, 2);
        return (this.work[1] & 0xff) << 8 | this.work[0] & 0xff;
    }

    /**
     * Skip over bytes in the stream. See the general contract of the
     * <code>skipBytes</code> method of <code>DataInput</code>.
     * <p/>
     * Bytes for this operation are read from the contained input stream.
     * 
     * @param n
     *            the number of bytes to be skipped.
     * 
     * @return the actual number of bytes skipped.
     * @throws IOException
     *             if an I/O error occurs.
     */
    public final int skipBytes(int n) throws IOException
    {
        return this.dis.skipBytes(n);
    }
}
