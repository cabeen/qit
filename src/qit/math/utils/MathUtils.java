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

package qit.math.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectStats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MathUtils
{
    public static double gaussian(double value, double mean, double std)
    {
        double z = (value - mean) / std;
        double g = Math.exp(-0.5 * z * z);
        double p = g / (std * Math.sqrt(2 * Math.PI));

        return p;
    }

    public static double parse(String value, double def)
    {
        if (number(value))
        {
            return Double.valueOf(value);
        }
        else
        {
            return def;
        }
    }

    public static boolean number(String value)
    {
        try
        {
            double number = Double.valueOf(value);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    public static double cubicthresh(double x)
    {
        if (x < 0)
        {
            return 0;
        }
        else if (x > 1)
        {
            return 1;
        }
        else
        {
            return 3 * x * x - 2 * x * x * x;
        }
    }

    public static double tetrahedraVolume(Vect a, Vect b, Vect c, Vect d)
    {
        Vect u = a.minus(d);
        Vect v = b.minus(d);
        Vect w = c.minus(d);

        return Math.abs(v.cross(w).dot(u)) / 6.0;
    }

    public static boolean valid(double value)
    {
        return !Double.isInfinite(value) && !Double.isNaN(value);
    }

    public static Vect zscores(Vect vect)
    {
        VectStats stats = new VectStats().withInput(vect).run();

        Vect out = vect.minus(stats.mean);

        if (MathUtils.nonzero(stats.std))
        {
            out.timesEquals(1.0 / stats.std);
        }

        return out;
    }

    public static void shuffle(int[] vals)
    {
        for (int i = vals.length - 1; i > 0; i--)
        {
            int index = Global.RANDOM.nextInt(i + 1);
            // Simple swap
            int a = vals[index];
            vals[index] = vals[i];
            vals[i] = a;
        }
    }

    public static int[] subset(int num, int sub)
    {
        if (num == 0 || sub == 0)
        {
            return new int[]{};
        }

        Global.assume(sub <= num, String.format("invalid which %d of %d", sub, num));

        int[] out = new int[sub];

        int[] idx = new int[num];
        for (int i = 0; i < num; i++)
        {
            idx[i] = i;
        }

        shuffle(idx);

        for (int i = 0; i < sub; i++)
        {
            out[i] = idx[i];
        }

        return out;
    }

    public static List<Integer> equals(int[] data, int val)
    {
        List<Integer> out = Lists.newArrayList();
        for (int i = 0; i < data.length; i++)
        {
            if (data[i] == val)
            {
                out.add(i);
            }
        }

        return out;
    }

    public static int[] plus(int[] data, int v)
    {
        int[] output = new int[data.length];

        for (int i = 0; i < data.length; i++)
        {
            output[i] = data[i] + v;
        }

        return output;
    }

    public static void plusEquals(int[] data, int v)
    {
        for (int i = 0; i < data.length; i++)
        {
            data[i] += v;
        }
    }

    public static int[] relabel(int[] labels)
    {
        Map<Integer, Integer> counts = MathUtils.counts(labels);
        Map<Integer, Integer> lookup = MathUtils.remap(counts);

        return map(labels, lookup);
    }

    public static int[] map(int[] labels, Map<Integer, Integer> lookup)
    {
        int[] out = new int[labels.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = lookup.get(labels[i]);
        }

        return out;
    }

    public static Map<Integer, Integer> sort(Map<Integer, Double> weights)
    {
        int cdim = weights.size();
        int[] ckey = new int[cdim];
        double[] cval = new double[cdim];
        int c = 0;
        for (Integer key : weights.keySet())
        {
            ckey[c] = key;
            cval[c] = weights.get(key);
            c += 1;
        }

        int[] perm = MathUtils.permutation(cval);
        Map<Integer, Integer> labelmap = Maps.newHashMap();
        for (int i = 0; i < cdim; i++)
        {
            int idx = perm[cdim - 1 - i];
            int key = ckey[idx];
            int nlab = i + 1;
            labelmap.put(key, nlab);
        }

        return labelmap;
    }

    // reorder labels by count
    public static Map<Integer, Integer> remap(Map<Integer, Integer> counts)
    {
        int cdim = counts.size();
        int[] ckey = new int[cdim];
        int[] cval = new int[cdim];
        int c = 0;
        for (Integer key : counts.keySet())
        {
            ckey[c] = key;
            cval[c] = counts.get(key);
            c += 1;
        }

        int[] perm = MathUtils.permutation(cval);
        Map<Integer, Integer> labelmap = Maps.newHashMap();
        for (int i = 0; i < cdim; i++)
        {
            labelmap.put(ckey[perm[cdim - 1 - i]], i + 1);
        }

        return labelmap;
    }

    public static double logGamma(double xx)
    {
        double x, y, tmp, ser;
        double[] cof = {76.18009172947146, -86.50532032941677, 24.01409824083091, -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5};
        int j;
        y = x = xx;
        tmp = x + 5.5;
        tmp -= (x + 0.5) * Math.log(tmp);
        ser = 1.000000000190015;
        for (j = 0; j <= 5; j++)
        {
            ser += cof[j] / ++y;
        }
        return -tmp + Math.log(2.5066282746310005 * ser / x);
    }

    public static double factorial(int n)
    {
        if (n <= 1)
        {
            return 1.0;
        }
        return Math.exp(logGamma(n + 1.0));
    }

    /**
     * The confluent hypergeometric function, usually given the symbol 1F1. This
     * is a porting to Java of the function hyperg_1F1_luke in the file
     * specfunc/hyperg_1F1.c from the GNU Scientific Library. For algorithm, see
     * [Luke, Algorithms for the Computation of Mathematical Functions, p.182]
     * (GSL license info to be added) GSL home page is
     * http://www.gnu.org/software/gsl/
     * <p/>
     * from: http://www.etomica.org/
     */
    public static double confluentHypergeometric1F1(double a, double c, double xin)
    {
        final double RECUR_BIG = 1.0e+50;
        final double GSL_DBL_EPSILON = 2.2204460492503131e-16;
        final int nmax = 5000;
        int n = 3;
        final double x = -xin;
        final double x3 = x * x * x;
        final double t0 = a / c;
        final double t1 = (a + 1.0) / (2.0 * c);
        final double t2 = (a + 2.0) / (2.0 * (c + 1.0));
        double F = 1.0;
        double prec;

        double Bnm3 = 1.0; /* B0 */
        double Bnm2 = 1.0 + t1 * x; /* B1 */
        double Bnm1 = 1.0 + t2 * x * (1.0 + t1 / 3.0 * x); /* B2 */

        double Anm3 = 1.0; /* A0 */
        double Anm2 = Bnm2 - t0 * x; /* A1 */
        double Anm1 = Bnm1 - t0 * (1.0 + t2 * x) * x + t0 * t1 * (c / (c + 1.0)) * x * x; /* A2 */

        while (true)
        {
            double npam1 = n + a - 1;
            double npcm1 = n + c - 1;
            double npam2 = n + a - 2;
            double npcm2 = n + c - 2;
            double tnm1 = 2 * n - 1;
            double tnm3 = 2 * n - 3;
            double tnm5 = 2 * n - 5;
            double F1 = (n - a - 2) / (2 * tnm3 * npcm1);
            double F2 = (n + a) * npam1 / (4 * tnm1 * tnm3 * npcm2 * npcm1);
            double F3 = -npam2 * npam1 * (n - a - 2) / (8 * tnm3 * tnm3 * tnm5 * (n + c - 3) * npcm2 * npcm1);
            double E = -npam1 * (n - c - 1) / (2 * tnm3 * npcm2 * npcm1);

            double An = (1.0 + F1 * x) * Anm1 + (E + F2 * x) * x * Anm2 + F3 * x3 * Anm3;
            double Bn = (1.0 + F1 * x) * Bnm1 + (E + F2 * x) * x * Bnm2 + F3 * x3 * Bnm3;
            double r = An / Bn;

            prec = Math.abs((F - r) / F);
            F = r;

            if (prec < GSL_DBL_EPSILON || n > nmax)
            {
                break;
            }

            if (Math.abs(An) > RECUR_BIG || Math.abs(Bn) > RECUR_BIG)
            {
                An /= RECUR_BIG;
                Bn /= RECUR_BIG;
                Anm1 /= RECUR_BIG;
                Bnm1 /= RECUR_BIG;
                Anm2 /= RECUR_BIG;
                Bnm2 /= RECUR_BIG;
                Anm3 /= RECUR_BIG;
                Bnm3 /= RECUR_BIG;
            }
            else if (Math.abs(An) < 1.0 / RECUR_BIG || Math.abs(Bn) < 1.0 / RECUR_BIG)
            {
                An *= RECUR_BIG;
                Bn *= RECUR_BIG;
                Anm1 *= RECUR_BIG;
                Bnm1 *= RECUR_BIG;
                Anm2 *= RECUR_BIG;
                Bnm2 *= RECUR_BIG;
                Anm3 *= RECUR_BIG;
                Bnm3 *= RECUR_BIG;
            }

            n++;
            Bnm3 = Bnm2;
            Bnm2 = Bnm1;
            Bnm1 = Bn;
            Anm3 = Anm2;
            Anm2 = Anm1;
            Anm1 = An;
        }

        return F;
        // result->err = 2.0 * fabs(F * prec);
        // result->err += 2.0 * GSL_DBL_EPSILON * (n-1.0) * fabs(F);
    }

    public static List<Double> select(List<Double> elems, int[] labels, int idx)
    {
        List<Double> out = Lists.newArrayList();
        for (int i = 0; i < labels.length; i++)
        {
            if (labels[i] == idx)
            {
                out.add(elems.get(i));
            }
        }

        return out;
    }

    public static List<Double> times(List<Double> values, double factor)
    {
        List<Double> out = Lists.newArrayList();
        for (Double v : values)
        {
            out.add(factor * v);
        }
        return out;
    }

    public static void timesEquals(List<Double> values, double factor)
    {
        for (int i = 0; i < values.size(); i++)
        {
            values.set(i, values.get(i) * factor);
        }
    }

    public static double sum(List<Double> values)
    {
        double out = 0;
        for (Double v : values)
        {
            out += v;
        }
        return out;
    }

    public static int diff(int[] a, int[] b)
    {
        int out = 0;
        for (int i = 0; i < a.length; i++)
        {
            if (a[i] != b[i])
            {
                out += 1;
            }
        }
        return out;
    }

    public static Integer maxkey(Map<Integer, Integer> mapping)
    {
        int maxkey = -1;
        int maxvalue = 0;

        for (Integer key : mapping.keySet())
        {
            int value = mapping.get(key);
            if (value > maxvalue)
            {
                maxkey = key;
                maxvalue = value;
            }
        }

        return maxkey;
    }

    public static Map<Integer, Integer> counts(int[] vals)
    {
        Map<Integer, Integer> out = Maps.newHashMap();
        for (int val : vals)
        {
            if (out.containsKey(val))
            {
                out.put(val, out.get(val) + 1);
            }
            else
            {
                out.put(val, 1);
            }
        }

        return out;
    }

    public static int count(int[] vals, int value)
    {
        int out = 0;
        for (int val : vals)
        {
            if (val == value)
            {
                out += 1;
            }
        }
        return out;
    }

    public static int[] counts(int[] vals, int max)
    {
        int[] counts = new int[max];
        for (int i = 0; i < vals.length; i++)
        {
            counts[vals[i]]++;
        }

        return counts;
    }

    public static Map<Integer, Integer> hist(int[] vals)
    {
        Map<Integer, Integer> out = Maps.newHashMap();
        for (int v : vals)
        {
            Integer count = out.containsKey(v) ? out.get(v) + 1 : 1;
            out.put(v, count);
        }

        return out;
    }

    public static Set<Integer> values(int[] vals)
    {
        Set<Integer> out = Sets.newHashSet();
        for (int val : vals)
        {
            out.add(val);
        }
        return out;
    }

    public static int[] permutation(final int[] values)
    {
        List<Integer> indices = Lists.newArrayList();
        for (int i = 0; i < values.length; i++)
        {
            indices.add(i);
        }

        Comparator<Integer> comparator = new Comparator<Integer>()
        {
            public int compare(Integer i, Integer j)
            {
                return Double.compare(values[i], values[j]);
            }
        };

        Collections.sort(indices, comparator);

        int[] out = new int[values.length];
        for (int i = 0; i < values.length; i++)
        {
            out[i] = indices.get(i);
        }

        return out;
    }

    public static int[] permutation(final double[] values)
    {
        List<Integer> indices = Lists.newArrayList();
        for (int i = 0; i < values.length; i++)
        {
            indices.add(i);
        }

        Comparator<Integer> comparator = new Comparator<Integer>()
        {
            public int compare(Integer i, Integer j)
            {
                return Double.compare(values[i], values[j]);
            }
        };

        Collections.sort(indices, comparator);

        int[] out = new int[values.length];
        for (int i = 0; i < values.length; i++)
        {
            out[i] = indices.get(i);
        }

        return out;
    }

    public static int[] inverse(int[] perm)
    {
        int[] out = new int[perm.length];
        for (int i = 0; i < perm.length; i++)
        {
            out[perm[i]] = i;
        }

        return out;
    }

    public static int signzero(double x)
    {
        return x < 0 ? -1 : x > 0 ? 1 : 0;
    }

    public static int sign(double x)
    {
        return x < 0 ? -1 : 1;
    }

    public static int ipart(double x)
    {
        return (int) x;
    }

    public static int round(double x)
    {
        return ipart(x + 0.5);
    }

    public static double fpart(double x)
    {
        return x - (int) x;
    }

    public static double rfpart(double x)
    {
        return 1 - fpart(x);
    }

    public static boolean[] threshold(double[] array, double threshold)
    {
        int n = array.length;
        boolean[] out = new boolean[n];
        for (int i = 0; i < n; i++)
        {
            out[i] = array[i] >= threshold;
        }

        return out;
    }

    public static int count(boolean[] array)
    {
        return counttrue(array);
    }

    public static int counttrue(boolean[] array)
    {
        int count = 0;
        for (boolean v : array)
        {
            if (v)
            {
                count += 1;
            }
        }

        return count;
    }

    public static int countfalse(boolean[] array)
    {
        int count = 0;
        for (boolean v : array)
        {
            if (!v)
            {
                count += 1;
            }
        }

        return count;
    }

    public static boolean[] flip(boolean[] array)
    {
        int n = array.length;
        boolean[] out = new boolean[n];
        for (int i = 0; i < n; i++)
        {
            out[i] = !array[i];
        }

        return out;
    }

    public static boolean[] and(boolean[] a, boolean[] b)
    {
        if (a.length != b.length)
        {
            throw new RuntimeException("array lengths do not match");
        }

        int n = a.length;
        boolean[] out = new boolean[n];
        for (int i = 0; i < n; i++)
        {
            out[i] = a[i] && b[i];
        }

        return out;
    }

    public static boolean[] or(boolean[] a, boolean[] b)
    {
        if (a.length != b.length)
        {
            throw new RuntimeException("array lengths do not match");
        }

        int n = a.length;
        boolean[] out = new boolean[n];
        for (int i = 0; i < n; i++)
        {
            out[i] = a[i] || b[i];
        }

        return out;
    }

    public static double[] constantWeights(int n)
    {
        double[] out = new double[n];
        for (int i = 0; i < n; i++)
        {
            out[i] = 1.0 / n;
        }

        return out;
    }

    public static double[] polyWeights(double ratio, int num)
    {
        if (ratio < 0 || ratio > 1 || num < 1)
        {
            throw new RuntimeException("invalid parameters");
        }

        double[] out = new double[num];
        for (int i = 0; i < num; i++)
        {
            double d = 1 - 2.0 * i / num;
            out[i] = 1 - ratio * Math.pow(d, 2);
        }

        MathUtils.timesEquals(out, 1.0 / MathUtils.sum(out));

        return out;
    }

    public static double sinc(double v)
    {
        if (MathUtils.zero(v))
        {
            return 1;
        }
        else
        {
            return Math.sin(v) / v;
        }
    }

    public static double logistic(double gain, double offset, double value)
    {
        return 1.0 / (1.0 + Math.exp(-gain * (value + offset)));
    }

    public static double mean(double[] vals)
    {
        double out = 0;
        for (double d : vals)
        {
            out += d;
        }

        return out / vals.length;
    }

    public static double var(double[] vals)
    {
        double mean = mean(vals);
        double out = 0;

        for (double d : vals)
        {
            double delta = d - mean;
            out += delta * delta;
        }

        return out / vals.length;
    }

    public static double[] nearest(double[] x, double[] y, double[] nx)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("The given input domain and range must have the same length.");
        }

        for (int i = 0; i < x.length - 1; i++)
        {
            if (x[i + 1] <= x[i])
            {
                throw new RuntimeException("The given input values must be increasing.");
            }
        }

        double[] ny = new double[nx.length];

        double t_start = x[0];
        double t_end = x[x.length - 1];

        for (int i = 0; i < nx.length; i++)
        {
            // Map the domain coordinate to a zero-based array index
            double new_nt = (x.length - 1) * (nx[i] - t_start) / (t_end - t_start);

            // Round to index
            int index = (int) Math.round(new_nt);

            if (index < 0 || index >= x.length)
            {
                throw new IllegalArgumentException("The output domain specifies a coordinate outside the input domain's interval.  Index = " + index);
            }

            ny[i] = y[index];
        }

        return ny;
    }

    public static double trapz(double[] x, double[] y)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("The given input domain and range must have the same length.");
        }

        for (int i = 0; i < x.length - 1; i++)
        {
            if (x[i + 1] <= x[i])
            {
                throw new RuntimeException("The given input values must be increasing.");
            }
        }

        int num = x.length;
        double output = 0;

        for (int i = 0; i < num - 1; i++)
        {
            output += trapezoidArea(x[i + 1] - x[i], y[i + 1], y[i]);
        }

        return output;
    }

    public static double[] cumpdf(int[] counts)
    {
        double[] output = new double[counts.length];
        int total = MathUtils.sum(counts);

        for (int i = 0; i < counts.length; i++)
        {
            double p = counts[i] / (double) total;
            output[i] = p;

            if (i > 0)
            {
                output[i] += output[i - 1];
            }
        }

        return output;
    }

    public static double[] cumtrapz(double[] x, double[] y)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("The given arrays must have equal lengths.");
        }

        double[] output = new double[x.length];
        int num = x.length;

        double sum = 0;
        output[0] = 0;

        for (int i = 0; i < num - 1; i++)
        {
            sum += trapezoidArea(x[i + 1] - x[i], y[i + 1], y[i]);
            output[i + 1] = sum;
        }

        return output;
    }

    public double[] cumprod(double[] vals)
    {
        double[] out = new double[vals.length];
        out[0] = vals[0];

        for (int i = 1; i < vals.length; i++)
        {
            double curr = vals[i];
            double prev = out[i - 1];
            out[i] = curr * prev;
        }

        return out;
    }

    public double[] cumsum(double[] vals)
    {
        double[] out = new double[vals.length];
        out[0] = vals[0];

        for (int i = 1; i < vals.length; i++)
        {
            double curr = vals[i];
            double prev = out[i - 1];
            out[i] = curr + prev;
        }

        return out;
    }

    public double[] seq(double start, double step, double end)
    {
        int num = (int) Math.floor((end - start) / step);
        double[] out = new double[num];

        for (int i = 0; i < num; i++)
        {
            out[i] = start + step * i;
        }

        return out;
    }

    public static double trapezoidArea(double base, double lengthA, double lengthB)
    {
        if (base <= 0)
        {
            throw new IllegalArgumentException("The trapezoidal base must be positive.");
        }

        return 0.5d * base * (lengthA + lengthB);
    }

    public static double[] linspace(double a, double b, int num)
    {
        if (a >= b)
        {
            throw new IllegalArgumentException("The starting value must be less than the ending value.");
        }

        double[] output = new double[num];
        double d = b - a;
        for (int i = 0; i < num - 1; i++)
        {
            output[i] = a + d * i / (num - 1);
        }

        output[num - 1] = b;

        return output;
    }

    public static double[] gradient(double[] data, double step)
    {
        double[] output = new double[data.length];

        if (step <= 0)
        {
            throw new IllegalArgumentException("The step size must be positive.");
        }

        int num = data.length;

        // Use forward difference for the first and last point
        output[0] = (data[1] - data[0]) / step;
        output[num - 1] = (data[num - 1] - data[num - 2]) / step;

        // Use central difference for interior points
        for (int i = 1; i < data.length - 1; i++)
        {
            output[i] = (data[i + 1] - data[i - 1]) / (2 * step);
        }

        return output;
    }

    public static void sqrtEquals(double[] input)
    {
        for (int i = 0; i < input.length; i++)
        {
            input[i] = Math.sqrt(input[i]);
        }
    }

    public static double[] sqrt(double[] input)
    {
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = Math.sqrt(input[i]);
        }

        return output;
    }

    public static void powEquals(double[] input, double p)
    {
        for (int i = 0; i < input.length; i++)
        {
            input[i] = Math.pow(input[i], p);
        }
    }

    public static double[] pow(double[] input, double p)
    {
        double[] output = new double[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = Math.pow(input[i], p);
        }

        return output;
    }

    public static double dot(double[] a, double[] b)
    {
        double out = 0;

        for (int i = 0; i < a.length; i++)
        {
            out += a[i] * b[i];
        }

        return out;
    }

    public static double norm(double[] p)
    {
        return Math.sqrt(dot(p, p));
    }

    public static double[] diff(double[] data)
    {
        double[] output = new double[data.length - 1];

        for (int i = 0; i < data.length - 1; i++)
        {
            output[i] = data[i + 1] - data[i];
        }

        return output;
    }

    public static double[] linear(double[] x, double[] y, double[] nx)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("The given input domain and range must have the same length.");
        }

        for (int i = 0; i < x.length - 1; i++)
        {
            if (x[i + 1] < x[i])
            {
                throw new RuntimeException("The given input values must not be decreasing");
            }
        }

        double[] ny = new double[nx.length];

        for (int i = 0; i < nx.length; i++)
        {
            if (nx[i] < x[0] - Global.DELTA || nx[i] > x[x.length - 1] + Global.DELTA)
            {
                String info = String.format("Min = %f, Max = %f, Value = %f, Index = %d", x[0], x[x.length - 1], nx[i], i);
                throw new RuntimeException(
                        "The new parameters are defined outside the domain of the old parameters.  This is extrapolation, and it is not defined in this situation.  "
                                + info);
            }

            for (int j = 0; j < x.length - 1; j++)
            {
                if (nx[i] >= x[j] && nx[i] <= x[j + 1])
                {
                    double delta = x[j + 1] - x[j];
                    double epsilon = nx[i] - x[j];
                    double s = delta == 0 ? 0 : epsilon / delta;
                    ny[i] = y[j] * (1 - s) + y[j + 1] * s;
                }
            }
        }

        return ny;
    }

    public static double[][] cov(double[][] data)
    {
        // m observations of n dimensions
        int m = data.length;
        int n = data[0].length;

        double[] mean = new double[n];

        // Compute mean
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < m; j++)
            {
                mean[i] += data[j][i] / m;
            }
        }

        // compute zero mean data
        Matrix zmd = new Matrix(m, n);
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < m; j++)
            {
                zmd.set(j, i, data[j][i] - mean[i]);
            }
        }

        // Compute the covariance matrix
        Matrix out = zmd.times(zmd.transpose()).times(1 / (double) (m - 1));

        // Return a reference to the data array
        return out.toArray();
    }

    public static double sum(double[][] x)
    {
        double out = 0;
        for (double[] element : x)
        {
            out += sum(element);
        }

        return out;
    }

    public static int sum(int[] x)
    {
        int out = 0;
        for (double element : x)
        {
            out += element;
        }

        return out;
    }

    public static double sum(double[] x)
    {
        double out = 0;
        for (double element : x)
        {
            out += element;
        }

        return out;
    }

    public static int min(int x, int y, int z)
    {
        return Math.min(x, Math.min(y, z));
    }

    public static double min(double x, double y, double z)
    {
        return Math.min(x, Math.min(y, z));
    }

    public static int max(int x, int y, int z)
    {
        return Math.max(x, Math.max(y, z));
    }

    public static double max(double x, double y, double z)
    {
        return Math.max(x, Math.max(y, z));
    }

    public static double max(double[] x)
    {
        double max = x[0];
        for (int i = 1; i < x.length; i++)
        {
            if (x[i] > max)
            {
                max = x[i];
            }
        }

        return max;
    }

    public static int max(int[] x)
    {
        int max = x[0];
        for (int i = 1; i < x.length; i++)
        {
            if (x[i] > max)
            {
                max = x[i];
            }
        }

        return max;
    }

    public static double[][] toDouble(float[][] input)
    {
        double[][] output = new double[input.length][];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = new double[input[i].length];
            for (int j = 0; j < input[i].length; j++)
            {
                output[i][j] = input[i][j];
            }
        }

        return output;
    }

    public static double[][] toDouble(int[][] input)
    {
        double[][] output = new double[input.length][];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = new double[input[i].length];
            for (int j = 0; j < input[i].length; j++)
            {
                output[i][j] = input[i][j];
            }
        }

        return output;
    }

    public static float[][] copy(float[][] input)
    {
        float[][] output = new float[input.length][];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = new float[input[i].length];
            System.arraycopy(input[i], 0, output[i], 0, output[i].length);
        }

        return output;
    }

    public static double[][] copy(double[][] input)
    {
        double[][] output = new double[input.length][];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = new double[input[i].length];
            System.arraycopy(input[i], 0, output[i], 0, output[i].length);
        }

        return output;
    }

    public static void copy(double[][] input, double[][] output)
    {
        checkLengths(input, output);

        for (int i = 0; i < input.length; i++)
        {
            System.arraycopy(input[i], 0, output[i], 0, output[i].length);
        }
    }

    public static int[] copy(int[] input)
    {
        int[] output = new int[input.length];
        System.arraycopy(input, 0, output, 0, input.length);
        return output;
    }

    public static double[] copy(double[] input)
    {
        double[] output = new double[input.length];
        System.arraycopy(input, 0, output, 0, input.length);
        return output;
    }

    public static double[][] times(double[][] data, double scalar)
    {
        double[][] output = new double[data.length][data[0].length];

        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < data[i].length; j++)
            {
                output[i][j] = data[i][j] * scalar;
            }
        }

        return output;
    }

    public static void timesEquals(double[][] a, double[][] b)
    {
        checkLengths(a, b);

        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                a[i][j] *= b[i][j];
            }
        }
    }

    public static double[][] times(double[][] a, double[][] b)
    {
        checkLengths(a, b);

        double[][] output = new double[a.length][a[0].length];

        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                output[i][j] = a[i][j] * b[i][j];
            }
        }

        return output;
    }

    public static double[] times(double[] a, double[] b)
    {
        checkLengths(a, b);

        double[] output = new double[a.length];

        for (int i = 0; i < a.length; i++)
        {
            output[i] = a[i] * b[i];
        }

        return output;
    }

    public static double[] times(double[] data, double scalar)
    {
        double[] output = new double[data.length];

        for (int i = 0; i < data.length; i++)
        {
            output[i] = data[i] * scalar;
        }

        return output;
    }

    public static double[] plus(double[] data, double scalar)
    {
        double[] output = new double[data.length];

        for (int i = 0; i < data.length; i++)
        {
            output[i] = data[i] + scalar;
        }

        return output;
    }

    public static double[][] plus(double[][] a, double[][] b)
    {
        checkLengths(a, b);

        double[][] output = new double[a.length][a[0].length];

        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                output[i][j] = a[i][j] + b[i][j];
            }
        }

        return output;
    }

    public static double[] plus(double[] a, double[] b)
    {
        double[] output = new double[a.length];

        for (int i = 0; i < a.length; i++)
        {
            output[i] = a[i] + b[i];
        }

        return output;
    }

    public static double[] minus(double[] data, double scalar)
    {
        double[] output = new double[data.length];

        for (int i = 0; i < data.length; i++)
        {
            output[i] = data[i] - scalar;
        }

        return output;
    }

    public static double[][] minus(double[][] a, double[][] b)
    {
        double[][] output = new double[a.length][a[0].length];

        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                output[i][j] = a[i][j] - b[i][j];
            }
        }

        return output;
    }

    public static double[] minus(double[] a, double[] b)
    {
        double[] output = new double[a.length];

        for (int i = 0; i < a.length; i++)
        {
            output[i] = a[i] - b[i];
        }

        return output;
    }

    public static void timesEquals(double[][] data, double scalar)
    {
        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < data[i].length; j++)
            {
                data[i][j] *= scalar;
            }
        }
    }

    public static void timesEquals(double[] a, double[] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] *= b[i];
        }
    }

    public static void timesEquals(double[] data, double scalar)
    {
        check(scalar);

        for (int i = 0; i < data.length; i++)
        {
            data[i] *= scalar;
        }
    }

    public static void plusEquals(double[] data, double scalar)
    {
        for (int i = 0; i < data.length; i++)
        {
            data[i] += scalar;
        }
    }

    public static void plusEquals(double[][] a, double[][] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                a[i][j] += b[i][j];
            }
        }

    }

    public static void plusEquals(double[] a, double[] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] += b[i];
        }
    }

    public static void minusEquals(double[] data, double scalar)
    {
        for (int i = 0; i < data.length; i++)
        {
            data[i] -= scalar;
        }
    }

    public static void minusEquals(double[] a, double[] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] -= b[i];
        }
    }

    public static void minusEquals(double[][] a, double[][] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                a[i][j] -= b[i][j];
            }
        }
    }

    public static double[][] linearComb(double alpha, double[][] a, double beta, double[][] b)
    {
        double[][] output = new double[a.length][a[0].length];

        for (int i = 0; i < output.length; i++)
        {
            for (int j = 0; j < output[0].length; j++)
            {
                output[i][j] = alpha * a[i][j] + beta * b[i][j];
            }
        }

        return output;
    }

    public static void linearComb_equals(double[][] a, double beta, double[][] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                a[i][j] = a[i][j] + beta * b[i][j];
            }
        }
    }

    public static void checkLengths(double[][] a, double[][] b)
    {
        if (a.length != b.length || a[0].length != b[0].length)
        {
            throw new IllegalArgumentException("The given arrays must have the same lengths.");
        }
    }

    public static void checkLengths(double[] a, double[] b)
    {
        if (a.length != b.length)
        {
            throw new IllegalArgumentException("The given arrays must have the same length.");
        }
    }

    public static void check(double value)
    {
        if (Double.isNaN(value) || Double.isInfinite(value))
        {
            throw new IllegalArgumentException("The input is not well-defined.  Value = " + value);
        }
    }

    public static float mean(float[] vals)
    {
        float out = 0;
        for (float d : vals)
        {
            out += d;
        }

        return out / vals.length;
    }

    public static float var(float[] vals)
    {
        float mean = mean(vals);
        float out = 0;

        for (float d : vals)
        {
            float delta = d - mean;
            out += delta * delta;
        }

        return out / vals.length;
    }

    public static float[] nearest(float[] x, float[] y, float[] nx)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("The given input domain and range must have the same length.");
        }

        for (int i = 0; i < x.length - 1; i++)
        {
            if (x[i + 1] <= x[i])
            {
                throw new RuntimeException("The given input values must be increasing.");
            }
        }

        float[] ny = new float[nx.length];

        float t_start = x[0];
        float t_end = x[x.length - 1];

        for (int i = 0; i < nx.length; i++)
        {
            // Map the domain coordinate to a zero-based array index
            float new_nt = (x.length - 1) * (nx[i] - t_start) / (t_end - t_start);

            // Round to index
            int index = Math.round(new_nt);

            if (index < 0 || index >= x.length)
            {
                throw new IllegalArgumentException("The output domain specifies a coordinate outside the input domain's interval.  Index = " + index);
            }

            ny[i] = y[index];
        }

        return ny;
    }

    public static float trapz(float[] x, float[] y)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("The given input domain and range must have the same length.");
        }

        for (int i = 0; i < x.length - 1; i++)
        {
            if (x[i + 1] <= x[i])
            {
                throw new RuntimeException("The given input values must be increasing.");
            }
        }

        int num = x.length;
        float output = 0;

        for (int i = 0; i < num - 1; i++)
        {
            output += trapezoidArea(x[i + 1] - x[i], y[i + 1], y[i]);
        }

        return output;
    }

    public static float[] cumtrapz(float[] x, float[] y)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("The given arrays must have equal lengths.");
        }

        float[] output = new float[x.length];
        int num = x.length;

        float sum = 0;
        output[0] = 0;

        for (int i = 0; i < num - 1; i++)
        {
            sum += trapezoidArea(x[i + 1] - x[i], y[i + 1], y[i]);
            output[i + 1] = sum;
        }

        return output;
    }

    public static float trapezoidArea(float base, float lengthA, float lengthB)
    {
        if (base <= 0)
        {
            throw new IllegalArgumentException("The trapezoidal base must be positive.");
        }

        return 0.5f * base * (lengthA + lengthB);
    }

    public static float[] gradient(float[] data, float step)
    {
        float[] output = new float[data.length];

        if (step <= 0)
        {
            throw new IllegalArgumentException("The step size must be positive.");
        }

        int num = data.length;

        // Use forward difference for the first and last point
        output[0] = (data[1] - data[0]) / step;
        output[num - 1] = (data[num - 1] - data[num - 2]) / step;

        // Use central difference for interior points
        for (int i = 1; i < data.length - 1; i++)
        {
            output[i] = (data[i + 1] - data[i - 1]) / (2 * step);
        }

        return output;
    }

    public static double square(double v)
    {
        return v * v;
    }

    public static void sqrtEquals(float[] input)
    {
        for (int i = 0; i < input.length; i++)
        {
            input[i] = (float) Math.sqrt(input[i]);
        }
    }

    public static float[] sqrt(float[] input)
    {
        float[] output = new float[input.length];
        for (int i = 0; i < input.length; i++)
        {
            output[i] = (float) Math.sqrt(input[i]);
        }

        return output;
    }

    public static float dot(float[] a, float[] b)
    {
        float out = 0;

        for (int i = 0; i < a.length; i++)
        {
            out += a[i] * b[i];
        }

        return out;
    }

    public static float norm(float[] p)
    {
        return (float) Math.sqrt(dot(p, p));
    }

    public static float[] diff(float[] data)
    {
        float[] output = new float[data.length - 1];

        for (int i = 0; i < data.length - 1; i++)
        {
            output[i] = data[i + 1] - data[i];
        }

        return output;
    }

    public static float[] linear(float[] x, float[] y, float[] nx)
    {
        if (x.length != y.length)
        {
            throw new IllegalArgumentException("The given input domain and range must have the same length.");
        }

        for (int i = 0; i < x.length - 1; i++)
        {
            if (x[i + 1] < x[i])
            {
                throw new RuntimeException("The given input values must not be decreasing");
            }
        }

        float[] ny = new float[nx.length];

        for (int i = 0; i < nx.length; i++)
        {
            if (nx[i] < x[0] - Global.DELTA || nx[i] > x[x.length - 1] + Global.DELTA)
            {
                String info = String.format("Min = %f, Max = %f, Value = %f, Index = %d", x[0], x[x.length - 1], nx[i], i);
                throw new RuntimeException(
                        "The new parameters are defined outside the domain of the old parameters.  This is extrapolation, and it is not defined in this situation.  "
                                + info);
            }

            for (int j = 0; j < x.length - 1; j++)
            {
                if (nx[i] >= x[j] && nx[i] <= x[j + 1])
                {
                    float delta = x[j + 1] - x[j];
                    float epsilon = nx[i] - x[j];
                    float s = delta == 0 ? 0 : epsilon / delta;
                    ny[i] = y[j] * (1 - s) + y[j + 1] * s;
                }
            }
        }

        return ny;
    }

    public static float sum(float[][] x)
    {
        float out = 0;
        for (float[] element : x)
        {
            out += sum(element);
        }

        return out;
    }

    public static float sum(float[] x)
    {
        float out = 0;
        for (float element : x)
        {
            out += element;
        }

        return out;
    }

    public static float max(float[] x)
    {
        float max = x[0];
        for (int i = 1; i < x.length; i++)
        {
            if (x[i] > max)
            {
                max = x[i];
            }
        }

        return max;
    }

    public static void copy(float[][] input, float[][] output)
    {
        checkLengths(input, output);

        for (int i = 0; i < input.length; i++)
        {
            System.arraycopy(input[i], 0, output[i], 0, output[i].length);
        }
    }

    public static float[] copy(float[] input)
    {
        float[] output = new float[input.length];
        System.arraycopy(input, 0, output, 0, input.length);
        return output;
    }

    public static float[][] times(float[][] data, float scalar)
    {
        float[][] output = new float[data.length][data[0].length];

        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < data[i].length; j++)
            {
                output[i][j] = data[i][j] * scalar;
            }
        }

        return output;
    }

    public static void timesEquals(float[][] a, float[][] b)
    {
        checkLengths(a, b);

        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                a[i][j] *= b[i][j];
            }
        }
    }

    public static float[][] times(float[][] a, float[][] b)
    {
        checkLengths(a, b);

        float[][] output = new float[a.length][a[0].length];

        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[i].length; j++)
            {
                output[i][j] = a[i][j] * b[i][j];
            }
        }

        return output;
    }

    public static float[] times(float[] a, float[] b)
    {
        checkLengths(a, b);

        float[] output = new float[a.length];

        for (int i = 0; i < a.length; i++)
        {
            output[i] = a[i] * b[i];
        }

        return output;
    }

    public static float[] times(float[] data, float scalar)
    {
        float[] output = new float[data.length];

        for (int i = 0; i < data.length; i++)
        {
            output[i] = data[i] * scalar;
        }

        return output;
    }

    public static float[] plus(float[] data, float scalar)
    {
        float[] output = new float[data.length];

        for (int i = 0; i < data.length; i++)
        {
            output[i] = data[i] + scalar;
        }

        return output;
    }

    public static float[][] plus(float[][] a, float[][] b)
    {
        checkLengths(a, b);

        float[][] output = new float[a.length][a[0].length];

        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                output[i][j] = a[i][j] + b[i][j];
            }
        }

        return output;
    }

    public static float[] plus(float[] a, float[] b)
    {
        float[] output = new float[a.length];

        for (int i = 0; i < a.length; i++)
        {
            output[i] = a[i] + b[i];
        }

        return output;
    }

    public static float[] minus(float[] data, float scalar)
    {
        float[] output = new float[data.length];

        for (int i = 0; i < data.length; i++)
        {
            output[i] = data[i] - scalar;
        }

        return output;
    }

    public static float[][] minus(float[][] a, float[][] b)
    {
        float[][] output = new float[a.length][a[0].length];

        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                output[i][j] = a[i][j] - b[i][j];
            }
        }

        return output;
    }

    public static float[] minus(float[] a, float[] b)
    {
        float[] output = new float[a.length];

        for (int i = 0; i < a.length; i++)
        {
            output[i] = a[i] - b[i];
        }

        return output;
    }

    public static void timesEquals(float[][] data, float scalar)
    {
        for (int i = 0; i < data.length; i++)
        {
            for (int j = 0; j < data[i].length; j++)
            {
                data[i][j] *= scalar;
            }
        }
    }

    public static void timesEquals(float[] a, float[] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] *= b[i];
        }
    }

    public static void timesEquals(float[] data, float scalar)
    {
        check(scalar);

        for (int i = 0; i < data.length; i++)
        {
            data[i] *= scalar;
        }
    }

    public static void plusEquals(float[] data, float scalar)
    {
        for (int i = 0; i < data.length; i++)
        {
            data[i] += scalar;
        }
    }

    public static void plusEquals(float[][] a, float[][] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                a[i][j] += b[i][j];
            }
        }

    }

    public static void plusEquals(float[] a, float[] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] += b[i];
        }
    }

    public static void minusEquals(float[] data, float scalar)
    {
        for (int i = 0; i < data.length; i++)
        {
            data[i] -= scalar;
        }
    }

    public static void minusEquals(float[] a, float[] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            a[i] -= b[i];
        }
    }

    public static void minusEquals(float[][] a, float[][] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                a[i][j] -= b[i][j];
            }
        }
    }

    public static float[][] linearComb(float alpha, float[][] a, float beta, float[][] b)
    {
        float[][] output = new float[a.length][a[0].length];

        for (int i = 0; i < output.length; i++)
        {
            for (int j = 0; j < output[0].length; j++)
            {
                output[i][j] = alpha * a[i][j] + beta * b[i][j];
            }
        }

        return output;
    }

    public static void linearCombEquals(float[][] a, float beta, float[][] b)
    {
        for (int i = 0; i < a.length; i++)
        {
            for (int j = 0; j < a[0].length; j++)
            {
                a[i][j] = a[i][j] + beta * b[i][j];
            }
        }
    }

    public static void checkLengths(float[][] a, float[][] b)
    {
        if (a.length != b.length || a[0].length != b[0].length)
        {
            throw new IllegalArgumentException("The given arrays must have the same lengths.");
        }
    }

    public static void checkLengths(float[] a, float[] b)
    {
        if (a.length != b.length)
        {
            throw new IllegalArgumentException("The given arrays must have the same length.");
        }
    }

    public static void check(float value)
    {
        if (Float.isNaN(value) || Float.isInfinite(value))
        {
            throw new IllegalArgumentException("The input is not well-defined.  Value = " + value);
        }
    }

    public static void reshapeByFirstCoord(double[][] input, double[] output)
    {
        int sizeA = input.length;
        int sizeB = input[0].length;

        if (output.length != sizeA * sizeB)
        {
            throw new IllegalArgumentException("The output array must have length " + sizeA * sizeB);
        }

        for (int j = 0; j < sizeB; j++)
        {
            for (int i = 0; i < sizeA; i++)
            {
                output[j * sizeA + i] = input[i][j];
            }
        }

    }

    public static void setAll(double[] array, double value)
    {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = value;
        }
    }

    public static void setAll(int[] array, int value)
    {
        for (int i = 0; i < array.length; i++)
        {
            array[i] = value;
        }
    }

    public static void reshapeBySecondCoord(double[][] input, double[] output)
    {
        int sizeA = input.length;
        int sizeB = input[0].length;

        if (output.length != sizeA * sizeB)
        {
            throw new IllegalArgumentException("The output array must have length " + sizeA * sizeB);
        }

        for (int i = 0; i < sizeA; i++)
        {
            for (int j = 0; j < sizeB; j++)
            {
                output[i * sizeB + j] = input[i][j];
            }
        }
    }

    public static double[][] removePoints(double[][] array, int[] indices)
    {
        for (int i = 0; i < indices.length; i++)
        {
            for (int j = i + 1; j < indices.length; j++)
            {
                if (indices[i] == indices[j])
                {
                    throw new IllegalArgumentException("The array of indices should have unique elements.");
                }
                else if (indices[i] > array[0].length)
                {
                    throw new IllegalArgumentException("You cannot remove points than the input array holds.");
                }
            }
        }

        int newNum = array[0].length - indices.length;
        int dim = array.length;
        int num = array[0].length;

        if (newNum < 1)
        {
            throw new IllegalArgumentException("The must be at least a single remaining point.  The given indices include all points.");
        }

        double[][] output = new double[dim][newNum];

        int seeker = 0;
        for (int i = 0; i < num; i++)
        {
            for (int indice : indices)
            {
                if (i != indice)
                {
                    for (int k = 0; k < dim; k++)
                    {
                        output[k][seeker++] = array[k][i];
                    }
                }
            }
        }

        if (seeker != newNum)
        {
            throw new RuntimeException("Bug, expected to have output of size " + newNum + " but only had " + seeker + " points.");
        }

        return output;
    }

    public static int[] detectDuplicatePoints(double[][] array)
    {
        int num = array[0].length;
        int dim = array.length;

        double[] pointA = new double[dim];
        double[] pointB = new double[dim];

        ArrayList<Integer> indices = new ArrayList<Integer>();

        // Check for duplicate points
        for (int i = 0; i < num; i++)
        {
            for (int j = i + 1; j < num; j++)
            {

                for (int k = 0; k < dim; k++)
                {
                    pointA[k] = array[k][i];
                    pointB[k] = array[k][j];
                }

                double distance = distance(pointA, pointB);
                if (distance < Global.DELTA)
                {
                    indices.add(new Integer(j));
                }
            }
        }

        int[] out = new int[indices.size()];

        for (int i = 0; i < indices.size(); i++)
        {
            out[i] = indices.get(i).intValue();
        }

        return out;
    }

    public static double distance(double[] a, double[] b)
    {
        double out = 0;
        for (int i = 0; i < a.length; i++)
        {
            double delta = a[i] - b[i];
            out += delta * delta;
        }
        return Math.sqrt(out);
    }

    public static double computeMeanMagnitude(double[][] array)
    {
        int dim = array.length;
        int num = array[0].length;

        double error = 0;
        double magnitude;

        for (int i = 0; i < num; i++)
        {
            magnitude = 0;
            for (int j = 0; j < dim; j++)
            {
                magnitude += array[j][i] * array[j][i];
            }

            magnitude = Math.sqrt(magnitude);
            error += magnitude;
        }

        error /= num;

        return error;
    }

    public static double[][] toDoubleArray(float[][] array)
    {

        int dim = array.length;
        int num = array[0].length;

        double[][] out = new double[dim][num];

        for (int i = 0; i < dim; i++)
        {
            for (int j = 0; j < num; j++)
            {
                out[i][j] = array[i][j];
            }
        }

        return out;
    }

    public static float[][] toFloatArray(double[][] array)
    {
        int dim = array.length;
        int num = array[0].length;

        float[][] out = new float[dim][num];

        for (int i = 0; i < dim; i++)
        {
            for (int j = 0; j < num; j++)
            {
                out[i][j] = (float) array[i][j];
            }
        }

        return out;
    }

    public static float[] toFloatArray(double[] array)
    {
        int dim = array.length;
        float[] out = new float[dim];
        for (int i = 0; i < dim; i++)
        {
            out[i] = (float) array[i];
        }

        return out;
    }

    public static double[] toDoubleArray(float[] array)
    {
        int dim = array.length;
        double[] out = new double[dim];
        for (int i = 0; i < dim; i++)
        {
            out[i] = array[i];
        }

        return out;
    }

    public static double frobeniusNorm(double[][] data)
    {
        double out = 0;

        for (double[] element : data)
        {
            for (int j = 0; j < data[0].length; j++)
            {
                out += element[j] * element[j];
            }
        }

        return Math.sqrt(out);
    }

    public static double mean(double[][] data)
    {
        double out = 0;

        for (double[] element : data)
        {
            for (int j = 0; j < data[0].length; j++)
            {
                out += element[j];
            }
        }

        return out / (data.length * data[0].length);
    }

    public static double var(double[][] data)
    {
        double mean = mean(data);
        double out = 0;

        for (double[] element : data)
        {
            for (int j = 0; j < data[0].length; j++)
            {
                out += Math.pow(element[j] - mean, 2);
            }
        }

        return out / (data.length + data[0].length);
    }

    public static double[][] difference(double[][] arrayA, double[][] arrayB)
    {
        if (arrayA.length != arrayB.length || arrayA[0].length != arrayB[0].length)
        {
            throw new IllegalArgumentException("The array dimensions must agree.");
        }

        int dim = arrayA.length;
        int num = arrayA[0].length;

        double[][] out = new double[dim][num];

        for (int i = 0; i < dim; i++)
        {
            for (int j = 0; j < num; j++)
            {
                out[i][j] = arrayA[i][j] - arrayB[i][j];
            }
        }

        return out;
    }

    public static double[] difference(double[] a, double[] b)
    {
        int dim = a.length;

        double[] out = new double[dim];

        for (int i = 0; i < dim; i++)
        {
            out[i] = a[i] - b[i];
        }

        return out;
    }

    public static double[][] generateRandomDoubleData(int num, int dim, double max)
    {
        double[] mean = generateRandomDoubleVector(dim, max);
        double[] var = generateRandomDoubleVector(dim, max);

        double[][] out = new double[dim][num];

        // Create the random data
        for (int i = 0; i < dim; i++)
        {
            for (int j = 0; j < num; j++)
            {
                out[i][j] = mean[i] + var[i] * Global.RANDOM.nextDouble();
            }
        }

        return out;
    }

    public static double[] generateRandomDoubleVector(int dim, double max)
    {
        double[] out = new double[dim];

        double mag = 0;
        for (int i = 0; i < dim; i++)
        {
            out[i] = 2 * (Global.RANDOM.nextDouble() - 0.5);
            mag += out[i] * out[i];
        }

        mag = Math.sqrt(mag);

        for (int i = 0; i < dim; i++)
        {
            out[i] *= max / mag;
        }

        return out;
    }

    public static double generateRandomPositiveDoubleScalar(double max)
    {
        if (max <= 0)
        {
            throw new IllegalArgumentException("Maximum must be positive.");
        }

        double out = Global.RANDOM.nextDouble() * max;
        if (out == 0)
        {
            return generateRandomPositiveDoubleScalar(max);
        }
        else
        {
            return out;
        }
    }

    public static double[][] transpose(double[][] array)
    {
        if (array == null)
        {
            throw new IllegalArgumentException("The argument cannot be null");
        }

        int sizeA = array.length;
        int sizeB = array[0].length;

        double[][] out = new double[sizeB][sizeA];

        for (int i = 0; i < sizeA; i++)
        {
            for (int j = 0; j < sizeB; j++)
            {
                out[j][i] = array[i][j];
            }
        }

        return out;
    }

    public static double[][] normalizeColumns(double[][] source)
    {
        // Create the cube
        int num = source[0].length;
        int dim = source.length;

        double[] mag = new double[num];

        for (int i = 0; i < num; i++)
        {
            for (int j = 0; j < dim; j++)
            {
                mag[i] += source[j][i] * source[j][i];
            }
            mag[i] = Math.sqrt(mag[i]);
        }

        // double radius = new ArithmeticAverage().average(mag);
        double radius = max(mag);

        double[][] out = new double[dim][num];

        for (int i = 0; i < num; i++)
        {
            if (mag[i] != 0)
            {
                for (int j = 0; j < dim; j++)
                {
                    out[j][i] = source[j][i] / mag[i] * radius;
                }
            }
        }

        return out;
    }

    public static double min(double[][] data)
    {
        double min = data[0][0];
        for (double[] element : data)
        {
            for (int j = 0; j < data[0].length; j++)
            {
                min = Math.min(min, element[j]);
            }
        }

        return min;
    }

    public static double max(double[][] data)
    {
        double max = data[0][0];
        for (double[] element : data)
        {
            for (int j = 0; j < data[0].length; j++)
            {
                max = Math.max(max, element[j]);
            }
        }

        return max;
    }

    public static int minidx(double[] data)
    {
        if (data.length == 0)
        {
            throw new RuntimeException("invalid data");
        }

        double min = data[0];
        int idx = 0;
        for (int i = 1; i < data.length; i++)
        {
            if (data[i] < min)
            {
                min = data[i];
                idx = i;
            }
        }

        return idx;
    }

    public static int maxidx(double[] data)
    {
        double max = data[0];
        int idx = 0;
        for (int i = 1; i < data.length; i++)
        {
            if (data[i] > max)
            {
                max = data[i];
                idx = i;
            }
        }

        return idx;
    }

    public static double min(double[] data)
    {
        double min = data[0];
        for (double element : data)
        {
            min = Math.min(min, element);
        }

        return min;
    }

    public static boolean eq(double a, double b)
    {
        return Math.abs(a - b) < Global.DELTA;
    }

    public static boolean eq(double a, double b, double t)
    {
        return Math.abs(a - b) < t;
    }

    public static boolean zero(double v)
    {
        return eq(v, 0);
    }

    public static boolean nonzero(double v)
    {
        return !eq(v, 0);
    }

    public static double zerosafe(double v)
    {
        return MathUtils.zero(v) ? 1.0 : v;
    }

    public static boolean unit(double v)
    {
        return eq(v, 1);
    }

    public static boolean open(double v, double low, double high)
    {
        return low < v && v < high;
    }

    public static boolean closed(double v, double low, double high)
    {
        return low <= v && v <= high;
    }

    public static Vect poly(double t, int order)
    {
        Vect out = VectSource.createND(order + 1);

        out.set(0, 1);

        for (int i = 0; i < order; i++)
        {
            out.set(i + 1, t * out.get(i));
        }

        return out;
    }

    public static double unitsine(double x)
    {
        return 0.5 * (Math.sin(Math.PI * (x - 0.5)) + 1);
    }

    public static double sigmoid(double value, double thresh, double slope)
    {
        double exp = Math.exp(-slope * (value - thresh));
        return 1.0 / (1.0 + exp);
    }

    public static double unitmap(double value, double low, double high)
    {
        return low + (high - low) * value;
    }

    public static double unitinv(double value)
    {
        return 1.0 - value;
    }

    public static double ramp(double value, double low, double high)
    {
        if (value < low)
        {
            return 0;
        }
        else if (value > high)
        {
            return 1;
        }
        else
        {
            double delta = MathUtils.eq(high, low) ? 1.0 : high - low;
            return (value - low) / delta;
        }
    }
}