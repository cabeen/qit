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

import com.google.common.collect.Maps;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.datasets.Volume;
import qit.data.utils.TableUtils;

import java.util.Map;

@ModuleDescription("Mask a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeRegionMinima implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    private Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask restricting which voxels are checked")
    private Mask mask;

    @ModuleInput
    @ModuleDescription("a mask encoding regions")
    private Mask regions;

    @ModuleInput
    @ModuleDescription("a table encoding region names")
    private Table lookup;

    @ModuleParameter
    @ModuleDescription("retain the minimum value")
    private boolean minimum = false;

    @ModuleParameter
    @ModuleDescription("retain the sample voxel indices")
    private boolean sample = false;

    @ModuleParameter
    @ModuleDescription("specify the lookup index field")
    private String index = "index";

    @ModuleParameter
    @ModuleDescription("specify the lookup name field")
    private String name = "name";

    @ModuleParameter
    @ModuleDescription("specify the output field name for the minimum value")
    private String min = "min";

    @ModuleParameter
    @ModuleDescription("specify the output field name for the sample i index")
    private String i = "i";

    @ModuleParameter
    @ModuleDescription("specify the output field name for the sample j index")
    private String j = "j";

    @ModuleParameter
    @ModuleDescription("specify the output field name for the sample k index")
    private String k = "k";

    @ModuleOutput
    @ModuleDescription("output table")
    private Table output;

    @Override
    public Module run()
    {
        Map<Integer, String> lut = TableUtils.createIntegerStringLookup(this.lookup, this.index, this.name);

        Map<Integer,Pair<Sample,Double>> minima = Maps.newHashMap();
        for (Sample sample : this.input.getSampling())
        {
            int label = this.regions.get(sample);
            double value = this.input.get(sample, 0);

            if (this.input.valid(sample, this.mask) && label > 0 && lut.containsKey(label))
            {
                if (minima.containsKey(label))
                {
                    Pair<Sample,Double> pair = minima.get(label);
                    if (value < pair.b)
                    {
                        minima.put(label, Pair.of(sample, value));
                    }
                }
                else
                {
                    minima.put(label, Pair.of(sample, value));
                }
            }
        }

        Table out = new Table();
        out.withField(this.name);
        out.withField(this.index);

        if (this.sample)
        {
            out.withField(this.i);
            out.withField(this.j);
            out.withField(this.k);
        }

        if (this.minimum)
        {
            out.withField(this.min);
        }

        for (int label : lut.keySet())
        {
            if (minima.containsKey(label))
            {
                Pair<Sample, Double> pair = minima.get(label);

                Record record = new Record();
                record.with(this.name, lut.get(label));
                record.with(this.index, label);

                if (this.sample)
                {
                    record.with(this.i, pair.a.getI());
                    record.with(this.j, pair.a.getJ());
                    record.with(this.k, pair.a.getK());
                }

                if (this.minimum)
                {
                    record.with(this.min, pair.b);
                }

                out.addRecord(record);
            }
        }

        this.output = out;

        return this;
    }
}
