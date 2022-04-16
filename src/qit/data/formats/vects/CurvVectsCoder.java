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

import qit.data.datasets.Vects;
import qit.data.source.VectSource;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/** a coder for the freesurfer surface curvature file format (they store it separate from the mesh geometry) */
public class CurvVectsCoder
{
    public static final int NEW_VERSION_MAGIC_NUMBER = 16777215;

    @SuppressWarnings("unused")
    public static Vects read(String fn) throws IOException
    {
        // assume big-endian
        DataInputStream dis = new DataInputStream(new FileInputStream(fn));

        int vnum = fread3(dis);
        int fnum;
        float[] curv;

        if (vnum == NEW_VERSION_MAGIC_NUMBER)
        {
            vnum = dis.readInt();
            fnum = dis.readInt();
            int dim = dis.readInt();

            curv = new float[vnum];
            for (int i = 0; i < vnum; i++)
            {
                curv[i] = dis.readFloat();
            }
        }
        else
        {
            fnum = fread3(dis);
            curv = new float[vnum];
            for (int i = 0; i < vnum; i++)
            {
                curv[i] = dis.readShort() / 100.0f;
            }
        }

        dis.close();

        Vects vects = new Vects();
        for (int i = 0; i < vnum; i++)
        {
            vects.add(VectSource.create1D(curv[i]));
        }

        return vects;
    }

    // a funky three byte integer used in freesurfer
    public static int fread3(DataInputStream dis) throws IOException
    {
        int b1 = dis.readByte() & 0xff;
        int b2 = dis.readByte() & 0xff;
        int b3 = dis.readByte() & 0xff;

        return (b1 << 16) + (b2 << 8) + b3;
    }
}
