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
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.math.structs.Segment;

@ModuleDescription("Simplify curves with the Ramer-Douglas-Peucker algorithm")
@ModuleCitation("Heckbert, Paul S.; Garland, Michael (1997). Survey of polygonal simplification algorithms")
@ModuleAuthor("Ryan Cabeen")
public class CurvesSimplify implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("the distance threshold")
    public double epsilon = 2.0;

    @ModuleParameter
    @ModuleDescription("use radial distance")
    public boolean radial = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("modify curves in-place")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output simplified curves")
    public Curves output;

    public CurvesSimplify run()
    {
        Curves out = this.inplace ? this.input : this.input.copy();

        double ep2 = this.epsilon * this.epsilon;
        for (Curve curve : out)
        {
            if (curve.size() < 2)
            {
                continue;
            }

            Vects points = new Vects(curve.size());
            for (Vect v : curve)
            {
                points.add(v);
            }

            boolean[] markers = null;
            if (this.radial)
            {
                markers = simplifyRadialDistance(points, ep2);
            }
            else
            {
                markers = simplifyDouglasPeucker(points, ep2);
            }

            curve.subset(markers);
        }

        this.output = out;

        return this;
    }

    public static boolean[] simplifyRadialDistance(Vects points, double sqTolerance)
    {
        int num = points.size();
        Vect point = null;
        Vect prevPoint = points.get(0);

        boolean[] markers = new boolean[num];
        markers[0] = true;
        markers[num - 1] = true;

        for (int i = 0; i < num; i++)
        {
            point = points.get(i);
            if (point.dist2(prevPoint) > sqTolerance)
            {
                markers[i] = true;
                prevPoint = point;
            }
        }

        return markers;
    }

    public static boolean[] simplifyDouglasPeucker(Vects points, double sqTolerance)
    {
        int num = points.size();
        int first = 0;
        int last = num - 1;
        boolean[] markers = new boolean[num];
        markers[first] = true;
        markers[last] = true;

        simplifyDouglasPeucker(points, sqTolerance, first, last, markers);
        return markers;
    }

    private static void simplifyDouglasPeucker(Vects points, double sqTolerance, int first, int last, boolean[] markers)
    {
        double maxValue = 0;
        int maxIdx = -1;

        for (int idx = first + 1; idx < last; idx++)
        {
            double sqDist = new Segment(points.get(first), points.get(last)).dist2(points.get(idx));

            if (sqDist > maxValue)
            {
                maxIdx = idx;
                maxValue = sqDist;
            }
        }

        if (maxValue > sqTolerance)
        {
            markers[maxIdx] = true;

            simplifyDouglasPeucker(points, sqTolerance, first, maxIdx, markers);
            simplifyDouglasPeucker(points, sqTolerance, maxIdx, last, markers);
        }
    }

    public static Curves apply(Curves curves, double radius)
    {
        return new CurvesSimplify()
        {{
            this.input = curves;
            this.epsilon = radius;
        }}.run().output;
    }
}
