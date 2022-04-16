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

package qitview.views;

import com.google.common.collect.Maps;
import com.jogamp.opengl.GL2;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

import qit.data.datasets.Table;
import qitview.main.Viewer;
import qitview.widgets.BasicButton;
import qitview.widgets.BasicLabel;
import qitview.widgets.BasicTable;
import qitview.widgets.ControlPanel;

public class TableView extends AbstractView<Table>
{
    private transient JDialog editor = null;
    private transient List<Integer> rowKeys;

    public TableView()
    {
        super();
        super.initPanel();
        this.initEditor();
    }

    private void initEditor()
    {
        final InnerTableModel model = new InnerTableModel();
        final BasicTable table = new BasicTable(model);
        table.setAutoResizeMode(BasicTable.AUTO_RESIZE_LAST_COLUMN);

        this.observable.addObserver((o, arg) ->
        {
            model.fireTableStructureChanged();
            model.fireTableDataChanged();
            table.updateUI();
        });

        JScrollPane scroll = BasicTable.createStripedJScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel edit = new JPanel();
        edit.setLayout(new BorderLayout());
        edit.add(scroll, BorderLayout.CENTER);

        BasicButton add = new BasicButton("Add Row");
        add.addActionListener(e12 ->
        {
            if (this.hasData())
            {
                Table ndata = this.data.copy();
                ndata.addRecord(this.data.getNumRecords());
                TableView.this.setData(ndata);
                model.fireTableDataChanged();
            }
        });

        BasicButton del = new BasicButton("Remove Row");
        del.addActionListener(e1 ->
        {
            if (this.hasData())
            {
                int sel = table.getSelectedRow();
                Table ndata = TableView.this.data.copy();
                ndata.remove(TableView.this.rowKeys.get(sel));
                TableView.this.setData(ndata);
                model.fireTableDataChanged();
            }
        });

        BasicButton close = new BasicButton("Close Editor");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        buttonPanel.add(add);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(del);
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(close);

        edit.add(buttonPanel, BorderLayout.SOUTH);

        this.editor = new JDialog(Viewer.getInstance().gui.getFrame(), "Table Editor");
        this.editor.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        this.editor.add(edit, BorderLayout.CENTER);
        this.editor.setSize(300, 300);
        this.editor.pack();
        this.editor.setVisible(false);
        close.addActionListener(e13 -> this.editor.hide());
    }

    @Override
    public Table getData()
    {
        return this.data;
    }

    public TableView setData(Table d)
    {
        this.bounds = null;

        if (d != null)
        {
            this.rowKeys = d.getKeys();
        }

        super.setData(d);

        return this;
    }

    protected ControlPanel makeInfoControls()
    {
        ControlPanel infoPanel = new ControlPanel();
        infoPanel.addControl("Type: ", new BasicLabel(this.toString()));
        infoPanel.addControl(" ", new BasicLabel());
        {
            final BasicLabel label = new BasicLabel("");
            infoPanel.addControl("Columns: ", label);
            this.observable.addObserver((o, arg) ->
            {
                if (TableView.this.hasData())
                {
                    label.setText(String.valueOf(TableView.this.data.getNumFields()));
                }
                else
                {
                    label.setText("NA");
                }
            });
        }
        {
            final BasicLabel label = new BasicLabel("");
            infoPanel.addControl("Rows: ", label);
            this.observable.addObserver((o, arg) ->
            {
                if (TableView.this.hasData())
                {
                    label.setText(String.valueOf(TableView.this.data.getNumRecords()));
                }
                else
                {
                    label.setText("NA");
                }
            });
        }
        {
            BasicButton elem = new BasicButton("Edit");
            elem.setToolTipText("open a window for viewing and editing the table");
            elem.addActionListener(e -> this.showEditor());
            infoPanel.addControl(elem);
        }

        return infoPanel;
    }

    protected Map<String, ControlPanel> makeRenderControls()
    {
        Map<String, ControlPanel> controls = Maps.newLinkedHashMap();

        ControlPanel infoPanel = new ControlPanel();
        {
            BasicButton elem = new BasicButton("Open Editor");
            elem.setToolTipText("open a window for viewing and editing the table");
            elem.addActionListener(e -> showEditor());
            infoPanel.addControl(elem);
        }

        controls.put("View", infoPanel);

        return controls;
    }

    public void showEditor()
    {
        this.editor.show();
    }

    private class InnerTableModel extends AbstractTableModel
    {
        static final long serialVersionUID = 5929107069446200397L;

        private InnerTableModel()
        {
        }

        public int getColumnCount()
        {
            return TableView.this.hasData() ? TableView.this.data.getNumFields() : 0;
        }

        public int getRowCount()
        {
            return TableView.this.hasData() ? TableView.this.data.getNumRecords() : 0;
        }

        public String getColumnName(int col)
        {
            return TableView.this.data.getFieldName(col);
        }

        public Object getValueAt(int row, int col)
        {
            return TableView.this.data.get(TableView.this.rowKeys.get(row), col);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public Class getColumnClass(int c)
        {
            return TableView.this.data.getClass();
        }

        public boolean isCellEditable(int row, int col)
        {
            return true;
        }

        public void setValueAt(String value, int row, int col)
        {
            Integer key = TableView.this.rowKeys.get(row);
            String field = TableView.this.data.getField(col);
            TableView.this.data.set(key, field, value);
            TableView.this.touchData();
        }
    }
}