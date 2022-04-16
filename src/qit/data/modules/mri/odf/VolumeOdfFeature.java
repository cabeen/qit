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

package qit.data.modules.mri.odf;

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
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;

import java.util.function.Supplier;

@ModuleDescription("Estimate spherical harmonics representing the ODF.  SUpported features include: generalized fractional anisotropy (GFA) and normalized entropy (NE)")
@ModuleAuthor("Ryan Cabeen")
public class VolumeOdfFeature implements Module
{
    public static final String GFA = "GFA"; // Generalized fractional anisotropy
    public static final String NE = "NE"; // Normalized entropy

    @ModuleInput
    @ModuleDescription("the input ODF volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the name of the feature (GFA, NE)")
    public String name = GFA;

    @ModuleParameter
    @ModuleDescription("the number of threads in the pool")
    public Integer threads = 1;

    @ModuleOutput
    @ModuleDescription("the output spharm volume")
    public Volume output;

    public VolumeOdfFeature run()
    {
        this.output = new VolumeFunction(factory(this.input.getDim())).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        return this;
    }

    public Supplier<VectFunction> factory(final int dim)
    {
        return () ->
        {
            if (GFA.equals(VolumeOdfFeature.this.name))
            {
                return new VectFunction()
                {
                    public void apply(Vect input1, Vect output1)
                    {
                        output1.set(0, gfa(input1));
                    }
                }.init(dim, 1);
            }
            else if (NE.equals(VolumeOdfFeature.this.name))
            {
                return new VectFunction()
                {
                    public void apply(Vect input1, Vect output1)
                    {
                        output1.set(0, ne(input1));
                    }
                }.init(dim, 1);
            }
            else
            {
                Logging.error("invalid feature: " + name);
                return null;
            }
        };
    }

    public static double gfa(Vect input)
    {
        double std = new VectStats().withInput(input).run().std;
        double mse = new VectStats().withInput(input.sq()).run().mean;
        double gfa = std / Math.sqrt(mse);

        return gfa;
    }

    public static double ne(Vect input)
    {
        int n = input.size();
        double log = new VectStats().withInput(input.log()).run().mean;
        double ne = Double.isNaN(log) || Double.isInfinite(log) ? 0 : -n * log/ Math.log(n);

        return ne;
    }
}
