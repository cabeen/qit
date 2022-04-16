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

// Model: y = alpha * (frac * exp(-beta * x) + (1.0 - frac) * exp(-gamma * x))
// with alpha, beta, gamma > 0; beta < gamma; frac in [0, 1]
public class BiExpDecay extends Model<BiExpDecay>
{
    public final static String NAME = "biexp";

    public static final String ALPHA = "alpha";
    public static final String FRAC = "frac";
    public static final String BFRAC = "bfrac";
    public static final String GFRAC = "gfrac";
    public static final String BETA = "beta";
    public static final String GAMMA = "gamma";

    public double alpha;
    public double frac;
    public double beta;
    public double gamma;

    public static boolean matches(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lname = name.toLowerCase();
        return lname.contains(".biexp") || lname.contains(".biexpdecay");
    }

    public BiExpDecay()
    {
        this.clear();
    }

    public BiExpDecay(BiExpDecay model)
    {
        this();
        this.set(model);
    }

    public BiExpDecay(Vect encoding)
    {
        this();
        this.setEncoding(encoding);
    }

    public int getDegreesOfFreedom()
    {
        return 4;
    }

    public void clear()
    {
        this.alpha = 0;
        this.frac = 0;
        this.beta = 0;
        this.gamma = 0;
    }

    public double baseline()
    {
        return this.alpha;
    }

    public void setAlpha(double v)
    {
        this.alpha = v;
    }

    public void setFrac(double v)
    {
        this.frac = v;
    }

    public void setBeta(double v)
    {
        this.beta = v;
    }

    public void setGamma(double v)
    {
        this.gamma = v;
    }

    public double getAlpha()
    {
        return this.alpha;
    }

    public double getFrac()
    {
        return this.frac;
    }

    public double getBeta()
    {
        return this.beta;
    }

    public double getGamma()
    {
        return this.gamma;
    }

    public double dist(BiExpDecay model)
    {
        double dalpha = this.alpha - model.alpha;
        double dfrac = this.frac - model.frac;
        double dbeta = this.beta - model.beta;
        double dgamma = this.gamma - model.gamma;

        double out = 0;
        out += dalpha * dalpha;
        out += dfrac * dfrac;
        out += dbeta * dbeta;
        out += dgamma * dgamma;
        out = Math.sqrt(out);

        return out;
    }

    @Override
    public BiExpDecay set(BiExpDecay model)
    {
        this.clear();
        this.alpha = model.alpha;
        this.frac = model.frac;
        this.beta = model.beta;
        this.gamma = model.gamma;
        return this;
    }

    @Override
    public BiExpDecay copy()
    {
        return new BiExpDecay(this);
    }

    @Override
    public BiExpDecay proto()
    {
        return new BiExpDecay();
    }

    @Override
    public int getEncodingSize()
    {
        return 4;
    }

    @Override
    public BiExpDecay setEncoding(Vect encoding)
    {
        this.alpha = encoding.get(0);
        this.frac = encoding.get(1);
        this.beta = encoding.get(2);
        this.gamma = encoding.get(3);

        return this;
    }

    @Override
    public void getEncoding(Vect encoding)
    {
        Global.assume(encoding.size() == this.getEncodingSize(), "invalid encoding");

        encoding.set(0, this.alpha);
        encoding.set(1, this.frac);
        encoding.set(2, this.beta);
        encoding.set(3, this.gamma);
    }

    @Override
    public String toString()
    {
        String out = "{alpha: " + this.alpha +
                ", frac : " + this.frac +
                ", beta : " + this.beta +
                ", gamma: " + this.gamma +
                "}";

        return out;
    }

    @Override
    public List<String> features()
    {
        List<String> out = Lists.newArrayList();
        out.add(ALPHA);
        out.add(FRAC);
        out.add(BETA);
        out.add(GAMMA);
        out.add(GFRAC);
        out.add(BFRAC);

        return out;
    }

    @Override
    public Vect feature(String name)
    {
        if (ALPHA.equals(name))
        {
            return VectSource.create1D(this.getAlpha());
        }
        if (FRAC.equals(name))
        {
            return VectSource.create1D(this.getFrac());
        }
        if (BETA.equals(name))
        {
            return VectSource.create1D(this.getBeta());
        }
        if (GAMMA.equals(name))
        {
            return VectSource.create1D(this.getGamma());
        }
        if (BFRAC.equals(name))
        {
            return VectSource.create1D(this.getFrac());
        }
        if (GFRAC.equals(name))
        {
            return VectSource.create1D(1.0 - this.getFrac());
        }

        throw new RuntimeException("invalid index: " + name);
    }

    @Override
    public BiExpDecay getThis()
    {
        return this;
    }

    public static VectFunction synth(final Vect varying)
    {
        return new VectFunction()
        {
            public void apply(Vect input, Vect output)
            {
                BiExpDecay model = new BiExpDecay(input);
                double alpha = model.getAlpha();
                double frac = model.getFrac();
                double beta = model.getBeta();
                double gamma = model.getGamma();

                for (int i = 0; i < varying.size(); i++)
                {
                    double x = varying.get(i);
                    double c1 = frac * Math.exp(-beta * x);
                    double c2 = (1.0 - frac) * Math.exp(-gamma * x);
                    double y = alpha * (c1 + c2);
                    output.set(i, y);
                }
            }
        }.init(new BiExpDecay().getEncodingSize(), varying.size());
    }
}
