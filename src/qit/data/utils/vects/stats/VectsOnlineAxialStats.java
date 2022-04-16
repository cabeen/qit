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

package qit.data.utils.vects.stats;

import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.MatrixSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;

public class VectsOnlineAxialStats
{
    private Matrix dyads = null;
    private double sumw = 0;

    public int count = 0;
    public Vect mean;
    public Double lambda;
    public Double coherence;

    public VectsOnlineAxialStats update(Vect v)
    {
        return this.update(1, v);
    }

    public VectsOnlineAxialStats update(double w, Vect v)
    {
        Matrix dyad = MatrixSource.dyadic(v).times(w);

        // Update sufficient statistics
        if (this.dyads == null)
        {
            this.dyads = dyad;
        }
        else
        {
            this.dyads.plusEquals(dyad);
        }

        this.sumw += w;
        this.count += 1;

        // Compute resulting statistics
        EigenDecomp eig = MatrixUtils.eig(this.dyads.times(1.0 / this.sumw));
        double v0 = eig.values.get(0);
        double v1 = eig.values.get(1);
        double v2 = eig.values.get(2);

        this.coherence = 1 - Math.sqrt((v1 + v2) / (2 * v0));
        this.lambda = v0;
        this.mean = eig.vectors.get(0);

        return this;
    }
}
