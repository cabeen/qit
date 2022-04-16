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
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Pair;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.models.Tensor;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VolumeUtils;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.function.Function;

@ModuleDescription("Project fibers onto a reference volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersProject implements Module
{
    @ModuleInput
    @ModuleDescription("the input fibers to project")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the reference fibers (may be deformed from another space)")
    public Volume reference;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask of the input image")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a deformation from the input to the reference")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("a threshold for compartment existence")
    public double thresh = 0.01;

    @ModuleParameter
    @ModuleDescription("keep the reference fiber orientations")
    public boolean keep = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maximum angle for matching")
    public Double angle = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply fiber smoothing before projection with the given bandwidth")
    public Double presmooth = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use soft projection with angular smoothing")
    public boolean soft;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("use the given soft smoothing angular bandwidth (zero to one, where greater is more smoothing)")
    public Double soften = 0.1;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("save the angular deviation to the fiber statistic field of the output")
    public boolean deviation = false;

    @ModuleParameter
    @ModuleDescription("apply fiber smoothing after projection")
    public boolean smooth = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("apply smoothing with the given amount (bandwidth in mm) (default is largest voxel dimension)")
    public Double hpos = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("smoothly mix reference and the input voxel fiber orientations (0 is all input, 1 is all reference)")
    public Double mix = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the smoothing filter radius in voxels")
    public int support = 3;

    @ModuleParameter
    @ModuleDescription("the number of threads")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("the output fibers")
    public Volume output;

    @Override
    public VolumeFibersProject run()
    {
        Sampling sampling = this.input.getSampling();

        final int pcomps = Fibers.count(this.reference.getDim());
        final Volume projected = this.input.proto(this.reference.getDim());
        projected.setModel(ModelType.Fibers);

        Logging.info("detected fiber count in input: " + Fibers.count(this.input.getDim()));
        Logging.info("detected fiber count in reference: " + Fibers.count(this.reference.getDim()));

        Function<Sample, Fibers> refGet = (s) -> new Fibers(this.reference.get(s));

        Logging.info("keep is " + this.keep);

        Volume invol = this.input;

        if (this.presmooth != null)
        {
            Logging.info("presmoothing");
            Global.assume(!this.input.getModel().equals(ModelType.Tensor), "presmoothing not available for tensor input");

            invol = new VolumeFibersSmooth()
            {{
                this.input = VolumeFibersProject.this.input;
                this.mask = VolumeFibersProject.this.mask;
                this.hpos = VolumeFibersProject.this.presmooth;
                this.support = VolumeFibersProject.this.support;
                this.threads = VolumeFibersProject.this.threads;
                this.comps = Fibers.count(VolumeFibersProject.this.input.getDim());
            }}.run().output;
        }

        if (this.deform != null)
        {
            Logging.info("deforming");
            Volume refreg = new VolumeFibersTransform()
            {{
                this.input = VolumeFibersProject.this.reference;
                this.reference = VolumeFibersProject.this.input;
                this.deform = VolumeFibersProject.this.deform;
                this.mask = VolumeFibersProject.this.mask;
                this.threads = VolumeFibersProject.this.threads;
                this.comps = pcomps;
            }}.run().output;

            refGet = (s) -> new Fibers(refreg.get(s));
        }

        Function<Vect, Fibers> inputGet = v -> new Fibers(v);

        if (this.input.getModel().equals(ModelType.Tensor))
        {
            inputGet = v ->
            {
                Tensor tensor = new Tensor(v);
                Fibers out = new Fibers(1);
                out.setBaseline(tensor.getBaseline());
                out.setFrac(0, tensor.feature(Tensor.FEATURES_FA).get(0));
                out.setLine(0, tensor.getVec(0));

                return out;
            };
        }

        Function<Pair<Fibers, Fibers>, Fibers> projectHard = (pair) ->
        {
            Fibers test = pair.a;
            Fibers ref = pair.b;
            Fibers out = ref.proto();

            out.setBaseline(test.base);
            out.setDiffusivity(test.diff);

            boolean[] matched = new boolean[test.size()];

            for (int i = 0; i < ref.size(); i++)
            {
                if (ref.getFrac(i) <= this.thresh)
                {
                    continue;
                }

                Vect refLine = ref.getLine(i);

                Vect dists = new Vect(test.size());
                dists.setAll(Double.MAX_VALUE);

                for (int j = 0; j < test.size(); j++)
                {
                    if (matched[j])
                    {
                        continue;
                    }

                    double testFrac = test.getFrac(j);
                    Vect testLine = test.getLine(j);

                    if (testFrac <= this.thresh)
                    {
                        continue;
                    }

                    double dist = refLine.angleLineDeg(testLine);
                    if (this.angle == null || dist <= this.angle)
                    {
                        dists.set(j, dist);
                    }
                }

                int minidx = dists.minidx();
                double mindist = dists.get(minidx);

                if (mindist == Double.MAX_VALUE)
                {
                    continue;
                }

                matched[minidx] = true;
                out.setFrac(i, test.getFrac(minidx));
                out.setStat(i, test.getStat(minidx));
                out.setLabel(i, test.getLabel(minidx));

                if (this.mix == null)
                {
                    out.setLine(i, test.getLine(minidx));
                }
                else
                {
                    Matrix mref = MatrixSource.dyadic(refLine);
                    Matrix mtest = MatrixSource.dyadic(test.getLine(minidx));
                    Matrix mmix = mtest.times(this.mix).plus(1.0 - this.mix, mref);
                    Vect mline = MatrixUtils.eig(mmix).vectors.get(0);

                    out.setLine(i, mline);
                }

                if (this.keep)
                {
                    out.setLine(i, ref.getLine(i));
                }

                if (this.deviation)
                {
                    out.setStat(i, mindist);
                }
            }

            return out;
        };

        Function<Pair<Fibers, Fibers>, Fibers> projectSoft = (pair) ->
        {
            Fibers test = pair.a;
            Fibers ref = pair.b;
            Fibers out = ref.proto();

            out.setBaseline(test.base);
            out.setDiffusivity(test.diff);

            Vect fracs = VectSource.createND(out.size());

            for (int i = 0; i < ref.size(); i++)
            {
                Vect refLine = ref.getLine(i);
                double refFrac = ref.getFrac(i);

                if (refFrac < this.thresh)
                {
                    continue;
                }

                double s = MathUtils.sign(this.soften);
                double k = s * 1.0 / Math.tan(s * Math.PI * this.soften / 2.0);

                double sumExp = 1e-6;
                double sumFrac = 0;
                Matrix sumMat = new Matrix(3, 3);

                for (int j = 0; j < test.size(); j++)
                {
                    double testFrac = test.getFrac(j);
                    Vect testLine = test.getLine(j);

                    if (testFrac > this.thresh)
                    {
                        double dot = testLine.dot(refLine);
                        double exp = Math.exp(k * dot * dot);
                        double prod = exp * testFrac;

                        sumFrac += prod;
                        sumExp += exp;
                        sumMat.plusEquals(MatrixSource.dyadic(testLine).times(prod));
                    }
                }

                fracs.set(i, sumFrac / sumExp);

                if (this.keep)
                {
                    out.setLine(i, refLine);
                }
                else
                {
                    Matrix meanMat = sumMat.times(1.0 / sumExp);

                    if (this.mix != null)
                    {
                        meanMat = meanMat.times(this.mix).plus(1.0 - this.mix, MatrixSource.dyadic(refLine));
                    }

                    MatrixUtils.EigenDecomp eig = MatrixUtils.eig(meanMat);
                    Vect meanLine = eig.vectors.get(0);
                    double coh = eig.values.get(0);

                    out.setLine(i, meanLine);
                    out.setStat(i, coh);
                }
            }

            double fsumTest = test.getFracSum();
            double fsumRef = fracs.sum();

            for (int i = 0; i < ref.size(); i++)
            {
                out.setFrac(i, fsumTest * fracs.get(i) / fsumRef);
            }

            return out;
        };

        Logging.info("projecting");
        for (Sample sample : sampling)
        {
            if (invol.valid(sample, this.mask))
            {
                Fibers test = inputGet.apply(invol.get(sample));
                Fibers ref = refGet.apply(sample);
                Pair pair = Pair.of(test, ref);
                Fibers proj = this.soft ? projectSoft.apply(pair) : projectHard.apply(pair);
                projected.set(sample, proj.getEncoding());
            }
        }

        if (this.smooth)
        {
            Logging.info("postsmoothing");
            final double myhpos = this.hpos != null ? this.hpos : this.input.getSampling().deltaMax();

            this.output = new VolumeFibersSmooth()
            {{
                this.input = projected;
                this.mask = VolumeFibersProject.this.mask;
                this.hpos = myhpos;
                this.support = VolumeFibersProject.this.support;
                this.threads = VolumeFibersProject.this.threads;
                this.comps = pcomps;
            }}.run().output;
        }
        else
        {
            this.output = projected;
        }

        return this;
    }
}
