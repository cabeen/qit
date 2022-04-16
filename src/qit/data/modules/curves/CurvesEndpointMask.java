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

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskDilate;
import qit.data.modules.mask.MaskFilterMode;

@ModuleDescription("Extract curve endpoints and create a mask")
@ModuleAuthor("Ryan Cabeen")
public class CurvesEndpointMask implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference volume")
    public Volume reference;

    @ModuleParameter
    @ModuleDescription("curve head label")
    public int head = 1;

    @ModuleParameter
    @ModuleDescription("curve tail label")
    public int tail = 2;

    @ModuleParameter
    @ModuleDescription("use the given number of vertices from the endpoints")
    public int num = 1;

    @ModuleParameter
    @ModuleDescription("dilate the endpoint mask")
    public int dilate = 0;

    @ModuleParameter
    @ModuleDescription("apply a mode filter")
    public boolean mode;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public CurvesEndpointMask run()
    {
        Sampling sampling = this.reference.getSampling();
        Curves curves = this.input;

        Logging.info("orienting curves");
        curves = new CurvesOrient()
        {{
            this.input = CurvesEndpointMask.this.input;
        }}.run().output;

        Logging.info("sampling endpoints");
        Mask out = new Mask(sampling);
        for (Curves.Curve curve : curves)
        {
            int max = curve.size();
            for (int i = 0; i < this.num; i++)
            {
                int hidx = i;
                int tidx = max - 1 - i;

                if (hidx < max - 1)
                {
                    Sample sample = sampling.nearest(curve.get(hidx));
                    if (sampling.contains(sample))
                    {
                        out.set(sample, this.head);
                    }
                }

                if (tidx > 0)
                {
                    Sample sample = sampling.nearest(curve.get(tidx));
                    if (sampling.contains(sample))
                    {
                        out.set(sample, this.tail);
                    }
                }
            }
        }

        if (this.dilate > 0)
        {
            Logging.info("dilating mask");
            final Mask fout = out;
            out = new MaskDilate()
            {{
                this.input = fout;
                this.num = CurvesEndpointMask.this.dilate;
            }}.run().output;
        }

        if (this.mode)
        {
            Logging.info("filtering mode");
            out = MaskFilterMode.apply(out);
        }

        this.output = out;

        return this;
    }
}
