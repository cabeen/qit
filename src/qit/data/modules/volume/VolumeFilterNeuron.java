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
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskDilate;
import qit.data.modules.mask.MaskGreater;
import qit.data.modules.mask.MaskUnion;
import qit.data.source.SamplingSource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@ModuleDescription("Filter a volume to isolate neuronal structures")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFilterNeuron implements Module
{
    @ModuleInput
    @ModuleDescription("input volume")
    public Volume input;

    @ModuleParameter
    @ModuleDescription("the number of median filter passes")
    Integer median = 1;

    @ModuleParameter
    @ModuleDescription("the number of dilation passes")
    Integer dilate = 1;

    @ModuleParameter
    @ModuleDescription("the minimum number of voxels in a neuron connected component")
    Integer min = 300;

    @ModuleParameter
    @ModuleDescription("the threshold for segmenting cell bodies")
    Double thresh = 0.01;

    @ModuleParameter
    @ModuleDescription("apply the Frangi filter")
    boolean frangi = false;

    @ModuleParameter
    @ModuleDescription("the threshold for Frangi segmentation")
    double frangiThresh = 0.01;

    @ModuleParameter
    @ModuleDescription("the number of scales used in the Frangi filter")
    int frangiScales = 5;

    @ModuleParameter
    @ModuleDescription("the lowest scale used in the Frangi filter")
    int frangiLow = 1;

    @ModuleParameter
    @ModuleDescription("the highest scale used in the Frangi filter")
    Integer frangiHigh = 10;

    @ModuleParameter
    @ModuleDescription("number of threads")
    public int threads = 1;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output frangi")
    public Volume outputFrangi = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output mask")
    public Mask outputMask = null;

    @ModuleOutput
    @ModuleDescription("output volume")
    public Volume output = null;

    public VolumeFilterNeuron run()
    {
        Volume out = this.input.copy();

        for (int i = 0; i < this.median; i++)
        {
            VolumeFilterMedian myMedian = new VolumeFilterMedian();
            myMedian.input = out;
            myMedian.threads = this.threads;
            out = myMedian.run().output;
        }

        VolumeThreshold myThresh = new VolumeThreshold();
        myThresh.input = out;
        myThresh.threshold = this.thresh;
        Mask mask = myThresh.run().output;

        if (this.frangi)
        {
            VolumeFilterFrangi myFrangi = new VolumeFilterFrangi();
            myFrangi.input = out;
            myFrangi.low = this.frangiLow;
            myFrangi.high = this.frangiHigh;
            myFrangi.scales = this.frangiScales;
            myFrangi.threads = this.threads;
            Volume frangi = myFrangi.run().output;

            VolumeThreshold myFrangiThresh = new VolumeThreshold();
            myFrangiThresh.input = frangi;
            myFrangiThresh.threshold = this.frangiThresh;
            Mask myMask = myFrangiThresh.run().output;

            MaskUnion myUnion = new MaskUnion();
            myUnion.left = mask;
            myUnion.right = myMask;
            mask = myUnion.run().output;

            this.outputFrangi = frangi;
        }

        for (int i = 0; i < this.dilate; i++)
        {
            MaskDilate myDilate = new MaskDilate();
            myDilate.input = mask;
            mask = myDilate.run().output;
        }

        MaskGreater myGreater = new MaskGreater();
        myGreater.input = mask;
        myGreater.minimum = this.min;
        mask = myGreater.run().output;

        VolumeMask myMask = new VolumeMask();
        myMask.input = this.input;
        myMask.mask = mask;
        out = myMask.run().output;

        this.output = out;
        this.outputMask = mask;

        return this;
    }
}
