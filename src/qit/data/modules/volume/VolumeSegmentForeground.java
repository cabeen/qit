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


package qit.data.modules.volume;

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Integers;
import qit.data.datasets.*;
import qit.data.datasets.Record;
import qit.data.modules.mask.MaskFill;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.structs.Gaussian;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Segment the foreground of a volume using a statistical approach")
@ModuleAuthor("Ryan Cabeen")
public class VolumeSegmentForeground implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input = null;

    @ModuleParameter
    @ModuleDescription("the number of times to median prefilter")
    public Integer median = 1;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply a threshold")
    public Double thresh = null;

    @ModuleParameter
    @ModuleDescription("fill the foreground mask to remove islands")
    public boolean fill = false;

    @ModuleParameter
    @ModuleDescription("extract the largest island")
    public boolean largest = false;

    @ModuleParameter
    @ModuleDescription("apply markov random field spatial regularization")
    public boolean mrf = false;

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
    @ModuleOptional
    @ModuleDescription("the output mask")
    public Mask output = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output report")
    public Table report = null;

    public VolumeSegmentForeground run()
    {
        Sampling sampling = this.input.getSampling();

        Logging.info("prefiltering");
        Volume original = this.input.getVolume(0);
        Volume prefilt = VolumeFilterMedian.apply(original, this.median);
        Volume volume = VolumeThreshold.applyToVolume(prefilt, this.thresh);

        Logging.info("computing intensity statistics");
        final VectOnlineStats fgStats = new VectOnlineStats();
        final VectOnlineStats bgStats = new VectOnlineStats();

        {
            Logging.info("computing gaussian spatial statistics");
            Gaussian gauss = VolumeUtils.gaussian(volume);

            for (Sample sample : sampling)
            {
                Vect mypos = sampling.world(sample);
                double mahal = gauss.mahal2(mypos);
                double myval = volume.get(sample, 0);

                if (mahal <= 5)
                {
                    fgStats.update(myval);
                }

                if (mahal >= 7)
                {
                    bgStats.update(myval);
                }
            }
        }

        Volume unary = volume.proto(2);
        final Mask seg = new Mask(sampling);

        Runnable updateUnary = () ->
        {
            Logging.info("... computing unary potentials");
            for (Sample sample : sampling)
            {
                double myval = volume.get(sample, 0);

                double pbg = MathUtils.gaussian(myval, bgStats.mean, bgStats.std);
                double pfg = MathUtils.gaussian(myval, fgStats.mean, fgStats.std);

                unary.set(sample, 0, pbg);
                unary.set(sample, 1, pfg);
            }
        };

        Runnable updateLabels = () ->
        {
            Logging.info("computing labels");
            for (Sample sample : sampling)
            {
                seg.set(sample, unary.get(sample).maxidx());
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
                double myval = volume.get(sample, 0);
                if (seg.foreground(sample))
                {
                    fgStats.update(myval);
                }
                else
                {
                    bgStats.update(myval);
                }
            }

            Logging.info("... detected foreground mean: " + fgStats.mean);
            Logging.info("... detected foreground std: " + fgStats.std);
            Logging.info("... detected background mean: " + bgStats.mean);
            Logging.info("... detected background std: " + bgStats.std);
        };

        updateUnary.run();
        updateLabels.run();

        if (!this.mrf)
        {
            updateStats.run();
            updateUnary.run();
            updateLabels.run();
        }
        else
        {
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
                        double valueCenter = volume.get(sampleCenter, 0);

                        Vect costs = VectSource.createND(2);
                        for (int label = 0; label < 2; label++)
                        {
                            double cost = 0;
                            double costCount = 0;

                            for (Integers n : neighbors)
                            {
                                Sample sampleOuter = sampleCenter.offset(n);

                                if (!volume.valid(sampleOuter))
                                {
                                    continue;
                                }

                                double valueOuter = volume.get(sampleOuter, 0);
                                double delta = (valueOuter - valueCenter) / fgStats.std;
                                double pair = this.mrfGamma * Math.exp(-this.mrfCrfGain * delta * delta);
                                int labelOuter = seg.get(sampleOuter);

                                double myunary = -Math.log(unary.get(sampleCenter, label) + Global.DELTA);
                                double mypairwise = labelOuter != label ? pair : 0;

                                cost += myunary + mypairwise;
                                costCount += 1;
                            }

                            costs.set(label, cost / costCount);
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

                    Logging.info(String.format("... at em iter %d, icm iter %d, with cost %g, and %d updated voxels", em + 1, icm + 1, energy / sampling.size(), changed));

                    if (changed == 0)
                    {
                        break;
                    }
                }
            }
        }

        if (this.fill)
        {
            Logging.info("filling mask");
            seg.set(MaskFill.apply(seg));
        }

        if (this.largest)
        {
            Logging.info("extracting the largest");
            seg.set(MaskUtils.largest(seg));
        }

        Table metrics = new Table();
        metrics.withField("name");
        metrics.withField("value");
        metrics.addRecord(new Record().with("name", "fg_mean").with("value", fgStats.mean));
        metrics.addRecord(new Record().with("name", "fg_std").with("value", fgStats.std));
        metrics.addRecord(new Record().with("name", "bg_mean").with("value", bgStats.mean));
        metrics.addRecord(new Record().with("name", "bg_std").with("value", bgStats.std));
        metrics.addRecord(new Record().with("name", "snr").with("value", fgStats.mean / bgStats.mean));
        metrics.addRecord(new Record().with("name", "cnr").with("value", fgStats.std / bgStats.mean));
        metrics.addRecord(new Record().with("name", "svnvr").with("value", fgStats.var/ bgStats.var));

        this.output = seg;
        this.report = metrics;

        return this;
    }

    public static Mask apply(Volume volume)
    {
        return new VolumeSegmentForeground()
        {{
            this.input = volume;
        }}.run().output;
    }
}