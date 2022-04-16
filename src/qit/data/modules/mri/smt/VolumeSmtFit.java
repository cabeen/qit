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

package qit.data.modules.mri.smt;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;
import smile.math.special.Erf;

import java.util.List;

@ModuleDescription("Estimate microscopic diffusion parameters")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Kaden, E., Kruggel, F., & Alexander, D. C. (2016). Quantitative mapping of the per-axon diffusion coefficients in brain white matter. Magnetic resonance in medicine, 75(4), 1752-1763.")
public class VolumeSmtFit implements Module
{
    private static final double SQRTPI = Math.sqrt(Math.PI);

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
    @ModuleDescription("the input number of threads")
    public int threads = 1;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum number of iterations")
    public int maxiter = 5000;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the starting simplex size")
    public double rhobeg = 0.1;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the ending simplex size")
    public double rhoend = 1e-6;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for perpendicular diffusivity")
    public double dperp = 0.5e-3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for parallel diffusivity")
    public double dpar = 1.7e-3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum diffusivity")
    public double dmax = 3.0e-3;

    @ModuleOutput
    @ModuleDescription("the output parameter map")
    public Volume output;

    public VolumeSmtFit run()
    {
        this.output = new VolumeFunction(this.fitter()).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        return this;
    }

    public VectFunction fitter()
    {
        final List<Pair<Integer, List<Integer>>> shells = Lists.newArrayList();
        for (int shell : this.gradients.getShells(true))
        {
            shells.add(Pair.of(shell, this.gradients.getShellsIdx(shell)));
        }

        final Vect bvals = VectSource.createND(shells.size());
        for (int i = 0; i < shells.size(); i++)
        {
            bvals.set(i, shells.get(i).a);
        }

        Logging.info("bvals: " + bvals.toString());

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double s0 = VolumeSmtFit.this.gradients.zero(input);

                if (MathUtils.zero(s0))
                {
                    output.setAll(0);
                    return;
                }
                else
                {
                    Vect means = VectSource.createND(bvals.size());
                    for (int j = 0; j < shells.size(); j++)
                    {
                        means.set(j, input.sub(shells.get(j).b).mean());
                    }

                    output.set(fit(bvals, means));
                }
            }
        }.init(this.gradients.size(), 6);
    }

    public Vect fit(Vect bvals, Vect means)
    {
        double scale = means.get(0);
        double[] p = {1.0, 1000 * this.dpar, 1000 * this.dperp};
        int cons = 6;

        Calcfc func = new Calcfc()
        {
            @Override
            public double Compute(int n, int m, double[] x, double[] con)
            {
                double b0 = scale * x[0];
                double dpar = x[1] / 1000.0;
                double dperp = x[2] / 1000.0;

                con[0] = b0;
                con[1] = dpar;
                con[2] = dperp;
                con[3] = VolumeSmtFit.this.dmax - dpar;
                con[4] = VolumeSmtFit.this.dmax - dperp;
                con[5] = dpar - dperp;

                double cost = VolumeSmtFit.this.synth(bvals, b0, dpar, dperp).minus(means).norm2();

                cost /= scale * scale;
                cost /= means.size();

                return cost;
            }
        };
        Cobyla.FindMinimum(func, p.length, cons, p, this.rhobeg, this.rhoend, 0, this.maxiter);

        double b0 = scale * p[0];
        double dpar = p[1] / 1000.0;
        double dperp = p[2] / 1000.0;
        double md = (2 * dpar + dperp) / 3.0;

        double ddpar = dpar - md;
        double ddperp = dperp - md;

        double num = 2.0 * ddpar * ddpar + ddperp * ddperp;
        double den = 2.0 * dpar * dpar + dperp * dperp;
        double fa = Math.sqrt(1.5 * num / den);
        double fapow = Math.pow(fa, 3);

        Vect out = VectSource.createND(6);
        out.set(0, b0);
        out.set(1, dpar);
        out.set(2, dperp);
        out.set(3, md);
        out.set(4, fa);
        out.set(5, fapow);

        return out;
    }

    public Vect synth(Vect bvals, double b0, double dpar, double dperp)
    {
        Vect out = VectSource.createND(bvals.size());

        for (int i = 0; i < bvals.size(); i++)
        {
            out.set(i, b0);

            if (MathUtils.nonzero(b0) && dpar > dperp)
            {
                double b = bvals.get(i);
                double delta = Math.sqrt(b * (dpar - dperp));
                double erf = Erf.erf(delta);
                double e = b0 * Math.exp(-b * dperp) * SQRTPI * erf / (2.0 * delta);

                if (Double.isFinite(e))
                {
                    out.set(i, e);
                }
            }
        }

        return out;
    }
}
