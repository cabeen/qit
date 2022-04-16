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

package qit.data.modules.curves;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.data.datasets.Affine;
import qit.data.datasets.Curves;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.utils.vects.register.VectsRegisterAffineLeastSquares;

@ModuleDescription("Make a left/right pair of curves symmetric")
@ModuleAuthor("Ryan Cabeen")
public class CurvesSymmetrize implements Module
{
    @ModuleInput
    @ModuleDescription("input left curves")
    public Curves inputLeft;

    @ModuleInput
    @ModuleDescription("input right curves")
    public Curves inputRight;

    @ModuleInput
    @ModuleDescription("input reference")
    public Volume reference;

    @ModuleOutput
    @ModuleDescription("output left curves")
    public Curves outputLeft;

    @ModuleOutput
    @ModuleDescription("output right curves")
    public Curves outputRight;

    @Override
    public CurvesSymmetrize run()
    {
        Vects source = new Vects();
        Vects dest = new Vects();

        Sampling sampling = this.reference.getSampling();
        int ni = sampling.numI();
        int nj = sampling.numI();
        int nk = sampling.numI();

        Vect p000 = sampling.world(0, 0, 0);
        Vect p001 = sampling.world(0, 0, nk);
        Vect p010 = sampling.world(0, nj, 0);
        Vect p100 = sampling.world(ni, 0, 0);
        Vect p011 = sampling.world(0, nj, nk);
        Vect p110 = sampling.world(nj, nj, 0);
        Vect p101 = sampling.world(ni, 0, nk);
        Vect p111 = sampling.world(ni, nj, nk);

        source.add(p000); dest.add(p100);
        source.add(p001); dest.add(p101);
        source.add(p010); dest.add(p110);
        source.add(p011); dest.add(p111);
        source.add(p100); dest.add(p000);
        source.add(p101); dest.add(p001);
        source.add(p110); dest.add(p010);
        source.add(p111); dest.add(p011);

        Affine xfm = VectsRegisterAffineLeastSquares.estimate(source, dest);
        Curves flipLeft = CurvesTransform.apply(this.inputLeft, xfm);
        Curves flipRight = CurvesTransform.apply(this.inputRight, xfm);

        this.outputLeft = this.inputLeft.copy();
        this.outputLeft.add(flipRight);

        this.outputRight = this.inputRight.copy();
        this.outputRight.add(flipLeft);

        return this;
    }
}
