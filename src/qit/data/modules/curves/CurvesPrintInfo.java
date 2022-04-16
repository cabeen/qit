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

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Box;

@ModuleDescription("Print basic information about curves")
@ModuleAuthor("Ryan Cabeen")
public class CurvesPrintInfo implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    private Curves input;

    @ModuleParameter
    @ModuleDescription("print statistics")
    private boolean stats = false;

    @ModuleParameter
    @ModuleDescription("print statistics of curve lengths")
    private boolean length = false;

    @Override
    public Module run()
    {

        System.out.println("");
        System.out.println(String.format("  %s:", this.getClass().getSimpleName()));
        System.out.println("");
        System.out.println(String.format("    count: %d", this.input.size()));
        System.out.println("");
        System.out.println(String.format("    attributes:"));
        System.out.println("");
        for (String name : this.input.names())
        {
            System.out.println(String.format("      name: '%s', dim: %d", name, this.input.dim(name)));
        }
        System.out.println("");

        if (this.input.size() > 0)
        {
            Box b = this.input.bounds();
            System.out.println(String.format("    min X: %g", b.getMin().getX()));
            System.out.println(String.format("    min Y: %g", b.getMin().getY()));
            System.out.println(String.format("    min Z: %g", b.getMin().getZ()));
            System.out.println(String.format("    max X: %g", b.getMax().getX()));
            System.out.println(String.format("    max Y: %g", b.getMax().getY()));
            System.out.println(String.format("    max Z: %g", b.getMax().getZ()));
            System.out.println("");
        }

        if (this.stats)
        {
            for (String name : this.input.names())
            {
                VectsOnlineStats results = new VectsOnlineStats(this.input.dim(name));
                for (int i = 0; i < this.input.size(); i++)
                {
                    for (int j = 0; j < this.input.get(i).size(); j++)
                    {
                        results.update(this.input.get(i).get(name, j));
                    }
                }

                System.out.println(String.format("    attribute statistics for '%s':", name));
                System.out.println("");
                System.out.println(String.format("      mean: %s", results.mean));
                System.out.println(String.format("      std: %s", results.std));
                System.out.println(String.format("      var: %s", results.var));
                System.out.println(String.format("      min: %s", results.min));
                System.out.println(String.format("      max: %s", results.max));
                System.out.println("");
            }
        }

        if (this.length)
        {
            Vect lengths = VectSource.createND(this.input.size());
            for (int i = 0; i < this.input.size(); i++)
            {
                lengths.set(i, this.input.get(i).length());
            }

            VectStats results = new VectStats().withInput(lengths).run();

            System.out.println("    curve length statistics:");
            System.out.println("");
            System.out.println(String.format("      mean: %g", results.mean));
            System.out.println(String.format("      std: %g", results.std));
            System.out.println(String.format("      var: %g", results.var));
            System.out.println(String.format("      min: %g", results.min));
            System.out.println(String.format("      qlow: %g", results.qlow));
            System.out.println(String.format("      median: %g", results.median));
            System.out.println(String.format("      qhigh: %g", results.qhigh));
            System.out.println(String.format("      max: %g", results.max));
            System.out.println("");
        }

        return this;
    }
}
