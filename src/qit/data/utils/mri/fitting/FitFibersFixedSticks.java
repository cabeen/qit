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

import qit.base.Logging;
import qit.base.annot.*;
import qit.data.datasets.Vect;
import qit.data.models.Fibers;
import qit.data.models.Mcsmt;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.structs.Shells;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

@ModuleDescription("Estimate the diffusivity and total fraction with fixed stick orientations and other constraints")
@ModuleAuthor("Ryan Cabeen")
public class FitFibersFixedSticks
{
    public enum Mode {Fracs, FracsFixedSum, FracsDiff, FracSum, FracSumDiff, Diff, FracSumDiffMultistage, FracsDiffMultistage};

    public static final int DEFAULT_MAXITERS = 1000;
    public static final double DEFAULT_RHOBEG = 0.5;
    public static final double DEFAULT_RHOEND = 1e-3;
    public static final double DEFAULT_DINT = 1.7e-3;
    public static final double DEFAULT_FRAC = 0.5;
    public static final double DEFAULT_FSCALE = 10.0;
    public static final double DEFAULT_DSCALE = 1000;
    public static final double DEFAULT_DMAX = 3.0e-3;
    public static final double DEFAULT_DFIX = 1.25e-3;
    public static final int DEFAULT_RESTARTS = 1;

    @ModuleParameter
    @ModuleDescription("specify a fitting mode (determines which parameters are constrained)")
    public Mode mode = Mode.FracSumDiffMultistage;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum number of iterations")
    public int maxiters = FitFibersFixedSticks.DEFAULT_MAXITERS;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the starting simplex size")
    public double rhobeg = FitFibersFixedSticks.DEFAULT_RHOBEG;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the ending simplex size")
    public double rhoend = FitFibersFixedSticks.DEFAULT_RHOEND;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for parallel diffusivity")
    public double dint = FitFibersFixedSticks.DEFAULT_DINT;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the initial guess for intra-neurite volume fraction")
    public double frac = FitFibersFixedSticks.DEFAULT_FRAC;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the scaling for volume fraction optimization")
    public double fscale = FitFibersFixedSticks.DEFAULT_FSCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the scaling for diffusivity optimization")
    public double dscale = FitFibersFixedSticks.DEFAULT_DSCALE;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum diffusivity")
    public double dmax = FitFibersFixedSticks.DEFAULT_DMAX;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use the given fraction of axial diffusivity for the perpendicular direction")
    public Double dperp = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use a tortuosity model")
    public boolean tort = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the number of iterations for multi-stage fitting")
    public Integer multistages = 2;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the number of iterations for multi-stage fitting")
    public Integer restarts = FitFibersFixedSticks.DEFAULT_RESTARTS;

    public Fibers fit(Gradients gradients, Vect input, Fibers fibers)
    {
        Fibers bestModel = fibers.copy();
        double bestCost = Double.MAX_VALUE;

        for (int i = 0; i < this.restarts; i++)
        {
            Fibers myModel = fibers.copy();

            switch (this.mode)
            {
                case Fracs:
                    myModel = fit(gradients, input, myModel, new FibersFracs());
                    break;
                case FracsFixedSum:
                    myModel = fit(gradients, input, myModel, new FibersFracsFixedSum(fibers.getFracSum()));
                    break;
                case FracSum:
                    myModel = fit(gradients, input, myModel, new FibersFracSum());
                    break;
                case FracsDiff:
                    myModel = fit(gradients, input, myModel, new FibersFracsDiff());
                    break;
                case FracSumDiff:
                    myModel = fit(gradients, input, myModel, new FibersFracSumDiff());
                    break;
                case Diff:
                    myModel = fit(gradients, input, myModel, new FibersDiff());
                    break;
                case FracSumDiffMultistage:
                    for (int j = 0; j < this.multistages; j++)
                    {
                        myModel = fit(gradients, input, myModel, new FibersFracSum());
                        myModel = fit(gradients, input, myModel, new FibersDiff());
                    }
                    break;
                case FracsDiffMultistage:
                    myModel = fit(gradients, input, myModel, new FibersFracSum());
                    myModel = fit(gradients, input, myModel, new FibersDiff());
                    for (int j = 0; j < this.multistages; j++)
                    {
                        myModel = fit(gradients, input, myModel, new FibersFracs());
                        myModel = fit(gradients, input, myModel, new FibersDiff());
                    }
                    break;
            }

            double myCost = cost(gradients, input, myModel);

            if (myCost < bestCost)
            {
                bestModel = myModel;
                bestCost = myCost;
            }
        }

        return bestModel;
    }

    public double cost(Gradients gradients, Vect input, Fibers model)
    {
        Vect synth = model.synth(gradients, this.dperp, this.tort);
        // return ModelUtils.rmse(input, synth) / model.getBaseline();
        return ModelUtils.rmse(input, synth);
    }

    public Fibers fit(Gradients gradients, Vect input, Fibers fibers, FibersParam paramer)
    {
        Fibers model = fibers.sort();
        double[] param = paramer.param(model);
        int cons = paramer.constraints(model);

        Calcfc func = (n, m, x, con) ->
        {
            Fibers current = paramer.model(x, model);
            paramer.constrain(current, con);
            return cost(gradients, input, current);
        };
        Cobyla.FindMinimum(func, param.length, cons, param, this.rhobeg, this.rhoend, 0, this.maxiters);

        return paramer.model(param, model);
    }

    private interface FibersParam
    {
        int constraints(Fibers model);

        void constrain(Fibers model, double[] cons);

        double[] param(Fibers model);

        Fibers model(double[] param, Fibers model);
    }

    private class FibersFracsDiff implements FibersParam
    {
        public int constraints(Fibers model)
        {
            return 2 * (model.size() + 2);
        }

        public void constrain(Fibers model, double[] cons)
        {
            int comps = model.size();

            for (int i = 0; i < comps; i++)
            {
                cons[2 * i + 0] = model.getFrac(i);
                cons[2 * i + 1] = 1.0 - model.getFrac(i);
            }

            cons[2 * comps + 0] = model.getFracSum();
            cons[2 * comps + 1] = 1.0 - model.getFracSum();

            cons[2 * comps + 2] = model.getDiffusivity();
            cons[2 * comps + 3] = FitFibersFixedSticks.this.dmax - model.getDiffusivity();
        }

        public double[] param(Fibers model)
        {
            int comps = model.size();

            double[] out = new double[comps + 1];
            for (int i = 0; i < comps; i++)
            {
                out[i] = model.getFrac(i) * FitFibersFixedSticks.this.fscale;
            }

            out[comps] = model.getDiffusivity() * FitFibersFixedSticks.this.dscale;

            return out;
        }

        public Fibers model(double[] param, Fibers model)
        {
            int comps = model.size();
            Fibers out = model.copy();

            for (int i = 0; i < comps; i++)
            {
                out.setFrac(i, param[i] / FitFibersFixedSticks.this.fscale);
            }

            out.setDiffusivity(param[comps] / FitFibersFixedSticks.this.dscale);

            return out;
        }
    }

    private class FibersFracs implements FibersParam
    {
        public int constraints(Fibers model)
        {
            return 2 * (model.size() + 1);
        }

        public void constrain(Fibers model, double[] cons)
        {
            int comps = model.size();

            for (int i = 0; i < comps; i++)
            {
                cons[2 * i + 0] = model.getFrac(i);
                cons[2 * i + 1] = 1.0 - model.getFrac(i);
            }

            cons[2 * comps + 0] = model.getFracSum();
            cons[2 * comps + 1] = 1.0 - model.getFracSum();
        }

        public double[] param(Fibers model)
        {
            int comps = model.size();

            double[] out = new double[comps];
            for (int i = 0; i < comps; i++)
            {
                out[i] = model.getFrac(i) * FitFibersFixedSticks.this.fscale;
            }

            return out;
        }

        public Fibers model(double[] param, Fibers model)
        {
            int comps = model.size();
            Fibers out = model.copy();

            for (int i = 0; i < comps; i++)
            {
                out.setFrac(i, param[i] / FitFibersFixedSticks.this.fscale);
            }

            return out;
        }
    }

    private class FibersFracsFixedSum implements FibersParam
    {
        double fsum;

        FibersFracsFixedSum(double fsum)
        {
            this.fsum = fsum;
        }

        public int constraints(Fibers model)
        {
            return 2 * (model.size() + 1);
        }

        public void constrain(Fibers model, double[] cons)
        {
            int comps = model.size();

            for (int i = 0; i < comps; i++)
            {
                cons[2 * i + 0] = model.getFrac(i);
                cons[2 * i + 1] = 1.0 - model.getFrac(i);
            }

            cons[2 * comps + 0] = model.getFracSum() - 0.95 * this.fsum;
            cons[2 * comps + 1] = 1.05 * this.fsum - model.getFracSum();
        }

        public double[] param(Fibers model)
        {
            int comps = model.size();

            double[] out = new double[comps];
            for (int i = 0; i < comps; i++)
            {
                out[i] = model.getFrac(i) * FitFibersFixedSticks.this.fscale;
            }

            return out;
        }

        public Fibers model(double[] param, Fibers model)
        {
            int comps = model.size();
            Fibers out = model.copy();

            for (int i = 0; i < comps; i++)
            {
                out.setFrac(i, param[i] / FitFibersFixedSticks.this.fscale);
            }

            return out;
        }
    }

    private class FibersFracSum implements FibersParam
    {
        public int constraints(Fibers model)
        {
            return 2;
        }

        public void constrain(Fibers model, double[] cons)
        {
            cons[0] = model.getFracSum();
            cons[1] = 1.0 - model.getFracSum();
        }

        public double[] param(Fibers model)
        {
            return new double[]{model.getFracSum() * FitFibersFixedSticks.this.fscale};
        }

        public Fibers model(double[] param, Fibers model)
        {
            Fibers out = model.copy();
            out.scale(param[0] / FitFibersFixedSticks.this.fscale / model.getFracSum());
            return out;
        }
    }

    private class FibersDiff implements FibersParam
    {
        public int constraints(Fibers model)
        {
            return 2;
        }

        public void constrain(Fibers model, double[] cons)
        {
            cons[0] = model.getDiffusivity();
            cons[1] = 1.0 - model.getDiffusivity();
        }

        public double[] param(Fibers model)
        {
            return new double[]{model.getDiffusivity() * FitFibersFixedSticks.this.dscale};
        }

        public Fibers model(double[] param, Fibers model)
        {
            Fibers out = model.copy();
            out.setDiffusivity(param[0] / FitFibersFixedSticks.this.dscale);
            return out;
        }
    }

    private class FibersFracSumDiff implements FibersParam
    {
        public int constraints(Fibers model)
        {
            return 4;
        }

        public void constrain(Fibers model, double[] cons)
        {
            cons[0] = model.getFracSum();
            cons[1] = 1.0 - model.getFracSum();
            cons[2] = model.getDiffusivity();
            cons[3] = 1.0 - model.getDiffusivity();
        }

        public double[] param(Fibers model)
        {
            double[] out = new double[2];
            out[0] = model.getFracSum() * FitFibersFixedSticks.this.fscale;
            out[1] = model.getDiffusivity() * FitFibersFixedSticks.this.dscale;
            return out;
        }

        public Fibers model(double[] param, Fibers model)
        {
            Fibers out = model.copy();
            out.scale(param[0] / FitFibersFixedSticks.this.fscale / out.getFracSum());
            out.setDiffusivity(param[1] / FitFibersFixedSticks.this.dscale);
            return out;
        }
    }
}
