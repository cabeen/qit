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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import qit.base.Logging;
import qit.base.structs.Pointer;
import qitview.main.Viewer;

public class SwingUtils
{
    public static void addMenu(JPopupMenu menu, String name, ActionListener listener)
    {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(listener);
        menu.add(item);
    }

    public static void addMenu(JMenu menu, String name, ActionListener listener)
    {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(listener);
        menu.add(item);
    }

    public static void safeStatus(final String msg)
    {
        // this method is for messaging from non-GUI threads
        SwingUtils.invokeAndWait(() -> Viewer.getInstance().control.setStatusMessage(msg));
    }

    public static void safeMessage(final String msg)
    {
        // this method is for messaging from non-GUI threads
        SwingUtils.invokeAndWait(() ->
        {
            SwingUtils.showMessage(msg);
        });
    }

    public static void invokeAndWait(Runnable run)
    {
        try
        {
            SwingUtilities.invokeAndWait(run);
        }
        catch (InterruptedException | InvocationTargetException e)
        {
            Logging.info("warning: failed to run thread.  see stack trace for debugging information");
            e.printStackTrace();
        }
    }

    public static void runInProgressWindow(String title, String message, Runnable run)
    {
        SwingUtilities.invokeLater(() ->
        {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            {
                JProgressBar progressBar = new JProgressBar();
                progressBar.setIndeterminate(true);
                progressBar.setBorder(BorderFactory.createTitledBorder(message));
                panel.add(progressBar);
            }

            final BasicLabel elapsed = new BasicLabel("Elapsed time: 00:00:00");
            final BasicButton cancel = new BasicButton("Cancel");

            {
                JPanel subpanel = new JPanel();
                subpanel.setLayout(new BoxLayout(subpanel, BoxLayout.LINE_AXIS));
                subpanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                subpanel.add(elapsed);
                subpanel.add(Box.createHorizontalGlue());
                subpanel.add(cancel);

                panel.add(subpanel);
            }

            JFrame frame = Viewer.getInstance().gui.getFrame();

            final JDialog progressDialog = new JDialog(frame);

            progressDialog.setTitle(title);
            progressDialog.add(panel);
            progressDialog.setResizable(false);
            progressDialog.pack();
            progressDialog.setLocationRelativeTo(frame);
            progressDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            progressDialog.setVisible(true);

            Thread thread = new Thread(() ->
            {
                try
                {
                    Thread.sleep(10);
                    run.run();
                }
                catch (RuntimeException e)
                {
                    String msg = "Warning, this process failed.  Please check the console for more information";
                    Viewer.getInstance().control.setStatusMessage(msg);
                    SwingUtils.showMessage(msg);
                    e.printStackTrace();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                if (progressDialog != null)
                {
                    progressDialog.dispose();
                }
            });

            frame.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowDeactivated(WindowEvent e)
                {
                    progressDialog.setAlwaysOnTop(false);
                }

                @Override
                public void windowActivated(WindowEvent e)
                {
                    progressDialog.setAlwaysOnTop(true);
                }
            });

            cancel.addActionListener(e ->
            {
                if (thread.isAlive())
                {
                    Viewer.getInstance().control.setStatusMessage("canceled " + title.toLowerCase());
                    thread.stop();
                }

                if (progressDialog != null)
                {
                    progressDialog.dispose();
                }
            });

            progressDialog.addWindowListener(new WindowAdapter()
            {
                public void windowClosing(WindowEvent e)
                {
                    if (thread.isAlive())
                    {
                        Viewer.getInstance().control.setStatusMessage("canceled " + title.toLowerCase());
                        thread.stop();
                    }

                    if (progressDialog != null)
                    {
                        progressDialog.dispose();
                    }
                }
            });

            thread.start();

            final long startTime = System.currentTimeMillis();
            new Thread(() ->
            {
                while (thread.isAlive())
                {
                    try
                    {
                        Thread.sleep(100);

                        long elapsedTime = (System.currentTimeMillis() - startTime);
                        String time = SwingUtils.time(elapsedTime);

                        SwingUtilities.invokeLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                elapsed.setText("Elapsed time: " + time);
                            }
                        });
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }).start();
            Logging.info("running " + title);
        });
    }

    public static JPanel justifyLeft(JPanel panel)
    {
        JPanel container = new JPanel();
        BoxLayout layout = new BoxLayout(container, BoxLayout.Y_AXIS);
        container.setLayout(layout);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(panel);

        return container;
    }

    public static void addEscapeListener(final JDialog dialog)
    {
        ActionListener listener = e -> dialog.setVisible(false);
        dialog.getRootPane().registerKeyboardAction(listener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public static void addEscapeListener(final JFrame frame)
    {
        ActionListener listener = e -> frame.setVisible(false);
        frame.getRootPane().registerKeyboardAction(listener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }


    public static Color chooseColor(Window window, Color initial)
    {
        JColorChooser tcc = new JColorChooser(initial);
        final JOptionPane option = new JOptionPane(tcc, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        option.setIcon(null);
        final JDialog dialog = new JDialog(window, "Choose a color", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setContentPane(option);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent we)
            {
                Viewer.getInstance().control.setStatusMessage("choose a color or cancel");
            }
        });
        option.addPropertyChangeListener(e ->
        {
            String prop = e.getPropertyName();

            if (dialog.isVisible() && e.getSource() == option && prop.equals(JOptionPane.VALUE_PROPERTY))
            {
                dialog.setVisible(false);
            }
        });
        dialog.pack();
        dialog.setVisible(true);

        boolean okay = ((Integer) option.getValue()).intValue() == JOptionPane.OK_OPTION;
        String s = okay ? "updated" : "kept";
        Color c = okay ? tcc.getColor() : initial;
        Viewer.getInstance().control.setStatusMessage(s + " color (" + c.getRed() + ", " + c.getGreen() + ", " + c.getBlue() + ")");

        return c;
    }

    public static boolean getDecision(Component parent, String msg)
    {
        return JOptionPane.showConfirmDialog(parent, msg, "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    public static boolean getDecision(String msg)
    {
        return getDecision(Viewer.getInstance().gui.getFrame(), msg);
    }

    public static boolean confirm(String msg, Runnable runnable)
    {
        if (getDecision(msg))
        {
            runnable.run();
            return true;
        }
        else
        {
            return false;
        }
    }

    public static String getStringSafe(String prompt, String initial)
    {
        final Pointer<String> out = new Pointer<>();
        out.set("");

        SwingUtils.invokeAndWait(() ->
        {
            out.set((String) JOptionPane.showInputDialog(Viewer.getInstance().gui.getFrame(), prompt, prompt, JOptionPane.PLAIN_MESSAGE, null, null, initial));
        });

        return out.get();
    }

    public static String getStringSafe(Component parent, String prompt, String initial)
    {
        final Pointer<String> out = new Pointer<>();
        out.set("");

        SwingUtils.invokeAndWait(() ->
        {
            out.set((String) JOptionPane.showInputDialog(parent, prompt, prompt, JOptionPane.PLAIN_MESSAGE, null, null, initial));
        });

        return out.get();
    }

    public static Optional<String> getStringOptional(String prompt, String initial)
    {
        final Pointer<String> out = new Pointer<>();
        SwingUtils.invokeAndWait(() ->
        {
            out.set((String) JOptionPane.showInputDialog(Viewer.getInstance().gui.getFrame(), prompt, prompt, JOptionPane.PLAIN_MESSAGE, null, null, initial));
        });

        return out.get() == null || out.get().length() == 0 ? Optional.empty() : Optional.of(out.get());
    }

    public static Optional<String> getStringOptional(Component parent, String prompt, String initial)
    {
        final Pointer<String> out = new Pointer<>();
        SwingUtils.invokeAndWait(() ->
        {
            out.set((String) JOptionPane.showInputDialog(parent, prompt, prompt, JOptionPane.PLAIN_MESSAGE, null, null, initial));
        });

        return out.get() == null || out.get().length() == 0 ? Optional.empty() : Optional.of(out.get());
    }

    public static String getString(Component parent, String prompt, String initial)
    {
        String output = (String) JOptionPane.showInputDialog(parent, prompt, prompt, JOptionPane.PLAIN_MESSAGE, null, null, initial);

        return output;
    }

    public static Optional<String> getStringOptionalEventThread(String prompt, String initial)
    {
        final Pointer<String> out = new Pointer<>();
        out.set((String) JOptionPane.showInputDialog(Viewer.getInstance().gui.getFrame(), prompt, prompt, JOptionPane.PLAIN_MESSAGE, null, null, initial));
        return out.get() == null || out.get().length() == 0 ? Optional.empty() : Optional.of(out.get());
    }

    public static void showMessage(Component parent, String title, String msg)
    {
        JOptionPane.showMessageDialog(parent, msg, title, JOptionPane.INFORMATION_MESSAGE);
        Viewer.getInstance().control.setStatusMessage(msg);
    }

    public static void showMessage(Component parent, String msg)
    {
        JOptionPane.showMessageDialog(parent, msg);
        Viewer.getInstance().control.setStatusMessage(msg);
    }

    public static void showMessage(String msg)
    {
        JOptionPane.showMessageDialog(Viewer.getInstance().gui.getFrame(), msg);
        Viewer.getInstance().control.setStatusMessage(msg);
    }

    public static void showMessageScroll(String msg)
    {
        JTextArea textArea = new JTextArea(msg);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        scrollPane.setPreferredSize(new Dimension(500, 500));
        JOptionPane.showMessageDialog(Viewer.getInstance().gui.getFrame(), scrollPane);
    }

    public static java.util.List<String> format(String doc, int maxwidth)
    {
        java.util.List<String> lines = Lists.newArrayList();

        StringBuilder line = new StringBuilder();
        for (String token : doc.split(" "))
        {
            int len = token.length();
            if (line.length() + len > maxwidth)
            {
                lines.add(line.toString());
                line = new StringBuilder();
            }

            line.append(token);
            line.append(" ");
        }

        lines.add(line.toString());

        return lines;
    }

    /**
     * Installs a listener to receive notification when the text of any
     * {@code JTextComponent} is changed. Internally, it installs a
     * {@link DocumentListener} on the text component's {@link Document},
     * and a {@link PropertyChangeListener} on the text component to detect
     * if the {@code Document} itself is replaced.
     *
     * @param text           any text component, such as a {@link JTextField}
     *                       or {@link JTextArea}
     * @param changeListener a listener to receieve {@link ChangeEvent}s
     *                       when the text is changed; the source object for the events
     *                       will be the text component
     * @throws NullPointerException if either parameter is null
     */
    public static void addChangeListener(final JTextComponent text, final ChangeListener changeListener)
    {
        Objects.requireNonNull(text);
        Objects.requireNonNull(changeListener);
        final DocumentListener dl = new DocumentListener()
        {
            private int lastChange = 0, lastNotifiedChange = 0;

            @Override
            public void insertUpdate(DocumentEvent e)
            {
                changedUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                changedUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                lastChange++;
                SwingUtilities.invokeLater(() ->
                {
                    if (lastNotifiedChange != lastChange)
                    {
                        lastNotifiedChange = lastChange;
                        changeListener.stateChanged(new ChangeEvent(text));
                    }
                });
            }
        };
        text.addPropertyChangeListener("document", e ->
        {
            Document d1 = (Document) e.getOldValue();
            Document d2 = (Document) e.getNewValue();
            if (d1 != null)
            {
                d1.removeDocumentListener(dl);
            }
            if (d2 != null)
            {
                d2.addDocumentListener(dl);
            }
            dl.changedUpdate(null);
        });

        Document d = text.getDocument();
        if (d != null)
        {
            d.addDocumentListener(dl);
        }
    }

    public static String time(long millis)
    {
        long secs = millis / 1000;

        String seconds = Integer.toString((int) (secs % 60));
        String minutes = Integer.toString((int) ((secs % 3600) / 60));
        String hours = Integer.toString((int) (secs / 3600));

        if (seconds.length() < 2)
        {
            seconds = "0" + seconds;
        }

        if (minutes.length() < 2)
        {
            minutes = "0" + minutes;
        }

        if (hours.length() < 2)
        {
            hours = "0" + hours;
        }

        String time = hours + ":" + minutes + ":" + seconds;

        return time;
    }
}