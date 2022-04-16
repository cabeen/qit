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

public class Sphere extends JsonDataset implements Intersectable, Boxable
{
    private Vect center = VectSource.create3D();
    private double radius = 1.0;

    public static Sphere fromPointRadius(Vect p, double r)
    {
        return new Sphere(p, r);
    }

    public static Sphere fromBox(Box b)
    {
        Box biso = b.iso();
        Vect p = biso.getCenter();
        double r = b.range(0).delta() / 2.0
                ;
        return new Sphere(p, r);
    }

    public Sphere(Vect p, double r)
    {
        Global.assume(r > 0, "radius must be positive");
        Global.assume(p.size() == 3, "center must be 3D");

        this.center.set(p);
        this.radius = r;
    }

    public Vect getCenter()
    {
        return this.center.copy();
    }

    public double getRadius()
    {
        return this.radius;
    }

    public Vects sample(int num)
    {
        Vects out = new Vects();

        double rad = this.getRadius();
        Vect center = this.getCenter();
        double cx = center.get(0);
        double cy = center.get(1);
        double cz = center.get(2);

        while (out.size() < num)
        {
            double rx = Global.RANDOM.nextDouble();
            double ry = Global.RANDOM.nextDouble();
            double rz = Global.RANDOM.nextDouble();

            double sx = cx + (2 * rx - 1) * rad;
            double sy = cy + (2 * ry - 1) * rad;
            double sz = cz + (2 * rz - 1) * rad;

            Vect sv = VectSource.create3D(sx, sy, sz);

            if (this.contains(sv))
            {
                out.add(sv);
            }
        }

        return out;
    }

    public boolean contains(Vect point)
    {
        double d2 = point.dist2(this.center);
        double r2 = this.radius * this.radius;

        return d2 < r2;
    }

    public int label(Vect point)
    {
        return this.contains(point) ? 1 : 0;
    }

    public List<LineIntersection> intersect(Line line)
    {
        // http://paulbourke.net/geometry/circlesphere/index.html#linesphere

        Vect p1 = line.getPoint();
        Vect p2 = line.getOtherPoint();
        Vect p3 = this.center;
        double r = this.radius;

        Vect p21 = p2.minus(p1);
        Vect p13 = p1.minus(p3);

        double a = p21.norm2();
        double b = 2 * p21.dot(p13);
        double c = p3.norm2() + p1.norm2() - 2 * p3.dot(p1) - r * r;

        double des = b * b - 4 * a * c;

        List<LineIntersection> out = Lists.newArrayList();
        if (des < 0)
        {
            // no solution
        }
        else if (MathUtils.zero(des))
        {
            // one solution (its tangent)
            double u = -b / 2 * a;
            Vect p = p1.plus(u, p2.minus(p1));
            out.add(new LineIntersection(u, p));
        }
        else
        {
            // two solutions (it goes through)
            double d = Math.sqrt(des);
            double uPlus = (-b + d) / (2 * a);
            double uNeg = (-b - d) / (2 * a);

            Vect delta = p2.minus(p1);
            Vect pPlus = p1.plus(uPlus, delta);
            Vect pNeg = p1.plus(uNeg, delta);

            out.add(new LineIntersection(uPlus, pPlus));
            out.add(new LineIntersection(uNeg, pNeg));
        }

        return out;
    }

    public Sphere copy()
    {
        return new Sphere(this.center.copy(), this.radius);
    }

    public Box box()
    {
        Interval[] intervals = new Interval[this.center.size()];
        for (int i = 0; i < this.center.size(); i++)
        {
            double ci = this.center.get(i);
            intervals[i] = new Interval(ci - this.radius, ci + this.radius);
        }
        return new Box(intervals);
    }

    public static Sphere read(String fn) throws IOException
    {
        return JsonDataset.read(Sphere.class, fn);
    }
}
