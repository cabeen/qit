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
import qit.base.cli.CliOption;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.data.datasets.Volume;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.utils.VolumeUtils;
import qit.data.utils.TableUtils;

import java.util.List;
import java.util.Map;

public class VolumeDiceBatch implements CliMain
{
    public static void main(String[] args)
    {
        new VolumeDiceBatch().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "Compute the weighted dice coefficient between a set of density maps.  Citation: Cousineau et al. NeuroImage: Clinical 2017";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("left").withArg("<FilePattern>").withDoc("specify the first set of segmentations"));
            cli.withOption(new CliOption().asInput().withName("right").withArg("<FilePattern>").withDoc("specify the second set of segmentations"));
            cli.withOption(new CliOption().asParameter().withName("names").withArg("<Spec>").withDoc("specify bundle identifiers (e.g. a file that lists the bundle names)"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<File>").withDoc("specify an output table"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            String left = entries.keyed.get("left").get(0);
            String right = entries.keyed.get("right").get(0);
            String output = entries.keyed.get("output").get(0);

            List<Volume> leftVolumes = Lists.newArrayList();
            List<Volume> rightVolumes = Lists.newArrayList();
            List<String> segNames = Lists.newArrayList();

            String rnames = entries.keyed.get("names").get(0);
            List<String> names = CliUtils.names(rnames, Lists.newArrayList(rnames));

            for (String name : names)
            {
                Logging.info(String.format("... reading %s", name));
                leftVolumes.add(Volume.read(String.format(left, name)));
                rightVolumes.add(Volume.read(String.format(right, name)));
                segNames.add(name);
            }

            Logging.info(String.format("found %d labels", leftVolumes.size()));

            Table out = new Table();
            out.addField("name");
            out.addField("value");

            for (int i = 0; i < segNames.size(); i++)
            {
                String name = segNames.get(i);
                Logging.info(String.format("... processing %s", name));
                double dice = VolumeUtils.diceWeighted(leftVolumes.get(i), rightVolumes.get(i));
                out.addRecord(new Record().with("name", name).with("value", String.valueOf(dice)));
            }

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
