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
import qit.base.annot.*;
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
import java.util.function.Function;

@ModuleDescription("Estimate multi-compartment diffusion parameters with some new parameters")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Kaden, E., Kruggel, F., & Alexander, D. C. (2016). Quantitative mapping of the per-axon diffusion coefficients in brain white matter. Magnetic resonance in medicine, 75(4), 1752-1763.")
@ModuleUnlisted
public class VolumeFluidSmtFit implements Module
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
    public double rhoend = 1e-3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for parallel diffusivity")
    public double di = 1.7e-3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for intra-neurite volume fraction")
    public double fi = 0.5;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for fluid volume fraction")
    public double ff = 0.05;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for fluid diffusivity")
    public double df = 3.0e-3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the scaling for volume fraction optimization")
    public double fscale = 10.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the scaling for diffusivity optimization")
    public double dscale = 1000;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum diffusivity")
    public double dmax = 5.0e-3;

    @ModuleOutput
    @ModuleDescription("the output parameter map")
    public Volume output;

    public VolumeFluidSmtFit run()
    {
        this.output = new VolumeFunction(this.fitter()).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        return this;
    }

    public VectFunction fitter()
    {
        final List<Pair<Integer, List<Integer>>> shellMap = Lists.newArrayList();
        for (int shell : this.gradients.getShells(true))
        {
            shellMap.add(Pair.of(shell, this.gradients.getShellsIdx(shell)));
        }

        final Vect shells = VectSource.createND(shellMap.size());
        for (int i = 0; i < shellMap.size(); i++)
        {
            shells.set(i, shellMap.get(i).a);
        }

        Logging.info("shells: " + shells.toString());

        Function<Vect, Vect> fit = (input) ->
        {
            Vect means = VectSource.createND(shells.size());
            for (int j = 0; j < shellMap.size(); j++)
            {
                means.set(j, input.sub(shellMap.get(j).b).mean());
            }

            double b0 = means.get(0);

            Vect out = VectSource.createND(5);
            out.set(0, b0);

            if (!MathUtils.zero(b0))
            {
                means.divEquals(b0);

                double[] p = {this.fscale * this.fi, this.fscale * this.ff, this.dscale * this.df};
                int cons = 7;

                Calcfc func = (n, m, x, con) ->
                {
                    double vfi = x[0] / this.fscale;
                    double vff = x[1] / this.fscale;
                    double vdf = x[2] / this.dscale;

                    con[0] = vfi;
                    con[1] = 1.0 - vfi;
                    con[2] = vff;
                    con[3] = 1.0 - vff;
                    con[4] = vdf;
                    con[5] = this.dmax - vdf;
                    con[6] = vdf - this.di;

                    Vect pred = VectSource.createND(shells.size());

                    for (int i = 0; i < shells.size(); i++)
                    {
                        double b = shells.get(i);

                        double tint = Math.sqrt(b * this.di);
                        double sint = vfi * SQRTPI * Erf.erf(tint) / (2.0 * tint);

                        double fex = 1.0 - vfi;
                        double dex = fex * this.di;
                        double tex = Math.sqrt(b * (this.di - dex));
                        double sex = fex * Math.exp(-b * dex) * SQRTPI * Erf.erf(tex) / (2.0 * tex);

                        double sfl = vff * Math.exp(-b * vdf);

                        double s = (1.0 - vff) * (sint + sex) + sfl;

                        if (Double.isFinite(s))
                        {
                            pred.set(i, s);
                        }
                    }

                    double cost = pred.minus(means).norm();

                    double ddf = vdf - this.df;
                    double sdf = 0.001;
                    cost += 0.00005 * ddf * ddf / (sdf * sdf);

                    return cost;
                };
                Cobyla.FindMinimum(func, p.length, cons, p, this.rhobeg, this.rhoend, 0, this.maxiter);

                double fi = p[0] / this.fscale;
                double ff = p[1] / this.fscale;
                double df = p[2] / this.dscale;

                out.set(1, fi);
                out.set(2, ff);
                out.set(3, df);
            }

            return out;
        };

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(fit.apply(input));
            }
        }.init(this.gradients.size(), 4);
    }
}
