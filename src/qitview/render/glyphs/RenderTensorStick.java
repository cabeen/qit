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

import java.awt.event.ActionEvent;
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
import qit.data.models.Tensor;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
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

public class RenderTensorStick extends RenderGlyph
{
    private static final String ATTR_NONE = "none";
    private static final String ATTR_FA = Tensor.FEATURES_FA;
    private static final String ATTR_MD = Tensor.FEATURES_MD;
    private static final String ATTR_RD = Tensor.FEATURES_RD;
    private static final String ATTR_AD = Tensor.FEATURES_AD;
    private static final String ATTR_LINE = "line";
    private static final String[] ATTRS = {ATTR_NONE, ATTR_FA, ATTR_MD, ATTR_RD, ATTR_AD, ATTR_LINE};

    private transient String thickSelection = ATTR_NONE;
    private transient UnitMapDialog thickTransfer = new UnitMapDialog("Stick Thicknessness", 0, 1);
    private transient VectFunction thickFunction = null;

    private transient String lengthSelection = ATTR_NONE;
    private transient UnitMapDialog lengthTransfer = new UnitMapDialog( "Stick Length", 0, 1);
    private transient VectFunction lengthFunction = null;

    private transient String alphaSelection = ATTR_NONE;
    private transient UnitMapDialog alphaTransfer = new UnitMapDialog( "Stick Alpha", 0, 1);
    private transient VectFunction alphaFunction = null;

    private transient VectFunction coloring = null;
    private BasicComboBox<String> comboAttribute = new BasicComboBox<>();
    private BasicComboBox<String> comboColorType = new BasicComboBox<>();
    private BasicComboBox<ColormapSolid> comboColorSolid;
    private BasicComboBox<ColormapDiscrete> comboColorDiscrete;
    private BasicComboBox<ColormapScalar> comboColorScalar;
    private BasicComboBox<ColormapVector> comboColorVector;

    private double thresh = 0.15;
    private int width = 2;
    private int resolution = 12;
    private double thick = 0.3f;
    private double length = 0.75;
    private double wash = 0.0f;
    private boolean glyphs = false;

    protected transient boolean updateColoring = false;
    protected transient Runnable updateParent;
    protected transient RenderGeometry render;

    public RenderTensorStick(Runnable p)
    {
        this.updateParent = p;
        this.render = new RenderGeometry(this.updateParent);

        this.thickTransfer.addObserver((o, arg) -> RenderTensorStick.this.updateThickness());
        this.lengthTransfer.addObserver((o, arg) -> RenderTensorStick.this.updateLength());
        this.alphaTransfer.addObserver((o, arg) -> RenderTensorStick.this.updateAlpha());
    }

    @Override
    public ControlPanel getPanel()
    {
        ControlPanel panel = new ControlPanel();
        {
            JCheckBox cb = new JCheckBox();
            cb.addItemListener(e ->
            {
                RenderTensorStick.this.glyphs = e.getStateChange() == ItemEvent.SELECTED;
                RenderTensorStick.this.updateParent.run();
            });
            panel.addControl("Glyphs", cb);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.thresh));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nthresh = ((Number) tfield.getValue()).doubleValue();
                if (nthresh != RenderTensorStick.this.thresh)
                {
                    RenderTensorStick.this.thresh = nthresh;
                    RenderTensorStick.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thresh to " + RenderTensorStick.this.thresh);
                }
            });
            panel.addControl("Threshold", tfield);
        }
        {
            final BasicFormattedTextField sfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            sfield.setValue(new Double(this.length));
            sfield.addPropertyChangeListener("value", e ->
            {
                double nlength = ((Number) sfield.getValue()).doubleValue();
                if (nlength != RenderTensorStick.this.length)
                {
                    RenderTensorStick.this.length = nlength;
                    RenderTensorStick.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated length to " + RenderTensorStick.this.length);
                }
            });
            panel.addControl("Length", sfield);
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(this.width, 0, 100, 1);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                if (value != RenderTensorStick.this.width)
                {
                    RenderTensorStick.this.width = value;
                    RenderTensorStick.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated width to " + RenderTensorStick.this.width);
                }
            });
            panel.addControl("Width", elem);
        }
        {
            final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.resolution, 6, 200, 1));
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                RenderTensorStick.this.resolution = value;
                Viewer.getInstance().control.setStatusMessage("updated resolution: " + RenderTensorStick.this.resolution);
                RenderTensorStick.this.updateParent.run();
            });
            panel.addControl("Res", elem);
        }
        {
            final BasicFormattedTextField hfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            hfield.setValue(new Double(this.thick));
            hfield.addPropertyChangeListener("value", e ->
            {
                double nthick = ((Number) hfield.getValue()).doubleValue();
                if (nthick != RenderTensorStick.this.thick)
                {
                    RenderTensorStick.this.thick = nthick;
                    RenderTensorStick.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thick to " + RenderTensorStick.this.thick);
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
                if (nwash != RenderTensorStick.this.wash)
                {
                    RenderTensorStick.this.wash = nwash;
                    RenderTensorStick.this.updateColoring();
                    Viewer.getInstance().control.setStatusMessage("updated wash to " + RenderTensorStick.this.wash);
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

            this.comboColorSolid.setVisible(true);
            this.comboColorDiscrete.setVisible(false);
            this.comboColorScalar.setVisible(false);
            this.comboColorVector.setVisible(false);

            this.comboColorType.setSelectedItem(ColormapState.VECTOR);
            this.comboColorSolid.setSelectedIndex(0);
            this.comboColorDiscrete.setSelectedIndex(0);
            this.comboColorScalar.setSelectedIndex(0);
            this.comboColorVector.setSelectedIndex(0);

            final ActionListener listener = e -> RenderTensorStick.this.updateColoring();

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
                RenderTensorStick.this.comboColorType.setSelectedItem(ColormapState.VECTOR);
            }

            this.comboColorSolid.addActionListener(listener);
            this.comboColorDiscrete.addActionListener(listener);
            this.comboColorScalar.addActionListener(listener);
            this.comboColorVector.addActionListener(listener);

            this.updateColoring();

            this.comboColorType.addActionListener(e ->
            {
                String selection = (String) RenderTensorStick.this.comboColorType.getSelectedItem();

                RenderTensorStick.this.comboColorSolid.setVisible(false);
                RenderTensorStick.this.comboColorDiscrete.setVisible(false);
                RenderTensorStick.this.comboColorScalar.setVisible(false);
                RenderTensorStick.this.comboColorVector.setVisible(false);

                switch (selection)
                {
                    case ColormapState.SOLID:
                        RenderTensorStick.this.comboColorSolid.setVisible(true);
                        break;
                    case ColormapState.DISCRETE:
                        RenderTensorStick.this.comboColorDiscrete.setVisible(true);
                        break;
                    case ColormapState.SCALAR:
                        RenderTensorStick.this.comboColorScalar.setVisible(true);
                        break;
                    case ColormapState.VECTOR:
                        RenderTensorStick.this.comboColorVector.setVisible(true);
                        break;
                }
            });

            Viewer.getInstance().colormaps.getObservable().addObserver((o, arg) -> RenderTensorStick.this.updateColoring());

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

                RenderTensorStick.this.thickSelection = selected;
                RenderTensorStick.this.updateThickness();
            };
            cb.addActionListener(listener);
            panel.addControl("Thickness", cb);
        }
        {
            final BasicButton button = new BasicButton("Edit Thickness Transfer...");
            button.addActionListener(arg0 -> RenderTensorStick.this.thickTransfer.show());

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

                RenderTensorStick.this.lengthSelection = selected;
                RenderTensorStick.this.updateLength();
            };
            cb.addActionListener(listener);
            panel.addControl("Length", cb);
        }
        {
            final BasicButton button = new BasicButton("Edit Length Transfer...");
            button.addActionListener(arg0 -> RenderTensorStick.this.lengthTransfer.show());

            panel.addControl(button);
        }
        {
            final BasicComboBox<String> cb = new BasicComboBox<>();
            for (String attr : ATTRS)
            {
                cb.addItem(attr);
            }
            final ActionListener listener = new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    final String selected = cb.getItemAt(cb.getSelectedIndex());
                    if (selected == null)
                    {
                        return;
                    }

                    RenderTensorStick.this.alphaSelection = selected;
                    RenderTensorStick.this.updateAlpha();
                }
            };
            cb.addActionListener(listener);
            panel.addControl("Alpha", cb);
        }
        {
            final BasicButton button = new BasicButton("Edit Alpha Transfer...");
            button.addActionListener(arg0 -> RenderTensorStick.this.alphaTransfer.show());

            panel.addControl(button);
        }
        {
            final ControlPanel renderPanel = this.render.getPanel();
            final BasicButton button = new BasicButton("Render Settings...");
            button.addActionListener(arg0 -> JOptionPane.showMessageDialog(null, renderPanel, "Render Settings", JOptionPane.QUESTION_MESSAGE));

            panel.addControl(button);
        }

        return panel;
    }

    private void updateThickness()
    {
        if (!RenderTensorStick.this.thickSelection.equals(ATTR_NONE))
        {
            RenderTensorStick.this.thickFunction = RenderTensorStick.this.thickTransfer.toFunction();
        }

        RenderTensorStick.this.updateParent.run();
    }

    private void updateLength()
    {
        if (!RenderTensorStick.this.lengthSelection.equals(ATTR_NONE))
        {
            RenderTensorStick.this.lengthFunction = RenderTensorStick.this.lengthTransfer.toFunction();
        }

        RenderTensorStick.this.updateParent.run();
    }

    private void updateAlpha()
    {
        if (!RenderTensorStick.this.alphaSelection.equals(ATTR_NONE))
        {
            RenderTensorStick.this.alphaFunction = RenderTensorStick.this.alphaTransfer.toFunction();
        }

        RenderTensorStick.this.updateParent.run();
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

    private void renderLines(GL2 gl, Vect coord, Tensor tensor)
    {
        double fa = tensor.feature(Tensor.FEATURES_FA).get(0);
        if (fa >= this.thresh)
        {
            Vect line = tensor.getVec(0);

            Map<String, Vect> attrs = Maps.newHashMap();
            attrs.put(ATTR_NONE, VectSource.create1D(1.0));
            attrs.put(ATTR_FA, tensor.feature(Tensor.FEATURES_FA));
            attrs.put(ATTR_MD, tensor.feature(Tensor.FEATURES_MD));
            attrs.put(ATTR_RD, tensor.feature(Tensor.FEATURES_RD));
            attrs.put(ATTR_AD, tensor.feature(Tensor.FEATURES_AD));
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
            double a = color.get(3);

            gl.glDisable(GL2.GL_LIGHTING);
            gl.glLineWidth(Math.round(fthick));

            gl.glColor4d(r, g, b, a);
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(lx0, ly0, lz0);
            gl.glVertex3d(lx1, ly1, lz1);
            gl.glEnd();
        }
    }

    private void renderGlyphs(GL2 gl, Vect coord, Tensor tensor)
    {
        Mesh stickMesh = MeshSource.cylinder(this.resolution);
        MeshUtils.computeNormals(stickMesh);
        MeshUtils.flipNormals(stickMesh); // @TODO: figure out why we need this...

        stickMesh.vattr.add(Mesh.COLOR, VectSource.create4D(1, 1, 1, 1));
        stickMesh.vattr.add(Mesh.OPACITY, VectSource.create1D(1));

        double fa = tensor.feature(Tensor.FEATURES_FA).get(0);
        if (fa >= this.thresh)
        {
            Vect line = tensor.getVec(0);

            Map<String, Vect> attrs = Maps.newHashMap();
            attrs.put(ATTR_NONE, VectSource.create1D(1.0));
            attrs.put(ATTR_FA, tensor.feature(Tensor.FEATURES_FA));
            attrs.put(ATTR_MD, tensor.feature(Tensor.FEATURES_MD));
            attrs.put(ATTR_RD, tensor.feature(Tensor.FEATURES_RD));
            attrs.put(ATTR_AD, tensor.feature(Tensor.FEATURES_AD));
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

    @Override
    public void renderModel(GL2 gl, Vect coord, Vect model)
    {
        if (this.updateColoring)
        {
            this.updateColoringDirect();
            this.updateColoring = false;
        }

        Tensor tensor = new Tensor(model);
        if (this.glyphs)
        {
            this.renderGlyphs(gl, coord, tensor);
        }
        else
        {
            this.renderLines(gl, coord, tensor);
        }
    }

    @Override
    public boolean valid(int dim)
    {
        return Tensor.DT_DIM == dim;
    }
}