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

package qit.data.formats.volume;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import mil.nga.tiff.*;
import mil.nga.tiff.util.TiffConstants;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VolumeSource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static qit.data.formats.volume.BufferedImageVolumeCoder.importRGBA;

/**
 * Wrapper for TIFF library
 */
public class TiffVolumeCoder
{
    public static Map<Pair<Integer, Integer>, Integer> CACHE = Maps.newHashMap();

    public static boolean matches(String fn)
    {
        fn = fn.toLowerCase();

        return fn.endsWith("tiff") || fn.endsWith("tif");
    }

    public static Volume read(String fn) throws IOException
    {
        TIFFImage tiffImage = TiffReader.readTiff(new File(fn));
        List<FileDirectory> directories = tiffImage.getFileDirectories();

        Rasters rasters = directories.get(0).readRasters();
        int width = rasters.getWidth();
        int height = rasters.getHeight();
        int depth = directories.size();
        int samples = rasters.getSamplesPerPixel();
        int bits = 0;
        for (int i = 0; i < samples; i++)
        {
            bits = Math.max(bits, rasters.getBitsPerSample().get(i));
        }

        Logging.info("detected TIFF directories: " + depth);
        Logging.info("detected TIFF width: " + width);
        Logging.info("detected TIFF height: " + height);
        Logging.info("detected TIFF samples: " + samples);
        Logging.info("detected TIFF max bitrate: " + bits);

        for (int i = 0; i < samples; i++)
        {
            Logging.info(String.format("detected bit rate %d at sample %d", rasters.getBitsPerSample().get(i), i));
            Logging.info(String.format("detected field type %s at sample %d", rasters.getFieldTypes()[i].toString(), i));
        }

        CACHE.put(Pair.of(width, height), bits);

        Sampling sampling = SamplingSource.create(width, height, depth);
        Volume out = VolumeSource.create(sampling, samples);

        for (int k = 0; k < depth; k++)
        {
            // use the previously loaded raster to save time
            if (k > 0)
            {
                rasters = directories.get(k).readRasters();
            }

            for (int j = 0; j < height; j++)
            {
                for (int i = 0; i < width; i++)
                {
                    Number[] nums = rasters.getPixel(i, j);
                    for (int d = 0; d < samples; d++)
                    {
                        out.set(i, j, k, d, nums[d].doubleValue());
                    }
                }
            }
        }

        return out;
    }

    public static void write(Volume volume, String fn) throws IOException
    {
        Sampling sampling = volume.getSampling();
        int width = sampling.numI();
        int height = sampling.numJ();
        int depth = sampling.numK(); // maybe use this for multi-directory files?
        int samples = volume.getDim();
        int bits = 16;
        int format = TiffConstants.SAMPLE_FORMAT_UNSIGNED_INT;

        Pair<Integer, Integer> key = Pair.of(width, height);
        if (CACHE.containsKey(key))
        {
            bits = CACHE.get(key);
        }

        List<Integer> bitsList = Lists.newArrayList();
        int[] bitsArray = new int[samples];
        int[] formatArray = new int[samples];

        for (int i = 0; i < samples; i++)
        {
            bitsList.add(bits);
            bitsArray[i] = bits;
            formatArray[i] = format;
        }

        TIFFImage tiffImage = new TIFFImage();

        for (int k = 0; k < depth; k++)
        {
            Rasters rasters = new Rasters(width, height, bitsArray, formatArray);
            int rowsPerStrip = rasters.calculateRowsPerStrip(TiffConstants.PLANAR_CONFIGURATION_CHUNKY);

            for (int j = 0; j < height; j++)
            {
                for (int i = 0; i < width; i++)
                {
                    for (int d = 0; d < samples; d++)
                    {
                        double pixelValue = volume.get(i, j, k, d);
                        rasters.setPixelSample(d, i, j, pixelValue);
                    }
                }
            }

            FileDirectory directory = new FileDirectory();
            directory.setWriteRasters(rasters);
            directory.setImageWidth(width);
            directory.setImageHeight(height);
            directory.setSamplesPerPixel(samples);
            directory.setBitsPerSample(bitsList);
            directory.setRowsPerStrip(rowsPerStrip);
            directory.setSampleFormat(format);
            directory.setCompression(TiffConstants.COMPRESSION_NO);
            directory.setPhotometricInterpretation(TiffConstants.PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO);
            directory.setPlanarConfiguration(TiffConstants.PLANAR_CONFIGURATION_CHUNKY);

            tiffImage.add(directory);
        }

        TiffWriter.writeTiff(new File(fn), tiffImage);
    }
}
