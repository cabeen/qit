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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jogamp.opengl.GL2;

import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import javax.swing.JCheckBox;

import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.models.Spharm;
import qit.data.utils.MeshUtils;
import qit.math.source.VectFunctionSource;
import qit.math.structs.Face;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;
import qitview.main.Viewer;
import qitview.render.RenderGlyph;
import qitview.render.RenderUtils;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.ControlPanel;

public class RenderSpharm extends RenderGlyph
{
    private double scale = 1.0;
    private int order = 8;
    private int detail = Spharm.DETAIL_DEFAULT;
    private double minamp = 0;
    private double maxamp = 10;
    private double thresh = 0.15;
    private boolean highlight = false;
    private boolean normalize = false;
    private float wash = 0.0f;
    private ColorOrder corder = ColorOrder.RGB;

    private enum ColorOrder
    {
        RGB, RBG, GRB, GBR, BRG, BGR
    }

    protected transient Runnable updateParent;

    public RenderSpharm(Runnable p)
    {
        this.updateParent = p;
    }

    @Override
    public ControlPanel getPanel()
    {
        ControlPanel panel = new ControlPanel();
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setToolTipText("Scale the glyphs by the given amount.  Useful for detailing with unusual voxel sizes");
            elem.setValue(new Double(this.scale));
            elem.addPropertyChangeListener("value", e ->
            {
                double nscale = ((Number) elem.getValue()).doubleValue();
                if (nscale != RenderSpharm.this.scale)
                {
                    RenderSpharm.this.scale = nscale;
                    RenderSpharm.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated scaleCamera to " + RenderSpharm.this.scale);
                }
            });
            panel.addControl("Scale", elem);
        }
        {
            Integer[] arr = {2, 4, 8, 10, 12, 14, 16};

            BasicComboBox<Integer> elem = new BasicComboBox<>(arr);
            elem.setToolTipText("Specify the maximum order used.  If the data has a lower order, that will take precedence");
            elem.setSelectedItem(this.order);
            elem.addActionListener(e ->
            {
                Integer value = (Integer) elem.getSelectedItem();
                if (value != RenderSpharm.this.order)
                {
                    RenderSpharm.this.order = value;
                    RenderSpharm.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated glyph order to " + RenderSpharm.this.order);
                }
            });
            panel.addControl("Max Order", elem);
        }
        {
            List<Integer> list = Lists.newArrayList();
            for (int d = 1; d <= Spharm.DETAIL_MAX; d++)
            {
                list.add(d);
            }

            Integer[] arr = new Integer[list.size()];
            list.toArray(arr);

            BasicComboBox<Integer> elem = new BasicComboBox<>(arr);
            elem.setToolTipText("Specify the detail of the mesh.  Each detail level increments the mesh triangle count by about 500");
            elem.setSelectedItem(this.detail);
            elem.addActionListener(e ->
            {
                Integer value = (Integer) elem.getSelectedItem();
                if (value != RenderSpharm.this.detail)
                {
                    RenderSpharm.this.detail = value;
                    RenderSpharm.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated glyph detail to " + RenderSpharm.this.detail);
                }
            });
            panel.addControl("Detail", elem);
        }
        {
            List<String> list = Lists.newArrayList();
            for (ColorOrder c : ColorOrder.values())
            {
                list.add(c.toString());
            }

            String[] arr = new String[list.size()];
            list.toArray(arr);

            BasicComboBox<String> elem = new BasicComboBox<>(arr);
            elem.setToolTipText("Specify the ordering of the colors mapping XYZ directions");
            elem.setSelectedItem(this.corder.toString());
            elem.addActionListener(e ->
            {
                ColorOrder value = ColorOrder.valueOf((String) elem.getSelectedItem());
                if (value != RenderSpharm.this.corder)
                {
                    RenderSpharm.this.corder = value;
                    RenderSpharm.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated glyph coloring to " + RenderSpharm.this.corder);
                }
            });
            panel.addControl("Coloring", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setToolTipText("Wash out the colors by blending them with white by the given amount");
            elem.setValue(new Double(this.wash));
            elem.addPropertyChangeListener("value", e ->
            {
                float nwash = ((Number) elem.getValue()).floatValue();
                if (nwash != RenderSpharm.this.wash)
                {
                    RenderSpharm.this.wash = nwash;
                    RenderSpharm.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated wash to " + RenderSpharm.this.wash);
                }
            });
            panel.addControl("Wash", elem);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setToolTipText("Specify the minimum amplitude to render.  Any mesh vertices below this will be hidden");
            tfield.setValue(new Double(this.minamp));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nminamp = ((Number) tfield.getValue()).doubleValue();
                if (nminamp != RenderSpharm.this.minamp)
                {
                    RenderSpharm.this.minamp = nminamp;
                    RenderSpharm.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated minamp to " + RenderSpharm.this.minamp);
                }
            });
            panel.addControl("Min. Amp.", tfield);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setToolTipText("Specify the maximum amplitude to render.  Any mesh vertices above this will be hidden");
            tfield.setValue(new Double(this.maxamp));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nmaxmap = ((Number) tfield.getValue()).doubleValue();
                if (nmaxmap != RenderSpharm.this.maxamp)
                {
                    RenderSpharm.this.maxamp = nmaxmap;
                    RenderSpharm.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated maxamp to " + RenderSpharm.this.maxamp);
                }
            });
            panel.addControl("Max. Amp.", tfield);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.thresh));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nthresh = ((Number) tfield.getValue()).doubleValue();
                if (nthresh != RenderSpharm.this.thresh)
                {
                    RenderSpharm.this.thresh = nthresh;
                    RenderSpharm.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thresh to " + RenderSpharm.this.thresh);
                }
            });
            panel.addControl("Threshold", tfield);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setToolTipText("Apply highlighting to the glyph.  Emphasize colors closer to the maximum amplitude");
            elem.setSelected(this.highlight);
            elem.addItemListener(e ->
            {
                RenderSpharm.this.highlight = e.getStateChange() == ItemEvent.SELECTED;
                RenderSpharm.this.updateParent.run();
                Viewer.getInstance().control.setStatusMessage("updated highlight to " + RenderSpharm.this.highlight);
            });
            panel.addControl("Highlight", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setToolTipText("Scale the glyph so that they are all roughly the same size.");
            elem.setSelected(this.normalize);
            elem.addItemListener(e ->
            {
                RenderSpharm.this.normalize = e.getStateChange() == ItemEvent.SELECTED;
                RenderSpharm.this.updateParent.run();
                Viewer.getInstance().control.setStatusMessage("updated normalization to " + RenderSpharm.this.normalize);
            });
            panel.addControl("Normalize", elem);
        }

        return panel;
    }

    @Override
    public boolean valid(int dim)
    {
        return Spharm.valid(dim);
    }

    @Override
    public void renderModel(GL2 gl, Vect coord, Vect model)
    {
        int size = model.size();
        int order = Spharm.sizeToOrder(size);
        int maxOrder = Spharm.nearestOrder(this.order);

        if (maxOrder < order)
        {
            order = maxOrder;
            model = model.sub(0, Spharm.orderToSize(maxOrder));
        }

        Mesh mesh = Spharm.getMesh(this.detail, order);
        Matrix matrix = Spharm.getMatrix(this.detail, order);
        Vect amps = matrix.times(model);

        double min = Math.max(0, amps.min());
        double max = Math.max(0, amps.max());
        double delta = MathUtils.eq(min, max) ? 1.0 : max - min;

        if (max < this.thresh)
        {
            return;
        }

        Map<Vertex, Double> scalings = Maps.newHashMap();
        {
            int idx = 0;
            for (Vertex vert : mesh.graph.verts())
            {
                double amp = amps.get(idx);
                idx += 1;

                if (amp < this.minamp)
                {
                    amp = 0;
                }

                if (amp > this.maxamp)
                {
                    amp = 0;
                }

                double scaling = (amp - min) / delta;

                if (this.normalize)
                {
                    amp = scaling;
                }

                scalings.put(vert, scaling);

                Vect point = mesh.vattr.get(vert, Spharm.ATTR_LINE);
                mesh.vattr.set(vert, Mesh.COORD, point.times(amp));
            }
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

                if (this.highlight)
                {
                    double scale = scalings.get(v);
                    scale = Math.sqrt(Math.max(scale, 1e-3));

                    r *= scale;
                    g *= scale;
                    b *= scale;
                }

                Vect p = mesh.vattr.get(v, Mesh.COORD);
                double px = p.get(0);
                double py = p.get(1);
                double pz = p.get(2);

                Vect n = mesh.vattr.get(v, Mesh.NORMAL);
                double nx = n.get(0);
                double ny = n.get(1);
                double nz = n.get(2);

                if (this.corder.equals(ColorOrder.RGB))
                {
                    gl.glColor3d(r, g, b);
                }
                else if (this.corder.equals(ColorOrder.RBG))
                {
                    gl.glColor3d(r, b, g);
                }
                else if (this.corder.equals(ColorOrder.GBR))
                {
                    gl.glColor3d(b, r, g);
                }
                else if (this.corder.equals(ColorOrder.GRB))
                {
                    gl.glColor3d(g, r, b);
                }
                else if (this.corder.equals(ColorOrder.BRG))
                {
                    gl.glColor3d(g, b, r);
                }
                else if (this.corder.equals(ColorOrder.BGR))
                {
                    gl.glColor3d(b, g, r);
                }
                else
                {
                    Logging.info("invalid spharm glyph coloring!");
                }

                gl.glNormal3d(nx, ny, nz);
                gl.glVertex3d(px, py, pz);
            }
        }
        gl.glEnd();
        gl.glPopMatrix();
    }
}