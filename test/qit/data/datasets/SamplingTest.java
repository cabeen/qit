package qit.data.datasets;

import org.junit.jupiter.api.Test;
import qit.base.structs.Integers;
import qit.data.source.VectSource;

import static org.junit.jupiter.api.Assertions.*;

class SamplingTest
{
    private Sampling sampling3x4x5()
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(1, 1, 1);
        return new Sampling(start, delta, new Integers(3, 4, 5));
    }

    private Sampling samplingWithSpacing()
    {
        Vect start = VectSource.create3D(10, 20, 30);
        Vect delta = VectSource.create3D(0.5, 1.0, 2.0);
        return new Sampling(start, delta, new Integers(4, 5, 6));
    }

    // ===== construction =====

    @Test
    void constructBasic()
    {
        Sampling s = sampling3x4x5();
        assertEquals(3, s.numI());
        assertEquals(4, s.numJ());
        assertEquals(5, s.numK());
    }

    @Test
    void constructWithSpacing()
    {
        Sampling s = samplingWithSpacing();
        assertEquals(0.5, s.deltaI(), 1e-10);
        assertEquals(1.0, s.deltaJ(), 1e-10);
        assertEquals(2.0, s.deltaK(), 1e-10);
    }

    @Test
    void constructWithOrigin()
    {
        Sampling s = samplingWithSpacing();
        assertEquals(10.0, s.startI(), 1e-10);
        assertEquals(20.0, s.startJ(), 1e-10);
        assertEquals(30.0, s.startK(), 1e-10);
    }

    @Test
    void size()
    {
        Sampling s = sampling3x4x5();
        assertEquals(60, s.size());
    }

    // ===== num accessors =====

    @Test
    void numAccessors()
    {
        Sampling s = sampling3x4x5();
        assertEquals(3, s.num(0));
        assertEquals(4, s.num(1));
        assertEquals(5, s.num(2));
    }

    @Test
    void numCopy()
    {
        Sampling s = sampling3x4x5();
        Integers n = s.num();
        assertEquals(3, n.size());
        assertEquals(3, n.get(0));
        assertEquals(4, n.get(1));
        assertEquals(5, n.get(2));
    }

    // ===== delta accessors =====

    @Test
    void deltaAccessors()
    {
        Sampling s = samplingWithSpacing();
        assertEquals(0.5, s.delta(0), 1e-10);
        assertEquals(1.0, s.delta(1), 1e-10);
        assertEquals(2.0, s.delta(2), 1e-10);
    }

    @Test
    void deltaCopy()
    {
        Sampling s = samplingWithSpacing();
        Vect d = s.delta();
        assertEquals(3, d.size());
        assertEquals(0.5, d.get(0), 1e-10);
    }

    @Test
    void deltaMinMax()
    {
        Sampling s = samplingWithSpacing();
        assertEquals(0.5, s.deltaMin(), 1e-10);
        assertEquals(2.0, s.deltaMax(), 1e-10);
    }

    // ===== start accessors =====

    @Test
    void startAccessors()
    {
        Sampling s = samplingWithSpacing();
        assertEquals(10.0, s.start(0), 1e-10);
        assertEquals(20.0, s.start(1), 1e-10);
        assertEquals(30.0, s.start(2), 1e-10);
    }

    @Test
    void startCopy()
    {
        Sampling s = samplingWithSpacing();
        Vect st = s.start();
        assertEquals(3, st.size());
        assertEquals(10.0, st.get(0), 1e-10);
    }

    // ===== first / last / center =====

    @Test
    void first()
    {
        Sampling s = sampling3x4x5();
        Sample f = s.first();
        assertEquals(0, f.getI());
        assertEquals(0, f.getJ());
        assertEquals(0, f.getK());
    }

    @Test
    void last()
    {
        Sampling s = sampling3x4x5();
        Sample l = s.last();
        assertEquals(2, l.getI());
        assertEquals(3, l.getJ());
        assertEquals(4, l.getK());
    }

    @Test
    void center()
    {
        Sampling s = sampling3x4x5();
        Sample c = s.center();
        // center uses (n-1)/2
        assertEquals(1, c.getI());  // (3-1)/2 = 1
        assertEquals(1, c.getJ());  // (4-1)/2 = 1
        assertEquals(2, c.getK());  // (5-1)/2 = 2
    }

    // ===== index conversion =====

    @Test
    void indexFromCoords()
    {
        Sampling s = sampling3x4x5();
        int idx = s.index(0, 0, 0);
        assertEquals(0, idx);
    }

    @Test
    void indexRoundtrip()
    {
        Sampling s = sampling3x4x5();
        int idx = s.index(1, 2, 3);
        Sample sample = s.sample(idx);
        assertEquals(1, sample.getI());
        assertEquals(2, sample.getJ());
        assertEquals(3, sample.getK());
    }

    @Test
    void indexFromSample()
    {
        Sampling s = sampling3x4x5();
        Sample sample = new Sample(1, 2, 3);
        int idx = s.index(sample);
        assertTrue(idx >= 0 && idx < s.size());
    }

    @Test
    void indexOutOfBoundsThrows()
    {
        // index() only checks if linear index >= size, not individual dims
        // 3x4x5 = 60 voxels, so we need idx >= 60
        Sampling s = sampling3x4x5();
        assertThrows(RuntimeException.class, () -> s.index(2, 3, 5));
    }

    // ===== containment =====

    @Test
    void containsValidIndex()
    {
        Sampling s = sampling3x4x5();
        assertTrue(s.contains(0));
        assertTrue(s.contains(s.size() - 1));
    }

    @Test
    void containsInvalidIndex()
    {
        Sampling s = sampling3x4x5();
        assertFalse(s.contains(-1));
        assertFalse(s.contains(s.size()));
    }

    @Test
    void containsValidCoords()
    {
        Sampling s = sampling3x4x5();
        assertTrue(s.contains(0, 0, 0));
        assertTrue(s.contains(2, 3, 4));
    }

    @Test
    void containsInvalidCoords()
    {
        Sampling s = sampling3x4x5();
        assertFalse(s.contains(-1, 0, 0));
        assertFalse(s.contains(3, 0, 0));
        assertFalse(s.contains(0, 4, 0));
        assertFalse(s.contains(0, 0, 5));
    }

    @Test
    void containsSample()
    {
        Sampling s = sampling3x4x5();
        assertTrue(s.contains(new Sample(0, 0, 0)));
        assertTrue(s.contains(new Sample(2, 3, 4)));
        assertFalse(s.contains(new Sample(3, 0, 0)));
    }

    @Test
    void containsSingleDimension()
    {
        Sampling s = sampling3x4x5();
        assertTrue(s.containsI(0));
        assertTrue(s.containsI(2));
        assertFalse(s.containsI(3));
        assertTrue(s.containsJ(3));
        assertFalse(s.containsJ(4));
        assertTrue(s.containsK(4));
        assertFalse(s.containsK(5));
    }

    // ===== boundary =====

    @Test
    void boundaryCorner()
    {
        Sampling s = sampling3x4x5();
        assertTrue(s.boundary(new Sample(0, 0, 0)));
        assertTrue(s.boundary(new Sample(2, 3, 4)));
    }

    @Test
    void boundaryEdge()
    {
        Sampling s = sampling3x4x5();
        assertTrue(s.boundary(new Sample(0, 1, 1)));
    }

    @Test
    void boundaryInterior()
    {
        Sampling s = sampling3x4x5();
        assertFalse(s.boundary(new Sample(1, 1, 1)));
        assertFalse(s.boundary(new Sample(1, 2, 3)));
    }

    // ===== world / voxel coordinate transforms =====

    @Test
    void worldOrigin()
    {
        Sampling s = sampling3x4x5();
        Vect w = s.world(new Sample(0, 0, 0));
        assertEquals(0.0, w.get(0), 1e-10);
        assertEquals(0.0, w.get(1), 1e-10);
        assertEquals(0.0, w.get(2), 1e-10);
    }

    @Test
    void worldWithSpacing()
    {
        Sampling s = samplingWithSpacing();
        Vect w = s.world(new Sample(2, 3, 1));
        // world = start + delta * index
        assertEquals(10.0 + 0.5 * 2, w.get(0), 1e-10);
        assertEquals(20.0 + 1.0 * 3, w.get(1), 1e-10);
        assertEquals(30.0 + 2.0 * 1, w.get(2), 1e-10);
    }

    @Test
    void worldFromIndex()
    {
        Sampling s = sampling3x4x5();
        Vect w1 = s.world(0);
        Vect w2 = s.world(new Sample(0, 0, 0));
        assertEquals(w1.get(0), w2.get(0), 1e-10);
        assertEquals(w1.get(1), w2.get(1), 1e-10);
        assertEquals(w1.get(2), w2.get(2), 1e-10);
    }

    @Test
    void nearestRoundtrip()
    {
        Sampling s = sampling3x4x5();
        Sample original = new Sample(1, 2, 3);
        Vect world = s.world(original);
        Sample nearest = s.nearest(world);
        assertEquals(original, nearest);
    }

    // ===== voxel volume =====

    @Test
    void voxelVolume()
    {
        Sampling s = samplingWithSpacing();
        assertEquals(0.5 * 1.0 * 2.0, s.voxvol(), 1e-10);
    }

    @Test
    void voxelVolumeUnit()
    {
        Sampling s = sampling3x4x5();
        assertEquals(1.0, s.voxvol(), 1e-10);
    }

    // ===== planar =====

    @Test
    void notPlanar()
    {
        Sampling s = sampling3x4x5();
        assertFalse(s.planar());
    }

    @Test
    void planar()
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(1, 1, 1);
        Sampling s = new Sampling(start, delta, new Integers(3, 4, 1));
        assertTrue(s.planar());
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Sampling s = sampling3x4x5();
        Sampling c = s.copy();
        assertEquals(s.numI(), c.numI());
        assertEquals(s.numJ(), c.numJ());
        assertEquals(s.numK(), c.numK());
        assertEquals(s.size(), c.size());
    }

    // ===== resample =====

    @Test
    void resampleUniform()
    {
        Sampling s = sampling3x4x5();
        Sampling r = s.resample(10);
        assertEquals(10, r.numI());
        assertEquals(10, r.numJ());
        assertEquals(10, r.numK());
    }

    @Test
    void resampleSpecific()
    {
        Sampling s = sampling3x4x5();
        Sampling r = s.resample(6, 8, 10);
        assertEquals(6, r.numI());
        assertEquals(8, r.numJ());
        assertEquals(10, r.numK());
    }

    // ===== grow =====

    @Test
    void grow()
    {
        Sampling s = sampling3x4x5();
        Sampling g = s.grow(2);
        assertEquals(7, g.numI());
        assertEquals(8, g.numJ());
        assertEquals(9, g.numK());
    }

    // ===== compatible =====

    @Test
    void compatibleSame()
    {
        Sampling s = sampling3x4x5();
        assertTrue(s.compatible(s.copy()));
    }

    @Test
    void compatibleDifferent()
    {
        Sampling a = sampling3x4x5();
        Sampling b = samplingWithSpacing();
        assertFalse(a.compatible(b));
    }

    // ===== equals =====

    @Test
    void equalsSame()
    {
        Sampling a = sampling3x4x5();
        Sampling b = sampling3x4x5();
        assertEquals(a, b);
    }

    // ===== iterator =====

    @Test
    void iteratorCoversAllSamples()
    {
        Sampling s = sampling3x4x5();
        int count = 0;
        for (Sample sample : s)
        {
            count++;
        }
        assertEquals(s.size(), count);
    }

    @Test
    void iteratorSamplesAreValid()
    {
        Sampling s = sampling3x4x5();
        for (Sample sample : s)
        {
            assertTrue(s.contains(sample));
        }
    }
}
