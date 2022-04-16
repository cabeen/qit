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

package qit.data.modules.mask;

import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.utils.TableUtils;

import java.util.Map;
import java.util.Set;

@ModuleDescription("Relabel a mask by replacing voxel labels with new values")
@ModuleAuthor("Ryan Cabeen")
public class MaskRelabel implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input lookup")
    public Table lookup;

    @ModuleInput
    @ModuleDescription("input mapping (includes fields named 'from' and 'to')")
    public Table mapping;

    @ModuleParameter
    @ModuleDescription("preserve the input labels if possible")
    public boolean preserve = false;

    @ModuleParameter
    @ModuleDescription("use names for remapping (instead of index labels)")
    public boolean names = false;

    @ModuleParameter
    @ModuleDescription("specify a from field to us in mapping")
    public String from = "from";

    @ModuleParameter
    @ModuleDescription("specify a to field to us in mapping")
    public String to = "to";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the name field")
    public String name = "name";

    @ModuleParameter
    @ModuleDescription("the index field")
    public String index = "index";

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask outputMask;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output lookup")
    public Table outputLookup;

    @Override
    public MaskRelabel run()
    {
        Map<String,Integer> outlut = Maps.newLinkedHashMap();
        Map<Integer, Integer> mapper = Maps.newHashMap();
        mapper.put(0,0);

        if (this.names)
        {
            Global.assume(this.lookup != null, "named mapping requires a lookup");

            Map<String,Integer> lut = TableUtils.createStringIntegerLookup(this.lookup, this.name, this.index);

            for (int key : this.mapping.keys())
            {
                Record row = this.mapping.getRecord(key);
                String fromName = row.get(this.from);
                String toName = row.get(this.to);

                if (!outlut.containsKey(toName))
                {
                    if (this.preserve)
                    {
                        outlut.put(toName, lut.get(fromName));
                    }
                    else
                    {
                        outlut.put(toName, outlut.size() + 1);
                    }
                }

                Global.assume(lut.containsKey(fromName), "from name not found: " + fromName);
                Global.assume(outlut.containsKey(toName), "to name not found: " + toName);

                int fromIdx = lut.get(fromName);
                int toIdx = outlut.get(toName);
                mapper.put(fromIdx, toIdx);
            }
        }
        else
        {
            for (int key : this.mapping.keys())
            {
                Record row = this.mapping.getRecord(key);
                int fromIdx = Integer.valueOf(row.get(this.from));
                int toIdx = Integer.valueOf(row.get(this.to));
                mapper.put(fromIdx, toIdx);

                String toName = "region" + toIdx;
                if (!outlut.containsKey(toName))
                {
                    outlut.put(toName, toIdx);
                }
            }
        }

        Mask outMask = this.mask.proto();
        for (Sample sample : this.mask.getSampling())
        {
            int label = this.mask.get(sample);
            if (mapper.containsKey(label))
            {
                outMask.set(sample, mapper.get(label));
            }
        }

        Table outLookup = new Table();
        outLookup.withField(this.name);
        outLookup.withField(this.index);
        for (String n : outlut.keySet())
        {
            Record row = new Record();
            row.with(this.name, n);
            row.with(this.index, outlut.get(n));
            outLookup.addRecord(row);
        }

        this.outputMask = outMask;
        this.outputLookup = outLookup;

        return this;
    }

    public static Mask extract(Mask volume, Set<Integer> labels)
    {
        Mask out = volume.proto();

        for (Sample sample : volume.getSampling())
        {
            if (labels.contains(volume.get(sample)))
            {
                out.set(sample, 1);
            }
        }

        return out;
    }
}

