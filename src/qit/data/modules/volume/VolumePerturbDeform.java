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
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.*;
import qit.data.datasets.Record;
import qit.data.modules.mask.MaskDeform;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.source.VectFunctionSource;

import java.util.function.BiConsumer;

@ModuleDescription("Perturb a volume morphometry for data augmentation")
@ModuleAuthor("Ryan Cabeen")
public class VolumePerturbDeform implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the probability of a flip perturbation (zero to one)")
    public Double flip = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling standard deviation of the angular perturbation (degrees)")
    public Double angle = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling standard deviation of the total scaling perturbation (added to a scaling factor of one)")
    public Double scale = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling standard deviation of the anisotropic scaling perturbation (added to an anisotropic scaling of one)")
    public Double aniso = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling standard deviation of the anisotropic shear perturbation")
    public Double shear = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling standard deviation of the translational shift perturbation")
    public Double shift = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply a deform in the given region")
    public Mask deform = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the amount of deform")
    public Double deformEffect = 3.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the extent of deform")
    public Double deformExtent = 10.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the number of deformation integration iterations")
    public Integer deformIters = 10;

    @ModuleParameter
    @ModuleDescription("randomize the amount of deform with the given sample standard deviation")
    public Double deformRandomize = 0.0;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output volume")
    public Volume outputVolume;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output deformation field")
    public Deformation outputDeform;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output affine transform")
    public Affine outputAffine;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output table")
    public Table outputTable;

    @Override
    public VolumePerturbDeform run()
    {
        Sampling sampling = this.input.getSampling();
        Volume perturb = this.input.copy();

        Table table = new Table();
        table.addField("name");
        table.addField("value");
        BiConsumer<String, Double> addRow = (name, value) ->
        {
            table.addRecord(new Record().with("name", name).with("value", String.valueOf(value)));
        };

        if (this.deform != null)
        {
            double myeffect = this.deformEffect + Global.RANDOM.nextGaussian() * this.deformRandomize;

            addRow.accept("deform_base", this.deformEffect);
            addRow.accept("deform_std", this.deformRandomize);
            addRow.accept("deform_sample", myeffect);

            Logging.info("producing deform");
            MaskDeform deformer = new MaskDeform();
            deformer.input = this.deform;
            deformer.effect = myeffect;
            deformer.extent = this.deformExtent;
            deformer.iters = this.deformIters;
            deformer.run();

            Deformation mydeform = deformer.backward;

            Logging.info("deforming volume");
            VolumeTransform xfm = new VolumeTransform();
            xfm.deform = mydeform;
            xfm.input = this.input;
            xfm.reference = this.input;
            perturb = xfm.run().output;

            this.outputDeform = mydeform;
        }
        else
        {
            this.outputDeform = new Deformation(this.input.proto(3));
        }

        Logging.info("computing image statistics");
        final Volume autosub = VolumeUtils.autodownsample(this.input);
        Vect center = VolumeUtils.gaussian(autosub).getMean();
        double radius = 0.5 * sampling.deltaMax() * sampling.numMax();

        Logging.info("... detected center of mass: " + center.toString());
        Logging.info("... detected radius: " + radius);

        Affine affine = Affine.id(3).plus(1.0, center);

        if (this.flip != null)
        {
            if (Global.RANDOM.nextFloat() < this.flip)
            {
                addRow.accept("flipped", 1.0);
                affine = affine.plus(-1.0, center).times(-1, 1, 1).plus(center);
            }
        }

        if (this.scale != null)
        {
            double myscale = 1 + Global.RANDOM.nextGaussian() * this.scale;
            addRow.accept("scale_std", this.scale);
            addRow.accept("scale_sample", myscale);

            affine = affine.times(myscale, myscale, myscale);
        }

        if (this.aniso != null)
        {
            double myanisox = 1 + Global.RANDOM.nextGaussian() * this.aniso;
            double myanisoy = 1 + Global.RANDOM.nextGaussian() * this.aniso;
            double myanisoz = 1 + Global.RANDOM.nextGaussian() * this.aniso;

            addRow.accept("aniso_std", this.aniso);
            addRow.accept("anisox_sample", myanisox);
            addRow.accept("anisoy_sample", myanisoy);
            addRow.accept("anisoz_sample", myanisoz);

            affine = affine.times(myanisox, myanisoy, myanisoz);
        }

        if (this.shear != null)
        {
            addRow.accept("shear_base", this.shear);

            Matrix mymat = MatrixSource.identity(4);
            for (int i = 0; i < 3; i++)
            {
                for (int j = 0; j < 3; j++)
                {
                    if (i != j)
                    {
                        double myshear = Global.RANDOM.nextGaussian() * this.shear;
                        addRow.accept(String.format("shear%d%d_sample", i, j), myshear);
                        mymat.set(i, j, myshear);
                    }
                }
            }

            affine = affine.compose(new Affine(mymat));
        }

        if (this.angle != null)
        {
            double myrad = Math.toRadians(this.angle);
            double myangle = Global.RANDOM.nextGaussian() * myrad;

            addRow.accept("angle_std", this.angle);
            addRow.accept("angle_sample", myangle);

            Matrix mymat = MatrixSource.rotation(VectSource.random(3).times(myangle));
            affine = affine.compose(new Affine(mymat.hom()));
        }

        if (this.shift != null)
        {
            Vect myshift = VectSource.gaussian(3).times(this.shift);
            addRow.accept("shift_base", this.shift);
            addRow.accept("shiftx_sample", myshift.getX());
            addRow.accept("shifty_sample", myshift.getY());
            addRow.accept("shiftz_sample", myshift.getZ());

            affine = affine.plus(myshift);
        }

        affine = affine.plus(-1, center);

        Logging.info("applying spatial perturbation");
        VolumeTransform xfm = new VolumeTransform();
        xfm.invaffine = affine;
        xfm.input = perturb;
        xfm.reference = this.input;
        perturb = xfm.run().output;

        this.outputVolume = perturb;
        this.outputTable = table;
        this.outputAffine = affine;

        return this;
    }
}