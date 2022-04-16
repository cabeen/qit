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

import qit.base.JsonDataset;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;

public class Triangle extends JsonDataset
{
    private Vect a;
    private Vect b;
    private Vect c;

    public Triangle(Vect a, Vect b, Vect c)
    {
        this.a = a.copy();
        this.b = b.copy();
        this.c = c.copy();
    }

    public Vect getA()
    {
        return this.a.copy();
    }

    public Vect getB()
    {
        return this.b.copy();
    }

    public Vect getC()
    {
        return this.c.copy();
    }

    public double area()
    {
        double ab = this.a.dist(this.b);
        double bc = this.b.dist(this.c);
        double ca = this.c.dist(this.a);
        double s = (ab + bc + ca) / 2;
        double p = s * (s - ab) * (s - bc) * (s - ca);

        // rounding error may push this below zero
        double area = p < 0 ? 0 : Math.sqrt(p);

        return area;
    }

    public Plane plane()
    {
        Vect bma = this.b.minus(this.a);
        Vect cma = this.c.minus(this.a);
        Vect norm = bma.cross(cma).normalize();

        double a = norm.get(0);
        double b = norm.get(1);
        double c = norm.get(2);
        double d = -norm.dot(this.a);

        return new Plane(a, b, c, d);
    }

    public Bary closest(Vect p)
    {
        /*
         * A function that computes the closest point in barycentric coordinates
         * (u, v, w) in a given triangle abc following relation: p = u * a + v *
         * b + w * c, subject to u + v + w = 1. Source: Real-Time Collision
         * Detection by Chister Ericson
         */

        // Check if P in vertex region outside A
        Vect ab = this.b.minus(this.a);
        Vect ac = this.c.minus(this.a);
        Vect ap = p.minus(this.a);
        double d1 = ab.dot(ap);
        double d2 = ac.dot(ap);

        if (d1 <= 0.0 & d2 <= 0.0)
        {
            return new Bary(1, 0, 0);
        }

        // Check if P in vertex region outside B
        Vect bp = p.minus(this.b);
        double d3 = ab.dot(bp);
        double d4 = ac.dot(bp);

        if (d3 >= 0.0 & d4 <= d3)
        {
            return new Bary(0, 1, 0);
        }

        // Check if P in edge region of AB, if so return projection of P onto AB
        double vc = d1 * d4 - d3 * d2;
        if (vc <= 0.0 & d1 >= 0.0 & d3 <= 0.0)
        {
            double v = d1 / (d1 - d3);
            return new Bary(1 - v, v, 0);
        }

        // Check if P in vertex region outside C
        Vect cp = p.minus(this.c);
        double d5 = ab.dot(cp);
        double d6 = ac.dot(cp);
        if (d6 >= 0.0 & d5 <= d6)
        {
            return new Bary(0, 0, 1);
        }

        // Check if P in edge region of AC, if so return projection of P onto AC
        double vb = d5 * d2 - d1 * d6;
        if (vb <= 0.0 & d2 >= 0.0 & d6 <= 0.0)
        {
            double w = d2 / (d2 - d6);
            return new Bary(1 - w, 0, w);
        }

        // Check if P in edge region of BC, if so return projection of P onto BC
        double va = d3 * d6 - d5 * d4;
        if (va <= 0.0 & d4 - d3 >= 0.0 & d5 - d6 >= 0.0)
        {
            double w = (d4 - d3) / (d4 - d3 + (d5 - d6));
            return new Bary(0, 1 - w, w);
        }

        // P inside face region, Compute Q through barycentric coordinates (u,
        // v, w)
        double denom = 1.0 / (va + vb + vc);
        double v = vb * denom;
        double w = vc * denom;
        // return a + ab * v + ac * w, which is u * a + v * b + w * c
        // u = va * denom = 1.0 - v - w
        double u = va * denom;
        return new Bary(u, v, w);
    }

    public double dist(Vect vect)
    {
        Bary bary = this.closest(vect);
        double d = vect.dist(this.vect(bary));
        return d;
    }

    public Vect vect(Bary bary)
    {
        Vect ua = this.a.times(bary.getU());
        Vect vb = this.b.times(bary.getV());
        Vect wc = this.c.times(bary.getW());
        return ua.plus(vb).plus(wc);
    }

    public Vect center()
    {
        return this.a.plus(this.b).plus(this.c).times(1.0 / 3.0);
    }

    public Vect nearest(Bary bary)
    {
        switch (VectSource.create3D(bary.getU(), bary.getV(), bary.getW()).maxidx())
        {
            case 0:
                return this.a;
            case 1:
                return this.b;
            default:
                return this.c;
        }
    }

    public Triangle copy()
    {
        return new Triangle(this.a.copy(), this.b.copy(), this.c.copy());
    }
}
