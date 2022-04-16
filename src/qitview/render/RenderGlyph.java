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

package qitview.render;

import com.google.common.collect.Lists;
import com.jogamp.opengl.GL2;
import java.util.List;

import qit.base.Global;
import qit.base.structs.Pair;
import qit.data.datasets.Vect;
import qitview.models.Viewable;
import qitview.render.glyphs.RenderFibers;
import qitview.render.glyphs.RenderKurtosis;
import qitview.render.glyphs.RenderNoddi;
import qitview.render.glyphs.RenderOdf;
import qitview.render.glyphs.RenderSpharm;
import qitview.render.glyphs.RenderTensorEllipsoid;
import qitview.render.glyphs.RenderTensorStick;
import qitview.render.glyphs.RenderVectorStick;
import qitview.widgets.ControlPanel;

public abstract class RenderGlyph
{
    abstract public ControlPanel getPanel();

    abstract public boolean valid(int dim);

    public abstract void renderModel(GL2 gl, Vect coord, Vect model);

    public static List<Pair<String, RenderGlyph>> getAll(Runnable updateParent)
    {
        List<Pair<String, RenderGlyph>> all = Lists.newArrayList();
        all.add(Pair.of("VectorStick", new RenderVectorStick(updateParent)));
        all.add(Pair.of("TensorStick", new RenderTensorStick(updateParent)));
        all.add(Pair.of("TensorEllipsoid", new RenderTensorEllipsoid(updateParent)));
        all.add(Pair.of("Kurtosis", new RenderKurtosis(updateParent)));
        all.add(Pair.of("Fibers", new RenderFibers(updateParent)));
        all.add(Pair.of("Spharm", new RenderSpharm(updateParent)));
        all.add(Pair.of("ODF", new RenderOdf(updateParent)));

        if (Global.getExpert())
        {
            all.add(Pair.of("Noddi", new RenderNoddi(updateParent)));
        }

        return all;
    }
}