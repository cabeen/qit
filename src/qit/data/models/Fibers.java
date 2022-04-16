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

package qit.data.models;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.structs.Integers;
import qit.base.utils.ComboUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.VectUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.base.Model;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;

public class Fibers extends Model<Fibers>
{
    private final static double NO_MATCH_ANGLE = 45;

    public final static String NAME = "xfib";
    public final static String COLOR = "color";

    public static final String BASE = "base";
    public static final String DIFF = "diff";
    public static final String LINE = "line";
    public static final String RGB = "rgb";
    public static final String FISO = "fiso";
    public static final String FRAC = "frac";
    public static final String ICVF = "icvf";
    public static final String STAT = "stat";
    public static final String LABEL = "label";

    public double base;
    public double diff;
    public Vects lines;
    public Vect fracs;
    public Vect stats;
    public int[] labels;

    public static boolean valid(int size)
    {
        return size >= 2 && (size - 2) % 6 == 0;
    }

    public static boolean matches(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lname = PathUtils.basename(name).toLowerCase();
        return lname.contains(".fibers") || lname.contains(".fibres") || lname.contains(".xfib") || lname.contains(".xfibers") || lname.contains(".bedpost") || lname.contains(".bedpostx") || lname.contains(".xfibres") || lname.contains(".fom") || lname.contains("peaks");
    }

    public static int count(Vect encoding)
    {
        int size = encoding.size();
        return count(size);
    }

    public static int count(int size)
    {
        Global.assume(valid(size), "invalid fibers encoding with size: " + size);

        int n = (size - 2) / 6;

        return n;
    }

    public Fibers(int n)
    {
        this.lines = new Vects();
        this.fracs = new Vect(n);
        this.stats = new Vect(n);
        this.labels = new int[n];

        for (int i = 0; i < n; i++)
        {
            this.fracs.set(i, 0);
            this.stats.set(i, 0);
            this.labels[i] = 0;
            this.lines.add(VectSource.create3D(1, 0, 0));
        }
    }

    public Fibers(Fibers fibers)
    {
        this(fibers.size());
        this.set(fibers);
    }

    public Fibers(Vect encoding)
    {
        this(count(encoding));
        this.setEncoding(encoding);
    }

    public void clear()
    {
        this.base = 0;
        this.diff = 0;
        for (int i = 0; i < this.size(); i++)
        {
            this.lines.set(i, VectSource.create3D(1, 0, 0));
            this.fracs.set(i, 0);
            this.stats.set(i, 0);
            this.labels[i] = 0;
        }
    }

    public void setFracSum(double fsum)
    {
        this.scale(fsum / this.getFracSum());
    }

    public void scale(double fscale)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.setFrac(i, this.getFrac(i) * fscale);
        }
    }

    public void shift(double fshift)
    {
        for (int i = 0; i < this.size(); i++)
        {
            this.setFrac(i, this.getFrac(i) + fshift);
        }
    }

    public Fibers convert(int n)
    {
        Fibers out = new Fibers(n);

        out.base = this.base;
        out.diff = this.diff;

        int[] perm = VectUtils.permutation(this.fracs);
        for (int i = 0; i < n; i++)
        {
            if (i < this.size())
            {
                int pidx = perm[perm.length - 1 - i];
                out.setLine(i, this.getLine(pidx));
                out.setFrac(i, this.getFrac(pidx));
                out.setStat(i, this.getStat(pidx));
                out.setLabel(i, this.getLabel(pidx));
            }
            else
            {
                out.setLine(i, VectSource.create3D());
                out.setFrac(i, 0);
                out.setStat(i, 0);
                out.setLabel(i, 0);
            }
        }

        return out;
    }

    public Fibers set(Fibers fibers)
    {
        Global.assume(fibers.size() <= this.size(), "too many input fibers");

        this.clear();
        this.base = fibers.base;
        this.diff = fibers.diff;

        for (int i = 0; i < fibers.size(); i++)
        {
            this.lines.set(i, fibers.lines.get(i).copy());
            this.fracs.set(i, fibers.fracs.get(i));
            this.stats.set(i, fibers.stats.get(i));
            this.labels[i] = fibers.labels[i];
        }

        return this;
    }

    public double baseline()
    {
        return this.base;
    }

    public double getBaseline()
    {
        return this.base;
    }

    public double getDiffusivity()
    {
        return this.diff;
    }

    public Vect getLine(int idx)
    {
        return this.lines.get(idx).copy();
    }

    public Vects getLines()
    {
        return this.lines.copy();
    }

    public double getFrac(int idx)
    {
        return this.fracs.get(idx);
    }

    public double getStat(int idx)
    {
        return this.stats.get(idx);
    }

    public Vect getStats()
    {
        return this.stats.copy();
    }

    public Vect getFracs()
    {
        return this.fracs.copy();
    }

    public int getLabel(int idx)
    {
        return this.labels[idx];
    }

    public Integers getLabels()
    {
        return new Integers(this.labels);
    }

    public void setBaseline(double v)
    {
        this.base = v;
    }

    public void setDiffusivity(double v)
    {
        this.diff = v;
    }

    public void setLine(int idx, Vect line)
    {
        this.lines.get(idx).set(line);
    }

    public void setFrac(int idx, double frac)
    {
        if (Double.isInfinite(frac) || Double.isNaN(frac))
        {
            this.fracs.set(idx, 0);
        }
        else
        {
            this.fracs.set(idx, frac);
        }
    }

    public void select(int idx)
    {
        for (int i = 0; i < this.size(); i++)
        {
            if (i != idx)
            {
                this.setFrac(i, 0);
            }
        }
    }

    public void setStat(int idx, double stat)
    {
        if (Double.isInfinite(stat) || Double.isNaN(stat))
        {
            this.stats.set(idx, 0);
        }
        else
        {
            this.stats.set(idx, stat);
        }
    }

    public void setLabel(int idx, int label)
    {
        this.labels[idx] = label;
    }

    public int size()
    {
        return this.lines.size();
    }

    public Fibers copy()
    {
        return new Fibers(this);
    }

    public Fibers proto()
    {
        return new Fibers(this.size());
    }

    public int getDegreesOfFreedom()
    {
        // one for baseline
        // one for diffusivity
        // for each fiber
        //   two for orientation
        //   one for fraction

        return 1 + 3 * this.fracs.size();
    }

    public int getEncodingSize()
    {
        return 2 + 6 * this.fracs.size();
    }

    public static int size(int comps)
    {
        return new Fibers(comps).getEncodingSize();
    }

    public Fibers setEncoding(Vect encoding)
    {
        this.base = encoding.get(0);
        this.diff = encoding.get(1);
        for (int i = 0; i < this.size(); i++)
        {
            int idx = 2 + 6 * i;
            double frac = encoding.get(idx);
            double stat = encoding.get(idx + 1);
            if (frac < 0)
            {
                this.fracs.set(i, 0);
            }
            else
            {
                this.fracs.set(i, frac);
            }
            this.stats.set(i, stat);

            this.labels[i] = (int) Math.round(encoding.get(idx + 2));

            for (int j = 0; j < 3; j++)
            {
                this.lines.get(i).set(j, encoding.get(idx + 3 + j));
            }

            this.lines.get(i).normalizeEquals();
        }
//
//        double sumf = this.fracs.sum();
//
//        if (sumf > 1)
//        {
//            this.fracs.timesEquals(1.0 / sumf);
//        }

        return this;
    }

    public void getEncoding(Vect encoding)
    {
        int n = count(encoding);
        Global.assume(n == this.size(), "invalid fibers encoding");

        encoding.set(0, this.base);
        encoding.set(1, this.diff);
        for (int i = 0; i < n; i++)
        {
            int idx = 2 + 6 * i;
            encoding.set(idx, this.fracs.get(i));
            encoding.set(idx + 1, this.stats.get(i));
            encoding.set(idx + 2, this.labels[i]);
            for (int j = 0; j < 3; j++)
            {
                encoding.set(idx + 3 + j, this.lines.get(i).get(j));
            }
        }
    }

    public String toString()
    {
        String out = "{s0: " + this.base + ", d: " + this.diff + ", sticks: [";

        for (int i = 0; i < this.size(); i++)
        {
            Vect u = this.lines.get(i);
            double f = this.fracs.get(i);
            double s = this.stats.get(i);

            if (i > 0)
            {
                out += ", ";
            }

            out += "{f: " + f + ", s: " + s + ", u: [";
            out += u.get(0) + ", " + u.get(1) + ", " + u.get(2) + "]}";
        }

        out += "]}";

        return out;
    }

    public double getFracSum()
    {
        return this.fracs.sum();
    }

    public double getFracMax()
    {
        return this.fracs.max();
    }

    public double getFracIso()
    {
        return 1 - this.fracs.sum();
    }

    public double dist2(Fibers model)
    {
        double out = 0;
        for (int i = 0; i < this.size(); i++)
        {
            double afrac = this.getFrac(i);
            if (MathUtils.zero(afrac))
            {
                continue;
            }

            Vect aline = this.getLine(i);
            Double mindist2 = null;

            for (int j = 0; j < model.size(); j++)
            {
                Vect bline = model.getLine(j);
                double dot = aline.dot(bline);
                double dist2 = 1 - dot * dot;
                if (mindist2 == null || dist2 < mindist2)
                {
                    mindist2 = dist2;
                }
            }

            out += afrac * mindist2;
        }

        return out;
    }

    public Fibers sort()
    {
        Fibers out = this.copy();

        if (out.size() > 0)
        {
            Vect fracs = this.getFracs();
            Integers sortidx = fracs.sort();

            for (int i = 0; i < this.size(); i++)
            {
                int idx = sortidx.get(i);
                out.setFrac(i, this.getFrac(idx));
                out.setLine(i, this.getLine(idx));
            }
        }

        return out;
    }

    public Fibers crop(double min)
    {
        Fibers fibers = this.copy();
        fibers.sort();

        int num = 0;
        for (int i = 0; i < fibers.size(); i++)
        {
            if (fibers.getFrac(i) > min)
            {
                num += 1;
            }
        }

        Fibers out = new Fibers(num);
        out.setBaseline(this.getBaseline());
        out.setDiffusivity(this.getDiffusivity());
        for (int i = 0; i < num; i++)
        {
            out.setFrac(i, fibers.getFrac(i));
            out.setLine(i, fibers.getLine(i));
        }

        return out;
    }

    public int threshSoft(double min)
    {
        if (this.size() == 0)
        {
            return 0;
        }

        double fsum = this.getFracSum();
        double frem = 0;

        // this is a complex scheme because there are problems with taking a single pass
        // it is better to progressively prune fibers, for example if you have three compartments
        // below the threshold, it makes more sense to keep the minimal subset, not remove them all

        Vect fracs = this.getFracs();
        Integers idx = fracs.sort();

        int count = 0;
        for (int i = 0; i < this.size(); i++)
        {
            for (int j : idx)
            {
                double frac = this.getFrac(j);
                if (frac < min && MathUtils.nonzero(frac))
                {
                    frem += frac;
                    this.setFrac(j, 0);
                    count += 1;
                }
            }
        }

        for (int i = 0; i < this.size(); i++)
        {
            double frac = this.getFrac(i);
            double nfrac = MathUtils.eq(fsum, frem) ? 0.0 : frac * fsum / (fsum - frem);
            this.setFrac(i, nfrac);
        }

        return count;
    }

    public double dist(Fibers model)
    {
        return Math.sqrt(this.dist2(model));
    }

    public int count(double thresh)
    {
        int count = 0;
        for (int i = 0; i < this.size(); i++)
        {
            if (this.getFrac(i) > thresh)
            {
                count += 1;
            }
        }

        return count;
    }

    public Fibers thresh(double thresh)
    {
        Fibers out = this.proto();
        out.setBaseline(this.getBaseline());
        out.setDiffusivity(this.getDiffusivity());

        int idx = 0;
        for (int i = 0; i < this.size(); i++)
        {
            double frac = this.getFrac(i);
            Vect line = this.getLine(i);

            if (frac > thresh)
            {
                out.setFrac(idx, frac);
                out.setLine(idx, line);
                idx += 1;
            }
        }

        return out;
    }

    private Map<Integer, Integer> match(Fibers model, double thresh)
    {
        List<Integer> leftIdx = Lists.newArrayList();
        for (int i = 0; i < this.size(); i++)
        {
            leftIdx.add(i);
        }

        List<Integer> rightIdx = Lists.newArrayList();
        for (int i = 0; i < model.size(); i++)
        {
            if (model.getFrac(i) >= thresh)
            {
                rightIdx.add(i);
            }
        }

        int size = Math.min(leftIdx.size(), rightIdx.size());
        List<List<Integer>> rightPermutations = ComboUtils.permutations(rightIdx);
        List<List<Integer>> leftSubsets = ComboUtils.subsets(leftIdx, size);

        Matrix costs = new Matrix(this.size(), model.size());
        for (int i = 0; i < this.size(); i++)
        {
            for (int j = 0; j < model.size(); j++)
            {
                costs.set(i, j, this.getLine(i).angleLineDeg(model.getLine(j)));
            }
        }

        double minDist = Double.MAX_VALUE;
        List<Integer> minLeftSubset = Lists.newArrayList();
        List<Integer> minRightPermutation = Lists.newArrayList();
        for (List<Integer> leftSubset : leftSubsets)
        {
            for (List<Integer> rightPermutation : rightPermutations)
            {
                double dist = 0;
                for (int idx = 0; idx < size; idx++)
                {
                    dist += costs.get(leftSubset.get(idx), rightPermutation.get(idx));
                }

                if (dist < minDist)
                {
                    minDist = dist;
                    minLeftSubset = leftSubset;
                    minRightPermutation = rightPermutation;
                }
            }
        }

        Map<Integer, Integer> pi = Maps.newHashMap();
        for (int i = 0; i < size; i++)
        {
            pi.put(minLeftSubset.get(i), minRightPermutation.get(i));
        }

        return pi;
    }

    public double errorMissing(Fibers model, double thresh)
    {
        return Math.max(0, this.count(thresh) - model.count(thresh));
    }

    public double errorExtra(Fibers model, double thresh)
    {
        return Math.max(0, model.count(thresh) - this.count(thresh));
    }

    public double errorFracIso(Fibers model)
    {
        return Math.abs(this.getFracIso() - model.getFracIso());
    }

    public double errorFracTotal(Fibers model, double thresh)
    {
        double out = 0;

        Map<Integer, Integer> match = this.match(model, thresh);
        for (int i = 0; i < this.size(); i++)
        {
            double leftFrac = this.getFrac(i);
            if (leftFrac > thresh)
            {
                if (match.containsKey(i))
                {
                    double rightFrac = model.getFrac(match.get(i));
                    out += Math.abs(leftFrac - rightFrac);
                }
                else
                {
                    out += leftFrac;
                }
            }
        }

        return out;
    }

    public double errorFracHaus(Fibers model, double thresh)
    {
        double error = 0.0;

        Map<Integer, Integer> match = this.match(model, thresh);
        for (int i = 0; i < this.size(); i++)
        {
            double leftFrac = this.getFrac(i);
            if (leftFrac > thresh)
            {
                if (match.containsKey(i))
                {
                    double rightFrac = model.getFrac(match.get(i));
                    error = Math.max(error, Math.abs(leftFrac - rightFrac));
                }
                else
                {
                    error = Math.max(error, leftFrac);
                }
            }
        }

        return error;
    }

    public double errorLineTotal(Fibers model, double thresh)
    {
        double out = 0;

        Map<Integer, Integer> match = this.match(model, thresh);
        for (int i = 0; i < this.size(); i++)
        {
            double leftFrac = this.getFrac(i);
            Vect leftLine = this.getLine(i);

            if (leftFrac > thresh)
            {
                if (match.containsKey(i))
                {
                    Vect rightLine = model.getLine(match.get(i));
                    double angle = leftLine.angleLineDeg(rightLine);

                    out += angle;
                }
                else
                {
                    out += NO_MATCH_ANGLE;
                }
            }
        }

        return out;
    }

    public double errorLineHaus(Fibers model, double thresh)
    {
        double out = 0;

        Map<Integer, Integer> match = this.match(model, thresh);
        for (int i = 0; i < this.size(); i++)
        {
            double leftFrac = this.getFrac(i);
            Vect leftLine = this.getLine(i);

            if (leftFrac > thresh)
            {
                if (match.containsKey(i))
                {
                    Vect rightLine = model.getLine(match.get(i));
                    double angle = leftLine.angleLineDeg(rightLine);

                    out = Math.max(out, angle);
                }
                else
                {
                    out = Math.max(out, NO_MATCH_ANGLE);
                }
            }
        }

        return out;
    }

    public static double rmse(Vect signal, Gradients gradients, Vect param)
    {
        return rmse(signal, gradients, new Fibers(param));
    }

    public static double rmse(Vect signal, Gradients gradients, Fibers fibers)
    {
        double s0 = fibers.base;
        double d = fibers.diff;
        double mse = 0;

        for (int i = 0; i < gradients.size(); i++)
        {
            double b = gradients.getBval(i);
            Vect g = gradients.getBvec(i).normalize();
            double ndb = -d * b;

            double si = 0;
            double sumf = 0;

            for (int j = 0; j < fibers.size(); j++)
            {
                double f = fibers.fracs.get(j);
                Vect u = fibers.lines.get(j);
                double gdotu = g.dot(u);
                double arg = ndb * gdotu * gdotu;
                si += f * s0 * Math.exp(arg);
                sumf += f;
            }

            si += (1 - sumf) * s0 * Math.exp(ndb);

            double ds = si - signal.get(i);
            mse += ds * ds / gradients.size();
        }

        double rmse = Math.sqrt(mse);

        return rmse;
    }

    public List<String> features()
    {
        List<String> out = Lists.newArrayList();
        out.add(BASE);
        out.add(DIFF);
        out.add(FRAC);
        for (int i = 0; i < this.size(); i++)
        {
            out.add(LINE + (i + 1));
            out.add(RGB + (i + 1));
            out.add(FRAC + (i + 1));
            out.add(STAT + (i + 1));
            out.add(LABEL + (i + 1));
        }
        out.add(FISO);

        return out;
    }

    public Vect feature(String name)
    {
        if (BASE.equals(name))
        {
            return VectSource.create1D(this.base);
        }
        if (DIFF.equals(name))
        {
            return VectSource.create1D(this.diff);
        }
        if (FRAC.equals(name))
        {
            return VectSource.create1D(this.fracs.sum());
        }
        if (FISO.equals(name))
        {
            return VectSource.create1D(this.getFracIso());
        }
        if (name != null)
        {
            for (int i = 0; i < this.size(); i++)
            {
                if (name.equals(LINE + (i + 1)))
                {
                    return this.lines.get(i);
                }
                if (name.equals(RGB + (i + 1)))
                {
                    return this.lines.get(i).abs().times(this.fracs.get(i));
                }
                if (name.equals(FRAC + (i + 1)))
                {
                    return VectSource.create1D(this.fracs.get(i));
                }
                if (name.equals(STAT + (i + 1)))
                {
                    return VectSource.create1D(this.stats.get(i));
                }
                if (name.equals(LABEL + (i + 1)))
                {
                    return VectSource.create1D(this.labels[i]);
                }
                if (name.equals(ICVF + (i + 1)))
                {
                    return VectSource.create1D((1.0 - this.getFracIso()) * this.fracs.get(i));
                }
            }
        }

        throw new RuntimeException("invalid index: " + name);
    }

    public Fibers getThis()
    {
        return this;
    }

    public Vect synth(final Gradients gradients)
    {
        return synth(gradients, null, false);
    }

    public Vect synth(final Gradients gradients, Double dperp, boolean tort)
    {
        Vect output = VectSource.createND(gradients.size());
        for (int i = 0; i < gradients.size(); i++)
        {
            double b = gradients.getBval(i);
            Vect g = gradients.getBvec(i);
            double d = this.getDiffusivity();
            double s = this.getBaseline();

            double v = 0;

            double f0 = 1 - this.getFracs().sum();
            double dext = tort ? f0 * d : d;
            double iso = s * f0 * Math.exp(-b * dext);
            v += iso;

            for (int j = 0; j < this.size(); j++)
            {
                double dot = this.getLine(j).dot(g);
                double dot2 = dot * dot;
                double decay = Math.exp(-b * d * dot2);

                if (dperp != null && dperp > 0)
                {
                    decay *= Math.exp(-b * 2.0 * dperp * d * (1.0 - dot2));
                }

                double fib = s * this.getFrac(j) * decay;

                v += fib;
            }

            output.set(i, v);
        }

        return output;
    }

    public static VectFunction synther(int comps, final Gradients gradients)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                output.set(new Fibers(input).synth(gradients));
            }
        }.init(new Fibers(comps).getEncodingSize(), gradients.size());
    }
}