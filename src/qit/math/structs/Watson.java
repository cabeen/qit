/*******************************************************************************
*
* Copyright (c) 2010-2016, Ryan Cabeen
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in the
*    documentation and/or other materials provided with the distribution.
* 3. All advertising materials mentioning features or use of this software
*    must display the following acknowledgement:
*    This product includes software developed by the Ryan Cabeen.
* 4. Neither the name of the Ryan Cabeen nor the
*    names of its contributors may be used to endorse or promote products
*    derived from this software without specific prior written permission.
*
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
*
*******************************************************************************/

package qit.math.structs;

import qit.base.JsonDataset;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.mri.CaminoUtils;
import qit.math.utils.MathUtils;

import java.io.IOException;

public class Watson extends JsonDataset
{
    private Vect mu;
    private double kappa;

    @SuppressWarnings("unused")
    private Watson()
    {
    }

    public Watson(Vect mean, double kappa)
    {
        this.mu = mean.copy();
        this.kappa = kappa;
    }

    public Vect getMu()
    {
        return this.mu.copy();
    }

    public double getKappa()
    {
        return this.kappa;
    }

    public Vect sample()
    {
        return CaminoUtils.sampleWatson(this.mu, this.kappa);
    }

    public double density(Vect input)
    {
        return CaminoUtils.densityWatson(input, this.mu, this.kappa);
    }

    public double nll(Vect input)
    {
        return CaminoUtils.nllWatson(input, this.mu, this.kappa);
    }

    public double nll(Vects points)
    {
        double nll = 0;
        for (Vect v : points)
        {
            nll += this.nll(v);
        }

        return nll;
    }

    public double bic(Vects points)
    {
        double nll = this.nll(points);
        double numParam = 3; // two for direction, one for dispersion
        double numPoints = points.size();
        double bic = numParam * Math.log(numPoints) + 2 * nll;

        return bic;
    }

    public static Watson read(String fn) throws IOException
    {
        return JsonDataset.read(Watson.class, fn);
    }

    public static double dispersionIndexToKappa(double odi)
    {
        double s = MathUtils.sign(odi);
        double k = s * 1.0 / Math.tan(s * Math.PI * odi / 2.0);
        if (Double.isInfinite(k))
        {
            k = Double.MAX_VALUE;
        }

        return k;
    }

    public static double kappaToDispersionIndex(double kappa)
    {
        return (2.0 / Math.PI) * Math.atan2(1.0, Math.abs(kappa));
    }

    public Watson copy()
    {
        return new Watson(this.mu.copy(), this.kappa);
    }
}