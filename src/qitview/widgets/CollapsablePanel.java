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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.io.File;
import java.util.Observable;
import javax.imageio.ImageIO;
import javax.swing.JPanel;
import qit.base.Global;
import qit.base.structs.ObservableInstance;
import qit.base.utils.PathUtils;
import qitview.main.Viewer;

public class CollapsablePanel extends JPanel
{
    private static Image OPEN = null;
    private static Image CLOSED = null;

    private static int OFFSET = 30;
    private static int PAD = 5;

    private static final long serialVersionUID = 8099395061555810439L;
    private boolean selected = false;
    JPanel content;
    HeaderPanel header;
    ObservableInstance observable = new ObservableInstance();

    static
    {
        String icons = Global.getRoot();
        icons = PathUtils.join(icons, "share");
        icons = PathUtils.join(icons, "gfx");
        icons = PathUtils.join(icons, "icons");
        icons = PathUtils.join(icons, "navigation");

        String open = PathUtils.join(icons, "Down16.gif");
        String closed = PathUtils.join(icons, "Forward16.gif");

        try
        {
            OPEN = ImageIO.read(new File(open));
        }
        catch (Exception e)
        {
            Viewer.getInstance().control.setStatusMessage("warning: failed to read icon: " + open);
        }

        try
        {
            CLOSED = ImageIO.read(new File(closed));
        }
        catch (Exception e)
        {
            Viewer.getInstance().control.setStatusMessage("warning: failed to read icon: " + closed);
        }
    }

    private class HeaderPanel extends JPanel implements MouseListener
    {
        private static final long serialVersionUID = 1139436205008428100L;
        String text;
        Font font;

        public HeaderPanel(String t)
        {
            this.addMouseListener(this);
            this.text = t;

            int size = (int) (1.25 * new BasicLabel().getFont().getSize());
            this.font = new BasicLabel().getFont().deriveFont(size);
            this.setPreferredSize(new Dimension(200, 20));
        }

        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = this.getHeight();
            g2.setFont(this.font);
            FontRenderContext frc = g2.getFontRenderContext();
            LineMetrics lm = this.font.getLineMetrics(this.text, frc);
            float height = lm.getAscent() + lm.getDescent();
            float yBase = (h + height) / 2;
            float yDown = yBase - lm.getDescent();
            float yUp = yBase - height;

            if (CollapsablePanel.this.selected)
            {
                if (OPEN == null)
                {
                    g2.drawString("v", PAD, yDown);
                }
                else
                {
                    g2.drawImage(OPEN, PAD, (int) yUp, null);
                }
            }
            else
            {
                if (CLOSED == null)
                {
                    g2.drawString(">", PAD, yDown);
                }
                else
                {
                    g2.drawImage(CLOSED, PAD, (int) yUp, null);
                }
            }
            g2.drawString(this.text, OFFSET, yDown);
        }

        public void mouseClicked(MouseEvent e)
        {
        }

        public void mouseEntered(MouseEvent e)
        {
        }

        public void mouseExited(MouseEvent e)
        {
        }

        public void mousePressed(MouseEvent e)
        {
        }

        public void mouseReleased(MouseEvent e)
        {
            // note: for some reason mouseClicked doesn't always get called on macOS Sierra, so let's add this here...
            CollapsablePanel.this.toggleSelection();
        }
    }

    public CollapsablePanel(String text, JPanel panel)
    {
        super(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(1, 3, 0, 3);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = GridBagConstraints.REMAINDER;

        this.header = new HeaderPanel(text);

        this.content = panel;

        this.add(this.header, gbc);
        this.add(this.content, gbc);
        this.content.setVisible(false);

        BasicLabel padding = new BasicLabel();
        gbc.weighty = 1.0;
        this.add(padding, gbc);
    }

    public void setSelection(boolean visible)
    {
        if (this.content.isShowing() != visible)
        {
            this.toggleSelection();
        }
    }

    public void toggleSelection()
    {
        this.selected = !this.selected;

        if (this.content.isShowing())
        {
            this.content.setVisible(false);
        }
        else
        {
            this.content.setVisible(true);
        }
        this.validate();
        this.observable.changed();
        this.header.repaint();
    }

    public Observable getObservable()
    {
        return this.observable;
    }

}
