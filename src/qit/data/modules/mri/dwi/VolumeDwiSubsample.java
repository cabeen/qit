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

package qit.data.modules.mri.dwi;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Subsample a mri weighted volume by removing channels to maximize the separation of gradient directions.  This is currently optimized for single shell data")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiSubsample implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume indwi;

    @ModuleInput
    @ModuleDescription("the input gradients")
    public Gradients ingrad;

    @ModuleParameter
    @ModuleDescription("the number of baseline channels to keep")
    public int nums = 1;

    @ModuleParameter
    @ModuleDescription("the number of diffusion-weighted channels to keep")
    public int numd = 12;

    @ModuleOutput
    @ModuleDescription("the output diffusion-weighted MR volume")
    public Volume outdwi;

    @ModuleOutput
    @ModuleDescription("the output gradients")
    public Gradients outgrad;

    public VolumeDwiSubsample run()
    {
        List<Integer> subset = Lists.newArrayList();
        Vects bvecs = new Vects();
        Vects bvals = new Vects();

        // greedily select gradients to keep
        for (int idx : this.ingrad.getBaselineIdx())
        {
            if (bvecs.size() >= this.nums)
            {
                break;
            }

            bvecs.add(this.ingrad.getBvec(idx));
            bvals.add(VectSource.create1D(this.ingrad.getBval(idx)));
            subset.add(idx);
        }

        List<Integer> kept = Lists.newArrayList();
        List<Integer> left = Lists.newArrayList();
        left.addAll(this.ingrad.getDvecIdx());
        while (kept.size() < this.numd && left.size() > 0)
        {
            if (kept.size() == 0)
            {
                int ridx = left.remove(Global.RANDOM.nextInt(left.size()));
                kept.add(ridx);
            }
            else
            {
                // find the dir that maximizes the sum squared distance
                double[] dists = new double[left.size()];
                for (int i = 0; i < left.size(); i++)
                {
                    Vect a = this.ingrad.getBvec(left.get(i));
                    for (int j = 0; j < kept.size(); j++)
                    {
                        Vect b = this.ingrad.getBvec(kept.get(j));
                        double dist = a.angleLineDeg(b);
                        dists[i] += dist;
                    }
                }
                int[] perm = MathUtils.permutation(dists);

                int kidx = left.remove(perm[perm.length - 1]);
                kept.add(kidx);
            }
        }

        for (int idx : kept)
        {
            bvecs.add(this.ingrad.getBvec(idx));
            bvals.add(VectSource.create1D(this.ingrad.getBval(idx)));
            subset.add(idx);
        }

        this.outgrad = new Gradients(bvecs, bvals);
        this.outdwi = VolumeUtils.subvolumes(this.indwi, subset);

        return this;
    }
}
