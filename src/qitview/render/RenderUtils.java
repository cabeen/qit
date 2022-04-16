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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.glu.GLU;
import qit.base.Global;
import qit.base.Logging;
import qit.data.datasets.Matrix;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qitview.main.Interface;
import qitview.main.Viewer;
import qitview.models.ScreenPoint;
import qitview.models.WorldPoint;

public class RenderUtils
{
    public static GLU GLU = new GLU();

    public static double fade(Vect look, Vect dir)
    {
        double angle = look.angleDeg(dir) / 90.;
        double order = angle < 1 ? 6 : 3;
        angle = angle < 1 ? angle : 2 - angle;

        double xnp = Math.pow(angle, order - 1);
        double xn = angle * xnp;

        return order * xnp + (order - 1) * xn;
    }

    public static ScreenPoint worldToScreen(GLAutoDrawable drawable, Vect p, int width, int height)
    {
        GL2 gl = drawable.getGL().getGL2();

        double[] modelview = new double[16];
        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);

        double[] projection = new double[16];
        gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);

        int[] viewport = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);

        double x = p.getX();
        double y = p.getY();
        double z = p.getZ();

        double[] point = new double[3];
        GLU.gluProject(x, y, z, modelview, 0, projection, 0, viewport, 0, point, 0);

        double scaleX = width / (double) viewport[2];
        double scaleY = height / (double) viewport[3];

        int screenX = (int) Math.round(scaleX * point[0]);
        int screenY = height - (int) Math.round(scaleY * point[1]);

        return new ScreenPoint(screenX, screenY);
    }

    public static WorldPoint screenToWorld(GLAutoDrawable drawable, ScreenPoint p, int width, int height)
    {
        GL2 gl = drawable.getGL().getGL2();

        double[] modelview = new double[16];
        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);

        double[] projection = new double[16];
        gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);

        int[] viewport = new int[4];
        gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);

        // This funny business here is to accomodate hi-dpi screens
        // It turns out that JOGL supports the higher resolution, but
        // the Swing components do not.  This means the mouse coordinates
        // can be at a different scaleCamera than the viewport.  We have to
        // do some additional work below to make sure we account for that!

        double nx = p.x / (double) width;
        double ny = p.y / (double) height;

        int winX = (int) Math.round(viewport[0] + viewport[2] * nx);
        int winY = (int) Math.round(viewport[1] + viewport[3] * ny);

        FloatBuffer dbuf = FloatBuffer.allocate(1);
        gl.glReadPixels(winX, winY, 1, 1, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, dbuf);
        double winZ = dbuf.get(0);

        double[] hit = new double[3];
        GLU.gluUnProject(winX, winY, winZ, modelview, 0, projection, 0, viewport, 0, hit, 0);

        double[] point = new double[3];
        GLU.gluUnProject(winX, winY, 0, modelview, 0, projection, 0, viewport, 0, point, 0);

        double[] up = new double[3];
        GLU.gluUnProject(0, winY, 0, modelview, 0, projection, 0, viewport, 0, up, 0);

        WorldPoint out = new WorldPoint();
        out.up = VectSource.create(up);
        out.point = VectSource.create(point);
        out.hit = VectSource.create(hit);
        out.screen = p;

        return out;
    }
    public static void glTransform(GL2 gl, Matrix m)
    {
        double[] bufferAff = new double[16];
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                bufferAff[j * 4 + i] = m.get(i, j);
            }
        }
        bufferAff[15] = 1;
        ByteBuffer buffer16d = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());

        // jdk 9 changed the buffer interface, so we need this goofy cast for backwards compatibility
        gl.glMultMatrixd((DoubleBuffer) ((Buffer) buffer16d.asDoubleBuffer().put(bufferAff)).flip());
    }

    public static void glTransform(GL2 gl, Vect pos)
    {
        double px = pos.get(0);
        double py = pos.get(1);
        double pz = pos.get(2);

        gl.glTranslated(px, py, pz);
    }

    public static void glTransform(GL2 gl, double scale, Vect pos)
    {
        glTransform(gl, VectSource.create3D(scale, scale, scale), pos);
    }

    public static void glTransform(GL2 gl, Vect scale, Vect pos)
    {
        double sx = scale.get(0);
        double sy = scale.get(1);
        double sz = scale.get(2);

        double px = pos.get(0);
        double py = pos.get(1);
        double pz = pos.get(2);

        gl.glTranslated(px, py, pz);
        gl.glScaled(sx, sy, sz);
    }

    public static void glTransform(GL2 gl, Vect scale, Vect pos, Vect dir)
    {
        double sx = scale.get(0);
        double sy = scale.get(1);
        double sz = scale.get(2);

        double px = pos.get(0);
        double py = pos.get(1);
        double pz = pos.get(2);

        Vect v1 = dir;
        Vect v2 = v1.perp();
        Vect v3 = v1.cross(v2);

        double[] fbuffer = new double[16];
        fbuffer[0 * 4 + 0] = v3.get(0);
        fbuffer[0 * 4 + 1] = v3.get(1);
        fbuffer[0 * 4 + 2] = v3.get(2);
        fbuffer[1 * 4 + 0] = v2.get(0);
        fbuffer[1 * 4 + 1] = v2.get(1);
        fbuffer[1 * 4 + 2] = v2.get(2);
        fbuffer[2 * 4 + 0] = v1.get(0);
        fbuffer[2 * 4 + 1] = v1.get(1);
        fbuffer[2 * 4 + 2] = v1.get(2);
        fbuffer[15] = 1;

        ByteBuffer bbuffer = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());

        gl.glTranslated(px, py, pz);

        // jdk 9 changed the buffer interface, so we need this goofy cast for backwards compatibility
        gl.glMultMatrixd((DoubleBuffer) ((Buffer) bbuffer.asDoubleBuffer().put(fbuffer)).flip());
        gl.glScaled(sx, sy, sz);
    }

    public static void glTransform(GL2 gl, Vect scale, Vect pos, Matrix R)
    {
        double sx = scale.get(0);
        double sy = scale.get(1);
        double sz = scale.get(2);

        double px = pos.get(0);
        double py = pos.get(1);
        double pz = pos.get(2);

        double[] fbuffer = new double[16];
        fbuffer[0 * 4 + 0] = R.get(0, 0);
        fbuffer[0 * 4 + 1] = R.get(1, 0);
        fbuffer[0 * 4 + 2] = R.get(2, 0);
        fbuffer[1 * 4 + 0] = R.get(0, 1);
        fbuffer[1 * 4 + 1] = R.get(1, 1);
        fbuffer[1 * 4 + 2] = R.get(2, 1);
        fbuffer[2 * 4 + 0] = R.get(0, 2);
        fbuffer[2 * 4 + 1] = R.get(1, 2);
        fbuffer[2 * 4 + 2] = R.get(2, 2);
        fbuffer[15] = 1;

        ByteBuffer bbuffer = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder());

        gl.glTranslated(px, py, pz);

        // jdk 9 changed the buffer interface, so we need this goofy cast for backwards compatibility
        gl.glMultMatrixd((DoubleBuffer) ((Buffer) bbuffer.asDoubleBuffer().put(fbuffer)).flip());
        gl.glScaled(sx, sy, sz);
    }

    public static int pack(Vect fcolor)
    {
        // compute 0<->255 RGB values
        int r = (int) Math.round(fcolor.get(0) * 255);
        int g = (int) Math.round(fcolor.get(1) * 255);
        int b = (int) Math.round(fcolor.get(2) * 255);
        int a = fcolor.size() > 3 ? (int) Math.round(fcolor.get(3) * 255) : 255;

        // pack into int array
        int icolor = 0;
        icolor |= (r & 0xFF) << 24;
        icolor |= (g & 0xFF) << 16;
        icolor |= (b & 0xFF) << 8;
        icolor |= (a & 0xFF) << 0;

        return icolor;
    }

    public static int pack(Vect fcolor, double alpha)
    {
        // compute 0<->255 RGB values
        int r = (int) Math.round(fcolor.get(0) * 255);
        int g = (int) Math.round(fcolor.get(1) * 255);
        int b = (int) Math.round(fcolor.get(2) * 255);
        int a = (int) Math.round(alpha * 255);

        // pack into int array
        int icolor = 0;
        icolor |= (r & 0xFF) << 24;
        icolor |= (g & 0xFF) << 16;
        icolor |= (b & 0xFF) << 8;
        icolor |= (a & 0xFF) << 0;

        return icolor;
    }


    public static void multiply(FloatBuffer a, FloatBuffer b, FloatBuffer d)
    {
        final int aP = a.position();
        final int bP = b.position();
        final int dP = d.position();

        for (int i = 0; i < 4; i++)
        {
            final float ai0 = a.get(aP + i + 0 * 4);
            final float ai1 = a.get(aP + i + 1 * 4);
            final float ai2 = a.get(aP + i + 2 * 4);
            final float ai3 = a.get(aP + i + 3 * 4);

            d.put(dP + i + 0 * 4, ai0 * b.get(bP + 0 + 0 * 4) + ai1 * b.get(bP + 1 + 0 * 4) + ai2 * b.get(bP + 2 + 0 * 4) + ai3 * b.get(bP + 3 + 0 * 4));
            d.put(dP + i + 1 * 4, ai0 * b.get(bP + 0 + 1 * 4) + ai1 * b.get(bP + 1 + 1 * 4) + ai2 * b.get(bP + 2 + 1 * 4) + ai3 * b.get(bP + 3 + 1 * 4));
            d.put(dP + i + 2 * 4, ai0 * b.get(bP + 0 + 2 * 4) + ai1 * b.get(bP + 1 + 2 * 4) + ai2 * b.get(bP + 2 + 2 * 4) + ai3 * b.get(bP + 3 + 2 * 4));
            d.put(dP + i + 3 * 4, ai0 * b.get(bP + 0 + 3 * 4) + ai1 * b.get(bP + 1 + 3 * 4) + ai2 * b.get(bP + 2 + 3 * 4) + ai3 * b.get(bP + 3 + 3 * 4));
        }
    }

    public static float[] multiply(float[] a, float[] b)
    {
        float[] tmp = new float[16];
        multiply(FloatBuffer.wrap(a), FloatBuffer.wrap(b), FloatBuffer.wrap(tmp));
        return tmp;
    }

    public static float[] translate(Vect vec)
    {
        return new float[]{
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                (float) vec.getX(), (float) vec.getY(), (float) vec.getZ(), 1.0f};
    }

    public static float[] translate(float x, float y, float z)
    {
        return new float[]{
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                x, y, z, 1.0f};
    }

    public static float[] identity()
    {
        return new float[]{
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 1.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f};
    }

    public static float[] scale(double x, double y, double z)
    {
        return new float[]{
                (float) x, 0.0f, 0.0f, 0.0f,
                0.0f, (float) y, 0.0f, 0.0f,
                0.0f, 0.0f, (float) z, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f};
    }

    public static float[] scale(double s)
    {
        return scale(s, s, s);
    }

    public static float[] rotate(float a, float x, float y, float z)
    {
        float s, c;
        s = (float) Math.sin(Math.toRadians(a));
        c = (float) Math.cos(Math.toRadians(a));
        return new float[]{
                x * x * (1.0f - c) + c, y * x * (1.0f - c) + z * s, x * z * (1.0f - c) - y * s, 0.0f,
                x * y * (1.0f - c) - z * s, y * y * (1.0f - c) + c, y * z * (1.0f - c) + x * s, 0.0f,
                x * z * (1.0f - c) + y * s, y * z * (1.0f - c) - x * s, z * z * (1.0f - c) + c, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f};
    }

    public static float[] rotate(Matrix matrix)
    {
        int rows = matrix.rows();
        int cols = matrix.cols();

        Global.assume(rows == 3 || rows == 4, "rows must be three or four");
        Global.assume(cols == 3 || cols == 4, "cols must be three or four");

        float[] array = new float[16];
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                array[j * 4 + i] = (float) matrix.get(i, j);
            }
        }
        array[15] = 1;

        return array;
    }

    public static FloatBuffer buffer(float[] array)
    {
        // jdk 9 changed the buffer interface, so we need this goofy cast for backwards compatibility
        return (FloatBuffer) ((Buffer) ByteBuffer.allocateDirect(4 * array.length).order(ByteOrder.nativeOrder()).asFloatBuffer().put(array)).flip();
    }

    public static FloatBuffer buffer(Matrix array)
    {
        // jdk 9 changed the buffer interface, so we need this goofy cast for backwards compatibility
        return (FloatBuffer) ((Buffer) ByteBuffer.allocateDirect(4 * array.rows() * array.cols()).order(ByteOrder.nativeOrder()).asFloatBuffer().put(array.transpose().flatten().toFloatArray())).flip();
    }

    public static FloatBuffer buffer(float value, int count)
    {
        float[] array = new float[count];
        for (int i = 0; i < count; i++)
        {
            array[i] = value;
        }

        return buffer(array);
    }

    public static void composite(GL2 gl, int width, int height)
    {

//                    FloatBuffer depthBuffer = FloatBuffer.allocate(width * height);
//                    gl.glReadPixels(0, 0, width, height, GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, depthBuffer);
//
//                    double minDepth = Double.MAX_VALUE;
//                    double maxDepth = 0;
//                    for (int i = 0; i < width; i++)
//                    {
//                        for (int j = 0; j < height; j++)
//                        {
//                            int idx = j * width + i;
//                            float depth = depthBuffer.get(idx);
//                            maxDepth = Math.max(maxDepth, depth);
//                            minDepth = Math.min(minDepth, depth);
//                        }
//                    }
//
//                    double deltaDepth = maxDepth == minDepth ? 1.0 : (maxDepth - minDepth);

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 3);
        gl.glReadBuffer(GL2.GL_BACK);
        gl.glPixelStorei(GL2.GL_PACK_ALIGNMENT, 1);
        gl.glReadPixels(0, 0, width, height, GL2.GL_RGB, GL2.GL_UNSIGNED_BYTE, buffer);

        int[] pixels = new int[width * height];
        {
            for (int row = 0; row < height; row++)
            {
                for (int col = 0; col < width; col++)
                {
                    int sidx = 3 * (row * width + col);
                    int didx = row * width + col; // (height - 1 - row) * width + col;

                    int iR = buffer.get(sidx + 0);
                    int iG = buffer.get(sidx + 1);
                    int iB = buffer.get(sidx + 2);
                    int iA = 255;

                    pixels[didx] = (iA & 0x000000FF) << 24 | (iR & 0x000000FF) << 16 | (iG & 0x000000FF) << 8 | (iB & 0x000000FF);
                }
            }
        }

        byte[] src = new byte[width * height * 3];

        for (int i = 0; i < width; i++)
        {
            for (int j = 0; j < height; j++)
            {
                int bidx = j * width + i;

                int red = ((0xFF0000 & pixels[bidx]) >> 16);
                int green = ((0x00FF00 & pixels[bidx]) >> 8);
                int blue = ((0x0000FF & pixels[bidx]) >> 0);

                int gray = (int) Math.round((red + green + blue) / 3.0);

                int tidx = 3 * (j * width + i);
                byte br = (byte) (0xFF & red);
                byte bg = (byte) (0xFF & green);
                byte bb = (byte) (0xFF & blue);
                byte value = (byte) (0xFF & gray);
                src[tidx + 0] = br;
                src[tidx + 1] = bg;
                src[tidx + 2] = bb;
            }
        }

        gl.glPixelStorei(GL2.GL_UNPACK_ALIGNMENT, 1);
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_PIXELS, 0);
        gl.glPixelStorei(GL2.GL_UNPACK_SKIP_ROWS, 0);

        gl.glDrawPixels(width, height, GL2.GL_RGB, GL2.GL_UNSIGNED_BYTE, ByteBuffer.wrap(src));
    }
}
