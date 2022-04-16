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

package qit.data.modules.mri.dwi;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.mri.structs.Gradients;

import java.util.List;

@ModuleDescription("Extract baseline signal statistics from a diffusion-weighted MR volume")
@ModuleCitation("Vos, Sjoerd B., et al. \"The importance of correcting for signal drift in diffusion MRI.\" Magnetic resonance in medicine 77.1 (2017): 285-299.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiBaseline implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the input gradients")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input mask")
    public Mask mask;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output mean baseline signal")
    public Volume mean;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output median baseline signal")
    public Volume median;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output variance of baseline signal (only meaningful if there are multiple baselines)")
    public Volume var;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output standard deviation of baseline signal (only meaningful if there are multiple baselines)")
    public Volume std;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output complete set of baseline signals")
    public Volume cat;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output signal drift estimates (slope, stderr, tstat, pval, sig) measured by percentage change")
    public Volume drift;

    public VolumeDwiBaseline run()
    {
        int ndrift = 6;
        List<Integer> bidx = this.gradients.getBaselineIdx();

        Volume meanOut = this.input.proto(1);
        Volume medianOut = this.input.proto(1);
        Volume stdOut = this.input.proto(1);
        Volume varOut = this.input.proto(1);
        Volume catOut = this.input.proto(bidx.size());
        Volume driftOut = this.input.proto(ndrift);

        for (Sample sample : this.input.getSampling())
        {
            if (this.input.valid(sample, this.mask))
            {
                Vect baselines = this.input.get(sample).sub(bidx);
                VectStats stats = new VectStats().withInput(baselines).run();

                meanOut.set(sample, 0, stats.mean);
                medianOut.set(sample, 0, stats.median);
                catOut.set(sample, baselines);

                if (baselines.size() > 1)
                {
                    stdOut.set(sample, 0, stats.std);
                    varOut.set(sample, 0, stats.var);

                    SimpleRegression regression = new SimpleRegression();
                    for (int i = 0; i < baselines.size(); i++)
                    {
                        // report slopes as percentage of normalized signal, like Vos et al
                        double x = bidx.get(i) / (double)   this.input.getDim();
                        double y = 100 * baselines.get(i) / stats.mean;
                        regression.addData(x, y);
                    }

                    double driftIntercept = regression.getIntercept();
                    double driftEstimate = regression.getSlope();
                    double driftError = regression.getSlopeStdErr();
                    double driftTstat = driftEstimate / driftError;
                    double driftPval = 2.0 * (new NormalDistribution().cumulativeProbability(-Math.abs(driftTstat)));
                    double driftSig = driftPval <= 0.05 ? 1.0 : 0.0;

                    Vect driftVect = VectSource.createND(ndrift);

                    driftVect.set(0, driftIntercept);
                    driftVect.set(1, driftEstimate);
                    driftVect.set(2, driftError);
                    driftVect.set(3, driftTstat);
                    driftVect.set(4, driftPval);
                    driftVect.set(5, driftSig);

                    driftOut.set(sample, driftVect);
                }
            }
        }

        this.mean = meanOut;
        this.median = medianOut;
        this.std = stdOut;
        this.var = varOut;
        this.cat = catOut;
        this.drift = driftOut;

        return this;
    }
}
