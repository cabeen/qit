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

package qit.data.modules.mri.model;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.BiTensor;
import qit.data.models.Fibers;
import qit.data.models.Kurtosis;
import qit.data.models.Noddi;
import qit.data.models.Tensor;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.ModelUtils;
import qit.math.structs.VectFunction;

@ModuleDescription("Compute the root mean square error between a model and dwi volume")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Burnham, K. P., & Anderson, D. R. (2004). Multimodel inference: understanding AIC and BIC in model selection. Sociological methods & research, 33(2), 261-304.")
public class VolumeModelError implements Module
{
    public enum ModelErrorType
    {
        MAD, NMAD, MSE, RMSE, NRMSE, BIC, AIC, AICc
    }

    @ModuleInput
    @ModuleDescription("input model volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("input dwi")
    public Volume dwi;

    @ModuleInput
    @ModuleDescription("input gradients")
    public Gradients gradients;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a model name (default will try to detect it)")
    public String model = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("specify an error type (default is root mean square error)")
    public ModelErrorType type = ModelErrorType.RMSE;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify an specific degrees of freedom (useful for models that were constrained when fitting)")
    public Integer dof = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify a NODDI parallel diffusivity")
    public Double dparNoddi = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify a NODDI isotropic diffusivity")
    public Double disoNoddi = null;

    @ModuleOutput
    @ModuleDescription("output error volume")
    public Volume output;

    @Override
    public VolumeModelError run()
    {
        Gradients grads = this.gradients.copy().rotate(this.input.getSampling().quat());

        if (this.disoNoddi != null)
        {
            Noddi.ISOTROPIC = this.disoNoddi;
        }

        if (this.dparNoddi != null)
        {
            Noddi.PARALLEL = this.dparNoddi;
        }

        VectFunction synther = ModelUtils.synth(ModelUtils.select(this.input.getModel(), this.model), this.input.getDim(), grads);

        Volume out = this.input.proto(1);
        for (Sample sample : this.input.getSampling())
        {
            if (this.input.valid(sample, this.mask))
            {
                Vect params = this.input.get(sample);
                Vect meas = this.dwi.get(sample);
                Vect pred = synther.apply(params);

                int n = meas.size();
                int k = this.dof(params);

                switch (this.type)
                {
                    case MAD:
                    {
                        out.set(sample, ModelUtils.mad(meas, pred));
                        break;
                    }
                    case NMAD:
                    {
                        out.set(sample, ModelUtils.nmad(grads, meas, pred));
                        break;
                    }
                    case RMSE:
                    {
                        out.set(sample, ModelUtils.rmse(meas, pred));
                        break;
                    }
                    case MSE:
                    {
                        out.set(sample, ModelUtils.mse(meas, pred));
                        break;
                    }
                    case NRMSE:
                    {
                        out.set(sample, ModelUtils.nrmse(grads, meas, pred));
                        break;
                    }
                    case BIC:
                    {
                        out.set(sample, Math.log(n) * k + n * Math.log(meas.minus(pred).sq().sum() / n));
                        break;
                    }
                    case AIC:
                    {
                        out.set(sample, 2 * k + n * Math.log(meas.minus(pred).sq().sum() / n));
                        break;
                    }
                    case AICc:
                    {
                        out.set(sample, 2 * k + n * Math.log(meas.minus(pred).sq().sum() / n) + 2 * k * (k  + 1) / (n - k - 1));
                        break;
                    }
                    default:
                    {
                        Logging.error("invalid type: " + this.type);
                    }
                }
            }
        }

        this.output = out;

        return this;
    }

    private int dof(Vect params)
    {
        Integer k = this.dof;

        if (k == null)
        {
            switch (this.input.getModel())
            {
                case Tensor:
                    k = new Tensor(params).getDegreesOfFreedom();
                    break;
                case BiTensor:
                    k = new BiTensor(params).getDegreesOfFreedom();
                    break;
                case Fibers:
                    k = new Fibers(params).getDegreesOfFreedom();
                    break;
                case Kurtosis:
                    k = new Kurtosis(params).getDegreesOfFreedom();
                    break;
                case Noddi:
                    k = new Noddi(params).getDegreesOfFreedom();
                    break;
                default:
                    Logging.error("unsupported model: " + this.input.getModel());
            }
        }

        return k;
    }
}
