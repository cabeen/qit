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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Triple;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.source.TableSource;
import qit.data.utils.TableUtils;
import qit.math.utils.MathUtils;
import qit.math.utils.expression.StringExpression;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

@ModuleDescription("Select columns from a table")
@ModuleAuthor("Ryan Cabeen")
public class TableSelect implements Module
{
    @ModuleInput
    @ModuleDescription("input table")
    public Table input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an predicate for selecting records (using existing field names)")
    public String where;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("sort by fields (e.g. field2,#field3,^#field1).  '#' indicates the value is numeric, and '^' indicates the sorting should be reversed")
    public String sort;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("select a unique set from the given fields (comma delimited)")
    public String unique;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("include only specific fields (comma delimited regular expressions)")
    public String retain;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("exclude specific fields (comma delimited regular expressions)")
    public String remove;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("an renaming of fields (e.g. newfield=oldfield)")
    public String rename;

    @ModuleParameter
    @ModuleDescription("delete fields with an empty name")
    public boolean dempty;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("concatenate existing fields into a new one, e.g. newfield=%{fieldA}_%{fieldB}")
    public String cat;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("add a constant field (e.g. newfield=value,newfield2=value2)")
    public String constant;

    @ModuleOutput
    @ModuleDescription("output table")
    public Table output;

    @Override
    public TableSelect run()
    {
        Table table = this.input;

        if (this.dempty)
        {
            table = TableUtils.dempty(table);
        }

        if (this.rename != null)
        {
            table = TableUtils.rename(table, this.rename);
        }

        if (this.retain != null)
        {
            table = TableUtils.retain(table, this.retain);
        }

        if (this.remove != null)
        {
            table = TableUtils.remove(table, this.remove);
        }

        if (this.sort != null)
        {
            table = TableUtils.sort(table, this.sort);
        }

        if (this.unique != null)
        {
            table = TableUtils.unique(table, this.unique);
        }

        if (this.constant != null)
        {
            table = TableUtils.constant(table, this.constant);
        }

        if (this.cat != null)
        {
            table = TableUtils.cat(table, this.cat);
        }

        if (this.where != null)
        {
            table = TableUtils.where(table, this.where);
        }

        this.output = table;

        return this;
    }
}
