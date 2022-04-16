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
import qit.base.JsonDataset;
import qit.math.utils.MathUtils;

public class Bary extends JsonDataset
{
    // Model a point on a face using barycentric coordinates:
    // p = u * a + v * b + w * c, subject to u + v + w = 1
    private double u;
    private double v;
    private double w;

    public Bary(double u, double v, double w)
    {
        Global.assume(MathUtils.unit(u + v + w), "parameters do not define valid barycentric coordinates");

        this.u = u;
        this.v = v;
        this.w = w;
    }

    public double getU()
    {
        return this.u;
    }

    public double getV()
    {
        return this.v;
    }

    public double getW()
    {
        return this.w;
    }

    public boolean isVert()
    {
        return MathUtils.unit(this.u) || MathUtils.unit(this.v) || MathUtils.unit(this.w);
    }

    public boolean isEdge()
    {
        return MathUtils.unit(this.u + this.v) || MathUtils.unit(this.v + this.w) || MathUtils.unit(this.w + this.u);
    }

    public int hashCode()
    {
        int hc = 0;
        hc += (int) (1000 * this.u);
        hc += (int) (1000 * this.v);
        hc += (int) (1000 * this.w);

        return hc;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Bary))
        {
            return false;
        }

        if (obj == this)
        {
            return true;
        }

        Bary b = (Bary) obj;
        return MathUtils.eq(this.u, b.u) && MathUtils.eq(this.v, b.v) && MathUtils.eq(this.w, b.w);
    }

    public Bary copy()
    {
        return new Bary(this.u, this.v, this.w);
    }
}
