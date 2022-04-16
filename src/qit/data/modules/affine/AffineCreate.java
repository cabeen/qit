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

package qit.data.modules.affine;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.math.structs.Quaternion;

@ModuleDescription("Create an affine transform based on user specified parameters")
@ModuleAuthor("Ryan Cabeen")
public class AffineCreate implements Module
{
    @ModuleParameter
    @ModuleDescription("translation in x")
    public double transX = 0;

    @ModuleParameter
    @ModuleDescription("translation in y")
    public double transY = 0;

    @ModuleParameter
    @ModuleDescription("translation in z")
    public double transZ = 0;

    @ModuleParameter
    @ModuleDescription("rotation axis in x")
    public double rotX = 0;

    @ModuleParameter
    @ModuleDescription("rotation axis in y")
    public double rotY = 0;

    @ModuleParameter
    @ModuleDescription("rotation axis in z")
    public double rotZ = 0;

    @ModuleParameter
    @ModuleDescription("rotation angle")
    public double rotA = 0;

    @ModuleParameter
    @ModuleDescription("scaleCamera in x")
    public double scaleX = 1;

    @ModuleParameter
    @ModuleDescription("scaleCamera in y")
    public double scaleY = 1;

    @ModuleParameter
    @ModuleDescription("scaleCamera in z")
    public double scaleZ = 1;

    @ModuleParameter
    @ModuleDescription("skew in x")
    public double skewX = 0;

    @ModuleParameter
    @ModuleDescription("skew in y")
    public double skewY = 0;

    @ModuleParameter
    @ModuleDescription("skew in z")
    public double skewZ = 0;

    @ModuleOutput
    @ModuleDescription("output affine")
    public Affine output;
    
    @Override
    public AffineCreate run()
    {
        Matrix rotMatrix = MatrixSource.rotation(VectSource.create3D(this.rotX, this.rotY, this.rotZ), this.rotA);
        Matrix scaleMatrix = MatrixSource.diag(this.scaleX, this.scaleY, this.scaleZ);
        Matrix skewMatrix = MatrixSource.skew(this.skewX, this.skewY, this.skewZ);
        Matrix linear = rotMatrix.times(scaleMatrix).times(skewMatrix);
        Vect trans = VectSource.create3D(this.transX, this.transY, this.transZ);

        this.output = new Affine(linear, trans);

        return this;
    }
}
