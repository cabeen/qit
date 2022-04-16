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

package qit.data.formats.table;

import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Table;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** a coder for the freesurfer annotation format */
public class AnnotTableCoder
{
    public static Table read(InputStream is) throws IOException
    {
        DataInputStream dis = new DataInputStream(is);

        // Number of vertices
        int n = dis.readInt();
        // Do something with these at some point!
        int[] vidx = new int[n];
        int[] vatt = new int[n];
        Table table = new Table();
        table.withField(Table.NAME);
        table.withField(Table.INDEX);
        table.withField(Table.RED);
        table.withField(Table.GREEN);
        table.withField(Table.BLUE);
        table.withField(Table.ALPHA);

        for (int i = 0; i < n; i++)
        {
            // Vertex index
            vidx[i] = dis.readInt();

            // Vertex label
            vatt[i] = dis.readInt();
        }

        // Whether a map is present
        int map = dis.readInt();

        if (map != 0)
        {
            // Number of entries
            int ne = dis.readInt();

            if (ne > 0)
            {
                // Original tab
                int tlen = dis.readInt();
                for (int i = 0; i < tlen; i++)
                {
                    dis.readInt();
                }

                for (int i = 0; i < ne; i++)
                {
                    // The number of characters in the name
                    int len = dis.readInt() - 1;
                    char[] namech = new char[len];

                    // Label name
                    for (int j = 0; j < len; j++)
                    {
                        namech[j] = (char) dis.readByte();
                    }

                    // Skip null terminator
                    dis.readByte();

                    String name = new String(namech);
                    int r = dis.readInt();
                    int g = dis.readInt();
                    int b = dis.readInt();
                    int a = 255 - dis.readInt();
                    int idx = r + g * 256 + b * 256 * 256;

                    table.set(idx, Table.NAME, name);
                    table.set(idx, Table.INDEX, String.valueOf(idx));
                    table.set(idx, Table.RED, String.valueOf(r));
                    table.set(idx, Table.GREEN, String.valueOf(g));
                    table.set(idx, Table.BLUE, String.valueOf(b));
                    table.set(idx, Table.ALPHA, String.valueOf(a));
                }
            }
            else
            {
                Logging.info("using legacy annot version");
                int version = -ne;
                Global.assume(version == 2, "Unsupported format version");

                ne = dis.readInt();

                // Original tab
                int tlen = dis.readInt();
                char[] orig_tab = new char[tlen];
                for (int i = 0; i < tlen; i++)
                {
                    orig_tab[i] = (char) dis.readByte();
                }

                // Number of entries to read
                int ner = dis.readInt();
                for (int i = 0; i < ner; i++)
                {
                    // Structure
                    int arridx = dis.readInt();
                    Global.assume(arridx >= 0, "Invalid label index");

                    // The number of characters in the name
                    int len = dis.readInt() - 1;
                    char[] namech = new char[len];

                    // Label name
                    for (int j = 0; j < len; j++)
                    {
                        namech[j] = (char) dis.readByte();
                    }

                    // Skip null terminator
                    dis.readByte();

                    String name = new String(namech);
                    int r = dis.readInt();
                    int g = dis.readInt();
                    int b = dis.readInt();
                    int a = 255 - dis.readInt();
                    int idx = r + g * 256 + b * 256 * 256;

                    table.set(idx, Table.NAME, name);
                    table.set(idx, Table.INDEX, String.valueOf(idx));
                    table.set(idx, Table.RED, String.valueOf(r));
                    table.set(idx, Table.GREEN, String.valueOf(g));
                    table.set(idx, Table.BLUE, String.valueOf(b));
                    table.set(idx, Table.ALPHA, String.valueOf(a));
                }
            }
        }

        dis.close();

        return table;
    }
}
