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

package qit.data.modules.mri.tensor;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskSort;
import qit.data.source.SamplingSource;
import qit.data.models.Tensor;
import qit.math.structs.DisjointSet;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Tensor volume graph-based segmentation based on principal direction")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTensorSegmentGraph implements Module
{
    @ModuleInput
    @ModuleDescription("the input tensor volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("use a line metric")
    public boolean line = false;

    @ModuleParameter
    @ModuleDescription("use a log Euclidean metric")
    public boolean log = false;

    @ModuleParameter
    @ModuleDescription("a threshold for grouping")
    public Double threshold = 1.0;
    
    @ModuleOutput
    @ModuleDescription("the output segmentation mask")
    public Mask output;

    private List<Edge> graph()
    {
        Sampling sampling = this.input.getSampling();
        int window = 1;
        int dim = 1 + 2 * window;
        int center = window;
        Sampling fsampling = SamplingSource.create(dim, dim, dim);

        List<Edge> edges = Lists.newArrayList();
        for (Sample a : sampling)
        {
            if (!this.input.valid(a, this.mask))
            {
                continue;
            }

            Tensor aModel = new Tensor(this.input.get(a));
            if (this.log)
            {
                aModel = aModel.log();
            }
            int aidx = sampling.index(a);

            for (Sample off : fsampling)
            {
                int bi = a.getI() + off.getI() - center;
                int bj = a.getJ() + off.getJ() - center;
                int bk = a.getK() + off.getK() - center;
                Sample b = new Sample(bi, bj, bk);

                if (!this.input.valid(b, this.mask))
                {
                    continue;
                }

                Tensor bModel = new Tensor(this.input.get(b));
                if (this.log)
                {
                    bModel = bModel.log();
                }
                int bidx = sampling.index(b);

                double dist = this.line ? aModel.getVec(0).angleLineDeg(bModel.getVec(0)) : aModel.dist(bModel);

                if (!Double.isNaN(dist) && !Double.isInfinite(dist))
                {
                    edges.add(new Edge(aidx, bidx, dist));
                }
            }
        }

        return edges;
    }

    public VolumeTensorSegmentGraph run()
    {
        Global.assume(this.mask != null, "invalid mask");

        Logging.info("started tensor volume segmentation");

        Logging.info("building graph");
        List<Edge> edges = this.graph();

        Logging.info("segmenting graph");
        DisjointSet<Integer> segments = this.segment(edges, this.threshold);

        Logging.info("preparing output");
        Mask out = new Mask(this.input.getSampling());
        Sampling sampling = out.getSampling();
        Map<Integer, Integer> lookup = segments.getLookup();
        for (Sample sample : sampling)
        {
            if (!this.input.valid(sample, this.mask))
            {
                continue;
            }

            int idx = sampling.index(sample);

            if (!segments.has(idx))
            {
                continue;
            }

            int nlabel = lookup.get(segments.find(idx));
            out.set(sample, nlabel);
        }

        out = MaskSort.sort(out);

        this.output = out;

        Logging.info("finshed tensor volume segmentation");

        return this;
    }

    public DisjointSet<Integer> segment(List<Edge> edges, double c)
    {
        Set<Integer> vertices = Sets.newHashSet();
        for (Edge edge : edges)
        {
            vertices.add(edge.a);
            vertices.add(edge.b);
        }

        Collections.sort(edges, new Comparator<Edge>()
        {
            public int compare(Edge left, Edge right)
            {
                return Double.compare(left.w, right.w);
            }
        });

        DisjointSet<Integer> forest = new DisjointSet<Integer>();
        Map<Integer, Double> thresholds = Maps.newHashMap();
        for (Integer vertex : vertices)
        {
            forest.add(vertex);
            thresholds.put(vertex, c);
        }

        for (Edge edge : edges)
        {
            int a = forest.find(edge.a);
            int b = forest.find(edge.b);
            if (a != b)
            {
                double w = edge.w;
                double at = thresholds.get(a);
                double bt = thresholds.get(b);

                if (w <= at && w <= bt)
                {
                    forest.join(a, b);
                    Integer root = forest.find(a);
                    thresholds.put(root, w + c / forest.getSize(root));
                }
            }
        }

        return forest;
    }

    private static class Edge
    {
        // start
        int a;

        // end
        int b;

        // weight
        double w;

        Edge(int a, int b, double w)
        {
            this.a = a;
            this.b = b;
            this.w = w;
        }
    }
}
