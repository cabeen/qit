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

import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectUtils;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.math.structs.CovarianceType;
import qit.math.structs.Gaussian;
import qit.math.structs.GaussianMixture;
import qit.math.utils.MathUtils;

import java.io.IOException;

/** Gaussian mixture model clustering by expectation maximization */
public class VectsClusterGM extends VectsClusterEM
{
    public GaussianMixture model;
    public Matrix membership;
    public CovarianceType type = CovarianceType.full;
    public double add = 0;

    public VectsClusterGM withModel(GaussianMixture m)
    {
        this.model = m;
        this.k = m.getNum();
        return this;
    }

    public GaussianMixture getModel()
    {
        return this.model;
    }

    public VectsClusterGM withType(CovarianceType t)
    {
        this.type = t;
        this.labels = null;
        return this;
    }

    public VectsClusterGM withAdd(Double v)
    {
        this.add = v;
        this.labels = null;
        return this;
    }

    public VectsClusterGM proto()
    {
        return new VectsClusterGM().with(this);
    }

    public VectsClusterGM with(VectsCluster set)
    {
        VectsClusterGM cast = (VectsClusterGM) set;
        super.with(set);
        this.model = cast.model;
        this.membership = cast.membership;
        this.type = cast.type;

        return this;
    }

    public double cost()
    {
        return this.model.nll(this.vects);
    }

    public VectsClusterGM allocate()
    {
        int num = this.vects.size();
        this.labels = new int[num];
        this.membership = new Matrix(num, this.k);
        
        return this;
    }
    
    public VectsClusterGM init(int[] labels)
    {
        this.allocate();
        
        int num = this.vects.size();
        for (int i = 0; i < labels.length; i++)
        {
            this.labels[i] = labels[i];
        }

        for (int i = 0; i < num; i++)
        {
            this.membership.set(i, labels[i] - 1, 1.0);
        }

        this.maximization();
        
        return this;
    }

    public VectsClusterGM init()
    {
        int[] labels = new VectsClusterKM().withInitial(this.initial).withWeights(this.weights).withVects(this.vects).withK(this.k).run().labels;
        this.init(labels);

        return this;
    }

    public VectsClusterGM expectation()
    {
        this.model.membership(this.vects, this.membership);

        for (int i = 0; i < this.vects.size(); i++)
        {
            this.labels[i] = VectUtils.maxidx(this.membership.getRow(i)) + 1;
        }

        return this;
    }

    public VectsClusterGM maximization()
    {
        int k = this.membership.cols();

        double[] weights = MatrixUtils.colsum(this.membership).normalizeProb().toArray();
        Gaussian[] comps = new Gaussian[k];
        for (int i = 0; i < k; i++)
        {
            comps[i] = new VectsGaussianFitter().withWeights(this.membership.getColumn(i).normalizeProb()).withType(this.type).withInput(this.vects).withAdd(this.add).getOutput();
        }

        this.model = new GaussianMixture(weights, comps);
        
        return this;
    }

    public VectsClusterEM read(String fn) throws IOException
    {
        this.model = GaussianMixture.read(fn);
        return this;
    }

    public VectsClusterEM write(String fn) throws IOException
    {
        this.model.write(fn);
        return this;
    }
}
