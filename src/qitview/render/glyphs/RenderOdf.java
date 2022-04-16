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

package qitview.render.glyphs;

import com.jogamp.opengl.GL2;

import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import javax.swing.JCheckBox;
import qit.base.structs.Named;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.modules.mri.odf.VolumeOdfFeature;
import qit.data.modules.vects.VectsHull;
import qit.data.utils.MeshUtils;
import qit.data.utils.VectsUtils;
import qit.math.source.VectFunctionSource;
import qit.math.structs.Face;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;
import qitview.main.Viewer;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.render.RenderGlyph;
import qitview.render.RenderUtils;
import qitview.views.VectsView;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.ControlPanel;

public class RenderOdf extends RenderGlyph
{
    private transient Vects points = null;
    private transient Mesh mesh = null;

    private double scale = 1.0;
    private double thresh = 0.01;
    private boolean minmax = false;
    private boolean gfa = false;
    private float wash = 0.0f;

    protected transient Runnable updateParent;

    public RenderOdf(Runnable p)
    {
        this.updateParent = p;
    }

    public void setPoints(Vects v)
    {
        Viewer.getInstance().control.setStatusMessage("normalizing points");
        this.points = VectsUtils.normalize(v);

        Viewer.getInstance().control.setStatusMessage("generating glyph mesh from convex hull");
        VectsHull huller = new VectsHull();
        huller.input = this.points;
        Mesh hull = huller.run().output;

        for (Vertex vert : hull.graph.verts())
        {
            Vect sample = hull.vattr.get(vert, Mesh.COORD).normalize();
            Vect color = VectFunctionSource.rgb().apply(sample);

            hull.vattr.set(vert, Mesh.COLOR, color);
            hull.vattr.set(vert, Mesh.NORMAL, sample);
        }

        this.mesh = hull;
    }

    @Override
    public ControlPanel getPanel()
    {
        ControlPanel panel = new ControlPanel();
        {
            final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Vects, true, true);
            elem.addActionListener(e ->
            {
                Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                VectsView view = (VectsView) entry.getValue();

                if (view != null && view.hasData())
                {
                    RenderOdf.this.setPoints(view.getData());
                    Viewer.getInstance().control.setStatusMessage("using points: " + entry.getName());
                }
                else
                {
                    RenderOdf.this.points = null;
                    RenderOdf.this.mesh = null;
                }
            });
            panel.addControl("Points", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.scale));
            elem.addPropertyChangeListener("value", e ->
            {
                double nscale = ((Number) elem.getValue()).doubleValue();
                if (nscale != RenderOdf.this.scale)
                {
                    RenderOdf.this.scale = nscale;
                    RenderOdf.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated scaleCamera to " + RenderOdf.this.scale);
                }
            });
            panel.addControl("Scale", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.wash));
            elem.addPropertyChangeListener("value", e ->
            {
                float nwash = ((Number) elem.getValue()).floatValue();
                if (nwash != RenderOdf.this.wash)
                {
                    RenderOdf.this.wash = nwash;
                    RenderOdf.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated wash to " + RenderOdf.this.wash);
                }
            });
            panel.addControl("Wash", elem);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.thresh));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nthresh = ((Number) tfield.getValue()).doubleValue();
                if (nthresh != RenderOdf.this.thresh)
                {
                    RenderOdf.this.thresh = nthresh;
                    RenderOdf.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thresh to " + RenderOdf.this.thresh);
                }
            });
            panel.addControl("Threshold", tfield);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setSelected(this.minmax);
            elem.addItemListener(e ->
            {
                RenderOdf.this.minmax = e.getStateChange() == ItemEvent.SELECTED;
                RenderOdf.this.updateParent.run();
                Viewer.getInstance().control.setStatusMessage("updated normalization to " + RenderOdf.this.minmax);
            });
            panel.addControl("Min Max Normalize", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setSelected(this.gfa);
            elem.addItemListener(e ->
            {
                RenderOdf.this.gfa = e.getStateChange() == ItemEvent.SELECTED;
                RenderOdf.this.updateParent.run();
                Viewer.getInstance().control.setStatusMessage("updated gfa scaling to " + RenderOdf.this.gfa);
            });
            panel.addControl("Scale by GFA", elem);
        }

        return panel;
    }

    @Override
    public boolean valid(int dim)
    {
        return this.mesh != null && this.points.size() == dim;
    }

    @Override
    public void renderModel(GL2 gl, Vect coord, Vect model)
    {
        if (this.mesh != null)
        {
            Vect odf = model;

            if (odf == null || odf.nan() || odf.infinite())
            {
                return;
            }

            double min = odf.min();
            double max = odf.max();
            double delta = MathUtils.eq(min, max) ? 1.0 : max - min;

            if (max < this.thresh)
            {
                return;
            }

            double factor = this.gfa ? 7.5 * VolumeOdfFeature.gfa(odf) : 1.0;

            int idx = 0;
            for (Vertex vert : this.mesh.graph.verts())
            {
                double amp = odf.get(idx);

                if (this.minmax)
                {
                    amp = (amp - min) / delta;
                }

                Vect pos = this.points.get(idx).times(amp);
                idx += 1;

                this.mesh.vattr.set(vert, Mesh.COORD, pos);
            }

            VectFunction washer = null;

            if (MathUtils.nonzero(this.wash))
            {
                washer = VectFunctionSource.wash(this.wash);
            }

            MeshUtils.computeNormals(this.mesh);

            gl.glPushMatrix();
            RenderUtils.glTransform(gl, this.scale * factor, coord);
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            gl.glEnable(GL2.GL_CULL_FACE);
            gl.glCullFace(GL2.GL_BACK);
            gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
            gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[]{1f, 1f, 1f, 1f}, 0);

            gl.glShadeModel(GL2.GL_SMOOTH);

            gl.glBegin(GL2.GL_TRIANGLES);
            for (Face f : mesh.graph.faces())
            {
                Vertex va = f.getA();
                Vertex vb = f.getB();
                Vertex vc = f.getC();

                for (Vertex v : new Vertex[]{va, vb, vc})
                {
                    Vect c = mesh.vattr.get(v, Mesh.COLOR);

                    if (washer != null)
                    {
                        c = washer.apply(c);
                    }

                    double r = c.get(0);
                    double g = c.get(1);
                    double b = c.get(2);

                    Vect p = this.mesh.vattr.get(v, Mesh.COORD);
                    double px = p.get(0);
                    double py = p.get(1);
                    double pz = p.get(2);

                    Vect n = this.mesh.vattr.get(v, Mesh.NORMAL);
                    double nx = n.get(0);
                    double ny = n.get(1);
                    double nz = n.get(2);

                    gl.glColor3d(r, g, b);
                    gl.glNormal3d(nx, ny, nz);
                    gl.glVertex3d(px, py, pz);
                }
            }
            gl.glEnd();
            gl.glPopMatrix();
        }
    }
}