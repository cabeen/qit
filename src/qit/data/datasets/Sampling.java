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
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import qit.base.Global;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.utils.JsonUtils;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.math.structs.Box;
import qit.math.structs.Containable;
import qit.math.structs.Quaternion;

import java.util.Iterator;
import qit.math.structs.Segment;
import qit.math.utils.MathUtils;

public class Sampling implements Iterable<Sample>
{
    private final Vect start;
    private final Vect delta;
    private final Integers num;
    private final Quaternion quat; // nullable
    private final int size;

    // store so they don't have to be recomputed
    private transient final Matrix mat;
    private transient final Matrix invmat;

    public Sampling(Vect s, Vect d, Quaternion r, Integers n)
    {
        Global.assume(s.size() == 3, "invalid start vector");
        Global.assume(d.size() == 3, "invalid delta vector");
        Global.assume(n.size() == 3, "invalid nums");
        Global.assume(n.getI() > 0, "invalid samples in x");
        Global.assume(n.getJ() > 0, "invalid samples in y");
        Global.assume(n.getK() > 0, "invalid samples in z");
        Global.assume(d.getX() > 0, "invalid delta in x");
        Global.assume(d.getY() > 0, "invalid delta in y");
        Global.assume(d.getZ() > 0, "invalid delta in z");

        this.start = s.copy();
        this.delta = d.copy();
        this.num = n.copy();

        if (r != null)
        {
            this.quat = r.copy();
            this.mat = r.matrix();
            this.invmat = this.mat.inv();
        }
        else
        {
            this.quat = new Quaternion(0, 0, 0);
            this.mat = MatrixSource.identity(3);
            this.invmat = MatrixSource.identity(3);
        }

        this.size = this.num.getI() * this.num.getJ() * this.num.getK();
    }

    public Sampling(Vect s, Vect d, Integers n)
    {
        this(s, d, null, n);
    }

    public int numI()
    {
        return this.num.get(0);
    }

    public int numJ()
    {
        return this.num.get(1);
    }

    public int numK()
    {
        return this.num.get(2);
    }

    public Integers num()
    {
        return this.num.copy();
    }

    public int num(int idx)
    {
        return this.num.get(idx);
    }

    public Sample first()
    {
        return this.sample(0);
    }

    public Sample last()
    {
        return this.sample(this.size() - 1);
    }

    public Sample center()
    {
        int i = (this.numI() - 1) / 2;
        int j = (this.numJ() - 1) / 2;
        int k = (this.numK() - 1) / 2;

        return new Sample(i, j, k);
    }

    public boolean contains(int idx)
    {
        return idx >= 0 && idx < this.size;
    }

    public boolean containsI(int i)
    {
        if (i < 0 || i >= this.num.get(0))
        {
            return false;
        }

        return true;
    }

    public boolean containsJ(int j)
    {
        if (j < 0 || j >= this.num.get(1))
        {
            return false;
        }

        return true;
    }

    public boolean containsK(int k)
    {
        if (k < 0 || k >= this.num.get(2))
        {
            return false;
        }

        return true;
    }

    public boolean contains(int i, int j, int k)
    {
        if (i < 0 || i >= this.num.get(0))
        {
            return false;
        }

        if (j < 0 || j >= this.num.get(1))
        {
            return false;
        }

        if (k < 0 || k >= this.num.get(2))
        {
            return false;
        }

        return true;
    }

    public boolean contains(Sample sample)
    {
        return this.contains(sample.getI(), sample.getJ(), sample.getK());
    }

    public boolean containsExclusive(int i, int j, int k)
    {
        if (i < 0 || i > this.num.get(0))
        {
            return false;
        }

        if (j < 0 || j > this.num.get(1))
        {
            return false;
        }

        if (k < 0 || k > this.num.get(2))
        {
            return false;
        }

        return true;
    }

    public boolean containsExclusive(Sample sample)
    {
        return this.containsExclusive(sample.getI(), sample.getJ(), sample.getK());
    }

    public List<Sample> traverse(Segment segment)
    {
        Sample s0 = this.nearest(segment.getA());
        Sample s1 = this.nearest(segment.getB());

        if (s0.equals(s1))
        {
            List<Sample> out = Lists.newArrayList();
            out.add(s0);
            return out;
        }
        else
        {
            Vect n0 = this.world(s0);

            int i0 = s0.get(0);
            int j0 = s0.get(1);
            int k0 = s0.get(2);

            int i1 = s1.get(0);
            int j1 = s1.get(1);
            int k1 = s1.get(2);

            int iMin = Math.min(i0, i1);
            int jMin = Math.min(j0, j1);
            int kMin = Math.min(k0, k1);

            int iMax = Math.max(i0, i1);
            int jMax = Math.max(j0, j1);
            int kMax = Math.max(k0, k1);

            double iDelta = this.deltaI();
            double jDelta = this.deltaJ();
            double kDelta = this.deltaK();

            int iNum = this.numI();
            int jNum = this.numJ();
            int kNum = this.numK();

            double xNear = n0.get(0);
            double yNear = n0.get(1);
            double zNear = n0.get(2);

            Vect a = segment.getA();
            Vect b = segment.getB();

            double x0 = a.get(0);
            double y0 = a.get(1);
            double z0 = a.get(2);

            double x1 = b.get(0);
            double y1 = b.get(1);
            double z1 = b.get(2);

            double iChange = x0 < x1 ? 1 : -1;
            double jChange = y0 < y1 ? 1 : -1;
            double kChange = z0 < z1 ? 1 : -1;

            double xDelta = Math.abs(x1 - x0);
            double yDelta = Math.abs(y1 - y0);
            double zDelta = Math.abs(z1 - z0);

            double tDeltaX = MathUtils.zero(xDelta) ? Double.MAX_VALUE : iDelta / xDelta;
            double tDeltaY = MathUtils.zero(yDelta) ? Double.MAX_VALUE : jDelta / yDelta;
            double tDeltaZ = MathUtils.zero(zDelta) ? Double.MAX_VALUE : kDelta / zDelta;

            double tmx = MathUtils.zero(xDelta) ? Double.MAX_VALUE : (xNear + 0.5 * iDelta - x0) / xDelta;
            double tmy = MathUtils.zero(yDelta) ? Double.MAX_VALUE : (yNear + 0.5 * jDelta - y0) / yDelta;
            double tmz = MathUtils.zero(zDelta) ? Double.MAX_VALUE : (zNear + 0.5 * kDelta - z0) / zDelta;

            int i = i0;
            int j = j0;
            int k = k0;

            List<Sample> out = Lists.newArrayList();
            out.add(new Sample(i, j, k));
            while (this.contains(i, j, k))
            {
                if (tmx < tmy)
                {
                    if (tmx < tmz)
                    {
                        i += iChange;
                        if (i == iNum || i < 0)
                        {
                            break;
                        }
                        tmx += tDeltaX;
                    }
                    else
                    {
                        k += kChange;
                        if (k == kNum || k < 0)
                        {
                            break;
                        }
                        tmz += tDeltaZ;
                    }
                }
                else
                {
                    if (tmy < tmz)
                    {
                        j += jChange;
                        if (j == jNum || j < 0)
                        {
                            break;
                        }
                        tmy += tDeltaY;
                    }
                    else
                    {
                        k += kChange;
                        if (k == kNum || k < 0)
                        {
                            break;
                        }
                        tmz += tDeltaZ;
                    }
                }

                Sample ns = new Sample(i, j, k);
                out.add(new Sample(i, j, k));

                if (ns.equals(s1) || i < iMin || i > iMax || j < jMin || j > jMax || k < kMin || k > kMax)
                {
                    break;
                }
            }

            return out;
        }
    }

    public boolean compatible(Sampling sampling)
    {
        // only approximate tests for floating point values

        boolean out = true;
        out &= this.num().equals(sampling.num());
        out &= this.delta().minus(sampling.delta()).norm() < 1e-3;
        out &= this.start().minus(sampling.start()).norm() < 1e-3;

        if (this.quat() != null)
        {
            out &= this.quat().matrix().minus(sampling.quat().matrix()).normF() < 1e-3;
        }

        return out;
    }

    public List<Sample> traverse(Vects polyline)
    {
        List<Sample> out = Lists.newArrayList();
        for (int i = 1; i < polyline.size(); i++)
        {
            Vect a = polyline.get(i - 1);
            Vect b = polyline.get(i);
            List<Sample> sub = this.traverse(new Segment(a, b));

            while (sub.size() > 0 && out.size() > 0 && out.get(out.size() - 1).equals(sub.get(0)))
            {
                sub.remove(0);
            }

            out.addAll(sub);
        }

        return out;
    }

    public List<Pair<Sample,Vect>> traverseLine(Vects polyline)
    {
        List<Pair<Sample,Vect>> out = Lists.newArrayList();
        for (int i = 1; i < polyline.size(); i++)
        {
            Vect a = polyline.get(i - 1);
            Vect b = polyline.get(i);
            Segment segment = new Segment(a, b);
            List<Sample> sub = this.traverse(segment);

            while (sub.size() > 0 && out.size() > 0 && out.get(out.size() - 1).equals(sub.get(0)))
            {
                sub.remove(0);
            }

            Vect line = segment.getB().minus(segment.getA()).normalize();

            for (Sample s : sub)
            {
                out.add(Pair.of(s, line));
            }
        }

        return out;
    }

    public List<Pair<Sample,Vect>> traverseAttribute(Vects polyline, Vects attrs)
    {
        Global.assume(polyline.size() == attrs.size(), "polyline and attributes do not match");

        List<Pair<Sample,Vect>> out = Lists.newArrayList();
        for (int i = 1; i < polyline.size(); i++)
        {
            Vect a = polyline.get(i - 1);
            Vect b = polyline.get(i);

            Vect va = attrs.get(i - 1);
            Vect vb = attrs.get(i);

            List<Sample> sub = this.traverse(new Segment(a, b));

            while (sub.size() > 0 && out.size() > 0 && out.get(out.size() - 1).equals(sub.get(0)))
            {
                sub.remove(0);
            }

            for (Sample s : sub)
            {
                Vect p = this.world(s);
                Vect nearest = p.dist(a) < p.dist(b) ? va : vb;
                out.add(Pair.of(s, nearest));
            }
        }

        return out;
    }

    public Sample mirror(Sample sample)
    {
        // if the voxel is off boundary, return a mirror sample
        // if not, return the input sample

        int i = sample.getI();
        int j = sample.getJ();
        int k = sample.getK();
        int ni = this.numI();
        int nj = this.numJ();
        int nk = this.numK();

        if (i < 0)
        {
            i = -i;
        }

        if (j < 0)
        {
            j = -j;
        }

        if (k < 0)
        {
            k = -k;
        }

        if (i >= ni)
        {
            i = 2 * ni - i - 1;
        }
        if (j >= nj)
        {
            j = 2 * nj - j - 1;
        }
        if (k >= nk)
        {
            k = 2 * nk - k - 1;
        }

        return new Sample(i, j, k);
    }

    public boolean boundary(int idx)
    {
        return this.boundary(this.sample(idx));
    }

    public boolean boundary(int i, int j, int k)
    {
        if (i == 0 || i == this.num.get(0) - 1)
        {
            return true;
        }

        if (j == 0 || j == this.num.get(1) - 1)
        {
            return true;
        }

        if (k == 0 || k == this.num.get(2) - 1)
        {
            return true;
        }

        return false;
    }

    public boolean boundary(Sample sample)
    {
        return this.boundary(sample.getI(), sample.getJ(), sample.getK());
    }

    public Sample sample(int idx)
    {
        int i = idx % this.num.get(0);
        int tmp = (idx - i) / this.num.get(0);
        int j = tmp % this.num.get(1);
        int k = (tmp - j) / this.num.get(1);

        return new Sample(i, j, k);
    }

    public int index(int i, int j, int k)
    {
        int idx = i + this.num.get(0) * j + k * this.num.get(0) * this.num.get(1);
        if (idx >= this.size)
        {
            throw new RuntimeException("invalid sample " + i + ", " + j + ", " + k + " in sampling " + this);
        }

        return idx;
    }

    public Affine affine()
    {
        Quaternion quat = this.quat();
        Vect start = this.start();
        return new Affine(quat.matrix(), start);
    }

    public int index(Sample s)
    {
        return this.index(s.getI(), s.getJ(), s.getK());
    }

    public int size()
    {
        return this.size;
    }

    public Quaternion quat()
    {
        return this.quat;
    }

    public double quatA()
    {
        return this.quat.getA();
    }

    public double quatB()
    {
        return this.quat.getB();
    }

    public double quatC()
    {
        return this.quat.getC();
    }

    public double quatD()
    {
        return this.quat.getD();
    }

    public Vect start()
    {
        return this.start.copy();
    }

    public double start(int idx)
    {
        return this.start.get(idx);
    }

    public double startI()
    {
        return this.start.get(0);
    }

    public double startJ()
    {
        return this.start.get(1);
    }

    public double startK()
    {
        return this.start.get(2);
    }

    public Vect delta()
    {
        return this.delta.copy();
    }

    public double delta(int idx)
    {
        return this.delta.get(idx);
    }

    public double deltaI()
    {
        return this.delta.get(0);
    }

    public double deltaJ()
    {
        return this.delta.get(1);
    }

    public double deltaK()
    {
        return this.delta.get(2);
    }

    public int numMin()
    {
        return Math.min(this.num.get(0), Math.min(this.num.get(1), this.num.get(2)));
    }

    public int numMax()
    {
        return Math.max(this.num.get(0), Math.max(this.num.get(1), this.num.get(2)));
    }

    public double deltaMin()
    {
        return Math.min(this.delta.get(0), Math.min(this.delta.get(1), this.delta.get(2)));
    }

    public double deltaMax()
    {
        return Math.max(this.delta.get(0), Math.max(this.delta.get(1), this.delta.get(2)));
    }

    public double deltaDiag()
    {
        double di = this.deltaI();
        double dj = this.deltaJ();
        double dk = this.deltaK();

        return Math.sqrt(di * di + dj * dj + dk * dk);
    }

    public Vect random(int i, int j, int k)
    {
        // uniformly random sample from a voxel

        double vx = Global.RANDOM.nextDouble() + (i - 0.5);
        double vy = Global.RANDOM.nextDouble() + (j - 0.5);
        double vz = Global.RANDOM.nextDouble() + (k - 0.5);
        Vect world = this.world(VectSource.create3D(vx, vy, vz));

        return world;
    }

    public Vect random(Sample sample)
    {
        return random(sample.getI(), sample.getJ(), sample.getK());
    }

    public Box bounds()
    {
        int mi = this.numI() - 1;
        int mj = this.numJ() - 1;
        int mk = this.numK() - 1;

        Vect v000 = this.world(new Sample(0, 0, 0));
        Vect v001 = this.world(new Sample(0, 0, mk));
        Vect v010 = this.world(new Sample(0, mj, 0));
        Vect v100 = this.world(new Sample(mi, 0, 0));
        Vect v011 = this.world(new Sample(0, mj, mk));
        Vect v110 = this.world(new Sample(mi, mj, 0));
        Vect v101 = this.world(new Sample(mi, 0, mk));
        Vect v111 = this.world(new Sample(mi, mj, mk));

        Box out = Box.createUnion(v000, v111);
        out = out.union(v001);
        out = out.union(v010);
        out = out.union(v100);
        out = out.union(v011);
        out = out.union(v110);
        out = out.union(v101);

        return out;
    }

    public boolean contains(Vect p)
    {
        return this.contains(this.nearest(p));
    }

    public double voxvol()
    {
        return this.delta.get(0) * this.delta.get(1) * this.delta.get(2);
    }

    public Vect world(int idx)
    {
        return this.world(this.sample(idx));
    }

    public Vect world(Sample sample)
    {
        return this.world(sample.getI(), sample.getJ(), sample.getK());
    }

    public Vect world(int i, int j, int k)
    {
        return this.world(VectSource.create(i, j, k));
    }

    public Vect world(Vect voxel)
    {
        return this.start.plus(this.mat.times(this.delta.times(voxel)));
    }

    public Vect voxel(Vect p)
    {
        return this.invmat.times(p.minus(this.start)).times(this.delta.recip());
    }

    public boolean planar()
    {
        boolean nox = this.numI() == 1;
        boolean noy = this.numJ() == 1;
        boolean noz = this.numK() == 1;

        boolean oxy = !nox && !noy && noz;
        boolean oxz = !nox && noy && !noz;
        boolean oyz = nox && !noy && !noz;

        return oxy || oxz || oyz;
    }

    public Sample nearestInside(Vect p)
    {
        Vect voxel = this.voxel(p);

        int i = (int) Math.round(voxel.getX());
        int j = (int) Math.round(voxel.getY());
        int k = (int) Math.round(voxel.getZ());

        i = Math.min(this.numI() - 1, Math.max(0, i));
        j = Math.min(this.numJ() - 1, Math.max(0, j));
        k = Math.min(this.numK() - 1, Math.max(0, k));

        return new Sample(i, j, k);
    }

    public Sample nearest(Vect p)
    {
        Vect voxel = this.voxel(p);

        int i = (int) Math.round(voxel.getX());
        int j = (int) Math.round(voxel.getY());
        int k = (int) Math.round(voxel.getZ());

        return new Sample(i, j, k);
    }

    public Sampling crop(Containable select)
    {
        if (select == null)
        {
            return this.copy();
        }

        int maxI = 0;
        int maxJ = 0;
        int maxK = 0;
        int minI = this.numI();
        int minJ = this.numJ();
        int minK = this.numK();

        for (Sample sample : this)
        {
            if (select.contains(this.world(sample)))
            {
                maxI = Math.max(sample.getI(), maxI);
                maxJ = Math.max(sample.getJ(), maxJ);
                maxK = Math.max(sample.getK(), maxK);
                minI = Math.min(sample.getI(), minI);
                minJ = Math.min(sample.getJ(), minJ);
                minK = Math.min(sample.getK(), minK);
            }
        }

        int numI = maxI - minI;
        int numJ = maxJ - minJ;
        int numK = maxK - minK;

        if (numI <= 0 || numJ <= 0 || numK <= 0)
        {
            return this.copy();
        }

        return new Sampling(this.world(new Sample(minI, minJ, minK)), this.delta, this.quat, new Integers(numI, numJ, numK));
    }

    public Iterable<Sample> iterateBox(Box box)
    {
        Vect min = box.getMin();
        Vect max = box.getMax();

        int iMin = this.numI() - 1;
        int jMin = this.numJ() - 1;
        int kMin = this.numK() - 1;
        int iMax = 0;
        int jMax = 0;
        int kMax = 0;
        
        Vects corners = new Vects();
        corners.add(this.voxel(VectSource.create3D(min.getX(), min.getY(), min.getZ())));
        corners.add(this.voxel(VectSource.create3D(min.getX(), min.getY(), max.getZ())));
        corners.add(this.voxel(VectSource.create3D(min.getX(), max.getY(), min.getZ())));
        corners.add(this.voxel(VectSource.create3D(min.getX(), max.getY(), max.getZ())));
        corners.add(this.voxel(VectSource.create3D(max.getX(), min.getY(), min.getZ())));
        corners.add(this.voxel(VectSource.create3D(max.getX(), min.getY(), max.getZ())));
        corners.add(this.voxel(VectSource.create3D(max.getX(), max.getY(), min.getZ())));
        corners.add(this.voxel(VectSource.create3D(max.getX(), max.getY(), max.getZ())));
        
        for (Vect corner : corners)
        {
            iMin = (int) Math.min(iMin, Math.floor(corner.getX()));
            jMin = (int) Math.min(jMin, Math.floor(corner.getY()));
            kMin = (int) Math.min(kMin, Math.floor(corner.getZ()));
            iMax = (int) Math.max(iMax, Math.ceil(corner.getX()));
            jMax = (int) Math.max(jMax, Math.ceil(corner.getY()));
            kMax = (int) Math.max(kMax, Math.ceil(corner.getZ()));
        }

        final int iStart = Math.max(0, Math.min(this.numI(), iMin));
        final int iEnd   = Math.max(0, Math.min(this.numI(), iMax));
        final int jStart = Math.max(0, Math.min(this.numJ(), jMin));
        final int jEnd   = Math.max(0, Math.min(this.numJ(), jMax));
        final int kStart = Math.max(0, Math.min(this.numK(), kMin));
        final int kEnd   = Math.max(0, Math.min(this.numK(), kMax));

        final int iNum = Math.max(0, iEnd - iStart);
        final int jNum = Math.max(0, jEnd - jStart);
        final int kNum = Math.max(0, kEnd - kStart);
        
        final int ijkNum = iNum * jNum * kNum;

        return new Iterable<Sample>()
        {
            @Override
            public Iterator<Sample> iterator()
            {
                return new Iterator<Sample>()
                {
                    private int idx = 0;

                    public boolean hasNext()
                    {
                        return this.idx < ijkNum - 1;
                    }

                    public Sample next()
                    {
                        int di = this.idx % iNum;
                        int tmp = (this.idx - di) / iNum;
                        int dj = tmp % jNum;
                        int dk = (tmp - dj) / jNum;

                        int i = iStart + di;
                        int j = jStart + dj;
                        int k = kStart + dk;

                        Sample out = new Sample(i, j, k);
                        this.idx += 1;

                        return out;
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException("Operation not supported for sample iterators");
                    }
                };
            }
        };
    }

    public Sampling range(String spec)
    {
        if (spec == null || "all".equals(spec) || "whole".equals(spec))
        {
            return this;
        }

        String[] tokens = StringUtils.split(spec, ",");

        if (tokens.length != 3)
        {
            throw new RuntimeException("invalid range elements: " + spec);
        }

        int[] starts = new int[3];
        int[] deltas = new int[3];
        int[] num = new int[3];

        for (int i = 0; i < 3; i++)
        {
            String token = tokens[i];
            try
            {
                // Check for lone index
                Integer value = Integer.valueOf(token);
                starts[i] = value;
                deltas[i] = 1;
                num[i] = 1;
                continue;
            }
            catch (NumberFormatException e)
            {
                // continue;
            }
            try
            {
                // check for slicing
                String start = "start";
                String delta = "1";
                String end = "end";

                if (token.contains(":"))
                {
                    String[] slicing = StringUtils.split(token, ":");
                    if (slicing.length == 1)
                    {
                        if (token.startsWith(":"))
                        {
                            end = token;
                        }
                        else
                        {
                            start = token;
                        }
                    }
                    else if (slicing.length == 2)
                    {
                        start = slicing[0];
                        end = slicing[1];
                    }
                    else if (slicing.length == 3)
                    {
                        start = slicing[0];
                        delta = slicing[1];
                        end = slicing[2];
                    }
                    else if (slicing.length > 3)
                    {
                        throw new RuntimeException("invalid range slicing: " + spec);
                    }
                }
                else
                {
                    int idx = Integer.valueOf(token);
                    start = Integer.toString(idx);
                    end = Integer.toString(idx + 1);
                }

                int startidx = start.equals("start") ? 0 : Integer.valueOf(start);
                int deltaidx = Integer.valueOf(delta);
                int endidx = end.equals("end") ? this.num(i) : Integer.valueOf(end);

                if (startidx < 0)
                {
                    startidx = this.num(i) + startidx;
                }

                if (endidx < 0)
                {
                    endidx = this.num(i) + endidx;
                }

                if (startidx < 0 || startidx >= this.num(i))
                {
                    throw new RuntimeException("invalid range start: " + spec);
                }

                if (deltaidx < 1)
                {
                    throw new RuntimeException("invalid range delta: " + spec);
                }

                if (endidx < 1 || endidx > this.num(i))
                {
                    throw new RuntimeException("invalid range end: " + spec);
                }

                if (endidx <= startidx)
                {
                    throw new RuntimeException("invalid range range: " + spec);
                }

                starts[i] = startidx;
                deltas[i] = deltaidx;
                num[i] = Math.min(this.num(i) - startidx, endidx - startidx) / deltaidx;
            }
            catch (NumberFormatException e)
            {
                throw new RuntimeException("invalid range number format: " + spec);
            }
        }

        double ndx = this.delta.get(0) * deltas[0];
        double ndy = this.delta.get(1) * deltas[1];
        double ndz = this.delta.get(2) * deltas[2];
        int nnx = num[0];
        int nny = num[1];
        int nnz = num[2];

        Vect start = this.world(starts[0], starts[1], starts[2]);
        Vect delta = VectSource.create3D(ndx, ndy, ndz);
        Integers nums = new Integers(nnx, nny, nnz);

        return new Sampling(start, delta, this.quat, nums);
    }

    public Sampling range(Sample start, Sample end)
    {
        // right now this is exclusive of "end", maybe change this?

        Global.assume(this.contains(start), "invalid starting sample");
        Global.assume(this.containsExclusive(end), "invalid ending sample");

        int nx = end.getI() - start.getI();
        int ny = end.getJ() - start.getJ();
        int nz = end.getK() - start.getK();

        Integers nums = new Integers(nx, ny, nz);

        return new Sampling(this.world(start), this.delta, this.quat, nums);
    }

    public Sampling grow(int num)
    {
        int numX = this.numI() + 2 * num;
        int numY = this.numJ() + 2 * num;
        int numZ = this.numK() + 2 * num;

        return new Sampling(this.world(-num, -num, -num), this.delta, this.quat, new Integers(numX, numY, numZ));
    }

    public Sampling resample(int num)
    {
        return this.resample(num, num, num);
    }

    public Sampling resample(int numX, int numY, int numZ)
    {
        Global.assume(numX > 0, "invalid number of samples: " + numX);
        Global.assume(numY > 0, "invalid number of samples: " + numY);
        Global.assume(numZ > 0, "invalid number of samples: " + numZ);

        double deltaX = this.deltaI();
        double deltaY = this.deltaJ();
        double deltaZ = this.deltaK();

        if (numX != this.numI() && numX > 1 && this.numI() > 1)
        {
            deltaX = (this.numI() - 1) * this.deltaI() / (numX - 1);
        }

        if (numY != this.numJ() && numY > 1 && this.numJ() > 1)
        {
            deltaY = (this.numJ() - 1) * this.deltaJ() / (numY - 1);
        }

        if (numZ != this.numK() && numZ > 1 && this.numK() > 1)
        {
            deltaZ = (this.numK() - 1) * this.deltaK() / (numZ - 1);
        }

        Vect delta = VectSource.create3D(deltaX, deltaY, deltaZ);
        Integers nums = new Integers(numX, numY, numZ);

        return new Sampling(this.start, delta, this.quat, nums);
    }

    public Sampling resample(double delta)
    {
        return this.resample(delta, delta, delta);
    }

    public Sampling resample(double dx, double dy, double dz)
    {
        Global.assume(dx > 0, "invalid step size");
        Global.assume(dy > 0, "invalid step size");
        Global.assume(dz > 0, "invalid step size");

        double sizeX = this.numI() * this.deltaI();
        double sizeY = this.numJ() * this.deltaJ();
        double sizeZ = this.numK() * this.deltaK();

        int numX = (int) Math.ceil(sizeX / dx);
        int numY = (int) Math.ceil(sizeY / dy);
        int numZ = (int) Math.ceil(sizeZ / dz);

        return new Sampling(this.start, VectSource.create3D(dx, dy, dz), this.quat, new Integers(numX, numY, numZ));
    }

    public Sampling proto(Integers n)
    {
        return new Sampling(this.start.copy(), this.delta.copy(), this.quat.copy(), n);
    }

    public Sampling proto(Vect d, Integers n)
    {
        return new Sampling(this.start.copy(), d, this.quat.copy(), n);
    }

    public Sampling zoom(double fi, double fj, double fk)
    {
        int nx = this.numI() == 1 ? this.numI() : (int) Math.round(fi * this.numI());
        int ny = this.numJ() == 1 ? this.numJ() : (int) Math.round(fj * this.numJ());
        int nz = this.numK() == 1 ? this.numK() : (int) Math.round(fk * this.numK());

        nx = Math.max(1, nx);
        ny = Math.max(1, ny);
        nz = Math.max(1, nz);

        return this.resample(nx, ny, nz);
    }

    public Sampling zoom(double factor)
    {
        return zoom(factor, factor, factor);
    }

    public Vects corners()
    {
        Vects out = new Vects();

        out.add(this.world(0, 0, 0));
        out.add(this.world(this.numI() - 1, 0, 0));
        out.add(this.world(0, this.numJ() - 1, 0));
        out.add(this.world(this.numI() - 1, this.numJ() - 1, 0));
        out.add(this.world(0, 0, this.numK() - 1));
        out.add(this.world(this.numI() - 1, 0, this.numK() - 1));
        out.add(this.world(0, this.numJ() - 1, this.numK() - 1));
        out.add(this.world(this.numI() - 1, this.numJ() - 1, this.numK() - 1));

        return out;
    }

    public Sampling copy()
    {
        return new Sampling(this.start, this.delta, this.quat, this.num);
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Sampling))
        {
            return false;
        }

        if (obj == this)
        {
            return true;
        }

        Sampling s = (Sampling) obj;

        boolean out = true;
        out &= s.num.equals(this.num);
        out &= s.delta.equals(this.delta);
        out &= s.start.equals(this.start);

        if (this.quat != null)
        {
            out &= s.quat.equals(this.quat);
        }

        return out;
    }

    public String toString()
    {
        return JsonUtils.encode(this);
    }

    public int hashCode()
    {
        return this.numI() + this.numJ() + this.numK() + this.size;
    }

    public Iterable<Sample> iterateNeighborhood(final int r)
    {
        return iterateNeighborhood(new Sample(0, 0, 0), new Integers(r, r, r));
    }

    public Iterable<Sample> iterateNeighborhood(Sample sample, final int r)
    {
        return iterateNeighborhood(sample, new Integers(r, r, r));
    }

    public Iterable<Sample> iterateNeighborhood(final Integers radius)
    {
        return iterateNeighborhood(new Sample(0, 0, 0), radius);
    }

    public Iterable<Sample> iterateNeighborhood(final Sample sample, final Integers radius)
    {
        return new Iterable<Sample>()
        {
            @Override
            public Iterator<Sample> iterator()
            {
                return new SampleIteratorNeighborhood(sample, radius);
            }
        };
    }

    public Iterable<Sample> iterateI(int i)
    {
        final int fi = i;
        return new Iterable<Sample>()
        {
            @Override
            public Iterator<Sample> iterator()
            {
                return new SampleIteratorI(fi);
            }
        };
    }

    public Iterable<Sample> iterateJ(int j)
    {
        final int fj = j;
        return new Iterable<Sample>()
        {
            @Override
            public Iterator<Sample> iterator()
            {
                return new SampleIteratorJ(fj);
            }
        };
    }

    public Iterable<Sample> iterateK(int k)
    {
        final int fk = k;
        return new Iterable<Sample>()
        {
            @Override
            public Iterator<Sample> iterator()
            {
                return new SampleIteratorK(fk);
            }
        };
    }

    public Iterator<Sample> iterator()
    {
        return new SampleIterator();
    }

    private class SampleIterator implements Iterator<Sample>
    {
        private int idx = 0;

        public boolean hasNext()
        {
            return this.idx < Sampling.this.size;
        }

        public Sample next()
        {
            return Sampling.this.sample(this.idx++);
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Operation not supported for sample iterators");
        }
    }

    private class SampleIteratorI implements Iterator<Sample>
    {
        private int i = 0;
        private int idx = 0;

        public SampleIteratorI(int i)
        {
            this.i = i;
        }

        public boolean hasNext()
        {
            return this.idx < Sampling.this.numJ() * Sampling.this.numK();
        }

        public Sample next()
        {
            int j = this.idx % Sampling.this.numJ();
            int k = (this.idx - j) / Sampling.this.numJ();
            Sample out = new Sample(this.i, j, k);
            this.idx += 1;

            return out;
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Operation not supported for sample iterators");
        }
    }

    private class SampleIteratorJ implements Iterator<Sample>
    {
        private int j = 0;
        private int idx = 0;

        public SampleIteratorJ(int j)
        {
            this.j = j;
        }

        public boolean hasNext()
        {
            return this.idx < Sampling.this.numI() * Sampling.this.numK();
        }

        public Sample next()
        {
            int i = this.idx % Sampling.this.numI();
            int k = (this.idx - i) / Sampling.this.numI();
            Sample out = new Sample(i, this.j, k);
            this.idx += 1;

            return out;
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Operation not supported for sample iterators");
        }
    }

    private class SampleIteratorK implements Iterator<Sample>
    {
        private int k = 0;
        private int idx = 0;

        public SampleIteratorK(int k)
        {
            this.k = k;
        }

        public boolean hasNext()
        {
            return this.idx < Sampling.this.numI() * Sampling.this.numJ();
        }

        public Sample next()
        {
            int i = this.idx % Sampling.this.numI();
            int j = (this.idx - i) / Sampling.this.numI();
            Sample out = new Sample(i, j, this.k);
            this.idx += 1;

            return out;
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Operation not supported for sample iterators");
        }
    }

    private class SampleIteratorNeighborhood implements Iterator<Sample>
    {
        private Sample center;
        private Integers window;
        private Integers offset;
        private int idx = 0;

        public SampleIteratorNeighborhood(Sample center, Integers radius)
        {
            this.center = center;
            this.offset = radius;

            int[] widx = new int[3];
            widx[0] = 2 * radius.getI() + 1;
            widx[1] = 2 * radius.getJ() + 1;
            widx[2] = 2 * radius.getK() + 1;

            this.window = new Integers(widx);
        }

        public boolean hasNext()
        {
            return this.idx < this.window.getI() * this.window.getJ() * this.window.getK();
        }

        public Sample next()
        {
            int di = this.idx % this.window.getI();
            int tmp = (this.idx - di) / this.window.getI();
            int dj = tmp % this.window.getJ();
            int dk = (tmp - dj) / this.window.getJ();

            int i = this.center.getI() + di - this.offset.getI();
            int j = this.center.getJ() + dj - this.offset.getJ();
            int k = this.center.getK() + dk - this.offset.getK();

            Sample out = new Sample(i, j, k);
            this.idx += 1;

            return out;
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Operation not supported for sample iterators");
        }
    }
}