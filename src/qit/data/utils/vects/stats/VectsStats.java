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

package qit.data.utils.vects.stats;

import qit.base.Logging;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;

public class VectsStats
{
    public Vect min;
    public Vect max;
    public Vect mean;
    public Vect var;
    public Vect sum;
    public Vect std;
    public Vect stde;
    public Vect cv;
    public Vect num;
    public Vect median;
    public Vect qlow;
    public Vect qhigh;
    public Vect iqr;
    public Boolean output;

    public Vects input;
    public boolean robust = false;
    public Vect weights;

    public VectsStats withInput(Vects v)
    {
        this.input = v;
        this.output = null;

        return this;
    }

    public VectsStats withWeights(Vect v)
    {
        this.weights = v;
        this.output = null;

        return this;
    }

    public VectsStats withRobust(boolean v)
    {
        this.robust = v;
        this.output = null;

        return this;
    }

    public VectsStats run()
    {
        int num = this.input.size();
        int dim = this.input.getDim();

        if (num == 0)
        {
            // report that no stats were computed
            this.output = false;
            return this;
        }

        this.qlow = VectSource.createND(dim);
        this.median = VectSource.createND(dim);
        this.qhigh = VectSource.createND(dim);
        this.iqr = VectSource.createND(dim);
        this.num = VectSource.createND(dim);
        this.min = VectSource.createND(dim);
        this.max = VectSource.createND(dim);
        this.sum = VectSource.createND(dim);
        this.mean = VectSource.createND(dim);
        this.var = VectSource.createND(dim);
        this.std = VectSource.createND(dim);
        this.stde = VectSource.createND(dim);
        this.cv = VectSource.createND(dim);

        for (int i = 0; i < dim; i++)
        {
            Vect slice = VectSource.createND(num);

            for (int j = 0; j < num; j++)
            {
                slice.set(j, this.input.get(j).get(i));
            }
            
            VectStats vs = new VectStats().withInput(slice).withWeights(this.weights).withRobust(this.robust).run();

            this.qlow.set(i, vs.qlow);
            this.median.set(i, vs.median);
            this.qhigh.set(i, vs.qhigh);
            this.iqr.set(i, vs.iqr);
            this.num.set(i, vs.num);
            this.min.set(i, vs.min);
            this.max.set(i, vs.max);
            this.sum.set(i, vs.sum);
            this.mean.set(i, vs.mean);
            this.var.set(i, vs.var);
            this.std.set(i, vs.std);
            this.stde.set(i, vs.stde);
            this.cv.set(i, vs.cv);
        }
        
        this.output = true;

        return this;
    }

    public boolean getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
