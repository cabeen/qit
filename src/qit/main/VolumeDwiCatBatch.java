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
import qit.base.structs.Pair;
import qit.base.structs.Pointer;
import qit.base.utils.JavaUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Curves;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.curves.CurvesCrop;
import qit.data.modules.curves.CurvesMeasure;
import qit.data.modules.curves.CurvesThickness;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.curves.CurvesFunctionSample;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.volume.VolumeInterpTrilinear;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class VolumeDwiCatBatch implements CliMain
{
    public static void main(String[] args)
    {
        try
        {
            new VolumeDwiCatBatch().run(Lists.newArrayList(args));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Logging.error("an error occurred");
        }
    }

    public void run(List<String> args) throws IOException
    {
        Logging.info("starting " + this.getClass().getSimpleName());

        String doc = "Concatenate multiple diffusion MRI datasets in batch mode";

        CliSpecification cli = new CliSpecification();
        cli.withName(this.getClass().getSimpleName());
        cli.withDoc(doc);
        cli.withOption(new CliOption().asInput().withName("input-dwi").withArg("<FilePattern>").withDoc("specify the pattern for loading dwi volumes (must contain %s)"));
        cli.withOption(new CliOption().asInput().withName("input-bvecs").withArg("<FilePattern>").withDoc("specify the pattern for loading b-vectors (must contain %s)"));
        cli.withOption(new CliOption().asInput().withName("input-bvals").withArg("<FilePattern>").withDoc("specify the pattern for loading b-values (must contain %s)"));
        cli.withOption(new CliOption().asParameter().withName("names").withArg("<Spec>").withDoc("specify the list of identifiers for %s substitution"));
        cli.withOption(new CliOption().asParameter().asOptional().withName("skip").withDoc("skip cases with missing data (otherwise exit with an error)"));
        cli.withOption(new CliOption().asOutput().withName("output-dwi").withArg("<Filename>").withDoc("specify the output dwi filename"));
        cli.withOption(new CliOption().asOutput().withName("output-bvecs").withArg("<Filename>").withDoc("specify the output b-bvectors filename"));
        cli.withOption(new CliOption().asOutput().withName("output-bvals").withArg("<Filename>").withDoc("specify the output b-values filename"));
        cli.withAuthor("Ryan Cabeen");

        Logging.info("parsing arguments");
        CliValues entries = cli.parse(args);

        Logging.info("started");
        String inputDwi = entries.keyed.get("input-dwi").get(0);
        String inputBvecs = entries.keyed.get("input-bvecs").get(0);
        String inputBvals = entries.keyed.get("input-bvals").get(0);

        String outputDwi = entries.keyed.get("output-dwi").get(0);
        String outputBvecs = entries.keyed.get("output-bvecs").get(0);
        String outputBvals = entries.keyed.get("output-bvals").get(0);

        String rnames = entries.keyed.get("names").get(0);
        List<String> names = CliUtils.names(rnames, Lists.newArrayList(rnames));

        boolean skip = entries.keyed.containsKey("skip");

        Logging.info(String.format("found %d datasets", names.size()));

        List<Pair<Integer, String>> dwis = Lists.newArrayList();
        Vects bvecs = new Vects();
        Vects bvals = new Vects();

        for (String name : names)
        {
            String fnDwi = String.format(inputDwi, name);
            String fnBvecs = String.format(inputBvecs, name);
            String fnBvals = String.format(inputBvals, name);

            boolean found = true;
            found &= PathUtils.exists(fnDwi);
            found &= PathUtils.exists(fnBvecs);
            found &= PathUtils.exists(fnBvals);

            Logging.info("preprocessing batch %s:", name);
            Logging.info("  dwi: %s", fnDwi);
            Logging.info("  bvecs: %s", fnBvecs);
            Logging.info("  bvals: %s", fnBvals);

            if (found)
            {
                Logging.info("reading gradients");
                try
                {
                    Gradients mygrads = Gradients.read(fnBvecs, fnBvals);

                    bvecs.addAll(mygrads.getBvecs());
                    bvals.addAll(mygrads.getBvals());
                    dwis.add(Pair.of(mygrads.size(), fnDwi));

                    Logging.info("  detected number of directions: " + mygrads.size());
                }
                catch (RuntimeException e)
                {
                    if (skip)
                    {
                        Logging.info("  warning, failed to read gradients, skipping dataset: " + name);
                    }
                    else
                    {
                        throw e;
                    }
                }
            }
            else if (skip)
            {
                Logging.info("  warning, at least one file was missing,  skipping dataset: " + name);
            }
        }

        if (dwis.size() == 0)
        {
            Logging.error("no datasets were found!");
        }

        Logging.info("found a total of %d gradient directions", bvals.size());

        int idx = 0;
        Volume dwi = null;

        for (int i = 0; i < dwis.size(); i++)
        {
            String myfn = dwis.get(i).b;
            int mysize = dwis.get(i).a;

            Logging.info("reading: " + myfn);
            Volume mydwi = Volume.read(myfn);

            if (mydwi.getDim() != mysize)
            {
                Logging.error(String.format("dwi dimension (%d) does not match number of gradients (%d)", mydwi.getDim(), mysize));
            }

            if (dwi == null)
            {
                dwi = mydwi.proto(bvecs.size());
            }

            for (int d = 0; d < mydwi.getDim(); d++)
            {
                Logging.info("  filling channel " + idx);
                for (Sample sample : mydwi.getSampling())
                {
                    dwi.set(sample, idx, mydwi.get(sample, d));
                }

                idx += 1;
            }
        }

        Logging.info("writing output bvecs to " + outputBvecs);
        bvecs.write(outputBvecs);

        Logging.info("writing output bvals to " + outputBvals);
        bvals.write(outputBvals);

        Logging.info("writing output dwi to " + outputDwi);
        dwi.write(outputDwi);

        Logging.info("finished");
    }
}
