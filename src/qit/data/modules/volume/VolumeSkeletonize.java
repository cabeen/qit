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

package qit.data.modules.volume;

import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

@ModuleDescription("Extract a skeleton from the local maxima and ridges of a volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeSkeletonize implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("exclude voxels with an input intensity below this value")
    public Double threshold = null;

    @ModuleOutput
    @ModuleDescription("output skeleton")
    public Mask output;

    public VolumeSkeletonize run()
    {
        Sampling sampling = this.input.getSampling();

        Mask X = new Mask(sampling);
        Mask Y = new Mask(sampling);
        Mask Z = new Mask(sampling);

        for (Sample sample : sampling)
        {
            if (!valid(sample))
            {
                continue;
            }

            double value = this.input.get(sample, 0);

            Vect cofg = VectSource.create3D();
            double sum = 0;

            for (int k = -1; k <= 1; k++)
            {
                for (int j = -1; j <= 1; j++)
                {
                    for (int i = -1; i <= 1; i++)
                    {
                        Sample nsample = sample.offset(new Integers(i, j, k));
                        double nvalue = this.input.get(nsample, 0);

                        sum += nvalue;
                        cofg.plus(value, VectSource.create3D(i, j, k));
                    }
                }
            }

            cofg.timesEquals(1.0 / sum);
            double cofgl = cofg.norm();
            cofg.timesEquals(1.0 / cofgl);

            int idel = 0;
            int jdel = 0;
            int kdel = 0;

            if (cofgl > 0.1)
            {
                idel = (int) Math.max(Math.min(Math.round(cofg.getX()), 1), -1);
                jdel = (int) Math.max(Math.min(Math.round(cofg.getY()), 1), -1);
                kdel = (int) Math.max(Math.min(Math.round(cofg.getZ()), 1), -1);
            }
            else
            {
                double maxcost = 0;

                // search half the voxels...
                for (int k = 0; k <= 1; k++)
                {
                    for (int j = -1; j <= 1; j++)
                    {
                        for (int i = -1; i <= 1; i++)
                        {
                            if (k == 1 || j == 1 || (j == 0 && i == 1))
                            {
                                double up = this.input.get(sample.offset(new Integers(i, j, k)), 0);
                                double down = this.input.get(sample.offset(new Integers(-i, -j, -k)), 0);

                                double w = Math.pow(i * i + j * j + k * k, -0.7);
                                double cost = w * (2.0 * value - up - down);

                                if (cost > maxcost)
                                {
                                    maxcost = cost;
                                    idel = i;
                                    jdel = j;
                                    kdel = k;
                                }
                            }
                        }
                    }
                }
            }

            X.set(sample, idel);
            Y.set(sample, jdel);
            Z.set(sample, kdel);
        }

        Mask XX = new Mask(sampling);
        Mask YY = new Mask(sampling);
        Mask ZZ = new Mask(sampling);

        for (Sample sample : sampling)
        {
            if (!valid(sample))
            {
                continue;
            }

            int[] localsum = new int[27];
            int localmax = 0;

            for (int z = 0; z < 27; z++)
            {
                localsum[z] = 0;
            }

            for (int k = -1; k <= 1; k++)
            {
                for (int j = -1; j <= 1; j++)
                {
                    for (int i = -1; i <= 1; i++)
                    {
                        Sample nsample = sample.offset(new Integers(i, j, k));
                        int idel = X.get(nsample);
                        int jdel = Y.get(nsample);
                        int kdel = Z.get(nsample);

                        int a = (1 + kdel) * 9 + (1 + jdel) * 3 + 1 + idel;
                        int b = (1 - kdel) * 9 + (1 - jdel) * 3 + 1 - idel;

                        localsum[a] = localsum[a] + 1;
                        localsum[b] = localsum[b] + 1;
                    }
                }
            }

            for (int k = -1; k <= 1; k++)
            {
                for (int j = -1; j <= 1; j++)
                {
                    for (int i = -1; i <= 1; i++)
                    {
                        int idx = (1 + k) * 9 + (1 + j) * 3 + 1 + i;

                        if (localsum[idx] > localmax)
                        {
                            localmax = localsum[idx];
                            XX.set(sample, i);
                            YY.set(sample, j);
                            ZZ.set(sample, k);
                        }
                    }
                }
            }
        }

        Mask out = new Mask(sampling);

        for (Sample sample : sampling)
        {
            if (!valid(sample))
            {
                continue;
            }

            double value = this.input.get(sample, 0);
            int idel = XX.get(sample);
            int jdel = YY.get(sample);
            int kdel = ZZ.get(sample);

            double a = this.input.get(sample.offset(new Integers(idel, jdel, kdel)), 0);
            double b = this.input.get(sample.offset(new Integers(-idel, -jdel, -kdel)), 0);
            double c = this.input.get(sample.offset(new Integers(2 * idel, 2 * jdel, 2 * kdel)), 0);
            double d = this.input.get(sample.offset(new Integers(-2 * idel, -2 * jdel, -2 * kdel)), 0);

            if ((idel != 0 || jdel != 0 || kdel != 0) && value >= a && value > b && value >= c && value >= d)
            {
                out.set(sample, 1);
            }
        }

        this.output = out;

        return this;
    }

    private boolean valid(Sample sample)
    {
        if (this.input.getSampling().boundary(sample) || !this.input.valid(sample, this.mask))
        {
            return false;
        }

        double value = this.input.get(sample, 0);

        if (MathUtils.zero(value))
        {
            return false;
        }

        if (this.threshold != null && value < this.threshold)
        {
            return false;
        }

        return true;
    }
}
