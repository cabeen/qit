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

import com.google.common.collect.Maps;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.vects.stats.VectOnlineStats;

import java.util.List;
import java.util.Map;

@ModuleDescription("Compute the spherical mean for each shell.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiSphericalMean implements Module
{
    @ModuleInput
    @ModuleDescription("the dwi diffusion-weighted MR volume")
    public Volume dwi;

    @ModuleInput
    @ModuleDescription("the (original) source gradients")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleOutput
    @ModuleDescription("the output resampled diffusion-weighted MR volume")
    public Volume outputDwi;

    @ModuleOutput
    @ModuleDescription("the output b-values")
    public Vects outputBvals;

    public VolumeDwiSphericalMean run()
    {
        Vects outputBvals = new Vects();

        List<Integer> shells = this.gradients.getShells(true);
        Map<Integer,List<Integer>> which = Maps.newHashMap();

        for (Integer shell : shells)
        {
            outputBvals.add(VectSource.create1D(shell));
            which.put(shell, this.gradients.getShellsIdx(false, String.valueOf(shell)));
        }

        Volume outputDwi = this.dwi.proto(shells.size());

        for (Sample sample : this.dwi.getSampling())
        {
            if (this.dwi.valid(sample, this.mask))
            {
                for (int i = 0; i < shells.size(); i++)
                {
                    VectOnlineStats stats = new VectOnlineStats();
                    for (Integer idx : which.get(shells.get(i)))
                    {
                        stats.update(this.dwi.get(sample, idx));
                    }
                    outputDwi.set(sample, i, stats.mean);
                }
            }
        }

        this.outputDwi = outputDwi;
        this.outputBvals = outputBvals;

        return this;
    }
}
