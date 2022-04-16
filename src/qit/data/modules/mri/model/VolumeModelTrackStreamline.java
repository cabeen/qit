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


package qit.data.modules.mri.model;

import qit.base.Global;
import qit.base.Logging;
import qit.base.Model;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleExpert;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.models.Noddi;
import qit.data.models.Spharm;
import qit.data.models.Tensor;
import qit.data.models.VectModel;
import qit.data.modules.curves.*;
import qit.data.modules.mri.fibers.VolumeFibersProjectVector;
import qit.data.modules.mri.odf.VolumeOdfPeaks;
import qit.data.modules.mri.spharm.VolumeSpharmPeaks;
import qit.data.modules.vects.VectsCreateSphere;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.VectSource;
import qit.data.utils.VectsUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.enums.KernelInterpolationType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.estimation.*;
import qit.data.utils.mri.fields.*;
import qit.data.utils.mri.structs.ModelEstimator;
import qit.data.utils.mri.structs.StreamlineField;
import qit.data.utils.mri.structs.StreamlineTracker;
import qit.math.source.SelectorSource;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.function.BiFunction;

@ModuleDescription("Perform deterministic multi-fiber streamline tractography from a model volume.  This supports tensor, fibers, spharm, and noddi volumes.")
@ModuleCitation("Cabeen, R. P., Bastin, M. E., & Laidlaw, D. H. (2016). Kernel regression estimation of fiber orientation mixtures in diffusion MRI. NeuroImage, 127, 158-172.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeModelTrackStreamline implements Module
{
    @ModuleInput
    @ModuleDescription("the input model volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("seed from vects")
    public Vects seedVects;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a seed mask (one seed per voxel is initiated from this)")
    public Mask seedMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("seed from solids (one seed per object is initiated from this)")
    public Solids seedSolids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an include mask (curves are only included if they touch this, i.e. AND)")
    public Mask includeMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an include solids (curves are only included if they touch this, i.e. AND)")
    public Solids includeSolids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an additional include mask (curves are only included if they touch this, i.e. AND)")
    public Mask includeAddMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an additional include solids (curves are only included if they touch this, i.e. AND)")
    public Solids includeAddSolids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an exclude mask (curves are removed if they touch this mask, i.e. NOT)")
    public Mask excludeMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an exclude solids object (curves are removed if they touch any solid, ie.e NOT)")
    public Solids excludeSolids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a trap mask (tracking terminates when leaving this mask)")
    public Mask trapMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a stop mask (tracking terminates when reaching this mask)")
    public Mask stopMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a stop solids object  (tracking terminates when reaching any solid)")
    public Solids stopSolids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an endpoint mask (curves are only retained if they end inside this)")
    public Mask endMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an endpoint solids object (curves are only retained if they end inside this)")
    public Solids endSolids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a tracking mask (tracking is stopped if a curve exits this mask)")
    public Mask trackMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a tracking solids (tracking is stopped if a curve exits solids)")
    public Solids trackSolids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a containment mask (a minimum fraction of arclength must be inside this mask)")
    public Mask containMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a connection mask (tracks will be constrained to connect distinct labels)")
    public Mask connectMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("specify the spherical points for processing the odf (the number of vectors should match the dimensionality of the input volume)")
    public Vects odfPoints;

    @ModuleParameter
    @ModuleDescription("increase or decrease the total number of samples by a given factor (regardless of whether using vects, mask, or solids)")
    public Double samplesFactor = 1.0;

    @ModuleParameter
    @ModuleDescription("the number of samples per voxel when using mask seeding")
    public Integer samplesMask = 1;

    @ModuleParameter
    @ModuleDescription("the number of samples per object when using solid seeding")
    public Integer samplesSolids = 5000;

    @ModuleParameter
    @ModuleDescription("a minimum cutoff value for tracking (depend on the voxel model, e.g. FA for DTI, frac for Fibers, etc.)")
    public double min = 0.075;

    @ModuleParameter
    @ModuleDescription("the angle stopping criteria (maximum change in orientation per step)")
    public Double angle = 45.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a fixed amount of orientation dispersion during tracking (e.g. 0.1)")
    public Double disperse = 0.0;

    @ModuleParameter
    @ModuleDescription("the step size for tracking")
    public Double step = 1.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum streamline length")
    public Double minlen = 0.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maximum streamline length")
    public Double maxlen = 1000000d;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a maximum number of seeds")
    public Integer maxseeds = 2000000;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a maximum number of tracks")
    public Integer maxtracks = 2000000;

    @ModuleParameter
    @ModuleDescription("the interpolation type")
    public KernelInterpolationType interp = KernelInterpolationType.Nearest;

    @ModuleParameter
    @ModuleDescription("use monodirectional seeding (default is bidirectional), e.g. for seeding from a subcortical structure")
    public Boolean mono = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use vector field integration (the default is to use axes, which have not preferred forward/back direction)")
    public Boolean vector = false;

    @ModuleParameter
    @ModuleDescription("use probabilistic sampling and selection")
    public Boolean prob = false;

    @ModuleParameter
    @ModuleDescription("follow the maximum probability direction (only used when the prob flag is enabled)")
    public Boolean probMax = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("raise the sample probability to the given power (to disable, leave at one)")
    public Double probPower = 1.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the prior gain for angle changes (beta * change / threshold) (t disable, set to zero)")
    public Double probAngle = 5.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the number of ODF points to sample for probabilistic sampling (or you can provide specific odfPoints)")
    public Integer probPoints = 300;

    @ModuleParameter
    @ModuleDescription("find connections in the end mask")
    public Boolean endConnect = false;

    @ModuleInput
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("use a force field")
    public Volume force;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use the hybrid approach")
    public boolean hybrid = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a seed multiplication factor for the first stage of hybrid tracking")
    public Double hybridSamplesFactor = 1.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify an minimum for the first stage of hybrid tracking")
    public Double hybridMin = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify an angle for the first stage of hybrid tracking")
    public Double hybridAngle = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify an amount of dispersion to in the first stage of hybrid tracking")
    public Double hybridDisperse = 0.1;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify an pre-smoothing bandwidth (before projection)")
    public Double hybridPresmooth = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify an angle for hybrid projection")
    public Double hybridProjAngle = 45.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a minimum norm for hybrid projection")
    public Double hybridProjNorm = 0.01;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a minimum compartment fraction for hybrid projection")
    public Double hybridProjFrac = 0.025;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a minimum total fraction for hybrid projection")
    public Double hybridProjFsum = 0.05;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify an post-smoothing bandwidth (after projection)")
    public Double hybridPostsmooth = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("the interpolation type for the first stage of hybrid tracking")
    public KernelInterpolationType hybridInterp = KernelInterpolationType.Nearest;

    @ModuleInput
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("use the given peaks (otherwise they will be computed from the input)")
    public Volume hybridPeaks;

    @ModuleInput
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("use the given connect mask during the initial tracking phase (but not the secondary one)")
    public Mask hybridConnectMask;

    @ModuleInput
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("use the given trap mask during the initial tracking phase (but not the secondary one)")
    public Mask hybridTrapMask;

    @ModuleInput
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("use the given exclude mask during the initial tracking phase (but not the secondary one)")
    public Mask hybridExcludeMask;

    @ModuleInput
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("use the given stop mask during the initial tracking phase (but not the secondary one)")
    public Mask hybridStopMask;

    @ModuleInput
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("use the given track mask during the initial tracking phase (but not the secondary one)")
    public Mask hybridTrackMask;

    @ModuleInput
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("use the given curves (and skip the first stage of hybrid tracking)")
    public Curves hybridCurves;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the prior gain forces (larger values have smoother effects and zero is hard selection)")
    public Double gforce = 0.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("a model name (default will try to detect it)")
    public String model = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the number of threads")
    public int threads = 3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("a maximum value for tracking (FA for dti, frac for xfib/spharm, ficvf for noddi)")
    public Double max = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the mixing weight for orientation updates (0 to 1)")
    public Double mixing = 1.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("an arclength containment threshold")
    public Double arclen = 0.8;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("only allow tracking to travel a given reach distance from the seed")
    public Double reach = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use fourth-order Runge-Kutta integration (default is Euler)")
    public boolean rk = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("binarize masks (ignore multiple labels in include and other masks)")
    public Boolean binarize = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("include empty curves for seeds that don't track")
    public Boolean empty = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("use log-euclidean estimation (dti only)")
    public Boolean log = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("a maximum number of compartments (xfib only)")
    public int comps = 3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the filter radius in voxels of the interpolation kernel")
    public Integer support = 3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the positional interpolation kernel bandwidth in mm")
    public Double hpos = 1.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleExpert
    @ModuleDescription("the model interpolation kernel bandwidth (units based on model parameters)")
    public Double hval = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleExpert
    @ModuleDescription("the baseline interpolation kernel bandwidth (units based on baseline signal)")
    public Double hsig = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the estimation type (xfib only)")
    public FibersEstimator.EstimationType estimation = FibersEstimator.EstimationType.Match;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the selection type (xfib only)")
    public FibersEstimator.SelectionType selection = FibersEstimator.SelectionType.Linear;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("a data adaptive threshold (xfib only)")
    public double lambda = 0.99;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the minima volume fraction (xfib only)")
    public Double minval = 0.01;

    @ModuleParameter
    @ModuleDescription("print progress messages")
    public boolean quiet = false;

    @ModuleOutput
    @ModuleDescription("the output tractography curves")
    public Curves output;

    private static class TrackParam
    {
        double min;
        double angle;
        double disperse;
        int comps;
        boolean vector;
        boolean prob;
        KernelInterpolationType interp;
        Mask connect;
        Mask trap;
        Mask track;
        Mask exclude;
        Mask stop;
    }

    public VolumeModelTrackStreamline run()
    {
        TrackParam params = new TrackParam();
        params.disperse = this.disperse;
        params.comps = this.comps;
        params.interp = this.interp;
        params.angle = this.angle;
        params.min = this.min;
        params.vector = this.vector;
        params.prob = this.prob;
        params.connect = this.connectMask;
        params.trap = this.trapMask;
        params.exclude = this.excludeMask;
        params.stop = this.stopMask;
        params.track = this.trackMask;

        if (!this.hybrid)
        {
            Logging.info(!this.quiet, "started streamline tracking");

            Logging.info(!this.quiet, "seeding");
            Vects seeds = this.multiply(this.samplesFactor, this.seeds());

            Logging.info(!this.quiet, "tracking");
            this.output = this.tracker(this.input, seeds, params);
        }
        else
        {
            Logging.info(!this.quiet, "started hybrid streamline tracking");
            Volume peaks = this.hybridPeaks;

            if (peaks == null)
            {
                ModelType detected = ModelUtils.select(this.input.getModel(), this.model);

                if (this.odfPoints != null && this.odfPoints.size() == this.input.getDim())
                {
                    VolumeOdfPeaks peaker = new VolumeOdfPeaks();
                    peaker.input = this.input;
                    peaker.points = this.odfPoints;
                    peaker.thresh = 0.25 * this.min;
                    peaker.mask = this.trackMask;
                    peaker.comps = 4;
                    peaks = peaker.run().output;
                }
                else if (ModelType.Spharm.equals(detected))
                {
                    VolumeSpharmPeaks peaker = new VolumeSpharmPeaks();
                    peaker.input = this.input;
                    peaker.thresh = 0.25 * this.min;
                    peaker.mask = this.trackMask;
                    peaker.comps = 4;
                    peaks = peaker.run().output;
                }
                else if (ModelType.Fibers.equals(detected))
                {
                    peaks = this.input;
                }
            }

            Global.assume(peaks != null, "hybrid tracking requires multi-fiber input");

            Logging.info(!this.quiet, "seeding");
            Vects seeds = this.seeds();

            Curves curves = this.hybridCurves;

            if (curves == null)
            {
                Logging.info(!this.quiet, "pre-tracking");
                params.disperse = this.hybridDisperse != null ? this.hybridDisperse : this.disperse;
                params.interp = this.hybridInterp != null ? this.hybridInterp : this.interp;
                params.angle = this.hybridAngle != null ? this.hybridAngle : this.angle;
                params.min = this.hybridMin != null ? this.hybridMin : this.min;
                params.prob = true;
                params.connect = this.hybridConnectMask != null ? this.hybridConnectMask : this.connectMask;
                params.trap = this.hybridTrapMask != null ? this.hybridTrapMask : this.trapMask;
                params.exclude = this.hybridExcludeMask != null ? this.hybridExcludeMask : this.excludeMask;
                params.stop = this.hybridStopMask != null ? this.hybridStopMask : this.stopMask;
                params.track = this.hybridTrackMask != null ? this.hybridTrackMask : this.trackMask;
                curves = this.tracker(this.input, this.multiply(this.hybridSamplesFactor, seeds), params);
            }

            try
            {
                curves.write("/tmp/curves.vtk.gz");
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            Logging.info(!this.quiet, "computing orientation map");
            CurvesOrientationMap mapper = new CurvesOrientationMap();
            mapper.input = curves;
            mapper.refvolume = this.input;
            mapper.vector = true;
            mapper.orient = true;
            mapper.smooth = true;
            mapper.sigma = VolumeModelTrackStreamline.this.hybridPresmooth;
            Volume mapped = mapper.run().output;

            Logging.info(!this.quiet, "computing tom mask");
            VolumeThreshold masker = new VolumeThreshold();
            masker.input = mapped;
            masker.magnitude = true;
            masker.threshold = 1e-3;
            Mask mask = masker.run().output;

            Logging.info(!this.quiet, "projecting fibers");
            VolumeFibersProjectVector projecter = new VolumeFibersProjectVector();
            projecter.input = peaks;
            projecter.reference = mapped;
            projecter.mask = mask;
            projecter.angle = this.hybridProjAngle;
            projecter.norm = this.hybridProjNorm;
            projecter.frac = this.hybridProjFrac;
            projecter.fsum = this.hybridProjFsum;
            projecter.threads = this.threads;
            Volume projected = projecter.run().output;

            if (this.hybridPostsmooth != null && MathUtils.nonzero(this.hybridPostsmooth))
            {
                Logging.info(!this.quiet, "computing projection mask");
                VolumeThreshold pmasker = new VolumeThreshold();
                pmasker.input = mapped;
                pmasker.magnitude = true;
                pmasker.threshold = 1e-3;
                Mask pmask = pmasker.run().output;

                Logging.info(!this.quiet, "post-smoothing vectors");
                VolumeFilterGaussian smoother = new VolumeFilterGaussian();
                smoother.input = projected;
                smoother.mask = pmask;
                smoother.sigma = this.hybridPostsmooth;
                smoother.support = 3;
                smoother.threads = this.threads;
                projected = smoother.run().output;
            }

            Logging.info(!this.quiet, "post-tracking");
            params.disperse = this.disperse;
            params.interp = this.interp;
            params.angle = this.angle;
            params.min = this.min;
            params.vector = true;
            params.prob = false;
            params.connect = this.connectMask;
            params.trap = this.trapMask;
            params.exclude = this.excludeMask;
            params.stop = this.stopMask;
            params.track = this.trackMask;

            this.output = this.tracker(projected, this.multiply(this.samplesFactor, seeds), params);
        }

        Logging.info(!this.quiet, String.format("final curve count: %d", this.output.size()));

        Logging.info(!this.quiet, "finished streamline tracking");

        return this;
    }

    private Mask createSeedMask()
    {
        if (this.hybridCurves != null)
        {
            Logging.info("extract seed mask from hybrid curves");
            CurvesMask masker = new CurvesMask();
            masker.input = this.hybridCurves;
            masker.refvolume = this.input;
            return masker.run().output;
        }
        else
        {
            Logging.info("extract seed mask for whole brain");

            ModelType detected = ModelUtils.select(this.input.getModel(), this.model);

            VolumeModelFeature featurer = new VolumeModelFeature();
            featurer.input = this.input;

            if (ModelType.Tensor.equals(detected))
            {
                featurer.feature = Tensor.FEATURES_FA;
            }
            else if (ModelType.Fibers.equals(detected))
            {
                featurer.feature = Fibers.FRAC;
            }
            else if (ModelType.Spharm.equals(detected))
            {
                featurer.feature = Spharm.MAX;
            }
            else if (ModelType.Noddi.equals(detected))
            {
                featurer.feature = Noddi.FICVF;
            }
            else if (this.input.getDim() == 3)
            {
                this.input.setModel(ModelType.Vect);
                featurer.feature = VectModel.MAG;
            }
            else if (this.odfPoints != null && this.odfPoints.size() == this.input.getDim())
            {
                this.input.setModel(ModelType.Vect);
                featurer.feature = VectModel.AMP;
                featurer.feature = null; // use the sum
            }
            else
            {
                throw new RuntimeException("model not available for tracking: " + this.model);
            }

            Volume feature = featurer.run().output;

            Mask mask = new VolumeThreshold()
            {{
                this.input = feature;
                this.threshold = VolumeModelTrackStreamline.this.min;
            }}.run().output;

            return mask;
        }
    }

    private StreamlineField field(Volume volume, TrackParam params)
    {
        ModelType detected = ModelUtils.select(volume.getModel(), this.model);
        BiFunction<Model, ModelEstimator, VolumeKernelModelEstimator> create = (m, e) ->
        {
            VolumeKernelModelEstimator vestimator = new VolumeKernelModelEstimator(m);
            vestimator.estimator = e;
            vestimator.volume = volume;
            vestimator.interp = params.interp;
            vestimator.hpos = this.hpos;
            vestimator.support = this.support;

            return vestimator;
        };

        if (this.odfPoints != null && this.odfPoints.size() == volume.getDim())
        {
            VolumeKernelModelEstimator vestimator = create.apply(new VectModel(volume.getDim()), new VectEstimator());

            if (params.prob)
            {
                Logging.info("using full ODF field");
                return new OdfField(vestimator, this.odfPoints);
            }
            else
            {
                Logging.info("using ODF peak field");
                return new OdfPeakField(vestimator, this.odfPoints);
            }
        }
        else if (ModelType.Tensor.equals(detected))
        {
            Logging.info("using tensor peak field");

            TensorEstimator estimator = new TensorEstimator();
            estimator.log = this.log;
            VolumeKernelModelEstimator vestimator = create.apply(new Tensor(), estimator);

            return new TensorPeakField(vestimator);
        }
        else if (ModelType.Fibers.equals(detected))
        {
            Logging.info("using multi-fiber field");

            FibersEstimator estimator = new FibersEstimator();
            estimator.estimation = this.estimation;
            estimator.selection = this.selection;
            estimator.lambda = this.lambda;
            estimator.maxcomps = params.comps;
            estimator.minfrac = this.minval;

            VolumeKernelModelEstimator vestimator = create.apply(new Fibers(params.comps), estimator);
            vestimator.estimator = estimator;
            vestimator.hpos = this.hpos;
            vestimator.hval = this.hval;
            vestimator.hsig = this.hsig;

            return new FibersField(vestimator);
        }
        else if (ModelType.Spharm.equals(detected))
        {
            SpharmEstimator estimator = new SpharmEstimator();
            VolumeKernelModelEstimator vestimator = create.apply(new Spharm(volume.getDim()), estimator);

            if (params.prob)
            {
                Logging.info("using full spharm field");

                Vects mypoints = this.odfPoints == null ? VectsCreateSphere.odf(this.probPoints) : this.odfPoints;
                Logging.infosub("using %d ODF points", mypoints.size());
                return new SpharmField(vestimator, mypoints, volume.getDim());
            }
            else
            {
                Logging.info("using spharm peak field");
                return new SpharmPeakField(vestimator, volume.getDim());
            }
        }
        else if (ModelType.Noddi.equals(detected))
        {
            Logging.info("using noddi peak field");
            return new NoddiField(create.apply(new Noddi(), new NoddiEstimator()));
        }
        else if (volume.getDim() == 3)
        {
            if (params.vector)
            {
                Logging.info("using directional vector field");
                return new VectField(create.apply(new VectModel(3), new VectEstimator()));
            }
            else
            {
                Logging.info("using axial vector field");
                return new VectField(create.apply(new VectModel(3), new LineEstimator()));
            }
        }

        throw new RuntimeException("model not available for tracking: " + this.model);
    }

    private Vects seeds()
    {
        Vects seeds = new Vects();

        if (this.seedVects != null)
        {
            seeds.addAll(this.seedVects);
        }

        if (this.seedSolids != null)
        {
            seeds.addAll(this.seedSolids.sample(this.samplesSolids));
        }

        if (this.seedMask != null)
        {
            seeds.addAll(VectsUtils.seed(this.seedMask, this.samplesMask, null));
        }

        if (seeds.size() == 0)
        {
            Logging.info(!this.quiet, "creating seed mask from volume");
            seeds.addAll(VectsUtils.seed(this.createSeedMask(), this.samplesMask, null));
        }

        return seeds;
    }

    private Vects multiply(double factor, Vects seeds)
    {
        Vects out = seeds;

        if (factor > 0 && !MathUtils.unit(factor))
        {
            int count = (int) (factor * out.size());
            Logging.info(!this.quiet, String.format("using factor %g to change seed count from %d to %d", factor, out.size(), count));

            if (count < out.size())
            {
                out = VectsUtils.subsample(out, count);
            }
            else
            {
                double jitter = this.input.getSampling().deltaMax();

                out = seeds.copy();
                while (out.size() < count)
                {
                    Vect seed = seeds.get(Global.RANDOM.nextInt(seeds.size()));
                    Vect jseed = seed.plus(VectSource.random(jitter, 3));
                    out.add(jseed);
                }
            }
        }

        if (this.maxseeds != null)
        {
            out = VectsUtils.subsample(out, this.maxseeds);
        }

        return out;
    }

    private Curves select(Curves curves)
    {
        if (this.containMask != null)
        {
            CurvesMaskSelect select = new CurvesMaskSelect();
            select.input = curves;
            select.contain = this.containMask;
            select.thresh = this.arclen;
            curves = select.run().output;
        }

        if (this.includeMask != null)
        {
            CurvesMaskSelect select = new CurvesMaskSelect();
            select.input = curves;
            select.endpoints = false;
            select.include = this.includeMask;
            select.binarize = this.binarize;
            select.skip = true;
            curves = select.run().output;
        }

        if (this.includeSolids != null && this.includeSolids.size() > 0)
        {
            CurvesSelect select = new CurvesSelect();
            select.input = curves;
            select.endpoints = false;
            select.solids = this.includeSolids;
            select.or = true;
            curves = select.run().output;
        }

        if (this.includeAddMask != null)
        {
            CurvesMaskSelect select = new CurvesMaskSelect();
            select.input = curves;
            select.endpoints = false;
            select.include = this.includeAddMask;
            select.binarize = this.binarize;
            select.skip = true;
            curves = select.run().output;
        }

        if (this.includeAddSolids != null && this.includeAddSolids.size() > 0)
        {
            CurvesSelect select = new CurvesSelect();
            select.input = curves;
            select.endpoints = false;
            select.solids = this.includeAddSolids;
            select.or = true;
            curves = select.run().output;
        }

        if (this.endMask != null)
        {
            CurvesMaskSelect select = new CurvesMaskSelect();
            select.input = curves;
            select.endpoints = true;
            select.include = this.endMask;
            select.binarize = this.binarize;
            select.connect = this.endConnect;
            select.skip = true;
            curves = select.run().output;
        }

        if (this.endSolids != null)
        {
            CurvesSelect select = new CurvesSelect();
            select.input = curves;
            select.endpoints = true;
            select.solids = this.endSolids;
            select.or = this.binarize;
            curves = select.run().output;
        }

        if (this.excludeSolids != null)
        {
            CurvesSelect select = new CurvesSelect();
            select.input = curves;
            select.endpoints = false;
            select.solids = this.excludeSolids;
            select.exclude = true;
            curves = select.run().output;
        }

        if (this.maxtracks != null && this.maxtracks < curves.size())
        {
            CurvesReduce reduce = new CurvesReduce();
            reduce.input = curves;
            reduce.count = this.maxtracks;
            curves = reduce.run().output;
        }

        return curves;
    }

    public Curves tracker(Volume volume, Vects seeds, TrackParam params)
    {
        StreamlineTracker tracker = new StreamlineTracker();
        tracker.field = field(volume, params);
        tracker.low.put(tracker.field.getAttr(), params.min);
        tracker.seeds = seeds;
        tracker.disperse = params.disperse;
        tracker.angle = params.angle;
        tracker.vector = params.vector;
        tracker.prob = params.prob;
        tracker.step = this.step;
        tracker.minlen = this.minlen;
        tracker.maxlen = this.maxlen;
        tracker.reach = this.reach;
        tracker.mixing = this.mixing;
        tracker.empty = this.empty;
        tracker.rk = this.rk;
        tracker.mono = this.mono;
        tracker.probMax = this.probMax;
        tracker.probAngle = this.probAngle;
        tracker.probPower = this.probPower;
        tracker.threads = this.threads;
        tracker.chatty = !this.quiet;
        tracker.filter = (in) -> this.select(in);

        if (params.trap != null)
        {
            tracker.trap = SelectorSource.mask(params.trap);
        }

        if (params.track != null)
        {
            tracker.track = SelectorSource.mask(params.track);
        }

        if (params.stop != null)
        {
            tracker.stop = SelectorSource.mask(params.stop);
        }

        if (this.force != null)
        {
            tracker.force = VolumeUtils.interp(InterpolationType.Trilinear, this.force);
            tracker.gforce = this.gforce;
        }

        if (this.trackMask != null)
        {
            tracker.track = SelectorSource.mask(this.trackMask);
        }

        if (this.trackSolids != null)
        {
            tracker.track = SelectorSource.or(tracker.track, this.trackSolids);
        }

        if (this.stopMask != null)
        {
            tracker.stop = SelectorSource.mask(this.stopMask);
        }

        if (this.stopSolids != null)
        {
            tracker.stop = SelectorSource.or(tracker.stop, this.stopSolids);
        }

        if (this.max != null)
        {
            tracker.high.put(tracker.field.getAttr(), this.max);
        }

        Curves out = tracker.run().output;

        if (params.exclude != null)
        {
            CurvesMaskSelect select = new CurvesMaskSelect();
            select.input = out;
            select.endpoints = false;
            select.exclude = params.exclude;
            select.thresh = this.arclen;
            out = select.run().output;
        }

        if (params.connect != null)
        {
            out = CurvesConnectRegions.apply(out, params.connect);
        }

        return out;
    }
}