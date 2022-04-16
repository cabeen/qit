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

import qit.base.Logging;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;

public class PhantomSource
{
    public static Volume phantomBrain(int slices)
    {
        int size = 30;

        Sampling sampling = SamplingSource.create(size, size, slices);
        Volume out = VolumeSource.create(sampling, new Fibers(3).getEncodingSize());

        int x0_cc = 0;
        int y0_cc = 0;
        int t_cc = 5;
        int r_cc = 10;
        int r2_cc = r_cc * r_cc;

        int x0_cing = 1;
        int y0_cing = 5;
        int t_cing = r_cc - y0_cing;

        int x0_cst = x0_cc + r_cc;
        int t_cst = 2 * t_cc;

        int x0_slf = x0_cst + t_cst - 1;
        int y0_slf = y0_cc + r_cc;

        for (Sample sample : sampling)
        {
            int x = sample.get(0);
            int y = sample.get(1);

            int dx_cc = x - x0_cc;
            int dy_cc = y - y0_cc;
            int s2_cc = dx_cc * dx_cc / r2_cc + dy_cc * dy_cc / r2_cc;
            int b_cc = y0_cc + r_cc + t_cc;

            boolean cing = x > x0_cing && x <= x0_cing + t_cing;
            cing &= y > y0_cing && y <= y0_cing + t_cing;

            boolean cc_bot = y >= b_cc && y < b_cc + t_cc;
            boolean cc_top = !cc_bot && s2_cc >= 1.0;
            cc_top &= x < x0_cst + t_cst - 1 && y <= b_cc;

            boolean cst = x >= x0_cst && x < x0_cst + t_cst;
            boolean slf = x >= x0_slf && y >= y0_slf;

            if (cing && cc_bot && cc_top)
            {
                Logging.info("warning: cing and cc are expected to be disjoint");
            }

            if (cc_top || cc_bot || cing || cst || slf)
            {
                Fibers fibers = new Fibers(3);

                fibers.setBaseline(1000);
                fibers.setDiffusivity(1e-3);

                if (cc_top)
                {
                    Vect a = VectSource.create3D(dy_cc, -dx_cc, 0).normalize();
                    Vect b = VectSource.create3D(1, 0, 0);
                    double c = (dy_cc - r_cc) / (double) t_cc;
                    c = Math.max(c, 0);
                    c = Math.min(c, 0.5);

                    Vect dir = a.times(1 - c).plus(b.times(c)).normalize();

                    fibers.setFrac(0, 0.2);
                    fibers.setLine(0, dir);
                }
                else if (cc_bot)
                {
                    fibers.setFrac(0, 0.2);
                    fibers.setLine(0, VectSource.create3D(1, 0, 0));
                }
                else if (cing)
                {
                    fibers.setFrac(0, 0.35);
                    fibers.setLine(0, VectSource.create3D(0, 0, 1));
                }

                if (cst)
                {
                    fibers.setFrac(1, 0.25);
                    fibers.setLine(1, VectSource.create3D(0, 1, 0));
                }

                if (slf)
                {
                    fibers.setFrac(2, 0.35);
                    fibers.setLine(2, VectSource.create3D(0, 0, 1));
                }

                out.set(sample, fibers.getEncoding());
            }

        }

        return out;
    }

    public static Volume phantomCross(int size, double degrees, int slices)
    {
        double radians = Math.PI * degrees / 180;
        double dx = Math.cos(radians);
        double dy = Math.sin(radians);
        double slope = dy / dx;
        double d = 0.25;

        int width = (int) Math.ceil(size * dx);
        int height = (int) Math.ceil(size * dy);

        Fibers fibers = new Fibers(2);

        fibers.setBaseline(1000);
        fibers.setDiffusivity(1e-3);

        Vect a = VectSource.create(dx, dy, 0).normalize();
        Vect b = VectSource.create(dx, -dy, 0).normalize();
        Vect c = VectSource.create(0, 0, 1);

        if (width <= 1 || height <= 1)
        {
            Sampling sampling = SamplingSource.create(1, 1, 1);
            Volume out = VolumeSource.create(sampling, fibers.getEncodingSize());

            fibers.setFrac(0, 0.5);
            fibers.setLine(0, a);

            fibers.setFrac(1, 0.5);
            fibers.setLine(1, b);

            out.set(0, fibers.getEncoding());

            return out;
        }
        else
        {
            Sampling sampling = SamplingSource.create(width, height, slices);
            Volume out = VolumeSource.create(sampling, fibers.getEncodingSize());

            for (Sample sample : sampling)
            {
                double i = sample.getI();
                double j = sample.getJ();

                boolean ra = j > slope * i - d * height && j < slope * i + d * height;
                boolean rb = j < (1.0 + d) * height - slope * i && j > (1.0 - d) * height - slope * i;

                if (ra && rb)
                {
                    fibers.setFrac(0, 0.5);
                    fibers.setLine(0, a);

                    fibers.setFrac(1, 0.5);
                    fibers.setLine(1, b);
                }
                else if (ra && !rb)
                {
                    fibers.setFrac(0, 0.5);
                    fibers.setLine(0, a);

                    fibers.setFrac(1, 0.0);
                    fibers.setLine(1, c);
                }
                else if (!ra && rb)
                {
                    fibers.setFrac(0, 0.5);
                    fibers.setLine(0, b);

                    fibers.setFrac(1, 0.0);
                    fibers.setLine(1, c);
                }
                else
                {
                    fibers.setFrac(0, 0.0);
                    fibers.setLine(0, c);

                    fibers.setFrac(1, 0.0);
                    fibers.setLine(1, c);
                }

                out.set(sample, fibers.getEncoding());
            }

            return out;
        }
    }

    public static Volume phantomFanTee(int nx, int ny, int slices)
    {
        Fibers fibers = new Fibers(1);

        fibers.setBaseline(1000);
        fibers.setDiffusivity(1e-3);

        Sampling sampling = SamplingSource.create(nx, slices, ny);
        Volume out = VolumeSource.create(sampling, fibers.getEncodingSize());

        double fx = 0.55;
        double fy = 1.2;

        for (Sample sample : sampling)
        {
            double x = sample.getI() / (double) nx;
            double y = sample.getK() / (double) ny;

            double bx = x / fx;
            double by = y / fy;

            fibers.setFrac(0, 1.0);
            if (bx * bx + by * by < 1)
            {
                fibers.setLine(0, VectSource.create(0, 4, 1).normalize());
            }
            else
            {
                double vx = -2 * (1 - x) * by;
                double vz = x + 2 * (1 - x) * bx;
                fibers.setLine(0, VectSource.create(vx, 0, vz).normalize());
            }

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }

    public static Volume phantomTee(int nx, int ny, int slices)
    {
        Fibers fibers = new Fibers(1);

        fibers.setBaseline(1000);
        fibers.setDiffusivity(1e-3);

        Sampling sampling = SamplingSource.create(nx, ny, slices);
        Volume out = VolumeSource.create(sampling, fibers.getEncodingSize());

        Vect a = VectSource.create(0, 1, 0).normalize();
        Vect b = VectSource.create(0, 1, 3).normalize();
        double fa = 1.0;
        double fb = 1.0;
        double thresh = sampling.numI() / 2;

        for (Sample sample : sampling)
        {
            if (sample.getI() < thresh)
            {
                fibers.setFrac(0, fa);
                fibers.setLine(0, a);
            }
            else
            {
                fibers.setFrac(0, fb);
                fibers.setLine(0, b);
            }

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }

    public static Volume phantomTriple(int nx, int ny, int slices)
    {
        Fibers fibers = new Fibers(2);

        fibers.setBaseline(1000);
        fibers.setDiffusivity(1e-3);

        Sampling sampling = SamplingSource.create(nx, ny, slices);
        Volume out = VolumeSource.create(sampling, fibers.getEncodingSize());

        Vect a = VectSource.create(3, 1, 0).normalize();
        Vect b = VectSource.create(0, 1, 0).normalize();
        Vect c = VectSource.create(0, 1, 3).normalize();
        double fa = 0.5;
        double fb = 0.5;
        double fc = 0.5;
        double thresh = sampling.numI() / 2;

        for (Sample sample : sampling)
        {
            fibers.setFrac(0, fa);
            fibers.setLine(0, a);

            if (sample.getI() < thresh)
            {
                fibers.setFrac(1, fb);
                fibers.setLine(1, b);
            }
            else
            {
                fibers.setFrac(1, fc);
                fibers.setLine(1, c);
            }

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }

    public static Volume phantomFanTriple(int nx, int ny, int slices)
    {
        Fibers fibers = new Fibers(2);

        fibers.setBaseline(1000);
        fibers.setDiffusivity(1e-3);

        Sampling sampling = SamplingSource.create(nx, slices, ny);
        Volume out = VolumeSource.create(sampling, fibers.getEncodingSize());

        double fx = 0.55;
        double fy = 1.2;

        for (Sample sample : sampling)
        {
            double x = sample.getI() / (double) nx;
            double y = sample.getK() / (double) ny;

            double bx = x / fx;
            double by = y / fy;

            fibers.setFrac(0, 0.5);
            if (bx * bx + by * by < 1)
            {
                fibers.setLine(0, VectSource.create(0, 4, 1).normalize());
            }
            else
            {
                double vx = -2 * (1 - x) * by;
                double vz = x + 2 * (1 - x) * bx;
                fibers.setLine(0, VectSource.create(vx, 0, vz).normalize());
            }

            fibers.setFrac(1, 0.5);
            fibers.setLine(1, VectSource.create(1, 0, 0));

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }

    public static Volume phantomCurve(int nx, int ny, int slices)
    {
        Fibers fibers = new Fibers(1);

        fibers.setBaseline(1000);
        fibers.setDiffusivity(1e-3);

        Sampling sampling = SamplingSource.create(nx, ny, slices);
        Volume out = VolumeSource.create(sampling, fibers.getEncodingSize());

        for (Sample sample : sampling)
        {
            double x = sample.getI() / (double) nx;
            double y = sample.getJ() / (double) ny;

            double vx = y + 0.5;
            double vy = -(x + 0.5);

            fibers.setFrac(0, 1.0);
            fibers.setLine(0, VectSource.create(vx, vy, 0).normalize());

            out.set(sample, fibers.getEncoding());
        }

        return out;
    }
}
