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

package qit.data.modules.mri.relaxometry;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.*;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.VectSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.mri.fitting.FitExpDecayLLS;
import qit.data.utils.mri.fitting.FitExpDecayNLLS;
import qit.data.utils.volume.VolumeFunction;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.function.Supplier;

@ModuleDescription("Fit an exponential decay model to volumetric data: y = alpha * exp(-beta * x)")
@ModuleAuthor("Ryan Cabeen")
public class VolumeExpDecayFit implements Module
{
    @ModuleInput
    @ModuleDescription("the input dwi")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the varying parameters values used for fitting, if not provided the start and step parameters are used instead")
    public Vects varying;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the method for fitting (lls, wlls, nlls)")
    public String method = FitExpDecayLLS.WLLS;

    @ModuleParameter
    @ModuleDescription("the starting echo time (not used if you provide a varying input)")
    public Double start = 1d;

    @ModuleParameter
    @ModuleDescription("the spacing between echo times (not used if you provide a varying input)")
    public Double step = 1d;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a subset of data to include (comma-separated list of indices starting from zero)")
    public String which = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a subset of data to exclude (comma-separated list of indices starting from zero)")
    public String exclude = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a speciic subset of values to include (as opposed to indices)")
    public String select = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum alpha")
    public Double minAlpha = 0d;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum beta")
    public Double minBeta = 0d;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply a threshold to the input image, e.g. remove zero values")
    public Double thresh = null;

    @ModuleParameter
    @ModuleDescription("apply shrinkage regularization for low SNR regions")
    public boolean shrinkage = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the amount of smoothing to apply")
    public Integer shrinkSmooth = 5;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maximum shrinkage snr")
    public Double shrinkSnrMax = 7d;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum shrinkage snr")
    public Double shrinkSnrMin = 5d;

    @ModuleParameter
    @ModuleDescription("the mynumber of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output exp decay model volume")
    public Volume output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output exp decay alpha parameter")
    public Volume outputAlpha;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output exp decay beta parameter")
    public Volume outputBeta;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output exp decay error")
    public Volume outputError;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output exp decay residuals")
    public Volume outputResiduals;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output exp decay SNR map")
    public Volume outputSnr;

    public VolumeExpDecayFit run()
    {
        Volume myinput = this.input;
        int mynum = myinput.getDim();
        Vect myparam = VectSource.linspace(this.start, this.start + mynum * this.step, mynum);

        if (this.varying != null)
        {
            myparam = this.varying.flatten();
        }

        if (this.thresh != null)
        {
            myinput = VolumeUtils.mask(myinput, VolumeThreshold.apply(myinput, this.thresh));
        }

        if (this.which != null || this.exclude != null)
        {
            VectFunction mysubset = VectFunctionSource.subset(mynum, this.which, this.exclude);

            myparam = mysubset.apply(myparam);
            myinput = VolumeUtils.apply(myinput, mysubset);
            mynum = myinput.getDim();
        }

        if (this.select != null)
        {
            VectFunction mysubset = VectFunctionSource.select(myparam, this.select);

            myparam = mysubset.apply(myparam);
            myinput = VolumeUtils.apply(myinput, mysubset);
            mynum = myinput.getDim();
        }

        Supplier<VectFunction> factory = null;

        switch (this.method)
        {
            case FitExpDecayLLS.LLS:
                factory = new FitExpDecayLLS().withWeighted(false).withVarying(myparam).withMins(this.minAlpha, this.minBeta);
                break;
            case FitExpDecayLLS.WLLS:
                factory = new FitExpDecayLLS().withWeighted(true).withVarying(myparam).withMins(this.minAlpha, this.minBeta);
                break;
            case FitExpDecayNLLS.NLLS:
                factory = new FitExpDecayNLLS().withVarying(myparam);
                break;
            default:
                Logging.error("invalid method: " + this.method);
        }

        Volume fit = new VolumeFunction(factory).withInput(myinput).withMask(this.mask).withThreads(this.threads).run();
        Volume alpha = fit.getVolume(0);
        Volume beta = fit.getVolume(1);

        Volume error = fit.proto(1);
        Volume residuals = myinput.proto();
        for (Sample sample : myinput.getSampling())
        {
            if (myinput.valid(sample, this.mask))
            {
                double e = 0;
                for (int i = 0; i < mynum; i++)
                {
                    double y = myinput.get(sample, i);
                    double p = alpha.get(sample, 0) * Math.exp(-beta.get(sample, 0) * myparam.get(i));
                    double r = y - p;
                    e += r * r;

                    residuals.set(sample, i, r);
                }

                double rmse = Math.sqrt(e / mynum);
                error.set(sample, rmse);
            }
        }

        Volume alphaSmooth = new VolumeFilterGaussian()
        {{
            this.input = alpha;
            this.sigma = -1.0;
            this.num = VolumeExpDecayFit.this.shrinkSmooth;
        }}.run().output;

        Volume errorSmooth = new VolumeFilterGaussian()
        {{
            this.input = error;
            this.sigma = -1.0;
            this.num = VolumeExpDecayFit.this.shrinkSmooth;
        }}.run().output;

        Volume snr = VolumeUtils.ratio(alphaSmooth, errorSmooth);
        double shrinkSnrDelta = this.shrinkSnrMax - this.shrinkSnrMin;
        shrinkSnrDelta = MathUtils.zero(shrinkSnrDelta) ? 1.0 : 0.0;

        if (this.shrinkage)
        {
            Logging.info("applying shrinkage");
            for (Sample sample : myinput.getSampling())
            {
                if (myinput.valid(sample, this.mask))
                {
                    double mysnr = snr.get(sample, 0);
                    double myalpha = alpha.get(sample, 0);
                    double mybeta = beta.get(sample, 0);

                    double weight = Math.min(1.0, Math.max(0.0, (mysnr - this.shrinkSnrMin) / shrinkSnrDelta));

                    myalpha *= weight;
                    mybeta *= weight;

                    alpha.set(sample, 0, myalpha);
                    beta.set(sample, 0, mybeta);
                    fit.set(sample, 0, myalpha);
                    fit.set(sample, 1, mybeta);
                }
            }
        }

        this.output = fit;
        this.outputAlpha = alpha;
        this.outputBeta = beta;
        this.outputError = error;
        this.outputResiduals = residuals;
        this.outputSnr = snr;

        return this;
    }
}