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

package qit.data.modules.volume;

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
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.math.utils.MathUtils;

@ModuleDescription("Filter a volume with a bilateral filter")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Elad, M. (2002). On the origin of the bilateral filter and ways to improve it. IEEE Transactions on image processing, 11(10), 1141-1151.")
public class VolumeFilterBilateral implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input = null;
    
    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("volume channel (default applies to all)")
    public Integer channel;
    
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
    
    @ModuleParameter
    @ModuleDescription("number of repeats")
    public int iterations = 1;
    
    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    public VolumeFilterBilateral run()
    {
        Global.assume(this.hpos > 0, "hpos must be positive");
        Global.assume(this.support > 0, "support must be positive");
        Global.assume(this.iterations > 0, "iterations must be positive");
        Global.assume(this.mask == null || this.mask.getSampling().equals(this.input.getSampling()), "mask dimensions do not match");

        int n = 1 + 2 * this.support;
        Volume filter = VolumeSource.gauss(this.input.getSampling(), n, n, n, this.hpos);

        int dim = this.input.getDim();
        Sampling sampling = this.input.getSampling();
        Sampling fsampling = filter.getSampling();
        Volume out = this.input.copy();

        Volume volume = this.input;
        for (int i = 0; i < this.iterations; i++)
        {
            // only make another copy if we need it
            if (i > 0)
            {
                volume = out.copy();
            }

            for (int d = 0; d < dim; d++)
            {
                for (Sample sample : sampling)
                {
                    if (!out.valid(sample, this.mask))
                    {
                        continue;
                    }

                    if (this.channel != null && d != this.channel)
                    {
                        out.set(sample, d, volume.get(sample, d));
                        continue;
                    }

                    double source = volume.get(sample, d);

                    double sum = 0;
                    double wsum = 0;

                    for (Sample fsample : fsampling)
                    {
                        int ni = sample.getI() + fsample.getI() - this.support;
                        int nj = sample.getJ() + fsample.getJ() - this.support;
                        int nk = sample.getK() + fsample.getK() - this.support;
                        Sample nsample = new Sample(ni, nj, nk);

                        if (sampling.contains(nsample))
                        {
                            double value = volume.get(nsample, d);
                            double weight = filter.get(fsample, 0);

                            if (this.hval != null)
                            {
                                double delta = (source - value);
                                weight *= Math.exp(-delta * delta / (this.hval * this.hval));
                            }

                            wsum += weight;
                            sum += value * weight;
                        }
                    }

                    if (!MathUtils.zero(wsum))
                    {
                        sum /= wsum;
                    }

                    out.set(sample, d, sum);
                }
            }
        }

        this.output = out;

        return this;
    }
}
