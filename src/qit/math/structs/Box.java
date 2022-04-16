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

package qit.math.structs;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.JsonDataset;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.List;

public class Box extends JsonDataset implements Intersectable, Boxable
{
    public static Box create(Iterable<Vect> vs)
    {
        Box out = null;
        for (Vect v : vs)
        {
            if (out == null)
            {
                out = create(v);
            }
            else
            {
                out = out.union(v);
            }
        }

        return out;
    }

    public static Box createRadius(Vect v, double radius)
    {
        Global.assume(radius >= 0 && MathUtils.nonzero(radius), "invalid radius: " + radius);

        Interval[] ints = new Interval[v.size()];
        for (int i = 0; i < v.size(); i++)
        {
            ints[i] = new Interval(v.get(i) - radius, v.get(i) + radius);
        }
        return new Box(ints);
    }

    public static Box createRadius(Vect v, Vect r)
    {
        Global.assume(v.size() == r.size(), "invalid radius");

        for (int i = 0; i < r.size(); i++)
        {
            Global.assume(r.get(i) >= 0 && MathUtils.nonzero(r.get(i)), "invalid radius: " + r.get(i));
        }

        Interval[] ints = new Interval[v.size()];
        for (int i = 0; i < v.size(); i++)
        {
            ints[i] = new Interval(v.get(i) - r.get(i), v.get(i) + r.get(i));
        }

        return new Box(ints);
    }

    public static Box create(Vect v)
    {
        Interval[] ints = new Interval[v.size()];
        for (int i = 0; i < v.size(); i++)
        {
            ints[i] = new Interval(v.get(i), v.get(i));
        }
        return new Box(ints);
    }

    public static Box createUnion(Vect a, Vect b, Vect c)
    {
        Interval[] ints = new Interval[a.size()];
        for (int i = 0; i < a.size(); i++)
        {
            double min = Math.min(a.get(i), Math.min(b.get(i), c.get(i)));
            double max = Math.max(a.get(i), Math.max(b.get(i), c.get(i)));
            ints[i] = new Interval(min, max);
        }
        return new Box(ints);
    }

    public static Box createUnion(Vect a, Vect b)
    {
        Interval[] ints = new Interval[a.size()];
        for (int i = 0; i < a.size(); i++)
        {
            double min = Math.min(a.get(i), b.get(i));
            double max = Math.max(a.get(i), b.get(i));
            ints[i] = new Interval(min, max);
        }
        return new Box(ints);
    }

    private final Interval[] intervals;

    public Box(int dim)
    {
        this.intervals = new Interval[dim];
    }

    public Box(Interval[] ints)
    {
        this.intervals = new Interval[ints.length];
        for (int i = 0; i < ints.length; i++)
        {
            this.intervals[i] = ints[i].copy();
        }
    }

    public double getDiameter()
    {
        return this.getMax().dist((this.getMin()));
    }

    public Vect getMin()
    {
        int dim = this.intervals.length;
        Vect out = VectSource.createND(dim);
        for (int i = 0; i < dim; i++)
        {
            out.set(i, this.intervals[i].getMin());
        }
        return out;
    }

    public Vect getMax()
    {
        int dim = this.intervals.length;
        Vect out = VectSource.createND(dim);
        for (int i = 0; i < dim; i++)
        {
            out.set(i, this.intervals[i].getMax());
        }
        return out;
    }

    public Interval getInterval(int i)
    {
        return this.intervals[i].copy();
    }

    public Vect getCenter()
    {
        int dim = this.intervals.length;
        Vect out = VectSource.createND(dim);
        for (int i = 0; i < dim; i++)
        {
            double min = this.intervals[i].getMin();
            double max = this.intervals[i].getMax();
            out.set(i, min + (max - min) / 2.0);
        }
        return out;
    }

    public Interval range(int idx)
    {
        return this.intervals[idx];
    }

    public Box copy()
    {
        return new Box(this.intervals);
    }

    public int dim()
    {
        return this.intervals.length;
    }

    public Box iso()
    {
        double[] lens = new double[this.dim()];

        double maxlen = 0;
        for (int i = 0; i < this.dim(); i++)
        {
            Interval intv = this.range(i);
            lens[i] = intv.size();
            maxlen = Math.max(maxlen, lens[i]);
        }

        Interval[] nints = new Interval[this.dim()];
        for (int i = 0; i < this.dim(); i++)
        {
            Interval intv = this.range(i);
            double delta = (maxlen - lens[i]) / 2;
            double nmin = intv.getMin() - delta;
            double nmax = intv.getMax() + delta;
            nints[i] = new Interval(nmin, nmax);
        }
        return new Box(nints);
    }

    public Box union(Box b)
    {
        Interval[] ints = new Interval[this.dim()];
        for (int i = 0; i < this.dim(); i++)
        {
            ints[i] = this.range(i).union(b.range(i));
        }
        return new Box(ints);
    }

    public Box union(Vect vect)
    {
        Interval[] ints = new Interval[this.dim()];
        for (int i = 0; i < this.dim(); i++)
        {
            ints[i] = this.range(i).union(vect.get(i));
        }
        return new Box(ints);
    }

    public Box scale(double delta)
    {
        Global.assume(delta > 0, "invalid delta");

        Interval[] ints = new Interval[this.dim()];
        for (int i = 0; i < this.dim(); i++)
        {
            Interval intv = this.range(i);
            double size = intv.getMax() - intv.getMin();
            double nsize = delta * size;
            double change = (nsize - size) / 2.0;
            ints[i] = intv.grow(change);
        }
        return new Box(ints);
    }

    public Box buffer(double delta)
    {
        Global.assume(delta > 0, "invalid delta");

        Interval[] ints = new Interval[this.dim()];
        for (int i = 0; i < this.dim(); i++)
        {
            ints[i] = this.range(i).grow(delta);
        }
        return new Box(ints);
    }

    public Box grow(double delta)
    {
        Interval[] ints = new Interval[this.dim()];
        for (int i = 0; i < this.dim(); i++)
        {
            ints[i] = this.range(i).grow(delta);
        }
        return new Box(ints);
    }

    public Box shift(Vect v)
    {
        Interval[] ints = new Interval[this.dim()];
        for (int i = 0; i < this.dim(); i++)
        {
            ints[i] = this.range(i).shift(v.get(i));
        }
        return new Box(ints);
    }

    public boolean intersects(Segment s)
    {
        return s.intersects(this);
    }

    public boolean intersects(Box b)
    {
        for (int i = 0; i < this.dim(); i++)
        {
            if (!this.range(i).intersects(b.range(i)))
            {
                return false;
            }
        }

        return true;
    }

    public boolean contains(Box v)
    {
        for (int i = 0; i < this.dim(); i++)
        {
            if (!this.intervals[i].contains(v.intervals[i]))
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public Box box()
    {
        return this;
    }

    @Override
    public boolean contains(Vect v)
    {
        for (int i = 0; i < this.dim(); i++)
        {
            if (!this.intervals[i].contains(v.get(i)))
            {
                return false;
            }
        }

        return true;
    }

    public int label(Vect point)
    {
        return this.contains(point) ? 1 : 0;
    }

    @Override
    public List<LineIntersection> intersect(Line line)
    {
        Vect min = this.getMin();
        Vect max = this.getMax();
        List<Plane> planes = Lists.newArrayList();
        planes.add(Plane.createPositiveX(min.getX()));
        planes.add(Plane.createNegativeX(max.getX()));
        planes.add(Plane.createPositiveY(min.getY()));
        planes.add(Plane.createNegativeY(max.getY()));
        planes.add(Plane.createPositiveZ(min.getZ()));
        planes.add(Plane.createNegativeZ(max.getZ()));

        Box grown = this.grow(Global.DELTA);

        List<LineIntersection> out = Lists.newArrayList();
        for (Plane plane : planes)
        {
            for (LineIntersection pint : plane.intersect(line))
            {
                if (grown.contains(pint.getPoint()))
                {
                    out.add(pint);
                }
            }
        }

        return out;
    }

    public Vects sample(Integer num)
    {
        Vects out = new Vects();

        for (int i = 0; i < num; i++)
        {
            double rx = Global.RANDOM.nextDouble();
            double ry = Global.RANDOM.nextDouble();
            double rz = Global.RANDOM.nextDouble();

            Vect min = this.getMin();
            Vect max = this.getMax();

            double dx = max.get(0) - min.get(0);
            double dy = max.get(1) - min.get(1);
            double dz = max.get(2) - min.get(2);

            double sx = min.get(0) + rx * dx;
            double sy = min.get(1) + ry * dy;
            double sz = min.get(2) + rz * dz;

            Vect sv = VectSource.create3D(sx, sy, sz);

            out.add(sv);
        }

        return out;
    }

    public static Box read(String fn) throws IOException
    {
        return JsonDataset.read(Box.class, fn);
    }
}
