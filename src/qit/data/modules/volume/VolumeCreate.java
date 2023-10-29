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

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;

import java.util.function.Function;

@ModuleDescription("Create a volume based on user specified parameters")
@ModuleAuthor("Ryan Cabeen")
public class VolumeCreate implements Module
{
    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an optional reference volume for setting the voxel grid")
    public Volume reference = null;

    @ModuleParameter
    @ModuleDescription("the number of channels")
    public int channels = 1;

    @ModuleParameter
    @ModuleDescription("voxel spacing in x")
    public double deltax = 1;

    @ModuleParameter
    @ModuleDescription("voxel spacing in y")
    public double deltay = 1;

    @ModuleParameter
    @ModuleDescription("voxel spacing in z")
    public double deltaz = 1;
    
    @ModuleParameter
    @ModuleDescription("number of voxels in x")
    public int numx = 128;

    @ModuleParameter
    @ModuleDescription("number of voxels in y")
    public int numy = 128;

    @ModuleParameter
    @ModuleDescription("number of voxels in z")
    public int numz = 128;

    @ModuleParameter
    @ModuleDescription("starting position in x")
    public double startx = 0;

    @ModuleParameter
    @ModuleDescription("starting position in y")
    public double starty = 0;

    @ModuleParameter
    @ModuleDescription("starting position in z")
    public double startz = 0;
    
    @ModuleParameter
    @ModuleDescription("constant value")
    public double value = 0;

    @ModuleParameter
    @ModuleDescription("draw a grid")
    public boolean grid = false;

    @ModuleParameter
    @ModuleDescription("the grid spacing in voxels (number of voxels between grid lines)")
    public int gridSpace = 20;

    @ModuleParameter
    @ModuleDescription("the grid thickness in voxels (diameter will be 2 * n + 1)")
    public int gridThick = 0;

    @ModuleParameter
    @ModuleDescription("the grid intensity")
    public int gridValue = 1;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;
    
    @Override
    public VolumeCreate run()
    {
        if (this.reference != null)
        {
           this.output = this.reference.proto();
        }
        else
        {
            Vect start = VectSource.create3D(this.startx, this.startx, this.startx);
            Vect delta = VectSource.create3D(this.deltax, this.deltay, this.deltaz);
            Integers nums = new Integers(this.numx, this.numy, this.numz);

            this.output = VolumeSource.create(new Sampling(start, delta, nums), this.channels);
        }

        this.output.setAll(VectSource.create1D(this.value));

        if (this.grid)
        {
            Function<Integer,Boolean> check = (idx) ->
            {
                int count = Math.round(((float) idx) / this.gridSpace);
                int offset = idx - count * this.gridSpace;

                if (idx == 57 || idx == 64 || idx == 41)
                {
                    Logging.info(idx + " " + count + " " + offset);
                }

                return Math.abs(offset) <= this.gridThick;
            };

            for (Sample sample : this.output.getSampling())
            {
                boolean checkI = check.apply(sample.getI());
                boolean checkJ = check.apply(sample.getJ());
                boolean checkK = check.apply(sample.getK());

                if (checkI || checkJ || checkK)
                {
                    this.output.set(sample, VectSource.createND(this.output.getDim(), this.gridValue));
                }
            }
        }

        return this;
    }
}
