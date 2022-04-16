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

package qit.data.source;

import qit.base.structs.Indexed;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Record;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.VectsUtils;

import java.util.List;

/** utilities for creating vects */
public class VectsSource
{

    public static Vects create()
    {
        return new Vects();
    }

    public static Vects create(double[][] vals)
    {
        Vects out = new Vects();
        for (int i = 0; i < vals.length; i++)
        {
            out.add(VectSource.create(vals[i]));
        }

        return out;
    }

    public static Vects create(Table table)
    {
        int num = table.getNumRecords();
        int dim = table.getNumFields() - 1;

        String base = Vects.ELEM;
        for (String field : table.getSchema())
        {
            if (field.contains("_"))
            {
                base = field.split("_")[0];
            }
        }

        Vects out = new Vects(num);
        for (Object[] row : table)
        {
            Vect v = new Vect(dim);
            for (int j = 0; j < dim; j++)
            {
                v.set(j, Double.valueOf(row[table.getIndex(vectElemName(base, j))].toString()));
            }
        }

        return out;
    }

    public static Vects paste(List<Vects> vects)
    {
        int n = vects.get(0).size();
        int m = vects.size();
        int dim = 0;
        for (Vects vs : vects)
        {
            if (vs.size() != n)
            {
                throw new RuntimeException("Invalid sizes");
            }
            else
            {
                dim += vs.getDim();
            }
        }

        Vects out = new Vects();
        for (int i = 0; i < n; i++)
        {
            Vect v = new Vect(dim);
            int idx = 0;
            for (int j = 0; j < m; j++)
            {
                Vect p = vects.get(j).get(i);
                for (int k = 0; k < p.size(); k++)
                {
                    v.set(idx++, p.get(k));
                }
            }
            out.add(v);
        }

        return out;
    }

    public static Vects unpack(int dim, Vect v)
    {
        if (v.size() % dim != 0)
        {
            throw new RuntimeException("invalid vector channel");
        }

        int num = v.size() / dim;
        Vects out = new Vects(num);
        for (int i = 0; i < num; i++)
        {
            Vect nv = new Vect(dim);
            for (int j = 0; j < dim; j++)
            {
                nv.set(j, v.get(i * dim + j));
            }
            out.add(nv);
        }

        return out;
    }

    public static Vects pack(Indexed<Vects> vs)
    {
        Vects out = new Vects(vs.size());
        for (Vects v : vs)
        {
            out.add(VectsUtils.pack(v));
        }
        return out;
    }

    public static Vects pack(Iterable<Vects> vs)
    {
        Vects out = new Vects();
        for (Vects v : vs)
        {
            out.add(VectsUtils.pack(v));
        }
        return out;
    }

    public static Vects copy(Vects vects)
    {
        Vects out = new Vects(vects.size());
        for (Vect v : vects)
        {
            out.add(v.copy());
        }
        return out;
    }

    public static Vects copy(int num, Vect value)
    {
        Vects out = new Vects(num);
        for (int i = 0; i < num; i++)
        {
            out.add(value.copy());
        }
        return out;
    }

    public static Table createTable(Vects vects)
    {
        int dim = vects.getDim();

        Schema schema = new Schema();
        schema.add(Vects.INDEX);

        for (int i = 0; i < dim; i++)
        {
            schema.add(vectElemName(Vects.ELEM, i));
        }

        Table out = new Table(schema);

        int idx = 0;
        Record map = new Record();
        for (int i = 0; i < vects.size(); i++)
        {
            map.with(Vects.INDEX, String.valueOf(i));
            for (int j = 0; j < dim; j++)
            {
                map.with(vectElemName(Vects.ELEM, j), String.valueOf(vects.get(j)));
            }

            out.addRecord(idx++, map);
            map.clear();
        }

        return out;
    }

    public static Vects create(List<Vect> vects)
    {
        return new Vects(vects);
    }

    public static Vects create(int[][] vals)
    {
        int num = vals.length;
        Vects out = new Vects(num);

        if (num > 0)
        {
            for (int[] val : vals)
            {
                out.add(VectSource.create(val));
            }
        }

        return out;
    }

    public static Vects create(Matrix mat)
    {
        Vects out = new Vects(mat.rows());
        for (int i = 0; i < mat.cols(); i++)
        {
            out.add(mat.getRow(i));
        }
        return out;
    }

    public static Vects create(Vect[] vs)
    {
        return new Vects(vs);
    }

    public static Vects create1D(Vect vect)
    {
        Vects out = new Vects();
        for (int i = 0; i < vect.size(); i++)
        {
            out.add(VectSource.create1D(vect.get(i)));
        }
        return out;
    }

    public static Vects create(Vect vect)
    {
        Vects out = new Vects();
        out.add(vect);
        return out;
    }

    public static Vects empty()
    {
        return new Vects();
    }

    public static String vectElemName(String base, int i)
    {
        return base + "_" + i;
    }

    public static int vectElemIdx(String base, String name)
    {
        if (name.startsWith(base))
        {
            return Integer.valueOf(name.split("_")[1]);
        }
        else
        {
            return -1;
        }
    }

    public static Vects sphere(int resolution, boolean half)
    {
        Vects points = MeshSource.spherePoints(resolution).vattr.getAll(Mesh.COORD);
        Vects out = new Vects();

        for (int i = 0; i < points.size(); i++)
        {
            Vect point = points.get(i);

            if (half && point.getZ() < 0)
            {
                continue;
            }
            else
            {
                out.add(point);
            }
        }

        return out;
    }

    public static Vects sphere(int resolution)
    {
        return sphere(resolution, false);
    }
}
