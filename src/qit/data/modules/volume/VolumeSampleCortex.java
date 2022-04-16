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
import com.google.common.collect.Sets;
import org.apache.commons.math3.analysis.FunctionUtils;
import qit.base.Logging;
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
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.models.Noddi;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.VectUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.enums.KernelInterpolationType;
import qit.data.utils.mri.estimation.NoddiEstimator;
import qit.data.utils.mri.estimation.VolumeKernelModelEstimator;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;

import java.util.List;
import java.util.Set;

@ModuleDescription("Estimate cortical gray matter parameters")
@ModuleAuthor("Ryan Cabeen")
public class VolumeSampleCortex implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("input mesh")
    public Mesh mesh;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input gray matter probability map")
    public Volume gm;

    @ModuleParameter
    @ModuleDescription("the name of the volumetric data (only used for naming mesh attributes)")
    public String name = "data";

    @ModuleParameter
    @ModuleDescription("pial attribute name")
    public String pial = "pial";

    @ModuleParameter
    @ModuleDescription("the middle attribute name")
    public String middle = "middle";

    @ModuleParameter
    @ModuleDescription("white attribute name")
    public String white = "white";

    @ModuleParameter
    @ModuleDescription("the number of points to sample")
    public int samples = 15;

    @ModuleParameter
    @ModuleDescription("the inner search buffer size")
    public double inner = 0.0;

    @ModuleParameter
    @ModuleDescription("the outer search buffer size")
    public double outer = 0.0;

    @ModuleParameter
    @ModuleDescription("use linear weights, e.g. 1 - abs(x)")
    public boolean linear = false;

    @ModuleParameter
    @ModuleDescription("use the given weight gain, e.g. exp(-gain*x^2))")
    public double gain = 12;

    @ModuleParameter
    @ModuleDescription("the z-score for filtering")
    public Double zscore = 3.0;

    @ModuleParameter
    @ModuleDescription("use Tukey's biweight m-estimator for robust statistics")
    public boolean mest = false;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output mesh")
    public Mesh output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output weights")
    public Volume weights;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output num vects")
    public Vects num;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output mean vects")
    public Vects median;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output mean vects")
    public Vects mean;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output std vects")
    public Vects std;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output min vects")
    public Vects min;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output max vects")
    public Vects max;

    @Override
    public Module run()
    {
        Mesh mesh = this.inplace ? this.mesh : this.mesh.copy();
        this.weights = this.input.proto(1);

        Sampling sampling = this.input.getSampling();
        VectFunction mydata = VolumeUtils.interp(InterpolationType.Trilinear, this.input);
        VectFunction mygm = VectFunctionSource.constant(VectSource.create1D(1.0));

        if (this.gm != null)
        {
            mygm = VolumeUtils.interp(InterpolationType.Trilinear, this.gm);
        }

        Vects mynum = new Vects();
        Vects mymedian = new Vects();
        Vects mymean = new Vects();
        Vects mystd = new Vects();
        Vects mymin = new Vects();
        Vects mymax = new Vects();

        for (Vertex vertex : mesh.vattr)
        {
            Vect mypial = mesh.vattr.get(vertex, this.pial);
            Vect mymiddle = mesh.vattr.get(vertex, this.middle);
            Vect mywhite = mesh.vattr.get(vertex, this.white);
            Vect orient = mypial.minus(mywhite).normalize();

            Vect start = mywhite.minus(this.inner, orient);
            Vect end = mypial.plus(this.outer, orient);
            Vect delta = end.minus(start);

            List<Double> weightRawValues = Lists.newArrayList();
            List<Double> dataRawValues = Lists.newArrayList();
            List<Vect> posRawValues = Lists.newArrayList();

            for (int i = 0; i < this.samples; i++)
            {
                double alpha = i / (double) (this.samples - 1);
                double reach = alpha - 0.5;
                double weight = this.linear || this.gain <= 0 ? 1.0 - Math.abs(reach) : Math.exp(-this.gain * reach * reach);

                Vect mypos = start.plus(alpha, delta);

                if (this.input.valid(sampling.nearest(mypos), this.mask))
                {
                    double data = mydata.apply(mypos).get(0);
                    weight *= mygm.apply(mypos).get(0);

                    posRawValues.add(mypos);
                    weightRawValues.add(weight);
                    dataRawValues.add(data);
                }
            }

            if (dataRawValues.size() > 0)
            {
                VectStats dataRawStats = new VectStats().withInput(VectSource.create(dataRawValues)).withWeights(VectSource.create(weightRawValues)).run();

                List<Vect> posValues = Lists.newArrayList();
                List<Double> weightValues = Lists.newArrayList();
                List<Double> dataValues = Lists.newArrayList();

                for (int i = 0; i < dataRawValues.size(); i++)
                {
                    double dataValue = dataRawValues.get(i);
                    double dataZscore = Math.abs(dataValue - dataRawStats.mean) / dataRawStats.std;

                    if (dataZscore < this.zscore)
                    {
                        posValues.add(posRawValues.get(i));
                        weightValues.add(weightRawValues.get(i));
                        dataValues.add(dataValue);
                    }
                }

                if (dataValues.size() == 0)
                {
                    if (weightRawValues.size() > 0)
                    {
                        int maxidx = VectUtils.maxidx(VectSource.create(weightRawValues));

                        posValues.add(posRawValues.get(maxidx));
                        weightValues.add(weightRawValues.get(maxidx));
                        dataValues.add(dataRawValues.get(maxidx));
                    }
                    else
                    {
                        posValues.add(mymiddle);
                        weightValues.add(1.0);
                        dataValues.add(0.0);
                    }
                }

                if (dataValues.size() > 0)
                {
                    Vect weights = VectSource.create(weightValues);
                    weights.divSafeEquals(weights.sum());

                    VectsStats posStats = new VectsStats().withInput(VectsSource.create(posValues)).withWeights(weights).withRobust(this.mest).run();
                    VectStats dataStats = new VectStats().withInput(VectSource.create(dataValues)).withWeights(weights).withRobust(this.mest).run();

                    int maxidx = VectUtils.maxidx(weights);

                    mesh.vattr.set(vertex, this.name + "_position", posStats.mean);
                    mesh.vattr.set(vertex, this.name + "_mid", VectSource.create1D(dataValues.get(maxidx)));
                    mesh.vattr.set(vertex, this.name + "_num", VectSource.create1D(dataStats.num));
                    mesh.vattr.set(vertex, this.name + "_median", VectSource.create1D(dataStats.median));
                    mesh.vattr.set(vertex, this.name + "_mean", VectSource.create1D(dataStats.mean));
                    mesh.vattr.set(vertex, this.name + "_std", VectSource.create1D(dataStats.std));
                    mesh.vattr.set(vertex, this.name + "_min", VectSource.create1D(dataStats.min));
                    mesh.vattr.set(vertex, this.name + "_max", VectSource.create1D(dataStats.max));

                    mynum.add(VectSource.create1D(dataStats.num));
                    mymean.add(VectSource.create1D(dataStats.median));
                    mymedian.add(VectSource.create1D(dataStats.mean));
                    mystd.add(VectSource.create1D(dataStats.std));
                    mymin.add(VectSource.create1D(dataStats.min));
                    mymax.add(VectSource.create1D(dataStats.max));

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
        this.num = mynum;
        this.median = mymedian;
        this.mean = mymean;
        this.std = mystd;
        this.min = mymin;
        this.max = mymax;

        return this;
    }
}
