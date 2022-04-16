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
import com.google.common.collect.Queues;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.Vertex;

import java.util.List;
import java.util.Queue;

@ModuleDescription("Fill holes in a mesh.  This can be applied to either the entire mesh or a selection")
@ModuleAuthor("Ryan Cabeen")
public class MeshHoleFill implements Module
{
    @ModuleInput
    @ModuleDescription("the input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output mesh")
    public Mesh output;

    public MeshHoleFill run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();
        extrude(mesh);

        this.output = mesh;

        return this;
    }

    public static void extrude(Mesh mesh)
    {
        Queue<Vertex> bound = Queues.newLinkedBlockingQueue(mesh.graph.bound());

        Logging.info(String.format("found %d boundary vertices", bound.size()));

        if (bound.size() < 3)
        {
            // what is there to do here?
            return;
        }

        List<Vertex> added = Lists.newArrayList();
        outer: while(bound.size() > 0)
        {
            if (bound.size() < 3)
            {
                Logging.info("skipping degenerate case");
                bound.clear();
                continue;
            }
            if (bound.size() == 3)
            {
                Logging.info("filled hold of 3 vertices");

                Vertex a = bound.poll();
                Vertex b = bound.poll();
                Vertex c = bound.poll();

                if (mesh.graph.hasEdge(b, a))
                {
                    mesh.graph.add(new Face(a, b, c));
                }
                else
                {
                    mesh.graph.add(new Face(a, c, b));
                }
            }
            else
            {
                Vertex start = bound.poll();

                List<Vertex> group = Lists.newArrayList();
                group.add(start);

                Vertex current = start;

                do
                {
                    boolean found = false;
                    for (Edge edge : mesh.graph.edgeStar(current))
                    {
                        Vertex next = edge.opposite(current);
                        if (next.equals(start))
                        {
                            current = start;
                            found = true;

                            break;
                        }

                        if (mesh.graph.boundary(edge) && bound.contains(next))
                        {
                            current = next;

                            group.add(next);
                            bound.remove(next);

                            found = true;

                            break;
                        }
                    }

                    if (!found)
                    {
                        Logging.info("an error occurred searching boundary, skipping");
                        break outer;
                    }
                }
                while (current != start);

                VectsOnlineStats stats = new VectsOnlineStats(3);
                for (Vertex vertex : group)
                {
                    stats.update(mesh.vattr.get(vertex, Mesh.COORD));
                }

                Vect mean = stats.mean;

                Vertex center = mesh.graph.addVertex();
                added.add(center);

                mesh.vattr.add(center);
                mesh.vattr.set(center, Mesh.COORD, mean);

                for (String attr : mesh.vattr.attrs())
                {
                    if (!attr.equals((Mesh.COORD)))
                    {
                        mesh.vattr.set(center, attr, mesh.vattr.get(group.get(0), attr));
                    }
                }

                for (int i = 0; i < group.size(); i++)
                {
                    Vertex a = group.get(i == 0 ? group.size() - 1 : i - 1);
                    Vertex b = group.get(i);

                    try
                    {
                        mesh.graph.add(new Face(b, a, center));
                    }
                    catch (Exception e)
                    {
                        try
                        {
                            mesh.graph.add(new Face(a, a, center));
                        }
                        catch (Exception e2)
                        {
                            Logging.info("skipping face");

                        }
                    }
                }

                Logging.info(String.format("filled hole of %d vertices", group.size()));
            }
        }

        // @TODO add some mesh fairing to the new vertices
    }
}
