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
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.vects.stats.VectsStats;
import qit.data.utils.MatrixUtils;
import qit.math.utils.MathUtils;

public class Line extends JsonDataset
{
    // Model a line = p + t * d, for t in [-inf,inf] with |d| = 1
    private final Vect pos;
    private final Vect dir;

    public static Line fit(Vects vs)
    {
        Global.assume(vs.size() >= 2, "Cannot fit a line to fewer than two points");
        Global.assume(vs.getDim() == 3, "Vects must be three dimensional");

        // Find the least-squares best-fit line with homogeneous coordinates

        int n = vs.size();
        Matrix A = new Matrix(n, 3);
        Vect pos = new VectsStats().withInput(vs).run().mean;

        int i = 0;
        for (Vect v : vs)
        {
            A.set(i, 0, v.get(0) - pos.get(0));
            A.set(i, 1, v.get(1) - pos.get(1));
            A.set(i, 2, v.get(2) - pos.get(2));
            i++;
        }

        Matrix B = A.transpose().times(A).times(1.0 / n);
        Vect dir = MatrixUtils.eig(B).vectors.get(0);

        return fromPosDir(pos, dir);
    }

    public static Line fromTwoPoints(Vect a, Vect b)
    {
        Vect p = a;
        Vect d = b.minus(a);
        return new Line(p, d);
    }

    public static Line fromPosDir(Vect pos, Vect dir)
    {
        return new Line(pos, dir);
    }

    private Line(Vect s, Vect d)
    {
        this.pos = s.copy();
        this.dir = d.copy();
    }

    public Vect getPoint()
    {
        return this.pos.copy();
    }

    public Vect getOtherPoint()
    {
        return this.pos.plus(this.dir);
    }

    public Vect getPoint(double u)
    {
        return this.pos.plus(u, this.dir);
    }

    public Vect getDir()
    {
        return this.dir.copy();
    }

    public Pair<Double,Vect> nearestParam(Vect p)
    {
        // see http://paulbourke.net/geometry/pointlineplane/
        double num = p.minus(this.pos).dot(this.dir);
        double denom = this.dir.norm2();

        // maybe handle this better
        Global.assume(MathUtils.nonzero(denom), "indeterminate solution");

        double s = num / denom;
        Vect inter = this.pos.plus(s, dir);

        return Pair.of(s, inter);
    }

    public Vect nearest(Vect p)
    {
        return nearestParam(p).b;
    }

    public double dist(Vect p)
    {
        return this.nearest(p).dist(p);
    }

    private Pair<Double, Double> nearestPair(Line line)
    {
        Vect p1 = this.pos;
        Vect p2 = p1.plus(this.dir);
        Vect p3 = line.pos;
        Vect p4 = p3.plus(line.dir);

        double p1x = p1.get(0);
        double p1y = p1.get(1);
        double p1z = p1.get(2);
        double p2x = p2.get(0);
        double p2y = p2.get(1);
        double p2z = p2.get(2);
        double p3x = p3.get(0);
        double p3y = p3.get(1);
        double p3z = p3.get(2);
        double p4x = p4.get(0);
        double p4y = p4.get(1);
        double p4z = p4.get(2);

        double p13x = p1x - p3x;
        double p13y = p1y - p3y;
        double p13z = p1z - p3z;
        double p43x = p4x - p3x;
        double p43y = p4y - p3y;
        double p43z = p4z - p3z;

        Global.assume(!(Math.abs(p43x) < Global.DELTA && Math.abs(p43y) < Global.DELTA && Math.abs(p43z) < Global.DELTA), "indeterminante solution");

        double p21x = p2x - p1x;
        double p21y = p2y - p1y;
        double p21z = p2z - p1z;

        Global.assume(!(Math.abs(p21x) < Global.DELTA && Math.abs(p21y) < Global.DELTA && Math.abs(p21z) < Global.DELTA), "indeterminate solution");

        double d1343 = p13x * p43x + p13y * p43y + p13z * p43z;
        double d4321 = p43x * p21x + p43y * p21y + p43z * p21z;
        double d1321 = p13x * p21x + p13y * p21y + p13z * p21z;
        double d4343 = p43x * p43x + p43y * p43y + p43z * p43z;
        double d2121 = p21x * p21x + p21y * p21y + p21z * p21z;

        double denom = d2121 * d4343 - d4321 * d4321;

        Global.assume(Math.abs(denom) > Global.DELTA, "indeterminate solution");

        double numer = d1343 * d4321 - d1321 * d4343;

        double mua = numer / denom;
        double mub = (d1343 + d4321 * mua) / d4343;

        return Pair.of(mua, mub);
    }

    public Vect nearest(Line line)
    {
        Pair<Double, Double> pair = nearestPair(line);

        return this.getPoint(pair.a);
    }

    public double dist(Line line)
    {
        Pair<Double, Double> pair = nearestPair(line);

        return this.getPoint(pair.a).dist(line.getPoint(pair.b));
    }

    public Line copy()
    {
        return new Line(this.pos.copy(), this.dir.copy());
    }
}
