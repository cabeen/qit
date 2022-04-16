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
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.math.utils.MathUtils;

import java.io.IOException;

public class WatsonMixture extends JsonDataset
{
    private int num;
    private double[] weights;
    private Watson[] comps;

    @SuppressWarnings("unused")
    private WatsonMixture()
    {
    }

    public WatsonMixture(double[] weights, Watson[] comps)
    {
        Global.assume(weights.length == comps.length, "invalid mixture");
        this.num = weights.length;

        Global.assume(MathUtils.unit(MathUtils.sum(weights)), "invalid mixture weights");

        this.weights = MathUtils.copy(weights);
        this.comps = new Watson[this.num];
        for (int i = 0; i < this.num; i++)
        {
            this.comps[i] = comps[i];
        }
    }

    public int getNum()
    {
        return this.num;
    }

    public double getWeight(int i)
    {
        return this.weights[i];
    }

    public Watson getComp(int i)
    {
        return this.comps[i].copy();
    }

    public void resp(Vects points, double[][] resp)
    {
        int k = this.num;

        for (int i = 0; i < points.size(); i++)
        {
            Vect point = points.get(i);
            double sum = 0;
            for (int j = 0; j < k; j++)
            {
                double den = this.density(j, point);
                resp[j][i] = den;
                sum += den;
            }

            for (int j = 0; j < this.num; j++)
            {
                resp[j][i] /= sum;
            }
        }
    }

    public double density(int i, Vect input)
    {
        return this.weights[i] * this.comps[i].density(input);
    }

    public double density(Vect input)
    {
        double out = 0;
        for (int i = 0; i < this.num; i++)
        {
            out += this.density(i, input);
        }

        return out;
    }

    public int label(Vect point)
    {
        double max = Double.MIN_VALUE;
        int label = -1;

        for (int idx = 0; idx < this.num; idx++)
        {
            double like = this.weights[idx] * this.comps[idx].density(point);
            if (like > max)
            {
                max = like;
                label = idx;
            }
        }

        return label;
    }

    public double nll(Vects points)
    {
        double nll = 0;
        for (Vect v : points)
        {
            nll -= Math.log(this.density(v));
        }

        return nll;
    }

    public double bic(Vects points)
    {
        double nll = this.nll(points);

        double numParam = 4 * this.num;
        double numPoints = points.size();
        double bic = numParam * Math.log(numPoints) + 2 * nll;

        return bic;
    }

    public static WatsonMixture read(String fn) throws IOException
    {
        return JsonDataset.read(WatsonMixture.class, fn);
    }

    public WatsonMixture copy()
    {
        return new WatsonMixture(this.weights, this.comps);
    }
}