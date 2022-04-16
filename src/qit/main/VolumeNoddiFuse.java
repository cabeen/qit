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
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.models.Noddi;
import qit.data.utils.mri.estimation.NoddiEstimator;

import java.util.List;

public class VolumeNoddiFuse implements CliMain
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

            String doc = "fuse a collection of noddi volumes";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<Volume(s)>").withDoc("specify the input noddi volumes").withNoMax());
            cli.withOption(new CliOption().asInput().asOptional().withName("mask").withArg("<Mask>").withDoc("specify a mask"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("pattern").withArg("<String(s)>").withDoc("specify a list of names that will be substituted with input %s"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("estimation").withArg("<String>").withDoc("specify a type of estimation").withDefault(NoddiEstimator.DEFAULT_ESTIMATION));
            cli.withOption(new CliOption().asParameter().asOptional().withName("fractions").withDoc("use volume fractions in estimation"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<Volume>").withDoc("specify the output noddi volume").withNum(1));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            List<String> inputFns = entries.keyed.get("input");
            String outputFn = entries.keyed.get("output").get(0);
            String estimation = entries.keyed.get("estimation").get(0);
            boolean fractions = entries.keyed.containsKey("fractions");

            if (entries.keyed.containsKey("pattern"))
            {
                List<String> names = CliUtils.names(entries.keyed.get("pattern").get(0), null);
                Global.assume(names != null, "invalid pattern");

                List<String> rawFns = inputFns;
                inputFns = Lists.newArrayList();

                for (String rawPair : rawFns)
                {
                    for (String name : names)
                    {
                        inputFns.add(rawPair.replace("%s", name));
                    }
                }
            }

            Logging.info(String.format("found %d volumes", inputFns.size()));
            List<Volume> input = Lists.newArrayList();
            for (String fn : inputFns)
            {
                Logging.info("reading: " + fn);
                input.add(Volume.read(fn));
            }

            Mask mask = null;
            if (entries.keyed.containsKey("mask"))
            {
                String fn = entries.keyed.get("mask").get(0);
                Logging.info("using mask: " + fn);
                mask = Mask.read(entries.keyed.get("mask").get(0));
            }

            NoddiEstimator estimator = new NoddiEstimator();
            estimator.estimation = estimation;
            estimator.weightICVF = fractions;
            estimator.weightISOVF = fractions;

            int support = 1;
            double hpos = 1.0;

            if (entries.keyed.containsKey("support"))
            {
                support = Integer.valueOf(entries.keyed.get("support").get(0));
            }
            if (entries.keyed.containsKey("hpos"))
            {
                hpos = Double.valueOf(entries.keyed.get("hpos").get(0));
            }

            Sampling sampling = input.get(0).getSampling();
            Volume filter = VolumeSource.gauss(sampling, support, support, support, hpos);
            Sampling fsampling = filter.getSampling();

            Noddi proto = new Noddi();
            Volume out = VolumeSource.create(sampling, proto.getEncodingSize());

            int cx = (fsampling.numI() - 1) / 2;
            int cy = (fsampling.numJ() - 1) / 2;
            int cz = (fsampling.numK() - 1) / 2;

            int size = sampling.size();
            int step = Math.max(1, size / 50);

            Logging.progress("fusing");
            for (Sample sample : sampling)
            {
                int idx = sampling.index(sample);
                if (idx % step == 0)
                {
                    Logging.progress(String.format("%d percent processed", 100 * idx / (size - 1)));
                }

                if (!out.valid(sample, mask))
                {
                    continue;
                }

                List<Double> weights = Lists.newArrayList();
                List<Vect> models = Lists.newArrayList();
                for (Sample fsample : fsampling)
                {
                    int ni = sample.getI() + fsample.getI() - cx;
                    int nj = sample.getJ() + fsample.getJ() - cy;
                    int nk = sample.getK() + fsample.getK() - cz;
                    Sample nsample = new Sample(ni, nj, nk);

                    if (sampling.contains(nsample))
                    {
                        for (Volume vol : input)
                        {
                            Vect nmodel = vol.get(nsample);
                            double weight = filter.get(fsample, 0) / input.size();

                            models.add(nmodel);
                            weights.add(weight);
                        }
                    }
                }

                Vect estimate = estimator.run(weights, models);

                if (estimate != null)
                {
                    out.set(sample, estimate);
                }
            }

            Logging.info("writing: " + outputFn);
            out.write(outputFn);

            Logging.info("finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}
