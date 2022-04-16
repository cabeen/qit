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
import qit.base.structs.DataType;
import qit.base.structs.Integers;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.Box;

/**
 * utilties for creating volumes
 */
public class VolumeSource
{

    public static Volume create(Box box, int nx, int ny, int nz, int dim)
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
        Volume out = new Volume(samp, Global.getDataType(), dim);

        return out;
    }

    public static Volume create(Matrix[] matrices)
    {
        int nx = matrices[0].cols();
        int ny = matrices[0].rows();
        int nz = 1;
        int dim = matrices.length;
        Volume volume = create(nx, ny, nz, dim);
        for (int d = 0; d < dim; d++)
        {
            for (int j = 0; j < ny; j++)
            {
                for (int i = 0; i < nx; i++)
                {
                    volume.set(i, j, 0, d, matrices[d].get(i, j));
                }
            }
        }
        return volume;
    }

    public static Volume create(Matrix matrix)
    {
        int nx = matrix.cols();
        int ny = matrix.rows();
        int nz = 1;
        int dim = 1;
        Volume volume = create(nx, ny, nz, dim);
        for (int j = 0; j < ny; j++)
        {
            for (int i = 0; i < nx; i++)
            {
                volume.set(i, j, 0, 0, matrix.get(i, j));
            }
        }
        return volume;
    }

    public static Volume create(Sampling sampling, int dim)
    {
        return new Volume(sampling, Global.getDataType(), dim);
    }

    public static Volume create(Sampling sampling)
    {
        return new Volume(sampling, Global.getDataType(), 1);
    }

    public static Volume create(int nx, int ny, int nz, int dim)
    {
        return new Volume(SamplingSource.create(nx, ny, nz), Global.getDataType(), dim);
    }

    public static Volume create(int nx, int ny, int nz)
    {
        return new Volume(SamplingSource.create(nx, ny, nz), Global.getDataType(), 1);
    }

    public static Volume create(Integers nums)
    {
        if (nums.size() == 3)
        {
            return create(nums.getI(), nums.getJ(), nums.getK());
        }
        else if (nums.size() == 4)
        {
            return create(nums.getI(), nums.getJ(), nums.getK(), nums.getL());
        }
        else
        {
            throw new RuntimeException("invalid integers: " + nums.toString());
        }
    }

    public static Volume cube(int r)
    {
        int n = 1 + 2 * r;
        Volume element = new Volume(SamplingSource.create(n, n, n), DataType.DOUBLE, 1);
        element.setAll(VectSource.create1D(1.0));
        return element;
    }

    public static Volume cross(int r)
    {
        int n = 1 + 2 * r;
        Volume element = new Volume(SamplingSource.create(n, n, n), DataType.DOUBLE, 1);
        for (int i = 0; i < n; i++)
        {
            element.set(i, r, r, 0, 1);
            element.set(r, i, r, 0, 1);
            element.set(r, r, i, 0, 1);
        }
        return element;
    }

    public static Volume sphere(int r)
    {
        int n = 1 + 2 * r;
        Volume element = new Volume(SamplingSource.create(n, n, n), DataType.DOUBLE, 1);

        int r2 = r * r;
        for (Sample sample : element.getSampling())
        {
            int di = sample.getI() - r;
            int dj = sample.getJ() - r;
            int dk = sample.getK() - r;
            int d2 = di * di + dj * dj + dk * dk;

            if (d2 <= r2)
            {
                element.set(sample, 0, 1);
            }
        }
        return element;
    }

    public static Volume cube()
    {
        return cube(1);
    }

    public static Volume cross()
    {
        return cross(1);
    }

    public static Volume triangle(int n)
    {
        return triangle(n, n, n);
    }

    public static Volume triangle(int n, int m, int l)
    {
        Global.assume(n > 0 && n % 2 != 0, "triangle size must be positive and odd");
        Global.assume(m > 0 && m % 2 != 0, "triangle size must be positive and odd");
        Global.assume(l > 0 && l % 2 != 0, "triangle size must be positive and odd");

        Volume filter = new Volume(SamplingSource.create(n, m, l), DataType.DOUBLE, 1);

        int ci = (n - 1) / 2;
        int cj = (m - 1) / 2;
        int ck = (l - 1) / 2;

        double sum = 0;
        for (Sample sample : filter.getSampling())
        {
            double x = Math.abs(sample.getI() - ci) / (double) (ci + 1);
            double y = Math.abs(sample.getJ() - cj) / (double) (cj + 1);
            double z = Math.abs(sample.getK() - ck) / (double) (ck + 1);

            double value = (1.0 - x) * (1.0 - y) * (1.0 - z);

            filter.set(sample, 0, value);
            sum += value;
        }

        for (Sample sample : filter.getSampling())
        {
            filter.set(sample, 0, filter.get(sample, 0) / sum);
        }

        return filter;
    }

    public static Volume diffCentralX(Sampling ref)
    {
        double h = ref.deltaI();
        double f = 1.0 / (2.0 * h);

        Volume filter = VolumeSource.create(3, 1, 1);
        filter.set(0, 0, 0, f);
        filter.set(1, 0, 0, 0);
        filter.set(2, 0, 0, -f);

        return filter;
    }

    public static Volume diffCentralY(Sampling ref)
    {
        double h = ref.deltaJ();
        double f = 1.0 / (2.0 * h);

        Volume filter = VolumeSource.create(1, 3, 1);
        filter.set(0, 0, 0, f);
        filter.set(0, 1, 0, 0);
        filter.set(0, 2, 0, -f);

        return filter;
    }

    public static Volume diffCentralZ(Sampling ref)
    {
        double h = ref.deltaK();
        double f = 1.0 / (2.0 * h);

        Volume filter = VolumeSource.create(1, 1, 3);
        filter.set(0, 0, 0, f);
        filter.set(0, 0, 1, 0);
        filter.set(0, 0, 2, -f);

        return filter;
    }

    public static Volume laplacian(Sampling ref, int n, int m, int l, double sigma, double amp)
    {
        Global.assume(n > 0 && n % 2 != 0, "filter size not positive and odd");
        Global.assume(m > 0 && m % 2 != 0, "filter size not positive and odd");
        Global.assume(l > 0 && l % 2 != 0, "filter size not positive and odd");
        Global.assume(sigma > 0, "variance not positive");

        Vect delta = ref.delta();
        double dx = delta.getX();
        double dy = delta.getY();
        double dz = delta.getZ();

        Volume filter = new Volume(SamplingSource.create(n, m, l), DataType.DOUBLE, 1);

        double s2 = sigma * sigma;
        double a2 = amp * amp ;
        int ci = (n - 1) / 2;
        int cj = (m - 1) / 2;
        int ck = (l - 1) / 2;

        for (Sample sample : filter.getSampling())
        {
            double x = (sample.getI() - ci) * dx;
            double y = (sample.getJ() - cj) * dy;
            double z = (sample.getK() - ck) * dz;
            double d2 = x * x + y * y + z * z;

            double value = Math.exp(-0.5 * d2 / s2) * ((d2 / s2) - 1.0) / a2;

            filter.set(sample, 0, value);
        }

        return filter;
    }

    public static Volume gauss(Sampling ref, int n, int m, int l, double sigma)
    {
        Global.assume(n > 0 && n % 2 != 0, "filter size not positive and odd");
        Global.assume(m > 0 && m % 2 != 0, "filter size not positive and odd");
        Global.assume(l > 0 && l % 2 != 0, "filter size not positive and odd");
        Global.assume(sigma > 0, "variance not positive");

        Vect delta = ref.delta();
        double dx = delta.getX();
        double dy = delta.getY();
        double dz = delta.getZ();

        Volume filter = new Volume(SamplingSource.create(n, m, l), DataType.DOUBLE, 1);

        double s2 = sigma * sigma;
        int ci = (n - 1) / 2;
        int cj = (m - 1) / 2;
        int ck = (l - 1) / 2;

        double sum = 0;
        for (Sample sample : filter.getSampling())
        {
            double x = (sample.getI() - ci) * dx;
            double y = (sample.getJ() - cj) * dy;
            double z = (sample.getK() - ck) * dz;

            double value = Math.exp(-(x * x + y * y + z * z) / s2);
            sum += value;
            filter.set(sample, 0, value);
        }

        filter = new VolumeFunction(new Affine(1).times(1 / sum)).withInput(filter).withMessages(false).run();

        return filter;
    }

    public static Volume boxcar(int nx, int ny, int nz)
    {
        int n = nx + ny + nz;

        Volume filter = new Volume(SamplingSource.create(nx, ny, nz), DataType.DOUBLE, 1);
        filter.setAll(VectSource.create1D(1.0 / n));

        return filter;
    }

    public static Volume dx(Sampling sampling)
    {
        Volume filter = new Volume(SamplingSource.create(3, 1, 1), DataType.DOUBLE, 1);
        double f = 1.0 / (2 * sampling.deltaI());
        filter.set(0, 0, 0, 0, -f);
        filter.set(2, 0, 0, 0, f);

        return filter;
    }

    public static Volume dy(Sampling sampling)
    {
        Volume filter = new Volume(SamplingSource.create(1, 3, 1), DataType.DOUBLE, 1);
        double f = 1.0 / (2 * sampling.deltaJ());
        filter.set(0, 0, 0, 0, -f);
        filter.set(0, 2, 0, 0, f);

        return filter;
    }

    public static Volume dz(Sampling sampling)
    {
        Volume filter = new Volume(SamplingSource.create(1, 1, 3), DataType.DOUBLE, 1);
        double f = 1.0 / (2 * sampling.deltaJ());
        filter.set(0, 0, 0, 0, -f);
        filter.set(0, 0, 2, 0, f);

        return filter;
    }

    public static Volume d2x(Sampling sampling)
    {
        Volume filter = new Volume(SamplingSource.create(3, 1, 1), DataType.DOUBLE, 1);
        double f = 1.0 / (sampling.deltaI() * sampling.deltaI());
        filter.set(0, 0, 0, 0, f);
        filter.set(1, 0, 0, 0, -2 * f);
        filter.set(2, 0, 0, 0, f);

        return filter;
    }


    public static Volume d2y(Sampling sampling)
    {
        Volume filter = new Volume(SamplingSource.create(1, 3, 1), DataType.DOUBLE, 1);
        double f = 1.0 / (sampling.deltaJ() * sampling.deltaJ());
        filter.set(0, 0, 0, 0, f);
        filter.set(0, 1, 0, 0, -2 * f);
        filter.set(0, 2, 0, 0, f);

        return filter;
    }


    public static Volume d2z(Sampling sampling)
    {
        Volume filter = new Volume(SamplingSource.create(1, 1, 3), DataType.DOUBLE, 1);
        double f = 1.0 / (sampling.deltaK() * sampling.deltaK());
        filter.set(0, 0, 0, 0, f);
        filter.set(0, 0, 1, 0, -2 * f);
        filter.set(0, 0, 2, 0, f);

        return filter;
    }

    public static Volume sobel(Sampling ref, int edgeDim)
    {
        Global.assume(edgeDim >= 0 && edgeDim <= 2, "invalid filter edge dimension");

        if (!ref.planar())
        {
            Volume filter = VolumeSource.create(3, 3, 3);

            int aDim = edgeDim == 0 ? 1 : edgeDim == 1 ? 0 : 0;
            int bDim = edgeDim == 0 ? 2 : edgeDim == 1 ? 2 : 1;

            double[][] f = {{1, 4, 1}, {3, 6, 3}, {1, 3, 1}};
            int[] sidx = new int[3];

            for (int a = 0; a < 3; a++)
            {
                for (int b = 0; b < 3; b++)
                {
                    double v = f[a][b];
                    sidx[aDim] = a;
                    sidx[bDim] = b;

                    sidx[edgeDim] = 0;
                    filter.set(new Sample(sidx), 0, v);

                    sidx[edgeDim] = 2;
                    filter.set(new Sample(sidx), 0, -v);
                }
            }

            return filter;
        }
        else
        {
            int ni = ref.numI() == 1 ? 1 : 3;
            int nj = ref.numJ() == 1 ? 1 : 3;
            int nk = ref.numK() == 1 ? 1 : 3;

            Volume filter = VolumeSource.create(ni, nj, nk);
            Sampling fref = filter.getSampling();

            if (fref.num(edgeDim) == 1)
            {
                return filter;
            }

            int aDim = edgeDim == 0 ? 1 : edgeDim == 1 ? 0 : 0;
            int bDim = edgeDim == 0 ? 2 : edgeDim == 1 ? 2 : 1;

            double[][] f = {{1, 2, 1}, {1, 2, 1}, {1, 2, 1}};
            int[] sidx = new int[3];

            for (int a = 0; a < 3; a++)
            {
                for (int b = 0; b < 3; b++)
                {
                    double v = f[a][b];
                    sidx[aDim] = a;
                    sidx[bDim] = b;

                    sidx[edgeDim] = 0;
                    if (fref.contains(new Sample(sidx)))
                    {
                        filter.set(new Sample(sidx), 0, v);
                    }

                    sidx[edgeDim] = 2;
                    if (fref.contains(new Sample(sidx)))
                    {
                        filter.set(new Sample(sidx), 0, -v);
                    }
                }
            }

            return filter;
        }
    }

    public static Volume sobel2DX()
    {
        Volume filter = new Volume(SamplingSource.create(3, 3, 1), DataType.DOUBLE, 1);
        filter.set(0, 0, 0, 0, -1);
        filter.set(0, 1, 0, 0, -2);
        filter.set(0, 2, 0, 0, -1);
        filter.set(2, 0, 0, 0, 1);
        filter.set(2, 1, 0, 0, 2);
        filter.set(2, 2, 0, 0, 1);
        return filter;
    }

    public static Volume sobel2DY()
    {
        Volume filter = new Volume(SamplingSource.create(3, 3, 1), DataType.DOUBLE, 1);
        filter.set(0, 0, 0, 0, -1);
        filter.set(1, 0, 0, 0, -2);
        filter.set(2, 0, 0, 0, -1);
        filter.set(0, 2, 0, 0, 1);
        filter.set(1, 2, 0, 0, 2);
        filter.set(2, 2, 0, 0, 1);
        return filter;
    }
}