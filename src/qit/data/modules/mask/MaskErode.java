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


package qit.data.modules.mask;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.source.MaskSource;

import java.util.List;

@ModuleDescription("Erode a mask morphologically")
@ModuleAuthor("Ryan Cabeen")
public class MaskErode implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleParameter
    @ModuleDescription("the number of iterations")
    public int num = 1;

    @ModuleDescription("specify an element: cross, cube, or sphere. you can also specify an optional size, e.g. cross(3)")
    public String element = MaskSource.DEFAULT_ELEMENT;

    @ModuleParameter
    @ModuleDescription("treat voxels outside mask as background")
    public boolean outside = false;

    @ModuleParameter
    @ModuleDescription("print messages")
    public boolean verbose = false;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskErode run()
    {
        Mask in = this.input.copy();
        Mask out = this.input.proto();
        Mask element = MaskSource.element(this.element);

        Sampling sampling = this.input.getSampling();
        Sampling esampling = element.getSampling();

        int ci = (esampling.numI() - 1) / 2;
        int cj = (esampling.numJ() - 1) / 2;
        int ck = (esampling.numK() - 1) / 2;

        boolean pi = sampling.numI() == 1;
        boolean pj = sampling.numJ() == 1;
        boolean pk = sampling.numK() == 1;

        List<Sample> esamples = Lists.newArrayList();
        for (Sample esample : esampling)
        {
            if (element.foreground(esample))
            {
                esamples.add(esample);
            }
        }

        for (int i = 0; i < this.num; i++)
        {
            Logging.info(this.verbose, "running iteration " + (i + 1));
            for (Sample sample : sampling)
            {
                boolean erode = false;
                for (Sample esample : esamples)
                {
                    int ni = sample.getI() + esample.getI() - ci;
                    int nj = sample.getJ() + esample.getJ() - cj;
                    int nk = sample.getK() + esample.getK() - ck;

                    boolean oi = pi && ni != 0;
                    boolean oj = pj && nj != 0;
                    boolean ok = pk && nk != 0;
                    if (oi || oj || ok)
                    {
                        // when the image is planar, skip out of plane erosion
                        continue;
                    }

                    if (!sampling.contains(ni, nj, nk))
                    {
                        if (this.outside)
                        {
                            erode = true;
                            break;
                        }
                    }
                    else if (in.background(ni, nj, nk))
                    {
                        erode = true;
                        break;
                    }
                }

                if (!erode)
                {
                    out.set(sample, in.get(sample));
                }
                else
                {
                    out.set(sample, 0);
                }
            }

            Mask tmp = out;
            out = in;
            in = tmp;
        }

        this.output = in;
        return this;
    }

    public static Mask apply(Mask mask)
    {
        return new MaskErode()
        {{
            this.input = mask;
            this.num = 1;
        }}.run().output;
    }

    public static Mask apply(Mask mask, int mynum)
    {
        return new MaskErode()
        {{
            this.input = mask;
            this.num = mynum;
        }}.run().output;
    }
}
