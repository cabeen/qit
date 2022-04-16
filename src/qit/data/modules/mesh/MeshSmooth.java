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

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Smooth a mesh")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Taubin, G. (1995, September). A signal processing approach to fair surface design. In Proceedings of the 22nd annual conference on Computer graphics and interactive techniques (pp. 351-358). ACM.")
public class MeshSmooth implements Module
{
    @ModuleInput
    @ModuleDescription("the input")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("the attribute to smooth")
    public String attr = Mesh.COORD;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the output attribute names (if none, the input is replaced)")
    public String attrout = null;

    @ModuleParameter
    @ModuleDescription("a number of iterations")
    public int num = 5;

    @ModuleParameter
    @ModuleDescription("the lambda laplacian smoothing parameter")
    public Double lambda = 0.3;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the optional mu parameter for Taubin's method")
    public Double mu = null;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    public MeshSmooth run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();

        String[] inputs = this.attr.split(",");
        String[] outputs = inputs;

        if (this.attrout != null)
        {
            outputs = this.attrout.split(",");
        }

        for (int i = 0; i < inputs.length; i++)
        {
            Logging.info("started mesh smoothing " + inputs[i]);
            for (int iter = 0; iter < this.num; iter++)
            {
                Logging.info("started mesh smoothing iteration " + iter);

                smooth(mesh, inputs[i], outputs[i], this.lambda);
                if (this.mu != null)
                {
                    smooth(mesh, inputs[i], outputs[i], -this.mu);
                }
            }
        }

        Logging.info("recomputing normals");
        MeshUtils.computeNormals(mesh);

        Logging.info("finished mesh smoothing");

        this.output = mesh;

        return this;
    }

    public static void smooth(Mesh mesh, String input, String output, double param)
    {
        mesh.vattr.add(output, mesh.vattr.proto(input));

        for (Vertex vi : mesh.graph.verts())
        {
            Vect pi = mesh.vattr.get(vi, input);

            // Either create or zero the new vect
            Vect lap = new Vect(pi.size());

            // Compute the average of the ring vertices
            List<Vertex> vring = mesh.graph.vertRing(vi);
            double sumw = 0;
            for (Vertex vj : vring)
            {
                Vect pj = mesh.vattr.get(vj, input);
                Vect pd = pj.minus(pi);
                double w = 1.0; // update this later to have fancy weights

                lap.plusEquals(w, pd);
                sumw += w;
            }

            if (MathUtils.nonzero(sumw))
            {
                lap.timesEquals(1.0 / sumw);
            }

            pi.plusEquals(param, lap);

            mesh.vattr.set(vi, output, pi);
        }
    }

    public static Mesh apply(Mesh mesh, int mynum)
    {
        return new MeshSmooth()
        {{
            this.input = mesh;
            this.num = mynum;
        }}.run().output;
    }

    public static Mesh apply(Mesh mesh, String myattr, int mynum)
    {
        return new MeshSmooth()
        {{
            this.input = mesh;
            this.attr = myattr;
            this.num = mynum;
        }}.run().output;
    }

    public static Mesh apply(Mesh mesh, int mynum, double mylamb)
    {
        return new MeshSmooth()
        {{
            this.input = mesh;
            this.num = mynum;
            this.lambda = mylamb;
        }}.run().output;
    }

    public static Mesh apply(Mesh mesh, String myattr, int mynum, double mylamb)
    {
        return new MeshSmooth()
        {{
            this.input = mesh;
            this.attr = myattr;
            this.num = mynum;
            this.lambda = mylamb;
        }}.run().output;
    }


    public static void applyInPlace(Mesh mesh, int mynum, double mylamb)
    {
        new MeshSmooth()
        {{
            this.input = mesh;
            this.num = mynum;
            this.lambda = mylamb;
            this.inplace = true;
        }}.run();
    }

    public static void applyInPlace(Mesh mesh, String myattr, int mynum, double mylamb)
    {
        new MeshSmooth()
        {{
            this.input = mesh;
            this.attr = myattr;
            this.num = mynum;
            this.lambda = mylamb;
            this.inplace = true;
        }};
    }
}
