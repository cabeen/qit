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


package qitview.panels;

import com.google.common.collect.Lists;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;
import qit.base.Logging;
import qitview.main.Constants;
import qitview.main.Interface;
import qitview.main.Viewer;
import qitview.widgets.BasicTable;
import qitview.widgets.RowTableModel;
import qitview.widgets.SwingUtils;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.text.DefaultCaret;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class Processes
{
    public enum ProcessState
    {
        Complete, Running, Canceled, Failed, Waiting
    }

    private ProcessTableModel model;
    private BasicTable table;
    private JFrame window = null;
    private boolean persist = false;

    public Processes()
    {
        this.model = new ProcessTableModel();
        this.table = new BasicTable(this.model);

        this.table.setFocusable(false);
        this.table.getColumnModel().getColumn(0).setMinWidth(100);
        this.table.getColumnModel().getColumn(1).setMinWidth(250);
        this.table.getColumnModel().getColumn(2).setMinWidth(100);
        this.table.setCellSelectionEnabled(true);
        this.table.setRowSelectionAllowed(true);
        this.table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        this.table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        this.table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        SwingUtilities.invokeLater(() ->
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JScrollPane scroll = BasicTable.createStripedJScrollPane(Processes.this.table);
            panel.add(scroll);

            {
                JPopupMenu menu = new JPopupMenu();
                SwingUtils.addMenu(menu, "Cancel Selected Running Processes", e -> this.cancelSelection());
                SwingUtils.addMenu(menu, "Clear Selected Ended Processes", e -> this.clearSelection());
                SwingUtils.addMenu(menu, "Cancel All Running Processes", e -> this.cancelAll());
                SwingUtils.addMenu(menu, "Clear All Ended Processes", e -> this.clearAll());
                this.table.setComponentPopupMenu(menu);
            }

            Timer timer = new Timer(0, e ->
            {
                this.table.updateUI();
                this.table.repaint();

                boolean done = true;
                for (ProcessRecord r : this.model)
                {
                    r.update();

                    if (r.state.equals(ProcessState.Running))
                    {
                        done = false;
                    }
                }

                if (done && !this.persist)
                {
                    this.setVisible(false);
                }
            });

            timer.setDelay(500); // delay for 30 seconds
            timer.start();

            JTextArea mtext = new JTextArea();
            DefaultCaret caret = (DefaultCaret) mtext.getCaret();
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            Logging.LOGGER.addAppender(new JTextAreaAppender(mtext));

            JScrollPane messages = new JScrollPane(mtext);
            messages.setMinimumSize(new Dimension(Constants.MSG_WIDTH, Constants.MSG_HEIGHT));
            messages.setPreferredSize(new Dimension(Constants.MSG_WIDTH, Constants.MSG_HEIGHT));

            JFrame frame = Viewer.getInstance().gui.getFrame();
            this.window = new JFrame(Constants.SETTINGS_PROCESSES);

            JTabbedPane tabs = new JTabbedPane();
            tabs.addTab("Processes", panel);
            tabs.addTab("Messages", messages);

            this.window.add(tabs);

            this.window.pack();
            this.window.setPreferredSize(new Dimension(600, 500));
            this.window.pack();
            this.window.setLocationRelativeTo(frame);
            this.window.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
            this.window.setResizable(true);
            this.window.setVisible(false);

            this.window.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    Processes.this.persist = false;
                }
            });

            SwingUtils.addEscapeListener(this.window);
        });
    }

    public void request()
    {
        this.persist = true;
        this.setVisible(true);
    }

    public void show()
    {
        this.setVisible(true);
    }

    public void setVisible(boolean v)
    {
        if (this.window != null)
        {
            this.window.setVisible(v);
        }
    }

    public List<Integer> getSelectionIndex()
    {
        List<Integer> out = Lists.newArrayList();
        for (int idx : this.table.getSelectedRows())
        {
            out.add(this.table.convertRowIndexToModel(idx));
        }

        return out;
    }

    public void cancelSelection()
    {
        for (Integer i : this.getSelectionIndex())
        {
            ProcessRecord r = this.model.getRow(i);
            if (r.state.equals(ProcessState.Running))
            {
                r.cancel();
            }
        }
    }

    public void cancelAll()
    {
        List<Integer> idx = Lists.newArrayList();

        for (int i = 0; i < this.model.getRowCount(); i++)
        {
            ProcessRecord r = this.model.getRow(i);
            if (r.state.equals(ProcessState.Running))
            {
                r.cancel();
            }
        }

        this.model.removeRows(idx);
    }

    public void clearSelection()
    {
        List<Integer> idx = Lists.newArrayList();

        for (Integer i : this.getSelectionIndex())
        {
            ProcessRecord r = this.model.getRow(i);
            if (!r.state.equals(ProcessState.Running))
            {
                idx.add(i);
            }
        }

        this.model.removeRows(idx);
    }

    public void clearAll()
    {
        List<Integer> idx = Lists.newArrayList();

        for (int i = 0; i < this.model.getRowCount(); i++)
        {
            ProcessRecord r = this.model.getRow(i);
            if (!r.state.equals(ProcessState.Running))
            {
                idx.add(i);
            }
        }

        this.model.removeRows(idx);
    }

    public int size()
    {
        return this.model.getRowCount();
    }

    public void add(String message, Runnable run)
    {
        this.model.insertRow(0, new ProcessRecord(message, run));
    }

    private class ProcessTableModel extends RowTableModel<ProcessRecord>
    {
        ProcessTableModel()
        {
            super(Arrays.asList(new String[]{"Status", "Description", "Elapsed"}));
            setRowClass(ProcessRecord.class);
            setColumnClass(0, String.class);
            setColumnClass(1, String.class);
            setColumnClass(2, String.class);
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            //all cells false
            return false;
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            ProcessRecord record = getRow(row);

            switch (column)
            {
                case 0:
                    return record.getState();
                case 1:
                    return record.getMessage();
                case 2:
                    return record.getElapsed();
                default:
                    return null;
            }
        }
    }

    public class ProcessRecord
    {
        private String message = "";
        private ProcessState state = ProcessState.Waiting;
        private long creation = System.currentTimeMillis();
        private long start = 0;
        private long last = 0;
        private Thread thread = null;

        public ProcessRecord(String message, Runnable run)
        {
            this.message = message;
            this.thread = new Thread(() ->
            {
                try
                {
                    Processes.this.setVisible(true);

                    ProcessRecord.this.start = System.currentTimeMillis();
                    ProcessRecord.this.state = ProcessState.Running;
                    Thread.sleep(10);
                    run.run();
                }
                catch (RuntimeException e)
                {
                    ProcessRecord.this.state = ProcessState.Failed;
                    String msg = "Warning, this process failed.  Please check the console for more information";
                    Viewer.getInstance().control.setStatusMessage(msg);
                    SwingUtils.showMessage(msg);
                    e.printStackTrace();
                }
                catch (InterruptedException e)
                {
                    ProcessRecord.this.state = ProcessState.Failed;
                    e.printStackTrace();
                }
                finally
                {
                    if (ProcessRecord.this.state.equals(ProcessState.Running))
                    {
                        // this is required to handle cancellation
                        ProcessRecord.this.state = ProcessState.Complete;
                    }
                }
            });

            this.thread.start();
        }

        public void cancel()
        {
            if (this.thread.isAlive())
            {
                ProcessRecord.this.state = ProcessState.Canceled;
                thread.stop();
            }
        }

        public String getState()
        {
            return this.state.toString();
        }

        public String getMessage()
        {
            return this.message;
        }

        public void update()
        {
            // assume this gets called very frequenty, so this is a reasonable way to keep track of timing
            if (this.state.equals(ProcessState.Running))
            {
                this.last = (System.currentTimeMillis() - this.start);
            }
        }

        public String getElapsed()
        {
            // assume this gets called very frequenty, so this is a reasonable way to keep track of timing
            if (this.state.equals(ProcessState.Running))
            {
                this.last = (System.currentTimeMillis() - this.start);
            }

            return SwingUtils.time(this.last);
        }

        public String toString()
        {
            return this.message;
        }

        public int hashCode()
        {
            return this.message.hashCode() + new Long(this.creation).hashCode();
        }

        public boolean equals(Object obj)
        {
            if (obj == this)
            {
                return true;
            }

            if (!(obj instanceof ProcessRecord))
            {
                return false;
            }

            ProcessRecord v = (ProcessRecord) obj;
            return v.message == this.message && v.creation == this.creation;
        }
    }

    public class JTextAreaAppender extends AppenderSkeleton
    {
        JTextArea textarea;
        PatternLayout layout = new PatternLayout(Logging.LOG_PATTERN);

        public JTextAreaAppender(JTextArea textArea)
        {
            this.textarea = textArea;
        }

        public JTextArea getTextArea()
        {
            return this.textarea;
        }

        public void append(LoggingEvent loggingEvent)
        {
            this.textarea.append(this.layout.format(loggingEvent));
        }

        public boolean requiresLayout()
        {
            return false;
        }

        public void close()
        {
        }
    }
}
