/*******************************************************************************
 *
 * Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
 * All rights reserved.
 *
 * The Software remains the property of Ryan Cabeen ("the Author").
 *
 * The Software is distributed "AS IS" under this Licence solely for
 * non-commercial use in the hope that it will be useful, but in order
 * that the Author as a charitable foundation protects its assets for
 * the benefit of its educational and research purposes, the Author
 * makes clear that no condition is made or to be implied, nor is any
 * warranty given or to be implied, as to the accuracy of the Software,
 * or that it will be suitable for any particular purpose or for use
 * under any specific conditions. Furthermore, the Author disclaims
 * all responsibility for the use which is made of the Software. It
 * further disclaims any liability for the outcomes arising from using
 * the Software.
 *
 * The Licensee agrees to indemnify the Author and hold the
 * Author harmless from and against any and all claims, damages and
 * liabilities asserted by third parties (including claims for
 * negligence) which arise directly or indirectly from the use of the
 * Software or the sale of any products based on the Software.
 *
 * No part of the Software may be reproduced, modified, transmitted or
 * transferred in any form or by any means, electronic or mechanical,
 * without the express permission of the Author. The permission of
 * the Author is not required if the said reproduction, modification,
 * transmission or transference is done without financial return, the
 * conditions of this Licence are imposed upon the receiver of the
 * product, and all original and amended source code is included in any
 * transmitted product. You may be held legally responsible for any
 * copyright infringement that is caused or encouraged by your failure to
 * abide by these terms and conditions.
 *
 * You are not permitted under this Licence to use this Software
 * commercially. Use for which any financial return is received shall be
 * defined as commercial use, and includes (1) integration of all or part
 * of the source code or the Software into a product for sale or license
 * by or on behalf of Licensee to third parties or (2) use of the
 * Software or any derivative of it for research with the final aim of
 * developing software products for sale or license to a third party or
 * (3) use of the Software or any derivative of it for research with the
 * final aim of developing non-software products for sale or license to a
 * third party, or (4) use of the Software to provide any service to an
 * external organisation for which payment is received.
 *
 ******************************************************************************/


package qit.data.utils.mri.fitting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.math3.special.Erf;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.data.datasets.Vect;
import qit.data.models.Noddi;
import qit.data.models.Tensor;
import qit.data.source.VectSource;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;
import qit.math.utils.optim.jcobyla.CobylaExitStatus;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class FitNoddiSMT implements Supplier<VectFunction>
{
    public static final String NAME = "SMT";

    private static final double SQRTPI = Math.sqrt(Math.PI);
    public static final double THRESH_LOW = 0.15;
    public static final double THRESH_HIGH = 0.25;

    public static final CostType DEFAULT_COST = CostType.SE;

    public Gradients gradients;
    public CostType cost = DEFAULT_COST;
    public boolean skipdir = false;
    public boolean extra = false;
    public boolean full = false;
    public boolean faodi = false;
    public Double prior = 1.0;
    public double maxden = 1.0;
    public int maxiter = 1000;
    public double rhobeg = 0.05;
    public double rhoend = 1e-3;

    public VectFunction get()
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

        final Function<Double, Double> odiInit = this.faodi ? mapFAtoODI(this.gradients) : fa -> 0.5;

        FitTensorLLS lls = new FitTensorLLS();
        lls.gradients = this.gradients;
        final VectFunction tensorFitter = lls.get();
        final VectFunction syntherHigh = Noddi.synth(this.gradients, 0);
        final VectFunction syntherMed = Noddi.synth(this.gradients, 1);
        final VectFunction syntherLow = Noddi.synth(this.gradients, 2);
        final Function<Double, VectFunction> synthGet = odi -> odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;

        double dic = Noddi.INVIVO_PARALLEL;
        double diso = Noddi.INVIVO_ISOTROPIC;

        Vect aisos = VectSource.createND(shells.size());
        Vect aics = VectSource.createND(shells.size());

        for (int i = 0; i < shells.size(); i++)
        {
            double b = shells.get(i);

            if (MathUtils.nonzero(b))
            {
                double aiso = Math.exp(-b * diso);

                double tic = Math.sqrt(b * dic);
                double aic = SQRTPI * Erf.erf(tic) / (2.0 * tic);

                aisos.set(i, aiso);
                aics.set(i, aic);
            }
            else
            {
                aisos.set(i, 1.0);
                aics.set(i, 1.0);
            }
        }

        BiConsumer<Vect, Noddi> fitSMT = (input, model) ->
        {
            Vect means = VectSource.createND(shells.size());
            for (int j = 0; j < shellMap.size(); j++)
            {
                means.set(j, input.sub(shellMap.get(j).b).mean() / model.getBaseline());
            }

            Function<double[], Double> coster = (x) ->
            {
                double fiso = x[0];
                double fic = x[1];

                Vect pred = VectSource.createND(shells.size());

                for (int i = 0; i < shells.size(); i++)
                {
                    double b = shells.get(i);

                    if (MathUtils.nonzero(b))
                    {
                        // we only need to compute the extracellular signal inside this loop
                        double aiso = aisos.get(i);
                        double aic = aics.get(i);

                        double fex = 1.0 - fic;
                        double dex = fex * dic;
                        double tex = Math.sqrt(b * (dic - dex));
                        double aex = Math.exp(-b * dex) * SQRTPI * Erf.erf(tex) / (2.0 * tex);

                        if (Double.isFinite(aic) && Double.isFinite(aex))
                        {
                            double s = fiso * aiso + (1.0 - fiso) * (fic * aic + fex * aex);
                            pred.set(i, s);
                        }
                    }
                    else
                    {
                        pred.set(i, 1.0);
                    }
                }

                return pred.dist(means);
            };

            int cons = 4;
            List<double[]> ps = Lists.newArrayList();

            ps.add(new double[]{0.25, 0.75});
            ps.add(new double[]{0.75, 0.25});

            Noddi modelBest = model.copy();
            Double costBest = null;

            for (double[] p : ps)
            {
                Calcfc func = (n, m, x, con) ->
                {
                    double fiso = x[0];
                    double fic = x[1];

                    con[0] = fiso;
                    con[1] = 1.0 - fiso;
                    con[2] = fic;
                    con[3] = FitNoddiSMT.this.maxden - fic;

                    return coster.apply(x);
                };
                Cobyla.FindMinimum(func, p.length, cons, p, FitNoddiSMT.this.rhobeg, FitNoddiSMT.this.rhoend, 0, FitNoddiSMT.this.maxiter);

                double cost = coster.apply(p);

                if (costBest == null || cost < costBest)
                {
                    modelBest.setFISO(p[0]);
                    modelBest.setFICVF(p[1]);

                    costBest = cost;
                }
            }

            model.set(modelBest);
        };

        BiConsumer<Vect, Noddi> fitOdi = (input, model) ->
        {
            double[] p = {model.getODI()};
            int cons = 2;

            Calcfc func = (n, m, x, con) ->
            {
                double odi = x[0];

                Noddi noddi = model.copy();
                noddi.setODI(odi);

                con[0] = odi;
                con[1] = 1.0 - odi;

                Vect pred = synthGet.apply(odi).apply(noddi.getEncoding());
                double cost = input.dist(pred) / model.getBaseline();

                return cost;
            };
            Cobyla.FindMinimum(func, p.length, cons, p, FitNoddiSMT.this.rhobeg, FitNoddiSMT.this.rhoend, 0, FitNoddiSMT.this.maxiter);

            model.setODI(p[0]);
        };

        BiConsumer<Vect, Noddi> fitFrac = (input, model) ->
        {
            double[] p = {model.getFICVF()};
            int cons = 2;

            Calcfc func = (n, m, x, con) ->
            {
                double fic = x[0];

                con[0] = fic;
                con[1] = FitNoddiSMT.this.maxden - fic;

                Noddi noddi = model.copy();
                noddi.setFICVF(fic);

                Vect pred = synthGet.apply(model.getODI()).apply(noddi.getEncoding());
                double cost = input.dist(pred) / model.getBaseline();

                return cost;
            };
            Cobyla.FindMinimum(func, p.length, cons, p, FitNoddiSMT.this.rhobeg, FitNoddiSMT.this.rhoend, 0, FitNoddiSMT.this.maxiter);

            model.setFICVF(p[0]);
        };

        BiConsumer<Vect, Noddi> fitAll = (input, model) ->
        {
            double[] p = {model.getFICVF(), model.getODI()};
            int cons = 4;

            Calcfc func = (n, m, x, con) ->
            {
                double ficvf = x[0];
                double odi = x[1];
                VectFunction synther = odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;

                model.setFICVF(ficvf);
                model.setODI(odi);

                Vect pred = synther.apply(model.getEncoding());
                double cost = input.dist(pred) / model.getBaseline();

                con[0] = odi;
                con[1] = 1.0 - odi;
                con[2] = ficvf;
                con[3] = FitNoddiSMT.this.maxden - ficvf;

                return cost;
            };
            Cobyla.FindMinimum(func, p.length, cons, p, FitNoddiSMT.this.rhobeg, FitNoddiSMT.this.rhoend, 0, FitNoddiSMT.this.maxiter);

            model.setFICVF(p[0]);
            model.setODI(p[1]);
        };

        final Function<Vect, Vect> fit = (input) ->
        {
            final double baseline = ModelUtils.baselines(this.gradients, input).mean();

            final Noddi model = new Noddi();
            model.setBaseline(baseline);

            if (!MathUtils.zero(baseline))
            {
                fitSMT.accept(input, model);

                if (!this.skipdir)
                {
                    final Tensor tensor = new Tensor(tensorFitter.apply(input));
                    model.setDir(tensor.getVec(0));
                    model.setODI(odiInit.apply(tensor.feature(Tensor.FEATURES_FA).get(0)));

                    fitOdi.accept(input, model);

                    if (this.extra)
                    {
                        fitFrac.accept(input, model);
                    }

                    if (this.full)
                    {
                        fitAll.accept(input, model);
                    }

                    // use a prior to regularize fic when free water dominates
                    if (this.prior != null && model.getFISO() > this.prior)
                    {
                        double fiso = model.getFISO();
                        double mix = fiso < this.prior ? 0 : (fiso - this.prior) / (1.0 - this.prior);
                        model.setFICVF((1.0 - mix * mix) * model.getFICVF());
                        model.setODI((1.0 - mix * mix) * model.getODI());
                    }
                }
            }

            return model.getEncoding();
        };

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(fit.apply(input));
            }
        }.init(this.gradients.size(), new Noddi().getEncodingSize());
    }

    public Function<Double, Double> mapFAtoODI(Gradients gradients)
    {
        FitTensorLLS tfit = new FitTensorLLS();
        tfit.gradients = this.gradients;
        VectFunction fitter = tfit.get();
        VectFunction synther = Noddi.synth(gradients);
        Noddi noddi = new Noddi();

        final int bins = 100;
        VectOnlineStats[] stats = new VectOnlineStats[bins];

        Function<Double, Integer> paramToIndex = param -> (int) Math.min(bins - 1, Math.max(0, Math.round(param * (bins - 1))));

        for (int i = 0; i < bins; i++)
        {
            stats[i] = new VectOnlineStats();
        }

        for (int i = 0; i < bins; i++)
        {
            double odi = i / (double) (bins - 1);

            noddi.setBaseline(1.0);
            noddi.setDir(VectSource.createZ());
            noddi.setFICVF(0.25);
            noddi.setFISO(0.05);
            noddi.setODI(odi);

            Vect signal = synther.apply(noddi.getEncoding());
            Tensor tensor = new Tensor(fitter.apply(signal));
            double fa = tensor.feature(Tensor.FEATURES_FA).get(0);
            int idx = paramToIndex.apply(fa);

            stats[idx].update(odi);
        }

        final double[] odis = new double[bins];
        for (int i = 0; i < bins; i++)
        {
            if (stats[i].num > 0)
            {
                Logging.infosub("fa = %g -> odi = %g", i / (double) bins, stats[i].mean);
                odis[i] = stats[i].mean;
            }
            else
            {
                odis[i] = 0.0;
            }
        }


        return fa -> odis[paramToIndex.apply(fa)];
    }
}
