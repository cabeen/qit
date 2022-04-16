/*******************************************************************************
  *
  * Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
  * All rights reserved.
  *
  * The Software remains the property of Ryan Cabeen ("the Author").
  *
  * The Software is distributed "AS IS" under this Licence solely for
  * non-commercial use in the hope that it will be useful, but in order
  * that the Author as a charitable foundation protects its assets for
  * the benefit of its educational and research purposes, the Author
  * makes clear that no condition is made or to be implied, nor is any
  * warranty given or to be implied, as to the accuracy of the Software,
  * or that it will be suitable for any particular purpose or for use
  * under any specific conditions. Furthermore, the Author disclaims
  * all responsibility for the use which is made of the Software. It
  * further disclaims any liability for the outcomes arising from using
  * the Software.
  *
  * The Licensee agrees to indemnify the Author and hold the
  * Author harmless from and against any and all claims, damages and
  * liabilities asserted by third parties (including claims for
  * negligence) which arise directly or indirectly from the use of the
  * Software or the sale of any products based on the Software.
  *
  * No part of the Software may be reproduced, modified, transmitted or
  * transferred in any form or by any means, electronic or mechanical,
  * without the express permission of the Author. The permission of
  * the Author is not required if the said reproduction, modification,
  * transmission or transference is done without financial return, the
  * conditions of this Licence are imposed upon the receiver of the
  * product, and all original and amended source code is included in any
  * transmitted product. You may be held legally responsible for any
  * copyright infringement that is caused or encouraged by your failure to
  * abide by these terms and conditions.
  *
  * You are not permitted under this Licence to use this Software
  * commercially. Use for which any financial return is received shall be
  * defined as commercial use, and includes (1) integration of all or part
  * of the source code or the Software into a product for sale or license
  * by or on behalf of Licensee to third parties or (2) use of the
  * Software or any derivative of it for research with the final aim of
  * developing software products for sale or license to a third party or
  * (3) use of the Software or any derivative of it for research with the
  * final aim of developing non-software products for sale or license to a
  * third party, or (4) use of the Software to provide any service to an
  * external organisation for which payment is received.
  *
  ******************************************************************************/

package qit.data.source;

import com.google.common.collect.Maps;
import qit.base.Global;
import qit.data.datasets.Affine;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.modules.mesh.MeshSimplify;
import qit.data.modules.mesh.MeshSubdivide;
import qit.data.utils.MeshUtils;
import qit.data.utils.mesh.MeshFunction;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.util.Map;

/** utlities for creating meshes */
public class MeshSource
{
    public final static double MESH_TETRA_M = 0.6083;
    public final static double MESH_TETRA_C = Math.cos(MESH_TETRA_M * Math.PI);
    public final static double MESH_TETRA_S = Math.sin(MESH_TETRA_M * Math.PI);
    public final static double[][] MESH_TETRA_VERTS = {
            {0, 0, 1},
            {MESH_TETRA_S, 0, MESH_TETRA_C},
            {MESH_TETRA_S * Math.cos(2 * Math.PI / 3), MESH_TETRA_S * Math.sin(2 * Math.PI / 3), MESH_TETRA_C},
            {MESH_TETRA_S * Math.cos(4 * Math.PI / 3), MESH_TETRA_S * Math.sin(4 * Math.PI / 3), MESH_TETRA_C}};
    public final static int[][] MESH_TETRA_FACES = {
            {0, 1, 2},
            {0, 3, 1},
            {0, 2, 3},
            {2, 1, 3}};

    public final static double MESH_ICO_PHI = (1 + Math.sqrt(5)) / 2;
    public final static double MESH_ICO_A = 1.0 / 2.0;
    public final static double MESH_ICO_B = 1.0 / (2.0 * MESH_ICO_PHI);
    public final static double[][] MESH_ICO_VERTS = {
            {0, MESH_ICO_B, -MESH_ICO_A},
            {MESH_ICO_B, MESH_ICO_A, 0},
            {-MESH_ICO_B, MESH_ICO_A, 0},
            {0, MESH_ICO_B, MESH_ICO_A},
            {0, -MESH_ICO_B, MESH_ICO_A},
            {-MESH_ICO_A, 0, MESH_ICO_B},
            {0, -MESH_ICO_B, -MESH_ICO_A},
            {MESH_ICO_A, 0, -MESH_ICO_B},
            {MESH_ICO_A, 0, MESH_ICO_B},
            {-MESH_ICO_A, 0, -MESH_ICO_B},
            {MESH_ICO_B, -MESH_ICO_A, 0},
            {-MESH_ICO_B, -MESH_ICO_A, 0}
    };
    public final static int[][] MESH_ICO_FACES = {
            {0, 1, 2},
            {3, 2, 1},
            {3, 4, 5},
            {3, 8, 4},
            {0, 6, 7},
            {0, 9, 6},
            {4, 10, 11},
            {6, 11, 10},
            {2, 5, 9},
            {11, 9, 5},
            {1, 7, 8},
            {10, 8, 7},
            {3, 5, 2},
            {3, 1, 8},
            {0, 2, 9},
            {0, 7, 1},
            {6, 9, 11},
            {6, 10, 7},
            {4, 11, 5},
            {4, 8, 10}
    };

    public final static double[][] MESH_BOX_VERTS = {
            {-1, -1, -1},
            {1, -1, -1},
            {-1, 1, -1},
            {1, 1, -1},
            {-1, -1, 1},
            {1, -1, 1},
            {-1, 1, 1},
            {1, 1, 1}
    };
    public final static int[][] MESH_BOX_FACES = {
            {0, 1, 2},
            {2, 1, 3},
            {4, 6, 5},
            {6, 7, 5},
            {1, 0, 4},
            {2, 3, 6},
            {1, 4, 5},
            {6, 3, 7},
            {0, 2, 4},
            {3, 1, 5},
            {4, 2, 6},
            {3, 5, 7}

    };
    
    private static Mesh BOX_CACHE = null;
    private static Mesh TETRA_CACHE = null;
    private static Mesh ICO_CACHE = null;
    private static Mesh OCTA_CACHE = null;
    private static Map<Integer, Mesh> SPHERE_CACHE = Maps.newHashMap();
    private static Map<Integer, Mesh> HEMISPHERE_CACHE = Maps.newHashMap();
    private static Map<Integer, Mesh> CYLINDER_CACHE = Maps.newHashMap();
    private static Map<Integer, Mesh> CONE_CACHE = Maps.newHashMap();

    public static void clearCache()
    {
        TETRA_CACHE = null;
        ICO_CACHE = null;
        OCTA_CACHE = null;
        SPHERE_CACHE.clear();
        CYLINDER_CACHE.clear();
        HEMISPHERE_CACHE.clear();
    }

    public static Mesh triangle()
    {
        Mesh mesh = new Mesh();
        Vertex va = new Vertex(0);
        Vertex vb = new Vertex(1);
        Vertex vc = new Vertex(2);

        Face face = new Face(va, vb, vc);
        mesh.graph.add(face);

        mesh.vattr.set(va, Mesh.COORD, VectSource.create3D(0, 0, 0));
        mesh.vattr.set(vb, Mesh.COORD, VectSource.create3D(0, 0, 1));
        mesh.vattr.set(vc, Mesh.COORD, VectSource.create3D(0, 1, 0));

        return mesh;
    }

    public static Mesh tetrahedron()
    {
        if (TETRA_CACHE != null)
        {
            return TETRA_CACHE.copy();
        }

        Mesh mesh = new Mesh();

        for (int i = 0; i < MESH_TETRA_VERTS.length; i++)
        {
            double x = MESH_TETRA_VERTS[i][0];
            double y = MESH_TETRA_VERTS[i][1];
            double z = MESH_TETRA_VERTS[i][2];

            Vertex v = new Vertex(i);
            Vect c = VectSource.create3D(x, y, z);
            mesh.graph.add(v);
            mesh.vattr.add(v);
            mesh.vattr.set(v, Mesh.COORD, c);
        }

        for (int[] element : MESH_TETRA_FACES)
        {
            int a = element[0];
            int b = element[1];
            int c = element[2];
            Vertex va = new Vertex(a);
            Vertex vb = new Vertex(b);
            Vertex vc = new Vertex(c);
            Face f = new Face(va, vb, vc);
            mesh.graph.add(f);
        }

        TETRA_CACHE = mesh.copy();

        return mesh;
    }

    public static Mesh box()
    {
        if (BOX_CACHE != null)
        {
            return BOX_CACHE.copy();
        }

        Mesh mesh = new Mesh();

        for (int i = 0; i < MESH_BOX_VERTS.length; i++)
        {
            double x = MESH_BOX_VERTS[i][0];
            double y = MESH_BOX_VERTS[i][1];
            double z = MESH_BOX_VERTS[i][2];

            Vertex v = new Vertex(i);
            Vect c = VectSource.create3D(x, y, z);
            mesh.graph.add(v);
            mesh.vattr.set(v, Mesh.COORD, c);
        }

        for (int[] element : MESH_BOX_FACES)
        {
            // note: change the face orientation...
            int a = element[0];
            int b = element[2];
            int c = element[1];
            Vertex va = new Vertex(a);
            Vertex vb = new Vertex(b);
            Vertex vc = new Vertex(c);
            Face f = new Face(va, vb, vc);
            mesh.graph.add(f);
        }

        BOX_CACHE = mesh.copy();

        return mesh;
    }

    public static Mesh icosahedron()
    {
        if (ICO_CACHE != null)
        {
            return ICO_CACHE.copy();
        }

        Mesh mesh = new Mesh();

        for (int i = 0; i < MESH_ICO_VERTS.length; i++)
        {
            double x = MESH_ICO_VERTS[i][0];
            double y = MESH_ICO_VERTS[i][1];
            double z = MESH_ICO_VERTS[i][2];

            Vertex v = new Vertex(i);
            Vect c = VectSource.create3D(x, y, z);
            mesh.graph.add(v);
            mesh.vattr.set(v, Mesh.COORD, c);
        }

        for (int[] element : MESH_ICO_FACES)
        {
            // note: change the face orientation...
            int a = element[0];
            int b = element[2];
            int c = element[1];
            Vertex va = new Vertex(a);
            Vertex vb = new Vertex(b);
            Vertex vc = new Vertex(c);
            Face f = new Face(va, vb, vc);
            mesh.graph.add(f);
        }

        ICO_CACHE = mesh.copy();

        return mesh;
    }

    public static Mesh cone(int num)
    {
        if (CONE_CACHE.containsKey(num))
        {
            return CONE_CACHE.get(num).copy();
        }

        Global.assume(num >= 3, "invalid number of samples");

        Mesh mesh = new Mesh();

        int count = 0;
        Vertex top = new Vertex(count++);
        Vertex bottom = new Vertex(count++);

        mesh.vattr.set(top, Mesh.COORD, VectSource.create(0, 0, 1));
        mesh.vattr.set(bottom, Mesh.COORD, VectSource.create(0, 0, -1));

        Vertex[] bottomRimTube = new Vertex[num];
        Vertex[] bottomRimCap = new Vertex[num];

        for (int i = 0; i < num; i++)
        {
            bottomRimTube[i] = new Vertex(count++);
            bottomRimCap[i] = new Vertex(count++);

            double x = Math.cos(2 * Math.PI * i / num);
            double y = Math.sin(2 * Math.PI * i / num);

            mesh.vattr.set(bottomRimTube[i], Mesh.COORD, VectSource.create(x, y, -1));
            mesh.vattr.set(bottomRimCap[i], Mesh.COORD, VectSource.create(x, y, -1));
        }

        for (int i = 0; i < num; i++)
        {
            {
                Vertex bottomCurrent = bottomRimTube[i];
                Vertex bottomPrev = i == 0 ? bottomRimTube[num - 1] : bottomRimTube[i - 1];
                mesh.graph.add(new Face(top, bottomCurrent, bottomPrev));
            }
            {
                Vertex bottomCurrent = bottomRimCap[i];
                Vertex bottomPrev = i == 0 ? bottomRimCap[num - 1] : bottomRimCap[i - 1];
                mesh.graph.add(new Face(bottomCurrent, bottom, bottomPrev));
            }
        }

        CONE_CACHE.put(num, mesh.copy());

        return mesh;
    }

    public static Mesh cylinder(int num)
    {
        if (CYLINDER_CACHE.containsKey(num))
        {
            return CYLINDER_CACHE.get(num).copy();
        }

        Global.assume(num >= 3, "invalid number of samples");

        Mesh mesh = new Mesh();

        int count = 0;
        Vertex top = new Vertex(count++);
        Vertex bottom = new Vertex(count++);

        mesh.vattr.set(top, Mesh.COORD, VectSource.create(0, 0, 1));
        mesh.vattr.set(bottom, Mesh.COORD, VectSource.create(0, 0, -1));

        Vertex[] topRimTube = new Vertex[num];
        Vertex[] bottomRimTube = new Vertex[num];
        Vertex[] topRimCap = new Vertex[num];
        Vertex[] bottomRimCap = new Vertex[num];

        for (int i = 0; i < num; i++)
        {
            topRimTube[i] = new Vertex(count++);
            bottomRimTube[i] = new Vertex(count++);
            topRimCap[i] = new Vertex(count++);
            bottomRimCap[i] = new Vertex(count++);

            double x = Math.cos(2 * Math.PI * i / num);
            double y = Math.sin(2 * Math.PI * i / num);

            mesh.vattr.set(topRimTube[i], Mesh.COORD, VectSource.create(x, y, 1));
            mesh.vattr.set(bottomRimTube[i], Mesh.COORD, VectSource.create(x, y, -1));
            mesh.vattr.set(topRimCap[i], Mesh.COORD, VectSource.create(x, y, 1));
            mesh.vattr.set(bottomRimCap[i], Mesh.COORD, VectSource.create(x, y, -1));
        }

        for (int i = 0; i < num; i++)
        {
            {
                Vertex topCurrent = topRimTube[i];
                Vertex topPrev = i == 0 ? topRimTube[num - 1] : topRimTube[i - 1];
                Vertex bottomCurrent = bottomRimTube[i];
                Vertex bottomPrev = i == 0 ? bottomRimTube[num - 1] : bottomRimTube[i - 1];

                mesh.graph.add(new Face(topCurrent, bottomPrev, topPrev));
                mesh.graph.add(new Face(topCurrent, bottomCurrent, bottomPrev));
            }

            {
                Vertex topCurrent = topRimCap[i];
                Vertex topPrev = i == 0 ? topRimCap[num - 1] : topRimCap[i - 1];
                Vertex bottomCurrent = bottomRimCap[i];
                Vertex bottomPrev = i == 0 ? bottomRimCap[num - 1] : bottomRimCap[i - 1];

                mesh.graph.add(new Face(topCurrent, topPrev, top));
                mesh.graph.add(new Face(bottomCurrent, bottom, bottomPrev));
            }
        }

        CYLINDER_CACHE.put(num, mesh.copy());

        return mesh;
    }

    public static Mesh octohedron()
    {
        if (OCTA_CACHE != null)
        {
            return OCTA_CACHE.copy();
        }

        int count = 0;
        Vertex up = new Vertex(count++);
        Vertex north = new Vertex(count++);
        Vertex south = new Vertex(count++);
        Vertex east = new Vertex(count++);
        Vertex west = new Vertex(count++);
        Vertex down = new Vertex(count++);

        Mesh mesh = new Mesh();
        mesh.vattr.set(up, Mesh.COORD, VectSource.create(0, 0, 1));
        mesh.vattr.set(east, Mesh.COORD, VectSource.create(-1, 0, 0));
        mesh.vattr.set(west, Mesh.COORD, VectSource.create(1, 0, 0));
        mesh.vattr.set(north, Mesh.COORD, VectSource.create(0, 1, 0));
        mesh.vattr.set(south, Mesh.COORD, VectSource.create(0, -1, 0));
        mesh.vattr.set(down, Mesh.COORD, VectSource.create(0, 0, -1));

        mesh.graph.add(new Face(south, east, down));
        mesh.graph.add(new Face(east, north, down));
        mesh.graph.add(new Face(north, west, down));
        mesh.graph.add(new Face(west, south, down));

        mesh.graph.add(new Face(west, up, south));
        mesh.graph.add(new Face(south, up, east));
        mesh.graph.add(new Face(east, up, north));
        mesh.graph.add(new Face(north, up, west));

        OCTA_CACHE = mesh.copy();

        return mesh;
    }

    public static Mesh hemisphere(int num)
    {
        if (HEMISPHERE_CACHE.containsKey(num))
        {
            return HEMISPHERE_CACHE.get(num).copy();
        }

        int count = 0;
        Vertex up = new Vertex(count++);
        Vertex north = new Vertex(count++);
        Vertex south = new Vertex(count++);
        Vertex east = new Vertex(count++);
        Vertex west = new Vertex(count++);

        Mesh mesh = new Mesh();
        mesh.vattr.set(up, Mesh.COORD, VectSource.create(0, 0, 1));
        mesh.vattr.set(east, Mesh.COORD, VectSource.create(-1, 0, 0));
        mesh.vattr.set(west, Mesh.COORD, VectSource.create(1, 0, 0));
        mesh.vattr.set(north, Mesh.COORD, VectSource.create(0, 1, 0));
        mesh.vattr.set(south, Mesh.COORD, VectSource.create(0, -1, 0));

        mesh.graph.add(new Face(west, up, south));
        mesh.graph.add(new Face(south, up, east));
        mesh.graph.add(new Face(east, up, north));
        mesh.graph.add(new Face(north, up, west));

        MeshSubdivide subdiv = new MeshSubdivide();
        subdiv.input = mesh;
        subdiv.inplace = true;
        subdiv.num = num;
        subdiv.run();

        MeshUtils.normalize(mesh, Mesh.COORD);

        HEMISPHERE_CACHE.put(num, mesh.copy());

        return mesh;
    }

    public static Mesh spherePoints(int num)
    {
        Mesh mesh = icosahedron();

        while (mesh.graph.numVertex() < num)
        {
            MeshSubdivide subdiv = new MeshSubdivide();
            subdiv.input = mesh;
            subdiv.inplace = true;
            subdiv.num = 1;
            subdiv.run();
        }

        MeshSimplify simplify = new MeshSimplify();
        simplify.input = mesh ;
        simplify.maxvert = num;
        simplify.inplace = true;
        simplify.run();

        return mesh;
    }

    public static Mesh sphere(int num)
    {
        if (SPHERE_CACHE.containsKey(num))
        {
            return SPHERE_CACHE.get(num).copy();
        }

        Mesh mesh = icosahedron();

        MeshSubdivide subdiv = new MeshSubdivide();
        subdiv.input = mesh;
        subdiv.inplace = true;
        subdiv.num = num;
        subdiv.run();

        MeshUtils.normalize(mesh, Mesh.COORD);

        SPHERE_CACHE.put(num, mesh.copy());

        return mesh;
    }

    public static Mesh cylinder(int res, double thickness, Vect start, Vect end)
    {
        Mesh out = cylinder(res);

        double length = start.dist(end);
        new MeshFunction(new Affine(3).times(VectSource.create3D(thickness, thickness, length / 2.0))).withMesh(out).run();
        Vect delta = end.minus(start);

        Vect v1 = delta.normalize();
        Vect v2 = v1.perp();
        Vect v3 = v1.cross(v2);

        double[][] R = new double[3][3];
        R[0][0] = v3.get(0);
        R[1][0] = v3.get(1);
        R[2][0] = v3.get(2);
        R[0][1] = v2.get(0);
        R[1][1] = v2.get(1);
        R[2][1] = v2.get(2);
        R[0][2] = v1.get(0);
        R[1][2] = v1.get(1);
        R[2][2] = v1.get(2);

        new MeshFunction(new Affine(3).rotate(R)).withMesh(out).run();
        new MeshFunction(new Affine(3).plus(start.times(0.5).plus(0.5, end))).withMesh(out).run();

        return out;
    }
}