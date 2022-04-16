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
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Deformation;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.VectsUtils;
import qit.math.structs.Segment;

@ModuleUnlisted
@ModuleDescription("Compute the closest point transform of curves")
@ModuleCitation("(in preparation)")
@ModuleAuthor("Ryan Cabeen")
public class CurvesClosestPointTransform implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a deformation from the curves to the landmark space")
    public Deformation deform = null;

    @ModuleParameter
    @ModuleDescription("the landmarks used to sample the transform")
    public Vects landmarks = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("unpack the closest points for visualization purposes")
    public boolean unpack = false;

    @ModuleOutput
    @ModuleDescription("the output vects")
    public Vects output = null;

    public Vects transform(Curve curve)
    {
        Vects vects = new Vects();

        for (int k = 0; k < landmarks.size(); k++)
        {
            Vect landmark = this.landmarks.get(k);
            Double minDist2 = null;
            Vect minPos = null;

            if (curve.size() == 1)
            {
                minPos = curve.get(0);
            }
            else
            {
                for (int j = 0; j < curve.size() - 1; j++)
                {
                    Vect start = curve.get(j);
                    Vect end = curve.get(j + 1);

                    if (this.deform != null)
                    {
                        start = deform.apply(start);
                        end = deform.apply(end);
                    }

                    Segment seg = new Segment(start, end);
                    double param = seg.nearest(landmark);
                    Vect pos = seg.get(param);
                    double dist2 = pos.dist2(landmark);

                    if (minDist2 == null || dist2 < minDist2)
                    {
                        minDist2 = dist2;
                        minPos = pos;
                    }
                }
            }

            vects.add(minPos);
        }

        return vects;
    }

    @Override
    public CurvesClosestPointTransform run()
    {
        Vects out = new Vects();

        int ppcent = 0;
        for (int i = 0; i < this.input.size(); i++)
        {
            int pcent = Math.round((100 * (i+1)) / (float) this.input.size());
            if (pcent > ppcent)
            {
                Logging.info(String.format("...%d percent completed", pcent));
                ppcent = pcent;
            }

            Vects vects = this.transform(this.input.get(i));
            if (this.unpack)
            {
                out.addAll(vects);
            }
            else
            {
                out.add(VectsUtils.pack(vects));
            }
        }

        this.output = out;

        return this;
    }
}