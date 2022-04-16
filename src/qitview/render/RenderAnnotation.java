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
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.nio.ByteBuffer;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.structs.VectFunction;
import qit.math.utils.colormaps.ColormapScalar;
import qitview.widgets.BasicComboBox;
import qitview.widgets.BasicSpinner;
import qitview.widgets.BasicTextField;
import qitview.widgets.ColorWidget;
import qitview.widgets.ControlPanel;

public class RenderAnnotation
{
    public static final String DEFAULT_FONT = "Helvetica";
    public static final int DEFAULT_POINT = 20;
    public static final int DEFAULT_MINOR = 2;
    public static final int DEFAULT_MAJOR = 5;
    public static final int DEFAULT_WIDTH = 20;
    public static final int DEFAULT_HEIGHT = 300;
    public static final boolean DEFAULT_ALIAS = true;
    public static final boolean DEFAULT_ITALICS = false;
    public static final String DEFAULT_LABEL = "Attribute";

    private JPanel panel = new JPanel();
    private BasicComboBox<String> fontCombo = new BasicComboBox<>();
    private BasicSpinner pointSpin = new BasicSpinner(new SpinnerNumberModel(DEFAULT_POINT, 4, 100, 1));
    private ColorWidget colorChooser = new ColorWidget();
    private JCheckBox boldCheck = new JCheckBox();
    private JCheckBox italicsCheck = new JCheckBox();

    private BasicComboBox<ColormapScalar> colormapCombo = null;
    private JCheckBox colormapCheck = new JCheckBox();
    private BasicTextField labelField = new BasicTextField(DEFAULT_LABEL);
    private JCheckBox aliasCheck = new JCheckBox();
    private BasicSpinner minorSpin = new BasicSpinner(new SpinnerNumberModel(DEFAULT_MINOR, 1, 10, 1));
    private BasicSpinner majorSpin = new BasicSpinner(new SpinnerNumberModel(DEFAULT_MAJOR, 3, 10, 1));
    private BasicSpinner widthSpin = new BasicSpinner(new SpinnerNumberModel(DEFAULT_WIDTH, 4, 500, 1));
    private BasicSpinner heightSpin = new BasicSpinner(new SpinnerNumberModel(DEFAULT_HEIGHT, 4, 500, 1));

    public RenderAnnotation(BasicComboBox<ColormapScalar> cmc)
    {
        this.colormapCombo = cmc;

        java.util.List<String> fonts = Lists.newArrayList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
        {
            this.fontCombo.addItem(f);
        }
        if (fonts.contains(DEFAULT_FONT))
        {
            this.fontCombo.setSelectedItem(DEFAULT_FONT);
        }
        this.aliasCheck.setSelected(DEFAULT_ALIAS);
        this.italicsCheck.setSelected(DEFAULT_ITALICS);
        this.colormapCombo.setSelectedIndex(0);

        ControlPanel controls = new ControlPanel();
        controls.addControl("Visible", this.colormapCheck);
        controls.addControl("Colormap", this.colormapCombo);
        controls.addControl("Label", this.labelField);
        controls.addControl("Font", this.fontCombo);
        controls.addControl("Point", this.pointSpin);
        controls.addControl("Bold", this.boldCheck);
        controls.addControl("Italics", this.italicsCheck);
        controls.addControl("Anti-alias", this.aliasCheck);
        controls.addControl("Color", this.colorChooser.getPanel());
        controls.addControl("Minor Ticks", this.minorSpin);
        controls.addControl("Major Ticks", this.majorSpin);
        controls.addControl("Box Width", this.widthSpin);
        controls.addControl("Box Height", this.heightSpin);

        this.panel.add(controls);
    }

    public String getFont()
    {
        return (String) this.fontCombo.getSelectedItem();
    }

    public int getPoint()
    {
        return Integer.valueOf(this.pointSpin.getModel().getValue().toString());
    }

    public int getMinor()
    {
        return Integer.valueOf(this.minorSpin.getModel().getValue().toString());
    }

    public int getMajor()
    {
        return Integer.valueOf(this.majorSpin.getModel().getValue().toString());
    }

    public int getWidth()
    {
        return Integer.valueOf(this.widthSpin.getModel().getValue().toString());
    }

    public int getHeight()
    {
        return Integer.valueOf(this.heightSpin.getModel().getValue().toString());
    }

    public Color getColor()
    {
        return this.colorChooser.getColor();
    }

    public boolean getVisibility()
    {
        return this.colormapCheck.isSelected();
    }

    public boolean getBold()
    {
        return this.boldCheck.isSelected();
    }

    public boolean getItalics()
    {
        return this.italicsCheck.isSelected();
    }

    public boolean getAlias()
    {
        return this.aliasCheck.isSelected();
    }

    public ColormapScalar getColormap()
    {
        return (ColormapScalar) this.colormapCombo.getSelectedItem();
    }

    public String getLabel()
    {
        return this.labelField.getText();
    }

    public JPanel getPanel()
    {
        return this.panel;
    }

    public void render(GLAutoDrawable drawable)
    {
        ColormapScalar cm = this.getColormap();

        if (this.getVisibility() && cm != null)
        {
            Color color = this.getColor();
            int width = this.getWidth();
            int height = this.getHeight();
            int minor = this.getMinor();
            int major = this.getMajor();

            double min = cm.getMin();
            double max = cm.getMax();
            double delta = max - min;
            VectFunction function = cm.getFunction();
            ByteBuffer colors = ByteBuffer.allocateDirect(width * height * 4);
            for (int i = 0; i < width; i++)
            {
                for (int j = 0; j < height; j++)
                {
                    double unit = j / (double) (height - 1);
                    double value = unit * delta + min;
                    Vect vect = function.apply(VectSource.create1D(value));
                    float vr = (float) vect.get(0);
                    float vg = (float) vect.get(1);
                    float vb = (float) vect.get(2);
                    Color colorv = new Color(vr, vg, vb);
                    byte r = (byte) colorv.getRed();
                    byte g = (byte) colorv.getGreen();
                    byte b = (byte) colorv.getBlue();

                    int idx = 3 * width * j + 3 * i;
                    colors.put(idx + 0, r);
                    colors.put(idx + 1, g);
                    colors.put(idx + 2, b);
                }
            }

            ByteBuffer ticks = ByteBuffer.allocateDirect(width * 4);
            for (int i = 0; i < width; i++)
            {
                int idx = 3 * i;
                ticks.put(idx + 0, (byte) color.getRed());
                ticks.put(idx + 1, (byte) color.getGreen());
                ticks.put(idx + 2, (byte) color.getBlue());
            }

            int posx = width;
            int posy = (drawable.getSurfaceHeight() - height) / 2;

            GL2 gl = drawable.getGL().getGL2();
            gl.glWindowPos2d(posx, posy);
            gl.glDrawPixels(width, height, gl.GL_RGB, gl.GL_UNSIGNED_BYTE, colors);

            int dmajor = (height - 1) / (major - 1);
            int dminor = dmajor / (minor + 1);
            for (int i = 0; i < major; i++)
            {
                gl.glWindowPos2d(posx + width, posy + i * dmajor);
                gl.glDrawPixels(width / 4, 1, gl.GL_RGB, gl.GL_UNSIGNED_BYTE, ticks);

                if (i < major - 1)
                {
                    for (int j = 1; j <= minor; j++)
                    {
                        gl.glWindowPos2d(posx + width, posy + i * dmajor + j * dminor);
                        gl.glDrawPixels(width / 8, 1, gl.GL_RGB, gl.GL_UNSIGNED_BYTE, ticks);
                    }
                }
            }

            int point = this.getPoint();
            int style = Font.PLAIN;
            if (this.getBold())
            {
                style |= Font.BOLD;
            }
            if (this.getItalics())
            {
                style |= Font.ITALIC;
            }

            Font font = new Font(this.getFont(), style, point);
            float red = color.getRed() / (float) 255;
            float green = color.getGreen() / (float) 255;
            float blue = color.getBlue() / (float) 255;
            String label = this.getLabel();

            TextRenderer texter = new TextRenderer(font, this.getAlias(), false);
            texter.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
            texter.setColor(red, green, blue, 0.8f);
            texter.draw(label, (int) (0.5 * width), (int) (posy - 1.5 * point));

            for (int i = 0; i < major; i++)
            {
                double unit = i / (double) (major - 1);
                double value = unit * delta + min;
                String text = String.format("%.3g", value);
                int tx = posx + width + (int) (0.35 * width);
                int ty = posy + i * dmajor - (int) (0.25 * point);
                texter.draw(text, tx, ty);
            }

            texter.endRendering();
        }
    }
}