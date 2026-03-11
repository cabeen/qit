package qit.math.structs;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IntervalTest
{
    private static final double EPS = 1e-9;

    @Test
    void constructBasic()
    {
        Interval i = new Interval(1.0, 5.0);
        assertEquals(1.0, i.getMin(), EPS);
        assertEquals(5.0, i.getMax(), EPS);
    }

    @Test
    void constructReversedThrows()
    {
        // constructor asserts min < max || eq(min, max), so reversed args throw
        assertThrows(RuntimeException.class, () -> new Interval(5.0, 1.0));
    }

    @Test
    void constructDegenerate()
    {
        Interval i = new Interval(3.0, 3.0);
        assertEquals(3.0, i.getMin(), EPS);
        assertEquals(3.0, i.getMax(), EPS);
        assertEquals(0.0, i.delta(), EPS);
    }

    @Test
    void delta()
    {
        Interval i = new Interval(2.0, 7.0);
        assertEquals(5.0, i.delta(), EPS);
    }

    @Test
    void size()
    {
        Interval i = new Interval(2.0, 7.0);
        assertEquals(5.0, i.size(), EPS);
    }

    @Test
    void getHalf()
    {
        Interval i = new Interval(2.0, 8.0);
        assertEquals(5.0, i.getHalf(), EPS);
    }

    @Test
    void containsValue()
    {
        Interval i = new Interval(0.0, 10.0);
        assertTrue(i.contains(5.0));
        assertTrue(i.contains(0.0));  // boundary
        assertTrue(i.contains(10.0)); // boundary
        assertFalse(i.contains(-0.1));
        assertFalse(i.contains(10.1));
    }

    @Test
    void containsInterval()
    {
        Interval outer = new Interval(0.0, 10.0);
        Interval inner = new Interval(2.0, 8.0);
        Interval overlapping = new Interval(5.0, 15.0);

        assertTrue(outer.contains(inner));
        assertTrue(outer.contains(outer)); // self-containment
        assertFalse(outer.contains(overlapping));
        assertFalse(inner.contains(outer));
    }

    @Test
    void intersects()
    {
        Interval a = new Interval(0.0, 5.0);
        Interval b = new Interval(3.0, 8.0);
        Interval c = new Interval(6.0, 9.0);
        Interval d = new Interval(5.0, 7.0); // touching at boundary

        assertTrue(a.intersects(b));
        assertTrue(b.intersects(a));
        assertFalse(a.intersects(c));
        assertFalse(c.intersects(a));
        assertTrue(a.intersects(d)); // touching counts as intersecting
    }

    @Test
    void unionInterval()
    {
        Interval a = new Interval(1.0, 5.0);
        Interval b = new Interval(3.0, 8.0);
        Interval u = a.union(b);

        assertEquals(1.0, u.getMin(), EPS);
        assertEquals(8.0, u.getMax(), EPS);
    }

    @Test
    void unionValue()
    {
        Interval i = new Interval(2.0, 6.0);

        Interval expanded = i.union(10.0);
        assertEquals(2.0, expanded.getMin(), EPS);
        assertEquals(10.0, expanded.getMax(), EPS);

        Interval shrunkSide = i.union(-1.0);
        assertEquals(-1.0, shrunkSide.getMin(), EPS);
        assertEquals(6.0, shrunkSide.getMax(), EPS);

        Interval same = i.union(4.0); // value inside interval
        assertEquals(2.0, same.getMin(), EPS);
        assertEquals(6.0, same.getMax(), EPS);
    }

    @Test
    void grow()
    {
        Interval i = new Interval(2.0, 8.0);
        Interval grown = i.grow(1.0);
        assertEquals(1.0, grown.getMin(), EPS);
        assertEquals(9.0, grown.getMax(), EPS);
    }

    @Test
    void shift()
    {
        Interval i = new Interval(2.0, 8.0);
        Interval shifted = i.shift(3.0);
        assertEquals(5.0, shifted.getMin(), EPS);
        assertEquals(11.0, shifted.getMax(), EPS);
    }

    @Test
    void copy()
    {
        Interval i = new Interval(1.0, 5.0);
        Interval c = i.copy();
        assertEquals(i.getMin(), c.getMin(), EPS);
        assertEquals(i.getMax(), c.getMax(), EPS);
    }

    @Test
    void immutability()
    {
        Interval i = new Interval(2.0, 8.0);
        i.grow(5.0);
        i.shift(10.0);
        i.union(100.0);
        // original should be unchanged since all methods return new instances
        assertEquals(2.0, i.getMin(), EPS);
        assertEquals(8.0, i.getMax(), EPS);
    }
}
