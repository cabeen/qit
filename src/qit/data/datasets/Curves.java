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
import com.google.common.collect.Sets;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.cli.CliUtils;
import qit.base.structs.Indexed;
import qit.base.structs.Pair;
import qit.data.formats.curves.*;
import qit.data.datasets.Curves.Curve;
import qit.data.source.VectSource;
import qit.math.structs.Box;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * curves with named per-vertex attributes
 */
public class Curves implements Indexed<Curve>, Dataset
{
    // some standards to use for attributes
    public final static String COORD = "coord";
    public final static String CURVE_IDX = "curve_index";
    public final static String START_IDX = "start_index";
    public final static String END_IDX = "end_index";
    public final static String INDEX = "index";
    public final static String ARCLENGTH = "arclength";
    public final static String LENGTH = "length";
    public final static String FRACTION = "fraction";
    public final static String COLOR = "color";
    public final static String OPACITY = "opacity";
    public final static String DIST = "dist";
    public final static String VLIKE = "vlike";
    public final static String CLIKE = "clike";
    public final static String PVAL = "pval";
    public final static String MAHAL = "mahal";
    public final static String TANGENT = "tangent";
    public final static String NORMAL = "normal";
    public final static String BINORMAL = "binormal";
    public final static String CURVATURE = "curv_point";
    public final static String CURVATURE_SUM = "curv_sum";
    public final static String CURVATURE_MEAN = "curv_mean";
    public final static String CURVATURE_VAR = "curv_var";
    public final static String CURVATURE_STD = "curv_std";
    public final static String CURVATURE_MIN = "curv_min";
    public final static String CURVATURE_MAX = "curv_max";
    public final static String DENSITY = "density_point";
    public final static String DENSITY_SUM = "density_sum";
    public final static String DENSITY_MEAN = "density_mean";
    public final static String DENSITY_VAR = "density_var";
    public final static String DENSITY_STD = "density_std";
    public final static String DENSITY_MIN = "density_min";
    public final static String DENSITY_MAX = "density_max";
    public final static String LENGTH_SUM = "density_sum";
    public final static String LENGTH_MEAN = "density_mean";
    public final static String LENGTH_VAR = "density_var";
    public final static String LENGTH_STD = "density_std";
    public final static String LENGTH_MIN = "density_min";
    public final static String LENGTH_MAX = "density_max";
    public final static String COUNT = "count";
    public final static String GAMMA = "gamma";
    public final static String TORSION = "tor";
    public final static String LABEL = "label";
    public final static String SAMPLED = "sampled";
    public final static String THICKNESS = "thickness";
    public final static String THICK = "thick";
    public final static String ALPHA = "ALPHA";

    public final static String NUM_CURVES = "num_curves";
    public final static String VOLUME = "volume";

    private List<Curve> curves;

    private int nattr;
    private List<String> names = new ArrayList<>();
    private List<Vect> protos = new ArrayList<>();

    public Curves()
    {
        this(0, new Vect(3));
    }

    public Curves(Curve curve)
    {
        this();
        this.add(curve);
    }

    public Curves(int n)
    {
        this(n, new Vect(3));
    }

    public Curves(Vect proto)
    {
        this(0, proto);
    }

    public Curves(int n, Vect proto)
    {
        this.curves = new ArrayList<>(n);

        this.nattr = 1;
        this.names.add(Curves.COORD);
        this.protos.add(proto.proto());
    }

    public Curves(Curves curves)
    {
        this.curves = new ArrayList<>(curves.size());

        for (Curve curve : curves)
        {
            this.curves.add(new Curve(curve));
        }

        this.nattr = curves.nattr;
        this.names.addAll(curves.names);
        this.protos.addAll(curves.protos);
    }

    public Curves(Vects coords)
    {
        int n = coords.size();
        Curve curve = this.add(n);
        for (int i = 0; i < n; i++)
        {
            curve.set(i, coords.get(i));
        }
    }

    public synchronized Curves copy(int idx)
    {
        Curves out = new Curves();
        out.add(this.get(idx));
        return out;
    }

    public synchronized Curves copy(boolean[] bs)
    {
        Global.assume(this.size() == bs.length, "invalid mask");

        Curves ncurves = new Curves();

        for (String name : this.names())
        {
            ncurves.add(name, this.proto(name));
        }

        for (int i = 0; i < this.size(); i++)
        {
            if (bs[i])
            {
                ncurves.add(this.get(i));
            }
        }

        return ncurves;
    }

    public Curves clean()
    {
        Set<Integer> which = Sets.newHashSet();

        for (int i = 0; i < this.size(); i++)
        {
            if (this.get(i).size() < 2)
            {
                which.add(i);
            }
        }

        this.remove(which);

        return this;
    }

    public synchronized void remove(Set<Integer> which)
    {
        List<Integer> list = Lists.newArrayList(which);
        Collections.sort(list, Collections.reverseOrder());
        for (Integer idx : list)
        {
            this.remove(idx);
        }
    }

    public synchronized void remove(boolean[] filter)
    {
        Global.assume(this.size() == filter.length, "invalid mask");

        for (int i = this.size() - 1; i >= 0; i--)
        {
            if (filter[i])
            {
                this.remove(i);
            }
        }
    }

    public synchronized void keep(boolean[] filter)
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

    public synchronized Box bounds()
    {
        double xmin = Double.MAX_VALUE;
        double ymin = Double.MAX_VALUE;
        double zmin = Double.MAX_VALUE;

        double xmax = Double.MIN_VALUE;
        double ymax = Double.MIN_VALUE;
        double zmax = Double.MIN_VALUE;

        for (Curve curve : this)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                Vect p = curve.get(i);
                double x = p.getX();
                double y = p.getY();
                double z = p.getZ();

                xmin = Math.min(x, xmin);
                ymin = Math.min(y, ymin);
                zmin = Math.min(z, zmin);

                xmax = Math.max(x, xmax);
                ymax = Math.max(y, ymax);
                zmax = Math.max(z, zmax);
            }
        }

        Vect min = VectSource.create3D(xmin, ymin, zmin);
        Vect max = VectSource.create3D(xmax, ymax, zmax);

        return Box.createUnion(min, max);
    }

    public synchronized Vect lengths()
    {
        Vect out = new Vect(this.size());
        for (int i = 0; i < this.size(); i++)
        {
            out.set(i, this.get(i).length());
        }

        return out;
    }

    public synchronized void reverse()
    {
        for (Curve curve : this)
        {
            curve.reverse();
        }
    }

    public synchronized void resample(Vect gamma)
    {
        for (Curve curve : this)
        {
            curve.resample(gamma);
        }
    }

    public synchronized void shuffle()
    {
        Collections.shuffle(this.curves);
    }

    public synchronized void add(Vects vects)
    {
        Curve curve = this.add(vects.size());

        for (int i = 0; i < vects.size(); i++)
        {
            curve.set(i, vects.get(i));
        }
    }

    public synchronized void add(Curves curves)
    {
        for (String name : curves.names())
        {
            this.add(name, curves.proto(name));
        }

        for (Curve curve : curves)
        {
            Curve ncurve = this.add(curve.size());
            for (int i = 0; i < curve.size(); i++)
            {
                for (String name : curves.names())
                {
                    ncurve.set(name, i, curve.get(name, i));
                }
            }
        }
    }

    public synchronized Curve add(Curve curve)
    {
        for (String name : curve.names())
        {
            this.add(name, curve.proto(name));
        }

        Curve ncurve = this.add(curve.size());
        for (int i = 0; i < curve.size(); i++)
        {
            for (String name : curve.names())
            {
                ncurve.set(name, i, curve.get(name, i));
            }
        }

        return ncurve;
    }

    public synchronized int size()
    {
        return this.curves.size();
    }

    public synchronized int curveCount()
    {
        return this.curves.size();
    }

    public synchronized int vertexCount()
    {
        int out = 0;

        for (Curve curve : this)
        {
            out += curve.size();
        }

        return out;
    }

    public synchronized Curve get(int i)
    {
        return this.curves.get(i);
    }


    public synchronized void removeAll()
    {
        this.curves.clear();
    }

    public synchronized void remove(int i)
    {
        this.curves.remove(i);
    }

    public synchronized Curve add(int n)
    {
        Curve c = new Curve(n);
        this.curves.add(c);
        return c;
    }

    public synchronized boolean has(String name)
    {
        return this.names.contains(name);
    }

    public synchronized Curves copy()
    {
        return new Curves(this);
    }

    public synchronized void add(String name, Vect proto)
    {
        if (!this.names.contains(name))
        {
            this.nattr++;
            this.names.add(name);
            this.protos.add(proto);

            for (Curve curve : this.curves)
            {
                curve.add(proto);
            }
        }
    }

    public synchronized Curves proto()
    {
        Curves curves = new Curves();
        for (String attr : this.names())
        {
            curves.add(attr, this.proto(attr));
        }

        return curves;
    }

    public synchronized Vect proto(String name)
    {
        int idx = this.names.indexOf(name);

        Global.assume(idx >= 0, "invalid attribute name");

        return this.protos.get(idx).proto();
    }

    public synchronized void remove(String name)
    {
        Global.assume(!name.equals(Curves.COORD), "cannot remove coordinates attribute");

        if (this.names.contains(name))
        {
            int idx = this.names.indexOf(name);
            Global.assume(idx >= 0, "invalid attribute name");

            this.nattr--;
            this.names.remove(idx);
            this.protos.remove(idx);
            for (Curve curve : this.curves)
            {
                curve.data.remove(idx);
            }
        }
    }

    public synchronized void rename(String from, String to)
    {
        if (this.names.contains(to))
        {
            this.remove(to);
        }

        int idx = this.names.indexOf(from);
        Global.assume(idx >= 0, "invalid attribute name");

        this.names.set(idx, to);
    }

    public synchronized List<String> names()
    {
        return this.names;
    }

    public synchronized int dim(String name)
    {
        int idx = this.names.indexOf(name);
        Global.assume(idx >= 0, "invalid attribute name");
        return this.protos.get(idx).size();
    }

    public synchronized void apply(VectFunction func, String from, String to)
    {
        Vect buffer = func.protoOut();
        for (Curve curve : this)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                func.apply(curve.get(from, i), buffer);
                curve.set(to, i, buffer);
            }
        }
    }

    public synchronized void apply(VectFunction xfm)
    {
        this.apply(xfm, Curves.COORD, Curves.COORD);
    }

    public synchronized void sample(VectFunction func, String attr)
    {
        Vect buff = func.protoOut();
        this.add(attr, buff.proto());
        for (Curve curve : this)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                Vect coord = curve.get(Curves.COORD, i);
                func.apply(coord, buff);
                curve.set(attr, i, buff);
            }
        }
    }

    public synchronized int numVertices()
    {
        int numVertices = 0;
        for (Curves.Curve curve : curves)
        {
            numVertices += curve.size();
        }

        return numVertices;
    }

    public Vects flatten()
    {
        return flatten(COORD);
    }

    public Vects flatten(String attr)
    {
        Vects out = new Vects();

        for (Curve curve : this)
        {
            out.add(curve.getAll(attr).flatten());
        }

        return out;
    }

    public Vects cat(String attr)
    {
        Vects out = new Vects();

        for (Curve curve : this)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                out.add(curve.get(attr, i));
            }
        }

        return out;
    }

    public synchronized Iterator<Curve> iterator()
    {
        return this.curves.iterator();
    }

    public class Curve implements Indexed<Vect>
    {
        private int num;
        private List<float[]> data = new ArrayList<>();

        private Curve(int n)
        {
            this.num = n;
            for (Vect proto : Curves.this.protos)
            {
                this.add(proto);
            }
        }

        private Curve(Curve curve)
        {
            this.num = curve.num;
            for (float[] d : curve.data)
            {
                this.data.add(MathUtils.copy(d));
            }
        }

        public synchronized Box bounds()
        {
            Box cbox = null;
            for (Vect v : this)
            {
                if (cbox == null)
                {
                    cbox = Box.create(v);
                }
                else
                {
                    cbox = cbox.union(v);
                }
            }

            return cbox;
        }

        public synchronized Pair<Double, Integer> nearestVertex(Vect p)
        {
            double minDist = Double.MAX_VALUE;
            int minIdx = -1;

            for (int i = 0; i < this.size(); i++)
            {
                Vect q = this.get(i);
                double dist = p.dist(q);
                if (dist < minDist)
                {
                    minDist = dist;
                    minIdx = i;
                }
            }

            return Pair.of(minDist, minIdx);
        }

        public synchronized Vect integral(String attr)
        {
            Global.assume(this.has(attr), "Attribute not found: " + attr);

            Vect out = this.get(attr, 0).proto();
            Vect buff = out.proto();

            for (int i = 1; i < this.size(); i++)
            {
                Vect ca = this.get(i - 1);
                Vect cb = this.get(i);

                Vect va = this.get(attr, i - 1);
                Vect vb = this.get(attr, i);

                double dist = ca.dist(cb);
                buff.set(va);
                buff.plusEquals(vb);
                buff.timesEquals(dist / 2.0);

                out.plusEquals(buff);
            }

            return out;
        }

        public synchronized double length()
        {
            double len = 0;
            Vect prev = null;
            for (Vect vect : this)
            {
                if (prev != null)
                {
                    len += vect.dist(prev);
                }

                prev = vect;
            }

            return len;
        }

        public synchronized Curve reverse()
        {
            for (int i = 0; i < Curves.this.nattr; i++)
            {
                int dim = Curves.this.protos.get(i).size();
                float[] vals = this.data.get(i);

                for (int j = 0; j < dim; j++)
                {
                    for (int k = 0; k < this.num / 2; k++)
                    {
                        int idxa = k * dim + j;
                        int idxb = (this.num - 1 - k) * dim + j;
                        float a = vals[idxa];
                        float b = vals[idxb];
                        vals[idxb] = a;
                        vals[idxa] = b;
                    }
                }
            }

            return this;
        }

        public synchronized Vect cumlength()
        {
            Vect out = VectSource.createND(this.size());
            out.set(0, 0);

            for (int i = 1; i < this.size(); i++)
            {
                Vect curr = this.get(i);
                Vect prev = this.get(i - 1);
                double dist = curr.dist(prev);
                out.set(i, out.get(i - 1) + dist);
            }

            return out;
        }

        public synchronized void subset(boolean[] markers)
        {
            Global.assume(markers.length == this.size(), "invalid markers");

            int nn = 0;
            for (boolean marker : markers)
            {
                if (marker)
                {
                    nn++;
                }
            }

            int curr = 0;
            int[] idx = new int[nn];
            for (int i = 0; i < markers.length; i++)
            {
                if (markers[i])
                {
                    idx[curr++] = i;
                }
            }

            for (int i = 0; i < Curves.this.nattr; i++)
            {
                int dim = Curves.this.protos.get(i).size();
                float[] vals = this.data.get(i);
                float[] nvals = new float[dim * nn];

                for (int j = 0; j < dim; j++)
                {
                    for (int k = 0; k < nn; k++)
                    {
                        nvals[k * dim + j] = vals[idx[k] * dim + j];
                    }
                }

                this.data.set(i, nvals);
            }

            this.num = nn;
        }

        public synchronized Curve resample(Vect gamma)
        {
            // assumed: gamma is from 0 to 1
            int n = this.num;
            int nn = gamma.size();

            if (n == 0 || n == 1)
            {
                return this;
            }

            Vect t = this.cumlength();
            double length = t.get(n - 1);

            double[] nt = new double[nn];
            for (int i = 0; i < nn; i++)
            {
                nt[i] = length * gamma.get(i);
            }

            double[] v = new double[n];

            for (int i = 0; i < Curves.this.nattr; i++)
            {
                int dim = Curves.this.protos.get(i).size();
                float[] vals = this.data.get(i);
                float[] nvals = new float[dim * nn];

                for (int j = 0; j < dim; j++)
                {
                    for (int k = 0; k < n; k++)
                    {
                        v[k] = vals[k * dim + j];
                    }

                    double[] nv = MathUtils.linear(t.toArray(), v, nt);

                    for (int k = 0; k < nn; k++)
                    {
                        nvals[k * dim + j] = (float) nv[k];
                    }
                }

                this.data.set(i, nvals);
            }

            this.num = nn;

            return this;
        }

        public synchronized Vect getHead()
        {
            return this.get(0);
        }

        public synchronized Vect getMiddle()
        {
            return this.get((this.num - 1) / 2);
        }

        public synchronized Vect getTail()
        {
            return this.get(this.num - 1);
        }

        public synchronized Vect getHead(String name)
        {
            return this.get(name, 0);
        }

        public synchronized Vect getMiddle(String name)
        {
            return this.get(name, (this.num - 1) / 2);
        }

        public synchronized Vect getTail(String name)
        {
            return this.get(name, this.num - 1);
        }

        public synchronized Vect get(int idx)
        {
            return this.get(Curves.COORD, idx);
        }

        public synchronized Vect get(String name, int idx)
        {
            int nidx = Curves.this.names.indexOf(name);
            Global.assume(nidx >= 0, "attribute does not exist: " + name);

            Vect out = Curves.this.protos.get(nidx).proto();
            int dim = out.size();
            int start = dim * idx;
            float[] d = this.data.get(nidx);
            for (int i = 0; i < dim; i++)
            {
                out.set(i, d[start + i]);
            }

            return out;
        }

        public synchronized int getint(String name, int idx)
        {
            return MathUtils.round(this.get(name, idx).get(0));
        }

        public synchronized Vects getAll()
        {
            Vects out = new Vects();
            for (int i = 0; i < this.size(); i++)
            {
                out.add(this.get(i));
            }
            return out;
        }

        public synchronized Vects getAll(String name)
        {
            Vects out = new Vects();
            for (int i = 0; i < this.size(); i++)
            {
                out.add(this.get(name, i));
            }
            return out;
        }

        public synchronized Vects getAll(List<String> names)
        {
            Vects out = new Vects();
            for (int i = 0; i < this.size(); i++)
            {
                Vect item = null;
                for (String name : names)
                {
                    Vect value = this.get(name, i);
                    item = item == null ? value : item.cat(value);
                }
                out.add(item);
            }
            return out;
        }

        public synchronized void set(int idx, Vect value)
        {
            this.set(Curves.COORD, idx, value);
        }

        public synchronized void set(String name, int idx, Vect value)
        {
            if (!this.has(name))
            {
                this.add(name, value.proto());
            }

            int nidx = Curves.this.names.indexOf(name);
            Global.assume(nidx >= 0, "attribute does not exist: " + name);

            int dim = Curves.this.protos.get(nidx).size();
            Global.assume(value.size() == dim, "invalid input dimension");

            int start = dim * idx;
            float[] d = this.data.get(nidx);
            for (int i = 0; i < dim; i++)
            {
                d[start + i] = (float) value.get(i);
            }
        }

        public synchronized void setAll(String name, Vect value)
        {
            for (int i = 0; i < this.size(); i++)
            {
                set(name, i, value);
            }
        }

        public synchronized int size()
        {
            return this.num;
        }

        public synchronized int dim(String name)
        {
            int idx = Curves.this.names.indexOf(name);
            Global.assume(idx >= 0, "attribute does not exist: " + name);

            return Curves.this.protos.get(idx).size();
        }

        public synchronized void add(String name, Vect proto)
        {
            Curves.this.add(name, proto);
        }

        public synchronized void remove(String name)
        {
            Curves.this.remove(name);
        }

        public synchronized void rename(String from, String to)
        {
            Curves.this.rename(from, to);
        }

        public synchronized boolean has(String name)
        {
            return Curves.this.has(name);
        }

        public synchronized Vect proto(String name)
        {
            return Curves.this.proto(name);
        }

        public synchronized List<String> names()
        {
            return Curves.this.names();
        }

        public synchronized Indexed<Vect> get(final String name)
        {
            final int nidx = Curves.this.names.indexOf(name);
            return new Indexed<Vect>()
            {
                public synchronized int size()
                {
                    return Curve.this.num;
                }

                public synchronized Iterator<Vect> iterator()
                {
                    return new ArrayIterator(Curves.this.protos.get(nidx), Curve.this.data.get(nidx));
                }

                public synchronized Vect get(int idx)
                {
                    return Curve.this.get(name, idx);
                }
            };
        }

        public synchronized Iterator<Vect> iterator()
        {
            return new ArrayIterator(Curves.this.protos.get(0), this.data.get(0));
        }

        private void add(Vect proto)
        {
            int dim = proto.size();
            float[] nattr = new float[this.num * dim];
            this.data.add(nattr);
        }
    }

    public static class ArrayIterator implements Iterator<Vect>
    {
        Vect proto;
        float[] array;
        int dim;
        int pos;

        public ArrayIterator(Vect proto, float[] array)
        {
            this.proto = proto;
            this.array = array;
            this.pos = 0;
            this.dim = proto.size();
        }

        public synchronized boolean hasNext()
        {
            return this.pos < this.array.length;
        }

        public synchronized Vect next() throws NoSuchElementException
        {
            if (this.pos >= this.array.length)
            {
                throw new NoSuchElementException();
            }

            Vect out = this.proto.proto();
            for (int i = 0; i < this.dim; i++)
            {
                out.set(i, this.array[this.pos + i]);
            }

            this.pos += this.dim;

            return out;
        }

        public synchronized void remove() throws UnsupportedOperationException, IllegalStateException
        {
            throw new UnsupportedOperationException();
        }
    }

    public synchronized static Curves read(Iterable<String> fns) throws IOException
    {
        Curves curves = null;
        for (String fn : fns)
        {
            if (curves == null)
            {
                curves = read(fn);
            }
            else
            {
                curves.add(read(fn));
            }
        }
        return curves;
    }

    public synchronized static Curves read(String[] fns) throws IOException
    {
        Curves curves = null;
        for (String fn : fns)
        {
            if (curves == null)
            {
                curves = read(fn);
            }
            else
            {
                curves.add(read(fn));
            }
        }
        return curves;
    }

    public synchronized static Curves read(String fn) throws IOException
    {
        CliUtils.validate(fn);
        InputStream is = new FileInputStream(fn);

        if (fn.endsWith("vtk.gz"))
        {
            return VtkCurvesCoder.read(new GZIPInputStream(is));
        }
        else if (fn.endsWith("vtk"))
        {
            return VtkCurvesCoder.read(is);
        }
        else if (fn.endsWith("fib"))
        {
            return VtkCurvesCoder.read(is);
        }
        else if (fn.endsWith("obj"))
        {
            return MincObjCurvesCoder.read(is);
        }
        else if (fn.endsWith("data") || fn.endsWith("vrl"))
        {
            return VrlCurvesCoder.read(is);
        }
        else if (fn.endsWith("pdb"))
        {
            return PdbCurvesCoder.read(is);
        }
        else if (fn.endsWith("tck"))
        {
            return TckCurvesCoder.read(fn);
        }
        else if (fn.endsWith("trk"))
        {
            return DtkCurvesCoder.read(is);
        }
        else if (fn.endsWith("swc"))
        {
            return SwcCurvesCoder.read(fn);
        }
        else if (fn.endsWith("txt"))
        {
            return TxtCurvesCoder.read(is);
        }
        else
        {
            return VtkCurvesCoder.read(is);
        }
    }

    public synchronized void write(String fn) throws IOException
    {
        OutputStream os = new FileOutputStream(fn);

        if (fn.endsWith("vtk.gz"))
        {
            VtkCurvesCoder.write(this, new GZIPOutputStream(os));
        }
        else if (fn.endsWith("vtk"))
        {
            VtkCurvesCoder.write(this, os);
        }
        else if (fn.endsWith("fib"))
        {
            VtkCurvesCoder.write(this, os);
        }
        else if (fn.endsWith("tck"))
        {
            TckCurvesCoder.write(this, os);
        }
        else if (fn.endsWith("vrl"))
        {
            VrlCurvesCoder.write(this, os);
        }
        else if (fn.endsWith("pdb"))
        {
            PdbCurvesCoder.write(this, os);
        }
        else if (fn.endsWith("trk"))
        {
            DtkCurvesCoder.write(this, os);
        }
        else if (fn.endsWith("txt"))
        {
            TxtCurvesCoder.write(this, fn);
        }
        else if (fn.endsWith("wrl") || fn.endsWith("vrml"))
        {
            VrmlCurvesCoder.write(this, os);
        }
        else if (fn.endsWith("csv") && fn.contains("particle"))
        {
            ParticleCurvesCoder.write(this, os);
        }
        else if (fn.endsWith("csv"))
        {
            CsvCurvesCoder.write(this, fn);
        }
        else if (fn.endsWith("obj"))
        {
            if (Global.MINC || fn.contains("minc"))
            {
                MincObjCurvesCoder.write(this, os);
            }
            else
            {
                LightwaveObjCurvesCoder.write(this, os);
            }
        }
        else
        {
            VtkCurvesCoder.write(this, os);
        }
    }

    public synchronized List<String> getExtensions()
    {
        List<String> out = Lists.newArrayList();
        out.add("vtk.gz");
        out.add("vtk");
        out.add("trk");
        out.add("tck");
        out.add("txt");
        out.add("csv");
        out.add("obj");
        out.add("vrl");
        out.add("pdb");
        return out;
    }
}
