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

package qit.data.modules.mri.noddi;

import com.google.common.collect.Lists;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.models.Noddi;
import qit.data.utils.mri.estimation.NoddiEstimator;

import java.util.List;

@ModuleUnlisted
@ModuleDescription("Demonstrate noddi model-based interpolation")
@ModuleAuthor("Ryan Cabeen")
public class VolumeNoddiEstimationImageDemo implements Module
{
    @ModuleParameter
    @ModuleDescription("the estimation method for interpolation")
    public String estimation = NoddiEstimator.SCATTER_SPLINE_C2;

    @ModuleParameter
    @ModuleDescription("the size of the demo in voxels")
    public Integer size = 11;

    @ModuleParameter
    @ModuleDescription("the isotropic volume fraction in corner A")
    public Double aisovf = 0.05;

    @ModuleParameter
    @ModuleDescription("the isotropic volume fraction in corner B")
    public Double bisovf = 0.05;

    @ModuleParameter
    @ModuleDescription("the isotropic volume fraction in corner C")
    public Double cisovf = 0.05;

    @ModuleParameter
    @ModuleDescription("the isotropic volume fraction in corner D")
    public Double disovf = 0.05;

    @ModuleParameter
    @ModuleDescription("the intra-cellular volume fraction in corner A")
    public Double aicvf = 0.50;

    @ModuleParameter
    @ModuleDescription("the intra-cellular volume fraction in corner B")
    public Double bicvf = 0.50;

    @ModuleParameter
    @ModuleDescription("the intra-cellular volume fraction in corner C")
    public Double cicvf = 0.50;

    @ModuleParameter
    @ModuleDescription("the intra-cellular volume fraction in corner D")
    public Double dicvf = 0.50;

    @ModuleParameter
    @ModuleDescription("the orientation dispersion index in corner A")
    public Double aod = 1.0;

    @ModuleParameter
    @ModuleDescription("the orientation dispersion index in corner B")
    public Double bod = 0.5;

    @ModuleParameter
    @ModuleDescription("the orientation dispersion index in corner C")
    public Double cod = 0.5;

    @ModuleParameter
    @ModuleDescription("the orientation dispersion index in corner D")
    public Double dod = 0.5;

    @ModuleParameter
    @ModuleDescription("the fiber angle in corner A")
    public Double atheta = 0.0;

    @ModuleParameter
    @ModuleDescription("the fiber angle in corner B")
    public Double btheta = 0.0;

    @ModuleParameter
    @ModuleDescription("the fiber angle in corner C")
    public Double ctheta = Math.PI / 4.0;

    @ModuleParameter
    @ModuleDescription("the fiber angle in corner D")
    public Double dtheta = Math.PI / 2.0;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output volume")
    public Volume output;

    public VolumeNoddiEstimationImageDemo run()
    {
        Noddi proto = new Noddi();
        Sampling sampling = SamplingSource.create(this.size, this.size, 1);
        Volume outVolume = VolumeSource.create(sampling, proto.getEncodingSize());
        int max = this.size - 1;

        Noddi aModel = new Noddi();
        aModel.setFISO(this.aisovf);
        aModel.setFICVF(this.aicvf);
        aModel.setODI(this.aod);
        aModel.setDir(VectSource.create3D(Math.cos(this.atheta), Math.sin(this.atheta), 0));

        Noddi bModel = new Noddi();
        bModel.setFISO(this.bisovf);
        bModel.setFICVF(this.bicvf);
        bModel.setODI(this.bod);
        bModel.setDir(VectSource.create3D(Math.cos(this.btheta), Math.sin(this.btheta), 0));

        Noddi cModel = new Noddi();
        cModel.setFISO(this.cisovf);
        cModel.setFICVF(this.cicvf);
        cModel.setODI(this.cod);
        cModel.setDir(VectSource.create3D(Math.cos(this.ctheta), Math.sin(this.ctheta), 0));

        Noddi dModel = new Noddi();
        dModel.setFISO(this.disovf);
        dModel.setFICVF(this.dicvf);
        dModel.setODI(this.dod);
        dModel.setDir(VectSource.create3D(Math.cos(this.dtheta), Math.sin(this.dtheta), 0));

        NoddiEstimator estimator = new NoddiEstimator();
        estimator.estimation = this.estimation;

        List<Double> weights = Lists.newArrayList();
        weights.add(0.0);
        weights.add(0.0);
        weights.add(0.0);
        weights.add(0.0);

        List<Vect> models = Lists.newArrayList();
        models.add(aModel.getEncoding());
        models.add(bModel.getEncoding());
        models.add(cModel.getEncoding());
        models.add(dModel.getEncoding());

        outVolume.set(0, 0, 0, aModel.getEncoding());
        outVolume.set(max, 0, 0, bModel.getEncoding());
        outVolume.set(max, max, 0, cModel.getEncoding());
        outVolume.set(0, max, 0, dModel.getEncoding());

        weights.set(2, 0.32);
        weights.set(3, 0.67);
        estimator.run(weights, models);

        weights.set(2, 0.33);
        weights.set(3, 0.66);
        estimator.run(weights, models);

        weights.set(2, 0.35);
        weights.set(3, 0.65);
        estimator.run(weights, models);

        weights.set(2, 0.40);
        weights.set(3, 0.60);
        estimator.run(weights, models);

        weights.set(2, 0.50);
        weights.set(3, 0.50);
        estimator.run(weights, models);

        for (int i = 0; i < this.size; i++)
        {
            for (int j = 0; j < this.size; j++)
            {
                double iw = ((double) i) / max;
                double jw = ((double) j) / max;
                double riw = 1.0 - iw;
                double rjw = 1.0 - jw;

                weights.set(0, riw * rjw);
                weights.set(1, iw * rjw);
                weights.set(2, iw * jw);
                weights.set(3, riw * jw);

                Vect estimate = estimator.run(weights, models);

                outVolume.set(i, j, 0, estimate);
            }
        }


        this.output = outVolume;

        return this;
    }
}