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

package qit.data.modules.mri.fibers;

import qit.base.Logging;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.utils.VolumeUtils;
import qit.data.utils.enums.KernelInterpolationType;
import qit.data.utils.enums.ReorientationType;
import qit.data.utils.volume.VolumeSample;
import qit.data.models.Fibers;
import qit.data.utils.mri.estimation.FibersEstimator;
import qit.data.utils.mri.estimation.VolumeKernelModelEstimator;
import qit.math.structs.VectFunction;
import qit.math.source.VectFunctionSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ModuleDescription("Spatially transform a fibers volume")
@ModuleCitation("Cabeen, R. P., Bastin, M. E., & Laidlaw, D. H. (2016). Kernel regression estimation of fiber orientation mixtures in mri MRI. NeuroImage, 127, 158-172.")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersTransform implements Module
{
    @ModuleInput
    @ModuleDescription("the input fibers volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("input reference volume")
    public Volume reference;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("use an input mask (defined in the input space)")
    public Mask inputMask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an affine xfm")
    public Affine affine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an inverse affine xfm")
    public Affine invaffine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply a deformation xfm")
    public Deformation deform;

    @ModuleParameter
    @ModuleDescription("reverse the order, i.e. compose the affine(deform(x)), whereas the default is deform(affine(x))")
    public boolean reverse = false;

    @ModuleParameter
    @ModuleDescription("specify a reorient method")
    public ReorientationType reorientation = ReorientationType.Jacobian;

    @ModuleParameter
    @ModuleDescription("the estimation type")
    public FibersEstimator.EstimationType estimation = FibersEstimator.EstimationType.Match;

    @ModuleParameter
    @ModuleDescription("the selection type")
    public FibersEstimator.SelectionType selection = FibersEstimator.SelectionType.Adaptive;

    @ModuleParameter
    @ModuleDescription("the interpolation type")
    public KernelInterpolationType interp = KernelInterpolationType.Trilinear;

    @ModuleParameter
    @ModuleDescription("a maxima number of fiber compartments")
    public int comps = 3;

    @ModuleParameter
    @ModuleDescription("the filter radius in voxels")
    public Integer support = 3;

    @ModuleParameter
    @ModuleDescription("the positional bandwidth in mm")
    public Double hpos = 1.0;

    @ModuleParameter
    @ModuleDescription("a minima volume fraction")
    public double min = 0.01;

    @ModuleParameter
    @ModuleDescription("a data adaptive threshold")
    public double lambda = 0.99;

    @ModuleParameter
    @ModuleDescription("the number of threads")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("the output transformed volume")
    public Volume output;

    public VolumeFibersTransform run()
    {
        Sampling sampling = this.reference.getSampling();
        final Volume myinput = this.inputMask != null ? VolumeUtils.mask(this.input, this.inputMask) : this.input;

        final Volume out = VolumeSource.create(sampling, new Fibers(this.comps).getEncodingSize());
        out.setModel(ModelType.Fibers);

        Matrix[] map = computeMap(sampling);

        Logging.progress("started transforming fibers volume");
        if (this.threads < 2)
        {
            for (int k = 0; k < sampling.numK(); k++)
            {
                processSlice(map, k, myinput, out);
            }
        }
        else
        {
            ExecutorService exec = Executors.newFixedThreadPool(this.threads);

            for (int k = 0; k < sampling.numK(); k++)
            {
                final int fk = k;

                exec.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        VolumeFibersTransform.this.processSlice(map, fk, myinput, out);
                    }
                });
            }

            exec.shutdown();
            try
            {
                exec.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            }
            catch (InterruptedException e)
            {
                Logging.error("failed to execute in concurrent mode");
            }
        }
        Logging.progress("finished transforming fibers volume");

        this.output = out;

        return this;
    }

    public void processSlice(Matrix[] map, int k, Volume myinput, Volume out)
    {
        Logging.progress("started resampling slice " + k);

        FibersEstimator estimator = new FibersEstimator();
        estimator.estimation = this.estimation;
        estimator.selection = this.selection;
        estimator.lambda = this.lambda;
        estimator.maxcomps = this.comps;
        estimator.minfrac = this.min;

        VolumeKernelModelEstimator vestimator = new VolumeKernelModelEstimator(new Fibers(this.comps));
        vestimator.estimator = estimator;
        vestimator.volume = myinput;
        vestimator.interp = this.interp;
        vestimator.hpos = this.hpos;
        vestimator.support = this.support;

        VectFunction xfm = VolumeUtils.xfm(this.affine, this.invaffine, this.deform, this.reverse);

        Sampling sampling = out.getSampling();

        for (int j = 0; j < sampling.numJ(); j++)
        {
            for (int i = 0; i < sampling.numI(); i++)
            {
                Sample sample = new Sample(i, j, k);

                if (!out.valid(sample, this.mask))
                {
                    continue;
                }

                Vect source = sampling.world(sample);
                Vect dest = xfm.apply(source);
                Vect sampled = vestimator.estimate(dest);

                if (sampled != null)
                {
                    out.set(sample, sampled);
                }
            }
        }

        for (int j = 0; j < sampling.numJ(); j++)
        {
            for (int i = 0; i < sampling.numI(); i++)
            {
                if (out.valid(i, j, k, this.mask))
                {
                    continue;
                }

                int idx = sampling.index(i, j, k);
                Fibers fibers = new Fibers(out.get(idx));

                // don't change fibers with no associated transform
                if (map[idx] != null)
                {
                    for (int c = 0; c < fibers.size(); c++)
                    {
                        Matrix rxfm = map[idx].inv();
                        Vect oriented = rxfm.times(fibers.getLine(c)).normalize();
                        fibers.setLine(c, oriented);
                        out.set(idx, fibers.getEncoding());
                    }
                }
            }
        }

        Logging.progress("finished resampling slice " + k);
    }

    private Matrix[] computeMap(Sampling reference)
    {
        Logging.progress("started computing reorientation function");
        VectFunction xfm = VolumeUtils.xfm(this.affine, this.invaffine, this.deform, this.reverse);
        Volume vxfm = new VolumeSample().withSampling(reference).withFunction(xfm).getOutput();
        final Matrix[] map = VolumeUtils.reorient(vxfm, this.mask, this.reorientation);
        Logging.progress("finished computing reorientation function");

        return map;
    }
}
