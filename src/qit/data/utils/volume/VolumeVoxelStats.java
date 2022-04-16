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

package qit.data.utils.volume;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectStats;
import qit.math.utils.MathUtils;

import java.util.List;

public class VolumeVoxelStats
{
    public Double min;
    public Double max;
    public Double mean;
    public Double var;
    public Double sum;
    public Double std;
    public Double stde;
    public Double cv;
    public Double num;
    public Double median;
    public Double mad;
    public Double qlow;
    public Double qhigh;
    public Double iqr;
    public boolean output = false;

    public Volume input;
    public Mask mask;
    public Integer channel = 0;
    public Integer label = null;
    public Volume weights;

    public VolumeVoxelStats withInput(Volume v)
    {
        this.input = v;
        this.output = false;

        return this;
    }

    public VolumeVoxelStats withMask(Mask v)
    {
        this.mask = v;
        this.output = false;

        return this;
    }

    public VolumeVoxelStats withChannel(Integer v)
    {
        this.channel = v;
        this.output = false;

        return this;
    }

    public VolumeVoxelStats withLabel(Integer v)
    {
        this.label = v;
        this.output = false;

        return this;
    }

    public VolumeVoxelStats withWeights(Volume v)
    {
        this.weights = v;
        this.output = false;

        return this;
    }

    public VolumeVoxelStats run()
    {
        Global.assume(this.channel >= 0 && this.channel < this.input.getDim(), "invalid channel");

        List<Double> weights = Lists.newArrayList();
        List<Double> values = Lists.newArrayList();
        double sumw = 0;

        for (Sample sample : this.input.getSampling())
        {
            if (!this.input.valid(sample, this.mask))
            {
               continue;
            }

            if (this.mask != null && this.label != null && this.mask.get(sample) != this.label)
            {
                continue;
            }

            double w = this.weights != null ? this.weights.get(sample, 0) : 1.0;

            values.add(this.input.get(sample, channel));
            weights.add(w);
            sumw += w;
        }

        int num = values.size();

        if (num == 0)
        {
            this.output = false;
            return this;
        }

        Vect vv = VectSource.create(values);
        Vect vw = VectSource.create(weights);

        if (MathUtils.nonzero(sumw))
        {
            vw.times(1.0 / sumw);
        }

        VectStats vs = new VectStats().withInput(vv).withWeights(vw).run();

        this.qlow = vs.qlow;
        this.median = vs.median;
        this.mad = vs.mad;
        this.qhigh = vs.qhigh;
        this.iqr = vs.iqr;
        this.num = vs.num;
        this.min = vs.min;
        this.max = vs.max;
        this.sum = vs.sum;
        this.mean = vs.mean;
        this.var = vs.var;
        this.std = vs.std;
        this.stde = vs.stde;
        this.cv = vs.cv;

        this.output = true;

        return this;
    }

    public boolean getOutput()
    {
        if (this.output == false)
        {
            this.run();
        }

        return this.output;
    }

    public Record record(String basename)
    {
        Record out = new Record();
        if (this.num != null && this.num > 0)
        {
            out.with(basename + "_num", this.num);
            out.with(basename + "_mean", this.mean);
            out.with(basename + "_median", this.median);
            out.with(basename + "_mad", this.mad);
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
            out.with(basename + "_mad", "NA");
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
}
