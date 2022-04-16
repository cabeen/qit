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

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Volume;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.structs.Histogram;
import qit.math.utils.MathUtils;

@ModuleDescription("Harmonize a volume with a reference volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeHarmonize implements Module
{
    public enum Type { Peak }

    @ModuleInput
    @ModuleDescription("the input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an input mask to restrict both statistics and harmonized voxels")
    public Mask inputMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an input mask to only restrict computing statistics")
    public Mask inputStatMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a reference volume")
    public Volume reference;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a reference mask")
    public Mask referenceMask;

    @ModuleParameter
    @ModuleDescription("the number of bins")
    public int bins = 256;

    @ModuleParameter
    @ModuleDescription("the method for computing the histogram lower bound")
    public VolumeHistogram.Bound lower = VolumeHistogram.Bound.ThreeStd;

    @ModuleParameter
    @ModuleDescription("the method for computing the histogram upper bound")
    public VolumeHistogram.Bound upper = VolumeHistogram.Bound.ThreeStd;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify a specific lower bound for user defined mode")
    public Double mylower = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify a specific upper bound for user defined mode")
    public Double myupper = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the amount of smoothing (relative to bins)")
    public Double smooth = 10.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a lower threshold for excluding background")
    public Double lowerThresh = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an upper threshold for excluding background")
    public Double upperThresh = null;

    @ModuleOutput
    @ModuleDescription("the output volume")
    public Volume output;

    public VolumeHarmonize run()
    {
        VolumeHistogram histogrammer = new VolumeHistogram();
        histogrammer.smooth = this.smooth;
        histogrammer.hdata = false;
        histogrammer.mylower = this.mylower;
        histogrammer.myupper = this.myupper;
        histogrammer.lower = this.lower;
        histogrammer.upper = this.upper;
        histogrammer.bins = this.bins;
        histogrammer.lowerThresh = this.lowerThresh;
        histogrammer.upperThresh = this.upperThresh;

        histogrammer.input = this.input;
        histogrammer.mask = this.inputStatMask != null ? this.inputStatMask : this.inputMask;
        Histogram inputHistogram = histogrammer.get().copy();

        double target = 1.0;

        if (this.reference != null)
        {
            histogrammer.input = this.reference;
            histogrammer.mask = this.referenceMask;
            Histogram referenceHistogram = histogrammer.get().copy();

            target = referenceHistogram.mode();
        }

        double mode = inputHistogram.mode();
        double factor = MathUtils.zero(mode) ? 1.0 : target / mode;

        Logging.info("detected mode: " + mode);
        Logging.info("using factor: " + factor);

        Volume myoutput = this.input.proto();
        for (Sample sample : this.input.getSampling())
        {
            if (this.input.valid(sample, this.inputMask))
            {
                for (int d = 0; d < this.input.getDim(); d++)
                {
                    myoutput.set(sample, d, factor * this.input.get(sample, d));
                }
            }
        }

        this.output = myoutput;

        return this;
    }
}
