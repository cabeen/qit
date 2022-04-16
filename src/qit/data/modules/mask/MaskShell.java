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

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.utils.MaskUtils;

@ModuleDescription("Compute the shell of a mask (the voxels at the boundary of the mesh)")
@ModuleAuthor("Ryan Cabeen")
public class MaskShell implements Module
{
    public enum ShellMode { Inner, Outer, Multi }

    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleParameter
    @ModuleDescription("dilate this many times")
    public int num = 1;

    @ModuleParameter
    @ModuleDescription("specify a mode for computing the shell")
    public ShellMode mode = ShellMode.Inner;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskShell run()
    {
        if (this.mode == ShellMode.Outer)
        {
            MaskDilate dilater = new MaskDilate();
            dilater.input = this.input;
            dilater.num = this.num;
            Mask dilate = dilater.run().output;

            MaskInvert inverter = new MaskInvert();
            inverter.input = this.input;
            Mask invert = inverter.run().output;

            MaskIntersection intersector = new MaskIntersection();
            intersector.left = dilate;
            intersector.right = invert;
            Mask out = intersector.run().output;

            this.output = out;
        }
        else if (this.mode == ShellMode.Inner)
        {
            MaskErode eroder = new MaskErode();
            eroder.input = this.input;
            eroder.num = this.num;
            Mask erode = eroder.run().output;

            MaskInvert inverter = new MaskInvert();
            inverter.input = erode;
            Mask invert = inverter.run().output;

            MaskIntersection intersector = new MaskIntersection();
            intersector.left = this.input;
            intersector.right = invert;
            Mask out = intersector.run().output;

            this.output = out;
        }
        else
        {
            Mask out = this.input.proto();

            for (int label : MaskUtils.listNonzero(this.input))
            {
                Mask region = MaskUtils.equal(this.input, label);
                MaskErode eroder = new MaskErode();
                eroder.input = region;
                eroder.num = this.num;
                Mask erode = eroder.run().output;

                MaskInvert inverter = new MaskInvert();
                inverter.input = erode;
                Mask invert = inverter.run().output;

                MaskIntersection intersector = new MaskIntersection();
                intersector.left = region;
                intersector.right = invert;
                Mask shell = intersector.run().output;

                out.setAll(shell, label);
            }

            this.output = out;
        }

        return this;
    }

    public static Mask apply(Mask mask)
    {
        return new MaskShell()
        {{
            this.input = mask;
        }}.run().output;
    }

    public static Mask apply(Mask mask, int mynum)
    {
        return new MaskShell()
        {{
            this.input = mask;
            this.num = mynum;
        }}.run().output;
    }
}