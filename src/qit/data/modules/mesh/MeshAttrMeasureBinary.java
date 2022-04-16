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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mesh;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.MeshUtils;
import qit.data.utils.TableUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.data.utils.vects.stats.VectStats;
import qit.math.structs.Face;
import qit.math.structs.Histogram;
import qit.math.structs.Triangle;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@ModuleDescription("Measure regional attributes properties of a mesh with a binary labelling")
@ModuleAuthor("Ryan Cabeen")
public class MeshAttrMeasureBinary implements Module
{
    @ModuleInput
    @ModuleDescription("input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("compute statistics of the following numerical attributes (a comma-delimited list can be used)")
    public String attr;

    @ModuleParameter
    @ModuleDescription("the name of the parcellation attribute")
    public String label = "label";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the name of the whole attribute")
    public String whole = "whole";

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("add a parcellation attribute")
    public Vects labels;

    @ModuleParameter
    @ModuleDescription("a prefix for naming")
    public String prefix = "";

    @ModuleParameter
    @ModuleDescription("the coordinate field (comma-separate list too)")
    public String coord = "coord";

    @ModuleParameter
    @ModuleDescription("the output table name field")
    public String outname = "name";

    @ModuleParameter
    @ModuleDescription("the output table value field")
    public String outvalue = "value";

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public MeshAttrMeasureBinary run()
    {
        Mesh mesh = this.input;

        if (this.labels != null)
        {
            MeshSetVects setter = new MeshSetVects();
            setter.mesh = mesh;
            setter.inplace = true;
            setter.name = this.label;
            setter.vects = this.labels;
            setter.run();
        }

        Table map = new Table();
        map.withField(this.outname);
        map.withField(this.outvalue);

        Function<Vertex, Boolean> checkWhole = (vertex) ->
                this.whole != null ? MathUtils.round(mesh.vattr.get(vertex, this.whole).get(0)) > 0 : true;

        Function<Vertex, Boolean> checkParcel = (vertex) ->
                MathUtils.round(mesh.vattr.get(vertex, this.label).get(0)) > 0;

        BiConsumer<String, Double> insert = (name, value) ->
                map.addRecord(new Record().with(this.outname, name).with(this.outvalue, value));

        if (this.attr != null)
        {
            for (String myattr : this.attr.split(","))
            {
                if (mesh.vattr.has(myattr))
                {
                    {
                        Vect valuesParcel = MeshUtils.values(mesh, checkParcel, myattr).flatten();
                        VectStats statsParcel = VectStats.stats(valuesParcel);
                        Map<String, Double> mapParcel = statsParcel.lookup();
                        mapParcel.put("mode", Histogram.create(valuesParcel).mode());

                        Vect valuesWhole = MeshUtils.values(mesh, checkWhole, myattr).flatten();
                        VectStats statsWhole = VectStats.stats(valuesWhole);
                        Map<String, Double> mapWhole = statsWhole.lookup();
                        mapWhole.put("mode", Histogram.create(valuesWhole).mode());

                        for (String key : mapParcel.keySet())
                        {
                            insert.accept(String.format("%sparcel_%s_%s", this.prefix, myattr, key), mapParcel.get(key));
                        }

                        for (String key : mapWhole.keySet())
                        {
                            insert.accept(String.format("%swhole_%s_%s", this.prefix, myattr, key), mapWhole.get(key));
                        }

                        double fractionSum = mapParcel.get("sum") / mapWhole.get("sum");
                        double fractionMean = mapParcel.get("mean") / mapWhole.get("mean");

                        fractionSum = Double.isFinite(fractionSum) ? fractionSum : 0;
                        fractionMean = Double.isFinite(fractionMean) ? fractionMean : 0;

                        insert.accept(String.format("%sfraction_%s_sum", this.prefix, myattr), fractionSum);
                        insert.accept(String.format("%sfraction_%s_mean", this.prefix, myattr), fractionMean);
                    }
                }
            }
        }

        for (String mycoord : this.coord.split(","))
        {
            double total = 0;
            double intra = 0;

            for (Face myface : mesh.graph.faces())
            {
                boolean pa = checkParcel.apply(myface.getA());
                boolean pb = checkParcel.apply(myface.getB());
                boolean pc = checkParcel.apply(myface.getC());

                boolean wa = checkWhole.apply(myface.getA());
                boolean wb = checkWhole.apply(myface.getB());
                boolean wc = checkWhole.apply(myface.getC());

                Vect va = mesh.vattr.get(myface.getA(), mycoord);
                Vect vb = mesh.vattr.get(myface.getB(), mycoord);
                Vect vc = mesh.vattr.get(myface.getC(), mycoord);

                double area = new Triangle(va, vb, vc).area();

                if (pa && pb && pc)
                {
                    intra += area;
                }

                if (wa && wb && wc)
                {
                    total += area;
                }
            }

            double frac = total > 0 ? intra / total : 0;

            insert.accept(String.format("%sparcel_%s_area", this.prefix, mycoord), intra);
            insert.accept(String.format("%swhole_%s_area", this.prefix, mycoord), total);
            insert.accept(String.format("%sfraction_%s_area", this.prefix, mycoord), frac);
        }

        this.output = map;

        return this;
    }
}