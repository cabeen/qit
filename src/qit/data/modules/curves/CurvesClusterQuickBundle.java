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

import com.google.common.collect.Lists;
import qit.base.Global;
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
import qit.base.structs.Pair;
import qit.data.datasets.Curves;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.CurvesUtils;
import qit.data.utils.VectsUtils;
import qit.math.utils.MathUtils;

import java.util.Collections;
import java.util.List;

@ModuleDescription("Cluster curves with the quicksbundles algorithm")
@ModuleCitation("Garyfallidis, E., Brett, M., Correia, M. M., Williams, G. B., & Nimmo-Smith, I. (2012). Quickbundles, a method for tractography simplification. Frontiers in neuroscience, 6, 175.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesClusterQuickBundle implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("take a subset of curves before clustering (not applicable if you provide fewer)")
    public Integer subset = null;

    @ModuleParameter
    @ModuleDescription("the number of sample vertices")
    public Integer samples = 5;

    @ModuleParameter
    @ModuleDescription("the separation threshold")
    public Double thresh = 1000.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("relabel to reflect cluster size")
    public boolean relabel = false;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output curves")
    public Curves output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output centers (the centroid curves for each cluster)")
    public Curves centers;

    @Override
    public CurvesClusterQuickBundle run()
    {
        Logging.info("... starting curve clustering");

        if (this.input.size() == 0)
        {
            this.output = new Curves();
            this.centers = new Curves();
            return this;
        }

        // theta depends on the number of samples, so 
        // let's assume the input threshold is the average
        // interpoint distances we'd like to allow
        // this was *not* from the quickbundles paper, but
        // rather something I've added
        
        double theta = this.thresh * this.thresh * this.samples;
        
        Logging.info("... preprocessing");

        Curves curves = null;

        if (this.subset != null)
        {
            CurvesReduce reducer = new CurvesReduce();
            reducer.input = this.input;
            reducer.count = this.subset;
            curves = reducer.run().output;
        }
        else
        {
            curves = this.input.copy();
        }

        CurvesResample resampler = new CurvesResample();
        resampler.input = curves;
        resampler.num = this.samples;
        Curves sampled = resampler.run().output;

        Vects T = new Vects();
        Vects Tf = new Vects();

        for (int i = 0; i < curves.size(); i++)
        {
            Vects vects = sampled.get(i).getAll();
            Vects flipped = vects.copy();
            Collections.reverse(flipped);

            T.add(VectsUtils.pack(vects));
            Tf.add(VectsUtils.pack(flipped));
        }

        Logging.info("... started");
        int[] clabels = new int[curves.size()];
        List<Pair<Vect, Integer>> C = Lists.newArrayList();
        C.add(Pair.of(T.get(0), 1));
        for (int i = 2; i < curves.size(); i++)
        {
            int M = C.size();
            Vect t = T.get(i);
            Vect tf = Tf.get(i);
            double[] alld = new double[M];
            boolean[] flip = new boolean[M];

            for (int k = 0; k < M; k++)
            {
                Pair<Vect, Integer> ck = C.get(k);
                Vect v = ck.a.times(1.0 / ck.b);
                double d = t.dist2(v);
                double f = tf.dist2(v);
                if (f < d)
                {
                    d = f;
                    flip[k] = true;
                }
                alld[k] = d;
            }

            int l = MathUtils.minidx(alld);
            double m = alld[l];

            if (m < theta)
            {
                Pair<Vect, Integer> cl = C.get(l);
                if (flip[l])
                {
                    cl.a.plusEquals(tf);
                }
                else
                {
                    cl.a.plusEquals(t);
                }
                cl.b += 1;
                clabels[i] = l + 1;
            }
            else
            {
                C.add(Pair.of(t, 1));
                clabels[i] = C.size();
            }
        }

        Logging.info("... post-processing");
        CurvesUtils.attrSetLabelsPerCurve(curves, Curves.LABEL, clabels);

        if (this.relabel)
        {
            CurvesRelabel relabel = new CurvesRelabel();
            relabel.input = curves;
            relabel.inplace = true;
            relabel.run();
        }

        Curves centers = new Curves();
        for (Pair<Vect,Integer> c : C)
        {
            Vect v = c.a.times(1.0 / c.b);
            double[] dists = new double[curves.size()];

            for (int j = 0; j < dists.length; j++)
            {
                Vect t = T.get(j);
                Vect tf = Tf.get(j);
                double d = t.dist2(v);
                double f = tf.dist2(v);
                if (f < d)
                {
                    d = f;
                }
                dists[j] = d;
            }
            int minidx = MathUtils.minidx(dists);

            centers.add(this.input.get(minidx));
        }

        this.centers = centers;
        this.output = curves;

        return this;
    }

    public static Curves applyCluster(Curves curves, double threshold)
    {
        return new CurvesClusterQuickBundle()
        {{
            this.input = curves;
            this.thresh = threshold;
        }}.run().output;
    }

    public static Curves applySimplify(Curves curves, double threshold)
    {
        return new CurvesClusterQuickBundle()
        {{
            this.input = curves;
            this.thresh = threshold;
        }}.run().centers;
    }
}