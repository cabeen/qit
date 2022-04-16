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

import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OffMeshCoder
{
    // source: http://www.geom.uiuc.edu/software/geomview/ooglman.html

    @SuppressWarnings("deprecation")
    public static Mesh read(InputStream is) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));

        Mesh mesh = new Mesh();

        // Check first line for identifier
        if (!dis.readLine().startsWith("OFF"))
        {
            throw new RuntimeException("Stream does not encode an OFF mesh");
        }

        // read the number of verts and faces
        String[] ntokens = dis.readLine().split(" ");
        int nvert = Integer.valueOf(ntokens[0]);
        int nface = Integer.valueOf(ntokens[1]);
        
        for (int i = 0; i < nvert; i++)
        {
            String[] vtokens = dis.readLine().split(" ");
            double x = Double.valueOf(vtokens[0]);
            double y = Double.valueOf(vtokens[1]);
            double z = Double.valueOf(vtokens[2]);
            
            Vect coords = VectSource.create3D(x, y, z);
            Vertex vert = new Vertex(i);
            mesh.graph.add(vert);
            mesh.vattr.add(vert);
            mesh.vattr.set(vert, Mesh.COORD, coords);
        }
        
        for (int i = 0; i < nface; i++)
        {
            String[] ftokens = dis.readLine().split(" ");
            int a = Integer.valueOf(ftokens[0]);
            int b = Integer.valueOf(ftokens[1]);
            int c = Integer.valueOf(ftokens[2]);
            
            Vertex va = new Vertex(a);
            Vertex vb = new Vertex(b);
            Vertex vc = new Vertex(c);

            mesh.graph.add(new Face(va, vb, vc));
        }
        
        dis.close();

        return mesh;
    }

    public static void write(Mesh mesh, OutputStream os) throws IOException
    {
        DataOutputStream pw = new DataOutputStream(new BufferedOutputStream(os));
        pw.writeBytes("OFF\n");
        pw.writeBytes(String.format("%d %d\n", mesh.graph.numVertex(), mesh.graph.numFace()));

        List<Vertex> verts = new ArrayList<Vertex>(mesh.graph.numVertex());
        for (Vertex vert : mesh.graph.verts())
        {
            verts.add(vert);
        }

        Map<Vertex, Integer> vmap = new HashMap<Vertex, Integer>();
        for (int i = 0; i < verts.size(); i++)
        {
            vmap.put(verts.get(i), i);
        }

        for (Vertex vert : verts)
        {
            Vect v = mesh.vattr.get(vert, Mesh.COORD);
            double x = v.get(0);
            double y = v.get(1);
            double z = v.get(2);
            
            pw.writeBytes(String.format("%g %g %g\n", x, y, z));
        }
        
        for (Face f : mesh.graph.faces())
        {
            int a = vmap.get(f.getA());
            int b = vmap.get(f.getB());
            int c = vmap.get(f.getC());

            pw.writeBytes(String.format("3 %d %d %d\n", a, b, c));
        }

        pw.close();
    }
}
