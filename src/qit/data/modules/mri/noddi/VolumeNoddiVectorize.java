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
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Noddi;
import qit.data.source.MatrixSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.mri.CaminoUtils;
import qit.data.utils.volume.VolumeFunction;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;

import java.util.function.Supplier;

@ModuleDescription("Vectorize a NODDI volume")
@ModuleUnlisted
@ModuleAuthor("Ryan Cabeen")
public class VolumeNoddiVectorize implements Module
{
    public static final String RANKONE = "RankOne";
    public static final String SCATTER = "Scatter";
    public static final String LOGSCATTER = "LogScatter";

    @ModuleInput
    @ModuleDescription("the input volume (either noddi or vectorized)")
    public Volume input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleParameter
    @ModuleDescription("convert a vectorized volume back to noddi")
    public boolean back = false;

    @ModuleParameter
    @ModuleDescription("the vectorization type (RankOne,Scatter,LogScatter)")
    public String method = RANKONE;

    @ModuleParameter
    @ModuleDescription("the number of threads to use")
    public int threads = 1;

    @ModuleOutput
    @ModuleDescription("the output volume (vectorized or noddi)")
    public Volume output;

    public VolumeNoddiVectorize run()
    {
        output = new VolumeFunction(factory()).withInput(this.input).withMask(this.mask).withThreads(this.threads).run();

        return this;
    }

    public Supplier<VectFunction> factory()
    {
        return () ->
        {
            if (VolumeNoddiVectorize.this.back)
            {
                return new VectFunction()
                {
                    public void apply(Vect in, Vect out)
                    {
                        double baseline = in.get(0);
                        double fiso = in.get(1);
                        double ficvf = in.get(2);

                        Matrix matrix = new Matrix(3, 3);
                        matrix.set(0, 0, in.get(3));
                        matrix.set(0, 1, in.get(4));
                        matrix.set(0, 2, in.get(5));
                        matrix.set(1, 0, in.get(6));
                        matrix.set(1, 1, in.get(7));
                        matrix.set(1, 2, in.get(8));
                        matrix.set(2, 0, in.get(9));
                        matrix.set(2, 1, in.get(10));
                        matrix.set(2, 2, in.get(11));

                        Noddi model = new Noddi();
                        model.setBaseline(baseline);
                        model.setFISO(fiso);
                        model.setFICVF(ficvf);

                        if (VolumeNoddiVectorize.this.method.equals(RANKONE))
                        {
                            MatrixUtils.EigenDecomp eig = MatrixUtils.eig(matrix);

                            model.setDir(eig.vectors.get(0));
                            model.setKappa(eig.values.get(0));
                        }
                        else
                        {

                            MatrixUtils.EigenDecomp eig = MatrixUtils.eig(matrix);

                            Vect vec1 = eig.vectors.get(0);
                            double val1 = eig.values.get(0);
                            double val2 = eig.values.get(1);

                            if (VolumeNoddiVectorize.this.method.equals(LOGSCATTER))
                            {
                                val1 = Math.exp(val1);
                                val2 = Math.exp(val2);
                            }

                            double val12 = val1 - val2;

                            if (MathUtils.zero(val12))
                            {
                                model.setDir(vec1);
                                model.setKappa(0.0);
                            }
                            else
                            {
                                // use bipolar
                                Vect mu = vec1;

                                model.setDir(mu);
                                Double kappa = CaminoUtils.kappaWatson(val1);

                                if (kappa != null)
                                {
                                    model.setKappa(kappa);
                                }
                            }
                        }

                        model.getEncoding(out);
                    }
                }.init(12, new Noddi().getEncodingSize());
            }
            else
            {
                return new VectFunction()
                {
                    public void apply(Vect in, Vect out)
                    {
                        Noddi model = new Noddi(in);
                        out.set(0, model.getBaseline());
                        out.set(1, model.getFISO());
                        out.set(2, model.getFICVF());

                        Matrix matrix = null;
                        if (VolumeNoddiVectorize.this.method.equals(RANKONE))
                        {
                            matrix = MatrixSource.dyadic(model.getDir()).times(model.getKappa());
                        }
                        else
                        {
                            matrix = model.getScatter(VolumeNoddiVectorize.this.method.equals(LOGSCATTER));
                        }

                        out.set(3, matrix.get(0, 0));
                        out.set(4, matrix.get(0, 1));
                        out.set(5, matrix.get(0, 2));
                        out.set(6, matrix.get(1, 0));
                        out.set(7, matrix.get(1, 1));
                        out.set(8, matrix.get(1, 2));
                        out.set(9, matrix.get(2, 0));
                        out.set(10, matrix.get(2, 1));
                        out.set(11, matrix.get(2, 2));
                    }
                }.init(new Noddi().getEncodingSize(), 12);
            }
        };
    }
}