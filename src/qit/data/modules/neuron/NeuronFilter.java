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


package qit.data.modules.neuron;

import com.google.common.collect.Maps;
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
import qit.base.structs.Pair;
import qit.data.datasets.Neuron;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.NeuronUtils;

import java.util.Map;

@ModuleDescription("Filter a neuron")
@ModuleAuthor("Ryan Cabeen")
public class NeuronFilter implements Module
{
    @ModuleInput
    @ModuleDescription("the input neuron")
    public Neuron input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("select only the neurons with the given root labels (comma-separated list)")
    public String which = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply laplacian smoothing the given number of times")
    public Integer laplacianIters = null;

    @ModuleParameter
    @ModuleDescription("the smoothing weight for laplacian smoothing (only relevant if smoothing is enabled)")
    public Double laplacianLambda = 0.25;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply lowess smoothing with the given neighborhood size, e.g. 5")
    public Integer lowessNum = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("apply lowess smoothing with the given local polynomial order")
    public Integer lowessOrder = 2;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("simplify the neuron segments with the Douglas-Peucker algorithm")
    public Double simplify = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("cut away the portion of the neuron past the given Euclidean distance away from the root")
    public Double cut = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("relabel the nodes to be sequential")
    public boolean relabel = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("perform a topological sorting of nodes")
    public boolean sort = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("separate the basal dendrite (detected by the soma branch with the farthest reach)")
    public boolean debase = false;

    @ModuleOutput
    @ModuleDescription("output neuron")
    public Neuron output;

    @Override
    public NeuronFilter run()
    {
        Neuron neuron = this.input.copy();

        if (this.which != null)
        {
            neuron = NeuronUtils.which(neuron, this.which);
        }

        if (this.laplacianIters != null)
        {
            neuron = NeuronUtils.laplacian(neuron, this.laplacianIters, this.laplacianLambda);
        }

        if (this.lowessNum != null)
        {
            neuron = NeuronUtils.lowess(neuron, this.lowessNum, this.lowessOrder);
        }

        if (this.simplify != null)
        {
            neuron = NeuronUtils.simplify(neuron, this.simplify);
        }

        if (this.cut != null)
        {
            neuron = NeuronUtils.cut(neuron, this.cut);
            neuron = NeuronUtils.relabel(neuron);
        }

        if (this.relabel)
        {
            neuron = NeuronUtils.relabel(neuron);
        }

        if (this.sort)
        {
            neuron = NeuronUtils.sort(neuron);
        }

        if (this.debase)
        {
            neuron = NeuronUtils.debase(neuron);
        }

        this.output = neuron;

        return this;
    }

    public static Neuron applySmoothLaplacian(Neuron data, int num)
    {
        return new NeuronFilter()
        {{
            this.input = data;
            this.laplacianIters = num;
        }}.run().output;
    }

    public static Neuron applySmoothLoess(Neuron data, int num)
    {
        return new NeuronFilter()
        {{
            this.input = data;
            this.lowessNum = num;
        }}.run().output;
    }

    public static Neuron applySimplify(Neuron data, double thresh)
    {
        return new NeuronFilter()
        {{
            this.input = data;
            this.simplify = thresh;
        }}.run().output;
    }

    public static Neuron applyCut(Neuron data, double thresh)
    {
        return new NeuronFilter()
        {{
            this.input = data;
            this.cut = thresh;
        }}.run().output;
    }

    public static Neuron applySort(Neuron data)
    {
        return new NeuronFilter()
        {{
            this.input = data;
            this.sort = true;
        }}.run().output;
    }

    public static Neuron applyRelabel(Neuron data)
    {
        return new NeuronFilter()
        {{
            this.input = data;
            this.relabel = true;
        }}.run().output;
    }
}
