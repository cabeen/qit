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

import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.utils.ArrayUtils;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * a code for the visualization toolkit format
 */
public class VtkMeshCoder
{
    public static boolean ASCII = false;

    public final static String VTK_FLAG = "@Mesh";
    public final static String VTK_IDENT = "# vtk DataFile";
    public final static String VTK_VERSION = "Version 3.0";
    public final static String VTK_ASCII_ENCODING = "ASCII";
    public final static String VTK_BINARY_ENCODING = "BINARY";
    public final static String VTK_DATASET = "DATASET POLYDATA";
    public final static String VTK_POINTS = "POINTS";
    public final static String VTK_FLOAT_TYPE = "float";
    public final static String VTK_DOUBLE_TYPE = "double";
    public final static String VTK_UCHAR_TYPE = "unsigned_char";
    public final static String VTK_POINT_DATA = "POINT_DATA";
    public final static String VTK_FIELD = "FIELD FieldData";
    public final static String VTK_COLOR_SCALARS = "COLOR_SCALARS color";
    public final static String VTK_COLOR = "color";
    public final static String VTK_POLY = "POLYGONS";
    public final static String VTK_STRIPS = "TRIANGLE_STRIPS";
    public final static int VTK_POLY_SIZE = 3;

    public static boolean detect(String fn)
    {
        try
        {
            InputStream is = new FileInputStream(fn);

            if (fn.endsWith(".gz"))
            {
                is = new GZIPInputStream(is);
            }

            DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
            String line = null;

            // Check first line for identifier
            if (!(line = dis.readLine()).startsWith(VTK_IDENT))
            {
                return false;
            }

            // Skip the comment
            String comment = dis.readLine();

            return comment.contains(VTK_FLAG);
        }
        catch (IOException e)
        {
        }

        return false;
    }

    @SuppressWarnings("deprecation")
    public static Mesh read(InputStream is) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        String line = null;

        // Check first line for identifier
        if (!(line = dis.readLine()).startsWith(VTK_IDENT))
        {
            Logging.error("Stream does not encode VTK polydata");
        }

        // Skip the comment
        line = dis.readLine();

        // Check the encoding
        line = dis.readLine();
        boolean binary = line.startsWith(VTK_BINARY_ENCODING);

        // Check the data set type
        while ((line = dis.readLine()) != null && !line.startsWith(VTK_DATASET))
        {
        }
        if (line == null)
        {
            Logging.error("Stream does not encode a VTK polydata");
        }

        // Read the points
        int nvert;
        float[] coords;
        {
            while (!line.startsWith(VTK_POINTS))
            {
                if ((line = dis.readLine()) == null)
                {
                    Logging.error("Failed to read points");
                }
            }

            String[] pointTokens = line.split("\\s+");
            nvert = Integer.parseInt(pointTokens[1]);
            String ptype = pointTokens[2];
            coords = new float[nvert * 3];

            if (binary)
            {
                if (ptype.equals(VTK_FLOAT_TYPE))
                {
                    ArrayUtils.readArrayBinaryFloat(dis, coords);
                }
                else if (ptype.equals(VTK_DOUBLE_TYPE))
                {
                    ArrayUtils.readArrayBinaryDouble(dis, coords);
                }
                else if (ptype.equals(VTK_UCHAR_TYPE))
                {
                    ArrayUtils.readArrayBinaryUchar(dis, coords);
                }
                else
                {
                    Logging.error("invalid point type: " + ptype);
                }
            }
            else
            {
                ArrayUtils.readArrayText(dis, coords);
            }
        }

        // initialize the mesh
        Mesh mesh = new Mesh();
        for (int i = 0; i < nvert; i++)
        {
            double x = coords[i * 3 + 0];
            double y = coords[i * 3 + 1];
            double z = coords[i * 3 + 2];

            Vertex vert = new Vertex(i);
            mesh.graph.add(vert);
            mesh.vattr.add(vert);
            mesh.vattr.set(vert, Mesh.COORD, VectSource.create3D(x, y, z));
        }

        // Read the faces
        {
            while (!line.startsWith(VTK_POLY) && !line.startsWith(VTK_STRIPS))
            {
                if ((line = dis.readLine()) == null)
                {
                    Logging.error("Failed to read faces");
                }
            }

            if (line.startsWith(VTK_POLY))
            {
                String[] ftokens = line.split("\\s+");
                int nfaces = Integer.parseInt(ftokens[1]);
                int nints = Integer.parseInt(ftokens[2]);

                if (nints % (VTK_POLY_SIZE + 1) != 0 || nints / (VTK_POLY_SIZE + 1) != nfaces)
                {
                    Logging.error("invalid poly header");
                }

                int fcount = 0;
                if (binary)
                {
                    for (int i = 0; i < nfaces; i++)
                    {
                        int n = dis.readInt();
                        if (n > 3)
                        {
                            Logging.error("Only triangle meshes are supported");
                        }

                        int a = dis.readInt();
                        int b = dis.readInt();
                        int c = dis.readInt();

                        Vertex va = new Vertex(a);
                        Vertex vb = new Vertex(b);
                        Vertex vc = new Vertex(c);

                        mesh.graph.add(new Face(va, vb, vc));
                    }
                }
                else
                {
                    while (fcount < nfaces)
                    {
                        while (true)
                        {
                            line = dis.readLine();

                            if (line == null)
                            {
                                Logging.error("Unexpected end of line");
                            }

                            if (line.length() > 0)
                            {
                                break;
                            }
                        }

                        String[] fitokens = line.trim().split("\\s+");
                        if (fitokens.length >= VTK_POLY_SIZE + 1)
                        {
                            int a = Integer.parseInt(fitokens[1]);
                            int b = Integer.parseInt(fitokens[2]);
                            int c = Integer.parseInt(fitokens[3]);

                            Vertex va = new Vertex(a);
                            Vertex vb = new Vertex(b);
                            Vertex vc = new Vertex(c);

                            try
                            {
                                mesh.graph.add(new Face(va, vb, vc));
                            }
                            catch (Exception e)
                            {
                                Logging.info("warning, failed to add face: " + line);
                            }

                            fcount++;
                        }

                    }
                }
            }
            else if (line.startsWith(VTK_STRIPS))
            {
                String[] ftokens = line.split("\\s+");
                int numStrips = Integer.parseInt(ftokens[1]);
                // int numIntegers = Integer.parseInt(ftokens[2]);

                if (binary)
                {
                    for (int i = 0; i < numStrips; i++)
                    {
                        int numIndices = dis.readInt();
                        Global.assume(numIndices > 0, "invalid triangle strip");

                        int[] strip = new int[numIndices];
                        for (int j = 0; j < numIndices; j++)
                        {
                            strip[j] = dis.readInt();
                        }

                        for (int j = 0; j < numIndices - 2; j++)
                        {
                            // alternate face orientations so they're consistent
                            if (j % 2 == 0)
                            {
                                Vertex va = new Vertex(strip[j]);
                                Vertex vb = new Vertex(strip[j + 1]);
                                Vertex vc = new Vertex(strip[j + 2]);
                                mesh.graph.add(new Face(va, vb, vc));
                            }
                            else
                            {
                                Vertex va = new Vertex(strip[j]);
                                Vertex vb = new Vertex(strip[j + 2]);
                                Vertex vc = new Vertex(strip[j + 1]);
                                mesh.graph.add(new Face(va, vb, vc));
                            }
                        }
                    }
                }
                else
                {
                    int stripCount = 0;
                    while (stripCount < numStrips)
                    {
                        Global.assume((line = dis.readLine()) != null, "Failed to read faces");

                        String[] fitokens = line.split("\\s+");
                        int numIndicies = Integer.parseInt(fitokens[0]);
                        Global.assume(numIndicies == fitokens.length - 1, "invalid triangle strip");

                        int[] strip = new int[numIndicies];
                        for (int j = 0; j < numIndicies; j++)
                        {
                            strip[j] = Integer.parseInt(fitokens[j + 1]);
                        }

                        for (int j = 0; j < numIndicies - 2; j++)
                        {
                            // alternate face orientations so they're consistent
                            if (j % 2 == 0)
                            {
                                Vertex va = new Vertex(strip[j]);
                                Vertex vb = new Vertex(strip[j + 1]);
                                Vertex vc = new Vertex(strip[j + 2]);
                                mesh.graph.add(new Face(va, vb, vc));
                            }
                            else
                            {
                                Vertex va = new Vertex(strip[j]);
                                Vertex vb = new Vertex(strip[j + 2]);
                                Vertex vc = new Vertex(strip[j + 1]);
                                mesh.graph.add(new Face(va, vb, vc));
                            }

                        }

                        stripCount += 1;
                    }
                }
            }
        }

        while (line != null)
        {
            line = dis.readLine();
            if (line == null)
            {
                break;
            }

            if (line.startsWith(VTK_COLOR_SCALARS))
            {
                int fdim = Integer.parseInt(line.split(" ")[2]);
                float[] fvals = new float[nvert * fdim];
                if (binary)
                {
                    ArrayUtils.readArrayBinaryUchar(dis, fvals);
                }
                else
                {
                    ArrayUtils.readArrayText(dis, fvals);
                }

                for (int i = 0; i < nvert; i++)
                {
                    Vect fvect = VectSource.createND(fdim);
                    for (int j = 0; j < fdim; j++)
                    {
                        double fval = fvals[fdim * i + j] / 255.0;
                        fvect.set(j, fval);
                    }

                    if (fdim == 3)
                    {
                        Vect nfvect = VectSource.create4D();
                        nfvect.set(0, fvect.get(0));
                        nfvect.set(1, fvect.get(1));
                        nfvect.set(2, fvect.get(2));
                        nfvect.set(3, 1.0);
                        fvect = nfvect;
                    }

                    mesh.vattr.set(new Vertex(i), Mesh.COLOR, fvect);
                }
            }

            if (line.startsWith(VTK_FIELD) && nvert > 0)
            {
                int nfields = Integer.parseInt(line.split(" ")[2]);
                for (int f = 0; f < nfields; f++)
                {
                    String[] ftokens = null;
                    while (ftokens == null || ftokens.length < 3)
                    {
                        if ((line = dis.readLine()) == null)
                        {
                            Logging.error("Failed to read attribute field header");
                        }
                        else
                        {
                            ftokens = line.split(" ");
                        }
                    }

                    String fname = ftokens[0];
                    int fdim = Integer.parseInt(ftokens[1]);
                    int vidx = Integer.parseInt(ftokens[2]);
                    String ftype = ftokens[3];

                    Vect fvect = new Vect(fdim);

                    float[] fvals = new float[vidx * fdim];
                    if (binary)
                    {
                        if (ftype.equals(VTK_FLOAT_TYPE))
                        {
                            ArrayUtils.readArrayBinaryFloat(dis, fvals);
                        }
                        else if (ftype.equals(VTK_DOUBLE_TYPE))
                        {
                            ArrayUtils.readArrayBinaryDouble(dis, fvals);
                        }
                        else if (ftype.equals(VTK_UCHAR_TYPE))
                        {
                            ArrayUtils.readArrayBinaryUchar(dis, fvals);
                        }
                        else
                        {
                            Logging.error("invalid point type: " + ftype);
                        }
                    }
                    else
                    {
                        ArrayUtils.readArrayText(dis, fvals);
                    }

                    for (int i = 0; i < vidx; i++)
                    {
                        for (int j = 0; j < fdim; j++)
                        {
                            fvect.set(j, fvals[fdim * i + j]);
                        }

                        mesh.vattr.set(new Vertex(i), fname, fvect);
                    }
                }
            }
        }

        dis.close();

        return mesh;
    }

    public static void write(Mesh mesh, OutputStream os) throws IOException
    {
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
        boolean binary = !ASCII;
        boolean ftype = true;

        int nvert = mesh.graph.numVertex();

        dos.writeBytes(VTK_IDENT);
        dos.writeBytes(" ");
        dos.writeBytes(VTK_VERSION);
        dos.writeBytes("\n");

        dos.writeBytes(VTK_FLAG);
        dos.writeBytes(" Generated on ");
        dos.writeBytes(new Date().toString());
        dos.writeBytes("\n");

        if (binary)
        {
            dos.writeBytes(VTK_BINARY_ENCODING);
        }
        else
        {
            dos.writeBytes(VTK_ASCII_ENCODING);
        }
        dos.writeBytes("\n");

        dos.writeBytes(VTK_DATASET);
        dos.writeBytes("\n");

        dos.writeBytes(VTK_POINTS);
        dos.writeBytes(" ");
        dos.writeBytes(String.valueOf(nvert));
        dos.writeBytes(" ");
        if (ftype)
        {
            dos.writeBytes(VTK_FLOAT_TYPE);
        }
        else
        {
            dos.writeBytes(VTK_DOUBLE_TYPE);
        }
        dos.writeBytes("\n");

        List<Vertex> verts = new ArrayList<>(mesh.graph.numVertex());
        for (Vertex vert : mesh.graph.verts())
        {
            verts.add(vert);
        }

        Map<Vertex, Integer> vmap = new HashMap<>();
        for (int i = 0; i < verts.size(); i++)
        {
            vmap.put(verts.get(i), i);
        }

        // write points
        for (Vertex vert : verts)
        {
            Vect vect = mesh.vattr.get(vert, Mesh.COORD);
            for (int i = 0; i < vect.size(); i++)
            {
                double v = vect.get(i);
                if (binary)
                {
                    if (ftype)
                    {
                        dos.writeFloat((float) v);
                    }
                    else
                    {
                        dos.writeDouble(v);
                    }
                }
                else
                {
                    if (i != 0)
                    {
                        dos.writeBytes(" ");
                    }
                    if (ftype)
                    {
                        dos.writeBytes(String.valueOf((float) v));
                    }
                    else
                    {
                        dos.writeBytes(String.valueOf(v));
                    }
                    if (i == vect.size() - 1)
                    {
                        dos.writeBytes("\n");
                    }
                }
            }
        }
        dos.writeBytes("\n");

        // write faces
        dos.writeBytes(VTK_POLY);
        dos.writeBytes(" ");
        dos.writeBytes(String.valueOf(mesh.graph.numFace()));
        dos.writeBytes(" ");
        dos.writeBytes(String.valueOf((VTK_POLY_SIZE + 1) * mesh.graph.numFace()));
        dos.writeBytes("\n");

        for (Face f : mesh.graph.faces())
        {
            if (binary)
            {
                dos.writeInt(VTK_POLY_SIZE);
                dos.writeInt(vmap.get(f.getA()));
                dos.writeInt(vmap.get(f.getB()));
                dos.writeInt(vmap.get(f.getC()));
            }
            else
            {
                dos.writeBytes(" ");
                dos.writeBytes(String.valueOf(VTK_POLY_SIZE));
                dos.writeBytes(" ");
                dos.writeBytes(String.valueOf(vmap.get(f.getA())));
                dos.writeBytes(" ");
                dos.writeBytes(String.valueOf(vmap.get(f.getB())));
                dos.writeBytes(" ");
                dos.writeBytes(String.valueOf(vmap.get(f.getC())));
                dos.writeBytes("\n");
            }
        }
        dos.writeBytes("\n");

        // write attributes
        dos.writeBytes(VTK_POINT_DATA);
        dos.writeBytes(" ");
        dos.writeBytes(String.valueOf(nvert));
        dos.writeBytes("\n");

        Set<String> attrs = Sets.newHashSet();
        for (String attr : mesh.vattr.attrs())
        {
            if (!attr.startsWith(".") && !attr.equals(Mesh.COORD))
            {
                attrs.add(attr);
            }

        }

        if (attrs.contains(VTK_COLOR) && mesh.vattr.dim(VTK_COLOR) >= 3)
        {
            int cdim = mesh.vattr.dim(VTK_COLOR);

            dos.writeBytes(VTK_COLOR_SCALARS);
            dos.writeBytes(" ");
            dos.writeBytes(cdim == 3 ? "3" : "4");
            dos.writeBytes("\n");

            for (Vertex vert : verts)
            {
                Vect vect = mesh.vattr.get(vert, VTK_COLOR);
                for (int i = 0; i < Math.min(4, cdim); i++)
                {
                    double dv = vect.get(i);
                    int iv = (int) (dv * 255.0);
                    if (binary)
                    {
                        dos.writeByte((int) iv);
                    }
                    else
                    {
                        if (i != 0)
                        {
                            dos.writeBytes(" ");
                        }
                        dos.writeBytes(String.valueOf(dv));
                    }
                }
                if (!binary)
                {
                    dos.writeBytes("\n");
                }
            }
            dos.writeBytes("\n");
            attrs.remove(VTK_COLOR);
        }

        if (verts.size() > 0)
        {
            dos.writeBytes(VTK_FIELD + " " + attrs.size() + "\n");

            for (String s : attrs)
            {
                boolean color = Mesh.COLOR.equals(s);
                int dim = mesh.vattr.get(verts.get(0), s).size();
                dos.writeBytes(s);
                dos.writeBytes(" ");
                dos.writeBytes(String.valueOf(dim));
                dos.writeBytes(" ");
                dos.writeBytes(String.valueOf(nvert));
                dos.writeBytes(" ");
                if (color)
                {
                    dos.writeBytes(VTK_UCHAR_TYPE);
                }
                else if (ftype)
                {
                    dos.writeBytes(VTK_FLOAT_TYPE);
                }
                else
                {
                    dos.writeBytes(VTK_DOUBLE_TYPE);
                }
                dos.writeBytes("\n");

                for (Vertex v : verts)
                {
                    Vect vect = mesh.vattr.get(v, s);
                    for (int i = 0; i < dim; i++)
                    {
                        double val = vect.get(i);
                        if (binary)
                        {
                            if (color)
                            {
                                int c = (int) Math.max(0, Math.min(255, Math.round(255f * val)));
                                dos.writeByte((char) c);
                            }
                            else if (ftype)
                            {
                                dos.writeFloat((float) val);
                            }
                            else
                            {
                                dos.writeDouble(val);
                            }
                        }
                        else
                        {
                            if (i != 0)
                            {
                                dos.writeBytes(" ");
                            }
                            if (color)
                            {
                                int c = (int) Math.max(0, Math.min(255, Math.round(255f * val)));
                                dos.writeBytes(String.valueOf(c));
                            }
                            else if (ftype)
                            {
                                dos.writeBytes(String.valueOf((float) val));
                            }
                            else
                            {
                                dos.writeBytes(String.valueOf(v));
                            }
                            if (i == dim - 1)
                            {
                                dos.writeBytes("\n");
                            }
                        }
                    }
                }
                dos.writeBytes("\n");
            }
        }

        dos.close();
    }
}
