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


package qit.data.modules.mri.dwi;

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
import qit.base.cli.CliUtils;
import qit.base.structs.Integers;
import qit.data.datasets.Sample;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.utils.mri.structs.Gradients;

import java.util.List;
import java.util.Map;

@ModuleDescription("Average subvolumes from diffusion MRI.  This is often used to improve the SNR when the scan includes repeats")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiAverage implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume dwi;

    @ModuleInput
    @ModuleDescription("the input gradients (must match input DWI)")
    public Gradients gradients;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a file or comma-separated list of integers specifying how the data should be grouped, e.g. 1,1,1,2,2,2,3,3,3,...")
    public String average = null;

    @ModuleOutput
    @ModuleDescription("the output dwi volume")
    public Volume outputDwi;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output gradients")
    public Gradients outputGradients;

    public VolumeDwiAverage run()
    {
        int dim = this.dwi.getDim();

        Logging.info("found dwi dimension: " + dim);
        Logging.info("found gradients dimension: " + this.gradients.size());
        Global.assume(dim == this.gradients.size(), "gradients and dwi dimensions must match");

        Integers labels = new Integers(CliUtils.parseWhich(this.average));

        Logging.info("found labels size: " + labels.size());
        Global.assume(dim == labels.size(), "labels and dwi dimensions must match");

        Map<Integer,Integer> counts = labels.counts();

        List<Integers> groups = Lists.newArrayList();
        for (Integer key : counts.keySet())
        {
            int[] group = new int[counts.get(key)];

            int idx = 0;
            for (int i = 0; i < dim; i++)
            {
                if (labels.get(i) == key)
                {
                    group[idx] = i;
                    idx += 1;
                }
            }

            Global.assume(idx == group.length, "bug found in group indexing");

            Integers ngroup = new Integers(group);
            groups.add(ngroup);
        }

        int ndim = groups.size();
        Logging.info("found number of groups: " + ndim);

        Volume outDwi = this.dwi.proto(ndim);
        for (Sample sample : this.dwi.getSampling())
        {
            Vect out = new Vect(ndim);

            for (int i = 0; i < ndim; i++)
            {
                out.set(i, this.dwi.get(sample).sub(groups.get(i)).mean());
            }

            outDwi.set(sample, out);
        }

        Vects bvals = this.gradients.getBvals();
        Vects bvecs = this.gradients.getBvecs();
        Vects nbvals = new Vects();
        Vects nbvecs = new Vects();

        for (int i = 0; i < ndim; i++)
        {
            Integers which = groups.get(i);
            nbvals.add(bvals.sub(which).mean());
            nbvecs.add(bvecs.sub(which).mean().normalize());
        }

        Gradients outGrads = new Gradients(nbvecs, nbvals);

        this.outputDwi = outDwi;
        this.outputGradients = outGrads;

        return this;
    }

}
