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

package qit.data.modules.mesh;

import qit.base.Logging;
import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOptional;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Mesh;

@ModuleDescription("Close a mesh vertex selection")
@ModuleAuthor("Ryan Cabeen")
public class MeshAttrClose implements Module
{
    @ModuleInput
    @ModuleDescription("input mesh")
    public Mesh input;

    @ModuleParameter
    @ModuleDescription("select the largest component as an intermediate step")
    public boolean largest = false;

    @ModuleParameter
    @ModuleDescription("the number of iterations")
    public int num = 1;

    @ModuleParameter
    @ModuleDescription("the input attribute")
    public String attrin = Mesh.SELECTION;

    @ModuleParameter
    @ModuleOptional
    @ModuleDescription("the output attribute")
    public String attrout = null;

    @ModuleParameter
    @ModuleDescription("run without copying data (be aware of side effects)")
    public boolean inplace = false;

    @ModuleOutput
    @ModuleDescription("output mesh")
    public Mesh output;

    @Override
    public MeshAttrClose run()
    {
        Mesh mesh = this.inplace ? this.input : this.input.copy();

        Logging.info("started mesh selection closing");

        MeshAttrDilate dilater = new MeshAttrDilate();
        dilater.attrin = this.attrin;
        dilater.attrout = this.attrout;
        dilater.num = this.num;
        dilater.inplace = this.inplace;
        mesh = dilater.run().output;

        if (this.largest)
        {
            MeshAttrComponents comps = new MeshAttrComponents();
            comps.input = mesh;
            comps.largest = true;
            comps.attrin = this.attrin;
            comps.attrout = this.attrout;
            comps.inplace = inplace;
            mesh = comps.run().output;
        }

        MeshAttrErode eroder = new MeshAttrErode();
        eroder.input = mesh;
        eroder.attrin = this.attrin;
        eroder.attrout = this.attrout;
        eroder.num = this.num;
        eroder.inplace = this.inplace;
        mesh = eroder.run().output;

        Logging.info("finished mesh selection closing");

        this.output = mesh;
        return this;
    }
}
