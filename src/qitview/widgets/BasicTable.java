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

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

@SuppressWarnings("serial")
public class BasicTable extends JTable
{
    private static final Color EVEN_ROW_COLOR = new Color(241, 245, 250);
    private static final Color TABLE_GRID_COLOR = new Color(0xd9d9d9);

    private static final CellRendererPane CELL_RENDER_PANE = new CellRendererPane();

    public BasicTable(TableModel dm)
    {
        super(dm);
        this.init();
    }

    private void init()
    {
        // setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.setTableHeader(this.createTableHeader());
        this.getTableHeader().setReorderingAllowed(false);
        this.setOpaque(false);
        this.setGridColor(TABLE_GRID_COLOR);
        this.setIntercellSpacing(new Dimension(0, 0));
        // turn off grid painting as we'll handle this manually in order to
        // paint grid lines over the entire viewport.
        this.setShowGrid(false);
    }

    /**
     * Creates a JTableHeader that paints the table header background to the
     * right of the right-most column if neccesasry.
     */
    private JTableHeader createTableHeader()
    {
        return new JTableHeader(this.getColumnModel())
        {
            private static final long serialVersionUID = 2222398980055519137L;

            @Override
            protected void paintComponent(Graphics g)
            {
                super.paintComponent(g);
                // if this JTableHEader is parented in a JViewport, then paint
                // the
                // table header background to the right of the last column if
                // neccessary.
                JViewport viewport = (JViewport) this.table.getParent();
                if (viewport != null && this.table.getWidth() < viewport.getWidth())
                {
                    int x = this.table.getWidth();
                    int width = viewport.getWidth() - this.table.getWidth();
                    paintHeader(g, this.getTable(), x, width);
                }
            }
        };
    }

    /**
     * Paints the given JTable's table default header background at given x for
     * the given width.
     */
    private static void paintHeader(Graphics g, JTable table, int x, int width)
    {
        TableCellRenderer renderer = table.getTableHeader().getDefaultRenderer();
        Component component = renderer.getTableCellRendererComponent(table, "", false, false, -1, 2);

        component.setBounds(0, 0, width, table.getTableHeader().getHeight());

        ((JComponent) component).setOpaque(false);
        CELL_RENDER_PANE.paintComponent(g, component, null, x, 0, width, table.getTableHeader().getHeight(), true);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
    {
        Component component = super.prepareRenderer(renderer, row, column);
        // if the rendere is a JComponent and the given row isn't part of a
        // selection, make the renderer non-opaque so that striped rows show
        // through.
        if (component instanceof JComponent)
        {
            ((JComponent) component).setOpaque(this.getSelectionModel().isSelectedIndex(row));
        }
        return component;
    }

    // Stripe painting Viewport. //////////////////////////////////////////////

    /**
     * Creates a JViewport that draws a striped backgroud corresponding to the
     * row positions of the given JTable.
     */
    private static class StripedViewport extends JViewport
    {
        private final JTable fTable;

        public StripedViewport(JTable table)
        {
            this.fTable = table;
            this.setOpaque(false);
            this.initListeners();
        }

        private void initListeners()
        {
            // install a listener to cause the whole table to repaint when
            // a column is resized. we do this because the extended grid
            // lines may need to be repainted. this could be cleaned up,
            // but for now, it works fine.
            PropertyChangeListener listener = this.createTableColumnWidthListener();
            for (int i = 0; i < this.fTable.getColumnModel().getColumnCount(); i++)
            {
                this.fTable.getColumnModel().getColumn(i).addPropertyChangeListener(listener);
            }
        }

        private PropertyChangeListener createTableColumnWidthListener()
        {
            return new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent evt)
                {
                    StripedViewport.this.repaint();
                }
            };
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            this.paintStripedBackground(g);
            this.paintVerticalGridLines(g);
            super.paintComponent(g);
        }

        private void paintStripedBackground(Graphics g)
        {
            // build the row index at the top of the clip bounds (the first row
            // to paint).
            int rowAtPoint = this.fTable.rowAtPoint(g.getClipBounds().getLocation());
            // build the y coordinate of the first row to paint. if there are no
            // rows in the table, start painting at the top of the supplied
            // clipping bounds.
            int topY = rowAtPoint < 0 ? g.getClipBounds().y : this.fTable.getCellRect(rowAtPoint, 0, true).y;

            // create a counter variable to hold the current row. if there are
            // no
            // rows in the table, start the counter at 0.
            int currentRow = rowAtPoint < 0 ? 0 : rowAtPoint;
            while (topY < g.getClipBounds().y + g.getClipBounds().height)
            {
                int bottomY = topY + this.fTable.getRowHeight();
                g.setColor(this.getRowColor(currentRow));
                g.fillRect(g.getClipBounds().x, topY, g.getClipBounds().width, bottomY);
                topY = bottomY;
                currentRow++;
            }
        }

        private Color getRowColor(int row)
        {
            return row % 2 == 0 ? EVEN_ROW_COLOR : this.getBackground();
        }

        private void paintVerticalGridLines(Graphics g)
        {
            // paint the column grid dividers for the non-existent rows.
            int x = 0;
            for (int i = 0; i < this.fTable.getColumnCount(); i++)
            {
                TableColumn column = this.fTable.getColumnModel().getColumn(i);
                // increase the x position by the width of the current column.
                x += column.getWidth();
                g.setColor(TABLE_GRID_COLOR);
                // draw the grid line (not sure what the -1 is for, but
                // BasicTableUI
                // also does it.
                g.drawLine(x - 1, g.getClipBounds().y, x - 1, this.getHeight());
            }
        }
    }

    public static JScrollPane createStripedJScrollPane(JTable table)
    {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setViewport(new StripedViewport(table));
        scrollPane.getViewport().setView(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, createCornerComponent(table));
        return scrollPane;
    }

    /**
     * Creates a component that paints the header background for use in a
     * JScrollPane corner.
     */
    private static JComponent createCornerComponent(final JTable table)
    {
        return new JComponent()
        {
            @Override
            protected void paintComponent(Graphics g)
            {
                paintHeader(g, table, 0, this.getWidth());
            }
        };
    }
}
