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

import qit.base.Logging;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.utils.mri.structs.PeakFinder;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Extract the peaks from an odf volume.  This finds the average direction of local maxima clustered by hierarchical clustering.  The output fibers will encode the peak ODF value in place of volume fraction.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeOdfPeaks implements Module
{
    @ModuleInput
    @ModuleDescription("the input ODF volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the odf directions")
    public Vects points;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the maximum number of comps")
    public Integer comps = 3;

    @ModuleParameter
    @ModuleDescription("scaleCamera peak values by generalized fractional anisotropy after extraction")
    public boolean gfa = false;

    @ModuleParameter
    @ModuleDescription("the minimum peak threshold")
    public Double thresh = 0.01;

    @ModuleParameter
    @ModuleDescription("the mode for extracting the peak fraction from the lobe")
    public PeakFinder.PeakMode mode = PeakFinder.PeakMode.Mean;

    @ModuleParameter
    @ModuleDescription("the minimum angle in degrees for hierarchical clustering of local maxima")
    public Double cluster = 25.0;

    @ModuleParameter
    @ModuleDescription("match the ODF sum")
    public boolean match = false;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("the output fibers volume")
    public Volume output;

    public VolumeOdfPeaks run()
    {
        this.output = new VolumeFunction(factory()).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();
        this.output.setModel(ModelType.Fibers);

        return this;
    }

    public VectFunction get()
    {
        final PeakFinder peaker = new PeakFinder();
        peaker.cluster = this.cluster;
        peaker.thresh = this.thresh;
        peaker.gfa = this.gfa;
        peaker.comps = this.comps;
        peaker.mode = this.mode;
        peaker.match = this.match;
        peaker.init(this.points);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                if (input.max() > VolumeOdfPeaks.this.thresh)
                {
                    Fibers fibers = peaker.find(input);
                    output.set(fibers.getEncoding());
                }
            }
        }.init(this.points.size(), new Fibers(VolumeOdfPeaks.this.comps).getEncodingSize());
    }

    public Supplier<VectFunction> factory()
    {
        return () -> get();
    }
}
