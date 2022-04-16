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

import org.apache.commons.io.FilenameUtils;
import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Sample;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.volume.VolumeVoxelStats;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

/**
 * Java ImageIO reader for images with RGBA (assume 0 to 1.0)
 */
public class BufferedImageVolumeCoder
{
    public static boolean VERBOSE = false;

    public static boolean matches(String fn)
    {
        fn = fn.toLowerCase();

        return fn.endsWith("png") || fn.endsWith("jpg") || fn.endsWith("gif") || fn.endsWith("bmp") || fn.endsWith("tiff") || fn.endsWith("tif");
    }

    public static Volume importRGBA(BufferedImage img)
    {
        int w = img.getWidth();
        int h = img.getHeight();

        Volume out = VolumeSource.create(w, h, 1, 4);
        for (int i = 0; i < w; i++)
        {
            for (int j = 0; j < h; j++)
            {
                // this new arg constructor retrieves the alpha
                Color c = new Color(img.getRGB(i, j), true);
                double r = c.getRed() / 255.0;
                double g = c.getGreen() / 255.0;
                double b = c.getBlue() / 255.0;
                double a = c.getAlpha() / 255.0;

                out.set(i, j, 0, 0, r);
                out.set(i, j, 0, 1, g);
                out.set(i, j, 0, 2, b);
                out.set(i, j, 0, 3, a);
            }
        }

        return out;
    }

    public static Volume importRGB(BufferedImage img)
    {
        int w = img.getWidth();
        int h = img.getHeight();

        Volume out = VolumeSource.create(w, h, 1, 3);
        for (int i = 0; i < w; i++)
        {
            for (int j = 0; j < h; j++)
            {
                Color c = new Color(img.getRGB(i, j));
                double r = c.getRed() / 255.0;
                double g = c.getGreen() / 255.0;
                double b = c.getBlue() / 255.0;

                out.set(i, j, 0, 0, r);
                out.set(i, j, 0, 1, g);
                out.set(i, j, 0, 2, b);
            }
        }

        return out;
    }

    public static Volume importGrayByte(BufferedImage img)
    {
        int w = img.getWidth();
        int h = img.getHeight();

        DataBufferByte buffer = (DataBufferByte) img.getRaster().getDataBuffer();

        Volume out = VolumeSource.create(w, h, 1, 1);
        for (int i = 0; i < w; i++)
        {
            for (int j = 0; j < h; j++)
            {
                out.set(i, j, 0, 0, buffer.getElem(i + j * w));
            }
        }

        return out;
    }

    public static Volume importGrayUShort(BufferedImage img)
    {
        int w = img.getWidth();
        int h = img.getHeight();

        DataBufferUShort buffer = (DataBufferUShort) img.getRaster().getDataBuffer(); // Safe cast as img is of type TYPE_USHORT_GRAY

        Volume out = VolumeSource.create(w, h, 1, 1);
        for (int j = 0; j < h; j++)
        {
            for (int i = 0; i < w; i++)
            {
                out.set(i, j, 0, 0, buffer.getElem(i + j * w));
            }
        }

        return out;
    }

    private static BufferedImage exportGray(Volume volume, boolean high)
    {
        int numx = volume.getSampling().numI();
        int numy = volume.getSampling().numJ();
        int numz = volume.getSampling().numK();

        boolean nox = numx == 1;
        boolean noy = numy == 1;
        boolean noz = numz == 1;

        boolean oxy = !nox && !noy && noz;
        boolean oxz = !nox && noy && !noz;
        boolean oyz = nox && !noy && !noz;

        int width = oyz ? numy : numx;
        int height = oxy ? numy : numz;
        int dim = volume.getDim();

        Global.assume(oxy || oxz || oyz, "volume is not planar");
        Global.assume(dim == 1, "image channel is invalid");

        double max = high ? 65535.0 : 255.0;
        int type = high ? BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage img = new BufferedImage(width, height, type);
        WritableRaster raster = img.getRaster();

        // detect whether the image stored grayscale from zero to one
        VectOnlineStats stats = new VectOnlineStats();
        for (Sample sample : volume.getSampling())
        {
            stats.update(volume.get(sample, 0));
        }
        double scale = stats.mean + stats.std < 1.0 ? max : 1.0;

        for (int w = 0; w < width; w++)
        {
            for (int h = 0; h < height; h++)
            {
                int i = oyz ? 0 : w;
                int j = oxy ? h : oyz ? w : 0;
                int k = oxy ? 0 : h;
                int v = (int) Math.min(Math.max(scale * volume.get(i, j, k, 0), 0), max);

                raster.setSample(w, h, 0, v);
            }
        }

        return img;
    }

    private static BufferedImage exportRGB(Volume volume)
    {
        int numx = volume.getSampling().numI();
        int numy = volume.getSampling().numJ();
        int numz = volume.getSampling().numK();

        boolean nox = numx == 1;
        boolean noy = numy == 1;
        boolean noz = numz == 1;

        boolean oxy = !nox && !noy && noz;
        boolean oxz = !nox && noy && !noz;
        boolean oyz = nox && !noy && !noz;

        int width = oyz ? numy : numx;
        int height = oxy ? numy : numz;
        int dim = volume.getDim();

        Global.assume(oxy || oxz || oyz, "volume is not planar");
        Global.assume(dim == 1 || dim == 3 || dim == 4, "image channel is invalid");

        int type = dim == 1 ? BufferedImage.TYPE_BYTE_GRAY : dim == 3 ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage img = new BufferedImage(width, height, type);

        for (int w = 0; w < width; w++)
        {
            for (int h = 0; h < height; h++)
            {
                int i = oyz ? 0 : w;
                int j = oxy ? h : oyz ? w : 0;
                int k = oxy ? 0 : h;
                int r = 0;
                int g = 0;
                int b = 0;
                int a = 255;

                if (dim == 1)
                {
                    int v = (int) Math.min(Math.max(volume.get(i, j, k, 0) * 255.0, 0), 255);
                    r = v;
                    g = v;
                    b = v;
                }
                else
                {
                    r = (int) Math.min(Math.max(volume.get(i, j, k, 0) * 255.0, 0), 255);
                    g = (int) Math.min(Math.max(volume.get(i, j, k, 1) * 255.0, 0), 255);
                    b = (int) Math.min(Math.max(volume.get(i, j, k, 2) * 255.0, 0), 255);

                    if (dim == 4)
                    {
                        a = (int) Math.min(Math.max(volume.get(w, h, 0, 3) * 255.0, 0), 255);
                    }
                }

                img.setRGB(w, h, new Color(r, g, b, a).getRGB());
            }
        }

        return img;
    }

    public static Volume read(String fn) throws IOException
    {
        BufferedImage img = ImageIO.read(new File(fn));
        if (img == null)
        {
           throw new IOException("failed to read image: " + fn);
        }

        switch (img.getType())
        {
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                Logging.info(VERBOSE, "detected argb image");
                return importRGBA(img);
            case BufferedImage.TYPE_3BYTE_BGR:
            case BufferedImage.TYPE_INT_BGR:
            case BufferedImage.TYPE_INT_RGB:
            case BufferedImage.TYPE_USHORT_555_RGB:
            case BufferedImage.TYPE_USHORT_565_RGB:
                Logging.info(VERBOSE, "detected rgb image");
                return importRGB(img);
            case BufferedImage.TYPE_BYTE_BINARY:
                Logging.info(VERBOSE, "detected binary grayscale image");
                return importGrayByte(img);
            case BufferedImage.TYPE_BYTE_GRAY:
                Logging.info(VERBOSE, "detected 8-bit grayscale image");
                return importGrayByte(img);
            case BufferedImage.TYPE_USHORT_GRAY:
                Logging.info(VERBOSE, "detected 16-bit grayscale image");
                return importGrayUShort(img);
            default:
                Logging.info(VERBOSE, "could not detect image type");
                return importRGB(img);
        }
    }

    public static void write(Volume volume, String fn) throws IOException
    {
        if (volume.getDim() == 1)
        {
            if (fn.endsWith("tif") || fn.endsWith("tiff") || fn.endsWith("png"))
            {
                ImageIO.write(exportGray(volume, true), FilenameUtils.getExtension(fn), new File(fn));
            }
            else
            {
                ImageIO.write(exportGray(volume, false), FilenameUtils.getExtension(fn), new File(fn));
            }
        }
        else
        {
            ImageIO.write(exportRGB(volume), FilenameUtils.getExtension(fn), new File(fn));
        }
    }
}
