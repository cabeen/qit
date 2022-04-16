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
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.math.structs.Box;
import qit.math.structs.Distance;
import qit.math.source.DistanceSource;

import java.util.Set;

@ModuleDescription("Cull redundant curves.  This can be done using SCPT (scpt) or pairwise distance-based clustering (haus, cham, end, or cutoff).  DistanceExtrinsic based clustering is much slower but technically more accurate.")
@ModuleCitation("Zhang, S., Correia, S., & Laidlaw, D. H. (2008). Identifying white-matter fiber bundles in DTI data using an automated proximity-based fiber-clustering method. IEEE transactions on visualization and computer graphics, 14(5), 1044-1053.")
@ModuleAuthor("Ryan Cabeen")
public class CurvesCull implements Module
{
    @ModuleInput
    @ModuleDescription("the input curves")
    public Curves input;

    @ModuleParameter
    @ModuleDescription("the name of the inter-curve distance (scpt, haus, cham, end, or cutoff).  Pairwise distances can be symmeterized by adding mean, min, or max to the name (except scpt).")
    public String dist = "scpt";

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the threshold for removal in mm (but the exact meaning of this depends on the distance metric, so be careful)")
    public Double thresh = 1.5;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    @Override
    public CurvesCull run()
    {
        int nc = this.input.size();

        Logging.progress(String.format("culling %d input", nc));

        if (this.dist.equals("scpt"))
        {
            Logging.info("using SCPT culling");

            CurvesClusterSCPT culler = new CurvesClusterSCPT();
            culler.input = this.input;
            culler.thresh = this.thresh;
            this.output = culler.run().protos;
        }
        else
        {
            Logging.info("using distance-based culling");

            Logging.progress("computing bounding boxes");
            Box[] boxes = new Box[this.input.size()];
            for (int i = 0; i < this.input.size(); i++)
            {
                boxes[i] = this.input.get(i).bounds();
            }

            Logging.progress("computing curve comparisons");
            // initially keep the first curve
            int count = 0;

            Distance<Curve> curveDist = DistanceSource.curve(this.dist);
            Distance<Box> boxDist = DistanceSource.boxMinTaxi();
            Set<Integer> keep = Sets.newHashSet();
            for (int i = 0; i < nc; i++)
            {
                Curve a = this.input.get(i);
                Box ba = boxes[i];

                boolean found = false;
                for (Integer j : keep)
                {
                    count += 1;
                    Curve b = this.input.get(j);
                    Box bb = boxes[j];

                    // don't even compare curves that are far apart
                    if (boxDist.dist(ba, bb) < this.thresh)
                    {
                        continue;
                    }

                    // check if this curve is too close
                    if (curveDist.dist(a, b) < this.thresh)
                    {
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    keep.add(i);
                }
            }

            Logging.progress(String.format("computed %d curve comparisons", count));
            Logging.progress(String.format("kept %d curves", keep.size()));

            boolean[] bkeep = new boolean[this.input.size()];

            for (Integer idx : keep)
            {
                bkeep[idx] = true;
            }

            this.output = this.input.copy(bkeep);
        }

        return this;
    }
}
