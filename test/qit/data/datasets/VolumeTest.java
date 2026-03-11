package qit.data.datasets;

import org.junit.jupiter.api.Test;
import qit.base.structs.DataType;
import qit.base.structs.Integers;
import qit.data.source.VectSource;

import static org.junit.jupiter.api.Assertions.*;

class VolumeTest
{
    private Sampling sampling3x4x5()
    {
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(1, 1, 1);
        return new Sampling(start, delta, new Integers(3, 4, 5));
    }

    private Volume volume3x4x5()
    {
        return new Volume(sampling3x4x5(), DataType.DOUBLE, 1);
    }

    private Volume volume3x4x5dim3()
    {
        return new Volume(sampling3x4x5(), DataType.DOUBLE, 3);
    }

    // ===== construction =====

    @Test
    void constructBasic()
    {
        Volume v = volume3x4x5();
        assertNotNull(v);
        assertEquals(1, v.getDim());
        assertEquals(DataType.DOUBLE, v.getType());
    }

    @Test
    void constructMultiDim()
    {
        Volume v = volume3x4x5dim3();
        assertEquals(3, v.getDim());
    }

    @Test
    void constructFloat()
    {
        Volume v = new Volume(sampling3x4x5(), DataType.FLOAT, 1);
        assertEquals(DataType.FLOAT, v.getType());
    }

    @Test
    void constructInt()
    {
        Volume v = new Volume(sampling3x4x5(), DataType.INT, 2);
        assertEquals(DataType.INT, v.getType());
        assertEquals(2, v.getDim());
    }

    // ===== sampling =====

    @Test
    void getSampling()
    {
        Volume v = volume3x4x5();
        Sampling s = v.getSampling();
        assertNotNull(s);
        assertEquals(3, s.numI());
        assertEquals(4, s.numJ());
        assertEquals(5, s.numK());
    }

    // ===== get / set scalar =====

    @Test
    void setAndGetByIndex()
    {
        Volume v = volume3x4x5();
        v.set(0, 0, 42.0);
        assertEquals(42.0, v.get(0, 0), 1e-10);
    }

    @Test
    void setAndGetByCoords()
    {
        Volume v = volume3x4x5();
        v.set(1, 2, 3, 99.0);
        assertEquals(99.0, v.get(1, 2, 3, 0), 1e-10);
    }

    @Test
    void setAndGetBySample()
    {
        Volume v = volume3x4x5();
        Sample s = new Sample(1, 2, 3);
        v.set(s, 7.5);
        assertEquals(7.5, v.get(s, 0), 1e-10);
    }

    @Test
    void setAndGetMultiDim()
    {
        Volume v = volume3x4x5dim3();
        v.set(0, 0, 0, 0, 1.0);
        v.set(0, 0, 0, 1, 2.0);
        v.set(0, 0, 0, 2, 3.0);
        assertEquals(1.0, v.get(0, 0, 0, 0), 1e-10);
        assertEquals(2.0, v.get(0, 0, 0, 1), 1e-10);
        assertEquals(3.0, v.get(0, 0, 0, 2), 1e-10);
    }

    // ===== get / set Vect =====

    @Test
    void setAndGetVect()
    {
        Volume v = volume3x4x5dim3();
        Vect val = VectSource.create3D(1.0, 2.0, 3.0);
        Sample s = new Sample(1, 1, 1);
        v.set(s, val);
        Vect result = v.get(s);
        assertEquals(1.0, result.get(0), 1e-10);
        assertEquals(2.0, result.get(1), 1e-10);
        assertEquals(3.0, result.get(2), 1e-10);
    }

    @Test
    void setAndGetVectByCoords()
    {
        Volume v = volume3x4x5dim3();
        Vect val = VectSource.create3D(4.0, 5.0, 6.0);
        v.set(1, 2, 3, val);
        Vect result = v.get(1, 2, 3);
        assertEquals(4.0, result.get(0), 1e-10);
        assertEquals(5.0, result.get(1), 1e-10);
        assertEquals(6.0, result.get(2), 1e-10);
    }

    // ===== initial values =====

    @Test
    void initialValuesAreZero()
    {
        Volume v = volume3x4x5();
        for (Sample s : v.getSampling())
        {
            assertEquals(0.0, v.get(s, 0), 1e-10);
        }
    }

    // ===== setAll =====

    @Test
    void setAllVect()
    {
        Volume v = volume3x4x5dim3();
        Vect val = VectSource.create3D(1.0, 1.0, 1.0);
        v.setAll(val);
        for (Sample s : v.getSampling())
        {
            assertEquals(1.0, v.get(s, 0), 1e-10);
            assertEquals(1.0, v.get(s, 1), 1e-10);
            assertEquals(1.0, v.get(s, 2), 1e-10);
        }
    }

    @Test
    void setAllWithMask()
    {
        Volume v = volume3x4x5();
        Mask m = new Mask(v.getSampling());
        m.set(new Sample(1, 1, 1), 1);
        m.set(new Sample(2, 2, 2), 1);

        Vect val = new Vect(1);
        val.set(0, 42.0);
        v.setAll(m, val);

        assertEquals(42.0, v.get(new Sample(1, 1, 1), 0), 1e-10);
        assertEquals(42.0, v.get(new Sample(2, 2, 2), 0), 1e-10);
        assertEquals(0.0, v.get(new Sample(0, 0, 0), 0), 1e-10);
    }

    // ===== valid =====

    @Test
    void validSample()
    {
        Volume v = volume3x4x5();
        assertTrue(v.valid(new Sample(0, 0, 0)));
        assertTrue(v.valid(new Sample(2, 3, 4)));
        assertFalse(v.valid(new Sample(3, 0, 0)));
    }

    @Test
    void validCoords()
    {
        Volume v = volume3x4x5();
        assertTrue(v.valid(0, 0, 0));
        assertTrue(v.valid(2, 3, 4));
        assertFalse(v.valid(3, 0, 0));
    }

    @Test
    void validWithMask()
    {
        Volume v = volume3x4x5();
        Mask m = new Mask(v.getSampling());
        m.set(new Sample(1, 1, 1), 1);

        assertTrue(v.valid(new Sample(1, 1, 1), m));
        assertFalse(v.valid(new Sample(0, 0, 0), m));
    }

    @Test
    void validWithNullMask()
    {
        Volume v = volume3x4x5();
        assertTrue(v.valid(new Sample(0, 0, 0), null));
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Volume v = volume3x4x5();
        v.set(0, 0, 0, 42.0);
        Volume c = v.copy();
        assertEquals(42.0, c.get(0, 0, 0, 0), 1e-10);
        assertEquals(v.getDim(), c.getDim());
        assertEquals(v.getSampling().size(), c.getSampling().size());
    }

    @Test
    void copyIndependence()
    {
        Volume v = volume3x4x5();
        v.set(0, 0, 0, 42.0);
        Volume c = v.copy();
        c.set(0, 0, 0, 99.0);
        assertEquals(42.0, v.get(0, 0, 0, 0), 1e-10);
        assertEquals(99.0, c.get(0, 0, 0, 0), 1e-10);
    }

    // ===== proto =====

    @Test
    void proto()
    {
        Volume v = volume3x4x5dim3();
        v.set(0, 0, 0, 0, 42.0);
        Volume p = v.proto();
        assertEquals(v.getDim(), p.getDim());
        assertEquals(v.getSampling().size(), p.getSampling().size());
        assertEquals(0.0, p.get(0, 0, 0, 0), 1e-10);
    }

    @Test
    void protoNewDim()
    {
        Volume v = volume3x4x5();
        Volume p = v.proto(5);
        assertEquals(5, p.getDim());
    }

    @Test
    void protoNewSampling()
    {
        Volume v = volume3x4x5();
        Vect start = VectSource.create3D(0, 0, 0);
        Vect delta = VectSource.create3D(1, 1, 1);
        Sampling newSampling = new Sampling(start, delta, new Integers(10, 10, 10));
        Volume p = v.proto(newSampling);
        assertEquals(10, p.getSampling().numI());
    }

    // ===== dproto =====

    @Test
    void dproto()
    {
        Volume v = volume3x4x5dim3();
        Vect vp = v.dproto();
        assertEquals(3, vp.size());
        assertEquals(0.0, vp.get(0), 1e-10);
    }

    // ===== getVolume / setVolume =====

    @Test
    void getVolumeSingleDim()
    {
        Volume v = volume3x4x5dim3();
        v.set(0, 0, 0, 1, 42.0);
        Volume sub = v.getVolume(1);
        assertEquals(1, sub.getDim());
        assertEquals(42.0, sub.get(0, 0, 0, 0), 1e-10);
    }

    @Test
    void setVolumeSingleDim()
    {
        Volume v = volume3x4x5dim3();
        Volume sub = new Volume(sampling3x4x5(), DataType.DOUBLE, 1);
        sub.set(0, 0, 0, 99.0);
        v.setVolume(2, sub);
        assertEquals(99.0, v.get(0, 0, 0, 2), 1e-10);
    }

    // ===== set from volume =====

    @Test
    void setFromVolume()
    {
        Volume a = volume3x4x5();
        a.set(1, 1, 1, 42.0);

        Volume b = volume3x4x5();
        b.set(a);
        assertEquals(42.0, b.get(1, 1, 1, 0), 1e-10);
    }

    // ===== vect =====

    @Test
    void vect()
    {
        Sampling s = new Sampling(
            VectSource.create3D(0, 0, 0),
            VectSource.create3D(1, 1, 1),
            new Integers(2, 2, 1));
        Volume v = new Volume(s, DataType.DOUBLE, 1);
        v.set(0, 0, 0, 1.0);
        v.set(1, 0, 0, 2.0);
        v.set(0, 1, 0, 3.0);
        v.set(1, 1, 0, 4.0);

        Vect flat = v.vect();
        assertEquals(4, flat.size());
    }
}
