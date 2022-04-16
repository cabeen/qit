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

import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * a coder for the freesurfer annotation format
 */
public class AnnotVectsCoder
{
    // based on "read_annotation.m"
    public static Vects read(String fn) throws IOException
    {
        DataInputStream dis = new DataInputStream(new FileInputStream(fn));

        int n = dis.readInt();
        int[] vertices = new int[n];
        int[] labels = new int[n];

        for (int i = 0; i < n; i++)
        {
            // Vertex index
            vertices[i] = dis.readInt();

            // Vertex label
            labels[i] = dis.readInt();

            // note: these labels depend on the embedded colormap...
        }

        dis.close();

        // Next we indulge some oddities of Freesurfer's software implementation,
        // in order to map the vertex labels to those in FreeSurferColorLUT.txt
        // The index in each entry "labels" is a ABGR color that we use to
        // look up the correct label.  This is NOT a bijection, so we have to
        // use the filename to resolve things.  Yes, I agree, this is bizarre.
        //
        // see: https://surfer.nmr.mgh.harvard.edu/fswiki/LabelsClutsAnnotationFiles

        boolean left = fn.contains("lh");
        String bn = null;
        if (fn.contains("aparc.a2009s"))
        {
            bn = "share/fs/parcs/aparc.a2009s+aseg.csv";
        }
        else
        {
            bn = "share/fs/parcs/aparc+aseg.csv";

        }

        if (bn != null)
        {
            String lutFn = PathUtils.join(Global.getRoot(), bn);
            Table lut = Table.read(lutFn);

            Map<Integer, Integer> big = Maps.newHashMap();
            Map<Integer, Integer> little = Maps.newHashMap();

            for (Integer key : lut.keys())
            {
                Record row = lut.getRecord(key);
                int r = Integer.valueOf(row.get("r"));
                int g = Integer.valueOf(row.get("g"));
                int b = Integer.valueOf(row.get("b"));
                int index = Integer.valueOf(row.get("index"));
                String name = row.get("name");

                if (!left && name.contains("lh") || name.contains("Left"))
                {
                    continue;
                }

                if (left && name.contains("rh") || name.contains("Right"))
                {
                    continue;
                }

                Pair<String, Integer> entry = Pair.of(name, index);

                {
                    int label = 0;
                    label |= (b & 0xFF) << 16;
                    label |= (g & 0xFF) << 8;
                    label |= (r & 0xFF) << 0;

                    Global.assume(!little.containsKey(label), "duplicate label found");
                    little.put(label, index);
                }

                {
                    int label = 0;
                    label |= (r & 0xFF) << 24;
                    label |= (g & 0xFF) << 16;
                    label |= (b & 0xFF) << 8;

                    Global.assume(!big.containsKey(label), "duplicate label found");
                    big.put(label, index);
                }
            }

            for (int i = 0; i < n; i++)
            {
                int label = labels[i];
                if (little.containsKey(label))
                {
                    labels[i] = little.get(label);
                }
                else if (big.containsKey(label))
                {
                    labels[i] = big.get(label);
                }
            }
        }

        Vects vects = new Vects();
        for (int i = 0; i < n; i++)
        {
            vects.add(VectSource.create1D(labels[i]));
        }
        return vects;
    }
}
