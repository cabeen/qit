package qit.math.structs;

import org.junit.jupiter.api.Test;
import qit.data.datasets.Vect;

import static org.junit.jupiter.api.Assertions.*;

class TriangleTest
{
    private static final double EPS = 1e-6;

    private Triangle rightTriangleXY()
    {
        Vect a = new Vect(new double[]{0, 0, 0});
        Vect b = new Vect(new double[]{1, 0, 0});
        Vect c = new Vect(new double[]{0, 1, 0});
        return new Triangle(a, b, c);
    }

    // ===== construction and accessors =====

    @Test
    void vertices()
    {
        Triangle t = rightTriangleXY();
        assertEquals(0.0, t.getA().get(0), EPS);
        assertEquals(1.0, t.getB().get(0), EPS);
        assertEquals(1.0, t.getC().get(1), EPS);
    }

    @Test
    void verticesCopyIndependence()
    {
        Triangle t = rightTriangleXY();
        Vect a = t.getA();
        a.set(0, 999.0);
        assertEquals(0.0, t.getA().get(0), EPS);
    }

    // ===== area =====

    @Test
    void areaRightTriangle()
    {
        // right triangle with legs 1,1: area = 0.5
        Triangle t = rightTriangleXY();
        assertEquals(0.5, t.area(), EPS);
    }

    @Test
    void areaEquilateral()
    {
        // equilateral triangle with side 2: area = sqrt(3)
        Vect a = new Vect(new double[]{0, 0, 0});
        Vect b = new Vect(new double[]{2, 0, 0});
        Vect c = new Vect(new double[]{1, Math.sqrt(3), 0});
        Triangle t = new Triangle(a, b, c);
        assertEquals(Math.sqrt(3), t.area(), EPS);
    }

    @Test
    void areaDegenerateTriangle()
    {
        // collinear points => area 0
        Vect a = new Vect(new double[]{0, 0, 0});
        Vect b = new Vect(new double[]{1, 0, 0});
        Vect c = new Vect(new double[]{2, 0, 0});
        Triangle t = new Triangle(a, b, c);
        assertEquals(0.0, t.area(), EPS);
    }

    // ===== center =====

    @Test
    void center()
    {
        Triangle t = rightTriangleXY();
        Vect center = t.center();
        assertEquals(1.0 / 3.0, center.get(0), EPS);
        assertEquals(1.0 / 3.0, center.get(1), EPS);
        assertEquals(0.0, center.get(2), EPS);
    }

    // ===== plane / normal =====

    @Test
    void planeNormal()
    {
        // right triangle in XY plane should have normal along Z
        Triangle t = rightTriangleXY();
        Plane p = t.plane();
        // plane equation: ax + by + cz + d = 0
        // for XY plane: z = 0, so normal is (0,0,1) or (0,0,-1)
        // a should be ~0, b should be ~0
        // we verify all three vertices lie on the plane
        Vect va = t.getA();
        double eval = p.getA() * va.get(0) + p.getB() * va.get(1) + p.getC() * va.get(2) + p.getD();
        assertEquals(0.0, eval, EPS);
    }

    // ===== closest point / distance =====

    @Test
    void distAtVertex()
    {
        Triangle t = rightTriangleXY();
        Vect p = new Vect(new double[]{0, 0, 0});
        assertEquals(0.0, t.dist(p), EPS);
    }

    @Test
    void distAboveTriangle()
    {
        // point directly above centroid at height 1
        Triangle t = rightTriangleXY();
        Vect p = new Vect(new double[]{0.2, 0.2, 1.0});
        assertEquals(1.0, t.dist(p), EPS);
    }

    @Test
    void closestPointOnVertex()
    {
        Triangle t = rightTriangleXY();
        Vect p = new Vect(new double[]{-1, -1, 0});
        Bary bary = t.closest(p);
        // closest vertex should be A (origin)
        assertEquals(1.0, bary.getU(), EPS);
        assertEquals(0.0, bary.getV(), EPS);
        assertEquals(0.0, bary.getW(), EPS);
    }

    @Test
    void closestPointInside()
    {
        Triangle t = rightTriangleXY();
        Vect p = new Vect(new double[]{0.2, 0.2, 0.0});
        Bary bary = t.closest(p);
        // point is inside triangle, barycentric coords should sum to 1
        assertEquals(1.0, bary.getU() + bary.getV() + bary.getW(), EPS);
        assertTrue(bary.getU() > 0);
        assertTrue(bary.getV() > 0);
        assertTrue(bary.getW() > 0);
    }

    @Test
    void vectFromBary()
    {
        Triangle t = rightTriangleXY();
        // barycentric (1,0,0) should give vertex A
        Bary baryA = new Bary(1, 0, 0);
        Vect va = t.vect(baryA);
        assertEquals(0.0, va.get(0), EPS);
        assertEquals(0.0, va.get(1), EPS);

        // barycentric (0,1,0) should give vertex B
        Bary baryB = new Bary(0, 1, 0);
        Vect vb = t.vect(baryB);
        assertEquals(1.0, vb.get(0), EPS);
        assertEquals(0.0, vb.get(1), EPS);

        // centroid
        Bary baryCentroid = new Bary(1.0 / 3, 1.0 / 3, 1.0 / 3);
        Vect vc = t.vect(baryCentroid);
        assertEquals(1.0 / 3.0, vc.get(0), EPS);
        assertEquals(1.0 / 3.0, vc.get(1), EPS);
    }

    // ===== copy =====

    @Test
    void copy()
    {
        Triangle t = rightTriangleXY();
        Triangle c = t.copy();
        assertEquals(t.getA().get(0), c.getA().get(0), EPS);
        assertEquals(t.getB().get(0), c.getB().get(0), EPS);
    }
}
