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

package qit.data.modules.mri.noddi;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.*;
import qit.data.models.Noddi;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.enums.KernelInterpolationType;
import qit.data.utils.mri.estimation.NoddiEstimator;
import qit.data.utils.mri.estimation.VolumeKernelModelEstimator;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsStats;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;

import java.util.List;

@ModuleDescription("Estimate cortical gray matter noddi parameters")
@ModuleAuthor("Ryan Cabeen")
public class VolumeNoddiCortex implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleInput
    @ModuleDescription("input mesh")
    public Mesh mesh;

    @ModuleParameter
    @ModuleDescription("the interpolation type")
    public KernelInterpolationType interp = KernelInterpolationType.Trilinear;

    @ModuleParameter
    @ModuleDescription("specify an estimation method")
    public String estimation = NoddiEstimator.DEFAULT_ESTIMATION;

    @ModuleParameter
    @ModuleDescription("the filter radius in voxels")
    public Integer support = 3;

    @ModuleParameter
    @ModuleDescription("the positional bandwidth in mm")
    public Double hpos = 1.0;

    @ModuleParameter
    @ModuleDescription("pial attribute name")
    public String pial = "pial";

    @ModuleParameter
    @ModuleDescription("white attribute name")
    public String white = "white";

    @ModuleParameter
    @ModuleDescription("the number of points to sample")
    public int samples = 15;

    @ModuleParameter
    @ModuleDescription("the inner search buffer size")
    public double inner = 0.5;

    @ModuleParameter
    @ModuleDescription("the outer search buffer size")
    public double outer = 0.5;

    @ModuleParameter
    @ModuleDescription("the maximum isotropic volume fraction")
    public Double maxiso = 0.5;

    @ModuleParameter
    @ModuleDescription("the maximum isotropic volume fraction")
    public Double zscore = 3.0;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output weights")
    public Volume weights;

    @Override
    public Module run()
    {
        Mesh mesh = this.inplace ? this.mesh : this.mesh.copy();
        this.weights = this.input.proto(1);

        NoddiEstimator estimator = new NoddiEstimator();
        estimator.parse(this.estimation);

        VolumeKernelModelEstimator vestimator = new VolumeKernelModelEstimator(new Noddi());
        vestimator.estimator = estimator;
        vestimator.volume = input;
        vestimator.support = this.support;
        vestimator.hpos = this.hpos;
        vestimator.interp = this.interp;

        Sampling sampling = this.input.getSampling();

        for (Vertex vertex : mesh.vattr)
        {
            Vect mypial = mesh.vattr.get(vertex, this.pial);
            Vect mywhite = mesh.vattr.get(vertex, this.white);
            Vect orient = mypial.minus(mywhite).normalize();

            Vect start = mywhite.minus(this.inner, orient);
            Vect end = mypial.plus(this.outer, orient);
            Vect delta = end.minus(start);

            List<Double> weightRawValues = Lists.newArrayList();
            List<Double> ficvfRawValues = Lists.newArrayList();
            List<Double> odiRawValues = Lists.newArrayList();
            List<Vect> posRawValues = Lists.newArrayList();

            for (int i = 0; i < this.samples; i++)
            {
                double alpha = i / (double) (this.samples - 1);
                double weight = 1.0 - Math.abs(alpha - 0.5);
                Vect mypos = start.plus(alpha, delta);

                if (this.input.valid(sampling.nearest(mypos), this.mask))
                {
                    Noddi mymodel = new Noddi(vestimator.apply(mypos));

                    if (mymodel.getFISO() < this.maxiso)
                    {
                        double frac = (1.0 - mymodel.getFISO());
                        weight *= frac * frac;

                        posRawValues.add(mypos);
                        weightRawValues.add(weight);
                        ficvfRawValues.add(mymodel.getFICVF());
                        odiRawValues.add(mymodel.getODI());
                    }
                }
            }

            if (ficvfRawValues.size() > 0)
            {
                VectStats ficvfRawStats = new VectStats().withInput(VectSource.create(ficvfRawValues)).withWeights(VectSource.create(weightRawValues)).run();
                VectStats odiRawStats = new VectStats().withInput(VectSource.create(odiRawValues)).withWeights(VectSource.create(weightRawValues)).run();

                List<Vect> posValues = Lists.newArrayList();
                List<Double> weightValues = Lists.newArrayList();
                List<Double> ficvfValues = Lists.newArrayList();
                List<Double> odiValues = Lists.newArrayList();

                for (int i = 0; i < ficvfRawValues.size(); i++)
                {
                    double ficvfValue = ficvfRawValues.get(i);
                    double odiValue = odiRawValues.get(i);

                    double ficvfZscore = Math.abs(ficvfValue - ficvfRawStats.mean) / ficvfRawStats.std;
                    double odiZscore = Math.abs(odiValue - odiRawStats.mean) / odiRawStats.std;

                    if (ficvfZscore < this.zscore && odiZscore < this.zscore)
                    {
                        posValues.add(posRawValues.get(i));
                        weightValues.add(weightRawValues.get(i));
                        ficvfValues.add(ficvfValue);
                        odiValues.add(odiValue);
                    }
                }

                if (ficvfValues.size() > 0)
                {
                    Vect weights = VectSource.create(weightValues);
                    weights.divSafeEquals(weights.sum());

                    VectsStats posStats = new VectsStats().withInput(VectsSource.create(posValues)).withWeights(weights).run();
                    VectStats ficvfStats = new VectStats().withInput(VectSource.create(ficvfValues)).withWeights(weights).run();
                    VectStats odiStats = new VectStats().withInput(VectSource.create(odiValues)).withWeights(weights).run();

                    mesh.vattr.set(vertex, "noddi_position", posStats.mean);
                    mesh.vattr.set(vertex, "noddi_count", VectSource.create1D(ficvfStats.num));
                    mesh.vattr.set(vertex, "ficvf_max", VectSource.create1D(ficvfStats.max));
                    mesh.vattr.set(vertex, "ficvf_mean", VectSource.create1D(ficvfStats.mean));
                    mesh.vattr.set(vertex, "ficvf_std", VectSource.create1D(ficvfStats.std));
                    mesh.vattr.set(vertex, "ficvf_min", VectSource.create1D(ficvfStats.min));
                    mesh.vattr.set(vertex, "odi_max", VectSource.create1D(odiStats.max));
                    mesh.vattr.set(vertex, "odi_mean", VectSource.create1D(odiStats.mean));
                    mesh.vattr.set(vertex, "odi_std", VectSource.create1D(odiStats.std));
                    mesh.vattr.set(vertex, "odi_min", VectSource.create1D(odiStats.min));

                    for (int i = 0; i < posValues.size(); i++)
                    {
                        Sample sample = sampling.nearest(posValues.get(i));
                        if (sampling.contains(sample))
                        {
                            this.weights.set(sample, 0, Math.max(this.weights.get(sample, 0), weightValues.get(i)));
                        }
                    }
                }
            }
        }

        this.output = mesh;

        return this;
    }
}
