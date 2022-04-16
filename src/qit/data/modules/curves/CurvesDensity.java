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

package qit.data.modules.curves;

import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.modules.volume.VolumeNormalize;
import qit.data.source.SamplingSource;
import qit.data.utils.VolumeUtils;
import qit.math.structs.Box;

@ModuleDescription("Compute a volumetric density of curves.  This works by find the voxels that intersect the curves and accumulating how many curves intersected each voxel.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesDensity implements Module
{
    public enum CurvesDensityType {Count, CountNorm, Color, ColorNorm}

    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference volume (exclusive with refmask)")
    public Volume reference;

    @ModuleParameter
    @ModuleDescription("specify the type of normalization")
    public CurvesDensityType type = CurvesDensityType.Count;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("when no reference sampling is present, create a volume with this voxel size")
    public double delta = 1.0;

    @ModuleOutput
    @ModuleDescription("output density volume")
    public Volume output;

    @Override
    public CurvesDensity run()
    {
        Sampling sampling = null;

        if (this.reference != null)
        {
            sampling = this.reference.getSampling();
        }
        else
        {
            Box box = this.input.bounds().scale(1.1);
            sampling = SamplingSource.create(box, delta);
        }

        switch (this.type)
        {
            case Count:
            {
                this.output = VolumeUtils.density(sampling, this.input, false);
                break;
            }
            case CountNorm:
            {
                this.output = VolumeUtils.density(sampling, this.input, true);
                break;
            }
            case Color:
            {
                this.output = VolumeUtils.color(sampling, this.input, false);
                break;
            }
            case ColorNorm:
            {
                this.output = VolumeUtils.color(sampling, this.input, true);
                break;
            }
        }

        return this;
    }

    public static Volume apply(Curves curves, Volume ref)
    {
        return new CurvesDensity()
        {{
            this.input = curves;
            this.reference = ref;
        }}.run().output;
    }
}
