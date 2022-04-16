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

import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.models.Tensor;
import qit.data.source.VectSource;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.fitting.FitTensorLLS;
import qit.data.utils.mri.fitting.FitTensorSimplexNLLS;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;

import java.util.Map;

@ModuleDescription("This program evaluates model fitting of the tensor model with a simulation")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class EvaluateTensorFit implements Module
{
    @ModuleParameter
    @ModuleDescription("the gradients used for synthesis")
    public Gradients gradients;

    @ModuleParameter
    @ModuleDescription("the number of samples")
    public int samples = 100;

    @ModuleParameter
    @ModuleDescription("the baseline value")
    public double baseline = 250;

    @ModuleParameter
    @ModuleDescription("the first eigenvalues (comma delimited)")
    public double first = 4e-3;

    @ModuleParameter
    @ModuleDescription("the second eigenvalues (comma delimited)")
    public double second = 2e-3;

    @ModuleParameter
    @ModuleDescription("the third eigenvalues (comma delimited)")
    public double third = 1e-3;

    @ModuleParameter
    @ModuleDescription("the noise standard deviations to test")
    public String noise = "0,1,2,3,4,5,6,7,8,9,10";

    @ModuleParameter
    @ModuleDescription("use Rician noise model")
    public boolean rician = false;

    @ModuleOutput
    @ModuleDescription("output table of results")
    public Table output;

    @Override
    public EvaluateTensorFit run()
    {
        Table table = new Table();
        table.withField("fitter");
        table.withField("noise");
        table.withField("sample");
        table.withField("baseline");
        table.withField("first");
        table.withField("second");
        table.withField("third");
        table.withField("fa");
        table.withField("md");
        table.withField("rd");
        table.withField("ad");
        table.withField("firstest");
        table.withField("secondest");
        table.withField("thirdest");
        table.withField("faest");
        table.withField("mdest");
        table.withField("rdest");
        table.withField("adest");
        table.withField("firstdel");
        table.withField("seconddel");
        table.withField("thirddel");
        table.withField("fadel");
        table.withField("mddel");
        table.withField("rddel");
        table.withField("addel");
        table.withField("firsterr");
        table.withField("seconderr");
        table.withField("thirderr");
        table.withField("faerr");
        table.withField("mderr");
        table.withField("rderr");
        table.withField("aderr");
        table.withField("direrr");
        table.withField("sigerr");
        table.withField("runtime");

        Vect d1 = VectSource.randomUnit();
        Vect d2 = d1.perp();
        Vect d3 = d1.cross(d2);

        Tensor model = new Tensor();
        model.setBaseline(this.baseline);
        model.setVec(0, d1);
        model.setVec(1, d2);
        model.setVec(2, d3);
        model.setVal(0, this.first);
        model.setVal(1, this.second);
        model.setVal(2, this.third);

        double fa = model.feature(Tensor.FEATURES_FA).get(0);
        double md = model.feature(Tensor.FEATURES_MD).get(0);
        double rd = model.feature(Tensor.FEATURES_RD).get(0);
        double ad = model.feature(Tensor.FEATURES_AD).get(0);

        VectFunction synther = Tensor.synth(this.gradients);

        Map<String, VectFunction> fitters = Maps.newLinkedHashMap();

        {
            FitTensorLLS fit = new FitTensorLLS();
            fit.gradients = this.gradients;
            fitters.put("lls", fit.get());
        }

        {
            FitTensorLLS fit = new FitTensorLLS();
            fit.gradients = this.gradients;
            fit.baseline = true;
            fitters.put("blls", fit.get());
        }
        {
            FitTensorLLS fit = new FitTensorLLS();
            fit.gradients = this.gradients;
            fit.weighted = true;
            fitters.put("wlls", fit.get());
        }
        {
            FitTensorLLS fit = new FitTensorLLS();
            fit.gradients = this.gradients;
            fit.weighted = true;
            fit.baseline = true;
            fitters.put("bwlls", fit.get());
        }
        {
            FitTensorSimplexNLLS fit = new FitTensorSimplexNLLS();
            fit.gradients = this.gradients;
            fit.baseline = true;
            fitters.put("bnlls", fit.get());
        }

        {
            FitTensorSimplexNLLS fit = new FitTensorSimplexNLLS();
            fit.gradients = this.gradients;
            fitters.put("nlls", fit.get());
        }

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
                    Tensor fitted = new Tensor(fitters.get(sfitter).apply(noised));
                    long runtime = System.currentTimeMillis() - start;

                    Vect predsig = synther.apply(fitted.getEncoding());

                    double firstest = fitted.getVal(0);
                    double secondest = fitted.getVal(1);
                    double thirdest = fitted.getVal(2);
                    double faest = fitted.feature(Tensor.FEATURES_FA).get(0);
                    double mdest = fitted.feature(Tensor.FEATURES_MD).get(0);
                    double rdest = fitted.feature(Tensor.FEATURES_RD).get(0);
                    double adest = fitted.feature(Tensor.FEATURES_AD).get(0);

                    double firstdel = (firstest - this.first) / this.first;
                    double seconddel = (secondest - this.second) / this.second;
                    double thirddel = (thirdest - this.third) / this.third;
                    double fadel = (faest - fa) / fa;
                    double mddel = (mdest - md) / md;
                    double rddel = (rdest - rd) / rd;
                    double addel = (adest - ad) / ad;

                    double firsterr = Math.abs(firstdel);
                    double seconderr = Math.abs(seconddel);
                    double thirderr = Math.abs(thirddel);
                    double faerr = Math.abs(fadel);
                    double mderr = Math.abs(mddel);
                    double rderr = Math.abs(rddel);
                    double aderr = Math.abs(addel);

                    double direrr = fitted.getVec(0).angleLineDeg(model.getVec(0));
                    double sigerr = predsig.dist(signal) / this.baseline;

                    Record rec = new Record();
                    rec.with("fitter", sfitter);
                    rec.with("noise", snoise);
                    rec.with("sample", i);
                    rec.with("baseline", this.baseline);
                    rec.with("first", this.first);
                    rec.with("second", this.second);
                    rec.with("third", this.third);
                    rec.with("fa", fa);
                    rec.with("md", md);
                    rec.with("rd", rd);
                    rec.with("ad", ad);
                    rec.with("firstest", firstest);
                    rec.with("secondest", secondest);
                    rec.with("thirdest", thirdest);
                    rec.with("faest", faest);
                    rec.with("mdest", mdest);
                    rec.with("rdest", rdest);
                    rec.with("adest", adest);
                    rec.with("firstdel", firstdel);
                    rec.with("seconddel", seconddel);
                    rec.with("thirddel", thirddel);
                    rec.with("fadel", fadel);
                    rec.with("mddel", mddel);
                    rec.with("rddel", rddel);
                    rec.with("addel", addel);
                    rec.with("firsterr", firsterr);
                    rec.with("seconderr", seconderr);
                    rec.with("thirderr", thirderr);
                    rec.with("faerr", faerr);
                    rec.with("mderr", mderr);
                    rec.with("rderr", rderr);
                    rec.with("aderr", aderr); 
                    rec.with("direrr", direrr);
                    rec.with("sigerr", sigerr);
                    rec.with("runtime", runtime);

                    table.addRecord(rec);
                }
            }
        }

        this.output = table;

        return this;
    }
}
