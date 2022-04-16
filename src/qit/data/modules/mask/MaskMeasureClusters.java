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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;
import qit.data.utils.vects.stats.VectOnlineStats;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ModuleDescription("Measure individual cluster sizes of a mask")
@ModuleAuthor("Ryan Cabeen")
public class MaskMeasureClusters implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("restrict the analysis to a given region of interest")
    public Mask region;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("use a lookup for region names")
    public Table lookup = null;

    @ModuleParameter
    @ModuleDescription("specify the output name field")
    public String nameField = "name";

    @ModuleParameter
    @ModuleDescription("specify the output cluster field")
    public String clusterField = "cluster";

    @ModuleParameter
    @ModuleDescription("specify the output voxel count field name")
    public String voxelsField = "voxels";

    @ModuleDescription("specify the output volume field name")
    public String volumeField = "volume";

    @ModuleParameter
    @ModuleDescription("specify the lookup name field")
    public String lutNameField = "name";

    @ModuleParameter
    @ModuleDescription("specify the lut index field name")
    public String lutIndexField = "index";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum component volume (mm^3) to be included")
    public Double minvolume;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum component voxel count to be included")
    public Integer minvoxels;

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public MaskMeasureClusters run()
    {
        Mask mask = this.region != null ? MaskUtils.and(this.input, this.region) : this.input;

        Map<Integer, String> lut = Maps.newHashMap();
        for (Integer label : MaskUtils.listNonzero(this.input))
        {
            lut.put(label, String.format("region_%d", label));
        }

        if (this.lookup != null)
        {
            lut = TableUtils.createIntegerStringLookup(this.lookup, this.lutIndexField, this.lutNameField);
        }

        Table table = new Table();
        table.withField(this.nameField);
        table.withField(this.clusterField);
        table.withField(this.voxelsField);
        table.withField(this.volumeField);

        Sampling sampling = mask.getSampling();
        double voxel = sampling.voxvol();

        Consumer<Triple<String, String, Integer>> add = (triple) ->
        {
            int count = triple.c;
            double volume = count * voxel;

            if (this.minvolume != null && volume < this.minvolume)
            {
                return;
            }

            if (this.minvoxels != null && count < this.minvoxels)
            {
                return;
            }

            Record record = new Record();
            record.with(this.nameField, triple.a);
            record.with(this.clusterField, triple.b);
            record.with(this.voxelsField, String.valueOf(count));
            record.with(this.volumeField, String.valueOf(volume));

            table.addRecord(record);
        };

        List<Integer> idx = Lists.newArrayList(lut.keySet());
        Collections.sort(idx);
        for (Integer i : idx)
        {
            Logging.info("processing label: " + i);

            String rid = "foreground";

            if (idx.size() > 1)
            {
                rid = String.valueOf(i);

                if (this.lookup != null && lut.containsKey(i))
                {
                    rid = lut.get(i);
                }
            }

            Map<Integer, Integer> clusterCounts = MaskUtils.counts(MaskComponents.apply(MaskUtils.equal(mask, i)));
            for (Integer key : clusterCounts.keySet())
            {
                add.accept(Triple.of(rid, String.valueOf(key), clusterCounts.get(key)));
            }
        }

        this.output = table;

        return this;
    }

    public static Table apply(Mask mask)
    {
        return new MaskMeasureClusters()
        {{
            this.input = mask;
        }}.run().output;
    }
}
