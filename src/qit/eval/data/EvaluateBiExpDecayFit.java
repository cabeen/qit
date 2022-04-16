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
import qit.data.models.BiExpDecay;
import qit.data.source.VectSource;
import qit.data.utils.mri.fitting.FitBiExpDecayNLLS;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;

import java.util.Map;

@ModuleDescription("This program evaluates model fitting of bi-exponential decay models with a simulation")
@ModuleUnlisted
@ModuleAuthor("Ryan Cabeen")
public class EvaluateBiExpDecayFit implements Module
{
    @ModuleParameter
    @ModuleDescription("the number of samples")
    public int samples = 100;

    @ModuleParameter
    @ModuleDescription("the number of varying values")
    public int num = 100;

    @ModuleParameter
    @ModuleDescription("the alpha value")
    public double alpha = 400;

    @ModuleParameter
    @ModuleDescription("the beta value")
    public double frac = 0.25;

    @ModuleParameter
    @ModuleDescription("the beta value")
    public double beta = 0.15;

    @ModuleParameter
    @ModuleDescription("the beta value")
    public double gamma = 0.35;

    @ModuleParameter
    @ModuleDescription("the noise Gaussian standard deviation")
    public String noise = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20";

    @ModuleOutput
    @ModuleDescription("output table of results")
    public Table output;

    @Override
    public EvaluateBiExpDecayFit run()
    {
        Table table = new Table();
        table.withField("method");
        table.withField("noise");
        table.withField("sample");
        table.withField("alpha");
        table.withField("frac");
        table.withField("beta");
        table.withField("gamma");
        table.withField("alphaest");
        table.withField("fracest");
        table.withField("betaest");
        table.withField("gammaest");
        table.withField("alphadel");
        table.withField("fracdel");
        table.withField("betadel");
        table.withField("gammadel");
        table.withField("alphaerr");
        table.withField("fracerr");
        table.withField("betaerr");
        table.withField("gammaerr");
        table.withField("totalerr");
        table.withField("sigerr");

        BiExpDecay model = new BiExpDecay();

        model.setAlpha(this.alpha);
        model.setFrac(this.frac);
        model.setBeta(this.beta);
        model.setGamma(this.gamma);

        Vect varying = VectSource.createND(this.num);
        for (int i = 0; i < this.num; i++)
        {
            varying.set(i, i);
        }

        VectFunction synther = BiExpDecay.synth(varying);
        Vect signal = synther.apply(model.getEncoding());

        Map<String, VectFunction> fitters = Maps.newLinkedHashMap();
        fitters.put(FitBiExpDecayNLLS.NLLS, FitBiExpDecayNLLS.get(varying));

        for (String ntoken : this.noise.split(","))
        {
            VectFunction noiser = VectFunctionSource.gaussianND(varying.size(), Double.valueOf(ntoken));

            for (int i = 0; i < this.samples; i++)
            {
                Vect noised = noiser.apply(signal);

                for (String method : fitters.keySet())
                {
                    BiExpDecay fitted = new BiExpDecay(fitters.get(method).apply(noised));
                    Vect predsig = synther.apply(fitted.getEncoding());

                    double alphaest = fitted.getAlpha();
                    double fracest = fitted.getFrac();
                    double betaest = fitted.getBeta();
                    double gammaest = fitted.getGamma();
                    
                    double alphadel = (alphaest - this.alpha) / this.alpha;
                    double fracdel = (fracest - this.frac) / this.frac;
                    double betadel = (betaest - this.beta) / this.beta;
                    double gammadel = (gammaest - this.gamma) / this.gamma;

                    double alphaerr = Math.abs(alphadel);
                    double fracerr = Math.abs(fracdel);
                    double betaerr = Math.abs(betadel);
                    double gammaerr = Math.abs(gammadel);

                    double totalerr = 0.25 * (alphaerr + fracerr + betaerr + gammaerr);
                    double sigerr = predsig.dist(signal) / this.alpha;

                    Record rec = new Record();
                    rec.with("method", method);
                    rec.with("noise", ntoken);
                    rec.with("sample", i);
                    rec.with("alpha", this.alpha);
                    rec.with("frac", this.frac);
                    rec.with("beta", this.beta);
                    rec.with("gamma", this.gamma);
                    rec.with("alphaest", alphaest);
                    rec.with("fracest", fracest);
                    rec.with("betaest", betaest);
                    rec.with("gammaest", gammaest);
                    rec.with("alphadel", alphadel);
                    rec.with("fracdel", fracdel);
                    rec.with("betadel", betadel);
                    rec.with("gammadel", gammadel);
                    rec.with("alphaerr", alphaerr);
                    rec.with("fracerr", fracerr);
                    rec.with("betaerr", betaerr);
                    rec.with("gammaerr", gammaerr);
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
