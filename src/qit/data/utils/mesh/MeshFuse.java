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

package qit.data.utils.mesh;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Vertex;

import java.util.List;
import java.util.Map;

public class MeshFuse
{
    private Map<String, Map<Vertex, VectsOnlineStats>> stats = Maps.newHashMap();
    private Mesh ref;
    private Mesh output;

    public MeshFuse addMesh(Mesh mesh)
    {
        if (this.ref == null)
        {
            Logging.info("building output mesh");
            Mesh mean = mesh.copy();

            Logging.info("building stats data structures");
            Map<String, Map<Vertex, VectsOnlineStats>> astats = Maps.newHashMap();
            List<String> attrs = Lists.newArrayList(mean.vattr.attrs());
            for (String attr : attrs)
            {
                Logging.info("building stats for: " + attr);
                Vect proto = mean.vattr.proto(attr);
                Map<Vertex, VectsOnlineStats> vstats = Maps.newHashMap();
                for (Vertex vert : mean.vattr)
                {
                    vstats.put(vert, new VectsOnlineStats(proto.size()));
                }
                astats.put(attr, vstats);
            }

            this.stats = astats;
            this.ref = mean;
        }

        for (String attr : this.stats.keySet())
        {
            for (Vertex vert : this.ref.vattr)
            {
                this.stats.get(attr).get(vert).update(mesh.vattr.get(vert, attr));
            }
        }
        
        return this;
    }

    public MeshFuse run()
    {
        if (this.ref == null)
        {
            throw new RuntimeException("no input meshes were specified");
        }

        for (String attr : this.stats.keySet())
        {
            Vect proto = this.ref.vattr.proto(attr);
            this.ref.vattr.remove(attr);
            this.ref.vattr.add(attr + "_mean", proto);
            this.ref.vattr.add(attr + "_std", proto);

            Map<Vertex, VectsOnlineStats> vstats = this.stats.get(attr);
            for (Vertex vert : this.ref.vattr)
            {
                VectsOnlineStats vals = vstats.get(vert);
                this.ref.vattr.set(vert, attr + "_mean", vals.mean);
                this.ref.vattr.set(vert, attr + "_std", vals.std);
            }
        }

        this.ref.vattr.rename(Mesh.COORD + "_mean", Mesh.COORD);
        this.output = this.ref;
        
        return this;
    }

    public Mesh getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }
}
