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

import com.google.common.collect.Lists;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskDilate;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.curves.CurvesFunctionApply;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.math.source.DistanceSource;
import qit.math.structs.Box;
import qit.math.structs.CovarianceType;
import qit.math.structs.Distance;
import qit.math.structs.Gaussian;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;

@ModuleDescription("Segment a bundle by matching vertices along its length based on a prototype curve")
@ModuleAuthor("Ryan Cabeen")
public class CurvesSegmentAlong implements Module
{
    public static final String ARCLEN_ATTR = "label_arc";
    public static final String DIST_ATTR = "label_dist";

    public enum CurvesSegmentAlongType
    {
        Arclength, Distance, Hybrid
    }

    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input prototype curve (if not supplied, one will be computed)")
    public Curves proto;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a deformation to transform the proto curves to the coordinates of the input curves")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("the method for segmenting along the bundle")
    CurvesSegmentAlongType method = CurvesSegmentAlongType.Hybrid;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the number of points used for along tract mapping (if not supplied, the number of points in the proto will be used)")
    public Integer samples = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the density of points used for along tract mapping (if not supplied, the number of points in the proto will be used)")
    public Double density = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an attribute name for the label used to indicate the position along the bundle")
    public String label = Curves.LABEL;

    @ModuleParameter
    @ModuleDescription("remove outliers")
    public boolean outlier = false;

    @ModuleParameter
    @ModuleDescription("the number of points used for outlier rejection")
    public Integer outlierCount = 10;

    @ModuleParameter
    @ModuleDescription("the probability threshold for outlier rejection")
    public Double outlierThresh = 0.99;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use the following attributes for outlier detection (if they exist)")
    public String outlierAttrs = "FA,MD,frac,diff,S0,base";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the volume resolution for distance segmentation")
    public Double delta = 1.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the power for the mixing function (must be a positive even number, e.g. 2, 4, 6)")
    public Double power = 4.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output distance volume (not provided by arclength segmentation)")
    public Volume outputDist;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output label volume (not provided by arclength segmentation")
    public Volume outputLabel;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output proto curve (useful in case it was computed automatically)")
    public Curves outputProto;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output core curve (the nearest curve to the prototype)")
    public Curves outputCore;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    public CurvesSegmentAlong run()
    {
        if (this.input.size() == 0)
        {
            this.output = new Curves();
            return this;
        }

        Curves mycurves = this.inplace ? this.input : this.input.copy();
        Curves myprotos = this.pinit(mycurves, this.proto);

        this.orient(mycurves, myprotos);
        this.match(mycurves, myprotos);
        this.along(mycurves, myprotos);
        this.core(mycurves, myprotos);
        this.outlier(mycurves);

        this.output = mycurves;
        this.outputProto = myprotos;

        return this;
    }

    private Curves pinit(Curves curves, Curves protos)
    {
        if (protos != null)
        {
            return protos.copy();
        }

        Logging.info("finding mean curve for prototype");
        // find mean curve and use that as the prototype
        int meanidx = new CurvesResample()
        {{
            this.input = curves;
            this.num = 10;
            this.orient = true;
        }}.run().output.flatten().closestMean();

        Curves myprotos = new Curves();
        myprotos.add(this.input.get(meanidx));
        myprotos = CurvesOrient.axis(myprotos);

        if (this.samples != null && this.density != null)
        {
            Logging.error("samples and density are exclusive options");
        }

        if (this.samples != null)
        {
            Logging.info("resampling with samples: " + this.samples);
            CurvesResample resampler = new CurvesResample();
            resampler.input = myprotos;
            resampler.num = this.samples;
            myprotos = resampler.run().output;
        }
        else if (this.density != null)
        {
            Logging.info("resampling with density: " + this.density);
            CurvesResample resampler = new CurvesResample();
            resampler.input = myprotos;
            resampler.density = this.density;
            myprotos = resampler.run().output;
        }

        return myprotos;
    }

    private void orient(Curves curves, Curves protos)
    {
        Logging.info("orienting curves");
        Curve proto = protos.get(0);
        Vect protoHead = proto.getHead();
        Vect protoTail = proto.getTail();

        for (Curve curve : curves)
        {
            Vect testHead = curve.getHead();
            Vect testTail = curve.getTail();

            double distForward = testHead.dist2(protoHead) + testTail.dist2(protoTail);
            double distReverse = testHead.dist2(protoTail) + testTail.dist2(protoHead);

            if (distReverse < distForward)
            {
                curve.reverse();
            }
        }
    }

    private void match(Curves curves, Curves protos)
    {
        if (this.deform != null)
        {
            Logging.info("deforming proto curve");
            new CurvesFunctionApply().withCurves(protos).withFunction(this.deform).run();
        }

        Logging.info("finding new proto from inside bundle");
        Curve proto = protos.get(0);
        Distance<Curve> dister = DistanceSource.curveChamfer();
        Vect gamma = VectSource.linspace(0f, 1f, proto.size());
        double minDist = Double.MAX_VALUE;
        Curve minCurve = curves.get(0);

        for (Curve curve : curves)
        {
            Curve test = new Curves().add(curve).resample(gamma);
            double dist = dister.dist(proto, test);

            if (dist < minDist)
            {
                minDist = dist;
                minCurve = test;
            }
        }

        protos.removeAll();
        protos.add(minCurve);

        Logging.info(String.format("found proto with %d points", proto.size()));
        for (int i = 0; i < proto.size(); i++)
        {
            protos.get(0).set(this.label, i, VectSource.create1D(i + 1));
        }
    }

    public void arclength(Curves curves, Curves protos, String name)
    {
        Logging.progress("started segmenting curve by arclength matching");
        Curve proto = protos.get(0);

        Logging.info("computing arclength parameterization");
        CurvesFeatures features = new CurvesFeatures();
        features.input = curves;
        features.fraction = true;
        features.inplace = true;
        features.run();

        Logging.info("labeling vertices");
        int numPoints = proto.size();
        curves.add(name, VectSource.create1D());
        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                double fraction = curve.get(Curves.FRACTION, i).get(0);
                int label = (int) Math.round(fraction * (numPoints - 1)) + 1;
                curve.set(name, i, VectSource.create1D(label));
            }
        }

        Logging.progress("finished along tract segmentation");
    }

    public void distance(Curves curves, Curves protos, String name)
    {
        Logging.progress("started segmenting curve by distance matching");
        Curve proto = protos.get(0);

        Box box = curves.bounds().scale(1.5).buffer(5 * this.delta);
        Sampling sampling = SamplingSource.create(box, this.delta);
        Mask mask = MaskUtils.voxelize(sampling, curves);

        MaskDilate dilater = new MaskDilate();
        dilater.input = mask;
        dilater.num = 1;
        mask = dilater.run().output;

        this.outputDist = VolumeSource.create(sampling);
        this.outputLabel = this.outputDist.proto();

        this.outputDist.setAll(VectSource.create1D(Double.MAX_VALUE));

        Logging.progress("computing distance and label maps");
        for (int i = 0; i < proto.size(); i++)
        {
            Vect p = proto.get(i);
            for (Sample s : sampling)
            {
                if (mask.foreground(s))
                {
                    double d2 = sampling.world(s).dist2(p);
                    double pd2 = this.outputDist.get(s, 0);
                    if (d2 < pd2)
                    {
                        // use one-based labels
                        this.outputLabel.set(s, 0, i + 1);
                        this.outputDist.set(s, 0, d2);
                    }
                }
            }
        }

        Logging.progress("parameterizing curves");
        curves.add(name, VectSource.create1D());
        curves.add(Curves.DIST, VectSource.create1D());
        for (Curve c : curves)
        {
            for (int i = 0; i < c.size(); i++)
            {
                Vect point = c.get(i);
                Sample nearestSample = sampling.nearest(point);
                int nearestLabel = (int) Math.round(this.outputLabel.get(nearestSample, 0));
                double nearestDist = this.outputDist.get(nearestSample, 0);
                c.set(name, i, VectSource.create1D(nearestLabel));
                c.set(Curves.DIST, i, VectSource.create1D(nearestDist));
            }
        }

        Logging.progress("finished along tract segmentation");
    }

    public void hybrid(Curves curves, String name)
    {
        Logging.progress("started segmenting curve by hybrid matching");

        // assume that distance and arclength labels are present

        curves.add(name, VectSource.create1D());
        int maxLabel = 0;

        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                double fraction = curve.get(Curves.FRACTION, i).get(0);

                int labelArclen = (int) curve.get(ARCLEN_ATTR, i).get(0);
                int labelDist = (int) curve.get(DIST_ATTR, i).get(0);

                double mix = Math.pow(2 * fraction - 1, 4);
                int labelHybrid = (int) Math.round(mix * labelArclen + (1.0 - mix) * labelDist);
                maxLabel = Math.max(labelHybrid, maxLabel);

                curve.set(name, i, VectSource.create1D(labelHybrid));
            }
        }

        Logging.progress("finished along tract segmentation");
    }

    private void along(Curves mycurves, Curves myprotos)
    {
        switch (this.method)
        {
            case Arclength:
            {
                this.arclength(mycurves, myprotos, this.label);
                break;
            }
            case Distance:
            {
                this.distance(mycurves, myprotos, this.label);
                break;
            }
            case Hybrid:
            {
                this.arclength(mycurves, myprotos, ARCLEN_ATTR);
                this.distance(mycurves, myprotos, DIST_ATTR);
                this.hybrid(mycurves, this.label);
                break;
            }
        }

        mycurves.remove(ARCLEN_ATTR);
        mycurves.remove(DIST_ATTR);
        mycurves.remove(Curves.FRACTION);
        mycurves.remove(Curves.DIST);
    }

    private void core(Curves mycurves, Curves myprotos)
    {
        Logging.info("finding core bundle");
        Curve myproto = myprotos.get(0);
        Curves core = new Curves();

        Vect gamma = VectSource.linspace(0f, 1f, myproto.size());
        double minDist = Double.MAX_VALUE;
        Curve minCurve = null;
        Distance<Curve> dister = DistanceSource.curve("haus");

        for (Curve curve : mycurves)
        {
            double dist = dister.dist(myproto, new Curves().add(curve).resample(gamma));

            if (dist < minDist)
            {
                minDist = dist;
                minCurve = curve;
            }
        }

        if (minCurve != null)
        {
            core.add(minCurve);
        }
        else
        {
            Logging.info("warning: no core found");
        }

        this.outputCore = core;
    }

    private void outlier(Curves curves)
    {
        if (!this.outlier)
        {
            return;
        }

        // assume the curves have already been oriented

        Logging.info("started removing outliers");

        Logging.info("resampling curves");
        Vect gamma = VectSource.linspace(0, 1, this.outlierCount);
        Curves resampled = curves.copy();
        resampled.resample(gamma);

        Logging.info("flattening curves");
        List<String> names = Lists.newArrayList();
        names.add(Curves.COORD);
        if (this.outlierAttrs != null)
        {
            for (String name : this.outlierAttrs.split(","))
            {
                if (curves.has(name))
                {
                    Logging.info(String.format("using %s attribute for outlier detection", name));
                    names.add(name);
                }
            }
        }

        Vects flatten = new Vects();
        for (Curve curve : resampled)
        {
            flatten.add(curve.getAll(names).flatten());
        }

        Logging.info("outlier data dimension: " + flatten.getDim());

        Logging.info("fitting gaussian");
        VectsGaussianFitter fitter = new VectsGaussianFitter().withInput(flatten).withAdd(1e-6);
        fitter.withType(CovarianceType.diagonal);
        Gaussian gauss = fitter.getOutput();

        Logging.info("testing for outliers");
        boolean[] inlier = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            Vect x = flatten.get(i);
            double mahal2 = gauss.mahal2(x);

            // mahal2 is distributed as chi-squared
            double pcum = new ChiSquaredDistribution(gauss.getDim() - 1).cumulativeProbability(mahal2);

            inlier[i] = pcum <= this.outlierThresh;
        }

        int kept = MathUtils.count(inlier);

        Logging.info(String.format("keeping %d of %d curves", kept, inlier.length));
        curves.keep(inlier);

        Logging.info("finished removing outliers");
    }
}