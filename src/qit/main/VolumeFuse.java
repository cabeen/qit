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
import com.google.common.collect.Sets;
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.volume.VolumeStats;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Set;

public class VolumeFuse implements CliMain
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

            String doc = "fuse volumes";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<Volume(s)>").withDoc("the input volumes").withNoMax());
            cli.withOption(new CliOption().asInput().asOptional().withName("mask").withArg("<Mask>").withDoc("specify a mask"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("pattern").withArg("<String(s)>").withDoc("specify a list of names that will be substituted with input %s"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("bg").withArg("<Double>").withDoc("specify an additional background value for softmax or sumnorm"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("gain").withArg("<Double>").withDoc("specify a gain for softmax"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("offset").withArg("<Double>").withDoc("specify a offset for softmax"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("skip").withDoc("skip missing files"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("limit").withDoc("do not load more than this number of images"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("robust").withDoc("use robust statistics"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("exmulti").withDoc("exclude extra multi-channel volumes (load only the first one)"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("norm").withDoc("use the vector norm (or absolute value)"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-cat").withArg("<Volume>").withDoc("specify the output concatenated volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-min").withArg("<Volume>").withDoc("specify the output min volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-max").withArg("<Volume>").withDoc("specify the output max volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-sum").withArg("<Volume>").withDoc("specify the output sum volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-mean").withArg("<Volume>").withDoc("specify the output mean volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-median").withArg("<Volume>").withDoc("specify the output median volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-var").withArg("<Volume>").withDoc("specify the output var volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-std").withArg("<Volume>").withDoc("specify the output std volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-cv").withArg("<Volume>").withDoc("specify the output cv volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-softmax").withArg("<Volume>").withDoc("specify the output softmax"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-sumnorm").withArg("<Volume>").withDoc("specify the output sumnorm"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-first").withArg("<Volume>").withDoc("specify the output first volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-last").withArg("<Volume>").withDoc("specify the output last volume"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            List<String> inputFns = entries.keyed.get("input");
            boolean skip = entries.keyed.containsKey("skip");
            boolean robust = entries.keyed.containsKey("robust");
            boolean exmulti = entries.keyed.containsKey("exmulti");
            boolean norm = entries.keyed.containsKey("norm");

            Integer limit = Integer.MAX_VALUE;
            if (entries.keyed.containsKey("limit"))
            {
                limit = Integer.valueOf(entries.keyed.get("limit").get(0));
            }

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
                if (input.size() >= limit)
                {
                    Logging.info("warning: skipping subsequent volumes due to limit: " + limit);
                    break;
                }

                if (!PathUtils.exists(fn))
                {
                    if (skip)
                    {
                        Logging.info("ignoring: " + fn);
                    }
                    else
                    {
                        Logging.error("file not found: " + fn);
                    }
                    continue;
                }

                Logging.info("reading: " + fn);
                Volume volume = Volume.read(fn);
                input.add(volume);

                if (exmulti && volume.getDim() > 1)
                {
                    Logging.info("warning: skipping subsequent volumes due to exmulti flag");
                    break;
                }
            }

            Mask mask = null;
            if (entries.keyed.containsKey("mask"))
            {
                String fn = entries.keyed.get("mask").get(0);
                Logging.info("using mask: " + fn);
                mask = Mask.read(entries.keyed.get("mask").get(0));
            }

            if (entries.keyed.containsKey("output-first"))
            {
                String fn = entries.keyed.get("output-first").get(0);
                Logging.info("writing first: " + fn);
                input.get(0).write(fn);
            }

            if (entries.keyed.containsKey("output-last"))
            {
                String fn = entries.keyed.get("output-last").get(0);
                Logging.info("writing first: " + fn);
                input.get(input.size() - 1).write(fn);
            }

            if (entries.keyed.containsKey("output-cat"))
            {
                String fn = entries.keyed.get("output-cat").get(0);

                int dim = 0;
                for (int i = 0; i < input.size(); i++)
                {
                    dim += input.get(i).getDim();
                }

                Volume cat = input.get(0).proto(dim);
                for (Sample sample : cat.getSampling())
                {
                    int idx = 0;
                    for (int i = 0; i < input.size(); i++)
                    {
                        Volume volume = input.get(i);
                        for (int j = 0; j < volume.getDim(); j++)
                        {
                            cat.set(sample, idx, volume.get(sample, j));
                            idx += 1;
                        }
                    }
                }

                Logging.info("writing cat: " + fn);
                cat.write(fn);
            }

            if (entries.keyed.containsKey("output-softmax"))
            {
                String fn = entries.keyed.get("output-softmax").get(0);

                Double bg = null;
                Double gain = 1.0;
                Double offset = 0.0;

                if (entries.keyed.containsKey("bg"))
                {
                    bg = Double.valueOf(entries.keyed.get("bg").get(0));
                }

                if (entries.keyed.containsKey("gain"))
                {
                    gain = Double.valueOf(entries.keyed.get("gain").get(0));
                }

                if (entries.keyed.containsKey("offset"))
                {
                    offset = Double.valueOf(entries.keyed.get("offset").get(0));
                }

                Volume softmax = input.get(0).proto(input.size());
                for (Sample sample : softmax.getSampling())
                {
                    Vect values = VectSource.createND(input.size());

                    for (int i = 0; i < input.size(); i++)
                    {
                        values.set(i, input.get(i).get(sample, 0));
                    }

                    Vect exp = values.plus(offset).times(gain).exp();
                    double expsum = exp.sum();

                    if (bg != null)
                    {
                        expsum += Math.exp(gain * (bg + offset));
                    }

                    if (MathUtils.zero(expsum))
                    {
                        expsum = 1.0;
                    }

                    for (int i = 0; i < input.size(); i++)
                    {
                        softmax.set(sample, i, exp.get(i) / expsum);
                    }
                }

                Logging.info("writing softmax: " + fn);
                softmax.write(fn);
            }

            if (entries.keyed.containsKey("output-sumnorm"))
            {
                String fn = entries.keyed.get("output-sumnorm").get(0);

                Double bg = null;

                if (entries.keyed.containsKey("bg"))
                {
                    bg = Double.valueOf(entries.keyed.get("bg").get(0));
                }

                Volume softmax = input.get(0).proto(input.size());
                for (Sample sample : softmax.getSampling())
                {
                    Vect values = VectSource.createND(input.size());

                    for (int i = 0; i < input.size(); i++)
                    {
                        double v = input.get(i).get(sample, 0);
                        values.set(i, v);
                    }

                    double sum = values.sum();

                    if (bg != null)
                    {
                        sum += bg;
                    }

                    if (MathUtils.zero(sum))
                    {
                        sum = 1.0;
                    }

                    for (int i = 0; i < input.size(); i++)
                    {
                        softmax.set(sample, i, values.get(i) / sum);
                    }
                }

                Logging.info("writing sumnorm: " + fn);
                softmax.write(fn);
            }

            Set<String> stats = Sets.newHashSet();
            stats.add("output-min");
            stats.add("output-max");
            stats.add("output-sum");
            stats.add("output-var");
            stats.add("output-std");
            stats.add("output-mean");
            stats.add("output-median");
            stats.add("output-cv");

            boolean fuseit = false;
            for (String stat : stats)
            {
                if (entries.keyed.containsKey(stat))
                {
                    fuseit = true;
                    break;
                }
            }

            if (fuseit)
            {
                VolumeStats fuser = new VolumeStats();
                fuser.withInput(input);
                fuser.withRobust(robust);
                fuser.withNorm(norm);

                if (mask != null)
                {
                    fuser.withMask(mask);
                }

                Logging.info("started fuser");
                fuser.run();

                if (entries.keyed.containsKey("output-min"))
                {
                    String fn = entries.keyed.get("output-min").get(0);
                    Logging.info("writing mean: " + fn);
                    fuser.min.write(fn);
                }

                if (entries.keyed.containsKey("output-max"))
                {
                    String fn = entries.keyed.get("output-max").get(0);
                    Logging.info("writing max: " + fn);
                    fuser.max.write(fn);
                }

                if (entries.keyed.containsKey("output-sum"))
                {
                    String fn = entries.keyed.get("output-sum").get(0);
                    Logging.info("writing sum: " + fn);
                    fuser.sum.write(fn);
                }

                if (entries.keyed.containsKey("output-mean"))
                {
                    String fn = entries.keyed.get("output-mean").get(0);
                    Logging.info("writing mean: " + fn);
                    fuser.mean.write(fn);
                }

                if (entries.keyed.containsKey("output-median"))
                {
                    String fn = entries.keyed.get("output-median").get(0);
                    Logging.info("writing median: " + fn);
                    fuser.median.write(fn);
                }

                if (entries.keyed.containsKey("output-var"))
                {
                    String fn = entries.keyed.get("output-var").get(0);
                    Logging.info("writing var: " + fn);
                    fuser.var.write(fn);
                }

                if (entries.keyed.containsKey("output-std"))
                {
                    String fn = entries.keyed.get("output-std").get(0);
                    Logging.info("writing std: " + fn);
                    fuser.std.write(fn);
                }

                if (entries.keyed.containsKey("output-cv"))
                {
                    String fn = entries.keyed.get("output-cv").get(0);
                    Logging.info("writing cv: " + fn);
                    fuser.cv.write(fn);
                }
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
