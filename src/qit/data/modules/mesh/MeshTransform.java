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
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.*;
import qit.data.modules.curves.CurvesTransform;
import qit.data.source.VectSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.mesh.MeshFunction;
import qit.math.structs.Quaternion;
import qit.math.structs.VectFunction;
import qit.math.source.VectFunctionSource;

@ModuleDescription("Transform a mesh")
@ModuleAuthor("Ryan Cabeen")
public class MeshTransform implements Module
{
    @ModuleInput
    @ModuleDescription("input mesh")
    public Mesh input;

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

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply a transform to match the pose of a volume")
    public Volume pose;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply a transform to match the inverse pose of a volume")
    public Volume invpose;

    @ModuleParameter
    @ModuleDescription("reverse the order, i.e. compose the affine(deform(x)), whereas the default is deform(affine(x))")
    public boolean reverse = false;

    @ModuleParameter
    @ModuleDescription("a comma-separated list of attributes to transform")
    public String attrs = "coord";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("translate the mesh in by the given about in the x dimension")
    public Double tx;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("translate the mesh in by the given about in the y dimension")
    public Double ty;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("translate the mesh in by the given about in the z dimension")
    public Double tz;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("scale the mesh in by the given about in the x dimension")
    public Double sx;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("scale the mesh in by the given about in the y dimension")
    public Double sy;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("scale the mesh in by the given about in the z dimension")
    public Double sz;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("output mesh")
    public Mesh output;

    @Override
    public MeshTransform run()
    {
        VectFunction xfm = null;
        if (this.pose != null)
        {
            Sampling sampling = this.pose.getSampling();
            Vect start = sampling.start();
            Quaternion quat = sampling.quat();

            xfm = new Affine(quat, start);
        }
        else if (this.invpose != null)
        {
            Sampling sampling = this.invpose.getSampling();
            Vect start = sampling.start();
            Quaternion quat = sampling.quat();

            xfm = new Affine(quat, start).inv();
        }
        else if (this.tx != null || this.ty != null || this.tz != null)
        {
            double dtx = this.tx == null ? 0.0 : this.tx;
            double dty = this.ty == null ? 0.0 : this.ty;
            double dtz = this.tz == null ? 0.0 : this.tz;
            xfm = Affine.id(3).plus(VectSource.create3D(dtx, dty, dtz));
        }
        else if (this.sx != null || this.sy != null || this.sz != null)
        {
            double dsx = this.sx == null ? 1.0 : this.sx;
            double dsy = this.sy == null ? 1.0 : this.sy;
            double dsz = this.sz == null ? 1.0 : this.sz;
            xfm = Affine.id(3).times(VectSource.create3D(dsx, dsy, dsz));
        }
        else
        {
            xfm = VolumeUtils.xfm(this.affine, this.invaffine, this.deform, this.reverse);
        }

        Mesh mesh = this.inplace ? this.input : this.input.copy();

        for (String attr : this.attrs.split(","))
        {
            Logging.info("transforming " + attr);
            new MeshFunction(xfm).withMesh(mesh).withSource(attr).withDest(attr).run();
        }

        this.output = mesh;

        return this;
    }

    public static Mesh applyPose(Mesh data, Volume volume)
    {
        return new MeshTransform()
        {{
            this.input = data;
            this.pose = volume;
        }}.run().output;
    }

    public static Mesh applyInversePose(Mesh data, Volume volume)
    {
        return new MeshTransform()
        {{
            this.input = data;
            this.invpose = volume;
        }}.run().output;
    }
}
