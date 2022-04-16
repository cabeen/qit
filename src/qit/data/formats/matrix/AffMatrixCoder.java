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

package qit.data.formats.matrix;

import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/*
 * An affine transformation produced by DTI-TK
 *
 * Example file:
<pre> 
 MATRIX
       1.021004890842118     -0.06423193967073045      0.04264945868553273
     0.07833239320835178        1.024599160348651       -0.284416765124265
    -0.03901881930543296       0.1829675846227298        1.028739613453591
VECTOR
       4.732884007365726        33.69504942108425       -35.19452694852902
</pre>
 */
public class AffMatrixCoder
{
    public static Matrix read(String fn) throws IOException
    {
        Matrix out = new Matrix(4, 4);

        BufferedReader br = new BufferedReader(new FileReader(fn));
        
        // skip matrix line
        String line = br.readLine();

        // read matrix
        for (int i = 0; i < 3; i++)
        {
            line = br.readLine();
            if (line == null)
            {
                br.close();
                throw new RuntimeException("unexpected end of file");
            }
            
            String[] tokens = line.split("\\s+");
            
            for (int j = 0; j < 3; j++)
            {
                out.set(i, j, Double.valueOf(tokens[j+1]));
            }
        }
        
        // skip vector line
        line = br.readLine();
        if (line == null)
        {
            br.close();
            throw new RuntimeException("unexpected end of file");
        }
        
        // read translation
        line = br.readLine();
        if (line == null)
        {
            br.close();
            throw new RuntimeException("unexpected end of file");
        }
        
        String[] tokens = line.split("\\s+");

        for (int i = 0; i < 3; i++)
        {
            out.set(i, 3, Double.valueOf(tokens[i+1]));
        }
        
        out.set(3, 3, 1);
        
        br.close();

        return out;
    }

    public static void write(Affine affine, OutputStream os) throws IOException
    {
        Matrix matrix = affine.linear();
        Vect vector = affine.trans();

        PrintWriter pw = new PrintWriter(new BufferedOutputStream(os));
        pw.write("MATRIX\n");
        for (int i = 0; i < matrix.rows(); i++)
        {
            for (int j = 0; j < matrix.cols(); j++)
            {
                pw.write(String.format(" %g", matrix.get(i, j)));
            }
            pw.write("\n");
        }

        pw.write("VECTOR\n");
        for (int j = 0; j < vector.size(); j++)
        {
            pw.write(String.format(" %g", vector.get(j)));
        }
        pw.write("\n");
        pw.close();
    }
}
