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

import org.apache.commons.lang3.StringUtils;
import qit.data.datasets.Table;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TxtTableCoder
{
    public static void write(Table table, OutputStream os) throws FileNotFoundException
    {
        PrintWriter pw = new PrintWriter(os);

        List<String> fields = new ArrayList<>(table.getFields());
        Collections.sort(fields);

        String[] header = new String[fields.size()];
        for (int i = 0; i < header.length; i++)
        {
            header[i] = table.getFieldName(i);
        }
        pw.println(StringUtils.join(header, " "));

        for (Object[] row : table)
        {
            pw.println(StringUtils.join(row, " "));
        }
        
        pw.close();
    }

    public static Table read(InputStream is) throws IOException
    {
        Table table = new Table();
        table.withField(Table.NAME);
        table.withField(Table.RED);
        table.withField(Table.GREEN);
        table.withField(Table.BLUE);
        table.withField(Table.ALPHA);
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;

        while ((line = br.readLine()) != null)
        {
            String[] tokens = line.split("\\s+");
            if (tokens.length < 2)
            {
                throw new RuntimeException("invalid text lut");
            }

            int idx = Integer.valueOf(tokens[0]);
            String name = tokens[1];
            int r = tokens.length > 2 ? Integer.valueOf(tokens[2]) : 0;
            int g = tokens.length > 3 ? Integer.valueOf(tokens[3]) : 0;
            int b = tokens.length > 4 ? Integer.valueOf(tokens[4]) : 0;
            int a = tokens.length > 5 ? Integer.valueOf(tokens[5]) : 0;
            
            table.set(idx, Table.NAME, name);
            table.set(idx, Table.RED, String.valueOf(r));
            table.set(idx, Table.GREEN, String.valueOf(g));
            table.set(idx, Table.BLUE, String.valueOf(b));
            table.set(idx, Table.ALPHA, String.valueOf(a));
        }

        br.close();

        return table;
    }
}
