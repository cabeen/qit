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

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.base.structs.Integers;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;

@ModuleDescription("Demosaic a volume to convert tiles of a single slice into a volume")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class VolumeDemosaic implements Module
{
    @ModuleInput
    @ModuleDescription("input")
    public Volume input;

    @ModuleParameter
    @ModuleDescription("the number of voxels buffering the tiles")
    public int buffer = 0;

    @ModuleParameter
    @ModuleDescription("the tile size in the i dimension")
    public int width = 0;

    @ModuleParameter
    @ModuleDescription("the tile size in the j dimension")
    public int height = 0;

    @ModuleOutput
    @ModuleDescription("output")
    public Volume output;

    @Override
    public VolumeDemosaic run()
    {
        Sampling sampling = this.input.getSampling();
        int ni = sampling.numI();
        int nj = sampling.numJ();
        int nk = sampling.numK();

        Global.assume(nk == 1, "expected a planar ij volume");

        int nit = (ni + this.buffer) / (this.width + this.buffer);
        int njt = (nj + this.buffer) / (this.height + this.buffer);
        int slices = nit * njt;

        Logging.info(String.format("found %d rows", nit));
        Logging.info(String.format("found %d columns", njt));

        Sampling nsampling = sampling.proto(new Integers(this.width, this.height, slices));
        Volume nvolume = VolumeSource.create(nsampling);

        for (Sample sample : nsampling)
        {
            int i = sample.getI();
            int j = sample.getJ();
            int k = sample.getK();

            int it = k % njt;
            int jt = (k - it) / njt;

            Logging.info(String.format("processing:"));
            Logging.info(String.format("   %d", k));
            Logging.info(String.format("   %d", it));
            Logging.info(String.format("   %d", jt));

            int ip = i + it * this.width + Math.max(0, it - 1) * this.buffer;
            int jp = j + jt * this.height + Math.max(0, jt - 1) * this.buffer;

            if (sampling.contains(ip, jp, 0))
            {
                nvolume.set(sample, this.input.get(ip, jp, 0));
            }
        }

        this.output = nvolume;

        return this;
    }
}
