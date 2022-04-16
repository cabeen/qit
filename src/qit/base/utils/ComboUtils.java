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

package qit.base.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import qit.base.Logging;
import qit.math.structs.MixedRadix;

/**
 * utilities for combinatorial things
 */
public class ComboUtils
{
    public static List<Map<String, String>> product(Map<String, List<String>> input)
    {
        int n = input.size();
        int[] counts = new int[n];
        Map<String, Integer> indexer = Maps.newHashMap();
        int idx = 0;
        for (String var : input.keySet())
        {
            counts[idx] = input.get(var).size();
            indexer.put(var, idx);
            idx++;
        }

        List<Map<String, String>> out = Lists.newArrayList();
        MixedRadix mr = new MixedRadix(counts);
        for (int i = 0; i < mr.size(); i++)
        {
            int[] midx = mr.getValue(i);
            Map<String, String> env = Maps.newHashMap();
            for (String var : input.keySet())
            {
                env.put(var, input.get(var).get(midx[indexer.get(var)]));
            }
            out.add(env);
        }

        return out;
    }

    public static List<Map<String, String>> tuples(Map<String, List<String>> input)
    {
        Integer total = null;
        for (String var : input.keySet())
        {
            if (total == null)
            {
                total = input.get(var).size();
            }
            else if (total != input.get(var).size())
            {
                Logging.error("variables must have equal counts to make tuples");
            }
        }

        List<Map<String, String>> out = Lists.newArrayList();
        for (int i = 0; i < total; i++)
        {
            Map<String, String> env = Maps.newHashMap();
            for (String var : input.keySet())
            {
                env.put(var, input.get(var).get(i));
            }
            out.add(env);
        }
        return out;
    }

    public static List<Map<String, String>> groups(Map<String, List<String>> keys, boolean product)
    {
        if (product)
        {
            return product(keys);
        }
        else
        {
            return tuples(keys);
        }
    }

    public static <K> List<List<K>> subsets(List<K> items, int size)
    {
        // yes i know this is an expensive way to do this
        List<List<K>> out = Lists.newArrayList();
        for (List<K> subset : subsets(items))
        {
            if (subset.size() == size)
            {
                out.add(subset);
            }
        }
        return out;
    }

    public static <K> List<List<K>> subsets(List<K> items)
    {
        List<List<K>> out = Lists.newArrayList();
        if (items.isEmpty())
        {
            List<K> empty = Lists.newArrayList();
            out.add(empty);
            return out;
        }

        List<K> list = Lists.newArrayList(items);
        K head = list.get(0);
        List<K> rest = Lists.newArrayList(list.subList(1, list.size()));
        for (List<K> set : subsets(rest))
        {
            List<K> newList = Lists.newArrayList();
            newList.add(head);
            newList.addAll(set);
            out.add(newList);
            out.add(set);
        }
        return out;
    }

    public static <K> List<List<K>> permutations(List<K> items)
    {
        PermutationInner<K> p = new PermutationInner<K>();
        p.run(new ArrayList<K>(), items);
        return p.output;
    }

    // based on http://introcs.cs.princeton.edu/java/23recursion/Permutations.java.html
    private static class PermutationInner<K>
    {
        private List<List<K>> output = Lists.newArrayList();

        private void run(List<K> left, List<K> right)
        {
            int size = right.size();
            if (size == 0)
            {
                output.add(left);
            }
            else
            {
                for (int i = 0; i < size; i++)
                {
                    List<K> nextLeft = Lists.newArrayList();
                    List<K> nextRight = Lists.newArrayList();

                    nextLeft.addAll(left);
                    nextLeft.add(right.get(i));

                    nextRight.addAll(right.subList(0, i));
                    nextRight.addAll(right.subList(i + 1, size));

                    this.run(nextLeft, nextRight);
                }
            }
        }
    }

    public static void main(String[] args)
    {
        String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        int num = 4;
        List<String> items = Lists.newArrayList();
        for (int i = 0; i < num; i++)
        {
            items.add(alphabet.substring(i, i + 1));
        }

        List<List<String>> out = permutations(items);

        for (List<String> p : out)
        {
            System.out.println(StringUtils.join(p, ""));
        }
    }
}