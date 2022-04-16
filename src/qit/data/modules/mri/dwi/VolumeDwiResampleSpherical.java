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

package qit.data.modules.mri.dwi;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.source.MatrixSource;
import qit.data.utils.MatrixUtils;
import qit.data.utils.MeshUtils;
import qit.data.utils.VectsUtils;
import qit.data.utils.mri.structs.Gradients;
import qit.data.utils.mri.ModelUtils;
import qit.math.structs.Bary;
import qit.math.structs.Face;
import qit.math.structs.Triangle;
import qit.math.source.VectFunctionSource;
import qit.math.utils.MathUtils;

import java.util.List;

@ModuleDescription("Resample a diffusion-weighted MR volume to have a different set of gradients (single shell only)")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDwiResampleSpherical implements Module
{
    @ModuleInput
    @ModuleDescription("the input diffusion-weighted MR volume")
    public Volume input;

    @ModuleInput
    @ModuleDescription("the (original) source gradients")
    public Gradients source;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;

    @ModuleInput
    @ModuleDescription("the destination gradient directions")
    public Vects dest;

    @ModuleParameter
    @ModuleDescription("specify a filtering method (linear, local, or global)")
    public String filter = "linear";

    @ModuleParameter
    @ModuleDescription("the lambda parameter")
    public Double kappa = 5.0;

    @ModuleParameter
    @ModuleDescription("the lambda parameter")
    public Double lambda = 0.1;

    @ModuleParameter
    @ModuleDescription("the sigma parameter")
    public Double sigma = 20.0;

    @ModuleParameter
    @ModuleDescription("use adaptive resampling")
    public boolean adaptive = false;

    @ModuleOutput
    @ModuleDescription("the output resampled diffusion-weighted MR volume")
    public Volume output;

    public VolumeDwiResampleSpherical run()
    {
        Matrix filter = null;
        if ("linear".equals(this.filter))
        {
            filter = this.sphericalLinearFilter();
        }
        else if ("local".equals(this.filter))
        {
            filter = this.sphericalLocalSplineFilter();
        }
        else if ("global".equals(this.filter))
        {
            filter = this.sphericalGlobalSplineFilter();
        }
        else
        {
            throw new RuntimeException("invalid method: " + this.filter);
        }

        Logging.progress("resampling dwi");
        int nsource = this.source.size();
        int ndest = this.dest.size();
        double std2 = this.sigma * this.sigma;
        Volume out = this.input.proto(ndest);
        Sampling sampling = out.getSampling();
        int size = sampling.size();
        int step = size / 100;
        for (int idx = 0; idx < size; idx++)
        {
            if (idx % step == 0)
            {
                Logging.progress(String.format("%d percent processed", 100 * idx / (size - 1)));
            }

            if (this.mask != null && !this.mask.getSampling().equals(this.input.getSampling()))
            {
                throw new RuntimeException("mask dimensions do not match");
            }

            Vect sv = this.input.get(idx);

            if (this.adaptive)
            {
                // compute the data adaptive triangle
                Matrix kernel = new Matrix(nsource, nsource);
                for (int i = 0; i < nsource; i++)
                {
                    for (int j = i + 1; j < nsource; j++)
                    {
                        double d = sv.get(i) - sv.get(j);
                        double k = Math.exp(-d * d / std2);
                        kernel.set(i, j, k);
                        kernel.set(j, i, k);
                    }
                }

                // multiply the filter and data-adaptive weights
                Matrix afilter = filter.times(kernel);

                // normalize the filter
                Vect sums = MatrixUtils.rowsum(afilter);
                for (int i = 0; i < afilter.rows(); i++)
                {
                    for (int j = 0; j < afilter.cols(); j++)
                    {
                        afilter.set(i, j, afilter.get(i, j) / sums.get(i));
                    }
                }

                Vect dv = afilter.times(sv);
                out.set(idx, dv);

            }
            else
            {
                Vect dv = filter.times(sv);
                out.set(idx, dv);
            }
        }

        Logging.progress("finished dwi resampling");
        this.output = out;

        return this;
    }

    public Volume getOutput()
    {
        if (this.output == null)
        {
            this.run();
        }

        return this.output;
    }

    public Matrix sphericalGlobalSplineFilter()
    {
        Logging.progress("lambda = " + this.kappa);

        if (this.kappa <= 0)
        {
            throw new RuntimeException("lambda must be positive");
        }

        this.dest = VectsUtils.apply(this.dest, VectFunctionSource.normalize());

        int nsource = this.source.size();
        boolean[] zsource = new boolean[nsource];
        for (int i = 0; i < nsource; i++)
        {
            zsource[i] = MathUtils.zero(this.source.getBval(i)) || MathUtils.zero(this.source.getBvec(i).norm());
        }

        int ndest = this.dest.size();
        boolean[] zdest = new boolean[ndest];
        for (int i = 0; i < this.dest.size(); i++)
        {
            zdest[i] = MathUtils.zero(this.dest.get(i).norm());
        }

        List<Integer> dsource = Lists.newArrayList();
        for (int i = 0; i < nsource; i++)
        {
            if (!zsource[i])
            {
                dsource.add(i);
            }
        }

        List<Integer> ddest = Lists.newArrayList();
        for (int i = 0; i < ndest; i++)
        {
            if (!zdest[i])
            {
                ddest.add(i);
            }
        }

        int ndsource = dsource.size();
        int nddest = ddest.size();
        int nzsource = nsource - ndsource;

        Matrix B = new Matrix(ndsource, nddest);
        for (int i = 0; i < ndsource; i++)
        {
            for (int j = 0; j < nddest; j++)
            {
                Vect di = this.source.getBvec(dsource.get(i));
                Vect dj = this.dest.get(ddest.get(j));
                double dot = di.dot(dj);
                double phi = dot * dot;
                double basis = Math.exp(-this.kappa * phi);
                B.set(i, j, basis);
            }
        }

        Matrix A = new Matrix(nddest, nddest);
        for (int i = 0; i < nddest; i++)
        {
            for (int j = 0; j < ndsource; j++)
            {
                Vect di = this.dest.get(ddest.get(i));
                Vect dj = this.dest.get(ddest.get(j));
                double dot = di.dot(dj);
                double phi = dot * dot;
                double basis = Math.exp(-this.kappa * phi);
                A.set(i, j, basis);
            }
        }

        Matrix LtL = MatrixSource.identity(ndsource);

        Matrix Bt = B.transpose();
        Matrix BtB = Bt.times(B);
        Matrix BtBpL = BtB.plus(this.lambda, LtL);
        Matrix BtBpLinv = BtBpL.inv();
        Matrix dfilter = A.times(BtBpLinv).times(Bt);

        Matrix filter = new Matrix(ndest, nsource);
        for (int i = 0; i < ndest; i++)
        {
            for (int j = 0; j < nsource; j++)
            {
                if (zsource[j] && zdest[i])
                {
                    filter.set(i, j, 1.0 / nzsource);
                }
            }
        }

        for (int i = 0; i < nddest; i++)
        {
            for (int j = 0; j < ndsource; j++)
            {
                filter.set(ddest.get(i), dsource.get(j), dfilter.get(i, j));
            }
        }

        return filter;
    }

    public Matrix sphericalLocalSplineFilter()
    {
        Logging.progress("lambda = " + this.kappa);

        if (this.kappa <= 0)
        {
            throw new RuntimeException("lambda must be positive");
        }

        int nsource = this.source.size();
        boolean[] zsource = new boolean[nsource];
        for (int i = 0; i < nsource; i++)
        {
            zsource[i] = MathUtils.zero(this.source.getBval(i)) || MathUtils.zero(this.source.getBvec(i).norm());
        }

        int ndest = this.dest.size();
        boolean[] zdest = new boolean[ndest];
        for (int i = 0; i < this.dest.size(); i++)
        {
            zdest[i] = MathUtils.zero(this.dest.get(i).norm());
        }

        Logging.progress("precaching interpolation weights");
        double[][] weights = new double[ndest][nsource];
        for (int i = 0; i < ndest; i++)
        {
            double sumw = 0;
            for (int j = 0; j < nsource; j++)
            {
                if (!zsource[j] && !zdest[i])
                {
                    double dot = this.source.getBvec(j).dot(this.dest.get(i));
                    double w = Math.exp(this.kappa * dot * dot);
                    weights[i][j] = w;
                    sumw += w;
                }
                else if (zsource[j] && zdest[i])
                {
                    double w = 1.0;
                    weights[i][j] = w;
                    sumw += w;
                }
            }

            if (MathUtils.zero(sumw))
            {
                throw new RuntimeException("lambda is too large");
            }

            for (int j = 0; j < nsource; j++)
            {
                weights[i][j] /= sumw;
            }
        }

        Matrix filter = new Matrix(weights);

        return filter;
    }

    public Matrix sphericalLinearFilter()
    {
        Logging.progress("computing linear reconstruction filter");

        int nsource = this.source.size();
        boolean[] zsource = new boolean[nsource];
        for (int i = 0; i < nsource; i++)
        {
            zsource[i] = MathUtils.zero(this.source.getBval(i)) || MathUtils.zero(this.source.getBvec(i).norm());
        }

        int ndest = this.dest.size();
        boolean[] zdest = new boolean[ndest];
        for (int i = 0; i < this.dest.size(); i++)
        {
            zdest[i] = MathUtils.zero(this.dest.get(i).norm());
        }

        Mesh mesh = ModelUtils.triangulate(this.source);
        double[][] weights = new double[ndest][nsource];
        for (int i = 0; i < ndest; i++)
        {
            if (zdest[i])
            {
                for (int j = 0; j < nsource; j++)
                {
                    if (zsource[j])
                    {
                        weights[i][j] = 1.0;
                    }
                }
            }
            else
            {
                Vect vect = this.dest.get(i);
                Face face = MeshUtils.closestFace(mesh, Mesh.COORD, vect);
                int uidx = face.getA().id() % nsource;
                int vidx = face.getB().id() % nsource;
                int widx = face.getC().id() % nsource;
                Vect uvect = mesh.vattr.get(face.getA(), Mesh.COORD);
                Vect vvect = mesh.vattr.get(face.getB(), Mesh.COORD);
                Vect wvect = mesh.vattr.get(face.getC(), Mesh.COORD);
                Triangle tri = new Triangle(uvect, vvect, wvect);
                Bary bary = tri.closest(vect);
                double uweight = bary.getU();
                double vweight = bary.getV();
                double wweight = bary.getW();

                weights[i][uidx] = uweight;
                weights[i][vidx] = vweight;
                weights[i][widx] = wweight;
            }

            double sumw = MathUtils.sum(weights[i]);
            if (MathUtils.zero(sumw))
            {
                throw new RuntimeException("invalid filter");
            }

            for (int j = 0; j < nsource; j++)
            {
                weights[i][j] /= sumw;
            }
        }
        Matrix filter = new Matrix(weights);

        return filter;
    }
}
