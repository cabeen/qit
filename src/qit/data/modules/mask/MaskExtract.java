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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Table;
import qit.data.utils.TableUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Extract specific labels from a mask")
@ModuleAuthor("Ryan Cabeen")
public class MaskExtract implements Module
{
    public enum MaskExtractMode
    {
        Binary, Preserve, Distinct
    }

    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input lookup table (must store the index and name of each label)")
    public Table lookup;

    @ModuleParameter
    @ModuleDescription("the label(s) to extract (comma delimited, e.g. 1,2,3 or lh-temporal,rh-temporal if a lookup is used)")
    public String label = "1";

    @ModuleParameter
    @ModuleDescription("the mode for extracting labels")
    public MaskExtractMode mode = MaskExtractMode.Binary;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the name field in the lookup table")
    public String name = "name";

    @ModuleParameter
    @ModuleDescription("the index field in the lookup table")
    public String index = "index";

    @ModuleOutput
    @ModuleDescription("output mask")
    public Mask output;

    @Override
    public MaskExtract run()
    {
        List<Integer> labels = CliUtils.parseWhichWithLookup(this.label, this.lookup, this.name, this.index);
        this.output = extract(this.input, labels, this.mode);

        return this;
    }

    public static Mask extract(Mask volume, List<Integer> labels, MaskExtractMode mode)
    {
        Mask out = volume.proto();

        if (labels.size() == 0)
        {
            return out;
        }

        Map<Integer, Integer> mapping = Maps.newHashMap();
        for (Integer label : labels)
        {
            mapping.put(label, mapping.size() + 1);
        }

        for (Sample sample : volume.getSampling())
        {
            int plabel = volume.get(sample);
            if (labels.contains(plabel))
            {
                int nlabel = 1;

                switch (mode)
                {
                    case Binary:
                        nlabel = 1;
                        break;
                    case Preserve:
                        nlabel = plabel;
                        break;
                    case Distinct:
                        nlabel = mapping.get(plabel);
                        break;
                }

                out.set(sample, nlabel);
            }
        }

        return out;
    }

    public static Mask extract(Mask volume, int label)
    {
        Mask out = volume.proto();

        for (Sample sample : volume.getSampling())
        {
            if (label == volume.get(sample))
            {
                out.set(sample, 1);
            }
        }

        return out;
    }

    public static Mask apply(Mask mask)
    {
        return new MaskExtract()
        {{
            this.input = mask;
        }}.run().output;
    }

    public static Mask apply(Mask mask, int mylabel)
    {
        return new MaskExtract()
        {{
            this.input = mask;
            this.label = String.valueOf(mylabel);
        }}.run().output;
    }

    public static Mask apply(Mask mask, String mylabel)
    {
        return new MaskExtract()
        {{
            this.input = mask;
            this.label = mylabel;
        }}.run().output;
    }
}

