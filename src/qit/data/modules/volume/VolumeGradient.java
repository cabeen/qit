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
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;

import java.util.Set;

@ModuleDescription("Compute the gradient of an image")
@ModuleAuthor("Ryan Cabeen")
public class VolumeGradient implements Module
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

    public VolumeGradient run()
    {
        {
            Set<String> types = Sets.newHashSet();
            types.add(TYPE_FORWARD);
            types.add(TYPE_BACKWARD);
            types.add(TYPE_CENTRAL);
            Global.assume(types.contains(this.type), "invalid approximation type: " + this.type);
        }

        Sampling sampling = this.input.getSampling();
        Volume out = this.input.proto(3);

        int nx = sampling.numI();
        int ny = sampling.numJ();
        int nz = sampling.numK();

        double dx = 1.0 / sampling.deltaI();
        double dy = 1.0 / sampling.deltaJ();
        double dz = 1.0 / sampling.deltaK();

        int d = this.channel;
        for (Sample s : sampling)
        {
            if (out.valid(s, this.mask))
            {
                int i = s.getI();
                int j = s.getJ();
                int k = s.getK();

                double val = this.input.get(s, d);

                double di = 0;
                double dj = 0;
                double dk = 0;

                if (nx == 1)
                {
                    di = 0;
                }
                else if (i == 0 || TYPE_FORWARD.equals(this.type))
                {
                    di = dx * (this.input.get(i + 1, j, k, d) - val);
                }
                else if (i == (nx - 1) || TYPE_BACKWARD.equals(this.type))
                {
                    di = dx * (val - this.input.get(i - 1, j, k, d));
                }
                else if (TYPE_CENTRAL.equals(this.type))
                {
                    di = 0.5 * dx * (this.input.get(i + 1, j, k, d) - this.input.get(i - 1, j, k, d));
                }

                if (ny == 1)
                {
                    dj = 0;
                }
                else if (j == 0 || TYPE_FORWARD.equals(this.type))
                {
                    dj = dy * (this.input.get(i, j + 1, k, d) - val);
                }
                else if (j == (ny - 1) || TYPE_BACKWARD.equals(this.type))
                {
                    dj = dy * (val - this.input.get(i, j - 1, k, d));
                }
                else if (TYPE_CENTRAL.equals(this.type))
                {
                    dj = 0.5 * dy * (this.input.get(i, j + 1, k, d) - this.input.get(i, j - 1, k, d));
                }

                if (nz == 1)
                {

                }
                else if (k == 0 || TYPE_FORWARD.equals(this.type))
                {
                    dk = dz * (this.input.get(i, j, k + 1, d) - val);
                }
                else if (k == (nz - 1) || TYPE_BACKWARD.equals(this.type))
                {
                    dk = dz * (val - this.input.get(i, j, k - 1, d));
                }
                else if (TYPE_CENTRAL.equals(this.type))
                {
                    dk = 0.5 * dz * (this.input.get(i, j, k + 1, d) - this.input.get(i, j, k - 1, d));
                }

                out.set(s, 0, di);
                out.set(s, 1, dj);
                out.set(s, 2, dk);
            }
        }

        this.output = out;

        return this;
    }

    public static Volume apply(Volume volume)
    {
        return new VolumeGradient()
        {{
            this.input = volume;
        }}.run().output;
    }
}
