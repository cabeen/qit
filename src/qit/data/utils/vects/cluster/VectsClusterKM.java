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

/** Vanilla k-means clustering */
public class VectsClusterKM extends VectsClusterEM
{
    protected Vects mixing;
    protected Vects centers;

    public VectsClusterKM()
    {
    }

    public VectsClusterKM proto()
    {
        return new VectsClusterKM().with(this);
    }

    public Vects getCenters()
    {
        return this.centers;
    }

    public VectsClusterKM with(VectsCluster set)
    {
        super.with(set);
        VectsClusterKM cast = (VectsClusterKM) set;
        this.centers = cast.centers;
        this.mixing = cast.mixing;

        return this;
    }

    public double cost()
    {
        double error = 0;
        for (int i = 0; i < this.vects.size(); i++)
        {
            error += this.dist(this.vects.get(i), this.centers.get(this.labels[i] - 1));
        }
        return error;
    }

    public VectsClusterKM allocate()
    {
        assert (this.vects != null); // compensate for an aggressive static analyzer
        Global.assume(this.vects != null, "invalid input vectors");
        Global.assume(this.k > 0, "invalid number of clusters");
        Global.assume(this.k <= this.vects.size(), "too many clusters for dataset");
        Global.assume(this.vects.size() != 0, "invalid input vectors");

        this.labels = new int[this.vects.size()];
        this.iters = 0;
        this.mixing = new Vects();
        this.centers = new Vects();

        int dim = this.vects.get(0).size();
        for (int i = 0; i < this.k; i++)
        {
            this.mixing.add(VectSource.create1D());
            this.centers.add(VectSource.createND(dim));
        }

        return this;
    }

    public VectsClusterKM init(int[] labels)
    {
        this.init();
        for (int i = 0; i < labels.length; i++)
        {
            this.labels[i] = labels[i];
        }

        return this;
    }

    public VectsClusterKM init()
    {
        this.allocate();

        int start = 0;
        if (this.initial != null)
        {
            start = Math.min(this.centers.size(), this.initial.size());
            for (int i = 0; i < this.centers.size(); i++)
            {
                this.centers.set(i, this.initial.get(i).copy());
            }
        }
        
        int[] sub = MathUtils.subset(this.vects.size(), this.k);
        for (int i = start; i < this.k; i++)
        {
            this.centers.set(i, this.vects.get(sub[i]).copy());
        }

        return this;
    }

    public double dist(Vect a, Vect b)
    {
        return a.dist2(b);
    }

    public VectsClusterKM expectation()
    {
        for (int i = 0; i < this.vects.size(); i++)
        {
            double[] dists = new double[this.centers.size()];
            for (int j = 0; j < this.centers.size(); j++)
            {
                dists[j] = this.dist(this.centers.get(j), this.vects.get(i));
            }
            int kidx = MathUtils.minidx(dists);

            this.labels[i] = kidx + 1;
        }

        return this;
    }

    public VectsClusterKM maximization()
    {
        this.centers.setAll(0.0);
        this.mixing.setAll(0.0);

        for (int i = 0; i < this.vects.size(); i++)
        {
            int kidx = this.labels[i] - 1;
            double weight = this.weights == null ? 1.0 : this.weights.get(i);
            this.centers.get(kidx).plusEquals(weight, this.vects.get(i));
            this.mixing.get(kidx).plusEquals(weight);
        }

        for (int i = 0; i < this.k; i++)
        {
            double mix = this.mixing.get(i).get(0);
            double norm = MathUtils.zero(mix) ? 1.0 : 1.0 / mix;
            this.centers.get(i).timesEquals(norm);
            this.mixing.get(i).timesEquals(norm);
        }

        return this;
    }

    public VectsClusterKM read(String fn) throws IOException
    {
        Table table = Table.read(fn);

        this.k = table.getNumRecords();
        this.mixing = new Vects(this.k);
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
            this.mixing.set(kidx, VectSource.create1D(mix));
            this.centers.set(kidx, center);
        }

        return this;
    }

    public VectsClusterKM write(String fn) throws IOException
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
            Double mix = this.mixing.get(i).get(0);
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
