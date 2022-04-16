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

package qit.data.utils.vects.cluster;

import qit.base.Global;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

import java.io.IOException;

/** Bernoulli mixture model clustering by expectation maximization */
public class VectsClusterBM extends VectsClusterEM
{
    protected Vect mixing;
    protected Vects centers;
    protected double[][] resp;

    public VectsClusterBM proto()
    {
        return new VectsClusterBM().with(this);
    }

    public VectsClusterBM with(VectsCluster set)
    {
        VectsClusterBM cast = (VectsClusterBM) set;
        super.with(set);
        this.mixing = cast.mixing;
        this.centers = cast.centers;
        this.resp = cast.resp;

        return this;
    }

    public double cost()
    {
        double nll = 0;
        for (Vect v : this.vects)
        {
            nll -= Math.log(this.density(v));
        }

        return nll;
    }

    public VectsClusterBM allocate()
    {
        Global.assume(this.vects != null && this.k > 0, "invalid cluster parameters");
        assert(this.vects != null); // make static analyzer happy...
        Global.assume(this.vects.size() != 0, "invalid input vectors");

        int num = this.vects.size();
        this.mixing = VectSource.createND(num, 1.0 / num);
        this.resp = new double[num][this.k];
        
        this.labels = new int[this.vects.size()];
        this.iters = 0;
        this.centers = new Vects();

        int dim = this.vects.get(0).size();
        for (int i = 0; i < this.k; i++)
        {
            this.centers.add(VectSource.createND(dim));
        }

        return this;
    }

    public VectsClusterBM init(int[] labels)
    {
        this.allocate();

        for (int i = 0; i < labels.length; i++)
        {
            this.labels[i] = labels[i];
        }

        for (int i = 0; i < this.vects.size(); i++)
        {
            this.resp[i][labels[i] - 1] = 1.0;
        }

        return this;
    }

    public VectsClusterBM init()
    {
        int[] labels = new VectsClusterKM().withInitial(this.initial).withWeights(this.weights).withVects(this.vects)
                .withK(this.k).run().labels;
        this.init(labels);

        return this;
    }

    public VectsClusterBM expectation()
    {
        for (int i = 0; i < this.vects.size(); i++)
        {
            for (int j = 0; j < this.k; j++)
            {
                this.resp[i][j] = this.mixing.get(j) * this.density(this.vects.get(i), j);
            }
            double sum = MathUtils.sum(this.resp[i]);

            double factor = 1.0 / sum;
            if (!Double.isNaN(factor) && !Double.isInfinite(factor))
            {
                MathUtils.timesEquals(this.resp[i], factor);
            }

            this.labels[i] = MathUtils.maxidx(this.resp[i]) + 1;
        }

        return this;
    }

    public VectsClusterBM maximization()
    {
        int num = this.vects.size();
        int dim = this.vects.get(0).size();
        double sumq = 0;
        for (int j = 0; j < this.k; j++)
        {
            Vect mu = VectSource.createND(dim);
            double alpha = 0;
            for (int i = 0; i < num; i++)
            {
                alpha += this.resp[i][j];
                mu.plusEquals(this.resp[i][j], this.vects.get(i));
            }
            sumq += alpha;

            if (!MathUtils.zero(alpha))
            {
                double norm = 1 / alpha;
                mu.timesEquals(norm);
            }

            this.mixing.set(j, alpha);
            this.centers.set(j, mu);
        }

        if (!MathUtils.zero(sumq))
        {
            this.mixing.timesEquals(1.0 / sumq);
        }

        return this;
    }

    public double density(Vect input)
    {
        double out = 0;
        for (int i = 0; i < this.k; i++)
        {
            out += this.density(input, i);
        }

        return out;
    }

    public double density(Vect point, int comp)
    {
        Vect bern = this.centers.get(comp);
        double out = 1.0;
        for (int i = 0; i < point.size(); i++)
        {
            double v = point.get(i);
            double b = bern.get(i);
            double f = v < 0.5 ? 1 - b : b;
            out *= f;
        }

        return out;
    }

    public VectsClusterBM read(String fn) throws IOException
    {
        Table table = Table.read(fn);

        this.k = table.getNumRecords();
        this.mixing = VectSource.createND(this.k);
        this.centers = new Vects(this.k);
        int dim = table.getNumFields() - 2;

        for (int i = 0; i < this.k; i++)
        {
            int label = Integer.valueOf(table.get(i, "label").toString());
            double mix = Double.valueOf(table.get(i, "mix").toString());
            Vect center = VectSource.createND(dim);
            for (int j = 0; j < dim; j++)
            {
                center.set(j, Double.valueOf(table.get(i, "p" + j).toString()));
            }

            int kidx = label - 1;
            this.mixing.set(kidx, mix);
            this.centers.set(kidx, center);
        }

        return this;
    }

    public VectsClusterBM write(String fn) throws IOException
    {
        int dim = this.centers.get(0).size();

        Table table = new Table();
        table.withField("label");
        table.withField("mix");
        for (int j = 0; j < dim; j++)
        {
            table.withField("p" + j);
        }

        for (int i = 0; i < this.k; i++)
        {
            Double mix = this.mixing.get(i);
            Vect center = this.centers.get(i);
            String[] row = new String[dim];
            row[0] = String.valueOf(i + 1);
            row[1] = String.valueOf(mix);
            for (int j = 0; j < dim; j++)
            {
                row[2 + j] = String.valueOf(center.get(j));
            }
            table.addRecord(i, row);
        }

        table.write(fn);

        return this;
    }
}
