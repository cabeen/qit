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


package qit.data.modules.volume;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.math.structs.VectFunction;

import java.util.List;

@ModuleDescription("Sample a volume along a polyline.  The results are stored in table along with the world coordinates and position along the segment")
@ModuleAuthor("Ryan Cabeen")
public class VolumeSampleLine implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("input vects (two points in a vects object)")
    public Vects vects;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the number of samples")
    public int num = 5;

    @ModuleParameter
    @ModuleDescription("image interpolation method")
    public InterpolationType interp = InterpolationType.Trilinear;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the name of the volume value field")
    public String value = "value";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("a which of dimensions to use")
    public String dims;

    @ModuleParameter
    @ModuleDescription("include vector values")
    public boolean vector = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the field for the x coordinate")
    public String vx = "x";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the field for the y coordinate")
    public String vy = "y";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the field for the z coordinate")
    public String vz = "z";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the position along the line segment in mm")
    public String pos = "posCamera";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the index along the line segment")
    public String idx = "idx";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public Module run()
    {
        VectFunction sampler = VolumeUtils.interp(this.interp, this.input);

        List<Integer> dimidx = Lists.newArrayList();
        if (this.dims != null)
        {
            for (String dim : this.dims.split(" "))
            {
                dimidx.add(Integer.valueOf(dim));
            }
        }
        else
        {
            for (int i = 0; i < this.input.getDim(); i++)
            {
                dimidx.add(i);
            }
        }

        Schema schema = new Schema();

        if (this.vx != null)
        {
            schema.add(this.vx);
        }

        if (this.vy != null)
        {
            schema.add(this.vy);
        }

        if (this.vz != null)
        {
            schema.add(this.vz);
        }

        if (this.pos != null)
        {
            schema.add(this.pos);
        }

        if (this.idx != null)
        {
            schema.add(this.idx);
        }

        if (this.vector)
        {
            for (Integer idx : dimidx)
            {
                schema.add(this.value + "_" + idx);
            }
        }
        else
        {
            schema.add(this.value);
        }

        Table table = new Table(schema);
        Record row = new Record();

        Global.assume(this.vects.size() > 1, "vects must be at least two points");

        double length = 0;
        int ridx = 0;
        for (int j = 1; j < this.vects.size(); j++)
        {
            Vect a = this.vects.get(j - 1);
            Vect b = this.vects.get(j);
            Vect del = b.minus(a);

            for (int i = 0; i < this.num; i++)
            {
                double alpha = i / (this.num - 1.0);
                Vect d = del.times(alpha);
                Vect p = a.plus(d);
                Vect v = sampler.apply(p);

                double npos = length + d.norm();

                row.clear();

                if (this.vx != null)
                {
                    row.with(this.vx, String.valueOf(p.getX()));
                }
                if (this.vy != null)
                {
                    row.with(this.vy, String.valueOf(p.getY()));
                }
                if (this.vz != null)
                {
                    row.with(this.vz, String.valueOf(p.getZ()));
                }

                if (this.pos != null)
                {
                    row.with(this.pos, String.valueOf(npos));
                }

                if (this.idx != null)
                {
                    row.with(this.idx, String.valueOf(ridx));
                }

                if (this.vector)
                {
                    for (Integer didx : dimidx)
                    {
                        row.with(this.value + "_" + didx, String.valueOf(v.get(didx)));
                    }
                } else
                {
                    row.with(this.value, String.valueOf(v.get(0)));
                }

                table.addRecord(ridx, row);

                ridx += 1;
            }

            length += del.norm();
        }

        this.output = table;

        return this;
    }
}
