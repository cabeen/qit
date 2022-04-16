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

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.volume.VolumeRenderColormap;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.colormaps.ColormapDiscrete;
import qit.math.utils.colormaps.ColormapScalar;
import qit.math.utils.colormaps.ColormapSource;

import java.util.List;

@ModuleDescription("Render a volume slice based on a colormap")
@ModuleAuthor("Ryan Cabeen")
public class VolumeRender implements Module
{
    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input background")
    public Volume background = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input foreground colored using a scalar colormap")
    public Volume foreground = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input labels colored using a discrete colormap")
    public Mask labels = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input foreground mask")
    public Mask fgmask = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input background mask")
    public Mask bgmask = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("input slice range, e.g. :,:,80")
    public String range = "all";

    @ModuleParameter
    @ModuleDescription("background colormap")
    public String bgmap = ColormapSource.GRAYSCALE;

    @ModuleParameter
    @ModuleDescription("background lower bound (supports statistics like min, max, etc)")
    public String bglow = "0";

    @ModuleParameter
    @ModuleDescription("background upper bound (supports statistics like min, max, etc)")
    public String bghigh = "1";

    @ModuleParameter
    @ModuleDescription("scalar colormap for coloring the volume")
    public String fgmap = ColormapSource.GRAYSCALE;

    @ModuleParameter
    @ModuleDescription("scalar lower bound (supports statistics like min, max, etc)")
    public String fglow = "0";

    @ModuleParameter
    @ModuleDescription("scalar upper bound (supports statistics like min, max, etc)")
    public String fghigh = "1";

    @ModuleParameter
    @ModuleDescription("scalar lower bound for range (0 to 1)")
    public String fgrlow = "0";

    @ModuleParameter
    @ModuleDescription("scalar upper bound for range (0 to 1)")
    public String fgrhigh = "1";

    @ModuleParameter
    @ModuleDescription("discrete colormap for coloring the label volume")
    public String discrete = ColormapSource.WHITE;

    @ModuleParameter
    @ModuleDescription("invert colormap")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("wash out colors")
    public double wash = 0.0;

    @ModuleParameter
    @ModuleDescription("blending of background with foreground")
    public double alpha = 1.0;

    @ModuleParameter
    @ModuleDescription("a label for the colormap")
    public String label = "attribute";

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output RGB volume rendering")
    public Volume output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output RGB colormap rendering")
    public Volume colormap;

    public VolumeRender run()
    {
        ColormapDiscrete discreteColormap = ColormapSource.getDiscrete(this.discrete);

        ColormapScalar backgroundColormap = ColormapSource.getScalar(this.bgmap);
        backgroundColormap.withMin(parse(this.bglow, this.background));
        backgroundColormap.withMax(parse(this.bghigh, this.background));
        VectFunction backgroundFunction = backgroundColormap.getFunction();

        ColormapScalar foregroundColormap = ColormapSource.getScalar(this.fgmap);
        foregroundColormap.withMin(parse(this.fglow, this.foreground));
        foregroundColormap.withMax(parse(this.fghigh, this.foreground));

        double fgrlowval = Double.valueOf(this.fgrlow);
        double fgrhighval = Double.valueOf(this.fgrhigh);

        if (this.invert)
        {
            Logging.info("inverting colormap");
            double tmp = fgrlowval;
            fgrlowval = fgrhighval;
            fgrhighval = tmp;
        }

        List<Pair<Double, Double>> fgtransfer = Lists.newArrayList();
        fgtransfer.add(Pair.of(0.0, fgrlowval));
        fgtransfer.add(Pair.of(1.0, fgrhighval));
        foregroundColormap.withTransfer(fgtransfer);

        VectFunction foregroundFunction = foregroundColormap.getFunction();
        Volume renderedColormap = new VolumeRenderColormap().withColormap(foregroundColormap).withLabel(this.label).getOutput();
        this.colormap = renderedColormap;

        Sampling inputSampling = null;

        if (this.background != null)
        {
            inputSampling = this.background.getSampling();
        }
        else if (this.foreground != null)
        {
            inputSampling = this.foreground.getSampling();
        }
        else if (this.labels != null)
        {
            inputSampling = this.labels.getSampling();
        }

        if (inputSampling != null)
        {
            Sampling outputSampling = inputSampling.range(this.range);
            Volume out = VolumeSource.create(outputSampling, 3);

            boolean blend = !MathUtils.unit(this.alpha);
            for (Sample outputSample : outputSampling)
            {
                Sample inputSample = inputSampling.nearest(outputSampling.world(outputSample));

                Vect render = VectSource.create3D(0, 0, 0);

                if (this.background != null && this.background.valid(inputSample, this.bgmask))
                {
                    Vect value = this.background.get(inputSample);
                    Vect color = backgroundFunction.apply(value);
                    render = color;
                }

                if (this.labels != null && this.labels.valid(inputSample, this.fgmask))
                {
                    int value = this.labels.get(inputSample);
                    if (value != 0)
                    {
                        Vect color = discreteColormap.getColor(value);
                        render = blend ? render.times(1 - this.alpha).plus(this.alpha, color) : color;
                    }
                }

                if (this.foreground != null && this.foreground.valid(inputSample, this.fgmask))
                {
                    Vect value = this.foreground.get(inputSample);
                    Vect color = foregroundFunction.apply(value);
                    render = blend ? render.times(1 - this.alpha).plus(this.alpha, color) : color;
                }

                out.set(outputSample, render);
            }

            this.output = out;
        }

        return this;
    }

    private static double parse(String value, Volume reference)
    {
        if (MathUtils.number(value))
        {
            return Double.valueOf(value);
        }
        else
        {
            VolumeVoxelStats stats = new VolumeVoxelStats().withInput(reference).run();

            switch (value)
            {
                case "zero":
                    return 0;
                case "half":
                    return 0.5;
                case "one":
                    return 1;
                case "neghalf":
                    return -0.5;
                case "negone":
                    return -1;
                case "mean":
                    return stats.mean;
                case "median":
                    return stats.median;
                case "min":
                    return stats.min;
                case "max":
                    return stats.max;
                case "qlow":
                    return stats.qlow;
                case "qhigh":
                    return stats.qhigh;
                case "threeup":
                    return stats.mean + 3 * stats.std;
                case "threedown":
                    return stats.mean - 3 * stats.std;
                case "twoup":
                    return stats.mean + 2 * stats.std;
                case "twodown":
                    return stats.mean - 2 * stats.std;
                case "oneup":
                    return stats.mean + 1 * stats.std;
                case "onedown":
                    return stats.mean - 1 * stats.std;
            }

            throw new RuntimeException("invalid value: " + value);
        }
    }
}
