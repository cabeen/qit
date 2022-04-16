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
import com.google.common.collect.Sets;
import com.jogamp.opengl.GL2;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;
import org.apache.commons.io.FilenameUtils;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliUtils;
import qit.base.structs.Named;
import qit.base.structs.Pair;
import qit.base.utils.JavaUtils;
import qit.base.utils.PathUtils;
import qit.data.datasets.Curves;
import qit.data.datasets.Curves.Curve;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.datasets.Volume;
import qit.data.modules.curves.*;
import qit.data.modules.volume.VolumeEnhanceContrast;
import qit.data.source.VectSource;
import qit.data.utils.CurvesUtils;
import qit.data.utils.curves.CurvesFunctionApply;
import qit.data.utils.vects.stats.VectStats;
import qit.math.source.VectFunctionSource;
import qit.math.structs.Box;
import qit.math.structs.Line;
import qit.math.structs.Sphere;
import qit.math.structs.VectFunction;
import qit.math.utils.colormaps.ColormapDiscrete;
import qit.math.utils.colormaps.ColormapScalar;
import qit.math.utils.colormaps.ColormapSolid;
import qit.math.utils.colormaps.ColormapVector;
import qitview.main.Constants;
import qitview.main.Interface;
import qitview.main.Viewer;
import qitview.models.*;
import qitview.panels.Viewables;
import qitview.render.RenderGeometry;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicLabel;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ColormapState;
import qitview.widgets.ControlPanel;
import qitview.widgets.SwingUtils;
import qitview.widgets.UnitMapDialog;

public class CurvesView extends AbstractView<Curves>
{
    private enum Interaction
    {
        Query, Remove, None
    }

    private static final String NONE = "None";

    private static final Vect SELECTED_COLOR = VectSource.create4D(1, 0, 0, 1);
    private static final Vect DEFAULT_COLOR = VectSource.create4D(1, 1, 1, 1);
    private static final int MIN_WIDTH = 1;
    private static final int MAX_WIDTH = 2500;
    private static final int STEP_WIDTH = 1;
    private static final int DEF_MAX_NUM = 100000;

    private final static int MIN_RES = 5;
    private final static int MAX_RES = 20;
    private final static int STEP_RES = 1;

    private transient ReentrantLock lock = new ReentrantLock();

    private transient Integer list = null;
    private transient boolean update = false;

    private transient RenderGeometry render;
    private transient CurvesTubes tuber = new CurvesTubes();
    private transient Set<Integer> subset = null;
    private transient boolean[] shown = null;
    private transient Set<Integer> selected = Sets.newHashSet();

    private transient BasicComboBox<String> comboColorAttribute;
    private transient BasicComboBox<String> comboColorType;
    private transient BasicComboBox<ColormapSolid> comboColorSolid;
    private transient BasicComboBox<ColormapDiscrete> comboColorDiscrete;
    private transient BasicComboBox<ColormapScalar> comboColorScalar;
    private transient BasicComboBox<ColormapVector> comboColorVector;
    private transient BasicButton autoMinMaxColor;
    private transient BasicButton editColorSolid;
    private transient BasicButton editColorDiscrete;
    private transient BasicButton editColorScalar;

    private transient BasicComboBox<String> comboThickAttribute;
    private transient UnitMapDialog transferThick = new UnitMapDialog("Thickness", 0, 1);
    private transient BasicButton autoMinMaxThick;

    private transient BasicComboBox<String> comboWeightAttribute;
    private transient UnitMapDialog transferWeight = new UnitMapDialog("Weightness", 0, 1);
    private transient BasicButton autoMinMaxWeight;

    private transient BasicComboBox<String> comboOpacityAttribute;
    private transient UnitMapDialog transferOpacity = new UnitMapDialog("Opacityness", 0, 1);
    private transient BasicButton autoMinMaxOpacity;

    private transient SolidsView solidsClip;
    private transient SolidsView solidsInclude;
    private transient SolidsView solidsExclude;

    private String name = Curves.LABEL;
    private int label = 0;
    private String which = "";
    private boolean limit = true;
    private boolean endpoints = false;
    private int maxnum = DEF_MAX_NUM;
    private double wash = 0.0f;
    private int pointWidth = 4;
    private int lineWidth = 2;
    private double precision = 1;
    private boolean smoothCaps = false;
    private boolean showHeads = false;
    private boolean showTails = false;
    private boolean showEnds = false;
    private boolean showPoints = false;
    private boolean showLines = true;
    private boolean showTubes = false;
    private double opacity = 1f;

    final transient ActionListener coloringListener = e -> CurvesView.this.updateColoring();
    final transient ActionListener listenerThick = e -> CurvesView.this.updateThickness();
    final transient ActionListener listenerWeight = e -> CurvesView.this.updateColoring();
    final transient ActionListener listenerOpacity = e -> CurvesView.this.updateColoring();
    private transient Observer changeObserver = (o, arg) -> this.update = true;

    public CurvesView()
    {
        super();

        this.render = new RenderGeometry(() -> this.update = true);
        this.render.setMeshSmooth(true);

        this.tuber.fthick = 0.15;

        this.observable.addObserver(this.changeObserver);
        this.observable.addObserver((o, arg) -> CurvesView.this.updateAttributes());

        this.initColoring();
        this.initThickness();
        super.initPanel();
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

    public CurvesView setData(Curves d)
    {
        if (d != null)
        {
            this.data = d;

            if (this.data.size() > 0)
            {
                this.bounds = this.data.bounds();
                Viewer.getInstance().gui.canvas.render3D.boxUpdate();
            }
        }

        this.updateTangents();
        this.updateColoring();
        this.updateThickness();
        super.setData(this.data);

        return this;
    }

    public Curves getData()
    {
        return this.data;
    }

    private void updateAttributes()
    {
        BasicComboBox comboColor = this.comboColorAttribute;
        Object selectedColor = comboColor.getSelectedItem();
        comboColor.removeActionListener(this.coloringListener);
        comboColor.removeAllItems();

        BasicComboBox comboThick = this.comboThickAttribute;
        Object selectedThick = comboThick.getSelectedItem();
        comboThick.removeActionListener(this.listenerThick);
        comboThick.removeAllItems();

        BasicComboBox comboWeight = this.comboWeightAttribute;
        Object selectedWeight = comboWeight.getSelectedItem();
        comboWeight.removeActionListener(this.listenerWeight);
        comboWeight.removeAllItems();

        BasicComboBox comboOpacity = this.comboOpacityAttribute;
        Object selectedOpacity = comboOpacity.getSelectedItem();
        comboOpacity.removeActionListener(this.listenerOpacity);
        comboOpacity.removeAllItems();

        comboThick.addItem(NONE);
        comboWeight.addItem(NONE);
        comboOpacity.addItem(NONE);
        for (String attr : this.data.names())
        {
            if (!Curves.COLOR.equals(attr) && !Curves.OPACITY.equals(attr) && !Curves.THICKNESS.equals(attr))
            {
                comboColor.addItem(attr);
                comboThick.addItem(attr);
                comboWeight.addItem(attr);
                comboOpacity.addItem(attr);
            }
        }

        comboColor.setSelectedItem(selectedColor == null ? Curves.TANGENT : selectedColor);
        comboColor.addActionListener(this.coloringListener);

        comboThick.setSelectedItem(selectedThick == null ? NONE : selectedThick);
        comboThick.addActionListener(this.listenerThick);

        comboWeight.setSelectedItem(selectedWeight == null ? NONE : selectedWeight);
        comboWeight.addActionListener(this.listenerWeight);

        comboOpacity.setSelectedItem(selectedOpacity == null ? NONE : selectedOpacity);
        comboOpacity.addActionListener(this.listenerOpacity);
    }

    private void initThickness()
    {
        this.transferThick.addObserver((o, arg) -> CurvesView.this.updateThickness());

        this.autoMinMaxThick = new BasicButton("Auto Min/Max");
        this.autoMinMaxThick.addActionListener(arg0 -> CurvesView.this.autoMinMaxThick());

        this.comboThickAttribute = new BasicComboBox<>();
        this.comboThickAttribute.setToolTipText("specify which attribute to use for curve thickness");

        this.updateThickness();
    }

    private void initColoring()
    {
        this.transferWeight.addObserver((o, arg) -> CurvesView.this.updateColoring());

        this.autoMinMaxWeight = new BasicButton("Auto Min/Max");
        this.autoMinMaxWeight.addActionListener(arg0 -> CurvesView.this.autoMinMaxWeight());

        this.comboWeightAttribute = new BasicComboBox<>();
        this.comboWeightAttribute.setToolTipText("specify which attribute to use for curve thickness");

        this.transferOpacity.addObserver((o, arg) -> CurvesView.this.updateColoring());

        this.autoMinMaxOpacity = new BasicButton("Auto Min/Max");
        this.autoMinMaxOpacity.addActionListener(arg0 -> CurvesView.this.autoMinMaxOpacity());

        this.comboOpacityAttribute = new BasicComboBox<>();
        this.comboOpacityAttribute.setToolTipText("specify which attribute to use for curve thickness");

        Viewer.getInstance().colormaps.getObservable().addObserver((o, arg) -> CurvesView.this.updateColoring());

        this.autoMinMaxColor = new BasicButton("Auto Min/Max");
        {
            this.autoMinMaxColor.addActionListener(arg0 -> CurvesView.this.autoMinMaxColor());
        }

        this.editColorSolid = new BasicButton("Edit Solid Colormap");
        {
            this.editColorSolid.addActionListener(arg0 ->
            {
                ColormapState cmap = Viewer.getInstance().colormaps;
                cmap.showSolid();
                cmap.setComboSolid((ColormapSolid) CurvesView.this.comboColorSolid.getSelectedItem());
            });
        }

        this.editColorDiscrete = new BasicButton("Edit Discrete Colormap");
        {
            this.editColorDiscrete.addActionListener(arg0 ->
            {
                ColormapState cmap = Viewer.getInstance().colormaps;
                cmap.showDiscrete();
                cmap.setComboDiscrete((ColormapDiscrete) CurvesView.this.comboColorDiscrete.getSelectedItem());
            });
        }

        this.editColorScalar = new BasicButton("Edit Scalar Colormap");
        {
            this.editColorScalar.addActionListener(arg0 ->
            {
                ColormapState cmap = Viewer.getInstance().colormaps;
                cmap.showScalar();
                cmap.setComboScalar((ColormapScalar) CurvesView.this.comboColorScalar.getSelectedItem());
            });
        }

        this.comboColorAttribute = new BasicComboBox<>();

        this.observable.addObserver((o, arg) -> CurvesView.this.updateAttributes());

        this.comboColorType = new BasicComboBox<>();
        this.comboColorType.addItem(ColormapState.SOLID);
        this.comboColorType.addItem(ColormapState.DISCRETE);
        this.comboColorType.addItem(ColormapState.SCALAR);
        this.comboColorType.addItem(ColormapState.VECTOR);

        this.comboColorType.setToolTipText("specify the type of colormap to use for the vertex coloring");
        this.comboColorAttribute.setToolTipText("specify which attribute to use for vertex coloring");

        final ColormapState cms = Viewer.getInstance().colormaps;
        this.comboColorSolid = cms.getComboSolid();
        this.comboColorDiscrete = cms.getComboDiscrete();
        this.comboColorScalar = cms.getComboScalar();
        this.comboColorVector = cms.getComboVector();

        this.comboColorSolid.addActionListener(this.coloringListener);
        this.comboColorDiscrete.addActionListener(this.coloringListener);
        this.comboColorScalar.addActionListener(this.coloringListener);
        this.comboColorVector.addActionListener(this.coloringListener);

        this.comboColorType.addActionListener(this.coloringListener);
        this.comboColorType.addActionListener(e ->
        {
            String selection = (String) CurvesView.this.comboColorType.getSelectedItem();

            CurvesView.this.comboColorSolid.setVisible(false);
            CurvesView.this.comboColorDiscrete.setVisible(false);
            CurvesView.this.comboColorScalar.setVisible(false);
            CurvesView.this.comboColorVector.setVisible(false);

            switch (selection)
            {
                case ColormapState.SOLID:
                    CurvesView.this.comboColorSolid.setVisible(true);
                    CurvesView.this.editColorSolid.setVisible(true);
                    CurvesView.this.editColorDiscrete.setVisible(false);
                    CurvesView.this.editColorScalar.setVisible(false);
                    CurvesView.this.autoMinMaxColor.setVisible(false);
                    break;
                case ColormapState.DISCRETE:
                    CurvesView.this.comboColorDiscrete.setVisible(true);
                    CurvesView.this.editColorSolid.setVisible(false);
                    CurvesView.this.editColorDiscrete.setVisible(true);
                    CurvesView.this.editColorScalar.setVisible(false);
                    CurvesView.this.autoMinMaxColor.setVisible(false);
                    break;
                case ColormapState.SCALAR:
                    CurvesView.this.comboColorScalar.setVisible(true);
                    CurvesView.this.editColorSolid.setVisible(false);
                    CurvesView.this.editColorDiscrete.setVisible(false);
                    CurvesView.this.editColorScalar.setVisible(true);
                    CurvesView.this.autoMinMaxColor.setVisible(true);
                    break;
                case ColormapState.VECTOR:
                    CurvesView.this.comboColorVector.setVisible(true);
                    CurvesView.this.autoMinMaxColor.setVisible(false);
                    CurvesView.this.editColorSolid.setVisible(false);
                    CurvesView.this.editColorDiscrete.setVisible(false);
                    CurvesView.this.editColorScalar.setVisible(false);
                    break;
            }

            CurvesView.this.updateColoring();
        });

        this.comboColorSolid.setVisible(false);
        this.comboColorDiscrete.setVisible(false);
        this.comboColorScalar.setVisible(false);
        this.comboColorVector.setVisible(true);

        this.comboColorAttribute.setSelectedItem(Curves.TANGENT);
        this.comboColorType.setSelectedItem(ColormapState.VECTOR);
        this.comboColorSolid.setSelectedIndex(0);
        this.comboColorDiscrete.setSelectedIndex(0);
        this.comboColorScalar.setSelectedIndex(0);
        this.comboColorVector.setSelectedIndex(0);

        this.updateColoring();
    }

    private static Pair<Double, Double> minmax(Curves curves, String attribute)
    {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (Curve curve : curves)
        {
            for (int i = 0; i < curve.size(); i++)
            {
                double value = curve.get(attribute, i).get(0);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }

        return Pair.of(min, max);
    }

    private void autoMinMaxThick()
    {
        if (CurvesView.this.hasData())
        {
            Curves curves = this.getData();
            String attribute = (String) this.comboThickAttribute.getSelectedItem();

            if (curves.has(attribute))
            {
                Pair<Double, Double> minmax = minmax(curves, attribute);
                this.transferThick.set(minmax.a, minmax.b);
                this.update = true;
                this.updateThickness();
            }
        }
    }

    private void autoMinMaxWeight()
    {
        if (CurvesView.this.hasData())
        {
            Curves curves = this.getData();
            String attribute = (String) this.comboWeightAttribute.getSelectedItem();

            if (curves.has(attribute))
            {
                Pair<Double, Double> minmax = minmax(curves, attribute);
                this.transferWeight.set(minmax.a, minmax.b);
                this.update = true;
                this.updateColoring();
            }
        }
    }

    private void autoMinMaxOpacity()
    {
        if (CurvesView.this.hasData())
        {
            Curves curves = this.getData();
            String attribute = (String) this.comboOpacityAttribute.getSelectedItem();

            if (curves.has(attribute))
            {
                Pair<Double, Double> minmax = minmax(curves, attribute);
                this.transferOpacity.set(minmax.a, minmax.b);
                this.update = true;
                this.updateColoring();
            }
        }
    }

    private void autoMinMaxColor()
    {
        if (CurvesView.this.hasData())
        {
            Curves curves = this.getData();

            String attribute = (String) this.comboColorAttribute.getSelectedItem();
            String ctype = (String) this.comboColorType.getSelectedItem();

            if (ctype.equals(ColormapState.SCALAR) && curves.has(attribute))
            {
                Pair<Double, Double> minmax = minmax(curves, attribute);

                ColormapScalar cm = (ColormapScalar) this.comboColorScalar.getSelectedItem();
                cm.withMin(minmax.a);
                cm.withMax(minmax.b);
                this.update = true;
                Viewer.getInstance().colormaps.update();
                this.updateColoring();
            }
        }
    }

    protected ControlPanel makeInfoControls()
    {
        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Type: ", new BasicLabel(this.toString()));
        infoPanel.addControl(" ", new BasicLabel());
        {
            final BasicLabel labelCurves = new BasicLabel("");
            final BasicLabel labelVertices = new BasicLabel("");
            final BasicLabel labelMin = new BasicLabel("");
            final BasicLabel labelLowQ = new BasicLabel("");
            final BasicLabel labelMedian = new BasicLabel("");
            final BasicLabel labelHighQ = new BasicLabel("");
            final BasicLabel labelMax = new BasicLabel("");
            final BasicLabel labelSum = new BasicLabel("");
            final BasicLabel labelLabel = new BasicLabel("");
            final BasicLabel labelMinX = new BasicLabel("");
            final BasicLabel labelMaxX = new BasicLabel("");
            final BasicLabel labelMinY = new BasicLabel("");
            final BasicLabel labelMaxY = new BasicLabel("");
            final BasicLabel labelMinZ = new BasicLabel("");
            final BasicLabel labelMaxZ = new BasicLabel("");

            infoPanel.addControl("Curves: ", labelCurves);
            infoPanel.addControl("Vertices: ", labelVertices);
            infoPanel.addControl("Min Length: ", labelMin);
            infoPanel.addControl("LowQ Length: ", labelLowQ);
            infoPanel.addControl("Median Length: ", labelMedian);
            infoPanel.addControl("HighQ Length: ", labelHighQ);
            infoPanel.addControl("Max Length: ", labelMax);
            infoPanel.addControl("Total Length: ", labelSum);
            infoPanel.addControl("Labels: ", labelLabel);
            infoPanel.addControl("Min X: ", labelMinX);
            infoPanel.addControl("Max X: ", labelMaxX);
            infoPanel.addControl("Min Y: ", labelMinY);
            infoPanel.addControl("Max Y: ", labelMaxY);
            infoPanel.addControl("Min Z: ", labelMinZ);
            infoPanel.addControl("Max Z: ", labelMaxZ);

            Runnable updateInfo = () ->
            {
                if (this.hasData() && this.data.size() > 0)
                {
                    int curves = CurvesView.this.data.curveCount();
                    int vertices = CurvesView.this.data.vertexCount();
                    Vect lengths = this.data.lengths();
                    VectStats stats = new VectStats().withInput(lengths).run();
                    List<Integer> idx = CurvesUtils.list(this.data, Curves.LABEL);
                    Box box = this.getBounds();

                    DecimalFormat df = new DecimalFormat("0.00##");

                    labelCurves.setText(String.valueOf(curves));
                    labelVertices.setText(String.valueOf(vertices));
                    labelMin.setText(df.format(stats.min));
                    labelLowQ.setText(df.format(stats.qlow));
                    labelMedian.setText(df.format(stats.median));
                    labelHighQ.setText(df.format(stats.qhigh));
                    labelMax.setText(df.format(stats.max));
                    labelSum.setText(df.format(stats.sum));
                    labelLabel.setText(String.valueOf(idx.size()));
                    labelMinX.setText(df.format(box.getMin().getX()));
                    labelMaxX.setText(df.format(box.getMax().getX()));
                    labelMinY.setText(df.format(box.getMin().getY()));
                    labelMaxY.setText(df.format(box.getMax().getY()));
                    labelMinZ.setText(df.format(box.getMin().getZ()));
                    labelMaxZ.setText(df.format(box.getMax().getZ()));
                }
                else
                {
                    labelCurves.setText("NA");
                    labelVertices.setText("NA");
                    labelMin.setText("NA");
                    labelLowQ.setText("NA");
                    labelMedian.setText("NA");
                    labelHighQ.setText("NA");
                    labelMax.setText("NA");
                    labelSum.setText("NA");
                    labelLabel.setText("NA");
                    labelMinX.setText("NA");
                    labelMaxX.setText("NA");
                    labelMinY.setText("NA");
                    labelMaxY.setText("NA");
                    labelMinZ.setText("NA");
                    labelMaxZ.setText("NA");
                }
            };

            this.observable.addObserver((a, b) -> updateInfo.run());

            BasicButton elem = new BasicButton("Update Info");
            elem.setToolTipText("Update the info to reflect the most current data");
            elem.addActionListener(a -> updateInfo.run());

            infoPanel.addControl(elem);
        }

        return infoPanel;
    }

    protected Map<String, ControlPanel> makeEditControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();
        {
            ControlPanel panel = new ControlPanel();

            List<Runnable> resets = Lists.newArrayList();

            {
                JCheckBox limitCheckBox = new JCheckBox();
                limitCheckBox.setToolTipText("When checked, a limited number of curves will be shown");
                limitCheckBox.setSelected(this.limit);
                limitCheckBox.addItemListener(e ->
                {
                    this.limit = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                    Logging.info("using limit: " + this.limit);
                });
                panel.addControl("Limit", limitCheckBox);
            }
            {
                final BasicSpinner spin = new BasicSpinner(new SpinnerNumberModel(this.maxnum, 1, 10000000, 1));
                spin.addChangeListener(e ->
                {
                    this.maxnum = Integer.valueOf(spin.getValue().toString());
                    this.update = true;
                });
                resets.add(() ->
                {

                    spin.setValue(DEF_MAX_NUM);
                    this.maxnum = DEF_MAX_NUM;
                });

                panel.addControl("Max num", spin);
            }
            {
                final BasicButton elem = new BasicButton("Shuffle Subset");
                elem.addActionListener(e ->
                {
                    Logging.info("Shuffling which");
                    this.subset = null;
                    this.update = true;
                });
                panel.addControl("", elem);
            }

            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("specify the search radius for selecting curves");
                elem.setValue(new Double(this.precision));
                elem.addPropertyChangeListener("value", e ->
                {
                    double nprecision = ((Number) elem.getValue()).doubleValue();
                    if (nprecision != CurvesView.this.precision)
                    {
                        CurvesView.this.precision = nprecision;
                        CurvesView.this.update = true;
                        Logging.info("updated precision to " + CurvesView.this.precision);
                    }
                });
                panel.addControl("Precision", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField();
                elem.setValue(this.name);
                elem.setToolTipText("Specify the label attribute");
                elem.addPropertyChangeListener("value", e ->
                {
                    String nname = (String) elem.getValue();
                    if (nname != CurvesView.this.name)
                    {
                        CurvesView.this.name = nname;
                        CurvesView.this.update = true;
                        Logging.info("updated name to " + CurvesView.this.name);
                    }
                });
                resets.add(() ->
                {
                    this.name = Curves.LABEL;
                    elem.setValue(this.name);
                });
                panel.addControl("Label Attr", elem);
            }
            {
                final BasicSpinner spin = new BasicSpinner(new SpinnerNumberModel(this.label, 0, 10000, 1));

                spin.addChangeListener(e ->
                {
                    this.label = Integer.valueOf(spin.getValue().toString());
                    this.update = true;
                });
                resets.add(() ->
                {
                    this.label = 0;
                    spin.setValue(this.label);
                });
                panel.addControl("Label Index", spin);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField();
                elem.setValue(this.which);
                elem.setToolTipText("Specify which labels to render, e.g. 0,2:10");
                elem.addPropertyChangeListener("value", e ->
                {
                    String nwhich = (String) elem.getValue();
                    if (nwhich != this.which)
                    {
                        this.which = nwhich;
                        this.update = true;
                        Logging.info("updated which to " + this.which);
                    }
                });
                resets.add(() ->
                {
                    this.which = "";
                    elem.setValue(this.which);
                });
                panel.addControl("Which Label", elem);
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Solids, true, true);
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                    SolidsView solids = (SolidsView) entry.getValue();

                    if (!JavaUtils.equals(solids, CurvesView.this.solidsClip))
                    {
                        this.setSolidsClip(solids);
                    }
                });
                resets.add(() ->
                {
                    this.setSolidsClip(null);
                    elem.setSelectedItem(Viewables.NONE);
                });
                panel.addControl("Solids Clip", elem);
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Solids, true, true);
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                    SolidsView solids = (SolidsView) entry.getValue();

                    if (!JavaUtils.equals(solids, this.solidsInclude))
                    {
                        this.setSolidsInclude(solids);
                    }
                });
                resets.add(() ->
                {
                    this.setSolidsInclude(null);
                    elem.setSelectedItem(Viewables.NONE);
                });
                panel.addControl("Solids Include", elem);
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Solids, true, true);
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                    SolidsView solids = (SolidsView) entry.getValue();

                    if (!JavaUtils.equals(solids, CurvesView.this.solidsExclude))
                    {
                        this.setSolidsExclude(solids);
                    }
                });
                resets.add(() ->
                {
                    this.setSolidsExclude(null);
                    elem.setSelectedItem(Viewables.NONE);
                });
                panel.addControl("Solids Exclude", elem);
            }
            {
                final BasicButton button = new BasicButton("Reset Filtering");
                button.setToolTipText("Reset the curves filtering options to show all of the data");
                {
                    button.addActionListener(e ->
                    {
                        for (Runnable reset : resets)
                        {
                            reset.run();
                        }
                        Viewer.getInstance().gui.setStatusMessage("reset curves filtering settings to default");
                    });
                }
                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Retain Selected Curves");
                button.setToolTipText("Remove curves curves that do not meet the selection criteria from the underlying dataset");
                button.addActionListener(e ->
                {
                    CurvesView.this.data.keep(which(false));

                    if (CurvesView.this.solidsClip != null && CurvesView.this.solidsClip.hasData())
                    {
                        CurvesCrop crop = new CurvesCrop();
                        crop.input = CurvesView.this.data;
                        crop.solids = CurvesView.this.solidsClip.getData();
                        CurvesView.this.data = crop.run().output;
                    }

                    this.update = true;
                });

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Export Selected Curves");
                button.setToolTipText("Take the selected curves and export them to a new dataset in your workspace");
                button.addActionListener(arg0 ->
                {
                    Curves selection = CurvesView.this.data.copy(which(false));

                    if (CurvesView.this.solidsClip != null && CurvesView.this.solidsClip.hasData())
                    {
                        CurvesCrop crop = new CurvesCrop();
                        crop.input = selection;
                        crop.solids = CurvesView.this.solidsClip.getData();
                        crop.and = true;
                        selection = crop.run().output;
                    }

                    String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Specify a name", "curves.selection");
                    CurvesView viewable = new CurvesView();
                    viewable.setData(selection);
                    viewable.setName(name);
                    Viewer.getInstance().qviewables.offer(viewable);
                });

                panel.addControl(button);
            }
            {
                JCheckBox endpointsCheckBox = new JCheckBox();
                endpointsCheckBox.setToolTipText("When checked, a only endpoints will be used for inclusion and exclusion");
                endpointsCheckBox.setSelected(this.endpoints);
                endpointsCheckBox.addItemListener(e ->
                {
                    this.endpoints = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                    Logging.info("using endpoints: " + this.endpoints);
                });
                panel.addControl("Endpoints", endpointsCheckBox);
            }
            controls.put("Filtering", panel);
        }
        {
            ControlPanel panel = new ControlPanel();

            String modeExport = "Export";
            String modeReplace = "Replace";

            BasicComboBox modeComboBox = new BasicComboBox();
            modeComboBox.addItem(modeExport);
            modeComboBox.addItem(modeReplace);
            panel.addControl("Mode", modeComboBox);

            final Supplier<Boolean> replace = () -> modeComboBox.getSelectedItem().equals(modeReplace);

            for (ViewableAction action : ViewableActions.Curves)
            {
                final BasicButton elem = new BasicButton(action.getName());
                elem.setToolTipText(action.getDescription());
                elem.addActionListener(e -> action.getAction(replace.get()).accept(this));
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
            ControlPanel panel = new ControlPanel();
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("render endpoints for each curve");
                elem.setSelected(this.showEnds);
                elem.addItemListener(e ->
                {
                    this.showEnds = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                });
                panel.addControl("Show Endpoints", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("render heads for each curve");
                elem.setSelected(this.showHeads);
                elem.addItemListener(e ->
                {
                    this.showHeads = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                });
                panel.addControl("Show Heads", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("render tails for each curve");
                elem.setSelected(this.showTails);
                elem.addItemListener(e ->
                {
                    this.showTails = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                });
                panel.addControl("Show Tails", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("render points for each curve vertex");
                elem.setSelected(this.showPoints);
                elem.addItemListener(e ->
                {
                    this.showPoints = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                });
                panel.addControl("Show Points", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("render lines that connect vertices");
                elem.setSelected(this.showLines);
                elem.addItemListener(e ->
                {
                    this.showLines = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                });
                panel.addControl("Show Lines", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("render 3D tubes depicting the curves");
                elem.setSelected(this.showTubes);
                elem.addItemListener(e ->
                {
                    this.showTubes = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                });
                panel.addControl("Show Tubes", elem);
            }
            {
                final JPanel combos = new JPanel();
                combos.add(this.comboColorSolid);
                combos.add(this.comboColorDiscrete);
                combos.add(this.comboColorScalar);
                combos.add(this.comboColorVector);

                panel.addControl("Color Attribute", this.comboColorAttribute);
                panel.addControl("Color Type", this.comboColorType);
                panel.addControl("Color Map", combos);
                panel.addControl(this.editColorSolid);
                panel.addControl(this.editColorDiscrete);
                panel.addControl(this.editColorScalar);
                panel.addControl(this.autoMinMaxColor);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setValue(new Double(this.wash));
                elem.addPropertyChangeListener("value", e ->
                {
                    double nwash = ((Number) elem.getValue()).doubleValue();
                    if (nwash != this.wash)
                    {
                        this.wash = nwash;
                        this.update = true;
                        this.updateColoring();
                        Logging.info("updated wash to " + CurvesView.this.wash);
                    }
                });
                panel.addControl("Wash", elem);
            }
            {
                SpinnerNumberModel model = new SpinnerNumberModel(this.pointWidth, MIN_WIDTH, MAX_WIDTH, STEP_WIDTH);
                final BasicSpinner elem = new BasicSpinner(model);
                elem.addChangeListener(e ->
                {
                    int value = Integer.valueOf(elem.getValue().toString());
                    if (value != this.pointWidth)
                    {
                        this.pointWidth = value;
                        this.update = true;
                    }
                });
                panel.addControl("Point Width", elem);
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
                panel.addControl("Line Width", elem);
            }
            {
                final JSlider elem = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(100 * this.opacity));
                elem.addChangeListener(e ->
                {
                    this.opacity = elem.getModel().getValue() / 100.;
                    this.update = true;
                });
                panel.addControl("Line Opacity", elem);
            }
            {
                SpinnerNumberModel model = new SpinnerNumberModel(this.tuber.resolution, MIN_RES, MAX_RES, STEP_RES);
                final BasicSpinner elem = new BasicSpinner(model);
                elem.addChangeListener(e ->
                {
                    int value = Integer.valueOf(elem.getValue().toString());
                    if (value != CurvesView.this.tuber.resolution)
                    {
                        this.tuber.resolution = value;
                        this.update = true;
                    }
                });
                panel.addControl("Tube detail", elem);
            }
            {
                final BasicFormattedTextField hfield = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                hfield.setValue(new Double(this.tuber.fthick));
                hfield.addPropertyChangeListener("value", e ->
                {
                    double value = ((Number) hfield.getValue()).doubleValue();
                    if (value != CurvesView.this.tuber.fthick)
                    {
                        this.tuber.fthick = value;
                        this.update = true;
                    }
                });
                panel.addControl("Tube Thickness", hfield);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("smooth the endcaps of the tubes");
                elem.setSelected(this.smoothCaps);
                elem.addItemListener(e ->
                {
                    this.smoothCaps = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                });
                panel.addControl("Smooth Caps", elem);
            }
            {
                panel.addControl("Thickness Attribute", this.comboThickAttribute);
                final BasicButton button = new BasicButton("Edit Thickness Transfer...");
                button.addActionListener(e -> this.transferThick.show());
                panel.addControl(button);
                panel.addControl(this.autoMinMaxThick);
            }
            {
                panel.addControl("Weight Attribute", this.comboWeightAttribute);
                final BasicButton button = new BasicButton("Edit Weight Transfer...");
                button.addActionListener(e -> this.transferWeight.show());
                panel.addControl(button);
                panel.addControl(this.autoMinMaxWeight);
            }
            {
                panel.addControl("Opacity Attribute", this.comboOpacityAttribute);
                final BasicButton button = new BasicButton("Edit Opacity Transfer...");
                button.addActionListener(e -> this.transferOpacity.show());
                panel.addControl(button);
                panel.addControl(this.autoMinMaxOpacity);
            }

            controls.put("Curves", panel);
        }

        controls.put("Tubes", this.render.getPanel());

        return controls;
    }

    public void setSolidsClip(SolidsView a)
    {
        if (this.solidsClip != null)
        {
            this.solidsClip.observable.deleteObserver(this.changeObserver);
        }

        // can be null
        this.solidsClip = a;

        if (this.solidsClip != null)
        {
            this.solidsClip.observable.addObserver(this.changeObserver);
        }

        this.update = true;
    }

    public void setSolidsInclude(SolidsView a)
    {
        if (this.solidsInclude != null)
        {
            this.solidsInclude.observable.deleteObserver(this.changeObserver);
        }

        // can be null
        this.solidsInclude = a;

        if (this.solidsInclude != null)
        {
            this.solidsInclude.observable.addObserver(this.changeObserver);
        }

        this.update = true;
    }

    public void setSolidsExclude(SolidsView a)
    {
        if (this.solidsExclude != null)
        {
            this.solidsExclude.observable.deleteObserver(this.changeObserver);
        }

        // can be null
        this.solidsExclude = a;

        if (this.solidsExclude != null)
        {
            this.solidsExclude.observable.addObserver(this.changeObserver);
        }

        this.update = true;
    }

    private void updateTangents()
    {
        if (this.data != null)
        {
            this.lock.lock();

            try
            {
                CurvesUtils.attrSetTangent(this.data);
            }
            finally
            {
                this.lock.unlock();
            }
        }
    }

    private void updateThickness()
    {
        if (this.data != null)
        {
            this.lock.lock();

            try
            {
                this.data.add(Curves.THICKNESS, VectSource.create1D(1));
                String attribute = (String) this.comboThickAttribute.getSelectedItem();

                if (this.data.has(attribute))
                {
                    VectFunction function = this.transferThick.toFunction();
                    int dattr = this.data.dim(attribute);
                    int dthick = function.getDimIn();
                    if (attribute == null || dthick != dattr)
                    {
                        function = VectFunctionSource.constant(dattr, VectSource.createND(dthick, 1)).compose(function);
                    }

                    // apply function
                    new CurvesFunctionApply().withCurves(this.data).withFunction(function).withInput(attribute).withOutput(Curves.THICKNESS).run();
                }
                else
                {
                    for (Curve curve : this.data)
                    {
                        for (int i = 0; i < curve.size(); i++)
                        {
                            curve.set(Curves.THICKNESS, i, VectSource.create1D(1));
                        }
                    }
                }

                // signal a change
                this.update = true;
            }
            finally
            {
                this.lock.unlock();
            }
        }

    }

    private void updateColoring()
    {
        if (this.data != null)
        {
            this.lock.lock();

            try
            {
                this.data.add(Curves.COLOR, VectSource.create4D(1, 1, 1, 1));
                String attributeColor = (String) this.comboColorAttribute.getSelectedItem();
                String ctype = (String) this.comboColorType.getSelectedItem();

                VectFunction coloring = null;
                switch (ctype)
                {
                    case ColormapState.SOLID:
                        coloring = ((ColormapSolid) this.comboColorSolid.getSelectedItem()).getFunction();
                        break;
                    case ColormapState.DISCRETE:
                        coloring = ((ColormapDiscrete) this.comboColorDiscrete.getSelectedItem()).getFunction();
                        break;
                    case ColormapState.SCALAR:
                        coloring = ((ColormapScalar) this.comboColorScalar.getSelectedItem()).getFunction();
                        break;
                    case ColormapState.VECTOR:
                        coloring = ((ColormapVector) this.comboColorVector.getSelectedItem()).getFunction();
                        break;
                    default:
                        Logging.error("invalid colortype");
                }

                if (attributeColor == null)
                {
                    attributeColor = Curves.TANGENT;
                }

                if (!this.data.has(Curves.TANGENT))
                {
                    CurvesUtils.attrSetTangent(this.data);
                }

                if (this.data.has(attributeColor))
                {
                    String attributeWeight = (String) this.comboWeightAttribute.getSelectedItem();
                    String attributeOpacity = (String) this.comboOpacityAttribute.getSelectedItem();

                    VectFunction washer = VectFunctionSource.wash(this.wash);
                    VectFunction weighter = this.transferWeight.toFunction();
                    VectFunction opacer = this.transferOpacity.toFunction();

                    int dattr = this.data.dim(attributeColor);
                    int dcolor = coloring.getDimIn();

                    for (Curve curve : this.data)
                    {
                        for (int i = 0; i < curve.size(); i++)
                        {
                            Vect data = curve.get(attributeColor, i);
                            if (dattr != dcolor)
                            {
                                data = VectSource.createND(dcolor, 1).times(data.get(0));
                            }

                            Vect color = washer.apply(coloring.apply(data));

                            if (this.data.has(attributeWeight))
                            {
                                Vect dataWeight = curve.get(attributeWeight, i);
                                Vect scalarWeight = data.size() == 1 ? dataWeight : VectSource.create1D(dataWeight.norm());
                                double weight = weighter.apply(scalarWeight).get(0);
                                color.set(0, color.get(0) * weight);
                                color.set(1, color.get(1) * weight);
                                color.set(2, color.get(2) * weight);
                            }

                            if (this.data.has(attributeOpacity))
                            {
                                Vect dataOpacity = curve.get(attributeOpacity, i);
                                Vect scalarOpacity = data.size() == 1 ? dataOpacity : VectSource.create1D(dataOpacity.norm());
                                double opac = opacer.apply(scalarOpacity).get(0);
                                color.set(3, color.get(3) * opac);
                            }

                            curve.set(Curves.COLOR, i, color);
                        }
                    }

                    // signal a change
                    this.update = true;
                }
                else
                {
                    Logging.info("warning: failed to find curves attribute " + attributeColor);
                }
            }
            finally
            {
                this.lock.unlock();
            }
        }
    }

    private boolean[] which(boolean subset)
    {
        Solids include = null;
        if (this.solidsInclude != null && this.solidsInclude.hasData())
        {
            include = this.solidsInclude.getData();
        }

        Solids exclude = null;
        if (this.solidsExclude != null && this.solidsExclude.hasData())
        {
            exclude = this.solidsExclude.getData();
        }

        Set<Integer> whichidx = Sets.newHashSet(CliUtils.parseWhich(this.which));

        boolean[] out = new boolean[this.data.size()];
        for (int i = 0; i < this.data.size(); i++)
        {
            Curve curve = this.data.get(i);

            if (subset && this.subset != null && !this.subset.contains(i))
            {
                continue;
            }

            if (curve.size() == 0)
            {
                continue;
            }

            if (this.label > 0 && this.data.has(this.name))
            {
                int clabel = (int) Math.round(curve.get(this.name, 0).get(0));
                if (clabel != this.label)
                {
                    continue;
                }
            }

            if (whichidx.size() > 0)
            {
                int clabel = (int) Math.round(curve.get(this.name, 0).get(0));
                if (!whichidx.contains(clabel))
                {
                    continue;
                }
            }

            if (this.endpoints)
            {
                if (include != null && !(include.contains(curve.getHead()) || include.contains(curve.getTail())))
                {
                    continue;
                }

                if (exclude != null && (exclude.contains(curve.getHead()) || exclude.contains(curve.getTail())))
                {
                    continue;
                }
            }
            else
            {
                if (include != null && !include.contains(curve.getAll()))
                {
                    continue;
                }

                if (exclude != null && exclude.contains(curve.getAll()))
                {
                    continue;
                }
            }

            out[i] = true;
        }

        return out;
    }

    public void display(GL2 gl)
    {
        if (this.lock.tryLock())
        {
            try
            {
                int count = this.data.size();
                if (this.limit && this.maxnum < count)
                {
                    boolean change = false;

                    if (this.subset == null)
                    {
                        this.subset = Sets.newHashSet();
                    }

                    while (this.subset.size() < this.maxnum)
                    {
                        this.subset.add(Global.RANDOM.nextInt(count));
                        change = true;
                    }

                    while (this.subset.size() > this.maxnum)
                    {
                        int diff = this.subset.size() - this.maxnum;

                        Set<Integer> ridx = Sets.newHashSet();
                        while (ridx.size() < diff)
                        {
                            ridx.add(Global.RANDOM.nextInt(diff));
                        }

                        Set<Integer> relem = Sets.newHashSet();
                        int idx = 0;
                        for (Integer elem : this.subset)
                        {
                            if (ridx.contains(idx))
                            {
                                relem.add(elem);
                            }
                            idx++;
                        }

                        this.subset.removeAll(relem);

                        change = true;
                    }

                    if (change)
                    {
                        this.update = true;
                    }
                }
                else
                {
                    if (this.subset != null)
                    {
                        this.subset = null;
                        this.update = true;
                    }
                }

                if (this.update && this.list != null)
                {
                    gl.glDeleteLists(this.list, 1);
                    this.list = null;
                    this.update = false;
                }

                gl.glEnable(GL2.GL_BLEND);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
                gl.glEnable(GL2.GL_ALPHA_TEST);
                gl.glAlphaFunc(GL2.GL_ALWAYS, 0.0f);

                if (this.list == null)
                {
                    int idx = gl.glGenLists(1);
                    if (idx == 0)
                    {
                        Logging.info("warning: failed to create display list");
                        return;
                    }

                    this.list = idx;
                    gl.glNewList(idx, GL2.GL_COMPILE);

                    this.shown = which(true);
                    for (int i = 0; i < this.data.size(); i++)
                    {
                        if (this.shown[i])
                        {
                            this.render(gl, this.data.get(i), this.selected.contains(i));
                        }
                    }
                    gl.glEndList();
                }

                if (this.list != null)
                {
                    gl.glCallList(this.list);
                }
            }
            finally
            {
                this.lock.unlock();
            }
        }
    }

    public void render(GL2 gl, Curve curve, boolean selected)
    {
        this.tuber.smooth = this.smoothCaps;

        boolean hasThick = curve.has(Curves.THICKNESS);

        if ((this.showHeads || this.showTails || this.showEnds) && curve.size() > 1)
        {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_POINT_SMOOTH);

            Vect head = curve.getHead();
            Vect tail = curve.getTail();

            boolean headDraw = this.showHeads || this.showEnds;
            boolean tailDraw = this.showTails || this.showEnds;

            if (this.solidsClip != null && this.solidsClip.hasData())
            {
                Solids clip = this.solidsClip.getData();
                headDraw = clip.containsAll(head);
                tailDraw = clip.containsAll(tail);
            }

            Vect headColor = selected ? SELECTED_COLOR : curve.has(Curves.COLOR) ? curve.get(Curves.COLOR, 0) : DEFAULT_COLOR;
            Vect tailColor = selected ? SELECTED_COLOR : curve.has(Curves.COLOR) ? curve.get(Curves.COLOR, curve.size() - 1) : DEFAULT_COLOR;

            int headSize = hasThick ? (int) (curve.get(Curves.THICKNESS, 0).get(0) * this.pointWidth) : this.pointWidth;
            int tailSize = hasThick ? (int) (curve.get(Curves.THICKNESS, curve.size() - 1).get(0) * this.pointWidth) : this.pointWidth;

            if (headDraw)
            {
                float headRed = (float) headColor.get(0);
                float headGreen = (float) headColor.get(1);
                float headBlue = (float) headColor.get(2);
                float headAlpha = (float) (headColor.get(3) * this.opacity);

                float headX = (float) head.get(0);
                float headY = (float) head.get(1);
                float headZ = (float) head.get(2);

                gl.glPointSize(headSize);
                gl.glBegin(GL2.GL_POINTS);
                gl.glColor4f(headRed, headGreen, headBlue, headAlpha);
                gl.glVertex3f(headX, headY, headZ);
                gl.glEnd();
            }

            if (tailDraw)
            {
                float tailRed = (float) tailColor.get(0);
                float tailGreen = (float) tailColor.get(1);
                float tailBlue = (float) tailColor.get(2);
                float tailAlpha = (float) (tailColor.get(3) * this.opacity);

                float tailX = (float) tail.get(0);
                float tailY = (float) tail.get(1);
                float tailZ = (float) tail.get(2);

                gl.glPointSize(tailSize);
                gl.glBegin(GL2.GL_POINTS);
                gl.glColor4f(tailRed, tailGreen, tailBlue, tailAlpha);
                gl.glVertex3f(tailX, tailY, tailZ);
                gl.glEnd();
            }
        }

        if (this.showPoints)
        {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_POINT_SMOOTH);

            for (int i = 0; i < curve.size(); i++)
            {
                Vect vect = curve.get(i);
                int size = hasThick ? (int) (curve.get(Curves.THICKNESS, i).get(0) * this.pointWidth) : this.pointWidth;

                if (this.solidsClip != null && this.solidsClip.hasData())
                {
                    Solids clip = this.solidsClip.getData();
                    if (!clip.containsAll(vect))
                    {
                        continue;
                    }
                }

                Vect color = selected ? SELECTED_COLOR : curve.has(Curves.COLOR) ? curve.get(Curves.COLOR, i) : DEFAULT_COLOR;

                float red = (float) color.get(0);
                float green = (float) color.get(1);
                float blue = (float) color.get(2);
                float alpha = (float) (color.get(3) * this.opacity);

                float x = (float) vect.get(0);
                float y = (float) vect.get(1);
                float z = (float) vect.get(2);

                gl.glPointSize(size);
                gl.glBegin(GL2.GL_POINTS);
                gl.glColor4f(red, green, blue, alpha);
                gl.glVertex3f(x, y, z);
                gl.glEnd();
            }

        }

        if (this.showLines)
        {
            Vect ppos = curve.get(0);
            Vect pcolor = selected ? SELECTED_COLOR : curve.has(Curves.COLOR) ? curve.get(Curves.COLOR, 0) : DEFAULT_COLOR;

            gl.glDisable(GL2.GL_LIGHTING);

            gl.glLineWidth((float) this.lineWidth);

            Solids clip = this.solidsClip == null || !this.solidsClip.hasData() ? null : this.solidsClip.getData();

            BiConsumer<Vect, Vect> vertex = (color, pos) ->
            {
                float pr = (float) color.get(0);
                float pg = (float) color.get(1);
                float pb = (float) color.get(2);
                float pa = (float) (color.get(3) * this.opacity);

                float px = (float) pos.get(0);
                float py = (float) pos.get(1);
                float pz = (float) pos.get(2);

                gl.glColor4f(pr, pg, pb, pa * (float) this.opacity);
                gl.glVertex3f(px, py, pz);
            };

            boolean open = false;

            if (clip == null || clip.containsAll(ppos))
            {
                gl.glBegin(GL2.GL_LINE_STRIP);
                vertex.accept(pcolor, ppos);
                open = true;
            }

            for (int j = 1; j < curve.size(); j++)
            {
                Vect pos = curve.get(j);

                if (clip != null && !clip.containsAll(pos))
                {
                    if (open)
                    {
                        gl.glEnd();
                        open = false;
                    }

                    continue;
                }

                if (!open)
                {
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    open = true;
                }

                Vect color = selected ? SELECTED_COLOR : curve.has(Curves.COLOR) ? curve.get(Curves.COLOR, j) : DEFAULT_COLOR;
                vertex.accept(color, pos);
            }

            if (open)
            {
                gl.glEnd();
            }
        }

        if (this.showTubes)
        {
            if (this.solidsClip != null && this.solidsClip.hasData())
            {
                if (curve.size() > 0)
                {
                    CurvesCrop crop = new CurvesCrop();
                    crop.input = new Curves(curve);
                    crop.solids = this.solidsClip.getData();
                    crop.and = true;
                    for (Curve cropped : crop.run().output)
                    {
                        this.render.render(gl, this.tuber.single(cropped));
                    }
                }
            }
            else
            {
                this.render.render(gl, this.tuber.single(curve));
            }
        }

    }

    public Double dist(WorldMouse mouse)
    {
        if (!this.hasData() || mouse.press == null)
        {
            return null;
        }

        double mindist = Double.MAX_VALUE;
        Line pressLine = Line.fromTwoPoints(mouse.press.point, mouse.press.hit);

        boolean endpoints = this.showEnds && !(this.showLines || this.showPoints || this.showTubes);
        for (int i = 0; i < this.data.size(); i++)
        {
            if (this.shown == null || this.shown[i])
            {
                Curve curve = this.data.get(i);
                for (int j = 0; j < curve.size(); j++)
                {
                    if (endpoints && j > 1 && j < curve.size() - 1)
                    {
                        continue;
                    }

                    Vect pos = curve.get(j);
                    Pair<Double, Vect> param = pressLine.nearestParam(pos);
                    double distLine = param.b.dist(pos);

                    if (param.a > 0 && distLine < this.precision)
                    {
                        mindist = Math.min(mouse.press.point.dist(pos), mindist);
                    }
                }
            }
        }

        return mindist;
    }

    private Interaction parse(WorldMouse mouse, String mode)
    {
        for (Interaction i : Interaction.values())
        {
            if (i.toString().equals(mode))
            {
                return i;
            }
        }

        if (Constants.INTERACTION_ROTATE.equals(mode))
        {
            if (mouse.shift && mouse.control)
            {
                return Interaction.Remove;
            }
            else if (mouse.shift || mouse.control)
            {
                return Interaction.Query;
            }
            else
            {
                return Interaction.None;
            }
        }

        return Interaction.None;
    }

    public List<String> modes()
    {
        List<String> out = Lists.newArrayList();
        out.add(Interaction.Query.toString());
        out.add(Interaction.Remove.toString());
        return out;
    }

    public void handle(WorldMouse mouse, String mode)
    {
        if (this.lock.tryLock())
        {
            try
            {
                if (!this.hasData() || mouse.current == null || !mouse.pick)
                {
                    this.selected.clear();
                    return;
                }

                Interaction inter = parse(mouse, mode);

                Vect hit = mouse.current.hit;

                Sphere selector = Sphere.fromPointRadius(hit, this.precision);
                Map<Integer, Map<String, Double>> found = Maps.newHashMap();

                Integer closestVertexIdx = null;
                Integer closestCurveIdx = null;
                Double closestCurveDist = null;

                for (int i = 0; i < this.data.size(); i++)
                {
                    if (this.shown == null || this.shown[i])
                    {
                        Curve curve = this.data.get(i);

                        // find the closest vertex
                        Integer minIdx = null;
                        double minDist = Double.MAX_VALUE;

                        for (int j = 0; j < curve.size(); j++)
                        {
                            Vect pos = curve.get(j);

                            if (selector.contains(pos))
                            {
                                double dist = hit.dist(pos);
                                if (dist < minDist)
                                {
                                    minIdx = j;
                                    minDist = dist;
                                }
                            }
                        }

                        if (minIdx != null)
                        {
                            Map<String, Double> map = Maps.newHashMap();
                            for (String attr : this.data.names())
                            {
                                Vect v = curve.get(attr, minIdx);
                                if (v.size() == 1)
                                {
                                    map.put(attr, v.get(0));
                                }
                            }

                            found.put(i, map);

                            if (closestCurveDist == null || minDist < closestCurveDist)
                            {
                                closestVertexIdx = minIdx;
                                closestCurveDist = minDist;
                                closestCurveIdx = i;
                            }
                        }
                    }
                }

                this.selected.clear();
                this.selected.addAll(found.keySet());
                this.touchData();

                if (found.keySet().size() > 0)
                {
                    if (inter == Interaction.Remove)
                    {
                        if (mouse.press != null)
                        {
                            this.data.remove(found.keySet());
                            this.touchData();

                            Viewer.getInstance().control.setStatusMessage(String.format("removed %d curves", found.keySet().size()));
                        }
                        else
                        {
                            Viewer.getInstance().gui.setStatusMessage("clicking will delete the selected curves");
                            return;
                        }
                    }
                    else if (inter == Interaction.Query)
                    {
                        if (mouse.press != null)
                        {
                            StringBuilder builder = new StringBuilder();
                            builder.append("\n");
                            builder.append("\n");

                            {
                                builder.append("Closest vertex:");

                                builder.append(String.format("  curve index: %d\n", closestCurveIdx));
                                builder.append(String.format("  vertex index: %d\n", closestVertexIdx));
                                Map<String, Double> map = found.get(closestCurveIdx);
                                for (String name : map.keySet())
                                {
                                    builder.append(String.format("  %s: %g\n", name, map.get(name)));
                                }
                                builder.append("\n");
                            }

                            Viewer.getInstance().control.setStatusMessage(builder.toString());
                        }
                        else
                        {
                            Viewer.getInstance().gui.setStatusMessage("clicking will print information about the selected curves");
                        }
                    }
                }
            }
            finally
            {
                this.lock.unlock();
            }
        }
    }
}
