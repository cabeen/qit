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


package qit.data.modules.mri.kurtosis;

import qit.base.Logging;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Volume;
import qit.data.utils.mri.fitting.FitKurtosisFreeWaterLLS;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.fitting.FitKurtosisLLS;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Fit a kurtosis volume.  Warning: this does not yet estimate the kurtosis indices, e.g. FA, MK, etc.")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Veraart, J., Sijbers, J., Sunaert, S., Leemans, A., Jeurissen, B.: Weighted linear least squares estimation of mri mri parameters: strengths, limitations, and pitfalls. NeuroImage 81 (2013) 335-346")
public class VolumeKurtosisFit implements Module
{
    public enum KurtosisFitType
    {
        LLS, WLLS, FWLLS, FWWLLS
    }

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
    @ModuleDescription("specify an estimation method, (FW enables free water elimination)")
    public KurtosisFitType method = KurtosisFitType.LLS;

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
    @ModuleAdvanced
    @ModuleDescription("the number of threads to use")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("output kurtosis volume")
    public Volume output;

    @Override
    public VolumeKurtosisFit run()
    {
        Supplier<VectFunction> factory = () ->
        {
            Gradients grads = VolumeKurtosisFit.this.gradients.copy();

            boolean subset = false;
            subset |= VolumeKurtosisFit.this.shells != null;
            subset |= VolumeKurtosisFit.this.which != null;
            subset |= VolumeKurtosisFit.this.exclude != null;

            VectFunction subsetter = null;
            if (subset)
            {
                Pair<Gradients, VectFunction> pair = grads.subset(VolumeKurtosisFit.this.shells, VolumeKurtosisFit.this.which, VolumeKurtosisFit.this.exclude);

                grads = pair.a;
                subsetter = pair.b;
            }

            VectFunction fitter = null;
            switch (VolumeKurtosisFit.this.method)
            {
                case LLS:
                {
                    FitKurtosisLLS fit = new FitKurtosisLLS();
                    fit.gradients = grads;
                    fit.weighted = false;
                    fitter = fit.get();
                    break;
                }

                case WLLS:
                {
                    FitKurtosisLLS fit = new FitKurtosisLLS();
                    fit.gradients = grads;
                    fit.weighted = true;
                    fitter = fit.get();
                    break;
                }

                case FWLLS:
                {
                    FitKurtosisFreeWaterLLS fit = new FitKurtosisFreeWaterLLS();
                    fit.gradients = grads;
                    fit.weighted = false;
                    fitter = fit.get();
                    break;
                }

                case FWWLLS:
                {
                    FitKurtosisFreeWaterLLS fit = new FitKurtosisFreeWaterLLS();
                    fit.gradients = grads;
                    fit.weighted = true;
                    fitter = fit.get();
                    break;
                }

                default:
                    Logging.error("unsupported fit type");
            }


            if (subsetter != null)
            {
                fitter = subsetter.compose(fitter);
            }

            return fitter;
        };

        Volume out = new VolumeFunction(factory).withInput(this.input).withMask(this.mask).withThreads(this.threads).run().setModel(ModelType.Kurtosis);

        this.output = out;

        return this;
    }
}
