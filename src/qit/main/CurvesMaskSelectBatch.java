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
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Curves;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.modules.curves.CurvesMaskSelect;

import java.util.List;

public class CurvesMaskSelectBatch implements CliMain
{
    public static void main(String[] args)
    {
        new CurvesMaskSelectBatch().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "select curves from masks in batch mode";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<File>").withDoc("specify an input curves"));
            cli.withOption(new CliOption().asInput().asOptional().withName("deform").withArg("<File>").withDoc("specify a deformation between curves and volumes"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("names").withArg("<Spec>").withDoc("specify bundle identifiers"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("thresh").withArg("<Value>").withDoc("specify threshold for containment"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("endpoints").withDoc("specify that only endpoints should be tested"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("invert").withDoc("specify masks should be inverted"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("binarize").withDoc("specify masks should be binarized"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("include").withArg("<FilePattern>").withDoc("specify an filename pattern for an include mask"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("exclude").withArg("<FilePattern>").withDoc("specify an filename pattern for an exclude mask"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("contain").withArg("<FilePattern>").withDoc("specify an filename pattern for an contain mask"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<FilePattern>").withDoc("specify an output directory"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            String input = entries.keyed.get("input").get(0);
            String output = entries.keyed.get("output").get(0);
            String rnames = entries.keyed.get("names").get(0);
            boolean endpoints = entries.keyed.containsKey("endpoints");
            boolean invert = entries.keyed.containsKey("invert");
            boolean binarize = entries.keyed.containsKey("binarize");
            List<String> names = CliUtils.names(rnames, Lists.newArrayList(rnames));

            Logging.info("reading input: " + input);
            Curves curves = Curves.read(input);

            CurvesMaskSelect select = new CurvesMaskSelect();
            select.input = curves;
            select.binarize = binarize;
            select.invert = invert;
            select.endpoints = endpoints;

            if (entries.keyed.containsKey("deform"))
            {
                String fn = entries.keyed.get("deform").get(0);

                Logging.info("reading deform: " + fn);
                Deformation deform = Deformation.read(fn);
                select.deform = deform;
            }

            Logging.info(String.format("found %d batches", names.size()));
            for (String name : names)
            {
                Logging.info("started batch: " + name);

                if (entries.keyed.containsKey("include"))
                {
                    String pattern = entries.keyed.get("include").get(0);
                    String fn = String.format(pattern, name);
                    Mask mask = Mask.read(fn);
                    select.include = mask;
                }

                if (entries.keyed.containsKey("exclude"))
                {
                    String pattern = entries.keyed.get("exclude").get(0);
                    String fn = String.format(pattern, name);
                    Mask mask = Mask.read(fn);
                    select.exclude = mask;
                }

                if (entries.keyed.containsKey("contain"))
                {
                    String pattern = entries.keyed.get("contain").get(0);
                    String fn = String.format(pattern, name);
                    Mask mask = Mask.read(fn);
                    select.contain = mask;
                }

                Curves selection = select.run().output;
                String fn = String.format(output, name);
                PathUtils.mkpar(fn);
                selection.write(fn);
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
