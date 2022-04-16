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

import qit.base.Logging;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.modules.curves.CurvesClosestPointTransform;
import qit.data.modules.curves.CurvesResample;
import qit.data.modules.curves.CurvesSimplify;
import qit.data.utils.CurvesUtils;
import qit.data.utils.vects.cluster.VectsClusterKM;
import qit.math.utils.MathUtils;

public class CurvesClusterSCPTInject
{
    private Curves curves;

    private VectsClusterKM cluster = null;
    private CurvesClosestPointTransform transform = null;
    private Double density = null;
    private Double epsilon = null;

    public CurvesClusterSCPTInject()
    {
    }

    public CurvesClusterSCPTInject withCurves(Curves c)
    {
        this.curves = c;

        return this;
    }

    public CurvesClusterSCPTInject withCluster(VectsClusterKM m)
    {
        this.cluster = m;

        return this;
    }

    public CurvesClusterSCPTInject withTransform(CurvesClosestPointTransform v)
    {
        this.transform = v;

        return this;
    }

    public CurvesClusterSCPTInject withDensity(double d)
    {
        this.density = d;
        return this;
    }

    public CurvesClusterSCPTInject withEpsilon(double d)
    {
        this.epsilon = d;
        return this;
    }

    public Curves run()
    {
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

        Logging.info("... transforming");
        this.transform.input = source;
        Vects values = this.transform.run().output;

        Logging.info("... clustering");
        this.cluster.withVects(values);
        int[] labels = this.cluster.getOutput();
        CurvesUtils.attrSetLabelsPerCurve(this.curves, Curves.LABEL, labels);

        Logging.info("... cleaning up");
        Vects centers = this.cluster.getCenters();
        Curves out = new Curves();
        for (int i = 0; i < centers.size(); i++)
        {
            Vect mcenter = centers.get(i);
            double[] dists = new double[values.size()];
            for (int j = 0; j < values.size(); j++)
            {
                dists[j] = values.get(j).dist(mcenter);
            }
            int minidx = MathUtils.minidx(dists);

            out.add(this.curves.get(minidx));
        }

        return out;
    }
}
