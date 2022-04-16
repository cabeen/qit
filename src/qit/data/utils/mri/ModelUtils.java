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


package qit.data.utils.mri;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import numerics.BesselFunctions;
import qit.base.Global;
import qit.base.Logging;
import qit.base.ModelType;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.models.BiTensor;
import qit.data.models.ExpDecay;
import qit.data.models.Mcsmt;
import qit.data.models.VectModel;
import qit.data.modules.vects.VectsHull;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MeshUtils;
import qit.data.utils.vects.stats.VectStats;
import qit.base.Model;
import qit.data.models.Fibers;
import qit.data.utils.mri.structs.Gradients;
import qit.data.models.Kurtosis;
import qit.data.utils.mri.structs.ModelEstimator;
import qit.data.models.Noddi;
import qit.data.models.Spharm;
import qit.data.models.Tensor;
import qit.data.utils.mri.estimation.FibersEstimator;
import qit.data.utils.mri.estimation.NoddiEstimator;
import qit.data.utils.mri.estimation.TensorEstimator;
import qit.math.source.VectFunctionSource;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.VectFunction;
import qit.math.structs.HalfEdgePoly;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.Map;
import java.util.Set;

public class ModelUtils
{
    // rician sigma = this * mean(noisy samples)
    public static final double RICIAN_MEAN = Math.sqrt(2.0 / Math.PI); // 0.80

    // rician sigma = this * std(noisy samples)
    public static final double RICIAN_STD = Math.sqrt(2.0 /(4.0 - Math.PI)); // 1.53

    public static double MIN_SNR = 0.02;

    public static ModelType select(String s)
    {
        return select(null, s);
    }

    public static ModelType select(ModelType m, ModelType s)
    {
        if (s != null && !ModelType.Vect.equals(s))
        {
            return s;
        }

        if (m != null)
        {
            return m;
        }

        return ModelType.Vect;
    }

    public static ModelType select(ModelType m, String s)
    {
        if (s != null)
        {
            if (Tensor.matches(s) || ModelType.Tensor.name().equals(s))
            {
                return ModelType.Tensor;
            }
            else if (Fibers.matches(s) || ModelType.BiTensor.name().equals(s))
            {
                return ModelType.BiTensor;
            }
            else if (Fibers.matches(s) || ModelType.Fibers.name().equals(s))
            {
                return ModelType.Fibers;
            }
            else if (Spharm.matches(s) || ModelType.Spharm.name().equals(s))
            {
                return ModelType.Spharm;
            }
            else if (Kurtosis.matches(s) || ModelType.Kurtosis.name().equals(s))
            {
                return ModelType.Kurtosis;
            }
            else if (Noddi.matches(s)|| ModelType.Noddi.name().equals(s))
            {
                return ModelType.Noddi;
            }
            else if (ExpDecay.matches(s)|| ModelType.ExpDecay.name().equals(s))
            {
                return ModelType.ExpDecay;
            }
            else if (Mcsmt.matches(s)|| ModelType.Mcsmt.name().equals(s))
            {
                return ModelType.Mcsmt;
            }
        }

        if (m != null)
        {
            return m;
        }

        return ModelType.Vect;
    }

    public static Model proto(ModelType type, String s, int dim)
    {
        return proto(select(type, s), dim);
    }

    public static Model proto(String s, int dim)
    {
        return proto(select(null, s), dim);
    }

    public static Model proto(ModelType m, int dim)
    {
        if (ModelType.Tensor.equals(m))
        {
            return new Tensor();
        }
        else if (ModelType.BiTensor.equals(m))
        {
            return new BiTensor();
        }
        else if (ModelType.Fibers.equals(m))
        {
            return new Fibers(Fibers.count(dim));
        }
        else if (ModelType.Noddi.equals(m))
        {
            return new Noddi();
        }
        else if (ModelType.Kurtosis.equals(m))
        {
            return new Kurtosis();
        }
        else if (ModelType.Spharm.equals(m))
        {
            return new Spharm(dim);
        }
        else if (ModelType.Mcsmt.equals(m))
        {
            return new Mcsmt();
        }
        else
        {
            return new VectModel(dim);
        }
    }

    public static ModelEstimator estimator(ModelType m)
    {
        if (ModelType.Tensor.equals(m))
        {
            return new TensorEstimator();
        }
        else if (ModelType.Fibers.equals(m))
        {
            return new FibersEstimator();
        }
        else if (ModelType.Noddi.equals(m))
        {
            return new NoddiEstimator();
        }
        else
        {
            throw new RuntimeException("unsupported model: " + m);
        }
    }

    public static <E extends Model<E>> VectFunction feature(final E model, final String name)
    {
        final int dim = model.feature(name).size();
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                model.setEncoding(input);
                output.set(0, model.feature(name));
            }
        }.init(model.getEncodingSize(), dim);
    }

    public static VectFunction synth(ModelType model, int dim, Gradients gradients)
    {
        if (ModelType.Tensor.equals(model))
        {
            return Tensor.synth(gradients);
        }
        else if (ModelType.BiTensor.equals(model))
        {
            return BiTensor.synth(gradients);
        }
        else if (ModelType.Noddi.equals(model))
        {
            return Noddi.synth(gradients);
        }
        else if (ModelType.Kurtosis.equals(model))
        {
            return Kurtosis.synth(gradients);
        }
        else if (ModelType.Fibers.equals(model))
        {
            return Fibers.synther(Fibers.count(dim), gradients);
        }
        else
        {
            throw new RuntimeException("invalid model type: " + model);
        }
    }

    public static double cost(CostType cost, Gradients gradients, Vect meas, Vect pred)
    {
        switch(cost)
        {
            case SE:
                return se(meas, pred);
            case MSE:
                return mse(meas, pred);
            case RMSE:
                return rmse(meas, pred);
            case NRMSE:
                return nrmse(gradients, meas, pred);
            case CHISQ:
                return chisq(meas, pred, estimateSigmaGaussian(gradients, meas));
            case RLL:
                return rll(meas, pred, estimateSigmaRician(gradients, meas));
            default:
                throw new RuntimeException("unsupported cost: " + cost.toString());
        }
    }

    public static double nrmse(Gradients gradients, Vect meas, Vect pred)
    {
        // Return the root mean square error normalized by the baseline signal

        // meas is the measurement
        // pred is model prediction

        // returns the root mean square error

        double baseline = baselineStats(gradients, meas).mean;
        baseline = MathUtils.zero(baseline) ? 1.0 : baseline;
        return meas.minus(pred).norm() / baseline / (double) meas.size();
    }

    public static double rmse(Vect meas, Vect pred)
    {
        // Return the root mean square error

        // meas is the measurement
        // pred is model prediction

        // returns the root mean square error

        return meas.minus(pred).norm() / (double) meas.size();
    }

    public static double mse(Vect meas, Vect pred)
    {
        // Return the root mean square error

        // meas is the measurement
        // pred is model prediction

        // returns the mean square error

        return meas.minus(pred).norm2() / (double) meas.size();
    }

    public static double nmad(Gradients gradients, Vect meas, Vect pred)
    {
        // Return the root mean square error

        // meas is the measurement
        // pred is model prediction

        // returns the normalized median absolute deviation

        double baseline = baselineStats(gradients, meas).median;
        baseline = MathUtils.zero(baseline) ? 1.0 : baseline;

        return meas.minus(pred).abs().median() / baseline;
    }

    public static double mad(Vect meas, Vect pred)
    {
        // Return the root mean square error

        // meas is the measurement
        // pred is model prediction

        // returns the median absolute deviation

        return meas.minus(pred).abs().median();
    }

    public static double se(Vect meas, Vect pred)
    {
        // Return the root mean square error

        // meas is the measurement
        // pred is model prediction

        // returns the square error

        return meas.minus(pred).norm2();
    }

    public static double chisq(Vect meas, Vect pred, double sigma)
    {
        // Return the chi-squared error

        // meas is the measurement
        // pred is model prediction
        // sigma the standard deviation of the Gaussian noise distribution

        // returns the chi-square statistic

        return meas.minus(pred).sq().sum() / (sigma * sigma);
    }

    public static double rchisq(Vect meas, Vect pred, double sigma)
    {
        // Return the approximate Rician chi-squared error

        // meas is the measurement
        // pred is model prediction
        // sigma the standard deviation of the Gaussian distributions underlying the Rician distribution.

        // returns the chi-square statistic

        double s2 = sigma * sigma;

        // this term is an approximation to the Rician noise bias
        Vect offset = pred.sq().plus(s2).sqrt();

        return meas.minus(offset).sq().sum() / s2;
    }

    public static double rllfull(Vect meas, Vect pred, double sigma)
    {
        // Rician Log likelihood
        // After Daniel C Alexander's implementation in RicianLogLik.m

        // Computes the log likelihood of the measurements given the model pred
        // for the Rician noise model.

        // meas is the measurement
        // pred is model prediction
        // sigma the standard deviation of the Gaussian distributions underlying the Rician distribution.

        // returns the log-likelihood given a Rician noise model

        double loglik = 0;

        for (int i = 0; i < meas.size(); i++)
        {
            double sig2 = sigma * sigma;
            double sig2i = 1.0 / sig2;
            double preds = pred.get(i);
            double logBessI0 = logbesselI0(preds * meas.get(i) * sig2i);

            // Constant terms excluded for a slight speed gain.
            loglik += 0.5 * sig2i * preds * preds - logBessI0;

            Global.assume(!Double.isNaN(loglik), "invalid loglik");
        }

        return loglik;
    }

    public static double rll(Vect meas, Vect pred, double sigma)
    {
        // Rician Log likelihood
        // After Ferizi et al NIMG MRM 2014

        double loglik = 0;
        double s2 = sigma * sigma;
        int n = meas.size();

        for (int i = 0; i < n; i++)
        {
            double m = meas.get(i);
            double p = pred.get(i);

            double d = m - Math.sqrt(p * p + s2);
            double d2 = d * d;

            loglik += d2;
        }

        loglik /= 2.0 * s2;
        loglik -= n * Math.log(sigma * Math.sqrt(2 * Math.PI));

        return loglik;
    }

    public static double estimateSigmaGaussian(Gradients gradients, Vect signal)
    {
        // After Gary Hui Zhang's per-voxel implementation in EstimateSignal.m

        return baselineStats(gradients, signal).std;
    }

    public static double estimateSigmaRician(Gradients gradients, Vect signal)
    {
        // After Gary Hui Zhang's per-voxel implementation in EstimateSignal.m

        VectStats baselines = baselineStats(gradients, signal);

        double sigma = baselines.std;
        double sigmaMin = MIN_SNR * baselines.mean;
        return RICIAN_STD * (sigma < sigmaMin ? MIN_SNR : sigma);
    }

    public static Vect baselines(Gradients gradients, Vect signal)
    {
        Vect baselines = VectSource.createND(gradients.getNumBaselines());
        int idx = 0;
        for (int which : gradients.getBaselineIdx())
        {
            baselines.set(idx, signal.get(which));
            idx += 1;
        }

        return baselines;
    }

    public static VectStats baselineStats(Gradients gradients, Vect signal)
    {
        Vect baselines = VectSource.createND(gradients.getNumBaselines());
        int idx = 0;
        for (int which : gradients.getBaselineIdx())
        {
            baselines.set(idx, signal.get(which));
            idx += 1;
        }

        return new VectStats().withInput(baselines).run();
    }

    public static Vect logbesselI0(Vect x)
    {
        Vect out = x.proto();
        for (int i = 0; i < x.size(); i++)
        {
            out.set(i, logbesselI0(x.get(i)));
        }
        return out;
    }

    public static double logbesselI0(double x)
    {
        // After Daniel C Alexander's implementation in logbesseli0.m

        // Computes log(besseli(0,x)) robustly.  Computing it directly cause
        // numerical problems at high x, but the function has asymptotic linear
        // behaviour, which we approximate here for high x.

        // For very large arguments to besselI0, we approximate log besseli using a
        // linear model of the asymptotic behaviour.
        // The linear parameters come from this command:
        // app=regress(log(besseli(0,500:700))',[ones(201,1) (500:700)']);
        // app = [-3.61178295877576 0.99916157999904];

        if (x < 700)
        {
            // exact value
            return Math.log(BesselFunctions.besselI0(x));
        }
        else
        {
            // approximate value

            return x - Math.log(2 * Math.PI * x) / 2;
        }
    }

    public static Gradients generate(int zeros, int num, double bval)
    {
        Logging.info("started generating uniformly spaced gradients");

        // initialize
        Vects bvecs = new Vects();
        Vects bvals = new Vects();

        for (int i = 0; i < zeros; i++)
        {
            bvecs.add(VectSource.create3D());
            bvals.add(VectSource.create1D(0));
        }

        for (int i = 0; i < num; i++)
        {
            // let's assume there are no duplicates by chance...
            bvecs.add(VectSource.randomUnit());
            bvals.add(VectSource.create1D(bval));
        }

        HalfEdgePoly graph = triangulate(new Gradients(bvecs, bvals)).graph;

        for (int i = 0; i < 10000; i++)
        {
            Logging.info("started iteration " + i);
            for (Vertex v : graph.verts())
            {
                int idx = v.id();
                Vect vdir = bvecs.get(idx % num);
                Matrix outer = MatrixSource.dyadic(vdir);

                for (Vertex n : graph.vertRing(v))
                {
                    Vect ndir = bvecs.get(n.id() % num);
                    outer.plusEquals(MatrixSource.dyadic(ndir));
                }

                Vertex vo = new Vertex((idx + num) % (2 * num));
                for (Vertex n : graph.vertRing(vo))
                {
                    Vect ndir = bvecs.get(n.id() % num);
                    outer.plusEquals(MatrixSource.dyadic(ndir));
                }

                Vect nvdir = MatrixUtils.eig(outer).vectors.get(0);
                bvecs.set(idx % num, nvdir);
            }
        }

        Logging.info("finished generating uniformly spaced gradients");

        return new Gradients(bvecs, bvals);
    }

    public static Mesh triangulate(Gradients gradients)
    {
        return triangulate(gradients.getBvecs());
    }

    public static Mesh triangulate(Vects bvecs)
    {
        // note: use vertex index mod N to map from mesh vertices to signal
        // vector index

        Logging.info("started meshing gradients");

        int num = bvecs.size();
        Map<Vertex, Vect> coords = Maps.newHashMap();
        for (int i = 0; i < num; i++)
        {
            Vect g = bvecs.get(i);

            if (MathUtils.zero(g.norm()))
            {
                continue;
            }

            Vect ng = g.normalize();
            coords.put(new Vertex(i), ng);
            coords.put(new Vertex(num + i), ng.times(-1.0));
        }

        Mesh mesh = VectsHull.hull(coords);

        for (Vertex v : coords.keySet())
        {
            if (mesh.graph.has(v))
            {
                continue;
            }

            Logging.info("adding lost vertex");

            Vect c = coords.get(v);
            Face nearest = MeshUtils.closestFace(mesh, Mesh.COORD, mesh.graph.faces(), c);

            // find boundary edges
            Set<Edge> bound = Sets.newHashSet();
            for (Edge e : mesh.graph.edges(nearest))
            {
                if (!nearest.equals(mesh.graph.opposite(nearest, e)))
                {
                    bound.add(e);
                }
            }

            // generate the new faces
            Set<Face> newfaces = Sets.newHashSet();
            for (Edge e : bound)
            {
                newfaces.add(new Face(e.getA(), e.getB(), v));
            }
        }

        Logging.info("finished meshing gradients");

        return mesh;
    }

    public static VectFunction noiseND(int dim, double sigma, boolean rician)
    {
        if (rician)
        {
            return ModelUtils.ricianND(dim, sigma);
        }
        else
        {
            return VectFunctionSource.gaussianND(dim, sigma);
        }
    }

    public static VectFunction ricianND(final int dim, final double sigma)
    {
        // create a function to generate rician noise with a given sigma (of underlying gaussian)
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(CaminoUtils.sampleRician(input, sigma));
            }
        }.init(dim, dim);
    }

    public static double snrdb(double s0, double sigma)
    {
        // compute the signal to noise ratio in decibels

        Global.assume(s0 > 0, "expected positive baseline signal: " + s0);
        Global.assume(sigma > 0, "expected positive noise sigma: " + sigma);

        return 20.0 * Math.log10(s0 / sigma);
    }

    public static double std(double s0, double snrdb)
    {
        // convert the SNR in decibels to a noise standard deviation

        Global.assume(s0 > 0, "expected positive baseline signal");
        double sigma = s0 / Math.exp(snrdb * (Math.log(10.0) / 20.0));

        return sigma;
    }

    public static double bias(double signal, double noise)
    {
        double snr = signal / Math.sqrt(noise);
        double snrsq = snr * snr;

        double c1 = 2.0 + snrsq;
        double c2 = -0.125 * Math.PI * Math.exp(-0.5 * snrsq) * (2.0 + snrsq) * (2.0 + snrsq) * BesselFunctions.besselI0(0.25 * snrsq);
        double c3 = snrsq * BesselFunctions.besselI1(0.25 * snrsq);
        double corr = c1 + c2 + c3;
        corr = corr < 0.001 || corr > 10.0 ? 1.0 : corr;

        double bias = 2.0 * noise / corr;

        return bias;
    }
}
