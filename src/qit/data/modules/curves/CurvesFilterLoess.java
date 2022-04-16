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
import qit.base.annot.*;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

import java.util.function.Function;

@ModuleDescription("Filter curves with lowess smoothing")
@ModuleAuthor("Ryan Cabeen")
public class CurvesFilterLoess implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("resample with a given vertex density (mm/vertex), otherwise the original arclength sampling is used ")
    public Double density = null;

    @ModuleParameter
    @ModuleDescription("use the given neighborhood size for local estimation")
    public int num = 5;

    @ModuleParameter
    @ModuleDescription("use a given polynomial order for local estimation")
    public Integer order = 2;

    @ModuleParameter
    @ModuleDescription("smooth endpoints")
    public boolean endpoints = false;

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @Override
    public CurvesFilterLoess run()
    {
        Curves in = this.input;
        Curves out = new Curves();

        for (int idx = 0; idx < in.size(); idx++)
        {
            Curves.Curve curve = in.get(idx);

            double length = curve.length();
            Vect lengths = curve.cumlength();
            Vects gamma = VectsSource.create1D(lengths.times(1.0 / length));
            Vects pos = curve.getAll();

            VectFunction function = VectFunctionSource.lowess(gamma, pos, this.num, this.order);

            if (this.density != null)
            {
                int nverts = (int) Math.ceil(length / this.density);
                gamma = VectsSource.create1D(VectSource.linspace(0f, 1f, nverts));
            }

            Curves.Curve ncurve = out.add(gamma.size());

            if (this.endpoints)
            {
                for (int i = 0; i < gamma.size(); i++)
                {
                    ncurve.set(i, function.apply(gamma.get(i)));
                }
            }
            else
            {
                ncurve.set(0, curve.get(0));
                for (int i = 1; i < gamma.size() - 1; i++)
                {
                    ncurve.set(i, function.apply(gamma.get(i)));
                }
                ncurve.set(gamma.size() - 1, curve.get(curve.size() - 1));
            }
        }

        this.output = out;

        return this;
    }

    public static Curves apply(Curves curves)
    {
        return new CurvesFilterLoess()
        {{
            this.input = curves;
        }}.run().output;
    }
}
