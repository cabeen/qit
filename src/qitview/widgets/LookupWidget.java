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
import qit.base.structs.ObservableInstance;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Observable;

public class LookupWidget
{
    public static class TableRow
    {
        Color color;
        Double alpha;
        Integer label;
    }

    private static final String[] NAMES = {"label", "alpha", "color"};
    private static final Class[] CLASSES = {Integer.class, Double.class, Color.class};

    public transient ObservableInstance observable = new ObservableInstance();
    private JPanel panel = new JPanel();
    private InnerTableModel model = new InnerTableModel();
    private List<TableRow> rows = Lists.newArrayList();

    public LookupWidget()
    {
        final JTable table = new JTable(LookupWidget.this.model);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        table.setDefaultRenderer(Color.class, new ColorCellRenderer(true));
        table.setDefaultEditor(Color.class, new ColorEditor());
        table.setDefaultEditor(Integer.class, new IntegerEditor(0, 100000));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        BasicButton add = new BasicButton("Add Entry");
        add.addActionListener(e ->
        {
            int max = 0;
            for (TableRow row : rows)
            {
                max = Math.max(max, row.label);
            }

            int r = Global.RANDOM.nextInt(255);
            int g = Global.RANDOM.nextInt(255);
            int b = Global.RANDOM.nextInt(255);
            Color color = new Color(r, g, b);
            double alpha = 1.0;
            int label = max + 1;

            TableRow row = new TableRow();
            row.color = color;
            row.alpha = alpha;
            row.label = label;

            LookupWidget.this.rows.add(row);
            LookupWidget.this.model.fireTableDataChanged();
        });

        BasicButton del = new BasicButton("Remove Entry");
        del.addActionListener(e ->
        {
            int sel = table.getSelectedRow();
            int size = LookupWidget.this.rows.size();

            if (sel < 0 && size == 0)
            {
                return;
            }

            if (sel < 0)
            {
                sel = LookupWidget.this.rows.size() - 1;
            }

            if (sel >= 0)
            {
                LookupWidget.this.rows.remove(sel);
                LookupWidget.this.model.fireTableDataChanged();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.add(add);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(del);
        buttonPanel.add(Box.createHorizontalGlue());

        this.panel.setLayout(new BorderLayout());
        this.panel.add(scroll, BorderLayout.CENTER);
        this.panel.add(buttonPanel, BorderLayout.SOUTH);
    }

    public void clear()
    {
        this.rows.clear();
        LookupWidget.this.model.fireTableDataChanged();
    }

    public void with(Color color, double alpha, int label)
    {
        TableRow row = new TableRow();
        row.color = color;
        row.alpha = alpha;
        row.label = label;

        this.rows.add(row);
        LookupWidget.this.model.fireTableDataChanged();
    }

    public void changed()
    {
        this.model.fireTableDataChanged();
        this.panel.updateUI();
        this.observable.changed();
    }

    public final Observable getObservable()
    {
        return this.observable;
    }

    public JPanel getPanel()
    {
        return this.panel;
    }

    public List<TableRow> getRows()
    {
        return this.rows;
    }

    private class InnerTableModel extends AbstractTableModel
    {
        static final long serialVersionUID = 5929107069446200397L;

        private InnerTableModel()
        {
        }

        public int getColumnCount()
        {
            return NAMES.length;
        }

        public int getRowCount()
        {
            return LookupWidget.this.rows.size();
        }

        public String getColumnName(int col)
        {
            return NAMES[col];
        }

        public Object getValueAt(int row, int col)
        {
            TableRow data = LookupWidget.this.rows.get(row);
            return col == 2 ? data.color : col == 1 ? data.alpha : data.label;
        }

        public Class getColumnClass(int c)
        {
            return CLASSES[c];
        }

        public boolean isCellEditable(int row, int col)
        {
            return true;
        }

        public void setValueAt(Object value, int row, int col)
        {
            TableRow data = LookupWidget.this.rows.get(row);
            if (col == 2)
            {
                data.color = (Color) value;
            }
            else if (col == 1)
            {
                data.alpha = (Double) value;
            }
            else if (col == 0)
            {
                data.label = (Integer) value;
            }

            LookupWidget.this.changed();
        }
    }
}