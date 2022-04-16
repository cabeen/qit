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

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskIntersection;
import qit.data.utils.MaskUtils;
import qit.data.utils.vects.stats.VectOnlineStats;

import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Function;

@ModuleDescription("Segment a volume using a statistical region-growing technique")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Adams, R., & Bischof, L. (1994). Seeded region growing. IEEE Transactions on pattern analysis and machine intelligence, 16(6), 641-647.")
public class VolumeSegmentRegionGrowing implements Module
{
    @ModuleInput
    @ModuleDescription("the input volume")
    private Volume input;

    @ModuleInput
    @ModuleDescription("the input seed mask")
    private Mask seed;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an option mask for including only specific voxels in the segmentation")
    private Mask include;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an option for excluding voxels from the segmentation")
    private Mask exclude;

    @ModuleParameter
    @ModuleDescription("the neighbor count threshold")
    private int nbThresh = 0;

    @ModuleParameter
    @ModuleDescription("the intensity stopping threshold")
    private Double dataThresh = 3.0;

    @ModuleParameter
    @ModuleDescription("the gradient stopping threshold")
    private Double gradThresh = 3.0;

    @ModuleParameter
    @ModuleDescription("the maxima number of iterations")
    private int maxiter = 10000;

    @ModuleParameter
    @ModuleDescription("the maxima region volume")
    private double maxsize = Double.MAX_VALUE;

    @ModuleParameter
    @ModuleDescription("use a full 27-voxel neighborhood (default is 6-voxel)")
    public boolean full = false;

    @ModuleOutput
    @ModuleDescription("the output segmentation")
    private Mask output;

    public VolumeSegmentRegionGrowing run()
    {
        Volume data = this.input.getVolume(0);
        Volume grad = VolumeGradientMagnitude.apply(data);

        Mask foreground = this.seed.proto();
        foreground.setAll(1);
        foreground = MaskUtils.mask(foreground, this.include);
        foreground.setAll(this.exclude, 0);

        Mask out = MaskUtils.mask(this.seed.copy(), foreground);
        List<Integers> neighborhood = this.full ? Global.NEIGHBORS_27 : Global.NEIGHBORS_6;
        Sampling sampling = this.input.getSampling();
        Queue<Sample> queue = Queues.newConcurrentLinkedQueue();
        VectOnlineStats dataStats = new VectOnlineStats();
        VectOnlineStats gradStats = new VectOnlineStats();

        for (Sample sample : sampling)
        {
            if (data.valid(sample, out))
            {
                dataStats.update(data.get(sample, 0));
                gradStats.update(grad.get(sample, 0));
                out.set(sample, 1);
                queue.add(sample);
            }
        }

        Consumer<String> status = (msg) ->
        {
            Logging.info("==========================================");
            Logging.info(msg);
            Logging.info("... num: " + dataStats.num);
            Logging.info("... data mean: " + dataStats.mean);
            Logging.info("... data std: " + dataStats.std);
            Logging.info("... grad mean: " + gradStats.mean);
            Logging.info("... grad std: " + gradStats.std);
            Logging.info("==========================================");
        };

        status.accept("seed statistics");

        Function<Sample, Integer> nbCount = (sample) ->
        {
            int count = 0;
            for (Integers off : neighborhood)
            {
                Sample nsample = new Sample(sample, off);

                if (data.valid(nsample))
                {
                    count += out.foreground(nsample) ? 1 : 0;
                }
            }
            return count;
        };

        int iter = 0;
        double voxel = sampling.voxvol();
        while (!queue.isEmpty())
        {
            if (this.maxsize > 0 && dataStats.num * voxel > this.maxsize)
            {
                Logging.info("maximum region size reached");
                break;
            }

            if (this.maxiter > 0 && iter > this.maxiter)
            {
                Logging.info("maximum iterations reached");
                break;
            }

            Sample s = queue.poll();
            for (Integers off : neighborhood)
            {
                Sample ns = new Sample(s, off);
                if (data.valid(ns, foreground) && foreground.foreground(ns) && !ns.equals(s) && out.background(ns))
                {
                    boolean nbScore = nbCount.apply(ns) > this.nbThresh;
                    boolean dataScore = Math.abs(data.get(ns, 0) - dataStats.mean) / dataStats.std < this.dataThresh;
                    boolean gradScore = (grad.get(ns, 0) - gradStats.mean) / gradStats.std < this.gradThresh;

                    if (dataScore && gradScore && nbScore)
                    {
                        out.set(ns, 1);
                        queue.add(ns);

                        dataStats.update(data.get(ns, 0));
                        gradStats.update(grad.get(ns, 0));
                    }
                }
            }

            iter += 1;

            if (iter % 5000 == 0)
            {
                status.accept("statistics at iteration " + iter);
            }
        }

        status.accept("final statistics");

        this.output = out;

        return this;
    }

    public Mask getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
