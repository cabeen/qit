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

package qit.data.modules.vects;

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
import qit.base.annot.ModuleParameter;
import qit.base.cli.CliUtils;
import qit.base.structs.Pair;
import qit.base.utils.PathUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.TableUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.data.utils.vects.stats.VectsStats;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Fuse a collection of vectors using kernel regression")
@ModuleAuthor("Ryan Cabeen")
public class VectsFuseKernel implements Module
{
    @ModuleParameter
    @ModuleDescription("an input pattern (must contain %s for case identifier)")
    public String input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input table")
    public Table table;

    @ModuleParameter
    @ModuleDescription("the name of the case identifier (the table should have a column by this name)")
    public String id = "id";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the continuous variable, if none is provided then no sampling is performed")
    public String scalar = null;

    @ModuleParameter
    @ModuleDescription("the values for scalar variable sampling")
    public String samples = "1,2,3,4,5,6,7,8,9,10";

    @ModuleParameter
    @ModuleDescription("the bandwidth for scalar variable sampling")
    public double sigma = 5;

    @ModuleParameter
    @ModuleDescription("output file pattern (must include a %s)")
    public String output;

    @Override
    public VectsFuseKernel run() throws IOException
    {
        Global.assume(this.table.hasField(this.id), "expected table to have matching id field: " + this.id);

        Logging.info("loading vects");
        List<String> ids = Lists.newArrayList();
        Map<String, Integer> keymap = Maps.newHashMap();
        Vects vects = new Vects();

        for (int key : this.table.keys())
        {
            String i = this.table.get(key, this.id);
            String fn = String.format(this.input, i);
            Logging.info("reading " + fn);
            if (PathUtils.exists(fn))
            {
                try
                {
                    vects.add(Vect.read(fn));
                    ids.add(i);
                    keymap.put(i, key);
                }
                catch (RuntimeException e)
                {
                    Logging.info("... error loading vects, skipping it");
                }
            }
            else
            {
                Logging.info("... vects not found, skipping it");
            }
        }
        Logging.info("vects count: " + vects.size());

        Logging.info("fusing");
        PathUtils.mkpar(this.output);

        if (this.scalar != null)
        {
            Global.assume(this.table.hasField(this.scalar), "expected table to have matching scalar field: " + this.scalar);
            Map<Pair<String, String>, Vect> weightMap = Maps.newHashMap();

            for (String scalarSample : CliUtils.names(this.samples))
            {
                Logging.info("... processing sample: " + scalarSample);

                Logging.info("...... computing weights");
                double sampleValue = Double.parseDouble(scalarSample);
                List<Double> weightList = Lists.newArrayList();
                List<Vect> valueList = Lists.newArrayList();

                for (int i = 0; i < ids.size(); i++)
                {
                    String myid = ids.get(i);
                    double caseValue = Double.valueOf(this.table.get(keymap.get(myid), this.scalar));

                    double diffValue = sampleValue - caseValue;
                    double kernel = Math.exp(-1 * this.sigma * diffValue * diffValue);

                    if (kernel > 1e-2)
                    {
                        weightList.add(kernel);
                        valueList.add(vects.get(i));
                    }
                }

                Logging.info("...... averaging with neighborhood of " + valueList.size());
                Vect weights = VectSource.create(weightList);
                Vects values = VectsSource.create(valueList);

                weights.divSafeEquals(weights.sum());
                Vect mean = new VectsStats().withInput(values).withWeights(weights).run().mean;

                String fn = String.format(this.output, scalarSample);
                Logging.info("...... writing: " + fn);
                mean.write(fn);
            }
        }
        else
        {
            Logging.info("...... averaging");
            Vect mean = new VectsStats().withInput(vects).run().mean;

            Logging.info("...... writing: " + this.output);
            mean.write(this.output);
        }

        Logging.info("finished");

        return this;
    }
}
