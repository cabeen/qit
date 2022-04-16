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


package qit.eval.data;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.base.utils.ComboUtils;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.models.Noddi;
import qit.data.source.VectSource;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.fitting.FitNoddiSMT;
import qit.data.utils.mri.fitting.FitNoddiSimplex;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;

import java.util.List;
import java.util.Map;

@ModuleDescription("This program evaluates model fitting of the NODDI model with a simulation")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class EvaluateNoddiFit implements Module
{
    @ModuleInput
    @ModuleDescription("the gradients used for synthesis")
    public Gradients gradients;

    @ModuleParameter
    @ModuleDescription("the experiment name")
    public String name = "experiment";

    @ModuleParameter
    @ModuleDescription("the number of samples")
    public int samples = 200;

    @ModuleParameter
    @ModuleDescription("the baseline value")
    public double baseline = 100;

    @ModuleParameter
    @ModuleDescription("the ficvf values to test (comma delimited)")
    public String ficvf = "0.25,0.5,0.75";

    @ModuleParameter
    @ModuleDescription("the fiso values to test (comma delimited)")
    public String fiso = "0.0";

    @ModuleParameter
    @ModuleDescription("the odi values to test (comma delimited)")
    public String odi = "0.25,0.5,0.75";

    @ModuleParameter
    @ModuleDescription("the noise standard deviations to test")
    public String noise = "0,1,2,3,4,5";

    @ModuleParameter
    @ModuleDescription("use Rician noise model")
    public boolean rician = false;

    @ModuleParameter
    @ModuleDescription("use tuples of parameters instead of a cartesian product")
    public boolean tuples = false;

    @ModuleOutput
    @ModuleDescription("output table of results")
    public Table output;

    @Override
    public EvaluateNoddiFit run()
    {
        Table table = new Table();
        table.withField("name");
        table.withField("fitter");
        table.withField("noise");
        table.withField("sample");
        table.withField("pid");
        table.withField("baseline");
        table.withField("ficvf");
        table.withField("odi");
        table.withField("fiso");
        table.withField("ficvfest");
        table.withField("odiest");
        table.withField("fisoest");
        table.withField("ficvfdel");
        table.withField("odidel");
        table.withField("fisodel");
        table.withField("ficvferr");
        table.withField("odierr");
        table.withField("fisoerr");
        table.withField("nrmse");
        table.withField("runtime");

        Noddi model = new Noddi();
        model.setBaseline(this.baseline);
        model.setDir(VectSource.randomUnit());

        VectFunction synther = Noddi.synth(this.gradients);

        Map<String, VectFunction> fitters = Maps.newLinkedHashMap();

        {
            FitNoddiSimplex fit = new FitNoddiSimplex();
            fit.gradients = this.gradients;
            fit.maxiter = 10000;
            fit.rhobeg = 0.1;
            fit.rhoend = 1e-4;
            fitters.put("simplex", fit.get());
        }
        {
            FitNoddiSMT fit = new FitNoddiSMT();
            fit.gradients = this.gradients;
            fit.full = true;
            fit.maxiter = 1000;
            fit.rhobeg = 0.1;
            fit.rhoend = 1e-4;
            fitters.put("smt", fit.get());
        }

        Map<String, List<String>> inputs = Maps.newHashMap();
        inputs.put("ficvf", Lists.newArrayList(this.ficvf.split(",")));
        inputs.put("fiso", Lists.newArrayList(this.fiso.split(",")));
        inputs.put("odi", Lists.newArrayList(this.odi.split(",")));

        int pid = 0;
        for (Map<String, String> params : ComboUtils.groups(inputs, !this.tuples))
        {
            double ficvf = Double.valueOf(params.get("ficvf"));
            double fiso = Double.valueOf(params.get("fiso"));
            double odi = Double.valueOf(params.get("odi"));

            model.setFICVF(ficvf);
            model.setODI(Double.valueOf(odi));
            model.setFISO(Double.valueOf(fiso));

            Logging.info("... testing ficvf: " + ficvf);
            Logging.info("... testing odi: " + odi);
            Logging.info("... testing fiso: " + fiso);

            for (String sfitter : fitters.keySet())
            {
                Logging.info("...... testing fitter: " + sfitter);

                Vect signal = synther.apply(model.getEncoding());

                for (String snoise : this.noise.split(","))
                {
                    Logging.info("......... testing noise: " + snoise);
                    VectFunction noiser = ModelUtils.noiseND(this.gradients.size(), Double.valueOf(snoise), this.rician);

                    for (int i = 0; i < this.samples; i++)
                    {
                        Logging.info("............ testing sample: " + i);
                        Vect noised = noiser.apply(signal);

                        long start = System.currentTimeMillis();
                        Noddi fitted = new Noddi(fitters.get(sfitter).apply(noised));
                        long runtime = System.currentTimeMillis() - start;

                        Vect pred = synther.apply(fitted.getEncoding());
                        double nrmse = pred.dist(noised) / this.baseline;

                        double ficvfest = fitted.getFICVF();
                        double odiest = fitted.getODI();
                        double fisoest = fitted.getFISO();

                        double ficvfdel = (ficvfest - ficvf);
                        double odidel = (odiest - odi);
                        double fisodel = (fisoest - fiso);

                        double ficvferr = Math.abs(ficvfdel);
                        double odierr = Math.abs(odidel);
                        double fisoerr = Math.abs(fisodel);

                        Record rec = new Record();
                        rec.with("name", this.name);
                        rec.with("fitter", sfitter);
                        rec.with("noise", snoise);
                        rec.with("sample", i);
                        rec.with("pid", pid);
                        rec.with("baseline", this.baseline);
                        rec.with("ficvf", ficvf);
                        rec.with("odi", odi);
                        rec.with("fiso", fiso);
                        rec.with("ficvfest", ficvfest);
                        rec.with("odiest", odiest);
                        rec.with("fisoest", fisoest);
                        rec.with("ficvfdel", ficvfdel);
                        rec.with("odidel", odidel);
                        rec.with("fisodel", fisodel);
                        rec.with("ficvferr", ficvferr);
                        rec.with("odierr", odierr);
                        rec.with("fisoerr", fisoerr);
                        rec.with("nrmse", nrmse);
                        rec.with("runtime", runtime);

                        table.addRecord(rec);
                    }
                }
            }

            pid += 1;
        }

        this.output = table;

        return this;
    }
}
