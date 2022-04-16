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

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;

import java.util.List;

@ModuleDescription("Reduce the number of mri samples in a mri weighted image")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiReduce implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume dwi;

    @ModuleInput
    @ModuleDescription("the input gradients (must match input DWI)")
    public Gradients gradients;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("include only specific shells")
    public String shells = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("include only specific gradients (comma separated zero-based indices)")
    public String which = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("exclude specific gradients (comma separated zero-based indices)")
    public String exclude = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("scale the baseline signal by the given amount (this is very rarely necessary)")
    public Double bscale = null;

    @ModuleOutput
    @ModuleDescription("the output dwi volume")
    public Volume outputDwi;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output gradients")
    public Gradients outputGradients;

    public VolumeDwiReduce run()
    {
        Pair<Gradients, VectFunction> pair = this.gradients.subset(this.shells, this.which, this.exclude);

        Gradients gout = pair.a;
        VectFunction subsetter = pair.b;

        int dim = subsetter.getDimOut();

        Logging.info("reducing number of scans to " + dim);

        Volume out = this.dwi.proto(dim);

        for (Sample sample : this.dwi.getSampling())
        {
            out.set(sample, subsetter.apply(this.dwi.get(sample)));
        }

        if (this.bscale != null)
        {
            Logging.info("scaling baseline volumes");
            List<Integer> bidx = gout.getBaselineIdx();

            for (Sample sample : out.getSampling())
            {
                Vect signal = out.get(sample);

                for (Integer idx : bidx)
                {
                    signal.timesEquals(idx, this.bscale);
                }

                out.set(sample, signal);
            }
        }

        this.outputDwi = out;
        this.outputGradients = gout;

        return this;
    }

}
