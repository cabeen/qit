/*******************************************************************************
 * Copyright (c) 2010-2016, Ryan Cabeen
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. All advertising materials mentioning features or use of this software
 * must display the following acknowledgement:
 * This product includes software developed by the Ryan Cabeen.
 * 4. Neither the name of the Ryan Cabeen nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY RYAN CABEEN ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RYAN CABEEN BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package qit.data.modules.vects;

import qit.base.Module;
import qit.base.annot.ModuleAuthor;
import qit.base.annot.ModuleDescription;
import qit.base.annot.ModuleInput;
import qit.base.annot.ModuleOutput;
import qit.base.annot.ModuleParameter;
import qit.data.datasets.Affine;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.math.structs.Line;

@ModuleDescription("Compute an affine transform to align a set of points to the z-axis")
@ModuleAuthor("Ryan Cabeen")
public class VectsAlignAxis implements Module
{
    @ModuleInput
    @ModuleDescription("input vects (should be roughly linear")
    public Vects input;

    @ModuleOutput
    @ModuleDescription("output affine transform")
    public Affine output;

    @Override
    public VectsAlignAxis run()
    {
        this.output = align(this.input);

        return this;
    }

    public static Affine align(Vects vects)
    {
        Vect dir = Line.fit(vects).getDir();
        Vect ref = vects.get(vects.size() - 1).minus(vects.get(0)).normalize();

        if (dir.dot(ref) < 0)
        {
            dir.timesEquals(-1.0);
        }

        Vect rz = dir.normalize();
        Vect ry = rz.perp();
        Vect rx = rz.cross(ry);

        Matrix rot = new Matrix(3,3);
        rot.setRow(0, rx);
        rot.setRow(1, ry);
        rot.setRow(2, rz);

        Affine xfm = new Affine(rot, VectSource.create3D());

        return xfm;
    }
}
