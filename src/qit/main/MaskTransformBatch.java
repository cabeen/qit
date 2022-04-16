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
import qit.base.structs.Pointer;
import qit.base.utils.PathUtils;
import qit.data.datasets.*;
import qit.data.modules.mask.MaskTransform;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;
import qit.data.utils.enums.InterpolationType;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MaskTransformBatch implements CliMain
{
    public static void main(String[] args)
    {
        new MaskTransformBatch().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "Apply a spatial transformation to many masks";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<FilePattern>").withDoc("specify a pattern for mask inputs (contains %s)"));
            cli.withOption(new CliOption().asParameter().withName("names").withArg("<Spec>").withDoc("specify identifiers (used for substitution of %s in the input and output)"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("reference").withArg("<Filename>").withDoc("specify a reference volume"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("affine").withArg("<Filename>").withDoc("specify an affine transform"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("deform").withArg("<Filename>").withDoc("specify a deformation"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("invaffine").withArg("<Filename>").withDoc("specify an inverse affine transform"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("reverse").withDoc("apply the reverse transform"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("threads").withArg("<Integer>").withDoc("use the given number of threads").withDefault("1"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<FilePattern>").withDoc("specify a pattern for mask outputs (contains %s)"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            final String input = entries.keyed.get("input").get(0);
            final String output = entries.keyed.get("output").get(0);
            final int threads = Integer.valueOf(entries.keyed.get("threads").get(0));

            final String rnames = entries.keyed.get("names").get(0);
            final List<String> names = CliUtils.names(rnames, Lists.newArrayList(rnames));

            final Pointer<Affine> affine = Pointer.to(null);
            final Pointer<Affine> invaffine = Pointer.to(null);
            final Pointer<Deformation> deform = Pointer.to(null);

            Logging.info("detected %d names", names.size());

            Logging.info("reading reference volume");
            Volume ref = Volume.read(entries.keyed.get("reference").get(0));

            if (entries.keyed.containsKey("affine"))
            {
                Logging.info("reading affine");
                affine.set(Affine.read(entries.keyed.get("affine").get(0)));
            }

            if (entries.keyed.containsKey("invaffine"))
            {
                Logging.info("reading inverse affine");
                invaffine.set(Affine.read(entries.keyed.get("invaffine").get(0)));
            }

            if (entries.keyed.containsKey("deform"))
            {
                Logging.info("reading deformation");
                deform.set(Deformation.read(entries.keyed.get("deform").get(0)));
            }

            Consumer<String> process = (name) ->
            {
                try
                {
                    String infn = String.format(input, name);
                    String outfn = String.format(output, name);

                    PathUtils.mkpar(outfn);

                    Logging.info("processing: " + name);

                    MaskTransform transformer = new MaskTransform();
                    transformer.input = Mask.read(infn);
                    transformer.reference = ref;
                    transformer.reverse = entries.keyed.containsKey("reverse");
                    transformer.affine = affine.get();
                    transformer.invaffine = invaffine.get();
                    transformer.deform = deform.get();
                    transformer.run().output.write(outfn);

                }
                catch (IOException e)
                {
                    Logging.info("warning: an error occurred processing " + name);
                }
            };

            if (threads <= 1)
            {
                for (String name : names)
                {
                    process.accept(name);
                }
            }
            else
            {
                Logging.info("using threads: " + threads);

                ExecutorService exec = Executors.newFixedThreadPool(threads);
                for (final String name : names)
                {
                    exec.execute(() -> process.accept(name));
                }

                exec.shutdown();
                try
                {
                    exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                }
                catch (InterruptedException e)
                {
                    Logging.error("failed to execute in concurrent mode");
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
