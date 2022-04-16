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
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Volume;
import qit.data.modules.volume.VolumeMagnitude;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.utils.MaskUtils;
import qit.data.utils.volume.VolumeVoxelStats;

import java.util.List;
import java.util.Map;

public class VolumeMeasureBatch implements CliMain
{
    public static void main(String[] args)
    {
        new VolumeMeasureBatch().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "Compute measures of a set of volumes in batch mode.";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<FilePattern>").withDoc("specify an input filename pattern (masks or density volumes)"));
            cli.withOption(new CliOption().asParameter().withName("names").withArg("<Spec>").withDoc("specify bundle identifiers"));
            cli.withOption(new CliOption().asInput().asOptional().withName("mask").withArg("<Mask>").withDoc("specify a mask for including voxels"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<Directory>").withDoc("specify an output directory"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("density").withDoc("measure density from input mask"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("thresh").withArg("<Double>").withDoc("specify a density threshold for volumetry").withDefault("0.5"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("volume").withArg("<String=Volume> [...]").withDoc("specify volumes to sample").withNoMax());
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            String inputPattern = entries.keyed.get("input").get(0);
            String output = entries.keyed.get("output").get(0);
            String rnames = entries.keyed.get("names").get(0);
            Double thresh = Double.valueOf(entries.keyed.get("thresh").get(0));
            boolean density = entries.keyed.containsKey("density");
            List<String> names = CliUtils.names(rnames, Lists.newArrayList(rnames));

            final Mask mask = entries.readMask("mask");

            Map<String, Volume> volumes = Maps.newHashMap();
            for (String pair : entries.keyed.get("volume"))
            {
                String[] tokens = pair.split("=");
                String name = tokens[0];
                String fn = tokens[1];

                Logging.info("using volume: " + fn);
                Volume volume = Volume.read(fn);
                volumes.put(name, volume);
            }

            Logging.info(String.format("found %d inputs", names.size()));
            Logging.info(String.format("found %d volumes", volumes.size()));

            Map<String, Record> results = Maps.newLinkedHashMap();
            for (String name : names)
            {
                Logging.info("started batch: " + name);

                String inputFn = String.format(inputPattern, name);

                if (!PathUtils.exists(inputFn))
                {
                    Logging.info("skipping: " + name);
                    continue;
                }

                Volume myden = Volume.read(inputFn);

                if (myden.getDim() > 1)
                {
                    Logging.info("detected multi-channel density map, computing vector magnitude");
                    myden = VolumeMagnitude.apply(myden);
                }

                Mask mymask = VolumeThreshold.apply(myden, thresh);

                mymask = MaskUtils.mask(mymask, mask);

                if (density)
                {
                    VolumeVoxelStats stats = new VolumeVoxelStats().withInput(myden).withMask(mymask).run();

                    if (stats.output)
                    {
                        Record measures = new Record();
                        measures.with("density_mean", stats.mean);
                        measures.with("density_qlow", stats.qlow);
                        measures.with("density_median", stats.median);
                        measures.with("density_qhigh", stats.qhigh);
                        measures.with("density_max", stats.max);
                        measures.with("density_sum", stats.sum);

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

                for (String volumeName : volumes.keySet())
                {
                    Volume volume = volumes.get(volumeName);
                    {
                        VolumeVoxelStats stats = new VolumeVoxelStats().withInput(volume).withMask(mymask).run();

                        if (stats.output)
                        {
                            String base = volumeName + "_";
                            Record measures = new Record();
                            measures.with(base + "mean", stats.mean);
                            measures.with(base + "std", stats.std);
                            measures.with(base + "mad", stats.mad);
                            measures.with(base + "sum", stats.sum);
                            measures.with(base + "min", stats.min);
                            measures.with(base + "qlow", stats.qlow);
                            measures.with(base + "median", stats.median);
                            measures.with(base + "qhigh", stats.qhigh);
                            measures.with(base + "max", stats.max);

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

                    if (density)
                    {
                        VolumeVoxelStats stats = new VolumeVoxelStats().withInput(volume).withMask(mymask).withWeights(myden).run();

                        if (stats.output)
                        {
                            String base = volumeName + "_weighted_";
                            Record measures = new Record();
                            measures.with(base + "mean", stats.mean);
                            measures.with(base + "sum", stats.sum);
                            measures.with(base + "qlow", stats.qlow);
                            measures.with(base + "median", stats.median);
                            measures.with(base + "qhigh", stats.qhigh);

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
                }

                if (!results.containsKey("volume"))
                {
                    results.put("volume", new Record());
                }
                results.get("volume").with(name, MaskUtils.volume(mymask));
            }

            if (!PathUtils.exists(output))
            {
                PathUtils.mkdirs(output);
            }

            for (String name : results.keySet())
            {
                String fn = PathUtils.join(output, name + ".csv");

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
