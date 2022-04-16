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

package qit.data.modules.mri.relaxometry;

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
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.ExpDecay;
import qit.data.utils.volume.VolumeBinaryFunction;
import qit.math.structs.BinaryVectFunction;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

@ModuleDescription("Synthesize a volume from exp decay parameters")
@ModuleAuthor("Ryan Cabeen")
public class VolumeExpDecayError implements Module
{
    public static final String ME = "me";
    public static final String NME = "nme";
    public static final String SSE = "sse";
    public static final String NSSE = "nsse";
    public static final String MSE = "mse";
    public static final String NMSE = "nmse";
    public static final String RMSE = "rmse";
    public static final String NRMSE = "nrmse";

    @ModuleInput
    @ModuleDescription("input exp decay sample volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the varying parameters")
    public Vects varying;

    @ModuleInput
    @ModuleDescription("the exponential decay model parameter volume")
    public Volume model;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the type of error metric (me, nme, sse, nsse, mse, nmse, rmse, nrmse)")
    public String method = NRMSE;

    @ModuleOutput
    @ModuleDescription("output error")
    public Volume output;

    @Override
    public VolumeExpDecayError run()
    {
        final VectFunction synther = ExpDecay.synth(this.varying.flatten());
        BinaryVectFunction error = new BinaryVectFunction()
        {

            @Override
            public void apply(Vect left, Vect right, Vect output)
            {
                ExpDecay model = new ExpDecay(right);
                Vect prediction = synther.apply(right);
                Vect measurement = left;

                double base = model.getAlpha();
                base = MathUtils.zero(base) ? 1.0 : base;
                double base2 = base * base;

                Vect e = prediction.minus(measurement);
                Vect se = e.sq();

                double me = e.mean();
                double nme = me / base;
                double sse = se.sum();
                double nsse = sse / base2;
                double mse = se.mean();
                double nmse = mse / base2;
                double rmse = Math.sqrt(mse);
                double nrmse = rmse / base;

                switch(VolumeExpDecayError.this.method)
                {
                    case ME:
                        output.set(0, me);
                        return;
                    case NME:
                        output.set(0, nme);
                        return;
                    case SSE:
                        output.set(0, sse);
                        return;
                    case NSSE:
                        output.set(0, nsse);
                        return;
                    case MSE:
                        output.set(0, mse);
                        return;
                    case NMSE:
                        output.set(0, nmse);
                        return;
                    case RMSE:
                        output.set(0, rmse);
                        return;
                    case NRMSE:
                        output.set(0, nrmse);
                        return;
                    default:
                        Logging.error("invalid method: " + VolumeExpDecayError.this.method);
                }
            }
        }.init(this.varying.size(), new ExpDecay().getEncodingSize(), 1);

        Volume out = new VolumeBinaryFunction(error).withLeft(this.input).withRight(this.model).withMask(this.mask).getOutput();

        this.output = out;

        return this;
    }
}
