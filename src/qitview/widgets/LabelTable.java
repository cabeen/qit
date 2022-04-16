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
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Optional;

public class LabelTable
{
    public static class TableRow
    {
        public Integer label;
        public String name;
    }

    private static final String[] NAMES = {"label", "name"};
    private static final Class[] CLASSES = {Integer.class, String.class};

    public transient ObservableInstance observable = new ObservableInstance();
    private JPanel panel = new JPanel();
    private InnerTableModel model = new InnerTableModel();
    private JTable table = new JTable(LabelTable.this.model);
    private List<TableRow> rows = Lists.newArrayList();
    private BasicButton select = new BasicButton("Select");

    public LabelTable()
    {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setDefaultEditor(Integer.class, new IntegerEditor(0, 1000));

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setMinWidth(50);
        columnModel.getColumn(0).setMaxWidth(50);
        columnModel.getColumn(0).setPreferredWidth(50);
        columnModel.getColumn(1).setMinWidth(200);
        columnModel.getColumn(1).setMaxWidth(200);
        columnModel.getColumn(1).setPreferredWidth(200);

        BasicButton add = new BasicButton("Add");
        add.addActionListener(e ->
        {
            int max = 0;
            for (TableRow row : rows)
            {
                max = Math.max(max, row.label);
            }

            int label = max + 1;

            TableRow row = new TableRow();
            row.label = label;
            row.name = "region" + label;

            LabelTable.this.rows.add(row);
            LabelTable.this.model.fireTableDataChanged();
        });

        BasicButton del = new BasicButton("Remove");
        del.addActionListener(e ->
        {
            List<Integer> rows = Lists.newArrayList();
            for (int idx : table.getSelectedRows())
            {
                rows.add(idx);
            }

            Collections.sort(rows);
            Collections.reverse(rows);

            for (int sel : rows)
            {
                int size = LabelTable.this.rows.size();

                if (sel < 0 && size == 0)
                {
                    return;
                }

                if (sel < 0)
                {
                    sel = LabelTable.this.rows.size() - 1;
                }

                if (sel >= 0)
                {
                    LabelTable.this.rows.remove(sel);
                    LabelTable.this.model.fireTableDataChanged();
                }
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPanel.add(add);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(del);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(this.select);
        buttonPanel.add(Box.createHorizontalGlue());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(250, 200));

        this.panel.setLayout(new BorderLayout());
        this.panel.add(scroll, BorderLayout.CENTER);
        this.panel.add(buttonPanel, BorderLayout.SOUTH);
    }

    public Optional<TableRow> getSelectedRow()
    {
        int selection = this.table.getSelectedRow();
        return selection == -1 ? Optional.empty() : Optional.of(this.rows.get(selection));
    }

    public void addSelectActionListener(ActionListener al)
    {
        this.select.addActionListener(al);
    }

    public void clear()
    {
        this.rows.clear();
        LabelTable.this.model.fireTableDataChanged();
    }

    public void with(int mylabel, String myname)
    {
        TableRow row = new TableRow();
        row.label = mylabel;
        row.name = myname;

        this.rows.add(row);
        LabelTable.this.model.fireTableDataChanged();
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
        public int getColumnCount()
        {
            return NAMES.length;
        }

        public int getRowCount()
        {
            return LabelTable.this.rows.size();
        }

        public String getColumnName(int col)
        {
            return NAMES[col];
        }

        public Object getValueAt(int row, int col)
        {
            TableRow data = LabelTable.this.rows.get(row);
            return col == 1 ? data.name: data.label;
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
            TableRow data = LabelTable.this.rows.get(row);
            if (col == 1)
            {
                data.name = (String) value;
            }
            else if (col == 0)
            {
                data.label = (Integer) value;
            }

            LabelTable.this.changed();
        }
    }
}

