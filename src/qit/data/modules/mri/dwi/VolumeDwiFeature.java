/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.modules.mri.dwi;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Compute features of the the diffusion weighted signal.  These features are statistical summaries of the signal within and across shells (depending on the method), mostly avoiding any modeling assumptions")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiFeature implements Module
{
    enum VolumeDwiFeatureType
    {
        Attenuation, AnisoCoeffVar, AnisoStd, AnisoMu, SphericalMean, SphericalMeanNorm, SphericalMeanADC, SphericalStd, NoiseStd, NoiseCoeffVar,
    }

    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the input gradients")
    public Gradients gradients;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the method for computing anisotropy")
    public VolumeDwiFeatureType feature = VolumeDwiFeatureType.AnisoCoeffVar;

    @ModuleOutput
    @ModuleDescription("the output anisotropy volume")
    public Volume output;

    public VolumeDwiFeature run()
    {
        this.output = new VolumeFunction(this.aniso()).withInput(this.input).withMask(this.mask).withThreads(1).run();

        return this;
    }

    private VectFunction aniso()
    {
        List<List<Integer>> shells = shells(this.gradients, false);
        List<List<Integer>> bshells = shells(this.gradients, true);

        switch (VolumeDwiFeature.this.feature)
        {
            case Attenuation:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        double mean = VolumeDwiFeature.this.gradients.zero(input);
                        for (int i = 0; i < input.size(); i++)
                        {
                            output.set(i, input.get(i) / mean);
                        }
                    }
                }.init(this.input.getDim(), this.input.getDim());
            case AnisoCoeffVar:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        Vect signal = VolumeDwiFeature.this.gradients.adc(input);
                        Vect values = VectSource.createND(shells.size());
                        for (int j = 0; j < shells.size(); j++)
                        {
                            Vect sub = signal.sub(shells.get(j));
                            values.set(j, sub.std() / sub.mean());
                        }

                        output.set(0, values.mean());
                    }
                }.init(this.input.getDim(), 1);
            case AnisoStd:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        Vect signal = VolumeDwiFeature.this.gradients.adc(input);
                        Vect values = VectSource.createND(shells.size());
                        for (int j = 0; j < shells.size(); j++)
                        {
                            Vect sub = signal.sub(shells.get(j));
                            values.set(j, sub.std());
                        }

                        output.set(0, values.mean());
                    }
                }.init(this.input.getDim(), 1);
            case AnisoMu:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        Vect signal = VolumeDwiFeature.this.gradients.norm(input);
                        Vect values = VectSource.createND(shells.size());
                        for (int j = 0; j < shells.size(); j++)
                        {
                            Vect sub = signal.sub(shells.get(j));
                            values.set(j, sub.std());
                        }
                        output.set(0, values.mean());
                    }
                }.init(this.input.getDim(), 1);
            case SphericalMean:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        for (int j = 0; j < bshells.size(); j++)
                        {
                            double mean = input.sub(bshells.get(j)).mean();
                            output.set(j, MathUtils.valid(mean) ? mean : 0.0);
                        }
                    }
                }.init(this.input.getDim(), bshells.size());
            case SphericalMeanNorm:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        double base = input.sub(bshells.get(0)).mean();
                        if (MathUtils.nonzero(base))
                        {
                            for (int j = 1; j < bshells.size(); j++)
                            {
                                double norm = input.sub(bshells.get(j)).mean() / base;
                                output.set(j - 1, MathUtils.valid(norm) ? norm : 0.0);
                            }
                        }
                    }
                }.init(this.input.getDim(), bshells.size() - 1);
            case SphericalStd:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        for (int j = 0; j < bshells.size(); j++)
                        {
                            double var = input.sub(bshells.get(j)).std();
                            output.set(j, MathUtils.valid(var) ? var : 0.0);
                        }
                    }
                }.init(this.input.getDim(), bshells.size());
            case SphericalMeanADC:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        Vect adc = VolumeDwiFeature.this.gradients.adc(input);
                        for (int j = 0; j < shells.size(); j++)
                        {
                            double mean = adc.sub(shells.get(j)).mean();
                            output.set(j, MathUtils.valid(mean) ? mean : 0.0);
                        }
                    }
                }.init(this.input.getDim(), bshells.size());
            case NoiseStd:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        output.set(0, VolumeDwiFeature.this.gradients.zeros(input).std());
                    }
                }.init(this.input.getDim(), 1);
            case NoiseCoeffVar:
                return new VectFunction()
                {
                    public void apply(Vect input, Vect output)
                    {
                        Vect zeros = VolumeDwiFeature.this.gradients.zeros(input);
                        output.set(0, zeros.std() / (zeros.mean() + 1e-6));
                    }
                }.init(this.input.getDim(), 1);
            default:
                throw new RuntimeException("invalid feature: " + this.feature);
        }
    }

    private static List<List<Integer>> shells(Gradients gradients, boolean baseline)
    {
        List<List<Integer>> shells = Lists.newArrayList();
        for (int shell : gradients.getShells(baseline))
        {
            shells.add(gradients.getShellsIdx(shell));
        }

        return shells;
    }
}
