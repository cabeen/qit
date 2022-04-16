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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.*;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeGradient;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.MaskSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;

@ModuleDescription("Dilate a mask morphologically.")
@ModuleAuthor("Ryan Cabeen")
public class MaskDeform implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleParameter
    @ModuleDescription("deformation effect size")
    public double effect = 1;

    @ModuleParameter
    @ModuleDescription("deformation spatial extent")
    public double extent = 1;

    @ModuleParameter
    @ModuleDescription("number of velocity field integrations")
    public int iters = 8;

    @ModuleParameter
    @ModuleDescription("the velocity field interpolation method")
    public InterpolationType interp = InterpolationType.Nearest;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output forward deformation")
    public Volume velocity;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output forward deformation")
    public Deformation forward;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output backward deformation")
    public Deformation backward;

    @Override
    public MaskDeform run()
    {
        Sampling sampling = this.input.getSampling();

        Logging.info("... computing gradient field");
        Mask region = this.input;
        Volume dist = MaskDistanceTransform.apply(region, true);
        Mask outer = MaskUtils.invert(VolumeThreshold.apply(dist, 3 * this.extent));
        Volume grad = VolumeFilterGaussian.apply(VolumeGradient.apply(dist), outer, -0.50);

        Logging.info("... computing velocity field");
        double mindist = VolumeUtils.minval(dist);
        double md2 = mindist * mindist;
        double e2 = this.extent * this.extent;
        Volume myvelocity = grad.copy();

        for (Sample sample : sampling)
        {
            if (outer.foreground(sample))
            {
                double d = dist.get(sample).get(0);
                Vect v = myvelocity.get(sample);
                double f = d < 0 ? 5.0 * d * d / md2 : d * d / e2;
                Vect nv = v.normalize().times(this.effect * Math.exp(-0.5 * f));

                myvelocity.set(sample, nv);
            }
        }

        Volume myforward = myvelocity.proto();
        Volume mybackward = myvelocity.proto();

        for (Sample sample : sampling)
        {
            Vect ident = sampling.world(sample);
            myforward.set(sample, ident);
            mybackward.set(sample, ident);
        }

        VectFunction ivelo = VolumeUtils.interp(this.interp, myvelocity);
        double step = 1.0 / (double) this.iters;

        for (int i = 0; i < this.iters; i++)
        {
            Logging.info("... integrating velocity field");
            for (Sample sample : sampling)
            {
                if (outer.foreground(sample))
                {
                    Vect mf = myforward.get(sample);
                    Vect mb = mybackward.get(sample);

                    myforward.set(sample, mf.plus(step, ivelo.apply(mf)));
                    mybackward.set(sample, mb.minus(step, ivelo.apply(mb)));
                }
            }
        }

        myforward = VolumeFilterGaussian.apply(myforward, -1);
        mybackward = VolumeFilterGaussian.apply(mybackward, -1);

        for (Sample sample : sampling)
        {
            Vect ident = sampling.world(sample);
            myforward.set(sample, myforward.get(sample).minus(ident));
            mybackward.set(sample, mybackward.get(sample).minus(ident));
        }

        this.velocity = myvelocity;
        this.forward = new Deformation(myforward);
        this.backward = new Deformation(mybackward);

        return this;
    }
}