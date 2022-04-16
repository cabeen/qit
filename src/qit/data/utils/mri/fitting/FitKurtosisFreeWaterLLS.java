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

import qit.data.datasets.Vect;
import qit.data.models.Kurtosis;
import qit.data.models.Tensor;
import qit.data.source.VectSource;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

public class FitKurtosisFreeWaterLLS implements Supplier<VectFunction>
{
    // Hoy, A. R., Koay, C. G., Kecskemeti, S. R., & Alexander, A. L. (2014). Optimization
    // of a free water elimination two-compartment model for diffusion tensor imaging.
    // NeuroImage, 103, 323-333.

    public static final CostType DEFAULT_COST = CostType.SE;
    public static final String DEFAULT_SCHEDULE = "10,10,10";
    public static final double DEFAULT_SCALE = 1.0;

    public Gradients gradients;
    public CostType cost = DEFAULT_COST;
    public String schedule = DEFAULT_SCHEDULE;
    public double scale = DEFAULT_SCALE;
    public double prior = 0.5;
    public boolean weighted = false;

    public VectFunction get()
    {
        final VectFunction fitter = new FitKurtosisLLS()
        {{
            this.gradients = FitKurtosisFreeWaterLLS.this.gradients;
            this.weighted = FitKurtosisFreeWaterLLS.this.weighted;
        }}.get();
        final VectFunction synther = Kurtosis.synth(this.gradients);
        final int[] schedule = parseSchedule(this.schedule);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double s0 = ModelUtils.baselineStats(FitKurtosisFreeWaterLLS.this.gradients, input).mean;

                double lowFrac = 0.0;
                double highFrac = 1.0;

                double bestCost = Double.MAX_VALUE;
                Kurtosis bestModel = null;
                for (int l = 0; l < schedule.length; l++)
                {
                    int num = schedule[l];
                    Vect fracs = VectSource.linspace(lowFrac, highFrac, num);

                    for (int i = 0; i < num; i++)
                    {
                        double frac = fracs.get(i);
                        double invfrac = 1.0 - frac;

                        Vect sig = input.proto();
                        for (int idx = 0; idx < input.size(); idx++)
                        {
                            double bval = FitKurtosisFreeWaterLLS.this.gradients.getBval(idx);
                            double expiso = Math.exp(-bval * Kurtosis.FREE_DIFF);
                            double si = input.get(idx);

                            double term = (si - s0 * frac * expiso) / invfrac;
                            sig.set(idx, term);
                        }

                        Kurtosis model = new Kurtosis(fitter.apply(sig));
                        model.setFreeWater(frac);

                        Vect pred = synther.apply(model.getEncoding());

                        double testCost = ModelUtils.cost(FitKurtosisFreeWaterLLS.this.cost, gradients, input, pred);

                        if (testCost < bestCost)
                        {
                            bestCost = testCost;
                            bestModel = model;
                        }
                    }

                    double reduce = FitKurtosisFreeWaterLLS.this.scale / (num - 1);
                    double deltaFrac = reduce * (highFrac - lowFrac);

                    lowFrac = Math.min(Math.max(0, Math.max(lowFrac, bestModel.getFreeWater() - deltaFrac)), 1);
                    highFrac = Math.min(Math.max(0, Math.min(highFrac, bestModel.getFreeWater() + deltaFrac)), 1);
                }

                // use a prior to regularize tensors when free water dominates
                // this smoothly shrinks the tensor to zero and isotropic as fw goes to one
                double p = FitKurtosisFreeWaterLLS.this.prior;
                double f = bestModel.getFreeWater();
                if (f > p)
                {
                    double lin = (f - p) / (1.0 - p);
                    double mix = lin * lin;
                    double mixinv = 1.0 - mix;
                    double b0 = bestModel.baseline();
                    Tensor tensor = new Tensor(b0, bestModel.getTensorD());
                    Vect vals = tensor.getVals();

                    double mean = mix * vals.mean();
                    Vect pvals = vals.times(mixinv).plus(mix, VectSource.create3D(mean, mean, mean));

                    tensor.setVals(pvals);
                    bestModel.setTensorD(tensor.getMatrix());
                }

                output.set(bestModel.getEncoding());

            }
        }.init(this.gradients.size(), new Kurtosis().getEncodingSize());
    }

    private int[] parseSchedule(String spec)
    {
        String[] tokens = spec.split(",");
        int[] out = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++)
        {
            out[i] = Integer.valueOf(tokens[i]);
        }
        return out;
    }
}
