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
import org.apache.commons.lang3.StringUtils;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.structs.Integers;
import qit.data.formats.vects.TxtVectsCoder;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.VectUtils;
import qit.data.utils.vects.stats.VectStats;
import qit.math.utils.MathUtils;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * an n-dimensional vector
 */
public class Vect implements Dataset
{
    public final static String INDEX = "index";

    private double[] vect;

    @SuppressWarnings("unused")
    private Vect()
    {
    }

    public Vect(String s)
    {

    }

    public Vect(int dim)
    {
        this.vect = new double[dim];
    }

    public Vect(double[] data)
    {
        this(data.length);

        for (int i = 0; i < data.length; i++)
        {
            this.set(i, data[i]);
        }
    }

    public Vects vects()
    {
        return VectsSource.create1D(this);
    }

    public double first()
    {
        return this.vect[0];
    }

    public double last()
    {
        return this.vect[this.vect.length - 1];
    }

    public int size()
    {
        return this.vect.length;
    }

    public Vect sub(int start, int end)
    {
        int dim = end - start;
        Vect out = new Vect(dim);
        for (int i = 0; i < dim; i++)
        {
            out.set(i, this.get(i + start));
        }

        return out;
    }

    public Vect getv(int idx)
    {
        return VectSource.create1D(this.get(idx));
    }

    public Vect subex(int which)
    {
        int dim = this.size() - 1;

        Vect out = new Vect(dim);
        for (int i = 0; i < this.size(); i++)
        {
            if (i < which)
            {
                out.set(i, this.get(i));
            }
            else if (i > which)
            {
                out.set(i - 1, this.get(i - 1));
            }
        }

        return out;
    }

    public Vect sub(List<Integer> idx)
    {
        int dim = idx.size();
        Vect out = new Vect(dim);
        for (int i = 0; i < dim; i++)
        {
            out.set(i, this.get(idx.get(i)));
        }

        return out;
    }

    public Vect sub(Integers idx)
    {
        int dim = idx.size();
        Vect out = new Vect(dim);
        for (int i = 0; i < dim; i++)
        {
            out.set(i, this.get(idx.get(i)));
        }

        return out;
    }

    public double get(int idx)
    {
        return this.vect[idx];
    }

    public Vect hom()
    {
        int num = this.size();
        Vect out = new Vect(num + 1);
        for (int i = 0; i < num; i++)
        {
            out.set(i, this.get(i));
        }
        out.set(num, 1.0);

        return out;
    }

    public Vect dehom()
    {
        int num = this.size();
        Vect out = new Vect(num - 1);

        double hom = this.get(num - 1);

        for (int i = 0; i < num - 1; i++)
        {
            out.set(i, hom == 0 ? Double.POSITIVE_INFINITY : this.get(i) / hom);
        }

        return out;
    }

    public Vect set(int idx, double value)
    {
        this.vect[idx] = value;

        return this;
    }

    public Vect setFirst(double value)
    {
        this.vect[0] = value;

        return this;
    }

    public Vect setLast(double value)
    {
        this.vect[this.vect.length - 1] = value;

        return this;
    }

    public Vect setAll(int start, int end, double value)
    {
        for (int i = start; i < end; i++)
        {
            this.vect[i] = value;
        }

        return this;
    }

    public Vect setAll(double value)
    {
        for (int i = 0; i < this.vect.length; i++)
        {
            this.vect[i] = value;
        }

        return this;
    }

    public Vect proto()
    {
        return new Vect(this.vect.length);
    }

    public Vect proto(double value)
    {
        return new Vect(this.vect.length).setAll(value);
    }

    public Vect copy(int dim)
    {
        Vect out = new Vect(dim);
        for (int i = 0; i < Math.min(dim, out.size()); i++)
        {
            out.set(i, this.get(i));
        }

        return out;
    }

    public Vect copy()
    {
        return new Vect(MathUtils.copy(this.vect));
    }

    public void get(double[] out)
    {
        for (int i = 0; i < this.size(); i++)
        {
            out[i] = this.get(i);
        }
    }

    public void set(double[] in)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, in[i]);
        }
    }

    public void inc(int idx, double val)
    {
        this.vect[idx] += val;
    }

    public void dec(int idx, double val)
    {
        this.vect[idx] -= val;
    }

    public void get(float[] out)
    {
        for (int i = 0; i < this.size(); i++)
        {
            out[i] = (float) this.get(i);
        }
    }

    public void set(float[] in)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, in[i]);
        }
    }

    public void get(Vect v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            v.set(i, this.get(i));
        }
    }

    public void set(Vect v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, v.get(i));
        }
    }

    public void set(int offset, Vect v)
    {
        for (int i = 0; i < v.size(); i++)
        {
            this.set(offset + i, v.get(i));
        }
    }

    public void minusEquals(double s)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, this.get(i) - s);
        }
    }

    public Vect minus(double s)
    {
        Vect out = this.copy();
        out.minusEquals(s);
        return out;
    }

    public void minusEquals(double s, Vect v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, this.get(i) - s * v.get(i));
        }
    }

    public Vect minus(double s, Vect v)
    {
        Vect out = this.copy();
        out.minusEquals(s, v);
        return out;
    }

    public void minusEquals(Vect v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, this.get(i) - v.get(i));
        }
    }

    public Vect minus(Vect v)
    {
        Vect out = this.copy();
        out.minusEquals(v);
        return out;
    }

    public void plusEquals(double s)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, this.get(i) + s);
        }
    }

    public Vect plus(double s)
    {
        Vect out = this.copy();
        out.plusEquals(s);
        return out;
    }

    public void plusEquals(double s, Vect v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, this.get(i) + s * v.get(i));
        }
    }

    public Vect plus(double s, Vect v)
    {
        Vect out = this.copy();
        out.plusEquals(s, v);
        return out;
    }

    public void plusEquals(Vect v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, this.get(i) + v.get(i));
        }
    }

    public Vect plus(Vect v)
    {
        Vect out = this.copy();
        out.plusEquals(v);
        return out;
    }

    public void plusEquals(int i, double v)
    {
        this.set(i, this.get(i) + v);
    }

    public Vect plus(int i, double v)
    {
        Vect out = this.copy();
        out.plusEquals(i, v);
        return out;
    }

    public void timesEquals(int i, double v)
    {
        this.set(i, this.get(i) * v);
    }

    public Vect times(int i, double v)
    {
        Vect out = this.copy();
        out.timesEquals(i, v);
        return out;
    }

    public void timesEquals(double v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, v * this.get(i));
        }
    }

    public Vect times(double v)
    {
        Vect out = this.copy();
        out.timesEquals(v);
        return out;
    }

    public void timesEquals(Vect v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, this.get(i) * v.get(i));
        }
    }

    public void minEquals(Vect v)
    {
        for (int i = 0; i < v.size(); i++)
        {
            this.set(i, Math.min(this.get(i), v.get(i)));
        }
    }

    public void maxEquals(Vect v)
    {
        for (int i = 0; i < v.size(); i++)
        {
            this.set(i, Math.max(this.get(i), v.get(i)));
        }
    }

    public Vect min(Vect v)
    {
        Vect out = this.copy();
        for (int i = 0; i < v.size(); i++)
        {
            out.set(i, Math.min(this.get(i), v.get(i)));
        }
        return out;
    }

    public Vect max(Vect v)
    {
        Vect out = this.copy();
        for (int i = 0; i < v.size(); i++)
        {
            out.set(i, Math.max(this.get(i), v.get(i)));
        }
        return out;
    }

    public Vect times(Vect v)
    {
        Vect out = this.copy();
        out.timesEquals(v);
        return out;
    }

    public void divEquals(int i, double v)
    {
        this.set(i, this.get(i) / v);
    }

    public Vect div(int i, double v)
    {
        Vect out = this.copy();
        out.divEquals(i, v);
        return out;
    }

    public void divEquals(double v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, this.get(i) / v);
        }
    }

    public void divSafeEquals(double v)
    {
        if (MathUtils.nonzero(v))
        {
            for (int i = 0; i < this.size(); i++)
            {
                this.set(i, this.get(i) / v);
            }
        }
    }

    public Vect div(double v)
    {
        Vect out = this.copy();
        out.divEquals(v);
        return out;
    }

    public Vect divSafe(double v)
    {
        Vect out = this.copy();
        out.divSafeEquals(v);

        return out;
    }

    public void divSafeEquals(Vect v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            double vv = v.get(i);
            if (MathUtils.nonzero(vv))
            {
                this.set(i, this.get(i) / vv);
            }
        }
    }

    public void divEquals(Vect v)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.set(i, this.get(i) / v.get(i));
        }
    }

    public Vect div(Vect v)
    {
        Vect out = this.copy();
        out.divEquals(v);
        return out;
    }

    public Vect divSafe(Vect v)
    {
        Vect out = this.copy();
        out.divSafeEquals(v);
        return out;
    }

    public Vect recip()
    {
        Vect out = this.proto();
        for (int i = 0; i < this.size(); i++)
        {
            out.set(i, 1.0 / this.get(i));
        }

        return out;
    }

    public Vect cross(Vect v)
    {
        double x = this.getY() * v.getZ() - this.getZ() * v.getY();
        double y = this.getZ() * v.getX() - this.getX() * v.getZ();
        double z = this.getX() * v.getY() - this.getY() * v.getX();

        Vect out = this.proto();
        out.set(0, x);
        out.set(1, y);
        out.set(2, z);

        return out;
    }

    public Vect normalize()
    {
        return this.divSafe(this.norm());
    }

    public Vect normalizeProb()
    {
        double sum = this.sum();
        if (MathUtils.zero(sum))
        {
            return this.proto(1.0).divSafe(this.size());
        }
        else
        {
            return this.divSafe(sum);
        }
    }

    public void normalizeProbEquals()
    {
        double sum = this.sum();
        if (MathUtils.zero(sum))
        {
            this.setAll(1.0).divSafeEquals(this.size());
        }
        else
        {
            this.divSafeEquals(sum);
        }
    }

    public Vect normalizeSum()
    {
        return this.divSafe(this.sum());
    }

    public Vect normalizeSumEquals()
    {
        this.set(this.divSafe(this.sum()));
        return this;
    }

    public Vect normalizeEquals()
    {
        this.set(this.normalize());
        return this;
    }

    public Vect abs()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.abs(this.get(i)));
        }

        return out;
    }

    public Vect negexp()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.exp(-1.0 * this.get(i)));
        }

        return out;
    }

    public Vect exp()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.exp(this.get(i)));
        }

        return out;
    }

    public Vect log()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.log(this.get(i)));
        }

        return out;
    }

    public Vect logSafe()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            double v = this.get(i);
            out.set(i, v <= 0 ? Double.NEGATIVE_INFINITY : Math.log(v));
        }

        return out;
    }

    public Vect log10()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.log10(this.get(i)));
        }

        return out;
    }

    public Vect round()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.round(this.get(i)));
        }

        return out;
    }

    public Vect floor()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.floor(this.get(i)));
        }

        return out;
    }

    public Vect ceil()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.ceil(this.get(i)));
        }

        return out;
    }

    public Vect sq()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            double v = this.get(i);
            out.set(i, v * v);
        }

        return out;
    }

    public Vect sqrt()
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.sqrt(this.get(i)));
        }

        return out;
    }

    public Vect pow(double v)
    {
        Vect out = this.proto();
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, Math.pow(this.get(i), v));
        }

        return out;
    }

    public Vect cat(Vect v)
    {
        Vect out = new Vect(this.size() + v.size());

        for (int i = 0; i < this.size(); i++)
        {
            out.set(i, this.get(i));
        }

        for (int i = 0; i < v.size(); i++)
        {
            out.set(this.size() + i, v.get(i));
        }

        return out;
    }

    public Integers less(double t)
    {
        List<Integer> which = Lists.newArrayList();
        for (int i = 0; i < this.size(); i++)
        {
            if (this.get(i) < t)
            {
                which.add(i);
            }
        }
        return new Integers(which);
    }

    public Integers greater(double t)
    {
        List<Integer> which = Lists.newArrayList();
        for (int i = 0; i < this.size(); i++)
        {
            if (this.get(i) > t)
            {
                which.add(i);
            }
        }
        return new Integers(which);
    }

    public Vect thresh(double t)
    {
        Vect out = this.proto();
        for (int i = 0; i < this.size(); i++)
        {
            if (this.get(i) > t)
            {
                out.set(i, 1);
            }
            else
            {
                out.set(i, 0);
            }
        }
        return out;
    }

    public double prod()
    {
        double out = this.get(0);

        for (int i = 1; i < this.size(); i++)
        {
            out *= this.get(i);
        }

        return out;
    }

    public Vect cumprod()
    {
        Vect out = new Vect(this.size());
        out.set(0, this.get(0));

        for (int i = 1; i < this.size(); i++)
        {
            double curr = this.get(i);
            double prev = out.get(i - 1);
            out.set(i, curr * prev);
        }

        return out;
    }

    public Vect cumsum()
    {
        Vect out = new Vect(this.size());
        out.set(0, this.get(0));

        for (int i = 1; i < this.size(); i++)
        {
            double curr = this.get(i);
            double prev = out.get(i - 1);
            out.set(i, curr + prev);
        }

        return out;
    }

    public double sum()
    {
        double out = 0;
        for (int i = 0; i < this.size(); i++)
        {
            double v = this.get(i);
            out += v;
        }
        return out;
    }

    public double mean()
    {
        return this.sum() / this.size();
    }

    public double median()
    {
        List<Double> values = Lists.newArrayList();
        for (double val : this.vect)
        {
            values.add(val);
        }
        Collections.sort(values);
        double out = values.get(values.size() / 2);
        return out;
    }

    public Double min()
    {
        Double min = null;
        for (int i = 0; i < this.size(); i++)
        {
            double v = this.get(i);
            if (!Double.isNaN(v))
            {
                min = min == null ? v : Math.min(min, v);
            }
        }
        return min;
    }

    public int maxidx()
    {
        return MathUtils.maxidx(this.vect);
    }

    public int minidx()
    {
        return MathUtils.minidx(this.vect);
    }

    public Double max()
    {
        Double max = null;
        for (int i = 0; i < this.size(); i++)
        {
            double v = this.get(i);
            if (!Double.isNaN(v))
            {
                max = max == null ? v : Math.max(max, v);
            }
        }
        return max;
    }

    public Double var()
    {
        double mean = this.mean();

        double vsum = 0;
        for (int i = 0; i < this.size(); i++)
        {
            double v = this.get(i) - mean;

            vsum += v * v;
        }

        double var = vsum / (double) (this.size() - 1);

        return var;
    }

    public Double std()
    {
        return Math.sqrt(var());
    }

    public double maxabs()
    {
        double max = this.get(0);
        for (int i = 0; i < this.size(); i++)
        {
            max = Math.max(max, Math.abs(this.get(i)));
        }
        return max;
    }

    public double dot(Vect v)
    {
        double out = 0;
        for (int i = 0; i < v.size(); i++)
        {
            out += this.get(i) * v.get(i);
        }
        return out;
    }

    public List<Integer> zero()
    {
        List<Integer> out = Lists.newArrayList();

        for (int i = 0; i < this.size(); i++)
        {
            if (MathUtils.zero(this.vect[i]))
            {
                out.add(i);
            }
        }

        return out;
    }

    public List<Integer> above(double value)
    {
        List<Integer> out = Lists.newArrayList();

        for (int i = 0; i < this.size(); i++)
        {
            if (this.vect[i] > value)
            {
                out.add(i);
            }
        }

        return out;
    }

    public List<Integer> below(double value)
    {
        List<Integer> out = Lists.newArrayList();

        for (int i = 0; i < this.size(); i++)
        {
            if (this.vect[i] < value)
            {
                out.add(i);
            }
        }

        return out;
    }

    public List<Integer> negative()
    {
        return below(0);
    }

    public List<Integer> positive()
    {
        return above(0);
    }

    public double norm()
    {
        if (this.vect.length == 1)
        {
            return Math.abs(this.vect[0]);
        }
        else
        {
            return Math.sqrt(this.dot(this));
        }
    }

    public double norm2()
    {
        return this.dot(this);
    }

    public double norm1()
    {
        return this.abs().sum();
    }

    public double dist(Vect v)
    {
        return this.minus(v).norm();
    }

    public double dist2(Vect v)
    {
        return this.minus(v).norm2();
    }

    public double angleLineDeg(Vect v)
    {
        double dot = Math.abs(this.normalize().dot(v.normalize()));
        if (dot > 1)
        {
            dot = 1;
        }
        double angle = 180 * Math.acos(dot) / Math.PI;
        return angle;
    }

    public double angleLineRad(Vect v)
    {
        double dot = Math.abs(this.dot(v));
        if (dot > 1)
        {
            dot = 1;
        }

        return Math.acos(dot);
    }

    public double angleRad(Vect v)
    {
        double dot = this.dot(v);
        Vect cross = this.cross(v);

        return Math.atan2(cross.norm(), dot);
    }

    public Vect removeNaN()
    {
        List<Double> out = Lists.newArrayList();
        for (int i = 0; i < this.size(); i++)
        {
            double value = this.get(i);
            if (Double.isFinite(value))
            {
                out.add(value);
            }
        }

        return VectSource.create(out);
    }

    public Vect perp()
    {
        // return any vector that is orthogonal
        Vect p = this.cross(VectSource.create(-1, 0, 0));
        if (MathUtils.zero(p.norm()))
        {
            p = this.cross(VectSource.create(0, -1, 0));
        }
        return p.normalize();
    }

    public double cot(Vect v)
    {
        double ab = this.dot(v);
        double naxb = this.cross(v).norm();
        return ab / naxb;
    }

    public double angleDeg(Vect v)
    {
        return 180 * this.angleRad(v) / Math.PI;
    }

    public double angleCot(Vect v)
    {
        double dot = this.dot(v);
        double ma = this.norm();
        double mb = v.norm();
        double d = ma * ma * mb * mb - dot * dot;
        Global.assume(d >= 0, "Cotangent of angle between vectors is not well defined");
        return Math.abs(Math.atan2(Math.sqrt(d), dot));
    }

    public boolean infinite()
    {
        for (int i = 0; i < this.size(); i++)
        {
            if (Double.isInfinite(this.get(i)))
            {
                return true;
            }
        }
        return false;
    }

    public boolean nan()
    {
        for (int i = 0; i < this.size(); i++)
        {
            if (Double.isNaN(this.get(i)))
            {
                return true;
            }
        }
        return false;
    }

    public boolean finite()
    {
        for (int i = 0; i < this.size(); i++)
        {
            if (!Double.isFinite(this.get(i)))
            {
                return false;
            }
        }
        return true;
    }

    public Vect clean()
    {
        Vect out = this.proto();

        for (int i = 0; i < this.size(); i++)
        {
            double v = this.get(i);
            out.set(i, Double.isFinite(v) ? v : 0);
        }

        return out;
    }

    public double getX()
    {
        return this.get(0);
    }

    public double getY()
    {
        return this.get(1);
    }

    public double getZ()
    {
        return this.get(2);
    }

    public double getW()
    {
        return this.get(3);
    }

    public double[] toArray()
    {
        double[] out = new double[this.vect.length];
        System.arraycopy(this.vect, 0, out, 0, this.vect.length);
        return out;
    }

    public float[] toFloatArray()
    {
        float[] out = new float[this.vect.length];
        for (int i = 0; i < this.vect.length; i++)
        {
            out[i] = (float) this.get(i);
        }
        return out;
    }

    public void toArray(double[] array)
    {
        Global.assume(array.length == this.vect.length, "invalid array");

        System.arraycopy(this.vect, 0, array, 0, this.vect.length);
    }

    public Object clone()
    {
        return this.copy();
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Vect))
        {
            return false;
        }
        else if (obj == this)
        {
            return true;
        }
        else
        {
            Vect a = this;
            Vect b = (Vect) obj;

            Global.assume(a.size() == b.size(), "the vectors have different dimensions");

            for (int i = 0; i < a.size(); i++)
            {
                if (!MathUtils.eq(a.get(i), b.get(i)))
                {
                    return false;
                }
            }
            return true;
        }
    }

    public Vect autoweights()
    {
        double beta = 2.0 * new VectStats().withInput(this).run().std;

        if (MathUtils.zero(beta))
        {
            beta = 1.0;
        }

        int num = this.size();
        Vect out = new Vect(num);

        for (int i = 0; i < num; i++)
        {
            double v = this.get(i);
            double w = Math.exp(-v / beta);
            out.set(i, w);
        }

        return out;
    }

    public Vect repcoord(int count)
    {
        int num = this.size();
        Vect out = new Vect(num * count);

        for (int i = 0; i < num; i++)
        {
            double v = this.get(i);

            for (int j = 0; j < count; j++)
            {
                out.set(count * i + j, v);
            }
        }

        return out;
    }

    public Vect rep(int count)
    {
        int num = this.size();
        Vect out = new Vect(num * count);

        for (int i = 0; i < num; i++)
        {
            double v = this.get(i);

            for (int j = 0; j < count; j++)
            {
                out.set(num * j + i, v);
            }
        }

        return out;
    }

    public Integers sort()
    {
        return new Integers(MathUtils.permutation(this.vect));
    }

    public Vect perm(Integers which)
    {
        Vect out = new Vect(which.size());
        for (int i = 0; i < which.size(); i++)
        {
            out.set(i, this.get(which.get(i)));
        }

        return out;
    }

    public int hashCode()
    {
        int h = 0;
        for (double element : this.vect)
        {
            h += (int) element;
        }

        return h;
    }

    public String encodeJson()
    {
        StringBuffer out = new StringBuffer();
        out.append("[" + this.vect[0]);
        for (int i = 1; i < this.vect.length; i++)
        {
            out.append("," + this.vect[i]);
        }
        out.append("]");

        return out.toString();
    }

    public String toString()
    {
        return this.encodeJson();
    }

    public static Vect read(String fn) throws IOException
    {
        return Vects.read(fn).flatten();
    }

    public void write(String fn) throws IOException
    {
        if (fn.endsWith("json"))
        {
            PrintWriter pw = new PrintWriter(fn);
            pw.println(this.encodeJson());
            pw.close();
        }
        else if (fn.endsWith("csv"))
        {
            PrintWriter pw = new PrintWriter(fn);
            String[] tokens = new String[this.size()];
            for (int i = 0; i < this.size(); i++)
            {
                tokens[i] = String.valueOf(this.get(i));
            }
            pw.println(StringUtils.join(tokens, ","));
            pw.close();
        }
        else
        {
            OutputStream os = fn.equals("-") ? System.out : new FileOutputStream(fn);

            if (fn.endsWith(".gz"))
            {
                os = new GZIPOutputStream(os);
            }

            PrintWriter pw = new PrintWriter(os, true);
            for (int i = 0; i < this.size(); i++)
            {
                pw.println(this.get(i));
            }
            pw.close();
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