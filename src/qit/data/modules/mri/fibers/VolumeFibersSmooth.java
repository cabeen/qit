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

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleExpert;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.models.Fibers;
import qit.data.utils.VolumeUtils;
import qit.data.utils.mri.estimation.FibersEstimator;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ModuleDescription("Smooth a fibers volume")
@ModuleCitation("Cabeen, R. P., Bastin, M. E., & Laidlaw, D. H. (2016). Kernel regression estimation of fiber orientation mixtures in mri MRI. NeuroImage, 127, 158-172.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersSmooth implements Module
{
    @ModuleInput
    @ModuleDescription("the input fibers volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleExpert
    @ModuleDescription("the estimation type")
    public FibersEstimator.EstimationType estimation = FibersEstimator.EstimationType.Match;

    @ModuleParameter
    @ModuleExpert
    @ModuleDescription("the selection type")
    public FibersEstimator.SelectionType selection = FibersEstimator.SelectionType.Adaptive;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the positional bandwidth in mm (negative value will use the voxel size, zero will skip smoothing)")
    public Double hpos = 1.0;

    @ModuleParameter
    @ModuleDescription("the filter radius in voxels")
    public Integer support = 3;

    @ModuleParameter
    @ModuleDescription("a maxima number of fiber compartments")
    public int comps = 3;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("the baseline signal adaptive bandwidth")
    public Double hsig = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("treat hsig as a fraction of the mean baseline signal (inside the mask)")
    public boolean hsigrel = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("the fraction adaptive bandwidth")
    public Double hfrac = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("the diffusivity adaptive bandwidth")
    public Double hdiff = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("the fiber adaptive bandwidth")
    public Double hdir = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("a minima volume fraction")
    public double min = 0.01;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("a data adaptive threshold")
    public double lambda = 0.99;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("a number of restarts")
    public int restarts = 2;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("pass through the data without smoothing")
    public Boolean pass = false;

    @ModuleOutput
    @ModuleDescription("the output fibers volume")
    public Volume output;

    // a member variable for storing the hsig multiplier
    private Double hfactor = 1.0;

    public VolumeFibersSmooth run()
    {
        if (this.pass || MathUtils.zero(this.hpos))
        {
            this.output = this.input;
            return this;
        }

        Sampling sampling = this.input.getSampling();

        if (this.hsigrel && this.hsig != null)
        {
            VectOnlineStats stats = VolumeUtils.featureStats(this.input, this.mask, val -> new Fibers(val).getBaseline());
            this.hfactor = stats.mean;

            Logging.info("detected mean baseline intensity: " + this.hfactor);
            Logging.info("using relative hsig bandwidth: " + this.hfactor * this.hsig);
        }

        final Volume out = VolumeSource.create(this.input.getSampling(), new Fibers(this.comps).getEncodingSize());
        out.setModel(ModelType.Fibers);

        Logging.progress("smoothing fibers");
        if (this.threads < 2)
        {
            for (int k = 0; k < sampling.numK(); k++)
            {
                processSlice(k, out);
            }
        }
        else
        {
            ExecutorService exec = Executors.newFixedThreadPool(this.threads);

            for (int k = 0; k < sampling.numK(); k++)
            {
                final int fk = k;

                exec.execute(() -> VolumeFibersSmooth.this.processSlice(fk, out));
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

        return this;
    }

    private void processSlice(int k, Volume out)
    {
        double myhpos = this.hpos < 0 ? out.getSampling().deltaMax() : this.hpos;

        FibersEstimator estimator = new FibersEstimator();
        estimator.estimation = this.estimation;
        estimator.selection = this.selection;
        estimator.lambda = this.lambda;
        estimator.maxcomps = this.comps;
        estimator.minfrac = this.min;
        estimator.restarts = this.restarts;

        Fibers proto = new Fibers(this.comps);
        int n = this.support;
        Volume filter = VolumeSource.gauss(out.getSampling(), n, n, n, myhpos);
        Sampling sampling = this.input.getSampling();
        Sampling fsampling = filter.getSampling();

        int cx = (fsampling.numI() - 1) / 2;
        int cy = (fsampling.numJ() - 1) / 2;
        int cz = (fsampling.numK() - 1) / 2;

        Logging.info(String.format("processing slice %d/%d", k + 1, sampling.numK()));
        for (int j = 0; j < sampling.numJ(); j++)
        {
            for (int i = 0; i < sampling.numI(); i++)
            {
                Sample sample = new Sample(i, j, k);

                if (!this.input.valid(sample, this.mask))
                {
                    continue;
                }

                Fibers source = new Fibers(this.input.get(sample));
                Fibers ref = null;
                Fibers initial = null;

                Global.assume(source != null, "invalid model found");

                {
                    // compute reference fibers
                    int count = 0;
                    for (int m = 0; m < source.size(); m++)
                    {
                        if (source.getFrac(m) >= estimator.minfrac)
                        {
                            count++;
                        }
                    }

                    if (count > 0)
                    {
                        ref = new Fibers(count);
                        ref.setBaseline(source.getBaseline());
                        int ridx = 0;
                        for (int m = 0; m < source.size(); m++)
                        {
                            if (source.getFrac(m) >= estimator.minfrac)
                            {
                                ref.setFrac(ridx, source.getFrac(m));
                                ref.setLine(ridx, source.getLine(m));
                                ridx++;
                            }
                        }
                    }
                }

                if (ref == null)
                {
                    ref = source.convert(proto.size());
                }

                List<Double> weights = Lists.newArrayList();
                List<Vect> models = Lists.newArrayList();
                for (Sample fsample : fsampling)
                {
                    int ni = sample.getI() + fsample.getI() - cx;
                    int nj = sample.getJ() + fsample.getJ() - cy;
                    int nk = sample.getK() + fsample.getK() - cz;
                    Sample nsample = new Sample(ni, nj, nk);

                    if (!this.input.valid(nsample, this.mask))
                    {
                        continue;
                    }

                    Vect nmodel = this.input.get(nsample);
                    double weight = filter.get(fsample, 0);

                    // change the model weight when using a bilateral filter
                    if (this.hdir != null)
                    {
                        Global.assume(ref != null, "no reference fibers found");

                        double dist2 = new Fibers(nmodel).dist(ref);
                        double h = this.hdir;
                        double h2 = h * h;
                        double kern = Math.exp(-dist2 / h2);
                        weight *= kern;
                    }

                    if (this.hsig != null)
                    {
                        Global.assume(ref != null, "no reference fibers found");

                        double db = new Fibers(nmodel).getBaseline() - ref.getBaseline();
                        double dist2 = db * db;
                        double h = this.hfactor * this.hsig;
                        double h2 = h * h;
                        double kern = Math.exp(-dist2 / h2);
                        weight *= kern;
                    }

                    if (this.hfrac != null)
                    {
                        Global.assume(ref != null, "no reference fibers found");

                        double db = new Fibers(nmodel).getFracSum() - ref.getFracSum();
                        double dist2 = db * db;
                        double h = this.hfrac;
                        double h2 = h * h;
                        double kern = Math.exp(-dist2 / h2);
                        weight *= kern;
                    }

                    if (this.hdiff != null)
                    {
                        Global.assume(ref != null, "no reference fibers found");

                        double db = new Fibers(nmodel).getDiffusivity() - ref.getDiffusivity();
                        double dist2 = db * db;
                        double h = this.hdiff;
                        double h2 = h * h;
                        double kern = Math.exp(-dist2 / h2);
                        weight *= kern;
                    }

                    models.add(nmodel);
                    weights.add(weight);
                }

                Vect filt = null;

                if (initial != null)
                {
                    filt = estimator.run(initial.getEncoding(), weights, models);
                }
                else
                {
                    filt = estimator.run(weights, models);
                }

                if (filt == null)
                {
                    out.set(sample, source.convert(proto.size()).getEncoding());
                    continue;
                }

                out.set(sample, filt);
            }
        }
    }
}