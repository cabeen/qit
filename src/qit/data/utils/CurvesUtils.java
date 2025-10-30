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

package qit.data.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.base.utils.JavaUtils;
import qit.data.datasets.*;
import qit.data.datasets.Record;
import qit.data.datasets.Curves.Curve;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.curves.CurvesFunctionApply;
import qit.data.utils.curves.CurvesSelector;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.*;
import qit.math.source.SelectorSource;
import qit.math.utils.MathUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * utilties for processing curves
 */
public class CurvesUtils
{
    public static Curves waypoints(Curves curves, Mask mask)
    {
        List<Integer> idx = MaskUtils.listNonzero(mask);
        Global.assume(idx.size() != 0, "invalid waypoints");

        for (int i : idx)
        {
            Mask submask = MaskUtils.equal(mask, i);
            Containable selector = SelectorSource.mask(submask);
            curves = new CurvesSelector().withCurves(curves).withSelector(selector).getOutput();
        }

        return curves;
    }

    public static boolean[] selectBy(Curves curves, Containable selector, boolean exclude)
    {
        boolean[] bs = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            boolean hit = false;
            for (Vect p : curve)
            {
                if (selector.contains(p))
                {
                    hit = true;
                    break;
                }
            }

            bs[i] = hit ^ exclude;
        }
        return bs;
    }


    public static void remove(Curves curves, Curves sub, double thresh, Distance<Curve> dist)
    {
        Logging.info("remove curves from set of size " + sub.size());

        int step = 5;
        int start = curves.size();
        Logging.info("starting with " + start + " curves");
        for (int i = 0; i < sub.size(); i++)
        {
            if (i % step == 0)
            {
                Logging.info("checking curve " + i);
            }

            Curve s = sub.get(i);
            List<Integer> remove = Lists.newArrayList();
            for (int j = 0; j < curves.size(); j++)
            {
                if (dist.dist(s, curves.get(j)) < thresh)
                {
                    remove.add(j);
                }
            }

            for (Integer j : remove)
            {
                curves.remove(j);
            }
        }
        int end = curves.size();
        Logging.info("finished with " + end + " curves");
        Logging.info("removed " + (start - end) + " curves");
    }

    public static void remove(Curves curves, Curves sub)
    {
        // assume curves are approximately equal
        Logging.info("remove curves from set of size " + sub.size());

        int step = 100;
        int start = curves.size();
        Logging.info("starting with " + start + " curves");
        for (int i = 0; i < sub.size(); i++)
        {
            if (i % step == 0)
            {
                Logging.info("checking curve " + i);
            }

            Curve s = sub.get(i);
            Vect shead = s.getHead();
            Vect stail = s.getTail();

            List<Integer> remove = Lists.newArrayList();
            for (int j = 0; j < curves.size(); j++)
            {
                Curve c = curves.get(j);
                Vect chead = c.getHead();
                Vect ctail = c.getTail();

                double dhh = shead.dist(chead);
                double dtt = stail.dist(ctail);
                double dht = shead.dist(ctail);
                double dth = stail.dist(chead);

                double da = Math.max(dhh, dtt);
                double db = Math.max(dht, dth);

                double d = Math.min(da, db);

                if (d < 1e-3)
                {
                    remove.add(j);
                }
            }

            Collections.reverse(remove);
            ;
            for (Integer j : remove)
            {
                curves.remove(j);
            }
        }
        int end = curves.size();
        Logging.info("finished with " + end + " curves");
        Logging.info("removed " + (start - end) + " curves");
    }

    public static int[] sort(Curves curves, int[] label)
    {
        Map<Integer, Double> weights = Maps.newHashMap();
        for (int i = 0; i < label.length; i++)
        {
            int lab = label[i];
            double val = curves.get(i).length();

            double prev = weights.containsKey(lab) ? weights.get(lab) : 0;
            weights.put(lab, prev + val);
        }

        Map<Integer, Integer> lookup = MathUtils.sort(weights);

        int[] out = new int[label.length];
        for (int i = 0; i < label.length; i++)
        {
            out[i] = lookup.get(label[i]);
        }

        return out;
    }

    public static Matrix distance(Curves curves, Distance<Curve> dist)
    {
        int num = curves.size();
        Matrix mat = new Matrix(num, num);

        Logging.info("started computing curve distance matrix");
        int total = num * num / 2;
        int percent = 0;
        int count = 0;

        for (int i = 0; i < num; i++)
        {
            for (int j = i + 1; j < num; j++)
            {
                count += 1;
                int p = 100 * count / total;
                if (p > percent)
                {
                    percent = p;
                    Logging.info("processed " + p + " percent");
                }

                double d = dist.dist(curves.get(i), curves.get(j));

                mat.set(i, j, d);
                mat.set(j, i, d);
            }
        }
        Logging.info("finished computing distance matrix");
        return mat;
    }

    public static List<Integer> list(Curves curves, String attr)
    {
        if (!curves.has(attr))
        {
            return Lists.newArrayList();
        }

        Set<Integer> set = Sets.newHashSet();

        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                int val = MathUtils.round(curve.get(attr, i).get(0));
                set.add(val);
            }
        }

        List<Integer> list = Lists.newArrayList(set);
        Collections.sort(list);

        return list;
    }

    public static Record measure(Curves curves, String attr, int idx)
    {
        Record out = new Record();

        for (String name : curves.names())
        {
            if (name.equals(Curves.COLOR) || name.equals(Curves.OPACITY))
            {
                continue;
            }

            if (curves.dim(name) == 1)
            {
                Vects cat = new Vects();
                for (Curve curve : curves)
                {
                    for (int i = 0; i < curve.size(); i++)
                    {
                        int val = MathUtils.round(curve.get(attr, i).get(0));
                        if (val == idx)
                        {
                            cat.add(curve.get(name, i));
                        }
                    }
                }

                if (cat.size() > 0)
                {
                    VectsStats stats = new VectsStats().withInput(cat).run();

                    out.with(name + "_mean", String.valueOf(stats.mean.get(0)));
                    out.with(name + "_var", String.valueOf(stats.var.get(0)));
                    out.with(name + "_std", String.valueOf(stats.std.get(0)));
                    out.with(name + "_cv", String.valueOf(stats.cv.get(0)));
                    out.with(name + "_min", String.valueOf(stats.min.get(0)));
                    out.with(name + "_qlow", String.valueOf(stats.qlow.get(0)));
                    out.with(name + "_median", String.valueOf(stats.median.get(0)));
                    out.with(name + "_qhigh", String.valueOf(stats.qhigh.get(0)));
                    out.with(name + "_max", String.valueOf(stats.max.get(0)));
                    out.with(name + "_iqr", String.valueOf(stats.iqr.get(0)));
                    out.with(name + "_sum", String.valueOf(stats.sum.get(0)));
                    out.with(name + "_num", String.valueOf(stats.num.get(0)));
                }
            }
        }

        return out;
    }

    public static Mask labels(Sampling sampling, Curves curves, String attr)
    {
        List<Integer> labels = CurvesUtils.list(curves, attr);
        Map<Integer, Integer> labelToIndex = JavaUtils.reverseLookup(labels);

        Volume probs = VolumeSource.create(sampling, labels.size());
        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                Vect pos = curve.get(i);

                if (sampling.contains(pos))
                {
                    Integer label = curve.getint(attr, i);
                    int idx = labelToIndex.get(label);
                    Sample sample = sampling.nearest(pos);

                    probs.set(sample, idx, probs.get(sample, idx) + 1);
                }
            }
        }

        Mask out = new Mask(sampling);
        for (Sample sample : sampling)
        {
            Vect prob = probs.get(sample);
            if (MathUtils.nonzero(prob.sum()))
            {
                out.set(sample, labels.get(prob.maxidx()));
            }
        }

        return out;
    }

    public static VectOnlineStats stats(Curves curves, String attr)
    {
        Global.assume(curves.proto(attr).size() == 1, "invalid attribute");

        VectOnlineStats stats = new VectOnlineStats();
        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                double v = curve.get(attr, i).get(0);
                stats.update(v);
            }
        }

        return stats;
    }

    public static double volume(Curves curves)
    {
        double delta = 1.0;
        Box box = curves.bounds().scale(1.25);
        Sampling sampling = SamplingSource.create(box, delta);
        double volume = CurvesUtils.volume(curves, sampling, 0.5);
        return volume;
    }

    public static double volume(Curves curves, Sampling ref, double thresh)
    {
        Volume den = VolumeUtils.density(ref, curves, false);
        VolumeThreshold module = new VolumeThreshold();
        module.input = den;
        module.threshold = thresh;
        Mask mask = module.run().output;
        double volume = MaskUtils.volume(mask);

        return volume;
    }

    public static boolean[] selectByEndpoints(Curves curves, Mask mask)
    {
        Sampling sampling = mask.getSampling();
        boolean[] bs = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            Vect head = curve.getHead();
            Vect tail = curve.getTail();
            boolean phead = mask.foreground(sampling.nearest(head));
            boolean ptail = mask.foreground(sampling.nearest(tail));

            bs[i] = phead | ptail;
        }
        return bs;
    }

    public static boolean[] selectByEndpoints(Curves curves, VectFunction prob, double thresh)
    {
        boolean[] bs = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            Vect head = curve.getHead();
            Vect tail = curve.getTail();

            boolean phead = prob.apply(head).get(0) > thresh;
            boolean ptail = prob.apply(tail).get(0) > thresh;

            bs[i] = phead | ptail;
        }
        return bs;
    }

    public static boolean[] selectByMinLength(Curves curves, double length)
    {
        boolean[] bs = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            bs[i] = curves.get(i).length() > length;
        }
        return bs;
    }

    public static boolean[] selectByMaxLength(Curves curves, double length)
    {
        boolean[] bs = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            bs[i] = curves.get(i).length() < length;
        }
        return bs;
    }

    public static boolean[] selectByMeanAttr(Curves curves, String attr, double threshold)
    {
        boolean[] bs = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            Vect sum = curve.integral(attr);
            double length = curve.length();
            double mean = sum.get(0) / length;

            bs[i] = mean > threshold;
        }
        return bs;
    }

    public static boolean[] selectByAttr(Curves curves, String attr, Vect value)
    {
        boolean[] bs = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            for (int j = 0; j < curve.size(); j++)
            {
                if (value.equals(curve.get(attr, j)))
                {
                    bs[i] = true;
                    break;
                }
            }
        }
        return bs;
    }

    public static boolean[] selectByCount(Curves curves, int n)
    {
        n = Math.min(curves.size(), n);
        boolean[] bs = new boolean[curves.size()];
        for (int i : MathUtils.subset(curves.size(), n))
        {
            bs[i] = true;
        }

        return bs;
    }

    public static void attrSet(Curves curves, Vect vect, String attr)
    {
        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                curve.set(attr, i, vect);
            }
        }
    }

    public static void attrSet(Curve curve, Vect vect, String attr)
    {
        for (int i = 0; i < curve.size(); i++)
        {
            curve.set(attr, i, vect);
        }
    }

    public static void attrSet(Curves curves, Vects vects, String attr)
    {
        int idx = 0;
        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                curve.set(attr, i, vects.get(idx++));
            }
        }
    }

    public static int[] attrGetLabelsPerCurve(Curves curves, String attr)
    {
        Global.assume(curves.has(attr), "labels must exist");

        int[] out = new int[curves.size()];
        for (int i = 0; i < out.length; i++)
        {
            // assume the first vertex will give us the label
            out[i] = (int) Math.round(curves.get(i).get(attr, 0).get(0));
        }

        return out;
    }

    public static Vects attrGet(Curves curves, String attr)
    {
        int guess = curves.size() * curves.get(0).size();
        Vects vects = new Vects(guess);
        for (Curve curve : curves)
        {
            for (Vect val : curve.get(attr))
            {
                vects.add(val);
            }
        }

        return new Vects(vects);
    }

    public static void attrRename(Curves curves, String from, String to)
    {
        curves.rename(from, to);
    }

    public static void attrCopy(Curves curves, String from, String to)
    {
        if (!curves.has(to))
        {
            curves.add(to, curves.proto(from));
        }

        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                curve.set(to, i, curve.get(from, i));
            }
        }
    }

    public static void attrToCoord(Curves curves, String attr)
    {
        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                curve.set(Curves.COORD, i, curve.get(attr, i));
            }
        }
    }

    public static void attrKeep(Curves curves, String attr)
    {
        for (String n : curves.names())
        {
            if (!n.equals(Curves.COORD) && !attr.equals(n))
            {
                curves.remove(n);
            }
        }
    }

    public static void attrRemove(Curves curves, String attr)
    {
        curves.remove(attr);
    }

    public static void attrSetFraction(Curves curves)
    {
        for (Curve curve : curves)
        {
            if (curve.size() == 0)
            {
                continue;
            }
            else if (curve.size() == 1)
            {
                curve.set(Curves.FRACTION, 0, VectSource.create1D(0));
            }
            else
            {
                Vect t = curve.cumlength();
                t.timesEquals(1.0 / t.last());

                Vect buff = new Vect(1);

                if (!curve.has(Curves.FRACTION))
                {
                    curve.add(Curves.FRACTION, buff);
                }

                for (int i = 0; i < curve.size(); i++)
                {
                    buff.set(0, t.get(i));
                    curve.set(Curves.FRACTION, i, buff);
                }
            }
        }
    }

    public static void attrSetPerVert(Curve curve, String name, Vects values)
    {
        Global.assume(curve.size() == values.size(), "structure sizes do not match");

        if (!curve.has(name))
        {
            curve.add(name, values.get(0).proto());
        }

        for (int j = 0; j < curve.size(); j++)
        {
            curve.set(name, j, values.get(j));
        }
    }

    public static void attrSetIndex(Curves curves)
    {
        for (Curve curve : curves)
        {
            Vect buff = new Vect(1);

            if (!curve.has(Curves.INDEX))
            {
                curve.add(Curves.INDEX, buff);
            }

            for (int i = 0; i < curve.size(); i++)
            {
                buff.set(0, i);
                curve.set(Curves.INDEX, i, buff);
            }
        }
    }

    public static void attrSetArclength(Curves curves)
    {
        for (Curve curve : curves)
        {
            Vect t = curve.cumlength();

            Vect buff = new Vect(1);

            if (!curve.has(Curves.ARCLENGTH))
            {
                curve.add(Curves.ARCLENGTH, buff);
            }

            for (int i = 0; i < curve.size(); i++)
            {
                buff.set(0, t.get(i));
                curve.set(Curves.ARCLENGTH, i, buff);
            }
        }
    }

    public static void attrSetColor(Curves curves)
    {
        CurvesUtils.attrSetTangent(curves);
        new CurvesFunctionApply().withCurves(curves).withFunction(VectFunctionSource.rgb()).withInput(Curves.TANGENT).withOutput(Curves.COLOR).run();
    }

    public static void attrSetTangent(Curves curves)
    {
        curves.add(Curves.TANGENT, VectSource.create3D());

        for (Curve curve : curves)
        {
            if (curve.size() < 2)
            {
                continue;
            }

            // use forward approximation at head
            Vect head_curr = curve.get(0);
            Vect head_next = curve.get(1);
            Vect head_tangent = head_next.minus(head_curr).normalize();
            curve.set(Curves.TANGENT, 0, head_tangent);

            // use backward approximation at tail
            int tail_idx = curve.size() - 1;
            Vect tail_prev = curve.get(tail_idx - 1);
            Vect tail_curr = curve.get(tail_idx);
            Vect tail_tangent = tail_curr.minus(tail_prev).normalize();
            curve.set(Curves.TANGENT, tail_idx, tail_tangent);

            // use central approximation at interior
            for (int i = 1; i < curve.size() - 1; i++)
            {
                int pi = i - 1;
                int ni = i + 1;
                Vect prev = curve.get(pi);
                Vect curr = curve.get(i);
                Vect next = curve.get(ni);

                // compute the average of the mousePrev and next segments
                Vect pdel = curr.minus(prev);
                Vect ndel = next.minus(curr);
                Vect tangent = pdel.plus(ndel).times(2.0).normalize();

                // add the attribute
                curve.set(Curves.TANGENT, i, tangent);
            }
        }
    }

    public static void attrSetAll(Curves curves, String name, Vect vect)
    {
        if (!curves.has(name))
        {
            curves.add(name, vect.proto());
        }

        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);

            for (int j = 0; j < curve.size(); j++)
            {
                curve.set(name, j, vect);
            }
        }
    }

    public static void attrSetPerCurve(Curves curves, String name, Vect vect)
    {
        Global.assume(curves.size() == vect.size(), "curves and vector do not match");

        if (!curves.has(name))
        {
            curves.add(name, VectSource.create1D());
        }

        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            Vect value = VectSource.create1D(vect.get(i));

            for (int j = 0; j < curve.size(); j++)
            {
                curve.set(name, j, value);
            }
        }
    }

    public static void attrSetPerCurve(Curves curves, String name, double[] values)
    {
        Global.assume(curves.size() == values.length, "curves and vector do not match");

        if (!curves.has(name))
        {
            curves.add(name, VectSource.create1D());
        }

        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            Vect value = VectSource.create1D(values[i]);

            for (int j = 0; j < curve.size(); j++)
            {
                curve.set(name, j, value);
            }
        }
    }

    public static void attrSetPerCurve(Curves curves, String name, Vects vects)
    {
        Global.assume(curves.size() == vects.size(), "vectors do not match curves");

        if (curves.size() == 0)
        {
            return;
        }

        if (!curves.has(name))
        {
            curves.add(name, vects.get(0).proto());
        }

        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            Vect projv = vects.get(i);

            for (int j = 0; j < curve.size(); j++)
            {
                curve.set(name, j, projv);
            }
        }
    }

    public static void attrSetLabelsPerCurve(Curves curves, String name, int[] labels)
    {
        if (!curves.has(name))
        {
            curves.add(name, VectSource.create1D(0.0));
        }

        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            for (int j = 0; j < curve.size(); j++)
            {
                curve.set(name, j, VectSource.create1D(labels[i]));
            }
        }
    }
}
