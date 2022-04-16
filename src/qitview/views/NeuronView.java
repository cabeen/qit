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

package qitview.views;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jogamp.opengl.GL2;
import qit.base.Logging;
import qit.data.datasets.Curves;
import qit.data.datasets.Mesh;
import qit.data.datasets.Neuron;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.modules.curves.CurvesTubes;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.MeshUtils;
import qit.math.structs.Edge;
import qit.math.structs.Face;
import qit.math.structs.Vertex;
import qitview.models.WorldMouse;
import qitview.render.RenderGeometry;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicLabel;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ColorWidget;
import qitview.widgets.ControlPanel;

import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;
import java.awt.Color;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class NeuronView extends AbstractView<Neuron>
{
    private static final int MIN_WIDTH = 1;
    private static final int MAX_WIDTH = 25;
    private static final int STEP_WIDTH = 1;

    private enum RenderMode
    {
        None, Point, Wire, Solid, Sculpt
    }

    private enum TrunkColorMode
    {
        Solid, DEC, Segment, Tree
    }

    private RenderMode rootRenderMode = RenderMode.Sculpt;
    private RenderMode forkRenderMode = RenderMode.Sculpt;
    private RenderMode leafRenderMode = RenderMode.Sculpt;
    private RenderMode trunkRenderMode = RenderMode.Sculpt;
    private TrunkColorMode trunkColorMode = TrunkColorMode.Solid;
    private double rootScale = 1.0;
    private double forkScale = 1.0;
    private double leafScale = 1.0;
    private double trunkScale = 1.0;
    private Color rootColor = Color.ORANGE;
    private Color forkColor = Color.CYAN;
    private Color leafColor = Color.YELLOW;
    private Color trunkColor = Color.WHITE;
    private int lineWidth = 2;
    private int sphereResolution = 2;
    private int tubeResolution = 5;
    private double opacity = 1.0f;

    private transient Integer list = null;
    private transient boolean update = false;

    private transient RenderGeometry render = null;
    private transient CurvesTubes tuber = new CurvesTubes();

    private transient Mesh sphereMesh;
    private transient Mesh boxMesh;

    private transient Vects roots = null;
    private transient Vects forks = null;
    private transient Vects leaves = null;
    private transient Curves trunks = null;

    private transient Runnable updateGeometry = () ->
    {
        this.sphereMesh = MeshSource.sphere(this.sphereResolution);
        this.boxMesh = this.sphereMesh; // MeshSource.box();

        MeshUtils.computeNormals(this.sphereMesh);
        MeshUtils.computeNormals(this.boxMesh);
    };

    private transient Runnable updateCurveColors = () ->
    {
        if (this.data != null)
        {
            if (!this.trunks.has(Curves.TANGENT))
            {
                CurvesUtils.attrSetTangent(this.trunks);
            }

            this.trunks.add(Curves.COLOR, VectSource.create3D());

            Function<Integer,Vect> colormap = (idx) ->
            {
                float hue = (float) (idx / (Math.PI * 2.0));
                float sat = 0.5f;
                float val = 1.0f;
                Color color = Color.getHSBColor(hue, sat, val);

                return VectSource.create3(color);
            };

            switch (this.trunkColorMode)
            {
                case DEC:
                    for (Curves.Curve curve : this.trunks)
                    {
                        for (int i = 0; i < curve.size(); i++)
                        {
                            Vect t = curve.get(Curves.TANGENT, i);
                            curve.set(Curves.COLOR, i, t.abs());
                        }
                    }
                    break;

                case Segment:
                    for (Curves.Curve curve : this.trunks)
                    {
                        for (int i = 0; i < curve.size(); i++)
                        {
                            int idx = (int) curve.get(Neuron.ATTR_SEGMENT, i).get(0);
                            curve.set(Curves.COLOR, i, colormap.apply(idx));
                        }
                    }
                    break;

                case Tree:
                    for (Curves.Curve curve : this.trunks)
                    {
                        for (int i = 0; i < curve.size(); i++)
                        {
                            int idx = (int) curve.get(Neuron.ATTR_TREE, i).get(0);
                            curve.set(Curves.COLOR, i, colormap.apply(idx));
                        }
                    }
                    break;

                default:
                    double r = this.trunkColor.getRed() / 255.0;
                    double g = this.trunkColor.getGreen() / 255.0;
                    double b = this.trunkColor.getBlue() / 255.0;
                    CurvesUtils.attrSetAll(this.trunks, Curves.COLOR, VectSource.create3D(r, g, b));
                    break;
            }
        }
    };

    public NeuronView()
    {
        super();

        this.render = new RenderGeometry(() -> { this.update = true; });
        this.render.setMeshSmooth(true);

        super.initPanel();
        this.updateGeometry.run();
        this.updateCurveColors.run();
        this.observable.addObserver((a, b) ->
                {
                    if (this.hasData())
                    {
                        this.bounds = this.getData().box();
                    }
                }
        );
    }

    public NeuronView setData(Neuron d)
    {
        this.roots = d == null ? null : d.toVectsRoots();
        this.forks = d == null ? null : d.toVectsForks();
        this.leaves = d == null ? null : d.toVectsLeaves();
        this.trunks = d == null ? null : d.toCurves();
        this.bounds = d == null ? null : d.box();

        super.setData(d);

        this.updateCurveColors.run();
        this.update = true;

        return this;
    }

    @Override
    public Neuron getData()
    {
        return this.data;
    }

    protected ControlPanel makeInfoControls()
    {
        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Type: ", new BasicLabel(this.toString()));
        infoPanel.addControl(" ", new BasicLabel());
        {
            final BasicLabel roots = new BasicLabel("");
            final BasicLabel forks = new BasicLabel("");
            final BasicLabel leaves = new BasicLabel("");
            final BasicLabel nodes = new BasicLabel("");

            infoPanel.addControl("Roots: ", roots);
            infoPanel.addControl("Forks: ", forks);
            infoPanel.addControl("Leaves: ", leaves);
            infoPanel.addControl("Nodes: ", nodes);

            this.observable.addObserver((o, arg) ->
            {
                if (NeuronView.this.hasData() && this.roots != null)
                {
                    roots.setText(String.valueOf(this.roots.size()));
                    forks.setText(String.valueOf(this.forks.size()));
                    leaves.setText(String.valueOf(this.leaves.size()));
                    nodes.setText(String.valueOf(this.getData().nodes.size()));
                }
                else
                {
                    roots.setText("NA");
                    forks.setText("NA");
                    leaves.setText("NA");
                    nodes.setText("NA");
                }
            });
        }
        return infoPanel;
    }

    protected Map<String, ControlPanel> makeEditControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();

        return controls;
    }

    protected Map<String, ControlPanel> makeRenderControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();
        {
            ControlPanel dataPanel = new ControlPanel();
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change the radius used for ball rendering");
                elem.setValue(new Float(this.rootScale));
                elem.addPropertyChangeListener("value", e ->
                {
                    double value = ((Number) elem.getValue()).doubleValue();
                    if (value != this.rootScale)
                    {
                        this.rootScale = value;
                        this.update = true;
                    }
                });
                dataPanel.addControl("Root Scale", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change the radius used for ball rendering");
                elem.setValue(new Float(this.forkScale));
                elem.addPropertyChangeListener("value", e ->
                {
                    double value = ((Number) elem.getValue()).doubleValue();
                    if (value != this.forkScale)
                    {
                        this.forkScale = value;
                        this.update = true;
                    }
                });
                dataPanel.addControl("Fork Scale", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change the radius used for ball rendering");
                elem.setValue(new Float(this.leafScale));
                elem.addPropertyChangeListener("value", e ->
                {
                    double value = ((Number) elem.getValue()).doubleValue();
                    if (value != this.leafScale)
                    {
                        this.leafScale = value;
                        this.update = true;
                    }
                });
                dataPanel.addControl("Leaf Scale", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change the radius used for ball rendering");
                elem.setValue(new Float(this.trunkScale));
                elem.addPropertyChangeListener("value", e ->
                {
                    double value = ((Number) elem.getValue()).doubleValue();
                    if (value != this.trunkScale)
                    {
                        this.trunkScale = value;
                        this.update = true;
                    }
                });
                dataPanel.addControl("Trunk Scale", elem);
            }
            {
                final ColorWidget elem = new ColorWidget();
                elem.setColor(NeuronView.this.rootColor);
                elem.getObservable().addObserver((o, arg) ->
                {
                    this.rootColor = elem.getColor();
                    this.update = true;
                });
                dataPanel.addControl("Root Color", elem.getPanel());
            }
            {
                final ColorWidget elem = new ColorWidget();
                elem.setColor(NeuronView.this.forkColor);
                elem.getObservable().addObserver((o, arg) ->
                {
                    this.forkColor = elem.getColor();
                    this.update = true;
                });
                dataPanel.addControl("Fork Color", elem.getPanel());
            }
            {
                final ColorWidget elem = new ColorWidget();
                elem.setColor(NeuronView.this.leafColor);
                elem.getObservable().addObserver((o, arg) ->
                {
                    this.leafColor = elem.getColor();
                    this.update = true;
                });
                dataPanel.addControl("Leaf Color", elem.getPanel());
            }
            {
                final ColorWidget elem = new ColorWidget();
                elem.setColor(NeuronView.this.trunkColor);
                elem.getObservable().addObserver((o, arg) ->
                {
                    this.trunkColor = elem.getColor();
                    this.updateCurveColors.run();
                    this.update = true;
                });
                dataPanel.addControl("Trunk Color", elem.getPanel());
            }
            {
                final BasicComboBox<RenderMode> elem = new BasicComboBox<>();
                for (RenderMode mode : RenderMode.values())
                {
                    elem.addItem(mode);
                }
                elem.setSelectedItem(this.rootRenderMode);
                elem.setToolTipText("specify the rendering mode for roots");
                elem.addActionListener(e ->
                {
                    RenderMode value = (RenderMode) elem.getSelectedItem();
                    if (value != this.rootRenderMode)
                    {
                        this.rootRenderMode = value;
                        this.update = true;
                    }
                });
                dataPanel.addControl("Root Render Mode", elem);
            }
            {
                final BasicComboBox<RenderMode> elem = new BasicComboBox<>();
                for (RenderMode mode : RenderMode.values())
                {
                    elem.addItem(mode);
                }
                elem.setSelectedItem(this.forkRenderMode);
                elem.setToolTipText("specify the rendering mode for forks");
                elem.addActionListener(e ->
                {
                    RenderMode value = (RenderMode) elem.getSelectedItem();
                    if (value != this.forkRenderMode)
                    {
                        this.forkRenderMode = value;
                        this.update = true;
                    }
                });
                dataPanel.addControl("Fork Render Mode", elem);
            }
            {
                final BasicComboBox<RenderMode> elem = new BasicComboBox<>();
                for (RenderMode mode : RenderMode.values())
                {
                    elem.addItem(mode);
                }
                elem.setSelectedItem(this.leafRenderMode);
                elem.setToolTipText("specify the rendering mode for leafs");
                elem.addActionListener(e ->
                {
                    RenderMode value = (RenderMode) elem.getSelectedItem();
                    if (value != this.leafRenderMode)
                    {
                        this.leafRenderMode = value;
                        this.update = true;
                    }
                });
                dataPanel.addControl("Leaf Render Mode", elem);
            }
            {
                final BasicComboBox<RenderMode> elem = new BasicComboBox<>();
                for (RenderMode mode : RenderMode.values())
                {
                    elem.addItem(mode);
                }
                elem.setSelectedItem(this.trunkRenderMode);
                elem.setToolTipText("specify the rendering mode for trunks");
                elem.addActionListener(e ->
                {
                    RenderMode value = (RenderMode) elem.getSelectedItem();
                    if (value != this.trunkRenderMode)
                    {
                        this.trunkRenderMode = value;
                        this.update = true;
                    }
                });
                dataPanel.addControl("Trunk Render Mode", elem);
            }
            {
                final BasicComboBox<TrunkColorMode> elem = new BasicComboBox<>();
                for (TrunkColorMode ad : TrunkColorMode.values())
                {
                    elem.addItem(ad);
                }
                elem.setSelectedItem(this.trunkColorMode);
                elem.setToolTipText("specify which solid type you will create next)");
                elem.addActionListener(e ->
                {
                    TrunkColorMode value = (TrunkColorMode) elem.getSelectedItem();
                    if (value != this.trunkColorMode)
                    {
                        this.trunkColorMode = value;
                        this.updateCurveColors.run();
                        this.update = true;
                    }
                });
                dataPanel.addControl("Trunk Color Mode", elem);
            }
            {
                final JSlider elem = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(100 * this.opacity));
                elem.addChangeListener(e ->
                {
                    this.opacity = elem.getModel().getValue() / 100.;
                    this.update = true;
                });
                dataPanel.addControl("Opacity", elem);
            }
            {
                SpinnerNumberModel model = new SpinnerNumberModel(this.lineWidth, MIN_WIDTH, MAX_WIDTH, STEP_WIDTH);
                final BasicSpinner elem = new BasicSpinner(model);
                elem.addChangeListener(e ->
                {
                    int value = Integer.valueOf(elem.getValue().toString());
                    if (value != this.lineWidth)
                    {
                        this.lineWidth = value;
                        this.update = true;
                    }
                });
                dataPanel.addControl("Line Width", elem);
            }
            {
                final BasicSpinner spin = new BasicSpinner(new SpinnerNumberModel(this.sphereResolution, 1, 10, 1));
                spin.setToolTipText("specify a resolution for sphere mesh rendering");

                spin.addChangeListener(e ->
                {
                    int value = Integer.valueOf(spin.getValue().toString());
                    this.sphereResolution = value;
                    this.updateGeometry.run();
                    this.update = true;
                });

                dataPanel.addControl("Sphere Resolution", spin);
            }
            {
                final BasicSpinner spin = new BasicSpinner(new SpinnerNumberModel(this.tubeResolution, 5, 20, 1));
                spin.setToolTipText("specify a resolution for tube mesh rendering");

                spin.addChangeListener(e ->
                {
                    int value = Integer.valueOf(spin.getValue().toString());
                    this.tubeResolution = value;
                    this.updateGeometry.run();
                    this.update = true;
                });

                dataPanel.addControl("Tube Resolution", spin);
            }

            controls.put("Rendering", dataPanel);
        }

        return controls;
    }

    private void display(GL2 gl, Vect point, double radius, Color color, RenderMode mode)
    {
        double r = color.getRed() / 255.0;
        double g = color.getGreen() / 255.0;
        double b = color.getBlue() / 255.0;
        double a = this.opacity;
        gl.glColor4d(r, g, b, a);

        switch (mode)
        {
            case Point:
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glPointSize((int) radius);
                gl.glEnable(GL2.GL_POINT_SMOOTH);

                gl.glBegin(GL2.GL_POINTS);
                gl.glVertex3d(point.getX(), point.getY(), point.getZ());
                gl.glEnd();
                break;

            case Wire:
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth(this.lineWidth);

                gl.glColor4d(r, g, b, a);
                for (Edge edge : this.sphereMesh.graph.edges())
                {
                    Vect curr = this.sphereMesh.vattr.get(edge.getA(), Mesh.COORD).times(radius).plus(point);
                    Vect next = this.sphereMesh.vattr.get(edge.getB(), Mesh.COORD).times(radius).plus(point);

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(curr.get(0), curr.get(1), curr.get(2));
                    gl.glVertex3d(next.get(0), next.get(1), next.get(2));
                    gl.glEnd();
                }
                break;

            case Solid:
            case Sculpt:
                gl.glEnable(GL2.GL_LIGHTING);
                gl.glEnable(GL2.GL_COLOR_MATERIAL);

                gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
                gl.glShadeModel(GL2.GL_SMOOTH);

                gl.glEnable(GL2.GL_COLOR);
                gl.glEnable(GL2.GL_BLEND);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
                gl.glEnable(GL2.GL_ALPHA_TEST);
                gl.glAlphaFunc(GL2.GL_ALWAYS, 0.0f);

                gl.glBegin(GL2.GL_TRIANGLES);
                for (Face f : this.sphereMesh.graph.faces())
                {
                    Vertex va = f.getA();
                    Vertex vb = f.getB();
                    Vertex vc = f.getC();

                    for (Vertex v : new Vertex[]{va, vb, vc})
                    {
                        double myrad = mode == RenderMode.Sculpt ? radius * point.getW() : radius;
                        Vect p = this.sphereMesh.vattr.get(v, Mesh.COORD).times(myrad).plus(point);

                        double px = p.get(0);
                        double py = p.get(1);
                        double pz = p.get(2);

                        Vect n = this.sphereMesh.vattr.get(v, Mesh.NORMAL);

                        double nx = n.get(0);
                        double ny = n.get(1);
                        double nz = n.get(2);

                        gl.glNormal3d(nx, ny, nz);
                        gl.glVertex3d(px, py, pz);
                    }
                }
                gl.glEnd();
                break;
        }
    }

    private void display(GL2 gl, Curves.Curve curve)
    {
        switch (this.trunkRenderMode)
        {
            case Point:
                for (Vect node : curve)
                {
                    this.display(gl, node, this.trunkScale, this.trunkColor, RenderMode.Point);
                }
                break;

            case Wire:
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth((int) this.trunkScale);

                gl.glBegin(GL2.GL_LINE_STRIP);
                for (int j = 0; j < curve.size(); j++)
                {
                    Vect pos = curve.get(Curves.COORD, j);

                    double x = pos.get(0);
                    double y = pos.get(1);
                    double z = pos.get(2);

                    gl.glVertex3d(x, y, z);

                    if (curve.has(Curves.COLOR))
                    {
                        Vect color = curve.get(Curves.COLOR, j);

                        double r = color.getX();
                        double g = color.getY();
                        double b = color.getZ();
                        double a = this.opacity;

                        gl.glColor4d(r, g, b, a);
                    }
                }
                gl.glEnd();
                break;

            case Solid:
            case Sculpt:
                this.tuber.dthick = this.trunkScale;
                this.tuber.smooth = false;
                this.tuber.thick = Neuron.ATTR_RADIUS;
                this.tuber.fthick = this.trunkScale;
                this.tuber.noThick = this.trunkRenderMode != RenderMode.Sculpt;
                this.tuber.resolution = this.tubeResolution;
                Mesh mesh = this.tuber.single(curve);

                gl.glEnable(GL2.GL_LIGHTING);
                gl.glEnable(GL2.GL_COLOR_MATERIAL);

                gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE);
                gl.glShadeModel(GL2.GL_SMOOTH);

                gl.glEnable(GL2.GL_COLOR);
                gl.glEnable(GL2.GL_BLEND);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
                gl.glEnable(GL2.GL_ALPHA_TEST);
                gl.glAlphaFunc(GL2.GL_ALWAYS, 0.0f);

                gl.glBegin(GL2.GL_TRIANGLES);
                for (Face f : mesh.graph.faces())
                {
                    Vertex va = f.getA();
                    Vertex vb = f.getB();
                    Vertex vc = f.getC();

                    for (Vertex v : new Vertex[]{va, vb, vc})
                    {
                        Vect p = mesh.vattr.get(v, Mesh.COORD);
                        Vect n = mesh.vattr.get(v, Mesh.NORMAL);
                        Vect c = mesh.vattr.get(v, Mesh.COLOR);

                        gl.glColor4d(c.getX(), c.getY(), c.getZ(), 1.0);
                        gl.glNormal3d(n.getX(), n.getY(), n.getZ());
                        gl.glVertex3d(p.getX(), p.getY(), p.getZ());
                    }
                }
                gl.glEnd();
                break;
        }
    }

    public void display(GL2 gl)
    {
        if (this.data == null)
        {
            return;
        }

        if (this.update && this.list != null)
        {
            Logging.info(String.format("deleting neuron display list for %s", this.getName()));
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

                Logging.info(String.format("creating neuron display list for %s", this.getName()));
                gl.glNewList(idx, GL2.GL_COMPILE);

                for (Curves.Curve curve : this.trunks)
                {
                    this.display(gl, curve);
                }

                for (Vect root : this.roots)
                {
                    this.display(gl, root, this.rootScale, this.rootColor, this.rootRenderMode);
                }

                for (Vect fork : this.forks)
                {
                    this.display(gl, fork, this.forkScale, this.forkColor, this.forkRenderMode);
                }

                for (Vect leaf : this.leaves)
                {
                    this.display(gl, leaf, this.leafScale, this.leafColor, this.leafRenderMode);
                }

                gl.glEndList();
            }
        }

        if (this.list != null)
        {
            gl.glCallList(this.list);
        }
    }

    public List<String> modes()
    {
        List<String> out = Lists.newArrayList();
        return out;
    }

    public void handle(WorldMouse mouse, String mode)
    {
        return;
    }
}
