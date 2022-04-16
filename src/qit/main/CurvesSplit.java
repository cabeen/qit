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
import qit.base.CliMain;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.data.datasets.Curves;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.utils.CurvesUtils;
import qit.data.utils.TableUtils;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CurvesSplit implements CliMain
{
    public static void main(String[] args)
    {
        new CurvesSplit().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "Split a curves file into multiple parts";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<File>").withDoc("specify an input curves file"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("attr").withArg("<String>").withDoc("use a specific label attribute name").withDefault(Curves.LABEL));
            cli.withOption(new CliOption().asInput().asOptional().withName("table").withArg("<File>").withDoc("specify a table for looking up names"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("names").withArg("<String(s)>").withDoc("specify a which of names to extract"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<Pattern>").withDoc("specify the output filename pattern with %s"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            String input = entries.keyed.get("input").get(0);
            String attr = entries.keyed.get("attr").get(0);
            String output = entries.keyed.get("output").get(0);

            Curves curves = Curves.read(input);

            Map<Integer, String> lut = Maps.newHashMap();
            if (entries.keyed.containsKey("table"))
            {
                String fn = entries.keyed.get("table").get(0);
                Logging.info("reading: " + fn);
                Record map = TableUtils.createLookup(Table.read(fn));
                for (String name : map.keySet())
                {
                    Integer label = Integer.valueOf(map.get(name));
                    lut.put(label, name);
                }
            }
            else
            {
                for (Integer label : CurvesUtils.list(curves, attr))
                {
                    lut.put(label, String.valueOf(label));
                }
            }

            Set<String> names = Sets.newHashSet();
            if (entries.keyed.containsKey("names"))
            {
                Logging.info("reading names");
                names.addAll(CliUtils.names(entries.keyed.get("names")));
                Logging.info(String.format("using %d names", names.size()));
            }
            else
            {
                names.addAll(lut.values());
            }

            Map<String, Curves> groups = Maps.newLinkedHashMap();
            for (Curves.Curve curve : curves)
            {
                int label = MathUtils.round(curve.get(attr, 0).get(0));

                if (!lut.containsKey(label))
                {
                    continue;
                }

                String name = lut.get(label);

                if (!names.contains(name))
                {
                    continue;
                }

                if (!groups.containsKey(name))
                {
                    Curves group = new Curves();
                    groups.put(name, group);
                }

                groups.get(name).add(curve);
            }

            for (String name : groups.keySet())
            {
                String fn = String.format(output, name);
                Logging.info(String.format("writing %s to %s", name, fn));
                groups.get(name).write(fn);
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