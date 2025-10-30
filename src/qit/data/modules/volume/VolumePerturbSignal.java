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
import qit.base.annot.*;
import qit.data.datasets.*;
import qit.data.datasets.Record;
import qit.data.source.VectSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.volume.VolumeVoxelStats;

import java.util.function.BiConsumer;

@ModuleDescription("Perturb the volume signal for data augmentation")
@ModuleAuthor("Ryan Cabeen")
public class VolumePerturbSignal implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input blending volume")
    public Volume blend;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the blending factor sampling mean value")
    public Double blendMean = 0.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the averaging factor sampling standard deviation")
    public Double blendStd = 0.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling standard deviation of the intensity noise perturbation")
    public Double noise = 0.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling mean of the bias field perturbation")
    public Double biasMean = 0.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling standard deviation of the bias field perturbation")
    public Double biasStd = 0.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling mean of the contrast perturbation (zero is no change)")
    public Double contrastMean = 0.0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify the sampling standard deviation of the contrast perturbation (zero is no change)")
    public Double contrastStd = 0.0;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output volume")
    public Volume outputVolume;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output table")
    public Table outputTable;

    @Override
    public VolumePerturbSignal run()
    {
        int dim = this.input.getDim();
        Sampling sampling = this.input.getSampling();
        Volume perturb = this.input.copy();

        Table table = new Table();
        table.addField("name");
        table.addField("value");
        BiConsumer<String, Double> addRow = (name, value) ->
        {
            table.addRecord(new Record().with("name", name).with("value", String.valueOf(value)));
        };

        if (this.blend != null)
        {
            addRow.accept("blend", 1.0);
            addRow.accept("blend_mean", this.blendMean);
            addRow.accept("blend_std", this.blendStd);

            double myblend = Math.min(1.0, Math.max(0, this.blendMean + this.blendStd * Global.RANDOM.nextGaussian()));
            VolumeUtils.lincombEquals(1.0 - myblend, perturb, myblend, this.blend);
        }

        Logging.info("computing image statistics");
        final Volume autosub = VolumeUtils.autodownsample(this.input);
        Mask foreground = new VolumeSegmentForeground()
        {{
            this.input = autosub;
            this.thresh = 0.0;
            this.fill = true;
        }}.run().output;

        double mean = new VolumeVoxelStats().withInput(autosub).withMask(foreground).run().mean;
        Vect center = VolumeUtils.gaussian(autosub).getMean();
        double radius = 0.5 * sampling.deltaMax() * sampling.numMax();

        Logging.info("... detected mean intensity: " + mean);
        Logging.info("... detected center of mass: " + center.toString());
        Logging.info("... detected radius: " + radius);

        Volume biasfield = perturb.proto(1);
        biasfield.setAll(VectSource.create1D(1.0));

        Logging.info("computing bias field");
        Vect poly = VectSource.gaussian(6).times(this.biasStd).plus(this.biasMean).abs();
        addRow.accept("bias_mean", this.biasMean);
        addRow.accept("bias_std", this.biasStd);
        addRow.accept("biasXX_sample", poly.get(0));
        addRow.accept("biasYY_sample", poly.get(1));
        addRow.accept("biasZZ_sample", poly.get(2));
        addRow.accept("biasXY_sample", poly.get(3));
        addRow.accept("biasXZ_sample", poly.get(4));
        addRow.accept("biasYZ_sample", poly.get(5));

        for (Sample sample : sampling)
        {
            Vect world = sampling.world(sample);
            Vect norm = world.minus(center).div(radius);

            double x = norm.getX();
            double y = norm.getY();
            double z = norm.getZ();

            Vect data = VectSource.createND(6);
            data.set(0, x * x);
            data.set(1, y * y);
            data.set(2, z * z);
            data.set(3, x * y);
            data.set(4, x * z);
            data.set(5, y * z);

            double mybias = 1 - data.dot(poly);
            biasfield.set(sample, mybias);
        }

        Volume noisefield = perturb.proto(1);
        double mynoise = this.noise * mean;

        addRow.accept("noise_base", this.noise);
        addRow.accept("noise_sample", mynoise);

        Logging.info("computing noise field");
        for (Sample sample : sampling)
        {
            noisefield.set(sample, VectSource.gaussian(dim).times(mynoise));
        }

        double mycontrast = this.contrastMean + this.contrastStd * Global.RANDOM.nextGaussian();
        addRow.accept("contrast_mean", this.contrastMean);
        addRow.accept("contrast_std", this.contrastStd);
        addRow.accept("contrast_sample", mycontrast);

        Logging.info("applying contrast changes");
        for (Sample sample : sampling)
        {
            for (int i = 0; i < dim; i++)
            {
                double value = perturb.get(sample, i);
                double dval = value / mean;

                value += noisefield.get(sample, i);
                value *= biasfield.get(sample, 0);
                value += 0.5 * mean * mycontrast * dval * dval;
                value = Math.abs(value);

                perturb.set(sample, i, value);
            }
        }

        this.outputVolume = perturb;
        this.outputTable = table;

        return this;
    }
}