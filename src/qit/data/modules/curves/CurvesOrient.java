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

package qit.data.modules.curves;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.VectsUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;

@ModuleDescription("Orient curves to best match endpoints, i.e. flip them to make the starts and ends as close as possible")
@ModuleAuthor("Ryan Cabeen")
public class CurvesOrient implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("a maxima number of iterations")
    public int iters = 10;

    @ModuleParameter
    @ModuleDescription("orient to the nearest spatial axis")
    public boolean axis = false;

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @Override
    public CurvesOrient run()
    {
        this.output = this.axis ? axis(this.input) : intrinsic(this.input, this.iters);

        return this;
    }

    public static Curves axis(Curves input)
    {
        if (input.size() == 0)
        {
            return input.copy();
        }

        Curves out = input.copy();
        for (int i = 0; i < out.size(); i++)
        {
            Curves.Curve curve = out.get(i);

            Vect sum = VectSource.create3D();
            double num = curve.size() - 1;

            for (int j = 0; j < num; j++)
            {
                sum.plusEquals(1.0 / num, curve.get(j + 1).minus(curve.get(j)).normalize());
            }

            sum.normalizeEquals();

            double xdot = sum.dot(VectSource.createX());
            double ydot = sum.dot(VectSource.createY());
            double zdot = sum.dot(VectSource.createZ());
            double xabs = Math.abs(xdot);
            double yabs = Math.abs(ydot);
            double zabs = Math.abs(zdot);

            double minabs = Math.min(xabs, Math.min(yabs, zabs));

            if (xabs == minabs && xdot < 0)
            {
                curve.reverse();
            }
            else if (yabs == minabs && ydot < 0)
            {
                curve.reverse();
            }
            else if (zabs == minabs && zdot < 0)
            {
                curve.reverse();
            }
        }

        return out;
    }

    public static Curves intrinsic(Curves input, int iters)
    {
        if (input.size() == 0)
        {
            return input.copy();
        }

        Curves source = input.copy();

        int num = source.size();
        Vects heads = new Vects();
        Vects tails = new Vects();
        Vects mids = new Vects();

        Vect gamma = VectSource.create3D(0, 0.5, 1.0);

        for (Curves.Curve curve : source)
        {
            curve.resample(gamma);

            Vect head = curve.get(0);
            Vect tail = curve.size() > 2 ? curve.get(2) : head;
            Vect mid = curve.size() > 1 ? curve.get(1) : tail;

            heads.add(head);
            tails.add(tail);
            mids.add(mid);
        }

        // compute an guess for a good prototype
        int proto = 0;
        {
            Vect midMean = VectsUtils.mean(mids);
            double distMean = midMean.dist2(mids.get(0));
            for (int i = 1; i < num; i++)
            {
                double dist = midMean.dist2(mids.get(i));
                if (dist < distMean)
                {
                    proto = i;
                    distMean = dist;
                }
            }
        }

        boolean[] flip = new boolean[num];
        double[] dists = new double[num];
        for (int iter = 0; iter < iters; iter++)
        {
            VectsOnlineStats headStats = new VectsOnlineStats(3);
            VectsOnlineStats tailStats = new VectsOnlineStats(3);

            for (int i = 0; i < num; i++)
            {
                if (i == proto)
                {
                    dists[i] = 0;
                    continue;
                }

                boolean flipi = flip[i];
                boolean flipp = flip[proto];

                Vect headi = flipi ? heads.get(i) : tails.get(i);
                Vect taili = flipi ? tails.get(i) : heads.get(i);

                Vect headp = flipp ? heads.get(proto) : tails.get(proto);
                Vect tailp = flipp ? tails.get(proto) : heads.get(proto);

                double dhh = headi.dist2(headp);
                double dht = headi.dist2(tailp);
                double dth = taili.dist2(headp);
                double dtt = taili.dist2(tailp);

                double dorig = dhh + dtt;
                double dflip = dht + dth;

                if (dorig > dflip)
                {
                    flip[i] = !flipi;
                }

                dists[i] = Math.min(dorig, dflip);
                headStats.update(headi);
                tailStats.update(taili);
            }

            double minDist = Double.MAX_VALUE;
            int minIdx = proto;
            for (int i = 0; i < num; i++)
            {
                boolean flipi = flip[i];
                boolean flipp = flip[proto];

                Vect headi = flipi ? heads.get(i) : tails.get(i);
                Vect taili = flipi ? tails.get(i) : heads.get(i);

                Vect headp = flipp ? heads.get(proto) : tails.get(proto);
                Vect tailp = flipp ? tails.get(proto) : heads.get(proto);

                double dhh = headi.dist2(headp);
                double dtt = taili.dist2(tailp);

                double dist = dhh + dtt;

                if (dist < minDist)
                {
                    minIdx = i;
                    minDist = dist;
                }
            }
            proto = minIdx;
        }

        Curves out = input.copy();
        for (int i = 0; i < num; i++)
        {
            if (flip[i])
            {
                out.get(i).reverse();
            }
        }

        return out;
    }
}
