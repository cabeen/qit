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

package qit.data.modules.mri.fibers;

import qit.base.Global;
import qit.base.ModelType;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.base.annot.ModuleUnlisted;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.models.Fibers;
import qit.data.source.MatrixSource;
import qit.data.source.SamplingSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;

@ModuleDescription("Create a benchmark fibers phantom")
@ModuleAuthor("Ryan Cabeen")
public class VolumeFibersPhantomCrossing implements Module
{
    @ModuleParameter
    @ModuleDescription("the size of the phantom")
    public int size = 50;

    @ModuleParameter
    @ModuleDescription("volume fraction of the primary bundle")
    public double fracPrimary = 0.4;

    @ModuleParameter
    @ModuleDescription("volume fraction of the secondary bundle")
    public double fracSecondary = 0.4;

    @ModuleParameter
    @ModuleDescription("the relative radius of the primary bundle (0 to 1)")
    public double radiusPrimary = 0.4;

    @ModuleParameter
    @ModuleDescription("the relative radius of the secondary bundle (0 to 1)")
    public double radiusSecondary = 0.2;

    @ModuleParameter
    @ModuleDescription("the angle between the primary and secondary bundles")
    public double angle = 45;

    @ModuleParameter
    @ModuleDescription("the amount of noise added to the compartment fiber orientations")
    public double noiseAngle = 5;

    @ModuleParameter
    @ModuleDescription("the amount of noise added to the compartment volume fraction")
    public double noiseFraction= 0.05;

    @ModuleOutput
    @ModuleDescription("the synthesized output fibers")
    public Volume output;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output bundle mask")
    public Mask outputBundle;

    @ModuleOutput
    @ModuleOptional
    @ModuleDescription("the output end mask")
    public Mask outputEnd;

    public VolumeFibersPhantomCrossing run()
    {
        Fibers proto = new Fibers(2);
        proto.setBaseline(0.0);
        proto.setDiffusivity(1.3e-4);

        double cos = Math.cos(this.angle * Math.PI / 180.0);
        double sin = Math.sin(this.angle * Math.PI / 180.0);
        Vect linePrimary = VectSource.create3D(0, 0, 1);
        Vect lineSecondary = VectSource.create3D(0, cos, sin).normalize();

        Sampling sampling = SamplingSource.create(this.size, this.size, this.size);
        Volume fibers = VolumeSource.create(sampling, proto.getEncodingSize());
        fibers.setModel(ModelType.Fibers);

        Mask bundle = new Mask(sampling);
        Mask end = new Mask(sampling);

        for (Sample sample : sampling)
        {
            Fibers model = proto.copy();

            double fi = sample.getI() / (double) (sampling.numI() - 1);
            double fj = sample.getJ() / (double) (sampling.numJ() - 1);
            double fk = sample.getK() / (double) (sampling.numK() - 1);

            double pi = Math.abs(fi - 0.5) / (0.5 * this.radiusPrimary);
            double pj = Math.abs(fj - 0.5) / (0.5 * this.radiusPrimary);

            if (pi * pi + pj * pj < 1.0)
            {
                // primary bundle is here
                model.setBaseline(1.0);
                model.setFrac(0, this.fracPrimary);
                model.setLine(0, linePrimary);

                bundle.set(sample, 1);
            }

            double sfj = cos * (fj - 0.5) - sin * (fk - 0.5) + 0.5;
            double sj = Math.abs(sfj - 0.5) / (0.5 * this.radiusSecondary);

            if (pi * pi + sj * sj < 1.0)
            {
                // secondary bundle is here
                model.setBaseline(1.0);
                model.setFrac(1, this.fracSecondary);
                model.setLine(1, lineSecondary);
            }

            if (sample.getK() == 0 && bundle.foreground(sample))
            {
                end.set(sample, 1);
            }

            if (sample.getK() == sampling.numK() - 1 && bundle.foreground(sample))
            {
                end.set(sample, 2);
            }

            fibers.set(sample, model.getEncoding());
        }

        this.output = fibers;
        this.outputBundle = bundle;
        this.outputEnd = end;

        return this;
    }
}
