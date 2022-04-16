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

import qit.base.Global;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Curves;
import qit.data.source.VectSource;

@ModuleDescription("Create a curves dataset consisting of a helix bundle")
@ModuleAuthor("Ryan Cabeen")
public class CurvesCreateHelix implements Module
{
    @ModuleParameter
    @ModuleDescription("the radius of the helix")
    public double radius = 10;

    @ModuleParameter
    @ModuleDescription("the length of one loop of the helix")
    public double length = 5;

    @ModuleParameter
    @ModuleDescription("the number of loops in the helix")
    public int loops = 2;

    @ModuleParameter
    @ModuleDescription("the number of steps sampled along each loop of the helix")
    public int steps = 100;

    @ModuleParameter
    @ModuleDescription("the width of the helix bundle")
    public double sigma = 1;

    @ModuleParameter
    @ModuleDescription("the number of samples in the helix bundle")
    public int samples = 1;

    @ModuleParameter
    @ModuleDescription("the starting position in x")
    public double startx = 0;

    @ModuleParameter
    @ModuleDescription("the starting position in y")
    public double starty = 0;

    @ModuleParameter
    @ModuleDescription("the starting position in z")
    public double startz = 0;

    @ModuleOutput
    @ModuleDescription("output curves")
    public Curves output;

    @Override
    public CurvesCreateHelix run()
    {
        Curves out = new Curves();

        int points = this.loops * this.steps;
        double delta = 2.0 * Math.PI / this.steps;

        for (int s = 0; s < this.samples; s++)
        {
            double sx = this.samples > 1 ? this.sigma * Global.RANDOM.nextGaussian() : 0;
            double sy = this.samples > 1 ? this.sigma * Global.RANDOM.nextGaussian() : 0;
            double sz = this.samples > 1 ? this.sigma * Global.RANDOM.nextGaussian() : 0;

            Curves.Curve curve = out.add(points);

            for (int i = 0; i < points; i++)
            {
                double t = i * delta;
                double px = this.startx + sx + this.radius * Math.cos(t);
                double py = this.starty = sy + this.radius * Math.sin(t);
                double pz = this.startz + sz + this.length * t;

                curve.set(i, VectSource.create3D(px, py, pz));
            }
        }

        this.output = out;

        return this;
    }
}
