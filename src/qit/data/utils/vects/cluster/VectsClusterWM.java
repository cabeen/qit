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

import qit.data.datasets.Vect;
import qit.data.utils.vects.stats.VectsWatsonFitter;
import qit.math.structs.Watson;
import qit.math.structs.WatsonMixture;
import qit.math.utils.MathUtils;

import java.io.IOException;

/** Watson mixture model clustering by expectation maximization */
public class VectsClusterWM extends VectsClusterEM
{
    protected WatsonMixture model;
    protected double[][] resp;

    public VectsClusterWM withModel(WatsonMixture m)
    {
        this.model = m;
        this.k = m.getNum();

        return this;
    }

    public VectsClusterWM proto()
    {
        return new VectsClusterWM().with(this);
    }

    public VectsClusterWM with(VectsCluster set)
    {
        VectsClusterWM cast = (VectsClusterWM) set;
        super.with(set);
        this.model = cast.model;
        this.resp = cast.resp;

        return this;
    }

    public double cost()
    {
        return this.model.nll(this.vects);
    }

    public VectsClusterWM allocate()
    {
        int num = this.vects.size();
        this.resp = new double[num][this.k];

        return this;
    }

    public VectsClusterWM init(int[] labels)
    {
        this.allocate();

        for (int i = 0; i < labels.length; i++)
        {
            this.labels[i] = labels[i];
        }

        for (int i = 0; i < this.vects.size(); i++)
        {
            this.resp[i][labels[i]] = 1.0;
        }

        return this;
    }

    public VectsClusterWM init()
    {
        int[] labels = new VectsClusterKM().withInitial(this.initial).withWeights(this.weights).withVects(this.vects)
                .withK(this.k).run().labels;
        this.init(labels);

        return this;
    }

    public VectsClusterWM expectation()
    {
        this.model.resp(this.vects, this.resp);

        for (int i = 0; i < this.vects.size(); i++)
        {
            this.labels[i] = MathUtils.maxidx(this.resp[i]) + 1;
        }

        return this;
    }

    public VectsClusterWM maximization()
    {
        int k = this.resp.length;

        double[] rsums = new double[k];
        for (int i = 0; i < k; i++)
        {
            rsums[i] = MathUtils.sum(this.resp[i]);
        }

        double rsum = MathUtils.sum(rsums);

        double[] weights = new double[k];
        Watson[] comps = new Watson[k];
        for (int i = 0; i < k; i++)
        {
            weights[i] = rsums[i] / rsum;
            comps[i] = new VectsWatsonFitter().withWeights(new Vect(this.resp[i])).withInput(this.vects).getOutput();
        }

        this.model = new WatsonMixture(weights, comps);

        return this;
    }

    public VectsClusterEM read(String fn) throws IOException
    {
        throw new IOException("reading not implemented");
    }

    public VectsClusterEM write(String fn) throws IOException
    {
        throw new IOException("writing not implemented");
    }
}
