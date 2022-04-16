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
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.base.structs.Integers;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.source.VectSource;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.structs.GaussianWatsonMixture;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ModuleUnlisted
@ModuleDescription("Segment compartments of a fibers volume with PASCAL")
@ModuleCitation("(in preparation)")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersSegmentPASCAL implements Module
{
    public static final double DEFAULT_GAMMA = 1e-6;

    @ModuleInput
    @ModuleDescription("the input fibers volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the model to apply (specify the filename)")
    public String model;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a deformation")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("a minimum volume fraction to be included")
    public Double minimum = 0.01;

    @ModuleParameter
    @ModuleDescription("include a prior on the unary term based on the compartment volume fraction")
    public Boolean priorUnary = false;

    @ModuleParameter
    @ModuleDescription("include a spatial prior")
    public Boolean priorPairwise = false;

    @ModuleParameter
    @ModuleDescription("specify a weight for the spatial prior")
    public Double gamma = DEFAULT_GAMMA;

    @ModuleParameter
    @ModuleDescription("specify a maximum number of iterations for spatial regularization")
    public Integer maxiters = 100;

    @ModuleParameter
    @ModuleDescription("use a full 27 connected neighborhood for spatial prior")
    public Boolean full = false;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleParameter
    @ModuleDescription("pass through the data without processing them")
    public Boolean pass = false;

    @ModuleOutput
    @ModuleDescription("the output fibers volume")
    public Volume output;

    public VolumeFibersSegmentPASCAL run()
    {
        if (this.pass)
        {
            return this;
        }

        Sampling sampling = this.input.getSampling();
        final Volume out = this.input.proto();

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

                exec.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        VolumeFibersSegmentPASCAL.this.processSlice(fk, out);
                    }
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

        Logging.info("optimizing segmentation");
        optimize(out);

        this.output = out;

        return this;
    }

    private void optimize(Volume data)
    {
        Sampling sampling = data.getSampling();
        Mask select = new Mask(sampling);

        double total = 0;
        for (Sample sample : sampling)
        {
            if (data.valid(sample, this.mask))
            {
                Fibers model = new Fibers(data.get(sample));

                Vect probs = model.getStats();
                int maxidx = probs.maxidx();
                select.set(sample, maxidx);

                total += 1;
            }
            else
            {
                select.set(sample, -1);
            }
        }

        if (this.priorPairwise)
        {
            Logging.info("starting spatial regularization");

            List<Integers> neighbors = this.full ? Global.NEIGHBORS_27 : Global.NEIGHBORS_6;

            for (int iter = 0; iter < this.maxiters; iter++)
            {
                int changed = 0;
                double energy = 0;

                for (Sample sample : sampling)
                {
                    if (!data.valid(sample, this.mask))
                    {
                        continue;
                    }

                    Fibers model = new Fibers(data.get(sample));

                    Vect probs = model.getStats();
                    Vect cost = probs.plus(1e-6).log().times(-1);

                    for (int c = 0; c < model.size(); c++)
                    {
                        double frac = model.getFrac(c);
                        Vect line = model.getLine(c);

                        double dcost = 0;
                        double ncount = 0;
                        for (Integers n : neighbors)
                        {
                            Sample nsample = sample.offset(n);

                            if (!data.valid(nsample, this.mask))
                            {
                                continue;
                            }

                            Fibers nmodel = new Fibers(data.get(nsample));

                            int ncopt = select.get(nsample);
                            double nfrac = nmodel.getFrac(ncopt);
                            Vect nline = nmodel.getLine(ncopt);

                            double dot = line.dot(nline);
                            double dline = 1 - dot * dot;
                            double dfrac = Math.abs(frac - nfrac);

                            dcost += this.gamma * dline * dfrac;
                            ncount += 1;
                        }

                        if (ncount > 0)
                        {
                            dcost /= ncount;
                        }

                        cost.set(c, cost.get(c) + dcost);
                    }

                    int copt = select.get(sample);
                    int dcopt = cost.minidx();

                    if (dcopt != copt)
                    {
                        select.set(sample, dcopt);
                        changed += 1;
                    }

                    energy += cost.get(select.get(sample));
                }

                energy /= total;

                if (changed == 0)
                {
                    Logging.info(String.format("finished spatial regularization after %d iterations", iter + 1));
                    break;
                }

                Logging.info(String.format("... at iteration %d, the cost is %g after changed %d voxels", iter + 1, energy, changed));
            }
        }

        for (Sample sample : sampling)
        {
            Fibers model = new Fibers(data.get(sample));
            model.select(select.get(sample));
            data.set(sample, model.getEncoding());
        }
    }

    private void processSlice(int k, Volume out)
    {
        try
        {
            Sampling sampling = this.input.getSampling();
            GaussianWatsonMixture mixture = GaussianWatsonMixture.read(this.model);
            double delta = sampling.deltaMin();
            VectFunction reorient = this.deform == null ? null : VectFunctionSource.reorient(this.deform, delta, delta, delta);

            Logging.info(String.format("... processing slice %d/%d", k + 1, sampling.numK()));
            for (int j = 0; j < sampling.numJ(); j++)
            {
                for (int i = 0; i < sampling.numI(); i++)
                {
                    Sample sample = new Sample(i, j, k);

                    if (this.input.valid(sample, this.mask))
                    {
                        Fibers model = new Fibers(this.input.get(sample));

                        Vect densities = VectSource.createND(model.size());

                        for (int c = 0; c < model.size(); c++)
                        {
                            if (model.getFrac(c) >= this.minimum)
                            {
                                Vect pos = sampling.world(sample);
                                Vect dir = model.getLine(c);

                                if (this.deform != null)
                                {
                                    Vect cat = reorient.apply(VectSource.cat(pos, dir));
                                    pos = cat.sub(0, 3);
                                    dir = cat.sub(3, 6);
                                }

                                double density = mixture.density(pos, dir);

                                if (this.priorUnary)
                                {
                                    density *= model.getFrac(c);
                                }

                                densities.set(c, density);
                            }
                        }

                        double sumden = densities.sum();

                        if (MathUtils.zero(sumden))
                        {
                            densities.setAll(1.0 / model.size());
                        }
                        else
                        {
                            densities.timesEquals(1.0 / sumden);
                        }

                        for (int c = 0; c < model.size(); c++)
                        {
                            model.setStat(c, densities.get(c));
                        }

                        out.set(sample, model.getEncoding());
                    }
                }
            }
        }
        catch (IOException e)
        {
            Logging.error("failed to read model: " + this.model);
        }
    }
}
