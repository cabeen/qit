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
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectUtils;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

@ModuleDescription("Filter a volume using a Frangi filter.  This extracts tubular structures (scale level is the sigma parameter of a Gaussian in mm)")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Frangi, Alejandro F., et al. \"Multiscale vessel enhancement filtering.\" International Conference on Medical Image Computing and Computer-Assisted Intervention. Springer, Berlin, Heidelberg, 1998.")
public class VolumeFilterFrangi implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("filter support (filter will be 2 * support + 1 voxels in each dimension)")
    public int support = 3;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("number of smoothing iterations")
    public int num = 1;

    @ModuleParameter
    @ModuleDescription("the low scale level")
    public double low = 1.0;

    @ModuleParameter
    @ModuleDescription("the high scale level")
    public double high = 5.0;

    @ModuleParameter
    @ModuleDescription("the number of scale samples")
    public int scales = 5;

    @ModuleParameter
    @ModuleDescription("the alpha Frangi parameter")
    public double alpha = 0.5;

    @ModuleParameter
    @ModuleDescription("the beta Frangi parameter")
    public double beta = 0.5;

    @ModuleParameter
    @ModuleDescription("the gamma Frangi parameter for 3D structures")
    public double gamma = 300;

    @ModuleParameter
    @ModuleDescription("the gamma Frangi parameter for 2D structures")
    public double gammaPlanar = 15;

    @ModuleParameter
    @ModuleDescription("number of threads")
    public int threads = 1;

    @ModuleParameter
    @ModuleDescription("use detect dark tubes instead of bright tubes")
    public boolean dark = false;

    @ModuleParameter
    @ModuleDescription("use a sobel operator (default is central finite difference)")
    public boolean sobel = false;

    @ModuleParameter
    @ModuleDescription("return the full scale space (default is maximum)")
    public boolean full;

    @ModuleParameter
    @ModuleDescription("pass un-masked values through the filter")
    public boolean pass;

    @ModuleOutput
    @ModuleDescription("output filter response")
    public Volume output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output scale of the tubular structure")
    public Volume outputScale;

    public VolumeFilterFrangi run()
    {
        Vect sigmas = VectSource.linspace(this.low, this.high, this.scales);
        Volume volume = this.input;

        Volume scales = this.input.proto(1);
        Volume output = this.input.proto(1);

        for (int i = 0; i < this.scales; i++)
        {
            Logging.info(String.format("processing scale: %d/%d", i + 1, this.scales));
            Logging.info(String.format("... sigma: %g", sigmas.get(i)));

            Logging.info("... computing gaussian");
            VolumeFilterGaussian gaussianer = new VolumeFilterGaussian();
            gaussianer.mask = this.mask;
            gaussianer.num = this.num;
            gaussianer.support = this.support;
            gaussianer.threads = this.threads;
            gaussianer.pass = this.pass;
            gaussianer.sigma = sigmas.get(i);
            gaussianer.input = volume;
            Volume gauss = gaussianer.run().output;

            Logging.info("... computing hessian");
            VolumeFilterHessian hessianer = new VolumeFilterHessian();
            hessianer.mask = this.mask;
            hessianer.threads = this.threads;
            hessianer.sobel = this.sobel;
            hessianer.input = gauss;
            Volume hessian = hessianer.run().output;

            Logging.info("... computing frangi measure");
            final boolean planar = volume.getSampling().planar();
            VolumeFunction frangier = new VolumeFunction(() -> new VectFunction()
            {
                public void apply(Vect inv, Vect outv)
                {
                    if (inv.finite())
                    {
                        outv.set(0, frangi(MatrixSource.createRows(inv, 3, 3), planar));
                    }
                    else
                    {
                        Logging.info("found invalid intensity, skipping frangi");
                        outv.set(0, 0);
                    }
                }
            }.init(9, 1));
            frangier.withMask(this.mask);
            frangier.withThreads(this.threads);
            frangier.withMessages(false);

            Volume frangi = frangier.withInput(hessian).run();

            for (Sample sample : volume.getSampling())
            {
                if (volume.valid(sample, this.mask))
                {
                    double value = frangi.get(sample, 0);
                    double max = output.get(sample, 0);

                    if (value > max)
                    {
                        output.set(sample, 0, value);
                        scales.set(sample, 0, sigmas.get(i));
                    }
                }
            }
        }

        this.output = output;
        this.outputScale = scales;

        return this;
    }

    private double frangi(Matrix hessian, boolean planar)
    {
        double ad = 2 * this.alpha * this.alpha;
        double bd = 2 * this.beta * this.beta;

        MatrixUtils.EigenDecomp eig = MatrixUtils.eig(hessian);

        int[] perm = VectUtils.permutation(eig.values.abs());
        double l1 = eig.values.get(perm[0]);
        double l2 = eig.values.get(perm[1]);
        double l3 = eig.values.get(perm[2]);

        Global.assume(Math.abs(l1) <= Math.abs(l2) && Math.abs(l2) <= Math.abs(l3), "invalid eigen sort");

        if (planar)
        {
            double cd = 2 * this.gammaPlanar * this.gammaPlanar;

            double rb = Math.abs(l2) / Math.abs(l3); // line filter
            double s2 = l2 * l2 + l3 * l3; // square hessian norm

            boolean pass = this.dark ? l3 > 0 : l3 < 0;

            if (Double.isFinite(rb) && pass)
            {
                double fa = Math.exp(-rb * rb / bd);
                double fb = (1 - Math.exp(-s2 / cd));
                return fa * fb;
            }
        }
        else
        {
            double cd = 2 * this.gamma * this.gamma;

            double ra = Math.abs(l2) / Math.abs(l3); // deviation from plate
            double rb = Math.abs(l1) / Math.sqrt(Math.abs(l2 * l3)); // deviation from blob
            double s2 = l1 * l1 + l2 * l2 + l3 * l3; // square hessian norm

            boolean pass = this.dark ? l2 >= 0 && l3 >= 0 : l2 <= 0 && l3 <= 0;

            if (Double.isFinite(rb) && Double.isFinite(ra) && pass)
            {
                double fa = (1 - Math.exp(-ra * ra / ad));
                double fb = Math.exp(-rb * rb / bd);
                double fc = (1 - Math.exp(-s2 / cd));
                return fa * fb * fc;
            }
        }

        return 0;
    }
}
