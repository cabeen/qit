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
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.modules.mask.MaskClose;
import qit.data.modules.mask.MaskDistanceTransform;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.modules.volume.VolumeTransform;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@ModuleDescription("Project fibers onto a reference volume")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class VolumeFibersProjectVector implements Module
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
    public double norm = 0.01;

    @ModuleParameter
    @ModuleDescription("a minimum threshold compartment volume fraction")
    public double frac = 0.025;

    @ModuleParameter
    @ModuleDescription("a minimum threshold total volume fraction")
    public double fsum = 0.1;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maximum angle for matching")
    public Double angle = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply fiber smoothing before projection with the given bandwidth")
    public Double presmooth = null;

    @ModuleParameter
    @ModuleDescription("apply fiber smoothing after projection")
    public boolean smooth = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("apply smoothing with the given amount (bandwidth in mm) (default is largest voxel dimension)")
    public Double sigma = null;

    @ModuleParameter
    @ModuleDescription("only smooth restrict the projected fibers")
    public boolean restrict = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the smoothing filter radius in voxels")
    public int support = 3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("include spatial regularization with a Markov random field")
    public Boolean mrf = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify a weight for the spatial prior")
    public Double gamma = DEFAULT_GAMMA;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify a maximum distance for MRF optimization")
    public Double maxdist = 5.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify a maximum number of iterations for spatial regularization")
    public Integer maxiters = 100;

    @ModuleParameter
    @ModuleDescription("use a full 27 connected neighborhood for spatial prior")
    public Boolean full = false;

    @ModuleParameter
    @ModuleDescription("retain volume fractions after smoothing")
    public Boolean retain = false;

    @ModuleParameter
    @ModuleDescription("the number of threads")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("the output vector field")
    public Volume output;

    @Override
    public VolumeFibersProjectVector run()
    {
        final int comps = Fibers.count(this.input.getDim());
        final Sampling sampling = this.input.getSampling();
        Volume projected = this.input.proto(3);

        final Volume refvol = ((Supplier<Volume>) () ->
        {
            if (this.deform != null)
            {
                Logging.info("deforming");
                return new VolumeTransform()
                {{
                    this.input = VolumeFibersProjectVector.this.reference;
                    this.reference = VolumeFibersProjectVector.this.input;
                    this.deform = VolumeFibersProjectVector.this.deform;
                    this.mask = VolumeFibersProjectVector.this.mask;
                    this.threads = VolumeFibersProjectVector.this.threads;
                    this.reorient = true;
                }}.run().output;
            }
            else
            {
                return this.reference;
            }
        }).get();

        final Volume invol = ((Supplier<Volume>) () ->
        {
            if (this.presmooth != null)
            {
                Logging.info("presmoothing");
                return new VolumeFibersSmooth()
                {{
                    this.input = VolumeFibersProjectVector.this.input;
                    this.mask = VolumeFibersProjectVector.this.mask;
                    this.hpos = VolumeFibersProjectVector.this.presmooth;
                    this.support = VolumeFibersProjectVector.this.support;
                    this.threads = VolumeFibersProjectVector.this.threads;
                    this.comps = Fibers.count(VolumeFibersProjectVector.this.input.getDim());
                }}.run().output;
            }
            else
            {
                return this.input;
            }
        }).get();

        Logging.info("detected fiber count in input: " + comps);

        BiFunction<Vect,Vect,Vect> align = (vect, ref) -> (ref.dot(vect) < 0) ? vect.times(-1) : vect;
        Function<Double,Double> clamp = (v) -> Math.min(1.0, Math.max(0, v));

        if (!this.mrf)
        {
            Function<Sample, Vect> project = (sample) ->
            {
                Fibers test = new Fibers(invol.get(sample));
                Vect ref = refvol.get(sample);

                if (ref.norm() <= this.norm || test.getFracSum() < this.fsum)
                {
                    return VectSource.create3D();
                }

                double mindist = Double.MAX_VALUE;
                Integer minidx = null;
                Vect refdir = ref.normalize();

                for (int j = 0; j < test.size(); j++)
                {
                    double testFrac = test.getFrac(j);
                    Vect testLine = test.getLine(j);

                    if (testFrac <= this.frac)
                    {
                        continue;
                    }

                    double dist = refdir.angleLineDeg(testLine);
                    if ((this.angle == null || dist <= this.angle) && dist < mindist)
                    {
                        mindist = dist;
                        minidx = j;
                    }
                }

                if (minidx == null)
                {
                    return VectSource.create3D();
                }
                else
                {
                    Vect vect = test.getLine(minidx);
                    double frac = test.getFrac(minidx);
                    return align.apply(vect.times(frac), refdir);
                }
            };

            Logging.info("projecting");
            for (Sample sample : sampling)
            {
                if (invol.valid(sample, this.mask))
                {
                    projected.set(sample, project.apply(sample));
                }
            }
        }
        else
        {
            Logging.info("starting spatial regularization");

            List<Integers> neighbors = this.full ? Global.NEIGHBORS_27 : Global.NEIGHBORS_6;

            Volume data = this.input.proto(4 * comps + 1);
            Mask priorMask = VolumeThreshold.apply(VolumeUtils.mag(refvol), this.frac);
            Volume priorDist = MaskDistanceTransform.apply(priorMask);

            double maxAngle = this.angle != null ? this.angle : 90;
            Mask segmentation = new Mask(sampling);
            int countVoxels = 0;

            for (Sample sample : sampling)
            {
                if (invol.valid(sample, this.mask))
                {
                    Vect ref = refvol.get(sample);
                    Fibers test = new Fibers(invol.get(sample));

                    if (ref.norm() > this.norm && test.getFracSum() >= this.fsum)
                    {
                        Vect costs = VectSource.createND(comps + 1);

                        for (int i = 0; i < comps; i++)
                        {
                            double myFrac = test.getFrac(i);

                            if (myFrac <= this.frac)
                            {
                                data.set(sample, 4 * i + 0, 0);
                                data.set(sample, 4 * i + 1, 0);
                                data.set(sample, 4 * i + 2, 0);
                                data.set(sample, 4 * i + 3, 1.0);
                                costs.set(i, 1.0);
                            }
                            else
                            {
                                Vect myVect = align.apply(test.getLine(i).times(myFrac), ref);
                                double myAngle = ref.angleLineDeg(myVect);
                                double myCost = clamp.apply(myAngle / 90);

                                data.set(sample, 4 * i + 0, myVect.getX());
                                data.set(sample, 4 * i + 1, myVect.getY());
                                data.set(sample, 4 * i + 2, myVect.getZ());
                                data.set(sample, 4 * i + 3, myCost);
                                costs.set(i, myCost);
                            }
                        }

                        {
                            double myDist = priorDist.get(sample, 0);
                            double myCost = 1.0 - clamp.apply(myDist / this.maxdist);
                            myCost = Math.min(myCost, maxAngle / 90);

                            data.set(sample, 4 * comps, myCost);
                            costs.set(comps, myCost);
                        }

                        segmentation.set(sample, costs.minidx());
                        countVoxels += 1;
                    }
                }
            }

            BiFunction<Sample, Integer, Double> getCost = (sample, idx) ->
            {
                if (idx == comps)
                {
                    return data.get(sample, 4 * idx);
                }
                else
                {
                    return data.get(sample, 4 * idx + 3);
                }
            };

            BiFunction<Sample, Integer, Vect> getVect = (sample, idx) ->
            {
                if (idx == comps)
                {
                    return VectSource.create3D();
                }
                else
                {
                    double dirx = data.get(sample, 4 * idx + 0);
                    double diry = data.get(sample, 4 * idx + 1);
                    double dirz = data.get(sample, 4 * idx + 2);
                    return VectSource.create3D(dirx, diry, dirz);
                }
            };

            for (int iter = 0; iter < this.maxiters; iter++)
            {
                int changed = 0;
                double energy = 0;

                for (Sample sampleCenter : sampling)
                {
                    if (!data.valid(sampleCenter, this.mask))
                    {
                        continue;
                    }

                    Vect costs = VectSource.createND(comps + 1);

                    for (int labelCenter = 0; labelCenter <= comps; labelCenter++)
                    {
                        Vect vectCenter = getVect.apply(sampleCenter, labelCenter);

                        double cost = 0;
                        double countNeighbor = 0;

                        for (Integers n : neighbors)
                        {
                            Sample sampleOuter = sampleCenter.offset(n);

                            if (!data.valid(sampleOuter, this.mask))
                            {
                                continue;
                            }

                            int labelOuter = segmentation.get(sampleOuter);
                            Vect vectOuter = getVect.apply(sampleOuter, labelOuter);

                            if (labelCenter == comps && labelOuter == comps)
                            {
                                // pass through
                            }
                            else if (labelCenter < comps && labelOuter == comps)
                            {
                                //cost += this.gamma * vectCenter.norm();
                                cost += this.gamma;
                            }
                            else if (labelCenter == comps && labelOuter < comps)
                            {
                                // cost += this.gamma * vectOuter.norm();
                                cost += this.gamma;
                            }
                            else
                            {
                                double myAngle = vectOuter.angleLineDeg(vectCenter);
                                // double myCost = clamp.apply(myAngle / 90);
                                //cost += this.gamma * myCost;
                                if (myAngle > maxAngle)
                                {
                                    cost += this.gamma;
                                }
                            }

                            countNeighbor += 1;
                        }

                        if (countNeighbor > 0)
                        {
                            cost /= countNeighbor;
                        }

                        cost += getCost.apply(sampleCenter, labelCenter);
                        costs.set(labelCenter, cost);
                    }

                    int prevLabel = segmentation.get(sampleCenter);
                    int label = costs.minidx();

                    if (prevLabel != label)
                    {
                        segmentation.set(sampleCenter, label);
                        changed += 1;
                    }

                    energy += costs.get(label);
                }

                Logging.info(String.format("... at iteration %d, the value is %g after changed %d voxels", iter + 1, energy / countVoxels, changed));

                if (changed == 0)
                {
                    Logging.info(String.format("finished spatial regularization after %d iterations", iter + 1));
                    break;
                }
            }

            for (Sample sample : sampling)
            {
                projected.set(sample, getVect.apply(sample, segmentation.get(sample)));
            }
        }

        if (this.smooth)
        {
            Mask mask = new Mask(this.input.getSampling());
            mask.setAll(1);

            if (this.restrict)
            {
                VolumeThreshold thresher = new VolumeThreshold();
                thresher.input = projected;
                thresher.magnitude = true;
                thresher.threshold = this.norm;
                mask = thresher.run().output;

                mask = MaskUtils.largest(mask);

                MaskClose closer = new MaskClose();
                closer.input = mask;
                closer.num = 2;
                mask = closer.run().output;
            }

            Logging.info("postsmoothing");
            double defsigma = this.input.getSampling().deltaMax();
            double facsigma = this.sigma < 0 ? Math.abs(this.sigma) : 1.0;
            final double mysigma = this.sigma != null && this.sigma > 0 ? this.sigma : facsigma * defsigma;
            final Mask mymask = mask;

            Volume out = new VolumeFilterGaussian()
            {{
                this.input = projected;
                this.mask = mymask;
                this.sigma = mysigma;
                this.support = VolumeFibersProjectVector.this.support;
                this.threads = VolumeFibersProjectVector.this.threads;
            }}.run().output;

            if (this.retain)
            {
                for (Sample sample : mymask.getSampling())
                {
                    if (projected.valid(sample, mymask))
                    {
                        double raw = projected.get(sample).norm();
                        out.set(sample, out.get(sample).normalize().times(raw));
                    }
                }
            }

            this.output = out;
        }
        else
        {
            this.output = projected;
        }

        return this;
    }
}
