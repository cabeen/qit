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


package qit.data.datasets;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Indexed;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.formats.vects.*;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsStats;
import qit.math.utils.MathUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * a list of vectors
 */
@SuppressWarnings("serial")
public class Vects extends ArrayList<Vect> implements Dataset, Indexed<Vect>
{
    public final static String ELEM = "elem";
    public final static String INDEX = "index";

    public Vects()
    {
        super();
    }

    public Vects(int n)
    {
        super(n);
    }

    public Vects(List<Vect> vects)
    {
        super(vects);
    }


    public Vects(Vect vect)
    {
        super();
        this.add(vect);
    }

    public Vects(Iterable<Vect> vects)
    {
        super();

        for (Vect v : vects)
        {
            this.add(v);
        }
    }

    public Vects(Vect[] vs)
    {
        super(vs.length);
        for (Vect v : vs)
        {
            this.add(v);
        }
    }

    public Vects(Vects vs)
    {
        super(vs.size());
        for (Vect v : vs)
        {
            this.add(v);
        }
    }

    public double dist(Vects vects)
    {
        double out = 0;

        int num = Math.min(this.size(), vects.size());

        if (num == 0)
        {
            return out;
        }

        for (int i = 0; i < num; i++)
        {
            out += this.get(i).dist2(vects.get(i));
        }

        return Math.sqrt(out / num);
    }

    public Vects copy()
    {
        Vects out = new Vects();
        for (Vect v : this)
        {
            out.add(v.copy());
        }
        return out;
    }

    public Vects perm(Integers which)
    {
        Vects out = new Vects();
        for (Integer idx : which)
        {
            out.add(this.get(idx));
        }

        return out;
    }

    public void setAll(double value)
    {
        for (Vect vect : this)
        {
            vect.setAll(value);
        }
    }

    public int getDim()
    {
        if (this.size() == 0)
        {
            return 0;
        }
        else
        {
            return this.get(0).size();
        }
    }

    public void keep(boolean[] filter)
    {
        Global.assume(this.size() == filter.length, "invalid filter");

        for (int i = this.size() - 1; i >= 0; i--)
        {
            if (!filter[i])
            {
                this.remove(i);
            }
        }
    }

    public Vects copy(boolean[] filter)
    {
        Global.assume(this.size() == filter.length, "invalid filter");

        Vects out = new Vects();

        for (int i = 0; i < filter.length; i++)
        {
            if (filter[i])
            {
                out.add(this.get(i));
            }
        }

        return out;
    }

    public Vect flatten()
    {
        int n = this.size();
        int d = this.getDim();

        Vect out = VectSource.createND(n * d);
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < d; j++)
            {
                int idx = i * d + j;
                out.set(idx, this.get(i).get(j));
            }
        }

        return out;
    }

    public Vect dim(int idx)
    {
        Vect out = VectSource.createND(this.size());
        for (int i = 0; i < this.size(); i++)
        {
            out.set(i, this.get(i).get(idx));
        }
        return out;
    }

    public Vects sub(int start, int end)
    {
        Vects out = new Vects();

        for (Vect v : this)
        {
            out.add(v.sub(start, end));
        }

        return out;
    }

    public Vects subList(int start, int end)
    {
        return new Vects(super.subList(start, end));
    }

    public Vects transpose()
    {
        int m = this.size();
        int n = this.get(0).size();

        Vects out = new Vects(n);
        for (int i = 0; i < n; i++)
        {
            Vect v = new Vect(m);
            for (int j = 0; j < m; j++)
            {
                v.set(j, this.get(j).get(i));
            }

            out.add(v);
        }

        return out;
    }

    public Vect column(int idx)
    {
        Vect out = VectSource.createND(this.size());
        for (int i = 0; i < this.size(); i++)
        {
            out.set(i, this.get(i).get(idx));
        }

        return out;
    }

    public Vects sub(Integers idx)
    {
        int dim = idx.size();
        Vects out = new Vects();
        for (int i = 0; i < dim; i++)
        {
            out.add(this.get(idx.get(i)));
        }

        return out;
    }

    public Vect max()
    {
        Global.assume(this.size() > 0, "empty vects found");

        Vect out = this.get(0);
        for (int i = 1; i < this.size(); i++)
        {
            Vect v = this.get(i);
            for (int j = 0; j < this.getDim(); j++)
            {
                out.set(j, Math.max(out.get(j), v.get(j)));
            }
        }

        return out;
    }

    public Vect min()
    {
        Global.assume(this.size() > 0, "empty vects found");

        Vect out = this.get(0);
        for (int i = 1; i < this.size(); i++)
        {
            Vect v = this.get(i);
            for (int j = 0; j < this.getDim(); j++)
            {
                out.set(j, Math.min(out.get(j), v.get(j)));
            }
        }

        return out;
    }

    public Vect sum()
    {
        Global.assume(this.size() > 0, "empty vects found");

        Vect out = this.get(0);
        for (int i = 1; i < this.size(); i++)
        {
            out.plusEquals(this.get(i));
        }

        return out;
    }

    public Vect mean()
    {
        Global.assume(this.size() > 0, "empty vects found");

        return this.sum().times(1.0 / this.size());
    }

    public int closestMean()
    {
        Vect mean = this.mean();

        int num = this.size();
        double[] dists = new double[num];

        for (int i = 0; i < num; i++)
        {
            dists[i] = this.get(i).dist2(mean);
        }

        return MathUtils.minidx(dists);
    }

    public int nearest(Vect v)
    {
        int minidx = 0;
        double dist = v.dist(this.get(0));

        for (int i = 1; i < this.size(); i++)
        {
            double mydist = v.dist(this.get(i));
            if (mydist < dist)
            {
                dist = mydist;
                minidx = i;
            }
        }

        return minidx;
    }

    public double[][] toDimNumArray()
    {
        double[][] out = new double[this.getDim()][this.size()];

        for (int i = 0; i < this.size(); i++)
        {
            for (int j = 0; j < this.getDim(); j++)
            {
                out[j][i] = this.get(i).get(j);
            }
        }

        return out;
    }

    public double[][] toNumDimArray()
    {
        double[][] out = new double[this.size()][this.getDim()];

        for (int i = 0; i < this.size(); i++)
        {
            for (int j = 0; j < this.getDim(); j++)
            {
                out[i][j] = this.get(i).get(j);
            }
        }

        return out;
    }

    public Vect first()
    {
        return this.get(0);
    }

    public Vect last()
    {
        return this.get(this.size() - 1);
    }

    public Vect head()
    {
        return this.get(0);
    }

    public Vect tail()
    {
        return this.get(this.size() - 1);
    }

    public String encodeJson()
    {
        List<String> elems = Lists.newArrayList();
        for (Vect vect : this)
        {
            elems.add(vect.encodeJson());
        }
        return "[" + StringUtils.join(elems, ",\n") + "]";
    }

    public String toString()
    {
        return this.encodeJson();
    }

    public static Vects read(String fn) throws IOException
    {
        if (fn.endsWith("csv"))
        {
            return CsvVectsCoder.read(fn);
        }
        else if (fn.endsWith("particles.csv"))
        {
            return ParticlesVectsCoder.read(fn);
        }
        else if (fn.endsWith("raw"))
        {
            return RawVectsCoder.read(fn);
        }
        else if (fn.endsWith("swc"))
        {
            return SwcVectsCoder.read(fn);
        }
        else if (fn.endsWith("mgh"))
        {
            return MghVectsCoder.read(fn);
        }
        else if (fn.endsWith("annot"))
        {
            return AnnotVectsCoder.read(fn);
        }
        else if (fn.endsWith("txt"))
        {
            return TxtVectsCoder.read(fn);
        }
        else if (fn.endsWith("txt.gz"))
        {
            return TxtVectsCoder.read(fn);
        }
        else if (fn.endsWith("xfm") || fn.endsWith("bval") || fn.endsWith("bvec"))
        {
            return TxtVectsCoder.read(fn);
        }
        else
        {
            try
            {
                return CurvVectsCoder.read(fn);
            }
            catch (Exception e)
            {
                return TxtVectsCoder.read(fn);
            }
        }
    }

    public void write(String fn) throws IOException
    {
        PathUtils.mkpar(fn);

        if (fn.endsWith("csv"))
        {
            CsvVectsCoder.write(this, fn);
        }
        else if (fn.endsWith("particles.csv"))
        {
            ParticlesVectsCoder.write(this, fn);
        }
        else if (fn.endsWith("raw.gz"))
        {
            RawVectsCoder.write(this, fn);
        }
        else if (fn.endsWith("raw"))
        {
            RawVectsCoder.write(this, fn);
        }
        else if (fn.endsWith("obj"))
        {
            LightwaveObjVectsCoder.write(this, new FileOutputStream(fn));
        }
        else if (fn.endsWith("json"))
        {
            FileUtils.writeStringToFile(new File(fn), this.encodeJson());
        }
        else if (fn.endsWith("txt.gz"))
        {
            TxtVectsCoder.write(this, fn);
        }
        else if (fn.endsWith("txt"))
        {
            TxtVectsCoder.write(this, fn);
        }
        else
        {
            TxtVectsCoder.write(this, fn);
        }
    }

    public List<String> getExtensions()
    {
        List<String> out = Lists.newArrayList();
        out.add("csv");
        out.add("txt");
        out.add("json");
        return out;
    }

    public String getDefaultExtension()
    {
        return "csv";
    }
}
