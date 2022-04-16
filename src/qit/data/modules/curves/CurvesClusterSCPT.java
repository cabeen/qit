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

package qit.data.modules.curves;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleExpert;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Curves;
import qit.data.datasets.Vects;
import qit.data.utils.curves.CurvesClusterSCPTInject;
import qit.data.utils.vects.cluster.VectsClusterDPM;
import qit.data.utils.vects.cluster.VectsClusterKM;

@ModuleDescription("Cluster curves with a sparse closest point transform.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesClusterSCPT implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("take a subset of curves before clustering (not applicable if you provide fewer)")
    public Integer subset = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the threshold size for clusters")
    public Double thresh = 22.0;

    @ModuleParameter
    @ModuleDescription("retain only the largest cluster")
    public boolean largest = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("retain only clusters that are a given proportion of the total (zero to one)")
    public Double fraction = null;

    @ModuleParameter
    @ModuleExpert
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("preprocess by resampling the curves")
    public Double preden = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("preprocess by simplifying the curves")
    public Double preeps = 1.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the number of curves for landmarking")
    public Integer lmsub = 5000;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the simplifification threshold for landmarking")
    public Double lmeps = 1d;

    @ModuleParameter
    @ModuleExpert
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the number of clusters for landmarking")
    public Integer lmnum = 2;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the landmarking threshold")
    public Double lmthresh = 30d;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the maxima number of iterations")
    public Integer iters = 100;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the number of restarts")
    public Integer restarts = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the number of clusters")
    public Integer num = 2;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output curves")
    public Curves output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the computed landmarks")
    public Vects landmarks = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the prototypical curves for each cluster")
    public Curves protos = null;

    @Override
    public CurvesClusterSCPT run()
    {
        if (this.input.size() == 0)
        {
            this.output = new Curves();
            this.protos = new Curves();
            return this;
        }

        Curves curves = null;

        if (this.subset != null)
        {
            CurvesReduce reducer = new CurvesReduce();
            reducer.input = this.input;
            reducer.count = this.subset;
            curves = reducer.run().output;
        }
        else
        {
            curves = this.input.copy();
        }

        if (curves.size() == 0)
        {
            Logging.info("no curves found");
            this.landmarks = new Vects();
            this.output = new Curves();
            this.protos = new Curves();

            return this;
        }

        CurvesClosestPointTransform transform = new CurvesClosestPointTransform();
        {
            CurvesLandmarks landmarker = new CurvesLandmarks();
            landmarker.input = curves;

            if (this.lmsub != null)
            {
                landmarker.subsamp = this.lmsub;
            }

            if (this.lmeps != null)
            {
                landmarker.eps = this.lmeps;
            }

            if (this.lmthresh != null)
            {
                landmarker.radius = this.lmthresh;
            }

            if (this.lmnum != null)
            {
                landmarker.num = this.lmnum;
            }

            Vects lm = landmarker.getOutput();
            transform.landmarks = lm;
            this.landmarks = lm;

            Logging.info("computed %d landmarks", lm.size());
        }

        VectsClusterKM cluster;
        if (this.thresh != null)
        {
            double lambda = this.thresh * this.thresh * transform.landmarks.size();
            cluster = new VectsClusterDPM().withLambda(lambda);
        }
        else
        {
            cluster = new VectsClusterKM();
        }

        cluster.withMaxIter(this.iters);

        int k = Math.min(this.num, curves.size());
        cluster.withK(k);

        if (this.restarts != null)
        {
            cluster.withRestarts(this.restarts);
        }

        CurvesClusterSCPTInject op = new CurvesClusterSCPTInject();
        op.withCurves(curves);
        op.withTransform(transform);
        op.withCluster(cluster);

        if (this.preden != null)
        {
            op.withDensity(this.preden);
        }

        if (this.preeps != null)
        {
            op.withEpsilon(this.preeps);
        }

        this.protos = op.run();

        CurvesRelabel relabel = new CurvesRelabel();
        relabel.input = curves;
        relabel.largest = this.largest;
        relabel.threshold = this.fraction;
        relabel.inplace = true;
        relabel.run();

        this.output = curves;

        return this;
    }

    public static Curves applyCluster(Curves curves, double threshold)
    {
        return new CurvesClusterSCPT()
        {{
            this.input = curves;
            this.thresh = threshold;
        }}.run().output;
    }

    public static Curves applySimplify(Curves curves, double threshold)
    {
        return new CurvesClusterSCPT()
        {{
            this.input = curves;
            this.thresh = threshold;
        }}.run().protos;
    }
}
