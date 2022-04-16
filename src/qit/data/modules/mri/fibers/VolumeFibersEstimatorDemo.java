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

package qit.data.modules.mri.fibers;

import com.google.common.collect.Lists;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VolumeSource;
import qit.data.models.Fibers;
import qit.data.utils.mri.estimation.FibersEstimator;

import java.util.List;

@ModuleUnlisted
@ModuleDescription("Demonstrate fiber estimation at a single voxel")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersEstimatorDemo implements Module
{
    @ModuleInput
    @ModuleDescription("an input fibers volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the sample i index")
    public int i = 0;

    @ModuleParameter
    @ModuleDescription("the sample j index")
    public int j = 0;

    @ModuleParameter
    @ModuleDescription("the sample k index")
    public int k = 0;

    @ModuleParameter
    @ModuleDescription("the spatial bandwidth")
    public double hpos = 2.0;

    @ModuleParameter
    @ModuleDescription("the data-adaptive bandwidth")
    public double hdir = 0.5;

    @ModuleParameter
    @ModuleDescription("the minima fraction")
    public double minv = 0.05;

    @ModuleParameter
    @ModuleDescription("the support")
    public int support = 5;

    @ModuleParameter
    @ModuleDescription("a data adaptive threshold")
    public double lambda = 0.9999;

    @ModuleParameter
    @ModuleDescription("a maxima number of fiber compartments")
    public int comps = 3;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the sample position")
    public Vects pos;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the position weights")
    public Volume wpos;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the data-adaptive weights")
    public Volume wdir;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the combined weights")
    public Volume wall;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the aggregated fiber models")
    public Volume agg;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the separated fiber models")
    public Volume sep;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the estimated fiber model")
    public Volume est;

    public VolumeFibersEstimatorDemo run()
    {
        FibersEstimator estimator = new FibersEstimator();
        estimator.estimation = FibersEstimator.EstimationType.Match;
        estimator.selection = FibersEstimator.SelectionType.Adaptive;
        estimator.lambda = this.lambda;
        estimator.maxcomps = this.comps;
        estimator.minfrac = this.minv;

        List<Double> weights = Lists.newArrayList();
        List<Vect> models = Lists.newArrayList();

        Volume wposOut = this.input.proto(1);
        Volume wdirOut = this.input.proto(1);
        Volume wallOut = this.input.proto(1);

        int n = 2 * this.support + 1;
        Volume posWeights = VolumeSource.gauss(this.input.getSampling(), n, n, n, this.hpos);
        Sampling neighborhood = posWeights.getSampling();

        int centerX = (neighborhood.numI() - 1) / 2;
        int centerY = (neighborhood.numJ() - 1) / 2;
        int centerZ = (neighborhood.numK() - 1) / 2;

        Sample sample = new Sample(this.i, this.j, this.k);
        Fibers ref = new Fibers(this.input.get(sample));

        for (Sample offset : neighborhood)
        {
            int ni = sample.getI() + offset.getI() - centerX;
            int nj = sample.getJ() + offset.getJ() - centerY;
            int nk = sample.getK() + offset.getK() - centerZ;
            Sample neighbor = new Sample(ni, nj, nk);

            if (this.input.valid(neighbor, this.mask))
            {
                Fibers model = new Fibers(this.input.get(neighbor));
                double posWeight = posWeights.get(offset, 0);

                double dist2 = model.dist2(ref);
                double h = this.hdir;
                double h2 = h * h;
                double dirWeight = Math.exp(-dist2 / h2);

                double allWeight = posWeight * dirWeight;

                models.add(model.getEncoding());
                weights.add(allWeight);

                wposOut.set(neighbor, 0, posWeight);
                wdirOut.set(neighbor, 0, dirWeight);
                wallOut.set(neighbor, 0, allWeight);
            }
        }

        Fibers est = new Fibers(estimator.run(ref.getEncoding(), weights, models));

        List<Fibers> sepModels = Lists.newArrayList();
        for (Vect v: models)
        {
            Fibers m = new Fibers(v);
            for (int i = 0; i < m.size(); i++)
            {
                if (m.getFrac(i) > this.minv)
                {
                    Fibers sep = new Fibers(1);
                    sep.setFrac(0, m.getFrac(i));
                    sep.setLine(0, m.getLine(i));
                    sepModels.add(sep);
                }
            }
        }

        Volume aggOut = this.input.proto(SamplingSource.create(models.size(), 1, 1));
        for (int idx = 0; idx < aggOut.getSampling().size(); idx++)
        {
            aggOut.set(idx, models.get(idx));
        }

        Volume sepOut = VolumeSource.create(SamplingSource.create(sepModels.size(), 1, 1), new Fibers(1).getEncodingSize());
        for (int idx = 0; idx < sepOut.getSampling().size(); idx++)
        {
            sepOut.set(idx, sepModels.get(idx).getEncoding());
        }

        Volume estOut = this.input.proto(SamplingSource.create(1, 1, 1));
        estOut.set(0, est.getEncoding());

        Vects posOut = new Vects();
        posOut.add(this.input.getSampling().world(sample));

        this.pos = posOut;
        this.wpos = wposOut;
        this.wdir = wdirOut;
        this.wall = wallOut;

        this.agg = aggOut;
        this.sep = sepOut;
        this.est = estOut;

        return this;
    }
}
