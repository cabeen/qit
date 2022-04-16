package qit.data.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliUtils;
import qit.base.structs.Pair;
import qit.base.structs.Pointer;
import qit.base.structs.Triple;
import qit.data.datasets.Matrix;
import qit.data.datasets.Neuron;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.modules.curves.CurvesSimplify;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.Containable;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.Linkage;
import smile.clustering.linkage.SingleLinkage;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NeuronUtils
{
    public static Neuron which(Neuron neuron, String which)
    {
        Neuron out = new Neuron();

        Map<Integer, Neuron> split = split(neuron);

        for (Integer key : CliUtils.parseWhich(which))
        {
            if (split.containsKey(key))
            {
                out.nodes.addAll(split.get(key).nodes);
            }
        }

        return out;
    }

    public static Neuron laplacian(Neuron input, int iters, double lambda)
    {
        Neuron neuron = input.copy();
        Neuron.Index index = neuron.index();

        Map<Integer, Pair<Integer, Integer>> neighbors = Maps.newHashMap();
        for (Integer idx : index.trunks)
        {
            neighbors.put(idx, Pair.of(null, null));
        }

        for (Integer idx : index.trunks)
        {
            Integer pidx = index.map.get(idx).parent;
            neighbors.get(idx).a = index.trunks.contains(pidx) ? pidx : null;

            if (neighbors.containsKey(pidx))
            {
                neighbors.get(pidx).b = idx;
            }
        }

        for (int i = 0; i < iters; i++)
        {
            Map<Neuron.Node, Pair<Vect, Double>> update = Maps.newHashMap();

            for (Neuron.Node node : neuron.nodes)
            {
                if (neighbors.keySet().contains(node.label))
                {
                    Vect pos = VectSource.create3D(node.xpos, node.ypos, node.zpos);
                    double rad = node.radius;

                    Vect apos = VectSource.create3D();
                    double arad = 0;
                    double aweight = 0;

                    Vect bpos = VectSource.create3D();
                    double brad = 0;
                    double bweight = 0;

                    Pair<Integer, Integer> pair = neighbors.get(node.label);

                    if (pair.a != null)
                    {
                        Neuron.Node mynode = index.map.get(pair.a);
                        apos = VectSource.create3D(mynode.xpos, mynode.ypos, mynode.zpos);
                        arad = mynode.radius;
                        aweight = lambda;
                    }

                    if (pair.b != null)
                    {
                        Neuron.Node mynode = index.map.get(pair.b);
                        bpos = VectSource.create3D(mynode.xpos, mynode.ypos, mynode.zpos);
                        brad = mynode.radius;
                        bweight = lambda;
                    }

                    double weight = ((1.0 - aweight) + (1.0 - bweight));

                    Vect mypos = pos.times(weight).plus(aweight, apos).plus(bweight, bpos).times(0.5);
                    double myrad = 0.5 * (weight * rad + aweight * arad + bweight * brad);

                    update.put(node, Pair.of(mypos, myrad));
                }
            }

            for (Neuron.Node node : update.keySet())
            {
                Pair<Vect, Double> myupdate = update.get(node);

                node.xpos = myupdate.a.getX();
                node.ypos = myupdate.a.getY();
                node.zpos = myupdate.a.getZ();
                node.radius = myupdate.b;
            }
        }

        return neuron;
    }

    public static Neuron polynomial(Neuron input, int order, Double lambda)
    {
        Neuron neuron = input.copy();
        Neuron.Index index = neuron.index();

        Map<Integer, List<Integer>> segments = segments(neuron);
        Logging.info("... segment count: " + segments.size());

        for (Integer key : segments.keySet())
        {
            List<Integer> segment = segments.get(key);
            outer:
            for (int neworder = order; neworder > 1; neworder--)
            {
                try
                {
                    Vect cumlen = new Vect(segment.size());
                    Vects pos = new Vects();
                    for (int i = 0; i < segment.size(); i++)
                    {
                        pos.add(index.map.get(segment.get(i)).vect());

                        if (i > 0)
                        {
                            double dist = pos.get(i).dist(pos.get(i - 1));
                            cumlen.set(i, cumlen.get(i - 1) + dist);
                        }
                    }

                    Vect weights = VectSource.createND(segment.size(), 1.0 / (segment.size() - 2));
                    weights.set(0, 0);
                    weights.set(segment.size() - 1, 0);

                    Vect gamma = cumlen.times(1.0 / cumlen.get(segment.size() - 1));
                    VectFunction function = VectFunctionSource.polynomial(gamma.vects(), weights.vects(), pos, neworder, lambda);

                    for (int i = 1; i < segment.size() - 1; i++)
                    {
                        Vect npos = function.apply(gamma.getv(i));

                        Neuron.Node node = index.map.get(segment.get(i));
                        node.xpos = npos.getX();
                        node.ypos = npos.getY();
                        node.zpos = npos.getZ();
                    }

                    Logging.infosub("... filtered segment with order %d and %d points", neworder, segment.size());

                    break outer;
                }
                catch (RuntimeException e)
                {
                    Logging.info("...... failed to fit polynomial, skipping");
                }
            }
        }

        return neuron;
    }

    public static Neuron simplify(Neuron input, double epsilon)
    {
        Neuron.Index index = input.index();
        Map<Integer, List<Integer>> segments = segments(input);
        Logging.info("... segment count: " + segments.size());

        Map<Integer, Neuron.Node> nodes = Maps.newHashMap();
        int removed = 0;

        for (Integer key : segments.keySet())
        {
            List<Integer> segment = segments.get(key);
            Map<Integer, Integer> nodeToSeg = Maps.newHashMap();
            for (int i = 0; i < segment.size(); i++)
            {
                nodeToSeg.put(segment.get(i), i);
            }

            if (segment.size() > 2)
            {
                Vects vects = new Vects();
                for (int i = 0; i < segment.size(); i++)
                {
                    vects.add(index.map.get(segment.get(i)).vect());
                }

                boolean[] include = CurvesSimplify.simplifyDouglasPeucker(vects, epsilon);
                removed += MathUtils.countfalse(include);

                for (int i = 0; i < include.length; i++)
                {
                    if (include[i])
                    {
                        Neuron.Node node = index.map.get(segment.get(i)).copy();
                        nodes.put(node.label, node);
                    }
                }
            }
        }

        Logging.infosub("removed %d of %d points", removed, input.nodes.size());

        Neuron neuron = new Neuron();

        for (int idx : nodes.keySet())
        {
            Neuron.Node node = nodes.get(idx);
            int parent = node.parent;
            while (parent != -1 && !nodes.containsKey(parent))
            {
                parent = index.map.get(parent).parent;
            }
            node.parent = parent;

            neuron.nodes.add(node);
        }

        return relabel(neuron);
    }

    public static Neuron lowess(Neuron input, int num, int order)
    {
        Neuron neuron = input.copy();
        Neuron.Index index = neuron.index();

        Map<Integer, List<Integer>> segments = segments(neuron);
        Logging.info("... segment count: " + segments.size());

        for (Integer key : segments.keySet())
        {
            List<Integer> segment = segments.get(key);

            if (segment.size() > 4)
            {
                for (int i = 1; i < segment.size() - 1; i++)
                {
                    try
                    {
                        Vects pos = new Vects();

                        int centerIndex = 0;
                        Vect centerPos = null;

                        for (int j = Math.max(0, i - num); j < Math.min(segment.size(), i + num); j++)
                        {
                            Vect mypos = index.map.get(segment.get(j)).vect();
                            if (j == i)
                            {
                                centerIndex = pos.size();
                                centerPos = mypos;
                            }

                            pos.add(mypos);
                        }

                        Vect cumlen = new Vect(pos.size());
                        for (int j = 1; j < pos.size(); j++)
                        {
                            double dist = pos.get(j).dist(pos.get(j - 1));
                            cumlen.set(j, cumlen.get(j - 1) + dist);
                        }

                        Vect dists = new Vect(pos.size());
                        for (int j = 0; j < pos.size(); j++)
                        {
                            dists.set(j, centerPos.dist(pos.get(j)));
                        }

                        Vect weights = dists.times(1.0 / dists.max()).abs().pow(3).times(-1).plus(1).pow(3);
                        Vect gamma = cumlen.times(1.0 / cumlen.get(cumlen.size() - 1));

                        VectFunction function = VectFunctionSource.polynomial(gamma.vects(), weights.vects(), pos, order);
                        Vect npos = function.apply(gamma.getv(centerIndex));

                        Global.assume(npos.finite(), "is not finite");
                        if (npos.finite())
                        {
                            Neuron.Node node = index.map.get(segment.get(i));
                            node.xpos = npos.getX();
                            node.ypos = npos.getY();
                            node.zpos = npos.getZ();
                        }
                    }
                    catch (RuntimeException e)
                    {
                        Logging.info("... skipping point");
                    }
                }

                Logging.infosub("... filtered segment with %d points", segment.size());
            }
        }

        return neuron;
    }

    public static Neuron kernel(Neuron input, double sigma, int order, double thresh)
    {
        Neuron neuron = input.copy();
        Neuron.Index index = neuron.index();

        Map<Integer, List<Integer>> segments = segments(neuron);
        Logging.info("... segment count: " + segments.size());

        for (Integer key : segments.keySet())
        {
            List<Integer> segment = segments.get(key);

            outer:
            for (int neworder = order; neworder > 1; neworder--)
            {
                try
                {
                    Vect cumlen = new Vect(segment.size());
                    Vects pos = new Vects();
                    for (int i = 0; i < segment.size(); i++)
                    {
                        pos.add(index.map.get(segment.get(i)).vect());

                        if (i > 0)
                        {
                            double dist = pos.get(i).dist(pos.get(i - 1));
                            cumlen.set(i, cumlen.get(i - 1) + dist);
                        }
                    }

                    Vects gamma = VectsSource.create1D(cumlen.times(1.0 / cumlen.get(segment.size() - 1)));

                    VectFunction function = VectFunctionSource.kernel(sigma, gamma, pos, neworder, thresh);

                    for (int i = 1; i < segment.size() - 1; i++)
                    {
                        Vect npos = function.apply(gamma.get(i));

                        Neuron.Node node = index.map.get(segment.get(i));
                        node.xpos = npos.getX();
                        node.ypos = npos.getY();
                        node.zpos = npos.getZ();
                    }

                    Logging.infosub("... filtered segment with order %d and %d points", neworder, segment.size());

                    break outer;
                }
                catch (RuntimeException e)
                {
                    Logging.info("...... failed to fit kernel, skipping");
                }
            }
        }

        return neuron;
    }

    public static Map<Integer, List<Integer>> segments(Neuron neuron)
    {
        Neuron.Index index = neuron.index();
        Map<Integer, List<Integer>> segments = Maps.newHashMap();

        Queue<Integer> queue = Queues.newPriorityQueue();
        queue.addAll(index.leaves);

        while (!queue.isEmpty())
        {
            Integer start = queue.poll();

            Integer current = start;

            List<Integer> segment = Lists.newArrayList();
            segment.add(current);

            while (index.map.containsKey(current))
            {
                int parent = index.map.get(current).parent;
                segment.add(parent);

                if (index.forks.contains(parent) || index.roots.contains(parent))
                {
                    if (!segments.containsKey(start) && !index.roots.contains(parent))
                    {
                        queue.add(parent);
                    }
                    break;
                }

                current = parent;
            }

            if (segment.size() > 3)
            {
                segments.put(start, segment);
            }
        }

        return segments;
    }

    public static Neuron cut(Neuron neuron, double dist)
    {
        Neuron out = new Neuron();

        Neuron.Index index = neuron.index();
        Map<Integer, Set<Integer>> tree = Maps.newHashMap();

        tree.put(-1, Sets.newHashSet());
        for (Neuron.Node node : neuron.nodes)
        {
            tree.put(node.label, Sets.newHashSet());
        }

        for (Neuron.Node node : neuron.nodes)
        {
            int child = node.label;
            int parent = node.parent;

            tree.get(parent).add(child);
        }

        for (Integer root : index.roots)
        {
            Neuron.Node rootNode = index.map.get(root);
            Vect rootPos = rootNode.vect();

            out.nodes.add(rootNode);

            Queue<Integer> search = Queues.newArrayDeque();
            search.addAll(tree.get(root));

            while (!search.isEmpty())
            {
                Integer test = search.poll();
                Neuron.Node testNode = index.map.get(test);
                Vect testPos = testNode.vect();

                if (testPos.dist(rootPos) < dist)
                {
                    out.nodes.add(testNode);
                    search.addAll(tree.get(test));
                }
            }
        }

        return out;
    }

    public static Map<Integer, Neuron> split(Neuron neuron)
    {
        Neuron.Index index = neuron.index();

        Map<Integer, Integer> rootmap = Maps.newHashMap();
        for (Integer current : index.map.keySet())
        {
            Integer root = null;
            List<Integer> path = Lists.newArrayList();

            while (true)
            {
                path.add(current);

                if (rootmap.keySet().contains(current))
                {
                    root = rootmap.get(current);
                    break;
                }

                if (index.roots.contains(current))
                {
                    root = current;
                    break;
                }

                current = index.map.get(current).parent;
            }

            for (Integer node : path)
            {
                rootmap.put(node, root);
            }
        }

        Map<Integer, Neuron> out = Maps.newHashMap();
        for (Integer current : rootmap.keySet())
        {
            int root = rootmap.get(current);
            if (!out.containsKey(root))
            {
                out.put(root, new Neuron());
            }

            out.get(root).nodes.add(index.map.get(current));
        }

        for (Integer key : out.keySet())
        {
            out.put(key, relabel(out.get(key)));
        }

        return out;
    }

    public static Neuron jitter(Neuron neuron, double amount, boolean roots, boolean forks, boolean leaves, boolean trunks)
    {
        Neuron out = neuron.copy();

        Neuron.Index index = out.index();
        Consumer<List<Integer>> jitterme = (keys) ->
        {
            for (Integer key : keys)
            {
                double dx = amount * Global.RANDOM.nextGaussian();
                double dy = amount * Global.RANDOM.nextGaussian();
                double dz = amount * Global.RANDOM.nextGaussian();

                index.map.get(key).shift(dx, dy, dz);
            }
        };

        if (leaves)
        {
            jitterme.accept(index.leaves);
        }

        if (forks)
        {
            jitterme.accept(index.forks);
        }

        if (roots)
        {
            jitterme.accept(index.roots);
        }

        if (trunks)
        {
            jitterme.accept(index.trunks);
        }

        return out;
    }

    public static Neuron relabel(Neuron neuron)
    {
        Neuron out = new Neuron();

        Map<Integer, Integer> lookup = Maps.newHashMap();

        // relabel the ndoes and create a lookup table
        lookup.put(-1, -1);
        for (Neuron.Node node : neuron.nodes)
        {
            Neuron.Node mynode = node.copy();
            mynode.label = out.nodes.size() + 1;
            out.nodes.add(mynode);

            lookup.put(node.label, mynode.label);
        }

        // relabel the parent pointers
        for (Neuron.Node node : out.nodes)
        {
            node.parent = lookup.get(node.parent);
        }

        return out;
    }

    public static Neuron debase(Neuron neuron)
    {
        // This function segments the basal dendrite from a neuron file by isolating the soma branch that contains the longest dendritic branch

        Neuron out = neuron.copy();
        Neuron.Index index = out.index();

        double basalLength = 0;
        Neuron.Node basalRoot = null;
        Neuron.Node basalNode = null;

        for (Integer leaf : index.leaves)
        {
            double length = 0;
            Neuron.Node previous = null;
            Neuron.Node current = index.map.get(leaf);

            while (!index.roots.contains(current.label))
            {
                previous = current;
                current = index.map.get(current.parent);

                length += previous.vect().dist(current.vect());
            }

            if (basalLength < length)
            {
                basalLength = length;
                basalNode = previous;
                basalRoot = current;
            }
        }

        if (basalRoot != null)
        {
            Logging.info("detected basal dendrite with length: " + basalLength);
            Neuron.Node newRoot = basalRoot.copy();
            newRoot.label = NeuronUtils.maxlabel(out) + 1;
            out.nodes.add(newRoot);
            basalNode.parent = newRoot.label;
        }
        else
        {
            Logging.info("no basal dendrite was found");
        }

        return out;
    }

    public static int maxlabel(Neuron neuron)
    {
        int maxlabel = 0;
        for (Neuron.Node node : neuron.nodes)
        {
            maxlabel = Math.max(maxlabel, node.label);
        }

        return maxlabel;
    }

    public static Neuron sort(Neuron neuron)
    {
        Neuron out = new Neuron();

        // topological sort by pre-order traversal
        Neuron.Index index = neuron.index();
        Map<Integer, Set<Integer>> tree = Maps.newHashMap();

        tree.put(-1, Sets.newHashSet());
        for (Neuron.Node node : neuron.nodes)
        {
            tree.put(node.label, Sets.newHashSet());
        }

        for (Neuron.Node node : neuron.nodes)
        {
            int child = node.label;
            int parent = node.parent;

            tree.get(parent).add(child);
        }

        List<Integer> sorted = Lists.newArrayList();

        for (Integer root : index.roots)
        {
            Queue<Integer> queue = Queues.newPriorityQueue();
            queue.add(root);

            while (queue.size() > 0)
            {
                Integer idx = queue.poll();
                sorted.add(idx);

                for (Integer cidx : tree.get(idx))
                {
                    queue.add(cidx);
                }
            }
        }

        // relabel by order
        Map<Integer, Integer> lookup = Maps.newHashMap();
        lookup.put(-1, -1);
        for (int i = 0; i < sorted.size(); i++)
        {
            Neuron.Node mynode = index.map.get(sorted.get(i));
            lookup.put(mynode.label, i + 1);
            mynode.label = i + 1;

            out.nodes.add(mynode);
        }

        // relabel the parent pointers
        for (Neuron.Node node : out.nodes)
        {
            node.parent = lookup.get(node.parent);
        }

        return out;
    }

    public static Neuron apply(Neuron neuron, VectFunction xfm)
    {
        Neuron out = neuron.copy();

        for (Neuron.Node node : neuron.nodes)
        {
            node.set(xfm.apply(node.vect()));
        }

        return out;
    }
}
