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

package qit.data.modules.mri.gradients;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Vects;
import qit.data.modules.vects.VectsMath;
import qit.data.modules.vects.VectsTransform;
import qit.data.utils.mri.structs.Gradients;

@ModuleDescription("Transform gradient directions (note: the order of operations is transpose, flip, swap, perm, affine)")
@ModuleAuthor("Ryan Cabeen")
public class GradientsTransform implements Module
{
    @ModuleInput
    @ModuleDescription("input gradients")
    public Gradients input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an affine transform to gradient directions (and normalize afterwards)")
    public Affine affine = null;

    @ModuleParameter
    @ModuleDescription("round the gradients magnitudes")
    public boolean round;

    @ModuleParameter
    @ModuleDescription("specify how coarsely to round the gradient magnitudes")
    public int rounder = 100;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("select a subset of gradients")
    public String subset = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("flip a coodinate (x, y, or z)")
    public String flip = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("swap a pair of coordinates (xy, xz, or yz)")
    public String swap = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("permute by coordinate index, e.g. 1,0,2")
    public String perm = null;

    @ModuleOutput
    @ModuleDescription("output transformed gradients")
    public Gradients output;

    @Override
    public GradientsTransform run()
    {
        Gradients gradients = this.input;

        if (this.round)
        {
            gradients = roundit(gradients, this.rounder);
        }

        Vects bvals = gradients.getBvals();
        Vects bvecs = gradients.getBvecs();

        VectsTransform transformer = new VectsTransform();
        transformer.input = bvecs;
        transformer.affine = this.affine;
        transformer.subset = this.subset;
        transformer.flip = this.flip;
        transformer.swap = this.swap;
        transformer.perm = this.perm;
        transformer.normalize = true;
        bvecs = transformer.run().output;

        this.output = new Gradients(bvecs, bvals);

        return this;
    }

    public static Gradients roundit(Gradients gradients, Integer rounder)
    {
        Vects bvals = gradients.getBvals();
        Vects bvecs = gradients.getBvecs();

        if (rounder != null)
        {
            VectsMath mather = new VectsMath();
            mather.input = bvals;
            mather.expression = String.format("%d.0 * round(v / %d.0)", rounder, rounder);
            bvals = mather.run().output;
        }

        return new Gradients(bvecs, bvals);
    }
}
