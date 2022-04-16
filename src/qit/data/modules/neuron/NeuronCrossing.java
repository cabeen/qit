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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Neuron;
import qit.data.datasets.Neuron.Index;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.structs.Segment;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ModuleDescription("Detect the crossing points between two neurons")
@ModuleAuthor("Ryan Cabeen")
public class NeuronCrossing implements Module
{
    enum NeuronCrossingMode { Min, Max, Mean}

    @ModuleInput
    @ModuleDescription("the input left neuron")
    public Neuron left;

    @ModuleInput
    @ModuleDescription("the input right neuron")
    public Neuron right;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the minimum distance to be considered a crossing (by default it will be the average inter-node distance)")
    public Double thresh = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("specify a statistic for computing the threshold (not relevant if you provide a specific value)")
    public NeuronCrossingMode mode = NeuronCrossingMode.Mean;

    @ModuleOutput
    @ModuleDescription("output vects")
    public Vects output;

    @Override
    public NeuronCrossing run()
    {
        Function<Neuron, List<Segment>> split = (neuron) ->
        {
            Neuron.Index index = neuron.index();

            List<Segment> out = Lists.newArrayList();
            for (Integer idx : index.trunks)
            {
                Neuron.Node anode = index.map.get(idx);

                if (anode.parent >= 0)
                {
                    Neuron.Node bnode = index.map.get(anode.parent);

                    Vect apos = VectSource.create3D(anode.xpos, anode.ypos, anode.zpos);
                    Vect bpos = VectSource.create3D(bnode.xpos, bnode.ypos, bnode.zpos);

                    out.add(new Segment(apos, bpos));
                }
            }

            return out;
        };

        List<Segment> myleft = split.apply(this.left);
        List<Segment> myright = split.apply(this.right);

        Double mythresh = this.thresh;

        if (mythresh == null)
        {
            Logging.info("computing automatic threshold");

            VectOnlineStats stats = new VectOnlineStats();

            for (Segment seg : myleft)
            {
                stats.update(seg.length());
            }

            for (Segment seg : myright)
            {
                stats.update(seg.length());
            }

            Logging.info("detected min spacing: " + stats.min);
            Logging.info("detected max spacing: " + stats.max);
            Logging.info("detected mean spacing: " + stats.mean);
            Logging.info("using spacing: " + this.mode);

            switch(this.mode)
            {
                case Min:
                    mythresh = stats.min;
                    break;
                case Max:
                    mythresh = stats.max;
                    break;
                case Mean:
                    mythresh = stats.mean;
                    break;
            }
        }

        Logging.info("using threshold: " + mythresh);

        Vects out = new Vects();
        for (Segment a : myleft)
        {
            for (Segment b : myright)
            {
                Pair<Double,Double> nearest = Segment.nearest(a, b);
                Vect ap = a.get(nearest.a);
                Vect bp = b.get(nearest.b);
                double dist = ap.dist(bp);

                if (dist < mythresh)
                {
                    Vect mean = ap.times(0.5).plus(0.5, bp);
                    out.add(mean.cat(VectSource.create1D(dist)));
                }
            }
        }

        Logging.infosub("found %d crossing points", out.size());

        this.output = out;

        return this;
    }
}
