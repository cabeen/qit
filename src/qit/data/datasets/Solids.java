/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.datasets;

import com.google.common.collect.Lists;
import qit.base.JsonDataset;
import qit.base.structs.Indexed;
import qit.base.structs.Pair;
import qit.math.structs.Box;
import qit.math.structs.Boxable;
import qit.math.structs.Intersectable;
import qit.math.structs.Line;
import qit.math.structs.LineIntersection;
import qit.math.structs.Plane;
import qit.math.structs.Sphere;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class Solids extends JsonDataset implements Intersectable, Boxable, Indexed<Intersectable>
{
    private List<Sphere> spheres = Lists.newArrayList();
    private List<Box> boxes = Lists.newArrayList();
    private List<Plane> planes = Lists.newArrayList();

    public static Solids of(Sphere sphere)
    {
        Solids out = new Solids();
        out.addSphere(sphere);
        return out;
    }

    public static Solids of(Box box)
    {
        Solids out = new Solids();
        out.addBox(box);
        return out;
    }

    public static Solids of(Plane plane)
    {
        Solids out = new Solids();
        out.addPlane(plane);
        return out;
    }

    public Solids()
    {
        super();
    }

    public Solids add(Solids s)
    {
        for (Box box : s.boxes)
        {
            this.addBox(box.copy());
        }

        for (Sphere sphere : s.spheres)
        {
            this.addSphere(sphere.copy());
        }

        for (Plane plane : s.planes)
        {
            this.addPlane(plane.copy());
        }

        return this;
    }

    public Solids addSpheres(Iterable<Sphere> ss)
    {
        for (Sphere s : ss)
        {
            this.spheres.add(s);
        }

        return this;
    }

    public Solids addSphere(Sphere s)
    {
        this.spheres.add(s);
        return this;
    }

    public Solids removeSphere(int i)
    {
        this.spheres.remove(i);
        return this;
    }

    public Solids removeSpheres()
    {
        this.spheres.clear();
        return this;
    }

    public Solids setSphere(int i, Sphere s)
    {
        this.spheres.set(i, s);
        return this;
    }

    public Sphere getSphere(int i)
    {
        return this.spheres.get(i);
    }

    public int numSpheres()
    {
        return this.spheres.size();
    }

    public List<Sample> samples(Sampling sampling)
    {
        List<Sample> out = Lists.newArrayList();
        for (int i = 0; i < this.numSpheres(); i++)
        {
            Sphere sphere = this.getSphere(i);
            for (Sample sample : sampling.iterateBox(sphere.box()))
            {
                if (sphere.contains(sampling.world(sample)))
                {
                    out.add(sample);
                }
            }
        }

        for (int i = 0; i < this.numBoxes(); i++)
        {
            Box box = this.getBox(i);
            for (Sample sample : sampling.iterateBox(box))
            {
                if (box.contains(sampling.world(sample)))
                {
                    out.add(sample);
                }
            }
        }

        return out;
    }

    public List<Integer> selectSpheres(Vect point)
    {
        List<Integer> out = Lists.newArrayList();
        for (int i = 0; i < this.spheres.size(); i++)
        {
            Sphere sphere = this.spheres.get(i);
            if (sphere.contains(point))
            {
                out.add(i);
            }
        }
        return out;
    }

    public List<Pair<Integer, List<LineIntersection>>> selectSpheres(Line line)
    {
        List<Pair<Integer, List<LineIntersection>>> out = Lists.newArrayList();
        for (int i = 0; i < this.spheres.size(); i++)
        {
            Sphere sphere = this.spheres.get(i);
            List<LineIntersection> is = sphere.intersect(line);
            if (is.size() > 0)
            {
                out.add(Pair.of(i, is));
            }
        }
        return out;
    }

    public Solids addBoxes(Iterable<Box> bs)
    {
        for (Box b : bs)
        {
            this.boxes.add(b);
        }

        return this;
    }

    public Solids addBox(Box s)
    {
        this.boxes.add(s);
        return this;
    }

    public Solids removeBox(int i)
    {
        this.boxes.remove(i);
        return this;
    }

    public Box getBox(int i)
    {
        return this.boxes.get(i);
    }

    public Solids setBox(int i, Box b)
    {
        this.boxes.set(i, b);
        return this;
    }

    public int numBoxes()
    {
        return this.boxes.size();
    }

    public List<Integer> selectBoxes(Vect point)
    {
        List<Integer> out = Lists.newArrayList();
        for (int i = 0; i < this.boxes.size(); i++)
        {
            Box box = this.boxes.get(i);
            if (box.contains(point))
            {
                out.add(i);
            }
        }
        return out;
    }

    public List<Pair<Integer, List<LineIntersection>>> selectBoxes(Line line)
    {
        List<Pair<Integer, List<LineIntersection>>> out = Lists.newArrayList();
        for (int i = 0; i < this.boxes.size(); i++)
        {
            Box box = this.boxes.get(i);
            List<LineIntersection> is = box.intersect(line);
            if (is.size() > 0)
            {
                out.add(Pair.of(i, is));
            }
        }
        return out;
    }

    public Solids removeBoxes()
    {
        this.boxes.clear();
        return this;
    }

    public Solids addPlanes(Iterable<Plane> bs)
    {
        for (Plane b : bs)
        {
            this.planes.add(b);
        }

        return this;
    }

    public Solids addPlane(Plane s)
    {
        this.planes.add(s);
        return this;
    }

    public Solids removePlane(int i)
    {
        this.planes.remove(i);
        return this;
    }

    public Plane getPlane(int i)
    {
        return this.planes.get(i);
    }

    public Solids setPlane(int i, Plane b)
    {
        this.planes.set(i, b);
        return this;
    }

    public int numPlanes()
    {
        return this.planes.size();
    }

    public List<Integer> selectPlanes(Vect point)
    {
        List<Integer> out = Lists.newArrayList();
        for (int i = 0; i < this.planes.size(); i++)
        {
            Plane box = this.planes.get(i);
            if (box.contains(point))
            {
                out.add(i);
            }
        }
        return out;
    }

    public List<Pair<Integer, List<LineIntersection>>> selectPlanes(Line line)
    {
        List<Pair<Integer, List<LineIntersection>>> out = Lists.newArrayList();
        for (int i = 0; i < this.planes.size(); i++)
        {
            Plane box = this.planes.get(i);
            List<LineIntersection> is = box.intersect(line);
            if (is.size() > 0)
            {
                out.add(Pair.of(i, is));
            }
        }
        return out;
    }

    public Solids removePlanes()
    {
        this.planes.clear();
        return this;
    }

    public Solids remove(int idx)
    {
        int ns = this.spheres.size();
        int nb = this.boxes.size();

        if (idx < ns)
        {
            this.spheres.remove(idx);
        }
        else if (idx < (ns + nb))
        {
            this.boxes.remove(idx - ns);
        }
        else
        {
            this.planes.remove(idx - ns - nb);
        }

        return this;
    }


    public Solids removeAll()
    {
        this.spheres.clear();
        this.boxes.clear();
        this.planes.clear();
        return this;
    }

    @Override
    public Intersectable get(int idx)
    {
        int ns = this.spheres.size();
        int nb = this.boxes.size();

        if (idx < ns)
        {
            return this.spheres.get(idx);
        }
        else if (idx < (ns + nb))
        {
            return this.boxes.get(idx - ns);
        }
        else
        {
            return this.planes.get(idx - ns - nb);
        }
    }

    @Override
    public int size()
    {
        return this.spheres.size() + this.boxes.size() + this.planes.size();
    }

    public boolean and(Vects vects)
    {
        if (this.size() == 0)
        {
            return false;
        }

        boolean[] pass = new boolean[this.size()];

        for (int i = 0; i < vects.size(); i++)
        {
            for (int j = 0; j < this.size(); j++)
            {
                pass[j] |= this.get(j).contains(vects.get(i));
            }
        }

        boolean out = true;
        for (int j = 0; j < this.size(); j++)
        {
            out &= pass[j];
        }

        return out;
    }

    public List<Integer> select(Vect point)
    {
        List<Integer> out = Lists.newArrayList();
        out.addAll(this.selectSpheres(point));
        for (Integer i : this.selectBoxes(point))
        {
            out.add(i + this.spheres.size());
        }
        for (Integer i : this.selectPlanes(point))
        {
            out.add(i + this.spheres.size() + this.boxes.size());
        }
        return out;
    }

    public List<Pair<Integer, List<LineIntersection>>> select(Line line)
    {
        List<Pair<Integer, List<LineIntersection>>> out = Lists.newArrayList();
        out.addAll(this.selectSpheres(line));
        for (Pair<Integer, List<LineIntersection>> i : this.selectBoxes(line))
        {
            i.a += this.spheres.size();
            out.add(i);
        }
        for (Pair<Integer, List<LineIntersection>> i : this.selectPlanes(line))
        {
            i.a += this.spheres.size() + this.boxes.size();
            out.add(i);
        }
        return out;
    }

    @Override
    public Iterator<Intersectable> iterator()
    {
        return new Iterator<Intersectable>()
        {
            private int cursor = 0;

            @Override
            public boolean hasNext()
            {
                return this.cursor < Solids.this.size();
            }

            @Override
            public Intersectable next()
            {
                if (this.hasNext())
                {
                    int current = this.cursor;
                    this.cursor++;
                    return Solids.this.get(current);
                }
                throw new NoSuchElementException();
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public Solids copy()
    {
        Solids out = new Solids();
        for (Sphere s : this.spheres)
        {
            out.addSphere(s.copy());
        }
        for (Box b : this.boxes)
        {
            out.addBox(b.copy());
        }
        for (Plane p : this.planes)
        {
            out.addPlane(p.copy());
        }
        return out;
    }

    @Override
    public Box box()
    {
        Box out = null;
        for (Sphere s : this.spheres)
        {
            if (out == null)
            {
                out = s.box();
            }
            else
            {
                out = out.union(s.box());
            }
        }

        for (Box b : this.boxes)
        {
            if (out == null)
            {
                out = b;
            }
            else
            {
                out = out.union(b);
            }
        }

        // note: planes are not boundable, so skip them

        return out;
    }

    public int label(Vect point)
    {
        int idx = 1;

        for (Sphere s : this.spheres)
        {
            if (s.contains(point))
            {
                return idx;
            }

            idx += 1;
        }

        for (Box b : this.boxes)
        {
            if (b.contains(point))
            {
                return idx;
            }

            idx += 1;
        }

        for (Plane p : this.planes)
        {
            if (p.contains(point))
            {
                return idx;
            }

            idx += 1;
        }

        return 0;
    }

    @Override
    public boolean contains(Vect point)
    {
        for (Sphere s : this.spheres)
        {
            if (s.contains(point))
            {
                return true;
            }
        }

        for (Box b : this.boxes)
        {
            if (b.contains(point))
            {
                return true;
            }
        }

        for (Plane p : this.planes)
        {
            if (p.contains(point))
            {
                return true;
            }
        }

        return false;
    }

    public boolean containsAll(Vect point)
    {
        boolean out = true;

        for (int i = 0; i < this.size(); i++)
        {
            out &= this.get(i).contains(point);
        }

        return out;
    }

    public boolean contains(Vects points)
    {
        for (Vect point : points)
        {
            for (Sphere s : this.spheres)
            {
                if (s.contains(point))
                {
                    return true;
                }
            }

            for (Box b : this.boxes)
            {
                if (b.contains(point))
                {
                    return true;
                }
            }

            for (Plane p : this.planes)
            {
                if (p.contains(point))
                {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<LineIntersection> intersect(Line line)
    {
        List<LineIntersection> out = Lists.newArrayList();

        for (Sphere s : this.spheres)
        {
            out.addAll(s.intersect(line));
        }

        for (Box b : this.boxes)
        {
            out.addAll(b.intersect(line));
        }

        for (Plane p : this.planes)
        {
            out.addAll(p.intersect(line));
        }

        return out;
    }

    public Vects sample(Integer num)
    {
        Vects out = new Vects();

        for (Sphere s : this.spheres)
        {
            out.addAll(s.sample(num));
        }

        for (Box b : this.boxes)
        {
            out.addAll(b.sample(num));
        }

        // note planes are not boundable, so skip them

        return out;
    }

    public static Solids read(String fn) throws IOException
    {
        return JsonDataset.read(Solids.class, fn);
    }
}