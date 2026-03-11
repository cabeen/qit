package qit.data.datasets;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MatrixTest
{
    private static final double EPS = 1e-9;

    // ===== construction =====

    @Test
    void constructRowsCols()
    {
        Matrix m = new Matrix(3, 4);
        assertEquals(3, m.rows());
        assertEquals(4, m.cols());
        assertEquals(0.0, m.get(0, 0), EPS);
    }

    @Test
    void constructWithValue()
    {
        Matrix m = new Matrix(2, 3, 5.0);
        assertEquals(2, m.rows());
        assertEquals(3, m.cols());
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                assertEquals(5.0, m.get(i, j), EPS);
            }
        }
    }

    @Test
    void constructFromArray()
    {
        double[][] data = {{1, 2}, {3, 4}};
        Matrix m = new Matrix(data);
        assertEquals(2, m.rows());
        assertEquals(2, m.cols());
        assertEquals(1.0, m.get(0, 0), EPS);
        assertEquals(4.0, m.get(1, 1), EPS);
    }

    // ===== accessors =====

    @Test
    void setAndGet()
    {
        Matrix m = new Matrix(2, 2);
        m.set(0, 0, 1.0);
        m.set(0, 1, 2.0);
        m.set(1, 0, 3.0);
        m.set(1, 1, 4.0);
        assertEquals(1.0, m.get(0, 0), EPS);
        assertEquals(2.0, m.get(0, 1), EPS);
        assertEquals(3.0, m.get(1, 0), EPS);
        assertEquals(4.0, m.get(1, 1), EPS);
    }

    @Test
    void isSquare()
    {
        assertTrue(new Matrix(3, 3).isSquare());
        assertFalse(new Matrix(2, 3).isSquare());
    }

    @Test
    void compatible()
    {
        Matrix a = new Matrix(2, 3);
        Matrix b = new Matrix(2, 3);
        Matrix c = new Matrix(3, 2);
        assertTrue(a.compatible(b));
        assertFalse(a.compatible(c));
    }

    // ===== row/column access =====

    @Test
    void getRowAndColumn()
    {
        Matrix m = new Matrix(new double[][]{{1, 2, 3}, {4, 5, 6}});
        Vect row = m.getRow(0);
        assertEquals(3, row.size());
        assertEquals(1.0, row.get(0), EPS);
        assertEquals(2.0, row.get(1), EPS);
        assertEquals(3.0, row.get(2), EPS);

        Vect col = m.getColumn(1);
        assertEquals(2, col.size());
        assertEquals(2.0, col.get(0), EPS);
        assertEquals(5.0, col.get(1), EPS);
    }

    @Test
    void setRowAndColumn()
    {
        Matrix m = new Matrix(2, 3);
        m.setRow(0, new Vect(new double[]{1, 2, 3}));
        assertEquals(1.0, m.get(0, 0), EPS);
        assertEquals(2.0, m.get(0, 1), EPS);
        assertEquals(3.0, m.get(0, 2), EPS);

        m.setColumn(1, new Vect(new double[]{10, 20}));
        assertEquals(10.0, m.get(0, 1), EPS);
        assertEquals(20.0, m.get(1, 1), EPS);
    }

    // ===== setAll =====

    @Test
    void setAll()
    {
        Matrix m = new Matrix(2, 2);
        m.setAll(7.0);
        assertEquals(7.0, m.get(0, 0), EPS);
        assertEquals(7.0, m.get(1, 1), EPS);
    }

    @Test
    void setAllDiag()
    {
        Matrix m = new Matrix(3, 3);
        m.setAllDiag(1.0);
        assertEquals(1.0, m.get(0, 0), EPS);
        assertEquals(1.0, m.get(1, 1), EPS);
        assertEquals(1.0, m.get(2, 2), EPS);
        assertEquals(0.0, m.get(0, 1), EPS);
    }

    // ===== scalar arithmetic =====

    @Test
    void timesScalar()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix result = m.times(2.0);
        assertEquals(2.0, result.get(0, 0), EPS);
        assertEquals(8.0, result.get(1, 1), EPS);
        // original unchanged
        assertEquals(1.0, m.get(0, 0), EPS);
    }

    @Test
    void timesEqualsScalar()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        m.timesEquals(3.0);
        assertEquals(3.0, m.get(0, 0), EPS);
        assertEquals(12.0, m.get(1, 1), EPS);
    }

    // ===== matrix arithmetic =====

    @Test
    void plusMatrix()
    {
        Matrix a = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix b = new Matrix(new double[][]{{5, 6}, {7, 8}});
        Matrix c = a.plus(b);
        assertEquals(6.0, c.get(0, 0), EPS);
        assertEquals(8.0, c.get(0, 1), EPS);
        assertEquals(10.0, c.get(1, 0), EPS);
        assertEquals(12.0, c.get(1, 1), EPS);
    }

    @Test
    void minusMatrix()
    {
        Matrix a = new Matrix(new double[][]{{5, 6}, {7, 8}});
        Matrix b = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix c = a.minus(b);
        assertEquals(4.0, c.get(0, 0), EPS);
        assertEquals(4.0, c.get(0, 1), EPS);
        assertEquals(4.0, c.get(1, 0), EPS);
        assertEquals(4.0, c.get(1, 1), EPS);
    }

    @Test
    void timesMatrix()
    {
        // [[1,2],[3,4]] * [[5,6],[7,8]] = [[19,22],[43,50]]
        Matrix a = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix b = new Matrix(new double[][]{{5, 6}, {7, 8}});
        Matrix c = a.times(b);
        assertEquals(19.0, c.get(0, 0), EPS);
        assertEquals(22.0, c.get(0, 1), EPS);
        assertEquals(43.0, c.get(1, 0), EPS);
        assertEquals(50.0, c.get(1, 1), EPS);
    }

    @Test
    void timesVect()
    {
        // [[1,2],[3,4]] * [5,6] = [17,39]
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Vect v = new Vect(new double[]{5, 6});
        Vect result = m.times(v);
        assertEquals(2, result.size());
        assertEquals(17.0, result.get(0), EPS);
        assertEquals(39.0, result.get(1), EPS);
    }

    @Test
    void timesElem()
    {
        Matrix a = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix b = new Matrix(new double[][]{{5, 6}, {7, 8}});
        Matrix c = a.timesElem(b);
        assertEquals(5.0, c.get(0, 0), EPS);
        assertEquals(12.0, c.get(0, 1), EPS);
        assertEquals(21.0, c.get(1, 0), EPS);
        assertEquals(32.0, c.get(1, 1), EPS);
    }

    @Test
    void plusScaledMatrix()
    {
        // a + s*b
        Matrix a = new Matrix(new double[][]{{1, 0}, {0, 1}});
        Matrix b = new Matrix(new double[][]{{2, 2}, {2, 2}});
        Matrix c = a.plus(0.5, b);
        assertEquals(2.0, c.get(0, 0), EPS);
        assertEquals(1.0, c.get(0, 1), EPS);
        assertEquals(1.0, c.get(1, 0), EPS);
        assertEquals(2.0, c.get(1, 1), EPS);
    }

    // ===== row/col broadcast =====

    @Test
    void plusRows()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Vect v = new Vect(new double[]{10, 20});
        Matrix result = m.plusRows(v);
        assertEquals(11.0, result.get(0, 0), EPS);
        assertEquals(22.0, result.get(0, 1), EPS);
        assertEquals(13.0, result.get(1, 0), EPS);
        assertEquals(24.0, result.get(1, 1), EPS);
    }

    @Test
    void minusRows()
    {
        Matrix m = new Matrix(new double[][]{{11, 22}, {13, 24}});
        Vect v = new Vect(new double[]{10, 20});
        Matrix result = m.minusRows(v);
        assertEquals(1.0, result.get(0, 0), EPS);
        assertEquals(2.0, result.get(0, 1), EPS);
    }

    @Test
    void plusCols()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Vect v = new Vect(new double[]{10, 20});
        Matrix result = m.plusCols(v);
        assertEquals(11.0, result.get(0, 0), EPS);
        assertEquals(12.0, result.get(0, 1), EPS);
        assertEquals(23.0, result.get(1, 0), EPS);
        assertEquals(24.0, result.get(1, 1), EPS);
    }

    // ===== linear algebra (JAMA) =====

    @Test
    void transpose()
    {
        Matrix m = new Matrix(new double[][]{{1, 2, 3}, {4, 5, 6}});
        Matrix t = m.transpose();
        assertEquals(3, t.rows());
        assertEquals(2, t.cols());
        assertEquals(1.0, t.get(0, 0), EPS);
        assertEquals(4.0, t.get(0, 1), EPS);
        assertEquals(2.0, t.get(1, 0), EPS);
        assertEquals(5.0, t.get(1, 1), EPS);
    }

    @Test
    void transposeDoubleRoundtrip()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix tt = m.transpose().transpose();
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                assertEquals(m.get(i, j), tt.get(i, j), EPS);
            }
        }
    }

    @Test
    void determinant2x2()
    {
        // det([[1,2],[3,4]]) = 1*4 - 2*3 = -2
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        assertEquals(-2.0, m.det(), 1e-6);
    }

    @Test
    void determinant3x3()
    {
        // det([[1,0,0],[0,2,0],[0,0,3]]) = 6
        Matrix m = new Matrix(new double[][]{{1, 0, 0}, {0, 2, 0}, {0, 0, 3}});
        assertEquals(6.0, m.det(), 1e-6);
    }

    @Test
    void determinantIdentity()
    {
        Matrix id = new Matrix(3, 3);
        id.setAllDiag(1.0);
        assertEquals(1.0, id.det(), 1e-6);
    }

    @Test
    void inverse()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix inv = m.inv();

        // m * inv should be identity
        Matrix product = m.times(inv);
        assertEquals(1.0, product.get(0, 0), 1e-6);
        assertEquals(0.0, product.get(0, 1), 1e-6);
        assertEquals(0.0, product.get(1, 0), 1e-6);
        assertEquals(1.0, product.get(1, 1), 1e-6);
    }

    @Test
    void inverseRoundtrip()
    {
        Matrix m = new Matrix(new double[][]{{2, 1}, {5, 3}});
        Matrix roundtrip = m.inv().inv();
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                assertEquals(m.get(i, j), roundtrip.get(i, j), 1e-6);
            }
        }
    }

    @Test
    void trace()
    {
        Matrix m = new Matrix(new double[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        assertEquals(15.0, m.trace(), EPS);
    }

    @Test
    void rank()
    {
        // identity has full rank
        Matrix id = new Matrix(3, 3);
        id.setAllDiag(1.0);
        assertEquals(3, id.rank());

        // zero matrix has rank 0
        assertEquals(0, new Matrix(3, 3).rank());
    }

    @Test
    void diag()
    {
        Matrix m = new Matrix(new double[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        Vect d = m.diag();
        assertEquals(3, d.size());
        assertEquals(1.0, d.get(0), EPS);
        assertEquals(5.0, d.get(1), EPS);
        assertEquals(9.0, d.get(2), EPS);
    }

    // ===== norms =====

    @Test
    void normF()
    {
        // Frobenius norm of [[1,2],[3,4]] = sqrt(1+4+9+16) = sqrt(30)
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        assertEquals(Math.sqrt(30.0), m.normF(), 1e-6);
    }

    @Test
    void sumsq()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        assertEquals(30.0, m.sumsq(), EPS);
    }

    @Test
    void inner()
    {
        Matrix a = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix b = new Matrix(new double[][]{{5, 6}, {7, 8}});
        // inner = 1*5 + 2*6 + 3*7 + 4*8 = 5+12+21+32 = 70
        assertEquals(70.0, a.inner(b), EPS);
    }

    // ===== statistics =====

    @Test
    void mean()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        assertEquals(2.5, m.mean(), EPS);
    }

    @Test
    void meanRow()
    {
        // meanRow returns mean of each column (the "mean row")
        Matrix m = new Matrix(new double[][]{{1, 4}, {3, 6}});
        Vect mr = m.meanRow();
        assertEquals(2, mr.size());
        assertEquals(2.0, mr.get(0), EPS); // mean of column 0: (1+3)/2
        assertEquals(5.0, mr.get(1), EPS); // mean of column 1: (4+6)/2
    }

    @Test
    void meanColumn()
    {
        // meanColumn returns mean of each row
        Matrix m = new Matrix(new double[][]{{1, 3}, {4, 6}});
        Vect mc = m.meanColumn();
        assertEquals(2, mc.size());
        assertEquals(2.0, mc.get(0), EPS); // mean of row 0: (1+3)/2
        assertEquals(5.0, mc.get(1), EPS); // mean of row 1: (4+6)/2
    }

    // ===== structural operations =====

    @Test
    void sub()
    {
        Matrix m = new Matrix(new double[][]{{1, 2, 3}, {4, 5, 6}, {7, 8, 9}});
        Matrix s = m.sub(0, 1, 1, 2);
        assertEquals(2, s.rows());
        assertEquals(2, s.cols());
        assertEquals(2.0, s.get(0, 0), EPS);
        assertEquals(3.0, s.get(0, 1), EPS);
        assertEquals(5.0, s.get(1, 0), EPS);
        assertEquals(6.0, s.get(1, 1), EPS);
    }

    @Test
    void catRows()
    {
        Matrix a = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix b = new Matrix(new double[][]{{5}, {6}});
        Matrix c = a.catRows(b);
        assertEquals(2, c.rows());
        assertEquals(3, c.cols());
        assertEquals(5.0, c.get(0, 2), EPS);
        assertEquals(6.0, c.get(1, 2), EPS);
    }

    @Test
    void catCols()
    {
        Matrix a = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix b = new Matrix(new double[][]{{5, 6}});
        Matrix c = a.catCols(b);
        assertEquals(3, c.rows());
        assertEquals(2, c.cols());
        assertEquals(5.0, c.get(2, 0), EPS);
        assertEquals(6.0, c.get(2, 1), EPS);
    }

    @Test
    void flatten()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Vect f = m.flatten();
        assertEquals(4, f.size());
        assertEquals(1.0, f.get(0), EPS);
        assertEquals(2.0, f.get(1), EPS);
        assertEquals(3.0, f.get(2), EPS);
        assertEquals(4.0, f.get(3), EPS);
    }

    @Test
    void packRow()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Vect p = m.packRow();
        assertEquals(4, p.size());
        // row-major: 1,2,3,4
        assertEquals(1.0, p.get(0), EPS);
        assertEquals(2.0, p.get(1), EPS);
        assertEquals(3.0, p.get(2), EPS);
        assertEquals(4.0, p.get(3), EPS);
    }

    @Test
    void packColumn()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Vect p = m.packColumn();
        assertEquals(4, p.size());
        // column-major: 1,3,2,4
        assertEquals(1.0, p.get(0), EPS);
        assertEquals(3.0, p.get(1), EPS);
        assertEquals(2.0, p.get(2), EPS);
        assertEquals(4.0, p.get(3), EPS);
    }

    // ===== element-wise transforms =====

    @Test
    void expAndLog()
    {
        Matrix m = new Matrix(new double[][]{{0, 1}, {2, 3}});
        Matrix e = m.exp();
        assertEquals(1.0, e.get(0, 0), 1e-6);
        assertEquals(Math.E, e.get(0, 1), 1e-6);

        // log(exp(m)) should round-trip
        Matrix roundtrip = e.log();
        for (int i = 0; i < 2; i++)
        {
            for (int j = 0; j < 2; j++)
            {
                assertEquals(m.get(i, j), roundtrip.get(i, j), 1e-6);
            }
        }
    }

    // ===== predicates =====

    @Test
    void nan()
    {
        Matrix good = new Matrix(new double[][]{{1, 2}, {3, 4}});
        assertFalse(good.nan());

        Matrix bad = new Matrix(new double[][]{{1, Double.NaN}, {3, 4}});
        assertTrue(bad.nan());
    }

    @Test
    void infinite()
    {
        Matrix good = new Matrix(new double[][]{{1, 2}, {3, 4}});
        assertFalse(good.infinite());

        Matrix bad = new Matrix(new double[][]{{1, Double.POSITIVE_INFINITY}, {3, 4}});
        assertTrue(bad.infinite());
    }

    // ===== copy independence =====

    @Test
    void copy()
    {
        Matrix m = new Matrix(new double[][]{{1, 2}, {3, 4}});
        Matrix c = m.copy();
        assertEquals(m.get(0, 0), c.get(0, 0), EPS);
        c.set(0, 0, 999.0);
        assertEquals(1.0, m.get(0, 0), EPS);
    }

    @Test
    void proto()
    {
        Matrix m = new Matrix(3, 4);
        Matrix p = m.proto();
        assertEquals(3, p.rows());
        assertEquals(4, p.cols());
        assertEquals(0.0, p.get(0, 0), EPS);
    }

    // ===== homogeneous =====

    @Test
    void hom()
    {
        Matrix m = new Matrix(new double[][]{{1, 0}, {0, 1}});
        Matrix h = m.hom();
        assertEquals(3, h.rows());
        assertEquals(3, h.cols());
        assertEquals(1.0, h.get(0, 0), EPS);
        assertEquals(1.0, h.get(1, 1), EPS);
        assertEquals(1.0, h.get(2, 2), EPS);
        assertEquals(0.0, h.get(0, 2), EPS);
    }
}
