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

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.vects.cluster.VectsClusterDPM;
import qit.data.utils.vects.cluster.VectsClusterKM;
import qit.data.utils.vects.stats.VectsGaussianFitter;
import qit.math.structs.Gaussian;
import qit.math.utils.MathUtils;

@ModuleDescription("Cluster curves using simple curve features (length, endpoints, position, shape) using K-means")
@ModuleAuthor("Ryan Cabeen")
public class CurvesClusterFeature implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("include the length")
    public boolean length = false;

    @ModuleParameter
    @ModuleDescription("include tensor product of endpoints")
    public boolean endpoints = false;

    @ModuleParameter
    @ModuleDescription("include the Gaussian shape")
    public boolean gaussian = false;

    @ModuleParameter
    @ModuleDescription("the number of clusters")
    public Integer num = 2;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the threshold size for clusters (enables DP-means)")
    public Double thresh = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the maxima number of iterations")
    public Integer iters = 100;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("the number of restarts")
    public Integer restarts = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("relabel to reflect cluster size")
    public boolean relabel = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("keep only the largest cluster")
    public boolean largest = false;

    public boolean verbose = true;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output curves")
    public Curves output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the prototypical curves for each cluster")
    public Curves protos = null;

    @Override
    public CurvesClusterFeature run()
    {
        if (this.input.size() == 0)
        {
            msg("no curves found");
            this.output = new Curves();
            this.protos = new Curves();

            return this;
        }

        VectsClusterKM cluster;
        if (this.thresh != null)
        {
            cluster = new VectsClusterDPM().withLambda(this.thresh * this.thresh);
        }
        else
        {
            cluster = new VectsClusterKM();
        }

        cluster.withMaxIter(this.iters);

        int k = Math.min(this.num, this.input.size());
        cluster.withK(k);

        if (this.restarts != null)
        {
            cluster.withRestarts(this.restarts);
        }

        Curves curves = this.inplace ? this.input : this.input.copy();

        msg("... extracting features");
        Vects features = new Vects();

        int dim = 0;
        dim += this.length ? 1 : 0;
        dim += this.endpoints ? 9: 0;
        dim += this.gaussian ? 12 : 0;

        Vect gamma = VectSource.create3D(0, 0.5, 1.0);

        for (Curves.Curve curve : input)
        {
            Vect vect = VectSource.createND(dim);
            int idx = 0;

            if (this.length)
            {
                vect.set(idx, curve.length());
                idx += 1;
            }

            if (this.endpoints)
            {
                Vect head = curve.getHead();
                Vect tail = curve.getTail();
                Vect outer = MatrixSource.outer(head, tail).packRow().abs().sqrt();

                vect.set(idx, outer);
                idx += 9;
            }

            if (this.gaussian)
            {
                VectsGaussianFitter fitter = new VectsGaussianFitter();
                fitter.withInput(curve.getAll());
                Gaussian gauss = fitter.run().getOutput();

                vect.set(idx, gauss.getMean());
                vect.set(idx + 3, gauss.getCov().packRow());
            }

            features.add(vect);
        }

        msg("... clustering");
        cluster.withVects(features);
        int[] labels = cluster.getOutput();
        CurvesUtils.attrSetLabelsPerCurve(curves, Curves.LABEL, labels);

        Vects centers = cluster.getCenters();
        msg(String.format("... found %d clusters", centers.size()));

        msg("... finding prototypes");
        Curves pcurves = new Curves();
        for (int i = 0; i < centers.size(); i++)
        {
            Vect mcenter = centers.get(i);
            double[] dists = new double[features.size()];
            for (int j = 0; j < features.size(); j++)
            {
                dists[j] = features.get(j).dist(mcenter);
            }
            int minidx = MathUtils.minidx(dists);

            pcurves.add(curves.get(minidx));
        }

        if (this.relabel || this.largest)
        {
            CurvesRelabel relabel = new CurvesRelabel();
            relabel.input = curves;
            relabel.inplace = true;
            relabel.run();
        }

        this.protos = pcurves;
        this.output = curves;

        return this;
    }

    public void msg(String msg)
    {
        if (this.verbose)
        {
            Logging.info(msg);
        }
    }
}
