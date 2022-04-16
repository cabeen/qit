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

import qit.base.Global;
import qit.base.JsonDataset;
import qit.base.structs.Pair;
import qit.data.datasets.Vect;
import qit.math.utils.MathUtils;

public class Segment extends JsonDataset implements Boxable
{
    // Model a line segment p = a + (b - a) * t, for t in [0,1]
    private final Vect a;
    private final Vect b;

    public Segment(Vect a, Vect b)
    {
        this.a = a.copy();
        this.b = b.copy();
    }

    public Vect getA()
    {
        return this.a.copy();
    }

    public Vect getB()
    {
        return this.b.copy();
    }
    
    public Vect get(double u)
    {
        return a.plus(u, b.minus(a));
    }

    public double dist(Vect p)
    {
        return p.dist(get(this.nearest(p)));
    }

    public double dist2(Vect p)
    {
        return p.dist2(get(this.nearest(p)));
    }

    public double dist(Segment seg)
    {
        return this.dist(this.a, this.b, seg.a, seg.b);
    }

    public double dist2(Segment seg)
    {
        return dist2(this.a, this.b, seg.a, seg.b);
    }

    public double dist(Vect pa, Vect pb, Vect qa, Vect qb)
    {
        return Math.sqrt(dist2(pa, pb, qa, qb));
    }

    private static double dist2(Vect pa, Vect pb, Vect qa, Vect qb)
    {
        Vect u = pb.minus(pa);
        Vect v = qb.minus(qa);
        Vect w = pa.minus(qa);
        double a = u.dot(u); // always >= 0
        double b = u.dot(v);
        double c = v.dot(v); // always >= 0
        double d = u.dot(w);
        double e = v.dot(w);
        double D = a * c - b * b; // always >= 0
        double sc, sN, sD = D; // sc = sN / sD, default sD = D >= 0
        double tc, tN, tD = D; // tc = tN / tD, default tD = D >= 0

        // compute the line parameters of the two closest points
        if (D < Global.DELTA)
        { // the lines are almost parallel
            sN = 0.0; // force using point P0 on segment S1
            sD = 1.0; // to prevent possible division by 0.0 later
            tN = e;
            tD = c;
        }
        else
        { // build the closest points on the infinite lines
            sN = b * e - c * d;
            tN = a * e - b * d;
            if (sN < 0.0)
            { // sc < 0 => the s=0 edge is visible
                sN = 0.0;
                tN = e;
                tD = c;
            }
            else if (sN > sD)
            { // sc > 1 => the s=1 edge is visible
                sN = sD;
                tN = e + b;
                tD = c;
            }
        }

        if (tN < 0.0)
        { // tc < 0 => the t=0 edge is visible
            tN = 0.0;
            // recompute sc for this edge
            if (-d < 0.0)
            {
                sN = 0.0;
            }
            else if (-d > a)
            {
                sN = sD;
            }
            else
            {
                sN = -d;
                sD = a;
            }
        }
        else if (tN > tD)
        { // tc > 1 => the t=1 edge is visible
            tN = tD;
            // recompute sc for this edge
            if (-d + b < 0.0)
            {
                sN = 0;
            }
            else if (-d + b > a)
            {
                sN = sD;
            }
            else
            {
                sN = -d + b;
                sD = a;
            }
        }
        // finally do the division to build sc and tc
        sc = Math.abs(sN) < Global.DELTA ? 0.0 : sN / sD;
        tc = Math.abs(tN) < Global.DELTA ? 0.0 : tN / tD;

        // build the difference of the two closest points
        // = S1(sc) - S2(tc)
        Vect dp = w.copy();
        dp.plusEquals(sc, u);
        dp.minusEquals(tc, v);

        return dp.norm2(); // return the closest distance
    }

    public static Pair<Double,Double> nearest(Segment left, Segment right)
    {
        Vect pa = left.a;
        Vect pb = left.b;
        Vect qa = right.a;
        Vect qb = right.b;

        Vect u = pb.minus(pa);
        Vect v = qb.minus(qa);
        Vect w = pa.minus(qa);
        double a = u.dot(u); // always >= 0
        double b = u.dot(v);
        double c = v.dot(v); // always >= 0
        double d = u.dot(w);
        double e = v.dot(w);
        double D = a * c - b * b; // always >= 0
        double sc, sN, sD = D; // sc = sN / sD, default sD = D >= 0
        double tc, tN, tD = D; // tc = tN / tD, default tD = D >= 0

        // compute the line parameters of the two closest points
        if (D < Global.DELTA)
        { // the lines are almost parallel
            sN = 0.0; // force using point P0 on segment S1
            sD = 1.0; // to prevent possible division by 0.0 later
            tN = e;
            tD = c;
        }
        else
        { // build the closest points on the infinite lines
            sN = b * e - c * d;
            tN = a * e - b * d;
            if (sN < 0.0)
            { // sc < 0 => the s=0 edge is visible
                sN = 0.0;
                tN = e;
                tD = c;
            }
            else if (sN > sD)
            { // sc > 1 => the s=1 edge is visible
                sN = sD;
                tN = e + b;
                tD = c;
            }
        }

        if (tN < 0.0)
        { // tc < 0 => the t=0 edge is visible
            tN = 0.0;
            // recompute sc for this edge
            if (-d < 0.0)
            {
                sN = 0.0;
            }
            else if (-d > a)
            {
                sN = sD;
            }
            else
            {
                sN = -d;
                sD = a;
            }
        }
        else if (tN > tD)
        { // tc > 1 => the t=1 edge is visible
            tN = tD;
            // recompute sc for this edge
            if (-d + b < 0.0)
            {
                sN = 0;
            }
            else if (-d + b > a)
            {
                sN = sD;
            }
            else
            {
                sN = -d + b;
                sD = a;
            }
        }
        // finally do the division to build sc and tc
        sc = Math.abs(sN) < Global.DELTA ? 0.0 : sN / sD;
        tc = Math.abs(tN) < Global.DELTA ? 0.0 : tN / tD;

        return Pair.of(sc, tc);
    }

    public double nearest(Vect p)
    {
        Vect ba = b.minus(a);

        // test for equal endpoints
        if (MathUtils.zero(ba.sum()))
        {
            return 0;
        }

        // v = a + (b - a) * t
        double t = ba.dot(p.minus(a)) / ba.norm2();

        if (t > 0 - Global.DELTA && t < 1 + Global.DELTA)
        {
            return t;
        }
        else if (t < 0)
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }

    private double intersect(int dim, double val)
    {
        double a = this.a.get(dim);
        double b = this.b.get(dim);
        double d = b - a;

        if (d == 0.0)
        {
            if (a != val)
            {
                return -1;
            }
            else if (a == b)
            {
                return 0;
            }
        }

        return (val - a) / (b - a);
    }

    public boolean intersects(Box box)
    {
        for (int i = 0; i < box.dim(); i++)
        {
            double tn = this.intersect(i, box.range(i).getMin());
            double tf = this.intersect(i, box.range(i).getMax());

            if (MathUtils.unit(tn))
            {
                boolean hit = true;
                for (int j = 0; j < box.dim(); j++)
                {
                    if (j != i)
                    {
                        double va = this.a.get(j);
                        double vb = this.b.get(j);
                        double ov = tn * (vb - va) + va;
                        hit &= box.range(j).contains(ov);
                    }
                }
                if (hit)
                {
                    return true;
                }
            }

            if (MathUtils.unit(tf))
            {
                boolean hit = true;
                for (int j = 0; j < box.dim(); j++)
                {
                    if (j != i)
                    {
                        double va = this.a.get(j);
                        double vb = this.b.get(j);
                        double ov = tf * (vb - va) + va;
                        hit &= box.range(j).contains(ov);
                    }
                }
                if (hit)
                {
                    return true;
                }
            }
        }

        return false;
    }

    public double length()
    {
        return this.a.dist(this.b);
    }

    public Segment copy()
    {
        return new Segment(this.a.copy(), this.b.copy());
    }

    @Override
    public Box box()
    {
        return Box.createUnion(this.a, this.b);
    }
}
