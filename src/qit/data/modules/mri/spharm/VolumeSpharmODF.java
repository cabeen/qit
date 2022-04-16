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

package qit.data.modules.mri.spharm;

import qit.base.Logging;
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
import qit.data.modules.vects.VectsCreateSphere;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Sample an orientation distribution function (ODF) from a spharm volume.")
@ModuleAuthor("Ryan Cabeen, Dogu Baran Aydogan")
public class VolumeSpharmODF implements Module
{
    @ModuleInput
    @ModuleDescription("the input spharm volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the sphere points for estimation")
    public Vects points;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the number of points to use if you need to generate spherical points")
    public Integer num = 300;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("the output odf volume")
    public Volume output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output odf directions (only relevant if you didn't provide a specific set)")
    public Vects outpoints;

    public VolumeSpharmODF run()
    {
        Vects mypoints = this.points;

        if (mypoints == null)
        {
            VectsCreateSphere run = new VectsCreateSphere();
            run.points = this.num;
            run.smooth = 5;
            run.subdiv = this.num > 162 ? 3 : 2;
            mypoints = run.run().output;

            Logging.infosub("generated %d points", mypoints.size());
        }

        this.output = new VolumeFunction(factory(mypoints, this.input.getDim())).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();
        this.outpoints = mypoints;

        return this;
    }

    public static Supplier<VectFunction> factory(final Vects mypoints, final int dim)
    {
        int order = Spharm.sizeToOrder(dim);
        final Matrix bmat = Spharm.bmatrix(order, mypoints);

        return () -> new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(bmat.times(input));
            }
        }.init(dim, mypoints.size());
    };
}
