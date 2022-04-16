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
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Matrix;
import qit.data.source.MatrixSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.MatrixUtils;
import qit.math.structs.Distance;
import qit.math.source.DistanceSource;
import smile.clustering.SpectralClustering;

@ModuleDescription("Cluster curves with spectral clustering.")
@ModuleCitation("Cluster curves with spectral clustering.  O'Donnell, L. J., & Westin, C. F. (2007). Automatic tractography segmentation using a high-dimensional white matter atlas. IEEE transactions on medical imaging, 26(11), 1562-1575.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesClusterSpectral implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("the name of the inter-curve distance")
    public String dist = DistanceSource.DEFAULT_CURVE;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the number of clusters")
    public Integer num = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("resample the curves to speed up computation")
    public Double density = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("simplify the curves to speed up computation")
    public Double epsilon = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("relabel to reflect cluster size")
    public boolean relabel = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    @Override
    public CurvesClusterSpectral run()
    {
        Logging.info("... starting curves spectral clustering");

        Curves source = this.input;

        if (this.density != null)
        {
            Logging.info("resampling curves");
            CurvesResample resampler = new CurvesResample();
            resampler.input = source;
            resampler.density = this.density;
            source = resampler.run().output;
        }
        
        if (this.epsilon != null)
        {
            Logging.info("simplifying curves");
            CurvesSimplify simplify = new CurvesSimplify();
            simplify.epsilon = this.epsilon;
            simplify.input = source;
            source = simplify.run().output;
        }

        Logging.info("... computing distance matrix");
        Distance<Curve> distf = DistanceSource.curve(this.dist);
        Matrix distMat = MatrixSource.distAllPairs(distf, source);

        Logging.info("... computing similarity matrix");
        Matrix simMat = MatrixUtils.distToSim(distMat);

        // similarity matrix must be symmetric with zero diagonal
        simMat.setAllDiag(0);
        simMat = MatrixUtils.symmeterizeMean(simMat);

        Logging.info("... started spectral clustering with k = " + this.num);
        int[] labels = new SpectralClustering(simMat.toArray(), this.num).getClusterLabel();

        // increment by one to be consistent with rest of code
        for (int i = 0; i < labels.length; i++)
        {
            labels[i] += 1;
        }

        Curves curves = this.inplace ? this.input : this.input.copy();
        CurvesUtils.attrSetLabelsPerCurve(curves, Curves.LABEL, labels);

        if (this.relabel)
        {
            CurvesRelabel relabel = new CurvesRelabel();
            relabel.input = curves;
            relabel.inplace = true;
            relabel.run();
        }

        this.output = curves;

        return this;
    }
}
