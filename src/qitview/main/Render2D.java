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

package qitview.main;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;
import qit.base.structs.Pair;
import qit.data.datasets.Matrix;
import qit.data.datasets.Sample;
import qit.data.datasets.Sampling;
import qit.data.datasets.Vect;
import qit.data.datasets.Volume;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.math.utils.MathUtils;
import qitview.models.ScreenMouse;
import qitview.models.ScreenPoint;
import qitview.models.Slicer;
import qitview.models.Viewable;
import qitview.models.VolumeSlicePlane;
import qitview.models.WorldMouse;
import qitview.render.RenderUtils;
import qitview.views.MaskView;
import qitview.views.VolumeView;
import qitview.widgets.MyMouseEvent;
import qitview.widgets.MyMouseListener;

import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.function.Consumer;

public class Render2D implements MyMouseListener
{
    public static final int POINT = 32;
    public static final Font FONT = new Font("Helvetica", Font.BOLD, POINT);
    public static final TextRenderer TEXTER = new TextRenderer(FONT, true, false);

    public transient ScreenMouse mouse = new ScreenMouse();

    public double scaleCamera = Constants.SCALE_DEFAULT;
    public Vect posCamera = VectSource.create3D(Constants.XPOS_DEFAULT, Constants.YPOS_DEFAULT, Constants.ZPOS_DEFAULT);

    public double xposMouse = 10 * Constants.XPOS_FACTOR;
    public double yposMouse = 10 * Constants.YPOS_FACTOR;
    public double scaleMouse = -1.5 * Constants.SCALE_FACTOR;
    public double scaleMin = 1.0;

    public VolumeSlicePlane plane = null;

    Render2D(VolumeSlicePlane plane)
    {
        this.plane = plane;
    }

    public void update()
    {
        if (this.hasVolume())
        {
            this.getVolume().update(this.plane);
        }

        if (this.hasOverlay())
        {
            this.getOverlay().update(this.plane);
        }

        if (this.hasMask())
        {
            this.getMask().update(this.plane);
        }
    }

    public void render(GLAutoDrawable drawable, int width, int height)
    {
        GL2 gl = drawable.getGL().getGL2();
        Settings state = Viewer.getInstance().settings;

        double aspect = width / (double) height;

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glEnable(GL2.GL_NORMALIZE);

        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        if (this.hasVolume())
        {
            Volume volume = this.getVolume().getData();
            Sampling sampling = volume.getSampling();

            double di = sampling.deltaI() * sampling.numI();
            double dj = sampling.deltaJ() * sampling.numJ();
            double dk = sampling.deltaK() * sampling.numK();

            Vect min = VectSource.create3D();
            Vect max = VectSource.create3D(di, dj, dk);

            double largest = max.minus(min).max();
            float scale = (float) (MathUtils.zero(largest) ? 1.9f : 1.9f / largest);
            Vect center = max.plus(min).times(-0.5);

            Matrix rotCamera = MatrixSource.identity(3);
            if (this.plane.equals(VolumeSlicePlane.J))
            {
                rotCamera = rotCamera.times(MatrixSource.rotation(Constants.X_AXIS, -Math.PI / 2.0));
            }
            else if (this.plane.equals(VolumeSlicePlane.I))
            {
                rotCamera = rotCamera.times(MatrixSource.rotation(Constants.Z_AXIS, -Math.PI / 2.0));
                rotCamera = rotCamera.times(MatrixSource.rotation(Constants.Y_AXIS, -Math.PI / 2.0));
            }

            Matrix modelview = MatrixSource.trans4(this.posCamera);
            modelview = modelview.times(rotCamera.hom());
            modelview = modelview.times(MatrixSource.scale4(this.scaleCamera));
            modelview = modelview.times(MatrixSource.scale4(scale));
            modelview = modelview.times(MatrixSource.trans4(center));
            modelview = modelview.times(sampling.quat().matrix().inv().hom());
            modelview = modelview.times(MatrixSource.trans4(sampling.start().times(-1.0)));

            Vect sample = this.getVolume().getSlicer().sample().vect();
            Vect plusCamera = modelview.times(sampling.world(sample.plus(state.referenceWindow)).hom()).dehom();
            Vect minusCamera = modelview.times(sampling.world(sample.plus(-state.referenceWindow)).hom()).dehom();

            // assume a z-axis camera, and automatically determine the right order
            double nearZ = Math.min(-plusCamera.getZ(), -minusCamera.getZ());
            double farZ = Math.max(-plusCamera.getZ(), -minusCamera.getZ());

            {
                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();
                if (aspect > 1.0)
                {
                    gl.glOrtho(-aspect, aspect, -1.0, 1.0, nearZ, farZ);
                }
                else
                {
                    double raspect = 1.0 / aspect;
                    gl.glOrtho(-1.0, 1.0, -raspect, raspect, nearZ, farZ);
                }

                gl.glMatrixMode(GL2.GL_MODELVIEW);
                gl.glLoadIdentity();
                gl.glPushMatrix();
                gl.glMultMatrixf(RenderUtils.buffer(modelview));

                if (state.showVolume2D)
                {
                    this.getVolume().display(gl, this.plane);
                }

                if (this.hasOverlay() && state.showOverlay2D)
                {
                    this.getOverlay().display(gl, this.plane);
                }

                if (state.showGeometry2D)
                {
                    for (Viewable<?> viewable : Viewer.getInstance().data.getVisible())
                    {
                        if (viewable.hasData())
                        {
                            boolean isMask = viewable instanceof MaskView;
                            boolean isVolume = viewable instanceof VolumeView;

                            if (!isMask && !isVolume)
                            {
                                viewable.display(gl);
                            }
                        }
                    }
                }

                // handle mouse events
                ScreenMouse screen = this.mouse;

                WorldMouse world = new WorldMouse();
                world.current = screen.current == null ? null : RenderUtils.screenToWorld(drawable, screen.current, width, height);
                world.press = screen.press == null ? null : RenderUtils.screenToWorld(drawable, screen.press, width, height);
                world.pick = screen.pick;
                world.control = screen.control;
                world.shift = screen.shift;
                world.time = screen.time;

                if (world.press != null && !world.pick && !world.shift && !world.control)
                {
                    this.getVolume().setSample(sampling.nearest(world.current.hit));
                }

                if (world.press != null && world.pick && world.shift && !world.control)
                {
                    Sample nearest = sampling.nearest(world.current.hit);

                    Viewer.getInstance().control.setQuery(sampling, sampling.world(nearest));
                    Viewer.getInstance().control.showQuery();
                }

                if (this.hasMask() && state.showMask2D)
                {
                    if (world.press != null && world.pick && !world.shift && world.control)
                    {
                        this.getMask().handleDraw(world, this.plane);
                    }
                    else if (world.press != null && world.pick && world.shift && world.control)
                    {
                        this.getMask().handleErase(world, this.plane);
                    }
                    else if (world.press == null && world.pick)
                    {
                        this.getMask().handle(world, null);
                    }
                }
            }

            if (this.hasMask() && state.showMask2D && this.getMask().getData().getSampling().compatible(sampling))
            {
                this.getMask().display(gl, this.plane);
            }

            // render reference objects
            Viewer.getInstance().gui.getQuery().render(gl, this.plane);

            if (state.showCross2D)
            {
                this.drawCross(gl);
            }

            gl.glPopMatrix();
        }

        {
            float red = VolumeSlicePlane.I.equals(this.plane) ? 1f : 0f;
            float green = VolumeSlicePlane.J.equals(this.plane) ? 1f : 0f;
            float blue = VolumeSlicePlane.K.equals(this.plane) ? 1f : 0f;

            {
                String text = "2D Slice " + this.plane.toString();

                if (Viewer.getInstance().gui.hasReferenceVolume())
                {
                    switch (this.plane)
                    {
                        case I:
                            text = text + " = " + Viewer.getInstance().gui.getReferenceVolume().getSlicer().idxI();
                            break;
                        case J:
                            text = text + " = " + Viewer.getInstance().gui.getReferenceVolume().getSlicer().idxJ();
                            break;
                        case K:
                            text = text + " = " + Viewer.getInstance().gui.getReferenceVolume().getSlicer().idxK();
                            break;

                    }
                }

                // warning: TextRenderer seems to have a memory leak
                // beware of creating a new object inside this loop
                int point = MathUtils.round(POINT / Viewer.getInstance().scaling);
                TEXTER.beginRendering(width, height);
                TEXTER.setColor(1.0f, 1.0f, 1.0f, 0.75f);
                TEXTER.draw(text, point, point);
                TEXTER.endRendering();
            }

            gl.glDisable(GL2.GL_LIGHTING);
            gl.glLineWidth(5);

            gl.glColor4f(red, green, blue, 0.5f);
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex2d(-0.65, -0.65);
            gl.glVertex2d(0.65, -0.65);
            gl.glEnd();

            //Right edge
            gl.glBegin(GL2.GL_LINES);
            gl.glVertex2d(100, 100);
            gl.glVertex2d(100, 200);
            gl.glEnd();

            //Left edge
            gl.glBegin(GL2.GL_LINE_STRIP);
            gl.glVertex2d(100, 100);
            gl.glVertex2d(100, 200);
            gl.glVertex2d(200, 200);
            gl.glVertex2d(200, 100);
            gl.glVertex2d(100, 100);
            gl.glEnd();
        }
    }

    private void drawCross(GL2 gl)
    {
        Slicer slicer = this.getVolume().getSlicer();
        Sampling sampling = this.getVolume().getData().getSampling();

        Sample nearest = slicer.sample();
        Vect scen = VectSource.create3D(nearest.getI(), nearest.getJ(), nearest.getK());

        Vect i0 = sampling.world(VectSource.create3D(0, nearest.getJ(), nearest.getK()));
        Vect i1 = sampling.world(VectSource.create3D(scen.getX() - 2, nearest.getJ(), nearest.getK()));
        Vect i2 = sampling.world(VectSource.create3D(scen.getX() + 2, nearest.getJ(), nearest.getK()));
        Vect i3 = sampling.world(VectSource.create3D(sampling.numI() - 1, nearest.getJ(), nearest.getK()));

        Vect j0 = sampling.world(VectSource.create3D(nearest.getI(), 0, nearest.getK()));
        Vect j1 = sampling.world(VectSource.create3D(nearest.getI(), scen.getY() - 2, nearest.getK()));
        Vect j2 = sampling.world(VectSource.create3D(nearest.getI(), scen.getY() + 2, nearest.getK()));
        Vect j3 = sampling.world(VectSource.create3D(nearest.getI(), sampling.numJ() - 1, nearest.getK()));

        Vect k0 = sampling.world(VectSource.create3D(nearest.getI(), nearest.getJ(), 0));
        Vect k1 = sampling.world(VectSource.create3D(nearest.getI(), nearest.getJ(), scen.getZ() - 2));
        Vect k2 = sampling.world(VectSource.create3D(nearest.getI(), nearest.getJ(), scen.getZ() + 2));
        Vect k3 = sampling.world(VectSource.create3D(nearest.getI(), nearest.getJ(), sampling.numK() - 1));

        gl.glEnable(GL2.GL_LIGHTING);

        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, RenderUtils.buffer(0.5f, 4));
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, RenderUtils.buffer(0.25f, 4));
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, RenderUtils.buffer(0f, 4));
        gl.glEnable(GL2.GL_LIGHT1);

        gl.glEnable(GL2.GL_POINT_SMOOTH);
        double delta = 2 * sampling.deltaMax();

        gl.glColor4d(1, 1, 1, 0.75);
        gl.glPointSize(3);

        Consumer<Pair<Vect, Vect>> drawLine = (p) ->
        {
            double len = p.a.dist(p.b);
            int num = MathUtils.round(len / delta);
            Vect ba = p.b.minus(p.a).normalize();

            gl.glBegin(GL2.GL_POINTS);
            for (int i = 0; i < num; i++)
            {
                Vect t = p.a.plus(len * i / (num - 1.0), ba);

                gl.glVertex3d(t.getX(), t.getY(), t.getZ());
            }
            gl.glEnd();
        };

        Runnable drawI = () ->
        {
            drawLine.accept(Pair.of(i0, i1));
            drawLine.accept(Pair.of(i2, i3));
        };

        Runnable drawJ = () ->
        {
            drawLine.accept(Pair.of(j0, j1));
            drawLine.accept(Pair.of(j2, j3));
        };

        Runnable drawK = () ->
        {
            drawLine.accept(Pair.of(k0, k1));
            drawLine.accept(Pair.of(k2, k3));
        };

        if (this.plane != null)
        {
            switch (this.plane)
            {
                case I:
                    drawJ.run();
                    drawK.run();
                    break;
                case J:
                    drawI.run();
                    drawK.run();
                    break;
                case K:
                    drawI.run();
                    drawJ.run();
                    break;
            }
        }
    }

    public boolean hasVolume()
    {
        return Viewer.getInstance().gui.hasReferenceVolume();
    }

    public boolean hasOverlay()
    {
        return Viewer.getInstance().gui.hasReferenceOverlay();
    }

    public boolean hasMask()
    {
        return Viewer.getInstance().gui.hasReferenceMask();
    }

    public VolumeView getVolume()
    {
        return Viewer.getInstance().gui.getReferenceVolume();
    }

    public VolumeView getOverlay()
    {
        return Viewer.getInstance().gui.getReferenceOverlay();
    }

    public MaskView getMask()
    {
        return Viewer.getInstance().gui.getReferenceMask();
    }

    private void mouseUpdate(MyMouseEvent e)
    {
        // this.touched();

        State state = Viewer.getInstance();

        ScreenMouse mouse = this.mouse;
        mouse.current = new ScreenPoint(e.xpos, e.ypos);

        mouse.control = (e.modifiers & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK;
        mouse.control |= (e.modifiers & MouseEvent.BUTTON2_DOWN_MASK) == MouseEvent.BUTTON2_DOWN_MASK;

        mouse.shift = (e.modifiers & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;
        mouse.shift |= (e.modifiers & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK;

        mouse.pick = (e.modifiers & MouseEvent.ALT_DOWN_MASK) == MouseEvent.ALT_DOWN_MASK;
        mouse.pick |= state.keys.containsKey(KeyEvent.VK_BACK_QUOTE);
        mouse.pick |= state.keys.containsKey(KeyEvent.VK_DEAD_TILDE);
        mouse.pick |= state.keys.containsKey(KeyEvent.VK_F1);

        mouse.time = System.currentTimeMillis();

        this.mouse.doubled = e.clicks > 1;
    }

    @Override
    public void mouseClicked(MyMouseEvent e)
    {

    }

    public synchronized void mousePressed(MyMouseEvent e)
    {
        mouseUpdate(e);
        ScreenMouse mouse = this.mouse;
        mouse.press = mouse.current;
    }

    public synchronized void mouseReleased(MyMouseEvent e)
    {
        mouseUpdate(e);
        this.mouse.press = null;
    }

    @Override
    public void mouseEntered(MyMouseEvent e)
    {

    }

    @Override
    public void mouseExited(MyMouseEvent e)
    {

    }

    public synchronized void mouseMoved(MyMouseEvent e)
    {
        mouseUpdate(e);
        ScreenMouse mouse = this.mouse;
        mouse.press = null;
    }

    public synchronized void mouseDragged(MyMouseEvent e)
    {
        // this.touched();

        if (this.hasVolume())
        {
            ScreenMouse mouse = this.mouse;

            Integer x = e.xpos;
            Integer y = e.ypos;

            if (mouse.current != null)
            {
                int dx = x - mouse.current.x;
                int dy = y - mouse.current.y;

                if (!mouse.pick)
                {
                    if ((e.hasLeft() && mouse.control && !mouse.shift) || e.hasRight())
                    {
                        this.scale(dy * this.scaleMouse);
                    }
                    else if ((e.hasLeft() && !mouse.control && mouse.shift) || e.hasMiddle())
                    {
                        this.moveX(dx * this.xposMouse);
                        this.moveY(dy * this.yposMouse);
                    }
                }
                else
                {
                    if (e.hasLeft() && !mouse.control && !mouse.shift)
                    {
                        this.changeSlice(dy);
                    }
                }
            }
        }

        mouseUpdate(e);
    }

    private void scale(double s)
    {
        double pscale = this.scaleCamera;
        this.scaleCamera += s;
        this.scaleCamera = Math.max(this.scaleMin, this.scaleCamera);

        double nscale = this.scaleCamera / pscale;
        this.posCamera.timesEquals(0, nscale);
        this.posCamera.timesEquals(1, nscale);
    }

    private void moveX(double s)
    {
        this.posCamera.plusEquals(0, s);
    }

    private void moveY(double s)
    {
        this.posCamera.plusEquals(1, s);
    }

    public void setGeometryVisible(boolean v)
    {
        Viewer.getInstance().settings.showGeometry2D = v;
    }

    public boolean getGeometryVisible()
    {
        return Viewer.getInstance().settings.showGeometry2D;
    }

    public void setVolumeVisible(boolean v)
    {
        Viewer.getInstance().settings.showVolume2D = v;
    }

    public boolean getVolumeVisible()
    {
        return Viewer.getInstance().settings.showVolume2D;
    }

    public void setMaskVisible(boolean v)
    {
        Viewer.getInstance().settings.showMask2D = v;
    }

    public boolean getMaskVisible()
    {
        return Viewer.getInstance().settings.showMask2D;
    }

    public void setCrossVisible(boolean v)
    {
        Viewer.getInstance().settings.showCross2D = v;
    }

    public boolean getCrossVisible()
    {
        return Viewer.getInstance().settings.showCross2D;
    }

    public void setOverlayVisible(boolean v)
    {
        Viewer.getInstance().settings.showOverlay2D = v;
    }

    public boolean getOverlayVisible()
    {
        return Viewer.getInstance().settings.showOverlay2D;
    }

    public void mouseWheelMoved(MouseWheelEvent e)
    {
        int notches = e.getWheelRotation();
        int delta = notches < 0 ? 1 : -1;
        this.changeSlice(delta);
    }

    public void changeSlice(int delta)
    {
        // this works because the slices of compatible volumes are linked
        if (this.hasVolume())
        {
            this.getVolume().changeSlice(delta, this.plane);
        }
        else if (this.hasOverlay())
        {
            this.getOverlay().changeSlice(delta, this.plane);
        }
        else if (this.hasMask())
        {
            this.getMask().changeSlice(delta, this.plane);
        }
    }

    public synchronized void resetView()
    {
        this.posCamera.set(0, Constants.XPOS_DEFAULT);
        this.posCamera.set(1, Constants.YPOS_DEFAULT);
        this.posCamera.set(2, Constants.ZPOS_DEFAULT);
        this.scaleCamera = Constants.SCALE_DEFAULT;
    }
}

