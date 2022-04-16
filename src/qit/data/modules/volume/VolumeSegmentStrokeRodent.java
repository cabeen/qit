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

import com.google.common.collect.Maps;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.*;
import qit.data.modules.mask.MaskFill;
import qit.data.modules.mask.MaskFilterMode;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.CovarianceType;
import qit.math.structs.Gaussian;

import java.util.Map;

@ModuleDescription("Segment a stroke lesion from a rodent MRI")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class VolumeSegmentStrokeRodent implements Module
{
    @ModuleInput
    @ModuleDescription("input T2 volume")
    public Volume ttwo;

    @ModuleInput
    @ModuleDescription("input ADC volume")
    public Volume adc;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the minimum ADC")
    public double minadc = 0.1;

    @ModuleParameter
    @ModuleDescription("the maximum ADC")
    public double maxadc = 1.0;

    @ModuleParameter
    @ModuleDescription("the minimum T2")
    public double minttwo = 10;

    @ModuleParameter
    @ModuleDescription("the maximum T2")
    public double maxttwo = 150;

    @ModuleParameter
    @ModuleDescription("the number of histogram bins")
    public int bins = 100;

    @ModuleParameter
    @ModuleDescription("the histogram smoothing bandwidth")
    public int width = 10;

    @ModuleParameter
    @ModuleDescription("the minimum lesion volume for inclusion")
    public double minvolume = 10;

    @ModuleOutput
    @ModuleDescription("output segmentation mask")
    public Mask outputLabels;

    @ModuleOutput
    @ModuleDescription("output segmentation probability density map")
    public Volume outputDensity;

    public VolumeSegmentStrokeRodent run()
    {
        VolumeBiHistogram histogrammer = new VolumeBiHistogram();
        histogrammer.x = this.ttwo;
        histogrammer.xmin = this.minttwo;
        histogrammer.xmax = this.maxttwo;
        histogrammer.xbins = this.bins;
        histogrammer.y = this.adc;
        histogrammer.ymin = this.minadc;
        histogrammer.ymax = this.maxadc;
        histogrammer.ybins = this.bins;
        histogrammer.mask = this.mask;
        histogrammer.exclude = true;
        histogrammer.run();

        Volume histogram = histogrammer.output;
        Mask mapping = histogrammer.mapping;

        VolumeFilterGaussian smoother = new VolumeFilterGaussian();
        smoother.input = histogram;
        smoother.num = this.width;
        histogram = smoother.run().output;

        VolumeSegmentLocalExtrema segmenter = new VolumeSegmentLocalExtrema();
        segmenter.input = histogram;
        Mask segmented = segmenter.run().output;

        Sampling sampling = this.adc.getSampling();
        Mask labels = new Mask(sampling);
        Volume density = this.adc.proto(1);
        Map<Integer, VectsOnlineStats> stats = Maps.newHashMap();

        for (Sample sample : sampling)
        {
            if (mapping.foreground(sample))
            {
                int idx = mapping.get(sample);
                int label = segmented.get(idx);
                labels.set(sample, label);

                if (!stats.containsKey(label))
                {
                    stats.put(label, new VectsOnlineStats(3));
                }

                double count = histogram.get(idx, 0);
                density.set(sample, count);

                double ttwoval = this.ttwo.get(sample, 0);
                double adcval = this.ttwo.get(sample, 0);

                Vect data = VectSource.create3D(count, ttwoval, adcval);
                stats.get(label).update(data);
            }
        }

        Integer lesionLabel = null;
        double lesionttwomean = 0;

        Logging.info("... detected segments:");
        for (int label : stats.keySet())
        {
            VectsOnlineStats stat = stats.get(label);
            double volume = stat.num * sampling.voxvol();
            double densum = stat.sum.getX();
            double ttwomean = stat.mean.getY();
            double adcmean = stat.mean.getZ();

            Logging.info(String.format("...... label: %d, volume: %03g, count: %d, density: %03g, mean T2: %03g, mean ADC: %03g", label, volume, stat.num, densum, ttwomean, adcmean));

            if (volume > this.minvolume)
            {
                if (lesionLabel == null || ttwomean > lesionttwomean)
                {
                    lesionttwomean = ttwomean;
                    lesionLabel = label;
                }
            }
        }

        labels = MaskUtils.equal(labels, lesionLabel);
        labels = MaskFill.apply(labels);
        labels = MaskUtils.erode(labels, 1);
        labels = MaskFilterMode.apply(labels);

        Vects samples = new Vects();
        for (Sample sample : sampling)
        {
            if (labels.foreground(sample))
            {
                double ttwoval = this.ttwo.get(sample, 0);
                double adcval = this.adc.get(sample, 0);

                samples.add(VectSource.create2D(ttwoval, adcval));
            }
            else
            {
                labels.set(sample, 0);
                density.set(sample, 0);
            }
        }

        VectsGaussianFitter fitter = new VectsGaussianFitter();
        fitter.withInput(samples);
        fitter.withType(CovarianceType.diagonal);
        Gaussian gaussian = fitter.getOutput();

        Logging.info("Gaussian: " + gaussian.toString());

        labels = MaskUtils.dilate(labels, 3);

        for (Sample sample : sampling)
        {
            if (labels.foreground(sample))
            {
                double ttwoval = this.ttwo.get(sample, 0);
                double adcval = this.adc.get(sample, 0);
                Vect data = VectSource.create2D(ttwoval, adcval);

                double mahal2 = gaussian.mahal2(data);
                double pcum = new ChiSquaredDistribution(1).cumulativeProbability(mahal2);
                double pval = 1 - pcum;

                labels.set(sample, pval > 1e-3 ? 1 : 0);
                density.set(sample, pval);
            }
        }

        labels = MaskFill.apply(labels);

        this.outputLabels = labels;
        this.outputDensity = density;

        return this;
    }

}
