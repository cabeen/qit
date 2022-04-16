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

package qit.main;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.CliMain;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliOption;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mesh;
import qit.data.source.VectSource;
import qit.data.utils.vects.stats.VectsOnlineStats;
import qit.math.structs.Tally;
import qit.math.structs.Vertex;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MeshFuse implements CliMain
{
    public static void main(String[] args)
    {
        new VolumeFuse().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "fuse meshes";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<Meshes>").withDoc("the input meshes").withNoMax());
            cli.withOption(new CliOption().asParameter().asOptional().withName("pattern").withArg("<String(s)>").withDoc("specify a list of names that will be substituted with input %s"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("vector").withArg("<String>").withDoc("specify vector attributes (comma-separated)"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("discrete").withArg("<String>").withDoc("specify discrete attributes (comma-separated)"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output").withArg("<Mesh>").withDoc("specify the output fused mesh"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            Set<String> vector = Sets.newHashSet(entries.keyed.get("vector").get(0).split(","));
            Set<String> discrete = Sets.newHashSet(entries.keyed.get("discrete").get(0).split(","));

            List<String> inputFns = entries.keyed.get("input");

            if (entries.keyed.containsKey("pattern"))
            {
                List<String> names = CliUtils.names(entries.keyed.get("pattern").get(0), null);
                Global.assume(names != null, "invalid pattern");

                List<String> rawFns = inputFns;
                inputFns = Lists.newArrayList();

                for (String rawPair : rawFns)
                {
                    for (String name : names)
                    {
                        String fn = rawPair.replace("%s", name);

                        if (PathUtils.exists(fn))
                        {
                            inputFns.add(fn);
                        }
                        else
                        {
                            Logging.info("warning: skipping file: " + fn);
                        }
                    }
                }
            }

            Logging.info(String.format("found %d meshes", inputFns.size()));

            Logging.info("reading reference mesh");
            Mesh out = Mesh.read(inputFns.get(0));

            int nverts = out.vattr.size();
            Vertex[] verts = new Vertex[nverts];

            int idx = 0;
            for (Vertex v : out.vattr)
            {
                verts[idx] = v;
                idx += 1;
            }

            Map<String, VectsOnlineStats[]> vectAttrs = Maps.newHashMap();
            Map<String, Tally[]> discreteAttrs = Maps.newHashMap();

            for (String attr : out.vattr.attrs())
            {
                if (discrete.contains(attr))
                {
                    Tally[] vs = new Tally[nverts];
                    for (int i = 0; i < nverts; i++)
                    {
                        vs[i] = new Tally();
                    }

                    discreteAttrs.put(attr, vs);
                }
                else if (vector.contains(attr))
                {
                    VectsOnlineStats[] vs = new VectsOnlineStats[nverts];
                    int dim = out.vattr.dim(attr);

                    for (int i = 0; i < nverts; i++)
                    {
                        vs[i] = new VectsOnlineStats(dim);
                    }

                    vectAttrs.put(attr, vs);
                }
            }

            for (String fn : inputFns)
            {
                Logging.info("... reading: " + fn);
                Mesh mesh = Mesh.read(fn);

                if (mesh.vattr.size() != out.vattr.size())
                {
                    Logging.info("...... mesh does not match reference, skipping");
                    continue;
                }

                for (String attr : mesh.vattr.attrs())
                {
                    if (discrete.contains(attr))
                    {
                        Tally[] vs = discreteAttrs.get(attr);
                        for (int i = 0; i < nverts; i++)
                        {
                            vs[i].increment((int) mesh.vattr.get(verts[i], attr).get(0));
                        }
                    }
                    else if (vector.contains(attr))
                    {
                        VectsOnlineStats[] vs = vectAttrs.get(attr);
                        for (int i = 0; i < nverts; i++)
                        {
                            vs[i].update(mesh.vattr.get(verts[i], attr));
                        }
                    }
                }
            }

            for (String attr : out.vattr.attrs())
            {
                if (discrete.contains(attr))
                {
                    Tally[] vs = discreteAttrs.get(attr);
                    for (int i = 0; i < nverts; i++)
                    {
                        out.vattr.set(verts[i], attr, VectSource.create1D(vs[i].mode()));
                    }
                }
                else if (vector.contains(attr))
                {
                    VectsOnlineStats[] vs = vectAttrs.get(attr);
                    for (int i = 0; i < nverts; i++)
                    {
                        out.vattr.set(verts[i], attr, vs[i].mean);
                    }
                }
            }

//            for (String attr : out.vattr.attrs())
//            {
//                boolean disc = discrete.contains(attr);
//                String vattr = attr + "." + (disc ? "entropy" : "variance");
//                out.vattr.add(vattr, VectSource.createND(out.vattr.dim(attr)));
//
//                if (disc)
//                {
//                    Tally[] vs = discreteAttrs.get(attr);
//                    for (int i = 0; i < nverts; i++)
//                    {
//                        out.vattr.set(verts[i], vattr, VectSource.create1D(vs[i].entropy()));
//                    }
//                }
//                else if (vector.contains(attr))
//                {
//                    VectsOnlineStats[] vs = vectAttrs.get(attr);
//                    for (int i = 0; i < nverts; i++)
//                    {
//                        out.vattr.set(verts[i], vattr, vs[i].var);
//                    }
//                }
//            }

            if (entries.keyed.containsKey("output"))
            {
                String fn = entries.keyed.get("output").get(0);
                Logging.info("writing output: " + fn);
                out.write(fn);
            }

            Logging.info("finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}
