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

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.structs.Integers;
import qit.data.datasets.*;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@ModuleDescription("Denoise a volume using random matrix theory.  Noise is estimated using a universal Marchenko Pastur distribution and removed via principal component analysis")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Veraart, J., Novikov, D. S., Christiaens, D., Ades-Aron, B., Sijbers, J., & Fieremans, E. (2016). Denoising of diffusion MRI using random matrix theory. NeuroImage, 142, 394-406.")
public class VolumeFilterPCA implements Module
{
    public enum VolumeFilterPCAType {MP, CenterMP}

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the type of PCA filtering to use")
    public VolumeFilterPCAType type = VolumeFilterPCAType.MP;

    @ModuleParameter
    @ModuleDescription("the window size")
    public int window = 5;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("output denoised volume")
    public Volume output = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output noise map")
    public Volume noise = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output pca component count")
    public Volume comps = null;

    public VolumeFilterPCA run()
    {
        Sampling sampling = this.input.getSampling();
        Volume out = this.input.proto();
        Volume est = this.input.proto(1);
        Volume cut = this.input.proto(1);


        Function<Sample,Matrix> Xer = (sample) ->
        {
            int ci = sample.getI();
            int cj = sample.getJ();
            int ck = sample.getK();

            Vects samples = new Vects();
            for (int i = ci - this.window; i <= ci + this.window; i++)
            {
                for (int j = cj - this.window; j <= cj + this.window; j++)
                {
                    for (int k = ck - this.window; k <= ck + this.window; k++)
                    {
                        if (this.input.valid(i, j, k, this.mask))
                        {
                            samples.add(this.input.get(i, j, k));
                        }
                    }
                }
            }

            return MatrixSource.createCols(samples);
        };

        Map<VolumeFilterPCAType,Consumer<Sample>> types = Maps.newHashMap();

        types.put(VolumeFilterPCAType.MP, (sample) ->
        {
            Vect x = this.input.get(sample);
            Matrix X = Xer.apply(sample);

            Matrix Xt = X.transpose();
            Matrix cov = X.times(Xt);
            MatrixUtils.EigenDecomp eig = MatrixUtils.eig(cov);

            int m = X.rows();
            int n = X.cols();

            double lamr = eig.values.get(0) / (double) n;
            double clam = 0.0;
            Double sigma2 = 0.0;
            int cutoffp = 0;

            for (int p = 0; p < m; p++)
            {
                double lam = eig.values.get(p) / (double) n;
                clam += lam;

                double gam = (double) (p + 1) / (double) n;
                double sigsq1 = clam / (p + 1) / Math.max(gam, 1.0);
                double sigsq2 = (lam - lamr) / (4.0) / Math.sqrt(gam);

                if (sigsq2 < sigsq1)
                {
                    sigma2 = sigsq1;
                    cutoffp = p + 1;
                }
            }

            Vect denoised = x;
            if (cutoffp > 0)
            {
                Matrix ev = MatrixSource.createRows(eig.vectors);
                Matrix dv = MatrixSource.diag(new Vect(m).setAll(0, cutoffp, 1.0));
                Matrix M = ev.times(dv.times(ev.transpose()));
                denoised = M.times(x);
            }

            double level = Math.sqrt(sigma2);

            out.set(sample, denoised);
            est.set(sample, level);
            cut.set(sample, cutoffp);
        });

        types.put(VolumeFilterPCAType.CenterMP, (sample) ->
        {
            Vect x = this.input.get(sample);
            Matrix X = Xer.apply(sample);

            Vect mean = X.meanColumn();
            X.minusColsEquals(mean);

            Matrix Xt = X.transpose();
            Matrix cov = X.times(Xt);
            MatrixUtils.EigenDecomp eig = MatrixUtils.eig(cov);

            int m = X.rows();
            int n = X.cols();

            double lamr = eig.values.get(0) / (double) n;
            double clam = 0.0;
            Double sigma2 = 0.0;
            int cutoffp = 0;

            for (int p = 0; p < m; p++)
            {
                double lam = eig.values.get(p) / (double) n;
                clam += lam;

                double gam = (double) (p + 1) / (double) n;
                double sigsq1 = clam / (p + 1) / Math.max(gam, 1.0);
                double sigsq2 = (lam - lamr) / (4.0) / Math.sqrt(gam);

                if (sigsq2 < sigsq1)
                {
                    sigma2 = sigsq1;
                    cutoffp = p + 1;
                }
            }

            Vect denoised = x;
            if (cutoffp > 0)
            {
                Matrix ev = MatrixSource.createRows(eig.vectors);
                Matrix dv = MatrixSource.diag(new Vect(m).setAll(0, cutoffp, 1.0));
                Matrix M = ev.times(dv.times(ev.transpose()));
                denoised = M.times(x.minus(mean)).plus(mean);
            }

            double level = Math.sqrt(sigma2);

            out.set(sample, denoised);
            est.set(sample, level);
            cut.set(sample, cutoffp);
        });

        Consumer<Sample> denoise = types.containsKey(this.type) ? types.get(this.type) : (s) -> {};

        Consumer<Integer> denoiseSlice = (k) ->
        {
            Logging.info("... denoising slice: " + k);

            for (int j = 0; j < sampling.numJ(); j++)
            {
                for (int i = 0; i < sampling.numI(); i++)
                {
                    if (this.input.valid(i, j, k, this.mask))
                    {
                        denoise.accept(new Sample(i, j, k));
                    }
                }
            }
        };

        if (this.threads == null || this.threads < 2)
        {
            for (int k = 0; k < sampling.numK(); k++)
            {
                denoiseSlice.accept(k);
            }
        }
        else
        {
            ExecutorService exec = Executors.newFixedThreadPool(this.threads);

            for (int k = 0; k < sampling.numK(); k++)
            {
                final int fk = k;
                exec.execute(() ->
                {
                    denoiseSlice.accept(fk);
                });
            }

            exec.shutdown();
            try
            {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
            catch (InterruptedException e)
            {
                Logging.error("failed to execute in concurrent mode");
            }

        }

        this.output = out;
        this.noise = est;
        this.comps = cut;

        return this;
    }
}