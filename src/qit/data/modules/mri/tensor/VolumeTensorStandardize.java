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

package qit.data.modules.mri.tensor;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Tensor;
import qit.data.modules.volume.VolumeStandardize;
import qit.data.utils.MatrixUtils;

@ModuleDescription("Standardize the pose of a tensor volume.  This will remove the rotation and tranlation from the image grid and rotate the tensors accordingly")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTensorStandardize implements Module
{
    @ModuleInput
    @ModuleDescription("the input tensor volume")
    public Volume input;

    @ModuleOutput
    @ModuleDescription("the output transformed tensor volume")
    public Volume output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output affine xfm")
    public Affine xfm;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output inverse affine xfm")
    public Affine invxfm;

    public VolumeTensorStandardize run()
    {
        VolumeStandardize std = new VolumeStandardize();
        std.input = this.input;
        std.run();

        Volume out = std.output;
        Affine xfm = std.xfm;
        Affine invxfm = std.invxfm;

        Matrix rot = MatrixUtils.orthogonalize(std.xfm.linear()).inv();

        for (Sample sample : out.getSampling())
        {
            Tensor tensor = new Tensor(out.get(sample));
            for (int i = 0; i < 3; i++)
            {
                Vect oriented = rot.times(tensor.getVec(i)).normalize();
                tensor.setVec(i, oriented);
            }
            out.set(sample, tensor.getEncoding());
        }

        this.output = out;
        this.xfm = xfm;
        this.invxfm = invxfm;

        return this;
    }
}