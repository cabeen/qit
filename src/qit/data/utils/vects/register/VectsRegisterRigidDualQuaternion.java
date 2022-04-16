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
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectsUtils;
import qit.math.utils.MathUtils;

// Walker, Michael W., Lejun Shao, and Richard A. Volz. \"Estimating 3-D location parameters using dual number quaternions.\" CVGIP: image understanding 54.3 (1991): 358-367."
public class VectsRegisterRigidDualQuaternion
{
    public static Affine estimate(Vects source, Vects dest)
    {
        return estimate(null, source, dest);
    }

    public static Affine estimate(Vect weights, Vects source, Vects dest)
    {
        int num = source.size();

        if (weights == null || MathUtils.zero(weights.sum()))
        {
            weights = VectSource.createND(num, 1.0);
        }

        Global.assume(weights.size() == num, "weights do not match source vects");
        Global.assume(dest.size() == num, "dest vects do not match source vects");
        Global.assume(source.getDim() == 3, "source must be three dimensional");
        Global.assume(dest.getDim() == 3, "source must be three dimensional");

        // Normalize the weights
        weights = weights.times(1.0 / weights.sum());

        Matrix C1 = new Matrix(4, 4);
        Matrix C2 = new Matrix(4, 4);

        for (int i = 0; i < num; i++)
        {
            C1.plusEquals((qToQ(dest.get(i)).transpose().times(qToW(source.get(i)))).times(weights.get(i)));
            C2.plusEquals((qToW(source.get(i)).minus(qToQ(dest.get(i)))).times(weights.get(i)));
        }

        C1.timesEquals(-2);
        C2.timesEquals(2);

        Matrix A = (C2.transpose().times(C2).times(.5).minus(C1).minus(C1.transpose())).times(.5);

        // Find the largest eigenvalue
        MatrixUtils.EigenDecomp eig = MatrixUtils.eig(A);

        int maxIndex = 0;
        double maxD = eig.values.get(0);

        for (int i = 1; i < 4; i++)
        {
            if (eig.values.get(i) > maxD)
            {
                maxD = eig.values.get(i);
                maxIndex = i;
            }
        }

        // Compute the rotation and translation
        Vect qdouble = eig.vectors.get(maxIndex);

        Matrix q = MatrixSource.createCol(eig.vectors.get(maxIndex));
        Matrix qhat = q.sub(0, 2, 0, 0);
        Matrix term1 = MatrixSource.identity(3).times(Math.pow(qdouble.get(3), 2) - qhat.transpose().times(qhat).get(0, 0));
        Matrix term2 = qhat.times(qhat.transpose()).times(2);
        Matrix term3 = qToK(qdouble).times(2 * qdouble.get(3));
        Matrix R = term1.plus(term2).plus(term3);

        Matrix s = C2.times(q).times(-.5);
        Matrix p = qToW(qdouble).transpose().times(s);
        Vect t = p.sub(0, 2, 0, 0).packColumn();

        return new Affine(R, t);
    }

    private static Matrix qToK(Vect q)
    {
        Global.assume(q.size() == 3 || q.size() == 4, "invalid q: " + q.toString());

        Matrix K = new Matrix(3, 3);
        K.set(0, 0, 0);
        K.set(0, 1, q.get(2));
        K.set(0, 2, -q.get(1));
        K.set(1, 0, -q.get(2));
        K.set(1, 1, 0);
        K.set(1, 2, q.get(0));
        K.set(2, 0, q.get(1));
        K.set(2, 1, -q.get(0));
        K.set(2, 2, 0);

        return K;
    }

    private static Matrix qToW(Vect q)
    {
        Global.assume(q.size() == 3 || q.size() == 4, "invalid q: " + q.toString());

        // Check the size, and add a zero if necessary
        q = q.size() == 3 ? q.cat(VectSource.create1D(0)) : q;

        Matrix K = qToK(q);

        Matrix W = new Matrix(4, 4);
        W.set(0, 2, 0, 2, MatrixSource.identity(3).times(q.get(3)).minus(K));
        Vect qhat = VectSource.create3D(q.get(0), q.get(1), q.get(2));

        for (int i = 0; i < 3; i++)
        {
            W.set(i, 3, qhat.get(i));
            W.set(3, i, -qhat.get(i));
        }
        W.set(3, 3, q.get(3));

        return W;
    }

    private static Matrix qToQ(Vect q)
    {
        Global.assume(q.size() == 3 || q.size() == 4, "invalid q: " + q.toString());

        // Check the size, and add a zero if necessary
        q = q.size() == 3 ? q.cat(VectSource.create1D(0)) : q;

        Matrix K = qToK(q);
        Matrix Qmtx = new Matrix(4, 4);
        Qmtx.set(0, 2, 0, 2, MatrixSource.identity(3).times(q.get(3)).plus(K));
        Vect qhat = VectSource.create3D(q.get(0), q.get(1), q.get(2));

        for (int i = 0; i < 3; i++)
        {
            Qmtx.set(i, 3, qhat.get(i));
            Qmtx.set(3, i, -qhat.get(i));
        }
        Qmtx.set(3, 3, q.get(3));

        return Qmtx;
    }
}
