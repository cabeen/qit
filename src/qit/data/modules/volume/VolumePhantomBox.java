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

package qit.data.modules.volume;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;

@ModuleDescription("Create a simple box phantom")
@ModuleAuthor("Ryan Cabeen")
public class VolumePhantomBox implements Module
{
    @ModuleParameter
    @ModuleDescription("image width")
    private int width = 100;

    @ModuleParameter
    @ModuleDescription("image height")
    private int height = 100;

    @ModuleParameter
    @ModuleDescription("image slices")
    private int slices = 1;

    @ModuleParameter
    @ModuleDescription("box relative dimensions")
    private double size = 0.5;

    @ModuleOutput
    @ModuleDescription("output volume")
    private Volume output;

    public VolumePhantomBox()
    {
    }

    public VolumePhantomBox run()
    {
        int i0 = (int) Math.floor(this.width * this.size * 0.5);
        int i1 = (int) Math.floor(this.width * this.size * 1.5);
        int j0 = (int) Math.floor(this.height * this.size * 0.5);
        int j1 = (int) Math.floor(this.height * this.size * 1.5);
        int k0 = (int) Math.floor(this.slices * this.size * 0.5);
        int k1 = (int) Math.floor(this.slices * this.size * 1.5);

        Volume out = VolumeSource.create(this.width, this.height, this.slices);
        for (int i = i0; i <= i1; i++)
        {
            for (int j = j0; j <= j1; j++)
            {
                for (int k = k0; k <= k1; k++)
                {
                    if (out.getSampling().contains(i, j, k))
                    {
                        out.set(i, j, k, 0, 1.0);
                    }
                }
            }
        }

        this.output = out;

        return this;
    }

    public Volume getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
