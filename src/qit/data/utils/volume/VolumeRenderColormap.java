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

package qit.data.utils.volume;

import qit.base.Global;
import qit.data.formats.volume.BufferedImageVolumeCoder;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;
import qit.math.utils.colormaps.ColormapScalar;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

public class VolumeRenderColormap
{
    private ColormapScalar colormap;
    private Color foreground = Color.WHITE;
    private Color background = Color.BLACK;
    private int width = 250;
    private int height = 600;
    private int minor = 2;
    private int major = 5;
    private int point = 40;
    private boolean italics = false;
    private boolean bold = false;
    private String font = "Helvetica";
    private String label = "Attribute";
    private Volume output = null;

    public Volume getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }

    private VolumeRenderColormap clear()
    {
        this.output = null;
        return this;
    }

    public ColormapScalar getColormap()
    {
        return colormap;
    }

    public VolumeRenderColormap withColormap(ColormapScalar colormap)
    {
        this.colormap = colormap;
        return this.clear();
    }

    public Color getForeground()
    {
        return foreground;
    }

    public VolumeRenderColormap withForeground(Color color)
    {
        this.foreground = color;
        return this.clear();
    }

    public Color getBackground()
    {
        return background;
    }

    public VolumeRenderColormap withBackground(Color color)
    {
        this.background = color;
        return this.clear();
    }

    public int getWidth()
    {
        return width;
    }

    public VolumeRenderColormap withWidth(int width)
    {
        this.width = width;
        return this.clear();
    }

    public int getHeight()
    {
        return height;
    }

    public VolumeRenderColormap withHeight(int height)
    {
        this.height = height;
        return this.clear();
    }

    public int getMinor()
    {
        return minor;
    }

    public VolumeRenderColormap withMinor(int minor)
    {
        this.minor = minor;
        return this.clear();
    }

    public int getMajor()
    {
        return major;
    }

    public VolumeRenderColormap withMajor(int major)
    {
        this.major = major;
        return this.clear();
    }

    public int getPoint()
    {
        return point;
    }

    public VolumeRenderColormap withPoint(int point)
    {
        this.point = point;
        return this.clear();
    }

    public boolean isItalics()
    {
        return italics;
    }

    public VolumeRenderColormap withItalics(boolean italics)
    {
        this.italics = italics;
        return this.clear();
    }

    public boolean isBold()
    {
        return bold;
    }

    public VolumeRenderColormap withBold(boolean bold)
    {
        this.bold = bold;
        return this.clear();
    }

    public String getFont()
    {
        return font;
    }

    public VolumeRenderColormap withFont(String font)
    {
        this.font = font;
        return this.clear();
    }

    public String getLabel()
    {
        return label;
    }

    public VolumeRenderColormap withLabel(String label)
    {
        this.label = label;
        return this.clear();
    }

    public void run()
    {
        Global.assume(this.colormap != null, "no colormap found");

        BufferedImage img = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setBackground(this.background);
        g.clearRect(0, 0, this.width, this.height);

        int barI = (int) (0.1 * this.width);
        int barJ = barI + this.point / 2;
        int barW = (int) (0.2 * this.width);
        int barH = (this.height - 3 * barJ - this.point);
        barH -= (barH - 1) % (this.major - 1);

        double min = this.colormap.getMin();
        double max = this.colormap.getMax();
        double delta = max - min;
        VectFunction function = this.colormap.getFunction();
        ByteBuffer colors = ByteBuffer.allocateDirect(this.width * this.height * 4);
        for (int i = 0; i < barW; i++)
        {
            for (int j = 0; j < barH; j++)
            {
                double unit = 1 - j / (double) (barH - 1);
                double value = unit * delta + min;
                Vect vect = function.apply(VectSource.create1D(value));
                float vr = (float) vect.get(0);
                float vg = (float) vect.get(1);
                float vb = (float) vect.get(2);
                Color colorv = new Color(vr, vg, vb);
                img.setRGB(barI + i, barJ + j, colorv.getRGB());
            }
        }

        int tickI = barI + barW;
        int tickJ = barJ;
        int majorW = barW / 4;
        int minorW = barW / 8;

        int majorD = (barH - 1) / (this.major - 1);
        int minorD = majorD / (this.minor + 1);

        for (int a = 0; a < this.major; a++)
        {
            for (int i = 0; i < majorW; i++)
            {
                img.setRGB(tickI + i, tickJ + a * majorD, this.foreground.getRGB());
            }

            if (a < this.major - 1)
            {
                for (int b = 1; b <= this.minor; b++)
                {
                    for (int i = 0; i < minorW; i++)
                    {
                        img.setRGB(tickI + i, tickJ + a * majorD + b * minorD, this.foreground.getRGB());
                    }
                }
            }
        }

        int style = Font.PLAIN;
        if (this.bold)
        {
            style |= Font.BOLD;
        }
        if (this.italics)
        {
            style |= Font.ITALIC;
        }

        Font font = new Font(this.font, style, this.point);

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(font);
        g.setColor(this.foreground);

        g.drawString(this.label, barI, 2 * barJ + barH + this.point);

        for (int i = 0; i < this.major; i++)
        {
            double unit = 1.0 - i / (double) (this.major - 1);
            double value = unit * delta + min;
            String text = String.format("%.3g", value);
            int tx = tickI + (int) (0.35 * barW);
            int ty = tickJ + i * majorD + (int) (0.35 * this.point);
            g.drawString(text, tx, ty);
        }

        g.dispose();

        this.output = BufferedImageVolumeCoder.importRGB(img);
    }
}