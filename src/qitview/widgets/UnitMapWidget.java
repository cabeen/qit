/*******************************************************************************
 *
 * Quantitative Imaging Toolkit (QIT) (c) 2012-2022 Ryan Cabeen
 * All rights reserved.
 *
 * The Software remains the property of Ryan Cabeen ("the Author").
 *
 * The Software is distributed "AS IS" under this Licence solely for
 * non-commercial use in the hope that it will be useful, but in order
 * that the Author as a charitable foundation protects its assets for
 * the benefit of its educational and research purposes, the Author
 * makes clear that no condition is made or to be implied, nor is any
 * warranty given or to be implied, as to the accuracy of the Software,
 * or that it will be suitable for any particular purpose or for use
 * under any specific conditions. Furthermore, the Author disclaims
 * all responsibility for the use which is made of the Software. It
 * further disclaims any liability for the outcomes arising from using
 * the Software.
 *
 * The Licensee agrees to indemnify the Author and hold the
 * Author harmless from and against any and all claims, damages and
 * liabilities asserted by third parties (including claims for
 * negligence) which arise directly or indirectly from the use of the
 * Software or the sale of any products based on the Software.
 *
 * No part of the Software may be reproduced, modified, transmitted or
 * transferred in any form or by any means, electronic or mechanical,
 * without the express permission of the Author. The permission of
 * the Author is not required if the said reproduction, modification,
 * transmission or transference is done without financial return, the
 * conditions of this Licence are imposed upon the receiver of the
 * product, and all original and amended source code is included in any
 * transmitted product. You may be held legally responsible for any
 * copyright infringement that is caused or encouraged by your failure to
 * abide by these terms and conditions.
 *
 * You are not permitted under this Licence to use this Software
 * commercially. Use for which any financial return is received shall be
 * defined as commercial use, and includes (1) integration of all or part
 * of the source code or the Software into a product for sale or license
 * by or on behalf of Licensee to third parties or (2) use of the
 * Software or any derivative of it for research with the final aim of
 * developing software products for sale or license to a third party or
 * (3) use of the Software or any derivative of it for research with the
 * final aim of developing non-software products for sale or license to a
 * third party, or (4) use of the Software to provide any service to an
 * external organisation for which payment is received.
 *
 ******************************************************************************/


package qitview.widgets;

import com.google.common.collect.Lists;
import qit.base.Global;
import qit.base.structs.Pair;
import qit.data.datasets.Vect;
import qit.data.source.VectSource;
import qit.math.source.VectFunctionSource;
import qit.math.structs.VectFunction;
import qit.math.utils.MathUtils;
import qit.math.utils.colormaps.ColormapSource;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.math.BigDecimal;
import java.util.List;
import java.util.Observable;
import java.util.Stack;

public class UnitMapWidget extends Observable
{
    private static final int POINT_RADIUS = 5;
    private static final int LINE_WIDTH = 3;

    private static final int CANVAS_WIDTH = 350;
    private static final int CANVAS_HEIGHT = 350;
    private static final int CANVAS_LEFT_PAD = 40;
    private static final int CANVAS_RIGHT_PAD = 25;
    private static final int CANVAS_TOP_PAD = 15;
    private static final int CANVAS_BOTTOM_PAD = 25;
    private static final int CANVAS_X_OFFSET = 5;
    private static final int CANVAS_Y_OFFSET = 5;

    private static final int AXIS_WIDTH = 2;

    private double min = 0.0;
    private double max = 1.0;
    private List<Pair<Double, Double>> transfer = Lists.newArrayList();

    private VectFunction coloring = null;

    final BasicTextField minf = new BasicTextField();
    final BasicTextField maxf = new BasicTextField();

    private JPanel panel = new JPanel();
    private ColormapPanel colormap = new ColormapPanel();
    private Integer selected = null;
    private Stack<Integer> added = new Stack<>();

    public UnitMapWidget(double min, double max)
    {
        this();

        Global.assume(min < max, "invalid bounds");

        this.min = min;
        this.max = max;
    }

    public UnitMapWidget()
    {
        this.colormap.addKeyListener(new KeyListener()
        {
            public void keyTyped(KeyEvent e)
            {
            }

            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE)
                {
                    UnitMapWidget.this.pop();
                }
            }

            public void keyReleased(KeyEvent e)
            {
            }
        });

        this.panel.setLayout(new BorderLayout());

        this.reset();
        {
            JPanel control = new JPanel();
            control.add(this.colormap);
            control.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            this.panel.add(control, BorderLayout.CENTER);
        }

        {
            JPanel subpanel = new JPanel();
            subpanel.setLayout(new BorderLayout());

            {
                BasicButton reset = new BasicButton("Reset");
                reset.addActionListener(e -> UnitMapWidget.this.reset());

                BasicButton pop = new BasicButton("Pop");
                pop.addActionListener(e -> UnitMapWidget.this.pop());

                BasicButton invert = new BasicButton("Invert");
                invert.addActionListener(e -> UnitMapWidget.this.invert());

                BasicButton mirror = new BasicButton("Mirror");
                mirror.addActionListener(e -> UnitMapWidget.this.mirror());

                JPanel subsubpanel = new JPanel();
                subsubpanel.setLayout(new BoxLayout(subsubpanel, BoxLayout.LINE_AXIS));
                subsubpanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                subsubpanel.add(reset);
                subsubpanel.add(Box.createRigidArea(new Dimension(5, 0)));
                subsubpanel.add(pop);
                subsubpanel.add(Box.createRigidArea(new Dimension(5, 0)));
                subsubpanel.add(invert);
                subsubpanel.add(Box.createRigidArea(new Dimension(5, 0)));
                subsubpanel.add(mirror);
                subsubpanel.add(Box.createHorizontalGlue());

                subpanel.add(subsubpanel, BorderLayout.NORTH);
            }
            {
                this.minf.setText(String.valueOf(this.min));
                this.maxf.setText(String.valueOf(this.max));

                JPanel subsubpanel = new JPanel();
                subsubpanel.setLayout(new BoxLayout(subsubpanel, BoxLayout.LINE_AXIS));
                subsubpanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
                subsubpanel.add(new BasicLabel("Min:"));
                subsubpanel.add(this.minf);
                subsubpanel.add(Box.createRigidArea(new Dimension(10, 0)));
                subsubpanel.add(new BasicLabel("Max:"));
                subsubpanel.add(this.maxf);
                subsubpanel.add(Box.createHorizontalGlue());

                SwingUtils.addChangeListener(this.minf, e ->
                {
                    String text = UnitMapWidget.this.minf.getText();
                    try
                    {
                        double value = Double.valueOf(text);
                        UnitMapWidget.this.min = value;
                    }
                    catch (NumberFormatException nfe)
                    {
                        UnitMapWidget.this.minf.setText(String.valueOf(UnitMapWidget.this.min));
                    }
                    UnitMapWidget.this.change();
                });

                SwingUtils.addChangeListener(this.maxf, e ->
                {
                    String text = UnitMapWidget.this.maxf.getText();
                    try
                    {
                        double value = Double.valueOf(text);
                        UnitMapWidget.this.max = value;
                    }
                    catch (NumberFormatException nfe)
                    {
                        UnitMapWidget.this.maxf.setText(String.valueOf(UnitMapWidget.this.max));
                    }
                    UnitMapWidget.this.change();
                });

                subpanel.add(subsubpanel, BorderLayout.SOUTH);
            }

            this.panel.add(subpanel, BorderLayout.SOUTH);
        }
    }

    private double filter(double v)
    {
        return MathUtils.valid(v) ? new BigDecimal(v).setScale(3, BigDecimal.ROUND_HALF_EVEN).doubleValue() : 0;
    }

    public void withMin(double v)
    {
        this.min = filter(v);
        this.minf.setText(String.valueOf(this.min));
        this.change();
    }

    public void withMax(double v)
    {
        this.max = filter(v);
        this.maxf.setText(String.valueOf(this.max));

        this.change();
    }

    public void mirror()
    {
        double value = Math.max(Math.abs(this.min), Math.abs(this.max));
        this.withMin(-value);
        this.withMax(value);
    }

    public void withTransfer(List<Pair<Double, Double>> v)
    {
        this.added.clear();
        this.transfer.clear();
        this.transfer.addAll(v);

        this.change();
    }

    public void withColoring(VectFunction coloring)
    {
        this.coloring = coloring;
        this.change();
    }

    public double getMin()
    {
        return this.min;
    }

    public double getMax()
    {
        return this.max;
    }

    public List<Pair<Double, Double>> getTransfer()
    {
        return this.transfer;
    }

    public void set(double min, double max)
    {
        this.min = min;
        this.max = max;
        this.maxf.setText(String.valueOf(this.max));

        this.change();
    }

    public void change()
    {
        this.colormap.repaint();
        this.setChanged();
        this.notifyObservers();
    }

    public JPanel getPanel()
    {
        return this.panel;
    }

    public VectFunction toFunction()
    {
        int num = this.numPoints();
        double[] xs = new double[num];
        Vect[] ys = new Vect[num];
        double delta = this.max - this.min;

        for (int i = 0; i < num; i++)
        {
            double xval = this.min + this.getPointX(i) * delta;
            double yval = this.getPointY(i);

            xs[i] = xval;
            ys[i] = VectSource.create1D(yval);
        }

        VectFunction out = VectFunctionSource.linearInterp(xs, ys);

        return out;
    }

    private void reset()
    {
        this.added.clear();
        this.transfer.clear();
        this.transfer.add(Pair.of(0.0, 0.0));
        this.transfer.add(Pair.of(1.0, 1.0));

        this.change();
    }

    private int numPoints()
    {
        return this.transfer.size();
    }

    private void invert()
    {
        for (Pair<Double, Double> point : this.transfer)
        {
            point.b = 1.0 - point.b;
        }

        this.change();
    }

    private void pop()
    {
        if (!this.added.isEmpty())
        {
            int idx = this.added.pop();
            this.transfer.remove(idx);
            this.change();
        }
    }

    private void addPoint(int idx, double x, double y)
    {
        Global.assume(idx <= this.numPoints() && idx >= 0, "invalid index");

        x = Math.min(1.0, Math.max(0.0, x));
        y = Math.min(1.0, Math.max(0.0, y));

        this.added.push(idx);
        this.transfer.add(idx, Pair.of(x, y));

        this.change();
    }

    private void movePoint(int idx, double x, double y)
    {
        Global.assume(idx <= this.numPoints() && idx >= 0, "invalid index");

        x = Math.min(1.0, Math.max(0.0, x));
        y = Math.min(1.0, Math.max(0.0, y));

        this.transfer.get(idx).a = x;
        this.transfer.get(idx).b = y;

        this.change();
    }

    private double getPointX(int idx)
    {
        double x = this.transfer.get(idx).a;
        return x;
    }

    private double getPointY(int idx)
    {
        double y = this.transfer.get(idx).b;
        return y;
    }

    private class ColormapPanel extends JPanel implements MouseListener, MouseMotionListener
    {
        private static final long serialVersionUID = 8372608460644807584L;

        private int mouseI;
        private int mouseJ;

        public ColormapPanel()
        {
            this.setSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
            this.setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
            this.setMinimumSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));

            this.setBackground(Color.BLACK);
            this.addMouseMotionListener(this);
            this.addMouseListener(this);
        }

        public void paint(Graphics g)
        {
            this.update(g);
        }

        public void update(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g;

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Dimension dim = this.getSize();
            int w = (int) dim.getWidth();
            int h = (int) dim.getHeight();
            g2.setStroke(new BasicStroke(8.0f));

            // draw background
            g2.setPaint(Color.WHITE);
            g2.fillRect(0, 0, w, h);

            if (UnitMapWidget.this.coloring != null)
            {
                int bh = CANVAS_HEIGHT - CANVAS_BOTTOM_PAD - CANVAS_TOP_PAD;
                int bw = CANVAS_WIDTH - CANVAS_RIGHT_PAD - CANVAS_LEFT_PAD;

                for (int j = 0; j < bh; j++)
                {
                    double frac = 1.0 - j / (double) bh;
                    Vect vc = UnitMapWidget.this.coloring.apply(VectSource.create1D(frac));
                    g2.setPaint(ColormapSource.color(vc));
                    g2.fillRect(CANVAS_LEFT_PAD, CANVAS_TOP_PAD + j, bw, 1);
                }
            }

            // draw axes
            g2.setPaint(Color.GRAY);
            g2.setStroke(new BasicStroke(AXIS_WIDTH));
            // x axis
            {
                int i0 = CANVAS_LEFT_PAD;
                int j0 = CANVAS_HEIGHT - CANVAS_BOTTOM_PAD;
                int i1 = CANVAS_WIDTH - CANVAS_RIGHT_PAD;
                int j1 = CANVAS_HEIGHT - CANVAS_BOTTOM_PAD;
                g2.drawLine(i0, j0, i1, j1);
            }
            // y axis
            {
                int i0 = CANVAS_LEFT_PAD;
                int j0 = CANVAS_TOP_PAD;
                int i1 = CANVAS_LEFT_PAD;
                int j1 = CANVAS_HEIGHT - CANVAS_BOTTOM_PAD;
                g2.drawLine(i0, j0, i1, j1);
            }

            // draw points and lines
            for (int idx = 0; idx < UnitMapWidget.this.numPoints(); idx++)
            {
                int i = this.getI(idx);
                int j = this.getJ(idx);

                {
                    g2.setPaint(Color.LIGHT_GRAY);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawLine(i, CANVAS_HEIGHT - CANVAS_BOTTOM_PAD, i, j);
                    g2.drawLine(CANVAS_LEFT_PAD, j, i, j);

                    double xval = UnitMapWidget.this.min + UnitMapWidget.this.getPointX(idx) * (UnitMapWidget.this.max - UnitMapWidget.this.min);
                    double yval = UnitMapWidget.this.getPointY(idx);

                    boolean print = idx == 0 || idx == UnitMapWidget.this.numPoints() - 1;
                    print |= UnitMapWidget.this.selected != null && UnitMapWidget.this.selected == idx;
                    if (print)
                    {
                        g2.setPaint(Color.BLACK);
                        int yoff = idx == 0 ? j - CANVAS_Y_OFFSET : j + CANVAS_Y_OFFSET;
                        g2.drawString(String.format("%.2f", xval), i - CANVAS_X_OFFSET, CANVAS_HEIGHT - CANVAS_X_OFFSET);
                        g2.drawString(String.format("%.2f", yval), 5, yoff);
                    }
                }

                g2.setPaint(Color.WHITE);
                g2.fillOval(i - POINT_RADIUS - 1, j - POINT_RADIUS - 1, 2 * POINT_RADIUS + 3, 2 * POINT_RADIUS + 3);

                g2.setPaint(Color.BLACK);
                g2.fillOval(i - POINT_RADIUS, j - POINT_RADIUS, 2 * POINT_RADIUS + 1, 2 * POINT_RADIUS + 1);


                if (idx == 0)
                {
                    g2.setPaint(Color.WHITE);
                    g2.setStroke(new BasicStroke(LINE_WIDTH + 1));
                    g2.drawLine(0, j, i, j);

                    g2.setPaint(Color.BLACK);
                    g2.setStroke(new BasicStroke(LINE_WIDTH));
                    g2.drawLine(0, j, i, j);
                }
                else
                {
                    int ip = this.getI(idx - 1);
                    int jp = this.getJ(idx - 1);

                    g2.setPaint(Color.WHITE);
                    g2.setStroke(new BasicStroke(LINE_WIDTH + 1));
                    g2.drawLine(ip, jp, i, j);

                    g2.setPaint(Color.BLACK);
                    g2.setStroke(new BasicStroke(LINE_WIDTH));
                    g2.drawLine(ip, jp, i, j);
                }

                if (idx == UnitMapWidget.this.numPoints() - 1)
                {
                    g2.setPaint(Color.WHITE);
                    g2.setStroke(new BasicStroke(LINE_WIDTH + 1));
                    g2.drawLine(i, j, CANVAS_WIDTH, j);

                    g2.setPaint(Color.BLACK);
                    g2.setStroke(new BasicStroke(LINE_WIDTH));
                    g2.drawLine(i, j, CANVAS_WIDTH, j);
                }
            }
        }

        public int mapXtoI(double x)
        {
            int i = (int) Math.round(CANVAS_LEFT_PAD + x * (CANVAS_WIDTH - CANVAS_LEFT_PAD - CANVAS_RIGHT_PAD));
            return i;
        }

        public int mapYtoJ(double y)
        {
            int j = (int) Math.round(CANVAS_TOP_PAD + (1.0 - y) * (CANVAS_HEIGHT - CANVAS_TOP_PAD - CANVAS_BOTTOM_PAD));
            return j;
        }

        public double mapItoX(int i)
        {
            double x = (i - CANVAS_LEFT_PAD) / (double) (CANVAS_WIDTH - CANVAS_LEFT_PAD - CANVAS_RIGHT_PAD);
            return x;
        }

        public double mapJtoY(int j)
        {
            double y = 1.0 - (j - CANVAS_TOP_PAD) / (double) (CANVAS_HEIGHT - CANVAS_TOP_PAD - CANVAS_BOTTOM_PAD);

            return y;
        }

        public int getI(int idx)
        {
            double x = UnitMapWidget.this.transfer.get(idx).a;
            int i = this.mapXtoI(x);

            if (MathUtils.nonzero(x - this.mapItoX(i)))
            {
                throw new RuntimeException("invalid mapping");
            }

            return i;
        }

        public int getJ(int idx)
        {
            double y = UnitMapWidget.this.transfer.get(idx).b;
            int j = this.mapYtoJ(y);

            if (MathUtils.nonzero(y - this.mapJtoY(j)))
            {
                throw new RuntimeException("invalid mapping");
            }

            return j;
        }

        public void addPointCanvas(int idx, int i, int j)
        {
            double x = this.mapItoX(i);
            double y = this.mapJtoY(j);

            if (MathUtils.nonzero(i - this.mapXtoI(x)))
            {
                throw new RuntimeException("invalid mapping");
            }

            if (MathUtils.nonzero(j - this.mapYtoJ(y)))
            {
                throw new RuntimeException("invalid mapping");
            }

            UnitMapWidget.this.addPoint(idx, x, y);
        }

        public void movePointCanvas(int idx, int i, int j)
        {
            double x = this.mapItoX(i);
            double y = this.mapJtoY(j);

            if (MathUtils.nonzero(i - this.mapXtoI(x)))
            {
                throw new RuntimeException("invalid mapping");
            }

            if (MathUtils.nonzero(j - this.mapYtoJ(y)))
            {
                throw new RuntimeException("invalid mapping");
            }

            if (MathUtils.nonzero(x - this.mapItoX(this.mapXtoI(x))))
            {
                throw new RuntimeException("invalid mapping");
            }

            if (MathUtils.nonzero(y - this.mapJtoY(this.mapYtoJ(y))))
            {
                throw new RuntimeException("invalid mapping");
            }

            UnitMapWidget.this.movePoint(idx, x, y);
        }

        public void mousePressed(MouseEvent e)
        {
            this.mouseI = e.getX();
            this.mouseJ = e.getY();

            UnitMapWidget.this.selected = null;

            for (int idx = 0; idx < UnitMapWidget.this.numPoints(); idx++)
            {
                int i = this.getI(idx);
                int j = this.getJ(idx);
                int di = i - this.mouseI;
                int dj = j - this.mouseJ;
                double dist = Math.sqrt(di * di + dj * dj);

                if (dist < POINT_RADIUS)
                {
                    UnitMapWidget.this.selected = idx;
                }
            }

            if (UnitMapWidget.this.selected == null)
            {
                for (int idx = 0; idx < UnitMapWidget.this.numPoints() - 1; idx++)
                {
                    boolean valid = this.mouseI > this.getI(idx) && this.mouseI < this.getI(idx + 1);
                    valid &= this.mouseI >= CANVAS_LEFT_PAD && this.mouseI <= CANVAS_WIDTH - CANVAS_RIGHT_PAD;
                    valid &= this.mouseJ >= CANVAS_TOP_PAD && this.mouseI <= CANVAS_HEIGHT - CANVAS_BOTTOM_PAD;

                    if (valid)
                    {
                        this.addPointCanvas(idx + 1, this.mouseI, this.mouseJ);
                        UnitMapWidget.this.selected = idx + 1;
                        this.repaint();
                        break;
                    }
                }
            }
        }

        public void mouseDragged(MouseEvent e)
        {
            int nmouseI = e.getX();
            int nmouseJ = e.getY();

            if (UnitMapWidget.this.selected != null)
            {
                // move in x
                {
                    int di = nmouseI - this.mouseI;
                    int i = this.getI(UnitMapWidget.this.selected);
                    int ni = i + di;
                    boolean valid = ni >= CANVAS_LEFT_PAD && ni <= CANVAS_WIDTH - CANVAS_RIGHT_PAD;
                    valid &= nmouseI >= CANVAS_LEFT_PAD && nmouseI <= CANVAS_WIDTH - CANVAS_RIGHT_PAD;

                    if (UnitMapWidget.this.selected > 0)
                    {
                        int ti = this.getI(UnitMapWidget.this.selected - 1);
                        valid &= ni > ti;
                        valid &= nmouseI > ti;
                    }

                    if (UnitMapWidget.this.selected < UnitMapWidget.this.numPoints() - 1)
                    {
                        int ti = this.getI(UnitMapWidget.this.selected + 1);
                        valid &= ni < ti;
                        valid &= nmouseI < ti;
                    }

                    if (valid)
                    {
                        this.movePointCanvas(UnitMapWidget.this.selected, ni, this.getJ(UnitMapWidget.this.selected));
                    }
                }

                // move in y
                {
                    int dj = nmouseJ - this.mouseJ;
                    int j = this.getJ(UnitMapWidget.this.selected);
                    int nj = j + dj;
                    boolean valid = nj >= CANVAS_TOP_PAD && nj <= CANVAS_HEIGHT - CANVAS_BOTTOM_PAD;
                    valid &= nmouseJ >= CANVAS_TOP_PAD && nmouseJ <= CANVAS_HEIGHT - CANVAS_BOTTOM_PAD;

                    if (valid)
                    {
                        this.movePointCanvas(UnitMapWidget.this.selected, this.getI(UnitMapWidget.this.selected), nj);
                    }
                }
                this.repaint();
            }

            this.mouseI = nmouseI;
            this.mouseJ = nmouseJ;
        }

        public void mouseReleased(MouseEvent e)
        {
            UnitMapWidget.this.selected = null;
        }

        public void mouseMoved(MouseEvent e)
        {
        }

        public void mouseClicked(MouseEvent e)
        {
        }

        public void mouseExited(MouseEvent e)
        {
        }

        public void mouseEntered(MouseEvent e)
        {
        }
    }
}
