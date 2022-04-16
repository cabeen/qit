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
import java.awt.Color;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.data.datasets.Matrix;
import qit.data.datasets.Mesh;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MatrixSource;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.structs.Box;
import qit.math.structs.Edge;
import qit.math.structs.Interval;
import qit.math.structs.Line;
import qit.math.structs.LineIntersection;
import qit.math.structs.Plane;
import qit.math.structs.Sphere;
import qitview.main.Constants;
import qitview.main.Viewer;
import qitview.models.WorldMouse;
import qitview.models.WorldPoint;
import qitview.panels.Viewables;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicLabel;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ColorWidget;
import qitview.widgets.ControlPanel;
import qitview.widgets.SwingUtils;

public class SolidsView extends AbstractView<Solids>
{
    private enum Interaction
    {
        Add, Remove, Select, Move, Size, None
    }

    private enum Type
    {
        Sphere, Box, Plane
    }

    private enum AddType
    {
        Sphere, Box, PlaneRandom, PlanePositiveX, PlaneNegativeX, PlanePositiveY, PlaneNegativeY, PlanePositiveZ, PlaneNegativeZ, PlaneCopy, PlaneCopyFlipped
    }

    private enum Side
    {
        LowX(VectSource.create3D(-1, 0, 0)),
        HighX(VectSource.create3D(1, 0, 0)),
        LowY(VectSource.create3D(0, -1, 0)),
        HighY(VectSource.create3D(0, 1, 0)),
        LowZ(VectSource.create3D(0, 0, -1)),
        HighZ(VectSource.create3D(0, 0, 1));

        Vect dir;

        Side(Vect d)
        {
            this.dir = d;
        }
    }

    private static class SolidsPick
    {
        boolean pressed = false;
        Vect point;
        int idx;
        Type type;
        Sphere sphere;
        Box box;
        Plane plane;
    }

    private static final int MIN_WIDTH = 1;
    private static final int MAX_WIDTH = 25;
    private static final int STEP_WIDTH = 1;

    private transient SolidsPick picked = null;
    private transient boolean block = false;
    private transient long lastTime = 0;

    private transient Mesh sphereMesh;
    private transient Mesh boxMesh;

    private transient Integer blinkSphereIdx = null;
    private transient Integer blinkBoxIdx = null;
    private transient Integer blinkPlaneIdx = null;
    private transient Vect blinkPoint = null;
    private transient Vect pointA = null;
    private transient Vect pointB = null;

    private int lineWidth = 2;
    private int sphereResolution = 2;
    private double minrad = 0.5;
    private double maxrad = 1000;
    private double size = 5;
    private double opacity = 0.5f;
    private Color color = Color.WHITE;
    private AddType mode = AddType.Sphere;

    private transient Runnable updateGeometry = () ->
    {
        this.sphereMesh = MeshSource.sphere(this.sphereResolution);
        this.boxMesh = this.sphereMesh; // MeshSource.box();

        MeshUtils.computeNormals(this.sphereMesh);
        MeshUtils.computeNormals(this.boxMesh);
    };

    public SolidsView()
    {
        super();

        super.initPanel();
        this.updateGeometry.run();

        this.observable.addObserver((a, b) ->
            {
                if (this.hasData())
                {
                    this.bounds = this.getData().box();
                }
            }
        );
    }

    public SolidsView setData(Solids d)
    {
        // solids are often resized, so they shouldn't change the viewing box
        this.bounds = null;
        super.setData(d);

        return this;
    }

    @Override
    public Solids getData()
    {
        return this.data;
    }

    protected ControlPanel makeInfoControls()
    {
        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Type: ", new BasicLabel(this.toString()));
        infoPanel.addControl(" ", new BasicLabel());
        {
            final BasicLabel label = new BasicLabel("");
            infoPanel.addControl("Size: ", label);
            this.observable.addObserver((o, arg) ->
            {
                if (SolidsView.this.hasData())
                {
                    label.setText(String.valueOf(this.data.size()));
                }
                else
                {
                    label.setText("NA");
                }
            });
        }
        return infoPanel;
    }

    protected Map<String, ControlPanel> makeEditControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();
        {
            ControlPanel dataPanel = new ControlPanel();
            {
                final BasicComboBox<AddType> elem = new BasicComboBox<>();
                for (AddType ad : AddType.values())
                {
                    elem.addItem(ad);
                }
                elem.setSelectedItem(this.mode);
                elem.setToolTipText("specify which solid type you will create next)");
                elem.addActionListener(e ->
                {
                    this.mode = (AddType) elem.getSelectedItem();
                    this.updateGeometry.run();

                });
                dataPanel.addControl("Add Type", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setValue(new Double(this.size));
                elem.setToolTipText("Change the default size when creating new solids");
                elem.addPropertyChangeListener("value", e ->
                {
                    SolidsView.this.size = ((Number) elem.getValue()).doubleValue();
                    Logging.info("updated default size to " + SolidsView.this.size);
                });
                dataPanel.addControl("Size", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setValue(new Double(this.minrad));
                elem.setToolTipText("set the minimum radius for spheres (useful for avoiding accidentally shrinking it down to nothing)Lo");
                elem.addPropertyChangeListener("value", e ->
                {
                    this.minrad = ((Number) elem.getValue()).doubleValue();
                    Logging.info("updated min radius to " + this.minrad);
                });
                dataPanel.addControl("Min Radius", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setValue(new Double(this.minrad));
                elem.setToolTipText("set the minimum radius for spheres (useful for avoiding accidentally shrinking it down to nothing)Lo");
                elem.addPropertyChangeListener("value", e ->
                {
                    this.minrad = ((Number) elem.getValue()).doubleValue();
                    Logging.info("updated min radius to " + this.minrad);
                });
                dataPanel.addControl("Min Radius", elem);
            }
            {
                final BasicButton elem = new BasicButton("Flip all planes");
                elem.setToolTipText("flip the orientation of the planes in the solids (this cannot be undone)");
                elem.addActionListener(e ->
                {
                    if (this.hasData() && SwingUtils.getDecision("This will flip all planes, are you sure?"))
                    {
                        Solids data1 = this.getData();
                        for (int i = 0; i < data1.numPlanes(); i++)
                        {
                            Plane plane = data1.getPlane(i);
                            data1.setPlane(i, Plane.fromPointNormal(plane.point(), plane.normal().times(-1)));
                        }
                        this.setData(data1);
                    }
                });
                dataPanel.addControl(elem);
            }
            {
                final BasicButton elem = new BasicButton("Remove all boxes");
                elem.setToolTipText("remove all boxes from the solid (this cannot be undone)");
                elem.addActionListener(e ->
                {
                    if (this.hasData() && SwingUtils.getDecision("This will remove all boxes, are you sure?"))
                    {
                        this.getData().removeBoxes();
                        this.touchData();
                    }
                });
                dataPanel.addControl(elem);
            }
            {
                final BasicButton elem = new BasicButton("Remove all spheres");
                elem.setToolTipText("remove all spheres from the solid (this cannot be undone)");
                elem.addActionListener(e ->
                {
                    if (this.hasData() && SwingUtils.getDecision("This will remove all spheres, are you sure?"))
                    {
                        this.getData().removeSpheres();
                        this.touchData();
                    }
                });
                dataPanel.addControl(elem);
            }
            {
                final BasicButton elem = new BasicButton("Remove all planes");
                elem.setToolTipText("remove all planes from the solid (this cannot be undone)");
                elem.addActionListener(e ->
                {
                    if (this.hasData() && SwingUtils.getDecision("This will remove all planes, are you sure?"))
                    {
                        this.getData().removePlanes();
                        this.touchData();
                    }
                });
                dataPanel.addControl(elem);
            }
            {
                final BasicButton elem = new BasicButton("Remove all solids");
                elem.setToolTipText("remove everything from the solid (this cannot be undone)");
                elem.addActionListener(e ->
                {
                    if (this.hasData() && SwingUtils.getDecision("This will remove all solids, are you sure?"))
                    {
                        this.getData().removeAll();
                        this.touchData();
                    }
                });
                dataPanel.addControl(elem);
            }

            controls.put("Interaction", dataPanel);
        }
        {
            ControlPanel panel = new ControlPanel();

            String modeReplace = "Replace";
            String modeExport = "Export";

            BasicComboBox modeComboBox = new BasicComboBox();
            modeComboBox.addItem(modeReplace);
            modeComboBox.addItem(modeExport);

            panel.addControl("Mode", modeComboBox);

            {
                final BasicButton elem = new BasicButton("Copy Solids");
                elem.setToolTipText("Create a copy of this mesh");
                elem.addActionListener(e ->
                {
                    if (this.hasData())
                    {
                        Viewables.consumeSolids().accept("copy", this.getData().copy());
                    }
                });
                panel.addControl(elem);
            }

            controls.put("Processing", panel);
        }

        return controls;
    }

    protected Map<String, ControlPanel> makeRenderControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();
        {
            ControlPanel dataPanel = new ControlPanel();
            {
                final ColorWidget elem = new ColorWidget();
                elem.setColor(SolidsView.this.color);
                elem.getObservable().addObserver((o, arg) ->
                {
                    this.color = elem.getColor();
                    Logging.info("updated color");
                });
                dataPanel.addControl("Line Color", elem.getPanel());
            }
            {
                final JSlider elem = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(100 * this.opacity));
                elem.addChangeListener(e ->
                {
                    this.opacity = elem.getModel().getValue() / 100.;
                });
                dataPanel.addControl("Line Opacity", elem);
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
                });

                dataPanel.addControl("Sphere Resolution", spin);
            }

            controls.put("Rendering", dataPanel);
        }

        return controls;
    }

    private void display(GL2 gl, Plane plane)
    {
        double r = this.color.getRed() / 255.0;
        double g = this.color.getGreen() / 255.0;
        double b = this.color.getBlue() / 255.0;
        double a = this.opacity;
        gl.glColor4d(r, g, b, a);

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(this.lineWidth);

        Box box = Viewer.getInstance().gui.canvas.render3D.box;

        if (box == null)
        {
            // only draw the plane if there is a well defined scene
            return;
        }

        Vect ref = this.pointA != null ? this.pointA : box.getCenter();

        Vect point = plane.nearest(ref);
        Vect normal = plane.normal();

        Vect u = normal.perp().normalize();
        Vect v = normal.cross(u).normalize();

        double maxSize = box.getInterval(0).size();
        maxSize = Math.max(maxSize, box.getInterval(1).size());
        maxSize = Math.max(maxSize, box.getInterval(2).size());

        double radius = 0.75 * maxSize;
        int num = 64;

        Vects ps = new Vects();
        for (int i = 0; i < num; i++)
        {
            double angle = 2.0 * Math.PI * i / (num - 1d);
            double su = radius * Math.cos(angle);
            double sv = radius * Math.sin(angle);
            ps.add(point.plus(su, u).plus(sv, v));
        }

        Vect npoint = point.plus(0.25 * radius, normal);

        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(point.getX(), point.getY(), point.getZ());
        gl.glVertex3d(npoint.getX(), npoint.getY(), npoint.getZ());
        gl.glEnd();

        for (int i = 0; i < num; i++)
        {
            Vect pa = ps.get(i == 0 ? num - 1 : i - 1);
            Vect pb = ps.get(i);
            Vect pc = point;

            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(pa.getX(), pa.getY(), pa.getZ());
            gl.glVertex3d(pb.getX(), pb.getY(), pb.getZ());
            gl.glEnd();

            gl.glBegin(GL2.GL_TRIANGLES);
            gl.glVertex3d(pa.getX(), pa.getY(), pa.getZ());
            gl.glVertex3d(pb.getX(), pb.getY(), pb.getZ());
            gl.glVertex3d(pc.getX(), pc.getY(), pc.getZ());

            gl.glVertex3d(pc.getX(), pc.getY(), pc.getZ());
            gl.glVertex3d(pb.getX(), pb.getY(), pb.getZ());
            gl.glVertex3d(pa.getX(), pa.getY(), pa.getZ());
            gl.glEnd();
        }
    }

    private void display(GL2 gl, Line line)
    {
        Box box = Viewer.getInstance().gui.canvas.render3D.box;

        if (box == null)
        {
            // only draw the plane if there is a well defined scene
            return;
        }

        Vect a = null;
        Vect b = null;

        List<LineIntersection> uInts = box.intersect(line);

        for (int i = 0; i < uInts.size(); i++)
        {
            LineIntersection uInt = uInts.get(i);
            if (uInt.getAlpha() < 0)
            {
                a = uInt.getPoint();
            }
            else if (uInt.getAlpha() > 0)
            {
                b = uInt.getPoint();
            }
        }

        if (a != null && b != null)
        {
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(a.getX(), a.getY(), a.getZ());
            gl.glVertex3d(b.getX(), b.getY(), b.getZ());
            gl.glEnd();
        }
    }

    private void display(GL2 gl, Box box)
    {
        double r = this.color.getRed() / 255.0;
        double g = this.color.getGreen() / 255.0;
        double b = this.color.getBlue() / 255.0;
        double a = this.opacity;
        gl.glColor4d(r, g, b, a);

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(this.lineWidth);

        Vect min = box.getMin();
        Vect max = box.getMax();

        double startx = min.get(0);
        double endx = max.get(0);
        double starty = min.get(1);
        double endy = max.get(1);
        double startz = min.get(2);
        double endz = max.get(2);

        // one side
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(startx, starty, startz);
        gl.glVertex3d(endx, starty, startz);
        gl.glVertex3d(endx, endy, startz);
        gl.glVertex3d(startx, endy, startz);
        gl.glVertex3d(startx, starty, startz);
        gl.glEnd();

        // other side
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(startx, starty, endz);
        gl.glVertex3d(endx, starty, endz);
        gl.glVertex3d(endx, endy, endz);
        gl.glVertex3d(startx, endy, endz);
        gl.glVertex3d(startx, starty, endz);
        gl.glEnd();

        // connections
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(startx, starty, startz);
        gl.glVertex3d(startx, starty, endz);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(endx, starty, startz);
        gl.glVertex3d(endx, starty, endz);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(startx, endy, startz);
        gl.glVertex3d(startx, endy, endz);
        gl.glEnd();

        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(endx, endy, startz);
        gl.glVertex3d(endx, endy, endz);
        gl.glEnd();
    }

    private void display(GL2 gl, Sphere sphere)
    {
        double r = this.color.getRed() / 255.0;
        double g = this.color.getGreen() / 255.0;
        double b = this.color.getBlue() / 255.0;
        double a = this.opacity;
        gl.glColor4d(r, g, b, a);

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(this.lineWidth);

        Vect cen = sphere.getCenter();
        double rad = sphere.getRadius();

        gl.glColor4d(r, g, b, a);
        for (Edge edge : this.sphereMesh.graph.edges())
        {
            Vect curr = this.sphereMesh.vattr.get(edge.getA(), Mesh.COORD).times(rad).plus(cen);
            Vect next = this.sphereMesh.vattr.get(edge.getB(), Mesh.COORD).times(rad).plus(cen);

            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(curr.get(0), curr.get(1), curr.get(2));
            gl.glVertex3d(next.get(0), next.get(1), next.get(2));
            gl.glEnd();
        }
    }

    public void display(GL2 gl)
    {
        boolean blink = (System.currentTimeMillis() % 500L) < 250L;

        if (this.data == null)
        {
            return;
        }

        for (int i = 0; i < this.data.numBoxes(); i++)
        {
            if (this.blinkBoxIdx != null && this.blinkBoxIdx == i && blink)
            {
                continue;
            }

            display(gl, this.getData().getBox(i));
        }

        for (int i = 0; i < this.data.numSpheres(); i++)
        {
            if (this.blinkSphereIdx != null && this.blinkSphereIdx == i && blink)
            {
                continue;
            }

            display(gl, this.getData().getSphere(i));
        }

        for (int i = 0; i < this.data.numPlanes(); i++)
        {
            if (this.blinkPlaneIdx != null && this.blinkPlaneIdx == i && blink)
            {
                continue;
            }

            display(gl, this.getData().getPlane(i));
        }

        if (this.blinkPoint != null && !blink)
        {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor3d(255, 0, 0);
            gl.glEnable(GL2.GL_POINT_SMOOTH);
            gl.glPointSize(20);
            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex3d(this.blinkPoint.getX(), this.blinkPoint.getY(), this.blinkPoint.getZ());
            gl.glEnd();
        }

        if (this.pointA != null && this.pointB != null)
        {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor3d(255, 0, 0);
            gl.glEnable(GL2.GL_POINT_SMOOTH);

            gl.glPointSize(20);
            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex3d(this.pointA.getX(), this.pointA.getY(), this.pointA.getZ());
            gl.glEnd();

            gl.glPointSize(10);
            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex3d(this.pointB.getX(), this.pointB.getY(), this.pointB.getZ());
            gl.glEnd();

            gl.glPointSize(1);

            gl.glLineWidth(5);
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(this.pointA.getX(), this.pointA.getY(), this.pointA.getZ());
            gl.glVertex3d(this.pointB.getX(), this.pointB.getY(), this.pointB.getZ());
            gl.glEnd();
        }
    }

    private Pair<Integer, LineIntersection> closest(Line line, List<Pair<Integer, List<LineIntersection>>> hits)
    {
        Double minDistOuter = null;
        Pair<Integer, LineIntersection> minHit = null;
        for (Pair<Integer, List<LineIntersection>> hit : hits)
        {
            Double minDist = null;
            LineIntersection minInter = null;
            for (LineIntersection inter : hit.b)
            {
                double dist = line.getPoint().dist2(inter.getPoint());
                if (minDist == null || dist < minDist)
                {
                    minDist = dist;
                    minInter = inter;
                }
            }

            if (minInter != null && (minDistOuter == null || minDist < minDistOuter))
            {
                minHit = Pair.of(hit.a, minInter);
            }
        }

        return minHit;
    }

    private SolidsPick hit(Line pressLine)
    {
        Pair<Integer, LineIntersection> hitSphere = closest(pressLine, this.data.selectSpheres(pressLine));
        Pair<Integer, LineIntersection> hitBox = closest(pressLine, this.data.selectBoxes(pressLine));
        Pair<Integer, LineIntersection> hitPlane = closest(pressLine, this.data.selectPlanes(pressLine));

        int count = (hitSphere != null ? 1 : 0) + (hitBox != null ? 1 : 0) + (hitPlane != null ? 1 : 0);
        if (count > 1)
        {
            // test if the box or sphere is closer
            double distSphere = hitSphere != null ? hitSphere.b.getPoint().dist2(pressLine.getPoint()) : Double.MAX_VALUE;
            double distBox = hitBox != null ? hitBox.b.getPoint().dist2(pressLine.getPoint()) : Double.MAX_VALUE;
            double distPlane = hitPlane != null ? hitPlane.b.getPoint().dist2(pressLine.getPoint()) : Double.MAX_VALUE;

            if (distSphere < distBox && distSphere < distPlane)
            {
                hitBox = null;
                hitPlane = null;
            }
            else if (distBox < distSphere && distBox < distPlane)
            {
                hitSphere = null;
                hitPlane = null;
            }
            else
            {
                hitBox = null;
                hitSphere = null;
            }
        }

        if (hitSphere != null && hitBox == null && hitPlane == null)
        {
            SolidsPick pickSphere = new SolidsPick();
            pickSphere.idx = hitSphere.a;
            pickSphere.point = hitSphere.b.getPoint();
            pickSphere.type = Type.Sphere;
            pickSphere.sphere = this.getData().getSphere(hitSphere.a);
            return pickSphere;
        }
        else if (hitSphere == null && hitBox != null && hitPlane == null)
        {
            SolidsPick pickBox = new SolidsPick();
            pickBox.idx = hitBox.a;
            pickBox.point = hitBox.b.getPoint();
            pickBox.type = Type.Box;
            pickBox.box = this.getData().getBox(hitBox.a);
            return pickBox;
        }
        else if (hitSphere == null && hitBox == null && hitPlane != null)
        {
            SolidsPick pickPlane = new SolidsPick();
            pickPlane.idx = hitPlane.a;
            pickPlane.point = hitPlane.b.getPoint();
            pickPlane.type = Type.Plane;
            pickPlane.plane = this.getData().getPlane(hitPlane.a);
            return pickPlane;
        }
        else
        {
            // either no hit or an ambiguous one, so skip it
            return null;
        }
    }

    public Double dist(WorldMouse mouse)
    {
        if (!this.hasData() || mouse.press == null)
        {
            return null;
        }

        Line pressLine = Line.fromTwoPoints(mouse.press.point, mouse.press.hit);

        Pair<Integer, LineIntersection> hitSphere = closest(pressLine, this.data.selectSpheres(pressLine));
        Pair<Integer, LineIntersection> hitBox = closest(pressLine, this.data.selectBoxes(pressLine));

        Double dist = null;

        if (hitSphere != null)
        {
            dist = hitSphere.b.getPoint().dist(mouse.press.point);
        }

        if (hitBox != null)
        {
            double d = hitBox.b.getPoint().dist(mouse.press.point);
            dist = dist == null ? d : Math.min(dist, d);
        }

        return dist;
    }

    private Interaction parse(WorldMouse mouse, String mode)
    {
        for (Interaction i : Interaction.values())
        {
            if (Interaction.Add.toString().equals(mode) && this.block)
            {
                return Interaction.Select;
            }

            if (Interaction.Size.toString().equals(mode) && this.picked == null)
            {
                return Interaction.Select;
            }

            if (Interaction.Move.toString().equals(mode) && this.picked == null)
            {
                return Interaction.Select;
            }

            if (i.toString().equals(mode))
            {
                return i;
            }
        }

        if (Constants.INTERACTION_ROTATE.equals(mode))
        {
            if (!mouse.pick)
            {
                return Interaction.None;
            }

            if (mouse.control && mouse.shift)
            {
                return Interaction.Remove;
            }

            if (this.picked == null)
            {
                if (mouse.control && !mouse.shift && !this.block)
                {
                    return Interaction.Add;
                }
                else
                {
                    return Interaction.Select;
                }
            }
            else
            {
                if (!mouse.control && mouse.shift)
                {
                    return Interaction.Size;
                }
                else
                {
                    return Interaction.Move;
                }
            }
        }

        return Interaction.Move;
    }

    private void handleAdd(WorldMouse mouse)
    {
        this.picked = null;
        this.blinkBoxIdx = null;
        this.blinkSphereIdx = null;
        this.blinkPlaneIdx = null;
        this.blinkPoint = null;
        this.pointA = null;
        this.pointB = null;

        // only add solids inside the scene box (if there is one)
        Box box = Viewer.getInstance().gui.canvas.render3D.box;
        if (box == null)
        {
            return;
        }

        WorldPoint world = mouse.press == null ? mouse.current : mouse.press;

        Vect normal = VectSource.randomUnit();
        switch (this.mode)
        {
            case PlanePositiveX:
                normal = VectSource.create(-1, 0, 0);
                break;
            case PlaneNegativeX:
                normal = VectSource.create(1, 0, 0);
                break;
            case PlanePositiveY:
                normal = VectSource.create(0, -1, 0);
                break;
            case PlaneNegativeY:
                normal = VectSource.create(0, 1, 0);
                break;
            case PlanePositiveZ:
                normal = VectSource.create(0, 0, -1);
                break;
            case PlaneNegativeZ:
                normal = VectSource.create(0, 0, 1);
                break;
            case PlaneCopy:
                if (this.hasData() && this.getData().numPlanes() > 0)
                {
                    normal = this.getData().getPlane(this.getData().numPlanes() - 1).normal();
                }
                break;
            case PlaneCopyFlipped:
                if (this.hasData() && this.getData().numPlanes() > 0)
                {
                    normal = this.getData().getPlane(this.getData().numPlanes() - 1).normal().times(-1);
                }
                break;
        }

        switch (this.mode)
        {
            case Sphere:
            {
                Sphere nsphere = new Sphere(world.hit, this.size);
                if (box.contains(nsphere.getCenter()))
                {
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().control.setStatusMessage("added sphere");
                        Solids solids = this.hasData() ? this.getData() : new Solids();
                        solids.addSphere(nsphere);
                        this.setData(solids);

                        this.blinkPoint = null;

                        this.block = true;
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will add the blinking sphere");
                        this.blinkPoint = nsphere.getCenter();
                    }
                }
                break;
            }
            case Box:
            {
                Box nbox = new Sphere(world.hit, this.size).box();
                if (box.contains(nbox.getCenter()))
                {
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().control.setStatusMessage("added box");
                        Solids solids = this.hasData() ? this.getData() : new Solids();
                        solids.addBox(nbox);
                        this.setData(solids);

                        this.blinkPoint = null;

                        this.block = true;
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will add the blinking box");
                        this.blinkPoint = nbox.getCenter();
                    }
                }
                break;
            }
            case PlanePositiveX:
                // pass through
            case PlaneNegativeX:
                // pass through
            case PlanePositiveY:
                // pass through
            case PlaneNegativeY:
                // pass through
            case PlanePositiveZ:
                // pass through
            case PlaneNegativeZ:
                // pass through
            case PlaneRandom:
                // pass through
            case PlaneCopy:
                // pass through
            case PlaneCopyFlipped:
                // pass through

            {
                Plane nplane = Plane.fromPointNormal(world.hit, normal);
                if (box.contains(world.hit))
                {
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().control.setStatusMessage("added plane");
                        Solids solids = this.hasData() ? this.getData() : new Solids();
                        solids.addPlane(nplane);
                        this.setData(solids);

                        this.blinkPoint = null;

                        this.block = true;
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will add a plane at the blinking point");
                        this.blinkPoint = world.hit;
                    }
                }
                break;
            }
        }
    }

    private void handleRemove(WorldMouse mouse)
    {
        this.picked = null;
        this.blinkPoint = null;
        this.pointA = null;
        this.pointB = null;

        WorldPoint world = mouse.press == null ? mouse.current : mouse.press;
        Line pressLine = Line.fromTwoPoints(world.point, world.hit);
        SolidsPick hit = hit(pressLine);

        if (hit != null)
        {
            switch (hit.type)
            {
                case Sphere:
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().control.setStatusMessage("removed sphere");
                        this.getData().removeSphere(hit.idx);
                        this.blinkSphereIdx = null;
                        this.blinkBoxIdx = null;
                        this.blinkPlaneIdx = null;
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will remove the blinking sphere");
                        this.blinkSphereIdx = hit.idx;
                        this.blinkBoxIdx = null;
                        this.blinkPlaneIdx = null;
                    }
                    break;
                case Box:
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().control.setStatusMessage("removed box");
                        this.getData().removeBox(hit.idx);
                        this.blinkSphereIdx = null;
                        this.blinkBoxIdx = null;
                        this.blinkPlaneIdx = null;
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will remove the blinking box");
                        this.blinkBoxIdx = hit.idx;
                        this.blinkSphereIdx = null;
                        this.blinkPlaneIdx = null;
                    }
                    break;
                case Plane:
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().control.setStatusMessage("removed plane");
                        this.getData().removePlane(hit.idx);
                        this.blinkSphereIdx = null;
                        this.blinkBoxIdx = null;
                        this.blinkPlaneIdx = null;
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will remove the blinking plane");
                        this.blinkPlaneIdx = hit.idx;
                        this.blinkSphereIdx = null;
                        this.blinkBoxIdx = null;
                    }
                    break;
            }

            this.touchData();
            this.block = false;
        }
    }

    private void handleSelect(WorldMouse mouse)
    {
        this.picked = null;
        this.blinkBoxIdx = null;
        this.blinkSphereIdx = null;
        this.blinkPlaneIdx = null;
        this.blinkPoint = null;
        this.pointA = null;
        this.pointB = null;

        WorldPoint world = mouse.press != null ? mouse.press : mouse.current;
        Line pressLine = Line.fromTwoPoints(world.point, world.hit);
        SolidsPick hit = hit(pressLine);

        if (hit != null)
        {
            switch (hit.type)
            {
                case Sphere:
                    this.picked = hit;
                    this.picked.pressed = mouse.press != null;
                    break;
                case Box:
                    this.picked = hit;
                    this.picked.pressed = mouse.press != null;
                    break;
                case Plane:
                    this.picked = hit;
                    this.picked.pressed = mouse.press != null;
                    break;
            }
        }
    }

    private void handleMove(WorldMouse mouse)
    {
        this.blinkBoxIdx = null;
        this.blinkSphereIdx = null;
        this.blinkPoint = null;

        try
        {
            if (this.picked == null)
            {
                Logging.info("warning: no pick found");
                return;
            }

            WorldPoint world = mouse.press != null ? mouse.press : mouse.current;
            Line line = Line.fromTwoPoints(world.point, world.hit);

            Vect start = picked.point;
            Plane pickPlane = Plane.fromPointNormal(start, line.getDir().normalize());
            Line dragLine = Line.fromTwoPoints(mouse.current.point, mouse.current.hit);
            List<LineIntersection> inters = pickPlane.intersect(dragLine);

            if (inters.size() != 1)
            {
                // this happens when the mouse moves outside the window
                return;
            }

            Vect end = inters.get(0).getPoint();
            Vect delta = end.minus(start);

            switch (this.picked.type)
            {
                case Sphere:
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().gui.setStatusMessage("moving sphere");
                        Sphere psphere = this.picked.sphere;
                        Sphere nsphere = new Sphere(psphere.getCenter().plus(delta), psphere.getRadius());
                        this.pointA = nsphere.getCenter();
                        this.pointB = this.pointA;
                        this.getData().setSphere(this.picked.idx, nsphere);
                        this.setData(this.getData());
                        this.touchData();
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will move sphere");
                        this.pointA = this.getData().getSphere(this.picked.idx).getCenter();
                        this.pointB = this.pointA;
                    }
                    break;
                case Box:
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().gui.setStatusMessage("moving box");
                        Box nbox = this.picked.box.shift(delta);
                        this.pointA = nbox.getCenter();
                        this.pointB = this.pointA;
                        this.getData().setBox(this.picked.idx, nbox);
                        this.setData(this.getData());
                        this.touchData();
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will move box");
                        this.pointA = this.getData().getBox(this.picked.idx).getCenter();
                        this.pointB = this.pointA;
                    }
                    break;

                case Plane:
                    Plane plane = this.picked.plane;
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().gui.setStatusMessage("moving plane");

                        Vect point = plane.point();
                        Vect normal = plane.normal();

                        double dot = normal.dot(delta);
                        Vect npoint = point.plus(dot, normal);
                        Plane nplane = Plane.fromPointNormal(npoint, normal);

                        this.pointA = nplane.nearest(this.picked.point);
                        this.pointB = this.pointA;
                        this.getData().setPlane(this.picked.idx, nplane);
                        this.setData(this.getData());
                        this.touchData();
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will move plane in normal direction");
                        this.pointA = plane.nearest(this.picked.point);
                        this.pointB = this.pointA;
                    }
                    break;
            }
        }
        catch (RuntimeException e)
        {
            // this catches errors from moving the mouse offscreen
        }
    }

    private void handleSize(WorldMouse mouse)
    {
        try
        {
            this.blinkBoxIdx = null;
            this.blinkSphereIdx = null;
            this.blinkPoint = null;

            if (mouse.current == null)
            {
                this.picked = null;
                return;
            }

            if (this.picked == null)
            {
                Logging.info("warning: no pick found");
                return;
            }

            WorldPoint world = mouse.press != null ? mouse.press : mouse.current;
            Line line = Line.fromTwoPoints(world.point, world.hit);

            Vect start = mouse.press != null ? this.picked.point : mouse.current.hit;
            Plane pickPlane = Plane.fromPointNormal(start, line.getDir().normalize());
            Line dragLine = Line.fromTwoPoints(mouse.current.point, mouse.current.hit);
            List<LineIntersection> inters = pickPlane.intersect(dragLine);

            if (inters.size() != 1)
            {
                // this might catch an edge case
                Logging.info("warning: no plane intersection");
                return;
            }

            Vect end = inters.get(0).getPoint();
            Vect delta = end.minus(start);

            switch (this.picked.type)
            {
                case Sphere:
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().gui.setStatusMessage("scaling sphere");
                        Sphere psphere = this.picked.sphere;

                        Vect centerSphere = pickPlane.nearest(this.picked.sphere.getCenter());
                        double rad = this.picked.sphere.getRadius();
                        double fac = rad / start.minus(centerSphere).norm();
                        double frad = fac * end.minus(centerSphere).norm();
                        double nrad = Math.min(this.maxrad, Math.max(this.minrad, frad));

                        Sphere nsphere = new Sphere(psphere.getCenter(), nrad);
                        this.getData().setSphere(this.picked.idx, nsphere);

                        this.pointA = psphere.getCenter();
                        this.pointB = mouse.current.hit;

                        this.touchData();
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will scaleCamera sphere");
                        Vect pA = this.picked.sphere.getCenter();
                        Vect pB = mouse.current.hit;

                        this.pointA = pA;
                        this.pointB = pB;
                    }
                    break;
                case Box:
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().gui.setStatusMessage("scaling box");
                        Vect centerBox = pickPlane.nearest(this.picked.box.getCenter());
                        Vect cdelta = start.minus(centerBox);

                        double maxDot = 0;
                        Side maxSide = null;

                        for (Side side : Side.values())
                        {
                            double dot = side.dir.dot(cdelta);
                            if (dot > maxDot || maxSide == null)
                            {
                                maxDot = dot;
                                maxSide = side;
                            }
                        }

                        if (maxSide == null)
                        {
                            return;
                        }

                        try
                        {
                            double change = maxSide.dir.dot(delta);
                            Interval x = this.picked.box.range(0);
                            Interval y = this.picked.box.range(1);
                            Interval z = this.picked.box.range(2);

                            switch (maxSide)
                            {
                                case LowX:
                                    x = new Interval(x.getMin() - change, x.getMax());
                                    this.pointB = VectSource.create3D(x.getMin(), y.getHalf(), z.getHalf());
                                    break;
                                case HighX:
                                    x = new Interval(x.getMin(), x.getMax() + change);
                                    this.pointB = VectSource.create3D(x.getMax(), y.getHalf(), z.getHalf());
                                    break;
                                case LowY:
                                    y = new Interval(y.getMin() - change, y.getMax());
                                    this.pointB = VectSource.create3D(x.getHalf(), y.getMin(), z.getHalf());
                                    break;
                                case HighY:
                                    y = new Interval(y.getMin(), y.getMax() + change);
                                    this.pointB = VectSource.create3D(x.getHalf(), y.getMax(), z.getHalf());
                                    break;
                                case LowZ:
                                    z = new Interval(z.getMin() - change, z.getMax());
                                    this.pointB = VectSource.create3D(x.getHalf(), y.getHalf(), z.getMin());
                                    break;
                                case HighZ:
                                    z = new Interval(z.getMin(), z.getMax() + change);
                                    this.pointB = VectSource.create3D(x.getHalf(), y.getHalf(), z.getMax());
                                    break;
                            }

                            Box nbox = new Box(new Interval[]{x, y, z});
                            this.pointA = nbox.getCenter();

                            // compute how to resize box
                            this.getData().setBox(this.picked.idx, new Box(new Interval[]{x, y, z}));
                            this.touchData();
                        }
                        catch (Exception e)
                        {
                            // this occurs when the user tries to turn the box inside out
                            Logging.info("cannot shink box any farther");
                        }
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will scaleCamera the side of a box");
                        Vect centerBox = pickPlane.nearest(this.picked.box.getCenter());
                        Vect cdelta = start.minus(centerBox);

                        double maxDot = 0;
                        Side maxSide = null;

                        for (Side side : Side.values())
                        {
                            double dot = side.dir.dot(cdelta);
                            if (dot > maxDot || maxSide == null)
                            {
                                maxDot = dot;
                                maxSide = side;
                            }
                        }

                        if (maxSide == null)
                        {
                            return;
                        }

                        try
                        {
                            Interval x = this.picked.box.range(0);
                            Interval y = this.picked.box.range(1);
                            Interval z = this.picked.box.range(2);

                            switch (maxSide)
                            {
                                case LowX:
                                    this.pointB = VectSource.create3D(x.getMin(), y.getHalf(), z.getHalf());
                                    break;
                                case HighX:
                                    this.pointB = VectSource.create3D(x.getMax(), y.getHalf(), z.getHalf());
                                    break;
                                case LowY:
                                    this.pointB = VectSource.create3D(x.getHalf(), y.getMin(), z.getHalf());
                                    break;
                                case HighY:
                                    this.pointB = VectSource.create3D(x.getHalf(), y.getMax(), z.getHalf());
                                    break;
                                case LowZ:
                                    this.pointB = VectSource.create3D(x.getHalf(), y.getHalf(), z.getMin());
                                    break;
                                case HighZ:
                                    this.pointB = VectSource.create3D(x.getHalf(), y.getHalf(), z.getMax());
                                    break;
                            }

                            this.pointA = this.picked.box.getCenter();
                        }
                        catch (Exception e)
                        {
                            // this occurs when the user tries to turn the box inside out
                            Logging.info("cannot shink box any farther");
                        }
                    }
                    break;
                case Plane:
                    Plane plane = this.picked.plane;
                    if (mouse.press != null)
                    {
                        Viewer.getInstance().gui.setStatusMessage("rotating plane");

                        int pressX = mouse.press.screen.x;
                        int pressY = mouse.press.screen.y;
                        int currentX = mouse.current.screen.x;
                        int currentY = mouse.current.screen.y;

                        double deltaX = 0.01 * Math.abs(currentX - pressX);
                        deltaX = Math.copySign(deltaX, currentX - pressX);
                        double deltaY = 0.01 * Math.abs(currentY - pressY);
                        deltaY = Math.copySign(deltaY, currentY - pressY);

                        if (deltaX == 0 && deltaY == 0)
                        {
                            // this is important for avoiding reslicing when this.picked.voxel is not up to date
                            return;
                        }

                        if (this.lastTime == mouse.time)
                        {
                            // skip duplicate drag events
                            return;
                        }

                        this.lastTime = mouse.time;

                        Vect up = Viewer.getInstance().gui.canvas.render3D.up;
                        Vect look = Viewer.getInstance().gui.canvas.render3D.look;
                        Vect cross = look.cross(up);

                        Matrix rotUp = MatrixSource.rotation(up, deltaX);
                        Matrix rotCross = MatrixSource.rotation(cross, deltaY);

                        Vect point = plane.nearest(this.picked.point);
                        Vect normal = rotCross.times(rotUp.times(plane.normal()));

                        Plane nplane = Plane.fromPointNormal(point, normal);
                        this.getData().setPlane(this.picked.idx, nplane);

                        this.pointA = nplane.nearest(this.picked.point);
                        this.pointB = this.pointA.plus(nplane.normal());

                        this.touchData();
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("clicking will rotate plane");
                        this.pointA = plane.nearest(this.picked.point);
                        this.pointB = this.pointA;
                    }
                    break;
            }
        }
        catch (RuntimeException e)
        {
            // this happens if there's a geometric problem with picking
        }
    }

    private void clearSelection()
    {
        this.picked = null;
        this.pointA = null;
        this.pointB = null;
        this.blinkBoxIdx = null;
        this.blinkSphereIdx = null;
        this.blinkPlaneIdx = null;
        this.blinkPoint = null;
    }

    public List<String> modes()
    {
        List<String> out = Lists.newArrayList();
        out.add(Interaction.Move.toString());
        out.add(Interaction.Size.toString());
        out.add(Interaction.Add.toString());
        out.add(Interaction.Remove.toString());
        return out;
    }

    public void handle(WorldMouse mouse, String mode)
    {
        if (!mouse.pick)
        {
            if (this.picked != null)
            {
                Viewer.getInstance().gui.setStatusMessage(" ");
            }

            clearSelection();
        }

        if (mouse.press == null)
        {
            this.block = false;
        }

        if (mouse.press != null && this.picked != null && !this.picked.pressed)
        {
            this.picked = null;
        }

        if (mouse.press == null && this.picked != null && this.picked.pressed)
        {
            this.picked = null;
        }

        if (!this.hasData() || mouse.current == null)
        {
            return;
        }

        switch (parse(mouse, mode))
        {
            case Add:
                handleAdd(mouse);
                return;
            case Remove:
                handleRemove(mouse);
                return;
            case Select:
                handleSelect(mouse);
                return;
            case Move:
                handleMove(mouse);
                return;
            case Size:
                handleSize(mouse);
                return;
            default:
                clearSelection();
        }

        return;
    }
}