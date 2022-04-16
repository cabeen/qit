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

import com.google.common.io.LittleEndianDataOutputStream;
import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.io.*;
import java.util.function.Supplier;

/**
 * a coder for the stereo-lithography file format
 */
public class StlMeshCoder
{
    // http://www.fabbers.com/tech/STL_Format

    public static void write(Mesh mesh, OutputStream os) throws IOException
    {
        // always write binary in little endian
        LittleEndianDataOutputStream dos = new LittleEndianDataOutputStream(new BufferedOutputStream(os));

        // empty header
        dos.write(new byte[80]);

        int numFaces = mesh.graph.numFace();
        dos.writeInt(numFaces);

        for (Face face : mesh.graph.faces())
        {
            Vect a = mesh.vattr.get(face.getA(), Mesh.COORD);
            Vect b = mesh.vattr.get(face.getB(), Mesh.COORD);
            Vect c = mesh.vattr.get(face.getC(), Mesh.COORD);
            Vect n = MeshUtils.normal(a, b, c);

            dos.writeFloat((float) n.getX());
            dos.writeFloat((float) n.getY());
            dos.writeFloat((float) n.getZ());
            dos.writeFloat((float) a.getX());
            dos.writeFloat((float) a.getY());
            dos.writeFloat((float) a.getZ());
            dos.writeFloat((float) b.getX());
            dos.writeFloat((float) b.getY());
            dos.writeFloat((float) b.getZ());
            dos.writeFloat((float) c.getX());
            dos.writeFloat((float) c.getY());
            dos.writeFloat((float) c.getZ());

            // attribute byte count: keep empty for portability
            dos.writeByte(0);
            dos.writeByte(0);
        }

        dos.close();
    }

    public static Mesh read(InputStream is) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));

        Global.assume(dis.readLine().startsWith("solid"), "Stream does not encode an ascii stl mesh");
        String line = dis.readLine();

        Mesh mesh = new Mesh();
        Supplier<Vertex> parseVertex = () ->
        {
            Vertex v = new Vertex(mesh.vattr.size() + 1);

            try
            {
                String[] tokens = dis.readLine().trim().split(" ");
                double x = Double.valueOf(tokens[1]);
                double y = Double.valueOf(tokens[2]);
                double z = Double.valueOf(tokens[3]);

                mesh.graph.add(v);
                mesh.vattr.add(v);
                mesh.vattr.set(v, Mesh.COORD, VectSource.create3D(x, y, z));

                return v;
            }
            catch (Exception e)
            {
                Logging.info("warning: failed to parse vertex " + v.id());
                return null;
            }
        };

        while (line != null)
        {
            if (line.contains("outer loop"))
            {
                Vertex a = parseVertex.get();
                Vertex b = parseVertex.get();
                Vertex c = parseVertex.get();

                mesh.graph.add(new Face(a, b, c));
            }

            line = dis.readLine();
        }

        dis.close();

        return mesh;
    }
}
