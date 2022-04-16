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
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.data.utils.mri.structs.ModelEstimator;
import qit.data.models.Tensor;

import java.util.List;

public class TensorEstimator extends ModelEstimator
{
    public static final double LARGE = 1e6;
    public boolean log = false;

    @Override
    public Vect run(List<Double> weights, List<Vect> input)
    {
        Global.assume(weights.size() == input.size(), "invalid weights");

        double sumw = 0;
        Matrix summ = new Matrix(3, 3);
        double sumb = 0;
        double sumf = 0;
        boolean logged = false;

        for (int i = 0; i < weights.size(); i++)
        {
            double w = weights.get(i);
            Tensor t = new Tensor(input.get(i));
            double b = t.getBaseline();
            double f = t.getFreeWater();
            Vect vals = t.getVals();

            if (this.log && vals.min() > 1e-15)
            {
                for (int j = 0; j < 3; j++)
                {
                    double v = Math.abs(vals.get(j));
                    double logv = Math.max(-LARGE, Math.log(v));
                    vals.set(j, logv);
                }

                logged = true;
            }

            Vects vecs = t.getVecs();
            Matrix m = MatrixSource.eig(vals, vecs);

            sumw += w;
            summ.plusEquals(w, m);
            sumb += w * b;
            sumf += w * f;
        }

        if (sumw > 0)
        {
            double b = sumb / sumw;
            double f = sumf / sumw;
            Matrix m = summ.times(1.0 / sumw);

            if (logged)
            {
                try
                {
                    EigenDecomp eig = MatrixUtils.eig(m);

                    for (int j = 0; j < 3; j++)
                    {
                        double v = eig.values.get(j);
                        double expv = Math.min(LARGE, Math.exp(v));
                        eig.values.set(j, expv);
                    }

                    m = MatrixSource.eig(eig.values, eig.vectors);

                    return new Tensor(b, m, f).getEncoding();
                }
                catch (Exception e)
                {
                    return new Tensor().getEncoding();
                }
            }
            else
            {
                return new Tensor(b, m, f).getEncoding();
            }
        }
        else
        {
            return new Tensor().getEncoding();
        }
    }
}
