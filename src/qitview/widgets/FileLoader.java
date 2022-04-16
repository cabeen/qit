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
import com.google.common.collect.Maps;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.TableColumn;

import net.iharder.dnd.FileDrop;
import qit.base.Logging;
import qit.base.utils.JsonUtils;
import qit.base.utils.PathUtils;
import qitview.models.Viewable;
import qitview.main.Viewer;
import qitview.models.ViewableType;

public class FileLoader
{
    public FileLoader()
    {
        this(null, Lists.newArrayList());
    }

    public FileLoader(List<String> fns)
    {
        this(null, fns);
    }

    public FileLoader(JFrame parent, List<String> fns)
    {
        if (parent == null)
        {
            parent = Viewer.getInstance().gui.getFrame();
        }

        final FileLoadTableModel model = new FileLoadTableModel();
        final BasicTable table = new BasicTable(model);

        table.getColumnModel().getColumn(0).setMaxWidth(200);
        table.getColumnModel().getColumn(1).setMaxWidth(Integer.MAX_VALUE);

        table.setFocusable(false);
        table.setRowSelectionAllowed(true);
        table.setPreferredScrollableViewportSize(new Dimension(800, 200));
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(BasicTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setShowGrid(true);

        for (String fn : fns)
        {
            model.addRow(new FileLoadRecord(ViewableType.guess(fn), fn));
        }

        {
            JPopupMenu menu = new JPopupMenu();
            {
                JMenuItem clearSelection = new JMenuItem("Clear Selection");
                clearSelection.addActionListener(e ->
                {
                    List<Integer> selection = Lists.newArrayList();
                    for (int idx : table.getSelectedRows())
                    {
                        selection.add(table.convertRowIndexToModel(idx));
                    }

                    Collections.sort(selection);
                    Collections.reverse(selection);
                    for (Integer idx : selection)
                    {
                        model.removeRowRange(idx, idx);
                    }
                });

                menu.add(clearSelection);
            }
            {
                JMenuItem clearAll = new JMenuItem("Clear All");
                clearAll.addActionListener(e -> model.removeRowAll());

                menu.add(clearAll);
            }
            menu.addSeparator();

            for (final ViewableType type : ViewableType.values())
            {
                JMenuItem clearAll = new JMenuItem("Set to " + type.getText());
                clearAll.addActionListener(e ->
                {
                    List<Integer> selection = Lists.newArrayList();
                    for (int idx : table.getSelectedRows())
                    {
                        selection.add(table.convertRowIndexToModel(idx));
                    }

                    for (Integer idx : selection)
                    {
                        model.getRow(idx).type = type;
                    }

                    model.fireTableDataChanged();
                });

                menu.add(clearAll);
            }

            table.setComponentPopupMenu(menu);
        }

        new FileDrop(table, files ->
        {
            for (File file : files)
            {
                String fn = file.getAbsolutePath();
                if (PathUtils.exists(fn))
                {
                    model.addRow(new FileLoadRecord(ViewableType.guess(fn), fn));
                }
                else
                {
                    Logging.info("skipping: " + fn);
                }
            }
        });

        BasicComboBox<ViewableType> typeCombo = new BasicComboBox<>(ViewableType.values());
        TableColumn column = table.getColumnModel().getColumn(0);
        column.setCellEditor(new DefaultCellEditor(typeCombo));

        final JDialog dialog = new JDialog(parent);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        panel.add(BasicTable.createStripedJScrollPane(table));

        {
            BasicButton help = new BasicButton("Help");
            help.setToolTipText("show some information about file loading");
            help.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    StringBuilder msg = new StringBuilder();
                    msg.append("This window allows you to load a collection of files\n");
                    msg.append("into the viewer.  You must also specify the file types\n");
                    msg.append("of the files before loading them.  You can add files to\n");
                    msg.append("the list using the Add button, and then load them using\n");
                    msg.append("the Load button.\n");

                    SwingUtils.showMessage(msg.toString());
                }
            });

            BasicButton cancel = new BasicButton("Cancel");
            cancel.setToolTipText("close this window and do not load anything");
            cancel.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    dialog.dispose();
                    return;
                }
            });

            BasicButton add = new BasicButton("Add more files");
            add.setToolTipText("add files to the list in this window");
            add.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent e)
                {
                    List<String> fns = Viewer.getInstance().gui.chooseLoadFiles("Choose files to add", dialog);
                    if (fns != null)
                    {
                        for (String fn : fns)
                        {
                            if (PathUtils.exists(fn))
                            {
                                model.addRow(new FileLoadRecord(ViewableType.guess(fn), fn));
                            }
                            else
                            {
                                Logging.info("skipping: " + fn);
                            }
                        }
                    }
                }
            });


            BasicButton load = new BasicButton("Load files into workspace");
            load.setToolTipText("load the listed files");
            load.addActionListener(e ->
            {
                Map<ViewableType, List<String>> fns1 = Maps.newLinkedHashMap();
                for (int i = 0; i < model.getRowCount(); i++)
                {
                    FileLoadRecord row = model.getRow(i);

                    if (!fns1.containsKey(row.type))
                    {
                        fns1.put(row.type, Lists.newArrayList());
                    }

                    fns1.get(row.type).add(row.filename);
                }

                for (ViewableType type : fns1.keySet())
                {
                    Viewer.getInstance().control.readQueue(type, fns1.get(type), Viewer.FULLPATH);
                }

                dialog.dispose();
                return;
            });

            {
                JPanel subpanel = new JPanel();
                subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));
                subpanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                subpanel.add(help);
                subpanel.add(Box.createHorizontalGlue());
                subpanel.add(cancel);
                subpanel.add(Box.createRigidArea(new Dimension(5, 0)));
                subpanel.add(add);
                subpanel.add(Box.createRigidArea(new Dimension(5, 0)));
                subpanel.add(load);

                panel.add(subpanel);
            }

            dialog.getRootPane().setDefaultButton(load);
        }

        dialog.add(panel);
        dialog.setTitle("Load files...");
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setResizable(true);
        dialog.setVisible(true);
        dialog.setMinimumSize(new Dimension(500, 200));
        SwingUtils.addEscapeListener(dialog);
    }

    private class FileLoadTableModel extends RowTableModel<FileLoadRecord>
    {
        FileLoadTableModel()
        {
            super(Arrays.asList(new String[]{"Type", "Name",}));
            setRowClass(FileLoadRecord.class);
            setColumnClass(0, ViewableType.class);
            setColumnClass(1, String.class);
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            FileLoadRecord record = getRow(row);

            switch (column)
            {
                case 0:
                    return record.type;
                case 1:
                    return record.filename;
                default:
                    return null;
            }
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            FileLoadRecord record = getRow(row);

            switch (column)
            {
                case 0:
                    record.type = (ViewableType) value;
                    break;
                case 1:
                    record.filename = (String) value;
                    break;
            }

            fireTableCellUpdated(row, column);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return true;
        }
    }

    public static class FileLoadRecord
    {
        public ViewableType type = ViewableType.Volume;
        public String filename = "";

        public FileLoadRecord(ViewableType t, String f)
        {
            this.type = t;
            this.filename = f;
        }

        public FileLoadRecord withType(ViewableType v)
        {
            this.type = v;
            return this;
        }

        public FileLoadRecord withFilename(String v)
        {
            this.filename = v;
            return this;
        }

        public String toString()
        {
            return JsonUtils.encode(this);
        }

        public int hashCode()
        {
            return this.filename.hashCode();
        }

        public boolean equals(Object obj)
        {
            if (obj == this)
            {
                return true;
            }

            if (!(obj instanceof Viewable))
            {
                return false;
            }

            FileLoadRecord v = (FileLoadRecord) obj;
            return v.type.equals(this.type) && v.filename.equals(this.filename);
        }
    }
}
