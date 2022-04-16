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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BihSearch<E extends Boxable, F extends Boxable>
{
    private static BoundComp COMP = new BoundComp();
    private BihNode root;

    public BihSearch(Set<E> data, int max_depth)
    {
        this.root = this.bih(new ArrayList<E>(data), 0, max_depth);
    }

    public Set<E> intersections(F b)
    {
        return this.intersections(b, this.root);
    }

    private Set<E> intersections(F b, BihNode n)
    {
        Box bbox = b.box();

        if (n.isLeaf)
        {
            if (bbox.intersects(n.box))
            {
                return new HashSet<E>(n.data);
            }
            else
            {
                return new HashSet<E>();
            }
        }
        else if (bbox.range(n.axis).getMax() < n.rightMin)
        {
            return new HashSet<E>(this.intersections(b, n.left));
        }
        else if (bbox.range(n.axis).getMin() > n.leftMax)
        {
            return new HashSet<E>(this.intersections(b, n.right));
        }
        else
        {
            Set<E> ints = new HashSet<E>();
            ints.addAll(this.intersections(b, n.left));
            ints.addAll(this.intersections(b, n.right));
            return ints;
        }
    }

    private BihNode bih(List<E> data, int depth, int max_depth)
    {
        if (data.size() == 0)
        {
            throw new RuntimeException("cannot use an empty list");
        }
        else if (data.size() == 1 || max_depth > 0 && depth >= max_depth)
        {
            Box box = data.get(0).box();
            for (E e : data)
            {
                box = box.union(e.box());
            }

            // Handle leaves
            BihNode node = new BihNode();
            node.isLeaf = true;
            node.data = data;
            node.box = box;
            return node;
        }
        else
        {
            // Select axis based on depth
            int axis = depth % data.get(0).box().dim();
            COMP.axis = axis;

            // Sort the list of boundables
            Collections.sort(data, COMP);

            // Split the sorted list in half
            int sidx = data.size() / 2;
            List<E> left = data.subList(0, sidx);
            List<E> right = data.subList(sidx, data.size());

            Double lmax = null;

            for (Boxable b : left)
            {
                if (lmax == null)
                {
                    lmax = b.box().range(axis).getMax();
                }
                else if (lmax < b.box().range(axis).getMax())
                {
                    lmax = b.box().range(axis).getMax();
                }
            }

            Double rmin = null;
            for (Boxable b : right)
            {
                if (rmin == null)
                {
                    rmin = b.box().range(axis).getMin();
                }
                else if (rmin > b.box().range(axis).getMin())
                {
                    rmin = b.box().range(axis).getMin();
                }
            }

            BihNode node = new BihNode();
            node.leftMax = lmax;
            node.rightMin = rmin;
            node.axis = axis;
            node.isLeaf = false;
            node.left = this.bih(left, depth + 1, max_depth);
            node.right = this.bih(right, depth + 1, max_depth);

            return node;
        }
    }

    public class BihNode
    {
        private double leftMax;
        private double rightMin;
        private int axis;
        private boolean isLeaf;
        private BihNode left;
        private BihNode right;
        private List<E> data;
        private Box box;
    }

    public static class BoundComp implements Comparator<Boxable>
    {
        private int axis;

        public int compare(Boxable a, Boxable b)
        {
            return a.box().range(this.axis).getMin() < b.box().range(this.axis).getMin() ? -1 : a.box().range(this.axis).getMin() > b.box()
                    .range(this.axis).getMin() ? 1 : 0;
        }
    }
}
