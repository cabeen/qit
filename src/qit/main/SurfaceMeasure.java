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
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliSpecification;
import qit.base.cli.CliOption;
import qit.base.cli.CliValues;
import qit.base.utils.PathUtils;
import qit.data.datasets.Mesh;
import qit.data.datasets.Record;
import qit.data.modules.mesh.MeshHull;
import qit.data.source.TableSource;
import qit.data.utils.MeshUtils;

import java.util.List;

public class SurfaceMeasure implements CliMain
{
    public static void main(String[] args)
    {
        new SurfaceMeasure().run(Lists.newArrayList(args));
    }
    
    public void run(List<String> args)
    {
        try
        {
            String doc = "Compute measures of brain surfaces, including surface area, ";
            doc += "hull surface area, gyrification, and lateralization, i.e. (left - right) / (left + right).";

            CliSpecification cli = new CliSpecification();
            cli.withName(this.getClass().getSimpleName());
            cli.withDoc(doc);
            cli.withOption(new CliOption().asInput().withName("input").withArg("<dir>").withDoc("specify an input surface directory"));
            cli.withOption(new CliOption().asOutput().withName("output").withArg("<fn>").withDoc("specify an output measure filename"));
            cli.withAuthor("Ryan Cabeen");

            Logging.info("parsing arguments");
            CliValues values = cli.parse(args);

            Logging.info("started");
            String input = values.keyed.get("input").get(0);
            String output = values.keyed.get("output").get(0);

            Global.assume(PathUtils.exists(input), "input not found: " + input);
            PathUtils.backup(output);

            Logging.info("reading meshes");
            Mesh leftPial = Mesh.read(PathUtils.join(input, "lh.pial.vtk"));
            Mesh rightPial = Mesh.read(PathUtils.join(input, "rh.pial.vtk"));

            Logging.info("computing hulls");
            Mesh leftHull = MeshHull.hull(leftPial);
            Mesh rightHull = MeshHull.hull(rightPial);

            Logging.info("computing surface areas");
            double leftPialArea = MeshUtils.area(leftPial);
            double rightPialArea = MeshUtils.area(rightPial);
            double leftHullArea = MeshUtils.area(leftHull);
            double rightHullArea = MeshUtils.area(rightHull);
            double leftGyri = leftPialArea / leftHullArea;
            double rightGyri = rightPialArea / rightHullArea;
            double latPialArea = (leftPialArea - rightPialArea) / (leftPialArea + rightPialArea);
            double latGyri = (leftGyri - rightGyri) / (leftGyri + rightGyri);
            
            Logging.info("aggregating results");
            Record out = new Record();
            
            out.with("left.pial.surface.area", String.valueOf(leftPialArea));
            out.with("right.pial.surface.area", String.valueOf(rightPialArea));
            out.with("left.pial.hull.area", String.valueOf(leftHullArea));
            out.with("right.pial.hull.area", String.valueOf(rightHullArea));
            out.with("left.gyrification", String.valueOf(leftGyri));
            out.with("right.gyrification", String.valueOf(rightGyri));
            out.with("pial.surface.area.lateralization", String.valueOf(latPialArea));
            out.with("pial.gyrification.lateralization", String.valueOf(latGyri));
            
            Logging.info("writing output:" + output);
            TableSource.createNarrow(out).write(output);
            
            Logging.info("finished");
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Logging.error("an error occurred: " + e.getMessage());
        }
    }
}
