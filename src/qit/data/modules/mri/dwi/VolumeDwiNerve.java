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

import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.Tensor;
import qit.data.source.VectSource;
import qit.data.utils.mri.fitting.FitTensorLLS;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

@ModuleDescription("Estimate microstructural parameters of nerves")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class VolumeDwiNerve implements Module
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
    @ModuleDescription("the intra-axonal diffusivity")
    public double dintra = 1.7e-3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the fluid diffusivity")
    public double dfluid = 3e-3;

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
    public double dmax = 3.0e-3;

    @ModuleOutput
    @ModuleDescription("the output parameter map")
    public Volume output;

    public VolumeDwiNerve run()
    {
        this.output = new VolumeFunction(this.fitterFluid()).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        return this;
    }

    public VectFunction fitter()
    {
        final Gradients grads = VolumeDwiNerve.this.gradients;

        FitTensorLLS lls = new FitTensorLLS();
        lls.weighted = false;
        lls.gradients = grads;
        final VectFunction initfit = lls.get();

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                final double s0 = VolumeDwiNerve.this.gradients.zero(input);

                if (MathUtils.zero(s0))
                {
                    output.setAll(0);
                    return;
                }
                else
                {
                    Tensor teninit = new Tensor(initfit.apply(input));
                    double fa = teninit.feature(Tensor.FEATURES_FA).get(0);
                    double ad = teninit.feature(Tensor.FEATURES_AD).get(0);
                    final Vect pd = teninit.feature(Tensor.FEATURES_PD);

                    Vect q2s = VectSource.createND(grads.size());

                    for (int i = 0; i < grads.size(); i++)
                    {
                        Vect q = grads.getBvec(i);
                        double qdotd = q.dot(pd);
                        double q2 = qdotd * qdotd;
                        q2s.set(i, q2);
                    }

                    double[] p = {VolumeDwiNerve.this.fscale * fa, VolumeDwiNerve.this.dscale * ad};
                    int cons = 4;

                    Calcfc func = new Calcfc()
                    {
                        @Override
                        public double Compute(int n, int m, double[] x, double[] con)
                        {
                            double frac = x[0] / VolumeDwiNerve.this.fscale;
                            double dintra = x[1] / VolumeDwiNerve.this.dscale;

                            double ifrac = 1.0 - frac;
                            double dtrans = ifrac * dintra;

                            con[0] = dintra;
                            con[1] = VolumeDwiNerve.this.dmax - dintra;
                            con[2] = frac;
                            con[3] = 1.0 - frac;

                            Vect pred = VectSource.createND(VolumeDwiNerve.this.gradients.size());

                            for (int i = 0; i < VolumeDwiNerve.this.gradients.size(); i++)
                            {
                                double s = 0;

                                if (MathUtils.nonzero(s0))
                                {
                                    double b = grads.getBval(i);
                                    double q2 = q2s.get(i);

                                    double extra = Math.exp(-b * (dtrans * (1.0 - q2) + dintra * q2));
                                    double intra = Math.exp(-b * dintra * q2);

                                    s = s0 * (frac * intra + ifrac * extra);
                                }

                                pred.set(i, s);
                            }

                            double cost = pred.minus(input).norm() / s0;

                            return cost;
                        }
                    };
                    Cobyla.FindMinimum(func, p.length, cons, p, VolumeDwiNerve.this.rhobeg, VolumeDwiNerve.this.rhoend, 0, VolumeDwiNerve.this.maxiter);

                    double frac = p[0] / VolumeDwiNerve.this.fscale;
                    double dint = p[1] / VolumeDwiNerve.this.dscale;

                    output.set(0, s0);
                    output.set(1, frac);
                    output.set(2, dint);
                }
            }
        }.init(this.gradients.size(), 3);
    }

    public VectFunction fitterFluid()
    {
        final Gradients grads = VolumeDwiNerve.this.gradients;

        FitTensorLLS lls = new FitTensorLLS();
        lls.weighted = false;
        lls.gradients = grads;
        final VectFunction initfit = lls.get();

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                final double s0 = VolumeDwiNerve.this.gradients.zero(input);

                if (MathUtils.zero(s0))
                {
                    output.setAll(0);
                    return;
                }
                else
                {
                    Tensor teninit = new Tensor(initfit.apply(input));
                    double fa = teninit.feature(Tensor.FEATURES_FA).get(0);
                    double ad = teninit.feature(Tensor.FEATURES_AD).get(0);
                    final Vect pd = teninit.feature(Tensor.FEATURES_PD);

                    Vect q2s = VectSource.createND(grads.size());

                    for (int i = 0; i < grads.size(); i++)
                    {
                        Vect q = grads.getBvec(i);
                        double qdotd = q.dot(pd);
                        double q2 = qdotd * qdotd;
                        q2s.set(i, q2);
                    }

                    double dintraInit = VolumeDwiNerve.this.dscale * ad;
                    double fintraInit = VolumeDwiNerve.this.fscale * fa;
                    double fisoInit = VolumeDwiNerve.this.fscale * (1.0 - fa);
                    double[] p = {dintraInit, fintraInit, fisoInit};
                    int cons = 6;

                    Calcfc func = new Calcfc()
                    {
                        @Override
                        public double Compute(int n, int m, double[] x, double[] con)
                        {
                            double dintra = x[0] / VolumeDwiNerve.this.dscale;
                            double fintra = x[1] / VolumeDwiNerve.this.fscale;
                            double fiso = x[2] / VolumeDwiNerve.this.fscale;

                            double ftissue = 1.0 - fiso;
                            double fextra = 1.0 - fintra;
                            double dlong = dintra;
                            double dtrans = fextra * dlong;

                            con[0] = fintra;
                            con[1] = 1.0 - fintra;
                            con[2] = fiso;
                            con[3] = 1.0 - fiso;
                            con[4] = dintra;
                            con[5] = VolumeDwiNerve.this.dmax - dintra;

                            Vect pred = VectSource.createND(grads.size());

                            for (int i = 0; i < grads.size(); i++)
                            {
                                double s = 0;

                                if (MathUtils.nonzero(s0))
                                {
                                    double b = grads.getBval(i);
                                    double q2 = q2s.get(i);

                                    double sextra = Math.exp(-b * (dtrans * (1.0 - q2) + dlong * q2));
                                    double sintra = Math.exp(-b * dintra * q2);
                                    double stissue = (fintra * sintra + fextra * sextra);
                                    double sfluid = Math.exp(-b * VolumeDwiNerve.this.dfluid);
                                    double stotal = (fiso * sfluid + ftissue * stissue);

                                    s = s0 * stotal;
                                }

                                pred.set(i, s);
                            }

                            double cost = pred.minus(input).norm() / s0;

                            return cost;
                        }
                    };
                    Cobyla.FindMinimum(func, p.length, cons, p, VolumeDwiNerve.this.rhobeg, VolumeDwiNerve.this.rhoend, 0, VolumeDwiNerve.this.maxiter);

                    double dintra = p[0] / VolumeDwiNerve.this.dscale;
                    double fintra = p[1] / VolumeDwiNerve.this.fscale;
                    double fiso = p[2] / VolumeDwiNerve.this.fscale;

                    output.set(0, s0);
                    output.set(1, dintra);
                    output.set(2, fintra);
                    output.set(3, fiso);
                }
            }
        }.init(this.gradients.size(), 4);
    }
}
