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

@ModuleDescription("Detect and replace outliers of a diffusion-weighted MR volume using Gaussian Process regression.  This first detects outliers using a very smooth GP model and then replaces only those outliers using a more rigid GP model")
@ModuleCitation("Andersson, J. L., & Sotiropoulos, S. N. (2015). Non-parametric representation and prediction of single-and multi-shell diffusion-weighted MRI data using Gaussian processes. NeuroImage, 122, 166-176.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiResampleOutlierGP implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the input gradients (must match input DWI)")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the threshold for outlier detection. this is the fractional change in single value from the GP prediction to qualify as an outlier")
    public Double outlier = 0.35;

    @ModuleParameter
    @ModuleDescription("the minimum fraction of gradient directions to use for outlier replacement.  if more than this fraction are considered inliers, then interpolation will be used")
    public Double include = 0.75;

    @ModuleParameter
    @ModuleDescription("the lambda parameter for detecting outliers (controls smoothness)")
    public Double lambdaDetect = 1.0;

    @ModuleParameter
    @ModuleDescription("the lambda parameter for replacing outliers (controls smoothness)")
    public Double lambdaPredict = 1.0;

    @ModuleParameter
    @ModuleDescription("resample all signal values with reduced GP (default will only resample outliers)")
    public boolean resample = false;

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

    public VolumeDwiResampleOutlierGP run()
    {
        Volume out = new VolumeFunction(this.factory()).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        this.output = out;

        return this;
    }

    public Supplier<VectFunction> factory()
    {
        return () ->
        {
            final MercerKernel<Vect> kernel = new MercerKernel<Vect>()
            {
                @Override
                public double k(Vect a, Vect b)
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

                    double cvec = MathUtils.zero(VolumeDwiResampleOutlierGP.this.alpha) ? 1.0 : Math.exp(-dvec / VolumeDwiResampleOutlierGP.this.alpha);
                    double cval = MathUtils.zero(VolumeDwiResampleOutlierGP.this.beta) ? 1.0 : Math.exp(-dval / VolumeDwiResampleOutlierGP.this.beta);

                    double k = cvec * cval;

                    Global.assume(!Double.isNaN(k), "invalid k");

                    return k;
                }
            };

            final List<Integer> which = VolumeDwiResampleOutlierGP.this.gradients.getDvecIdx();
            final List<Integer> baselines = VolumeDwiResampleOutlierGP.this.gradients.getBaselineIdx();
            final int dim = VolumeDwiResampleOutlierGP.this.gradients.size();

            return new VectFunction()
            {
                public void apply(Vect input1, Vect output1)
                {
                    double baseline = ModelUtils.baselineStats(VolumeDwiResampleOutlierGP.this.gradients, input1).mean;

                    for (Integer idx : baselines)
                    {
                        output1.set(idx, baseline);
                    }

                    int num = which.size();
                    boolean[] include1 = new boolean[num];

                    {
                        Vect[] x = new Vect[num];
                        double[] y = new double[num];

                        for (int i = 0; i < num; i++)
                        {
                            int idx = which.get(i);

                            Vect vec = VolumeDwiResampleOutlierGP.this.gradients.getBvec(idx);
                            double val = VolumeDwiResampleOutlierGP.this.gradients.getBval(idx);

                            Vect xv = vec.cat(VectSource.create1D(val));
                            double yv = MathUtils.zero(baseline) ? 0 : input1.get(idx) / baseline;

                            x[i] = xv;
                            y[i] = yv;
                        }

                        GaussianProcessRegression<Vect> gpDetect = new GaussianProcessRegression<>(x, y, kernel, VolumeDwiResampleOutlierGP.this.lambdaDetect);

                        for (int i = 0; i < num; i++)
                        {
                            double py = gpDetect.predict(x[i]);
                            double vy = y[i];

                            double change = MathUtils.zero(vy) ? 1.0 : Math.abs(py - vy) / vy;

                            int idx = which.get(i);
                            output1.set(idx, change);

                            include1[i] = change < VolumeDwiResampleOutlierGP.this.outlier;
                        }
                    }

                    int numinc = MathUtils.count(include1);

                    if (numinc > VolumeDwiResampleOutlierGP.this.include * dim)
                    {
                        Vect[] x = new Vect[numinc];
                        double[] y = new double[numinc];

                        int incidx = 0;
                        for (int i = 0; i < num; i++)
                        {
                            if (include1[i])
                            {
                                int idx = which.get(i);

                                Vect vec = VolumeDwiResampleOutlierGP.this.gradients.getBvec(idx);
                                double val = VolumeDwiResampleOutlierGP.this.gradients.getBval(idx);

                                Vect xv = vec.cat(VectSource.create1D(val));
                                double yv = MathUtils.zero(baseline) ? 0 : input1.get(idx) / baseline;

                                x[incidx] = xv;
                                y[incidx] = yv;

                                incidx += 1;
                            }
                        }
                        Global.assume(incidx == numinc, "bug in filling gp data");

                        GaussianProcessRegression<Vect> gpDetect = new GaussianProcessRegression<>(x, y, kernel, VolumeDwiResampleOutlierGP.this.lambdaPredict);

                        for (int i = 0; i < num; i++)
                        {
                            int idx = which.get(i);
                            if (!VolumeDwiResampleOutlierGP.this.resample && include1[i])
                            {
                                output1.set(idx, input1.get(idx));
                            }
                            else
                            {
                                Vect vec = VolumeDwiResampleOutlierGP.this.gradients.getBvec(idx);
                                double val = VolumeDwiResampleOutlierGP.this.gradients.getBval(idx);

                                Vect px = vec.cat(VectSource.create1D(val));
                                double py = baseline * gpDetect.predict(px);

                                output1.set(idx, py);
                            }
                        }
                    }
                    else
                    {
                        for (int i = 0; i < num; i++)
                        {
                            int idx = which.get(i);
                            output1.set(idx, input1.get(idx));
                        }
                    }
                }
            }.init(dim, dim);
        };
    }
}
