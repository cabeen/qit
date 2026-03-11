package qit.math.structs;

import org.junit.jupiter.api.Test;
import qit.data.datasets.Vect;

import static org.junit.jupiter.api.Assertions.*;

class BoxTest
{
    private static final double EPS = 1e-9;

    private Box box3D(double minX, double maxX, double minY, double maxY, double minZ, double maxZ)
    {
        return new Box(new Interval[]{
            new Interval(minX, maxX),
            new Interval(minY, maxY),
            new Interval(minZ, maxZ)
        });
    }

    // ===== creation =====

    @Test
    void createFromPoint()
    {
        Vect p = new Vect(new double[]{1.0, 2.0, 3.0});
        Box b = Box.create(p);
        assertEquals(3, b.dim());
        assertEquals(1.0, b.getMin().get(0), EPS);
        assertEquals(1.0, b.getMax().get(0), EPS);
        assertEquals(2.0, b.getMin().get(1), EPS);
        assertEquals(3.0, b.getMax().get(2), EPS);
    }

    @Test
    void createUnionTwoPoints()
    {
        Vect a = new Vect(new double[]{0, 0, 0});
        Vect b = new Vect(new double[]{1, 2, 3});
        Box box = Box.createUnion(a, b);
        assertEquals(0.0, box.getMin().get(0), EPS);
        assertEquals(1.0, box.getMax().get(0), EPS);
        assertEquals(0.0, box.getMin().get(1), EPS);
        assertEquals(2.0, box.getMax().get(1), EPS);
        assertEquals(3.0, box.getMax().get(2), EPS);
    }

    @Test
    void createUnionThreePoints()
    {
        Vect a = new Vect(new double[]{0, 5, 0});
        Vect b = new Vect(new double[]{3, 0, 0});
        Vect c = new Vect(new double[]{0, 0, 7});
        Box box = Box.createUnion(a, b, c);
        assertEquals(0.0, box.getMin().get(0), EPS);
        assertEquals(3.0, box.getMax().get(0), EPS);
        assertEquals(0.0, box.getMin().get(1), EPS);
        assertEquals(5.0, box.getMax().get(1), EPS);
        assertEquals(7.0, box.getMax().get(2), EPS);
    }

    @Test
    void createRadius()
    {
        Vect center = new Vect(new double[]{5, 5, 5});
        Box b = Box.createRadius(center, 2.0);
        assertEquals(3.0, b.getMin().get(0), EPS);
        assertEquals(7.0, b.getMax().get(0), EPS);
    }

    // ===== accessors =====

    @Test
    void dim()
    {
        Box b = box3D(0, 1, 0, 1, 0, 1);
        assertEquals(3, b.dim());
    }

    @Test
    void getCenter()
    {
        Box b = box3D(0, 10, 0, 20, 0, 30);
        Vect center = b.getCenter();
        assertEquals(5.0, center.get(0), EPS);
        assertEquals(10.0, center.get(1), EPS);
        assertEquals(15.0, center.get(2), EPS);
    }

    @Test
    void getDiameter()
    {
        // unit cube: diameter = sqrt(3)
        Box b = box3D(0, 1, 0, 1, 0, 1);
        assertEquals(Math.sqrt(3.0), b.getDiameter(), EPS);
    }

    @Test
    void getInterval()
    {
        Box b = box3D(1, 5, 2, 8, 3, 9);
        Interval i = b.getInterval(0);
        assertEquals(1.0, i.getMin(), EPS);
        assertEquals(5.0, i.getMax(), EPS);
    }

    // ===== contains =====

    @Test
    void containsPoint()
    {
        Box b = box3D(0, 10, 0, 10, 0, 10);
        assertTrue(b.contains(new Vect(new double[]{5, 5, 5})));
        assertTrue(b.contains(new Vect(new double[]{0, 0, 0})));    // boundary
        assertTrue(b.contains(new Vect(new double[]{10, 10, 10}))); // boundary
        assertFalse(b.contains(new Vect(new double[]{11, 5, 5})));
        assertFalse(b.contains(new Vect(new double[]{5, -1, 5})));
    }

    @Test
    void containsBox()
    {
        Box outer = box3D(0, 10, 0, 10, 0, 10);
        Box inner = box3D(2, 8, 2, 8, 2, 8);
        Box overlapping = box3D(5, 15, 5, 15, 5, 15);
        assertTrue(outer.contains(inner));
        assertTrue(outer.contains(outer));
        assertFalse(outer.contains(overlapping));
        assertFalse(inner.contains(outer));
    }

    // ===== intersects =====

    @Test
    void intersects()
    {
        Box a = box3D(0, 5, 0, 5, 0, 5);
        Box b = box3D(3, 8, 3, 8, 3, 8);
        Box c = box3D(6, 9, 6, 9, 6, 9);
        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
        assertFalse(a.intersects(c));
        assertFalse(c.intersects(a));
    }

    @Test
    void intersectsTouching()
    {
        Box a = box3D(0, 5, 0, 5, 0, 5);
        Box b = box3D(5, 10, 5, 10, 5, 10);
        assertTrue(a.intersects(b)); // touching at corner
    }

    // ===== union =====

    @Test
    void unionBox()
    {
        Box a = box3D(0, 5, 0, 5, 0, 5);
        Box b = box3D(3, 10, 3, 10, 3, 10);
        Box u = a.union(b);
        assertEquals(0.0, u.getMin().get(0), EPS);
        assertEquals(10.0, u.getMax().get(0), EPS);
    }

    @Test
    void unionVect()
    {
        Box b = box3D(0, 5, 0, 5, 0, 5);
        Box expanded = b.union(new Vect(new double[]{10, 10, 10}));
        assertEquals(10.0, expanded.getMax().get(0), EPS);
        assertEquals(0.0, expanded.getMin().get(0), EPS);
    }

    // ===== grow / scale / shift =====

    @Test
    void grow()
    {
        Box b = box3D(2, 8, 2, 8, 2, 8);
        Box grown = b.grow(1.0);
        assertEquals(1.0, grown.getMin().get(0), EPS);
        assertEquals(9.0, grown.getMax().get(0), EPS);
    }

    @Test
    void scale()
    {
        Box b = box3D(0, 10, 0, 10, 0, 10);
        Box scaled = b.scale(2.0);
        // size doubles from 10 to 20, so min becomes -5, max becomes 15
        assertEquals(-5.0, scaled.getMin().get(0), EPS);
        assertEquals(15.0, scaled.getMax().get(0), EPS);
    }

    @Test
    void shift()
    {
        Box b = box3D(0, 5, 0, 5, 0, 5);
        Box shifted = b.shift(new Vect(new double[]{10, 20, 30}));
        assertEquals(10.0, shifted.getMin().get(0), EPS);
        assertEquals(15.0, shifted.getMax().get(0), EPS);
        assertEquals(20.0, shifted.getMin().get(1), EPS);
        assertEquals(25.0, shifted.getMax().get(1), EPS);
    }

    @Test
    void buffer()
    {
        Box b = box3D(2, 8, 2, 8, 2, 8);
        Box buffered = b.buffer(1.0);
        assertEquals(1.0, buffered.getMin().get(0), EPS);
        assertEquals(9.0, buffered.getMax().get(0), EPS);
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Box b = box3D(1, 5, 2, 8, 3, 9);
        Box c = b.copy();
        assertEquals(b.getMin().get(0), c.getMin().get(0), EPS);
        assertEquals(b.getMax().get(2), c.getMax().get(2), EPS);
    }

    // ===== immutability =====

    @Test
    void immutability()
    {
        Box b = box3D(0, 10, 0, 10, 0, 10);
        b.grow(5);
        b.scale(2.0);
        b.union(new Vect(new double[]{100, 100, 100}));
        // all methods return new Box, original unchanged
        assertEquals(0.0, b.getMin().get(0), EPS);
        assertEquals(10.0, b.getMax().get(0), EPS);
    }

    // ===== label =====

    @Test
    void label()
    {
        Box b = box3D(0, 10, 0, 10, 0, 10);
        assertEquals(1, b.label(new Vect(new double[]{5, 5, 5})));
        assertEquals(0, b.label(new Vect(new double[]{15, 5, 5})));
    }
}
