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

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jogamp.opengl.GL2;
import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.FixedCapacityStack;
import qit.base.structs.Integers;
import qit.base.structs.Named;
import qit.base.structs.ObservableInstance;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Vects;
import qit.data.source.VectSource;
import qit.data.utils.MaskUtils;
import qit.math.structs.Line;
import qit.math.structs.LineIntersection;
import qit.math.structs.Plane;
import qitview.main.Viewer;
import qitview.models.Viewable;
import qitview.models.ViewableAction;
import qitview.models.ViewableActions;
import qitview.models.ViewableType;
import qitview.models.VolumeSlicePlane;
import qitview.models.WorldMouse;
import qitview.panels.Viewables;
import qitview.render.RenderVolumeSlice;
import qitview.render.RenderVolumeTexture;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicLabel;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ControlPanel;
import qitview.widgets.LabelTable;
import qitview.widgets.SwingUtils;
import qitview.widgets.VerticalLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.SpinnerNumberModel;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MaskView extends SliceableView<Mask>
{
    public enum Interaction
    {
        Draw, Erase, None
    }

    private static final String MODE_SQUARE_STENCIL = "Square Stencil";
    private static final String MODE_CIRCLE_STENCIL = "Circle Stencil";
    private static final String MODE_FREEHAND = "Freehand";

    public static final String SLICE = "Slice Rendering";
    public static final String TEXTURE = "Volume Rendering";
    private static final String LABELS = "Labels";
    private static final String DRAWING = "Drawing";
    private static final String PROCESSING = "Processing";

    public static final int MAX_INT = 10000;
    public static final int HISTORY_SIZE = 10;

    private enum Slice
    {
        I, J, K
    }

    private transient RenderVolumeSlice<MaskView> renderSlice;
    private transient RenderVolumeTexture<MaskView> renderTexture;
    private transient LabelTable labelTable = new LabelTable();
    private transient ObservableInstance observe = new ObservableInstance();

    private transient List<Vects> loops = Lists.newArrayList();
    private transient Vects polygon = new Vects();
    private transient Slice slice = null;
    private transient boolean erase = false;

    private transient FixedCapacityStack<Map<Sample, Integer>> undos = new FixedCapacityStack<>(HISTORY_SIZE);
    private transient FixedCapacityStack<Map<Sample, Integer>> redos = new FixedCapacityStack<>(HISTORY_SIZE);

    private transient Map<Sample, Integer> stencilCache = Maps.newHashMap();

    private int stencilLabel = 1;
    private int stencilSize = 2;
    private String drawingMode = MODE_CIRCLE_STENCIL;

    public MaskView()
    {
        super();
        this.renderSlice = new RenderVolumeSlice<>(this);
        this.renderSlice.setOpacity(0.7f);
        this.renderSlice.setNoBG(true);

        this.renderTexture = new RenderVolumeTexture<>(this);

        super.initPanel();
    }

    public MaskView setData(Mask d)
    {
        if (d != null)
        {
            this.bounds = d.getSampling().bounds();
            this.slicer = Viewer.getInstance().control.getSlicer(d.getSampling());
            Viewer.getInstance().gui.canvas.render3D.boxUpdate();
        }

        super.setData(d);

        this.renderSlice.updateBufferAll();
        this.copyLabelDataToView();

        return this;
    }

    public Mask getData()
    {
        this.renderSlice.pushBack();
        return this.data;
    }

    protected ControlPanel makeInfoControls()
    {
        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Type: ", new BasicLabel(this.toString()));
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
        final BasicLabel values = new BasicLabel("");
        final BasicLabel labels = new BasicLabel("");

        Runnable updateInfo = () ->
        {
            if (MaskView.this.hasData())
            {
                Sampling sampling = MaskView.this.data.getSampling();

                DecimalFormat df = new DecimalFormat("#0.000");
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
                values.setText(String.valueOf(MaskUtils.count(MaskView.this.data)));
                labels.setText(String.valueOf(MaskUtils.listNonzero(MaskView.this.data).size()));
            }
        };

        this.observable.addObserver((a, b) -> updateInfo.run());

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
        infoPanel.addControl("Values: ", values);
        infoPanel.addControl("Labels: ", labels);

        BasicButton elem = new BasicButton("Update Info");
        elem.setToolTipText("Update the info to reflect the most current data");
        elem.addActionListener(a -> updateInfo.run());

        infoPanel.addControl(elem);

        return infoPanel;
    }

    private void copyLabelDataToView()
    {
        if (this.hasData())
        {
            Mask mask = this.getData();
            MaskView.this.labelTable.clear();
            for (Integer label : MaskUtils.listNonzero(mask))
            {
                MaskView.this.labelTable.with(label, mask.getName(label));
            }
            MaskView.this.labelTable.changed();
        }
    }

    private void copyLabelViewToData()
    {
        if (this.hasData())
        {
            Mask mask = this.getData();
            for (LabelTable.TableRow row : MaskView.this.labelTable.getRows())
            {
                mask.setName(row.label, row.name);
            }
            this.observe.changed();
        }
    }

    public void update()
    {
        this.copyLabelDataToView();
        this.observe.changed();
    }

    public Observable getObservable()
    {
        return this.observe;
    }

    protected Map<String, ControlPanel> makeEditControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();

        final BasicSpinner drawLabel = new BasicSpinner(new SpinnerNumberModel(this.stencilLabel, 0, MAX_INT, 1));

        {
            this.labelTable.getObservable().addObserver((o, arg) -> MaskView.this.copyLabelViewToData());
            this.labelTable.addSelectActionListener(a -> this.labelTable.getSelectedRow().ifPresent(row -> drawLabel.setValue(row.label)));

            ControlPanel subpanel = new ControlPanel();
            subpanel.add(this.labelTable.getPanel());

            controls.put(LABELS, subpanel);
        }
        {
            ControlPanel subpanel = new ControlPanel();
            {
                drawLabel.setToolTipText("specify the label used when drawing on the mask");
                drawLabel.addChangeListener(e ->
                {
                    int value = Integer.valueOf(drawLabel.getModel().getValue().toString());
                    MaskView.this.stencilLabel = value;
                    Logging.info("changed drawing stencilLabel to: " + MaskView.this.stencilLabel);
                });
                subpanel.addControl("Draw Label", drawLabel);
            }
            {
                ControlPanel glyphPanel = new ControlPanel();
                glyphPanel.setLayout(new BoxLayout(glyphPanel, BoxLayout.Y_AXIS));

                final BasicComboBox<String> elem = new BasicComboBox<>();
                elem.addItem(MODE_CIRCLE_STENCIL);
                elem.addItem(MODE_SQUARE_STENCIL);
                elem.addItem(MODE_FREEHAND);

                elem.addActionListener(e ->
                {
                    MaskView.this.drawingMode = (String) elem.getSelectedItem();
                    Logging.info("updated drawing mode: " + MaskView.this.drawingMode);
                });
                elem.setToolTipText("specify the type of drawing element");
                subpanel.addControl("Drawing Mode", elem);
            }
            {
                final BasicSpinner elem = new BasicSpinner(new SpinnerNumberModel(this.stencilSize, 1, MAX_INT, 1));
                elem.setToolTipText("change the size of the drawing element (only makes sense for square and circular types)");
                elem.addChangeListener(e ->
                {
                    int value = Integer.valueOf(elem.getModel().getValue().toString());
                    MaskView.this.stencilSize = value;
                    Logging.info("changed drawing stencil stencilSize to: " + MaskView.this.stencilSize);
                });
                subpanel.addControl("Stencil Size", elem);
            }
            {
                final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Solids, true, true);
                elem.setToolTipText("use an existing solids data object to stamp the contained voxels with the current drawing label (useful for 3D region drawing, cannot be undone)");
                elem.addActionListener(e ->
                {
                    Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                    SolidsView solidsView = (SolidsView) entry.getValue();

                    if (solidsView != null && solidsView.hasData() && MaskView.this.hasData())
                    {
                        Logging.info("Setting solids stamp: " + entry.getName());
                        MaskUtils.set(MaskView.this.getData(), solidsView.getData(), MaskView.this.stencilLabel);
                        MaskView.this.updateAll();
                        elem.setSelectedItem(Viewables.NONE);
                    }
                });
                subpanel.addControl("Stamp Solids", elem);
            }
            {
                final BasicButton elem = new BasicButton("Clear Mask");
                elem.setToolTipText("clear the mask to zeros (warning: causes data loss)");
                elem.addActionListener(arg0 ->
                {
                    if (MaskView.this.hasData() && SwingUtils.getDecision("Are you sure you want to clear the mask? (This cannot be undone)"))
                    {
                        MaskView.this.getData().setAll(0);
                        MaskView.this.updateAll();
                        Viewer.getInstance().control.setStatusMessage("cleared mask");
                    }
                });
                subpanel.addControl(elem);
            }
            {
                final BasicButton elem = new BasicButton("Copy From Previous Slice");
                elem.setToolTipText("copy the mask labels from the elemious slice");
                elem.addActionListener(arg0 -> MaskView.this.copyFromOffset(-1));
                subpanel.addControl(elem);
            }
            {
                final BasicButton elem = new BasicButton("Copy From Next Slice");
                elem.setToolTipText("copy the mask labels form the elem slice");
                elem.addActionListener(arg0 -> MaskView.this.copyFromOffset(1));
                subpanel.addControl(elem);
            }
            {
                final BasicButton elem = new BasicButton("Undo Drawing");
                elem.setToolTipText("Undo the last change (go backward in drawing history)");
                elem.addActionListener(arg0 -> MaskView.this.undo());
                subpanel.addControl(elem);
            }
            {
                final BasicButton elem = new BasicButton("Redo Drawing");
                elem.setToolTipText("Redo the the last change (go forward in drawing history)");
                elem.addActionListener(arg0 -> MaskView.this.redo());
                subpanel.addControl(elem);
            }

            controls.put(DRAWING, subpanel);
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

            Consumer<ViewableAction> addAction = action ->
            {
                final BasicButton elem = new BasicButton(action.getName());
                elem.setToolTipText(action.getDescription());
                elem.addActionListener(e -> action.getAction(replace.get()).accept(this));
                panel.addControl(elem);
            };

            for (ViewableAction action : ViewableActions.Mask)
            {
                addAction.accept(action);
            }

            for (ViewableAction action : this.renderSlice.getLocalActions())
            {
                addAction.accept(action);
            }

            controls.put(PROCESSING, panel);
        }

        return controls;
    }

    protected Map<String, ControlPanel> makeRenderControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();
        controls.put(SLICE, this.renderSlice.getPanel());

        if (Global.getExpert())
        {
            // @TODO: fix bugs in this related to volume voxel counts
            controls.put(TEXTURE, this.renderTexture.getPanel());
        }

        return controls;
    }

    public void dispose(GL2 gl)
    {
        this.renderSlice.dispose(gl);
    }

    public void updateAll()
    {
        this.renderSlice.updateBufferAll();
        this.renderTexture.update();
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
        this.clearHistory();
        this.renderSlice.updateBufferI();
    }

    public void updateJ()
    {
        this.clearHistory();
        this.renderSlice.updateBufferJ();
    }

    public void updateK()
    {
        this.clearHistory();
        this.renderSlice.updateBufferK();
    }

    public void clearHistory()
    {
        this.undos.clear();
        this.redos.clear();
        this.stencilCache.clear();
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

        this.renderLoops(gl);
    }

    public void display(GL2 gl)
    {
        this.renderSlice.display(gl);
        this.renderTexture.display(gl);
        this.renderLoops(gl);
    }

    private void renderLoops(GL2 gl)
    {
        if (this.loops.size() > 0)
        {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glLineWidth(1);

            Vect color = this.renderSlice.color(VectSource.create1D(this.erase ? 0 : this.stencilLabel));
            gl.glColor3d(color.getX(), color.getY(), color.getZ());

            for (Vects loop : this.loops)
            {
                for (int i = 0; i < loop.size(); i++)
                {
                    int pi = i == 0 ? loop.size() - 1 : i - 1;
                    Vect prev = loop.get(pi);
                    Vect curr = loop.get(i);

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(prev.getX(), prev.getY(), prev.getZ());
                    gl.glVertex3d(curr.getX(), curr.getY(), curr.getZ());
                    gl.glEnd();
                }
            }
        }
    }

    private void setStencil()
    {
        if (this.stencilCache.size() != 0)
        {
            Map<Sample, Integer> undo = Maps.newHashMap();
            undo.putAll(this.stencilCache);

            this.undos.push(undo);
            this.stencilCache.clear();
        }
    }

    private void setFreehand()
    {
        if (this.polygon.size() == 0 || this.slice == null)
        {
            return;
        }

        long start = System.currentTimeMillis();
        Sampling sampling = this.data.getSampling();

        Sample init = sampling.nearest(this.polygon.get(0));
        int minI = init.getI();
        int maxI = init.getI();
        int minJ = init.getJ();
        int maxJ = init.getJ();
        int minK = init.getK();
        int maxK = init.getK();

        for (Vect point : this.polygon)
        {
            Sample sample = sampling.nearest(point);

            minI = Math.min(minI, sample.getI());
            maxI = Math.max(maxI, sample.getI());
            minJ = Math.min(minJ, sample.getJ());
            maxJ = Math.max(maxJ, sample.getJ());
            minK = Math.min(minK, sample.getK());
            maxK = Math.max(maxK, sample.getK());
        }

        Map<Sample, Integer> values = Maps.newHashMap();
        for (int si = minI; si <= maxI; si++)
        {
            for (int sj = minJ; sj <= maxJ; sj++)
            {
                for (int sk = minK; sk <= maxK; sk++)
                {
                    Sample sample = new Sample(si, sj, sk);
                    Vect pos = sampling.world(sample);

                    double tx = 0;
                    double ty = 0;

                    if (this.slice.equals(Slice.I))
                    {
                        tx = pos.getY();
                        ty = pos.getZ();
                    }
                    else if (this.slice.equals(Slice.J))
                    {
                        tx = pos.getX();
                        ty = pos.getZ();
                    }
                    else if (this.slice.equals(Slice.K))
                    {
                        tx = pos.getX();
                        ty = pos.getY();
                    }

                    boolean inside = false;
                    for (int i = 0, j = this.polygon.size() - 1; i < this.polygon.size(); j = i++)
                    {
                        double vix = 0;
                        double viy = 0;
                        double vjx = 0;
                        double vjy = 0;

                        if (this.slice.equals(Slice.I))
                        {
                            vix = this.polygon.get(i).getY();
                            viy = this.polygon.get(i).getZ();
                            vjx = this.polygon.get(j).getY();
                            vjy = this.polygon.get(j).getZ();
                        }
                        else if (this.slice.equals(Slice.J))
                        {
                            vix = this.polygon.get(i).getX();
                            viy = this.polygon.get(i).getZ();
                            vjx = this.polygon.get(j).getX();
                            vjy = this.polygon.get(j).getZ();
                        }
                        else if (this.slice.equals(Slice.K))
                        {
                            vix = this.polygon.get(i).getX();
                            viy = this.polygon.get(i).getY();
                            vjx = this.polygon.get(j).getX();
                            vjy = this.polygon.get(j).getY();
                        }

                        // yes, this is neat!
                        if (((viy > ty) != (vjy > ty)) && (tx < (vjx - vix) * (ty - viy) / (vjy - viy) + vix))
                        {
                            inside = !inside;
                        }
                    }

                    if (inside)
                    {
                        values.put(sample, this.erase ? 0 : this.stencilLabel);
                    }
                }
            }
        }

        int dur = (int) (System.currentTimeMillis() - start);
        Logging.info(String.format("computed freehand voxels from %d polygon points in %d ms", this.polygon.size(), dur));

        this.setValuesWithHistory(values);

        this.polygon.clear();
        this.loops.clear();
    }

    private void setValuesWithHistory(Map<Sample, Integer> values)
    {
        if (!values.isEmpty())
        {
            // drawing can be very slow!
            // to make it fast, we draw all samples from a given stencil and only update the visible slices
            // this requires an intermediate data structure that must be synchronized with the full data when needed
            this.redos.clear();
            Map<Sample, Integer> cache = this.renderSlice.setValues(values);
            this.undos.push(cache);
        }
    }

    private void undo()
    {
        if (!this.undos.isEmpty())
        {
            Map<Sample, Integer> undo = this.undos.pop();
            Map<Sample, Integer> redo = this.renderSlice.setValues(undo);
            this.redos.push(redo);
        }
        else
        {
            Logging.info("no history of drawing for undo");
        }
    }

    private void redo()
    {
        if (!this.redos.isEmpty())
        {
            Map<Sample, Integer> redo = this.redos.pop();
            Map<Sample, Integer> undo = this.renderSlice.setValues(redo);
            this.undos.push(undo);
        }
        else
        {
            Logging.info("no history of drawing for redo");
        }
    }

    private void handleFreehand(WorldMouse mouse, VolumeSlicePlane plane)
    {
        if (mouse.press == null)
        {
            return;
        }

        final Vect hit = mouse.current == null ? mouse.press.hit : mouse.current.hit;
        final Sampling sampling = this.data.getSampling();
        final Sample nearest = sampling.nearest(hit);

        if (!sampling.contains(nearest))
        {
            return;
        }

        Vect point = sampling.world(this.slicer.sample());
        Vect normal = null;
        Vect origin = sampling.world(0, 0, 0);
        Slice slice = null;

        boolean useI = (plane == null && this.slicer.showI()) || (plane != null && plane.equals(VolumeSlicePlane.I));
        boolean useJ = (plane == null && this.slicer.showJ()) || (plane != null && plane.equals(VolumeSlicePlane.J));
        boolean useK = (plane == null && this.slicer.showK()) || (plane != null && plane.equals(VolumeSlicePlane.K));

        if (useI && this.slicer.idxI() == nearest.getI())
        {
            normal = sampling.world(1, 0, 0).minus(origin).normalize().times(sampling.deltaI());
            slice = Slice.I;
        }

        if (useJ && this.slicer.idxJ() == nearest.getJ())
        {
            normal = sampling.world(0, 1, 0).minus(origin).normalize().times(sampling.deltaJ());
            slice = Slice.J;
        }

        if (useK && this.slicer.idxK() == nearest.getK())
        {
            normal = sampling.world(0, 0, 1).minus(origin).normalize().times(sampling.deltaK());
            slice = Slice.K;
        }

        if (normal != null)
        {
            Line line = Line.fromTwoPoints(mouse.current.point, mouse.current.hit);
            List<LineIntersection> inters = Plane.fromPointNormal(point, normal).intersect(line);

            if (inters.size() > 0)
            {
                Vect pos = inters.get(0).getPoint();

                if (this.loops.size() != 1)
                {
                    this.loops.clear();
                    this.loops.add(new Vects());
                }

                if (this.slice != slice)
                {
                    this.polygon.clear();
                    this.loops.clear();
                    this.loops.add(new Vects());
                }

                double lift = 0.5;
                Vect a = pos.plus(lift, normal);
                Vect b = pos.plus(-lift, normal);
                double da = line.getDir().dot(a.minus(line.getPoint()));
                double db = line.getDir().dot(b.minus(line.getPoint()));

                this.loops.get(0).add(da < db ? a : b);
                this.polygon.add(pos);
                this.slice = slice;
            }
        }
    }

    private void handleStencil(WorldMouse mouse, VolumeSlicePlane plane)
    {
        final Vect hit = mouse.current == null ? mouse.press.hit : mouse.current.hit;
        final Sampling sampling = this.data.getSampling();
        final Sample nearest = sampling.nearest(hit);
        final Vect vnearest = VectSource.create(nearest.getI(), nearest.getJ(), nearest.getK());

        if (!sampling.contains(nearest))
        {
            return;
        }

        final Map<Sample, Integer> values = Maps.newHashMap();

        if (this.stencilSize > 0)
        {
            int range = this.stencilSize - 1;
            final double radMax = range * range;

            this.loops.clear();
            Function<Integers, Boolean> func = integers ->
            {
                Sample nsample = nearest.offset(integers);

                if (!sampling.contains(nsample) || !MaskView.this.slicer.contains(nsample))
                {
                    return false;
                }

                double rad = VectSource.create(integers).norm2();
                if (MaskView.this.drawingMode.equals(MODE_CIRCLE_STENCIL) && rad > radMax)
                {
                    return false;
                }

                if (!this.stencilCache.containsKey(nsample))
                {
                    values.put(nsample, this.erase ? 0 : this.stencilLabel);
                }

                return true;
            };

            double lift = 0.1;

            boolean useI = (plane == null && this.slicer.showI()) || (plane != null && plane.equals(VolumeSlicePlane.I));
            boolean useJ = (plane == null && this.slicer.showJ()) || (plane != null && plane.equals(VolumeSlicePlane.J));
            boolean useK = (plane == null && this.slicer.showK()) || (plane != null && plane.equals(VolumeSlicePlane.K));

            for (int a = -range; a <= range; a++)
            {
                for (int b = -range; b <= range; b++)
                {
                    if (useI && this.slicer.idxI() == nearest.getI())
                    {
                        if (func.apply(new Integers(0, a, b)))
                        {
                            Vects vects = new Vects();
                            for (double d : new double[]{lift, -lift})
                            {
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D(d, (a + 0.5), (b + 0.5)))));
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D(d, (a - 0.5), (b + 0.5)))));
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D(d, (a - 0.5), (b - 0.5)))));
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D(d, (a + 0.5), (b - 0.5)))));
                            }
                            this.loops.add(vects);
                        }
                    }

                    if (useJ && this.slicer.idxJ() == nearest.getJ())
                    {
                        if (func.apply(new Integers(a, 0, b)))
                        {
                            Vects vects = new Vects();

                            for (double d : new double[]{lift, -lift})
                            {
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D((a + 0.5), d, (b + 0.5)))));
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D((a - 0.5), d, (b + 0.5)))));
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D((a - 0.5), d, (b - 0.5)))));
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D((a + 0.5), d, (b - 0.5)))));
                            }
                            this.loops.add(vects);
                        }
                    }

                    if (useK && this.slicer.idxK() == nearest.getK())
                    {
                        if (func.apply(new Integers(a, b, 0)))
                        {
                            Vects vects = new Vects();

                            for (double d : new double[]{lift, -lift})
                            {
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D((a + 0.5), (b + 0.5), d))));
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D((a - 0.5), (b + 0.5), d))));
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D((a - 0.5), (b - 0.5), d))));
                                vects.add(sampling.world(vnearest.plus(VectSource.create3D((a + 0.5), (b - 0.5), d))));
                            }
                            this.loops.add(vects);
                        }
                    }
                }
            }
        }

        if (mouse.press != null && !values.isEmpty())
        {
            if (this.stencilCache.size() == 0)
            {
                this.redos.clear();
            }

            Map<Sample, Integer> cache = this.renderSlice.setValues(values);
            this.stencilCache.putAll(cache);
        }
    }

    public Double dist(WorldMouse mouse)
    {
        return this.renderSlice.dist(mouse);
    }

    public void changeSliceI(int delta)
    {
        this.clearHistory();
        this.renderSlice.changeSliceI(delta);
    }

    public void changeSliceJ(int delta)
    {
        this.clearHistory();
        this.renderSlice.changeSliceJ(delta);
    }

    public void changeSliceK(int delta)
    {
        this.clearHistory();
        this.renderSlice.changeSliceK(delta);
    }

    public void changeSlice(int delta)
    {
        this.clearHistory();
        this.renderSlice.changeSlice(delta);
    }

    public void changeSlice(int delta, VolumeSlicePlane plane)
    {
        this.clearHistory();
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

    public void copyFromOffset(int delta)
    {
        if (!this.hasData())
        {
            return;
        }

        Mask data = this.getData();
        Sampling sampling = data.getSampling();

        Vect look = Viewer.getInstance().gui.canvas.render3D.look;

        double dotI = Math.abs(look.getX());
        double dotJ = Math.abs(look.getY());
        double dotK = Math.abs(look.getZ());

        Sample sample = this.slicer.sample();

        if (dotI > dotJ && dotI > dotK)
        {
            int i = sample.getI();
            int ni = i + delta;
            if (sampling.containsI(ni))
            {
                Logging.info("copying i slice from index:" + ni);
                for (int j = 0; j < sampling.numJ(); j++)
                {
                    for (int k = 0; k < sampling.numK(); k++)
                    {
                        data.set(i, j, k, data.get(ni, j, k));
                    }
                }
            }

            this.updateI();
        }
        else if (dotJ > dotI && dotJ > dotK)
        {
            int j = sample.getJ();
            int nj = j + delta;
            if (sampling.containsJ(nj))
            {
                Logging.info("copying j slice from index:" + nj);
                for (int i = 0; i < sampling.numI(); i++)
                {
                    for (int k = 0; k < sampling.numK(); k++)
                    {
                        data.set(i, j, k, data.get(i, nj, k));
                    }
                }
            }

            this.updateJ();
        }
        else
        {
            int k = sample.getK();
            int nk = k + delta;
            if (sampling.containsK(nk))
            {
                Logging.info("copying k slice from index:" + nk);
                for (int i = 0; i < sampling.numI(); i++)
                {
                    for (int j = 0; j < sampling.numJ(); j++)
                    {
                        data.set(i, j, k, data.get(i, j, nk));
                    }
                }
            }

            this.updateK();
        }
    }

    private Interaction parse(WorldMouse mouse, String mode)
    {
        if (!this.hasData() || mouse.current == null || !mouse.pick)
        {
            return Interaction.None;
        }

        if (mouse.press == null && this.drawingMode.equals(MODE_FREEHAND))
        {
            return Interaction.None;
        }

        for (Interaction i : Interaction.values())
        {
            if (i.toString().equals(mode))
            {
                return i;
            }
        }

        if (mouse.control)
        {
            if (mouse.shift)
            {
                return Interaction.Erase;
            }
            else
            {
                return Interaction.Draw;
            }
        }
        else
        {
            return Interaction.None;
        }
    }

    private void handleInteraction(WorldMouse mouse, VolumeSlicePlane plane)
    {
        boolean free = this.drawingMode.equals(MODE_FREEHAND);

        String a = this.erase ? "erasing" : "drawing";
        String b = free ? "freehand" : "stencil";
        if (mouse.press == null)
        {
            Viewer.getInstance().gui.setStatusMessage(String.format("clicking will start %s in %s mode", a, b));
        }
        else
        {
            Viewer.getInstance().gui.setStatusMessage(String.format("%s in %s mode", a, b));
        }

        if (free)
        {
            this.handleFreehand(mouse, plane);
        }
        else
        {
            this.handleStencil(mouse, plane);
        }
    }

    public List<String> modes()
    {
        List<String> out = this.renderSlice.modes();
        out.add(Interaction.Draw.toString());
        out.add(Interaction.Erase.toString());
        return out;
    }

    public void handleErase(WorldMouse mouse, VolumeSlicePlane plane)
    {
        this.erase = true;
        handleInteraction(mouse, plane);
    }

    public void handleDraw(WorldMouse mouse, VolumeSlicePlane plane)
    {
        this.erase = false;
        handleInteraction(mouse, plane);
    }

    public void handleNone(WorldMouse mouse, String mode, VolumeSlicePlane plane)
    {
        if (this.drawingMode.equals(MODE_FREEHAND))
        {
            this.setFreehand();
        }
        else
        {
            this.setStencil();
        }

        this.loops.clear();
        this.polygon.clear();
        this.renderSlice.handle(mouse, mode);
    }

    public void handle(WorldMouse mouse, String mode)
    {
        switch (parse(mouse, mode))
        {
            case Erase:
                handleErase(mouse, null);
                return;

            case Draw:
                handleDraw(mouse, null);
                return;

            case None:
                handleNone(mouse, mode, null);
                return;
        }
    }
}
