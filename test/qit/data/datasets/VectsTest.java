package qit.data.datasets;

import org.junit.jupiter.api.Test;
import qit.data.source.VectSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class VectsTest
{
    // ===== construction =====

    @Test
    void constructEmpty()
    {
        Vects vs = new Vects();
        assertEquals(0, vs.size());
    }

    @Test
    void constructWithCapacity()
    {
        Vects vs = new Vects(100);
        assertEquals(0, vs.size());
    }

    @Test
    void constructFromList()
    {
        Vects vs = new Vects(Arrays.asList(
            VectSource.create3D(1, 2, 3),
            VectSource.create3D(4, 5, 6)));
        assertEquals(2, vs.size());
    }

    @Test
    void constructFromSingleVect()
    {
        Vect v = VectSource.create3D(1, 2, 3);
        Vects vs = new Vects(v);
        assertEquals(1, vs.size());
        assertEquals(v, vs.get(0));
    }

    @Test
    void constructCopy()
    {
        Vects original = new Vects();
        original.add(VectSource.create3D(1, 2, 3));
        Vects copy = new Vects(original);
        assertEquals(1, copy.size());
    }

    // ===== add =====

    @Test
    void addVects()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(1, 0, 0));
        vs.add(VectSource.create3D(0, 1, 0));
        vs.add(VectSource.create3D(0, 0, 1));
        assertEquals(3, vs.size());
    }

    // ===== statistics =====

    @Test
    void mean()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(2, 4, 6));
        vs.add(VectSource.create3D(4, 6, 8));
        Vect m = vs.mean();
        assertEquals(3.0, m.get(0), 1e-10);
        assertEquals(5.0, m.get(1), 1e-10);
        assertEquals(7.0, m.get(2), 1e-10);
    }

    @Test
    void sum()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(1, 2, 3));
        vs.add(VectSource.create3D(4, 5, 6));
        Vect s = vs.sum();
        assertEquals(5.0, s.get(0), 1e-10);
        assertEquals(7.0, s.get(1), 1e-10);
        assertEquals(9.0, s.get(2), 1e-10);
    }

    @Test
    void min()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(1, 5, 3));
        vs.add(VectSource.create3D(4, 2, 6));
        Vect m = vs.min();
        assertEquals(1.0, m.get(0), 1e-10);
        assertEquals(2.0, m.get(1), 1e-10);
        assertEquals(3.0, m.get(2), 1e-10);
    }

    @Test
    void max()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(1, 5, 3));
        vs.add(VectSource.create3D(4, 2, 6));
        Vect m = vs.max();
        assertEquals(4.0, m.get(0), 1e-10);
        assertEquals(5.0, m.get(1), 1e-10);
        assertEquals(6.0, m.get(2), 1e-10);
    }

    // ===== nearest =====

    @Test
    void nearest()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(0, 0, 0));
        vs.add(VectSource.create3D(10, 10, 10));
        vs.add(VectSource.create3D(5, 5, 5));

        Vect query = VectSource.create3D(4, 4, 4);
        int idx = vs.nearest(query);
        assertEquals(2, idx);
        assertEquals(5.0, vs.get(idx).get(0), 1e-10);
    }

    // ===== dim =====

    @Test
    void dimExtractsColumn()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(1, 2, 3));
        vs.add(VectSource.create3D(4, 5, 6));
        vs.add(VectSource.create3D(7, 8, 9));

        Vect col = vs.dim(1);
        assertEquals(3, col.size());
        assertEquals(2.0, col.get(0), 1e-10);
        assertEquals(5.0, col.get(1), 1e-10);
        assertEquals(8.0, col.get(2), 1e-10);
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(1, 2, 3));
        Vects c = vs.copy();
        assertEquals(1, c.size());
        assertEquals(1.0, c.get(0).get(0), 1e-10);
    }

    @Test
    void copyIndependence()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(1, 2, 3));
        Vects c = vs.copy();
        c.add(VectSource.create3D(4, 5, 6));
        assertEquals(1, vs.size());
        assertEquals(2, c.size());
    }

    // ===== flatten =====

    @Test
    void flatten()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(1, 2, 3));
        vs.add(VectSource.create3D(4, 5, 6));

        Vect flat = vs.flatten();
        assertEquals(6, flat.size());
        assertEquals(1.0, flat.get(0), 1e-10);
        assertEquals(4.0, flat.get(3), 1e-10);
    }

    // ===== toNumDimArray =====

    @Test
    void toNumDimArray()
    {
        Vects vs = new Vects();
        vs.add(VectSource.create3D(1, 2, 3));
        vs.add(VectSource.create3D(4, 5, 6));

        double[][] arr = vs.toNumDimArray();
        assertEquals(2, arr.length);
        assertEquals(3, arr[0].length);
        assertEquals(1.0, arr[0][0], 1e-10);
        assertEquals(6.0, arr[1][2], 1e-10);
    }
}
