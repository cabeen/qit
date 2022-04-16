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
import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.MeshUtils;
import qit.data.utils.TableUtils;
import qit.data.utils.mesh.MeshFunction;
import qit.math.structs.VectFunction;

import java.util.Map;

@ModuleDescription("Create a node link representation of mask connectivity in a mesh")
@ModuleAuthor("Ryan Cabeen")
public class MaskNodeLink implements Module
{
    @ModuleInput
    @ModuleDescription("input mask")
    public Mask input;

    @ModuleInput
    @ModuleDescription("input table")
    public Table lookup;

    @ModuleInput
    @ModuleDescription("input attributes")
    public Table attributes;

    @ModuleParameter
    @ModuleDescription("index")
    public String index = "index";

    @ModuleParameter
    @ModuleDescription("name")
    public String name = "name";

    @ModuleParameter
    @ModuleDescription("group")
    public String group = "group";

    @ModuleParameter
    @ModuleDescription("value")
    public String value = "value";

    @ModuleParameter
    @ModuleDescription("radius")
    public double radius = 10;

    @ModuleParameter
    @ModuleDescription("subdiv")
    public int subdiv = 2;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output links")
    public Curves links;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("output nodes")
    public Mesh nodes;

    @Override
    public MaskNodeLink run()
    {
        Map<Integer, Vect> centroids = MaskUtils.centroids(this.input);
        Record lut = TableUtils.createLookup(this.lookup, this.name, this.index);

        Mesh outNodes = null;
        for (String key : lut.keySet())
        {
            int idx = Integer.valueOf(lut.get(key));
            Vect center = centroids.get(idx);

            Mesh sphere = MeshSource.sphere(this.subdiv);
            VectFunction xfm = Affine.id(3).times(this.radius).plus(center);
            new MeshFunction(xfm).withMesh(sphere).run();
            MeshUtils.setAll(sphere, Mesh.INDEX, VectSource.create1D(idx));
            outNodes = MeshUtils.mbind(outNodes, sphere);
        }

        Curves outLinks = new Curves();

        Map<String, Curves.Curve> cache = Maps.newHashMap();
        for (Integer key : this.attributes.keys())
        {
            String recordName = this.attributes.get(key, this.name);

            if (!cache.containsKey(recordName))
            {
                String[] tokens = recordName.split("_");
                Global.assume(tokens.length == 2, "invalid connection name");

                String startName = tokens[0];
                String endName = tokens[1];
                Global.assume(lut.containsKey(startName), "invalid connection start: " + startName);
                Global.assume(lut.containsKey(endName), "invalid connection end: " + endName);

                Vect startPos = centroids.get(Integer.valueOf(lut.get(startName)));
                Vect endPos = centroids.get(Integer.valueOf(lut.get(endName)));

                Curves.Curve curve = outLinks.add(2);
                curve.set(0, startPos);
                curve.set(1, endPos);

                cache.put(recordName, curve);
            }

            Curves.Curve curve = cache.get(recordName);

            String recordGroup = this.attributes.get(key, this.group);

            if (!outLinks.has(recordGroup))
            {
                outLinks.add(recordGroup, VectSource.create1D());
            }

            Vect v = VectSource.create1D(this.attributes.get(key, this.value));
            curve.set(recordGroup, 0, v);
            curve.set(recordGroup, 1, v);
        }

        this.nodes = outNodes;
        this.links = outLinks;

        return this;
    }
}

