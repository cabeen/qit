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

package qitview.render;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.jogamp.opengl.GL2;
import qit.base.Global;
import qit.base.Logging;
import qit.base.Model;
import qit.base.ModelType;
import qit.base.cli.CliUtils;
import qit.base.structs.Named;
import qit.base.structs.ObservableInstance;
import qit.base.utils.JavaUtils;
import qit.data.datasets.*;
import qit.data.source.MeshSource;
import qit.data.source.VectSource;
import qit.data.source.VolumeSource;
import qit.data.utils.mri.ModelUtils;
import qit.math.structs.VectFunction;
import qit.math.structs.Vertex;
import qit.math.utils.colormaps.ColormapDiscrete;
import qit.math.utils.colormaps.ColormapScalar;
import qit.math.utils.colormaps.ColormapVector;
import qitview.main.Constants;
import qitview.main.Settings;
import qitview.main.Viewer;
import qitview.models.Slicer;
import qitview.models.Viewable;
import qitview.models.ViewableAction;
import qitview.models.ViewableType;
import qitview.models.WorldMouse;
import qitview.panels.Viewables;
import qitview.views.MaskView;
import qitview.views.SliceableView;
import qitview.views.VolumeView;
import qitview.widgets.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class RenderVolumeSlice<E extends SliceableView<?>>
{
    private static int INDEX = 0;
    private static final String FEATURE_NONE = "None";
    private static final double LIFT = 0.01;
    private static final int TUBE_RES = 5;
    private static final double TUBE_THICK = 1.0;
    private static final int MAX_OFFSET = 512;
    private static final int[] TEX_SIZES = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768};

    private enum SlabType
    {
        Mean, Min, Max
    }

    private enum Interaction
    {
        None, Query, Slice, Select
    }

    private enum Direction
    {
        I, J, K
    }

    private static class Selection
    {
        Vect world;
        Sample voxel;
        Direction dir;
    }

    private class TextureBuffer
    {
        Boolean fill = false;
        Boolean changed = false;
        Integer index = null;
        Integer size = null;
        Integer width = null;
        Integer height = null;
        ByteBuffer data = null;

        private void computeSize(int max)
        {
            Integer ntex = null;
            for (int s : TEX_SIZES)
            {
                if (s >= max)
                {
                    ntex = s;
                    break;
                }
            }
            Global.assume(ntex != null, "slice too large for texture loading!");

            this.size = ntex;
        }

        private void allocateBuffer(int w, int h)
        {
            int tsize = w * h * 4;
            if (this.data == null || this.data.capacity() != tsize)
            {
                Logging.info(String.format("allocating texture with size %d x %d for data %s with %d bytes", w, h, RenderVolumeSlice.this.volume.getName(), tsize));
                this.data = ByteBuffer.allocate(tsize);
                this.width = w;
                this.height = h;
            }
        }

        private void dispose(GL2 gl)
        {
            if (this.index != null)
            {
                Logging.info(String.format("deleting texture buffer %d for data %s", this.index, RenderVolumeSlice.this.volume.getName()));
                gl.glDeleteTextures(1, IntBuffer.wrap(new int[]{this.index}));
                this.index = null;
            }

            this.changed = true;
        }
    }

    private class AdapterBuffer
    {
        private AdapterBuffer(int myindex, Volume mydata)
        {
            this.index = myindex;
            this.data = mydata;
        }

        public boolean changed = false;
        public Integer index = null;
        public Volume data = null;
    }

    private transient Observer updateObserver = (o, arg) ->
    {
        this.updateAll();
    };

    private transient Consumer<Integer> setChannel = (c) ->
    {
    };
    private transient Consumer<Integer> setSlab = (c) ->
    {
    };

    private ControlPanel panel = new ControlPanel();

    private BasicComboBox<String> comboColorType;
    private BasicComboBox<ColormapDiscrete> comboColorDiscrete;
    private BasicComboBox<ColormapScalar> comboColorScalar;
    private BasicComboBox<ColormapVector> comboColorVector;

    private Slicer slicer;

    private Queue<Sample> textureQ = Queues.newLinkedBlockingQueue();

    private TextureBuffer textureI = new TextureBuffer();
    private TextureBuffer textureJ = new TextureBuffer();
    private TextureBuffer textureK = new TextureBuffer();

    private AdapterBuffer adapterI = null;
    private AdapterBuffer adapterJ = null;
    private AdapterBuffer adapterK = null;

    private Vect pointA = null;
    private Vect pointB = null;
    private Vect pointC = null;

    private E volume;
    private MaskView mask;
    private VolumeView weight;
    private VolumeView opacityMap;

    private RenderGeometry tubeRender;

    private boolean visible = true;
    private boolean smooth = false;
    private boolean grid = false;
    private boolean box = false;
    private Vect offset = VectSource.create3D();
    private double opacity = 1f;
    private double bglevel = 0;
    private boolean nobg = false;
    private int channel = 0;
    private int slab = 0;
    private SlabType slabType = SlabType.Mean;
    private String feature = FEATURE_NONE;
    private VectFunction colormap = null;
    private boolean tube = true;
    private transient UnitMapDialog opacityTransfer = new UnitMapDialog("Opacity", 0, 1);
    private transient VectFunction opacityFunction = null;

    private transient boolean updateColoring = false;
    private transient boolean fillTextureQ = false;

    private transient Selection picked = null;
    private transient long lastTime = 0;
    private transient int id;

    private Mesh mesh;

    private ObservableInstance observable = new ObservableInstance();

    private String which = "";
    private transient Set<Integer> whichidx = Sets.newHashSet(CliUtils.parseWhich(this.which));

    public RenderVolumeSlice(E parent)
    {
        this.id = INDEX;
        INDEX += 1;

        parent.observable.addObserver((a, b) -> this.updateBufferAll());

        this.volume = parent;
        this.tubeRender = new RenderGeometry(() -> this.updateBufferAll());
        this.tubeRender.setMeshSmooth(true);

        this.mesh = MeshSource.cylinder(TUBE_RES);

        // manually compute normals to avoid including caps
        for (Vertex v : this.mesh.graph.verts())
        {
            Vect pos = this.mesh.vattr.get(v, Mesh.COORD);
            Vect norm = VectSource.create3D(pos.get(0), pos.get(1), 0).normalize();
            this.mesh.vattr.set(v, Mesh.NORMAL, norm);
        }

        this.opacityTransfer.addObserver(new Observer()
        {
            public void update(Observable o, Object arg)
            {
                RenderVolumeSlice.this.updateOpacityTransfer();
            }
        });
        this.updateOpacityTransfer();

        this.initPanel();
        this.updateBufferAll();
    }

    public ControlPanel getPanel()
    {
        return this.panel;
    }

    public int getChannel()
    {
        return this.channel;
    }

    public void nextChannel()
    {
        if (this.hasData())
        {
            this.setChannel.accept(Math.floorMod(this.channel + 1, this.getDim()));
        }
    }

    public void prevChannel()
    {
        if (this.hasData())
        {
            this.setChannel.accept(Math.floorMod(this.channel - 1, this.getDim()));
        }
    }

    public void pushBack()
    {
        this.pushBackI();
        this.pushBackJ();
        this.pushBackK();
    }

    public void setMask(MaskView m)
    {
        // can be null

        if (this.mask != null)
        {
            this.mask.observable.deleteObserver(this.updateObserver);
        }

        this.mask = m;

        if (this.mask != null)
        {
            this.mask.observable.addObserver(this.updateObserver);
        }

        this.updateBufferAll();
    }

    public void setWeight(VolumeView m)
    {
        // can be null

        if (this.weight != null)
        {
            this.weight.observable.deleteObserver(this.updateObserver);
        }

        this.weight = m;

        if (this.weight != null)
        {
            this.weight.observable.addObserver(this.updateObserver);
        }

        this.updateBufferAll();
    }

    public void setOpacityMap(VolumeView m)
    {
        // can be null

        if (this.opacityMap != null)
        {
            this.opacityMap.observable.deleteObserver(this.updateObserver);
        }

        this.opacityMap = m;

        if (this.opacityMap != null)
        {
            this.opacityMap.observable.addObserver(this.updateObserver);
        }

        this.updateBufferAll();
    }

    public void setVisible(boolean v)
    {
        this.visible = v;
        this.observable.changed();
    }

    public void setOpacity(double v)
    {
        this.opacity = v;
        this.observable.changed();
    }

    public void setNoBG(boolean v)
    {
        this.nobg = v;
        this.observable.changed();
    }

    public void setVectorColormap()
    {
        this.comboColorType.setSelectedItem(ColormapState.VECTOR);
    }

    public String getScalarColormapName()
    {
        return ((ColormapScalar) this.comboColorScalar.getSelectedItem()).getName();
    }

    public void setScalarColormap(String name)
    {
        if (name == null)
        {
            return;
        }

        ComboBoxModel<ColormapScalar> model = Viewer.getInstance().colormaps.getComboScalar().getModel();

        for (int i = 0; i < model.getSize(); i++)
        {
            if (model.getElementAt(i).getName().equals(name))
            {
                this.comboColorScalar.setSelectedIndex(i);
            }
        }
    }

    private void initPanel()
    {
        final boolean mask = (this.volume instanceof MaskView);

        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("render the slices");
            elem.setSelected(this.visible);
            elem.addItemListener(e ->
            {
                this.visible = e.getStateChange() == ItemEvent.SELECTED;
                this.updateBufferAll();
            });
            this.observable.addObserver((o, arg) -> elem.setSelected(RenderVolumeSlice.this.visible));
            this.panel.addControl("Visible", elem);
        }
        {
            final JCheckBox showI = new JCheckBox();
            final JCheckBox showJ = new JCheckBox();
            final JCheckBox showK = new JCheckBox();

            this.volume.observable.addObserver((o, arg) ->
            {
                if (RenderVolumeSlice.this.volume.hasData())
                {
                    Slicer slicer12 = RenderVolumeSlice.this.volume.getSlicer();
                    showI.setModel(slicer12.modelButtonI());
                    showJ.setModel(slicer12.modelButtonJ());
                    showK.setModel(slicer12.modelButtonK());

                    for (JCheckBox b : new JCheckBox[]{showI, showJ, showK})
                    {
                        b.addChangeListener(slicer12);
                    }
                }
            });

            final BasicSpinner spinI = new BasicSpinner(new SpinnerNumberModel(0, 0, 0, 1));
            final BasicSpinner spinJ = new BasicSpinner(new SpinnerNumberModel(0, 0, 0, 1));
            final BasicSpinner spinK = new BasicSpinner(new SpinnerNumberModel(0, 0, 0, 1));

            for (BasicSpinner s : new BasicSpinner[]{spinI, spinJ, spinK})
            {
                ((BasicSpinner.DefaultEditor) s.getEditor()).getTextField().setColumns(3);
            }

            this.volume.observable.addObserver((o, arg) ->
            {
                if (RenderVolumeSlice.this.volume.hasData())
                {
                    Slicer slicer1 = RenderVolumeSlice.this.volume.getSlicer();
                    spinI.setModel(slicer1.modelSpinnerI());
                    spinJ.setModel(slicer1.modelSpinnerJ());
                    spinK.setModel(slicer1.modelSpinnerK());

                    for (BasicSpinner s : new BasicSpinner[]{spinI, spinJ, spinK})
                    {
                        s.addChangeListener(slicer1);
                    }
                }
            });

            final BasicSpinner offI = new BasicSpinner(new SpinnerNumberModel(0, -MAX_OFFSET, MAX_OFFSET, 1));
            final BasicSpinner offJ = new BasicSpinner(new SpinnerNumberModel(0, -MAX_OFFSET, MAX_OFFSET, 1));
            final BasicSpinner offK = new BasicSpinner(new SpinnerNumberModel(0, -MAX_OFFSET, MAX_OFFSET, 1));

            BasicSpinner[] offs = new BasicSpinner[]{offI, offJ, offK};
            for (int i = 0; i < 3; i++)
            {
                final BasicSpinner off = offs[i];
                final int fi = i;
                off.addChangeListener(e ->
                {
                    double value = Double.valueOf(off.getModel().getValue().toString());
                    this.offset.set(fi, value);
                    this.updateBufferAll();
                });
            }

            JPanel panelI = new JPanel();
            panelI.setLayout(new GridLayout(1, 3));
            panelI.add(spinI);
            panelI.add(offI);
            panelI.add(showI);
            this.panel.addControl("Slice I", panelI);

            JPanel panelJ = new JPanel();
            panelJ.setLayout(new GridLayout(1, 3));
            panelJ.add(spinJ);
            panelJ.add(offJ);
            panelJ.add(showJ);
            this.panel.addControl("Slice J", panelJ);

            JPanel panelK = new JPanel();
            panelK.setLayout(new GridLayout(1, 3));
            panelK.add(spinK);
            panelK.add(offK);
            panelK.add(showK);
            this.panel.addControl("Slice K", panelK);
        }
        if (!mask)
        {
            {
                final SpinnerNumberModel model = new SpinnerNumberModel(this.channel, 0, 1024, 1);
                final BasicSpinner elem = new BasicSpinner(model);
                elem.setToolTipText("specify the channel to render");
                elem.addChangeListener(e ->
                {
                    int value = Integer.valueOf(elem.getModel().getValue().toString());
                    this.channel = value;
                    this.updateBufferAll();
                });
                this.panel.addControl("Channel", elem);
                this.volume.observable.addObserver((o, arg) ->
                {
                    if (RenderVolumeSlice.this.volume.hasData())
                    {
                        int max = RenderVolumeSlice.this.getDim() - 1;
                        RenderVolumeSlice.this.channel = Math.min(max, RenderVolumeSlice.this.channel);
                        model.setMaximum(max);
                    }
                });

                this.setChannel = (c) ->
                {
                    this.channel = c;
                    elem.setValue(c);
                    this.updateBufferAll();
                };
            }
            {
                final BasicComboBox<String> elem = new BasicComboBox<>();
                elem.setToolTipText("specify the feature to render");
                elem.addItem(FEATURE_NONE);
                elem.addActionListener(a ->
                {
                    String f = (String) elem.getSelectedItem();
                    if (f != null && !f.equals(RenderVolumeSlice.this.feature))
                    {
                        RenderVolumeSlice.this.feature = f;

                        if (f.toLowerCase().contains("rgb"))
                        {
                            RenderVolumeSlice.this.comboColorType.setSelectedItem(ColormapState.VECTOR);
                        }
                        else
                        {
                            RenderVolumeSlice.this.comboColorType.setSelectedItem(ColormapState.SCALAR);
                            RenderVolumeSlice.this.autoMinMax();
                        }

                        RenderVolumeSlice.this.updateBufferAll();
                    }
                });

                this.panel.addControl("Feature", elem);
                this.volume.observable.addObserver((o, arg) ->
                {
                    if (RenderVolumeSlice.this.volume.hasData())
                    {
                        elem.removeAllItems();
                        elem.addItem(FEATURE_NONE);

                        ModelType type = RenderVolumeSlice.this.getModel();

                        if (!type.equals(ModelType.Vect))
                        {
                            int dim = RenderVolumeSlice.this.getDim();
                            Model proto = ModelUtils.proto(RenderVolumeSlice.this.getModel(), dim);
                            List<String> features = proto.features();
                            for (String feature1 : features)
                            {
                                elem.addItem(feature1);
                            }
                        }
                    }
                });
            }
            {
                final SpinnerNumberModel model = new SpinnerNumberModel(this.slab, -1, 1024, 1);
                final BasicSpinner elem = new BasicSpinner(model);
                elem.setToolTipText("specify the slab to render");
                elem.addChangeListener(e ->
                {
                    int value = Integer.valueOf(elem.getModel().getValue().toString());
                    this.slab = value;
                    this.updateBufferAll();
                });
                this.panel.addControl("Slab Size", elem);

                final BasicComboBox<SlabType> combo = new BasicComboBox<>();
                combo.setToolTipText("specify the feature to render");
                for (SlabType type : SlabType.values())
                {
                    combo.addItem(type);
                }
                combo.setSelectedItem(this.slabType);
                combo.addActionListener(a ->
                {
                    this.slabType = (SlabType) combo.getSelectedItem();
                    this.updateBufferAll();
                });
                this.panel.addControl("Slab Type", combo);
            }
        }

        if (mask)
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField();
            elem.setValue(this.which);
            elem.setToolTipText("Specify which labels to exclusively render, e.g. 0,2:10");
            elem.addPropertyChangeListener("value", e ->
            {
                try
                {
                    String nwhich = (String) elem.getValue();
                    if (nwhich != RenderVolumeSlice.this.which)
                    {
                        Set<Integer> nwhichidx = Sets.newHashSet(CliUtils.parseWhich(nwhich));

                        RenderVolumeSlice.this.which = nwhich;
                        RenderVolumeSlice.this.whichidx = nwhichidx;
                        RenderVolumeSlice.this.updateBufferAll();
                        Logging.info("updated which to " + RenderVolumeSlice.this.which);
                    }
                }
                catch (Exception ex)
                {
                    Logging.info("warning, invalid which: " + elem.getValue());
                }
            });
            panel.addControl("Which Label", elem);
        }

        {
            final JSlider elem = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(100 * this.opacity));
            elem.setToolTipText("specify the transparency of the slice rendering");
            elem.addChangeListener(e ->
            {
                RenderVolumeSlice.this.opacity = elem.getModel().getValue() / 100.;
                RenderVolumeSlice.this.updateBufferAll();
            });
            this.observable.addObserver((o, arg) -> elem.setValue((int) Math.round(100 * RenderVolumeSlice.this.opacity)));
            this.panel.addControl("Opacity", elem);
        }

        if (!mask)
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.bglevel));
            elem.setToolTipText("specify the threshold value for hiding background voxels");
            elem.addPropertyChangeListener("value", e ->
            {
                double nbglevel = ((Number) elem.getValue()).doubleValue();
                if (nbglevel != RenderVolumeSlice.this.bglevel)
                {
                    RenderVolumeSlice.this.bglevel = nbglevel;
                    RenderVolumeSlice.this.updateBufferAll();
                }
            });
            this.observable.addObserver((o, arg) -> elem.setValue(RenderVolumeSlice.this.bglevel));
            panel.addControl("BG Level", elem);
        }
        {
            final BasicButton auto = new BasicButton("Auto Min/Max");
            auto.setToolTipText("delect the min and max intensities and update the colormap to use them");
            {
                auto.addActionListener(arg0 -> RenderVolumeSlice.this.autoMinMax());
            }

            final BasicButton editColorDiscrete = new BasicButton("Edit Discrete Colormap");
            editColorDiscrete.addActionListener(arg0 ->
            {
                ColormapState cmap = Viewer.getInstance().colormaps;
                cmap.showDiscrete();
                cmap.setComboDiscrete((ColormapDiscrete) RenderVolumeSlice.this.comboColorDiscrete.getSelectedItem());
            });

            final BasicButton editColorScalar = new BasicButton("Edit Scalar Colormap");
            editColorScalar.addActionListener(arg0 ->
            {
                ColormapState cmap = Viewer.getInstance().colormaps;
                cmap.showScalar();
                cmap.setComboScalar((ColormapScalar) RenderVolumeSlice.this.comboColorScalar.getSelectedItem());
            });

            final ColormapState cms = Viewer.getInstance().colormaps;
            this.comboColorDiscrete = cms.getComboDiscrete();
            this.comboColorDiscrete.setSelectedIndex(0);
            this.comboColorScalar = cms.getComboScalar();
            this.comboColorScalar.setSelectedIndex(0);
            this.comboColorVector = cms.getComboVector();
            this.comboColorVector.setSelectedIndex(0);

            if (mask)
            {
                Viewer.getInstance().colormaps.getObservable().addObserver((o, arg) ->
                {
                    RenderVolumeSlice.this.colormap = ((ColormapDiscrete) RenderVolumeSlice.this.comboColorDiscrete.getSelectedItem()).getFunction();
                    RenderVolumeSlice.this.updateBufferAll();
                });

                this.comboColorDiscrete.addActionListener(e ->
                {
                    RenderVolumeSlice.this.colormap = ((ColormapDiscrete) RenderVolumeSlice.this.comboColorDiscrete.getSelectedItem()).getFunction();
                    RenderVolumeSlice.this.updateBufferAll();
                });

                this.colormap = ((ColormapDiscrete) this.comboColorDiscrete.getSelectedItem()).getFunction();

                final JPanel combos = new JPanel();
                combos.add(this.comboColorDiscrete);

                this.panel.addControl("Color Map", combos);
                this.panel.addControl(editColorDiscrete);
            }
            else
            {
                this.comboColorType = new BasicComboBox<>();
                this.comboColorType.addItem(ColormapState.SCALAR);
                this.comboColorType.addItem(ColormapState.VECTOR);
                this.comboColorType.setToolTipText("specify the type of colormap to use for the vertex coloring");

                Viewer.getInstance().colormaps.getObservable().addObserver((o, arg) ->
                {
                    RenderVolumeSlice.this.colormap = ((ColormapScalar) RenderVolumeSlice.this.comboColorScalar.getSelectedItem()).getFunction();
                    RenderVolumeSlice.this.updateBufferAll();
                });

                this.comboColorScalar.addActionListener(e -> RenderVolumeSlice.this.updateColoring());
                this.comboColorVector.addActionListener(e -> RenderVolumeSlice.this.updateColoring());

                this.comboColorScalar.setVisible(true);
                this.comboColorVector.setVisible(false);
                this.colormap = ((ColormapScalar) this.comboColorScalar.getSelectedItem()).getFunction();

                this.comboColorType.addActionListener(e ->
                {
                    String selection = (String) RenderVolumeSlice.this.comboColorType.getSelectedItem();

                    switch (selection)
                    {
                        case ColormapState.SCALAR:
                            RenderVolumeSlice.this.comboColorScalar.setVisible(true);
                            RenderVolumeSlice.this.comboColorVector.setVisible(false);
                            auto.setVisible(true);
                            editColorScalar.setVisible(true);
                            break;
                        case ColormapState.VECTOR:
                            RenderVolumeSlice.this.comboColorScalar.setVisible(false);
                            RenderVolumeSlice.this.comboColorVector.setVisible(true);
                            auto.setVisible(false);
                            editColorScalar.setVisible(false);
                            break;
                    }

                    RenderVolumeSlice.this.updateAll();
                });

                final JPanel combos = new JPanel();
                this.comboColorType.setSelectedItem(ColormapState.SCALAR);
                combos.add(this.comboColorScalar);
                combos.add(this.comboColorVector);

                this.panel.addControl("Color Type", this.comboColorType);
                this.panel.addControl("Color Map", combos);
                this.panel.addControl(editColorScalar);
                this.panel.addControl(auto);
            }
        }

        {
            Map<String, JCheckBox> boxes = Maps.newLinkedHashMap();
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("show tubes on the border of the slice");
                elem.setSelected(this.tube);
                elem.addItemListener(e ->
                {
                    RenderVolumeSlice.this.tube = e.getStateChange() == ItemEvent.SELECTED;
                });
                this.observable.addObserver((o, arg) -> elem.setSelected(RenderVolumeSlice.this.tube));

                boxes.put("Tubes", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setToolTipText("smooth the rendering of each slice (otherwise show pixels as uniform squares)");
                elem.setSelected(this.smooth);
                elem.addItemListener(e ->
                {
                    RenderVolumeSlice.this.smooth = e.getStateChange() == ItemEvent.SELECTED;
                });
                this.observable.addObserver((o, arg) -> elem.setSelected(RenderVolumeSlice.this.smooth));

                boxes.put("Smooth", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setSelected(this.grid);
                elem.setToolTipText("show the voxel grid (useful only when zooming in)");
                elem.addItemListener(e ->
                {
                    RenderVolumeSlice.this.grid = e.getStateChange() == ItemEvent.SELECTED;
                    RenderVolumeSlice.this.updateBufferAll();
                });
                this.observable.addObserver((o, arg) -> elem.setSelected(RenderVolumeSlice.this.grid));
                boxes.put("Show Grid", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setSelected(this.box);
                elem.setToolTipText("show volume bounding box");
                elem.addItemListener(e ->
                {
                    RenderVolumeSlice.this.box = e.getStateChange() == ItemEvent.SELECTED;
                    RenderVolumeSlice.this.updateBufferAll();
                });
                this.observable.addObserver((o, arg) -> elem.setSelected(RenderVolumeSlice.this.box));
                boxes.put("Show Box", elem);
            }
            {
                final JCheckBox elem = new JCheckBox();
                elem.setSelected(this.nobg);
                elem.setToolTipText("hide background voxels (if the intensity is below the background theshold)");
                elem.addItemListener(e ->
                {
                    RenderVolumeSlice.this.nobg = e.getStateChange() == ItemEvent.SELECTED;
                    RenderVolumeSlice.this.updateBufferAll();
                });
                this.observable.addObserver((o, arg) -> elem.setSelected(RenderVolumeSlice.this.nobg));
                boxes.put("No BG", elem);
            }
            {
                int rows = (int) Math.ceil(boxes.size() / 2.0);
                JPanel gridPanel = new JPanel();
                gridPanel.setLayout(new GridLayout(rows, 4));
                for (String name : boxes.keySet())
                {
                    gridPanel.add(new BasicLabel(name));
                    gridPanel.add(boxes.get(name));
                }
                this.panel.addControl(gridPanel);
            }

            if (!mask)
            {
                {
                    final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Mask, true, true);
                    elem.setPrototypeDisplayValue(Viewables.NONE);
                    elem.setToolTipText("use a mask to hide some voxels from the rendering");
                    elem.addActionListener(e ->
                    {
                        Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                        MaskView mask1 = (MaskView) entry.getValue();

                        if (!entry.equals(Viewables.NONE) && !JavaUtils.equals(mask1, RenderVolumeSlice.this.mask))
                        {
                            RenderVolumeSlice.this.setMask(mask1);
                            RenderVolumeSlice.this.updateBufferAll();
                        }

                        if (entry.equals(Viewables.NONE))
                        {
                            RenderVolumeSlice.this.setMask(null);
                            RenderVolumeSlice.this.updateBufferAll();
                        }
                    });
                    this.panel.addControl("Mask", elem);
                }
                {
                    final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Volume, true, true);
                    elem.setPrototypeDisplayValue(Viewables.NONE);
                    elem.setToolTipText("use a volume to weight the rendering (useful for tensor orientation visualization)");
                    elem.addActionListener(e ->
                    {
                        Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                        VolumeView volume12 = (VolumeView) entry.getValue();

                        if (!entry.equals(Viewables.NONE) && !JavaUtils.equals(volume12, RenderVolumeSlice.this.volume))
                        {
                            RenderVolumeSlice.this.setWeight(volume12);
                            RenderVolumeSlice.this.updateBufferAll();
                        }

                        if (entry.equals(Viewables.NONE))
                        {
                            RenderVolumeSlice.this.setWeight(null);
                            RenderVolumeSlice.this.updateBufferAll();
                        }
                    });
                    this.panel.addControl("Weight", elem);
                }
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Volume, true, true);
                elem.setPrototypeDisplayValue(Viewables.NONE);
                elem.setToolTipText("use a volume to modulate the opacity of rendering");
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                    VolumeView opmap = (VolumeView) entry.getValue();

                    if (!entry.equals(Viewables.NONE))
                    {
                        Viewer.getInstance().gui.setStatusMessage("set a new opacity map");
                        this.setOpacityMap(opmap);
                    }
                    else
                    {
                        Viewer.getInstance().gui.setStatusMessage("removed the opacity map");
                        this.setOpacityMap(null);
                    }
                });
                this.panel.addControl("Opacity", elem);
            }
            {
                final BasicButton button = new BasicButton("Edit Opacity Transfer...");
                button.addActionListener(arg0 -> this.opacityTransfer.show());
                this.panel.addControl(button);
            }
            {
                final BasicButton auto = new BasicButton("Auto Min/Max Opacity");
                auto.setToolTipText("detect the min and max intensities and update the opacity map to use them");
                auto.addActionListener(arg0 -> this.autoMinMaxOpacity());
                this.panel.addControl(auto);
            }
        }
    }

    public void dispose(GL2 gl)
    {
        this.textureI.dispose(gl);
        this.textureJ.dispose(gl);
        this.textureK.dispose(gl);
    }

    public void autoMinMax()
    {
        if (this.volume.hasData())
        {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            if (this.channel < 0 || this.channel >= this.getDim())
            {
                Viewer.getInstance().control.setStatusMessage("warning: invalid data channel");
                return;
            }

            for (Sample sample : this.getSampling())
            {
                double value = this.getValue(sample).get(this.channel);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            String selection = (String) this.comboColorType.getSelectedItem();

            if (selection.equals(ColormapState.SCALAR))
            {
                ColormapScalar cm = (ColormapScalar) this.comboColorScalar.getSelectedItem();
                cm.withMin(min);
                cm.withMax(max);
                this.colormap = cm.getFunction();
                this.updateBufferAll();
                Viewer.getInstance().colormaps.update();
            }
        }
    }

    private void autoMinMaxOpacity()
    {
        if (this.opacityMap.hasData())
        {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            Volume vol = this.opacityMap.getData();

            for (Sample sample : vol.getSampling())
            {
                double value = vol.getDim() > 1 ? vol.get(sample).norm() : vol.get(sample, 0);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            this.opacityTransfer.set(min, max);
            this.updateOpacityTransfer();
            this.updateBufferAll();
        }
    }

    private void updateOpacityTransfer()
    {
        this.opacityFunction = this.opacityTransfer.toFunction();
        this.observable.changed();
        this.updateBufferAll();
    }

    private boolean hasData()
    {
        return this.volume.hasData();
    }

    private void checkBuffers()
    {
        Sampling sampling = this.getSampling();

        this.textureI.computeSize(sampling.numI());
        this.textureJ.computeSize(sampling.numJ());
        this.textureK.computeSize(sampling.numK());

        this.textureI.allocateBuffer(this.textureJ.size, this.textureK.size);
        this.textureJ.allocateBuffer(this.textureI.size, this.textureK.size);
        this.textureK.allocateBuffer(this.textureI.size, this.textureJ.size);
    }

    public List<ViewableAction> getLocalActions()
    {
        List<ViewableAction> out = Lists.newArrayList();
        {
            ViewableAction action = new ViewableAction();
            action.withName("Convert to RGB");
            action.withDescription("apply the current colormap to the data to produce the resulting RGB data volume");
            action.withFunction(data -> Optional.of(this.exportColormapRGB(false).get()));

            out.add(action);
        }

        {
            ViewableAction action = new ViewableAction();
            action.withName("Convert to RGBA");
            action.withDescription("apply the current colormap to the data to produce the resulting RGBA data volume");
            action.withFunction(data -> Optional.of(this.exportColormapRGB(true).get()));
            out.add(action);
        }

        return out;
    }

    public Optional<Volume> exportColormapRGB(boolean alpha)
    {
        Sampling s = this.getSampling();
        Mask m = this.mask == null ? null : this.mask.getData();
        Volume w = this.weight == null ? null : this.weight.getData();
        Volume o = this.opacityMap == null ? null : this.opacityMap.getData();

        Volume out = VolumeSource.create(s, alpha ? 4 : 3);

        for (Sample sample : s)
        {
            if (m == null || m.foreground(sample))
            {
                Vect val = this.getValue(sample);

                double op = 1.0;
                if (w != null)
                {
                    val.timesEquals(w.get(sample, 0));
                }
                if (o != null)
                {
                    double ov = o.getDim() > 1 ? o.get(sample).norm() : o.get(sample, 0);
                    op = this.opacityFunction.apply(VectSource.create1D(ov)).get(0);
                }

                Vect color = this.rgba(val, op);

                out.set(sample, alpha ? color : color.sub(0, 3));
            }
        }

        return Optional.of(out);
    }

    private void fillTextureQ()
    {
        if (!this.volume.hasData())
        {
            return;
        }

        long start = System.currentTimeMillis();
        Mask m = this.mask == null ? null : this.mask.getData();
        Volume w = this.weight == null ? null : this.weight.getData();
        Volume o = this.opacityMap == null ? null : this.opacityMap.getData();

        int ri = this.slicer.idxI();
        int rj = this.slicer.idxJ();
        int rk = this.slicer.idxK();

        boolean changedI = false;
        boolean changedJ = false;
        boolean changedK = false;

        int count = 0;
        while (!this.textureQ.isEmpty())
        {
            Sample sample = this.textureQ.poll();
            int si = sample.getI();
            int sj = sample.getJ();
            int sk = sample.getK();

            double myw = w == null ? 1.0 : w.get(sample, 0);
            double myop = 1.0;
            if (o != null)
            {
                double ov = o.getDim() > 1 ? o.get(sample).norm() : o.get(sample, 0);
                myop = this.opacityFunction.apply(VectSource.create1D(ov)).get(0);
            }

            if (si == ri || sj == rj || sk == rk)
            {
                if (m != null && m.background(sample))
                {
                    continue;
                }

                if (si == ri)
                {
                    int idx = 4 * this.textureI.width * sk + 4 * sj;
                    int color = this.pack(this.getValueI(sample).times(myw), myop);
                    this.textureI.data.putInt(idx, color);
                    changedI = true;
                    count += 1;
                }

                if (sj == rj)
                {
                    int idx = 4 * this.textureJ.width * sk + 4 * si;
                    int color = this.pack(this.getValueJ(sample).times(myw), myop);
                    this.textureJ.data.putInt(idx, color);
                    changedJ = true;
                    count += 1;
                }

                if (sk == rk)
                {
                    int idx = 4 * this.textureK.width * sj + 4 * si;
                    int color = this.pack(this.getValueK(sample).times(myw), myop);
                    this.textureK.data.putInt(idx, color);
                    changedK = true;
                    count += 1;
                }
            }
        }

        this.textureI.changed |= changedI;
        this.textureJ.changed |= changedJ;
        this.textureK.changed |= changedK;

        long dur = System.currentTimeMillis() - start;
        Logging.info(String.format("updated data in buffer queue for %s with %d voxels, which took %d ms", this.volume.getName(), count, (int) dur));
    }

    private void fillTextureI()
    {
        this.clearI();
        if (!this.volume.hasData())
        {
            return;
        }
        this.checkBuffers();

        long start = System.currentTimeMillis();
        Sampling s = this.getSampling();
        Mask m = this.mask == null ? null : this.mask.getData();
        Volume w = this.weight == null ? null : this.weight.getData();
        Volume o = this.opacityMap == null ? null : this.opacityMap.getData();

        for (int b = 0; b < s.numK(); b++)
        {
            for (int a = 0; a < s.numJ(); a++)
            {
                Sample samp = new Sample(this.slicer.idxI(), a, b);
                int color = 0;
                if (m == null || m.foreground(samp))
                {
                    Vect val = this.getValueI(samp);
                    double op = 1.0;
                    if (w != null)
                    {
                        val.timesEquals(w.get(samp, 0));
                    }
                    if (o != null)
                    {
                        double ov = o.getDim() > 1 ? o.get(samp).norm() : o.get(samp, 0);
                        op = this.opacityFunction.apply(VectSource.create1D(ov)).get(0);
                    }
                    color = this.pack(val, op);
                }

                int idx = 4 * this.textureI.width * b + 4 * a;
                this.textureI.data.putInt(idx, color);
            }
        }

        this.textureI.changed = true;

        long dur = System.currentTimeMillis() - start;
        Logging.info(String.format("updated data in bufferI for %s, which took %d ms", this.volume.getName(), (int) dur));
    }

    private void fillTextureJ()
    {
        this.clearI();
        if (!this.volume.hasData())
        {
            return;
        }
        this.checkBuffers();
        long start = System.currentTimeMillis();

        Sampling s = this.getSampling();
        Mask m = this.mask == null ? null : this.mask.getData();
        Volume w = this.weight == null ? null : this.weight.getData();
        Volume o = this.opacityMap == null ? null : this.opacityMap.getData();

        for (int b = 0; b < s.numK(); b++)
        {
            for (int a = 0; a < s.numI(); a++)
            {
                Sample samp = new Sample(a, this.slicer.idxJ(), b);
                int color = 0;
                if (m == null || m.foreground(samp))
                {
                    Vect val = this.getValueJ(samp);
                    double op = 1.0;
                    if (w != null)
                    {
                        val.timesEquals(w.get(samp, 0));
                    }
                    if (o != null)
                    {
                        double ov = o.getDim() > 1 ? o.get(samp).norm() : o.get(samp, 0);
                        op = this.opacityFunction.apply(VectSource.create1D(ov)).get(0);
                    }
                    color = this.pack(val, op);
                }

                int idx = 4 * this.textureJ.width * b + 4 * a;
                this.textureJ.data.putInt(idx, color);
            }
        }

        this.textureJ.changed = true;

        long dur = System.currentTimeMillis() - start;
        Logging.info(String.format("updated data in bufferJ for %s, which took %d ms", this.volume.getName(), (int) dur));
    }

    private void fillTextureK()
    {
        this.clearI();
        if (!this.volume.hasData())
        {
            return;
        }
        this.checkBuffers();
        long start = System.currentTimeMillis();

        Sampling s = this.getSampling();
        Mask m = this.mask == null ? null : this.mask.getData();
        Volume w = this.weight == null ? null : this.weight.getData();
        Volume o = this.opacityMap == null ? null : this.opacityMap.getData();

        int count = 0;
        for (int b = 0; b < s.numJ(); b++)
        {
            for (int a = 0; a < s.numI(); a++)
            {
                Sample samp = new Sample(a, b, this.slicer.idxK());
                int color = 0;
                if (m == null || m.foreground(samp))
                {
                    Vect val = this.getValueK(samp);
                    double op = 1.0;
                    if (w != null)
                    {
                        val.timesEquals(w.get(samp, 0));
                    }
                    if (o != null)
                    {
                        double ov = o.getDim() > 1 ? o.get(samp).norm() : o.get(samp, 0);
                        op = this.opacityFunction.apply(VectSource.create1D(ov)).get(0);
                    }
                    color = this.pack(val, op);
                }

                int idx = 4 * this.textureK.width * b + 4 * a;
                this.textureK.data.putInt(idx, color);
                count += 1;
            }
        }
        this.textureK.changed = true;

        long dur = System.currentTimeMillis() - start;
        Logging.info(String.format("updated data in bufferK for %s with %d voxels, which took %d ms", this.volume.getName(), count, (int) dur));
    }

    public Vect color(Vect value)
    {
        if (this.colormap == null || this.channel >= value.size() || this.channel < 0)
        {
            return VectSource.create3D();
        }

        int cdim = this.colormap.getDimIn();

        if (cdim > value.size())
        {
            return VectSource.create3D();
        }

        Vect cval = (cdim == 1) ? VectSource.create1D(value.get(this.channel)) : value.copy(cdim);
        Vect vcolor = this.colormap.apply(cval);

        return vcolor;
    }

    public Vect rgba(Vect value, double op)
    {
        if (value == null || value.nan() || value.infinite())
        {
            value = VectSource.create1D(0);
        }

        if (this.colormap == null || this.channel >= value.size() || this.channel < 0)
        {
            return VectSource.create4D();
        }

        final boolean mask = (this.volume instanceof MaskView);
        if (mask && this.whichidx.size() > 0 && !this.whichidx.contains((int) Math.round(value.get(0))))
        {
            return VectSource.create4D();
        }

        int cdim = this.colormap.getDimIn();

        if (cdim > value.size())
        {
            return VectSource.create4D();
        }

        Vect cval = (cdim == 1) ? VectSource.create1D(value.get(this.channel)) : value.copy(cdim);
        Vect vcolor = this.colormap.apply(cval);

        Double max = value.max();
        double alpha = this.nobg && (max == null || Math.abs(max) <= this.bglevel) ? 0 : this.opacity * op * vcolor.get(3);

        double red = vcolor.getX();
        double green = vcolor.getY();
        double blue = vcolor.getZ();

        return VectSource.create4D(red, green, blue, alpha);
    }

    public int pack(Vect value, double op)
    {
        if (value == null || value.nan() || value.infinite())
        {
            value = VectSource.create1D(0);
        }

        if (this.colormap == null || this.channel >= value.size() || this.channel < 0)
        {
            return 0;
        }

        final boolean mask = (this.volume instanceof MaskView);
        if (mask && this.whichidx.size() > 0 && !this.whichidx.contains((int) Math.round(value.get(0))))
        {
            return 0;
        }

        int cdim = this.colormap.getDimIn();

        if (cdim > value.size())
        {
            return 0;
        }

        Vect cval = (cdim == 1) ? VectSource.create1D(value.get(this.channel)) : value.copy(cdim);
        Vect vcolor = this.colormap.apply(cval);

        Double max = value.max();
        double alpha = this.nobg && (max == null || Math.abs(max) <= this.bglevel) ? 0 : this.opacity * op * vcolor.get(3);

        // compute 0<->255 RGB values
        int r = (int) Math.round(vcolor.get(0) * 255);
        int g = (int) Math.round(vcolor.get(1) * 255);
        int b = (int) Math.round(vcolor.get(2) * 255);
        int a = (int) Math.round(alpha * 255);

        // pack into int array
        int icolor = 0;
        icolor |= (r & 0xFF) << 24;
        icolor |= (g & 0xFF) << 16;
        icolor |= (b & 0xFF) << 8;
        icolor |= (a & 0xFF) << 0;

        return icolor;
    }

    public synchronized void displaySliceI(GL2 gl)
    {
        Sampling sampling = this.getSampling();
        TextureBuffer buffer = this.textureI;

        this.slicer = this.volume.getSlicer();

        if (this.textureI.fill)
        {
            this.fillTextureI();
            this.textureI.fill = false;
        }

        if (this.fillTextureQ)
        {
            this.fillTextureQ();
            this.fillTextureQ = false;
        }

        double lowY = -0.5;
        double lowZ = -0.5;
        double highY = sampling.numJ() - 0.5;
        double highZ = sampling.numK() - 0.5;
        double slice = this.slicer.idxI() + this.offset.get(0);

        Vect a = sampling.world(VectSource.create3D(slice, lowY, lowZ));
        Vect b = sampling.world(VectSource.create3D(slice, highY, lowZ));
        Vect c = sampling.world(VectSource.create3D(slice, highY, highZ));
        Vect d = sampling.world(VectSource.create3D(slice, lowY, highZ));

        this.mesh.vattr.setAll(Mesh.COLOR, VectSource.create(1, 0, 0, 1));
        int numW = sampling.numJ();
        int numH = sampling.numK();

        this.displaySlice(gl, buffer, numW, numH, a, b, c, d);
    }

    public synchronized void displaySliceJ(GL2 gl)
    {
        Sampling sampling = this.getSampling();
        TextureBuffer buffer = this.textureJ;

        this.slicer = this.volume.getSlicer();

        if (this.textureJ.fill)
        {
            this.fillTextureJ();
            this.textureJ.fill = false;
        }

        if (this.fillTextureQ)
        {
            this.fillTextureQ();
            this.fillTextureQ = false;
        }

        double lowX = -0.5;
        double lowZ = -0.5;
        double highX = sampling.numI() - 0.5;
        double highZ = sampling.numK() - 0.5;
        double slice = this.slicer.idxJ() + this.offset.get(1);

        Vect a = sampling.world(VectSource.create3D(lowX, slice, lowZ));
        Vect b = sampling.world(VectSource.create3D(highX, slice, lowZ));
        Vect c = sampling.world(VectSource.create3D(highX, slice, highZ));
        Vect d = sampling.world(VectSource.create3D(lowX, slice, highZ));

        this.mesh.vattr.setAll(Mesh.COLOR, VectSource.create(0, 1, 0, 1));
        int numW = sampling.numI();
        int numH = sampling.numK();

        this.displaySlice(gl, buffer, numW, numH, a, b, c, d);
    }

    public synchronized void displaySliceK(GL2 gl)
    {
        Sampling sampling = this.getSampling();
        TextureBuffer buffer = this.textureK;

        this.slicer = this.volume.getSlicer();

        if (this.textureK.fill)
        {
            this.fillTextureK();
            this.textureK.fill = false;
        }

        if (this.fillTextureQ)
        {
            this.fillTextureQ();
            this.fillTextureQ = false;
        }

        double lowX = -0.5;
        double lowY = -0.5;
        double highX = sampling.numI() - 0.5;
        double highY = sampling.numJ() - 0.5;
        double slice = this.slicer.idxK() + this.offset.get(2);

        Vect a = sampling.world(VectSource.create3D(lowX, lowY, slice));
        Vect b = sampling.world(VectSource.create3D(highX, lowY, slice));
        Vect c = sampling.world(VectSource.create3D(highX, highY, slice));
        Vect d = sampling.world(VectSource.create3D(lowX, highY, slice));

        this.mesh.vattr.setAll(Mesh.COLOR, VectSource.create(0, 0, 1, 1));
        int numW = sampling.numI();
        int numH = sampling.numJ();

        this.displaySlice(gl, buffer, numW, numH, a, b, c, d);
    }

    private void displaySlice(GL2 gl, TextureBuffer tex, int numW, int numH, Vect a, Vect b, Vect c, Vect d)
    {
        if (numW == 1 || numH == 1 || tex.data == null)
        {
            return;
        }

        Settings settings = Viewer.getInstance().settings;

        int texW = tex.width;
        int texH = tex.height;

        double scaleW = (double) numW / (double) texW;
        double scaleH = (double) numH / (double) texH;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3f(1f, 1f, 1f);

        gl.glEnable(GL2.GL_TEXTURE_2D);

        if (tex.index == null)
        {
            int[] sid = new int[1];
            gl.glGenTextures(1, sid, 0);
            tex.index = sid[0];
            Logging.info(String.format("created GL texture %d for data %s", tex.index, this.volume.getName()));
        }

        gl.glBindTexture(GL2.GL_TEXTURE_2D, tex.index);
        int glinterp = this.smooth ? GL2.GL_LINEAR : GL2.GL_NEAREST;
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, glinterp);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, glinterp);

        if (tex.changed)
        {
            long start = System.currentTimeMillis();
            gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, texW, texH, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_INT_8_8_8_8_REV, tex.data);
            long dur = System.currentTimeMillis() - start;
            tex.changed = false;
            Logging.info(String.format("pushing to GL texture %d with data %s, which took %d ms", tex.index, this.volume.getName(), (int) dur));
        }

        // this still has issues with depth ordering...
        gl.glDisable(GL2.GL_CULL_FACE);
        gl.glEnable(GL2.GL_COLOR);
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL2.GL_ALPHA_TEST);
        gl.glAlphaFunc(GL2.GL_ALWAYS, 0.0f);

        gl.glBegin(GL2.GL_QUADS);
        gl.glNormal3d(0, 1, 0);

        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(a.get(0), a.get(1), a.get(2));

        gl.glTexCoord2d(scaleW, 0.0);
        gl.glVertex3d(b.get(0), b.get(1), b.get(2));

        gl.glTexCoord2d(scaleW, scaleH);
        gl.glVertex3d(c.get(0), c.get(1), c.get(2));

        gl.glTexCoord2d(0.0, scaleH);
        gl.glVertex3d(d.get(0), d.get(1), d.get(2));

        gl.glEnd();

        // without this all other geometry looks for textures
        gl.glDisable(GL2.GL_TEXTURE_2D);

        if (this.tube)
        {
            Vect[] vs = {a, b, c, d};
            for (int i = 0; i < 4; i++)
            {
                Vect curr = vs[i % 4];
                Vect next = vs[(i + 1) % 4];

                Vect delta = next.minus(curr);
                double length = delta.norm();
                Vect dir = delta.normalize();
                gl.glColor3d(1.0, 0.0, 0.0);

                double thick = TUBE_THICK;
                if (this.hasData())
                {
                    Sampling s = this.getSampling();
                    thick = 0.001 * s.deltaMax() * s.numMax();
                }

                Vect scale = VectSource.create3D(thick, thick, length / 2.0);
                Vect pos = curr.plus(0.5, delta);

                gl.glPushMatrix();
                RenderUtils.glTransform(gl, scale, pos, dir);
                this.tubeRender.render(gl, this.mesh);
                gl.glPopMatrix();
            }
        }

        if (this.grid)
        {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glLineWidth(settings.scaleWidth);

            double rc = settings.scaleRed;
            double gc = settings.scaleGreen;
            double bc = settings.scaleBlue;
            gl.glColor3d(rc, gc, bc);

            Vect lift = b.minus(a).cross(d.minus(a)).normalize().times(LIFT);
            for (int idx = 0; idx <= numW; idx++)
            {
                double left = idx / (double) numW;
                double right = 1 - left;

                Vect start = a.times(left).plus(right, b);
                Vect end = d.times(left).plus(right, c);

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(start.getX() + lift.getX(), start.getY() + lift.getY(), start.getZ() + lift.getZ());
                gl.glVertex3d(end.getX() + lift.getX(), end.getY() + lift.getY(), end.getZ() + lift.getZ());
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(start.getX() - lift.getX(), start.getY() - lift.getY(), start.getZ() - lift.getZ());
                gl.glVertex3d(end.getX() - lift.getX(), end.getY() - lift.getY(), end.getZ() - lift.getZ());
                gl.glEnd();
            }

            for (int idx = 0; idx <= numH; idx++)
            {
                double left = idx / (double) numH;
                double right = 1 - left;

                Vect start = a.times(left).plus(right, d);
                Vect end = b.times(left).plus(right, c);

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(start.getX() + lift.getX(), start.getY() + lift.getY(), start.getZ() + lift.getZ());
                gl.glVertex3d(end.getX() + lift.getX(), end.getY() + lift.getY(), end.getZ() + lift.getZ());
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(start.getX() - lift.getX(), start.getY() - lift.getY(), start.getZ() - lift.getZ());
                gl.glVertex3d(end.getX() - lift.getX(), end.getY() - lift.getY(), end.getZ() - lift.getZ());
                gl.glEnd();
            }
        }
    }

    public void displayBox(GL2 gl)
    {
        if (this.box)
        {
            Settings settings = Viewer.getInstance().settings;

            gl.glDisable(GL2.GL_LIGHTING);
            gl.glLineWidth(settings.scaleWidth);

            double rc = settings.scaleRed;
            double gc = settings.scaleGreen;
            double bc = settings.scaleBlue;
            gl.glColor3d(rc, gc, bc);

            Sampling sampling = this.getSampling();

            int x = sampling.numI();
            int y = sampling.numJ();
            int z = sampling.numK();

            Vect topA = sampling.world(0, 0, z);
            Vect topB = sampling.world(0, y, z);
            Vect topC = sampling.world(x, y, z);
            Vect topD = sampling.world(x, 0, z);
            Vect botA = sampling.world(0, 0, 0);
            Vect botB = sampling.world(0, y, 0);
            Vect botC = sampling.world(x, y, 0);
            Vect botD = sampling.world(x, 0, 0);

            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(topA.getX(), topA.getY(), topA.getZ());
            gl.glVertex3d(topB.getX(), topB.getY(), topB.getZ());
            gl.glVertex3d(topC.getX(), topC.getY(), topC.getZ());
            gl.glVertex3d(topD.getX(), topD.getY(), topD.getZ());
            gl.glVertex3d(topA.getX(), topA.getY(), topA.getZ());
            gl.glEnd();

            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(botA.getX(), botA.getY(), botA.getZ());
            gl.glVertex3d(botB.getX(), botB.getY(), botB.getZ());
            gl.glVertex3d(botC.getX(), botC.getY(), botC.getZ());
            gl.glVertex3d(botD.getX(), botD.getY(), botD.getZ());
            gl.glVertex3d(botA.getX(), botA.getY(), botA.getZ());
            gl.glEnd();

            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(topA.getX(), topA.getY(), topA.getZ());
            gl.glVertex3d(botA.getX(), botA.getY(), botA.getZ());
            gl.glEnd();

            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(topB.getX(), topB.getY(), topB.getZ());
            gl.glVertex3d(botB.getX(), botB.getY(), botB.getZ());
            gl.glEnd();

            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(topC.getX(), topC.getY(), topC.getZ());
            gl.glVertex3d(botC.getX(), botC.getY(), botC.getZ());
            gl.glEnd();

            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(topD.getX(), topD.getY(), topD.getZ());
            gl.glVertex3d(botD.getX(), botD.getY(), botD.getZ());
            gl.glEnd();
        }
    }

    public void displayReference(GL2 gl)
    {
        if (this.pointA != null && this.pointB != null && this.pointC != null)
        {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor3d(255, 0, 0);
            gl.glEnable(GL2.GL_POINT_SMOOTH);

            gl.glPointSize(5);
            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex3d(this.pointA.getX(), this.pointA.getY(), this.pointA.getZ());
            gl.glEnd();

            gl.glPointSize(10);
            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex3d(this.pointB.getX(), this.pointB.getY(), this.pointB.getZ());
            gl.glEnd();

            gl.glPointSize(5);
            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex3d(this.pointC.getX(), this.pointC.getY(), this.pointC.getZ());
            gl.glEnd();

            gl.glPointSize(1);

            gl.glLineWidth(2);
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex3d(this.pointA.getX(), this.pointA.getY(), this.pointA.getZ());
            gl.glVertex3d(this.pointB.getX(), this.pointB.getY(), this.pointB.getZ());
            gl.glVertex3d(this.pointC.getX(), this.pointC.getY(), this.pointC.getZ());
            gl.glEnd();
        }
    }

    public void display(GL2 gl)
    {
        if (this.visible && this.hasData())
        {
            this.slicer = this.volume.getSlicer();

            if (this.updateColoring)
            {
                this.updateColoringDirect();
                this.updateColoring = false;
            }

            if (this.slicer.showI())
            {
                displaySliceI(gl);
            }

            if (this.slicer.showJ())
            {
                displaySliceJ(gl);
            }

            if (this.slicer.showK())
            {
                displaySliceK(gl);
            }

            this.displayBox(gl);
            this.displayReference(gl);
        }
    }

    public void updateColoring()
    {
        this.updateColoring = true;
    }

    public void updateColoringDirect()
    {
        Logging.info("started updating colormap of " + this.volume.getName());
        String type = (String) this.comboColorType.getSelectedItem();

        switch (type)
        {
            case ColormapState.SCALAR:
                this.colormap = ((ColormapScalar) this.comboColorScalar.getSelectedItem()).getFunction();
                break;
            case ColormapState.VECTOR:
                this.colormap = ((ColormapVector) this.comboColorVector.getSelectedItem()).getFunction();
                break;
        }

        this.updateBufferAll();
    }

    public void setSample(Sample sample)
    {
        if (this.hasData())
        {
            Sample previous = this.volume.getSlicer().sample();
            this.volume.getSlicer().set(sample);

            if (previous.getI() != sample.getI())
            {
                this.updateBufferI();
            }

            if (previous.getJ() != sample.getJ())
            {
                this.updateBufferJ();
            }

            if (previous.getK() != sample.getK())
            {
                this.updateBufferK();
            }
        }
    }

    public void updateAll()
    {
        this.updateColoring();
    }

    public void updateBufferAll()
    {
        this.updateBufferI();
        this.updateBufferJ();
        this.updateBufferK();
    }

    public void updateBufferQ()
    {
        this.fillTextureQ = true;
    }

    public void updateBufferI()
    {
        this.textureI.fill = true;
    }

    public void updateBufferJ()
    {
        this.textureJ.fill = true;
    }

    public void updateBufferK()
    {
        this.textureK.fill = true;
    }

    private void pushBackI()
    {
        if (this.adapterI != null && this.adapterI.changed)
        {
            // avoid recursive calls by getting the data directly
            Object data = RenderVolumeSlice.this.volume.getDataDirect();
            Global.assume(data instanceof Mask, "expected a mask");
            Mask mask = (Mask) data;

            int i = this.adapterI.index;
            int nj = this.getSampling().numJ();
            int nk = this.getSampling().numK();

            for (int j = 0; j < nj; j++)
            {
                for (int k = 0; k < nk; k++)
                {
                    mask.set(i, j, k, (int) Math.round(this.adapterI.data.get(j, k, 0, 0)));
                }
            }

            this.adapterI.changed = false;
        }
    }

    private void pushBackJ()
    {
        if (this.adapterJ != null && this.adapterJ.changed)
        {
            // avoid recursive calls by getting the data directly
            Object data = RenderVolumeSlice.this.volume.getDataDirect();
            Global.assume(data instanceof Mask, "expected a mask");
            Mask mask = (Mask) data;

            int j = this.adapterJ.index;
            int ni = this.getSampling().numI();
            int nk = this.getSampling().numK();

            for (int i = 0; i < ni; i++)
            {
                for (int k = 0; k < nk; k++)
                {
                    mask.set(i, j, k, (int) Math.round(this.adapterJ.data.get(i, k, 0, 0)));
                }
            }

            this.adapterJ.changed = false;
        }
    }

    private void pushBackK()
    {
        if (this.adapterK != null && this.adapterK.changed)
        {
            // avoid recursive calls by getting the data directly
            Object data = RenderVolumeSlice.this.volume.getDataDirect();
            Global.assume(data instanceof Mask, "expected a mask");
            Mask mask = (Mask) data;

            int k = this.adapterK.index;
            int ni = this.getSampling().numI();
            int nj = this.getSampling().numJ();

            for (int i = 0; i < ni; i++)
            {
                for (int j = 0; j < nj; j++)
                {
                    mask.set(i, j, k, (int) Math.round(this.adapterK.data.get(i, j, 0, 0)));
                }
            }

            this.adapterK.changed = false;
        }
    }

    private void clearI()
    {
        if (this.adapterI != null)
        {
            this.pushBackI();
            this.adapterI.data = null;
            this.adapterI = null;
        }
    }

    private void clearJ()
    {
        if (this.adapterJ != null)
        {
            this.pushBackJ();
            this.adapterJ.data = null;
            this.adapterJ = null;
        }
    }

    private void clearK()
    {
        if (this.adapterK != null)
        {
            this.pushBackK();
            this.adapterK.data = null;
            this.adapterK = null;
        }
    }

    public Map<Sample, Integer> setValues(Map<Sample, Integer> values)
    {
        Map<Sample, Integer> cache = Maps.newHashMap();

        if (values.size() == 0)
        {
            return cache;
        }

        for (Sample sample : values.keySet())
        {
            int label = values.get(sample);

            if (this.adapterI != null && sample.getI() == this.adapterI.index)
            {
                cache.put(sample, (int) this.adapterI.data.get(sample.getJ(), sample.getK(), 0, 0));

                this.adapterI.changed = true;
                this.adapterI.data.set(sample.getJ(), sample.getK(), 0, label);
                // this.updateBufferI();
            }

            if (this.adapterJ != null && sample.getJ() == this.adapterJ.index)
            {
                cache.put(sample, (int) this.adapterJ.data.get(sample.getI(), sample.getK(), 0, 0));

                this.adapterJ.changed = true;
                this.adapterJ.data.set(sample.getI(), sample.getK(), 0, label);
                // this.updateBufferJ();
            }

            if (this.adapterK != null && sample.getK() == this.adapterK.index)
            {
                cache.put(sample, (int) this.adapterK.data.get(sample.getI(), sample.getJ(), 0, 0));

                this.adapterK.changed = true;
                this.adapterK.data.set(sample.getI(), sample.getJ(), 0, label);
                // this.updateBufferK();
            }
        }

        this.textureQ.addAll(values.keySet());
        this.updateBufferQ();

        return cache;
    }

    private Vect getValueI(Sample sample)
    {
        int i = sample.getI();

        // check if we have to update the slice (use lazy evaluation)
        if (this.adapterI == null || i != this.adapterI.index)
        {
            Sampling sampling = this.getSampling();
            int nj = sampling.numJ();
            int nk = sampling.numK();

            Volume vals = VolumeSource.create(nj, nk, 1, this.getDim());

            for (int j = 0; j < nj; j++)
            {
                for (int k = 0; k < nk; k++)
                {
                    int myslab = RenderVolumeSlice.this.slab;
                    if (myslab == 1)
                    {
                        vals.set(j, k, 0, this.getValue(i, j, k));
                    }
                    else
                    {
                        int count = 0;
                        Vect stat = null;
                        for (int ii = i - myslab; ii <= i + myslab; ii++)
                        {
                            if (sampling.contains(ii, j, k))
                            {
                                Vect value = this.getValue(ii, j, k);
                                count += 1;

                                if (stat != null)
                                {
                                    switch (RenderVolumeSlice.this.slabType)
                                    {
                                        case Mean:
                                            stat.plusEquals(value.minus(stat).times(1.0 / (double) count));
                                            break;
                                        case Max:
                                            stat.maxEquals(value);
                                            break;
                                        case Min:
                                            stat.minEquals(value);
                                            break;
                                    }
                                }
                                else
                                {
                                    stat = value;
                                }
                            }

                            vals.set(j, k, 0, stat);
                        }
                    }
                }
            }

            this.pushBackI();
            this.adapterI = new AdapterBuffer(i, vals);
        }

        return this.adapterI.data.get(sample.getJ(), sample.getK(), 0);
    }

    private Vect getValueJ(Sample sample)
    {
        int j = sample.getJ();

        // check if we have to update the slice (use lazy evaluation)
        if (this.adapterJ == null || j != this.adapterJ.index)
        {
            Sampling sampling = this.getSampling();
            int ni = this.getSampling().numI();
            int nk = this.getSampling().numK();

            Volume vals = VolumeSource.create(ni, nk, 1, this.getDim());

            for (int i = 0; i < ni; i++)
            {
                for (int k = 0; k < nk; k++)
                {
                    int myslab = RenderVolumeSlice.this.slab;
                    if (myslab == 1)
                    {
                        vals.set(i, k, 0, this.getValue(i, j, k));
                    }
                    else
                    {
                        int count = 0;
                        Vect stat = null;
                        for (int jj = j - myslab; jj <= j + myslab; jj++)
                        {
                            if (sampling.contains(i, jj, k))
                            {
                                Vect value = this.getValue(i, jj, k);
                                count += 1;

                                if (stat != null)
                                {
                                    switch (RenderVolumeSlice.this.slabType)
                                    {
                                        case Mean:
                                            stat.plusEquals(value.minus(stat).times(1.0 / (double) count));
                                            break;
                                        case Max:
                                            stat.maxEquals(value);
                                            break;
                                        case Min:
                                            stat.minEquals(value);
                                            break;
                                    }
                                }
                                else
                                {
                                    stat = value;
                                }
                            }

                            vals.set(i, k, 0, stat);
                        }
                    }
                }
            }

            this.pushBackJ();
            this.adapterJ = new AdapterBuffer(j, vals);
        }

        return this.adapterJ.data.get(sample.getI(), sample.getK(), 0);
    }

    private Vect getValueK(Sample sample)
    {
        int k = sample.getK();

        // check if we have to update the slice (use lazy evaluation)
        if (this.adapterK == null || k != this.adapterK.index)
        {
            Sampling sampling = this.getSampling();
            int ni = this.getSampling().numI();
            int nj = this.getSampling().numJ();

            Volume vals = VolumeSource.create(ni, nj, 1, this.getDim());

            for (int i = 0; i < ni; i++)
            {
                for (int j = 0; j < nj; j++)
                {
                    int myslab = RenderVolumeSlice.this.slab;
                    if (myslab == 1)
                    {
                        vals.set(i, j, 0, this.getValue(i, j, k));
                    }
                    else
                    {
                        int count = 0;
                        Vect stat = null;
                        for (int kk = k - myslab; kk <= k + myslab; kk++)
                        {
                            if (sampling.contains(i, j, kk))
                            {
                                Vect value = this.getValue(i, j, kk);
                                count += 1;

                                if (stat != null)
                                {
                                    switch (RenderVolumeSlice.this.slabType)
                                    {
                                        case Mean:
                                            stat.plusEquals(value.minus(stat).times(1.0 / (double) count));
                                            break;
                                        case Max:
                                            stat.maxEquals(value);
                                            break;
                                        case Min:
                                            stat.minEquals(value);
                                            break;
                                    }
                                }
                                else
                                {
                                    stat = value;
                                }
                            }

                            vals.set(i, j, 0, stat);
                        }
                    }
                }
            }

            this.pushBackK();
            this.adapterK = new AdapterBuffer(k, vals);
        }

        return this.adapterK.data.get(sample.getI(), sample.getJ(), 0);
    }

    private Vect getValue(Sample sample)
    {
        return this.getValue(sample.getI(), sample.getJ(), sample.getK());
    }

    private Vect getValue(int i, int j, int k)
    {
        Object obj = RenderVolumeSlice.this.volume.getData();
        if (obj instanceof Mask)
        {
            return VectSource.create1D(((Mask) obj).get(i, j, k));
        }
        else
        {
            Volume vol = (Volume) obj;
            String feat = RenderVolumeSlice.this.feature;
            if (feat.equals(FEATURE_NONE))
            {
                return vol.get(i, j, k);
            }
            else
            {
                Model model = ModelUtils.proto(vol.getModel(), vol.getDim());
                model.setEncoding(vol.get(i, j, k));
                return model.feature(feat);
            }
        }
    }

    private Sampling getSampling()
    {
        // avoid recursive calls by getting the data directly
        Object out = RenderVolumeSlice.this.volume.getDataDirect();
        if (out instanceof Mask)
        {
            return ((Mask) out).getSampling();
        }
        else
        {
            Global.assume(out instanceof Volume, "expected a volume");
            return ((Volume) out).getSampling();
        }
    }

    private int getDim()
    {
        // avoid recursive calls by getting the data directly
        Object out = RenderVolumeSlice.this.volume.getDataDirect();
        if (out instanceof Mask)
        {
            return 1;
        }
        else
        {
            Global.assume(out instanceof Volume, "expected a volume");
            Volume vol = (Volume) out;
            String feat = RenderVolumeSlice.this.feature;
            if (feat.equals(FEATURE_NONE))
            {
                return vol.getDim();
            }
            else
            {
                Model model = ModelUtils.proto(vol.getModel(), vol.getDim());
                return model.feature(feat).size();
            }
        }
    }

    private ModelType getModel()
    {
        // avoid recursive calls by getting the data directly
        Object out = RenderVolumeSlice.this.volume.getDataDirect();
        if (out instanceof Mask)
        {
            return ModelType.Vect;
        }
        else
        {
            Global.assume(out instanceof Volume, "expected a volume");
            return ((Volume) out).getModel();
        }
    }

    public Double dist(WorldMouse mouse)
    {
        if (!this.hasData() || mouse.press == null || mouse.current == null)
        {
            return null;
        }

        Vect hit = mouse.current == null ? mouse.press.hit : mouse.current.hit;
        Sampling sampling = this.getSampling();
        Sample nearest = sampling.nearest(hit);

        boolean pi = this.slicer.idxI() == nearest.getI();
        boolean pj = this.slicer.idxJ() == nearest.getJ();
        boolean pk = this.slicer.idxK() == nearest.getK();

        if (pi || pj || pk)
        {
            return mouse.press.hit.dist(mouse.press.point);
        }
        else
        {
            return null;
        }
    }

    public void changeSliceI(int delta)
    {
        Sampling sampling = this.getSampling();
        Sample sample = this.slicer.sample();

        int ni = sample.getI() + delta;
        if (sampling.containsI(ni))
        {
            // Logging.info("moving i slice to index " + ni);
            this.slicer.setI(ni);
        }

        this.updateBufferI();
    }

    public void changeSliceJ(int delta)
    {
        Sampling sampling = this.getSampling();
        Sample sample = this.slicer.sample();

        int nj = sample.getJ() + delta;
        if (sampling.containsJ(nj))
        {
            // Logging.info("moving j slice to index " + nj);
            this.slicer.setJ(nj);
        }

        this.updateBufferJ();
    }

    public void changeSliceK(int delta)
    {
        Sampling sampling = this.getSampling();
        Sample sample = this.slicer.sample();

        int nk = sample.getK() + delta;
        if (sampling.containsK(nk))
        {
            // Logging.info("moving k slice to index " + nk);
            this.slicer.setK(nk);
        }

        this.updateBufferK();
    }

    public void changeSlice(int delta)
    {
        Sampling sampling = this.getSampling();

        Vect look = Viewer.getInstance().gui.canvas.render3D.look;

        double dotI = Math.abs(look.getX());
        double dotJ = Math.abs(look.getY());
        double dotK = Math.abs(look.getZ());

        boolean showI = this.slicer.showI();
        boolean showJ = this.slicer.showJ();
        boolean showK = this.slicer.showK();

        boolean frontI = showI && dotI > dotJ && dotI > dotK;
        boolean frontJ = showJ && dotJ > dotI && dotJ > dotK;
        boolean frontK = showK && dotK > dotI && dotK > dotJ;

        boolean onlyI = showI && !showJ && !showK;
        boolean onlyJ = !showI && showJ && !showK;
        boolean onlyK = !showI && !showJ && showK;

        Sample sample = this.slicer.sample();

        if (onlyI || frontI)
        {
            int ni = sample.getI() + delta;
            if (sampling.containsI(ni))
            {
                // Logging.info("moving i slice to index " + ni);
                this.slicer.setI(ni);
            }
        }
        else if (onlyJ || frontJ)
        {
            int nj = sample.getJ() + delta;
            if (sampling.containsJ(nj))
            {
                // Logging.info("moving j slice to index " + nj);
                this.slicer.setJ(nj);
            }
        }
        else if (onlyK || frontK)
        {
            int nk = sample.getK() + delta;
            if (sampling.containsK(nk))
            {
                // Logging.info("moving k slice to index " + nk);
                this.slicer.setK(nk);
            }
        }
        else
        {
            // skip it this time
        }

        this.updateBufferAll();
    }

    public void setSliceI(int ni)
    {
        Sampling sampling = this.getSampling();

        if (sampling.containsI(ni))
        {
            // Logging.info("moving i slice to index " + ni);
            this.slicer.setI(ni);
        }

        this.updateBufferI();
    }

    public void setSliceJ(int nj)
    {
        Sampling sampling = this.getSampling();

        if (sampling.containsJ(nj))
        {
            // Logging.info("moving j slice to index " + nj);
            this.slicer.setJ(nj);
        }

        this.updateBufferJ();
    }

    public void setSliceK(int nk)
    {
        Sampling sampling = this.getSampling();

        if (sampling.containsK(nk))
        {
            // Logging.info("moving k slice to index " + nk);
            this.slicer.setK(nk);
        }

        this.updateBufferK();
    }

    private void clearSelection()
    {
        this.picked = null;
        this.pointA = null;
        this.pointB = null;
        this.pointC = null;
    }

    private void handleSelect(WorldMouse mouse)
    {
        Vect hit = mouse.press == null ? mouse.current.hit : mouse.press.hit;
        Sampling sampling = this.getSampling();
        Sample nearest = sampling.nearest(hit);

        if (!sampling.contains(nearest))
        {
            this.picked = null;
            this.pointA = null;
            this.pointB = null;
            this.pointC = null;
            return;
        }

        Selection selection = new Selection();
        selection.world = hit;
        selection.voxel = nearest;

        int di = Math.abs(this.slicer.idxI() - nearest.getI());
        int dj = Math.abs(this.slicer.idxJ() - nearest.getJ());
        int dk = Math.abs(this.slicer.idxK() - nearest.getK());

        if (di < dj && di < dk)
        {
            if (this.slicer.showI())
            {
                selection.dir = Direction.I;

                Sample s = nearest;

                Sample sA = new Sample(0, s.getJ(), s.getK());
                Sample sB = new Sample(s.getI(), s.getJ(), s.getK());
                Sample sC = new Sample(sampling.numI() - 1, s.getJ(), s.getK());

                this.pointA = sampling.world(sA);
                this.pointB = sampling.world(sB);
                this.pointC = sampling.world(sC);

                Viewer.getInstance().gui.setStatusMessage("clicking will change selected slice");
            }
        }
        else if (dj < di && dj < dk)
        {
            if (this.slicer.showJ())
            {
                selection.dir = Direction.J;

                Sample s = nearest;

                Sample sA = new Sample(s.getI(), 0, s.getK());
                Sample sB = new Sample(s.getI(), s.getJ(), s.getK());
                Sample sC = new Sample(s.getI(), sampling.numJ(), s.getK());

                this.pointA = sampling.world(sA);
                this.pointB = sampling.world(sB);
                this.pointC = sampling.world(sC);

                Viewer.getInstance().gui.setStatusMessage("clicking will change selected slice");
            }
        }
        else if (this.slicer.showK())
        {
            selection.dir = Direction.K;

            Sample s = nearest;

            Sample sA = new Sample(s.getI(), s.getJ(), 0);
            Sample sB = new Sample(s.getI(), s.getJ(), s.getK());
            Sample sC = new Sample(s.getI(), s.getJ(), sampling.numK());

            this.pointA = sampling.world(sA);
            this.pointB = sampling.world(sB);
            this.pointC = sampling.world(sC);

            Viewer.getInstance().gui.setStatusMessage("clicking will change selected slice");
        }

        if (selection.dir != null)
        {
            // the above code might not find a visible direction
            this.picked = selection;
        }
    }

    private void handleQuery(WorldMouse mouse)
    {
        this.pointA = null;
        this.pointB = null;
        this.pointC = null;
        this.picked = null;

        Vect hit = mouse.current.hit;
        Sampling sampling = this.getSampling();
        Vect world = sampling.world(sampling.nearest(hit));

        Viewer.getInstance().control.setQuery(sampling, world);
        Viewer.getInstance().control.showQuery();
    }

    private void handleSlice(WorldMouse mouse)
    {
        if (mouse.press == null)
        {
            // only move when the mouse button is down
            return;
        }

        Sampling sampling = this.getSampling();

        if (this.picked == null || this.picked.dir == null)
        {
            return;
        }

        int pressX = mouse.press.screen.x;
        int pressY = mouse.press.screen.y;
        int currentX = mouse.current.screen.x;
        int currentY = mouse.current.screen.y;

        int deltaX = (int) Math.floor(Math.abs(currentX - pressX));
        deltaX = (int) Math.copySign(deltaX, currentX - pressX);
        int deltaY = (int) Math.floor(Math.abs(currentY - pressY));
        deltaY = (int) Math.copySign(deltaY, currentY - pressY);

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

        switch (this.picked.dir)
        {
            case I:
            {
                Vect dirI = VectSource.create(1, 0, 0);
                int ni = this.picked.voxel.getI() + delta(look, up, cross, dirI, deltaX, deltaY);
                if (sampling.containsI(ni) && slicer.idxI() != ni)
                {
                    // Logging.info("moving i slice to index " + ni);
                    this.slicer.setI(ni);

                    Sample s = this.picked.voxel;

                    Sample sA = new Sample(0, s.getJ(), s.getK());
                    Sample sB = new Sample(ni, s.getJ(), s.getK());
                    Sample sC = new Sample(sampling.numI() - 1, s.getJ(), s.getK());

                    this.pointA = sampling.world(sA);
                    this.pointB = sampling.world(sB);
                    this.pointC = sampling.world(sC);

                    this.updateBufferI();
                }

                break;
            }
            case J:
            {
                Vect dirJ = VectSource.create(0, 1, 0);
                int nj = this.picked.voxel.getJ() + delta(look, up, cross, dirJ, deltaX, deltaY);
                if (sampling.containsJ(nj) && slicer.idxJ() != nj)
                {
                    // Logging.info("moving j slice to index " + nj);
                    this.slicer.setJ(nj);

                    Sample s = this.picked.voxel;

                    Sample sA = new Sample(s.getI(), 0, s.getK());
                    Sample sB = new Sample(s.getI(), nj, s.getK());
                    Sample sC = new Sample(s.getI(), sampling.numJ(), s.getK());

                    this.pointA = sampling.world(sA);
                    this.pointB = sampling.world(sB);
                    this.pointC = sampling.world(sC);

                    this.updateBufferJ();
                }

                break;
            }
            case K:
            {
                Vect dirK = VectSource.create(0, 0, 1);
                int nk = this.picked.voxel.getK() + delta(look, up, cross, dirK, deltaX, deltaY);
                if (sampling.containsK(nk) && slicer.idxK() != nk)
                {
                    // Logging.info("moving k slice to index " + nk);
                    this.slicer.setK(nk);

                    Sample s = this.picked.voxel;

                    Sample sA = new Sample(s.getI(), s.getJ(), 0);
                    Sample sB = new Sample(s.getI(), s.getJ(), nk);
                    Sample sC = new Sample(s.getI(), s.getJ(), sampling.numK());

                    this.pointA = sampling.world(sA);
                    this.pointB = sampling.world(sB);
                    this.pointC = sampling.world(sC);

                    this.updateBufferK();
                }

                break;
            }
        }
    }

    private static int delta(Vect look, Vect up, Vect cross, Vect dir, int deltaX, int deltaY)
    {
        double dotCross = dir.dot(cross);
        double dotUp = dir.dot(up);
        double dotLook = dir.dot(look);
        double adotCross = Math.abs(dotCross);
        double adotUp = Math.abs(dotUp);
        double adotLook = Math.abs(dotLook);

        if (adotUp > adotCross && adotUp > adotLook)
        {
            return dotUp < 0 ? -deltaY : deltaY;
        }
        else if (adotCross > adotUp && adotCross > adotLook)
        {
            return dotCross < 0 ? -deltaX : deltaX;
        }
        else
        {
            return deltaY;
        }
    }

    public List<String> modes()
    {
        List<String> out = Lists.newArrayList();
        out.add(Interaction.Query.toString());
        out.add(Interaction.Slice.toString());
        return out;
    }

    private Interaction parse(WorldMouse mouse, String mode)
    {
        for (Interaction i : Interaction.values())
        {
            if (Interaction.Slice.toString().equals(mode) && (this.picked == null || mouse.press == null))
            {
                return Interaction.Select;
            }

            if (i.toString().equals(mode))
            {
                return i;
            }
        }

        if (Constants.INTERACTION_ROTATE.equals(mode) && mouse.pick)
        {
            if (mouse.shift && !mouse.control)
            {
                return Interaction.Query;
            }
            else if (!mouse.shift && !mouse.control)
            {
                if (mouse.press == null || this.picked == null)
                {
                    return Interaction.Select;
                }
                else
                {
                    return Interaction.Slice;
                }
            }
        }

        return Interaction.None;
    }

    public void handle(WorldMouse mouse, String mode)
    {
        if (!mouse.pick)
        {
            clearSelection();
        }

        if (!this.hasData() || mouse.current == null)
        {
            return;
        }

        switch (parse(mouse, mode))
        {
            case Select:
                handleSelect(mouse);
                return;
            case Query:
                handleQuery(mouse);
                return;
            case Slice:
                handleSlice(mouse);
                return;
            default:
                clearSelection();
        }
    }
}
