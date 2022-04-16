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

package qit.math.structs;

import qit.base.Global;
import qit.base.JsonDataset;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

import java.io.IOException;

public class GaussianWatsonMixture extends JsonDataset
{
    public static final boolean FAST = false;

    private int num;
    private double[] weights;
    private Gaussian[] pos;
    private Watson[] dir;

    public GaussianWatsonMixture()
    {
    }

    public GaussianWatsonMixture(double[] weights, Gaussian[] pos, Watson[] dir)
    {
        Global.assume(weights.length == dir.length, "invalid mixture");
        Global.assume(weights.length == dir.length, "invalid mixture");
        
        this.num = weights.length;

        Global.assume(MathUtils.unit(MathUtils.sum(weights)), "invalid mixture weights");
        
        this.weights = MathUtils.copy(weights);
        this.pos = new Gaussian[this.num];
        this.dir = new Watson[this.num];
        for (int i = 0; i < this.num; i++)
        {
            this.pos[i] = pos[i];
            this.dir[i] = dir[i];
        }
    }
    
    public int getNum()
    {
        return this.num;
    }
    
    public Gaussian getPos(int i)
    {
        return this.pos[i].copy();
    }
    
    public Watson getDir(int i)
    {
        return this.dir[i].copy();
    }

    public double density(int i, Vect pos, Vect dir)
    {
        double w = this.weights[i];
        double p = this.pos[i].density(pos);
        double d = 1.0;

        if (!FAST)
        {
            d = this.dir[i].density(dir);
        }
        else
        {
            // this is okay in some cases
            double mutdir = this.dir[i].getMu().dot(dir);
            d = Math.exp(this.dir[i].getKappa() * mutdir * mutdir);
        }

        return w * p * d;
    }

    public double density(Vect pos, Vect dir)
    {
        double out = 0;
        for (int i = 0; i < this.num; i++)
        {
            out += this.density(i, pos, dir);
        }

        return out;
    }

    public void resp(Vect pos, Vect dir, Vect resp)
    {
        double sum = 0;
        for (int i = 0; i < this.num; i++)
        {
            double den = this.density(i, pos, dir);
            resp.set(i, den);
            sum += den;
        }

        if (sum == 0)
        {
            throw new RuntimeException("invalid responsibility");
        }

        for (int i = 0; i < this.num; i++)
        {
            double r = resp.get(i);
            double nr = r / sum;
            resp.set(i, nr);
        }
    }

    public void resp(Vects pos, Vects dir, Matrix resp)
    {
        for (int j = 0; j < pos.size(); j++)
        {
            Vect p = pos.get(j);
            Vect d = dir.get(j);

            double sum = 0;
            for (int i = 0; i < this.num; i++)
            {
                double den = this.density(i, p, d);
                resp.set(i, j, den);
                sum += den;
            }

            if (sum != 0)
            {
                for (int i = 0; i < this.num; i++)
                {
                    double r = resp.get(i, j);
                    double nr = r / sum;
                    resp.set(i, j, nr);
                }
            } else
            {
                for (int i = 0; i < this.num; i++)
                {
                    double nr = 1.0 / this.num;
                    resp.set(i, j, nr);
                }
            }
        }
    }

    public Matrix resp(Vects pos, Vects dir)
    {
        Matrix out = new Matrix(this.num, pos.size());
        this.resp(pos, dir, out);
        return out;
    }

    public void density(Vects pos, Vects dir, Vect out)
    {
        for (int i = 0; i < pos.size(); i++)
        {
            out.set(i, this.density(pos.get(i), dir.get(i)));
        }
    }

    public Vect density(Vects pos, Vects dir)
    {
        Vect out = VectSource.createND(pos.size());
        this.density(pos, dir, out);
        return out;
    }

    public double nll(Vects pos, Vects dir)
    {
        double nll = 0;
        for (int i = 0; i < pos.size(); i++)
        {
            nll -= Math.log(this.density(pos.get(i), dir.get(i)));
        }

        return nll;
    }

    public double bic(Vects pos, Vects dir)
    {
        double nll = this.nll(pos, dir);

        double numParam = 4 * this.num;
        double numPoints = pos.size();

        double bic = numParam * Math.log(numPoints) + 2 * nll;

        return bic;
    }

    public static GaussianWatsonMixture read(String fn) throws IOException
    {
        return JsonDataset.read(GaussianWatsonMixture.class, fn);
    }

    public GaussianWatsonMixture copy()
    {
        return new GaussianWatsonMixture(this.weights, this.pos, this.dir);
    }
}