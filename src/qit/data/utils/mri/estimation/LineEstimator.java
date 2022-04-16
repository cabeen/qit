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
import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.models.Noddi;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.data.utils.mri.CaminoUtils;
import qit.data.utils.mri.structs.ModelEstimator;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.function.Supplier;

public class LineEstimator extends ModelEstimator
{
    @Override
    public Vect run(List<Double> weights, List<Vect> input)
    {
        Global.assume(weights.size() == input.size(), "invalid weights");

        Vect premean = premean(weights, input);

        double weightSum = 0;
        Vect vectSum = VectSource.create3D();

        for (int i = 0; i < weights.size(); i++)
        {
            double weight = weights.get(i);
            Vect vect = input.get(i);

            if (premean.dot(vect) < 0)
            {
                vect.timesEquals(-1.0);
            }

            weightSum += weight;
            vectSum.plusEquals(weight, vect);
        }

        if (MathUtils.zero(weightSum))
        {
            Logging.info("returned zero vector");
            return VectSource.create3D();
        }

        double norm = MathUtils.zero(weightSum) ? 1.0 : 1.0 / weightSum;
        Vect vect = vectSum.times(norm);

        return vect;
    }

    public static Vect premean(List<Double> weights, List<Vect> input)
    {
        double weightSum = 0;
        Matrix matrixSum = new Matrix(3, 3);

        for (int i = 0; i < weights.size(); i++)
        {
            double weight = weights.get(i);
            Vect peak = input.get(i);
            Matrix matrix = MatrixSource.outer(peak, peak);

            weightSum += weight;
            matrixSum.plusEquals(weight, matrix);
        }

        if (MathUtils.zero(weightSum))
        {
            Logging.info("returned zero vector");
            return VectSource.create3D();
        }

        double norm = 1.0 / weightSum;
        Matrix matrix = matrixSum.times(norm);

        EigenDecomp eig = MatrixUtils.eig(matrix);
        return eig.vectors.get(0).normalize();
    }
}