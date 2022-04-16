/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.utils.mri.estimation;

import qit.base.Global;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.MatrixSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MatrixUtils.EigenDecomp;
import qit.data.utils.mri.CaminoUtils;
import qit.data.utils.mri.structs.ModelEstimator;
import qit.data.models.Noddi;
import qit.math.utils.MathUtils;

import java.util.List;

public class NoddiEstimator extends ModelEstimator
{
    public static final String COMPONENT = "Component";
    public static final String RANK_ONE = "RankOne";
    public static final String SCATTER = "Scatter";
    public static final String LOG_SCATTER = "LogScatter";
    public static final String SCATTER_SPLINE_L0 = "ScatterSplineL0";
    public static final String SCATTER_SPLINE_C0 = "ScatterSplineC0";
    public static final String SCATTER_SPLINE_C1 = "ScatterSplineC1";
    public static final String SCATTER_SPLINE_C2 = "ScatterSplineC2";
    public static final String SCATTER_SPLINE_S0 = "ScatterSplineS0";
    public static final String SCATTER_SPLINE_S1 = "ScatterSplineS1";
    public static final String SCATTER_SPLINE_S2 = "ScatterSplineS2";

    public static final String DEFAULT_ESTIMATION = COMPONENT;

    public static final String[] METHODS = {
            COMPONENT, RANK_ONE, SCATTER, LOG_SCATTER, SCATTER_SPLINE_L0,
            SCATTER_SPLINE_C0, SCATTER_SPLINE_C1, SCATTER_SPLINE_C2,
            SCATTER_SPLINE_S0, SCATTER_SPLINE_S1, SCATTER_SPLINE_S2
    };

    public String estimation = SCATTER_SPLINE_C2;
    public double beta = 0.05;
    public boolean weightICVF = false;
    public boolean weightISOVF = false;

    public NoddiEstimator parse(String method)
    {
        if (method.startsWith("Weighted"))
        {
            this.weightICVF = true;
            this.weightISOVF = true;
            this.estimation = method.replace("Weighted", "");
        }
        else
        {
            this.estimation = method;
        }

        return this;
    }

    @Override
    public Vect run(List<Double> weights, List<Vect> input)
    {
        switch (this.estimation)
        {
            case COMPONENT:
                return this.component(weights, input);
            case RANK_ONE:
                return this.rankone(weights, input);
            default:
                return this.scatter(this.estimation, weights, input);
        }
    }

    private Vect component(List<Double> weights, List<Vect> input)
    {
        Global.assume(weights.size() == input.size(), "invalid weights");

        double weightSum = 0;
        double odiSum = 0;
        Matrix matrixSum = new Matrix(3, 3);
        double icvfSum = 0;
        double isovfSum = 0;
        double baselineSum = 0;

        for (int i = 0; i < weights.size(); i++)
        {
            Noddi noddi = new Noddi(input.get(i));

            double baseline = noddi.getBaseline();
            double isovf = noddi.getFISO();
            double icvf = noddi.getFICVF();
            double odi = noddi.getODI();
            double weight = weights.get(i);

            if (Double.isNaN(odi))
            {
                odi = 0.0;
            }

            if (this.weightICVF)
            {
                weight *= icvf;
            }

            if (this.weightISOVF)
            {
                weight *= (1.0 - isovf);
            }

            Vect dir = noddi.getDir();
            Matrix matrix = MatrixSource.outer(dir, dir);

            weightSum += weight;
            odiSum += weight * odi;
            matrixSum.plusEquals(weight, matrix);
            icvfSum += weight * icvf;
            isovfSum += weight * isovf;
            baselineSum += weight * baseline;
        }

        if (MathUtils.zero(weightSum))
        {
            return new Noddi().getEncoding();
        }

        double norm = 1.0 / weightSum;
        double icvf = icvfSum * norm;
        double isovf = isovfSum * norm;
        double odi = odiSum * norm;
        double baseline = baselineSum * norm;
        Matrix matrix = matrixSum.times(norm);
        Vect dir = MatrixUtils.eig(matrix).vectors.get(0);

        Noddi noddi = new Noddi();
        noddi.setFICVF(icvf);
        noddi.setFISO(isovf);
        noddi.setODI(odi);
        noddi.setDir(dir);
        noddi.setBaseline(baseline);

        return noddi.getEncoding();
    }

    private Vect rankone(List<Double> weights, List<Vect> input)
    {
        Global.assume(weights.size() == input.size(), "invalid weights");

        double weightSum = 0;
        Matrix matrixSum = new Matrix(3, 3);
        double icvfSum = 0;
        double isovfSum = 0;
        double baselineSum = 0;

        for (int i = 0; i < weights.size(); i++)
        {
            Noddi noddi = new Noddi(input.get(i));

            double baseline = noddi.getBaseline();
            Vect dir = noddi.getDir();
            double isovf = noddi.getFISO();
            double icvf = noddi.getFICVF();
            double kappa = noddi.getKappa();

            Matrix mat = MatrixSource.outer(dir, dir).times(kappa);

            double weight = weights.get(i);

            if (this.weightICVF)
            {
                weight *= icvf;
            }

            if (this.weightISOVF)
            {
                weight *= (1.0 - isovf);
            }

            weightSum += weight;
            matrixSum.plusEquals(weight, mat);
            icvfSum += weight * icvf;
            isovfSum += weight * isovf;
            baselineSum += weight * baseline;
        }

        if (MathUtils.zero(weightSum))
        {
            return new Noddi().getEncoding();
        }

        double norm = 1.0 / weightSum;
        double icvf = icvfSum * norm;
        double isovf = isovfSum * norm;
        double baseline = baselineSum * norm;
        EigenDecomp eig = MatrixUtils.eig(matrixSum.times(norm));

        Vect dir = eig.vectors.get(0);
        double kappa = eig.values.get(0);

        Noddi noddi = new Noddi();
        noddi.setFICVF(icvf);
        noddi.setFISO(isovf);
        noddi.setKappa(kappa);
        noddi.setDir(dir);
        noddi.setBaseline(baseline);

        return noddi.getEncoding();
    }

    private Vect scatter(String method, List<Double> weights, List<Vect> input)
    {
        Global.assume(weights.size() == input.size(), "invalid weights");

        double weightSum = 0;
        double icvfSum = 0;
        double isovfSum = 0;
        Matrix scatterSum = new Matrix(3, 3);
        double baselineSum = 0;

        for (int i = 0; i < weights.size(); i++)
        {
            Noddi noddi = new Noddi(input.get(i));

            double baseline = noddi.getBaseline();
            double isovf = noddi.getFISO();
            double icvf = noddi.getFICVF();
            Matrix scatter = noddi.getScatter(LOG_SCATTER.equals(method));

            double weight = weights.get(i);

            if (this.weightICVF)
            {
                weight *= icvf;
            }

            if (this.weightISOVF)
            {
                weight *= (1.0 - isovf);
            }

            weightSum += weight;
            icvfSum += weight * icvf;
            isovfSum += weight * isovf;
            baselineSum += weight * baseline;
            scatterSum.plusEquals(weight, scatter);
        }

        if (MathUtils.zero(weightSum))
        {
            return new Noddi().getEncoding();
        }

        double norm = 1.0 / weightSum;
        double icvf = icvfSum * norm;
        double isovf = isovfSum * norm;
        double baseline = baselineSum * norm;
        Matrix scatter = scatterSum.times(norm);

        Noddi noddi = new Noddi();
        noddi.setFICVF(icvf);
        noddi.setFISO(isovf);
        noddi.setBaseline(baseline);

        EigenDecomp eig = MatrixUtils.eig(scatter);

        Vect vec1 = eig.vectors.get(0);
        double val1 = eig.values.get(0);
        double val2 = eig.values.get(1);
        double val3 = eig.values.get(2);

        if (LOG_SCATTER.equals(method))
        {
            val1 = Math.exp(val1);
            val2 = Math.exp(val2);
            val3 = Math.exp(val3);
        }

        double val12 = val1 - val2;
        double val23 = val2 - val3;
        double ratio = val12 / val23 / this.beta;

        if (MathUtils.zero(val12))
        {
            noddi.setDir(vec1);
            noddi.setKappa(0.0);
        }
        else if (ratio > 1.0)
        {
            // use bipolar
            Vect mu = vec1;

            noddi.setDir(mu);
            Double kappa = CaminoUtils.kappaWatson(val1);

            if (kappa != null)
            {
                noddi.setKappa(kappa);
            }
        }
        else
        {
            noddi.setDir(vec1);
            Double kappa = null;

            switch (method)
            {
                case SCATTER:
                {
                    kappa = CaminoUtils.kappaWatson(val1);
                }
                break;
                case LOG_SCATTER:
                {
                    kappa = CaminoUtils.kappaWatson(val1);
                }
                break;
                case SCATTER_SPLINE_L0:
                {
                    double alpha = 1 - ratio;
                    double valMean = (val1 + val2 + val3) / 3.0;
                    double valBlend = (alpha) * valMean + (1 - alpha) * val1;
                    kappa = CaminoUtils.kappaWatson(valBlend);
                }
                break;
                case SCATTER_SPLINE_C0:
                {
                    double e = 1 - ratio;
                    double r = e;
                    double r2 = r * r;
                    double r3 = r2 * r;
                    double alpha = 3 * r2 - 2 * r3;
                    double valMean = (val1 + val2 + val3) / 3.0;
                    double valBlend = (alpha) * valMean + (1 - alpha) * val1;
                    kappa = CaminoUtils.kappaWatson(valBlend);
                }
                break;
                case SCATTER_SPLINE_C1:
                {
                    double e = 1 - ratio;
                    double r = e * e;
                    double r2 = r * r;
                    double r3 = r2 * r;
                    double alpha = 3 * r2 - 2 * r3;
                    double valMean = (val1 + val2 + val3) / 3.0;
                    double valBlend = (alpha) * valMean + (1 - alpha) * val1;
                    kappa = CaminoUtils.kappaWatson(valBlend);
                }
                break;
                case SCATTER_SPLINE_C2:
                {
                    double e = 1 - ratio;
                    double r = e * e * e;
                    double r2 = r * r;
                    double r3 = r2 * r;
                    double alpha = 3 * r2 - 2 * r3;
                    double valMean = (val1 + val2 + val3) / 3.0;
                    double valBlend = (alpha) * valMean + (1 - alpha) * val1;
                    kappa = CaminoUtils.kappaWatson(valBlend);
                }
                break;
                case SCATTER_SPLINE_S0:
                {
                    double e = 1 - ratio;
                    double r = e;
                    double alpha = (2.0 / Math.PI) * Math.asin(r);
                    double valMean = (val1 + val2 + val3) / 3.0;
                    double valBlend = (alpha) * valMean + (1 - alpha) * val1;
                    kappa = CaminoUtils.kappaWatson(valBlend);
                }
                break;
                case SCATTER_SPLINE_S1:
                {
                    double e = 1 - ratio;
                    double r = e * e;
                    double alpha = (2.0 / Math.PI) * Math.asin(r);
                    double valMean = (val1 + val2 + val3) / 3.0;
                    double valBlend = (alpha) * valMean + (1 - alpha) * val1;
                    kappa = CaminoUtils.kappaWatson(valBlend);
                }
                break;
                case SCATTER_SPLINE_S2:
                {
                    double e = 1 - ratio;
                    double r = e * e * e;
                    double alpha = (2.0 / Math.PI) * Math.asin(r);
                    double valMean = (val1 + val2 + val3) / 3.0;
                    double valBlend = (alpha) * valMean + (1 - alpha) * val1;
                    kappa = CaminoUtils.kappaWatson(valBlend);
                }
                break;
                default:
                {
                    throw new RuntimeException("invalid estimation type: " + method);
                }
            }

            if (kappa != null)
            {
                noddi.setKappa(kappa);
            }
        }

        return noddi.getEncoding();
    }
}