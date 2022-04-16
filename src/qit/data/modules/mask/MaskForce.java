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
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeGradient;

@ModuleDescription("Compute a force field of a mask")
@ModuleAuthor("Ryan Cabeen")
public class MaskForce implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input lookup table (must store the index and name of each label)")
    public Table lookup;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the distance for the force field")
    public Double range = 2.0;

    @ModuleParameter
    @ModuleDescription("only include forces outside")
    public boolean outside = false;

    @ModuleParameter
    @ModuleDescription("flip the orientation of the forces")
    public boolean flip = false;

    @ModuleParameter
    @ModuleDescription("apply fiber smoothing after projection")
    public boolean smooth = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("apply smoothing with the given amount (bandwidth in mm) (default is largest voxel dimension)")
    public Double sigma = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the smoothing filter radius in voxels")
    public int support = 3;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("only process certain parcellation labels (by default all will be processed)")
    public String which = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the name field in the lookup table")
    public String name = "name";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the index field in the lookup table")
    public String index = "index";

    @ModuleParameter
    @ModuleDescription("the number of threads")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("the output vectors")
    public Volume output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output mask of labels")
    public Mask labels;

    @Override
    public Module run()
    {
        Mask mask = this.input;

        if (this.which != null)
        {
            MaskExtract extracter = new MaskExtract();
            extracter.input = mask;
            extracter.name = this.name;
            extracter.lookup = this.lookup;
            extracter.index = this.index;
            extracter.label = this.which;
            mask = extracter.run().output;
        }

        MaskDistanceTransform dter = new MaskDistanceTransform();
        dter.input = mask;
        dter.signed = !this.outside;
        Volume dt = dter.run().output;

        VolumeGradient grader = new VolumeGradient();
        grader.input = dt;
        Volume grad = grader.run().output;

        Volume out = grad.proto(3);

        for (Sample sample : dt.getSampling())
        {
            double dtv = Math.abs(dt.get(sample, 0));
            double prob = (this.flip ? 1 : -1) * Math.exp(-1.0 * dtv / this.range);
            out.set(sample, grad.get(sample).normalize().times(prob));
        }

        if (this.smooth)
        {
            final double mysigma = this.sigma != null ? this.sigma : out.getSampling().deltaMax();

            Logging.info("smoothing");
            VolumeFilterGaussian filter = new VolumeFilterGaussian();
            filter.input = out;
            filter.sigma = mysigma;
            filter.support = this.support;
            filter.threads = this.threads;
            out = filter.run().output;
        }

        this.output = out;
        this.labels = mask;

        return this;
    }
}
