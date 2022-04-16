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

package qit.data.modules.curves;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Mask;
import qit.data.datasets.Record;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.modules.mask.MaskBinarize;
import qit.data.modules.mask.MaskComponents;
import qit.data.modules.mask.MaskExtract;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.TableUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.utils.MathUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@ModuleDescription("Measure how many curves overlap a given mask parcellation")
@ModuleAuthor("Ryan Cabeen")
public class CurvesMaskMap implements Module
{
    enum CurvesMaskMapMode { Count, Binary, Fraction }

    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleInput
    @ModuleDescription("input regions")
    public Mask regions;

    @ModuleInput
    @ModuleDescription("use a lookup for region names")
    public Table lookup = null;

    @ModuleParameter
    @ModuleDescription("specify the lookup table name field")
    public String nameField = "name";

    @ModuleParameter
    @ModuleDescription("specify the lookup table index field")
    public String indexField = "index";

    @ModuleParameter
    @ModuleDescription("specify the way that curves are counted")
    public CurvesMaskMapMode mode = CurvesMaskMapMode.Binary;

    @ModuleParameter
    @ModuleDescription("select only curves with endpoints in the mask")
    public boolean endpoints;

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public CurvesMaskMap run()
    {
        Table out = new Table();
        out.withField("name");
        out.withField("value");

        Map<Integer, String> lut = TableUtils.createIntegerStringLookup(this.lookup, this.indexField, this.nameField);
        List<Integer> labels = MaskUtils.listNonzero(this.regions);

        for (int label : labels)
        {
            String name = "region" + label;
            if (lut.containsKey(label))
            {
                name = lut.get(label);
            }

            Logging.info("... testing: " + name);

            Mask submask = MaskExtract.extract(this.regions, label);

            CurvesSelect select = new CurvesSelect();
            select.input = this.input;
            select.mask = submask;
            select.endpoints = this.endpoints;
            double count = select.run().output.size();

            double value = count;
            switch (this.mode)
            {
                case Fraction:
                    value = count / (double) this.input.size();
                    break;
                case Binary:
                    value = MathUtils.nonzero(count) ? 1 : 0;
                    break;
            }

            Record record = new Record();
            record.with("name", name);
            record.with("value", value);

            out.addRecord(record);
        }

        this.output = out;

        return this;
    }
}
