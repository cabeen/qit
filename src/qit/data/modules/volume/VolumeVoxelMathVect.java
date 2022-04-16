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

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.math.utils.expression.VectExpression;

@ModuleDescription("Evaluate an expression at each voxel of volume data")
@ModuleAuthor("Ryan Cabeen")
public class VolumeVoxelMathVect implements Module
{
    @ModuleInput
    @ModuleDescription("the input volume stored as a variable named 'a'")
    public Volume a;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input volume stored as a variable named 'b'")
    public Volume b;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input volume stored as a variable named 'c'")
    public Volume c;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input volume stored as a variable named 'd'")
    public Volume d;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("add a variable for world coordinates of the given voxel")
    public boolean world;

    @ModuleParameter
    @ModuleDescription("the expression to evaluate")
    public String expression = "mean(a)";

    @ModuleParameter
    @ModuleDescription("use the specified value for undefined output, e.g. NaN or Infinity")
    public double undefined = 0.0;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    public VolumeVoxelMathVect run()
    {
        VectExpression e = new VectExpression(this.expression);

        Volume out = null;
        for (Sample sample : this.a.getSampling())
        {
            if (this.a.valid(sample, this.mask))
            {
                try
                {
                    if (this.world)
                    {
                        e.with("world", this.a.getSampling().world(sample));
                    }

                    e.with("a", this.a.get(sample));

                    if (this.b != null)
                    {
                        e.with("b", this.b.get(sample));
                    }

                    if (this.c != null)
                    {
                        e.with("c", this.c.get(sample));
                    }

                    if (this.d != null)
                    {
                        e.with("d", this.d.get(sample));
                    }

                    Vect value = e.eval();

                    if (out == null)
                    {
                        out = this.a.proto(value.size());
                    }

                    out.set(sample, value);
                }
                catch (RuntimeException re)
                {
                    re.printStackTrace();
                    out.set(sample, this.undefined);
                }
            }
        }

        this.output = out;

        return this;
    }
}
