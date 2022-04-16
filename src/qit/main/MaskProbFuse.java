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
import qit.base.cli.CliOption;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Volume;
import qit.data.source.MaskSource;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;

public class MaskProbFuse implements CliMain
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

            String doc = "Fuse probability maps into a multi-label mask.  Each input volume should be a probability map for an individual region.  The labels will match the order of the inputs";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<Volume(s)>").withDoc("the input probability maps").withNoMax());
            cli.withOption(new CliOption().asInput().withName("thresh").withArg("<double>").withDoc("specify the minimum probability threshold").withDefault("0.25"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-label").withArg("<Mask>").withDoc("specify the output maxima likelihood label"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-prob").withArg("<Volume>").withDoc("specify the output label probability"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            List<String> inputFns = entries.keyed.get("input");
            double thresh = Double.valueOf(entries.keyed.get("thresh").get(0));

            Logging.info(String.format("found %d volumes", inputFns.size()));

            Volume probs = null;

            if (inputFns.size() == 1)
            {
                Logging.info("detected single volume");
                probs = Volume.read(inputFns.get(0));
            }
            else
            {
                Logging.info("detected many volumes");
                for (int i = 0; i < inputFns.size(); i++)
                {
                    String fn = inputFns.get(i);

                    Logging.info("reading: " + fn);
                    Volume volume = Volume.read(fn);

                    if (probs == null)
                    {
                        probs = volume.proto(inputFns.size());
                    }

                    probs.setVolume(i, volume);
                }
            }

            Logging.info("detected labels: " + probs.getDim());

            Volume prob = probs.proto(1);
            Mask label = MaskSource.create(prob.getSampling());

            for (Sample sample : label.getSampling())
            {
                // initialize to background
                int maxLabel = 0;
                double maxProb = 0;

                for (int i = 0; i < probs.getDim(); i++)
                {
                    double p = probs.get(sample, i);

                    if (p > maxProb && p > thresh)
                    {
                        maxProb = p;
                        maxLabel = i + 1; // zero is background
                    }
                }

                prob.set(sample, 0, maxProb);
                label.set(sample, maxLabel);
            }

            if (entries.keyed.containsKey("output-label"))
            {
                String fn = entries.keyed.get("output-label").get(0);
                Logging.info("writing label map: " + fn);
                label.write(fn);
            }

            if (entries.keyed.containsKey("output-prob"))
            {
                String fn = entries.keyed.get("output-prob").get(0);
                Logging.info("writing probability map: " + fn);
                prob.write(fn);
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
