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

package qit.data.modules.mask;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.utils.MaskUtils;
import qit.data.utils.volume.VolumeBinaryFunction;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.BinaryVectFunction;
import qit.math.source.BinaryVectFunctionSource;
import qit.math.source.VectFunctionSource;

@ModuleDescription("Compute a distance transform using pff's fast algorithm")
@ModuleCitation("Felzenszwalb, P., & Huttenlocher, D. (2004). DistanceExtrinsic transforms of sampled functions. Cornell University.")
@ModuleAuthor("Ryan Cabeen")
public class MaskDistanceTransform implements Module
{
    @ModuleInput
    @ModuleDescription("the input mask")
    public Mask input;

    @ModuleParameter
    @ModuleDescription("compute a signed transform")
    public boolean signed = false;

    @ModuleOutput
    @ModuleDescription("the output distance transform")
    public Volume output;

    public MaskDistanceTransform run()
    {
        Volume dist = dt(this.input);

        if (this.signed)
        {
            Mask inverted = MaskUtils.invert(this.input);
            Volume idist = dt(inverted);
            BinaryVectFunction comb = BinaryVectFunctionSource.lincomb(1.0, -1.0);
            dist = new VolumeBinaryFunction(comb).withLeft(dist).withRight(idist).getOutput();
        }

        this.output = dist;

        return this;
    }

    private static Volume dt(Mask input)
    {
        Sampling sampling = input.getSampling();
        int numx = sampling.numI();
        int numy = sampling.numJ();
        int numz = sampling.numK();

        int maxd = Math.max(numx, Math.max(numy, numz)) + 1;
        double[] f = new double[maxd];
        double[] z = new double[maxd];
        int[] v = new int[maxd];
        double[] d = new double[maxd];

        double maxr = Math.pow(2.0 * maxd * sampling.deltaMax(), 2);

        Volume dist = input.protoVolume();
        for (Sample sample : dist.getSampling())
        {
            if (!dist.valid(sample, input))
            {
                dist.set(sample, 0, maxr);
            }
        }

        for (int i = 0; i < numx; i++)
        {
            for (int j = 0; j < numy; j++)
            {
                int n = numz;
                for (int k = 0; k < n; k++)
                {
                    f[k] = dist.get(i, j, k, 0);
                    z[k] = 0;
                    v[k] = 0;
                    d[k] = 0;
                }
                dt(n, f, z, v, d);
                for (int k = 0; k < n; k++)
                {
                    dist.set(i, j, k, 0, d[k]);
                }
            }
        }

        for (int i = 0; i < numx; i++)
        {
            for (int k = 0; k < numz; k++)
            {
                int n = numy;
                for (int j = 0; j < n; j++)
                {
                    f[j] = dist.get(i, j, k, 0);
                    z[j] = 0;
                    v[j] = 0;
                    d[j] = 0;
                }
                dt(n, f, z, v, d);
                for (int j = 0; j < n; j++)
                {
                    dist.set(i, j, k, 0, d[j]);
                }
            }
        }

        for (int j = 0; j < numy; j++)
        {
            for (int k = 0; k < numz; k++)
            {
                int n = numx;
                for (int i = 0; i < n; i++)
                {
                    f[i] = dist.get(i, j, k, 0);
                    z[i] = 0;
                    v[i] = 0;
                    d[i] = 0;
                }
                dt(n, f, z, v, d);
                for (int i = 0; i < n; i++)
                {
                    dist.set(i, j, k, 0, d[i]);
                }
            }
        }

        return new VolumeFunction(VectFunctionSource.sqrt()).withInput(dist).run();
    }

    private static void dt(int n, double[] f, double[] z, int[] v, double[] d)
    {
        int k = 0;
        v[0] = 0;
        z[0] = Double.NEGATIVE_INFINITY;
        z[1] = Double.POSITIVE_INFINITY;

        for (int q = 1; q < n; q++)
        {
            while (true)
            {
                int vk = v[k];
                double fq = f[q];
                double fvk = f[vk];
                double ta = fq + q * q;
                double tb = fvk + vk * vk;
                double tc = 2 * q - 2 * vk;
                double s = (ta - tb) / tc;
                if (s <= z[k])
                {
                    k = k - 1;
                    continue;
                }
                else
                {
                    k = k + 1;
                    v[k] = q;
                    z[k] = s;
                    z[k + 1] = Double.MAX_VALUE;
                    break;
                }
            }
        }

        k = 0;
        for (int q = 0; q < n; q++)
        {
            while (z[k + 1] < q)
            {
                k = k + 1;
            }
            double ta = q - v[k];
            d[q] = ta * ta + f[v[k]];
        }
    }

    public static Volume apply(Mask mask, boolean signed)
    {
        MaskDistanceTransform run = new MaskDistanceTransform();
        run.input = mask;
        run.signed = signed;

        return run.run().output;
    }

    public static Volume apply(Mask mask)
    {
        return apply(mask, false);
    }
}
