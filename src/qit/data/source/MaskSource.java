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

import qit.base.Global;
import qit.base.cli.CliUtils;
import qit.base.structs.Integers;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.modules.volume.VolumeMagnitude;
import qit.math.structs.Box;
import qit.math.utils.MathUtils;

import java.io.IOException;

/**
 * utilties for creating masks
 */
public class MaskSource
{
    public static int DEFAULT_RADIUS = 1;
    public static String DEFAULT_ELEMENT = "cross";

    public static Mask element(String name)
    {
        String lower = name.toLowerCase();

        int radius = Integer.valueOf(CliUtils.args(lower, "1"));
        Mask out = null;
        if (lower.contains("cross"))
        {
            out = MaskSource.cross(radius);
        }
        else if (lower.contains("cube"))
        {
            out = MaskSource.cube(radius);
        }
        else if (lower.contains("sphere"))
        {
            out = MaskSource.sphere(radius);
        }

        Global.assume(out != null, "unknown element: " + name);

        return out;
    }

    public static Mask cube(int r)
    {
        int n = 1 + 2 * r;
        Mask element = new Mask(SamplingSource.create(n, n, n));
        element.setAll(1);
        return element;
    }

    public static Mask cross(int r)
    {
        int n = 1 + 2 * r;
        Mask element = new Mask(SamplingSource.create(n, n, n));
        for (int i = 0; i < n; i++)
        {
            element.set(i, r, r, 1);
            element.set(r, i, r, 1);
            element.set(r, r, i, 1);
        }
        return element;
    }

    public static Mask sphere(int r)
    {
        int n = 1 + 2 * r;
        Mask element = new Mask(SamplingSource.create(n, n, n));

        int r2 = r * r;
        for (Sample sample : element.getSampling())
        {
            int di = sample.getI() - r;
            int dj = sample.getJ() - r;
            int dk = sample.getK() - r;
            int d2 = di * di + dj * dj + dk * dk;

            if (d2 <= r2)
            {
                element.set(sample, 1);
            }
        }
        return element;
    }

    public static Mask discretize(Volume volume)
    {
        if (volume.getDim() > 0)
        {
            volume = VolumeMagnitude.apply(volume);
        }


        return round(volume);
    }

    public static Mask round(Volume volume)
    {
        Global.assume(volume.getDim() == 1, "expected a scalar volume");

        Mask out = new Mask(volume.getSampling());
        for (Sample sample : volume.getSampling())
        {
            out.set(sample, (int) Math.round(volume.get(sample, 0)));
        }

        return out;
    }

    public static Mask create(Sampling sampling, String spec) throws IOException
    {
        Mask mask = new Mask(sampling);

        if (spec != null)
        {
            if (PathUtils.exists(spec))
            {
                mask = Mask.read(spec);
            }
            else
            {
                Sampling range = sampling.range(spec);
                for (Sample r : range)
                {
                    Sample sample = sampling.nearest(range.world(r));
                    mask.set(sample, 1);
                }
            }
        }

        return mask;
    }

    public static Mask create(Box box, double delta)
    {
        int nx = (int) Math.ceil(box.getInterval(0).size() / delta);
        int ny = (int) Math.ceil(box.getInterval(1).size() / delta);
        int nz = (int) Math.ceil(box.getInterval(2).size() / delta);

        return create(box, nx, ny, nz);
    }

    public static Mask create(Box box, int nx, int ny, int nz)
    {
        double sx = box.range(0).getMin();
        double sy = box.range(1).getMin();
        double sz = box.dim() < 3 ? 0.0 : box.range(2).getMin();
        double dx = (box.range(0).getMax() - sx) / nx;
        double dy = (box.range(1).getMax() - sy) / ny;
        double dz = box.dim() < 3 ? 1.0 : (box.range(2).getMax() - sz) / nz;

        Vect start = VectSource.create3D(sx, sy, sz);
        Vect delta = VectSource.create3D(dx, dy, dz);
        Integers nums = new Integers(nx, ny, nz);

        Sampling samp = new Sampling(start, delta, nums);
        Mask out = new Mask(samp);

        return out;
    }

    public static Mask create(int nx, int ny, int nz)
    {
        return new Mask(SamplingSource.create(nx, ny, nz));
    }

    public static Mask cube()
    {
        return cube(1);
    }

    public static Mask cross()
    {
        return cross(1);
    }

    public static Mask create(Sampling sampling)
    {
        return new Mask(sampling);
    }

    public static Mask create(Volume volume)
    {
       Mask out = new Mask(volume.getSampling());

       for (Sample sample : volume.getSampling())
       {
           out.set(sample, MathUtils.round(volume.get(sample, 0)));
       }

       return out;
    }

    public static Mask create(Sampling sampling, int label)
    {
        Mask out = new Mask(sampling);
        out.setAll(label);
        return out;
    }

    public static Mask mask(Sampling sampling, Box box)
    {
        Mask mask = new Mask(sampling);
        Sample min = sampling.nearest(box.getMin());
        Sample max = sampling.nearest(box.getMax());

        for (int i = min.getI(); i <= max.getI(); i++)
        {
            for (int j = min.getJ(); j < max.getJ(); j++)
            {
                for (int k = min.getK(); k < max.getK(); k++)
                {
                    mask.set(new Sample(i, j, k), 1);
                }
            }
        }

        return mask;
    }

    public static Mask mask(Volume volume)
    {
        Mask out = new Mask(volume.getSampling());

        for (Sample sample : volume.getSampling())
        {
            out.set(sample, (int) Math.round(volume.get(sample, 0)));
        }

        return out;
    }

    public static Mask mask(Volume volume, int dim)
    {
        Mask out = new Mask(volume.getSampling());

        for (Sample sample : volume.getSampling())
        {
            out.set(sample, (int) Math.round(volume.get(sample, dim)));
        }

        return out;
    }
}
