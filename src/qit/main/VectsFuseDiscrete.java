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
import qit.base.cli.CliOption;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectsStats;
import qit.math.structs.Tally;

import java.util.List;
import java.util.Set;

public class VectsFuseDiscrete implements CliMain
{
    public static void main(String[] args)
    {
        new VectsFuseDiscrete().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "fuse vects";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<Vect(s)>").withDoc("the input vects").withNoMax());
            cli.withOption(new CliOption().asParameter().asOptional().withName("pattern").withArg("<String(s)>").withDoc("specify a list of names that will be substituted with input %s"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("skip").withDoc("skip missing files"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output").withArg("<Vect>").withDoc("specify the output mode vects"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            List<String> inputFns = entries.keyed.get("input");
            boolean skip = entries.keyed.containsKey("skip");

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

            Logging.info(String.format("found %d vects", inputFns.size()));
            Vects input = new Vects();
            for (String fn : inputFns)
            {
                if (!PathUtils.exists(fn))
                {
                    if (skip)
                    {
                        Logging.info("ignoring: " + fn);
                    } else
                    {
                        Logging.error("file not found: " + fn);
                    }
                    continue;
                }

                Vect vect = Vect.read(fn);
                input.add(vect);

                Logging.info("read " + fn + " with " + vect.size() + " dims");
            }

            Logging.info("detected size: " + input.size());
            Logging.info("detected dim: " + input.getDim());

            Logging.info("started fuser");
            Vects out = new Vects();
            for (int i = 0; i < input.getDim(); i++)
            {
                Tally tally = new Tally();
                for (int j = 0; j < input.size(); j++)
                {
                    tally.increment((int) input.get(j).get(i));
                }
                out.add(VectSource.create1D(tally.mode()));
            }

            if (entries.keyed.containsKey("output"))
            {
                String fn = entries.keyed.get("output").get(0);
                Logging.info("writing mode: " + fn);
                out.write(fn);
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
