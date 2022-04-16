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

import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Integers;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;

import java.util.Map;

@ModuleDescription("Perform mode filtering a mask.  Each voxel will be replaced by the most frequent label in the surrounding neighborhood, so this is like performing non-linear smoothing a mask")
@ModuleAuthor("Ryan Cabeen")
public class MaskFilterMode implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask to restrict filtering")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the radius in voxels")
    public int radius = 1;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskFilterMode run()
    {
        Mask out = this.input.proto();
        Sampling sampling = this.input.getSampling();
        int r = this.radius;

        for (Sample sample : sampling)
        {
            if (!this.input.valid(sample, this.mask))
            {
                continue;
            }

            Map<Integer, Integer> counts = Maps.newHashMap();

            for (int di = -r; di <= r; di++)
            {
                for (int dj = -r; dj <= r; dj++)
                {
                    for (int dk = -r; dk <= r; dk++)
                    {
                        Sample nsample = sample.offset(new Integers(di, dj, dk));

                        if (!sampling.contains(nsample))
                        {
                            continue;
                        }

                        int label = this.input.get(nsample);

                        if (!counts.containsKey(label))
                        {
                            counts.put(label, 1);
                        }
                        else
                        {
                            counts.put(label, counts.get(label) + 1);
                        }

                    }
                }
            }

            // initialize with the previous label
            int max = 1;
            int maxlabel = this.input.get(sample);

            for (Integer label : counts.keySet())
            {
                int count = counts.get(label);

                // only update if greater than (this will preserve the original if there is a tie equal to one)
                if (count > max)
                {
                    max = count;
                    maxlabel = label;
                }
            }

            out.set(sample, maxlabel);
        }

        this.output = out;
        return this;
    }

    public static Mask apply(Mask mask)
    {
        MaskFilterMode runner = new MaskFilterMode();
        runner.input = mask;
        return runner.run().output;
    }

    public static Mask apply(Mask input, Mask mask)
    {
        MaskFilterMode runner = new MaskFilterMode();
        runner.input = input;
        runner.mask = mask;
        return runner.run().output;
    }
}
