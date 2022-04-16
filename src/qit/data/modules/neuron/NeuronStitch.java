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


package qit.data.modules.neuron;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.base.structs.Pointer;
import qit.base.structs.Triple;
import qit.data.datasets.Matrix;
import qit.data.datasets.Neuron;
import qit.data.datasets.Neuron.Index;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Containable;
import qit.math.structs.Segment;
import qit.math.utils.MathUtils;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.Linkage;
import smile.clustering.linkage.SingleLinkage;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@ModuleDescription("Perform stitching to build neuron segments into a complete neuron.  The neuron will be split into pieces and rebuilt.")
@ModuleAuthor("Ryan Cabeen")
public class NeuronStitch implements Module
{
    enum NeuronStitchSomaMode
    {
        Manual, RadiusMax, Cluster
    }

    @ModuleInput
    @ModuleDescription("the input neuron")
    public Neuron input;

    @ModuleInput
    @ModuleOptional
    @ModuleAdvanced
    @ModuleDescription("a solids object for manually defining somas for stitching, e.g. each box and sphere will define a region for building somas (only used when the soma mode is Manual)")
    public Solids somas = null;

    @ModuleParameter
    @ModuleDescription("choose a mode for selecting the soma(s).  Manual selection is for user-defined soma (from a solids object passed to the somas option).  RadiusMax is for choosing the soma from the node with the largest radius.  Cluster is for detecting the soma by clustering endpoints and finding the largest cluster (depends on the cluster option for defining the cluster spatial extent)")
    public NeuronStitchSomaMode mode = NeuronStitchSomaMode.RadiusMax;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a maximum distance for stitching together segments.  If two segments are farther than this distance, they will not be stitched (default is to stitch everything")
    public Double threshold = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a specific radius for cluster-based soma selection (only used for the Cluster soma detection mode)")
    public Double cluster = null;

    @ModuleOutput
    @ModuleDescription("output neuron")
    public Neuron output;

    @Override
    public NeuronStitch run()
    {
        double myThresh = this.threshold == null ? Double.MAX_VALUE : this.threshold;

        Neuron.Index index = this.input.index();

        BiFunction<Integer, Integer, Double> dist = (a, b) -> index.map.get(a).vect().dist(index.map.get(b).vect());
        Function<List<Integer>, Integer> head = (list) -> list.get(0);
        Function<List<Integer>, Integer> tail = (list) -> list.get(list.size() - 1);

        Set<Integer> found = Sets.newHashSet();
        final LinkedList<List<Integer>> pieces = Lists.newLinkedList();

        List<Integer> starts = Lists.newArrayList();
        starts.addAll(index.forks);
        starts.addAll(index.leaves);

        for (Integer node : starts)
        {
            List<Integer> piece = Lists.newArrayList();
            piece.add(node);
            node = index.map.get(node).parent;

            while (node != -1)
            {
                piece.add(node);

                if (found.contains(node) || index.forks.contains(node))
                {
                    break;
                }

                node = index.map.get(node).parent;
            }

            if (piece.size() > 1)
            {
                pieces.add(piece);
                found.addAll(piece);

                Logging.infosub("..... found piece with %d nodes", piece.size());
            }
        }

        Logging.infosub("... found a total of %d pieces with %d remaining nodes", pieces.size(), (index.map.size() - found.size()));

        Neuron out = new Neuron();

        List<Integer> ends = Lists.newArrayList();
        for (List<Integer> piece : pieces)
        {
            ends.add(head.apply(piece));
            ends.add(tail.apply(piece));
        }

        Map<Integer, Integer> nodeMap = Maps.newHashMap();

        final Pointer<Integer> maxlabel = Pointer.to(1);
        Supplier<Integer> getLabel = () ->
        {
            maxlabel.set(maxlabel.get() + 1);
            return maxlabel.get() - 1;
        };

        Consumer<List<Integer>> buildSoma = (inside) ->
        {
            VectsOnlineStats posStats = new VectsOnlineStats(3);
            VectOnlineStats radStats = new VectOnlineStats();

            for (Integer end : inside)
            {
                posStats.update(index.map.get(end).vect());
                radStats.update(index.map.get(end).radius);
            }

            Neuron.Node somaNode = new Neuron.Node();
            somaNode.label = getLabel.get();
            somaNode.type = Neuron.TYPE_SOMA;
            somaNode.parent = -1;
            somaNode.xpos = posStats.mean.getX();
            somaNode.ypos = posStats.mean.getY();
            somaNode.zpos = posStats.mean.getZ();
            somaNode.radius = radStats.mean;

            out.nodes.add(somaNode);
            for (Integer key : inside)
            {
                nodeMap.put(key, somaNode.label);
            }

            Logging.info("... added soma with %d neighbors", nodeMap.size());
        };

        if (this.mode == NeuronStitchSomaMode.Cluster)
        {
            Double myCluster = this.cluster;

            if (this.cluster == null)
            {
                myCluster = 0.01 * this.input.box().getDiameter();
            }

            Logging.info("building soma with automated clustering");

            int nends = ends.size();
            Matrix dists = new Matrix(nends, nends);
            for (int i = 0; i < nends; i++)
            {
                for (int j = i + 1; j < nends; j++)
                {
                    double mydist = dist.apply(ends.get(i), ends.get(j));
                    dists.set(i, j, mydist);
                    dists.set(j, i, mydist);
                }
            }

            Linkage link = new SingleLinkage(dists.toArray());
            HierarchicalClustering clusterer = new HierarchicalClustering(link);
            int[] labels = clusterer.partition(myCluster);

            Map<Integer, Integer> counts = MathUtils.counts(labels);
            Integer somakey = MathUtils.maxkey(counts);

            List<Integer> inside = Lists.newArrayList();
            for (int i = 0; i < labels.length; i++)
            {
                if (labels[i] == somakey)
                {
                    inside.add(ends.get(i));
                }
            }

            buildSoma.accept(inside);
        }
        else if (this.mode == NeuronStitchSomaMode.Manual)
        {
            Global.nonnull(this.somas, "somas");
            Logging.info("building soma with user-defined solids");

            Consumer<Containable> buildContainableSoma = (containable) ->
            {
                List<Integer> inside = Lists.newArrayList();
                for (Integer end : ends)
                {
                    if (containable.contains(index.map.get(end).vect()))
                    {
                        inside.add(end);
                    }
                }

                buildSoma.accept(inside);
            };

            for (int i = 0; i < somas.numSpheres(); i++)
            {
                buildContainableSoma.accept(this.somas.getSphere(i));
            }

            for (int i = 0; i < somas.numBoxes(); i++)
            {
                buildContainableSoma.accept(this.somas.getBox(i));
            }
        }
        else
        {
            Neuron.Node maxNode = this.input.nodes.stream().max(Comparator.comparing(Neuron.Node::getRadius)).orElseThrow(NoSuchElementException::new);

            Neuron.Node somaNode = new Neuron.Node();
            somaNode.label = getLabel.get();
            somaNode.type = Neuron.TYPE_SOMA;
            somaNode.parent = -1;
            somaNode.xpos = maxNode.xpos;
            somaNode.ypos = maxNode.ypos;
            somaNode.zpos = maxNode.zpos;
            somaNode.radius = maxNode.radius;

            out.nodes.add(somaNode);
            nodeMap.put(maxNode.label, somaNode.label);

            Logging.info("... detected soma at node %d with radius " + maxNode.label, maxNode.label);
        }

        BiConsumer<Integer, List<Integer>> eatPiece = (parent, piece) ->
        {
            for (int i = 0; i < piece.size(); i++)
            {
                int key = piece.get(i);

                if (i == 0 && nodeMap.containsKey(key))
                {
                    continue;
                }

                Neuron.Node inputNode = index.map.get(key);

                Neuron.Node outputNode = new Neuron.Node();
                outputNode.label = getLabel.get();
                outputNode.type = Neuron.TYPE_DENDRITE;
                outputNode.xpos = inputNode.xpos;
                outputNode.ypos = inputNode.ypos;
                outputNode.zpos = inputNode.zpos;
                outputNode.radius = inputNode.radius;
                outputNode.parent = parent;

                parent = outputNode.label;
                out.nodes.add(outputNode);
                nodeMap.put(inputNode.label, outputNode.label);
            }

            piece.clear();
        };

        Function<Integer, Pair<Integer, Double>> getClosestOutput = (key) ->
        {
            Pair<Integer, Double> match = Pair.of(-1, Double.MAX_VALUE);

            for (Neuron.Node mynode : out.nodes)
            {
                double mydist = mynode.vect().dist(index.map.get(key).vect());

                if (mydist < match.b)
                {
                    match.a = mynode.label;
                    match.b = mydist;
                }
            }

            return match;
        };

        while (pieces.size() > 0)
        {
            for (List<Integer> piece : pieces)
            {
                Integer myhead = head.apply(piece);
                Integer mytail = tail.apply(piece);

                if (piece.size() > 1 && nodeMap.containsKey(myhead))
                {
                    Logging.info("... added a connected branch");
                    eatPiece.accept(nodeMap.get(myhead), piece);
                }
                else if (piece.size() > 1 && nodeMap.containsKey(mytail))
                {
                    Logging.info("... added a connected branch");
                    Collections.reverse(piece);
                    eatPiece.accept(nodeMap.get(mytail), piece);
                }
            }

            pieces.removeIf(item -> item.size() == 0);

            Triple<Integer, List<Integer>, Double> myclosest = Triple.of(-1, null, Double.MAX_VALUE);

            for (List<Integer> piece : pieces)
            {
                int myhead = head.apply(piece);
                int mytail = tail.apply(piece);

                Pair<Integer, Double> myheadMatch = getClosestOutput.apply(myhead);
                Pair<Integer, Double> mytailMatch = getClosestOutput.apply(mytail);
                double mindist = Math.min(myheadMatch.b, mytailMatch.b);

                if (mindist < myclosest.c && mindist < myThresh)
                {
                    if (myheadMatch.b < mytailMatch.b)
                    {
                        myclosest.a = myheadMatch.a;
                        myclosest.b = piece;
                        myclosest.c = mindist;
                    }
                    else if (mytailMatch.b < myheadMatch.b)
                    {
                        Collections.reverse(piece);
                        myclosest.a = mytailMatch.a;
                        myclosest.b = piece;
                        myclosest.c = mindist;
                    }
                }
            }

            if (myclosest.b != null)
            {
                Logging.info("... added close branch with distance %g", myclosest.c);
                eatPiece.accept(myclosest.a, myclosest.b);
            }
            else if (pieces.size() > 0)
            {
                Logging.info("... added a dangling tree");
                eatPiece.accept(-1, pieces.get(0));
                pieces.get(0).clear();
            }
            pieces.removeIf(item -> item.size() == 0);
        }

        this.output = out;

        return this;
    }

    public static Neuron apply(Neuron data)
    {
        return new NeuronStitch()
        {{
            this.input = data;
        }}.run().output;
    }
}
