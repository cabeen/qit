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
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.structs.Integers;
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.vects.VectsHistogram;
import qit.data.modules.volume.VolumeEnhanceContrast;
import qit.data.source.VectSource;
import qit.data.source.VectsSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.MatrixUtils;
import qit.data.utils.VectUtils;
import qit.data.utils.VectsUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.source.DistanceSource;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.function.Consumer;

@ModuleDescription("Filter curves based on their agreement with a tract orientation map")
@ModuleAuthor("Ryan Cabeen")
public class CurvesFilterTOM implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("input reference volume")
    public Volume reference;

    @ModuleParameter
    @ModuleDescription("the number of iterations")
    public int iters = 1;

    @ModuleParameter
    @ModuleDescription("a gain factor for computing likelihoods")
    public double beta = 1.0;

    @ModuleParameter
    @ModuleDescription("a threshold for outlier rejection")
    public double thresh = 0.05;

    @ModuleParameter
    @ModuleDescription("orient the bundle prior to mapping")
    public boolean orient = false;

    @ModuleParameter
    @ModuleDescription("compute a vector orientation (you may want to enable the orient flag)")
    public boolean vector = false;

    @ModuleParameter
    @ModuleDescription("specify a method for normalizing orientation magnitudes")
    public VolumeEnhanceContrast.VolumeEnhanceContrastType norm = VolumeEnhanceContrast.VolumeEnhanceContrastType.Histogram;

    @ModuleParameter
    @ModuleDescription("apply fiber smoothing after mapping")
    public boolean smooth = false;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleOptional
    @ModuleDescription("apply smoothing with the given amount (bandwidth in mm) (default is largest voxel dimension)")
    public Double sigma = null;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("the smoothing filter radius in voxels")
    public int support = 3;

    @ModuleOutput
    @ModuleDescription("the output inlier curves")
    public Curves output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("input reference volume")
    public Volume tom;

    @Override
    public CurvesFilterTOM run()
    {
        Curves myCurves = this.input.copy();
        Sampling mySampling = this.reference.getSampling();
        Vect myWeights = VectSource.createND(myCurves.size(), 1.0);
        Volume myTom = null;

        Consumer<Vect> printit = (vect) ->
        {
            VectsHistogram histogram = new VectsHistogram();
            histogram.input = VectsSource.create1D(vect);
            histogram.lower = VectsHistogram.Bound.ThreeStd;
            histogram.upper = VectsHistogram.Bound.ThreeStd;
            histogram.print = true;
            histogram.run();
        };

        for (int iter = 0; iter < this.iters; iter++)
        {
            // Vect lengths = myCurves.lengths();
            // Vect lengthw = lengths.minus(lengths.mean()).divSafe(lengths.std()).abs().negexp();
            // myWeights.timesEquals(lengthw);

            CurvesOrientationMap tomer = new CurvesOrientationMap();
            tomer.input = myCurves;
            tomer.refvolume = this.reference;
            tomer.orient = this.orient;
            tomer.vector = this.vector;
            tomer.norm = this.norm;
            tomer.smooth = this.smooth;
            tomer.sigma = this.sigma;
            tomer.support = this.support;
            myTom = tomer.apply(myWeights);

            myCurves.add(Curves.CLIKE, VectSource.create1D());
            Vect dists = new Vect(myCurves.size());
            for (int i = 0; i < myCurves.size(); i++)
            {
                Curves.Curve curve = myCurves.get(i);
                VectOnlineStats stats = new VectOnlineStats();

                for (Pair<Sample, Vect> pair : myTom.getSampling().traverseLine(curve.getAll(Curves.COORD)))
                {
                    Sample sample = pair.a;
                    Vect dir = pair.b.normalize();

                    if (mySampling.contains(sample))
                    {
                        Vect mean = myTom.get(sample).normalize();
                        double dist = Math.abs(mean.dot(dir));

                        if (Double.isFinite(dist))
                        {
                            stats.update(dist);
                        }
                    }
                }

                curve.setAll(Curves.CLIKE, VectSource.create1D(stats.mean));
                dists.set(i, stats.mean);
            }

            Vect likes = dists.sq().times(-1.0 * this.beta).exp();
            Vect rankw = VectUtils.rankweight(dists);

            myWeights = VectSource.createND(myCurves.size(), 1.0);
            myWeights.timesEquals(rankw);

            printit.accept(likes);
        }

        this.output = myCurves;
        this.tom = myTom;

        return this;
    }
}
