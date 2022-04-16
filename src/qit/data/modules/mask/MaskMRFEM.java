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

package qit.data.modules.mask;

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Integers;
import qit.data.datasets.*;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Refine a segmentation using a Expectation Maximization Markov Random Field framework")
@ModuleAuthor("Ryan Cabeen")
public class MaskMRFEM implements Module
{
    @ModuleInput
    @ModuleDescription("input region")
    public Mask input;

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume volume;

    @ModuleInput
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the distance in voxels for adaptation")
    public Double distance = 1.0;

    @ModuleParameter
    @ModuleDescription("use a 6-neighborhood (instead of a full 27-neighborhood with diagonals)")
    public boolean mrfCross = false;

    @ModuleParameter
    @ModuleDescription("use the following spatial regularization weight")
    public Double mrfGamma = 1.0;

    @ModuleParameter
    @ModuleDescription("use the gain used for the conditional random field (zero will disable it)")
    public Double mrfCrfGain = 1.0;

    @ModuleParameter
    @ModuleDescription("use the following number of MRF optimization iterations")
    public Integer mrfIcmIters = 5;

    @ModuleParameter
    @ModuleDescription("use the following number of expectation maximization iteration")
    public Integer mrfEmIters = 5;

    @ModuleOutput
    @ModuleDescription("output input")
    public Mask output;

    @Override
    public MaskMRFEM run()
    {
        final Sampling sampling = this.input.getSampling();
        final Volume data = this.volume.getVolume(0);

        final VectOnlineStats fgStats = new VectOnlineStats();
        final VectOnlineStats bgStats = new VectOnlineStats();

        Volume unary = data.proto(2);
        final Mask seg = this.input.copy();

        double gain = -1 * Math.log(0.5) / this.distance;
        Volume dist = MaskDistanceTransform.apply(this.input);

        Runnable updateUnary = () ->
        {
            Logging.info("... computing unary potentials");
            for (Sample sample : sampling)
            {
                if (data.valid(sample, this.mask))
                {
                    double myval = data.get(sample, 0);

                    double pfg = MathUtils.gaussian(myval, fgStats.mean, fgStats.std);
                    double pbg = MathUtils.gaussian(myval, bgStats.mean, bgStats.std);

                    double dfg = Math.exp(-gain * dist.get(sample, 0));
                    double dbg = 1.0 - dfg;

                    unary.set(sample, 0, pbg * dbg);
                    unary.set(sample, 1, pfg * dfg);
                }
            }
        };

        Runnable updateLabels = () ->
        {
            Logging.info("computing labels");
            for (Sample sample : sampling)
            {
                if (data.valid(sample, this.mask))
                {
                    seg.set(sample, unary.get(sample).maxidx());
                }
            }
            int count = MaskUtils.count(seg);
            Logging.info("... detected foreground voxels: " + count);
            Logging.info("... detected background voxels: " + (sampling.size() - count));
        };

        Runnable updateStats = () ->
        {
            Logging.info("... updating statistics");
            for (Sample sample : sampling)
            {
                if (data.valid(sample, this.mask))
                {
                    double myval = data.get(sample, 0);
                    if (seg.foreground(sample))
                    {
                        fgStats.update(myval);
                    }
                    else
                    {
                        bgStats.update(myval);
                    }
                }
            }

            Logging.info("... detected foreground mean: " + fgStats.mean);
            Logging.info("... detected foreground std: " + fgStats.std);
            Logging.info("... detected background mean: " + bgStats.mean);
            Logging.info("... detected background std: " + bgStats.std);
        };

        updateStats.run();
        updateUnary.run();
        updateLabels.run();

        Logging.info("running MRF-EM regularization");
        List<Integers> neighbors = this.mrfCross ? Global.NEIGHBORS_6 : Global.NEIGHBORS_27;

        for (int em = 0; em < this.mrfEmIters; em++)
        {
            Logging.info("running maximization");
            updateStats.run();
            updateUnary.run();

            Logging.info("running expectation");
            for (int icm = 0; icm < this.mrfIcmIters; icm++)
            {
                int changed = 0;
                double energy = 0;

                for (Sample sampleCenter : sampling)
                {
                    if (data.valid(sampleCenter, this.mask))
                    {
                        double valueCenter = data.get(sampleCenter, 0);

                        Vect costs = VectSource.createND(2);
                        for (int label = 0; label < 2; label++)
                        {
                            double costCount = 0;
                            double costPairwise = 0;

                            for (Integers n : neighbors)
                            {
                                Sample sampleOuter = sampleCenter.offset(n);

                                if (data.valid(sampleOuter, this.mask))
                                {
                                    double valueOuter = data.get(sampleOuter, 0);
                                    double delta = (valueOuter - valueCenter) / fgStats.std;
                                    double pair = this.mrfGamma * Math.exp(-this.mrfCrfGain * delta * delta);
                                    int labelOuter = seg.get(sampleOuter);

                                    double mypairwise = labelOuter != label ? pair : 0;

                                    costPairwise += mypairwise;
                                    costCount += 1;
                                }
                            }

                            double mycost = -Math.log(unary.get(sampleCenter, label) + Global.DELTA);

                            if (costCount > 0)
                            {
                                mycost += costPairwise / costCount;
                            }

                            costs.set(label, mycost);
                        }

                        int prevLabel = seg.get(sampleCenter);
                        int nextLabel = costs.minidx();

                        if (prevLabel != nextLabel)
                        {
                            seg.set(sampleCenter, nextLabel);
                            changed += 1;
                        }

                        energy += costs.get(nextLabel);
                    }
                }

                Logging.info(String.format("... at em iter %d, icm iter %d, with cost %g, and %d updated voxels", em + 1, icm + 1, energy / sampling.size(), changed));

                if (changed == 0)
                {
                    break;
                }
            }
        }

        this.output = seg;

        return this;
    }
}
