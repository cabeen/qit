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

import qit.base.Logging;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

/** a coder to support a format used by Brown VRL group tools */
public class VrlCurvesCoder
{
    private static final String ATTR = "attr";
    private static final int NCOORD = 3;
    private static final int XCOORD = 0;
    private static final int YCOORD = 1;
    private static final int ZCOORD = 2;
    private static final String DELIMITER = " ";

    public static Curves read(InputStream is) throws IOException
    {
        is = new BufferedInputStream(is);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;

        line = br.readLine();
        if (line == null)
        {
            Logging.error("invalid file");
        }
        
        int nICurve = Integer.parseInt(line);
        
        Curves curves = new Curves(nICurve, new Vect(3));

        for (int i = 0; i < nICurve; i++)
        {
            line = br.readLine();
            if (line == null)
            {
                Logging.error("invalid file");
            }
            
            int npoint = Integer.parseInt(line);

            Curve curve = curves.add(npoint);;

            for (int j = 0; j < npoint; j++)
            {
                line = br.readLine();
                if (line == null)
                {
                    Logging.error("invalid file");
                }
                
                String[] tokens = line.split(DELIMITER);

                double x = Double.parseDouble(tokens[XCOORD]);
                double y = Double.parseDouble(tokens[YCOORD]);
                double z = Double.parseDouble(tokens[ZCOORD]);

                curve.set(Curves.COORD, j, VectSource.create3D(x, y, z));
                
                for (int k = NCOORD; k < tokens.length; k++)
                {
                    double v = Double.parseDouble(tokens[k]);
                    String name = ATTR + String.valueOf(k - NCOORD);
                    curve.set(name, j, VectSource.create1D(v));
                }
            }
        }

        br.close();

        return curves;
    }

    public static void write(Curves curves, OutputStream os) throws IOException
    {
        os = new BufferedOutputStream(os);
        PrintWriter pw = new PrintWriter(os);

        // Write out the data
        pw.println(curves.size());
        for (Curve curve : curves)
        {
            pw.println(curve.size());
            for (Vect vect : curve)
            {
                pw.print(vect.get(0));
                pw.print(DELIMITER);
                pw.print(vect.get(1));
                pw.print(DELIMITER);
                pw.print(vect.get(2));
                pw.println();
            }
        }
        pw.close();
    }
}
