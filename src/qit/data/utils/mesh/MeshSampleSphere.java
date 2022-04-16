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

package qit.data.utils.mesh;

import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Bounded;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.modules.mask.MaskDilate;
import qit.data.source.MaskSource;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.structs.Bary;
import qit.math.structs.BihSearch;
import qit.math.structs.Box;
import qit.math.structs.Face;
import qit.math.structs.Triangle;
import qit.math.structs.Vertex;

import java.util.Set;
import java.util.function.Function;

public class MeshSampleSphere
{
    private enum VertexSearchType
    {
        NAIVE, BIH, GRID
    }

    private Mesh input;
    private Mesh output;
    private String insphere = Mesh.COORD;
    private String outsphere = Mesh.COORD;
    private Set<String> labels = Sets.newHashSet();
    private Set<String> skip = Sets.newHashSet();
    private VertexSearchType search = VertexSearchType.BIH;
    
    public MeshSampleSphere()
    {

    }

    public MeshSampleSphere withInput(Mesh m)
    {
        this.input = m;

        return this;
    }

    public MeshSampleSphere withOutput(Mesh m)
    {
        this.output = m;

        return this;
    }

    public MeshSampleSphere addLabel(String s)
    {
        this.labels.add(s);

        return this;
    }

    public MeshSampleSphere addSkip(String s)
    {
        this.skip.add(s);

        return this;
    }

    public MeshSampleSphere withInputSphere(String s)
    {
        this.insphere = s;

        return this;
    }

    public MeshSampleSphere withOutputSphere(String s)
    {
        this.outsphere = s;

        return this;
    }

    public MeshSampleSphere withSearch(String s)
    {
        this.search = VertexSearchType.valueOf(s);

        return this;
    }

    public MeshSampleSphere run()
    {
        Logging.info("using input sphere attribute: " + this.insphere);
        Logging.info("using output sphere attribute: " + this.outsphere);

        if (VertexSearchType.NAIVE.equals(this.search))
        {
            this.naive();
        }
        else if (VertexSearchType.BIH.equals(this.search))
        {
            this.bih();
        }
        else if (VertexSearchType.GRID.equals(this.search))
        {
            this.grid();
        }
        else
        {
            Logging.error("invalid search type");
        }

        return this;
    }

    private void naive()
    {
        for (Vertex vert : this.output.vattr)
        {
            // build the coordinates
            Vect v = this.output.vattr.get(vert, this.outsphere);

            // find the closest face
            Double min_dist = null;
            Face min_face = null;
            Bary min_bary = null;
            for (Face face : this.input.graph.faces())
            {
                Vect a = this.input.vattr.get(face.getA(), this.insphere);
                Vect b = this.input.vattr.get(face.getB(), this.insphere);
                Vect c = this.input.vattr.get(face.getC(), this.insphere);

                Triangle tri = new Triangle(a, b, c);
                Bary bary = tri.closest(v);
                Vect coord = tri.vect(bary);
                double dist = coord.dist(v);
                if (min_dist == null || dist < min_dist)
                {
                    min_dist = dist;
                    min_face = face;
                    min_bary = bary;
                }
            }

            // resampling
            for (String name : this.input.vattr.attrs())
            {
                if (!name.equals(this.insphere))
                {
                    Vect a = this.input.vattr.get(min_face.getA(), name);
                    Vect b = this.input.vattr.get(min_face.getB(), name);
                    Vect c = this.input.vattr.get(min_face.getC(), name);

                    Triangle tri = new Triangle(a, b, c);
                    
                    // either nearest neighbor or linear
                    Vect val = this.labels.contains(name) ? tri.nearest(min_bary) : tri.vect(min_bary);
                    
                    this.output.vattr.set(vert, name, val);
                }
            }

            this.output.vattr.set(vert, Mesh.DISTANCE, VectSource.create1D(min_dist));
        }
    }

    private void bih()
    {
        Logging.info("bulding search tree");
        BihSearch<Bounded<Face>, Bounded<Vect>> bih = MeshUtils.bihFaces(this.input, this.insphere);

        Logging.info("querying");
        Box mbox = MeshUtils.bounds(this.input, this.insphere);
        VectOnlineStats mstats = MeshUtils.sizeStats(this.input, this.insphere);
        int num = this.output.vattr.size();
        int idx = 0;
        int step = num / 100;
        for (Vertex vert : this.output.vattr)
        {
            if (idx % step == 0)
            {
                Logging.progress(String.format("%d percent processed", 100 * idx / (num - 1)));
            }
            idx += 1;
            
            Vect v = this.output.vattr.get(vert, this.outsphere);
            Box box = Box.createRadius(v, mstats.max);

            Global.assume(box.intersects(mbox), "vert lies too far from the surface");

            Bounded<Vect> rec = new Bounded<Vect>(v, box);
            Set<Bounded<Face>> ints = bih.intersections(rec);

            // Grow the box until a match is found, searches are cheap...
            while (ints.size() == 0)
            {
                box = box.grow(mstats.max);
                ints = bih.intersections(new Bounded<>(v, box));
            }

            Double min_dist = null;
            Face min_face = null;
            Bary min_bary = null;

            for (Bounded<Face> inter : ints)
            {
                Face face = inter.getData();
                Vect a = this.input.vattr.get(face.getA(), this.insphere);
                Vect b = this.input.vattr.get(face.getB(), this.insphere);
                Vect c = this.input.vattr.get(face.getC(), this.insphere);

                Triangle tri = new Triangle(a, b, c);
                Bary bary = tri.closest(v);
                Vect coord = tri.vect(bary);
                double dist = coord.dist(v);
                if (min_dist == null || dist < min_dist)
                {
                    min_dist = dist;
                    min_face = face;
                    min_bary = bary;
                }
            }

            for (String name : this.input.vattr.attrs())
            {
                if (!name.equals(this.insphere) && !this.skip.contains(name))
                {
                    Vect a = this.input.vattr.get(min_face.getA(), name);
                    Vect b = this.input.vattr.get(min_face.getB(), name);
                    Vect c = this.input.vattr.get(min_face.getC(), name);

                    Triangle tri = new Triangle(a, b, c);

                    Vect val = this.labels.contains(name) ? tri.nearest(min_bary) : tri.vect(min_bary);
                    this.output.vattr.set(vert, name, val);
                }
            }

            this.output.vattr.set(vert, Mesh.DISTANCE, VectSource.create1D(min_dist));
        }
        
        Logging.info("100 percent processed");
    }

    private void grid()
    {
        Logging.info("building grid index");
        Mask labels = MaskSource.create(MeshUtils.bounds(this.input, this.insphere).scale(1.1), 256, 256, 256);
        Function<Vect, Sample> sample = (p) -> labels.getSampling().nearest(p);
        for (Vertex v : this.input.vattr)
        {
            Vect p = this.input.vattr.get(v, this.insphere);
            labels.set(sample.apply(p), v.id());
        }
        MaskDilate.apply(labels, 5);

        for (Vertex vert : this.output.vattr)
        {
            // build the coordinates
            Vect pos = this.output.vattr.get(vert, this.outsphere);

            Vertex nvert = new Vertex(labels.get(sample.apply(pos)));
            Vect npos = this.input.vattr.get(nvert, this.insphere);
            double ndist = pos.dist(npos);

            boolean found = true;
            while (found)
            {
                found = false;
                for (Vertex rvert : this.input.graph.vertRing(nvert))
                {
                    Vect rpos = this.input.vattr.get(rvert, this.insphere);
                    double rdist = rpos.dist(pos);

                    if (rdist < ndist)
                    {
                        nvert = rvert;
                        ndist = rdist;

                        found = true;
                    }
                }
            }

            // find the closest face
            Double min_dist = null;
            Face min_face = null;
            Bary min_bary = null;
            for (Face face : this.input.graph.faceRing(nvert))
            {
                Vect a = this.input.vattr.get(face.getA(), this.insphere);
                Vect b = this.input.vattr.get(face.getB(), this.insphere);
                Vect c = this.input.vattr.get(face.getC(), this.insphere);

                Triangle tri = new Triangle(a, b, c);
                Bary bary = tri.closest(pos);
                Vect coord = tri.vect(bary);
                double dist = coord.dist(pos);

                if (min_dist == null || dist < min_dist)
                {
                    min_dist = dist;
                    min_face = face;
                    min_bary = bary;
                }
            }

            // resampling
            for (String name : this.input.vattr.attrs())
            {
                if (!name.equals(this.insphere) && !this.skip.contains(name))
                {
                    Vect a = this.input.vattr.get(min_face.getA(), name);
                    Vect b = this.input.vattr.get(min_face.getB(), name);
                    Vect c = this.input.vattr.get(min_face.getC(), name);

                    Triangle tri = new Triangle(a, b, c);

                    // either nearest neighbor or linear
                    Vect val = this.labels.contains(name) ? tri.nearest(min_bary) : tri.vect(min_bary);

                    this.output.vattr.set(vert, name, val);
                }
            }

            this.output.vattr.set(vert, Mesh.DISTANCE, VectSource.create1D(min_dist));
        }
    }
}
