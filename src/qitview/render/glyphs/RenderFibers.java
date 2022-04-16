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

import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;

import qit.base.Logging;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.models.Fibers;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.colormaps.ColormapDiscrete;
import qit.math.utils.colormaps.ColormapScalar;
import qit.math.utils.colormaps.ColormapSolid;
import qit.math.utils.colormaps.ColormapVector;
import qitview.main.Viewer;
import qitview.render.RenderGeometry;
import qitview.render.RenderGlyph;
import qitview.render.RenderUtils;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ColormapState;
import qitview.widgets.ControlPanel;
import qitview.widgets.UnitMapDialog;

public class RenderFibers extends RenderGlyph
{
    private static final String ATTR_NONE = "none";
    private static final String ATTR_BASE = "base";
    private static final String ATTR_FISO = "fiso";
    private static final String ATTR_FRAC = "frac";
    private static final String ATTR_STAT = "stat";
    private static final String ATTR_LINE = "line";
    private static final String ATTR_DIFF = "diff";
    private static final String[] ATTRS = {ATTR_NONE, ATTR_BASE, ATTR_FISO, ATTR_FRAC, ATTR_STAT, ATTR_LINE, ATTR_DIFF};

    private transient String thickSelection = ATTR_FRAC;
    private transient UnitMapDialog thickTransfer = new UnitMapDialog("Stick Thickness", 0, 1);
    private transient VectFunction thickFunction = null;

    private transient String lengthSelection = ATTR_NONE;
    private transient UnitMapDialog lengthTransfer = new UnitMapDialog("Stick Length", 0, 1);
    private transient VectFunction lengthFunction = null;

    private transient String alphaSelection = ATTR_NONE;
    private transient UnitMapDialog alphaTransfer = new UnitMapDialog("Stick Alpha", 0, 1);
    private transient VectFunction alphaFunction = null;
    private transient Mesh ballMesh = null;

    private transient VectFunction coloring = null;
    private BasicComboBox<String> comboAttribute = new BasicComboBox<>();
    private BasicComboBox<String> comboColorType = new BasicComboBox<>();
    private BasicComboBox<ColormapSolid> comboColorSolid;
    private BasicComboBox<ColormapDiscrete> comboColorDiscrete;
    private BasicComboBox<ColormapScalar> comboColorScalar;
    private BasicComboBox<ColormapVector> comboColorVector;

    private double minstat = 0.00;
    private double minfrac = 0.05;
    private double minball = 0.05;
    private double diffscale = 1.0;
    private int width = 2;
    private int resolution = 12;
    private int subdiv = 2;
    private double radius = 0.3f;
    private double thick = 0.3f;
    private double length = 0.75;
    private double wash = 0.0f;
    private boolean glyphs = false;
    private boolean ball = false;
    private boolean sticks = true;

    protected transient boolean updateColoring = false;
    protected transient Runnable updateParent;
    protected transient RenderGeometry render;

    public RenderFibers(Runnable p)
    {
        this.updateParent = p;
        this.render = new RenderGeometry(this.updateParent);

        this.ballMesh = MeshSource.sphere(this.subdiv);
        MeshUtils.computeNormals(this.ballMesh);

        this.thickTransfer.addObserver((o, arg) -> RenderFibers.this.updateThickness());
        this.lengthTransfer.addObserver((o, arg) -> RenderFibers.this.updateLength());
        this.alphaTransfer.addObserver((o, arg) -> RenderFibers.this.updateAlpha());
    }

    @Override
    public ControlPanel getPanel()
    {
        ControlPanel panel = new ControlPanel();
        {
            JCheckBox cb = new JCheckBox();
            cb.addItemListener(e ->
            {
                RenderFibers.this.glyphs = e.getStateChange() == ItemEvent.SELECTED;
                RenderFibers.this.updateParent.run();
            });
            panel.addControl("Glyphs", cb);
        }
        {
            JCheckBox cb = new JCheckBox();
            cb.addItemListener(e ->
            {
                RenderFibers.this.sticks = e.getStateChange() == ItemEvent.SELECTED;
                RenderFibers.this.updateParent.run();
            });
            panel.addControl("Sticks", cb);
        }
        {
            JCheckBox cb = new JCheckBox();
            cb.addItemListener(e ->
            {
                RenderFibers.this.ball = e.getStateChange() == ItemEvent.SELECTED;
                RenderFibers.this.updateParent.run();
            });
            panel.addControl("Ball", cb);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.minfrac));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nthresh = ((Number) tfield.getValue()).doubleValue();
                if (nthresh != RenderFibers.this.minfrac)
                {
                    RenderFibers.this.minfrac = nthresh;
                    RenderFibers.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thresh to " + RenderFibers.this.minfrac);
                }
            });
            panel.addControl("Min Stick Frac", tfield);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.minstat));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nthresh = ((Number) tfield.getValue()).doubleValue();
                if (nthresh != RenderFibers.this.minstat)
                {
                    RenderFibers.this.minstat = nthresh;
                    RenderFibers.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thresh to " + RenderFibers.this.minstat);
                }
            });
            panel.addControl("Min Stick Stat", tfield);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.minball));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nthresh = ((Number) tfield.getValue()).doubleValue();
                if (nthresh != RenderFibers.this.minball)
                {
                    RenderFibers.this.minball = nthresh;
                    RenderFibers.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thresh to " + RenderFibers.this.minball);
                }
            });
            panel.addControl("Min Ball Frac", tfield);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.diffscale));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nthresh = ((Number) tfield.getValue()).doubleValue();
                if (nthresh != RenderFibers.this.diffscale)
                {
                    RenderFibers.this.diffscale = nthresh;
                    RenderFibers.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated diffscale to " + RenderFibers.this.diffscale);
                }
            });
            panel.addControl("Diff Scale", tfield);
        }
        {
            final BasicFormattedTextField sfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            sfield.setValue(new Double(this.length));
            sfield.addPropertyChangeListener("value", e ->
            {
                double nlength = ((Number) sfield.getValue()).doubleValue();
                if (nlength != RenderFibers.this.length)
                {
                    RenderFibers.this.length = nlength;
                    RenderFibers.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated length to " + RenderFibers.this.length);
                }
            });
            panel.addControl("Stick Length", sfield);
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(this.width, 0, 100, 1);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                if (value != RenderFibers.this.width)
                {
                    RenderFibers.this.width = value;
                    RenderFibers.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated width to " + RenderFibers.this.width);
                }
            });
            panel.addControl("Width", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.resolution, 6, 200, 1));
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                RenderFibers.this.resolution = value;
                Viewer.getInstance().control.setStatusMessage("updated resolution: " + RenderFibers.this.resolution);
                RenderFibers.this.updateParent.run();
            });
            panel.addControl("Resolution", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.subdiv, 0, 5, 1));
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                RenderFibers.this.subdiv = value;
                RenderFibers.this.ballMesh = MeshSource.sphere(RenderFibers.this.subdiv);
                MeshUtils.computeNormals(RenderFibers.this.ballMesh);

                Viewer.getInstance().control.setStatusMessage("updated subdivisions: " + RenderFibers.this.subdiv);
                RenderFibers.this.updateParent.run();
            });
            panel.addControl("Subdivisions", elem);
        }
        {
            final BasicFormattedTextField hfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            hfield.setValue(new Double(this.radius));
            hfield.addPropertyChangeListener("value", e ->
            {
                double nradius = ((Number) hfield.getValue()).doubleValue();
                if (nradius != RenderFibers.this.radius)
                {
                    RenderFibers.this.radius = nradius;
                    RenderFibers.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated radius to " + RenderFibers.this.radius);
                }
            });
            panel.addControl("Ball Radius", hfield);
        }
        {
            final BasicFormattedTextField hfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            hfield.setValue(new Double(this.thick));
            hfield.addPropertyChangeListener("value", e ->
            {
                double nthick = ((Number) hfield.getValue()).doubleValue();
                if (nthick != RenderFibers.this.thick)
                {
                    RenderFibers.this.thick = nthick;
                    RenderFibers.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thick to " + RenderFibers.this.thick);
                }
            });
            panel.addControl("Thickness", hfield);
        }
        {
            final BasicFormattedTextField wfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            wfield.setValue(new Double(this.wash));
            wfield.addPropertyChangeListener("value", e ->
            {
                double nwash = ((Number) wfield.getValue()).doubleValue();
                if (nwash != RenderFibers.this.wash)
                {
                    RenderFibers.this.wash = nwash;
                    RenderFibers.this.updateColoring();
                    Viewer.getInstance().control.setStatusMessage("updated wash to " + RenderFibers.this.wash);
                }
            });
            panel.addControl("Wash", wfield);
        }
        {
            this.comboColorType.addItem(ColormapState.SOLID);
            this.comboColorType.addItem(ColormapState.DISCRETE);
            this.comboColorType.addItem(ColormapState.SCALAR);
            this.comboColorType.addItem(ColormapState.VECTOR);

            final ColormapState cms = Viewer.getInstance().colormaps;
            this.comboColorSolid = cms.getComboSolid();
            this.comboColorDiscrete = cms.getComboDiscrete();
            this.comboColorScalar = cms.getComboScalar();
            this.comboColorVector = cms.getComboVector();

            this.comboColorSolid.setVisible(false);
            this.comboColorDiscrete.setVisible(false);
            this.comboColorScalar.setVisible(false);
            this.comboColorVector.setVisible(true);

            this.comboColorType.setSelectedItem(ColormapState.VECTOR);
            this.comboColorSolid.setSelectedIndex(0);
            this.comboColorDiscrete.setSelectedIndex(0);
            this.comboColorScalar.setSelectedIndex(0);
            this.comboColorVector.setSelectedIndex(0);

            final ActionListener listener = e -> RenderFibers.this.updateColoring();

            {
                this.comboAttribute.addItem(ATTR_LINE);
                for (String attr : ATTRS)
                {
                    if (!Mesh.COLOR.equals(attr) && !Mesh.OPACITY.equals(attr))
                    {
                        this.comboAttribute.addItem(attr);
                    }
                }
                this.comboAttribute.setSelectedItem(ATTR_LINE);
                this.comboAttribute.addActionListener(listener);
                RenderFibers.this.comboColorType.setSelectedItem(ColormapState.VECTOR);
            }

            this.comboColorSolid.addActionListener(listener);
            this.comboColorDiscrete.addActionListener(listener);
            this.comboColorScalar.addActionListener(listener);
            this.comboColorVector.addActionListener(listener);

            this.updateColoring();

            this.comboColorType.addActionListener(e ->
            {
                String selection = (String) RenderFibers.this.comboColorType.getSelectedItem();

                RenderFibers.this.comboColorSolid.setVisible(false);
                RenderFibers.this.comboColorDiscrete.setVisible(false);
                RenderFibers.this.comboColorScalar.setVisible(false);
                RenderFibers.this.comboColorVector.setVisible(false);

                switch (selection)
                {
                    case ColormapState.SOLID:
                        RenderFibers.this.comboColorSolid.setVisible(true);
                        break;
                    case ColormapState.DISCRETE:
                        RenderFibers.this.comboColorDiscrete.setVisible(true);
                        break;
                    case ColormapState.SCALAR:
                        RenderFibers.this.comboColorScalar.setVisible(true);
                        break;
                    case ColormapState.VECTOR:
                        RenderFibers.this.comboColorVector.setVisible(true);
                        break;
                }
            });

            Viewer.getInstance().colormaps.getObservable().addObserver((o, arg) -> RenderFibers.this.updateColoring());

            final JPanel combos = new JPanel();
            combos.add(this.comboColorSolid);
            combos.add(this.comboColorDiscrete);
            combos.add(this.comboColorScalar);
            combos.add(this.comboColorVector);

            panel.addControl("Color Attribute", this.comboAttribute);
            panel.addControl("Color Type", this.comboColorType);
            panel.addControl("Color Map", combos);
        }
        {
            final BasicComboBox<String> cb = new BasicComboBox<>();
            for (String attr : ATTRS)
            {
                cb.addItem(attr);
            }
            final ActionListener listener = e ->
            {
                final String selected = cb.getItemAt(cb.getSelectedIndex());
                if (selected == null)
                {
                    return;
                }

                RenderFibers.this.thickSelection = selected;
                RenderFibers.this.updateThickness();
            };
            cb.setSelectedItem(RenderFibers.this.thickSelection);
            listener.actionPerformed(null);
            cb.addActionListener(listener);
            panel.addControl("Thickness", cb);
        }
        {
            final BasicButton button = new BasicButton("Edit Thickness Transfer...");
            {
                button.addActionListener(arg0 -> RenderFibers.this.thickTransfer.show());
            }

            panel.addControl(button);
        }
        {
            final BasicComboBox<String> cb = new BasicComboBox<>();
            for (String attr : ATTRS)
            {
                cb.addItem(attr);
            }
            final ActionListener listener = e ->
            {
                final String selected = cb.getItemAt(cb.getSelectedIndex());
                if (selected == null)
                {
                    return;
                }

                RenderFibers.this.lengthSelection = selected;
                RenderFibers.this.updateLength();
            };
            cb.addActionListener(listener);
            panel.addControl("Length", cb);
        }
        {
            final BasicButton button = new BasicButton("Edit Length Transfer...");
            button.addActionListener(arg0 -> RenderFibers.this.lengthTransfer.show());
            panel.addControl(button);
        }
        {
            final BasicComboBox<String> cb = new BasicComboBox<>();
            for (String attr : ATTRS)
            {
                cb.addItem(attr);
            }
            final ActionListener listener = e ->
            {
                final String selected = cb.getItemAt(cb.getSelectedIndex());
                if (selected == null)
                {
                    return;
                }

                RenderFibers.this.alphaSelection = selected;
                RenderFibers.this.updateAlpha();
            };
            cb.addActionListener(listener);
            panel.addControl("Alpha", cb);
        }
        {
            final BasicButton button = new BasicButton("Edit Alpha Transfer...");
            button.addActionListener(arg0 -> RenderFibers.this.alphaTransfer.show());
            panel.addControl(button);
        }
        {
            final ControlPanel renderPanel = this.render.getPanel();
            final BasicButton button = new BasicButton("Edit Mesh Rendering...");
            button.addActionListener(arg0 -> JOptionPane.showMessageDialog(null, renderPanel, "Render Settings", JOptionPane.QUESTION_MESSAGE));
            panel.addControl(button);
        }

        return panel;
    }

    private void updateThickness()
    {
        if (!RenderFibers.this.thickSelection.equals(ATTR_NONE))
        {
            RenderFibers.this.thickFunction = RenderFibers.this.thickTransfer.toFunction();
        }

        RenderFibers.this.updateParent.run();
    }

    private void updateLength()
    {
        if (!RenderFibers.this.lengthSelection.equals(ATTR_NONE))
        {
            RenderFibers.this.lengthFunction = RenderFibers.this.lengthTransfer.toFunction();
        }

        RenderFibers.this.updateParent.run();
    }

    private void updateAlpha()
    {
        if (!RenderFibers.this.alphaSelection.equals(ATTR_NONE))
        {
            RenderFibers.this.alphaFunction = RenderFibers.this.alphaTransfer.toFunction();
        }

        RenderFibers.this.updateParent.run();
    }

    private void updateColoring()
    {
        this.updateColoring = true;
    }

    private void updateColoringDirect()
    {
        // look up coloring function
        String attribute = (String) this.comboAttribute.getSelectedItem();
        String ctype = (String) this.comboColorType.getSelectedItem();

        switch (ctype)
        {
            case ColormapState.SOLID:
                this.coloring = ((ColormapSolid) this.comboColorSolid.getSelectedItem()).getFunction();
                break;
            case ColormapState.DISCRETE:
                this.coloring = ((ColormapDiscrete) this.comboColorDiscrete.getSelectedItem()).getFunction();
                break;
            case ColormapState.SCALAR:
                this.coloring = ((ColormapScalar) this.comboColorScalar.getSelectedItem()).getFunction();
                break;
            case ColormapState.VECTOR:
                this.coloring = ((ColormapVector) this.comboColorVector.getSelectedItem()).getFunction();
                break;
            default:
                Logging.error("invalid colortype");
        }

        int dattr = attribute.equals(ATTR_LINE) ? 3 : 1;
        int dcolor = coloring.getDimIn();
        if (attribute == null || dcolor != dattr)
        {
            this.coloring = VectFunctionSource.constant(dattr, VectSource.createND(dcolor, 1)).compose(coloring);
        }

        // apply wash
        this.coloring = coloring.compose(VectFunctionSource.wash(this.wash));

        // signal a change
        this.updateParent.run();
    }

    private void renderLines(GL2 gl, Vect coord, Fibers fibers)
    {
        for (int k = 0; k < fibers.size(); k++)
        {
            if (fibers.getFrac(k) >= this.minfrac && fibers.getStat(k) >= this.minstat)
            {
                Vect frac = VectSource.create1D(fibers.getFrac(k));
                Vect stat = VectSource.create1D(fibers.getStat(k));
                Vect line = fibers.getLine(k);

                Map<String, Vect> attrs = Maps.newHashMap();
                attrs.put(ATTR_NONE, VectSource.create1D(1.0));
                attrs.put(ATTR_BASE, VectSource.create1D(fibers.getBaseline()));
                attrs.put(ATTR_FISO, VectSource.create1D(fibers.getFracIso()));
                attrs.put(ATTR_DIFF, VectSource.create1D(fibers.getDiffusivity()));
                attrs.put(ATTR_FRAC, frac);
                attrs.put(ATTR_STAT, stat);
                attrs.put(ATTR_LINE, line);

                String attribute = (String) this.comboAttribute.getSelectedItem();
                Vect color = this.coloring == null ? VectFunctionSource.rgb().apply(line) : this.coloring.apply(attrs.get(attribute));
                double alpha = this.alphaFunction == null ? 1.0 : this.alphaFunction.apply(attrs.get(this.alphaSelection)).get(0);
                color.set(3, alpha);

                double flength = this.length;
                if (this.lengthFunction != null)
                {
                    flength *= this.lengthFunction.apply(attrs.get(this.lengthSelection)).get(0);
                }

                double fthick = this.thick * this.width;
                if (this.thickFunction != null)
                {
                    fthick *= this.thickFunction.apply(attrs.get(this.thickSelection)).get(0);
                }

                double s = flength;
                double lx0 = (coord.get(0) - s * line.get(0));
                double lx1 = (coord.get(0) + s * line.get(0));
                double ly0 = (coord.get(1) - s * line.get(1));
                double ly1 = (coord.get(1) + s * line.get(1));
                double lz0 = (coord.get(2) - s * line.get(2));
                double lz1 = (coord.get(2) + s * line.get(2));

                double r = color.get(0);
                double g = color.get(1);
                double b = color.get(2);
                double a = 1.0; // opacity.get(0);

                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth(Math.round(fthick));

                gl.glColor4d(r, g, b, a);
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(lx0, ly0, lz0);
                gl.glVertex3d(lx1, ly1, lz1);
                gl.glEnd();
            }
        }
    }

    private void renderGlyphs(GL2 gl, Vect coord, Fibers fibers)
    {
        if (this.sticks)
        {
            Mesh stickMesh = MeshSource.cylinder(this.resolution);
            MeshUtils.computeNormals(stickMesh);
            MeshUtils.flipNormals(stickMesh); // @TODO: figure out why we need this...

            stickMesh.vattr.add(Mesh.COLOR, VectSource.create4D(1, 1, 1, 1));
            stickMesh.vattr.add(Mesh.OPACITY, VectSource.create1D(1));

            for (int k = 0; k < fibers.size(); k++)
            {
                if (fibers.getFrac(k) >= this.minfrac && fibers.getStat(k) >= this.minstat)
                {
                    Vect frac = VectSource.create1D(fibers.getFrac(k));
                    Vect stat = VectSource.create1D(fibers.getStat(k));
                    Vect line = fibers.getLine(k);

                    Map<String, Vect> attrs = Maps.newHashMap();
                    attrs.put(ATTR_NONE, VectSource.create1D(1.0));
                    attrs.put(ATTR_BASE, VectSource.create1D(fibers.getBaseline()));
                    attrs.put(ATTR_FISO, VectSource.create1D(fibers.getFracIso()));
                    attrs.put(ATTR_DIFF, VectSource.create1D(fibers.getDiffusivity()));
                    attrs.put(ATTR_FRAC, frac);
                    attrs.put(ATTR_STAT, stat);
                    attrs.put(ATTR_LINE, line);

                    String attribute = (String) this.comboAttribute.getSelectedItem();
                    Vect color = this.coloring == null ? VectSource.create4D(1, 1, 1, 1) : this.coloring.apply(attrs.get(attribute));
                    double alpha = this.alphaFunction == null ? 1.0 : this.alphaFunction.apply(attrs.get(this.alphaSelection)).get(0);
                    color.set(3, alpha);

                    stickMesh.vattr.setAll(Mesh.COLOR, color);

                    double flength = this.length;
                    if (this.lengthFunction != null)
                    {
                        flength *= this.lengthFunction.apply(attrs.get(this.lengthSelection)).get(0);
                    }

                    double fthick = this.thick;
                    if (this.thickFunction != null)
                    {
                        fthick *= this.thickFunction.apply(attrs.get(this.thickSelection)).get(0);
                    }

                    Vect scale = VectSource.create3D(fthick, fthick, flength);

                    gl.glPushMatrix();
                    RenderUtils.glTransform(gl, scale, coord, line);
                    this.render.render(gl, stickMesh);
                    gl.glPopMatrix();
                }
            }
        }

        double fraciso = fibers.getFracIso();
        double diff = fibers.getDiffusivity();

        if (this.ball && fraciso > this.minball && MathUtils.nonzero(diff))
        {
            // scaleCamera the alpha based on the threshold
            double alpha = (fraciso - this.minball) / (1.0 - this.minball);
            double scale = MathUtils.nonzero(this.diffscale) ? this.diffscale * 1000 * diff / 3.0 : 1.0;

            gl.glPushMatrix();
            gl.glColor4d(1, 1, 1, alpha);
            RenderUtils.glTransform(gl, this.length * scale, coord);
            this.render.render(gl, this.ballMesh);
            gl.glPopMatrix();
        }
    }


    @Override
    public void renderModel(GL2 gl, Vect coord, Vect model)
    {
        if (this.updateColoring)
        {
            this.updateColoringDirect();
            this.updateColoring = false;
        }

        Fibers fibers = new Fibers(model);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(this.width);
        gl.glColor3d(1f, 1f, 1f);

        if (this.glyphs)
        {
            this.renderGlyphs(gl, coord, fibers);
        }
        else
        {
            this.renderLines(gl, coord, fibers);
        }
    }

    @Override
    public boolean valid(int dim)
    {
        return Fibers.valid(dim);
    }
}