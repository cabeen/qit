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


package qit.data.modules.mri.noddi;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Volume;
import qit.data.utils.MatrixUtils;
import qit.data.models.Noddi;

@ModuleDescription("Extract ABTIN (ABsolute TIssue density from NODDI) parameters from a noddi volume.  Port from: https://github.com/sepehrband/ABTIN/blob/master/ABTIN.m")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Sepehrband, F., Clark, K. A., Ullmann, J. F.P., Kurniawan, N. D., Leanage, G., Reutens, D. C. and Yang, Z. (2015), Brain tissue compartment density estimated using diffusion-weighted MRI yields tissue parameters consistent with histology. Hum. Brain Mapp.. doi: 10.1002/hbm.22872 Link: http://onlinelibrary.wiley.com/doi/10.1002/hbm.22872/abstract")
public class VolumeNoddiAbtin implements Module
{
    @ModuleInput
    @ModuleDescription("input noddi volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the alpha parameter (see theory section of paper)")
    public double alpha = 25;

    @ModuleParameter
    @ModuleDescription("the alpha parameter (see theory section of paper)")
    public double gratio = 0.7;

    @ModuleOutput
    @ModuleDescription("output myelin density volume")
    public Volume outputMylDen;

    @ModuleOutput
    @ModuleDescription("output CSF density volume")
    public Volume outputCSFDen;

    @ModuleOutput
    @ModuleDescription("output fiber density volume")
    public Volume outputFibDen;

    @ModuleOutput
    @ModuleDescription("output cellular density volume")
    public Volume outputCelDen;

    @Override
    public VolumeNoddiAbtin run()
    {
        double beta = 1.0 / ((1.0 / (this.gratio * this.gratio)) - 1);

        Volume outMylDen = this.input.proto(1);
        Volume outCSFDen = this.input.proto(1);
        Volume outFibDen = this.input.proto(1);
        Volume outCelDen = this.input.proto(1);

        for (Sample sample : this.input.getSampling())
        {
            if (this.input.valid(sample, this.mask))
            {
                Noddi noddi = new Noddi(this.input.get(sample));
                double fin = noddi.getFICVF();
                double fen = noddi.getFECVF();
                double fcsf = noddi.getFISO();

                Matrix A = new Matrix(3,3);
                A.set(0, 0, beta + fin);
                A.set(0, 1, fin);
                A.set(0, 2, 0);
                A.set(1, 0, fen);
                A.set(1, 1, this.alpha + fen);
                A.set(1, 2, 0);
                A.set(2, 0, fcsf);
                A.set(2, 1, fcsf);
                A.set(2, 2, 1);

                Matrix B = new Matrix(3, 1);
                B.set(0, 0, fin);
                B.set(1, 0, fen);
                B.set(2, 0, fcsf);

                Matrix soln = MatrixUtils.solve(A, B);

                double vm = soln.get(0, 0);
                double vbc = soln.get(1, 0);
                double vcsf = soln.get(2, 0);

                double vn = beta * vm;
                double vc = this.alpha * vbc;

                double vcel = vc + vbc;
                double vfib = vn + vm;

                outMylDen.set(sample, vm);
                outCSFDen.set(sample, vcsf);
                outFibDen.set(sample, vfib);
                outCelDen.set(sample, vcel);
            }
        }

        this.outputMylDen = outMylDen;
        this.outputCSFDen = outCSFDen;
        this.outputFibDen = outFibDen;
        this.outputCelDen = outCelDen;

        return this;
    }
}
