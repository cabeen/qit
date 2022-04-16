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
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskErode;
import qit.data.source.MaskSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectUtils;
import qit.data.utils.volume.VolumeFilter;
import qit.math.utils.MathUtils;

@ModuleDescription("Filter a volume to compute its Hessian")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFilterHessian implements Module
{
    enum VolumeFilterHessianMode { Matrix, Eigen, Determ, Norm, Westin, Spherical, Linear, Planar, Ridge, Blob, DarkBlob, LightBlob}

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("number of threads")
    public int threads = 1;

    @ModuleParameter
    @ModuleDescription("use a sobel operator (default is central finite difference)")
    public boolean sobel = false;

    @ModuleParameter
    @ModuleDescription("return the given output mode")
    public VolumeFilterHessianMode mode = VolumeFilterHessianMode.Matrix;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Volume output;

    public VolumeFilterHessian run()
    {
        Sampling ref = this.input.getSampling();
        Volume volume = this.input;

        Volume fx = this.sobel ? VolumeSource.sobel(ref, 0) : VolumeSource.diffCentralX(ref);
        Volume fy = this.sobel ? VolumeSource.sobel(ref, 1) : VolumeSource.diffCentralY(ref);
        Volume fz = this.sobel ? VolumeSource.sobel(ref, 2) : VolumeSource.diffCentralZ(ref);

        VolumeFilter filterer = new VolumeFilter();
        filterer.withNormalize(false);
        filterer.withThreads(this.threads);

        // use this flag with the above masks to avoid errors on the boundary
        filterer.withBoundary(false);

        Logging.info("...... computing masks");
        final Mask gmask = this.mask == null ? MaskSource.create(ref, 1) : this.mask;
        final Mask hmask = new MaskErode(){{ this.input = gmask; this.num = 2; this.outside = true; this.verbose = false; }}.run().output;

        Logging.info("...... computing first order differences");
        filterer.withMask(gmask);
        Volume dx = filterer.withFilter(fx).withInput(volume).run().getOutput();
        Volume dy = filterer.withFilter(fy).withInput(volume).run().getOutput();
        Volume dz = filterer.withFilter(fz).withInput(volume).run().getOutput();

        Logging.info("...... computing second order differences");
        filterer.withMask(hmask);
        Volume dxx = filterer.withFilter(fx).withInput(dx).run().getOutput();
        Volume dxy = filterer.withFilter(fy).withInput(dx).run().getOutput();
        Volume dxz = filterer.withFilter(fz).withInput(dx).run().getOutput();
        Volume dyx = filterer.withFilter(fx).withInput(dy).run().getOutput();
        Volume dyy = filterer.withFilter(fy).withInput(dy).run().getOutput();
        Volume dyz = filterer.withFilter(fz).withInput(dy).run().getOutput();
        Volume dzx = filterer.withFilter(fx).withInput(dz).run().getOutput();
        Volume dzy = filterer.withFilter(fy).withInput(dz).run().getOutput();
        Volume dzz = filterer.withFilter(fz).withInput(dz).run().getOutput();

        int dim = 1;
        switch(this.mode)
        {
            case Matrix:
                dim = 9;
                break;
            case Westin:
            case Eigen:
                dim = 3;
                break;
        }

        Volume out = VolumeSource.create(ref, dim);

        for (Sample sample : ref)
        {
            if (volume.valid(sample, gmask))
            {
                Matrix hessian = new Matrix(3, 3);

                hessian.set(0, 0, dxx.get(sample, 0));
                hessian.set(0, 1, dxy.get(sample, 0));
                hessian.set(0, 2, dxz.get(sample, 0));
                hessian.set(1, 0, dyx.get(sample, 0));
                hessian.set(1, 1, dyy.get(sample, 0));
                hessian.set(1, 2, dyz.get(sample, 0));
                hessian.set(2, 0, dzx.get(sample, 0));
                hessian.set(2, 1, dzy.get(sample, 0));
                hessian.set(2, 2, dzz.get(sample, 0));

                MatrixUtils.EigenDecomp eig = MatrixUtils.eig(hessian);
                int[] perm = VectUtils.permutation(eig.values.abs());
                double l1 = eig.values.get(perm[2]);
                double l2 = eig.values.get(perm[1]);
                double l3 = eig.values.get(perm[0]);

                double hxx = hessian.get(0, 0);
                double hxy = hessian.get(0, 1);
                double hxz = hessian.get(0, 2);
                double hyy = hessian.get(1, 1);
                double hyz = hessian.get(1, 2);
                double hzz = hessian.get(2, 2);

                double det = hessian.det();
                double norm = hessian.normF();
                double cs = l3 / (l1 + 1e-6);
                double cl = (l1 - l2) / (l1 + 1e-6);
                double cp = (l2 - l3) / (l1 + 1e-6);
                double blob = MathUtils.square(hxx * hyy * hzz) - MathUtils.square(hxz * hxy * hyz);

                double darkblob = det >= 0 ? blob : 0;
                double lightblob = det <= 0 ? blob : 0;

                switch(this.mode)
                {
                    case Matrix:
                        out.set(sample, hessian.packRow());
                        break;
                    case Eigen:
                        out.set(sample, VectSource.create(l1, l2, l3));
                        break;
                    case Westin:
                        out.set(sample, VectSource.create(cs, cp, cl));
                        break;
                    case Linear:
                        out.set(sample, cl);
                        break;
                    case Planar:
                        out.set(sample, cp);
                        break;
                    case Spherical:
                        out.set(sample, cs);
                        break;
                    case Ridge:
                        out.set(sample, l1);
                        break;
                    case Blob:
                        out.set(sample, blob);
                        break;
                    case LightBlob:
                        out.set(sample, lightblob);
                        break;
                    case DarkBlob:
                        out.set(sample, darkblob);
                        break;
                    case Determ:
                        out.set(sample, det);
                        break;
                    case Norm:
                        out.set(sample, norm);
                        break;
                }
            }
        }

        this.output = out;

        return this;
    }
}
