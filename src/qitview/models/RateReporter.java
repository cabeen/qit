package qitview.models;

import com.google.common.collect.EvictingQueue;
import qit.data.utils.vects.stats.VectOnlineStats;

public class RateReporter
{
    public static int TICK = 1000;

    private long check = System.currentTimeMillis();
    private EvictingQueue<Long> times = EvictingQueue.create(100);

    public void touch()
    {
        this.times.add(System.currentTimeMillis());
    }

    public int rate()
    {
        VectOnlineStats stats = new VectOnlineStats();

        Long prev = null;
        for (long time : this.times)
        {
            if (prev != null)
            {
                stats.update(time - prev);
            }

            prev = time;
        }

        return (int) Math.round(1000.0 / stats.mean);
    }

    public boolean show()
    {
        long current = System.currentTimeMillis();
        if (current - this.check > TICK)
        {
            this.check = current;
            return true;
        }

        return false;
    }
}
