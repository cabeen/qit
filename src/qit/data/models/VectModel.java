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

public class VectModel extends Model<VectModel>
{
    public static final String MAG = "mag";
    public static final String SUM = "sum";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String MEAN = "mean";
    public static final String AMP = "amp";
    public Vect vect;

    public VectModel(int dim)
    {
        this.vect = new Vect(dim);
    }

    public VectModel(VectModel model)
    {
        this(model.getEncodingSize());
        this.set(model);
    }

    public VectModel(Vect encoding)
    {
        this(encoding.size());
        this.vect.set(encoding);
    }

    public int getDegreesOfFreedom()
    {
        return this.vect.size();
    }

    public double dist(VectModel model)
    {
        return this.vect.dist2(model.vect);
    }

    @Override
    public double baseline()
    {
        return this.vect.norm();
    }

    @Override
    public VectModel set(VectModel model)
    {
        this.vect.set(model.vect);
        return this;
    }

    @Override
    public VectModel copy()
    {
        return new VectModel(this);
    }

    @Override
    public VectModel proto()
    {
        return new VectModel(this.getEncodingSize());
    }

    @Override
    public int getEncodingSize()
    {
        return this.vect.size();
    }

    @Override
    public VectModel setEncoding(Vect encoding)
    {
        this.vect.set(encoding);

        return this;
    }

    @Override
    public void getEncoding(Vect encoding)
    {
        Global.assume(encoding.size() == this.getEncodingSize(), "invalid encoding");
        encoding.set(this.vect);
    }

    @Override
    public String toString()
    {
        return this.toString();
    }

    @Override
    public List<String> features()
    {
        List<String> out = Lists.newArrayList();
        out.add(MAG);
        out.add(SUM);
        out.add(MEAN);
        out.add(MIN);
        out.add(MAX);

        return out;
    }

    @Override
    public Vect feature(String name)
    {
        if (MAG.equals(name))
        {
            return VectSource.create1D(this.vect.norm());
        }

        if (SUM.equals(name))
        {
            return VectSource.create1D(this.vect.sum());
        }

        if (MEAN.equals(name))
        {
            return VectSource.create1D(this.vect.mean());
        }

        if (MIN.equals(name))
        {
            return VectSource.create1D(this.vect.min());
        }

        if (MAX.equals(name))
        {
            return VectSource.create1D(this.vect.max());
        }

        throw new RuntimeException("invalid index: " + name);
    }

    @Override
    public VectModel getThis()
    {
        return this;
    }
}
