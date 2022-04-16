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

import com.jogamp.opengl.GL2;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import qit.base.Global;
import qit.base.Logging;
import qit.base.structs.Named;
import qit.data.datasets.Mask;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qitview.panels.Viewables;
import qitview.main.Viewer;
import qitview.models.Slicer;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.models.WorldMouse;
import qitview.views.MaskView;
import qitview.views.SliceableView;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ControlPanel;

public class RenderVolumeGlyph<E extends SliceableView<?>>
{
    private static final int MAX_OFFSET = 999;

    private E volume;
    private ControlPanel panel = null;

    private static class SliceList
    {
        boolean update = false;
        Integer index = null;
    }

    private SliceList listI = new SliceList();
    private SliceList listJ = new SliceList();
    private SliceList listK = new SliceList();
    private SliceList listR = new SliceList();

    private transient MaskView maskSlice;
    private transient MaskView maskRegion;

    private Vect offset = VectSource.create3D();
    private Observer updateObserver = (o, arg) -> RenderVolumeGlyph.this.updateAll();

    public RenderVolumeGlyph(E parent)
    {
        parent.observable.addObserver(this.updateObserver);
        this.volume = parent;
        this.initPanel();
        this.updateAll();
    }

    public void dispose(GL2 gl)
    {
        for (SliceList list : new SliceList[]{this.listI, this.listJ, this.listK, this.listR})
        {
            if (list.index != null)
            {
                gl.glDeleteLists(list.index, 1);
                list.index = null;
                list.update = true;
            }
        }
    }

    public ControlPanel getPanel()
    {
        if (this.panel == null)
        {
            this.initPanel();
        }

        return this.panel;
    }

    private void initPanel()
    {
        this.panel = new ControlPanel();

        {
            final JCheckBox showI = new JCheckBox();
            final JCheckBox showJ = new JCheckBox();
            final JCheckBox showK = new JCheckBox();

            this.volume.observable.addObserver((o, arg) ->
            {
                if (RenderVolumeGlyph.this.hasData())
                {
                    Slicer slicer = RenderVolumeGlyph.this.volume.getSlicer();
                    showI.setModel(slicer.modelButtonI());
                    showJ.setModel(slicer.modelButtonJ());
                    showK.setModel(slicer.modelButtonK());

                    for (JCheckBox b : new JCheckBox[]{showI, showJ, showK})
                    {
                        b.addChangeListener(slicer);
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
                if (RenderVolumeGlyph.this.hasData())
                {
                    Slicer slicer = RenderVolumeGlyph.this.volume.getSlicer();
                    spinI.setModel(slicer.modelSpinnerI());
                    spinJ.setModel(slicer.modelSpinnerJ());
                    spinK.setModel(slicer.modelSpinnerK());

                    for (BasicSpinner s : new BasicSpinner[]{spinI, spinJ, spinK})
                    {
                        s.addChangeListener(slicer);
                    }
                }
            });

            final BasicSpinner offI = new BasicSpinner(new SpinnerNumberModel(0, -MAX_OFFSET, MAX_OFFSET, 1));
            final BasicSpinner offJ = new BasicSpinner(new SpinnerNumberModel(0, -MAX_OFFSET, MAX_OFFSET, 1));
            final BasicSpinner offK = new BasicSpinner(new SpinnerNumberModel(0, -MAX_OFFSET, MAX_OFFSET, 1));

            BasicSpinner[] offs = new BasicSpinner[]{offI, offJ, offK};
            for (int idx = 0; idx < 3; idx++)
            {
                final BasicSpinner off = offs[idx];
                final int fidx = idx;
                off.addChangeListener(e ->
                {
                    float value = Float.valueOf(off.getModel().getValue().toString());
                    RenderVolumeGlyph.this.offset.set(fidx, value);
                    if (fidx == 0)
                    {
                        RenderVolumeGlyph.this.updateI();
                    }
                    else if (fidx == 1)
                    {
                        RenderVolumeGlyph.this.updateJ();
                    }
                    else if (fidx == 2)
                    {
                        RenderVolumeGlyph.this.updateK();
                    }
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

            showI.setToolTipText("show the I slice (spans the JK plane)");
            showJ.setToolTipText("show the J slice (spans the IK plane)");
            showK.setToolTipText("show the K slice (spans the IJ plane)");
            spinI.setToolTipText("specify the current slice to show");
            spinJ.setToolTipText("specify the current slice to show");
            spinK.setToolTipText("specify the current slice to show");
            offI.setToolTipText("specify an offset for the slice (useful for glyph rendering)");
            offJ.setToolTipText("specify an offset for the slice (useful for glyph rendering)");
            offK.setToolTipText("specify an offset for the slice (useful for glyph rendering)");
        }
        {
            final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Mask, true, true);
            elem.setPrototypeDisplayValue(Viewables.NONE);
            elem.setToolTipText("exclude some voxels from glyph rendering using a mask");
            elem.addActionListener(e ->
            {
                Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                MaskView a = (MaskView) entry.getValue();

                if (this.maskSlice != null)
                {
                    this.maskSlice.observable.deleteObserver(this.updateObserver);
                }

                // can be null
                this.maskSlice = a;

                if (this.maskSlice != null)
                {
                    this.maskSlice.observable.addObserver(this.updateObserver);
                }

                this.updateAll();
            });
            this.panel.addControl("Slice Mask", elem);
        }
        {
            final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Mask, true, true);
            elem.setPrototypeDisplayValue(Viewables.NONE);
            elem.setToolTipText("render glyphs for all voxels in the given mask (warning: only practical for small regions)");
            elem.addActionListener(e ->
            {
                Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                MaskView a = (MaskView) entry.getValue();

                // can be null
                RenderVolumeGlyph.this.maskRegion = a;
                this.listR.update = true;
            });
            this.panel.addControl("Region Mask", elem);
        }
    }

    private boolean hasData()
    {
        return this.volume.hasData();
    }

    private Volume getData()
    {
        Object out = this.volume.getData();
        Global.assume(out instanceof Volume, "expected a volume");
        return (Volume) out;
    }

    public void updateAll()
    {
        this.listI.update = true;
        this.listJ.update = true;
        this.listK.update = true;
        this.listR.update = true;
    }

    public void updateI()
    {
        this.listI.update = true;
    }

    public void updateJ()
    {
        this.listJ.update = true;
    }

    public void updateK()
    {
        this.listK.update = true;
    }

    public void display(GL2 gl, RenderGlyph glyph)
    {
        displayRegion(gl, glyph);
        displaySliceI(gl, glyph);
        displaySliceJ(gl, glyph);
        displaySliceK(gl, glyph);
    }

    public void displayRegion(GL2 gl, RenderGlyph glyph)
    {
        if (this.volume == null)
        {
            return;
        }

        if (this.hasData() && !glyph.valid(((Volume) this.volume.getData()).getDim()))
        {
            return;
        }

        if (this.listR.update && this.listR.index != null)
        {
            gl.glDeleteLists(this.listR.index, 1);
            this.listR.index = null;
            this.listR.update = false;
        }

        Mask region = this.maskRegion != null && this.maskRegion.hasData() ? this.maskRegion.getData() : null;

        if (region != null)
        {
            if (this.listR.index == null && region != null)
            {
                Logging.info("started rendering glyphs in region mask");
                int idx = gl.glGenLists(1);
                if (idx != 0)
                {
                    this.listR.index = idx;

                    gl.glNewList(idx, GL2.GL_COMPILE);

                    gl.glDisable(GL2.GL_LIGHTING);
                    gl.glLineWidth(1);
                    gl.glColor3f(1f, 1f, 1f);

                    Volume data = this.getData();
                    Sampling sampling = data.getSampling();
                    Sampling msampling = region.getSampling();
                    for (Sample sample : sampling)
                    {
                        Vect world = sampling.world(sample);
                        if (region.foreground(msampling.nearest(world)))
                        {
                            Vect coord = world.plus(this.offset);
                            glyph.renderModel(gl, coord, data.get(sample));
                        }
                    }
                }

                gl.glEndList();
            }
        }

        if (this.listR.index != null)
        {
            gl.glCallList(this.listR.index);
        }
    }

    public void displaySliceI(GL2 gl, RenderGlyph glyph)
    {
        if (this.volume == null)
        {
            return;
        }

        if (this.hasData() && !glyph.valid(((Volume) this.volume.getData()).getDim()))
        {
            return;
        }

        if (this.listI.update && this.listI.index != null)
        {
            gl.glDeleteLists(this.listI.index, 1);
            this.listI.index = null;
            this.listI.update = false;
        }

        Slicer slicer = this.volume.getSlicer();
        Mask slice = this.maskSlice != null && this.maskSlice.hasData() ? this.maskSlice.getData() : null;

        if (slicer.showI() && this.listI.index == null)
        {
            int idx = gl.glGenLists(1);
            if (idx != 0)
            {
                Logging.info("started rendering glyphs in slice i");

                this.listI.index = idx;

                gl.glNewList(idx, GL2.GL_COMPILE);

                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth(1);
                gl.glColor3f(1f, 1f, 1f);

                Volume data = this.getData();
                Sampling sampling = data.getSampling();
                for (int a = 0; a < data.getSampling().numJ(); a++)
                {
                    for (int b = 0; b < data.getSampling().numK(); b++)
                    {
                        Sample s = new Sample(this.volume.getSlicer().idxI(), a, b);
                        Vect w = sampling.world(s);
                        if (slice == null || slice.foreground(slice.getSampling().nearest(w)))
                        {
                            Vect coord = w.plus(this.offset);
                            glyph.renderModel(gl, coord, data.get(s));
                        }
                    }
                }

                gl.glEndList();

                Logging.info("finished rendering glyphs in slice i");
            }
        }

        if (slicer.showI() && this.listI.index != null)
        {
            gl.glCallList(this.listI.index);
        }
    }

    public void displaySliceJ(GL2 gl, RenderGlyph glyph)
    {
        if (this.volume == null)
        {
            return;
        }

        if (this.hasData() && !glyph.valid(((Volume) this.volume.getData()).getDim()))
        {
            return;
        }

        if (this.listJ.update && this.listJ.index != null)
        {
            gl.glDeleteLists(this.listJ.index, 1);
            this.listJ.index = null;
            this.listJ.update = false;
        }

        Slicer slicer = this.volume.getSlicer();
        Mask slice = this.maskSlice != null && this.maskSlice.hasData() ? this.maskSlice.getData() : null;

        if (slicer.showJ() && this.listJ.index == null)
        {
            int idx = gl.glGenLists(1);
            if (idx != 0)
            {
                Logging.info("started rendering glyphs in slice j");

                this.listJ.index = idx;

                gl.glNewList(idx, GL2.GL_COMPILE);

                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth(1);
                gl.glColor3f(1f, 1f, 1f);

                Volume data = this.getData();
                Sampling sampling = data.getSampling();
                for (int a = 0; a < data.getSampling().numI(); a++)
                {
                    for (int b = 0; b < data.getSampling().numK(); b++)
                    {
                        Sample s = new Sample(a, this.volume.getSlicer().idxJ(), b);
                        Vect w = sampling.world(s);
                        if (slice == null || slice.foreground(slice.getSampling().nearest(w)))
                        {
                            Vect coord = w.plus(this.offset);
                            glyph.renderModel(gl, coord, data.get(s));
                        }
                    }
                }

                gl.glEndList();

                Logging.info("finished rendering glyphs in slice j");
            }
        }

        if (slicer.showJ() && this.listJ.index != null)
        {
            gl.glCallList(this.listJ.index);
        }
    }

    public void displaySliceK(GL2 gl, RenderGlyph glyph)
    {
        if (this.volume == null)
        {
            return;
        }

        if (this.hasData() && !glyph.valid(((Volume) this.volume.getData()).getDim()))
        {
            return;
        }

        if (this.listK.update && this.listK.index != null)
        {
            gl.glDeleteLists(this.listK.index, 1);
            this.listK.index = null;
            this.listK.update = false;
        }

        Slicer slicer = this.volume.getSlicer();
        Mask slice = this.maskSlice != null && this.maskSlice.hasData() ? this.maskSlice.getData() : null;

        if (slicer.showK() && this.listK.index == null)
        {
            int idx = gl.glGenLists(1);
            if (idx != 0)
            {
                Logging.info("started rendering glyphs in slice k");

                this.listK.index = idx;

                gl.glNewList(idx, GL2.GL_COMPILE);

                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth(1);
                gl.glColor3f(1f, 1f, 1f);

                Volume data = this.getData();
                Sampling sampling = data.getSampling();
                for (int a = 0; a < data.getSampling().numI(); a++)
                {
                    for (int b = 0; b < data.getSampling().numJ(); b++)
                    {
                        Sample s = new Sample(a, b, this.volume.getSlicer().idxK());
                        Vect w = sampling.world(s);
                        if (slice == null || slice.foreground(slice.getSampling().nearest(w)))
                        {
                            Vect coord = w.plus(this.offset);
                            glyph.renderModel(gl, coord, data.get(s));
                        }
                    }
                }

                gl.glEndList();

                Logging.info("finished rendering glyphs in slice k");
            }
        }

        if (slicer.showK() && this.listK.index != null)
        {
            gl.glCallList(this.listK.index);
        }
    }

    public void handle(WorldMouse mouse)
    {
        if (!this.hasData() || (mouse.press == null && mouse.current == null))
        {
            return;
        }

        Vect hit = mouse.current == null ? mouse.press.hit : mouse.current.hit;
        Volume data = this.getData();
        Sampling sampling = data.getSampling();
        Sample nearest = sampling.nearest(hit);

        if (sampling.contains(nearest))
        {
            Viewer.getInstance().control.setStatusMessage("sample: " + nearest + ", model: " + data.get(nearest).toString());
        }
    }
}