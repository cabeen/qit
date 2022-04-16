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


package qit.data.modules.mask;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Sampling;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.modules.mesh.MeshSimplify;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeMarchingCubes;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.MeshUtils;
import qit.data.utils.mesh.MeshMarchingCubesTable;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.util.Map;
import java.util.Set;

@ModuleDescription("Extract surfaces from a mask.  If there are multiple labels in the volume, distinct meshes will be produced for each label.")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Lorensen, W. E., & Cline, H. E. (1987, August). Marching cubes: A high resolution 3D surface construction algorithm. In ACM siggraph computer graphics (Vol. 21, No. 4, pp. 163-169). ACM.")
public class MaskMarchingCubes implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("a table listing a subset of indices")
    public Table table;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("perform Gaussian smoothing before surface extraction")
    public Double std = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a given support in voxels for smoothing")
    public Integer support = 3;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("use a given isolevel for smoothed surface extraction")
    public Double level = 0.5;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a string specifying a subset of labels, e.g. 1,2,4:6")
    public String which;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the index field name to use in the table")
    public String tindex = "index";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the mesh attribute for the index value (default is \"label\")")
    public String mindex = Mesh.LABEL;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("simplify the mesh to have the given mean triangle area")
    public Double meanarea = null;

    @ModuleOutput
    @ModuleDescription("output mesh")
    public Mesh output;

    @Override
    public MaskMarchingCubes run()
    {
        Set<Integer> idxes = Sets.newHashSet();
        if (this.table != null)
        {
            for (Integer key : this.table.getKeys())
            {
                idxes.add(Integer.valueOf(this.table.get(key, this.tindex)));
            }
        }
        else if (this.which != null)
        {
            for (Integer key : CliUtils.parseWhich(this.which))
            {
                idxes.add(key);
            }
        }
        else
        {
            idxes.addAll(MaskUtils.listNonzero(this.input));
        }

        Map<Integer, Mesh> meshes = mc(this.input, idxes);

        Logging.info("extracting " + idxes.size() + " regions");

        Mesh out = null;
        for (int index : idxes)
        {
            Mesh mesh = meshes.get(index);
            MeshUtils.setAll(mesh, this.mindex, VectSource.create1D(index));

            if (out == null)
            {
                out = mesh;
            }
            else
            {
                out.add(mesh);
            }
        }

        if (this.meanarea != null)
        {
            Logging.info("simplifying mesh");
            MeshSimplify mySimplify = new MeshSimplify();
            mySimplify.input = out;
            mySimplify.meanarea = this.meanarea;
            out = mySimplify.run().output;
        }

        this.output = out;
        return this;
    }

    // a custom mc implementation for the discrete-valued mask volume
    public Map<Integer, Mesh> mc(Mask mask, Set<Integer> labels)
    {
        if (std != null)
        {
            Map<Integer, Mesh> meshes = Maps.newHashMap();

            for (int label : labels)
            {
                Logging.info("...extracting region: " + label);
                MaskExtract extractor = new MaskExtract();
                extractor.input = mask;
                extractor.label = String.valueOf(label);
                Mask submask = extractor.run().output;

                MaskBoundary bounder = new MaskBoundary();
                bounder.input = mask;
                Mask boundary = bounder.run().output;

                MaskDilate dilater = new MaskDilate();
                dilater.input = boundary;
                dilater.num = this.support;
                boundary = dilater.run().output;

                VolumeFilterGaussian filter = new VolumeFilterGaussian();
                filter.input = submask.copyVolume();
                filter.sigma = std;
                filter.support = support;
                filter.mask = boundary;
                filter.pass = true;
                Volume volume = filter.run().output;

                Mesh mesh = new VolumeMarchingCubes().withInput(volume).withBackground(0).withLevel(this.level).run().output;
                meshes.put(label, mesh);
            }

            return meshes;
        }
        else
        {
            Map<Integer, Mesh> meshes = Maps.newHashMap();

            Sampling sampling = mask.getSampling();
            for (int label : labels)
            {
                meshes.put(label, new Mesh());
            }

            int[] vertValues = new int[8];
            Vertex[] edgeVertexs = new Vertex[12];
            Vertex[] triVertexs = new Vertex[3];
            int background = 0;

            int nx = sampling.numI();
            int ny = sampling.numJ();
            int nz = sampling.numK();
            for (int i = -1; i < nx; i++)
            {
                for (int j = -1; j < ny; j++)
                {
                    for (int k = -1; k < nz; k++)
                    {
                        for (int idx = 0; idx < 8; idx++)
                        {
                            int ni = i + MeshMarchingCubesTable.MC_VERT_OFFSETS[idx][0];
                            int nj = j + MeshMarchingCubesTable.MC_VERT_OFFSETS[idx][1];
                            int nk = k + MeshMarchingCubesTable.MC_VERT_OFFSETS[idx][2];
                            vertValues[idx] = sampling.contains(ni, nj, nk) ? mask.get(ni, nj, nk) : background;
                        }

                        // maintain a vflag for each region
                        for (int label : labels)
                        {
                            Mesh mesh = meshes.get(label);

                            // Test for a discrete boundary
                            int vflag = 0;
                            for (int vidx = 0; vidx < 8; vidx++)
                            {
                                if (vertValues[vidx] == label)
                                {
                                    vflag |= 1 << vidx;
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
                                    double lambda = 0.5f; // extract a boundary at the midpoint... is there a better way?

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

                                // flip the triangle because zero is outside
                                mesh.graph.add(new Face(triVertexs[0], triVertexs[2], triVertexs[1]));
                            }
                        }
                    }
                }
            }

            return meshes;
        }
    }

    public static Mesh apply(Mask mask)
    {
        return new MaskMarchingCubes()
        {{
            this.input = mask;
        }}.run().output;
    }

    public static Mesh apply(Mask mask, int label)
    {
        return new MaskMarchingCubes()
        {{
            this.input = mask;
            this.which = String.valueOf(label);
        }}.run().output;
    }

    public static Mesh apply(Mask mask, String label)
    {
        return new MaskMarchingCubes()
        {{
            this.input = mask;
            this.which = label;
        }}.run().output;
    }
}
