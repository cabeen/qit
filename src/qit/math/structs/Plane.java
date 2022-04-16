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
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class Plane extends JsonDataset implements Intersectable
{
    public static Plane createNegativeX(double v)
    {
        return new Plane(1, 0, 0, -v);
    }

    public static Plane createNegativeY(double v)
    {
        return new Plane(0, 1, 0, -v);
    }

    public static Plane createNegativeZ(double v)
    {
        return new Plane(0, 0, 1, -v);
    }

    public static Plane createPositiveX(double v)
    {
        return new Plane(-1, 0, 0, -v);
    }

    public static Plane createPositiveY(double v)
    {
        return new Plane(0, -1, 0, -v);
    }

    public static Plane createPositiveZ(double v)
    {
        return new Plane(0, 0, -1, -v);
    }

    public static Plane fromPointNormal(Vect position, Vect normal)
    {
        Vect n = normal.normalize();
        double a = n.get(0);
        double b = n.get(1);
        double c = n.get(2);
        double d = -position.dot(n);

        return new Plane(a, b, c, d);
    }

    // Model the plane: a * x + b * y + c * z + d = 0
    private double a;
    private double b;
    private double c;
    private double d;

    public Plane(double a, double b, double c, double d)
    {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;

        double denom = Math.sqrt(this.a * this.a + this.b * this.b + this.c * this.c);

        Global.assume(!MathUtils.zero(denom), "invalid plane");
    }

    public double getA()
    {
        return this.a;
    }

    public double getB()
    {
        return this.b;
    }

    public double getC()
    {
        return this.c;
    }

    public double getD()
    {
        return this.d;
    }

    public Vect normal()
    {
        return VectSource.create3D(this.a, this.b, this.c).normalize();
    }

    public boolean contains(Vect point)
    {
        double x = point.get(0);
        double y = point.get(1);
        double z = point.get(2);

        double num = x * this.a + y * this.b + z * this.c + this.d;

        return num < 0;
    }

    public int label(Vect point)
    {
        return this.contains(point) ? 1 : 0;
    }

    public Vect point()
    {
        // the point closest to the origin
        return this.normal().times(-this.d);
    }

    public Vect nearest(Vect p)
    {
        Vect del = p.minus(this.point());
        Vect normal = this.normal();
        double dot = del.dot(normal);
        return p.minus(dot, normal);
    }

    public double dist(Vect vect)
    {
        Global.assume(vect != null, "null vector found");

        double x = vect.get(0);
        double y = vect.get(1);
        double z = vect.get(2);

        double num = x * this.a + y * this.b + z * this.c + this.d;
        double denom = Math.sqrt(this.a * this.a + this.b * this.b + this.c * this.c);

        return num / denom;
    }

    public List<LineIntersection> intersect(Line line)
    {
        // see http://paulbourke.net/geometry/pointlineplane/

        Vect p1 = line.getPoint();
        Vect p2 = line.getOtherPoint();
        Vect p3 = VectSource.create(this.a, this.b, this.c);

        double num = p1.dot(p3) + this.d;
        double denom = p3.dot(p1.minus(p2));

        List<LineIntersection> out = Lists.newArrayList();

        if (MathUtils.nonzero(denom))
        {
            // maybe handle this better
            Global.assume(MathUtils.nonzero(denom), "indeterminate solution");

            double u = num / denom;
            Vect v = line.getPoint(u);

            out.add(new LineIntersection(u, v));
        }

        return out;
    }

    public static Plane fit(Collection<Vect> vs)
    {
        // Find the plane that is the least-squares best-fit
        // Find P = argmin<sub>P</sub>||AP - B|| where P = (a, b, c),
        // B = (1, 1, 1) and a * x + b * y + c * z = 1
        // and A is the matrix of coordinates

        int N = vs.size();
        Matrix A = new Matrix(N, 3);
        Matrix B = new Matrix(N, 1);

        int i = 0;
        for (Vect v : vs)
        {
            A.set(i, 0, v.get(0));
            A.set(i, 1, v.get(1));
            A.set(i, 2, v.get(2));
            B.set(i, 0, 1);
            i++;
        }

        Matrix P = MatrixUtils.solve(A, B);

        // Normalize the solution so that we have
        // a * x + b * y + c * z + d = 0 and
        // a * a + b * b + c * c = 1
        double norm = P.normf();
        if (norm == 0)
        {
            throw new RuntimeException("The given face is degenerate");
        }

        double a = P.get(0, 0) / norm;
        double b = P.get(1, 0) / norm;
        double c = P.get(2, 0) / norm;
        double d = -1 / norm;

        return new Plane(a, b, c, d);
    }

    public Plane copy()
    {
        return new Plane(this.a, this.b, this.c, this.d);
    }

    public static Plane read(String fn) throws IOException
    {
        return JsonDataset.read(Plane.class, fn);
    }
}
