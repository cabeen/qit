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

import com.google.common.collect.Sets;
import com.jogamp.opengl.GL2;

import java.awt.event.ItemEvent;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.Observer;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SpinnerNumberModel;

import qit.base.Global;
import qit.base.Logging;
import qit.base.cli.CliUtils;
import qit.base.structs.Named;
import qit.base.structs.ObservableInstance;
import qit.base.utils.JavaUtils;
import qit.data.datasets.Mask;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Solids;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;
import qit.math.utils.colormaps.ColormapDiscrete;
import qit.math.utils.colormaps.ColormapScalar;
import qitview.panels.Viewables;
import qitview.main.Viewer;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.views.AbstractView;
import qitview.views.MaskView;
import qitview.views.SolidsView;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicFormattedTextField;
import qitview.widgets.BasicSpinner;
import qitview.widgets.ColormapState;
import qitview.widgets.ControlPanel;

public class RenderVolumeTexture<E extends AbstractView<?>>
{
    private static final int[] TEX_SIZES = {32, 64, 128, 256, 512, 1024, 2048, 4096};

    private Observer changeObserver = (o, arg) -> RenderVolumeTexture.this.update();
    private ControlPanel panel = new ControlPanel();

    private BasicComboBox<ColormapDiscrete> comboColorDiscrete;
    private BasicComboBox<ColormapScalar> comboColorScalar;

    private boolean update = false;

    private Integer textureIdentifier;
    private Integer textureSize;

    private E volume;
    private MaskView mask;

    private boolean visible = false;
    private boolean smooth = true;
    private int channel = 0;
    private double opacity = 1.0;
    private double scale = 1.0;
    private double low = 0.01;
    private double high = 10000.0;
    private VectFunction colormap = null;

    private SolidsView solidsClip;

    private ObservableInstance observable = new ObservableInstance();

    private String which = "";
    private transient Set<Integer> whichidx = Sets.newHashSet(CliUtils.parseWhich(this.which));

    public RenderVolumeTexture(E parent)
    {
        parent.observable.addObserver(this.changeObserver);

        this.volume = parent;

        this.initPanel();
        this.update();
    }

    public void setOpacity(double v)
    {
        this.opacity = v;
        this.observable.changed();
    }

    public ControlPanel getPanel()
    {
        return this.panel;
    }

    private Vect getValue(Sample sample, int dim)
    {
        Object out = RenderVolumeTexture.this.volume.getDataDirect();
        if (out instanceof Mask)
        {
            Mask cmask = (Mask) out;

            return VectSource.create1D(cmask.get(sample));
        }
        else
        {
            Global.assume(out instanceof Volume, "expected a volume");
            Volume cvolume = (Volume) out;
            return VectSource.create1D(cvolume.get(sample, dim));
        }
    }

    private Sampling getSampling()
    {
        // avoid recursive calls by getting the data directly
        Object out = RenderVolumeTexture.this.volume.getDataDirect();
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

    private Integer getChannel()
    {
        if (this.volume.hasData())
        {
            Object out = RenderVolumeTexture.this.volume.getDataDirect();
            if (out instanceof Mask)
            {
                return 1;
            }
            else
            {
                Global.assume(out instanceof Volume, "expected a volume");
                Volume cvolume = (Volume) out;
                return ((Volume) out).getDim();
            }
        }
        else
        {
            return 1;
        }
    }

    private void initPanel()
    {
        final boolean mask = (this.volume instanceof MaskView);

        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("render the volume");
            elem.setSelected(this.visible);
            elem.addItemListener(e ->
            {
                RenderVolumeTexture.this.visible = e.getStateChange() == ItemEvent.SELECTED;
                RenderVolumeTexture.this.update();
            });
            this.observable.addObserver((o, arg) -> elem.setSelected(RenderVolumeTexture.this.visible));
            this.panel.addControl("Visible", elem);
        }
        {
            final SpinnerNumberModel model = new SpinnerNumberModel(this.channel, 0, 1024, 1);
            final BasicSpinner elem = new BasicSpinner(model);
            elem.setToolTipText("specify which channel to use for rendering");
            elem.addChangeListener(e ->
            {
                int value = Integer.valueOf(elem.getModel().getValue().toString());
                RenderVolumeTexture.this.channel = value;
                RenderVolumeTexture.this.update();
            });
            this.panel.addControl("Channel", elem);
            this.volume.observable.addObserver((o, arg) ->
            {
                if (RenderVolumeTexture.this.volume.hasData())
                {
                    if (RenderVolumeTexture.this.volume.hasData())
                    {
                        int max = RenderVolumeTexture.this.getChannel() - 1;
                        RenderVolumeTexture.this.channel = Math.min(max, RenderVolumeTexture.this.channel);
                        model.setMaximum(max);
                    }
                }
            });
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField();
            elem.setValue(this.which);
            elem.setToolTipText("Specify which labels to exclusively render, e.g. 0,2:10");
            elem.addPropertyChangeListener("value", e ->
            {
                String nwhich = (String) elem.getValue();
                if (nwhich != RenderVolumeTexture.this.which)
                {
                    RenderVolumeTexture.this.which = nwhich;
                    RenderVolumeTexture.this.whichidx = Sets.newHashSet(CliUtils.parseWhich(nwhich));
                    RenderVolumeTexture.this.update();
                    Logging.info("updated which to " + RenderVolumeTexture.this.which);
                }
            });
            panel.addControl("Which Label", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.scale));
            elem.setToolTipText("specify a scaling factor (large improves resolution and requires more computation)");
            elem.addPropertyChangeListener("value", e ->
            {
                double nscale = ((Number) elem.getValue()).doubleValue();
                if (nscale != RenderVolumeTexture.this.scale)
                {
                    RenderVolumeTexture.this.scale = nscale;
                    RenderVolumeTexture.this.update();
                }
            });
            this.observable.addObserver((o, arg) -> elem.setValue(RenderVolumeTexture.this.scale));
            panel.addControl("Scale", elem);
        }
        {
            final JSlider elem = new JSlider(JSlider.HORIZONTAL, 0, 100, (int) Math.round(100 * this.opacity));
            elem.setToolTipText("specify the opacity of the rendering");
            elem.addChangeListener(e ->
            {
                RenderVolumeTexture.this.opacity = elem.getModel().getValue() / 100.;
                RenderVolumeTexture.this.update();
            });
            this.observable.addObserver((o, arg) -> elem.setValue((int) Math.round(100 * RenderVolumeTexture.this.opacity)));
            this.panel.addControl("Opacity", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.low));
            elem.setToolTipText("specify the low threshold for including voxels in the rendering");
            elem.addPropertyChangeListener("value", e ->
            {
                double nlow = ((Number) elem.getValue()).doubleValue();
                if (nlow != RenderVolumeTexture.this.low)
                {
                    RenderVolumeTexture.this.low = nlow;
                    RenderVolumeTexture.this.update();
                }
            });
            this.observable.addObserver((o, arg) -> elem.setValue(RenderVolumeTexture.this.low));
            panel.addControl("Low Thresh", elem);
        }
        {
            final BasicFormattedTextField elem = new BasicFormattedTextField(NumberFormat.getNumberInstance());
            elem.setValue(new Double(this.high));
            elem.setToolTipText("specify the high threshold for including voxels in the rendering");
            elem.addPropertyChangeListener("value", e ->
            {
                double nhigh = ((Number) elem.getValue()).doubleValue();
                if (nhigh != RenderVolumeTexture.this.high)
                {
                    RenderVolumeTexture.this.high = nhigh;
                    RenderVolumeTexture.this.update();
                }
            });
            this.observable.addObserver((o, arg) -> elem.setValue(RenderVolumeTexture.this.high));
            panel.addControl("High Thresh", elem);
        }
        {
            final JCheckBox elem = new JCheckBox();
            elem.setToolTipText("smooth the mesh rendering within each texture");
            elem.setSelected(this.smooth);
            elem.addItemListener(e ->
            {
                RenderVolumeTexture.this.smooth = e.getStateChange() == ItemEvent.SELECTED;
                RenderVolumeTexture.this.update();
            });
            this.observable.addObserver((o, arg) -> elem.setSelected(RenderVolumeTexture.this.smooth));

            this.panel.addControl("Smooth", elem);
        }
        {
            final BasicButton auto = new BasicButton("Auto Min/Max");
            auto.setToolTipText("detect the visible range");
            auto.addActionListener(arg0 -> RenderVolumeTexture.this.autoMinMax());

            final ColormapState cms = Viewer.getInstance().colormaps;
            this.comboColorDiscrete = cms.getComboDiscrete();
            this.comboColorDiscrete.setSelectedIndex(0);
            this.comboColorScalar = cms.getComboScalar();
            this.comboColorScalar.setSelectedIndex(0);

            if (mask)
            {
                Viewer.getInstance().colormaps.getObservable().addObserver((o, arg) ->
                {
                    RenderVolumeTexture.this.colormap = ((ColormapDiscrete) RenderVolumeTexture.this.comboColorDiscrete.getSelectedItem()).getFunction();
                    RenderVolumeTexture.this.update();
                });

                this.comboColorDiscrete.addActionListener(e ->
                {
                    RenderVolumeTexture.this.colormap = ((ColormapDiscrete) RenderVolumeTexture.this.comboColorDiscrete.getSelectedItem()).getFunction();
                    RenderVolumeTexture.this.update();
                });

                this.colormap = ((ColormapDiscrete) this.comboColorDiscrete.getSelectedItem()).getFunction();

                final JPanel combos = new JPanel();
                combos.add(this.comboColorDiscrete);

                this.panel.addControl("Colormap", combos);
            }
            else
            {
                Viewer.getInstance().colormaps.getObservable().addObserver((o, arg) ->
                {
                    RenderVolumeTexture.this.colormap = ((ColormapScalar) RenderVolumeTexture.this.comboColorScalar.getSelectedItem()).getFunction();
                    RenderVolumeTexture.this.update();
                });

                this.comboColorScalar.addActionListener(e ->
                {
                    RenderVolumeTexture.this.colormap = ((ColormapScalar) RenderVolumeTexture.this.comboColorScalar.getSelectedItem()).getFunction();
                    RenderVolumeTexture.this.update();
                });

                this.comboColorScalar.setVisible(true);
                this.colormap = ((ColormapScalar) this.comboColorScalar.getSelectedItem()).getFunction();

                final JPanel combos = new JPanel();
                combos.add(this.comboColorScalar);
                this.panel.addControl("Colormap", combos);
                this.panel.addControl(auto);
            }
        }

        {
            final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Mask, true, true);
            elem.setPrototypeDisplayValue(Viewables.NONE);
            elem.setToolTipText("specify a mask for excluding voxels from the rendering");
            elem.addActionListener(e ->
            {
                Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                MaskView mask1 = (MaskView) entry.getValue();

                if (!entry.equals(Viewables.NONE) && !JavaUtils.equals(mask1, RenderVolumeTexture.this.mask))
                {
                    RenderVolumeTexture.this.mask = mask1;
                    RenderVolumeTexture.this.update();
                    Logging.info("using mask: " + entry.getName());
                }

                if (entry.equals(Viewables.NONE))
                {
                    RenderVolumeTexture.this.mask = null;
                    RenderVolumeTexture.this.update();
                    Logging.info("using no mask");
                }
            });
            this.panel.addControl("Mask", elem);
        }
        {
            final BasicComboBox<Named<Viewable<?>>> elem = Viewer.getInstance().data.getComboBox(ViewableType.Solids, true, true);
            elem.setToolTipText("specify a solids data object for excluding voxels from the rendering");
            elem.addActionListener(e ->
            {
                Named<Viewable<?>> entry = elem.getItemAt(elem.getSelectedIndex());
                SolidsView solids = (SolidsView) entry.getValue();

                if (!JavaUtils.equals(solids, RenderVolumeTexture.this.solidsClip))
                {
                    RenderVolumeTexture.this.setSolidsClip(solids);
                }
            });
            panel.addControl("Solids Clip", elem);
        }
    }

    private void autoMinMax()
    {
        if (RenderVolumeTexture.this.volume.hasData())
        {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            Volume vol = (Volume) this.volume.getData();

            if (this.channel < 0 || this.channel >= vol.getDim())
            {
                Logging.info("warning; invalid data channel");
                return;
            }

            for (Sample sample : vol.getSampling())
            {
                double value = vol.get(sample, this.channel);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            ColormapScalar cm = (ColormapScalar) this.comboColorScalar.getSelectedItem();
            cm.withMin(min);
            cm.withMax(max);
            this.colormap = cm.getFunction();
            this.update();
            Viewer.getInstance().colormaps.update();
        }
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

    private boolean hasData()
    {
        return this.volume.hasData();
    }

    public int color(Vect value)
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
        double alpha = max < this.low || max > this.high ? 0 : this.opacity * vcolor.get(3);

        int color = RenderUtils.pack(vcolor, alpha);

        return color;
    }

    private void updateTexture(GL2 gl)
    {
        Logging.info("started updating texture");

        Sampling sampling = this.getSampling();
        int numI = sampling.numI();
        int numJ = sampling.numJ();
        int numK = sampling.numK();

        // a hacky way to find the texture size
        Integer ntex = null;
        for (int s : TEX_SIZES)
        {
            if (s >= numI && s >= numJ && s >= numK)
            {
                ntex = s;
                break;
            }
        }

        Global.assume(ntex != null, "volume too large for texture loading!");
        this.textureSize = ntex;
        int vsize = this.textureSize * this.textureSize * this.textureSize;
        int bsize = vsize * 4;

        ByteBuffer buffer = ByteBuffer.allocate(bsize);

        gl.glEnable(GL2.GL_TEXTURE_3D);
        int[] sid = new int[1];
        gl.glGenTextures(1, sid, 0);

        this.textureIdentifier = sid[0];
        gl.glBindTexture(GL2.GL_TEXTURE_3D, this.textureIdentifier);

        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_WRAP_R, GL2.GL_CLAMP_TO_BORDER);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);

        // Convert the data to RGBA data.

        Mask mask = this.mask == null ? null : this.mask.getData();

        int ts = this.textureSize;
        int ts2 = ts * ts;

        for (Sample sample : sampling)
        {
            int idx = 4 * (ts2 * sample.getK() + ts * sample.getJ() + sample.getI());
            int color = 0;

            boolean passed = true;

            if (this.solidsClip != null && this.solidsClip.hasData())
            {
                Solids clip = this.solidsClip.getData();
                if (!clip.contains(sampling.world(sample)))
                {
                    passed = false;
                }
            }

            if (mask != null && mask.background(sample))
            {
                passed = false;
            }

            if (passed)
            {
                Vect value = this.getValue(sample, this.channel);
                color = this.color(value);
            }

            buffer.putInt(idx, color);
        }

        gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL2.GL_RGBA, numI, numJ, numK, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, buffer);
        gl.glBindTexture(GL2.GL_TEXTURE_3D, 0);

        Logging.info("finished updating texture");
    }

    public void display(GL2 gl)
    {
        if (this.visible && this.hasData())
        {
            if (this.update || this.textureIdentifier == null)
            {
                this.updateTexture(gl);
                this.update = false;
            }

            Sampling sampling = this.getSampling();

            double numI = sampling.numI();
            double numJ = sampling.numJ();
            double numK = sampling.numK();

            double lowI = -0.5;
            double lowJ = -0.5;
            double highI = numI - 0.5;
            double highJ = numJ - 0.5;

            Matrix invrot = Viewer.getInstance().gui.canvas.render3D.cameraRot.inv();

            // rotate the texture
            gl.glMatrixMode(GL2.GL_TEXTURE);
            gl.glLoadIdentity();

            //rotate around the center of the texture
            gl.glTranslated(0.5, 0.5, 0.5);

            // scaleCamera the texture to support volumes with different dimensions
            gl.glScaled(numI / numI, numI / numJ, numI / numK);

            // rotate the texture
            RenderUtils.glTransform(gl, invrot);

            // undo the centering
            gl.glTranslated(-0.5, -0.5, -0.5);

            gl.glEnable(GL2.GL_TEXTURE_3D);
            gl.glBindTexture(GL2.GL_TEXTURE_3D, this.textureIdentifier);

            int glinterp = this.smooth ? GL2.GL_LINEAR : GL2.GL_NEAREST;
            gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, glinterp);
            gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, glinterp);

            gl.glDisable(GL2.GL_CULL_FACE);
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL2.GL_ALPHA_TEST);
            gl.glAlphaFunc(GL2.GL_GREATER, 0.03f);

            Vect center = sampling.world(sampling.center());
            float scaleI = (float) numI / (float) this.textureSize;
            float scaleJ = (float) numJ / (float) this.textureSize;
            int samplesK = (int) Math.round(numK * this.scale);

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glPushMatrix();
            RenderUtils.glTransform(gl, center);
            RenderUtils.glTransform(gl, invrot);
            RenderUtils.glTransform(gl, center.times(-1));

            for (int k = 0; k < samplesK; k++)
            {
                float kidx = (k / (float) (samplesK - 1));
                float kvox = kidx * (float) (sampling.numK() - 1);

                Vect a = sampling.world(VectSource.create3D(lowI, lowJ, kvox));
                Vect b = sampling.world(VectSource.create3D(highI, lowJ, kvox));
                Vect c = sampling.world(VectSource.create3D(highI, highJ, kvox));
                Vect d = sampling.world(VectSource.create3D(lowI, highJ, kvox));

                gl.glBegin(GL2.GL_QUADS);

                gl.glTexCoord3f(0.0f, 0.0f, kidx);
                gl.glVertex3d(a.get(0), a.get(1), a.get(2));

                gl.glTexCoord3f(scaleI, 0.0f, kidx);
                gl.glVertex3d(b.get(0), b.get(1), b.get(2));

                gl.glTexCoord3f(scaleI, scaleJ, kidx);
                gl.glVertex3d(c.get(0), c.get(1), c.get(2));

                gl.glTexCoord3f(0.0f, scaleJ, kidx);
                gl.glVertex3d(d.get(0), d.get(1), d.get(2));

                gl.glEnd();
            }

            // preserve the matrix mode
            gl.glPopMatrix();
            gl.glMatrixMode(GL2.GL_TEXTURE);
            gl.glLoadIdentity();
            gl.glMatrixMode(GL2.GL_MODELVIEW);

            gl.glDisable(GL2.GL_TEXTURE_3D);
        }
    }

    public void update()
    {
        this.update = true;
    }
}