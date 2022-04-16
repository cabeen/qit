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

import com.google.common.collect.Maps;
import com.jogamp.opengl.GL2;
import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.SpinnerNumberModel;

import qit.base.structs.Pair;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.models.Kurtosis;
import qit.data.source.MeshSource;
import qit.data.utils.MeshUtils;
import qit.math.source.VectFunctionSource;
import qit.math.structs.Face;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;
import qitview.main.Viewer;
import qitview.render.RenderGlyph;
import qitview.render.RenderUtils;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ControlPanel;

public class RenderKurtosis extends RenderGlyph
{
    private static final String ATTR_SAMPLE = "sample";

    private static final int DETAIL_DEFAULT = 3;
    private static final int DETAIL_MIN = 1;
    private static final int DETAIL_MAX = 5;
    private static final int DETAIL_STEP = 1;

    private transient Map<Integer,Pair<Vects,Mesh>> meshes = Maps.newHashMap();

    private double scale = 1.0;
    private int detail = DETAIL_DEFAULT;
    private double alpha = 10.0;
    private double thresh = 0.0;
    private boolean gauss = false;
    private boolean normalize = true;
    private float wash = 0.0f;

    protected transient Runnable updateParent;

    public RenderKurtosis(Runnable p)
    {
        this.updateParent = p;
    }

    @Override
    public ControlPanel getPanel()
    {
        ControlPanel panel = new ControlPanel();
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.scale));
            elem.addPropertyChangeListener("value", e ->
            {
                double nscale = ((Number) elem.getValue()).doubleValue();
                if (nscale != RenderKurtosis.this.scale)
                {
                    RenderKurtosis.this.scale = nscale;
                    RenderKurtosis.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated scaleCamera to " + RenderKurtosis.this.scale);
                }
            });
            panel.addControl("Scale", elem);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.alpha));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nval = ((Number) tfield.getValue()).doubleValue();
                if (nval != RenderKurtosis.this.alpha)
                {
                    RenderKurtosis.this.alpha = nval;
                    RenderKurtosis.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated alpha to " + RenderKurtosis.this.alpha);
                }
            });
            panel.addControl("Alpha", tfield);
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(this.detail, DETAIL_MIN, DETAIL_MAX, DETAIL_STEP);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                if (value != RenderKurtosis.this.detail)
                {
                    RenderKurtosis.this.detail = value;
                    RenderKurtosis.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated glyph detail to " + RenderKurtosis.this.detail);
                }
            });
            panel.addControl("Detail", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.wash));
            elem.addPropertyChangeListener("value", e ->
            {
                float nwash = ((Number) elem.getValue()).floatValue();
                if (nwash != RenderKurtosis.this.wash)
                {
                    RenderKurtosis.this.wash = nwash;
                    RenderKurtosis.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated wash to " + RenderKurtosis.this.wash);
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
                if (nthresh != RenderKurtosis.this.thresh)
                {
                    RenderKurtosis.this.thresh = nthresh;
                    RenderKurtosis.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thresh to " + RenderKurtosis.this.thresh);
                }
            });
            panel.addControl("Threshold", tfield);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setSelected(this.gauss);
            elem.addItemListener(e ->
            {
                RenderKurtosis.this.gauss = e.getStateChange() == ItemEvent.SELECTED;
                RenderKurtosis.this.updateParent.run();
                Viewer.getInstance().control.setStatusMessage("updated gaussian to " + RenderKurtosis.this.gauss);
            });
            panel.addControl("Gaussian", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setSelected(this.normalize);
            elem.addItemListener(e ->
            {
                RenderKurtosis.this.normalize = e.getStateChange() == ItemEvent.SELECTED;
                RenderKurtosis.this.updateParent.run();
                Viewer.getInstance().control.setStatusMessage("updated normalization to " + RenderKurtosis.this.normalize);
            });
            panel.addControl("Normalize", elem);
        }

        return panel;
    }

    @Override
    public boolean valid(int dim)
    {
        return Kurtosis.valid(dim);
    }

    @Override
    public void renderModel(GL2 gl, Vect coord, Vect model)
    {
        if (!this.meshes.containsKey(this.detail))
        {
            Mesh mesh = MeshSource.sphere(this.detail);

            Vects samples = new Vects();
            for (Vertex vert : mesh.graph.verts())
            {
                Vect sample = mesh.vattr.get(vert, Mesh.COORD).normalize();
                Vect color = VectFunctionSource.rgb().apply(sample);

                mesh.graph.add(vert);
                mesh.vattr.set(vert, Mesh.COORD, sample);
                mesh.vattr.set(vert, Mesh.COLOR, color);
                mesh.vattr.set(vert, Mesh.NORMAL, sample);

                samples.add(sample);
            }

            this.meshes.put(this.detail, Pair.of(samples, mesh));
        }

        Pair<Vects,Mesh> pair = this.meshes.get(this.detail);
        Vects samples = pair.a;
        Mesh mesh = pair.b;
        Vect odf = new Kurtosis(model).odf(this.gauss, this.alpha, samples);

        if (odf == null || odf.nan() || odf.infinite())
        {
            return;
        }

        double min = odf.min();
        double max = odf.max();
        double delta = MathUtils.eq(min, max) ? 1.0 : max - min;

        int idx = 0;
        for (Vertex vert : mesh.graph.verts())
        {
            double amp = odf.get(idx);

            if (this.normalize)
            {
                amp = (amp - min) / delta;
            }

            Vect pos = samples.get(idx).times(amp);
            idx += 1;

            mesh.vattr.set(vert, Mesh.COORD, pos);
        }

        VectFunction washer = null;

        if (MathUtils.nonzero(this.wash))
        {
            washer = VectFunctionSource.wash(this.wash);
        }

        MeshUtils.computeNormals(mesh);

        gl.glPushMatrix();
        RenderUtils.glTransform(gl, this.scale, coord);
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

                Vect p = mesh.vattr.get(v, Mesh.COORD);
                double px = p.get(0);
                double py = p.get(1);
                double pz = p.get(2);

                Vect n = mesh.vattr.get(v, Mesh.NORMAL);
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