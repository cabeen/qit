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

import cern.colt.bitvector.QuickBitVector;
import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.structs.TriConsumer;
import qit.data.datasets.*;
import qit.data.modules.vects.VectsRegisterLinear;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.math.structs.Quaternion;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@ModuleDescription("Change the ordering of voxels in a volume.  You can flip and shift the voxels.  Which shifting outside the field a view, the data is wrapped around.  Shifting is applied before flipping")
@ModuleAuthor("Ryan Cabeen")
public class VolumeReorder implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    private Volume input;

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

    @ModuleParameter
    @ModuleDescription("swap ij")
    private boolean swapij;

    @ModuleParameter
    @ModuleDescription("swap ik")
    private boolean swapik;

    @ModuleParameter
    @ModuleDescription("swap jk")
    private boolean swapjk;

    @ModuleOutput
    @ModuleDescription("output volume")
    private Volume output;

    public VolumeReorder run()
    {
        Volume out = this.input.copy();

        if (this.shifti != 0 || this.shiftj != 0 || this.shiftk != 0)
        {
            Sampling ref = out.getSampling();
            int numi = ref.numI();
            int numj = ref.numJ();
            int numk = ref.numK();

            Logging.info("shifting voxels");
            Volume tmp = out.copy();
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
            Sampling ref = out.getSampling();
            int numi = ref.numI();
            int numj = ref.numJ();
            int numk = ref.numK();
            Volume tmp = out.copy();
            for (Sample sample : ref)
            {
                int ni = this.flipi ? numi - 1 - sample.getI() : sample.getI();
                int nj = this.flipj ? numj - 1 - sample.getJ() : sample.getJ();
                int nk = this.flipk ? numk - 1 - sample.getK() : sample.getK();

                out.set(ni, nj, nk, tmp.get(sample));
            }
        }

        BiConsumer<Vect, Vect> swap = (va, vb) ->
        {
            Vect tmp = vb.copy();
            vb.set(va);
            va.set(tmp);
        };

        if (this.swapij)
        {
            Sampling ref = out.getSampling();
            Integers num = new Integers(ref.numJ(), ref.numI(), ref.numK());
            Vect delta = VectSource.create3D(ref.deltaJ(), ref.deltaI(), ref.deltaK());
            Sampling nref = new Sampling(ref.start(), delta, ref.quat(), num);
            Volume tmp = out.copy();
            out = VolumeSource.create(nref, tmp.getDim());
            for (Sample sample : ref)
            {
                out.set(sample.getJ(), sample.getI(), sample.getK(), tmp.get(sample));
            }
        }

        if (this.swapik)
        {
            Sampling ref = out.getSampling();
            Integers num = new Integers(ref.numK(), ref.numJ(), ref.numI());
            Vect delta = VectSource.create3D(ref.deltaK(), ref.deltaJ(), ref.deltaI());
            Sampling nref = new Sampling(ref.start(), delta, ref.quat(), num);
            Volume tmp = out.copy();
            out = VolumeSource.create(nref, tmp.getDim());
            for (Sample sample : ref)
            {
                out.set(sample.getK(), sample.getJ(), sample.getI(), tmp.get(sample));
            }
        }

        if (this.swapjk)
        {
            Sampling ref = out.getSampling();
            Integers num = new Integers(ref.numI(), ref.numK(), ref.numJ());
            Vect delta = VectSource.create3D(ref.deltaI(), ref.deltaK(), ref.deltaJ());
            Sampling nref = new Sampling(ref.start(), delta, ref.quat(), num);
            Volume tmp = out.copy();
            out = VolumeSource.create(nref, tmp.getDim());
            for (Sample sample : ref)
            {
                out.set(sample.getI(), sample.getK(), sample.getJ(), tmp.get(sample));
            }
        }

        this.output = out;

        return this;
    }
}
