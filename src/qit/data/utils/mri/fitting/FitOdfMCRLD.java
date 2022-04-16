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
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.models.Mcsmt;
import qit.data.source.VectSource;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.structs.Shells;
import qit.math.structs.VectFunction;

public class FitOdfMCRLD extends FitMcsmt
{
    public final static int DEFAULT_ITERS = 250;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the maximum number of iterations")
    public int iters = DEFAULT_ITERS;

    public VectFunction fitter(final Gradients gradients, final Vects dirs)
    {
        Global.assume(dirs != null, "no encoding odf encoding directions were found");

        VectFunction smter = this.fitter(gradients);
        final Shells sheller = new Shells(gradients);

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Mcsmt smt = new Mcsmt(smter.apply(input));

                int m = gradients.size(); // number of mri samples
                int n = dirs.size(); // output odf samples

                final Matrix H = new Matrix(m, n);
                for (int nidx = 0; nidx < n; nidx++)
                {
                    for (int midx = 0; midx < m; midx++)
                    {
                        Vect v = dirs.get(nidx);
                        Vect q = gradients.getBvec(midx);
                        double b = gradients.getBval(midx);

                        double dot = q.dot(v);
                        double val = Math.exp(-smt.getDiff() * b * dot * dot);
                        H.set(midx, nidx, val);
                    }
                }

                final Matrix HT = H.transpose();
                final Matrix HTH = HT.times(H);
                final Vect finit = VectSource.createND(n, 1.0 / n);

                Vect extsig = sheller.expand(smt.synthExtrinsic(sheller.shells()));
                Vect intsig = input.minus(extsig).abs();
                Vect normsig = gradients.norm(intsig);

                Vect f = finit.copy();

                for (int i = 0; i < FitOdfMCRLD.this.iters; i++)
                {
                    f.timesEquals(HT.times(normsig).divSafe(HTH.times(f)));
                }

                f.timesEquals(smt.getFrac() / f.sum());

                output.set(f);
            }
        }.init(gradients.size(), dirs.size());
    }
}
        