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

package qit.data.utils.vects.cluster;

import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

/** spatial-axial k-means clustering */
public class VectsClusterSAKM extends VectsClusterKM
{
    public static double DEFAULT_ALPHA = 1;
    public static double DEFAULT_BETA = 15;
    
    protected double alpha = DEFAULT_ALPHA;
    protected double beta = DEFAULT_BETA;

    public VectsClusterSAKM proto()
    {
        return new VectsClusterSAKM().with(this);
    }
    
    public VectsClusterSAKM with(VectsCluster set)
    {
        VectsClusterSAKM cast = (VectsClusterSAKM) set;
        super.with(set);
        this.alpha = cast.alpha;
        this.beta = cast.beta;
        
        return this;
    }
    
    public VectsClusterSAKM withK(int k)
    {
        return (VectsClusterSAKM) super.withK(k);
    }
    
    public VectsClusterSAKM withAlpha(double a)
    {
        this.alpha = a;
        this.labels = null;
        return this;
    }

    public VectsClusterSAKM withBeta(double b)
    {
        this.beta = b;
        this.labels = null;
        return this;
    }
    
    public static Vect pos(Vect v)
    {
        return VectSource.create(v.get(0), v.get(1), v.get(2));
    }
    
    public static Vect dir(Vect v)
    {
        return VectSource.create(v.get(3), v.get(4), v.get(5));
    }
    
    public static Vect cat(Vect p, Vect d)
    {
        Vect out = VectSource.createND(6);
        out.set(0, p.get(0));
        out.set(1, p.get(1));
        out.set(2, p.get(2));
        out.set(3, d.get(0));
        out.set(4, d.get(1));
        out.set(5, d.get(2));
        
        return out;
    }
    
    public double dist(Vect a, Vect b)
    {
        Vect ap = pos(a);
        Vect bp = pos(b);
        Vect ad = dir(a);
        Vect bd = dir(b);
        
        double d2p = ap.dist(bp);
        
        double dot = ad.dot(bd);
        double d2d = 1 - dot * dot;
        
        return this.alpha * d2p + this.beta * d2d;
    }
    
    public VectsClusterSAKM maximization()
    {
        this.centers.setAll(0.0);
        this.mixing.setAll(0.0);
        
        Vect[] pos = new Vect[this.k];
        Matrix[] dyads = new Matrix[this.k];
        for (int i = 0; i < this.k; i++)
        {
            pos[i] = VectSource.createND(3);
            dyads[i] = MatrixSource.constant(3,  3,  0);
        }
        
        for (int i = 0; i < this.vects.size(); i++)
        {
            int idx = this.labels[i] - 1;
            Vect v = this.vects.get(i);
            Vect p = pos(v);
            Vect d = dir(v);
            double weight = this.weights == null ? 1.0 : this.weights.get(i);
            
            if (idx >= this.k)
            {
                throw new RuntimeException("invalid label: " + idx);
            }
            
            pos[idx].plusEquals(weight, p);
            dyads[idx].plusEquals(weight, MatrixSource.dyadic(d));
            this.mixing.get(idx).plusEquals(weight);
        }
        
        for (int i = 0; i < this.k; i++)
        {
            double mix = this.mixing.get(i).get(0);
            double norm = MathUtils.zero(mix) ? 1.0 : 1.0 / mix;

            Vect p = pos[i].times(norm);
            Vect d = dyads[i].times(norm).prineig();
            
            this.centers.get(i).set(cat(p, d));
        }
        
        return this;
    }
}
