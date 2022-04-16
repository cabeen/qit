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
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.Spharm;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Resample an ODF using spherical harmonics.  Using a lower maximum order will lead to smoother resamplings")
@ModuleAuthor("Ryan Cabeen")
public class VolumeOdfResample implements Module
{
    @ModuleInput
    @ModuleDescription("the input ODF volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the input odf directions")
    public Vects dirs;

    @ModuleInput
    @ModuleDescription("the directions to resample")
    public Vects resample;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the maximum spherical harmonic order used for resampling")
    public Integer order = 8;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("the output ODF volume")
    public Volume output;

    public VolumeOdfResample run()
    {
        this.output = new VolumeFunction(factory()).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        return this;
    }

    public Supplier<VectFunction> factory()
    {
        return () ->
        {
            Matrix spharmToNewOdf= Spharm.bmatrix(VolumeOdfResample.this.order, VolumeOdfResample.this.resample);
            Matrix oldOdfToSpharm = Spharm.bmatrix(VolumeOdfResample.this.order, VolumeOdfResample.this.dirs).inv();
            final Matrix transform = spharmToNewOdf.times(oldOdfToSpharm);

            return new VectFunction()
            {
                public void apply(Vect input1, Vect output1)
                {
                    output1.set(transform.times(input1));
                }
            }.init(transform.cols(), transform.rows());
        };
    }
}
