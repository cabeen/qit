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

package qit.data.modules.mri.smt;

import com.google.common.collect.Lists;
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
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Mcsmt;
import qit.data.utils.mri.fitting.FitMcsmt;
import qit.data.utils.mri.fitting.FitMcsmtDot;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.structs.Shells;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

@ModuleDescription("Estimate multi-compartment microscopic diffusion parameters")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Kaden, E., Kelm, N. D., Carson, R. P., Does, M. D., & Alexander, D. C. (2016). Multi-compartment microscopic diffusion imaging. NeuroImage, 139, 346-359.")
public class VolumeMcsmtFit implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the input gradients")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the input number of threads")
    public int threads = 1;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum number of iterations")
    public int maxiters = FitMcsmt.DEFAULT_MAXITERS;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the starting simplex size")
    public double rhobeg = FitMcsmt.DEFAULT_RHOBEG;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the ending simplex size")
    public double rhoend = FitMcsmt.DEFAULT_RHOEND;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for parallel diffusivity")
    public double dint = FitMcsmt.DEFAULT_DINT;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for intra-neurite volume fraction")
    public double frac = FitMcsmt.DEFAULT_FRAC;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the scaling for volume fraction optimization")
    public double fscale = FitMcsmt.DEFAULT_FSCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the scaling for diffusivity optimization")
    public double dscale = FitMcsmt.DEFAULT_DSCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum diffusivity")
    public double dmax = FitMcsmt.DEFAULT_DMAX;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("include a dot compartment")
    public boolean dot = false;

    @ModuleOutput
    @ModuleDescription("the output parameter map")
    public Volume output;

    public VolumeMcsmtFit run()
    {
        if (this.dot)
        {
            FitMcsmtDot fit = new FitMcsmtDot();
            fit.rhoend = this.rhoend;
            fit.rhobeg = this.rhobeg;
            fit.frac = this.frac;
            fit.dint = this.dint;
            fit.dmax = this.dmax;
            fit.fscale = this.fscale;
            fit.dscale = this.dscale;
            fit.maxiters = this.maxiters;

            this.output = new VolumeFunction(fit.fitter(this.gradients)).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();
        }
        else
        {
            FitMcsmt fit = new FitMcsmt();
            fit.rhoend = this.rhoend;
            fit.rhobeg = this.rhobeg;
            fit.frac = this.frac;
            fit.dint = this.dint;
            fit.dmax = this.dmax;
            fit.fscale = this.fscale;
            fit.dscale = this.dscale;
            fit.maxiters = this.maxiters;

            this.output = new VolumeFunction(fit.fitter(this.gradients)).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();
        }

        this.output.setModel(ModelType.Mcsmt);

        return this;
    }
}
