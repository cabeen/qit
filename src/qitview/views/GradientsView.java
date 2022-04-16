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

package qitview.views;

import com.google.common.collect.Maps;
import com.jogamp.opengl.GL2;
import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import java.util.Map;
import javax.swing.JCheckBox;
import qit.base.Logging;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.mri.ModelUtils;
import qit.data.utils.mri.structs.Gradients;
import qitview.main.Viewer;
import qitview.render.RenderGeometry;
import qitview.render.RenderUtils;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicLabel;
import qitview.widgets.ControlPanel;

public class GradientsView extends AbstractView<Gradients>
{
    private transient RenderGeometry hullRender;
    private transient RenderGeometry linesRender;

    private transient Integer list = null;
    private transient boolean update = false;

    private Boolean hullShow = false;
    private Boolean linesShow = true;
    private Float thick = 0.02f;
    private Float length = 1.0f;
    private int resolution = 12;

    public GradientsView()
    {
        super();

        this.linesRender = new RenderGeometry(() -> {this.update = true;});
        this.hullRender = new RenderGeometry(() -> {this.update = true;});
        super.initPanel();
    }

    public GradientsView setData(Gradients d)
    {
        this.bounds = null;
        super.setData(d);
        return this;
    }

    public void dispose(GL2 gl)
    {
        if (this.list != null)
        {
            gl.glDeleteLists(this.list, 1);
            this.list = null;
            this.update = true;
        }
    }

    protected ControlPanel makeInfoControls()
    {
        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Type: ", new BasicLabel(this.toString()));
        infoPanel.addControl(" ", new BasicLabel());
        {
            final BasicLabel label = new BasicLabel("");
            infoPanel.addControl("Total: ", label);
            this.observable.addObserver((o, arg) ->
            {
                if (this.hasData())
                {
                    label.setText(String.valueOf(this.data.size()));
                }
                else
                {
                    label.setText("NA");
                }
            });
        }
        {
            final BasicLabel label = new BasicLabel("");
            infoPanel.addControl("Baseline: ", label);
            this.observable.addObserver((o, arg) ->
            {
                if (this.hasData())
                {
                    label.setText(String.valueOf(this.data.getNumBaselines()));
                }
                else
                {
                    label.setText("NA");
                }
            });
        }
        {
            final BasicLabel label = new BasicLabel("");
            infoPanel.addControl("Diffusion: ", label);
            this.observable.addObserver((o, arg) ->
            {
                if (this.hasData())
                {
                    label.setText(String.valueOf(this.data.getNumDvecs()));
                }
                else
                {
                    label.setText("NA");
                }
            });
        }

        return infoPanel;
    }

    protected Map<String, ControlPanel> makeRenderControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();

        {
            ControlPanel panel = new ControlPanel();
            {
                JCheckBox element = new JCheckBox();
                element.setToolTipText("show the spherical hull of the gradients");
                element.addItemListener(e ->
                {
                    this.hullShow = e.getStateChange() == ItemEvent.SELECTED;
                    Viewer.getInstance().control.setStatusMessage("changed hull visibility to: " + this.hullShow);
                    this.update = true;
                });
                panel.addControl("Show hull", element);
            }
            {
                JCheckBox element = new JCheckBox();
                element.setToolTipText("show lines of the convex hull");
                element.addItemListener(e ->
                {
                    this.linesShow = e.getStateChange() == ItemEvent.SELECTED;
                    Viewer.getInstance().control.setStatusMessage("changed line visibility to: " + this.linesShow);
                    this.update = true;
                });
                panel.addControl("Show lines", element);
            }
            {
                final BasicFormattedTextField element = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                element.setValue(new Float(this.thick));
                element.setToolTipText("change the thickness of the hull lines");
                element.addPropertyChangeListener("value", e ->
                {
                    this.thick = ((Number) element.getValue()).floatValue();
                    Viewer.getInstance().control.setStatusMessage("updated thickness to " + this.thick);
                    this.update = true;
                });
                panel.addControl("Thickness", element);
            }
            {
                final BasicFormattedTextField element = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                element.setValue(new Float(this.length));
                element.setToolTipText("change the length of the line rendering");
                element.addPropertyChangeListener("value", e ->
                {
                    this.length = ((Number) element.getValue()).floatValue();
                    Logging.info("updated length to: " + this.length);
                    this.update = true;
                });
                panel.addControl("Length", element);
            }
            controls.put("Render", panel);
        }

        controls.put("Lines", this.linesRender.getPanel());
        controls.put("Hull", this.hullRender.getPanel());

        return controls;
    }

    @Override
    public Gradients getData()
    {
        return this.data;
    }

    public void display(GL2 gl)
    {
        if (this.update && this.list != null)
        {
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
                gl.glNewList(idx, GL2.GL_COMPILE);

                if (this.hullShow)
                {
                    Mesh hull = ModelUtils.triangulate(this.data);
                    MeshUtils.computeNormals(hull);

                    this.hullRender.render(gl, hull);
                }

                if (this.linesShow)
                {
                    Mesh mesh = MeshSource.cylinder(this.resolution);
                    MeshUtils.computeNormals(mesh);

                    Vect scale = VectSource.create3D(this.thick, this.thick, this.length);
                    Vect pos = VectSource.create3D(0, 0, 0);

                    for (Vect line : this.data.getBvecs())
                    {

                        gl.glPushMatrix();
                        RenderUtils.glTransform(gl, scale, pos, line);
                        this.linesRender.render(gl, mesh);
                        gl.glPopMatrix();
                    }
                }

                gl.glEndList();
            }
        }

        if (this.list != null)
        {
            gl.glCallList(this.list);
        }
    }
}