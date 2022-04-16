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
 * product, and all original and amended mesh code is included in any
 * transmitted product. You may be held legally responsible for any
 * copyright infringement that is caused or encouraged by your failure to
 * abide by these terms and conditions.
 *
 * You are not permitted under this Licence to use this Software
 * commercially. Use for which any financial return is received shall be
 * defined as commercial use, and includes (1) integration of all or part
 * of the mesh code or the Software into a product for sale or license
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

import com.google.common.collect.Sets;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@ModuleDescription("Convert a mesh between file formats")
@ModuleAuthor("Ryan Cabeen")
public class MeshMapSphere implements Module
{
    private static final String INWARD = "inward";

    @ModuleInput
    @ModuleDescription("input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("the world space Laplacian smoothing iterations")
    public int preiters = 100;

    @ModuleParameter
    @ModuleDescription("the Laplacian smoothing iterations")
    public int iters = 100;

    @ModuleParameter
    @ModuleDescription("the minimum Laplacian smoothing rate")
    public double lambmin= 0.1;

    @ModuleParameter
    @ModuleDescription("the maximum Laplacian smoothing rate")
    public double lambmax = 0.5;

    @ModuleParameter
    @ModuleDescription("the curvature gain")
    public double gain = 5.0;

    @ModuleOutput
    @ModuleDescription("output mesh")
    public Mesh output;

    @Override
    public MeshMapSphere run()
    {
        Mesh mesh = this.input.copy();

        int nverts = mesh.graph.numVertex();
        int nfaces = mesh.graph.numFace();
        Set<Vertex> inward = Sets.newHashSet();
        mesh.vattr.add(INWARD, VectSource.create1D());
        mesh.vattr.add(Mesh.GAUSS_CURV, VectSource.create1D());
        mesh.vattr.add(Mesh.DISTANCE, VectSource.create1D());

        Logging.info("number of vertices: " + nverts);
        Logging.info("number of faces: " + nfaces);

        Consumer<String> runSmooth = (String attr) ->
        {
            Logging.info("smoothing mesh");
            for (Vertex vi : mesh.graph.verts())
            {
                Vect pi = mesh.vattr.get(vi, attr);
                Vect lap = new Vect(pi.size());
                List<Vertex> vring = mesh.graph.vertRing(vi);

                double area = 0;
                double angle = 0;
                for (Face fj : mesh.graph.faceRing(vi))
                {
                    Edge opp = fj.opposite(vi);
                    Vect aj = mesh.vattr.get(opp.getA(), attr);
                    Vect bj = mesh.vattr.get(opp.getB(), attr);

                    Vect daj = aj.minus(pi);
                    Vect dbj = bj.minus(pi);

                    angle += Math.acos(daj.dot(dbj) / (daj.norm() * dbj.norm()));
                    area += MeshUtils.area(mesh, fj) / 3.0;
                }

                double k = (2 * Math.PI - angle) / area;
                double sigk = 1.0 / (1.0 + Math.exp(-1.0 * this.gain * k));
                double trank = MathUtils.square(2 * sigk - 1);
                double lamb = this.lambmin + (this.lambmax - this.lambmin) * trank;

                double sumw = 0;
                for (Vertex vj : vring)
                {
                    Vect pj = mesh.vattr.get(vj, attr);
                    Vect pd = pj.minus(pi);
                    double w = 1.0;

                    lap.plusEquals(w, pd);
                    sumw += w;
                }

                Vect si = pi.plus(lamb, lap.divSafe(sumw));
                mesh.vattr.set(vi, attr, si);

                mesh.vattr.set(vi, Mesh.GAUSS_CURV, VectSource.create1D(k));
                mesh.vattr.set(vi, Mesh.DISTANCE, VectSource.create1D(lamb));
            }
        };

        Logging.info("presmoothing");
        MeshUtils.copy(mesh, Mesh.COORD, Mesh.SMOOTH);
        for (int i = 0; i < this.preiters; i++)
        {
            runSmooth.accept(Mesh.SMOOTH);
        }

        Logging.info("computing vertex statistics");
        VectsOnlineStats stats = new VectsOnlineStats(3);
        MeshUtils.consume(mesh, (v) -> stats.update(v), Mesh.SMOOTH);

        Logging.info("normalizing to intial sphere map");
        MeshUtils.apply(mesh, (v) -> v.minus(stats.mean).divSafe(stats.std).normalize(), Mesh.SMOOTH, Mesh.SPHERE);

        Logging.info("computing inwards");
        Runnable runSphere = () ->
        {
            inward.clear();

            for (Face face : mesh.graph.faces())
            {
                Vect centroid = MeshUtils.centroid(mesh, face, Mesh.SPHERE);
                Vect normal = MeshUtils.normal(mesh, face, Mesh.SPHERE);
                double metric = normal.dot(centroid);

                if (metric < 0)
                {
                    inward.add(face.getA());
                    inward.add(face.getB());
                    inward.add(face.getC());
                }
            }

            mesh.vattr.setAll(INWARD, VectSource.create1D(0));
            for (Vertex vertex : inward)
            {
                mesh.vattr.set(vertex, INWARD, VectSource.create1D(1.0));
            }

            MeshUtils.apply(mesh, (v) -> v.normalize(), Mesh.SPHERE, Mesh.SPHERE);

            Logging.info("number of inward faces: " + inward.size());
        };

        if (2 * inward.size() > nverts)
        {
            Logging.info("flipping faces");
            MeshUtils.flipFaces(mesh);
        }

        for (int i = 0; i < this.iters; i++)
        {
            Logging.info("iteration " +i);
            runSmooth.accept(Mesh.SPHERE);
            runSphere.run();
        }

        this.output = mesh;

        return this;
    }
}
