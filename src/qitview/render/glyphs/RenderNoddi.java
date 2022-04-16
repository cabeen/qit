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
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.NumberFormat;
import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;

import qit.base.Logging;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.models.Noddi;
import qit.data.modules.mesh.MeshSubdivide;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.data.utils.vects.stats.VectOnlineStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
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

public class RenderNoddi extends RenderGlyph
{
    private static final String ATTR_NONE = "none";
    private static final String ATTR_ECVF = "ecvf";
    private static final String ATTR_ICVF = "icvf";
    private static final String ATTR_ISOVF = "isovf";
    private static final String ATTR_OD = "od";
    private static final String ATTR_KAPPA = "kappa";
    private static final String ATTR_TORT = "tort";
    private static final String ATTR_EAD = "ead";
    private static final String ATTR_ERD = "erd";
    private static final String ATTR_DIR = "dir";
    private static final String[] ATTRS = {ATTR_NONE, ATTR_ICVF, ATTR_ECVF, ATTR_ISOVF, ATTR_OD, ATTR_KAPPA, ATTR_TORT, ATTR_EAD, ATTR_ERD, ATTR_DIR};

    private static final int MIN_RES = 1;
    private static final int MAX_RES = 5;
    private static final int STEP_RES = 1;

    private transient VectFunction coloring = null;
    private BasicComboBox<String> comboAttribute = new BasicComboBox<>();
    private BasicComboBox<String> comboColorType = new BasicComboBox<>();
    private BasicComboBox<ColormapSolid> comboColorSolid;
    private BasicComboBox<ColormapDiscrete> comboColorDiscrete;
    private BasicComboBox<ColormapScalar> comboColorScalar;
    private BasicComboBox<ColormapVector> comboColorVector;

    private int detail = 1;
    private int width = 2;
    private double disc = 0.2;
    private double thick = 0.5;
    private double wash = 0.0f;
    private double scaleIntra = 1.0;
    private double scaleExtra = 1.0;
    private double scaleIso = 1.0;

    private boolean glyphs = false;
    private boolean intra = false;
    private boolean extra = false;
    private boolean iso = false;

    protected transient boolean updateColoring = false;
    protected transient Runnable updateParent;
    protected transient RenderGeometry renderIntra;
    protected transient RenderGeometry renderExtra;
    protected transient RenderGeometry renderIso;

    public RenderNoddi(Runnable p)
    {
        this.updateParent = p;
        this.renderIntra = new RenderGeometry(this.updateParent);
        this.renderExtra = new RenderGeometry(this.updateParent);
        this.renderIso = new RenderGeometry(this.updateParent);
    }

    @Override
    public ControlPanel getPanel()
    {
        ControlPanel panel = new ControlPanel();
        {
            JCheckBox cb = new JCheckBox();
            cb.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent e)
                {
                    RenderNoddi.this.glyphs = e.getStateChange() == ItemEvent.SELECTED;
                    RenderNoddi.this.updateParent.run();
                }
            });
            panel.addControl("Glyphs", cb);
        }
        {
            JCheckBox cb = new JCheckBox();
            cb.addItemListener(e ->
            {
                RenderNoddi.this.intra = e.getStateChange() == ItemEvent.SELECTED;
                RenderNoddi.this.updateParent.run();
            });
            panel.addControl("Intra", cb);
        }
        {
            JCheckBox cb = new JCheckBox();
            cb.addItemListener(e ->
            {
                RenderNoddi.this.extra = e.getStateChange() == ItemEvent.SELECTED;
                RenderNoddi.this.updateParent.run();
            });
            panel.addControl("Extra", cb);
        }
        {
            JCheckBox cb = new JCheckBox();
            cb.addItemListener(e ->
            {
                RenderNoddi.this.iso = e.getStateChange() == ItemEvent.SELECTED;
                RenderNoddi.this.updateParent.run();
            });
            panel.addControl("Iso", cb);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.scaleIntra));
            elem.addPropertyChangeListener("value", e ->
            {
                double nscale = ((Number) elem.getValue()).doubleValue();
                if (nscale != RenderNoddi.this.scaleIso)
                {
                    RenderNoddi.this.scaleIntra = nscale;
                    RenderNoddi.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated scaleCamera to " + RenderNoddi.this.scaleIntra);
                }
            });
            panel.addControl("Intracellular Scale", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.scaleExtra));
            elem.addPropertyChangeListener("value", e ->
            {
                double nscale = ((Number) elem.getValue()).doubleValue();
                if (nscale != RenderNoddi.this.scaleIso)
                {
                    RenderNoddi.this.scaleExtra = nscale;
                    RenderNoddi.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated scaleCamera to " + RenderNoddi.this.scaleExtra);
                }
            });
            panel.addControl("Extracellular Scale", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.scaleIso));
            elem.addPropertyChangeListener("value", e ->
            {
                double nscale = ((Number) elem.getValue()).doubleValue();
                if (nscale != RenderNoddi.this.scaleIso)
                {
                    RenderNoddi.this.scaleIso = nscale;
                    RenderNoddi.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated scaleCamera to " + RenderNoddi.this.scaleIso);
                }
            });
            panel.addControl("IsotropicFull Scale", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.thick));
            elem.addPropertyChangeListener("value", e ->
            {
                double nthick = ((Number) elem.getValue()).doubleValue();
                if (nthick != RenderNoddi.this.thick)
                {
                    RenderNoddi.this.thick = nthick;
                    RenderNoddi.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated thick to " + RenderNoddi.this.thick);
                }
            });
            panel.addControl("Thick", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.disc));
            elem.addPropertyChangeListener("value", e ->
            {
                double ndisc = ((Number) elem.getValue()).doubleValue();
                if (ndisc != RenderNoddi.this.disc)
                {
                    RenderNoddi.this.disc = ndisc;
                    RenderNoddi.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated scaleCamera to " + RenderNoddi.this.disc);
                }
            });
            panel.addControl("Disc", elem);
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(this.width, 0, 100, 1);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                if (value != RenderNoddi.this.width)
                {
                    RenderNoddi.this.width = value;
                    RenderNoddi.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated width to " + RenderNoddi.this.width);
                }
            });
            panel.addControl("Width", elem);
        }
        {
            final BasicFormattedTextField wfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            wfield.setValue(new Double(this.wash));
            wfield.addPropertyChangeListener("value", e ->
            {
                double nwash = ((Number) wfield.getValue()).doubleValue();
                if (nwash != RenderNoddi.this.wash)
                {
                    RenderNoddi.this.wash = nwash;
                    RenderNoddi.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated wash to " + RenderNoddi.this.wash);
                }
            });
            panel.addControl("Wash", wfield);
        }
        {
            SpinnerNumberModel model = new SpinnerNumberModel(this.detail, MIN_RES, MAX_RES, STEP_RES);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getValue().toString());
                if (value != RenderNoddi.this.detail)
                {
                    RenderNoddi.this.detail = value;
                    RenderNoddi.this.updateParent.run();
                    Viewer.getInstance().control.setStatusMessage("updated glyph detail to " + RenderNoddi.this.detail);
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

            final ActionListener listener = e -> RenderNoddi.this.updateColoring();

            {
                this.comboAttribute.addItem(ATTR_DIR);
                for (String attr : new Noddi().features())
                {
                    if (!Mesh.COLOR.equals(attr) && !Mesh.OPACITY.equals(attr))
                    {
                        this.comboAttribute.addItem(attr);
                    }
                }
                this.comboAttribute.setSelectedItem(ATTR_DIR);
                this.comboAttribute.addActionListener(listener);
                RenderNoddi.this.comboColorType.setSelectedItem(ColormapState.VECTOR);
            }

            this.comboColorSolid.addActionListener(listener);
            this.comboColorDiscrete.addActionListener(listener);
            this.comboColorScalar.addActionListener(listener);
            this.comboColorVector.addActionListener(listener);

            this.updateColoring();

            this.comboColorType.addActionListener(e ->
            {
                String selection = (String) RenderNoddi.this.comboColorType.getSelectedItem();

                RenderNoddi.this.comboColorSolid.setVisible(false);
                RenderNoddi.this.comboColorDiscrete.setVisible(false);
                RenderNoddi.this.comboColorScalar.setVisible(false);
                RenderNoddi.this.comboColorVector.setVisible(false);

                switch (selection)
                {
                    case ColormapState.SOLID:
                        RenderNoddi.this.comboColorSolid.setVisible(true);
                        break;
                    case ColormapState.DISCRETE:
                        RenderNoddi.this.comboColorDiscrete.setVisible(true);
                        break;
                    case ColormapState.SCALAR:
                        RenderNoddi.this.comboColorScalar.setVisible(true);
                        break;
                    case ColormapState.VECTOR:
                        RenderNoddi.this.comboColorVector.setVisible(true);
                        break;
                }
            });

            Viewer.getInstance().colormaps.getObservable().addObserver((o, arg) -> RenderNoddi.this.updateColoring());

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
            final ControlPanel renderPanel = this.renderIntra.getPanel();
            final BasicButton button = new BasicButton("Intracellular Render Settings...");
            {
                button.addActionListener(arg0 -> JOptionPane.showMessageDialog(null, renderPanel, "Render Settings", JOptionPane.QUESTION_MESSAGE));
            }

            panel.addControl(button);
        }
        {
            final ControlPanel renderPanel = this.renderExtra.getPanel();
            final BasicButton button = new BasicButton("Extracellular Render Settings...");
            {
                button.addActionListener(arg0 -> JOptionPane.showMessageDialog(null, renderPanel, "Render Settings", JOptionPane.QUESTION_MESSAGE));
            }

            panel.addControl(button);
        }
        {
            final ControlPanel renderPanel = this.renderIso.getPanel();
            final BasicButton button = new BasicButton("IsotropicFull Render Settings...");
            {
                button.addActionListener(arg0 -> JOptionPane.showMessageDialog(null, renderPanel, "Render Settings", JOptionPane.QUESTION_MESSAGE));
            }

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

        int dattr = attribute.equals(ATTR_DIR) ? 3 : new Noddi().feature(attribute).size();
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

    private void renderLines(GL2 gl, Vect coord, Noddi model)
    {
        double icvf = model.feature(Noddi.FICVF).get(0);
        double isovf = model.feature(Noddi.FISO).get(0);
        double od = model.feature(Noddi.ODI).get(0);
        Vect dir = model.feature(Noddi.DIR);

        double ticvf = icvf * (1 - isovf);

        if ((1 - isovf) < 1e-3 || icvf < 1e-3)
        {
            return;
        }

        Map<String, Vect> attrs = Maps.newHashMap();
        attrs.put(ATTR_NONE, VectSource.create1D(1.0));
        attrs.put(ATTR_ICVF, VectSource.create1D(icvf));
        attrs.put(ATTR_ISOVF, VectSource.create1D(isovf));
        attrs.put(ATTR_OD, VectSource.create1D(od));
        attrs.put(ATTR_DIR, dir);

        String attribute = (String) this.comboAttribute.getSelectedItem();
        Vect color = this.coloring == null ? VectFunctionSource.rgb().apply(dir) : this.coloring.apply(attrs.get(attribute));

        double s = this.scaleIntra;
        double lx0 = coord.get(0) - s * dir.get(0);
        double lx1 = coord.get(0) + s * dir.get(0);
        double ly0 = coord.get(1) - s * dir.get(1);
        double ly1 = coord.get(1) + s * dir.get(1);
        double lz0 = coord.get(2) - s * dir.get(2);
        double lz1 = coord.get(2) + s * dir.get(2);

        double r = color.get(0);
        double g = color.get(1);
        double b = color.get(2);

        gl.glColor3d(r, g, b);
        gl.glBegin(GL2.GL_LINE_STRIP);
        gl.glVertex3d(lx0, ly0, lz0);
        gl.glVertex3d(lx1, ly1, lz1);
        gl.glEnd();
    }

    private void renderGlyphs(GL2 gl, Vect coord, Noddi model)
    {
        double icvf = model.feature(Noddi.FICVF).get(0);
        double ecvf = model.feature(Noddi.ECVF).get(0);
        double isovf = model.feature(Noddi.FISO).get(0);
        double od = model.feature(Noddi.ODI).get(0);
        double kappa = model.feature(Noddi.KAPPA).get(0);
        double tort = model.feature(Noddi.TORT).get(0);
        double ead = model.feature(Noddi.EAD).get(0);
        double erd = model.feature(Noddi.ERD).get(0);
        Vect dir = model.feature(Noddi.DIR);

        double anisovf = (1 - isovf);
        double ticvf = icvf * anisovf;
        double tecvf = ecvf * anisovf;

        Map<String, Vect> attrs = Maps.newHashMap();
        attrs.put(ATTR_NONE, VectSource.create1D(1.0));
        attrs.put(ATTR_ICVF, VectSource.create1D(icvf));
        attrs.put(ATTR_ECVF, VectSource.create1D(ecvf));
        attrs.put(ATTR_ISOVF, VectSource.create1D(isovf));
        attrs.put(ATTR_OD, VectSource.create1D(od));
        attrs.put(ATTR_KAPPA, VectSource.create1D(kappa));
        attrs.put(ATTR_TORT, VectSource.create1D(tort));
        attrs.put(ATTR_DIR, dir);

        String attribute = (String) this.comboAttribute.getSelectedItem();
        Vect color = this.coloring == null ? VectFunctionSource.rgb().apply(dir) : this.coloring.apply(attrs.get(attribute));

        double sizeIntra = this.scaleIntra * anisovf * icvf;
        double sizeExtra = this.scaleExtra * anisovf * ecvf;
        double sizeIso = this.scaleIso;

        if (this.intra && ticvf > 1e-2)
        {
            Mesh mesh = MeshSource.octohedron();

            MeshSubdivide subdiv = new MeshSubdivide();
            subdiv.input = mesh;
            subdiv.inplace = true;
            subdiv.num = this.detail;
            subdiv.run();

            mesh.vattr.add(Mesh.DISTANCE, VectSource.create1D());
            VectOnlineStats stats = new VectOnlineStats();
            for (Vertex v : mesh.vattr)
            {
                Vect c = mesh.vattr.get(v, Mesh.COORD).normalize();
                double dot = c.dot(VectSource.create3D(0, 0, 1));
                double value = Math.exp(kappa * dot * dot);

                stats.update(value);
                mesh.vattr.set(v, Mesh.DISTANCE, VectSource.create1D(value));
                mesh.vattr.set(v, Mesh.COORD, c);
            }

            double delta = stats.max == stats.min ? 1.0 : stats.max - stats.min;
            for (Vertex v : mesh.vattr)
            {
                Vect c = mesh.vattr.get(v, Mesh.COORD);
                double value = mesh.vattr.get(v, Mesh.DISTANCE).get(0);
                double nvalue = od + (1 - od) * (value - stats.min) / delta;
                mesh.vattr.set(v, Mesh.COORD, c.times(nvalue));
            }

            MeshUtils.computeNormals(mesh);

            int cr = (int) Math.round(255 * color.get(0));
            int cg = (int) Math.round(255 * color.get(1));
            int cb = (int) Math.round(255 * color.get(2));

            float[] hsv = Color.RGBtoHSB(cr, cg, cb, new float[3]);
            float ch = hsv[0];
            float cs = (float) ((1 - od) * hsv[1]);
            float cv = hsv[2];
            Color cc = new Color(Color.HSBtoRGB(ch, cs, cv));
            color.set(0, cc.getRed() / 255f);
            color.set(1, cc.getGreen() / 255f);
            color.set(2, cc.getBlue() / 255f);

            mesh.vattr.add(Mesh.COLOR, VectSource.create4D(1, 1, 1, 1));
            mesh.vattr.add(Mesh.OPACITY, VectSource.create1D(1));

            mesh.vattr.setAll(Mesh.OPACITY, VectSource.create1D(1.0));
            mesh.vattr.setAll(Mesh.COLOR, color);

            Vect size = VectSource.create3D(sizeIntra, sizeIntra, sizeIntra);

            gl.glPushMatrix();
            RenderUtils.glTransform(gl, size, coord, dir);
            this.renderIntra.render(gl, mesh);
            gl.glPopMatrix();
        }

        if (this.extra && tecvf > 1e-2)
        {
            Mesh mesh = MeshSource.octohedron();

            MeshSubdivide subdiv = new MeshSubdivide();
            subdiv.input = mesh;
            subdiv.inplace = true;
            subdiv.num = this.detail;
            subdiv.run();

            double f = erd / ead;
            for (Vertex v : mesh.vattr)
            {
                Vect c = mesh.vattr.get(v, Mesh.COORD).normalize();
                Vect sc = VectSource.create3D(c.getX(), f * c.getY(), f * c.getZ());
                mesh.vattr.set(v, Mesh.COORD, sc);
            }

            MeshUtils.computeNormals(mesh);

            int cr = (int) Math.round(255 * color.get(0));
            int cg = (int) Math.round(255 * color.get(1));
            int cb = (int) Math.round(255 * color.get(2));

            float[] hsv = Color.RGBtoHSB(cr, cg, cb, new float[3]);
            float ch = hsv[0];
            float cs = (float) ((1 - od) * hsv[1]);
            float cv = hsv[2];
            Color cc = new Color(Color.HSBtoRGB(ch, cs, cv));
            color.set(0, cc.getRed() / 255f);
            color.set(1, cc.getGreen() / 255f);
            color.set(2, cc.getBlue() / 255f);

            mesh.vattr.add(Mesh.COLOR, VectSource.create4D(1, 1, 1, 1));
            mesh.vattr.add(Mesh.OPACITY, VectSource.create1D(1));

            mesh.vattr.setAll(Mesh.OPACITY, VectSource.create1D(1.0));
            mesh.vattr.setAll(Mesh.COLOR, color);

            Vect size = VectSource.create3D(sizeExtra, sizeExtra, sizeExtra);

            gl.glPushMatrix();
            RenderUtils.glTransform(gl, size, coord, dir);
            this.renderExtra.render(gl, mesh);
            gl.glPopMatrix();
        }

        if (this.iso && isovf > 1e-2)
        {
            Mesh ballMesh = MeshSource.sphere(1);
            MeshUtils.computeNormals(ballMesh);

            ballMesh.vattr.add(Mesh.COLOR, VectSource.create4D(1, 1, 1, 1));
            ballMesh.vattr.add(Mesh.OPACITY, VectSource.create1D(1));

            ballMesh.vattr.setAll(Mesh.OPACITY, VectSource.create1D(0.75 * isovf));
            ballMesh.vattr.setAll(Mesh.COLOR, VectSource.create4D(1, 1, 1, 1));

            gl.glPushMatrix();
            RenderUtils.glTransform(gl, sizeIso, coord);
            this.renderIso.render(gl, ballMesh);
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

        Noddi noddi = new Noddi(model);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glLineWidth(this.width);
        gl.glColor3f(1f, 1f, 1f);

        if (this.glyphs)
        {
            this.renderGlyphs(gl, coord, noddi);
        }
        else
        {
            this.renderLines(gl, coord, noddi);
        }
    }

    @Override
    public boolean valid(int dim)
    {
        return Noddi.DIM == dim;
    }
}