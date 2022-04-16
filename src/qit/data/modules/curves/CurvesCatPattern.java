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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.cli.CliUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Curves;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectStats;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Box;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.*;

@ModuleDescription("Concatenate curves using a filename pattern")
@ModuleAuthor("Ryan Cabeen")
public class CurvesCatPattern implements Module
{
    @ModuleParameter
    @ModuleDescription("a pattern to read curves filenames (should contains %s for substitution)")
    private String pattern;

    @ModuleParameter
    @ModuleDescription("a list of identifiers (will be substituted in the input pattern)")
    private String names;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("optionally use the given attribute to account for along bundle parameterization")
    private String along;

    @ModuleParameter
    @ModuleDescription("use the given bundle attribute/field name")
    private String bundleName = "bundle_name";

    @ModuleParameter
    @ModuleDescription("use the given along attribute/field name")
    private String alongName = "along_name";

    @ModuleParameter
    @ModuleDescription("use the given bundle attribute/field index")
    private String bundleIndex = "bundle_index";

    @ModuleParameter
    @ModuleDescription("use the given along attribute/field index")
    private String alongIndex = "along_index";

    @ModuleOutput
    @ModuleDescription("the output combined curves")
    private Curves curves = null;

    @ModuleOutput
    @ModuleDescription("the table to store labels")
    private Table table = null;

    @Override
    public CurvesCatPattern run()
    {
        try
        {
            int bundle = 1;
            int along = 1;

            Table out = new Table();
            out.withField(this.bundleName);
            out.withField(this.bundleIndex);

            if (this.along != null)
            {
                out.withField(this.along);
                out.withField(this.alongName);
                out.withField(this.alongIndex);
            }

            Curves cout = null;
            for (String name : CliUtils.names(this.names, null))
            {
                String fn = String.format(this.pattern, name);
                if (PathUtils.exists(fn))
                {
                    Logging.infosub("reading %s", fn);
                    Curves curves = Curves.read(fn);

                    if (this.along != null && !curves.has(this.along))
                    {
                        Logging.info("warning: along attribute not found, skipping");
                        continue;
                    }

                    curves.add(this.bundleIndex, VectSource.create1D());
                    if (this.along != null)
                    {
                        curves.add(this.alongIndex, VectSource.create1D());
                    }

                    Map<Integer,Integer> alongmap = Maps.newHashMap();
                    if (this.along != null)
                    {
                        Set<Integer> alongset = Sets.newHashSet();
                        for (Curves.Curve curve : curves)
                        {
                            for (int i = 0; i < curve.size(); i++)
                            {
                                alongset.add(MathUtils.round(curve.get(this.along, i).get(0)));
                            }
                        }

                        List<Integer> alonglist = Lists.newArrayList(alongset);
                        Collections.sort(alonglist);
                        for (Integer val : alonglist)
                        {
                            Record record = new Record();
                            record.with(this.bundleName, name);
                            record.with(this.bundleIndex, bundle);
                            record.with(this.along, val);
                            record.with(this.alongName, String.format("%s_%03d", name, val));
                            record.with(this.alongIndex, along);
                            out.addRecord(record);

                            alongmap.put(val, along);

                            along += 1;
                        }
                    }

                    for (Curves.Curve curve : curves)
                    {
                        for (int i = 0; i < curve.size(); i++)
                        {
                            curve.set(this.bundleIndex, i, VectSource.create1D(bundle));

                            if (this.along != null)
                            {
                                int val = MathUtils.round(curve.get(this.along, i).get(0));
                                curve.set(this.alongIndex, i, VectSource.create1D(alongmap.get(val)));
                            }
                        }
                    }

                    if (this.along == null)
                    {
                        Record record = new Record();
                        record.with(this.bundleName, name);
                        record.with(this.bundleIndex, bundle);
                        out.addRecord(record);
                    }

                    bundle += 1;

                    if (cout == null)
                    {
                        cout = curves;
                    }
                    else
                    {
                        cout.add(curves);
                    }
                }
            }

            this.curves = cout;
            this.table = out;
        }
        catch (
                IOException e)

        {
            e.printStackTrace();
            Logging.error(e.getMessage());
        }

        return this;
    }
}
