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
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.utils.MathUtils;

@ModuleDescription("Clean a mask by performing morphological opening followed by closing")
@ModuleAuthor("Ryan Cabeen")
public class MaskAdapt implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume volume;

    @ModuleParameter
    @ModuleDescription("the distance in voxels for adaptation")
    public int diameter = 1;

    @ModuleParameter
    @ModuleDescription("the statistical threshold")
    public double thresh = 1.0;

    @ModuleParameter
    @ModuleDescription("use robust statistics")
    public boolean robust = false;

    @ModuleParameter
    @ModuleDescription("use a Huber estimator robust statistics")
    public boolean robustHuber = false;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @ModuleOutput
    @ModuleDescription("output prob")
    public Volume outputProb;

    @Override
    public MaskAdapt run()
    {
        Mask mask = this.input;
        Volume prob = this.volume.proto();
        Mask out = this.input.proto();

        for (int label : MaskUtils.listNonzero(mask))
        {
            Mask submask = MaskUtils.equal(mask, label);

            Vect values = MaskUtils.values(submask, this.volume);

            VectStats stats = new VectStats();
            stats.withInput(values);
            stats.withRobust(this.robust);
            stats.robustHuber = this.robustHuber;
            stats.run();

            double mean = stats.mean;
            double std = stats.std;

            Logging.info("... processing label: " + label);
            Logging.info("... ... computed mean: " + mean);
            Logging.info("... ... computed std: " + std);
            Logging.info("... ... computed median: " + stats.median);
            Logging.info("... ... computed mad: " + stats.mad);

            if (MathUtils.zero(std))
            {
                std = 1.0;
            }

            Mask extmask = MaskUtils.dilate(mask, this.diameter);
            Mask intmask = MaskUtils.erode(mask, this.diameter);

            for (Sample sample : submask.getSampling())
            {
                if (sample.equals(new Sample(60,61,36)))
                {
                    Logging.info("debug");
                }
                if (intmask.foreground(sample))
                {
                    out.set(sample, label);
                }
                else if (extmask.foreground(sample))
                {
                    double myvalue = this.volume.get(sample, 0);
                    double myprob = Math.abs(myvalue - mean) / std;
                    double prev = prob.get(sample, 0);

                    prob.set(sample, 0, Math.min(myprob, prev));

                    if (myprob < this.thresh && myprob > prev)
                    {
                        out.set(sample, label);
                    }
                }
            }
        }

        this.output = out;
        this.outputProb = prob;

        return this;
    }
}
