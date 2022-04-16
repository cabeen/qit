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

import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Integers;
import qit.data.datasets.Affine;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Tensor;
import qit.data.modules.mask.MaskLargest;
import qit.data.modules.mri.model.VolumeModelFeature;
import qit.data.modules.mri.tensor.VolumeTensorFit;
import qit.data.modules.volume.VolumeSegmentForegroundOtsu;
import qit.data.modules.volume.VolumeThresholdOtsu;
import qit.data.source.MatrixSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.mri.structs.Gradients;

import java.util.Map;
import java.util.function.Function;

@ModuleDescription("Correct for errors in the orientation of diffusion gradients using the fiber coherence index")
@ModuleCitation("Schilling, Kurt G., et al. \"A fiber coherence index for quality control of B-table orientation in diffusion MRI scans.\" Magnetic resonance imaging (2019).")
@ModuleAuthor("Ryan Cabeen")
public class GradientsMatch implements Module
{
    @ModuleInput
    @ModuleDescription("the input gradients")
    public Gradients input;

    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume dwi;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("skip spatial scaling (note: this is not included in the paper by Schilling et al)")
    public boolean nospat;

    @ModuleOutput
    @ModuleDescription("the output matched gradients")
    public Gradients output;

    public GradientsMatch run()
    {
        Logging.info("fitting initial tensors");
        VolumeTensorFit fitter = new VolumeTensorFit();
        fitter.input = this.dwi;
        fitter.gradients = this.input;
        fitter.mask = this.mask;
        fitter.method = VolumeTensorFit.TensorFitType.LLS;
        final Volume tensors = fitter.run().output;

        Function<String,Volume> feature = (name) ->
        {
            VolumeModelFeature runner = new VolumeModelFeature();
            runner.input = tensors;
            runner.feature = name;
            return runner.run().output;
        };

        Volume b0 = feature.apply(Tensor.FEATURES_S0);
        Volume pd = feature.apply(Tensor.FEATURES_PD);
        Volume fa = feature.apply(Tensor.FEATURES_FA);

        Mask mymask = this.mask;

        if (mymask == null)
        {
            // this is not in the paper, but it will faill if you include background voxels
            Logging.info("extracting foreground mask");
            VolumeSegmentForegroundOtsu module = new VolumeSegmentForegroundOtsu();
            module.input = b0;
            mymask = module.run().output;
        }

        Logging.info("fitting initial tensors");
        VolumeThresholdOtsu thresher = new VolumeThresholdOtsu();
        thresher.input = fa;
        thresher.mask = mymask;
        Mask fg = thresher.run().output;

        MaskLargest larger = new MaskLargest();
        larger.input = fg;
        fg = larger.run().output;

        Logging.info("foreground size: " + MaskUtils.count(fg));

        Map<String, Matrix> init = Maps.newLinkedHashMap();
        init.put("FlipNoSwapNo", MatrixSource.ident());
        init.put("FlipXSwapNo", MatrixSource.flipX());
        init.put("FlipYSwapNo", MatrixSource.flipY());
        init.put("FlipZSwapNo", MatrixSource.flipZ());
        init.put("FlipNoSwapXY", MatrixSource.ident().times(MatrixSource.swapXY()));
        init.put("FlipXSwapXY", MatrixSource.flipX().times(MatrixSource.swapXY()));
        init.put("FlipYSwapXY", MatrixSource.flipY().times(MatrixSource.swapXY()));
        init.put("FlipZSwapXY", MatrixSource.flipZ().times(MatrixSource.swapXY()));
        init.put("FlipNoSwapXZ", MatrixSource.ident().times(MatrixSource.swapXZ()));
        init.put("FlipXSwapXZ", MatrixSource.flipX().times(MatrixSource.swapXZ()));
        init.put("FlipYSwapXZ", MatrixSource.flipY().times(MatrixSource.swapXZ()));
        init.put("FlipZSwapXZ", MatrixSource.flipZ().times(MatrixSource.swapXZ()));
        init.put("FlipNoSwapYZ", MatrixSource.ident().times(MatrixSource.swapYZ()));
        init.put("FlipXSwapYZ", MatrixSource.flipX().times(MatrixSource.swapYZ()));
        init.put("FlipYSwapYZ", MatrixSource.flipY().times(MatrixSource.swapYZ()));
        init.put("FlipZSwapYZ", MatrixSource.flipZ().times(MatrixSource.swapYZ()));

        Map<String, Matrix> mats = Maps.newLinkedHashMap();
        Matrix qform = this.dwi.getSampling().quat().matrix();

        // check whether the volume has a rotation
        boolean useqform = qform.minus(MatrixSource.identity(3)).norm2() > 1e-3;

        for (String name : init.keySet())
        {
            Matrix mat = init.get(name);

            if (useqform)
            {
                mats.put("QformNo" + name, mat);
                mats.put("QformYes" + name, qform.times(mat));
            }
            else
            {
                mats.put(name, mat);
            }
        }

        Sampling sampling = pd.getSampling();
        double angle = Math.cos(Math.toRadians(30));
        String bestKey = null;
        Double bestCoh = null;

        double dmax = sampling.deltaMax();
        double dmin = sampling.deltaMin();

        for (String matkey : mats.keySet())
        {
            Matrix mat = mats.get(matkey);

            double coh = 0;

            for (Sample sample : pd.getSampling())
            {
                if (pd.valid(sample, fg))
                {
                    Vect pos = sampling.world(sample);
                    Vect pdv = mat.times(pd.get(sample));
                    double fav = fa.get(sample, 0);

                    for (Integers offset : Global.NEIGHBORS_27)
                    {
                        Sample nsample = sample.offset(offset);
                        if (pd.valid(nsample, fg))
                        {
                            Vect npos = sampling.world(nsample);
                            Vect npdv = mat.times(pd.get(nsample));
                            double nfav = fa.get(nsample, 0);

                            Vect delta = npos.minus(pos);
                            Vect line = delta.normalize();
                            double dist = delta.norm();
                            double dscale = this.nospat ? 1.0 : 1.0 + (dmin - dist) / dmax;

                            double dot = Math.abs(line.dot(pdv)) > angle ? 1.0 : 0;
                            double ndot = Math.abs(line.dot(npdv)) > angle ? 1.0 : 0;
                            double ncoh = dscale * dot * ndot * (fav + nfav);

                            coh += ncoh;
                        }
                    }
                }
            }

            Logging.info(String.format("... transform %s has coherence %g", matkey, coh));

            if (bestCoh == null || coh > bestCoh)
            {
                bestCoh = coh;
                bestKey = matkey;
            }
        }

        Logging.info("found optimal transform: " + bestKey);
        Logging.info("applying transform to gradients");
        this.output = this.input.transform(Affine.linear(mats.get(bestKey)));

        return this;
    }
}
