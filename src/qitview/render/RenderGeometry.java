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


package qitview.render;

import com.google.common.collect.Sets;
import com.jogamp.opengl.GL2;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliUtils;
import qit.base.structs.Named;
import qit.base.utils.JavaUtils;
import qit.data.datasets.Affine;
import qit.data.datasets.Mesh;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.math.structs.Face;
import qit.math.structs.Vertex;
import qit.math.utils.MathUtils;
import qitview.main.Viewer;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.panels.Viewables;
import qitview.views.AffineView;
import qitview.views.SolidsView;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ColorWidget;
import qitview.widgets.ControlPanel;

import javax.swing.JCheckBox;
import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

public class RenderGeometry extends Observable
{
    private static final int MIN_WIDTH = 1;
    private static final int MAX_WIDTH = 25;
    private static final int STEP_WIDTH = 1;

    private Observer updateObserver = new Observer()
    {
        public void update(Observable o, Object arg)
        {
            RenderGeometry.this.changed();
        }
    };

    private ControlPanel panel = new ControlPanel();
    private Runnable updateParent;
    private AffineView affine;

    protected String coord = Mesh.COORD;

    private boolean showVertices = false;
    protected double opacityVertices = 1.0f;
    protected boolean solidVertices = true;
    protected double liftVertices = 0;
    protected int widthVertices = 2;
    protected Color colorVertices = Color.WHITE;

    protected boolean showWireframe = false;
    protected double opacityWireframe = 1.0f;
    protected boolean solidWireframe = true;
    protected double liftWireframe = 0;
    protected int widthWireframe = 2;
    protected Color colorWireframe = Color.WHITE;

    protected boolean showMesh = true;
    protected double opacityMesh = 1.0f;
    protected boolean showNormals = false;
    protected double lengthNormal = 1.0f;

    protected double meshShine = 125.0f;
    protected String name = Mesh.LABEL;
    protected String include = "";
    protected String exclude = "";
    protected boolean lighting = true;
    protected boolean opacityAdvanced = true;
    protected boolean meshSmooth = true;
    protected boolean showBack = false;
    protected boolean flipNormal = false;
    protected boolean flipFace = false;
    protected boolean cullFace = false;
    protected boolean sortReverse = false;

    private SolidsView solidsClip;

    public RenderGeometry(final Runnable p)
    {
        this.updateParent = p;

        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("render the mesh vertices as points");
            elem.setSelected(this.showVertices);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.showVertices = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated mesh visibility to " + RenderGeometry.this.showVertices);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.showVertices));
            this.panel.addControl("Show vertices", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("render the mesh wireframe as lines");
            elem.setSelected(this.showWireframe);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.showWireframe = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated mesh wireframe to " + RenderGeometry.this.showWireframe);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.showWireframe));
            this.panel.addControl("Show wireframe", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("render the mesh as a 3D surface");
            elem.setSelected(this.showMesh);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.showMesh = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated mesh visibility to " + RenderGeometry.this.showMesh);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.showMesh));
            this.panel.addControl("Show mesh", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("render vertex normals");
            elem.setSelected(this.showNormals);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.showNormals = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated mesh normals to " + RenderGeometry.this.showNormals);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.showNormals));
            this.panel.addControl("Show normals", elem);
        }
        {
            final JSlider elem = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(100 * this.opacityVertices));
            elem.setToolTipText("specify the transparency of the mesh vertices");
            elem.addChangeListener(e ->
            {
                double value = ((Number) elem.getValue()).doubleValue();
                if (value != RenderGeometry.this.opacityVertices)
                {
                    RenderGeometry.this.opacityVertices = value / 100.0;
                    Logging.info("updated vertices opacity to " + RenderGeometry.this.opacityVertices);
                    RenderGeometry.this.update();
                }
            });
            this.addObserver((o, arg) -> elem.setValue((int) Math.round(100 * RenderGeometry.this.opacityVertices)));
            this.panel.addControl("Vertices opacity", elem);
        }
        {
            final JSlider elem = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(100 * this.opacityWireframe));
            elem.setToolTipText("specify the wireframe transparency");
            elem.addChangeListener(e ->
            {
                double value = ((Number) elem.getValue()).doubleValue();
                if (value != RenderGeometry.this.opacityWireframe)
                {
                    RenderGeometry.this.opacityWireframe = value / 100.0;
                    Logging.info("updated wireframe opacity to " + RenderGeometry.this.opacityWireframe);
                    RenderGeometry.this.update();
                }
            });
            this.addObserver((o, arg) -> elem.setValue((int) Math.round(100 * RenderGeometry.this.opacityWireframe)));
            this.panel.addControl("Wireframe opacity", elem);
        }
        {
            final JSlider elem = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(100 * this.opacityMesh));
            elem.setToolTipText("specify the mesh opacity (you should sort the triangles using the Data menu for best results)");
            elem.addChangeListener(e ->
            {
                double value = ((Number) elem.getValue()).doubleValue();
                if (value != RenderGeometry.this.opacityMesh)
                {
                    RenderGeometry.this.opacityMesh = value / 100.0;
                    Logging.info("updated mesh opacity to " + RenderGeometry.this.opacityMesh);
                    RenderGeometry.this.update();
                }
            });
            this.addObserver((o, arg) -> elem.setValue((int) Math.round(100 * RenderGeometry.this.opacityMesh)));
            this.panel.addControl("Mesh opacity", elem);
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(this.widthVertices, MIN_WIDTH, MAX_WIDTH, STEP_WIDTH);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.setToolTipText("specify the point width of the vertex rendering");
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                if (value != RenderGeometry.this.widthVertices)
                {
                    RenderGeometry.this.widthVertices = value;
                    RenderGeometry.this.update();
                    Logging.info("updated mesh vertices width to " + RenderGeometry.this.widthVertices);
                }
            });
            this.addObserver((o, arg) -> elem.setValue(RenderGeometry.this.widthVertices));
            this.panel.addControl("Vertices width", elem);
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(this.widthWireframe, MIN_WIDTH, MAX_WIDTH, STEP_WIDTH);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.setToolTipText("specify the line width of the wireframe rendering");
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                if (value != RenderGeometry.this.widthWireframe)
                {
                    RenderGeometry.this.widthWireframe = value;
                    RenderGeometry.this.update();
                    Logging.info("updated mesh line width to " + RenderGeometry.this.widthWireframe);
                }
            });
            this.addObserver((o, arg) -> elem.setValue(RenderGeometry.this.widthWireframe));
            this.panel.addControl("Wireframe width", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.liftVertices));
            elem.setToolTipText("displace the vertex points off the mesh in the normal direction (avoids overlap artifact)");
            elem.addPropertyChangeListener("value", new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent e)
                {
                    double value = ((Number) elem.getValue()).doubleValue();
                    if (value != RenderGeometry.this.liftVertices)
                    {
                        RenderGeometry.this.liftVertices = value;
                        Logging.info("updated vertices lift to " + RenderGeometry.this.liftVertices);
                        RenderGeometry.this.update();
                    }
                }
            });
            this.addObserver((o, arg) -> elem.setText(String.valueOf(RenderGeometry.this.liftVertices)));
            this.panel.addControl("Vertices lift", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setToolTipText("displace the wireframe off the mesh in the normal direction (avoids overlap artifact)");
            elem.setValue(new Double(this.liftWireframe));
            elem.addPropertyChangeListener("value", e ->
            {
                double value = ((Number) elem.getValue()).doubleValue();
                if (value != RenderGeometry.this.liftWireframe)
                {
                    RenderGeometry.this.liftWireframe = value;
                    Logging.info("updated wireframe lift to " + RenderGeometry.this.liftWireframe);
                    RenderGeometry.this.update();
                }
            });
            this.addObserver((o, arg) -> elem.setText(String.valueOf(RenderGeometry.this.liftWireframe)));
            this.panel.addControl("Wireframe lift", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("render the mesh vertices with a solid color");
            elem.setSelected(this.solidVertices);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.solidVertices = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated vertex solid coloring to " + RenderGeometry.this.solidVertices);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.solidVertices));
            this.panel.addControl("Solid vertices", elem);
        }
        {
            final ColorWidget elem = new ColorWidget();
            elem.setColor(RenderGeometry.this.colorVertices);
            elem.getObservable().addObserver((o, arg) ->
            {
                RenderGeometry.this.colorVertices = elem.getColor();
                RenderGeometry.this.update();
                Logging.info("updated mesh vertices color");
            });
            this.panel.addControl("Vertices color", elem.getPanel());
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("render the mesh wireframe with a solid color");
            elem.setSelected(this.solidWireframe);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.solidWireframe = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated wireframe solid coloring to " + RenderGeometry.this.solidWireframe);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.solidWireframe));
            this.panel.addControl("Solid wireframe", elem);
        }
        {
            final ColorWidget elem = new ColorWidget();
            elem.setColor(RenderGeometry.this.colorWireframe);
            elem.getObservable().addObserver((o, arg) ->
            {
                RenderGeometry.this.colorWireframe = elem.getColor();
                RenderGeometry.this.update();
                Logging.info("updated mesh wireframe color");
            });
            this.panel.addControl("Wireframe color", elem.getPanel());
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setToolTipText("specify the lenght of the lines used to render normal vectors");
            elem.setValue(new Double(this.lengthNormal));
            elem.addPropertyChangeListener("value", e ->
            {
                double value = ((Number) elem.getValue()).doubleValue();
                if (value != RenderGeometry.this.lengthNormal)
                {
                    RenderGeometry.this.lengthNormal = value;
                    Logging.info("updated normal length to " + RenderGeometry.this.lengthNormal);
                    RenderGeometry.this.update();
                }
            });
            this.addObserver((o, arg) -> elem.setText(String.valueOf(RenderGeometry.this.lengthNormal)));
            this.panel.addControl("Normal length", elem);
        }
        {
            final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Solids, true, true);
            elem.setToolTipText("exclude some triangles from rendering using a solids data object");
            elem.addActionListener(e ->
            {
                Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                SolidsView solids = (SolidsView) entry.getValue();

                if (!JavaUtils.equals(solids, RenderGeometry.this.solidsClip))
                {
                    RenderGeometry.this.setSolidsClip(solids);
                }
            });
            panel.addControl("Solids Clip", elem);
        }
        {
            final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Affine, true, true);
            elem.setToolTipText("apply an affine transform to the mesh before rendering");
            elem.setPrototypeDisplayValue(Viewables.NONE);
            elem.addActionListener(e ->
            {
                Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                AffineView affine1 = (AffineView) entry.getValue();

                if (!entry.equals(Viewables.NONE) && !JavaUtils.equals(affine1, RenderGeometry.this.affine))
                {
                    RenderGeometry.this.setAffine(affine1);
                    RenderGeometry.this.changed();
                    Logging.info("using affine: " + entry.getName());
                }
            });
            panel.addControl("Affine", elem);
        }
        {
            JCheckBox elem = new JCheckBox();
            elem.setToolTipText("use lighting (for mesh rendering only)");
            elem.setSelected(this.lighting);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.lighting = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated lighting: " + RenderGeometry.this.lighting);
            });
            this.panel.addControl("Enable lighting", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("use smooth interpolation for shading (for mesh rendering only)");
            elem.setSelected(this.meshSmooth);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.meshSmooth = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated mesh smoothing to " + RenderGeometry.this.meshSmooth);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.meshSmooth));
            this.panel.addControl("Smooth mesh", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setToolTipText("specify the shininess of the surface (for mesh rendering with specular lighting only)");
            elem.setValue(new Double(this.meshShine));
            elem.addPropertyChangeListener("value", e ->
            {
                double value = ((Number) elem.getValue()).doubleValue();
                if (value != RenderGeometry.this.meshShine)
                {
                    RenderGeometry.this.meshShine = value;
                    Logging.info("updated mesh shine to " + RenderGeometry.this.meshShine);
                    RenderGeometry.this.update();
                }
            });
            this.addObserver((o, arg) -> elem.setText(String.valueOf(RenderGeometry.this.meshShine)));
            this.panel.addControl("Mesh shine", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("render back faces of triangles (for mesh rendering only)");
            elem.setSelected(this.showBack);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.showBack = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated mesh visibility to " + RenderGeometry.this.showBack);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.showBack));
            this.panel.addControl("Show mesh back", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("cull faces that face away from the camera (for mesh rendering only)");
            elem.setSelected(this.cullFace);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.cullFace = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated face culling to " + RenderGeometry.this.cullFace);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.cullFace));
            this.panel.addControl("Cull faces", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("flip the orientation of the face normal (for mesh rendering only)");
            elem.setSelected(this.flipNormal);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.flipNormal = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated mesh visibility to " + RenderGeometry.this.flipNormal);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.flipNormal));
            this.panel.addControl("Flip mesh normal", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("reverse the order of the face vertices (for mesh rendering only)");
            elem.setSelected(this.flipFace);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.flipFace = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated face flipping to " + RenderGeometry.this.flipFace);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.flipFace));
            this.panel.addControl("Flip mesh face", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("reverse the order of the face depth sorting (for showing multiple structures)");
            elem.setSelected(this.sortReverse);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.sortReverse = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated sort reversal to " + RenderGeometry.this.sortReverse);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.sortReverse));
            this.panel.addControl("Reverse Sorting", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("enable advanced features for mesh transparency (for mesh rendering only)");
            elem.setSelected(this.opacityAdvanced);
            elem.addItemListener(e ->
            {
                RenderGeometry.this.opacityAdvanced = e.getStateChange() == ItemEvent.SELECTED;
                RenderGeometry.this.update();
                Logging.info("updated advanced to " + RenderGeometry.this.opacityAdvanced);
            });
            this.addObserver((o, arg) -> elem.setSelected(RenderGeometry.this.opacityAdvanced));
            this.panel.addControl("Advanced Opacity", elem);
        }
    }

    public void setSolidsClip(SolidsView a)
    {
        if (this.solidsClip != null)
        {
            this.solidsClip.observable.deleteObserver(this.updateObserver);
        }

        // can be null
        this.solidsClip = a;

        if (this.solidsClip != null)
        {
            this.solidsClip.observable.addObserver(this.updateObserver);
        }

        this.changed();
    }

    public ControlPanel getPanel()
    {
        return this.panel;
    }

    public RenderGeometry changed()
    {
        this.setChanged();
        this.notifyObservers();
        this.update();

        return this;
    }

    public RenderGeometry update()
    {
        this.updateParent.run();

        return this;
    }

    public void setAffine(AffineView a)
    {
        if (this.affine != null)
        {
            this.affine.observable.deleteObserver(this.updateObserver);
        }

        // can be null
        this.affine = a;

        if (this.affine != null)
        {
            this.affine.observable.addObserver(this.updateObserver);
        }

        this.changed();
    }

    public RenderGeometry setMeshSmooth(boolean v)
    {
        this.meshSmooth = v;
        this.changed();
        return this;
    }

    public RenderGeometry setName(String v)
    {
        this.name = v;
        this.changed();
        return this;
    }

    public RenderGeometry setInclude(String v)
    {
        this.include = v;
        this.changed();
        return this;
    }

    public RenderGeometry setExclude(String v)
    {
        this.exclude = v;
        this.changed();
        return this;
    }

    public RenderGeometry setShowMesh(boolean v)
    {
        this.showMesh = v;
        this.changed();
        return this;
    }

    public RenderGeometry setShowBack(boolean v)
    {
        this.showBack = v;
        this.changed();
        return this;
    }

    public RenderGeometry setMeshWireframe(boolean v)
    {
        this.showWireframe = v;
        this.changed();
        return this;
    }

    public RenderGeometry setMeshNormals(boolean v)
    {
        this.showNormals = v;
        this.changed();
        return this;
    }

    public RenderGeometry setWidthWireframe(int v)
    {
        this.widthWireframe = v;
        this.changed();
        return this;
    }

    public RenderGeometry setMeshShine(double v)
    {
        this.meshShine = v;
        this.changed();
        return this;
    }

    public RenderGeometry setOpactiyMesh(double v)
    {
        this.opacityMesh = v;
        this.changed();
        return this;
    }

    public RenderGeometry setOpacity(boolean v)
    {
        this.opacityAdvanced = v;
        this.changed();
        return this;
    }

    public RenderGeometry setColorWireframe(Color v)
    {
        this.colorWireframe = v;
        this.changed();
        return this;
    }

    public RenderGeometry setCoord(String v)
    {
        this.coord = v;
        this.changed();
        return this;
    }

    public void render(GL2 gl, Mesh mesh)
    {
        render(gl, mesh, false);
    }

    public void render(GL2 gl, Mesh mesh, boolean sort)
    {
        if (this.showMesh)
        {
            if (this.showBack)
            {
                renderMesh(gl, mesh, true, sort);
            }

            renderMesh(gl, mesh, false, sort);
        }

        if (this.showVertices)
        {
            renderVertices(gl, mesh);
        }

        if (this.showWireframe)
        {
            renderWireframe(gl, mesh);
        }

        if (this.showNormals)
        {
            renderNormals(gl, mesh);
        }
    }

    private void renderMesh(GL2 gl, Mesh mesh, boolean back, boolean sort)
    {
        Global.assume(mesh.vattr.has(Mesh.NORMAL), "mesh must have normals");

        if (this.lighting)
        {
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
        }
        else
        {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisable(GL2.GL_COLOR_MATERIAL);
        }

        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, new float[]{1f, 1f, 1f, 1f}, 0);
        gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, new float[]{(float) this.meshShine}, 0);

        if (this.meshSmooth)
        {
            gl.glShadeModel(GL2.GL_SMOOTH);
        }
        else
        {
            gl.glShadeModel(GL2.GL_FLAT);
        }

        boolean coloring = mesh.vattr.has(Mesh.COLOR);

        if (this.cullFace)
        {
            gl.glEnable(GL2.GL_CULL_FACE);
        }
        else
        {
            gl.glDisable(GL2.GL_CULL_FACE);
        }
        gl.glCullFace(GL2.GL_BACK);

        if (this.opacityAdvanced && this.opacityMesh < 0.99)
        {
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL2.GL_ALPHA_TEST);
            gl.glAlphaFunc(GL2.GL_GREATER, 0.1f);
        }

        Affine aff = this.affine == null ? null : this.affine.getData();
        Affine jac = aff == null ? null : aff.jac();

        Set<Integer> includeIdx = Sets.newHashSet(CliUtils.parseWhich(this.include));
        Set<Integer> excludeIdx = Sets.newHashSet(CliUtils.parseWhich(this.exclude));

        int[] perm = null;
        Face[] faces = new Face[mesh.graph.numFace()];

        if (sort)
        {
            Vect eye = Viewer.getInstance().gui.canvas.render3D.eye;
            Vect look = Viewer.getInstance().gui.canvas.render3D.look;
            double[] dists = new double[mesh.graph.numFace()];

            int idx = 0;
            for (Face face : mesh.graph.faces())
            {
                Vect a = mesh.vattr.get(face.getA(), this.coord);
                Vect b = mesh.vattr.get(face.getB(), this.coord);
                Vect c = mesh.vattr.get(face.getC(), this.coord);

                Vect p = a.plus(b).plus(c).times(1.0 / 3.0);
                double dist = eye.minus(p).dot(look);

                dists[idx] = dist;
                faces[idx] = face;
                idx += 1;
            }

            perm = MathUtils.permutation(dists);
        }
        else
        {
            perm = new int[mesh.graph.numFace()];

            int idx = 0;
            for (Face face : mesh.graph.faces())
            {
                faces[idx] = face;
                perm[idx] = idx;
                idx += 1;
            }
        }

        gl.glBegin(GL2.GL_TRIANGLES);
        for (int i = 0; i < perm.length; i++)
        {
            Face f = this.sortReverse ? faces[perm[perm.length - 1 - i]] : faces[perm[i]];

            Vertex va = f.getA();
            Vertex vb = f.getB();
            Vertex vc = f.getC();

            if (this.solidsClip != null && this.solidsClip.hasData())
            {
                Solids clip = this.solidsClip.getData();
                Vect pa = mesh.vattr.get(va, this.coord);
                Vect pb = mesh.vattr.get(vb, this.coord);
                Vect pc = mesh.vattr.get(vc, this.coord);

                if (!clip.contains(pa) || !clip.contains(pb) || !clip.contains(pc))
                {
                    continue;
                }
            }

            if (mesh.vattr.has(this.name))
            {
                int la = (int) Math.round(mesh.vattr.get(va, this.name).get(0));
                int lb = (int) Math.round(mesh.vattr.get(vb, this.name).get(0));
                int lc = (int) Math.round(mesh.vattr.get(vc, this.name).get(0));

                if (includeIdx.size() > 0)
                {
                    if (!includeIdx.contains(la) || !includeIdx.contains(lb) || !includeIdx.contains(lc))
                    {
                        continue;
                    }
                }

                if (excludeIdx.size() > 0)
                {
                    if (excludeIdx.contains(la) || excludeIdx.contains(lb) || excludeIdx.contains(lc))
                    {
                        continue;
                    }
                }
            }

            boolean flip = back ^ this.flipFace;
            for (Vertex v : new Vertex[]{va, flip ? vc : vb, flip ? vb : vc})
            {
                if (coloring && mesh.vattr.dim(Mesh.COLOR) == 4)
                {
                    Vect c = mesh.vattr.get(v, Mesh.COLOR);
                    double r = c.get(0);
                    double g = c.get(1);
                    double b = c.get(2);
                    double a = c.size() == 4 ? c.get(3) : 1.0;
                    a *= this.opacityMesh;

                    gl.glColor4d(r, g, b, a);
                }

                Vect p = mesh.vattr.get(v, this.coord);
                if (aff != null)
                {
                    p = aff.apply(p);
                }

                double px = p.get(0);
                double py = p.get(1);
                double pz = p.get(2);

                Vect n = mesh.vattr.get(v, Mesh.NORMAL);
                if (jac != null)
                {
                    n = jac.apply(n).normalize();
                }

                double nx = n.get(0);
                double ny = n.get(1);
                double nz = n.get(2);

                if (back ^ this.flipNormal)
                {
                    nx *= -1;
                    ny *= -1;
                    nz *= -1;
                }

                gl.glNormal3d(nx, ny, nz);
                gl.glVertex3d(px, py, pz);
            }
        }
        gl.glEnd();
    }

    private void renderNormals(GL2 gl, Mesh mesh)
    {
        double r = this.colorWireframe.getRed() / 255.0;
        double g = this.colorWireframe.getGreen() / 255.0;
        double b = this.colorWireframe.getBlue() / 255.0;

        gl.glColor4d(r, g, b, 1.0);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(this.widthWireframe);

        Affine aff = this.affine == null ? null : this.affine.getData();
        Affine jac = aff == null ? null : aff.jac();

        Set<Integer> includeIdx = Sets.newHashSet(CliUtils.parseWhich(this.include));
        Set<Integer> excludeIdx = Sets.newHashSet(CliUtils.parseWhich(this.exclude));

        for (Vertex v : mesh.graph.verts())
        {
            Vect p = mesh.vattr.get(v, this.coord);
            if (aff != null)
            {
                p = aff.apply(p);
            }

            if (this.solidsClip != null && this.solidsClip.hasData())
            {
                Solids clip = this.solidsClip.getData();

                if (!clip.contains(p))
                {
                    continue;
                }
            }

            if (mesh.vattr.has(this.name))
            {
                int lv = (int) Math.round(mesh.vattr.get(v, this.name).get(0));

                if (includeIdx.size() > 0)
                {
                    if (!includeIdx.contains(lv))
                    {
                        continue;
                    }
                }

                if (excludeIdx.size() > 0)
                {
                    if (excludeIdx.contains(lv))
                    {
                        continue;
                    }
                }
            }

            double px = p.get(0);
            double py = p.get(1);
            double pz = p.get(2);

            Vect n = mesh.vattr.get(v, Mesh.NORMAL).copy();
            if (jac != null)
            {
                n = jac.apply(n).normalize();
            }

            n.timesEquals(this.lengthNormal);
            double nx = n.get(0);
            double ny = n.get(1);
            double nz = n.get(2);

            if (this.flipNormal)
            {
                nx *= -1;
                ny *= -1;
                nz *= -1;
            }

            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(px, py, pz);
            gl.glVertex3d(px + nx, py + ny, pz + nz);
            gl.glEnd();
        }
    }

    private void renderVertices(GL2 gl, Mesh mesh)
    {
        boolean coloring = mesh.vattr.has(Mesh.COLOR);

        double r = this.colorVertices.getRed() / 255.0;
        double g = this.colorVertices.getGreen() / 255.0;
        double b = this.colorVertices.getBlue() / 255.0;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glPointSize(this.widthVertices);
        gl.glEnable(GL2.GL_POINT_SMOOTH);
        gl.glColor4d(r, g, b, this.opacityVertices);

        gl.glBegin(GL2.GL_POINTS);

        Affine aff = this.affine == null ? null : this.affine.getData();

        Set<Integer> includeIdx = Sets.newHashSet(CliUtils.parseWhich(this.include));
        Set<Integer> excludeIdx = Sets.newHashSet(CliUtils.parseWhich(this.exclude));

        for (Vertex v : mesh.graph.verts())
        {
            if (this.solidsClip != null && this.solidsClip.hasData())
            {
                Solids clip = this.solidsClip.getData();
                Vect p = mesh.vattr.get(v, this.coord);

                if (!clip.contains(p))
                {
                    continue;
                }
            }

            if (mesh.vattr.has(this.name))
            {
                int lv = (int) Math.round(mesh.vattr.get(v, this.name).get(0));

                if (includeIdx.size() > 0)
                {
                    if (!includeIdx.contains(lv))
                    {
                        continue;
                    }
                }

                if (excludeIdx.size() > 0)
                {
                    if (excludeIdx.contains(lv))
                    {
                        continue;
                    }
                }
            }

            Vect p = mesh.vattr.get(v, this.coord);
            if (aff != null)
            {
                p = aff.apply(p);
            }

            Vect n = mesh.vattr.get(v, Mesh.NORMAL);
            p = p.plus(this.liftVertices, n);

            if (coloring && !this.solidVertices && mesh.vattr.dim(Mesh.COLOR) == 4)
            {
                Vect c = mesh.vattr.get(v, Mesh.COLOR);
                double vr = c.get(0);
                double vg = c.get(1);
                double vb = c.get(2);
                double va = c.size() == 4 ? c.get(3) : 1.0;
                va *= this.opacityVertices;

                gl.glColor4d(vr, vg, vb, va);
            }

            double px = p.get(0);
            double py = p.get(1);
            double pz = p.get(2);
            gl.glVertex3d(px, py, pz);
        }

        gl.glEnd();
    }

    private void renderWireframe(GL2 gl, Mesh mesh)
    {
        boolean coloring = mesh.vattr.has(Mesh.COLOR);

        double r = this.colorWireframe.getRed() / 255.0;
        double g = this.colorWireframe.getGreen() / 255.0;
        double b = this.colorWireframe.getBlue() / 255.0;

        gl.glLineWidth(this.widthWireframe);
        gl.glColor4d(r, g, b, this.opacityWireframe);
        gl.glDisable(GL2.GL_LIGHTING);

        Affine aff = this.affine == null ? null : this.affine.getData();

        Set<Integer> includeIdx = Sets.newHashSet(CliUtils.parseWhich(this.include));
        Set<Integer> excludeIdx = Sets.newHashSet(CliUtils.parseWhich(this.exclude));

        for (Face f : mesh.graph.faces())
        {
            gl.glBegin(GL2.GL_LINE_LOOP);

            if (this.solidsClip != null && this.solidsClip.hasData())
            {
                Solids clip = this.solidsClip.getData();
                Vect pa = mesh.vattr.get(f.getA(), this.coord);
                Vect pb = mesh.vattr.get(f.getB(), this.coord);
                Vect pc = mesh.vattr.get(f.getC(), this.coord);

                if (!clip.contains(pa) || !clip.contains(pb) || !clip.contains(pc))
                {
                    continue;
                }
            }

            if (mesh.vattr.has(this.name))
            {
                int la = (int) Math.round(mesh.vattr.get(f.getA(), this.name).get(0));
                int lb = (int) Math.round(mesh.vattr.get(f.getB(), this.name).get(0));
                int lc = (int) Math.round(mesh.vattr.get(f.getC(), this.name).get(0));

                if (includeIdx.size() > 0)
                {
                    if (!includeIdx.contains(la) || !includeIdx.contains(lb) || !includeIdx.contains(lc))
                    {
                        continue;
                    }
                }

                if (excludeIdx.size() > 0)
                {
                    if (excludeIdx.contains(la) || excludeIdx.contains(lb) || excludeIdx.contains(lc))
                    {
                        continue;
                    }
                }
            }

            // order doesn't matter here
            for (Vertex v : new Vertex[]{f.getA(), f.getB(), f.getC()})
            {
                Vect p = mesh.vattr.get(v, this.coord);
                if (aff != null)
                {
                    p = aff.apply(p);
                }

                Vect n = mesh.vattr.get(v, Mesh.NORMAL);
                p = p.plus(this.liftWireframe, n);

                if (coloring && !this.solidWireframe && mesh.vattr.dim(Mesh.COLOR) == 4)
                {
                    Vect c = mesh.vattr.get(v, Mesh.COLOR);
                    double vr = c.get(0);
                    double vg = c.get(1);
                    double vb = c.get(2);
                    double va = c.size() == 4 ? c.get(3) : 1.0;
                    va *= this.opacityWireframe;

                    gl.glColor4d(vr, vg, vb, va);
                }

                double px = p.get(0);
                double py = p.get(1);
                double pz = p.get(2);
                gl.glVertex3d(px, py, pz);
            }

            gl.glEnd();
        }
    }

}