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

import com.google.common.collect.Lists;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;

import java.util.List;

@ModuleDescription("Crop curves to retain only portions inside the selection")
@ModuleAuthor("Ryan Cabeen")
public class CurvesCrop implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("some solids")
    public Solids solids;

    @ModuleParameter
    @ModuleDescription("invert the selection")
    public boolean invert;

    @ModuleParameter
    @ModuleDescription("require that curves are inside all solids (logical AND)")
    public boolean and;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify an attribute for cropping")
    public String attr;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("require that the given attribute at each vertex is above this value")
    public Double above;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("require that the given attribute at each vertex is below this value")
    public Double below;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("require that the given attribute approximately equals this value")
    public Double equals;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the threshold distance for approximate cropping")
    public Double delta = 1e-3;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    public CurvesCrop run()
    {
        Curves out = new Curves();

        for (String attr : this.input.names())
        {
            out.add(attr, this.input.proto(attr));
        }

        for (Curves.Curve curve : this.input)
        {
            List<Pair<Integer, Integer>> pairs = Lists.newArrayList();
            {
                Integer start = null;
                Integer prev = null;

                for (int i = 0; i < curve.size(); i++)
                {
                    boolean pass = pass(curve.get(i));

                    if (this.attr != null && (this.above != null || this.below != null || this.equals != null))
                    {
                        double value = curve.get(this.attr, i).get(0);

                        if (this.above != null)
                        {
                            pass &= value > this.above;
                        }

                        if (this.below != null)
                        {
                            pass &= value < this.below;
                        }

                        if (this.equals != null)
                        {
                            pass &= Math.abs(value - this.equals) < this.delta;
                        }
                    }

                    if (pass && start == null)
                    {
                        // initialize
                        start = i;
                    }
                    else if (!pass && start != null && prev != null && prev > start)
                    {
                        // finish a segment
                        pairs.add(Pair.of(start, prev));
                        start = null;
                    }

                    prev = i;
                }

                if (start != null && prev != null && prev > start)
                {
                    pairs.add(Pair.of(start, prev));
                }
            }

            for (Pair<Integer, Integer> pair : pairs)
            {
                int start = pair.a;
                int end = pair.b + 1; // last is exclusive
                int count = end - start;

                Curves.Curve ncurve = out.add(count);
                for (int i = 0; i < count; i++)
                {
                    int cidx = start + i;
                    ncurve.set(i, curve.get(cidx));

                    for (String attr : this.input.names())
                    {
                        ncurve.set(attr, i, curve.get(attr, cidx));
                    }
                }

                out.add(ncurve);
            }
        }

        this.output = out;
        return this;
    }

    private boolean pass(Vect p)
    {
        boolean pass = true;

        if (this.mask != null)
        {
            Sample nearest = this.mask.getSampling().nearest(p);
            boolean valid = this.mask.getSampling().contains(nearest);
            boolean inside = valid && this.mask.foreground(nearest);
            pass = this.invert ? !inside : inside;
        }

        if (this.solids != null)
        {
            if (this.and)
            {
                for (int i = 0; i < this.solids.size(); i++)
                {
                    boolean inside = this.solids.get(i).contains(p);
                    pass &= this.invert ? !inside : inside;
                }
            }
            else
            {
                boolean inside = this.solids.contains(p);
                pass &= this.invert ? !inside : inside;
            }
        }

        return pass;
    }
}
