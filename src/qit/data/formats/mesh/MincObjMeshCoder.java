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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/* a minc object code */
public class MincObjMeshCoder
{
    // http://www.bic.mni.mcgill.ca/users/mishkin/mni_obj_format.pdf

    public static void write(Mesh mesh, OutputStream os) throws IOException
    {
        if (!mesh.vattr.has(Mesh.NORMAL))
        {
            MeshUtils.computeNormals(mesh);
        }

        // index the vertices
        // use a linked hash map to ensure the order of iteration is consistent
        Map<Vertex, Integer> vmap = Maps.newLinkedHashMap();
        {
            // obj expects one-based vertex indices
            int idx = 0;
            for (Vertex vertex : mesh.graph.verts())
            {
                vmap.put(vertex, idx);
                idx += 1;
            }
        }

        PrintWriter pw = new PrintWriter(new BufferedOutputStream(os));
        pw.write("P 0.3 0.7 0.5 100 1 "); // these look like material settings

        pw.write(String.format("%d \n", mesh.graph.numVertex()));

        for (Vertex vertex : vmap.keySet())
        {
            Vect v = mesh.vattr.get(vertex, Mesh.COORD);
            double x = v.get(0);
            double y = v.get(1);
            double z = v.get(2);

            pw.write(String.format("%g %g %g \n", x, y, z));
        }

        for (Vertex vertex : vmap.keySet())
        {
            Vect v = mesh.vattr.get(vertex, Mesh.NORMAL);
            double x = v.get(0);
            double y = v.get(1);
            double z = v.get(2);

            pw.write(String.format("%g %g %g \n", x, y, z));
        }

        pw.write(String.format("%d \n", mesh.graph.numFace()));

        pw.write("2 "); // this indicates per-vertex colors

        Vect dc = VectSource.create4D(1.0, 0.78, 0.67, 1.0);
        for (Vertex vertex : vmap.keySet())
        {
            Vect c = mesh.vattr.has(Mesh.COLOR) ? mesh.vattr.get(vertex, Mesh.COLOR) : dc;
            double r = c.get(0);
            double g = c.get(1);
            double b = c.get(2);
            double a = c.size() == 4 ? c.get(3) : 1.0;

            pw.write(String.format("%g %g %g %g \n", r, g, b, a));
        }

        pw.write("\n");

        int lineCount = 0;
        int faceCount = 0;

        // this part stores the "end index" of each polygon
        // this is useful for mixing polygon sizes, but here we only store triangles
        for (Face f : mesh.graph.faces())
        {
            faceCount += 1;
            lineCount += 1;

            pw.write(String.format("%d ", 3 * faceCount));

            if (lineCount > 8)
            {
                lineCount = 0;
                pw.write("\n");
            }
        }

        pw.write("\n");

        for (Face f : mesh.graph.faces())
        {
            int a = vmap.get(f.getA());
            int b = vmap.get(f.getB());
            int c = vmap.get(f.getC());

            pw.write(String.format("%d %d %d\n", a, b, c));
        }

        pw.write("\n");

        pw.close();
    }

    public static Mesh read(InputStream is) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));

        Mesh mesh = new Mesh();

        List<String> tokens = Lists.newArrayList();
        while (true)
        {
            String line = dis.readLine();

            if (line == null)
            {
                break;
            }

            for (String token : line.split(" "))
            {
                token = token.trim();
                if (token.length() > 0)
                {
                    tokens.add(token);
                }
            }
        }

        dis.close();

        // Check first line for identifier
        if (!tokens.get(0).equals("P"))
        {
            throw new RuntimeException("Stream does not encode a MINC OBJ mesh");
        }

        int idx = 6;

        int nvert = Integer.valueOf(tokens.get(idx++));

        Global.assume(nvert > 0, "invalid number of vertices: " + nvert);

        for (int i = 0; i < nvert; i++)
        {
            double x = Double.valueOf(tokens.get(idx++));
            double y = Double.valueOf(tokens.get(idx++));
            double z = Double.valueOf(tokens.get(idx++));

            Vertex vert = new Vertex(i + 1);
            mesh.vattr.set(vert, Mesh.COORD, VectSource.create3D(x, y, z));
        }

        for (int i = 0; i < nvert; i++)
        {
            double x = Double.valueOf(tokens.get(idx++));
            double y = Double.valueOf(tokens.get(idx++));
            double z = Double.valueOf(tokens.get(idx++));

            Vertex vert = new Vertex(i + 1);
            mesh.vattr.set(vert, Mesh.NORMAL, VectSource.create3D(x, y, z));
        }

        int nfaces = Integer.valueOf(tokens.get(idx++));

        Global.assume(nfaces > 0, "invalid number of faces: " + nfaces);

        int color = Integer.valueOf(tokens.get(idx++));


        if (color == 0)
        {
            // solid color
            double r = Double.valueOf(tokens.get(idx++));
            double g = Double.valueOf(tokens.get(idx++));
            double b = Double.valueOf(tokens.get(idx++));
            double a = Double.valueOf(tokens.get(idx++));

            for (int i = 0; i < nvert; i++)
            {
                Vertex vert = new Vertex(i + 1);
                mesh.vattr.set(vert, Mesh.COLOR, VectSource.create4D(r, g, b, a));
            }
        }
        else if (color == 1)
        {
            // per polygon colors

            Logging.info("warning: polygon colors were detected, but are not supported");

            // increment the index to skip over these
            for (int i = 0; i < nfaces; i++)
            {
                idx += 4;
            }
        }
        else if (color == 2)
        {
            // per vertex colors

            for (int i = 0; i < nvert; i++)
            {
                double r = Double.valueOf(tokens.get(idx++));
                double g = Double.valueOf(tokens.get(idx++));
                double b = Double.valueOf(tokens.get(idx++));
                double a = Double.valueOf(tokens.get(idx++));

                Vertex vert = new Vertex(i + 1);
                mesh.vattr.set(vert, Mesh.COLOR, VectSource.create4D(r, g, b, a));
            }
        }

        // skip the face end indices
        for (int i = 0; i < nfaces; i++)
        {
            idx += 1;
        }

        for (int i = 0; i < nfaces; i++)
        {
            Vertex a = new Vertex(Integer.valueOf(tokens.get(idx++)) + 1);
            Vertex b = new Vertex(Integer.valueOf(tokens.get(idx++)) + 1);
            Vertex c = new Vertex(Integer.valueOf(tokens.get(idx++)) + 1);

            try
            {
                mesh.graph.add(new Face(a, b, c));
            }
            catch (Exception e)
            {
                Logging.info("invalid face");
            }
        }

        return mesh;
    }
}
