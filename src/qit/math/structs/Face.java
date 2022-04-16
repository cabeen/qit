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

import qit.base.utils.JsonUtils;

public class Face
{
    private Vertex a;
    private Vertex b;
    private Vertex c;

    public Face(Vertex a, Vertex b, Vertex c)
    {
        if (a.equals(b) || a.equals(c) || b.equals(c))
        {
            throw new RuntimeException("Face edges cannot be equal");
        }

        this.a = a;
        this.b = b;
        this.c = c;
    }

    public Face flip()
    {
        return new Face(this.b, this.a, this.c);
    }

    public Edge opposite(Vertex vert)
    {
        if (this.a.equals(vert))
        {
            return new Edge(this.b, this.c);
        }
        else if (this.b.equals(vert))
        {
            return new Edge(this.c, this.a);
        }
        else if (this.c.equals(vert))
        {
            return new Edge(this.a, this.b);
        }

        throw new RuntimeException("face does not contain vertex");
    }

    public boolean connected(Face f)
    {
        return this.contains(f.a) || this.contains(f.b) || this.contains(f.c);
    }

    public boolean connected(Edge e)
    {
        return this.contains(e.getA()) || this.contains(e.getB());
    }

    public boolean contains(Vertex v)
    {
        return v.equals(this.a) || v.equals(this.b) || v.equals(this.c);
    }

    public boolean contains(Edge e)
    {
        return this.contains(e.getA()) && this.contains(e.getB());
    }

    public Vertex getA()
    {
        return this.a;
    }

    public Vertex getB()
    {
        return this.b;
    }

    public Vertex getC()
    {
        return this.c;
    }

    public boolean equals(Object obj)
    {
        if (!(obj instanceof Face))
        {
            return false;
        }

        if (obj == this)
        {
            return true;
        }

        Face f = (Face) obj;

        // Note: equality under a (orientation preserving) rotation of vertices,
        // but not flips
        if (f.a.equals(this.a) && f.b.equals(this.b) && f.c.equals(this.c))
        {
            return true;
        }
        if (f.a.equals(this.b) && f.b.equals(this.c) && f.c.equals(this.a))
        {
            return true;
        }
        if (f.a.equals(this.c) && f.b.equals(this.a) && f.c.equals(this.b))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    public int hashCode()
    {
        // Note: a flipped face will have the same hash code!
        return this.a.hashCode() + this.b.hashCode() + this.c.hashCode();
    }

    public String toString()
    {
        return JsonUtils.encode(this);
    }
}
