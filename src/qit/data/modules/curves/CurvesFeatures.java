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

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.structs.Box;
import qit.math.utils.MathUtils;

@ModuleDescription("Compute features of curves and add them as vertex attributes")
@ModuleAuthor("Ryan Cabeen")
public class CurvesFeatures implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("compute tangents")
    public boolean tangent = false;

    @ModuleParameter
    @ModuleDescription("compute vertex colors")
    public boolean color = false;

    @ModuleParameter
    @ModuleDescription("compute per-vertex arclength")
    public boolean arclength = false;

    @ModuleParameter
    @ModuleDescription("compute per-vertex index")
    public boolean index = false;

    @ModuleParameter
    @ModuleDescription("compute per-vertex fraction along curve")
    public boolean fraction = false;

    @ModuleParameter
    @ModuleDescription("compute per-curve vertex count")
    public boolean count = false;

    @ModuleParameter
    @ModuleDescription("compute per-curve length")
    public boolean length = false;

    @ModuleParameter
    @ModuleDescription("compute the per-vertex frenet frame")
    public boolean frame = false;

    @ModuleParameter
    @ModuleDescription("compute the per-vertex curvatures")
    public boolean curvature = false;

    @ModuleParameter
    @ModuleDescription("compute the per-vertex density")
    public boolean density = false;

    @ModuleParameter
    @ModuleDescription("compute the per-curve statistics of vertex curvature and density")
    public boolean stats = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("compute all possible features")
    public boolean all = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify a voxel size used for computing density")
    public double voxel = 1.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    @Override
    public CurvesFeatures run()
    {
        Curves curves = this.inplace ? this.input : this.input.copy();

        if (this.density && curves.size() > 0)
        {
            Box box = curves.bounds();
            Mask ref = MaskSource.create(box, this.voxel);

            Volume density = new CurvesDensity()
            {{
                this.input = curves;
                this.reference = ref.copyVolume();
                this.run();
            }}.output;

            new CurvesSample()
            {{
                this.input = curves;
                this.attr = Curves.DENSITY;
                this.volume = density;
                this.inplace = true;
                this.run();
            }};
        }

        if (this.all || this.tangent)
        {
            CurvesUtils.attrSetTangent(curves);
        }

        if (this.all || this.color)
        {
            CurvesUtils.attrSetColor(curves);
        }

        if (this.all || this.arclength)
        {
            CurvesUtils.attrSetArclength(curves);
        }

        if (this.all || this.index)
        {
            CurvesUtils.attrSetIndex(curves);
        }

        if (this.all || this.fraction)
        {
            CurvesUtils.attrSetFraction(curves);
        }

        // second order approximations
        // see "Asymptotic Analysis of Discrete Normals and Curvatures of Polylines" by Langer et al

        for (Curves.Curve curve : curves)
        {
            if (curve.size() < 2)
            {
                Logging.info("warning: skipping curve features");
                continue;
            }

            // pad the beginning and end for five-point approximation
            Vects points = new Vects();

            int num = curve.size();
            Vect deltaHead = curve.get(1).minus(curve.get(0));
            Vect deltaTail = curve.get(num - 1).minus(curve.get(num - 2));

            points.add(curve.get(0).minus(deltaHead));
            for (int i = 0; i < curve.size(); i++)
            {
                points.add(curve.get(i));
            }
            points.add(curve.get(num - 1).plus(deltaTail));

            VectOnlineStats curvStats = new VectOnlineStats();
            VectOnlineStats densityStats = new VectOnlineStats();
            for (int i = 0; i < curve.size(); i++)
            {
                if (this.all || this.count)
                {
                    curve.setAll(Curves.COUNT, VectSource.create1D(curve.size()));
                }

                if (this.all || this.length)
                {
                    curve.setAll(Curves.LENGTH, VectSource.create1D(curve.length()));
                }

                Vect prev = points.get(i);
                Vect curr = points.get(i + 1);
                Vect next = points.get(i + 2);

                Vect ev = next.minus(curr);
                Vect dv = prev.minus(curr);

                double d = dv.norm();
                double e = ev.norm();

                if (MathUtils.zero(d) || MathUtils.zero(e))
                {
                    Logging.info("warning: undefined curvature");
                    continue;
                }

                double d2 = d * d;
                double e2 = e * e;
                double de = d * e;
                double dpe = d + e;

                Vect binormal = ev.cross(dv).normalize();
                Vect tangent = dv.times(1.0 / d2).plus(ev.times(1.0 / e2)).times(de / dpe);
                Vect vcurv = ev.times(1.0 / d).plus(dv.times(1.0 / e)).times(2.0 / dpe);
                Vect normal = vcurv.normalize();
                double curv = vcurv.norm();

                curvStats.update(curv);

                if (this.density)
                {
                    densityStats.update(curve.get(Curves.DENSITY, i).get(0));
                }

                if (this.all || this.frame)
                {
                    curve.set(Curves.TANGENT, i, tangent);
                }

                if (this.all || this.frame)
                {
                    curve.set(Curves.NORMAL, i, normal);
                }

                if (this.all || this.frame)
                {
                    curve.set(Curves.BINORMAL, i, binormal);
                }

                if (this.all || this.curvature)
                {
                    curve.set(Curves.CURVATURE, i, VectSource.create1D(curv));
                }
            }

            if (this.all || this.stats)
            {
                if (this.curvature)
                {
                    curve.setAll(Curves.CURVATURE_MIN, VectSource.create1D(curvStats.min));
                    curve.setAll(Curves.CURVATURE_MAX, VectSource.create1D(curvStats.max));
                    curve.setAll(Curves.CURVATURE_VAR, VectSource.create1D(curvStats.var));
                    curve.setAll(Curves.CURVATURE_STD, VectSource.create1D(curvStats.std));
                    curve.setAll(Curves.CURVATURE_MEAN, VectSource.create1D(curvStats.mean));
                    curve.setAll(Curves.CURVATURE_SUM, VectSource.create1D(curvStats.sum));
                }

                if (this.density)
                {
                    curve.setAll(Curves.DENSITY_MIN, VectSource.create1D(densityStats.min));
                    curve.setAll(Curves.DENSITY_MAX, VectSource.create1D(densityStats.max));
                    curve.setAll(Curves.DENSITY_VAR, VectSource.create1D(densityStats.var));
                    curve.setAll(Curves.DENSITY_STD, VectSource.create1D(densityStats.std));
                    curve.setAll(Curves.DENSITY_MEAN, VectSource.create1D(densityStats.mean));
                    curve.setAll(Curves.DENSITY_SUM, VectSource.create1D(densityStats.sum));
                }
            }
        }

        this.output = curves;

        return this;
    }
}
