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
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskClose;
import qit.data.modules.mask.MaskFilterMode;
import qit.data.modules.mask.MaskSet;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.source.MaskSource;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.source.VolumeSource;
import qit.data.utils.VectUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.InterpolationType;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@ModuleDescription("Compute the force field of a cortical mesh")
@ModuleAuthor("Ryan Cabeen")
public class MeshForceCortex implements Module
{
    @ModuleInput
    @ModuleDescription("input mesh")
    public Mesh input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference volume (exclusive with refmask)")
    public Volume refvolume;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference mask (exclusive with refvolume)")
    public Mask refmask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input lookup table (must store the index and name of each label)")
    public Table lookup;

    @ModuleParameter
    @ModuleDescription("pial attribute name")
    public String pial = "pial";

    @ModuleParameter
    @ModuleDescription("white attribute name")
    public String white = "white";

    @ModuleParameter
    @ModuleDescription("parcellation attribute name")
    public String parc = "aparc";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("only process certain parcellation labels (by default all will be processed)")
    public String which = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the name field in the lookup table")
    public String name = "name";

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the index field in the lookup table")
    public String index = "index";

    @ModuleParameter
    @ModuleDescription("the minimum white-pial to be included")
    public double mindist = 1;

    @ModuleParameter
    @ModuleDescription("the number of points to sample")
    public int samples = 15;

    @ModuleParameter
    @ModuleDescription("the inner buffer size")
    public double inner = 2.0;

    @ModuleParameter
    @ModuleDescription("the outer buffer size")
    public double outer = 2.0;

    @ModuleParameter
    @ModuleDescription("skip the filling step")
    public boolean nofill = false;

    @ModuleParameter
    @ModuleDescription("apply fiber smoothing after projection")
    public boolean smooth = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("apply smoothing with the given amount (bandwidth in mm) (default is largest voxel dimension)")
    public Double sigma = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the smoothing filter radius in voxels")
    public int support = 3;

    @ModuleParameter
    @ModuleDescription("the number of threads")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("the output vectors")
    public Volume output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output labels")
    public Mask labels;

    @Override
    public MeshForceCortex run()
    {
        Global.assume(this.refmask != null ^ this.refvolume != null, "only refmask or refvolume must be specified but not both");
        Sampling sampling = this.refmask == null ? this.refvolume.getSampling() : this.refmask.getSampling();

        Mesh mesh = this.input;

        Volume myvects = VolumeSource.create(sampling, 3);
        Volume myweights = VolumeSource.create(sampling, 1);
        Volume mysums = VolumeSource.create(sampling, 1);
        Mask mylabels = MaskSource.create(sampling);

        List<Integer> mywhich = CliUtils.parseWhichWithLookup(this.which, this.lookup, this.name, this.index);

        if (this.which == null ^ mywhich.size() == 0)
        {
            this.output = myvects;
            this.labels = mylabels;

            return this;
        }

        Function<Vertex,Integer> getlabel = (vertex) ->
        {
            if (this.parc == null || !mesh.vattr.has(this.parc))
            {
                return 0;
            }
            else
            {
                return (int) mesh.vattr.get(vertex, this.parc).get(0);
            }
        };

        for (Vertex vertex : mesh.vattr)
        {
            int mylabel = getlabel.apply(vertex);
            Vect mypial = mesh.vattr.get(vertex, this.pial);
            Vect mywhite = mesh.vattr.get(vertex, this.white);
            Vect myvect = mypial.minus(mywhite);
            Vect mydir = myvect.normalize();

            if (myvect.norm() < this.mindist)
            {
                continue;
            }

            if (mywhich.size() > 0 && !mywhich.contains(mylabel))
            {
                continue;
            }

            Vect start = mywhite.minus(this.inner, mydir);
            Vect end = mypial.plus(this.outer, mydir);
            Vect delta = end.minus(start);

            for (int i = 0; i < this.samples; i++)
            {
                double alpha = i / (double) (this.samples - 1);
                double myweight = 1.0 - 2.0 * Math.abs(alpha - 0.5);

                Vect mypos = start.plus(alpha, delta);
                Sample sample = sampling.nearest(mypos);

                if (myvects.valid(sampling.nearest(mypos)))
                {
                    double sum = mysums.get(sample, 0);
                    Vect vect = myvects.get(sample);

                    mysums.set(sample, sum + myweight);
                    myvects.set(sample, vect.plus(mydir.times(myweight * myweight)));

                    double weight = myweights.get(sample).get(0);

                    if (mylabels.background(sample) || weight < myweight)
                    {
                        mylabels.set(sample, mylabel);
                        myweights.set(sample, weight);
                    }
                }
            }
        }

        for (Sample sample : sampling)
        {
            double sum = mysums.get(sample, 0);
            if (MathUtils.nonzero(sum))
            {
                myvects.set(sample, myvects.get(sample).times(1.0 / (double) sum));
            }
        }

        if (!this.nofill)
        {
            Mask prevlabels = mylabels;

            Logging.info("filling");
            MaskFilterMode moder = new MaskFilterMode();
            moder.input = mylabels;
            mylabels = moder.run().output;

            MaskSet setter = new MaskSet();
            setter.input = mylabels;
            setter.mask = prevlabels;
            setter.label = 0;
            Mask fix = setter.run().output;

            VolumeFilterGaussian filter = new VolumeFilterGaussian();
            filter.input = myvects;
            filter.mask = fix;
            filter.sigma = 2.0 * myvects.getSampling().deltaMax();
            filter.support = this.support;
            filter.threads = this.threads;
            myvects = filter.run().output;
        }

        if (this.smooth)
        {
            final double mysigma = this.sigma != null ? this.sigma : myvects.getSampling().deltaMax();

            Logging.info("smoothing");
            VolumeFilterGaussian filter = new VolumeFilterGaussian();
            filter.input = myvects;
            filter.sigma = mysigma;
            filter.support = this.support;
            filter.threads = this.threads;
            myvects = filter.run().output;
        }

        this.output = myvects;
        this.labels = mylabels;

        return this;
    }
}
