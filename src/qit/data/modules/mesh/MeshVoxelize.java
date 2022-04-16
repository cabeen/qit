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

package qit.data.modules.mesh;

import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.modules.mask.MaskFill;
import qit.math.structs.Face;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

@ModuleDescription("Voxelize a mesh to a mask (should be watertight if the mesh is also)")
@ModuleAuthor("Ryan Cabeen")
public class MeshVoxelize implements Module
{
    @ModuleInput
    @ModuleDescription("input input")
    public Mesh input;

    @ModuleInput
    @ModuleDescription("reference mask")
    public Mask reference;

    @ModuleParameter
    @ModuleDescription("a label to draw")
    public Integer label = 1;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a attribute to draw (instead of the constant label)")
    public String attr = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the coordinate attribute to use (not applicable when specifying inner and outer shells)")
    public String coord = Mesh.COORD;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("label vertices in a shell between an inner and outer spatial attribute, e.g. like white and pial cortical surfaces")
    public String innerAttr = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("label vertices in a shell between an inner and outer spatial attribute, e.g. like white and pial cortical surfaces")
    public String outerAttr = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("label vertices in a shell between this attribute and the primary one, e.g. like white and pial cortical surfaces")
    public Double innerBuffer = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("label vertices in a shell between this attribute and the primary one, e.g. like white and pial cortical surfaces")
    public Double outerBuffer = null;

    @ModuleParameter
    @ModuleDescription("fill the inside of the mask")
    public boolean fill = false;

    @ModuleOutput
    @ModuleDescription("the output mask")
    public Mask output;

    @Override
    public MeshVoxelize run()
    {
        Mask out = this.reference.proto();
        double delta = 0.5 * this.reference.getSampling().deltaMin();

        for (Face face : this.input.graph.faces())
        {
            Vertex va = face.getA();
            Vertex vb = face.getB();
            Vertex vc = face.getC();

            int la = this.label;
            int lb = this.label;
            int lc = this.label;

            if (this.attr != null)
            {
                la = (int) Math.round(this.input.vattr.get(va, this.attr).get(0));
                lb = (int) Math.round(this.input.vattr.get(vb, this.attr).get(0));
                lc = (int) Math.round(this.input.vattr.get(vc, this.attr).get(0));
            }

            if (this.innerAttr != null && this.outerAttr != null)
            {
                Vect a0 = this.input.vattr.get(va, this.innerAttr);
                Vect b0 = this.input.vattr.get(vb, this.innerAttr);
                Vect c0 = this.input.vattr.get(vc, this.innerAttr);

                Vect a1 = this.input.vattr.get(va, this.outerAttr);
                Vect b1 = this.input.vattr.get(vb, this.outerAttr);
                Vect c1 = this.input.vattr.get(vc, this.outerAttr);

                Vect a01 = a1.minus(a0);
                Vect b01 = b1.minus(b0);
                Vect c01 = c1.minus(c0);

                if (this.innerBuffer != null || this.outerBuffer != null)
                {
                    if (this.innerBuffer != null)
                    {
                        a0.minusEquals(-this.innerBuffer, a01.normalize());
                        b0.minusEquals(-this.innerBuffer, b01.normalize());
                        c0.minusEquals(-this.innerBuffer, c01.normalize());
                    }

                    if (this.outerBuffer != null)
                    {
                        a1.plusEquals(this.outerBuffer, a01.normalize());
                        b1.plusEquals(this.outerBuffer, b01.normalize());
                        c1.plusEquals(this.outerBuffer, c01.normalize());
                    }

                    a01 = a1.minus(a0);
                    b01 = b1.minus(b0);
                    c01 = c1.minus(c0);
                }

                double maxdist = 0;
                maxdist = Math.max(maxdist, a01.norm());
                maxdist = Math.max(maxdist, b01.norm());
                maxdist = Math.max(maxdist, c01.norm());
                int num = MathUtils.round(maxdist / delta);

                for (int i = 0; i < num; i++)
                {
                    double alpha = i / (double) (num - 1);
                    Vect a = a0.plus(alpha, a01);
                    Vect b = b0.plus(alpha, b01);
                    Vect c = c0.plus(alpha, c01);
                    render(a, b, c, la, lb, lc, out);
                }
            }
            else
            {
                Vect a = this.input.vattr.get(va, this.coord);
                Vect b = this.input.vattr.get(vb, this.coord);
                Vect c = this.input.vattr.get(vc, this.coord);

                render(a, b, c, la, lb, lc, out);
            }
        }

        if (this.fill)
        {
            MaskFill filler = new MaskFill();
            filler.input = out;
            out = filler.run().output;
        }

        this.output = out;

        return this;
    }

    private static void render(Vect a, Vect b, Vect c, int la, int lb, int lc, Mask out)
    {
        Sampling sampling = out.getSampling();
        double threshold = 0.95 * sampling.deltaMin();
        double bound = 0;
        boolean recurse = false;
        int lm = mode(la, lb, lc);

        outer: for (Vect[] pair : new Vect[][]{{a,b}, {a, c}, {b, c}})
        {
            for (int i = 0; i < 3; i++)
            {
                double delta = Math.abs(pair[0].get(i) - pair[1].get(i));
                bound = Math.max(bound, delta);
                if (bound > threshold)
                {
                    recurse = true;
                    break outer;
                }
            }
        }

        if (!recurse)
        {
            for (Vect v : new Vect[]{a, b, c})
            {
                if (sampling.contains(v))
                {
                    out.set(sampling.nearest(v), lm);
                }
            }
        }
        else
        {
            Vect mab = a.plus(b).times(0.5);
            Vect mac = a.plus(c).times(0.5);
            Vect mbc = b.plus(c).times(0.5);

            render(a, mab, mac, la, la, la, out);
            render(b, mbc, mab, lb, lb, lb, out);
            render(c, mac, mbc, lc, lc, lc, out);
            render(mab, mbc, mac, lm, lm, lm, out);
        }
    }

    private static int mode(int a, int b, int c)
    {
        if (a == b || a == c)
        {
            return a;
        }
        else if (b == c)
        {
            return b;
        }
        else
        {
            return c;
        }
    }
}
