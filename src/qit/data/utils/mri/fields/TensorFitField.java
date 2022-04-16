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

package qit.data.utils.mri.fields;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.fitting.FitTensorLLS;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.structs.StreamlineField;
import qit.data.models.Tensor;
import qit.math.structs.VectFunction;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class TensorFitField extends StreamlineField
{
    private Set<String> attrs = Sets.newHashSet(Tensor.FEATURES);
    private VectFunction fit;
    private VectFunction pd;
    private VectFunction[] index;
    private Map<String, Integer> lookup;

    public TensorFitField(Volume dwi, Gradients gradients, InterpolationType interp)
    {
        FitTensorLLS lls = new FitTensorLLS();
        lls.gradients = gradients;

        this.fit = VolumeUtils.interp(interp, dwi).compose(lls.get());
        this.pd = new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(new Tensor(input).getVec(0));
            }
        }.init(Tensor.DT_DIM, 3);

        this.lookup = Maps.newHashMap();
        this.index = new VectFunction[Tensor.FEATURES.length];
        for (int i = 0; i < Tensor.FEATURES.length; i++)
        {
            String name = Tensor.FEATURES[i];
            this.index[i] = ModelUtils.feature(new Tensor(), name);
            this.lookup.put(name, i);
        }
    }

    public List<StreamSample> getSamples(Vect pos)
    {
        Vect ten = this.fit.apply(pos);
        StreamSample fiber = new TensorFiber(pos, ten);
        return Lists.newArrayList(fiber);
    }

    public String getAttr()
    {
        return Tensor.FEATURES_FA;
    }

    public class TensorFiber extends StreamSample
    {
        Vect tensor;
        Vect[] attr = new Vect[TensorFitField.this.index.length];

        public TensorFiber(Vect p, Vect ten)
        {
            super(p, TensorFitField.this.pd.apply(ten));
            this.tensor = ten;
        }

        public Set<String> getAttrs()
        {
            return TensorFitField.this.attrs;
        }

        public Vect getAttr(String name)
        {
            int idx = TensorFitField.this.lookup.get(name);
            if (this.attr[idx] == null)
            {
                this.attr[idx] = TensorFitField.this.index[idx].apply(this.tensor);
            }

            return this.attr[idx];
        }
    }
}