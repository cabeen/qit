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

import com.google.common.collect.Lists;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.volume.VolumeFilter;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.util.List;

@ModuleDescription("Create a mesh from image slices")
@ModuleAuthor("Ryan Cabeen")
public class VolumeSliceMesh implements Module
{
    public enum VolumeSliceMeshAxis
    {
        i, j, k
    }

    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("specify a name for the attribute")
    public String attribute = "data";

    @ModuleParameter
    @ModuleDescription("specify a range of slices to extract (or a specific slice)")
    public String range = "start:25:end";

    @ModuleParameter
    @ModuleDescription("the slice axis")
    public VolumeSliceMeshAxis axis = VolumeSliceMeshAxis.k;

    @ModuleParameter
    @ModuleDescription("remove background voxels")
    public boolean nobg = false;

    @ModuleParameter
    @ModuleDescription("specify a threshold for background removal")
    public double thresh = 1e-6;

    @ModuleParameter
    @ModuleDescription("add vertex colors")
    public boolean color = false;

    @ModuleParameter
    @ModuleDescription("use min/max normalization")
    public boolean normalize = false;

    @ModuleOutput
    @ModuleDescription("output mesh")
    public Mesh output;

    public VolumeSliceMesh run()
    {
        Volume volume = this.input;

        if (this.normalize)
        {
            Volume fvol = volume;
            volume = new VolumeEnhanceContrast(){{ this.input = fvol; this.type = VolumeEnhanceContrastType.Ramp; }}.run().output;
        }

        Sampling ref = volume.getSampling();

        int max = 0;

        switch (this.axis)
        {
            case i:
                max = ref.numI() - 1;
                break;
            case j:
                max = ref.numJ() - 1;
                break;
            case k:
                max = ref.numK() - 1;
                break;
        }

        int start = 0;
        int step = 1;
        int end = max;

        String[] tokens = this.range.split(":");

        if (tokens.length == 3)
        {
            start = tokens[0].equals("start") ? 0 : Integer.valueOf(tokens[0]);
            step = Integer.valueOf(tokens[1]);
            end = tokens[2].equals("end") ? max : Integer.valueOf(tokens[2]);
        }
        else if (tokens.length == 2)
        {
            start = tokens[0].equals("start") ? 0 : Integer.valueOf(tokens[0]);
            end = tokens[1].equals("end") ? max : Integer.valueOf(tokens[1]);
        }
        else if (tokens.length == 1)
        {
            String token = tokens[0];
            start = Integer.valueOf(token);
            step = 1;
            end = start + 1;
        }

        Mesh mesh = null;
        for (int s = start; s < end; s += step)
        {
            Mesh slice = new Mesh();

            switch (this.axis)
            {
                case i:
                {
                    Vertex[][] verts = new Vertex[ref.numJ()][ref.numK()];
                    for (int j = 0; j < ref.numJ(); j++)
                    {
                        for (int k = 0; k < ref.numK(); k++)
                        {
                            this.addVertex(ref, s, j, k, j, k, slice, verts);
                        }
                    }
                    for (int j = 1; j < ref.numJ(); j++)
                    {
                        for (int k = 1; k < ref.numK(); k++)
                        {
                            this.addFace(verts, j, k, slice);
                        }
                    }
                    break;
                }
                case j:
                {
                    Vertex[][] verts = new Vertex[ref.numI()][ref.numK()];
                    for (int i = 0; i < ref.numI(); i++)
                    {
                        for (int k = 0; k < ref.numK(); k++)
                        {
                            this.addVertex(ref, i, s, k, i, k, slice, verts);
                        }
                    }
                    for (int i = 1; i < ref.numI(); i++)
                    {
                        for (int k = 1; k < ref.numK(); k++)
                        {
                            this.addFace(verts, i, k, slice);
                        }
                    }
                    break;
                }
                case k:
                {
                    Vertex[][] verts = new Vertex[ref.numI()][ref.numJ()];
                    for (int i = 0; i < ref.numI(); i++)
                    {
                        for (int j = 0; j < ref.numJ(); j++)
                        {
                            this.addVertex(ref, i, j, s, i, j, slice, verts);
                        }
                    }
                    for (int i = 1; i < ref.numI(); i++)
                    {
                        for (int j = 1; j < ref.numJ(); j++)
                        {
                            this.addFace(verts, i, j, slice);
                        }
                    }
                    break;
                }
            }

            mesh =  MeshUtils.mbind(mesh, slice);
        }

        this.output = mesh;

        return this;
    }

    private boolean valid(int i, int j, int k)
    {
        if (!this.input.valid(i, j , k, this.mask))
        {
            return false;
        }

        if (this.nobg && this.input.get(i, j, k, 0) < this.thresh)
        {
            return false;
        }

        return true;
    }

    private void addVertex(Sampling sampling, int i, int j, int k, int a, int b, Mesh slice, Vertex[][] verts)
    {
        if (this.valid(i, j, k))
        {
            Vertex v = slice.graph.addVertex();
            slice.graph.add(v);
            slice.vattr.set(v, Mesh.COORD, sampling.world(i, j, k));
            slice.vattr.set(v, this.attribute, this.input.get(i, j, k));

            if (this.color)
            {
                if (this.input.getDim() == 3)
                {
                    double cr = this.input.get(i, j, k, 0);
                    double cg = this.input.get(i, j, k, 1);
                    double cb = this.input.get(i, j, k, 2);
                    slice.vattr.set(v, Mesh.COLOR, VectSource.create4D(cr, cg, cb, 1.0));
                }
                else if (this.input.getDim() == 4)
                {
                    double cr = this.input.get(i, j, k, 0);
                    double cg = this.input.get(i, j, k, 1);
                    double cb = this.input.get(i, j, k, 2);
                    double ca = this.input.get(i, j, k, 3);
                    slice.vattr.set(v, Mesh.COLOR, VectSource.create4D(cr, cg, cb, ca));
                }
                else
                {
                    double c = this.input.get(i, j, k, 0);
                    slice.vattr.set(v, Mesh.COLOR, VectSource.create4D(c, c, c, 1.0));
                }

            }
            verts[a][b] = v;
        }
    }

    private void addFace(Vertex[][] verts, int a, int b, Mesh slice)
    {
        Vertex va = verts[a - 1][b - 1];
        Vertex vb = verts[a - 1][b];
        Vertex vc = verts[a][b];
        Vertex vd = verts[a][b - 1];

        if (va != null && vb != null && vc != null)
        {
            slice.graph.add(new Face(va, vb, vc));
        }

        if (va != null && vc != null && vd != null)
        {
            slice.graph.add(new Face(va, vc, vd));
        }
    }
}
