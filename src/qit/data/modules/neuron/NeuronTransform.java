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

import java.io.IOException;
import java.util.Map;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.base.utils.PathUtils;
import qit.data.datasets.Affine;
import qit.data.datasets.Neuron;
import qit.data.datasets.Neuron.Node;
import qit.data.utils.NeuronUtils;
import qit.data.utils.VectsUtils;
import qit.data.utils.VolumeUtils;
import qit.math.source.VectFunctionSource;

@ModuleDescription("Apply a spatial transformation to a neuron")
@ModuleAuthor("Ryan Cabeen")
public class NeuronTransform implements Module
{
    @ModuleInput
    @ModuleDescription("the neuron")
    public Neuron input;

    @ModuleParameter
    @ModuleDescription("translate the neuron position in x by the given amount")
    public double xshift = 0;

    @ModuleParameter
    @ModuleDescription("translate the neuron position in y by the given amount")
    public double yshift = 0;

    @ModuleParameter
    @ModuleDescription("translate the neuron position in z by the given amount")
    public double zshift = 0;

    @ModuleParameter
    @ModuleDescription("scale the neuron position in x by the given amount")
    public double xscale = 1;

    @ModuleParameter
    @ModuleDescription("scale the neuron position in y by the given amount")
    public double yscale = 1;

    @ModuleParameter
    @ModuleDescription("scale the neuron position in z by the given amount")
    public double zscale = 1;

    @ModuleParameter
    @ModuleDescription("add the given amount to the radius")
    public double rshift = 0;

    @ModuleParameter
    @ModuleDescription("scale the neuron radius by the given amount")
    public double rscale = 1;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("set the radius to a constant value")
    public Double rset = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("jitter the position of the nodes by a random amount")
    public Double jitter = null;

    @ModuleParameter
    @ModuleDescription("jitter only leaves and roots")
    public boolean jitterEnds  = false;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an affine xfm")
    public Affine affine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("apply an inverse affine xfm")
    public Affine invaffine;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("swap a pair of coordinates (xy, xz, or yz)")
    public String swap = null;

    @ModuleOutput
    @ModuleDescription("output neuron")
    public Neuron output;

    @Override
    public NeuronTransform run()
    {
        Neuron neuron = this.input.copy();

        neuron.shift(this.xshift, this.yshift, this.zshift);
        neuron.scale(this.xscale, this.yscale, this.zscale);

        for (Node node : neuron.nodes)
        {
            if (this.rset != null)
            {
                node.radius = this.rset;
            }
            else
            {
                node.radius += this.rshift;
                node.radius *= this.rscale;
            }
        }

        if (this.jitter != null)
        {
            if (this.jitterEnds)
            {
                neuron = NeuronUtils.jitter(neuron, this.jitter, true, false, true, false);
            }
            else
            {
                neuron = NeuronUtils.jitter(neuron, this.jitter, false, false, false, true);
            }
        }

        if (this.swap != null)
        {
            String lowswap = this.swap.toLowerCase();
            if (lowswap.equals("xy") || lowswap.equals("yx"))
            {
                neuron = NeuronUtils.apply(neuron, VectFunctionSource.swap(0, 1, 3));
            }
            else if (lowswap.equals("yz") || lowswap.equals("zy"))
            {
                neuron = NeuronUtils.apply(neuron, VectFunctionSource.swap(1, 2, 3));
            }
            else if (lowswap.equals("xz") || lowswap.equals("zx"))
            {
                neuron = NeuronUtils.apply(neuron, VectFunctionSource.swap(0, 2, 3));
            }
            else
            {
                Logging.error("unrecognized swap: " + this.swap);
            }
        }

        if (this.affine != null)
        {
            neuron = NeuronUtils.apply(neuron, this.affine);
        }

        if (this.invaffine != null)
        {
            neuron = NeuronUtils.apply(neuron, this.affine.inv());
        }

        this.output = neuron;

        return this;
    }
}
