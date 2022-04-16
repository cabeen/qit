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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.AttrMap;
import qit.data.datasets.Mesh;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.structs.DisjointSet;
import qit.math.structs.Face;
import qit.math.structs.HalfEdgePoly;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Compute connected components of a mesh")
@ModuleAuthor("Ryan Cabeen")
public class MeshComponents implements Module
{
    @ModuleInput
    @ModuleDescription("the input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("retain only the largest component")
    public boolean largest;

    @ModuleParameter
    @ModuleDescription("the name of the component attribute")
    public String attr = Mesh.INDEX;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("retain components selected with the given attribute")
    public String select;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("retain components above a given surface area")
    public Double area;

    @ModuleParameter
    @ModuleDescription("invert the selection")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    public MeshComponents run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();

        DisjointSet<Integer> matching = new DisjointSet<>();
        int maxlabel = 1;

        Map<Vertex, Integer> labels = Maps.newHashMap();
        for (Vertex vi : mesh.graph.verts())
        {
            List<Integer> nlabels = Lists.newArrayList();
            for (Vertex vj : mesh.graph.vertRing(vi))
            {
                if (labels.containsKey(vj))
                {
                    nlabels.add(labels.get(vj));
                }
            }

            if (nlabels.size() == 0)
            {
                matching.add(maxlabel);
                labels.put(vi, maxlabel);
                maxlabel += 1;
            }
            else
            {
                int minlabel = 0;
                int mincount = Integer.MAX_VALUE;
                for (Integer nlabel : nlabels)
                {
                    int count = matching.getSize(nlabel);
                    if (count < mincount)
                    {
                        minlabel = nlabel;
                        mincount = count;
                    }
                }

                labels.put(vi, minlabel);

                for (int a : nlabels)
                {
                    for (int b : nlabels)
                    {
                        if (a != b)
                        {
                            matching.join(a, b);
                        }
                    }
                }
            }
        }

        Map<Integer, Integer> counts = Maps.newHashMap();
        for (Vertex vi : mesh.graph.verts())
        {
            int label = labels.get(vi);
            if (label == 0)
            {
                continue;
            }

            int nlabel = matching.find(label);
            labels.put(vi, nlabel);

            int prev = counts.containsKey(nlabel) ? counts.get(nlabel) : 0;
            counts.put(nlabel, prev + 1);
        }

        if (this.area != null)
        {
            Map<Integer, Double> areas = Maps.newHashMap();
            for (Face face : mesh.graph.faces())
            {
                Vertex vertex = face.getA();
                int label = labels.get(vertex);

                if (!areas.containsKey(label))
                {
                    areas.put(label, 0d);
                }

                areas.put(label, areas.get(label) + MeshUtils.area(mesh, face));
            }

            AttrMap<Vertex> nvattr = new AttrMap<Vertex>();
            for (Vertex vi : mesh.graph.verts())
            {
                int label = labels.get(vi);
                if (areas.get(label) > this.area)
                {
                    nvattr.add(vi);
                }
            }
            nvattr.copy(mesh.vattr);

            HalfEdgePoly ngraph = mesh.graph.proto();
            for (Face face : mesh.graph.faces())
            {
                Vertex va = face.getA();
                Vertex vb = face.getB();
                Vertex vc = face.getC();
                if (nvattr.contains(va) && nvattr.contains(vb) && nvattr.contains(vc))
                {
                    ngraph.add(face);
                }
            }

            mesh.graph = ngraph;
            mesh.vattr = nvattr;
        }

        Map<Integer, Integer> lookup = MathUtils.remap(counts);

        if (this.largest)
        {
            AttrMap<Vertex> nvattr = new AttrMap<Vertex>();
            for (Vertex vi : mesh.graph.verts())
            {
                int label = lookup.get(labels.get(vi));
                if (label == 1)
                {
                    nvattr.add(vi);
                }
            }
            nvattr.copy(mesh.vattr);

            HalfEdgePoly ngraph = mesh.graph.proto();
            for (Face face : mesh.graph.faces())
            {
                Vertex va = face.getA();
                Vertex vb = face.getB();
                Vertex vc = face.getC();
                if (nvattr.contains(va) && nvattr.contains(vb) && nvattr.contains(vc))
                {
                    ngraph.add(face);
                }
            }

            mesh.graph = ngraph;
            mesh.vattr = nvattr;
        }

        if (this.select != null && mesh.vattr.has(this.select))
        {
            Set<Integer> selected = Sets.newHashSet();
            for (Vertex vi : mesh.graph.verts())
            {
                if (MathUtils.nonzero(mesh.vattr.get(vi, this.select).get(0)))
                {
                    int label = lookup.get(labels.get(vi));
                    selected.add(label);
                }
            }

            AttrMap<Vertex> nvattr = new AttrMap<>();
            for (Vertex vi : mesh.graph.verts())
            {
                int label = lookup.get(labels.get(vi));
                if (selected.contains(label) ^ this.invert)
                {
                    nvattr.add(vi);
                }
            }
            nvattr.copy(mesh.vattr);

            HalfEdgePoly ngraph = mesh.graph.proto();
            for (Face face : mesh.graph.faces())
            {
                Vertex va = face.getA();
                Vertex vb = face.getB();
                Vertex vc = face.getC();
                if (nvattr.contains(va) && nvattr.contains(vb) && nvattr.contains(vc))
                {
                    ngraph.add(face);
                }
            }

            mesh.graph = ngraph;
            mesh.vattr = nvattr;
        }

        if (!this.largest)
        {
            mesh.vattr.add(this.attr, VectSource.create1D());
            for (Vertex vi : mesh.graph.verts())
            {
                int label = lookup.get(labels.get(vi));
                mesh.vattr.set(vi, this.attr, VectSource.create(label));
            }
        }

        this.output = mesh;

        return this;
    }

    public static Mesh applyLargest(Mesh mesh)
    {
        return new MeshComponents()
        {{
            this.input = mesh;
            this.largest = true;
        }}.run().output;
    }

    public static Mesh applyArea(Mesh mesh, double myarea)
    {
        return new MeshComponents()
        {{
            this.input = mesh;
            this.area = myarea;
        }}.run().output;
    }
}
