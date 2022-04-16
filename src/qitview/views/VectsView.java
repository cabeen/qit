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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.jogamp.opengl.GL2;
import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliUtils;
import qit.base.structs.Named;
import qit.base.structs.ObservableInstance;
import qit.base.structs.Pair;
import qit.base.utils.JavaUtils;
import qit.data.datasets.Mesh;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.utils.MeshUtils;
import qit.math.source.SelectorSource;
import qit.math.source.VectFunctionSource;
import qit.math.structs.Box;
import qit.math.structs.Containable;
import qit.math.structs.Line;
import qit.math.structs.LineIntersection;
import qit.math.structs.Plane;
import qit.math.structs.VectFunction;
import qit.math.utils.colormaps.ColormapDiscrete;
import qit.math.utils.colormaps.ColormapScalar;
import qit.math.utils.colormaps.ColormapSolid;
import qit.math.utils.colormaps.ColormapVector;
import qitview.main.Constants;
import qitview.main.Viewer;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.models.WorldMouse;
import qitview.render.RenderGeometry;
import qitview.render.RenderGlyph;
import qitview.render.RenderUtils;
import qitview.render.RenderVectsGlyph;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicLabel;
import qitview.widgets.BasicSpinner;
import qitview.widgets.BasicTable;
import qitview.widgets.ColormapState;
import qitview.widgets.ControlPanel;
import qitview.widgets.SwingUtils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.Set;

public class VectsView extends AbstractView<Vects>
{
    private static final Vect RED = VectSource.create4D(1.0, 0.0, 0.0, 1.0);
    private static final Vect WHITE = VectSource.create4D(1.0, 1.0, 1.0, 1.0);
    public static final String GLYPH = "Glyph";
    public static final String NONE = "None";
    public static final String PROTO = "                  ";

    private static final int MIN_WIDTH = 1;
    private static final int MAX_WIDTH = 25;
    private static final int STEP_WIDTH = 1;

    private enum Interaction
    {
        None, List, Select, Move, Remove, Add
    }

    private static class VectPick
    {
        Vect pos;
        Integer idx;

        public VectPick(Vect pos, Integer idx)
        {
            this.pos = pos;
            this.idx = idx;
        }
    }

    private transient Integer list = null;
    private transient boolean update = false;

    public transient ObservableInstance viewObservable = new ObservableInstance();

    private transient RenderGeometry render;
    private transient RenderVectsGlyph renderGlyphs;
    private transient Map<String, Pair<JPanel, RenderGlyph>> glyphs = Maps.newLinkedHashMap();

    private transient BasicComboBox<String> glyphCombo;
    private transient final VectsTableModel model = new VectsTableModel();

    private transient BasicComboBox<String> comboColorType;
    private transient BasicComboBox<ColormapSolid> comboColorSolid;
    private transient BasicComboBox<ColormapDiscrete> comboColorDiscrete;
    private transient BasicComboBox<ColormapScalar> comboColorScalar;
    private transient BasicComboBox<ColormapVector> comboColorVector;
    private transient BasicButton autoMinMax;
    private transient VectPick picked = null;
    private transient boolean block = false;

    private transient AffineView affine;
    private transient SolidsView solidsInclude;
    private transient SolidsView solidsExclude;

    private transient Set<Integer> subset = null;
    private transient boolean[] shown = null;

    public Boolean ballShow = false;
    public Boolean ballRadiusModulate = false;
    public int ballRadiusIndex = 0;
    public Boolean pointShow = true;
    public Boolean glyphShow = false;
    public Boolean indexColor = true;
    public double wash = 0.0f;
    public Float ballRadius = 2f;
    public int pointWidth = 4;
    public int resolution = 2;
    public int period = 0;
    public String which = "";
    public boolean limit = true;
    public int maxnum = 1000;
    public double opacity = 1f;
    public int xCoordIndex = 0;
    public int yCoordIndex = 1;
    public int zCoordIndex = 2;
    public int attrStartIndex = 3;
    public int attrEndIndex = -1;
    public int glyphStartIndex = 3;
    public int glyphEndIndex = -1;

    private transient Runnable updater = () ->
    {
        this.update = true;
        this.renderGlyphs.update();
    };

    private transient Observer updateObserver = (a, b) -> this.updater.run();

    public VectsView()
    {
        super();

        this.initColoring();


        this.render = new RenderGeometry(() -> { this.update = true; });
        this.render.setMeshSmooth(true);

        this.renderGlyphs = new RenderVectsGlyph(this);

        for (Pair<String, RenderGlyph> pair : RenderGlyph.getAll(() -> this.renderGlyphs.update()))
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(pair.b.getPanel());
            panel.setVisible(false);

            this.glyphs.put(pair.a, Pair.of(panel, pair.b));
        }

        super.initPanel();
    }

    private void initColoring()
    {
        Viewer.getInstance().colormaps.getObservable().addObserver(this.updateObserver);

        final ActionListener listener = e ->
        {
            this.update = true;
        };

        this.autoMinMax = new BasicButton("Auto Min/Max");
        this.autoMinMax.addActionListener(e -> this.autoMinMax());

        this.comboColorType = new BasicComboBox<>();
        this.comboColorType.addItem(ColormapState.SOLID);
        this.comboColorType.addItem(ColormapState.DISCRETE);
        this.comboColorType.addItem(ColormapState.SCALAR);
        this.comboColorType.addItem(ColormapState.VECTOR);

        this.comboColorType.setToolTipText("specify the type of colormap to use for vect coloring");

        final ColormapState cms = Viewer.getInstance().colormaps;
        this.comboColorSolid = cms.getComboSolid();
        this.comboColorDiscrete = cms.getComboDiscrete();
        this.comboColorScalar = cms.getComboScalar();
        this.comboColorVector = cms.getComboVector();

        this.comboColorSolid.addActionListener(listener);
        this.comboColorDiscrete.addActionListener(listener);
        this.comboColorScalar.addActionListener(listener);
        this.comboColorVector.addActionListener(listener);

        this.comboColorType.addActionListener(e ->
        {
            String selection = (String) this.comboColorType.getSelectedItem();

            this.comboColorSolid.setVisible(false);
            this.comboColorDiscrete.setVisible(false);
            this.comboColorScalar.setVisible(false);
            this.comboColorVector.setVisible(false);

            switch (selection)
            {
                case ColormapState.SOLID:
                    this.comboColorSolid.setVisible(true);
                    this.autoMinMax.setVisible(false);
                    break;
                case ColormapState.DISCRETE:
                    this.comboColorDiscrete.setVisible(true);
                    this.autoMinMax.setVisible(false);
                    break;
                case ColormapState.SCALAR:
                    this.comboColorScalar.setVisible(true);
                    this.autoMinMax.setVisible(true);
                    break;
                case ColormapState.VECTOR:
                    this.comboColorVector.setVisible(true);
                    this.autoMinMax.setVisible(false);
                    break;
            }

            this.update = true;
        });

        this.comboColorSolid.setVisible(false);
        this.comboColorDiscrete.setVisible(false);
        this.comboColorScalar.setVisible(false);
        this.comboColorVector.setVisible(true);

        this.comboColorType.setSelectedItem(ColormapState.SOLID);
        this.comboColorSolid.setSelectedIndex(0);
        this.comboColorDiscrete.setSelectedIndex(0);
        this.comboColorScalar.setSelectedIndex(0);
        this.comboColorVector.setSelectedIndex(0);
    }

    private VectFunction getColoring()
    {
        if (this.hasData() && this.data.size() > 0 && this.getAttributeDim() > 0)
        {
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

            int dattr = this.getAttributeDim();
            int dcolor = coloring.getDimIn();
            if (dcolor != dattr)
            {
                coloring = VectFunctionSource.constant(dattr, VectSource.createND(dcolor, 1)).compose(coloring);
            }

            // apply wash
            coloring = coloring.compose(VectFunctionSource.wash(this.wash));

            return coloring;
        }
        else
        {
            return null;
        }
    }

    private void autoMinMax()
    {
        if (this.hasData())
        {
            String ctype = (String) this.comboColorType.getSelectedItem();

            if (ctype.equals(ColormapState.SCALAR))
            {
                double min = Double.MAX_VALUE;
                double max = Double.MIN_VALUE;
                for (int i = 0; i < this.data.size(); i++)
                {
                    double value = this.getAttribute(i).get(0);
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }

                ColormapScalar cm = (ColormapScalar) this.comboColorScalar.getSelectedItem();
                cm.withMin(min);
                cm.withMax(max);
                this.update = true;

                Viewer.getInstance().colormaps.update();
            }
        }
    }

    public double getBallRadius()
    {
        return this.ballRadius;
    }

    public VectsView setData(Vects d)
    {
        this.model.fireTableDataChanged();
        super.setData(d);

        if (d != null)
        {
            try
            {
                Vects pos = new Vects();
                for (int i = 0; i < d.size(); i++)
                {
                    pos.add(this.getCoord(i));
                }

                Box out = null;
                for (int i = 0; i < d.size(); i++)
                {
                    Vect v = this.getCoord(i);

                    if (out == null)
                    {
                        out = Box.create(v);
                    }
                    else
                    {
                        out = out.union(v);
                    }
                }

                this.bounds = out;
                Viewer.getInstance().gui.canvas.render3D.boxUpdate();
            }
            catch (RuntimeException e)
            {
            }

            this.update = true;
        }

        return this;
    }

    @Override
    public Vects getData()
    {
        return this.data;
    }

    protected ControlPanel makeInfoControlsOld()
    {
        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Type: ", new BasicLabel(this.toString()));
        infoPanel.addControl(" ", new BasicLabel());
        {
            final BasicLabel label = new BasicLabel("");
            infoPanel.addControl("Size: ", label);
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
            infoPanel.addControl("Dim: ", label);
            this.observable.addObserver((o, arg) ->
            {
                if (this.hasData())
                {
                    label.setText(String.valueOf(this.data.getDim()));
                }
                else
                {
                    label.setText("NA");
                }
            });
        }

        return infoPanel;
    }

    protected ControlPanel makeInfoControls()
    {
        final BasicLabel labelNum = new BasicLabel("");
        final BasicLabel labelDim = new BasicLabel("");
        final BasicLabel labelMinX = new BasicLabel("");
        final BasicLabel labelMaxX = new BasicLabel("");
        final BasicLabel labelMinY = new BasicLabel("");
        final BasicLabel labelMaxY = new BasicLabel("");
        final BasicLabel labelMinZ = new BasicLabel("");
        final BasicLabel labelMaxZ = new BasicLabel("");

        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Number: ", labelNum);
        infoPanel.addControl("Dimension: ", labelDim);
        infoPanel.addControl("Min X: ", labelMinX);
        infoPanel.addControl("Max X: ", labelMaxX);
        infoPanel.addControl("Min Y: ", labelMinY);
        infoPanel.addControl("Max Y: ", labelMaxY);
        infoPanel.addControl("Min Z: ", labelMinZ);
        infoPanel.addControl("Max Z: ", labelMaxZ);

        Runnable updateInfo = () ->
        {
            if (this.hasData() && this.hasBounds())
            {
                Vects data = this.data;
                Box box = this.getBounds();
                DecimalFormat df = new DecimalFormat("0.00##");

                labelNum.setText(df.format(data.size()));
                labelDim.setText(df.format(data.getDim()));
                labelMinX.setText(df.format(box.getMin().getX()));
                labelMaxX.setText(df.format(box.getMax().getX()));
                labelMinY.setText(df.format(box.getMin().getY()));
                labelMaxY.setText(df.format(box.getMax().getY()));
                labelMinZ.setText(df.format(box.getMin().getZ()));
                labelMaxZ.setText(df.format(box.getMax().getZ()));
            }
            else
            {
                labelNum.setText("NA");
                labelDim.setText("NA");
                labelMinX.setText("NA");
                labelMaxX.setText("NA");
                labelMinY.setText("NA");
                labelMaxY.setText("NA");
                labelMinZ.setText("NA");
                labelMaxZ.setText("NA");
            }
        };

        this.observable.addObserver((a, b) -> updateInfo.run());

        {
            BasicButton elem = new BasicButton("Update Info");
            elem.setToolTipText("Update the info to reflect the most current data");
            elem.addActionListener(e -> updateInfo.run());

            infoPanel.addControl(elem);
        }

        return infoPanel;
    }

    protected Map<String, ControlPanel> makeRenderControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();

        {
            ControlPanel renderPanel = new ControlPanel();
            {
                JCheckBox elem = new JCheckBox();
                elem.setToolTipText("Render a point for each vector");
                elem.addItemListener(e ->
                {
                    this.pointShow = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                    Logging.info("updated point visibility to " + this.pointShow);
                });
                elem.setSelected(this.pointShow);
                renderPanel.addControl("Show points", elem);
            }
            {
                JCheckBox elem = new JCheckBox();
                elem.setToolTipText("Render a ball for each vector");
                elem.addItemListener(e ->
                {
                    this.ballShow = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                    Logging.info("updated ball visibility to " + this.ballShow);
                });
                elem.setSelected(this.ballShow);
                renderPanel.addControl("Show balls", elem);
            }
            {
                JCheckBox elem = new JCheckBox();
                elem.setToolTipText("Color by vect index");
                elem.addItemListener(e ->
                {
                    this.indexColor = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                    Logging.info("updated color by index to " + this.indexColor);
                });
                elem.setSelected(this.indexColor);
                renderPanel.addControl("Color by Index", elem);
            }
            {
                final JPanel combos = new JPanel();
                combos.add(this.comboColorSolid);
                combos.add(this.comboColorDiscrete);
                combos.add(this.comboColorScalar);
                combos.add(this.comboColorVector);

                renderPanel.addControl("Colortype", this.comboColorType);
                renderPanel.addControl("Colormap", combos);
                renderPanel.addControl(autoMinMax);
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
                    }
                });
                renderPanel.addControl("Wash", elem);
                this.viewObservable.addObserver((o, arg) -> elem.setValue(this.wash));
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
                renderPanel.addControl("Point Width", elem);
                this.viewObservable.addObserver((o, arg) -> elem.setValue(this.pointWidth));
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change the radius used for ball rendering");
                elem.setValue(new Float(this.ballRadius));
                elem.addPropertyChangeListener("value", e ->
                {
                    this.ballRadius = ((Number) elem.getValue()).floatValue();
                    this.update = true;
                    Logging.info("updated radius to " + this.ballRadius);
                });
                renderPanel.addControl("Ball Radius", elem);
            }
            {
                final BasicSpinner spin = new BasicSpinner(new SpinnerNumberModel(this.resolution, 1, 10, 1));
                spin.setToolTipText("change the mesh resolution for ball rendering");

                spin.addChangeListener(e ->
                {
                    int value = Integer.valueOf(spin.getValue().toString());
                    this.resolution = value;
                    this.update = true;
                });

                renderPanel.addControl("Resolution", spin);
            }
            {
                final BasicSpinner spin = new BasicSpinner(new SpinnerNumberModel(this.period, 0, 1024, 1));
                spin.setToolTipText("when non-zero, the colormap will be recycled using modular arithmetic");

                spin.addChangeListener(e ->
                {
                    int value = Integer.valueOf(spin.getValue().toString());
                    this.period = value;
                    this.update = true;
                });

                renderPanel.addControl("Period", spin);
            }
            {
                final JSlider elem = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(100 * this.opacity));
                elem.addChangeListener(e ->
                {
                    this.opacity = elem.getModel().getValue() / 100.;
                    this.update = true;
                });
                this.viewObservable.addObserver((o, arg) -> elem.setValue((int) Math.round(100 * this.opacity)));
                renderPanel.addControl("Point Opacity", elem);
            }
            {
                JCheckBox elem = new JCheckBox();
                elem.setToolTipText("Modulate the ball radius by the magnitude of the vects attribute (if it has one)");
                elem.addItemListener(e ->
                {
                    this.ballRadiusModulate = e.getStateChange() == ItemEvent.SELECTED;
                    this.update = true;
                    Logging.info("updated ball visibility to " + this.ballRadiusModulate);
                });
                elem.setSelected(this.ballRadiusModulate);
                renderPanel.addControl("Modulate ball radius", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("use the given vector index to modulate the ball radius (when that is properly enabled)");
                elem.setValue(new Integer(this.ballRadiusIndex));
                elem.addPropertyChangeListener("value", e ->
                {
                    this.ballRadiusIndex = Integer.parseInt(elem.getValue().toString());
                    this.update = true;
                });
                renderPanel.addControl("Ball Radius Index", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change the index used for the x coordinate");
                elem.setValue(new Integer(this.xCoordIndex));
                elem.addPropertyChangeListener("value", e ->
                {
                    this.xCoordIndex = Integer.parseInt(elem.getValue().toString());
                    this.update = true;
                });
                renderPanel.addControl("X Coord Index", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change the index used for the y coordinate");
                elem.setValue(new Integer(this.yCoordIndex));
                elem.addPropertyChangeListener("value", e ->
                {
                    this.yCoordIndex = Integer.parseInt(elem.getValue().toString());
                    this.update = true;
                });
                renderPanel.addControl("Y Coord Index", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change the index used for the z coordinate");
                elem.setValue(new Integer(this.zCoordIndex));
                elem.addPropertyChangeListener("value", e ->
                {
                    this.zCoordIndex = Integer.parseInt(elem.getValue().toString());
                    this.update = true;
                });
                renderPanel.addControl("Z Coord Index", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change starting index of the attribute");
                elem.setValue(new Integer(this.attrStartIndex));
                elem.addPropertyChangeListener("value", e ->
                {
                    this.attrStartIndex = Integer.parseInt(elem.getValue().toString());
                    this.update = true;
                });
                renderPanel.addControl("Attr Start Index", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
                elem.setToolTipText("change ending index of the attribute (inclusive where -1 indicates the last index)");
                elem.setValue(new Integer(this.attrEndIndex));
                elem.addPropertyChangeListener("value", e ->
                {
                    this.attrEndIndex = Integer.parseInt(elem.getValue().toString());
                    this.update = true;
                });
                renderPanel.addControl("Attr End Index", elem);
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Affine, true, true);
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                    AffineView affine1 = (AffineView) entry.getValue();

                    if (!JavaUtils.equals(affine1, this.affine))
                    {
                        this.setAffine(affine1);
                        Logging.info("using affine: " + entry.getName());
                    }
                });
                renderPanel.addControl("Affine", elem);
            }
            {
                BasicButton elem = new BasicButton("Edit");
                elem.setToolTipText("open a window for editing vector values");
                elem.addActionListener(e ->
                {
                    final BasicTable table = new BasicTable(this.model);
                    table.setAutoResizeMode(BasicTable.AUTO_RESIZE_LAST_COLUMN);

                    JScrollPane scroll = BasicTable.createStripedJScrollPane(table);
                    scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                    JPanel edit = new JPanel();
                    edit.setLayout(new BorderLayout());
                    edit.add(scroll, BorderLayout.CENTER);

                    BasicButton add = new BasicButton("Add");
                    add.addActionListener(e12 ->
                    {
                        int dim = this.data.getDim();
                        Vects ndata = this.data.copy();
                        ndata.add(VectSource.createND(dim));
                        this.setData(ndata);
                        this.model.fireTableDataChanged();
                    });

                    BasicButton del = new BasicButton("Delete");
                    del.addActionListener(e1 ->
                    {
                        int sel = table.getSelectedRow();
                        Vects ndata = this.data.copy();
                        ndata.remove(sel);
                        this.setData(ndata);
                        this.model.fireTableDataChanged();
                    });

                    BasicButton close = new BasicButton("Close");

                    JPanel buttonPanel = new JPanel();
                    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
                    buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                    buttonPanel.add(add);
                    buttonPanel.add(javax.swing.Box.createRigidArea(new Dimension(10, 0)));
                    buttonPanel.add(del);
                    buttonPanel.add(javax.swing.Box.createHorizontalGlue());
                    buttonPanel.add(close);

                    edit.add(buttonPanel, BorderLayout.SOUTH);

                    final JDialog dialog = new JDialog(Viewer.getInstance().gui.getFrame(), "Vects Editor");
                    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    dialog.add(edit, BorderLayout.NORTH);
                    dialog.setSize(300, 300);
                    dialog.pack();
                    dialog.setVisible(true);

                    close.addActionListener(e13 -> dialog.dispose());
                });
                renderPanel.addControl(elem);
            }

            controls.put("Vects", renderPanel);
        }

        controls.put("Mesh", this.render.getPanel());

        {
            ControlPanel glyphPanel = new ControlPanel();
            glyphPanel.setLayout(new BoxLayout(glyphPanel, BoxLayout.Y_AXIS));

            this.glyphCombo = new BasicComboBox<>();
            this.glyphCombo.setPrototypeDisplayValue(PROTO);
            this.glyphCombo.addItem(NONE);
            for (String name : this.glyphs.keySet())
            {
                this.glyphCombo.addItem(name);
            }
            this.glyphCombo.addActionListener(e ->
            {
                for (String name : VectsView.this.glyphs.keySet())
                {
                    VectsView.this.glyphs.get(name).a.setVisible(false);
                }

                String selected = (String) VectsView.this.glyphCombo.getSelectedItem();
                if (!NONE.equals(selected))
                {
                    VectsView.this.glyphs.get(selected).a.setVisible(true);
                }
            });

            ControlPanel typePanel = new ControlPanel();
            {
                JCheckBox elem = new JCheckBox();
                elem.setToolTipText("make the glyphs visible");
                elem.addItemListener(e ->
                {
                    VectsView.this.glyphShow = e.getStateChange() == ItemEvent.SELECTED;
                    Logging.info("updated glyph visibility to " + VectsView.this.glyphShow);
                });
                elem.setSelected(this.glyphShow);
                typePanel.addControl("Visible", elem);
            }

            this.glyphCombo.setToolTipText("change the type of glyph shown (if you choose one that does not match the data, nothing will be shown)");
            typePanel.addControl("Type", this.glyphCombo);
            this.glyphCombo.addActionListener(a -> this.renderGlyphs.update());
            glyphPanel.add(typePanel);

            for (String name : VectsView.this.glyphs.keySet())
            {
                glyphPanel.add(VectsView.this.glyphs.get(name).a);
            }

            controls.put(GLYPH, glyphPanel);
        }

        return controls;
    }

    protected Map<String, ControlPanel> makeEditControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();

        {
            ControlPanel panel = new ControlPanel();
            {
                JCheckBox cb = new JCheckBox();
                cb.setSelected(this.limit);
                cb.addItemListener(e ->
                {
                    this.limit = e.getStateChange() == ItemEvent.SELECTED;
                    this.updater.run();
                    Logging.info("using limit: " + this.limit);
                });
                panel.addControl("Limit", cb);
            }
            {
                final BasicSpinner spin = new BasicSpinner(new SpinnerNumberModel(this.maxnum, 1, 100000, 1));

                spin.addChangeListener(e ->
                {
                    this.maxnum = Integer.valueOf(spin.getValue().toString());
                    this.updater.run();
                });

                panel.addControl("Max num", spin);
            }
            {
                final BasicButton elem = new BasicButton("Shuffle Subset");
                elem.addActionListener(arg0 ->
                {
                    Logging.info("Shuffling which");
                    this.subset = null;
                    this.updater.run();
                });
                panel.addControl("", elem);
            }
            {
                final BasicFormattedTextField elem = new BasicFormattedTextField();
                elem.setValue(this.which);
                elem.setToolTipText("Specify points to render, e.g. 0,2:10");
                elem.addPropertyChangeListener("value", e ->
                {
                    String nwhich = (String) elem.getValue();
                    if (nwhich != this.which)
                    {
                        this.which = nwhich;
                        this.updater.run();
                        Logging.info("updated which to " + this.which);
                    }
                });
                panel.addControl("Which Label", elem);
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
                panel.addControl("Solids Include", elem);
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Solids, true, true);
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                    SolidsView solids = (SolidsView) entry.getValue();

                    if (!JavaUtils.equals(solids, this.solidsExclude))
                    {
                        this.setSolidsExclude(solids);
                    }
                });
                panel.addControl("Solids Exclude", elem);
            }
            {
                final BasicButton button = new BasicButton("Retain Shown Selection");
                {
                    button.addActionListener(arg0 ->
                    {
                        this.data.keep(this.shown);
                        this.updater.run();
                    });
                }

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Retain Full Selection");
                {
                    button.addActionListener(arg0 ->
                    {
                        this.data.keep(which(false));
                        this.updater.run();
                    });
                }

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Export Shown Selection");
                {
                    button.addActionListener(arg0 ->
                    {
                        Vects selection = this.data.copy(this.shown);
                        String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Specify a name", "vects.selection");
                        VectsView viewable = new VectsView();
                        viewable.setData(selection);
                        viewable.setName(name);
                        Viewer.getInstance().qviewables.offer(viewable);
                    });
                }

                panel.addControl(button);
            }
            {
                final BasicButton button = new BasicButton("Export Full Selection");
                {
                    button.addActionListener(arg0 ->
                    {
                        Vects selection = this.data.copy(which(false));
                        String name = SwingUtils.getString(Viewer.getInstance().gui.getFrame(), "Specify a name", "vects.selection");
                        VectsView viewable = new VectsView();
                        viewable.setData(selection);
                        viewable.setName(name);
                        Viewer.getInstance().qviewables.offer(viewable);
                    });
                }

                panel.addControl(button);
            }
            controls.put("Selection", panel);
        }

        return controls;
    }

    private boolean[] which(boolean subset)
    {
        Containable selectInclude = SelectorSource.constant(true);
        if (this.solidsInclude != null && this.solidsInclude.hasData())
        {
            selectInclude = this.solidsInclude.getData();
        }

        Containable selectExclude = SelectorSource.constant(false);
        if (this.solidsExclude != null && this.solidsExclude.hasData())
        {
            selectExclude = this.solidsExclude.getData();
        }

        Set<Integer> whichidx = Sets.newHashSet(CliUtils.parseWhich(this.which));

        boolean[] out = new boolean[this.data.size()];
        for (int i = 0; i < this.data.size(); i++)
        {
            Vect coord = this.getCoord(i);

            if (subset && this.subset != null && !this.subset.contains(i))
            {
                continue;
            }

            if (whichidx.size() > 0 && !whichidx.contains(i))
            {
                continue;
            }

            if (!selectInclude.contains(coord))
            {
                continue;
            }

            if (selectExclude.contains(coord))
            {
                continue;
            }

            out[i] = true;
        }

        return out;
    }

    private RenderGlyph getGlyph()
    {
        if (this.glyphCombo == null)
        {
            return null;
        }

        String selected = (String) this.glyphCombo.getSelectedItem();
        if (this.glyphs.containsKey(selected))
        {
            return this.glyphs.get(selected).b;
        }

        return null;
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

        this.updater.run();
    }

    public void setSolidsInclude(SolidsView a)
    {
        if (this.solidsInclude != null)
        {
            this.solidsInclude.observable.deleteObserver(this.updateObserver);
        }

        // can be null
        this.solidsInclude = a;

        if (this.solidsInclude != null)
        {
            this.solidsInclude.observable.addObserver(this.updateObserver);
        }

        this.updater.run();
    }

    public void setSolidsExclude(SolidsView a)
    {
        if (this.solidsExclude != null)
        {
            this.solidsExclude.observable.deleteObserver(this.updateObserver);
        }

        // can be null
        this.solidsExclude = a;

        if (this.solidsExclude != null)
        {
            this.solidsExclude.observable.addObserver(this.updateObserver);
        }

        this.updater.run();
    }

    public void dispose(GL2 gl)
    {
        if (this.list != null)
        {
            Logging.info(String.format("deleting vects display list for %s", this.getName()));
            gl.glDeleteLists(this.list, 1);
            this.list = null;
            this.update = true;
        }

        this.renderGlyphs.dispose(gl);
    }

    public void display(GL2 gl)
    {
        if (this.data == null || this.data.getDim() < 3)
        {
            return;
        }

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
                this.updater.run();
            }
        }
        else
        {
            if (this.subset != null)
            {
                this.subset = null;
                this.updater.run();
            }
        }

        // glyphs use their own display list, so keep this separate
        if (this.glyphShow)
        {
            RenderGlyph glyph = this.getGlyph();
            if (glyph != null)
            {
                this.shown = which(true);
                this.renderGlyphs.display(gl, glyph);
            }
        }

        if (this.update && this.list != null)
        {
            Logging.info(String.format("deleting vects display list for %s", this.getName()));
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

                Logging.info(String.format("creating vects display list for %s", this.getName()));
                gl.glNewList(idx, GL2.GL_COMPILE);

                this.shown = which(true);
                VectFunction colormap = this.getColoring();

                if (this.ballShow)
                {
                    Mesh mesh = MeshSource.sphere(this.resolution);
                    MeshUtils.computeNormals(mesh);

                    for (int i = 0; i < this.data.size(); i++)
                    {
                        if (this.shown[i])
                        {
                            double radius = this.ballRadius;

                            if (this.ballRadiusModulate)
                            {
                                Vect vect = this.data.get(i);
                                if (this.ballRadiusIndex > 0 && this.ballRadiusIndex < vect.size())
                                {
                                    radius *= vect.get(this.ballRadiusIndex);
                                }
                            }

                            Vect scale = VectSource.create3D(1.0, 1.0, 1.0).times(radius);
                            Vect coord = this.getCoord(i);
                            Vect color = colormap == null ? WHITE : colormap.apply(this.getAttribute(i));

                            boolean sel = this.picked != null && this.block && i == this.picked.idx;
                            color = sel ? RED : color;

                            gl.glColor4d(color.get(0), color.get(1), color.get(2), color.get(3) * this.opacity);

                            gl.glPushMatrix();
                            RenderUtils.glTransform(gl, scale, coord);
                            this.render.render(gl, mesh);
                            gl.glPopMatrix();
                        }
                    }
                }
                else if (this.pointShow)
                {
                    gl.glDisable(GL2.GL_LIGHTING);
                    gl.glPointSize(this.pointWidth);
                    gl.glEnable(GL2.GL_POINT_SMOOTH);

                    gl.glBegin(GL2.GL_POINTS);
                    for (int i = 0; i < this.data.size(); i++)
                    {
                        if (this.shown[i])
                        {
                            Vect coord = this.getCoord(i);
                            Vect color = colormap == null ? WHITE : colormap.apply(this.getAttribute(i));

                            boolean sel = this.picked != null && this.block && i == this.picked.idx;
                            color = sel ? RED : color;

                            gl.glColor4d(color.get(0), color.get(1), color.get(2), color.get(3) * this.opacity);
                            gl.glVertex3d(coord.get(0), coord.get(1), coord.get(2));
                        }
                    }
                    gl.glEnd();
                }

                gl.glEndList();
            }
        }

        if (this.list != null)
        {
            gl.glCallList(this.list);
        }
    }

    private class VectsTableModel extends AbstractTableModel
    {
        static final long serialVersionUID = 5929107069446200397L;

        private VectsTableModel()
        {
        }

        public int getColumnCount()
        {
            return VectsView.this.data.getDim();
        }

        public int getRowCount()
        {
            return VectsView.this.data.size();
        }

        public String getColumnName(int col)
        {
            return "elem(" + col + ")";
        }

        public Object getValueAt(int row, int col)
        {
            return VectsView.this.data.get(row).get(col);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Class getColumnClass(int c)
        {
            return this.getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col)
        {
            return true;
        }

        public void setValueAt(Object value, int row, int col)
        {
            VectsView.this.data.get(row).set(col, Double.valueOf(String.valueOf(value)));
            VectsView.this.updater.run();
        }

    }

    public boolean getVisibility(int i)
    {
        return this.shown[i];
    }

    public Vect getCoord(int i)
    {
        double x = getSafe(i, this.xCoordIndex);
        double y = getSafe(i, this.yCoordIndex);
        double z = getSafe(i, this.zCoordIndex);

        Vect out = VectSource.create3D(x, y, z);

        if (this.affine != null && this.affine.hasData())
        {
            out = this.affine.getData().apply(out);
        }

        return out;
    }

    public Vect getAttribute(int i)
    {
        if (this.indexColor)
        {
            // zero is a black background, so set it to one-based
            return VectSource.create1D(i + 1);
        }
        else
        {
            int start = indexSafe(this.attrStartIndex);
            int end = indexSafe(this.attrEndIndex) + 1;

            return this.data.get(i).sub(start, end);
        }
    }

    public int getAttributeDim()
    {
        if (this.indexColor)
        {
            return 1;
        }
        else
        {
            int start = indexSafe(this.attrStartIndex);
            int end = indexSafe(this.attrEndIndex) + 1;

            return end - start;
        }
    }

    public Vect getGlyph(int i)
    {
        int start = indexSafe(this.glyphStartIndex);
        int end = indexSafe(this.glyphEndIndex) + 1;

        return this.data.get(i).sub(start, end);
    }

    public int getGlyphDim()
    {
        int start = indexSafe(this.glyphStartIndex);
        int end = indexSafe(this.glyphEndIndex + 1);

        return end - start;
    }

    public int indexSafe(int idx)
    {
        int dim = this.data.getDim();
        if (idx < 0)
        {
            idx = dim + idx;
        }

        if (idx >= dim)
        {
            return dim - 1;
        }
        else if (idx < 0)
        {
            return 0;
        }
        else
        {
            return idx;
        }
    }

    public boolean hasCoord()
    {
        return this.hasData() && this.data.getDim() >= 3;
    }

    public double getSafe(int i, int j)
    {
        if (this.hasData() && i >= 0 && j >= 0 && i < this.data.size() && j < this.data.getDim())
        {
            return this.data.get(i).get(j);
        }

        return 0;
    }

    public Double dist(WorldMouse mouse)
    {
        if (!this.hasData() || mouse.press == null)
        {
            return null;
        }

        double mindist = Double.MAX_VALUE;

        for (int i = 0; i < this.data.size(); i++)
        {
            Line pressLine = Line.fromTwoPoints(mouse.press.point, mouse.press.hit);
            double distLine = pressLine.dist(this.getCoord(i));

            if (distLine <= 1.5 * this.ballRadius)
            {
                mindist = Math.min(mouse.press.point.dist(data.get(i)), mindist);
            }
        }

        return mindist;
    }

    private Interaction parse(WorldMouse mouse, String mode)
    {
        for (Interaction i : Interaction.values())
        {
            if (Interaction.Add.toString().equals(mode) && this.block)
            {
                return Interaction.None;
            }

            if (Interaction.Remove.toString().equals(mode) && this.block)
            {
                return Interaction.None;
            }

            if (i.toString().equals(mode))
            {
                return i;
            }
        }

        if (Constants.INTERACTION_ROTATE.equals(mode))
        {
            if (mouse.control && mouse.shift && !this.block)
            {
                return Interaction.Remove;
            }
            else if (picked == null)
            {
                if (mouse.control && !mouse.shift && !this.block)
                {
                    return Interaction.Add;
                }
                else if (!mouse.shift)
                {
                    return Interaction.Select;
                }
                else
                {
                    return Interaction.None;
                }
            }
            else
            {
                if (!mouse.control && mouse.shift)
                {
                    return Interaction.List;
                }
                else
                {
                    return Interaction.Move;
                }
            }
        }

        return Interaction.None;
    }

    public List<String> modes()
    {
        List<String> out = Lists.newArrayList();
        out.add(Interaction.List.toString());
        out.add(Interaction.Move.toString());
        out.add(Interaction.Add.toString());
        out.add(Interaction.Remove.toString());
        return out;
    }

    public void handle(WorldMouse mouse, String mode)
    {
        if (!this.hasData() || mouse.press == null || mouse.current == null || !mouse.pick)
        {
            this.block = false;
            this.picked = null;
            return;
        }

        Interaction inter = parse(mouse, mode);

        Vect hit = mouse.current == null ? mouse.press.hit : mouse.current.hit;

        if (Interaction.Add.equals(inter))
        {
            Vects ndata = this.data.copy();
            ndata.add(hit);
            this.setData(ndata);
            this.block = true;
            Viewer.getInstance().control.setStatusMessage("added vect: " + mouse.press.hit);
            return;
        }

        if (data.size() == 0)
        {
            // the operations below only work when vects exist
            return;
        }

        double minDist2 = this.getCoord(0).dist2(hit);
        int nearest = 0;
        for (int i = 1; i < this.data.size(); i++)
        {
            double dist2 = this.getCoord(i).dist2(hit);
            if (dist2 < minDist2)
            {
                nearest = i;
                minDist2 = dist2;
            }
        }

        Vect vect = this.getCoord(nearest);
        Line pressLine = Line.fromTwoPoints(mouse.press.point, mouse.press.hit);

        if (Interaction.Remove.equals(inter))
        {
            Vects ndata = this.data.copy();
            ndata.remove(nearest);
            this.setData(ndata);
            Viewer.getInstance().control.setStatusMessage("removed vect: " + nearest);

            this.block = true;
            this.picked = null;
        }
        else if (Interaction.List.equals(inter))
        {
            Viewer.getInstance().control.setStatusMessage("nearest vect:\n  index: " + nearest + "\n  position: " + vect);
        }
        else if (Interaction.Select.equals(inter))
        {
            Logging.info("selecting vect: " + nearest);
            this.picked = new VectPick(vect, nearest);
            this.block = true;
        }
        else if (Interaction.Move.equals(inter))
        {
            Logging.info("moving vect: " + nearest);
            this.block = true;

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

            Vect start = this.picked.pos;
            Plane plane = Plane.fromPointNormal(start, pressLine.getDir().normalize());
            Line dragLine = Line.fromTwoPoints(mouse.current.point, mouse.current.hit);
            List<LineIntersection> inters = plane.intersect(dragLine);

            if (inters.size() != 1)
            {
                // this might catch an edge case
                Logging.info("warning: no plane intersection");
                return;
            }

            Vect end = inters.get(0).getPoint();
            Vect delta = end.minus(start);

            this.data.set(this.picked.idx, this.picked.pos.plus(delta));
            this.setData(this.data);
        }
    }
}
