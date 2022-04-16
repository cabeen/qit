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


package qit.data.modules.mri.spharm;

import qit.base.Logging;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.Spharm;
import qit.data.modules.mri.gradients.GradientsTransform;
import qit.data.modules.vects.VectsCreateSphere;
import qit.data.modules.vects.VectsCreateSphereLookup;
import qit.data.source.VectsSource;
import qit.data.utils.mri.fitting.FitOdfDampedRichardsonLucy;
import qit.data.utils.mri.fitting.FitOdfRichardsonLucy;
import qit.data.utils.mri.fitting.FitSpharmCSD;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Fit a spherical harmonic model to a diffusion volume.")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Dell'Acqua, F., Scifo, P., Rizzo, G., Catani, M., Simmons, A., Scotti, G., Fazio, F. (2010). A modified damped Richardson-Lucy algorithm to reduce isotropic background effects in spherical deconvolution. Neuroimage, 49(2), 1446-1458. ")
@ModuleUnlisted
public class VolumeSpharmFit implements Module
{
    public enum VolumeSpharmFitMode { RLD, CSD };

    @ModuleInput
    @ModuleDescription("input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the gradients")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the mask")
    public Mask mask;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify rounding factor for the gradients")
    public Integer round = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a subset of gradient shells to include (comma-separated list of b-values)")
    public String shells = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a subset of gradients to include (comma-separated list of indices starting from zero)")
    public String which = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a subset of gradients to exclude (comma-separated list of indices starting from zero)")
    public String exclude = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maximum number of points for RL deconvolution")
    public Integer points = null;

    @ModuleParameter
    @ModuleDescription("specify an estimation method")
    public VolumeSpharmFitMode method = VolumeSpharmFitMode.RLD;

    @ModuleParameter
    @ModuleDescription("the kernel diffusivity for RLD")
    public double alpha = FitOdfRichardsonLucy.DEFAULT_ALPHA;

    @ModuleParameter
    @ModuleDescription("the kernel diffusivity for RLD")
    public double beta = FitOdfRichardsonLucy.DEFAULT_BETA;

    @ModuleParameter
    @ModuleDescription("the damping threshold parameter for RLD")
    public double eta = FitOdfDampedRichardsonLucy.DEFAULT_ETA;

    @ModuleParameter
    @ModuleDescription("the damping profile parameter for RLD")
    public double nu = FitOdfDampedRichardsonLucy.DEFAULT_NU;

    @ModuleParameter
    @ModuleDescription("the number of iterations for RLD")
    public int rlditers = FitOdfRichardsonLucy.DEFAULT_RLDITERS;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify the maximum spherical harmonic order")
    public Integer order = 8;

    @ModuleParameter
    @ModuleDescription("the baseline fiber response for CSD")
    public double baseline = FitSpharmCSD.DEFAULT_BASELINE;

    @ModuleParameter
    @ModuleDescription("the axial fiber response for CSD")
    public double axial = FitSpharmCSD.DEFAULT_AXIAL;

    @ModuleParameter
    @ModuleDescription("the radial fiber response for CSD")
    public double radial = FitSpharmCSD.DEFAULT_RADIAL;

    @ModuleParameter
    @ModuleDescription("the tau parameter for CSD")
    public double tau = FitSpharmCSD.DEFAULT_TAU;

    @ModuleParameter
    @ModuleDescription("the lambda parameter for CSD")
    public double lambda = FitSpharmCSD.DEFAULT_LAMBDA;

    @ModuleParameter
    @ModuleDescription("the number of iterations for CSD")
    public int csditers = FitSpharmCSD.DEFAULT_ITERS;

    @ModuleParameter
    @ModuleDescription("do not normalize the signal for CSD")
    public boolean nonorm = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the number of threads to use")
    public int threads = 1;

    @ModuleParameter
    @ModuleDescription("use fine-grained multi-threading")
    public boolean columns = false;

    @ModuleOutput
    @ModuleDescription("output spharm volume")
    public Volume output;

    @Override
    public VolumeSpharmFit run()
    {
        Supplier<VectFunction> create = () ->
        {
            Gradients grads = VolumeSpharmFit.this.gradients.copy();

            boolean subset = false;
            subset |= this.round != null;
            subset |= this.shells != null;
            subset |= this.which != null;
            subset |= this.exclude != null;

            VectFunction subsetter = null;
            if (subset)
            {
                Pair<Gradients, VectFunction> pair = GradientsTransform.roundit(this.gradients, this.round).subset(this.shells, this.which, this.exclude);

                grads = pair.a;
                subsetter = pair.b;
            }

            VectFunction fitter = null;
            if (VolumeSpharmFit.this.method.equals(VolumeSpharmFitMode.RLD))
            {
                final Vects fdirs = this.points != null && this.points > 0 ? VectsCreateSphere.odf(this.points) : new VectsCreateSphereLookup().run().output;
                FitOdfRichardsonLucy fit = new FitOdfRichardsonLucy();
                fit.alpha = VolumeSpharmFit.this.alpha;
                fit.beta = VolumeSpharmFit.this.beta;
                fit.rlditers = VolumeSpharmFit.this.rlditers;
                final Matrix transform = Spharm.bmatrix(VolumeSpharmFit.this.order, fdirs).inv();

                fitter = fit.fitter(grads, fdirs).compose(new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        output.set(transform.times(input));
                    }
                }.init(transform.cols(), transform.rows()));
            }
            else if (VolumeSpharmFit.this.method.equals(VolumeSpharmFitMode.CSD))
            {
                FitSpharmCSD fit = new FitSpharmCSD();
                fit.gradients = grads;
                fit.order = this.order;
                fit.iters = this.csditers;
                fit.tau = this.tau;
                fit.lambda = this.lambda;
                fit.baseline = this.baseline;
                fit.axial = this.axial;
                fit.radial = this.radial;
                fit.normalize = !this.nonorm;
                fitter = fit.get();
            }
            else
            {
                Logging.error("invalid method: " + VolumeSpharmFit.this.method);
            }

            if (subsetter != null)
            {
                fitter = subsetter.compose(fitter);
            }

            return fitter;
        };

        this.output = new VolumeFunction(create.get()).withInput(this.input).withMask(this.mask).withThreads(this.threads).withSlice(!this.columns).run().setModel(ModelType.Spharm);

        return this;
    }
}
