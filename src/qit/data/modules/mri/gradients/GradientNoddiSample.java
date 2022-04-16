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

package qit.data.modules.mri.gradients;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.mri.structs.Gradients;
import qit.data.models.Noddi;
import qit.data.utils.mri.ModelUtils;
import qit.math.structs.VectFunction;

import java.util.List;

@ModuleUnlisted
@ModuleDescription("Sample Noddis and the signal values associated with the given gradients.")
@ModuleAuthor("Ryan Cabeen")
public class GradientNoddiSample implements Module
{
    @ModuleInput
    @ModuleDescription("the input gradients")
    public Gradients gradients;

    @ModuleParameter
    @ModuleDescription("the number of samples")
    public Integer samples = 5000;

    @ModuleParameter
    @ModuleDescription("the baseline value (one is recommended)")
    public Double baseline = 1.0;

    @ModuleParameter
    @ModuleDescription("use a fixed orientation (otherwise this is uniform on the sphere)")
    public boolean orient = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the Rician noise level in (SNR dB)")
    public Double rician = null;

    @ModuleParameter
    @ModuleDescription("the formatting for signal fields")
    public String signal = "signal%d";

    @ModuleParameter
    @ModuleDescription("the model features to include (comma separated)")
    public String features = Noddi.ODI;

    @ModuleOutput
    @ModuleDescription("the output sampled values")
    public Table output;

    public GradientNoddiSample run()
    {
        List<String> feats = Lists.newArrayList(this.features.split(","));

        int dim = this.gradients.size();

        VectFunction synth = Noddi.synth(this.gradients);

        if (this.rician != null)
        {
            double noise = ModelUtils.std(this.baseline, this.rician);
            synth = synth.compose(ModelUtils.ricianND(dim, noise));
        }

        Table out = new Table();
        for (int i = 0; i < dim; i++)
        {
            out.withField(String.format(this.signal, i));
        }
        for (String feature : feats)
        {
            out.withField(feature);
        }

        for (int i = 0; i < this.samples; i++)
        {
            Noddi Noddi = sample();
            Vect signal = synth.apply(Noddi.getEncoding());

            Record record = new Record();
            for (int j = 0; j < dim; j++)
            {
                record.with(String.format(this.signal, j), String.valueOf(signal.get(j)));
            }
            for (String feature : feats)
            {
                double value = Noddi.feature(feature).get(0);
                record.with(feature, String.valueOf(value));
            }

            out.addRecord(record);
        }

        this.output = out;

        return this;
    }

    private Noddi sample()
    {
        Vect dir = this.orient ? VectSource.create3D(1, 0, 0) : VectSource.randomUnit();
        double od = Global.RANDOM.nextDouble();
        double icvf = Global.RANDOM.nextDouble();
        double isovf = 0.0; // Global.RANDOM.nextDouble();

        Noddi noddi = new Noddi();
        noddi.setBaseline(this.baseline);
        noddi.setDir(dir);
        noddi.setODI(od);
        noddi.setFISO(isovf);
        noddi.setFICVF(icvf);

        return noddi;
    }
}
