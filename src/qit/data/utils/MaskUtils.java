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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.data.datasets.*;
import qit.data.datasets.Record;
import qit.data.datasets.Curves.Curve;
import qit.data.modules.mask.MaskComponents;
import qit.data.modules.mask.MaskDilate;
import qit.data.modules.mask.MaskErode;
import qit.data.modules.mask.MaskMeasure;
import qit.data.source.MaskSource;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Box;
import qit.math.structs.Containable;
import qit.math.structs.Sphere;
import qit.math.utils.MathUtils;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * utilties for processing masks
 */
public class MaskUtils
{
    public static Multimap<Integer, Integer> rag(Mask mask, boolean full)
    {
        List<Integers> neighborhood = full ? Global.NEIGHBORS_27 : Global.NEIGHBORS_6;
        Multimap<Integer, Integer> rag = HashMultimap.create();

        for (Sample sample : mask.getSampling())
        {
            if (!mask.valid(sample, mask))
            {
                continue;
            }

            int label = mask.get(sample);

            for (Integers neighbor : neighborhood)
            {
                Sample nsample = new Sample(sample, neighbor);

                if (!mask.valid(nsample))
                {
                    continue;
                }

                int nlabel = mask.get(nsample);

                if (nlabel != label)
                {
                    rag.put(label, nlabel);
                    rag.put(nlabel, label);
                }
            }
        }

        return rag;
    }


    public static Mask zoom(Mask input, double fi, double fj, double fk)
    {
        Sampling sampling = input.getSampling();
        Sampling zoomSampling = sampling.zoom(fi, fj, fk);
        Mask out = MaskSource.create(zoomSampling);
        for (Sample zoomSample : zoomSampling)
        {
            Sample lowSample = sampling.nearest(zoomSampling.world(zoomSample));
            out.set(zoomSample, input.get(lowSample));
        }
        return out;
    }

    public static Mask zoom(Mask input, double factor)
    {
        return zoom(input, factor, factor, factor);
    }

    public static Mask crop(Mask mask, Sampling sampling)
    {
        Sampling psampling = mask.getSampling();
        Mask out = new Mask(sampling);
        for (Sample sample : sampling)
        {
            Sample psample = psampling.nearest(sampling.world(sample));
            out.set(sample, mask.get(psample));
        }

        return out;
    }

    public static List<Sample> samples(Mask mask)
    {
        List<Sample> out = Lists.newArrayList();
        for (Sample sample : mask.getSampling())
        {
            if (mask.foreground(sample))
            {
                out.add(sample);
            }
        }
        return out;
    }

    public static Mask select(Mask ref, Containable select)
    {
        Mask out = ref.proto();

        Sampling sampling = out.getSampling();
        for (Sample sample : sampling)
        {
            if (select.contains(sampling.world(sample)))
            {
                out.set(sample, 1);
            }
        }

        return out;
    }

    public static void set(Mask mask, Containable select, int label)
    {
        Sampling sampling = mask.getSampling();
        for (Sample sample : sampling)
        {
            if (select.contains(sampling.world(sample)))
            {
                mask.set(sample, label);
            }
        }
    }

    public static void set(Mask mask, Solids solids, int label)
    {
        // this is more efficient than using a containable

        Sampling sampling = mask.getSampling();

        for (int i = 0; i < solids.numSpheres(); i++)
        {
            Sphere sphere = solids.getSphere(i);
            for (Sample sample : sampling.iterateBox(sphere.box()))
            {
                if (sphere.contains(sampling.world(sample)))
                {
                    mask.set(sample, label);
                }
            }
        }

        for (int i = 0; i < solids.numBoxes(); i++)
        {
            Box box = solids.getBox(i);
            for (Sample sample : sampling.iterateBox(box))
            {
                if (box.contains(sampling.world(sample)))
                {
                    mask.set(sample, label);
                }
            }
        }
    }

    public static double dice(Mask a, Mask b)
    {
        Global.assume(a.getSampling().equals(b.getSampling()), "samplings must match");

        int dab = 0;
        int sab = 0;

        for (Sample s : a.getSampling())
        {
            boolean va = MathUtils.nonzero(a.get(s));
            boolean vb = MathUtils.nonzero(b.get(s));

            if (va && vb)
            {
                dab += 1;
            }

            if (va)
            {
                sab += 1;
            }

            if (vb)
            {
                sab += 1;
            }
        }

        double dice = sab == 0 ? 1 : 2.0 * dab / sab;

        return dice;
    }

    public static Mask mask(Mask left, Mask right)
    {
        if (right == null)
        {
            return left.copy();
        }

        Global.assume(left.getSampling().equals(right.getSampling()), "samplings must match");

        Mask out = left.proto();
        for (Sample sample : left.getSampling())
        {
            if (left.foreground(sample) && right.foreground(sample))
            {
                out.set(sample, left.get(sample));
            }
        }

        return out;
    }

    public Volume logistic(Volume volume, Mask seg, Mask fg)
    {
        int iters = 100;
        double alpha = 0.0;
        double beta = 1.0;
        double perror = Double.MAX_VALUE;

        for (int iter = 0; iter < iters; iter++)
        {
            double gradA = 0;
            double gradB = 0;
            double hessAA = 0;
            double hessAB = 0;
            double hessBB = 0;
            double error = 0;
            int count = 0;

            for (Sample sample : volume.getSampling())
            {
                if (volume.valid(sample, fg))
                {
                    double x = volume.get(sample, 0);
                    double y = seg.get(sample);

                    double d = alpha + beta * x;
                    double p = 1.0 / (1.0 + Math.exp(-d));
                    double e = y - p;

                    gradA += e;
                    gradB += x * e;

                    double h = p * (1.0 - p);
                    hessAA += h;
                    hessAB += h * x;
                    hessBB += h * x * x;

                    error += Math.abs(e);
                    count += 1;
                }
            }

            Matrix hess = new Matrix(2, 2);
            hess.set(0, 0, hessAA);
            hess.set(1, 0, hessAB);
            hess.set(0, 1, hessAB);
            hess.set(1, 1, hessBB);

            try
            {
                Vect delta = hess.inv().times(VectSource.create2D(gradA, gradB));

                alpha += delta.get(0);
                beta += delta.get(1);
                error /= count;

                Logging.info("... iteration = " + iter);
                Logging.info("... error = " + error);
                Logging.info("... alpha = " + alpha);
                Logging.info("... beta = " + beta);

                if (Math.abs(error - perror) < 1e-3)
                {
                    break;
                }

                perror = error;
            }
            catch (RuntimeException e)
            {
                break;
            }
        }

        Volume out = volume.proto(1);
        for (Sample sample : volume.getSampling())
        {
            if (volume.valid(sample, fg))
            {
                double x = volume.get(sample, 0);
                double d = alpha + beta * x;
                out.set(sample, 0, 1.0 / (1.0 + Math.exp(-d)));
            }
        }

        return out;
    }

    public static Mask and(Mask left, Mask right)
    {
        if (right == null)
        {
            return left.copy();
        }

        if (left == null)
        {
            return right.copy();
        }

        Global.assume(left.getSampling().equals(right.getSampling()), "samplings must match");

        Mask out = left.proto();
        for (Sample sample : left.getSampling())
        {
            int leftLabel = left.get(sample);
            int rightLabel = right.get(sample);
            if (leftLabel != 0 && rightLabel != 0)
            {
                out.set(sample, Math.max(leftLabel, rightLabel));
            }
        }

        return out;
    }

    public static Mask or(Mask left, Mask right)
    {
        Global.assume(left.getSampling().equals(right.getSampling()), "samplings must match");

        Mask out = left.proto();
        for (Sample sample : left.getSampling())
        {
            int leftLabel = left.get(sample);
            int rightLabel = right.get(sample);
            int newLabel = leftLabel != 0 ? leftLabel : rightLabel;

            out.set(sample, newLabel);
        }

        return out;
    }

    public static Mask distinct(Mask left, Mask right)
    {
        Global.assume(left.getSampling().equals(right.getSampling()), "samplings must match");

        Mask out = left.proto();
        for (Sample sample : left.getSampling())
        {
            int leftLabel = left.get(sample);
            int rightLabel = right.get(sample);
            int newLabel = leftLabel != 0 ? 1 : rightLabel != 0 ? 2 : 0;

            out.set(sample, newLabel);
        }

        return out;
    }

    public static Mask combine(Mask left, Mask right, boolean max)
    {
        Global.assume(left.getSampling().equals(right.getSampling()), "samplings must match");

        List<Integer> leftLables = MaskUtils.listNonzero(left);
        List<Integer> rightLables = MaskUtils.listNonzero(right);

        Map<Integer, Integer> leftMap = Maps.newHashMap();
        Map<Integer, Integer> rightMap = Maps.newHashMap();

        int count = 1;

        leftMap.put(0, 0);
        for (Integer label : leftLables)
        {
            leftMap.put(label, count);
            count += 1;
        }

        rightMap.put(0, 0);
        for (Integer label : rightLables)
        {
            rightMap.put(label, count);
            count += 1;
        }

        Mask out = left.proto();
        for (Sample sample : left.getSampling())
        {
            int leftLabel = leftMap.get(left.get(sample));
            int rightLabel = rightMap.get(right.get(sample));

            if (max && (leftLabel != 0 || rightLabel != 0))
            {
                out.set(sample, Math.max(leftLabel, rightLabel));
            }
            else
            {
                if (leftLabel != 0)
                {
                    out.set(sample, leftLabel);
                }

                if (rightLabel != 0)
                {
                    out.set(sample, rightLabel);
                }
            }
        }

        return out;
    }

    public static Mask invert(Mask input, Mask mask)
    {
        Mask out = input.proto();
        for (Sample sample : input.getSampling())
        {
            if (input.valid(sample, mask))
            {
                if (input.get(sample) == 0)
                {
                    out.set(sample, 1);
                }
                else
                {
                    out.set(sample, 0);
                }
            }
        }

        return out;
    }

    public static Mask invert(Mask volume)
    {
        Mask out = volume.proto();
        for (Sample sample : volume.getSampling())
        {
            if (volume.get(sample) == 0)
            {
                out.set(sample, 1);
            }
            else
            {
                out.set(sample, 0);
            }
        }

        return out;
    }

    public static int mode(Mask volume)
    {
        Map<Integer, Integer> counts = counts(volume);

        int label = 0;
        Integer count = null;
        for (Integer k : counts.keySet())
        {
            int c = counts.get(k);
            if (count == null || c > count)
            {
                label = k;
                count = c;
            }
        }

        return label;
    }

    public static Map<Integer, Integer> counts(Mask volume)
    {
        Map<Integer, Integer> counts = Maps.newHashMap();
        for (Sample sample : volume.getSampling())
        {
            int val = volume.get(sample);

            if (val == 0)
            {
                continue;
            }

            if (counts.containsKey(val))
            {
                counts.put(val, counts.get(val) + 1);
            }
            else
            {
                counts.put(val, 1);
            }
        }

        return counts;
    }

    public static Mask map(Mask volume, Map<Integer, Integer> map)
    {
        Mask out = volume.proto();

        for (Sample sample : volume.getSampling())
        {
            int val = volume.get(sample);
            if (map.containsKey(val))
            {
                out.set(sample, map.get(val));
            }
            else
            {
                out.set(sample, 0);
            }
        }

        return out;
    }

    public static Mask filter(Mask volume, Integer thresh)
    {
        Map<Integer, Integer> counts = counts(volume);
        Map<Integer, Integer> lookup = MathUtils.remap(counts);

        Mask out = volume.proto();
        for (Sample sample : volume.getSampling())
        {
            int val = volume.get(sample);

            if (!counts.containsKey(val) || counts.get(val) < thresh)
            {
                out.set(sample, 0);
            }
            else
            {
                out.set(sample, lookup.get(val));
            }
        }

        return out;
    }

    public static Mask compress(Mask[] volumes)
    {
        Mask out = volumes[0].proto();
        for (Sample sample : out.getSampling())
        {
            for (int i = 0; i < volumes.length; i++)
            {
                if (volumes[i].get(sample) != 0)
                {
                    out.set(sample, i + 1);
                    break;
                }
            }
        }

        return out;
    }

    public static Mask erode(Mask mask, int num)
    {
        MaskErode run = new MaskErode();
        run.input = mask;
        run.num = num;
        return run.run().output;
    }

    public static Mask dilate(Mask mask, int num)
    {
        MaskDilate run = new MaskDilate();
        run.input = mask;
        run.num = num;
        return run.run().output;
    }

    public static Mask disjoint(Mask[] volumes)
    {
        // join labels from multiple volumes giving unique labels to
        // combinations. do this by converting the sequence of input values at
        // each voxel to a binary number. this might be bigger than 64-bits, so
        // use big numbers and only use combinations that have been seen (we
        // assume the combinations are very sparse, that is count << N * N)

        int maxlabel = 0;
        Map<BigInteger, Integer> lookup = Maps.newHashMap();
        Mask out = volumes[0].proto();
        for (Sample sample : out.getSampling())
        {
            BigInteger idx = BigInteger.valueOf(0);
            for (int i = 0; i < volumes.length; i++)
            {
                if (volumes[i].get(sample) != 0)
                {
                    idx = idx.add(BigInteger.valueOf(2).shiftLeft(i));
                }
            }

            if (lookup.containsKey(idx))
            {
                out.set(sample, lookup.get(idx));
            }
            else
            {
                maxlabel += 1;
                lookup.put(idx, maxlabel);
                out.set(sample, maxlabel);
            }
        }

        return out;
    }

    public static List<Mask> expand(Mask volume, List<Integer> idx)
    {
        List<Mask> out = Lists.newArrayList();

        Map<Integer, Integer> lut = Maps.newHashMap();
        for (int i = 0; i < idx.size(); i++)
        {
            out.add(volume.proto());
            lut.put(idx.get(i), i);
        }

        for (Sample sample : volume.getSampling())
        {
            int value = volume.get(sample);
            if (lut.containsKey(value))
            {
                out.get(lut.get(value)).set(sample, 1);
            }
        }

        return out;
    }

    // use a goofy naming convention to try and avoid a common bug when calling
    // list.remove(0) and list.remove(new Integer(0))

    public static List<Integer> listNonzero(Mask volume)
    {
        return listNonzero(volume, null);
    }

    public static List<Integer> listNonzero(Mask volume, Mask mask)
    {
        List<Integer> out = listAll(volume, mask);
        out.remove(new Integer(0));

        return out;
    }

    public static List<Integer> listAll(Mask volume)
    {
        return listAll(volume, null);
    }

    public static int count(Mask mask)
    {
        int out = 0;

        for (Sample sample : mask.getSampling())
        {
            if (mask.get(sample) != 0)
            {
                out += 1;
            }
        }

        return out;
    }

    public static Vect values(Mask mask, Volume data)
    {
        int num = MaskUtils.count(mask);
        Vect values = new Vect(num);

        int idx = 0;
        for (Sample sample : mask.getSampling())
        {
            if (mask.foreground(sample))
            {
                values.set(idx, data.get(sample, 0));
                idx += 1;
            }
        }

        return values;
    }

    public static List<Integer> listAll(Mask volume, Mask mask)
    {
        Set<Integer> found = Sets.newHashSet();

        for (Sample sample : volume.getSampling())
        {
            int label = volume.get(sample);
            if (label != 0)
            {
                found.add(label);
            }
        }

        List<Integer> out = Lists.newArrayList(found);
        Collections.sort(out);

        return out;
    }

    public static Box bounds(Mask mask)
    {
        Sampling sampling = mask.getSampling();
        Box box = null;
        for (Sample sample : sampling)
        {
            if (mask.foreground(sample))
            {
                if (box == null)
                {
                    box = Box.create(sampling.world(sample));
                }
                else
                {
                    box = box.union(sampling.world(sample));
                }
            }
        }
        return box;
    }

    public static Record attr(Mask volume)
    {
        Record out = new Record();

        Sampling sampling = volume.getSampling();
        VectsOnlineStats stats = new VectsOnlineStats(3);
        for (Sample sample : sampling)
        {
            if (volume.get(sample) != 0)
            {
                stats.update(sampling.world(sample));
            }
        }

        out.with("volume", sampling.voxvol() * stats.num);
        out.with("meanx", stats.mean.get(0));
        out.with("meany", stats.mean.get(1));
        out.with("meanz", stats.mean.get(2));
        out.with("minx", stats.min.get(0));
        out.with("miny", stats.min.get(1));
        out.with("minz", stats.min.get(2));
        out.with("maxx", stats.max.get(0));
        out.with("maxy", stats.max.get(1));
        out.with("maxz", stats.max.get(2));

        return out;
    }

    public static Mask binarize(Mask mask)
    {
        Mask out = mask.proto();
        for (Sample sample : mask.getSampling())
        {
            if (mask.foreground(sample))
            {
                out.set(sample, 1);
            }
        }

        return out;
    }

    public static double volume(Mask mask)
    {
        double out = 0;
        double voxel = mask.getSampling().voxvol();
        for (Sample sample : mask.getSampling())
        {
            if (mask.get(sample) != 0)
            {
                out += voxel;
            }
        }

        return out;
    }

    public static double volume(Mask mask, int label)
    {
        double out = 0;
        double voxel = mask.getSampling().voxvol();
        for (Sample sample : mask.getSampling())
        {
            if (mask.get(sample) == label)
            {
                out += voxel;
            }
        }

        return out;
    }

    public static Map<Integer, Double> volumes(Mask mask)
    {
        Map<Integer, Double> out = Maps.newHashMap();
        double voxel = mask.getSampling().voxvol();

        for (Sample sample : mask.getSampling())
        {
            int label = mask.get(sample);
            if (label > 0)
            {
                if (!out.containsKey(label))
                {
                    out.put(label, 0.0);
                }

                out.put(label, out.get(label) + voxel);
            }
        }

        return out;
    }

    public static Vects coords(Mask mask)
    {
        Vects out = new Vects();
        for (Sample s : mask.getSampling())
        {
            if (mask.get(s) != 0)
            {
                out.add(mask.getSampling().world(s));
            }
        }

        return out;
    }

    public static void equal(Mask volume, int label, Mask out)
    {
        for (Sample sample : volume.getSampling())
        {
            out.set(sample, volume.get(sample) == label ? 1 : 0);
        }
    }

    public static void equal(Mask volume, List<Integer> idx, Mask out)
    {
        Set<Integer> set = Sets.newHashSet(idx);
        for (Sample sample : volume.getSampling())
        {
            int t = volume.get(sample);
            boolean hit = set.contains(t);
            out.set(sample, hit ? 1 : 0);
        }
    }

    public static Mask equal(Mask volume, int idx)
    {
        Mask out = volume.proto();
        equal(volume, idx, out);
        return out;
    }

    public static Mask equal(Mask volume, List<Integer> idx)
    {
        Mask out = volume.proto();
        equal(volume, idx, out);
        return out;
    }

    public static Mask lesser(Mask volume, double thresh)
    {
        MaskComponents comper = new MaskComponents();
        comper.input = volume;
        Mask cc = comper.run().output;

        double vox = cc.getSampling().voxvol();
        Map<Integer, Double> vols = Maps.newHashMap();

        for (Sample sample : volume.getSampling())
        {
            int label = cc.get(sample);
            if (label > 0)
            {
                if (vols.containsKey(label))
                {
                    vols.put(label, vols.get(label) + vox);
                }
                else
                {
                    vols.put(label, vox);
                }
            }
        }

        for (Sample sample : volume.getSampling())
        {
            int label = cc.get(sample);
            if (label > 0)
            {
                double vol = vols.get(label);

                if (vol >= thresh)
                {
                    cc.set(sample, 0);
                }
            }
        }

        return cc;
    }

    public static Mask greater(Mask volume, double thresh)
    {
        MaskComponents comper = new MaskComponents();
        comper.input = volume;
        Mask cc = comper.run().output;

        double vox = cc.getSampling().voxvol();
        Map<Integer, Double> vols = Maps.newHashMap();

        for (Sample sample : volume.getSampling())
        {
            int label = cc.get(sample);
            if (label > 0)
            {
                if (vols.containsKey(label))
                {
                    vols.put(label, vols.get(label) + vox);
                }
                else
                {
                    vols.put(label, vox);
                }
            }
        }

        for (Sample sample : volume.getSampling())
        {
            int label = cc.get(sample);
            if (label > 0)
            {
                double vol = vols.get(label);

                if (vol <= thresh)
                {
                    cc.set(sample, 0);
                }
            }
        }

        return cc;
    }

    public static Mask largest(Mask volume)
    {
        MaskComponents comper = new MaskComponents();
        comper.input = volume;
        Mask cc = comper.run().output;
        return MaskUtils.mask(volume, equal(cc, 1));
    }

    public static Mask largestn(Mask volume, int n)
    {
        MaskComponents comper = new MaskComponents();
        comper.input = volume;
        Mask cc = comper.run().output;

        Mask top = cc.proto();
        for (Sample sample : cc.getSampling())
        {
            if (cc.get(sample) <= n)
            {
                top.set(sample, 1);
            }
        }

        return MaskUtils.mask(volume, top);
    }

    public static Map<Integer, Vect> centroids(Mask mask)
    {
        Sampling sampling = mask.getSampling();
        Map<Integer, Vect> sums = Maps.newHashMap();
        Map<Integer, Integer> counts = Maps.newHashMap();

        for (Sample s : sampling)
        {
            int label = mask.get(s);

            if (mask.get(s) != 0)
            {
                Vect sum = sums.containsKey(label) ? sums.get(label) : VectSource.create3D();
                int count = counts.containsKey(label) ? counts.get(label) : 0;

                sums.put(label, sum.plus(sampling.world(s)));
                counts.put(label, count + 1);
            }
        }

        Map<Integer, Vect> out = Maps.newHashMap();
        for (Integer key : sums.keySet())
        {
            out.put(key, sums.get(key).times(1.0 / counts.get(key)));
        }

        return out;
    }

    public static Mask voxelize(Sampling sampling, Curves curves)
    {
        Mask out = new Mask(sampling);
        for (Curve curve : curves)
        {
            for (Sample sample : sampling.traverse(curve.getAll(Curves.COORD)))
            {
                if (sampling.contains(sample))
                {
                    out.set(sample, 1);
                }
            }
        }

        return out;
    }

    public static VectOnlineStats stats(Mask region, Volume data)
    {
        VectOnlineStats stats = new VectOnlineStats();
        for (Sample sample : region.getSampling())
        {
            if (region.foreground(sample))
            {
                stats.update(data.get(sample, 0));
            }
        }
        return stats;
    }

    public static Map<Integer,VectOnlineStats> statsMulti(Mask regions, Volume data)
    {
        Map<Integer, VectOnlineStats> stats = Maps.newHashMap();
        for (Integer idx : MaskUtils.listNonzero(regions))
        {
            Logging.info("... computing stats for region: " + idx);
            stats.put(idx, stats(MaskUtils.equal(regions, idx), data));
        }
        return stats;
    }

    public static Mask split(Mask tissue, Vects landmarks)
    {
        Global.assume(landmarks.size() == 8, "expeced 8 landmarks");

        Vect shift = landmarks.get(0);
        Vect left = landmarks.get(2);
        Vect right = landmarks.get(3);
        Vect superior = landmarks.get(4);
        Vect inferior = landmarks.get(5);
        Vect anterior = landmarks.get(6);
        Vect posterior = landmarks.get(7);
        Vect midleft = left.plus(shift).times(0.5);
        Vect midright = right.plus(shift).times(0.5);

        Function<Vect,Vect> map = (v) ->
        {
            double x = v.getX();
            double y = v.getY();
            double z = v.getZ();

            Vect out = VectSource.createND(7);
            out.set(0, x * x);
            out.set(1, y * y);
            out.set(2, z * z);
            out.set(3, x);
            out.set(4, y);
            out.set(5, z);
            out.set(6, 1);

            return out;
        };

        Matrix A = new Matrix(9, 7);
        Vect B = VectSource.createND(9);

        List<Pair<Vect, Double>> pairs = Lists.newArrayList();
        pairs.add(Pair.of(shift, 0.0));
        pairs.add(Pair.of(left, 1.0));
        pairs.add(Pair.of(right, -1.0));
        pairs.add(Pair.of(midleft, 0.5));
        pairs.add(Pair.of(midright, -0.5));
        pairs.add(Pair.of(anterior, 0.0));
        pairs.add(Pair.of(posterior, 0.0));
        pairs.add(Pair.of(superior, 0.0));
        pairs.add(Pair.of(inferior, 0.0));

        for (int idx = 0; idx < 9; idx++)
        {
            Pair<Vect,Double> pair = pairs.get(idx);
            A.setRow(idx, map.apply(pair.a));
            B.set(idx, pair.b);
        }

        Vect params = MatrixUtils.solve(A, B);

        Sampling sampling = tissue.getSampling();
        Mask out = tissue.proto();
        for (Sample sample : sampling)
        {
            if (tissue.foreground(sample))
            {
                Vect world = sampling.world(sample);
                double value = map.apply(world).dot(params);
                out.set(sample, value < 0 ? 2 : 1);
            }
        }

        return out;
    }
}
