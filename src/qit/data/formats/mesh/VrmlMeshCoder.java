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

import com.google.common.collect.Maps;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.utils.MeshUtils;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

public class VrmlMeshCoder
{
    // Taubin et al. "Geometry Coding and VRML"

    public static void write(Mesh mesh, OutputStream os) throws IOException
    {
        boolean color = mesh.vattr.has(Mesh.COLOR);

        // index the vertices
        // use a linked hash map to ensure the order of iteration is consistent
        Map<Vertex, Integer> vmap = Maps.newLinkedHashMap();
        {
            int idx = 0;
            for (Vertex vertex : mesh.graph.verts())
            {
                vmap.put(vertex, idx);
                idx += 1;
            }
        }

        PrintWriter pw = new PrintWriter(new BufferedOutputStream(os));
        pw.write("#VRML V2.0 utf8\n");
        pw.write("Shape {\n");
        pw.write(" geometry IndexedFaceSet {\n");
        pw.write("  coord Coordinate {\n");
        pw.write("   point [\n");

        for (Vertex vertex : vmap.keySet())
        {
            Vect v = mesh.vattr.get(vertex, Mesh.COORD);
            pw.write(String.format("    %g %g %g,\n", v.getX(), v.getY(), v.getZ()));
        }

        pw.write("   ]\n"); // end point
        pw.write("  }\n"); // end Coordinates

        if (color)
        {
            pw.write("  color Color {\n");
            pw.write("   color [\n");

            for (Vertex vertex : vmap.keySet())
            {
                Vect v = mesh.vattr.get(vertex, Mesh.COLOR);
                double r = v.get(0);
                double g = v.get(1);
                double b = v.get(2);

                pw.write(String.format("    %g %g %g,\n", r, g, b));
            }

            pw.write("   ]\n"); // end color
            pw.write("  }\n"); // end Color
        }

        pw.write("  coordIndex [\n");

        for (Face f : mesh.graph.faces())
        {
            int a = vmap.get(f.getA());
            int b = vmap.get(f.getB());
            int c = vmap.get(f.getC());

            pw.write(String.format("   %d, %d, %d, -1,\n", a, b, c));
        }

        pw.write("  ]\n"); // end coordIndex

        pw.write(" }\n"); // end IndexedFaceSet
        pw.write("}\n"); // end Shape

        pw.close();
    }
}
