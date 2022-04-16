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


package qit.data.modules.mri.bitensor;

import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleExpert;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Volume;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.fitting.FitBiTensorSimplexNLLS;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Fit a bi-tensor volume to a diffusion-weighted MRI.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeBiTensorFit implements Module
{
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
    @ModuleDescription("specify an estimation method")
    public FitBiTensorSimplexNLLS.BiTensorFitType method = FitBiTensorSimplexNLLS.BiTensorFitType.Isotropic;

    @ModuleParameter
    @ModuleDescription("specify a cost function for non-linear fitting")
    public CostType cost = CostType.SE;

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
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the maximum fluid diffusivity")
    public double fluidDiffMin = FitBiTensorSimplexNLLS.DEFAULT_MIN_DIFF;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the maximum fluid diffusivity")
    public double fluidDiffMax = FitBiTensorSimplexNLLS.DEFAULT_MAX_DIFF;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the tissue scaling factor")
    public double scaleTissue = FitBiTensorSimplexNLLS.DEFAULT_TISSUE_SCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the fluid scaling factor")
    public double scaleFluid = FitBiTensorSimplexNLLS.DEFAULT_FLUID_SCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the volume fraction scaling factor")
    public double scaleFrac = FitBiTensorSimplexNLLS.DEFAULT_FRAC_SCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the directional scaling factor (radians)")
    public double scaleAngle = FitBiTensorSimplexNLLS.DEFAULT_ANGLE_SCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the optimization parameter for the beginning rho value")
    public double rhobeg = FitBiTensorSimplexNLLS.DEFAULT_RHOBEG;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the optimization parameter for the ending rho value")
    public double rhoend = FitBiTensorSimplexNLLS.DEFAULT_RHOEND;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the maximum number of optimization iterations")
    public int maxiters = FitBiTensorSimplexNLLS.DEFAULT_MAXITERS;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("print excessive messages, e.g. the current voxel")
    public boolean chatty = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the number of threads to use")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("output bitensor volume (name output like *.bti and an directory of volumes will be created)")
    public Volume output;

    @Override
    public VolumeBiTensorFit run()
    {
        Supplier<VectFunction> factory = () ->
        {
            Gradients grads = VolumeBiTensorFit.this.gradients.copy();

            boolean subset = false;
            subset |= VolumeBiTensorFit.this.shells != null;
            subset |= VolumeBiTensorFit.this.which != null;
            subset |= VolumeBiTensorFit.this.exclude != null;

            VectFunction subsetter = null;
            if (subset)
            {
                Pair<Gradients, VectFunction> pair = grads.subset(VolumeBiTensorFit.this.shells, VolumeBiTensorFit.this.which, VolumeBiTensorFit.this.exclude);

                grads = pair.a;
                subsetter = pair.b;
            }

            FitBiTensorSimplexNLLS fit = new FitBiTensorSimplexNLLS();
            fit.gradients = grads;
            fit.cost = VolumeBiTensorFit.this.cost;
            fit.method = VolumeBiTensorFit.this.method;
            fit.scaleFrac = VolumeBiTensorFit.this.scaleFrac;
            fit.scaleTissue = VolumeBiTensorFit.this.scaleTissue;
            fit.scaleFluid = VolumeBiTensorFit.this.scaleFluid;
            fit.scaleAngle = VolumeBiTensorFit.this.scaleAngle;
            fit.fluidDiffMin = VolumeBiTensorFit.this.fluidDiffMin;
            fit.fluidDiffMax = VolumeBiTensorFit.this.fluidDiffMax;
            fit.maxiters = VolumeBiTensorFit.this.maxiters;
            fit.rhobeg = VolumeBiTensorFit.this.rhobeg;
            fit.rhoend = VolumeBiTensorFit.this.rhoend;
            VectFunction fitter = fit.create();

            if (subsetter != null)
            {
                fitter = subsetter.compose(fitter);
            }

            return fitter;
        };

        VolumeFunction vf = new VolumeFunction(factory);
        vf.input = this.input;
        vf.mask = this.mask;
        vf.threads = this.threads;
        vf.chatty = this.chatty;

        Volume out = vf.run().setModel(ModelType.BiTensor);

        this.output = out;

        return this;
    }
}