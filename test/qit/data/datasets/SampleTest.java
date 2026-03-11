package qit.data.datasets;

import org.junit.jupiter.api.Test;
import qit.base.structs.Integers;

import static org.junit.jupiter.api.Assertions.*;

class SampleTest
{
    // ===== construction =====

    @Test
    void constructFromCoords()
    {
        Sample s = new Sample(1, 2, 3);
        assertEquals(1, s.getI());
        assertEquals(2, s.getJ());
        assertEquals(3, s.getK());
    }

    @Test
    void constructFromArray()
    {
        Sample s = new Sample(new int[]{4, 5, 6});
        assertEquals(4, s.getI());
        assertEquals(5, s.getJ());
        assertEquals(6, s.getK());
    }

    @Test
    void constructFromIntegers()
    {
        Sample s = new Sample(new Integers(7, 8, 9));
        assertEquals(7, s.getI());
        assertEquals(8, s.getJ());
        assertEquals(9, s.getK());
    }

    @Test
    void constructFromWrongArrayLengthThrows()
    {
        assertThrows(RuntimeException.class, () -> new Sample(new int[]{1, 2}));
        assertThrows(RuntimeException.class, () -> new Sample(new int[]{1, 2, 3, 4}));
    }

    @Test
    void constructWithOffset()
    {
        Sample base = new Sample(1, 2, 3);
        Sample offset = new Sample(base, new Integers(10, 20, 30));
        assertEquals(11, offset.getI());
        assertEquals(22, offset.getJ());
        assertEquals(33, offset.getK());
    }

    // ===== accessors =====

    @Test
    void getByIndex()
    {
        Sample s = new Sample(10, 20, 30);
        assertEquals(10, s.get(0));
        assertEquals(20, s.get(1));
        assertEquals(30, s.get(2));
    }

    @Test
    void getByInvalidIndexThrows()
    {
        Sample s = new Sample(1, 2, 3);
        assertThrows(RuntimeException.class, () -> s.get(3));
        assertThrows(RuntimeException.class, () -> s.get(-1));
    }

    @Test
    void getAsArray()
    {
        Sample s = new Sample(1, 2, 3);
        int[] arr = s.get();
        assertEquals(1, arr[0]);
        assertEquals(2, arr[1]);
        assertEquals(3, arr[2]);
    }

    @Test
    void getIntoArray()
    {
        Sample s = new Sample(5, 6, 7);
        int[] out = new int[3];
        s.get(out);
        assertEquals(5, out[0]);
        assertEquals(6, out[1]);
        assertEquals(7, out[2]);
    }

    // ===== conversion =====

    @Test
    void toIntegers()
    {
        Sample s = new Sample(1, 2, 3);
        Integers ints = s.integers();
        assertEquals(1, ints.get(0));
        assertEquals(2, ints.get(1));
        assertEquals(3, ints.get(2));
    }

    @Test
    void toVect()
    {
        Sample s = new Sample(1, 2, 3);
        Vect v = s.vect();
        assertEquals(3, v.size());
        assertEquals(1.0, v.get(0), 1e-10);
        assertEquals(2.0, v.get(1), 1e-10);
        assertEquals(3.0, v.get(2), 1e-10);
    }

    // ===== offset =====

    @Test
    void offset()
    {
        Sample s = new Sample(1, 2, 3);
        Sample result = s.offset(new Integers(5, 5, 5));
        assertEquals(6, result.getI());
        assertEquals(7, result.getJ());
        assertEquals(8, result.getK());
    }

    // ===== equals / hashCode =====

    @Test
    void equalsIdentical()
    {
        Sample a = new Sample(1, 2, 3);
        Sample b = new Sample(1, 2, 3);
        assertEquals(a, b);
    }

    @Test
    void equalsDifferent()
    {
        Sample a = new Sample(1, 2, 3);
        Sample b = new Sample(1, 2, 4);
        assertNotEquals(a, b);
    }

    @Test
    void hashCodeConsistent()
    {
        Sample a = new Sample(1, 2, 3);
        Sample b = new Sample(1, 2, 3);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ===== toString =====

    @Test
    void toStringNotNull()
    {
        Sample s = new Sample(1, 2, 3);
        assertNotNull(s.toString());
    }
}
