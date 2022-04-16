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
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.Quaternion;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

@ModuleDescription("Create a mosaic from an image volume.  This will take a 3D volume and create a single slice that shows the whole dataset.  This is mainly useful for quality assessment.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeMosaic implements Module
{
    public enum VolumeMosaicAxis
    {
        i, j, k
    }

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("choose an axis for slicing")
    public VolumeMosaicAxis axis = VolumeMosaicAxis.i;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a string for cropping the volume, e.g. :,:,10:2:17 would show slices 10, 12, 14, and 16 etc." )
    public String crop = null;

    @ModuleParameter
    @ModuleDescription("treat the input as RGB")
    public boolean rgb;

    @ModuleParameter
    @ModuleDescription("enhance the contrast")
    public boolean enhance;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a linear colormap with the given fixed minimum")
    public Double min;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a linear colormap with the given fixed maximum")
    public Double max;

    @ModuleParameter
    @ModuleDescription("save out multichannel slices (the output filename must include '%d' if you use this option)")
    public boolean multichannel;

    @ModuleParameter
    @ModuleDescription("output filename to save mosaic")
    public String output;

    @Override
    public Module run()
    {
        Volume invol = VolumeUtils.mask(this.input, this.mask);

        if (this.crop != null)
        {
            Sampling sampling = this.input.getSampling().range(this.crop);
            invol = invol.copy(sampling);
        }

        final Volume finvol = invol;

        if (this.multichannel)
        {
            Global.assume(this.output.contains("%"), "multichannel output must contain a '%d'");

            for (int i = 0; i < this.input.getDim(); i++)
            {
                String fn = String.format(this.output, i);
                try
                {
                    Logging.info("... writing: " + fn);
                    PathUtils.mkpar(fn);
                    this.mosaic(this.normalize(finvol.getVolume(i))).write(fn);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

        }
        else if (this.rgb)
        {
            String fn = this.output;
            try
            {
                Logging.info("... writing: " + fn);
                PathUtils.mkpar(fn);
                this.mosaic(finvol).write(fn);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            String fn = this.output;
            try
            {
                Logging.info("... writing: " + fn);
                PathUtils.mkpar(fn);
                this.mosaic(this.normalize(finvol.getVolume(0))).write(fn);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return this;
    }

    private Volume normalize(final Volume volume)
    {
        if (this.rgb)
        {
            Global.assume(volume.getDim() == 3, "expected RGB volume");
            return volume;
        }
        else if (this.enhance)
        {
            return new VolumeEnhanceContrast()
            {{
                this.input = volume;
                this.nobg = true;
            }}.run().output;
        }
        else if (this.min != null || this.max != null)
        {
            final VectOnlineStats stats = new VectOnlineStats();

            for (Sample sample : volume.getSampling())
            {
                 stats.update(volume.get(sample, 0));
            }

            final double smin = this.min != null ? this.min : stats.min;
            final double smax = this.max != null ? this.max : stats.max;
            final double scale = MathUtils.eq(smin, smax) ? 0.0 : 1.0 / (smax - smin);

            VectFunction fun = new VectFunction()
            {
                public void apply(Vect inv, Vect outv)
                {
                    outv.set(inv.minus(smin).times(scale));
                }
            }.init(1, 1);

            return new VolumeFunction(fun).withInput(volume).withMessages(false).run();
        }
        else
        {
            return new VolumeNormalize()
            {{
                this.input = volume;
                this.type = VolumeNormalizeType.UnitMax;
            }}.run().output;
        }
    }

    private Volume mosaic(Volume volume)
    {
        switch (this.axis)
        {
            case i:
            {
                return mosaicI(volume);
            }
            case j:
            {
                return mosaicJ(volume);
            }
            case k:
            {
                return mosaicK(volume);
            }
        }

        throw new RuntimeException("invalid axis: " + this.axis);
    }

    private Volume mosaicI(Volume volume)
    {
        Sampling sampling = volume.getSampling();
        int ni = sampling.numI();
        int nj = sampling.numJ();
        int nk = sampling.numK();

        int space = 1;
        int startf = Math.min(ni - 1, Math.max(0, 0));
        int endf = ni;
        int delta = endf - startf;
        int slices = (int) Math.floor(delta / (double) space);
        int size = (int) Math.ceil(Math.sqrt(slices));

        int gwidth = size;
        int gheight = size * (size - 1) >= slices ? size - 1 : size;

        Logging.info("slice count: " + slices);
        Logging.info("grid size: " + size);
        Logging.info("grid width: " + gwidth);
        Logging.info("grid height: " + gheight);

        int width = gwidth * nj;
        int height = gheight * nk;
        Volume out = VolumeSource.create(width, height, 1, this.rgb ? 3 : 1);

        for (int a = 0; a < nj; a++)
        {
            for (int b = 0; b < nk; b++)
            {
                for (int c = 0; c < slices; c++)
                {
                    int mi = c % gwidth;
                    int mj = (c - mi) / gheight;

                    int s = startf + c * space;
                    for (int d  = 0; d < out.getDim(); d++)
                    {
                        out.set(mi * nj + a, mj * nk + b, 0, d, volume.get(s, nj - 1 - a, nk - 1 - b, d));
                    }
                }
            }
        }

        return out;
    }

    private Volume mosaicJ(Volume volume)
    {
        Sampling sampling = volume.getSampling();
        int ni = sampling.numI();
        int nj = sampling.numJ();
        int nk = sampling.numK();

        int space = 1;
        int startf = Math.min(nj - 1, Math.max(0, 0));
        int endf = nj;
        int delta = endf - startf;
        int slices = (int) Math.floor(delta / (double) space);
        int size = (int) Math.ceil(Math.sqrt(slices));

        int gwidth = size;
        int gheight = size * (size - 1) >= slices ? size - 1 : size;

        Logging.info("slice count: " + slices);
        Logging.info("grid size: " + size);
        Logging.info("grid width: " + gwidth);
        Logging.info("grid height: " + gheight);

        int width = gwidth * ni;
        int height = gheight * nk;
        Volume out = VolumeSource.create(width, height, 1, this.rgb ? 3 : 1);

        for (int a = 0; a < ni; a++)
        {
            for (int b = 0; b < nk; b++)
            {
                for (int c = 0; c < slices; c++)
                {
                    int mi = c % gwidth;
                    int mj = (c - mi) / gheight;

                    int s = startf + c * space;
                    for (int d  = 0; d < out.getDim(); d++)
                    {
                        out.set(mi * ni + a, mj * nk + b, 0, d, volume.get(ni - 1 - a, s, nk - 1 - b, d));
                    }
                }
            }
        }

        return out;
    }

    private Volume mosaicK(Volume volume)
    {
        Sampling sampling = volume.getSampling();
        int ni = sampling.numI();
        int nj = sampling.numJ();
        int nk = sampling.numK();

        int space = 1;
        int startf = Math.min(nk - 1, Math.max(0, 0));
        int endf = nk;
        int delta = endf - startf;
        int slices = (int) Math.floor(delta / (double) space);
        int size = (int) Math.ceil(Math.sqrt(slices));

        int gwidth = size;
        int gheight = size * (size - 1) >= slices ? size - 1 : size;

        Logging.info("slice count: " + slices);
        Logging.info("grid size: " + size);
        Logging.info("grid width: " + gwidth);
        Logging.info("grid height: " + gheight);

        int width = gwidth * ni;
        int height = gheight * nj;
        Volume out = VolumeSource.create(width, height, 1, this.rgb ? 3 : 1);

        for (int a = 0; a < ni; a++)
        {
            for (int b = 0; b < nj; b++)
            {
                for (int c = 0; c < slices; c++)
                {
                    int mi = c % gwidth;
                    int mj = (c - mi) / gheight;

                    int s = startf + c * space;
                    for (int d  = 0; d < out.getDim(); d++)
                    {
                        out.set(mi * ni + a, mj * nj + b, 0, d, volume.get(ni - 1 - a, nj - 1 - b, s, d));
                    }
                }
            }
        }

        return out;
    }
}
