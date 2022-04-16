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
import qit.base.cli.CliOption;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.structs.Pointer;
import qit.base.utils.JavaUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Curves;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Volume;
import qit.data.modules.curves.CurvesSmooth;
import qit.data.modules.curves.CurvesThickness;
import qit.data.utils.CurvesUtils;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.curves.CurvesFunctionSample;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.volume.VolumeInterpTrilinear;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CurvesMeasureAlongBatch implements CliMain
{
    public static void main(String[] args)
    {
        try
        {
            new CurvesMeasureAlongBatch().run(Lists.newArrayList(args));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Logging.error("an error occurred");
        }
    }

    public void run(List<String> args) throws IOException
    {
        Logging.info("starting " + this.getClass().getSimpleName());

        String doc = "Compute measures of a set of curves in batch mode.";

        CliSpecification cli = new CliSpecification();
        cli.withName(this.getClass().getSimpleName());
        cli.withDoc(doc);
        cli.withOption(new CliOption().asInput().withName("input").withArg("<FilePattern>").withDoc("specify an input bundle filename pattern"));
        cli.withOption(new CliOption().asParameter().withName("names").withArg("<Spec>").withDoc("specify bundle identifiers (e.g. a file that lists the bundle names)"));
        cli.withOption(new CliOption().asInput().asOptional().withName("volume").withArg("<String=Volume> [...]").withDoc("specify volumes to sample").withNoMax());
        cli.withOption(new CliOption().asInput().asOptional().withName("deform").withArg("<File>").withDoc("specify a deformation between curves and volumes"));
        cli.withOption(new CliOption().asParameter().asOptional().withName("attrs").withArg("<String> [<String>] [...]").withDoc("only include the specified attributes").withNoMax());
        cli.withOption(new CliOption().asParameter().asOptional().withName("label").withArg("<String>").withDoc("compute statistics with parameterization attribute (discrete valued)").withDefault("label"));
        cli.withOption(new CliOption().asParameter().asOptional().withName("voxel").withDoc("use voxel-based measurement (default is vertex-based)"));
        cli.withOption(new CliOption().asParameter().asOptional().withName("interp").withArg("<String>").withDoc("specify an interpolation method").withDefault(VolumeInterpTrilinear.NAME));
        cli.withOption(new CliOption().asParameter().asOptional().withName("iters").withArg("<Int>").withDoc("smooth the sampled data with the given number of laplacian iterations").withDefault("3"));
        cli.withOption(new CliOption().asParameter().asOptional().withName("lambda").withArg("<Double>").withDoc("smooth the sampled data with the given laplacian weighting").withDefault("0.3"));
        cli.withOption(new CliOption().asParameter().asOptional().withName("threads").withArg("<Integer>").withDoc("specify a number of threads").withDefault("1"));
        cli.withOption(new CliOption().asOutput().withName("output").withArg("<Directory>").withDoc("specify an output directory"));
        cli.withAuthor("Ryan Cabeen");

        Logging.info("parsing arguments");
        CliValues entries = cli.parse(args);

        Logging.info("started");
        String input = entries.keyed.get("input").get(0);
        String output = entries.keyed.get("output").get(0);
        String rnames = entries.keyed.get("names").get(0);
        List<String> names = CliUtils.names(rnames, Lists.newArrayList(rnames));
        boolean voxel = entries.keyed.containsKey("voxel");

        int threads = entries.keyed.containsKey("threads") ? Integer.valueOf(entries.keyed.get("threads").get(0)) : 1;

        Logging.info(String.format("found %d structures", names.size()));

        Map<String, String> fns = Maps.newLinkedHashMap();
        for (String name : names)
        {
            String fn = String.format(input, name);
            fns.put(name, fn);
            Logging.info("using input: " + fn);
        }

        List<String> attrs = Lists.newArrayList();
        if (entries.keyed.containsKey("attrs") && entries.keyed.get("attrs").size() > 0)
        {
            attrs.addAll(entries.keyed.get("attrs"));
            Logging.info("using attrs: " + attrs);
            Logging.info(String.format("found %d attrs", attrs.size()));
        }

        Pointer<VectFunction> deform = Pointer.to(VectFunctionSource.identity(3));
        if (entries.keyed.containsKey("deform"))
        {
            String fn = entries.keyed.get("deform").get(0);
            deform.set(Deformation.read(fn));
        }

        Map<String, Volume> idxes = Maps.newLinkedHashMap();
        if (entries.keyed.containsKey("volume"))
        {
            for (String pair : entries.keyed.get("volume"))
            {
                String[] tokens = pair.split("=");
                String name = tokens[0];
                String fn = tokens[1];

                Logging.info("using volume: " + fn);
                Volume volume = Volume.read(fn);
                idxes.put(name, volume);
            }
        }

        String interp = entries.keyed.get("interp").get(0);
        Integer iters = Integer.valueOf(entries.keyed.get("iters").get(0));
        Double lambda = Double.valueOf(entries.keyed.get("lambda").get(0));
        final String label = entries.keyed.containsKey("label") ? entries.keyed.get("label").get(0) : null;

        Map<String, Record> collector = Maps.newConcurrentMap();

        Consumer<String> process = (name) ->
        {
            try
            {
                String fn = fns.get(name);

                if (!PathUtils.exists(fn))
                {
                    Logging.info("skipping: " + name);
                    return;
                }

                Logging.info("reading: " + fn);
                Curves curves = Curves.read(fn);

                if (curves.size() == 0)
                {
                    Logging.info("skipping: " + name);
                    return;
                }

                if (voxel)
                {
                    Logging.info("computing density-based parameterized curves statistics");
                    Volume mydensity = VolumeUtils.density(JavaUtils.getFirstValue(idxes).getSampling(), curves);
                    Mask labelmask = CurvesUtils.labels(mydensity.getSampling(), curves, label);
                    List<Integer> labels = CurvesUtils.list(curves, label);
                    double voxvol = mydensity.getSampling().voxvol();

                    for (int i = 0; i < labels.size(); i++)
                    {
                        Mask submask = MaskUtils.equal(labelmask, labels.get(i));
                        String pname = String.format("%s_%03d", name, labels.get(i));
                        Logging.info("processing parameter: " + pname);

                        Record measures = new Record();
                        BiConsumer<VolumeVoxelStats, String> addem = (stats, base) ->
                        {
                            boolean valid = stats != null && stats.num != null && stats.num > 0;
                            measures.with(base + "_mean", valid ? stats.mean : "NA");
                            measures.with(base + "_sum", valid ? stats.sum : "NA");
                            measures.with(base + "_qlow", valid ? stats.qlow : "NA");
                            measures.with(base + "_median", valid ? stats.median : "NA");
                            measures.with(base + "_qhigh", valid ? stats.qhigh : "NA");
                            measures.with(base + "_max", valid ? stats.max : "NA");
                            measures.with(base + "_min", valid ? stats.min : "NA");
                            measures.with(base + "_vol", valid ? stats.num * voxvol : 0);
                        };

                        for (String idx : idxes.keySet())
                        {
                            Volume volume = idxes.get(idx);

                            VolumeVoxelStats stats = new VolumeVoxelStats().withInput(volume).withMask(submask).run();
                            addem.accept(stats, idx);

                            VolumeVoxelStats wstats = new VolumeVoxelStats().withInput(volume).withMask(submask).withWeights(mydensity).run();
                            addem.accept(wstats, idx + "_weighted");
                        }

                        collector.put(pname, measures);
                    }
                }
                else
                {
                    for (String idx : idxes.keySet())
                    {
                        Logging.info("sampling: " + idx);
                        VectFunction sampler = deform.get().compose(VolumeUtils.interp(InterpolationType.valueOf(interp), idxes.get(idx)));
                        new CurvesFunctionSample().withCurves(curves).withFunction(sampler).withAttribute(idx).run();

                        if (iters > 0)
                        {
                            Logging.info("smoothing: " + idx);
                            CurvesSmooth.smoothInPlace(curves, idx, iters, lambda);
                        }
                    }

                    boolean thick = false;
                    for (String attr : attrs)
                    {
                        if (attr.startsWith("thick"))
                        {
                            thick = true;
                            break;
                        }
                    }

                    Curves mythick = null;
                    if (thick)
                    {
                        mythick = CurvesThickness.apply(curves);
                        for (String attr : Lists.newArrayList(mythick.names()))
                        {
                            if (!attr.equals(Curves.COORD) && !attr.equals(Curves.THICK) && !attr.equals(label))
                            {
                                mythick.remove(attr);
                            }
                        }

                        if (iters > 0)
                        {
                            Logging.info("smoothing: " + Curves.THICK);
                            CurvesSmooth.smoothInPlace(mythick, Curves.THICK, iters, lambda);
                        }
                    }

                    Logging.info("computing parameterized statistics");
                    for (Integer mylabel : CurvesUtils.list(curves, label))
                    {
                        String pname = String.format("%s_%03d", name, mylabel);
                        Logging.info("processing parameter: " + pname);
                        Record record = new Record();
                        record.with(CurvesUtils.measure(curves, label, mylabel));
                        if (thick)
                        {
                            record.with(CurvesUtils.measure(mythick, label, mylabel));
                        }
                        collector.put(pname, record);
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                Logging.error("an error occurred: " + e.getMessage());
            }
        };

        if (threads <= 1)
        {
            for (String name : names)
            {
                process.accept(name);
            }
        }
        else
        {
            Logging.info("using threads: " + threads);

            ExecutorService exec = Executors.newFixedThreadPool(threads);
            for (final String name : names)
            {
                exec.execute(() ->
                {
                    process.accept(name);
                });
            }

            exec.shutdown();
            try
            {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
            catch (InterruptedException e)
            {
                Logging.error("failed to execute in concurrent mode");
            }
        }

        Map<String, Record> results = Maps.newLinkedHashMap();
        for (String name : collector.keySet())
        {
            Record measures = collector.get(name);
            for (String measure : measures.keySet())
            {
                if (attrs.size() > 0)
                {
                    boolean pass = false;
                    for (String attr : attrs)
                    {
                        if (measure.contains(attr))
                        {
                            pass = true;
                        }
                    }
                    if (!pass)
                    {
                        continue;
                    }
                }

                if (!results.containsKey(measure))
                {
                    results.put(measure, new Record());
                }

                results.get(measure).with(name, measures.get(measure));
            }
        }

        PathUtils.mkdirs(output);
        for (String name : results.keySet())
        {
            String fn = PathUtils.join(output, name + ".csv");
            Logging.info("writing: " + fn);
            results.get(name).write(fn);
        }

        Logging.info("finished");
    }
}
