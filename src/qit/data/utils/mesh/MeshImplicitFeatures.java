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

import qit.data.datasets.Mesh;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.source.VectFunctionSource;

public class MeshImplicitFeatures
{
    private Mesh mesh;
    private Volume implicit;

    public MeshImplicitFeatures()
    {

    }

    public MeshImplicitFeatures withImplicit(Volume volume)
    {
        this.implicit = volume;
        return this;
    }

    public MeshImplicitFeatures withMesh(Mesh input)
    {
        this.mesh = input;

        return this;
    }

    public Mesh run()
    {
        this.mesh.vattr.remove(Mesh.SHAPE_INDEX);
        this.mesh.vattr.remove(Mesh.CURVEDNESS);
        this.mesh.vattr.remove(Mesh.MEAN_CURV);
        this.mesh.vattr.remove(Mesh.GAUSS_CURV);
        this.mesh.vattr.remove(Mesh.NORMAL);
        this.mesh.vattr.remove(Mesh.MAX_PRIN_CURV);
        this.mesh.vattr.remove(Mesh.MIN_PRIN_CURV);

        Sampling sampling = this.implicit.getSampling();
        double dx = sampling.deltaI();
        double dy = sampling.deltaJ();
        double dz = sampling.deltaK();

        VectFunction interpf = VolumeUtils.interp(InterpolationType.Trilinear, this.implicit);
        VectFunction func = VectFunctionSource.diff(interpf, dx, dy, dz).compose(VectFunctionSource.features());
        
        for (Vertex vert : this.mesh.graph.verts())
        {
            Vect pos = this.mesh.vattr.get(vert, Mesh.COORD);
            Vect feature = func.apply(pos);
            
            double norm_x = feature.get(VectFunctionSource.FEATURE_NORM_X);
            double norm_y = feature.get(VectFunctionSource.FEATURE_NORM_Y);
            double norm_z = feature.get(VectFunctionSource.FEATURE_NORM_Z);
            double gauss = feature.get(VectFunctionSource.FEATURE_GAUSS);
            double mean = feature.get(VectFunctionSource.FEATURE_MEAN);
            double kmin = feature.get(VectFunctionSource.FEATURE_KMIN);
            double kmax = feature.get(VectFunctionSource.FEATURE_KMAX);
            double kmin_x = feature.get(VectFunctionSource.FEATURE_EMIN_X);
            double kmin_y = feature.get(VectFunctionSource.FEATURE_EMIN_Y);
            double kmin_z = feature.get(VectFunctionSource.FEATURE_EMIN_Z);
            double kmax_x = feature.get(VectFunctionSource.FEATURE_EMAX_X);
            double kmax_y = feature.get(VectFunctionSource.FEATURE_EMAX_Y);
            double kmax_z = feature.get(VectFunctionSource.FEATURE_EMAX_Z);
            double si = feature.get(VectFunctionSource.FEATURE_SI);
            double cn = feature.get(VectFunctionSource.FEATURE_CN);
            
            this.mesh.vattr.set(vert, Mesh.SHAPE_INDEX, VectSource.create1D(si));
            this.mesh.vattr.set(vert, Mesh.CURVEDNESS, VectSource.create1D(cn));
            this.mesh.vattr.set(vert, Mesh.MAX_PRIN_CURV, VectSource.create1D(kmax));
            this.mesh.vattr.set(vert, Mesh.MIN_PRIN_CURV, VectSource.create1D(kmin));
            this.mesh.vattr.set(vert, Mesh.MIN_PRIN_DIR, VectSource.create3D(kmin_x, kmin_y, kmin_z).normalize());
            this.mesh.vattr.set(vert, Mesh.MAX_PRIN_DIR, VectSource.create3D(kmax_x, kmax_y, kmax_z).normalize());
            this.mesh.vattr.set(vert, Mesh.NORMAL, VectSource.create3D(norm_x, norm_y, norm_z));
            this.mesh.vattr.set(vert, Mesh.GAUSS_CURV, VectSource.create1D(gauss));
            this.mesh.vattr.set(vert, Mesh.MEAN_CURV, VectSource.create1D(mean));
            this.mesh.vattr.set(vert, Mesh.GAUSS_CURV, VectSource.create1D(gauss));
            this.mesh.vattr.set(vert, Mesh.MEAN_CURV, VectSource.create1D(mean));
        }
        
        return this.mesh;
    }
}
