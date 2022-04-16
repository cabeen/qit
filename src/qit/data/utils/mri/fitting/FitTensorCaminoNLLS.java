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

import imaging.DW_Scheme;
import inverters.ModelIndex;
import inverters.NonLinearDT_Inversion;
import qit.base.Logging;
import qit.data.datasets.Vect;
import qit.data.utils.mri.structs.Gradients;
import qit.data.models.Tensor;
import qit.data.utils.mri.CaminoUtils;
import qit.math.structs.VectFunction;

public class FitTensorCaminoNLLS extends VectFunction
{
    /*
     * This class implements the abstract methods of MarquardtChiSqFitter to
     * provide a Levenburg-Marquardt algorithm for fitting a mri tensor to
     * DW-MR measurements. The fitter fits the model to the normalized data
     * directly without taking logs, so that the noise statistics are less
     * corrupted. The mri tensor is constrained to be positive definite by
     * optimizing the parameters of its Cholesky decomposition.
     */
    public static final ModelIndex CHOL = ModelIndex.NLDT_POS;

    /*
     * This class implements the abstract methods of MarquardtFitter to provide
     * a Levenburg-Marquardt algorithm for fitting a mri tensor to DW-MR
     * measurements. The fitter fits the model to the normalized data directly
     * without taking logs, so that the noise statistics are less corrupted. The
     * mri tensor is unconstrained so may not end up positive definite.
     */
    public static final ModelIndex UNC = ModelIndex.NLDT;

    /*
     * Optimizes the Cholesky decomposition of the tensor, similar to
     * DiffTensorFitter, but uses the Minpack optimizer rather than
     * optimizers.MarquardtMinimiser.
     */
    public static final ModelIndex CHOL_MINPACK = ModelIndex.NLDT_MINPACK;

    public static final ModelIndex DEFAULT_MODEL = CHOL;

    private Double clamp = null;
    private Gradients gradients;
    private VectFunction output;

    public FitTensorCaminoNLLS withGradients(Gradients g)
    {
        this.gradients = g;
        this.output = null;

        return this;
    }

    public FitTensorCaminoNLLS withClamp(double d)
    {
        this.clamp = d;
        this.output = null;

        return this;
    }

    public FitTensorCaminoNLLS run()
    {
        final Gradients fgradients = this.gradients;

        this.output = new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                DW_Scheme scheme = CaminoUtils.scheme(fgradients);
                double[] voxel = input.toArray();
                NonLinearDT_Inversion inv = new NonLinearDT_Inversion(scheme, DEFAULT_MODEL);
                double[] soln = inv.invert(voxel);

                if (soln.length != 8)
                {
                    throw new RuntimeException("bug invalid output");
                }

                double lns0 = soln[1];
                double dxx = soln[2];
                double dxy = soln[3];
                double dxz = soln[4];
                double dyy = soln[5];
                double dyz = soln[6];
                double dzz = soln[7];

                output.set(Tensor.DT_S0, Math.exp(lns0));
                output.set(Tensor.DT_XX, dxx);
                output.set(Tensor.DT_YY, dyy);
                output.set(Tensor.DT_ZZ, dzz);
                output.set(Tensor.DT_XY, dxy);
                output.set(Tensor.DT_XZ, dxz);
                output.set(Tensor.DT_YZ, dyz);
                
                if (FitTensorCaminoNLLS.this.clamp != null)
                {
                    Tensor tensor = new Tensor(output);
                    for (int i = 0; i < 3; i++)
                    {
                        double val = tensor.getVal(i);
                        if (val < FitTensorCaminoNLLS.this.clamp)
                        {
                            Logging.info("clamping eigenvalue " + i + " to " + FitTensorCaminoNLLS.this.clamp);
                            tensor.setVal(i, FitTensorCaminoNLLS.this.clamp);
                        }
                    }
                    output.set(tensor.getEncoding());
                }
            }
        }.init(this.gradients.size(), new Tensor().getEncodingSize());

        return this;
    }

    public VectFunction getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
