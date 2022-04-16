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
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

@ModuleDescription("Compute geometric features of a mesh at each vertex.  A locally quadratic approximation at each vertex is used to find curvature features")
@ModuleAuthor("Ryan Cabeen")
public class MeshFeatures implements Module
{
    @ModuleInput
    @ModuleDescription("the input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("the mesh attribute")
    public String attr = Mesh.COORD;

    @ModuleParameter
    @ModuleDescription("the number of vertex rings to use for estimation")
    public int rings = 2;

    @ModuleParameter
    @ModuleDescription("the number of pre-smoothing iterations")
    public int smooth = 0;

    @ModuleParameter
    @ModuleDescription("refine the vertex positions to match the quadratic estimate (a loess type filter)")
    public boolean refine = false;

    @ModuleParameter
    @ModuleDescription("the gradient step size for refinement")
    public double step = 0.001;

    @ModuleParameter
    @ModuleDescription("the error threshold for refinement")
    public double thresh = 0.01;

    @ModuleParameter
    @ModuleDescription("the maximum number of refinement iterations")
    public int iters = 1000;

    @ModuleParameter
    @ModuleDescription("the scaling factor for boundary estimation")
    public double scaling = 2.0;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    public MeshFeatures run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();

        Logging.info("presmoothing mesh");
        VectFunction featurer = VectFunctionSource.features();
        Mesh smoothed = MeshSmooth.apply(mesh, attr, smooth);
        MeshUtils.computeNormals(smoothed, attr);

        Logging.info("estimating quadratic curvatures");
        for (Vertex vi : mesh.graph.verts())
        {
            VectOnlineStats edgeLengths = new VectOnlineStats();
            for (Edge edge : smoothed.graph.edgeStar(vi))
            {
                edgeLengths.update(MeshUtils.length(smoothed, edge));
            }
            double delta = scaling * edgeLengths.mean;

            // Find the vertex neighborhood
            Set<Vertex> neighborhood = Sets.newHashSet();
            neighborhood.add(vi);

            Set<Vertex> queue = Sets.newHashSet();
            queue.add(vi);

            for (int i = 0; i < rings; i++)
            {
                List<Vertex> nextup = Lists.newArrayList();

                for (Vertex v : queue)
                {
                    List<Vertex> ring = mesh.graph.vertRing(v);
                    for (Vertex vj : ring)
                    {
                        if (!neighborhood.contains(vj))
                        {
                            nextup.add(vj);
                            neighborhood.add(vj);
                        }
                    }
                }

                queue.clear();
                queue.addAll(nextup);
            }

            try
            {
                // Compute the local quadratic surface
                int N = 3 * neighborhood.size();
                Matrix A = new Matrix(N, 10);
                Matrix B = new Matrix(N, 1);

                Function<Vect, Vect> rower = (v) ->
                {
                    double x = v.getX();
                    double y = v.getY();
                    double z = v.getZ();

                    Vect out = VectSource.createND(10);
                    out.set(0, 1);
                    out.set(1, x);
                    out.set(2, y);
                    out.set(3, z);
                    out.set(4, x * y);
                    out.set(5, x * z);
                    out.set(6, y * z);
                    out.set(7, x * x);
                    out.set(8, y * y);
                    out.set(9, z * z);

                    return out;
                };

                Pointer<Integer> idxer = Pointer.to(0);
                BiConsumer<Vect, Double> insert = (v, d) ->
                {
                    int i = idxer.get();
                    A.setRow(i, rower.apply(v));
                    B.set(i, 0, d);
                    idxer.set(i + 1);
                };

                for (Vertex vertex : neighborhood)
                {
                    Vect pos = smoothed.vattr.get(vertex, attr);
                    Vect norm = smoothed.vattr.get(vertex, Mesh.NORMAL);

                    insert.accept(pos.plus(delta, norm), delta);
                    insert.accept(pos, 0.0);
                    insert.accept(pos.plus(-delta, norm), -delta);
                }

                Matrix Q = A.inv().times(B);
                double a = Q.get(0, 0);
                double b = Q.get(1, 0);
                double c = Q.get(2, 0);
                double d = Q.get(3, 0);
                double e = Q.get(4, 0);
                double f = Q.get(5, 0);
                double g = Q.get(6, 0);
                double h = Q.get(7, 0);
                double i = Q.get(8, 0);
                double j = Q.get(9, 0);

                Function<Vect, Pair<Vect,Matrix>> differentiate = pos ->
                {
                    double x = pos.getX();
                    double y = pos.getY();
                    double z = pos.getZ();

                    double px = b + e * y + f * z + 2 * h * x;
                    double py = c + e * x + g * z + 2 * i * y;
                    double pz = d + f * x + g * y + 2 * j * z;
                    double pxy = e;
                    double pxz = f;
                    double pyz = g;
                    double pxx = 2 * h;
                    double pyy = 2 * i;
                    double pzz = 2 * j;

                    Vect grad = VectSource.create3D(px, py, pz);
                    Matrix hess = MatrixSource.diag(pxx, pyy, pzz);
                    hess.set(0, 1, pxy);
                    hess.set(1, 0, pxy);
                    hess.set(0, 2, pxz);
                    hess.set(2, 0, pxz);
                    hess.set(1, 2, pyz);
                    hess.set(2, 1, pyz);

                    return Pair.of(grad, hess);
                };

                Vect pos = mesh.vattr.get(vi, attr);
                double feval = rower.apply(pos).dot(Q.flatten());

                if (this.refine)
                {
                    for (int iter = 0; iter < this.iters; iter++)
                    {
                        Pair<Vect,Matrix> mydiff = differentiate.apply(pos);

                        Vect mypos = pos.minus(mydiff.a.times(MathUtils.sign(feval) * this.step));
                        double myfeval = rower.apply(mypos).dot(Q.flatten());

                        pos = mypos;
                        feval = myfeval;

                        if (Math.abs(myfeval) < this.thresh)
                        {
                            break;
                        }
                    }
                }

                Pair<Vect,Matrix> differ = differentiate.apply(pos);
                Vect diff = VectSource.createND(VectFunctionSource.DIFF_DIM);
                diff.set(VectFunctionSource.DIFF_F, feval);
                diff.set(VectFunctionSource.DIFF_X, differ.a.getX());
                diff.set(VectFunctionSource.DIFF_Y, differ.a.getY());
                diff.set(VectFunctionSource.DIFF_Z, differ.a.getZ());
                diff.set(VectFunctionSource.DIFF_XX, differ.b.get(0, 0));
                diff.set(VectFunctionSource.DIFF_XY, differ.b.get(0, 1));
                diff.set(VectFunctionSource.DIFF_XZ, differ.b.get(0, 2));
                diff.set(VectFunctionSource.DIFF_YY, differ.b.get(1, 1));
                diff.set(VectFunctionSource.DIFF_YZ, differ.b.get(1, 2));
                diff.set(VectFunctionSource.DIFF_ZZ, differ.b.get(2, 2));
                Vect feature = featurer.apply(diff);

                double normX = feature.get(VectFunctionSource.FEATURE_NORM_X);
                double normY = feature.get(VectFunctionSource.FEATURE_NORM_Y);
                double normZ = feature.get(VectFunctionSource.FEATURE_NORM_Z);
                double gauss = feature.get(VectFunctionSource.FEATURE_GAUSS);
                double mean = feature.get(VectFunctionSource.FEATURE_MEAN);
                double kmin = feature.get(VectFunctionSource.FEATURE_KMIN);
                double kmax = feature.get(VectFunctionSource.FEATURE_KMAX);
                double kminX = feature.get(VectFunctionSource.FEATURE_EMIN_X);
                double kminY = feature.get(VectFunctionSource.FEATURE_EMIN_Y);
                double kminZ = feature.get(VectFunctionSource.FEATURE_EMIN_Z);
                double kmaxX = feature.get(VectFunctionSource.FEATURE_EMAX_X);
                double kmaxY = feature.get(VectFunctionSource.FEATURE_EMAX_Y);
                double kmaxZ = feature.get(VectFunctionSource.FEATURE_EMAX_Z);
                double si = feature.get(VectFunctionSource.FEATURE_SI);
                double cn = feature.get(VectFunctionSource.FEATURE_CN);
                double ci = feature.get(VectFunctionSource.FEATURE_CI);

                if (this.refine)
                {
                    mesh.vattr.set(vi, attr, pos);
                    mesh.vattr.set(vi, Mesh.NORMAL, VectSource.create3D(normX, normY, normZ));
                }

                mesh.vattr.set(vi, Mesh.FEVAL, VectSource.create1D(feval));
                mesh.vattr.set(vi, Mesh.SHAPE_INDEX, VectSource.create1D(si));
                mesh.vattr.set(vi, Mesh.CURVEDNESS, VectSource.create1D(cn));
                mesh.vattr.set(vi, Mesh.CURVED_INDEX, VectSource.create1D(ci));
                mesh.vattr.set(vi, Mesh.MAX_PRIN_CURV, VectSource.create1D(kmax));
                mesh.vattr.set(vi, Mesh.MIN_PRIN_CURV, VectSource.create1D(kmin));
                mesh.vattr.set(vi, Mesh.MIN_PRIN_DIR, VectSource.create3D(kminX, kminY, kminZ).normalize());
                mesh.vattr.set(vi, Mesh.MAX_PRIN_DIR, VectSource.create3D(kmaxX, kmaxY, kmaxZ).normalize());
                mesh.vattr.set(vi, Mesh.GAUSS_CURV, VectSource.create1D(gauss));
                mesh.vattr.set(vi, Mesh.MEAN_CURV, VectSource.create1D(mean));
                mesh.vattr.set(vi, Mesh.GAUSS_CURV, VectSource.create1D(gauss));
                mesh.vattr.set(vi, Mesh.MEAN_CURV, VectSource.create1D(mean));
            }
            catch (RuntimeException e)
            {
                // handle singular configurations
                Logging.info("estimation failed at vertex: " + vi);
                e.printStackTrace();
            }
        }

        this.output = mesh;

        return this;
    }
}