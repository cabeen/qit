/*******************************************************************************
 *
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 *    must display the following acknowledgement:
 *    This product includes software developed by the Ryan Cabeen.
 * 3. Neither the name of the Ryan Cabeen nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
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
 *
 *******************************************************************************/

package qit.data.utils.mri.fitting;

import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.MatrixSource;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.models.Kurtosis;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

public class FitKurtosisLLS implements Supplier<VectFunction>
{
    public static final String LLS = "lls";
    public static final String WLLS = "wlls";

    public Gradients gradients;
    public boolean weighted = false;

    public FitKurtosisLLS withGradients(Gradients g)
    {
        this.gradients = g;

        return this;
    }

    public FitKurtosisLLS withWeighted(boolean v)
    {
        this.weighted = v;

        return this;
    }

    public VectFunction get()
    {
        Logging.info("WARNING: kurtosis fitting does not yet compute all of the derived parameters");

        final Matrix A = Kurtosis.getLinearSystemMatrixLog(this.gradients);
        final Matrix Ainv = A.inv();

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                double b0 = ModelUtils.baselineStats(FitKurtosisLLS.this.gradients, input).mean;
                Vect norm = input.times(1.0 / Math.max(b0, Global.DELTA));

                Matrix B = Kurtosis.getLinearSystemSignalLog(norm);
                Matrix X = Ainv.times(B);

                if (FitKurtosisLLS.this.weighted)
                {
                    Matrix W = MatrixSource.diag(A.times(X.getColumn(0)).exp());
                    X = (W.times(A)).inv().times(W.times(B));
                }

                Kurtosis model = new Kurtosis();
                model.dt = Kurtosis.getLinearSystemSolution(X.getColumn(0));
                model.b0 = b0;
                model.updateFeatures();

                output.set(model.getEncoding());
            }
        }.init(this.gradients.size(), new Kurtosis().getEncodingSize());
    }
}