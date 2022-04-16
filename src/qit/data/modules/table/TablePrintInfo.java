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

package qit.data.modules.table;

import org.apache.commons.lang3.text.StrSubstitutor;
import qit.base.Module;
import qit.base.annot.*;
import qit.base.cli.CliUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Table;

@ModuleDescription("Print basic information about a table")
@ModuleAuthor("Ryan Cabeen")
public class TablePrintInfo implements Module
{
    @ModuleInput
    @ModuleDescription("the input table")
    private Table input;

    @ModuleParameter
    @ModuleDescription("specify the token for identifying missing values")
    private String na = "NA";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("only print values from the given field (specify either the field index or name)")
    private String field;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("print a pattern for each line, e.g. '${fieldA} and ${fieldB}")
    private String pattern;

    @ModuleParameter
    @ModuleDescription("print the row index")
    private boolean indexed;

    @Override
    public Module run()
    {
        if (this.pattern != null)
        {
            String pat = this.pattern.contains("%{") ? this.pattern.replace('%', '$') : this.pattern;

            for (Integer key : this.input.keys())
            {
                Record record = this.input.getRecord(key);
                System.out.println(new StrSubstitutor(record.map()).replace(pat));
            }
        }
        else if (this.field != null)
        {
            int index = this.input.hasField(this.field) ? this.input.getFieldIndex(this.field) : Integer.parseInt(this.field);

            for (Integer key : this.input.keys())
            {
                String value = this.input.get(key, index);
                if (this.indexed)
                {
                    System.out.print(key + ":");
                }
                String d = this.indexed ? String.format("(%d) ", key) : "";
                System.out.println(String.format("%s%s", d, value));
            }
        }
        else
        {
            System.out.println();
            System.out.println(String.format("  %s:", this.getClass().getSimpleName()));
            System.out.println();
            System.out.println(String.format("    num records (rows): %d", this.input.getNumRecords()));
            System.out.println(String.format("    num fields (cols): %d", this.input.getNumFields()));
            System.out.println();
            System.out.println(String.format("    fields:"));
            System.out.println();
            for (int i = 0; i < this.input.getNumFields(); i++)
            {
                int na = 0;
                for (Integer key : this.input.keys())
                {
                    String value = this.input.get(key, i);
                    if (value == null || value.toLowerCase().equals(this.na))
                    {
                        na += 1;
                    }
                }

                String n = this.input.getField(i);
                String c = na > 0 ? String.format(" (%d missing values)", na) : "";
                String d = this.indexed ? String.format("(%d) ", i) : "";
                System.out.println(String.format("      %s%s%s", d, n, c));

            }
            System.out.println("");
        }

        return this;
    }
}
