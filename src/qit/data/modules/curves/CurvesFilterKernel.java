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
import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

import java.util.List;
import java.util.Map;

@ModuleDescription("Filter curves with kernel regression.  This uses a non-parametric statistical approach to smooth the curves")
@ModuleAuthor("Ryan Cabeen")
public class CurvesFilterKernel implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("resample with a given vertex density (mm/vertex), otherwise the original arclength sampling is used ")
    public Double density = null;

    @ModuleParameter
    @ModuleDescription("use the given spatial bandwidth")
    public Double sigma = 4.0;

    @ModuleParameter
    @ModuleDescription("specify the order of the local approximating polynomial")
    public int order = 0;

    @ModuleParameter
    @ModuleDescription("the threshold for excluding data from local regression")
    public Double thresh = 0.05;

    @ModuleParameter
    @ModuleDescription("the attributes to smooth (comma separated list)")
    public String attrs = "coord";

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @Override
    public CurvesFilterKernel run()
    {
        Curves in = this.input;
        Curves out = new Curves();

        List<String> names = Lists.newArrayList();
        for (String name : this.attrs.split(","))
        {
            if (in.has(name))
            {
                names.add(name);
            }
        }

        for (Curves.Curve curve : in)
        {
            double length = curve.length();
            Vect lengths = curve.cumlength();
            Vect gamma = lengths;

            Map<String, VectFunction> functions = Maps.newHashMap();
            for (String attr : names)
            {
                Vects vs = curve.getAll(attr);
                VectFunction function = VectFunctionSource.kernel(this.sigma, VectsSource.create1D(gamma), vs, this.order, this.thresh);
                functions.put(attr, function);
            }

            if (this.density != null)
            {
                int verts = (int) Math.ceil(length / this.density);
                gamma = VectSource.linspace(0f, length, verts);
            }

            Curves.Curve ncurve = out.add(gamma.size());
            for (String name : in.names())
            {
                if (functions.keySet().contains(name))
                {
                    VectFunction function = functions.get(name);
                    for (int i = 0; i < gamma.size(); i++)
                    {
                        ncurve.set(name, i, function.apply(VectSource.create1D(gamma.get(i))));
                    }
                }
                else
                {
                    for (int i = 0; i < gamma.size(); i++)
                    {
                        double gv = gamma.get(i);

                        for (int j = 0; j < lengths.size() - 1; j++)
                        {
                            if (gv <= lengths.get(j) && gv <= lengths.get(j + 1))
                            {
                                ncurve.set(name, i, curve.get(name, j));
                                break;
                            }
                        }
                    }
                }

            }
        }

        this.output = out;

        return this;
    }
}
