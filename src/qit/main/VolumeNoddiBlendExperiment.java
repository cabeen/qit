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
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.mri.structs.Gradients;
import qit.data.models.Noddi;
import qit.data.utils.mri.estimation.NoddiEstimator;
import qit.math.structs.VectFunction;

import java.util.List;
import java.util.Map;

public class VolumeNoddiBlendExperiment implements CliMain
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

            String doc = "run a noddi blending experiment";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("gradients").withArg("<Gradients>").withDoc("specify gradientss"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("size").withArg("<Integer>").withDoc("specify a size").withDefault("21"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("abase").withArg("<Double>").withDoc("specify a noddi parameter").withDefault("1"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("bbase").withArg("<Double>").withDoc("specify a noddi parameter").withDefault("1"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("aicvf").withArg("<Double>").withDoc("specify a noddi parameter").withDefault("0.70"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("bicvf").withArg("<Double>").withDoc("specify a noddi parameter").withDefault("0.70"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("aisovf").withArg("<Double>").withDoc("specify a noddi parameter").withDefault("0.01"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("bisovf").withArg("<Double>").withDoc("specify a noddi parameter").withDefault("0.01"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("aod").withArg("<Double>").withDoc("specify a noddi parameter").withDefault("0.2"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("bod").withArg("<Double>").withDoc("specify a noddi parameter").withDefault("0.2"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("angle").withArg("<Double>").withDoc("specify an angle between models in degrees").withDefault("90"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<Directory>").withDoc("specify an output directory").withNum(1));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            Gradients gradients = Gradients.read(entries.keyed.get("gradients").get(0));
            Integer size = Integer.valueOf(entries.keyed.get("size").get(0));
            Double abase = Double.valueOf(entries.keyed.get("abase").get(0));
            Double bbase = Double.valueOf(entries.keyed.get("bbase").get(0));
            Double aicvf = Double.valueOf(entries.keyed.get("aicvf").get(0));
            Double bicvf = Double.valueOf(entries.keyed.get("bicvf").get(0));
            Double aisovf = Double.valueOf(entries.keyed.get("aisovf").get(0));
            Double bisovf = Double.valueOf(entries.keyed.get("bisovf").get(0));
            Double aod = Double.valueOf(entries.keyed.get("aod").get(0));
            Double bod = Double.valueOf(entries.keyed.get("bod").get(0));
            Double angle = Double.valueOf(entries.keyed.get("angle").get(0));
            String output = entries.keyed.get("output").get(0);

            double x = Math.cos(Math.toRadians(angle));
            double y = Math.sin(Math.toRadians(angle));

            PathUtils.mkdirs(output);

            Sampling sampling = SamplingSource.create(size, size, size);
            Map<String,NoddiEstimator> est = Maps.newHashMap();
            Map<String,Volume> out = Maps.newHashMap();
            out.put("signal", VolumeSource.create(sampling, gradients.size()));
            for (String method : NoddiEstimator.METHODS)
            {
                NoddiEstimator e = new NoddiEstimator();
                e.estimation = method;

                est.put(method, e);
                out.put(method, VolumeSource.create(sampling, new Noddi().getEncodingSize()));
            }

            List<Double> ws = Lists.newArrayList();
            ws.add(Double.valueOf(0));
            ws.add(Double.valueOf(0));
            List<Vect> vs = Lists.newArrayList();
            vs.add(null);
            vs.add(null);

            for (int i = 0; i < size; i++)
            {
                double wi = i / (double) (size - 1);

                for (int j = 0; j < size; j++)
                {
                    double wj = (j + 1) / (double) (size + 1);

                    for (int k = 0; k < size; k++)
                    {
                        double wk = (k + 1) / (double) (size + 1);

                        Noddi a = new Noddi();
                        a.setFISO(aisovf);
                        a.setBaseline(abase);
                        a.setFICVF(wj);
                        a.setODI(wk);
                        a.setDir(VectSource.create3D(1, 0, 0));

                        Noddi b = new Noddi();
                        b.setFISO(bisovf);
                        b.setBaseline(bbase);
                        b.setFICVF(wj);
                        b.setODI(wk);
                        b.setDir(VectSource.create3D(x, y, 0));

                        VectFunction synther = Noddi.synth(gradients);
                        Vect sa = synther.apply(a.getEncoding());
                        Vect sb = synther.apply(b.getEncoding());

                        out.get("signal").set(i, j, k, sa.times(wi).plus(sb.times(1.0 - wi)));

                        ws.set(0, wi);
                        ws.set(1, 1.0 - wi);
                        vs.set(0, a.getEncoding());
                        vs.set(1, b.getEncoding());

                        for (String method : NoddiEstimator.METHODS)
                        {
                            out.get(method).set(i, j, k, est.get(method).run(ws, vs));
                        }
                    }
                }
            }

            out.get("signal").write(PathUtils.join(output, "signal.nii.gz"));
            for (String method : NoddiEstimator.METHODS)
            {
                out.get(method).write(PathUtils.join(output, String.format("%s.noddi", method)));
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
