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
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;
import qit.base.Logging;
import qit.base.structs.Pair;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.MatrixSource;
import qit.data.source.VectSource;
import qit.math.structs.Box;
import qit.math.structs.Line;
import qit.math.structs.Plane;
import qit.math.utils.MathUtils;
import qitview.models.ScreenMouse;
import qitview.models.ScreenPoint;
import qitview.models.Viewable;
import qitview.models.WorldMouse;
import qitview.models.WorldPoint;
import qitview.panels.Viewables;
import qitview.render.RenderUtils;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicTextField;
import qitview.widgets.ControlPanel;
import qitview.widgets.MyMouseEvent;
import qitview.widgets.MyMouseListener;
import qitview.widgets.SwingUtils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.function.Supplier;

public class Render3D implements MyMouseListener
{
    public static final int POINT = 32;
    public static final Font FONT = new Font("Helvetica", Font.BOLD, POINT);
    public static final TextRenderer TEXTER = new TextRenderer(FONT, true, false);

    public transient ScreenMouse mouse = new ScreenMouse();
    public transient String mode = Constants.INTERACTION_ROTATE;

    public transient Vect eye = null;
    public transient Vect look = null;
    public transient Vect up = null;

    public transient Box box = null;

    public float cameraScale = Constants.SCALE_DEFAULT;
    public Vect cameraPos = VectSource.create3D(Constants.XPOS_DEFAULT, Constants.YPOS_DEFAULT, Constants.ZPOS_DEFAULT);
    public Matrix cameraRot = Constants.ROT_DEFAULT;

    public void render(GLAutoDrawable drawable, int width, int height)
    {
        Settings state = Viewer.getInstance().settings;

        if (MathUtils.nonzero(state.xrotAuto))
        {
            this.rotateX(state.xrotAuto);
        }

        if (MathUtils.nonzero(state.yrotAuto))
        {
            this.rotateY(state.yrotAuto);
        }

        double aspect = width / (double) height;

        GL2 gl = drawable.getGL().getGL2();
        gl.glMatrixMode(GL2.GL_PROJECTION);

        double fov = state.fov;
        double near = 0.01f;
        double far = 100f;

        gl.glLoadIdentity();

        if (state.orthoView)
        {
            if (aspect > 1.0)
            {
                gl.glOrtho(-aspect, aspect, -1.0, 1.0, near, far);
            }
            else
            {
                double raspect = 1.0 / aspect;
                gl.glOrtho(-1.0, 1.0, -raspect, raspect, near, far);
            }
        }
        else
        {
            RenderUtils.GLU.gluPerspective(fov, aspect, near, far);
        }

        this.renderData(drawable, width, height);

        {
            // warning: TextRenderer seems to have a memory leak
            // beware of creating a new object inside this loop
            int point = MathUtils.round(POINT / Viewer.getInstance().scaling);
            TEXTER.beginRendering(width, height);
            TEXTER.setColor(1.0f, 1.0f, 1.0f, 0.75f);
            TEXTER.draw("3D", point, point);
            TEXTER.endRendering();
        }
    }

    public BufferedImage screenshot(GLAutoDrawable drawable, int w, int h, int scale)
    {
        GL2 gl = drawable.getGL().getGL2();
        Settings settings = Viewer.getInstance().settings;

        Runnable setbg = () ->
        {
            float backgroundRed = settings.bgRed;
            float backgroundGreen = settings.bgGreen;
            float backgroundBlue = settings.bgBlue;
            float backgroundAlpha = settings.transparent ? 0.0f : 1.0f;

            gl.glClearColor(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha);
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        };

        Supplier<int[]> shot = () ->
        {
            ByteBuffer pixelsRGBA = ByteBuffer.allocateDirect(w * h * 4);

            gl.glReadBuffer(GL2.GL_BACK);
            gl.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);
            gl.glReadPixels(0, 0, w, h, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, pixelsRGBA);

            int[] pixelInts = new int[w * h];
            int p = w * h * 4;
            int q;
            int i = 0;
            int w3 = w * 4;
            for (int row = 0; row < h; row++)
            {
                p -= w3;
                q = p;
                for (int col = 0; col < w; col++)
                {
                    int iR = Math.floorMod(pixelsRGBA.get(q++), 256);
                    int iG = Math.floorMod(pixelsRGBA.get(q++), 256);
                    int iB = Math.floorMod(pixelsRGBA.get(q++), 256);
                    int iA = Math.floorMod(pixelsRGBA.get(q++), 256);

                    int pint = (iA & 0xFF) << 24 | (iR & 0xFF) << 16 | (iG & 0xFF) << 8 | (iB & 0xFF);

                    pixelInts[i++] = pint;
                }
            }

            return pixelInts;
        };

        BufferedImage bufferedImage = new BufferedImage(scale * w, scale * h, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < scale; i++)
        {
            for (int j = 0; j < scale; j++)
            {
                int tw = w / scale;
                int th = h / scale;

                double fov = settings.fov;
                double aspect = w / (double) h;
                double near = 0.01f;
                double far = 100f;

                gl.glMatrixMode(GL2.GL_PROJECTION);
                gl.glLoadIdentity();

                if (settings.orthoView)
                {
                    double ntop = aspect > 1.0 ? 1.0 : aspect;
                    double nbottom = aspect > 1.0 ? -1.0 : -aspect;
                    double nright = aspect > 1.0 ? aspect : 1.0;
                    double nleft = aspect > 1.0 ? -aspect : -1.0;

                    double left = nleft + (((nright - nleft) * (j * tw)) / w);
                    double right = left + (((nright - nleft) * tw) / w);
                    double bottom = nbottom + (ntop - nbottom) * (i * th) / h;
                    double top = bottom + (ntop - nbottom) * th / h;

                    gl.glOrtho(left, right, bottom, top, near, far);
                }
                else
                {
                    double ntop = near * Math.tan(fov * 3.14159265 / 360.0);
                    double nbottom = -ntop;
                    double nleft = nbottom * aspect;
                    double nright = ntop * aspect;

                    double left = nleft + (((nright - nleft) * (j * tw)) / w);
                    double right = left + (((nright - nleft) * tw) / w);
                    double bottom = nbottom + (ntop - nbottom) * (i * th) / h;
                    double top = bottom + (ntop - nbottom) * th / h;

                    gl.glFrustum(left, right, bottom, top, near, far);
                }

                setbg.run();
                this.renderData(drawable, w, h);
                int[] pixelInts = shot.get();
                bufferedImage.setRGB(j * w, (scale - 1 - i) * h, w, h, pixelInts, 0, w);
            }
        }

        return bufferedImage;
    }

    /******************
     * PRIVATE METHODS
     ******************/

    // this assumes the frustrum has already been set up with one of the above
    private void renderData(GLAutoDrawable drawable, int width, int height)
    {
        Viewables data = Viewer.getInstance().data;
        Settings state = Viewer.getInstance().settings;

        synchronized (data)
        {
            GL2 gl = drawable.getGL().getGL2();

            if (this.box == null)
            {
                // check if there's anything to render
                return;
            }

            // probe some settings data
            Box box = this.box.copy();

            // compute bounds
            Vect min = box.getMin();
            Vect max = box.getMax();

            double largest = max.minus(min).max();

            float scale = (float) (MathUtils.zero(largest) ? 0.5f : 0.5f / largest);
            Vect center = max.plus(min).times(-0.5);

            List<Pair<Integer, float[]>> pairs = Lists.newArrayList();
            pairs.add(Pair.of(GL2.GL_LIGHT1, Constants.LIGHT1));
            pairs.add(Pair.of(GL2.GL_LIGHT2, Constants.LIGHT2));
            pairs.add(Pair.of(GL2.GL_LIGHT2, Constants.LIGHT3));

            // start the show

            for (Pair<Integer, float[]> pair : pairs)
            {
                gl.glLightfv(pair.a, GL2.GL_AMBIENT, RenderUtils.buffer(state.ambientLight, 4));
                gl.glLightfv(pair.a, GL2.GL_DIFFUSE, RenderUtils.buffer(state.diffuseLight, 4));
                gl.glLightfv(pair.a, GL2.GL_SPECULAR, RenderUtils.buffer(state.specularLight, 4));
                gl.glLightfv(pair.a, GL2.GL_POSITION, RenderUtils.buffer(pair.b));
                gl.glEnable(pair.a);
            }

            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            gl.glEnable(GL2.GL_NORMALIZE);

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();

            gl.glPushMatrix();

            Matrix modelview = MatrixSource.trans4(this.cameraPos);
            modelview = modelview.times(this.cameraRot.hom());
            modelview = modelview.times(MatrixSource.scale4(this.cameraScale));
            modelview = modelview.times(MatrixSource.scale4(scale));
            modelview = modelview.times(MatrixSource.trans4(center));

            gl.glMultMatrixf(RenderUtils.buffer(modelview));

            for (Viewable<?> viewable : Viewer.getInstance().data.getVisible())
            {
                if (viewable.hasData())
                {
                    viewable.display(gl);
                }
            }

            // update the look info
            this.updateLook(drawable);

            // handle mouse events
            ScreenMouse screen = this.mouse;

            WorldMouse world = new WorldMouse();
            world.current = screen.current == null ? null : RenderUtils.screenToWorld(drawable, screen.current, width, height);
            world.press = screen.press == null ? null : RenderUtils.screenToWorld(drawable, screen.press, width, height);
            world.pick = screen.pick;
            world.control = screen.control;
            world.shift = screen.shift;
            world.time = screen.time;

            if (this.mouse.doubled)
            {
                double mindist = Double.MAX_VALUE;
                Integer minidx = null;
                for (int idx = 0; idx < data.size(); idx++)
                {
                    if (data.getVisibility(idx))
                    {
                        Double dist = data.getViewable(idx).dist(world);
                        if (dist != null && dist < mindist)
                        {
                            mindist = dist;
                            minidx = idx;
                        }
                    }
                }

                if (minidx != null)
                {
                    Viewer.getInstance().control.setStatusMessage("selected: " + data.getName(minidx));

                    if (world.shift)
                    {
                        data.addSelection(minidx);
                    }
                    else
                    {
                        data.setSelection(minidx);
                    }
                }

                this.mouse.doubled = false;
            }

            {
                WorldMouse bgworld = new WorldMouse();
                data.getFirstSelectionIndex().ifPresent(sel ->
                {
                    String mode = this.mode;
                    for (int i = 0; i < data.size(); i++)
                    {
                        Viewable v = data.getViewable(i);

                        // see Controller.mouseDragger for highly coupled rules for interaction
                        if (sel != null && i == sel)
                        {
                            if (world.shift || world.control)
                            {
                                // always respect panning and zooming
                                mode = Constants.INTERACTION_ROTATE;
                            }
                            else if (v.modes().contains(mode))
                            {
                                if (world.pick)
                                {
                                    // let the user easily rotate by undoing the manual mode with the pick button
                                    mode = Constants.INTERACTION_ROTATE;
                                    world.pick = false;
                                }
                                else
                                {
                                    // the manual mode is set, so simulate a pick
                                    world.pick = true;
                                }
                            }

                            v.handle(world, mode);
                        }
                        else
                        {
                            v.handle(bgworld, Constants.INTERACTION_ROTATE);
                        }
                    }
                });
            }

            // render reference objects
            renderReference(gl, box, world.current);
            Viewer.getInstance().gui.getQuery().render3D(gl);
            renderAnatomical(drawable, box, width, height);

            gl.glPopMatrix();

            Viewer.getInstance().annotation.render(drawable);
        }
    }

    private void updateLook(GLAutoDrawable drawable)
    {
        GL2 gl = drawable.getGL().getGL2();

        double[] modelview = new double[16];
        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);

        double[] projection = new double[16];
        gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);

        int[] viewport = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);

        // get the center point
        int winX = (int) Math.round(viewport[2] * 0.5);
        int winY = (int) Math.round(viewport[3] * 0.5);

        FloatBuffer dbuf = FloatBuffer.allocate(1);
        gl.glReadPixels(winX, winY, 1, 1, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, dbuf);
        double winZ = dbuf.get(0);

        double[] hit = new double[3];
        RenderUtils.GLU.gluUnProject(winX, winY, winZ, modelview, 0, projection, 0, viewport, 0, hit, 0);

        double[] eye = new double[3];
        RenderUtils.GLU.gluUnProject(winX, winY, 0, modelview, 0, projection, 0, viewport, 0, eye, 0);

        double[] up = new double[3];
        RenderUtils.GLU.gluUnProject(winX, 0, winZ, modelview, 0, projection, 0, viewport, 0, up, 0);

        this.eye = VectSource.create(eye);
        this.up = VectSource.create(hit).minus(VectSource.create(up)).normalize();
        this.look = VectSource.create(hit).minus(this.eye).normalize();
    }

    public void renderAnatomical(GLAutoDrawable drawable, Box box, int width, int height)
    {
        if (!Viewer.getInstance().settings.anatomical)
        {
            return;
        }

        int point = 32;
        int halfpoint = point / 2;
        Font font = new Font("Helvetica", Font.BOLD, point);
        float red = 255 / (float) 255;
        float green = 255 / (float) 255;
        float blue = 255 / (float) 255;

        Vect cen = box.getCenter();
        Vect max = box.getMax();
        Vect min = box.getMin();
        Vect high = max.plus(max.minus(cen).times(0.2));
        Vect low = min.minus(cen.minus(min).times(0.2));

        ScreenPoint right = RenderUtils.worldToScreen(drawable, VectSource.create(high.getX(), cen.getY(), cen.getZ()), width, height);
        ScreenPoint left = RenderUtils.worldToScreen(drawable, VectSource.create(low.getX(), cen.getY(), cen.getZ()), width, height);
        ScreenPoint ant = RenderUtils.worldToScreen(drawable, VectSource.create(cen.getX(), high.getY(), cen.getZ()), width, height);
        ScreenPoint pos = RenderUtils.worldToScreen(drawable, VectSource.create(cen.getX(), low.getY(), cen.getZ()), width, height);
        ScreenPoint sup = RenderUtils.worldToScreen(drawable, VectSource.create(cen.getX(), cen.getY(), high.getZ()), width, height);
        ScreenPoint inf = RenderUtils.worldToScreen(drawable, VectSource.create(cen.getX(), cen.getY(), low.getZ()), width, height);

        Vect look = this.look.normalize();
        TextRenderer TEXTER = new TextRenderer(font, true, false);

        TEXTER.beginRendering(width, height);

        TEXTER.setColor(red, green, blue, (float) fade(look, VectSource.create(1, 0, 0)));
        TEXTER.draw("R", right.x - halfpoint, height - right.y - halfpoint);

        TEXTER.setColor(red, green, blue, (float) fade(look, VectSource.create(-1, 0, 0)));
        TEXTER.draw("L", left.x - halfpoint, height - left.y - halfpoint);

        TEXTER.setColor(red, green, blue, (float) fade(look, VectSource.create(0, 1, 0)));
        TEXTER.draw("A", ant.x - halfpoint, height - ant.y - halfpoint);

        TEXTER.setColor(red, green, blue, (float) fade(look, VectSource.create(0, -1, 0)));
        TEXTER.draw("P", pos.x - halfpoint, height - pos.y - halfpoint);

        TEXTER.setColor(red, green, blue, (float) fade(look, VectSource.create(0, 0, 1)));
        TEXTER.draw("S", sup.x - halfpoint, height - sup.y - halfpoint);

        TEXTER.setColor(red, green, blue, (float) fade(look, VectSource.create(0, 0, -1)));
        TEXTER.draw("I", inf.x - halfpoint, height - inf.y - halfpoint);

        TEXTER.endRendering();
    }

    private static double fade(Vect look, Vect dir)
    {
        double angle = look.angleDeg(dir) / 90.;
        double order = angle < 1 ? 6 : 3;
        angle = angle < 1 ? angle : 2 - angle;

        double xnp = Math.pow(angle, order - 1);
        double xn = angle * xnp;

        return order * xnp + (order - 1) * xn;
    }

    public void renderReference(GL2 gl, Box box, WorldPoint point)
    {
        try
        {
            Settings settings = Viewer.getInstance().settings;
            if (Viewer.getInstance().settings.scaleVisible)
            {
                Vect min = box.getMin();
                Vect max = box.getMax();

                double startX = min.get(0);
                double endX = max.get(0);
                double startY = min.get(1);
                double endY = max.get(1);
                double startZ = min.get(2);
                double endZ = max.get(2);

                double sizeX = endX - startX;
                double sizeY = endY - startY;
                double sizeZ = endZ - startZ;

                double scale = Viewer.getInstance().settings.scaleValue;
                if (MathUtils.zero(scale))
                {
                    int maxNum = 10;
                    double sizeMax = Math.max(sizeZ, Math.max(sizeY, sizeX));
                    scale = Math.round(sizeMax / maxNum);
                }

                startX = Math.floor(startX / scale) * scale;
                endX = Math.ceil(endX / scale) * scale;
                startY = Math.floor(startY / scale) * scale;
                endY = Math.ceil(endY / scale) * scale;
                startZ = Math.floor(startZ / scale) * scale;
                endZ = Math.ceil(endZ / scale) * scale;

                Line line = Line.fromTwoPoints(point.point, point.hit);

                Vect startXint = Plane.createPositiveX(startX).intersect(line).get(0).getPoint();
                Vect endXint = Plane.createPositiveX(endX).intersect(line).get(0).getPoint();
                double startXdist = point.point.dist(startXint);
                double endXdist = point.point.dist(endXint);
                double farX = (startXdist > endXdist) ? startX : endX;
                int numX = (int) Math.round((endX - startX) / scale);

                Vect startYint = Plane.createPositiveY(startY).intersect(line).get(0).getPoint();
                Vect endYint = Plane.createPositiveY(endY).intersect(line).get(0).getPoint();
                double startYdist = point.point.dist(startYint);
                double endYdist = point.point.dist(endYint);
                double farY = (startYdist > endYdist) ? startY : endY;
                int numY = (int) Math.round((endY - startY) / scale);

                Vect startZint = Plane.createPositiveZ(startZ).intersect(line).get(0).getPoint();
                Vect endZint = Plane.createPositiveZ(endZ).intersect(line).get(0).getPoint();
                double startZdist = point.point.dist(startZint);
                double endZdist = point.point.dist(endZint);
                double farZ = (startZdist > endZdist) ? startZ : endZ;
                int numZ = (int) Math.round((endZ - startZ) / scale);

                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth(settings.scaleWidth);

                double r = settings.scaleRed;
                double g = settings.scaleGreen;
                double b = settings.scaleBlue;
                gl.glColor3d(r, g, b);

                for (int i = 0; i <= numX; i++)
                {
                    double lineX = startX + scale * i;

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(lineX, farY, startZ);
                    gl.glVertex3d(lineX, farY, endZ);
                    gl.glEnd();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(lineX, startY, farZ);
                    gl.glVertex3d(lineX, endY, farZ);
                    gl.glEnd();
                }

                for (int i = 0; i <= numY; i++)
                {
                    double lineY = startY + scale * i;

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(farX, lineY, startZ);
                    gl.glVertex3d(farX, lineY, endZ);
                    gl.glEnd();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(startX, lineY, farZ);
                    gl.glVertex3d(endX, lineY, farZ);
                    gl.glEnd();
                }

                for (int i = 0; i <= numZ; i++)
                {
                    double lineZ = startZ + scale * i;

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(farX, startY, lineZ);
                    gl.glVertex3d(farX, endY, lineZ);
                    gl.glEnd();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(startX, farY, lineZ);
                    gl.glVertex3d(endX, farY, lineZ);
                    gl.glEnd();
                }

                if (settings.scaleBoxVisible)
                {
                    // one side
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(startX, startY, startZ);
                    gl.glVertex3d(endX, startY, startZ);
                    gl.glVertex3d(endX, endY, startZ);
                    gl.glVertex3d(startX, endY, startZ);
                    gl.glVertex3d(startX, startY, startZ);
                    gl.glEnd();

                    // other side
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(startX, startY, endZ);
                    gl.glVertex3d(endX, startY, endZ);
                    gl.glVertex3d(endX, endY, endZ);
                    gl.glVertex3d(startX, endY, endZ);
                    gl.glVertex3d(startX, startY, endZ);
                    gl.glEnd();

                    // connections
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(startX, startY, startZ);
                    gl.glVertex3d(startX, startY, endZ);
                    gl.glEnd();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(endX, startY, startZ);
                    gl.glVertex3d(endX, startY, endZ);
                    gl.glEnd();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(startX, endY, startZ);
                    gl.glVertex3d(startX, endY, endZ);
                    gl.glEnd();

                    gl.glBegin(GL2.GL_LINE_STRIP);
                    gl.glVertex3d(endX, endY, startZ);
                    gl.glVertex3d(endX, endY, endZ);
                    gl.glEnd();
                }
            }

            if (settings.boxVisible)
            {
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth(settings.lineWidth);

                double r = settings.boxRed;
                double g = settings.boxGreen;
                double b = settings.boxBlue;
                gl.glColor3d(r, g, b);

                Vect min = box.getMin();
                Vect max = box.getMax();

                double startx = min.get(0);
                double endx = max.get(0);
                double starty = min.get(1);
                double endy = max.get(1);
                double startz = min.get(2);
                double endz = max.get(2);

                // one side
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(startx, starty, startz);
                gl.glVertex3d(endx, starty, startz);
                gl.glVertex3d(endx, endy, startz);
                gl.glVertex3d(startx, endy, startz);
                gl.glVertex3d(startx, starty, startz);
                gl.glEnd();

                // other side
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(startx, starty, endz);
                gl.glVertex3d(endx, starty, endz);
                gl.glVertex3d(endx, endy, endz);
                gl.glVertex3d(startx, endy, endz);
                gl.glVertex3d(startx, starty, endz);
                gl.glEnd();

                // connections
                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(startx, starty, startz);
                gl.glVertex3d(startx, starty, endz);
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(endx, starty, startz);
                gl.glVertex3d(endx, starty, endz);
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(startx, endy, startz);
                gl.glVertex3d(startx, endy, endz);
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glVertex3d(endx, endy, startz);
                gl.glVertex3d(endx, endy, endz);
                gl.glEnd();
            }

            if (settings.frameVisible)
            {
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glLineWidth(settings.lineWidth);

                Vect max = box.getMax();

                double x = 1.25 * max.get(0);
                double y = 1.25 * max.get(1);
                double z = 1.25 * max.get(2);

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glColor3d(1.0, 0.0, 0.0);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(x, 0, 0);
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glColor3d(0.0, 1.0, 0.0);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(0, y, 0);
                gl.glEnd();

                gl.glBegin(GL2.GL_LINE_STRIP);
                gl.glColor3d(0.0, 0.0, 1.0);
                gl.glVertex3d(0, 0, 0);
                gl.glVertex3d(0, 0, z);
                gl.glEnd();
            }
        }
        catch (Exception e)
        {
            // skip this render
        }
    }

    private void mouseUpdate(MyMouseEvent e)
    {
        // this.touched();

        Settings state = Viewer.getInstance().settings;
        ScreenMouse mouse = this.mouse;
        mouse.current = new ScreenPoint(e.xpos, e.ypos);

        mouse.control = (e.modifiers & MouseEvent.CTRL_DOWN_MASK) == MouseEvent.CTRL_DOWN_MASK;
        mouse.control |= (e.modifiers & MouseEvent.BUTTON2_DOWN_MASK) == MouseEvent.BUTTON2_DOWN_MASK;

        mouse.shift = (e.modifiers & MouseEvent.SHIFT_DOWN_MASK) == MouseEvent.SHIFT_DOWN_MASK;
        mouse.shift |= (e.modifiers & MouseEvent.BUTTON3_DOWN_MASK) == MouseEvent.BUTTON3_DOWN_MASK;

        mouse.pick = (e.modifiers & MouseEvent.ALT_DOWN_MASK) == MouseEvent.ALT_DOWN_MASK;
        mouse.pick |= Viewer.getInstance().keys.containsKey(KeyEvent.VK_BACK_QUOTE);
        mouse.pick |= Viewer.getInstance().keys.containsKey(KeyEvent.VK_DEAD_TILDE);
        mouse.pick |= Viewer.getInstance().keys.containsKey(KeyEvent.VK_F1);

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

        if (Viewer.getInstance().settings.autoSort)
        {
            Viewer.getInstance().control.sortFaces();
        }
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

        ScreenMouse mouse = this.mouse;
        Settings state = Viewer.getInstance().settings;

        if (mouse.current != null)
        {
            boolean rotate = Constants.INTERACTION_ROTATE.equals(this.mode);
            boolean pan = Constants.INTERACTION_PAN.equals(this.mode);
            boolean zoom = Constants.INTERACTION_ZOOM.equals(this.mode);

            double dx = e.xpos - mouse.current.x;
            double dy = e.ypos - mouse.current.y;

            boolean mrb = e.hasMiddle() && e.hasRight() && !e.hasLeft();

            if (!mouse.pick)
            {
                if ((e.hasLeft() && mouse.control && mouse.shift) || (e.hasMiddle() && e.hasRight()))
                {
                    this.scale(dy * state.scaleMouse);
                }
                else if ((zoom && e.hasLeft()) || (e.hasLeft() && mouse.control && !mouse.shift) || (!e.hasMiddle() && e.hasRight()))
                {
                    this.moveZ(dy * state.zposMouse);
                }
                else if ((pan && e.hasLeft()) || (e.hasLeft() && !mouse.control && mouse.shift) || (e.hasMiddle() && !e.hasRight()))
                {
                    this.moveX(dx * state.xposMouse);
                    this.moveY(dy * state.yposMouse);
                }
                else if ((rotate && e.hasLeft()) || (e.hasLeft() && mouse.control && mouse.shift) || mrb)
                {
                    this.rotateY(dx * state.yrotMouse);
                    this.rotateX(dy * state.xrotMouse);
                }
            }
        }

        mouseUpdate(e);
    }

    public void mouseWheelMoved(MouseWheelEvent e)
    {
        Interface inter = Viewer.getInstance().gui;
        int notches = e.getWheelRotation();
        if (notches < 0)
        {
            // up
            inter.changeSlice(1);
        }
        else
        {
            // down
            inter.changeSlice(-1);
        }
    }

    public synchronized void rotate(Matrix R)
    {
        this.cameraRot = R.times(this.cameraRot);
    }

    public synchronized void moveX(double delta)
    {
        this.cameraPos.plusEquals(0, delta);
    }

    public synchronized void moveZ(double delta)
    {
        this.cameraPos.plusEquals(2, delta);
    }

    public synchronized void moveY(double delta)
    {
        this.cameraPos.plusEquals(1, delta);
    }

    public synchronized void scale(double delta)
    {
        this.cameraScale += delta;
        this.cameraScale = Math.max(Constants.SCALE_MIN, this.cameraScale);
    }

    public synchronized void rotateY(double delta)
    {
        Matrix rotmat = MatrixSource.rotation(Constants.Y_AXIS, delta);
        this.cameraRot = rotmat.times(this.cameraRot);
    }

    public synchronized void rotateX(double delta)
    {
        Matrix rotmat = MatrixSource.rotation(Constants.X_AXIS, delta);
        this.cameraRot = rotmat.times(this.cameraRot);
    }

    public synchronized void pose(double azimuth, double elevation)
    {
        Logging.info("updating pose angles");
        Matrix xrot = MatrixSource.rotation(Constants.X_AXIS, Math.PI * elevation / 180.);
        Matrix yrot = MatrixSource.rotation(Constants.Y_AXIS, Math.PI * azimuth / 180.);
        this.cameraRot = xrot.times(yrot);
    }

    public synchronized void resetPose()
    {
        this.cameraRot = Constants.ROT_DEFAULT;
        this.cameraPos.set(0, Constants.XPOS_DEFAULT);
        this.cameraPos.set(1, Constants.YPOS_DEFAULT);
        this.cameraPos.set(2, Constants.ZPOS_DEFAULT);
        this.cameraScale = Constants.SCALE_DEFAULT;
    }

    public synchronized void poseRight()
    {
        Logging.info("showing right view");
        this.resetPose();
        this.rotate(MatrixSource.rotation(Constants.X_AXIS, -Math.PI / 2.0));
        this.rotate(MatrixSource.rotation(Constants.Y_AXIS, -Math.PI / 2.0));
    }

    public synchronized void poseLeft()
    {
        Logging.info("showing left view");
        this.resetPose();
        this.rotate(MatrixSource.rotation(Constants.X_AXIS, -Math.PI / 2.0));
        this.rotate(MatrixSource.rotation(Constants.Y_AXIS, +Math.PI / 2.0));
    }

    public synchronized void poseTop()
    {
        Logging.info("showing top view");
        this.resetPose();
    }

    public synchronized void poseBottom()
    {
        Logging.info("showing bottom view");
        this.resetPose();
        this.rotate(MatrixSource.rotation(Constants.Y_AXIS, Math.PI));
    }

    public synchronized void poseBack()
    {
        Logging.info("showing back view");
        this.resetPose();
        this.rotate(MatrixSource.rotation(Constants.X_AXIS, -Math.PI / 2.0));
    }

    public synchronized void poseFront()
    {
        Logging.info("showing front view");
        this.poseBack();
        this.rotate(MatrixSource.rotation(Constants.Y_AXIS, Math.PI));
    }

    public synchronized void zoomDetail()
    {
        Logging.info("zooming to selected objects");
        boxDetail();
        zoomReset();
    }

    public synchronized void boxUpdate()
    {
        Settings state = Viewer.getInstance().settings;

        if (state.detail != null)
        {
            this.box = state.detail;
        }
        else
        {
            boxOverview();
        }
    }

    public synchronized void boxDetail()
    {
        Box nbox = null;
        for (int idx : Viewer.getInstance().data.getSelectionIndex())
        {
            Viewable<?> r = Viewer.getInstance().data.getViewable(idx);
            if (nbox == null)
            {
                nbox = r.getBounds();
            }
            else
            {
                nbox = nbox.union(r.getBounds());
            }
        }

        this.box = nbox;
        Viewer.getInstance().settings.detail = nbox;
    }

    public synchronized void zoomReset()
    {
        this.cameraPos.set(0, Constants.XPOS_DEFAULT);
        this.cameraPos.set(1, Constants.YPOS_DEFAULT);
        this.cameraPos.set(2, Constants.ZPOS_DEFAULT);
        this.cameraScale = Constants.SCALE_DEFAULT;
    }

    public synchronized void boxOverview()
    {
        Box nbox = null;
        for (Viewable<?> v : Viewer.getInstance().data.getAll())
        {
            if (v.hasBounds())
            {
                if (nbox == null)
                {
                    nbox = v.getBounds().copy();
                }
                else
                {
                    nbox = nbox.union(v.getBounds());
                }
            }
        }

        this.box = nbox;
        Viewer.getInstance().settings.detail = null;
    }

    public synchronized void zoomOverview()
    {
        Logging.info("zooming to all objects");
        boxOverview();
        zoomReset();
    }

    public synchronized void poseAngles()
    {
        try
        {
            final JDialog dialog = new JDialog();
            ControlPanel controls = new ControlPanel();

            final BasicTextField azimuth = new BasicTextField("6");
            final BasicTextField elevation = new BasicTextField("6");

            azimuth.setColumns(10);
            elevation.setColumns(10);

            controls.addControl("Azimuth", azimuth);
            controls.addControl("Elevation", elevation);

            JPanel wholePanel = new JPanel();
            wholePanel.setLayout(new BoxLayout(wholePanel, BoxLayout.PAGE_AXIS));
            wholePanel.add(controls);

            BasicButton cancel = new BasicButton("Cancel");
            cancel.setToolTipText("close this window without changing the view");
            cancel.addActionListener(new ActionListener()
            {
                public synchronized void actionPerformed(ActionEvent e)
                {
                    dialog.dispose();
                }
            });

            BasicButton update = new BasicButton("Update");
            update.setToolTipText("apply the changes and keep this window open");
            update.addActionListener(new ActionListener()
            {
                public synchronized void actionPerformed(ActionEvent ae)
                {
                    try
                    {
                        double a = Double.valueOf(azimuth.getText());
                        double e = Double.valueOf(elevation.getText());
                        Render3D.this.pose(a, e);
                    }
                    catch (Exception e)
                    {
                        Viewer.getInstance().control.setStatusError("failed to set pose");
                    }
                }
            });

            BasicButton apply = new BasicButton("Apply");
            apply.setToolTipText("apply the changes and close this window");
            apply.addActionListener(new ActionListener()
            {
                public synchronized void actionPerformed(ActionEvent ae)
                {
                    try
                    {
                        double a = Double.valueOf(azimuth.getText());
                        double e = Double.valueOf(elevation.getText());
                        Render3D.this.pose(a, e);
                    }
                    catch (Exception e)
                    {
                        Viewer.getInstance().control.setStatusError("failed to set pose");
                    }
                    dialog.dispose();
                }
            });

            {
                JPanel subpanel = new JPanel();
                subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));
                subpanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                subpanel.add(cancel);
                subpanel.add(javax.swing.Box.createHorizontalGlue());
                subpanel.add(update);
                subpanel.add(javax.swing.Box.createHorizontalGlue());
                subpanel.add(apply);

                wholePanel.add(subpanel);
            }

            // respond to keyboard
            dialog.getRootPane().setDefaultButton(apply);

            dialog.add(wholePanel);
            dialog.setTitle("Set a viewing orientation");
            dialog.pack();
            dialog.setLocationRelativeTo(Viewer.getInstance().gui.getFrame());
            dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            dialog.setResizable(false);
            dialog.setVisible(true);
            SwingUtils.addEscapeListener(dialog);

            azimuth.setText("0");
            elevation.setText("0");
        }
        catch (RuntimeException e)
        {
            Viewer.getInstance().control.setStatusMessage("warning: failed to zoom angles");
            e.printStackTrace();
        }
    }

    public synchronized void autoUpdate()
    {
    }
}
