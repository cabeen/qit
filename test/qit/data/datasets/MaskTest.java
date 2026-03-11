package qit.data.datasets;

import org.junit.jupiter.api.Test;
import qit.base.structs.Integers;
import qit.data.source.VectSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MaskTest
{
    private Sampling sampling3x4x5()
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(1, 1, 1);
        return new Sampling(start, delta, new Integers(3, 4, 5));
    }

    private Mask mask3x4x5()
    {
        return new Mask(sampling3x4x5());
    }

    // ===== construction =====

    @Test
    void constructBasic()
    {
        Mask m = mask3x4x5();
        assertNotNull(m);
        assertEquals(3, m.getSampling().numI());
        assertEquals(4, m.getSampling().numJ());
        assertEquals(5, m.getSampling().numK());
    }

    @Test
    void initialValuesAreZero()
    {
        Mask m = mask3x4x5();
        for (Sample s : m.getSampling())
        {
            assertEquals(0, m.get(s));
        }
    }

    // ===== get / set =====

    @Test
    void setAndGetBySample()
    {
        Mask m = mask3x4x5();
        Sample s = new Sample(1, 2, 3);
        m.set(s, 5);
        assertEquals(5, m.get(s));
    }

    @Test
    void setAndGetByIndex()
    {
        Mask m = mask3x4x5();
        m.set(0, 1);
        assertEquals(1, m.get(0));
    }

    @Test
    void setAndGetByCoords()
    {
        Mask m = mask3x4x5();
        m.set(2, 3, 4, 7);
        assertEquals(7, m.get(2, 3, 4));
    }

    @Test
    void setAndGetByArray()
    {
        Mask m = mask3x4x5();
        m.set(new int[]{1, 1, 1}, 3);
        assertEquals(3, m.get(new int[]{1, 1, 1}));
    }

    // ===== foreground / background =====

    @Test
    void backgroundByDefault()
    {
        Mask m = mask3x4x5();
        Sample s = new Sample(0, 0, 0);
        assertTrue(m.background(s));
        assertFalse(m.foreground(s));
    }

    @Test
    void foregroundAfterSet()
    {
        Mask m = mask3x4x5();
        Sample s = new Sample(1, 1, 1);
        m.set(s, 1);
        assertTrue(m.foreground(s));
        assertFalse(m.background(s));
    }

    @Test
    void foregroundByIndex()
    {
        Mask m = mask3x4x5();
        m.set(5, 1);
        assertTrue(m.foreground(5));
        assertFalse(m.background(5));
    }

    @Test
    void foregroundByCoords()
    {
        Mask m = mask3x4x5();
        m.set(1, 2, 3, 2);
        assertTrue(m.foreground(1, 2, 3));
        assertFalse(m.background(1, 2, 3));
    }

    @Test
    void nonZeroLabelIsForeground()
    {
        Mask m = mask3x4x5();
        m.set(new Sample(0, 0, 0), 42);
        assertTrue(m.foreground(new Sample(0, 0, 0)));
    }

    // ===== setAll =====

    @Test
    void setAllLabel()
    {
        Mask m = mask3x4x5();
        m.setAll(3);
        for (Sample s : m.getSampling())
        {
            assertEquals(3, m.get(s));
        }
    }

    @Test
    void setAllWithMask()
    {
        Mask m = mask3x4x5();
        Mask region = mask3x4x5();
        region.set(new Sample(1, 1, 1), 1);
        region.set(new Sample(2, 2, 2), 1);

        m.setAll(region, 5);
        assertEquals(5, m.get(new Sample(1, 1, 1)));
        assertEquals(5, m.get(new Sample(2, 2, 2)));
        assertEquals(0, m.get(new Sample(0, 0, 0)));
    }

    // ===== valid =====

    @Test
    void validSample()
    {
        Mask m = mask3x4x5();
        assertTrue(m.valid(new Sample(0, 0, 0)));
        assertTrue(m.valid(new Sample(2, 3, 4)));
        assertFalse(m.valid(new Sample(3, 0, 0)));
    }

    @Test
    void validCoords()
    {
        Mask m = mask3x4x5();
        assertTrue(m.valid(0, 0, 0));
        assertFalse(m.valid(-1, 0, 0));
    }

    @Test
    void validWithMask()
    {
        Mask m = mask3x4x5();
        Mask region = mask3x4x5();
        region.set(new Sample(1, 1, 1), 1);

        assertTrue(m.valid(new Sample(1, 1, 1), region));
        assertFalse(m.valid(new Sample(0, 0, 0), region));
    }

    @Test
    void validWithNullMask()
    {
        Mask m = mask3x4x5();
        assertTrue(m.valid(new Sample(0, 0, 0), null));
    }

    // ===== label management =====

    @Test
    void setAndGetName()
    {
        Mask m = mask3x4x5();
        m.setName(1, "brain");
        assertTrue(m.hasName(1));
        assertEquals("brain", m.getName(1));
    }

    @Test
    void getNameDefault()
    {
        Mask m = mask3x4x5();
        String name = m.getName(5);
        assertNotNull(name);
        assertTrue(name.contains("5"));
    }

    @Test
    void hasLabel()
    {
        Mask m = mask3x4x5();
        m.setName(1, "brain");
        assertTrue(m.hasLabel(1));
        assertFalse(m.hasLabel(99));
    }

    @Test
    void getDefinedLabels()
    {
        Mask m = mask3x4x5();
        m.setName(1, "brain");
        m.setName(2, "csf");
        Set<Integer> labels = m.getDefinedLabels();
        assertTrue(labels.contains(1));
        assertTrue(labels.contains(2));
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Mask m = mask3x4x5();
        m.set(new Sample(1, 1, 1), 5);
        Mask c = m.copy();
        assertEquals(5, c.get(new Sample(1, 1, 1)));
        assertEquals(m.getSampling().size(), c.getSampling().size());
    }

    @Test
    void copyIndependence()
    {
        Mask m = mask3x4x5();
        m.set(new Sample(1, 1, 1), 5);
        Mask c = m.copy();
        c.set(new Sample(1, 1, 1), 99);
        assertEquals(5, m.get(new Sample(1, 1, 1)));
        assertEquals(99, c.get(new Sample(1, 1, 1)));
    }

    // ===== proto =====

    @Test
    void proto()
    {
        Mask m = mask3x4x5();
        m.set(new Sample(1, 1, 1), 5);
        Mask p = m.proto();
        assertEquals(m.getSampling().size(), p.getSampling().size());
        assertEquals(0, p.get(new Sample(1, 1, 1)));
    }

    // ===== set from mask =====

    @Test
    void setFromMask()
    {
        Mask a = mask3x4x5();
        a.set(new Sample(1, 1, 1), 3);

        Mask b = mask3x4x5();
        b.set(a);
        assertEquals(3, b.get(new Sample(1, 1, 1)));
    }

    // ===== volume conversion =====

    @Test
    void protoVolume()
    {
        Mask m = mask3x4x5();
        Volume v = m.protoVolume();
        assertEquals(1, v.getDim());
        assertEquals(m.getSampling().size(), v.getSampling().size());
    }

    @Test
    void protoVolumeMultiDim()
    {
        Mask m = mask3x4x5();
        Volume v = m.protoVolume(3);
        assertEquals(3, v.getDim());
    }

    @Test
    void copyVolume()
    {
        Mask m = mask3x4x5();
        m.set(new Sample(1, 1, 1), 7);
        Volume v = m.copyVolume();
        assertEquals(7.0, v.get(new Sample(1, 1, 1), 0), 1e-10);
        assertEquals(0.0, v.get(new Sample(0, 0, 0), 0), 1e-10);
    }

    // ===== vect =====

    @Test
    void vect()
    {
        Sampling s = new Sampling(
            VectSource.create3D(0, 0, 0),
            VectSource.create3D(1, 1, 1),
            new Integers(2, 2, 1));
        Mask m = new Mask(s);
        m.set(0, 0, 0, 1);
        m.set(1, 0, 0, 2);

        Vect flat = m.vect();
        assertEquals(4, flat.size());
    }
}
