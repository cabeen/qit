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

package qit.data.modules.volume;

import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Volume;
import qit.data.utils.volume.VolumeFunction;
import qit.math.source.VectFunctionSource;

import java.util.Set;

@ModuleDescription("Compute the gradient magnitude of an image")
@ModuleAuthor("Ryan Cabeen")
public class VolumeGradientMagnitude implements Module
{
    public static final String TYPE_FORWARD = "forward";
    public static final String TYPE_BACKWARD = "backward";
    public static final String TYPE_CENTRAL = "central";

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the volume channel (default applies to all)")
    public Integer channel = 0;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("type of finite difference approximation (forward, backward, central)")
    public String type = TYPE_CENTRAL;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output = null;

    public VolumeGradientMagnitude run()
    {
        {
            Set<String> types = Sets.newHashSet();
            types.add(TYPE_FORWARD);
            types.add(TYPE_BACKWARD);
            types.add(TYPE_CENTRAL);
            Global.assume(types.contains(this.type), "invalid approximation type: " + this.type);
        }

        VolumeGradient grader = new VolumeGradient();
        grader.input = this.input;
        grader.mask = this.mask;
        grader.channel = this.channel;
        grader.type = this.type;
        Volume grad = grader.run().output;

        Volume mag = new VolumeFunction(VectFunctionSource.norm()).withInput(grad).withMask(this.mask).run();

        this.output = mag;

        return this;
    }

    public static Volume apply(Volume volume)
    {
        return new VolumeGradientMagnitude()
        {{
            this.input = volume;
        }}.run().output;
    }
}
