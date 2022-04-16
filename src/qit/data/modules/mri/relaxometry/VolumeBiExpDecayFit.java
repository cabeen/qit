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

package qit.data.modules.mri.relaxometry;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.mri.fitting.FitBiExpDecayNLLS;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

@ModuleDescription("Fit an bi-exponential decay model to volumetric data: y = alpha * (frac * exp(-beta * x) + (1 - frac) exp(-gamma * x))")
@ModuleUnlisted
@ModuleAuthor("Ryan Cabeen")
public class VolumeBiExpDecayFit implements Module
{
    @ModuleInput
    @ModuleDescription("the input dwi")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the varying parameters values used for fitting, if not provided the start and step parameters are used instead")
    public Vects varying;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the starting echo time (not used if you provide a varying input)")
    public Double start = 1d;

    @ModuleParameter
    @ModuleDescription("the spacing between echo times (not used if you provide a varying input)")
    public Double step = 1d;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("the output exp decay model volume")
    public Volume output;

    public VolumeBiExpDecayFit run()
    {
        int num = this.input.getDim();
        double end = this.start + num * this.step;
        Vect param = VectSource.linspace(this.start, end, num);

        if (this.varying != null)
        {
            param = this.varying.flatten();
        }

        this.output = new VolumeFunction(FitBiExpDecayNLLS.get(param)).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        return this;
    }
}
