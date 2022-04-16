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
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qit.data.datasets.Matrix;
import qit.data.datasets.Record;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.formats.volume.NiftiHeader;
import qit.data.formats.volume.NiftiVolumeCoder;
import qit.data.utils.volume.VolumeVoxelStats;

@ModuleDescription("Print basic information about a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumePrintInfo implements Module
{
    @ModuleInput
    @ModuleDescription("the input volume")
    private Volume input;

    @ModuleParameter
    @ModuleDescription("print statistics")
    private boolean stats = false;

    @ModuleParameter
    @ModuleDescription("print complete nifti header (only relevant if the input is nifti)")
    private boolean nifti = false;

    @Override
    public Module run()
    {
        Sampling s = this.input.getSampling();
        Matrix m = s.quat().matrix();

        System.out.println("");
        System.out.println(String.format("  %s:", this.getClass().getSimpleName()));
        System.out.println("");
        System.out.println(String.format("    dimension: %d", this.input.getDim()));
        System.out.println(String.format("    model: %s", this.input.getModel().toString()));
        System.out.println(String.format("    datatype: %s", this.input.getType().toString()));
        System.out.println("");
        System.out.println(String.format("    num I: %d", s.numI()));
        System.out.println(String.format("    num J: %d", s.numJ()));
        System.out.println(String.format("    num K: %d", s.numK()));
        System.out.println(String.format("    total voxels: %d", s.size()));
        System.out.println("");
        System.out.println(String.format("    delta I: %g", s.deltaI()));
        System.out.println(String.format("    delta J: %g", s.deltaJ()));
        System.out.println(String.format("    delta K: %g", s.deltaK()));
        System.out.println(String.format("    voxel volume: %g", s.voxvol()));
        System.out.println("");
        System.out.println(String.format("    origin I: %g", s.startI()));
        System.out.println(String.format("    origin J: %g", s.startJ()));
        System.out.println(String.format("    origin K: %g", s.startK()));
        System.out.println("");
        System.out.println(String.format("    rotation quat A: %g", s.quatA()));
        System.out.println(String.format("    rotation quat B: %g", s.quatB()));
        System.out.println(String.format("    rotation quat C: %g", s.quatC()));
        System.out.println(String.format("    rotation quat D: %g", s.quatD()));
        System.out.println("");
        System.out.println(String.format("    rotation matrix:"));
        System.out.println(String.format("      [[%g, %g, %g],", m.get(0, 0), m.get(0, 1), m.get(0, 2)));
        System.out.println(String.format("       [%g, %g, %g],", m.get(1, 0), m.get(1, 1), m.get(1, 2)));
        System.out.println(String.format("       [%g, %g, %g]]", m.get(2, 0), m.get(2, 1), m.get(2, 2)));
        System.out.println("");

        if (this.stats)
        {
            for (int i = 0; i < this.input.getDim(); i++)
            {
                VolumeVoxelStats results = new VolumeVoxelStats().withInput(this.input).withChannel(i).run();

                if (this.input.getDim() == 1)
                {
                    System.out.println("    Volume value statistics:");
                }
                else
                {
                    System.out.println(String.format("    Volume channel %d value statistics:", i));
                }
                System.out.println("");
                System.out.println(String.format("      mean: %g", results.mean));
                System.out.println(String.format("      std: %g", results.std));
                System.out.println(String.format("      var: %g", results.var));
                System.out.println(String.format("      iqr: %g", results.iqr));
                System.out.println(String.format("      min: %g", results.min));
                System.out.println(String.format("      qlow: %g", results.qlow));
                System.out.println(String.format("      median: %g", results.median));
                System.out.println(String.format("      qhigh: %g", results.qhigh));
                System.out.println(String.format("      max: %g", results.max));
                System.out.println(String.format("      sum: %g", results.sum));
                System.out.println("");
            }
        }

        if (this.nifti)
        {
            for (Triple<Sampling,Integer, Boolean> c : NiftiVolumeCoder.CACHE.keySet())
            {
                NiftiHeader hdr = NiftiVolumeCoder.CACHE.get(c).a;
                Record map = hdr.info();

                System.out.println("    nifti:");
                for (String name : map.keySet())
                {
                    System.out.println("      " + name + ": " + map.get(name));
                }
                System.out.println("");
            }
        }

        return this;
    }
}
