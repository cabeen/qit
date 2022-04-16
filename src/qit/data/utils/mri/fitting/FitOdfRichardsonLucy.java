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

import qit.base.Global;
import qit.base.Logging;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.VectsUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.function.Supplier;

public class FitOdfRichardsonLucy
{
    public final static double DEFAULT_ALPHA = 1.7e-3;
    public final static double DEFAULT_BETA = 0; // 1.2e-4;
    public final static int DEFAULT_RLDITERS = 500;

    @ModuleParameter
    @ModuleDescription("the axial response function parameter")
    public Double alpha = DEFAULT_ALPHA;

    @ModuleParameter
    @ModuleDescription("the radial response function parameter")
    public Double beta = DEFAULT_BETA;

    @ModuleParameter
    @ModuleDescription("the number of iterations")
    public int rlditers = DEFAULT_RLDITERS;

    public VectFunction fitter(Gradients gradients, Vects points)
    {
        Global.assume(points != null, "no encoding odf encoding directions were found");

        points = VectsUtils.normalize(points);

        int m = gradients.size(); // number of mri samples
        int n = points.size(); // output odf samples

        final Matrix H = new Matrix(m, n);
        for (int midx = 0; midx < m; midx++)
        {
            Vect q = gradients.getBvec(midx).normalize();
            double b = gradients.getBval(midx);

            for (int nidx = 0; nidx < n; nidx++)
            {
                Vect v = points.get(nidx);

                double dot2 = MathUtils.square(q.dot(v));
                double val = Math.exp(-this.alpha * b * dot2);

                if (MathUtils.nonzero(this.beta))
                {
                    val *= Math.exp(-2.0 * this.beta * b * (1.0 - dot2));
                }

                H.set(midx, nidx, val);
            }
        }

        final Matrix HT = H.transpose();
        final Matrix HTH = HT.times(H);
        final Vect finit = VectSource.createND(n, 1.0 / n);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Vect s = gradients.norm(input);
                Vect f = finit.copy();

                for (int i = 0; i < FitOdfRichardsonLucy.this.rlditers; i++)
                {
                    Vect update = HT.times(s).divSafe(HTH.times(f));
                    f.timesEquals(update);
                }

                output.set(f);
            }
        }.init(gradients.size(), points.size());
    }
}
