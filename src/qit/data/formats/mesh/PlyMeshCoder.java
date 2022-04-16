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
import qit.base.structs.Pair;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * a coder for the Stanford Triangle Format
 */
public class PlyMeshCoder
{
    // http://paulbourke.net/dataformats/ply/

    public static void write(Mesh mesh, OutputStream os) throws IOException
    {
        List<String> attrs = Lists.newArrayList(mesh.vattr.attrs());

        DataOutputStream pw = new DataOutputStream(new BufferedOutputStream(os));
        pw.writeBytes("ply\n");
        pw.writeBytes("format ascii 1.0\n");
        pw.writeBytes("comment created by qit\n");
        pw.writeBytes(String.format("element vertex %d\n", mesh.graph.numVertex()));
        pw.writeBytes("property float x\n");
        pw.writeBytes("property float y\n");
        pw.writeBytes("property float z\n");

        if (mesh.vattr.has(Mesh.COLOR))
        {
            pw.writeBytes("property uchar red\n");
            pw.writeBytes("property uchar green\n");
            pw.writeBytes("property uchar blue\n");
        }

        // for (String name : attrs)
        // {
        //     if (mesh.vattr.dim(name) == 1)
        //     {
        //         pw.writeBytes(String.format("property double %s\n", name));
        //     }
        // }

        pw.writeBytes(String.format("element face %d\n", mesh.graph.numFace()));
        pw.writeBytes("property list uchar int vertex_index\n");
        pw.writeBytes("end_header\n");

        List<Vertex> verts = Lists.newArrayList();
        for (Vertex vert : mesh.graph.verts())
        {
            verts.add(vert);
        }

        Map<Vertex, Integer> vmap = Maps.newHashMap();
        for (int i = 0; i < verts.size(); i++)
        {
            vmap.put(verts.get(i), i);
        }

        for (Vertex vert : verts)
        {
            Vect p = mesh.vattr.get(vert, Mesh.COORD);
            double x = p.get(0);
            double y = p.get(1);
            double z = p.get(2);

            StringBuilder build = new StringBuilder();
            build.append(x);
            build.append(' ');
            build.append(y);
            build.append(' ');
            build.append(z);

            if (mesh.vattr.has(Mesh.COLOR))
            {
                Vect c = mesh.vattr.get(vert, Mesh.COLOR);
                int r = (int) Math.min(255, Math.max(0, Math.round(255.0 * c.get(0))));
                int g = (int) Math.min(255, Math.max(0, Math.round(255.0 * c.get(1))));
                int b = (int) Math.min(255, Math.max(0, Math.round(255.0 * c.get(2))));

                build.append(' ');
                build.append(r);
                build.append(' ');
                build.append(g);
                build.append(' ');
                build.append(b);
            }

            // for (String name : mesh.vattr.attrs())
            // {
            //     if (mesh.vattr.dim(name) == 1)
            //     {
            //         Vect v = mesh.vattr.get(vert, name);

            //         build.append(' ');
            //         build.append(v.get(0));
            //     }
            // }

            build.append("\n");

            pw.writeBytes(build.toString());
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

    public static Mesh read(InputStream is) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));

        Supplier<String[]> token = () ->
        {
            try
            {
                String line = dis.readLine();
                while (line != null)
                {
                    String[] tokens = line.trim().split("#")[0].split(" ");
                    if (tokens.length > 0 && tokens[0].length() > 0)
                    {
                        return tokens;
                    }
                }
            }
            catch (IOException e)
            {
            }

            throw new RuntimeException("failed to read tokens from file!");
        };


        // Check first line for identifier
        String[] tokens = token.get();
        Global.assume(tokens[0].startsWith("ply"), "input file does have a ply identifier");

        // check format
        tokens = token.get();
        Global.assume(tokens.length > 1 && tokens[1].equals("ascii"), "only ascii ply files are supported");

        // read the comment
        tokens = token.get();

        // read vertices info
        tokens = token.get();
        Global.assume(tokens.length > 2 && tokens[1].equals("vertex"), "expected vertices: ");
        int nverts = Integer.valueOf(tokens[2]);

        Function<String, Boolean> isAlpha = (name) ->
        {
            String lower = name.toLowerCase();
            return lower.equals("a") || lower.equals("alpha");
        };

        Function<String, Boolean> isRed = (name) ->
        {
            String lower = name.toLowerCase();
            return lower.equals("r") || lower.equals("red");
        };

        Function<String, Boolean> isGreen = (name) ->
        {
            String lower = name.toLowerCase();
            return lower.equals("g") || lower.equals("green");
        };

        Function<String, Boolean> isBlue = (name) ->
        {
            String lower = name.toLowerCase();
            return lower.equals("b") || lower.equals("blue");
        };

        Function<Pair<String, Double>, Double> parseColor = (pair) ->
        {
            String type = pair.a;
            double value = pair.b;

            if (type.equals("uchar") || type.equals("int") || type.equals("char") || type.equals("long"))
            {
                return value / 255.0;
            }
            else
            {
                return value;
            }
        };

        // read vertex properties
        boolean hasColor = false;
        boolean hasAlpha = false;

        List<Pair<String, String>> names = Lists.newArrayList();
        tokens = token.get();
        while (tokens[0].equals("property"))
        {
            Global.assume(tokens.length == 3, "expected three property tokens");
            String type = tokens[1];
            String name = tokens[2];
            names.add(Pair.of(type, name));
            tokens = token.get();

            hasColor |= isRed.apply(name) || isGreen.apply(name) || isBlue.apply(name);
            hasAlpha = isAlpha.apply(name);
        }

        // read face info
        Global.assume(tokens.length > 2 && tokens[1].equals("face"), "expected faces");
        int nfaces = Integer.valueOf(tokens[2]);

        // skip face properties
        while (!tokens[0].equals("end_header"))
        {
            tokens = token.get();
        }

        Mesh mesh = new Mesh();

        for (int i = 0; i < nverts; i++)
        {
            tokens = token.get();

            Vertex vert = new Vertex(i);
            mesh.graph.add(vert);
            mesh.vattr.add(vert);

            Vect coord = VectSource.create3D();
            Vect color = VectSource.create4D();
            color.set(3, 1);

            for (int j = 0; j < names.size(); j++)
            {
                String type = names.get(j).a;
                String name = names.get(j).b;
                double value = Double.parseDouble(tokens[j]);

                if (hasColor && isRed.apply(name))
                {
                    color.set(0, parseColor.apply(Pair.of(type, value)));
                }
                else if (hasColor && isGreen.apply(name))
                {
                    color.set(1, parseColor.apply(Pair.of(type, value)));
                }
                else if (hasColor && isBlue.apply(name))
                {
                    color.set(2, parseColor.apply(Pair.of(type, value)));
                }
                else if (hasColor && isAlpha.apply(name))
                {
                    color.set(3, parseColor.apply(Pair.of(type, value)));
                }
                else if (name.equals("x"))
                {
                    coord.set(0, value);
                }
                else if (name.equals("y"))
                {
                    coord.set(1, value);
                }
                else if (name.equals("z"))
                {
                    coord.set(2, value);
                }
                else
                {
                    mesh.vattr.set(vert, name, VectSource.create1D(value));
                }
            }

            mesh.vattr.set(vert, Mesh.COORD, coord);

            if (hasColor)
            {
                mesh.vattr.set(vert, Mesh.COLOR, color);
            }
        }

        for (int i = 0; i < nfaces; i++)
        {
            tokens = token.get();
            int n = Integer.valueOf(tokens[0]);

            if (n == 3)
            {
                int a = Integer.valueOf(tokens[1]);
                int b = Integer.valueOf(tokens[2]);
                int c = Integer.valueOf(tokens[3]);

                Vertex va = new Vertex(a);
                Vertex vb = new Vertex(b);
                Vertex vc = new Vertex(c);

                mesh.graph.add(new Face(va, vb, vc));
            }
        }

        dis.close();

        return mesh;
    }
}
