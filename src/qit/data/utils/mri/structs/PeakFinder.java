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


package qit.data.utils.mri.structs;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Integers;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.models.Fibers;
import qit.data.modules.mri.odf.VolumeOdfFeature;
import qit.data.modules.vects.VectsHull;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.VectsUtils;
import qit.data.utils.vects.stats.VectsAxialStats;
import qit.data.utils.vects.stats.VectsStats;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.Linkage;
import smile.clustering.linkage.SingleLinkage;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PeakFinder
{
    public static final int DEFAULT_COMPS = 3;
    public static final double DEFAULT_CLUSTER = 30;
    public static final double DEFAULT_THRESH = 0.01;

    public enum PeakMode
    {
        Sum, Mean, Max
    }

    private List<Integers> neighbors = null;
    private Vects points = null;

    public int comps = DEFAULT_COMPS;
    public double thresh = DEFAULT_THRESH;
    public double cluster = DEFAULT_CLUSTER;

    public PeakMode mode = PeakMode.Sum;
    public boolean match = false;
    public boolean gfa = false;

    public void init(int detail)
    {
        this.init(MeshSource.sphere(detail));
    }

    public void init(Mesh mesh)
    {
        this.points = new Vects();
        this.neighbors = Lists.newArrayList();

        {
            List<Vertex> verts = Lists.newArrayList();
            Map<Vertex, Integer> reverse = Maps.newHashMap();
            for (Vertex vert : mesh.graph.verts())
            {
                this.points.add(mesh.vattr.get(vert, Mesh.COORD));
                reverse.put(vert, verts.size());
                verts.add(vert);
            }

            for (Vertex vert : verts)
            {
                List<Vertex> ring = mesh.graph.vertRing(vert);
                int[] n = new int[ring.size()];
                for (int i = 0; i < ring.size(); i++)
                {
                    n[i] = reverse.get(ring.get(i));
                }
                this.neighbors.add(new Integers(n));
            }
        }
    }

    public void init(Vects points)
    {
        VectsHull huller = new VectsHull();
        huller.input = VectsUtils.normalize(points);
        Mesh mesh = huller.run().output;

        this.init(mesh);
    }

    public Vects getPoints()
    {
        return this.points;
    }

    public Fibers find(Vect odf)
    {
        Global.assume(this.points != null, "you must run init first");

        Fibers fibers = new Fibers(this.comps);

        double factor = this.gfa ? VolumeOdfFeature.gfa(odf) : 1.0;

        int[] nearest = new int[odf.size()];
        Map<Integer, Vects> peaks = Maps.newHashMap();
        peaks.put(-1, new Vects());

        outer:
        for (int i = 0; i < odf.size(); i++)
        {
            double qi = odf.get(i);

            if (qi < this.thresh)
            {
                // this one is ignored
                nearest[i] = -1;
                continue;
            }

            int maxidx = i;
            double maxq = qi;

            for (int n : this.neighbors.get(i))
            {
                double qj = odf.get(n);
                if (qj > maxq)
                {
                    maxidx = n;
                    maxq = qj;
                }
            }

            nearest[i] = maxidx;

            if (maxidx == i)
            {
                peaks.put(i, new Vects());
            }
        }

        for (int i = 0; i < odf.size(); i++)
        {
            int maxidx = nearest[i];
            while (maxidx != -1 && !peaks.containsKey(maxidx))
            {
                maxidx = nearest[maxidx];
            }

            nearest[i] = maxidx;
            peaks.get(maxidx).add(VectSource.create1D(odf.get(i)));
        }

        for (Integer pidx : Sets.newHashSet(peaks.keySet()))
        {
            if (pidx != -1 && peaks.containsKey(pidx))
            {
                Vect di = this.points.get(pidx);

                for (Integer npidx : Sets.newHashSet(peaks.keySet()))
                {
                    if (npidx != -1 && npidx != pidx)
                    {
                        Vect dj = this.points.get(npidx);
                        double dist = di.angleLineDeg(dj);

                        if (dist <= 0.5 * this.cluster)
                        {
                            peaks.get(pidx).addAll(peaks.get(npidx));
                            peaks.remove(npidx);
                        }
                    }
                }
            }
        }

        List<Integer> peakidx = Lists.newArrayList(peaks.keySet());
        peakidx.remove(Integer.valueOf(-1));
        int npeaks = peakidx.size();

        try
        {
            if (npeaks == 1)
            {
                throw new RuntimeException("skip to baseline condition");
            }

            // yes, this is O(n^2), but n should be small

            Matrix dists = new Matrix(npeaks, npeaks);
            for (int i = 0; i < npeaks; i++)
            {
                Vect di = this.points.get(peakidx.get(i));
                for (int j = i + 1; j < npeaks; j++)
                {
                    Vect dj = this.points.get(peakidx.get(j));

                    double dist = di.angleLineDeg(dj);
                    dists.set(i, j, dist);
                    dists.set(j, i, dist);
                }
            }

            Linkage link = new SingleLinkage(dists.toArray());
            HierarchicalClustering clusterer = new HierarchicalClustering(link);
            int[] labels = clusterer.partition(this.cluster);

            double[] clusterQval = new double[npeaks];
            Vect[] clusterDir = new Vect[npeaks];
            int clusterCount = 0;
            int clusterCountMax = MathUtils.max(labels) + 1;
            for (int i = 0; i < clusterCountMax; i++)
            {
                Vects vs = new Vects();
                Vects qs = new Vects();

                for (int j = 0; j < npeaks; j++)
                {
                    if (labels[j] == i)
                    {
                        int pidx = peakidx.get(j);
                        vs.add(this.points.get(peakidx.get(j)));

                        switch (this.mode)
                        {
                            case Sum:
                                qs.add(peaks.get(pidx).sum());
                                break;
                            case Mean:
                                qs.add(peaks.get(pidx).mean());
                                break;
                            case Max:
                                qs.add(peaks.get(pidx).max());
                                break;
                        }
                    }
                }

                if (vs.size() > 0)
                {
                    switch (this.mode)
                    {
                        case Sum:
                            clusterQval[i] = qs.flatten().sum();
                            break;
                        case Mean:
                            clusterQval[i] = qs.flatten().mean();
                            break;
                        case Max:
                            clusterQval[i] = qs.flatten().max();
                            break;
                    }

                    clusterDir[i] = new VectsAxialStats().withInput(vs).run().mean;
                    clusterCount += 1;
                }
            }

            // sort fracs and make fibers
            int[] perm = MathUtils.permutation(clusterQval);
            for (int i = 0; i < Math.min(this.comps, clusterCount); i++)
            {
                int pidx = perm[perm.length - i - 1];
                Vect vect = clusterDir[pidx];
                double frac = factor * clusterQval[pidx];
                fibers.setLine(i, vect);
                fibers.setFrac(i, frac);
            }
        }
        catch (Exception e)
        {
            // this is used when there is a single peak or hierarchical clustering fails

            Vects vs = new Vects();
            Vects qs = new Vects();
            for (Integer peak : peaks.keySet())
            {
                if (peak != -1)
                {
                    vs.add(this.points.get(peak));

                    switch (this.mode)
                    {
                        case Sum:
                            qs.add(VectSource.create1D(peaks.get(peak).flatten().sum()));
                            break;
                        case Mean:
                            qs.add(VectSource.create1D(peaks.get(peak).flatten().mean()));
                            break;
                        case Max:
                            qs.add(VectSource.create1D(peaks.get(peak).flatten().max()));
                            break;
                    }
                }
            }

            Integers sort = qs.flatten().sort();

            for (int i = 0; i < Math.min(this.comps, sort.size()); i++)
            {
                int pidx = sort.get(sort.size()- i - 1);
                Vect vect = vs.get(pidx);
                double frac = factor * qs.get(pidx).get(0);
                fibers.setLine(i, vect);
                fibers.setFrac(i, frac);
            }
        }

        if (this.match)
        {
            double sum = odf.sum();
            double fsum = fibers.getFracSum();
            double scale = sum / fsum;

            for (int i = 0; i < fibers.size(); i++)
            {
                fibers.setFrac(i, fibers.getFrac(i) * scale);
            }
        }

        return fibers;
    }
}
