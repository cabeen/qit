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

package qit.data.utils.curves;

import qit.base.Logging;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.VectUtils;
import qit.math.structs.VectFunction;
import qit.math.structs.GaussianWatsonMixture;
import qit.math.source.VectFunctionSource;
import qit.math.utils.MathUtils;

public class CurvesBag
{
    private Curves input;
    private Vects output;

    private VectFunction xfm = null;
    private Double delta = 1.0;
    private Double thresh = null;
    private GaussianWatsonMixture model;
    private boolean soft = false;
    private boolean norm = false;

    public CurvesBag()
    {
    }

    public CurvesBag withModel(GaussianWatsonMixture m)
    {
        this.model = m;
        this.output = null;
        
        return this;
    }

    public CurvesBag withInput(Curves c)
    {
        this.input = c;
        this.output = null;
        
        return this;
    }

    public CurvesBag withSoft(boolean s)
    {
        this.soft = s;
        this.output = null;
        
        return this;
    }

    public CurvesBag withNorm(boolean v)
    {
        this.norm = v;
        this.output = null;
        
        return this;
    }

    public CurvesBag withTransform(VectFunction f)
    {
        this.xfm = f;
        
        return this;
    }

    public CurvesBag withDelta(double v)
    {
        this.delta = v;
        
        return this;
    }

    public CurvesBag withThresh(double v)
    {
        this.thresh = v;
        
        return this;
    }

    public CurvesBag run()
    {
        Vects features = new Vects();
        VectFunction reorient = this.xfm == null ? null : VectFunctionSource.reorient(this.xfm, this.delta, this.delta, this.delta);

        Logging.info("... starting curve processing");
        int dim = this.model.getNum();
        Vect resp = VectSource.createND(dim);
        int ppercent = 0;
        int num = this.input.size();
        for (int i = 0; i < num; i++)
        {
            int percent = (int) Math.ceil(100.0 * (i + 1) / num);
            if (percent >= ppercent + 5)
            {
                Logging.info("... processed " + percent + " percent");
                ppercent = percent;
            }

            Curve curve = this.input.get(i);
            Vect feature = VectSource.createND(dim);
            for (int j = 0; j < curve.size() - 1; j++)
            {
                Vect start = curve.get(j);
                Vect end = curve.get(j + 1);

                Vect diff = start.minus(end);
                double length = diff.norm();

                if (MathUtils.zero(length))
                {
                    continue;
                }

                Vect pos = start.plus(end).times(0.5);
                Vect dir = diff.times(1.0 / length);

                if (this.xfm != null)
                {
                    Vect cat = reorient.apply(VectSource.cat(pos, dir));
                    pos = cat.sub(0, 3);
                    dir = cat.sub(3, 6);
                }

                this.model.resp(pos, dir, resp);
                if (this.soft)
                {
                    // increment all bags with weights by response
                    feature.plusEquals(length, resp);
                }
                else
                {
                    // increment only the most likely bag
                    int midx = VectUtils.maxidx(resp);
                    feature.set(midx, feature.get(midx) + length);
                }
            }

            if (this.norm)
            {
                double sum = feature.sum();
                if (sum != 0)
                {
                    feature.timesEquals(1.0 / sum);
                }
            }

            if (this.thresh != null)
            {
                feature = feature.thresh(this.thresh);
            }

            features.add(feature);
        }
        Logging.info("... finished curve processing");

        this.output = features;

        return this;
    }

    public Vects getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
