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


package qit.data.modules.mri.noddi;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Record;
import qit.data.datasets.Table;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.models.Noddi;
import qit.data.source.VectSource;
import qit.data.utils.mri.structs.Gradients;
import qit.math.structs.VectFunction;

@ModuleDescription("This program evaluates the resolution required to accurately synthesize mri data with the NODDI model")
@ModuleAuthor("Ryan Cabeen")
@ModuleUnlisted
public class TableNoddiMeshEval implements Module
{
    @ModuleParameter
    @ModuleDescription("the gradients")
    public Gradients gradients;

    @ModuleParameter
    @ModuleDescription("the number of samples")
    public int samples = 100;

    @ModuleParameter
    @ModuleDescription("the number of samples")
    public int resolution = 4;

    @ModuleParameter
    @ModuleDescription("the baseline value")
    public double baseline = 4000;

    @ModuleParameter
    @ModuleDescription("the baseline value")
    public double ficvf = 0.5;

    @ModuleParameter
    @ModuleDescription("the baseline value")
    public double fiso = 0.1;

    @ModuleOutput
    @ModuleDescription("output table of results")
    public Table output;

    @Override
    public TableNoddiMeshEval run()
    {
        Table table = new Table();
        table.withField("odi");
        table.withField("resolution");
        table.withField("error");

        Noddi model = new Noddi();
        model.setBaseline(this.baseline);
        model.setDir(VectSource.create3D(1, 1, 1).normalize());
        model.setFICVF(this.ficvf);
        model.setFISO(this.fiso);
        Vect odis = VectSource.linspace(0, 1, this.samples);

        VectFunction truther = Noddi.synth(this.gradients, this.resolution);
        Vects truths = new Vects();
        for (int j = 0; j < odis.size(); j++)
        {
            double odi = odis.get(j);
            model.setODI(odi);
            truths.add(truther.apply(model.getEncoding()));
        }

        for (int i = 0; i < this.resolution; i++)
        {
            final VectFunction tester = Noddi.synth(this.gradients, i);

            for (int j = 0; j < odis.size(); j++)
            {
                double odi = odis.get(j);
                model.setODI(odi);
                Vect test = tester.apply(model.getEncoding());
                double error = truths.get(j).dist(test) / this.baseline;

                Record rec = new Record();
                rec.with("odi", odi);
                rec.with("resolution", i);
                rec.with("error", error);

                table.addRecord(rec);
            }
        }

        this.output = table;

        return this;
    }
}
