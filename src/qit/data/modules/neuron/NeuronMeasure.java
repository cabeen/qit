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
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mesh;
import qit.data.datasets.Neuron;
import qit.data.datasets.Neuron.Index;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.modules.mesh.MeshMeasure;
import qit.data.source.VectSource;
import qit.data.utils.NeuronUtils;
import qit.data.utils.VectUtils;
import qit.data.utils.vects.stats.VectOnlineStats;

@ModuleDescription("Measure statistics of a neuron file")
@ModuleAuthor("Ryan Cabeen")
public class NeuronMeasure implements Module
{
    @ModuleInput
    @ModuleDescription("the input neuron")
    public Neuron input;

    @ModuleParameter
    @ModuleDescription("include statistics for single neurons")
    public boolean single = false;

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public NeuronMeasure run()
    {
        Neuron neuron = this.input.copy();

        Table table = new Table();

        if (this.single)
        {
            table.withField("root");
            table.withField("leaves");
            table.withField("forks");
            table.withField("branches");
            table.withField("childmean");
            table.withField("childmax");
        }
        else
        {
            table.withField("name");
            table.withField("value");

            Index index = neuron.index();
            table.addRecord(new Record().with("name", "nodes").with("value", neuron.nodes.size()));
            table.addRecord(new Record().with("name", "roots").with("value", index.roots.size()));
            table.addRecord(new Record().with("name", "leaves").with("value", index.leaves.size()));
            table.addRecord(new Record().with("name", "forks").with("value", index.forks.size()));
        }

        VectOnlineStats leavesStats = new VectOnlineStats();
        VectOnlineStats forksStats = new VectOnlineStats();
        VectOnlineStats branchStats = new VectOnlineStats();
        VectOnlineStats childMeanStats = new VectOnlineStats();
        VectOnlineStats childMaxStats = new VectOnlineStats();
        VectOnlineStats spaceStats = new VectOnlineStats();

        Map<Integer, Neuron> split = NeuronUtils.split(neuron);
        for (Integer root : split.keySet())
        {
            Neuron myneuron = split.get(root);
            Index myindex = myneuron.index();

            int myleaves = myindex.leaves.size();
            int myforks = myindex.forks.size();
            int mybranches = 0;
            double childMean = 0;
            double childMax = 0;

            VectOnlineStats mystats = new VectOnlineStats();
            for (Integer idx : myindex.trunks)
            {
                Neuron.Node anode = myindex.map.get(idx);

                if (anode.parent >= 0)
                {
                    Neuron.Node bnode = myindex.map.get(anode.parent);

                    Vect apos = VectSource.create3D(anode.xpos, anode.ypos, anode.zpos);
                    Vect bpos = VectSource.create3D(bnode.xpos, bnode.ypos, bnode.zpos);
                    double dist = apos.dist(bpos);

                    mystats.update(dist);
                    spaceStats.update(dist);
                }
            }

            if (myindex.children.containsKey(root))
            {
                mybranches = myindex.children.get(root);

                VectOnlineStats mychildStats = new VectOnlineStats();
                for (int forks : myindex.forks)
                {
                    mychildStats.update(myindex.children.get(forks));
                }

                childMean = mychildStats.num == 0 ? 0 : mychildStats.mean;
                childMax = mychildStats.num == 0 ? 0 : mychildStats.max;
            }

            if (this.single)
            {
                Record record = new Record();
                record.with("root", root);
                record.with("leaves", myleaves);
                record.with("forks", myforks);
                record.with("branches", mybranches);
                record.with("childmean", childMean);
                record.with("childmax", childMax);
                record.with("spacemin", mystats.min);
                record.with("spacemax", mystats.max);
                record.with("spacemean", mystats.mean);

                table.addRecord(record);
            }

            leavesStats.update(myleaves);
            forksStats.update(myforks);
            branchStats.update(mybranches);
            childMeanStats.update(childMean);
            childMaxStats.update(childMax);
        }

        double leaves = leavesStats.num == 0 ? 0 : leavesStats.mean;
        double forks = forksStats.num == 0 ? 0 : forksStats.mean;
        double branches = branchStats.num == 0 ? 0 : branchStats.mean;
        double childMean = childMeanStats.num == 0 ? 0 : childMeanStats.mean;
        double childMax = childMaxStats.num == 0 ? 0 : childMaxStats.max;

        if (!single)
        {
            table.addRecord(new Record().with("name", "leaves").with("value", leaves));
            table.addRecord(new Record().with("name", "forks").with("value", forks));
            table.addRecord(new Record().with("name", "branches").with("value", branches));
            table.addRecord(new Record().with("name", "childmean").with("value", childMean));
            table.addRecord(new Record().with("name", "childmax").with("value", childMax));
            table.addRecord(new Record().with("name", "spacemin").with("value", spaceStats.min));
            table.addRecord(new Record().with("name", "spacemax").with("value", spaceStats.max));
            table.addRecord(new Record().with("name", "spacemean").with("value", spaceStats.mean));
        }

        this.output = table;

        return this;
    }

    public static Table apply(Neuron data)
    {
        return new NeuronMeasure()
        {{
            this.input = data;
        }}.run().output;
    }
}
