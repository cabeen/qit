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

package qit.main;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliValues;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectsStats;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class MaskRegionsMeasure implements CliMain
{
    public static void main(String[] args)
    {
        new MaskRegionsMeasure().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "measure statistics of volume data in a set of regions of interest";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asParameter().asOptional().withName("basic").withDoc("report only basic statistics"));
            cli.withOption(new CliOption().asInput().withName("regions").withArg("<Mask>").withDoc("specify input region of interest(s)"));
            cli.withOption(new CliOption().asInput().asOptional().withName("weight").withArg("<Volume>").withDoc("specify a volumetric weighting for computing statistics"));
            cli.withOption(new CliOption().asInput().withName("lookup").withArg("<Table>").withDoc("a table listing the names of regions"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<Directory>").withDoc("specify an output directory"));
            cli.withOption(new CliOption().asInput().asOptional().withName("mask").withArg("<Mask>").withDoc("specify a mask for including voxels"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("volume").withArg("<String=Volume> [...]").withDoc("specify volumes to measure").withNoMax());
            cli.withOption(new CliOption().asParameter().asOptional().withName("name").withArg("<String>").withDoc("specify a lookup table field for region names").withDefault("name"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("index").withArg("<String>").withDoc("specify a lookup table field for region index (label)").withDefault("index"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("na").withArg("<String>").withDoc("specify a name for missing values").withDefault("NA"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            boolean basic = entries.keyed.containsKey("basic");
            String regionsFn = entries.keyed.get("regions").get(0);
            String lookupFn = entries.keyed.get("lookup").get(0);
            String outputDn = entries.keyed.get("output").get(0);
            String nameField = entries.keyed.get("name").get(0);
            String indexField = entries.keyed.get("index").get(0);
            String naName = entries.keyed.get("na").get(0);

            Logging.info("using lookup: " + lookupFn);
            Record regionMap = TableUtils.createLookup(Table.read(lookupFn), indexField, nameField);

            Logging.info("using regions: " + regionsFn);
            Mask regions = Mask.read(regionsFn);
            regions = MaskUtils.mask(regions, entries.readMask("mask"));

            Function<Sample,Vect> getWeight = (s) -> {return VectSource.create1D(1.0);};

            if (entries.keyed.containsKey("weight"))
            {
                String weightFn = entries.keyed.get("weight").get(0);
                Logging.info("using weight: " + weightFn);
                Volume weight = Volume.read(weightFn);

                getWeight = (s) -> { return weight.get(s); };
            }

            Map<String, Volume> volumeMap = Maps.newLinkedHashMap();
            if (entries.keyed.containsKey("volume"))
            {
                for (String pair : entries.keyed.get("volume"))
                {
                    String[] tokens = pair.split("=");
                    Global.assume(tokens.length == 2, "expected a name=fn pair for volume option");
                    String name = tokens[0];
                    String fn = tokens[1];

                    Logging.info("using volume: " + fn);
                    Volume volume = Volume.read(fn);
                    volumeMap.put(name, volume);
                }
            }

            double voxel = regions.getSampling().voxvol();
            Set<String> labels = regionMap.keySet();
            Map<String, Record> results = Maps.newLinkedHashMap();

            for (String volumeKey : volumeMap.keySet())
            {
                Map<String,Pair<Vects,Vects>> values = Maps.newHashMap();
                for (String regionKey : labels)
                {
                    values.put(regionKey, Pair.of(new Vects(), new Vects()));
                }

                // this single pass is much faster than started VolumeVoxelStats for each region
                Volume volume = volumeMap.get(volumeKey);
                for (Sample sample : volume.getSampling())
                {
                    Vect weight = getWeight.apply(sample);
                    Vect value = volume.get(sample);
                    String label = String.valueOf(regions.get(sample));

                    if (labels.contains(label))
                    {
                        Pair<Vects, Vects> pair = values.get(label);
                        pair.a.add(weight);
                        pair.b.add(value);
                    }
                }

                for (String regionKey : Lists.newArrayList(regionMap.keySet()))
                {
                    String name = regionMap.get(regionKey);

                    Logging.info("processing: " + name);
                    Pair<Vects, Vects> pair = values.get(regionKey);
                    VectsStats stats = new VectsStats().withWeights(pair.a.flatten()).withInput(pair.b);
                    Record measures = new Record();
                    if (stats.run().getOutput())
                    {
                        measures.with(String.format("%s_mean", volumeKey), String.valueOf(stats.mean.get(0)));
                        measures.with(String.format("%s_std", volumeKey), String.valueOf(stats.std.get(0)));
                        measures.with(String.format("%s_median", volumeKey), String.valueOf(stats.median.get(0)));
                        measures.with(String.format("%s_iqr", volumeKey), String.valueOf(stats.iqr.get(0)));
                        measures.with(String.format("%s_volume", volumeKey), String.valueOf(stats.num.get(0) * voxel));

                        if (!basic)
                        {
                            measures.with(String.format("%s_stde", volumeKey), String.valueOf(stats.stde.get(0)));
                            measures.with(String.format("%s_min", volumeKey), String.valueOf(stats.min.get(0)));
                            measures.with(String.format("%s_qlow", volumeKey), String.valueOf(stats.qlow.get(0)));
                            measures.with(String.format("%s_qhigh", volumeKey), String.valueOf(stats.qhigh.get(0)));
                            measures.with(String.format("%s_max", volumeKey), String.valueOf(stats.max.get(0)));
                            measures.with(String.format("%s_num", volumeKey), String.valueOf(stats.num.get(0)));
                            measures.with(String.format("%s_sum", volumeKey), String.valueOf(stats.sum.get(0)));
                        }
                    }
                    else
                    {
                        measures.with(String.format("%s_mean", volumeKey), naName);
                        measures.with(String.format("%s_std", volumeKey), naName);
                        measures.with(String.format("%s_median", volumeKey), naName);
                        measures.with(String.format("%s_iqr", volumeKey), naName);
                        measures.with(String.format("%s_volume", volumeKey), naName);

                        if (!basic)
                        {
                            measures.with(String.format("%s_stde", volumeKey), naName);
                            measures.with(String.format("%s_min", volumeKey), naName);
                            measures.with(String.format("%s_qlow", volumeKey), naName);
                            measures.with(String.format("%s_qhigh", volumeKey), naName);
                            measures.with(String.format("%s_max", volumeKey), naName);
                            measures.with(String.format("%s_num", volumeKey), naName);
                            measures.with(String.format("%s_sum", volumeKey), naName);
                        }
                    }

                    for (String measure : measures.keySet())
                    {
                        if (!results.containsKey(measure))
                        {
                            results.put(measure, new Record());
                        }

                        results.get(measure).with(name, measures.get(measure));
                    }
                }
            }

            if (!PathUtils.exists(outputDn))
            {
                PathUtils.mkdirs(outputDn);
            }

            for (String name : results.keySet())
            {
                String fn = PathUtils.join(outputDn, name + ".csv");

                Logging.info("writing: " + fn);
                results.get(name).write(fn);
            }

            Logging.info("finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}