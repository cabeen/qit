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
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
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
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

import java.util.function.Function;

@ModuleDescription("Prune outlier fibers")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersPrune implements Module
{
    @ModuleInput
    @ModuleDescription("the input fibers")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask of the input image")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("globally normalize the volume fractions to be in a unit range")
    public boolean normalize = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a minimum volume fraction")
    public Double min = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a threshold angle for pruning (in degrees)")
    public Double thresh = null;

    @ModuleOutput
    @ModuleDescription("the output fibers")
    public Volume output;

    @Override
    public VolumeFibersPrune run()
    {
        Sampling sampling = this.input.getSampling();
        Volume data = this.input.copy();

        if (this.normalize)
        {
            double fmin = Double.MAX_VALUE;
            double fmax = Double.MIN_VALUE;

            for (Sample sample : sampling)
            {
                if (data.valid(sample, this.mask))
                {
                    double fsum = new Fibers(data.get(sample)).getFracSum();

                    fmax = Math.max(fmax, fsum);
                    fmin = Math.min(fmin, fsum);
                }
            }

            double off = fmin < 0 ? fmin : 0;
            double scale = MathUtils.eq(off, fmax) ? 1.0 : 1.0 / (fmax - off);

            for (Sample sample : sampling)
            {
                if (data.valid(sample, this.mask))
                {
                    Fibers fibers = new Fibers(data.get(sample));
                    for (int i = 0; i < fibers.size(); i++)
                    {
                        double frac = fibers.getFrac(i);
                        double nfrac = scale * (frac - off);
                        fibers.setFrac(i, nfrac);
                    }
                    data.set(sample, fibers.getEncoding());
                }
            }
        }

        if (this.min != null)
        {
            int count = 0;

            for (Sample sample : sampling)
            {
                if (!data.valid(sample, this.mask))
                {
                    continue;
                }

                Fibers fibers = new Fibers(data.get(sample));
                count += fibers.threshSoft(this.min);
                data.set(sample, fibers.getEncoding());
            }

            Logging.infosub("removed %d fibers using volume fraction threshold", count);
        }

        if (this.thresh != null)
        {
            Volume dists = data.proto(Fibers.count(data.getDim()));
            VectOnlineStats stats = new VectOnlineStats();

            for (Sample sample : sampling)
            {
                if (!data.valid(sample, this.mask))
                {
                    continue;
                }

                Vect pos = sampling.world(sample);
                Fibers fibers = new Fibers(data.get(sample));

                for (int i = 0; i < fibers.size(); i++)
                {
                    double frac = fibers.getFrac(i);
                    Vect line = fibers.getLine(i);

                    if (frac <= this.min)
                    {
                        dists.set(sample, i, 180.0);
                        continue;
                    }
                    else
                    {
                        double min = 180.0;

                        for (Sample neigh : sampling.iterateNeighborhood(sample, 1))
                        {
                            if (neigh.equals(sample) || !data.valid(neigh, this.mask))
                            {
                                continue;
                            }

                            Vect npos = sampling.world(neigh);
                            Vect dpos = npos.minus(pos).normalize();

                            Fibers nfibers = new Fibers(data.get(neigh));

                            for (int j = 0; j < nfibers.size(); j++)
                            {
                                double nfrac = nfibers.getFrac(j);
                                Vect nline = nfibers.getLine(j);

                                if (nfrac > this.min)
                                {
                                    double dist = line.angleLineDeg(nline);
                                    dist += line.angleLineDeg(dpos);
                                    dist += nline.angleLineDeg(dpos);
                                    dist /= 3.0;

                                    min = Math.min(min, dist);
                                }
                            }
                        }

                        dists.set(sample, i, min);
                        stats.update(min);
                    }
                }
            }

            int count = 0;

            for (Sample sample : sampling)
            {
                if (!this.input.valid(sample, this.mask))
                {
                    continue;
                }

                Fibers fibers = new Fibers(this.input.get(sample));

                double fsum = fibers.getFracSum();
                double frem = 0;

                for (int i = 0; i < fibers.size(); i++)
                {
                    double dist = dists.get(sample, i);
                    if (dist > this.thresh)
                    {
                        frem += fibers.getFrac(i);
                        fibers.setFrac(i, 0);
                        count += 1;
                    }
                }

                for (int i = 0; i < fibers.size(); i++)
                {
                    double frac = fibers.getFrac(i);
                    double nfrac = MathUtils.eq(fsum, frem) ? 0.0 : frac * fsum / (fsum - frem);
                    fibers.setFrac(i, nfrac);
                }

                data.set(sample, fibers.getEncoding());
            }

            Logging.infosub("removed %d fibers using angular threshold", count);
        }

        this.output = data;

        return this;
    }
}
