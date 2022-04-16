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
import qit.base.Global;
import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.modules.mask.MaskClose;
import qit.data.modules.mask.MaskComponents;
import qit.data.modules.mask.MaskDilate;
import qit.data.modules.volume.VolumeThreshold;
import qit.data.source.SamplingSource;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;
import qit.math.structs.Box;

import java.util.Map;

@ModuleUnlisted
@ModuleDescription("Compute waypoint masks and containment masks from a set of curves.  The output is meant to resemble masks drawn at each end of a bundle and a single mask in the main body")
@ModuleAuthor("Ryan Cabeen")
public class CurvesWaypoints implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the reference mask")
    public Mask ref;

    @ModuleParameter
    @ModuleDescription("the volume resolution")
    public double delta = 1;

    @ModuleParameter
    @ModuleDescription("the arclength fraction to grow")
    public Double fraction = 0.1;

    @ModuleParameter
    @ModuleDescription("the dilation amount")
    public int dilate = 1;

    @ModuleParameter
    @ModuleDescription("the number of closure iterations")
    public int close = 25;

    @ModuleOutput
    @ModuleDescription("the output mask")
    public Mask mask;

    @ModuleOutput
    @ModuleDescription("the output waypoints")
    public Mask waypoints;

    public CurvesWaypoints run()
    {
        Logging.info("... started masking curves");

        Curves curves = this.input;
        Sampling sampling = null;

        if (this.ref == null)
        {
            Box box = curves.bounds().scale(1.25);
            sampling = SamplingSource.create(box, this.delta);
        }
        else
        {
            sampling = this.ref.getSampling();
        }

        Volume density = VolumeUtils.density(sampling, curves);
        VolumeThreshold module = new VolumeThreshold();
        module.input = density;
        module.threshold = 0.5;
        Mask mask = module.run().output;
        Mask waypoints = mask.proto();

        CurvesOrient orient = new CurvesOrient();
        orient.input = curves;
        curves = orient.run().output;

        for (Curve curve : curves)
        {
            Sample head = sampling.nearest(curve.getHead());
            Sample tail = sampling.nearest(curve.getTail());

            waypoints.set(head, 1);
            waypoints.set(tail, 2);

            double clen = curve.length();
            double clow = this.fraction * clen;
            double chigh = (1 - this.fraction) * clen;
            Vect ccums = curve.cumlength();

            for (int i = 0; i < curve.size(); i++)
            {
                double ccum = ccums.get(i);
                Vect pos = curve.get(i);
                Sample samp = sampling.nearest(pos);

                if (ccum < clow)
                {
                    waypoints.set(samp, 1);
                }
                else if (ccum > chigh)
                {
                    waypoints.set(samp, 2);
                }
            }
        }

        for (int i = 0; i < this.close; i++)
        {
            MaskClose closer = new MaskClose();
            closer.input = waypoints;
            closer.num = i;
            Mask closed = closer.run().output;

            MaskComponents comper = new MaskComponents();
            comper.input = closed;
            Mask cc = comper.run().output;
            int numcomps = MaskUtils.listNonzero(cc).size();
            Global.assume(numcomps >= 2, "invalid number of components");

            if (numcomps == 2)
            {
                break;
            }
        }

        // select the two largest (assuming the labels are in decreasing size)
        Map<Integer, Integer> map = Maps.newHashMap();
        map.put(1, 1);
        map.put(2, 2);
        waypoints = MaskUtils.map(waypoints, map);

        {
            MaskDilate dilater = new MaskDilate();
            dilater.input = mask;
            dilater.num = this.dilate;
            mask = dilater.run().output;
        }

        {
            MaskDilate dilater = new MaskDilate();
            dilater.input = waypoints;
            dilater.num = this.dilate;
            waypoints = dilater.run().output;
        }

        Logging.info("... finished masking curves");

        this.mask = mask;
        this.waypoints = waypoints;

        return this;
    }

    public Mask getOutputMask()
    {
        if (this.mask == null)
        {
            this.run();
        }

        return this.mask;
    }

    public Mask getOutputWaypoints()
    {
        if (this.waypoints == null)
        {
            this.run();
        }

        return this.waypoints;
    }
}
