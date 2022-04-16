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


package qit.data.modules.mri.tensor;

import qit.base.Logging;
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
import qit.data.models.Tensor;
import qit.data.modules.mri.gradients.GradientsTransform;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.fitting.FitTensorFreeWaterLLS;
import qit.data.utils.mri.fitting.FitTensorFreeWaterSimplex;
import qit.data.utils.mri.fitting.FitTensorRestore;
import qit.data.utils.mri.fitting.FitTensorSimplexNLLS;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.fitting.FitTensorLLS;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@ModuleDescription("Fit a tensor volume to a diffusion-weighted MRI.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTensorFit implements Module
{
    public enum TensorFitType
    {
        LLS, WLLS, NLLS, FWLLS, FWWLLS, FWNLLS, RESTORE
    }

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
    public TensorFitType method = TensorFitType.LLS;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify a cost function for non-linear fitting")
    public CostType cost = CostType.SE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use the lowest single shell for tensor estimation (if used, this will skip the shells, which, and exclude flags)")
    public boolean single = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use the best combination of b-values (first try b<=1250, and otherwise use the lowest single shell)")
    public boolean bestb = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify the multiple for rounding b-values, e.g. if the rounder in 100, then 1233 is treated as 1200")
    public Integer rounder = 100;

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
    @ModuleDescription("estimate the baseline value separately from tensor parameter estimation (only for LLS)")
    public boolean baseline = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the number of threads to use")
    public int threads = 1;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("clamp the diffusivity to be greater or equal to the given value")
    public Double clamp = 0.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the diffusivity for free water modeling")
    public double free = Tensor.FREE_DIFF;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the step for gradient descent")
    public double gamma = 1e-1;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the convergence threshold (chisq of normalized signal)")
    public double thresh = 1e-9;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the maximum number of iterations")
    public int maxiter = 5000;

    @ModuleOutput
    @ModuleDescription("output tensor volume (name output like *.dti and an directory of volumes will be created) ")
    public Volume output;

    @Override
    public VolumeTensorFit run()
    {
        Tensor.FREE_DIFF = this.free;

        Supplier<VectFunction> factory = () ->
        {
            Gradients grads = VolumeTensorFit.this.gradients.copy();
            VectFunction subsetter = null;

            if (this.single)
            {
                GradientsTransform xfm = new GradientsTransform();
                xfm.input = grads;
                xfm.round = true;
                xfm.rounder = this.rounder;
                grads = xfm.run().output;

                List<Integer> shells = grads.getShells(true);
                Collections.sort(shells);

                Integer base = shells.get(0);
                Integer first = shells.get(1);

                String myshells = base + "," + first;
                Logging.info("using single shell query: " + myshells);

                Pair<Gradients, VectFunction> pair = grads.subset(myshells, null, null);

                grads = pair.a;
                subsetter = pair.b;
            }
            else if (this.bestb)
            {
                GradientsTransform xfm = new GradientsTransform();
                xfm.input = grads;
                xfm.round = true;
                xfm.rounder = this.rounder;
                grads = xfm.run().output;

                List<Integer> shells = grads.getShells(true);
                Collections.sort(shells);

                Integer base = shells.get(0);
                String myshells = String.valueOf(base);

                for (int i = 1; i < shells.size(); i++)
                {
                    Integer shell = shells.get(i);
                    if (shell < 1250)
                    {
                        myshells += "," + shell;
                    }
                }

                if (myshells.equals(String.valueOf(base)) && shells.size() > 1)
                {
                    myshells += "," + shells.get(1);
                }

                Logging.info("using bestb shell query: " + myshells);
                Pair<Gradients, VectFunction> pair = grads.subset(myshells, null, null);

                grads = pair.a;
                subsetter = pair.b;
            }
            else
            {
                boolean subset = false;
                subset |= this.shells != null;
                subset |= this.which != null;
                subset |= this.exclude != null;

                if (subset)
                {
                    Pair<Gradients, VectFunction> pair = grads.subset(this.shells, VolumeTensorFit.this.which, VolumeTensorFit.this.exclude);

                    grads = pair.a;
                    subsetter = pair.b;
                }
            }


            TensorFitType myMethod = VolumeTensorFit.this.method;
            if (!grads.multishell())
            {
                if (myMethod.equals(TensorFitType.FWLLS))
                {
                    Logging.info("data is single shell, defaulting to LLS fitting");
                    myMethod = TensorFitType.LLS;
                }

                if (myMethod.equals(TensorFitType.FWWLLS))
                {
                    Logging.info("data is single shell, defaulting to WLLS fitting");
                    myMethod = TensorFitType.WLLS;
                }

                if (myMethod.equals(TensorFitType.FWNLLS))
                {
                    Logging.info("data is single shell, defaulting to NLLS fitting");
                    myMethod = TensorFitType.NLLS;
                }
            }

            VectFunction fitter = null;
            switch (myMethod)
            {
                case NLLS:
                {
                    FitTensorSimplexNLLS fit = new FitTensorSimplexNLLS();
                    fit.gradients = grads;
                    fit.baseline = VolumeTensorFit.this.baseline;
                    fit.cost = VolumeTensorFit.this.cost;
                    fitter = fit.get();
                    break;
                }

                case LLS:
                {
                    FitTensorLLS fit = new FitTensorLLS();
                    fit.gradients = grads;
                    fit.clamp = VolumeTensorFit.this.clamp;
                    fit.baseline = VolumeTensorFit.this.baseline;
                    fit.weighted = false;
                    fitter = fit.get();
                    break;
                }

                case WLLS:
                {
                    FitTensorLLS fit = new FitTensorLLS();
                    fit.gradients = grads;
                    fit.weighted = true;
                    fit.clamp = VolumeTensorFit.this.clamp;
                    fit.baseline = VolumeTensorFit.this.baseline;
                    fitter = fit.get();
                    break;
                }

                case FWLLS:
                {
                    FitTensorFreeWaterLLS fit = new FitTensorFreeWaterLLS();
                    fit.gradients = grads;
                    fit.weighted = false;
                    fit.cost = VolumeTensorFit.this.cost;
                    fitter = fit.get();
                    break;
                }

                case FWWLLS:
                {
                    FitTensorFreeWaterLLS fit = new FitTensorFreeWaterLLS();
                    fit.gradients = grads;
                    fit.weighted = true;
                    fit.cost = VolumeTensorFit.this.cost;
                    fitter = fit.get();
                    break;
                }

                case FWNLLS:
                {
                    FitTensorFreeWaterSimplex fit = new FitTensorFreeWaterSimplex();
                    fit.gradients = grads;
                    fit.cost = VolumeTensorFit.this.cost;
                    fitter = fit.get();
                    break;
                }

                case RESTORE:
                {
                    FitTensorRestore fit = new FitTensorRestore();
                    fit.gradients = grads;
                    fitter = fit.get();
                    break;
                }

                default:
                    Logging.error("unsupported fit type");
            }


            if (subsetter != null)
            {
                fitter = subsetter.compose(fitter);
            }

            return fitter;
        };

        Volume out = new VolumeFunction(factory).withInput(this.input).withMask(this.mask).withThreads(this.threads).run().setModel(ModelType.Tensor);

        this.output = out;

        return this;
    }
}
