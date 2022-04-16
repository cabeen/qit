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
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Curves;
import qit.data.datasets.Record;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;

import java.util.List;
import java.util.Map;

public class CurvesTable implements CliMain
{
    public static void main(String[] args)
    {
        new CurvesTable().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "Concatenate curves files";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<File(s)>").withDoc("specify an input curves").withNoMax());
            cli.withOption(new CliOption().asParameter().asOptional().withName("names").withArg("<String(s)>").withDoc("use pattern-based input with the given names"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("label").withDoc("add a label attribute"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("attr").withArg("<String>").withDoc("use a specific label attribute name").withDefault(Curves.LABEL));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-table").withArg("<File>").withDoc("specify the output table name"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<File>").withDoc("specify the output"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            List<String> input = entries.keyed.get("input");
            boolean label = entries.keyed.containsKey("label");
            String attr = entries.keyed.get("attr").get(0);
            String output = entries.keyed.get("output").get(0);

            Map<String,String> fns = Maps.newLinkedHashMap();
            if (entries.keyed.containsKey("names"))
            {
                Logging.info("using pattern-based input");
                if (input.size() != 1)
                {
                    Logging.error("expected a single input pattern");
                }

                String pattern = input.get(0);
                for (String name : CliUtils.names(entries.keyed.get("names")))
                {
                    String fn = String.format(pattern, name);
                    fns.put(name, fn);
                }
            }
            else
            {
                Logging.info("using filename input");

                // use filenames if no names are explicitly specified
                for (String fn : input)
                {
                    fns.put(fn, fn);
                }
            }

            Curves out = null;
            Record lut = new Record();
            int idx = 1;
            for (String name : fns.keySet())
            {
                String fn = fns.get(name);
                if (!PathUtils.exists(fn))
                {
                    Logging.info("skipping: " + fn);
                    continue;
                }

                Logging.info("reading: " + fn);
                Curves curves = Curves.read(fn);

                if (label)
                {
                    curves.add(attr, VectSource.create1D(0));
                    CurvesUtils.attrSetAll(curves, attr, VectSource.create1D(idx));
                    lut.with(name, String.valueOf(idx));
                    idx += 1;
                }

                if (out == null)
                {
                    out = curves;
                }
                else
                {
                    out.add(curves);
                }
            }
            
            Logging.info("writing curves: " + output);
            out.write(output);

            if (entries.keyed.containsKey("output-table"))
            {
                String fn = entries.keyed.get("output-table").get(0);

                Logging.info("writing table: " + fn);
                lut.write(fn);
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