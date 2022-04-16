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
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

import java.io.IOException;

public class Histogram extends JsonDataset
{
    public static Histogram create(int bins, double min, double max)
    {
        int num = bins + 1;
        Vect breaks = VectSource.createND(num);
        double delta = (max - min) / (num - 1.0);
        Global.assume(delta > 0, "invalid breaks");

        for (int i = 0; i < num; i++)
        {
            breaks.set(i, min + delta * i);
        }

        return new Histogram(breaks);
    }

    public static Histogram create(Vect values)
    {
        return create(values, 256, 0.05);
    }

    public static Histogram create(Vect values, int bins, double clip)
    {
        Vect sorted = values.perm(values.sort());

        int minidx = MathUtils.round((sorted.size() - 1) * clip);
        int maxidx = MathUtils.round((sorted.size() - 1) * (1.0 - clip));

        Histogram histogram = Histogram.create(bins, sorted.get(minidx), sorted.get(maxidx));

        for (int idx = minidx; idx <= maxidx; idx++)
        {
            histogram.update(sorted.get(idx));
        }

        return histogram;
    }

    private Vect breaks; // this has N entries
    private Vect data; // this has N-1 entries

    private Histogram()
    {
    }

    public Histogram(Vect breaks)
    {
        for (int i = 0; i < breaks.size() - 1; i++)
        {
            Global.assume(breaks.get(i) < breaks.get(i + 1), "invalid histogram breaks");
        }

        this.breaks = breaks.copy();
        this.data = VectSource.createND(breaks.size() - 1);
    }

    public double entropy()
    {
        double out = 0;
        double sum = this.sum();

        for (int i = 0; i < this.size(); i++)
        {
            double prob = this.get(i) / sum;

            out += -1.0 * prob * Math.log(prob + 1e-12);
        }

        return out;
    }

    public void cdf()
    {
        Vect cdf = VectSource.createND(this.size());
        double sum = this.sum();

        for (int i = 0; i < this.size(); i++)
        {
            double p = this.getData(i) / sum;
            cdf.set(i, p);

            if (i > 0)
            {
                cdf.inc(i, cdf.get(i - 1));
            }
        }

        this.data.set(cdf);
    }

    public boolean contains(double value)
    {
        return value >= this.breaks.first() && value <= this.breaks.last();
    }

    public void update(double value)
    {
        if (this.contains(value))
        {
            for (int i = 1; i < this.breaks.size(); i++)
            {
                if (value <= this.breaks.get(i))
                {
                    this.data.inc(i - 1, 1);
                    return;
                }
            }
        }
    }

    public double getBoundBelow(int i)
    {
        return this.breaks.get(i);
    }

    public double getBoundAbove(int i)
    {
        return this.breaks.get(i + 1);
    }

    public double chi2(Histogram h)
    {
        Global.assume(this.compatible(h), "incompatible histograms");

        double out = 0;
        for (int i = 0; i < this.data.size(); i++)
        {
            double diff = this.data.get(i) - h.data.get(i);
            double sum = this.data.get(i) + h.data.get(i);

            out += diff * diff / sum;
        }

        return out;
    }

    public boolean compatible(Histogram h)
    {
        if (h.data.size() != this.data.size())
        {
            return false;
        }

        for (int i = 0; i < this.data.size(); i++)
        {
            if (!MathUtils.eq(this.breaks.get(i), h.breaks.get(i)))
            {
                return false;
            }
        }

        return true;
    }

    public double get(int i)
    {
        return this.data.get(i);
    }

    public double getData(int i)
    {
        return this.data.get(i);
    }

    public int size()
    {
        return this.data.size();
    }

    public int getBins()
    {
        return this.data.size();
    }

    public Vect vect()
    {
        return this.data.copy();
    }

    public double sum()
    {
        return this.data.sum();
    }

    public int maxIndex()
    {
        return this.data.maxidx();
    }

    public double maxValue()
    {
        return this.data.max();
    }

    public double mode()
    {
        int idx = this.maxIndex();
        if (idx == 0)
        {
            return this.breaks.first();
        }
        else if (idx == this.size() - 1)
        {
            return this.breaks.last();
        }
        else
        {
            return 0.5 * (this.getBoundAbove(idx) + this.getBoundBelow(idx));
        }

    }

    public double otsu()
    {
        double sumB = 0;
        double wB = 0;
        double maximum = 0.0;
        int maxidx = 0;
        double sum1 = 0;
        int total = 0;

        for (int i = 0; i < this.data.size(); i++)
        {
            sum1 += i * this.data.get(i);
            total += this.data.get(i);
        }

        for (int i = 0; i < this.data.size(); i++)
        {
            wB += this.data.get(i);
            double wF = total - wB;

            if (wB == 0 || wF == 0)
            {
                continue;
            }
            else
            {
                sumB += i * this.data.get(i);
                double mF = (sum1 - sumB) / wF;
                double between = wB * wF * ((sumB / wB) - mF) * ((sumB / wB) - mF);

                if (between >= maximum)
                {
                    maxidx = i;
                    maximum = between;
                }
            }
        }

        double otsu = this.breaks.get(maxidx + 1);

        return otsu;
    }

    public void normalize()
    {
        this.data.divSafeEquals(this.data.sum());
    }

    public Vect density()
    {
        return this.data.divSafe(this.data.sum());
    }

    public void smoothBins(double h)
    {
        double factor = -1.0 / (2.0 * h * h);
        Vect smoothed = this.data.proto();

        for (int i = 0; i < this.data.size(); i++)
        {
            double vali = this.data.get(i);
            for (int j = 0; j < this.data.size(); j++)
            {
                double weight = Math.exp(factor * (i - j) * (i - j));
                smoothed.inc(j, weight * vali);
            }
        }

        this.data.set(smoothed.times(this.data.sum() / smoothed.sum()));
    }

    public void smoothData(double h)
    {
        double factor = -1.0 / (2.0 * h * h);
        Vect smoothed = this.data.proto();

        for (int i = 0; i < this.data.size(); i++)
        {
            double vali = this.data.get(i);

            smoothed.inc(i, 1);

            double distlow = 0;
            for (int j = i - 1; j >= 0; j--)
            {
                distlow += Math.abs(this.getBoundAbove(i) - this.getBoundBelow(i));

                double weight = Math.exp(factor * distlow * distlow);
                smoothed.inc(j, weight * vali);
            }

            double disthigh = 0;
            for (int j = i + 1; j < this.data.size(); j++)
            {
                disthigh += Math.abs(this.getBoundAbove(i) - this.getBoundBelow(i));

                double weight = Math.exp(factor * disthigh * disthigh);
                smoothed.inc(j, weight * vali);
            }
        }

        this.data.set(smoothed.times(this.data.sum() / smoothed.sum()));
    }

    public static Histogram read(String fn) throws IOException
    {
        return JsonDataset.read(Histogram.class, fn);
    }

    public Histogram copy()
    {
        Histogram h = new Histogram();
        h.breaks = this.breaks.copy();
        h.data = this.data.copy();

        return h;
    }

    public Table table()
    {
        Table out = new Table();
        out.addField("index");
        out.addField("below");
        out.addField("middle");
        out.addField("above");
        out.addField("value");

        for (int i = 0; i < this.size(); i++)
        {
            double below = this.getBoundBelow(i);
            double above = this.getBoundAbove(i);
            double middle = 0.5 * (below + above);

            Record record = new Record();
            record.with("index", String.valueOf(i));
            record.with("below", String.valueOf(below));
            record.with("middle", String.valueOf(middle));
            record.with("above", String.valueOf(above));
            record.with("value", String.valueOf(this.get(i)));
            out.addRecord(record);
        }

        return out;
    }
}
