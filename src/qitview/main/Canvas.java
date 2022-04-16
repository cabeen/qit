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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.base.structs.Triple;
import qit.base.utils.PathUtils;
import qit.math.utils.MathUtils;
import qitview.models.RateReporter;
import qitview.models.ScreenMouse;
import qitview.models.Slicer;
import qitview.models.Viewable;
import qitview.models.ViewableType;
import qitview.models.VolumeSlicePlane;
import qitview.render.RenderAnnotation;
import qitview.widgets.ColormapState;
import qitview.widgets.MyMouseEvent;
import qitview.widgets.MyMouseListener;
import qitview.widgets.SwingUtils;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

public class Canvas extends GLCanvas implements GLEventListener, MouseListener, MouseMotionListener, MouseWheelListener
{
    enum Layout
    {
        View3D, SliceI, SliceJ, SliceK, SliceI3D, SliceJ3D, SliceK3D, OneByThree, TwoByTwo
    }

    public transient FPSAnimator animator;
    public transient ScreenMouse mouse = new ScreenMouse();
    public transient long touched = System.currentTimeMillis();

    public boolean showFPS = false;
    public RateReporter fps = new RateReporter();

    protected Layout layout = Layout.View3D;

    public Render3D render3D = new Render3D();
    public Render2D renderI = new Render2D(VolumeSlicePlane.I);
    public Render2D renderJ = new Render2D(VolumeSlicePlane.J);
    public Render2D renderK = new Render2D(VolumeSlicePlane.K);

    public transient State state = null;

    protected Canvas(State state)
    {
        super(new GLCapabilities(GLProfile.get(GLProfile.GL2)){{
            // we need this to reduce z-fighting
            this.setDepthBits(64);

            // we need this to support transparent screenshots
            this.setAlphaBits(8);

            // should we use this?
            this.setHardwareAccelerated(true);
        }});

        this.state = state;

        Logging.info("setting up colormaps");
        state.colormaps = new ColormapState();

        Logging.info("setting up annotations");
        state.annotation = new RenderAnnotation(state.colormaps.getComboScalar());

        Logging.info("setting up scaling");
        boolean win = state.osname.toLowerCase().contains("windows");
        boolean linux = state.osname.toLowerCase().contains("linux");
        boolean newjvm = !state.jvm.startsWith("1.") || state.jvm.startsWith("1.9");

        if (win && newjvm)
        {
            state.scaling = Toolkit.getDefaultToolkit().getScreenResolution() / 96.0;
            Logging.info("detected scaling: " + state.scaling);
        }

        String scale = System.getProperty("sun.java2d.uiScale");
        if (linux && scale != null)
        {
            state.scaling = Double.valueOf(scale);
            Logging.info("detected scaling: " + state.scaling);
        }

        Logging.info("setting up event listeners");
        this.addGLEventListener(this);
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
        this.addMouseWheelListener(this);

        SwingUtilities.invokeLater(() ->
        {
            this.animator = new FPSAnimator(this, Constants.FPS_FAST, true);
            this.animator.start();
        });
    }

    @Override
    public void init(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();

        Logging.info("... initing GL canvas");
        Logging.info("... GLCapabilities: " + drawable.getChosenGLCapabilities());
        Logging.info("... INIT GL: " + gl.getClass().getName());
        Logging.info("... GL_VENDOR: " + gl.glGetString(GL2.GL_VENDOR));
        Logging.info("... GL_RENDERER: " + gl.glGetString(GL2.GL_RENDERER));
        Logging.info("... GL_VERSION: " + gl.glGetString(GL2.GL_VERSION));

        Viewer.getInstance().started = true;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
    {
    }

    @Override
    public void dispose(GLAutoDrawable drawable)
    {
        Logging.info("... disposing GL canvas");
        GL2 gl = drawable.getGL().getGL2();

        for (Viewable viewable : Viewer.getInstance().data.getAll())
        {
            viewable.dispose(gl);
        }
    }

    @Override
    public void display(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();
        this.fps.touch();

        Runnable setbg = () ->
        {
            float backgroundRed = this.state.settings.bgRed;
            float backgroundGreen = this.state.settings.bgGreen;
            float backgroundBlue = this.state.settings.bgBlue;
            float backgroundAlpha = 1.0f;

            gl.glClearColor(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        };

        try
        {
            int width = (int) (this.state.scaling * this.getSurfaceWidth());
            int height = (int) (this.state.scaling * this.getSurfaceHeight());

            {
                setbg.run();

                int widthSplit = (int) (this.state.settings.halve * width);
                int widthHalf = width / 2;
                int heightHalf = height / 2;

                int widthSplitLow = (int) Math.round(0.33 * width);
                int widthSplitHigh = (int) Math.round(0.66 * width);

                int heightSplitLow = (int) Math.round(this.state.settings.split * height);
                int heightSplitHigh = (int) Math.round((1.0 - this.state.settings.split) * height);

                // add items that have been loaded
                while (!this.state.qviewables.isEmpty())
                {
                    Viewable<?> q = this.state.qviewables.poll();
                    Viewer.getInstance().control.add(q);
                }

                if (Layout.View3D.equals(this.layout))
                {
                    gl.glViewport(0, 0, width, height);
                    this.render3D.render(drawable, width, height);
                }
                else if (Layout.SliceI.equals(this.layout))
                {
                    gl.glViewport(0, 0, width, height);
                    this.renderI.render(drawable, width, height);
                }
                else if (Layout.SliceJ.equals(this.layout))
                {
                    gl.glViewport(0, 0, width, height);
                    this.renderJ.render(drawable, width, height);
                }
                else if (Layout.SliceK.equals(this.layout))
                {
                    gl.glViewport(0, 0, width, height);
                    this.renderK.render(drawable, width, height);
                }
                else if (Layout.SliceI3D.equals(this.layout))
                {
                    gl.glViewport(0, 0, widthSplit, height);
                    this.render3D.render(drawable, widthSplit, height);

                    gl.glViewport(widthSplit, 0, widthSplit, height);
                    this.renderI.render(drawable, widthSplit, height);
                }
                else if (Layout.SliceJ3D.equals(this.layout))
                {
                    gl.glViewport(0, 0, widthSplit, height);
                    this.render3D.render(drawable, widthSplit, height);

                    gl.glViewport(widthSplit, 0, widthSplit, height);
                    this.renderJ.render(drawable, widthSplit, height);
                }
                else if (Layout.SliceK3D.equals(this.layout))
                {
                    gl.glViewport(0, 0, widthSplit, height);
                    this.render3D.render(drawable, widthSplit, height);

                    gl.glViewport(widthSplit, 0, widthSplit, height);
                    this.renderK.render(drawable, widthSplit, height);
                }
                else if (Layout.OneByThree.equals(this.layout))
                {
                    gl.glViewport(0, heightSplitLow, width, heightSplitHigh);
                    this.render3D.render(drawable, width, heightSplitHigh);

                    gl.glViewport(0, 0, widthSplitLow, heightSplitLow);
                    this.renderI.render(drawable, widthSplitLow, heightSplitLow);

                    gl.glViewport(widthSplitLow, 0, widthSplitLow, heightSplitLow);
                    this.renderJ.render(drawable, widthSplitLow, heightSplitLow);

                    gl.glViewport(widthSplitHigh, 0, widthSplitLow, heightSplitLow);
                    this.renderK.render(drawable, widthSplitLow, heightSplitLow);
                }
                else if (Layout.TwoByTwo.equals(this.layout))
                {
                    gl.glViewport(0, 0, widthHalf, heightHalf);
                    this.render3D.render(drawable, widthHalf, heightHalf);

                    gl.glViewport(widthHalf, heightHalf, widthHalf, heightHalf);
                    this.renderI.render(drawable, widthHalf, heightHalf);

                    gl.glViewport(0, heightHalf, widthHalf, heightHalf);
                    this.renderJ.render(drawable, widthHalf, heightHalf);

                    gl.glViewport(widthHalf, 0, widthHalf, heightHalf);
                    this.renderK.render(drawable, widthHalf, heightHalf);
                }
                else
                {
                    Logging.error("invalid layout: " + this.layout);
                }
            }

            while (!this.state.qshots.isEmpty())
            {
                Pair<String, Integer> shot = this.state.qshots.poll();
                try
                {

                    String msg = "File(s) exists!  Overwrite?";
                    if (PathUtils.exists(shot.a) && !SwingUtils.getDecision(msg))
                    {
                        continue;
                    }


                    setbg.run();
                    BufferedImage bi = this.render3D.screenshot(drawable, width, height, shot.b);
                    ImageIO.write(bi, "png", new File(shot.a));
                    Logging.info("saved screenshot to " + shot.a);
                }
                catch (IOException e)
                {
                    Logging.info("failed to save screenshot to " + shot.a);
                }
            }

            if (this.showFPS)
            {
                gl.glViewport(0, 0, width, height);
                int point = 32;
                Font font = new Font("Helvetica", Font.BOLD, point);
                TextRenderer texter = new TextRenderer(font, true, false);

                texter.beginRendering(width, height);
                texter.setColor(1.0f, 1.0f, 1.0f, 0.75f);
                texter.draw("FPS: " + this.fps.rate(), point, this.getSurfaceHeight() - 2 * point);
                texter.endRendering();
            }
        }
        catch (RuntimeException e)
        {
            Viewer.getInstance().control.setStatusMessage("warning: failed display loop due to : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isVisible3D()
    {
        switch (this.layout)
        {
            case View3D:
            case TwoByTwo:
            case OneByThree:
            case SliceI3D:
            case SliceJ3D:
            case SliceK3D:
                return true;
            default:
                return false;
        }
    }

    public boolean isVisible2D(VolumeSlicePlane plane)
    {
        switch (this.layout)
        {
            case TwoByTwo:
            case OneByThree:
                return true;
            case SliceI:
            case SliceI3D:
                return plane.equals(VolumeSlicePlane.I);
            case SliceJ:
            case SliceJ3D:
                return plane.equals(VolumeSlicePlane.J);
            case SliceK:
            case SliceK3D:
                return plane.equals(VolumeSlicePlane.K);
            default:
                return false;
        }
    }

    private Pair<MyMouseListener, MyMouseEvent> mouseLayout(MouseEvent e)
    {
        Settings state = Viewer.getInstance().settings;

        int width = this.getWidth();
        int height = this.getHeight();

        MyMouseListener mel = this.render3D;
        MyMouseEvent mev = new MyMouseEvent();
        mev.button = e.getButton();
        mev.clicks = e.getClickCount();
        mev.modifiers = e.getModifiersEx();

        if (this.state.osname.contains("win") || this.state.osname.contains("linux"))
        {
            mev.xpos = MathUtils.round(this.state.scaling * e.getX());
            mev.ypos = MathUtils.round(this.state.scaling * (height - e.getY()));

            width = (int) (this.state.scaling * width);
            height = (int) (this.state.scaling * height);
        }
        else
        {
            double myscale = this.getSurfaceWidth() / (double) this.getWidth();

            mev.xpos = MathUtils.round(myscale * e.getX());
            mev.ypos = MathUtils.round(myscale * (height - e.getY()));

            width = (int) (myscale * width);
            height = (int) (myscale * height);
        }

        int widthSplit = (int) (state.halve * width);
        int widthHalf = width / 2;
        int heightHalf = height / 2;

        int widthSplitLow = (int) Math.round(0.33 * width);
        int widthSplitHigh = (int) Math.round(0.66 * width);

        int heightSplit = (int) Math.round(state.split * height);

        if (Layout.View3D.equals(this.layout))
        {
            mel = this.render3D;
        }
        else if (Layout.SliceI.equals(this.layout))
        {
            mel = this.renderI;
        }
        else if (Layout.SliceJ.equals(this.layout))
        {
            mel = this.renderJ;
        }
        else if (Layout.SliceK.equals(this.layout))
        {
            mel = this.renderK;
        }
        else if (Layout.SliceI3D.equals(this.layout))
        {
            if (mev.xpos < widthSplit)
            {
                mel = this.render3D;
            }
            else
            {
                mel = this.renderI;
                mev.shift(-widthSplit, 0);
            }
        }
        else if (Layout.SliceJ3D.equals(this.layout))
        {
            if (mev.xpos < widthSplit)
            {
                mel = this.render3D;
            }
            else
            {
                mel = this.renderJ;
                mev.shift(-widthSplit, 0);
            }
        }
        else if (Layout.SliceK3D.equals(this.layout))
        {
            if (mev.xpos < widthSplit)
            {
                mel = this.render3D;
            }
            else
            {
                mel = this.renderK;
                mev.shift(-widthSplit, 0);
            }
        }
        else if (Layout.OneByThree.equals(this.layout))
        {
            if (mev.ypos < heightSplit)
            {
                if (mev.xpos < widthSplitLow)
                {
                    mel = this.renderI;
                    mev.shift(0, 0);
                }
                else if (mev.xpos < widthSplitHigh)
                {
                    mel = this.renderJ;
                    mev.shift(-widthSplitLow, 0);
                }
                else
                {
                    mel = this.renderK;
                    mev.shift(-widthSplitHigh, 0);
                }
            }
            else
            {
                mel = this.render3D;
                mev.shift(0, -heightSplit);
            }
        }
        else if (Layout.TwoByTwo.equals(this.layout))
        {
            if (mev.ypos < heightHalf)
            {
                if (mev.xpos < widthHalf)
                {
                    mel = this.render3D;
                }
                else
                {
                    mel = this.renderK;
                    mev.shift(-widthHalf, 0);
                }
            }
            else
            {
                if (mev.xpos < widthHalf)
                {
                    mel = this.renderJ;
                    mev.shift(0, -heightHalf);
                }
                else
                {
                    mel = this.renderI;
                    mev.shift(-widthHalf, -heightHalf);
                }
            }
        }

        return Pair.of(mel, mev);
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        Pair<MyMouseListener, MyMouseEvent> pair = mouseLayout(e);
        pair.a.mouseClicked(pair.b);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        Pair<MyMouseListener, MyMouseEvent> pair = mouseLayout(e);
        pair.a.mousePressed(pair.b);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        Pair<MyMouseListener, MyMouseEvent> pair = mouseLayout(e);
        pair.a.mouseReleased(pair.b);
    }

    @Override
    public void mouseEntered(MouseEvent e)
    {
        Pair<MyMouseListener, MyMouseEvent> pair = mouseLayout(e);
        pair.a.mouseEntered(pair.b);
    }

    @Override
    public void mouseExited(MouseEvent e)
    {
        Pair<MyMouseListener, MyMouseEvent> pair = mouseLayout(e);
        pair.a.mouseExited(pair.b);
    }

    @Override
    public void mouseDragged(MouseEvent e)
    {
        this.touched();

        Pair<MyMouseListener, MyMouseEvent> pair = mouseLayout(e);
        pair.a.mouseDragged(pair.b);
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
        Pair<MyMouseListener, MyMouseEvent> pair = mouseLayout(e);
        pair.a.mouseMoved(pair.b);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e)
    {
        mouseLayout(e).a.mouseWheelMoved(e);
    }

    public void touched()
    {
        this.touched = System.currentTimeMillis();
    }

}
