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

package qit.data.modules.volume;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleCitation;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.math.structs.DisjointSet;
import qit.math.utils.MathUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@ModuleDescription("Process a volume with mean shift analysis for either anisotropic filtering or segmentation")
@ModuleAuthor("Ryan Cabeen")
@ModuleCitation("Comaniciu, D., & Meer, P. (2002). Mean shift: A robust approach toward feature space analysis. IEEE Transactions on pattern analysis and machine intelligence, 24(5), 603-619.")
public class VolumeFilterMeanShift implements Module
{
    @ModuleInput
    @ModuleDescription("the input volume")
    private Volume input;
    
    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a mask")
    public Mask mask;
    
    @ModuleParameter
    @ModuleDescription("the spatial bandwidth in mm")
    private double hpos = 8;

    @ModuleParameter
    @ModuleDescription("the adaptive bandwidth in units of intensity")
    private double hval = 64;

    @ModuleParameter
    @ModuleDescription("the maxima number of iterations")
    private int iters = 5000;

    @ModuleParameter
    @ModuleDescription("the threshold for stopping gradient updates")
    private double minshift = 1e-3;

    @ModuleParameter
    @ModuleDescription("the minima cluster size")
    private double minsize = 50;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output filtered image")
    private Volume filtered;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output segmentation")
    private Mask segmented;
   
    public VolumeFilterMeanShift run()
    {
        Sampling sampling = this.input.getSampling();
        double scaling = Math.sqrt(Math.log(10));
        int rspat = (int) Math.ceil(hpos * scaling);
        int rspat2 = rspat * rspat;
        double rval = hval * scaling;

        double rval2 = rval * rval;
        double hpos2 = hpos * hpos;
        double hval2 = hval * hval;

        int ispat = (int) Math.ceil(rspat / sampling.deltaI());
        int jspat = (int) Math.ceil(rspat / sampling.deltaJ());
        int kspat = (int) Math.ceil(rspat / sampling.deltaK());
        List<int[]> offsets = Lists.newArrayList();
        for (int dk = -kspat; dk <= kspat; dk++)
        {
            for (int dj = -jspat; dj <= jspat; dj++)
            {
                for (int di = -ispat; di <= ispat; di++)
                {
                    double dx = di * sampling.deltaI();
                    double dy = dj * sampling.deltaJ();
                    double dz = dk * sampling.deltaK();

                    if (dz * dz + dy * dy + dx * dx <= rspat2)
                    {
                        offsets.add(new int[] { di, dj, dk });
                    }
                }
            }
        }
        
        int size = sampling.size();
        int step = size / 100;

        Vect[] spats = new Vect[size];
        Vect[] vals = new Vect[size];
        DisjointSet<Integer> ds = new DisjointSet<>();

        Volume outvol = this.input.proto();
        Logging.info("performing mean shift segmentation");
        for (int idx = 0; idx < size; idx++)
        {
            if (idx % step == 0)
            {
                Logging.info(String.format("%d percent processed", 100 * idx / (size - 1)));
            }

            if (!this.input.valid(idx, mask))
            {
                continue;
            }

            Vect scurr = sampling.world(idx);
            Vect vcurr = this.input.get(idx);

            int iters = 0;
            double shift = Double.MAX_VALUE;
            do
            {
                Sample ncurr = sampling.nearest(scurr);
                Vect smean = VectSource.create3D();
                Vect vmean = VectSource.createND(this.input.getDim());
                double weight = 0;

                for (int[] off : offsets)
                {
                    int inei = ncurr.getI() + off[0];
                    int jnei = ncurr.getJ() + off[1];
                    int knei = ncurr.getK() + off[2];
                    if (sampling.contains(inei, jnei, knei))
                    {
                        Vect vnei = this.input.get(inei, jnei, knei);
                        double dv2 = vcurr.dist2(vnei);

                        if (dv2 <= rval2)
                        {
                            Vect snei = sampling.world(inei, jnei, knei);
                            double dr2 = snei.dist2(scurr);

                            double ks = Math.exp(-dr2 / hpos2);
                            double kv = Math.exp(-dv2 / hval2);
                            double k = ks * kv;

                            smean.plusEquals(k, snei);
                            vmean.plusEquals(k, vnei);
                            weight += k;
                        }
                    }
                }

                double norm = 1.0 / weight;
                smean.timesEquals(norm);
                vmean.timesEquals(norm);

                double dr2 = smean.dist2(scurr);
                double dv2 = vmean.dist2(vcurr);

                shift = Math.sqrt(dr2 / hpos2 + dv2 / hval2);
                iters++;

                scurr.set(smean);
                vcurr.set(0, vmean);
            }
            while (shift > this.minshift && iters < this.iters);
            
            outvol.set(idx, vcurr);

            spats[idx] = scurr;
            vals[idx] = vcurr;
            ds.add(idx);

            for (int idx2 = 0; idx2 < idx; idx2++)
            {
                if (!this.input.valid(idx, mask))
                {
                    continue;
                }
                
                double dr2 = spats[idx].dist2(spats[idx2]);
                double dv2 = vals[idx].dist2(vals[idx2]);

                if (dr2 < hpos2 && dv2 < hval2)
                {
                    ds.join(idx, idx2);
                }
            }
        }

        Map<Integer, Integer> counts = Maps.newHashMap();
        for (int idx = 0; idx < size; idx++)
        {
            if (!this.input.valid(idx, mask))
            {
                continue;
            }
            
            int key = ds.find(idx);
            if (counts.containsKey(key))
            {
                counts.put(key, counts.get(key) + 1);
            }
            else
            {
                counts.put(key, 1);
            }
        }

        Set<Integer> small = Sets.newHashSet();
        Set<Integer> big = Sets.newHashSet();
        for (Integer k : Sets.newHashSet(counts.keySet()))
        {
            if (counts.get(k) < this.minsize)
            {
                small.add(k);
            }
            else
            {
                big.add(k);
            }
        }

        for (Integer s : small)
        {
            double mind = Double.MAX_VALUE;
            int mini = -1;
            for (Integer k : big)
            {
                double dr2 = spats[s].dist2(spats[k]);
                double dv2 = vals[s].dist2(vals[k]);
                double d = Math.sqrt(dr2 / hpos2 + dv2 / hval2);
                
                if (mini == -1 || d < mind)
                {
                    mind = d;
                    mini = k;
                }
            }
            
            counts.put(mini, counts.get(mini) + counts.get(s));
            counts.remove(s);
            ds.join(mini,  s);
        }
        
        Logging.info("remapping labels");
        Map<Integer, Integer> labelmap = MathUtils.remap(counts);

        Mask outseg = new Mask(this.input.getSampling());
        for (int idx = 0; idx < size; idx++)
        {
            if (!this.input.valid(idx, mask))
            {
                continue;
            }
            
            int key = ds.find(idx);
            int label = labelmap.get(key);
            if (counts.get(key) < this.minsize)
            {
                outseg.set(idx, 0);
            }
            else
            {
                outseg.set(idx, label);
            }
        }

        this.filtered = outvol;
        this.segmented = outseg;
        
        return this;
    }
}