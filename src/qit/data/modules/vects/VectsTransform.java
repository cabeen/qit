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

package qit.data.modules.vects;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Deformation;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.VectsUtils;
import qit.data.utils.VolumeUtils;
import qit.math.source.VectFunctionSource;

import java.util.List;

@ModuleDescription("Transform vects.  (note: the order of operations is transpose, flip, swap, perm, affine)")
@ModuleAuthor("Ryan Cabeen")
public class VectsTransform implements Module
{
    @ModuleInput
    @ModuleDescription("input vects")
    public Vects input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an affine xfm")
    public Affine affine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an inverse affine xfm")
    public Affine invaffine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply a deformation xfm")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("reverse the order, i.e. compose the affine(deform(x)), whereas the default is deform(affine(x))")
    public boolean reverse = false;

    @ModuleParameter
    @ModuleDescription("force rows > cols")
    public boolean rows = false;

    @ModuleParameter
    @ModuleDescription("force cols > rows")
    public boolean cols = false;

    @ModuleParameter
    @ModuleDescription("transpose")
    public boolean transpose = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("which the coordinates")
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

    @ModuleParameter
    @ModuleDescription("negative the vectors")
    public boolean negate = false;

    @ModuleParameter
    @ModuleDescription("normalize")
    public boolean normalize = false;

    @ModuleOutput
    @ModuleDescription("output transformed vects")
    public Vects output;

    @Override
    public VectsTransform run()
    {
        Vects vects = this.input;

        if (this.rows && vects.size() < vects.getDim())
        {
            vects = VectsUtils.transpose(vects);
        }

        if (this.cols && vects.size() > vects.getDim())
        {
            vects = VectsUtils.transpose(vects);
        }

        if (this.transpose)
        {
            vects = VectsUtils.transpose(vects);
        }

        if (this.subset != null)
        {
            List<Integer> which = Lists.newArrayList();
            for (String token : this.subset.split("[,\\s]+"))
            {
                which.add(Integer.valueOf(token));
            }

            vects = VectsUtils.sub(vects, which);
        }

        if (this.flip != null)
        {
            String lowflips = this.flip.toLowerCase();
            for (int i = 0; i < lowflips.length(); i++)
            {
                String lowflip = lowflips.substring(i, i+1);
                if (lowflip.equals("x") || lowflip.equals("i"))
                {
                    vects = VectsUtils.apply(vects, VectFunctionSource.scale(-1.0, 1.0, 1.0));
                }
                else if (lowflip.equals("y") || lowflip.equals("j"))
                {
                    vects = VectsUtils.apply(vects, VectFunctionSource.scale(1.0, -1.0, 1.0));
                }
                else if (lowflip.equals("z") || lowflip.equals("k"))
                {
                    vects = VectsUtils.apply(vects, VectFunctionSource.scale(1.0, 1.0, -1.0));
                }
                else
                {
                    Logging.error("unrecognized flip: " + this.flip);
                }
            }
        }

        if (this.swap != null)
        {
            String lowswap = this.swap.toLowerCase();
            if (lowswap.equals("xy") || lowswap.equals("yx"))
            {
                vects = VectsUtils.apply(vects, VectFunctionSource.swap(0, 1, 3));
            }
            else if (lowswap.equals("yz") || lowswap.equals("zy"))
            {
                vects = VectsUtils.apply(vects, VectFunctionSource.swap(1, 2, 3));
            }
            else if (lowswap.equals("xz") || lowswap.equals("zx"))
            {
                vects = VectsUtils.apply(vects, VectFunctionSource.swap(0, 2, 3));
            }
            else
            {
                Logging.error("unrecognized swap: " + this.swap);
            }
        }

        if (this.perm != null)
        {
            String[] sidx = this.perm.split(",");
            int[] pidx = new int[sidx.length];
            for (int i = 0; i < pidx.length; i++)
            {
                int idx = Integer.valueOf(sidx[i]);
                Global.assume(idx >= 0 && idx < vects.getDim(), "invalid index: " + idx);
                pidx[i] = idx;
            }

            Vects nvects = new Vects();
            for (Vect vect : vects)
            {
                Vect nvect = new Vect(pidx.length);
                for (int i = 0; i < pidx.length; i++)
                {
                    double nval = vect.get(pidx[i]);
                    nvect.set(i, nval);
                }
                nvects.add(nvect);
            }
            vects = nvects;
        }

        if (this.affine != null || this.invaffine != null || this.deform != null)
        {
            vects = VectsUtils.apply(vects, VolumeUtils.xfm(this.affine, this.invaffine, this.deform, this.reverse));
        }

        if (this.normalize)
        {
            vects = VectsUtils.normalize(vects);
        }

        if (this.negate)
        {
            for (Vect v : vects)
            {
                v.timesEquals(-1.0);
            }

        }

        this.output = vects;

        return this;
    }
}
