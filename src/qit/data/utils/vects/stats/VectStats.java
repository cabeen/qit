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

package qit.data.utils.vects.stats;

import com.google.common.collect.Maps;
import qit.base.Global;
import qit.data.datasets.Record;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.VectUtils;
import qit.math.utils.MathUtils;

import java.util.Map;

public class VectStats
{
    public Double min;
    public Double max;
    public Double mean;
    public Double var;
    public Double sum;
    public Double std;
    public Double cv;
    public Double num;
    public Double median;
    public Double mad;
    public Double qlow;
    public Double qhigh;
    public Double iqr;
    public Double stde;
    public Boolean output;

    public Vect input;
    public Vect weights;

    // https://www.real-statistics.com/descriptive-statistics/m-estimators/;
    transient public boolean medianWeighted = false;
    transient public boolean robust = false;
    transient public int robustIters = 1000;
    transient public Double robustTolerance = 1e-6;
    transient public boolean robustHuber = false;
    transient public Double robustScaleTukey = 4.685;
    transient public Double robustScaleHuber = 1.339;

    public VectStats withInput(Vect v)
    {
        this.input = v;
        this.output = null;

        return this;
    }

    public VectStats withWeights(Vect v)
    {
        this.weights = v;
        this.output = null;

        return this;
    }

    public VectStats withRobust(boolean v)
    {
        this.robust = v;
        this.output = null;

        return this;
    }

    public VectStats run()
    {
        this.inner(this.input, this.weights);

        if (this.robust)
        {
            this.medianWeighted = false;
            for (int iter = 0; iter < this.robustIters; iter++)
            {
                Vect myweights = this.weights == null ? this.input.proto().setAll(1.0) : this.weights.copy();

                double m = this.mean;
                double d = this.mad;
                double s = this.robustHuber ? this.robustScaleHuber : this.robustScaleTukey;

                for (int i = 0; i < myweights.size(); i++)
                {
                    double v = this.input.get(i);
                    double u = Math.abs(v - m) / (d * s + Global.DELTA);
                    double w = this.robustHuber ? u : MathUtils.square(1.0 - u * u);
                    w = Math.min(1.0, Math.max(0.0, w));
                    myweights.set(i, w);
                }

                myweights.timesEquals(1.0 / myweights.sum());

                this.inner(this.input, myweights);

                double change = Math.abs(this.mean - m) / m;
                if (change < this.robustTolerance)
                {
                    break;
                }
            }
        }

        return this;
    }

    public VectStats inner(Vect myinput, Vect myweights)
    {
        if (myinput.size() == 1)
        {
            double val = myinput.get(0);
            this.mean = val;
            this.qlow = val;
            this.qhigh = val;
            this.median = val;
            this.mad = 0.0;
            this.min = val;
            this.max = val;
            this.sum = val;
            this.iqr = 0.0;
            this.var = 0.0;
            this.std = 0.0;
            this.stde = 0.0;
            this.cv = 0.0;
            this.iqr = 0.0;
            this.num = 1.0;

            this.output = true;

            return this;
        }
        else
        {
            int num = myinput.size();
            if (num == 0)
            {
                this.output = false;
                return this;
            }

            if (myweights == null)
            {
                myweights = VectSource.createND(num);
                myweights.setAll(1.0 / num);
            }

            double sumw = myweights.sum();

            {
                int[] perm = VectUtils.permutation(myinput);

                double inc = 0;
                double qlow = 0.25 * sumw;
                double qmed = 0.5 * sumw;
                double qhigh = 0.75 * sumw;
                boolean flow = true;
                boolean fmed = true;
                boolean fhigh = true;

                for (int i = 0; i < num; i++)
                {
                    int idx = perm[i];
                    double v = myinput.get(idx);
                    double w = myweights.get(idx);

                    inc += this.medianWeighted ? w : sumw / num;

                    if (flow && inc > qlow)
                    {
                        this.qlow = v;
                        flow = false;
                    }

                    if (fmed && inc > qmed)
                    {
                        this.median = v;
                        fmed = false;
                    }

                    if (fhigh && inc > qhigh)
                    {
                        this.qhigh = v;
                        fhigh = false;
                    }
                }
            }

            if (this.median != null)
            {
                double[] devs = new double[num];
                for (int i = 0; i < num; i++)
                {
                    double value = myinput.get(i);
                    devs[i] = Math.abs(value - this.median);
                }

                int[] madperm = MathUtils.permutation(devs);
                double madinc = 0;
                double qmad = 0.5 * sumw;

                for (int i = 0; i < num; i++)
                {
                    int idx = madperm[i];
                    double v = devs[i];
                    double w = myweights.get(idx);

                    madinc += this.medianWeighted ? w : sumw / num;

                    if (madinc > qmad)
                    {
                        this.mad = v;
                        break;
                    }
                }
            }

            this.num = 0.0;
            this.min = null;
            this.max = null;
            this.sum = 0.0;
            double wsum = 0.0;
            for (int i = 0; i < num; i++)
            {
                double w = myweights.get(i);
                double v = myinput.get(i);

                this.num++;
                this.min = this.min == null ? v : Math.min(this.min, v);
                this.max = this.max == null ? v : Math.max(this.max, v);
                this.sum += v;
                wsum += w * v;
            }

            this.mean = wsum / sumw;
            this.var = 0.0;
            for (int i = 0; i < num; i++)
            {
                double w = myweights.get(i);
                double v = myinput.get(i);

                double dv = this.mean - v;
                this.var += w * dv * dv;
            }

            this.var /= sumw;
            this.std = Math.sqrt(this.var);
            this.stde = this.std / Math.sqrt(num);
            this.cv = this.std / this.mean;
            this.iqr = this.qhigh != null ? this.qhigh - this.qlow : null;

            this.output = true;

            return this;

        }
    }

    public boolean getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }

    public Map<String,Double> lookup()
    {
        Map<String, Double> out = Maps.newHashMap();

        out.put("min", this.min);
        out.put("max", this.max);
        out.put("mean", this.mean);
        out.put("var", this.var);
        out.put("std", this.std);
        out.put("stde", this.stde);
        out.put("median", this.median);
        out.put("qlow", this.qlow);
        out.put("qhigh", this.qhigh);
        out.put("iqr", this.iqr);
        out.put("mad", this.mad);
        out.put("sum", this.sum);
        out.put("num", this.num);

        return out;
    }

    public Record record(String basename)
    {
        Record out = new Record();
        if (this.num == null || this.num > 0)
        {
            out.with(basename + "_num", this.num);
            out.with(basename + "_mean", this.mean);
            out.with(basename + "_median", this.median);
            out.with(basename + "_qlow", this.qlow);
            out.with(basename + "_qhigh", this.qhigh);
            out.with(basename + "_iqr", this.iqr);
            out.with(basename + "_sum", this.sum);
            out.with(basename + "_max", this.max);
            out.with(basename + "_min", this.min);
            out.with(basename + "_var", this.var);
            out.with(basename + "_std", this.std);
            out.with(basename + "_stde", this.stde);
            out.with(basename + "_sum", this.sum);
            out.with(basename + "_cv", this.cv);
        }
        else
        {
            out.with(basename + "_num", "NA");
            out.with(basename + "_mean", "NA");
            out.with(basename + "_median", "NA");
            out.with(basename + "_qlow", "NA");
            out.with(basename + "_qhigh", "NA");
            out.with(basename + "_iqr", "NA");
            out.with(basename + "_sum", "NA");
            out.with(basename + "_max", "NA");
            out.with(basename + "_min", "NA");
            out.with(basename + "_var", "NA");
            out.with(basename + "_std", "NA");
            out.with(basename + "_stde", "NA");
            out.with(basename + "_cv", "NA");
            out.with(basename + "_sum", "NA");
        }

        return out;
    }

    public static VectStats stats(Vect vect)
    {
        return new VectStats().withInput(vect).run();
    }
}
