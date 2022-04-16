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
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

@ModuleDescription("Create particles representing a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeParticles implements Module
{
    enum ParticleType { Constant, Density }
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("at most, produce this many samples per voxel")
    public int samples = 4;

    @ModuleParameter
    @ModuleDescription("sample particles adaptively, that is, most particles with higher image intensities")
    public boolean adaptive;

    @ModuleParameter
    @ModuleDescription("specify a threshold for background removal, e.g. 0.01 (based on input image intensities)")
    public double thresh = 0.0;

    @ModuleParameter
    @ModuleDescription("use the given type of contrast enhancement to normalize the image")
    public VolumeEnhanceContrast.VolumeEnhanceContrastType type = VolumeEnhanceContrast.VolumeEnhanceContrastType.Ramp;

    @ModuleParameter
    @ModuleDescription("use the given image interpolation method")
    public InterpolationType interp = InterpolationType.Nearest;

    @ModuleParameter
    @ModuleDescription("output filename (should end in csv)")
    public String output;

    public VolumeParticles run()
    {
        Sampling sampling = this.input.getSampling();
        VectFunction coloring = this.color();
        Pair<Double,Double> minmax = this.minmax();

        double min = minmax.a;
        double delta = MathUtils.eq(minmax.a, minmax.b) ? 1.0 : minmax.b - minmax.a;

        try
        {
            Logging.info("started writing particles");
            PathUtils.mkpar(this.output);
            PrintWriter pw = new PrintWriter(new BufferedOutputStream(new FileOutputStream(this.output)));

            pw.write("float32 Position[0], float32 Position[1], float32 Position[2],");
            pw.write("float32 Color[0], float32 Color[1], float32 Color[2]\n");
            for (int k = 0; k < sampling.numK(); k++)
            {
                Logging.info(String.format("... on slice %d of %d", k+1, sampling.numK()));
                for (int j = 0; j < sampling.numJ(); j++)
                {
                    for (int i = 0; i < sampling.numI(); i++)
                    {
                        double value = this.input.get(i, j, k).norm();
                        if (this.input.valid(i, j, k, this.mask) && value >= this.thresh)
                        {
                            int count = this.samples;

                            if (this.adaptive)
                            {
                                count *= (value - min) / delta;
                            }

                            for (int c = 0; c < count; c++)
                            {
                                Vect pos = sampling.random(i, j, k);
                                pw.write(String.format("%g,%g,%g,", pos.getX(), pos.getY(), pos.getZ()));

                                Vect color = coloring.apply(pos);
                                pw.write(String.format("%g,%g,%g\n", color.getX(), color.getY(), color.getZ()));
                            }
                        }
                    }
                }
            }

            pw.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Logging.info("finished writing particles");

        return this;
    }

    private Pair<Double,Double> minmax()
    {
        Double min = null;
        Double max = null;

        for (Sample sample : this.input.getSampling())
        {
            if (this.input.valid(sample, this.mask))
            {
                double value = this.input.get(sample).norm();

                min = (min == null) ? value : Math.min(min, value);
                max = (max == null) ? value : Math.max(max, value);
            }
        }

        return Pair.of(min, max);
    }

    private VectFunction color()
    {
        if (this.input.getDim() == 3)
        {
            Logging.info("detected RGB image");
            return VolumeUtils.interp(this.interp, this.input);
        }
        else if (this.input.getDim() == 4)
        {
            Logging.info("detected RGBA image");
            boolean[] filter = new boolean[]{true, true, true, false};
            return VolumeUtils.interp(this.interp, this.input).compose(VectFunctionSource.subset(filter));
        }
        else if (this.input.getDim() == 1)
        {
            Logging.info("detected grayscale image");
            VolumeEnhanceContrast module = new VolumeEnhanceContrast();
            module.input = this.input;
            module.mask = this.mask;
            module.thresh = this.thresh;
            module.type = this.type;
            Volume normalized = module.run().output;
            return VolumeUtils.interp(this.interp, normalized).compose(VectFunctionSource.repeat(1, 3));
        }
        else
        {
            throw new RuntimeException("multi-channel images are not supported");
        }
    }
}
