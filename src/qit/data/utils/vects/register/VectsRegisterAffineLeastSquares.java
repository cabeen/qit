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

package qit.data.utils.vects.register;

import qit.base.Global;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.math.utils.MathUtils;

public class VectsRegisterAffineLeastSquares
{
    public static Affine estimate(Vects source, Vects dest)
    {
        Global.assume(source.size() == dest.size(), "the number of vects must match");
        Global.assume(source.size() > 0, "a positive number of vects must be used");
        Global.assume(source.getDim() == dest.getDim(), "the dimensions of the vects must match");

        int dim = source.getDim();
        int num = source.size();

        int sizeJ = dim * (dim + 1);
        int sizeI = dim * num;

        Matrix lhs = new Matrix(sizeI, sizeJ);
        Matrix rhs = new Matrix(sizeI, 1);

        for (int i = 0; i < num; i++) {
            for (int j = 0; j < dim; j++) {
                for (int k = 0; k < dim; k++) {
                    lhs.set(i * dim + j, j * dim + k, source.get(i).get(k));
                }
                lhs.set(i * dim + j, dim * dim + j, 1);
                rhs.set(i * dim + j, 0, dest.get(i).get(j));
            }
        }

        // Compute the rotation
        Matrix solution = MatrixUtils.solve(lhs, rhs);

        // Create the transforms
        Vect linearCoeff = solution.sub(0, dim * dim - 1, 0, 0).packColumn();
        Matrix R = MatrixSource.createCols(linearCoeff, dim, dim);
        Vect t = solution.sub(dim * dim, sizeJ - 1, 0, 0).packColumn();

        return new Affine(R, t);
    }
}
