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

package qit.data.modules.mask;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.utils.MaskUtils;
import qit.math.structs.DisjointSet;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Compute connected components of a mask.  The output will be sorted by the number of voxels per component, e.g. the largest component will have label 1, and the second largest will have label 2, etc.")
@ModuleAuthor("Ryan Cabeen")
public class MaskComponents implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleParameter
    @ModuleDescription("use a full 27-voxel neighborhood (default is 6-voxel)")
    public boolean full = false;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("include only specific component labels, e.g. \"1,2\" would select the two largest components")
    public String which = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("filter out components with fewer then the given number of voxels")
    public Integer minvoxels = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("filter out components that are smaller in volume than the given threshold")
    public Double minvolume = null;

    @ModuleParameter
    @ModuleDescription("keep the input labels (only relevant to filtering options)")
    public boolean keep = false;

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskComponents run()
    {
        List<Integers> neighborhood = this.full ? Global.NEIGHBORS_27 : Global.NEIGHBORS_6;

        Sampling samples = this.input.getSampling();
        Mask out = this.input.proto();
        DisjointSet<Integer> matching = new DisjointSet<>();
        int maxlabel = 1;

        for (Sample sample : samples)
        {
            int value = this.input.get(sample);
            if (value == 0)
            {
                continue;
            }

            List<Integer> nlabels = Lists.newArrayList();
            for (Integers offset : neighborhood)
            {
                Sample nsample = new Sample(sample, offset);

                if (!samples.contains(nsample))
                {
                    continue;
                }

                int nvalue = this.input.get(nsample);
                if (nvalue == 0 || value != nvalue)
                {
                    continue;
                }

                int nlabel = out.get(nsample);
                if (nlabel != 0)
                {
                    nlabels.add(nlabel);
                }
            }

            if (nlabels.size() == 0)
            {
                matching.add(maxlabel);
                out.set(sample, maxlabel);
                maxlabel += 1;
            }
            else
            {
                int minlabel = 0;
                int mincount = Integer.MAX_VALUE;
                for (Integer nlabel : nlabels)
                {
                    int count = matching.getSize(nlabel);
                    if (count < mincount)
                    {
                        minlabel = nlabel;
                        mincount = count;
                    }
                }

                out.set(sample, minlabel);

                for (int a : nlabels)
                {
                    for (int b : nlabels)
                    {
                        if (a != b)
                        {
                            matching.join(a, b);
                        }
                    }
                }
            }
        }

        Map<Integer, Integer> counts = Maps.newHashMap();
        for (Sample sample : samples)
        {
            int label = out.get(sample);
            if (label == 0)
            {
                continue;
            }

            int nlabel = matching.find(label);

            out.set(sample, nlabel);

            int prev = counts.containsKey(nlabel) ? counts.get(nlabel) : 0;
            counts.put(nlabel, prev + 1);
        }

        Map<Integer, Integer> lookup = MathUtils.remap(counts);

        Set<Integer> idx = Sets.newHashSet(CliUtils.parseWhich(this.which));

        for (Sample sample : samples)
        {
            int label = out.get(sample);
            if (label == 0)
            {
                continue;
            }

            int nlabel = lookup.get(label);
            double voxvol = samples.voxvol();

            if (this.which != null && !idx.contains(nlabel))
            {
                out.set(sample, 0);
            }
            else if (this.minvoxels != null && counts.get(label) <= this.minvoxels)
            {
                out.set(sample, 0);
            }
            else if (this.minvolume != null && counts.get(label) * voxvol <= this.minvolume)
            {
                out.set(sample, 0);
            }
            else
            {
                out.set(sample, nlabel);
            }
        }

        if (this.keep)
        {
            Logging.info("preserving mask");
            out = MaskUtils.mask(this.input, out);
        }

        this.output = out;

        return this;
    }

    public static Mask apply(Mask mask)
    {
        MaskComponents comper = new MaskComponents();
        comper.input = mask;
        return comper.run().output;
    }
}

