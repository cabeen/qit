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
import com.google.common.collect.Sets;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Volume;
import qit.data.modules.table.TableWiden;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;
import qit.data.utils.volume.VolumeVoxelStats;

import java.util.Map;
import java.util.Set;

@ModuleDescription("Collect statistical sumaries of a volume using a mask.  This supports single label masks and more advanced options for multi-label masks (e.g. loading names associated with each label)")
@ModuleAuthor("Ryan Cabeen")
public class VolumeMeasureMulti implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a weighting")
    public Volume weight;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("use a lookup for region names")
    public Table lookup = null;

    @ModuleParameter
    @ModuleDescription("use the background")
    public boolean background = false;

    @ModuleParameter
    @ModuleDescription("use multiple regions")
    public boolean multiple = false;

    @ModuleParameter
    @ModuleDescription("specify a region basename")
    public String base = "region";

    @ModuleParameter
    @ModuleDescription("specify the lookup index field")
    public String index = "index";

    @ModuleParameter
    @ModuleDescription("specify the channel index field")
    public String channel = "channel";

    @ModuleParameter
    @ModuleDescription("specify the lookup name field")
    public String name = "name";

    @ModuleParameter
    @ModuleDescription("specify the output region field name")
    public String region = "region";

    @ModuleParameter
    @ModuleDescription("specify the output statistic field name")
    public String stat = "stat";

    @ModuleParameter
    @ModuleDescription("specify the output value field name")
    public String value = "value";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify which statistics should be included (e.g. mean)")
    public String include = null;

    @ModuleParameter
    @ModuleDescription("combine the region and stat fields into one")
    public boolean union = false;

    @ModuleParameter
    @ModuleDescription("the character for joining region and stat names (depends on union flag)")
    public String join = "_";

    @ModuleParameter
    @ModuleDescription("exclude the statistics field")
    public boolean nostat = false;

    @ModuleParameter
    @ModuleDescription("widen the table on the statistics field")
    public boolean widen = false;

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public VolumeMeasureMulti run()
    {
        if (this.mask == null)
        {
            // when no mask is specified measure the whole volume
            this.mask = new Mask(this.input.getSampling());
            this.mask.setAll(1);
        }

        Map<Integer, String> lut = null;
        if (this.lookup != null)
        {
            lut = TableUtils.createIntegerStringLookup(this.lookup, this.index, this.name);
        }
        else if (this.background && !this.multiple)
        {
            lut = Maps.newHashMap();
            lut.put(0, "background");
            lut.put(1, "foreground");
        }
        else if (this.multiple && this.background)
        {
            lut = Maps.newHashMap();
            for (Integer label : MaskUtils.listAll(this.mask))
            {
                lut.put(label, this.base + label);
            }
        }
        else if (this.multiple && !this.background)
        {
            lut = Maps.newHashMap();
            for (Integer label : MaskUtils.listNonzero(this.mask))
            {
                lut.put(label, this.base + label);
            }
        }

        Table table = new Table();
        if (lut != null)
        {
            table.withField(this.region);
        }
        table.withField(this.channel);
        table.withField(this.stat);
        table.withField(this.value);

        for (int i = 0; i < this.input.getDim(); i++)
        {
            if (lut == null)
            {
                double vol = MaskUtils.volume(this.mask);
                VolumeVoxelStats results = new VolumeVoxelStats().withInput(this.input).withChannel(i).withMask(this.mask).withWeights(this.weight).run();
                table.addRecord(new String[]{String.valueOf(i), "mean", String.valueOf(results.mean)});
                table.addRecord(new String[]{String.valueOf(i), "std", String.valueOf(results.std)});
                table.addRecord(new String[]{String.valueOf(i), "stde", String.valueOf(results.stde)});
                table.addRecord(new String[]{String.valueOf(i), "min", String.valueOf(results.min)});
                table.addRecord(new String[]{String.valueOf(i), "qlow", String.valueOf(results.qlow)});
                table.addRecord(new String[]{String.valueOf(i), "median", String.valueOf(results.median)});
                table.addRecord(new String[]{String.valueOf(i), "qhigh", String.valueOf(results.qhigh)});
                table.addRecord(new String[]{String.valueOf(i), "iqr", String.valueOf(results.iqr)});
                table.addRecord(new String[]{String.valueOf(i), "max", String.valueOf(results.max)});
                table.addRecord(new String[]{String.valueOf(i), "num", String.valueOf(results.num)});
                table.addRecord(new String[]{String.valueOf(i), "sum", String.valueOf(results.sum)});
                table.addRecord(new String[]{String.valueOf(i), "volume", String.valueOf(vol)});
            }
            else
            {
                for (Integer label : lut.keySet())
                {
                    String name = lut.get(label);
                    double voxel = this.mask.getSampling().voxvol();
                    VolumeVoxelStats results = new VolumeVoxelStats().withInput(this.input).withMask(mask).withChannel(i).withLabel(label).withWeights(this.weight).run();
                    table.addRecord(new String[]{name, String.valueOf(i), "mean", String.valueOf(results.mean)});
                    table.addRecord(new String[]{name, String.valueOf(i), "std", String.valueOf(results.std)});
                    table.addRecord(new String[]{name, String.valueOf(i), "stde", String.valueOf(results.stde)});
                    table.addRecord(new String[]{name, String.valueOf(i), "min", String.valueOf(results.min)});
                    table.addRecord(new String[]{name, String.valueOf(i), "qlow", String.valueOf(results.qlow)});
                    table.addRecord(new String[]{name, String.valueOf(i), "median", String.valueOf(results.median)});
                    table.addRecord(new String[]{name, String.valueOf(i), "qhigh", String.valueOf(results.qhigh)});
                    table.addRecord(new String[]{name, String.valueOf(i), "max", String.valueOf(results.max)});
                    table.addRecord(new String[]{name, String.valueOf(i), "num", String.valueOf(results.num)});
                    table.addRecord(new String[]{name, String.valueOf(i), "sum", String.valueOf(results.sum)});
                    // table.addRecord(new String[]{name, String.valueOf(i), "volume", String.valueOf(results.num * voxel)});
                }
            }
        }

        if (this.include != null && this.include.length() > 0)
        {
            Set<String> which = Sets.newHashSet();
            for (String token : this.include.split(","))
            {
                which.add(token.toLowerCase());
            }

            Table out = table.proto();
            for (Integer key : table.keys())
            {
                Record rec = table.getRecord(key);
                if (which.contains(rec.get(this.stat).toLowerCase()))
                {
                    out.addRecord(rec);
                }
            }

            table = out;
        }

        if (this.multiple && this.union)
        {
            Table out = new Table();
            out.withField(this.stat);
            out.withField(this.value);

            for (Integer key : table.keys())
            {
                Record rec = table.getRecord(key);
                String mystat = rec.get(this.stat);
                String myregion = rec.get(this.region);
                String myvalue = rec.get(this.value);

                Record nrec = new Record();
                nrec.with(this.stat, myregion + this.join + mystat);
                nrec.with(this.value, myvalue);
                out.addRecord(nrec);
            }

            table = out;
        }

        if (this.nostat)
        {
            Table out = new Table();
            out.withField(this.region);
            out.withField(this.value);

            for (Integer key : table.keys())
            {
                Record rec = table.getRecord(key);
                String myregion = rec.get(this.region);
                String myvalue = rec.get(this.value);

                Record nrec = new Record();
                nrec.with(this.region, myregion);
                nrec.with(this.value, myvalue);
                out.addRecord(nrec);
            }

            table = out;
        }

        if (this.widen)
        {
            final Table ftable = table;
            table = new TableWiden()
            {{
                this.input = ftable;
                this.name = VolumeMeasureMulti.this.stat;
                this.value = VolumeMeasureMulti.this.value;
            }}.run().output;
        }

        this.output = table;

        return this;
    }

    public static Table apply(Volume volume)
    {
        return new VolumeMeasureMulti()
        {{
            this.input = volume;
        }}.run().output;
    }

    public static Table apply(Volume volume, Mask mymask)
    {
        return new VolumeMeasureMulti()
        {{
            this.input = volume;
            this.mask = mymask;
        }}.run().output;
    }
}
