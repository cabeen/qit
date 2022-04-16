/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.main;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.data.utils.TableUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class VolumesTable implements CliMain
{
    public static void main(String[] args)
    {
        new VolumeFuse().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "create a table from one or multiple volumes";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<String=Volume(s)>").withDoc("specify the input volumes (field=file field2=file2 ...)").withNoMax());
            cli.withOption(new CliOption().asInput().asOptional().withName("mask").withArg("<Mask>").withDoc("specify a mask"));
            cli.withOption(new CliOption().asInput().asOptional().withName("lookup").withArg("<Table>").withDoc("use a lookup to map mask labels to names"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("pattern").withArg("<String(s)>").withDoc("specify a list of names that will be substituted with input %s"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("multiple").withDoc("use multiple mask labels"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("lookupNameField").withArg("<String>").withDoc("specify a name field in the lookup table").withDefault(("name")));
            cli.withOption(new CliOption().asParameter().asOptional().withName("lookupIndexField").withArg("<String>").withDoc("specify an voxel field in the lookup table").withDefault("index"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("vi").withArg("<String>").withDoc("specify a voxel i voxel name"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("vj").withArg("<String>").withDoc("specify a voxel j voxel name"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("vk").withArg("<String>").withDoc("specify a voxel k voxel name"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("voxel").withArg("<String>").withDoc("specify a voxel voxel field name").withDefault("voxel"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<Table>").withDoc("specify the output table"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            List<String> inputPairs = entries.keyed.get("input");
            String outputFn = entries.keyed.get("output").get(0);

            String first = null;
            Map<String, Volume> volumes = Maps.newLinkedHashMap();

            if (entries.keyed.containsKey("pattern"))
            {
                List<String> names = CliUtils.names(entries.keyed.get("pattern").get(0), null);
                Global.assume(names != null, "invalid pattern");

                List<String> rawPairs = inputPairs;
                inputPairs = Lists.newArrayList();

                for (String rawPair : rawPairs)
                {
                    for (String name : names)
                    {
                        inputPairs.add(rawPair.replace("%s", name));
                    }
                }
            }

            Logging.info(String.format("found %d volumes", inputPairs.size()));

            for (String inputPair : inputPairs)
            {
                String[] pair = inputPair.split("=");
                Global.assume(pair.length == 2, "invalid input pair (use = to separate)");

                String field = pair[0];
                Logging.info("reading: " + pair[1]);
                Volume volume = Volume.read(pair[1]);
                volumes.put(field, volume);

                if (first == null)
                {
                    first = field;
                }
            }

            Mask vmask = null;
            Mask smask = null;

            if (entries.keyed.containsKey("mask"))
            {
                vmask = Mask.read(entries.keyed.get("mask").get(0));
            }

            if (entries.keyed.containsKey("range"))
            {
                String range = entries.keyed.get("range").get(0);
                try
                {
                    smask = MaskSource.create(volumes.get(first).getSampling(), range);
                }
                catch (IOException e)
                {
                    Logging.error("failed to parse range: " + range);
                }
            }

            String vi = null;
            String vj = null;
            String vk = null;
            String voxel = null;

            Schema schema = new Schema();
            if (entries.keyed.containsKey("vi"))
            {
                vi = entries.keyed.get("vi").get(0);
                schema.add(vi);
            }
            if (entries.keyed.containsKey("vj"))
            {
                vj = entries.keyed.get("vj").get(0);
                schema.add(vj);
            }
            if (entries.keyed.containsKey("vk"))
            {
                vk = entries.keyed.get("vk").get(0);
                schema.add(vk);
            }
            if (entries.keyed.containsKey("voxel"))
            {
                voxel = entries.keyed.get("voxel").get(0);
                schema.add(voxel);
            }

            for (String field : volumes.keySet())
            {
                schema.add(field);
            }

            Map<Integer, String> lut = null;
            boolean multiple = entries.keyed.containsKey("multiple");
            String nameField = null;
            String indexField = null;

            if (multiple)
            {
                nameField = entries.keyed.get("lookupNameField").get(0);
                indexField = entries.keyed.get("lookupIndexField").get(0);

                if (entries.keyed.containsKey("lookup"))
                {
                    schema.add(nameField);
                    Table prelut = Table.read(entries.keyed.get("lookup").get(0));
                    lut = TableUtils.createIntegerStringLookup(prelut, indexField, nameField);
                }
                else
                {
                    schema.add(indexField);
                }
            }

            Table table = new Table(schema);

            Sampling sampling = volumes.get(first).getSampling();
            for (Sample sample : sampling)
            {
                if (volumes.get(first).valid(sample, vmask) && volumes.get(first).valid(sample, smask))
                {
                    Record row = new Record();

                    if (vi != null)
                    {
                        row.with(vi, String.valueOf(sample.getI()));
                    }
                    if (vj != null)
                    {
                        row.with(vj, String.valueOf(sample.getJ()));
                    }
                    if (vk != null)
                    {
                        row.with(vk, String.valueOf(sample.getK()));
                    }
                    if (voxel != null)
                    {
                        row.with(voxel, String.valueOf(sampling.index(sample)));
                    }
                    for (String field : volumes.keySet())
                    {
                        row.with(field, String.valueOf(volumes.get(field).get(sample, 0)));
                    }

                    if (multiple)
                    {
                        int label = vmask.get(sample);
                        if (lut != null)
                        {
                            row.with(nameField, lut.get(label));
                        }
                        else
                        {
                            row.with(indexField, String.valueOf(label));
                        }
                    }

                    table.addRecord(row);
                }
            }

            Logging.info("writing: " + outputFn);
            table.write(outputFn);

            Logging.info("finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}