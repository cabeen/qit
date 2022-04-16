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

package qit.data.modules.mask;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Mask;

@ModuleDescription("Change the ordering of voxels in a mask.  You can flip and shift the voxels.  Which shifting outside the field a view, the data is wrapped around.  Shifting is applied before flipping")
@ModuleAuthor("Ryan Cabeen")
public class MaskReorder implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    private Mask input;

    @ModuleParameter
    @ModuleDescription("flip in i")
    private boolean flipi;

    @ModuleParameter
    @ModuleDescription("flip in j")
    private boolean flipj;

    @ModuleParameter
    @ModuleDescription("flip in k")
    private boolean flipk;

    @ModuleParameter
    @ModuleDescription("shift in i")
    private int shifti = 0;

    @ModuleParameter
    @ModuleDescription("shift in j")
    private int shiftj = 0;

    @ModuleParameter
    @ModuleDescription("shift in k")
    private int shiftk = 0;

    @ModuleOutput
    @ModuleDescription("output mask")
    private Mask output;
    
    public MaskReorder run()
    {
        Mask out = this.input.copy();
        Sampling ref = this.input.getSampling();

        int numi = ref.numI();
        int numj = ref.numJ();
        int numk = ref.numK();

        if (this.shifti != 0 || this.shiftj != 0 || this.shiftk != 0)
        {
            Logging.info("shifting voxels");
            Mask tmp = out.copy();
            for (Sample sample : ref)
            {
                int si = (sample.getI() + this.shifti) % numi;
                int sj = (sample.getJ() + this.shiftj) % numj;
                int sk = (sample.getK() + this.shiftk) % numk;

                out.set(sample, tmp.get(si, sj, sk));
            }
        }

        if (this.flipi || this.flipj || this.flipk)
        {
            Logging.info("flipping voxels");
            Mask tmp = out.copy();
            for (Sample sample : ref)
            {
                int ni = this.flipi ? numi - 1 - sample.getI() : sample.getI();
                int nj = this.flipj ? numj - 1 - sample.getJ() : sample.getJ();
                int nk = this.flipk ? numk - 1 - sample.getK() : sample.getK();

                out.set(ni, nj, nk, tmp.get(sample));
            }
        }

        this.output = out;
        
        return this;
    }

    public static Mask flipi(Mask mask)
    {
        MaskReorder run = new MaskReorder();
        run.input = mask;
        run.flipi = true;
        return run.run().output;
    }

    public static Mask flipj(Mask mask)
    {
        MaskReorder run = new MaskReorder();
        run.input = mask;
        run.flipj = true;
        return run.run().output;
    }

    public static Mask flipk(Mask mask)
    {
        MaskReorder run = new MaskReorder();
        run.input = mask;
        run.flipk = true;
        return run.run().output;
    }
}
