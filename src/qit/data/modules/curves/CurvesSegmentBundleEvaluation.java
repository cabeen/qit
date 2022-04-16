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


package qit.data.modules.curves;

import qit.base.Global;
import qit.base.JsonDataset;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Curves;
import qit.data.datasets.Record;
import qit.data.datasets.Schema;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.VolumeSource;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.math.structs.CovarianceType;
import qit.math.structs.Gaussian;

@ModuleUnlisted
@ModuleDescription("Evaluate a Gaussian model for bundle segmentation")
@ModuleCitation("(in preparation)")
@ModuleAuthor("Ryan Cabeen")
public class CurvesSegmentBundleEvaluation implements Module
{
    @ModuleInput
    @ModuleDescription("the subjects curves")
    public Curves subjects;

    @ModuleInput
    @ModuleDescription("the atlas curves")
    public Curves atlas;

    @ModuleInput
    @ModuleDescription("the landmarks")
    public Vects landmarks;

    @ModuleParameter
    @ModuleDescription("the covariance type (full,diagonal,spherical)")
    public String type = "full";

    @ModuleParameter
    @ModuleDescription("the number of prior values")
    public int numprior = 200;

    @ModuleParameter
    @ModuleDescription("the minima prior value")
    public double minprior = 1;

    @ModuleParameter
    @ModuleDescription("the maxima prior value")
    public double maxprior = 250;

    @ModuleParameter
    @ModuleDescription("the number of mix values")
    public int nummix = 201;

    @ModuleParameter
    @ModuleDescription("the minima mix value")
    public double minmix = 0.0;

    @ModuleParameter
    @ModuleDescription("the maxima mix value")
    public double maxmix = 0.1;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output complete results")
    public Table results = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output complete results")
    public Table best = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output visualizatin")
    public Volume vis = null;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output best model")
    public JsonDataset model = null;

    public CurvesSegmentBundleEvaluation run()
    {
        Global.assume(this.minprior > 0, "prior must be positive");
        Global.assume(this.numprior > 0, "prior number must be positive");
        Global.assume(this.maxprior > this.minprior, "prior not well defined");
        Global.assume(this.minmix >= 0, "mix must be positive");
        Global.assume(this.nummix > 0, "mix number must be positive");
        Global.assume(this.maxmix > this.minmix, "mix not well defined");

        CurvesClosestPointTransform cpta = new CurvesClosestPointTransform();
        cpta.landmarks = this.landmarks;
        cpta.input = this.atlas;
        Vects vatlas = cpta.run().output;

        CurvesClosestPointTransform cpts = new CurvesClosestPointTransform();
        cpts.landmarks = this.landmarks;
        cpts.input = this.subjects;
        Vects vsubjects = cpts.run().output;

        Logging.info("fitting baseline model");
        VectsGaussianFitter fitter = new VectsGaussianFitter();
        fitter.withInput(vsubjects);
        fitter.withType(CovarianceType.valueOf(this.type));
        Gaussian gsubjects = fitter.getOutput();

        Logging.info("started parameter sweep");

        Schema schema = new Schema();
        schema.add("mix");
        schema.add("prior");
        schema.add("kl");

        Table resultsTable = new Table(schema);

        double best = Double.MAX_VALUE;
        Record bestRow = null;
        Gaussian bestModel = null;

        Volume volume = VolumeSource.create(this.numprior, this.nummix, 1, 3);
        double delmix = (this.maxmix - this.minmix) / (this.nummix - 1);
        double delprior = (this.maxprior - this.minprior) / (this.numprior - 1);

        for (int j = 0; j < this.nummix; j++)
        {
            for (int i = 0; i < this.numprior; i++)
            {
                Logging.info(String.format(" sweeping: i = %d of %d, j = %d of %d", i + 1, this.numprior, j + 1, this.nummix));
                double mix = this.minmix + delmix * j;
                double prior = this.minprior + delprior * i;

                VectsGaussianFitter f = new VectsGaussianFitter();
                f.withInput(vatlas);
                f.withType(CovarianceType.valueOf(this.type));
                f.withPrior(prior);
                f.withMix(mix);
                Gaussian gatlas = f.getOutput();

                double kl = 0;

                if ("diagonal".equals(this.type) || "spherical".equals(this.type))
                {
                    int d = gatlas.getDim();

                    double logDetLeft = gsubjects.getCov().diag().log().sum();
                    double logDetRight = gatlas.getCov().diag().log().sum();

                    double logdet = logDetRight - logDetLeft;
                    double trace = gatlas.getCov().diag().recip().dot(gsubjects.getCov().diag());
                    Vect del = gsubjects.getMean().minus(gatlas.getMean());
                    double sdel = del.dot(gsubjects.getCov().diag().recip().times(del));

                    kl = 0.5 * (logdet - d + trace + sdel);
                }
                else
                {
                    gsubjects.kl(gatlas);
                }

                volume.set(i, j, 0, 0, prior);
                volume.set(i, j, 0, 1, mix);
                volume.set(i, j, 0, 2, kl);

                Record rec = new Record();
                rec.with("mix", String.valueOf(mix));
                rec.with("prior", String.valueOf(prior));
                rec.with("kl", String.valueOf(kl));
                resultsTable.addRecord(rec);

                if (kl < best)
                {
                    best = kl;
                    bestRow = rec;
                    bestModel = gatlas;
                }
            }
        }

        Logging.info("writing results");
        Table bestTable = new Table(schema);
        if (bestRow != null)
        {
            bestTable.addRecord(0, bestRow);
        }
        this.best = bestTable;

        this.results = resultsTable;
        this.model = bestModel;
        this.vis = volume;

        return this;
    }
}