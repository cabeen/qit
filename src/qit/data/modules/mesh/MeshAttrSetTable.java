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

import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.datasets.Table;
import qit.data.modules.table.TableMerge;
import qit.data.source.VectSource;
import qit.data.utils.TableUtils;
import qit.math.structs.Face;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@ModuleDescription("Create a table listing regions of a mask")
@ModuleAuthor("Ryan Cabeen")
public class MeshAttrSetTable implements Module
{
    @ModuleInput
    @ModuleDescription("input mesh")
    public Mesh mesh;

    @ModuleInput
    @ModuleDescription("input table")
    public Table table;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a lookup table to relate names to indices")
    public Table lookup = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a field name to merge on")
    public String merge = "name";

    @ModuleParameter
    @ModuleDescription("the table index field name")
    public String index = "index";

    @ModuleParameter
    @ModuleDescription("a table field to get (can be comma delimited)")
    public String value = "value";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the mesh index field name (defaults to table index field name)")
    public String mindex = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the mesh value field name (defaults to table value field name)")
    public String mvalue = null;

    @ModuleParameter
    @ModuleDescription("a background value")
    public double background = 0;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an missing value")
    public Double missing = null;

    @ModuleParameter
    @ModuleDescription("remove triangles with vertices that are not included in the input table")
    public boolean remove = false;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("output mesh")
    public Mesh output;

    @Override
    public MeshAttrSetTable run()
    {
        Mesh mesh = this.inplace ? this.mesh : this.mesh.copy();

        Table mtable = this.table;

        if (this.lookup != null)
        {
            TableMerge module = new TableMerge();
            module.left = mtable;
            module.right = this.lookup;
            module.field = this.merge;
            mtable = module.run().output;
        }

        String meshIndexField = this.mindex == null ? this.index : this.mindex;
        Global.assume(mesh.vattr.has(meshIndexField), "expected mesh to have index field: " + meshIndexField);

        String[] values = this.value.split(",");
        String[] mvalues = this.mvalue == null ? values : this.mvalue.split(",");

        Global.assume(values.length == mvalues.length, "value names have difference sizes");

        Set<Integer> labels = Sets.newHashSet();
        for (int i = 0; i < values.length; i++)
        {
            Map<Integer, Double> lut = TableUtils.createIntegerDoubleLookup(mtable, this.index, values[i]);

            if (!lut.containsKey(0))
            {
                lut.put(0, this.background);
            }

            if (!mesh.vattr.has(mvalues[i]))
            {
                mesh.vattr.add(mvalues[i], VectSource.create1D());
            }

            for (Vertex vert : this.mesh.vattr)
            {
                int label = MathUtils.round(mesh.vattr.get(vert, meshIndexField).get(0));

                if (lut.containsKey(label))
                {
                    mesh.vattr.set(vert, mvalues[i], VectSource.create1D(lut.get(label)));
                }
                else if (this.missing != null)
                {
                    mesh.vattr.set(vert, mvalues[i], VectSource.create1D(this.missing));
                }
            }

            labels.addAll(lut.keySet());
        }

        if (this.remove)
        {
            final Mesh fmesh = mesh;
            final Mesh out = new Mesh();

            for (String name : mesh.vattr.attrs())
            {
                out.vattr.add(name, mesh.vattr.proto(name));
            }

            Consumer<Vertex> addVertex = (vert) ->
            {
                out.graph.add(vert);
                out.vattr.add(vert);

                for (String name : fmesh.vattr.attrs())
                {
                    out.vattr.set(vert, name, fmesh.vattr.get(vert, name));
                }
            };

            for (Face face : mesh.graph.faces())
            {
                int labelA = MathUtils.round(mesh.vattr.get(face.getA(), meshIndexField).get(0));
                int labelB = MathUtils.round(mesh.vattr.get(face.getB(), meshIndexField).get(0));
                int labelC = MathUtils.round(mesh.vattr.get(face.getC(), meshIndexField).get(0));

                boolean keep = true;
                keep &= labels.contains(labelA);
                keep &= labels.contains(labelB);
                keep &= labels.contains(labelC);

                if (keep)
                {
                    addVertex.accept(face.getA());
                    addVertex.accept(face.getB());
                    addVertex.accept(face.getC());

                    out.graph.add(face);
                }
            }

            mesh = out;
        }

        this.output = mesh;

        return this;
    }
}
