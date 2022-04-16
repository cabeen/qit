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

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Volume;
import qit.math.utils.expression.ScalarExpression;

import java.math.BigDecimal;

@ModuleDescription("Evaluate an expression at each voxel of volume data")
@ModuleAuthor("Ryan Cabeen")
public class VolumeVoxelMathScalar implements Module
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

    @ModuleParameter
    @ModuleDescription("the expression to evaluate")
    public String expression = "a > 0.5";

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("use the specificed value for undefined output, e.g. NaN or Infinity")
    public double undefined = 0.0;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    public VolumeVoxelMathScalar run()
    {
        ScalarExpression e = new ScalarExpression(this.expression);

        int dim = this.a.getDim();

        if (this.b != null)
        {
            dim = Math.max(dim, this.b.getDim());
        }

        if (this.c != null)
        {
            dim = Math.max(dim, this.c.getDim());
        }

        if (this.d != null)
        {
            dim = Math.max(dim, this.d.getDim());
        }

        Volume out = this.a.proto(dim);

        for (Sample sample : this.a.getSampling())
        {
            for (int d = 0; d < dim; d++)
            {
                if (this.a.valid(sample, this.mask))
                {
                    try
                    {
                        e.with("i", new BigDecimal(sample.getI()));
                        e.with("j", new BigDecimal(sample.getJ()));
                        e.with("k", new BigDecimal(sample.getK()));
                        e.with("d", new BigDecimal(d));

                        e.with("a", new BigDecimal(this.a.get(sample, Math.min(d, this.a.getDim() - 1))));

                        if (this.b != null)
                        {
                            e.with("b", new BigDecimal(this.b.get(sample, Math.min(d, this.b.getDim() - 1))));
                        }

                        if (this.c != null)
                        {
                            e.with("c", new BigDecimal(this.c.get(sample, Math.min(d, this.c.getDim() - 1))));
                        }

                        if (this.d != null)
                        {
                            e.with("d", new BigDecimal(this.d.get(sample, Math.min(d, this.d.getDim() - 1))));
                        }

                        double value = e.eval().doubleValue();
                        out.set(sample, d, value);
                    }
                    catch (RuntimeException re)
                    {
                        out.set(sample, 0, this.undefined);
                    }
                }
            }
        }

        this.output = out;

        return this;
    }
}
