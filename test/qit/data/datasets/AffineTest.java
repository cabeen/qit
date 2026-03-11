package qit.data.datasets;

import org.junit.jupiter.api.Test;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.math.structs.Quaternion;

import static org.junit.jupiter.api.Assertions.*;

class AffineTest
{
    // ===== construction =====

    @Test
    void constructIdentity()
    {
        Affine a = new Affine(3);
        Vect v = VectSource.create3D(1.0, 2.0, 3.0);
        Vect result = a.apply(v);
        assertEquals(1.0, result.get(0), 1e-10);
        assertEquals(2.0, result.get(1), 1e-10);
        assertEquals(3.0, result.get(2), 1e-10);
    }

    @Test
    void constructFromMatrix()
    {
        Matrix m = new Matrix(4, 4);
        m.set(0, 0, 1); m.set(1, 1, 1); m.set(2, 2, 1); m.set(3, 3, 1);
        m.set(0, 3, 10); // translate x by 10
        Affine a = new Affine(m);

        Vect v = VectSource.create3D(0, 0, 0);
        Vect result = a.apply(v);
        assertEquals(10.0, result.get(0), 1e-10);
        assertEquals(0.0, result.get(1), 1e-10);
        assertEquals(0.0, result.get(2), 1e-10);
    }

    @Test
    void constructFromRotationAndTranslation()
    {
        Matrix R = MatrixSource.identity(3);
        Vect T = VectSource.create3D(5.0, 10.0, 15.0);
        Affine a = new Affine(R, T);

        Vect v = VectSource.create3D(0, 0, 0);
        Vect result = a.apply(v);
        assertEquals(5.0, result.get(0), 1e-10);
        assertEquals(10.0, result.get(1), 1e-10);
        assertEquals(15.0, result.get(2), 1e-10);
    }

    @Test
    void staticId()
    {
        Affine a = Affine.id(3);
        Vect v = VectSource.create3D(7, 8, 9);
        Vect result = a.apply(v);
        assertEquals(7.0, result.get(0), 1e-10);
        assertEquals(8.0, result.get(1), 1e-10);
        assertEquals(9.0, result.get(2), 1e-10);
    }

    // ===== apply =====

    @Test
    void applyTranslation()
    {
        Matrix R = MatrixSource.identity(3);
        Vect T = VectSource.create3D(1, 2, 3);
        Affine a = new Affine(R, T);

        Vect v = VectSource.create3D(10, 20, 30);
        Vect result = a.apply(v);
        assertEquals(11.0, result.get(0), 1e-10);
        assertEquals(22.0, result.get(1), 1e-10);
        assertEquals(33.0, result.get(2), 1e-10);
    }

    @Test
    void applyScaling()
    {
        Matrix R = new Matrix(3, 3);
        R.set(0, 0, 2); R.set(1, 1, 3); R.set(2, 2, 4);
        Vect T = VectSource.create3D(0, 0, 0);
        Affine a = new Affine(R, T);

        Vect v = VectSource.create3D(1, 1, 1);
        Vect result = a.apply(v);
        assertEquals(2.0, result.get(0), 1e-10);
        assertEquals(3.0, result.get(1), 1e-10);
        assertEquals(4.0, result.get(2), 1e-10);
    }

    // ===== inverse =====

    @Test
    void inverseOfIdentity()
    {
        Affine a = Affine.id(3);
        Affine inv = a.inv();
        Vect v = VectSource.create3D(1, 2, 3);
        Vect result = inv.apply(v);
        assertEquals(1.0, result.get(0), 1e-10);
        assertEquals(2.0, result.get(1), 1e-10);
        assertEquals(3.0, result.get(2), 1e-10);
    }

    @Test
    void inverseRoundtrip()
    {
        Matrix R = MatrixSource.identity(3);
        Vect T = VectSource.create3D(5, 10, 15);
        Affine a = new Affine(R, T);
        Affine inv = a.inv();

        Vect v = VectSource.create3D(1, 2, 3);
        Vect forward = a.apply(v);
        Vect back = inv.apply(forward);
        assertEquals(1.0, back.get(0), 1e-8);
        assertEquals(2.0, back.get(1), 1e-8);
        assertEquals(3.0, back.get(2), 1e-8);
    }

    // ===== compose =====

    @Test
    void composeTranslations()
    {
        Matrix I = MatrixSource.identity(3);
        Affine a = new Affine(I, VectSource.create3D(1, 0, 0));
        Affine b = new Affine(I, VectSource.create3D(0, 2, 0));
        Affine c = a.compose(b);

        Vect v = VectSource.create3D(0, 0, 0);
        Vect result = c.apply(v);
        assertEquals(1.0, result.get(0), 1e-10);
        assertEquals(2.0, result.get(1), 1e-10);
        assertEquals(0.0, result.get(2), 1e-10);
    }

    // ===== extract components =====

    @Test
    void extractLinear()
    {
        Matrix R = new Matrix(3, 3);
        R.set(0, 0, 2); R.set(1, 1, 3); R.set(2, 2, 4);
        Vect T = VectSource.create3D(10, 20, 30);
        Affine a = new Affine(R, T);

        Matrix linear = a.linear();
        assertEquals(3, linear.rows());
        assertEquals(3, linear.cols());
        assertEquals(2.0, linear.get(0, 0), 1e-10);
    }

    @Test
    void extractTranslation()
    {
        Matrix R = MatrixSource.identity(3);
        Vect T = VectSource.create3D(10, 20, 30);
        Affine a = new Affine(R, T);

        Vect trans = a.trans();
        assertEquals(10.0, trans.get(0), 1e-10);
        assertEquals(20.0, trans.get(1), 1e-10);
        assertEquals(30.0, trans.get(2), 1e-10);
    }

    @Test
    void mat4()
    {
        Affine a = Affine.id(3);
        Matrix m = a.mat4();
        assertEquals(4, m.rows());
        assertEquals(4, m.cols());
        assertEquals(1.0, m.get(3, 3), 1e-10);
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Matrix R = MatrixSource.identity(3);
        Vect T = VectSource.create3D(5, 10, 15);
        Affine a = new Affine(R, T);
        Affine c = a.copy();

        Vect v = VectSource.create3D(0, 0, 0);
        Vect result = c.apply(v);
        assertEquals(5.0, result.get(0), 1e-10);
    }

    // ===== plus (translation composition) =====

    @Test
    void plusTranslation()
    {
        Affine a = Affine.id(3);
        Affine b = a.plus(VectSource.create3D(5, 10, 15));

        Vect v = VectSource.create3D(0, 0, 0);
        Vect result = b.apply(v);
        assertEquals(5.0, result.get(0), 1e-10);
        assertEquals(10.0, result.get(1), 1e-10);
        assertEquals(15.0, result.get(2), 1e-10);
    }
}
