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

package qit.data.modules.volume;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskBoundary;
import qit.data.modules.mask.MaskDilate;
import qit.data.modules.mesh.MeshComponents;
import qit.data.modules.mesh.MeshSimplify;
import qit.data.modules.mesh.MeshSmooth;
import qit.data.source.VectSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.mesh.MeshMarchingCubesTable;
import qit.math.structs.Face;
import qit.math.structs.Vertex;
import qit.math.source.VectFunctionSource;

import java.util.function.Function;

@ModuleDescription("Extract a mesh from a volume level set.  This can be used for extracting boundary surfaces of distance fields, probability maps, parametric maps, etc.  The background level should be changed depending on the context though to make sure the mesh orientation is correct.")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Lorensen, W. E., & Cline, H. E. (1987, August). Marching cubes: A high resolution 3D surface construction algorithm. In ACM SIGGRAPH Computer Graphics (Vol. 21, No. 4, pp. 163-169)")
public class VolumeMarchingCubes implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleParameter
    @ModuleDescription("level set to extract")
    public double level = 0.5;

    @ModuleParameter
    @ModuleDescription("background value for outside sampling range (should be zero if the background is 'black' and a large number if the input is similar to a distance field)")
    public double background = 0;

    @ModuleParameter
    @ModuleDescription("exclude triangles on the boundary of the volume")
    public boolean nobound = false;

    @ModuleParameter
    @ModuleDescription("retain only the largest component")
    public boolean largest = false;

    @ModuleParameter
    @ModuleDescription("extract a mesh for every channel")
    public boolean multi;

    @ModuleParameter
    @ModuleDescription("a label attribute name for multi-channel extraction")
    public String label = "label";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply a median filter a given number of times before surface extraction")
    public Integer median = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply g Gaussian smoothing before surface extraction")
    public Double std = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a given support in voxels for voxel smoothing")
    public Integer support = 3;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("simplify the mesh to have the given mean triangle area")
    public Double meanarea = null;

    @ModuleParameter
    @ModuleDescription("do not simplify vertices on the mesh boundary")
    public boolean noboundmesh = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("smooth the mesh")
    public Double smooth = null;

    @ModuleOutput
    @ModuleDescription("output mesh")
    public Mesh output;

    public VolumeMarchingCubes withInput(Volume input)
    {
        this.input = input;
        this.output = null;

        return this;
    }

    public VolumeMarchingCubes withLevel(double l)
    {
        this.level = l;
        this.output = null;

        return this;
    }

    public VolumeMarchingCubes withBackground(double b)
    {
        this.background = b;
        this.output = null;

        return this;
    }

    public VolumeMarchingCubes run()
    {
        Function<Mesh, Mesh> postproc = (mesh) ->
        {
            Mesh out = mesh;

            if (this.meanarea != null)
            {
                Logging.info("simplifying mesh");
                MeshSimplify module = new MeshSimplify();
                module.input = out;
                module.meanarea = this.meanarea;
                module.nobound = this.noboundmesh;
                out = module.run().output;
            }

            if (this.largest)
            {
                Logging.info("selecting largest mesh component");
                MeshComponents module = new MeshComponents();
                module.input = out;
                module.largest = true;
                out = module.run().output;
            }

            if (this.smooth != null)
            {
                Logging.info("smoothing mesh");
                MeshSmooth module = new MeshSmooth();
                module.input = out;
                module.lambda = this.smooth;
                out = module.run().output;
            }

            return out;
        };

        Volume volume = this.input;

        if (this.std != null)
        {
            // only filter the voxels around the isolevel

            VolumeThreshold thresh = new VolumeThreshold();
            thresh.input = volume;
            thresh.threshold = this.level;
            Mask mask = thresh.run().output;

            MaskBoundary bounder = new MaskBoundary();
            bounder.input = mask;
            Mask boundary = bounder.run().output;

            MaskDilate dilater = new MaskDilate();
            dilater.input = boundary;
            dilater.num = this.support;
            boundary = dilater.run().output;

            VolumeFilterGaussian filter = new VolumeFilterGaussian();
            filter.input = volume;
            filter.sigma = std;
            filter.support = support;
            filter.mask = boundary;
            filter.pass = true;
            volume = filter.run().output;
        }

        if (this.median != null)
        {
            for (int i = 0; i < this.median; i++)
            {
                Logging.info("applying median filter");
                VolumeFilterMedian module = new VolumeFilterMedian();
                module.input = volume;
                volume = module.run().output;
            }
        }

        if (this.multi)
        {
            Mesh out = null;

            for (int i = 0; i < volume.getDim(); i++)
            {
                Logging.info(String.format("... extracting channel: %d / %d", (i + 1), volume.getDim()));
                Mesh mesh = mc(volume, this.level, this.nobound, this.background, i);

                mesh.vattr.setAll(this.label, VectSource.create1D(i + 1));

                if (out == null)
                {
                    out = mesh;
                }
                else
                {
                    out.add(mesh);
                }
            }

            this.output = postproc.apply(out);
        }
        else
        {
            Mesh out = mc(volume, this.level, this.nobound, this.background, 0);

            this.output = postproc.apply(out);
        }

        return this;
    }

    public static Mesh mc(Volume volume, double level, boolean nobound, double background, int channel)
    {
        Mesh mesh = new Mesh();
        Sampling sampling = volume.getSampling();

        double[] vertValues = new double[8];
        Vertex[] edgeVertexs = new Vertex[12];
        Vertex[] triVertexs = new Vertex[3];

        int nx = sampling.numI();
        int ny = sampling.numJ();
        int nz = sampling.numK();
        for (int i = -1; i < nx; i++)
        {
            for (int j = -1; j < ny; j++)
            {
                for (int k = -1; k < nz; k++)
                {
                    boolean anybg = false;

                    for (int idx = 0; idx < 8; idx++)
                    {
                        int ni = i + MeshMarchingCubesTable.MC_VERT_OFFSETS[idx][0];
                        int nj = j + MeshMarchingCubesTable.MC_VERT_OFFSETS[idx][1];
                        int nk = k + MeshMarchingCubesTable.MC_VERT_OFFSETS[idx][2];

                        boolean isfg = sampling.contains(ni, nj, nk);
                        vertValues[idx] = isfg ? volume.get(ni, nj, nk, channel) : background;

                        anybg |= !isfg;
                    }

                    if (nobound && anybg)
                    {
                        continue;
                    }

                    // Test the iso value
                    int vflag = 0;
                    for (int idx = 0; idx < 8; idx++)
                    {
                        if (vertValues[idx] <= level)
                        {
                            vflag |= 1 << idx;
                        }
                    }

                    int[] tri = MeshMarchingCubesTable.MC_TRIS[vflag];

                    int eflag = MeshMarchingCubesTable.MC_EDGE_CODES[vflag];
                    if (eflag == 0)
                    {
                        continue;
                    }

                    for (int idx = 0; idx < 12; idx++)
                    {
                        if ((eflag & 1 << idx) > 0)
                        {
                            double startv = vertValues[MeshMarchingCubesTable.MC_EDGES[idx][0]];
                            double endv = vertValues[MeshMarchingCubesTable.MC_EDGES[idx][1]];
                            double lambda = startv == endv ? 0.5f : (level - startv) / (endv - startv);

                            int[] offa = MeshMarchingCubesTable.MC_VERT_OFFSETS[MeshMarchingCubesTable.MC_EDGES[idx][0]];
                            int[] offb = MeshMarchingCubesTable.MC_VERT_OFFSETS[MeshMarchingCubesTable.MC_EDGES[idx][1]];

                            int ai = i + offa[0];
                            int aj = j + offa[1];
                            int ak = k + offa[2];
                            Vect vb = sampling.world(ai, aj, ak);

                            int bi = i + offb[0];
                            int bj = j + offb[1];
                            int bk = k + offb[2];
                            Vect cb = sampling.world(bi, bj, bk);

                            cb.minusEquals(vb);
                            vb.plusEquals(lambda, cb);

                            // make sure indices are non-negative and unique
                            int aidx = ai + 1 + (nx + 1) * (aj + 1) + (ak + 1) * (nx + 1) * (ny + 1);
                            int bidx = bi + 1 + (nx + 1) * (bj + 1) + (bk + 1) * (nx + 1) * (ny + 1);

                            int ridx = MeshMarchingCubesTable.MC_EDGE_DIR[idx] > 0 ? aidx : bidx;
                            int eidx = 3 * ridx + MeshMarchingCubesTable.MC_EDGE_DIM[idx];

                            Vertex v = new Vertex(eidx);
                            edgeVertexs[idx] = v;
                            mesh.graph.add(v);
                            mesh.vattr.add(v);
                            mesh.vattr.set(v, Mesh.COORD, vb);
                        }
                    }

                    int idx = 0;
                    while (tri[idx] != -1)
                    {
                        triVertexs[0] = edgeVertexs[tri[idx++]];
                        triVertexs[1] = edgeVertexs[tri[idx++]];
                        triVertexs[2] = edgeVertexs[tri[idx++]];

                        mesh.graph.add(new Face(triVertexs[0], triVertexs[1], triVertexs[2]));
                    }
                }
            }
        }

        return mesh;
    }

    public static void features(Volume volume, Mesh mesh)
    {
        VolumeUtils.interp(InterpolationType.Trilinear, volume).compose(VectFunctionSource.features());
    }

    public static Mesh apply(Volume volume, double mylevel)
    {
        return new VolumeMarchingCubes()
        {{
            this.input = volume;
            this.level = mylevel;
        }}.run().output;
    }
}
