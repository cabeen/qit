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

package qit.data.modules.mri.noddi;

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.KernelInterpolationType;
import qit.data.utils.enums.ReorientationType;
import qit.data.utils.volume.VolumeSample;
import qit.data.models.Noddi;
import qit.data.utils.mri.estimation.NoddiEstimator;
import qit.data.utils.mri.estimation.VolumeKernelModelEstimator;
import qit.math.structs.VectFunction;
import qit.math.source.VectFunctionSource;

@ModuleDescription("Transform a noddi volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeNoddiTransform implements Module
{
    @ModuleInput
    @ModuleDescription("the input noddi volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("input reference volume")
    public Volume reference;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask (defined in the reference space)")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("use an input mask (defined in the input space)")
    public Mask inputMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an affine xfm")
    public Affine affine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an inverse affine xfm")
    public Affine invaffine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply a deformation xfm")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("reverse the order, i.e. compose the affine(deform(x)), whereas the default is deform(affine(x))")
    public boolean reverse = false;

    @ModuleParameter
    @ModuleDescription("specify a reorient method")
    public ReorientationType reorient = ReorientationType.Jacobian;

    @ModuleParameter
    @ModuleDescription("the interpolation type")
    public KernelInterpolationType interp = KernelInterpolationType.Trilinear;

    @ModuleParameter
    @ModuleDescription("the filter radius in voxels")
    public Integer support = 3;

    @ModuleParameter
    @ModuleDescription("the positional bandwidth in mm")
    public Double hpos = 1.0;

    @ModuleParameter
    @ModuleDescription("specify an estimation method")
    public String estimation = NoddiEstimator.DEFAULT_ESTIMATION ;

    @ModuleOutput
    @ModuleDescription("the output transformed noddi volume")
    public Volume output;

    public VolumeNoddiTransform run()
    {
        Sampling sampling = this.reference.getSampling();
        Volume myinput = this.input;

        if (this.inputMask != null)
        {
            myinput = VolumeUtils.mask(myinput, this.inputMask);
        }

        VectFunction xfm = VolumeUtils.xfm(this.affine, this.invaffine, this.deform, this.reverse);

        NoddiEstimator estimator = new NoddiEstimator();
        estimator.parse(this.estimation);

        VolumeKernelModelEstimator vestimator = new VolumeKernelModelEstimator(new Noddi());
        vestimator.estimator = estimator;
        vestimator.volume = myinput;
        vestimator.support = this.support;
        vestimator.hpos = this.hpos;
        vestimator.interp = this.interp;

        Sampling reference = sampling;
        int size = reference.size();
        int step = size / 50;

        Logging.progress("resampling noddi");
        Volume out = VolumeSource.create(reference, myinput.getDim());
        out.setModel(myinput.getModel());

        for (int idx = 0; idx < size; idx++)
        {
            if (idx % step == 0)
            {
                Logging.progress(String.format("%d percent processed", 100 * idx / (size - 1)));
            }

            if (!out.valid(idx, this.mask))
            {
                continue;
            }

            Vect source = reference.world(idx);
            Vect dest = xfm.apply(source);
            Vect sampled = vestimator.estimate(dest);

            if (sampled != null)
            {
                out.set(idx, sampled);
            }
        }

        Logging.progress("reorienting");
        Volume vxfm = new VolumeSample().withSampling(reference).withFunction(xfm).getOutput();

        Matrix[] map = VolumeUtils.reorient(vxfm, this.mask, this.reorient);
        for (int idx = 0; idx < size; idx++)
        {
            if (idx % step == 0)
            {
                Logging.progress(String.format("%d percent processed", 100 * idx / (size - 1)));
            }

            if (out.valid(idx, this.mask))
            {
                continue;
            }

            Noddi noddi = new Noddi(out.get(idx));

            // don't change fibers with no associated transform
            if (map[idx] != null)
            {
                Matrix rxfm = map[idx].inv();
                Vect oriented = rxfm.times(noddi.getDir()).normalize();
                noddi.setDir(oriented);
                out.set(idx, noddi.getEncoding());
            }
        }

        this.output = out;
        return this;
    }
}