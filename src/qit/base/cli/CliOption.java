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

package qit.base.cli;

import com.google.common.collect.Lists;
import java.util.List;

public class CliOption
{
    public enum CliOptionType
    {Input, Parameter, Output}

    private String name = "option"; // the flag name, i.e. --name
    private List<String> args = Lists.newArrayList(); // the example arguments, e.g. {"<Double>","<String>"}
    private String doc = "a undocumented parameter"; // the documentation string
    private int min = 0; // the minimum number of arguments
    private int max = 0; // the maximum number of arguments
    private String def = null; // the default value (null implies no default)
    private boolean optional = false; // whether the parameter is required
    private boolean advanced = false; // whether the parameter is somehow advanced
    private CliOptionType type = CliOptionType.Input;

    public CliOption withName(String v)
    {
        this.name = v;
        return this;
    }

    public CliOption withArg(String v)
    {
        this.args.add(v);
        this.max = this.args.size();
        this.min = this.args.size();

        return this;
    }

    public CliOption withArgs(List<String> v)
    {
        for (String arg : v)
        {
            this.withArg(arg);
        }
        return this;
    }

    public CliOption withDoc(String v)
    {
        this.doc = v;
        return this;
    }

    public CliOption withDefault(String d)
    {
        this.def = d;
        return this;
    }

    public CliOption withMin(int v)
    {
        this.min = v;
        return this;
    }

    public CliOption withMax(int v)
    {
        this.max = v;
        return this;
    }

    public CliOption withNum(int v)
    {
        this.min = v;
        this.max = v;
        return this;
    }

    public CliOption withNoMax()
    {
        this.max = Integer.MAX_VALUE;
        return this;
    }

    public CliOption asOptional()
    {
        this.optional = true;
        return this;
    }

    public CliOption asAdvanced()
    {
        this.advanced = true;
        return this;
    }

    public CliOption asInput()
    {
        this.type = CliOptionType.Input;
        return this;
    }

    public CliOption asParameter()
    {
        this.type = CliOptionType.Parameter;
        return this;
    }

    public CliOption asOutput()
    {
        this.type = CliOptionType.Output;
        return this;
    }

    public String getName()
    {
        return this.name;
    }

    public List<String> getArgs()
    {
        return Lists.newArrayList(this.args);
    }

    public String getDoc()
    {
        return this.doc;
    }

    public boolean hasDefault()
    {
        return this.def != null;
    }

    public String getDefault()
    {
        return this.def;
    }

    public int getMin()
    {
        return this.min;
    }

    public int getMax()
    {
        return this.max;
    }

    public boolean isOptional()
    {
        return this.optional;
    }

    public boolean isAdvanced()
    {
        return this.advanced;
    }

    public boolean isInput()
    {
        return this.type.equals(CliOptionType.Input);
    }

    public boolean isParameter()
    {
        return this.type.equals(CliOptionType.Parameter);
    }

    public boolean isOutput()
    {
        return this.type.equals(CliOptionType.Output);
    }
}
