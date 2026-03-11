package qit.math.utils;

import org.junit.jupiter.api.Test;
import qit.base.Global;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MathUtilsTest
{
    private static final double EPS = 1e-9;

    // ===== equality / comparison =====

    @Test
    void eqWithinDelta()
    {
        assertTrue(MathUtils.eq(1.0, 1.0));
        assertTrue(MathUtils.eq(1.0, 1.0 + 1e-7)); // within default DELTA=1e-6
        assertFalse(MathUtils.eq(1.0, 1.0 + 1e-5)); // outside DELTA
    }

    @Test
    void eqCustomThreshold()
    {
        assertTrue(MathUtils.eq(1.0, 1.5, 0.6));
        assertFalse(MathUtils.eq(1.0, 1.5, 0.4));
    }

    @Test
    void zero()
    {
        assertTrue(MathUtils.zero(0.0));
        assertTrue(MathUtils.zero(1e-7));
        assertFalse(MathUtils.zero(1e-5));
    }

    @Test
    void nonzero()
    {
        assertFalse(MathUtils.nonzero(0.0));
        assertTrue(MathUtils.nonzero(1.0));
    }

    @Test
    void unit()
    {
        assertTrue(MathUtils.unit(1.0));
        assertTrue(MathUtils.unit(1.0 + 1e-7));
        assertFalse(MathUtils.unit(0.5));
    }

    @Test
    void valid()
    {
        assertTrue(MathUtils.valid(0.0));
        assertTrue(MathUtils.valid(1e308));
        assertTrue(MathUtils.valid(-1e308));
        assertFalse(MathUtils.valid(Double.NaN));
        assertFalse(MathUtils.valid(Double.POSITIVE_INFINITY));
        assertFalse(MathUtils.valid(Double.NEGATIVE_INFINITY));
    }

    @Test
    void openAndClosed()
    {
        assertTrue(MathUtils.open(5.0, 0.0, 10.0));
        assertFalse(MathUtils.open(0.0, 0.0, 10.0)); // boundary excluded
        assertFalse(MathUtils.open(10.0, 0.0, 10.0));

        assertTrue(MathUtils.closed(5.0, 0.0, 10.0));
        assertTrue(MathUtils.closed(0.0, 0.0, 10.0)); // boundary included
        assertTrue(MathUtils.closed(10.0, 0.0, 10.0));
        assertFalse(MathUtils.closed(-1.0, 0.0, 10.0));
    }

    // ===== sign / rounding =====

    @Test
    void sign()
    {
        assertEquals(1, MathUtils.sign(5.0));
        assertEquals(-1, MathUtils.sign(-3.0));
        assertEquals(1, MathUtils.sign(0.0)); // sign(0) = 1
    }

    @Test
    void signzero()
    {
        assertEquals(1, MathUtils.signzero(5.0));
        assertEquals(-1, MathUtils.signzero(-3.0));
        assertEquals(0, MathUtils.signzero(0.0));
    }

    @Test
    void round()
    {
        assertEquals(3, MathUtils.round(2.7));
        assertEquals(2, MathUtils.round(2.3));
        // round uses ipart(x+0.5), so for negatives: ipart(-2.7+0.5) = ipart(-2.2) = -2
        assertEquals(-2, MathUtils.round(-2.7));
    }

    @Test
    void ipart()
    {
        assertEquals(3, MathUtils.ipart(3.7));
        assertEquals(-3, MathUtils.ipart(-3.7));
    }

    @Test
    void fpart()
    {
        assertEquals(0.7, MathUtils.fpart(3.7), 1e-6);
    }

    // ===== pure math functions =====

    @Test
    void gaussian()
    {
        // standard normal at mean should give 1/sqrt(2*pi) ~ 0.3989
        double atMean = MathUtils.gaussian(0.0, 0.0, 1.0);
        assertEquals(1.0 / Math.sqrt(2 * Math.PI), atMean, 1e-10);

        // symmetric: f(mean+1) == f(mean-1)
        double above = MathUtils.gaussian(1.0, 0.0, 1.0);
        double below = MathUtils.gaussian(-1.0, 0.0, 1.0);
        assertEquals(above, below, 1e-10);

        // wider std -> lower peak
        double wide = MathUtils.gaussian(0.0, 0.0, 2.0);
        assertTrue(wide < atMean);
    }

    @Test
    void cubicthresh()
    {
        assertEquals(0.0, MathUtils.cubicthresh(-1.0), EPS);
        assertEquals(0.0, MathUtils.cubicthresh(0.0), EPS);
        assertEquals(1.0, MathUtils.cubicthresh(1.0), EPS);
        assertEquals(1.0, MathUtils.cubicthresh(2.0), EPS);
        assertEquals(0.5, MathUtils.cubicthresh(0.5), EPS); // 3*(0.25) - 2*(0.125) = 0.5
    }

    @Test
    void logGamma()
    {
        // logGamma(1) = log(0!) = 0
        assertEquals(0.0, MathUtils.logGamma(1.0), 1e-6);
        // logGamma(2) = log(1!) = 0
        assertEquals(0.0, MathUtils.logGamma(2.0), 1e-6);
        // logGamma(6) = log(5!) = log(120)
        assertEquals(Math.log(120.0), MathUtils.logGamma(6.0), 1e-6);
    }

    @Test
    void factorial()
    {
        assertEquals(1.0, MathUtils.factorial(0), EPS);
        assertEquals(1.0, MathUtils.factorial(1), EPS);
        assertEquals(2.0, MathUtils.factorial(2), 1e-6);
        assertEquals(6.0, MathUtils.factorial(3), 1e-6);
        assertEquals(24.0, MathUtils.factorial(4), 1e-6);
        assertEquals(120.0, MathUtils.factorial(5), 1e-4);
    }

    @Test
    void sinc()
    {
        // sinc(0) = 1 by convention
        assertEquals(1.0, MathUtils.sinc(0.0), EPS);
        // sinc(pi) = sin(pi)/pi ~ 0
        assertEquals(0.0, MathUtils.sinc(Math.PI), 1e-10);
    }

    @Test
    void logistic()
    {
        // logistic with gain=1, offset=0 at value=0 should be 0.5
        double mid = MathUtils.logistic(1.0, 0.0, 0.0);
        assertEquals(0.5, mid, 1e-6);

        // large positive -> near 1
        double high = MathUtils.logistic(1.0, 0.0, 100.0);
        assertTrue(high > 0.999);

        // large negative -> near 0
        double low = MathUtils.logistic(1.0, 0.0, -100.0);
        assertTrue(low < 0.001);
    }

    @Test
    void square()
    {
        assertEquals(9.0, MathUtils.square(3.0), EPS);
        assertEquals(9.0, MathUtils.square(-3.0), EPS);
        assertEquals(0.0, MathUtils.square(0.0), EPS);
    }

    @Test
    void ramp()
    {
        // ramp maps value from [low,high] to [0,1]
        assertEquals(0.0, MathUtils.ramp(0.0, 0.0, 10.0), EPS);
        assertEquals(1.0, MathUtils.ramp(10.0, 0.0, 10.0), EPS);
        assertEquals(0.5, MathUtils.ramp(5.0, 0.0, 10.0), EPS);
    }

    @Test
    void unitmap()
    {
        // maps value in [0,1] to [low,high]
        assertEquals(0.0, MathUtils.unitmap(0.0, 0.0, 10.0), EPS);
        assertEquals(10.0, MathUtils.unitmap(1.0, 0.0, 10.0), EPS);
        assertEquals(5.0, MathUtils.unitmap(0.5, 0.0, 10.0), EPS);
    }

    // ===== number parsing =====

    @Test
    void number()
    {
        assertTrue(MathUtils.number("3.14"));
        assertTrue(MathUtils.number("-1"));
        assertTrue(MathUtils.number("0"));
        assertFalse(MathUtils.number("abc"));
        assertFalse(MathUtils.number(""));
    }

    @Test
    void parse()
    {
        assertEquals(3.14, MathUtils.parse("3.14", 0.0), EPS);
        assertEquals(0.0, MathUtils.parse("abc", 0.0), EPS);
    }

    // ===== array operations =====

    @Test
    void sumDoubleArray()
    {
        assertEquals(10.0, MathUtils.sum(new double[]{1.0, 2.0, 3.0, 4.0}), EPS);
        assertEquals(0.0, MathUtils.sum(new double[]{}), EPS);
    }

    @Test
    void sumIntArray()
    {
        assertEquals(10, MathUtils.sum(new int[]{1, 2, 3, 4}));
    }

    @Test
    void mean()
    {
        assertEquals(2.5, MathUtils.mean(new double[]{1.0, 2.0, 3.0, 4.0}), EPS);
    }

    @Test
    void var()
    {
        // variance of {1,2,3,4}: mean=2.5, var = ((1.5^2+0.5^2+0.5^2+1.5^2)/4) = 1.25
        assertEquals(1.25, MathUtils.var(new double[]{1.0, 2.0, 3.0, 4.0}), 1e-6);
    }

    @Test
    void dotProduct()
    {
        assertEquals(32.0, MathUtils.dot(new double[]{1, 2, 3}, new double[]{4, 5, 6}), EPS);
    }

    @Test
    void norm()
    {
        assertEquals(5.0, MathUtils.norm(new double[]{3.0, 4.0}), EPS);
        assertEquals(0.0, MathUtils.norm(new double[]{0.0, 0.0}), EPS);
    }

    @Test
    void linspace()
    {
        double[] result = MathUtils.linspace(0.0, 1.0, 5);
        assertEquals(5, result.length);
        assertEquals(0.0, result[0], EPS);
        assertEquals(1.0, result[4], EPS);
        assertEquals(0.25, result[1], EPS);
    }

    @Test
    void linspaceSinglePointThrows()
    {
        // linspace requires a < b
        assertThrows(IllegalArgumentException.class, () -> MathUtils.linspace(5.0, 5.0, 1));
    }

    @Test
    void diff()
    {
        double[] result = MathUtils.diff(new double[]{1.0, 3.0, 6.0, 10.0});
        assertArrayEquals(new double[]{2.0, 3.0, 4.0}, result, EPS);
    }

    @Test
    void gradient()
    {
        // gradient of linear function should be constant
        double[] data = new double[]{0.0, 1.0, 2.0, 3.0, 4.0};
        double[] g = MathUtils.gradient(data, 1.0);
        assertEquals(5, g.length);
        // central differences for interior points should all be 1.0
        for (int i = 1; i < g.length - 1; i++)
        {
            assertEquals(1.0, g[i], EPS);
        }
    }

    @Test
    void trapezoidArea()
    {
        // trapezoid with base=4, parallel sides 3 and 5: area = 4*(3+5)/2 = 16
        assertEquals(16.0, MathUtils.trapezoidArea(4.0, 3.0, 5.0), EPS);
    }

    @Test
    void trapz()
    {
        // integral of constant function 2.0 from 0 to 3
        double[] x = {0.0, 1.0, 2.0, 3.0};
        double[] y = {2.0, 2.0, 2.0, 2.0};
        assertEquals(6.0, MathUtils.trapz(x, y), EPS);

        // integral of x from 0 to 1 = 0.5
        double[] x2 = {0.0, 0.5, 1.0};
        double[] y2 = {0.0, 0.5, 1.0};
        assertEquals(0.5, MathUtils.trapz(x2, y2), EPS);
    }

    @Test
    void copyDouble()
    {
        double[] orig = {1.0, 2.0, 3.0};
        double[] c = MathUtils.copy(orig);
        assertArrayEquals(orig, c, EPS);
        c[0] = 999.0;
        assertEquals(1.0, orig[0], EPS); // verify independence
    }

    @Test
    void copyInt()
    {
        int[] orig = {1, 2, 3};
        int[] c = MathUtils.copy(orig);
        assertArrayEquals(orig, c);
        c[0] = 999;
        assertEquals(1, orig[0]); // verify independence
    }

    // ===== array arithmetic =====

    @Test
    void plusArray()
    {
        double[] result = MathUtils.plus(new double[]{1.0, 2.0}, new double[]{3.0, 4.0});
        assertArrayEquals(new double[]{4.0, 6.0}, result, EPS);
    }

    @Test
    void minusArray()
    {
        double[] result = MathUtils.minus(new double[]{5.0, 8.0}, new double[]{1.0, 3.0});
        assertArrayEquals(new double[]{4.0, 5.0}, result, EPS);
    }

    @Test
    void timesArray()
    {
        double[] result = MathUtils.times(new double[]{2.0, 3.0}, new double[]{4.0, 5.0});
        assertArrayEquals(new double[]{8.0, 15.0}, result, EPS);
    }

    @Test
    void timesScalar()
    {
        double[] result = MathUtils.times(new double[]{1.0, 2.0, 3.0}, 2.0);
        assertArrayEquals(new double[]{2.0, 4.0, 6.0}, result, EPS);
    }

    @Test
    void plusScalar()
    {
        double[] result = MathUtils.plus(new double[]{1.0, 2.0}, 10.0);
        assertArrayEquals(new double[]{11.0, 12.0}, result, EPS);
    }

    @Test
    void sqrtArray()
    {
        double[] result = MathUtils.sqrt(new double[]{4.0, 9.0, 16.0});
        assertArrayEquals(new double[]{2.0, 3.0, 4.0}, result, EPS);
    }

    @Test
    void powArray()
    {
        double[] result = MathUtils.pow(new double[]{2.0, 3.0}, 2.0);
        assertArrayEquals(new double[]{4.0, 9.0}, result, EPS);
    }

    // ===== min/max =====

    @Test
    void minArray()
    {
        assertEquals(1.0, MathUtils.min(new double[]{3.0, 1.0, 5.0, 2.0}), EPS);
    }

    @Test
    void maxArray()
    {
        assertEquals(5.0, MathUtils.max(new double[]{3.0, 1.0, 5.0, 2.0}), EPS);
    }

    @Test
    void minidx()
    {
        assertEquals(1, MathUtils.minidx(new double[]{3.0, 1.0, 5.0, 2.0}));
    }

    @Test
    void maxidx()
    {
        assertEquals(2, MathUtils.maxidx(new double[]{3.0, 1.0, 5.0, 2.0}));
    }

    @Test
    void minThreeArgs()
    {
        assertEquals(1, MathUtils.min(3, 1, 2));
        assertEquals(1.0, MathUtils.min(3.0, 1.0, 2.0), EPS);
    }

    @Test
    void maxThreeArgs()
    {
        assertEquals(3, MathUtils.max(3, 1, 2));
        assertEquals(3.0, MathUtils.max(3.0, 1.0, 2.0), EPS);
    }

    // ===== boolean array operations =====

    @Test
    void threshold()
    {
        boolean[] result = MathUtils.threshold(new double[]{1.0, 5.0, 3.0, 7.0}, 4.0);
        assertFalse(result[0]);
        assertTrue(result[1]);
        assertFalse(result[2]);
        assertTrue(result[3]);
    }

    @Test
    void countTrue()
    {
        assertEquals(2, MathUtils.counttrue(new boolean[]{true, false, true, false}));
        assertEquals(0, MathUtils.counttrue(new boolean[]{false, false}));
    }

    @Test
    void countFalse()
    {
        assertEquals(2, MathUtils.countfalse(new boolean[]{true, false, true, false}));
    }

    @Test
    void flip()
    {
        boolean[] result = MathUtils.flip(new boolean[]{true, false, true});
        assertFalse(result[0]);
        assertTrue(result[1]);
        assertFalse(result[2]);
    }

    @Test
    void and()
    {
        boolean[] result = MathUtils.and(
            new boolean[]{true, true, false, false},
            new boolean[]{true, false, true, false}
        );
        assertTrue(result[0]);
        assertFalse(result[1]);
        assertFalse(result[2]);
        assertFalse(result[3]);
    }

    @Test
    void or()
    {
        boolean[] result = MathUtils.or(
            new boolean[]{true, true, false, false},
            new boolean[]{true, false, true, false}
        );
        assertTrue(result[0]);
        assertTrue(result[1]);
        assertTrue(result[2]);
        assertFalse(result[3]);
    }

    // ===== int array operations =====

    @Test
    void plusIntArray()
    {
        int[] result = MathUtils.plus(new int[]{1, 2, 3}, 10);
        assertArrayEquals(new int[]{11, 12, 13}, result);
    }

    // ===== permutation =====

    @Test
    void permutationDouble()
    {
        // permutation returns indices that sort the array ascending
        int[] perm = MathUtils.permutation(new double[]{30.0, 10.0, 20.0});
        assertEquals(1, perm[0]); // 10.0 is smallest
        assertEquals(2, perm[1]); // 20.0 is middle
        assertEquals(0, perm[2]); // 30.0 is largest
    }

    @Test
    void permutationInt()
    {
        int[] perm = MathUtils.permutation(new int[]{30, 10, 20});
        assertEquals(1, perm[0]);
        assertEquals(2, perm[1]);
        assertEquals(0, perm[2]);
    }

    @Test
    void inversePerm()
    {
        int[] perm = {2, 0, 1};
        int[] inv = MathUtils.inverse(perm);
        // inv[perm[i]] == i for all i
        for (int i = 0; i < perm.length; i++)
        {
            assertEquals(i, inv[perm[i]]);
        }
    }

    // ===== distance =====

    @Test
    void distance()
    {
        assertEquals(5.0, MathUtils.distance(new double[]{0, 0}, new double[]{3, 4}), EPS);
        assertEquals(0.0, MathUtils.distance(new double[]{1, 2}, new double[]{1, 2}), EPS);
    }

    // ===== constant/polyWeights =====

    @Test
    void constantWeights()
    {
        double[] w = MathUtils.constantWeights(3);
        assertEquals(3, w.length);
        for (double v : w)
        {
            assertEquals(1.0 / 3.0, v, EPS);
        }
    }

    // ===== 2D array operations =====

    @Test
    void sum2D()
    {
        double[][] m = {{1.0, 2.0}, {3.0, 4.0}};
        assertEquals(10.0, MathUtils.sum(m), EPS);
    }

    @Test
    void copy2D()
    {
        double[][] orig = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] c = MathUtils.copy(orig);
        assertEquals(orig[0][0], c[0][0], EPS);
        c[0][0] = 999.0;
        assertEquals(1.0, orig[0][0], EPS);
    }

    @Test
    void frobeniusNorm()
    {
        double[][] data = {{1.0, 2.0}, {3.0, 4.0}};
        // frobenius = sqrt(1+4+9+16) = sqrt(30)
        assertEquals(Math.sqrt(30.0), MathUtils.frobeniusNorm(data), EPS);
    }

    @Test
    void transpose()
    {
        double[][] m = {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
        double[][] t = MathUtils.transpose(m);
        assertEquals(3, t.length);
        assertEquals(2, t[0].length);
        assertEquals(1.0, t[0][0], EPS);
        assertEquals(4.0, t[0][1], EPS);
        assertEquals(2.0, t[1][0], EPS);
        assertEquals(5.0, t[1][1], EPS);
    }
}
