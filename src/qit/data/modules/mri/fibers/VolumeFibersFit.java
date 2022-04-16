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

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleExpert;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.models.Mcsmt;
import qit.data.models.Tensor;
import qit.data.modules.mri.gradients.GradientsTransform;
import qit.data.modules.mri.odf.VolumeOdfPeaks;
import qit.data.modules.mri.spharm.VolumeSpharmPeaks;
import qit.data.modules.vects.VectsCreateSphere;
import qit.data.modules.vects.VectsCreateSphereLookup;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.fitting.FitFibersFixedSticks;
import qit.data.utils.mri.fitting.FitFibersMultiStage;
import qit.data.utils.mri.fitting.FitMcsmt;
import qit.data.utils.mri.fitting.FitOdfRichardsonLucy;
import qit.data.utils.mri.fitting.FitSpharmCSD;
import qit.data.utils.mri.fitting.FitTensorLLS;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.structs.PeakFinder;
import qit.data.utils.mri.structs.Shells;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@ModuleDescription("Fit a fibers volume using a multi-stage procedure")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersFit extends FitFibersMultiStage implements Module
{
    public enum Method
    {
        CSD, RLD
    }

    public enum DiffInit
    {
        Tensor, Fixed, Best
    }

    public static final int DEFAULT_COMPS = 3;
    public static final double DEFAULT_MINFRAC = 0.01;

    @ModuleInput
    @ModuleDescription("input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the gradients")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the mask")
    public Mask mask;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify rounding factor for the gradients")
    public Integer round = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a subset of gradient shells to include (comma-separated list of b-values)")
    public String shells = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a subset of gradients to include (comma-separated list of indices starting from zero)")
    public String which = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("specify a subset of gradients to exclude (comma-separated list of indices starting from zero)")
    public String exclude = null;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleParameter
    @ModuleDescription("use fine-grained multi-threading")
    public boolean columns = false;

    @ModuleParameter
    @ModuleDescription("the minimum volume fraction")
    public double minfrac = DEFAULT_MINFRAC;

    @ModuleParameter
    @ModuleDescription("the maximum number of fiber compartments to extract")
    public int comps = DEFAULT_COMPS;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the number of points used for peak extraction")
    public Integer points = 256;

    @ModuleParameter
    @ModuleDescription("the angle for peak clustering")
    public double cluster = PeakFinder.DEFAULT_CLUSTER;

    @ModuleParameter
    @ModuleDescription("the statistic for aggregating peaks")
    public PeakFinder.PeakMode peak = PeakFinder.PeakMode.Sum;

    @ModuleParameter
    @ModuleDescription("the maximum diffusivity for MCSMT")
    public double dmax = FitMcsmt.DEFAULT_DMAX;

    @ModuleParameter
    @ModuleDescription("the maximum number of iterations for MCSMT")
    public int maxiters = FitMcsmt.DEFAULT_MAXITERS;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("use the givel fraction of axial diffusivity when fitting fiber volume fractions")
    public Double dperp = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("use the given fixed axial diffusivity when fitting fiber volume fractions")
    public Double dfix = FitFibersFixedSticks.DEFAULT_DFIX;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use a tortuosity model (otherwise the extra-cellular diffusivity is matched to the intra)")
    public boolean tort = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("use the given fixed axial diffusivity when fitting fiber volume fractions")
    public Integer restarts = FitFibersFixedSticks.DEFAULT_RESTARTS;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("skip SMT estimation (for multishell data only)")
    public boolean nosmt = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify an parameter estimation mode for single shell analysis")
    public FitFibersFixedSticks.Mode modeSingle = FitFibersFixedSticks.Mode.FracSumDiffMultistage;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify an parameter estimation mode for single shell analysis")
    public FitFibersFixedSticks.Mode modeMulti = FitFibersFixedSticks.Mode.FracsFixedSum;

    @ModuleParameter
    @ModuleDescription("specify a method for initializing diffusivity")
    public DiffInit diffinit = DiffInit.Best;

    @ModuleParameter
    @ModuleDescription("specify an orientation estimation method")
    public Method method = Method.RLD;

    @ModuleParameter
    @ModuleDescription("the number of iterations for RLD")
    public int rldIters = FitOdfRichardsonLucy.DEFAULT_RLDITERS;

    @ModuleParameter
    @ModuleDescription("the alpha parameter for RLD")
    public double rldAlpha = FitOdfRichardsonLucy.DEFAULT_ALPHA;

    @ModuleParameter
    @ModuleDescription("the beta parameter for RLD")
    public double rldBeta = FitOdfRichardsonLucy.DEFAULT_BETA;

    @ModuleParameter
    @ModuleDescription("specify the maximum spherical harmonic order for CSD")
    public Integer csdOrder = 8;

    @ModuleParameter
    @ModuleDescription("the axial fiber response for CSD")
    public double csdAxial = FitSpharmCSD.DEFAULT_AXIAL;

    @ModuleParameter
    @ModuleDescription("the radial fiber response for CSD")
    public double csdRadial = FitSpharmCSD.DEFAULT_RADIAL;

    @ModuleParameter
    @ModuleDescription("the tau parameter for CSD")
    public double csdTau = FitSpharmCSD.DEFAULT_TAU;

    @ModuleParameter
    @ModuleDescription("the lambda parameter for CSD")
    public double csdLambda = FitSpharmCSD.DEFAULT_LAMBDA;

    @ModuleParameter
    @ModuleDescription("the number of iterations for CSD")
    public int csdIters = FitSpharmCSD.DEFAULT_ITERS;

    @ModuleParameter
    @ModuleExpert
    @ModuleDescription("pass through without fraction fitting (for debugging purposes)")
    public boolean pass = false;

    @ModuleOutput
    @ModuleDescription("output fibers volume")
    public Volume output;

    public Volume fit()
    {
        Logging.info("fitting without spharm cache");

        Pair<Gradients, VectFunction> pair = GradientsTransform.roundit(this.gradients, this.round).subset(this.shells, this.which, this.exclude);
        final Gradients mygradients = pair.a;
        final VectFunction mysubsetter = pair.b;

        Supplier<VectFunction> factory = () ->
        {
            final Supplier<VectFunction> dwi2peaker = () ->
            {
                final Vects mypoints = this.points > 0 ? VectsCreateSphere.odf(this.points) : new VectsCreateSphereLookup().run().output;
                Logging.infosub("using %d odf points", mypoints.size());

                if (this.comps == 1)
                {
                    final VectFunction dwi2tensor = new FitTensorLLS()
                    {{
                        this.gradients = mygradients;
                        this.clamp = 0.0;
                    }}.get();

                    VectFunction tensor2peaks = new VectFunction() {
                        public void apply(Vect input, Vect output)
                        {
                            Tensor tensor = new Tensor(input);
                            Fibers fibers = new Fibers(1);
                            fibers.setBaseline(tensor.getBaseline());
                            fibers.setDiffusivity(tensor.feature(Tensor.FEATURES_AD).get(0));
                            fibers.setFrac(0, tensor.feature(Tensor.FEATURES_FA).get(0));
                            fibers.setLine(0, tensor.feature(Tensor.FEATURES_PD));
                            output.set(fibers.getEncoding());
                        }
                    }.init(Tensor.DT_DIM, new Fibers(1).getEncodingSize());

                    return dwi2tensor.compose(tensor2peaks);
                }

                switch (this.method)
                {
                    case RLD:
                        FitOdfRichardsonLucy odffit = new FitOdfRichardsonLucy();
                        odffit.rlditers = this.rldIters;
                        odffit.alpha = this.rldAlpha;
                        odffit.beta = this.rldBeta;

                        VectFunction dwi2odf = odffit.fitter(mygradients, mypoints);

                        VectFunction odf2peaks = new VolumeOdfPeaks()
                        {{
                            this.thresh = 0.0;
                            this.points = mypoints;
                            this.mode = VolumeFibersFit.this.peak;
                            this.comps = VolumeFibersFit.this.comps;
                            this.cluster = VolumeFibersFit.this.cluster;
                        }}.get();

                        return dwi2odf.compose(odf2peaks);

                    case CSD:
                        FitSpharmCSD fit = new FitSpharmCSD();
                        fit.gradients = mygradients;
                        fit.order = this.csdOrder;
                        fit.iters = this.csdIters;
                        fit.tau = this.csdTau;
                        fit.lambda = this.csdLambda;
                        fit.axial = this.csdAxial;
                        fit.radial = this.csdRadial;
                        VectFunction dwi2spharm = fit.get();

                        VectFunction spharm2peaks = new VolumeSpharmPeaks()
                        {{
                            this.thresh = 0.0;
                            this.comps = VolumeFibersFit.this.comps;
                            this.cluster = VolumeFibersFit.this.cluster;
                        }}.get(dwi2spharm.getDimOut());

                        return dwi2spharm.compose(spharm2peaks);
                    default:
                        throw new RuntimeException("unknown method: " + this.method);
                }
            };

            final VectFunction dwi2tensor = new FitTensorLLS()
            {{
                this.gradients = mygradients;
                this.clamp = 0.0;
            }}.get();

            final VectFunction dwi2peaks = mysubsetter.compose(dwi2peaker.get());
            final Shells sheller = new Shells(mygradients);
            final FitMcsmt smter = new FitMcsmt();
            smter.dmax = this.dmax;
            smter.maxiters = this.maxiters;

            final FitFibersFixedSticks sticker = new FitFibersFixedSticks();
            sticker.dmax = this.dmax;
            sticker.maxiters = this.maxiters;
            sticker.tort = this.tort;
            sticker.dperp = this.dperp;
            sticker.restarts = this.restarts;

            Logging.info("detected shells: " + (mygradients.multishell() ? "multi" : "single"));

            return VectFunction.create((input) ->
            {
                final double baseline = ModelUtils.baselines(mygradients, input).mean();

                Tensor tensor = new Tensor(dwi2tensor.apply(input));

                Fibers empty = new Fibers(0);
                empty.setBaseline(baseline);
                empty.setDiffusivity(tensor.feature(Tensor.FEATURES_MD).get(0));

                Fibers single = new Fibers(1);
                single.setBaseline(baseline);
                single.setDiffusivity(tensor.feature(Tensor.FEATURES_AD).get(0));
                single.setFrac(0, tensor.feature(Tensor.FEATURES_FA).get(0));
                single.setLine(0, tensor.feature(Tensor.FEATURES_PD));

                if (MathUtils.zero(baseline))
                {
                    return new Fibers(this.comps).getEncoding();
                }

                if (this.comps == 0)
                {
                    return empty.convert(this.comps).getEncoding();
                }

                Fibers model = new Fibers(dwi2peaks.apply(input.div(baseline)));
                model.setBaseline(baseline);
                model.setDiffusivity(this.dfix);
                model = model.crop(this.minfrac);

                if (model.size() == 0 || this.pass)
                {
                    return new Fibers(this.comps).getEncoding();
                }

                FitFibersFixedSticks.Mode mode = this.modeSingle;

                List<Fibers> inits = Lists.newArrayList();

                if (mygradients.multishell() && !this.nosmt)
                {
                    mode = this.modeMulti;

                    Mcsmt mcsmt = new Mcsmt(smter.fit(sheller.shells(), sheller.mean(input)));
                    model.setDiffusivity(mcsmt.getDiff());
                    model.setFracSum(mcsmt.getFrac());
                    inits.add(model.copy());
                }
                else
                {
                    if (this.diffinit == DiffInit.Fixed || this.diffinit == DiffInit.Best)
                    {
                        model.setDiffusivity(this.dfix);
                        inits.add(model.copy());
                    }

                    if (this.diffinit == DiffInit.Tensor || this.diffinit == DiffInit.Best)
                    {
                        model.setDiffusivity(tensor.feature(Tensor.FEATURES_AD).get(0));
                        inits.add(model.copy());

                        model.setDiffusivity(tensor.feature(Tensor.FEATURES_RD).get(0));
                        inits.add(model.copy());

                        model.setDiffusivity(tensor.feature(Tensor.FEATURES_MD).get(0));
                        inits.add(model.copy());
                    }
                }

                // This is a first attempt at compartment count selection, but it is too aggressive in some WM
                // inits.add(empty);
                // inits.add(single);

                // for (int i = model.size() - 1; i > 1; i--)
                // {
                //     for (Fibers init : Lists.newArrayList(inits))
                //     {
                //         inits.add(init.convert(i));
                //     }
                // }

                // Collections.reverse(inits);

                Fibers bestModel = model;
                double bestCost = Double.MAX_VALUE;

                sticker.mode = mode;
                for (Fibers init : inits)
                {
                    Fibers myModel = sticker.fit(mygradients, input, init);
                    // myModel.threshSoft(this.minfrac);
                    double myCost = sticker.cost(mygradients, input, myModel);

                    if (myCost <= bestCost)
                    {
                        bestCost = myCost;
                        bestModel = myModel;
                    }
                }

                return bestModel.convert(this.comps).getEncoding();

            }, mygradients.size(), Fibers.size(this.comps));
        };

        return new VolumeFunction(factory).withInput(this.input).withMask(this.mask).withThreads(this.threads).withSlice(!this.columns).run().setModel(ModelType.Fibers);
    }

    @Override
    public Module run()
    {
        this.output = fit();

        return this;
    }
}