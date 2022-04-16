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

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.data.datasets.Mask;
import qit.data.datasets.Sampling;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.source.MaskSource;
import qit.data.source.VectSource;
import qit.math.structs.Box;

@ModuleDescription("Create a mask from a box")
@ModuleAuthor("Ryan Cabeen")
public class MaskBoxCreate implements Module
{
    @ModuleInput
    @ModuleDescription("the input box")
    public Solids input;

    @ModuleParameter
    @ModuleDescription("voxel spacing")
    public double delta = 1;

    @ModuleParameter
    @ModuleDescription("round the starting point")
    public boolean round = false;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;
    
    @Override
    public MaskBoxCreate run()
    {
        Box box = this.input.box();
        Vect min = box.getMin();
        Vect max = box.getMax();

        int nx = (int) Math.round((max.get(0) - min.get(0)) / delta);
        int ny = (int) Math.round((max.get(1) - min.get(1)) / delta);
        int nz = (int) Math.round((max.get(2) - min.get(2)) / delta);

        nx = nx < 1 ? 1 : nx;
        ny = ny < 1 ? 1 : ny;
        nz = nz < 1 ? 1 : nz;

        Vect starts = min;
        Vect deltas = VectSource.create(delta, delta, delta);
        Integers nums = new Integers(nx, ny, nz);

        if (this.round)
        {
            starts.round();
        }

        this.output = MaskSource.create(new Sampling(starts, deltas, nums));

        return this;
    }
}
