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

package qit.data.formats.mesh;

import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** a coder for the freesurfer mesh file format */
@SuppressWarnings("deprecation")
public class FreesurferMeshCoder
{
    private static final int TRIANGLE_FILE_MAGIC_NUMBER = 16777214;

    private static int fread3(DataInputStream dis) throws IOException
    {
        int b1 = dis.readUnsignedByte();
        int b2 = dis.readUnsignedByte();
        int b3 = dis.readUnsignedByte();
        return (b1 << 16) + (b2 << 8) + b3;
    }

    public static Mesh read(InputStream is) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));

        int magic = fread3(dis);

        Global.assume(magic == TRIANGLE_FILE_MAGIC_NUMBER, "invalid freesurfer triangle mesh");

        // Skip the comment
        dis.readLine();
        dis.readLine();

        int nv = dis.readInt();
        int nf = dis.readInt();

        Mesh mesh = new Mesh();
        for (int i = 0; i < nv; i++)
        {
            double x = dis.readFloat();
            double y = dis.readFloat();
            double z = dis.readFloat();

            Vect coords = VectSource.create3D(x, y, z);
            Vertex vert = new Vertex(i);
            mesh.graph.add(vert);
            mesh.vattr.add(vert);
            mesh.vattr.set(vert, Mesh.COORD, coords);
        }

        for (int i = 0; i < nf; i++)
        {
            int a = dis.readInt();
            int b = dis.readInt();
            int c = dis.readInt();
            Vertex va = new Vertex(a);
            Vertex vb = new Vertex(b);
            Vertex vc = new Vertex(c);

            try
            {
                mesh.graph.add(new Face(va, vb, vc));
            }
            catch (Exception e)
            {
                Logging.info(String.format("warning, failed to add face %d (%d, %d, %d) ", i, a, b, c));
            }
        }

        dis.close();

        MeshUtils.computeNormals(mesh);

        return mesh;
    }
}
