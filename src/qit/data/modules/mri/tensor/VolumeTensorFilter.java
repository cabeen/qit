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

package qit.data.modules.mri.tensor;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.models.Tensor;
import qit.math.utils.MathUtils;
import qit.math.utils.expression.ScalarExpression;

import java.math.BigDecimal;

@ModuleDescription("Filter a tensor volume in a variety of ways, e.g. changing units, clamping diffusivities, masking out regions")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTensorFilter implements Module
{
    @ModuleInput
    @ModuleDescription("the input tensor volume")
    public Volume input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the scaleCamera to multiply diffusivities (typically needed to changed the physical units)")
    public Double scale = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("clamp the diffusivities (values below the threshold will be set to it)")
    public Double clamp = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("zero out voxels that do not satisfy the given arithmetic expression, e.g. FA > 0.15 && MD < 0.001 && MD > 0.0001")
    public String expression = null;

    @ModuleOutput
    @ModuleDescription("the output tensor volume")
    public Volume output;

    public VolumeTensorFilter run()
    {
        Sampling sampling = this.input.getSampling();
        Volume out = this.input.proto();

        ScalarExpression exp = this.expression != null ? new ScalarExpression(this.expression) : null;

        for (Sample sample : sampling)
        {
            Tensor model = new Tensor(this.input.get(sample)).copy();

            if (this.scale != null)
            {
                model.scale(this.scale);
            }

            if (this.clamp != null)
            {
                model.clamp(this.clamp);
            }

            if (exp != null)
            {
                for (String feature : model.features())
                {
                    exp.with(feature, new BigDecimal(model.feature(feature).get(0)));
                }

                if (MathUtils.nonzero(exp.eval().doubleValue()))
                {
                    model.setZero();
                }
            }

            out.set(sample, model.getEncoding());
        }

        this.output = out;
        return this;
    }
}
