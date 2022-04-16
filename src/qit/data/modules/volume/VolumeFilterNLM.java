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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import numerics.BesselFunctions;
import org.apache.commons.math3.special.BesselJ;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.structs.Pointer;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Solids;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@ModuleDescription("Apply a non-local means filter to a volume")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Manjon, Jose V., et al. Adaptive non-local means denoising of MR images with spatially varying noise levels. Journal of Magnetic Resonance Imaging 31.1 (2010): 192-203.")
public class VolumeFilterNLM implements Module
{
    enum FilterMode
    {
        Volumetric, SliceI, SliceJ, SliceK
    }

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the volume channel (default applies to all)")
    public Integer channel;

    @ModuleParameter
    @ModuleDescription("indicate whether the filtering should be volumetric (default) or restricted to a specified slice")
    public FilterMode mode = FilterMode.Volumetric;

    @ModuleParameter
    @ModuleDescription("the patch size")
    public int patch = 1;

    @ModuleParameter
    @ModuleDescription("the search window size")
    public int search = 2;

    @ModuleParameter
    @ModuleDescription("the statistics window size (for computing mean and variance in adaptive filtering)")
    public int stats = 2;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a fixed noise level (skips adaptive noise esimation)")
    public Double h = null;

    @ModuleParameter
    @ModuleDescription("the minimum meaningful value for mean and vars")
    public double epsilon = 0.00001;

    @ModuleParameter
    @ModuleDescription("the mean intensity threshold")
    public double meanThresh = 0.95;

    @ModuleParameter
    @ModuleDescription("the variance intensity threshold")
    public double varThresh = 0.5;

    @ModuleParameter
    @ModuleDescription("a factor for scaling adaptive sensitivity (larger values remove more noise)")
    public double factor = 1.0;

    @ModuleParameter
    @ModuleDescription("use the h bandwidth as a fraction of the mean image intensity")
    public boolean hrel = false;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask for computing the mean image intensity for relative bandwidth (see hrel)")
    public Mask hrelMask ;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify which channels to use for hrel")
    public String hrelwhich = "0";

    @ModuleParameter
    @ModuleDescription("whether a Rician noise model should be used")
    public boolean rician = false;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output noise map")
    public Volume outputNoise = null;

    public VolumeFilterNLM run()
    {
        Function<Integer, Integers> support = (n) ->
        {
            switch (this.mode)
            {
                case Volumetric:
                    return new Integers(n, n, n);
                case SliceI:
                    return new Integers(0, n, n);
                case SliceJ:
                    return new Integers(n, 0, n);
                case SliceK:
                    return new Integers(n, n, 0);
                default:
                    throw new RuntimeException("invalid slice mode: " + this.mode);
            }
        };

        final Integers patchRadius = support.apply(this.patch);
        final Integers searchRadius = support.apply(this.search);
        final Integers statsRadius = support.apply(this.stats);

        final Sampling sampling = this.input.getSampling();
        final Volume out = this.input.proto();
        final Volume outnoise = this.input.proto(1);

        double hfactor = 1.0;

        if (this.hrel && this.h != null)
        {
            VectOnlineStats stats = new VectOnlineStats();
            List<Integer> which = CliUtils.parseIndexList(this.hrelwhich, this.input.getDim());

            Mask myHrelMask = this.mask;
            if (this.hrelMask != null)
            {
                Logging.info("using relative mask");
                myHrelMask = this.hrelMask;
            }

            for (Sample sample : sampling)
            {
                if (this.input.valid(sample, myHrelMask))
                {
                    for (int d : which)
                    {
                        stats.update(this.input.get(sample, d));
                    }
                }
            }

            hfactor = stats.mean;

            Logging.info("detected mean intensity: " + hfactor);
            Logging.info("using relative bandwidth: " + hfactor * this.h);
        }

        final Double myh = this.h != null ? hfactor * this.h : null;

        final Consumer<Integer> process = (didx) ->
        {
            Logging.info(String.format("processing subvolume %d/%d", didx + 1, this.input.getDim()));

            if (this.channel != null && didx != this.channel)
            {
                for (Sample sample : sampling)
                {
                    if (this.input.valid(sample, this.mask))
                    {
                        out.set(sample, didx, this.input.get(sample, didx));
                    }
                }

                return;
            }

            final Volume means = this.input.proto(1);
            final Volume vars = this.input.proto(1);
            final Volume noises = this.input.proto(1);
            final Volume estimates = this.input.proto(1);
            final Volume counts = this.input.proto(1);

            double minGlobal = Double.MAX_VALUE;
            double maxGlobal = Double.MIN_VALUE;

            Logging.info("... computing volume statistics");
            for (Sample centerSample : sampling)
            {
                if (this.input.valid(centerSample, this.mask))
                {
                    VectOnlineStats stats = new VectOnlineStats();
                    for (Sample statsSample : sampling.iterateNeighborhood(centerSample, statsRadius))
                    {
                        if (this.input.valid(statsSample, this.mask))
                        {
                            stats.update(this.input.get(statsSample, didx));
                        }
                    }

                    means.set(centerSample, stats.mean);
                    vars.set(centerSample, stats.var);

                    double value = this.input.get(centerSample, didx);
                    minGlobal = Math.min(minGlobal, value);
                    maxGlobal = Math.max(maxGlobal, value);
                }
            }

            Logging.info("... filtering volume");
            int lastk = -1;
            for (Sample centerSample : sampling)
            {
                if (centerSample.getK() != lastk)
                {
                    lastk = centerSample.getK();
                    Logging.infosub("...... processing slice %d of %d", lastk + 1, sampling.numK());
                }

                if (!this.input.valid(centerSample, this.mask))
                {
                    continue;
                }

                double meanCenter = means.get(centerSample, 0);
                double varCenter = vars.get(centerSample, 0);
                double cvCenter = MathUtils.zero(meanCenter) ? 0 : varCenter / meanCenter;

                double sumWeight = 0.0;
                double maxWeight = 0.0;

                Map<Sample, Double> sumPatches = Maps.newHashMap();
                for (Sample offset : sampling.iterateNeighborhood(patchRadius))
                {
                    sumPatches.put(offset, 0.0);
                }

                if (meanCenter <= this.epsilon || cvCenter <= this.epsilon)
                {
                    double weight = 1.0;

                    for (Sample patchSample : sampling.iterateNeighborhood(centerSample, patchRadius))
                    {
                        if (this.input.valid(patchSample, this.mask))
                        {
                            sumWeight += weight;

                            double value = this.input.get(patchSample, didx);
                            value = this.rician ? value * value : value;

                            estimates.set(patchSample, 0, estimates.get(patchSample, 0) + value);
                            counts.set(patchSample, 0, counts.get(patchSample, 0) + 1);
                        }
                    }

                    for (Sample offset : sumPatches.keySet())
                    {
                        Sample patchSample = centerSample.offset(offset.integers());

                        if (this.input.valid(patchSample, this.mask))
                        {
                            double value = this.input.get(patchSample, didx);
                            value = this.rician ? value * value : value;

                            sumPatches.put(offset, sumPatches.get(offset) + weight * value);
                        }
                    }
                }
                else
                {
                    double minDist = Double.MAX_VALUE;

                    if (myh == null)
                    {
                        for (Sample searchSample : sampling.iterateNeighborhood(centerSample, searchRadius))
                        {
                            if (centerSample.equals(searchSample) || !this.input.valid(searchSample, this.mask))
                            {
                                continue;
                            }

                            double meanNeigh = means.get(searchSample, 0);
                            double varNeigh = vars.get(searchSample, 0);

                            if (meanNeigh <= this.epsilon || varNeigh <= this.epsilon)
                            {
                                continue;
                            }

                            double meanRatio = meanCenter / meanNeigh;
                            double meanRatioInv = (maxGlobal - meanCenter) / (maxGlobal - meanNeigh);
                            double varRatio = varCenter / varNeigh;

                            boolean passMeanRatio = meanRatio > this.meanThresh && meanRatio < 1.0 / this.meanThresh;
                            boolean passMeanRatioInv = meanRatioInv > this.meanThresh && meanRatioInv < 1.0 / this.meanThresh;
                            boolean passVarRatio = varRatio > this.varThresh && varRatio < 1.0 / this.varThresh;

                            if ((passMeanRatio || passMeanRatioInv) && passVarRatio)
                            {
                                double count = 0;
                                double sum = 0;

                                for (Sample patchSample : sampling.iterateNeighborhood(searchSample, patchRadius))
                                {
                                    if (this.input.valid(patchSample, this.mask))
                                    {
                                        double v1 = this.input.get(patchSample, didx);
                                        double v2 = means.get(patchSample, 0);
                                        double dv = v1 - v2;

                                        sum += dv * dv;
                                        count += 1;
                                    }
                                }

                                double meanDist = sum / count;
                                minDist = Math.min(meanDist, minDist);
                            }
                        }

                        minDist = Double.isFinite(minDist) ? minDist : 1.0;

                        for (Sample patchSample : sampling.iterateNeighborhood(centerSample, patchRadius))
                        {
                            if (this.input.valid(patchSample, this.mask))
                            {
                                noises.set(patchSample, minDist);
                            }
                        }
                    }

                    for (Sample searchSample : sampling.iterateNeighborhood(centerSample, searchRadius))
                    {
                        if (!this.input.valid(searchSample, this.mask))
                        {
                            continue;
                        }

                        double meanNeigh = means.get(searchSample, 0);
                        double varNeigh = vars.get(searchSample, 0);

                        if (centerSample.equals(searchSample) || !this.input.valid(searchSample, this.mask))
                        {
                            continue;
                        }

                        double meanRatio = meanCenter / meanNeigh;
                        double meanRatioInv = (maxGlobal - meanCenter) / (maxGlobal - meanNeigh);
                        double varRatio = varCenter / varNeigh;

                        boolean passMeanRatio = meanRatio > this.meanThresh && meanRatio < 1.0 / this.meanThresh;
                        boolean passMeanRatioInv = meanRatioInv > this.meanThresh && meanRatioInv < 1.0 / this.meanThresh;
                        boolean passVarRatio = varRatio > this.varThresh && varRatio < 1.0 / this.varThresh;

                        if ((passMeanRatio || passMeanRatioInv) && passVarRatio)
                        {
                            double hh = myh != null ? myh * myh : minDist;
                            hh *= this.factor;

                            double sumDist = 0;
                            int countDist = 0;

                            for (Sample offset : sampling.iterateNeighborhood(patchRadius))
                            {
                                Sample centerPatchSample = centerSample.offset(offset.integers());
                                Sample searchPatchSample = searchSample.offset(offset.integers());

                                if (sampling.contains(centerPatchSample) && this.input.valid(searchPatchSample, this.mask))
                                {
                                    if (myh != null)
                                    {
                                        double v1 = this.input.get(centerPatchSample, didx);
                                        double v2 = this.input.get(searchPatchSample, didx);
                                        double dv = v1 - v2;
                                        sumDist += dv * dv;
                                    }
                                    else
                                    {
                                        double d1 = this.input.get(centerPatchSample, didx) - means.get(centerPatchSample, 0);
                                        double d2 = this.input.get(searchPatchSample, didx) - means.get(searchPatchSample, 0);
                                        double dv = d1 - d2;
                                        sumDist += dv * dv;
                                    }

                                    countDist += 1;
                                }
                            }

                            double meanDist = sumDist / countDist;

                            if (meanDist <= 3.0 * hh)
                            {
                                double weight = Math.exp(-meanDist / hh);

                                sumWeight += weight;
                                maxWeight = Math.max(maxWeight, weight);

                                if (!MathUtils.zero(weight))
                                {
                                    for (Sample offset : sumPatches.keySet())
                                    {
                                        Sample patchSample = searchSample.offset(offset.integers());

                                        if (this.input.valid(patchSample, this.mask))
                                        {
                                            double value = input.get(patchSample, didx);
                                            value = this.rician ? value * value : value;

                                            sumPatches.put(offset, sumPatches.get(offset) + weight * value);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    maxWeight = MathUtils.zero(maxWeight) ? 1.0 : maxWeight;
                    sumWeight += maxWeight;

                    for (Sample offset : sumPatches.keySet())
                    {
                        Sample patchSample = centerSample.offset(offset.integers());

                        if (this.input.valid(patchSample, this.mask))
                        {
                            double value = this.input.get(patchSample, didx);
                            value = this.rician ? value * value : value;

                            sumPatches.put(offset, sumPatches.get(offset) + maxWeight * value);
                        }
                    }

                    for (Sample offset : sumPatches.keySet())
                    {
                        Sample patchSample = centerSample.offset(offset.integers());

                        if (this.input.valid(patchSample, this.mask))
                        {
                            double estimate = estimates.get(patchSample, 0);
                            estimate += sumPatches.get(offset) / sumWeight;

                            estimates.set(patchSample, 0, estimate);
                            counts.set(patchSample, 0, counts.get(patchSample, 0) + 1);
                        }
                    }
                }
            }

            Logging.info("... merging patches");
            for (Sample sample : sampling)
            {
                double count = counts.get(sample, 0);
                if (this.input.valid(sample, this.mask) && MathUtils.nonzero(count))
                {
                    out.set(sample, didx, estimates.get(sample, 0) / count);
                    outnoise.set(sample, 0, noises.get(sample, 0) / out.getDim());
                }
            }

            if (this.rician)
            {
                Logging.info("... removing Rician bias");
                VolumeFilterGaussian smoother = new VolumeFilterGaussian();
                smoother.input = noises;
                smoother.sigma = 1.0;
                Volume smoothed = smoother.run().output;

                for (Sample sample : sampling)
                {
                    if (this.input.valid(sample, this.mask))
                    {
                        double noise = myh != null ? myh : smoothed.get(sample, 0);

                        double snr = means.get(sample, 0) / Math.sqrt(noise);
                        double snrsq = snr * snr;

                        double c1 = 2.0 + snrsq;
                        double c2 = -0.125 * Math.PI * Math.exp(-0.5 * snrsq) * (2.0 + snrsq) * (2.0 + snrsq) * BesselFunctions.besselI0(0.25 * snrsq);
                        double c3 = snrsq * BesselFunctions.besselI1(0.25 * snrsq);
                        double sum = c1 + c2 + c3;

                        double corr = 1.0;
                        if (Double.isFinite(corr) && sum >= 0.001 &&  sum <= 10)
                        {
                            corr = sum;
                        }

                        double bias = 2.0 * noise / corr;

                        double value = out.get(sample, didx);
                        value -= bias;
                        value = Math.sqrt(value);

                        out.set(sample, didx, value);
                    }
                }
            }
        };

        if (this.threads > 1 && this.input.getDim() > 1)
        {
            ExecutorService exec = Executors.newFixedThreadPool(this.threads);

            for (int d = 0; d < this.input.getDim(); d++)
            {
                final int didx = d;
                exec.execute(() -> process.accept(didx));
            }

            exec.shutdown();
            try
            {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
            catch (InterruptedException e)
            {
                Logging.error("failed to execute in concurrent mode");
            }
        }
        else
        {
            for (int d = 0; d < this.input.getDim(); d++)
            {
                process.accept(d);
            }
        }

        this.output = out;
        this.outputNoise = outnoise;

        return this;
    }
}
