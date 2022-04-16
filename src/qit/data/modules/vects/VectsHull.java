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

package qit.data.modules.vects;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.AttrMap;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.VectsUtils;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.HalfEdgePoly;
import qit.math.structs.Plane;
import qit.math.structs.Segment;
import qit.math.structs.Triangle;
import qit.math.structs.Vertex;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

@ModuleDescription("Compute the convex hull of vects")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Barber, C. B., Dobkin, D. P., & Huhdanpaa, H. (1996). The quickhull algorithm for convex hulls. ACM Transactions on Mathematical Software (TOMS), 22(4), 469-483.")
public class VectsHull implements Module
{
    public static final double DELTA = 1e-6;

    @ModuleInput
    @ModuleDescription("input vects")
    public Vects input;
    
    @ModuleOutput
    @ModuleDescription("output convex hull")
    public Mesh output;

    @Override
    public VectsHull run()
    {
        this.output = hull(this.input);
        return this;
    }

    public static Mesh hull(Vects vects)
    {
        Map<Vertex, Vect> coords = Maps.newHashMap();
        int idx = 0;
        for (Vect vect : vects)
        {
            coords.put(new Vertex(idx++), vect);
        }

        return hull(coords);
    }

    public static Mesh hull(Map<Vertex, Vect> coords)
    {
        Logging.info("started quickhull with " + coords.size() + " points");

        int dim = 3;

        // find vertices at min/max extreme points in each channel
        Double[] minCoords = new Double[dim];
        Double[] maxCoords = new Double[dim];
        Vertex[] minVerts = new Vertex[dim];
        Vertex[] maxVerts = new Vertex[dim];

        for (Vertex v : coords.keySet())
        {
            Vect vect = coords.get(v);
            for (int i = 0; i < dim; i++)
            {
                double c = vect.get(i);

                if (minCoords[i] == null || c < minCoords[i])
                {
                    minCoords[i] = c;
                    minVerts[i] = v;
                }

                if (maxCoords[i] == null || c > maxCoords[i])
                {
                    maxCoords[i] = c;
                    maxVerts[i] = v;
                }
            }
        }

        // make extreme point list
        List<Vertex> ep = Lists.newArrayList();
        for (Vertex v : minVerts)
        {
            ep.add(v);
        }
        for (Vertex v : maxVerts)
        {
            ep.add(v);
        }

        // compute the pyramid baseline from the most distant extreme points
        Double maxdab = null;
        Vertex va = null;
        Vertex vb = null;
        for (Vertex a : ep)
        {
            for (Vertex b : ep)
            {
                double dist = coords.get(a).dist(coords.get(b));
                if (maxdab == null || dist > maxdab)
                {
                    maxdab = dist;
                    va = a;
                    vb = b;
                }
            }
        }

        // use the most distant vertex for to make a triangle
        Segment ebase = new Segment(coords.get(va), coords.get(vb));
        Double maxdc = null;
        Vertex vc = null;
        for (Vertex c : ep)
        {
            double dist = ebase.dist(coords.get(c));
            if (maxdc == null || dist > maxdc)
            {
                maxdc = dist;
                vc = c;
            }
        }

        // compute the remaining point to form a tetrahedron
        Plane pbase = new Triangle(coords.get(va), coords.get(vb), coords.get(vc)).plane();
        Double maxdd = null;
        Vertex vd = null;
        for (Vertex d : coords.keySet())
        {
            double dist = Math.abs(pbase.dist(coords.get(d)));
            if (maxdd == null || dist > maxdd)
            {
                maxdd = dist;
                vd = d;
            }
        }

        // create a half edge structure for looking up face adjacency
        HalfEdgePoly poly = new HalfEdgePoly();
        poly.add(va);
        poly.add(vb);
        poly.add(vc);
        poly.add(vd);

        List<Vect> basev = Lists.newArrayList();
        basev.add(coords.get(va));
        basev.add(coords.get(vb));
        basev.add(coords.get(vc));
        basev.add(coords.get(vd));
        Vect mean = VectsUtils.mean(basev);

        if (new Triangle(coords.get(va), coords.get(vb), coords.get(vc)).plane().dist(mean) < 0)
        {
            poly.add(new Face(va, vb, vc));
            poly.add(new Face(vd, vb, va));
            poly.add(new Face(vd, va, vc));
            poly.add(new Face(vd, vc, vb));
        }
        else
        {
            poly.add(new Face(va, vc, vb));
            poly.add(new Face(vd, va, vb));
            poly.add(new Face(vd, vc, va));
            poly.add(new Face(vd, vb, vc));
        }

        // compute the planes for each face
        Map<Face, Plane> planes = Maps.newHashMap();
        for (Face f : poly.faces())
        {
            planes.put(f, new Triangle(coords.get(f.getA()), coords.get(f.getB()), coords.get(f.getC())).plane());
        }

        // assign outside points to faces
        Map<Face, Set<Vertex>> outside = Maps.newHashMap();
        for (Face f : poly.faces())
        {
            outside.put(f, new HashSet<Vertex>());
        }
        for (Vertex v : coords.keySet())
        {
            if (v == va || v == vb || v == vc || v == vd)
            {
                continue;
            }

            Vect c = coords.get(v);
            for (Face f : poly.faces())
            {
                if (planes.get(f).dist(c) > DELTA)
                {
                    outside.get(f).add(v);
                    break;
                }
            }
        }

        // populate the initial stack of faces to process
        Stack<Face> stack = new Stack<>();
        for (Face f : poly.faces())
        {
            if (outside.get(f).size() > 0)
            {
                stack.add(f);
            }
        }

        // process faces on stack
        int step = 25;
        int iter = 1;
        while (stack.size() > 0)
        {
            if (iter % step == 0)
            {
                Logging.info("started iteration " + iter++);
            }

            for (Face f : outside.keySet())
            {
                for (Vertex v : outside.get(f))
                {
                    Global.assume(!poly.has(v), "invalid settings!");
                }
            }

            Face f = stack.pop();
            Plane p = planes.get(f);
            if (outside.get(f).size() == 0)
            {
                continue;
            }

            // select the farthest point from the face
            Double maxd = null;
            Vertex maxv = null;
            for (Vertex v : outside.get(f))
            {
                double dist = p.dist(coords.get(v));
                if (maxd == null || dist > maxd)
                {
                    maxd = dist;
                    maxv = v;
                }
            }
            Vect maxc = coords.get(maxv);

            // find the neighboring and visible faces
            Set<Face> visfaces = Sets.newHashSet();
            visfaces.add(f);

            // this could be more efficient by recursively searching neighbors
            for (Face n : poly.faces())
            {
                if (planes.get(n).dist(maxc) > DELTA)
                {
                    visfaces.add(n);
                }
            }

            // find boundary edges
            Set<Edge> bound = Sets.newHashSet();
            for (Face vf : visfaces)
            {
                for (Edge e : poly.edges(vf))
                {
                    if (!visfaces.contains(poly.opposite(vf, e)))
                    {
                        bound.add(e);
                    }
                }
            }

            // generate the new faces
            Set<Face> newfaces = Sets.newHashSet();
            for (Edge e : bound)
            {
                newfaces.add(new Face(e.getA(), e.getB(), maxv));
            }

            // update structures and compute vis face union
            Set<Vertex> union = Sets.newHashSet();
            for (Face vf : visfaces)
            {
                for (Vertex v : outside.get(vf))
                {
                    if (!vf.equals(maxv))
                    {
                        union.add(v);
                    }
                }

                planes.remove(vf);
                stack.remove(vf);
                outside.remove(vf);
            }

            for (Face nf : newfaces)
            {
                Triangle tri = new Triangle(coords.get(nf.getA()), coords.get(nf.getB()), coords.get(nf.getC()));
                Plane plane = tri.plane();
                planes.put(nf, plane);
                outside.put(nf, new HashSet<Vertex>());
                stack.push(nf);
            }

            union.remove(maxv);
            for (Vertex v : union)
            {
                for (Face nf : newfaces)
                {
                    if (planes.get(nf).dist(coords.get(v)) > DELTA)
                    {
                        outside.get(nf).add(v);
                        break;
                    }
                }
            }

            for (Face nf : newfaces)
            {
                if (outside.get(nf).size() > 0)
                {
                    stack.push(nf);
                }
            }

            // build a new poly (can't remove faces from this impl)
            HalfEdgePoly npoly = new HalfEdgePoly();

            // add non-visible faces
            for (Face pf : poly.faces())
            {
                if (!visfaces.contains(pf))
                {
                    npoly.add(pf);
                }
            }

            npoly.add(maxv);
            for (Face nf : newfaces)
            {
                npoly.add(nf);
            }

            // set the polyhedron pointer
            poly = npoly;
        }

        Logging.info("number of hull vertices: " + poly.numVertex());
        Logging.info("number of hull edges: " + poly.numEdge());
        Logging.info("number of hull faces: " + poly.numFace());

        // create a mesh structure
        AttrMap<Vertex> attr = new AttrMap<>();
        attr.add(Mesh.COORD, VectSource.create3D());

        for (Vertex v : poly.verts())
        {
            attr.set(v, Mesh.COORD, coords.get(v));
        }

        Logging.info("finished computing quickhull");

        return new Mesh(attr, poly);
    }
}