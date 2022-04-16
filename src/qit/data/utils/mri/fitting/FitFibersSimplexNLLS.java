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
import qit.data.models.Fibers;
import qit.data.models.Tensor;
import qit.data.source.VectSource;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;

import java.util.function.Supplier;

public class FitFibersSimplexNLLS implements Supplier<VectFunction>
{
    public static final String NAME = "nlls";

    public static final double DIFF_SCALE = 1000;

    public static final int MAXITER = 3000;
    public static final int IPRINT = 0;
    public static final double RHOBEG = 0.5;
    public static final double RHOEND = 1e-3;

    public static final CostType DEFAULT_COST = CostType.SE;

    public Gradients gradients;
    public CostType cost = DEFAULT_COST;
    public int comps = 1;
    public double lambda = 0.001;
    public double power = 1;

    public VectFunction get()
    {
        FitTensorLLS lls = new FitTensorLLS();
        lls.weighted = true;
        lls.baseline = true;
        lls.gradients = this.gradients;
        final VectFunction initter = lls.get();
        final VectFunction synther = Fibers.synther(this.comps, this.gradients);

        return new VectFunction()
        {
            public void apply(final Vect input, Vect output)
            {
                Fibers initFibers = FitFibersSimplexNLLS.this.init(new Tensor(initter.apply(input)));

                final double s0 = initFibers.getBaseline();
                initFibers.setBaseline(initFibers.getBaseline() / s0);

                Vect initParam = param(initFibers);

                final int ndim = initParam.size();
                double[] x = initParam.toArray();
                final int mdim = (FitFibersSimplexNLLS.this.comps + 1) * 2;

                Calcfc func = new Calcfc()
                {
                    @Override
                    public double Compute(int n, int m, double[] x, double[] con)
                    {
                        Global.assume(n == ndim, "invalid value dimension: " + n);
                        Global.assume(m == mdim, "invalid constraint dimension: " + m);

                        Fibers model = model(new Vect(x));
                        model.setBaseline(model.getBaseline() * s0);
                        Vect pred = synther.apply(model.getEncoding());

                        Vect fracs = model.getFracs();

                        if (!MathUtils.unit(FitFibersSimplexNLLS.this.power))
                        {
                            fracs = fracs.pow(FitFibersSimplexNLLS.this.power);
                        }

                        double cost = ModelUtils.cost(FitFibersSimplexNLLS.this.cost, gradients, input, pred);
                        cost = cost / (s0 * s0) + lambda * fracs.sum();

                        con[0] = model.getDiffusivity();
                        for (int i = 0; i < FitFibersSimplexNLLS.this.comps; i++)
                        {
                            double frac = model.getFrac(i);
                            con[1 + 2 * i + 0] = frac;
                            con[1 + 2 * i + 1] = 1.0 - frac;
                        }

                        con[1 +  2 * FitFibersSimplexNLLS.this.comps] = 1.0 - model.getFracs().sum();

                        return cost;
                    }
                };
                Cobyla.FindMinimum(func, ndim, mdim, x, RHOBEG, RHOEND, IPRINT, MAXITER);

                Fibers modelNLLS = model(new Vect(x));
                modelNLLS.setBaseline(modelNLLS.getBaseline() * s0);
                output.set(modelNLLS.getEncoding());
            }
        }.init(this.gradients.size(), new Fibers(this.comps).getEncodingSize());
    }

    private Fibers init(Tensor tensor)
    {
        Fibers fibers = new Fibers(FitFibersSimplexNLLS.this.comps);
        fibers.setBaseline(tensor.getBaseline());
        fibers.setDiffusivity(tensor.feature(Tensor.FEATURES_MD).get(0));

        for (int i = 0; i < FitFibersSimplexNLLS.this.comps; i++)
        {
            if (i <= 3)
            {
                fibers.setFrac(i, tensor.getVal(i));
                fibers.setLine(i, tensor.getVec(i));
            }
            else
            {
                fibers.setFrac(i, 0.05);
                fibers.setLine(i, VectSource.randomUnit());
            }
        }

        double sum = fibers.getFracs().sum();

        for (int i = 0; i < FitFibersSimplexNLLS.this.comps; i++)
        {
            fibers.setFrac(i, fibers.getFrac(i) / (0.05 + sum));
        }

        return fibers;
    }

    private Vect param(Fibers model)
    {
        Vect out = VectSource.createND(3 + 3 * this.comps);

        out.set(0, model.getBaseline());
        out.set(1, model.getDiffusivity() * DIFF_SCALE);

        for (int i = 0; i < this.comps; i++)
        {
            double frac = model.getFrac(i);
            Vect line = model.getLine(i);

            double theta = Math.acos(line.getZ());
            double phi = Math.atan2(line.getY(), line.getX());

            out.set(2 + 3 * i + 0, frac);
            out.set(2 + 3 * i + 1, theta);
            out.set(2 + 3 * i + 2, phi);
        }

        return out;
    }

    private Fibers model(Vect param)
    {
        Fibers out = new Fibers(this.comps);

        out.setBaseline(param.get(0));
        out.setDiffusivity(param.get(1) / DIFF_SCALE);

        for (int i = 0; i < this.comps; i++)
        {
            double frac  = param.get(2 + 3 * i + 0);
            double theta = param.get(2 + 3 * i + 1);
            double phi   = param.get(2 + 3 * i + 2);

            double cosT = Math.cos(theta);
            double sinT = Math.sin(theta);
            double cosP = Math.cos(phi);
            double sinP = Math.sin(phi);

            double x = sinT * cosP;
            double y = sinT * sinP;
            double z = cosT;

            out.setFrac(i, frac);
            out.setLine(i, VectSource.create3D(x, y, z));
        }

        return out;
    }
}