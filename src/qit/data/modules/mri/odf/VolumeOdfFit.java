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

package qit.data.modules.mri.odf;

import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.vects.VectsCreateSphere;
import qit.data.utils.mri.fitting.FitOdfDampedRichardsonLucy;
import qit.data.utils.mri.fitting.FitOdfMCRLD;
import qit.data.utils.mri.fitting.FitOdfRichardsonLucy;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Fit an orientation distribution function (ODF) using spherical deconvolution")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Dell'Acqua, F., Scifo, P., Rizzo, G., Catani, M., Simmons, A., Scotti, G., & Fazio, F. (2010). A modified damped Richardson-Lucy algorithm to reduce isotropic background effects in spherical deconvolution. Neuroimage, 49(2), 1446-1458. ")
public class VolumeOdfFit implements Module
{
    @ModuleInput
    @ModuleDescription("the input dwi")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the gradients")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input odf directions (if you don't provide this, you they will be generated and returned in outpoints)")
    public Vects points;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

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
    @ModuleDescription("the number of points to use if you need to generate spherical points")
    public int num = 300;

    @ModuleParameter
    @ModuleDescription("the number of iterations for deconvolution")
    public int iters = FitOdfRichardsonLucy.DEFAULT_RLDITERS;

    @ModuleParameter
    @ModuleDescription("the kernel diffusivity for deconvolution")
    public double alpha = FitOdfRichardsonLucy.DEFAULT_ALPHA;

    @ModuleParameter
    @ModuleDescription("the kernel radial diffusivity for deconvolution")
    public double beta = FitOdfRichardsonLucy.DEFAULT_BETA;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("the output ODF volume")
    public Volume output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output odf directions (only relevant if you didn't provide a specific set)")
    public Vects outpoints;

    public VolumeOdfFit run()
    {
        Vects mypoints = this.points;

        if (this.points == null)
        {
            VectsCreateSphere run = new VectsCreateSphere();
            run.points = this.num;
            run.smooth = 5;
            run.subdiv = this.num> 162 ? 3 : 2;
            mypoints = run.run().output;
        }

        Vects fpoints = mypoints;

        Supplier<VectFunction> supplier = () ->
        {
            Gradients grads = this.gradients.copy();

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

            FitOdfRichardsonLucy rld = new FitOdfRichardsonLucy();
            rld.alpha = this.alpha;
            rld.beta = this.beta;
            rld.rlditers = this.iters;
            VectFunction fitter = rld.fitter(grads, fpoints);

            if (subsetter != null)
            {
                fitter = subsetter.compose(fitter);
            }

            return fitter;
        };

        this.output = new VolumeFunction(supplier).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();
        this.outpoints = mypoints;

        return this;
    }
}
