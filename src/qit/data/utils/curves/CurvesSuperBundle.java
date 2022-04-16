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

package qit.data.utils.curves;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.IndexedList;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.modules.curves.CurvesResample;
import qit.data.modules.curves.CurvesSimplify;
import qit.data.source.MatrixSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectUtils;
import qit.math.structs.Distance;
import qit.math.source.DistanceSource;
import qit.math.utils.MathUtils;

import java.util.List;

public class CurvesSuperBundle
{
    private Curves curves;
    private Distance<Curve> dist = DistanceSource.curveHausdorff();
    private int maxiter = 10;
    private int min = 50;
    private int max = 500;
    private Double thresh = null;
    private Double density = null;
    private Double epsilon = null;

    public CurvesSuperBundle()
    {
    }

    public CurvesSuperBundle withDistance(Distance<Curve> d)
    {
        this.dist = d;

        return this;
    }

    public CurvesSuperBundle withThresh(double d)
    {
        this.thresh = d;

        return this;
    }

    public CurvesSuperBundle withDensity(double d)
    {
        this.density = d;
        return this;
    }

    public CurvesSuperBundle withEpsilon(double d)
    {
        this.epsilon = d;
        return this;
    }

    public CurvesSuperBundle withCurves(Curves c)
    {
        this.curves = c;

        return this;
    }

    public CurvesSuperBundle run()
    {
        Logging.info("... starting curve clustering");

        Curves source = this.curves;

        if (this.density != null)
        {
            Logging.info("resampling curves");
            CurvesResample resampler = new CurvesResample();
            resampler.input = source;
            resampler.density = this.density;
            source = resampler.run().output;
        }

        if (this.epsilon != null)
        {
            Logging.info("simplifying curves");
            CurvesSimplify simplify = new CurvesSimplify();
            simplify.epsilon = this.epsilon;
            simplify.input = source;
            source = simplify.run().output;
        }

        Logging.info("started clustering");
        
        int numCurves = source.size();
        List<Curve> prototypes = Lists.newArrayList();
        int[] clusterLabels = new int[numCurves];

        for (int i = 0; i < this.min; i++)
        {
            int idx = Global.RANDOM.nextInt(source.size());
            prototypes.add(source.get(idx));
        }

        int iter = 0;
        while (iter < this.maxiter)
        {
            List<Integer> changedLabels = Lists.newArrayList();

            Logging.info("... assignment iteration " + iter);
            for (int i = 0; i < numCurves; i++)
            {
                Curve curve = this.curves.get(i);

                int numClusters = prototypes.size();
                double[] distClusters = new double[numClusters];
                for (int j = 0; j < numClusters; j++)
                {
                    distClusters[j] = this.dist.dist(curve, prototypes.get(j));
                }
                int minIdx = MathUtils.minidx(distClusters);
                double minDist = distClusters[minIdx];

                int prevLabel = clusterLabels[i];
                if (minDist > this.thresh && numClusters <= max)
                {
                    clusterLabels[i] = prototypes.size();
                    prototypes.add(curve);
                }
                else
                {
                    clusterLabels[i] = minIdx;
                }

                if (prevLabel != clusterLabels[i])
                {
                    changedLabels.add(clusterLabels[i]);
                }
            }

            Logging.info("... update iteration " + iter);
            for (Integer label : changedLabels)
            {
                List<Integer> idx = Lists.newArrayList();
                IndexedList<Curve> curves = new IndexedList<Curve>();
                for (Integer i : MathUtils.equals(clusterLabels, label))
                {
                    idx.add(i);
                    curves.add(this.curves.get(i));
                }

                Matrix clusterDists = MatrixSource.distSym(this.dist, curves);
                Vect clusterSumDists = MatrixUtils.rowsum(clusterDists);
                int minIdx = VectUtils.minidx(clusterSumDists);

                prototypes.set(label, this.curves.get(idx.get(minIdx)));
            }

            iter += 1;
        }

        clusterLabels = MathUtils.relabel(clusterLabels);

        CurvesUtils.attrSetLabelsPerCurve(this.curves, Curves.LABEL, clusterLabels);

        Logging.info("finished clustering");

        return this;
    }
}
