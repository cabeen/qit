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
import qit.base.CliMain;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliValues;
import qit.data.datasets.Mask;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.models.Fibers;

import java.util.List;

public class VolumeFibersCat implements CliMain
{
    public static void main(String[] args)
    {
        new VolumeFibersCat().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "concatenate fibers volumes";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<FibersVolume(s)>").withDoc("the input fibers volumes").withNoMax());
            cli.withOption(new CliOption().asInput().asOptional().withName("mask").withArg("<Mask>").withDoc("specify a mask"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<FibersVolume>").withDoc("specify the output fibers volume"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            List<String> inputFns = entries.keyed.get("input");
            String outputFn = entries.keyed.get("output").get(0);

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

            Sampling sampling = input.get(0).getSampling();
            int size = sampling.size();
            int step = size / 100;

            int count = 0;
            for (Volume volume : input)
            {
                count += Fibers.count(volume.getDim());
            }

            Fibers proto = new Fibers(count);
            Volume out = VolumeSource.create(sampling, proto.getEncodingSize());

            Logging.progress("concatenating volumes");
            for (int idx = 0; idx < size; idx++)
            {
                if (idx % step == 0)
                {
                    Logging.progress(String.format("%d percent processed", 100 * idx / (size - 1)));
                }

                if (out.valid(idx, mask))
                {
                    continue;
                }

                Fibers cat = proto.proto();

                VectOnlineStats s0Stats = new VectOnlineStats();
                VectOnlineStats diffStats = new VectOnlineStats();
                int fidx = 0;
                for (Volume volume : input)
                {
                    Fibers model = new Fibers(volume.get(idx));
                    double diff = model.getDiffusivity();
                    double s0 = model.getBaseline();

                    for (int midx = 0; midx < model.size(); midx++)
                    {
                        double frac = model.getFrac(midx);
                        Vect line = model.getLine(midx);

                        frac /= input.size();

                        cat.setFrac(fidx, frac);
                        cat.setLine(fidx, line);
                        fidx += 1;
                    }

                    s0Stats.update(s0);
                    diffStats.update(diff);
                }
                cat.setBaseline(s0Stats.mean);
                cat.setDiffusivity(diffStats.mean);

                out.set(idx, cat.getEncoding());
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