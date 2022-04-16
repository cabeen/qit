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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.volume.VolumeStackUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@ModuleDescription("Extract particles from an image stack.  This is meant to process datasets that are too big to be fully loaded into memory.  This will process one slice at a time and save the results to disk in the process.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeStackWriteBlocks implements Module
{
    @ModuleParameter
    @ModuleDescription("input image stack filename pattern (e.g. %04d will be replaced with 0000, 0001, etc. or e.g. %d will be replaced with 0, 1, etc.)")
    public String input = "/your/path/slice%04d.tif";

    @ModuleParameter
    @ModuleDescription("the block size in the i direction")
    public int isize = 256;

    @ModuleParameter
    @ModuleDescription("the block size in the j direction")
    public int jsize = 256;

    @ModuleParameter
    @ModuleDescription("the voxel size in the k direction")
    public int ksize = 64;

    @ModuleParameter
    @ModuleDescription("the amount of overlap in blocks")
    public int overlap = 8;

    @ModuleParameter
    @ModuleDescription("the voxel size in the i direction")
    public double idelta = 1.0;

    @ModuleParameter
    @ModuleDescription("the voxel size in the j direction")
    public double jdelta = 1.0;

    @ModuleParameter
    @ModuleDescription("the voxel delta in the k direction")
    public double kdelta = 1.0;

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
    @ModuleDescription("output block filename pattern (e.g. %04d will be replaced with 0000, 0001, etc. or e.g. %d will be replaced with 0, 1, etc.)")
    public String output = "/your/path/output%04d.nii.gz";

    @Override
    public VolumeStackWriteBlocks run() throws IOException
    {
        Global.assume(this.isize > this.overlap, "overlap must be smaller than block size in i");
        Global.assume(this.jsize > this.overlap, "overlap must be smaller than block size in j");
        Global.assume(this.ksize > this.overlap, "overlap must be smaller than block size in k");

        List<Integer> sidx = VolumeStackUtils.detect(this.input, this.dstart, this.dend, this.dstep);

        Global.assume(sidx.size() > 0, "no sidx were found!");

        Volume ref = Volume.read(String.format(this.input, sidx.get(0)));
        Sampling sref = ref.getSampling();
        Global.assume(sref.numK() == 1, "expected 2D input");

        int ni = sref.numI();
        int nj = sref.numJ();
        int nk = sidx.size();

        int dim = ref.getDim();

        Sampling sampling = SamplingSource.create(new Integers(ni, nj, nk), VectSource.create3D(this.idelta, this.jdelta, this.kdelta));

        int iBlockCount = (int) Math.ceil((ni + this.overlap) / (double) (this.isize + this.overlap));
        int jBlockCount = (int) Math.ceil((nj + this.overlap) / (double) (this.jsize + this.overlap));
        int kBlockCount = (int) Math.ceil((nk + this.overlap) / (double) (this.ksize + this.overlap));

        Logging.info(String.format("stack dimensions: %d x %d x %d", ni, nj, nk));
        Logging.info(String.format("grid dimensions: %d x %d x %d", iBlockCount, jBlockCount, kBlockCount));
        Logging.info(String.format("block dimensions: %d x %d x %d", this.isize, this.jsize, this.ksize));

        PathUtils.mkpar(this.output);
        final Map<Integer, Volume> slices = Maps.newHashMap();

        int blockIndex = 0;
        for (int k = 0; k < kBlockCount; k++)
        {
            int myStartK = this.ksize * k - (k  > 0 ? this.overlap : 0);
            int myEndK = Math.min(nk, myStartK + this.ksize + this.overlap);

            Logging.info("evicting cache");
            for (Integer idx : Sets.newHashSet(slices.keySet()))
            {
                // remove slices that preceed the ones we need
                if (idx < myStartK)
                {
                    slices.remove(idx);
                }
            }

            for (int j = 0; j < jBlockCount; j++)
            {
                for (int i = 0; i < iBlockCount; i++)
                {
                    int myStartI = this.isize * i - (i > 0 ? this.overlap : 0);
                    int myStartJ = this.jsize * j - (j > 0 ? this.overlap : 0);

                    int myEndI = Math.min(ni - 1, myStartI + this.isize + (i > 0 ? 2 * this.overlap : 0));
                    int myEndJ = Math.min(nj - 1, myStartJ + this.jsize + (j > 0 ? 2 * this.overlap : 0));

                    Sample myStart = new Sample(myStartI, myStartJ, myStartK);
                    Sample myEnd = new Sample(myEndI, myEndJ, myEndK);

                    if (sampling.contains(myStart))
                    {
                        Sampling mySampling = sampling.range(myStart, myEnd);

                        Volume myBlock = VolumeSource.create(mySampling, dim);
                        for (int kk = 0; kk < mySampling.numK(); kk++)
                        {
                            int myk = myStartK + kk;

                            if (myk < nk)
                            {
                                if (!slices.containsKey(myk))
                                {
                                    Logging.info("reading slice " + myk);
                                    slices.put(myk, Volume.read(String.format(this.input, sidx.get(myk))));
                                }

                                Volume slice = slices.get(myk);

                                for (int jj = 0; jj < mySampling.numJ(); jj++)
                                {
                                    for (int ii = 0; ii < mySampling.numI(); ii++)
                                    {
                                        int myi = myStartI + ii;
                                        int myj = myStartJ + jj;

                                        if (sref.contains(myi, myj, 0))
                                        {
                                            myBlock.set(ii, jj, kk, slice.get(myi, myj, 0));
                                        }
                                    }
                                }
                            }
                        }

                        Logging.info(String.format("writing blocking %d = (%d, %d, %d)", blockIndex, i, j, k));
                        String outfn = String.format(this.output, blockIndex);
                        myBlock.write(outfn);
                    }
                    else
                    {
                        Logging.info(String.format("skipping blocking %d = (%d, %d, %d)", blockIndex, i, j, k));
                    }

                    blockIndex += 1;
                }
            }
        }

        Logging.info("finished writing blocks");

        return this;
    }
}
