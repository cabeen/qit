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

package qit.data.formats.curves;

import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Vect;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/** an MNI curve dataset coder */
public class MincObjCurvesCoder
{
    private static final String MAGIC = "L";
    private static final String COMMENT = "#";
    private static final int THICKNESS = 3;
    private static final int NFIRST = 3;
    private static final int NRGBA = 4;
    private static final int MAX_COLOR_FLAG = 2;
    private static final int FIRST_INDEX = 0;
    private static final int NUM_IDX = 2;
    private static final String COLOR = "0 1 0 1 1";
    private static final int NUM_EDGE_TOKENS = 3;

    public static Curves read(InputStream is) throws IOException
    {
        is = new BufferedInputStream(is);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;

        // Skip empty/comment lines
        while ((line = br.readLine()) != null)
        {
            if (line.split(COMMENT)[0].trim().length() != 0)
            {
                break;
            }
        }

        // Parse the header
        if (line == null)
        {
            throw new RuntimeException("invalid file");
        }
        
        String[] header_tokens = line.split(COMMENT)[0].trim().split("\\s");

        Global.assume(line.startsWith(MAGIC) && header_tokens.length <= NFIRST, "Cannot parse MNI OBJ, invalid header: " + line);

        int num = Integer.parseInt(header_tokens[NUM_IDX]);

        // Parse the coordinates
        Vect[] vects = new Vect[num];
        for (int i = 0; i < num; i++)
        {
            if ((line = br.readLine()) == null)
            {
                Logging.error("Failed to parse");
            }

            String[] tokens = line.split(COMMENT)[0].trim().split("\\s");
            vects[i] = new Vect(3);
            for (int j = 0; j < 3; j++)
            {
                vects[i].set(j, Double.parseDouble(tokens[j]));
            }
        }

        // Parse the rest of the integers in the file
        List<Integer> edge_data = new LinkedList<Integer>();
        while ((line = br.readLine()) != null)
        {
            for (String token : line.split(COMMENT)[0].trim().split("\\s"))
            {
                edge_data.add(Integer.parseInt(token));
            }
        }

        br.close();

        // Parse the edges from the integers
        int nitems = edge_data.remove(0);
        int color_flag = edge_data.remove(0);

        Global.assume(color_flag <= MAX_COLOR_FLAG, "Invalid color flag found: " + line);

        // Compute the number of color specifiers to skip
        int nrem = color_flag == 0 ? NRGBA : color_flag == 1 ? nitems : num;
        for (int i = 0; i < nrem; i++)
        {
            edge_data.remove(0);
        }

        List<Integer> end_indices = new LinkedList<Integer>();
        for (int i = 0; i < nitems; i++)
        {
            end_indices.add(edge_data.remove(0));
        }

        List<Integer> start_indices = new LinkedList<Integer>();
        start_indices.add(FIRST_INDEX);
        start_indices.addAll(end_indices);

        // Create the curves
        Curves curves = new Curves(new Vect(3));
        for (int i = 0; i < start_indices.size() - 1; i++)
        {
            int sidx = start_indices.get(i);
            int eidx = start_indices.get(i + 1);
            int nvert = eidx - sidx;
            Curve curve = curves.add(nvert);

            for (int j = sidx; j < eidx; j++)
            {
                int idx = edge_data.get(j);
                curve.set(Curves.COORD, j - sidx, vects[idx]);
            }
        }

        return curves;
    }

    public static void write(Curves curves, OutputStream os) throws IOException
    {
        // Compute the total number of points
        int npoints = 0;
        for (Curve curve : curves)
        {
            npoints += curve.size();
        }

        os = new BufferedOutputStream(os);
        PrintWriter pw = new PrintWriter(os);

        // Print the header
        pw.println(String.format("%s %d %d", MAGIC, THICKNESS, npoints));

        // Print the coordinates
        for (Curve curve : curves)
        {
            for (Vect vect : curve.get(Curves.COORD))
            {
                pw.println(String.format("%g %g %g", vect.get(0), vect.get(1), vect.get(2)));
            }
        }

        // Print the number of items
        pw.println(curves.size());

        // Print the color
        pw.println(COLOR);

        // Print the end indices
        int counter = 0;
        for (Curve curve : curves)
        {
            counter += curve.size();
            pw.print(counter);
            pw.println(" ");
        }
        pw.println();

        // Print the indices
        for (int i = 0; i < npoints; i++)
        {
            pw.print(i);
            // Print a new line every so often and at the end
            if (i % NUM_EDGE_TOKENS == 0 || i == curves.size() - 1)
            {
                pw.println();
            }
            else
            {
                pw.println(" ");
            }
        }

        pw.close();
    }
}