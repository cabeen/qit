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
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.math.utils.MathUtils;

import java.io.IOException;

/** an abstract EM clustering algorithm */
public abstract class VectsClusterEM extends VectsCluster
{
    protected boolean messages = false;
    protected Integer k;
    protected Integer maxiters = 300;
    protected Integer restarts = 1;
    protected Double thresh = 1e-3;

    protected Vect weights;
    protected Vects vects;
    protected Vects initial;
    protected int[] labels;

    public int iters;

    public abstract VectsClusterEM proto();
    
    public int getK()
    {
        return this.k;
    }
    
    public int[] getLabels()
    {
        return this.labels;
    }
    
    public VectsClusterEM withMessages(boolean v)
    {
        this.messages = v;
        return this;
    }

    public VectsClusterEM withK(int k)
    {
        this.k = k;
        this.labels = null;
        return this;
    }

    public VectsClusterEM withRestarts(int n)
    {
        this.restarts = n;
        this.labels = null;
        return this;
    }

    public VectsClusterEM withThresh(double t)
    {
        this.thresh = t;
        this.labels = null;
        return this;
    }

    public VectsClusterEM withMaxIter(int n)
    {
        this.maxiters = n;
        this.labels = null;
        return this;
    }

    public VectsClusterEM withWeights(Vect ws)
    {
        this.weights = ws;
        this.labels = null;
        return this;
    }

    public VectsClusterEM withVects(Vects vs)
    {
        this.vects = vs;
        return this;
    }

    public VectsClusterEM withInitial(Vects vs)
    {
        this.initial = vs;
        this.labels = null;
        return this;
    }

    public VectsClusterEM with(VectsCluster set)
    {
        VectsClusterEM cast = (VectsClusterEM) set;
        this.k = cast.k;
        this.maxiters = cast.maxiters;
        this.iters = cast.iters;
        this.vects = cast.vects;
        this.weights = cast.weights;
        this.initial = cast.initial;
        this.labels = cast.labels;
        this.messages = cast.messages;
        this.restarts = cast.restarts;
        this.thresh = cast.thresh;

        return this;
    }

    public int iters()
    {
        return this.iters;
    }

    public VectsClusterEM run()
    {
        if (this.messages)
        {
            Logging.progress("initializing");
        }

        VectsClusterEM best = null;
        Double cost = null;

        for (int i = 0; i < this.restarts; i++)
        {
            if (this.messages)
            {
                Logging.progress("started batch " + (i+1));
            }
            
            VectsClusterEM r = this.proto();
            r.runSingle();
            double e = r.cost();

            if (cost == null || e < cost)
            {
                cost = e;
                best = r;
            }
        }

        this.with(best);
        
        return this;
    }

    public VectsClusterEM runSingle()
    {
        this.init();

        Double pcost = null;
        Double cost = Double.MAX_VALUE;

        if (this.messages)
        {
            Logging.progress("starting expectation maximization");
        }
        
        do
        {
            if (this.messages)
            {
                Logging.progress("... started iteration " + this.iters + ", cost = " + cost);
            }
            
            pcost = cost;
            this.expectation();
            this.maximization();
            cost = this.cost();
        }
        while (!MathUtils.eq(cost, pcost, this.thresh) && this.iters++ < this.maxiters);

        if (this.messages)
        {
            Logging.progress("finished expectation maximization");
            Logging.progress("... iterations = " + this.iters);
            Logging.progress("... cost = " + cost);
        }
        
        return this;
    }

    public int[] getOutput()
    {
        if (this.labels == null)
        {
            this.run();
        }
        
        return this.labels;
    }

    public abstract VectsClusterEM allocate();

    public abstract VectsClusterEM init();

    public abstract VectsClusterEM init(int[] labels);

    public abstract VectsClusterEM expectation();

    public abstract VectsClusterEM maximization();

    public abstract VectsClusterEM read(String fn) throws IOException;

    public abstract VectsClusterEM write(String fn) throws IOException;
}
