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
import qit.base.annot.*;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.formats.volume.BufferedImageVolumeCoder;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.volume.VolumeStackUtils;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@ModuleDescription("Extract particles from an image stack.  This is meant to process datasets that are too big to be fully loaded into memory.  This will process one slice at a time and save the results to disk in the process.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeStackParticles implements Module
{
    @ModuleParameter
    @ModuleDescription("input filename pattern (e.g. %04d will be replaced with 0000, 0001, etc. or e.g. %d will be replaced with 0, 1, etc.)")
    public String input = "slice%04d.tif";

    @ModuleParameter
    @ModuleDescription("the voxel size in the i direction")
    public double isize = 1;

    @ModuleParameter
    @ModuleDescription("the voxel size in the j direction")
    public double jsize = 1;

    @ModuleParameter
    @ModuleDescription("the voxel size in the k direction")
    public double ksize = 1;

    @ModuleParameter
    @ModuleDescription("start at the given pixel in i when reading the volume")
    public int istart = 0;

    @ModuleParameter
    @ModuleDescription("start at the given pixel in j when reading the volume")
    public int jstart = 0;

    @ModuleParameter
    @ModuleDescription("start at the given pixel in k when reading the volume")
    public int kstart = 0;

    @ModuleParameter
    @ModuleDescription("step the given number of pixels in i when reading the volume")
    public int istep = 1;

    @ModuleParameter
    @ModuleDescription("step the given number of pixels in j when reading the volume")
    public int jstep = 1;

    @ModuleParameter
    @ModuleDescription("step the given number of pixels in k when reading the volume")
    public int kstep = 1;

    @ModuleParameter
    @ModuleDescription("the number of particle samples per unit intensity (e.g. if the image intensity is one, it will produce this many particles)")
    public Double samples = 8.0;

    @ModuleParameter
    @ModuleDescription("specify a minimum window intensity (all voxels below this intensity are ignored)")
    public double min = 0.25;

    @ModuleParameter
    @ModuleDescription("specify a maximum window intensity (all voxels above this intensity are sampled uniformly)")
    public double max = 0.75;

    @ModuleParameter
    @ModuleDescription("keep empty particle slices")
    public boolean empty = false;

    @ModuleParameter
    @ModuleDescription("start at the given index when loading slices")
    public int dstart = 0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("end at the given index when loading slices")
    public Integer dend = null;

    @ModuleParameter
    @ModuleDescription("step the given amount when loading slices")
    public int dstep = 1;

    @ModuleParameter
    @ModuleDescription("use the given image interpolation method")
    public InterpolationType interp = InterpolationType.Nearest;

    @ModuleParameter
    @ModuleDescription("output filename of particles (should end in csv)")
    public String output = "output.csv";

    @Override
    public VolumeStackParticles run()
    {
        try
        {
            PathUtils.mkpar(this.output);
            PrintWriter pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(this.output)));

            pw.write("float32 Position[0], float32 Position[1], float32 Position[2],");
            pw.write("float32 Color[0], float32 Color[1], float32 Color[2]\n");

            List<Integer> sidx = VolumeStackUtils.detect(this.input, this.dstart, this.dend, this.dstep);

            Global.assume(sidx.size() > 0, "no sidx were found!");

            Volume ref = Volume.read(String.format(this.input, sidx.get(0)));
            Sampling sref = ref.getSampling();
            Global.assume(sref.numK() == 1, "expected 2D input");

            int ri = sref.numI();
            int rj = sref.numJ();
            int rk = sidx.size();

            int ni = (ri - this.istart) / this.istep;
            int nj = (rj - this.jstart) / this.jstep;
            int nk = (rk - this.kstart) / this.kstep;

            Logging.info("using ni: " + ni);
            Logging.info("using nj: " + nj);
            Logging.info("using nk: " + nk);

            BufferedImageVolumeCoder.VERBOSE = true;

            for (int k = 0; k < nk; k++)
            {
                int kk = k * this.kstep;

                try
                {
                    String fn = String.format(this.input, sidx.get(kk));
                    if (!PathUtils.exists(fn))
                    {
                        Logging.info("...slice not found: " + fn);
                        continue;
                    }

                    Logging.info("...processing slice: " + fn);
                    Volume slice = Volume.read(fn);
                    Sampling samp = ref.getSampling();
                    if (samp.numI() != ri || samp.numJ() != rj || samp.numK() != 1)
                    {
                        Logging.info("...invalid slice: " + k);
                        continue;
                    }

                    Sampling sampling = slice.getSampling();
                    VectFunction function = this.function(slice);

                    double delta = this.max <= this.min ? 1.0 : this.max - this.min;

                    for (int j = 0; j < nj; j++)
                    {
                        int jj = this.jstart + j * this.jstep;

                        for (int i = 0; i < ni; i++)
                        {
                            int ii = this.istart + i * this.istep;

                            double value = slice.get(ii, jj, 0).norm();
                            if (value >= this.min)
                            {
                                int count = (int) (this.samples * Math.min(1.0, (value - this.min) / delta));

                                for (int c = 0; c < count; c++)
                                {
                                    Vect pos = sampling.random(i, j, k);
                                    Vect color = function.apply(pos);

                                    pos.timesEquals(0, this.isize);
                                    pos.timesEquals(1, this.jsize);
                                    pos.timesEquals(2, this.ksize);

                                    pw.write(String.format("%g,%g,%g,", pos.getX(), pos.getY(), pos.getZ()));
                                    pw.write(String.format("%g,%g,%g\n", color.getX(), color.getY(), color.getZ()));
                                }
                            }
                        }
                    }
                }
                catch (RuntimeException e)
                {
                    Logging.info("... skipping slice due to error in reading: " + k);
                }
            }

            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Logging.info("finished writing particles");

        return this;
    }

    private VectFunction function(Volume volume)
    {
        if (volume.getDim() == 3)
        {
            Logging.info("detected RGB image");
            return VolumeUtils.interp(this.interp, volume);
        }
        else if (volume.getDim() == 4)
        {
            Logging.info("detected RGBA image (excluding alpha)");
            boolean[] filter = new boolean[]{true, true, true, false};
            return VolumeUtils.interp(this.interp, volume).compose(VectFunctionSource.subset(filter));
        }
        else if (volume.getDim() == 1)
        {
            Logging.info("detected grayscale image");
            return VolumeUtils.interp(this.interp, volume).compose(VectFunctionSource.repeat(1, 3));
        }
        else
        {
            throw new RuntimeException("multi-channel images are not supported");
        }
    }
}
