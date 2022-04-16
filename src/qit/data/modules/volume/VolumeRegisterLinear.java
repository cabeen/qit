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
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Affine;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.affine.AffineCreate;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsStats;
import qit.math.structs.Quaternion;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Estimate an affine transform between two volumes")
@ModuleAuthor("Ryan Cabeen")
public class VolumeRegisterLinear implements Module
{
    public static final int PARAM_TRANS_X = 0;
    public static final int PARAM_TRANS_Y = 1;
    public static final int PARAM_TRANS_Z = 2;
    public static final int PARAM_ROT_B = 3;
    public static final int PARAM_ROT_C = 4;
    public static final int PARAM_ROT_D = 5;
    public static final int PARAM_SCALE_X = 6;
    public static final int PARAM_SCALE_Y = 7;
    public static final int PARAM_SCALE_Z = 8;
    public static final int PARAM_SKEW_X = 9;
    public static final int PARAM_SKEW_Y = 10;
    public static final int PARAM_SKEW_Z = 11;
    public static final int PARAM_SIZE = 12;

    public enum VolumeRegisterLinearInit
    {
        World, Center, CenterSize
    }

    @ModuleInput
    @ModuleDescription("input moving volume, which is registered to the reference volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the reference volume")
    public Volume ref;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask used to exclude a portion of the input volume")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask used to exclude a portion of the reference volume")
    public Mask refmask;

    @ModuleParameter
    @ModuleDescription("the type of initialization")
    public VolumeRegisterLinearInit init = VolumeRegisterLinearInit.Center;

    @ModuleOutput
    @ModuleDescription("output transform")
    public Affine output;

    @Override
    public Module run()
    {
        Logging.info("initializing transform");
        Vect param = init();

        Logging.info("optimizing transform");

        Logging.info("final transform");
        print(param);

        this.output = decode(param);

        return this;
    }

    private Vect init()
    {
        Vect param = new Vect(PARAM_SIZE);
        param.set(PARAM_SCALE_X, 1.0);
        param.set(PARAM_SCALE_Y, 1.0);
        param.set(PARAM_SCALE_Z, 1.0);

        if (!this.init.equals(VolumeRegisterLinearInit.World))
        {
            Pair<Vect, Vect> inCenter = center(this.input, this.mask);
            Pair<Vect, Vect> refCenter = center(this.ref, this.refmask);

            Logging.info("input scaleCamera: " + inCenter.b);
            Logging.info("ref scaleCamera: " + refCenter.b);

            if (this.init.equals(VolumeRegisterLinearInit.Center))
            {
                Vect trans = inCenter.a.minus(refCenter.a);

                param.set(PARAM_TRANS_X, trans.getX());
                param.set(PARAM_TRANS_Y, trans.getY());
                param.set(PARAM_TRANS_Z, trans.getZ());
            }
            else if (this.init.equals(VolumeRegisterLinearInit.CenterSize))
            {
                Vect scale = refCenter.b.div(inCenter.b);
                Vect trans = inCenter.a.minus(scale.times(refCenter.a));

                param.set(PARAM_TRANS_X, trans.getX());
                param.set(PARAM_TRANS_Y, trans.getY());
                param.set(PARAM_TRANS_Z, trans.getZ());

                param.set(PARAM_SCALE_X, scale.getX());
                param.set(PARAM_SCALE_Y, scale.getY());
                param.set(PARAM_SCALE_Z, scale.getZ());
            }
        }

        return param;
    }

    private static void print(Vect param)
    {
        Logging.info("... translation X: " + param.get(PARAM_TRANS_X));
        Logging.info("... translation Y: " + param.get(PARAM_TRANS_Y));
        Logging.info("... translation Z: " + param.get(PARAM_TRANS_Z));

        Logging.info("... rotation B: " + param.get(PARAM_ROT_B));
        Logging.info("... rotation C: " + param.get(PARAM_ROT_C));
        Logging.info("... rotation D: " + param.get(PARAM_ROT_D));

        Logging.info("... scaleCamera X: " + param.get(PARAM_SCALE_X));
        Logging.info("... scaleCamera Y: " + param.get(PARAM_SCALE_Y));
        Logging.info("... scaleCamera Z: " + param.get(PARAM_SCALE_Z));

        Logging.info("... skew X: " + param.get(PARAM_SKEW_X));
        Logging.info("... skew Y: " + param.get(PARAM_SKEW_Y));
        Logging.info("... skew Z: " + param.get(PARAM_SKEW_Z));
    }

    private static Affine decode(Vect param)
    {
        AffineCreate creator = new AffineCreate();

        creator.transX = param.get(PARAM_TRANS_X);
        creator.transY = param.get(PARAM_TRANS_Y);
        creator.transZ = param.get(PARAM_TRANS_Z);

        creator.scaleX = param.get(PARAM_SCALE_X);
        creator.scaleY = param.get(PARAM_SCALE_Y);
        creator.scaleZ = param.get(PARAM_SCALE_Z);

        creator.skewX = param.get(PARAM_SKEW_X);
        creator.skewY = param.get(PARAM_SKEW_Y);
        creator.skewZ = param.get(PARAM_SKEW_Z);

        return creator.run().output;
    }

    private static Pair<Vect, Vect> center(Volume input, Mask mask)
    {
        boolean any = false;
        double wsum = 0;
        Vect mean = VectSource.create3D();
        Vect var = VectSource.create3D();

        Sampling sampling = input.getSampling();
        for (Sample sample : sampling)
        {
            if (input.valid(sample, mask))
            {
                double w = input.get(sample, 0);
                Vect v = sampling.world(sample);

                if (MathUtils.nonzero(w))
                {
                    any = true;
                    for (int i = 0; i < v.size(); i++)
                    {
                        wsum += w;

                        Vect meanOld = mean.copy();
                        Vect dm = v.minus(meanOld);
                        mean.plusEquals(dm.times(w / wsum));
                        var.plusEquals(v.minus(mean).times(dm).times(w));
                    }
                }
            }
        }

        if (!any)
        {
            Logging.error("an empty volume was found!");
        }

        Vect std = var.div(wsum).sqrt();

        return Pair.of(mean, std);
    }
}
