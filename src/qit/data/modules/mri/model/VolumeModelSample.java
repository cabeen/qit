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

import qit.base.Logging;
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
import qit.data.datasets.Mesh;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.Spharm;
import qit.data.modules.vects.VectsJitter;
import qit.data.utils.VectsUtils;
import qit.data.utils.enums.KernelInterpolationType;
import qit.data.utils.mri.estimation.SpharmEstimator;
import qit.data.utils.vects.cluster.VectsClusterDPM;
import qit.data.models.Fibers;
import qit.data.models.Noddi;
import qit.data.models.Tensor;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.estimation.FibersEstimator;
import qit.data.utils.mri.estimation.NoddiEstimator;
import qit.data.utils.mri.estimation.TensorEstimator;
import qit.data.utils.mri.estimation.VolumeKernelModelEstimator;

@ModuleDescription("Sample model parameters based on a variety of possible data objects")
@ModuleCitation("Cabeen, R. P., Bastin, M. E., & Laidlaw, D. H. (2016). Kernel regression estimation of fiber orientation mixtures in mri MRI. NeuroImage, 127, 158-172.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeModelSample implements Module
{
    @ModuleInput
    @ModuleDescription("the input model volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("vects storing the position where models should be sampled")
    public Vects vects;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("solids storing the position where models should be sampled")
    public Solids solids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("mask storing the position where models should be sampled")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("curves storing the position where models should be sampled")
    public Curves curves;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("mesh storing the position where models should be sampled")
    public Mesh mesh;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a model type (if you select None, the code will try to detect it)")
    public ModelType model = ModelType.Vect;
    
    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("jitter the samples by a given amount")
    public Double jitter = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("cull the samples at a given threshold ditance")
    public Double cull = null;

    @ModuleParameter
    @ModuleDescription("a multiplier of the number of samples taken (sometimes only makes sense when jittering)")
    public Integer multiplier = 1;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a maximum number of samples (the excess is randomly selected)")
    public Integer limit = null;

    @ModuleParameter
    @ModuleDescription("samples should store only model parameters (otherwise the position is prepended)")
    public boolean param = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the interpolation type")
    public KernelInterpolationType interp = KernelInterpolationType.Trilinear;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the interpolation filter radius in voxels")
    public Integer support = 5;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the positional interpolation bandwidth in mm")
    public Double hpos = 1.5;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("a maxima number of compartments (xfib only)")
    public int comps = 3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use log-euclidean estimation (dti only)")
    public Boolean log = false;

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
    @ModuleAdvanced
    @ModuleDescription("the number of threads")
    public int threads = 4;

    @ModuleOutput
    @ModuleDescription("the output sampled vects")
    public Vects output;

    public VolumeModelSample run()
    {
        Vects samples = new Vects();

        if (this.vects != null)
        {
            for (int i = 0; i < this.multiplier; i++)
            {
                samples.addAll(this.vects);
            }
        }

        if (this.solids != null)
        {
            samples.addAll(this.solids.sample(this.multiplier));
        }

        if (this.mesh != null)
        {
            for (int i = 0; i < this.multiplier; i++)
            {
                samples.addAll(this.mesh.vattr.getAll(Mesh.COORD));
            }
        }

        if (this.curves != null)
        {
            for (int i = 0; i < this.multiplier; i++)
            {
                for (Curves.Curve curve : this.curves)
                {
                    samples.addAll(curve.getAll(Curves.COORD));
                }
            }
        }

        if (this.mask != null)
        {
            Sampling sampling = this.mask.getSampling();
            for (Sample sample : this.mask.getSampling())
            {
                if (this.mask.valid(sample, this.mask))
                {
                    for (int i = 0; i < this.multiplier; i++)
                    {
                        if (this.jitter != null && this.jitter < 0)
                        {
                            samples.add(sampling.random(sample));

                        }
                        else
                        {
                            samples.add(sampling.world(sample));
                        }
                    }
                }
            }
        }

        VolumeKernelModelEstimator vestimator = null;

        ModelType m = ModelUtils.select(this.input.getModel(), this.model);
        if (ModelType.Tensor.equals(m))
        {
            TensorEstimator estimator = new TensorEstimator();
            estimator.log = this.log;
            vestimator = new VolumeKernelModelEstimator(new Tensor());
            vestimator.estimator = estimator;
        }
        else if (ModelType.Fibers.equals(m))
        {
            FibersEstimator estimator = new FibersEstimator();
            estimator.estimation = this.estimation;
            estimator.selection = this.selection;
            estimator.lambda = this.lambda;
            estimator.maxcomps = this.comps;
            estimator.minfrac = this.minval;
            vestimator = new VolumeKernelModelEstimator(new Fibers(this.comps));
            vestimator.estimator = estimator;
        }
        else if (ModelType.Noddi.equals(m))
        {
            NoddiEstimator estimator = new NoddiEstimator();
            vestimator = new VolumeKernelModelEstimator(new Noddi());
            vestimator.estimator = estimator;
        }
        else if (ModelType.Spharm.equals(m))
        {
            SpharmEstimator estimator = new SpharmEstimator();
            vestimator = new VolumeKernelModelEstimator(new Spharm(this.input.getDim()));
            vestimator.estimator = estimator;
        }
        else
        {
            Logging.error("model not available for tracking: " + this.model);
        }

        vestimator.volume = this.input;
        vestimator.interp = this.interp;
        vestimator.hpos = this.hpos;
        vestimator.support = this.support;

        Logging.info(String.format("starting with %d points", samples.size()));
        if (this.jitter != null && this.jitter > 0)
        {
            Logging.info("jittering");
            VectsJitter jitterer = new VectsJitter();
            jitterer.input = samples;
            jitterer.std = this.jitter;
            samples = jitterer.run().output;
        }

        if (this.cull != null)
        {
            Logging.info("culling");

            VectsClusterDPM cull = new VectsClusterDPM();
            cull.withK(1);
            cull.withVects(samples);
            cull.withLambda(this.cull);

            if (this.limit != null)
            {
                cull.withMax(this.limit);
            }

            samples = cull.run().getCenters();
        }

        if (this.limit != null)
        {
            Logging.info(String.format("reduced count to %d points", this.limit));
            samples = VectsUtils.subsample(samples, this.limit);
        }

        Vects out = new Vects();

        for (Vect sample : samples)
        {
            Vect model = vestimator.estimate(sample);
            model = this.param ? model : sample.cat(model);

            out.add(model);
        }

        Logging.info("finished sampling models");

        this.output = out;

        return this;
    }
}
