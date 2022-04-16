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
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

@ModuleDescription("Filter cures with polynomial splines")
@ModuleAuthor("Ryan Cabeen")
public class CurvesFilterPolynomial implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("resample with a given vertex density (mm/vertex), otherwise the original arclength sampling is used ")
    public Double density = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use regularization Tikhonov regularization (specify the negative log, so and input of 2.3 would give a regularization weight of 0.1)")
    public Double lambda = null;

    @ModuleParameter
    @ModuleDescription("use a given polynomial order")
    public Integer order = 10;

    @ModuleParameter
    @ModuleDescription("save the residuals")
    public boolean residual = false;

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output residual error between the polynomial and the curve")
    public Vects outputResiduals;

    @Override
    public CurvesFilterPolynomial run()
    {
        Curves in = this.residual ? this.input.copy() : this.input;
        Curves out = new Curves();
        Vects residuals = new Vects();

        for (Curves.Curve curve : in)
        {
            double length = curve.length();
            Vect lengths = curve.cumlength();
            Vects gamma = VectsSource.create1D(lengths.times(1.0 / length));
            Vects pos = curve.getAll();

            VectFunction function = VectFunctionSource.polynomial(gamma, pos, this.order, this.lambda);

            if (this.density != null)
            {
                int verts = (int) Math.ceil(length / this.density);
                gamma = VectsSource.create1D(VectSource.linspace(0f, 1f, verts));
            }

            Curves.Curve ncurve = out.add(gamma.size());
            for (int i = 0; i < gamma.size(); i++)
            {
                ncurve.set(i, function.apply(gamma.get(i)));
            }

            if (this.residual)
            {
                curve.resample(gamma.flatten());

                VectOnlineStats stats = new VectOnlineStats();
                for (int i = 0; i < gamma.size(); i++)
                {
                    Vect p = pos.get(i);
                    Vect f = ncurve.get(i);

                    double residual = p.dist(f);

                    ncurve.set("residual_point", i, VectSource.create1D(residual));
                    stats.update(residual);
                }

                for (int i = 0; i < gamma.size(); i++)
                {
                    ncurve.set("residual_mean", i, VectSource.create1D(stats.mean));
                    ncurve.set("residual_var", i, VectSource.create1D(stats.var));
                    ncurve.set("residual_max", i, VectSource.create1D(stats.max));
                    ncurve.set("residual_std", i, VectSource.create1D(stats.std));
                }

                residuals.add(VectSource.create1D(stats.mean));
            }
        }

        this.output = out;

        if (this.residual)
        {
            this.outputResiduals = residuals;
        }

        return this;
    }
}
