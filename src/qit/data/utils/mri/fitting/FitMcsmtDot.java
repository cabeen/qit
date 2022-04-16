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

package qit.data.utils.mri.fitting;

import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Vect;
import qit.data.models.Mcsmt;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.structs.Shells;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

@ModuleDescription("Estimate multi-compartment microscopic diffusion parameters")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Kaden, E., Kelm, N. D., Carson, R. P., Does, M. D., & Alexander, D. C. (2016). Multi-compartment microscopic diffusion imaging. NeuroImage, 139, 346-359.")
public class FitMcsmtDot
{
    public static final int DEFAULT_MAXITERS = 1500;
    public static final double DEFAULT_RHOBEG = 0.1;
    public static final double DEFAULT_RHOEND = 1e-3;
    public static final double DEFAULT_DINT = 1.7e-3;
    public static final double DEFAULT_FRAC = 0.5;
    public static final double DEFAULT_DOT = 0.01;
    public static final double DEFAULT_FSCALE = 10.0;
    public static final double DEFAULT_DSCALE = 1000;
    public static final double DEFAULT_DMAX = 3.0e-3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum number of iterations")
    public int maxiters = FitMcsmtDot.DEFAULT_MAXITERS;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the starting simplex size")
    public double rhobeg = FitMcsmtDot.DEFAULT_RHOBEG;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the ending simplex size")
    public double rhoend = FitMcsmtDot.DEFAULT_RHOEND;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for parallel diffusivity")
    public double dint = FitMcsmtDot.DEFAULT_DINT;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for intra-neurite volume fraction")
    public double frac = FitMcsmtDot.DEFAULT_FRAC;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for the dot volume fraction")
    public double dot = FitMcsmtDot.DEFAULT_DOT;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the scaling for volume fraction optimization")
    public double fscale = FitMcsmtDot.DEFAULT_FSCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the scaling for diffusivity optimization")
    public double dscale = FitMcsmtDot.DEFAULT_DSCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum diffusivity")
    public double dmax = FitMcsmtDot.DEFAULT_DMAX;

    public VectFunction fitter(Gradients gradients)
    {
        Shells sheller = new Shells(gradients);
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                if (MathUtils.zero(gradients.zero(input)))
                {
                    output.setAll(0);
                    return;
                }
                else
                {
                    output.set(fit(sheller.shells(), sheller.mean(input)));
                }
            }
        }.init(gradients.size(), new Mcsmt().getEncodingSize());
    }

    public Vect fit(Vect shells, Vect means)
    {
        double b0 = means.get(0);
        double[] p = {this.fscale * this.frac, this.dscale * this.dint, this.fscale * this.dot};
        int cons = 6;

        Calcfc func = (n, m, x, con) ->
        {
            double frac = x[0] / FitMcsmtDot.this.fscale;
            double diff = x[1] / FitMcsmtDot.this.dscale;
            double dot = x[2] / FitMcsmtDot.this.fscale;

            con[0] = diff;
            con[1] = FitMcsmtDot.this.dmax - diff;
            con[2] = frac;
            con[3] = 1.0 - frac;
            con[4] = dot;
            con[5] = 1.0 - dot;

            double cost = new Mcsmt(b0, frac, diff, dot).synth(shells).minus(means).norm() / b0;

            return cost;
        };
        Cobyla.FindMinimum(func, p.length, cons, p, this.rhobeg, this.rhoend, 0, this.maxiters);

        double frac = p[0] / this.fscale;
        double diff = p[1] / this.dscale;
        double dot = p[2] / this.fscale;

        Mcsmt out = new Mcsmt();
        out.setBase(b0);
        out.setFrac(frac);
        out.setDiff(diff);
        out.setDot(dot);

        return out.getEncoding();
    }
}