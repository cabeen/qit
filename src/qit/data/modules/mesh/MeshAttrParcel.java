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
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.structs.Histogram;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

@ModuleDescription("Extract and measure a parcel")
@ModuleAuthor("Ryan Cabeen")
public class MeshAttrParcel implements Module
{
    public enum MeshAttrParcelStat { Mode, Mean };

    @ModuleInput
    @ModuleDescription("the input Mesh")
    public Mesh input;

    @ModuleInput
    @ModuleDescription("the input prior attribute vectors")
    public Vects prior;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("the input whole surface attribute vectors")
    public Vects whole;

    @ModuleParameter
    @ModuleDescription("a comma-separated list of coordinate attributes")
    public String coord = Mesh.COORD;

    @ModuleParameter
    @ModuleDescription("the parcel name")
    public String name = "parcel";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the attributes to measure (comma-separated)")
    public String measure = null;

    @ModuleParameter
    @ModuleDescription("the attribute for segmentation")
    public String segment = "segment";

    @ModuleParameter
    @ModuleDescription("the value for thresholding")
    public double threshold = 0.5;

    @ModuleParameter
    @ModuleDescription("apply an inverse threshold")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("normalize the segmentation attribute (scale based on either the mode or mean)")
    public boolean normalize = false;

    @ModuleParameter
    @ModuleDescription("the statistic to use for normalization")
    public MeshAttrParcelStat stat = MeshAttrParcelStat.Mean;

    @ModuleParameter
    @ModuleDescription("use an Otsu threshold (the specific threshold is ignored)")
    public boolean otsu = false;

    @ModuleParameter
    @ModuleDescription("the number of bins for Otsu thresholding")
    public int bins = 512;

    @ModuleParameter
    @ModuleDescription("the number of smoothing operations")
    public int smooth = 2;

    @ModuleParameter
    @ModuleDescription("the number of mode filters")
    public int mode = 2;

    @ModuleOutput
    @ModuleDescription("the output segmentation mask")
    public Vects outputMask;

    @ModuleOutput
    @ModuleDescription("the output statistics")
    public Table outputTable;

    @Override
    public MeshAttrParcel run()
    {
        Mesh mesh = this.input;

        List<String> myattrs = Lists.newArrayList();
        if (this.measure != null)
        {
            for (String token : this.measure.split(","))
            {
                if (token.contains("="))
                {
                    String[] split = token.split("=");
                    String name = split[0];
                    String file = split[1];

                    try
                    {
                        Vects values = Vects.read(file);
                        MeshAttrSetVects.apply(mesh, name, values);
                        myattrs.add(name);
                    }
                    catch (IOException e)
                    {
                        Logging.info("warning, failed to load: " + file);
                    }
                }
                else
                {
                    myattrs.add(token);
                }
            }
        }

        MeshAttrSetVects.apply(mesh, Mesh.PRIOR, this.prior);
        MeshUtils.copy(mesh, this.segment, Mesh.SEGMENT);

        if (this.whole != null)
        {
            MeshAttrSetVects.apply(mesh, Mesh.WHOLE, this.whole);
        }
        else
        {
            mesh.vattr.setAll(Mesh.WHOLE, VectSource.create1D(1));
        }

        if (this.normalize)
        {
            Function<Vertex, Boolean> checkWhole = (vertex) -> MathUtils.round(mesh.vattr.get(vertex, Mesh.WHOLE).get(0)) > 0;
            Vects values = MeshUtils.values(mesh, checkWhole, Mesh.SEGMENT);
            double stat = 0;
            switch(this.stat)
            {
                case Mean:
                    stat = values.flatten().mean();
                    break;
                case Mode:
                    stat = Histogram.create(values.flatten()).mode();
                    break;
            }

            double factor = MathUtils.nonzero(mode) ? 1.0 / stat: 0;
            Function<Vect, Vect> harmonize = (x) -> x.times(factor);
            MeshUtils.applyEquals(mesh, harmonize, Mesh.SEGMENT);
        }

        double mythresh = this.threshold;

        if (this.otsu)
        {
            Function<Vertex, Boolean> checkWhole = (vertex) -> MathUtils.round(mesh.vattr.get(vertex, Mesh.PRIOR).get(0)) > 0;
            Vects values = MeshUtils.values(mesh, checkWhole, Mesh.SEGMENT);
            Histogram histogram = Histogram.create(this.bins, values.min().first(), values.max().first());

            for (Vect v : values)
            {
                histogram.update(v.first());
            }

            mythresh = histogram.otsu();

            Logging.info("computed otsu threshold: " + mythresh);
        }

        {
            MeshAttrLaplacian module = new MeshAttrLaplacian();
            module.input = mesh;
            module.attrin = Mesh.SEGMENT;
            module.attrout = Mesh.SEGMENT;
            module.num = this.smooth;
            module.inplace = true;
            module.run();
        }

        {
            MeshAttrThreshold module = new MeshAttrThreshold();
            module.input = mesh;
            module.threshold = mythresh;
            module.invert = this.invert;
            module.attrin = Mesh.SEGMENT;
            module.attrout = Mesh.SEGMENT;
            module.inplace = true;
            module.run();
        }

        {
            MeshAttrMask module = new MeshAttrMask();
            module.input = mesh;
            module.mask = Mesh.PRIOR;
            module.attrin = Mesh.SEGMENT;
            module.attrout = Mesh.SEGMENT;
            module.inplace = true;
            module.run();
        }

        {
            MeshAttrComponents module = new MeshAttrComponents();
            module.input = mesh;
            module.attrin = Mesh.SEGMENT;
            module.attrout = Mesh.SEGMENT;
            module.largest = true;
            module.inplace = true;
            module.run();
        }

        {
            MeshAttrMode module = new MeshAttrMode();
            module.input = mesh;
            module.attrin = Mesh.SEGMENT;
            module.attrout = Mesh.SEGMENT;
            module.num = this.mode;
            module.inplace = true;
            module.run();
        }

        {
            MeshAttrGetVects module = new MeshAttrGetVects();
            module.input = mesh;
            module.attr = Mesh.SEGMENT;
            this.outputMask = module.run().output;
        }

        {
            MeshAttrMeasureBinary module = new MeshAttrMeasureBinary();
            module.input = mesh;
            module.coord = this.coord;
            module.label = Mesh.SEGMENT;
            module.prefix = this.name + "_";
            module.attr = myattrs.size() > 0 ? String.join(",", myattrs) : null;
            this.outputTable = module.run().output;
        }

        {
            MeshAttrMeasureBinary module = new MeshAttrMeasureBinary();
            module.input = mesh;
            module.coord = this.coord;
            module.label = Mesh.SEGMENT;
            module.whole = Mesh.WHOLE;
            module.prefix = this.name + "_";
            module.attr = myattrs.size() > 0 ? String.join(",", myattrs) : null;
            this.outputTable = module.run().output;
        }

        this.outputTable.addRecord(new Record().with("name", "threshold").with("value", mythresh));

        return this;
    }
}
