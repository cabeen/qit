/*******************************************************************************
 * Topographic Tract Filtering Software License
 *
 * The following terms apply to all files associated with the software unless explicitly disclaimed in individual files.
 *
 * 1. Permission is granted to use this software without charge for non-commercial research purposes only. You may make verbatim copies of this software for personal use, or for use within your organization, provided that you include copies of all of the original copyright notices and associated disclaimers with each copy of the software.
 *
 * 2. YOU MAY NOT DISTRIBUTE COPIES of this software, or copies of software derived from this software, to others outside your organization.
 *
 * 3. This software is for research purposes only and has not been approved for clinical use.
 *
 *  4. If you publish results whose generation used this software, you must provide attribution to the authors of the software by referencing the appropriate papers.
 *
 *  5. IN NO EVENT SHALL THE UNIVERSITY OF SOUTHERN CALIFORNIA, THE AUTHORS, OR THE DISTRIBUTORS BE LIABLE TO ANY PARTY FOR DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE, ITS DOCUMENTATION, OR ANY DERIVATIVES THEREOF, EVEN IF THE AUTHORS HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * 6. THE UNIVERSITY OF SOUTHERN CALIFORNIA, THE AUTHORS, AND THE DISTRIBUTORS SPECIFICALLY DISCLAIM ANY WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT. THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, AND THE AUTHORS AND DISTRIBUTORS HAVE NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 ******************************************************************************/

package qit.data.modules.curves;

import com.google.common.collect.Lists;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectsUtils;
import qit.math.source.DistanceSource;
import qit.math.structs.Distance;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.function.Function;

@ModuleDescription("Filter curves to enhance their topographic regularity using the group graph spectral distance")
@ModuleCitation("Wang, J., Aydogan, D. B., Varma, R., Toga, A. W., & Shi, Y. (2018). Modeling topographic regularity in structural brain connectivity with application to tractogram filtering. NeuroImage.")
@ModuleAuthor("Junyan Wang, Ryan Cabeen")
public class CurvesFilterTTF implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("size of neighborhood")
    public Integer K = 20;

    @ModuleParameter
    @ModuleDescription("parameter for the exponential in the proximity measure")
    public Double sigma = 0.01;

    @ModuleParameter
    @ModuleDescription("the resolution for downsampling curves")
    public double density = 5;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    @Override
    public CurvesFilterTTF run()
    {
        final Curves curves = this.input.copy();

        int numCurves = curves.size();
        double sig2 = this.sigma * this.sigma;

        Logging.progress(String.format("filtering %d input curves", numCurves));

        Logging.progress("downsampling");
        CurvesResample resampler = new CurvesResample();
        resampler.density = this.density;
        resampler.input = curves;
        Curves sparse = resampler.run().output;

        Logging.progress("computing pair-wise track distances");
        Matrix distanceTracks = CurvesUtils.distance(sparse, DistanceSource.curveHausdorff());

        for (int i = 0; i < numCurves; i++)
        {
            Logging.progress("... processing curve " + i);

            Curves.Curve curve = sparse.get(i);
            int numPoints = curve.size();

            Integers closestTracks = distanceTracks.getRow(i).sort().sub(0, this.K);

            List<Matrix> EE = Lists.newArrayList();
            for (int j = 0; j < curve.size(); j++)
            {
                Vect point = curve.get(j);

                Vects pointNeighborhood = new Vects();
                for (int k = 0; k < this.K; k++)
                {
                    Curves.Curve neighborTrack = curves.get(closestTracks.get(k));
                    Pair<Double, Integer> nearestPoint = neighborTrack.nearestVertex(point);
                    int nearestIndex = nearestPoint.b;
                    pointNeighborhood.add(neighborTrack.get(nearestIndex));
                }

                EE.add(VectsUtils.normdist(pointNeighborhood, sig2));
            }

            Matrix ES = EE.get(0).copy();
            for (int j = 1; j < numPoints; j++)
            {
               ES = ES.catCols(EE.get(j));
            }

            MatrixUtils.SvdDecomp svd = MatrixUtils.svd(ES);
            Matrix V = svd.V;
            Matrix Vt = V.transpose();

            Matrix Es = new Matrix(numPoints, this.K);
            for (int j = 0; j < numPoints; j++)
            {
                Es.setRow(j, Vt.times(EE.get(j)).times(V).diag());
            }

            double ttf = Es.stdRow().mean();

            curves.get(i).setAll("TTF", VectSource.create1D(ttf));
        }

        // boolean[] bkeep = new boolean[this.input.size()];
        // this.output = this.input.copy(bkeep);

        this.output = curves;

        return this;
    }
}
