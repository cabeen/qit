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

package qit.data.modules.mask;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Volume;
import qit.data.utils.MaskUtils;
import qit.data.utils.vects.stats.VectOnlineStats;

import java.util.Map;

@ModuleDescription("Filter a mask in a variety of possible ways")
@ModuleAuthor("Ryan Cabeen")
public class MaskFilter implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a region to limit the filtering")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference volume")
    public Volume ref;

    @ModuleParameter
    @ModuleDescription("apply a mode filter")
    public boolean mode = false;

    @ModuleParameter
    @ModuleDescription("extract the largest region")
    public boolean largest = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("extract the largest N region")
    public Integer largestn = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minima region voxel count")
    public Integer minvox = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minima region volume")
    public Double minvol = null;

    @ModuleParameter
    @ModuleDescription("extract the region with the highest average average reference value")
    public boolean highest = false;

    @ModuleParameter
    @ModuleDescription("extract the region with the lowest average average reference value")
    public boolean lowest = false;

    @ModuleParameter
    @ModuleDescription("extract the region with the most total reference signal")
    public boolean most = false;

    @ModuleParameter
    @ModuleDescription("binarize the mask at the end")
    public boolean binarize = false;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskFilter run()
    {
        Mask mask = this.input.copy();

        if (this.mode)
        {
            mask = MaskFilterMode.apply(mask, this.mask);
        }

        if (this.largest)
        {
            mask = MaskUtils.largest(mask);
        }

        if (this.largestn != null)
        {
            mask = MaskUtils.largestn(mask, this.largestn);
        }

        if (this.minvol != null)
        {
            mask = MaskUtils.mask(mask, MaskUtils.greater(mask, this.minvol));
        }

        if (this.minvox != null)
        {
            mask = MaskUtils.mask(mask, MaskUtils.greater(mask, this.minvox * mask.getSampling().voxvol()));
        }

        if (this.ref != null)
        {
            Mask cc = MaskComponents.apply(mask);
            Map<Integer, VectOnlineStats> stats = MaskUtils.statsMulti(cc, this.ref);

            Pair<Integer,Double> max = Pair.of(0, 0.0);
            Pair<Integer,Double> min = Pair.of(0, Double.MAX_VALUE);
            Pair<Integer,Double> sum = Pair.of(0, 0.0);

            for (Integer key : stats.keySet())
            {
                double mymean = stats.get(key).mean;
                double mysum = stats.get(key).sum;
                Logging.info(String.format("... region %d mean = %g, sum = %g", key, mymean, mysum));

                if (mymean > max.b)
                {
                   max.a = key;
                   max.b = mymean;
                }

                if (mymean < min.b)
                {
                    min.a = key;
                    min.b = mymean;
                }

                if (mysum > sum.b)
                {
                    sum.a = key;
                    sum.b = mysum;
                }
            }

            if (this.highest)
            {
                mask = MaskUtils.mask(mask, MaskUtils.equal(cc, max.a));
            }

            if (this.lowest)
            {
                mask = MaskUtils.mask(mask, MaskUtils.equal(cc, min.a));
            }

            if (this.most)
            {
                mask = MaskUtils.mask(mask, MaskUtils.equal(cc, sum.a));
            }
        }

        if (this.binarize)
        {
            mask = MaskUtils.binarize(mask);
        }

        this.output = mask;

        return this;
    }
}

