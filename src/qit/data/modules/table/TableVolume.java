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

package qit.data.modules.table;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

import java.util.Collections;
import java.util.List;

@ModuleDescription("Map tabular data to a volume")
@ModuleAuthor("Ryan Cabeen")
public class TableVolume implements Module
{
    @ModuleInput
    @ModuleDescription("input table")
    public Table input;

    @ModuleInput
    @ModuleDescription("a reference mask")
    public Mask reference;

    @ModuleParameter
    @ModuleDescription("read vector values")
    public boolean vector = false;

    @ModuleParameter
    @ModuleDescription("index")
    public String voxel = "index";

    @ModuleParameter
    @ModuleDescription("value")
    public String value = "value";

    @ModuleParameter
    @ModuleDescription("a value to substitute for NAs")
    public double na = 0;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output;

    @Override
    public TableVolume run()
    {
        boolean foundI = false;
        boolean foundJ = false;
        boolean foundK = false;
        boolean foundV = false;

        List<Integer> dimidx = Lists.newArrayList();

        for (String field : this.input.getFields())
        {
            if (field.equals(this.voxel + "_i"))
            {
                foundI = true;
            }

            if (field.equals(this.voxel + "_j"))
            {
                foundJ = true;
            }

            if (field.equals(this.voxel + "_k"))
            {
                foundK = true;
            }

            if (field.equals(this.voxel))
            {
                foundV = true;
            }

            if (field.startsWith(this.value))
            {
                if (this.vector)
                {
                    int idx = Integer.valueOf(field.split("_")[1]);
                    dimidx.add(idx);
                }
                else
                {
                    dimidx.add(0);
                }
            }
        }

        boolean ijk = foundI || foundJ || foundK;

        if (!ijk && !foundV)
        {
            Logging.error("table does not encode voxels");
        }

        if (dimidx.size() == 0)
        {
            Logging.error("table does not image data");
        }

        int dim = Collections.max(dimidx) + 1;
        Volume out = this.reference.protoVolume(dim);
        Sampling sampling = out.getSampling();
        for (Integer key : this.input.getKeys())
        {
            Record row = this.input.getRecord(key);

            Sample sample;
            if (ijk)
            {
                int i = Integer.valueOf(row.get(this.voxel + "_i").toString());
                int j = Integer.valueOf(row.get(this.voxel + "_j").toString());
                int k = Integer.valueOf(row.get(this.voxel + "_k").toString());
                sample = new Sample(i, j, k);
            }
            else
            {
                int v = Integer.valueOf(row.get(this.voxel).toString());
                sample = sampling.sample(v);
            }

            Vect values = VectSource.createND(dim);
            if (this.vector)
            {

                for (Integer idx : dimidx)
                {
                    double val = MathUtils.parse(row.get(this.value + "_" + idx).toString(), this.na);
                    values.set(idx, val);
                }
            }
            else
            {
                double val = MathUtils.parse(row.get(this.value).toString(), this.na);
                values.set(0, val);
            }

            out.set(sample, values);
        }

        this.output = out;

        return this;
    }
}
