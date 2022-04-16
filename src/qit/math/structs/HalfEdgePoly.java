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

package qit.math.structs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HalfEdgePoly
{
    // It is important that these are linked hash maps, so they preserve vertex order
    private Map<Vertex, VertexHE> verts = Maps.newLinkedHashMap();
    private Map<Edge, EdgeHE> edges = Maps.newLinkedHashMap();
    private Map<Face, FaceHE> faces = Maps.newLinkedHashMap();
    private int max_vidx = 0;

    /*********************
     * HALF EDGE STRUCTS *
     *********************/

    private static class EdgeHE
    {
        private HalfEdge hedge;
        private Edge edge;

        public String toString()
        {
            return String.format("[EdgeHE he: %d]", this.hedge.id);
        }
    }

    private static class VertexHE
    {
        private Vertex vert;
        private HalfEdge hedge; // If null, the vertex is isolated

        public String toString()
        {
            return String.format("[VertexexHE %d, he: %d]", this.vert.id(), this.hedge.id);
        }
    }

    private static class FaceHE
    {
        private HalfEdge hedge;
        private Face face;

        public String toString()
        {
            return String.format("[FaceHE he: %d]", this.hedge.id);
        }
    }

    private static class HalfEdge
    {
        private static int ID = 0;
        private int id = ID++;
        private VertexHE vert;
        private FaceHE face; // If null, the half edge is boundary
        private EdgeHE edge;
        private HalfEdge prev;
        private HalfEdge next;
        private HalfEdge pair;

        public String toString()
        {
            return String.format("[HalfEdge %d, fv: %d, tv: %d, next: %d, mousePrev: %d, pair: %d: face: %s]", this.id,
                    this.vert.vert.id(), this.pair.vert.vert.id(), this.next.id, this.prev.id, this.pair.id, this.face == null ? "none" : "exists");
        }
    }

    /*************************************
     * PRIVATE DATA STRUCTURE OPERATIONS *
     *************************************/

    private Set<HalfEdge> bounds()
    {
        Set<HalfEdge> bounds = Sets.newHashSet();
        for (EdgeHE edge : this.edges.values())
        {
            if (edge.hedge.face == null)
            {
                bounds.add(edge.hedge);
            }
            else if (edge.hedge.pair.face == null)
            {
                bounds.add(edge.hedge.pair);
            }
        }

        return bounds;
    }

    private List<HalfEdge> findOut(VertexHE v)
    {
        Global.assume(v.hedge != null, "Isolated vertex encountered");

        List<HalfEdge> hedges = Lists.newArrayList();
        HalfEdge start = v.hedge;
        HalfEdge he = start;
        do
        {
            hedges.add(he);
            he = he.pair.next;
        }
        while (he != start);

        return hedges;
    }

    private boolean connected(VertexHE a, VertexHE b)
    {
        if (a.hedge == null || b.hedge == null)
        {
            return false;
        }

        for (HalfEdge he : this.findOut(a))
        {
            if (he.pair.vert == b)
            {
                return true;
            }
        }

        return false;
    }

    private HalfEdge hedge(VertexHE a, VertexHE b)
    {
        if (a.hedge == null || b.hedge == null)
        {
            return null;
        }

        HalfEdge start = a.hedge;
        HalfEdge he = start;
        do
        {
            if (he.pair.vert == b)
            {
                return he;
            }

            he = he.pair.next;
        }
        while (he != start);

        return null;
    }

    private HalfEdge findFreeIn(VertexHE v)
    {
        if (v.hedge != null)
        {
            HalfEdge start = v.hedge.pair;
            HalfEdge he = start;
            do
            {
                if (he.face == null)
                {
                    return he;
                }
                he = he.next.pair;
            }
            while (he != start);
        }

        throw new RuntimeException("No free incident half edge exists");
    }

    private HalfEdge findFreeIn(HalfEdge start, HalfEdge end)
    {
        if (start == end)
        {
            throw new RuntimeException("No free incident half edge exists");
        }

        HalfEdge he = start;
        do
        {
            if (he.face == null)
            {
                return he;
            }
            he = he.next.pair;

        }
        while (he != end);

        throw new RuntimeException("No free incident half edge exists");
    }

    private void makeAdj(HalfEdge in, HalfEdge out)
    {
        // Check if adjacency is already satisfied
        if (in.next == out)
        {
            return;
        }

        Global.assume(in.pair.vert == out.vert, "The given half edges cannot be adjacent");

        HalfEdge x = out.prev;
        HalfEdge y = in.next;
        HalfEdge xx = this.findFreeIn(out.pair, in);
        HalfEdge yy = xx.next;

        in.next = out;
        out.prev = in;

        xx.next = y;
        y.prev = xx;

        x.next = yy;
        yy.prev = x;
    }

    private boolean faceExists(HalfEdge[] loop)
    {
        for (HalfEdge he : loop)
        {
            if (he.face == null)
            {
                return false;
            }
        }

        return true;
    }

    private boolean canMakeFace(HalfEdge[] loop)
    {
        for (int i = 0; i < loop.length; i++)
        {
            if (loop[i].pair.vert != loop[(i + 1) % loop.length].vert)
            {
                return false;
            }
            else if (loop[i].face != null)
            {
                return false;
            }
        }

        return true;
    }

    private FaceHE makeFace(HalfEdge[] loop)
    {
        // Connect the adjacent half edges
        for (int i = 0; i < loop.length; i++)
        {
            this.makeAdj(loop[i], loop[(i + 1) % loop.length]);
        }

        // Create the face structure and set references
        FaceHE face = new FaceHE();
        face.hedge = loop[0];
        for (HalfEdge he : loop)
        {
            he.face = face;
        }

        return face;
    }

    /*************************
     * DIRECT ACCESS METHODS *
     *************************/

    public int numEdge()
    {
        return this.edges.size();
    }

    public int numFace()
    {
        return this.faces.size();
    }

    public int numVertex()
    {
        return this.verts.size();
    }

    public Iterable<Vertex> verts()
    {
        return this.verts.keySet();
    }

    public Iterable<Edge> edges()
    {
        return this.edges.keySet();
    }

    public Iterable<Face> faces()
    {
        return this.faces.keySet();
    }

    public boolean has(Vertex v)
    {
        return this.verts.containsKey(v);
    }

    public boolean has(Edge e)
    {
        return this.edges.containsKey(e);
    }

    public boolean has(Face f)
    {
        return this.faces.containsKey(f);
    }

    public Iterator<Vertex> iterator()
    {
        return this.verts().iterator();
    }

    public Vertex addVertex()
    {
        this.max_vidx++;
        return new Vertex(this.max_vidx);
    }

    public HalfEdgePoly proto()
    {
        return new HalfEdgePoly();
    }

    public HalfEdgePoly copy()
    {
        HalfEdgePoly mesh = new HalfEdgePoly();

        for (Vertex vert : this.verts())
        {
            mesh.add(vert);
        }

        for (Edge edge : this.edges())
        {
            mesh.add(edge);
        }

        for (Face face : this.faces())
        {
            mesh.add(face);
        }

        return mesh;
    }

    public List<String> validate()
    {
        List<String> msgs = Lists.newArrayList();

        for (VertexHE vert : this.verts.values())
        {
            if (vert.vert == null)
            {
                msgs.add("A vertex element is not properly initialized");
            }
        }

        for (FaceHE face : this.faces.values())
        {
            if (face.hedge == null)
            {
                msgs.add("A face points to a null half edge");
            }
        }

        for (EdgeHE edge : this.edges.values())
        {
            if (edge.hedge == null)
            {
                msgs.add("An edge points to a null half edge");
            }
            else
            {
                for (HalfEdge he : new HalfEdge[] { edge.hedge, edge.hedge.pair })
                {
                    if (he.edge == null)
                    {
                        msgs.add("A half edge points to a null edge");
                    }
                    else if (he.vert == null)
                    {
                        msgs.add("A half edge points to a null vertex");
                    }
                    else if (he.pair == null)
                    {
                        msgs.add("A half edge points to a null pair");
                    }
                    else if (he.next == null)
                    {
                        msgs.add("A half edge points to a null next");
                    }
                    else if (he.prev == null)
                    {
                        msgs.add("A half edge points to a null mousePrev");
                    }
                    else if (he.pair == he)
                    {
                        msgs.add("A half edge is paired with itself");
                    }
                    else if (he.prev == he)
                    {
                        msgs.add("A half edge's mousePrev is itself");
                    }
                    else if (he.next == he)
                    {
                        msgs.add("A half edge's next is itself");
                    }
                    else if (he.next.prev != he)
                    {
                        msgs.add("The next.mousePrev operation is not consistent");
                    }
                    else if (he.prev.next != he)
                    {
                        msgs.add("The mousePrev.next operation is not consistent");
                    }
                    else if (he.pair.pair != he)
                    {
                        msgs.add("The pair.pair operation is not consistent");
                    }
                    else if (he.pair == he)
                    {
                        msgs.add("The pair of a half edge is itself");
                    }
                    else if (he.prev == he)
                    {
                        msgs.add("The mousePrev of a half edge is itself");
                    }
                    else if (he.next == he)
                    {
                        msgs.add("The next of a half edge is itself");
                    }
                    else if (he.face == null && he.next.face != null)
                    {
                        msgs.add("The next of a boundary isn't a boundary");
                    }
                    else if (he.face != null && he.next.face == null)
                    {
                        msgs.add("The next of a non-boundary is a boundary");
                    }
                }
            }
        }

        return msgs;
    }

    public int numBoundComponents()
    {
        List<HalfEdge> bounds = Lists.newArrayList(this.bounds());
        int n = 0;
        HalfEdge current = null;

        while (bounds.size() > 0)
        {
            if (current != null && bounds.contains(current.next))
            {
                current = current.next;
            }
            else
            {
                n++;
                current = bounds.get(0);
            }
            bounds.remove(current);
        }
        return n;
    }

    public int numBound()
    {
        return this.bounds().size();
    }

    public List<Vertex> bound()
    {
        List<Vertex> out = Lists.newArrayList();
        for (HalfEdge he : Lists.newArrayList(this.bounds()))
        {
            out.add(he.vert.vert);
        }

        return out;
    }

    /**********************
     * Add/Remove METHODS *
     **********************/

    public boolean add(Vertex vert)
    {
        if (!this.has(vert))
        {
            VertexHE verthe = new VertexHE();
            verthe.vert = vert;
            verthe.hedge = null;
            this.verts.put(vert, verthe);

            this.max_vidx = Math.max(this.max_vidx, vert.id());

            return true;
        }

        return false;
    }

    public boolean add(Edge e)
    {
        // Check the vertex states and check for existing edge
        boolean add_a = this.add(e.getA());
        boolean add_b = this.add(e.getB());
        VertexHE vert_a = this.verts.get(e.getA());
        VertexHE vert_b = this.verts.get(e.getB());

        // Check if an edge already exists
        if (this.connected(vert_a, vert_b))
        {
            return false;
        }

        // Create the necessary structures
        EdgeHE edge = new EdgeHE();
        HalfEdge he_a = new HalfEdge();
        HalfEdge he_b = new HalfEdge();

        // Set most of the fields
        he_a.edge = edge;
        he_a.face = null;
        he_a.pair = he_b;
        he_a.vert = vert_a;
        he_a.next = he_b;
        he_a.prev = he_b;

        he_b.edge = edge;
        he_b.face = null;
        he_b.pair = he_a;
        he_b.vert = vert_b;
        he_b.next = he_a;
        he_b.prev = he_a;

        edge.edge = e;
        edge.hedge = he_a;

        // Connect the half edge to mesh
        if (!add_a && vert_a.hedge != null)
        {
            // vertex a already exists
            HalfEdge in = this.findFreeIn(vert_a);
            HalfEdge out = in.next;

            in.next = he_a;
            he_a.prev = in;

            he_b.next = out;
            out.prev = he_b;
        }
        else
        {
            // vertex a is new
            vert_a.hedge = he_a;
        }

        if (!add_b && vert_b.hedge != null)
        {
            // vertex b already exists
            HalfEdge in = this.findFreeIn(vert_b);
            HalfEdge out = in.next;

            in.next = he_b;
            he_b.prev = in;

            he_a.next = out;
            out.prev = he_a;
        }
        else
        {
            // vertex b is new
            vert_b.hedge = he_b;
        }

        // Add the new elements
        this.edges.put(e, edge);

        return true;
    }

    public boolean add(Face f)
    {
        this.add(new Edge(f.getA(), f.getB()));
        this.add(new Edge(f.getB(), f.getC()));
        this.add(new Edge(f.getC(), f.getA()));

        VertexHE vert_a = this.verts.get(f.getA());
        VertexHE vert_b = this.verts.get(f.getB());
        VertexHE vert_c = this.verts.get(f.getC());

        HalfEdge hab = this.hedge(vert_a, vert_b);
        HalfEdge hbc = this.hedge(vert_b, vert_c);
        HalfEdge hca = this.hedge(vert_c, vert_a);
        HalfEdge hba = this.hedge(vert_b, vert_a);
        HalfEdge hac = this.hedge(vert_a, vert_c);
        HalfEdge hcb = this.hedge(vert_c, vert_b);

        HalfEdge[][] hess = { { hab, hbc, hca }, { hab.pair, hbc.pair, hca.pair }, { hba, hac, hcb },
                { hba.pair, hac.pair, hcb.pair } };

        for (int i = 0; i < hess.length; i++)
        {
            HalfEdge[] hes = hess[i];

            if (this.faceExists(hes))
            {
                Logging.info("duplicate face found");
                return false;
            }
            else if (this.canMakeFace(hes))
            {
                // The third and fourth are flipped...
                if (i == 2 || i == 3)
                {
                    f.flip();
                }

                FaceHE face = this.makeFace(hes);
                face.face = f;
                this.faces.put(f, face);
                return true;
            }
        }

        for (String msg : this.validate())
        {
            Logging.info(msg);
        }

        Logging.info("warning: failed to make face: " + f);

        return false;
    }

    /************************
     * CONNECTIVITY METHODS *
     ************************/

    public boolean boundary(Edge e)
    {
        return !this.hasLeft(e) || !this.hasRight(e);
    }

    public boolean hasLeft(Edge e)
    {
        return this.edges.get(e).hedge.face != null;
    }

    public boolean hasRight(Edge e)
    {
        return this.edges.get(e).hedge.pair.face != null;
    }

    public Face faceLeft(Edge e)
    {
        return this.edges.get(e).hedge.face.face;
    }

    public Face faceRight(Edge e)
    {
        return this.edges.get(e).hedge.pair.face.face;
    }

    public Vertex leftVertex(Edge e)
    {
        return this.edges.get(e).hedge.next.next.vert.vert;
    }

    public Vertex rightVertex(Edge e)
    {
        return this.edges.get(e).hedge.pair.next.next.vert.vert;
    }

    public List<Edge> edgeStar(Vertex vert)
    {
        List<Edge> out = Lists.newArrayList();
        for (HalfEdge he : this.findOut(this.verts.get(vert)))
        {
            out.add(he.edge.edge);
        }
        return out;
    }

    public boolean hasEdge(Vertex a, Vertex b)
    {
        if (!this.verts.containsKey(a) || !this.verts.containsKey(b))
        {
            return false;
        }

        return this.hedge(this.verts.get(a), this.verts.get(b)) != null;
    }

    public boolean hasFace(Vertex a, Vertex b, Vertex c)
    {
        if (!this.verts.containsKey(a) || !this.verts.containsKey(b) || !this.verts.containsKey(c))
        {
            return false;
        }

        HalfEdge he = this.hedge(this.verts.get(a), this.verts.get(b));
        if (he != null && he.face != null && he.next.pair.vert.vert.equals(c))
        {
            return true;
        }
        else if (he != null && he.pair.face != null && he.pair.next.pair.vert.vert.equals(c))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public Face face(Vertex a, Vertex b, Vertex c)
    {
        HalfEdge he = this.hedge(this.verts.get(a), this.verts.get(b));
        if (he.face != null && he.next.pair.vert.vert.equals(c))
        {
            return he.face.face;
        }
        else if (he.pair.face != null && he.pair.next.pair.vert.vert.equals(c))
        {
            return he.pair.face.face;
        }
        else
        {
            throw new RuntimeException("The given face does not exist");
        }
    }

    public List<Vertex> vertRing(Vertex v)
    {
        List<Vertex> out = Lists.newArrayList();
        VertexHE vert = this.verts.get(v);
        for (HalfEdge he : this.findOut(vert))
        {
            out.add(he.pair.vert.vert);
        }

        return out;
    }

    public List<Edge> edgeRing(Vertex v)
    {
        List<Edge> out = Lists.newArrayList();
        VertexHE vert = this.verts.get(v);
        for (HalfEdge he : this.findOut(vert))
        {
            if (he.face != null)
            {
                out.add(he.next.edge.edge);
            }
        }

        return out;
    }

    public List<Edge> edges(Face f)
    {
        List<Edge> out = Lists.newArrayList();
        out.add(new Edge(f.getA(), f.getB()));
        out.add(new Edge(f.getB(), f.getC()));
        out.add(new Edge(f.getC(), f.getA()));

        return out;
    }

    public List<Face> faceRing(Vertex v)
    {
        List<Face> out = Lists.newArrayList();
        VertexHE vert = this.verts.get(v);
        for (HalfEdge he : this.findOut(vert))
        {
            if (he.face != null)
            {
                out.add(he.face.face);
            }
        }

        return out;
    }

    public List<Face> faceStar(Edge e)
    {
        List<Face> faces = Lists.newArrayList();
        faces.add(this.faceLeft(e));
        faces.add(this.faceRight(e));

        return faces;
    }

    public List<Face> faceStar(Face f)
    {
        List<Face> faces = Lists.newArrayList();
        faces.add(this.opposite(f, new Edge(f.getA(), f.getB())));
        faces.add(this.opposite(f, new Edge(f.getB(), f.getC())));
        faces.add(this.opposite(f, new Edge(f.getC(), f.getA())));

        return faces;
    }

    public Face opposite(Face f, Edge e)
    {
        Face left = this.faceLeft(e);
        Face right = this.faceRight(e);

        if (f.equals(left))
        {
            return right;
        }
        else if (f.equals(right))
        {
            return left;
        }
        else
        {
            throw new RuntimeException("invalid edge and face pair");
        }
    }
}