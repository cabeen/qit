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


package qit.data.modules.mri.fibers;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.source.VectSource;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.ModelUtils;
import qit.math.utils.MathUtils;
import qit.math.utils.optim.jcobyla.Calcfc;
import qit.math.utils.optim.jcobyla.Cobyla;
import qit.math.utils.optim.jcobyla.CobylaExitStatus;

@ModuleDescription("Fit fibers using a reference fibers volume for fiber orientations")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class VolumeFibersRefit implements Module
{
    @ModuleInput
    @ModuleDescription("input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the gradients")
    public Gradients gradients;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the mask")
    public Mask mask;

    @ModuleInput
    @ModuleDescription("input reference fibers volume")
    public Volume ref;

    @ModuleParameter
    @ModuleDescription("the stick diffusivity")
    public double stick = 1.7e-3;

    @ModuleParameter
    @ModuleDescription("the ball diffusivity")
    public double ball = 3e-3;

    @ModuleOutput
    @ModuleDescription("output fibers volume")
    public Volume output;

    @Override
    public Module run()
    {
        Gradients grads = this.gradients.copy().rotate(this.input.getSampling().quat());

        Volume out = this.ref.proto();
        Sampling sampling = out.getSampling();

        final int comps = Fibers.count(this.ref.getDim());
        final double aiso = Math.exp(-this.ball);

        Logging.info("starting fibers refitting");
        int slice = 0;
        int slices = sampling.numK();
        for (Sample sample : out.getSampling())
        {
            if (this.input.valid(sample, this.mask))
            {
                if (sample.getK() > slice - 1)
                {
                    slice += 1;
                    Logging.info("processing slice " + slice + " of " + slices);
                }

                final int ngrad = VolumeFibersRefit.this.gradients.size();
                final Gradients fgrad = this.gradients;
                final Vect signal = this.input.get(sample);
                final Fibers fibers = new Fibers(this.ref.get(sample));

                double baseline = ModelUtils.baselineStats(grads, signal).mean;
                double norm = MathUtils.zero(baseline) ? 1.0 : 1.0 / baseline;

                final Vect normSignal = signal.times(norm);

                final int xdim = comps + 2;
                final int cdim = 2 * (comps + 1) + 1;
                final Matrix constraints = new Matrix(cdim, xdim + 1);
                constraints.set(0, 0, 1.0);

                for (int i = 0; i <= comps; i++)
                {
                    constraints.set(1 + 2 * i, 1 + i, 1.0);
                    constraints.set(1 + 2 * i + 1, 1 + i, -1.0);
                    constraints.set(1 + 2 * i + 1, 1 + comps, 1.0);

                    constraints.set(1 + 2 * comps, 1 + i, -1.0);
                }
                constraints.set(1 + 2 * comps, comps, 1.0);

                final Calcfc calcfc = new Calcfc()
                {
                    @Override
                    public double Compute(int n, int m, double[] x, double[] con)
                    {
                        double d = x[0] / 1000;
                        double fiso = x[1];

                        Vect pred = VectSource.createND(fgrad.size());
                        double aiso  = fiso * Math.exp(-d);
                        for (int i = 0; i < fgrad.size(); i++)
                        {
                            Vect g = fgrad.getBvec(i);
                            double b = fgrad.getBval(i);

                            double p = aiso;
                            for (int j = 0; j < comps; j++)
                            {
                                Vect v = fibers.getLine(j).normalize();

                                double gdotv = g.dot(v);
                                double fcomp = x[2 + j];
                                double acomp = Math.exp(-d * b * gdotv * gdotv);

                                p += fcomp * acomp;
                            }
                            pred.set(i, p);
                        }

                        double cost = pred.minus(normSignal).norm() / ngrad;
                        constraints.times(VectSource.create(x).cat(VectSource.create1D(1.0))).get(con);

                        return cost;
                    }
                };

                double finit = 1.0 / (comps + 1);
                double[] x = new double[xdim];
                x[0] = 1.7;
                x[1] = finit;
                for (int i = 0; i < comps; i++)
                {
                    x[i + 2] = finit;
                }
                CobylaExitStatus exit = Cobyla.FindMinimum(calcfc, xdim, cdim, x, 0.1, 0.001, 1, 1000);

                for (int i = 0; i < comps; i++)
                {
                    fibers.setFrac(i, x[i + 2]);
                }

                fibers.setBaseline(baseline);
                fibers.setDiffusivity(x[0] / 1000);

                out.set(sample, fibers.getEncoding());
            }
        }

        Logging.info("finished fibers refitting");

        this.output = out;

        return this;
    }
}
