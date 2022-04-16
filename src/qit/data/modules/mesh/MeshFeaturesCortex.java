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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.base.structs.Pointer;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.Triangle;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

@ModuleDescription("Compute cortical surface features of a mesh at each vertex")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Winkler, A. M., Sabuncu, M. R., Yeo, B. T., Fischl, B., Greve, D. N., Kochunov, P., ... & Glahn, D. C. (2012). Measuring and comparing brain cortical surface area and other areal quantities. Neuroimage, 61(4), 1428-1443.")
public class MeshFeaturesCortex implements Module
{
    @ModuleInput
    @ModuleDescription("the input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("the mesh attribute")
    public String attr = Mesh.COORD;

    @ModuleParameter
    @ModuleDescription("the outer pial mesh attribute")
    public String outer = "pial";

    @ModuleParameter
    @ModuleDescription("the inner mesh attribute")
    public String inner = "white";

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    public MeshFeaturesCortex run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();

        for (Vertex vertex : mesh.vattr)
        {
            Vect vOuter = mesh.vattr.get(vertex, this.outer);
            Vect vInner = mesh.vattr.get(vertex, this.inner);

            final Pointer<Double> volume = Pointer.to(0d);
            final Pointer<Double> areaInner = Pointer.to(0d);
            final Pointer<Double> areaOuter = Pointer.to(0d);

            BiConsumer<Triangle,Triangle> update = (triOuter, triInner) ->
            {
                Vect Aw = triOuter.getA();
                Vect Bw = triOuter.getB();
                Vect Cw = triOuter.getC();
                Vect Ap = triInner.getA();
                Vect Bp = triInner.getB();
                Vect Cp = triInner.getC();

                volume.set(volume.get() + MathUtils.tetrahedraVolume(Aw, Bw, Cw, Ap));
                volume.set(volume.get() + MathUtils.tetrahedraVolume(Ap, Bp, Cp, Bw));
                volume.set(volume.get() + MathUtils.tetrahedraVolume(Ap, Cp, Cw, Bw));

                areaOuter.set(areaOuter.get() + new Triangle(Aw, Bw, Cw).area());
                areaInner.set(areaInner.get() + new Triangle(Ap, Bp, Cp).area());
            };

            for (Face face : mesh.graph.faceRing(vertex))
            {
                // Let's cut out the corner of the triangle and
                // split it into two segments
                Edge edge = face.opposite(vertex);

                // get the outer vertex positions
                Vect aOuter = mesh.vattr.get(edge.getA(), this.outer);
                Vect aInner = mesh.vattr.get(edge.getA(), this.inner);

                Vect bOuter = mesh.vattr.get(edge.getB(), this.outer);
                Vect bInner = mesh.vattr.get(edge.getB(), this.inner);

                // compute the centroid position
                Vect cOuter = new Triangle(vOuter, aOuter, bOuter).center();
                Vect cInner = new Triangle(vInner, aInner, bInner).center();

                // compute the half-length positions
                aOuter = aOuter.times(0.5).plus(0.5, vOuter);
                aInner = aInner.times(0.5).plus(0.5, vInner);

                bOuter = bOuter.times(0.5).plus(0.5, vOuter);
                bInner = bInner.times(0.5).plus(0.5, vInner);

                // create the new triangles
                Triangle aOuterTri = new Triangle(vOuter, aOuter, cOuter);
                Triangle aInnerTri = new Triangle(vInner, aInner, cInner);
                Triangle bOuterTri = new Triangle(vOuter, cOuter, bOuter);
                Triangle bInnerTri = new Triangle(vInner, cInner, bInner);

                // update the area and volume
                update.accept(aOuterTri, aInnerTri);
                update.accept(bOuterTri, bInnerTri);
            }

            double area = (areaOuter.get() + areaInner.get()) / 2.0;

            mesh.vattr.set(vertex, Mesh.VOLUME, VectSource.create1D(volume.get()));
            mesh.vattr.set(vertex, Mesh.AREA, VectSource.create1D(area));
            mesh.vattr.set(vertex, Mesh.AREA_INNER, VectSource.create1D(areaInner.get()));
            mesh.vattr.set(vertex, Mesh.AREA_OUTER, VectSource.create1D(areaOuter.get()));
        }

        this.output = mesh;

        return this;
    }
}