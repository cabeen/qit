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


package qit.data.utils.mri.estimation;

import com.google.common.collect.Lists;
import java.util.List;
import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.models.Fibers;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.VectUtils;
import qit.data.utils.mri.structs.ModelEstimator;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsAxialStats;
import qit.data.utils.vects.cluster.VectsClusterAKM;
import qit.math.utils.MathUtils;

public class FibersEstimator extends ModelEstimator
{
    public enum EstimationType { Match, Rank }
    public enum SelectionType { Max, Fixed, Linear, Adaptive }

    public int restarts = 5;
    public int maxcomps = 3;
    public double minfrac = 0.01;
    public EstimationType estimation = EstimationType.Match;
    public SelectionType selection = SelectionType.Adaptive;
    public Double lambda = 0.99; // for adaptive selection

    public Fibers proto()
    {
        return new Fibers(this.maxcomps);
    }

    private Vect rank(List<Double> weights, List<Vect> input)
    {
        Global.assume(SelectionType.Fixed.equals(this.selection), "rank-based estimation requires a fixed model size");

        int num = input.size();
        Fibers out = new Fibers(this.maxcomps);
        {
            double s0sum = 0;
            double dsum = 0;
            double wsum = 0;
            for (int i = 0; i < input.size(); i++)
            {
                double w = weights.get(i);
                Fibers model = new Fibers(input.get(i));
                double s0 = model.getBaseline();
                double d = model.getDiffusivity();

                s0sum += w * s0;
                dsum += w * Math.log(d);
                wsum += w;
            }

            if (MathUtils.zero(wsum))
            {
                return out.getEncoding();
            }

            double norm = 1.0 / wsum;
            double s0mean = norm * s0sum;
            double dmean = Math.exp(norm * dsum);

            out.setBaseline(s0mean);
            out.setDiffusivity(dmean);
        }

        Vect[] compWeights = new Vect[maxcomps];
        Vect[] compFracs = new Vect[maxcomps];
        Vects[] compLines = new Vects[maxcomps];

        for (int j = 0; j < maxcomps; j++)
        {
            compWeights[j] = new Vect(num);
            compFracs[j] = new Vect(num);
            compLines[j] = new Vects();
        }

        for (int i = 0; i < num; i++)
        {
            Fibers fibers = new Fibers(input.get(i));

            // compute ranking based on volume fraction
            int[] perm = VectUtils.permutation(fibers.getFracs());

            for (int j = 0; j < maxcomps; j++)
            {
                int pidx = maxcomps - 1 - perm[j];
                double weight = weights.get(i);

                compWeights[j].set(i, weight);
                compLines[j].add(fibers.getLine(pidx));
                compFracs[j].set(i, fibers.getFrac(pidx));
            }
        }

        for (int j = 0; j < maxcomps; j++)
        {
            double fmean = new VectStats().withInput(compFracs[j]).withWeights(compWeights[j]).run().mean;
            Vect lmean = new VectsAxialStats().withInput(compLines[j]).withWeights(compWeights[j]).run().mean;

            out.setFrac(j, fmean);
            out.setLine(j, lmean);
        }

        return out.getEncoding();
    }

    private Vect match(Vect initial, List<Double> weights, List<Vect> input)
    {
        Fibers out = new Fibers(this.maxcomps);
        {
            double s0sum = 0;
            double dsum = 0;
            double wsum = 0;
            for (int i = 0; i < input.size(); i++)
            {
                double w = weights.get(i);
                Fibers model = new Fibers(input.get(i));
                double s0 = model.getBaseline();
                double d = model.getDiffusivity();

                s0sum += w * s0;
                dsum += w * Math.log(d);
                wsum += w;
            }

            if (MathUtils.zero(wsum))
            {
                return out.getEncoding();
            }

            double norm = 1.0 / wsum;
            double s0mean = norm * s0sum;
            double dmean = Math.exp(norm * dsum);

            out.setBaseline(s0mean);
            out.setDiffusivity(dmean);
        }

        // set up the orientation optimization problem
        Vect sourceFracs = null;
        Vect sourceWeights = null;
        Vects sourceLines = null;

        int maxncomps = 0;
        int ecomps = 0;
        {
            int num = input.size();
            double countw = 0;
            double countsumw = 0;

            List<Double> bufferFracs = Lists.newArrayList();
            List<Vect> bufferLines = Lists.newArrayList();
            List<Double> bufferWeights = Lists.newArrayList();

            // perform some data wrangling
            for (int i = 0; i < num; i++)
            {
                double weight = weights.get(i);
                Fibers fibers = new Fibers(input.get(i));
                int count = 0;

                for (int j = 0; j < fibers.size(); j++)
                {
                    Vect line = fibers.getLine(j);
                    double frac = fibers.getFrac(j);

                    if (MathUtils.zero(weight) || MathUtils.zero(frac) || MathUtils.zero(line.norm()) || frac < minfrac)
                    {
                        continue;
                    }

                    bufferLines.add(line);
                    bufferFracs.add(frac);
                    bufferWeights.add(weight);

                    count += 1;
                }

                countw += weight * count;
                countsumw += weight;
                maxncomps = Math.max(count, maxncomps);
            }
            int count = bufferFracs.size();

            if (count == 0)
            {
                return out.getEncoding();
            }

            // set up the optimization problem
            sourceFracs = VectSource.create(bufferFracs);
            sourceWeights = VectSource.create(bufferWeights);
            sourceLines = VectsSource.create(bufferLines);

            if (MathUtils.zero(countsumw))
            {
                return out.getEncoding();
            }

            // normalize weights
            sourceWeights.timesEquals(1.0 / countsumw);

            if (SelectionType.Fixed.equals(selection) && count < maxcomps)
            {
                for (int i = 0; i < count; i++)
                {
                    double frac = sourceFracs.get(i) * sourceWeights.get(i);
                    out.setFrac(i, frac);
                    out.setLine(i, sourceLines.get(i));
                }

                return out.getEncoding();
            }

            // estimate number of components
            ecomps = Math.max(1, (int) Math.round(countw / countsumw));
        }

        Vect clusterWeights = sourceWeights.copy();
        clusterWeights.timesEquals(sourceFracs);
        double clusterWeightSum = clusterWeights.sum();

        if (MathUtils.zero(clusterWeightSum))
        {
            return out.getEncoding();
        }

        Vects clusterVects = sourceLines;
        Vects clusterInitial = initial != null ? new Fibers(initial).getLines() : null;
        clusterWeights.timesEquals(1.0 / clusterWeightSum);

        VectsClusterAKM cluster = null;
        if (SelectionType.Fixed.equals(selection))
        {
            cluster = cluster(maxcomps, this.restarts, clusterInitial, clusterWeights, clusterVects);
        }
        else if (SelectionType.Max.equals(selection))
        {
            cluster = cluster(maxncomps, this.restarts, clusterInitial, clusterWeights, clusterVects);
        }
        else if (SelectionType.Linear.equals(selection))
        {
            cluster = cluster(ecomps, this.restarts, clusterInitial, clusterWeights, clusterVects);
        }
        else if (SelectionType.Adaptive.equals(selection))
        {
            VectsClusterAKM[] clusters = new VectsClusterAKM[maxcomps];
            double[] costs = new double[maxcomps];

            for (int i = 1; i <= maxcomps; i++)
            {
                VectsClusterAKM c = cluster(i, this.restarts, clusterInitial, clusterWeights, clusterVects);
                clusters[i - 1] = c;
                costs[i - 1] = c.cost() + this.lambda * i;
            }

            cluster = clusters[MathUtils.minidx(costs)];
        }
        else
        {
            Logging.info("no model selection specified");
        }

        int k = cluster.getK();
        int[] labels = cluster.getLabels();
        double[] cfracs = new double[k];
        for (int i = 0; i < k; i++)
        {
            int label = i + 1;
            double sumwfrac = 0;
            for (int j = 0; j < labels.length; j++)
            {
                if (MathUtils.eq(labels[j], label))
                {
                    double w = sourceWeights.get(j);
                    double f = sourceFracs.get(j);
                    sumwfrac += w * f;
                }
            }
            double frac = sumwfrac;
            cfracs[i] = frac;
        }

        // copy the results out
        int[] perm = MathUtils.permutation(cfracs);
        int cnum = Math.min(maxcomps, perm.length);
        for (int i = 0; i < cnum; i++)
        {
            int pidx = perm[perm.length - 1 - i];
            int label = pidx + 1;

            if (MathUtils.count(labels, label) == 0)
            {
                out.setFrac(i, 0);
                out.setLine(i, new Vect(3));
            }
            else
            {
                out.setFrac(i, cfracs[pidx]);
                out.setLine(i, cluster.getCenters().get(pidx));
            }
        }

        return out.getEncoding();
    }

    private static VectsClusterAKM cluster(int k, int restarts, Vects initial, Vect weights, Vects vects)
    {
        VectsClusterAKM cluster = new VectsClusterAKM();
        cluster.withRestarts(restarts);
        cluster.withVects(vects);
        cluster.withWeights(weights);
        cluster.withK(Math.min(k, vects.size()));

        // setup the initial guess
        if (initial != null)
        {
            cluster.withInitial(initial);
        }

        // run the optimization
        cluster.init();

        // check for no clusters
        if (cluster.getK() > 0)
        {
            cluster.run();
        }

        return cluster;
    }

    @Override
    public Vect run(List<Double> weights, List<Vect> input)
    {
        return run(null, weights, input);
    }

    public Vect run(Vect initial, List<Double> weights, List<Vect> input)
    {
        if (EstimationType.Match.equals(this.estimation))
        {
            return this.match(initial, weights, input);
        }
        else if (EstimationType.Rank.equals(this.estimation))
        {
            return this.rank(weights, input);
        }
        else
        {
            throw new RuntimeException("unrecognized estimation: " + this.estimation);
        }
    }
}
