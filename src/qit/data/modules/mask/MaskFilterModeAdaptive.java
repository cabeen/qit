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
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Integers;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.math.utils.MathUtils;

import java.util.Map;

@ModuleDescription("Perform mode filtering a mask.  Each voxel will be replaced by the most frequent label in the surrounding neighborhood, so this is like performing non-linear smoothing a mask")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class MaskFilterModeAdaptive implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleDescription("input reference")
    public Volume reference;

    @ModuleParameter
    @ModuleDescription("positional bandwidth")
    public Double hpos = 1d;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("data adaptive bandwidth")
    public Double hval = null;

    @ModuleParameter
    @ModuleDescription("filter radius in voxels (filter will be 2 * support + 1 in each dimension)")
    public int support = 3;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask to restrict filtering")
    public Mask mask;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output probability")
    public Volume probability;

    @Override
    public MaskFilterModeAdaptive run()
    {
        Mask out = this.input.proto();
        Volume prob = this.input.protoVolume(1);

        Sampling sampling = this.input.getSampling();

        int n = 1 + 2 * this.support;
        Volume filter = VolumeSource.gauss(this.input.getSampling(), n, n, n, this.hpos);
        Sampling fsampling = filter.getSampling();

        for (int k = 0; k < sampling.numK(); k++)
        {
            Logging.info(String.format("... processing slice (%d/%d)", k + 1, sampling.numK()));

            for (int j = 0; j < sampling.numJ(); j++)
            {
                for (int i = 0; i < sampling.numI(); i++)
                {
                    if (!this.input.valid(i, j, k, this.mask))
                    {
                        continue;
                    }

                    Map<Integer, Integer> counts = Maps.newHashMap();
                    Map<Integer, Double> weights = Maps.newHashMap();
                    Map<Integer, Double> sums = Maps.newHashMap();
                    int total = 0;

                    double source = this.reference.get(i, j, k, 0);

                    for (Sample fsample : fsampling)
                    {
                        int ni = i + fsample.getI() - this.support;
                        int nj = j + fsample.getJ() - this.support;
                        int nk = k + fsample.getK() - this.support;
                        Sample nsample = new Sample(ni, nj, nk);

                        if (sampling.contains(nsample))
                        {
                            double value = this.reference.get(nsample, 0);
                            double weight = filter.get(fsample, 0);
                            int label = this.input.get(nsample);

                            if (weight < 1e-5)
                            {
                                continue;
                            }

                            if (this.hval != null)
                            {
                                double delta = (source - value);
                                weight *= Math.exp(-delta * delta / (this.hval * this.hval));
                            }

                            if (!counts.containsKey(label))
                            {
                                counts.put(label, 0);
                                weights.put(label, 0.0);
                                sums.put(label, 0.0);
                            }

                            counts.put(label, counts.get(label) + 1);
                            weights.put(label, weights.get(label) + weight);
                            sums.put(label, sums.get(label) + value * weight);
                            total += 1;
                        }
                    }

                    // initialize with the previous label
                    double max = 0;
                    int maxlabel = this.input.get(i, j, k);

                    for (Integer label : counts.keySet())
                    {
                        double prior = counts.get(label) / (double) total;
                        double data = sums.get(label) / weights.get(label);
                        double post = data * prior;

                        // only update if greater than (this will preserve the original if there is a tie equal to one)
                        if (post > max)
                        {
                            max = post;
                            maxlabel = label;
                        }
                    }

                    out.set(i, j, k, maxlabel);
                    prob.set(i, j, k, max);
                }
            }
        }

        this.output = out;
        this.probability = prob;

        return this;
    }
}
