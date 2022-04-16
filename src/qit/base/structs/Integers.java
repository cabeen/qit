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

package qit.base.structs;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.utils.JsonUtils;
import qit.data.datasets.Vects;
import qit.math.utils.MathUtils;

/** a structure for indexing into multi-dimensional arrays */
public class Integers implements Iterable<Integer>
{
    public static Integers dim(int n)
    {
        return new Integers(new int[n]);
    }

    private final int[] vals;

    public Integers(int[] v)
    {
        Global.assume(v.length > 0, "empty array found");

        this.vals = MathUtils.copy(v);
    }

    public Integers(List<Integer> vs)
    {
        Global.assume(vs.size() > 0, "empty array found");

        this.vals = new int[vs.size()];
        for (int i = 0; i < vs.size(); i++)
        {
            this.vals[i] = vs.get(i);
        }
    }

    public Integers(int a)
    {
        this.vals = new int[] { a };
    }

    public Integers(int a, int b)
    {
        this.vals = new int[] { a, b };
    }

    public Integers(int a, int b, int c)
    {
        this.vals = new int[] { a, b, c };
    }

    public Integers(int a, int b, int c, int d)
    {
        this.vals = new int[] { a, b, c, d };
    }

    public Integers times(int v)
    {
        int[] out = new int[this.vals.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = v * this.vals[i];
        }

        return new Integers(out);
    }

    public Integers plus(int v)
    {
        int[] out = new int[this.vals.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = this.vals[i] + v;
        }

        return new Integers(out);
    }

    public Integers times(Integers v)
    {
        int[] out = new int[this.vals.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = v.vals[i] * this.vals[i];
        }

        return new Integers(out);
    }

    public Integers plus(Integers v)
    {
        int[] out = new int[this.vals.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = this.vals[i] + v.vals[i];
        }

        return new Integers(out);
    }

    public int getI()
    {
        return this.vals[0];
    }

    public int getJ()
    {
        return this.vals[1];
    }

    public int getK()
    {
        return this.vals[2];
    }

    public int getL()
    {
        return this.vals[3];
    }

    public int get(int idx)
    {
        return this.vals[idx];
    }

    public int size()
    {
        return this.vals.length;
    }

    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (obj instanceof Integers)
        {
            Integers test = (Integers) obj;

            if (test.size() != this.size())
            {
                return false;
            }

            for (int i = 0; i < test.size(); i++)
            {
                if (test.get(i) != this.get(i))
                {
                    return false;
                }
            }

            return true;
        }
        else
        {
            return false;
        }
    }

    public Integers copy()
    {
        return new Integers(this.vals);
    }

    public String toString()
    {
        return JsonUtils.encode(this);
    }

    public int hashCode()
    {
        int sum = 0;
        for (int i = 0; i < this.size(); i++)
        {
            sum += this.get(i);
        }

        return sum;
    }

    public Set<Integer> unique()
    {
        Set<Integer> out = Sets.newHashSet();
        for (int i = 0; i < this.size(); i++)
        {
            out.add(this.get(i));
        }

        return out;
    }

    public Map<Integer,Integer> counts()
    {
        Map<Integer,Integer> out = Maps.newHashMap();
        for (int i = 0; i < this.size(); i++)
        {
            int v = this.get(i);
            if (!out.containsKey(v))
            {
                out.put(v, 0);
            }

            out.put(v, out.get(v) + 1);
        }

        return out;
    }

    public Integers reverse()
    {
        int[] out = new int[this.vals.length];
        for (int i = 0; i < out.length; i++)
        {
            out[i] = this.vals[this.vals.length - 1 - i];
        }

        return new Integers(out);
    }

    public Iterator<Integer> iterator()
    {
        return new IntegerIterator();
    }

    private class IntegerIterator implements Iterator<Integer>
    {
        private int idx = 0;

        public boolean hasNext()
        {
            return this.idx < Integers.this.size();
        }

        public Integer next()
        {
            return Integers.this.get(this.idx++);
        }

        public void remove()
        {
            throw new UnsupportedOperationException("Operation not supported for Integer iterators");
        }
    }

    public Integers sub(int start, int end)
    {
        int dim = end - start;
        int[] out = new int[dim];
        for (int i = 0; i < dim; i++)
        {
            out[i] = this.get(i + start);
        }

        return new Integers(out);
    }

    public static Integers read(String fn) throws IOException
    {
        Vects v = Vects.read(fn);

        int[] out = new int[v.size()];
        for (int i = 0; i < v.size(); i++)
        {
            out[i] = MathUtils.round(v.get(0).get(0));
        }

        return new Integers(out);
    }
}
