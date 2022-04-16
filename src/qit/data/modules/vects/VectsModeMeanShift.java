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

package qit.data.modules.vects;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;

import java.util.Map;

@ModuleDescription("Compute the positions of modes of vectors using the mean shift algorithm")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Comaniciu, D., & Meer, P. (2002). Mean shift: A robust approach toward feature space analysis. IEEE Transactions on pattern analysis and machine intelligence, 24(5), 603-619.")
public class VectsModeMeanShift implements Module
{
    @ModuleInput
    @ModuleDescription("input vects")
    public Vects input;

    @ModuleParameter
    @ModuleDescription("the spatial bandwidth")
    public Double bandwidth = 1.0;

    @ModuleParameter
    @ModuleDescription("the error threshold for convergence")
    public Double minshift = 1e-6;

    @ModuleParameter
    @ModuleDescription("the maximum number of iterations")
    public Integer maxiter = 10000;

    @ModuleOutput
    @ModuleDescription("output masses")
    public Vects masses;

    @ModuleOutput
    @ModuleDescription("output modes")
    public Vects modes;

    @Override
    public VectsModeMeanShift run()
    {
        Vects means = new Vects();

        int num = this.input.size();
        double h2 = this.bandwidth * this.bandwidth;

        for (int i = 0; i < num; i++)
        {
            double shift = Double.MAX_VALUE;
            Vect mean = this.input.get(i);

            int iter = 0;
            while (iter < this.maxiter && shift > this.minshift)
            {
                Vect nmean = mean.proto();
                double sumk = 0;

                for (int j = 0; j < num; j++)
                {
                    Vect other = this.input.get(j);

                    double d2 = mean.dist2(other);
                    double k = Math.exp(-d2 / h2);

                    nmean.plusEquals(k, other);
                    sumk += k;
                }

                if (MathUtils.nonzero(sumk))
                {
                    nmean.timesEquals(1.0 / sumk);
                }

                shift = mean.dist(nmean);
                mean = nmean;
                iter += 1;
            }

            means.add(mean);
        }

        Vects modesOut = new Vects();
        int[] labels = new int[num];
        for (int i = 0; i < num; i++)
        {
            Vect mean = means.get(i);
            Integer label = null;

            for (int j = 0; j < modesOut.size(); j++)
            {
                if (mean.dist(modesOut.get(j)) < this.bandwidth)
                {
                    label = j;
                    break;
                }
            }

            if (label == null)
            {
                modesOut.add(mean);
                label = 0;
            }

            labels[i] = label;
        }

        Vects massesOut = new Vects();
        if (modesOut.size() > 0)
        {
            Map<Integer, Integer> counts = MathUtils.counts(labels);

            for (int i = 0; i < modesOut.size(); i++)
            {
                massesOut.add(VectSource.create1D(counts.get(i) / (double) num));
            }
        }

        this.modes = modesOut;
        this.masses = massesOut;

        return this;
    }
}
