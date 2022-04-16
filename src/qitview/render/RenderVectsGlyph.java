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

import com.jogamp.opengl.GL2;

import java.util.Observable;
import java.util.Observer;

import qit.base.Logging;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qitview.main.Viewer;
import qitview.models.WorldMouse;
import qitview.views.VectsView;
import smile.math.Math;

public class RenderVectsGlyph
{
    private static final int MAX_OFFSET = 512;

    private VectsView parent;

    private transient Integer list = null;
    private transient boolean update = false;

    public RenderVectsGlyph(VectsView parent)
    {
        this.parent = parent;
        this.parent.observable.addObserver((a, b) -> this.update());
        this.update();
    }

    public void update()
    {
        this.update = true;
    }

    public void dispose(GL2 gl)
    {
        if (this.list != null)
        {
            Logging.info(String.format("deleting glyph display list for %s", this.parent.getName()));
            gl.glDeleteLists(this.list, 1);
            this.list = null;
            this.update = true;
        }
    }

    public void display(GL2 gl, RenderGlyph glyph)
    {
        if (this.parent == null)
        {
            return;
        }

        if (!this.parent.hasData())
        {
            return;
        }

        if (this.update && this.list != null)
        {
            Logging.info(String.format("deleting glyph display list for %s", this.parent.getName()));
            gl.glDeleteLists(this.list, 1);
            this.list = null;
            this.update = false;
        }

        if (this.list == null)
        {
            int idx = gl.glGenLists(1);
            if (idx != 0)
            {
                this.list = idx;

                Logging.info(String.format("creating glyph display list for %s", this.parent.getName()));
                gl.glNewList(idx, GL2.GL_COMPILE);

                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth(1);
                gl.glColor3f(1f, 1f, 1f);

                for (int i = 0; i < this.parent.getData().size(); i++)
                {
                    if (this.parent.getVisibility(i))
                    {
                        Vect coord = this.parent.getCoord(i);
                        Vect model = this.parent.getGlyph(i);
                        glyph.renderModel(gl, coord, model);
                    }
                }
            }

            gl.glEndList();
        }

        if (this.list != null)
        {
            gl.glCallList(this.list);
        }
    }

    public void handle(WorldMouse mouse)
    {
        if (!this.parent.hasData() || (mouse.press == null && mouse.current == null))
        {
            return;
        }

        Vect hit = mouse.current == null ? mouse.press.hit : mouse.current.hit;

        Vects vects = this.parent.getData();

        Double nearestDist = null;
        Vect nearestCoord = null;
        Vect nearestModel = null;
        for (Vect vect : vects)
        {
            Vect coord = vect.sub(0, 3);
            Vect param = vect.sub(3, vect.size());

            double dist = coord.dist(hit);
            if (nearestDist == null || dist < nearestDist)
            {
                nearestDist = dist;
                nearestCoord = coord;
                nearestModel = param;
            }
        }

        if (nearestDist != null)
        {
            Viewer.getInstance().control.setStatusMessage("position: " + nearestCoord + ", model: " + nearestModel);
        }
    }
}