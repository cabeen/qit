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
import qit.base.utils.PathUtils;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.modules.mask.MaskBinarize;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.MaskUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class MaskCat implements CliMain
{
    public static void main(String[] args)
    {
        new MaskCat().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "Concatenate mask files";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<File(s)>").withDoc("specify an input masks").withNoMax());
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<File>").withDoc("specify the output"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            List<String> input = entries.keyed.get("input");
            String output = entries.keyed.get("output").get(0);

            Logging.info(String.format("detected %d masks", input.size()));

            Function<String, Mask> readit = (fn) ->
            {
                Mask out = null;
                try
                {
                    out = Mask.read(fn);
                }
                catch (IOException e)
                {
                    Logging.error("error: failed to find mask: " + fn);
                }

                return MaskUtils.binarize(out);
            };

            Mask out = readit.apply(input.get(0));

            for (int i = 1; i < input.size(); i++)
            {
                Logging.info("reading: " + input.get(i));

                Mask mask = readit.apply(input.get(i));
                for (Sample sample : out.getSampling())
                {
                    if (out.valid(sample, mask))
                    {
                        out.set(sample, i + 1);
                    }
                }
            }

            Logging.info("writing: " + output);
            out.write(output);

            Logging.info("finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}