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
import qit.data.models.BiTensor;
import qit.data.models.Tensor;
import qit.data.source.MatrixSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.data.utils.mri.structs.ModelEstimator;

import java.util.List;
import java.util.function.Function;

public class BiTensorEstimator extends ModelEstimator
{
    public static final double LARGE = 1e6;
    public boolean log = false;

    @Override
    public Vect run(List<Double> weights, List<Vect> input)
    {
        Global.assume(weights.size() == input.size(), "invalid weights");

        Matrix tsum = new Matrix(3, 3);
        Matrix fsum = new Matrix(3, 3);

        double sumw = 0;
        double sumb = 0;
        double sumf = 0;

        boolean logged = false;

        for (int i = 0; i < weights.size(); i++)
        {
            double w = weights.get(i);
            BiTensor t = new BiTensor(input.get(i));

            double b = t.getBaseline();
            double f = t.getFraction();

            Vect tVals = t.getTissueVals();
            Vect fVals = t.getFluidVals();

            if (this.log)
            {
                if (tVals.min() > 1e-15 && fVals.min() > 1e-15)
                {
                    for (int j = 0; j < 3; j++)
                    {
                        tVals.set(j, Math.max(-LARGE, Math.log(Math.abs(tVals.get(j)))));
                        fVals.set(j, Math.max(-LARGE, Math.log(Math.abs(fVals.get(j)))));
                    }

                    logged = true;
                }
            }

            Vects tVecs = t.getTissueVecs();
            Matrix tMat = MatrixSource.eig(tVals, tVecs);

            Vects fVecs = t.getFluidVecs();
            Matrix fMat = MatrixSource.eig(fVals, fVecs);

            tsum.plusEquals(w, tMat);
            fsum.plusEquals(w, fMat);

            sumw += w;
            sumb += w * b;
            sumf += w * f;
        }

        BiTensor out = new BiTensor();

        if (sumw > 0)
        {
            double b = sumb / sumw;
            double f = sumf / sumw;

            Matrix tMat = tsum.times(1.0 / sumw);
            Matrix fMat = fsum.times(1.0 / sumw);

            if (logged)
            {
                Function<Matrix,Matrix> expmap = (m) ->
                {
                    EigenDecomp eig = MatrixUtils.eig(m);

                    for (int j = 0; j < 3; j++)
                    {
                        double v = eig.values.get(j);
                        double expv = Math.min(LARGE, Math.exp(v));
                        eig.values.set(j, expv);
                    }

                    return MatrixSource.eig(eig.values, eig.vectors);
                };

                try
                {
                    tMat = expmap.apply(tMat);
                    fMat = expmap.apply(fMat);
                }
                catch (Exception e)
                {
                }
            }

            out.setBaseline(b);
            out.setFraction(f);
            out.setTissueMatrix(tMat);
            out.setFluidMatrix(fMat);
        }

        return out.getEncoding();
    }
}
