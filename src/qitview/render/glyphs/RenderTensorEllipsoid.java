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

import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.text.NumberFormat;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;

import qit.base.Logging;
import qit.data.datasets.Matrix;
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
import qit.math.utils.colormaps.ColormapSource;
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

public class RenderTensorEllipsoid extends RenderGlyph
{
    private static final int MIN_RES = 1;
    private static final int MAX_RES = 5;
    private static final int STEP_RES = 1;

    private static final VectFunction CMAP_LINE = VectFunctionSource.rgb();
    private static final String ATTR_LINE = "line";

    private transient VectFunction coloring = null;
    private BasicComboBox<String> comboAttribute = new BasicComboBox<>();
    private BasicComboBox<String> comboColorType = new BasicComboBox<>();
    private BasicComboBox<ColormapSolid> comboColorSolid;
    private BasicComboBox<ColormapDiscrete> comboColorDiscrete;
    private BasicComboBox<ColormapScalar> comboColorScalar;
    private BasicComboBox<ColormapVector> comboColorVector;

    private int detail = 1;
    private int width = 2;
    private double scale = 1.0;
    private double thresh = 0.01;
    private double units = 1000d;
    private float wash = 0.0f;
    private boolean log = false;
    private boolean modulate = false;
    private boolean desaturate = true;
    private double lowfa = 0.15;
    private double highfa = 0.5;

    protected transient boolean updateColoring = false;
    protected transient Runnable updateParent;
    protected transient RenderGeometry render;

    public RenderTensorEllipsoid(Runnable p)
    {
        this.updateParent = p;
        this.render = new RenderGeometry(this.updateParent);
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
                if (nscale != RenderTensorEllipsoid.this.scale)
                {
                    RenderTensorEllipsoid.this.scale = nscale;
                    RenderTensorEllipsoid.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated scaleCamera to " + RenderTensorEllipsoid.this.scale);
                }
            });
            panel.addControl("Scale", elem);
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(this.width, 0, 100, 1);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                if (value != RenderTensorEllipsoid.this.width)
                {
                    RenderTensorEllipsoid.this.width = value;
                    RenderTensorEllipsoid.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated width to " + RenderTensorEllipsoid.this.width);
                }
            });
            panel.addControl("Width", elem);
        }
        {
            final BasicFormattedTextField wfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            wfield.setValue(new Double(this.wash));
            wfield.addPropertyChangeListener("value", e ->
            {
                float nwash = ((Number) wfield.getValue()).floatValue();
                if (nwash != RenderTensorEllipsoid.this.wash)
                {
                    RenderTensorEllipsoid.this.wash = nwash;
                    RenderTensorEllipsoid.this.updateColoring();
                    Viewer.getInstance().control.setStatusMessage("updated wash to " + RenderTensorEllipsoid.this.wash);
                }
            });
            panel.addControl("Wash", wfield);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.thresh));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nthresh = ((Number) tfield.getValue()).doubleValue();
                if (nthresh != RenderTensorEllipsoid.this.thresh)
                {
                    RenderTensorEllipsoid.this.thresh = nthresh;
                    RenderTensorEllipsoid.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thresh to " + RenderTensorEllipsoid.this.thresh);
                }
            });
            panel.addControl("Threshold", tfield);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.units));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nunits = ((Number) tfield.getValue()).doubleValue();
                if (nunits != RenderTensorEllipsoid.this.units)
                {
                    RenderTensorEllipsoid.this.units = nunits;
                    RenderTensorEllipsoid.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated units to " + RenderTensorEllipsoid.this.units);
                }
            });
            panel.addControl("Units", tfield);
        }
        {
            JCheckBox cb = new JCheckBox();
            cb.setSelected(this.log);
            cb.addItemListener(e ->
            {
                RenderTensorEllipsoid.this.log = e.getStateChange() == ItemEvent.SELECTED;
                RenderTensorEllipsoid.this.updateParent.run();
            });
            panel.addControl("Log", cb);
        }
        {
            JCheckBox cb = new JCheckBox();
            cb.setSelected(this.modulate);
            cb.addItemListener(e ->
            {
                RenderTensorEllipsoid.this.modulate = e.getStateChange() == ItemEvent.SELECTED;
                RenderTensorEllipsoid.this.updateParent.run();
            });
            panel.addControl("Modulate", cb);
        }
        {
            JCheckBox cb = new JCheckBox();
            cb.setSelected(this.desaturate);
            cb.addItemListener(e ->
            {
                RenderTensorEllipsoid.this.desaturate = e.getStateChange() == ItemEvent.SELECTED;
                RenderTensorEllipsoid.this.updateParent.run();
            });
            panel.addControl("Desaturate", cb);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.lowfa));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nlowfa = ((Number) tfield.getValue()).doubleValue();
                if (nlowfa != RenderTensorEllipsoid.this.lowfa)
                {
                    RenderTensorEllipsoid.this.lowfa = nlowfa;
                    RenderTensorEllipsoid.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated low fa to " + RenderTensorEllipsoid.this.lowfa);
                }
            });
            panel.addControl("Low FA", tfield);
        }
        {
            final BasicFormattedTextField tfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            tfield.setValue(new Double(this.highfa));
            tfield.addPropertyChangeListener("value", e ->
            {
                double nhighfa = ((Number) tfield.getValue()).doubleValue();
                if (nhighfa != RenderTensorEllipsoid.this.highfa)
                {
                    RenderTensorEllipsoid.this.highfa = nhighfa;
                    RenderTensorEllipsoid.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated high fa to " + RenderTensorEllipsoid.this.highfa);
                }
            });
            panel.addControl("High FA", tfield);
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(this.detail, MIN_RES, MAX_RES, STEP_RES);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                if (value != RenderTensorEllipsoid.this.detail)
                {
                    RenderTensorEllipsoid.this.detail = value;
                    RenderTensorEllipsoid.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated glyph detail to " + RenderTensorEllipsoid.this.detail);
                }
            });
            panel.addControl("Detail", elem);
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

            final ActionListener listener = e -> RenderTensorEllipsoid.this.updateColoring();

            {
                this.comboAttribute.addItem(ATTR_LINE);
                for (String attr : new Tensor().proto().features())
                {
                    if (!Mesh.COLOR.equals(attr))
                    {
                        this.comboAttribute.addItem(attr);
                    }
                }
                this.comboAttribute.setSelectedItem(ATTR_LINE);
                this.comboAttribute.addActionListener(listener);
                RenderTensorEllipsoid.this.comboColorType.setSelectedItem(ColormapState.VECTOR);
            }

            this.comboColorSolid.addActionListener(listener);
            this.comboColorDiscrete.addActionListener(listener);
            this.comboColorScalar.addActionListener(listener);
            this.comboColorVector.addActionListener(listener);

            this.updateColoring();

            this.comboColorType.addActionListener(e ->
            {
                String selection = (String) RenderTensorEllipsoid.this.comboColorType.getSelectedItem();

                RenderTensorEllipsoid.this.comboColorSolid.setVisible(false);
                RenderTensorEllipsoid.this.comboColorDiscrete.setVisible(false);
                RenderTensorEllipsoid.this.comboColorScalar.setVisible(false);
                RenderTensorEllipsoid.this.comboColorVector.setVisible(false);

                switch (selection)
                {
                    case ColormapState.SOLID:
                        RenderTensorEllipsoid.this.comboColorSolid.setVisible(true);
                        break;
                    case ColormapState.DISCRETE:
                        RenderTensorEllipsoid.this.comboColorDiscrete.setVisible(true);
                        break;
                    case ColormapState.SCALAR:
                        RenderTensorEllipsoid.this.comboColorScalar.setVisible(true);
                        break;
                    case ColormapState.VECTOR:
                        RenderTensorEllipsoid.this.comboColorVector.setVisible(true);
                        break;
                }
            });

            Viewer.getInstance().colormaps.getObservable().addObserver((o, arg) -> RenderTensorEllipsoid.this.updateColoring());

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
            final ControlPanel renderPanel = this.render.getPanel();
            final BasicButton button = new BasicButton("Render Settings...");
            button.addActionListener(arg0 -> JOptionPane.showMessageDialog(null, renderPanel, "Render Settings", JOptionPane.QUESTION_MESSAGE));

            panel.addControl(button);
        }

        return panel;
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

        int dattr = attribute.equals(ATTR_LINE) ? 3 : new Tensor().feature(attribute).size();
        int dcolor = this.coloring.getDimIn();
        if (attribute == null || dcolor != dattr)
        {
            this.coloring = VectFunctionSource.constant(dattr, VectSource.createND(dcolor, 1)).compose(coloring);
        }

        // apply wash
        this.coloring = coloring.compose(VectFunctionSource.wash(this.wash));

        // signal a change
        this.updateParent.run();
    }

    @Override
    public boolean valid(int dim)
    {
        return Tensor.DT_DIM == dim;
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
        double fa = tensor.feature(Tensor.FEATURES_FA).get(0);

        if (fa < this.thresh)
        {
            return;
        }

        Vect line = tensor.getVec(0);
        double v1 = Math.max(0, tensor.getVal(0));
        double v2 = Math.max(0, tensor.getVal(1));
        double v3 = Math.max(0, tensor.getVal(2));

        // Third eigenvector could imply a left or right-handed coordinate
        // system due to ambiguities in the eigendecomposition of the tensor, so
        // let's compute the cross product so we know it's always right-handed
        Vect cross = tensor.getVec(0).cross(tensor.getVec(1));

        double[][] R = new double[3][3];
        R[0][0] = tensor.getVec(0).get(0);
        R[1][0] = tensor.getVec(0).get(1);
        R[2][0] = tensor.getVec(0).get(2);
        R[0][1] = tensor.getVec(1).get(0);
        R[1][1] = tensor.getVec(1).get(1);
        R[2][1] = tensor.getVec(1).get(2);
        R[0][2] = cross.get(0);
        R[1][2] = cross.get(1);
        R[2][2] = cross.get(2);

        Mesh mesh = MeshSource.sphere(this.detail);
        MeshUtils.computeNormals(mesh);

        String attribute = (String) this.comboAttribute.getSelectedItem();

        Vect color = VectSource.create4D(1, 1, 1, 1);

        if (attribute.equals(ATTR_LINE) || this.coloring == null)
        {
            color = VectFunctionSource.rgb().apply(line);

            double scale = (fa - this.lowfa) / (this.highfa - this.lowfa);
            scale = Math.min(1.0, scale);
            scale = Math.max(0.0, scale);

            if (this.modulate)
            {
                // skip alpha
                for (int i = 0; i < 3; i++)
                {
                    color.set(i, scale * color.get(i));
                }
            }

            if (this.desaturate)
            {
                Vect hsv = ColormapSource.hsv(color);
                hsv.set(1, scale * hsv.get(1));
                color = ColormapSource.rgb(hsv);
            }
        }
        else
        {
            this.coloring.apply(tensor.feature(attribute));
        }

        mesh.vattr.setAll(Mesh.COLOR, color);

        Vect scale = VectSource.create3D(v1, v2, v3).times(this.units * this.scale);

        if (this.log)
        {
            for (int i = 0; i < scale.size(); i++)
            {
                scale.set(i, Math.log(scale.get(i) + 1));
            }
        }

        gl.glPushMatrix();
        RenderUtils.glTransform(gl, scale, coord, new Matrix(R));
        this.render.render(gl, mesh);
        gl.glPopMatrix();
    }
}