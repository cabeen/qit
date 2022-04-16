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

package qit.data.formats.curves;

import qit.base.structs.LEDataInputStream;
import qit.base.structs.LEDataOutputStream;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("unused")
public class PdbCurvesCoder
{
    /* http://graphics.stanford.edu/projects/dti/software/pdb_format.html */
    private static final int DOUBLE_SIZE = 8;
    private static final int SINGLE_SIZE = 4;
    private static final int TEXT_SIZE = 256;
    private static final int ALGO_SIZE = 2 * TEXT_SIZE + SINGLE_SIZE;
    private static final int MATRIX_SIZE = 4;
    private static final int NUM_ALGO = 0;
    private static final int HEADER_SIZE = MATRIX_SIZE * MATRIX_SIZE * DOUBLE_SIZE + 4 * SINGLE_SIZE + NUM_ALGO * ALGO_SIZE;
    private static final int PATH_HEADER_SIZE = 3 * SINGLE_SIZE;
    private static final String ALGO_NAME = "STT";

    public static Curves read(InputStream is) throws IOException
    {
        is = new BufferedInputStream(is);
        LEDataInputStream dis = new LEDataInputStream(is);
        Curves curves = new Curves(new Vect(3));

        int hsize = dis.readInt();
        double[][] mat = new double[MATRIX_SIZE][MATRIX_SIZE];
        for (int i = 0; i < MATRIX_SIZE; i++)
        {
            for (int j = 0; j < MATRIX_SIZE; j++)
            {
                mat[i][j] = dis.readDouble();
            }
        }

        int nstat = dis.readInt();
        boolean[] aggs = new boolean[nstat];
        for (int i = 0; i < nstat; i++)
        {
            int unused = dis.readInt();
            int agg = dis.readInt();
            int unused2 = dis.readInt();
            char[] text = new char[TEXT_SIZE];
            for (int j = 0; j < TEXT_SIZE; j++)
            {
                text[j] = (char) dis.readByte();
            }
            char[] unused3 = new char[TEXT_SIZE];
            for (int j = 0; j < TEXT_SIZE; j++)
            {
                unused3[j] = (char) dis.readByte();
            }

            int id = dis.readInt();

            aggs[i] = agg == 1;
        }

        int nalgo = dis.readInt();

        for (int i = 0; i < nalgo; i++)
        {
            char[] name = new char[TEXT_SIZE];
            for (int j = 0; j < TEXT_SIZE; j++)
            {
                name[j] = (char) dis.readByte();
            }

            char[] comment = new char[TEXT_SIZE];
            for (int j = 0; j < TEXT_SIZE; j++)
            {
                comment[j] = (char) dis.readByte();
            }

            int id = dis.readInt();
        }

        int version = dis.readInt();

        int npath = dis.readInt();

        for (int i = 0; i < npath; i++)
        {
            int phsize = dis.readInt();

            int np = dis.readInt();
            int id = dis.readInt();
            int seed = dis.readInt();

            for (int j = 0; j < nstat; j++)
            {
                double stat = dis.readDouble();
            }

            Curve curve = curves.add(np);

            for (int j = 0; j < np; j++)
            {
                double x = dis.readDouble();
                double y = dis.readDouble();
                double z = dis.readDouble();

                curve.set(Curves.COORD, j, VectSource.create3D(x, y, z));
            }

            for (int j = 0; j < nstat; j++)
             {
                if (aggs[j])
                 {
                    for (int k = 0; k < np; k++)
                     {
                        dis.readDouble(); // Stat
                    }
                }
            }
        }

        dis.close();

        return curves;
    }

    public static void write(Curves curves, OutputStream os) throws IOException
    {
        os = new BufferedOutputStream(os);
        LEDataOutputStream dos = new LEDataOutputStream(os);

        // Write header size
        dos.writeInt(HEADER_SIZE);

        // Write the transformation matrix
        for (int i = 0; i < MATRIX_SIZE; i++)
        {
            for (int j = 0; j < MATRIX_SIZE; j++)
            {
                if (i == j)
                {
                    dos.writeDouble(1.0);
                }
                else
                {
                    dos.writeDouble(0.0);
                }
            }
        }

        // Write the number of statistics
        dos.writeInt(0);

        // Write the number of algorithms
        dos.writeInt(NUM_ALGO);

        for (int i = 0; i < NUM_ALGO; i++)
        {
            // Write the algorithm name
            for (int j = 0; j < TEXT_SIZE; j++)
            {
                if (j < ALGO_NAME.length())
                {
                    dos.writeByte(ALGO_NAME.charAt(j));
                }
                else
                {
                    dos.writeByte(0);
                }
            }

            // Write the algorithm comment
            for (int j = 0; j < TEXT_SIZE; j++)
            {
                dos.writeByte(0);
            }

            // Write the algorithm id
            dos.writeInt(i);
        }

        // Write the algorithm name
        for (int i = 0; i < TEXT_SIZE; i++)
        {
            ;
        }

        // Write the version
        dos.writeInt(1);

        // Write the number of ICurves (pathways)
        dos.writeInt(curves.size());

        long[] poffsets = new long[curves.size()];
        int cidx = 0;
        for (Curve curve : curves)
        {
            // Write the pathway header size
            dos.writeInt(PATH_HEADER_SIZE);

            poffsets[cidx++] = dos.size();

            // Write the number of points
            dos.writeInt(curve.size());

            // Write the algo id
            dos.writeInt(0);

            // Write seed point index
            dos.writeInt(1);

            for (Vect v : curve.get(Curves.COORD))
            {
                // Write the vertex
                dos.writeDouble(v.get(0));
                dos.writeDouble(v.get(1));
                dos.writeDouble(v.get(2));
            }
        }

        for (long poffset : poffsets)
        {
            dos.writeLong(poffset);
        }

        dos.close();
    }
}
