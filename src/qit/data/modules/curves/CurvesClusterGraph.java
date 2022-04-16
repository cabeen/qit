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

import com.google.common.collect.Maps;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAdvanced;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.DisjointSet;
import qit.math.structs.Distance;
import qit.math.source.DistanceSource;

import java.util.Map;

@ModuleDescription("Curve bundle segmentation with graph-based thresholding")
@ModuleCitation("Felzenszwalb, P. F., & Huttenlocher, D. P. (2004). Efficient graph-based image segmentation. International journal of computer vision, 59(2), 167-181.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesClusterGraph implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;
    
    @ModuleParameter
    @ModuleDescription("the name of the inter-curve distance")
    public String dist = DistanceSource.DEFAULT_CURVE;
    
    @ModuleParameter
    @ModuleDescription("the threshold for grouping")
    public double thresh = 1.0;

    @ModuleParameter
    @ModuleAdvanced
    @ModuleDescription("retain the largest group")
    public boolean largest = false;

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
    public CurvesClusterGraph run()
    {
        Logging.progress("computing curve distances");
        int num = this.input.size();

        Distance<Curve> distf = DistanceSource.curve(this.dist);
        
        Logging.progress("building graph");
        DisjointSet<Integer> ds = new DisjointSet<>();
        for (int i = 0; i < num; i++)
        {
            ds.add(i);
        }

        for (int i = 0; i < num; i++)
        {
            Logging.progress("computing distances for curve " + i);
            for (int j = i + 1; j < num; j++)
            {
                double d = distf.dist(this.input.get(i), this.input.get(j));
                if (d < this.thresh)
                {
                    ds.join(i, j);
                }
            }
        }

        Map<Integer, Integer> lookup = ds.getLookup();

        Logging.progress("labeling components");
        int[] cidx = new int[num];
        Map<Integer, Integer> count = Maps.newHashMap();
        for (int i = 0; i < num; i++)
        {
            int v = lookup.get(ds.find(i));
            if (count.containsKey(v))
            {
                count.put(v, count.get(v) + 1);
            }
            else
            {
                count.put(v, 1);
            }
            cidx[i] = v;
        }

        Logging.progress("finding largest component");
        Integer max = null;
        if (this.largest)
        {
            for (Integer i : count.keySet())
            {
                if (max == null || count.get(i) > count.get(max))
                {
                    max = i;
                }
            }
        }

        Logging.progress("adding curve attributes");

        Curves curves = this.inplace ? this.input : this.input.copy();
        if (!this.largest)
        {
            curves.add(Curves.LABEL, VectSource.createND(1));
        }

        // assume this happens in the same order as initialization...
        boolean[] pass = new boolean[curves.size()];
        for (int i = 0; i < curves.size(); i++)
        {
            Curve curve = curves.get(i);
            Vect label = VectSource.create1D(cidx[i]);

            for (int j = 0; j < curve.size(); j++)
            {
                if (!this.largest)
                {
                    curve.set(Curves.LABEL, j, label);
                }
            }

            if (this.largest)
            {
                pass[i] = cidx[i] == max;
            }
        }

        if (this.largest)
        {
            curves.keep(pass);
        }

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
