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
import qit.data.source.MaskSource;

@ModuleDescription("Compute the local extrema of a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeExtrema implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the volume channel")
    public int channel = 0;

    @ModuleParameter
    @ModuleDescription("find the minima")
    public boolean minima = false;

    @ModuleParameter
    @ModuleDescription("find the maxima")
    public boolean maxima = false;

    @ModuleParameter
    @ModuleDescription("specify an element")
    public String element = MaskSource.DEFAULT_ELEMENT;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public VolumeExtrema run()
    {
        Mask element = MaskSource.element(this.element);
        Sampling sampling = input.getSampling();
        Sampling esampling = element.getSampling();

        int cx = (esampling.numI() - 1) / 2;
        int cy = (esampling.numJ() - 1) / 2;
        int cz = (esampling.numK() - 1) / 2;

        if (!this.maxima && !this.minima)
        {
            this.maxima = true;
            this.minima = true;
        }

        Mask out = MaskSource.create(this.input.getSampling());
        for (Sample sample : sampling)
        {
            if (!this.input.valid(sample, this.mask))
            {
                continue;
            }

            boolean min = true;
            boolean max = true;
            double value = this.input.get(sample, this.channel);
            for (Sample esample : esampling)
            {
                if (element.foreground(esample))
                {
                    int ni = sample.getI() + esample.getI() - cx;
                    int nj = sample.getJ() + esample.getJ() - cy;
                    int nk = sample.getK() + esample.getK() - cz;

                    if (!sampling.contains(ni, nj, nk))
                    {
                        continue;
                    }

                    double nvalue = this.input.get(ni, nj, nk, this.channel);
                    min &= value <= nvalue;
                    max &= nvalue <= value;
                }
            }

            boolean extrema = (this.minima && min) || (this.maxima && max);
            out.set(sample, extrema ? 1 : 0);
        }

        this.output = out;
        return this;
    }
}
