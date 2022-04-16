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
 * developing probAngleware products for sale or license to a third party or
 * (3) use of the Software or any derivative of it for research with the
 * final aim of developing non-probAngleware products for sale or license to a
 * third party, or (4) use of the Software to provide any service to an
 * external organisation for which payment is received.
 *
 ******************************************************************************/


package qit.data.utils.mri.structs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.math.structs.Containable;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class StreamlineTracker
{
    public StreamlineField field;

    public Double step = 1.0d;
    public Double angle = 35d;
    public Map<String, Double> low = Maps.newHashMap();
    public Map<String, Double> high = Maps.newHashMap();

    public Double minlen = 10d;
    public Double maxlen = 1000d;
    public Double reach = Double.MAX_VALUE;

    public Boolean rk = false;
    public Double disperse = null;
    public Boolean vector = false;
    public Double mixing = 1.0d;
    public Boolean empty = false;
    public Boolean mono = false;

    public Boolean prob = false;
    public Boolean probMax = false;
    public Double probAngle = 0.0;
    public Double probPower = 1.0;
    public double gforce = 0.0;

    public Vects seeds;
    public Containable track;
    public Containable stop;
    public Containable trap;
    public VectFunction force;
    public Function<Curves, Curves> filter = (curves) -> curves;

    public boolean chatty = true;
    public int threads = 5;

    public Curves output;

    public void msg(String msg)
    {
        Logging.info(this.chatty, msg);
    }

    public StreamlineTracker run()
    {
        msg("started tracking");
        msg("using seed count: " + seeds.size());

        if (this.threads == 1)
        {
            msg("using single threaded mode");
            this.output = StreamlineTracker.this.runBatch("tracking", this.seeds);
        }
        else
        {
            final ConcurrentHashMap<Integer, Curves> results = new ConcurrentHashMap<>();

            int pool = this.threads;
            if (pool < 1)
            {
                pool = Runtime.getRuntime().availableProcessors();
                msg("detected processors: " + pool);
            }

            msg("using multi-threading: " + pool);
            ExecutorService exec = Executors.newFixedThreadPool(pool);

            int nseed = this.seeds.size();
            final int fpool = nseed <= pool ? 1 : pool;
            int count = (int) Math.ceil(nseed / (double) fpool);

            msg("using batch size: " + count);
            for (int i = 0; i < fpool; i++)
            {
                final int fidx = i;
                int startIdx = fidx * count;
                int endIdx = Math.max(startIdx, Math.min(nseed, (fidx + 1) * count));

                if (startIdx != endIdx)
                {
                    final Vects seedsBatch = this.seeds.subList(startIdx, endIdx);
                    Runnable runnable = () ->
                    {
                        String base = String.format("batch (%s/%s)", (fidx + 1), fpool);
                        results.put(fidx, StreamlineTracker.this.runBatch(base, seedsBatch));
                    };
                    exec.execute(runnable);
                }
            }

            exec.shutdown();
            try
            {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
            catch (InterruptedException e)
            {
                Logging.error("failed to execute in concurrent mode");
            }

            msg("compiling results");

            // be sure to maintain the order of the seeds by keep track of the batch order
            Curves out = new Curves();
            for (int i = 0; i < fpool; i++)
            {
                if (results.containsKey(i))
                {
                    out.add(results.get(i));
                }
            }

            this.output = out;
        }

        msg("kept " + output.size() + " of " + this.seeds.size() + " tracks");
        msg("finished tracking");

        return this;
    }


    private Curves runBatch(String base, Vects seedsBatch)
    {
        Curves out = new Curves();

        int n = seedsBatch.size();

        double maxn = this.maxlen != null ? this.maxlen / this.step : Double.MAX_VALUE;
        double minn = this.minlen != null ? this.minlen / this.step : 0;
        double reachn = this.reach == null ? Double.MAX_VALUE : this.reach / this.step;

        msg(String.format("%s started", base));
        msg(String.format("%s using seed count: %d", base, n));

        int count = 0;
        int ppercent = 0;
        msg(String.format("%s processing: 0 percent", base));
        for (Vect seed : seedsBatch)
        {
            int percent = (int) Math.ceil(100.0 * (count + 1) / n);
            if (percent >= ppercent + 5)
            {
                ppercent = percent;
                msg(String.format("%s processing: %d percent", base, percent));
            }
            count++;

            List<StreamlineField.StreamSample> starts = this.starts(seed);
            if (starts.size() == 0)
            {
                if (this.empty)
                {
                    out.add(0);
                }

                continue;
            }

            StreamlineField.StreamSample start = starts.get(Global.RANDOM.nextInt(starts.size()));
            int trapStart = this.trap == null ? 0 : this.trap.label(start.getPosition());
            boolean stopStart = this.stop == null ? false : this.stop.label(start.getPosition()) != 0;

            Function<StreamlineField.StreamSample, List<StreamlineField.StreamSample>> integrate = (currentSample) ->
            {
                List<StreamlineField.StreamSample> myfibers = Lists.newArrayList();

                if (trapStart > 0)
                {
                    return myfibers;
                }

                int trapPrev = trapStart;
                boolean stopPrev = stopStart;

                while (myfibers.size() < maxn)
                {
                    StreamlineField.StreamSample nextSample = this.next(currentSample);

                    if (nextSample == null || myfibers.size() >= reachn)
                    {
                        break;
                    }

                    int trapNext = this.trap == null ? 0 : this.trap.label(nextSample.getPosition());
                    boolean stopNext = this.stop == null ? false : this.stop.label(nextSample.getPosition()) != 0;

                    if (trapNext != trapPrev && trapPrev != 0 && trapPrev != trapStart)
                    {
                        break;
                    }

                    if (stopNext && stopPrev)
                    {
                        break;
                    }

                    myfibers.add(nextSample);
                    currentSample = nextSample;
                    trapPrev = trapNext;
                }

                return myfibers;
            };

            List<StreamlineField.StreamSample> fibers = Lists.newArrayList(start);

            if (this.mono && !this.vector)
            {
                if (Global.RANDOM.nextBoolean())
                {
                    start.getOrientation().timesEquals(-1);
                }

                fibers.addAll(integrate.apply(start));
            }
            else if (this.vector && this.mono)
            {
                fibers.addAll(integrate.apply(start));
            }
            else
            {
                fibers.addAll(integrate.apply(start));
                if (this.vector && !this.mono)
                {
                    start.reverse = true;
                }
                start.getOrientation().timesEquals(-1);
                Collections.reverse(fibers);
                fibers.addAll(integrate.apply(start));
            }

            if (fibers.size() >= minn)
            {
                Curve curve = out.add(fibers.size());

                for (String attr : fibers.get(0).getAttrs())
                {
                    if (!out.has(attr))
                    {
                        out.add(attr, fibers.get(0).getAttr(attr).proto());
                    }
                }

                for (int i = 0; i < fibers.size(); i++)
                {
                    StreamlineField.StreamSample f = fibers.get(i);
                    curve.set(i, f.getPosition());

                    for (String attr : f.getAttrs())
                    {
                        curve.set(attr, i, f.getAttr(attr));
                    }
                }
            }
            else if (this.empty)
            {
                out.add(0);
            }
        }

        if (this.filter != null)
        {
            msg(String.format("%s filtering", base));
            out = this.filter.apply(out);
        }

        msg(String.format("%s finished", base));

        return out;
    }

    public List<StreamlineField.StreamSample> starts(Vect seed)
    {
        List<StreamlineField.StreamSample> samples = Lists.newArrayList();

        for (StreamlineField.StreamSample start : this.field.getSamples(seed))
        {
            if (this.disperse != null)
            {
                start.orientation.plusEquals(VectSource.gaussian(3).times(this.disperse));
                start.orientation.normalizeEquals();
            }

            if (this.stop != null && this.stop.contains(start.getPosition()))
            {
                continue;
            }

            if (!this.valid(start))
            {
                continue;
            }

            samples.add(start);
        }

        if (this.prob && samples.size() > 0)
        {
            List<StreamlineField.StreamSample> out = Lists.newArrayList();
            out.add(prob(seed, samples));
            return out;
        }
        else
        {
            return samples;
        }
    }

    public boolean valid(StreamlineField.StreamSample input)
    {
        if (input == null)
        {
            return false;
        }

        Vect pos = input.getPosition();

        if (this.track != null && !this.track.contains(pos))
        {
            return false;
        }

        if (!MathUtils.unit(input.getOrientation().norm()))
        {
            return false;
        }

        for (String attr : this.low.keySet())
        {
            if (input.getAttr(attr).get(0) < this.low.get(attr))
            {
                return false;
            }
        }

        for (String attr : this.high.keySet())
        {
            if (input.getAttr(attr).get(0) > this.high.get(attr))
            {
                return false;
            }
        }

        return true;
    }

    public StreamlineField.StreamSample next(StreamlineField.StreamSample input)
    {
        StreamlineField.StreamSample sample = this.rk ? nextRungeKutta(input) : nextEuler(input);

        if (sample != null && this.stop != null && !this.stop.contains(input.getPosition()) && this.stop.contains(sample.getPosition()))
        {
            return null;
        }

        return sample;
    }

    public StreamlineField.StreamSample nextRungeKutta(StreamlineField.StreamSample input)
    {
        Vect v1pos = input.getPosition();
        StreamlineField.StreamSample v1samp = sample(input.getPosition(), input.getOrientation(), input.reverse);
        if (v1samp == null)
        {
            return nextEuler(input);
        }
        Vect v1 = v1samp.getOrientation();

        Vect v2pos = v1pos.plus(0.5 * this.step, v1);
        StreamlineField.StreamSample v2samp = sample(v2pos, v1, input.reverse);
        if (v2samp == null)
        {
            return nextEuler(input);
        }
        Vect v2 = v2samp.getOrientation();

        Vect v3pos = v1pos.plus(0.5 * this.step, v2);
        StreamlineField.StreamSample v3samp = sample(v3pos, v1, input.reverse);
        if (v3samp == null)
        {
            return nextEuler(input);
        }
        Vect v3 = v3samp.getOrientation();

        Vect v4pos = v1pos.plus(this.step, v3);
        StreamlineField.StreamSample v4samp = sample(v4pos, v1, input.reverse);
        if (v4samp == null)
        {
            return nextEuler(input);
        }
        Vect v4 = v4samp.getOrientation();

        Vect vrk = v1.plus(v2.times(0.5)).plus(v3.times(0.5)).plus(v4).times(1.0 / 6.0).normalize();
        Vect vrkpos = v1pos.plus(this.step, vrk);

        return sample(vrkpos, vrk, input.reverse);
    }

    public StreamlineField.StreamSample nextEuler(StreamlineField.StreamSample input)
    {
        Vect v = input.getOrientation();

        if (this.disperse != null)
        {
            v.plusEquals(VectSource.gaussian(3).times(this.disperse));
            v.normalizeEquals();
        }

        Vect npos = input.getPosition().plus(this.step, v);
        return sample(npos, v, input.reverse);
    }

    public StreamlineField.StreamSample sample(Vect pos, Vect dir, boolean reverse)
    {
        // returns null if no valid samples were found

        List<StreamlineField.StreamSample> samples = Lists.newArrayList();

        // select samples below the angle threshold
        for (StreamlineField.StreamSample f : this.field.getSamples(pos))
        {
            if (!this.valid(f))
            {
                continue;
            }

            Vect v = f.getOrientation();

            if (reverse)
            {
                v.timesEquals(-1);
            }

            if (!this.vector && v.dot(dir) < 0)
            {
                v.timesEquals(-1);
            }

            f.angle = Math.abs(dir.angleDeg(v));

            if (f.angle <= this.angle)
            {
                samples.add(f);
            }
        }

        if (samples.size() == 0)
        {
            return null;
        }

        StreamlineField.StreamSample output = null;

        if (samples.size() == 1)
        {
            output = samples.get(0);
        }
        else if (this.prob)
        {
            // use probabilistic tractography
            output = prob(pos, samples);
        }
        else
        {
            // use deterministic tractography (pick the closest angle)
            double minDist = Double.MAX_VALUE;
            StreamlineField.StreamSample minSample = null;
            for (StreamlineField.StreamSample sample : samples)
            {
                double dist = sample.getOrientation().angleLineDeg(dir);
                if (minSample == null || dist < minDist)
                {
                    minDist = dist;
                    minSample = sample;
                }
            }

            output = minSample;
        }

        if (output != null && !MathUtils.unit(this.mixing))
        {
            // smooth the orientations
            Vect a = dir.times(1.0 - this.mixing);
            Vect b = output.orientation.times(this.mixing);
            output.orientation = a.plus(b).normalize();
        }

        output.reverse = reverse;

        return output;
    }

    private StreamlineField.StreamSample prob(Vect pos, List<StreamlineField.StreamSample> samples)
    {
        Vect probs = VectSource.createND(samples.size());

        for (int i = 0; i < samples.size(); i++)
        {
            double p = samples.get(i).getProbability();

            if (!MathUtils.unit(this.probPower))
            {
                p = Math.pow(p, this.probPower);
            }

            probs.set(i, p);
        }

        probs = probs.normalizeSum();

        if (MathUtils.nonzero(this.probAngle))
        {
            Vect probAngles = VectSource.createND(samples.size());
            probAngles.setAll(1.0);

            for (int i = 0; i < samples.size(); i++)
            {
                double p = Math.exp(-this.probAngle * samples.get(i).angle / this.angle);
                probAngles.set(i, p);
            }

            probs.timesEquals(probAngles.normalizeSum());
        }

        if (this.force != null)
        {
            Vect f = this.force.apply(pos);
            Vect forces = VectSource.createND(samples.size());
            forces.setAll(1.0);

            if (MathUtils.nonzero(f.norm()))
            {
                for (int i = 0; i < samples.size(); i++)
                {
                    double d = f.dot(samples.get(i).orientation);

                    if (this.vector)
                    {
                        d = (d + 1) / 2.0;
                    }

                    double p = Math.exp(-(1.0 - Math.abs(d)) / (this.gforce + 1e-6));

                    forces.set(i, p);
                }

                if (MathUtils.zero(this.gforce))
                {
                    int maxidx = forces.maxidx();
                    forces.setAll(0);
                    forces.set(maxidx, 1.0);
                }
            }

            probs.timesEquals(forces.normalizeSum());
        }

        double sum = probs.sum();
        if (sum > 0)
        {
            probs.divEquals(sum);
        }

        if (this.probMax)
        {
            // pick the most likely one based on the previous factors
            return samples.get(probs.maxidx());
        }
        else
        {
            // or sample from a categorical distribution
            Vect cumsum = probs.cumsum();
            double unit = Global.RANDOM.nextDouble();

            for (int i = 0; i < samples.size(); i++)
            {
                if (unit < cumsum.get(i))
                {
                    return samples.get(i);
                }
            }
        }

        // if something fails, then pick sample uniformly
        return samples.get(Global.RANDOM.nextInt(samples.size()));
    }
}