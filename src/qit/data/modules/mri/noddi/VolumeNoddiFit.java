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


package qit.data.modules.mri.noddi;

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
import qit.data.models.Noddi;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.fitting.FitNoddiSMT;
import qit.data.utils.mri.fitting.FitNoddiSimplex;
import qit.data.utils.mri.fitting.FitNoddiVarPro;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.fitting.FitNoddiGridSearch;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Fit a noddi volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeNoddiFit implements Module
{
    public enum VolumeNoddiFitMethod {SmartStart, FullSMT, SMT, FastSMT, NLLS, Grid}

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
    @ModuleDescription("the method for fitting")
    public VolumeNoddiFitMethod method = VolumeNoddiFitMethod.FullSMT;

    @ModuleParameter
    @ModuleDescription("the parallel diffusivity (change for ex vivo)")
    public Double dpar = Noddi.PARALLEL;

    @ModuleParameter
    @ModuleDescription("the isotropic diffusivity (change for ex vivo)")
    public Double diso = Noddi.ISOTROPIC;

    @ModuleParameter
    @ModuleDescription("include a dot compartment (set for ex vivo)")
    public boolean dot = false;

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
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleParameter
    @ModuleDescription("use fine-grained multi-threading")
    public boolean columns = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the schedule for grid search (comma separated)")
    public String schedule = FitNoddiGridSearch.DEFAULT_SCHEDULE;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("the scaleCamera of the grid refinement (positive)")
    public Double scale = FitNoddiGridSearch.DEFAULT_SCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("specify a maximum neurite density")
    public Double maxden = 0.99;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify a maximum number of iterations for non-linear optimization")
    public Integer maxiter = 100;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify the starting rho parameter for non-linear optimization")
    public double rhobeg = 0.1;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify the ending rho parameter for non-linear optimization")
    public double rhoend = 1e-4;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("specify the prior for regularizing voxels with mostly free water, a value between zero and one that indicates substantial free water, e.g. 0.95)")
    public Double prior = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleExpert
    @ModuleDescription("use grid search for an initial guess in Powell optimization")
    public Integer guess = null;

    @ModuleOutput
    @ModuleDescription("output noddi volume (name output like *.noddi and an directory of volumes will be created)")
    public Volume output;

    @Override
    public VolumeNoddiFit run()
    {
        Noddi.PARALLEL = this.dpar;
        Noddi.ISOTROPIC = this.diso;

        Supplier<VectFunction> factory = () ->
        {
            Gradients grads = VolumeNoddiFit.this.gradients.copy();

            boolean subset = false;
            subset |= this.shells != null;
            subset |= this.which != null;
            subset |= this.exclude != null;

            VectFunction subsetter = null;
            if (subset)
            {
                Pair<Gradients, VectFunction> pair = grads.subset(this.shells, this.which, this.exclude);

                grads = pair.a;
                subsetter = pair.b;
            }

            VectFunction fitter = null;
            if (VolumeNoddiFit.this.method.equals(VolumeNoddiFitMethod.NLLS))
            {
                FitNoddiSimplex fit = new FitNoddiSimplex();
                fit.gradients = grads;
                fit.grid = this.guess;
                fit.maxden = this.maxden;
                fit.prior = this.prior;
                fit.dot = this.dot;

                fitter = fit.get();
            }
            else if (VolumeNoddiFit.this.method.equals(VolumeNoddiFitMethod.FastSMT))
            {
                FitNoddiSMT fit = new FitNoddiSMT();
                fit.gradients = grads;
                fit.maxden = this.maxden;
                fit.maxiter = this.maxiter;
                fit.rhobeg = this.rhobeg;
                fit.rhoend = this.rhoend;
                fit.prior = this.prior;
                fit.skipdir = true;

                fitter = fit.get();
            }
            else if (VolumeNoddiFit.this.method.equals(VolumeNoddiFitMethod.SMT))
            {
                FitNoddiSMT fit = new FitNoddiSMT();
                fit.gradients = grads;
                fit.maxden = this.maxden;
                fit.maxiter = this.maxiter;
                fit.rhobeg = this.rhobeg;
                fit.rhoend = this.rhoend;
                fit.prior = this.prior;

                fitter = fit.get();
            }
            else if (VolumeNoddiFit.this.method.equals(VolumeNoddiFitMethod.SmartStart))
            {
                FitNoddiSMT fit = new FitNoddiSMT();
                fit.gradients = grads;
                fit.maxden = this.maxden;
                fit.maxiter = this.maxiter;
                fit.rhobeg = this.rhobeg;
                fit.rhoend = this.rhoend;
                fit.prior = this.prior;
                fit.faodi = true;
                fit.extra = true;
                fit.full = false;

                fitter = fit.get();
            }
            else if (VolumeNoddiFit.this.method.equals(VolumeNoddiFitMethod.FullSMT))
            {
                FitNoddiSMT fit = new FitNoddiSMT();
                fit.gradients = grads;
                fit.maxden = this.maxden;
                fit.maxiter = this.maxiter;
                fit.rhobeg = this.rhobeg;
                fit.rhoend = this.rhoend;
                fit.prior = this.prior;
                fit.full = true;

                fitter = fit.get();
            }
            else if (VolumeNoddiFit.this.method.equals(VolumeNoddiFitMethod.Grid))
            {
                FitNoddiGridSearch fit = new FitNoddiGridSearch();
                fit.gradients = grads;
                fit.schedule = this.schedule;
                fit.scale = this.scale;
                fit.maxden = this.maxden;
                fit.dot = this.dot;

                fitter = fit.get();
            }
            else
            {
                Logging.error("invalid method: " + VolumeNoddiFit.this.method);
            }

            if (subsetter != null)
            {
                fitter = subsetter.compose(fitter);
            }

            return fitter;
        };

        this.output = new VolumeFunction(factory).withInput(this.input).withMask(this.mask).withThreads(this.threads).withSlice(!this.columns).run().setModel(ModelType.Noddi);

        return this;
    }
}
