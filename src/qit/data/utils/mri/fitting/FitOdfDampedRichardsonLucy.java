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

package qit.data.utils.mri.fitting;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.VectsUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class FitOdfDampedRichardsonLucy implements Supplier<VectFunction>
{
    public final static double DEFAULT_ALPHA = 1.7e-3;
    public final static double DEFAULT_ETA = 0.0025;
    public final static double DEFAULT_NU = 8;
    public final static int DEFAULT_ITERS = 250;
    public final static double DEFAULT_SCALE = 100;

    public Gradients gradients;
    public Vects dirs;

    public Double alpha = DEFAULT_ALPHA; // the response function parameter
    public Double eta = DEFAULT_ETA; // a threshold parameter and controls where damping starts according to the FOF amplitude
    public Double nu = DEFAULT_NU; // a geometrical parameter describing the profile of the damping curve and how fast the damping turns on and off
    public int iters = DEFAULT_ITERS;

    public VectFunction get()
    {
        Global.assume(this.dirs != null, "no encoding odf encoding directions were found");

        this.dirs = VectsUtils.normalize(this.dirs);

        int m = this.gradients.size(); // number of mri samples
        int n = this.dirs.size(); // output odf samples

        final Matrix H = new Matrix(m, n);
        for (int nidx = 0; nidx < n; nidx++)
        {
            for (int midx = 0; midx < m; midx++)
            {
                Vect v = this.dirs.get(nidx);
                Vect q = this.gradients.getBvec(midx);
                double b = this.gradients.getBval(midx);

                double dot = Math.abs(q.dot(v));
                double val = Math.exp(-this.alpha * b * dot * dot);
                H.set(midx, nidx, val);
            }
        }

        final Matrix HT = H.transpose();
        final Matrix HTH = HT.times(H);
        final Vect finit = VectSource.createND(n, 1.0 / n);
        final Vect ones = VectSource.createND(n, 1);
        final double etanu = Math.pow(this.eta, this.nu);

        List<List<Integer>> shells = Lists.newArrayList();
        for (int shell : this.gradients.getShells(false))
        {
            shells.add(this.gradients.getShellsIdx(shell));
        }

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Vect s = FitOdfDampedRichardsonLucy.this.gradients.norm(input);
                Vect f = finit.copy();

                for (int i = 0; i < FitOdfDampedRichardsonLucy.this.iters; i++)
                {
                    Vect fnu = f.pow(FitOdfDampedRichardsonLucy.this.nu);
                    Vect feta = fnu.plus(etanu);
                    Vect r = ones.minus(fnu.divSafe(feta));

                    Vect stds = VectSource.createND(shells.size());
                    for (int j = 0; j < shells.size(); j++)
                    {
                        stds.set(j, s.sub(shells.get(j)).std());
                    }

                    double mu = Math.max(0.0, 1.0 - 4 * stds.mean());
                    Vect u = ones.minus(mu, r);

                    Vect HTHf = HTH.times(f);
                    Vect HTs = HT.times(s);
                    Vect rhs = HTs.minus(HTHf).divSafe(HTHf);
                    Vect update = ones.plus(u.times(rhs));

                    f.timesEquals(update);
                }

                output.set(f);
                output.timesEquals(DEFAULT_SCALE);
            }
        }.init(this.gradients.size(), this.dirs.size());
    }
}
