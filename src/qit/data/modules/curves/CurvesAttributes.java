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

package qit.data.modules.curves;

import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.utils.CurvesUtils;

import java.util.Set;

@ModuleDescription("Manipulate curves vertex attributes.  Operations support comma-delimited lists")
@ModuleAuthor("Ryan Cabeen")
public class CurvesAttributes implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("copy attribute (x=y syntax)")
    public String copy = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("rename attribute (x=y syntax)")
    public String rename = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("remove attribute (- for all)")
    public String remove = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("retain the given attributes and remove others)")
    public String retain = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    @Override
    public CurvesAttributes run()
    {
        Curves curves = this.inplace ? this.input : this.input.copy();
        
        if (this.retain != null)
        {
            Set<String> attrs = Sets.newHashSet(this.retain.split(","));
            attrs.add(Curves.COORD); // always keep the coordinates

            for (String attr : Sets.newHashSet(curves.names()))
            {
                if (!attrs.contains(attr))
                {
                    curves.remove(attr);
                }
            }
        }

        if (this.remove != null)
        {
            for (String attr : this.remove.split(","))
            {
                Global.assume(attr != Curves.COORD, "cannot remove coordinate attribute");
                curves.remove(attr);
            }
        }

        if (this.copy != null)
        {
            for (String pair : this.copy.split(","))
            {
                String[] tokens = pair.split("=");
                Global.assume(tokens.length == 2, "expected pattern x=y");

                String to = tokens[0];
                String from = tokens[1];

                CurvesUtils.attrCopy(curves, from, to);
            }
        }

        if (this.rename != null)
        {
            for (String pair : this.rename.split(","))
            {
                String[] tokens = pair.split("=");
                Global.assume(tokens.length == 2, "expected pattern x=y");

                String to = tokens[0];
                String from = tokens[1];

                curves.rename(from, to);
            }
        }

        this.output = curves;

        return this;
    }
}
