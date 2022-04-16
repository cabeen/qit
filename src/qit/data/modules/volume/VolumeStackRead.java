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
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.volume.VolumeStackUtils;

import java.io.IOException;
import java.util.List;

@ModuleDescription("Read a volume from an image stack")
@ModuleAuthor("Ryan Cabeen")
public class VolumeStackRead implements Module
{
    @ModuleParameter
    @ModuleDescription("the input filename input for reading each image (must contain %d or some a similar formatting charater for substituting the index)")
    public String input = "%03d.tif";

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
    @ModuleOptional
    @ModuleDescription("apply Gaussian smoothing to each slice with the given bandwidth in pixels (before downsampling)")
    public Double smooth = null;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    @Override
    public VolumeStackRead run() throws IOException
    {
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

        Integers num = new Integers(ni, nj, nk);
        Vect delta = VectSource.create3D(this.isize, this.jsize, this.ksize);

        int dim = ref.getDim();
        Sampling sampling = SamplingSource.create(num, delta);
        Volume out = VolumeSource.create(sampling, dim);

        for (int k = 0; k < nk; k++)
        {
            String fn = String.format(this.input, sidx.get(k));
            if (!PathUtils.exists(fn))
            {
                Logging.info("...slice not found: " + fn);
                continue;
            }

            Volume slice = Volume.read(fn);
            Sampling samp = ref.getSampling();
            if (samp.numI() != ri || samp.numJ() != rj || samp.numK() != 1)
            {
                Logging.info("...invalid slice: " + k);
                continue;
            }

            if (this.smooth != null)
            {
                Logging.info("...smoothing slice: " + k);
                VolumeFilterGaussian filter = new VolumeFilterGaussian();
                filter.input = slice;
                filter.sigma = this.smooth;
                filter.support = Math.max(this.istart, this.jstep);
                slice = filter.run().output;
            }

            try
            {
                for (int j = 0; j < nj; j++)
                {
                    int jj = this.jstart + j * this.jstep;

                    for (int i = 0; i < ni; i++)
                    {
                        int ii = this.istart + i * this.istep;

                        for (int d = 0; d < Math.min(slice.getDim(), dim); d++)
                        {
                            out.set(i, j, k, d, slice.get(ii, jj, 0, d));
                        }
                    }
                }

                Logging.info("...loaded slice: " + k);
            }
            catch (RuntimeException e)
            {
                Logging.info("...error loading slice: " + k);
            }
        }

        this.output = out;

        return this;
    }
}
