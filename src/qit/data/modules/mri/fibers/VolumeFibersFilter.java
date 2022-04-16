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

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

@ModuleDescription("Add orientation and volume fraction noise to a fibers volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersFilter implements Module
{
    @ModuleInput
    @ModuleDescription("input fibers volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the fraction gain")
    public Double base = 1.0;

    @ModuleParameter
    @ModuleDescription("the fraction gain")
    public Double frac = 2.0;

    @ModuleParameter
    @ModuleDescription("the angle gain")
    public Double angle = 1.0;

    @ModuleParameter
    @ModuleDescription("the align gain")
    public Double align = 2.0;

    @ModuleOutput
    @ModuleDescription("output noisy fibers volume")
    public Volume output;

    public VolumeFibersFilter run()
    {
        Volume out = this.input.proto();
        Sampling sampling = out.getSampling();

        VectOnlineStats baselineStats = new VectOnlineStats();
        for (Sample sample : out.getSampling())
        {
            if (this.input.valid(sample, this.mask))
            {
                baselineStats.update(new Fibers(this.input.get(sample)).getBaseline());
            }
        }

        double baseScale = baselineStats.var;

        for (Sample sampleA : out.getSampling())
        {
            if (!this.input.valid(sampleA, this.mask))
            {
                continue;
            }

            Vect posA = sampling.world(sampleA);
            Fibers modelA = new Fibers(this.input.get(sampleA));
            double baseA = modelA.getBaseline();

            for (int i = 0; i < modelA.size(); i++)
            {
                Vect lineA = modelA.getLine(i);
                double fracA = modelA.getFrac(i);

                VectOnlineStats stats = new VectOnlineStats();
                for (Integers offset : Global.NEIGHBORS_27)
                {
                    Sample sampleB = sampleA.offset(offset);

                    if (!sampleB.equals(sampleA) && this.input.valid(sampleB, this.mask))
                    {
                        Vect posB = sampling.world(sampleB);
                        Fibers modelB = new Fibers(this.input.get(sampleB));
                        double baseB = modelB.getBaseline();

                        for (int j = 0; j < modelB.size(); j++)
                        {
                            Vect lineB = modelB.getLine(j);
                            double fracB = modelB.getFrac(j);
                            Vect posAB = posB.minus(posA).normalize();

                            double dotAB = lineA.dot(lineB);
                            double dotPA = posAB.dot(lineA);
                            double dotPB = posAB.dot(lineB);

                            double errFrac = MathUtils.square(fracA - fracB);
                            double errBase = MathUtils.square(baseA - baseB) / baseScale;
                            double errAng = 1.0 - dotAB * dotAB;
                            double errPA = 1.0 - dotPA * dotPA;
                            double errPB = 1.0 - dotPB * dotPB;

                            double score = 1.0;
                            score *= Math.exp(-this.base * errBase);
                            score *= Math.exp(-this.frac * errFrac);
                            score *= Math.exp(-this.angle * errAng);
                            score *= Math.exp(-this.align * errPA);
                            score *= Math.exp(-this.align * errPB);

                            stats.update(score);
                        }
                    }
                }

                modelA.setStat(i, stats.max);
            }

            out.set(sampleA, modelA.getEncoding());
        }

        this.output = out;
        return this;
    }

    public Volume getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
