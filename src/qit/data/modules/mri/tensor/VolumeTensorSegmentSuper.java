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

package qit.data.modules.mri.tensor;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.mri.model.VolumeModelFeature;
import qit.data.modules.mask.MaskComponents;
import qit.data.modules.mask.MaskSort;
import qit.data.source.MaskSource;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.vects.cluster.VectsClusterEM;
import qit.data.utils.vects.cluster.VectsClusterSADPM;
import qit.data.utils.vects.cluster.VectsClusterSAKM;
import qit.data.models.Tensor;

import java.util.Collections;
import java.util.List;

@ModuleDescription("Segment supervoxels from a tensor volume")
@ModuleAuthor("Ryan Cabeen")
public class VolumeTensorSegmentSuper implements Module
{
    @ModuleInput
    @ModuleDescription("the input tensor volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a weight volume")
    public Volume weights;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a threshold size (any region smaller than this will be removed)")
    public Integer size = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the number of iterations")
    public Integer iters = 10;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the number of restarts")
    public Integer restarts = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the number of clusters (the initial number if dp-means is used)")
    public Integer num = 100;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the alpha parameter for spatial extent")
    public Double alpha = VectsClusterSAKM.DEFAULT_ALPHA;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the beta parameter for angular extent")
    public Double beta = VectsClusterSAKM.DEFAULT_BETA;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the lambda parameter for region size (for dp-means clustering)")
    public Double lambda = null;

    @ModuleOutput
    @ModuleDescription("the output segmentation mask")
    public Mask output;

    public VolumeTensorSegmentSuper run()
    {
        Logging.info("clustering voxels");
        VectsClusterEM cluster = getCluster();

        List<Sample> samples = MaskUtils.samples(this.mask);
        Vects values_tmp = new Vects();
        List<Double> weights_tmp = Lists.newArrayList();
        Sampling sampling = this.input.getSampling();

        VolumeModelFeature feature = new VolumeModelFeature();
        feature.input = this.input;
        feature.model = Tensor.NAME;
        feature.feature = Tensor.FEATURES_FA;
        Volume fa = feature.run().output;

        for (Sample sample : samples)
        {
            Vect pos = sampling.world(sample);
            Vect dir = new Tensor(this.input.get(sample)).getVec(0);

            double weight;
            if (this.weights == null)
            {
                weight = fa.get(sample, 0);
            }
            else
            {
                weight = this.weights.get(sample, 0);
            }

            Vect cat = VectSource.createND(6);
            for (int i = 0; i < 3; i++)
            {
                cat.set(i, pos.get(i));
                cat.set(3 + i, dir.get(i));
            }

            values_tmp.add(cat);
            weights_tmp.add(weight);
        }

        Vect weights = VectSource.create(weights_tmp);
        Vects values = values_tmp;

        cluster.withVects(values);
        cluster.withWeights(weights);
        int[] clabels = cluster.getOutput();
        Mask labels = MaskSource.create(this.input.getSampling());

        for (int idx = 0; idx < clabels.length; idx++)
        {
            labels.set(samples.get(idx), clabels[idx]);
        }

        Logging.info("sorting labels");
        labels = MaskSort.sort(labels);

        Logging.info("finding connected components");
        MaskComponents comper = new MaskComponents();
        comper.input = labels;
        labels = comper.run().output;

        if (this.size != null)
        {
            Logging.info("filtering by size");
            labels = MaskUtils.filter(labels, this.size);
        }

        Logging.info("verifying labels");
        List<Integer> list = MaskUtils.listNonzero(labels, this.mask);
        Collections.sort(list);
        Global.assume(list.get(0) >= 1 && list.get(list.size() - 1) == list.size(), "invalid labels");

        this.output = labels;

        return this;
    }

    private VectsClusterEM getCluster()
    {
        VectsClusterSAKM cluster;

        if (this.lambda != null)
        {
            cluster = new VectsClusterSADPM().withLambda(this.lambda);
        }
        else
        {
            cluster = new VectsClusterSAKM();
        }

        if (this.alpha != null)
        {
            cluster.withAlpha(this.alpha);
        }

        if (this.beta != null)
        {
            cluster.withBeta(this.beta);
        }

        if (this.restarts != null)
        {
            cluster.withRestarts(this.restarts);
        }

        cluster.withMaxIter(this.iters);
        cluster.withK(this.num);

        return cluster;
    }
}
