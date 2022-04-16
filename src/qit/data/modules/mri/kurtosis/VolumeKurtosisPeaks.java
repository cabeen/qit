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

package qit.data.modules.mri.kurtosis;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.models.Kurtosis;
import qit.data.utils.mri.structs.PeakFinder;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Extract the peaks from a kurtosis volume.  This finds the average direction of local maxima clustered by hierarchical clustering.  The output fibers will encode the peak ODF value in place of volume fraction.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeKurtosisPeaks implements Module
{
    @ModuleInput
    @ModuleDescription("the input kurtosis volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the maximum number of comps")
    public Integer comps = 3;

    @ModuleParameter
    @ModuleDescription("the alpha power value")
    public Double alpha = 10.0;

    @ModuleParameter
    @ModuleDescription("the minimum peak threshold")
    public Double thresh = 0.0;

    @ModuleParameter
    @ModuleDescription("the level of detail for spherical ODF sampling")
    public Integer detail = 4;

    @ModuleParameter
    @ModuleDescription("the minimum angle in degrees for hierarchical clustering of local maxima")
    public Double cluster = 5.0;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("the output fibers volume")
    public Volume output;

    public VolumeKurtosisPeaks run()
    {
        this.output = new VolumeFunction(factory(this.input.getDim())).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        return this;
    }

    public Supplier<VectFunction> factory(final int dim)
    {
        return () ->
        {
            final PeakFinder peaker = new PeakFinder();
            peaker.cluster = VolumeKurtosisPeaks.this.cluster;
            peaker.thresh = VolumeKurtosisPeaks.this.thresh;
            peaker.comps = VolumeKurtosisPeaks.this.comps;
            peaker.init(VolumeKurtosisPeaks.this.detail);

            Logging.progress("extracting peaks");
            return new VectFunction()
            {
                public void apply(Vect input1, Vect output1)
                {
                    Kurtosis model = new Kurtosis(input1);
                    Vect odf = model.odf(false, VolumeKurtosisPeaks.this.alpha, peaker.getPoints());

                    if (odf.max() > VolumeKurtosisPeaks.this.thresh)
                    {
                        Fibers fibers = peaker.find(odf);
                        output1.set(fibers.getEncoding());
                    }
                }
            }.init(dim, new Fibers(VolumeKurtosisPeaks.this.comps).getEncodingSize());
        };
    };
}
