/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.models;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.Model;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;

import java.util.List;

// Model: y = alpha * exp(-beta * x)
// with alpha, beta > 0
public class ExpDecay extends Model<ExpDecay>
{
    public final static String NAME = "exp";

    public static final String ALPHA = "alpha";
    public static final String BETA = "beta";

    public double alpha;
    public double beta;

    public static boolean matches(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lname = name.toLowerCase();
        return lname.contains(".expdec") || lname.contains(".expdecay");
    }

    public ExpDecay()
    {
        this.clear();
    }

    public ExpDecay(ExpDecay model)
    {
        this();
        this.set(model);
    }

    public ExpDecay(Vect encoding)
    {
        this();
        this.setEncoding(encoding);
    }

    public int getDegreesOfFreedom()
    {
        return 2;
    }

    public void clear()
    {
        this.alpha = 0;
        this.beta = 0;
    }

    public double baseline()
    {
        return this.alpha;
    }

    public void setAlpha(double v)
    {
        this.alpha = v;
    }

    public void setBeta(double v)
    {
        this.beta = v;
    }

    public double getAlpha()
    {
        return this.alpha;
    }

    public double getBeta()
    {
        return this.beta;
    }

    public double dist(ExpDecay model)
    {
        double dalpha = this.alpha - model.alpha;
        double dbeta = this.beta - model.beta;

        double out = 0;
        out += dalpha * dalpha;
        out += dbeta * dbeta;
        out = Math.sqrt(out);

        return out;
    }

    @Override
    public ExpDecay set(ExpDecay model)
    {
        this.clear();
        this.alpha = model.alpha;
        this.beta = model.beta;
        return this;
    }

    @Override
    public ExpDecay copy()
    {
        return new ExpDecay(this);
    }

    @Override
    public ExpDecay proto()
    {
        return new ExpDecay();
    }

    @Override
    public int getEncodingSize()
    {
        return 2;
    }

    @Override
    public ExpDecay setEncoding(Vect encoding)
    {
        this.alpha = encoding.get(0);
        this.beta = encoding.get(1);

        return this;
    }

    @Override
    public void getEncoding(Vect encoding)
    {
        Global.assume(encoding.size() == this.getEncodingSize(), "invalid encoding");

        encoding.set(0, this.alpha);
        encoding.set(1, this.beta);
    }

    @Override
    public String toString()
    {
        String out = "{alpha: " + this.alpha + ", beta : " + this.beta+ "}";

        return out;
    }

    @Override
    public List<String> features()
    {
        List<String> out = Lists.newArrayList();
        out.add(ALPHA);
        out.add(BETA);

        return out;
    }

    @Override
    public Vect feature(String name)
    {
        if (ALPHA.equals(name))
        {
            return VectSource.create1D(this.getAlpha());
        }
        if (BETA.equals(name))
        {
            return VectSource.create1D(this.getBeta());
        }

        throw new RuntimeException("invalid index: " + name);
    }

    @Override
    public ExpDecay getThis()
    {
        return this;
    }

    public static VectFunction synth(final Vect varying)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                ExpDecay model = new ExpDecay(input);
                double alpha = model.getAlpha();
                double beta = model.getBeta();

                for (int i = 0; i < varying.size(); i++)
                {
                    double x = varying.get(i);
                    double y = alpha * Math.exp(-beta * x);
                    output.set(i, y);
                }
            }
        }.init(new ExpDecay().getEncodingSize(), varying.size());
    }
}
