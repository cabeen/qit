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

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Record;
import qit.data.datasets.Sampling;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.volume.VolumeMeasure;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.SamplingSource;
import qit.data.source.TableSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.MaskUtils;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectUtils;
import qit.data.utils.VectsUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.volume.VolumeVoxelStats;
import qit.math.source.DistanceSource;
import qit.math.structs.Box;
import qit.math.structs.Distance;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Measure statistics of curves and store the results in a table")
@ModuleAuthor("Ryan Cabeen")
public class CurvesMeasure implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("the volume resolution")
    public double delta = 1;

    @ModuleParameter
    @ModuleDescription("the density threshold")
    public double thresh = 0.5;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("add advanced measures (outlier measures).  warning: these take polynomial time with the number of curves")
    public boolean advanced = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("add the endpoint correlation measure")
    public boolean endcor = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify a number of neighbors for topographic measures")
    public int neighbors = 16;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("specify a number of samples for endpoint correlations estimation")
    public int samples = 5000;

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public CurvesMeasure run()
    {
        Curves curves = this.input;
        Record out = measure(curves, this.delta, this.thresh);

        if (this.advanced)
        {
            out.with(this.outlier(curves, this.neighbors));
        }

        if (this.endcor)
        {
            out.with(this.endcor(curves, this.samples));
        }

        this.output = TableSource.createNarrow(out);
        return this;
    }

    public static Record measure(Curves curves, double delta, double thresh)
    {
        Record out = new Record();
        out.with("num_curves", curves.size());
        out.with(lengths(curves));
        out.with(volumetrics(curves, delta, thresh));
        out.with(attributes(curves));

        return out;
    }

    public static Record volumetrics(Curves curves, double delta, double thresh)
    {
        if (curves.size() > 0)
        {
            Box box = curves.bounds().scale(1.25);
            Sampling sampling = SamplingSource.create(box, delta);
            Volume trackDensityMap = VolumeUtils.density(sampling, curves);
            VolumeThreshold module = new VolumeThreshold();
            module.input = trackDensityMap;
            module.threshold = thresh;
            Mask mask = module.run().output;
            double volume = MaskUtils.volume(mask);

            VolumeVoxelStats stats = new VolumeVoxelStats().withInput(trackDensityMap).withMask(mask).run();

            Record out = stats.record("density");
            out.with(Curves.VOLUME, volume);
            return out;
        }
        else
        {
            Record out = new VolumeVoxelStats().record("density");
            out.with(Curves.VOLUME, "NA");
            return out;
        }
    }

    public static Record lengths(Curves curves)
    {
        if (curves.size() > 0)
        {
            return new VectStats().withInput(curves.lengths()).run().record("length");
        }
        else
        {
            return new VectStats().record("length");
        }
    }

    public static Record attributes(Curves curves)
    {
        Record out = new Record();

        int num = curves.size();
        for (String name : curves.names())
        {
            if (curves.dim(name) == 1)
            {
                if (num > 0)
                {
                    Vect vals = new Vect(num);
                    Vect wvals = new Vect(num);

                    VectOnlineStats pointStats = new VectOnlineStats();
                    for (int i = 0; i < num; i++)
                    {
                        double val = 0;
                        double wval = 0;

                        Curves.Curve curve = curves.get(i);
                        double length = curve.length();

                        if (curve.size() > 0)
                        {
                            for (int j = 0; j < curve.size(); j++)
                            {
                                double value = curve.get(name, j).get(0);
                                if (Double.isFinite(value))
                                {
                                    pointStats.update(value);
                                }
                            }

                            double integral = curve.integral(name).get(0);

                            val = MathUtils.zero(length) ? integral : integral / length;
                            wval = integral;
                        }

                        vals.set(i, val);
                        wvals.set(i, wval);
                    }

                    VectStats curvesStats = new VectStats().withInput(vals).run();
                    VectStats curveWeightedStats = new VectStats().withInput(wvals).run();

                    out.with(name + "_mean", curvesStats.mean);
                    out.with(name + "_median", curvesStats.median);
                    out.with(name + "_qlow", curvesStats.qlow);
                    out.with(name + "_qhigh", curvesStats.qhigh);
                    out.with(name + "_var", curvesStats.var);
                    out.with(name + "_std", curvesStats.std);
                    out.with(name + "_mad", curvesStats.mad);
                    out.with(name + "_cv", curvesStats.cv);
                    out.with(name + "_min", curvesStats.min);
                    out.with(name + "_max", curvesStats.max);
                    out.with(name + "_sum", curvesStats.sum);
                    out.with(name + "_twl", curveWeightedStats.sum);
                    out.with(name + "_mwl", curveWeightedStats.mean);
                    out.with(name + "_point_mean", pointStats.mean);
                    out.with(name + "_point_var", pointStats.var);
                    out.with(name + "_point_std", pointStats.std);
                    out.with(name + "_point_min", pointStats.min);
                    out.with(name + "_point_max", pointStats.max);
                    out.with(name + "_point_sum", pointStats.sum);
                }
                else
                {
                    out.with(name + "_mean", "NA");
                    out.with(name + "_median", "NA");
                    out.with(name + "_qlow", "NA");
                    out.with(name + "_qhigh", "NA");
                    out.with(name + "_var", "NA");
                    out.with(name + "_std", "NA");
                    out.with(name + "_max", "NA");
                    out.with(name + "_cv", "NA");
                    out.with(name + "_min", "NA");
                    out.with(name + "_max", "NA");
                    out.with(name + "_sum", "NA");
                    out.with(name + "_twl", "NA");
                    out.with(name + "_mwl", "NA");
                    out.with(name + "_point_mean", "NA");
                    out.with(name + "_point_var", "NA");
                    out.with(name + "_point_std", "NA");
                    out.with(name + "_point_min", "NA");
                    out.with(name + "_point_max", "NA");
                    out.with(name + "_point_sum", "NA");
                }
            }
        }
        return out;
    }

    public static Record knncor(final Curves curves, int k)
    {
        Record out = new Record();

        if (curves.size() > 0)
        {
            int n = curves.size();
            int k2 = k * k;

            final Curves oriented = new CurvesOrient()
            {{
                this.input = curves;
            }}.run().output;

            Vects heads = new CurvesEndpoints(){{ this.input = oriented; this.head = true; }}.run().output;
            Vects tails = new CurvesEndpoints(){{ this.input = oriented; this.tail = true; }}.run().output;

            Matrix distHeads = VectsUtils.dist(heads);
            Matrix distTails = VectsUtils.dist(tails);

            List<Integers> knnHeads = MatrixUtils.knn(distHeads, k);
            List<Integers> knnTails = MatrixUtils.knn(distTails, k);

            Vect corrs = new Vect(n);

            for (int i = 0; i < n; i++)
            {
                Integers nx = knnHeads.get(i);
                Integers ny = knnTails.get(i);

                double[] dxs = new double[k2];
                double[] dys = new double[k2];

                for (int a = 0; a < k; a++)
                {
                    for (int b = 0; b < k; b++)
                    {
                        int idx = b * k + a;

                        dxs[idx] = distHeads.get(nx.get(a), nx.get(b));
                        dys[idx] = distTails.get(ny.get(a), ny.get(b));
                    }
                }

                double corr = Math.abs(new PearsonsCorrelation().correlation(dxs, dys));

                corrs.set(i, corr);
            }

            out.with(new VectStats().withInput(corrs).run().record("topo"));
        }
        else
        {
            out.with(new VectStats().record("topo"));
        }

        return out;
    }

    public static Record outlier(final Curves curves, int k)
    {
        Record out = new Record();

        if (curves.size() > 2)
        {
            int n = curves.size();
            k = Math.min(curves.size() - 1, k);

            final Curves simplified = new CurvesSimplify()
            {{
                this.input = curves;
                this.epsilon = 1.0;
            }}.run().output;

            Distance<Curves.Curve> distFunc = DistanceSource.curveHausdorff();
            Matrix distCurves = CurvesUtils.distance(simplified, distFunc);

            Vect outliers = new Vect(n);
            Vect knn_outliers = new Vect(n);

            for (int i = 0; i < n; i++)
            {
                Vect distCurve = distCurves.getRow(i);
                Vect distCurveKnn = distCurve.sub(new Integers(VectUtils.permutation(distCurve)).sub(1, k + 1));

                double outlier = distCurveKnn.get(0);
                double knn_outlier = distCurveKnn.mean();

                outliers.set(i, outlier);
                knn_outliers.set(i, knn_outlier);
            }

            VectStats outlierStats = new VectStats().withInput(outliers).run();
            VectStats outlierKnnStats = new VectStats().withInput(outliers).run();

            int count = outliers.minus(outlierStats.median).times(1.0 / outlierStats.iqr).abs().above(1.5).size();
            double percent = count / (double) n;

            out.with(outlierStats.record("outlier"));
            out.with(outlierKnnStats.record("knn_outlier"));
            out.with("outlier_count", count);
            out.with("outlier_percent", percent);
        }
        else
        {
            out.with(new VectStats().record("outlier"));
            out.with(new VectStats().record("knn_outlier"));
            out.with("outlier_count", "NA");
        }

        return out;
    }

    public static Record endcor(final Curves curves, int k)
    {
        Curves mycurves = new CurvesOrient(){{
            this.input = curves;
        }}.run().output;

        double sx = 0.0;
        double sy = 0.0;
        double sxx = 0.0;
        double syy = 0.0;
        double sxy = 0.0;
        int n = 0;

        for (int i = 0; i < k; i++)
        {
            int aidx = Global.RANDOM.nextInt(mycurves.size());
            Curves.Curve acurve = mycurves.get(aidx);

            for (int j = i + 1; j < k; j++)
            {
                int bidx = Global.RANDOM.nextInt(mycurves.size());
                Curves.Curve bcurve = mycurves.get(bidx);

                double x = acurve.getHead().dist(bcurve.getHead());
                double y = acurve.getTail().dist(bcurve.getTail());

                sx += x;
                sy += y;
                sxx += x * x;
                syy += y * y;
                sxy += x * y;
                n += 1;
            }
        }

        double cov = sxy / n - sx * sy / n / n;
        double sigmax = Math.sqrt(sxx / n -  sx * sx / n / n);
        double sigmay = Math.sqrt(syy / n -  sy * sy / n / n);

        double cor = cov / sigmax / sigmay;

        Record out = new Record();
        out.with("endcor", cor);

        return out;
    }


    public static Table apply(Curves curves)
    {
        return new CurvesMeasure()
        {{
            this.input = curves;
        }}.run().output;
    }
}
