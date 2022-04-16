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

import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectsStats;

import java.util.List;

public class VolumeStats
{
    public Volume min;
    public Volume max;
    public Volume mean;
    public Volume var;
    public Volume sum;
    public Volume std;
    public Volume cv;
    public Volume num;
    public Volume median;
    public Volume qlow;
    public Volume qhigh;
    public Boolean output;

    public boolean norm = false;
    public List<Volume> input;
    public Mask mask;
    public Vect weights;
    public boolean robust = false;

    public VolumeStats withInput(List<Volume> v)
    {
        this.input = v;
        this.output = null;

        return this;
    }

    public VolumeStats withMask(Mask v)
    {
        this.mask = v;
        this.output = null;

        return this;
    }

    public VolumeStats withWeights(Vect v)
    {
        this.weights = v;
        this.output = null;

        return this;
    }

    public VolumeStats withNorm(boolean v)
    {
        this.norm = v;
        this.output = null;

        return this;
    }

    public VolumeStats withRobust(boolean v)
    {
        this.robust = v;
        this.output = null;

        return this;
    }

    public VolumeStats run()
    {
        int num = this.input.size();

        Volume proto = this.input.get(0).proto();
        this.qlow = proto;
        this.median = proto.proto();
        this.qhigh = proto.proto();
        this.num = proto.proto();
        this.min = proto.proto();
        this.max = proto.proto();
        this.sum = proto.proto();
        this.mean = proto.proto();
        this.var = proto.proto();
        this.std = proto.proto();
        this.cv = proto.proto();

        for (Sample sample : proto.getSampling())
        {
            if (proto.valid(sample, this.mask))
            {
                Vects slice = new Vects();
                for (int j = 0; j < num; j++)
                {
                    Vect value = this.input.get(j).get(sample);
                    slice.add(this.norm ? VectSource.create1D(value.norm()) : value);
                }

                VectsStats vs = new VectsStats().withInput(slice).withWeights(this.weights).withRobust(this.robust).run();

                this.qlow.set(sample, vs.qlow);
                this.median.set(sample, vs.median);
                this.qhigh.set(sample, vs.qhigh);
                this.num.set(sample, vs.num);
                this.min.set(sample, vs.min);
                this.max.set(sample, vs.max);
                this.sum.set(sample, vs.sum);
                this.mean.set(sample, vs.mean);
                this.var.set(sample, vs.var);
                this.std.set(sample, vs.std);
                this.cv.set(sample, vs.cv);
            }
        }

        this.output = true;

        return this;
    }

    public boolean getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
