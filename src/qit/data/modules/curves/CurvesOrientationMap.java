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

package qit.data.modules.curves;

import qit.base.Global;
import qit.base.Logging;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.models.Tensor;
import qit.data.modules.mask.MaskDilate;
import qit.data.modules.mri.fibers.VolumeFibersSmooth;
import qit.data.modules.volume.VolumeEnhanceContrast;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.MaskSource;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VolumeUtils;
import qit.math.utils.MathUtils;

@ModuleDescription("Compute an orientation map.  This will find the most likely direction in each voxel")
@ModuleAuthor("Ryan Cabeen")
public class CurvesOrientationMap implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference volume (exclusive with refmask)")
    public Volume refvolume;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference mask (exclusive with refvolume)")
    public Mask refmask;

    @ModuleParameter
    @ModuleDescription("orient the bundle prior to mapping")
    public boolean orient = false;

    @ModuleParameter
    @ModuleDescription("compute a vector orientation (you may want to enable the orient flag)")
    public boolean vector = false;

    @ModuleParameter
    @ModuleDescription("specify a method for normalizing orientation magnitudes")
    public VolumeEnhanceContrast.VolumeEnhanceContrastType norm = VolumeEnhanceContrast.VolumeEnhanceContrastType.Histogram;

    @ModuleParameter
    @ModuleDescription("apply fiber smoothing after mapping")
    public boolean smooth = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("apply smoothing with the given amount (bandwidth in mm) (a negative value will use the largest voxel dimension)")
    public Double sigma = -1.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the smoothing filter radius in voxels")
    public int support = 3;

    @ModuleParameter
    @ModuleDescription("return a fibers volume")
    public boolean fibers = false;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    @Override
    public CurvesOrientationMap run()
    {
        this.output = this.apply();

        return this;
    }

    public Volume apply()
    {
        return this.apply(null);
    }

    public Volume apply(Vect weights)
    {
        Global.assume(this.refmask != null ^ this.refvolume != null, "only refmask or refvolume must be specified but not both");
        Sampling sampling = this.refmask == null ? this.refvolume.getSampling() : this.refmask.getSampling();

        Volume dirs = VolumeSource.create(sampling, 3);
        Volume counts = VolumeSource.create(sampling);
        final Mask mask = new Mask(sampling);
        Curves curves = this.input;

        if (weights == null)
        {
            weights = VectSource.createND(curves.size(), 1.0);
        }

        weights = weights.divSafe(weights.sum());

        if (curves.size() == 0)
        {
            return dirs;
        }

        if (this.orient)
        {
            Logging.info("orienting curves");
            CurvesOrient orienter = new CurvesOrient();
            orienter.input = curves;
            curves = orienter.run().output;
        }

        if (this.vector)
        {
            Volume sum = VolumeSource.create(sampling, 3);

            Logging.info("computing voxelwise vector statistics");
            for (int i = 0; i < curves.size(); i++)
            {
                Curves.Curve curve = curves.get(i);
                double weight = weights.get(i);

                for (Pair<Sample, Vect> pair : sampling.traverseLine(curve.getAll(Curves.COORD)))
                {
                    Sample sample = pair.a;
                    Vect dir = pair.b;

                    if (sampling.contains(sample))
                    {
                        counts.set(sample, counts.get(sample).get(0) + weight);
                        sum.set(sample, sum.get(sample).plus(dir.times(weight)));
                        mask.set(sample, 1);
                    }
                }
            }

            for (Sample sample : sampling)
            {
                if (mask.foreground(sample))
                {
                    dirs.set(sample, sum.get(sample).times(1.0 / counts.get(sample, 0)));
                }
            }
        }
        else
        {
            Volume sum = VolumeSource.create(sampling, 9);

            Logging.info("computing voxelwise axial statistics");
            for (int i = 0; i < curves.size(); i++)
            {
                Curves.Curve curve = curves.get(i);
                double weight = weights.get(i);

                for (Pair<Sample, Vect> pair : sampling.traverseLine(curve.getAll(Curves.COORD)))
                {
                    Sample sample = pair.a;
                    Vect line = pair.b;

                    if (sampling.contains(sample))
                    {
                        counts.set(sample, counts.get(sample).get(0) + weight);
                        sum.set(sample, sum.get(sample).plus(MatrixSource.dyadic(line).times(weight).flatten()));
                        mask.set(sample, 1);
                    }
                }
            }

            for (Sample sample : sampling)
            {
                if (mask.foreground(sample))
                {
                    dirs.set(sample, sum.get(sample).times(1.0 / counts.get(sample, 0)));
                }
            }

            for (Sample sample : sampling)
            {
                if (mask.foreground(sample))
                {
                    Matrix dyad = new Matrix(3, 3).set(sum.get(sample));
                    MatrixUtils.EigenDecomp eig = MatrixUtils.eig(dyad);
                    Vect line = eig.vectors.get(0);
                    double fa = Tensor.fa(eig.values.get(0), eig.values.get(1), eig.values.get(2));

                    dirs.set(sample, line);
                }
            }
        }

        Logging.info("computing intensities");
        Volume fracs = new VolumeEnhanceContrast()
        {{
            this.input = counts;
            this.type = CurvesOrientationMap.this.norm;
            this.nobg = true;
        }}.run().output;

        Logging.info("creating vector output");
        for (Sample sample : sampling)
        {
            double frac = fracs.get(sample).get(0);
            Vect dir = dirs.get(sample);
            dirs.set(sample, dir.times(frac));
        }

        if (this.smooth)
        {
            Mask dmask = new MaskDilate()
            {{
                this.input = mask;
                this.num = CurvesOrientationMap.this.support;
            }}.run().output;

            VolumeFilterGaussian smoother = new VolumeFilterGaussian();
            smoother.input = dirs;
            smoother.support = this.support;
            smoother.sigma = this.sigma;
            smoother.mask = dmask;
            dirs = smoother.run().output;
        }

        return this.fibers ? VolumeUtils.vects2fibers(dirs) : dirs;
    }

    public static Volume apply(Curves mycurves, Volume myref)
    {
        return new CurvesOrientationMap()
        {{
            this.input = mycurves;
            this.refvolume = myref;
        }}.run().output;
    }
}