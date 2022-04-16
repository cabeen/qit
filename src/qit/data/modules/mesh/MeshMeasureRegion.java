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

package qit.data.modules.mesh;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mesh;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.TableSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.TableUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Measure regional attributes properties of a mesh")
@ModuleAuthor("Ryan Cabeen")
public class MeshMeasureRegion implements Module
{
    @ModuleInput
    @ModuleDescription("input mesh")
    public Mesh input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input lookup table")
    public Table lookup;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("add the following per-vertex labeling to use for computing region statistics")
    public Vects labels;

    @ModuleParameter
    @ModuleDescription("compute statistics of the following attributes (a comma-delimited list can be used)")
    public String attr;

    @ModuleParameter
    @ModuleDescription("use the given vertex attribute to identify regions and compute statistics")
    public String label = "label";

    @ModuleParameter
    @ModuleDescription("the lookup name field")
    public String lutname = "name";

    @ModuleParameter
    @ModuleDescription("the lookup index field")
    public String lutindex= "index";

    @ModuleParameter
    @ModuleDescription("the output name field")
    public String outname = "name";

    @ModuleParameter
    @ModuleDescription("the output value field")
    public String outvalue = "value";

    @ModuleParameter
    @ModuleDescription("output directory")
    public String output;

    @Override
    public MeshMeasureRegion run() throws IOException
    {
        Map<Integer, String> naming = Maps.newHashMap();

        if (this.lookup != null)
        {
            naming = TableUtils.createIntegerStringLookup(this.lookup, this.lutindex, this.lutname);
        }

        Mesh mesh = this.input;

        if (this.labels != null)
        {
            MeshSetVects setter = new MeshSetVects();
            setter.mesh = mesh;
            setter.inplace = true;
            setter.name = this.label;
            setter.vects = this.labels;
            mesh = setter.run().output;
        }

        PathUtils.mkdirs(this.output);

        for (String myattr : this.attr.split(","))
        {
            Map<Integer, VectOnlineStats> mystats = Maps.newHashMap();

            for (Vertex myvert : mesh.vattr)
            {
                int mylabel = MathUtils.round(mesh.vattr.get(myvert, this.label).get(0));
                double myvalue = mesh.vattr.get(myvert, myattr).get(0);

                if (mylabel != 0)
                {
                    if (!mystats.containsKey(mylabel))
                    {
                        mystats.put(mylabel, new VectOnlineStats());
                    }

                    mystats.get(mylabel).update(myvalue);
                }
            }

            Set<String> meas = Sets.newHashSet();
            Map<Integer, Map<String, Double>> mylookup = Maps.newHashMap();
            for (Integer mylabel : mystats.keySet())
            {
                mylookup.put(mylabel, mystats.get(mylabel).lookup());
                if (meas.size() == 0)
                {
                    meas.addAll(mylookup.get(mylabel).keySet());
                }

                if (!naming.containsKey(mylabel))
                {
                    naming.put(mylabel, String.format("region%d", mylabel));
                }
            }

            for (String mymeas : meas)
            {
                Table map = new Table();
                map.withField(this.outname);
                map.withField(this.outvalue);

                for (Integer mylabel : mylookup.keySet())
                {
                    String myname = naming.get(mylabel);
                    double myvalue = mylookup.get(mylabel).get(mymeas);
                    map.addRecord(new Record().with(this.outname, myname).with(this.outvalue, myvalue));
                }

                map.write(PathUtils.join(this.output, String.format("%s_%s.csv", myattr, mymeas)));
            }
        }

        return this;
    }
}
