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

import data.DataSynthesizer;
import imaging.B_VectorScheme;
import imaging.DW_Scheme;
import numerics.BesselFunctions;
import numerics.ConvergenceException;
import numerics.NewtonRaphsonSolver;
import numerics.Vector3D;
import numerics.WatsonD;
import numerics.WatsonDistribution;
import qit.base.Global;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.mri.structs.Gradients;
import qit.math.utils.MathUtils;

public class CaminoUtils
{
    public static DW_Scheme scheme(Gradients gradients)
    {
        int num = gradients.size();

        double[] bvals = new double[num];
        double[][] bvecs = new double[num][3];

        for (int i = 0; i < num; i++)
        {
            double bval = gradients.getBval(i);
            Vect bvec = gradients.getBvec(i);

            boolean zero = MathUtils.zero(bval);
            double bx = zero ? 0 : bvec.get(0);
            double by = zero ? 0 : bvec.get(1);
            double bz = zero ? 0 : bvec.get(2);

            bvals[i] = bval;
            bvecs[i][0] = bx;
            bvecs[i][1] = by;
            bvecs[i][2] = bz;
        }

        B_VectorScheme out = new B_VectorScheme(bvecs, bvals);

        return out;
    }

    public static Vect sampleWatson(Vect mu, double kappa)
    {
        return create(WatsonDistribution.nextVector(new Vector3D(mu.toArray()), kappa, Global.RANDOM));
    }

    public static double densityWatson(Vect input, Vect mu, double kappa)
    {
        double density = WatsonDistribution.pdf(new Vector3D(mu.toArray()), new Vector3D(input.toArray()), kappa);
        // camino density is not normalized
        return density / (4.0 * Math.PI);
    }

    public static double nllWatson(Vect input, Vect mu, double kappa)
    {
        double logpdf = WatsonDistribution.logPDF(new Vector3D(mu.toArray()), new Vector3D(input.toArray()), kappa);
        // note: this excludes the constant term log(4*pi)
        return -1 * logpdf;
    }

    public static Double kappaWatson(double lamb)
    {
        try
        {
            // try camino's lambda solver
            if (lamb > 0 && !MathUtils.zero(lamb))
            {
                double kappa = NewtonRaphsonSolver.solve(new WatsonD(), lamb, 0.0, 0.00001);
                return kappa;
            }
        }
        catch (ConvergenceException e)
        {
        }
        return null;
    }

    public static Double lambdaWatson(double kappa)
    {
        return new WatsonD().evaluate(kappa);
    }

    public static double besselI0(double phi)
    {
        return BesselFunctions.besselI0(phi);
    }

    public static Vect create(Vector3D vect)
    {
        Vect out = VectSource.create3D();
        out.set(0, vect.x);
        out.set(1, vect.y);
        out.set(2, vect.z);
        return out;
    }

    public static double sampleRician(double input, double sigma)
    {
        return DataSynthesizer.addNoise(new double[]{input}, sigma, Global.RANDOM)[0];
    }

    public static double[] sampleRician(Vect input, double sigma)
    {
        return DataSynthesizer.addNoise(input.copy().toArray(), sigma, Global.RANDOM);
    }

    public static Table precompute1F1(double mink, double maxk, int steps)
    {
        Table out = new Table();
        out.withField("kappa");
        out.withField("value");

        double delta = maxk - mink;
        for (int i = 0; i < steps; i++)
        {
            double k = mink + i * delta / (steps - 1);

            double v = 0;

            if (k < 700)
            {
                v = WatsonDistribution.hyper1F1(0.5D, 1.5D, k, 1.0E-9D);
            }
            else
            {
                v = Math.exp(WatsonDistribution.logHyper1F1(0.5D, 1.5D, k, 1.0E-9D));
            }

            Record row = new Record();
            row.with("kappa", String.valueOf(k));
            row.with("value", String.valueOf(v));

            out.addRecord(row);
        }

        return out;
    }
}
