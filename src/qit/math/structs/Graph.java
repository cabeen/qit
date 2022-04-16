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

package qit.math.structs;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.Set;

public class Graph
{
    private int nextid = 0;
    private Set<Vertex> vertices = Sets.newHashSet();
    private Multimap<Vertex, Vertex> adjacency = HashMultimap.create();
    private int edges = 0;

    public Graph()
    {
    }

    public void addVertex(Vertex n)
    {
        if (this.vertices.contains(n))
        {
            this.nextid = Math.max(this.nextid, n.id() + 1);
            this.vertices.add(n);
        }
    }

    public Vertex addVertex()
    {
        Vertex vertex = new Vertex(this.nextid++);
        this.vertices.add(vertex);
        return vertex;
    }

    public void addEdge(Edge e)
    {
        this.addEdge(e.getA(), e.getB());
    }

    public Edge addEdge(Vertex a, Vertex b)
    {
        this.addVertex(a);
        this.addVertex(b);

        if (!this.adjacency.containsEntry(a, b))
        {
            this.adjacency.put(a, b);
            this.adjacency.put(b, a);

            this.edges += 1;
        }

        return new Edge(a, b);
    }

    public boolean hasEdge(Vertex a, Vertex b)
    {
        return this.adjacency.get(a).contains(b);
    }

    public Edge getEdge(Vertex a, Vertex b)
    {
        return new Edge(a, b);
    }

    public boolean hasEdge(Edge e)
    {
        return this.hasEdge(e.getA(), e.getB());
    }

    public boolean hasVertex(Vertex n)
    {
        return this.vertices.contains(n);
    }

    public void removeVertex(Vertex v)
    {
        for (Vertex n : this.adjacent(v))
        {
            this.adjacency.remove(v, n);
            this.adjacency.remove(n, v);
        }
        this.vertices.remove(v);
    }

    public void removeEdge(Edge e)
    {
        this.adjacency.remove(e.getA(), e.getB());
        this.adjacency.remove(e.getB(), e.getA());
    }

    public Set<Edge> edges()
    {
        // Assume that Edge.equals handles direction
        Set<Edge> out = Sets.newHashSet();
        for (Vertex a : this.adjacency.keySet())
        {
            for (Vertex b : this.adjacent(a))
            {
                out.add(new Edge(a, b));
            }
        }

        return out;
    }

    public ImmutableSet<Vertex> vertices()
    {
        return ImmutableSet.copyOf(this.vertices);
    }

    public Set<Edge> star(Vertex a)
    {
        Set<Edge> star = Sets.newHashSet();
        for (Vertex b : this.adjacent(a))
        {
            star.add(new Edge(a, b));
        }

        return star;
    }

    public ImmutableSet<Vertex> adjacent(Vertex n)
    {
        return ImmutableSet.copyOf(this.adjacency.get(n));
    }

    public int getNumVertexs()
    {
        return this.vertices.size();
    }

    public int getNumEdges()
    {
        return this.edges;
    }
}
