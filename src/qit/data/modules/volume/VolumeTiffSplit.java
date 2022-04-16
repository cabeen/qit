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


package qit.data.modules.volume;

import com.google.common.collect.Lists;
import mil.nga.tiff.FileDirectory;
import mil.nga.tiff.Rasters;
import mil.nga.tiff.TIFFImage;
import mil.nga.tiff.TiffReader;
import mil.nga.tiff.TiffWriter;
import mil.nga.tiff.util.TiffConstants;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;

@ModuleDescription("Print info about the organization of a TIFF file")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTiffSplit implements Module
{
    @ModuleParameter
    @ModuleDescription("the filename of the input tiff file")
    public String input = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("extract only a specific sub-page (comma-separated list)")
    public String which = null;

    @ModuleParameter
    @ModuleDescription("the filename pattern of the output tiffs (should contain %d)")
    public String output = null;

    public VolumeTiffSplit run()
    {
        try
        {
            Logging.info("reading tiff header");

            TIFFImage tiffImage = TiffReader.readTiff(new File(this.input));
            List<FileDirectory> directories = tiffImage.getFileDirectories();

            List<Integer> idx = CliUtils.parseIndexList(this.which, directories.size());

            Logging.info("pages: " + directories.size());

            for (int k : idx)
            {
                Logging.info("reading page " + k);
                Rasters inRasters = directories.get(k).readRasters();
                int width = inRasters.getWidth();
                int height = inRasters.getHeight();
                int samples = inRasters.getSamplesPerPixel();


                List<Integer> bitsList = Lists.newArrayList();
                int[] bitsArray = new int[samples];
                int[] formatArray = new int[samples];

                for (int j = 0; j < samples; j++)
                {
                    int bits = inRasters.getBitsPerSample().get(j);

                    bitsList.add(bits);
                    bitsArray[j] = bits;
                    formatArray[j] = inRasters.getSampleFormat().get(j);
                }

                Rasters outRasters = new Rasters(width, height, bitsArray, formatArray);
                int rowsPerStrip = outRasters.calculateRowsPerStrip(TiffConstants.PLANAR_CONFIGURATION_CHUNKY);

                for (int j = 0; j < height; j++)
                {
                    for (int i = 0; i < width; i++)
                    {
                        Number[] nums = inRasters.getPixel(i, j);
                        for (int d = 0; d < samples; d++)
                        {
                            outRasters.setPixelSample(d, i, j, nums[d]);
                        }
                    }
                }

                FileDirectory directory = new FileDirectory();
                directory.setWriteRasters(outRasters);
                directory.setImageWidth(width);
                directory.setImageHeight(height);
                directory.setSamplesPerPixel(samples);
                directory.setBitsPerSample(bitsList);
                directory.setRowsPerStrip(rowsPerStrip);
                directory.setSampleFormat(formatArray[0]);
                directory.setCompression(TiffConstants.COMPRESSION_NO);
                directory.setPhotometricInterpretation(TiffConstants.PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO);
                directory.setPlanarConfiguration(TiffConstants.PLANAR_CONFIGURATION_CHUNKY);

                Logging.info("writing page " + k);
                TIFFImage outImage = new TIFFImage();
                outImage.add(directory);
                TiffWriter.writeTiff(new File(String.format(this.output, k)), outImage);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Logging.error(e.getMessage());
        }

        return this;
    }
}
