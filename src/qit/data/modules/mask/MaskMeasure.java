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
import qit.data.datasets.*;
import qit.data.datasets.Record;
import qit.data.modules.volume.VolumeMeasure;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;
import qit.data.utils.vects.stats.VectOnlineStats;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ModuleDescription("Measure properties of a mask")
@ModuleAuthor("Ryan Cabeen")
public class MaskMeasure implements Module
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
    @ModuleDescription("specify the output value field name")
    public String valueField = "value";

    @ModuleParameter
    @ModuleDescription("specify the lookup name field")
    public String lutNameField = "name";

    @ModuleParameter
    @ModuleDescription("specify the lut index field name")
    public String lutIndexField = "index";

    @ModuleParameter
    @ModuleDescription("report only the region volumes")
    public boolean volumes;

    @ModuleParameter
    @ModuleDescription("compute the fraction of each region")
    public boolean fraction;

    @ModuleParameter
    @ModuleDescription("compute cluster statistics of each region")
    public boolean cluster;

    @ModuleParameter
    @ModuleDescription("compute the position of each region")
    public boolean position;

    @ModuleParameter
    @ModuleDescription("run connected components labeling before anything else")
    public boolean components;

    @ModuleParameter
    @ModuleDescription("binarize the mask before anything else (overrides the components flag)")
    public boolean binarize;

    @ModuleParameter
    @ModuleDescription("include component counts in the report")
    public boolean comps;

    @ModuleParameter
    @ModuleDescription("include voxel counts in the report")
    public boolean counts;

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
    public MaskMeasure run()
    {
        Mask mask = this.region != null ? MaskUtils.and(this.input, this.region) : this.input;

        if (this.components)
        {
            final Mask fmask = mask;
            mask = new MaskComponents()
            {{
                this.input = fmask;
                this.minvolume = MaskMeasure.this.minvolume;
                this.minvoxels = MaskMeasure.this.minvoxels;
            }}.run().output;
        }

        if (this.binarize)
        {
            final Mask fmask = mask;
            mask = new MaskBinarize()
            {{
                this.input = fmask;
            }}.run().output;
        }

        Map<Integer, String> lut = Maps.newHashMap();
        for (Integer label : MaskUtils.listNonzero(this.input))
        {
            lut.put(label, String.format("region_%d", label));
        }

        if (this.lookup != null)
        {
            Map<Integer, String> full = TableUtils.createIntegerStringLookup(this.lookup, this.lutIndexField, this.lutNameField);
            List<Integer> list = MaskUtils.listNonzero(mask);

            lut.clear();
            for (Integer key: full.keySet())
            {
                if (list.contains(key))
                {
                    lut.put(key, full.get(key));
                }
                else
                {
                    Logging.info("warning: skipping lookup label not found in data: " + key);
                }
            }
        }

        Table table = new Table();
        table.withField(this.nameField);
        table.withField(this.valueField);

        Sampling sampling = mask.getSampling();
        double voxel = sampling.voxvol();

        Map<Integer, Vect> sums = Maps.newHashMap();
        Map<Integer, Integer> counts = Maps.newHashMap();

        for (Integer label : lut.keySet())
        {
            counts.put(label, 0);
            sums.put(label, VectSource.create3D());
        }

        for (Sample s : sampling)
        {
            int label = mask.get(s);

            if (mask.get(s) != 0)
            {
                Vect sum = sums.get(label);
                int count = counts.get(label);

                sums.put(label, sum.plus(sampling.world(s)));
                counts.put(label, count + 1);
            }
        }

        Consumer<Pair<String, String>> add = (pair) ->
        {
            Record record = new Record();
            record.with(this.nameField, pair.a);
            record.with(this.valueField, pair.b);
            table.addRecord(record);
        };

        VectOnlineStats stats = new VectOnlineStats();
        for (Integer key : counts.keySet())
        {
            stats.update(counts.get(key) * voxel);
        }

        if (this.volumes)
        {
            List<Integer> idx = Lists.newArrayList(counts.keySet());
            Collections.sort(idx);
            for (Integer i : idx)
            {
                int count = counts.get(i);
                double volume = count * voxel;

                String rid = "";

                if (idx.size() > 1)
                {
                    rid = String.valueOf(i);

                    if (this.lookup != null && lut.containsKey(i))
                    {
                        rid = lut.get(i);
                    }
                }

                add.accept(Pair.of(rid, String.valueOf(volume)));
            }
        }
        else
        {
            boolean any = counts.size() > 1;

            if (this.comps)
            {
                add.accept(Pair.of("component_count", String.valueOf(stats.num)));
                add.accept(Pair.of("component_max", String.valueOf(stats.max)));
                add.accept(Pair.of("component_min", String.valueOf(stats.min)));
                add.accept(Pair.of("component_mean", String.valueOf(stats.mean)));
                add.accept(Pair.of("component_sum", String.valueOf(stats.sum)));
                add.accept(Pair.of("component_std", any ? String.valueOf(stats.std) : "NA"));
            }

            List<Integer> idx = Lists.newArrayList(lut.keySet());
            Collections.sort(idx);
            for (Integer i : idx)
            {
                Logging.info("processing label: " + i);

                int count = counts.get(i);
                double volume = count * voxel;
                Vect mean = sums.get(i).times(1.0 / (double) count);

                String rid = "";

                if (idx.size() > 1)
                {
                    rid = String.valueOf(i);

                    if (this.lookup != null && lut.containsKey(i))
                    {
                        rid = lut.get(i);
                    }

                    rid = String.format("_%s", rid);
                }

                add.accept(Pair.of(String.format("volume%s", rid), String.valueOf(volume)));

                if (this.counts)
                {
                    add.accept(Pair.of(String.format("voxels%s", rid), String.valueOf(count)));
                }

                if (this.position)
                {
                    add.accept(Pair.of(String.format("xmean%s", rid), String.valueOf(mean.getX())));
                    add.accept(Pair.of(String.format("ymean%s", rid), String.valueOf(mean.getY())));
                    add.accept(Pair.of(String.format("zmean%s", rid), String.valueOf(mean.getZ())));
                }

                if (this.fraction)
                {
                    add.accept(Pair.of(String.format("fraction%s", rid), String.valueOf(volume / stats.sum)));
                }

                if (this.cluster)
                {
                    Map<Integer, Integer> clusterCounts = MaskUtils.counts(MaskComponents.apply(MaskUtils.equal(mask, i)));
                    VectOnlineStats clusterStats = new VectOnlineStats();
                    for (Integer key : clusterCounts.keySet())
                    {
                        clusterStats.update(clusterCounts.get(key));
                    }

                    add.accept(Pair.of(String.format("cluster_count%s", rid), String.valueOf(clusterStats.num)));
                    add.accept(Pair.of(String.format("cluster_voxels_mean%s", rid), String.valueOf(clusterStats.mean)));
                    add.accept(Pair.of(String.format("cluster_volumes_mean%s", rid), String.valueOf(clusterStats.mean * voxel)));
                    add.accept(Pair.of(String.format("cluster_voxels_std%s", rid), String.valueOf(clusterStats.std)));
                    add.accept(Pair.of(String.format("cluster_volumes_std%s", rid), String.valueOf(clusterStats.std * voxel)));
                    add.accept(Pair.of(String.format("cluster_voxels_max%s", rid), String.valueOf(clusterStats.max)));
                    add.accept(Pair.of(String.format("cluster_volumes_max%s", rid), String.valueOf(clusterStats.max * voxel)));
                }
            }
        }

        this.output = table;

        return this;
    }

    public static Table apply(Mask mask)
    {
        return new MaskMeasure()
        {{
            this.input = mask;
        }}.run().output;
    }
}
