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
import qit.base.Logging;
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
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@ModuleDescription("Record voxel values in a table")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTable implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a lookup table for matching names to mask labels")
    public Table lookup = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("value")
    public String value = "value";

    @ModuleParameter
    @ModuleDescription("use multiple mask labels")
    public boolean multiple = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("specify the lookup name field")
    public String lookupNameField = "name";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("specify the output voxel field name")
    public String lookupIndexField = "voxel";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("a mask specification")
    public String range;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("a which of dimensions to use")
    public String dims;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("include vector values")
    public boolean vector = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the field for the voxel i coordinate")
    public String vi = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the field for the voxel j coordinate")
    public String vj = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the field for the voxel k coordinate")
    public String vk = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the field for the voxel voxel")
    public String voxel = "voxel";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public Module run()
    {
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

        Mask vmask = null;
        if (this.mask != null)
        {
            vmask = this.mask;
        }

        if (this.range != null)
        {
            try
            {
                vmask = MaskUtils.and(vmask, MaskSource.create(input.getSampling(), this.range));
            }
            catch (IOException e)
            {
                Logging.error("failed to parse range: " + this.range);
            }
        }

        Schema schema = new Schema();
        if (this.vi != null)
        {
            schema.add(this.vi);
        }
        if (this.vj != null)
        {
            schema.add(this.vj);
        }
        if (this.vk != null)
        {
            schema.add(this.vk);
        }
        if (this.voxel != null)
        {
            schema.add(this.voxel);
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
        Map<Integer, String> lut = null;

        if (this.multiple)
        {
            if (this.lookup != null)
            {
                table.withField(this.lookupNameField);
                lut = TableUtils.createIntegerStringLookup(this.lookup, this.lookupIndexField, this.lookupNameField);
            }
            else
            {
                table.withField(this.lookupIndexField);
            }
        }

        Sampling sampling = this.input.getSampling();
        Record row = new Record();
        for (Sample sample : sampling)
        {
            if (this.input.valid(sample, vmask))
            {
                int vidx = sampling.index(sample);
                row.clear();

                if (this.vi != null)
                {
                    row.with(this.vi, String.valueOf(sample.getI()));
                }
                if (this.vj != null)
                {
                    row.with(this.vj, String.valueOf(sample.getJ()));
                }
                if (this.vk != null)
                {
                    row.with(this.vk, String.valueOf(sample.getK()));
                }
                if (this.voxel != null)
                {
                    row.with(this.voxel, String.valueOf(vidx));
                }

                if (this.vector)
                {
                    for (Integer didx : dimidx)
                    {
                        row.with(this.value + "_" + didx, String.valueOf(this.input.get(sample, didx)));
                    }
                }
                else
                {
                    row.with(this.value, String.valueOf(this.input.get(sample, 0)));
                }

                if (this.multiple)
                {
                    int label = this.mask.get(sample);
                    if (this.lookup != null)
                    {
                        row.with(this.lookupNameField, lut.get(label));
                    }
                    else
                    {
                        row.with(this.lookupIndexField, String.valueOf(label));

                    }
                }

                table.addRecord(vidx, row);
            }
        }

        this.output = table;

        return this;
    }
}
