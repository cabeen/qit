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
import qit.data.models.Tensor;
import qit.data.utils.mri.ModelUtils;
import qit.math.structs.VectFunction;

import java.util.List;

@ModuleUnlisted
@ModuleDescription("Randomly sample tensors and the signal values associated with the given gradients.")
@ModuleAuthor("Ryan Cabeen")
public class GradientTensorSample implements Module
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
    @ModuleDescription("the mean for the L1 gaussian")
    public Double meanL1 = 9.0E-4;

    @ModuleParameter
    @ModuleDescription("the std for the L1 gaussian")
    public Double stdL1 = 2.5E-4;

    @ModuleParameter
    @ModuleDescription("the mean for the L1 gaussian")
    public Double meanL2 = 7.0E-4;

    @ModuleParameter
    @ModuleDescription("the std for the L1 gaussian")
    public Double stdL2 = 2.50E-4;

    @ModuleParameter
    @ModuleDescription("the mean for the L1 gaussian")
    public Double meanL3 = 6.0E-4;

    @ModuleParameter
    @ModuleDescription("the std for the L1 gaussian")
    public Double stdL3 = 2.5E-4;

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
    public String features = Tensor.FEATURES_FA;

    @ModuleOutput
    @ModuleDescription("the output sampled values")
    public Table output;

    public GradientTensorSample run()
    {
        List<String> feats = Lists.newArrayList(this.features.split(","));

        int dim = this.gradients.size();

        VectFunction synth = Tensor.synth(this.gradients);

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
            Tensor tensor = sample();
            Vect signal = synth.apply(tensor.getEncoding());

            Record record = new Record();
            for (int j = 0; j < dim; j++)
            {
                record.with(String.format(this.signal, j), String.valueOf(signal.get(j)));
            }
            for (String feature : feats)
            {
                double value = tensor.feature(feature).get(0);
                record.with(feature, String.valueOf(value));
            }

            out.addRecord(record);
        }

        this.output = out;

        return this;
    }

    private Tensor sample()
    {
        Vect v1 = this.orient ? VectSource.create3D(1, 0, 0) : VectSource.randomUnit();
        Vect v2 = this.orient ? VectSource.create3D(0, 1, 0) : v1.perp();
        Vect v3 = this.orient ? VectSource.create3D(0, 0, 1) : v1.cross(v2);
        double l1 = this.meanL1 + this.stdL1 * Global.RANDOM.nextGaussian();
        double l2 = this.meanL2 + this.stdL2 * Global.RANDOM.nextGaussian();
        double l3 = this.meanL3 + this.stdL3 * Global.RANDOM.nextGaussian();

        Tensor tensor = new Tensor();
        tensor.setBaseline(this.baseline);
        tensor.setVec(0, v1);
        tensor.setVec(1, v2);
        tensor.setVec(2, v3);
        tensor.setVal(0, l1);
        tensor.setVal(1, l2);
        tensor.setVal(2, l3);

        return tensor;
    }
}
