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

package qit.data.modules.vects;

import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectsUtils;
import qit.data.utils.vects.register.*;
import qit.math.utils.MathUtils;

@ModuleDescription("Estimate a linear transform to register a given pair of vects.  You can choose one of several methods to specify the degrees of freedom of the transform and how the transform parameters are estimated")
@ModuleAuthor("Ryan Cabeen, Yonggang Shi")
public class VectsRegisterLinear implements Module
{
    public enum VectsRegisterLinearMethod {AffineLeastSquares, RigidDualQuaternion};

    @ModuleInput
    @ModuleDescription("input source vects")
    public Vects source;

    @ModuleInput
    @ModuleDescription("input dest vects (should match source)")
    public Vects dest;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input weights (only available for some methods)")
    public Vects weights;

    @ModuleParameter
    @ModuleDescription("the registration method (each has different degrees of freedom and estimation techniques)")
    public VectsRegisterLinearMethod method = VectsRegisterLinearMethod.AffineLeastSquares.AffineLeastSquares;

    @ModuleOutput
    @ModuleDescription("output affine transform mapping source to dest")
    public Affine output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output transformed source vects (redundant, but useful for validation)")
    public Vects transformed;

    @Override
    public VectsRegisterLinear run()
    {
        this.output = estimate(this.weights != null ? this.weights.flatten() : null, this.source, this.dest);
        this.transformed = VectsUtils.apply(this.source, this.output);

        return this;
    }

    public Affine estimate(Vect weights, Vects source, Vects dest)
    {
        switch(this.method)
        {
            case AffineLeastSquares:
                return VectsRegisterAffineLeastSquares.estimate(source, dest);
            case RigidDualQuaternion:
                return qit.data.utils.vects.register.VectsRegisterRigidDualQuaternion.estimate(weights, source, dest);
            default:
                throw new RuntimeException("error: unimplemented method: " + this.method);
        }
    }
}
