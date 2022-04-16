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

package qit.data.modules.curves;

import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.MeshUtils;
import qit.data.utils.curves.CurvesFunctionApply;
import qit.math.structs.Face;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.source.VectFunctionSource;
import qit.math.utils.MathUtils;

@ModuleDescription("Create a mesh representing 3D tubes based on the input curves")
@ModuleAuthor("Ryan Cabeen")
public class CurvesTubes implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an affine xfm")
    public Affine affine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an inverse affine xfm")
    public Affine invaffine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply a deformation xfm")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("use per-vertex orientation coloring")
    public boolean color = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the color wash")
    public Double wash = 0.2;

    @ModuleParameter
    @ModuleDescription("the default tube thickness")
    public Double dthick = 0.15;

    @ModuleParameter
    @ModuleDescription("the tube thickness factor (scales the thickness attribute, it it is present)")
    public Double fthick = 1.0;

    @ModuleParameter
    @ModuleDescription("use the given thickness attribute")
    public String thick = Curves.THICKNESS;

    @ModuleParameter
    @ModuleDescription("the tube resolution")
    public int resolution = 5;

    @ModuleParameter
    @ModuleDescription("skip color attributes")
    public boolean noColor = false;

    @ModuleParameter
    @ModuleDescription("skip thickness attributes")
    public boolean noThick = false;

    @ModuleParameter
    @ModuleDescription("create smooth tube caps (the default will use a separate disk for tube caps)")
    public boolean smooth = false;

    @ModuleOutput
    @ModuleDescription("the output tubes")
    public Mesh output;

    private transient VectFunction xfm;

    private CurvesTubes clear()
    {
        this.xfm = null;
        if (this.affine != null)
        {
            this.xfm = this.affine;
        }
        else if (this.invaffine != null)
        {
            this.xfm = this.invaffine.inv();
        }
        else if (this.deform != null)
        {
            this.xfm = this.deform;
        }
        else
        {
            this.xfm = VectFunctionSource.identity(3);
        }

        this.output = null;
        return this;
    }

    @Override
    public CurvesTubes run()
    {
        this.clear();

        if (!this.input.has(Curves.TANGENT))
        {
            CurvesUtils.attrSetTangent(this.input);
        }

        if (this.color)
        {
            CurvesUtils.attrSetColor(this.input);

            if (this.wash != null)
            {
                VectFunction coloring = VectFunctionSource.wash(this.wash);
                new CurvesFunctionApply().withCurves(this.input).withFunction(coloring).withInput(Curves.COLOR).withOutput(Curves.COLOR).run();
            }
        }

        Mesh out = null;

        Global.assume(this.resolution >= 3, "invalid number of samples");
        for (Curve curve : this.input)
        {
            Mesh tube = single(curve);
            out = MeshUtils.mbind(out, tube);
        }

        this.output = out;

        return this;
    }

    public Mesh single(Curve curve)
    {
        return single(curve, null, null);
    }

    public Mesh single(Curve curve, Integer start, Integer end)
    {
        Global.assume(curve.has(Curves.TANGENT), "tangent attr required");
        if (this.xfm == null)
        {
            this.clear();
        }

        boolean hasColor = this.noColor ? false : curve.has(Curves.COLOR);
        boolean hasThick = this.noThick ? false : curve.has(this.thick);

        Mesh mesh = new Mesh();
        mesh.vattr.add(Mesh.NORMAL, VectSource.create3D());
        mesh.vattr.add(Mesh.TEXTURE, VectSource.create2D());

        int count = 0;
        Vertex head = new Vertex(count++);
        Vertex tail = new Vertex(count++);

        Vect headNormal = curve.getHead(Curves.TANGENT).normalize().times(-1);
        Vect tailNormal = curve.getTail(Curves.TANGENT).normalize();

        mesh.vattr.set(head, Mesh.COORD, this.xfm.apply(curve.getHead()));
        mesh.vattr.set(head, Mesh.NORMAL, headNormal);
        mesh.vattr.set(head, Mesh.TEXTURE, VectSource.create2D(0, 0));

        if (hasColor)
        {
            mesh.vattr.set(head, Mesh.COLOR, curve.getHead(Curves.COLOR));
        }

        mesh.vattr.set(tail, Mesh.COORD, this.xfm.apply(curve.getTail()));
        mesh.vattr.set(tail, Mesh.NORMAL, tailNormal);
        mesh.vattr.set(tail, Mesh.TEXTURE, VectSource.create2D(0, 1));

        if (hasColor)
        {
            mesh.vattr.set(tail, Mesh.COLOR, curve.getTail(Curves.COLOR));
        }

        Vect pup = null;
        Vertex[] rim = null;
        Vertex[] prim = null;

        if (start == null)
        {
            start = 0;
        }

        if (end == null)
        {
            end = curve.size();
        }

        for (int j = start; j < end; j++)
        {
            Vect ocv = curve.get(j);
            Vect otv = curve.get(Curves.TANGENT, j).normalize();

            Vect cv = this.xfm.apply(ocv);
            Vect tv = this.xfm.apply(ocv.plus(otv)).minus(cv).normalize();

            Vect color = hasColor ? curve.get(Curves.COLOR, j) : null;
            double thick = hasThick ? this.fthick * curve.get(this.thick, j).get(0) : this.dthick;

            Vect xv = null;

            if (pup == null)
            {
                xv = tv.perp();
                pup = xv;
            }
            else
            {
                Vect nup = pup.minus(tv.times(pup.dot(tv)));
                if (MathUtils.zero(nup.norm()))
                {
                    xv = tv.perp();
                }
                else
                {
                    xv = nup.normalize();
                }
                pup = nup;
            }

            xv.normalizeEquals();
            Vect yv = tv.cross(xv).normalize();

            rim = new Vertex[this.resolution];
            for (int i = 0; i < this.resolution; i++)
            {
                Vertex v = new Vertex(count++);
                rim[i] = v;

                double x = Math.cos(2 * Math.PI * i / this.resolution);
                double y = Math.sin(2 * Math.PI * i / this.resolution);

                Vect r = xv.times(x).plus(yv.times(y));
                Vect pv = cv.plus(r.times(thick));

                double ucoord = i / (double) (this.resolution - 1);
                double vcoord = j / (double) (curve.size() - 1);
                Vect uv = VectSource.create2D(ucoord, vcoord);

                mesh.vattr.set(v, Mesh.COORD, pv);
                mesh.vattr.set(v, Mesh.NORMAL, r);
                mesh.vattr.set(v, Mesh.TEXTURE, uv);

                if (hasColor)
                {
                    mesh.vattr.set(v, Mesh.COLOR, color);
                }
            }

            if (j == 0)
            {
                Vertex[] nrim = rim;

                if (!smooth)
                {
                    nrim = new Vertex[this.resolution];
                    for (int i = 0; i < this.resolution; i++)
                    {
                        Vertex v = new Vertex(count++);
                        nrim[i] = v;

                        mesh.vattr.set(v, Mesh.COORD, mesh.vattr.get(rim[i], Mesh.COORD).copy());
                        mesh.vattr.set(v, Mesh.NORMAL, headNormal.copy());
                        mesh.vattr.set(v, Mesh.TEXTURE, mesh.vattr.get(rim[i], Mesh.TEXTURE).copy());

                        if (hasColor)
                        {
                            mesh.vattr.set(v, Mesh.COLOR, mesh.vattr.get(rim[i], Mesh.COLOR).copy());
                        }
                    }
                }

                // make the head cap
                for (int i = 0; i < this.resolution; i++)
                {
                    Vertex curr = nrim[i];
                    Vertex prev = i == 0 ? nrim[this.resolution - 1] : nrim[i - 1];
                    mesh.graph.add(new Face(curr, prev, head));
                }
            }
            else
            {
                if (j == curve.size() - 1)
                {
                    Vertex[] nrim = rim;

                    if (!this.smooth)
                    {
                        nrim = new Vertex[this.resolution];
                        for (int i = 0; i < this.resolution; i++)
                        {
                            Vertex v = new Vertex(count++);
                            nrim[i] = v;

                            mesh.vattr.set(v, Mesh.COORD, mesh.vattr.get(rim[i], Mesh.COORD).copy());
                            mesh.vattr.set(v, Mesh.NORMAL, tailNormal.copy());
                            mesh.vattr.set(v, Mesh.TEXTURE, mesh.vattr.get(rim[i], Mesh.TEXTURE).copy());

                            if (hasColor)
                            {
                                mesh.vattr.set(v, Mesh.COLOR, mesh.vattr.get(rim[i], Mesh.COLOR).copy());
                            }
                        }
                    }

                    for (int i = 0; i < this.resolution; i++)
                    {
                        Vertex curr = nrim[i];
                        Vertex prev = i == 0 ? nrim[this.resolution - 1] : nrim[i - 1];
                        mesh.graph.add(new Face(tail, prev, curr));
                    }
                }

                // make the segment
                for (int i = 0; i < this.resolution; i++)
                {
                    Vertex curr = rim[i];
                    Vertex prev = i == 0 ? rim[this.resolution - 1] : rim[i - 1];
                    Vertex pcurr = prim[i];
                    Vertex pprev = i == 0 ? prim[this.resolution - 1] : prim[i - 1];

                    mesh.graph.add(new Face(prev, pprev, curr));
                    mesh.graph.add(new Face(curr, pprev, pcurr));
                }
            }

            prim = rim;
        }

        return mesh;
    }
}
