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

package qit.data.formats.vects;

import org.apache.commons.lang3.StringUtils;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class TxtVectsCoder
{
    public static String FORMAT_FLAG = "--vects-format";
    public static String FORMAT = "%g";

    public static void write(Vects vects, String fn) throws IOException
    {
        OutputStream os = fn.equals("-") ? System.out : new FileOutputStream(fn);

        if (fn.endsWith(".gz"))
        {
            os = new GZIPOutputStream(os);
        }

        PrintWriter pw = new PrintWriter(os, true);

        for (Vect vect : vects)
        {
            String[] tokens = new String[vect.size()];
            for (int i = 0; i < vect.size(); i++)
            {
                tokens[i] = String.format("%g", vect.get(i));
            }
            pw.println(StringUtils.join(tokens, " "));
        }
        pw.close();
    }

    public static Vects read(String fn) throws IOException
    {
        Vects out = new Vects();

        InputStream is = new FileInputStream(fn);

        if (fn.endsWith(".gz"))
        {
            is = new GZIPInputStream(is);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        String line = null;

        while ((line = br.readLine()) != null)
        {
            // remove hashed comments
            String[] comment = line.split("#");
            String body = comment[0].trim();

            // split on a variety of possible delimiters
            String[] tokens = body.split("(\\s|,|;)+");

            if (tokens.length == 0)
            {
                continue;
            }

            if (out.size() > 0 && tokens.length != out.getDim())
            {
                br.close();
                throw new RuntimeException("inconsistent dimensions");
            }

            Vect vect = new Vect(tokens.length);
            for (int i = 0; i < tokens.length; i++)
            {
                String value = tokens[i].trim().replace("\r", "");
                vect.set(i, Double.valueOf(value));
            }
            out.add(vect);
        }

        br.close();

        return out;
    }
}
