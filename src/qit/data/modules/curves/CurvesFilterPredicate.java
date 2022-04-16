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
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.expression.VectExpression;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ModuleDescription("Filter curves with kernel regression.  This uses a non-parametric statistical approach to smooth the curves")
@ModuleAuthor("Ryan Cabeen")
public class CurvesFilterPredicate implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("The minimum number of vertices of the retained segments")
    public int min = 2;

    @ModuleParameter
    @ModuleDescription("Replace any dots in the attribute name with an underscore, e.g. pval.age would become pval_age")
    public boolean nodot = false;

    @ModuleParameter
    @ModuleDescription("the predicate for determining if a vertex should be kept")
    public String predicate = "pval < 0.05";

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @Override
    public CurvesFilterPredicate run()
    {
        VectExpression e = new VectExpression(this.predicate);
        Curves curves = this.input.proto();

        Map<String, String> fields = Maps.newHashMap();

        for (String field : this.input.names())
        {
            String expfield = field;

            if (this.nodot && expfield.contains("."))
            {
                expfield = field.replace(".", "_");
            }

            if (this.predicate.contains(expfield))
            {
                Logging.infosub("using attribute in expression: %s", expfield);
                fields.put(field, expfield);
            }
        }

        BiConsumer<Curves.Curve, Pair<Integer, Integer>> addSegment = (curve, p) ->
        {
            int num = p.b - p.a;
            Curves.Curve mycurve = curves.add(num);

            for (int i = 0; i < num; i++)
            {
                for (String field : this.input.names())
                {
                    mycurve.set(field, i, curve.get(field, p.a + i));
                }
            }
        };

        Logging.infosub("started with %d curves", this.input.size());

        for (Curves.Curve curve : this.input)
        {
            Integer first = null;

            for (int i = 0; i < curve.size(); i++)
            {
                for (String field : fields.keySet())
                {
                    e.with(fields.get(field), curve.get(field, i));
                }

                Vect r = e.eval();
                boolean pass = MathUtils.nonzero(r.get(0));

                if (pass)
                {
                    if (first == null)
                    {
                        first = i;
                    }
                }
                else if (first != null)
                {
                    if (i - first > this.min)
                    {
                        addSegment.accept(curve, Pair.of(first, i));
                        first = null;
                    }
                }
            }

            if (first != null && curve.size() - first > this.min)
            {
                addSegment.accept(curve, Pair.of(first, curve.size()));
            }
        }

        Logging.infosub("finished with %d curves", curves.size());

        this.output = curves;
        return this;
    }
}
