package qit.data.formats.curves;

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

import qit.base.Logging;
import qit.base.structs.LEDataInputStream;
import qit.base.structs.LEDataOutputStream;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;

import java.io.*;
import java.util.List;

// a coder for the MRtrix curves format
// reference: http://mrtrix.readthedocs.io/en/latest/getting_started/image_data.html
public class TckCurvesCoder
{
    public final static String FLOAT_BE = "Float32BE";
    public final static String FLOAT_LE = "Float32LE";
    public final static String MAGIC = "mrtrix tracks";
    public final static String DATATYPE = "datatype";
    public final static String START = "file";
    public final static String END = "END";

    @SuppressWarnings("deprecation")
    public static Curves read(String path) throws IOException
    {
        boolean little = false;
        Integer start = null;

        {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
            String line = dis.readLine();

            if (!line.startsWith(MAGIC))
            {
                Logging.error("Stream does not encode VTK polydata");
            }

            while (true)
            {
                line = dis.readLine();

                if (line.startsWith(END))
                {
                    break;
                }

                if (!line.contains(":"))
                {
                    Logging.error("Header did not encode valid key value pair: " + line);
                }

                String[] tokens = line.split(":");

                if (tokens.length != 2)
                {
                    Logging.error("Header did not encode valid key value pair: " + line);
                }

                String key = tokens[0].trim();
                String value = tokens[1].trim();

                if (key.equals(DATATYPE))
                {
                    if (value.equals(FLOAT_LE))
                    {
                        little = true;
                    }
                    else if (!value.equals(FLOAT_BE))
                    {
                        Logging.error("unknown datatype: " + value);
                    }
                }

                if (key.equals(START))
                {
                    String[] stokens = value.split(" ");
                    start = Integer.valueOf(stokens[stokens.length - 1]);

                    if (start <= 0)
                    {
                        Logging.error("invalid start: " + start);
                    }
                }
            }

            dis.close();
        }

        DataInput dis = null;
        if (little)
        {
            dis = new LEDataInputStream(new BufferedInputStream(new FileInputStream(path)));
        }
        else
        {
            dis = new DataInputStream(new BufferedInputStream(new FileInputStream(path)));
        }

        // skip the header
        for (int i = 0; i < start; i++)
        {
            dis.readByte();
        }

        Curves out = new Curves();

        try
        {

            List<Vect> vects = new Vects();
            while (true)
            {
                float x = dis.readFloat();
                float y = dis.readFloat();
                float z = dis.readFloat();

                if (Float.isNaN(x) || Float.isInfinite(x))
                {
                    Curve curve = out.add(vects.size());
                    for (int i = 0; i < vects.size(); i++)
                    {
                        curve.set(Curves.COORD, i, vects.get(i));
                    }
                    vects.clear();

                    if (Float.isInfinite(x))
                    {
                        break;
                    }
                }
                else
                {
                    vects.add(VectSource.create3D(x, y, z));
                }
            }
        }
        catch (EOFException e)
        {
            Logging.info("warning file ended early");
        }

        if (little)
        {
            ((LEDataInputStream) dis).close();
        }
        else
        {
            ((DataInputStream) dis).close();
        }

        return out;
    }

    public static void write(Curves curves, OutputStream os) throws IOException
    {
        int startByte = 512;
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("%s\n", MAGIC));
        builder.append(String.format("%s: %s\n", DATATYPE, FLOAT_LE));
        builder.append(String.format("%s: . %s\n", START, String.valueOf(startByte)));
        builder.append(String.format("%s\n", END));
        String header = builder.toString();

        int remainder = startByte - header.length();

        LEDataOutputStream dos = new LEDataOutputStream(os);
        dos.writeBytes(header);
        for (int i = 0; i < remainder; i++)
        {
            dos.writeByte(0);
        }

        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);

            for (int j = 0; j < curve.size(); j++)
            {
                Vect p = curve.get(j);
                float x = (float) p.getX();
                float y = (float) p.getY();
                float z = (float) p.getZ();

                dos.writeFloat(x);
                dos.writeFloat(y);
                dos.writeFloat(z);
            }

            if (i < curves.size() - 1)
            {
                dos.writeFloat(Float.NaN);
                dos.writeFloat(Float.NaN);
                dos.writeFloat(Float.NaN);
            }
            else
            {
                dos.writeFloat(Float.POSITIVE_INFINITY);
                dos.writeFloat(Float.POSITIVE_INFINITY);
                dos.writeFloat(Float.POSITIVE_INFINITY);
            }
        }

        dos.close();
    }
}
