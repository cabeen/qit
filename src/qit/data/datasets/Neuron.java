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

package qit.data.datasets;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import qit.base.Dataset;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Bounded;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.base.structs.Pointer;
import qit.base.structs.Triple;
import qit.base.utils.JavaUtils;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Box;
import qit.math.structs.Boxable;
import qit.math.utils.MathUtils;
import smile.clustering.HierarchicalClustering;
import smile.clustering.linkage.Linkage;
import smile.clustering.linkage.SingleLinkage;

/**
 * an entry in a table
 */
public class Neuron implements Dataset, Boxable
{
    public static final int TYPE_UNDEFINED = 0;
    public static final int TYPE_SOMA = 1;
    public static final int TYPE_AXON = 2;
    public static final int TYPE_DENDRITE = 3;
    public static final int TYPE_APICAL_DENDRITE = 4;
    public static final int TYPE_FORK_POINT = 5;
    public static final int TYPE_END_POINT = 6;
    public static final int TYPE_CUSTOM = 7;

    public static final int INDEX_LABEL = 0;
    public static final int INDEX_TYPE = 1;
    public static final int INDEX_X = 2;
    public static final int INDEX_Y = 3;
    public static final int INDEX_Z = 4;
    public static final int INDEX_R = 5;
    public static final int INDEX_PARENT = 6;

    public static final int ITEM_LENGTH = 7;

    public static final String ATTR_RADIUS = "radius";
    public static final String ATTR_LABEL = "label";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_TREE = "tree";
    public static final String ATTR_TREESORT = "treesort";
    public static final String ATTR_SEGMENT = "segment";

    public static class Node
    {
        public int label = 0;
        public int type = 0;
        public double xpos = 0;
        public double ypos = 0;
        public double zpos = 0;
        public double radius = 0;
        public int parent = 0;

        public double getRadius()
        {
            return this.radius;
        }

        public Node copy()
        {
            Node out = new Node();

            out.label = this.label;
            out.type = this.type;
            out.xpos = this.xpos;
            out.ypos = this.ypos;
            out.zpos = this.zpos;
            out.radius = this.radius;
            out.parent = this.parent;

            return out;
        }

        public void shift(double dx, double dy, double dz)
        {
            this.xpos += dx;
            this.ypos += dy;
            this.zpos += dz;
        }

        public void scale(double sx, double sy, double sz)
        {
            this.xpos *= sx;
            this.ypos *= sy;
            this.zpos *= sz;
        }

        public void set(Vect vect)
        {
            this.xpos = vect.getX();
            this.ypos = vect.getY();
            this.zpos = vect.getZ();
        }

        public Vect vect()
        {
            return VectSource.create3D(this.xpos, this.ypos, this.zpos);
        }
    }

    public static class Index
    {
        public Map<Integer, Node> map = Maps.newHashMap();
        public Map<Integer, Integer> children = Maps.newLinkedHashMap();
        public Map<Integer, Integer> tree = Maps.newLinkedHashMap();
        public Map<Integer, Integer> treesort = Maps.newLinkedHashMap();
        public List<Integer> roots = Lists.newArrayList();
        public List<Integer> leaves = Lists.newArrayList();
        public List<Integer> forks = Lists.newArrayList();
        public List<Integer> trunks = Lists.newArrayList();
    }

    public List<Node> nodes = Lists.newArrayList();

    public static Neuron read(String fn) throws IOException
    {
        // http://research.mssm.edu/cnic/swc.html

        InputStream is = new FileInputStream(fn);
        DataInputStream dis = new DataInputStream(new BufferedInputStream(is));
        String line = null;

        Neuron out = new Neuron();

        // Check the data set type
        while ((line = dis.readLine()) != null)
        {
            line = line.trim();
            if (line.length() != 0 && !line.startsWith("#"))
            {
                String[] tokens = line.split("\\s+");
                if (tokens.length >= ITEM_LENGTH)
                {
                    try
                    {
                        Node node = new Node();
                        node.label = Integer.parseInt(tokens[INDEX_LABEL]);
                        node.type = Integer.parseInt(tokens[INDEX_TYPE]);
                        node.xpos = Double.parseDouble(tokens[INDEX_X]);
                        node.ypos = Double.parseDouble(tokens[INDEX_Y]);
                        node.zpos = Double.parseDouble(tokens[INDEX_Z]);
                        node.radius = Double.parseDouble(tokens[INDEX_R]);
                        node.parent = Integer.parseInt(tokens[INDEX_PARENT]);

                        out.nodes.add(node);
                    }
                    catch (Exception e)
                    {
                        Logging.info("warning, failed to parse swc line: " + line);
                    }
                }
            }
        }

        Logging.info(String.format("found %d swc valid points", out.nodes.size()));

        dis.close();

        return out;
    }

    public Neuron add(Neuron neuron)
    {
        final Pointer<Integer> maxlabel = Pointer.to(this.maxlabel() + 1);
        final Map<Integer, Integer> lookup = Maps.newHashMap();

        Function<Integer, Integer> relabel = (label) ->
        {
            if (label == -1)
            {
                return -1;
            }

            if (!lookup.containsKey(label))
            {
                lookup.put(label, maxlabel.get());
                maxlabel.set(maxlabel.get() + 1);
            }

            return lookup.get(label);
        };

        for (Node node : neuron.nodes)
        {
            Node mynode = node.copy();
            mynode.label = relabel.apply(node.label);
            mynode.parent = relabel.apply(node.parent);

            this.nodes.add(mynode);
        }

        return this;
    }

    public int maxlabel()
    {
        int max = 0;
        for (Node node : this.nodes)
        {
            max = Math.max(node.label, max);
        }

        return max;
    }

    public void write(String fn) throws IOException
    {
        DataOutputStream pw = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fn)));

        for (Node node : this.nodes)
        {
            StringBuilder line = new StringBuilder();
            line.append(node.label);
            line.append(" ");
            line.append(node.type);
            line.append(" ");
            line.append(node.xpos);
            line.append(" ");
            line.append(node.ypos);
            line.append(" ");
            line.append(node.zpos);
            line.append(" ");
            line.append(node.radius);
            line.append(" ");
            line.append(node.parent);
            line.append("\n");

            pw.writeBytes(line.toString());
        }

        pw.close();
    }

    public Index index()
    {
        Index index = new Index();

        for (Node node : this.nodes)
        {
            if (!index.children.containsKey(node.label))
            {
                index.children.put(node.label, 0);
            }

            if (node.parent < 0)
            {
                index.roots.add(node.label);
            }
            else
            {
                int count = index.children.containsKey(node.parent) ? index.children.get(node.parent) : 0;
                index.children.put(node.parent, count + 1);
            }

            index.map.put(node.label, node);
        }

        for (Integer label : index.children.keySet())
        {
            switch (index.children.get(label))
            {
                case 0:
                    index.leaves.add(label);
                    break;
                case 1: // exactly one is an intermediate segment
                    index.trunks.add(label);
                    break;
                default: // more than one is a fork
                    if (!index.roots.contains(label))
                    {
                        index.forks.add(label);
                    }
            }
        }

        for (Integer label : index.map.keySet())
        {
            Node node = index.map.get(label);
            while (index.map.containsKey(node.parent))
            {
                node = index.map.get(node.parent);
            }

            index.tree.put(label, node.label);
        }

        for (Integer label : index.tree.keySet())
        {
            index.treesort.put(label, index.treesort.size() + 1);
        }

        Logging.info("detected leaves: " + index.leaves.size());
        Logging.info("detected forks: " + index.forks.size());
        Logging.info("detected roots: " + index.roots.size());
        Logging.info("detected trees: " + index.tree.size());
        Logging.info("children count: " + index.children.size());

        return index;
    }

    public Curves toCurves()
    {
        Index index = this.index();

        Queue<Integer> queue = Queues.newPriorityQueue();
        queue.addAll(index.leaves);
        queue.addAll(index.forks);

        Curves curves = new Curves();
        while (queue.size() > 0)
        {
            List<Integer> segment = Lists.newArrayList();

            int current = queue.poll();

            while (true)
            {
                if (index.map.containsKey(current))
                {
                    segment.add(current);

                    Node node = index.map.get(current);

                    if (segment.size() > 1 && (index.roots.contains(current) || index.forks.contains(current)))
                    {
                        break;
                    }
                    else
                    {
                        current = node.parent;
                    }
                }
                else
                {
                    Logging.info("warning: failed to find item with label " + current);
                    break;
                }
            }

            Curves.Curve curve = curves.add(segment.size());

            for (int i = 0; i < segment.size(); i++)
            {
                Node node = index.map.get(segment.get(i));

                double myradius = node.radius;

                if (i == 0 || i == segment.size() || index.roots.contains(node.label))
                {
                    myradius = 0;
                }

                curve.set(Curves.COORD, i, VectSource.create3D(node.xpos, node.ypos, node.zpos));
                curve.set(ATTR_RADIUS, i, VectSource.create1D(myradius));
                curve.set(ATTR_LABEL, i, VectSource.create1D(node.label));
                curve.set(ATTR_TYPE, i, VectSource.create1D(node.type));
                curve.set(ATTR_TREE, i, VectSource.create1D(index.tree.get(node.label)));
                curve.set(ATTR_TREESORT, i, VectSource.create1D(index.treesort.get(node.label)));
                curve.set(ATTR_SEGMENT, i, VectSource.create1D(curves.size()));
            }
        }

        return curves;
    }

    public Vects toVects()
    {
        Vects out = new Vects();
        for (Node node : this.nodes)
        {
            out.add(VectSource.create4D(node.xpos, node.ypos, node.zpos, node.radius));
        }

        return out;
    }

    public Vects toVectsRoots()
    {
        Index index = this.index();

        Vects out = new Vects();
        for (Integer label : index.roots)
        {
            Node node = index.map.get(label);
            out.add(VectSource.create4D(node.xpos, node.ypos, node.zpos, node.radius));
        }

        return out;
    }

    public Vects toVectsForks()
    {
        Index index = this.index();

        Vects out = new Vects();
        for (Integer label : index.forks)
        {
            Node node = index.map.get(label);
            out.add(VectSource.create4D(node.xpos, node.ypos, node.zpos, node.radius));
        }

        return out;
    }

    public Vects toVectsLeaves()
    {
        Index index = this.index();

        Vects out = new Vects();
        for (Integer label : index.leaves)
        {
            Node node = index.map.get(label);
            out.add(VectSource.create4D(node.xpos, node.ypos, node.zpos, node.radius));
        }

        return out;
    }

    public Neuron copy()
    {
        Neuron out = new Neuron();
        for (Node node : this.nodes)
        {
            out.nodes.add(node.copy());
        }

        return out;
    }

    @Override
    public List<String> getExtensions()
    {
        return Lists.newArrayList(new String[]{"swc"});
    }

    public void rscale(double dx, double dy, double dz)
    {
    }

    public void shift(double dx, double dy, double dz)
    {
        for (Node node : this.nodes)
        {
            node.shift(dx, dy, dz);
        }
    }

    public void scale(double sx, double sy, double sz)
    {
        for (Node node : this.nodes)
        {
            node.scale(sx, sy, sz);
        }
    }

    @Override
    public Box box()
    {
        Box out = null;
        for (Node node : this.nodes)
        {
            Vect v = VectSource.create3D(node.xpos, node.ypos, node.zpos);

            if (out == null)
            {
                out = Box.create(v);
            }
            else
            {
                out = out.union(v);
            }
        }

        return out;
    }
}
