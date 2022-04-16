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
import qit.base.annot.*;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

@ModuleDescription("Save the volume to an image stack")
@ModuleAuthor("Ryan Cabeen")
public class VolumeStackWrite implements Module
{
    public enum VolumeSaveStackAxis
    {
        i, j, k
    }

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleParameter
    @ModuleDescription("choose an axis for slicing")
    public VolumeSaveStackAxis axis = VolumeSaveStackAxis.k;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify a range, e.g. '10:50,5:25,start:2:end'")
    public String range = null;

    @ModuleParameter
    @ModuleDescription("enhance the contrast")
    public boolean enhance;

    @ModuleParameter
    @ModuleDescription("the output pattern for saving each image")
    public String pattern = "slice%04d.png";

    @ModuleParameter
    @ModuleDescription("output directory to the stack")
    public String output;

    @Override
    public Module run()
    {
        Function<Volume, Volume> prepare = (v) ->
        {
            final Volume cv = this.range == null ? v : v.copy(v.getSampling().range(this.range));

            if (this.enhance)
            {
                Logging.info("enhancing volume contrast");
                return new VolumeEnhanceContrast()
                {{
                    this.input = cv;
                    this.nobg = true;
                }}.run().output;
            }
            else if (cv.getDim() == 1)
            {
                Logging.info("normalizing volume intensities");
                return new VolumeNormalize()
                {{
                    this.input = cv;
                    this.type = VolumeNormalizeType.UnitMax;
                }}.run().output;
            }
            else
            {
                Logging.info("skipping signal processing");
                return cv;
            }
        };

        Volume volume = prepare.apply(this.input);

        try
        {
            PathUtils.mkdirs(this.output);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Sampling sampling = volume.getSampling();
        int ni = sampling.numI();
        int nj = sampling.numJ();
        int nk = sampling.numK();
        int dim = volume.getDim();

        Logging.info("  using directory: " + this.output);

        BiConsumer<Volume, Integer> write = (vol, idx) ->
        {
            try
            {
                String base = String.format(this.pattern, idx);
                Logging.info("  writing image: " + base);
                vol.write(PathUtils.join(this.output, base));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        };

        switch (this.axis)
        {
            case i:
            {
                Volume out = VolumeSource.create(nj, nk, 1, dim);

                for (int i = 0; i < ni; i++)
                {
                    for (int k = 0; k < nk; k++)
                    {
                        for (int j = 0; j < nj; j++)
                        {
                            for (int d = 0; d < dim; d++)
                            {
                                out.set(j, nk - 1 - k, 0, d, volume.get(i, j, k, d));
                            }
                        }
                    }

                    write.accept(out, i);
                }
                break;
            }
            case j:
            {
                Volume out = VolumeSource.create(ni, nk, 1, dim);

                for (int j = 0; j < nj; j++)
                {
                    for (int k = 0; k < nk; k++)
                    {
                        for (int i = 0; i < ni; i++)
                        {
                            for (int d = 0; d < dim; d++)
                            {
                                out.set(i, nk - 1 - k, 0, d, volume.get(i, j, k, d));
                            }
                        }
                    }

                    write.accept(out, j);
                }
                break;
            }
            case k:
            {
                Volume out = VolumeSource.create(ni, nj, 1, dim);

                for (int k = 0; k < nk; k++)
                {
                    for (int j = 0; j < nj; j++)
                    {
                        for (int i = 0; i < ni; i++)
                        {
                            for (int d = 0; d < dim; d++)
                            {
                                out.set(i, nj - 1 - j, 0, d, volume.get(i, j, k, d));
                            }
                        }
                    }

                    write.accept(out, k);
                }
                break;
            }
        }

        return this;
    }
}
