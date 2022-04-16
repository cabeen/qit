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
import qit.base.ModelType;
import qit.base.structs.Pair;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Volume;
import qit.data.modules.mri.model.VolumeModelFeature;
import qit.data.modules.volume.*;
import qit.data.source.MaskSource;
import qit.math.utils.colormaps.ColormapScalar;
import qitview.main.Viewer;
import qitview.models.*;
import qitview.panels.Viewables;
import qitview.render.RenderGlyph;
import qitview.render.RenderVolumeGlyph;
import qitview.render.RenderVolumeSlice;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicLabel;
import qitview.widgets.ControlPanel;
import qitview.widgets.SwingUtils;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.event.ItemEvent;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class VolumeView extends SliceableView<Volume>
{
    public static final String SLICE = "Slice Rendering";
    public static final String GLYPH = "Glyph Rendering";
    public static final String NONE = "None";
    public static final String PROTO = "           ";

    private transient RenderVolumeSlice<VolumeView> renderSlice = new RenderVolumeSlice<>(this);
    private transient RenderVolumeGlyph renderGlyphs = new RenderVolumeGlyph(this);
    private transient Map<String, Pair<JPanel, RenderGlyph>> glyphs = Maps.newLinkedHashMap();

    private transient BasicComboBox<String> glyphCombo;

    public Boolean glyphShow = false;

    public VolumeView()
    {
        super();

        for (Pair<String, RenderGlyph> glyph : RenderGlyph.getAll(() -> this.updateAll()))
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(glyph.b.getPanel());
            panel.setVisible(false);

            this.glyphs.put(glyph.a, Pair.of(panel, glyph.b));
        }

        super.initPanel();
    }

    public int getChannel()
    {
        return this.renderSlice.getChannel();
    }

    public void nextChannel()
    {
        this.renderSlice.nextChannel();
    }

    public void prevChannel()
    {
        this.renderSlice.prevChannel();
    }

    public void autoMinMax()
    {
        this.renderSlice.autoMinMax();
    }

    public VolumeView setData(Volume d)
    {
        if (d != null)
        {
            this.bounds = d.getSampling().bounds();
            this.slicer = Viewer.getInstance().control.getSlicer(d.getSampling());
            Viewer.getInstance().gui.canvas.render3D.boxUpdate();

            ModelType pmodel = this.hasData() ? this.getData().getModel() : ModelType.Vect;
            ModelType nmodel = d.getModel();

            if (!ModelType.Vect.equals(nmodel) && !pmodel.equals(nmodel))
            {
                this.renderSlice.setOpacity(1.0);
            }
        }

        super.setData(d);

        if (d != null)
        {
            Viewables v = Viewer.getInstance().data;

            if (d.getDim() == 3)
            {
                // use the rgb colormap when the data is 3D
                this.renderSlice.setVectorColormap();
            }
            else if (Viewer.getInstance().settings.colorshare)
            {
                // only update the colormap range the first time that it is used
                ComboBoxModel<ColormapScalar> model = Viewer.getInstance().colormaps.getComboScalar().getModel();
                List<String> names = Lists.newArrayList();
                for (int i = 0; i < model.getSize(); i++)
                {
                    names.add(model.getElementAt(i).getName());
                }

                boolean update = true;
                for (int i = 0; i < v.size(); i++)
                {
                    if (v.getViewable(i) instanceof VolumeView)
                    {
                        String name = ((VolumeView) v.getViewable(i)).renderSlice.getScalarColormapName();
                        if (name.equals(names.get(0)))
                        {
                            update = false;
                        }
                    }
                }

                if (update)
                {
                    this.autoMinMax();
                }
            }
            else
            {
                // get a list of scalar colormaps
                ComboBoxModel<ColormapScalar> model = Viewer.getInstance().colormaps.getComboScalar().getModel();
                List<String> names = Lists.newArrayList();
                for (int i = 0; i < model.getSize(); i++)
                {
                    names.add(model.getElementAt(i).getName());
                }

                // remove colormaps that are in use
                for (int i = 0; i < v.size(); i++)
                {
                    if (v.getViewable(i) instanceof VolumeView)
                    {
                        String name = ((VolumeView) v.getViewable(i)).renderSlice.getScalarColormapName();
                        names.remove(name);
                    }
                }

                // if there are any unused colormaps, use the first one and detect the intensity range
                if (names.size() > 0)
                {
                    this.renderSlice.setScalarColormap(names.get(0));
                    this.autoMinMax();
                }
            }
        }

        this.renderSlice.updateBufferAll();

        return this;
    }

    public String getScalarColormapName()
    {
        return this.renderSlice.getScalarColormapName();
    }

    public void setScalarColormap(String name)
    {
        this.renderSlice.setScalarColormap(name);
    }

    @Override
    public Volume getData()
    {
        return this.data;
    }

    protected ControlPanel makeInfoControls()
    {
        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Type: ", new BasicLabel(this.getClass().getSimpleName()));
        infoPanel.addControl(" ", new BasicLabel());

        final BasicLabel numI = new BasicLabel("");
        final BasicLabel numJ = new BasicLabel("");
        final BasicLabel numK = new BasicLabel("");
        final BasicLabel startI = new BasicLabel("");
        final BasicLabel startJ = new BasicLabel("");
        final BasicLabel startK = new BasicLabel("");
        final BasicLabel deltaI = new BasicLabel("");
        final BasicLabel deltaJ = new BasicLabel("");
        final BasicLabel deltaK = new BasicLabel("");
        final BasicLabel quatA = new BasicLabel("");
        final BasicLabel quatB = new BasicLabel("");
        final BasicLabel quatC = new BasicLabel("");
        final BasicLabel quatD = new BasicLabel("");
        final BasicLabel dim = new BasicLabel("");
        final BasicLabel model = new BasicLabel("");

        Runnable updateInfo = () ->
        {
            if (VolumeView.this.hasData())
            {
                Sampling sampling = VolumeView.this.data.getSampling();

                DecimalFormat df = new DecimalFormat("#0.000");
                model.setText(String.valueOf(VolumeView.this.data.getModel()));
                dim.setText(String.valueOf(VolumeView.this.data.getDim()));
                numI.setText(String.valueOf(sampling.numI()));
                numJ.setText(String.valueOf(sampling.numJ()));
                numK.setText(String.valueOf(sampling.numK()));
                startI.setText(df.format(sampling.startI()));
                startJ.setText(df.format(sampling.startJ()));
                startK.setText(df.format(sampling.startK()));
                deltaI.setText(df.format(sampling.deltaI()));
                deltaJ.setText(df.format(sampling.deltaJ()));
                deltaK.setText(df.format(sampling.deltaK()));
                quatA.setText(df.format(sampling.quatA()));
                quatB.setText(df.format(sampling.quatB()));
                quatC.setText(df.format(sampling.quatC()));
                quatD.setText(df.format(sampling.quatD()));
            }
        };

        this.observable.addObserver((a, b) -> updateInfo.run());

        infoPanel.addControl("Model: ", model);
        infoPanel.addControl("Dim: ", dim);
        infoPanel.addControl("Num I: ", numI);
        infoPanel.addControl("Num J: ", numJ);
        infoPanel.addControl("Num K: ", numK);
        infoPanel.addControl("Start I: ", startI);
        infoPanel.addControl("Start J: ", startJ);
        infoPanel.addControl("Start K: ", startK);
        infoPanel.addControl("Delta I: ", deltaI);
        infoPanel.addControl("Delta J: ", deltaJ);
        infoPanel.addControl("Delta K: ", deltaK);
        infoPanel.addControl("Quat A: ", quatA);
        infoPanel.addControl("Quat B: ", quatB);
        infoPanel.addControl("Quat C: ", quatC);
        infoPanel.addControl("Quat D: ", quatD);

        {
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

            String modeExport = "Export";
            String modeReplace = "Replace";
            BasicComboBox modeComboBox = new BasicComboBox();
            modeComboBox.addItem(modeExport);
            modeComboBox.addItem(modeReplace);
            panel.addControl("Mode", modeComboBox);

            final Supplier<Boolean> replace = () -> modeComboBox.getSelectedItem().equals(modeReplace);

            Consumer<ViewableAction> addAction = action ->
            {
                final BasicButton elem = new BasicButton(action.getName());
                elem.setToolTipText(action.getDescription());
                elem.addActionListener(e -> action.getAction(replace.get()).accept(this));
                panel.addControl(elem);
            };

            for (ViewableAction action : ViewableActions.Volume)
            {
                addAction.accept(action);
            }

            for (ViewableAction action : this.renderSlice.getLocalActions())
            {
                addAction.accept(action);
            }

            controls.put("Processing", panel);
        }
        return controls;
    }

    protected Map<String, ControlPanel> makeRenderControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();

        controls.put(SLICE, this.renderSlice.getPanel());

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
                for (String name : VolumeView.this.glyphs.keySet())
                {
                    VolumeView.this.glyphs.get(name).a.setVisible(false);
                }

                String selected = (String) VolumeView.this.glyphCombo.getSelectedItem();
                if (!NONE.equals(selected))
                {
                    VolumeView.this.glyphs.get(selected).a.setVisible(true);
                }
            });

            ControlPanel typePanel = new ControlPanel();
            {
                JCheckBox elem = new JCheckBox();
                elem.setToolTipText("make the glyphs visible");
                elem.addItemListener(e ->
                {
                    VolumeView.this.glyphShow = e.getStateChange() == ItemEvent.SELECTED;
                    Logging.info("updated glyph visibility to " + VolumeView.this.glyphShow);
                });
                elem.setSelected(this.glyphShow);
                typePanel.addControl("Visible", elem);
            }

            this.glyphCombo.setToolTipText("change the type of glyph shown (if you choose one that does not match the data, nothing will be shown)");
            typePanel.addControl("Type", this.glyphCombo);
            this.glyphCombo.addActionListener(a -> this.renderGlyphs.updateAll());
            glyphPanel.add(typePanel);
            glyphPanel.add(this.renderGlyphs.getPanel());

            for (String name : VolumeView.this.glyphs.keySet())
            {
                glyphPanel.add(VolumeView.this.glyphs.get(name).a);
            }

            controls.put(GLYPH, glyphPanel);
        }

        return controls;
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

    public void setSample(Sample sample)
    {
        this.renderSlice.setSample(sample);
    }

    public void dispose(GL2 gl)
    {
        this.renderSlice.dispose(gl);
        this.renderGlyphs.dispose(gl);
    }

    private void updateAll()
    {
        this.renderSlice.updateBufferAll();
        this.renderGlyphs.updateAll();
    }

    public void update(VolumeSlicePlane plane)
    {
        switch (plane)
        {
            case I:
                this.updateI();
                break;
            case J:
                this.updateJ();
                break;
            case K:
                this.updateK();
                break;
        }
    }

    public void updateI()
    {
        this.renderSlice.updateBufferI();
        this.renderGlyphs.updateI();
    }

    public void updateJ()
    {
        this.renderSlice.updateBufferJ();
        this.renderGlyphs.updateJ();
    }

    public void updateK()
    {
        this.renderSlice.updateBufferK();
        this.renderGlyphs.updateK();
    }

    public synchronized void display(GL2 gl, VolumeSlicePlane slice)
    {
        switch (slice)
        {
            case I:
                this.renderSlice.displaySliceI(gl);
                break;
            case J:
                this.renderSlice.displaySliceJ(gl);
                break;
            case K:
                this.renderSlice.displaySliceK(gl);
                break;
        }

        if (this.glyphShow)
        {
            RenderGlyph glyph = this.getGlyph();
            if (glyph != null)
            {
                switch (slice)
                {
                    case I:
                        this.renderGlyphs.displaySliceI(gl, glyph);
                        break;
                    case J:
                        this.renderGlyphs.displaySliceJ(gl, glyph);
                        break;
                    case K:
                        this.renderGlyphs.displaySliceK(gl, glyph);
                        break;
                }
            }
        }
    }

    public synchronized void display(GL2 gl)
    {
        if (this.glyphShow)
        {
            RenderGlyph glyph = this.getGlyph();
            if (glyph != null)
            {
                this.renderGlyphs.display(gl, glyph);
            }
        }

        if (this.renderSlice != null)
        {
            this.renderSlice.display(gl);
        }
    }

    public Double dist(WorldMouse mouse)
    {
        return this.renderSlice.dist(mouse);
    }

    public void changeSliceI(int delta)
    {
        this.renderSlice.changeSliceI(delta);
    }

    public void changeSliceJ(int delta)
    {
        this.renderSlice.changeSliceJ(delta);
    }

    public void changeSliceK(int delta)
    {
        this.renderSlice.changeSliceK(delta);
    }

    public void changeSlice(int delta)
    {
        this.renderSlice.changeSlice(delta);
    }

    public void changeSlice(int delta, VolumeSlicePlane plane)
    {
        if (plane == null)
        {
            this.changeSlice(delta);
        }
        else
        {
            switch (plane)
            {
                case I:
                    this.changeSliceI(delta);
                    break;
                case J:
                    this.changeSliceJ(delta);
                    break;
                case K:
                    this.changeSliceK(delta);
                    break;
            }
        }
    }

    public void setSliceI(int index)
    {
        this.renderSlice.setSliceI(index);
    }

    public void setSliceJ(int index)
    {
        this.renderSlice.setSliceJ(index);
    }

    public void setSliceK(int index)
    {
        this.renderSlice.setSliceK(index);
    }

    public void setSlice(int index, VolumeSlicePlane plane)
    {
        switch (plane)
        {
            case I:
                this.setSliceI(index);
                break;
            case J:
                this.setSliceJ(index);
                break;
            case K:
                this.setSliceK(index);
                break;
        }
    }

    public List<String> modes()
    {
        return this.renderSlice.modes();
    }

    public synchronized void handle(WorldMouse mouse, String mode)
    {
        this.renderSlice.handle(mouse, mode);
    }
}