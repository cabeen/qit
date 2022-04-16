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

import com.google.common.collect.Maps;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.utils.CurvesUtils;
import qit.math.utils.MathUtils;

import java.util.Map;

@ModuleDescription("Relabel curves from biggest to smallest cluster")
@ModuleAuthor("Ryan Cabeen")
public class CurvesRelabel implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("keep only the largest label")
    public boolean largest = false;

    @ModuleParameter
    @ModuleDescription("retain only clusters above a given proportion of the total")
    public Double threshold = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    @Override
    public CurvesRelabel run()
    {
        Curves curves = this.inplace ? this.input : this.input.copy();

        if (curves.has(Curves.LABEL))
        {
            int[] labels = CurvesUtils.attrGetLabelsPerCurve(curves, Curves.LABEL);

            Map<Integer, Integer> counts = Maps.newHashMap();
            for (int i = 0; i < this.input.size(); i++)
            {
                int length = (int) this.input.get(i).length();
                int idx = labels[i];

                if (counts.containsKey(idx))
                {
                    counts.put(idx, counts.get(idx) + length);
                }
                else
                {
                    counts.put(idx, length);
                }
            }

            Map<Integer, Integer> lookup = MathUtils.remap(counts);

            labels = MathUtils.map(labels, lookup);

            labels = MathUtils.relabel(labels);
            CurvesUtils.attrSetLabelsPerCurve(curves, Curves.LABEL, labels);

            if (this.largest)
            {
                int largestIndex = labels[0];
                double largestValue = counts.get(largestIndex);
                for (Integer index : counts.keySet())
                {
                    double value = counts.get(index);
                    if (counts.get(index) > largestValue)
                    {
                        largestIndex = index;
                        largestValue = value;
                    }
                }

                boolean[] filter = new boolean[this.input.size()];
                for (int i = 0; i < filter.length; i++)
                {
                    filter[i] = labels[i] == largestIndex;
                }

                curves.keep(filter);
            }

            if (this.threshold != null)
            {
                boolean[] filter = new boolean[this.input.size()];
                for (int i = 0; i < filter.length; i++)
                {
                    filter[i] = counts.get(labels[i]) / labels.length > this.threshold;
                }

                curves.keep(filter);

            }
        }

        this.output = curves;

        return this;
    }
}