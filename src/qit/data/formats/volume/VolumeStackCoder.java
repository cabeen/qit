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

import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/** A reader for stacks of images
 * see: http://wiki.cse.ucdavis.edu/keckcaves:imagestacks */
public class VolumeStackCoder
{
    // this business is no good, but since the format doesn't specify these, it'll have to do
    public static String ORIENTATION = "Z";
    public static boolean GRAYSCALE = false; // enable this for large grayscale stacks

    public static boolean matches(String fn)
    {
        return fn.endsWith("stack") || fn.endsWith("shdr");
    }

    public static Volume read(String fn) throws IOException
    {
        Logging.info("reading stack: " + fn);
        StackHeader header = StackHeader.read(fn);

        int numX = header.imageSize[0];
        int numY = header.imageSize[1];
        int numZ = header.numSlices;

        double deltaX = header.sampleSpacing[0];
        double deltaY = header.sampleSpacing[1];
        double deltaZ = header.sampleSpacing[2] * header.sliceIndexFactor;
        // this adjustment seems reasonable, but it's ambiguous in the range

        Vect starts = VectSource.create3D();
        Vect spacings = VectSource.create(header.sampleSpacing[0], header.sampleSpacing[1], header.sampleSpacing[2]);
        Integers nums = new Integers(numX, numY, numZ);
        Sampling sampling = new Sampling(starts, spacings, nums);
        Volume out = VolumeSource.create(sampling, GRAYSCALE ? 1 : 3);

        Logging.info(String.format("reading %d slices", header.numSlices));
        for (int slice = 0; slice < header.numSlices; slice++)
        {
            int id = header.sliceIndexStart + slice * header.sliceIndexFactor;
            String subfn = String.format(header.sliceFileNameTemplate, id);

            Logging.info("reading slice: " + subfn);

            BufferedImage img = ImageIO.read(new File(subfn));
            Global.assume(img != null, "failed to read image: " + subfn);

            for (int i = 0; i < numX; i++)
            {
                for (int j = 0; j < numY; j++)
                {
                    int pi = header.regionOrigin[0] + i;
                    int pj = header.regionOrigin[1] + j;

                    Color c = new Color(img.getRGB(pi, pj));
                    double red = c.getRed() / 255.0;
                    double green = c.getGreen() / 255.0;
                    double blue = c.getBlue() / 255.0;

                    if (GRAYSCALE)
                    {
                        double value = (red + green + blue) / 3.0;
                        out.set(i, j, slice, 0, value);
                    }
                    else
                    {
                        out.set(i, j, slice, VectSource.create3D(red, green, blue));
                    }
                }
            }
        }
        Logging.info("finished reading stack");

        return out;
    }

    public static void write(Volume volume, String fn) throws IOException
    {
        Sampling sampling = volume.getSampling();

        Logging.info("writing stack: " + fn);
        PathUtils.mkpar(fn);

        Logging.info("stack orientation: " + ORIENTATION);
        StackHeader header = new StackHeader();
        header.sliceFileNameTemplate = PathUtils.join(PathUtils.dirname(fn), "%d.png");

        if (Sets.newHashSet(new String[]{"z", "k", "axial", "transverse"}).contains(ORIENTATION.toLowerCase()))
        {
            header.imageSize[0] = sampling.numI();
            header.imageSize[1] = sampling.numJ();
            header.numSlices = sampling.numK();
            header.sampleSpacing[0] = sampling.deltaI();
            header.sampleSpacing[1] = sampling.deltaJ();
            header.sampleSpacing[2] = sampling.deltaK();
            header.write(fn);

            Volume subvolume = VolumeSource.create(sampling.numI(), sampling.numJ(), 1, volume.getDim());
            for (int k = 0; k < sampling.numK(); k++)
            {
                String subfn = String.format(header.sliceFileNameTemplate, k);
                for (int i = 0; i < sampling.numI(); i++)
                {
                    for (int j = 0; j < sampling.numJ(); j++)
                    {
                        subvolume.set(i, j, 0, volume.get(i, j, k));
                    }
                }

                Logging.info("writing slice: " + subfn);
                BufferedImageVolumeCoder.write(subvolume, subfn);
            }
        }
        else if (Sets.newHashSet(new String[]{"y", "j", "sagittal", "median"}).contains(ORIENTATION.toLowerCase()))
        {
            header.imageSize[0] = sampling.numI();
            header.imageSize[1] = sampling.numK();
            header.numSlices = sampling.numJ();
            header.sampleSpacing[0] = sampling.deltaI();
            header.sampleSpacing[1] = sampling.deltaK();
            header.sampleSpacing[2] = sampling.deltaJ();
            header.write(fn);

            Volume subvolume = VolumeSource.create(sampling.numI(), sampling.numK(), 1, volume.getDim());

            for (int j = 0; j < sampling.numJ(); j++)
            {
                String subfn = String.format(header.sliceFileNameTemplate, j);
                for (int i = 0; i < sampling.numI(); i++)
                {
                    for (int k = 0; k < sampling.numK(); k++)
                    {
                        subvolume.set(i, k, 0, volume.get(i, j, k));
                    }
                }

                Logging.info("writing slice: " + subfn);
                BufferedImageVolumeCoder.write(subvolume, subfn);
            }
        }
        else if (Sets.newHashSet(new String[]{"x", "i", "frontal", "coronal"}).contains(ORIENTATION.toLowerCase()))
        {
            header.imageSize[0] = sampling.numJ();
            header.imageSize[1] = sampling.numK();
            header.numSlices = sampling.numI();
            header.sampleSpacing[0] = sampling.deltaJ();
            header.sampleSpacing[1] = sampling.deltaK();
            header.sampleSpacing[2] = sampling.deltaI();
            header.write(fn);

            Volume subvolume = VolumeSource.create(sampling.numJ(), sampling.numK(), 1, volume.getDim());
            for (int i = 0; i < sampling.numI(); i++)
            {
                String subfn = String.format(header.sliceFileNameTemplate, i);

                for (int k = 0; k < sampling.numK(); k++)
                {
                    for (int j = 0; j < sampling.numJ(); j++)
                    {
                        subvolume.set(j, k, 0, volume.get(i, j, k));
                    }
                }

                Logging.info("writing slice: " + subfn);
                BufferedImageVolumeCoder.write(subvolume, subfn);
            }
        }
        else
        {
            Logging.error("invalid orientation: " + ORIENTATION);
        }

        Logging.info("finished writing stack");
    }


    public static class StackHeader
    {
        // n is the number of individual images in the stack.
        public int numSlices = 1; // = n

        // x and y are the width and height, respectively, of a subimage to be
        // extracted from each image in the stack.
        public int[] imageSize = {0, 0}; // = x y

        // x and y are the left and lower corner of a subimage to be extracted
        // from each image in the stack. If not given, these default to 0 0.
        public int[] regionOrigin = {0, 0}; // = x y

        // z is the distance between slice images, or slice thickness, in
        // world units x and y are the horizontal and vertical distances
        // between pixels in a slice, respectively. The actual units used are
        // mostly irrelevant, but they have to be consistent, otherwise the
        // volume will appear stretched. The units are only relevant when using
        // the measurement tool; any positions and distances will be in whatever
        // units are used for the sample spacing.
        public double[] sampleSpacing = {1.0, 1.0, 1.0}; // = x y z

        // i is the number used to generate the file name of the first image
        // slice. If not given, it defaults to 0.
        int sliceIndexStart = 0; // = s

        // f is the increment in numbers between adjacent images. If not given,
        // this defaults to 1 (consecutive numbers), but it can be any other
        // integer to skip slice images when reading.
        int sliceIndexFactor = 1; // = f

        // t is a template for generating slice file names given a slice index.
        // (Include the path to the file.) It cannot contain any spaces. The
        // template is a combination of a base name and a number, and a definition
        // of how to format the number. First, slices are indexed from 0 to n-1
        // (where n is the number of slices). Then, the slice index i is converted
        // into the file number by: number = s + i * f. The name template is a
        // printf-style string; it uses a %d command to insert the number into the
        // template. For example, if the template is Foo%dbar.jpg, then the generated
        // file names are Foo0bar.jpg, Foo1bar.jpg, Foo10bar.jpg, Foo100bar.jpg,
        // ssuming that s=0 and f=1. The %d command can be modified to generate
        // different formats:
        public String sliceFileNameTemplate = "%d.png"; // = t

        // this is undefined in the web range, but an example file seems to use it
        // to define the filename ala $(sliceDirectory)/$(sliceFileNameTemplate)
        public String sliceDirectory = null;

        public static StackHeader read(String fn) throws IOException
        {
            DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(fn)));

            String line = null;

            StackHeader out = new StackHeader();
            while ((line = dis.readLine()) != null)
            {
                String[] tokens = line.split("=");

                if (tokens.length != 2)
                {
                    continue;
                }

                String name = tokens[0].trim().split(" ")[0];
                String[] values = tokens[1].trim().split(" ");

                if (name.equals("numSlices") && values.length == 1)
                {
                    out.numSlices = Integer.valueOf(values[0]);
                }
                else if (name.equals("imageSize") && values.length == 2)
                {
                    out.imageSize[0] = Integer.valueOf(values[0]);
                    out.imageSize[1] = Integer.valueOf(values[1]);
                }
                else if (name.equals("regionOrigin") && values.length == 2)
                {
                    out.regionOrigin[0] = Integer.valueOf(values[0]);
                    out.regionOrigin[1] = Integer.valueOf(values[1]);
                }
                else if (name.equals("sampleSpacing") && values.length == 3)
                {
                    out.sampleSpacing[0] = Double.valueOf(values[0]);
                    out.sampleSpacing[1] = Double.valueOf(values[1]);
                    out.sampleSpacing[2] = Double.valueOf(values[2]);
                }
                else if (name.equals("sliceDirectory") && values.length == 1)
                {
                    out.sliceDirectory = values[0];
                }
                else if (name.equals("sliceFileNameTemplate") && values.length == 1)
                {
                    out.sliceFileNameTemplate = values[0];
                }
                else if (name.equals("sliceIndexStart") && values.length == 1)
                {
                    out.sliceIndexStart = Integer.valueOf(values[0]);
                }
                else if (name.equals("sliceIndexFactor") && values.length == 1)
                {
                    out.sliceIndexFactor = Integer.valueOf(values[0]);
                }
            }

            return out;
        }

        public void write(String fn) throws IOException
        {
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fn)));

            out.writeBytes(String.format("numSlices = %d\n", this.numSlices));
            out.writeBytes(String.format("imageSize = %d %d\n", this.imageSize[0], this.imageSize[1]));
            out.writeBytes(String.format("regionOrigin = %d %d\n", this.regionOrigin[0], this.regionOrigin[1]));
            out.writeBytes(String.format("sampleSpacing = %g %g %g\n", this.sampleSpacing[0], this.sampleSpacing[1], this.sampleSpacing[2]));
            out.writeBytes(String.format("sliceIndexStart = %d\n", this.sliceIndexStart));
            out.writeBytes(String.format("sliceIndexFactor = %d\n", this.sliceIndexFactor));
            out.writeBytes(String.format("sliceFileNameTemplate = %s\n", this.sliceFileNameTemplate));

            if (this.sliceDirectory != null)
            {
                out.writeBytes(String.format("sliceDirectory = %s\n", this.sliceDirectory));
            }

            out.close();
        }
    }

}
