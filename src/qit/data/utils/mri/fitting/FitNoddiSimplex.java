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

import qit.base.Global;
import qit.data.datasets.Vect;
import qit.data.models.Noddi;
import qit.data.models.Tensor;
import qit.data.source.VectSource;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

import java.util.function.Supplier;

/**
 * Powell, Michael JD. "A direct search optimization method that models the objective and
 * constraint functions by linear interpolation." Advances in optimization and numerical
 * analysis. Springer Netherlands, 1994. 51-67.
 */
public class FitNoddiSimplex implements Supplier<VectFunction>
{
    public static final String NAME = "Simplex";

    public static final double THRESH_LOW = 0.1;
    public static final double THRESH_HIGH = 0.25;

    public static final CostType DEFAULT_COST = CostType.SE;

    public Gradients gradients;
    public CostType cost = DEFAULT_COST;
    public Integer grid = null;
    public double maxden = 1.0;
    public int maxiter = 5000;
    public double rhobeg = 0.1;
    public double rhoend = 1e-5;
    public Double prior = 1.0;
    public boolean dot = false;

    public VectFunction get()
    {
        return this.dot ? createDot() : createNoDot();
    }

    public VectFunction createNoDot()
    {
        FitTensorLLS lls = new FitTensorLLS();
        lls.gradients = this.gradients;
        final VectFunction tensorFitter = lls.get();
        final VectFunction syntherHigh = Noddi.synth(this.gradients, 0);
        final VectFunction syntherMed = Noddi.synth(this.gradients, 1);
        final VectFunction syntherLow = Noddi.synth(this.gradients, 2);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                final Vect finput = input;
                final Tensor tensor = new Tensor(tensorFitter.apply(input));
                final double baseline = ModelUtils.baselineStats(gradients, input).mean;
                double[] x = {0.5, 0.5, 0.5};

                if (FitNoddiSimplex.this.grid != null && FitNoddiSimplex.this.grid > 1)
                {
                    double bestCost = Double.MAX_VALUE;
                    Noddi bestModel = null;

                    Vect values = VectSource.linspace(0, 1, FitNoddiSimplex.this.grid);

                    for (int i = 0; i < FitNoddiSimplex.this.grid; i++)
                    {
                        for (int j = 0; j < FitNoddiSimplex.this.grid; j++)
                        {
                            for (int k = 0; k < FitNoddiSimplex.this.grid; k++)
                            {
                                Noddi model = new Noddi();
                                model.setDir(tensor.getVec(0));
                                model.setBaseline(baseline);
                                model.setFICVF(values.get(i));
                                model.setFISO(values.get(j));
                                model.setODI(values.get(k));

                                double odi = values.get(k);
                                VectFunction synther = odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;
                                Vect pred = synther.apply(model.getEncoding());

                                double testCost = ModelUtils.cost(FitNoddiSimplex.this.cost, gradients, input, pred);

                                if (testCost < bestCost)
                                {
                                    bestCost = testCost;
                                    bestModel = model;
                                }
                            }
                        }
                    }

                    x[0] = bestModel.getFICVF();
                    x[1] = bestModel.getFISO();
                    x[2] = bestModel.getODI();
                }

                int cons = 6;
                Calcfc func = (n, m, x1, con) ->
                {
                    double ficvf = x1[0];
                    double fiso = x1[1];
                    double odi = x1[2];
                    VectFunction synther = odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;

                    Noddi model = new Noddi();
                    model.setDir(tensor.getVec(0));
                    model.setBaseline(baseline);
                    model.setFICVF(ficvf);
                    model.setFISO(fiso);
                    model.setODI(odi);

                    Vect pred = synther.apply(model.getEncoding());

                    double cost = ModelUtils.cost(FitNoddiSimplex.this.cost, gradients, finput, pred);
                    con[0] = odi;
                    con[1] = 1.0 - odi;
                    con[2] = ficvf;
                    con[3] = FitNoddiSimplex.this.maxden - ficvf;
                    con[4] = fiso;
                    con[5] = 1.0 - fiso;

                    return cost;
                };
                Cobyla.FindMinimum(func, x.length, cons, x, FitNoddiSimplex.this.rhobeg, FitNoddiSimplex.this.rhoend, 0, FitNoddiSimplex.this.maxiter);

                double fic = x[0];
                double fiso = x[1];
                double odi = x[2];

                // use a prior to regularize fic when free water dominates
                if (fiso > FitNoddiSimplex.this.prior)
                {
                    double mix = (fiso - FitNoddiSimplex.this.prior) / (1.0 - FitNoddiSimplex.this.prior);
                    fic *= (1.0 - mix * mix);
                    odi *= (1.0 - mix * mix);
                }

                Noddi model = new Noddi();
                model.setDir(tensor.getVec(0));
                model.setBaseline(baseline);
                model.setFICVF(fic);
                model.setFISO(fiso);
                model.setODI(odi);

                output.set(model.getEncoding());
            }
        }.init(this.gradients.size(), new Noddi().getEncodingSize());
    }

    public VectFunction createDot()
    {
        FitTensorLLS lls = new FitTensorLLS();
        lls.gradients = this.gradients;
        final VectFunction tensorFitter = lls.get();
        final VectFunction syntherHigh = Noddi.synth(this.gradients, 0);
        final VectFunction syntherMed = Noddi.synth(this.gradients, 1);
        final VectFunction syntherLow = Noddi.synth(this.gradients, 2);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                final Vect finput = input;
                final Tensor tensor = new Tensor(tensorFitter.apply(input));
                final double baseline = ModelUtils.baselineStats(gradients, input).mean;
                double[] x = {0.5, 0.5, 0.5, 0.5};

                if (FitNoddiSimplex.this.grid != null && FitNoddiSimplex.this.grid > 1)
                {
                    double bestCost = Double.MAX_VALUE;
                    Noddi bestModel = null;

                    Vect values = VectSource.linspace(0, 1, FitNoddiSimplex.this.grid);

                    for (int i = 0; i < FitNoddiSimplex.this.grid; i++)
                    {
                        for (int j = 0; j < FitNoddiSimplex.this.grid; j++)
                        {
                            for (int k = 0; k < FitNoddiSimplex.this.grid; k++)
                            {
                                for (int l = 0; l < FitNoddiSimplex.this.grid; l++)
                                {
                                    Noddi model = new Noddi();
                                    model.setDir(tensor.getVec(0));
                                    model.setBaseline(baseline);
                                    model.setFICVF(values.get(i));
                                    model.setFISO(values.get(j));
                                    model.setODI(values.get(k));
                                    model.setIRFRAC(values.get(l));

                                    double odi = values.get(k);
                                    VectFunction synther = odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;
                                    Vect pred = synther.apply(model.getEncoding());

                                    double testCost = ModelUtils.cost(FitNoddiSimplex.this.cost, gradients, input, pred);

                                    if (testCost < bestCost)
                                    {
                                        bestCost = testCost;
                                        bestModel = model;
                                    }
                                }
                            }
                        }
                    }

                    x[0] = bestModel.getFICVF();
                    x[1] = bestModel.getFISO();
                    x[2] = bestModel.getODI();
                    x[3] = bestModel.getIRFRAC();
                }

                int cons = 8;
                Calcfc func = (n, m, x1, con) ->
                {
                    double ficvf = x1[0];
                    double fiso = x1[1];
                    double odi = x1[2];
                    double irfrac = x1[3];
                    VectFunction synther = odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;

                    Noddi model = new Noddi();
                    model.setDir(tensor.getVec(0));
                    model.setBaseline(baseline);
                    model.setFICVF(ficvf);
                    model.setFISO(fiso);
                    model.setODI(odi);
                    model.setIRFRAC(irfrac);

                    Vect pred = synther.apply(model.getEncoding());

                    double cost = ModelUtils.cost(FitNoddiSimplex.this.cost, gradients, finput, pred);
                    con[0] = odi;
                    con[1] = 1.0 - odi;
                    con[2] = ficvf;
                    con[3] = FitNoddiSimplex.this.maxden - ficvf;
                    con[4] = fiso;
                    con[5] = 1.0 - fiso;
                    con[6] = irfrac;
                    con[7] = 1.0 - irfrac;

                    return cost;
                };
                Cobyla.FindMinimum(func, x.length, cons, x, FitNoddiSimplex.this.rhobeg, FitNoddiSimplex.this.rhoend, 0, FitNoddiSimplex.this.maxiter);

                double fic = x[0];
                double fiso = x[1];
                double odi = x[2];
                double irfrac = x[3];

                // use a prior to regularize fic when free water dominates
                if (fiso > FitNoddiSimplex.this.prior)
                {
                    double mix = (fiso - FitNoddiSimplex.this.prior) / (1.0 - FitNoddiSimplex.this.prior);
                    fic *= (1.0 - mix * mix);
                    odi *= (1.0 - mix * mix);
                }

                Noddi model = new Noddi();
                model.setDir(tensor.getVec(0));
                model.setBaseline(baseline);
                model.setFICVF(fic);
                model.setFISO(fiso);
                model.setODI(odi);
                model.setIRFRAC(irfrac);

                output.set(model.getEncoding());
            }
        }.init(this.gradients.size(), new Noddi().getEncodingSize());
    }
}
