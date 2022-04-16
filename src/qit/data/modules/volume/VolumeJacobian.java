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

package qit.data.modules.volume;

import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.MatrixSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.data.utils.VolumeUtils;
import qit.math.utils.MathUtils;

@ModuleDescription("Compute the jacobian of a deformation volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeJacobian implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    private Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("optional affine component to remove")
    private Affine affine;

    @ModuleParameter
    @ModuleDescription("treat the input as a deformation (i.e. displacement)")
    private boolean deformation = false;

    @ModuleParameter
    @ModuleDescription("return the eigenvalues (exclusive with determinant flag)")
    private boolean eigenvalues = false;

    @ModuleParameter
    @ModuleDescription("return the determinant (exclusive with eigenvalues flag)")
    private boolean determinant = false;

    @ModuleParameter
    @ModuleDescription("return the logarithm (works with other options)")
    private boolean logarithm = false;

    @ModuleOutput
    @ModuleDescription("output table")
    private Volume output;

    @Override
    public Module run()
    {
        Global.assume(!(this.eigenvalues && this.determinant), "cannot return both eigenvalues and determinant");

        Matrix[] jacs = VolumeUtils.jacobian(this.input, this.mask);
        Matrix aff = this.affine != null ? this.affine.mat3().minus(MatrixSource.identity(3)) : null;

        int dim = this.determinant ? 1 : this.eigenvalues ? 3 : 9;
        Sampling sampling = this.input.getSampling();
        Volume out = this.input.proto(dim);
        for (int i = 0; i < sampling.size(); i++)
        {
            Matrix jac = jacs[i];

            if (jac != null)
            {
                if (this.deformation)
                {
                    jac.plusEquals(MatrixSource.identity(3));
                }

                if (aff != null)
                {
                    // the affine transform contributes a constant component
                    jac.minusEquals(aff);
                }

                if (this.determinant)
                {
                    double value = jac.det();

                    if (this.logarithm)
                    {
                        Global.assume(MathUtils.nonzero(value), "log of zero determinant found: " + sampling.sample(i));
                        value = Math.log(value);
                    }

                    out.set(i, 0, value);
                }
                else if (this.eigenvalues)
                {
                    EigenDecomp eig = MatrixUtils.eig(jac);
                    Vect vals = eig.values;

                    if (this.logarithm)
                    {
                        for (int j = 0; j < vals.size(); j++)
                        {
                            Global.assume(MathUtils.nonzero(vals.get(j)), "log of zero eigenvalue found: " + sampling.sample(i));
                        }

                        vals = vals.log();
                    }

                    out.set(i, vals);
                }
                else
                {
                    if (this.logarithm)
                    {
                        jac = MatrixUtils.log(jac);
                    }

                    out.set(i, jac.packRow());
                }
            }
        }

        this.output = out;

        return this;
    }
}
