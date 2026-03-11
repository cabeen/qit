package qit.math.structs;

import org.junit.jupiter.api.Test;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;

import static org.junit.jupiter.api.Assertions.*;

class QuaternionTest
{
    private static final double EPS = 1e-6;

    // ===== construction =====

    @Test
    void constructIdentity()
    {
        // b=c=d=0 => identity rotation (a = sqrt(1-0) = 1)
        Quaternion q = new Quaternion(0, 0, 0);
        assertEquals(1.0, q.getA(), EPS);
        assertEquals(0.0, q.getB(), EPS);
        assertEquals(0.0, q.getC(), EPS);
        assertEquals(0.0, q.getD(), EPS);
    }

    @Test
    void constructFromAxisAngle()
    {
        // 0-degree rotation about any axis => identity
        Vect zAxis = new Vect(new double[]{0, 0, 1});
        Quaternion q = new Quaternion(zAxis, 0.0);
        assertEquals(1.0, q.getA(), EPS);
        assertEquals(0.0, q.getB(), EPS);
        assertEquals(0.0, q.getC(), EPS);
        assertEquals(0.0, q.getD(), EPS);
    }

    @Test
    void constructFromAxisAngle90Z()
    {
        // 90-degree rotation about Z axis
        Vect zAxis = new Vect(new double[]{0, 0, 1});
        Quaternion q = new Quaternion(zAxis, Math.PI / 2);
        assertEquals(Math.cos(Math.PI / 4), q.getA(), EPS);
        assertEquals(0.0, q.getB(), EPS);
        assertEquals(0.0, q.getC(), EPS);
        assertEquals(Math.sin(Math.PI / 4), q.getD(), EPS);
    }

    @Test
    void constructFromAxisAngle180()
    {
        // 180-degree rotation about X axis
        Vect xAxis = new Vect(new double[]{1, 0, 0});
        Quaternion q = new Quaternion(xAxis, Math.PI);
        assertEquals(0.0, q.getA(), EPS);
        assertEquals(1.0, Math.abs(q.getB()), EPS); // could be +/- 1
    }

    // ===== accessors =====

    @Test
    void getADerived()
    {
        // a = sqrt(1 - b^2 - c^2 - d^2) for unit quaternion
        Quaternion q = new Quaternion(0.5, 0.5, 0.0);
        double expected = Math.sqrt(1.0 - 0.25 - 0.25);
        assertEquals(expected, q.getA(), EPS);
    }

    // ===== inverse =====

    @Test
    void inverse()
    {
        Quaternion q = new Quaternion(0.1, 0.2, 0.3);
        Quaternion inv = q.inverse();
        assertEquals(-q.getB(), inv.getB(), EPS);
        assertEquals(-q.getC(), inv.getC(), EPS);
        assertEquals(-q.getD(), inv.getD(), EPS);
    }

    @Test
    void inverseRotationIsIdentity()
    {
        // q * q^-1 applied as rotation should yield identity matrix
        Vect axis = new Vect(new double[]{1, 1, 1});
        Quaternion q = new Quaternion(axis, Math.PI / 3);
        Matrix rot = q.matrix();
        Matrix invRot = q.inverse().matrix();
        Matrix product = rot.times(invRot);

        // should be close to identity
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, product.get(i, j), EPS);
            }
        }
    }

    // ===== matrix conversion =====

    @Test
    void identityMatrix()
    {
        Quaternion id = new Quaternion(0, 0, 0);
        Matrix m = id.matrix();
        assertEquals(3, m.rows());
        assertEquals(3, m.cols());
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, m.get(i, j), EPS);
            }
        }
    }

    @Test
    void rotationMatrixIsOrthogonal()
    {
        // R * R^T = I for any rotation matrix
        Vect axis = new Vect(new double[]{1, 2, 3});
        Quaternion q = new Quaternion(axis, 1.23);
        Matrix r = q.matrix();
        Matrix product = r.times(r.transpose());

        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, product.get(i, j), EPS);
            }
        }
    }

    @Test
    void rotationMatrixDetIsOne()
    {
        Vect axis = new Vect(new double[]{0, 1, 0});
        Quaternion q = new Quaternion(axis, Math.PI / 6);
        Matrix r = q.matrix();
        assertEquals(1.0, r.det(), EPS);
    }

    @Test
    void matrix90ZRotatesXtoY()
    {
        // 90-degree rotation about Z: [1,0,0] -> [0,1,0]
        Vect zAxis = new Vect(new double[]{0, 0, 1});
        Quaternion q = new Quaternion(zAxis, Math.PI / 2);
        Matrix r = q.matrix();

        Vect x = new Vect(new double[]{1, 0, 0});
        Vect result = r.times(x);
        assertEquals(0.0, result.get(0), EPS);
        assertEquals(1.0, result.get(1), EPS);
        assertEquals(0.0, result.get(2), EPS);
    }

    @Test
    void matrix4()
    {
        Quaternion q = new Quaternion(0, 0, 0);
        Matrix m4 = q.matrix4();
        assertEquals(4, m4.rows());
        assertEquals(4, m4.cols());
        assertEquals(1.0, m4.get(3, 3), EPS);
        assertEquals(0.0, m4.get(3, 0), EPS);
    }

    // ===== roundtrip: quaternion -> matrix -> quaternion =====

    @Test
    void matrixToQuaternionRoundtrip()
    {
        Vect axis = new Vect(new double[]{1, 0, 0});
        double angle = Math.PI / 4;
        Quaternion original = new Quaternion(axis, angle);
        Matrix rot = original.matrix();
        Quaternion recovered = new Quaternion(rot);

        // rotation matrices should match
        Matrix rotRecovered = recovered.matrix();
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                assertEquals(rot.get(i, j), rotRecovered.get(i, j), EPS);
            }
        }
    }

    // ===== equality =====

    @Test
    void equalsAndHashCode()
    {
        Quaternion a = new Quaternion(0.1, 0.2, 0.3);
        Quaternion b = new Quaternion(0.1, 0.2, 0.3);
        Quaternion c = new Quaternion(0.4, 0.5, 0.6);
        assertEquals(a, b);
        assertNotEquals(a, c);
        assertEquals(a, a);
        assertNotEquals(a, "not a quaternion");
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Quaternion q = new Quaternion(0.1, 0.2, 0.3);
        Quaternion c = q.copy();
        assertEquals(q, c);
        assertEquals(q.getB(), c.getB(), EPS);
    }
}
