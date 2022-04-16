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

import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.VectsUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;

import java.util.List;
import java.util.function.Supplier;

public class FitOdfFunkRadon implements Supplier<VectFunction>
{
    public Gradients gradients;
    public Vects dirs;
    public Double sigma = 10.0; // in degrees
    public Integer shell = null; // default will pick the lowest shell
    
    public VectFunction get()
    {
        this.dirs = VectsUtils.normalize(this.dirs);
        this.shell = this.shell == null ? this.gradients.getShells(false).get(0) : this.shell;
        final List<Integer> which = this.gradients.getShellsIdx(false, String.valueOf(this.shell));

        int m = which.size(); // number of mri samples
        int n = this.dirs.size(); // output odf samples
        int k = which.size(); // number of equator points

        double rsigma = this.sigma * Math.PI / 180; // convert degrees to radians
        double sigma2 = rsigma * rsigma;

        Matrix H = new Matrix(n, m);
        for (int a = 0; a < n; a++)
        {
            for (int b = 0; b < m; b++)
            {
                Vect v = this.dirs.get(a);
                Vect q = this.gradients.getBvec(which.get(b)).normalize();
                double dot = Math.abs(q.dot(v));
                dot = dot < 0 ? 0 : dot > 1 ? 1 : dot;
                double value = Math.acos(dot);
                value = Math.exp(-value * value / sigma2);
                H.set(a, b, value);
            }
        }

        Matrix S = new Matrix(n * k, n);
        for (int a = 0; a < n; a++)
        {
            Matrix R = MatrixSource.rotateAxis(this.dirs.get(a)).inv();

            for (int b = 0; b < k; b++)
            {
                int idx = a * k + b;

                double theta = b * 2.0 * Math.PI / (double) k;
                double cos = Math.cos(theta);
                double sin = Math.sin(theta);
                Vect s = R.times(VectSource.create3D(cos, sin, 0)).normalize();

                for (int c = 0; c < n; c++)
                {
                    Vect v = this.dirs.get(c);
                    double dot = Math.abs(s.dot(v));
                    dot = dot < 0 ? 0 : dot > 1 ? 1 : dot;
                    double value = Math.acos(dot);
                    value = Math.exp(-value * value / sigma2);
                    S.set(idx, c, value);
                }
            }
        }

        Matrix I = new Matrix(n, n * k);
        for (int a = 0; a < n; a++)
        {
            for (int b = 0; b < k; b++)
            {
                I.set(a, a * k + b, 1);
            }
        }

        final Matrix filter = I.times(S.times(H));

        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                Vect e = FitOdfFunkRadon.this.gradients.norm(input).sub(which);
                Vect td = filter.times(e);
                td.timesEquals(1.0 / td.sum());
                output.set(td);
            }
        }.init(this.gradients.size(), this.dirs.size());
    }
}
        