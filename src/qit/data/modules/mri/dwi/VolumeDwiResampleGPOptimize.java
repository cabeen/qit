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


package qit.data.modules.mri.dwi;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.modules.mri.gradients.GradientsReduce;
import qit.data.modules.volume.VolumeReduce;
import qit.data.source.VectsSource;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.structs.VectFunction;

import java.util.List;

@ModuleDescription("Optimize the parameters for VolumeDwiResampleGP")
@ModuleCitation("Andersson, J. L., & Sotiropoulos, S. N. (2015). Non-parametric representation and prediction of single-and multi-shell diffusion-weighted MRI data using Gaussian processes. NeuroImage, 122, 166-176.")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class VolumeDwiResampleGPOptimize implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the input gradients (must match input DWI)")
    public Gradients gradients;

    @ModuleInput
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the minimum lambda")
    public Double lambdamin = 0.1;

    @ModuleParameter
    @ModuleDescription("the minimum lambda")
    public Double lambdamax = 1.0;

    @ModuleParameter
    @ModuleDescription("the number of lambda samples")
    public Integer lambdanum = 100;

    @ModuleParameter
    @ModuleDescription("the minimum alpha")
    public Double alphamin = 0.1;

    @ModuleParameter
    @ModuleDescription("the minimum alpha")
    public Double alphamax = 10.0;

    @ModuleParameter
    @ModuleDescription("the number of alpha samples")
    public Integer alphanum = 100;

    @ModuleParameter
    @ModuleDescription("the minimum beta")
    public Double betamin = 0.1;

    @ModuleParameter
    @ModuleDescription("the minimum beta")
    public Double betamax = 10.0;

    @ModuleParameter
    @ModuleDescription("the number of beta samples")
    public Integer betanum = 100;

    @ModuleOutput
    @ModuleDescription("the output results")
    public Table output;

    public VolumeDwiResampleGPOptimize run()
    {
        List<Integer> dvecs = this.gradients.getDvecIdx();
        Table out = new Table();
        out.withField("lambda");
        out.withField("alpha");
        out.withField("beta");
        out.withField("mse");

        List<Sample> samples = Lists.newArrayList();
        for (Sample sample : this.input.getSampling())
        {
            if (this.input.valid(sample, this.mask))
            {
                samples.add(sample);
            }
        }

        for (int i = 0; i < this.lambdanum; i++)
        {
            for (int j = 0; j < this.alphanum; j++)
            {
                for (int k = 0; k < this.betanum; k++)
                {
                    Logging.info(String.format("started condition (%d,%d,%d)", i, j, k));

                    double lambda = this.lambdamin + i * (this.lambdamax - this.lambdamin) / this.lambdanum;
                    double alpha = this.alphamin + j * (this.alphamax - this.alphamin) / this.alphanum;
                    double beta = this.betamin + k * (this.betamax - this.betamin) / this.betanum;

                    VectOnlineStats stats = new VectOnlineStats();

                    for (Integer d : dvecs)
                    {
                        Logging.info("... started fold: " + d);

                        VolumeDwiResampleGP resample = new VolumeDwiResampleGP();
                        resample.mask = this.mask;
                        resample.lambda = lambda;
                        resample.alpha = alpha;
                        resample.beta = beta;

                        {
                            GradientsReduce greduce = new GradientsReduce();
                            greduce.input = this.gradients;
                            greduce.exclude = String.valueOf(d);
                            resample.gradients = greduce.run().output;
                        }

                        {
                            VolumeReduce vreduce = new VolumeReduce();
                            vreduce.input = this.input;
                            vreduce.exclude = String.valueOf(d);
                            resample.input = vreduce.run().output;
                        }

                        {
                            resample.reference = new Gradients(VectsSource.create(this.gradients.getBvec(d)),this.gradients.getBval(d));
                        }

                        VectFunction gp = resample.factory().get();

                        for (Sample sample : samples)
                        {
                            Vect signal = this.input.get(sample);

                            double pred = gp.apply(signal.subex(d)).get(0);
                            double truth = signal.get(d);

                            double delta = pred - truth;
                            double error = delta * delta;

                            stats.update(error);
                        }
                    }

                    Record rec = new Record();
                    rec.with("lambda", lambda);
                    rec.with("alpha", alpha);
                    rec.with("beta", beta);
                    rec.with("mse", stats.mean);
                }
            }
        }

        this.output = out;

        return this;
    }
}
