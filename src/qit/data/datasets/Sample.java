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

package qit.data.datasets;

import qit.base.structs.Integers;
import qit.base.utils.JsonUtils;
import qit.data.source.VectSource;

/** a representation of the address of a voxel in a volume (not the spatial coordinates) */
public class Sample
{
    private int i;
    private int j;
    private int k;

    public Sample(Integers idx)
    {
        if (idx.size() != 3)
        {
            throw new RuntimeException("invalid sample integers");
        }

        this.i = idx.getI();
        this.j = idx.getJ();
        this.k = idx.getK();
    }

    public Sample(int[] idx)
    {
        if (idx.length != 3)
        {
            throw new RuntimeException("invalid sample array length");
        }

        this.i = idx[0];
        this.j = idx[1];
        this.k = idx[2];
    }

    public Sample(int i, int j, int k)
    {
        this.i = i;
        this.j = j;
        this.k = k;
    }

    public Sample(Sample sample, Integers offset)
    {
        this(sample.getI() + offset.getI(), sample.getJ() + offset.getJ(), sample.getK() + offset.getK());
    }

    public Sample offset(Integers offset)
    {
        return new Sample(this, offset);
    }

    public Integers integers()
    {
        return new Integers(this.i, this.j, this.k);
    }

    public int getI()
    {
        return this.i;
    }

    public int getJ()
    {
        return this.j;
    }

    public int getK()
    {
        return this.k;
    }
    
    public void get(int[] out)
    {
        out[0] = this.i;
        out[1] = this.j;
        out[2] = this.k;
    }
    
    public int[] get()
    {
        int[] out = new int[3];
        this.get(out);
        return out;
    }

    public Vect vect()
    {
        return VectSource.create3D(this.i, this.j, this.k);
    }

    public int get(int idx)
    {
        if (idx == 0)
        {
            return this.i;
        }
        else if (idx == 1)
        {
            return this.j;
        }
        else if (idx == 2)
        {
            return this.k;
        }
        else
        {
            throw new RuntimeException("invalid index");
        }
    }

    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (obj instanceof Sample)
        {
            Sample test = (Sample) obj;
            return test.i == this.i && test.j == this.j && test.k == this.k;
        }
        else
        {
            return false;
        }
    }

    public String toString()
    {
        return JsonUtils.encode(this);
    }

    public int hashCode()
    {
        return this.i + this.j + this.k;
    }
}
