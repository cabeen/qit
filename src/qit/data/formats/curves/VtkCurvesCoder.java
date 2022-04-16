/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.formats.curves;

import com.google.common.collect.Sets;
import qit.base.Logging;
import qit.base.utils.ArrayUtils;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.Vertex;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/** a coder for the visualization toolbox format */
public class VtkCurvesCoder
{
    public static boolean ASCII = false;

    public final static String VTK_FLAG = "@Curves";
    public final static String VTK_IDENT = "# vtk DataFile";
    public final static String VTK_VERSION = "Version 3.0";
    public final static String VTK_ASCII_ENCODING = "ASCII";
    public final static String VTK_BINARY_ENCODING = "BINARY";
    public final static String VTK_DATASET = "DATASET POLYDATA";
    public final static String VTK_POINTS = "POINTS";
    public final static String VTK_FLOAT_TYPE = "float";
    public final static String VTK_DOUBLE_TYPE = "double";
    public final static String VTK_UCHAR_TYPE = "unsigned_char";
    public final static String VTK_LINES = "LINES";
    public final static String VTK_POINT_DATA = "POINT_DATA";
    public final static String VTK_FIELD = "FIELD FieldData";
    public final static String VTK_COLOR_SCALARS = "COLOR_SCALARS";
    public final static String VTK_COLOR_NAME = "RGB_colors";
    public final static String VTK_COLOR = "color";

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
    public static Curves read(InputStream is) throws IOException
    {
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        String line = line = dis.readLine();

        // Check first line for identifier
        if (!line.startsWith(VTK_IDENT))
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
            int nvert = Integer.parseInt(pointTokens[1]);
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

        // Read the lines
        Curves curves = new Curves(new Vect(3));
        {
            while (!line.startsWith(VTK_LINES))
            {
                if ((line = dis.readLine()) == null)
                {
                    Logging.error("Failed to read lines");
                }
            }

            String[] lineTokens = line.split("\\s+");
            int nlines = Integer.parseInt(lineTokens[1]);
            // int total = Integer.parseInt(lineTokens[1]);
            Vect buffer = new Vect(3);

            for (int i = 0; i < nlines; i++)
            {
                int length = 0;
                int[] vidx = null;

                if (binary)
                {
                    length = dis.readInt();
                    int num = length;
                    vidx = new int[num];
                    for (int j = 0; j < num; j++)
                    {
                        vidx[j] = dis.readInt();
                    }
                }
                else
                {
                    int idx = 0;
                    while (vidx == null || idx < length)
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

                        String[] tokens = line.split("\\s+");
                        if (vidx == null)
                        {
                            length = Integer.parseInt(tokens[0]);
                            int num = length;
                            vidx = new int[num];
                            for (int j = 1; j < tokens.length; j++)
                            {
                                vidx[idx++] = Integer.parseInt(tokens[j]);
                            }
                        }
                        else
                        {
                            for (String token : tokens)
                            {
                                vidx[idx++] = Integer.parseInt(token);
                            }
                        }
                    }
                }

                Curve curve = curves.add(length);
                for (int j = 0; j < length; j++)
                {
                    for (int k = 0; k < 3; k++)
                    {
                        buffer.set(k, coords[3 * vidx[j] + k]);
                    }

                    curve.set(Curves.COORD, j, buffer);
                }
            }
        }

        // Read the attributes
        while (line != null && !line.startsWith(VTK_POINT_DATA))
        {
            line = dis.readLine();
        }

        while (line != null && curves.size() > 0)
        {
            line = dis.readLine();
            if (line == null)
            {
                break;
            }

            if (line.startsWith(VTK_COLOR_SCALARS))
            {
                curves.add(Curves.COLOR, VectSource.createND(4));

                String[] tokens = line.trim().split(" ");
                String name = tokens[1].equals(VTK_COLOR_NAME) ? Curves.COLOR : tokens[1];

                int fnum = curves.numVertices();
                int fdim = Integer.parseInt(tokens[2]);
                float[] fvals = new float[fnum * fdim];
                if (binary)
                {
                    ArrayUtils.readArrayBinaryUchar(dis, fvals);
                }
                else
                {
                    ArrayUtils.readArrayText(dis, fvals);
                }

                int idx = 0;
                for (Curve curve : curves)
                {
                    for (int i = 0; i < curve.size(); i++)
                    {
                        Vect fvect = VectSource.create4D(1.0, 1.0, 1.0, 1.0);
                        for (int j = 0; j < fdim; j++)
                        {
                            double fval = fvals[fdim * idx + j];
                            fval = binary ? fval / 255.0 : fval;
                            fvect.set(j, fval);
                        }

                        curve.set(name, i, fvect);
                        idx += 1;
                    }
                }
            }

            if (line.startsWith(VTK_FIELD))
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
                    int fnum = Integer.parseInt(ftokens[2]);
                    String ftype = ftokens[3];

                    if (fname.toLowerCase().equals(Curves.COLOR))
                    {
                        continue;
                    }

                    Vect fvect = new Vect(fdim);

                    float[] fvals = new float[fnum * fdim];
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

                    int cidx = 0;
                    int vidx = 0;

                    curves.add(fname, fvect.proto());
                    Curve curve = curves.get(cidx);
                    for (int i = 0; i < fnum; i++)
                    {
                        // skip empty curves
                        while (curve.size() == 0)
                        {
                            vidx = 0;
                            cidx += 1;
                            curve = curves.get(cidx);
                        }

                        for (int j = 0; j < fdim; j++)
                        {
                            fvect.set(j, fvals[fdim * i + j]);
                        }

                        if (vidx >= curve.size())
                        {
                            Logging.info(String.format("warning: attribute %s has invalid vertex index %d in curve %d", fname, vidx, cidx));
                        }
                        else
                        {
                            curve.set(fname, vidx, fvect);
                        }

                        if (++vidx >= curve.size())
                        {
                            vidx = 0;
                            cidx += 1;
                        }

                        if (cidx < curves.size())
                        {
                            curve = curves.get(cidx);
                        }

                    }
                }
            }
        }

        dis.close();

        return curves;
    }

    public static void write(Curves curves, OutputStream os) throws IOException
    {
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
        boolean binary = !ASCII;
        boolean ftype = true;

        int nvert = 0;
        for (Curve curve : curves)
        {
            nvert += curve.size();
        }

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

        for (Curve curve : curves)
        {
            for (Vect vect : curve.get(Curves.COORD))
            {
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
        }

        if (binary)
        {
            dos.writeBytes("\n");
        }

        dos.writeBytes(VTK_LINES);
        dos.writeBytes(" ");
        dos.writeBytes(String.valueOf(curves.size()));
        dos.writeBytes(" ");
        dos.writeBytes(String.valueOf(curves.size() + nvert));
        dos.writeBytes("\n");

        int idx = 0;
        for (Curve curve : curves)
        {
            int len = curve.size();
            if (binary)
            {
                dos.writeInt(len);
            }
            else
            {
                dos.writeBytes(String.valueOf(len));
            }
            for (int i = 0; i < len; i++)
            {
                if (binary)
                {
                    dos.writeInt(idx++);
                }
                else
                {
                    dos.writeBytes(" ");
                    dos.writeBytes(String.valueOf(idx++));
                    if (i == len - 1)
                    {
                        dos.writeBytes("\n");
                    }
                }
            }
        }

        dos.writeBytes("\n");
        dos.writeBytes(VTK_POINT_DATA);
        dos.writeBytes(" ");
        dos.writeBytes(String.valueOf(nvert));
        dos.writeBytes("\n");

        Set<String> attrs = Sets.newHashSet();
        for (String attr : curves.names())
        {
            if (!attr.startsWith(".") && !attr.equals(Curves.COORD))
            {
                attrs.add(attr);
            }

        }

        if (attrs.contains(VTK_COLOR) && curves.dim(VTK_COLOR) >= 3)
        {
            int cdim = curves.dim(VTK_COLOR);

            dos.writeBytes(VTK_COLOR_SCALARS);
            dos.writeBytes(" ");
            dos.writeBytes(VTK_COLOR_NAME);
            dos.writeBytes(" ");
            dos.writeBytes(cdim == 3 ? "3" : "4");
            dos.writeBytes("\n");

            for (Curve curve : curves)
            {
                for (Vect vect : curve.get(VTK_COLOR))
                {
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
            }
            dos.writeBytes("\n");

            attrs.remove(VTK_COLOR);
        }

        dos.writeBytes(VTK_FIELD);
        dos.writeBytes(" ");
        dos.writeBytes(String.valueOf(attrs.size()));
        dos.writeBytes("\n");

        for (String s : attrs)
        {
            boolean color = "color".equals(s);
            int dim = curves.dim(s);
            dos.writeBytes(s);
            dos.writeBytes(" ");
            dos.writeBytes(String.valueOf(dim));
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

            for (Curve curve : curves)
            {
                for (Vect vect : curve.get(s))
                {
                    for (int i = 0; i < vect.size(); i++)
                    {
                        double v = vect.get(i);
                        if (binary)
                        {
                            if (color)
                            {
                                dos.writeByte((int) Math.round(255f * v)); // assume
                            }
                            else if (ftype)
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
                            if (color)
                            {
                                dos.writeBytes(String.valueOf(v)); // assume
                            }
                            else if (ftype)
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
            }
            dos.writeBytes("\n");
        }

        dos.close();
    }
}
