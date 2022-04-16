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

package qit.data.modules.mesh;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.utils.MeshUtils;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.HalfEdgePoly;
import qit.math.structs.Vertex;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@ModuleDescription("Simplify a input by removing short edges")
@ModuleAuthor("Ryan Cabeen")
public class MeshSimplify implements Module
{
    @ModuleInput
    @ModuleDescription("the input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("maxima number of iterations")
    public Integer maxiter = 10000;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("maxima number of vertices")
    public Integer maxvert = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("maxima number of edges")
    public Integer maxedge = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("maxima number of faces")
    public Integer maxface = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("maxima average face surface area")
    public Double meanarea = null;

    @ModuleParameter
    @ModuleDescription("do not remove boundary vertices")
    public boolean nobound = false;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleParameter
    @ModuleDescription("print messages")
    public boolean chatty = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    public MeshSimplify run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();

        if (this.maxiter == null)
        {
            this.maxiter = Integer.MAX_VALUE;
        }

        Logging.info(this.chatty, "started simplification");

        int num = 0;

        while (num++ < this.maxiter)
        {
            Logging.info(this.chatty, "started simplification iteration " + num);

            List<Edge> equeue = findValidEdges(mesh);

            simplify(mesh, equeue);

            int nv = mesh.graph.numVertex();
            int ne = mesh.graph.numEdge();
            int nf = mesh.graph.numFace();
            double sa = MeshUtils.area(mesh);
            double mfa = sa / nf;

            // check if all constraints are satisfied
            boolean tvert = this.maxvert == null ? true : nv <= this.maxvert;
            boolean tedge = this.maxedge == null ? true : ne <= this.maxedge;
            boolean tface = this.maxface == null ? true : nf <= this.maxface;
            boolean tarea = this.meanarea == null ? true : mfa > this.meanarea;

            if (tvert & tedge & tface & tarea)
            {
                break;
            }
        }

        Logging.info(this.chatty, "recomputing normals");
        MeshUtils.computeNormals(mesh);

        Logging.info(this.chatty, "finished simplification");

        this.output = mesh;

        return this;
    }

    public void simplify(Mesh mesh, Iterable<Edge> edges)
    {
        Map<Vertex, Vertex> nverts = new HashMap<>();
        for (Edge edge : edges)
        {
            Vertex vert = mesh.graph.addVertex();
            mesh.vattr.add(vert);

            for (String attr : mesh.vattr.attrs())
            {
                Vect va = mesh.vattr.get(edge.getA(), attr);
                Vect vb = mesh.vattr.get(edge.getB(), attr);
                Vect vect = va.copy();
                vect.timesEquals(0.5);
                vect.plusEquals(0.5, vb);
                mesh.vattr.set(vert, attr, vect);
            }

            nverts.put(edge.getA(), vert);
            nverts.put(edge.getB(), vert);
        }

        HalfEdgePoly ngraph = mesh.graph.proto();
        for (Face f : mesh.graph.faces())
        {
            Vertex a = f.getA();
            Vertex b = f.getB();
            Vertex c = f.getC();

            boolean ha = nverts.containsKey(a);
            boolean hb = nverts.containsKey(b);
            boolean hc = nverts.containsKey(c);

            int vc = (ha ? 1 : 0) + (hb ? 1 : 0) + (hc ? 1 : 0);
            if (vc == 0)
            {
                ngraph.add(f);
            }
            else if (vc == 1)
            {
                Vertex na = ha ? nverts.get(a) : a;
                Vertex nb = hb ? nverts.get(b) : b;
                Vertex nc = hc ? nverts.get(c) : c;
                Face nf = new Face(na, nb, nc);
                ngraph.add(nf);
            }
            else if (vc == 3)
            {
                Logging.error("edges aren't disjoint");
            }
        }

        mesh.graph = ngraph;

        List<Vertex> remove = Lists.newArrayList();
        for (Vertex v : mesh.vattr)
        {
            if (!mesh.graph.has(v))
            {
                remove.add(v);
            }
        }

        for (Vertex v : remove)
        {
            mesh.vattr.remove(v);
        }
    }

    public List<Edge> findValidEdges(Mesh mesh)
    {
        Set<Vertex> boundaryVerts = Sets.newHashSet();

        if (this.nobound)
        {
            for (Edge edge : mesh.graph.edges())
            {
                if (mesh.graph.boundary(edge))
                {
                    boundaryVerts.add(edge.getA());
                    boundaryVerts.add(edge.getB());
                }
            }
        }

        Function<Edge, Boolean> collapsable = (edge) ->
        {
            List<Vertex> averts = mesh.graph.vertRing(edge.getA());
            List<Vertex> bverts = mesh.graph.vertRing(edge.getB());

            // skip boundary vertices if needed
            if (this.nobound)
            {
                Set<Vertex> union = Sets.newHashSet(averts);
                union.addAll(bverts);

                for (Vertex v : union)
                {
                    if (boundaryVerts.contains(v))
                    {
                        return false;
                    }
                }
            }

            // this checks for a specific edge case
            // where the ring of a vertex folds
            // skipping these cases ensures the
            // simplification preserves the topology

            Set<Vertex> shared = Sets.newHashSet(averts);
            shared.retainAll(bverts);

            for (Vertex v : shared)
            {
                if (!mesh.graph.hasFace(edge.getA(), edge.getB(), v))
                {
                    return false;
                }
            }

            return true;
        };

        List<Edge> edges = new ArrayList<>(mesh.graph.numEdge());
        BitSet verts = new BitSet();

        Map<Edge, Double> cost = new HashMap<>();

        for (Edge edge : mesh.graph.edges())
        {
            cost.put(edge, MeshUtils.length(mesh, edge));
        }

        List<Edge> sorted = new ArrayList<>(cost.keySet());
        Collections.sort(sorted, new Comparator<Edge>()
        {
            public int compare(Edge e1, Edge e2)
            {
                double c1 = cost.get(e1);
                double c2 = cost.get(e2);
                return c1 < c2 ? -1 : c1 > c2 ? 1 : 0;
            }

        });

        for (Edge edge : sorted)
        {
            int idxa = edge.getA().id();
            int idxb = edge.getB().id();

            if (!verts.get(idxa) && !verts.get(idxb) && collapsable.apply(edge))
            {
                edges.add(edge);

                verts.set(idxa);
                verts.set(idxb);

                for (Vertex v : mesh.graph.vertRing(edge.getA()))
                {
                    verts.set(v.id());
                }

                for (Vertex v : mesh.graph.vertRing(edge.getB()))
                {
                    verts.set(v.id());
                }
            }
        }


        return edges;
    }

    public static Mesh apply(Mesh mesh, double mean)
    {
        return new MeshSimplify()
        {{
            this.input = mesh;
            this.meanarea = mean;
        }}.run().output;
    }
}
