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
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.modules.mask.MaskClose;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeMagnitude;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.modules.volume.VolumeTransform;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VolumeUtils;
import qit.math.utils.MathUtils;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@ModuleDescription("Project fibers onto a reference volume")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class VolumeFibersProjectVectorSoft implements Module
{
    public static final double DEFAULT_GAMMA = 1e-6;

    @ModuleInput
    @ModuleDescription("the input fibers to project")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the reference vectors")
    public Volume reference;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask of the input image")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a deformation field from the input to the reference")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("a minimum vector norm")
    public double normMin = 0.01;

    @ModuleParameter
    @ModuleDescription("a minimum threshold compartment volume fraction")
    public double fracMin = 0.01;

    @ModuleParameter
    @ModuleDescription("a minimum threshold compartment volume fraction")
    public double fracGain = 0.10;

    @ModuleParameter
    @ModuleDescription("a minimum threshold total volume fraction")
    public double fsumMin = 0.025;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maximum angle for matching")
    public Double angleMax = 75d;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the angle width for weighting")
    public Double angleGain = 30d;

    @ModuleParameter
    @ModuleDescription("apply fiber smoothing after projection")
    public boolean smooth = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("apply smoothing with the given amount (bandwidth in mm) (default is largest voxel dimension)")
    public Double sigma = null;

    @ModuleParameter
    @ModuleDescription("only smooth restrict the out fibers")
    public boolean restrict = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the smoothing filter radius in voxels")
    public int support = 3;

    @ModuleParameter
    @ModuleDescription("retain volume fractions after smoothing")
    public Boolean retain = false;

    @ModuleParameter
    @ModuleDescription("normalize by the maximum weight in a final step")
    public Boolean normalize = false;

    @ModuleParameter
    @ModuleDescription("the number of threads")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("the output vector field")
    public Volume output;

    @Override
    public VolumeFibersProjectVectorSoft run()
    {
        final Sampling sampling = this.input.getSampling();
        final Volume refvol = ((Supplier<Volume>) () ->
        {
            if (this.deform != null)
            {
                Logging.info("deforming");
                return new VolumeTransform()
                {{
                    this.input = VolumeFibersProjectVectorSoft.this.reference;
                    this.reference = VolumeFibersProjectVectorSoft.this.input;
                    this.deform = VolumeFibersProjectVectorSoft.this.deform;
                    this.mask = VolumeFibersProjectVectorSoft.this.mask;
                    this.threads = VolumeFibersProjectVectorSoft.this.threads;
                    this.reorient = true;
                }}.run().output;
            }
            else
            {
                return this.reference;
            }
        }).get();

        Logging.info("projecting");
        Volume out = this.input.proto(3);
        for (Sample sample : sampling)
        {
            if (!this.input.valid(sample, this.mask))
            {
                continue;
            }

            Fibers test = new Fibers(this.input.get(sample));
            Vect ref = refvol.get(sample);

            if (ref.norm() <= this.normMin || test.getFracSum() < this.fsumMin)
            {
                continue;
            }

            Vect refdir = ref.normalize();
            Matrix sumd = MatrixSource.identity(3);
            double sumw = 0;
            int count = 0;

            for (int j = 0; j < test.size(); j++)
            {
                double testFrac = test.getFrac(j);
                Vect testLine = test.getLine(j);
                double dist = refdir.angleLineDeg(testLine);

                if (testFrac >= this.fracMin && dist < this.angleMax)
                {
                    double angleDiff = MathUtils.eq(this.angleGain, this.angleMax) ? Global.DELTA : this.angleGain - this.angleMax;
                    double weightAngle = MathUtils.cubicthresh((dist - this.angleMax) / angleDiff);
                    double weightFrac = MathUtils.cubicthresh(testFrac / this.fracGain);
                    double weight = testFrac * weightAngle * weightFrac;

                    sumd.plusEquals(weight, MatrixSource.dyadic(testLine));
                    sumw += weight;
                    count += 1;
                }
            }

            if (count > 0 && MathUtils.nonzero(sumw))
            {
                Vect vect = MatrixUtils.eig(sumd.times(1.0 / sumw)).vectors.get(0).normalize();
                vect.timesEquals(ref.norm() * sumw);
                if (refdir.dot(vect) < 0)
                {
                    vect.timesEquals(-1);
                }

                out.set(sample, vect);
            }
        }

        if (this.smooth)
        {
            Mask mask = new Mask(this.input.getSampling());
            mask.setAll(1);

            if (this.restrict)
            {
                VolumeThreshold thresher = new VolumeThreshold();
                thresher.input = out;
                thresher.magnitude = true;
                thresher.threshold = this.normMin;
                mask = thresher.run().output;

                mask = MaskUtils.largest(mask);

                MaskClose closer = new MaskClose();
                closer.input = mask;
                closer.num = 2;
                mask = closer.run().output;
            }

            Logging.info("smoothing");
            double defsigma = this.input.getSampling().deltaMax();
            double facsigma = this.sigma != null && this.sigma < 0 ? Math.abs(this.sigma) : 1.0;
            final double mysigma = this.sigma != null && this.sigma > 0 ? this.sigma : facsigma * defsigma;
            final Mask mymask = mask;

            final Volume raw = out;
            out = new VolumeFilterGaussian()
            {{
                this.input = raw;
                this.mask = mymask;
                this.sigma = mysigma;
                this.support = VolumeFibersProjectVectorSoft.this.support;
                this.threads = VolumeFibersProjectVectorSoft.this.threads;
            }}.run().output;

            if (this.retain)
            {
                for (Sample sample : mymask.getSampling())
                {
                    if (raw.valid(sample, mymask))
                    {
                        double myraw = raw.get(sample).norm();
                        out.set(sample, out.get(sample).normalize().times(myraw));
                    }
                }
            }
        }

        if (this.normalize)
        {
            double max = VolumeUtils.maxval(VolumeMagnitude.apply(out));
            Logging.info("Normalizing with maximum value: " + max);

            for (Sample sample : out.getSampling())
            {
                out.set(sample, out.get(sample).times(1.0 / max));
            }
        }

        this.output = out;

        return this;
    }
}
