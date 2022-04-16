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

package qit.data.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Global;
import qit.base.structs.Bounded;
import qit.data.datasets.AttrMap;
import qit.data.datasets.Mesh;
import qit.data.datasets.Record;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.mesh.MeshFunction;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Bary;
import qit.math.structs.BihSearch;
import qit.math.structs.Box;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.VectFunction;
import qit.math.structs.HalfEdgePoly;
import qit.math.structs.Triangle;
import qit.math.structs.Vertex;
import qit.math.source.VectFunctionSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/** utilties for processing meshes */
public class MeshUtils
{
    public static void sample(Mesh mesh, Volume vol, String attr)
    {
        VectFunction interp = VolumeUtils.interp(InterpolationType.Trilinear, vol);
        sample(mesh, interp, attr);
    }

    public static void sample(Mesh mesh, Volume vol, String coord, String attr)
    {
        VectFunction interp = VolumeUtils.interp(InterpolationType.Trilinear, vol);
        sample(mesh, interp, coord, attr);
    }

    public static void sample(Mesh mesh, VectFunction func, String attr)
    {
        sample(mesh, func, Mesh.COORD, attr);
    }

    public static void sample(Mesh mesh, VectFunction func, String coord, String attr)
    {
        Vect buff = func.protoOut();

        mesh.vattr.add(attr, buff.proto());
        for (Vertex vert : mesh.vattr)
        {
            Vect pos = mesh.vattr.get(vert, coord);
            Vect value = func.apply(pos);
            mesh.vattr.set(vert, attr, value);
        }
    }

    public static void copy(Mesh mfrom, String from, Mesh mto, String to)
    {
        if (mto.vattr.size() != mfrom.vattr.size())
        {
            throw new RuntimeException("mesh dimensions do not match");
        }

        if (!mfrom.vattr.has(from))
        {
            throw new RuntimeException("mesh does not have attribute: " + from);
        }

        mto.vattr.add(to, mfrom.vattr.proto(from));
        for (Vertex v : mto.graph.verts())
        {
            mto.vattr.set(v, to, mfrom.vattr.get(v, from));
        }
    }

    public static void copy(Mesh mesh, String from, String to)
    {
        if (!mesh.vattr.has(from))
        {
            throw new RuntimeException("mesh does not have attribute: " + from);
        }

        mesh.vattr.copy(from, to);
    }

    public static void rename(Mesh mesh, String from, String to)
    {
        if (!mesh.vattr.has(from))
        {
            throw new RuntimeException("mesh does not have attribute: " + from);
        }

        mesh.vattr.rename(from, to);
    }

    public static void remove(Mesh mesh, String attr)
    {
        mesh.vattr.remove(attr);
    }

    public static void keep(Mesh mesh, List<String> attrs)
    {
        List<String> remove = Lists.newArrayList();
        for (String a : mesh.vattr.attrs())
        {
            if (!attrs.contains(a))
            {
                remove.add(a);
            }
        }

        for (String a : remove)
        {
            mesh.vattr.remove(a);
        }
    }

    public static void setAll(Mesh mesh, String attr, Vect vect)
    {
        mesh.vattr.add(attr, vect.proto());

        for (Vertex vert : mesh.vattr)
        {
            mesh.vattr.set(vert, attr, vect);
        }
    }

    public static void set(Mesh mesh, String attr, Vects vects)
    {
        Global.assume(vects.size() == mesh.vattr.size(), "mesh and vects dimensions do not match");

        mesh.vattr.add(attr, vects.get(0).proto());

        // assume vertices correspond to vectors by sorted index
        List<Vertex> sorted = MeshUtils.sort(mesh);
        for (int i = 0; i < sorted.size(); i++)
        {
            mesh.vattr.set(sorted.get(i), attr, vects.get(i));
        }
    }

    public static Vects get(Mesh mesh, String attr)
    {
        if (!mesh.vattr.has(attr))
        {
            throw new RuntimeException("mesh does not have attribute: " + attr);
        }

        // build values in sorted order
        List<Vertex> sorted = MeshUtils.sort(mesh);
        Vects out = new Vects(sorted.size());
        for (int i = 0; i < sorted.size(); i++)
        {
            out.add(mesh.vattr.get(sorted.get(i), attr));
        }

        return out;
    }

    public static void center(Mesh mesh, String name)
    {
        AttrMap<Vertex> attr = mesh.vattr;
        Vect mean = VectsUtils.mean(mesh.vattr.get(name));

        for (Vertex vert : mesh.graph.verts())
        {
            Vect v = attr.get(vert, name);
            v.plusEquals(-1.0, mean);
            attr.set(vert, name, v);
        }
    }

    public static void normalize(Mesh mesh, String name)
    {
        AttrMap<Vertex> attr = mesh.vattr;

        for (Vertex vert : mesh.graph.verts())
        {
            attr.set(vert, name, attr.get(vert, name).normalize());
        }
    }

    public static void addIndex(Mesh mesh)
    {
        mesh.vattr.add(Mesh.INDEX, VectSource.create1D());
        for (Vertex v : mesh.vattr)
        {
            mesh.vattr.set(v, Mesh.INDEX, VectSource.create1D(v.id()));
        }
    }

    public static void addDistance(Mesh mesh, String from, String to, String attr)
    {
        mesh.vattr.add(attr, VectSource.create1D());
        for (Vertex v : mesh.vattr)
        {
            Vect vfrom = mesh.vattr.get(v, from);
            Vect vto = mesh.vattr.get(v, to);
            double dist = vfrom.dist(vto);
            mesh.vattr.set(v, attr, VectSource.create1D(dist));
        }
    }

    public static void flipFaces(Mesh mesh)
    {
        HalfEdgePoly ngraph = mesh.graph.proto();
        for (Face f : mesh.graph.faces())
        {
            ngraph.add(f.flip());
        }

        mesh.graph = ngraph;
    }

    public static void flipNormals(Mesh mesh)
    {
        Global.assume(mesh.vattr.has(Mesh.NORMAL), "normals not found");

        new MeshFunction(VectFunctionSource.scale(-1, 3)).withMesh(mesh).withSource(Mesh.NORMAL).withDest(Mesh.NORMAL).run();
    }

    public static void computeNormals(Mesh mesh, String attr)
    {
        computeNormals(mesh, attr, Mesh.NORMAL);
    }

    public static void computeNormals(Mesh mesh, String input, String output)
    {
        mesh.vattr.add(output, VectSource.create3D());
        for (Vertex vi : mesh.graph.verts())
        {
            // Compute the initial normal
            Vect n = VectSource.create3D();
            for (Face f : mesh.graph.faceRing(vi))
            {
                Vect pa = mesh.vattr.get(f.getA(), input);
                Vect pb = mesh.vattr.get(f.getB(), input);
                Vect pc = mesh.vattr.get(f.getC(), input);

                Vect ba = pb.minus(pa);
                Vect ca = pc.minus(pa);
                Vect fn = ba.cross(ca).normalize();
                n.plusEquals(fn);
            }
            n.normalizeEquals();

            mesh.vattr.set(vi, output, n);
        }
    }

    public static void computeNormals(Mesh mesh)
    {
        computeNormals(mesh, Mesh.COORD);
    }

    public static Mesh mbind(Mesh mesh, Mesh input)
    {
        if (mesh == null)
        {
            return input;
        }
        else
        {
            mesh.add(input);
            return mesh;
        }
    }

    public static Mesh cat(List<Mesh> meshes)
    {
        Mesh out = meshes.get(0).copy();
        for (int i = 1; i < meshes.size(); i++)
        {
            out.add(meshes.get(i));
        }
        return out;
    }

    public static double area(Mesh mesh)
    {
        double area = 0;
        for (Face face : mesh.graph.faces())
        {
            area += MeshUtils.area(mesh, face);
        }
        return area;
    }

    public static Record measure(Mesh mesh)
    {
        Record out = new Record();

        out.with("num.verts", String.valueOf(mesh.graph.numVertex()));
        out.with("num.edges", String.valueOf(mesh.graph.numEdge()));
        out.with("num.faces", String.valueOf(mesh.graph.numFace()));
        out.with("num.attr", String.valueOf(mesh.vattr.numAttr()));
        out.with("surf.area", String.valueOf(MeshUtils.area(mesh)));
        out.with("euler", String.valueOf(MeshUtils.euler(mesh)));
        out.with("genus", String.valueOf(MeshUtils.genus(mesh)));

        Map<String, VectsOnlineStats> stats = Maps.newLinkedHashMap();

        VectsOnlineStats farea = new VectsOnlineStats(1);
        for (Face face : mesh.graph.faces())
        {
            farea.update(VectSource.create1D(MeshUtils.area(mesh, face)));
        }

        stats.put("face.area", farea);

        VectsOnlineStats elen = new VectsOnlineStats(1);
        for (Edge edge : mesh.graph.edges())
        {
            elen.update(VectSource.create1D(MeshUtils.length(mesh, edge)));
        }

        stats.put("edge.length", elen);
        for (String name : mesh.vattr.attrs())
        {
            if (name.equals(Mesh.COLOR) || name.equals(Mesh.OPACITY))
            {
                continue;
            }

            VectsOnlineStats astats = new VectsOnlineStats(mesh.vattr.dim(name));

            for (Vertex vert : mesh.graph.verts())
            {
                astats.update(mesh.vattr.get(vert, name));
            }

            stats.put(name, astats);
        }

        for (String name : stats.keySet())
        {
            VectsOnlineStats astats = stats.get(name);

            for (int i = 0; i < astats.dim; i++)
            {
                String suff = astats.dim == 1 ? "" : "(" + i + ")";
                out.with(name + ".mean" + suff, String.valueOf(astats.mean.get(i)));
                out.with(name + ".std" + suff, String.valueOf(astats.std.get(i)));
                out.with(name + ".var" + suff, String.valueOf(astats.var.get(i)));
                out.with(name + ".min" + suff, String.valueOf(astats.min.get(i)));
                out.with(name + ".max" + suff, String.valueOf(astats.max.get(i)));
            }
        }

        return out;
    }

    public static int euler(Mesh mesh)
    {
        return mesh.graph.numVertex() - mesh.graph.numEdge() + mesh.graph.numFace();
    }

    public static int genus(Mesh mesh)
    {
        return (2 - euler(mesh)) / 2;
    }

    public static double length(Mesh mesh, Edge edge)
    {
        Vect a = mesh.vattr.get(edge.getA(), Mesh.COORD);
        Vect b = mesh.vattr.get(edge.getB(), Mesh.COORD);

        return a.dist(b);
    }

    public static double area(Mesh mesh, Face f)
    {
        Vect a = mesh.vattr.get(f.getA(), Mesh.COORD);
        Vect b = mesh.vattr.get(f.getB(), Mesh.COORD);
        Vect c = mesh.vattr.get(f.getC(), Mesh.COORD);

        return new Triangle(a, b, c).area();
    }

    public static double dist(Mesh mesh, String attr, Face f, Vect v)
    {
        Vect a = mesh.vattr.get(f.getA(), attr);
        Vect b = mesh.vattr.get(f.getB(), attr);
        Vect c = mesh.vattr.get(f.getC(), attr);

        Triangle tri = new Triangle(a, b, c);
        Bary bary = tri.closest(v);
        double d = v.dist(tri.vect(bary));
        return d;
    }

    public static VectOnlineStats stats(Mesh mesh, String attr)
    {
        if (mesh.vattr.proto(attr).size() != 1)
        {
            throw new RuntimeException("invalid attribute");
        }

        VectOnlineStats stats = new VectOnlineStats();
        for (Vertex vert : mesh.vattr)
        {
            double v = mesh.vattr.get(vert, attr).get(0);
            stats.update(v, true);
        }

        return stats;
    }

    public static VectOnlineStats sizeStats(Mesh mesh, String attr)
    {
        VectOnlineStats stats = new VectOnlineStats();
        for (Face f : mesh.graph.faces())
        {
            Vect a = mesh.vattr.get(f.getA(), attr);
            Vect b = mesh.vattr.get(f.getB(), attr);
            Vect c = mesh.vattr.get(f.getC(), attr);

            Box box = Box.create(a);
            box = box.union(b);
            box = box.union(c);
            for (int i = 0; i < box.dim(); i++)
            {
                stats.update(box.range(i).size());
            }
        }
        return stats;
    }

    public static Box bounds(Mesh mesh, String attr)
    {
        Box box = null;
        for (Vertex vert : mesh.graph.verts())
        {
            Vect v = mesh.vattr.get(vert, attr);
            if (box == null)
            {
                box = Box.create(v);
            }
            else
            {
                box = box.union(v);
            }
        }

        return box;
    }

    public static Vect normal(Mesh mesh, Face f, String attr)
    {
        Vect pa = mesh.vattr.get(f.getA(), attr);
        Vect pb = mesh.vattr.get(f.getB(), attr);
        Vect pc = mesh.vattr.get(f.getC(), attr);

        return normal(pa, pb, pc);
    }

    public static Vect centroid(Mesh mesh, Face f, String attr)
    {
        Vect pa = mesh.vattr.get(f.getA(), attr);
        Vect pb = mesh.vattr.get(f.getB(), attr);
        Vect pc = mesh.vattr.get(f.getC(), attr);

        return pa.plus(pb).plus(pc).times(1.0 / 3.0);
    }

    public static Vect normal(Vect pa, Vect pb, Vect pc)
    {
        return pb.minus(pa).cross(pc.minus(pa)).normalize();
    }

    public static List<Vertex> sort(Mesh mesh)
    {
        List<Vertex> vs = new ArrayList<Vertex>(mesh.vattr.size());
        for (Vertex vert : mesh.vattr)
        {
            vs.add(vert);
        }

        Comparator<Vertex> comp = new Comparator<Vertex>()
        {
            public int compare(Vertex a, Vertex b)
            {
                return a.id() < b.id() ? -1 : a.id() > b.id() ? 1 : 0;
            }
        };

        List<Vertex> sorted = new LinkedList<Vertex>();
        sorted.addAll(vs);
        Collections.sort(sorted, comp);
        return sorted;
    }

    public static boolean edgesContain(Collection<Edge> edges, Vertex vert)
    {
        for (Edge edge : edges)
        {
            if (edge.contains(vert))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean facesContain(Collection<Face> faces, Vertex vert)
    {
        for (Face face : faces)
        {
            if (face.contains(vert))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean edgesContain(Collection<Edge> edges, Vertex a, Vertex b)
    {
        for (Edge edge : edges)
        {
            if (edge.contains(a) || edge.contains(b))
            {
                return true;
            }
        }

        return false;
    }

    public static Edge edge(Collection<Edge> edges, Vertex a, Vertex b)
    {
        for (Edge edge : edges)
        {
            if (edge.contains(a) && edge.contains(b))
            {
                return edge;
            }
        }

        return null;
    }

    public static boolean facesContain(Collection<Face> faces, Vertex a, Vertex b, Vertex c)
    {
        for (Face face : faces)
        {
            if (face.contains(a) && face.contains(b) && face.contains(c))
            {
                return true;
            }
        }

        return false;
    }

    public static Face face(Collection<Face> faces, Vertex a, Vertex b, Vertex c)
    {
        for (Face face : faces)
        {
            if (face.contains(a) && face.contains(b) && face.contains(c))
            {
                return face;
            }
        }

        return null;
    }

    public static Face closestFace(Mesh mesh, String attr, Vect v)
    {
        return closestFace(mesh, attr, mesh.graph.faces(), v);
    }

    public static Face closestFace(Mesh mesh, String attr, Iterable<Face> faces, Vect v)
    {
        if (faces == null)
        {
            faces = mesh.graph.faces();
        }

        Double min_dist = null;
        Face min_face = null;

        for (Face face : faces)
        {
            Vect a = mesh.vattr.get(face.getA(), attr);
            Vect b = mesh.vattr.get(face.getB(), attr);
            Vect c = mesh.vattr.get(face.getC(), attr);

            Triangle tri = new Triangle(a, b, c);
            Bary bary = tri.closest(v);
            Vect coord = tri.vect(bary);
            double dist = coord.dist(v);
            if (min_dist == null || dist < min_dist)
            {
                min_dist = dist;
                min_face = face;
            }
        }

        return min_face;
    }

    public static boolean circular(List<Edge> edges)
    {
        if (edges.size() < 2)
        {
            return false;
        }

        for (int i = 0; i < edges.size(); i++)
        {
            if (!edges.get(i).connected(edges.get((i + 1) % edges.size())))
            {
                return false;
            }
        }

        for (int i = 0; i < edges.size(); i++)
        {
            for (int j = 0; j < edges.size(); j++)
            {
                if (Math.abs(i - j) % (edges.size() - 1) > 1 && edges.get(i).connected(edges.get(j)))
                {
                    return false;
                }
            }
        }

        return true;
    }

    public static BihSearch<Bounded<Face>, Bounded<Vect>> bihFaces(Mesh mesh, String attr)
    {
        Set<Bounded<Face>> fset = new HashSet<>();
        for (Face face : mesh.graph.faces())
        {
            Vect a = mesh.vattr.get(face.getA(), attr);
            Vect b = mesh.vattr.get(face.getB(), attr);
            Vect c = mesh.vattr.get(face.getC(), attr);

            Box box = Box.createUnion(a, b, c).iso();
            Bounded<Face> rec = new Bounded<>(face, box);
            fset.add(rec);
        }
        return new BihSearch<>(fset, 16);
    }

    public static void apply(Mesh mesh, Function<Vect,Vect> f, String input, String output)
    {
        for (Vertex vertex : mesh.vattr)
        {
            Vect in = mesh.vattr.get(vertex, input);
            Vect out = f.apply(in);

            if (!mesh.vattr.has(output))
            {
                mesh.vattr.add(output, out.proto());
            }

            mesh.vattr.set(vertex, output, out);
        }
    }

    public static void applyEquals(Mesh mesh, Function<Vect,Vect> f, String attr)
    {
        for (Vertex vertex : mesh.vattr)
        {
            Vect in = mesh.vattr.get(vertex, attr);
            Vect out = f.apply(in);
            mesh.vattr.set(vertex, attr, out);
        }
    }

    public static void consume(Mesh mesh, Consumer<Vect> consumer, String attr)
    {
        for (Vertex vertex : mesh.vattr)
        {
            consumer.accept(mesh.vattr.get(vertex, attr));
        }
    }

    public static Vects values(Mesh mesh, Function<Vertex,Boolean> check, String attr)
    {
        Vects out = new Vects();

        for (Vertex v : mesh.vattr)
        {
            if (check.apply(v))
            {
                out.add(mesh.vattr.get(v, attr));
            }
        }

        return out;
    }
}
