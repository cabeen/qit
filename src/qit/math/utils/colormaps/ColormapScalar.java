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


package qit.math.utils.colormaps;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.JsonDataset;
import qit.base.structs.Pair;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;
import qit.math.source.VectFunctionSource;
import qit.math.utils.MathUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

public class ColormapScalar extends JsonDataset
{
    private transient VectFunction function = null;

    private String name = "default";
    private double min = 0;
    private double max = 1;
    private List<Pair<Double, Double>> transfer = null;
    private String coloring = ColormapSource.GRAYSCALE;

    public ColormapScalar()
    {
        this.update();
    }

    public ColormapScalar withName(String v)
    {
        this.name = v;
        return this;
    }

    public ColormapScalar withColoring(String v)
    {
        this.coloring = v;
        return this.update();
    }

    public ColormapScalar withMin(double v)
    {
        this.min = v;
        return this.update();
    }

    public ColormapScalar withMax(double v)
    {
        this.max = v;
        return this.update();
    }

    public ColormapScalar withTransfer(List<Pair<Double, Double>> pairs)
    {
        Global.assume(pairs.size() > 1, "invalid pairs");
        for (Pair<Double, Double> pair : pairs)
        {
            Global.assume(pair.a >= 0.0 && pair.b <= 1.0, "invalid pair");
        }

        this.transfer.clear();
        this.transfer.addAll(pairs);

        return this.update();
    }

    public String getName()
    {
        return this.name;
    }

    public String getColoring()
    {
        return this.coloring;
    }

    public double getMin()
    {
        return this.min;
    }

    public double getMax()
    {
        return this.max;
    }

    public List<Pair<Double, Double>> getPoints()
    {
        return this.transfer;
    }

    public VectFunction getFunction()
    {
        return this.function;
    }

    public VectFunction getColoringFunction()
    {
        return ColormapSource.getScalarFunction(this.coloring);
    }

    private ColormapScalar update()
    {
        if (this.transfer == null)
        {
            this.transfer = Lists.newArrayList();
            this.transfer.add(Pair.of(0.0, 0.0));
            this.transfer.add(Pair.of(1.0, 1.0));
        }

        VectFunction rampf = VectFunctionSource.ramp(this.min, 0, this.max, 1);
        VectFunction transferf = VectFunctionSource.linearInterp(this.transfer);
        VectFunction coloringf = ColormapSource.getScalarFunction(this.coloring);

        Global.assume(coloringf != null, "invalid colormap: " + this.coloring);
        this.function = rampf.compose(transferf.compose(coloringf));

        return this;
    }

    public String toString()
    {
        return this.name;
    }

    public BufferedImage colorbar()
    {
        int width = 500;
        int height = 750;
        Color color = Color.BLACK;
        int point = 34;
        int stroke = 6;
        int halfstroke = 3;

        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        double min = this.getMin();
        double max = this.getMax();
        double delta = max - min;
        VectFunction function = this.getFunction();

        int bwidth = MathUtils.round(0.05 * width);
        int bheight = MathUtils.round(0.05 * height);
        int cwidth = MathUtils.round(0.25 * width);
        int cheight = MathUtils.round(0.90 * height);

        for (int i = 0; i < cwidth; i++)
        {
            for (int j = 0; j < cheight; j++)
            {
                double unit = 1.0 - j / (double) (cheight - 1);
                double value = unit * delta + min;
                Vect vect = function.apply(VectSource.create1D(value));
                float vr = (float) vect.get(0);
                float vg = (float) vect.get(1);
                float vb = (float) vect.get(2);

                bi.setRGB(bwidth + i, bheight + j, new Color(vr, vg, vb).getRGB());
            }
        }

        Graphics2D g2 = bi.createGraphics();

        int majorNum = 2;
        int minorNum = 5;
        int majorLen = bwidth * 2;
        int minorLen = bwidth;

        int x = bwidth + cwidth + stroke / 2;

        for (int i = 0; i < majorNum; i++)
        {
            double unit = (double) i / (majorNum - 1);
            int ymaj = MathUtils.round(bheight + cheight * unit);

            if (i < majorNum - 1)
            {
                int ymajn = MathUtils.round(bheight + cheight * (double) (i + 1) / (majorNum - 1));
                int yd = MathUtils.round((ymajn - ymaj) / (double) (minorNum + 1));

                for (int j = 0; j < minorNum; j++)
                {
                    int ymin = ymaj + yd + j * yd;

                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(stroke));
                    g2.drawLine(x, ymin, x + minorLen, ymin);
                }
            }

            if ((ymaj + halfstroke) > (bheight + cheight))
            {
                ymaj -= halfstroke;
            }
            else if ((ymaj - halfstroke) < bheight)
            {
                ymaj += halfstroke;
            }

            g2.setColor(color);
            g2.setStroke(new BasicStroke(stroke));
            g2.drawLine(x, ymaj, x + majorLen, ymaj);

            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(new Font("Sans", Font.PLAIN, point));
            g2.drawString(new Double(min + (1.0 - unit) * delta).toString(), x + (int) (majorLen * 1.5), ymaj + (int) (point / 2.5));
        }

        return bi;
    }

    public ColormapScalar set(ColormapScalar c)
    {
        this.withName(c.getName());
        this.withColoring(c.getColoring());
        this.withMin(c.getMin());
        this.withMax(c.getMax());
        this.withTransfer(c.getPoints());

        return this.update();
    }

    @Override
    public ColormapScalar copy()
    {
        return new ColormapScalar().set(this);
    }

    public static ColormapScalar read(String fn) throws IOException
    {
        return JsonDataset.read(ColormapScalar.class, fn);
    }
}
