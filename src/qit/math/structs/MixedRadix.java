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

package qit.math.structs;

import qit.base.Global;
import qit.math.utils.MathUtils;

public class MixedRadix
{
    private int[] bases;
    private int num;
    private int[] sizes;

    public MixedRadix(int[] sizes)
    {
        Global.assume(sizes != null, "The arguments cannot be null.");

        for (int i = 0; i < sizes.length; i++)
        {
            Global.assume(sizes[i] >= 0, "The sizes must be non-negative.  Element " + i + " is negative: " + sizes[i]);
        }

        this.sizes = MathUtils.copy(sizes);
        this.bases = new int[sizes.length];

        // Compute the bases
        this.bases[0] = 1;
        for (int i = 1; i < sizes.length; i++)
        {
            this.bases[i] = sizes[i - 1] * this.bases[i - 1];
        }

        this.num = this.bases[this.bases.length - 1] * this.sizes[this.bases.length - 1];
    }

    public int[] getValue(int s)
    {
        Global.assume(s >= 0 && s <= this.num, "The decimal must be in the interval [0," + this.num + "].");

        int[] output = new int[sizes.length];
        int n = this.bases.length;

        if (n > 1)
        {
            // Initialize
            int[] buffer = new int[sizes.length];
            buffer[0] = s;
            output[0] = s - (int) Math.floor(s / this.bases[1]) * this.bases[1];

            // Loop
            for (int i = 1; i < n - 1; i++)
            {
                buffer[i] = buffer[i - 1] - output[i - 1] * this.bases[i - 1];
                output[i] = (buffer[i] - (int) Math.floor(buffer[i] / this.bases[i + 1]) * this.bases[i + 1]) / this.bases[i];
            }

            // Finish
            buffer[n - 1] = buffer[n - 2] - output[n - 2] * this.bases[n - 2];
            output[n - 1] = buffer[n - 1] / this.bases[n - 1];

            return output;
        }
        else
        {
            return new int[]{s};
        }
    }

    public int getValue(int[] b)
    {
        Global.assume(b != null, "The argument must not be null");
        Global.assume(this.bases.length == b.length, "The arrays must be the same size.");

        // Check if it is from the expected interval
        for (int i = 0; i < b.length; i++)
        {
            Global.assume(b[i] < this.sizes[i] && b[i] >= 0, "The " + i + "-th numeral '" + b[i] + "' must be from [0," + this.sizes[i] + "].");
        }

        int out = 0;
        for (int i = 0; i < this.bases.length; i++)
        {
            out += this.bases[i] * b[i];
        }

        return out;
    }

    public int size()
    {
        return this.num;
    }

    public int getNumElements(int i)
    {
        Global.assume(i >= 0 && i < this.sizes.length, "The given index must be from [0," + this.sizes.length + ").");

        return this.sizes[i];
    }

    public int getDimension()
    {
        return this.sizes.length;
    }
}