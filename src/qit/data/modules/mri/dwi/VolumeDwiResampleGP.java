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

import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import smile.math.kernel.MercerKernel;
import smile.regression.GaussianProcessRegression;

import java.util.List;
import java.util.function.Supplier;

@ModuleDescription("Resample a diffusion-weighted MR volume to have a different set of gradients (arbitrary b-vectors and b-values)")
@ModuleCitation("Andersson, J. L., & Sotiropoulos, S. N. (2015). Non-parametric representation and prediction of single-and multi-shell diffusion-weighted MRI data using Gaussian processes. NeuroImage, 122, 166-176.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiResampleGP implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the input gradients (must match input DWI)")
    public Gradients gradients;

    @ModuleInput
    @ModuleDescription("the gradients used for resampling")
    public Gradients reference;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the lambda parameter (controls smoothness)")
    public Double lambda = 0.1;

    @ModuleParameter
    @ModuleDescription("the alpha parameter (controls gradient direction scaling)")
    public Double alpha = 1.0;

    @ModuleParameter
    @ModuleDescription("the beta parameter (controls gradient strength scaling)")
    public Double beta = 1.0;

    @ModuleParameter
    @ModuleDescription("the number of threads to use")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("the output resampled diffusion-weighted MR volume (will match reference gradients)")
    public Volume output;

    public VolumeDwiResampleGP run()
    {
        Volume out = new VolumeFunction(this.factory()).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        this.output = out;

        return this;
    }

    public Supplier<VectFunction> factory()
    {
        return () ->
        {
            final MercerKernel<Vect> kernel = (a, b) ->
            {
                Vect avec = a.sub(0, 3);
                double aval = a.get(3);

                Vect bvec = b.sub(0, 3);
                double bval = b.get(3);

                double dot = Math.abs(avec.dot(bvec));
                dot = Math.min(Math.max(dot, 0), 1);

                aval = Math.log(aval + Global.DELTA);
                bval = Math.log(bval + Global.DELTA);

                double dvec = Math.acos(dot);
                double dval = Math.abs(aval - bval);

                double cvec = MathUtils.zero(VolumeDwiResampleGP.this.alpha) ? 1.0 : Math.exp(-dvec / VolumeDwiResampleGP.this.alpha);
                double cval = MathUtils.zero(VolumeDwiResampleGP.this.beta) ? 1.0 : Math.exp(-dval / VolumeDwiResampleGP.this.beta);

                double k = cvec * cval;

                Global.assume(!Double.isNaN(k), "invalid k");

                return k;
            };

            final List<Integer> gradientWhich = VolumeDwiResampleGP.this.gradients.getDvecIdx();
            final List<Integer> referenceWhich = VolumeDwiResampleGP.this.reference.getDvecIdx();
            final List<Integer> referenceBaseline = VolumeDwiResampleGP.this.reference.getBaselineIdx();

            final int dinput = VolumeDwiResampleGP.this.gradients.size();
            final int doutput = VolumeDwiResampleGP.this.reference.size();

            return new VectFunction()
            {
                public void apply(Vect input1, Vect output1)
                {
                    double baseline = ModelUtils.baselineStats(VolumeDwiResampleGP.this.gradients, input1).mean;

                    int num = gradientWhich.size();
                    Vect[] x = new Vect[num];
                    double[] y = new double[num];

                    for (int i = 0; i < num; i++)
                    {
                        int idx = gradientWhich.get(i);

                        Vect vec = VolumeDwiResampleGP.this.gradients.getBvec(idx);
                        double val = VolumeDwiResampleGP.this.gradients.getBval(idx);

                        Vect xv = vec.cat(VectSource.create1D(val));
                        double yv = MathUtils.zero(baseline) ? 0 : input1.get(idx) / baseline;

                        x[i] = xv;
                        y[i] = yv;
                    }

                    GaussianProcessRegression<Vect> gp = new GaussianProcessRegression<>(x, y, kernel, VolumeDwiResampleGP.this.lambda);

                    for (Integer idx : referenceBaseline)
                    {
                        output1.set(idx, baseline);
                    }

                    for (Integer idx : referenceWhich)
                    {
                        Vect vec = VolumeDwiResampleGP.this.reference.getBvec(idx);
                        double val = VolumeDwiResampleGP.this.reference.getBval(idx);

                        Vect px = vec.cat(VectSource.create1D(val));
                        double py = baseline * gp.predict(px);

                        output1.set(idx, py);
                    }
                }
            }.init(dinput, doutput);
        };
    }
}
