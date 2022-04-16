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
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import qit.base.CliMain;
import qit.base.Logging;
import qit.base.cli.CliOption;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliValues;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.volume.VolumeStats;
import qit.math.utils.MathUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VolumeParseEchoes implements CliMain
{
    public static void main(String[] args)
    {
        new VolumeParseEchoes().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "fuse volumes";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<Volume(s)>").withDoc("the input volumes").withNoMax());
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-volume").withArg("<Volume>").withDoc("specify the output volume"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-echoes").withArg("<Text>").withDoc("specify the output echo times"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            List<String> inputFns = entries.keyed.get("input");

            Logging.info(String.format("found %d volumes", inputFns.size()));

            List<Volume> volumes = Lists.newArrayList();
            Vects echoes = new Vects();

            for (String fn : inputFns)
            {
                if (!PathUtils.exists(fn))
                {
                    Logging.info("ignoring: " + fn);
                    continue;
                }

                Logging.info("reading volume: " + fn);
                Volume volume = Volume.read(fn);
                volumes.add(volume);

                double echo = 0;
                try
                {
                    String fnJson = fn.replace("nii.gz", "json");
                    Logging.info("reading json: " + fnJson);
                    String stringJson = FileUtils.readFileToString(new File(fnJson));
                    JsonObject dataJson = new Gson().fromJson(stringJson, JsonObject.class);
                    echo = Double.valueOf(dataJson.get("EchoTime").toString());
                    Logging.info("parsed echo time: " + echo);
                }
                catch (RuntimeException e)
                {
                    e.printStackTrace();
                }
                echoes.add(VectSource.create1D(echo));
            }

            int num = volumes.size();
            Integers which = echoes.flatten().sort();

            echoes = echoes.perm(which);
            Volume out = volumes.get(0).proto(num);
            for (Sample sample : out.getSampling())
            {
                Vect values = VectSource.createND(num);
                for (int i = 0; i < num; i++)
                {
                    values.set(i, volumes.get(i).get(sample, 0));
                }

                Vect sorted = values.perm(which);
                out.set(sample, sorted);
            }

            String volumeFn = entries.keyed.get("output-volume").get(0);
            String echoesFn = entries.keyed.get("output-echoes").get(0);

            Logging.info("writing output");
            out.write(volumeFn);
            echoes.write(echoesFn);

            Logging.info("finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}