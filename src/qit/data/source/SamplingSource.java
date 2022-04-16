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
import qit.base.structs.Integers;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.math.structs.Box;

/** utiltites for creating volumetric samplings */
public class SamplingSource
{
    public static Sampling create(int nx, int ny, int nz)
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(1, 1, 1);
        Integers num = new Integers(nx, ny, nz);

        return new Sampling(start, delta, num);
    }

    public static Sampling create(Integers num)
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(1, 1, 1);

        return new Sampling(start, delta, num);
    }

    public static Sampling create(Integers num, Vect delta)
    {
        Vect start = VectSource.create3D(0, 0, 0);

        return new Sampling(start, delta, num);
    }

    public static Sampling createIsotropic(double d, int nx, int ny, int nz)
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(d, d, d);
        Integers num = new Integers(nx, ny, nz);

        return new Sampling(start, delta, num);
    }

    public static Sampling create(Box box, double delta)
    {
        return create(box, VectSource.create3D(delta, delta, delta));
    }
    
    public static Sampling create(Box box, Vect delta)
    {
        Global.assume(delta.getX() > 0, "delta x must be positive");
        Global.assume(delta.getY() > 0, "delta y must be positive");
        Global.assume(delta.getZ() > 0, "delta z must be positive");

        Vect min = box.getMin();
        Vect max = box.getMax();

        int nx = (int) Math.round((max.get(0) - min.get(0)) / delta.getX());
        int ny = (int) Math.round((max.get(1) - min.get(1)) / delta.getY());
        int nz = (int) Math.round((max.get(2) - min.get(2)) / delta.getZ());

        nx = nx < 1 ? 1 : nx;
        ny = ny < 1 ? 1 : ny;
        nz = nz < 1 ? 1 : nz;

        Vect start = min;
        Integers num = new Integers(nx, ny, nz);

        return new Sampling(start, delta, num);
    }

    public static Sampling create(Box box, Integers num)
    {
        Global.assume(num.getI() > 0, "num x must be positive");
        Global.assume(num.getJ() > 0, "num y must be positive");
        Global.assume(num.getK() > 0, "num z must be positive");

        Vect min = box.getMin();
        Vect max = box.getMax();

        double dx = (max.get(0) - min.get(0)) / num.getI();
        double dy = (max.get(1) - min.get(0)) / num.getJ();
        double dz = (max.get(2) - min.get(0)) / num.getK();

        Vect start = min;
        Vect delta = VectSource.create3D(dx, dy, dz);

        return new Sampling(start, delta, num);
    }

    public static Sampling create2D(double sx, double sy, double dx, double dy, int nx, int ny)
    {
        Vect start = VectSource.create3D(sx, sy, 0);
        Vect delta = VectSource.create3D(dx, dy, 1);
        Integers num = new Integers(nx, ny, 1);

        return new Sampling(start, delta, num);
    }

    public static Sampling create2D(int nx, int ny)
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(1, 1, 1);
        Integers num = new Integers(nx, ny, 1);

        return new Sampling(start, delta, num);
    }

    public static Sampling createIsotropic2D(double d, int nx, int ny)
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(d, d, 1);
        Integers num = new Integers(nx, ny, 1);

        return new Sampling(start, delta, num);
    }

    public static Sampling createAnisotropic2D(double dx, double dy, int nx, int ny)
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(dx, dy, 1);
        Integers num = new Integers(nx, ny, 1);

        return new Sampling(start, delta, num);
    }

    public static Sampling createAnisotropic(double dx, double dy, double dz, int nx, int ny, int nz)
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(dx, dy, dz);
        Integers num = new Integers(nx, ny, nz);

        return new Sampling(start, delta, num);
    }

    public static Sample createSample(int i, int j, int k)
    {
        return new Sample(i, j, k);
    }

    public static Sampling pad(Sampling sampling, int padi, int padj, int padk)
    {
        int numI = sampling.numI() + 2 * padi;
        int numJ = sampling.numJ() + 2 * padj;
        int numK = sampling.numK() + 2 * padk;

        Vect start = sampling.world(new Sample(-padi, -padj, -padk));

        return new Sampling(start, sampling.delta(), sampling.quat(), new Integers(numI, numJ, numK));
    }
}
