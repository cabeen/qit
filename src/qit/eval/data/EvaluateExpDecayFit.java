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
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.models.ExpDecay;
import qit.data.source.VectSource;
import qit.data.utils.mri.fitting.FitExpDecayLLS;
import qit.data.utils.mri.fitting.FitExpDecayNLLS;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

import java.util.Map;

@ModuleDescription("This program evaluates model fitting of exponential decay models with a simulation")
@ModuleUnlisted
@ModuleAuthor("Ryan Cabeen")
public class EvaluateExpDecayFit implements Module
{
    @ModuleParameter
    @ModuleDescription("the number of samples")
    public int samples = 100;

    @ModuleParameter
    @ModuleDescription("the number of varying values")
    public int num = 5;

    @ModuleParameter
    @ModuleDescription("the alpha value")
    public double alpha = 100;

    @ModuleParameter
    @ModuleDescription("the beta value")
    public double beta = 1;

    @ModuleParameter
    @ModuleDescription("the noise Gaussian standard deviation")
    public String noise = "0,1,2,3,4,5,6,7,8,9,10";

    @ModuleOutput
    @ModuleDescription("output table of results")
    public Table output;

    @Override
    public EvaluateExpDecayFit run()
    {
        Table table = new Table();
        table.withField("method");
        table.withField("noise");
        table.withField("sample");
        table.withField("alpha");
        table.withField("beta");
        table.withField("alphaest");
        table.withField("betaest");
        table.withField("alphadel");
        table.withField("betadel");
        table.withField("alphaerr");
        table.withField("betaerr");
        table.withField("totalerr");
        table.withField("sigerr");

        ExpDecay model = new ExpDecay();

        model.setAlpha(this.alpha);
        model.setBeta(this.beta);

        Vect varying = VectSource.createND(this.num);
        for (int i = 0; i < this.num; i++)
        {
            varying.set(i, i);
        }

        VectFunction synther = ExpDecay.synth(varying);
        Vect signal = synther.apply(model.getEncoding());

        Map<String, VectFunction> fitters = Maps.newLinkedHashMap();
        fitters.put(FitExpDecayLLS.LLS, new FitExpDecayLLS().withVarying(varying).withWeighted(false).get());
        fitters.put(FitExpDecayLLS.WLLS, new FitExpDecayLLS().withVarying(varying).withWeighted(true).get());
        fitters.put(FitExpDecayNLLS.NLLS, new FitExpDecayNLLS().withVarying(varying).get());

        for (String ntoken : this.noise.split(","))
        {
            VectFunction noiser = VectFunctionSource.gaussianND(varying.size(), Double.valueOf(ntoken));

            for (int i = 0; i < this.samples; i++)
            {
                Vect noised = noiser.apply(signal);

                for (String method : fitters.keySet())
                {
                    ExpDecay fitted = new ExpDecay(fitters.get(method).apply(noised));
                    Vect predsig = synther.apply(fitted.getEncoding());

                    double alphaest = fitted.getAlpha();
                    double betaest = fitted.getBeta();
                    double alphadel = (alphaest - this.alpha) / this.alpha;
                    double betadel = (betaest - this.beta) / this.beta;
                    double alphaerr = Math.abs(alphadel);
                    double betaerr = Math.abs(betadel);
                    double totalerr = 0.5 * (alphaerr + betaerr);
                    double sigerr = predsig.dist(signal) / this.alpha;

                    Record rec = new Record();
                    rec.with("method", method);
                    rec.with("noise", ntoken);
                    rec.with("sample", i);
                    rec.with("alpha", this.alpha);
                    rec.with("beta", this.beta);
                    rec.with("alphaest", alphaest);
                    rec.with("betaest", betaest);
                    rec.with("alphadel", alphadel);
                    rec.with("betadel", betadel);
                    rec.with("alphaerr", alphaerr);
                    rec.with("betaerr", betaerr);
                    rec.with("totalerr", totalerr);
                    rec.with("sigerr", sigerr);

                    table.addRecord(rec);
                }
            }
        }

        this.output = table;

        return this;
    }
}
