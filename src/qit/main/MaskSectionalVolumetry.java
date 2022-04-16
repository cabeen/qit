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
import com.google.common.collect.Sets;
import org.apache.commons.lang3.text.StrSubstitutor;
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliOption;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.utils.ComboUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MaskSectionalVolumetry implements CliMain
{
    public static void main(String[] args)
    {
        new MaskSectionalVolumetry().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "perform volumetry from a collection of sectional segmentation masks";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);

            cli.withOption(new CliOption().asParameter().withName("pattern").withArg("pattern").withDoc("specify a list of patterns (contains: ${label}, ${rater}, ${case} and ${section})").withNoMax());
            cli.withOption(new CliOption().asParameter().withName("labels").withArg("<List>").withDoc("specify a list of label identifiers"));
            cli.withOption(new CliOption().asParameter().withName("raters").withArg("<List>").withDoc("specify a list of rater identifiers"));
            cli.withOption(new CliOption().asParameter().withName("cases").withArg("<List>").withDoc("specify a list of case identifiers"));
            cli.withOption(new CliOption().asParameter().withName("sections").withArg("<List>").withDoc("specify a list of case identifiers"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<Table>").withDoc("specify the output table"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            String pattern = entries.keyed.get("pattern").get(0).replace("%", "$");
            List<String> labels = CliUtils.names(entries.keyed.get("labels").get(0), null);
            List<String> raters = CliUtils.names(entries.keyed.get("raters").get(0), null);
            List<String> cases = CliUtils.names(entries.keyed.get("cases").get(0), null);
            List<String> sections = CliUtils.names(entries.keyed.get("sections").get(0), null);
            String output = entries.keyed.get("output").get(0);

            if (entries.pos.size() != 0)
            {
                Logging.error("invalid positional arguments");
            }

            Table table = new Table();
            table.withField("rater");
            table.withField("case");
            for (String label : labels)
            {
                table.withField(label);
            }

            for (String myrater : raters)
            {
                for (String mycase : cases)
                {
                    Record record = new Record();
                    record.with("rater", myrater);
                    record.with("case", mycase);

                    boolean any = false;

                    for (String mylabel : labels)
                    {
                        Logging.info(String.format("processing rater %s, case %s, label %s", myrater, mycase, mylabel));

                        double total = 0;

                        for (String mysection : sections)
                        {
                            Map<String, String> group = Maps.newLinkedHashMap();
                            group.put("label", mylabel);
                            group.put("rater", myrater);
                            group.put("case", mycase);
                            group.put("section", mysection);
                            String fn = new StrSubstitutor(group).replace(pattern);

                            Logging.info("... processing: " + fn);

                            if (PathUtils.exists(fn))
                            {
                                total += MaskUtils.count(Mask.read(fn));
                                any = true;
                            }
                            else
                            {
                                total += 0;
                            }
                        }

                        record.with(mylabel, String.valueOf(total));
                    }

                    if (any)
                    {
                        table.addRecord(record);
                    }
                }
            }

            Logging.info("writing output");
            table.write(output);

            Logging.info("finished");
        }

        catch (Exception e)

        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}
