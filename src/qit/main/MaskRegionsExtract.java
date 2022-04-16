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
import qit.base.CliMain;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliUtils;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Mesh;
import qit.data.datasets.Table;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskBoundary;
import qit.data.modules.mask.MaskDilate;
import qit.data.modules.mask.MaskExtract;
import qit.data.modules.volume.VolumeFilterGaussian;
import qit.data.modules.volume.VolumeMarchingCubes;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.MeshUtils;
import qit.data.utils.TableUtils;

import java.util.List;
import java.util.Map;

public class MaskRegionsExtract implements CliMain
{
    public static void main(String[] args)
    {
        new MaskRegionsExtract().run(Lists.newArrayList(args));
    }

    public void run(List<String> args)
    {
        try
        {
            Logging.info("starting " + this.getClass().getSimpleName());

            String doc = "extract mesh models from a mask for each region";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("regions").withArg("<Mask>").withDoc("specify input region of interest(s)"));
            cli.withOption(new CliOption().asInput().withName("lookup").withArg("<Table>").withDoc("a table listing the names of regions"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-mesh").withArg("<Pattern>").withDoc("specify an output pattern (contains %s) for mesh output"));
            cli.withOption(new CliOption().asOutput().asOptional().withName("output-mask").withArg("<Pattern>").withDoc("specify an output pattern (contains %s) for mask output"));
            cli.withOption(new CliOption().asInput().asOptional().withName("mask").withArg("<Mask>").withDoc("specify a mask for including voxels"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("name").withArg("<String>").withDoc("specify a lookup table field for region names").withDefault("name"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("index").withArg("<String>").withDoc("specify a lookup table field for region index (label)").withDefault("index"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("include").withArg("name(s)").withDoc("specify which names to include").withNoMax());
            cli.withOption(new CliOption().asParameter().asOptional().withName("exclude").withArg("name(s)").withDoc("specify which names to exclude").withNoMax());
            cli.withOption(new CliOption().asParameter().asOptional().withName("support").withArg("<Integer>").withDoc("specify a filter support size").withDefault("3"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("std").withArg("<Double>").withDoc("specify a smoothing bandwidth"));
            cli.withOption(new CliOption().asParameter().asOptional().withName("level").withArg("<Double>").withDoc("specify a isolevel").withDefault("0.5"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues entries = cli.parse(args);

            Logging.info("started");
            String regionsFn = entries.keyed.get("regions").get(0);
            String lookupFn = entries.keyed.get("lookup").get(0);
            String nameField = entries.keyed.get("name").get(0);
            String indexField = entries.keyed.get("index").get(0);
            double level = Double.valueOf(entries.keyed.get("level").get(0));

            Logging.info("using lookup: " + lookupFn);
            Map<Integer,String> lookup = TableUtils.createIntegerStringLookup(Table.read(lookupFn), indexField, nameField);
            Logging.info(String.format("found %d regions in lookup", lookup.size()));

            Logging.info("using regions: " + regionsFn);
            Mask regions = Mask.read(regionsFn);

            if (entries.keyed.containsKey("mask"))
            {
                String maskFn = entries.keyed.get("mask").get(0);
                Logging.info("using mask: " + maskFn);
                Mask mask = Mask.read(maskFn);
                regions = MaskUtils.mask(regions, mask);
            }
            List<String> include = null;
            List<String> exclude = null;

            if (entries.keyed.containsKey("include"))
            {
                include = CliUtils.names(entries.keyed.get("include"));
            }

            if (entries.keyed.containsKey("exclude"))
            {
                exclude = CliUtils.names(entries.keyed.get("exclude"));
            }

            String outputMeshPattern = entries.keyed.containsKey("output-mesh") ? entries.keyed.get("output-mesh").get(0) : null;
            String outputMaskPattern = entries.keyed.containsKey("output-mask") ? entries.keyed.get("output-mask").get(0) : null;

            Integer support = Integer.valueOf(entries.keyed.get("support").get(0));
            Double std = entries.keyed.containsKey("std") ? Double.valueOf(entries.keyed.get("std").get(0)) : null;

            for (Integer key : lookup.keySet())
            {
                String name = lookup.get(key);

                if (include != null && !include.contains(name))
                {
                    Logging.info("skipping region " + name);
                    continue;
                }

                if (exclude != null && exclude.contains(name))
                {
                    Logging.info("skipping region " + name);
                    continue;
                }

                Logging.info("processing region " + name);

                Logging.info("...extracting mask");
                MaskExtract extractor = new MaskExtract();
                extractor.input = regions;
                extractor.label = String.valueOf(key);
                Mask mask = extractor.run().output;

                if (outputMaskPattern != null)
                {
                    String fn = String.format(outputMaskPattern, name);
                    Logging.info("...writing mask " + fn);
                    PathUtils.mkpar(fn);
                    mask.write(fn);
                }

                if (outputMeshPattern != null)
                {
                    Volume volume = mask.copyVolume();
                    if (std != null)
                    {
                        Logging.info("...smoothing mask");
                        MaskBoundary bounder = new MaskBoundary();
                        bounder.input = mask;
                        Mask boundary = bounder.run().output;

                        MaskDilate dilater = new MaskDilate();
                        dilater.input = boundary;
                        dilater.num = support;
                        boundary = dilater.run().output;

                        VolumeFilterGaussian filter = new VolumeFilterGaussian();
                        filter.input = volume;
                        filter.sigma = std;
                        filter.support = support;
                        filter.mask = boundary;
                        filter.pass = true;
                        volume = filter.run().output;
                    }

                    Logging.info("...extracting mesh");
                    Mesh mesh = new VolumeMarchingCubes().withInput(volume).withBackground(0).withLevel(level).run().output;
                    MeshUtils.setAll(mesh, Mesh.INDEX, VectSource.create1D(key));

                    String fn = String.format(outputMeshPattern, name);
                    Logging.info("...writing mesh " + fn);
                    PathUtils.mkpar(fn);
                    mesh.write(fn);
                }
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
