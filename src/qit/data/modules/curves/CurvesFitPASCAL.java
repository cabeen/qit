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

import qit.base.Dataset;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Deformation;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.curves.CurvesPointFeatures;
import qit.data.utils.vects.cluster.VectsClusterEM;
import qit.data.utils.vects.cluster.VectsClusterGWM;
import qit.data.utils.vects.cluster.VectsClusterSADPM;
import qit.data.utils.vects.cluster.VectsClusterSAKM;
import qit.math.structs.CovarianceType;
import qit.math.structs.GaussianWatsonMixture;
import qit.math.utils.MathUtils;

@ModuleUnlisted
@ModuleDescription("Fit a PASCAL model to a bundle")
@ModuleCitation("(in preparation)")
@ModuleAuthor("Ryan Cabeen")
public class CurvesFitPASCAL implements Module
{
    public static final int DEFAULT_PRESMOOTH = 5;
    public static final double DEFAULT_REG_SPAT = 5.0;
    public static final double DEFAULT_REG_ANG = 0.01;
    public static final double DEFAULT_ALPHA = 1.0d;
    public static final double DEFAULT_BETA = 10.0d;
    public static final double DEFAULT_LAMBDA = 20.0d;

    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("regularize the spatial component by a given amount")
    public Double regspat = DEFAULT_REG_SPAT;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("regularize the angular compoennt by a given amount")
    public Double regang = DEFAULT_REG_ANG;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a deformation")
    public Deformation deform = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an attribute name for the label")
    public String attr = Curves.LABEL;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the number of iterations")
    public Integer iters = 20;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the number of restarts")
    public Integer restarts = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the initial number of clusters")
    public Integer num = 5;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the alpha parameter for spatial extent")
    public Double alpha = DEFAULT_ALPHA;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the beta parameter for angular extent")
    public Double beta = DEFAULT_BETA;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the lambda parameter for region size")
    public Double lambda = DEFAULT_LAMBDA;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("prefilter curves with polynomial spline of the given order")
    public Integer prefilter = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("weight by residual from polynomial spline with the given order")
    public Integer weight = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output model")
    public Dataset output = null;
//
//    @ModuleOutput
//    @ModuleOptional
//    @ModuleDescription("the output curves")
//    public Curves curves;

    public CurvesFitPASCAL run()
    {
        Curves curves = this.inplace ? this.input : this.input.copy();

        if (this.prefilter != null)
        {
            final Curves fcurves = curves;
            curves = new CurvesFilterPolynomial(){{
                this.input = fcurves;
                this.order = CurvesFitPASCAL.this.prefilter;
            }}.run().output;
        }

        VectsClusterEM cluster = getCluster();
        CurvesPointFeatures features = new CurvesPointFeatures().withInput(this.input);

        if (this.weight != null)
        {
            final Curves fcurves = curves;
            features.withWeights(new CurvesFilterPolynomial(){{
                this.input = fcurves;
                this.residual = true;
                this.order = CurvesFitPASCAL.this.weight;
            }}.run().outputResiduals.flatten().autoweights());
        }

        if (this.deform != null)
        {
            features.withTransform(this.deform);
        }
        features.run();
        
        this.output = fit(curves, features, cluster);
//        this.curves = curves;

        return this;
    }

    private VectsClusterEM getCluster()
    {
        VectsClusterSAKM cluster;

        if (this.lambda != null)
        {
            cluster = new VectsClusterSADPM().withLambda(this.lambda);
        }
        else
        {
            cluster = new VectsClusterSAKM();
        }

        if (this.alpha != null)
        {
            cluster.withAlpha(this.alpha);
        }

        if (this.beta != null)
        {
            cluster.withBeta(this.beta);
        }

        if (this.restarts != null)
        {
            cluster.withRestarts(this.restarts);
        }

        cluster.withMaxIter(this.iters);
        cluster.withK(this.num);

        return cluster;
    }

    public GaussianWatsonMixture fit(Curves curves, CurvesPointFeatures features, VectsClusterEM cluster)
    {
        Vect weights = features.getOutputWeights();
        Vects pos = features.getOutputPos();
        Vects dir = features.getOutputDir();

        Vects cat = new Vects();
        for (int i = 0; i < pos.size(); i++)
        {
            cat.add(VectSource.cat(pos.get(i), dir.get(i)));
        }

        cluster.withVects(cat);
        cluster.withWeights(weights);
        int[] labels = cluster.getOutput();

        int nk = MathUtils.values(labels).size();

        // run one iteration to estimate mixture model parameters
        VectsClusterGWM ncluster = new VectsClusterGWM();
        ncluster.withK(nk);
        ncluster.withVects(cat);
        ncluster.withWeights(weights);
        ncluster.withType(CovarianceType.full.toString());
        ncluster.withGaussianReg(this.regspat);
        ncluster.withWatsonReg(this.regang);
        ncluster.init(labels);

        // add these as vertex attributes
        labels = ncluster.getOutput();
        GaussianWatsonMixture model = ncluster.getModel();

//        Vect density = model.density(ncluster.getPos(), ncluster.getDir());
//
//        curves.add(this.attr, VectSource.createND(1));
//        curves.add(Curves.DENSITY, VectSource.createND(1));
//
//        // assume this happens in the same order as initialization...
//        int idx = 0;
//        for (int i = 0; i < curves.size(); i++)
//        {
//            Curve curve = curves.get(i);
//
//            for (int j = 0; j < curve.size(); j++)
//            {
//                double den = density.get(idx);
//                den = -Math.log(den);
//
//                curve.set(Curves.DENSITY, j, VectSource.create1D(den));
//                curve.set(this.attr, j, VectSource.create1D(labels[idx]));
//
//                if (j > 0)
//                {
//                    idx += 1;
//                }
//            }
//        }

        return model;
    }
}
