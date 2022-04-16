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

package qit.data.modules.mri.model;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.volume.VolumeFunction;
import qit.base.Model;
import qit.data.models.Tensor;
import qit.data.utils.mri.ModelUtils;
import qit.math.structs.VectFunction;

@ModuleDescription("Extract a feature from a model volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeModelFeature implements Module
{
    @ModuleInput
    @ModuleDescription("input model volume")
    public Volume input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a model name (default will try to detect it)")
    public String model = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a feature name")
    public String feature = null;

    @ModuleOutput
    @ModuleDescription("output feature volume")
    public Volume output;

    @Override
    public VolumeModelFeature run()
    {
        VectFunction apply = new VectFunction()
        {
            public void apply(Vect inv, Vect outv)
            {
                outv.set(0, inv.sum());
            }
        }.init(this.input.getDim(), 1);

        if (this.feature != null)
        {

            Model proto = ModelUtils.proto(this.input.getModel(), this.model, this.input.getDim());
            String model = ModelUtils.select(this.input.getModel(), this.model).toString();
            Logging.info(String.format("extracting %s from %s", feature, model));
            apply = ModelUtils.feature(proto, this.feature);
        }

        this.output = new VolumeFunction(apply).withInput(this.input).withMessages(false).run();

        return this;
    }

    public static Volume apply(Volume myinput, String myfeature)
    {
        return new VolumeModelFeature()
        {{
            this.input = myinput;
            this.feature = myfeature;
        }}.run().output;
    }
}
