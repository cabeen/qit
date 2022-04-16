/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.modules.volume;

import com.google.common.collect.Lists;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.structs.Pointer;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskSort;
import qit.data.source.MaskSource;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectUtils;
import qit.data.utils.vects.cluster.VectsClusterDPM;
import qit.data.utils.vects.cluster.VectsClusterGM;
import qit.data.utils.vects.cluster.VectsClusterKM;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.data.utils.volume.VolumeOnlineStats;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.data.utils.volume.VolumeVoxelVectStats;
import qit.math.structs.CovarianceType;
import qit.math.structs.Gaussian;
import qit.math.structs.GaussianMixture;
import qit.math.utils.MathUtils;

import java.awt.Point;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@ModuleDescription("Segment a volume by clustering intensities (with k-means, dp-means, or Gaussian mixtures)")
@ModuleAuthor("Ryan Cabeen")
public class VolumeSegmentCluster implements Module
{
    @ModuleInput
    @ModuleDescription("the input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("a maximum number of iterations")
    public int iters = 10;

    @ModuleParameter
    @ModuleDescription("the number classes (inital guess in the case of dp-means)")
    public int num = 3;

    @ModuleParameter
    @ModuleDescription("the number of restarts")
    public int restarts = 1;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a threshold for detecting the number of clusters in the DP-means algorithm")
    public Double lambda = null;

    @ModuleParameter
    @ModuleDescription("use a Bayesian probabilistic approach (MRF Gaussian mixture expectation maximization framework)")
    public boolean bayesian = false;

    @ModuleParameter
    @ModuleDescription("use the following number of expectation maximization iteration")
    public Integer emIters = 5;

    @ModuleParameter
    @ModuleDescription("specify a covariance type (spherical, diagonal, full)")
    public String emCov = CovarianceType.spherical.toString();

    @ModuleParameter
    @ModuleDescription("a regularization parameter added to the covariance")
    public Double emReg = 0.0;

    @ModuleParameter
    @ModuleDescription("use the following number of MRF optimization iterations")
    public Integer mrfIters = 5;

    @ModuleParameter
    @ModuleDescription("use a 6-neighborhood (instead of a full 27-neighborhood with diagonals)")
    public boolean mrfCross = false;

    @ModuleParameter
    @ModuleDescription("use the following spatial regularization weight")
    public Double mrfGamma = 1.0;

    @ModuleParameter
    @ModuleDescription("use the gain used for the conditional random field (zero will disable it)")
    public Double mrfCrfGain = 1.0;

    @ModuleOutput
    @ModuleDescription("the output label map")
    public Mask labels;

    @ModuleOutput
    @ModuleDescription("the output membership map")
    public Volume membership;

    public VolumeSegmentCluster run()
    {
        Logging.info("started volume cluster segmentation");

        Sampling sampling = this.input.getSampling();


        Supplier<Mask> kmeans = () ->
        {
            List<Sample> samples = Lists.newArrayList();
            Vects values = new Vects();

            for (Sample sample : sampling)
            {
                if (this.input.valid(sample, this.mask))
                {
                    samples.add(sample);
                    values.add(this.input.get(sample));
                }
            }

            VectsClusterKM cluster = this.lambda != null ? new VectsClusterDPM().withLambda(this.lambda) : new VectsClusterKM();
            cluster.withMaxIter(this.iters);
            cluster.withK(this.num);
            cluster.withVects(values);
            int[] clabels = cluster.getOutput();

            Logging.info("count: " + cluster.getK());

            final Mask labels = MaskSource.create(sampling);
            for (int idx = 0; idx < samples.size(); idx++)
            {
                labels.set(samples.get(idx), clabels[idx]);
            }

            labels.set(MaskSort.sort(labels));

            return labels;
        };

        Mask labels = kmeans.get();
        int count = MaskUtils.listNonzero(labels, this.mask).size();
        Volume member = this.input.proto(count);

        Logging.info("found clusters: " + count);

        Runnable labelmember = () ->
        {
            for (Sample sample : sampling)
            {
                if (this.input.valid(sample, this.mask))
                {
                    int label = labels.get(sample);
                    if (label > 0)
                    {
                        member.set(sample, label - 1, 1.0);
                    }
                }
            }
        };

        labelmember.run();

        if (this.bayesian)
        {
            Logging.info("building initial statistical models");
            Vect weights = new Vect(sampling.size());
            Vects values = new Vects();
            for (Sample sample : sampling)
            {
                values.add(this.input.get(sample));
            }

            VolumeVoxelVectStats stats = new VolumeVoxelVectStats().withInput(this.input).withMask(this.mask).run();
            Pointer<GaussianMixture> model = Pointer.to(null);

            Runnable maximization = () ->
            {
                Vect mixing = VectSource.createND(count);
                Gaussian[] comps = new Gaussian[count];

                for (int i = 0; i < count; i++)
                {
                    for (Sample sample : sampling)
                    {
                        if (this.input.valid(sample, this.mask))
                        {
                            double gamma = member.get(sample, i);
                            mixing.plusEquals(i, gamma);
                            weights.plusEquals(sampling.index(sample), gamma);
                        }
                    }

                    weights.normalizeProbEquals();
                    comps[i] = new VectsGaussianFitter().withWeights(weights).withType(CovarianceType.valueOf(this.emCov)).withInput(values).withAdd(this.emReg).getOutput();
                }

                model.set(new GaussianMixture(mixing.normalizeProb(), comps));
            };

            Supplier<Integer> expectation = () ->
            {
                int changed = 0;

                for (Sample sample : sampling)
                {
                    if (this.input.valid(sample, this.mask))
                    {
                        Vect point = values.get(sampling.index(sample));
                        Vect prob = model.get().density(point).normalizeProb();
                        member.set(sample, prob);
                        int nlabel = VectUtils.maxidx(prob) + 1;

                        if (nlabel != labels.get(sample))
                        {
                            labels.set(sample, nlabel);
                            changed += 1;
                        }
                    }
                }

                return changed;
            };

            List<Integers> neighbors = this.mrfCross ? Global.NEIGHBORS_6 : Global.NEIGHBORS_27;

            Supplier<Pair<Integer, Double>> mrf = () ->
            {
                int changed = 0;
                double total = 0;

                for (Sample sampleCenter : sampling)
                {
                    if (this.input.valid(sampleCenter, this.mask))
                    {
                        Vect valueCenter = values.get(sampling.index(sampleCenter));

                        Vect costs = VectSource.createND(count);
                        for (int label = 0; label < count; label++)
                        {
                            double costCount = 0;
                            double costPairwise = 0;

                            for (Integers n : neighbors)
                            {
                                Sample sampleOuter = sampleCenter.offset(n);

                                if (this.input.valid(sampleOuter, this.mask))
                                {
                                    Vect valueOuter = values.get(sampling.index(sampleOuter));
                                    double delta = valueOuter.minus(valueCenter).divSafe(stats.std).norm();
                                    double pair = this.mrfGamma * Math.exp(-this.mrfCrfGain * delta * delta);
                                    int labelOuter = labels.get(sampleOuter);

                                    double mypairwise = labelOuter != label ? pair : 0;

                                    costPairwise += mypairwise;
                                    costCount += 1;
                                }
                            }

                            double mycost = -Math.log(member.get(sampleCenter, label) + Global.DELTA);

                            if (costCount > 0)
                            {
                                mycost += costPairwise / costCount;
                            }

                            costs.set(label, mycost);
                        }

                        int prevLabel = labels.get(sampleCenter);
                        int nextLabel = costs.minidx() + 1;

                        if (prevLabel != nextLabel)
                        {
                            labels.set(sampleCenter, nextLabel);
                            changed += 1;
                        }

                        labelmember.run();

                        total += costs.get(nextLabel - 1);
                    }
                }

                total /= sampling.size();

                return Pair.of(changed, total);
            };

            Logging.progress("starting bayesian segmentation");

            Logging.info("running initial maximization");
            maximization.run();

            for (int em = 0; em < this.emIters; em++)
            {
                Logging.info("... running iteration %d mrf", em);
                for (int icm = 0; icm < this.mrfIters; icm++)
                {
                    Pair<Integer, Double> mrfout = mrf.get();
                    int changedMrf = mrfout.a;
                    double energyMrf = mrfout.b;

                    Logging.info(String.format("...... at em iter %d, icm iter %d, with cost %g, and %d updated voxels", em + 1, icm + 1, energyMrf, changedMrf));

                    if (mrfout.a == 0)
                    {
                        Logging.info("exiting mrf stage due to convergence");
                        break;
                    }
                }
                Logging.info("... running em iteration %d expectation", em);
                int changed = expectation.get();

                if (changed == 0)
                {
                    Logging.info("finishing optimization due to convergence");
                    break;
                }

                Logging.info("... running em iteration %d maximization", em);
                maximization.run();
            }

            labels.set(MaskSort.sort(labels));
        }

        List<Integer> list = MaskUtils.listNonzero(labels, this.mask);
        Logging.progress("... final cluster count: " + list.size());
        Logging.info("finished volume cluster segmentation");

        this.labels = labels;
        this.membership = member;

        return this;
    }
}
