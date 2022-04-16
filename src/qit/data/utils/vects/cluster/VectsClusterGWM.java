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

import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.MatrixUtils;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.data.utils.vects.stats.VectsWatsonFitter;
import qit.math.structs.CovarianceType;
import qit.math.structs.Gaussian;
import qit.math.structs.GaussianWatsonMixture;
import qit.math.structs.Watson;

import java.io.IOException;

/** Gaussian-Watson mixture model clustering by expectation maximization */
public class VectsClusterGWM extends VectsClusterEM
{
    protected GaussianWatsonMixture model;
    protected Matrix resp;
    protected Vects pos;
    protected Vects dir;
    protected Double kappa;
    protected Double regGaussian;
    protected Double regWatson;
    protected CovarianceType type;

    public VectsClusterGWM withModel(GaussianWatsonMixture m)
    {
        this.model = m;
        this.k = m.getNum();

        return this;
    }

    public GaussianWatsonMixture getModel()
    {
        return this.model;
    }

    public Vects getPos()
    {
        return this.pos;
    }

    public Vects getDir()
    {
        return this.dir;
    }
    
    public Matrix getResp()
    {
        return this.resp;
    }

    public VectsClusterGWM proto()
    {
        return new VectsClusterGWM().with(this);
    }

    public VectsClusterGWM with(VectsCluster set)
    {
        VectsClusterGWM cast = (VectsClusterGWM) set;
        super.with(set);
        this.model = cast.model;
        this.resp = cast.resp;
        this.pos = cast.pos;
        this.dir = cast.dir;
        this.kappa = cast.kappa;
        this.type = cast.type;

        return this;
    }

    public VectsClusterGWM withType(String t)
    {
        this.type = CovarianceType.valueOf(t);

        this.labels = null;
        return this;
    }

    public VectsClusterGWM withKappa(double k)
    {
        this.kappa = k;

        this.labels = null;
        return this;
    }

    public VectsClusterGWM withGaussianReg(double v)
    {
        this.regGaussian = v;

        this.labels = null;
        return this;
    }

    public VectsClusterGWM withWatsonReg(double v)
    {
        this.regWatson = v;

        this.labels = null;
        return this;
    }

    public double cost()
    {
        return this.model.nll(this.pos, this.dir);
    }

    public VectsClusterGWM allocate()
    {
        int num = this.vects.size();
        this.labels = new int[this.vects.size()];
        this.resp = new Matrix(this.k, num);

        this.pos = new Vects();
        this.dir = new Vects();
        for (Vect v : this.vects)
        {
            Vect pos = v.sub(0, 3);
            Vect dir = v.sub(3, 6);

            this.pos.add(pos);
            this.dir.add(dir);
        }

        return this;
    }

    public VectsClusterGWM init(int[] labels)
    {
        this.allocate();

        int num = this.vects.size();
        for (int i = 0; i < labels.length; i++)
        {
            this.labels[i] = labels[i];
        }

        for (int i = 0; i < num; i++)
        {
            int idx = labels[i] - 1;
            if (idx >= 0)
            {
                this.resp.set(idx, i, 1.0);
            }
            else
            {
                // give points with no label uniform responsibility
                for (int j = 0; j < this.k; j++)
                {
                    this.resp.set(j, i, 1.0 / this.k);
                }
            }
        }

        Logging.progress("fitting initial guess");
        this.maximization();

        return this;
    }

    public VectsClusterGWM init()
    {
        Logging.progress("computing initial guess");
        VectsClusterSAKM cluster = (VectsClusterSAKM) new VectsClusterSAKM().withInitial(this.initial)
                .withWeights(this.weights).withVects(this.vects).withK(this.k).withMaxIter(this.maxiters).run();
        this.init(cluster.labels);

        return this;
    }

    public VectsClusterGWM expectation()
    {
        this.model.resp(this.pos, this.dir, this.resp);

        for (int i = 0; i < this.vects.size(); i++)
        {
            Double maxval = null;
            Integer maxidx = null;
            for (int idx = 0; idx < this.k; idx++)
            {
                double val = this.resp.get(idx, i);
                if (maxval == null || val > maxval)
                {
                    maxval = val;
                    maxidx = idx;
                }
            }
            this.labels[i] = maxidx + 1;
        }

        return this;
    }

    public VectsClusterGWM maximization()
    {
        int k = this.resp.rows();
        Vect rsums = MatrixUtils.rowsum(this.resp);
        double rsum = rsums.sum();

        double[] weights = new double[k];
        Gaussian[] mpos = new Gaussian[k];
        Watson[] mdir = new Watson[k];
        for (int i = 0; i < k; i++)
        {
            Logging.info("fitting component " + i);
            weights[i] = rsums.get(i) / rsum;
            double norm = 1.0 / rsums.get(i);
            Vect r = this.resp.getRow(i).times(norm);

            mpos[i] = new VectsGaussianFitter().withWeights(r).withInput(this.pos).withType(this.type).withAdd(this.regGaussian).getOutput();
            mdir[i] = new VectsWatsonFitter().withWeights(r).withInput(this.dir).withReg(this.regWatson).withFixed(this.kappa).getOutput();
        }

        this.model = new GaussianWatsonMixture(weights, mpos, mdir);
        return this;
    }

    public VectsClusterEM read(String fn) throws IOException
    {
        throw new RuntimeException("reading not implemented");
    }

    public VectsClusterEM write(String fn) throws IOException
    {
        throw new RuntimeException("writing not implemented");
    }
}
