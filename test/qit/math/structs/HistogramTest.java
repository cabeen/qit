package qit.math.structs;

import org.junit.jupiter.api.Test;
import qit.data.datasets.Vect;

import static org.junit.jupiter.api.Assertions.*;

class HistogramTest
{
    private static final double EPS = 1e-9;

    // ===== creation =====

    @Test
    void createBasic()
    {
        Histogram h = Histogram.create(10, 0.0, 1.0);
        assertEquals(10, h.size());
        assertEquals(10, h.getBins());
    }

    @Test
    void createBreakBounds()
    {
        Histogram h = Histogram.create(5, 0.0, 10.0);
        assertEquals(0.0, h.getBoundBelow(0), EPS);
        assertEquals(10.0, h.getBoundAbove(h.size() - 1), EPS);
    }

    // ===== update / contains =====

    @Test
    void updateAndGet()
    {
        Histogram h = Histogram.create(10, 0.0, 10.0);
        h.update(5.0);
        h.update(5.0);
        h.update(5.0);
        assertEquals(3.0, h.sum(), EPS);
    }

    @Test
    void updateOutOfRange()
    {
        Histogram h = Histogram.create(10, 0.0, 10.0);
        h.update(-1.0);  // outside range, should be ignored
        h.update(11.0);  // outside range, should be ignored
        assertEquals(0.0, h.sum(), EPS);
    }

    @Test
    void containsValue()
    {
        Histogram h = Histogram.create(10, 0.0, 10.0);
        assertTrue(h.contains(0.0));
        assertTrue(h.contains(5.0));
        assertTrue(h.contains(10.0));
        assertFalse(h.contains(-0.1));
        assertFalse(h.contains(10.1));
    }

    // ===== sum =====

    @Test
    void sum()
    {
        Histogram h = Histogram.create(10, 0.0, 10.0);
        for (int i = 0; i < 100; i++)
        {
            h.update(i * 0.1);
        }
        assertEquals(100.0, h.sum(), EPS);
    }

    // ===== maxIndex / maxValue =====

    @Test
    void maxIndexAndValue()
    {
        Histogram h = Histogram.create(10, 0.0, 10.0);
        // put most samples in one bin
        h.update(1.0);
        h.update(1.5);
        h.update(1.5);
        h.update(1.5);
        h.update(5.0);

        assertEquals(h.get(h.maxIndex()), h.maxValue(), EPS);
        assertTrue(h.maxValue() >= 3.0);
    }

    // ===== mode =====

    @Test
    void mode()
    {
        Histogram h = Histogram.create(10, 0.0, 10.0);
        // concentrate values around 5
        for (int i = 0; i < 50; i++)
        {
            h.update(5.0);
        }
        h.update(1.0);
        // mode should be near 5.0
        double m = h.mode();
        assertTrue(m > 4.0 && m < 6.0);
    }

    // ===== normalize =====

    @Test
    void normalize()
    {
        Histogram h = Histogram.create(10, 0.0, 10.0);
        h.update(1.0);
        h.update(3.0);
        h.update(5.0);
        h.update(7.0);
        h.update(9.0);
        h.normalize();
        assertEquals(1.0, h.sum(), 1e-6);
    }

    // ===== density =====

    @Test
    void density()
    {
        Histogram h = Histogram.create(10, 0.0, 10.0);
        h.update(1.0);
        h.update(3.0);
        h.update(5.0);
        Vect d = h.density();
        assertEquals(h.size(), d.size());
        assertEquals(1.0, d.sum(), 1e-6);
    }

    // ===== entropy =====

    @Test
    void entropyUniform()
    {
        // uniform distribution should have high entropy
        Histogram h = Histogram.create(10, 0.0, 10.0);
        for (int i = 0; i < 10; i++)
        {
            h.update(i + 0.5);
        }
        double entropy = h.entropy();
        assertTrue(entropy > 0);
    }

    @Test
    void entropyConcentrated()
    {
        // concentrated distribution has lower entropy than uniform
        Histogram uniform = Histogram.create(10, 0.0, 10.0);
        for (int i = 0; i < 10; i++)
        {
            uniform.update(i + 0.5);
        }

        Histogram concentrated = Histogram.create(10, 0.0, 10.0);
        for (int i = 0; i < 10; i++)
        {
            concentrated.update(5.0);
        }

        assertTrue(concentrated.entropy() < uniform.entropy());
    }

    // ===== otsu =====

    @Test
    void otsuBimodal()
    {
        // bimodal distribution with peaks at low and high ends
        Histogram h = Histogram.create(100, 0.0, 100.0);
        // low cluster around 20
        for (int i = 0; i < 50; i++)
        {
            h.update(20.0);
        }
        // high cluster around 80
        for (int i = 0; i < 50; i++)
        {
            h.update(80.0);
        }

        double threshold = h.otsu();
        // otsu threshold should fall between the two clusters
        assertTrue(threshold > 20.0 && threshold < 80.0,
            "Otsu threshold " + threshold + " should be between 20 and 80");
    }

    // ===== compatible =====

    @Test
    void compatible()
    {
        Histogram a = Histogram.create(10, 0.0, 10.0);
        Histogram b = Histogram.create(10, 0.0, 10.0);
        Histogram c = Histogram.create(20, 0.0, 10.0);
        assertTrue(a.compatible(b));
        assertFalse(a.compatible(c));
    }

    // ===== vect =====

    @Test
    void vect()
    {
        Histogram h = Histogram.create(5, 0.0, 5.0);
        h.update(1.0);
        h.update(3.0);
        Vect v = h.vect();
        assertEquals(5, v.size());
        assertEquals(2.0, v.sum(), EPS);
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Histogram h = Histogram.create(10, 0.0, 10.0);
        h.update(5.0);
        h.update(5.0);
        Histogram c = h.copy();
        assertEquals(h.sum(), c.sum(), EPS);
        assertEquals(h.size(), c.size());

        // modify copy, original unchanged
        c.update(5.0);
        assertEquals(2.0, h.sum(), EPS);
        assertEquals(3.0, c.sum(), EPS);
    }

    // ===== table conversion =====

    @Test
    void table()
    {
        Histogram h = Histogram.create(5, 0.0, 5.0);
        h.update(1.0);
        qit.data.datasets.Table t = h.table();
        assertEquals(5, t.getNumRecords());
        assertTrue(t.hasField("index"));
        assertTrue(t.hasField("below"));
        assertTrue(t.hasField("middle"));
        assertTrue(t.hasField("above"));
        assertTrue(t.hasField("value"));
    }

    // ===== smoothBins =====

    @Test
    void smoothBinsPreservesTotal()
    {
        Histogram h = Histogram.create(20, 0.0, 20.0);
        h.update(10.0);
        h.update(10.0);
        h.update(10.0);
        double sumBefore = h.sum();
        h.smoothBins(2.0);
        assertEquals(sumBefore, h.sum(), 1e-4);
    }
}
