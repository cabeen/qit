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
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

@ModuleDescription("Resample the position of vertices along curves so that they have uniform spacing")
@ModuleAuthor("Ryan Cabeen")
public class CurvesResample implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("resample with a constant number of vertices per curve (0 specifices that the max should be used)")
    public Integer num = null;

    @ModuleParameter
    @ModuleDescription("reorient curves")
    public boolean orient = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("resample with a given vertex density (mm/vertex)")
    public Double density = null;

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @Override
    public CurvesResample run()
    {
        Curves out = this.input.copy().clean();

        if (this.orient)
        {
            final Curves fout = out;
            out = new CurvesOrient()
            {{
                this.input = fout;
            }}.run().output;
        }

        if (this.density != null)
        {
            for (Curves.Curve curve : out)
            {
                double length = curve.length();
                int verts = (int) Math.ceil(length / this.density);

                if (verts > 1)
                {
                    Vect gamma = VectSource.linspace(0f, 1f, verts);
                    curve.resample(gamma);
                }
            }
        }

        if (this.num != null && this.num > 1)
        {
            int count = this.num;
            if (count == 0)
            {
                VectOnlineStats stat = new VectOnlineStats();
                for (Curves.Curve curve : out)
                {
                    stat.update(curve.size());
                }
                count = MathUtils.round(stat.max);
            }

            Vect gamma = VectSource.linspace(0f, 1f, count);
            for (Curves.Curve curve : out)
            {
                curve.resample(gamma);
            }
        }

        this.output = out;
        return this;
    }
}
