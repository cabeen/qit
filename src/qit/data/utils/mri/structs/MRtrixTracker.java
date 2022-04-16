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


package qit.data.utils.mri.structs;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.models.Spharm;
import qit.data.modules.mri.spharm.VolumeSpharmPeaks;
import qit.data.source.VectSource;
import qit.math.structs.Containable;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MRtrixTracker
{
    public VectFunction field;

    public Double angleThreshold = 35d;
    public Double stepSize = 1.0d;
    public Integer maxTrials = 50;
    public Integer coneSamples = 12;
    public Double min = null;

    public Double minLength = 10d;
    public Double maxLength = 1000d;
    public Double reach = Double.MAX_VALUE;

    public Double mixing = 1.0d;
    public Boolean rk = false;
    public Boolean empty = false;
    public Boolean mono = false;

    public Vects seeds;
    public Containable track;
    public Containable stop;

    public boolean verbose = true;
    public int threads = 5;

    public Curves output;

    public void msg(String msg)
    {
        if (this.verbose)
        {
            Logging.info(msg);
        }
    }

    public MRtrixTracker run()
    {
        msg("started tracking");
        msg("using seed count: " + seeds.size());

        VolumeSpharmPeaks peakmake = new VolumeSpharmPeaks();
        peakmake.thresh = this.min;

        if (this.threads == 1)
        {
            msg("using single threaded mode");
            this.output = MRtrixTracker.this.runBatch("tracking", this.seeds);
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
                    Runnable runnable = new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            String base = String.format("batch (%s/%s)", (fidx + 1), fpool);
                            results.put(fidx, MRtrixTracker.this.runBatch(base, seedsBatch));
                        }
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

        double maxn = this.maxLength / this.stepSize;
        double minn = this.minLength / this.stepSize;
        double reachn = this.reach == null ? Double.MAX_VALUE : this.reach / this.stepSize;

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

            MRtrixSample start = this.starts(seed);
            if (start == null)
            {
                continue;
            }

            List<MRtrixSample> fibers = Lists.newArrayList(start);

            if (this.mono)
            {
                if (Global.RANDOM.nextBoolean())
                {
                    start.getOrientation().timesEquals(-1);
                }

                MRtrixSample curr = start;
                while (fibers.size() < maxn)
                {
                    MRtrixSample next = this.next(curr);

                    if (next == null || fibers.size() >= reachn)
                    {
                        break;
                    }

                    fibers.add(next);
                    curr = next;
                }
            }
            else
            {
                MRtrixSample curr = start;
                while (fibers.size() < maxn)
                {
                    MRtrixSample next = this.next(curr);

                    if (next == null || fibers.size() >= reachn)
                    {
                        break;
                    }

                    fibers.add(next);
                    curr = next;
                }

                Collections.reverse(fibers);
                start.getOrientation().timesEquals(-1);
                curr = start;

                while (fibers.size() < maxn)
                {
                    MRtrixSample next = this.next(curr);

                    if (next == null || fibers.size() >= reachn)
                    {
                        break;
                    }

                    fibers.add(next);
                    curr = next;
                }
            }

            if (fibers.size() >= minn)
            {
                Curve curve = out.add(fibers.size());

                out.add(Curves.DENSITY, VectSource.create1D());

                for (int i = 0; i < fibers.size(); i++)
                {
                    MRtrixSample f = fibers.get(i);
                    curve.set(i, f.getPosition());
                    curve.set(Curves.DENSITY, i, VectSource.create1D(f.getValue()));
                }
            }
            else if (this.empty)
            {
                out.add(0);
            }
        }
        msg(String.format("%s finished", base));

        return out;
    }

    public MRtrixSample starts(Vect seed)
    {
        // seed by finding a direction with a fod value above the threshold
        Spharm spharm = new Spharm(this.field.apply(seed));

        for (int i = 0; i < this.maxTrials; i++)
        {
            Vect dir = VectSource.randomUnit();
            double value = spharm.sample(dir);
            if (value > this.min)
            {
                return new MRtrixSample(seed, dir, value);
            }
        }

        return null;
    }

    public boolean valid(MRtrixSample input)
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

        if (this.min != null && input.getValue() < this.min)
        {
            return false;
        }

        return true;
    }

    public MRtrixSample next(MRtrixSample input)
    {
        MRtrixSample sample = this.rk ? nextRungeKutta(input) : nextEuler(input);

        if (sample != null && this.stop != null && !this.stop.contains(input.getPosition()) && this.stop.contains(sample.getPosition()))
        {
            return null;
        }

        return sample;
    }

    public MRtrixSample nextRungeKutta(MRtrixSample input)
    {
        Vect v1pos = input.getPosition();
        MRtrixSample v1samp = sample(input.getPosition(), input.getOrientation());
        if (v1samp == null)
        {
            return nextEuler(input);
        }
        Vect v1 = v1samp.getOrientation();

        Vect v2pos = v1pos.plus(0.5 * this.stepSize, v1);
        MRtrixSample v2samp = sample(v2pos, v1);
        if (v2samp == null)
        {
            return nextEuler(input);
        }
        Vect v2 = v2samp.getOrientation();

        Vect v3pos = v1pos.plus(0.5 * this.stepSize, v2);
        MRtrixSample v3samp = sample(v3pos, v1);
        if (v3samp == null)
        {
            return nextEuler(input);
        }
        Vect v3 = v3samp.getOrientation();

        Vect v4pos = v1pos.plus(this.stepSize, v3);
        MRtrixSample v4samp = sample(v4pos, v1);
        if (v4samp == null)
        {
            return nextEuler(input);
        }
        Vect v4 = v4samp.getOrientation();

        Vect vrk = v1.plus(v2.times(0.5)).plus(v3.times(0.5)).plus(v4).times(1.0 / 6.0).normalize();
        Vect vrkpos = v1pos.plus(this.stepSize, vrk);

        return sample(vrkpos, vrk);
    }

    public MRtrixSample nextEuler(MRtrixSample input)
    {
        Vect npos = input.getPosition().plus(this.stepSize, input.getOrientation());
        return sample(npos, input.getOrientation());
    }

    public MRtrixSample sample(Vect pos, Vect dir)
    {
        // select samples below the angle threshold
        Spharm spharm = new Spharm(this.field.apply(pos));
        Pair<Vect,Double> next = this.nextFodDir(dir, spharm);

        if (next == null)
        {
            return null;
        }

        MRtrixSample sample = new MRtrixSample(pos, next.a, next.b);

        if (!this.valid(sample))
        {
            return null;
        }

        Vect v = sample.getOrientation();
        if (v.dot(dir) < 0)
        {
            v.timesEquals(-1);
        }

        double angle = Math.abs(dir.angleDeg(v));
        if (angle > (this.angleThreshold))
        {
            return null;
        }

        if (!MathUtils.unit(this.mixing))
        {
            // smooth the orientations
            Vect a = dir.times(1.0 - mixing);
            Vect b = sample.orientation.times(this.mixing);
            sample.orientation = a.plus(b).normalize();
        }

        return sample;
    }

    private Vect randomConeDir(Vect dir)
    {
        double[] v = new double[3];
        do
        {
            v[0] = 2.0 * Global.RANDOM.nextDouble() - 1.0;
            v[1] = 2.0 * Global.RANDOM.nextDouble() - 1.0;
        }
        while (v[0] * v[0] + v[1] * v[1] > 1.0);

        double factor = Math.sin(Math.toRadians(this.angleThreshold));
        v[0] *= factor;
        v[1] *= factor;

        v[2] = 1.0 - (v[0] * v[0] + v[1] * v[1]);
        v[2] = v[2] < 0.0 ? 0.0 : Math.sqrt(v[2]);

        double dirx = dir.getX();
        double diry = dir.getY();
        double dirz = dir.getZ();

        if (dirx * dirx + diry * diry < 1e-4)
        {
            double outx = v[0];
            double outy = v[1];
            double outz = dirz > 0.0 ? v[2] : -v[2];

            return VectSource.create3D(outx, outy, outz);
        }
        else
        {
            double norm;
            double y[] = {dirx, diry, 0.0};
            norm = Math.sqrt(y[0] * y[0] + y[1] * y[1] + y[2] * y[2]);
            y[0] /= norm;
            y[1] /= norm;
            y[2] /= norm;

            double x[] = {-y[1], y[0], 0.0};
            double y2[] = {-x[1] * dirz, x[0] * dirz, x[1] * dirx - x[0] * diry};
            norm = Math.sqrt(y2[0] * y2[0] + y2[1] * y2[1] + y2[2] * y2[2]);
            y2[0] /= norm;
            y2[1] /= norm;
            y2[2] /= norm;

            double cx = v[0] * x[0] + v[1] * x[1];
            double cy = v[0] * y[0] + v[1] * y[1];

            double outx = cx * x[0] + cy * y2[0] + v[2] * dirx;
            double outy = cx * x[1] + cy * y2[1] + v[2] * diry;
            double outz = cy * y2[2] + v[2] * dirz;

            return VectSource.create3D(outx, outy, outz);
        }
    }

    private Pair<Vect,Double> nextFodDir(Vect dir, Spharm odf)
    {
        double max_val = 0.0;

        for (int n = 0; n < this.coneSamples; n++)
        {
            Vect cdir = randomConeDir(dir);
            double val = odf.sample(cdir);
            if (val > max_val)
            {
                max_val = val;
            }
        }

        if (Double.isNaN(max_val) || (max_val < this.min))
        {
            return null;
        }
        else
        {
            max_val *= 1.5;

            for (int n = 0; n < this.maxTrials; n++)
            {
                Vect rdir = randomConeDir(dir);
                double val = odf.sample(rdir);
                if (val > this.min)
                {
                    if (val > max_val)
                    {
                        Logging.info(String.format("warning: max odf value exceeded.  val = %g, max_val = %g", val, max_val));
                    }

                    if (Global.RANDOM.nextDouble() < (val / max_val))
                    {
                        return Pair.of(rdir, val);
                    }
                }
            }
        }

        return null;
    }

    public static class MRtrixSample
    {
        protected Vect position;
        protected Vect orientation;
        protected Double value;

        public MRtrixSample(Vect p, Vect v, double e)
        {
            this.position = p;
            this.orientation = v;
            this.value = e;
        }

        public Vect getPosition()
        {
            return this.position;
        }

        public Vect getOrientation()
        {
            return this.orientation;
        }

        public Double getValue()
        {
            return this.value;
        }
    }
}
