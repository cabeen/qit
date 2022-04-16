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

package qit.data.modules.mri.gradients;

import com.google.common.collect.Lists;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.mri.structs.Gradients;

import java.util.List;

@ModuleDescription("Reduce a set of gradients to a which based on user specification")
@ModuleAuthor("Ryan Cabeen")
public class GradientsReduce implements Module
{
    @ModuleInput
    @ModuleDescription("the input gradients")
    public Gradients input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("include only specific gradients (comma separated zero-based indices)")
    public String which = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("exclude specific gradients (comma separated zero-based indices)")
    public String exclude = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("include only specific shells")
    public String shells = null;

    @ModuleOutput
    @ModuleDescription("the output gradients")
    public Gradients output;

    public GradientsReduce run()
    {
        Vects bvecs = new Vects();
        Vects bvals = new Vects();

        List<Integer> subset = Lists.newArrayList();

        if (this.which != null)
        {
            for (Integer idx : CliUtils.parseWhich(this.which))
            {
                subset.add(idx);
            }
        }
        else if (this.exclude != null)
        {
            for (int i = 0; i < this.input.size(); i++)
            {
                subset.add(i);
            }

            for (Integer idx : CliUtils.parseWhich(this.exclude))
            {
                subset.remove(idx);
            }
        }
        else if (this.shells != null)
        {
            List<Integer> pshells = CliUtils.parseWhich(this.shells);

            for (int i = 0; i < this.input.size(); i++)
            {
                if (pshells.contains((int) Math.round(this.input.getBval(i))))
                {
                    subset.add(i);
                }
            }
        }
        else
        {
            for (int i = 0; i < this.input.size(); i++)
            {
                subset.add(i);
            }
        }

        for (Integer idx : subset)
        {
            bvecs.add(this.input.getBvec(idx));
            bvals.add(VectSource.create1D(this.input.getBval(idx)));
        }

        this.output = new Gradients(bvecs, bvals);

        return this;
    }
}
