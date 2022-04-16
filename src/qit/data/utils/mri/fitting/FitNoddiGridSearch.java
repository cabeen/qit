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
import qit.data.source.VectSource;
import qit.data.utils.mri.CostType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.models.Noddi;
import qit.data.models.Tensor;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

public class FitNoddiGridSearch implements Supplier<VectFunction>
{
    public static final String NAME = "Grid";

    public static final CostType DEFAULT_COST = CostType.SE;
    public static final String DEFAULT_SCHEDULE = "10,5,5,5,5,5";
    public static final double DEFAULT_SCALE = 1.0;
    public static final double THRESH_LOW = 0.1;
    public static final double THRESH_HIGH = 0.25;

    public Gradients gradients;
    public CostType cost = DEFAULT_COST;
    public String schedule = DEFAULT_SCHEDULE;
    public double scale = DEFAULT_SCALE;
    public double maxden = 0.99;
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
        final int[] schedule = parseSchedule(this.schedule);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Tensor tensor = new Tensor(tensorFitter.apply(input));
                double baseline = ModelUtils.baselineStats(gradients, input).mean;

                double lowICVF = 0.0;
                double highICVF = FitNoddiGridSearch.this.maxden;

                double lowISOVF = 0.0;
                double highISOVF = 1.0;

                double lowOD = 0.0;
                double highOD = 1.0;

                double bestCost = Double.MAX_VALUE;
                Noddi bestModel = null;
                for (int l = 0; l < schedule.length; l++)
                {
                    int num = schedule[l];
                    Vect icvfs = VectSource.linspace(lowICVF, highICVF, num);
                    Vect isovfs = VectSource.linspace(lowISOVF, highISOVF, num);
                    Vect ods = VectSource.linspace(lowOD, highOD, num);

                    for (int i = 0; i < num; i++)
                    {
                        for (int j = 0; j < num; j++)
                        {
                            for (int k = 0; k < num; k++)
                            {
                                Noddi model = new Noddi();
                                model.setDir(tensor.getVec(0));
                                model.setBaseline(baseline);
                                model.setFICVF(icvfs.get(i));
                                model.setFISO(isovfs.get(j));
                                model.setODI(ods.get(k));

                                double odi = ods.get(k);
                                VectFunction synther = odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;
                                Vect pred = synther.apply(model.getEncoding());

                                double testCost = ModelUtils.cost(FitNoddiGridSearch.this.cost, gradients, input, pred);

                                if (testCost < bestCost)
                                {
                                    bestCost = testCost;
                                    bestModel = model;
                                }
                            }
                        }
                    }

                    double reduce = FitNoddiGridSearch.this.scale / (num - 1);
                    double deltaICVF = reduce * (highICVF - lowICVF);
                    double deltaISOVF = reduce * (highISOVF - lowISOVF);
                    double deltaOD = reduce * (highOD - lowOD);

                    lowICVF = Math.min(Math.max(0, Math.max(lowICVF, bestModel.getFICVF() - deltaICVF)), 1);
                    highICVF = Math.min(Math.max(0, Math.min(highICVF, bestModel.getFICVF() + deltaICVF)), 1);

                    lowISOVF = Math.min(Math.max(0, bestModel.getFISO() - deltaISOVF), 1);
                    highISOVF = Math.min(Math.max(0, bestModel.getFISO() + deltaISOVF), 1);

                    lowOD = Math.min(Math.max(0, bestModel.getODI() - deltaOD), 1);
                    highOD = Math.min(Math.max(0, bestModel.getODI() + deltaOD), 1);
                }

                output.set(bestModel.getEncoding());
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
        final int[] schedule = parseSchedule(this.schedule);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Tensor tensor = new Tensor(tensorFitter.apply(input));
                double baseline = ModelUtils.baselineStats(gradients, input).mean;

                double lowICVF = 0.0;
                double highICVF = FitNoddiGridSearch.this.maxden;

                double lowISOVF = 0.0;
                double highISOVF = 1.0;

                double lowOD = 0.0;
                double highOD = 1.0;

                double lowIRFRAC = 0.0;
                double highIRFRAC = 1.0;

                double bestCost = Double.MAX_VALUE;
                Noddi bestModel = null;
                for (int num : schedule)
                {
                    Vect icvfs = VectSource.linspace(lowICVF, highICVF, num);
                    Vect isovfs = VectSource.linspace(lowISOVF, highISOVF, num);
                    Vect ods = VectSource.linspace(lowOD, highOD, num);
                    Vect irfs  = VectSource.linspace(lowIRFRAC, highIRFRAC, num);

                    for (int i = 0; i < num; i++)
                    {
                        for (int j = 0; j < num; j++)
                        {
                            for (int k = 0; k < num; k++)
                            {
                                for (int l = 0; l < num; l++)
                                {
                                    Noddi model = new Noddi();
                                    model.setDir(tensor.getVec(0));
                                    model.setBaseline(baseline);
                                    model.setFICVF(icvfs.get(i));
                                    model.setFISO(isovfs.get(j));
                                    model.setODI(ods.get(k));
                                    model.setIRFRAC(irfs.get(l));

                                    double odi = ods.get(k);
                                    VectFunction synther = odi < THRESH_LOW ? syntherLow : odi < THRESH_HIGH ? syntherMed : syntherHigh;
                                    Vect pred = synther.apply(model.getEncoding());

                                    double testCost = ModelUtils.cost(FitNoddiGridSearch.this.cost, gradients, input, pred);

                                    if (testCost < bestCost)
                                    {
                                        bestCost = testCost;
                                        bestModel = model;
                                    }
                                }
                            }
                        }
                    }

                    double reduce = FitNoddiGridSearch.this.scale / (num - 1);
                    double deltaICVF = reduce * (highICVF - lowICVF);
                    double deltaISOVF = reduce * (highISOVF - lowISOVF);
                    double deltaOD = reduce * (highOD - lowOD);
                    double deltaIRFRAC  = reduce * (highIRFRAC - lowIRFRAC);

                    lowICVF = Math.min(Math.max(0, Math.max(lowICVF, bestModel.getFICVF() - deltaICVF)), 1);
                    highICVF = Math.min(Math.max(0, Math.min(highICVF, bestModel.getFICVF() + deltaICVF)), 1);

                    lowISOVF = Math.min(Math.max(0, bestModel.getFISO() - deltaISOVF), 1);
                    highISOVF = Math.min(Math.max(0, bestModel.getFISO() + deltaISOVF), 1);

                    lowOD = Math.min(Math.max(0, bestModel.getODI() - deltaOD), 1);
                    highOD = Math.min(Math.max(0, bestModel.getODI() + deltaOD), 1);

                    lowIRFRAC = Math.min(Math.max(0, bestModel.getIRFRAC() - deltaIRFRAC), 1);
                    highIRFRAC = Math.min(Math.max(0, bestModel.getIRFRAC() + deltaIRFRAC), 1);
                }

                output.set(bestModel.getEncoding());
            }
        }.init(this.gradients.size(), new Noddi().getEncodingSize());
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
