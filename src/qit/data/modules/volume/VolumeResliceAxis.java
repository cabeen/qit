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
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.data.datasets.Affine;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sampling;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.VectUtils;
import qit.data.utils.VectsUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.volume.VolumeSample;
import qit.math.structs.Box;
import qit.math.structs.Line;
import qit.math.structs.Plane;
import qit.math.structs.Quaternion;

@ModuleDescription("Reslice a volume along a given axis (specified by a vects)")
@ModuleAuthor("Ryan Cabeen")
public class VolumeResliceAxis implements Module
{
    private enum VolumeResliceMode {I, J, K}

    @ModuleInput
    @ModuleDescription("input volume")
    private Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input vects (should roughly lie on a straight line)")
    private Vects vects;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the voxel spacing (by default it detects it from the input)")
    private Double delta;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    private Mask mask;

    @ModuleParameter
    @ModuleDescription("specify the target slice")
    private VolumeResliceMode mode = VolumeResliceMode.K;

    @ModuleParameter
    @ModuleDescription("image interpolation method")
    public InterpolationType interp = InterpolationType.Trilinear;

    private Affine xfm;
    private Curves axis;

    @ModuleOutput
    @ModuleDescription("output volume")
    private Volume output;

    public VolumeResliceAxis run()
    {
        int numPoints = this.vects.size();
        Line axisLine = Line.fit(this.vects);
        Vect axisDir = axisLine.getDir();

        {
            Vect ref = this.vects.get(this.vects.size() - 1).minus(this.vects.get(0)).normalize();
            if (axisDir.dot(ref) < 0)
            {
                axisDir.timesEquals(-1.0);
            }
        }

        Vect dir = VectSource.create3D(1, 0, 0);

        switch (this.mode)
        {
            case I:
                dir = VectSource.createX();
                break;
            case J:
                dir = VectSource.createY();
                break;
            case K:
                dir = VectSource.createZ();
                break;
        }

        Vect axisRotation = dir.cross(axisDir);
        double angleRotation = dir.angleRad(axisDir);
        Quaternion quat = new Quaternion(axisRotation, angleRotation);
        Matrix rotate = quat.matrix();

        Logging.info("axis of slicing: " + axisDir);
        Logging.info("axis of rotation: " + axisRotation);
        Logging.info("angle of rotation: " + angleRotation);

        Affine xfm = new Affine(rotate, VectSource.create3D());
        Affine invxfm = new Affine(rotate.inv(), VectSource.create3D());

        Sampling sampling = this.input.getSampling();

        Vects bounds = new Vects();
        bounds.add(sampling.world(0, 0, 0));
        bounds.add(sampling.world(sampling.numI() - 1, 0, 0));
        bounds.add(sampling.world(0, sampling.numJ() - 1, 0));
        bounds.add(sampling.world(sampling.numI() - 1, sampling.numJ() - 1, 0));
        bounds.add(sampling.world(0, 0, sampling.numK() - 1));
        bounds.add(sampling.world(sampling.numI() - 1, 0, sampling.numK() - 1));
        bounds.add(sampling.world(0, sampling.numJ() - 1, sampling.numK() - 1));
        bounds.add(sampling.world(sampling.numI() - 1, sampling.numJ() - 1, sampling.numK() - 1));
        bounds = VectsUtils.apply(bounds, invxfm);

        Box box = Box.create(bounds);

        Vect min = box.getMin();
        Vect max = box.getMax();

        double deltaMin = this.delta != null ? this.delta : sampling.deltaMin();
        int ni = (int) Math.ceil((max.getX() - min.getX()) / deltaMin);
        int nj = (int) Math.ceil((max.getY() - min.getY()) / deltaMin);
        int nk = (int) Math.ceil((max.getZ() - min.getZ()) / deltaMin);

        Vect start = xfm.apply(min);
        Vect deltas = VectSource.create3D(deltaMin, deltaMin, deltaMin);
        Integers nums = new Integers(ni, nj, nk);

        VolumeSample sampleValue = new VolumeSample();
        sampleValue.withSampling(new Sampling(start, deltas, quat, nums));
        sampleValue.withFunction(VolumeUtils.interp(this.interp, this.input));

        if (this.mask != null)
        {
            sampleValue.withMask(this.mask);
        }

        Curves axisCurves = new Curves();
        axisCurves.add(Curves.ALPHA, VectSource.create1D());
        axisCurves.add(Curves.DIST, VectSource.create1D());

        Curves.Curve axisCurve = axisCurves.add(numPoints);

        Vect alphas = VectSource.createND(numPoints);
        Vect dists = VectSource.createND(numPoints);
        Vects points = new Vects();
        for (int i = 0; i < numPoints; i++)
        {
            Vect vect = this.vects.get(i);
            Pair<Double,Vect> param = axisLine.nearestParam(vect);

            points.add(param.b);
            alphas.set(i, param.a);
            dists.set(i, param.b.dist(vect));
        }

        int[] perm = VectUtils.permutation(alphas);
        for (int i = 0; i < numPoints; i++)
        {
            axisCurve.set(Curves.COORD, i, points.get(perm[i]));
            axisCurve.set(Curves.ALPHA, i, VectSource.create1D(alphas.get(perm[i])));
            axisCurve.set(Curves.DIST, i, VectSource.create1D(dists.get(perm[i])));
        }

        this.axis = axisCurves;
        this.xfm = new Affine(rotate, start);
        this.output = sampleValue.getOutput();

        return this;
    }
}
