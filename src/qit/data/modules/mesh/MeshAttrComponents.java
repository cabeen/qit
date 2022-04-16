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
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.source.VectSource;
import qit.math.structs.DisjointSet;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.util.Map;

@ModuleDescription("Compute connected components of a mesh selection")
@ModuleAuthor("Ryan Cabeen")
public class MeshAttrComponents implements Module
{
    @ModuleInput
    @ModuleDescription("the input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("retain only the largest component")
    public boolean largest;

    @ModuleParameter
    @ModuleDescription("the input attribute")
    public String attrin = Mesh.SELECTION;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the output attribute")
    public String attrout = null;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    public MeshAttrComponents run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();

        String[] inputs = this.attrin.split(",");
        String[] outputs = inputs;

        if (this.attrout != null)
        {
            outputs = this.attrout.split(",");
        }

        for (int a = 0; a < inputs.length; a++)
        {

            DisjointSet<Integer> matching = new DisjointSet<>();
            int maxIndex = 1;

            Map<Vertex, Integer> vertexIndices = Maps.newHashMap();
            for (Vertex centerVertex : mesh.graph.verts())
            {
                int centerLabel = MathUtils.round(mesh.vattr.get(centerVertex, inputs[a]).get(0));

                if (centerLabel == 0)
                {
                    continue;
                }

                Map<Integer, Integer> ringMap = Maps.newHashMap();
                for (Vertex ringVertex : mesh.graph.vertRing(centerVertex))
                {
                    int ringLabel = MathUtils.round(mesh.vattr.get(ringVertex, inputs[a]).get(0));

                    if (ringLabel != 0 && vertexIndices.containsKey(ringVertex))
                    {
                        ringMap.put(vertexIndices.get(ringVertex), ringLabel);
                    }
                }

                if (ringMap.size() == 0)
                {
                    matching.add(maxIndex);
                    vertexIndices.put(centerVertex, maxIndex);
                    maxIndex += 1;
                } else
                {
                    int minIndex = 0;
                    int minCount = Integer.MAX_VALUE;
                    for (Integer ringIndex : ringMap.keySet())
                    {
                        int ringCount = matching.getSize(ringIndex);
                        if (ringMap.get(ringIndex) == centerLabel && ringCount < minCount)
                        {
                            minIndex = ringIndex;
                            minCount = ringCount;
                        }
                    }
                    vertexIndices.put(centerVertex, minIndex);

                    for (Integer ringIndex : ringMap.keySet())
                    {
                        if (ringMap.get(ringIndex) == centerLabel)
                        {
                            matching.join(ringIndex, minIndex);
                        }
                    }
                }
            }

            Map<Integer, Integer> counts = Maps.newHashMap();
            for (Vertex centerVertex : mesh.graph.verts())
            {
                if (vertexIndices.containsKey(centerVertex))
                {
                    int centerIndex = vertexIndices.get(centerVertex);
                    int matchIndex = matching.find(centerIndex);
                    vertexIndices.put(centerVertex, matchIndex);

                    int matchCount = counts.containsKey(matchIndex) ? counts.get(matchIndex) + 1 : 1;
                    counts.put(matchIndex, matchCount);
                }
            }

            Map<Integer, Integer> lookup = MathUtils.remap(counts);

            for (Vertex vertex : mesh.graph.verts())
            {
                double value = 0;

                if (vertexIndices.containsKey(vertex))
                {
                    int label = lookup.get(vertexIndices.get(vertex));
                    value = this.largest ? (label == 1 ? 1 : 0) : label;
                }

                mesh.vattr.set(vertex, outputs[a], VectSource.create1D(value));
            }
        }

        this.output = mesh;

        return this;
    }
}
