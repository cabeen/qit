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

import com.google.common.collect.Maps;
import qit.base.Global;
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
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.util.Map;

@ModuleDescription("Plot data from a planar image")
@ModuleAuthor("Ryan Cabeen")
public class VolumeSurfacePlot implements Module
{
    @ModuleInput
    @ModuleDescription("input")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("the channel")
    public int dimension = 0;

    @ModuleParameter
    @ModuleDescription("the scaleCamera in x")
    public double sx = 1.0;

    @ModuleParameter
    @ModuleDescription("the scaleCamera in y")
    public double sy = 1.0;

    @ModuleParameter
    @ModuleDescription("the scaleCamera in z")
    public double sz = 1.0;

    @ModuleOutput
    @ModuleDescription("output")
    public Mesh output;

    @Override
    public Module run()
    {
        Sampling sampling = input.getSampling();

        Global.assume(sampling.numK() == 1, "invalid volume for surface rendering");

        Mesh mesh = new Mesh();

        for (Sample sample : sampling)
        {
            if (input.valid(sample, mask))
            {
                Vertex vert = new Vertex(sampling.index(sample));
                mesh.graph.add(vert);

                Vect coord = sampling.world(sample);
                double x = this.sx * coord.get(0);
                double y = this.sy * coord.get(1);
                double z = this.sz * input.get(sample, this.dimension);
                mesh.vattr.set(vert, Mesh.COORD, VectSource.create3D(x, y, z));
                mesh.vattr.set(vert, Mesh.VALUE, VectSource.create1D(z));
            }
        }

        Map<Integer,Vertex> cache = Maps.newHashMap();

        for (Sample sample : sampling)
        {
            if (mask == null || mask.foreground(sample))
            {
                int idx = sampling.index(sample);
                cache.put(idx, new Vertex(idx));
            }
        }

        for (Sample sample : sampling)
        {
            int i = sample.getI();
            int j = sample.getJ();

            if (i == sampling.numI() - 1 || j == sampling.numJ() - 1)
            {
                continue;
            }

            int idx00 = sampling.index(new Sample(i, j, 0));
            int idx10 = sampling.index(new Sample(i + 1, j, 0));
            int idx01 = sampling.index(new Sample(i, j + 1, 0));
            int idx11 = sampling.index(new Sample(i + 1, j + 1, 0));

            boolean valid00 = mask == null || mask.foreground(idx00);
            boolean valid10 = mask == null || mask.foreground(idx01);
            boolean valid01 = mask == null || mask.foreground(idx10);
            boolean valid11 = mask == null || mask.foreground(idx11);

            Vertex vert00 = cache.get(idx00);
            Vertex vert01 = cache.get(idx01);
            Vertex vert10 = cache.get(idx10);
            Vertex vert11 = cache.get(idx11);

            if (valid00 && valid10 && valid11)
            {
                mesh.graph.add(new Face(vert00, vert10, vert11));
            }

            if (valid00 && valid11 && valid01)
            {
                mesh.graph.add(new Face(vert00, vert11, vert01));
            }
        }

        this.output = mesh;

        return this;
    }
}
