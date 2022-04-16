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

package qit.data.modules.mri.model;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleExpert;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.curves.CurvesClosestPointTransform;
import qit.data.modules.curves.CurvesDensity;
import qit.data.modules.curves.CurvesLandmarks;
import qit.data.modules.mask.MaskDistanceTransform;
import qit.data.utils.enums.KernelInterpolationType;
import qit.data.utils.vects.stats.VectsStats;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.VectSource;
import qit.data.utils.vects.cluster.VectsClusterKM;
import qit.data.utils.mri.estimation.FibersEstimator;

import java.util.Collections;
import java.util.List;

@ModuleUnlisted
@ModuleDescription("Parcellate a mask based on connectivity patterns derived from tractography")
@ModuleAuthor("Ryan Cabeen")
public class VolumeModelTrackParcellate implements Module
{
    public static final String PATH = "path";
    public static final String DIST = "dist";
    public static final String SCPT = "scpt";

    @ModuleInput
    @ModuleDescription("the input model volume")
    private Volume input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a model name (default will try to detect it)")
    public String model = null;

    @ModuleInput
    @ModuleDescription("a mask to parcellate based on connectivity")
    private Mask mask;

    @ModuleParameter
    @ModuleDescription("a number of samples per voxel")
    private int density = 100;

    @ModuleParameter
    @ModuleDescription("a number of segments to extract")
    private int segments = 2;

    @ModuleParameter
    @ModuleDescription("distance measure to use (path, dist, or scpt)")
    private String rep = SCPT;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a coarse sampling for correlation")
    private Double coarse;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a tracking mask")
    private Mask track;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a target mask")
    private Mask target;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an include mask")
    private Mask include;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an exclude mask")
    private Mask exclude;

    @ModuleParameter
    @ModuleDescription("the angle stopping criteria")
    private Double angle = 45.0;

    @ModuleParameter
    @ModuleDescription("the step size for tracking")
    private Double step = 1.0;

    @ModuleParameter
    @ModuleDescription("a minima value for tracking (FA for dti and frac for xfib)")
    private double min = 0.075;

    @ModuleParameter
    @ModuleDescription("the mixing weight for orientation updates (0 to 1)")
    private Double mixing = 1.0;

    @ModuleParameter
    @ModuleDescription("use endpoints for waypoint selection")
    private Boolean endpoints = false;

    @ModuleParameter
    @ModuleDescription("include empty curves for seeds that don't track")
    private Boolean empty = false;

    @ModuleParameter
    @ModuleDescription("the interpolation type")
    private KernelInterpolationType interp = KernelInterpolationType.Trilinear;

    @ModuleParameter
    @ModuleDescription("the filter radius in voxels")
    private Integer support = 5;

    @ModuleParameter
    @ModuleDescription("the positional bandwidth in mm")
    private Double hpos = 1.0;

    @ModuleParameter
    @ModuleDescription("a maxima number of compartments (xfib only)")
    private int comps = 3;

    @ModuleParameter
    @ModuleDescription("use log-euclidean estimation (dti only)")
    private Boolean log = false;

    @ModuleParameter
    @ModuleDescription("use randomized prob-fiber selection (xfib only)")
    private Boolean prob = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the estimation type (xfib only)")
    private FibersEstimator.EstimationType estimation = FibersEstimator.EstimationType.Match;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the selection type (xfib only)")
    private FibersEstimator.SelectionType selection = FibersEstimator.SelectionType.Linear;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("a data adaptive threshold (xfib only)")
    private double lambda = 0.99;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the minima volume fraction (xfib only)")
    private Double minval = 0.01;

    @ModuleParameter
    @ModuleDescription("the number of threads")
    private int threads = 4;

    @ModuleOutput
    @ModuleDescription("the output segmentation mask")
    private Mask outputMask;

    @ModuleOutput
    @ModuleDescription("the output tractography")
    private Curves outputCurves;

    public VolumeModelTrackParcellate run()
    {
        Logging.info("started connectivity based parcellation");

        VolumeModelTrackStreamline tracker = new VolumeModelTrackStreamline();
        tracker.input = this.input;
        tracker.model = this.model;
        tracker.trackMask = this.track;
        tracker.stopMask = this.target;
        tracker.includeMask = this.include;
        tracker.excludeMask = this.exclude;
        tracker.angle = this.angle;
        tracker.step = this.step;
        tracker.min = this.min;
        tracker.mixing = this.mixing;
        tracker.interp = this.interp;
        tracker.support = this.support;
        tracker.hpos = this.hpos;
        tracker.comps = this.comps;
        tracker.log = this.log;
        tracker.prob = this.prob;
        tracker.estimation = this.estimation;
        tracker.selection = this.selection;
        tracker.lambda = this.lambda;
        tracker.minval = this.minval;
        tracker.threads = this.threads;

        tracker.empty = true; // this is important!

        Sampling sampling = this.mask.getSampling();

        List<Sample> samples = Lists.newArrayList();
        Vects seeds = new Vects();

        for (Sample sample : sampling)
        {
            if (this.mask.foreground(sample))
            {
                samples.add(sample);
                for (int i = 0; i < this.density; i++)
                {
                    seeds.add(sampling.random(sample));
                }
            }
        }

        Logging.info(String.format("found %d voxels to segment", samples.size()));
        Logging.info(String.format("using %d total seed", seeds.size()));

        Logging.info("tracking");
        tracker.seedVects = seeds;
        Curves curves = tracker.run().output;
        curves.add("segment", VectSource.create1D(0));
        curves.add("sample", VectSource.create1D(0));
        Global.assume(curves.size() == seeds.size(), "invalid tracks");

        Logging.info("computing vector representation");
        List<Curves> sprouts = Lists.newArrayList();
        List<Sample> nsamples = Lists.newArrayList();
        List<Integer> removeCurves = Lists.newArrayList();

        for (int i = 0; i < samples.size(); i++)
        {
            Curves sprout = new Curves();
            for (int j = 0; j < this.density; j++)
            {
                int idx = i * this.density + j;
                Curve curve = curves.get(idx);

                if (curve.size() > 1)
                {
                    int sample = nsamples.size() + 1;
                    curve.setAll("sample", VectSource.create1D(sample));
                    sprout.add(curve);
                }
                else
                {
                    removeCurves.add(idx);
                }
            }
            if (sprout.size() > 0)
            {
                sprouts.add(sprout);
                nsamples.add(samples.get(i));
            }
        }

        samples = nsamples;

        Collections.sort(removeCurves);
        Collections.reverse(removeCurves);
        for (int i : removeCurves)
        {
            curves.remove(i);
        }

        Vects vects = vects(this.rep, sprouts);
        Global.assume(vects.size() == samples.size(), "invalid representation");

        Logging.info(String.format("clustering with %d segments", this.segments));
        VectsClusterKM cluster = new VectsClusterKM();
        cluster.withK(this.segments);
        cluster.withVects(vects);
        int[] labels = cluster.run().getLabels();
        Global.assume(labels.length == samples.size(), "invalid labels");

        Logging.info("preparing output");
        Mask outMask = new Mask(sampling);
        for (int i = 0; i < samples.size(); i++)
        {
            outMask.set(samples.get(i), labels[i]);
        }

        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            int s = (int) Math.round(curve.get("sample", 0).get(0)) - 1;
            int label = labels[s];
            curve.setAll("segment", VectSource.create1D(label));
        }

        Logging.info("finished connectivity based parcellation");

        this.outputMask = outMask;
        this.outputCurves = curves;

        return this;
    }

    private Vects vects(String name, List<Curves> sprouts)
    {
        switch (name)
        {
            case PATH:
                return path(sprouts);
            case DIST:
                return dist(sprouts);
            case SCPT:
                return scpt(sprouts);
            default:
                Logging.error("invalid distance: " + this.rep);
                return null;
        }
    }

    private Vects path(List<Curves> sprouts)
    {
        Logging.info("using path representation");

        Sampling isampling = this.input.getSampling();
        Sampling csampling = this.coarse == null ? isampling : isampling.resample(this.coarse);
        Mask coarseMask = new Mask(csampling);

        Logging.info(String.format("using a coarse mask with %d elements", csampling.size()));

        Vects out = new Vects();

        for (Curves curves : sprouts)
        {
            CurvesDensity denser = new CurvesDensity();
            denser.input = curves;
            denser.reference = coarseMask.copyVolume();
            Volume density = denser.run().output;

            VolumeThreshold thresher = new VolumeThreshold();
            thresher.input = density;
            thresher.threshold = 0.5;
            Mask mask = thresher.run().output;

            out.add(mask.vect());
        }

        return out;
    }

    private Vects dist(List<Curves> sprouts)
    {
        Logging.info("using distance representation");

        Sampling isampling = this.input.getSampling();
        Sampling csampling = this.coarse == null ? isampling : isampling.resample(this.coarse);
        Mask coarseMask = new Mask(csampling);

        Logging.info(String.format("using a coarse mask with %d elements", csampling.size()));

        Vects out = new Vects();

        for (Curves curves : sprouts)
        {
            CurvesDensity denser = new CurvesDensity();
            denser.input = curves;
            denser.reference = coarseMask.copyVolume();
            Volume density = denser.run().output;

            VolumeThreshold thresher = new VolumeThreshold();
            thresher.input = density;
            thresher.threshold = 0.5;
            Mask mask = thresher.run().output;

            MaskDistanceTransform dister = new MaskDistanceTransform();
            dister.input = mask;
            Volume dt = dister.run().output;

            out.add(dt.vect());
        }

        return out;
    }

    private Vects scpt(List<Curves> sprouts)
    {
        Logging.info("using scpt representation");

        Curves subset = new Curves();
        for (Curves curves : sprouts)
        {
            int max = (int) Math.ceil(0.05 * curves.size());
            for (int i = 0; i < max; i++)
            {
                subset.add(curves.get(i));
            }
        }
        CurvesLandmarks landmarker = new CurvesLandmarks();
        landmarker.input = subset;
        landmarker.eps = 2d;
        landmarker.radius = 25d;
        landmarker.num = 2;
        Vects lm = landmarker.getOutput();

        Vects out = new Vects();
        for (Curves curves : sprouts)
        {
            CurvesClosestPointTransform transform = new CurvesClosestPointTransform();
            transform.landmarks = lm;
            transform.input = curves;
            Vects values = transform.run().output;
            VectsStats stats = new VectsStats();
            stats.withInput(values);
            stats.run();

            out.add(stats.mean);
        }

        return out;
    }
}