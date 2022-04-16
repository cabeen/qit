/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.utils.mri.estimation;

import qit.base.Global;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.models.Mcsmt;
import qit.data.source.MatrixSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.data.utils.mri.CaminoUtils;
import qit.data.utils.mri.structs.ModelEstimator;
import qit.math.utils.MathUtils;

import java.util.List;

public class McsmtEstimator extends ModelEstimator
{
    public static final double EPS = 1e-6;

    @Override
    public Vect run(List<Double> weights, List<Vect> input)
    {
        Global.assume(weights.size() == input.size(), "invalid weights");

        double weightSum = 0;
        double baseSum = 0;
        double fracSum = 0;
        double diffSum = 0;

        for (int i = 0; i < weights.size(); i++)
        {
            Mcsmt mcsmt = new Mcsmt(input.get(i));

            double base = mcsmt.getBase();
            double frac = mcsmt.getFrac();
            double diff = Math.log(mcsmt.getDiff() + EPS);
            double weight = weights.get(i);

            weightSum += weight;
            baseSum += weight * base;
            fracSum += weight * frac;
            diffSum += weight * diff;
        }

        if (MathUtils.zero(weightSum))
        {
            return new Mcsmt().getEncoding();
        }

        double norm = 1.0 / weightSum;
        double base = baseSum * norm;
        double frac = fracSum * norm;
        double diff = Math.exp(diffSum * norm - EPS);

        Mcsmt mcsmt = new Mcsmt();
        mcsmt.setBase(base);
        mcsmt.setFrac(frac);
        mcsmt.setDiff(diff);

        return mcsmt.getEncoding();
    }
}