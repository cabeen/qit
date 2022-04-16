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
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskComponents;
import qit.data.modules.mask.MaskRegionMerge;
import qit.data.modules.mask.MaskSort;
import qit.data.source.MatrixSource;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.Linkage;
import smile.clustering.linkage.SingleLinkage;

import java.util.List;
import java.util.Map;

@ModuleDescription("Segment a volume to obtain SLIC supervoxels")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Radhakrishna Achanta, Appu Shaji, Kevin Smith, Aurelien Lucchi, Pascal Fua, and Sabine Susstrunk, SLIC Superpixels, EPFL Technical Report 149300, June 2010.")
public class VolumeSegmentSuperSLIC implements Module
{
    @ModuleInput
    @ModuleDescription("the input volume")
    private Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    private Mask mask;

    @ModuleParameter
    @ModuleDescription("a maximum number of iterations")
    private int iters = 500;

    @ModuleParameter
    @ModuleDescription("the convergence criteria")
    private double error = 1e-3;

    @ModuleParameter
    @ModuleDescription("the average size (along each dimension) for the supervoxels")
    private double size = 20;

    @ModuleParameter
    @ModuleDescription("the scaleCamera for intensities")
    private double scale = 1.0;

    @ModuleParameter
    @ModuleDescription("a threshold region volume for merging")
    private double merge = 10;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a threshold gradient magnitude for grouping")
    private Double group = null;

    @ModuleOutput
    private Mask output;

    public VolumeSegmentSuperSLIC run()
    {
        Logging.info("started volume SLIC supervoxel segmentation");

        Sampling sampling = this.input.getSampling();

        Logging.info("finding centers");
        int numI = sampling.numI();
        int numJ = sampling.numJ();
        int numK = sampling.numK();

        double deltaI = sampling.deltaI();
        double deltaJ = sampling.deltaJ();
        double deltaK = sampling.deltaK();

        int superWindowI = (int) Math.ceil(this.size / deltaI);
        int superWindowJ = (int) Math.ceil(this.size / deltaJ);
        int superWindowK = (int) Math.ceil(this.size / deltaK);

        int superNumI = (int) Math.max(1, Math.round(numI * deltaI / this.size));
        int superNumJ = (int) Math.max(1, Math.round(numJ * deltaJ / this.size));
        int superNumK = (int) Math.max(1, Math.round(numK * deltaK / this.size));

        List<Sample> samples = Lists.newArrayList();
        for (int i = 0; i < superNumI; i++)
        {
            for (int j = 0; j < superNumJ; j++)
            {
                for (int k = 0; k < superNumK; k++)
                {
                    int ni = (int) Math.round((numI - 1) * i / (double) (superNumI - 1));
                    int nj = (int) Math.round((numJ - 1) * j / (double) (superNumJ - 1));
                    int nk = (int) Math.round((numK - 1) * k / (double) (superNumK - 1));

                    if (this.input.valid(ni, nj, nk, this.mask))
                    {
                        samples.add(new Sample(ni, nj, nk));
                    }
                }
            }
        }
        Logging.info(String.format("...segmenting %d supervoxels", samples.size()));

        Logging.info("computing features");
        Volume feature = this.input.proto(3 + this.input.getDim());
        for (Sample sample : sampling)
        {
            if (!this.input.valid(sample, this.mask))
            {
                continue;
            }

            Vect pos = sampling.world(sample);
            Vect val = this.input.get(sample).times(this.scale * this.size);
            Vect cat = pos.cat(val);

            feature.set(sample, cat);
        }

        Logging.info("perturbing centers");
        Vects centers = new Vects();

        VolumeGradientMagnitude mager = new VolumeGradientMagnitude();
        mager.input = this.input;
        Volume mag = mager.run().output;

        Sampling locals = SamplingSource.create(3, 3, 3);
        for (Sample sample : samples)
        {
            Sample minSample = sample;
            double minMag = mag.get(sample, 0);

            for (Sample local : locals)
            {
                int i = sample.getI() + local.getI() - 1;
                int j = sample.getJ() + local.getJ() - 1;
                int k = sample.getK() + local.getK() - 1;

                if (this.input.valid(i, j, k, this.mask))
                {
                    double nmagv = mag.get(i, j, k, 0);
                    if (nmagv < minMag)
                    {
                        minMag = nmagv;
                        minSample = new Sample(i, j, k);
                    }
                }
            }

            centers.add(feature.get(minSample));
        }

        Logging.info("iterating");
        Mask out = new Mask(sampling);
        Volume dist = this.input.proto(1);

        for (int iter = 0; iter < this.iters; iter++)
        {
            dist.setAll(VectSource.create1D(Double.MAX_VALUE));
            for (int idx = 0; idx < centers.size(); idx++)
            {
                Vect center = centers.get(idx);
                Sample nearest = sampling.nearest(center.sub(0, 3));

                for (int i = -superWindowI; i <= superWindowI; i++)
                {
                    for (int j = -superWindowJ; j <= superWindowJ; j++)
                    {
                        for (int k = -superWindowK; k <= superWindowK; k++)
                        {
                            int ni = nearest.getI() + i;
                            int nj = nearest.getJ() + j;
                            int nk = nearest.getK() + k;

                            if (!this.input.valid(ni, nj, nk, this.mask))
                            {
                                continue;
                            }

                            double d = feature.get(ni, nj, nk).dist(center);
                            if (out.background(ni, nj, nk) || d < dist.get(ni, nj, nk, 0))
                            {
                                dist.set(ni, nj, nk, 0, d);
                                out.set(ni, nj, nk, idx + 1);
                            }
                        }
                    }
                }
            }

            Map<Integer, Pair<Integer,Vect>> stats = Maps.newHashMap();
            for (int label = 1; label <= centers.size(); label++)
            {
                if (!stats.containsKey(label))
                {
                    stats.put(label, Pair.of(0, feature.dproto()));
                }
            }

            for (Sample sample : sampling)
            {
                if (this.input.valid(sample, this.mask))
                {
                    int label = out.get(sample);
                    Pair<Integer,Vect> stat = stats.get(label);
                    stat.a += 1;
                    stat.b.plusEquals(feature.get(sample));
                }
            }

            double residual = 0;
            for (int i = 0; i < centers.size(); i++)
            {
                Vect pcenter = centers.get(i);

                if (stats.containsKey(i + 1))
                {
                    Pair<Integer,Vect> stat = stats.get(i + 1);
                    Vect ncenter = stat.b.times(1.0 / stat.a);
                    residual += pcenter.dist(ncenter);
                    centers.set(i, ncenter);
                }
            }
            residual /= centers.size();

            Logging.info(String.format("iteration: %d, residual: %g", iter, residual));
            if (residual < this.error)
            {
                break;
            }
        }

        Logging.info("cleaning up connectivity");
        out = MaskSort.sort(out);
        MaskComponents comper = new MaskComponents();
        comper.input = out;
        out = comper.run().output;

        MaskRegionMerge merger = new MaskRegionMerge();
        merger.input = out;
        merger.full = false;
        merger.threshold = this.merge;
        out = merger.run().output;

        if (this.group != null)
        {
            out = this.group(out, mag, this.group);
        }

        this.output = out;

        Logging.info("finished SLIC supervoxel segmentation");

        return this;
    }

    private static Mask group(Mask mask, Volume mag, double thresh)
    {
        List<Integer> labels = MaskUtils.listNonzero(mask);

        Map<Pair<Integer,Integer>,VectOnlineStats> edges = Maps.newHashMap();
        for (Sample sample : mask.getSampling())
        {
            if (!mask.valid(sample, mask))
            {
                continue;
            }

            int label = mask.get(sample);
            double value = mag.get(sample, 0);

            for (Integers neighbor : Global.NEIGHBORS_6)
            {
                Sample nsample = new Sample(sample, neighbor);

                if (!mask.valid(nsample))
                {
                    continue;
                }

                int nlabel = mask.get(nsample);

                if (nlabel != label)
                {
                    Pair<Integer,Integer> pair = Pair.of(Math.min(label, nlabel), Math.max(label, nlabel));
                    if (!edges.containsKey(pair))
                    {
                        edges.put(pair, new VectOnlineStats());
                    }

                    edges.get(pair).update(value);
                }
            }
        }

        Map<Integer,Integer> lut = Maps.newHashMap();
        for (int i = 0; i < labels.size(); i++)
        {
            lut.put(labels.get(i), i);
        }

        Matrix mat = MatrixSource.constant(labels.size(), labels.size(), Double.MAX_VALUE);
        for (Pair<Integer,Integer> edge : edges.keySet())
        {
            int i = lut.get(edge.a);
            int j = lut.get(edge.b);
            double dist = edges.get(edge).mean;

            mat.set(i, j, dist);
            mat.set(j, i, dist);
        }

        Linkage link = new SingleLinkage(mat.toArray());
        HierarchicalClustering cluster = new HierarchicalClustering(link);
        int[] clustering = cluster.partition(thresh);
        Global.assume(clustering.length == labels.size(), "invalid clustering result");

        Map<Integer, Integer> map = Maps.newHashMap();
        for (int i = 0; i < labels.size(); i++)
        {
            map.put(labels.get(i), clustering[i]);
        }

        return MaskUtils.map(mask, map);
    }
}
