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
import qit.base.structs.FiniteIterable;
import qit.math.structs.Vertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** represents a set of objects that all have named vector-valued attributes */
public class AttrMap<E> implements FiniteIterable<E>
{
    // It is important that these are linked hash maps, so they preserve vertex order
    private Map<E, List<Vect>> attrs = Maps.newLinkedHashMap();
    private List<String> names = Lists.newArrayList();
    private List<Vect> protos = Lists.newArrayList();

    public AttrMap<E> copy()
    {
        AttrMap<E> nattr = new AttrMap<E>();

        for (E e : this)
        {
            nattr.add(e);
            for (String name : this.attrs())
            {
                nattr.set(e, name, this.get(e, name));
            }
        }

        return nattr;
    }

    public boolean contains(E e)
    {
        return this.attrs.containsKey(e);
    }

    public void copy(AttrMap<E> from)
    {
        for (String name : from.attrs())
        {
            this.add(name, from.proto(name));

            for (E e : this)
            {
                this.set(e, name, from.get(e, name));
            }
        }
    }

    public void copy(String from, String to)
    {
        this.add(to, this.proto(from));
        for (E e : this)
        {
            this.set(e, to, this.get(e, from));
        }
    }

    public Iterator<E> keys()
    {
        return this.attrs.keySet().iterator();
    }

    public int numAttr()
    {
        return this.names.size();
    }

    public int numElem()
    {
        return this.attrs.size();
    }

    public int size()
    {
        return this.attrs.size();
    }

    public void add(String name, Vect proto)
    {
        if (!this.names.contains(name))
        {
            this.names.add(name);
            this.protos.add(proto.proto());
            for (E e : this.attrs.keySet())
            {
                this.attrs.get(e).add(proto.proto());
            }
        }
        else
        {
            if (this.dim(name) != proto.size())
            {
                throw new RuntimeException("channel mismatch found");
            }
        }
    }

    public void add(E E)
    {
        if (!this.attrs.containsKey(E))
        {
            List<Vect> vals = new ArrayList<Vect>(this.protos.size());
            for (Vect proto : this.protos)
            {
                vals.add(proto.proto());
            }

            this.attrs.put(E, vals);
        }
    }

    public void remove(E elem)
    {
        this.attrs.remove(elem);
    }

    public void remove(String name)
    {
        int idx = this.names.indexOf(name);
        if (idx >= 0)
        {
            this.names.remove(idx);
            this.protos.remove(idx);
            for (E E : this.attrs.keySet())
            {
                this.attrs.get(E).remove(idx);
            }
        }
    }

    public void setAll(String name, Vect val)
    {
        if (this.has(name))
        {
            this.add(name, val.proto());
        }

        for (E e : this)
        {
            this.set(e, name, val);
        }
    }

    public void set(E e, String name, Vect val)
    {
        int idx = this.names.indexOf(name);
        if (idx >= 0)
        {
            if (!this.attrs.containsKey(e))
            {
                this.add(e);
            }

            List<Vect> vals = this.attrs.get(e);
            Vect vect = vals.get(idx);
            if (vect.size() != val.size())
            {
                this.remove(name);
            }

            vect.set(val);
        }
        else
        {
            this.add(name, val);
            this.set(e, name, val);
        }
    }

    public Set<String> attrs()
    {
        return new HashSet<>(this.names);
    }

    public boolean has(String name)
    {
        return this.names.contains(name);
    }

    public Vect get(E E, String name)
    {
        int idx = this.names.indexOf(name);
        if (idx < 0)
        {
            throw new RuntimeException("attribute " + name + " doesn't exist");
        }

        List<Vect> vals = this.attrs.get(E);
        if (vals == null)
        {
            throw new RuntimeException("element not found in attribute map: " + E.toString());
        }

        return vals.get(idx);
    }

    public void rename(String from, String to)
    {
        int idx = this.names.indexOf(from);
        if (idx >= 0)
        {
            this.names.set(idx, to);
        }
        else
        {
            throw new RuntimeException("invalid attribute name");
        }
    }

    public int dim(String name)
    {
        return this.protos.get(this.names.indexOf(name)).size();
    }

    public Vect proto(String name)
    {
        return this.protos.get(this.names.indexOf(name)).proto();
    }

    public FiniteIterable<Vect> get(String name)
    {
        final String fname = name;
        return new FiniteIterable<Vect>()
        {
            public Iterator<Vect> iterator()
            {
                return new AttrIterator(fname);
            }

            public int size()
            {
                return AttrMap.this.attrs.size();
            }
        };
    }

    public Vects getAll(String name)
    {
        Vects out = new Vects();
        for (Vect v : this.get(name))
        {
            out.add(v);
        }
        return out;
    }

    private class AttrIterator implements Iterator<Vect>
    {
        String name;
        Iterator<E> E = AttrMap.this.attrs.keySet().iterator();

        public AttrIterator(String name)
        {
            this.name = name;
        }

        public boolean hasNext()
        {
            return this.E.hasNext();
        }

        public Vect next()
        {
            int idx = AttrMap.this.names.indexOf(this.name);
            return AttrMap.this.attrs.get(this.E.next()).get(idx);
        }

        public void remove()
        {
            throw new RuntimeException("Method not supported");
        }
    }

    public Iterator<E> iterator()
    {
        return this.attrs.keySet().iterator();
    }
}
