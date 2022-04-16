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

package qit.math.source;

import qit.base.Global;
import qit.base.cli.CliUtils;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.math.structs.Box;
import qit.math.structs.Distance;
import qit.math.structs.Interval;
import qit.math.utils.MathUtils;

public final class DistanceSource
{
    public static String DEFAULT_CURVE = "meanhaus";
    public static double DEFAULT_CUTOFF = 0.475;

    public static Distance<Curve> curve(String name)
    {
        String lower = name.toLowerCase();

        Distance<Curve> out = null;
        if (lower.contains("haus"))
        {
            out = DistanceSource.curveHausdorff();
        }
        else if (lower.contains("cham"))
        {
            out = DistanceSource.curveChamfer();
        }
        else if (lower.contains("end"))
        {
            out = DistanceSource.curveEndpoints();
        }
        else if (lower.contains("cutoff"))
        {
            double cutoff = Double.valueOf(CliUtils.args(lower, String.valueOf(DEFAULT_CUTOFF)));
            out = DistanceSource.curveCutoff(cutoff);
        }

        Global.assume(out != null, "unknown distance: " + name);

        if (lower.contains("mean"))
        {
            out = DistanceSource.symMean(out);
        }
        else if (lower.contains("min"))
        {
            out = DistanceSource.symMin(out);
        }
        else if (lower.contains("max"))
        {
            out = DistanceSource.symMax(out);
        }

        return out;
    }

    public static <E> Distance<E> symMean(final Distance<E> d)
    {
        return new Distance<E>()
        {
            public double dist(E left, E right)
            {
                return (d.dist(left, right) + d.dist(right, left)) / 2;
            }
        };
    }

    public static <E> Distance<E> symMin(final Distance<E> d)
    {
        return new Distance<E>()
        {
            public double dist(E left, E right)
            {
                return Math.min(d.dist(left, right), d.dist(right, left));
            }
        };
    }

    public static <E> Distance<E> symMax(final Distance<E> d)
    {
        return new Distance<E>()
        {
            public double dist(E left, E right)
            {
                return Math.max(d.dist(left, right), d.dist(right, left));
            }
        };
    }

    public static Distance<Vect> euclid()
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                return left.dist(right);
            }
        };
    }

    public static Distance<Vect> euclid2()
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                return left.dist2(right);
            }
        };
    }

    public static Distance<Vect> angleRad()
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                return left.angleRad(right);
            }
        };
    }

    public static Distance<Vect> angleDegrees()
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                return left.angleDeg(right);
            }
        };
    }

    public static Distance<Vect> angleCot()
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                return left.angleCot(right);
            }
        };
    }

    public static Distance<Vect> angleLineRad()
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                return left.angleLineDeg(right);
            }
        };
    }

    public static Distance<Vect> linearKernel()
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                double v = left.dot(right);
                return v;
            }
        };
    }

    public static Distance<Vect> polyKernel(final double gamma, final double r, final double d)
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                return Math.pow(gamma * left.dot(right) + r, d);
            }
        };
    }

    public static Distance<Vect> rbfKernel(final double gamma)
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                double d2 = left.dist2(right);
                return Math.exp(-gamma * d2);
            }
        };
    }

    public static Distance<Vect> mah(Matrix cov)
    {
        final int dim = cov.cols();
        if (cov.rows() != dim)
        {
            throw new RuntimeException("covariance matrix is not square");
        }

        final Matrix invcov = cov.inv();

        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {

                Vect d = left.minus(right);
                Vect md = invcov.times(d);
                return Math.sqrt(d.dot(md));
            }
        };
    }

    public static Distance<Vect> quad(final Matrix mat)
    {
        return new Distance<Vect>()
        {
            public double dist(Vect left, Vect right)
            {
                Vect d = left.minus(right);
                Vect md = mat.times(d);
                return d.dot(md);
            }
        };
    }

    public static Distance<Box> boxMinTaxi()
    {
        return new Distance<Box>()
        {
            public double dist(Box left, Box right)
            {
                double minDist = Double.MAX_VALUE;
                for (int k = 0; k < 3; k++)
                {
                    Interval rangeLeft = left.range(k);
                    Interval rangeRight = right.range(k);

                    double minLeft = rangeLeft.getMin();
                    double maxLeft = rangeLeft.getMax();
                    double minRight = rangeRight.getMin();
                    double maxRight = rangeRight.getMax();

                    double dist = Double.MAX_VALUE;
                    dist = Math.min(dist, Math.min(minLeft, minRight));
                    dist = Math.min(dist, Math.min(minLeft, maxRight));
                    dist = Math.min(dist, Math.min(maxLeft, maxRight));
                    dist = Math.min(dist, Math.min(maxLeft, minRight));

                    minDist = Math.min(minDist, dist);
                }
                return minDist;
            }
        };

    }

    public static Distance<Curve> curveHausdorff()
    {
        return new Distance<Curve>()
        {
            public double dist(Curve left, Curve right)
            {
                Double max = Double.MIN_VALUE;
                for (Vect p : left)
                {
                    double min = right.nearestVertex(p).a;
                    max = Math.max(min, max);
                }

                return max;
            }
        };
    }

    public static Distance<Curve> curveEndpoints()
    {
        return new Distance<Curve>()
        {
            public double dist(Curve left, Curve right)
            {
                Vect leftHead = left.getHead();
                Vect leftTail = left.getTail();
                Vect rightHead = right.getHead();
                Vect rightTail = right.getTail();

                double distA = (leftHead.dist(rightHead) + leftTail.dist(rightTail)) / 2;
                double distB = (leftHead.dist(rightTail) + leftTail.dist(rightHead)) / 2;

                return Math.min(distA, distB);
            }
        };
    }

    public static Distance<Curve> curveChamfer()
    {
        return new Distance<Curve>()
        {
            public double dist(Curve left, Curve right)
            {
                double sum = 0;
                double count = 0;
                for (Vect p : left)
                {
                    double min = right.nearestVertex(p).a;
                    sum += min;
                    count += 1;
                }
                double mean = sum / count;

                return mean;
            }
        };
    }

    public static Distance<Curve> curveCutoff(final double cutoff)
    {
        return new Distance<Curve>()
        {
            public double dist(Curve left, Curve right)
            {
                int na = left.size();
                int nb = right.size();

                Curve shortc = na < nb ? left : right;
                Curve longc = na < nb ? right : left;

                int n = shortc.size();
                double[] s = new double[n];
                double[] d = new double[n];
                double[] nd = new double[n];

                // Initialize
                Vect prev = shortc.get(0);
                double cd0 = longc.nearestVertex(prev).a - cutoff;
                s[0] = 0;
                d[0] = Math.max(cd0, 0);
                nd[0] = cd0 == 0 ? 0 : Math.max(cd0 / Math.abs(cd0), 0);

                for (int i = 1; i < n; i++)
                {
                    Vect curr = shortc.get(i);
                    double ds = curr.dist(prev);
                    double cd = longc.nearestVertex(curr).a - cutoff;

                    s[i] = s[i - 1] + ds;
                    d[i] = Math.max(cd, 0);
                    nd[i] = cd == 0 ? 0 : Math.max(cd / Math.abs(cd), 0);

                    prev = curr;
                }

                double num = MathUtils.trapz(s, d);
                double denom = MathUtils.trapz(s, nd);

                double dist = denom == 0 ? 0 : num / denom;

                return dist;
            }
        };
    }

    private DistanceSource()
    {
    }
}
