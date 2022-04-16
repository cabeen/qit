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
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.models.Spharm;
import qit.data.models.Tensor;
import qit.data.modules.vects.VectsCreateSphereLookup;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class FitSpharmCSD implements Supplier<VectFunction>
{
    public static final int DEFAULT_ORDER = 8;
    public static final int DEFAULT_ITERS = 50;
    public static final double DEFAULT_LAMBDA = 0.2;
    public static final double DEFAULT_TAU = 0.1;
    public static final double DEFAULT_BASELINE = 1.0;
    public static final double DEFAULT_AXIAL = 1.7e-3;
    public static final double DEFAULT_RADIAL = 1.2e-4;

    public Gradients gradients = null;
    public Integer shell = null; // default will pick the lowest shell
    public int order = DEFAULT_ORDER; // spherical harmonic order
    public int iters = DEFAULT_ITERS ; // spherical harmonic order
    public double lambda = DEFAULT_LAMBDA; // weight given to the constrained-positivity regularization
    public double tau = DEFAULT_TAU; // values below this fractional threshold are set to zero
    public double baseline = DEFAULT_BASELINE;
    public double axial = DEFAULT_AXIAL;
    public double radial = DEFAULT_RADIAL;
    public boolean normalize = false;

    public VectFunction get()
    {
        Global.assume(this.gradients != null, "invalid gradients");

        if (this.shell == null)
        {
            List<Integer> shells = this.gradients.getShells(false);
            this.shell = shells.get(shells.size() - 1);

            Logging.info("using highest shell by default");
        }

        final List<Integer> which = this.gradients.getShellsIdx(false, String.valueOf(this.shell));
        Logging.info(String.format("using %d signal measurements at b=%d: ", which.size(), this.shell));

        final int size = Spharm.orderToSize(this.order);

        Vects gdirs = new Vects();
        for (Integer idx : which)
        {
            gdirs.add(this.gradients.getBvec(idx));
        }
        Matrix bdwi = Spharm.bmatrix(this.order, gdirs);

        Vects sphere = new VectsCreateSphereLookup().run().output;

        Logging.info(String.format("using %d reconstruction points", sphere.size()));

        Matrix bsphere = Spharm.bmatrix(this.order, sphere);

        Tensor tensor = new Tensor();
        tensor.setBaseline(this.baseline);
        tensor.setVal(0, this.axial);
        tensor.setVal(1, this.radial);
        tensor.setVal(2, this.radial);
        tensor.setVec(0, VectSource.createZ());
        tensor.setVec(1, VectSource.createY());
        tensor.setVec(2, VectSource.createX());
        Vect respSig = Tensor.synth(this.gradients).apply(tensor.getEncoding()).sub(which);
        Vect respSH = MatrixUtils.solve(bdwi, respSig);

        List<Pair<Integer,Integer>> indices = Spharm.indices(this.order);

        List<Integer> zeros = Lists.newArrayList();
        for (int i = 0; i < indices.size(); i++)
        {
            if (indices.get(i).a == 0)
            {
                zeros.add(i);
            }
        }

        // find the rotational harmonics
        Vect respRH = VectSource.createND(zeros.size());
        Vect dirac = Spharm.bvect(this.order, VectSource.createZ());
        for (int i = 0; i < respRH.size(); i++)
        {
            int zidx = zeros.get(i);
            double sh = respSH.get(zidx);
            double scale = dirac.get(zidx);
            double rh = MathUtils.zero(scale) ? 0.0 : sh / scale;

            respRH.set(i, rh);
        }

        Vect diagRH = VectSource.createND(indices.size());
        for (int i = 0; i < diagRH.size(); i++)
        {
            int ridx = indices.get(i).b / 2;
            diagRH.set(i, respRH.get(ridx));
        }

        double nlambda = this.lambda * size * respRH.get(0) / (Math.sqrt(2) * bsphere.rows());
        bsphere.timesEquals(nlambda);

        Matrix X = bdwi.times(MatrixSource.diag(diagRH));
        Matrix XT = X.transpose();
        Matrix P = XT.times(X);

        Function<Vect,Vect> estimate = (input) ->
        {
            Vect sig = this.normalize ? this.gradients.norm(input).sub(which) : input.sub(which);
            Vect z = XT.times(sig);

            Vect output = MatrixUtils.solve(P, z, 1e-5);

            if (MathUtils.zero(this.lambda))
            {
                return output;
            }

            Vect trunc = output;
            if (output.size() - 1 > 16)
            {
                trunc = trunc.setAll(16, trunc.size(), 0);
            }

            Vect odf = bsphere.times(trunc);
            double thresh = bsphere.get(0, 0) * output.get(0) * FitSpharmCSD.this.tau;

            List<Integer> pass = odf.above(thresh);
            int fail = odf.size() - pass.size();

            if (pass.size() == 0)
            {
                output.setAll(0);
                return output;
            }

            if (fail == 0)
            {
                Vect nodf = bsphere.times(output);

                if (nodf.above(thresh).size() == odf.size())
                {
                    output.set(output);
                    return output;
                }
            }

            for (int i = 0; i < this.iters; i++)
            {
                List<Integer> passPrev = pass;
                int failPrev = fail;

                Matrix H = bsphere.subrows(pass);
                Matrix HTH = H.transpose().times(H);
                Matrix Q = P.plus(HTH);

                output = MatrixUtils.solve(Q, z, 1e-5);
                odf = bsphere.times(output);
                pass = odf.above(thresh);
                fail = odf.size() - pass.size();

                if (failPrev == fail)
                {
                    passPrev.removeAll(pass);
                    if (passPrev.size() == 0)
                    {
                        break;
                    }
                }
            }

            return output;
        };

        return VectFunction.create(estimate, this.gradients.size(), size);
    }
}
