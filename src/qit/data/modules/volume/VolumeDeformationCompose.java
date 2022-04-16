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

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.utils.volume.VolumeInterpTrilinear;
import qit.math.structs.VectFunction;

@ModuleDescription("Compose a deformation field with an affine transform")
@ModuleAuthor("Ryan Cabeen")
public class VolumeDeformationCompose implements Module
{
    @ModuleInput
    @ModuleDescription("an affine transform")
    public Affine affine;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("an deform deformation field")
    public Volume deform;

    @ModuleInput
    @ModuleOptional
    @ModuleDescription("a reference image")
    public Volume reference;

    @ModuleParameter
    @ModuleDescription("invert the affine transform")
    public boolean invert = false;

    @ModuleParameter
    @ModuleDescription("reverse the order of the transforms")
    public boolean reverse = false;

    @ModuleOutput
    @ModuleDescription("output")
    public Volume output;

    @Override
    public VolumeDeformationCompose run()
    {
        Affine myaffine = this.invert ? this.affine.inv() : this.affine;
        Volume out = this.reference == null ? this.deform.proto(3) : this.reference.proto(3);
        Sampling sampling = out.getSampling();

        if (this.deform != null)
        {
            VectFunction mydeform = new VolumeInterpTrilinear(this.deform);

            if (this.reverse)
            {
                for (Sample sample : out.getSampling())
                {
                    Vect world = sampling.world(sample);
                    Vect affed = myaffine.apply(world);
                    Vect deformed = affed.plus(mydeform.apply(affed));
                    Vect displace = deformed.minus(world);
                    out.set(sample, displace);
                }
            }
            else
            {
                for (Sample sample : out.getSampling())
                {
                    Vect world = sampling.world(sample);
                    Vect deformed = world.plus(mydeform.apply(world));
                    Vect affed = myaffine.apply(deformed);
                    Vect displace = affed.minus(world);
                    out.set(sample, displace);
                }
            }
        }
        else
        {
            if (this.reverse)
            {
                for (Sample sample : out.getSampling())
                {
                    Vect world = sampling.world(sample);
                    Vect affed = myaffine.apply(world);
                    Vect displace = affed.minus(world);
                    out.set(sample, displace);
                }
            }
            else
            {
                for (Sample sample : out.getSampling())
                {
                    Vect world = sampling.world(sample);
                    Vect affed = myaffine.apply(world);
                    Vect displace = affed.minus(world);
                    out.set(sample, displace);
                }
            }
        }

        this.output = out;
        return this;
    }
}
