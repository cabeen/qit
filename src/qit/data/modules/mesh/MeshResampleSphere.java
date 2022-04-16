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
import qit.base.annot.*;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.mesh.MeshSampleSphere;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@ModuleDescription("Resample a spherically parameterized mesh using a new spherical triangulation")
@ModuleAuthor("Ryan Cabeen")
public class MeshResampleSphere implements Module
{
    @ModuleInput
    @ModuleDescription("input mesh to be resampled")
    public Mesh input;

    @ModuleInput
    @ModuleDescription("reference spherical mesh with the new triangulation")
    public Mesh reference;

    @ModuleParameter
    @ModuleDescription("the input mesh spherical coordinate attribute")
    public String inputSphereAttr = Mesh.SPHERE;

    @ModuleParameter
    @ModuleDescription("the reference mesh spherical coordinate attribute")
    public String refSphereAttr = Mesh.COORD;

    @ModuleParameter
    @ModuleDescription("a comma-separated list of input attributes that are discrete labels (if any)")
    public String labels = "";

    @ModuleParameter
    @ModuleDescription("a comma-separated list of the input attributes that should be skipped (if any)")
    public String skips = "";

    @ModuleOutput
    @ModuleDescription("output mesh")
    public Mesh output;

    @Override
    public MeshResampleSphere run()
    {
        Mesh inmesh = this.input.copy();
        Mesh mesh = this.reference.copy();

        MeshUtils.copy(inmesh, "coord", "original");

        MeshSampleSphere sampler = new MeshSampleSphere();
        sampler.withInput(inmesh);
        sampler.withInputSphere(this.refSphereAttr);
        sampler.withOutput(mesh);
        sampler.withOutputSphere(this.refSphereAttr);
        Arrays.stream(this.labels.split(",")).map(s -> sampler.addLabel(s));
        Arrays.stream(this.skips.split(",")).map(s -> sampler.addSkip(s));
        sampler.run();

        MeshFeaturesCortex features = new MeshFeaturesCortex();
        features.input = mesh;
        features.inplace = true;
        features.run();

        MeshUtils.copy(mesh, "coord", "sphere");
        MeshUtils.copy(mesh, "original", "coord");
        MeshUtils.remove(mesh, "original");

        this.output = mesh;

        return this;
    }
}