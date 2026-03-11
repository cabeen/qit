package qit.data.datasets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VectTest
{
    private static final double EPS = 1e-9;

    // ===== construction =====

    @Test
    void constructWithSize()
    {
        Vect v = new Vect(3);
        assertEquals(3, v.size());
        assertEquals(0.0, v.get(0), EPS);
        assertEquals(0.0, v.get(1), EPS);
        assertEquals(0.0, v.get(2), EPS);
    }

    @Test
    void constructWithArray()
    {
        Vect v = new Vect(new double[]{1.0, 2.0, 3.0});
        assertEquals(3, v.size());
        assertEquals(1.0, v.get(0), EPS);
        assertEquals(2.0, v.get(1), EPS);
        assertEquals(3.0, v.get(2), EPS);
    }

    // ===== accessors =====

    @Test
    void setAndGet()
    {
        Vect v = new Vect(3);
        v.set(0, 5.0);
        v.set(1, 10.0);
        v.set(2, 15.0);
        assertEquals(5.0, v.get(0), EPS);
        assertEquals(10.0, v.get(1), EPS);
        assertEquals(15.0, v.get(2), EPS);
    }

    @Test
    void xyzAccessors()
    {
        Vect v = new Vect(new double[]{1.0, 2.0, 3.0});
        assertEquals(1.0, v.getX(), EPS);
        assertEquals(2.0, v.getY(), EPS);
        assertEquals(3.0, v.getZ(), EPS);
    }

    // ===== arithmetic (return new Vect) =====

    @Test
    void plusScalar()
    {
        Vect v = new Vect(new double[]{1.0, 2.0, 3.0});
        Vect result = v.plus(10.0);
        assertEquals(11.0, result.get(0), EPS);
        assertEquals(12.0, result.get(1), EPS);
        assertEquals(13.0, result.get(2), EPS);
        // original unchanged
        assertEquals(1.0, v.get(0), EPS);
    }

    @Test
    void plusVect()
    {
        Vect a = new Vect(new double[]{1.0, 2.0, 3.0});
        Vect b = new Vect(new double[]{4.0, 5.0, 6.0});
        Vect result = a.plus(b);
        assertEquals(5.0, result.get(0), EPS);
        assertEquals(7.0, result.get(1), EPS);
        assertEquals(9.0, result.get(2), EPS);
    }

    @Test
    void minusScalar()
    {
        Vect v = new Vect(new double[]{10.0, 20.0, 30.0});
        Vect result = v.minus(5.0);
        assertEquals(5.0, result.get(0), EPS);
        assertEquals(15.0, result.get(1), EPS);
        assertEquals(25.0, result.get(2), EPS);
    }

    @Test
    void minusVect()
    {
        Vect a = new Vect(new double[]{5.0, 8.0, 12.0});
        Vect b = new Vect(new double[]{1.0, 3.0, 7.0});
        Vect result = a.minus(b);
        assertEquals(4.0, result.get(0), EPS);
        assertEquals(5.0, result.get(1), EPS);
        assertEquals(5.0, result.get(2), EPS);
    }

    @Test
    void timesScalar()
    {
        Vect v = new Vect(new double[]{1.0, 2.0, 3.0});
        Vect result = v.times(3.0);
        assertEquals(3.0, result.get(0), EPS);
        assertEquals(6.0, result.get(1), EPS);
        assertEquals(9.0, result.get(2), EPS);
    }

    @Test
    void divScalar()
    {
        Vect v = new Vect(new double[]{10.0, 20.0, 30.0});
        Vect result = v.div(5.0);
        assertEquals(2.0, result.get(0), EPS);
        assertEquals(4.0, result.get(1), EPS);
        assertEquals(6.0, result.get(2), EPS);
    }

    // ===== geometry =====

    @Test
    void dotProduct()
    {
        Vect a = new Vect(new double[]{1.0, 2.0, 3.0});
        Vect b = new Vect(new double[]{4.0, 5.0, 6.0});
        assertEquals(32.0, a.dot(b), EPS);
    }

    @Test
    void crossProduct()
    {
        Vect x = new Vect(new double[]{1.0, 0.0, 0.0});
        Vect y = new Vect(new double[]{0.0, 1.0, 0.0});
        Vect z = x.cross(y);
        assertEquals(0.0, z.get(0), EPS);
        assertEquals(0.0, z.get(1), EPS);
        assertEquals(1.0, z.get(2), EPS);
    }

    @Test
    void crossProductAntiCommutative()
    {
        Vect a = new Vect(new double[]{1.0, 2.0, 3.0});
        Vect b = new Vect(new double[]{4.0, 5.0, 6.0});
        Vect ab = a.cross(b);
        Vect ba = b.cross(a);
        assertEquals(-ab.get(0), ba.get(0), EPS);
        assertEquals(-ab.get(1), ba.get(1), EPS);
        assertEquals(-ab.get(2), ba.get(2), EPS);
    }

    @Test
    void norm()
    {
        Vect v = new Vect(new double[]{3.0, 4.0});
        assertEquals(5.0, v.norm(), EPS);
    }

    @Test
    void norm2()
    {
        Vect v = new Vect(new double[]{3.0, 4.0});
        assertEquals(25.0, v.norm2(), EPS);
    }

    @Test
    void norm1()
    {
        Vect v = new Vect(new double[]{-3.0, 4.0, -2.0});
        assertEquals(9.0, v.norm1(), EPS);
    }

    @Test
    void dist()
    {
        Vect a = new Vect(new double[]{0.0, 0.0});
        Vect b = new Vect(new double[]{3.0, 4.0});
        assertEquals(5.0, a.dist(b), EPS);
    }

    @Test
    void normalize()
    {
        Vect v = new Vect(new double[]{3.0, 4.0, 0.0});
        Vect n = v.normalize();
        assertEquals(1.0, n.norm(), 1e-6);
        assertEquals(0.6, n.get(0), 1e-6);
        assertEquals(0.8, n.get(1), 1e-6);
    }

    @Test
    void angleDeg()
    {
        Vect x = new Vect(new double[]{1.0, 0.0, 0.0});
        Vect y = new Vect(new double[]{0.0, 1.0, 0.0});
        assertEquals(90.0, x.angleDeg(y), 1e-6);
    }

    @Test
    void angleRad()
    {
        Vect a = new Vect(new double[]{1.0, 0.0, 0.0});
        Vect b = new Vect(new double[]{1.0, 0.0, 0.0});
        assertEquals(0.0, a.angleRad(b), 1e-6);
    }

    // ===== statistics =====

    @Test
    void sum()
    {
        Vect v = new Vect(new double[]{1.0, 2.0, 3.0, 4.0});
        assertEquals(10.0, v.sum(), EPS);
    }

    @Test
    void mean()
    {
        Vect v = new Vect(new double[]{1.0, 2.0, 3.0, 4.0});
        assertEquals(2.5, v.mean(), EPS);
    }

    @Test
    void min()
    {
        Vect v = new Vect(new double[]{3.0, 1.0, 5.0, 2.0});
        assertEquals(1.0, v.min(), EPS);
    }

    @Test
    void max()
    {
        Vect v = new Vect(new double[]{3.0, 1.0, 5.0, 2.0});
        assertEquals(5.0, v.max(), EPS);
    }

    // ===== element-wise transforms =====

    @Test
    void abs()
    {
        Vect v = new Vect(new double[]{-1.0, 2.0, -3.0});
        Vect result = v.abs();
        assertEquals(1.0, result.get(0), EPS);
        assertEquals(2.0, result.get(1), EPS);
        assertEquals(3.0, result.get(2), EPS);
    }

    @Test
    void sq()
    {
        Vect v = new Vect(new double[]{2.0, 3.0, 4.0});
        Vect result = v.sq();
        assertEquals(4.0, result.get(0), EPS);
        assertEquals(9.0, result.get(1), EPS);
        assertEquals(16.0, result.get(2), EPS);
    }

    @Test
    void sqrtVect()
    {
        Vect v = new Vect(new double[]{4.0, 9.0, 16.0});
        Vect result = v.sqrt();
        assertEquals(2.0, result.get(0), EPS);
        assertEquals(3.0, result.get(1), EPS);
        assertEquals(4.0, result.get(2), EPS);
    }

    @Test
    void expAndLog()
    {
        Vect v = new Vect(new double[]{0.0, 1.0, 2.0});
        Vect e = v.exp();
        assertEquals(1.0, e.get(0), EPS);
        assertEquals(Math.E, e.get(1), 1e-6);

        // log(exp(x)) == x
        Vect roundtrip = e.log();
        assertEquals(0.0, roundtrip.get(0), 1e-6);
        assertEquals(1.0, roundtrip.get(1), 1e-6);
        assertEquals(2.0, roundtrip.get(2), 1e-6);
    }

    // ===== sub / cat =====

    @Test
    void sub()
    {
        Vect v = new Vect(new double[]{10.0, 20.0, 30.0, 40.0, 50.0});
        Vect s = v.sub(1, 4);
        assertEquals(3, s.size());
        assertEquals(20.0, s.get(0), EPS);
        assertEquals(30.0, s.get(1), EPS);
        assertEquals(40.0, s.get(2), EPS);
    }

    @Test
    void cat()
    {
        Vect a = new Vect(new double[]{1.0, 2.0});
        Vect b = new Vect(new double[]{3.0, 4.0});
        Vect c = a.cat(b);
        assertEquals(4, c.size());
        assertEquals(1.0, c.get(0), EPS);
        assertEquals(2.0, c.get(1), EPS);
        assertEquals(3.0, c.get(2), EPS);
        assertEquals(4.0, c.get(3), EPS);
    }

    // ===== predicates =====

    @Test
    void finite()
    {
        Vect good = new Vect(new double[]{1.0, 2.0, 3.0});
        assertTrue(good.finite());

        Vect bad = new Vect(new double[]{1.0, Double.NaN, 3.0});
        assertFalse(bad.finite());

        Vect inf = new Vect(new double[]{1.0, Double.POSITIVE_INFINITY, 3.0});
        assertFalse(inf.finite());
    }

    // ===== copy independence =====

    @Test
    void copy()
    {
        Vect v = new Vect(new double[]{1.0, 2.0, 3.0});
        Vect c = v.copy();
        assertEquals(v.get(0), c.get(0), EPS);
        c.set(0, 999.0);
        assertEquals(1.0, v.get(0), EPS); // original unchanged
    }

    // ===== prod / cumsum =====

    @Test
    void prod()
    {
        Vect v = new Vect(new double[]{2.0, 3.0, 4.0});
        assertEquals(24.0, v.prod(), EPS);
    }

    // ===== zero vector =====

    @Test
    void zeroVector()
    {
        Vect v = new Vect(4);
        assertEquals(0.0, v.norm(), EPS);
        assertEquals(0.0, v.sum(), EPS);
    }
}
