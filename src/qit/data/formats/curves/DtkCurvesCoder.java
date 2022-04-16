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

import qit.base.Logging;
import qit.base.structs.LEDataInputStream;
import qit.base.structs.LEDataOutputStream;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.formats.volume.DtkHeader;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DtkCurvesCoder
{
    public static boolean IMAGE = false;

    private static Sampling SAMPLING_CACHE = null;
    private static DtkHeader HEADER_CACHE = null;

    public static void setReference(Sampling sampling)
    {
        SAMPLING_CACHE = sampling;
    }

    public static Curves read(InputStream is) throws IOException
    {
        is = new BufferedInputStream(is);
        LEDataInputStream dis = new LEDataInputStream(is);
        DtkHeader h = DtkHeader.read(dis);

        // only store CRS coords, as Ras coords are buggy
        Curves curves = new Curves(h.n_count, new Vect(3));
        String[] scalars = new String[h.n_scalars];
        for (int i = 0; i < h.n_scalars; i++)
        {
            scalars[i] = "scalar_" + i;
            curves.add(scalars[i], new Vect(1));
        }

        Affine xfm = IMAGE ? Affine.id(3) : h.xfm();

        // n_count is zero for DTI curves, so don't use it...
        int idx = 0;
        int count = -1;
        while (true)
        {
            try
            {
                count = dis.readInt();

                Curve curve = curves.add(count);
                for (int i = 0; i < count; i++)
                {
                    double x = dis.readFloat();
                    double y = dis.readFloat();
                    double z = dis.readFloat();
                    Vect v = xfm.apply(VectSource.create3D(x, y, z));

                    curve.set(Curves.COORD, i, v);

                    for (int j = 0; j < h.n_scalars; j++)
                    {
                        double val = dis.readFloat();
                        curve.set(scalars[j], i, VectSource.create1D(val));
                    }
                }

                for (int j = 0; j < h.n_properties; j++)
                {
                    dis.readFloat();
                    // dont use properties
                }

                idx += 1;
            }
            catch (EOFException eof)
            {
                break;
            }
            catch (Exception e)
            {
                Logging.info(String.format("warning: skipping curve that was invalid. index: %d, count: %d ", idx, count));
                continue;
            }
        }

        dis.close();

        HEADER_CACHE = h;

        return curves;
    }

    public static void write(Curves curves, OutputStream os) throws IOException
    {
        os = new BufferedOutputStream(os);
        LEDataOutputStream dos = new LEDataOutputStream(os);

        DtkHeader h = HEADER_CACHE != null ? HEADER_CACHE : new DtkHeader();

        if (SAMPLING_CACHE != null)
        {
            h.voxel_size[0] = (float) SAMPLING_CACHE.deltaI();
            h.voxel_size[1] = (float) SAMPLING_CACHE.deltaJ();
            h.voxel_size[2] = (float) SAMPLING_CACHE.deltaK();

            h.dim[0] = SAMPLING_CACHE.numI();
            h.dim[1] = SAMPLING_CACHE.numJ();
            h.dim[2] = SAMPLING_CACHE.numK();

            h.vox_to_ras[0][0] = (float) SAMPLING_CACHE.deltaI();
            h.vox_to_ras[1][1] = (float) SAMPLING_CACHE.deltaJ();
            h.vox_to_ras[2][2] = (float) SAMPLING_CACHE.deltaK();
            h.vox_to_ras[0][3] = (float) SAMPLING_CACHE.startI();
            h.vox_to_ras[1][3] = (float) SAMPLING_CACHE.startJ();
            h.vox_to_ras[2][3] = (float) SAMPLING_CACHE.startK();
        }

        h.n_count = curves.size();
        h.n_properties = 0;
        h.n_scalars = 0;

        Affine xfm = IMAGE ? Affine.id(3) : h.xfm().inv();

        DtkHeader.write(dos, h);

        for (Curve curve : curves)
        {
            dos.writeInt(curve.size());

            for (Vect v : curve.get(Curves.COORD))
            {
                Vect tv = xfm.apply(v);
                dos.writeFloat((float) tv.get(0));
                dos.writeFloat((float) tv.get(1));
                dos.writeFloat((float) tv.get(2));
            }
        }

        dos.close();
    }
}
