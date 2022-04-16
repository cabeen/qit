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

package qit.data.modules.volume;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.text.StrSubstitutor;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.base.cli.CliUtils;
import qit.base.structs.Pair;
import qit.base.utils.ComboUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Volume;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@ModuleUnlisted
@ModuleDescription("Sample values from a collection of volumes")
@ModuleAuthor("Ryan Cabeen")
public class VolumeProbe implements Module
{
    @ModuleInput
    @ModuleDescription("the input pattern (may contain fields ${field1} and ${field2})")
    private String pattern = "volume.${key}.${field1}.${field2}.nii.gz";

    @ModuleInput
    @ModuleDescription("the probe definition (should have lookup fields and voxel sample fields")
    private Table probe = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("a comma delimited list of fields to use for pattern substitution")
    private String fields = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("sample fields given like i,j,k")
    private String samples = "i,j,k";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("variables to substitute with a product, e.g. x=a1,b1,c1,y=a2,b2")
    private String product = null;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the value field")
    private String value = "value";

    @ModuleOutput
    @ModuleDescription("the output values")
    private Table output;

    @Override
    public Module run() throws IOException
    {
        String spattern = pattern.replace('%', '$');
        String[] ijk = this.samples.split(",");

        String fieldI = ijk[0];
        String fieldJ = ijk[1];
        String fieldK = ijk[2];

        List<String> fields = Lists.newArrayList();
        if (this.fields != null)
        {
            fields = Lists.newArrayList(this.fields.split(","));
        }

        List<Map<String, String>> prods = Lists.newArrayList();
        if (this.product != null)
        {
            Map<String,List<String>> vars = Maps.newHashMap();
            for (String sub : this.product.split(";"))
            {
                String[] pair = sub.split("=");
                vars.put(pair[0], CliUtils.names(pair[1], null));
            }
            prods = ComboUtils.product(vars);
        }
        else
        {
            Map<String,String> def = Maps.newHashMap();
            prods.add(def);
        }

        Map<String, List<Pair<Map<String,String>, Integer>>> segmentation = Maps.newHashMap();

        for (Integer key : this.probe.keys())
        {
            Record row = this.probe.getRecord(key);

            for (Map<String,String> pmap : prods)
            {
                Map<String,String> env = Maps.newHashMap();
                for (String pkey : pmap.keySet())
                {
                    env.put(pkey, pmap.get(pkey));
                }

                for (String field : fields)
                {
                    env.put(field, row.get(field));
                }

                String fn = new StrSubstitutor(env).replace(spattern);

                if (!segmentation.containsKey(fn))
                {
                    segmentation.put(fn, Lists.<Pair<Map<String,String>, Integer>>newArrayList());
                }

                segmentation.get(fn).add(Pair.of(pmap, key));
            }
        }

        Logging.info("filenames found: " + segmentation.size());

        Table out = new Table(this.probe.getSchema().copy());
        for (String value : prods.get(0).keySet())
        {
            out.withField(value);
        }
        out.withField(this.value);

        for (String fn : segmentation.keySet())
        {
            Logging.info("processing: " + fn);
            Volume volume = Volume.read(fn);

            for (Pair<Map<String,String>, Integer> elem : segmentation.get(fn))
            {
                Record record = this.probe.getRecord(elem.b);
                int i = Integer.valueOf(record.get(fieldI));
                int j = Integer.valueOf(record.get(fieldJ));
                int k = Integer.valueOf(record.get(fieldK));
                double value = volume.get(i, j, k, 0);

                for (String pkey : elem.a.keySet())
                {
                    record.with(pkey, elem.a.get(pkey));
                }
                record.with(this.value, String.valueOf(value));
                out.addRecord(record);
            }
        }

        this.output = out;
        return this;
    }
}
