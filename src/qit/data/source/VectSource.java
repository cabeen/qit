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

package qit.data.source;

import org.apache.commons.lang3.StringUtils;
import qit.base.Global;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.math.structs.Box;
import qit.math.structs.Interval;

import java.awt.Color;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.util.List;

/** utilties for creating single vectors */
public class VectSource
{
    public static Vect create(Integers is)
    {
        Vect out = new Vect(is.size());
        for (int i = 0; i < out.size(); i++)
        {
            out.set(i, is.get(i));
        }

        return out;
    }

    public static Vect create3(Color color)
    {
        Vect out = new Vect(3);
        out.set(0, color.getRed() / (double) 255);
        out.set(1, color.getGreen() / (double) 255);
        out.set(2, color.getBlue() / (double) 255);

        return out;
    }

    public static Vect create4(Color color, double alpha)
    {
        Vect out = new Vect(3);
        out.set(0, color.getRed() / (double) 255);
        out.set(1, color.getGreen() / (double) 255);
        out.set(2, color.getBlue() / (double) 255);
        out.set(3, alpha);

        return out;
    }

    public static Vect create4(Color color)
    {
        return create4(color, 1.0);
    }

    public static Vect parse(String[] v)
    {
        Vect out = VectSource.createND(v.length);

        for (int i = 0; i < v.length; i++)
        {
            out.set(i, Double.parseDouble(v[i]));
        }

        return out;
    }

    public static Vect parse(String v)
    {
        if (PathUtils.exists(v))
        {
            try
            {
                return Vect.read(v);
            }
            catch (IOException e)
            {
                throw new RuntimeException("failed to read vect: " + v);
            }
        }
        else if (v.contains(","))
        {
            String[] tokens = v.trim().split(",");
            Vect out = new Vect(tokens.length);
            for (int i = 0; i < tokens.length; i++)
            {
                out.set(i, Double.parseDouble(tokens[i]));
            }

            return out;
        }
        else if (v.contains(" "))
        {
            String[] tokens = v.trim().split(" ");
            Vect out = new Vect(tokens.length);
            for (int i = 0; i < tokens.length; i++)
            {
                out.set(i, Double.parseDouble(tokens[i]));
            }

            return out;
        }
        else
        {
            return VectSource.create1D(Double.parseDouble(v));
        }
    }

    public static Vect linspace(double a, double b, int num)
    {
        if (a >= b)
        {
            throw new IllegalArgumentException("The starting value must be less than the ending value.");
        }

        Vect output = new Vect(num);
        double d = b - a;
        for (int i = 0; i < num - 1; i++)
        {
            output.set(i, a + d * i / (num - 1));
        }

        output.set(num - 1, b);

        return output;
    }

    public static Vect seq(int start, int step, int end)
    {
        int num = (int) Math.floor((end - start) / step) + 1;
        Global.assume(num > 0, String.format("invalid sequence: start=%d, step=%d, end=%d", start, step, end));

        Vect out = new Vect(num);

        for (int i = 0; i < num; i++)
        {
            out.set(i, start + step * i);
        }

        return out;
    }

    public static Vect seq(double start, double step, double end)
    {
        int num = (int) Math.floor((end - start) / step) + 1;
        Global.assume(num > 0, String.format("invalid sequence: start=%g, step=%g, end=%g", start, step, end));

        Vect out = new Vect(num);

        for (int i = 0; i < num; i++)
        {
            out.set(i, start + step * i);
        }

        return out;
    }

    public static Vect cat(Vect a, Vect b)
    {
        int dima = a.size();
        int dim = dima + b.size();
        Vect out = new Vect(dim);
        for (int i = 0; i < a.size(); i++)
        {
            out.set(i, a.get(i));
        }
        for (int i = 0; i < b.size(); i++)
        {
            out.set(dima + i, b.get(i));
        }

        return out;
    }

    public static Vect create(String v, String d)
    {
        String[] tokens = StringUtils.split(v, d);
        Vect out = new Vect(tokens.length);
        for (int i = 0; i < tokens.length; i++)
        {
            out.set(i, Double.valueOf(tokens[i]));
        }

        return out;
    }

    public static Vect create(String v)
    {
        String[] tokens = StringUtils.split(v);
        Vect out = new Vect(tokens.length);
        for (int i = 0; i < tokens.length; i++)
        {
            out.set(i, Double.valueOf(tokens[i]));
        }

        return out;
    }

    public static Vect create(double x)
    {
        return create1D(x);
    }

    public static Vect create(double x, double y)
    {
        return create2D(x, y);
    }

    public static Vect create(double x, double y, double z)
    {
        return create3D(x, y, z);
    }

    public static Vect create(double x, double y, double z, double w)
    {
        return create4D(x, y, z, w);
    }

    public static Vect create1D()
    {
        return create1D(0);
    }

    public static Vect create2D()
    {
        return create2D(0, 0);
    }

    public static Vect create3D()
    {
        return create3D(0, 0, 0);
    }

    public static Vect create4D()
    {
        return create4D(0, 0, 0, 0);
    }

    public static Vect create1D(String value)
    {
        Vect vect = new Vect(1);
        vect.set(0, Double.valueOf(value));
        return vect;
    }

    public static Vect create1D(double value)
    {
        Vect vect = new Vect(1);
        vect.set(0, value);
        return vect;
    }

    public static Vect create2D(double x, double y)
    {
        Vect vect = new Vect(2);
        vect.set(0, x);
        vect.set(1, y);
        return vect;
    }

    public static Vect create3D(double x, double y, double z)
    {
        Vect vect = new Vect(3);
        vect.set(0, x);
        vect.set(1, y);
        vect.set(2, z);

        return vect;
    }

    public static Vect create4D(double x, double y, double z, double w)
    {
        Vect vect = new Vect(4);
        vect.set(0, x);
        vect.set(1, y);
        vect.set(2, z);
        vect.set(3, w);

        return vect;
    }

    public static Vect createND(int dim, double value)
    {
        Vect vect = new Vect(dim);
        for (int i = 0; i < dim; i++)
        {
            vect.set(i, value);
        }
        return vect;
    }

    public static Vect createX()
    {
        return create3D(1, 0, 0);
    }

    public static Vect createY()
    {
        return create3D(0, 1, 0);
    }

    public static Vect createZ()
    {
        return create3D(0, 0, 1);
    }

    public static Vect create(List<Double> values)
    {
        Vect out = new Vect(values.size());
        for (int i = 0; i < values.size(); i++)
        {
            out.set(i, values.get(i));
        }
        return out;
    }

    public static Vect create(Vects values)
    {
        return values.flatten();
    }

    public static Vect create(double[] data)
    {
        Vect out = new Vect(data.length);
        for (int i = 0; i < data.length; i++)
        {
            out.set(i, data[i]);
        }
        return out;
    }

    public static Vect create(float[] data)
    {
        Vect out = new Vect(data.length);
        for (int i = 0; i < data.length; i++)
        {
            out.set(i, data[i]);
        }
        return out;
    }

    public static Vect create(int[] data)
    {
        Vect out = new Vect(data.length);
        for (int i = 0; i < data.length; i++)
        {
            out.set(i, data[i]);
        }
        return out;
    }

    public static Vect create(DataBuffer data)
    {
        int banks = data.getNumBanks();
        int size = data.getSize();

        Vect out = new Vect(banks * size);
        for (int i = 0; i < banks; i++)
        {
            for (int j = 0; j < size; j++)
            {
                out.set(size * i + j, data.getElemDouble(i, j));
            }
        }

        return out;
    }


    public static Vect uniform(Box box)
    {
        Vect v = new Vect(box.dim());
        for (int i = 0; i < box.dim(); i++)
        {
            Interval r = box.range(i);
            double delta = r.getMax() - r.getMin();
            double t = Global.RANDOM.nextDouble();
            v.set(i, r.getMin() + delta * t);
        }
        return v;
    }

    public static Vect createND(int dim)
    {
        return createND(dim, 0.0);
    }

    public static Vect randomUnit()
    {
        return random(3).normalize();
    }

    public static Vect random(int dim)
    {
        Vect vect = new Vect(dim);
        for (int i = 0; i < dim; i++)
        {
            vect.set(i, 2 * (Global.RANDOM.nextDouble() - 0.5));
        }

        return vect;
    }

    public static Vect random(double scale, int dim)
    {
        Vect vect = new Vect(dim);
        for (int i = 0; i < dim; i++)
        {
            vect.set(i, scale * Global.RANDOM.nextDouble());
        }

        return vect;
    }

    public static Vect nonZeroRandom(int dim)
    {
        Vect a = random(3);

        // Make sure a is non-zero
        while (a.get(0) == 0 && a.get(0) == 0 && a.get(0) == 0)
        {
            a = random(3);
        }

        return a;
    }

    public static Vect ones(int n)
    {
        Vect out = new Vect(n);
        for (int i = 0; i < n; i++)
        {
            out.set(i, 1.0);
        }
        return out;
    }

    public static Vect gaussian(int dim)
    {
        Vect vect = new Vect(dim);
        for (int i = 0; i < dim; i++)
        {
            vect.set(i, Global.RANDOM.nextGaussian());
        }

        return vect;
    }

    public static Vect poly(double v, int order)
    {
        Vect out = new Vect(order + 1);

        out.set(0, 1);
        for (int i = 1; i <= order; i++)
        {
            out.set(i, out.get(i - 1) * v);
        }

        return out;
    }
}