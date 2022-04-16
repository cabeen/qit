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
import com.google.common.collect.Queues;
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
import qit.data.datasets.Table;
import qit.data.datasets.Volume;
import qit.data.modules.curves.CurvesCrop;
import qit.data.modules.curves.CurvesMeasure;
import qit.data.modules.curves.CurvesThickness;
import qit.data.modules.volume.VolumeThreshold;
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
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MaskMeasureBatch implements CliMain
{
    public static void main(String[] args)
    {
        try
        {
            new MaskMeasureBatch().run(Lists.newArrayList(args));
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
        cli.withOption(new CliOption().asInput().withName("input").withArg("<FilePattern>").withDoc("specify an input mask filename pattern"));
        cli.withOption(new CliOption().asParameter().withName("names").withArg("<Spec>").withDoc("specify the case identifiers (e.g. a file that lists the subject ids)"));
        cli.withOption(new CliOption().asParameter().asOptional().withName("name").withArg("<String>").withDoc("specify an interpolation method").withDefault("name"));
        cli.withOption(new CliOption().asParameter().asOptional().withName("value").withArg("<String>").withDoc("specify an interpolation method").withDefault("value"));
        cli.withOption(new CliOption().asParameter().asOptional().withName("threads").withArg("<Integer>").withDoc("specify a number of threads").withDefault("1"));
        cli.withOption(new CliOption().asOutput().withName("output").withArg("<Table>").withDoc("specify an output table"));
        cli.withAuthor("Ryan Cabeen");

        Logging.info("parsing arguments");
        CliValues entries = cli.parse(args);

        Logging.info("started");
        String input = entries.keyed.get("input").get(0);
        String output = entries.keyed.get("output").get(0);
        String rnames = entries.keyed.get("names").get(0);
        List<String> names = CliUtils.names(rnames, Lists.newArrayList(rnames));

        int threads = entries.keyed.containsKey("threads") ? Integer.valueOf(entries.keyed.get("threads").get(0)) : 1;

        Logging.info(String.format("found %d structures", names.size()));

        Map<String, String> fns = Maps.newLinkedHashMap();
        for (String name : names)
        {
            String fn = String.format(input, name);
            fns.put(name, fn);
            Logging.info("using input: " + fn);
        }

        final String nameField = entries.keyed.get("name").get(0);
        final String valueField = entries.keyed.get("value").get(0);

        final Queue<Record> collector = Queues.newConcurrentLinkedQueue();

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

                Logging.info("processing: " + fn);
                Mask mask = Mask.read(fn);
                double volume = MaskUtils.volume(mask);
                collector.add(new Record().with(nameField, name).with(valueField, volume));
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

        Logging.info("writing output: " + output);
        Table out = new Table();
        out.withField(nameField);
        out.withField(valueField);
        out.addRecords(collector);
        out.write(output);

        Logging.info("finished");
    }
}
