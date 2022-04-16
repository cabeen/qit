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

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Correct for signal drift in a diffusion-weighted MR volume using a polynomial model")
@ModuleCitation("Vos, Sjoerd B., et al. \"The importance of correcting for signal drift in diffusion MRI.\" Magnetic resonance in medicine 77.1 (2017): 285-299.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiCorrectDrift implements Module
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

    @ModuleParameter
    @ModuleDescription("use global drift estimation")
    public boolean global;

    @ModuleParameter
    @ModuleDescription("use local drift estimation")
    public boolean local;

    @ModuleParameter
    @ModuleDescription("use isotropic drift estimation")
    public boolean iso;

    @ModuleParameter
    @ModuleDescription("use blended drift estimation")
    public boolean blend;

    @ModuleParameter
    @ModuleDescription("the order of the polynomial model")
    public int order = 2;

    @ModuleOutput
    @ModuleDescription("the output corrected dwi")
    public Volume output;

    public VolumeDwiCorrectDrift run()
    {
        Volume volume = this.input;

        if (this.global)
        {
            volume = this.global(volume);
        }

        if (this.local)
        {
            volume = this.local(volume);
        }

        if (this.iso)
        {
            volume = this.iso(volume);
        }

        if (this.blend)
        {
            volume = this.blend(volume);
        }

        this.output = volume;

        return this;
    }

    public Volume global(Volume volume)
    {
        final List<Integer> idxBase = this.gradients.getBaselineIdx();
        final int numBase = idxBase.size();

        Global.assume(idxBase.size() > 1, "the input DWI must have more than one baseline");
        Volume out = volume.proto();

        VectOnlineStats meanStats = new VectOnlineStats();
        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, this.mask))
            {
                Vect baselines = volume.get(sample).sub(idxBase);
                double mean = baselines.mean();
                meanStats.update(mean);
            }
        }

        double low = meanStats.mean - 2.0 * meanStats.std;
        double high = meanStats.mean + 2.0 * meanStats.std;

        List<VectOnlineStats> stats = Lists.newArrayList();
        for (int i = 0; i < numBase; i++)
        {
            stats.add(new VectOnlineStats());
        }

        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, this.mask))
            {
                Vect baselines = volume.get(sample).sub(idxBase);
                double mean = baselines.mean();

                if (mean > low && mean < high)
                {
                    for (int i = 0; i < numBase; i++)
                    {
                        stats.get(i).update(baselines.get(i));
                    }
                }
            }
        }

        Matrix A = new Matrix(numBase, VectSource.poly(0, this.order).size());
        Vect b = VectSource.createND(numBase);
        for (int i = 0; i < numBase; i++)
        {
            A.setRow(i, VectSource.poly(idxBase.get(i), this.order));
            b.set(i, stats.get(i).mean);
        }
        Vect coeff = MatrixUtils.solve(A, b);

        Vect scale = VectSource.createND(volume.getDim());
        for (int i = 0; i < volume.getDim(); i++)
        {
            double y = coeff.times(VectSource.poly(i, this.order)).sum();
            double s = MathUtils.nonzero(y) ? meanStats.mean / y : 0;
            scale.set(i, s);
        }

        for (int i = 0; i < volume.getDim(); i++)
        {
            Logging.info(String.format("... global decay at index %d is %g", i, scale.get(i)));
        }

        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, this.mask))
            {
                out.set(sample, volume.get(sample).times(scale));
            }
        }

        return out;
    }

    public Volume local(Volume volume)
    {
        List<Integer> idxBase = this.gradients.getBaselineIdx();

        Global.assume(idxBase.size() > 1, "the input DWI must have more than one baseline");
        Volume out = volume.proto();

        Matrix A = new Matrix(idxBase.size(), VectSource.poly(0, this.order).size());
        for (int i = 0; i < A.rows(); i++)
        {
            double bi = idxBase.get(i);
            A.setRow(i, VectSource.poly(bi, this.order));
        }

        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, this.mask))
            {
                Vect baselines = volume.get(sample).sub(idxBase);
                Vect coeff = MatrixUtils.solve(A, baselines);

                double mean = baselines.mean();

                for (int i = 0; i < volume.getDim(); i++)
                {
                    double b = coeff.times(VectSource.poly(i, this.order)).sum();
                    double x = volume.get(sample, i);
                    double nx = MathUtils.nonzero(b) ? mean * x / b : 0;

                    out.set(sample, i, nx);
                }
            }
        }

        return out;
    }

    public Volume iso(Volume volume)
    {
        List<Integer> idx = this.gradients.getDvecIdx();
        Volume out = volume.proto();

        Matrix A = new Matrix(idx.size(), VectSource.poly(0, this.order).size());
        for (int i = 0; i < A.rows(); i++)
        {
            double bi = idx.get(i);
            A.setRow(i, VectSource.poly(bi, this.order));
        }

        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, this.mask))
            {
                Vect subset = volume.get(sample).sub(idx);
                Vect coeff = MatrixUtils.solve(A, subset);

                double mean = subset.mean();

                for (int i = 0; i < volume.getDim(); i++)
                {
                    double b = coeff.times(VectSource.poly(i, this.order)).sum();
                    double x = volume.get(sample, i);
                    double nx = MathUtils.nonzero(b) ? mean * x / b : 0;

                    out.set(sample, i, nx);
                }
            }
        }

        return out;
    }

    public Volume blend(Volume volume)
    {
        List<Integer> bidx = this.gradients.getBaselineIdx();
        List<Integer> didx = this.gradients.getDvecIdx();
        Volume out = volume.proto();

        Matrix bA = new Matrix(bidx.size(), VectSource.poly(0, this.order).size());
        for (int i = 0; i < bA.rows(); i++)
        {
            bA.setRow(i, VectSource.poly(bidx.get(i), this.order));
        }

        Matrix dA = new Matrix(didx.size(), VectSource.poly(0, this.order).size());
        for (int i = 0; i < dA.rows(); i++)
        {
            dA.setRow(i, VectSource.poly(didx.get(i), this.order));
        }

        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, this.mask))
            {
                Vect dsig = volume.get(sample).sub(didx);
                Vect bsig = volume.get(sample).sub(bidx);

                Vect dcoef = MatrixUtils.solve(dA, dsig);
                Vect bcoef = MatrixUtils.solve(bA, bsig);

                double dmean = dsig.mean();
                double bmean = bsig.mean();

                for (int i = 0; i < volume.getDim(); i++)
                {
                    double dpred = dcoef.times(VectSource.poly(i, this.order)).sum();
                    double bpred = bcoef.times(VectSource.poly(i, this.order)).sum();
                    double x = volume.get(sample, i);

                    double dcorr = MathUtils.nonzero(dpred) ? dmean * x / dpred : 0;
                    double bcorr = MathUtils.nonzero(bpred) ? bmean * x / bpred : 0;

                    double blend = Math.max(0.0, Math.min(1.0, (x - dmean + 1e-6) / (bmean - dmean + 1e-6)));
                    double corr = blend * bcorr + (1.0 - blend) * dcorr;

                    out.set(sample, i, corr);
                }
            }
        }

        return out;
    }
}
