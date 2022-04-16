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
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.utils.volume.VolumeBlocker;

import java.util.List;

public class VolumeBlockRead implements CliMain
{
    public static void main(String[] args)
    {
        new VolumeBlockRead().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "compose blocks into a volume";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<Pattern>").withDoc("the input pattern (e.g. volume%d.nii.gz"));
            cli.withOption(new CliOption().asInput().withName("ref").withArg("<Volume>").withDoc("the reference volume"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("i").withArg("<Integer>").withDoc("the i block size"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("j").withArg("<Integer>").withDoc("the j block size"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("k").withArg("<Integer>").withDoc("the k block size").withDefault("1"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("skip").withDoc("skip files if they are missing"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<Volume>").withDoc("specify the output volume"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            boolean skip = entries.keyed.containsKey("skip");
            String inputPat = entries.keyed.get("input").get(0);
            String refFn = entries.keyed.get("ref").get(0);
            String outputFn = entries.keyed.get("output").get(0);

            VolumeBlocker blocker = new VolumeBlocker();
            if (entries.keyed.containsKey("i"))
            {
                int blockSizeX = Integer.valueOf(entries.keyed.get("i").get(0));
                Logging.info("using block size i: " + blockSizeX);
                blocker.withBlockSizeI(blockSizeX);
            }
            if (entries.keyed.containsKey("j"))
            {
                int blockSizeY = Integer.valueOf(entries.keyed.get("j").get(0));
                Logging.info("using block size j: " + blockSizeY);
                blocker.withBlockSizeJ(blockSizeY);
            }
            if (entries.keyed.containsKey("k"))
            {
                int blockSizeZ = Integer.valueOf(entries.keyed.get("k").get(0));
                Logging.info("using block size k: " + blockSizeZ);
                blocker.withBlockSizeK(blockSizeZ);
            }

            Logging.info("reading the reference volume: " + refFn);
            Global.setFresh(true);
            Sampling ref = Volume.read(refFn).getSampling();
            blocker.withSampling(ref);

            int blocks = blocker.getNumBlocks();
            Logging.info("using total block count: " + blocks);

            Volume volume = null;

            Logging.info("reading block: " + inputPat);
            for (int i = 0; i < blocks; i++)
            {
                String blockFn = String.format(inputPat, i);

                if (!PathUtils.exists(blockFn))
                {
                    if (!skip)
                    {
                        Logging.error("block not found: " + blockFn);
                    }

                    Logging.info(String.format("...skipping block (%d/%d)", i + 1, blocks));
                    continue;
                }

                Logging.info(String.format("...reading block (%d/%d)", i + 1, blocks));
                Volume blockVol = Volume.read(blockFn);

                if (volume == null)
                {
                    volume = VolumeSource.create(ref, blockVol.getDim());
                    volume.setModel(blockVol.getModel());
                }

                blocker.setBlock(volume, i, blockVol);
            }

            Logging.info("writing volume: " + outputFn);
            volume.write(outputFn);

            Logging.info("finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}
