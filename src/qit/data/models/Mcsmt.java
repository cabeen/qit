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
import org.apache.commons.math3.special.Erf;
import qit.base.Global;
import qit.base.Model;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.List;

// multi-compartment spherical mean technique from Kaden et al.
public class Mcsmt extends Model<Mcsmt>
{
    private static final double SQRTPI = Math.sqrt(Math.PI);

    public final static String NAME = "mcsmt";

    public static final String BASE = "base";
    public static final String FRAC = "frac";
    public static final String DIFF = "diff";
    public static final String DOT = "dot";

    public double base;
    public double frac;
    public double diff;
    public double dot = 0;

    public static boolean matches(String name)
    {
        if (name == null)
        {
            return false;
        }

        String lname = name.toLowerCase();
        return lname.contains(".mcsmt");
    }

    public Mcsmt()
    {
        this.clear();
    }

    public Mcsmt(double b, double f, double d)
    {
        this.base = b;
        this.frac = f;
        this.diff = d;
    }

    public Mcsmt(double b, double f, double d, double dd)
    {
        this.base = b;
        this.frac = f;
        this.diff = d;
        this.dot = dd;
    }

    public Mcsmt(Mcsmt model)
    {
        this();
        this.set(model);
    }

    public Mcsmt(Vect encoding)
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
        this.base= 0;
        this.frac = 0;
        this.diff = 0;
    }

    public double baseline()
    {
        return this.base;
    }

    public void setBase(double v)
    {
        this.base = v;
    }

    public void setFrac(double v)
    {
        this.frac = v;
    }

    public void setDiff(double v)
    {
        this.diff = v;
    }

    public void setDot(double v)
    {
        this.dot = v;
    }

    public double getBase()
    {
        return this.base;
    }

    public double getFrac()
    {
        return this.frac;
    }

    public double getDiff()
    {
        return this.diff;
    }

    public double getDot()
    {
        return this.dot;
    }

    public Vect synth(Vect shells)
    {
        Vect out = VectSource.createND(shells.size());

        for (int i = 0; i < shells.size(); i++)
        {
            out.set(i, this.getBase());

            if (MathUtils.nonzero(this.getBase()))
            {
                double b = shells.get(i);

                double fint = Math.sqrt(b * this.getDiff());
                double sint = SQRTPI * Erf.erf(fint) / (2.0 * fint);

                double base = this.getBase();
                double frac = this.getFrac();
                double diff = this.getDiff();

                double ifrac = 1.0 - frac;
                double ifdot = (1.0 - this.dot);

                double s = this.dot * base;

                if (Double.isFinite(sint))
                {
                    s += ifdot * base * frac * sint;
                }

                double dext = ifrac * diff;
                double fext = Math.sqrt(b * (diff - dext));
                double sext = Math.exp(-b * dext) * SQRTPI * Erf.erf(fext) / (2.0 * fext);

                if (Double.isFinite(sext))
                {
                    s += ifdot * base * ifrac * sext;
                }

                out.set(i, s);
            }
        }

        return out;
    }

    public Vect synthIntrinsic(Vect shells)
    {
        Vect out = VectSource.createND(shells.size());

        for (int i = 0; i < shells.size(); i++)
        {
            out.set(i, this.getBase());

            if (MathUtils.nonzero(this.getBase()))
            {
                double s = 0;
                double b = shells.get(i);

                double fint = Math.sqrt(b * this.getDiff());
                double sint = SQRTPI * Erf.erf(fint) / (2.0 * fint);

                if (Double.isFinite(sint))
                {
                    s += this.getBase() * this.getFrac() * sint;
                }

                out.set(i, s);
            }
        }

        return out;
    }

    public Vect synthExtrinsic(Vect shells)
    {
        Vect out = VectSource.createND(shells.size());

        for (int i = 0; i < shells.size(); i++)
        {
            double b = shells.get(i);
            double base = this.getBase();

            if (MathUtils.nonzero(base) && MathUtils.nonzero(b))
            {
                double dint = this.getDiff();
                double efrac = 1.0 - this.getFrac();
                double dext = efrac * dint;
                double fext = Math.sqrt(b * (dint - dext));
                double sext = Math.exp(-b * dext) * SQRTPI * Erf.erf(fext) / (2.0 * fext);

                if (Double.isFinite(sext))
                {
                    double sigext = this.getBase() * efrac * sext;
                    out.set(i, sigext);
                }
            }
        }

        return out;
    }

    public double dist(Mcsmt model)
    {
        double dfrac = this.frac - model.frac;
        double ddiff = this.diff - model.diff;

        double out = 0;
        out += dfrac * dfrac;
        out += ddiff * ddiff;
        out = Math.sqrt(out);

        return out;
    }

    @Override
    public Mcsmt set(Mcsmt model)
    {
        this.clear();
        this.base = model.base;
        this.frac = model.frac;
        this.diff = model.diff;
        this.dot = model.dot;
        return this;
    }

    @Override
    public Mcsmt copy()
    {
        return new Mcsmt(this);
    }

    @Override
    public Mcsmt proto()
    {
        return new Mcsmt();
    }

    @Override
    public int getEncodingSize()
    {
        return 4;
    }

    @Override
    public Mcsmt setEncoding(Vect encoding)
    {
        this.base = encoding.get(0);
        this.frac = encoding.get(1);
        this.diff = encoding.get(2);
        this.dot = encoding.get(3);

        return this;
    }

    @Override
    public void getEncoding(Vect encoding)
    {
        Global.assume(encoding.size() == this.getEncodingSize(), "invalid encoding");

        encoding.set(0, this.base);
        encoding.set(1, this.frac);
        encoding.set(2, this.diff);
        encoding.set(3, this.dot);
    }

    @Override
    public String toString()
    {
        String out = "{frac: " + this.frac + ", diff : " + this.diff+ "}";

        return out;
    }

    @Override
    public List<String> features()
    {
        List<String> out = Lists.newArrayList();
        out.add(BASE);
        out.add(FRAC);
        out.add(DIFF);
        out.add(DOT);

        return out;
    }

    @Override
    public Vect feature(String name)
    {
        if (BASE.equals(name))
        {
            return VectSource.create1D(this.getBase());
        }
        if (FRAC.equals(name))
        {
            return VectSource.create1D(this.getFrac());
        }
        if (DIFF.equals(name))
        {
            return VectSource.create1D(this.getDiff());
        }
        if (DOT.equals(name))
        {
            return VectSource.create1D(this.getDot());
        }

        throw new RuntimeException("invalid index: " + name);
    }

    @Override
    public Mcsmt getThis()
    {
        return this;
    }
}