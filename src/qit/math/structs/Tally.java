package qit.math.structs;

import com.google.common.collect.Maps;

import java.util.Map;

public class Tally
{
    int sum = 0;
    Map<Integer, Integer> totals = Maps.newHashMap();

    public void increment(Integer e)
    {
        if (!this.totals.containsKey(e))
        {
            this.totals.put(e, 0);
        }

        this.totals.put(e, this.totals.get(e) + 1);
        this.sum += 1;
    }

    public double entropy()
    {
        double out = 0;

        for (Integer key : this.totals.keySet())
        {
            double p = this.totals.get(key) / (double) sum;
            double logp = Math.log(p);

            out -= p * logp;
        }

        return out;
    }

    public Integer mode()
    {
        Integer out = null;
        Integer total = null;

        for (Integer e : this.totals.keySet())
        {
            int t = this.totals.get(e);

            if (out == null || t > total)
            {
                out = e;
                total = t;
            }
        }

        return out;
    }
}