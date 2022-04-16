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

import com.google.common.collect.Maps;
import qit.base.Logging;

import java.util.Map;

public class VectOnlineStats
{
    // model statistics that can be updated in an online fashion
    // this doesn't include median and quartiles unfortunately...
    public boolean init = false;
    public double min = 0;
    public double max = 0;
    public double mean = 0;
    public double var = 0;
    public double std = 0;
    public double stde = 0;
    public double sum = 0;
    public double num = 0;
    public double cv = 0;
    private double m2 = 0; // An intermediate for computing the variance

    public void update(double v)
    {
        this.update(v, false);
    }

    public void update(double v, boolean skipnan)
    {
        if (Double.isNaN(v))
        {
            if (skipnan)
            {
                Logging.info("nan encountered");
            }
            else
            {
                throw new RuntimeException("nan encountered");
            }
        }

        this.num += 1;
        this.min = this.init ? Math.min(this.min, v) : v;
        this.max = this.init ? Math.max(this.max, v) : v;
        this.sum += v;

        // Use an online algorithm for mean and variance
        double nmean = this.mean + (v - this.mean) / this.num;
        this.m2 += (v - nmean) * (v - this.mean);
        this.mean = nmean;
        this.var = this.m2 / this.num;
        this.std = Math.sqrt(this.var);
        this.stde = this.std / Math.sqrt(this.num);
        this.cv = this.std / this.mean;
        this.init = true;
    }

    public Map<String,Double> lookup()
    {
        Map<String, Double> out = Maps.newHashMap();

        out.put("min", this.min);
        out.put("max", this.max);
        out.put("mean", this.mean);
        out.put("var", this.var);
        out.put("std", this.std);
        out.put("stde", this.stde);
        out.put("sum", this.sum);
        out.put("num", this.num);

        return out;
    }

    public void clear()
    {
        this.init = false;
        this.min = 0;
        this.max = 0;
        this.mean = 0;
        this.var = 0;
        this.std = 0;
        this.stde = 0;
        this.sum = 0;
        this.num = 0;
        this.cv = 0;
        this.m2 = 0;
    }
}
