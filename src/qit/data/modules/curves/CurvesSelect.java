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
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Triple;
import qit.data.datasets.Curves;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.vects.stats.VectsStats;
import qit.data.source.VectSource;
import qit.data.utils.VectUtils;
import qit.data.utils.curves.CurvesSelector;
import qit.math.structs.Containable;
import qit.math.structs.Sphere;
import qit.math.source.SelectorSource;
import qit.math.utils.expression.VectExpression;

import java.util.List;

@ModuleDescription("Select a which of curves using a number of possible criteria")
@ModuleAuthor("Ryan Cabeen")
public class CurvesSelect implements Module
{
    public static final String EXP_LENGTH = "length";
    public static final String EXP_SIZE = "size";
    public static final String EXP_MIN = "min";
    public static final String EXP_MAX = "max";
    public static final String EXP_MEAN = "mean";
    public static final String EXP_SUM = "sum";

    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a deformation between curves and the masks")
    public Deformation deform = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("some solids")
    public Solids solids;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("some vects")
    public Vects vects;

    @ModuleParameter
    @ModuleDescription("vects radius")
    public double radius = 5;

    @ModuleParameter
    @ModuleDescription("use OR (instead of AND) to combine selections")
    public boolean or = false;

    @ModuleParameter
    @ModuleDescription("invert the selection after combining")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("exclude the selected curves")
    public boolean exclude = false;

    @ModuleParameter
    @ModuleDescription("select based on only curve endpoints")
    public boolean endpoints = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("select based on a boolean-valued expression using any of: length, size, min_attr, max_attr, mean_attr, or sum_attr")
    public String expression = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a minimum length")
    public Double minlen = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a maximum length")
    public Double maxlen = null;

    @ModuleParameter
    @ModuleDescription("select the longest curve")
    public boolean longest = false;

    @ModuleOutput
    @ModuleDescription("output selected curves")
    public Curves output;

    @Override
    public CurvesSelect run()
    {
        Containable selector = SelectorSource.constant(!this.or);

        if (this.mask != null)
        {
            Containable mselect = SelectorSource.mask(this.mask);
            if (this.or)
            {
                selector = SelectorSource.or(selector, mselect);
            }
            else
            {
                selector = SelectorSource.and(selector, mselect);
            }
        }

        if (this.solids != null)
        {
            if (this.or)
            {
                selector = SelectorSource.or(selector, this.solids);
            }
            else
            {
                selector = SelectorSource.and(selector, this.solids);
            }
        }

        if (this.vects != null)
        {
            Containable vselect = null;
            for (Vect vect : vects)
            {
                vselect = SelectorSource.or(vselect, new Sphere(vect, this.radius));
            }

            if (this.or)
            {
                selector = SelectorSource.or(selector, vselect);
            }
            else
            {
                selector = SelectorSource.and(selector, vselect);
            }
        }

        if (this.invert)
        {
            selector = SelectorSource.invert(selector);
        }

        CurvesSelector select = new CurvesSelector();
        select.withCurves(this.input);
        select.withSelector(selector);
        select.withExclude(this.exclude);
        select.withEndpoints(this.endpoints);
        select.withTransform(this.deform);
        Curves curves = select.getOutput();

        if (this.expression != null)
        {
            curves.keep(expression(curves, this.expression));
        }

        if (this.minlen != null || this.maxlen != null)
        {
            curves.keep(length(curves, this.minlen, this.maxlen));
        }

        if (this.longest)
        {
            curves = curves.copy(VectUtils.maxidx(this.input.lengths()));
        }

        this.output = curves;
        return this;
    }

    public static boolean[] length(Curves curves, Double low, Double high)
    {
        boolean[] filt = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            double length = curves.get(i).length();

            boolean pass = true;
            pass &= low == null || length >= low;
            pass &= high == null || length <= high;

            filt[i] = pass;
        }

        return filt;
    }

    public static boolean[] expression(Curves curves, String expression)
    {
        VectExpression e = new VectExpression(expression);

        List<Triple<String, String, String>> vars = Lists.newArrayList();
        for (String stat : new String[]{EXP_MIN, EXP_MAX, EXP_MEAN, EXP_SUM})
        {
            for (String field : curves.names())
            {
                String token = String.format("%s_%s", stat, field);
                if (expression.contains(token))
                {
                    vars.add(Triple.of(stat, field, token));
                }
            }
        }

        boolean useLength = expression.contains(EXP_LENGTH);
        boolean useSize = expression.contains(EXP_SIZE);

        boolean[] filt = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            Curves.Curve curve = curves.get(i);

            if (useLength)
            {
                double length = curve.length();
                e.with(EXP_LENGTH, VectSource.create1D(length));
            }

            if (useSize)
            {
                int size = curve.size();
                e.with(EXP_SIZE, VectSource.create1D(size));
            }

            for (Triple<String, String, String> var : vars)
            {
                VectsStats stats = new VectsStats().withInput(curve.getAll(var.b)).run();
                switch (var.a)
                {
                    case EXP_MIN:
                        e.with(var.c, stats.min);
                        break;
                    case EXP_MAX:
                        e.with(var.c, stats.max);
                        break;
                    case EXP_MEAN:
                        e.with(var.c, stats.mean);
                        break;
                    case EXP_SUM:
                        e.with(var.c, stats.sum);
                        break;
                    default:
                        Logging.error("bug found: " + var.c);
                }
            }

            Vect r = e.eval();
            filt[i] = Math.round(r.get(0)) != 0;
        }

        return filt;
    }

    public static Curves applyLength(Curves curves, double max)
    {
        return new CurvesSelect()
        {{
            this.input = curves;
            this.minlen = max;
        }}.run().output;
    }

    public static Curves applyLongest(Curves curves)
    {
        return new CurvesSelect()
        {{
            this.input = curves;
        }}.run().output;
    }
}
