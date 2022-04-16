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
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.data.models.Noddi;
import qit.data.utils.mri.estimation.NoddiEstimator;

import java.util.List;

@ModuleUnlisted
@ModuleDescription("Demonstrate noddi model-based interpolation")
@ModuleAuthor("Ryan Cabeen")
public class VolumeNoddiEstimationBlendDemo implements Module
{
    @ModuleParameter
    @ModuleDescription("the estimation method for interpolation")
    public String estimation = NoddiEstimator.SCATTER_SPLINE_C2;

    @ModuleParameter
    @ModuleDescription("the size of the demo in voxels")
    public Integer size = 1001;

    @ModuleParameter
    @ModuleDescription("the isotropic volume fraction in corner A")
    public Double isovf = 0.05;

    @ModuleParameter
    @ModuleDescription("the intra-cellular volume fraction in corner A")
    public Double icvf = 0.50;

    @ModuleParameter
    @ModuleDescription("the orientation dispersion index in corner A")
    public Double od = 0.25;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output table")
    public Table output;

    public VolumeNoddiEstimationBlendDemo run()
    {
        Noddi aModel = new Noddi();
        aModel.setFISO(this.isovf);
        aModel.setFICVF(this.icvf);
        aModel.setODI(this.od);
        aModel.setDir(VectSource.create3D(1, 0, 0));

        Noddi bModel = new Noddi();
        bModel.setFISO(this.isovf);
        bModel.setFICVF(this.icvf);
        bModel.setODI(this.od);
        bModel.setDir(VectSource.create3D(0, 1, 0));

        NoddiEstimator estimator = new NoddiEstimator();
        estimator.estimation = this.estimation;

        List<Double> weights = Lists.newArrayList();
        weights.add(0.0);
        weights.add(0.0);

        List<Vect> models = Lists.newArrayList();
        models.add(aModel.getEncoding());
        models.add(bModel.getEncoding());

        Table outTable = new Table();
        outTable.withField("mix");
        outTable.withField("method");
        outTable.withField("od");
        outTable.withField("kappa");
        outTable.withField("tort");
        outTable.withField("ead");
        outTable.withField("erd");
        for (int i = 0; i <= this.size; i++)
        {
            double w = ((double) i) / (double) this.size;
            double iw = 1.0 - w;

            weights.set(0, iw);
            weights.set(1, w);

            for (String estimation : NoddiEstimator.METHODS)
            {
                estimator.estimation = estimation;
                Noddi model = new Noddi(estimator.run(weights, models));

                Record record = new Record();
                record.with("mix", iw);
                record.with("method", estimation);
                record.with("od", model.getODI());
                record.with("kappa", model.getKappa());
                record.with("tort", model.getTORT());
                record.with("ead", model.getEAD());
                record.with("erd", model.getERD());

                outTable.addRecord(record);
            }
        }

        this.output = outTable;

        return this;
    }
}