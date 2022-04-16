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

package qit.data.utils.mesh;

import qit.base.Global;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.math.structs.Face;
import qit.math.structs.Intersectable;
import qit.math.structs.Line;
import qit.math.structs.LineIntersection;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.List;

public class MeshCropSolid
{
    private Mesh input;
    private Intersectable solid;
    private String label;
    private boolean invert;
    private Mesh output;
    
    public MeshCropSolid()
    {
        
    }
    
    private MeshCropSolid clear()
    {
        this.output = null;
        return this;
    }
    
    public MeshCropSolid withInput(Mesh v)
    {
        this.input = v;
        return this.clear();
    }

    
    public MeshCropSolid withSolid(Intersectable v)
    {
        this.solid = v;
        return this.clear();
    }

    public MeshCropSolid withInvert(boolean v)
    {
        this.invert = v;
        return this.clear();
    }
    
    public MeshCropSolid run()
    {
        Global.assume(this.input != null, "input mesh is required");
        Global.assume(this.solid != null, "selector is required");
        
        Mesh out = new Mesh();
        for (String attr : input.vattr.attrs())
        {
            out.vattr.add(attr, input.vattr.proto(attr));
        }

        for (Face face : input.graph.faces())
        {
            Vertex a = face.getA();
            Vertex b = face.getB();
            Vertex c = face.getC();
            Vect va = input.vattr.get(a, Mesh.COORD);
            Vect vb = input.vattr.get(b, Mesh.COORD);
            Vect vc = input.vattr.get(c, Mesh.COORD);

            boolean sa = solid.contains(va) ^ invert;
            boolean sb = solid.contains(vb) ^ invert;
            boolean sc = solid.contains(vc) ^ invert;
            int count = (sa ? 1 : 0) + (sb ? 1 : 0) + (sc ? 1 : 0);

            if (count == 0)
            {
                continue;
            }
            else if (count == 1 || count == 2)
            {
                // reduce these to a standard form
                if (count == 2)
                {
                    sa = !sa;
                    sb = !sb;
                    sc = !sc;
                }
                Vertex ra = sa ? a : sb ? b : c;
                Vertex rb = sa ? b : sb ? c : a;
                Vertex rc = sa ? c : sb ? a : b;

                Vect rva = sa ? va : sb ? vb : vc;
                Vect rvb = sa ? vb : sb ? vc : va;
                Vect rvc = sa ? vc : sb ? va : vb;

                List<LineIntersection> rvab = solid.intersect(Line.fromTwoPoints(rva, rvb));
                List<LineIntersection> rvac = solid.intersect(Line.fromTwoPoints(rva, rvc));

                if (rvab.size() != 2 || rvac.size() != 2)
                {
                    out.graph.add(face);
                }
                else
                {
                    Vect nvab = MathUtils.closed(rvab.get(0).getAlpha(), 0, 1) ? rvab.get(0).getPoint() : rvab.get(1).getPoint();
                    Vect nvac = MathUtils.closed(rvac.get(0).getAlpha(), 0, 1) ? rvac.get(0).getPoint() : rvac.get(1).getPoint();

                    Vertex nab = input.graph.addVertex();
                    Vertex nac = input.graph.addVertex();

                    out.vattr.set(nab, Mesh.COORD, nvab);
                    out.vattr.set(nac, Mesh.COORD, nvac);

                    if (count == 1)
                    {
                        for (String attr : input.vattr.attrs())
                        {
                            if (!Mesh.COORD.equals(attr))
                            {
                                out.vattr.set(nab, attr, input.vattr.get(ra, attr));
                                out.vattr.set(nac, attr, input.vattr.get(ra, attr));
                            }
                        }

                        out.graph.add(new Face(ra, nab, nac));
                    }
                    else if (count == 2)
                    {
                        for (String attr : input.vattr.attrs())
                        {
                            if (!Mesh.COORD.equals(attr))
                            {
                                out.vattr.set(nab, attr, input.vattr.get(rb, attr));
                                out.vattr.set(nac, attr, input.vattr.get(rc, attr));
                            }
                        }

                        out.graph.add(new Face(nab, rb, rc));
                        out.graph.add(new Face(nab, rc, nac));
                    }
                }
            }
            else
            {
                out.graph.add(face);
            }
        }

        for (Vertex vert : out.graph.verts())
        {
            if (input.vattr.contains(vert))
            {
                for (String attr : out.vattr.attrs())
                {
                    out.vattr.set(vert, attr, input.vattr.get(vert, attr));
                }
            }
        }

        this.output =  out;
        
        return this;
    }
    
    public Mesh getOutput()
    {
        if (this.output == null)
        {
            run();
        }
        
        return this.output;
    }
}
