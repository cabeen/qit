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
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Deformation;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.utils.MaskUtils;
import qit.data.utils.VolumeUtils;
import qit.data.utils.curves.CurvesSelector;
import qit.math.structs.Containable;
import qit.math.source.SelectorSource;

import java.util.List;

@ModuleDescription("Select curves using volumetric masks")
@ModuleAuthor("Ryan Cabeen")
public class CurvesMaskSelect implements Module
{
    @ModuleInput
    @ModuleDescription("input curves")
    public Curves input;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a deformation between curves and the masks")
    public Deformation deform = null;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("use an include mask (AND for multiple labels)")
    public Mask include;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("use an exclude mask (AND for multiple labels)")
    public Mask exclude;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("use a containment mask")
    public Mask contain;

    @ModuleParameter
    @ModuleDescription("binarize the include mask")
    public boolean binarize = false;

    @ModuleParameter
    @ModuleDescription("invert the exclude mask")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("skip masks without regions or an insufficient number of regions without error")
    public boolean skip = false;

    @ModuleParameter
    @ModuleDescription("specify a containment threshold")
    public Double thresh = 0.8;

    @ModuleParameter
    @ModuleDescription("select based on only curve endpoints")
    public boolean endpoints = false;

    @ModuleParameter
    @ModuleDescription("select curves with endpoints that connect different labels")
    public boolean connect = false;

    @ModuleOutput
    @ModuleDescription("the output curves")
    public Curves output;

    public CurvesMaskSelect run()
    {
        Curves curves = this.input;

        if (this.include != null)
        {
            Logging.info("using an inclusion mask");
            Mask mask = this.include;

            if (this.binarize)
            {
                mask = MaskUtils.binarize(mask);
            }

            List<Integer> idx = MaskUtils.listNonzero(mask);

            if (idx.size() == 0)
            {
                if (this.skip)
                {
                    Logging.info("skipping inclusion selection");
                }
                else
                {
                    Logging.error("expected regions for inclusion");
                }
            }

            for (int i : idx)
            {
                Mask submask = MaskUtils.equal(mask, i);
                Containable selector = SelectorSource.mask(submask);

                CurvesSelector select = new CurvesSelector();
                select.withCurves(curves);
                select.withSelector(selector);
                select.withEndpoints(this.endpoints || this.connect);

                if (this.deform != null)
                {
                    select.withTransform(this.deform);
                }

                curves = select.getOutput();
            }

            if (this.connect)
            {
                if (idx.size() > 2 || idx.size() == 0)
                {
                    if (this.skip)
                    {
                        Logging.info("skipping connection selection");
                    }
                    else
                    {
                        Logging.error("expected a pair of regions for connection");
                    }
                }
                else
                {
                    Sampling sampling = this.include.getSampling();
                    Curves connected = new Curves();

                    for (Curve curve : curves)
                    {
                        Vect head = curve.getHead();
                        Vect tail = curve.getTail();

                        if (this.deform != null)
                        {
                            head = this.deform.apply(head);
                            tail = this.deform.apply(tail);
                        }

                        if (!sampling.contains(head) || !sampling.contains(tail))
                        {
                            continue;
                        }

                        int headLabel = this.include.get(sampling.nearest(head));
                        int tailLabel = this.include.get(sampling.nearest(tail));

                        if (idx.size() == 2 && (headLabel == 0 || tailLabel == 0))
                        {
                            continue;
                        }

                        if (headLabel == tailLabel)
                        {
                            continue;
                        }

                        if (headLabel > tailLabel)
                        {
                            curve.reverse();
                        }

                        connected.add(curve);
                    }

                    curves = connected;
                }
            }
        }

        if (this.exclude != null)
        {
            Logging.info("using an exclusion mask");
            Mask mask = this.exclude;

            if (this.binarize)
            {
                mask = MaskUtils.binarize(mask);
            }

            if (this.invert)
            {
                mask = MaskUtils.invert(mask);
            }

            Containable selector = SelectorSource.mask(mask);

            CurvesSelector select = new CurvesSelector();
            select.withCurves(curves);
            select.withSelector(selector);
            select.withExclude(true);
            select.withEndpoints(this.endpoints);

            if (this.deform != null)
            {
                select.withTransform(this.deform);
            }

            curves = select.getOutput();
        }

        if (this.contain != null)
        {
            Logging.info("using an containment mask");
            Mask mask = this.contain;

            if (this.binarize)
            {
                mask = MaskUtils.binarize(mask);
            }

            if (this.invert)
            {
                mask = MaskUtils.invert(mask);
            }

            Sampling sampling = mask.getSampling();

            boolean[] bs = new boolean[curves.size()];
            for (int i = 0; i < curves.size(); i++)
            {
                Curves.Curve curve = curves.get(i);
                Vects polyline = curve.getAll(Curves.COORD);
                if (this.deform != null)
                {
                    for (int idx = 0; idx < polyline.size(); idx++)
                    {
                        polyline.set(idx, this.deform.apply(polyline.get(idx)));
                    }
                }

                int count = 0;
                int total = 0;
                for (Sample sample : sampling.traverse(polyline))
                {
                    if (sampling.contains(sample) && mask.foreground(sample))
                    {
                        count += 1;
                    }

                    total += 1;
                }

                double frac = count / (double) total;

                bs[i] = frac > this.thresh;
            }

            curves = curves.copy(bs);
        }

        this.output = curves;

        return this;
    }
}
