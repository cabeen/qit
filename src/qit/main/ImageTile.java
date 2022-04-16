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
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;

import java.io.IOException;
import java.util.List;

public class ImageTile implements CliMain
{
    public static void main(String[] args)
    {
        new ImageTile().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "combine image tiles into a single big image";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);

            cli.withOption(new CliOption().asInput().withName("pattern").withArg("<Pattern>").withDoc("specify an input pattern (containing two %d patterns for the rows and columns)"));
            cli.withOption(new CliOption().asInput().withName("maxidx").withArg("<Number>").withDoc("specify the maximum index").withDefault("100"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<File>").withDoc("specify the output"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            String pattern = entries.keyed.get("pattern").get(0);
            String output = entries.keyed.get("output").get(0);
            Integer maxidx = Integer.valueOf(entries.keyed.get("maxidx").get(0));
            boolean skip = entries.keyed.containsKey("skip");

            Integer rowStart = maxidx;
            Integer rowEnd = 0;
            Integer colStart = maxidx;
            Integer colEnd = 0;

            Logging.info("checking for tile ranges");
            for (int i = 0; i < maxidx; i++)
            {
                for (int j = 0; j < maxidx; j++)
                {
                    String fn = String.format(pattern, i, j);

                    if (PathUtils.exists(fn))
                    {
                        rowStart = Math.min(rowStart, j);
                        colStart = Math.min(colStart, i);
                        rowEnd = Math.max(rowEnd, j);
                        colEnd = Math.max(colEnd, i);
                    }
                }
            }

            rowEnd += 1;
            colEnd += 1;

            int rowNum = rowEnd - rowStart;
            int colNum = colEnd - colStart;

            Logging.info("detected row start: " + rowStart);
            Logging.info("detected row end: " + rowEnd);
            Logging.info("detected col start: " + colStart);
            Logging.info("detected col end: " + colEnd);

            Volume ref = Volume.read(String.format(pattern, rowStart, colStart));
            int widthTile = ref.getSampling().numI();
            int heightTile = ref.getSampling().numJ();

            int heightTotal = heightTile * rowNum;
            int widthTotal = widthTile * colNum;

            Logging.info("detected tile width: " + heightTile);
            Logging.info("detected tile height: " + widthTile);

            Logging.info("output width: " + widthTotal);
            Logging.info("detected height: " + heightTotal);

            Volume out = VolumeSource.create(widthTotal, heightTotal, 1, ref.getDim());

            for (int i = colStart; i < colEnd; i++)
            {
                for (int j = rowStart; j < rowEnd; j++)
                {
                    String fn = String.format(pattern, i, j);
                    if (!PathUtils.exists(fn))
                    {
                        Logging.info("... WARNING: skipping tile due to missing file: " + fn);
                        continue;
                    }

                    try
                    {
                        Logging.info("... reading tile: " + fn);
                        Volume tile = Volume.read(fn);
                        if (tile.getSampling().numI() != heightTile || tile.getSampling().numJ() != widthTile)
                        {
                            Logging.info("... WARNING: skipping tile due to size mismatch: " + fn);
                            continue;
                        }

                        Logging.info("... populating output");
                        for (int ii = 0; ii < widthTile; ii++)
                        {
                            for (int jj = 0; jj < heightTile; jj++)
                            {
                                out.set(i * widthTile + ii, j * heightTile + jj, 0, tile.get(ii, jj, 0));
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        Logging.info("... WARNING: skipping tile due to error loading: " + fn);
                    }
                    catch (RuntimeException e)
                    {
                        Logging.info("... WARNING: skipping tile due to error loading: " + fn);
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
