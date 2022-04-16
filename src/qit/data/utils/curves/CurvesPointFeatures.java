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

import com.google.common.collect.Lists;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Deformation;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;
import qit.math.source.VectFunctionSource;
import qit.math.utils.MathUtils;

import java.util.List;

/** compute position and directional features from curves */
public class CurvesPointFeatures
{
    private Curves curves;
    private Vect curvesWeights;
    private Deformation deform;
    private Double delta = 1.0;

    private Vect weights;
    private Vects pos;
    private Vects dir;

    public CurvesPointFeatures()
    {
    }

    public CurvesPointFeatures withInput(Curves input)
    {
        this.curves = input;
        return this;
    }

    public CurvesPointFeatures withWeights(Vect v)
    {
        this.curvesWeights= v;
        return this;
    }

    public CurvesPointFeatures withTransform(Deformation f)
    {
        this.deform = f;
        return this;
    }

    public CurvesPointFeatures withDelta(double d)
    {
        this.delta = d;
        return this;
    }

    public CurvesPointFeatures run()
    {
//        Global.assume(this.curvesWeights == null || this.curvesWeights.size() == this.curves.size(), "weights must match curves");

        Vects tmp_pos = new Vects();
        Vects tmp_dir = new Vects();
        List<Double> tmp_weights = Lists.newArrayList();
        VectFunction reorient = VectFunctionSource.reorient(this.deform, this.delta, this.delta, this.delta);

        for (int i = 0; i < this.curves.size(); i++)
        {
            Curve curve = this.curves.get(i);
            for (int j = 0; j < curve.size() - 1; j++)
            {
                Vect start = curve.get(j);
                Vect end = curve.get(j + 1);

                Vect diff = start.minus(end);
                double length = diff.norm();

                double weight = length;

                if (this.curvesWeights != null)
                {
                    weight *= this.curvesWeights.get(i);
                }

                Vect pos = start.plus(end).times(0.5);
                Vect dir = MathUtils.zero(length) ? VectSource.randomUnit() : diff.times(1.0 / length);

                if (this.deform != null)
                {
                    Vect cat = reorient.apply(VectSource.cat(pos, dir));
                    pos = cat.sub(0, 3);
                    dir = cat.sub(3, 6);
                }

                tmp_weights.add(weight);
                tmp_pos.add(pos);
                tmp_dir.add(dir);
            }
        }

        this.weights = VectSource.create(tmp_weights);
        this.pos = tmp_pos;
        this.dir = tmp_dir;

        return this;
    }

    public Vect getOutputWeights()
    {
        if (this.weights == null)
        {
            run();
        }
        return this.weights;
    }

    public Vects getOutputPos()
    {
        if (this.pos == null)
        {
            run();
        }
        return this.pos;
    }

    public Vects getOutputDir()
    {
        if (this.dir == null)
        {
            run();
        }
        return this.dir;
    }
}
