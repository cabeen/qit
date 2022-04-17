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

package qit.data.modules.mask;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;

@ModuleDescription("Close a mask using morphological operations")
@ModuleAuthor("Ryan Cabeen")
public class MaskBoundary implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleParameter
    @ModuleDescription("specify an element")
    public String element = MaskSource.DEFAULT_ELEMENT;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskBoundary run()
    {
        MaskDilate dilater = new MaskDilate();
        dilater.input = this.input;
        dilater.element = this.element;
        Mask dilate = dilater.run().output;

        MaskErode eroder = new MaskErode();
        eroder.input = input;
        eroder.element = this.element;
        Mask erode = eroder.run().output;

        MaskInvert inverter = new MaskInvert();
        inverter.input = erode;
        Mask invert = inverter.run().output;

        MaskIntersection intersector = new MaskIntersection();
        intersector.left = dilate;
        intersector.right = invert;
        Mask out = intersector.run().output;

        this.output = out;
        return this;
    }

    public static Mask apply(Mask mask)
    {
        return new MaskBoundary()
        {{
            this.input = mask;
        }}.run().output;
    }
}