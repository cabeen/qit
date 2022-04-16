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

import qit.base.Global;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

/** Markov random field inference of discrete labels with loopy belief propagation, after pff*/
public class VectsMRF
{
    private int num;
    private int values;
    private double dataWeight;
    private double smoothWeight;
    private Vects dataCost;
    private Matrix smoothCost;
    private Matrix[] messages;
    private Matrix[] prevMessages;

    public static Vects createCostNLL(double[] values)
    {
        for (double dist : values)
        {
            Global.assume(dist >= 0, "invalid values");
        }
        
        Vects out = new Vects(values.length);
        for (double value : values)
        {
            double cost = -1.0 * Math.log(value);
            double invcost = 1 - cost;
            Vect vect = VectSource.create2D(cost, invcost);
            out.add(vect);
        }
        
        return out;
    }
    
    public static Vects createCost(double[] values)
    {
        for (double dist : values)
        {
            if (dist < 0)
            {
                throw new RuntimeException("invalid values");
            }
        }
        
        Vects out = new Vects(values.length);
        for (double value : values)
        {
            double cost = value;
            double invcost = 1 - cost;
            Vect vect = VectSource.create2D(cost, invcost);
            out.add(vect);
        }
        
        return out;
    }
    
    public static Vects createCostExponential(double[] values, double thresh)
    {
        Global.assume(thresh > 0, "invalid threshold");
        
        for (double dist : values)
        {
            Global.assume(dist >= 0, "invalid values");
        }
        
        double beta = Math.log(2.0) / thresh;
        
        Vects out = new Vects(values.length);
        for (double value : values)
        {
            double cost = Math.exp(-beta * value);
            double invcost = 1 - cost;
            Vect vect = VectSource.create2D(cost, invcost);
            out.add(vect);
        }
        
        return out;
    }
    
    public static Vects createCostSigmoid(double[] values, double thresh)
    {
        Global.assume(thresh > 0, "invalid threshold");
        
        double beta = Math.log(2.0) / thresh;
        
        Vects out = new Vects(values.length);
        for (double value : values)
        {
            double exp = Math.exp(-beta * value);
            double num = 1 - exp;
            double denom = 1 + exp;
            double cost = denom == 0 ? 1 : num / denom;
            double invcost = 1 - cost;
            Vect vect = VectSource.create2D(cost, invcost);
            out.add(vect);
        }
        
        return out;
    }
    
    public VectsMRF(Double dataWeight, Double smoothWeight, Vects dataCost, Matrix smoothCost)
    {
        this.num = dataCost.size();
        this.values = dataCost.getDim();
        this.dataWeight = dataWeight == null ? 1.0 : dataWeight;
        this.smoothWeight = smoothWeight == null ? 1.0 : smoothWeight;
        this.dataCost = dataCost;
        this.smoothCost = smoothCost;
        this.messages = new Matrix[this.values];
        this.prevMessages = new Matrix[this.values];

        for (int i = 0; i < this.values; i++)
        {
            this.messages[i] = smoothCost.copy();
            this.prevMessages[i] = smoothCost.copy();
        }
    }

    public void run(int iter)
    {
        for (int t = 0; t < iter; t++)
        {
            for (int i = 0; i < this.num; i++)
            {
                for (int j = 0; j < this.num; j++)
                {
                    if (!MathUtils.zero(this.smoothCost.get(i, j)))
                    {
                        for (int qv = 0; qv < this.values; qv++)
                        {
                            double sum = 0;
                            double min = Double.MAX_VALUE;
                            for (int pv = 0; pv < this.values; pv++)
                            {
                                double data = this.dataCost.get(i).get(pv);
                                double smooth = pv == qv ? 0 : this.smoothCost.get(i, j);

                                double psum = this.dataWeight * data;
                                psum += this.smoothWeight * smooth;

                                for (int k = 0; k < this.num; k++)
                                {
                                    if (k != j && !MathUtils.zero(this.smoothCost.get(i, k)))
                                    {
                                        psum += this.prevMessages[qv].get(k, i);
                                    }
                                }

                                min = Math.min(min, psum);
                                sum += psum;
                            }

                            min /= sum;
                            this.messages[qv].set(i, j, min);
                        }
                    }
                }
            }

            Matrix[] tmp = this.prevMessages;
            this.prevMessages = this.messages;
            this.messages = tmp;
        }
    }

    public int[] output()
    {
        int[] out = new int[this.num];

        for (int i = 0; i < this.num; i++)
        {
            int best = -1;
            Double min = Double.MAX_VALUE;
            for (int v = 0; v < this.values; v++)
            {
                double val = this.dataWeight * this.dataCost.get(i).get(v);
                for (int j = 0; j < this.num; j++)
                {
                    if (!MathUtils.zero(this.smoothCost.get(j, i)))
                    {
                        val += this.messages[v].get(j, i);
                    }
                }
                
                if (best == -1 || val < min)
                {
                    min = val;
                    best = v;
                }
            }

            out[i] = best;
        }

        return out;
    }
};
